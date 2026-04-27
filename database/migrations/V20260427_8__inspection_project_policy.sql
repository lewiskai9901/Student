-- review #E + #F: 项目级业务策略配置 — 不同项目可差异化整改/驳回/申诉规则
-- NULL 表示沿用系统默认常量 (CorrectiveCase.MAX_AUTO_ESCALATION_LEVEL=3 等)

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'insp_projects'
      AND COLUMN_NAME = 'max_reject_count'
);
SET @sql := IF(@col_exists = 0,
    "ALTER TABLE insp_projects ADD COLUMN max_reject_count INT NULL COMMENT '任务自动驳回上限, NULL=系统默认 3'",
    "SELECT 'max_reject_count exists' AS msg");
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'insp_projects'
      AND COLUMN_NAME = 'max_escalation_level'
);
SET @sql := IF(@col_exists = 0,
    "ALTER TABLE insp_projects ADD COLUMN max_escalation_level INT NULL COMMENT '整改自动升级上限, NULL=系统默认 3'",
    "SELECT 'max_escalation_level exists' AS msg");
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'insp_projects'
      AND COLUMN_NAME = 'appeal_window_days'
);
SET @sql := IF(@col_exists = 0,
    "ALTER TABLE insp_projects ADD COLUMN appeal_window_days INT NULL COMMENT '申诉时效 (从 task 发布起的天数), NULL=系统默认 7'",
    "SELECT 'appeal_window_days exists' AS msg");
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
