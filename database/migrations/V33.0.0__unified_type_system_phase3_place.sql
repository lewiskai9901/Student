-- ============================================================
-- V33.0.0 统一类型系统 Phase 3: place_types 重构
-- 将 Universal PlaceType 适配统一类型模式
-- ============================================================

-- 1. 添加新列
ALTER TABLE place_types
    ADD COLUMN features JSON DEFAULT NULL COMMENT '行为特征 JSON {hasCapacity, bookable, assignable, occupiable}' AFTER description,
    ADD COLUMN parent_type_code VARCHAR(50) DEFAULT NULL COMMENT '父类型编码 (替代 parent_type_id)' AFTER base_category,
    ADD COLUMN max_depth INT DEFAULT NULL COMMENT '允许的最大层级深度' AFTER parent_type_code,
    ADD COLUMN default_user_type_codes JSON DEFAULT NULL COMMENT '关联用户类型编码列表' AFTER attribute_schema,
    ADD COLUMN default_org_type_codes JSON DEFAULT NULL COMMENT '关联组织类型编码列表' AFTER default_user_type_codes;

-- 2. 迁移 behavior flags → features JSON
UPDATE place_types
SET features = JSON_OBJECT(
    'hasCapacity', COALESCE(has_capacity, 0) = 1,
    'bookable', COALESCE(bookable, 0) = 1,
    'assignable', COALESCE(assignable, 0) = 1,
    'occupiable', COALESCE(occupiable, 0) = 1
)
WHERE deleted = 0;

-- 3. 迁移 parent_type_id → parent_type_code (自连接填充)
UPDATE place_types child
    INNER JOIN place_types parent ON child.parent_type_id = parent.id
SET child.parent_type_code = parent.type_code
WHERE child.parent_type_id IS NOT NULL AND child.deleted = 0;

-- 4. 重命名列: base_category → category
ALTER TABLE place_types CHANGE COLUMN base_category category VARCHAR(50) DEFAULT NULL COMMENT '分类编码 (SITE/BUILDING/FLOOR/ROOM/AREA/POINT)';

-- 5. 重命名列: attribute_schema → metadata_schema
ALTER TABLE place_types CHANGE COLUMN attribute_schema metadata_schema TEXT DEFAULT NULL COMMENT '扩展属性 JSON Schema';

-- 6. 重命名列: allowed_child_types → allowed_child_type_codes
ALTER TABLE place_types CHANGE COLUMN allowed_child_types allowed_child_type_codes JSON DEFAULT NULL COMMENT '允许的子类型编码列表';

-- 7. 删除旧列
ALTER TABLE place_types
    DROP COLUMN parent_type_id,
    DROP COLUMN has_capacity,
    DROP COLUMN bookable,
    DROP COLUMN assignable,
    DROP COLUMN occupiable,
    DROP COLUMN level_order;

-- 8. 更新 deleted 列类型 (soft-delete-stores-id pattern)
ALTER TABLE place_types MODIFY COLUMN deleted BIGINT DEFAULT 0 COMMENT '逻辑删除 (0=正常, >0=删除时的ID)';
