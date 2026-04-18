-- 事件/消息可靠性增强
-- 1) entity_events 增加 idempotency_key：业务可选传入，同一 key 重复调用只写一次
-- 2) msg_notifications 增加 send_status / retry_count / last_error / sent_at：为多通道与失败重试做准备

-- P1-5 幂等 key
ALTER TABLE entity_events
  ADD COLUMN idempotency_key VARCHAR(160) NULL COMMENT '业务幂等 key，唯一。拼接格式 {prefix}:{triggerId}:{subjectType}:{subjectId}'
    AFTER event_label;

-- MySQL 中 UNIQUE 索引允许多行 NULL，这正是我们要的（可选幂等）
ALTER TABLE entity_events
  ADD UNIQUE KEY uk_entity_events_idempotency (idempotency_key);

-- P1-4 通知发送状态
ALTER TABLE msg_notifications
  ADD COLUMN send_status VARCHAR(20) NOT NULL DEFAULT 'SENT' COMMENT '发送状态: PENDING/SENT/FAILED'
    AFTER event_id,
  ADD COLUMN retry_count INT NOT NULL DEFAULT 0 COMMENT '已重试次数'
    AFTER send_status,
  ADD COLUMN last_error VARCHAR(500) NULL COMMENT '最近一次失败原因（用于排查）'
    AFTER retry_count,
  ADD COLUMN sent_at DATETIME NULL COMMENT '真实送达时间（站内信=插入时；多通道=通道回调时）'
    AFTER last_error;

-- 将历史行的 sent_at 回填为 created_at（语义：历史站内信落库即送达）
UPDATE msg_notifications SET sent_at = created_at WHERE sent_at IS NULL AND deleted = 0;

-- 状态字段索引，便于后续补偿任务扫描 FAILED / PENDING
ALTER TABLE msg_notifications
  ADD INDEX idx_send_status (send_status, retry_count);
