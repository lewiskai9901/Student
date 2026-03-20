-- V35.0.0: V7 检查平台 - 评级集成 (Rating Integration)
-- 桥接表: V7 检查项目 → 评级配置

CREATE TABLE IF NOT EXISTS `insp_rating_links` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL DEFAULT 0,
    `project_id` BIGINT NOT NULL COMMENT 'V7 检查项目 ID',
    `rating_config_id` BIGINT NOT NULL COMMENT '评级配置 ID (rating_configs)',
    `period_type` VARCHAR(20) NOT NULL COMMENT '周期类型: WEEKLY, MONTHLY',
    `auto_calculate` TINYINT NOT NULL DEFAULT 1 COMMENT '是否自动计算: 1=是 0=否',
    `created_by` BIGINT DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_project_config` (`project_id`, `rating_config_id`, `deleted`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_rating_config_id` (`rating_config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='V7 检查项目-评级配置关联';
