-- V20260531_2 relation_types 加 max_per_subject 列
-- 背景: cardinality 元数据此前只有 max_per_resource (每资源上限),
--   缺"每主体上限"。member 关系 maxPerSubject=1 表示"每用户唯一归属"。
--   forceGrant 据此真正强制 cardinality。
-- 条件化幂等 (information_schema): 可重复执行。

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'relation_types'
      AND COLUMN_NAME = 'max_per_subject'
);
SET @sql := IF(@col_exists = 0,
    "ALTER TABLE relation_types ADD COLUMN max_per_subject INT NULL COMMENT '每个 subject 最多持有该 relation 的 resource 数 (null=无限, 如 member=1 每用户唯一归属)' AFTER max_by_subtype",
    "SELECT 'max_per_subject column already exists' AS msg");
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
