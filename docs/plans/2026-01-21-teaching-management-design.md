# 教务管理系统设计方案

> 版本: 1.0
> 日期: 2026-01-21
> 作者: Claude AI Assistant

## 一、概述

### 1.1 项目背景

在现有学生管理系统基础上，扩展教务管理功能，实现完整的教学管理闭环。

### 1.2 功能范围

| 模块 | 功能描述 | 优先级 |
|-----|---------|--------|
| 校历管理 | 学年学期、教学周、节假日、校级活动、调休联动 | P0 |
| 培养方案 | 专业教学计划、学期课程安排、学分要求 | P0 |
| 课程管理 | 课程库、课程分类、先修要求 | P0 |
| 教学任务 | 学期开课计划、教师任务分配 | P0 |
| 智能排课 | 自动排课算法、冲突检测、手动调整 | P0 |
| 调课管理 | 临时调课、代课、停课申请与审批 | P1 |
| 考试管理 | 考试安排、考场分配、监考安排 | P1 |
| 成绩管理 | 成绩录入、多评分制、成绩分析、补考重修 | P0 |

### 1.3 角色与权限

| 角色 | 权限范围 |
|-----|---------|
| 教务管理员 | 全部功能：校历、培养方案、排课、考试安排、成绩审核 |
| 院系管理员 | 本院系：教学任务分配、排课调整、成绩查看 |
| 班主任 | 本班级：查看课表、成绩查看、调课申请 |
| 任课教师 | 本人课程：成绩录入、调课申请、查看课表 |
| 学生 | 查看：个人课表、个人成绩 |

---

## 二、系统架构

### 2.1 领域模型设计（DDD）

```
domain/
└── teaching/                           # 教学领域
    ├── model/
    │   ├── aggregate/
    │   │   ├── AcademicYear.java       # 学年聚合根
    │   │   ├── Semester.java           # 学期聚合根
    │   │   ├── CurriculumPlan.java     # 培养方案聚合根
    │   │   ├── TeachingTask.java       # 教学任务聚合根
    │   │   ├── CourseSchedule.java     # 课表聚合根
    │   │   ├── Examination.java        # 考试聚合根
    │   │   └── StudentGrade.java       # 学生成绩聚合根
    │   ├── entity/
    │   │   ├── TeachingWeek.java       # 教学周
    │   │   ├── SchoolEvent.java        # 校历事件
    │   │   ├── PlanCourse.java         # 培养方案课程
    │   │   ├── TaskAssignment.java     # 任务分配
    │   │   ├── ScheduleEntry.java      # 排课条目
    │   │   ├── ScheduleAdjustment.java # 调课记录
    │   │   ├── ExamArrangement.java    # 考试安排
    │   │   ├── ExamInvigilator.java    # 监考安排
    │   │   └── GradeItem.java          # 成绩项
    │   └── valueobject/
    │       ├── TimeSlot.java           # 时间段(周几+节次)
    │       ├── WeekRange.java          # 周次范围(1-16周)
    │       ├── GradeScale.java         # 评分制
    │       ├── GradeWeight.java        # 成绩权重配置
    │       └── ScheduleConflict.java   # 排课冲突
    ├── repository/
    │   ├── AcademicYearRepository.java
    │   ├── SemesterRepository.java
    │   ├── CurriculumPlanRepository.java
    │   ├── TeachingTaskRepository.java
    │   ├── CourseScheduleRepository.java
    │   ├── ExaminationRepository.java
    │   └── StudentGradeRepository.java
    ├── service/
    │   ├── SchedulingDomainService.java    # 排课领域服务
    │   ├── ConflictDetectionService.java   # 冲突检测服务
    │   └── GradeCalculationService.java    # 成绩计算服务
    └── event/
        ├── SchedulePublishedEvent.java     # 课表发布事件
        ├── ScheduleAdjustedEvent.java      # 调课事件
        ├── GradeRecordedEvent.java         # 成绩录入事件
        └── ExamArrangedEvent.java          # 考试安排事件
```

### 2.2 应用服务层

```
application/
└── teaching/
    ├── AcademicCalendarApplicationService.java   # 校历应用服务
    ├── CurriculumPlanApplicationService.java     # 培养方案应用服务
    ├── TeachingTaskApplicationService.java       # 教学任务应用服务
    ├── SchedulingApplicationService.java         # 排课应用服务
    ├── ScheduleAdjustmentApplicationService.java # 调课应用服务
    ├── ExaminationApplicationService.java        # 考试应用服务
    ├── GradeApplicationService.java              # 成绩应用服务
    ├── command/
    │   ├── CreateSemesterCommand.java
    │   ├── AssignTeachingTaskCommand.java
    │   ├── GenerateScheduleCommand.java
    │   ├── AdjustScheduleCommand.java
    │   ├── ArrangeExamCommand.java
    │   └── RecordGradeCommand.java
    └── query/
        ├── SemesterDTO.java
        ├── TeachingWeekDTO.java
        ├── CurriculumPlanDTO.java
        ├── TeachingTaskDTO.java
        ├── ScheduleEntryDTO.java
        ├── ExamArrangementDTO.java
        └── StudentGradeDTO.java
```

### 2.3 与现有系统集成

```
┌──────────────────────────────────────────────────────────────────┐
│                        Teaching 教学领域                          │
├──────────┬──────────┬──────────┬──────────┬──────────┬──────────┤
│  校历    │ 培养方案 │ 教学任务 │  排课    │   考试   │   成绩   │
└────┬─────┴────┬─────┴────┬─────┴────┬─────┴────┬─────┴────┬─────┘
     │          │          │          │          │          │
     ▼          ▼          ▼          ▼          ▼          ▼
┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐
│Organization│ │  课程   │ │  User   │ │  Asset  │ │Inspection│ │ Rating  │
│(班级/专业)│ │(已有)   │ │ (教师)  │ │ (教室)  │ │(旷课扣分)│ │(成绩测评)│
└─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘
```

**集成点说明：**

| 集成模块 | 集成方式 | 说明 |
|---------|---------|------|
| Organization | 引用 | 排课需要班级、专业信息 |
| Course | 扩展 | 扩展现有课程表，增加分类、先修等字段 |
| User | 引用 | 教学任务分配教师、监考安排 |
| Asset | 引用 | 排课使用教室资源 |
| Inspection | 事件 | 旷课时发送事件触发量化扣分 |
| Rating | 事件 | 成绩录入后更新综合测评 |

