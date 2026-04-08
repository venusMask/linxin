-- 灵信 (LinXin) 性能优化脚本 V1.0.0.3__optimize_private_chat_index.sql

-- 为 messages 表添加复合索引，加速私聊读扩散查询
-- 该模式下 conversation_id 被统一设为 0，原索引 idx_user_message 失效
-- 通过 (sender_id, receiver_id) 或 (receiver_id, sender_id) 结合 send_time 进行高效范围查询

-- 优化正向查询 (A 发给 B)
CREATE INDEX idx_private_chat ON `messages` (`sender_id`, `receiver_id`, `send_time`);

-- 优化反向查询 (B 发给 A)
CREATE INDEX idx_private_chat_reverse ON `messages` (`receiver_id`, `sender_id`, `send_time`);
