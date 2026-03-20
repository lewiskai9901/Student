-- V56.0.0 Feature 6.1: NFC打卡 + Feature 6.2: IM集成

CREATE TABLE insp_nfc_tags (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  tag_uid VARCHAR(100) NOT NULL COMMENT 'NFC标签UID',
  location_name VARCHAR(200) NOT NULL,
  place_id BIGINT DEFAULT NULL COMMENT '关联场所',
  org_unit_id BIGINT DEFAULT NULL COMMENT '关联组织',
  is_active TINYINT DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_uid (tenant_id, tag_uid)
) COMMENT='NFC标签注册';

ALTER TABLE insp_submissions ADD COLUMN nfc_tag_uid VARCHAR(100) DEFAULT NULL;
ALTER TABLE insp_submissions ADD COLUMN checkin_time DATETIME DEFAULT NULL;

ALTER TABLE insp_webhook_subscriptions ADD COLUMN platform VARCHAR(30) DEFAULT 'GENERIC'
  COMMENT 'GENERIC|DINGTALK|WECOM|FEISHU|SLACK';
ALTER TABLE insp_webhook_subscriptions ADD COLUMN message_template TEXT DEFAULT NULL
  COMMENT '消息模板(支持变量替换)';
