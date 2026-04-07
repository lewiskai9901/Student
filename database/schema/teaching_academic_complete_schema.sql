-- =============================================================================
-- 教务管理 & 学术管理模块 — 完整 Schema (最终态)
--
-- 本文件是所有增量迁移脚本的收敛结果，标记各表的权威列定义。
-- 涉及迁移脚本:
--   V20260121_1__teaching_management_init.sql
--   V20260324_1__teaching_tables_upgrade.sql
--   V73.0.0__teaching_module_redesign.sql
--   V76.0.0__curriculum_plan_direction.sql
--   V84.0.0__academic_domain.sql
--   V86.0.0__offering_plan_link.sql
--
-- 生成日期: 2026-04-04
-- =============================================================================

SET NAMES utf8mb4;

-- ===================== 校历管理 (Calendar Domain) =====================

CREATE TABLE IF NOT EXISTS `academic_years` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `year_code` VARCHAR(20) NOT NULL COMMENT '学年编码 (如 2024-2025)',
    `year_name` VARCHAR(50) NOT NULL COMMENT '学年名称',
    `start_date` DATE NOT NULL,
    `end_date` DATE NOT NULL,
    `is_current` TINYINT DEFAULT 0 COMMENT '是否当前学年',
    `status` TINYINT DEFAULT 1 COMMENT '1=正常 0=停用',
    `created_by` BIGINT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_by` BIGINT,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    UNIQUE KEY `uk_year_code` (`year_code`)
) COMMENT '学年表';

CREATE TABLE IF NOT EXISTS `semesters` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `semester_code` VARCHAR(20) NOT NULL COMMENT '学期编码 (如 2024-2025-1)',
    `semester_name` VARCHAR(50) NOT NULL,
    `start_year` INT NOT NULL COMMENT '起始年份',
    `semester_type` TINYINT NOT NULL COMMENT '1=第一学期 2=第二学期',
    `start_date` DATE NOT NULL,
    `end_date` DATE NOT NULL,
    `is_current` TINYINT DEFAULT 0,
    `status` TINYINT DEFAULT 1 COMMENT '1=正常 0=已结束',
    `created_by` BIGINT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_by` BIGINT,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    UNIQUE KEY `uk_semester_code` (`semester_code`)
) COMMENT '学期表';

CREATE TABLE IF NOT EXISTS `academic_weeks` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `semester_id` BIGINT NOT NULL,
    `week_number` INT NOT NULL,
    `week_name` VARCHAR(50) COMMENT '如: 第1周',
    `start_date` DATE NOT NULL,
    `end_date` DATE NOT NULL,
    `is_current` TINYINT DEFAULT 0,
    `status` TINYINT DEFAULT 1,
    INDEX `idx_semester` (`semester_id`)
) COMMENT '教学周表';

CREATE TABLE IF NOT EXISTS `academic_event` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `year_id` BIGINT COMMENT '所属学年',
    `semester_id` BIGINT COMMENT '所属学期',
    `event_name` VARCHAR(100) NOT NULL,
    `event_type` TINYINT DEFAULT 5 COMMENT '1=开学 2=放假 3=考试 4=活动 5=其他',
    `start_date` DATE NOT NULL,
    `end_date` DATE,
    `all_day` TINYINT DEFAULT 1,
    `description` VARCHAR(500),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    INDEX `idx_year` (`year_id`),
    INDEX `idx_semester` (`semester_id`)
) COMMENT '校历事件表';

-- ===================== 学术管理 (Academic Domain) =====================

CREATE TABLE IF NOT EXISTS `major_categories` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `code` VARCHAR(10) NOT NULL COMMENT '大类编码 (01-14)',
    `name` VARCHAR(100) NOT NULL,
    `sort_order` INT DEFAULT 0,
    `enabled` TINYINT DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    UNIQUE KEY `uk_code` (`code`)
) COMMENT '专业大类表';

