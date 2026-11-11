-- =============================================================
-- 智能体会话历史表结构（MySQL，库名 ai_project）
-- 应用启动时会由 MybatisConfig 自动执行本脚本（IF NOT EXISTS，可重复执行），
-- 也可以手动执行一次。
-- 约束：本次新建的表名全部以 agent_ 开头。
-- =============================================================

-- 会话表
CREATE TABLE IF NOT EXISTS `agent_chat_session` (
    `id`         CHAR(36)     NOT NULL                COMMENT '会话 ID（UUID，恋爱大师同时用作后端 chatId）',
    `app_key`    VARCHAR(32)  NOT NULL                COMMENT '所属应用标识：loveApp / manus',
    `title`      VARCHAR(128) NOT NULL DEFAULT '新对话' COMMENT '会话标题（默认取首条用户消息）',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近活跃时间（列表按此倒序）',
    PRIMARY KEY (`id`),
    KEY `idx_agent_session_app` (`app_key`, `updated_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='智能体会话表';

-- 消息表
CREATE TABLE IF NOT EXISTS `agent_chat_message` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键（同时用于消息排序）',
    `session_id` CHAR(36)     NOT NULL                COMMENT '所属会话 ID（agent_chat_session.id）',
    `role`       VARCHAR(16)  NOT NULL                COMMENT '消息角色：user / ai',
    `content`    MEDIUMTEXT   NOT NULL                COMMENT '消息内容',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (`id`),
    KEY `idx_agent_message_session` (`session_id`, `id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='智能体会话消息表';