---

## 三、数据库设计

### 3.1 校历管理

```sql
-- =====================================================
-- 3.1.1 学年表
-- =====================================================
CREATE TABLE academic_years (
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

-- =====================================================
-- 3.1.2 学期表
-- =====================================================
CREATE TABLE semesters (
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

-- =====================================================
-- 3.1.3 教学周表
-- =====================================================
CREATE TABLE teaching_weeks (
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

-- =====================================================
-- 3.1.4 校历事件表
-- =====================================================
CREATE TABLE school_events (
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

-- =====================================================
-- 3.1.5 作息时间表
-- =====================================================
CREATE TABLE class_time_configs (
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
```

### 3.2 课程与培养方案

```sql
-- =====================================================
-- 3.2.1 课程表（扩展现有courses表）
-- =====================================================
-- 如果courses表已存在，执行ALTER；否则CREATE
-- 以下为完整结构

CREATE TABLE courses (
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

-- =====================================================
-- 3.2.2 培养方案表
-- =====================================================
CREATE TABLE curriculum_plans (
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

-- =====================================================
-- 3.2.3 培养方案课程表（方案中的课程安排）
-- =====================================================
CREATE TABLE curriculum_plan_courses (
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
```

### 3.3 教学任务

```sql
-- =====================================================
-- 3.3.1 教学任务表（学期开课计划）
-- =====================================================
CREATE TABLE teaching_tasks (
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

-- =====================================================
-- 3.3.2 教学任务教师分配表（支持多教师合上）
-- =====================================================
CREATE TABLE teaching_task_teachers (
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

-- =====================================================
-- 3.3.3 教师偏好设置表
-- =====================================================
CREATE TABLE teacher_preferences (
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
```

### 3.4 排课管理

```sql
-- =====================================================
-- 3.4.1 课表条目表（排课结果）
-- =====================================================
CREATE TABLE schedule_entries (
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

-- =====================================================
-- 3.4.2 排课冲突记录表
-- =====================================================
CREATE TABLE schedule_conflicts (
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

-- =====================================================
-- 3.4.3 调课申请表
-- =====================================================
CREATE TABLE schedule_adjustments (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    adjustment_code VARCHAR(50) NOT NULL COMMENT '调课单号',
    semester_id BIGINT NOT NULL COMMENT '学期ID',
    original_entry_id BIGINT NOT NULL COMMENT '原排课条目ID',
    adjustment_type TINYINT NOT NULL COMMENT '调整类型：1调课（换时间）2换教室 3代课（换教师）4停课',
    -- 原始信息
    original_date DATE NOT NULL COMMENT '原上课日期',
    original_weekday TINYINT COMMENT '原周几',
    original_slot INT COMMENT '原节次',
    original_classroom_id BIGINT COMMENT '原教室ID',
    original_teacher_id BIGINT COMMENT '原教师ID',
    -- 调整后信息
    new_date DATE COMMENT '新上课日期（调课时填）',
    new_weekday TINYINT COMMENT '新周几',
    new_slot INT COMMENT '新节次',
    new_classroom_id BIGINT COMMENT '新教室ID',
    new_teacher_id BIGINT COMMENT '代课教师ID',
    -- 申请信息
    applicant_id BIGINT NOT NULL COMMENT '申请人ID',
    apply_reason VARCHAR(500) NOT NULL COMMENT '申请原因',
    apply_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    attachment_urls JSON COMMENT '附件（如请假条）',
    -- 审批信息
    approval_status TINYINT DEFAULT 0 COMMENT '审批状态：0待审批 1已通过 2已拒绝 3已撤回',
    approver_id BIGINT COMMENT '审批人ID',
    approval_time DATETIME COMMENT '审批时间',
    approval_comment VARCHAR(500) COMMENT '审批意见',
    -- 执行信息
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
```

### 3.5 考试管理

```sql
-- =====================================================
-- 3.5.1 考试批次表
-- =====================================================
CREATE TABLE exam_batches (
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

-- =====================================================
-- 3.5.2 考试安排表
-- =====================================================
CREATE TABLE exam_arrangements (
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

-- =====================================================
-- 3.5.3 考场安排表
-- =====================================================
CREATE TABLE exam_rooms (
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

-- =====================================================
-- 3.5.4 考生安排表
-- =====================================================
CREATE TABLE exam_students (
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

-- =====================================================
-- 3.5.5 监考安排表
-- =====================================================
CREATE TABLE exam_invigilators (
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
```

### 3.6 成绩管理

```sql
-- =====================================================
-- 3.6.1 成绩录入批次表
-- =====================================================
CREATE TABLE grade_batches (
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

-- =====================================================
-- 3.6.2 成绩组成配置表（每门课的成绩构成）
-- =====================================================
CREATE TABLE grade_compositions (
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

-- =====================================================
-- 3.6.3 学生成绩表
-- =====================================================
CREATE TABLE student_grades (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    batch_id BIGINT COMMENT '录入批次ID',
    semester_id BIGINT NOT NULL COMMENT '学期ID',
    task_id BIGINT NOT NULL COMMENT '教学任务ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    class_id BIGINT NOT NULL COMMENT '班级ID',
    -- 成绩信息
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
    -- 状态信息
    grade_status TINYINT DEFAULT 0 COMMENT '状态：0未录入 1已录入 2已确认 3已发布 4有异议',
    is_makeup TINYINT DEFAULT 0 COMMENT '是否补考成绩',
    is_retake TINYINT DEFAULT 0 COMMENT '是否重修成绩',
    makeup_count INT DEFAULT 0 COMMENT '补考次数',
    -- 录入信息
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

-- =====================================================
-- 3.6.4 成绩明细表（各成绩项得分）
-- =====================================================
CREATE TABLE student_grade_items (
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

-- =====================================================
-- 3.6.5 成绩修改记录表
-- =====================================================
CREATE TABLE grade_change_logs (
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
```

### 3.7 视图与索引优化