CREATE TABLE IF NOT EXISTS `majors` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `major_code` VARCHAR(50) NOT NULL,
    `major_name` VARCHAR(100) NOT NULL,
    `major_category_code` VARCHAR(10) COMMENT '专业大类编码',
    `org_unit_id` BIGINT NOT NULL COMMENT '所属院系',
    `schooling_years` INT DEFAULT 3,
    `description` VARCHAR(500),
    `enrollment_target` VARCHAR(50) COMMENT '招生对象',
    `education_form` VARCHAR(50) COMMENT '办学形式',
    `lead_teacher_id` BIGINT,
    `lead_teacher_name` VARCHAR(100),
    `approval_year` INT COMMENT '批准年份',
    `major_status` VARCHAR(20) DEFAULT 'ENROLLING' COMMENT 'PREPARING/ENROLLING/SUSPENDED/REVOKED',
    `sort_order` INT DEFAULT 0,
    `status` TINYINT DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `created_by` BIGINT,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `updated_by` BIGINT,
    `deleted` TINYINT DEFAULT 0,
    UNIQUE KEY `uk_major_code` (`major_code`),
    INDEX `idx_org_unit` (`org_unit_id`),
    INDEX `idx_major_status` (`major_status`)
) COMMENT '专业表';

CREATE TABLE IF NOT EXISTS `major_directions` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `direction_code` VARCHAR(50) NOT NULL,
    `direction_name` VARCHAR(100) NOT NULL,
    `major_id` BIGINT NOT NULL,
    `description` VARCHAR(500),
    `enrollment_target` VARCHAR(50),
    `education_form` VARCHAR(50),
    `certificate_names` JSON COMMENT '职业资格证书列表',
    `training_standard` VARCHAR(200),
    `cooperation_enterprise` VARCHAR(200),
    `max_enrollment` INT,
    `sort_order` INT DEFAULT 0,
    `enabled` TINYINT DEFAULT 1,
    `status` TINYINT DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `created_by` BIGINT,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `updated_by` BIGINT,
    `deleted` TINYINT DEFAULT 0,
    UNIQUE KEY `uk_direction_code` (`direction_code`),
    INDEX `idx_major` (`major_id`)
) COMMENT '专业方向表';

CREATE TABLE IF NOT EXISTS `grade_major_directions` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `grade_id` BIGINT NOT NULL COMMENT '年级ID',
    `major_direction_id` BIGINT NOT NULL,
    `major_id` BIGINT NOT NULL,
    `major_name` VARCHAR(100),
    `planned_class_count` INT DEFAULT 0,
    `planned_student_count` INT DEFAULT 0,
    `actual_class_count` INT DEFAULT 0,
    `actual_student_count` INT DEFAULT 0,
    `remarks` VARCHAR(500),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by` BIGINT,
    `updated_by` BIGINT,
    `deleted` TINYINT DEFAULT 0,
    UNIQUE KEY `uk_grade_direction` (`grade_id`, `major_direction_id`),
    INDEX `idx_grade` (`grade_id`),
    INDEX `idx_major_direction` (`major_direction_id`)
) COMMENT '年级专业方向映射表';

CREATE TABLE IF NOT EXISTS `courses` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `course_code` VARCHAR(50) NOT NULL,
    `course_name` VARCHAR(100) NOT NULL,
    `course_name_en` VARCHAR(200),
    `course_category` TINYINT DEFAULT 1 COMMENT '1=公共基础 2=专业核心 3=专业方向 4=选修',
    `course_type` TINYINT DEFAULT 1 COMMENT '1=理论 2=实践 3=理论+实践',
    `course_nature` TINYINT DEFAULT 1 COMMENT '1=必修 2=限选 3=任选',
    `credits` DECIMAL(4,1) DEFAULT 0,
    `total_hours` INT DEFAULT 0,
    `theory_hours` INT DEFAULT 0,
    `practice_hours` INT DEFAULT 0,
    `weekly_hours` INT DEFAULT 2,
    `exam_type` TINYINT DEFAULT 1 COMMENT '1=考试 2=考查',
    `org_unit_id` BIGINT COMMENT '开课院系',
    `description` TEXT,
    `status` TINYINT DEFAULT 1,
    `created_by` BIGINT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_by` BIGINT,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    UNIQUE KEY `uk_course_code` (`course_code`),
    INDEX `idx_category` (`course_category`),
    INDEX `idx_org_unit` (`org_unit_id`)
) COMMENT '课程表';

