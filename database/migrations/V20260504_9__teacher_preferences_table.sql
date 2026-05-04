-- ============================================================
-- 补建 teacher_preferences 表 (2026-05-04)
-- 原计划在 V20260121_1__teaching_management_init.sql 里建, 但该文件未在
-- 当前库执行. 这里独立条件化创建, 已存在则跳过.
-- ============================================================
CREATE TABLE IF NOT EXISTS `teacher_preferences` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` BIGINT NOT NULL DEFAULT 1,
    `teacher_id` BIGINT NOT NULL COMMENT '教师ID',
    `semester_id` BIGINT NOT NULL COMMENT '学期ID',
    `preference_type` TINYINT NOT NULL COMMENT '1不可用时间 2偏好时间 3偏好教室',
    `weekday` TINYINT NULL COMMENT '周几(1-7)',
    `time_slot` INT NULL COMMENT '节次',
    `classroom_id` BIGINT NULL COMMENT '偏好教室ID',
    `priority` INT DEFAULT 0,
    `reason` VARCHAR(200) NULL,
    `status` TINYINT DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_teacher_semester` (`teacher_id`, `semester_id`),
    INDEX `idx_type` (`preference_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='教师排课偏好表';
