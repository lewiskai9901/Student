-- ============================================================================
-- V20260516_6: 从 user_roles ORG_UNIT scope 反推 access_relations (history backfill)
-- ============================================================================
-- V20260516_5 已经把可推断的 CLASS_TEACHER / GRADE_DIRECTOR user_roles 改成
-- ORG_UNIT scope. 现在反推同样数据写到 access_relations, 让 ReBAC 子查询和
-- plugin Resolver 能命中.
--
-- 未来路径:AccessEventHandler.handleUserRoleAssigned 会自动写 access_relation
-- (V20260516 同步 commit), 所以新分配的角色不需要再走这条 migration.
-- 此 migration 只补 historical 数据.
--
-- 幂等: 用 NOT EXISTS 检查 (relation, subject_id, resource_id, resource_type) 唯一.
-- ============================================================================

INSERT INTO access_relations
    (resource_type, resource_id, relation, subject_type, subject_id,
     access_level, valid_from, remark, tenant_id, created_by, created_at)
SELECT
    'org_unit'                AS resource_type,
    ur.scope_id                AS resource_id,
    r.role_code COLLATE utf8mb4_unicode_ci AS relation,
    'user'                     AS subject_type,
    ur.user_id                 AS subject_id,
    'FULL'                     AS access_level,
    NOW()                      AS valid_from,
    'V20260516_6 backfill from user_roles ORG_UNIT scope' AS remark,
    ur.tenant_id,
    NULL                       AS created_by,
    NOW()                      AS created_at
FROM user_roles ur
JOIN roles r ON r.id = ur.role_id
WHERE ur.scope_type = 'ORG_UNIT'
  AND ur.scope_id IS NOT NULL AND ur.scope_id > 0
  AND ur.is_active = 1
  AND NOT EXISTS (
      SELECT 1 FROM access_relations ar
      WHERE ar.deleted = 0
        AND ar.resource_type = 'org_unit'
        AND ar.resource_id = ur.scope_id
        AND ar.relation COLLATE utf8mb4_unicode_ci = r.role_code COLLATE utf8mb4_unicode_ci
        AND ar.subject_type = 'user'
        AND ar.subject_id = ur.user_id
  );

-- 也从 classes.teacher_id 直接反推 CLASS_TEACHER access_relations (兜底覆盖)
-- 即使 user_roles 没有该 user_id 的 ORG_UNIT 行, 只要 classes.teacher_id 指向就有效
INSERT INTO access_relations
    (resource_type, resource_id, relation, subject_type, subject_id,
     access_level, valid_from, remark, tenant_id, created_by, created_at)
SELECT
    'org_unit'                AS resource_type,
    c.org_unit_id              AS resource_id,
    'CLASS_TEACHER'            AS relation,
    'user'                     AS subject_type,
    c.teacher_id               AS subject_id,
    'FULL'                     AS access_level,
    NOW()                      AS valid_from,
    'V20260516_6 backfill from classes.teacher_id' AS remark,
    c.tenant_id,
    NULL                       AS created_by,
    NOW()                      AS created_at
FROM classes c
WHERE c.teacher_id IS NOT NULL
  AND c.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM access_relations ar
      WHERE ar.deleted = 0
        AND ar.resource_type = 'org_unit'
        AND ar.resource_id = c.org_unit_id
        AND ar.relation = 'CLASS_TEACHER'
        AND ar.subject_type = 'user'
        AND ar.subject_id = c.teacher_id
  );

-- 从 grade_directors.director_id 反推 GRADE_DIRECTOR access_relations
INSERT INTO access_relations
    (resource_type, resource_id, relation, subject_type, subject_id,
     access_level, valid_from, remark, tenant_id, created_by, created_at)
SELECT
    'org_unit'                AS resource_type,
    gd.org_unit_id             AS resource_id,
    'GRADE_DIRECTOR'           AS relation,
    'user'                     AS subject_type,
    gd.director_id             AS subject_id,
    'FULL'                     AS access_level,
    NOW()                      AS valid_from,
    'V20260516_6 backfill from grade_directors.director_id' AS remark,
    gd.tenant_id,
    NULL                       AS created_by,
    NOW()                      AS created_at
FROM grade_directors gd
WHERE gd.director_id IS NOT NULL
  AND gd.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM access_relations ar
      WHERE ar.deleted = 0
        AND ar.resource_type = 'org_unit'
        AND ar.resource_id = gd.org_unit_id
        AND ar.relation = 'GRADE_DIRECTOR'
        AND ar.subject_type = 'user'
        AND ar.subject_id = gd.director_id
  );
