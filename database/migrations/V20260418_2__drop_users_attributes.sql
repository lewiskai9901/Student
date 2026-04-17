-- P5-5: 清理 users.attributes 孤列
--
-- users.attributes 由 V20260407_4 添加，但从未接入 User 聚合 / UserPO / UserMapper。
-- 代码检索确认无任何读写路径，drop 不会影响任何功能。
-- 动态属性应使用 entity_type_configs.metadata_schema + 专用扩展表，而非 users 表裸 JSON 列。

SET @col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'users'
              AND COLUMN_NAME = 'attributes');
SET @s = IF(@col > 0, 'ALTER TABLE users DROP COLUMN attributes', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
