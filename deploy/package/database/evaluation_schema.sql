-- =====================================================
-- 学生综合素质测评模块 - 数据库表结构
-- 版本: 1.0
-- 创建日期: 2025-11-28
-- =====================================================

-- 设置字符集
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================
-- 第一部分: 基础字典表
-- =====================================================

-- ---------------------------------------------------
-- 1.1 学期表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `semesters`;
CREATE TABLE `semesters` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `semester_code` VARCHAR(20) NOT NULL COMMENT '学期编码:2024-2025-1',
    `semester_name` VARCHAR(50) NOT NULL COMMENT '学期名称',
    `academic_year` VARCHAR(20) NOT NULL COMMENT '学年:2024-2025',
    `semester_type` TINYINT NOT NULL COMMENT '学期类型:1第一学期,2第二学期',
    `start_date` DATE NOT NULL COMMENT '开始日期',
    `end_date` DATE NOT NULL COMMENT '结束日期',
    `is_current` TINYINT DEFAULT 0 COMMENT '是否当前学期',
    `status` TINYINT DEFAULT 1 COMMENT '状态:1启用,0禁用',
    `created_by` BIGINT COMMENT '创建人',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT COMMENT '更新人',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_semester_code` (`semester_code`),
    INDEX `idx_academic_year` (`academic_year`),
    INDEX `idx_current` (`is_current`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学期表';

-- ---------------------------------------------------
-- 1.2 行为类型字典表 (量化与综测的桥梁)
-- ---------------------------------------------------
DROP TABLE IF EXISTS `behavior_types`;
CREATE TABLE `behavior_types` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `behavior_code` VARCHAR(50) NOT NULL COMMENT '行为编码',
    `behavior_name` VARCHAR(100) NOT NULL COMMENT '行为名称',
    `behavior_category` VARCHAR(30) NOT NULL COMMENT '行为大类:ATTENDANCE/DISCIPLINE/HYGIENE/STUDY/ACTIVITY/HONOR',
    `behavior_nature` TINYINT NOT NULL DEFAULT 2 COMMENT '行为性质:1正向,2负向,3中性',
    `default_affect_scope` TINYINT DEFAULT 1 COMMENT '默认影响范围:1仅当事人,2宿舍全员,3班级全员',
    `description` VARCHAR(500) COMMENT '行为描述',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT DEFAULT 1 COMMENT '状态:1启用,0禁用',
    `created_by` BIGINT COMMENT '创建人',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT COMMENT '更新人',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_behavior_code` (`behavior_code`),
    INDEX `idx_category` (`behavior_category`),
    INDEX `idx_nature` (`behavior_nature`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行为类型字典表';

-- ---------------------------------------------------
-- 1.3 综测维度配置表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `evaluation_dimensions`;
CREATE TABLE `evaluation_dimensions` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `dimension_code` VARCHAR(20) NOT NULL COMMENT '维度编码:MORAL/INTELLECTUAL/PHYSICAL/AESTHETIC/LABOR/DEVELOPMENT',
    `dimension_name` VARCHAR(50) NOT NULL COMMENT '维度名称',
    `weight` DECIMAL(5,2) NOT NULL COMMENT '权重百分比',
    `base_score` DECIMAL(5,2) NOT NULL DEFAULT 60.00 COMMENT '基础分',
    `max_bonus_score` DECIMAL(5,2) NOT NULL DEFAULT 40.00 COMMENT '奖励分上限',
    `min_total_score` DECIMAL(5,2) DEFAULT 0.00 COMMENT '维度最低分',
    `max_total_score` DECIMAL(5,2) DEFAULT 100.00 COMMENT '维度最高分',
    `calculation_formula` TEXT COMMENT '计算公式说明',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT DEFAULT 1 COMMENT '状态:1启用,0禁用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dimension_code` (`dimension_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='综测维度配置表';

-- ---------------------------------------------------
-- 1.4 行为-综测映射表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `behavior_evaluation_effects`;
CREATE TABLE `behavior_evaluation_effects` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `behavior_type_id` BIGINT NOT NULL COMMENT '行为类型ID',
    `evaluation_dimension` VARCHAR(20) NOT NULL COMMENT '综测维度',
    `score_type` TINYINT NOT NULL DEFAULT 1 COMMENT '计分类型:1固定分,2按次累计,3按次递增,4按等级映射',
    `base_score` DECIMAL(5,2) NOT NULL COMMENT '基础分数(正数加分,负数扣分)',
    `max_score` DECIMAL(5,2) COMMENT '累计上限(正向行为)',
    `min_score` DECIMAL(5,2) COMMENT '累计下限(负向行为)',
    `increment_score` DECIMAL(5,2) COMMENT '递增分数(用于按次递增)',
    `trigger_condition` JSON COMMENT '触发条件(如:次数>=3)',
    `level_score_mapping` JSON COMMENT '等级分数映射',
    `priority` INT DEFAULT 0 COMMENT '优先级(同一事迹取最高)',
    `effective_semesters` INT DEFAULT 1 COMMENT '有效学期数(1=仅当期)',
    `status` TINYINT DEFAULT 1 COMMENT '状态:1启用,0禁用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    INDEX `idx_behavior_type` (`behavior_type_id`),
    INDEX `idx_dimension` (`evaluation_dimension`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行为-综测映射表';

-- =====================================================
-- 第二部分: 快照与关联表
-- =====================================================

-- ---------------------------------------------------
-- 2.1 学生关系快照表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `student_relationship_snapshots`;
CREATE TABLE `student_relationship_snapshots` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `snapshot_date` DATE NOT NULL COMMENT '快照日期',
    `snapshot_type` VARCHAR(20) NOT NULL DEFAULT 'DAILY' COMMENT '快照类型:DAILY/SEMESTER_START/SEMESTER_END',
    `student_id` BIGINT NOT NULL COMMENT '学生ID',
    `student_no` VARCHAR(50) COMMENT '学号',
    `student_name` VARCHAR(50) COMMENT '姓名',
    `class_id` BIGINT COMMENT '班级ID',
    `class_name` VARCHAR(100) COMMENT '班级名称',
    `grade_id` BIGINT COMMENT '年级ID',
    `grade_name` VARCHAR(50) COMMENT '年级名称',
    `department_id` BIGINT COMMENT '系部ID',
    `department_name` VARCHAR(100) COMMENT '系部名称',
    `dormitory_id` BIGINT COMMENT '宿舍ID',
    `building_id` BIGINT COMMENT '楼栋ID',
    `building_name` VARCHAR(50) COMMENT '楼栋名称',
    `dormitory_no` VARCHAR(50) COMMENT '宿舍号',
    `bed_no` VARCHAR(20) COMMENT '床位号',
    `is_dorm_leader` TINYINT DEFAULT 0 COMMENT '是否宿舍长',
    `student_status` TINYINT COMMENT '学生状态:1在读,2休学,3退学,4毕业',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_date_student` (`snapshot_date`, `student_id`),
    INDEX `idx_snapshot_date` (`snapshot_date`),
    INDEX `idx_student` (`student_id`),
    INDEX `idx_class` (`class_id`),
    INDEX `idx_dormitory` (`dormitory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生关系快照表';

-- ---------------------------------------------------
-- 2.2 宿舍成员快照表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `dormitory_member_snapshots`;
CREATE TABLE `dormitory_member_snapshots` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `snapshot_date` DATE NOT NULL COMMENT '快照日期',
    `record_id` BIGINT COMMENT '关联的检查记录ID',
    `dormitory_id` BIGINT NOT NULL COMMENT '宿舍ID',
    `building_id` BIGINT COMMENT '楼栋ID',
    `building_name` VARCHAR(50) COMMENT '楼栋名称',
    `dormitory_no` VARCHAR(50) COMMENT '宿舍号',
    `members` JSON NOT NULL COMMENT '成员列表:[{studentId,studentNo,studentName,bedNo,isDormLeader}]',
    `member_count` INT DEFAULT 0 COMMENT '成员数量',
    `leader_id` BIGINT COMMENT '宿舍长ID',
    `leader_name` VARCHAR(50) COMMENT '宿舍长姓名',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_snapshot_date` (`snapshot_date`),
    INDEX `idx_dormitory` (`dormitory_id`),
    INDEX `idx_record` (`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='宿舍成员快照表';

-- ---------------------------------------------------
-- 2.3 扣分项-学生关联表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `check_record_item_students`;
CREATE TABLE `check_record_item_students` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `record_item_id` BIGINT NOT NULL COMMENT '扣分明细ID',
    `record_id` BIGINT NOT NULL COMMENT '检查记录ID',
    `check_date` DATE NOT NULL COMMENT '检查日期',
    `student_id` BIGINT NOT NULL COMMENT '学生ID',
    `student_no` VARCHAR(50) COMMENT '学号',
    `student_name` VARCHAR(50) COMMENT '姓名',
    `student_class_id` BIGINT COMMENT '班级ID',
    `student_class_name` VARCHAR(100) COMMENT '班级名称',
    `student_dormitory_id` BIGINT COMMENT '宿舍ID',
    `student_dormitory_no` VARCHAR(50) COMMENT '宿舍号',
    `behavior_type_id` BIGINT COMMENT '行为类型ID',
    `behavior_type_code` VARCHAR(50) COMMENT '行为编码',
    `behavior_type_name` VARCHAR(100) COMMENT '行为名称',
    `original_deduct_score` DECIMAL(5,2) COMMENT '原始扣分',
    `deduct_count` INT DEFAULT 1 COMMENT '扣分次数/人数',
    `moral_score` DECIMAL(5,2) DEFAULT 0.00 COMMENT '德育影响分',
    `intellectual_score` DECIMAL(5,2) DEFAULT 0.00 COMMENT '智育影响分',
    `physical_score` DECIMAL(5,2) DEFAULT 0.00 COMMENT '体育影响分',
    `aesthetic_score` DECIMAL(5,2) DEFAULT 0.00 COMMENT '美育影响分',
    `labor_score` DECIMAL(5,2) DEFAULT 0.00 COMMENT '劳育影响分',
    `development_score` DECIMAL(5,2) DEFAULT 0.00 COMMENT '发展素质影响分',
    `synced_to_evaluation` TINYINT DEFAULT 0 COMMENT '是否已同步到综测',
    `sync_time` DATETIME COMMENT '同步时间',
    `evaluation_period_id` BIGINT COMMENT '综测周期ID',
    `confirmed` TINYINT DEFAULT 0 COMMENT '是否已确认:0待确认,1已确认,2已排除',
    `confirmed_by` BIGINT COMMENT '确认人',
    `confirmed_at` DATETIME COMMENT '确认时间',
    `exclude_reason` VARCHAR(500) COMMENT '排除原因',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_record_item` (`record_item_id`),
    INDEX `idx_record` (`record_id`),
    INDEX `idx_student` (`student_id`),
    INDEX `idx_check_date` (`check_date`),
    INDEX `idx_behavior_type` (`behavior_type_id`),
    INDEX `idx_synced` (`synced_to_evaluation`),
    INDEX `idx_period` (`evaluation_period_id`),
    INDEX `idx_confirmed` (`confirmed`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='扣分项-学生关联表';

-- ---------------------------------------------------
-- 2.4 评级结果-学生影响表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `rating_result_student_effects`;
CREATE TABLE `rating_result_student_effects` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `rating_result_id` BIGINT NOT NULL COMMENT '评级结果ID',
    `record_id` BIGINT NOT NULL COMMENT '检查记录ID',
    `check_date` DATE NOT NULL COMMENT '检查日期',
    `rating_template_id` BIGINT COMMENT '评级模板ID',
    `rating_template_name` VARCHAR(100) COMMENT '评级模板名称',
    `level_id` BIGINT COMMENT '等级ID',
    `level_name` VARCHAR(50) NOT NULL COMMENT '等级名称(快照)',
    `level_code` VARCHAR(30) COMMENT '等级编码',
    `target_type` VARCHAR(20) NOT NULL COMMENT '评级对象类型:CLASS/DORMITORY',
    `target_id` BIGINT NOT NULL COMMENT '对象ID',
    `target_name` VARCHAR(100) COMMENT '对象名称',
    `student_id` BIGINT NOT NULL COMMENT '学生ID',
    `student_no` VARCHAR(50) COMMENT '学号',
    `student_name` VARCHAR(50) COMMENT '姓名',
    `student_dormitory_id` BIGINT COMMENT '学生宿舍ID(快照)',
    `student_dormitory_no` VARCHAR(50) COMMENT '学生宿舍号(快照)',
    `student_role` VARCHAR(20) DEFAULT 'MEMBER' COMMENT '学生角色:MEMBER/DORM_LEADER/CLASS_LEADER',
    `evaluation_effect_type` TINYINT NOT NULL DEFAULT 0 COMMENT '影响类型:0无影响,1加分,2扣分',
    `evaluation_dimension` VARCHAR(20) COMMENT '影响维度',
    `evaluation_base_score` DECIMAL(5,2) DEFAULT 0.00 COMMENT '基础分',
    `evaluation_extra_score` DECIMAL(5,2) DEFAULT 0.00 COMMENT '角色额外分',
    `evaluation_total_score` DECIMAL(5,2) DEFAULT 0.00 COMMENT '总影响分',
    `synced_to_evaluation` TINYINT DEFAULT 0 COMMENT '是否已同步到综测',
    `sync_time` DATETIME COMMENT '同步时间',
    `evaluation_period_id` BIGINT COMMENT '综测周期ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_rating_result` (`rating_result_id`),
    INDEX `idx_record` (`record_id`),
    INDEX `idx_student` (`student_id`),
    INDEX `idx_check_date` (`check_date`),
    INDEX `idx_target` (`target_type`, `target_id`),
    INDEX `idx_synced` (`synced_to_evaluation`),
    INDEX `idx_period` (`evaluation_period_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级结果-学生影响表';

-- =====================================================
-- 第三部分: 课程成绩管理表
-- =====================================================

-- ---------------------------------------------------
-- 3.1 课程表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `courses`;
CREATE TABLE `courses` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `course_code` VARCHAR(50) NOT NULL COMMENT '课程编码',
    `course_name` VARCHAR(100) NOT NULL COMMENT '课程名称',
    `course_type` TINYINT NOT NULL COMMENT '课程类型:1必修,2选修,3实践',
    `credit` DECIMAL(3,1) NOT NULL COMMENT '学分',
    `total_hours` INT COMMENT '总学时',
    `department_id` BIGINT COMMENT '开课系部',
    `description` TEXT COMMENT '课程描述',
    `status` TINYINT DEFAULT 1 COMMENT '状态:1启用,0禁用',
    `created_by` BIGINT COMMENT '创建人',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT COMMENT '更新人',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    INDEX `idx_course_code` (`course_code`),
    INDEX `idx_department` (`department_id`),
    INDEX `idx_type` (`course_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程表';

-- ---------------------------------------------------
-- 3.2 班级课程安排表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `class_course_arrangements`;
CREATE TABLE `class_course_arrangements` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `semester_id` BIGINT NOT NULL COMMENT '学期ID',
    `class_id` BIGINT NOT NULL COMMENT '班级ID',
    `course_id` BIGINT NOT NULL COMMENT '课程ID',
    `teacher_id` BIGINT COMMENT '任课教师ID',
    `teacher_name` VARCHAR(50) COMMENT '任课教师姓名',
    `status` TINYINT DEFAULT 1 COMMENT '状态:1启用,0禁用',
    `created_by` BIGINT COMMENT '创建人',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT COMMENT '更新人',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_semester_class_course` (`semester_id`, `class_id`, `course_id`),
    INDEX `idx_semester` (`semester_id`),
    INDEX `idx_class` (`class_id`),
    INDEX `idx_course` (`course_id`),
    INDEX `idx_teacher` (`teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级课程安排表';

-- ---------------------------------------------------
-- 3.3 学生成绩表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `student_scores`;
CREATE TABLE `student_scores` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `semester_id` BIGINT NOT NULL COMMENT '学期ID',
    `student_id` BIGINT NOT NULL COMMENT '学生ID',
    `course_id` BIGINT NOT NULL COMMENT '课程ID',
    `arrangement_id` BIGINT COMMENT '课程安排ID',
    `regular_score` DECIMAL(5,2) COMMENT '平时成绩',
    `regular_weight` DECIMAL(3,2) DEFAULT 0.30 COMMENT '平时成绩权重',
    `midterm_score` DECIMAL(5,2) COMMENT '期中成绩',
    `midterm_weight` DECIMAL(3,2) DEFAULT 0.20 COMMENT '期中成绩权重',
    `final_score` DECIMAL(5,2) COMMENT '期末成绩',
    `final_weight` DECIMAL(3,2) DEFAULT 0.50 COMMENT '期末成绩权重',
    `total_score` DECIMAL(5,2) COMMENT '总评成绩',
    `makeup_score` DECIMAL(5,2) COMMENT '补考成绩',
    `retake_score` DECIMAL(5,2) COMMENT '重修成绩',
    `final_total_score` DECIMAL(5,2) COMMENT '最终成绩(含补考重修)',
    `grade_point` DECIMAL(3,2) COMMENT '绩点',
    `credit` DECIMAL(3,1) COMMENT '学分',
    `credit_point` DECIMAL(5,2) COMMENT '学分绩点',
    `score_status` TINYINT DEFAULT 0 COMMENT '成绩状态:0未录入,1已录入,2已确认,3缓考,4免修,5缺考',
    `is_passed` TINYINT COMMENT '是否及格',
    `remarks` VARCHAR(500) COMMENT '备注',
    `input_by` BIGINT COMMENT '录入人',
    `input_at` DATETIME COMMENT '录入时间',
    `confirmed_by` BIGINT COMMENT '确认人',
    `confirmed_at` DATETIME COMMENT '确认时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_semester_student_course` (`semester_id`, `student_id`, `course_id`),
    INDEX `idx_semester` (`semester_id`),
    INDEX `idx_student` (`student_id`),
    INDEX `idx_course` (`course_id`),
    INDEX `idx_status` (`score_status`),
    INDEX `idx_passed` (`is_passed`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生成绩表';

-- ---------------------------------------------------
-- 3.4 学生学期成绩汇总表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `student_semester_score_summary`;
CREATE TABLE `student_semester_score_summary` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `semester_id` BIGINT NOT NULL COMMENT '学期ID',
    `student_id` BIGINT NOT NULL COMMENT '学生ID',
    `total_courses` INT DEFAULT 0 COMMENT '总课程数',
    `passed_courses` INT DEFAULT 0 COMMENT '及格课程数',
    `failed_courses` INT DEFAULT 0 COMMENT '不及格课程数',
    `total_credit` DECIMAL(5,1) DEFAULT 0.0 COMMENT '总学分',
    `earned_credit` DECIMAL(5,1) DEFAULT 0.0 COMMENT '已获学分',
    `average_score` DECIMAL(5,2) COMMENT '平均分',
    `weighted_average_score` DECIMAL(5,2) COMMENT '加权平均分',
    `gpa` DECIMAL(4,2) COMMENT '平均绩点',
    `class_rank` INT COMMENT '班级排名',
    `class_total` INT COMMENT '班级总人数',
    `grade_rank` INT COMMENT '年级排名',
    `grade_total` INT COMMENT '年级总人数',
    `intellectual_base_score` DECIMAL(5,2) COMMENT '智育基础分',
    `intellectual_bonus_score` DECIMAL(5,2) COMMENT '智育奖励分',
    `intellectual_deduct_score` DECIMAL(5,2) COMMENT '智育扣分',
    `intellectual_total_score` DECIMAL(5,2) COMMENT '智育总分',
    `calculated_at` DATETIME COMMENT '计算时间',
    `status` TINYINT DEFAULT 0 COMMENT '状态:0待计算,1已计算,2已锁定',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_semester_student` (`semester_id`, `student_id`),
    INDEX `idx_semester` (`semester_id`),
    INDEX `idx_student` (`student_id`),
    INDEX `idx_gpa` (`gpa`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生学期成绩汇总表';

-- =====================================================
-- 第四部分: 荣誉与处分管理表
-- =====================================================

-- ---------------------------------------------------
-- 4.1 荣誉类型字典表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `honor_types`;
CREATE TABLE `honor_types` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `type_code` VARCHAR(50) NOT NULL COMMENT '类型编码',
    `type_name` VARCHAR(100) NOT NULL COMMENT '类型名称',
    `category` VARCHAR(30) NOT NULL COMMENT '类别:COMPETITION/CERTIFICATE/TITLE/ACTIVITY/PUBLICATION/OTHER',
    `evaluation_dimension` VARCHAR(20) NOT NULL COMMENT '影响维度',
    `description` VARCHAR(500) COMMENT '描述',
    `required_attachments` TINYINT DEFAULT 0 COMMENT '是否必须上传附件',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT DEFAULT 1 COMMENT '状态:1启用,0禁用',
    `created_by` BIGINT COMMENT '创建人',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT COMMENT '更新人',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_type_code` (`type_code`),
    INDEX `idx_category` (`category`),
    INDEX `idx_dimension` (`evaluation_dimension`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='荣誉类型字典表';

-- ---------------------------------------------------
-- 4.2 荣誉等级配置表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `honor_level_configs`;
CREATE TABLE `honor_level_configs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `honor_type_id` BIGINT NOT NULL COMMENT '荣誉类型ID',
    `level_code` VARCHAR(30) NOT NULL COMMENT '级别编码:NATIONAL/PROVINCIAL/CITY/SCHOOL/DEPARTMENT',
    `level_name` VARCHAR(50) NOT NULL COMMENT '级别名称',
    `rank_code` VARCHAR(30) COMMENT '名次编码:FIRST/SECOND/THIRD/EXCELLENCE/PARTICIPATION',
    `rank_name` VARCHAR(50) COMMENT '名次名称',
    `score` DECIMAL(5,2) NOT NULL COMMENT '加分分值',
    `max_count` INT COMMENT '同类最大计次(null不限)',
    `priority` INT DEFAULT 0 COMMENT '优先级(同一事迹取最高)',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT DEFAULT 1 COMMENT '状态:1启用,0禁用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    INDEX `idx_honor_type` (`honor_type_id`),
    INDEX `idx_level` (`level_code`),
    INDEX `idx_rank` (`rank_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='荣誉等级配置表';

-- ---------------------------------------------------
-- 4.3 学生荣誉申报表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `student_honor_applications`;
CREATE TABLE `student_honor_applications` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `application_code` VARCHAR(50) NOT NULL COMMENT '申报编号',
    `semester_id` BIGINT NOT NULL COMMENT '学期ID',
    `evaluation_period_id` BIGINT COMMENT '综测周期ID',
    `student_id` BIGINT NOT NULL COMMENT '学生ID',
    `honor_type_id` BIGINT NOT NULL COMMENT '荣誉类型ID',
    `honor_level_config_id` BIGINT COMMENT '荣誉等级配置ID',
    `honor_name` VARCHAR(200) NOT NULL COMMENT '荣誉名称/竞赛名称',
    `honor_level` VARCHAR(30) NOT NULL COMMENT '级别',
    `honor_rank` VARCHAR(30) COMMENT '名次/等级',
    `honor_date` DATE COMMENT '获得日期',
    `issuing_authority` VARCHAR(200) COMMENT '颁发机构',
    `certificate_no` VARCHAR(100) COMMENT '证书编号',
    `description` TEXT COMMENT '详细描述',
    `attachments` JSON COMMENT '附件列表:[{fileName,fileUrl,fileSize}]',
    `related_application_id` BIGINT COMMENT '关联申报ID(同一事迹更高级别)',
    `is_highest_level` TINYINT DEFAULT 1 COMMENT '是否该事迹最高级别',
    `same_event_group` VARCHAR(50) COMMENT '同一事迹分组标识',
    `expected_score` DECIMAL(5,2) COMMENT '预期得分',
    `actual_score` DECIMAL(5,2) COMMENT '实际得分',
    `evaluation_dimension` VARCHAR(20) COMMENT '影响维度',
    `status` TINYINT DEFAULT 0 COMMENT '状态:0待提交,1待班级审核,2待系部审核,3已通过,4已驳回,5已撤销',
    `current_step` INT DEFAULT 1 COMMENT '当前审核步骤',
    `class_reviewer_id` BIGINT COMMENT '班级审核人',
    `class_review_time` DATETIME COMMENT '班级审核时间',
    `class_review_opinion` VARCHAR(500) COMMENT '班级审核意见',
    `class_review_status` TINYINT COMMENT '班级审核结果:1通过,2驳回',
    `dept_reviewer_id` BIGINT COMMENT '系部审核人',
    `dept_review_time` DATETIME COMMENT '系部审核时间',
    `dept_review_opinion` VARCHAR(500) COMMENT '系部审核意见',
    `dept_review_status` TINYINT COMMENT '系部审核结果:1通过,2驳回',
    `synced_to_evaluation` TINYINT DEFAULT 0 COMMENT '是否已同步到综测',
    `sync_time` DATETIME COMMENT '同步时间',
    `applicant_type` TINYINT DEFAULT 1 COMMENT '申报人类型:1学生自主,2班主任代录,3系部录入',
    `submitted_at` DATETIME COMMENT '提交时间',
    `created_by` BIGINT COMMENT '创建人',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT COMMENT '更新人',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_application_code` (`application_code`),
    INDEX `idx_semester` (`semester_id`),
    INDEX `idx_student` (`student_id`),
    INDEX `idx_honor_type` (`honor_type_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_period` (`evaluation_period_id`),
    INDEX `idx_same_event` (`same_event_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生荣誉申报表';

-- ---------------------------------------------------
-- 4.4 学生处分表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `student_punishments`;
CREATE TABLE `student_punishments` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `punishment_code` VARCHAR(50) NOT NULL COMMENT '处分编号',
    `student_id` BIGINT NOT NULL COMMENT '学生ID',
    `punishment_type` TINYINT NOT NULL COMMENT '处分类型:1警告,2严重警告,3记过,4留校察看,5开除学籍',
    `punishment_name` VARCHAR(100) NOT NULL COMMENT '处分名称',
    `reason` TEXT NOT NULL COMMENT '处分原因',
    `decision_date` DATE NOT NULL COMMENT '处分决定日期',
    `effective_date` DATE NOT NULL COMMENT '生效日期',
    `revoked` TINYINT DEFAULT 0 COMMENT '是否已解除',
    `revoke_date` DATE COMMENT '解除日期',
    `revoke_reason` VARCHAR(500) COMMENT '解除原因',
    `evaluation_effect_type` TINYINT DEFAULT 1 COMMENT '综测影响类型:1限制上限,2直接扣分',
    `moral_score_cap` DECIMAL(5,2) COMMENT '德育分上限',
    `moral_deduct_score` DECIMAL(5,2) COMMENT '德育扣分',
    `document_no` VARCHAR(100) COMMENT '文件号',
    `attachments` JSON COMMENT '附件列表',
    `remarks` VARCHAR(500) COMMENT '备注',
    `input_by` BIGINT COMMENT '录入人',
    `input_at` DATETIME COMMENT '录入时间',
    `status` TINYINT DEFAULT 1 COMMENT '状态:1有效,0无效',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_punishment_code` (`punishment_code`),
    INDEX `idx_student` (`student_id`),
    INDEX `idx_type` (`punishment_type`),
    INDEX `idx_effective_date` (`effective_date`),
    INDEX `idx_revoked` (`revoked`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生处分表';

-- =====================================================
-- 第五部分: 综测评定核心表
-- =====================================================

-- ---------------------------------------------------
-- 5.1 综测评定周期表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `evaluation_periods`;
CREATE TABLE `evaluation_periods` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `period_code` VARCHAR(50) NOT NULL COMMENT '周期编码',
    `period_name` VARCHAR(100) NOT NULL COMMENT '周期名称',
    `semester_id` BIGINT NOT NULL COMMENT '学期ID',
    `academic_year` VARCHAR(20) NOT NULL COMMENT '学年',
    `semester_type` TINYINT NOT NULL COMMENT '学期类型',
    `data_start_date` DATE NOT NULL COMMENT '数据采集开始日期',
    `data_end_date` DATE NOT NULL COMMENT '数据采集截止日期',
    `application_start_date` DATE COMMENT '荣誉申报开始日期',
    `application_end_date` DATE COMMENT '荣誉申报截止日期',
    `review_start_date` DATE COMMENT '审核开始日期',
    `review_end_date` DATE COMMENT '审核截止日期',
    `publicity_start_date` DATE COMMENT '公示开始日期',
    `publicity_end_date` DATE COMMENT '公示结束日期',
    `appeal_end_date` DATE COMMENT '申诉截止日期',
    `status` TINYINT DEFAULT 0 COMMENT '状态:0未开始,1数据采集中,2荣誉申报中,3审核中,4公示中,5申诉处理中,6已结束',
    `is_locked` TINYINT DEFAULT 0 COMMENT '是否锁定',
    `locked_at` DATETIME COMMENT '锁定时间',
    `locked_by` BIGINT COMMENT '锁定人',
    `dimension_weights` JSON COMMENT '维度权重配置(可覆盖默认)',
    `special_configs` JSON COMMENT '特殊配置',
    `remarks` VARCHAR(500) COMMENT '备注',
    `created_by` BIGINT COMMENT '创建人',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT COMMENT '更新人',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_period_code` (`period_code`),
    INDEX `idx_semester` (`semester_id`),
    INDEX `idx_academic_year` (`academic_year`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='综测评定周期表';

-- ---------------------------------------------------
-- 5.2 学生综测结果主表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `student_evaluation_results`;
CREATE TABLE `student_evaluation_results` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `evaluation_period_id` BIGINT NOT NULL COMMENT '综测周期ID',
    `semester_id` BIGINT NOT NULL COMMENT '学期ID',
    `student_id` BIGINT NOT NULL COMMENT '学生ID',
    `student_no` VARCHAR(50) COMMENT '学号',
    `student_name` VARCHAR(50) COMMENT '姓名',
    `class_id` BIGINT COMMENT '班级ID',
    `class_name` VARCHAR(100) COMMENT '班级名称',
    `grade_id` BIGINT COMMENT '年级ID',
    `department_id` BIGINT COMMENT '系部ID',
    -- 德育
    `moral_base_score` DECIMAL(5,2) DEFAULT 60.00 COMMENT '德育基础分',
    `moral_bonus_score` DECIMAL(5,2) DEFAULT 0.00 COMMENT '德育奖励分',
    `moral_deduct_score` DECIMAL(5,2) DEFAULT 0.00 COMMENT '德育扣分',
    `moral_total_score` DECIMAL(5,2) DEFAULT 60.00 COMMENT '德育总分',
    `moral_weighted_score` DECIMAL(5,2) COMMENT '德育加权分',
    -- 智育
    `intellectual_base_score` DECIMAL(5,2) DEFAULT 60.00 COMMENT '智育基础分',
    `intellectual_bonus_score` DECIMAL(5,2) DEFAULT 0.00 COMMENT '智育奖励分',
    `intellectual_deduct_score` DECIMAL(5,2) DEFAULT 0.00 COMMENT '智育扣分',
    `intellectual_total_score` DECIMAL(5,2) DEFAULT 60.00 COMMENT '智育总分',
    `intellectual_weighted_score` DECIMAL(5,2) COMMENT '智育加权分',
    -- 体育
    `physical_base_score` DECIMAL(5,2) DEFAULT 60.00 COMMENT '体育基础分',
    `physical_bonus_score` DECIMAL(5,2) DEFAULT 0.00 COMMENT '体育奖励分',
    `physical_deduct_score` DECIMAL(5,2) DEFAULT 0.00 COMMENT '体育扣分',
    `physical_total_score` DECIMAL(5,2) DEFAULT 60.00 COMMENT '体育总分',
    `physical_weighted_score` DECIMAL(5,2) COMMENT '体育加权分',
    -- 美育
    `aesthetic_base_score` DECIMAL(5,2) DEFAULT 60.00 COMMENT '美育基础分',
    `aesthetic_bonus_score` DECIMAL(5,2) DEFAULT 0.00 COMMENT '美育奖励分',
    `aesthetic_deduct_score` DECIMAL(5,2) DEFAULT 0.00 COMMENT '美育扣分',
    `aesthetic_total_score` DECIMAL(5,2) DEFAULT 60.00 COMMENT '美育总分',
    `aesthetic_weighted_score` DECIMAL(5,2) COMMENT '美育加权分',
    -- 劳育
    `labor_base_score` DECIMAL(5,2) DEFAULT 60.00 COMMENT '劳育基础分',
    `labor_bonus_score` DECIMAL(5,2) DEFAULT 0.00 COMMENT '劳育奖励分',
    `labor_deduct_score` DECIMAL(5,2) DEFAULT 0.00 COMMENT '劳育扣分',
    `labor_total_score` DECIMAL(5,2) DEFAULT 60.00 COMMENT '劳育总分',
    `labor_weighted_score` DECIMAL(5,2) COMMENT '劳育加权分',
    -- 发展素质
    `development_base_score` DECIMAL(5,2) DEFAULT 60.00 COMMENT '发展素质基础分',
    `development_bonus_score` DECIMAL(5,2) DEFAULT 0.00 COMMENT '发展素质奖励分',
    `development_deduct_score` DECIMAL(5,2) DEFAULT 0.00 COMMENT '发展素质扣分',
    `development_total_score` DECIMAL(5,2) DEFAULT 60.00 COMMENT '发展素质总分',
    `development_weighted_score` DECIMAL(5,2) COMMENT '发展素质加权分',
    -- 综合得分
    `total_score` DECIMAL(6,2) COMMENT '综测总分',
    -- 排名
    `class_rank` INT COMMENT '班级排名',
    `class_total` INT COMMENT '班级参评人数',
    `grade_rank` INT COMMENT '年级排名',
    `grade_total` INT COMMENT '年级参评人数',
    `department_rank` INT COMMENT '系部排名',
    `department_total` INT COMMENT '系部参评人数',
    -- 处分影响
    `has_punishment` TINYINT DEFAULT 0 COMMENT '是否有处分',
    `punishment_type` TINYINT COMMENT '处分类型',
    `punishment_effect` VARCHAR(200) COMMENT '处分影响说明',
    -- 状态
    `status` TINYINT DEFAULT 0 COMMENT '状态:0待计算,1已计算,2待审核,3已审核,4公示中,5已生效,6已归档',
    `version` INT DEFAULT 1 COMMENT '版本号',
    `calculated_at` DATETIME COMMENT '计算时间',
    `reviewed_by` BIGINT COMMENT '审核人',
    `reviewed_at` DATETIME COMMENT '审核时间',
    `remarks` VARCHAR(500) COMMENT '备注',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_period_student` (`evaluation_period_id`, `student_id`),
    INDEX `idx_period` (`evaluation_period_id`),
    INDEX `idx_semester` (`semester_id`),
    INDEX `idx_student` (`student_id`),
    INDEX `idx_class` (`class_id`),
    INDEX `idx_total_score` (`total_score`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生综测结果主表';

-- ---------------------------------------------------
-- 5.3 学生综测明细表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `student_evaluation_details`;
CREATE TABLE `student_evaluation_details` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `result_id` BIGINT NOT NULL COMMENT '综测结果ID',
    `evaluation_period_id` BIGINT NOT NULL COMMENT '综测周期ID',
    `student_id` BIGINT NOT NULL COMMENT '学生ID',
    `detail_type` VARCHAR(30) NOT NULL COMMENT '明细类型:QUANTIFICATION/RATING/HONOR/PUNISHMENT/SCORE/MANUAL',
    `evaluation_dimension` VARCHAR(20) NOT NULL COMMENT '影响维度',
    `score_category` VARCHAR(20) NOT NULL COMMENT '分数类别:BASE/BONUS/DEDUCT',
    `source_type` VARCHAR(30) NOT NULL COMMENT '数据来源:CHECK_ITEM/RATING_RESULT/HONOR_APPLICATION/PUNISHMENT/ACADEMIC_SCORE/MANUAL_INPUT',
    `source_id` BIGINT COMMENT '来源记录ID',
    `source_code` VARCHAR(100) COMMENT '来源编码',
    `source_name` VARCHAR(200) COMMENT '来源名称/描述',
    `source_date` DATE COMMENT '发生日期',
    `score` DECIMAL(5,2) NOT NULL COMMENT '分数(正数加分,负数扣分)',
    `score_before_cap` DECIMAL(5,2) COMMENT '封顶前分数',
    `is_capped` TINYINT DEFAULT 0 COMMENT '是否被封顶',
    `cap_reason` VARCHAR(200) COMMENT '封顶原因',
    `behavior_type_id` BIGINT COMMENT '行为类型ID',
    `behavior_type_code` VARCHAR(50) COMMENT '行为编码',
    `confirmed` TINYINT DEFAULT 1 COMMENT '是否确认有效',
    `excluded` TINYINT DEFAULT 0 COMMENT '是否被排除',
    `exclude_reason` VARCHAR(500) COMMENT '排除原因',
    `remarks` VARCHAR(500) COMMENT '备注',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_result` (`result_id`),
    INDEX `idx_period` (`evaluation_period_id`),
    INDEX `idx_student` (`student_id`),
    INDEX `idx_dimension` (`evaluation_dimension`),
    INDEX `idx_detail_type` (`detail_type`),
    INDEX `idx_source` (`source_type`, `source_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生综测明细表';

-- ---------------------------------------------------
-- 5.4 综测结果修改历史表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `evaluation_result_history`;
CREATE TABLE `evaluation_result_history` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `result_id` BIGINT NOT NULL COMMENT '综测结果ID',
    `version` INT NOT NULL COMMENT '版本号',
    `old_data` JSON NOT NULL COMMENT '修改前数据',
    `new_data` JSON NOT NULL COMMENT '修改后数据',
    `changed_fields` JSON COMMENT '变更字段列表',
    `change_type` VARCHAR(30) NOT NULL COMMENT '修改类型:RECALCULATE/MANUAL_ADJUST/APPEAL_ADJUST/ERROR_CORRECT',
    `change_reason` VARCHAR(500) NOT NULL COMMENT '修改原因',
    `related_appeal_id` BIGINT COMMENT '关联申诉ID',
    `changed_by` BIGINT NOT NULL COMMENT '修改人ID',
    `changed_by_name` VARCHAR(50) COMMENT '修改人姓名',
    `changed_at` DATETIME NOT NULL COMMENT '修改时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_result` (`result_id`),
    INDEX `idx_version` (`version`),
    INDEX `idx_changed_at` (`changed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='综测结果修改历史表';

-- ---------------------------------------------------
-- 5.5 综测申诉表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `evaluation_appeals`;
CREATE TABLE `evaluation_appeals` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `appeal_code` VARCHAR(50) NOT NULL COMMENT '申诉编号',
    `evaluation_period_id` BIGINT NOT NULL COMMENT '综测周期ID',
    `result_id` BIGINT NOT NULL COMMENT '综测结果ID',
    `student_id` BIGINT NOT NULL COMMENT '学生ID',
    `appeal_type` TINYINT NOT NULL COMMENT '申诉类型:1分数异议,2数据缺失,3数据错误,4其他',
    `appeal_dimension` VARCHAR(20) COMMENT '申诉维度',
    `appeal_detail_ids` JSON COMMENT '申诉的明细ID列表',
    `current_score` DECIMAL(5,2) COMMENT '当前分数',
    `expected_score` DECIMAL(5,2) COMMENT '期望分数',
    `reason` TEXT NOT NULL COMMENT '申诉理由',
    `evidence` TEXT COMMENT '佐证材料说明',
    `attachments` JSON COMMENT '附件列表',
    `status` TINYINT DEFAULT 0 COMMENT '状态:0待处理,1处理中,2已通过,3已驳回,4已撤销',
    `handler_id` BIGINT COMMENT '处理人ID',
    `handler_name` VARCHAR(50) COMMENT '处理人姓名',
    `handle_time` DATETIME COMMENT '处理时间',
    `handle_opinion` TEXT COMMENT '处理意见',
    `adjusted_score` DECIMAL(5,2) COMMENT '调整后分数',
    `submitted_at` DATETIME COMMENT '提交时间',
    `created_by` BIGINT COMMENT '创建人',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_appeal_code` (`appeal_code`),
    INDEX `idx_period` (`evaluation_period_id`),
    INDEX `idx_result` (`result_id`),
    INDEX `idx_student` (`student_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='综测申诉表';

-- =====================================================
-- 第六部分: 特殊配置表
-- =====================================================

-- ---------------------------------------------------
-- 6.1 学期特殊配置表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `semester_special_configs`;
CREATE TABLE `semester_special_configs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `semester_id` BIGINT NOT NULL COMMENT '学期ID',
    `evaluation_period_id` BIGINT COMMENT '综测周期ID',
    `config_type` VARCHAR(30) NOT NULL COMMENT '配置类型:DIMENSION_SKIP/WEIGHT_ADJUST/SCORE_ADJUST',
    `config_scope` VARCHAR(20) DEFAULT 'ALL' COMMENT '配置范围:ALL/DEPARTMENT/CLASS/STUDENT',
    `scope_id` BIGINT COMMENT '范围对象ID',
    `config_value` JSON NOT NULL COMMENT '配置值',
    `reason` VARCHAR(500) COMMENT '配置原因',
    `effective_start` DATE COMMENT '生效开始日期',
    `effective_end` DATE COMMENT '生效结束日期',
    `status` TINYINT DEFAULT 1 COMMENT '状态:1启用,0禁用',
    `created_by` BIGINT COMMENT '创建人',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_semester` (`semester_id`),
    INDEX `idx_period` (`evaluation_period_id`),
    INDEX `idx_type` (`config_type`),
    INDEX `idx_scope` (`config_scope`, `scope_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学期特殊配置表';

-- =====================================================
-- 第七部分: 扩展现有量化系统表
-- =====================================================

-- 为 check_items 表添加行为类型关联字段
-- ALTER TABLE `check_items` ADD COLUMN `behavior_type_id` BIGINT COMMENT '行为类型ID' AFTER `status`;
-- ALTER TABLE `check_items` ADD INDEX `idx_behavior_type` (`behavior_type_id`);

-- 为 rating_levels 表添加综测影响配置字段
-- ALTER TABLE `rating_levels` ADD COLUMN `evaluation_effect_type` TINYINT DEFAULT 0 COMMENT '综测影响:0无,1加分,2扣分' AFTER `color`;
-- ALTER TABLE `rating_levels` ADD COLUMN `evaluation_dimension` VARCHAR(20) COMMENT '影响维度' AFTER `evaluation_effect_type`;
-- ALTER TABLE `rating_levels` ADD COLUMN `evaluation_score` DECIMAL(5,2) COMMENT '影响分数' AFTER `evaluation_dimension`;
-- ALTER TABLE `rating_levels` ADD COLUMN `special_role_effects` JSON COMMENT '特殊角色配置:[{role,extraScore}]' AFTER `evaluation_score`;

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================
-- 完成
-- =====================================================
