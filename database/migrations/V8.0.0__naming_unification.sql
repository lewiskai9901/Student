-- ============================================================================
-- V8.0.0 命名统一改造脚本
-- 日期: 2026-02-01
-- 说明: 确保所有组织相关表和字段命名统一
-- ============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------------
-- 1. 验证 org_units 表存在且结构正确
-- ---------------------------------------------------------------------------
-- org_units 表已在 V5.0.0 创建，此处仅验证

-- 确保 unit_category 字段存在（如果不存在则添加）
SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'org_units'
    AND COLUMN_NAME = 'unit_category'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE org_units ADD COLUMN unit_category VARCHAR(50) DEFAULT NULL COMMENT ''组织类别'' AFTER unit_type',
    'SELECT ''unit_category column already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 2. 更新权限代码: department -> org
-- ---------------------------------------------------------------------------
UPDATE permissions
SET permission_code = REPLACE(permission_code, 'department', 'org'),
    permission_name = REPLACE(permission_name, '部门', '组织')
WHERE permission_code LIKE '%department%';

-- ---------------------------------------------------------------------------
-- 3. 更新菜单名称
-- ---------------------------------------------------------------------------
UPDATE menus
SET menu_name = REPLACE(menu_name, '部门', '组织')
WHERE menu_name LIKE '%部门%';

-- ---------------------------------------------------------------------------
-- 4. 验证数据迁移完整性（仅查询，不修改）
-- ---------------------------------------------------------------------------
-- 以下查询用于验证，实际执行时可注释掉

-- 检查 org_units 表数据量
-- SELECT 'org_units count' as check_item, COUNT(*) as count FROM org_units WHERE deleted = 0;

-- 检查是否还有 departments 表残留数据
-- SELECT 'departments count (if exists)' as check_item,
--        CASE WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'departments')
--             THEN (SELECT COUNT(*) FROM departments WHERE deleted = 0)
--             ELSE 0 END as count;

-- ---------------------------------------------------------------------------
-- 5. 清理废弃的 departments 表（可选，建议在确认迁移完成后手动执行）
-- ---------------------------------------------------------------------------
-- 备份旧表（如果存在）
-- CREATE TABLE IF NOT EXISTS departments_backup_20260201 AS SELECT * FROM departments;

-- 注意：暂不删除旧表，保留作为备份
-- DROP TABLE IF EXISTS departments;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- 迁移完成
-- 执行后请验证:
-- 1. org_units 表数据完整
-- 2. 权限代码已更新
-- 3. 菜单名称已更新
-- ============================================================================
