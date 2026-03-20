-- V46.0.0 Feature 2.1: 检查项库 (Item Library)

CREATE TABLE insp_library_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  item_code VARCHAR(50) NOT NULL,
  item_name VARCHAR(200) NOT NULL,
  description TEXT,
  item_type VARCHAR(30) NOT NULL,
  category VARCHAR(100) COMMENT '库内分类',
  tags VARCHAR(500) COMMENT '标签(逗号分隔)',
  default_config JSON COMMENT '默认配置',
  default_validation_rules JSON,
  default_scoring_config JSON,
  default_help_content TEXT,
  usage_count INT DEFAULT 0 COMMENT '引用次数',
  is_standard TINYINT DEFAULT 0 COMMENT '是否标准项(不可删除)',
  created_by BIGINT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_code (tenant_id, item_code)
) COMMENT='检查项库';

ALTER TABLE insp_template_items ADD COLUMN library_item_id BIGINT DEFAULT NULL COMMENT '来源库项目ID';
ALTER TABLE insp_template_items ADD COLUMN sync_with_library TINYINT DEFAULT 0 COMMENT '是否与库同步';
