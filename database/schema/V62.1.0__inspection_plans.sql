-- V62.1.0: 检查计划 — 排期从项目层管理
-- 一个项目可以有多个检查计划，每个计划关联部分分区

CREATE TABLE insp_inspection_plans (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  project_id BIGINT NOT NULL,
  plan_name VARCHAR(100) NOT NULL,
  section_ids TEXT NOT NULL COMMENT 'JSON: 关联的一级分区ID列表',
  schedule_mode VARCHAR(20) DEFAULT 'REGULAR' COMMENT 'REGULAR=定期/ON_DEMAND=手动触发',
  cycle_type VARCHAR(20) DEFAULT 'DAILY' COMMENT 'DAILY/WEEKLY/MONTHLY',
  frequency INT DEFAULT 1 COMMENT '每周期执行次数',
  schedule_days VARCHAR(100) NULL COMMENT 'JSON: [1,3,5] 周几(WEEKLY时)',
  time_slots TEXT NULL COMMENT 'JSON: ["07:00-08:00","12:00-13:00"]',
  skip_holidays TINYINT DEFAULT 0,
  is_enabled TINYINT DEFAULT 1,
  sort_order INT DEFAULT 0,
  created_by BIGINT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  INDEX idx_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查计划';
