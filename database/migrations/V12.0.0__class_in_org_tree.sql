-- ============================================================
-- V12.0.0: 班级融入组织架构 (Shared ID + Extension Table)
-- 让班级成为组织树的叶子节点
-- ============================================================

-- 1. 添加 CLASS 类型到 org_unit_types（如果不存在）
INSERT INTO org_unit_types (type_code, type_name, parent_type_code, level_order,
    icon, color, description,
    can_be_inspected, can_have_children, max_depth, is_system, is_enabled, sort_order)
SELECT 'CLASS', '班级', 'DEPARTMENT', 5,
    'Users', '#3B82F6', '教学班级',
    1, 0, 0, 1, 1, 50
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM org_unit_types WHERE type_code = 'CLASS');

-- 2. 为现有班级创建 org_units 行（Shared ID）
-- classes.id 和 org_units.id 共享同一个 Snowflake ID
INSERT INTO org_units (id, unit_code, unit_name, unit_type, type_code,
    parent_id, tree_path, tree_level, leader_id, sort_order, status,
    created_at, created_by, deleted)
SELECT
    c.id,
    c.class_code,
    c.class_name,
    'CLASS',
    'CLASS',
    c.org_unit_id,
    CONCAT(COALESCE(p.tree_path, '/'), c.id, '/'),
    COALESCE(p.tree_level, 0) + 1,
    c.teacher_id,
    0,
    CASE WHEN c.status = 1 THEN 1 ELSE 0 END,
    COALESCE(c.created_at, NOW()),
    c.created_by,
    COALESCE(c.deleted, 0)
FROM classes c
LEFT JOIN org_units p ON p.id = c.org_unit_id AND p.deleted = 0
WHERE NOT EXISTS (SELECT 1 FROM org_units ou WHERE ou.id = c.id);
