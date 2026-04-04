-- =====================================================
-- 教务管理表升级迁移
-- 1. 升级 courses 表（添加缺失列）
-- 2. 创建所有缺失的教务管理表
-- =====================================================

-- 1. 升级 courses 表：添加新列（IF NOT EXISTS 不支持 ALTER，用 IGNORE 方式）
-- 安全添加列（忽略已存在的列）
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'courses' AND COLUMN_NAME = 'course_name_en') = 0,
    'ALTER TABLE courses ADD COLUMN course_name_en VARCHAR(200) COMMENT ''课程英文名'' AFTER course_name',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'courses' AND COLUMN_NAME = 'course_category') = 0,
    'ALTER TABLE courses ADD COLUMN course_category TINYINT DEFAULT 1 COMMENT ''课程类别''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'courses' AND COLUMN_NAME = 'course_nature') = 0,
    'ALTER TABLE courses ADD COLUMN course_nature TINYINT DEFAULT 1 COMMENT ''课程属性''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'courses' AND COLUMN_NAME = 'credits') = 0,
    'ALTER TABLE courses ADD COLUMN credits DECIMAL(4,1) DEFAULT 0 COMMENT ''学分''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'courses' AND COLUMN_NAME = 'theory_hours') = 0,
    'ALTER TABLE courses ADD COLUMN theory_hours INT DEFAULT 0 COMMENT ''理论学时''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'courses' AND COLUMN_NAME = 'practice_hours') = 0,
    'ALTER TABLE courses ADD COLUMN practice_hours INT DEFAULT 0 COMMENT ''实践学时''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'courses' AND COLUMN_NAME = 'weekly_hours') = 0,
    'ALTER TABLE courses ADD COLUMN weekly_hours INT DEFAULT 2 COMMENT ''周学时''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'courses' AND COLUMN_NAME = 'exam_type') = 0,
    'ALTER TABLE courses ADD COLUMN exam_type TINYINT DEFAULT 1 COMMENT ''考核方式''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'courses' AND COLUMN_NAME = 'org_unit_id') = 0,
    'ALTER TABLE courses ADD COLUMN org_unit_id BIGINT COMMENT ''开课部门ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. 培养方案表
