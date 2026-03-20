-- V53.0.0 Feature 4.3: 预警看板 + Feature 4.5: 同比/环比

-- 4.3 预警规则
CREATE TABLE insp_alert_rules (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  rule_name VARCHAR(100) NOT NULL,
  metric_type VARCHAR(50) NOT NULL COMMENT 'SCORE_DROP|CONSECUTIVE_FAIL|HIGH_DEVIATION|LOW_COMPLIANCE|OVERDUE_CORRECTION',
  threshold_config JSON NOT NULL COMMENT '阈值配置',
  severity VARCHAR(20) NOT NULL DEFAULT 'WARNING' COMMENT 'INFO|WARNING|CRITICAL',
  notification_channels JSON COMMENT '通知渠道配置',
  is_enabled TINYINT DEFAULT 1,
  project_id BIGINT DEFAULT NULL COMMENT 'NULL=全局',
  created_by BIGINT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
) COMMENT='预警规则';

-- 4.3 预警记录
CREATE TABLE insp_alerts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  alert_rule_id BIGINT NOT NULL,
  target_id BIGINT,
  target_type VARCHAR(30),
  target_name VARCHAR(200),
  metric_value DECIMAL(10,2) COMMENT '触发值',
  threshold_value DECIMAL(10,2) COMMENT '阈值',
  severity VARCHAR(20) NOT NULL,
  message TEXT NOT NULL,
  status VARCHAR(20) DEFAULT 'OPEN' COMMENT 'OPEN|ACKNOWLEDGED|RESOLVED|DISMISSED',
  acknowledged_by BIGINT DEFAULT NULL,
  acknowledged_at DATETIME DEFAULT NULL,
  resolved_at DATETIME DEFAULT NULL,
  triggered_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  INDEX idx_status (status),
  INDEX idx_triggered (triggered_at)
) COMMENT='预警记录';

-- 4.5 同比/环比
ALTER TABLE insp_period_summaries ADD COLUMN prev_period_score DECIMAL(8,2) DEFAULT NULL COMMENT '上期分数';
ALTER TABLE insp_period_summaries ADD COLUMN mom_change DECIMAL(6,2) DEFAULT NULL COMMENT '环比变化(%)';
ALTER TABLE insp_period_summaries ADD COLUMN yoy_score DECIMAL(8,2) DEFAULT NULL COMMENT '去年同期分数';
ALTER TABLE insp_period_summaries ADD COLUMN yoy_change DECIMAL(6,2) DEFAULT NULL COMMENT '同比变化(%)';
