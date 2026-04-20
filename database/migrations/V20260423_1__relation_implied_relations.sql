-- ============================================================
-- W4.2 — 关系类型 implied_relations 列
--
-- 为 relation_types 表新增 JSON 列, 存储关系链推导规则:
--   [ {"targetType":"user", "relation":"viewer", "discoveryRule":"OCCUPANTS_OF_PLACE"}, ... ]
-- 由 RelationTypePluginRegistrar 按 plugin 声明写入, AuthorizationService
-- 在 check() 调用时展开推导。
-- ============================================================

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name   = 'relation_types'
      AND column_name  = 'implied_relations'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE relation_types ADD COLUMN implied_relations JSON NULL COMMENT ''关系链推导: List<Implied{targetType,relation,discoveryRule}>''',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
