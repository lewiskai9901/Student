-- V40: Corrective BC enhancements - Issue Categories table + Corrective Cases missing columns

-- 1. Issue Categories (tree structure for classifying issues)
CREATE TABLE IF NOT EXISTS `insp_issue_categories` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`   BIGINT       NOT NULL DEFAULT 0,
    `name`        VARCHAR(200) NOT NULL COMMENT '分类名称',
    `code`        VARCHAR(50)  NULL     COMMENT '分类编码',
    `parent_id`   BIGINT       NULL     COMMENT '父分类ID',
    `sort_order`  INT          NOT NULL DEFAULT 0,
    `description` VARCHAR(500) NULL,
    `is_active`   TINYINT(1)   NOT NULL DEFAULT 1,
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_issue_cat_parent` (`parent_id`),
    INDEX `idx_issue_cat_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='V7 问题分类';

-- 2. Add missing CAPA columns to corrective_cases
ALTER TABLE `insp_corrective_cases`
    ADD COLUMN IF NOT EXISTS `issue_category_id`         BIGINT       NULL COMMENT '问题分类ID' AFTER `required_action`,
    ADD COLUMN IF NOT EXISTS `deficiency_code`            VARCHAR(50)  NULL COMMENT '缺陷代码' AFTER `issue_category_id`,
    ADD COLUMN IF NOT EXISTS `rca_method`                 VARCHAR(20)  NULL COMMENT 'NONE|FIVE_WHYS|FISHBONE|FAULT_TREE|PARETO' AFTER `deficiency_code`,
    ADD COLUMN IF NOT EXISTS `rca_data`                   TEXT         NULL COMMENT 'JSON RCA数据' AFTER `rca_method`,
    ADD COLUMN IF NOT EXISTS `preventive_action`          TEXT         NULL COMMENT '预防措施' AFTER `rca_data`,
    ADD COLUMN IF NOT EXISTS `effectiveness_check_date`   DATE         NULL COMMENT '效果验证日期' AFTER `verification_note`,
    ADD COLUMN IF NOT EXISTS `effectiveness_status`       VARCHAR(20)  NULL COMMENT 'PENDING|CONFIRMED|FAILED' AFTER `effectiveness_check_date`,
    ADD COLUMN IF NOT EXISTS `effectiveness_note`         TEXT         NULL COMMENT '效果验证说明' AFTER `effectiveness_status`;
