-- V1.0.0.2__add_message_status.sql
CREATE TABLE IF NOT EXISTS `message_status` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `message_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `read_status` TINYINT DEFAULT 0 COMMENT '0:未读, 1:已读',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    INDEX `idx_user_read` (`user_id`, `read_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
