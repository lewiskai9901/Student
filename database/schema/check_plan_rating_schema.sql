-- =====================================================
-- 检查计划评级系统 - 数据库表结构
-- 版本: 1.0
-- 创建日期: 2025-12-16
-- 说明: 支持按检查计划配置评级规则,支持单次检查评级和汇总评级
-- =====================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------
-- 1. 检查计划评级规则表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `check_plan_rating_rules`;
CREATE TABLE `check_plan_rating_rules` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `check_plan_id` BIGINT NOT NULL COMMENT '关联检查计划ID',
    `rule_name` VARCHAR(100) NOT NULL COMMENT '规则名称，如"卫生评级"',
    `rule_type` VARCHAR(20) NOT NULL COMMENT '评级类型：DAILY-单次检查评级，SUMMARY-汇总评级',

    -- 评分来源配置
    `score_source` VARCHAR(20) NOT NULL DEFAULT 'TOTAL' COMMENT '评分来源：TOTAL-总分，CATEGORY-按类别',
    `category_id` BIGINT NULL COMMENT '当score_source=CATEGORY时，指定类别ID',
    `category_name` VARCHAR(100) NULL COMMENT '类别名称(冗余存储)',
    `use_weighted_score` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否使用加权分数：0-原始扣分，1-加权扣分',

    -- 划分方式
    `division_method` VARCHAR(20) NOT NULL COMMENT '划分方式：SCORE_RANGE-分数段，RANK_COUNT-名次数量，PERCENTAGE-百分比',

    -- 汇总评级的分数聚合方式
    `summary_method` VARCHAR(20) NULL DEFAULT 'AVERAGE' COMMENT '汇总方式（仅SUMMARY类型）：AVERAGE-平均，SUM-累加',

    -- 其他配置
    `sort_order` INT DEFAULT 0 COMMENT '排序序号',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    `description` VARCHAR(500) NULL COMMENT '规则描述',
    `created_by` BIGINT NULL COMMENT '创建人ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) DEFAULT 0 COMMENT '是否删除',

    PRIMARY KEY (`id`),
    INDEX `idx_check_plan_id` (`check_plan_id`),
    INDEX `idx_rule_type` (`rule_type`),
    INDEX `idx_enabled` (`enabled`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查计划评级规则表';

-- ---------------------------------------------------
-- 2. 评级等级配置表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `check_plan_rating_levels`;
CREATE TABLE `check_plan_rating_levels` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `rule_id` BIGINT NOT NULL COMMENT '关联规则ID',
    `level_order` INT NOT NULL COMMENT '等级顺序（1最高，数字越小等级越高）',
    `level_name` VARCHAR(50) NOT NULL COMMENT '等级名称，如"优秀卫生班级"',
    `level_color` VARCHAR(20) NULL COMMENT '等级颜色（用于前端展示，如#FF0000）',
    `level_icon` VARCHAR(100) NULL COMMENT '等级图标URL或图标名',

    -- 划分条件（根据division_method使用不同字段）
    -- 注意：扣分越少越好，所以min_score表示扣分上限（不含），max_score表示扣分下限（含）
    `min_score` DECIMAL(10,2) NULL COMMENT '扣分下限（含），SCORE_RANGE时使用',
    `max_score` DECIMAL(10,2) NULL COMMENT '扣分上限（不含），SCORE_RANGE时使用',
    `rank_count` INT NULL COMMENT '名次数量，RANK_COUNT时使用（如前3名）',
    `percentage` DECIMAL(5,2) NULL COMMENT '百分比，PERCENTAGE时使用（如前10%）',

    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX `idx_rule_id` (`rule_id`),
    INDEX `idx_level_order` (`level_order`),
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级等级配置表';

-- ---------------------------------------------------
-- 3. 评级结果表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `check_plan_rating_results`;
CREATE TABLE `check_plan_rating_results` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `rule_id` BIGINT NOT NULL COMMENT '关联规则ID',
    `check_plan_id` BIGINT NOT NULL COMMENT '检查计划ID',
    `check_record_id` BIGINT NULL COMMENT '检查记录ID（DAILY类型时有值）',
    `check_date` DATE NULL COMMENT '检查日期（DAILY类型时有值）',

    -- 评级对象
    `class_id` BIGINT NOT NULL COMMENT '班级ID',
    `class_name` VARCHAR(100) NULL COMMENT '班级名称（冗余存储）',
    `grade_id` BIGINT NULL COMMENT '年级ID',
    `grade_name` VARCHAR(50) NULL COMMENT '年级名称（冗余存储）',

    -- 评级结果
    `level_id` BIGINT NOT NULL COMMENT '评级等级ID',
    `level_name` VARCHAR(50) NOT NULL COMMENT '等级名称（冗余存储）',
    `level_order` INT NOT NULL COMMENT '等级顺序（冗余存储）',
    `ranking` INT NULL COMMENT '排名（扣分从少到多）',
    `total_classes` INT NULL COMMENT '参与评级的班级总数',
    `score` DECIMAL(10,2) NOT NULL COMMENT '扣分（用于评级的分数）',

    -- 时间范围（SUMMARY类型时使用）
    `period_start` DATE NULL COMMENT '统计周期开始',
    `period_end` DATE NULL COMMENT '统计周期结束',
    `record_count` INT NULL COMMENT '包含的检查记录数（SUMMARY类型）',

    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    PRIMARY KEY (`id`),
    INDEX `idx_rule_id` (`rule_id`),
    INDEX `idx_check_plan_id` (`check_plan_id`),
    INDEX `idx_check_record_id` (`check_record_id`),
    INDEX `idx_class_id` (`class_id`),
    INDEX `idx_grade_id` (`grade_id`),
    INDEX `idx_level_id` (`level_id`),
    INDEX `idx_period` (`period_start`, `period_end`),
    INDEX `idx_check_date` (`check_date`),
    -- 唯一约束：同一规则+同一检查记录+同一班级只能有一条DAILY结果
    UNIQUE KEY `uk_daily_result` (`rule_id`, `check_record_id`, `class_id`),
    -- 唯一约束：同一规则+同一时间段+同一班级只能有一条SUMMARY结果
    UNIQUE KEY `uk_summary_result` (`rule_id`, `period_start`, `period_end`, `class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查计划评级结果表';

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================
-- 完成
-- =====================================================
