-- 角色自定义数据范围表
-- 用于存储角色对特定模块的自定义组织单元访问权限
CREATE TABLE IF NOT EXISTS role_custom_scope (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    module_code VARCHAR(50) NOT NULL COMMENT '模块代码',
    org_unit_id BIGINT NOT NULL COMMENT '组织单元ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_role_module_org (role_id, module_code, org_unit_id),
    KEY idx_role_id (role_id),
    KEY idx_module_code (module_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色自定义数据范围';

-- 更新现有role_data_permissions表，添加scope_code字段支持字符串类型
-- MySQL 8.0 不支持 ADD COLUMN IF NOT EXISTS, 用 information_schema 条件化
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='role_data_permissions' AND COLUMN_NAME='scope_code');
SET @s := IF(@c=0, "ALTER TABLE role_data_permissions ADD COLUMN scope_code VARCHAR(50) NULL COMMENT '数据范围代码(V2)' AFTER data_scope", "SELECT 1");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 数据迁移：将整数scope转换为字符串scope_code
UPDATE role_data_permissions SET scope_code = 'all' WHERE data_scope = 1 AND scope_code IS NULL;
UPDATE role_data_permissions SET scope_code = 'department' WHERE data_scope = 2 AND scope_code IS NULL;
UPDATE role_data_permissions SET scope_code = 'grade' WHERE data_scope = 3 AND scope_code IS NULL;
UPDATE role_data_permissions SET scope_code = 'class_only' WHERE data_scope = 4 AND scope_code IS NULL;
UPDATE role_data_permissions SET scope_code = 'self' WHERE data_scope = 5 AND scope_code IS NULL;
