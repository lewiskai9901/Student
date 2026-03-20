-- V48.0.0 Feature 2.4: 合规映射 (Compliance Mapping)

CREATE TABLE insp_compliance_standards (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  standard_code VARCHAR(50) NOT NULL COMMENT '标准编号',
  standard_name VARCHAR(200) NOT NULL COMMENT '标准名称',
  issuing_body VARCHAR(200) COMMENT '发布机构',
  effective_date DATE,
  version VARCHAR(50),
  description TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_code (tenant_id, standard_code)
) COMMENT='合规标准';

CREATE TABLE insp_compliance_clauses (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  standard_id BIGINT NOT NULL,
  clause_number VARCHAR(50) NOT NULL COMMENT '条款编号(如3.2.1)',
  clause_title VARCHAR(200) NOT NULL,
  clause_content TEXT,
  parent_clause_id BIGINT DEFAULT NULL COMMENT '上级条款',
  sort_order INT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  INDEX idx_standard (standard_id)
) COMMENT='合规条款';

CREATE TABLE insp_item_compliance_mappings (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  template_item_id BIGINT NOT NULL,
  clause_id BIGINT NOT NULL,
  coverage_level VARCHAR(20) DEFAULT 'FULL' COMMENT 'FULL|PARTIAL|REFERENCE',
  notes VARCHAR(500),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  INDEX idx_item (template_item_id),
  INDEX idx_clause (clause_id)
) COMMENT='检查项-条款映射';
