-- 预警规则表
CREATE TABLE IF NOT EXISTS academic_warning_rules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_name VARCHAR(100) NOT NULL COMMENT '规则名称',
    rule_type VARCHAR(30) NOT NULL COMMENT '规则类型: GRADE_FAIL/ATTENDANCE_LOW/CREDIT_SHORT/CUSTOM',
    warning_level TINYINT NOT NULL COMMENT '预警级别: 1黄色 2橙色 3红色',
    condition_params JSON NOT NULL COMMENT '条件参数',
    applicable_grades VARCHAR(100) COMMENT '适用年级（空=全部）',
    enabled TINYINT DEFAULT 1,
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_type (rule_type),
    INDEX idx_level (warning_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学业预警规则表';

-- 预警记录表
CREATE TABLE IF NOT EXISTS academic_warnings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL COMMENT '学生ID',
    student_no VARCHAR(50) COMMENT '学号',
    student_name VARCHAR(50) COMMENT '学生姓名',
    class_id BIGINT COMMENT '班级ID',
    class_name VARCHAR(100) COMMENT '班级名',
    rule_id BIGINT COMMENT '触发的规则ID',
    rule_name VARCHAR(100) COMMENT '规则名称',
    warning_type VARCHAR(30) NOT NULL COMMENT '预警类型',
    warning_level TINYINT NOT NULL COMMENT '预警级别: 1黄色 2橙色 3红色',
    description VARCHAR(500) NOT NULL COMMENT '预警描述',
    detail JSON COMMENT '详细数据（如挂科列表、出勤明细）',
    status TINYINT DEFAULT 0 COMMENT '0未处理 1已确认 2已干预 3已解除',
    handler_id BIGINT COMMENT '处理人',
    handle_note VARCHAR(500) COMMENT '处理备注',
    handled_at DATETIME COMMENT '处理时间',
    semester_id BIGINT COMMENT '学期ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_student (student_id),
    INDEX idx_class (class_id),
    INDEX idx_level (warning_level),
    INDEX idx_status (status),
    INDEX idx_semester (semester_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学业预警记录表';
