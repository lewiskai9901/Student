-- ============================================================================
-- V20260613_3: uk_relation 改为"仅约束活跃行" (修 revoke-regrant-revoke 撞键)
--
-- 背景: 旧 uk_relation = (resource_type, resource_id, relation, subject_type,
-- subject_id, deleted) — deleted 是 0/1 二值, 意味着**同一 tuple 至多一条归档行**:
-- 同一关系 grant → revoke(软删) → grant → 再 revoke 时, 第二次软删 UPDATE deleted=1
-- 直接 Duplicate entry (uk_relation)。任何关系类型都中招 (member 换绑两次/场所归属
-- 清除两次/关系管理页反复解绑)。2026-06-13 场所归属关系化 E2E (deletePlace 级联
-- 清理) 钓出此既有缺陷。
--
-- 修法: 加生成列 active_uniq = CASE WHEN deleted=0 THEN 1 ELSE NULL END,
-- uk_relation 改为 (...tuple..., active_uniq)。MySQL 复合唯一索引中任一列为 NULL
-- 的行豁免唯一检查 → 活跃 tuple 仍严格唯一, 归档行可无限堆叠。
--
-- 幂等: information_schema 条件化。baseline_v3.sql 已同步。
-- ============================================================================

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'access_relations' AND COLUMN_NAME = 'active_uniq');
SET @s := IF(@col = 0,
    "ALTER TABLE access_relations ADD COLUMN active_uniq TINYINT GENERATED ALWAYS AS (CASE WHEN deleted = 0 THEN 1 ELSE NULL END) STORED COMMENT '活跃行唯一性豁免列(归档行 NULL 不参与 uk_relation)'",
    "SELECT 'active_uniq exists' AS msg");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 重建 uk_relation: 先删旧 (含 deleted 的) 再建新 (含 active_uniq 的)
SET @old := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'access_relations'
      AND INDEX_NAME = 'uk_relation' AND COLUMN_NAME = 'deleted');
SET @s := IF(@old > 0,
    "ALTER TABLE access_relations DROP INDEX uk_relation",
    "SELECT 'old uk_relation already dropped' AS msg");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @idx := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'access_relations' AND INDEX_NAME = 'uk_relation');
SET @s := IF(@idx = 0,
    "ALTER TABLE access_relations ADD UNIQUE INDEX uk_relation (resource_type, resource_id, relation, subject_type, subject_id, active_uniq)",
    "SELECT 'new uk_relation exists' AS msg");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