CREATE TABLE IF NOT EXISTS `curriculum_plans` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `plan_code` VARCHAR(50) NOT NULL,
    `plan_name` VARCHAR(100) NOT NULL,
    `major_id` BIGINT NOT NULL,
    `major_direction_id` BIGINT COMMENT '专业方向',
    `grade_year` INT NOT NULL COMMENT '适用年级',
    `total_credits` DECIMAL(5,1),
    `required_credits` DECIMAL(5,1),
    `elective_credits` DECIMAL(5,1),
    `practice_credits` DECIMAL(5,1),
    `training_objective` TEXT,
    `graduation_requirement` TEXT,
    `version` INT DEFAULT 1,
    `status` TINYINT DEFAULT 0 COMMENT '0=草稿 1=已发布 2=已归档',
    `published_at` DATETIME,
    `published_by` BIGINT,
    `created_by` BIGINT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_by` BIGINT,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    UNIQUE KEY `uk_plan_code` (`plan_code`),
    INDEX `idx_major` (`major_id`),
    INDEX `idx_major_direction` (`major_direction_id`),
    INDEX `idx_grade_year` (`grade_year`)
) COMMENT '培养方案表';

CREATE TABLE IF NOT EXISTS `curriculum_plan_courses` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `plan_id` BIGINT NOT NULL,
    `course_id` BIGINT NOT NULL,
    `semester_number` TINYINT NOT NULL COMMENT '学期序号 (1-8)',
    `course_category` TINYINT,
    `course_type` TINYINT,
    `credits` DECIMAL(4,1),
    `total_hours` INT,
    `weekly_hours` INT,
    `theory_hours` INT,
    `practice_hours` INT,
    `exam_type` TINYINT,
    `sort_order` INT DEFAULT 0,
    `remark` VARCHAR(200),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_plan_course` (`plan_id`, `course_id`),
    INDEX `idx_plan` (`plan_id`),
    INDEX `idx_course` (`course_id`),
    INDEX `idx_semester` (`semester_number`)
) COMMENT '培养方案课程表';

-- ===================== 教务管理 (Teaching Domain) =====================

CREATE TABLE IF NOT EXISTS `semester_course_offerings` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `semester_id` BIGINT NOT NULL,
    `plan_id` BIGINT COMMENT '来源培养方案',
    `plan_course_id` BIGINT COMMENT '来源方案课程',
    `course_id` BIGINT NOT NULL,
    `applicable_grade` VARCHAR(50) COMMENT '适用年级',
    `weekly_hours` INT NOT NULL,
    `total_weeks` INT,
    `start_week` INT DEFAULT 1,
    `end_week` INT,
    `course_category` TINYINT,
    `course_type` TINYINT,
    `allow_combined` TINYINT DEFAULT 0 COMMENT '允许合堂',
    `max_combined_classes` INT DEFAULT 2,
    `allow_walking` TINYINT DEFAULT 0 COMMENT '允许走班',
    `status` TINYINT DEFAULT 0 COMMENT '0=草稿 1=已确认 2=已分配',
    `remark` VARCHAR(500),
    `created_by` BIGINT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    UNIQUE KEY `uk_semester_course` (`semester_id`, `course_id`, `applicable_grade`),
    INDEX `idx_semester` (`semester_id`),
    INDEX `idx_course` (`course_id`)
) COMMENT '学期开课计划表';

CREATE TABLE IF NOT EXISTS `class_course_assignments` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `semester_id` BIGINT NOT NULL,
    `class_id` BIGINT NOT NULL,
    `offering_id` BIGINT NOT NULL COMMENT '关联开课计划',
    `course_id` BIGINT NOT NULL,
    `weekly_hours` INT NOT NULL,
    `student_count` INT,
    `status` TINYINT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    UNIQUE KEY `uk_class_course` (`semester_id`, `class_id`, `course_id`),
    INDEX `idx_offering` (`offering_id`)
) COMMENT '班级开课分配表';

