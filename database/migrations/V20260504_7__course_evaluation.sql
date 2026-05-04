-- ============================================================
-- 学生评教 (2026-05-04)
--
-- 三层结构:
--   course_evaluations    评教活动 (一个学期一次, 关联 task 范围)
--   evaluation_indicators 评教指标项 (打分维度: 教学态度/方法/内容...)
--   evaluation_responses  学生提交 (每个学生对每个 task 一份)
--
-- 状态:
--   evaluation.status:    0=草稿 1=进行中 2=已结束
--   response.status:      0=未提交 1=已提交
-- ============================================================

CREATE TABLE IF NOT EXISTS `course_evaluations` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`       BIGINT       NOT NULL DEFAULT 1,
    `evaluation_code` VARCHAR(50)  NOT NULL,
    `evaluation_name` VARCHAR(200) NOT NULL COMMENT '活动名称',
    `semester_id`     BIGINT       NOT NULL,
    `org_unit_id`     BIGINT       NULL     COMMENT '归属(空=全校)',
    `start_time`      DATETIME     NULL,
    `end_time`        DATETIME     NULL,
    `status`          TINYINT      NOT NULL DEFAULT 0 COMMENT '0草稿 1进行中 2已结束',
    `anonymous`       TINYINT      NOT NULL DEFAULT 1 COMMENT '是否匿名',
    `description`     VARCHAR(500) NULL,
    `created_by`      BIGINT       NULL,
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_eval_code` (`evaluation_code`),
    INDEX `idx_semester` (`semester_id`),
    INDEX `idx_org_unit` (`org_unit_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='评教活动';

CREATE TABLE IF NOT EXISTS `evaluation_indicators` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`     BIGINT       NOT NULL DEFAULT 1,
    `evaluation_id` BIGINT       NOT NULL,
    `indicator_name` VARCHAR(200) NOT NULL COMMENT '指标名(如: 教学态度)',
    `description`   VARCHAR(500) NULL,
    `weight`        INT          NOT NULL DEFAULT 100 COMMENT '权重(0-100)',
    `max_score`     INT          NOT NULL DEFAULT 5 COMMENT '满分(默认 5 分制)',
    `sort_order`    INT          NOT NULL DEFAULT 0,
    `required`      TINYINT      NOT NULL DEFAULT 1 COMMENT '是否必填',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_eval` (`evaluation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='评教指标项';

CREATE TABLE IF NOT EXISTS `evaluation_responses` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`      BIGINT       NOT NULL DEFAULT 1,
    `evaluation_id`  BIGINT       NOT NULL,
    `task_id`        BIGINT       NOT NULL COMMENT '被评教学任务',
    `teacher_id`     BIGINT       NOT NULL COMMENT '被评教师',
    `student_id`     BIGINT       NOT NULL COMMENT '提交学生',
    `org_unit_id`    BIGINT       NULL     COMMENT '学生班级(数据权限)',
    `total_score`    DECIMAL(5,2) NULL     COMMENT '加权总分',
    `scores_json`    JSON         NULL     COMMENT '各指标打分: [{indicatorId, score}]',
    `comment`        VARCHAR(1000) NULL    COMMENT '主观评语',
    `status`         TINYINT      NOT NULL DEFAULT 0 COMMENT '0未提交 1已提交',
    `submitted_at`   DATETIME     NULL,
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`        TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_eval_task_student` (`evaluation_id`, `task_id`, `student_id`),
    INDEX `idx_teacher` (`teacher_id`),
    INDEX `idx_student` (`student_id`),
    INDEX `idx_org_unit` (`org_unit_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='评教提交';
