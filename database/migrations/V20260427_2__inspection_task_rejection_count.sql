-- P1#5: 任务驳回上限 + deadline 延期
-- 给 insp_tasks 加 rejection_count + extended_to 字段, 防止驳回-重提循环, 同时记录延期

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'insp_tasks'
      AND COLUMN_NAME = 'rejection_count'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE insp_tasks ADD COLUMN rejection_count INT NOT NULL DEFAULT 0 COMMENT ''驳回次数, 超过上限须管理员介入''',
    'SELECT ''rejection_count column already exists'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'insp_tasks'
      AND COLUMN_NAME = 'extended_to'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE insp_tasks ADD COLUMN extended_to DATE NULL COMMENT ''驳回延期到的有效日期, 为空表示无延期''',
    'SELECT ''extended_to column already exists'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
