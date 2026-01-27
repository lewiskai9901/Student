-- =====================================================
-- 教务管理系统数据库迁移脚本
-- 版本: V20260121_1
-- 日期: 2026-01-21
-- 描述: 创建教务管理系统所需的全部数据表
-- =====================================================

-- =====================================================
-- 一、校历管理模块
-- =====================================================

-- 1.1 学年表
CREATE TABLE IF NOT EXISTS academic_years (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    year_code VARCHAR(20) NOT NULL COMMENT '学年代码，如2025-2026',
    year_name VARCHAR(50) NOT NULL COMMENT '学年名称',
    start_date DATE NOT NULL COMMENT '学年开始日期',
    end_date DATE NOT NULL COMMENT '学年结束日期',
    is_current TINYINT DEFAULT 0 COMMENT '是否当前学年',
    status TINYINT DEFAULT 1 COMMENT '状态：1正常 0停用',
    created_by BIGINT COMMENT '创建人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT COMMENT '更新人',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_year_code (year_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学年表';

-- 1.2 学期表
CREATE TABLE IF NOT EXISTS semesters (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    academic_year_id BIGINT NOT NULL COMMENT '所属学年ID',
    semester_code VARCHAR(30) NOT NULL COMMENT '学期代码，如2025-2026-1',
    semester_name VARCHAR(50) NOT NULL COMMENT '学期名称',
    semester_type TINYINT NOT NULL COMMENT '学期类型：1秋季/第一学期 2春季/第二学期',
    start_date DATE NOT NULL COMMENT '学期开始日期',
    end_date DATE NOT NULL COMMENT '学期结束日期',
    teaching_start_date DATE COMMENT '教学开始日期',
    teaching_end_date DATE COMMENT '教学结束日期',
    exam_start_date DATE COMMENT '考试周开始日期',
    exam_end_date DATE COMMENT '考试周结束日期',
    total_teaching_weeks INT DEFAULT 18 COMMENT '教学周数',
    is_current TINYINT DEFAULT 0 COMMENT '是否当前学期',
    status TINYINT DEFAULT 1 COMMENT '状态：1正常 0停用',
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_semester_code (semester_code),
    INDEX idx_academic_year (academic_year_id),
    INDEX idx_is_current (is_current)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学期表';

-- 1.3 教学周表
CREATE TABLE IF NOT EXISTS teaching_weeks (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    semester_id BIGINT NOT NULL COMMENT '所属学期ID',
    week_number INT NOT NULL COMMENT '周次（第几周）',
    start_date DATE NOT NULL COMMENT '本周开始日期（周一）',
    end_date DATE NOT NULL COMMENT '本周结束日期（周日）',
    week_type TINYINT DEFAULT 1 COMMENT '周类型：1正常教学周 2考试周 3假期周 4实践周',
    week_label VARCHAR(50) COMMENT '周标签，如"国庆假期"',
    is_active TINYINT DEFAULT 1 COMMENT '是否有效（停课则为0）',
    remark VARCHAR(200) COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_semester_week (semester_id, week_number),
    INDEX idx_date_range (start_date, end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教学周表';

-- 1.4 校历事件表
CREATE TABLE IF NOT EXISTS school_events (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    semester_id BIGINT COMMENT '所属学期ID（跨学期事件可为空）',
    event_code VARCHAR(50) COMMENT '事件代码',
    event_name VARCHAR(100) NOT NULL COMMENT '事件名称',
    event_type TINYINT NOT NULL COMMENT '事件类型：1法定节假日 2学校假期 3校级活动 4调休补课 5临时停课',
    start_date DATE NOT NULL COMMENT '开始日期',
    end_date DATE NOT NULL COMMENT '结束日期',
    start_time TIME COMMENT '开始时间（精确到时间的事件）',
    end_time TIME COMMENT '结束时间',
    all_day TINYINT DEFAULT 1 COMMENT '是否全天事件',
    affect_schedule TINYINT DEFAULT 0 COMMENT '是否影响正常排课：1影响（停课）0不影响',
    affected_org_units JSON COMMENT '影响的组织单元ID列表（空表示全校）',
    swap_to_date DATE COMMENT '调休：调到哪天补课',
    swap_weekday TINYINT COMMENT '调休：按周几的课表上课（1-7）',
    color VARCHAR(20) DEFAULT '#1890ff' COMMENT '日历显示颜色',
    priority INT DEFAULT 0 COMMENT '显示优先级',
    description TEXT COMMENT '详细描述',
    attachment_urls JSON COMMENT '附件URL列表',
    status TINYINT DEFAULT 1 COMMENT '状态：1正常 0取消',
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_semester (semester_id),
    INDEX idx_date_range (start_date, end_date),
    INDEX idx_event_type (event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='校历事件表';

-- 1.5 作息时间配置表
CREATE TABLE IF NOT EXISTS class_time_configs (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    config_name VARCHAR(50) NOT NULL COMMENT '配置名称，如"标准作息"',
    semester_id BIGINT COMMENT '适用学期（空表示通用）',
    time_slot INT NOT NULL COMMENT '节次（第几节课）',
    slot_name VARCHAR(20) COMMENT '节次名称，如"第一节"',
    start_time TIME NOT NULL COMMENT '上课时间',
    end_time TIME NOT NULL COMMENT '下课时间',
    slot_type TINYINT DEFAULT 1 COMMENT '类型：1上午 2下午 3晚上',
    is_default TINYINT DEFAULT 0 COMMENT '是否默认配置',
    status TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_config_slot (config_name, time_slot),
    INDEX idx_semester (semester_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作息时间配置表';

-- =====================================================
-- 二、课程与培养方案模块
-- =====================================================

-- 2.1 课程表（扩展字段，如已存在则执行ALTER）
-- 检查courses表是否存在，不存在则创建
CREATE TABLE IF NOT EXISTS courses (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    course_code VARCHAR(50) NOT NULL COMMENT '课程代码',
    course_name VARCHAR(100) NOT NULL COMMENT '课程名称',
    course_name_en VARCHAR(200) COMMENT '课程英文名',
    course_category TINYINT DEFAULT 1 COMMENT '课程类别：1公共基础课 2专业基础课 3专业核心课 4专业选修课 5通识选修课 6实践课',
    course_type TINYINT DEFAULT 1 COMMENT '课程性质：1必修 2限选 3任选',
    course_nature TINYINT DEFAULT 1 COMMENT '课程属性：1理论课 2实验课 3理论+实验 4实践课',
    credits DECIMAL(4,1) DEFAULT 0 COMMENT '学分',
    total_hours INT DEFAULT 0 COMMENT '总学时',
    theory_hours INT DEFAULT 0 COMMENT '理论学时',
    practice_hours INT DEFAULT 0 COMMENT '实践/实验学时',
    weekly_hours INT DEFAULT 2 COMMENT '周学时',
    exam_type TINYINT DEFAULT 1 COMMENT '考核方式：1考试 2考查',
    grade_scale_type TINYINT DEFAULT 1 COMMENT '评分制：1百分制 2五级制 3二级制（通过/不通过）',
    org_unit_id BIGINT COMMENT '开课部门ID',
    prerequisite_ids JSON COMMENT '先修课程ID数组',
    description TEXT COMMENT '课程简介',
    syllabus_url VARCHAR(500) COMMENT '教学大纲URL',
    status TINYINT DEFAULT 1 COMMENT '状态：1启用 0停用',
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_course_code (course_code),
    INDEX idx_category (course_category),
    INDEX idx_org_unit (org_unit_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

-- 2.2 培养方案表
CREATE TABLE IF NOT EXISTS curriculum_plans (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    plan_code VARCHAR(50) NOT NULL COMMENT '方案代码',
    plan_name VARCHAR(100) NOT NULL COMMENT '方案名称，如"2024级计算机应用技术专业培养方案"',
    major_id BIGINT NOT NULL COMMENT '专业ID',
    grade_year INT NOT NULL COMMENT '适用年级（入学年份）',
    education_level TINYINT DEFAULT 1 COMMENT '培养层次：1中专 2大专 3本科',
    education_length TINYINT DEFAULT 3 COMMENT '学制（年）',
    total_credits DECIMAL(5,1) COMMENT '毕业总学分要求',
    required_credits DECIMAL(5,1) COMMENT '必修学分要求',
    elective_credits DECIMAL(5,1) COMMENT '选修学分要求',
    practice_credits DECIMAL(5,1) COMMENT '实践学分要求',
    degree_type VARCHAR(50) COMMENT '授予学位类型',
    training_objective TEXT COMMENT '培养目标',
    graduation_requirement TEXT COMMENT '毕业要求',
    version INT DEFAULT 1 COMMENT '版本号',
    status TINYINT DEFAULT 1 COMMENT '状态：0草稿 1已发布 2已归档',
    published_at DATETIME COMMENT '发布时间',
    published_by BIGINT COMMENT '发布人',
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_plan_code (plan_code),
    INDEX idx_major_year (major_id, grade_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='培养方案表';

-- 2.3 培养方案课程表
CREATE TABLE IF NOT EXISTS curriculum_plan_courses (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    plan_id BIGINT NOT NULL COMMENT '培养方案ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    semester_number TINYINT NOT NULL COMMENT '开课学期（第几学期，1-8）',
    course_category TINYINT COMMENT '在本方案中的课程类别（覆盖课程表默认值）',
    course_type TINYINT COMMENT '在本方案中的课程性质（必修/选修）',
    credits DECIMAL(4,1) COMMENT '学分（覆盖课程表默认值）',
    total_hours INT COMMENT '总学时',
    weekly_hours INT COMMENT '周学时',
    theory_hours INT COMMENT '理论学时',
    practice_hours INT COMMENT '实践学时',
    exam_type TINYINT COMMENT '考核方式',
    is_degree_course TINYINT DEFAULT 0 COMMENT '是否学位课程',
    sort_order INT DEFAULT 0 COMMENT '排序号',
    remark VARCHAR(200) COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_plan_course (plan_id, course_id),
    INDEX idx_plan_semester (plan_id, semester_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='培养方案课程表';

-- =====================================================
-- 三、教学任务模块
-- =====================================================

-- 3.1 教学任务表
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
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_task_code (task_code),
    INDEX idx_semester (semester_id),
    INDEX idx_class (class_id),
    INDEX idx_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教学任务表';

-- 3.2 教学任务教师分配表
CREATE TABLE IF NOT EXISTS teaching_task_teachers (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '教学任务ID',
    teacher_id BIGINT NOT NULL COMMENT '教师ID',
    teacher_role TINYINT DEFAULT 1 COMMENT '教师角色：1主讲 2辅讲 3助教',
    workload_ratio DECIMAL(3,2) DEFAULT 1.00 COMMENT '工作量比例',
    remark VARCHAR(200) COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_task_teacher (task_id, teacher_id),
    INDEX idx_teacher (teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教学任务教师分配表';

-- 3.3 教师偏好设置表
CREATE TABLE IF NOT EXISTS teacher_preferences (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    teacher_id BIGINT NOT NULL COMMENT '教师ID',
    semester_id BIGINT NOT NULL COMMENT '学期ID',
    preference_type TINYINT NOT NULL COMMENT '偏好类型：1不可用时间 2偏好时间 3偏好教室',
    weekday TINYINT COMMENT '周几（1-7）',
    time_slot INT COMMENT '节次',
    classroom_id BIGINT COMMENT '偏好教室ID',
    priority INT DEFAULT 0 COMMENT '优先级（数字越大越优先）',
    reason VARCHAR(200) COMMENT '原因说明',
    status TINYINT DEFAULT 1 COMMENT '状态',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_teacher_semester (teacher_id, semester_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师偏好设置表';

-- =====================================================
-- 四、排课管理模块
-- =====================================================

-- 4.1 课表条目表（排课结果）
CREATE TABLE IF NOT EXISTS schedule_entries (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    semester_id BIGINT NOT NULL COMMENT '学期ID',
    task_id BIGINT NOT NULL COMMENT '教学任务ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    class_id BIGINT NOT NULL COMMENT '班级ID',
    teacher_id BIGINT NOT NULL COMMENT '主讲教师ID',
    classroom_id BIGINT COMMENT '教室ID',
    weekday TINYINT NOT NULL COMMENT '周几（1-7，1=周一）',
    start_slot INT NOT NULL COMMENT '开始节次',
    end_slot INT NOT NULL COMMENT '结束节次（支持连堂）',
    start_week INT DEFAULT 1 COMMENT '起始周次',
    end_week INT DEFAULT 16 COMMENT '结束周次',
    week_type TINYINT DEFAULT 0 COMMENT '单双周：0每周 1单周 2双周',
    schedule_type TINYINT DEFAULT 1 COMMENT '课程类型：1正常 2实验 3实践',
    entry_status TINYINT DEFAULT 1 COMMENT '状态：1正常 2暂停 0删除',
    conflict_flag TINYINT DEFAULT 0 COMMENT '冲突标记：0无冲突 1有冲突（强制保存）',
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_semester (semester_id),
    INDEX idx_class (class_id),
    INDEX idx_teacher (teacher_id),
    INDEX idx_classroom (classroom_id),
    INDEX idx_time (weekday, start_slot),
    INDEX idx_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课表条目表';

-- 4.2 排课冲突记录表
CREATE TABLE IF NOT EXISTS schedule_conflicts (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    semester_id BIGINT NOT NULL COMMENT '学期ID',
    entry_id BIGINT NOT NULL COMMENT '排课条目ID',
    conflict_entry_id BIGINT COMMENT '冲突的另一条目ID',
    conflict_type TINYINT NOT NULL COMMENT '冲突类型：1教师冲突 2教室冲突 3班级冲突',
    conflict_detail VARCHAR(500) COMMENT '冲突详情描述',
    resolved TINYINT DEFAULT 0 COMMENT '是否已解决',
    resolved_by BIGINT COMMENT '解决人',
    resolved_at DATETIME COMMENT '解决时间',
    resolution_note VARCHAR(500) COMMENT '解决说明',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_entry (entry_id),
    INDEX idx_semester (semester_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排课冲突记录表';

-- 4.3 调课申请表
CREATE TABLE IF NOT EXISTS schedule_adjustments (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    adjustment_code VARCHAR(50) NOT NULL COMMENT '调课单号',
    semester_id BIGINT NOT NULL COMMENT '学期ID',
    original_entry_id BIGINT NOT NULL COMMENT '原排课条目ID',
    adjustment_type TINYINT NOT NULL COMMENT '调整类型：1调课（换时间）2换教室 3代课（换教师）4停课',
    original_date DATE NOT NULL COMMENT '原上课日期',
    original_weekday TINYINT COMMENT '原周几',
    original_slot INT COMMENT '原节次',
    original_classroom_id BIGINT COMMENT '原教室ID',
    original_teacher_id BIGINT COMMENT '原教师ID',
    new_date DATE COMMENT '新上课日期（调课时填）',
    new_weekday TINYINT COMMENT '新周几',
    new_slot INT COMMENT '新节次',
    new_classroom_id BIGINT COMMENT '新教室ID',
    new_teacher_id BIGINT COMMENT '代课教师ID',
    applicant_id BIGINT NOT NULL COMMENT '申请人ID',
    apply_reason VARCHAR(500) NOT NULL COMMENT '申请原因',
    apply_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    attachment_urls JSON COMMENT '附件（如请假条）',
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
    INDEX idx_original_entry (original_entry_id),
    INDEX idx_applicant (applicant_id),
    INDEX idx_status (approval_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调课申请表';

-- =====================================================
-- 五、考试管理模块
-- =====================================================

-- 5.1 考试批次表
CREATE TABLE IF NOT EXISTS exam_batches (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    batch_code VARCHAR(50) NOT NULL COMMENT '批次代码',
    batch_name VARCHAR(100) NOT NULL COMMENT '批次名称，如"2025-2026-1期末考试"',
    semester_id BIGINT NOT NULL COMMENT '学期ID',
    exam_type TINYINT NOT NULL COMMENT '考试类型：1期中 2期末 3补考 4重修',
    start_date DATE NOT NULL COMMENT '考试开始日期',
    end_date DATE NOT NULL COMMENT '考试结束日期',
    registration_deadline DATETIME COMMENT '报名截止时间（补考/重修用）',
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

-- 5.2 考试安排表
CREATE TABLE IF NOT EXISTS exam_arrangements (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    batch_id BIGINT NOT NULL COMMENT '考试批次ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    exam_date DATE NOT NULL COMMENT '考试日期',
    start_time TIME NOT NULL COMMENT '开始时间',
    end_time TIME NOT NULL COMMENT '结束时间',
    duration INT COMMENT '考试时长（分钟）',
    exam_form TINYINT DEFAULT 1 COMMENT '考试形式：1闭卷 2开卷 3机考 4口试 5实操',
    total_students INT DEFAULT 0 COMMENT '应考人数',
    remark VARCHAR(200) COMMENT '备注',
    status TINYINT DEFAULT 1 COMMENT '状态：1正常 0取消',
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_batch (batch_id),
    INDEX idx_course (course_id),
    INDEX idx_date (exam_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试安排表';

-- 5.3 考场安排表
CREATE TABLE IF NOT EXISTS exam_rooms (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    arrangement_id BIGINT NOT NULL COMMENT '考试安排ID',
    classroom_id BIGINT NOT NULL COMMENT '教室ID',
    room_code VARCHAR(20) COMMENT '考场编号',
    seat_count INT NOT NULL COMMENT '座位数',
    student_count INT DEFAULT 0 COMMENT '安排考生数',
    seat_layout JSON COMMENT '座位安排（学生ID与座位号映射）',
    remark VARCHAR(200) COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_arrangement (arrangement_id),
    INDEX idx_classroom (classroom_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考场安排表';

-- 5.4 考生安排表
CREATE TABLE IF NOT EXISTS exam_students (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    arrangement_id BIGINT NOT NULL COMMENT '考试安排ID',
    room_id BIGINT COMMENT '考场ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    seat_number VARCHAR(20) COMMENT '座位号',
    exam_status TINYINT DEFAULT 0 COMMENT '考试状态：0待考 1已考 2缺考 3作弊',
    remark VARCHAR(200) COMMENT '备注（如缺考原因）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_arrangement_student (arrangement_id, student_id),
    INDEX idx_student (student_id),
    INDEX idx_room (room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考生安排表';

-- 5.5 监考安排表
CREATE TABLE IF NOT EXISTS exam_invigilators (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    room_id BIGINT NOT NULL COMMENT '考场ID',
    teacher_id BIGINT NOT NULL COMMENT '监考教师ID',
    invigilator_role TINYINT DEFAULT 1 COMMENT '监考角色：1主监考 2副监考 3巡考',
    status TINYINT DEFAULT 1 COMMENT '状态：1正常 0取消',
    remark VARCHAR(200) COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_room_teacher (room_id, teacher_id),
    INDEX idx_teacher (teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监考安排表';

-- =====================================================
-- 六、成绩管理模块
-- =====================================================

-- 6.1 成绩录入批次表
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

-- 6.2 成绩组成配置表
CREATE TABLE IF NOT EXISTS grade_compositions (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '教学任务ID',
    item_name VARCHAR(50) NOT NULL COMMENT '成绩项名称，如"平时成绩"',
    item_type TINYINT NOT NULL COMMENT '成绩项类型：1平时 2期中 3期末 4实验 5实践 6其他',
    weight DECIMAL(5,2) NOT NULL COMMENT '权重（百分比）',
    full_score DECIMAL(5,1) DEFAULT 100 COMMENT '满分',
    is_required TINYINT DEFAULT 1 COMMENT '是否必录',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成绩组成配置表';

-- 6.3 学生成绩表
CREATE TABLE IF NOT EXISTS student_grades (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    batch_id BIGINT COMMENT '录入批次ID',
    semester_id BIGINT NOT NULL COMMENT '学期ID',
    task_id BIGINT NOT NULL COMMENT '教学任务ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    class_id BIGINT NOT NULL COMMENT '班级ID',
    grade_scale_type TINYINT DEFAULT 1 COMMENT '评分制：1百分制 2五级制 3二级制',
    regular_score DECIMAL(5,1) COMMENT '平时成绩',
    midterm_score DECIMAL(5,1) COMMENT '期中成绩',
    final_score DECIMAL(5,1) COMMENT '期末成绩',
    experiment_score DECIMAL(5,1) COMMENT '实验成绩',
    total_score DECIMAL(5,1) COMMENT '总评成绩',
    grade_level VARCHAR(10) COMMENT '等级（五级制：A/B/C/D/F）',
    grade_point DECIMAL(3,1) COMMENT '绩点',
    passed TINYINT COMMENT '是否通过：1是 0否',
    credits_earned DECIMAL(4,1) COMMENT '获得学分',
    grade_status TINYINT DEFAULT 0 COMMENT '状态：0未录入 1已录入 2已确认 3已发布 4有异议',
    is_makeup TINYINT DEFAULT 0 COMMENT '是否补考成绩',
    is_retake TINYINT DEFAULT 0 COMMENT '是否重修成绩',
    makeup_count INT DEFAULT 0 COMMENT '补考次数',
    input_teacher_id BIGINT COMMENT '录入教师ID',
    input_time DATETIME COMMENT '录入时间',
    confirm_time DATETIME COMMENT '确认时间',
    publish_time DATETIME COMMENT '发布时间',
    remark VARCHAR(500) COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_task_student (task_id, student_id),
    INDEX idx_semester (semester_id),
    INDEX idx_student (student_id),
    INDEX idx_course (course_id),
    INDEX idx_class (class_id),
    INDEX idx_status (grade_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生成绩表';

-- 6.4 成绩明细表
CREATE TABLE IF NOT EXISTS student_grade_items (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    grade_id BIGINT NOT NULL COMMENT '学生成绩ID',
    composition_id BIGINT NOT NULL COMMENT '成绩组成配置ID',
    item_name VARCHAR(50) COMMENT '成绩项名称',
    score DECIMAL(5,1) COMMENT '得分',
    full_score DECIMAL(5,1) COMMENT '满分',
    weight DECIMAL(5,2) COMMENT '权重',
    weighted_score DECIMAL(5,2) COMMENT '加权得分',
    remark VARCHAR(200) COMMENT '备注',
    input_time DATETIME COMMENT '录入时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_grade (grade_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成绩明细表';

-- 6.5 成绩修改记录表
CREATE TABLE IF NOT EXISTS grade_change_logs (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    grade_id BIGINT NOT NULL COMMENT '学生成绩ID',
    change_type TINYINT NOT NULL COMMENT '修改类型：1首次录入 2修改 3删除 4恢复',
    field_name VARCHAR(50) COMMENT '修改字段',
    old_value VARCHAR(100) COMMENT '原值',
    new_value VARCHAR(100) COMMENT '新值',
    change_reason VARCHAR(500) COMMENT '修改原因',
    operator_id BIGINT NOT NULL COMMENT '操作人ID',
    operated_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX idx_grade (grade_id),
    INDEX idx_operator (operator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成绩修改记录表';

-- =====================================================
-- 七、视图创建
-- =====================================================

-- 7.1 课表详情视图
CREATE OR REPLACE VIEW v_schedule_detail AS
SELECT
    se.id,
    se.semester_id,
    sm.semester_name,
    se.task_id,
    se.course_id,
    c.course_code,
    c.course_name,
    se.class_id,
    cls.name AS class_name,
    se.teacher_id,
    u.real_name AS teacher_name,
    se.classroom_id,
    cr.name AS classroom_name,
    se.weekday,
    se.start_slot,
    se.end_slot,
    se.start_week,
    se.end_week,
    se.week_type,
    se.entry_status
FROM schedule_entries se
LEFT JOIN semesters sm ON se.semester_id = sm.id
LEFT JOIN courses c ON se.course_id = c.id
LEFT JOIN classes cls ON se.class_id = cls.id
LEFT JOIN users u ON se.teacher_id = u.id
LEFT JOIN classrooms cr ON se.classroom_id = cr.id
WHERE se.deleted = 0;

-- 7.2 学生成绩详情视图
CREATE OR REPLACE VIEW v_student_grade_detail AS
SELECT
    sg.id,
    sg.semester_id,
    sm.semester_name,
    sg.course_id,
    c.course_code,
    c.course_name,
    c.credits,
    sg.student_id,
    s.student_no,
    s.name AS student_name,
    sg.class_id,
    cls.name AS class_name,
    sg.regular_score,
    sg.midterm_score,
    sg.final_score,
    sg.total_score,
    sg.grade_level,
    sg.grade_point,
    sg.passed,
    sg.credits_earned,
    sg.grade_status,
    sg.is_makeup,
    sg.is_retake
FROM student_grades sg
LEFT JOIN semesters sm ON sg.semester_id = sm.id
LEFT JOIN courses c ON sg.course_id = c.id
LEFT JOIN students s ON sg.student_id = s.id
LEFT JOIN classes cls ON sg.class_id = cls.id
WHERE sg.deleted = 0;

-- =====================================================
-- 八、初始数据
-- =====================================================

-- 8.1 默认作息时间配置
INSERT INTO class_time_configs (id, config_name, time_slot, slot_name, start_time, end_time, slot_type, is_default) VALUES
(1, '标准作息', 1, '第一节', '08:00:00', '08:45:00', 1, 1),
(2, '标准作息', 2, '第二节', '08:55:00', '09:40:00', 1, 1),
(3, '标准作息', 3, '第三节', '10:00:00', '10:45:00', 1, 1),
(4, '标准作息', 4, '第四节', '10:55:00', '11:40:00', 1, 1),
(5, '标准作息', 5, '第五节', '14:00:00', '14:45:00', 2, 1),
(6, '标准作息', 6, '第六节', '14:55:00', '15:40:00', 2, 1),
(7, '标准作息', 7, '第七节', '16:00:00', '16:45:00', 2, 1),
(8, '标准作息', 8, '第八节', '16:55:00', '17:40:00', 2, 1),
(9, '标准作息', 9, '第九节', '19:00:00', '19:45:00', 3, 1),
(10, '标准作息', 10, '第十节', '19:55:00', '20:40:00', 3, 1)
ON DUPLICATE KEY UPDATE config_name = config_name;

-- =====================================================
-- 完成
-- =====================================================
