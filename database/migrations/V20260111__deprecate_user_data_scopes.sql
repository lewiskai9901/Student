-- V20260111__deprecate_user_data_scopes.sql
-- 废弃用户级数据权限配置，统一使用角色级配置

-- 1. 清空 user_data_scopes 表数据
DELETE FROM user_data_scopes;

-- 2. 删除 users 表的 managed_class_id 字段（如果存在）
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME = 'users'
               AND COLUMN_NAME = 'managed_class_id');

SET @query := IF(@exist > 0,
    'ALTER TABLE users DROP COLUMN managed_class_id',
    'SELECT "Column managed_class_id does not exist" AS message');

PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 添加注释说明表已废弃
ALTER TABLE user_data_scopes COMMENT = '[DEPRECATED] 用户数据范围表 - 已废弃，使用角色级配置 role_data_permissions';
