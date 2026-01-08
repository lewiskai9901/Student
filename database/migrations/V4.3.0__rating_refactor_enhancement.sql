-- ============================================
-- 评级模块重构增强 - 数据库迁移脚本
-- 版本: 4.3.0
-- 日期: 2025-12-22
-- 说明:
--   1. 增强评级频次统计功能（连续记录、最近日期等）
--   2. 新增荣誉徽章系统
--   3. 增强评级结果表（趋势对比）
--   4. 优化索引性能
-- ============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================
-- 第一部分：增强现有表结构
-- =============================================

-- 1. 增强 check_plan_rating_rules 表
-- 添加模板和预警相关字段
SET @dbname = DATABASE();
SET @tablename = 'check_plan_rating_rules';

-- 添加 template_id 列（规则模板ID）
SET @columnname = 'template_id';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' BIGINT COMMENT ''基于的模板ID''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加 alert_enabled 列（是否启用预警）
SET @columnname = 'alert_enabled';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' TINYINT(1) DEFAULT 0 COMMENT ''是否启用预警: 0否 1是''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 2. 增强 check_plan_rating_frequency 表
-- 添加连续记录、最近日期等字段
SET @tablename = 'check_plan_rating_frequency';

-- 添加 level_color 列（冗余，便于展示）
SET @columnname = 'level_color';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' VARCHAR(20) COMMENT ''等级颜色（冗余）''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加 level_icon 列
SET @columnname = 'level_icon';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' VARCHAR(100) COMMENT ''等级图标（冗余）''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加 consecutive_count 列（连续获得次数）
SET @columnname = 'consecutive_count';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' INT DEFAULT 0 COMMENT ''连续获得次数（如连续3周优秀）''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加 best_streak 列（最佳连续记录）
SET @columnname = 'best_streak';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' INT DEFAULT 0 COMMENT ''最佳连续记录''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加 recent_dates 列（最近获得日期列表）
SET @columnname = 'recent_dates';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' JSON COMMENT ''最近获得日期列表（最近5次）''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 3. 增强 check_plan_rating_results 表
-- 添加趋势对比相关字段
SET @tablename = 'check_plan_rating_results';

-- 添加 previous_level_id 列（上次评级的等级ID）
SET @columnname = 'previous_level_id';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' BIGINT COMMENT ''上次评级的等级ID''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加 level_change 列（等级变化）
SET @columnname = 'level_change';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' VARCHAR(20) COMMENT ''等级变化: UP上升/DOWN下降/SAME持平/NEW首次''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加 percentile 列（百分位数）
SET @columnname = 'percentile';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' DECIMAL(5,2) COMMENT ''百分位数（ranking/total_classes*100）''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加 department_id 列（院系ID，冗余）
SET @columnname = 'department_id';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' BIGINT COMMENT ''院系ID（冗余）''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加 department_name 列（院系名称，冗余）
SET @columnname = 'department_name';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' VARCHAR(100) COMMENT ''院系名称（冗余）''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 4. 增强 notification_records 表
-- 添加评级通报相关字段
SET @tablename = 'notification_records';

-- 添加 check_plan_id 列（检查计划ID，用于评级通报）
SET @columnname = 'check_plan_id';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' BIGINT COMMENT ''检查计划ID（评级通报使用）''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加 period_start 列（统计周期开始）
SET @columnname = 'period_start';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' DATE COMMENT ''统计周期开始日期''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加 period_end 列（统计周期结束）
SET @columnname = 'period_end';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' DATE COMMENT ''统计周期结束日期''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加 class_count 列（涉及班级数，用于评级通报）
SET @columnname = 'class_count';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' INT DEFAULT 0 COMMENT ''涉及班级数量（评级通报）''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- =============================================
-- 第二部分：新增表结构
-- =============================================

