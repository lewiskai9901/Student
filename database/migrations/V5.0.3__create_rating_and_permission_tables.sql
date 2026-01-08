-- ============================================================================
-- 学生管理系统 - 架构重构迁移脚本 V5.0.3
-- 创建评级模块和权限系统表
-- 日期: 2026-01-02
-- ============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------------
-- 1. 评级模板表
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `rating_templates` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `template_code` VARCHAR(50) NOT NULL COMMENT '模板编码',
    `template_name` VARCHAR(100) NOT NULL COMMENT '模板名称',
    `description` TEXT DEFAULT NULL COMMENT '描述',
    `inspection_template_id` BIGINT DEFAULT NULL COMMENT '关联的检查模板',
    `is_default` TINYINT DEFAULT 0 COMMENT '是否默认',
    `status` TINYINT DEFAULT 1 COMMENT '状态',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_code` (`template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级模板表';

-- ---------------------------------------------------------------------------
-- 2. 评级规则表
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `rating_rules` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `template_id` BIGINT NOT NULL COMMENT '评级模板ID',
    `rule_code` VARCHAR(50) NOT NULL COMMENT '规则编码',
    `rule_name` VARCHAR(100) NOT NULL COMMENT '规则名称',
    `rating_basis` ENUM('TOTAL', 'SINGLE_CATEGORY', 'MULTI_CATEGORY', 'CUSTOM') NOT NULL COMMENT '评级依据',
    `category_ids` JSON DEFAULT NULL COMMENT '依据的类别ID列表',
    `score_type` ENUM('DEDUCTION', 'WEIGHTED', 'FINAL') NOT NULL COMMENT '使用的分数类型',
    `rating_method` ENUM('ABSOLUTE', 'PERCENTAGE', 'RANKING') NOT NULL COMMENT '评级方式',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `is_enabled` TINYINT DEFAULT 1 COMMENT '是否启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_rule` (`template_id`, `rule_code`),
    KEY `idx_template` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级规则表';

-- ---------------------------------------------------------------------------
-- 3. 评级等级表
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `rating_levels` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `rule_id` BIGINT NOT NULL COMMENT '规则ID',
    `level_code` VARCHAR(20) NOT NULL COMMENT '等级代码',
    `level_name` VARCHAR(50) NOT NULL COMMENT '等级名称',
    `min_score` DECIMAL(10,2) DEFAULT NULL COMMENT '最小分数',
    `max_score` DECIMAL(10,2) DEFAULT NULL COMMENT '最大分数',
    `min_percent` DECIMAL(5,2) DEFAULT NULL COMMENT '最小百分比',
    `max_percent` DECIMAL(5,2) DEFAULT NULL COMMENT '最大百分比',
    `top_n` INT DEFAULT NULL COMMENT '前N名',
    `top_percent` DECIMAL(5,2) DEFAULT NULL COMMENT '前N%',
    `color` VARCHAR(20) DEFAULT NULL COMMENT '颜色代码',
    `icon` VARCHAR(100) DEFAULT NULL COMMENT '图标',
    `reward_points` INT DEFAULT 0 COMMENT '奖励积分',
    `penalty_points` INT DEFAULT 0 COMMENT '惩罚积分',
    `level_order` INT NOT NULL COMMENT '等级顺序',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_rule` (`rule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级等级表';

-- ---------------------------------------------------------------------------
-- 4. 评级结果表
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `rating_results` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `record_id` BIGINT NOT NULL COMMENT '检查记录ID',
    `class_score_id` BIGINT NOT NULL COMMENT '班级得分ID',
    `class_id` BIGINT NOT NULL COMMENT '班级ID',
    `rule_id` BIGINT NOT NULL COMMENT '评级规则ID',
    `level_id` BIGINT NOT NULL COMMENT '评级等级ID',
    `rule_name` VARCHAR(100) NOT NULL COMMENT '规则名称快照',
    `level_code` VARCHAR(20) NOT NULL COMMENT '等级代码快照',
    `level_name` VARCHAR(50) NOT NULL COMMENT '等级名称快照',
    `calculated_score` DECIMAL(10,2) DEFAULT NULL COMMENT '计算分数',
    `calculated_percent` DECIMAL(5,2) DEFAULT NULL COMMENT '百分比',
    `calculated_rank` INT DEFAULT NULL COMMENT '排名',
    `reward_points` INT DEFAULT 0 COMMENT '奖励积分',
    `result_version` INT DEFAULT 1 COMMENT '结果版本',
    `is_latest` TINYINT DEFAULT 1 COMMENT '是否最新',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_record_class_rule_version` (`record_id`, `class_id`, `rule_id`, `result_version`),
    KEY `idx_record` (`record_id`),
    KEY `idx_class` (`class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级结果表';

