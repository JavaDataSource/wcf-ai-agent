package com.wcf.wcfaiagent.agent;

import com.wcf.wcfaiagent.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/**
 * ReAct (Reasoning and Acting) 模式的代理抽象类
 * 实现了思考-行动的循环模式
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public abstract class ReActAgent extends BaseAgent {

    // 连续"无需行动"的步骤数（模型连续不调用工具，说明它认为任务已完成或陷入停滞）
    private int noActionStreak = 0;

    // 连续多少步"无需行动"后主动结束任务
    private static final int MAX_NO_ACTION_STEPS = 2;

    /**
     * 处理当前状态并决定下一步行动
     *
     * @return 是否需要执行行动，true表示需要执行，false表示不需要执行
     */
    public abstract boolean think();

    /**
     * 执行决定的行动
     *
     * @return 行动执行结果
     */
    public abstract String act();

    /**
     * 执行单个步骤：思考和行动
     *
     * @return 步骤执行结果
     */
    @Override
    public String step() {
        try {
            // 先思考
            boolean shouldAct = think();
            if (!shouldAct) {
                noActionStreak++;
                // 模型连续多步不调用任何工具：视为任务已完成（或陷入停滞），
                // 主动结束循环。否则会反复注入下一步提示词"催促"模型，
                // 导致它乱调工具、无限循环直到耗尽 maxSteps（实测出现过连续 5 步无需行动
                // 后开始疯狂搜索无关内容的情况）
                if (noActionStreak >= MAX_NO_ACTION_STEPS) {
                    log.info("连续 {} 步无需行动，智能体主动结束任务", noActionStreak);
                    setState(AgentState.FINISHED);
                }
                return "思考完成 - 无需行动";
            }
            // 恢复行动后清零计数
            noActionStreak = 0;
            // 再行动
            return act();
        } catch (Exception e) {
            // 记录异常日志
            log.error("步骤执行失败", e);
            return "步骤执行失败: " + e.getMessage();
        }
    }

}
