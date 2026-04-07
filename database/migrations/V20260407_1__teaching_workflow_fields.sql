-- 补全教务工作流串联所需的字段

-- teaching_tasks: 添加 offering_id 和 teaching_class_id
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'teaching_tasks' AND COLUMN_NAME = 'offering_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE teaching_tasks ADD COLUMN offering_id BIGINT COMMENT ''关联开课计划ID'' AFTER class_id, ADD INDEX idx_offering (offering_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'teaching_tasks' AND COLUMN_NAME = 'teaching_class_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE teaching_tasks ADD COLUMN teaching_class_id BIGINT COMMENT ''关联教学班ID'' AFTER offering_id, ADD INDEX idx_teaching_class (teaching_class_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- exam_arrangements: 添加 task_id 和 class_id
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam_arrangements' AND COLUMN_NAME = 'task_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE exam_arrangements ADD COLUMN task_id BIGINT COMMENT ''关联教学任务ID'' AFTER course_id, ADD INDEX idx_task (task_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam_arrangements' AND COLUMN_NAME = 'class_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE exam_arrangements ADD COLUMN class_id BIGINT COMMENT ''关联班级ID'' AFTER task_id', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
