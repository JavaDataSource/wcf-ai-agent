package com.wcf.wcfaiagent.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.wcf.wcfaiagent.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 处理工具调用的基础代理类，具体实现了 think 和 act 方法，可以用作创建实例的父类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent {

    // 可用的工具
    private final ToolCallback[] availableTools;

    // 保存了工具调用信息的响应结果（要调用哪些工具）
    private ChatResponse toolCallChatResponse;

    // 工具调用管理者
    private final ToolCallingManager toolCallingManager;

    // 禁用Spring AI 内置的工具调用机制，自己维护选项和消息上下文
    private final ChatOptions chatOptions;

    // 单个工具返回结果的最大长度（超出部分截断）。实测一次网页抓取返回过 200 万+ 字符，
    // 直接塞进对话会撑爆大模型上下文，导致模型"跑偏"、反复乱调工具直到耗尽最大步数
    private static final int MAX_TOOL_RESULT_LENGTH = 8000;

    public ToolCallAgent(ToolCallback[] availableTools) {
        super();
        this.availableTools = availableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        // 禁用 Spring AI 内置的工具调用机制，自己维护选项和消息上下文
        this.chatOptions = DashScopeChatOptions.builder()
                .withInternalToolExecutionEnabled(false)
                .build();
    }

    /**
     * 处理当前状态并决定下一步行动
     *
     * @return 是否需要执行行动
     */
    @Override
    public boolean think() {
        // 1、校验提示词，拼接用户提示词
        if (StrUtil.isNotBlank(getNextStepPrompt())) {
            UserMessage userMessage = new UserMessage(getNextStepPrompt());
            getMessageList().add(userMessage);
        }
        // 2、调用 AI 大模型，获取工具调用结果
        List<Message> messageList = getMessageList();
        Prompt prompt = new Prompt(messageList, this.chatOptions);
        try {
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    // 注意：availableTools 是 ToolCallback[]（由 ToolCallbacks.from 构建），
                    // 必须用 toolCallbacks() 注册；如果用 tools() 会被当作 @Tool 注解的 POJO 解析，
                    // 导致抛出 "No @Tool annotated methods found in MethodToolCallback" 异常，
                    // 使每一步思考都失败、智能体空转 20 步后达到最大步骤
                    .toolCallbacks(availableTools)
                    .call()
                    .chatResponse();
            // 记录响应，用于等下 Act
            this.toolCallChatResponse = chatResponse;
            // 3、解析工具调用结果，获取要调用的工具
            // 助手消息
            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            // 获取要调用的工具列表
            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
            // 输出提示信息
            String result = assistantMessage.getText();
            log.info(getName() + "的思考：" + result);
            log.info(getName() + "选择了 " + toolCallList.size() + " 个工具来使用");
            String toolCallInfo = toolCallList.stream()
                    .map(toolCall -> String.format("工具名称：%s，参数：%s", toolCall.name(), toolCall.arguments()))
                    .collect(Collectors.joining("\n"));
            log.info(toolCallInfo);
            // 如果不需要调用工具，返回 false
            if (toolCallList.isEmpty()) {
                // 只有不调用工具时，才需要手动记录助手消息
                getMessageList().add(assistantMessage);
                return false;
            } else {
                // 需要调用工具时，无需记录助手消息，因为调用工具时会自动记录
                return true;
            }
        } catch (Exception e) {
            log.error(getName() + "的思考过程遇到了问题：" + e.getMessage());
            getMessageList().add(new AssistantMessage("处理时遇到了错误：" + e.getMessage()));
            return false;
        }
    }

    /**
     * 执行工具调用并处理结果
     *
     * @return 执行结果
     */
    @Override
    public String act() {
        if (!toolCallChatResponse.hasToolCalls()) {
            return "没有工具需要调用";
        }
        // 调用工具
        Prompt prompt = new Prompt(getMessageList(), this.chatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);
        // 记录消息上下文，conversationHistory 已经包含了助手消息和工具调用返回的结果
        List<Message> history = new ArrayList<>(toolExecutionResult.conversationHistory());
        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(history);
        // 截断过长的工具返回结果（同时影响回传给大模型的上下文与前端展示），
        // 避免超长网页内容把模型上下文撑爆
        List<ToolResponseMessage.ToolResponse> responses = toolResponseMessage.getResponses();
        if (responses.stream().anyMatch(response -> response.responseData() != null
                && response.responseData().length() > MAX_TOOL_RESULT_LENGTH)) {
            responses = responses.stream()
                    .map(response -> new ToolResponseMessage.ToolResponse(
                            response.id(), response.name(), truncateResult(response.responseData())))
                    .toList();
            toolResponseMessage = new ToolResponseMessage(responses);
            history.set(history.size() - 1, toolResponseMessage);
            setMessageList(history);
        } else {
            setMessageList(history);
        }
        // 判断是否调用了终止工具
        boolean terminateToolCalled = responses.stream()
                .anyMatch(response -> response.name().equals("doTerminate"));
        if (terminateToolCalled) {
            // 任务结束，更改状态
            setState(AgentState.FINISHED);
        }
        String results = responses.stream()
                .map(response -> "工具 " + response.name() + " 返回的结果：" + response.responseData())
                .collect(Collectors.joining("\n"));
        log.info(results);
        return results;
    }

    /**
     * 截断超长的工具返回结果，并追加截断提示。
     * 注意：工具结果此时已被 Spring AI JSON 编码成字符串字面量，截断点落在引号内，
     * 前端解码时会补齐结尾引号（详见前端 decodeToolPayload 的容错逻辑）；
     * 提示文本中不能包含真实换行等控制字符，否则 JSON 字面量非法、前端无法解码。
     */
    private String truncateResult(String data) {
        if (data == null || data.length() <= MAX_TOOL_RESULT_LENGTH) {
            return data;
        }
        return data.substring(0, MAX_TOOL_RESULT_LENGTH)
                + " ...[结果过长已截断，原始长度 " + data.length() + " 字符]";
    }

}
