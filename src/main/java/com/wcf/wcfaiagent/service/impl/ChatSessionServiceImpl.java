package com.wcf.wcfaiagent.service.impl;

import com.wcf.wcfaiagent.entity.ChatMessage;
import com.wcf.wcfaiagent.entity.ChatSession;
import com.wcf.wcfaiagent.mapper.ChatMessageMapper;
import com.wcf.wcfaiagent.mapper.ChatSessionMapper;
import com.wcf.wcfaiagent.service.ChatSessionService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 智能体会话历史服务实现：读写本地 MySQL（agent_chat_session / agent_chat_message）。
 */
@Service
public class ChatSessionServiceImpl implements ChatSessionService {

    @Resource
    private ChatSessionMapper chatSessionMapper;

    @Resource
    private ChatMessageMapper chatMessageMapper;

    @Override
    public ChatSession createSession(String appKey) {
        ChatSession session = new ChatSession();
        session.setId(UUID.randomUUID().toString());
        session.setAppKey(appKey);
        session.setTitle("新对话");
        LocalDateTime now = LocalDateTime.now();
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        chatSessionMapper.insert(session);
        return session;
    }

    @Override
    public List<ChatSession> listSessions(String appKey) {
        return chatSessionMapper.selectByAppKey(appKey);
    }

    @Override
    public ChatSession updateTitle(String id, String title) {
        chatSessionMapper.updateTitle(id, title);
        return chatSessionMapper.selectById(id);
    }

    @Override
    @Transactional("mysqlTransactionManager")
    public void deleteSession(String id) {
        chatMessageMapper.deleteBySessionId(id);
        chatSessionMapper.deleteById(id);
    }

    @Override
    public List<ChatMessage> listMessages(String sessionId) {
        return chatMessageMapper.selectBySessionId(sessionId);
    }

    @Override
    @Transactional("mysqlTransactionManager")
    public int saveMessages(String sessionId, List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        for (ChatMessage message : messages) {
            message.setSessionId(sessionId);
            message.setCreatedAt(now);
        }
        chatMessageMapper.batchInsert(messages);
        // 会话有新动态：刷新最近活跃时间，供会话列表排序
        chatSessionMapper.updateTime(sessionId);
        return messages.size();
    }
}
