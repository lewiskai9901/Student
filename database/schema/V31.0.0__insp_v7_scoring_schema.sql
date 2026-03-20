-- =====================================================
-- V7 检查平台 - 评分引擎 Schema (Phase 2)
-- 4 tables: scoring_profiles, score_dimensions, grade_bands, calculation_rules
-- =====================================================

-- 1. 评分配置
CREATE TABLE IF NOT EXISTS `insp_scoring_profiles` (
    `id`                      BIGINT         NOT NULL AUTO_INCREMENT,
    `tenant_id`               BIGINT         NOT NULL DEFAULT 0,
    `template_id`             BIGINT         NOT NULL,
    `base_score`              DECIMAL(10,2)  NOT NULL DEFAULT 100.00,
    `max_score`               DECIMAL(10,2)  NOT NULL DEFAULT 100.00,
    `min_score`               DECIMAL(10,2)  NOT NULL DEFAULT 0.00,
    `allow_negative`          TINYINT        NOT NULL DEFAULT 0,
    `precision_digits`        INT            NOT NULL DEFAULT 2,
    `aggregation_method`      VARCHAR(20)    NOT NULL DEFAULT 'WEIGHTED_AVG',
    `formula_engine`          VARCHAR(20)    NOT NULL DEFAULT 'EXPRESSION',
    `default_normalization`   JSON           NULL     COMMENT '全局默认归一化配置',
    `created_by`              BIGINT         NULL,
    `created_at`              DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_by`              BIGINT         NULL,
    `updated_at`              DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`                 TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_template` (`template_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 评分配置';

-- 2. 评分维度
CREATE TABLE IF NOT EXISTS `insp_score_dimensions` (
    `id`                 BIGINT         NOT NULL AUTO_INCREMENT,
    `tenant_id`          BIGINT         NOT NULL DEFAULT 0,
    `scoring_profile_id` BIGINT         NOT NULL,
    `dimension_code`     VARCHAR(50)    NOT NULL,
    `dimension_name`     VARCHAR(100)   NOT NULL,
    `weight`             INT            NOT NULL DEFAULT 100,
    `base_score`         DECIMAL(10,2)  NOT NULL DEFAULT 100.00,
    `pass_threshold`     DECIMAL(10,2)  NULL,
    `formula`            TEXT           NULL,
    `sort_order`         INT            NOT NULL DEFAULT 0,
    `created_at`         DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`         DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`            TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_profile` (`scoring_profile_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 评分维度';

-- 3. 等级映射
CREATE TABLE IF NOT EXISTS `insp_grade_bands` (
    `id`                 BIGINT         NOT NULL AUTO_INCREMENT,
    `tenant_id`          BIGINT         NOT NULL DEFAULT 0,
    `scoring_profile_id` BIGINT         NOT NULL,
    `dimension_id`       BIGINT         NULL     COMMENT 'null=总分等级',
    `grade_code`         VARCHAR(10)    NOT NULL,
    `grade_name`         VARCHAR(50)    NOT NULL,
    `min_score`          DECIMAL(10,2)  NOT NULL,
    `max_score`          DECIMAL(10,2)  NOT NULL,
    `color`              VARCHAR(20)    NULL,
    `icon`               VARCHAR(50)    NULL,
    `sort_order`         INT            NOT NULL DEFAULT 0,
    `created_at`         DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`         DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`            TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_profile` (`scoring_profile_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 等级映射';

-- 4. 计算规则链
CREATE TABLE IF NOT EXISTS `insp_calculation_rules` (
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`          BIGINT       NOT NULL DEFAULT 0,
    `scoring_profile_id` BIGINT       NOT NULL,
    `rule_code`          VARCHAR(50)  NOT NULL,
    `rule_name`          VARCHAR(100) NOT NULL,
    `priority`           INT          NOT NULL DEFAULT 0,
    `rule_type`          VARCHAR(30)  NOT NULL COMMENT 'CEILING|FLOOR|VETO|PROGRESSIVE|BONUS|TIME_DECAY|POPULATION_NORMALIZE|REPEAT_OFFENSE|CUSTOM',
    `config`             JSON         NOT NULL,
    `is_enabled`         TINYINT      NOT NULL DEFAULT 1,
    `created_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`            TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_profile_priority` (`scoring_profile_id`, `priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 计算规则链';
