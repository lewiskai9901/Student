-- 招生管理模块
-- 招生计划 + 报名记录

-- 招生计划表（每年每个专业方向的招生指标）
CREATE TABLE IF NOT EXISTS enrollment_plans (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    academic_year INT NOT NULL COMMENT '招生年份',
    major_id BIGINT NOT NULL COMMENT '专业ID',
    major_direction_id BIGINT COMMENT '专业方向ID',
    org_unit_id BIGINT COMMENT '招收系部',
    planned_count INT NOT NULL COMMENT '计划招生人数',
    actual_count INT DEFAULT 0 COMMENT '实际录取人数',
    registered_count INT DEFAULT 0 COMMENT '已报到人数',
    enrollment_target VARCHAR(50) DEFAULT '初中毕业生' COMMENT '招生对象',
    status TINYINT DEFAULT 0 COMMENT '0草稿 1已发布 2招生中 3已结束',
    remark VARCHAR(500),
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_year (academic_year),
    INDEX idx_major (major_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='招生计划表';

-- 报名记录表
CREATE TABLE IF NOT EXISTS enrollment_applications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id BIGINT NOT NULL COMMENT '招生计划ID',
    academic_year INT NOT NULL COMMENT '招生年份',
    applicant_name VARCHAR(50) NOT NULL COMMENT '姓名',
    gender TINYINT COMMENT '1男 2女',
    id_card VARCHAR(18) COMMENT '身份证号',
    phone VARCHAR(20) COMMENT '联系电话',
    guardian_name VARCHAR(50) COMMENT '监护人姓名',
    guardian_phone VARCHAR(20) COMMENT '监护人电话',
    graduate_from VARCHAR(100) COMMENT '毕业学校',
    major_id BIGINT NOT NULL COMMENT '报考专业',
    major_direction_id BIGINT COMMENT '报考方向',
    application_date DATE COMMENT '报名日期',
    exam_score DECIMAL(5,1) COMMENT '入学考试成绩(如有)',
    status TINYINT DEFAULT 0 COMMENT '0待审核 1已录取 2未录取 3已报到 4已放弃',
    review_comment VARCHAR(200) COMMENT '审核意见',
    reviewer_id BIGINT COMMENT '审核人',
    reviewed_at DATETIME COMMENT '审核时间',
    registered_at DATETIME COMMENT '报到时间',
    assigned_class_id BIGINT COMMENT '分配班级ID',
    assigned_student_id BIGINT COMMENT '创建的学生记录ID',
    remark VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_plan (plan_id),
    INDEX idx_year (academic_year),
    INDEX idx_status (status),
    INDEX idx_id_card (id_card)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报名记录表';
