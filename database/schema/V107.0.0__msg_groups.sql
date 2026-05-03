-- V107: 通知组 (Road to S+ - S+2)
-- 一个通知组聚合 N 个用户, 订阅规则可指向 receiverType=GROUP 推送给整组用户.

CREATE TABLE IF NOT EXISTS msg_groups (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  group_code VARCHAR(64) NOT NULL,
  group_name VARCHAR(128) NOT NULL,
  description VARCHAR(255) DEFAULT NULL,
  -- 是否启用 — 关闭后规则指向此组的消息将无人接收
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_by BIGINT DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_code (tenant_id, group_code, deleted),
  INDEX idx_tenant (tenant_id, enabled, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知组';

CREATE TABLE IF NOT EXISTS msg_group_members (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  group_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  added_by BIGINT DEFAULT NULL,
  added_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_group_user (group_id, user_id, deleted),
  INDEX idx_user (user_id),
  INDEX idx_group (group_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知组成员';
