-- V20260531_4 删除三个冗余归属列 — 归属唯一真相源 = access_relations member 关系
--
-- 背景: 组织归属 (用户属于哪个组织) 已统一表达为 access_relations 的
--   {member | user | org_unit} 关系 (每用户唯一, V20260531_3 有 UNIQUE 约束兜底)。
--   所有读/写路径已切走, 这三个旧的直连归属列彻底退役:
--     users.primary_org_unit_id   (+ idx_users_primary_org_unit)
--     user_student.org_unit_id    (+ idx_org_unit_id)
--     user_teacher.org_unit_id    (+ idx_org_unit)
--
-- 条件化幂等 (information_schema): 列/索引存在才删, 可重复执行。
-- 已升级库 (有列) → 真删; fresh-init (baseline 已不含列) → no-op。

-- ============================================================
-- 1. users.primary_org_unit_id (先删索引再删列)
-- ============================================================
SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND INDEX_NAME = 'idx_users_primary_org_unit'
);
SET @sql := IF(@idx_exists > 0,
    "ALTER TABLE users DROP INDEX idx_users_primary_org_unit",
    "SELECT 'idx_users_primary_org_unit not present' AS msg");
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'primary_org_unit_id'
);
SET @sql := IF(@col_exists > 0,
    "ALTER TABLE users DROP COLUMN primary_org_unit_id",
    "SELECT 'users.primary_org_unit_id already dropped' AS msg");
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- 2. user_student.org_unit_id (先删索引再删列)
--    索引名历史上是 idx_class_id (class_id→org_unit_id 改名时索引名未变),
--    动态按列名定位索引名后删除, 兼容 idx_class_id / idx_org_unit_id 两种命名。
-- ============================================================
SET @idx_name := (
    SELECT INDEX_NAME FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user_student'
      AND COLUMN_NAME = 'org_unit_id'
    LIMIT 1
);
SET @sql := IF(@idx_name IS NOT NULL,
    CONCAT("ALTER TABLE user_student DROP INDEX ", @idx_name),
    "SELECT 'user_student org_unit_id index not present' AS msg");
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user_student'
      AND COLUMN_NAME = 'org_unit_id'
);
SET @sql := IF(@col_exists > 0,
    "ALTER TABLE user_student DROP COLUMN org_unit_id",
    "SELECT 'user_student.org_unit_id already dropped' AS msg");
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- 3. user_teacher.org_unit_id (先删索引再删列)
-- ============================================================
SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user_teacher'
      AND INDEX_NAME = 'idx_org_unit'
);
SET @sql := IF(@idx_exists > 0,
    "ALTER TABLE user_teacher DROP INDEX idx_org_unit",
    "SELECT 'user_teacher.idx_org_unit not present' AS msg");
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user_teacher'
      AND COLUMN_NAME = 'org_unit_id'
);
SET @sql := IF(@col_exists > 0,
    "ALTER TABLE user_teacher DROP COLUMN org_unit_id",
    "SELECT 'user_teacher.org_unit_id already dropped' AS msg");
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
