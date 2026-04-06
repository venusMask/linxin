-- 灵信 (LinXin) 性能优化脚本 V1.0.0.2__performance_tuning.sql

-- 为 messages 表添加复合索引，加速增量同步查询
-- 覆盖私聊 (receiver_id, sequence_id)
CREATE INDEX idx_receiver_seq ON `messages` (`receiver_id`, `sequence_id`);

-- 覆盖群聊 (group_id, sequence_id)
CREATE INDEX idx_group_seq ON `messages` (`group_id`, `sequence_id`);

-- 覆盖发送者同步 (sender_id, sequence_id)
CREATE INDEX idx_sender_seq ON `messages` (`sender_id`, `sequence_id`);

-- 为 conversations 表添加索引，加速批量更新
CREATE INDEX idx_group_id ON `conversations` (`group_id`);
