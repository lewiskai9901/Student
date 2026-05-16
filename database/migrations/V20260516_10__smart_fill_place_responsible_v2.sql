-- ============================================================================
-- V20260516_10: places.responsible_user_id 智能填充 v2 (修 V20260516_9 错误的 type_code)
-- ============================================================================
-- V20260516_9 用了错误的 type_code (CLASSROOM 应为 TYPE_CLASSROOM, LAB 应为 TYPE_LAB).
-- 且生产无 DORMITORY_MANAGER user_role 分配, 第一规则跳过. 改为更鲁棒的多 fallback.
--
-- Fallback 链:
--   1) access_relations admin/responsible_for (V20260516_7 已批补, 7 行)
--   2) 教室类 (TYPE_CLASSROOM) → 该班级 teacher_id
--   3) 同组织 admin user (任何 DEPT_ADMIN/SCHOOL_ADMIN 的 user_id, 暂用 ALL scope 任意 user)
--   4) 所有 DORM_* 类型 → 系统 admin user_id=1 (SUPER_ADMIN) 兜底, 后续 admin 改
-- ============================================================================

-- 1) 教室类 → 班级 teacher_id (修了 type_code)
UPDATE places p
JOIN classes c ON c.org_unit_id = p.org_unit_id AND c.deleted = 0
SET p.responsible_user_id = c.teacher_id
WHERE p.responsible_user_id IS NULL
  AND p.deleted = 0
  AND p.type_code IN ('TYPE_CLASSROOM', 'TYPE_COMPUTER_LAB', 'TYPE_LAB', 'CLASSROOM')
  AND c.teacher_id IS NOT NULL;

-- 2) 同组织有 user_roles ORG_UNIT scope 的任意管理员 → 取最小 user_id
UPDATE places p
JOIN (
    SELECT ur.scope_id AS org_unit_id, MIN(ur.user_id) AS admin_user_id
    FROM user_roles ur
    JOIN roles r ON r.id = ur.role_id
    WHERE r.role_code IN ('DEPT_ADMIN', 'SCHOOL_ADMIN', 'ACADEMIC_DIRECTOR')
      AND ur.is_active = 1
      AND ur.scope_type = 'ORG_UNIT' AND ur.scope_id > 0
    GROUP BY ur.scope_id
) admin ON admin.org_unit_id = p.org_unit_id
SET p.responsible_user_id = admin.admin_user_id
WHERE p.responsible_user_id IS NULL AND p.deleted = 0;

-- 3) 仍 null 的 → 取该 org_unit 任意 ALL scope 管理员 (TENANT_ADMIN / SCHOOL_ADMIN / etc)
UPDATE places p
SET p.responsible_user_id = (
    SELECT MIN(u.id) FROM users u
    JOIN user_roles ur ON ur.user_id = u.id AND ur.is_active = 1
    JOIN roles r ON r.id = ur.role_id AND r.deleted = 0
    WHERE r.role_code IN ('SUPER_ADMIN', 'TENANT_ADMIN', 'SCHOOL_ADMIN', 'DEPT_ADMIN')
      AND u.deleted = 0 AND u.status = 1
)
WHERE p.responsible_user_id IS NULL AND p.deleted = 0;
