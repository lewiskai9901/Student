-- ============================================================================
-- V20260516_5: user_roles 从 ALL 改为 ORG_UNIT scope (有边界角色)
-- ============================================================================
-- 当前 611 个 user_roles 全部 scope_type=ALL, scope_id=0.
-- DataPermissionInterceptor 设计先进的 scoped_roles 机制实际未使用.
--
-- 这次根据现有数据反推 ORG_UNIT scope:
-- - CLASS_TEACHER: 通过 classes.teacher_id 反推所管理的班级 (org_unit_id)
-- - GRADE_DIRECTOR: 通过 grade_directors.director_id 反推所管理的年级
-- - SUBJECT_TEACHER: access_relations 不在本 migration 范围
--
-- 策略: 同一 user 管多个班级 → 多个 user_roles 行 (uk_user_role 已支持).
-- 找不到归属的保持原 ALL 行 (admin 后续手动配).
-- 幂等: 用 INSERT IGNORE + 检查 scope_id 是否已存在.
-- ============================================================================

-- ====================================================
-- CLASS_TEACHER → classes.teacher_id (有则展开 ORG_UNIT)
-- ====================================================
-- 对每个 (user_id=CLASS_TEACHER, class.teacher_id=user, class.org_unit_id=X) 三元组,
-- INSERT 一条 ORG_UNIT scope_id=X 的 user_role.
INSERT IGNORE INTO user_roles (user_id, role_id, tenant_id, scope_type, scope_id, is_active,
                                assigned_at, granted_at, reason)
SELECT
    ur.user_id,
    ur.role_id,
    ur.tenant_id,
    'ORG_UNIT'                                AS scope_type,
    c.org_unit_id                              AS scope_id,
    1                                          AS is_active,
    NOW()                                      AS assigned_at,
    NOW()                                      AS granted_at,
    'V20260516_5 migration from classes.teacher_id' AS reason
FROM user_roles ur
JOIN roles r ON r.id = ur.role_id
JOIN classes c ON c.teacher_id = ur.user_id AND c.deleted = 0
WHERE r.role_code = 'CLASS_TEACHER'
  AND ur.is_active = 1;

-- ====================================================
-- GRADE_DIRECTOR → grade_directors.director_id
-- ====================================================
INSERT IGNORE INTO user_roles (user_id, role_id, tenant_id, scope_type, scope_id, is_active,
                                assigned_at, granted_at, reason)
SELECT
    ur.user_id,
    ur.role_id,
    ur.tenant_id,
    'ORG_UNIT'                                AS scope_type,
    gd.org_unit_id                             AS scope_id,
    1                                          AS is_active,
    NOW()                                      AS assigned_at,
    NOW()                                      AS granted_at,
    'V20260516_5 migration from grade_directors.director_id' AS reason
FROM user_roles ur
JOIN roles r ON r.id = ur.role_id
JOIN grade_directors gd ON gd.director_id = ur.user_id AND gd.deleted = 0
WHERE r.role_code = 'GRADE_DIRECTOR'
  AND ur.is_active = 1;

-- 注: 不删除原 ALL 行 — 留作 fallback (用户在 access_relations 没命中时,
-- 至少角色还在). 但 DataPermissionInterceptor 优先用 ORG_UNIT 行的 scope_id
-- 拼 effectiveOrgPath/effectiveOrgId, ALL 行只在没有 ORG_UNIT 行时兜底.
--
-- 后续 P5 阶段加 RoleScopeBindingContribution 时, 插件能声明 "CLASS_TEACHER
-- 默认应该有 ORG_UNIT scope", 启动期 enforce 而不是事后修补.
