CREATE TABLE IF NOT EXISTS `grade_weight_configs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `semester_id` BIGINT NOT NULL,
    `course_id` BIGINT NOT NULL,
    `component_type` TINYINT NOT NULL COMMENT '1=平时 2=期中 3=期末',
    `weight_percent` INT NOT NULL COMMENT '权重百分比，如 20 表示 20%',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_semester_course_component` (`semester_id`, `course_id`, `component_type`),
    INDEX `idx_semester_course` (`semester_id`, `course_id`)
) COMMENT '成绩加权配置表';
