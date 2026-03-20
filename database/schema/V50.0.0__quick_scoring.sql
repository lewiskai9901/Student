-- V50.0.0 Feature 3.1: 快捷评分 (Quick Scoring Shortcuts)

CREATE TABLE insp_scoring_presets (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  template_id BIGINT NOT NULL,
  preset_name VARCHAR(100) NOT NULL,
  preset_type VARCHAR(20) NOT NULL COMMENT 'FULL_PASS|FULL_FAIL|CUSTOM',
  item_values JSON NOT NULL COMMENT '[{itemId:1,value:"合格",score:10},...]',
  usage_count INT DEFAULT 0,
  created_by BIGINT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  INDEX idx_template (template_id)
) COMMENT='评分预设';
