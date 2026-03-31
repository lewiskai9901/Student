-- V78.0.0: 学生学籍异动记录表
-- 记录学生每次状态变更的审计轨迹

CREATE TABLE IF NOT EXISTS student_status_changes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL COMMENT '学生ID',
    student_no VARCHAR(50) COMMENT '学号',
    student_name VARCHAR(50) COMMENT '学生姓名',
    change_type VARCHAR(30) NOT NULL COMMENT '异动类型: ENROLL/SUSPEND/RESUME/GRADUATE/WITHDRAW/EXPEL/TRANSFER_CLASS/TRANSFER_MAJOR',
    from_status VARCHAR(20) COMMENT '原状态',
    to_status VARCHAR(20) COMMENT '新状态',
    from_class_id BIGINT COMMENT '原班级ID（转班时）',
    from_class_name VARCHAR(100) COMMENT '原班级名',
    to_class_id BIGINT COMMENT '新班级ID（转班时）',
    to_class_name VARCHAR(100) COMMENT '新班级名',
    reason VARCHAR(500) COMMENT '异动原因',
    attachment_urls JSON COMMENT '附件URL',
    effective_date DATE COMMENT '生效日期',
    operator_id BIGINT COMMENT '操作人ID',
    operator_name VARCHAR(50) COMMENT '操作人姓名',
    remark VARCHAR(500) COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_student (student_id),
    INDEX idx_type (change_type),
    INDEX idx_date (effective_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生学籍异动记录表';