CREATE TABLE IF NOT EXISTS `teaching_tasks` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `task_code` VARCHAR(50) NOT NULL,
    `semester_id` BIGINT NOT NULL,
    `course_id` BIGINT NOT NULL,
    `class_id` BIGINT NOT NULL,
    `offering_id` BIGINT COMMENT '关联开课计划',
    `teaching_class_id` BIGINT COMMENT '关联教学班',
    `org_unit_id` BIGINT,
    `student_count` INT DEFAULT 0,
    `weekly_hours` INT NOT NULL,
    `total_hours` INT NOT NULL,
    `start_week` INT DEFAULT 1,
    `end_week` INT DEFAULT 16,
    `scheduling_status` TINYINT DEFAULT 0 COMMENT '0=未排 1=部分 2=完成',
    `task_status` TINYINT DEFAULT 1 COMMENT '0=草稿 1=已确认 2=已取消',
    `remark` VARCHAR(500),
    `created_by` BIGINT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_by` BIGINT,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    UNIQUE KEY `uk_task_code` (`task_code`),
    INDEX `idx_semester` (`semester_id`),
    INDEX `idx_course` (`course_id`),
    INDEX `idx_class` (`class_id`),
    INDEX `idx_offering` (`offering_id`),
    INDEX `idx_teaching_class` (`teaching_class_id`)
) COMMENT '教学任务表';

CREATE TABLE IF NOT EXISTS `teaching_task_teachers` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `task_id` BIGINT NOT NULL,
    `teacher_id` BIGINT NOT NULL,
    `teacher_role` TINYINT DEFAULT 1 COMMENT '1=主讲 2=辅助',
    `workload_ratio` DECIMAL(3,2) DEFAULT 1.00,
    `remark` VARCHAR(200),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_task_teacher` (`task_id`, `teacher_id`),
    INDEX `idx_teacher` (`teacher_id`)
) COMMENT '教学任务教师分配表';

CREATE TABLE IF NOT EXISTS `teaching_classes` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `semester_id` BIGINT NOT NULL,
    `class_name` VARCHAR(100) NOT NULL,
    `class_code` VARCHAR(50),
    `course_id` BIGINT NOT NULL,
    `class_type` TINYINT DEFAULT 1 COMMENT '1=普通 2=合堂 3=走班',
    `weekly_hours` INT NOT NULL,
    `student_count` INT DEFAULT 0,
    `required_room_type` VARCHAR(50),
    `required_capacity` INT,
    `start_week` INT DEFAULT 1,
    `end_week` INT,
    `status` TINYINT DEFAULT 1,
    `remark` VARCHAR(500),
    `created_by` BIGINT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    INDEX `idx_semester` (`semester_id`),
    INDEX `idx_course` (`course_id`)
) COMMENT '教学班表';

CREATE TABLE IF NOT EXISTS `teaching_class_members` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `teaching_class_id` BIGINT NOT NULL,
    `member_type` TINYINT NOT NULL COMMENT '1=行政班 2=学生',
    `admin_class_id` BIGINT,
    `student_id` BIGINT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_teaching_class` (`teaching_class_id`)
) COMMENT '教学班成员表';

CREATE TABLE IF NOT EXISTS `scheduling_constraints` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `semester_id` BIGINT NOT NULL,
    `constraint_name` VARCHAR(100) NOT NULL,
    `constraint_level` TINYINT NOT NULL COMMENT '1=全局 2=教师 3=班级 4=课程',
    `target_id` BIGINT,
    `target_name` VARCHAR(100),
    `constraint_type` VARCHAR(50) NOT NULL,
    `is_hard` TINYINT DEFAULT 1,
    `priority` INT DEFAULT 50,
    `params` JSON NOT NULL,
    `effective_weeks` VARCHAR(100),
    `enabled` TINYINT DEFAULT 1,
    `created_by` BIGINT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    INDEX `idx_semester` (`semester_id`),
    INDEX `idx_level_type` (`constraint_level`, `constraint_type`)
) COMMENT '排课约束规则表';

CREATE TABLE IF NOT EXISTS `schedule_entries` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `semester_id` BIGINT NOT NULL,
    `task_id` BIGINT NOT NULL,
    `teaching_class_id` BIGINT,
    `course_id` BIGINT NOT NULL,
    `class_id` BIGINT NOT NULL,
    `teacher_id` BIGINT NOT NULL,
    `classroom_id` BIGINT,
    `weekday` TINYINT NOT NULL COMMENT '1-7 周一至周日',
    `start_slot` INT NOT NULL,
    `end_slot` INT NOT NULL,
    `start_week` INT DEFAULT 1,
    `end_week` INT DEFAULT 16,
    `week_type` TINYINT DEFAULT 0 COMMENT '0=每周 1=单周 2=双周',
    `consecutive_group` VARCHAR(50) COMMENT '连排分组',
    `schedule_type` TINYINT DEFAULT 1,
    `entry_status` TINYINT DEFAULT 1,
    `conflict_flag` TINYINT DEFAULT 0,
    `created_by` BIGINT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_by` BIGINT,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    INDEX `idx_semester` (`semester_id`),
    INDEX `idx_task` (`task_id`),
    INDEX `idx_teacher_weekday` (`teacher_id`, `weekday`),
    INDEX `idx_classroom_weekday` (`classroom_id`, `weekday`),
    INDEX `idx_class_weekday` (`class_id`, `weekday`)
) COMMENT '课表条目表';

