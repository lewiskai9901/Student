CREATE TABLE IF NOT EXISTS `insp_holiday_calendars` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`     BIGINT       NOT NULL DEFAULT 0,
    `calendar_name` VARCHAR(200) NOT NULL,
    `year`          INT          NOT NULL,
    `holidays`      TEXT         NULL COMMENT 'JSON array of date strings',
    `workdays`      TEXT         NULL COMMENT 'JSON array of make-up workday dates',
    `is_default`    TINYINT(1)   NOT NULL DEFAULT 0,
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_holiday_year` (`year`),
    INDEX `idx_holiday_default` (`is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='V7 假日日历';
