-- =====================================================
-- V7 检查平台 - 分析报表 Schema (Phase 5)
-- 2 tables: daily_summaries, period_summaries
-- =====================================================

-- 日汇总（每个项目+日期+目标一行）
CREATE TABLE IF NOT EXISTS `insp_daily_summaries` (
    `id`                  BIGINT         NOT NULL AUTO_INCREMENT,
    `tenant_id`           BIGINT         NOT NULL DEFAULT 0,
    `project_id`          BIGINT         NOT NULL,
    `summary_date`        DATE           NOT NULL,
    `target_type`         VARCHAR(20)    NOT NULL COMMENT 'ORG|PLACE|USER|ASSET',
    `target_id`           BIGINT         NOT NULL,
    `target_name`         VARCHAR(200)   NULL,
    `org_unit_id`         BIGINT         NULL,
    `org_unit_name`       VARCHAR(200)   NULL,
    `inspection_count`    INT            NOT NULL DEFAULT 0,
    `avg_score`           DECIMAL(8,2)   NULL,
    `min_score`           DECIMAL(8,2)   NULL,
    `max_score`           DECIMAL(8,2)   NULL,
    `total_deductions`    DECIMAL(8,2)   NOT NULL DEFAULT 0,
    `total_bonuses`       DECIMAL(8,2)   NOT NULL DEFAULT 0,
    `pass_count`          INT            NOT NULL DEFAULT 0,
    `fail_count`          INT            NOT NULL DEFAULT 0,
    `ranking`             INT            NULL,
    `dimension_scores`    JSON           NULL     COMMENT '各维度分数 {"safety":85,"hygiene":90}',
    `grade`               VARCHAR(20)    NULL,
    `created_at`          DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`          DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`             TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_daily` (`project_id`, `summary_date`, `target_type`, `target_id`, `deleted`),
    INDEX `idx_project_date` (`project_id`, `summary_date`),
    INDEX `idx_target` (`target_type`, `target_id`),
    INDEX `idx_org_unit` (`org_unit_id`),
    INDEX `idx_ranking` (`project_id`, `summary_date`, `ranking`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='V7 日汇总';

-- 周期汇总（周/月/季/年）
CREATE TABLE IF NOT EXISTS `insp_period_summaries` (
    `id`                       BIGINT         NOT NULL AUTO_INCREMENT,
    `tenant_id`                BIGINT         NOT NULL DEFAULT 0,
    `project_id`               BIGINT         NOT NULL,
    `period_type`              VARCHAR(20)    NOT NULL COMMENT 'WEEKLY|MONTHLY|QUARTERLY|YEARLY',
    `period_start`             DATE           NOT NULL,
    `period_end`               DATE           NOT NULL,
    `target_type`              VARCHAR(20)    NOT NULL,
    `target_id`                BIGINT         NOT NULL,
    `target_name`              VARCHAR(200)   NULL,
    `org_unit_id`              BIGINT         NULL,
    `org_unit_name`            VARCHAR(200)   NULL,
    `inspection_days`          INT            NOT NULL DEFAULT 0,
    `avg_score`                DECIMAL(8,2)   NULL,
    `min_score`                DECIMAL(8,2)   NULL,
    `max_score`                DECIMAL(8,2)   NULL,
    `score_std_dev`            DECIMAL(8,4)   NULL,
    `trend_direction`          VARCHAR(10)    NULL     COMMENT 'UP|DOWN|STABLE',
    `trend_percent`            DECIMAL(8,2)   NULL,
    `ranking`                  INT            NULL,
    `dimension_scores`         JSON           NULL,
    `grade`                    VARCHAR(20)    NULL,
    `corrective_count`         INT            NOT NULL DEFAULT 0,
    `corrective_closed_count`  INT            NOT NULL DEFAULT 0,
    `created_at`               DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`               DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`                  TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_period` (`project_id`, `period_type`, `period_start`, `target_type`, `target_id`, `deleted`),
    INDEX `idx_project_period` (`project_id`, `period_type`, `period_start`),
    INDEX `idx_target` (`target_type`, `target_id`),
    INDEX `idx_org_unit` (`org_unit_id`),
    INDEX `idx_ranking` (`project_id`, `period_type`, `period_start`, `ranking`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='V7 周期汇总';