```sql
-- =====================================================
-- 3.7.1 课表视图（方便查询）
-- =====================================================
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
    cls.class_name,
    se.teacher_id,
    u.real_name AS teacher_name,
    se.classroom_id,
    cr.room_name AS classroom_name,
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

-- =====================================================
-- 3.7.2 学生成绩视图
-- =====================================================
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
    s.real_name AS student_name,
    sg.class_id,
    cls.class_name,
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
```

---

## 四、后端API设计

### 4.1 API端点规划

```
/api/v2/teaching/                           # 教学领域根路径
├── academic-years/                         # 学年管理
│   ├── GET    /                           # 学年列表
│   ├── POST   /                           # 创建学年
│   ├── GET    /{id}                       # 学年详情
│   ├── PUT    /{id}                       # 更新学年
│   ├── DELETE /{id}                       # 删除学年
│   └── POST   /{id}/set-current           # 设为当前学年
│
├── semesters/                              # 学期管理
│   ├── GET    /                           # 学期列表
│   ├── POST   /                           # 创建学期
│   ├── GET    /{id}                       # 学期详情
│   ├── PUT    /{id}                       # 更新学期
│   ├── DELETE /{id}                       # 删除学期
│   ├── POST   /{id}/set-current           # 设为当前学期
│   ├── GET    /{id}/weeks                 # 获取教学周列表
│   ├── POST   /{id}/weeks/generate        # 自动生成教学周
│   └── PUT    /{id}/weeks/batch           # 批量更新教学周
│
├── school-events/                          # 校历事件
│   ├── GET    /                           # 事件列表（支持日期范围筛选）
│   ├── POST   /                           # 创建事件
│   ├── PUT    /{id}                       # 更新事件
│   ├── DELETE /{id}                       # 删除事件
│   └── GET    /calendar                   # 获取日历视图数据
│
├── curriculum-plans/                       # 培养方案
│   ├── GET    /                           # 方案列表
│   ├── POST   /                           # 创建方案
│   ├── GET    /{id}                       # 方案详情
│   ├── PUT    /{id}                       # 更新方案
│   ├── DELETE /{id}                       # 删除方案
│   ├── POST   /{id}/publish               # 发布方案
│   ├── GET    /{id}/courses               # 方案课程列表
│   ├── POST   /{id}/courses               # 添加课程到方案
│   ├── PUT    /{id}/courses/{courseId}    # 更新方案课程
│   └── DELETE /{id}/courses/{courseId}    # 从方案移除课程
│
├── teaching-tasks/                         # 教学任务
│   ├── GET    /                           # 任务列表
│   ├── POST   /                           # 创建任务
│   ├── POST   /batch-generate             # 根据培养方案批量生成
│   ├── GET    /{id}                       # 任务详情
│   ├── PUT    /{id}                       # 更新任务
│   ├── DELETE /{id}                       # 删除任务
│   ├── POST   /{id}/assign-teacher        # 分配教师
│   └── DELETE /{id}/teachers/{teacherId}  # 移除教师
│
├── teacher-preferences/                    # 教师偏好
│   ├── GET    /                           # 获取偏好列表
│   ├── POST   /                           # 设置偏好
│   ├── DELETE /{id}                       # 删除偏好
│   └── GET    /my                         # 获取当前教师的偏好
│
├── schedules/                              # 排课管理
│   ├── GET    /                           # 课表查询（支持多维度筛选）
│   ├── POST   /                           # 手动排课
│   ├── PUT    /{id}                       # 修改排课
│   ├── DELETE /{id}                       # 删除排课
│   ├── POST   /auto-generate              # 智能排课
│   ├── POST   /check-conflict             # 冲突检测
│   ├── GET    /by-class/{classId}         # 班级课表
│   ├── GET    /by-teacher/{teacherId}     # 教师课表
│   ├── GET    /by-classroom/{classroomId} # 教室课表
│   ├── GET    /my                         # 我的课表（当前用户）
│   └── POST   /publish                    # 发布课表
│
├── schedule-adjustments/                   # 调课管理
│   ├── GET    /                           # 调课申请列表
│   ├── POST   /                           # 提交调课申请
│   ├── GET    /{id}                       # 申请详情
│   ├── POST   /{id}/approve               # 审批通过
│   ├── POST   /{id}/reject                # 审批拒绝
│   ├── POST   /{id}/withdraw              # 撤回申请
│   └── GET    /my                         # 我的调课申请
│
├── exams/                                  # 考试管理
│   ├── batches/                           # 考试批次
│   │   ├── GET    /                       # 批次列表
│   │   ├── POST   /                       # 创建批次
│   │   ├── GET    /{id}                   # 批次详情
│   │   ├── PUT    /{id}                   # 更新批次
│   │   └── DELETE /{id}                   # 删除批次
│   │
│   ├── arrangements/                       # 考试安排
│   │   ├── GET    /                       # 安排列表
│   │   ├── POST   /                       # 创建安排
│   │   ├── POST   /auto-arrange           # 自动安排考试
│   │   ├── PUT    /{id}                   # 更新安排
│   │   └── DELETE /{id}                   # 删除安排
│   │
│   ├── rooms/                              # 考场管理
│   │   ├── GET    /                       # 考场列表
│   │   ├── POST   /                       # 分配考场
│   │   ├── PUT    /{id}                   # 更新考场
│   │   ├── POST   /{id}/assign-students   # 安排考生座位
│   │   └── POST   /{id}/assign-invigilators # 安排监考
│   │
│   └── students/                           # 考生管理
│       ├── GET    /                       # 考生列表
│       └── PUT    /{id}/status            # 更新考试状态（缺考/作弊）
│
└── grades/                                 # 成绩管理
    ├── batches/                           # 录入批次
    │   ├── GET    /                       # 批次列表
    │   ├── POST   /                       # 创建批次
    │   └── PUT    /{id}/status            # 更新批次状态
    │
    ├── compositions/                       # 成绩组成配置
    │   ├── GET    /task/{taskId}          # 获取任务的成绩构成
    │   ├── POST   /task/{taskId}          # 配置成绩构成
    │   └── PUT    /{id}                   # 更新配置
    │
    ├── records/                            # 成绩记录
    │   ├── GET    /                       # 成绩查询（支持多条件筛选）
    │   ├── POST   /                       # 录入成绩
    │   ├── POST   /batch                  # 批量录入
    │   ├── POST   /import                 # Excel导入
    │   ├── GET    /export                 # Excel导出
    │   ├── PUT    /{id}                   # 修改成绩
    │   ├── POST   /{id}/confirm           # 确认成绩
    │   ├── POST   /batch-confirm          # 批量确认
    │   ├── POST   /publish                # 发布成绩
    │   ├── GET    /my                     # 我的成绩（学生）
    │   └── GET    /my-courses             # 我的课程成绩（教师）
    │
    └── statistics/                         # 成绩统计
        ├── GET    /course/{courseId}      # 课程成绩统计
        ├── GET    /class/{classId}        # 班级成绩统计
        ├── GET    /student/{studentId}    # 学生成绩单
        └── GET    /ranking                # 排名统计
```

