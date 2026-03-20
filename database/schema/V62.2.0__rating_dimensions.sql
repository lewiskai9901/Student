-- V62.2.0: 多维度评级 — 同一份数据评出多个奖项

CREATE TABLE insp_rating_dimensions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  project_id BIGINT NOT NULL,
  dimension_name VARCHAR(100) NOT NULL,
  section_ids TEXT NOT NULL COMMENT 'JSON: 关联的分区ID列表',
  aggregation VARCHAR(20) DEFAULT 'WEIGHTED_AVG' COMMENT 'WEIGHTED_AVG/SUM/AVG/MAX/MIN',
  grade_bands TEXT NULL COMMENT 'JSON: [{code,name,minScore,maxScore,color}]',
  award_name VARCHAR(100) NULL COMMENT '奖项名称',
  ranking_enabled TINYINT DEFAULT 1,
  sort_order INT DEFAULT 0,
  created_by BIGINT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  INDEX idx_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级维度';

CREATE TABLE insp_rating_results (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  dimension_id BIGINT NOT NULL,
  target_id BIGINT NOT NULL,
  target_name VARCHAR(100),
  target_type VARCHAR(20) NOT NULL COMMENT 'ORG/PLACE/USER',
  cycle_date DATE NOT NULL,
  score DECIMAL(10,2),
  grade VARCHAR(20),
  rank_no INT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  INDEX idx_dim_date (dimension_id, cycle_date),
  INDEX idx_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级结果';
