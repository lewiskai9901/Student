-- V20260531_1 access_relations 提升 is_primary 为实体列
-- 背景: 后续给 access_relations 建"每用户唯一归属"的生成列 UNIQUE 索引。
-- 直接对 metadata JSON 路径建 STORED 生成列在 MySQL 上不稳, 故先把 isPrimary 提升为实体列。
-- 条件化幂等 (information_schema): 可重复执行。

-- 1. 加列 is_primary (若不存在)
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'access_relations'
      AND COLUMN_NAME = 'is_primary'
);
SET @sql := IF(@col_exists = 0,
    "ALTER TABLE access_relations ADD COLUMN is_primary TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否主归属'",
    "SELECT 'is_primary column already exists' AS msg");
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. 从现有 metadata.isPrimary 回填 (V25 迁移用 CAST(... AS JSON) 写入 JSON true/false;
--    应用层 java.util.Map.of("isPrimary", true) 经 Jackson 也写 JSON boolean。
--    JSON_EXTRACT 取出后用 = true 匹配 JSON 布尔真, 同时容错字符串/数字形态。)
UPDATE access_relations
SET is_primary = 1
WHERE deleted = 0
  AND JSON_EXTRACT(metadata, '$.isPrimary') IN (CAST('true' AS JSON), CAST('1' AS JSON), CAST(1 AS JSON), CAST('"true"' AS JSON));
