-- 教师档案表（扩展用户表的教学相关信息）
CREATE TABLE IF NOT EXISTS teacher_profiles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '关联用户ID',
    employee_no VARCHAR(50) COMMENT '工号',
    title VARCHAR(30) COMMENT '职称：教授/副教授/讲师/助教/实训指导',
    title_level VARCHAR(20) COMMENT '职称等级：正高/副高/中级/初级',
    org_unit_id BIGINT COMMENT '所属系部',
    teaching_group VARCHAR(100) COMMENT '所属教研室/教学组',
    max_weekly_hours INT DEFAULT 20 COMMENT '每周最大课时',
    qualification TEXT COMMENT '教学资质描述',
    specialties JSON COMMENT '擅长领域/可授课程类别(JSON数组)',
    hire_date DATE COMMENT '入职日期',
    status TINYINT DEFAULT 1 COMMENT '1在职 2离职 3退休',
    remark VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_user_id (user_id),
    INDEX idx_org_unit (org_unit_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师档案表';

-- 教师可授课程关联表
CREATE TABLE IF NOT EXISTS teacher_course_qualifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    teacher_profile_id BIGINT NOT NULL COMMENT '教师档案ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    qualification_level TINYINT DEFAULT 1 COMMENT '资质等级：1初级 2中级 3高级',
    remark VARCHAR(200),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_teacher_course (teacher_profile_id, course_id),
    INDEX idx_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师可授课程表';
