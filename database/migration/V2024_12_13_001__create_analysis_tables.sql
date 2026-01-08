-- =====================================================
-- 统计分析系统表结构
-- 创建日期: 2024-12-13
-- 描述: 支持高度可配置的统计分析功能
-- =====================================================

-- 1. 分析配置主表
CREATE TABLE IF NOT EXISTS analysis_configs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    plan_id BIGINT NOT NULL COMMENT '所属检查计划ID',
    config_name VARCHAR(100) NOT NULL COMMENT '配置名称',
    config_desc VARCHAR(500) COMMENT '配置描述',

    -- 范围配置
    scope_type VARCHAR(20) DEFAULT 'time' COMMENT '范围类型: time-时间范围, record-记录选择, mixed-混合',
    scope_config JSON COMMENT '范围配置JSON',

    -- 目标配置
    target_type VARCHAR(20) DEFAULT 'all' COMMENT '目标类型: all-全部, department-部门, grade-年级, custom-自定义',
    target_config JSON COMMENT '目标配置JSON',

    -- 更新模式
    update_mode VARCHAR(20) DEFAULT 'static' COMMENT '更新模式: static-静态, dynamic-动态',
    auto_refresh TINYINT(1) DEFAULT 0 COMMENT '是否自动刷新',
    refresh_interval INT COMMENT '刷新间隔(分钟)',
    last_refresh_time DATETIME COMMENT '上次刷新时间',

    -- 缺检处理策略
    missing_strategy VARCHAR(20) DEFAULT 'avg' COMMENT '缺检策略: avg-平均扣分, weighted-加权平均, full_only-仅全覆盖, penalty-缺检惩罚, exempt-缺检豁免',

    -- 显示配置
    layout_config JSON COMMENT '布局配置JSON',

    -- 通用字段
    is_enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    is_public TINYINT(1) DEFAULT 0 COMMENT '是否公开',
    is_default TINYINT(1) DEFAULT 0 COMMENT '是否默认配置',
    sort_order INT DEFAULT 0 COMMENT '排序序号',
    creator_id BIGINT COMMENT '创建者ID',
    creator_name VARCHAR(50) COMMENT '创建者姓名',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) DEFAULT 0 COMMENT '是否删除',

    INDEX idx_plan_id (plan_id),
    INDEX idx_creator (creator_id),
    INDEX idx_enabled (is_enabled, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='统计分析配置表';

-- 2. 统计指标配置表
CREATE TABLE IF NOT EXISTS analysis_metrics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    config_id BIGINT NOT NULL COMMENT '分析配置ID',
    metric_code VARCHAR(50) NOT NULL COMMENT '指标编码',
    metric_name VARCHAR(100) NOT NULL COMMENT '指标名称',
    metric_desc VARCHAR(500) COMMENT '指标描述',

    -- 指标类型
    metric_type VARCHAR(30) NOT NULL COMMENT '指标类型: total_score-总扣分, avg_score-平均扣分, check_count-检查次数, coverage_rate-覆盖率, item_count-扣分项次, person_count-人次, weighted_score-加权扣分, ranking-排名, trend-趋势, distribution-分布, custom-自定义',

    -- 数据来源
    source_type VARCHAR(20) DEFAULT 'all' COMMENT '数据来源: all-全部类别, category-指定类别, item-指定扣分项',
    source_category_ids JSON COMMENT '来源类别ID列表',
    source_item_ids JSON COMMENT '来源扣分项ID列表',

    -- 聚合配置
    aggregation VARCHAR(20) DEFAULT 'sum' COMMENT '聚合方式: sum-求和, avg-平均, max-最大, min-最小, count-计数',
    group_by VARCHAR(30) COMMENT '分组维度: class-班级, grade-年级, department-部门, category-类别, date-日期',

    -- 自定义公式
    custom_formula VARCHAR(500) COMMENT '自定义计算公式',

    -- 显示配置
    display_format VARCHAR(50) DEFAULT '{value}' COMMENT '显示格式',
    decimal_places INT DEFAULT 2 COMMENT '小数位数',
    unit VARCHAR(20) COMMENT '单位',

    -- 图表配置
    chart_type VARCHAR(20) DEFAULT 'number' COMMENT '图表类型: number-数字卡片, bar-柱状图, line-折线图, pie-饼图, table-表格, rank-排行榜',
    chart_config JSON COMMENT '图表配置JSON',

    -- 排序配置
    sort_field VARCHAR(50) COMMENT '排序字段',
    sort_order VARCHAR(10) DEFAULT 'asc' COMMENT '排序方向: asc-升序, desc-降序',
    top_n INT COMMENT '只显示前N条',

    -- 高亮配置
    highlight_rules JSON COMMENT '高亮规则JSON',

    -- 通用字段
    is_enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    display_order INT DEFAULT 0 COMMENT '显示顺序',
    grid_position JSON COMMENT '网格位置配置',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_config_id (config_id),
    INDEX idx_metric_type (metric_type),
    CONSTRAINT fk_metric_config FOREIGN KEY (config_id) REFERENCES analysis_configs(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='统计指标配置表';

-- 3. 分析结果快照表
CREATE TABLE IF NOT EXISTS analysis_snapshots (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    config_id BIGINT NOT NULL COMMENT '分析配置ID',
    snapshot_name VARCHAR(100) COMMENT '快照名称',
    snapshot_desc VARCHAR(500) COMMENT '快照描述',

    -- 快照范围信息
    record_ids JSON COMMENT '纳入统计的记录ID列表',
    class_ids JSON COMMENT '涉及的班级ID列表',
    date_range_start DATE COMMENT '数据起始日期',
    date_range_end DATE COMMENT '数据截止日期',

    -- 统计摘要
    record_count INT DEFAULT 0 COMMENT '检查记录数',
    class_count INT DEFAULT 0 COMMENT '涉及班级数',
    total_score DECIMAL(12,2) DEFAULT 0 COMMENT '总扣分',
    avg_score DECIMAL(10,2) DEFAULT 0 COMMENT '平均扣分',

    -- 快照数据
    overview_data JSON COMMENT '概览数据JSON',
    metrics_data JSON COMMENT '各指标计算结果JSON',

    -- 元数据
    generated_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
    generated_by BIGINT COMMENT '生成者ID',
    generated_by_name VARCHAR(50) COMMENT '生成者姓名',
    is_auto TINYINT(1) DEFAULT 0 COMMENT '是否自动生成',
    version INT DEFAULT 1 COMMENT '版本号',

    INDEX idx_config_id (config_id),
    INDEX idx_generated_at (generated_at),
    CONSTRAINT fk_snapshot_config FOREIGN KEY (config_id) REFERENCES analysis_configs(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分析结果快照表';

-- 4. 类别映射表 (解决不同模板类别不一致问题)
CREATE TABLE IF NOT EXISTS category_mappings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    plan_id BIGINT NOT NULL COMMENT '检查计划ID',
    unified_code VARCHAR(50) NOT NULL COMMENT '统一类别编码',
    unified_name VARCHAR(100) NOT NULL COMMENT '统一类别名称',
    unified_type VARCHAR(30) COMMENT '统一类别类型: HYGIENE-卫生, DISCIPLINE-纪律, ATTENDANCE-考勤, DORMITORY-宿舍, OTHER-其他',

    -- 映射配置
    source_category_ids JSON NOT NULL COMMENT '源类别ID列表',
    source_category_names JSON COMMENT '源类别名称列表(冗余)',

    -- 显示配置
    color VARCHAR(20) COMMENT '显示颜色',
    icon VARCHAR(50) COMMENT '图标',
    sort_order INT DEFAULT 0 COMMENT '排序序号',

    -- 通用字段
    is_enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_plan_id (plan_id),
    UNIQUE KEY uk_plan_code (plan_id, unified_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='类别映射表';

-- 5. 分析配置模板表 (预设模板，方便快速创建)
CREATE TABLE IF NOT EXISTS analysis_config_templates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    template_name VARCHAR(100) NOT NULL COMMENT '模板名称',
    template_desc VARCHAR(500) COMMENT '模板描述',
    template_type VARCHAR(30) COMMENT '模板类型: system-系统预设, custom-用户自定义',

    -- 配置内容 (与analysis_configs结构对应)
    scope_type VARCHAR(20) DEFAULT 'time',
    scope_config JSON,
    target_type VARCHAR(20) DEFAULT 'all',
    target_config JSON,
    update_mode VARCHAR(20) DEFAULT 'static',
    missing_strategy VARCHAR(20) DEFAULT 'avg',
    layout_config JSON,

    -- 指标配置列表
    metrics_config JSON COMMENT '指标配置列表JSON',

    -- 通用字段
    is_enabled TINYINT(1) DEFAULT 1,
    sort_order INT DEFAULT 0,
    creator_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_template_type (template_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分析配置模板表';

-- 6. 插入系统预设模板
INSERT INTO analysis_config_templates (template_name, template_desc, template_type, scope_type, target_type, update_mode, missing_strategy, metrics_config, is_enabled, sort_order) VALUES
('班级排名统计', '统计各班级的扣分情况并排名，适用于学期汇总', 'system', 'time', 'all', 'static', 'avg',
 '[{"metricCode":"overview","metricName":"统计概览","metricType":"overview","chartType":"number"},{"metricCode":"class_ranking","metricName":"班级排名","metricType":"ranking","sourceType":"all","aggregation":"sum","chartType":"rank","sortOrder":"asc","topN":20},{"metricCode":"category_dist","metricName":"类别分布","metricType":"distribution","sourceType":"all","groupBy":"category","chartType":"pie"},{"metricCode":"trend","metricName":"扣分趋势","metricType":"trend","sourceType":"all","groupBy":"date","chartType":"line"}]',
 1, 1),

('动态监控面板', '实时监控检查情况，自动更新数据', 'system', 'time', 'all', 'dynamic', 'avg',
 '[{"metricCode":"overview","metricName":"实时概览","metricType":"overview","chartType":"number"},{"metricCode":"recent_ranking","metricName":"近期排名","metricType":"ranking","sourceType":"all","chartType":"rank","topN":10},{"metricCode":"daily_trend","metricName":"每日趋势","metricType":"trend","groupBy":"date","chartType":"line"}]',
 1, 2),

('卫生检查专项', '专门统计卫生类别的检查情况', 'system', 'time', 'all', 'static', 'avg',
 '[{"metricCode":"hygiene_total","metricName":"卫生总扣分","metricType":"total_score","sourceType":"category","chartType":"number"},{"metricCode":"hygiene_ranking","metricName":"卫生排名","metricType":"ranking","sourceType":"category","chartType":"rank"},{"metricCode":"hygiene_items","metricName":"卫生扣分项统计","metricType":"item_count","sourceType":"category","chartType":"bar"}]',
 1, 3),

('部门对比分析', '按部门汇总统计，用于部门间对比', 'system', 'time', 'all', 'static', 'avg',
 '[{"metricCode":"dept_overview","metricName":"部门概览","metricType":"overview","groupBy":"department","chartType":"number"},{"metricCode":"dept_ranking","metricName":"部门排名","metricType":"ranking","groupBy":"department","chartType":"bar"},{"metricCode":"dept_category","metricName":"部门类别分布","metricType":"distribution","groupBy":"department","chartType":"pie"}]',
 1, 4);
