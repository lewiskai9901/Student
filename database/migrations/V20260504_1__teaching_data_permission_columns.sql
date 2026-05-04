-- ============================================================
-- Teaching 模块数据权限补列迁移 (2026-05-04)
--
-- 目标: 让以下 4 张表具备 @DataPermission 行级过滤所需的字段
--   semester_course_offerings  + org_unit_id
--   scheduling_constraints     + org_unit_id
--   class_course_assignments   + created_by
--   schedule_conflict_records  + org_unit_id, created_by
--
-- 全部条件化 ALTER, 可重复执行 (information_schema 检测)
-- ============================================================

-- -----------------------------------------------------------
-- 1. semester_course_offerings  + org_unit_id
-- -----------------------------------------------------------
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'semester_course_offerings'
       AND COLUMN_NAME = 'org_unit_id') = 0,
    'ALTER TABLE `semester_course_offerings` ADD COLUMN `org_unit_id` BIGINT NULL COMMENT ''归属组织单元ID (数据权限)'' AFTER `applicable_grade`',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'semester_course_offerings'
       AND INDEX_NAME = 'idx_org_unit') = 0,
    'ALTER TABLE `semester_course_offerings` ADD INDEX `idx_org_unit` (`org_unit_id`)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------
-- 2. scheduling_constraints  + org_unit_id
-- -----------------------------------------------------------
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'scheduling_constraints'
       AND COLUMN_NAME = 'org_unit_id') = 0,
    'ALTER TABLE `scheduling_constraints` ADD COLUMN `org_unit_id` BIGINT NULL COMMENT ''归属组织单元ID (NULL=全校)'' AFTER `target_name`',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'scheduling_constraints'
       AND INDEX_NAME = 'idx_org_unit') = 0,
    'ALTER TABLE `scheduling_constraints` ADD INDEX `idx_org_unit` (`org_unit_id`)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------
-- 3. class_course_assignments  + created_by
-- -----------------------------------------------------------
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'class_course_assignments'
       AND COLUMN_NAME = 'created_by') = 0,
    'ALTER TABLE `class_course_assignments` ADD COLUMN `created_by` BIGINT NULL COMMENT ''创建人ID'' AFTER `status`',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------
-- 4. schedule_conflict_records  + org_unit_id + created_by
-- -----------------------------------------------------------
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'schedule_conflict_records'
       AND COLUMN_NAME = 'org_unit_id') = 0,
    'ALTER TABLE `schedule_conflict_records` ADD COLUMN `org_unit_id` BIGINT NULL COMMENT ''冲突归属组织单元ID (从 entry_id_1 派生)'' AFTER `entry_id_2`',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'schedule_conflict_records'
       AND COLUMN_NAME = 'created_by') = 0,
    'ALTER TABLE `schedule_conflict_records` ADD COLUMN `created_by` BIGINT NULL COMMENT ''检测创建人ID'' AFTER `org_unit_id`',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'schedule_conflict_records'
       AND INDEX_NAME = 'idx_org_unit') = 0,
    'ALTER TABLE `schedule_conflict_records` ADD INDEX `idx_org_unit` (`org_unit_id`)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------
-- 5. 数据回填 (BACKFILL)
--    semester_course_offerings.org_unit_id 留 NULL = 全校级（默认）
--    scheduling_constraints.org_unit_id    留 NULL = 全校级（默认）
--    class_course_assignments.created_by   留 NULL（历史数据无创建人，新行由应用层写入）
--    schedule_conflict_records.org_unit_id 从 entry_id_1 派生
-- -----------------------------------------------------------
UPDATE schedule_conflict_records r
JOIN schedule_entries e ON e.id = r.entry_id_1
SET r.org_unit_id = e.org_unit_id,
    r.created_by  = e.created_by
WHERE r.org_unit_id IS NULL;
