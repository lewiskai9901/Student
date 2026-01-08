-- ============================================================================
-- Student Management System V2 - Audit & Event Schema
-- ============================================================================
-- Version: 2.0
-- Description: Consolidated schema for audit logs, domain events, and snapshots
-- Created: 2026-01-06
-- ============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================================
-- PART 1: OPERATION & AUDIT LOGS
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1.1 Operation Logs - Basic request/response tracking
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `operation_logs`;
CREATE TABLE `operation_logs` (
    `id` BIGINT NOT NULL COMMENT 'Primary Key',
    `user_id` BIGINT NOT NULL COMMENT 'Operator User ID',
    `username` VARCHAR(50) NOT NULL COMMENT 'Username',
    `real_name` VARCHAR(50) DEFAULT NULL COMMENT 'Real Name',

    -- Operation info
    `operation_module` VARCHAR(50) NOT NULL COMMENT 'Module (student/class/dormitory/inspection/system)',
    `operation_type` VARCHAR(20) NOT NULL COMMENT 'Type (create/update/delete/query/export/login/logout)',
    `operation_name` VARCHAR(100) NOT NULL COMMENT 'Operation Name',

    -- Request info
    `request_method` VARCHAR(10) DEFAULT NULL COMMENT 'HTTP Method (GET/POST/PUT/DELETE)',
    `request_url` VARCHAR(500) DEFAULT NULL COMMENT 'Request URL',
    `request_params` TEXT DEFAULT NULL COMMENT 'Request Parameters (JSON)',

    -- Response info
    `response_status` INT DEFAULT NULL COMMENT 'HTTP Status Code',
    `response_time` INT DEFAULT NULL COMMENT 'Response Time (ms)',
    `error_message` TEXT DEFAULT NULL COMMENT 'Error Message',

    -- Client info
    `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'IP Address',
    `user_agent` VARCHAR(500) DEFAULT NULL COMMENT 'User Agent',
    `browser` VARCHAR(50) DEFAULT NULL COMMENT 'Browser',
    `os` VARCHAR(50) DEFAULT NULL COMMENT 'Operating System',

    -- Timestamps
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created At',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated At',
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'Soft Delete Flag',

    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_username` (`username`),
    KEY `idx_operation_module` (`operation_module`),
    KEY `idx_operation_type` (`operation_type`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Operation Logs';

-- ---------------------------------------------------------------------------
-- 1.2 Audit Logs - Detailed change tracking with before/after values
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `audit_logs`;
CREATE TABLE `audit_logs` (
    `id` BIGINT NOT NULL COMMENT 'Primary Key',

    -- Operation info
    `operation_type` VARCHAR(50) NOT NULL COMMENT 'Operation Type (CREATE/UPDATE/DELETE/STATUS_CHANGE)',
    `operation_module` VARCHAR(50) NOT NULL COMMENT 'Operation Module',
    `operation_desc` VARCHAR(500) DEFAULT NULL COMMENT 'Operation Description',

    -- Target entity
    `target_type` VARCHAR(50) DEFAULT NULL COMMENT 'Target Entity Type',
    `target_id` BIGINT DEFAULT NULL COMMENT 'Target Entity ID',
    `target_name` VARCHAR(200) DEFAULT NULL COMMENT 'Target Entity Name',

    -- Operator info
    `operator_id` BIGINT NOT NULL COMMENT 'Operator ID',
    `operator_name` VARCHAR(50) NOT NULL COMMENT 'Operator Name',
    `operator_ip` VARCHAR(50) DEFAULT NULL COMMENT 'Operator IP',
    `operator_ua` VARCHAR(500) DEFAULT NULL COMMENT 'User Agent',

    -- Change tracking
    `old_value` JSON DEFAULT NULL COMMENT 'Old Value (JSON)',
    `new_value` JSON DEFAULT NULL COMMENT 'New Value (JSON)',
    `diff_value` JSON DEFAULT NULL COMMENT 'Diff Value (JSON)',

    -- Request context
    `request_url` VARCHAR(500) DEFAULT NULL COMMENT 'Request URL',
    `request_method` VARCHAR(10) DEFAULT NULL COMMENT 'Request Method',
    `request_params` JSON DEFAULT NULL COMMENT 'Request Parameters',

    -- Result
    `response_code` INT DEFAULT NULL COMMENT 'Response Code',
    `execution_time` BIGINT DEFAULT NULL COMMENT 'Execution Time (ms)',
    `success` TINYINT DEFAULT 1 COMMENT 'Is Success (0/1)',
    `error_message` TEXT DEFAULT NULL COMMENT 'Error Message',

    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created At',

    PRIMARY KEY (`id`),
    KEY `idx_operator` (`operator_id`),
    KEY `idx_module` (`operation_module`),
    KEY `idx_target` (`target_type`, `target_id`),
    KEY `idx_time` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Detailed Audit Logs';

-- ---------------------------------------------------------------------------
-- 1.3 Check Audit Logs - Inspection-specific audit trail
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `check_audit_logs`;
CREATE TABLE `check_audit_logs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',

    -- Operation info
    `operation_type` VARCHAR(50) NOT NULL COMMENT 'Operation Type',
    `operation_desc` VARCHAR(200) DEFAULT NULL COMMENT 'Operation Description',

    -- Related entities
    `record_id` BIGINT DEFAULT NULL COMMENT 'Check Record ID',
    `template_id` BIGINT DEFAULT NULL COMMENT 'Template ID',
    `class_id` BIGINT DEFAULT NULL COMMENT 'Class ID',
    `deduction_id` BIGINT DEFAULT NULL COMMENT 'Deduction Detail ID',

    -- Operator info
    `operator_id` BIGINT NOT NULL COMMENT 'Operator ID',
    `operator_name` VARCHAR(50) DEFAULT NULL COMMENT 'Operator Name',
    `operator_ip` VARCHAR(50) DEFAULT NULL COMMENT 'IP Address',

    -- Change tracking
    `before_data` JSON DEFAULT NULL COMMENT 'Before Data (JSON)',
    `after_data` JSON DEFAULT NULL COMMENT 'After Data (JSON)',

    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created At',

    PRIMARY KEY (`id`),
    KEY `idx_record` (`record_id`),
    KEY `idx_template` (`template_id`),
    KEY `idx_class` (`class_id`),
    KEY `idx_operator` (`operator_id`),
    KEY `idx_time` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Inspection Audit Logs';

-- ---------------------------------------------------------------------------
-- 1.4 Appeal Audit Logs - Appeal process tracking
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `appeal_audit_logs`;
CREATE TABLE `appeal_audit_logs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `appeal_id` BIGINT NOT NULL COMMENT 'Appeal ID',

    -- Action info
    `action_type` TINYINT NOT NULL COMMENT 'Action Type (1=Submit,2=Review,3=Cancel,4=Modify,5=Publish,6=Effective)',
    `action_user_id` BIGINT NOT NULL COMMENT 'Action User ID',
    `action_user_name` VARCHAR(50) DEFAULT NULL COMMENT 'Action User Name',
    `action_time` DATETIME NOT NULL COMMENT 'Action Time',

    -- Status tracking
    `before_status` TINYINT DEFAULT NULL COMMENT 'Before Status',
    `after_status` TINYINT DEFAULT NULL COMMENT 'After Status',
    `before_score` DECIMAL(10,2) DEFAULT NULL COMMENT 'Before Score',
    `after_score` DECIMAL(10,2) DEFAULT NULL COMMENT 'After Score',

    -- Details
    `comment` TEXT DEFAULT NULL COMMENT 'Comment',
    `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'IP Address',
    `user_agent` VARCHAR(200) DEFAULT NULL COMMENT 'User Agent',

    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created At',

    PRIMARY KEY (`id`),
    KEY `idx_appeal` (`appeal_id`),
    KEY `idx_action_time` (`action_time`),
    KEY `idx_action_type` (`action_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Appeal Audit Logs';

-- ---------------------------------------------------------------------------
-- 1.5 Rating Change Log - Rating change tracking
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `rating_change_logs`;
CREATE TABLE `rating_change_logs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',

    -- Related entities
    `record_id` BIGINT NOT NULL COMMENT 'Check Record ID',
    `class_id` BIGINT NOT NULL COMMENT 'Class ID',
    `plan_id` BIGINT DEFAULT NULL COMMENT 'Check Plan ID',

    -- Change tracking
    `change_type` VARCHAR(20) NOT NULL COMMENT 'Change Type (INITIAL/APPEAL/CORRECTION/RECALCULATE)',
    `old_rating` VARCHAR(20) DEFAULT NULL COMMENT 'Old Rating',
    `new_rating` VARCHAR(20) NOT NULL COMMENT 'New Rating',
    `old_score` DECIMAL(10,2) DEFAULT NULL COMMENT 'Old Score',
    `new_score` DECIMAL(10,2) NOT NULL COMMENT 'New Score',

    -- Reason
    `reason` VARCHAR(500) DEFAULT NULL COMMENT 'Change Reason',
    `related_appeal_id` BIGINT DEFAULT NULL COMMENT 'Related Appeal ID',

    -- Operator
    `changed_by` BIGINT NOT NULL COMMENT 'Changed By User ID',
    `changed_by_name` VARCHAR(50) DEFAULT NULL COMMENT 'Changed By User Name',

    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created At',

    PRIMARY KEY (`id`),
    KEY `idx_record` (`record_id`),
    KEY `idx_class` (`class_id`),
    KEY `idx_plan` (`plan_id`),
    KEY `idx_time` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Rating Change Log';

-- ---------------------------------------------------------------------------
-- 1.6 Task Logs - Task operation tracking
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `task_logs`;
CREATE TABLE `task_logs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `task_id` BIGINT NOT NULL COMMENT 'Task ID',

    -- Operation info
    `action_type` VARCHAR(50) NOT NULL COMMENT 'Action Type (CREATE/ASSIGN/SUBMIT/APPROVE/REJECT/COMPLETE)',
    `action_desc` VARCHAR(500) DEFAULT NULL COMMENT 'Action Description',

    -- Status tracking
    `before_status` VARCHAR(20) DEFAULT NULL COMMENT 'Before Status',
    `after_status` VARCHAR(20) DEFAULT NULL COMMENT 'After Status',

    -- Operator
    `operator_id` BIGINT NOT NULL COMMENT 'Operator ID',
    `operator_name` VARCHAR(50) DEFAULT NULL COMMENT 'Operator Name',
    `operator_role` VARCHAR(50) DEFAULT NULL COMMENT 'Operator Role',

    -- Additional data
    `extra_data` JSON DEFAULT NULL COMMENT 'Extra Data (JSON)',
    `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'IP Address',

    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created At',

    PRIMARY KEY (`id`),
    KEY `idx_task` (`task_id`),
    KEY `idx_operator` (`operator_id`),
    KEY `idx_action_type` (`action_type`),
    KEY `idx_time` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Task Operation Logs';

-- ============================================================================
-- PART 2: DOMAIN EVENTS (Event Sourcing Support)
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 2.1 Domain Events - Core event store
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `domain_events`;
CREATE TABLE `domain_events` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `event_id` VARCHAR(36) NOT NULL COMMENT 'Event UUID',

    -- Event info
    `event_type` VARCHAR(100) NOT NULL COMMENT 'Event Type (e.g., ClassCreatedEvent)',
    `aggregate_type` VARCHAR(100) NOT NULL COMMENT 'Aggregate Type (e.g., SchoolClass)',
    `aggregate_id` VARCHAR(100) NOT NULL COMMENT 'Aggregate ID',
    `aggregate_version` INT NOT NULL COMMENT 'Aggregate Version',

    -- Event data
    `payload` JSON NOT NULL COMMENT 'Event Payload (JSON)',
    `metadata` JSON DEFAULT NULL COMMENT 'Event Metadata (JSON)',

    -- Timestamps
    `occurred_at` DATETIME(3) NOT NULL COMMENT 'When Event Occurred',
    `created_at` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'When Record Created',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_event_id` (`event_id`),
    KEY `idx_aggregate` (`aggregate_type`, `aggregate_id`),
    KEY `idx_occurred` (`occurred_at`),
    KEY `idx_type` (`event_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Domain Events Store';

-- ---------------------------------------------------------------------------
-- 2.2 Event Publications - Outbox pattern for reliable event publishing
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `event_publications`;
CREATE TABLE `event_publications` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `event_id` VARCHAR(36) NOT NULL COMMENT 'Event ID (FK to domain_events)',
    `event_type` VARCHAR(100) NOT NULL COMMENT 'Event Type',

    -- Publication status
    `status` ENUM('PENDING', 'PUBLISHED', 'FAILED') DEFAULT 'PENDING' COMMENT 'Publication Status',
    `retry_count` INT DEFAULT 0 COMMENT 'Retry Count',
    `last_error` TEXT DEFAULT NULL COMMENT 'Last Error Message',

    -- Target info
    `target_channel` VARCHAR(100) DEFAULT NULL COMMENT 'Target Channel/Queue',

    -- Timestamps
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created At',
    `published_at` DATETIME DEFAULT NULL COMMENT 'Published At',
    `next_retry_at` DATETIME DEFAULT NULL COMMENT 'Next Retry Time',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_event_id` (`event_id`),
    KEY `idx_status` (`status`),
    KEY `idx_next_retry` (`status`, `next_retry_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Event Publications Outbox';

-- ============================================================================
-- PART 3: BUSINESS SNAPSHOTS
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 3.1 Class Size Snapshots - Daily class population records
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `class_size_snapshots`;
CREATE TABLE `class_size_snapshots` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `class_id` BIGINT NOT NULL COMMENT 'Class ID',
    `snapshot_date` DATE NOT NULL COMMENT 'Snapshot Date',

    -- Population counts
    `student_count` INT NOT NULL COMMENT 'Total Student Count',
    `active_count` INT DEFAULT NULL COMMENT 'Active Students (excluding leave/suspension)',
    `male_count` INT DEFAULT NULL COMMENT 'Male Count',
    `female_count` INT DEFAULT NULL COMMENT 'Female Count',

    -- Metadata
    `snapshot_source` VARCHAR(20) DEFAULT 'AUTO' COMMENT 'Source (AUTO/MANUAL/PUBLISH)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created At',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_class_date` (`class_id`, `snapshot_date`),
    KEY `idx_snapshot_date` (`snapshot_date`),
    KEY `idx_class_id` (`class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Class Size Snapshots';

-- ---------------------------------------------------------------------------
-- 3.2 Student Relationship Snapshots - Student org assignments at point in time
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `student_relationship_snapshots`;
CREATE TABLE `student_relationship_snapshots` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `snapshot_date` DATE NOT NULL COMMENT 'Snapshot Date',
    `snapshot_type` VARCHAR(20) NOT NULL DEFAULT 'DAILY' COMMENT 'Type (DAILY/SEMESTER_START/SEMESTER_END)',

    -- Student info
    `student_id` BIGINT NOT NULL COMMENT 'Student ID',
    `student_no` VARCHAR(50) DEFAULT NULL COMMENT 'Student Number',
    `student_name` VARCHAR(50) DEFAULT NULL COMMENT 'Student Name',

    -- Organization assignments
    `class_id` BIGINT DEFAULT NULL COMMENT 'Class ID',
    `class_name` VARCHAR(100) DEFAULT NULL COMMENT 'Class Name',
    `grade_id` BIGINT DEFAULT NULL COMMENT 'Grade ID',
    `grade_name` VARCHAR(50) DEFAULT NULL COMMENT 'Grade Name',
    `department_id` BIGINT DEFAULT NULL COMMENT 'Department ID',
    `department_name` VARCHAR(100) DEFAULT NULL COMMENT 'Department Name',

    -- Dormitory assignments
    `dormitory_id` BIGINT DEFAULT NULL COMMENT 'Dormitory ID',
    `building_id` BIGINT DEFAULT NULL COMMENT 'Building ID',
    `building_name` VARCHAR(50) DEFAULT NULL COMMENT 'Building Name',
    `dormitory_no` VARCHAR(50) DEFAULT NULL COMMENT 'Dormitory Number',
    `bed_no` VARCHAR(20) DEFAULT NULL COMMENT 'Bed Number',
    `is_dorm_leader` TINYINT DEFAULT 0 COMMENT 'Is Dorm Leader (0/1)',

    -- Status
    `student_status` TINYINT DEFAULT NULL COMMENT 'Status (1=Active,2=Suspended,3=Withdrawn,4=Graduated)',

    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created At',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_date_student` (`snapshot_date`, `student_id`),
    KEY `idx_snapshot_date` (`snapshot_date`),
    KEY `idx_student` (`student_id`),
    KEY `idx_class` (`class_id`),
    KEY `idx_dormitory` (`dormitory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Student Relationship Snapshots';

-- ---------------------------------------------------------------------------
-- 3.3 Dormitory Member Snapshots - Dormitory composition at point in time
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `dormitory_member_snapshots`;
CREATE TABLE `dormitory_member_snapshots` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `snapshot_date` DATE NOT NULL COMMENT 'Snapshot Date',
    `record_id` BIGINT DEFAULT NULL COMMENT 'Related Check Record ID',

    -- Dormitory info
    `dormitory_id` BIGINT NOT NULL COMMENT 'Dormitory ID',
    `building_id` BIGINT DEFAULT NULL COMMENT 'Building ID',
    `building_name` VARCHAR(50) DEFAULT NULL COMMENT 'Building Name',
    `dormitory_no` VARCHAR(50) DEFAULT NULL COMMENT 'Dormitory Number',

    -- Members
    `members` JSON NOT NULL COMMENT 'Members Array [{studentId,studentNo,studentName,bedNo,isDormLeader}]',
    `member_count` INT DEFAULT 0 COMMENT 'Member Count',
    `leader_id` BIGINT DEFAULT NULL COMMENT 'Dorm Leader ID',
    `leader_name` VARCHAR(50) DEFAULT NULL COMMENT 'Dorm Leader Name',

    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created At',

    PRIMARY KEY (`id`),
    KEY `idx_snapshot_date` (`snapshot_date`),
    KEY `idx_dormitory` (`dormitory_id`),
    KEY `idx_record` (`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Dormitory Member Snapshots';

-- ---------------------------------------------------------------------------
-- 3.4 Analysis Snapshots - Statistical analysis point-in-time captures
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `analysis_snapshots`;
CREATE TABLE `analysis_snapshots` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `config_id` BIGINT NOT NULL COMMENT 'Analysis Config ID',
    `snapshot_name` VARCHAR(100) DEFAULT NULL COMMENT 'Snapshot Name',
    `snapshot_desc` VARCHAR(500) DEFAULT NULL COMMENT 'Snapshot Description',

    -- Scope info
    `record_ids` JSON DEFAULT NULL COMMENT 'Included Record IDs',
    `class_ids` JSON DEFAULT NULL COMMENT 'Included Class IDs',
    `date_range_start` DATE DEFAULT NULL COMMENT 'Data Start Date',
    `date_range_end` DATE DEFAULT NULL COMMENT 'Data End Date',

    -- Summary stats
    `record_count` INT DEFAULT 0 COMMENT 'Record Count',
    `class_count` INT DEFAULT 0 COMMENT 'Class Count',
    `total_score` DECIMAL(12,2) DEFAULT 0 COMMENT 'Total Deductions',
    `avg_score` DECIMAL(10,2) DEFAULT 0 COMMENT 'Average Deduction',

    -- Detailed data
    `overview_data` JSON DEFAULT NULL COMMENT 'Overview Data (JSON)',
    `metrics_data` JSON DEFAULT NULL COMMENT 'Metrics Results (JSON)',

    -- Metadata
    `generated_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Generated At',
    `generated_by` BIGINT DEFAULT NULL COMMENT 'Generated By User ID',
    `generated_by_name` VARCHAR(50) DEFAULT NULL COMMENT 'Generated By User Name',
    `is_auto` TINYINT(1) DEFAULT 0 COMMENT 'Is Auto-Generated (0/1)',
    `version` INT DEFAULT 1 COMMENT 'Version Number',

    PRIMARY KEY (`id`),
    KEY `idx_config_id` (`config_id`),
    KEY `idx_generated_at` (`generated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Analysis Snapshots';

-- ---------------------------------------------------------------------------
-- 3.5 Evaluation Result History - Historical evaluation records
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `evaluation_result_history`;
CREATE TABLE `evaluation_result_history` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',

    -- Original result reference
    `original_id` BIGINT NOT NULL COMMENT 'Original Result ID',
    `record_id` BIGINT NOT NULL COMMENT 'Check Record ID',

    -- Entity info
    `entity_type` VARCHAR(20) NOT NULL COMMENT 'Entity Type (CLASS/DORMITORY/STUDENT)',
    `entity_id` BIGINT NOT NULL COMMENT 'Entity ID',
    `entity_name` VARCHAR(100) DEFAULT NULL COMMENT 'Entity Name',

    -- Score info
    `base_score` DECIMAL(10,2) DEFAULT 100.00 COMMENT 'Base Score',
    `deduction_score` DECIMAL(10,2) DEFAULT 0.00 COMMENT 'Deduction Score',
    `final_score` DECIMAL(10,2) DEFAULT 100.00 COMMENT 'Final Score',
    `weighted_score` DECIMAL(10,2) DEFAULT NULL COMMENT 'Weighted Score',

    -- Rating
    `rating` VARCHAR(20) DEFAULT NULL COMMENT 'Rating (A/B/C/D/E)',

    -- Metadata
    `snapshot_reason` VARCHAR(100) DEFAULT NULL COMMENT 'Snapshot Reason',
    `snapshot_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Snapshot Time',
    `snapshot_by` BIGINT DEFAULT NULL COMMENT 'Snapshot By User ID',

    PRIMARY KEY (`id`),
    KEY `idx_original` (`original_id`),
    KEY `idx_record` (`record_id`),
    KEY `idx_entity` (`entity_type`, `entity_id`),
    KEY `idx_snapshot_at` (`snapshot_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Evaluation Result History';

-- ============================================================================
-- PART 4: LOGIN & SESSION TRACKING
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 4.1 Login Logs - User login/logout tracking
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `login_logs`;
CREATE TABLE `login_logs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `user_id` BIGINT DEFAULT NULL COMMENT 'User ID (NULL if login failed)',
    `username` VARCHAR(50) NOT NULL COMMENT 'Username',

    -- Login info
    `login_type` VARCHAR(20) NOT NULL DEFAULT 'PASSWORD' COMMENT 'Login Type (PASSWORD/WECHAT/SSO)',
    `login_status` TINYINT NOT NULL COMMENT 'Status (1=Success,0=Failed)',
    `login_message` VARCHAR(200) DEFAULT NULL COMMENT 'Login Message',

    -- Client info
    `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'IP Address',
    `user_agent` VARCHAR(500) DEFAULT NULL COMMENT 'User Agent',
    `browser` VARCHAR(50) DEFAULT NULL COMMENT 'Browser',
    `os` VARCHAR(50) DEFAULT NULL COMMENT 'Operating System',
    `device_type` VARCHAR(20) DEFAULT NULL COMMENT 'Device Type (PC/MOBILE/TABLET)',

    -- Location (optional)
    `location` VARCHAR(100) DEFAULT NULL COMMENT 'Location',

    -- Session
    `session_id` VARCHAR(100) DEFAULT NULL COMMENT 'Session ID',
    `logout_time` DATETIME DEFAULT NULL COMMENT 'Logout Time',
    `logout_type` VARCHAR(20) DEFAULT NULL COMMENT 'Logout Type (MANUAL/TIMEOUT/FORCED)',

    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Login Time',

    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_username` (`username`),
    KEY `idx_login_status` (`login_status`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_ip_address` (`ip_address`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Login Logs';

SET FOREIGN_KEY_CHECKS = 1;
