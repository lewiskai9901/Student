-- ============================================================================
-- Phase 3 W3.2: family_of 上移到 COMMON_EXT, 合并 EDU.guardian_of + HEALTH.family_of
-- ============================================================================
-- 背景:
--   原 EducationManifest 声明 guardian_of (DOMAIN, EDU)
--   原 HealthcareManifest 声明 family_of (DOMAIN, HEALTH)
--   两者语义重复 (家属/家长). 上移到 CommonExtManifest 的 COMMON_EXT.family_of.
--
-- 数据策略:
--   1. 字典 relation_types: 旧 EDU/HEALTH 行 软禁用 (is_enabled=0), 留作历史.
--   2. 新启动时 RelationTypeUpserter 会插入 COMMON_EXT.family_of (registered_by=CommonExtPlugin).
--   3. access_relations 表里旧 guardian_of/family_of 实例数据保留(历史关系不动).
--      新业务请直接使用 family_of (将由 COMMON_EXT 注册 enabled=1).
-- ============================================================================

UPDATE relation_types
SET is_enabled = 0,
    description = CONCAT(IFNULL(description,''), ' [DEPRECATED → 用 COMMON_EXT.family_of]')
WHERE relation_code = 'guardian_of'
  AND tier = 'DOMAIN'
  AND is_enabled = 1;

UPDATE relation_types
SET is_enabled = 0,
    description = CONCAT(IFNULL(description,''), ' [DEPRECATED → 用 COMMON_EXT.family_of]')
WHERE relation_code = 'family_of'
  AND tier = 'DOMAIN'
  AND is_enabled = 1;
