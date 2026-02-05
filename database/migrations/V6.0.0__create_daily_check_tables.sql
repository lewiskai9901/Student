-- V6.0.0: Create daily_check_categories and daily_check_targets tables
-- These tables are required for the scoring page to function properly

-- Check if daily_checks table exists, if not create it
CREATE TABLE IF NOT EXISTS daily_checks (
    id BIGINT PRIMARY KEY,
    plan_id BIGINT COMMENT '检查计划ID',
    check_date DATE NOT NULL COMMENT '检查日期',
    check_name VARCHAR(200) NOT NULL COMMENT '检查名称',
    template_id BIGINT COMMENT '模板ID',
    check_type INT DEFAULT 1 COMMENT '检查类型: 1-日常, 2-周检, 3-月检',
    total_rounds INT DEFAULT 1 COMMENT '总轮次',
    round_names JSON COMMENT '轮次名称配置',
    status INT DEFAULT 0 COMMENT '状态: 0-未开始, 1-进行中, 2-已完成',
    record_generated TINYINT DEFAULT 0 COMMENT '是否已生成记录',
    record_id BIGINT COMMENT '关联的检查记录ID',
    description TEXT COMMENT '检查描述',
    excluded_targets JSON COMMENT '排除的目标',
    checker_id BIGINT COMMENT '检查员ID',
    checker_name VARCHAR(100) COMMENT '检查员姓名',
    weight_config_id BIGINT COMMENT '加权配置ID',
    enable_weight TINYINT DEFAULT 0 COMMENT '是否启用加权',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_plan_id (plan_id),
    INDEX idx_check_date (check_date),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日常检查表';

-- Create daily_check_categories table
CREATE TABLE IF NOT EXISTS daily_check_categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    check_id BIGINT NOT NULL COMMENT '日常检查ID',
    category_id BIGINT NOT NULL COMMENT '类别ID(关联deduction_types)',
    category_name VARCHAR(100) NOT NULL COMMENT '类别名称',
    participated_rounds VARCHAR(100) DEFAULT '1' COMMENT '参与的轮次(逗号分隔)',
    check_rounds VARCHAR(100) DEFAULT '1' COMMENT '检查轮次配置',
    sort_order INT DEFAULT 0 COMMENT '排序',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_check_id (check_id),
    INDEX idx_category_id (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日常检查类别关联表';

-- Create daily_check_targets table
CREATE TABLE IF NOT EXISTS daily_check_targets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    check_id BIGINT NOT NULL COMMENT '日常检查ID',
    target_type INT DEFAULT 1 COMMENT '目标类型: 1-班级, 2-宿舍',
    target_id BIGINT NOT NULL COMMENT '目标ID(班级ID或宿舍ID)',
    target_name VARCHAR(100) COMMENT '目标名称',
    sort_order INT DEFAULT 0 COMMENT '排序',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_check_id (check_id),
    INDEX idx_target_id (target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日常检查目标关联表';