### 4.2 智能排课算法设计

```java
/**
 * 智能排课服务
 * 采用约束满足问题(CSP)求解 + 遗传算法优化
 */
public interface SchedulingDomainService {

    /**
     * 自动排课
     * @param request 排课请求（包含约束条件）
     * @return 排课结果
     */
    SchedulingResult autoGenerate(SchedulingRequest request);

    /**
     * 冲突检测
     * @param entry 待检测的排课条目
     * @return 冲突列表
     */
    List<ScheduleConflict> detectConflicts(ScheduleEntry entry);
}

/**
 * 排课请求
 */
@Data
public class SchedulingRequest {
    private Long semesterId;                    // 学期
    private List<Long> taskIds;                 // 待排教学任务
    private SchedulingConstraints constraints;  // 约束条件
    private SchedulingPreferences preferences;  // 偏好设置
}

/**
 * 排课约束（硬约束，必须满足）
 */
@Data
public class SchedulingConstraints {
    // 基础约束（必选）
    private boolean noTeacherConflict = true;    // 教师不冲突
    private boolean noClassroomConflict = true;  // 教室不冲突
    private boolean noClassConflict = true;      // 班级不冲突

    // 教室约束
    private boolean matchClassroomCapacity = true;  // 教室容量匹配
    private boolean matchClassroomType = true;      // 教室类型匹配（机房/实验室等）

    // 时间约束
    private List<Integer> availableWeekdays;     // 可用星期（如不排周六日）
    private List<Integer> availableSlots;        // 可用节次
    private Integer maxDailyHours;               // 每天最多课时
    private Integer maxContinuousHours;          // 最多连续课时
}

/**
 * 排课偏好（软约束，尽量满足）
 */
@Data
public class SchedulingPreferences {
    // 教师偏好
    private boolean respectTeacherPreference = true;  // 尊重教师时间偏好
    private Integer teacherPreferenceWeight = 10;     // 偏好权重

    // 课程偏好
    private boolean morningForMajorCourses = true;    // 专业课尽量上午
    private boolean continuousForLabCourses = true;   // 实验课尽量连堂

    // 分布偏好
    private boolean evenDistribution = true;          // 课程均匀分布
    private boolean avoidSingleSlot = true;           // 避免孤立课（一天只有一节）
}

/**
 * 排课算法实现（伪代码）
 */
public class GeneticSchedulingAlgorithm {

    /**
     * 主流程：
     * 1. 初始化：生成初始种群（随机可行解）
     * 2. 评估：计算每个个体的适应度（约束满足度+偏好满足度）
     * 3. 选择：轮盘赌/锦标赛选择优秀个体
     * 4. 交叉：组合两个个体生成新个体
     * 5. 变异：随机调整部分课程时间
     * 6. 迭代：重复2-5直到满足终止条件
     * 7. 输出：返回最优个体（最佳课表）
     */
    public SchedulingResult solve(SchedulingRequest request) {
        // 参数配置
        int populationSize = 100;      // 种群大小
        int maxGenerations = 500;      // 最大迭代次数
        double mutationRate = 0.1;     // 变异率
        double crossoverRate = 0.8;    // 交叉率

        // 1. 初始化种群
        List<Schedule> population = initializePopulation(request, populationSize);

        // 2. 迭代进化
        for (int gen = 0; gen < maxGenerations; gen++) {
            // 计算适应度
            evaluateFitness(population, request.getConstraints(), request.getPreferences());

            // 检查是否找到完美解
            Schedule best = getBest(population);
            if (best.getFitness() >= 1.0) {
                return SchedulingResult.success(best);
            }

            // 选择、交叉、变异生成新一代
            population = evolve(population, crossoverRate, mutationRate);
        }

        // 返回最优解（可能有部分冲突）
        Schedule best = getBest(population);
        return SchedulingResult.partial(best, detectConflicts(best));
    }
}
```

---

## 五、前端设计

### 5.1 路由结构

