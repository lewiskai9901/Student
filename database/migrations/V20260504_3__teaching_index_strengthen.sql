-- ============================================================
-- Teaching 模块索引补强 (2026-05-04)
--
-- 1. schedule_entries: 复合索引 (semester_id, weekday, start_slot)
--    替代原有 idx_time(weekday, start_slot) — 后者跨学期会全表扫
-- 2. teaching_tasks: 复合索引 (org_unit_id, semester_id)
--    支持按部门 + 学期常用组合查询
--
-- 全部条件化, 可重复执行
-- ============================================================

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'schedule_entries'
       AND INDEX_NAME = 'idx_semester_time') = 0,
    'ALTER TABLE `schedule_entries` ADD INDEX `idx_semester_time` (`semester_id`, `weekday`, `start_slot`)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'teaching_tasks'
       AND INDEX_NAME = 'idx_org_unit_semester') = 0,
    'ALTER TABLE `teaching_tasks` ADD INDEX `idx_org_unit_semester` (`org_unit_id`, `semester_id`)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
