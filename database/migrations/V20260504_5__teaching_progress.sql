-- ============================================================
-- 教学进度跟踪 (2026-05-04)
--
-- 记录每个 teaching_task 每节课的实际授课进度:
--   - 计划: 第几周第几节课讲哪一章/节
--   - 实际: 是否完成, 实际讲了什么内容
--   - 偏差: 进度落后/超前
--
-- 关联:
--   teaching_tasks (task_id) → 课程 + 班级 + 教师 信息从这里来
--   schedule_entries (entry_id, optional) → 关联具体某节排课
--
-- 状态: 0=待授课 1=已完成 2=未完成 3=调课
-- ============================================================

CREATE TABLE IF NOT EXISTS `teaching_progress` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`       BIGINT       NOT NULL DEFAULT 1,
    `task_id`         BIGINT       NOT NULL COMMENT '教学任务ID',
    `entry_id`        BIGINT       NULL     COMMENT '排课条目ID(可选)',
    `semester_id`     BIGINT       NOT NULL COMMENT '学期ID',
    `org_unit_id`     BIGINT       NULL     COMMENT '归属组织(数据权限)',
    `week_number`     INT          NOT NULL COMMENT '教学周次',
    `lesson_no`       INT          NOT NULL DEFAULT 1 COMMENT '本周第几节(从1起)',
    `planned_topic`   VARCHAR(500) NULL     COMMENT '计划讲授章节/主题',
    `actual_topic`    VARCHAR(500) NULL     COMMENT '实际讲授章节/主题',
    `chapter`         VARCHAR(100) NULL     COMMENT '章节编号',
    `progress_status` TINYINT      NOT NULL DEFAULT 0 COMMENT '0待授课 1已完成 2未完成 3调课',
    `attendance_count` INT         NULL     COMMENT '实到人数',
    `total_students`  INT          NULL     COMMENT '应到人数',
    `note`            VARCHAR(500) NULL     COMMENT '备注/学情记录',
    `recorded_at`     DATETIME     NULL     COMMENT '授课实际记录时间',
    `recorded_by`     BIGINT       NULL     COMMENT '记录人(教师)',
    `created_by`      BIGINT       NULL,
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_week_lesson` (`task_id`, `week_number`, `lesson_no`),
    INDEX `idx_semester_org` (`semester_id`, `org_unit_id`),
    INDEX `idx_task` (`task_id`),
    INDEX `idx_recorded_by` (`recorded_by`),
    INDEX `idx_status` (`progress_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='教学进度跟踪表';
