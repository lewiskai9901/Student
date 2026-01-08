-- ============================================================================
-- Student Management System - Architecture Refactoring Migration V5.0.2
-- Create Inspection Module Tables
-- Date: 2026-01-02
-- ============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------------
-- 1. Inspection Templates Table
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `inspection_templates` (
    `id` BIGINT NOT NULL COMMENT 'Primary Key',
    `template_code` VARCHAR(50) NOT NULL COMMENT 'Template Code',
    `template_name` VARCHAR(100) NOT NULL COMMENT 'Template Name',
    `description` TEXT DEFAULT NULL COMMENT 'Description',
    `current_version` INT DEFAULT 1 COMMENT 'Current Version',
    `applicable_scope` ENUM('ALL', 'DEPARTMENT', 'GRADE', 'CUSTOM') DEFAULT 'ALL' COMMENT 'Applicable Scope',
    `applicable_config` JSON DEFAULT NULL COMMENT 'Scope Config',
    `default_weight_scheme_id` BIGINT DEFAULT NULL COMMENT 'Default Weight Scheme ID',
    `default_appeal_config_id` BIGINT DEFAULT NULL COMMENT 'Default Appeal Config ID',
    `use_count` INT DEFAULT 0 COMMENT 'Use Count',
    `last_used_at` DATETIME DEFAULT NULL COMMENT 'Last Used At',
    `status` ENUM('DRAFT', 'PUBLISHED', 'DEPRECATED') DEFAULT 'DRAFT' COMMENT 'Status',
    `is_default` TINYINT DEFAULT 0 COMMENT 'Is Default',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by` BIGINT DEFAULT NULL,
    `updated_by` BIGINT DEFAULT NULL,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_code` (`template_code`),
    KEY `idx_status` (`status`),
    KEY `idx_default` (`is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Inspection Templates Table';

-- ---------------------------------------------------------------------------
-- 2. Template Versions Table
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `template_versions` (
    `id` BIGINT NOT NULL COMMENT 'Primary Key',
    `template_id` BIGINT NOT NULL COMMENT 'Template ID',
    `version_number` INT NOT NULL COMMENT 'Version Number',
    `version_name` VARCHAR(50) DEFAULT NULL COMMENT 'Version Name',
    `snapshot` JSON NOT NULL COMMENT 'Complete Snapshot',
    `change_summary` TEXT DEFAULT NULL COMMENT 'Change Summary',
    `change_detail` JSON DEFAULT NULL COMMENT 'Change Detail',
    `published_at` DATETIME DEFAULT NULL COMMENT 'Published At',
    `published_by` BIGINT DEFAULT NULL COMMENT 'Published By',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `created_by` BIGINT DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_version` (`template_id`, `version_number`),
    KEY `idx_template` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Template Versions Table';

-- ---------------------------------------------------------------------------
-- 3. Inspection Rounds Table
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `inspection_rounds` (
    `id` BIGINT NOT NULL COMMENT 'Primary Key',
    `template_id` BIGINT NOT NULL COMMENT 'Template ID',
    `round_code` VARCHAR(30) NOT NULL COMMENT 'Round Code',
    `round_name` VARCHAR(50) NOT NULL COMMENT 'Round Name',
    `scheduled_start_time` TIME DEFAULT NULL COMMENT 'Scheduled Start Time',
    `scheduled_end_time` TIME DEFAULT NULL COMMENT 'Scheduled End Time',
    `weight` DECIMAL(5,4) DEFAULT 1.0000 COMMENT 'Weight',
    `is_required` TINYINT DEFAULT 1 COMMENT 'Is Required',
    `applicable_days` JSON DEFAULT NULL COMMENT 'Applicable Days',
    `sort_order` INT DEFAULT 0 COMMENT 'Sort Order',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_round` (`template_id`, `round_code`),
    KEY `idx_template` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Inspection Rounds Table';

-- ---------------------------------------------------------------------------
-- 4. Inspection Categories Table
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `inspection_categories` (
    `id` BIGINT NOT NULL COMMENT 'Primary Key',
    `template_id` BIGINT NOT NULL COMMENT 'Template ID',
    `category_code` VARCHAR(50) NOT NULL COMMENT 'Category Code',
    `category_name` VARCHAR(100) NOT NULL COMMENT 'Category Name',
    `category_type` ENUM('HYGIENE', 'DISCIPLINE', 'ATTENDANCE', 'SAFETY', 'OTHER') NOT NULL COMMENT 'Category Type',
    `max_score` DECIMAL(6,2) DEFAULT 100.00 COMMENT 'Max Score',
    `weight` DECIMAL(5,4) DEFAULT 1.0000 COMMENT 'Weight',
    `link_type` ENUM('NONE', 'DORMITORY', 'CLASSROOM', 'STUDENT') DEFAULT 'NONE' COMMENT 'Link Type',
    `applicable_round_ids` JSON DEFAULT NULL COMMENT 'Applicable Round IDs',
    `icon` VARCHAR(100) DEFAULT NULL COMMENT 'Icon',
    `sort_order` INT DEFAULT 0 COMMENT 'Sort Order',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_category` (`template_id`, `category_code`),
    KEY `idx_template` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Inspection Categories Table';

-- ---------------------------------------------------------------------------
-- 5. Deduction Items Table V2
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `deduction_items_v2` (
    `id` BIGINT NOT NULL COMMENT 'Primary Key',
    `category_id` BIGINT NOT NULL COMMENT 'Category ID',
    `item_code` VARCHAR(50) NOT NULL COMMENT 'Item Code',
    `item_name` VARCHAR(200) NOT NULL COMMENT 'Item Name',
    `description` TEXT DEFAULT NULL COMMENT 'Description',
    `deduction_mode` ENUM('FIXED', 'PER_PERSON', 'RANGE', 'FORMULA') NOT NULL COMMENT 'Deduction Mode',
    `fixed_score` DECIMAL(6,2) DEFAULT NULL COMMENT 'Fixed Score',
    `base_score` DECIMAL(6,2) DEFAULT NULL COMMENT 'Base Score',
    `per_person_score` DECIMAL(6,2) DEFAULT NULL COMMENT 'Per Person Score',
    `max_persons` INT DEFAULT NULL COMMENT 'Max Persons',
    `range_config` JSON DEFAULT NULL COMMENT 'Range Config',
    `formula_expression` VARCHAR(500) DEFAULT NULL COMMENT 'Formula Expression',
    `min_score` DECIMAL(6,2) DEFAULT 0 COMMENT 'Min Score',
    `max_score` DECIMAL(6,2) DEFAULT NULL COMMENT 'Max Score',
    `requires_photo` TINYINT DEFAULT 0 COMMENT 'Requires Photo',
    `requires_student_select` TINYINT DEFAULT 0 COMMENT 'Requires Student Select',
    `allows_remark` TINYINT DEFAULT 1 COMMENT 'Allows Remark',
    `check_points` JSON DEFAULT NULL COMMENT 'Check Points',
    `sort_order` INT DEFAULT 0 COMMENT 'Sort Order',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_category_item` (`category_id`, `item_code`),
    KEY `idx_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Deduction Items Table V2';

-- ---------------------------------------------------------------------------
-- 6. Weight Schemes Table
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `weight_schemes` (
    `id` BIGINT NOT NULL COMMENT 'Primary Key',
    `scheme_code` VARCHAR(50) NOT NULL COMMENT 'Scheme Code',
    `scheme_name` VARCHAR(100) NOT NULL COMMENT 'Scheme Name',
    `description` TEXT DEFAULT NULL COMMENT 'Description',
    `weight_mode` ENUM('STANDARD', 'PER_CAPITA', 'SEGMENT', 'CUSTOM') NOT NULL COMMENT 'Weight Mode',
    `standard_size_mode` ENUM('FIXED', 'TARGET_AVERAGE', 'RANGE_AVERAGE') DEFAULT NULL COMMENT 'Standard Size Mode',
    `standard_size` INT DEFAULT NULL COMMENT 'Standard Size',
    `weight_formula` VARCHAR(200) DEFAULT NULL COMMENT 'Weight Formula',
    `min_weight` DECIMAL(5,4) DEFAULT 0.5000 COMMENT 'Min Weight',
    `max_weight` DECIMAL(5,4) DEFAULT 2.0000 COMMENT 'Max Weight',
    `segment_rules` JSON DEFAULT NULL COMMENT 'Segment Rules',
    `visibility` ENUM('GLOBAL', 'DEPARTMENT', 'PRIVATE') DEFAULT 'GLOBAL' COMMENT 'Visibility',
    `owner_dept_id` BIGINT DEFAULT NULL COMMENT 'Owner Dept ID',
    `owner_user_id` BIGINT DEFAULT NULL COMMENT 'Owner User ID',
    `use_count` INT DEFAULT 0 COMMENT 'Use Count',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by` BIGINT DEFAULT NULL,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_scheme_code` (`scheme_code`),
    KEY `idx_visibility` (`visibility`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Weight Schemes Table';

-- ---------------------------------------------------------------------------
-- 7. Inspection Records Table
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `inspection_records` (
    `id` BIGINT NOT NULL COMMENT 'Primary Key',
    `record_code` VARCHAR(50) NOT NULL COMMENT 'Record Code',
    `record_name` VARCHAR(100) NOT NULL COMMENT 'Record Name',
    `check_date` DATE NOT NULL COMMENT 'Check Date',
    `check_type` ENUM('DAILY', 'WEEKLY', 'SPECIAL', 'RANDOM') NOT NULL COMMENT 'Check Type',
    `template_id` BIGINT NOT NULL COMMENT 'Template ID',
    `template_version` INT NOT NULL COMMENT 'Template Version',
    `template_snapshot` JSON NOT NULL COMMENT 'Template Snapshot',
    `weight_scheme_snapshot` JSON DEFAULT NULL COMMENT 'Weight Scheme Snapshot',
    `scope_type` ENUM('ALL', 'DEPARTMENT', 'GRADE', 'CUSTOM') NOT NULL COMMENT 'Scope Type',
    `scope_config` JSON DEFAULT NULL COMMENT 'Scope Config',
    `total_classes` INT DEFAULT NULL COMMENT 'Total Classes',
    `total_deduction` DECIMAL(10,2) DEFAULT NULL COMMENT 'Total Deduction',
    `avg_deduction` DECIMAL(10,2) DEFAULT NULL COMMENT 'Average Deduction',
    `max_deduction` DECIMAL(10,2) DEFAULT NULL COMMENT 'Max Deduction',
    `min_deduction` DECIMAL(10,2) DEFAULT NULL COMMENT 'Min Deduction',
    `status` ENUM('DRAFT', 'CHECKING', 'SUBMITTED', 'REVIEWING', 'PUBLISHED', 'APPEALING', 'FINALIZED', 'ARCHIVED') NOT NULL DEFAULT 'DRAFT' COMMENT 'Status',
    `check_started_at` DATETIME DEFAULT NULL COMMENT 'Check Started At',
    `check_ended_at` DATETIME DEFAULT NULL COMMENT 'Check Ended At',
    `submitted_at` DATETIME DEFAULT NULL COMMENT 'Submitted At',
    `reviewed_at` DATETIME DEFAULT NULL COMMENT 'Reviewed At',
    `published_at` DATETIME DEFAULT NULL COMMENT 'Published At',
    `finalized_at` DATETIME DEFAULT NULL COMMENT 'Finalized At',
    `archived_at` DATETIME DEFAULT NULL COMMENT 'Archived At',
    `checker_id` BIGINT DEFAULT NULL COMMENT 'Checker ID',
    `submitter_id` BIGINT DEFAULT NULL COMMENT 'Submitter ID',
    `reviewer_id` BIGINT DEFAULT NULL COMMENT 'Reviewer ID',
    `publisher_id` BIGINT DEFAULT NULL COMMENT 'Publisher ID',
    `version` INT DEFAULT 0 COMMENT 'Optimistic Lock Version',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by` BIGINT DEFAULT NULL,
    `updated_by` BIGINT DEFAULT NULL,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_record_code` (`record_code`),
    KEY `idx_date` (`check_date`),
    KEY `idx_status` (`status`),
    KEY `idx_template` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Inspection Records Table';

-- ---------------------------------------------------------------------------
-- 8. Class Scores Table
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `class_scores` (
    `id` BIGINT NOT NULL COMMENT 'Primary Key',
    `record_id` BIGINT NOT NULL COMMENT 'Inspection Record ID',
    `class_id` BIGINT NOT NULL COMMENT 'Class ID',
    `class_code` VARCHAR(50) NOT NULL COMMENT 'Class Code Snapshot',
    `class_name` VARCHAR(100) NOT NULL COMMENT 'Class Name Snapshot',
    `class_size` INT NOT NULL COMMENT 'Class Size Snapshot',
    `org_unit_id` BIGINT NOT NULL COMMENT 'Org Unit ID',
    `org_unit_name` VARCHAR(100) DEFAULT NULL COMMENT 'Org Unit Name Snapshot',
    `enrollment_year` INT DEFAULT NULL COMMENT 'Enrollment Year',
    `grade_level` INT DEFAULT NULL COMMENT 'Grade Level',
    `original_deduction` DECIMAL(10,2) DEFAULT 0 COMMENT 'Original Deduction',
    `weight_coefficient` DECIMAL(5,4) DEFAULT 1.0000 COMMENT 'Weight Coefficient',
    `weighted_deduction` DECIMAL(10,2) DEFAULT NULL COMMENT 'Weighted Deduction',
    `final_score` DECIMAL(10,2) DEFAULT NULL COMMENT 'Final Score',
    `final_deduction` DECIMAL(10,2) DEFAULT NULL COMMENT 'Final Deduction',
    `overall_rank` INT DEFAULT NULL COMMENT 'Overall Rank',
    `dept_rank` INT DEFAULT NULL COMMENT 'Department Rank',
    `grade_rank` INT DEFAULT NULL COMMENT 'Grade Rank',
    `vs_prev_score` DECIMAL(10,2) DEFAULT NULL COMMENT 'Vs Previous Score',
    `vs_avg_score` DECIMAL(10,2) DEFAULT NULL COMMENT 'Vs Average Score',
    `appeal_count` INT DEFAULT 0 COMMENT 'Appeal Count',
    `appeal_approved` INT DEFAULT 0 COMMENT 'Appeal Approved Count',
    `appeal_score_change` DECIMAL(10,2) DEFAULT 0 COMMENT 'Appeal Score Change',
    `score_version` INT DEFAULT 1 COMMENT 'Score Version',
    `is_latest` TINYINT DEFAULT 1 COMMENT 'Is Latest Version',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_record_class_version` (`record_id`, `class_id`, `score_version`),
    KEY `idx_record` (`record_id`),
    KEY `idx_class` (`class_id`),
    KEY `idx_rank` (`record_id`, `overall_rank`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Class Scores Table';

-- ---------------------------------------------------------------------------
-- 9. Deduction Details Table
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `deduction_details` (
    `id` BIGINT NOT NULL COMMENT 'Primary Key',
    `record_id` BIGINT NOT NULL COMMENT 'Inspection Record ID',
    `class_score_id` BIGINT NOT NULL COMMENT 'Class Score ID',
    `category_id` BIGINT NOT NULL COMMENT 'Category ID',
    `category_name` VARCHAR(100) NOT NULL COMMENT 'Category Name Snapshot',
    `item_id` BIGINT NOT NULL COMMENT 'Deduction Item ID',
    `item_name` VARCHAR(200) NOT NULL COMMENT 'Item Name Snapshot',
    `deduction_mode` VARCHAR(20) NOT NULL COMMENT 'Deduction Mode',
    `round_id` BIGINT DEFAULT NULL COMMENT 'Round ID',
    `round_name` VARCHAR(50) DEFAULT NULL COMMENT 'Round Name',
    `link_type` ENUM('NONE', 'DORMITORY', 'CLASSROOM', 'STUDENT') DEFAULT NULL COMMENT 'Link Type',
    `link_id` BIGINT DEFAULT NULL COMMENT 'Link ID',
    `link_info` VARCHAR(200) DEFAULT NULL COMMENT 'Link Info',
    `deduct_score` DECIMAL(6,2) NOT NULL COMMENT 'Deduction Score',
    `person_count` INT DEFAULT NULL COMMENT 'Person Count',
    `student_ids` JSON DEFAULT NULL COMMENT 'Student IDs',
    `photo_urls` JSON DEFAULT NULL COMMENT 'Photo URLs',
    `video_urls` JSON DEFAULT NULL COMMENT 'Video URLs',
    `remark` TEXT DEFAULT NULL COMMENT 'Remark',
    `inspector_id` BIGINT DEFAULT NULL COMMENT 'Inspector ID',
    `inspector_name` VARCHAR(50) DEFAULT NULL COMMENT 'Inspector Name',
    `inspected_at` DATETIME DEFAULT NULL COMMENT 'Inspected At',
    `appeal_status` ENUM('NONE', 'PENDING', 'APPROVED', 'REJECTED') DEFAULT 'NONE' COMMENT 'Appeal Status',
    `appeal_id` BIGINT DEFAULT NULL COMMENT 'Appeal ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_record` (`record_id`),
    KEY `idx_class_score` (`class_score_id`),
    KEY `idx_category` (`category_id`),
    KEY `idx_appeal` (`appeal_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Deduction Details Table';

-- ---------------------------------------------------------------------------
-- 10. Appeals Table V2
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `appeals_v2` (
    `id` BIGINT NOT NULL COMMENT 'Primary Key',
    `appeal_code` VARCHAR(50) NOT NULL COMMENT 'Appeal Code',
    `record_id` BIGINT NOT NULL COMMENT 'Inspection Record ID',
    `class_score_id` BIGINT NOT NULL COMMENT 'Class Score ID',
    `deduction_detail_id` BIGINT NOT NULL COMMENT 'Deduction Detail ID',
    `class_id` BIGINT NOT NULL COMMENT 'Class ID',
    `class_name` VARCHAR(100) NOT NULL COMMENT 'Class Name Snapshot',
    `original_score` DECIMAL(6,2) NOT NULL COMMENT 'Original Score',
    `category_name` VARCHAR(100) NOT NULL COMMENT 'Category Name Snapshot',
    `item_name` VARCHAR(200) NOT NULL COMMENT 'Item Name Snapshot',
    `original_evidence` JSON DEFAULT NULL COMMENT 'Original Evidence',
    `appeal_type` ENUM('SCORE_DISPUTE', 'FACT_DISPUTE', 'PROCEDURE_DISPUTE') NOT NULL COMMENT 'Appeal Type',
    `appeal_reason` TEXT NOT NULL COMMENT 'Appeal Reason',
    `expected_score` DECIMAL(6,2) DEFAULT NULL COMMENT 'Expected Score',
    `evidence_urls` JSON DEFAULT NULL COMMENT 'Evidence URLs',
    `evidence_description` TEXT DEFAULT NULL COMMENT 'Evidence Description',
    `appellant_id` BIGINT NOT NULL COMMENT 'Appellant ID',
    `appellant_name` VARCHAR(50) NOT NULL COMMENT 'Appellant Name',
    `appellant_role` VARCHAR(50) DEFAULT NULL COMMENT 'Appellant Role',
    `appeal_time` DATETIME NOT NULL COMMENT 'Appeal Time',
    `deadline` DATETIME NOT NULL COMMENT 'Deadline',
    `status` ENUM('PENDING', 'LEVEL1_REVIEWING', 'LEVEL2_REVIEWING', 'APPROVED', 'REJECTED', 'WITHDRAWN', 'EXPIRED', 'PUBLICIZING', 'EFFECTIVE') NOT NULL DEFAULT 'PENDING' COMMENT 'Status',
    `current_node` INT DEFAULT 1 COMMENT 'Current Node',
    `current_approver_ids` JSON DEFAULT NULL COMMENT 'Current Approver IDs',
    `final_decision` ENUM('APPROVED', 'REJECTED', 'PARTIAL') DEFAULT NULL COMMENT 'Final Decision',
    `adjusted_score` DECIMAL(6,2) DEFAULT NULL COMMENT 'Adjusted Score',
    `score_change` DECIMAL(6,2) DEFAULT NULL COMMENT 'Score Change',
    `final_comment` TEXT DEFAULT NULL COMMENT 'Final Comment',
    `publicity_start` DATETIME DEFAULT NULL COMMENT 'Publicity Start',
    `publicity_end` DATETIME DEFAULT NULL COMMENT 'Publicity End',
    `publicity_days` INT DEFAULT 3 COMMENT 'Publicity Days',
    `effective_at` DATETIME DEFAULT NULL COMMENT 'Effective At',
    `version` INT DEFAULT 0 COMMENT 'Optimistic Lock Version',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_appeal_code` (`appeal_code`),
    KEY `idx_record` (`record_id`),
    KEY `idx_class` (`class_id`),
    KEY `idx_status` (`status`),
    KEY `idx_deadline` (`deadline`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Appeals Table V2';

-- ---------------------------------------------------------------------------
-- 11. Appeal Approvals Table
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `appeal_approvals` (
    `id` BIGINT NOT NULL COMMENT 'Primary Key',
    `appeal_id` BIGINT NOT NULL COMMENT 'Appeal ID',
    `node_order` INT NOT NULL COMMENT 'Node Order',
    `node_name` VARCHAR(50) DEFAULT NULL COMMENT 'Node Name',
    `approver_id` BIGINT NOT NULL COMMENT 'Approver ID',
    `approver_name` VARCHAR(50) NOT NULL COMMENT 'Approver Name',
    `approver_role` VARCHAR(50) DEFAULT NULL COMMENT 'Approver Role',
    `decision` ENUM('PENDING', 'APPROVED', 'REJECTED', 'TRANSFERRED') NOT NULL DEFAULT 'PENDING' COMMENT 'Decision',
    `comment` TEXT DEFAULT NULL COMMENT 'Comment',
    `adjusted_score` DECIMAL(6,2) DEFAULT NULL COMMENT 'Adjusted Score',
    `transfer_to_id` BIGINT DEFAULT NULL COMMENT 'Transfer To ID',
    `transfer_reason` TEXT DEFAULT NULL COMMENT 'Transfer Reason',
    `assigned_at` DATETIME DEFAULT NULL COMMENT 'Assigned At',
    `approved_at` DATETIME DEFAULT NULL COMMENT 'Approved At',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_appeal` (`appeal_id`),
    KEY `idx_approver` (`approver_id`),
    KEY `idx_status` (`decision`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Appeal Approvals Table';

SET FOREIGN_KEY_CHECKS = 1;
