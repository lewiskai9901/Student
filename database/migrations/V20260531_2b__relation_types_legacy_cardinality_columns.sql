-- V20260531_2b relation_types 补齐遗留 cardinality 列(修 fresh-init 债)
-- 背景: max_per_resource / capacity_bound / max_by_subtype 三列被 RelationTypeUpserter 写入,
--       但仓库内无任何建表/ADD 迁移创建它们(历史 ad-hoc 加到线上库, git 无 DDL)。
--       → 全新库 init-all 时这三列不存在, RelationTypeUpserter 启动即报列不存在。
-- 本迁移条件化补齐, 与线上既有列兼容(已存在则跳过)。

-- max_per_resource
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'relation_types' AND COLUMN_NAME = 'max_per_resource');
SET @s := IF(@c = 0,
    "ALTER TABLE relation_types ADD COLUMN max_per_resource INT NULL COMMENT '每资源上限(如 admin=1)'",
    "SELECT 'max_per_resource exists' AS msg");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- capacity_bound
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'relation_types' AND COLUMN_NAME = 'capacity_bound');
SET @s := IF(@c = 0,
    "ALTER TABLE relation_types ADD COLUMN capacity_bound TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否受场所容量约束'",
    "SELECT 'capacity_bound exists' AS msg");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- max_by_subtype
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'relation_types' AND COLUMN_NAME = 'max_by_subtype');
SET @s := IF(@c = 0,
    "ALTER TABLE relation_types ADD COLUMN max_by_subtype JSON NULL COMMENT '按子类型的每资源上限覆盖(如 teaches CLASS:10)'",
    "SELECT 'max_by_subtype exists' AS msg");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
