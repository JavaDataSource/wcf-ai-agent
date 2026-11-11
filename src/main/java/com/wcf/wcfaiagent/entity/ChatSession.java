package com.wcf.wcfaiagent.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 智能体会话实体（表 agent_chat_session）。
 */
@Data
public class ChatSession {

    /** 会话 ID（UUID），AI 恋爱大师同时用它作为后端 chatId 实现多会话上下文隔离 */
    private String id;

    /** 所属应用标识：loveApp / manus */
    private String appKey;

    /** 会话标题（默认取首条用户消息） */
    private String title;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 最近活跃时间（会话列表按此倒序） */
    private LocalDateTime updatedAt;
}
