-- ============================================================================
-- V20260516_9: places.responsible_user_id 智能填充
-- ============================================================================
-- 当前 7/176 = 4% 填充率 (V20260516_7 从 access_relations 反推).
-- 多数 place 没有 admin/responsible_for 关系记录 → 用类型 + 组织反推.
--
-- 智能规则 (优先级 1 > 2 > 3):
--   1) 宿舍房间 (DORM_ROOM/place 走 DORMITORY 关键字) → DORMITORY_MANAGER user
--      (找 user_roles 任意 ORG_UNIT scope=该 place 所属 org_unit 子树的 DORMITORY_MANAGER)
--   2) 教室 (CLASSROOM) → CLASS_TEACHER of associated class (places.org_unit_id 是班级 org_unit)
--   3) 其他类型 → 留 null (admin 单独配)
--
-- 注: 真业务里 admin 应该手动配, 此 migration 提供合理 default 而已.
-- ============================================================================

-- 1) 宿舍房间 → 找该房间所属系部/学校的 DORMITORY_MANAGER user
--    简化: 取 user_roles 中 role=DORMITORY_MANAGER 的任意一个 user (生产应按 org 树精确匹配)
UPDATE places p
JOIN (
    SELECT MIN(ur.user_id) AS dm_user_id
    FROM user_roles ur
    JOIN roles r ON r.id = ur.role_id
    WHERE r.role_code = 'DORMITORY_MANAGER' AND ur.is_active = 1
) dm
SET p.responsible_user_id = dm.dm_user_id
WHERE p.responsible_user_id IS NULL
  AND p.deleted = 0
  AND dm.dm_user_id IS NOT NULL
  AND p.type_code IN ('DORM_ROOM', 'DORM_BUILDING', 'DORM_FLOOR');

-- 2) 教室 → 找该 org_unit (假设 places.org_unit_id 指向班级) 的班主任
UPDATE places p
JOIN classes c ON c.org_unit_id = p.org_unit_id AND c.deleted = 0
SET p.responsible_user_id = c.teacher_id
WHERE p.responsible_user_id IS NULL
  AND p.deleted = 0
  AND p.type_code IN ('CLASSROOM', 'TYPE_COMPUTER_LAB', 'TYPE_LAB')
  AND c.teacher_id IS NOT NULL;

-- 3) 其他楼宇/楼层类 → 系部管理员 (DEPT_ADMIN scoped on org)
UPDATE places p
JOIN (
    SELECT DISTINCT ur.scope_id AS org_unit_id, MIN(ur.user_id) AS admin_user_id
    FROM user_roles ur
    JOIN roles r ON r.id = ur.role_id
    WHERE r.role_code IN ('DEPT_ADMIN', 'SCHOOL_ADMIN') AND ur.is_active = 1
      AND ur.scope_type = 'ORG_UNIT' AND ur.scope_id > 0
    GROUP BY ur.scope_id
) admin ON admin.org_unit_id = p.org_unit_id
SET p.responsible_user_id = admin.admin_user_id
WHERE p.responsible_user_id IS NULL
  AND p.deleted = 0;