```typescript
// router/teaching.ts
export const teachingRoutes = [
  {
    path: '/teaching',
    name: 'Teaching',
    meta: { title: '教务管理', icon: 'Calendar', order: 3 },
    children: [
      // ===== 校历管理 =====
      {
        path: 'calendar',
        name: 'AcademicCalendar',
        meta: { title: '校历管理', icon: 'Calendar' },
        children: [
          { path: 'years', name: 'AcademicYears', meta: { title: '学年管理' } },
          { path: 'semesters', name: 'Semesters', meta: { title: '学期管理' } },
          { path: 'events', name: 'SchoolEvents', meta: { title: '校历事件' } },
          { path: 'view', name: 'CalendarView', meta: { title: '校历视图' } },
        ]
      },

      // ===== 培养方案 =====
      {
        path: 'curriculum',
        name: 'CurriculumPlans',
        meta: { title: '培养方案', icon: 'Document' },
        children: [
          { path: 'plans', name: 'PlanList', meta: { title: '方案列表' } },
          { path: 'plans/:id', name: 'PlanDetail', meta: { title: '方案详情' } },
          { path: 'courses', name: 'CourseLibrary', meta: { title: '课程库' } },
        ]
      },

      // ===== 教学任务 =====
      {
        path: 'tasks',
        name: 'TeachingTasks',
        meta: { title: '教学任务', icon: 'List' },
        children: [
          { path: 'list', name: 'TaskList', meta: { title: '任务列表' } },
          { path: 'assign', name: 'TaskAssign', meta: { title: '任务分配' } },
          { path: 'preferences', name: 'TeacherPreferences', meta: { title: '教师偏好' } },
        ]
      },

      // ===== 排课管理 =====
      {
        path: 'schedule',
        name: 'ScheduleManagement',
        meta: { title: '排课管理', icon: 'Grid' },
        children: [
          { path: 'auto', name: 'AutoSchedule', meta: { title: '智能排课' } },
          { path: 'manual', name: 'ManualSchedule', meta: { title: '手动排课' } },
          { path: 'view', name: 'ScheduleView', meta: { title: '课表查询' } },
          { path: 'adjustment', name: 'ScheduleAdjustment', meta: { title: '调课管理' } },
        ]
      },

      // ===== 考试管理 =====
      {
        path: 'exam',
        name: 'ExamManagement',
        meta: { title: '考试管理', icon: 'EditPen' },
        children: [
          { path: 'batches', name: 'ExamBatches', meta: { title: '考试批次' } },
          { path: 'arrange', name: 'ExamArrange', meta: { title: '考试安排' } },
          { path: 'rooms', name: 'ExamRooms', meta: { title: '考场管理' } },
          { path: 'invigilators', name: 'Invigilators', meta: { title: '监考安排' } },
        ]
      },

      // ===== 成绩管理 =====
      {
        path: 'grades',
        name: 'GradeManagement',
        meta: { title: '成绩管理', icon: 'DataAnalysis' },
        children: [
          { path: 'input', name: 'GradeInput', meta: { title: '成绩录入' } },
          { path: 'query', name: 'GradeQuery', meta: { title: '成绩查询' } },
          { path: 'statistics', name: 'GradeStatistics', meta: { title: '成绩统计' } },
          { path: 'transcript', name: 'Transcript', meta: { title: '成绩单' } },
        ]
      },
    ]
  },

  // ===== 个人课表/成绩（快捷入口） =====
  {
    path: '/my-schedule',
    name: 'MySchedule',
    meta: { title: '我的课表', icon: 'Calendar' },
    component: () => import('@/views/teaching/schedule/MySchedule.vue')
  },
  {
    path: '/my-grades',
    name: 'MyGrades',
    meta: { title: '我的成绩', icon: 'Document' },
    component: () => import('@/views/teaching/grades/MyGrades.vue')
  }
]
```

### 5.2 核心页面设计

#### 5.2.1 校历视图（可视化日历）

```vue
<!-- views/teaching/calendar/CalendarView.vue -->
<template>
  <div class="calendar-view">
    <!-- 头部控制栏 -->
    <div class="calendar-header">
      <div class="semester-selector">
        <el-select v-model="currentSemesterId" placeholder="选择学期">
          <el-option v-for="s in semesters" :key="s.id" :label="s.semesterName" :value="s.id" />
        </el-select>
      </div>
      <div class="view-toggle">
        <el-radio-group v-model="viewMode">
          <el-radio-button value="month">月视图</el-radio-button>
          <el-radio-button value="semester">学期视图</el-radio-button>
        </el-radio-group>
      </div>
      <div class="legend">
        <span class="legend-item holiday">节假日</span>
        <span class="legend-item event">校级活动</span>
        <span class="legend-item exam">考试周</span>
        <span class="legend-item swap">调休补课</span>
      </div>
    </div>

    <!-- 月视图：使用FullCalendar -->
    <FullCalendar
      v-if="viewMode === 'month'"
      :options="calendarOptions"
    />

    <!-- 学期视图：教学周横向时间轴 -->
    <div v-else class="semester-timeline">
      <div class="week-row" v-for="week in teachingWeeks" :key="week.weekNumber">
        <div class="week-label">第{{ week.weekNumber }}周</div>
        <div class="week-days">
          <div
            v-for="day in 7"
            :key="day"
            class="day-cell"
            :class="getDayCellClass(week, day)"
            @click="showDayEvents(week, day)"
          >
            <span class="date">{{ getDayDate(week, day) }}</span>
            <div class="events">
              <span v-for="event in getDayEvents(week, day)" :key="event.id" class="event-dot" :style="{ background: event.color }"></span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 事件详情弹窗 -->
    <EventDetailDialog v-model="eventDialogVisible" :event="selectedEvent" @saved="refreshCalendar" />
  </div>
</template>
```

#### 5.2.2 智能排课界面

```vue
<!-- views/teaching/schedule/AutoSchedule.vue -->
<template>
  <div class="auto-schedule">
    <!-- 步骤条 -->
    <el-steps :active="currentStep" finish-status="success" class="schedule-steps">
      <el-step title="选择任务" description="选择待排课的教学任务" />
      <el-step title="约束设置" description="配置排课约束条件" />
      <el-step title="执行排课" description="运行智能排课算法" />
      <el-step title="调整确认" description="手动调整并确认" />
    </el-steps>

    <!-- 步骤1：选择教学任务 -->
    <div v-if="currentStep === 0" class="step-content">
      <TaskSelector v-model="selectedTasks" :semester-id="semesterId" />
    </div>

    <!-- 步骤2：约束条件配置 -->
    <div v-if="currentStep === 1" class="step-content">
      <el-card header="硬约束（必须满足）">
        <el-checkbox v-model="constraints.noTeacherConflict" disabled>教师时间不冲突</el-checkbox>
        <el-checkbox v-model="constraints.noClassroomConflict" disabled>教室不冲突</el-checkbox>
        <el-checkbox v-model="constraints.noClassConflict" disabled>班级时间不冲突</el-checkbox>
        <el-checkbox v-model="constraints.matchClassroomCapacity">教室容量匹配</el-checkbox>
        <el-checkbox v-model="constraints.matchClassroomType">教室类型匹配</el-checkbox>
      </el-card>

      <el-card header="软约束（尽量满足）" class="mt-4">
        <el-checkbox v-model="preferences.respectTeacherPreference">尊重教师时间偏好</el-checkbox>
        <el-checkbox v-model="preferences.morningForMajorCourses">专业课优先上午</el-checkbox>
        <el-checkbox v-model="preferences.continuousForLabCourses">实验课尽量连堂</el-checkbox>
        <el-checkbox v-model="preferences.evenDistribution">课程均匀分布</el-checkbox>
      </el-card>

      <el-card header="时间限制" class="mt-4">
        <el-form label-width="120px">
          <el-form-item label="可用星期">
            <el-checkbox-group v-model="constraints.availableWeekdays">
              <el-checkbox :value="1">周一</el-checkbox>
              <el-checkbox :value="2">周二</el-checkbox>
              <el-checkbox :value="3">周三</el-checkbox>
              <el-checkbox :value="4">周四</el-checkbox>
              <el-checkbox :value="5">周五</el-checkbox>
              <el-checkbox :value="6">周六</el-checkbox>
              <el-checkbox :value="7">周日</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
          <el-form-item label="每天最多课时">
            <el-input-number v-model="constraints.maxDailyHours" :min="2" :max="10" />
          </el-form-item>
        </el-form>
      </el-card>
    </div>

    <!-- 步骤3：执行排课 -->
    <div v-if="currentStep === 2" class="step-content scheduling-progress">
      <div class="progress-container">
        <el-progress :percentage="progress" :status="progressStatus" :stroke-width="20" />
        <div class="progress-info">
          <p>{{ progressMessage }}</p>
          <p v-if="scheduling">已处理: {{ processedCount }} / {{ totalCount }}</p>
        </div>
      </div>
      <el-button v-if="!scheduling" type="primary" size="large" @click="startScheduling">
        开始智能排课
      </el-button>
    </div>

    <!-- 步骤4：结果调整 -->
    <div v-if="currentStep === 3" class="step-content">
      <ScheduleEditor
        v-model="scheduleResult"
        :conflicts="conflicts"
        @resolve-conflict="handleResolveConflict"
      />
    </div>

    <!-- 底部操作栏 -->
    <div class="step-actions">
      <el-button v-if="currentStep > 0" @click="currentStep--">上一步</el-button>
      <el-button v-if="currentStep < 3" type="primary" @click="currentStep++">下一步</el-button>
      <el-button v-if="currentStep === 3" type="success" @click="confirmSchedule">确认并发布</el-button>
    </div>
  </div>
</template>
```