CREATE TABLE IF NOT EXISTS `schedule_adjustments` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `adjustment_code` VARCHAR(50) NOT NULL,
    `semester_id` BIGINT NOT NULL,
    `original_entry_id` BIGINT NOT NULL,
    `adjustment_type` TINYINT NOT NULL COMMENT '1=调课 2=停课 3=补课',
    `original_date` DATE NOT NULL,
    `original_weekday` TINYINT,
    `original_slot` INT,
    `original_classroom_id` BIGINT,
    `original_teacher_id` BIGINT,
    `new_date` DATE,
    `new_weekday` TINYINT,
    `new_slot` INT,
    `new_classroom_id` BIGINT,
    `new_teacher_id` BIGINT,
    `applicant_id` BIGINT NOT NULL,
    `apply_reason` VARCHAR(500) NOT NULL,
    `apply_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `attachment_urls` JSON,
    `approval_status` TINYINT DEFAULT 0 COMMENT '0=待审 1=通过 2=驳回',
    `approver_id` BIGINT,
    `approval_time` DATETIME,
    `approval_comment` VARCHAR(500),
    `executed` TINYINT DEFAULT 0,
    `executed_at` DATETIME,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    UNIQUE KEY `uk_adjustment_code` (`adjustment_code`),
    INDEX `idx_semester` (`semester_id`),
    INDEX `idx_entry` (`original_entry_id`)
) COMMENT '调课申请表';

CREATE TABLE IF NOT EXISTS `schedule_conflict_records` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `semester_id` BIGINT NOT NULL,
    `detection_batch` VARCHAR(50),
    `conflict_category` TINYINT NOT NULL COMMENT '1=教师 2=教室 3=班级',
    `conflict_type` VARCHAR(50) NOT NULL,
    `severity` TINYINT NOT NULL COMMENT '1=低 2=中 3=高',
    `description` VARCHAR(500) NOT NULL,
    `detail` JSON,
    `entry_id_1` BIGINT,
    `entry_id_2` BIGINT,
    `constraint_id` BIGINT,
    `resolution_status` TINYINT DEFAULT 0 COMMENT '0=未解决 1=已解决 2=已忽略',
    `resolution_note` VARCHAR(500),
    `resolved_by` BIGINT,
    `resolved_at` DATETIME,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_semester` (`semester_id`),
    INDEX `idx_status` (`resolution_status`)
) COMMENT '排课冲突记录表';

-- ===================== 考试管理 =====================

CREATE TABLE IF NOT EXISTS `exam_batches` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `batch_code` VARCHAR(50) NOT NULL,
    `batch_name` VARCHAR(100) NOT NULL,
    `semester_id` BIGINT NOT NULL,
    `exam_type` TINYINT NOT NULL COMMENT '1=期中 2=期末 3=补考 4=重修',
    `start_date` DATE NOT NULL,
    `end_date` DATE NOT NULL,
    `registration_deadline` DATETIME,
    `status` TINYINT DEFAULT 0 COMMENT '0=草稿 1=已发布 2=进行中 3=已结束',
    `remark` VARCHAR(500),
    `created_by` BIGINT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_by` BIGINT,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    UNIQUE KEY `uk_batch_code` (`batch_code`),
    INDEX `idx_semester` (`semester_id`)
) COMMENT '考试批次表';

CREATE TABLE IF NOT EXISTS `exam_arrangements` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `batch_id` BIGINT NOT NULL,
    `course_id` BIGINT NOT NULL,
    `task_id` BIGINT COMMENT '关联教学任务',
    `class_id` BIGINT COMMENT '关联班级',
    `exam_date` DATE NOT NULL,
    `start_time` TIME NOT NULL,
    `end_time` TIME NOT NULL,
    `duration` INT COMMENT '考试时长(分钟)',
    `exam_form` TINYINT DEFAULT 1 COMMENT '1=笔试 2=机试 3=口试 4=实操',
    `total_students` INT DEFAULT 0,
    `remark` VARCHAR(200),
    `status` TINYINT DEFAULT 1,
    `created_by` BIGINT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_by` BIGINT,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_batch` (`batch_id`),
    INDEX `idx_course` (`course_id`),
    INDEX `idx_task` (`task_id`)
) COMMENT '考试安排表';

CREATE TABLE IF NOT EXISTS `exam_rooms` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `arrangement_id` BIGINT NOT NULL,
    `classroom_id` BIGINT NOT NULL,
    `room_code` VARCHAR(20),
    `seat_count` INT NOT NULL,
    `student_count` INT DEFAULT 0,
    `seat_layout` JSON,
    `remark` VARCHAR(200),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_arrangement` (`arrangement_id`)
) COMMENT '考场安排表';

