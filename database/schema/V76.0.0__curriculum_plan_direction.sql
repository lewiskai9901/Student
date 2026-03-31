-- Add major_direction_id to curriculum_plans
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'curriculum_plans' AND COLUMN_NAME = 'major_direction_id') = 0,
    'ALTER TABLE curriculum_plans ADD COLUMN major_direction_id BIGINT COMMENT ''专业方向ID（可选，更精确的绑定）'' AFTER major_id',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Add index
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'curriculum_plans' AND INDEX_NAME = 'idx_major_direction') = 0,
    'ALTER TABLE curriculum_plans ADD INDEX idx_major_direction (major_direction_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
