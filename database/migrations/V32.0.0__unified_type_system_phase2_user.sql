-- ============================================================================
-- V32.0.0 - 统一类型系统 Phase 2: 用户类型重构
-- 与 OrgType 统一模式：category + features (JSON) + cross-references
-- ============================================================================

-- 1. 添加新列
ALTER TABLE user_types ADD COLUMN category VARCHAR(30) DEFAULT NULL COMMENT '分类: ADMIN/STAFF/MEMBER/EXTERNAL' AFTER parent_type_code;
ALTER TABLE user_types ADD COLUMN features JSON DEFAULT NULL COMMENT '行为特征 JSON {requiresOrg:bool, requiresPlace:bool, ...}' AFTER description;
ALTER TABLE user_types ADD COLUMN metadata_schema JSON DEFAULT NULL COMMENT '动态属性定义 JSON Schema' AFTER features;
ALTER TABLE user_types ADD COLUMN allowed_child_type_codes JSON DEFAULT NULL COMMENT '允许的子类型编码 JSON数组' AFTER metadata_schema;
ALTER TABLE user_types ADD COLUMN default_org_type_codes JSON DEFAULT NULL COMMENT '关联组织类型编码 JSON数组' AFTER default_role_codes;
ALTER TABLE user_types ADD COLUMN default_place_type_codes JSON DEFAULT NULL COMMENT '关联场所类型编码 JSON数组' AFTER default_org_type_codes;

-- 2. 迁移 category（根据现有 type_code 和 parent_type_code）
UPDATE user_types SET category = 'ADMIN' WHERE type_code IN ('ADMIN', 'SUPER_ADMIN', 'SYSTEM_ADMIN', 'ORG_ADMIN') AND deleted = 0;
UPDATE user_types SET category = 'STAFF' WHERE type_code IN ('TEACHER', 'TEACHING_STAFF', 'CLASS_TEACHER', 'COUNSELOR', 'INSPECTOR', 'DORM_MANAGER', 'ADMIN_STAFF') AND deleted = 0;
UPDATE user_types SET category = 'MEMBER' WHERE type_code IN ('STUDENT', 'UNDERGRADUATE', 'GRADUATE', 'EXCHANGE') AND deleted = 0;
UPDATE user_types SET category = 'EXTERNAL' WHERE type_code IN ('EXTERNAL', 'VISITOR', 'CONTRACTOR') AND deleted = 0;
-- 兜底: 未分类的根据 parent 设置
UPDATE user_types SET category = (SELECT ut2.category FROM (SELECT type_code, category FROM user_types WHERE deleted = 0) ut2 WHERE ut2.type_code = user_types.parent_type_code) WHERE category IS NULL AND parent_type_code IS NOT NULL AND deleted = 0;

-- 3. 迁移 requires_org / requires_place → features JSON
UPDATE user_types SET features = JSON_OBJECT(
    'requiresOrg', CAST(requires_org AS UNSIGNED) = 1,
    'requiresPlace', CAST(requires_place AS UNSIGNED) = 1
) WHERE deleted = 0;

-- 4. 迁移 default_role_codes 从逗号分隔字符串到 JSON 数组
-- 先处理有值的行（转为 JSON array）
UPDATE user_types SET default_role_codes = CONCAT('["', REPLACE(TRIM(default_role_codes), ',', '","'), '"]')
WHERE default_role_codes IS NOT NULL AND TRIM(default_role_codes) != '' AND deleted = 0;
-- 空值统一设为 NULL
UPDATE user_types SET default_role_codes = NULL WHERE TRIM(COALESCE(default_role_codes, '')) IN ('', '[""]') AND deleted = 0;

-- 5. 设置 allowed_child_type_codes
UPDATE user_types SET allowed_child_type_codes = '["SUPER_ADMIN","SYSTEM_ADMIN","ORG_ADMIN"]' WHERE type_code = 'ADMIN' AND deleted = 0;
UPDATE user_types SET allowed_child_type_codes = '["TEACHING_STAFF","CLASS_TEACHER","COUNSELOR","INSPECTOR","DORM_MANAGER","ADMIN_STAFF"]' WHERE type_code = 'TEACHER' AND deleted = 0;
UPDATE user_types SET allowed_child_type_codes = '["UNDERGRADUATE","GRADUATE","EXCHANGE"]' WHERE type_code = 'STUDENT' AND deleted = 0;
UPDATE user_types SET allowed_child_type_codes = '["VISITOR","CONTRACTOR"]' WHERE type_code = 'EXTERNAL' AND deleted = 0;

-- 6. 删除旧列
ALTER TABLE user_types DROP COLUMN level_order;
ALTER TABLE user_types DROP COLUMN requires_org;
ALTER TABLE user_types DROP COLUMN requires_place;

-- 7. 添加索引
CREATE INDEX idx_user_types_category ON user_types(category);
