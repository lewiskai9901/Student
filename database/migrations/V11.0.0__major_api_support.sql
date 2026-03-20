-- ================================================================
-- V11.0.0 - 对齐 major_directions 表与 PO 字段
-- 确保 level, years, is_segmented, phase1/2 列存在
-- grade_major_directions 已由 002 迁移创建，无需重复
-- ================================================================

-- 添加 level 列（层次：中级工/高级工/预备技师/技师）
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
   AND TABLE_NAME = 'major_directions'
   AND COLUMN_NAME = 'level') = 0,
  'ALTER TABLE major_directions ADD COLUMN `level` VARCHAR(20) COMMENT ''培养层次'' AFTER direction_code',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 years 列（学制年数）
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
   AND TABLE_NAME = 'major_directions'
   AND COLUMN_NAME = 'years') = 0,
  'ALTER TABLE major_directions ADD COLUMN `years` INT COMMENT ''学制年数'' AFTER `level`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 is_segmented 列（是否分段培养）
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
   AND TABLE_NAME = 'major_directions'
   AND COLUMN_NAME = 'is_segmented') = 0,
  'ALTER TABLE major_directions ADD COLUMN is_segmented TINYINT DEFAULT 0 COMMENT ''是否分段培养'' AFTER `years`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 phase1_level 列
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
   AND TABLE_NAME = 'major_directions'
   AND COLUMN_NAME = 'phase1_level') = 0,
  'ALTER TABLE major_directions ADD COLUMN phase1_level VARCHAR(20) COMMENT ''第一阶段层次'' AFTER is_segmented',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 phase1_years 列
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
   AND TABLE_NAME = 'major_directions'
   AND COLUMN_NAME = 'phase1_years') = 0,
  'ALTER TABLE major_directions ADD COLUMN phase1_years INT COMMENT ''第一阶段年数'' AFTER phase1_level',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 phase2_level 列
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
   AND TABLE_NAME = 'major_directions'
   AND COLUMN_NAME = 'phase2_level') = 0,
  'ALTER TABLE major_directions ADD COLUMN phase2_level VARCHAR(20) COMMENT ''第二阶段层次'' AFTER phase1_years',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 phase2_years 列
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
   AND TABLE_NAME = 'major_directions'
   AND COLUMN_NAME = 'phase2_years') = 0,
  'ALTER TABLE major_directions ADD COLUMN phase2_years INT COMMENT ''第二阶段年数'' AFTER phase2_level',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'V11.0.0 migration completed - major_directions columns aligned with PO' AS status;
