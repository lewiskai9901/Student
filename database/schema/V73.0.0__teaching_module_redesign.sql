-- ============================================================
-- Migration: V73.0.0__teaching_module_redesign.sql
-- Description: 教务模块重构 - 新增学期开课、教学班、排课约束、冲突记录表
--              并扩展 schedule_entries 支持教学班和连排分组
-- ============================================================

-- -----------------------------------------------------------
-- 1. semester_course_offerings (学期开课计划表)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `semester_course_offerings` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT,
    `semester_id`         BIGINT       NOT NULL COMMENT '学期ID',
    `course_id`           BIGINT       NOT NULL COMMENT '课程ID',
    `applicable_grade`    VARCHAR(50)  NULL     COMMENT '适用年级',
    `weekly_hours`        INT          NOT NULL COMMENT '周课时数',
    `total_weeks`         INT          NULL     COMMENT '总周数',
    `start_week`          INT          NOT NULL DEFAULT 1 COMMENT '起始周',
    `end_week`            INT          NULL     COMMENT '结束周',
    `course_category`     TINYINT      NULL     COMMENT '课程类别',
    `course_type`         TINYINT      NULL     COMMENT '课程性质',
    `allow_combined`      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否允许合堂',
    `max_combined_classes` INT         NOT NULL DEFAULT 2 COMMENT '最大合堂班数',
    `allow_walking`       TINYINT      NOT NULL DEFAULT 0 COMMENT '是否允许走班',
    `status`              TINYINT      NOT NULL DEFAULT 0 COMMENT '0草稿 1已确认 2已完成分配',
    `remark`              VARCHAR(500) NULL,
    `created_by`          BIGINT       NULL,
    `created_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`             TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_semester_course_grade` (`semester_id`, `course_id`, `applicable_grade`),
    INDEX `idx_semester` (`semester_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='学期开课计划表';

