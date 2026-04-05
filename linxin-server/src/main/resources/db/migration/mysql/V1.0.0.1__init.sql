-- 灵信 (LinXin) 数据库初始化脚本 V1.0.0.1__init.sql (全面对齐版)

-- 1. 用户表
CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL,
    `nickname` VARCHAR(100) NOT NULL,
    `avatar` VARCHAR(500) DEFAULT NULL,
    `phone` VARCHAR(20) DEFAULT NULL,
    `email` VARCHAR(100) DEFAULT NULL,
    `password` VARCHAR(255) NOT NULL,
    `password_version` INT DEFAULT 1, -- 为强制退出旧登录态添加的版本号

    `gender` TINYINT DEFAULT 0,
    `signature` VARCHAR(255) DEFAULT NULL,
    `status` TINYINT DEFAULT 1,
    `user_type` TINYINT DEFAULT 0,
    `last_login_time` DATETIME DEFAULT NULL,
    `last_login_ip` VARCHAR(50) DEFAULT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    UNIQUE KEY `uk_username_deleted` (`username`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. 好友关系表
CREATE TABLE IF NOT EXISTS `friends` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `friend_id` BIGINT NOT NULL,
    `friend_nickname` VARCHAR(100) DEFAULT NULL,
    `friend_group` VARCHAR(50) DEFAULT '默认分组',
    `tags` VARCHAR(255) DEFAULT NULL,
    `status` TINYINT DEFAULT 1,
    `apply_remark` VARCHAR(255) DEFAULT NULL,
    `sequence_id` BIGINT DEFAULT NULL, -- 为好友关系同步添加的序列号
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    UNIQUE KEY `uk_user_friend` (`user_id`, `friend_id`),
    INDEX `idx_user_friend_sequence` (`user_id`, `sequence_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. 好友申请表
CREATE TABLE IF NOT EXISTS `friend_apply` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `from_user_id` BIGINT NOT NULL,
    `to_user_id` BIGINT NOT NULL,
    `from_nickname` VARCHAR(100) DEFAULT NULL,
    `from_avatar` VARCHAR(500) DEFAULT NULL,
    `remark` VARCHAR(255) DEFAULT NULL,
    `status` TINYINT DEFAULT 0,
    `read_status` TINYINT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `handle_time` DATETIME DEFAULT NULL,
    `deleted` TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. 会话表
CREATE TABLE IF NOT EXISTS `conversations` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `peer_id` BIGINT NOT NULL,
    `peer_nickname` VARCHAR(100) DEFAULT NULL,
    `peer_avatar` VARCHAR(500) DEFAULT NULL,
    `type` TINYINT DEFAULT 0,
    `group_id` BIGINT DEFAULT NULL,
    `last_message_id` BIGINT DEFAULT NULL,
    `last_message_content` VARCHAR(500) DEFAULT NULL,
    `last_message_type` TINYINT DEFAULT NULL,
    `last_message_time` DATETIME DEFAULT NULL,
    `unread_count` INT DEFAULT 0,
    `top_status` TINYINT DEFAULT 0,
    `mute_status` TINYINT DEFAULT 0,
    `version` BIGINT DEFAULT 0, -- 为会话增量同步添加的版本号
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    UNIQUE KEY `uk_user_peer_type` (`user_id`, `peer_id`, `type`, `deleted`),
    INDEX `idx_user_conversation_version` (`user_id`, `version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. 消息表
CREATE TABLE IF NOT EXISTS `messages` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `conversation_id` BIGINT NOT NULL,
    `sender_id` BIGINT NOT NULL,
    `receiver_id` BIGINT NOT NULL,
    `group_id` BIGINT DEFAULT NULL,
    `message_type` TINYINT NOT NULL DEFAULT 1,
    `content` TEXT,
    `extra` JSON DEFAULT NULL,
    `send_status` TINYINT DEFAULT 1,
    `read_status` TINYINT DEFAULT 0, -- 消息读取状态
    `is_recall` TINYINT DEFAULT 0, -- 消息是否撤回

    `send_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `sequence_id` BIGINT UNIQUE,
    `is_ai` TINYINT(1) DEFAULT 0,
    `sender_type` VARCHAR(50) DEFAULT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. 群组表
CREATE TABLE IF NOT EXISTS `groups` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(64) NOT NULL,
    `avatar` VARCHAR(256) DEFAULT NULL,
    `owner_id` BIGINT NOT NULL,
    `announcement` VARCHAR(500) DEFAULT NULL,
    `member_limit` INT DEFAULT 500,
    `member_count` INT NOT NULL DEFAULT 0,
    `status` TINYINT NOT NULL DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. 群成员表
CREATE TABLE IF NOT EXISTS `group_members` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `group_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `nickname` VARCHAR(64) DEFAULT NULL,
    `role` TINYINT DEFAULT 0,
    `mute_status` TINYINT DEFAULT 0,
    `join_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    UNIQUE KEY `uk_group_user` (`group_id`, `user_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. AI 消耗日志表
CREATE TABLE IF NOT EXISTS `ai_usage_logs` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `provider_name` VARCHAR(50) NOT NULL,
    `model_name` VARCHAR(50) NOT NULL,
    `intent` VARCHAR(50) DEFAULT 'chat',
    `prompt_tokens` INT DEFAULT 0,
    `completion_tokens` INT DEFAULT 0,
    `total_tokens` INT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 9. 用户 Token 统计表
CREATE TABLE IF NOT EXISTS `user_token_usage` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `token_count` BIGINT DEFAULT 0,
    `last_update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. 邮箱验证码表
CREATE TABLE IF NOT EXISTS `email_verification_codes` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `email` VARCHAR(100) NOT NULL,
    `code` VARCHAR(10) NOT NULL,
    `type` VARCHAR(20) NOT NULL,
    `status` TINYINT DEFAULT 0,
    `expire_time` DATETIME NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    INDEX `idx_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. 初始数据
INSERT INTO `users` (`id`, `username`, `nickname`, `password`, `user_type`, `status`) 
VALUES (1, 'ai_assistant', 'AI 助手', 'system_protected', 1, 1);
