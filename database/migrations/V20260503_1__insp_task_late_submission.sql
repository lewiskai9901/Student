-- ============================================================
-- P2: 检查任务延迟交付标记
-- 字段: late_submission (是否延迟) + late_days (延迟天数, 0=按时)
-- 写入时机: InspTask.submit() 时计算 (today vs effective_due_date)
-- 用途: 审核侧筛选 / 数据分析延迟率
-- ============================================================

-- late_submission flag
SELECT IF(EXISTS(
  SELECT 1 FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'insp_tasks' AND COLUMN_NAME = 'late_submission'
), 'SELECT 1', 'ALTER TABLE insp_tasks ADD COLUMN late_submission TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''是否延迟提交 (提交时 today > effective_due_date 即为 1)'' AFTER submitted_at') INTO @s;
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- late_days int
SELECT IF(EXISTS(
  SELECT 1 FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'insp_tasks' AND COLUMN_NAME = 'late_days'
), 'SELECT 1', 'ALTER TABLE insp_tasks ADD COLUMN late_days INT NOT NULL DEFAULT 0 COMMENT ''延迟天数 (0=按时, >0=延迟)'' AFTER late_submission') INTO @s;
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- index on late_submission for review-side filter
SELECT IF(EXISTS(
  SELECT 1 FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'insp_tasks' AND INDEX_NAME = 'idx_insp_tasks_late_submission'
), 'SELECT 1', 'ALTER TABLE insp_tasks ADD INDEX idx_insp_tasks_late_submission (late_submission, status)') INTO @s;
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
