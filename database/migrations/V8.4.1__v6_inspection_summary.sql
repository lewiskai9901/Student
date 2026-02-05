-- =============================================
-- V8.4.1: V6检查系统 - 汇总统计表
-- =============================================

-- 1. 创建日汇总表
CREATE TABLE IF NOT EXISTS inspection_daily_summaries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL COMMENT '项目ID',
    summary_date DATE NOT NULL COMMENT '汇总日期',

    -- 目标汇总
    target_type VARCHAR(20) NOT NULL COMMENT '目标类型: ORG/SPACE/USER',
    target_id BIGINT NOT NULL COMMENT '目标ID',
    target_name VARCHAR(200) COMMENT '目标名称',

    -- 归属信息
    org_unit_id BIGINT COMMENT '归属组织ID',
    org_unit_name VARCHAR(200) COMMENT '归属组织名称',
    class_id BIGINT COMMENT '归属班级ID',
    class_name VARCHAR(200) COMMENT '归属班级名称',

    -- 分数汇总
    base_score DECIMAL(6,2) DEFAULT 100.00 COMMENT '基础分',
    total_deduction DECIMAL(6,2) DEFAULT 0.00 COMMENT '总扣分',
    total_bonus DECIMAL(6,2) DEFAULT 0.00 COMMENT '总加分',
    final_score DECIMAL(6,2) COMMENT '最终分数',
    weighted_score DECIMAL(6,2) COMMENT '加权分数',

    -- 检查统计
    inspection_count INT DEFAULT 0 COMMENT '检查次数',
    deduction_count INT DEFAULT 0 COMMENT '扣分次数',
    bonus_count INT DEFAULT 0 COMMENT '加分次数',

    -- 排名
    daily_rank INT COMMENT '当日排名',
    rank_change INT DEFAULT 0 COMMENT '排名变化（与前一天比）',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_project_date (project_id, summary_date),
    INDEX idx_target (target_type, target_id),
    INDEX idx_org_unit (org_unit_id),
    INDEX idx_class (class_id),
    INDEX idx_daily_rank (project_id, summary_date, daily_rank),

    UNIQUE KEY uk_project_date_target (project_id, summary_date, target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V6每日汇总表';

-- 2. 创建周汇总表
CREATE TABLE IF NOT EXISTS inspection_weekly_summaries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL COMMENT '项目ID',
    year INT NOT NULL COMMENT '年份',
    week_number INT NOT NULL COMMENT '周数',
    start_date DATE NOT NULL COMMENT '周起始日期',
    end_date DATE NOT NULL COMMENT '周结束日期',

    -- 目标汇总
    target_type VARCHAR(20) NOT NULL COMMENT '目标类型',
    target_id BIGINT NOT NULL COMMENT '目标ID',
    target_name VARCHAR(200) COMMENT '目标名称',

    -- 归属信息
    org_unit_id BIGINT COMMENT '归属组织ID',
    class_id BIGINT COMMENT '归属班级ID',

    -- 分数汇总
    average_score DECIMAL(6,2) COMMENT '平均分',
    total_deduction DECIMAL(6,2) DEFAULT 0.00 COMMENT '总扣分',
    total_bonus DECIMAL(6,2) DEFAULT 0.00 COMMENT '总加分',

    -- 检查统计
    inspection_days INT DEFAULT 0 COMMENT '检查天数',
    perfect_days INT DEFAULT 0 COMMENT '满分天数',

    -- 排名
    weekly_rank INT COMMENT '周排名',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_project_week (project_id, year, week_number),
    INDEX idx_target (target_type, target_id),

    UNIQUE KEY uk_project_week_target (project_id, year, week_number, target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V6周汇总表';

-- 3. 创建月汇总表
CREATE TABLE IF NOT EXISTS inspection_monthly_summaries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL COMMENT '项目ID',
    year INT NOT NULL COMMENT '年份',
    month INT NOT NULL COMMENT '月份',

    -- 目标汇总
    target_type VARCHAR(20) NOT NULL COMMENT '目标类型',
    target_id BIGINT NOT NULL COMMENT '目标ID',
    target_name VARCHAR(200) COMMENT '目标名称',

    -- 归属信息
    org_unit_id BIGINT COMMENT '归属组织ID',
    class_id BIGINT COMMENT '归属班级ID',

    -- 分数汇总
    average_score DECIMAL(6,2) COMMENT '平均分',
    total_deduction DECIMAL(6,2) DEFAULT 0.00 COMMENT '总扣分',
    total_bonus DECIMAL(6,2) DEFAULT 0.00 COMMENT '总加分',

    -- 检查统计
    inspection_days INT DEFAULT 0 COMMENT '检查天数',
    perfect_days INT DEFAULT 0 COMMENT '满分天数',

    -- 排名
    monthly_rank INT COMMENT '月排名',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_project_month (project_id, year, month),
    INDEX idx_target (target_type, target_id),

    UNIQUE KEY uk_project_month_target (project_id, year, month, target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V6月汇总表';

-- 4. 创建组织汇总视图
CREATE OR REPLACE VIEW v_org_inspection_summary AS
SELECT
    ds.project_id,
    ds.summary_date,
    ds.org_unit_id,
    ds.org_unit_name,
    COUNT(DISTINCT ds.target_id) as target_count,
    AVG(ds.final_score) as avg_score,
    SUM(ds.total_deduction) as total_deduction,
    SUM(ds.total_bonus) as total_bonus,
    SUM(ds.deduction_count) as deduction_count
FROM inspection_daily_summaries ds
WHERE ds.org_unit_id IS NOT NULL
GROUP BY ds.project_id, ds.summary_date, ds.org_unit_id, ds.org_unit_name;

-- 5. 创建班级汇总视图
CREATE OR REPLACE VIEW v_class_inspection_summary AS
SELECT
    ds.project_id,
    ds.summary_date,
    ds.class_id,
    ds.class_name,
    ds.org_unit_id,
    ds.org_unit_name,
    COUNT(DISTINCT ds.target_id) as target_count,
    AVG(ds.final_score) as avg_score,
    SUM(ds.total_deduction) as total_deduction,
    SUM(ds.total_bonus) as total_bonus,
    SUM(ds.deduction_count) as deduction_count
FROM inspection_daily_summaries ds
WHERE ds.class_id IS NOT NULL
GROUP BY ds.project_id, ds.summary_date, ds.class_id, ds.class_name, ds.org_unit_id, ds.org_unit_name;