CREATE TABLE IF NOT EXISTS `exam_invigilators` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `room_id` BIGINT NOT NULL,
    `teacher_id` BIGINT NOT NULL,
    `invigilator_role` TINYINT DEFAULT 1 COMMENT '1=主监 2=副监',
    `status` TINYINT DEFAULT 1,
    `remark` VARCHAR(200),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_room` (`room_id`),
    INDEX `idx_teacher` (`teacher_id`)
) COMMENT '监考安排表';

-- ===================== 成绩管理 =====================

CREATE TABLE IF NOT EXISTS `grade_batches` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `batch_code` VARCHAR(50) NOT NULL,
    `batch_name` VARCHAR(100) NOT NULL,
    `semester_id` BIGINT NOT NULL,
    `grade_type` TINYINT NOT NULL COMMENT '1=平时 2=期中 3=期末 4=总评',
    `start_time` DATETIME,
    `end_time` DATETIME,
    `status` TINYINT DEFAULT 0 COMMENT '0=草稿 1=提交 2=审核 3=已发布',
    `created_by` BIGINT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_by` BIGINT,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    UNIQUE KEY `uk_batch_code` (`batch_code`),
    INDEX `idx_semester` (`semester_id`)
) COMMENT '成绩录入批次表';

CREATE TABLE IF NOT EXISTS `student_grades` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `batch_id` BIGINT,
    `semester_id` BIGINT NOT NULL,
    `task_id` BIGINT NOT NULL COMMENT '关联教学任务',
    `course_id` BIGINT NOT NULL,
    `student_id` BIGINT NOT NULL,
    `class_id` BIGINT NOT NULL,
    `total_score` DECIMAL(5,1),
    `grade_level` VARCHAR(10) COMMENT 'A/B/C/D/F',
    `grade_point` DECIMAL(3,1),
    `passed` TINYINT,
    `credits_earned` DECIMAL(4,1),
    `grade_status` TINYINT DEFAULT 0 COMMENT '0=未录入 1=已录入 2=已确认',
    `is_makeup` TINYINT DEFAULT 0,
    `is_retake` TINYINT DEFAULT 0,
    `remark` VARCHAR(500),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    UNIQUE KEY `uk_student_course_semester` (`student_id`, `course_id`, `semester_id`, `is_retake`),
    INDEX `idx_batch` (`batch_id`),
    INDEX `idx_semester` (`semester_id`),
    INDEX `idx_task` (`task_id`),
    INDEX `idx_student` (`student_id`)
) COMMENT '学生成绩表';

-- ===================== 外键约束 =====================
-- 注: 仅在应用层已验证的核心关系上添加外键

-- 校历
ALTER TABLE `academic_weeks` ADD CONSTRAINT `fk_week_semester`
    FOREIGN KEY (`semester_id`) REFERENCES `semesters`(`id`) ON DELETE CASCADE;

ALTER TABLE `academic_event` ADD CONSTRAINT `fk_event_year`
    FOREIGN KEY (`year_id`) REFERENCES `academic_years`(`id`) ON DELETE SET NULL;

-- 学术
ALTER TABLE `major_directions` ADD CONSTRAINT `fk_direction_major`
    FOREIGN KEY (`major_id`) REFERENCES `majors`(`id`) ON DELETE CASCADE;

ALTER TABLE `curriculum_plans` ADD CONSTRAINT `fk_plan_major`
    FOREIGN KEY (`major_id`) REFERENCES `majors`(`id`);

