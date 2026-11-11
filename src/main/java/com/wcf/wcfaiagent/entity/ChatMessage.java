package com.wcf.wcfaiagent.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 智能体会话消息实体（表 agent_chat_message）。
 */
@Data
public class ChatMessage {

    /** 自增主键（同时用于消息排序） */
    private Long id;

    /** 所属会话 ID（agent_chat_session.id） */
    private String sessionId;

    /** 消息角色：user / ai */
    private String role;

    /** 消息内容 */
    private String content;

    /** 发送时间 */
    private LocalDateTime createdAt;
}