#### 5.2.3 课表展示组件

```vue
<!-- components/teaching/ScheduleGrid.vue -->
<template>
  <div class="schedule-grid">
    <table class="schedule-table">
      <thead>
        <tr>
          <th class="time-header">节次</th>
          <th v-for="day in weekdays" :key="day.value">{{ day.label }}</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="slot in timeSlots" :key="slot.slot">
          <td class="time-cell">
            <div class="slot-number">第{{ slot.slot }}节</div>
            <div class="slot-time">{{ slot.startTime }}-{{ slot.endTime }}</div>
          </td>
          <td
            v-for="day in weekdays"
            :key="day.value"
            class="schedule-cell"
            :class="{ 'has-course': getCourse(day.value, slot.slot) }"
            @click="handleCellClick(day.value, slot.slot)"
          >
            <div v-if="getCourse(day.value, slot.slot)" class="course-card" :style="{ background: getCourseColor(getCourse(day.value, slot.slot)) }">
              <div class="course-name">{{ getCourse(day.value, slot.slot).courseName }}</div>
              <div class="course-info">
                <span class="teacher">{{ getCourse(day.value, slot.slot).teacherName }}</span>
                <span class="classroom">{{ getCourse(day.value, slot.slot).classroomName }}</span>
              </div>
              <div class="week-range">{{ getCourse(day.value, slot.slot).weekRange }}</div>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
const weekdays = [
  { value: 1, label: '周一' },
  { value: 2, label: '周二' },
  { value: 3, label: '周三' },
  { value: 4, label: '周四' },
  { value: 5, label: '周五' },
  { value: 6, label: '周六' },
  { value: 7, label: '周日' },
]

// 根据课程类型返回不同颜色
const getCourseColor = (course: ScheduleEntry) => {
  const colors: Record<number, string> = {
    1: '#409EFF', // 理论课-蓝色
    2: '#67C23A', // 实验课-绿色
    3: '#E6A23C', // 实践课-橙色
  }
  return colors[course.scheduleType] || '#909399'
}
</script>
```

#### 5.2.4 成绩录入界面

```vue
<!-- views/teaching/grades/GradeInput.vue -->
<template>
  <div class="grade-input">
    <!-- 课程选择 -->
    <el-card class="filter-card">
      <el-form inline>
        <el-form-item label="学期">
          <el-select v-model="semesterId" @change="loadTasks">
            <el-option v-for="s in semesters" :key="s.id" :label="s.semesterName" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程">
          <el-select v-model="taskId" @change="loadStudentGrades">
            <el-option v-for="t in myTasks" :key="t.id" :label="`${t.courseName} - ${t.className}`" :value="t.id" />
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 成绩构成配置 -->
    <el-card v-if="taskId" class="composition-card">
      <template #header>
        <div class="card-header">
          <span>成绩构成</span>
          <el-button text type="primary" @click="showCompositionDialog = true">配置</el-button>
        </div>
      </template>
      <div class="composition-tags">
        <el-tag v-for="item in gradeComposition" :key="item.id">
          {{ item.itemName }}: {{ item.weight }}%
        </el-tag>
      </div>
    </el-card>

    <!-- 成绩表格 -->
    <el-card v-if="taskId" class="grade-table-card">
      <template #header>
        <div class="card-header">
          <span>成绩录入</span>
          <div class="actions">
            <el-button @click="importFromExcel">导入Excel</el-button>
            <el-button @click="exportToExcel">导出Excel</el-button>
            <el-button type="primary" @click="saveGrades" :loading="saving">保存</el-button>
          </div>
        </div>
      </template>

      <el-table :data="studentGrades" border stripe height="500">
        <el-table-column prop="studentNo" label="学号" width="120" fixed />
        <el-table-column prop="studentName" label="姓名" width="100" fixed />

        <!-- 动态成绩列 -->
        <el-table-column v-for="item in gradeComposition" :key="item.id" :label="`${item.itemName}(${item.weight}%)`" width="120">
          <template #default="{ row }">
            <el-input-number
              v-model="row.scores[item.id]"
              :min="0"
              :max="item.fullScore"
              :precision="1"
              size="small"
              controls-position="right"
            />
          </template>
        </el-table-column>

        <!-- 总评自动计算 -->
        <el-table-column label="总评" width="100">
          <template #default="{ row }">
            <span class="total-score" :class="getScoreClass(calculateTotal(row))">
              {{ calculateTotal(row) }}
            </span>
          </template>
        </el-table-column>

        <!-- 等级（五级制时显示） -->
        <el-table-column v-if="gradeScaleType === 2" label="等级" width="80">
          <template #default="{ row }">
            <el-tag :type="getGradeLevelType(row.gradeLevel)">{{ row.gradeLevel }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="备注" min-width="150">
          <template #default="{ row }">
            <el-input v-model="row.remark" placeholder="备注" size="small" />
          </template>
        </el-table-column>
      </el-table>

      <!-- 底部统计 -->
      <div class="grade-statistics">
        <span>应录入: {{ studentGrades.length }} 人</span>
        <span>已录入: {{ inputCount }} 人</span>
        <span>平均分: {{ averageScore }}</span>
        <span>及格率: {{ passRate }}%</span>
      </div>
    </el-card>

    <!-- 成绩构成配置弹窗 -->
    <CompositionConfigDialog
      v-model="showCompositionDialog"
      :task-id="taskId"
      @saved="loadComposition"
    />
  </div>
</template>
```

