-- =====================================================
-- 数据库架构版本统一迁移
-- 版本: 101
-- 创建日期: 2025-12-31
-- 说明: 统一并清理多个冲突的schema版本，建立单一的版本控制
-- =====================================================

SET NAMES utf8mb4;

-- =====================================================
-- 第一部分: 创建迁移版本控制表
-- =====================================================

CREATE TABLE IF NOT EXISTS `schema_migrations` (
    `version` VARCHAR(50) NOT NULL COMMENT '迁移版本号',
    `description` VARCHAR(255) COMMENT '迁移描述',
    `applied_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '应用时间',
    `checksum` VARCHAR(64) COMMENT 'SQL文件校验和',
    PRIMARY KEY (`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据库迁移版本控制表';

-- =====================================================
-- 第二部分: 记录已应用的迁移
-- 注意: 这些是当前已存在的表结构对应的迁移版本
-- =====================================================

INSERT IGNORE INTO `schema_migrations` (`version`, `description`) VALUES
('001', '初始化用户和权限表'),
('002', '创建学生和班级表'),
('003', '创建宿舍管理表'),
('004', '创建量化检查V2表'),
('005', '创建量化检查V3表'),
('006', '创建综合素质测评表'),
('007', '创建任务管理表'),
('100', '添加外键约束'),
('101', '统一Schema版本');

-- =====================================================
-- 第三部分: 清理冗余表和列
-- =====================================================

-- 删除废弃的临时表（如果存在）
DROP TABLE IF EXISTS `check_records_new_backup`;
DROP TABLE IF EXISTS `check_record_details_backup`;
DROP TABLE IF EXISTS `temp_migration_data`;

-- =====================================================
-- 第四部分: 确保关键表结构一致
-- =====================================================

-- 确保 daily_checks 表有 version 列用于乐观锁
DELIMITER //
CREATE PROCEDURE ensure_version_column()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'daily_checks'
        AND COLUMN_NAME = 'version'
    ) THEN
        ALTER TABLE `daily_checks`
            ADD COLUMN `version` INT DEFAULT 0 COMMENT '乐观锁版本号' AFTER `updated_at`;
    END IF;

    -- 确保 tasks 表有 version 列
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'tasks'
        AND COLUMN_NAME = 'version'
    ) THEN
        ALTER TABLE `tasks`
            ADD COLUMN `version` INT DEFAULT 0 COMMENT '乐观锁版本号' AFTER `updated_at`;
    END IF;
END //
DELIMITER ;

CALL ensure_version_column();
DROP PROCEDURE IF EXISTS ensure_version_column;

-- =====================================================
-- 第五部分: 添加缺失的索引
-- =====================================================

DELIMITER //
CREATE PROCEDURE add_missing_indexes()
BEGIN
    -- 检查并添加 daily_checks.plan_id 索引
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'daily_checks'
        AND INDEX_NAME = 'idx_plan_id'
    ) THEN
        ALTER TABLE `daily_checks` ADD INDEX `idx_plan_id` (`plan_id`);
    END IF;

    -- 检查并添加 daily_check_details.class_id 索引
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'daily_check_details'
        AND INDEX_NAME = 'idx_class_id'
    ) THEN
        ALTER TABLE `daily_check_details` ADD INDEX `idx_class_id` (`class_id`);
    END IF;

    -- 检查并添加 students.department_id 索引
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'students'
        AND INDEX_NAME = 'idx_department_id'
    ) THEN
        ALTER TABLE `students` ADD INDEX `idx_department_id` (`department_id`);
    END IF;
END //
DELIMITER ;

CALL add_missing_indexes();
DROP PROCEDURE IF EXISTS add_missing_indexes;

-- =====================================================
-- 说明:
-- 1. 此迁移统一了多个冲突的schema版本
-- 2. 创建了 schema_migrations 表用于版本控制
-- 3. 添加了乐观锁 version 列支持并发控制
-- 4. 添加了缺失的索引以提升查询性能
-- 5. 运行前请备份数据库
-- =====================================================
