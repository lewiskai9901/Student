-- P5-4e: 统一类型系统收尾 — 删除 user_types / place_types
--
-- 前置条件：V20260417_1 扩展 entity_type_configs 表结构、V20260417_2 将遗留数据
-- 回填到 entity_type_configs 已成功执行，后端已切到 EntityTypeConfigRepository。
--
-- 此迁移为最终清理：drop 两张并行类型表及其索引，entity_type_configs 成为唯一权威。

DROP TABLE IF EXISTS user_types;
DROP TABLE IF EXISTS place_types;