-- ---------------------------------------------------------------------------
-- 5. 数据权限配置表 (新设计)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `data_permissions` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `principal_type` ENUM('USER', 'ROLE') NOT NULL COMMENT '主体类型',
    `principal_id` BIGINT NOT NULL COMMENT '主体ID',
    `resource_module` VARCHAR(50) NOT NULL COMMENT '资源模块',
    `data_scope` ENUM('ALL', 'DEPARTMENT', 'DEPARTMENT_AND_CHILD', 'SELF_DEPARTMENT', 'GRADE', 'CLASS', 'SELF', 'CUSTOM') NOT NULL COMMENT '数据范围',
    `custom_org_unit_ids` JSON DEFAULT NULL COMMENT '自定义组织ID列表',
    `custom_class_ids` JSON DEFAULT NULL COMMENT '自定义班级ID列表',
    `custom_user_ids` JSON DEFAULT NULL COMMENT '自定义用户ID列表',
    `allowed_actions` JSON DEFAULT '["read"]' COMMENT '允许的操作',
    `conditions` JSON DEFAULT NULL COMMENT 'ABAC条件表达式',
    `priority` INT DEFAULT 0 COMMENT '优先级',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by` BIGINT DEFAULT NULL,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_principal` (`principal_type`, `principal_id`),
    KEY `idx_module` (`resource_module`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据权限配置表';

-- ---------------------------------------------------------------------------
-- 6. 操作审计日志表 (增强版)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `audit_logs` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `operation_type` VARCHAR(50) NOT NULL COMMENT '操作类型',
    `operation_module` VARCHAR(50) NOT NULL COMMENT '操作模块',
    `operation_desc` VARCHAR(500) DEFAULT NULL COMMENT '操作描述',
    `target_type` VARCHAR(50) DEFAULT NULL COMMENT '目标类型',
    `target_id` BIGINT DEFAULT NULL COMMENT '目标ID',
    `target_name` VARCHAR(200) DEFAULT NULL COMMENT '目标名称',
    `operator_id` BIGINT NOT NULL COMMENT '操作人ID',
    `operator_name` VARCHAR(50) NOT NULL COMMENT '操作人姓名',
    `operator_ip` VARCHAR(50) DEFAULT NULL COMMENT '操作人IP',
    `operator_ua` VARCHAR(500) DEFAULT NULL COMMENT 'User-Agent',
    `old_value` JSON DEFAULT NULL COMMENT '变更前值',
    `new_value` JSON DEFAULT NULL COMMENT '变更后值',
    `diff_value` JSON DEFAULT NULL COMMENT '差异',
    `request_url` VARCHAR(500) DEFAULT NULL COMMENT '请求URL',
    `request_method` VARCHAR(10) DEFAULT NULL COMMENT 'HTTP方法',
    `request_params` JSON DEFAULT NULL COMMENT '请求参数',
    `response_code` INT DEFAULT NULL COMMENT '响应码',
    `execution_time` BIGINT DEFAULT NULL COMMENT '执行耗时(ms)',
    `success` TINYINT DEFAULT 1 COMMENT '是否成功',
    `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_operator` (`operator_id`),
    KEY `idx_module` (`operation_module`),
    KEY `idx_target` (`target_type`, `target_id`),
    KEY `idx_time` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作审计日志表';

-- ---------------------------------------------------------------------------
-- 7. 领域事件存储表 (用于事件溯源)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `domain_events` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `event_id` VARCHAR(36) NOT NULL COMMENT '事件UUID',
    `event_type` VARCHAR(100) NOT NULL COMMENT '事件类型',
    `aggregate_type` VARCHAR(100) NOT NULL COMMENT '聚合类型',
    `aggregate_id` VARCHAR(100) NOT NULL COMMENT '聚合ID',
    `aggregate_version` INT NOT NULL COMMENT '聚合版本',
    `payload` JSON NOT NULL COMMENT '事件数据',
    `metadata` JSON DEFAULT NULL COMMENT '元数据',
    `occurred_at` DATETIME(3) NOT NULL COMMENT '发生时间',
    `created_at` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_event_id` (`event_id`),
    KEY `idx_aggregate` (`aggregate_type`, `aggregate_id`),
    KEY `idx_occurred` (`occurred_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='领域事件存储表';

-- ---------------------------------------------------------------------------
-- 8. 事件发布记录表
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `event_publications` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `event_id` VARCHAR(36) NOT NULL COMMENT '事件ID',
    `event_type` VARCHAR(100) NOT NULL COMMENT '事件类型',
    `status` ENUM('PENDING', 'PUBLISHED', 'FAILED') DEFAULT 'PENDING' COMMENT '状态',
    `retry_count` INT DEFAULT 0 COMMENT '重试次数',
    `last_error` TEXT DEFAULT NULL COMMENT '最后错误',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `published_at` DATETIME DEFAULT NULL COMMENT '发布时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_event_id` (`event_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='事件发布记录表';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- 迁移完成标记
-- ============================================================================
-- 执行完成后，请继续执行数据迁移脚本
