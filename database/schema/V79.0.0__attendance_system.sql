-- 考勤记录表
CREATE TABLE IF NOT EXISTS attendance_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    semester_id BIGINT NOT NULL COMMENT '学期ID',
    course_id BIGINT COMMENT '课程ID（课程考勤时）',
    class_id BIGINT NOT NULL COMMENT '班级ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    attendance_date DATE NOT NULL COMMENT '考勤日期',
    period INT COMMENT '节次（课程考勤时）',
    attendance_type TINYINT DEFAULT 1 COMMENT '1课程考勤 2日常考勤',
    status TINYINT NOT NULL COMMENT '1出勤 2迟到 3早退 4请假 5旷课',
    check_in_time DATETIME COMMENT '签到时间',
    check_method VARCHAR(20) DEFAULT 'MANUAL' COMMENT '签到方式: MANUAL/NFC/QR/FACE',
    remark VARCHAR(200) COMMENT '备注',
    recorded_by BIGINT COMMENT '记录人ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_student_date (student_id, attendance_date),
    INDEX idx_class_date (class_id, attendance_date),
    INDEX idx_course_date (course_id, attendance_date),
    INDEX idx_semester (semester_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤记录表';

-- 请假申请表
CREATE TABLE IF NOT EXISTS leave_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL COMMENT '学生ID',
    leave_type TINYINT NOT NULL COMMENT '1事假 2病假 3公假',
    start_date DATE NOT NULL COMMENT '开始日期',
    end_date DATE NOT NULL COMMENT '结束日期',
    start_period INT COMMENT '开始节次',
    end_period INT COMMENT '结束节次',
    reason VARCHAR(500) NOT NULL COMMENT '请假原因',
    attachment_urls JSON COMMENT '附件（病假条等）',
    approval_status TINYINT DEFAULT 0 COMMENT '0待审批 1已通过 2已拒绝',
    approver_id BIGINT COMMENT '审批人',
    approval_time DATETIME COMMENT '审批时间',
    approval_comment VARCHAR(200) COMMENT '审批意见',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_student (student_id),
    INDEX idx_date (start_date, end_date),
    INDEX idx_status (approval_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请假申请表';
