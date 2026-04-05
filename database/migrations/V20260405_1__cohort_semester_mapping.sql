-- 年级-学期映射表: 建立年级与校历的显式关联
-- 回答: "某年级在某学期，处于培养方案的第几学期"
CREATE TABLE IF NOT EXISTS `cohort_semester_mapping` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `cohort_id` BIGINT NOT NULL COMMENT '年级ID (grades表)',
    `semester_id` BIGINT NOT NULL COMMENT '学期ID (semesters表)',
    `program_semester` TINYINT NOT NULL COMMENT '培养方案第几学期 (1-8)',
    `plan_id` BIGINT COMMENT '关联培养方案',
    `status` TINYINT DEFAULT 1 COMMENT '1=正常 0=跳过(如休学学期)',
    `remark` VARCHAR(200),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_cohort_semester` (`cohort_id`, `semester_id`),
    INDEX `idx_semester` (`semester_id`),
    INDEX `idx_plan` (`plan_id`),
    CONSTRAINT `fk_csm_semester` FOREIGN KEY (`semester_id`) REFERENCES `semesters`(`id`),
    CONSTRAINT `fk_csm_plan` FOREIGN KEY (`plan_id`) REFERENCES `curriculum_plans`(`id`) ON DELETE SET NULL
) COMMENT '年级-学期映射表';
