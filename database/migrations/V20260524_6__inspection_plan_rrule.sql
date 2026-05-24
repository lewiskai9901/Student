-- ============================================================
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
-- V20260524_6: 调度组 RRULE 周期支持
--
-- 支持复杂周期模式 (e.g. "每月第 2 个周五") 而无需创建多个 plan.
-- 非空时 scheduler 优先用 rrule 计算 next occurrence, 忽略 cycleType/frequency/scheduleDays.
-- 兼容: rrule 留空 = 走旧 cycleType 逻辑.
-- ============================================================

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'insp_inspection_plans'
      AND column_name = 'rrule');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE insp_inspection_plans ADD COLUMN rrule VARCHAR(255) NULL
        COMMENT ''RRULE 周期表达式 (RFC 5545 子集); 非空时优先于 cycleType/frequency/scheduleDays. 支持 FREQ=DAILY|WEEKLY|MONTHLY + BYDAY + INTERVAL.''',
    'SELECT ''[idempotent] rrule 已存在''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
