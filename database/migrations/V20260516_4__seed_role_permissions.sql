-- ============================================================================
-- V20260516_4: 补 10 个 0 权限角色的 role_permissions
-- ============================================================================
-- 这些角色 user_roles 有人,role_data_scopes 有规则,但 role_permissions 0 条 —
-- Casbin 拦截器拒绝任何 @CasbinAccess 端点(没 super_admin bypass 时).
-- 按角色职责映射到现有 permission_code.
--
-- 设计:
--   SCHOOL_ADMIN: 全校 view+edit (除了 super 级 system:*)
--   ACADEMIC_DIRECTOR: 教务全权 view+edit + 其他 view
--   COUNSELOR: 学生工作 view + inspection 提交
--   DORMITORY_MANAGER: 宿舍全权 + 学生 view
--   GRADE_DIRECTOR: 年级 view + 部分 edit
--   CLASS_TEACHER: 班级 view + 学生 view
--   SUBJECT_TEACHER: 教学 view+edit + 成绩
--   GUEST: 最基础 view
--   STUDENT: 自己数据 view
--   TENANT_ADMIN: 系统 + 角色 + 用户 全权
-- ============================================================================

-- ====================================================
-- SCHOOL_ADMIN — 全校管理 view+edit (除 system:*)
-- ====================================================
INSERT IGNORE INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.id, p.id, 1
FROM roles r CROSS JOIN permissions p
WHERE r.role_code = 'SCHOOL_ADMIN'
  AND (
    p.permission_code IN ('dashboard:view')
    OR p.permission_code LIKE 'student:%'
    OR p.permission_code LIKE 'academic:%'
    OR p.permission_code LIKE 'teaching:%'
    OR p.permission_code LIKE 'insp:%:view'
    OR p.permission_code LIKE 'insp:%:edit'
    OR p.permission_code LIKE 'insp:%:create'
    OR p.permission_code LIKE 'insp:appeal:%'
    OR p.permission_code LIKE 'insp:corrective:%'
    OR p.permission_code LIKE 'place:%'
    OR p.permission_code LIKE 'asset:%:view'
    OR p.permission_code LIKE 'notification:%'
  );

-- ====================================================
-- ACADEMIC_DIRECTOR — 教务全权
-- ====================================================
INSERT IGNORE INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.id, p.id, 1
FROM roles r CROSS JOIN permissions p
WHERE r.role_code = 'ACADEMIC_DIRECTOR'
  AND (
    p.permission_code = 'dashboard:view'
    OR p.permission_code LIKE 'academic:%'
    OR p.permission_code LIKE 'teaching:%'
    OR p.permission_code LIKE 'student:info:view'
    OR p.permission_code LIKE 'student:class:view'
    OR p.permission_code LIKE 'student:class:edit'
    OR p.permission_code LIKE 'student:department:view'
    OR p.permission_code LIKE 'notification:view'
  );

-- ====================================================
-- COUNSELOR — 学生工作 view
-- ====================================================
INSERT IGNORE INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.id, p.id, 1
FROM roles r CROSS JOIN permissions p
WHERE r.role_code = 'COUNSELOR'
  AND (
    p.permission_code = 'dashboard:view'
    OR p.permission_code LIKE 'student:info:view'
    OR p.permission_code LIKE 'student:info:edit'
    OR p.permission_code LIKE 'student:class:view'
    OR p.permission_code LIKE 'student:dormitory:view'
    OR p.permission_code LIKE 'insp:%:view'
    OR p.permission_code LIKE 'insp:appeal:%'
    OR p.permission_code LIKE 'insp:corrective:%'
    OR p.permission_code LIKE 'notification:%'
  );

-- ====================================================
-- DORMITORY_MANAGER — 宿舍全权 + 学生 view
-- ====================================================
INSERT IGNORE INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.id, p.id, 1
FROM roles r CROSS JOIN permissions p
WHERE r.role_code = 'DORMITORY_MANAGER'
  AND (
    p.permission_code = 'dashboard:view'
    OR p.permission_code LIKE 'student:dormitory:%'
    OR p.permission_code = 'student:info:view'
    OR p.permission_code LIKE 'insp:%:view'
    OR p.permission_code LIKE 'insp:%:create'
    OR p.permission_code LIKE 'insp:%:edit'
    OR p.permission_code LIKE 'place:%:view'
    OR p.permission_code LIKE 'notification:%'
  );

