-- 灵信 (LinXin) 数据库初始化脚本 V1.0.0.1__init.sql
-- 整合了 Agent 开放能力与多端同步机制

-- 1. 用户表
CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名/灵信号',
    `nickname` VARCHAR(100) NOT NULL COMMENT '昵称',
    `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `password` VARCHAR(255) NOT NULL COMMENT '密码',
    `gender` TINYINT DEFAULT 0 COMMENT '性别: 0-未知, 1-男, 2-女',
    `signature` VARCHAR(255) DEFAULT NULL COMMENT '个性签名',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常',
    `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username_deleted` (`username`, `deleted`),
    UNIQUE KEY `uk_phone_deleted` (`phone`, `deleted`),
    UNIQUE KEY `uk_email_deleted` (`email`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 2. 好友关系表
CREATE TABLE IF NOT EXISTS `friends` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `friend_id` BIGINT NOT NULL COMMENT '好友用户ID',
    `friend_nickname` VARCHAR(100) DEFAULT NULL COMMENT '好友备注昵称',
    `friend_group` VARCHAR(50) DEFAULT '默认分组' COMMENT '好友分组',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT '私有标签，多个用逗号分隔（如: 家人,媳妇）',
    `status` TINYINT DEFAULT 1 COMMENT '关系状态: 0-待确认, 1-已是好友, 2-已拉黑',
    `apply_remark` VARCHAR(255) DEFAULT NULL COMMENT '申请备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_friend` (`user_id`, `friend_id`),
    KEY `idx_friend_id` (`friend_id`),
    KEY `idx_status` (`status`),
    KEY `idx_friend_tags` (`tags`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友关系表';

-- 3. 好友申请记录表
CREATE TABLE IF NOT EXISTS `friend_apply` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `from_user_id` BIGINT NOT NULL COMMENT '申请人ID',
    `to_user_id` BIGINT NOT NULL COMMENT '被申请人ID',
    `from_nickname` VARCHAR(100) DEFAULT NULL COMMENT '申请人昵称',
    `from_avatar` VARCHAR(500) DEFAULT NULL COMMENT '申请人头像',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '申请备注',
    `status` TINYINT DEFAULT 0 COMMENT '状态: 0-待处理, 1-已同意, 2-已拒绝, 3-已过期',
    `read_status` TINYINT DEFAULT 0 COMMENT '已读状态: 0-未读, 1-已读',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `handle_time` DATETIME DEFAULT NULL COMMENT '处理时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_from_user` (`from_user_id`),
    KEY `idx_to_user` (`to_user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友申请记录表';

-- 4. 会话表
CREATE TABLE IF NOT EXISTS `conversations` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `peer_id` BIGINT NOT NULL COMMENT '对方用户ID',
    `peer_nickname` VARCHAR(100) DEFAULT NULL COMMENT '对方昵称(冗余存储)',
    `peer_avatar` VARCHAR(500) DEFAULT NULL COMMENT '对方头像(冗余存储)',
    `type` TINYINT DEFAULT 0 COMMENT '会话类型：0-私聊 1-群聊',
    `group_id` BIGINT DEFAULT NULL COMMENT '群聊时非空',
    `last_message_id` BIGINT DEFAULT NULL COMMENT '最后一条消息ID',
    `last_message_content` VARCHAR(500) DEFAULT NULL COMMENT '最后一条消息内容摘要',
    `last_message_type` TINYINT DEFAULT NULL COMMENT '最后一条消息类型: 1-文本, 2-图片, 3-语音, 4-视频, 5-文件',
    `last_message_time` DATETIME DEFAULT NULL COMMENT '最后消息时间',
    `unread_count` INT DEFAULT 0 COMMENT '未读消息数量',
    `top_status` TINYINT DEFAULT 0 COMMENT '置顶状态: 0-未置顶, 1-已置顶',
    `mute_status` TINYINT DEFAULT 0 COMMENT '免打扰状态: 0-正常, 1-已静音',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_peer_type` (`user_id`, `peer_id`, `type`, `deleted`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_peer_id` (`peer_id`),
    KEY `idx_last_message_time` (`last_message_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话表';

-- 5. 消息表
CREATE TABLE IF NOT EXISTS `messages` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `conversation_id` BIGINT NOT NULL COMMENT '会话ID',
    `sender_id` BIGINT NOT NULL COMMENT '发送者ID',
    `receiver_id` BIGINT NOT NULL COMMENT '接收者ID',
    `group_id` BIGINT DEFAULT NULL COMMENT '群消息时非空',
    `message_type` TINYINT NOT NULL DEFAULT 1 COMMENT '消息类型: 1-文本, 2-图片...',
    `content` TEXT COMMENT '消息内容',
    `extra` JSON DEFAULT NULL COMMENT '扩展字段',
    `send_status` TINYINT DEFAULT 1 COMMENT '发送状态',
    `send_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    `sequence_id` BIGINT UNIQUE COMMENT '全局递增序列号，用于多端同步',
    `is_ai` TINYINT(1) DEFAULT 0 COMMENT '是否由AI发送: 0-人, 1-AI',
    `sender_type` VARCHAR(50) DEFAULT NULL COMMENT 'Agent名称标识',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_conversation_id` (`conversation_id`),
    KEY `idx_sender_id` (`sender_id`),
    KEY `idx_receiver_id` (`receiver_id`),
    KEY `idx_sequence_id` (`sequence_id`),
    KEY `idx_is_ai` (`is_ai`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- 6. 消息状态表
CREATE TABLE IF NOT EXISTS `message_status` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `message_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `read_status` TINYINT DEFAULT 0,
    `read_time` DATETIME DEFAULT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    UNIQUE KEY `uk_message_user` (`message_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. 群组基本信息表
CREATE TABLE IF NOT EXISTS `groups` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(64) NOT NULL,
    `avatar` VARCHAR(256) DEFAULT NULL,
    `owner_id` BIGINT NOT NULL,
    `member_count` INT NOT NULL DEFAULT 0,
    `status` TINYINT NOT NULL DEFAULT 0,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. 群成员关系表
CREATE TABLE IF NOT EXISTS `group_members` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `group_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `nickname` VARCHAR(64) DEFAULT NULL,
    `role` TINYINT DEFAULT 0,
    `join_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY `uk_group_user` (`group_id`, `user_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. Agent 令牌管理表
CREATE TABLE IF NOT EXISTS `agent_tokens` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
    `token` VARCHAR(128) NOT NULL UNIQUE COMMENT '令牌字符串',
    `agent_name` VARCHAR(50) NOT NULL COMMENT 'Agent名称',
    `scopes` VARCHAR(255) DEFAULT 'msg:send' COMMENT '授权范围',
    `status` TINYINT(1) DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `last_used_time` DATETIME DEFAULT NULL,
    `expire_time` DATETIME DEFAULT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_token` (`token`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='第三方Agent授权令牌';

-- 1. AI 消耗日志表
CREATE TABLE `ai_usage_logs` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `provider_name` VARCHAR(50) NOT NULL COMMENT '供应商名称',
    `model_name` VARCHAR(50) NOT NULL COMMENT '具体模型名称',
    `intent` VARCHAR(50) DEFAULT 'chat' COMMENT '意图类型',
    `prompt_tokens` INT DEFAULT 0 COMMENT '输入消耗',
    `completion_tokens` INT DEFAULT 0 COMMENT '输出消耗',
    `total_tokens` INT DEFAULT 0 COMMENT '总消耗',
    `request_id` VARCHAR(100) DEFAULT NULL COMMENT 'AI平台请求ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_time` (`user_id`, `create_time`),
    INDEX `idx_provider_model` (`provider_name`, `model_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI Token消耗统计日志';