### 5.3 前端目录结构

```
frontend/src/
├── api/v2/
│   └── teaching/
│       ├── index.ts              # 统一导出
│       ├── calendar.ts           # 校历API
│       ├── curriculum.ts         # 培养方案API
│       ├── task.ts               # 教学任务API
│       ├── schedule.ts           # 排课API
│       ├── exam.ts               # 考试API
│       └── grade.ts              # 成绩API
│
├── types/v2/
│   └── teaching.ts               # 教学模块类型定义
│
├── views/teaching/
│   ├── calendar/                 # 校历管理
│   │   ├── AcademicYears.vue
│   │   ├── Semesters.vue
│   │   ├── SchoolEvents.vue
│   │   └── CalendarView.vue
│   │
│   ├── curriculum/               # 培养方案
│   │   ├── PlanList.vue
│   │   ├── PlanDetail.vue
│   │   └── CourseLibrary.vue
│   │
│   ├── tasks/                    # 教学任务
│   │   ├── TaskList.vue
│   │   ├── TaskAssign.vue
│   │   └── TeacherPreferences.vue
│   │
│   ├── schedule/                 # 排课管理
│   │   ├── AutoSchedule.vue
│   │   ├── ManualSchedule.vue
│   │   ├── ScheduleView.vue
│   │   ├── ScheduleAdjustment.vue
│   │   └── MySchedule.vue
│   │
│   ├── exam/                     # 考试管理
│   │   ├── ExamBatches.vue
│   │   ├── ExamArrange.vue
│   │   ├── ExamRooms.vue
│   │   └── Invigilators.vue
│   │
│   └── grades/                   # 成绩管理
│       ├── GradeInput.vue
│       ├── GradeQuery.vue
│       ├── GradeStatistics.vue
│       ├── Transcript.vue
│       └── MyGrades.vue
│
└── components/teaching/          # 教学模块组件
    ├── ScheduleGrid.vue          # 课表网格
    ├── ScheduleEditor.vue        # 课表编辑器
    ├── TaskSelector.vue          # 任务选择器
    ├── TeacherSelector.vue       # 教师选择器
    ├── ClassroomSelector.vue     # 教室选择器
    ├── TimeSlotPicker.vue        # 时间段选择器
    ├── GradeInputTable.vue       # 成绩录入表格
    ├── GradeStatisticsChart.vue  # 成绩统计图表
    └── ConflictResolver.vue      # 冲突解决器
```

---

## 六、数据权限集成

### 6.1 扩展DataModule枚举

```java
// domain/access/model/DataModule.java
public enum DataModule {
    // 现有模块...

    // ===== 教学领域模块 =====
    /** 校历管理 */
    ACADEMIC_CALENDAR("academic_calendar", "校历管理", "Teaching"),
    /** 培养方案 */
    CURRICULUM_PLAN("curriculum_plan", "培养方案", "Teaching"),
    /** 教学任务 */
    TEACHING_TASK("teaching_task", "教学任务", "Teaching"),
    /** 排课管理 */
    COURSE_SCHEDULE("course_schedule", "排课管理", "Teaching"),
    /** 考试管理 */
    EXAMINATION("examination", "考试管理", "Teaching"),
    /** 成绩管理 */
    STUDENT_GRADE("student_grade", "成绩管理", "Teaching"),
}
```

### 6.2 权限点设计

```sql
-- 教务管理权限点
INSERT INTO permissions (permission_code, permission_name, module, action_type) VALUES
-- 校历管理
('teaching:calendar:view', '查看校历', 'teaching', 'view'),
('teaching:calendar:edit', '编辑校历', 'teaching', 'edit'),
-- 培养方案
('teaching:curriculum:view', '查看培养方案', 'teaching', 'view'),
('teaching:curriculum:edit', '编辑培养方案', 'teaching', 'edit'),
('teaching:curriculum:publish', '发布培养方案', 'teaching', 'audit'),
-- 教学任务
('teaching:task:view', '查看教学任务', 'teaching', 'view'),
('teaching:task:edit', '编辑教学任务', 'teaching', 'edit'),
('teaching:task:assign', '分配教学任务', 'teaching', 'audit'),
-- 排课管理
('teaching:schedule:view', '查看课表', 'teaching', 'view'),
('teaching:schedule:edit', '编辑课表', 'teaching', 'edit'),
('teaching:schedule:auto', '智能排课', 'teaching', 'audit'),
('teaching:schedule:publish', '发布课表', 'teaching', 'audit'),
('teaching:schedule:adjust', '申请调课', 'teaching', 'edit'),
('teaching:schedule:approve', '审批调课', 'teaching', 'audit'),
-- 考试管理
('teaching:exam:view', '查看考试安排', 'teaching', 'view'),
('teaching:exam:edit', '编辑考试安排', 'teaching', 'edit'),
('teaching:exam:arrange', '安排考场监考', 'teaching', 'audit'),
-- 成绩管理
('teaching:grade:view', '查看成绩', 'teaching', 'view'),
('teaching:grade:input', '录入成绩', 'teaching', 'edit'),
('teaching:grade:confirm', '确认成绩', 'teaching', 'audit'),
('teaching:grade:publish', '发布成绩', 'teaching', 'audit'),
('teaching:grade:modify', '修改已确认成绩', 'teaching', 'admin');
```

