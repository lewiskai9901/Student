-- ============================================================
-- Migration: V30.0.0__insp_v7_template_schema.sql
-- Description: V7 检查平台 - Template BC 建表 (7张表)
-- ============================================================

-- -----------------------------------------------------------
-- 1. insp_template_catalogs (模板分类目录 - 树形)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `insp_template_catalogs` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`    BIGINT       NOT NULL DEFAULT 0,
    `parent_id`    BIGINT       NULL     COMMENT '父分类ID，NULL=根分类',
    `catalog_code` VARCHAR(50)  NOT NULL,
    `catalog_name` VARCHAR(100) NOT NULL,
    `description`  VARCHAR(500) NULL,
    `icon`         VARCHAR(50)  NULL,
    `sort_order`   INT          NOT NULL DEFAULT 0,
    `is_enabled`   TINYINT      NOT NULL DEFAULT 1,
    `created_by`   BIGINT       NULL,
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_by`   BIGINT       NULL,
    `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_tenant_parent` (`tenant_id`, `parent_id`),
    UNIQUE INDEX `uk_tenant_code` (`tenant_id`, `catalog_code`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 模板分类目录';

-- -----------------------------------------------------------
-- 2. insp_templates (模板定义)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `insp_templates` (
    `id`                      BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`               BIGINT       NOT NULL DEFAULT 0,
    `template_code`           VARCHAR(50)  NOT NULL,
    `template_name`           VARCHAR(200) NOT NULL,
    `description`             TEXT         NULL,
    `catalog_id`              BIGINT       NULL,
    `tags`                    JSON         NULL     COMMENT '["安全","卫生"]',
    `applicable_target_types` JSON         NULL     COMMENT '["ORG","PLACE"]',
    `latest_version`          INT          NOT NULL DEFAULT 0,
    `status`                  VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    `is_default`              TINYINT      NOT NULL DEFAULT 0,
    `use_count`               INT          NOT NULL DEFAULT 0,
    `last_used_at`            DATETIME     NULL,
    `created_by`              BIGINT       NULL,
    `created_at`              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_by`              BIGINT       NULL,
    `updated_at`              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`                 TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_tenant_status` (`tenant_id`, `status`),
    UNIQUE INDEX `uk_tenant_code` (`tenant_id`, `template_code`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 检查模板';

-- -----------------------------------------------------------
-- 3. insp_template_versions (不可变版本快照)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `insp_template_versions` (
    `id`                        BIGINT   NOT NULL AUTO_INCREMENT,
    `tenant_id`                 BIGINT   NOT NULL DEFAULT 0,
    `template_id`               BIGINT   NOT NULL,
    `version`                   INT      NOT NULL,
    `structure_snapshot`        JSON     NOT NULL COMMENT '完整 sections+items 树',
    `scoring_profile_snapshot`  JSON     NULL     COMMENT '评分配置快照',
    `created_by`                BIGINT   NULL,
    `created_at`                DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted`                   TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_template_version` (`template_id`, `version`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 模板版本快照（不可变）';

-- -----------------------------------------------------------
-- 4. insp_template_sections (模板分区 - 层级)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `insp_template_sections` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`         BIGINT       NOT NULL DEFAULT 0,
    `template_id`       BIGINT       NOT NULL,
    `parent_section_id` BIGINT       NULL,
    `section_code`      VARCHAR(50)  NOT NULL,
    `section_name`      VARCHAR(200) NOT NULL,
    `sort_order`        INT          NOT NULL DEFAULT 0,
    `weight`            INT          NOT NULL DEFAULT 100,
    `is_repeatable`     TINYINT      NOT NULL DEFAULT 0,
    `condition_logic`   JSON         NULL,
    `created_by`        BIGINT       NULL,
    `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_by`        BIGINT       NULL,
    `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`           TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_template` (`template_id`),
    INDEX `idx_parent` (`parent_section_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 模板分区（层级）';

-- -----------------------------------------------------------
-- 5. insp_template_items (模板字段 - 22种类型)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `insp_template_items` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`        BIGINT       NOT NULL DEFAULT 0,
    `section_id`       BIGINT       NOT NULL,
    `item_code`        VARCHAR(50)  NOT NULL,
    `item_name`        VARCHAR(200) NOT NULL,
    `description`      TEXT         NULL,
    `item_type`        VARCHAR(30)  NOT NULL,
    `config`           JSON         NULL     COMMENT '类型特定配置',
    `validation_rules` JSON         NULL     COMMENT '验证规则数组',
    `response_set_id`  BIGINT       NULL,
    `scoring_config`   JSON         NULL     COMMENT '评分+归一化配置',
    `help_content`     TEXT         NULL     COMMENT '帮助文本/参考图片URL',
    `is_required`      TINYINT      NOT NULL DEFAULT 0,
    `is_scored`        TINYINT      NOT NULL DEFAULT 0,
    `require_evidence` TINYINT      NOT NULL DEFAULT 0 COMMENT '非媒体字段是否强制附证据',
    `sort_order`       INT          NOT NULL DEFAULT 0,
    `condition_logic`  JSON         NULL,
    `created_by`       BIGINT       NULL,
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_by`       BIGINT       NULL,
    `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`          TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_section` (`section_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 模板字段（22种类型）';

-- -----------------------------------------------------------
-- 6. insp_response_sets (可复用选项集)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `insp_response_sets` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`  BIGINT       NOT NULL DEFAULT 0,
    `set_code`   VARCHAR(50)  NOT NULL,
    `set_name`   VARCHAR(200) NOT NULL,
    `is_global`  TINYINT      NOT NULL DEFAULT 0,
    `is_enabled` TINYINT      NOT NULL DEFAULT 1,
    `created_by` BIGINT       NULL,
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_by` BIGINT       NULL,
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`    TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_tenant_code` (`tenant_id`, `set_code`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 可复用选项集';

-- -----------------------------------------------------------
-- 7. insp_response_set_options (选项集选项)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `insp_response_set_options` (
    `id`              BIGINT         NOT NULL AUTO_INCREMENT,
    `tenant_id`       BIGINT         NOT NULL DEFAULT 0,
    `response_set_id` BIGINT         NOT NULL,
    `option_value`    VARCHAR(100)   NOT NULL,
    `option_label`    VARCHAR(200)   NOT NULL,
    `option_color`    VARCHAR(20)    NULL,
    `score`           DECIMAL(10,2)  NULL,
    `is_flagged`      TINYINT        NOT NULL DEFAULT 0,
    `sort_order`      INT            NOT NULL DEFAULT 0,
    `created_at`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_response_set` (`response_set_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 选项集选项';
