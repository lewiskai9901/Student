-- 事件类型定义（可配置）
CREATE TABLE IF NOT EXISTS entity_event_types (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  category_code VARCHAR(30) NOT NULL,
  category_name VARCHAR(50) NOT NULL,
  type_code VARCHAR(50) NOT NULL,
  type_name VARCHAR(100) NOT NULL,
  has_score TINYINT DEFAULT 0,
  has_severity TINYINT DEFAULT 0,
  severity_levels VARCHAR(500) NULL COMMENT 'JSON数组',
  icon VARCHAR(50) NULL,
  color VARCHAR(20) NULL,
  applicable_subjects VARCHAR(100) NULL COMMENT 'JSON: ["USER","ORG","PLACE"]',
  is_system TINYINT DEFAULT 0,
  is_enabled TINYINT DEFAULT 1,
  sort_order INT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_type (tenant_id, type_code)
);

-- 事件记录
CREATE TABLE IF NOT EXISTS entity_events (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  subject_type VARCHAR(20) NOT NULL,
  subject_id BIGINT NOT NULL,
  subject_name VARCHAR(100) NULL,
  event_category VARCHAR(30) NOT NULL,
  event_type VARCHAR(50) NOT NULL,
  event_label VARCHAR(100) NULL,
  payload TEXT NULL COMMENT 'JSON',
  source_module VARCHAR(30) NULL,
  source_ref_type VARCHAR(30) NULL,
  source_ref_id BIGINT NULL,
  tags VARCHAR(500) NULL COMMENT 'JSON数组',
  created_by BIGINT NULL,
  created_by_name VARCHAR(50) NULL,
  occurred_at DATETIME NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  INDEX idx_subject (subject_type, subject_id, occurred_at DESC),
  INDEX idx_category (event_category, event_type, occurred_at DESC),
  INDEX idx_source (source_ref_type, source_ref_id),
  INDEX idx_occurred (occurred_at DESC)
);

-- 关联主体
CREATE TABLE IF NOT EXISTS entity_event_relations (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  event_id BIGINT NOT NULL,
  related_type VARCHAR(20) NOT NULL,
  related_id BIGINT NOT NULL,
  related_name VARCHAR(100) NULL,
  relation VARCHAR(30) NULL,
  INDEX idx_event (event_id),
  INDEX idx_related (related_type, related_id)
);

-- 预置事件类型
INSERT INTO entity_event_types (category_code, category_name, type_code, type_name, has_score, applicable_subjects, is_system) VALUES
('INSPECTION', '检查', 'INSP_GRADE', '检查评级', 1, '["ORG","PLACE","USER"]', 1),
('INSPECTION', '检查', 'INSP_VIOLATION', '违规记录', 1, '["USER","ORG","PLACE"]', 1),
('INSPECTION', '检查', 'INSP_COMMENDATION', '表扬记录', 1, '["USER","ORG","PLACE"]', 1),
('INSPECTION', '检查', 'INSP_RATING', '综合评级', 0, '["ORG","PLACE","USER"]', 1),
('INSPECTION', '检查', 'INSP_NOTE', '检查备注', 0, '["ORG","PLACE","USER"]', 1),
('ORG', '组织', 'ORG_TRANSFER', '组织调动', 0, '["USER"]', 1),
('ORG', '组织', 'ORG_JOIN', '加入组织', 0, '["USER"]', 1),
('ORG', '组织', 'ORG_LEAVE', '离开组织', 0, '["USER"]', 1),
('PLACE', '场所', 'PLACE_ASSIGN', '场所分配', 0, '["USER","ORG"]', 1),
('PLACE', '场所', 'PLACE_RELEASE', '场所释放', 0, '["USER","ORG"]', 1);
