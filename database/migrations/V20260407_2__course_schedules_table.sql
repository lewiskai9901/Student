-- 排课方案表
CREATE TABLE IF NOT EXISTS `course_schedules` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `semester_id` BIGINT NOT NULL,
    `name` VARCHAR(100) NOT NULL COMMENT '方案名称',
    `description` VARCHAR(500) COMMENT '方案说明',
    `status` TINYINT DEFAULT 0 COMMENT '0=草稿 1=已发布 2=已归档',
    `entry_count` INT DEFAULT 0 COMMENT '条目数',
    `generated_at` DATETIME COMMENT '自动排课时间',
    `published_at` DATETIME COMMENT '发布时间',
    `remark` VARCHAR(500),
    `created_by` BIGINT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_by` BIGINT,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    INDEX `idx_semester` (`semester_id`)
) COMMENT '排课方案表';

-- schedule_entries 增加 schedule_id
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'schedule_entries' AND COLUMN_NAME = 'schedule_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE schedule_entries ADD COLUMN schedule_id BIGINT COMMENT ''关联排课方案'' AFTER semester_id, ADD INDEX idx_schedule (schedule_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
