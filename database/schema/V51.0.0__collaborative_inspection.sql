-- V51.0.0 Feature 3.2: 协同检查 + Feature 3.3: AI图片识别

ALTER TABLE insp_tasks ADD COLUMN collaboration_mode VARCHAR(20) DEFAULT 'SINGLE'
  COMMENT 'SINGLE|SECTION_SPLIT|FULL_PARALLEL';

CREATE TABLE insp_task_section_assignments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  task_id BIGINT NOT NULL,
  section_id BIGINT NOT NULL,
  inspector_id BIGINT NOT NULL,
  status VARCHAR(20) DEFAULT 'PENDING',
  started_at DATETIME,
  completed_at DATETIME,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  INDEX idx_task (task_id)
) COMMENT='任务分区分配';

-- Feature 3.3: AI图片识别
ALTER TABLE insp_evidences ADD COLUMN ai_analysis JSON DEFAULT NULL COMMENT 'AI识别结果';
ALTER TABLE insp_evidences ADD COLUMN ai_confidence DECIMAL(4,2) DEFAULT NULL;