ALTER TABLE `curriculum_plans` ADD CONSTRAINT `fk_plan_direction`
    FOREIGN KEY (`major_direction_id`) REFERENCES `major_directions`(`id`) ON DELETE SET NULL;

ALTER TABLE `curriculum_plan_courses` ADD CONSTRAINT `fk_plancourse_plan`
    FOREIGN KEY (`plan_id`) REFERENCES `curriculum_plans`(`id`) ON DELETE CASCADE;

ALTER TABLE `curriculum_plan_courses` ADD CONSTRAINT `fk_plancourse_course`
    FOREIGN KEY (`course_id`) REFERENCES `courses`(`id`);

-- 教务: 开课→任务链
ALTER TABLE `semester_course_offerings` ADD CONSTRAINT `fk_offering_semester`
    FOREIGN KEY (`semester_id`) REFERENCES `semesters`(`id`);

ALTER TABLE `semester_course_offerings` ADD CONSTRAINT `fk_offering_course`
    FOREIGN KEY (`course_id`) REFERENCES `courses`(`id`);

ALTER TABLE `class_course_assignments` ADD CONSTRAINT `fk_assignment_offering`
    FOREIGN KEY (`offering_id`) REFERENCES `semester_course_offerings`(`id`);

ALTER TABLE `teaching_tasks` ADD CONSTRAINT `fk_task_semester`
    FOREIGN KEY (`semester_id`) REFERENCES `semesters`(`id`);

ALTER TABLE `teaching_tasks` ADD CONSTRAINT `fk_task_course`
    FOREIGN KEY (`course_id`) REFERENCES `courses`(`id`);

ALTER TABLE `teaching_task_teachers` ADD CONSTRAINT `fk_taskteacher_task`
    FOREIGN KEY (`task_id`) REFERENCES `teaching_tasks`(`id`) ON DELETE CASCADE;

-- 教务: 排课
ALTER TABLE `schedule_entries` ADD CONSTRAINT `fk_entry_task`
    FOREIGN KEY (`task_id`) REFERENCES `teaching_tasks`(`id`);

ALTER TABLE `schedule_entries` ADD CONSTRAINT `fk_entry_semester`
    FOREIGN KEY (`semester_id`) REFERENCES `semesters`(`id`);

ALTER TABLE `schedule_adjustments` ADD CONSTRAINT `fk_adjustment_entry`
    FOREIGN KEY (`original_entry_id`) REFERENCES `schedule_entries`(`id`);

-- 教务: 教学班
ALTER TABLE `teaching_class_members` ADD CONSTRAINT `fk_member_class`
    FOREIGN KEY (`teaching_class_id`) REFERENCES `teaching_classes`(`id`) ON DELETE CASCADE;

-- 考试
ALTER TABLE `exam_batches` ADD CONSTRAINT `fk_exambatch_semester`
    FOREIGN KEY (`semester_id`) REFERENCES `semesters`(`id`);

ALTER TABLE `exam_arrangements` ADD CONSTRAINT `fk_arrangement_batch`
    FOREIGN KEY (`batch_id`) REFERENCES `exam_batches`(`id`) ON DELETE CASCADE;

ALTER TABLE `exam_arrangements` ADD CONSTRAINT `fk_arrangement_course`
    FOREIGN KEY (`course_id`) REFERENCES `courses`(`id`);

ALTER TABLE `exam_rooms` ADD CONSTRAINT `fk_room_arrangement`
    FOREIGN KEY (`arrangement_id`) REFERENCES `exam_arrangements`(`id`) ON DELETE CASCADE;

ALTER TABLE `exam_invigilators` ADD CONSTRAINT `fk_invigilator_room`
    FOREIGN KEY (`room_id`) REFERENCES `exam_rooms`(`id`) ON DELETE CASCADE;

-- 成绩
ALTER TABLE `grade_batches` ADD CONSTRAINT `fk_gradebatch_semester`
    FOREIGN KEY (`semester_id`) REFERENCES `semesters`(`id`);

ALTER TABLE `student_grades` ADD CONSTRAINT `fk_grade_task`
    FOREIGN KEY (`task_id`) REFERENCES `teaching_tasks`(`id`);

ALTER TABLE `student_grades` ADD CONSTRAINT `fk_grade_course`
    FOREIGN KEY (`course_id`) REFERENCES `courses`(`id`);