-- 1. 评级规则模板表
DROP TABLE IF EXISTS `rating_rule_template`;
CREATE TABLE `rating_rule_template` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `template_name` VARCHAR(100) NOT NULL COMMENT '模板名称',
    `template_desc` TEXT COMMENT '模板描述',
    `template_config` JSON NOT NULL COMMENT '模板配置（完整规则配置JSON）',
    `scene_tag` VARCHAR(50) COMMENT '场景标签: DAILY_CHECK日常检查/MONTHLY_SUMMARY月度汇总/COMPETITION竞赛评比',
    `usage_count` INT DEFAULT 0 COMMENT '使用次数',
    `is_system` TINYINT(1) DEFAULT 0 COMMENT '是否系统预设: 0否 1是',
    `created_by` BIGINT COMMENT '创建人ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) DEFAULT 0 COMMENT '是否删除',

    PRIMARY KEY (`id`),
    INDEX `idx_scene_tag` (`scene_tag`),
    INDEX `idx_is_system` (`is_system`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级规则模板表';

-- 2. 荣誉徽章配置表
DROP TABLE IF EXISTS `rating_honor_badge`;
CREATE TABLE `rating_honor_badge` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `badge_name` VARCHAR(100) NOT NULL COMMENT '徽章名称: 卫生标兵/纪律模范/月度之星',
    `badge_icon` VARCHAR(200) COMMENT '徽章图标URL',
    `badge_level` VARCHAR(20) COMMENT '徽章等级: GOLD金质/SILVER银质/BRONZE铜质',
    `rule_id` BIGINT NOT NULL COMMENT '关联评级规则ID',
    `grant_condition` JSON NOT NULL COMMENT '授予条件配置',
    `description` TEXT COMMENT '徽章描述',
    `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    `auto_grant` TINYINT(1) DEFAULT 1 COMMENT '是否自动授予: 0手动 1自动',
    `created_by` BIGINT COMMENT '创建人ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) DEFAULT 0 COMMENT '是否删除',

    PRIMARY KEY (`id`),
    INDEX `idx_rule_id` (`rule_id`),
    INDEX `idx_badge_level` (`badge_level`),
    INDEX `idx_enabled` (`enabled`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='荣誉徽章配置表';

-- 3. 班级徽章获得记录表
DROP TABLE IF EXISTS `class_badge_record`;
CREATE TABLE `class_badge_record` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `badge_id` BIGINT NOT NULL COMMENT '徽章ID',
    `class_id` BIGINT NOT NULL COMMENT '班级ID',
    `class_name` VARCHAR(100) COMMENT '班级名称（冗余）',
    `grade_id` BIGINT COMMENT '年级ID',
    `grade_name` VARCHAR(50) COMMENT '年级名称（冗余）',
    `department_id` BIGINT COMMENT '院系ID',
    `department_name` VARCHAR(100) COMMENT '院系名称（冗余）',

    `granted_at` DATETIME NOT NULL COMMENT '授予时间',
    `granted_by` BIGINT COMMENT '授予人ID（手动授予时有值）',
    `period_start` DATE COMMENT '统计周期开始',
    `period_end` DATE COMMENT '统计周期结束',
    `achievement_data` JSON COMMENT '成就数据: {frequency: 15, rank: 1, rate: 75%, consecutiveCount: 5}',

    `certificate_url` VARCHAR(500) COMMENT '荣誉证书URL',
    `certificate_generated` TINYINT(1) DEFAULT 0 COMMENT '是否已生成证书',

    `revoked` TINYINT(1) DEFAULT 0 COMMENT '是否已撤销',
    `revoked_at` DATETIME COMMENT '撤销时间',
    `revoked_by` BIGINT COMMENT '撤销人ID',
    `revoked_reason` TEXT COMMENT '撤销原因',

    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    PRIMARY KEY (`id`),
    INDEX `idx_badge_id` (`badge_id`),
    INDEX `idx_class_id` (`class_id`),
    INDEX `idx_granted_at` (`granted_at`),
    INDEX `idx_period` (`period_start`, `period_end`),
    INDEX `idx_revoked` (`revoked`),
    UNIQUE KEY `uk_badge_class_period` (`badge_id`, `class_id`, `period_start`, `period_end`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级徽章获得记录表';

-- 4. 评级预警配置表
DROP TABLE IF EXISTS `rating_alert_config`;
CREATE TABLE `rating_alert_config` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `check_plan_id` BIGINT NOT NULL COMMENT '检查计划ID',
    `rule_id` BIGINT NOT NULL COMMENT '评级规则ID',
    `alert_name` VARCHAR(100) NOT NULL COMMENT '预警名称',
    `alert_type` VARCHAR(30) NOT NULL COMMENT '预警类型: CONTINUOUS_POOR连续较差/DECLINING_TREND下降趋势/BELOW_AVERAGE低于平均',
    `trigger_condition` JSON COMMENT '触发条件配置',
    `alert_level` VARCHAR(20) COMMENT '预警级别: WARNING警告/SERIOUS严重/URGENT紧急',
    `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    `notify_roles` JSON COMMENT '通知角色: ["班主任","年级主任","院系领导"]',
    `created_by` BIGINT COMMENT '创建人ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) DEFAULT 0 COMMENT '是否删除',

    PRIMARY KEY (`id`),
    INDEX `idx_check_plan_id` (`check_plan_id`),
    INDEX `idx_rule_id` (`rule_id`),
    INDEX `idx_alert_type` (`alert_type`),
    INDEX `idx_enabled` (`enabled`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级预警配置表';

-- 5. 评级预警记录表
DROP TABLE IF EXISTS `rating_alert_record`;
CREATE TABLE `rating_alert_record` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `alert_config_id` BIGINT NOT NULL COMMENT '预警配置ID',
    `class_id` BIGINT NOT NULL COMMENT '班级ID',
    `class_name` VARCHAR(100) COMMENT '班级名称（冗余）',

    `triggered_at` DATETIME NOT NULL COMMENT '触发时间',
    `alert_reason` TEXT COMMENT '预警原因详情',
    `alert_data` JSON COMMENT '预警数据（如连续记录、趋势数据等）',

    `notification_sent` TINYINT(1) DEFAULT 0 COMMENT '是否已发送通知',
    `notification_sent_at` DATETIME COMMENT '通知发送时间',

    `handled` TINYINT(1) DEFAULT 0 COMMENT '是否已处理',
    `handled_at` DATETIME COMMENT '处理时间',
    `handler_id` BIGINT COMMENT '处理人ID',
    `handle_remark` TEXT COMMENT '处理备注',

    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    PRIMARY KEY (`id`),
    INDEX `idx_alert_config_id` (`alert_config_id`),
    INDEX `idx_class_id` (`class_id`),
    INDEX `idx_triggered_at` (`triggered_at`),
    INDEX `idx_handled` (`handled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级预警记录表';

-- 6. 评级对比记录表
DROP TABLE IF EXISTS `rating_comparison_record`;
CREATE TABLE `rating_comparison_record` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `comparison_name` VARCHAR(100) COMMENT '对比名称',
    `comparison_type` VARCHAR(30) NOT NULL COMMENT '对比类型: TIME_PERIOD时间段/RULE规则/DEPARTMENT院系',
    `comparison_config` JSON NOT NULL COMMENT '对比配置',
    `result_data` JSON COMMENT '对比结果数据',
    `created_by` BIGINT COMMENT '创建人ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    PRIMARY KEY (`id`),
    INDEX `idx_comparison_type` (`comparison_type`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级对比记录表';

-- =============================================
-- 第三部分：优化索引
-- =============================================

-- 为 check_plan_rating_frequency 表添加复合索引
-- 提升按等级、时间段查询的性能
SET @tablename = 'check_plan_rating_frequency';
SET @indexname = 'idx_level_period';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND INDEX_NAME = @indexname) > 0,
  'SELECT 1',
  CONCAT('CREATE INDEX ', @indexname, ' ON ', @tablename, ' (level_id, period_start, period_end)')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 为 check_plan_rating_results 表添加复合索引
-- 提升按规则、日期查询的性能
SET @tablename = 'check_plan_rating_results';
SET @indexname = 'idx_rule_date';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND INDEX_NAME = @indexname) > 0,
  'SELECT 1',
  CONCAT('CREATE INDEX ', @indexname, ' ON ', @tablename, ' (rule_id, check_date)')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 为 check_plan_rating_results 表添加复合索引
-- 提升按班级、规则查询的性能
SET @indexname = 'idx_class_rule';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND INDEX_NAME = @indexname) > 0,
  'SELECT 1',
  CONCAT('CREATE INDEX ', @indexname, ' ON ', @tablename, ' (class_id, rule_id, check_date)')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- =============================================
-- 第四部分：插入系统预设数据
-- =============================================

-- 插入系统预设的评级规则模板
INSERT INTO `rating_rule_template` (`id`, `template_name`, `template_desc`, `template_config`, `scene_tag`, `is_system`, `created_at`) VALUES
(1, '三等级标准模板', '适用于日常检查的标准三等级评级',
 '{"ruleType":"DAILY","scoreSource":"TOTAL","divisionMethod":"SCORE_RANGE","levels":[{"levelOrder":1,"levelName":"优秀","levelColor":"#10b981","minScore":0,"maxScore":5},{"levelOrder":2,"levelName":"良好","levelColor":"#3b82f6","minScore":5,"maxScore":10},{"levelOrder":3,"levelName":"需改进","levelColor":"#ef4444","minScore":10,"maxScore":999}]}',
 'DAILY_CHECK', 1, NOW()),

(2, '竞赛式评比模板', '适用于月度评比的竞赛式评级',
 '{"ruleType":"SUMMARY","scoreSource":"TOTAL","divisionMethod":"RANK_COUNT","summaryMethod":"AVERAGE","levels":[{"levelOrder":1,"levelName":"一等奖","levelColor":"#fbbf24","rankCount":3},{"levelOrder":2,"levelName":"二等奖","levelColor":"#c0c0c0","rankCount":5},{"levelOrder":3,"levelName":"三等奖","levelColor":"#cd7f32","rankCount":7}]}',
 'COMPETITION', 1, NOW()),

(3, '百分比稳定模板', '适用于学期汇总的百分比评级',
 '{"ruleType":"SUMMARY","scoreSource":"TOTAL","divisionMethod":"PERCENTAGE","summaryMethod":"AVERAGE","levels":[{"levelOrder":1,"levelName":"A档","levelColor":"#10b981","percentage":10},{"levelOrder":2,"levelName":"B档","levelColor":"#3b82f6","percentage":20},{"levelOrder":3,"levelName":"C档","levelColor":"#f59e0b","percentage":30},{"levelOrder":4,"levelName":"D档","levelColor":"#ef4444","percentage":40}]}',
 'MONTHLY_SUMMARY', 1, NOW()),

(4, '纪律标兵模板', '适用于周度纪律评选',
 '{"ruleType":"SUMMARY","scoreSource":"CATEGORY","divisionMethod":"RANK_COUNT","summaryMethod":"SUM","categoryName":"纪律","levels":[{"levelOrder":1,"levelName":"纪律标兵","levelColor":"#fbbf24","rankCount":5},{"levelOrder":2,"levelName":"纪律优秀","levelColor":"#10b981","rankCount":10},{"levelOrder":3,"levelName":"纪律合格","levelColor":"#6b7280","rankCount":999}]}',
 'COMPETITION', 1, NOW())
ON DUPLICATE KEY UPDATE `template_name` = VALUES(`template_name`);

-- =============================================
-- 完成
-- =============================================

SET FOREIGN_KEY_CHECKS = 1;

-- 打印完成信息
SELECT '✅ 评级模块重构增强迁移完成！' AS status,
       '新增6张表，增强3张表，新增系统预设模板4个' AS summary;
