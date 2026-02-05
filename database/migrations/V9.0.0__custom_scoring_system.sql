-- ============================================
-- V9.0.0 自定义计分系统
-- 支持自定义策略、打分方式、计算规则
-- ============================================

-- 1. 计分策略定义表
CREATE TABLE IF NOT EXISTS scoring_strategies (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- 基本信息
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '策略代码',
    name VARCHAR(100) NOT NULL COMMENT '策略名称',
    description VARCHAR(500) COMMENT '策略描述',
    category VARCHAR(50) NOT NULL DEFAULT 'basic' COMMENT '策略分类: basic/grade/advanced/time',

    -- 公式配置
    formula_template TEXT NOT NULL COMMENT 'JavaScript公式模板',
    formula_description VARCHAR(500) COMMENT '公式说明，如: 得分 = 基准分 - Σ扣分',

    -- 参数定义 (JSON Schema格式)
    parameters_schema JSON COMMENT '策略参数定义',
    default_parameters JSON COMMENT '默认参数值',

    -- 兼容性配置
    supported_input_types JSON COMMENT '支持的打分方式代码列表',
    supported_rule_types JSON COMMENT '支持的计算规则类型',

    -- 系统标记
    is_system TINYINT(1) DEFAULT 0 COMMENT '是否系统内置',
    is_enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    sort_order INT DEFAULT 0 COMMENT '排序',

    -- 审计字段
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,

    INDEX idx_scoring_strategy_category (category),
    INDEX idx_scoring_strategy_enabled (is_enabled),
    INDEX idx_scoring_strategy_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='计分策略定义表';


-- 2. 打分方式定义表
CREATE TABLE IF NOT EXISTS input_types (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- 基本信息
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '打分方式代码',
    name VARCHAR(100) NOT NULL COMMENT '打分方式名称',
    description VARCHAR(500) COMMENT '描述',
    category VARCHAR(50) DEFAULT 'basic' COMMENT '分类: basic/extended',

    -- UI组件配置
    component_type VARCHAR(50) NOT NULL COMMENT 'UI组件类型: number/select/checkbox/slider/star/grade',
    component_config JSON COMMENT '组件配置参数',

    -- 值处理配置
    value_type VARCHAR(50) DEFAULT 'number' COMMENT '值类型: number/string/boolean/array',
    value_mapping JSON COMMENT '值映射规则，如等级到分数的映射',
    validation_rules JSON COMMENT '值验证规则',

    -- 系统标记
    is_system TINYINT(1) DEFAULT 0 COMMENT '是否系统内置',
    is_enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    sort_order INT DEFAULT 0,

    -- 审计字段
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,

    INDEX idx_input_type_category (category),
    INDEX idx_input_type_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='打分方式定义表';


-- 3. 计算规则定义表
CREATE TABLE IF NOT EXISTS calculation_rules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- 基本信息
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '规则代码',
    name VARCHAR(100) NOT NULL COMMENT '规则名称',
    description VARCHAR(500) COMMENT '描述',
    rule_type VARCHAR(50) NOT NULL COMMENT '规则类型: ceiling/floor/veto/progressive/bonus/penalty',

    -- 规则配置
    condition_formula TEXT COMMENT '条件公式(JavaScript)，返回boolean',
    action_formula TEXT COMMENT '动作公式(JavaScript)，返回调整后的分数',

    -- 参数定义
    parameters_schema JSON COMMENT '规则参数定义',
    default_parameters JSON COMMENT '默认参数',

    -- 执行配置
    priority INT DEFAULT 0 COMMENT '执行优先级，数字越小越先执行',
    stop_on_match TINYINT(1) DEFAULT 0 COMMENT '匹配后是否停止后续规则',

    -- 系统标记
    is_system TINYINT(1) DEFAULT 0,
    is_enabled TINYINT(1) DEFAULT 1,

    -- 审计字段
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,

    INDEX idx_calc_rule_type (rule_type),
    INDEX idx_calc_rule_priority (priority),
    INDEX idx_calc_rule_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='计算规则定义表';


-- 4. 内置函数表
CREATE TABLE IF NOT EXISTS formula_functions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- 基本信息
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '函数名',
    description VARCHAR(500) COMMENT '函数描述',
    category VARCHAR(50) COMMENT '分类: math/array/logic/date/string',

    -- 函数定义
    parameters_def JSON COMMENT '参数定义: [{name, type, required, description}]',
    return_type VARCHAR(50) COMMENT '返回类型',
    implementation TEXT COMMENT 'JavaScript实现代码',

    -- 使用示例
    examples JSON COMMENT '使用示例: [{input, output, description}]',

    -- 系统标记
    is_system TINYINT(1) DEFAULT 1,
    is_enabled TINYINT(1) DEFAULT 1,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,

    INDEX idx_formula_func_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公式内置函数表';


-- 5. 内置变量表
CREATE TABLE IF NOT EXISTS formula_variables (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- 基本信息
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '变量名',
    description VARCHAR(500) COMMENT '变量描述',
    category VARCHAR(50) COMMENT '分类: score/count/time/context',

    -- 变量定义
    value_type VARCHAR(50) COMMENT '值类型: number/string/boolean/array/object',
    default_value TEXT COMMENT '默认值',

    -- 来源说明
    source_description VARCHAR(500) COMMENT '值来源说明',

    is_system TINYINT(1) DEFAULT 1,
    is_enabled TINYINT(1) DEFAULT 1,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,

    INDEX idx_formula_var_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公式内置变量表';


-- ============================================
-- 初始数据: 计分策略
-- ============================================

INSERT INTO scoring_strategies (code, name, description, category, formula_template, formula_description, supported_input_types, is_system, sort_order) VALUES

-- 基础策略
('DEDUCTION', '扣分制', '从基准分开始扣除，得分不低于0', 'basic',
 'Math.max(0, ctx.baseScore - sum(ctx.deductions))',
 '得分 = 基准分 - Σ扣分',
 '["NUMERIC", "OPTIONS", "CHECKBOX"]', 1, 1),

('ADDITION', '加分制', '从0分开始累加', 'basic',
 'Math.min(ctx.maxScore || 100, sum(ctx.additions))',
 '得分 = Σ加分',
 '["NUMERIC", "OPTIONS"]', 1, 2),

('BASE_ADJUST', '基准调整制', '基准分可加可减', 'basic',
 'clamp(ctx.baseScore + sum(ctx.additions) - sum(ctx.deductions), 0, ctx.maxScore || 100)',
 '得分 = 基准分 + Σ加分 - Σ扣分',
 '["NUMERIC", "OPTIONS"]', 1, 3),

('CHECKLIST', '清单制', '按完成率计算得分', 'basic',
 'Math.round(ctx.completedCount / ctx.totalCount * 100)',
 '得分 = 完成数 / 总数 × 100',
 '["CHECKBOX"]', 1, 4),

-- 等级策略
('GRADE', '等级制', 'A/B/C/D等级评定', 'grade',
 'ctx.gradeMapping[ctx.grade] || 0',
 '等级 → 预设分数',
 '["GRADE"]', 1, 10),

('STAR_RATING', '星级制', '1-5星评定', 'grade',
 'ctx.stars * 20',
 '得分 = 星数 × 20',
 '["STAR"]', 1, 11),

('PASS_FAIL', '合格制', '合格/不合格二元判定', 'grade',
 'ctx.passed ? 100 : 0',
 '合格=100, 不合格=0',
 '["CHECKBOX", "OPTIONS"]', 1, 12),

('COMMENT', '评语制', '优/良/中/差评定', 'grade',
 '({"优": 95, "良": 80, "中": 65, "差": 40})[ctx.comment] || 0',
 '优=95, 良=80, 中=65, 差=40',
 '["OPTIONS", "GRADE"]', 1, 13),

-- 高级策略
('STEPPED', '阶梯计分', '按档位分级扣分', 'advanced',
 'lookupStep(ctx.value, ctx.steps)',
 '根据值查找对应档位分数',
 '["NUMERIC", "OPTIONS"]', 1, 20),

('PROGRESSIVE', '累进计分', '按次数递增扣分', 'advanced',
 'ctx.baseDeduction * Math.pow(ctx.multiplier || 2, ctx.occurrenceCount - 1)',
 '第n次扣分 = 基础扣分 × 倍率^(n-1)',
 '["NUMERIC", "COUNT"]', 1, 21),

('MULTIPLIER', '倍率计分', '特殊时期加倍', 'advanced',
 'ctx.score * (ctx.multiplier || 1)',
 '实际分 = 基础分 × 倍率',
 '["NUMERIC"]', 1, 22),

('RANKING', '排名计分', '按名次给分', 'advanced',
 'ctx.rankingScores[ctx.rank - 1] || 0',
 '第1名=100, 第2名=95, ...',
 '["NUMERIC"]', 1, 23),

-- 时间策略
('TIME_DECAY', '时间衰减', '历史数据权重递减', 'time',
 'weightedAverage(ctx.periodScores, ctx.weights)',
 '加权平均，近期权重大',
 '["NUMERIC"]', 1, 30),

('ROLLING', '滚动计算', '取最近N期平均', 'time',
 'average(ctx.periodScores.slice(-ctx.periods))',
 '最近N期平均分',
 '["NUMERIC"]', 1, 31),

('TREND_BONUS', '趋势加成', '进步给予奖励', 'time',
 'ctx.currentScore + (ctx.currentScore > ctx.previousScore ? ctx.bonusPoints : 0)',
 '环比上升则加分',
 '["NUMERIC"]', 1, 32),

('FORCED_DIST', '强制分布', '按比例限制等级', 'time',
 'applyForcedDistribution(ctx.scores, ctx.distribution)',
 'A(10%), B(20%), C(40%), D(20%), E(10%)',
 '["GRADE"]', 1, 33);


-- ============================================
-- 初始数据: 打分方式
-- ============================================

INSERT INTO input_types (code, name, description, category, component_type, component_config, value_type, is_system, sort_order) VALUES

-- 基础打分方式
('NUMERIC', '数值输入', '直接输入具体分数', 'basic', 'number',
 '{"min": 0, "max": 100, "step": 1, "precision": 1}',
 'number', 1, 1),

('OPTIONS', '档位选择', '选择预设的分值档位', 'basic', 'select',
 '{"allowCustom": false}',
 'number', 1, 2),

('CHECKBOX', '勾选完成', '是/否判断', 'basic', 'checkbox',
 '{"checkedValue": 1, "uncheckedValue": 0}',
 'boolean', 1, 3),

-- 扩展打分方式
('COUNT', '计数输入', '输入次数或数量', 'extended', 'number',
 '{"min": 0, "max": 999, "step": 1, "unit": "次"}',
 'number', 1, 10),

('STAR', '星级点选', '1-5星评分', 'extended', 'star',
 '{"max": 5, "allowHalf": true}',
 'number', 1, 11),

('GRADE', '等级选择', 'A/B/C/D等级选择', 'extended', 'grade',
 '{"grades": ["A", "B", "C", "D", "E"], "colors": ["#52c41a", "#1890ff", "#faad14", "#ff7a45", "#ff4d4f"]}',
 'string', 1, 12),

('SLIDER', '滑块选择', '滑动选择分值', 'extended', 'slider',
 '{"min": 0, "max": 100, "step": 5, "showStops": true}',
 'number', 1, 13),

('PERCENTAGE', '百分比', '百分比输入', 'extended', 'number',
 '{"min": 0, "max": 100, "step": 1, "suffix": "%"}',
 'number', 1, 14),

('TEXT', '文本评价', '输入文字评价', 'extended', 'textarea',
 '{"maxLength": 500, "rows": 3}',
 'string', 1, 15);


-- ============================================
-- 初始数据: 计算规则
-- ============================================

INSERT INTO calculation_rules (code, name, description, rule_type, condition_formula, action_formula, parameters_schema, priority, is_system) VALUES

('CEILING', '分数封顶', '限制最高得分', 'ceiling',
 'ctx.score > ctx.maxScore',
 'ctx.maxScore',
 '{"maxScore": {"type": "number", "default": 100, "label": "最高分"}}',
 10, 1),

('FLOOR', '分数保底', '限制最低得分', 'floor',
 'ctx.score < ctx.minScore',
 'ctx.minScore',
 '{"minScore": {"type": "number", "default": 0, "label": "最低分"}}',
 20, 1),

('VETO', '一票否决', '触发条件则直接归零', 'veto',
 'ctx.hasVetoItem === true',
 '0',
 '{"vetoItems": {"type": "array", "label": "否决项"}}',
 1, 1),

('PROGRESSIVE_PENALTY', '累进惩罚', '重复违规加重处罚', 'progressive',
 'ctx.occurrenceCount > 1',
 'ctx.baseDeduction * Math.pow(ctx.multiplier, ctx.occurrenceCount - 1)',
 '{"multiplier": {"type": "number", "default": 1.5, "label": "递增倍率"}}',
 30, 1),

('CONSECUTIVE_BONUS', '连续奖励', '连续达标给予奖励', 'bonus',
 'ctx.consecutiveCount >= ctx.threshold',
 'ctx.score + ctx.bonusPoints',
 '{"threshold": {"type": "number", "default": 3, "label": "连续次数"}, "bonusPoints": {"type": "number", "default": 5, "label": "奖励分数"}}',
 40, 1),

('TIME_PENALTY', '超时惩罚', '超时则扣分', 'penalty',
 'ctx.isOverdue === true',
 'ctx.score - ctx.penaltyPoints',
 '{"penaltyPoints": {"type": "number", "default": 10, "label": "超时扣分"}}',
 50, 1);


-- ============================================
-- 初始数据: 内置函数
-- ============================================

INSERT INTO formula_functions (name, description, category, parameters_def, return_type, implementation, examples, is_system) VALUES

-- 数学函数
('sum', '求和', 'math',
 '[{"name": "values", "type": "array", "required": true, "description": "数值数组"}]',
 'number',
 'function sum(arr) { return Array.isArray(arr) ? arr.reduce((a, b) => a + (Number(b) || 0), 0) : 0; }',
 '[{"input": "sum([1, 2, 3])", "output": "6"}]',
 1),

('average', '平均值', 'math',
 '[{"name": "values", "type": "array", "required": true}]',
 'number',
 'function average(arr) { return arr && arr.length ? arr.reduce((a, b) => a + b, 0) / arr.length : 0; }',
 '[{"input": "average([60, 70, 80])", "output": "70"}]',
 1),

('max', '最大值', 'math',
 '[{"name": "values", "type": "array", "required": true}]',
 'number',
 'function max(arr) { return arr && arr.length ? Math.max(...arr) : 0; }',
 '[{"input": "max([1, 5, 3])", "output": "5"}]',
 1),

('min', '最小值', 'math',
 '[{"name": "values", "type": "array", "required": true}]',
 'number',
 'function min(arr) { return arr && arr.length ? Math.min(...arr) : 0; }',
 '[{"input": "min([1, 5, 3])", "output": "1"}]',
 1),

('clamp', '限制范围', 'math',
 '[{"name": "value", "type": "number"}, {"name": "min", "type": "number"}, {"name": "max", "type": "number"}]',
 'number',
 'function clamp(value, min, max) { return Math.min(Math.max(value || 0, min || 0), max || 100); }',
 '[{"input": "clamp(150, 0, 100)", "output": "100"}]',
 1),

('round', '四舍五入', 'math',
 '[{"name": "value", "type": "number"}, {"name": "decimals", "type": "number", "default": 0}]',
 'number',
 'function round(value, decimals) { const m = Math.pow(10, decimals || 0); return Math.round((value || 0) * m) / m; }',
 '[{"input": "round(3.456, 2)", "output": "3.46"}]',
 1),

-- 数组函数
('count', '计数', 'array',
 '[{"name": "values", "type": "array"}]',
 'number',
 'function count(arr) { return Array.isArray(arr) ? arr.length : 0; }',
 '[{"input": "count([1, 2, 3])", "output": "3"}]',
 1),

('filter', '过滤', 'array',
 '[{"name": "values", "type": "array"}, {"name": "condition", "type": "function"}]',
 'array',
 'function filter(arr, fn) { return Array.isArray(arr) ? arr.filter(fn) : []; }',
 '[{"input": "filter([1, 2, 3], x => x > 1)", "output": "[2, 3]"}]',
 1),

-- 逻辑函数
('iif', '条件判断', 'logic',
 '[{"name": "condition", "type": "boolean"}, {"name": "trueValue", "type": "any"}, {"name": "falseValue", "type": "any"}]',
 'any',
 'function iif(cond, t, f) { return cond ? t : f; }',
 '[{"input": "iif(true, 100, 0)", "output": "100"}]',
 1),

-- 查找函数
('lookupStep', '阶梯查找', 'lookup',
 '[{"name": "value", "type": "number"}, {"name": "steps", "type": "array"}]',
 'number',
 'function lookupStep(value, steps) { if (!steps || !steps.length) return 0; const sorted = [...steps].sort((a,b) => b.threshold - a.threshold); for (let s of sorted) { if (value >= s.threshold) return s.score; } return 0; }',
 '[{"input": "lookupStep(85, [{threshold: 90, score: 100}, {threshold: 80, score: 90}])", "output": "90"}]',
 1),

('weightedAverage', '加权平均', 'math',
 '[{"name": "values", "type": "array"}, {"name": "weights", "type": "array"}]',
 'number',
 'function weightedAverage(values, weights) { if (!values || !values.length) return 0; let sum = 0, wSum = 0; for (let i = 0; i < values.length; i++) { const w = weights && weights[i] ? weights[i] : 1; sum += values[i] * w; wSum += w; } return wSum ? sum / wSum : 0; }',
 '[{"input": "weightedAverage([80, 90], [0.4, 0.6])", "output": "86"}]',
 1);


-- ============================================
-- 初始数据: 内置变量
-- ============================================

INSERT INTO formula_variables (name, description, category, value_type, source_description, is_system) VALUES

-- 分数相关
('baseScore', '基准分', 'score', 'number', '模板设置的基准分数', 1),
('maxScore', '最高分', 'score', 'number', '允许的最高分数', 1),
('minScore', '最低分', 'score', 'number', '允许的最低分数', 1),
('score', '当前得分', 'score', 'number', '计算中的当前分数', 1),
('deductions', '扣分列表', 'score', 'array', '所有扣分项的分值数组', 1),
('additions', '加分列表', 'score', 'array', '所有加分项的分值数组', 1),

-- 计数相关
('totalCount', '总项数', 'count', 'number', '检查项总数', 1),
('completedCount', '完成数', 'count', 'number', '已完成/已检查的项数', 1),
('occurrenceCount', '发生次数', 'count', 'number', '同一问题的发生次数', 1),
('consecutiveCount', '连续次数', 'count', 'number', '连续达标/违规的次数', 1),

-- 输入值
('value', '输入值', 'input', 'any', '当前检查项的输入值', 1),
('grade', '等级', 'input', 'string', '等级评定值: A/B/C/D', 1),
('stars', '星级', 'input', 'number', '星级评分: 1-5', 1),
('passed', '是否合格', 'input', 'boolean', '合格判定结果', 1),
('comment', '评语', 'input', 'string', '评语: 优/良/中/差', 1),

-- 时间相关
('currentPeriod', '当前周期', 'time', 'string', '当前评分周期', 1),
('periodScores', '周期得分', 'time', 'array', '历史周期得分数组', 1),
('previousScore', '上期得分', 'time', 'number', '上一周期的得分', 1),
('currentScore', '本期得分', 'time', 'number', '当前周期的得分', 1),
('isOverdue', '是否超时', 'time', 'boolean', '是否超过截止时间', 1),

-- 上下文
('rank', '排名', 'context', 'number', '当前排名位置', 1),
('hasVetoItem', '有否决项', 'context', 'boolean', '是否触发一票否决', 1),
('multiplier', '倍率', 'context', 'number', '当前适用的倍率', 1);