---

## 七、与现有系统集成

### 7.1 与量化检查联动

```java
// 旷课事件监听器
@Component
public class AbsenceEventHandler {

    @Autowired
    private InspectionService inspectionService;

    /**
     * 监听考勤记录，旷课时触发量化扣分
     */
    @EventListener
    public void handleAbsenceEvent(StudentAbsenceEvent event) {
        // 查找该班级的量化检查记录
        // 自动添加旷课扣分项
        DeductionRecord record = DeductionRecord.builder()
            .classId(event.getClassId())
            .studentId(event.getStudentId())
            .deductionType("旷课")
            .deductionScore(event.getAbsenceHours() * 2) // 每节课扣2分
            .reason("系统自动记录：" + event.getCourseName() + " 旷课")
            .recordDate(event.getDate())
            .build();

        inspectionService.addDeductionRecord(record);
    }
}
```

### 7.2 与综合测评联动

```java
// 成绩发布后更新综合测评
@Component
public class GradePublishedEventHandler {

    @Autowired
    private RatingService ratingService;

    @EventListener
    public void handleGradePublished(GradePublishedEvent event) {
        // 更新学生综合测评中的学业成绩维度
        ratingService.updateAcademicScore(
            event.getStudentId(),
            event.getSemesterId(),
            event.getGradePoint()  // 使用绩点
        );
    }
}
```

### 7.3 与"我的班级"集成

```java
// MyClassApplicationService 扩展
public class MyClassApplicationService {

    /**
     * 获取班级课表（在"我的班级"中显示）
     */
    public List<ScheduleEntryDTO> getClassSchedule(Long classId) {
        return scheduleRepository.findByClassId(classId);
    }

    /**
     * 获取班级成绩概览
     */
    public ClassGradeOverview getClassGradeOverview(Long classId, Long semesterId) {
        // 统计班级平均分、及格率、优秀率等
    }
}
```

---

## 八、实施计划

### 8.1 分阶段开发

| 阶段 | 模块 | 工作内容 | 预估工作量 |
|-----|------|---------|-----------|
| **Phase 1** | 基础设施 | 数据库表创建、领域模型搭建、基础CRUD | 3天 |
| **Phase 2** | 校历管理 | 学年学期、教学周、校历事件、日历视图 | 3天 |
| **Phase 3** | 培养方案 | 方案管理、课程库扩展、方案课程配置 | 2天 |
| **Phase 4** | 教学任务 | 任务管理、教师分配、教师偏好设置 | 2天 |
| **Phase 5** | 排课管理 | 手动排课、智能排课算法、冲突检测 | 5天 |
| **Phase 6** | 调课管理 | 调课申请、审批流程 | 2天 |
| **Phase 7** | 考试管理 | 考试批次、考场安排、监考分配 | 3天 |
| **Phase 8** | 成绩管理 | 成绩录入、成绩统计、成绩单 | 4天 |
| **Phase 9** | 系统集成 | 与量化检查、综合测评、我的班级联动 | 2天 |
| **Phase 10** | 测试优化 | 功能测试、性能优化、Bug修复 | 3天 |
| **总计** | | | **约29天** |

### 8.2 开发顺序建议

```
1. 基础设施 (Phase 1)
   ↓
2. 校历管理 (Phase 2) ─────┐
   ↓                      │
3. 培养方案 (Phase 3)      │ 可并行
   ↓                      │
4. 教学任务 (Phase 4) ←────┘
   ↓
5. 排课管理 (Phase 5) ← 依赖校历、任务
   ↓
6. 调课管理 (Phase 6) ← 依赖排课
   ↓
7. 考试管理 (Phase 7) ← 依赖校历
   ↓
8. 成绩管理 (Phase 8) ← 依赖任务、考试
   ↓
9. 系统集成 (Phase 9)
   ↓
10. 测试优化 (Phase 10)
```

---

## 九、技术要点

### 9.1 智能排课算法选型

| 算法 | 优点 | 缺点 | 适用场景 |
|-----|------|------|---------|
| **贪心算法** | 简单快速 | 易陷入局部最优 | 小规模、约束少 |
| **回溯算法** | 能找到精确解 | 指数级时间复杂度 | 小规模、要求精确 |
| **遗传算法** | 全局搜索能力强 | 需要调参 | 中大规模 |
| **模拟退火** | 跳出局部最优 | 收敛慢 | 中规模 |
| **约束传播+回溯** | 剪枝效率高 | 实现复杂 | 约束多的场景 |

**建议方案**：采用 **约束传播 + 遗传算法** 混合策略
1. 先用约束传播减少搜索空间
2. 再用遗传算法全局优化

### 9.2 性能优化建议

1. **课表查询**：使用Redis缓存热点课表数据
2. **智能排课**：采用异步任务+进度推送（WebSocket）
3. **成绩统计**：预计算并缓存统计结果
4. **大数据导入**：使用批量插入+事务分片

### 9.3 数据一致性

1. **排课发布后**：生成快照，原始数据只读
2. **调课执行**：使用分布式锁防止并发冲突
3. **成绩修改**：记录完整变更日志，支持审计追溯

---

## 十、附录

### 10.1 术语表

| 术语 | 英文 | 说明 |
|-----|------|------|
| 学年 | Academic Year | 一个完整教学年度，如2025-2026学年 |
| 学期 | Semester | 学年的半年划分 |
| 教学周 | Teaching Week | 学期内的周次编号 |
| 培养方案 | Curriculum Plan | 专业人才培养的课程体系设计 |
| 教学任务 | Teaching Task | 学期内某班级某课程的教学安排 |
| 排课 | Scheduling | 将教学任务安排到具体时间地点 |
| 调课 | Schedule Adjustment | 临时变更上课时间或地点 |
| 绩点 | Grade Point | 成绩的标准化指标 |

### 10.2 参考资料

1. 教育部《普通高等学校学生管理规定》
2. 业界排课系统：正方教务、金智教务、青果教务
3. 排课算法论文：《基于遗传算法的高校排课系统研究》

---

*文档结束*
