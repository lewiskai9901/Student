-- ============================================================================
-- V20260612_2: 场所归属/负责人关系化 (A3: 关系真相 + 物化投影)
--
-- 背景: 对齐 2026-05-31 用户归属统一重构, 清除平台最后一处"归属表示法分裂"。
--   - 场所→组织归属: places.org_unit_id 列(NULL=继承父场所) → access_relations
--     `belongs_to|place|org_unit` 关系 = 真相源(只存覆盖点, 无关系=继承)。
--   - places.org_unit_id 改名 effective_org_unit_id, 语义改为**投影列**(解析后
--     的有效组织, 含继承), 由 PlaceOrgProjector 维护, 业务代码禁直写。
--     数据权限 @DataPermission(orgUnitField) 改过此列 — 顺修"继承态场所匹配
--     不到数据权限过滤"的缺陷。
--   - 场所负责人: places.responsible_user_id 列 → access_relations
--     `responsible_for|user|place` 关系(覆盖点), 列直接 DROP, 无投影。
--   - DB 兜底: 生成列 + UNIQUE 索引 (仿 V20260531_3 membership_lock_key):
--       place_belongs_lock_key     每场所至多 1 条活跃 belongs_to  (maxPerSubject=1)
--       place_responsible_lock_key 每场所至多 1 个活跃 responsible (maxPerResource=1)
--
-- 顺序敏感: 关系生成(读旧列"覆盖点"语义)必须在列改名之前。
-- 幂等: 锁键/索引/改名/删列均 information_schema 条件化; 关系生成条件化在
--       "org_unit_id 列仍存在"内(重跑时列已改名→整段跳过), INSERT IGNORE 靠
--       锁键唯一索引双重兜底; 回填 CTE 是不动点(已填满后重算结果不变)。
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. DB 兜底锁键 (先于关系生成, 给 INSERT IGNORE 提供唯一键)
-- ---------------------------------------------------------------------------
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'access_relations' AND COLUMN_NAME = 'place_belongs_lock_key');
SET @s := IF(@col = 0,
    "ALTER TABLE access_relations ADD COLUMN place_belongs_lock_key BIGINT GENERATED ALWAYS AS (CASE WHEN relation='belongs_to' AND subject_type='place' AND resource_type='org_unit' AND deleted=0 THEN subject_id ELSE NULL END) STORED COMMENT '场所归属唯一约束键(活跃 place->org belongs_to 取 subject_id)'",
    "SELECT 'place_belongs_lock_key exists' AS msg");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @idx := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'access_relations' AND INDEX_NAME = 'uk_place_belongs_unique');
SET @s := IF(@idx = 0,
    "ALTER TABLE access_relations ADD UNIQUE INDEX uk_place_belongs_unique (place_belongs_lock_key)",
    "SELECT 'uk_place_belongs_unique exists' AS msg");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'access_relations' AND COLUMN_NAME = 'place_responsible_lock_key');
SET @s := IF(@col = 0,
    "ALTER TABLE access_relations ADD COLUMN place_responsible_lock_key BIGINT GENERATED ALWAYS AS (CASE WHEN relation='responsible_for' AND subject_type='user' AND resource_type='place' AND deleted=0 THEN resource_id ELSE NULL END) STORED COMMENT '场所责任人唯一约束键(活跃 user->place responsible_for 取 resource_id)'",
    "SELECT 'place_responsible_lock_key exists' AS msg");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @idx := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'access_relations' AND INDEX_NAME = 'uk_place_responsible_unique');
SET @s := IF(@idx = 0,
    "ALTER TABLE access_relations ADD UNIQUE INDEX uk_place_responsible_unique (place_responsible_lock_key)",
    "SELECT 'uk_place_responsible_unique exists' AS msg");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- ---------------------------------------------------------------------------
-- 2. 关系生成 — 把旧列的显式覆盖点搬进 access_relations
--    (条件化在 org_unit_id 列仍存在内; 重跑时列已改名 → 自然跳过)
-- ---------------------------------------------------------------------------
SET @old_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'places' AND COLUMN_NAME = 'org_unit_id');

SET @s := IF(@old_col = 1,
    "INSERT IGNORE INTO access_relations
        (resource_type, resource_id, relation, subject_type, subject_id,
         access_level, remark, created_by, tenant_id)
     SELECT 'org_unit', p.org_unit_id, 'belongs_to', 'place', p.id,
            'FULL', 'V20260612_2: migrated from places.org_unit_id', 1, p.tenant_id
     FROM places p WHERE p.deleted = 0 AND p.org_unit_id IS NOT NULL",
    "SELECT 'belongs_to migration skipped (column already renamed)' AS msg");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @resp_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'places' AND COLUMN_NAME = 'responsible_user_id');

