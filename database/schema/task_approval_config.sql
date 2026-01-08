-- 任务审批配置表（按系部分别配置审批人）
CREATE TABLE task_approval_configs (
    id BIGINT PRIMARY KEY COMMENT '主键',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    department_id BIGINT NOT NULL COMMENT '系部ID',
    department_name VARCHAR(100) NOT NULL COMMENT '系部名称',

    approval_level TINYINT NOT NULL COMMENT '审批级别: 1-第一级, 2-第二级, 3-第三级...',

    approver_id BIGINT NOT NULL COMMENT '审批人ID',
    approver_name VARCHAR(50) NOT NULL COMMENT '审批人姓名',
    approver_role VARCHAR(50) COMMENT '审批人角色（如：系领导、学工处领导）',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    UNIQUE KEY uk_task_dept_level (task_id, department_id, approval_level),
    INDEX idx_task (task_id),
    INDEX idx_department (department_id),
    INDEX idx_approver (approver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务审批配置表';
