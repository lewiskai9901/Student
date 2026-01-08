-- ============================================
-- 评级系统增强 - 数据库设计
-- 版本: 3.3.0
-- 日期: 2024
-- ============================================

-- 1. 评级规则表增强 (添加周期配置和审核配置)
-- 先检查列是否存在再添加
SET @dbname = DATABASE();
SET @tablename = 'check_plan_rating_rules';

-- 添加 rating_cycle 列
SET @columnname = 'rating_cycle';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' VARCHAR(20) DEFAULT ''DAILY'' COMMENT ''评级周期: DAILY每天 WEEKLY每周 MONTHLY每月 CUSTOM自定义''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加 cycle_config 列
SET @columnname = 'cycle_config';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' JSON COMMENT ''周期配置''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加 require_approval 列
SET @columnname = 'require_approval';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' TINYINT DEFAULT 0 COMMENT ''是否需要审核: 0否 1是''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加 auto_calculate 列
SET @columnname = 'auto_calculate';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' TINYINT DEFAULT 1 COMMENT ''是否自动计算: 0否 1是''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 2. 评级结果表增强
SET @tablename = 'check_plan_rating_results';

-- 添加 approval_status 列
SET @columnname = 'approval_status';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' TINYINT DEFAULT 0 COMMENT ''审核状态: 0待审核 1已通过 2已驳回''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加 approved_by 列
SET @columnname = 'approved_by';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' BIGINT COMMENT ''审核人ID''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加 approved_at 列
SET @columnname = 'approved_at';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' DATETIME COMMENT ''审核时间''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加 approval_remark 列
SET @columnname = 'approval_remark';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' VARCHAR(500) COMMENT ''审核备注''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加 publish_status 列
SET @columnname = 'publish_status';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' TINYINT DEFAULT 0 COMMENT ''发布状态: 0未发布 1已发布''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加 published_at 列
SET @columnname = 'published_at';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' DATETIME COMMENT ''发布时间''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加 version 列
SET @columnname = 'version';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' INT DEFAULT 1 COMMENT ''版本号''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加 original_level_id 列
SET @columnname = 'original_level_id';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' BIGINT COMMENT ''原始等级ID''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 3. 评级频次统计表 (新增)
CREATE TABLE IF NOT EXISTS check_plan_rating_frequency (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    check_plan_id BIGINT NOT NULL COMMENT '检查计划ID',
    class_id BIGINT NOT NULL COMMENT '班级ID',
    class_name VARCHAR(100) COMMENT '班级名称(冗余)',
    grade_id BIGINT COMMENT '年级ID',
    grade_name VARCHAR(100) COMMENT '年级名称(冗余)',
    rule_id BIGINT NOT NULL COMMENT '评级规则ID',
    rule_name VARCHAR(100) COMMENT '规则名称(冗余)',
    level_id BIGINT NOT NULL COMMENT '等级ID',
    level_name VARCHAR(50) NOT NULL COMMENT '等级名称',
    level_color VARCHAR(20) COMMENT '等级颜色',
    frequency INT DEFAULT 0 COMMENT '获得该等级的次数',
    total_ratings INT DEFAULT 0 COMMENT '参与评级的总次数',
    frequency_rate DECIMAL(5,2) DEFAULT 0 COMMENT '频次占比(%)',
    period_type VARCHAR(20) NOT NULL COMMENT '统计周期类型: WEEK MONTH QUARTER SEMESTER YEAR CUSTOM',
    period_start DATE NOT NULL COMMENT '周期开始日期',
    period_end DATE NOT NULL COMMENT '周期结束日期',
    period_label VARCHAR(50) COMMENT '周期标签: 2024年第1周, 2024年1月等',
    last_rating_date DATE COMMENT '最近一次获得该等级的日期',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_plan_period (check_plan_id, period_type, period_start, period_end),
    INDEX idx_class_rule (class_id, rule_id),
    INDEX idx_level_frequency (level_id, frequency DESC),
    UNIQUE KEY uk_class_rule_level_period (class_id, rule_id, level_id, period_type, period_start)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评级频次统计表';

-- 4. 评级审核日志表 (新增)
CREATE TABLE IF NOT EXISTS check_plan_rating_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    result_id BIGINT NOT NULL COMMENT '评级结果ID',
    action VARCHAR(20) NOT NULL COMMENT '操作类型: APPROVE REJECT MODIFY PUBLISH UNPUBLISH',
    before_status TINYINT COMMENT '操作前状态',
    after_status TINYINT COMMENT '操作后状态',
    before_level_id BIGINT COMMENT '修改前等级ID',
    after_level_id BIGINT COMMENT '修改后等级ID',
    operator_id BIGINT NOT NULL COMMENT '操作人ID',
    operator_name VARCHAR(50) COMMENT '操作人姓名',
    remark VARCHAR(500) COMMENT '操作备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_result (result_id),
    INDEX idx_operator (operator_id),
    INDEX idx_action_time (action, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评级审核日志表';

-- 5. 评级周期任务表 (新增 - 用于定时任务)
CREATE TABLE IF NOT EXISTS check_plan_rating_schedule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    check_plan_id BIGINT NOT NULL COMMENT '检查计划ID',
    rule_id BIGINT NOT NULL COMMENT '评级规则ID',
    schedule_type VARCHAR(20) NOT NULL COMMENT '调度类型: DAILY WEEKLY MONTHLY',
    cron_expression VARCHAR(100) COMMENT 'Cron表达式',
    next_run_time DATETIME COMMENT '下次执行时间',
    last_run_time DATETIME COMMENT '上次执行时间',
    last_run_status VARCHAR(20) COMMENT '上次执行状态: SUCCESS FAILED',
    last_run_message TEXT COMMENT '上次执行消息',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_plan_rule (check_plan_id, rule_id),
    INDEX idx_next_run (enabled, next_run_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评级定时任务表';

-- 6. 评级汇总统计表 (新增 - 用于快速查询)
CREATE TABLE IF NOT EXISTS check_plan_rating_summary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    check_plan_id BIGINT NOT NULL COMMENT '检查计划ID',
    rule_id BIGINT NOT NULL COMMENT '评级规则ID',
    period_type VARCHAR(20) NOT NULL COMMENT '周期类型',
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    period_label VARCHAR(50) COMMENT '周期标签',
    total_classes INT DEFAULT 0 COMMENT '参与班级总数',
    total_ratings INT DEFAULT 0 COMMENT '评级总次数',
    level_distribution JSON COMMENT '等级分布: [{levelId, levelName, count, percentage}]',
    top_classes JSON COMMENT '各等级TOP班级: [{levelId, classes: [{classId, className, frequency}]}]',
    calculated_at DATETIME COMMENT '计算时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_plan_rule_period (check_plan_id, rule_id, period_type, period_start),
    INDEX idx_period (period_type, period_start, period_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评级汇总统计表';
