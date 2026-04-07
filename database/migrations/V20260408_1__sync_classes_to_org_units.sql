-- Phase 3: 同步 classes 数据到 org_units
-- 为每个 class 创建对应的 org_unit (type_code=CLASS)
-- 并将扩展属性写入 org_units.attributes

-- 获取学校根节点（作为默认 parent）
SET @school_id = (SELECT id FROM org_units WHERE parent_id IS NULL AND deleted = 0 ORDER BY id LIMIT 1);

-- 为每个 class 创建 org_unit（如果尚未存在 CLASS 类型）
INSERT INTO org_units (id, unit_code, unit_name, unit_type, type_code, parent_id, tree_level, status, attributes, created_at, updated_at, deleted, tenant_id)
SELECT
    c.id + 9000000000000000000,  -- 避免 ID 冲突
    c.class_code,
    c.class_name,
    'CLASS',
    'CLASS',
    COALESCE(c.org_unit_id, @school_id),
    3,
    'ACTIVE',
    JSON_OBJECT(
        'enrollmentYear', CAST(c.enrollment_year AS UNSIGNED),
        'majorId', c.major_id,
        'headTeacher', c.teacher_id,
        'duration', COALESCE(c.duration, 3),
        'classType', COALESCE(c.class_type, 1),
        'sourceClassId', c.id
    ),
    NOW(), NOW(), 0, 1
FROM classes c
WHERE c.deleted = 0
AND NOT EXISTS (
    SELECT 1 FROM org_units o
    WHERE o.unit_code = c.class_code AND o.type_code = 'CLASS' AND o.deleted = 0
);

-- 回写 org_unit_id 到 classes 表（指向新创建的 org_unit）
UPDATE classes c
JOIN org_units o ON o.unit_code = c.class_code AND o.type_code = 'CLASS' AND o.deleted = 0
SET c.org_unit_id = o.id
WHERE c.deleted = 0;

SELECT CONCAT('Synced ', COUNT(*), ' classes to org_units') AS result
FROM org_units WHERE type_code = 'CLASS' AND deleted = 0;
