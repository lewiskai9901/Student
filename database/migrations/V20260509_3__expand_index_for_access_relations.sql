-- ============================================================
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
-- V20260509_3: access_relations 加 expand 专用索引
--
-- 背景: AccessRelationService.expand(resourceType, resourceId, relation) 是高频反查,
--   主查询: WHERE resource_type=? AND resource_id=? AND relation=? AND deleted=0
--           AND (valid_to IS NULL OR valid_to > NOW())
--
-- 现有索引 (V25 / V20260419_1):
--   idx_resource          (resource_type, resource_id, deleted)
--   idx_subject           (subject_type, subject_id, deleted)
--   idx_lookup            (resource_type, relation, subject_type, subject_id, deleted)  ← resource_id 不在前缀
--   idx_validity          (valid_from, valid_to)
--   idx_relation_validity (relation, valid_to, deleted)
--
-- idx_lookup 的列顺序错 — expand() 给定 resource_id, 但它不在前缀, 会走全表扫前两列再过滤.
-- 新建 idx_expand 4 列前缀完全覆盖 expand 主查询, 显著降低 IO.
--
-- 条件化 (项目惯例, 参考 V97/V104) — 重复执行幂等.
-- ============================================================

SET @stmt := IF(
    (SELECT COUNT(1) FROM information_schema.statistics
       WHERE table_schema = DATABASE()
         AND table_name   = 'access_relations'
         AND index_name   = 'idx_expand') = 0,
    'ALTER TABLE access_relations
        ADD INDEX idx_expand (resource_type, resource_id, relation, deleted)',
    'SELECT 1 -- idx_expand already exists, skip');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;
