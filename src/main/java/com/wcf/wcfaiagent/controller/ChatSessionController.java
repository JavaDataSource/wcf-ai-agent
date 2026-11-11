package com.wcf.wcfaiagent.controller;

import com.wcf.wcfaiagent.entity.ChatMessage;
import com.wcf.wcfaiagent.entity.ChatSession;
import com.wcf.wcfaiagent.service.ChatSessionService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 智能体会话历史接口（上下文路径 /api，完整路径前缀 /api/chat/session）。
 * 会话与消息持久化在本地 MySQL：表 agent_chat_session / agent_chat_message。
 */
@RestController
@RequestMapping("/chat/session")
public class ChatSessionController {

    @Resource
    private ChatSessionService chatSessionService;

    /** 新建会话 */
    @PostMapping
    public ChatSession createSession(@RequestBody CreateSessionRequest request) {
        return chatSessionService.createSession(request.appKey());
    }

    /** 会话列表（按最近活跃倒序） */
    @GetMapping("/list")
    public List<ChatSession> listSessions(@RequestParam String appKey) {
        return chatSessionService.listSessions(appKey);
    }

    /** 修改会话标题（默认取首条用户消息） */
    @PutMapping("/{id}")
    public ChatSession updateSession(@PathVariable String id, @RequestBody UpdateSessionRequest request) {
        return chatSessionService.updateTitle(id, request.title());
    }

    /** 删除会话（连同其全部消息） */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteSession(@PathVariable String id) {
        chatSessionService.deleteSession(id);
        return Collections.singletonMap("deleted", true);
    }

    /** 某会话的消息列表 */
    @GetMapping("/{id}/messages")
    public List<ChatMessage> listMessages(@PathVariable String id) {
        return chatSessionService.listMessages(id);
    }

    /** 追加保存消息（流式输出过程中的节流保存与最终保存共用同一接口） */
    @PostMapping("/{id}/messages")
    public Map<String, Integer> saveMessages(@PathVariable String id, @RequestBody SaveMessagesRequest request) {
        List<ChatMessage> messages = request.messages().stream().map(item -> {
            ChatMessage message = new ChatMessage();
            message.setRole(item.role());
            message.setContent(item.content());
            return message;
        }).toList();
        return Collections.singletonMap("saved", chatSessionService.saveMessages(id, messages));
    }

    // ---------- 请求体 DTO ----------

    /** 新建会话请求 */
    public record CreateSessionRequest(String appKey) {
    }

    /** 更新会话标题请求 */
    public record UpdateSessionRequest(String title) {
    }

    /** 批量保存消息请求 */
    public record SaveMessagesRequest(List<MessageItem> messages) {
    }

    /** 单条消息（只需角色与内容，时间由服务端填充） */
    public record MessageItem(String role, String content) {
    }
}
