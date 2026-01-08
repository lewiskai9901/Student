-- ============================================================
-- V2.1: 添加评价配置和评价结果表
-- 用于支持 DDD 架构下的 Rating Domain
-- ============================================================

-- 评价配置表
CREATE TABLE IF NOT EXISTS `rating_configs` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `check_plan_id` BIGINT NOT NULL COMMENT '检查计划ID',
    `rating_name` VARCHAR(100) NOT NULL COMMENT '评价名称',
    `period_type` VARCHAR(20) NOT NULL DEFAULT 'DAILY' COMMENT '周期类型: DAILY/WEEKLY/MONTHLY/SEMESTER',
    `division_method` VARCHAR(20) NOT NULL DEFAULT 'TOP_N' COMMENT '划分方式: TOP_N/PERCENTAGE/THRESHOLD',
    `division_value` DECIMAL(10,2) DEFAULT 3.00 COMMENT '划分值',
    `icon` VARCHAR(100) DEFAULT NULL COMMENT '图标',
    `color` VARCHAR(20) DEFAULT NULL COMMENT '颜色(十六进制)',
    `priority` INT DEFAULT 0 COMMENT '优先级',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
    `require_approval` TINYINT(1) DEFAULT 1 COMMENT '是否需要审批',
    `auto_publish` TINYINT(1) DEFAULT 0 COMMENT '是否自动发布',
    `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人',
    `deleted` TINYINT(1) DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    INDEX `idx_check_plan_id` (`check_plan_id`),
    INDEX `idx_period_type` (`period_type`),
    INDEX `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价配置表';

-- 评价结果表
CREATE TABLE IF NOT EXISTS `rating_results` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `rating_config_id` BIGINT NOT NULL COMMENT '评价配置ID',
    `check_plan_id` BIGINT DEFAULT NULL COMMENT '检查计划ID',
    `class_id` BIGINT NOT NULL COMMENT '班级ID',
    `class_name` VARCHAR(100) DEFAULT NULL COMMENT '班级名称',
    `period_type` VARCHAR(20) DEFAULT NULL COMMENT '周期类型',
    `period_start` DATE DEFAULT NULL COMMENT '周期开始日期',
    `period_end` DATE DEFAULT NULL COMMENT '周期结束日期',
    `ranking` INT DEFAULT NULL COMMENT '排名',
    `final_score` DECIMAL(10,2) DEFAULT NULL COMMENT '最终分数',
    `awarded` TINYINT(1) DEFAULT 0 COMMENT '是否获奖',
    `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/SUBMITTED/APPROVED/REJECTED/PUBLISHED/REVOKED',
    `calculated_at` DATETIME DEFAULT NULL COMMENT '计算时间',
    `submitted_at` DATETIME DEFAULT NULL COMMENT '提交时间',
    `approved_by` BIGINT DEFAULT NULL COMMENT '审批人',
    `approved_at` DATETIME DEFAULT NULL COMMENT '审批时间',
    `approval_comment` VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
    `published_by` BIGINT DEFAULT NULL COMMENT '发布人',
    `published_at` DATETIME DEFAULT NULL COMMENT '发布时间',
    `revoked_by` BIGINT DEFAULT NULL COMMENT '撤销人',
    `revoked_at` DATETIME DEFAULT NULL COMMENT '撤销时间',
    `reject_reason` VARCHAR(500) DEFAULT NULL COMMENT '拒绝原因',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    INDEX `idx_rating_config_id` (`rating_config_id`),
    INDEX `idx_class_id` (`class_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_period` (`period_start`, `period_end`),
    CONSTRAINT `fk_rating_result_config` FOREIGN KEY (`rating_config_id`) REFERENCES `rating_configs`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价结果表';

-- ============================================================
-- 更新 semesters 表，添加缺失的字段 (MySQL 5.7 兼容)
-- ============================================================

-- 使用存储过程安全添加列
DELIMITER //

DROP PROCEDURE IF EXISTS add_column_if_not_exists//

CREATE PROCEDURE add_column_if_not_exists()
BEGIN
    -- 添加 start_year 字段
    IF NOT EXISTS (
        SELECT * FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'semesters' AND COLUMN_NAME = 'start_year'
    ) THEN
        ALTER TABLE `semesters` ADD COLUMN `start_year` INT DEFAULT NULL COMMENT '开始年份' AFTER `end_date`;
    END IF;

    -- 添加 semester_type 字段
    IF NOT EXISTS (
        SELECT * FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'semesters' AND COLUMN_NAME = 'semester_type'
    ) THEN
        ALTER TABLE `semesters` ADD COLUMN `semester_type` INT DEFAULT NULL COMMENT '学期类型: 1第一学期 2第二学期' AFTER `start_year`;
    END IF;

    -- 添加索引 idx_semester_code
    IF NOT EXISTS (
        SELECT * FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'semesters' AND INDEX_NAME = 'idx_semester_code'
    ) THEN
        CREATE INDEX `idx_semester_code` ON `semesters`(`semester_code`);
    END IF;

    -- 添加索引 idx_is_current
    IF NOT EXISTS (
        SELECT * FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'semesters' AND INDEX_NAME = 'idx_is_current'
    ) THEN
        CREATE INDEX `idx_is_current` ON `semesters`(`is_current`);
    END IF;

    -- 添加索引 idx_start_year
    IF NOT EXISTS (
        SELECT * FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'semesters' AND INDEX_NAME = 'idx_start_year'
    ) THEN
        CREATE INDEX `idx_start_year` ON `semesters`(`start_year`);
    END IF;
END//

DELIMITER ;

CALL add_column_if_not_exists();
DROP PROCEDURE IF EXISTS add_column_if_not_exists;
