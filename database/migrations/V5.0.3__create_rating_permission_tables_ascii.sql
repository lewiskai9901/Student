-- ============================================================================
-- Student Management System - Architecture Refactoring Migration V5.0.3
-- Create Rating and Permission Tables
-- Date: 2026-01-02
-- ============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------------
-- 1. Rating Templates Table
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `rating_templates` (
    `id` BIGINT NOT NULL COMMENT 'Primary Key',
    `template_code` VARCHAR(50) NOT NULL COMMENT 'Template Code',
    `template_name` VARCHAR(100) NOT NULL COMMENT 'Template Name',
    `description` TEXT DEFAULT NULL COMMENT 'Description',
    `inspection_template_id` BIGINT DEFAULT NULL COMMENT 'Inspection Template ID',
    `is_default` TINYINT DEFAULT 0 COMMENT 'Is Default',
    `status` TINYINT DEFAULT 1 COMMENT 'Status',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_code` (`template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Rating Templates Table';

-- ---------------------------------------------------------------------------
-- 2. Rating Rules Table
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `rating_rules` (
    `id` BIGINT NOT NULL COMMENT 'Primary Key',
    `template_id` BIGINT NOT NULL COMMENT 'Rating Template ID',
    `rule_code` VARCHAR(50) NOT NULL COMMENT 'Rule Code',
    `rule_name` VARCHAR(100) NOT NULL COMMENT 'Rule Name',
    `rating_basis` ENUM('TOTAL', 'SINGLE_CATEGORY', 'MULTI_CATEGORY', 'CUSTOM') NOT NULL COMMENT 'Rating Basis',
    `category_ids` JSON DEFAULT NULL COMMENT 'Category IDs',
    `score_type` ENUM('DEDUCTION', 'WEIGHTED', 'FINAL') NOT NULL COMMENT 'Score Type',
    `rating_method` ENUM('ABSOLUTE', 'PERCENTAGE', 'RANKING') NOT NULL COMMENT 'Rating Method',
    `sort_order` INT DEFAULT 0 COMMENT 'Sort Order',
    `is_enabled` TINYINT DEFAULT 1 COMMENT 'Is Enabled',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_rule` (`template_id`, `rule_code`),
    KEY `idx_template` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Rating Rules Table';

-- ---------------------------------------------------------------------------
-- 3. Rating Levels Table
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `rating_levels` (
    `id` BIGINT NOT NULL COMMENT 'Primary Key',
    `rule_id` BIGINT NOT NULL COMMENT 'Rule ID',
    `level_code` VARCHAR(20) NOT NULL COMMENT 'Level Code',
    `level_name` VARCHAR(50) NOT NULL COMMENT 'Level Name',
    `min_score` DECIMAL(10,2) DEFAULT NULL COMMENT 'Min Score',
    `max_score` DECIMAL(10,2) DEFAULT NULL COMMENT 'Max Score',
    `min_percent` DECIMAL(5,2) DEFAULT NULL COMMENT 'Min Percent',
    `max_percent` DECIMAL(5,2) DEFAULT NULL COMMENT 'Max Percent',
    `top_n` INT DEFAULT NULL COMMENT 'Top N',
    `top_percent` DECIMAL(5,2) DEFAULT NULL COMMENT 'Top Percent',
    `color` VARCHAR(20) DEFAULT NULL COMMENT 'Color',
    `icon` VARCHAR(100) DEFAULT NULL COMMENT 'Icon',
    `reward_points` INT DEFAULT 0 COMMENT 'Reward Points',
    `penalty_points` INT DEFAULT 0 COMMENT 'Penalty Points',
    `level_order` INT NOT NULL COMMENT 'Level Order',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_rule` (`rule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Rating Levels Table';

-- ---------------------------------------------------------------------------
-- 4. Rating Results Table
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `rating_results` (
    `id` BIGINT NOT NULL COMMENT 'Primary Key',
    `record_id` BIGINT NOT NULL COMMENT 'Inspection Record ID',
    `class_score_id` BIGINT NOT NULL COMMENT 'Class Score ID',
    `class_id` BIGINT NOT NULL COMMENT 'Class ID',
    `rule_id` BIGINT NOT NULL COMMENT 'Rating Rule ID',
    `level_id` BIGINT NOT NULL COMMENT 'Rating Level ID',
    `rule_name` VARCHAR(100) NOT NULL COMMENT 'Rule Name Snapshot',
    `level_code` VARCHAR(20) NOT NULL COMMENT 'Level Code Snapshot',
    `level_name` VARCHAR(50) NOT NULL COMMENT 'Level Name Snapshot',
    `calculated_score` DECIMAL(10,2) DEFAULT NULL COMMENT 'Calculated Score',
    `calculated_percent` DECIMAL(5,2) DEFAULT NULL COMMENT 'Calculated Percent',
    `calculated_rank` INT DEFAULT NULL COMMENT 'Calculated Rank',
    `reward_points` INT DEFAULT 0 COMMENT 'Reward Points',
    `result_version` INT DEFAULT 1 COMMENT 'Result Version',
    `is_latest` TINYINT DEFAULT 1 COMMENT 'Is Latest',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_record_class_rule_version` (`record_id`, `class_id`, `rule_id`, `result_version`),
    KEY `idx_record` (`record_id`),
    KEY `idx_class` (`class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Rating Results Table';

-- ---------------------------------------------------------------------------
-- 5. Data Permissions Table
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `data_permissions` (
    `id` BIGINT NOT NULL COMMENT 'Primary Key',
    `principal_type` ENUM('USER', 'ROLE') NOT NULL COMMENT 'Principal Type',
    `principal_id` BIGINT NOT NULL COMMENT 'Principal ID',
    `resource_module` VARCHAR(50) NOT NULL COMMENT 'Resource Module',
    `data_scope` ENUM('ALL', 'DEPARTMENT', 'DEPARTMENT_AND_CHILD', 'SELF_DEPARTMENT', 'GRADE', 'CLASS', 'SELF', 'CUSTOM') NOT NULL COMMENT 'Data Scope',
    `custom_org_unit_ids` JSON DEFAULT NULL COMMENT 'Custom Org Unit IDs',
    `custom_class_ids` JSON DEFAULT NULL COMMENT 'Custom Class IDs',
    `custom_user_ids` JSON DEFAULT NULL COMMENT 'Custom User IDs',
    `allowed_actions` JSON DEFAULT NULL COMMENT 'Allowed Actions',
    `conditions` JSON DEFAULT NULL COMMENT 'ABAC Conditions',
    `priority` INT DEFAULT 0 COMMENT 'Priority',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by` BIGINT DEFAULT NULL,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_principal` (`principal_type`, `principal_id`),
    KEY `idx_module` (`resource_module`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Data Permissions Table';

-- ---------------------------------------------------------------------------
-- 6. Audit Logs Table
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `audit_logs` (
    `id` BIGINT NOT NULL COMMENT 'Primary Key',
    `operation_type` VARCHAR(50) NOT NULL COMMENT 'Operation Type',
    `operation_module` VARCHAR(50) NOT NULL COMMENT 'Operation Module',
    `operation_desc` VARCHAR(500) DEFAULT NULL COMMENT 'Operation Description',
    `target_type` VARCHAR(50) DEFAULT NULL COMMENT 'Target Type',
    `target_id` BIGINT DEFAULT NULL COMMENT 'Target ID',
    `target_name` VARCHAR(200) DEFAULT NULL COMMENT 'Target Name',
    `operator_id` BIGINT NOT NULL COMMENT 'Operator ID',
    `operator_name` VARCHAR(50) NOT NULL COMMENT 'Operator Name',
    `operator_ip` VARCHAR(50) DEFAULT NULL COMMENT 'Operator IP',
    `operator_ua` VARCHAR(500) DEFAULT NULL COMMENT 'User Agent',
    `old_value` JSON DEFAULT NULL COMMENT 'Old Value',
    `new_value` JSON DEFAULT NULL COMMENT 'New Value',
    `diff_value` JSON DEFAULT NULL COMMENT 'Diff Value',
    `request_url` VARCHAR(500) DEFAULT NULL COMMENT 'Request URL',
    `request_method` VARCHAR(10) DEFAULT NULL COMMENT 'Request Method',
    `request_params` JSON DEFAULT NULL COMMENT 'Request Params',
    `response_code` INT DEFAULT NULL COMMENT 'Response Code',
    `execution_time` BIGINT DEFAULT NULL COMMENT 'Execution Time (ms)',
    `success` TINYINT DEFAULT 1 COMMENT 'Is Success',
    `error_message` TEXT DEFAULT NULL COMMENT 'Error Message',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created At',
    PRIMARY KEY (`id`),
    KEY `idx_operator` (`operator_id`),
    KEY `idx_module` (`operation_module`),
    KEY `idx_target` (`target_type`, `target_id`),
    KEY `idx_time` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Audit Logs Table';

-- ---------------------------------------------------------------------------
-- 7. Domain Events Table
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `domain_events` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `event_id` VARCHAR(36) NOT NULL COMMENT 'Event UUID',
    `event_type` VARCHAR(100) NOT NULL COMMENT 'Event Type',
    `aggregate_type` VARCHAR(100) NOT NULL COMMENT 'Aggregate Type',
    `aggregate_id` VARCHAR(100) NOT NULL COMMENT 'Aggregate ID',
    `aggregate_version` INT NOT NULL COMMENT 'Aggregate Version',
    `payload` JSON NOT NULL COMMENT 'Event Data',
    `metadata` JSON DEFAULT NULL COMMENT 'Metadata',
    `occurred_at` DATETIME(3) NOT NULL COMMENT 'Occurred At',
    `created_at` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Created At',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_event_id` (`event_id`),
    KEY `idx_aggregate` (`aggregate_type`, `aggregate_id`),
    KEY `idx_occurred` (`occurred_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Domain Events Table';

-- ---------------------------------------------------------------------------
-- 8. Event Publications Table
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `event_publications` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `event_id` VARCHAR(36) NOT NULL COMMENT 'Event ID',
    `event_type` VARCHAR(100) NOT NULL COMMENT 'Event Type',
    `status` ENUM('PENDING', 'PUBLISHED', 'FAILED') DEFAULT 'PENDING' COMMENT 'Status',
    `retry_count` INT DEFAULT 0 COMMENT 'Retry Count',
    `last_error` TEXT DEFAULT NULL COMMENT 'Last Error',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created At',
    `published_at` DATETIME DEFAULT NULL COMMENT 'Published At',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_event_id` (`event_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Event Publications Table';

SET FOREIGN_KEY_CHECKS = 1;
