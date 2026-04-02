-- Add plan link fields to semester_course_offerings
-- When offerings are imported from a curriculum plan, these fields track the source

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'semester_course_offerings' AND COLUMN_NAME = 'plan_id') = 0,
    'ALTER TABLE semester_course_offerings ADD COLUMN plan_id BIGINT COMMENT ''来源培养方案ID（可选）'' AFTER semester_id',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'semester_course_offerings' AND COLUMN_NAME = 'plan_course_id') = 0,
    'ALTER TABLE semester_course_offerings ADD COLUMN plan_course_id BIGINT COMMENT ''来源方案课程ID（可选）'' AFTER plan_id',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
