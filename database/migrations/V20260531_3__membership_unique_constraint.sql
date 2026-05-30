-- V20260531_3 归属唯一约束 — 每用户至多一条 member 归属
-- 背景: 组织归属统一为 access_relations 的 member|user|org_unit 关系, 要求每用户唯一。
-- 方案: 生成列(member+user+org_unit+未删时取 subject_id, 否则 NULL)+ UNIQUE 索引。
--       NULL 不参与 UNIQUE, 故只约束"活跃的 user→org_unit member"唯一; 软删行(deleted=1)自动置 NULL 退出约束,
--       支持 revoke(软删)后重新 setMembership。参考 inspection_appeals.pending_lock_key 同模式。
-- 条件化幂等。前置: 实测库中无用户拥有多条活跃 member(已校验), 加约束安全。

-- 0. 先修历史 flag: member 既已每用户唯一, 其唯一归属即主归属 → is_primary 统一为 1
UPDATE access_relations
SET is_primary = 1
WHERE relation = 'member' AND subject_type = 'user' AND resource_type = 'org_unit'
  AND deleted = 0 AND is_primary = 0;

-- 1. 生成列 membership_lock_key
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'access_relations' AND COLUMN_NAME = 'membership_lock_key');
SET @s := IF(@col = 0,
    "ALTER TABLE access_relations ADD COLUMN membership_lock_key BIGINT GENERATED ALWAYS AS (CASE WHEN relation='member' AND subject_type='user' AND resource_type='org_unit' AND deleted=0 THEN subject_id ELSE NULL END) STORED COMMENT '归属唯一约束键(活跃 user->org member 取 subject_id)'",
    "SELECT 'membership_lock_key exists' AS msg");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 2. UNIQUE 索引
SET @idx := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'access_relations' AND INDEX_NAME = 'uk_membership_unique');
SET @s := IF(@idx = 0,
    "ALTER TABLE access_relations ADD UNIQUE INDEX uk_membership_unique (membership_lock_key)",
    "SELECT 'uk_membership_unique exists' AS msg");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
