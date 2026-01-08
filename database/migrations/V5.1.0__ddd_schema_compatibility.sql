-- ============================================================================
-- Student Management System - DDD Schema Compatibility Migration V5.1.0
-- Ensures existing tables are compatible with new DDD PO classes
-- Date: 2026-01-03
-- ============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------------
-- 1. Tasks table - Add version column for optimistic locking
-- ---------------------------------------------------------------------------
ALTER TABLE `tasks`
ADD COLUMN IF NOT EXISTS `version` INT DEFAULT 0 COMMENT 'Optimistic Lock Version' AFTER `attachment_ids`;

-- ---------------------------------------------------------------------------
-- 2. Students table - Ensure all columns exist for StudentPO
-- ---------------------------------------------------------------------------
-- Check if students table exists, if not create it
CREATE TABLE IF NOT EXISTS `students` (
    `id` BIGINT NOT NULL COMMENT 'Primary Key',
    `student_no` VARCHAR(50) NOT NULL COMMENT 'Student Number',
    `name` VARCHAR(100) NOT NULL COMMENT 'Student Name',
    `gender` TINYINT DEFAULT 1 COMMENT 'Gender: 1-Male, 2-Female',
    `id_card` VARCHAR(18) COMMENT 'ID Card Number',
    `phone` VARCHAR(20) COMMENT 'Phone Number',
    `email` VARCHAR(100) COMMENT 'Email',
    `birth_date` DATE COMMENT 'Birth Date',
    `enrollment_date` DATE COMMENT 'Enrollment Date',
    `expected_graduation_date` DATE COMMENT 'Expected Graduation Date',
    `class_id` BIGINT COMMENT 'Class ID',
    `dormitory_id` BIGINT COMMENT 'Dormitory ID',
    `bed_number` INT COMMENT 'Bed Number',
    `status` TINYINT DEFAULT 1 COMMENT 'Status: 1-Studying, 2-Suspended, 3-Withdrawn, 4-Graduated, 5-Expelled',
    `avatar_url` VARCHAR(500) COMMENT 'Avatar URL',
    `home_address` VARCHAR(500) COMMENT 'Home Address',
    `emergency_contact` VARCHAR(50) COMMENT 'Emergency Contact',
    `emergency_phone` VARCHAR(20) COMMENT 'Emergency Phone',
    `remark` TEXT COMMENT 'Remark',
    `deleted` INT DEFAULT 0 COMMENT 'Logical Delete Flag',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created At',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated At',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_student_no` (`student_no`),
    KEY `idx_class_id` (`class_id`),
    KEY `idx_dormitory_id` (`dormitory_id`),
    KEY `idx_status` (`status`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Students Table';

-- Add missing columns to students table if they don't exist
-- Note: Using stored procedure for conditional column additions

DELIMITER //

CREATE PROCEDURE add_column_if_not_exists(
    IN table_name VARCHAR(100),
    IN column_name VARCHAR(100),
    IN column_definition VARCHAR(500)
)
BEGIN
    DECLARE column_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO column_exists
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = table_name
      AND COLUMN_NAME = column_name;

    IF column_exists = 0 THEN
        SET @sql = CONCAT('ALTER TABLE `', table_name, '` ADD COLUMN `', column_name, '` ', column_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //

DELIMITER ;

-- Add missing columns to students
CALL add_column_if_not_exists('students', 'expected_graduation_date', 'DATE COMMENT ''Expected Graduation Date''');
CALL add_column_if_not_exists('students', 'avatar_url', 'VARCHAR(500) COMMENT ''Avatar URL''');
CALL add_column_if_not_exists('students', 'home_address', 'VARCHAR(500) COMMENT ''Home Address''');
CALL add_column_if_not_exists('students', 'emergency_contact', 'VARCHAR(50) COMMENT ''Emergency Contact''');
CALL add_column_if_not_exists('students', 'emergency_phone', 'VARCHAR(20) COMMENT ''Emergency Phone''');
CALL add_column_if_not_exists('students', 'remark', 'TEXT COMMENT ''Remark''');

-- ---------------------------------------------------------------------------
-- 3. Buildings table - Ensure all columns exist for BuildingPO
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `buildings` (
    `id` BIGINT NOT NULL COMMENT 'Primary Key',
    `building_no` VARCHAR(50) NOT NULL COMMENT 'Building Number',
    `building_name` VARCHAR(100) NOT NULL COMMENT 'Building Name',
    `building_type` TINYINT DEFAULT 1 COMMENT 'Type: 1-Teaching, 2-Dormitory, 3-Office, 4-Other',
    `total_floors` INT COMMENT 'Total Floors',
    `location` VARCHAR(200) COMMENT 'Location',
    `construction_year` INT COMMENT 'Construction Year',
    `description` TEXT COMMENT 'Description',
    `status` TINYINT DEFAULT 1 COMMENT 'Status: 0-Disabled, 1-Enabled',
    `deleted` INT DEFAULT 0 COMMENT 'Logical Delete Flag',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created At',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated At',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_building_no` (`building_no`),
    KEY `idx_building_type` (`building_type`),
    KEY `idx_status` (`status`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Buildings Table';

-- Add missing columns to buildings
CALL add_column_if_not_exists('buildings', 'total_floors', 'INT COMMENT ''Total Floors''');
CALL add_column_if_not_exists('buildings', 'location', 'VARCHAR(200) COMMENT ''Location''');
CALL add_column_if_not_exists('buildings', 'construction_year', 'INT COMMENT ''Construction Year''');
CALL add_column_if_not_exists('buildings', 'description', 'TEXT COMMENT ''Description''');

-- ---------------------------------------------------------------------------
-- 4. Dormitories table - Ensure all columns exist for DormitoryPO
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `dormitories` (
    `id` BIGINT NOT NULL COMMENT 'Primary Key',
    `building_id` BIGINT NOT NULL COMMENT 'Building ID',
    `department_id` BIGINT COMMENT 'Department ID',
    `dormitory_no` VARCHAR(50) NOT NULL COMMENT 'Dormitory Number',
    `floor_number` INT COMMENT 'Floor Number',
    `room_usage_type` TINYINT DEFAULT 1 COMMENT 'Usage Type: 1-Student, 2-Staff, 3-Guest',
    `bed_capacity` INT DEFAULT 4 COMMENT 'Bed Capacity',
    `bed_count` INT DEFAULT 0 COMMENT 'Actual Bed Count',
    `occupied_beds` INT DEFAULT 0 COMMENT 'Occupied Beds',
    `gender_type` TINYINT DEFAULT 0 COMMENT 'Gender Type: 0-Mixed, 1-Male, 2-Female',
    `facilities` VARCHAR(500) COMMENT 'Facilities',
    `notes` TEXT COMMENT 'Notes',
    `status` TINYINT DEFAULT 1 COMMENT 'Status: 0-Disabled, 1-Available, 2-Full, 3-Maintenance',
    `deleted` INT DEFAULT 0 COMMENT 'Logical Delete Flag',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created At',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated At',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dormitory_no` (`building_id`, `dormitory_no`),
    KEY `idx_building_id` (`building_id`),
    KEY `idx_department_id` (`department_id`),
    KEY `idx_status` (`status`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Dormitories Table';

-- Add missing columns to dormitories
CALL add_column_if_not_exists('dormitories', 'room_usage_type', 'TINYINT DEFAULT 1 COMMENT ''Usage Type: 1-Student, 2-Staff, 3-Guest''');
CALL add_column_if_not_exists('dormitories', 'bed_capacity', 'INT DEFAULT 4 COMMENT ''Bed Capacity''');
CALL add_column_if_not_exists('dormitories', 'bed_count', 'INT DEFAULT 0 COMMENT ''Actual Bed Count''');
CALL add_column_if_not_exists('dormitories', 'occupied_beds', 'INT DEFAULT 0 COMMENT ''Occupied Beds''');
CALL add_column_if_not_exists('dormitories', 'gender_type', 'TINYINT DEFAULT 0 COMMENT ''Gender Type: 0-Mixed, 1-Male, 2-Female''');
CALL add_column_if_not_exists('dormitories', 'facilities', 'VARCHAR(500) COMMENT ''Facilities''');
CALL add_column_if_not_exists('dormitories', 'notes', 'TEXT COMMENT ''Notes''');

-- ---------------------------------------------------------------------------
-- 5. Create student_dormitory_assignments table for tracking check-in/out
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `student_dormitory_assignments` (
    `id` BIGINT NOT NULL COMMENT 'Primary Key',
    `student_id` BIGINT NOT NULL COMMENT 'Student ID',
    `dormitory_id` BIGINT NOT NULL COMMENT 'Dormitory ID',
    `bed_number` INT NOT NULL COMMENT 'Bed Number',
    `check_in_date` DATE NOT NULL COMMENT 'Check In Date',
    `check_out_date` DATE COMMENT 'Check Out Date',
    `status` TINYINT DEFAULT 1 COMMENT 'Status: 1-Active, 0-Inactive',
    `remark` VARCHAR(500) COMMENT 'Remark',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created At',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated At',
    `deleted` INT DEFAULT 0 COMMENT 'Logical Delete Flag',
    PRIMARY KEY (`id`),
    KEY `idx_student_id` (`student_id`),
    KEY `idx_dormitory_id` (`dormitory_id`),
    KEY `idx_status` (`status`),
    UNIQUE KEY `uk_active_assignment` (`student_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Student Dormitory Assignments Table';

-- ---------------------------------------------------------------------------
-- 6. Cleanup: Drop the stored procedure
-- ---------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS add_column_if_not_exists;

-- ---------------------------------------------------------------------------
-- 7. Create indexes for better query performance
-- ---------------------------------------------------------------------------
-- Students indexes
CREATE INDEX IF NOT EXISTS `idx_students_name` ON `students` (`name`);
CREATE INDEX IF NOT EXISTS `idx_students_enrollment_date` ON `students` (`enrollment_date`);

-- Buildings indexes
CREATE INDEX IF NOT EXISTS `idx_buildings_name` ON `buildings` (`building_name`);

-- Dormitories indexes
CREATE INDEX IF NOT EXISTS `idx_dormitories_floor` ON `dormitories` (`floor_number`);

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- Migration Complete
-- This script ensures backward compatibility with existing data while
-- supporting new DDD architecture PO classes
-- ============================================================================
