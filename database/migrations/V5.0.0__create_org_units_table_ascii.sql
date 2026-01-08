-- ============================================================================
-- Student Management System - Architecture Refactoring Migration V5.0.0
-- Create unified org_units table
-- Date: 2026-01-02
-- ============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------------
-- 1. Create org_units table
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `org_units` (
    `id` BIGINT NOT NULL COMMENT 'Primary Key',
    `unit_code` VARCHAR(50) NOT NULL COMMENT 'Organization Code',
    `unit_name` VARCHAR(100) NOT NULL COMMENT 'Organization Name',
    `unit_type` ENUM('SCHOOL', 'COLLEGE', 'DEPARTMENT', 'TEACHING_GROUP') NOT NULL COMMENT 'Organization Type',
    `parent_id` BIGINT DEFAULT NULL COMMENT 'Parent ID',
    `tree_path` VARCHAR(500) DEFAULT NULL COMMENT 'Path e.g. /1/5/12/',
    `tree_level` INT DEFAULT 1 COMMENT 'Tree Level',
    `leader_id` BIGINT DEFAULT NULL COMMENT 'Leader ID',
    `deputy_leader_ids` JSON DEFAULT NULL COMMENT 'Deputy Leader IDs',
    `sort_order` INT DEFAULT 0 COMMENT 'Sort Order',
    `status` TINYINT DEFAULT 1 COMMENT 'Status: 0=disabled 1=enabled',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created At',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated At',
    `created_by` BIGINT DEFAULT NULL COMMENT 'Created By',
    `updated_by` BIGINT DEFAULT NULL COMMENT 'Updated By',
    `deleted` TINYINT DEFAULT 0 COMMENT 'Delete Flag',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_unit_code` (`unit_code`),
    KEY `idx_parent` (`parent_id`),
    KEY `idx_tree_path` (`tree_path`(255)),
    KEY `idx_type` (`unit_type`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Organization Units Table';

-- ---------------------------------------------------------------------------
-- 2. Create academic_years table
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `academic_years` (
    `id` BIGINT NOT NULL COMMENT 'Primary Key',
    `year_code` VARCHAR(20) NOT NULL COMMENT 'Year Code e.g. 2024-2025',
    `year_name` VARCHAR(50) NOT NULL COMMENT 'Year Name',
    `start_date` DATE NOT NULL COMMENT 'Start Date',
    `end_date` DATE NOT NULL COMMENT 'End Date',
    `semesters` JSON DEFAULT NULL COMMENT 'Semester Config',
    `is_current` TINYINT DEFAULT 0 COMMENT 'Is Current Year',
    `status` TINYINT DEFAULT 1 COMMENT 'Status',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_year_code` (`year_code`),
    KEY `idx_current` (`is_current`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Academic Years Table';

-- ---------------------------------------------------------------------------
-- 3. Create teacher_assignments table
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `teacher_assignments` (
    `id` BIGINT NOT NULL COMMENT 'Primary Key',
    `class_id` BIGINT NOT NULL COMMENT 'Class ID',
    `teacher_id` BIGINT NOT NULL COMMENT 'Teacher ID',
    `role_type` ENUM('HEAD_TEACHER', 'DEPUTY_HEAD', 'SUBJECT_TEACHER', 'COUNSELOR') NOT NULL COMMENT 'Role Type',
    `subject_id` BIGINT DEFAULT NULL COMMENT 'Subject ID',
    `is_primary` TINYINT DEFAULT 0 COMMENT 'Is Primary',
    `start_date` DATE NOT NULL COMMENT 'Start Date',
    `end_date` DATE DEFAULT NULL COMMENT 'End Date',
    `status` ENUM('ACTIVE', 'TRANSFERRED', 'RESIGNED', 'EXPIRED') DEFAULT 'ACTIVE' COMMENT 'Status',
    `transfer_reason` VARCHAR(500) DEFAULT NULL COMMENT 'Transfer Reason',
    `handover_teacher_id` BIGINT DEFAULT NULL COMMENT 'Handover Teacher ID',
    `workload_hours` DECIMAL(5,2) DEFAULT NULL COMMENT 'Workload Hours',
    `remark` TEXT DEFAULT NULL COMMENT 'Remark',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by` BIGINT DEFAULT NULL,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_class` (`class_id`),
    KEY `idx_teacher` (`teacher_id`),
    KEY `idx_status` (`status`),
    KEY `idx_role_type` (`role_type`),
    KEY `idx_active_assignment` (`class_id`, `role_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Teacher Assignments Table';

-- ---------------------------------------------------------------------------
-- 4. Create grade_directors table
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `grade_directors` (
    `id` BIGINT NOT NULL COMMENT 'Primary Key',
    `org_unit_id` BIGINT NOT NULL COMMENT 'Org Unit ID',
    `enrollment_year` INT NOT NULL COMMENT 'Enrollment Year',
    `director_id` BIGINT NOT NULL COMMENT 'Director ID',
    `deputy_director_ids` JSON DEFAULT NULL COMMENT 'Deputy Director IDs',
    `counselor_ids` JSON DEFAULT NULL COMMENT 'Counselor IDs',
    `max_class_count` INT DEFAULT NULL COMMENT 'Max Class Count',
    `enrollment_quota` INT DEFAULT NULL COMMENT 'Enrollment Quota',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_org_year` (`org_unit_id`, `enrollment_year`),
    KEY `idx_director` (`director_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Grade Directors Table';

SET FOREIGN_KEY_CHECKS = 1;
