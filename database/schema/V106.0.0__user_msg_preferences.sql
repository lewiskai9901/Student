-- V106: 用户消息偏好表 (Road to S - S-3)
-- 用户能按事件类型选择接收通道, 设置静默时段.

CREATE TABLE IF NOT EXISTS user_msg_preferences (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  user_id BIGINT NOT NULL,
  -- NULL = 全局默认; 非 NULL = 仅对该事件类型生效 (覆盖全局)
  event_type_code VARCHAR(64) DEFAULT NULL,
  -- 启用通道, JSON 数组: ["IN_APP","EMAIL"] — 空数组表示静默该事件
  channels VARCHAR(255) NOT NULL DEFAULT '["IN_APP"]',
  -- 静默时段 (HH:MM 格式), 例如 22:00 - 08:00 不发任何通道
  quiet_hours_start VARCHAR(5) DEFAULT NULL,
  quiet_hours_end VARCHAR(5) DEFAULT NULL,
  -- 是否启用 (false = 此偏好被禁用, fallback 全局默认)
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_user_event (tenant_id, user_id, event_type_code, deleted),
  INDEX idx_user (tenant_id, user_id),
  INDEX idx_event (event_type_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户消息偏好';