SET @s := IF(@resp_col = 1,
    "INSERT IGNORE INTO access_relations
        (resource_type, resource_id, relation, subject_type, subject_id,
         access_level, remark, created_by, tenant_id)
     SELECT 'place', p.id, 'responsible_for', 'user', p.responsible_user_id,
            'FULL', 'V20260612_2: migrated from places.responsible_user_id', 1, p.tenant_id
     FROM places p WHERE p.deleted = 0 AND p.responsible_user_id IS NOT NULL",
    "SELECT 'responsible_for migration skipped (column already dropped)' AS msg");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- ---------------------------------------------------------------------------
-- 3. 列改名: org_unit_id → effective_org_unit_id (语义: 投影列, 业务禁写)
-- ---------------------------------------------------------------------------
SET @s := IF(@old_col = 1,
    "ALTER TABLE places RENAME COLUMN org_unit_id TO effective_org_unit_id",
    "SELECT 'effective_org_unit_id already renamed' AS msg");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s := IF(@old_col = 1,
    "ALTER TABLE places MODIFY COLUMN effective_org_unit_id BIGINT DEFAULT NULL COMMENT '有效组织ID投影列(解析后含继承; 真相=belongs_to 关系; PlaceOrgProjector 维护, 业务禁直写)'",
    "SELECT 'effective_org_unit_id comment kept' AS msg");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- ---------------------------------------------------------------------------
-- 4. 投影回填 — 递归 CTE 不动点 (首跑: 旧值=覆盖点, 子取 COALESCE(自身覆盖, 父有效);
--    重跑: 列已是有效值, 重算结果不变)。deleted=1 孤儿节点不参与, 接受陈旧值。
--    经临时表中转, 规避 MySQL "递归 CTE 引用 UPDATE 目标表" 限制。
-- ---------------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_place_effective_org;
CREATE TEMPORARY TABLE tmp_place_effective_org AS
WITH RECURSIVE eff AS (
    SELECT id, effective_org_unit_id AS v
    FROM places WHERE parent_id IS NULL AND deleted = 0
    UNION ALL
    SELECT c.id, COALESCE(c.effective_org_unit_id, eff.v)
    FROM places c JOIN eff ON c.parent_id = eff.id
    WHERE c.deleted = 0
)
SELECT id, v FROM eff;

UPDATE places p
JOIN tmp_place_effective_org e ON p.id = e.id
SET p.effective_org_unit_id = e.v
WHERE NOT (p.effective_org_unit_id <=> e.v);

DROP TEMPORARY TABLE IF EXISTS tmp_place_effective_org;

-- ---------------------------------------------------------------------------
-- 5. 删除 responsible_user_id 列 (真相已在 responsible_for 关系)
-- ---------------------------------------------------------------------------
SET @s := IF(@resp_col = 1,
    "ALTER TABLE places DROP COLUMN responsible_user_id",
    "SELECT 'responsible_user_id already dropped' AS msg");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- ---------------------------------------------------------------------------
-- 6. 删除 V23 时代的列变更审计触发器 (引用已改名的 NEW.org_unit_id, 不删则
--    任何 places UPDATE 直接 SQLSyntaxError; 归属审计已由 PlaceEventHandler
--    监听 PlaceOrgAssignedEvent 承接, 且触发器会把投影器机械重算记成业务审计)
-- ---------------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_cascade_org_unit_update;

-- ---------------------------------------------------------------------------
-- 7. 删除 V23 时代引用旧列的 DB 对象 (代码零消费, 不删则对象失效/阻塞建库):
--    两个调试视图 + 递归函数 + 受影响子孙过程 — 语义已由投影列/PlaceOrgProjector 取代
-- ---------------------------------------------------------------------------
DROP VIEW IF EXISTS v_inheritance_tree;
DROP VIEW IF EXISTS v_places_effective_org;
DROP FUNCTION IF EXISTS get_effective_org_unit_id;
DROP PROCEDURE IF EXISTS get_affected_children;

-- ---------------------------------------------------------------------------
-- 验收 (人工执行, 应全部为 0 / 相等):
--
-- a) 不动点校验 — 每个非根场所: COALESCE(自身 belongs_to 覆盖, 父 effective) == 自身 effective
--    SELECT COUNT(*) FROM places c
--    JOIN places p ON c.parent_id = p.id
--    LEFT JOIN access_relations ar ON ar.relation='belongs_to' AND ar.subject_type='place'
--         AND ar.subject_id=c.id AND ar.deleted=0
--    WHERE c.deleted=0 AND NOT (COALESCE(ar.resource_id, p.effective_org_unit_id) <=> c.effective_org_unit_id);
--
-- b) 覆盖点守恒 — 迁移生成的 belongs_to 行数 == 迁移前显式 org_unit_id 数
--    SELECT COUNT(*) FROM access_relations
--    WHERE relation='belongs_to' AND subject_type='place' AND deleted=0;
--
-- c) 唯一键真实拒绝 — 给已有 belongs_to 的场所再插一条活跃 belongs_to 应报
--    ERROR 1062 Duplicate entry ... uk_place_belongs_unique
-- ============================================================================
