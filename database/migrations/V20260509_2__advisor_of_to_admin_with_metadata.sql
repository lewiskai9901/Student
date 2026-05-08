-- ============================================================================
-- Phase 3 W3.3: advisor_of (EDU) 合并到 admin (CORE) + metadata.role
-- ============================================================================
-- 背景:
--   原 EducationManifest.advisor_of (user → org_unit) 与 CORE.admin (user → org_unit)
--   语义重合 (都是组织负责人). 现统一用 CoreRelations.ADMIN, 通过 metadata.role
--   ('ADVISOR' / 'CLASS_TEACHER' / 'COUNSELOR' 等) 区分子角色.
--
-- 数据策略:
--   1. 已存在的 access_relations.relation='advisor_of' → 改为 'admin' + metadata.role='ADVISOR'
--   2. relation_types 字典里 advisor_of 行 软禁用 (is_enabled=0), 留作历史
--   3. 关系历史表 (history) 保留原 'advisor_of' 不动 — 审计需要
-- ============================================================================

-- 1) 现有关系数据: advisor_of → admin, 写 metadata.role='ADVISOR'
UPDATE access_relations
SET relation = 'admin',
    metadata = JSON_SET(IFNULL(metadata, JSON_OBJECT()), '$.role', 'ADVISOR')
WHERE relation = 'advisor_of'
  AND deleted = 0;

-- 2) 字典: advisor_of 行 软禁用
UPDATE relation_types
SET is_enabled = 0,
    description = CONCAT(IFNULL(description,''), ' [DEPRECATED → admin + metadata.role=ADVISOR]')
WHERE relation_code = 'advisor_of'
  AND is_enabled = 1;
