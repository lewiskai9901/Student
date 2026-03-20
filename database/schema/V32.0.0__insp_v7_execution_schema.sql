-- =====================================================
-- V7 检查平台 - 执行引擎 Schema (Phase 3)
-- 6 tables: projects, tasks, submissions, submission_details, evidences, project_inspectors
-- =====================================================

-- 1. 检查项目
CREATE TABLE IF NOT EXISTS `insp_projects` (
    `id`                  BIGINT         NOT NULL AUTO_INCREMENT,
    `tenant_id`           BIGINT         NOT NULL DEFAULT 0,
    `project_code`        VARCHAR(50)    NOT NULL,
    `project_name`        VARCHAR(200)   NOT NULL,
    `template_id`         BIGINT         NOT NULL,
    `template_version_id` BIGINT         NULL     COMMENT '锁定的模板版本',
    `scoring_profile_id`  BIGINT         NULL,
    `scope_type`          VARCHAR(20)    NOT NULL DEFAULT 'ORG' COMMENT 'ORG|PLACE|USER|CUSTOM',
    `scope_config`        JSON           NULL     COMMENT '范围配置(组织/场所/用户ID列表)',
    `target_type`         VARCHAR(20)    NOT NULL DEFAULT 'ORG' COMMENT 'ORG|PLACE|USER|ASSET',
    `start_date`          DATE           NOT NULL,
    `end_date`            DATE           NULL,
    `cycle_type`          VARCHAR(20)    NOT NULL DEFAULT 'DAILY' COMMENT 'DAILY|WEEKLY|BIWEEKLY|MONTHLY|QUARTERLY|CUSTOM',
    `cycle_config`        JSON           NULL     COMMENT '周期自定义配置',
    `time_slots`          JSON           NULL     COMMENT '时间段配置',
    `skip_holidays`       TINYINT        NOT NULL DEFAULT 0,
    `holiday_calendar_id` BIGINT         NULL,
    `excluded_dates`      JSON           NULL     COMMENT '排除日期列表',
    `assignment_mode`     VARCHAR(20)    NOT NULL DEFAULT 'ASSIGNED' COMMENT 'FREE|ASSIGNED|HYBRID|ROUND_ROBIN|LOAD_BALANCED',
    `review_required`     TINYINT        NOT NULL DEFAULT 1,
    `auto_publish`        TINYINT        NOT NULL DEFAULT 0,
    `status`              VARCHAR(20)    NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT|PUBLISHED|PAUSED|COMPLETED|ARCHIVED',
    `created_by`          BIGINT         NULL,
    `created_at`          DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_by`          BIGINT         NULL,
    `updated_at`          DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`             TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_project_code` (`project_code`, `deleted`),
    INDEX `idx_template` (`template_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_dates` (`start_date`, `end_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 检查项目';

-- 2. 检查任务
CREATE TABLE IF NOT EXISTS `insp_tasks` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`         BIGINT       NOT NULL DEFAULT 0,
    `task_code`         VARCHAR(50)  NOT NULL,
    `project_id`        BIGINT       NOT NULL,
    `task_date`         DATE         NOT NULL,
    `time_slot_code`    VARCHAR(50)  NULL,
    `time_slot_start`   TIME         NULL,
    `time_slot_end`     TIME         NULL,
    `inspector_id`      BIGINT       NULL,
    `inspector_name`    VARCHAR(100) NULL,
    `reviewer_id`       BIGINT       NULL,
    `reviewer_name`     VARCHAR(100) NULL,
    `status`            VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING|CLAIMED|IN_PROGRESS|SUBMITTED|UNDER_REVIEW|REVIEWED|PUBLISHED|CANCELLED|EXPIRED',
    `total_targets`     INT          NOT NULL DEFAULT 0,
    `completed_targets` INT          NOT NULL DEFAULT 0,
    `skipped_targets`   INT          NOT NULL DEFAULT 0,
    `submitted_at`      DATETIME     NULL,
    `reviewed_at`       DATETIME     NULL,
    `published_at`      DATETIME     NULL,
    `review_comment`    TEXT         NULL,
    `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`           TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_task_code` (`task_code`, `deleted`),
    INDEX `idx_project` (`project_id`),
    INDEX `idx_inspector` (`inspector_id`),
    INDEX `idx_task_date` (`task_date`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 检查任务';

-- 3. 检查提交 (每个检查目标)
CREATE TABLE IF NOT EXISTS `insp_submissions` (
    `id`                BIGINT         NOT NULL AUTO_INCREMENT,
    `tenant_id`         BIGINT         NOT NULL DEFAULT 0,
    `task_id`           BIGINT         NOT NULL,
    `target_type`       VARCHAR(20)    NOT NULL COMMENT 'ORG|PLACE|USER|ASSET',
    `target_id`         BIGINT         NOT NULL,
    `target_name`       VARCHAR(200)   NULL,
    `org_unit_id`       BIGINT         NULL     COMMENT '目标所属组织',
    `org_unit_name`     VARCHAR(200)   NULL,
    `weight_ratio`      DECIMAL(5,2)   NOT NULL DEFAULT 1.00 COMMENT '权重系数',
    `status`            VARCHAR(20)    NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING|LOCKED|IN_PROGRESS|COMPLETED|SKIPPED',
    `form_data`         JSON           NULL     COMMENT '完整表单响应',
    `score_breakdown`   JSON           NULL     COMMENT '各维度分数',
    `base_score`        DECIMAL(10,2)  NULL,
    `final_score`       DECIMAL(10,2)  NULL,
    `deduction_total`   DECIMAL(10,2)  NOT NULL DEFAULT 0.00,
    `bonus_total`       DECIMAL(10,2)  NOT NULL DEFAULT 0.00,
    `grade`             VARCHAR(10)    NULL,
    `passed`            TINYINT        NULL,
    `sync_version`      INT            NOT NULL DEFAULT 1 COMMENT '离线同步版本号',
    `completed_at`      DATETIME       NULL,
    `created_at`        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`           TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_task` (`task_id`),
    INDEX `idx_target` (`target_type`, `target_id`),
    INDEX `idx_org_unit` (`org_unit_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 检查提交(每个检查目标)';

-- 4. 提交明细 (每个评分项)
CREATE TABLE IF NOT EXISTS `insp_submission_details` (
    `id`                 BIGINT         NOT NULL AUTO_INCREMENT,
    `tenant_id`          BIGINT         NOT NULL DEFAULT 0,
    `submission_id`      BIGINT         NOT NULL,
    `template_item_id`   BIGINT         NOT NULL,
    `item_code`          VARCHAR(50)    NOT NULL,
    `item_name`          VARCHAR(200)   NOT NULL,
    `section_id`         BIGINT         NULL,
    `section_name`       VARCHAR(200)   NULL,
    `item_type`          VARCHAR(30)    NOT NULL,
    `response_value`     JSON           NULL     COMMENT '响应值',
    `scoring_mode`       VARCHAR(20)    NULL     COMMENT 'DEDUCTION|ADDITION|DIRECT|PASS_FAIL',
    `score`              DECIMAL(10,2)  NULL,
    `dimensions`         JSON           NULL     COMMENT '影响的维度',
    `is_flagged`         TINYINT        NOT NULL DEFAULT 0,
    `flag_reason`        VARCHAR(500)   NULL,
    `remark`             TEXT           NULL,
    `created_at`         DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`         DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`            TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_submission` (`submission_id`),
    INDEX `idx_template_item` (`template_item_id`),
    INDEX `idx_flagged` (`is_flagged`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 提交明细(每个评分项)';

-- 5. 证据文件
CREATE TABLE IF NOT EXISTS `insp_evidences` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`       BIGINT       NOT NULL DEFAULT 0,
    `submission_id`   BIGINT       NOT NULL,
    `detail_id`       BIGINT       NULL     COMMENT '关联明细,null=提交级',
    `evidence_type`   VARCHAR(20)  NOT NULL COMMENT 'PHOTO|VIDEO|DOCUMENT|SIGNATURE|GPS_POINT',
    `file_name`       VARCHAR(500) NULL,
    `file_path`       VARCHAR(1000) NULL,
    `file_url`        VARCHAR(1000) NULL,
    `file_size`       BIGINT       NULL,
    `mime_type`       VARCHAR(100) NULL,
    `thumbnail_url`   VARCHAR(1000) NULL,
    `latitude`        DECIMAL(10,7) NULL,
    `longitude`       DECIMAL(10,7) NULL,
    `captured_at`     DATETIME     NULL,
    `metadata`        JSON         NULL,
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted`         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_submission` (`submission_id`),
    INDEX `idx_detail` (`detail_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 证据文件';

-- 6. 检查员池
CREATE TABLE IF NOT EXISTS `insp_project_inspectors` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`     BIGINT       NOT NULL DEFAULT 0,
    `project_id`    BIGINT       NOT NULL,
    `user_id`       BIGINT       NOT NULL,
    `user_name`     VARCHAR(100) NULL,
    `role`          VARCHAR(20)  NOT NULL DEFAULT 'INSPECTOR' COMMENT 'INSPECTOR|REVIEWER|LEAD',
    `is_active`     TINYINT      NOT NULL DEFAULT 1,
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_project_user_role` (`project_id`, `user_id`, `role`, `deleted`),
    INDEX `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 检查员池';