CREATE TABLE IF NOT EXISTS curriculum_plans (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    plan_code VARCHAR(50) NOT NULL COMMENT '方案代码',
    plan_name VARCHAR(100) NOT NULL COMMENT '方案名称',
    major_id BIGINT NOT NULL COMMENT '专业ID',
    grade_year INT NOT NULL COMMENT '适用年级（入学年份）',
    total_credits DECIMAL(5,1) COMMENT '毕业总学分要求',
    required_credits DECIMAL(5,1) COMMENT '必修学分要求',
    elective_credits DECIMAL(5,1) COMMENT '选修学分要求',
    practice_credits DECIMAL(5,1) COMMENT '实践学分要求',
    training_objective TEXT COMMENT '培养目标',
    graduation_requirement TEXT COMMENT '毕业要求',
    version INT DEFAULT 1 COMMENT '版本号',
    status TINYINT DEFAULT 0 COMMENT '状态：0草稿 1已发布 2已归档',
    published_at DATETIME COMMENT '发布时间',
    published_by BIGINT COMMENT '发布人',
    created_by BIGINT COMMENT '创建人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT COMMENT '更新人',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_plan_code (plan_code),
    INDEX idx_major_year (major_id, grade_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='培养方案表';

-- 3. 培养方案课程表
CREATE TABLE IF NOT EXISTS curriculum_plan_courses (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    plan_id BIGINT NOT NULL COMMENT '培养方案ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    semester_number TINYINT NOT NULL COMMENT '开课学期（第几学期，1-8）',
    course_category TINYINT COMMENT '课程类别',
    course_type TINYINT COMMENT '课程性质',
    credits DECIMAL(4,1) COMMENT '学分',
    total_hours INT COMMENT '总学时',
    weekly_hours INT COMMENT '周学时',
    theory_hours INT COMMENT '理论学时',
    practice_hours INT COMMENT '实践学时',
    exam_type TINYINT COMMENT '考核方式',
    sort_order INT DEFAULT 0 COMMENT '排序号',
    remark VARCHAR(200) COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_plan_course (plan_id, course_id),
    INDEX idx_plan_semester (plan_id, semester_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='培养方案课程表';

-- 4. 教学任务表
CREATE TABLE IF NOT EXISTS teaching_tasks (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    task_code VARCHAR(50) NOT NULL COMMENT '任务编号',
    semester_id BIGINT NOT NULL COMMENT '学期ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    class_id BIGINT NOT NULL COMMENT '教学班级ID',
    org_unit_id BIGINT COMMENT '开课部门ID',
    student_count INT DEFAULT 0 COMMENT '学生人数',
    weekly_hours INT NOT NULL COMMENT '周学时',
    total_hours INT NOT NULL COMMENT '总学时',
    start_week INT DEFAULT 1 COMMENT '起始周',
    end_week INT DEFAULT 16 COMMENT '结束周',
    scheduling_status TINYINT DEFAULT 0 COMMENT '排课状态：0未排 1部分排 2已排完',
    task_status TINYINT DEFAULT 1 COMMENT '任务状态：0草稿 1已确认 2已取消',
    remark VARCHAR(500) COMMENT '备注',
    created_by BIGINT COMMENT '创建人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT COMMENT '更新人',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_task_code (task_code),
    INDEX idx_semester (semester_id),
    INDEX idx_class (class_id),
    INDEX idx_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教学任务表';

-- 5. 教学任务教师分配表
CREATE TABLE IF NOT EXISTS teaching_task_teachers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '教学任务ID',
    teacher_id BIGINT NOT NULL COMMENT '教师ID',
    teacher_role TINYINT DEFAULT 1 COMMENT '教师角色：1主讲 2辅讲 3助教',
    workload_ratio DECIMAL(3,2) DEFAULT 1.00 COMMENT '工作量比例',
    remark VARCHAR(200) COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_task_teacher (task_id, teacher_id),
    INDEX idx_teacher (teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教学任务教师分配表';

-- 6. 课表条目表
CREATE TABLE IF NOT EXISTS schedule_entries (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    semester_id BIGINT NOT NULL COMMENT '学期ID',
    task_id BIGINT NOT NULL COMMENT '教学任务ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    class_id BIGINT NOT NULL COMMENT '班级ID',
    teacher_id BIGINT NOT NULL COMMENT '主讲教师ID',
    classroom_id BIGINT COMMENT '教室ID',
    weekday TINYINT NOT NULL COMMENT '周几（1-7）',
    start_slot INT NOT NULL COMMENT '开始节次',
    end_slot INT NOT NULL COMMENT '结束节次',
    start_week INT DEFAULT 1 COMMENT '起始周次',
    end_week INT DEFAULT 16 COMMENT '结束周次',
    week_type TINYINT DEFAULT 0 COMMENT '单双周：0每周 1单周 2双周',
    schedule_type TINYINT DEFAULT 1 COMMENT '课程类型：1正常 2实验 3实践',
    entry_status TINYINT DEFAULT 1 COMMENT '状态：1正常 2暂停 0删除',
    conflict_flag TINYINT DEFAULT 0 COMMENT '冲突标记',
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_semester (semester_id),
    INDEX idx_class (class_id),
    INDEX idx_teacher (teacher_id),
    INDEX idx_classroom (classroom_id),
    INDEX idx_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课表条目表';

-- 7. 调课申请表
CREATE TABLE IF NOT EXISTS schedule_adjustments (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    adjustment_code VARCHAR(50) NOT NULL COMMENT '调课单号',
    semester_id BIGINT NOT NULL COMMENT '学期ID',
    original_entry_id BIGINT NOT NULL COMMENT '原排课条目ID',
    adjustment_type TINYINT NOT NULL COMMENT '调整类型：1调课 2换教室 3代课 4停课',
    original_date DATE NOT NULL COMMENT '原上课日期',
    original_weekday TINYINT COMMENT '原周几',
    original_slot INT COMMENT '原节次',
    original_classroom_id BIGINT COMMENT '原教室ID',
    original_teacher_id BIGINT COMMENT '原教师ID',
    new_date DATE COMMENT '新上课日期',
    new_weekday TINYINT COMMENT '新周几',
    new_slot INT COMMENT '新节次',
    new_classroom_id BIGINT COMMENT '新教室ID',
    new_teacher_id BIGINT COMMENT '代课教师ID',
    applicant_id BIGINT NOT NULL COMMENT '申请人ID',
    apply_reason VARCHAR(500) NOT NULL COMMENT '申请原因',
    apply_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    approval_status TINYINT DEFAULT 0 COMMENT '审批状态：0待审批 1已通过 2已拒绝 3已撤回',
    approver_id BIGINT COMMENT '审批人ID',
    approval_time DATETIME COMMENT '审批时间',
    approval_comment VARCHAR(500) COMMENT '审批意见',
    executed TINYINT DEFAULT 0 COMMENT '是否已执行',
    executed_at DATETIME COMMENT '执行时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_adjustment_code (adjustment_code),
    INDEX idx_semester (semester_id),
    INDEX idx_applicant (applicant_id),
    INDEX idx_status (approval_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调课申请表';

-- 8. 考试批次表
CREATE TABLE IF NOT EXISTS exam_batches (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    batch_code VARCHAR(50) NOT NULL COMMENT '批次代码',
    batch_name VARCHAR(100) NOT NULL COMMENT '批次名称',
    semester_id BIGINT NOT NULL COMMENT '学期ID',
    exam_type TINYINT NOT NULL COMMENT '考试类型：1期中 2期末 3补考 4重修',
    start_date DATE NOT NULL COMMENT '考试开始日期',
    end_date DATE NOT NULL COMMENT '考试结束日期',
    status TINYINT DEFAULT 0 COMMENT '状态：0筹备中 1报名中 2已安排 3进行中 4已结束',
    remark VARCHAR(500) COMMENT '备注',
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_batch_code (batch_code),
    INDEX idx_semester (semester_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试批次表';

-- 9. 考试安排表
CREATE TABLE IF NOT EXISTS exam_arrangements (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    batch_id BIGINT NOT NULL COMMENT '考试批次ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    exam_date DATE NOT NULL COMMENT '考试日期',
    start_time TIME NOT NULL COMMENT '开始时间',
    end_time TIME NOT NULL COMMENT '结束时间',
    duration INT COMMENT '考试时长（分钟）',
    exam_form TINYINT DEFAULT 1 COMMENT '考试形式：1闭卷 2开卷 3机考 4口试 5实操',
    total_students INT DEFAULT 0 COMMENT '应考人数',
    remark VARCHAR(200),
    status TINYINT DEFAULT 1,
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_batch (batch_id),
    INDEX idx_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试安排表';

-- 10. 考场安排表
CREATE TABLE IF NOT EXISTS exam_rooms (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    arrangement_id BIGINT NOT NULL COMMENT '考试安排ID',
    classroom_id BIGINT NOT NULL COMMENT '教室ID',
    room_code VARCHAR(20) COMMENT '考场编号',
    seat_count INT NOT NULL COMMENT '座位数',
    student_count INT DEFAULT 0 COMMENT '安排考生数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_arrangement (arrangement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考场安排表';

-- 11. 监考安排表
CREATE TABLE IF NOT EXISTS exam_invigilators (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    room_id BIGINT NOT NULL COMMENT '考场ID',
    teacher_id BIGINT NOT NULL COMMENT '监考教师ID',
    invigilator_role TINYINT DEFAULT 1 COMMENT '监考角色：1主监考 2副监考 3巡考',
    status TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_room_teacher (room_id, teacher_id),
    INDEX idx_teacher (teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监考安排表';

-- 12. 成绩录入批次表
CREATE TABLE IF NOT EXISTS grade_batches (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    batch_code VARCHAR(50) NOT NULL COMMENT '批次代码',
    batch_name VARCHAR(100) NOT NULL COMMENT '批次名称',
    semester_id BIGINT NOT NULL COMMENT '学期ID',
    grade_type TINYINT NOT NULL COMMENT '成绩类型：1正常成绩 2补考成绩 3重修成绩',
    start_time DATETIME COMMENT '录入开始时间',
    end_time DATETIME COMMENT '录入截止时间',
    status TINYINT DEFAULT 0 COMMENT '状态：0未开始 1进行中 2已截止 3已归档',
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_batch_code (batch_code),
    INDEX idx_semester (semester_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成绩录入批次表';

-- 13. 学生成绩表
CREATE TABLE IF NOT EXISTS student_grades (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    batch_id BIGINT COMMENT '录入批次ID',
    semester_id BIGINT NOT NULL COMMENT '学期ID',
    task_id BIGINT COMMENT '教学任务ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    class_id BIGINT NOT NULL COMMENT '班级ID',
    total_score DECIMAL(5,1) COMMENT '总评成绩',
    grade_point DECIMAL(3,1) COMMENT '绩点',
    passed TINYINT COMMENT '是否通过',
    credits_earned DECIMAL(4,1) COMMENT '获得学分',
    grade_status TINYINT DEFAULT 0 COMMENT '状态：0未录入 1已录入 2已确认 3已发布',
    remark VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_semester (semester_id),
    INDEX idx_student (student_id),
    INDEX idx_course (course_id),
    INDEX idx_class (class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生成绩表';
