-- V54.0.0 Feature 5.1: 子任务分解

CREATE TABLE insp_corrective_subtasks (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  case_id BIGINT NOT NULL COMMENT '所属纠正案例',
  subtask_name VARCHAR(200) NOT NULL,
  description TEXT,
  assignee_id BIGINT NOT NULL,
  status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING|IN_PROGRESS|COMPLETED|BLOCKED',
  priority INT DEFAULT 0,
  due_date DATE,
  completed_at DATETIME,
  sort_order INT DEFAULT 0,
  created_by BIGINT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  INDEX idx_case (case_id)
) COMMENT='纠正案例子任务';
