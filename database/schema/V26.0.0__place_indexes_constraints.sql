-- =====================================================
-- V26.0.0: Place 表索引和约束优化
-- Date: 2026-04-11
-- Description:
--   1. 添加 places 表的唯一约束（同一父节点下 place_code 唯一）
--   2. 添加/规范 places 表的性能索引
--   3. 添加 org_units 表的复合索引
--   4. 添加 place_occupants 表的复合索引
--
-- 注意：原 V9 创建 spaces 表时已建了部分索引（后经 rename_space_to_place 重命名）。
-- MySQL 8.0 不支持 DROP/CREATE INDEX IF [NOT] EXISTS, 统一用 information_schema 条件化,
-- 既能规范索引名又安全幂等。
-- =====================================================

-- 条件化 DROP INDEX (索引存在才删) + 条件化 ADD INDEX (索引不存在才加)。
-- 通过反复 SET @drop / @add 变量 + PREPARE/EXECUTE 实现。

-- ---------- helper 约定 ----------
-- DROP idx: SET @x:=COUNT(STATISTICS where INDEX_NAME); IF >0 ALTER DROP INDEX
-- ADD  idx: SET @x:=COUNT(STATISTICS where INDEX_NAME); IF =0 ALTER ADD INDEX

-- =====================================================
-- 1. places 表
-- =====================================================

-- 1.1 唯一约束：同一父节点下不允许重复的 place_code
SET @x := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='places' AND INDEX_NAME='uk_parent_place_code');
SET @s := IF(@x>0, "ALTER TABLE places DROP INDEX uk_parent_place_code", "SELECT 1");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
ALTER TABLE places ADD UNIQUE INDEX uk_parent_place_code (parent_id, place_code);

-- 1.2 path 前缀索引
SET @x := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='places' AND INDEX_NAME='idx_path');
SET @s := IF(@x>0, "ALTER TABLE places DROP INDEX idx_path", "SELECT 1");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @x := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='places' AND INDEX_NAME='idx_places_path');
SET @s := IF(@x>0, "ALTER TABLE places DROP INDEX idx_places_path", "SELECT 1");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
ALTER TABLE places ADD INDEX idx_places_path (path(100));

-- 1.3 parent_id 索引
SET @x := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='places' AND INDEX_NAME='idx_parent_id');
SET @s := IF(@x>0, "ALTER TABLE places DROP INDEX idx_parent_id", "SELECT 1");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @x := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='places' AND INDEX_NAME='idx_places_parent_id');
SET @s := IF(@x>0, "ALTER TABLE places DROP INDEX idx_places_parent_id", "SELECT 1");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
ALTER TABLE places ADD INDEX idx_places_parent_id (parent_id);

-- 1.4 type_code 索引
SET @x := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='places' AND INDEX_NAME='idx_type_code');
SET @s := IF(@x>0, "ALTER TABLE places DROP INDEX idx_type_code", "SELECT 1");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @x := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='places' AND INDEX_NAME='idx_places_type_code');
SET @s := IF(@x>0, "ALTER TABLE places DROP INDEX idx_places_type_code", "SELECT 1");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
ALTER TABLE places ADD INDEX idx_places_type_code (type_code);

-- 1.5 org_unit_id 索引
SET @x := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='places' AND INDEX_NAME='idx_org_unit');
SET @s := IF(@x>0, "ALTER TABLE places DROP INDEX idx_org_unit", "SELECT 1");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @x := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='places' AND INDEX_NAME='idx_places_org_unit_id');
SET @s := IF(@x>0, "ALTER TABLE places DROP INDEX idx_places_org_unit_id", "SELECT 1");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
ALTER TABLE places ADD INDEX idx_places_org_unit_id (org_unit_id);

-- 1.6 status 索引
SET @x := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='places' AND INDEX_NAME='idx_status');
SET @s := IF(@x>0, "ALTER TABLE places DROP INDEX idx_status", "SELECT 1");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @x := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='places' AND INDEX_NAME='idx_places_status');
SET @s := IF(@x>0, "ALTER TABLE places DROP INDEX idx_places_status", "SELECT 1");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
ALTER TABLE places ADD INDEX idx_places_status (status);

-- 1.7 复合索引：按类型 + 状态过滤
SET @x := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='places' AND INDEX_NAME='idx_places_type_status');
SET @s := IF(@x>0, "ALTER TABLE places DROP INDEX idx_places_type_status", "SELECT 1");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
ALTER TABLE places ADD INDEX idx_places_type_status (type_code, status);

-- =====================================================
-- 2. org_units 表
-- =====================================================

-- 2.1 tree_path 前缀索引
SET @x := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='org_units' AND INDEX_NAME='idx_tree_path');
SET @s := IF(@x>0, "ALTER TABLE org_units DROP INDEX idx_tree_path", "SELECT 1");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @x := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='org_units' AND INDEX_NAME='idx_org_units_tree_path');
SET @s := IF(@x>0, "ALTER TABLE org_units DROP INDEX idx_org_units_tree_path", "SELECT 1");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
ALTER TABLE org_units ADD INDEX idx_org_units_tree_path (tree_path(100));

-- 2.2 parent_id + sort_order 复合索引
SET @x := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='org_units' AND INDEX_NAME='idx_org_units_parent_sort');
SET @s := IF(@x>0, "ALTER TABLE org_units DROP INDEX idx_org_units_parent_sort", "SELECT 1");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
ALTER TABLE org_units ADD INDEX idx_org_units_parent_sort (parent_id, sort_order);

-- 2.3 unit_type 索引
SET @x := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='org_units' AND INDEX_NAME='idx_type');
SET @s := IF(@x>0, "ALTER TABLE org_units DROP INDEX idx_type", "SELECT 1");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @x := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='org_units' AND INDEX_NAME='idx_unit_type');
SET @s := IF(@x>0, "ALTER TABLE org_units DROP INDEX idx_unit_type", "SELECT 1");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @x := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='org_units' AND INDEX_NAME='idx_org_units_type');
SET @s := IF(@x>0, "ALTER TABLE org_units DROP INDEX idx_org_units_type", "SELECT 1");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
ALTER TABLE org_units ADD INDEX idx_org_units_type (unit_type);

-- =====================================================
-- 3. place_occupants 表
-- =====================================================

-- 3.1 place_id + status 复合索引
SET @x := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='place_occupants' AND INDEX_NAME='idx_occupants_place_status');
SET @s := IF(@x>0, "ALTER TABLE place_occupants DROP INDEX idx_occupants_place_status", "SELECT 1");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
ALTER TABLE place_occupants ADD INDEX idx_occupants_place_status (place_id, status);
