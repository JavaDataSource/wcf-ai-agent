package com.wcf.wcfaiagent.service;

import com.wcf.wcfaiagent.entity.ChatMessage;
import com.wcf.wcfaiagent.entity.ChatSession;

import java.util.List;

/**
 * 智能体会话历史服务：会话与消息持久化到本地 MySQL。
 */
public interface ChatSessionService {

    /** 新建会话（标题默认“新对话”，之后由前端用首条用户消息更新） */
    ChatSession createSession(String appKey);

    /** 某应用的会话列表（按最近活跃倒序） */
    List<ChatSession> listSessions(String appKey);

    /** 更新会话标题，返回更新后的会话 */
    ChatSession updateTitle(String id, String title);

    /** 删除会话（连同其全部消息） */
    void deleteSession(String id);

    /** 某会话的消息列表（按发送顺序） */
    List<ChatMessage> listMessages(String sessionId);

    /** 追加保存消息（只需 role/content，时间与归属由服务端填充），返回保存条数 */
    int saveMessages(String sessionId, List<ChatMessage> messages);
}
