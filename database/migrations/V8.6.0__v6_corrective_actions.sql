-- =============================================
-- V8.6.0: V6整改记录表
-- =============================================

CREATE TABLE IF NOT EXISTS v6_corrective_actions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    detail_id BIGINT COMMENT '关联的检查明细ID',
    target_id BIGINT COMMENT '检查目标ID',
    task_id BIGINT COMMENT '检查任务ID',
    project_id BIGINT COMMENT '检查项目ID',
    action_code VARCHAR(50) NOT NULL COMMENT '整改单号',
    issue_description VARCHAR(1000) COMMENT '问题描述',
    required_action VARCHAR(1000) COMMENT '整改要求',
    deadline DATE COMMENT '整改截止日期',
    assignee_id BIGINT COMMENT '整改责任人ID',
    assignee_name VARCHAR(100) COMMENT '整改责任人姓名',
    status ENUM('PENDING', 'SUBMITTED', 'VERIFIED', 'REJECTED', 'CANCELLED') DEFAULT 'PENDING' COMMENT '状态',
    correction_note VARCHAR(1000) COMMENT '整改说明',
    evidence_ids JSON COMMENT '整改证据ID列表',
    corrected_at DATETIME COMMENT '整改完成时间',
    verifier_id BIGINT COMMENT '验收人ID',
    verifier_name VARCHAR(100) COMMENT '验收人姓名',
    verified_at DATETIME COMMENT '验收时间',
    verification_note VARCHAR(500) COMMENT '验收说明',
    created_by BIGINT COMMENT '创建人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) DEFAULT 0,

    UNIQUE KEY uk_action_code (action_code),
    INDEX idx_detail_id (detail_id),
    INDEX idx_target_id (target_id),
    INDEX idx_task_id (task_id),
    INDEX idx_project_id (project_id),
    INDEX idx_assignee_id (assignee_id),
    INDEX idx_status (status),
    INDEX idx_deadline (deadline)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V6整改记录表';