-- ====================================================
-- GRADE_DIRECTOR — 年级 view + 部分 edit
-- ====================================================
INSERT IGNORE INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.id, p.id, 1
FROM roles r CROSS JOIN permissions p
WHERE r.role_code = 'GRADE_DIRECTOR'
  AND (
    p.permission_code = 'dashboard:view'
    OR p.permission_code LIKE 'student:info:view'
    OR p.permission_code LIKE 'student:info:edit'
    OR p.permission_code LIKE 'student:class:view'
    OR p.permission_code LIKE 'academic:%:view'
    OR p.permission_code LIKE 'teaching:%:view'
    OR p.permission_code LIKE 'insp:%:view'
    OR p.permission_code LIKE 'notification:%'
  );

-- ====================================================
-- CLASS_TEACHER — 班级 view + 学生 view+edit
-- ====================================================
INSERT IGNORE INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.id, p.id, 1
FROM roles r CROSS JOIN permissions p
WHERE r.role_code = 'CLASS_TEACHER'
  AND (
    p.permission_code = 'dashboard:view'
    OR p.permission_code LIKE 'student:info:view'
    OR p.permission_code LIKE 'student:info:edit'
    OR p.permission_code LIKE 'student:class:view'
    OR p.permission_code LIKE 'student:dormitory:view'
    OR p.permission_code LIKE 'teaching:%:view'
    OR p.permission_code LIKE 'insp:%:view'
    OR p.permission_code LIKE 'insp:corrective:%'
    OR p.permission_code LIKE 'notification:%'
  );

-- ====================================================
-- SUBJECT_TEACHER — 教学 view+edit + 成绩
-- ====================================================
INSERT IGNORE INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.id, p.id, 1
FROM roles r CROSS JOIN permissions p
WHERE r.role_code = 'SUBJECT_TEACHER'
  AND (
    p.permission_code = 'dashboard:view'
    OR p.permission_code LIKE 'teaching:%'
    OR p.permission_code LIKE 'student:info:view'
    OR p.permission_code LIKE 'student:class:view'
    OR p.permission_code LIKE 'academic:%:view'
    OR p.permission_code LIKE 'insp:%:view'
    OR p.permission_code LIKE 'notification:view'
  );

-- ====================================================
-- GUEST — 最基础 view
-- ====================================================
INSERT IGNORE INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.id, p.id, 1
FROM roles r CROSS JOIN permissions p
WHERE r.role_code = 'GUEST'
  AND p.permission_code IN ('dashboard:view', 'notification:view');

-- ====================================================
-- STUDENT — 自己数据 view (走 SELF scope)
-- ====================================================
INSERT IGNORE INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.id, p.id, 1
FROM roles r CROSS JOIN permissions p
WHERE r.role_code = 'STUDENT'
  AND (
    p.permission_code = 'dashboard:view'
    OR p.permission_code LIKE 'student:info:view'
    OR p.permission_code LIKE 'insp:%:view'
    OR p.permission_code = 'insp:appeal:view'
    OR p.permission_code LIKE 'notification:view'
  );

-- ====================================================
-- TENANT_ADMIN — 系统 + 角色 + 用户 全权
-- ====================================================
INSERT IGNORE INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.id, p.id, 1
FROM roles r CROSS JOIN permissions p
WHERE r.role_code = 'TENANT_ADMIN'
  AND (
    p.permission_code = 'dashboard:view'
    OR p.permission_code LIKE 'system:%'
    OR p.permission_code LIKE 'role:%'
    OR p.permission_code LIKE 'permission:%'
    OR p.permission_code LIKE 'user:%'
    OR p.permission_code LIKE 'tenant:%'
    OR p.permission_code LIKE 'organization:%'
  );
