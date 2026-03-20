-- V62.3.0: 违纪记录 — VIOLATION_RECORD 字段类型的数据存储

CREATE TABLE insp_violation_records (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  submission_id BIGINT NOT NULL,
  submission_detail_id BIGINT NOT NULL,
  section_id BIGINT NULL,
  item_id BIGINT NULL,
  user_id BIGINT NOT NULL COMMENT '违纪人员',
  user_name VARCHAR(50),
  class_info VARCHAR(100) COMMENT '班级/部门信息',
  occurred_at DATETIME NOT NULL,
  severity VARCHAR(20) DEFAULT 'MINOR' COMMENT 'MINOR/MODERATE/SEVERE',
  description TEXT,
  evidence_urls TEXT COMMENT 'JSON: 证据图片URL列表',
  score DECIMAL(10,2) COMMENT '扣分值',
  created_by BIGINT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  INDEX idx_submission (submission_id),
  INDEX idx_user (user_id),
  INDEX idx_date (occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='违纪记录';
