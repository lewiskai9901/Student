-- 角色自定义数据范围表 - 添加多维度支持
-- V2升级：支持组织单元、年级、班级三个维度的自定义范围

-- 添加scope_type字段（范围类型）
ALTER TABLE role_custom_scope
ADD COLUMN scope_type VARCHAR(20) NULL COMMENT '范围类型：ORG_UNIT=组织单元, GRADE=年级, CLASS=班级' AFTER module_code;

-- 添加target_id字段（统一的目标ID）
ALTER TABLE role_custom_scope
ADD COLUMN target_id BIGINT NULL COMMENT '目标ID（组织单元ID/年级ID/班级ID）' AFTER scope_type;

-- 数据迁移：将现有数据的scope_type设置为ORG_UNIT，target_id设置为org_unit_id
UPDATE role_custom_scope
SET scope_type = 'ORG_UNIT', target_id = org_unit_id
WHERE scope_type IS NULL;

-- 修改唯一键，支持多维度
-- 先删除旧的唯一键（如果存在）
DROP PROCEDURE IF EXISTS drop_index_if_exists;
DELIMITER //
CREATE PROCEDURE drop_index_if_exists()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'role_custom_scope' AND index_name = 'uk_role_module_org') THEN
        ALTER TABLE role_custom_scope DROP INDEX uk_role_module_org;
    END IF;
END //
DELIMITER ;
CALL drop_index_if_exists();
DROP PROCEDURE IF EXISTS drop_index_if_exists;

-- 创建新的唯一键（role_id + module_code + scope_type + target_id）- 如果不存在
DROP PROCEDURE IF EXISTS add_unique_key_if_not_exists;
DELIMITER //
CREATE PROCEDURE add_unique_key_if_not_exists()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'role_custom_scope' AND index_name = 'uk_role_module_scope_target') THEN
        ALTER TABLE role_custom_scope ADD UNIQUE KEY uk_role_module_scope_target (role_id, module_code, scope_type, target_id);
    END IF;
END //
DELIMITER ;
CALL add_unique_key_if_not_exists();
DROP PROCEDURE IF EXISTS add_unique_key_if_not_exists;

-- 添加索引（如果不存在）
DROP PROCEDURE IF EXISTS add_indexes_if_not_exists;
DELIMITER //
CREATE PROCEDURE add_indexes_if_not_exists()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'role_custom_scope' AND index_name = 'idx_scope_type') THEN
        ALTER TABLE role_custom_scope ADD INDEX idx_scope_type (scope_type);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'role_custom_scope' AND index_name = 'idx_target_id') THEN
        ALTER TABLE role_custom_scope ADD INDEX idx_target_id (target_id);
    END IF;
END //
DELIMITER ;
CALL add_indexes_if_not_exists();
DROP PROCEDURE IF EXISTS add_indexes_if_not_exists;
