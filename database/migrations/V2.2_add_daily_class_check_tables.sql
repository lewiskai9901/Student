-- ========================================
-- 量化管理系统 2.0 - 补充每日检查表和班级检查表
-- Version: 2.2
-- Date: 2025-10-23
-- Description: 添加 quantification_daily_records 和 quantification_class_checks 表
-- ========================================

USE student_management;

-- ========================================
-- 1. 每日检查记录表
-- ========================================
CREATE TABLE IF NOT EXISTS quantification_daily_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    record_code VARCHAR(50) NOT NULL UNIQUE COMMENT '记录编号,如DR20250119-001',
    task_id BIGINT NOT NULL COMMENT '关联的任务ID',
    task_name VARCHAR(100) COMMENT '任务名称(冗余)',
    check_date DATE NOT NULL COMMENT '检查日期',
    grade_id BIGINT COMMENT '年级ID',
    grade_name VARCHAR(50) COMMENT '年级名称(冗余)',
    checker_id BIGINT COMMENT '检查员ID',
    checker_name VARCHAR(50) COMMENT '检查员姓名(冗余)',
    total_class_count INT DEFAULT 0 COMMENT '应检查班级数',
    checked_class_count INT DEFAULT 0 COMMENT '已检查班级数',
    status TINYINT DEFAULT 1 COMMENT '状态:1-进行中 2-已完成',
    completed_at DATETIME COMMENT '完成时间',
    remarks TEXT COMMENT '备注',
    created_by BIGINT COMMENT '创建人',
    updated_by BIGINT COMMENT '更新人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除:0-未删除 1-已删除',
    INDEX idx_task_id (task_id),
    INDEX idx_check_date (check_date),
    INDEX idx_grade_id (grade_id),
    INDEX idx_status (status),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日检查记录表';

-- ========================================
-- 2. 班级检查明细表
-- ========================================
CREATE TABLE IF NOT EXISTS quantification_class_checks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    daily_record_id BIGINT NOT NULL COMMENT '每日检查记录ID',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    class_id BIGINT NOT NULL COMMENT '班级ID',
    class_name VARCHAR(100) COMMENT '班级名称(冗余)',
    grade_id BIGINT COMMENT '年级ID',
    total_deduction DECIMAL(10,2) DEFAULT 0 COMMENT '总扣分',
    deduction_count INT DEFAULT 0 COMMENT '扣分次数',
    check_status TINYINT DEFAULT 1 COMMENT '检查状态:1-待检查 2-检查中 3-已完成',
    checked_at DATETIME COMMENT '检查完成时间',
    checker_id BIGINT COMMENT '检查员ID',
    checker_name VARCHAR(50) COMMENT '检查员姓名(冗余)',
    remarks TEXT COMMENT '备注',
    created_by BIGINT COMMENT '创建人',
    updated_by BIGINT COMMENT '更新人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除:0-未删除 1-已删除',
    INDEX idx_daily_record_id (daily_record_id),
    INDEX idx_task_id (task_id),
    INDEX idx_class_id (class_id),
    INDEX idx_grade_id (grade_id),
    INDEX idx_check_status (check_status),
    INDEX idx_deleted (deleted),
    UNIQUE KEY uk_daily_record_class (daily_record_id, class_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级检查明细表';

-- ========================================
-- 3. 修改检查扣分记录表,添加班级检查ID关联
-- ========================================
-- 检查列是否存在,不存在则添加
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'student_management'
    AND TABLE_NAME = 'quantification_check_records'
    AND COLUMN_NAME = 'class_check_id');

SET @sql_add_column = IF(@column_exists = 0,
    'ALTER TABLE quantification_check_records ADD COLUMN class_check_id BIGINT COMMENT ''班级检查明细ID'' AFTER id',
    'SELECT ''Column class_check_id already exists'' AS message');

PREPARE stmt FROM @sql_add_column;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加索引
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'student_management'
    AND TABLE_NAME = 'quantification_check_records'
    AND INDEX_NAME = 'idx_class_check_id');

SET @sql_add_index = IF(@index_exists = 0,
    'ALTER TABLE quantification_check_records ADD INDEX idx_class_check_id (class_check_id)',
    'SELECT ''Index idx_class_check_id already exists'' AS message');

PREPARE stmt FROM @sql_add_index;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ========================================
-- 4. 说明
-- ========================================
-- quantification_daily_records: 每日检查记录,一天一条或多条(多个年级)
-- quantification_class_checks: 班级检查明细,每个班级一条
-- quantification_check_records: 具体的扣分记录,每次扣分一条,关联到班级检查明细

-- 数据流转:
-- quantification_tasks (任务)
--   ↓
-- quantification_daily_records (每日检查)
--   ↓
-- quantification_class_checks (班级检查明细)
--   ↓
-- quantification_check_records (扣分记录)

-- ========================================
-- 完成
-- ========================================
