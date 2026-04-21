-- V20260425_1: 消息派发死信表 (M4.2)
-- 用途: MessageDispatcher 捕获 per-user save 异常时写入, 保护整批其他用户仍能派发;
--       配合后续定时重试 job (next_retry_at < NOW() AND retry_count < N) 兜底.
CREATE TABLE IF NOT EXISTS msg_notification_failures (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id BIGINT COMMENT '触发此失败的 entity_events.id',
    rule_id BIGINT COMMENT '订阅规则 msg_subscription_rules.id',
    target_user_id BIGINT COMMENT '目标用户',
    error_message TEXT NOT NULL COMMENT '异常 class + message',
    retry_count INT DEFAULT 0 COMMENT '已重试次数',
    next_retry_at DATETIME COMMENT '下次重试时间; NULL = 不再重试',
    tenant_id BIGINT NOT NULL DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_retry (next_retry_at, retry_count),
    INDEX idx_event (event_id),
    INDEX idx_tenant (tenant_id)
) COMMENT='消息派发死信表 (单条 save 异常落此, 供定时重试)';
