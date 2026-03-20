-- =============================================
-- 将学校专有组织类型替换为通用类型
-- 执行前请确认旧类型未被 org_units 引用
-- =============================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 1. 删除旧的学校专有类型（不含 DEPARTMENT，它是通用的保留）
DELETE FROM org_unit_types WHERE type_code IN (
  'SCHOOL', 'COLLEGE', 'TEACHING_GROUP',
  'STUDENT_AFFAIRS', 'ACADEMIC_AFFAIRS',
  'LOGISTICS', 'FINANCE', 'GENERAL_OFFICE', 'HR'
) AND deleted = 0;

-- 2. 插入新的通用类型
INSERT INTO org_unit_types (type_code, type_name, parent_type_code, level_order, is_academic, can_have_children, is_system, is_enabled, sort_order)
VALUES
  ('ORGANIZATION', '组织',   NULL,           1, FALSE, TRUE,  TRUE, TRUE, 1),
  ('DIVISION',     '事业部', 'ORGANIZATION', 2, FALSE, TRUE,  TRUE, TRUE, 2),
  ('SECTION',      '科室',   'DEPARTMENT',   3, FALSE, TRUE,  TRUE, TRUE, 4),
  ('TEAM',         '小组',   'SECTION',      4, FALSE, FALSE, TRUE, TRUE, 5)
ON DUPLICATE KEY UPDATE
  type_name = VALUES(type_name),
  parent_type_code = VALUES(parent_type_code),
  level_order = VALUES(level_order),
  can_have_children = VALUES(can_have_children),
  sort_order = VALUES(sort_order);

-- 3. 更新已有的 DEPARTMENT 类型
UPDATE org_unit_types
SET type_name = '部门',
    parent_type_code = 'ORGANIZATION',
    level_order = 2,
    can_have_children = TRUE,
    sort_order = 3
WHERE type_code = 'DEPARTMENT' AND deleted = 0;
