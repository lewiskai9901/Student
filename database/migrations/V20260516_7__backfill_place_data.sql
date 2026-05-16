-- ============================================================================
-- V20260516_7: 批补 places.org_unit_id + responsible_user_id
-- ============================================================================
-- 当前 places 176 行:
--   org_unit_id 填充率 8/176 = 4.5% (DEPARTMENT_AND_BELOW 数据权限对场所基本失效)
--   responsible_user_id 填充率 1/176 = 0.6%
--
-- 反推规则:
--
-- 1) org_unit_id 反推: 走父子继承 — places.parent_id 形成树,
--    从根节点找出最近设了 org_unit_id 的祖先, 子孙继承.
--
-- 2) responsible_user_id 反推: 走 access_relations.admin/responsible_for
--    relation = user owns place. 没找到的留 null (admin 后续手动配).
--
-- 注: 真生产数据可能跨组织 (例如食堂归后勤但物理在某系楼内),
-- 此批补只是 best-effort. 已批的 admin 可在 UI 单独调整.
-- ============================================================================

-- ====================================================
-- 1) places.org_unit_id 沿父链反推
-- ====================================================
-- 递归找 org_unit_id 不为 null 的最近祖先, MySQL 8+ 用 CTE
WITH RECURSIVE place_ancestors AS (
    -- 锚: 所有 places
    SELECT id, parent_id, org_unit_id, 0 AS depth, id AS leaf_id
    FROM places WHERE deleted = 0
    UNION ALL
    -- 递归: 没填 org_unit_id 的, 沿父链向上找
    SELECT p.id, p.parent_id, p.org_unit_id, pa.depth + 1, pa.leaf_id
    FROM places p
    JOIN place_ancestors pa ON p.id = pa.parent_id
    WHERE pa.org_unit_id IS NULL AND p.deleted = 0 AND pa.depth < 20
)
UPDATE places p
JOIN (
    SELECT leaf_id, MIN(depth) AS min_depth FROM place_ancestors
    WHERE org_unit_id IS NOT NULL
    GROUP BY leaf_id
) closest ON closest.leaf_id = p.id
JOIN place_ancestors pa ON pa.leaf_id = closest.leaf_id AND pa.depth = closest.min_depth
SET p.org_unit_id = pa.org_unit_id
WHERE p.org_unit_id IS NULL AND p.deleted = 0;

-- ====================================================
-- 2) places.responsible_user_id 从 access_relations 反推
-- ====================================================
-- relation IN ('admin', 'responsible_for', 'manages') 且 resource_type='place'
-- 取最早设的那个 (created_at ASC).
UPDATE places p
JOIN (
    SELECT
        ar.resource_id AS place_id,
        MIN(ar.subject_id) AS user_id
    FROM access_relations ar
    WHERE ar.deleted = 0
      AND ar.resource_type = 'place'
      AND ar.subject_type = 'user'
      AND ar.relation IN ('admin', 'responsible_for', 'manages')
    GROUP BY ar.resource_id
) responsible ON responsible.place_id = p.id
SET p.responsible_user_id = responsible.user_id
WHERE p.responsible_user_id IS NULL AND p.deleted = 0;
