-- =============================================
-- V20260127_3: Inspection V4 Phase 3 Tables
-- Corrective Action + Student Behavior
-- =============================================

-- 1. Corrective Actions (整改工单)
CREATE TABLE IF NOT EXISTS corrective_actions (
    id BIGINT PRIMARY KEY COMMENT '主键ID (雪花算法)',
    action_code VARCHAR(32) NOT NULL COMMENT '工单编号',
    title VARCHAR(200) NOT NULL COMMENT '工单标题',
    description TEXT COMMENT '问题描述',
    source VARCHAR(32) NOT NULL COMMENT '来源: INSPECTION, APPEAL, MANUAL',
    source_id BIGINT COMMENT '来源ID (检查记录ID/申诉ID)',
    severity VARCHAR(16) NOT NULL DEFAULT 'MINOR' COMMENT '严重程度: MINOR, MODERATE, SEVERE, CRITICAL',
    category VARCHAR(32) NOT NULL DEFAULT 'OTHER' COMMENT '分类: HYGIENE, DISCIPLINE, SAFETY, OTHER',
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN' COMMENT '状态: OPEN, IN_PROGRESS, REVIEW, CLOSED, OVERDUE, ESCALATED',
    class_id BIGINT COMMENT '关联班级ID',
    assignee_id BIGINT COMMENT '责任人ID',
    deadline DATETIME COMMENT '整改截止时间',
    resolution_note TEXT COMMENT '整改说明',
    resolution_attachments JSON COMMENT '整改附件JSON',
    resolved_at DATETIME COMMENT '整改完成时间',
    verifier_id BIGINT COMMENT '验证人ID',
    verification_result VARCHAR(16) COMMENT '验证结果: PASS, FAIL',
    verification_comment TEXT COMMENT '验证备注',
    verified_at DATETIME COMMENT '验证时间',
    escalation_level INT NOT NULL DEFAULT 0 COMMENT '升级层级 (0=未升级)',
    created_by BIGINT COMMENT '创建人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_action_code (action_code),
    INDEX idx_status (status),
    INDEX idx_class_id (class_id),
    INDEX idx_assignee_id (assignee_id),
    INDEX idx_source (source, source_id),
    INDEX idx_deadline (deadline),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='整改工单';

-- 2. Auto Action Rules (自动创建规则)
CREATE TABLE IF NOT EXISTS auto_action_rules (
    id BIGINT PRIMARY KEY COMMENT '主键ID (雪花算法)',
    rule_code VARCHAR(32) NOT NULL COMMENT '规则编号',
    rule_name VARCHAR(100) NOT NULL COMMENT '规则名称',
    trigger_type VARCHAR(32) NOT NULL COMMENT '触发类型: INSPECTION_PUBLISHED, SEVERITY_THRESHOLD, REPEAT_VIOLATION',
    trigger_condition JSON NOT NULL COMMENT '触发条件JSON',
    severity VARCHAR(16) NOT NULL DEFAULT 'MINOR' COMMENT '生成工单严重程度',
    category VARCHAR(32) NOT NULL DEFAULT 'OTHER' COMMENT '生成工单分类',
    deadline_hours INT NOT NULL DEFAULT 72 COMMENT '整改截止小时数',
    auto_assign TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否自动分配给班主任',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_rule_code (rule_code),
    INDEX idx_trigger_type (trigger_type),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='自动创建规则';

-- 3. Student Behavior Records (学生行为记录)
CREATE TABLE IF NOT EXISTS student_behavior_records (
    id BIGINT PRIMARY KEY COMMENT '主键ID (雪花算法)',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    class_id BIGINT NOT NULL COMMENT '班级ID',
    behavior_type VARCHAR(16) NOT NULL COMMENT '行为类型: VIOLATION, COMMENDATION',
    source VARCHAR(32) NOT NULL COMMENT '来源: INSPECTION, TEACHER_REPORT, SELF_REPORT',
    source_id BIGINT COMMENT '来源ID',
    category VARCHAR(32) NOT NULL DEFAULT 'OTHER' COMMENT '分类: HYGIENE, DISCIPLINE, SAFETY, ATTENDANCE, ACADEMIC, OTHER',
    title VARCHAR(200) NOT NULL COMMENT '行为描述标题',
    detail TEXT COMMENT '详细描述',
    deduction_amount DECIMAL(10,2) DEFAULT 0 COMMENT '扣分/加分数值',
    status VARCHAR(32) NOT NULL DEFAULT 'RECORDED' COMMENT '状态: RECORDED, NOTIFIED, ACKNOWLEDGED, RESOLVED',
    recorded_by BIGINT COMMENT '记录人ID',
    recorded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    notified_at DATETIME COMMENT '通知时间',
    acknowledged_at DATETIME COMMENT '确认时间',
    resolved_at DATETIME COMMENT '处理完成时间',
    resolution_note TEXT COMMENT '处理说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_student_id (student_id),
    INDEX idx_class_id (class_id),
    INDEX idx_behavior_type (behavior_type),
    INDEX idx_status (status),
    INDEX idx_recorded_at (recorded_at),
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生行为记录';

-- 4. Student Behavior Alerts (学生行为预警)
CREATE TABLE IF NOT EXISTS student_behavior_alerts (
    id BIGINT PRIMARY KEY COMMENT '主键ID (雪花算法)',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    class_id BIGINT NOT NULL COMMENT '班级ID',
    alert_type VARCHAR(32) NOT NULL COMMENT '预警类型: FREQUENCY, SEVERITY, TREND',
    alert_level VARCHAR(16) NOT NULL DEFAULT 'WARNING' COMMENT '预警级别: INFO, WARNING, DANGER',
    title VARCHAR(200) NOT NULL COMMENT '预警标题',
    description TEXT COMMENT '预警描述',
    trigger_data JSON COMMENT '触发数据JSON',
    is_read TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已读',
    is_handled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已处理',
    handled_by BIGINT COMMENT '处理人ID',
    handled_at DATETIME COMMENT '处理时间',
    handle_note TEXT COMMENT '处理备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_student_id (student_id),
    INDEX idx_class_id (class_id),
    INDEX idx_alert_type (alert_type),
    INDEX idx_is_handled (is_handled),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生行为预警';
