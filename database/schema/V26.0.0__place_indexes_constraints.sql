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
-- 本脚本使用 DROP IF EXISTS + ADD 模式，既能规范索引名又安全幂等。
-- =====================================================

-- =====================================================
-- 1. places 表
-- =====================================================

-- 1.1 唯一约束：同一父节点下不允许重复的 place_code
--     MySQL 对 NULL 的处理：每个 NULL parent_id 被视为不同值，
--     所以根节点的 place_code 唯一性由 uk_place_code 保证即可。
ALTER TABLE places DROP INDEX IF EXISTS uk_parent_place_code;
ALTER TABLE places ADD UNIQUE INDEX uk_parent_place_code (parent_id, place_code);

-- 1.2 path 前缀索引（LIKE '/1/2/%' 查询）
--     原索引 idx_path(path(255)) 已存在，缩短前缀长度以节省空间
ALTER TABLE places DROP INDEX IF EXISTS idx_path;
ALTER TABLE places DROP INDEX IF EXISTS idx_places_path;
ALTER TABLE places ADD INDEX idx_places_path (path(100));

-- 1.3 parent_id 索引（查询子节点）
--     原索引 idx_parent_id 已存在，规范命名
ALTER TABLE places DROP INDEX IF EXISTS idx_parent_id;
ALTER TABLE places DROP INDEX IF EXISTS idx_places_parent_id;
ALTER TABLE places ADD INDEX idx_places_parent_id (parent_id);

-- 1.4 type_code 索引
--     原索引 idx_type_code 已存在，规范命名
ALTER TABLE places DROP INDEX IF EXISTS idx_type_code;
ALTER TABLE places DROP INDEX IF EXISTS idx_places_type_code;
ALTER TABLE places ADD INDEX idx_places_type_code (type_code);

-- 1.5 org_unit_id 索引
--     原索引 idx_org_unit 已存在，规范命名
ALTER TABLE places DROP INDEX IF EXISTS idx_org_unit;
ALTER TABLE places DROP INDEX IF EXISTS idx_places_org_unit_id;
ALTER TABLE places ADD INDEX idx_places_org_unit_id (org_unit_id);

-- 1.6 status 索引
--     原索引 idx_status 已存在，规范命名
ALTER TABLE places DROP INDEX IF EXISTS idx_status;
ALTER TABLE places DROP INDEX IF EXISTS idx_places_status;
ALTER TABLE places ADD INDEX idx_places_status (status);

-- 1.7 复合索引：按类型 + 状态过滤（新增）
ALTER TABLE places DROP INDEX IF EXISTS idx_places_type_status;
ALTER TABLE places ADD INDEX idx_places_type_status (type_code, status);

-- =====================================================
-- 2. org_units 表
-- =====================================================

-- 2.1 tree_path 前缀索引
--     可能已存在为 idx_tree_path(tree_path(255))，规范命名并缩短前缀
ALTER TABLE org_units DROP INDEX IF EXISTS idx_tree_path;
ALTER TABLE org_units DROP INDEX IF EXISTS idx_org_units_tree_path;
ALTER TABLE org_units ADD INDEX idx_org_units_tree_path (tree_path(100));

-- 2.2 parent_id + sort_order 复合索引（新增，加速按父节点查询并排序）
ALTER TABLE org_units DROP INDEX IF EXISTS idx_org_units_parent_sort;
ALTER TABLE org_units ADD INDEX idx_org_units_parent_sort (parent_id, sort_order);

-- 2.3 unit_type 索引
--     可能已存在为 idx_unit_type 或 idx_type，规范命名
ALTER TABLE org_units DROP INDEX IF EXISTS idx_type;
ALTER TABLE org_units DROP INDEX IF EXISTS idx_unit_type;
ALTER TABLE org_units DROP INDEX IF EXISTS idx_org_units_type;
ALTER TABLE org_units ADD INDEX idx_org_units_type (unit_type);

-- =====================================================
-- 3. place_occupants 表
-- =====================================================

-- 3.1 place_id + status 复合索引（新增，加速查询某场所的活跃占用者）
ALTER TABLE place_occupants DROP INDEX IF EXISTS idx_occupants_place_status;
ALTER TABLE place_occupants ADD INDEX idx_occupants_place_status (place_id, status);