-- -----------------------------------------------------------
-- 2. class_course_assignments (班级开课表)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `class_course_assignments` (
    `id`            BIGINT  NOT NULL AUTO_INCREMENT,
    `semester_id`   BIGINT  NOT NULL COMMENT '学期ID',
    `class_id`      BIGINT  NOT NULL COMMENT '行政班ID',
    `offering_id`   BIGINT  NOT NULL COMMENT '学期开课计划ID',
    `course_id`     BIGINT  NOT NULL COMMENT '课程ID',
    `weekly_hours`  INT     NOT NULL COMMENT '周课时',
    `student_count` INT     NULL     COMMENT '选课人数',
    `status`        TINYINT NOT NULL DEFAULT 0 COMMENT '0待确认 1已确认',
    `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_class_course` (`semester_id`, `class_id`, `course_id`),
    INDEX `idx_semester_class` (`semester_id`, `class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='班级开课表';

-- -----------------------------------------------------------
-- 3. teaching_classes (教学班表)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `teaching_classes` (
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT,
    `semester_id`        BIGINT       NOT NULL COMMENT '学期ID',
    `class_name`         VARCHAR(100) NOT NULL COMMENT '教学班名称',
    `class_code`         VARCHAR(50)  NULL     COMMENT '教学班编号',
    `course_id`          BIGINT       NOT NULL COMMENT '课程ID',
    `class_type`         TINYINT      NOT NULL DEFAULT 1 COMMENT '1普通 2合堂 3走班',
    `weekly_hours`       INT          NOT NULL COMMENT '周课时数',
    `student_count`      INT          NOT NULL DEFAULT 0 COMMENT '学生数',
    `required_room_type` VARCHAR(50)  NULL     COMMENT '教室类型要求',
    `required_capacity`  INT          NULL     COMMENT '教室容量要求',
    `start_week`         INT          NOT NULL DEFAULT 1 COMMENT '起始周',
    `end_week`           INT          NULL     COMMENT '结束周',
    `status`             TINYINT      NOT NULL DEFAULT 1 COMMENT '1有效 0无效',
    `remark`             VARCHAR(500) NULL,
    `created_by`         BIGINT       NULL,
    `created_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`            TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_semester` (`semester_id`),
    INDEX `idx_course` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='教学班表';

-- -----------------------------------------------------------
-- 4. teaching_class_members (教学班成员表 - 物理删除)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `teaching_class_members` (
    `id`                BIGINT  NOT NULL AUTO_INCREMENT,
    `teaching_class_id` BIGINT  NOT NULL COMMENT '教学班ID',
    `member_type`       TINYINT NOT NULL COMMENT '1整班 2个人',
    `admin_class_id`    BIGINT  NULL     COMMENT '行政班ID',
    `student_id`        BIGINT  NULL     COMMENT '学生ID',
    `created_at`        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_teaching_class` (`teaching_class_id`),
    INDEX `idx_admin_class` (`admin_class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='教学班成员表';

-- -----------------------------------------------------------
-- 5. scheduling_constraints (排课约束规则表)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `scheduling_constraints` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT,
    `semester_id`      BIGINT       NOT NULL COMMENT '学期ID',
    `constraint_name`  VARCHAR(100) NOT NULL COMMENT '约束名称',
    `constraint_level` TINYINT      NOT NULL COMMENT '1全局 2教师 3班级 4课程',
    `target_id`        BIGINT       NULL     COMMENT '目标ID',
    `target_name`      VARCHAR(100) NULL     COMMENT '目标名称',
    `constraint_type`  VARCHAR(50)  NOT NULL COMMENT '约束类型枚举',
    `is_hard`          TINYINT      NOT NULL DEFAULT 1 COMMENT '1硬约束 0软约束',
    `priority`         INT          NOT NULL DEFAULT 50 COMMENT '优先级权重(1-100)',
    `params`           JSON         NOT NULL COMMENT '约束参数',
    `effective_weeks`  VARCHAR(100) NULL     COMMENT '生效周次',
    `enabled`          TINYINT      NOT NULL DEFAULT 1,
    `created_by`       BIGINT       NULL,
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`          TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_semester` (`semester_id`),
    INDEX `idx_level_target` (`constraint_level`, `target_id`),
    INDEX `idx_type` (`constraint_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='排课约束规则表';

-- -----------------------------------------------------------
-- 6. schedule_conflict_records (排课冲突记录表 - 物理删除)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `schedule_conflict_records` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT,
    `semester_id`         BIGINT       NOT NULL COMMENT '学期ID',
    `detection_batch`     VARCHAR(50)  NULL     COMMENT '检测批次号',
    `conflict_category`   TINYINT      NOT NULL COMMENT '1资源冲突 2约束冲突 3软冲突',
    `conflict_type`       VARCHAR(50)  NOT NULL COMMENT '冲突类型',
    `severity`            TINYINT      NOT NULL COMMENT '1阻塞 2警告 3提示',
    `description`         VARCHAR(500) NOT NULL COMMENT '冲突描述',
    `detail`              JSON         NULL     COMMENT '冲突详情',
    `entry_id_1`          BIGINT       NULL     COMMENT '排课条目1',
    `entry_id_2`          BIGINT       NULL     COMMENT '排课条目2',
    `constraint_id`       BIGINT       NULL     COMMENT '关联约束ID',
    `resolution_status`   TINYINT      NOT NULL DEFAULT 0 COMMENT '0未处理 1已解决 2已忽略',
    `resolution_note`     VARCHAR(500) NULL     COMMENT '处理说明',
    `resolved_by`         BIGINT       NULL     COMMENT '处理人',
    `resolved_at`         DATETIME     NULL     COMMENT '处理时间',
    `created_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_semester` (`semester_id`),
    INDEX `idx_batch` (`detection_batch`),
    INDEX `idx_status` (`resolution_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='排课冲突记录表';

-- ============================================================
-- 7. ALTER schedule_entries - 新增教学班ID和连排分组列
-- ============================================================

-- 7a. 添加 teaching_class_id 列
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'schedule_entries' AND COLUMN_NAME = 'teaching_class_id') = 0,
    'ALTER TABLE schedule_entries ADD COLUMN teaching_class_id BIGINT COMMENT ''教学班ID'' AFTER task_id',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 7b. 添加 consecutive_group 列
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'schedule_entries' AND COLUMN_NAME = 'consecutive_group') = 0,
    'ALTER TABLE schedule_entries ADD COLUMN consecutive_group VARCHAR(50) COMMENT ''连排分组标识'' AFTER week_type',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 7c. 添加 teaching_class_id 索引
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'schedule_entries' AND INDEX_NAME = 'idx_teaching_class') = 0,
    'ALTER TABLE schedule_entries ADD INDEX idx_teaching_class (teaching_class_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 7d. 添加 consecutive_group 索引
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'schedule_entries' AND INDEX_NAME = 'idx_consecutive_group') = 0,
    'ALTER TABLE schedule_entries ADD INDEX idx_consecutive_group (consecutive_group)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
