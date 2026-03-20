-- V31.0.0 统一类型系统 Phase 1：组织类型重构
-- 添加 category, features, metadata_schema, allowed_child_type_codes, default_user_type_codes, default_place_type_codes
-- 删除旧字段 can_be_inspected, can_have_children, level_order, color

-- =============================================
-- 1. 添加新字段
-- =============================================
ALTER TABLE org_unit_types
    ADD COLUMN category VARCHAR(30) DEFAULT NULL COMMENT '内置分类: ROOT/BRANCH/FUNCTIONAL/GROUP/CONTAINER' AFTER type_name,
    ADD COLUMN features JSON DEFAULT NULL COMMENT '行为特征开关' AFTER description,
    ADD COLUMN metadata_schema JSON DEFAULT NULL COMMENT '动态扩展属性定义 (AttributeSchema)' AFTER features,
    ADD COLUMN allowed_child_type_codes JSON DEFAULT NULL COMMENT '允许的子类型编码列表' AFTER metadata_schema,
    ADD COLUMN default_user_type_codes JSON DEFAULT NULL COMMENT '关联的默认用户类型编码' AFTER allowed_child_type_codes,
    ADD COLUMN default_place_type_codes JSON DEFAULT NULL COMMENT '关联的默认场所类型编码' AFTER default_user_type_codes;

-- =============================================
-- 2. 迁移现有数据到 category + features
-- =============================================

-- 根据 type_code 设置 category
UPDATE org_unit_types SET category = 'ROOT'       WHERE type_code = 'ORGANIZATION' AND deleted = 0;
UPDATE org_unit_types SET category = 'BRANCH'     WHERE type_code = 'DIVISION'     AND deleted = 0;
UPDATE org_unit_types SET category = 'FUNCTIONAL' WHERE type_code = 'DEPARTMENT'   AND deleted = 0;
UPDATE org_unit_types SET category = 'FUNCTIONAL' WHERE type_code = 'SECTION'      AND deleted = 0;
UPDATE org_unit_types SET category = 'GROUP'      WHERE type_code = 'TEAM'         AND deleted = 0;
UPDATE org_unit_types SET category = 'CONTAINER'  WHERE type_code = 'GRADE'        AND deleted = 0;
UPDATE org_unit_types SET category = 'GROUP'      WHERE type_code = 'CLASS'        AND deleted = 0;
-- 其他未匹配的默认设为 FUNCTIONAL
UPDATE org_unit_types SET category = 'FUNCTIONAL' WHERE category IS NULL AND deleted = 0;

-- 迁移 can_be_inspected / can_have_children 到 features JSON
UPDATE org_unit_types SET features = JSON_OBJECT(
    'dataPermissionBoundary', (category IN ('ROOT', 'BRANCH')),
    'inspectionTarget', COALESCE(can_be_inspected, TRUE),
    'memberManagement', (category IN ('FUNCTIONAL', 'GROUP')),
    'attendance', (category = 'GROUP'),
    'scheduling', FALSE
) WHERE deleted = 0;

-- 设置 allowed_child_type_codes (根据现有层级关系)
UPDATE org_unit_types SET allowed_child_type_codes = JSON_ARRAY('DIVISION', 'DEPARTMENT', 'GRADE')
    WHERE type_code = 'ORGANIZATION' AND deleted = 0;
UPDATE org_unit_types SET allowed_child_type_codes = JSON_ARRAY('DEPARTMENT', 'SECTION')
    WHERE type_code = 'DIVISION' AND deleted = 0;
UPDATE org_unit_types SET allowed_child_type_codes = JSON_ARRAY('SECTION', 'TEAM')
    WHERE type_code = 'DEPARTMENT' AND deleted = 0;
UPDATE org_unit_types SET allowed_child_type_codes = JSON_ARRAY('CLASS')
    WHERE type_code = 'GRADE' AND deleted = 0;

-- 设置跨领域关联
UPDATE org_unit_types SET default_user_type_codes = JSON_ARRAY('ADMIN')
    WHERE type_code = 'ORGANIZATION' AND deleted = 0;
UPDATE org_unit_types SET default_user_type_codes = JSON_ARRAY('TEACHER', 'ADMIN_STAFF')
    WHERE type_code = 'DEPARTMENT' AND deleted = 0;
UPDATE org_unit_types SET default_user_type_codes = JSON_ARRAY('STUDENT', 'CLASS_TEACHER')
    WHERE type_code = 'CLASS' AND deleted = 0;

-- =============================================
-- 3. 删除旧字段
-- =============================================
ALTER TABLE org_unit_types
    DROP COLUMN can_be_inspected,
    DROP COLUMN can_have_children,
    DROP COLUMN level_order,
    DROP COLUMN color;

-- =============================================
-- 4. 添加索引
-- =============================================
CREATE INDEX idx_org_type_category ON org_unit_types (category);
