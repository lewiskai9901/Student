# 自定义计分策略系统设计

## 一、系统概述

### 1.1 设计目标

将硬编码的计分策略转变为**数据驱动**的配置系统，支持：
- 自定义计分策略（公式可配置）
- 自定义打分方式（UI组件可配置）
- 自定义计算规则（规则链可配置）
- JavaScript公式引擎（最大灵活性）

### 1.2 核心概念

```
┌─────────────────────────────────────────────────────────────────┐
│                        概念关系图                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  检查模板 (Template)                                            │
│      │                                                          │
│      ├── 使用 → 计分策略 (Scoring Strategy)                     │
│      │              │                                           │
│      │              └── 包含 → 公式模板 (Formula)               │
│      │                          │                               │
│      │                          └── 引用 → 内置函数/变量        │
│      │                                                          │
│      └── 包含 → 检查项 (Item)                                   │
│                   │                                             │
│                   ├── 使用 → 打分方式 (Input Type)              │
│                   │              │                              │
│                   │              └── 渲染 → UI组件              │
│                   │                                             │
│                   └── 应用 → 计算规则 (Calculation Rules)       │
│                                  │                              │
│                                  └── 封顶/保底/累进/否决        │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 1.3 系统架构

```
┌─────────────────────────────────────────────────────────────────┐
│                         前端层                                   │
├─────────────────────────────────────────────────────────────────┤
│  策略管理UI    打分方式管理UI    规则管理UI    公式编辑器        │
│       │              │              │              │            │
│       └──────────────┴──────────────┴──────────────┘            │
│                              │                                   │
│                      API 调用层                                  │
└─────────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                         后端层                                   │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │ 策略服务    │  │ 打分方式服务 │  │ 规则服务    │             │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
│          │                │                │                    │
│          └────────────────┴────────────────┘                    │
│                           │                                     │
│                  ┌────────┴────────┐                           │
│                  │   公式引擎      │                           │
│                  │  (GraalJS)      │                           │
│                  └─────────────────┘                           │
└─────────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                        数据库层                                  │
├─────────────────────────────────────────────────────────────────┤
│  scoring_strategies │ input_types │ calculation_rules │ ...    │
└─────────────────────────────────────────────────────────────────┘
```

---

## 二、数据库设计

### 2.1 计分策略表 (scoring_strategies)

```sql
CREATE TABLE scoring_strategies (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- 基本信息
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '策略代码',
    name VARCHAR(100) NOT NULL COMMENT '策略名称',
    description VARCHAR(500) COMMENT '策略描述',
    category VARCHAR(50) NOT NULL COMMENT '策略分类: basic/grade/advanced/time',

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
    is_system BOOLEAN DEFAULT FALSE COMMENT '是否系统内置',
    is_enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    sort_order INT DEFAULT 0 COMMENT '排序',

    -- 审计字段
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,

    INDEX idx_category (category),
    INDEX idx_enabled (is_enabled)
) COMMENT '计分策略定义表';
```

### 2.2 打分方式表 (input_types)

```sql
CREATE TABLE input_types (
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
    is_system BOOLEAN DEFAULT FALSE COMMENT '是否系统内置',
    is_enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    sort_order INT DEFAULT 0,

    -- 审计字段
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) COMMENT '打分方式定义表';
```

### 2.3 计算规则表 (calculation_rules)

```sql
CREATE TABLE calculation_rules (
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
    stop_on_match BOOLEAN DEFAULT FALSE COMMENT '匹配后是否停止后续规则',

    -- 系统标记
    is_system BOOLEAN DEFAULT FALSE,
    is_enabled BOOLEAN DEFAULT TRUE,

    -- 审计字段
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) COMMENT '计算规则定义表';
```

### 2.4 内置函数表 (formula_functions)

```sql
CREATE TABLE formula_functions (
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
    is_system BOOLEAN DEFAULT TRUE,
    is_enabled BOOLEAN DEFAULT TRUE,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) COMMENT '公式内置函数表';
```

### 2.5 内置变量表 (formula_variables)

```sql
CREATE TABLE formula_variables (
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

    is_system BOOLEAN DEFAULT TRUE,
    is_enabled BOOLEAN DEFAULT TRUE,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) COMMENT '公式内置变量表';
```

### 2.6 初始数据

```sql
-- ============================================
-- 计分策略初始数据
-- ============================================

INSERT INTO scoring_strategies (code, name, description, category, formula_template, formula_description, supported_input_types, is_system, sort_order) VALUES

-- 基础策略
('DEDUCTION', '扣分制', '从基准分开始扣除，得分不低于0', 'basic',
 'Math.max(0, ctx.baseScore - sum(ctx.deductions))',
 '得分 = 基准分 - Σ扣分',
 '["NUMERIC", "OPTIONS", "CHECKBOX"]', TRUE, 1),

('ADDITION', '加分制', '从0分开始累加', 'basic',
 'Math.min(ctx.maxScore || 100, sum(ctx.additions))',
 '得分 = Σ加分',
 '["NUMERIC", "OPTIONS"]', TRUE, 2),

('BASE_ADJUST', '基准调整制', '基准分可加可减', 'basic',
 'clamp(ctx.baseScore + sum(ctx.additions) - sum(ctx.deductions), 0, ctx.maxScore || 100)',
 '得分 = 基准分 + Σ加分 - Σ扣分',
 '["NUMERIC", "OPTIONS"]', TRUE, 3),

('CHECKLIST', '清单制', '按完成率计算得分', 'basic',
 'Math.round(ctx.completedCount / ctx.totalCount * 100)',
 '得分 = 完成数 / 总数 × 100',
 '["CHECKBOX"]', TRUE, 4),

-- 等级策略
('GRADE', '等级制', 'A/B/C/D等级评定', 'grade',
 'ctx.gradeMapping[ctx.grade] || 0',
 '等级 → 预设分数',
 '["GRADE"]', TRUE, 10),

('STAR_RATING', '星级制', '1-5星评定', 'grade',
 'ctx.stars * 20',
 '得分 = 星数 × 20',
 '["STAR"]', TRUE, 11),

('PASS_FAIL', '合格制', '合格/不合格二元判定', 'grade',
 'ctx.passed ? 100 : 0',
 '合格=100, 不合格=0',
 '["CHECKBOX", "OPTIONS"]', TRUE, 12),

('COMMENT', '评语制', '优/良/中/差评定', 'grade',
 '{"优": 95, "良": 80, "中": 65, "差": 40}[ctx.comment] || 0',
 '优=95, 良=80, 中=65, 差=40',
 '["OPTIONS", "GRADE"]', TRUE, 13),

-- 高级策略
('STEPPED', '阶梯计分', '按档位分级扣分', 'advanced',
 'lookupStep(ctx.value, ctx.steps)',
 '根据值查找对应档位分数',
 '["NUMERIC", "OPTIONS"]', TRUE, 20),

('PROGRESSIVE', '累进计分', '按次数递增扣分', 'advanced',
 'ctx.baseDeduction * Math.pow(ctx.multiplier || 2, ctx.occurrenceCount - 1)',
 '第n次扣分 = 基础扣分 × 倍率^(n-1)',
 '["NUMERIC", "COUNT"]', TRUE, 21),

('MULTIPLIER', '倍率计分', '特殊时期加倍', 'advanced',
 'ctx.score * (ctx.multiplier || 1)',
 '实际分 = 基础分 × 倍率',
 '["NUMERIC"]', TRUE, 22),

('RANKING', '排名计分', '按名次给分', 'advanced',
 'ctx.rankingScores[ctx.rank - 1] || 0',
 '第1名=100, 第2名=95, ...',
 '["NUMERIC"]', TRUE, 23),

-- 时间策略
('TIME_DECAY', '时间衰减', '历史数据权重递减', 'time',
 'weightedAverage(ctx.periodScores, ctx.weights)',
 '加权平均，近期权重大',
 '["NUMERIC"]', TRUE, 30),

('ROLLING', '滚动计算', '取最近N期平均', 'time',
 'average(ctx.periodScores.slice(-ctx.periods))',
 '最近N期平均分',
 '["NUMERIC"]', TRUE, 31),

('TREND_BONUS', '趋势加成', '进步给予奖励', 'time',
 'ctx.currentScore + (ctx.currentScore > ctx.previousScore ? ctx.bonusPoints : 0)',
 '环比上升则加分',
 '["NUMERIC"]', TRUE, 32),

('FORCED_DIST', '强制分布', '按比例限制等级', 'time',
 'applyForcedDistribution(ctx.scores, ctx.distribution)',
 'A(10%), B(20%), C(40%), D(20%), E(10%)',
 '["GRADE"]', TRUE, 33);


-- ============================================
-- 打分方式初始数据
-- ============================================

INSERT INTO input_types (code, name, description, category, component_type, component_config, value_type, is_system, sort_order) VALUES

-- 基础打分方式
('NUMERIC', '数值输入', '直接输入具体分数', 'basic', 'number',
 '{"min": 0, "max": 100, "step": 1, "precision": 1}',
 'number', TRUE, 1),

('OPTIONS', '档位选择', '选择预设的分值档位', 'basic', 'select',
 '{"allowCustom": false}',
 'number', TRUE, 2),

('CHECKBOX', '勾选完成', '是/否判断', 'basic', 'checkbox',
 '{"checkedValue": 1, "uncheckedValue": 0}',
 'boolean', TRUE, 3),

-- 扩展打分方式
('COUNT', '计数输入', '输入次数或数量', 'extended', 'number',
 '{"min": 0, "max": 999, "step": 1, "unit": "次"}',
 'number', TRUE, 10),

('STAR', '星级点选', '1-5星评分', 'extended', 'star',
 '{"max": 5, "allowHalf": true}',
 'number', TRUE, 11),

('GRADE', '等级选择', 'A/B/C/D等级选择', 'extended', 'grade',
 '{"grades": ["A", "B", "C", "D", "E"], "colors": ["#52c41a", "#1890ff", "#faad14", "#ff7a45", "#ff4d4f"]}',
 'string', TRUE, 12),

('SLIDER', '滑块选择', '滑动选择分值', 'extended', 'slider',
 '{"min": 0, "max": 100, "step": 5, "showStops": true}',
 'number', TRUE, 13),

('PERCENTAGE', '百分比', '百分比输入', 'extended', 'number',
 '{"min": 0, "max": 100, "step": 1, "suffix": "%"}',
 'number', TRUE, 14),

('TEXT', '文本评价', '输入文字评价', 'extended', 'textarea',
 '{"maxLength": 500, "rows": 3}',
 'string', TRUE, 15);


-- ============================================
-- 计算规则初始数据
-- ============================================

INSERT INTO calculation_rules (code, name, description, rule_type, condition_formula, action_formula, parameters_schema, priority, is_system) VALUES

('CEILING', '分数封顶', '限制最高得分', 'ceiling',
 'ctx.score > ctx.maxScore',
 'ctx.maxScore',
 '{"maxScore": {"type": "number", "default": 100, "label": "最高分"}}',
 10, TRUE),

('FLOOR', '分数保底', '限制最低得分', 'floor',
 'ctx.score < ctx.minScore',
 'ctx.minScore',
 '{"minScore": {"type": "number", "default": 0, "label": "最低分"}}',
 20, TRUE),

('VETO', '一票否决', '触发条件则直接归零', 'veto',
 'ctx.hasVetoItem === true',
 '0',
 '{"vetoItems": {"type": "array", "label": "否决项"}}',
 1, TRUE),

('PROGRESSIVE_PENALTY', '累进惩罚', '重复违规加重处罚', 'progressive',
 'ctx.occurrenceCount > 1',
 'ctx.baseDeduction * Math.pow(ctx.multiplier, ctx.occurrenceCount - 1)',
 '{"multiplier": {"type": "number", "default": 1.5, "label": "递增倍率"}}',
 30, TRUE),

('CONSECUTIVE_BONUS', '连续奖励', '连续达标给予奖励', 'bonus',
 'ctx.consecutiveCount >= ctx.threshold',
 'ctx.score + ctx.bonusPoints',
 '{"threshold": {"type": "number", "default": 3, "label": "连续次数"}, "bonusPoints": {"type": "number", "default": 5, "label": "奖励分数"}}',
 40, TRUE),

('TIME_PENALTY', '超时惩罚', '超时则扣分', 'penalty',
 'ctx.isOverdue === true',
 'ctx.score - ctx.penaltyPoints',
 '{"penaltyPoints": {"type": "number", "default": 10, "label": "超时扣分"}}',
 50, TRUE);


-- ============================================
-- 内置函数初始数据
-- ============================================

INSERT INTO formula_functions (name, description, category, parameters_def, return_type, implementation, examples, is_system) VALUES

-- 数学函数
('sum', '求和', 'math',
 '[{"name": "values", "type": "array", "required": true, "description": "数值数组"}]',
 'number',
 'function sum(arr) { return arr.reduce((a, b) => a + b, 0); }',
 '[{"input": "sum([1, 2, 3])", "output": "6"}]',
 TRUE),

('average', '平均值', 'math',
 '[{"name": "values", "type": "array", "required": true}]',
 'number',
 'function average(arr) { return arr.length ? arr.reduce((a, b) => a + b, 0) / arr.length : 0; }',
 '[{"input": "average([60, 70, 80])", "output": "70"}]',
 TRUE),

('max', '最大值', 'math',
 '[{"name": "values", "type": "array", "required": true}]',
 'number',
 'function max(arr) { return Math.max(...arr); }',
 '[{"input": "max([1, 5, 3])", "output": "5"}]',
 TRUE),

('min', '最小值', 'math',
 '[{"name": "values", "type": "array", "required": true}]',
 'number',
 'function min(arr) { return Math.min(...arr); }',
 '[{"input": "min([1, 5, 3])", "output": "1"}]',
 TRUE),

('clamp', '限制范围', 'math',
 '[{"name": "value", "type": "number"}, {"name": "min", "type": "number"}, {"name": "max", "type": "number"}]',
 'number',
 'function clamp(value, min, max) { return Math.min(Math.max(value, min), max); }',
 '[{"input": "clamp(150, 0, 100)", "output": "100"}]',
 TRUE),

('round', '四舍五入', 'math',
 '[{"name": "value", "type": "number"}, {"name": "decimals", "type": "number", "default": 0}]',
 'number',
 'function round(value, decimals = 0) { const m = Math.pow(10, decimals); return Math.round(value * m) / m; }',
 '[{"input": "round(3.456, 2)", "output": "3.46"}]',
 TRUE),

-- 数组函数
('count', '计数', 'array',
 '[{"name": "values", "type": "array"}]',
 'number',
 'function count(arr) { return arr.length; }',
 '[{"input": "count([1, 2, 3])", "output": "3"}]',
 TRUE),

('filter', '过滤', 'array',
 '[{"name": "values", "type": "array"}, {"name": "condition", "type": "function"}]',
 'array',
 'function filter(arr, fn) { return arr.filter(fn); }',
 '[{"input": "filter([1, 2, 3], x => x > 1)", "output": "[2, 3]"}]',
 TRUE),

-- 逻辑函数
('iif', '条件判断', 'logic',
 '[{"name": "condition", "type": "boolean"}, {"name": "trueValue", "type": "any"}, {"name": "falseValue", "type": "any"}]',
 'any',
 'function iif(cond, t, f) { return cond ? t : f; }',
 '[{"input": "iif(true, 100, 0)", "output": "100"}]',
 TRUE),

-- 查找函数
('lookupStep', '阶梯查找', 'lookup',
 '[{"name": "value", "type": "number"}, {"name": "steps", "type": "array"}]',
 'number',
 'function lookupStep(value, steps) { for (let s of steps.sort((a,b) => b.threshold - a.threshold)) { if (value >= s.threshold) return s.score; } return 0; }',
 '[{"input": "lookupStep(85, [{threshold: 90, score: 100}, {threshold: 80, score: 90}])", "output": "90"}]',
 TRUE),

('weightedAverage', '加权平均', 'math',
 '[{"name": "values", "type": "array"}, {"name": "weights", "type": "array"}]',
 'number',
 'function weightedAverage(values, weights) { let sum = 0, wSum = 0; for (let i = 0; i < values.length; i++) { sum += values[i] * (weights[i] || 1); wSum += weights[i] || 1; } return wSum ? sum / wSum : 0; }',
 '[{"input": "weightedAverage([80, 90], [0.4, 0.6])", "output": "86"}]',
 TRUE);


-- ============================================
-- 内置变量初始数据
-- ============================================

INSERT INTO formula_variables (name, description, category, value_type, source_description, is_system) VALUES

-- 分数相关
('baseScore', '基准分', 'score', 'number', '模板设置的基准分数', TRUE),
('maxScore', '最高分', 'score', 'number', '允许的最高分数', TRUE),
('minScore', '最低分', 'score', 'number', '允许的最低分数', TRUE),
('score', '当前得分', 'score', 'number', '计算中的当前分数', TRUE),
('deductions', '扣分列表', 'score', 'array', '所有扣分项的分值数组', TRUE),
('additions', '加分列表', 'score', 'array', '所有加分项的分值数组', TRUE),

-- 计数相关
('totalCount', '总项数', 'count', 'number', '检查项总数', TRUE),
('completedCount', '完成数', 'count', 'number', '已完成/已检查的项数', TRUE),
('occurrenceCount', '发生次数', 'count', 'number', '同一问题的发生次数', TRUE),
('consecutiveCount', '连续次数', 'count', 'number', '连续达标/违规的次数', TRUE),

-- 输入值
('value', '输入值', 'input', 'any', '当前检查项的输入值', TRUE),
('grade', '等级', 'input', 'string', '等级评定值: A/B/C/D', TRUE),
('stars', '星级', 'input', 'number', '星级评分: 1-5', TRUE),
('passed', '是否合格', 'input', 'boolean', '合格判定结果', TRUE),
('comment', '评语', 'input', 'string', '评语: 优/良/中/差', TRUE),

-- 时间相关
('currentPeriod', '当前周期', 'time', 'string', '当前评分周期', TRUE),
('periodScores', '周期得分', 'time', 'array', '历史周期得分数组', TRUE),
('previousScore', '上期得分', 'time', 'number', '上一周期的得分', TRUE),
('currentScore', '本期得分', 'time', 'number', '当前周期的得分', TRUE),
('isOverdue', '是否超时', 'time', 'boolean', '是否超过截止时间', TRUE),

-- 上下文
('rank', '排名', 'context', 'number', '当前排名位置', TRUE),
('hasVetoItem', '有否决项', 'context', 'boolean', '是否触发一票否决', TRUE),
('multiplier', '倍率', 'context', 'number', '当前适用的倍率', TRUE);
```

---

## 三、后端架构设计

### 3.1 领域模型

```
domain/scoring/
├── model/
│   ├── aggregate/
│   │   ├── ScoringStrategy.java      # 计分策略聚合根
│   │   ├── InputType.java            # 打分方式聚合根
│   │   └── CalculationRule.java      # 计算规则聚合根
│   ├── entity/
│   │   ├── FormulaFunction.java      # 内置函数
│   │   └── FormulaVariable.java      # 内置变量
│   └── valueobject/
│       ├── Formula.java              # 公式值对象
│       ├── ParameterSchema.java      # 参数定义
│       └── ComponentConfig.java      # 组件配置
├── repository/
│   ├── ScoringStrategyRepository.java
│   ├── InputTypeRepository.java
│   ├── CalculationRuleRepository.java
│   └── FormulaFunctionRepository.java
└── service/
    └── FormulaEngine.java            # 公式引擎领域服务
```

### 3.2 公式引擎设计

```java
/**
 * 公式引擎 - 基于GraalJS
 */
@Service
public class FormulaEngine {

    private final Context jsContext;
    private final Map<String, Value> builtInFunctions;

    /**
     * 执行公式
     * @param formula 公式字符串
     * @param context 上下文变量
     * @return 计算结果
     */
    public Object evaluate(String formula, Map<String, Object> context) {
        // 1. 创建沙箱环境
        // 2. 注入内置函数
        // 3. 注入上下文变量
        // 4. 执行公式
        // 5. 返回结果
    }

    /**
     * 验证公式语法
     */
    public ValidationResult validate(String formula) {
        // 语法检查
        // 变量引用检查
        // 函数调用检查
    }

    /**
     * 预览公式结果
     */
    public PreviewResult preview(String formula, Map<String, Object> sampleData) {
        // 使用示例数据执行公式
        // 返回中间步骤和最终结果
    }
}
```

### 3.3 API设计

```yaml
# 计分策略API
/api/v6/scoring-strategies:
  GET:    获取策略列表（支持分类过滤）
  POST:   创建自定义策略

/api/v6/scoring-strategies/{id}:
  GET:    获取策略详情
  PUT:    更新策略
  DELETE: 删除策略（仅自定义）

/api/v6/scoring-strategies/{id}/test:
  POST:   测试策略公式

# 打分方式API
/api/v6/input-types:
  GET:    获取打分方式列表
  POST:   创建自定义打分方式

/api/v6/input-types/{id}:
  GET:    获取详情
  PUT:    更新
  DELETE: 删除

# 计算规则API
/api/v6/calculation-rules:
  GET:    获取规则列表
  POST:   创建规则

/api/v6/calculation-rules/{id}:
  GET/PUT/DELETE: CRUD

# 公式API
/api/v6/formula/functions:
  GET:    获取内置函数列表

/api/v6/formula/variables:
  GET:    获取内置变量列表

/api/v6/formula/validate:
  POST:   验证公式语法

/api/v6/formula/preview:
  POST:   预览公式执行结果
```

---

## 四、前端架构设计

### 4.1 页面结构

```
views/inspection/v6/
├── TemplateManagement.vue       # 模板管理主页面
├── components/
│   ├── template/
│   │   ├── TemplateDialog.vue   # 模板编辑对话框
│   │   ├── TemplateBasicForm.vue    # 基本信息表单
│   │   └── TemplateScoringForm.vue  # 计分配置表单
│   ├── category/
│   │   └── CategoryDialog.vue   # 类别编辑对话框
│   ├── item/
│   │   ├── ItemDialog.vue       # 检查项编辑对话框
│   │   ├── ItemBasicForm.vue    # 基本信息
│   │   ├── ItemScoringForm.vue  # 计分配置
│   │   └── ItemRulesForm.vue    # 高级规则
│   ├── formula/
│   │   ├── FormulaEditor.vue    # 公式编辑器
│   │   ├── FormulaPreview.vue   # 公式预览
│   │   └── FunctionPicker.vue   # 函数选择器
│   └── scoring/
│       ├── StrategySelector.vue # 策略选择器
│       ├── InputTypeSelector.vue # 打分方式选择器
│       └── RuleChainEditor.vue  # 规则链编辑器

views/system/
├── ScoringStrategyConfig.vue    # 计分策略配置页面
├── InputTypeConfig.vue          # 打分方式配置页面
└── CalculationRuleConfig.vue    # 计算规则配置页面
```

### 4.2 组件设计

#### 4.2.1 策略选择器

```vue
<!-- StrategySelector.vue -->
<template>
  <div class="strategy-selector">
    <!-- 分组展示 -->
    <div class="strategy-group" v-for="group in strategyGroups">
      <div class="group-header" @click="toggleGroup(group.key)">
        <span>{{ group.label }}</span>
        <span class="toggle-icon">{{ group.expanded ? '−' : '+' }}</span>
      </div>
      <div class="group-content" v-show="group.expanded">
        <div
          class="strategy-card"
          v-for="strategy in group.items"
          :class="{ selected: modelValue === strategy.code }"
          @click="selectStrategy(strategy)"
        >
          <div class="card-title">{{ strategy.name }}</div>
          <div class="card-desc">{{ strategy.description }}</div>
          <div class="card-formula">{{ strategy.formulaDescription }}</div>
        </div>
      </div>
    </div>
  </div>
</template>
```

#### 4.2.2 公式编辑器

```vue
<!-- FormulaEditor.vue -->
<template>
  <div class="formula-editor">
    <!-- 编辑区 -->
    <div class="editor-area">
      <div class="code-editor" ref="editorRef">
        <!-- Monaco Editor 或 CodeMirror -->
      </div>
      <div class="editor-toolbar">
        <button @click="insertFunction">插入函数</button>
        <button @click="insertVariable">插入变量</button>
        <button @click="validateFormula">验证</button>
        <button @click="previewFormula">预览</button>
      </div>
    </div>

    <!-- 侧边栏 -->
    <div class="editor-sidebar">
      <!-- 函数列表 -->
      <div class="sidebar-section">
        <div class="section-title">内置函数</div>
        <div class="function-list">
          <div
            class="function-item"
            v-for="fn in functions"
            @click="insertFunctionAt(fn)"
          >
            <span class="fn-name">{{ fn.name }}</span>
            <span class="fn-desc">{{ fn.description }}</span>
          </div>
        </div>
      </div>

      <!-- 变量列表 -->
      <div class="sidebar-section">
        <div class="section-title">可用变量</div>
        <div class="variable-list">
          <div
            class="variable-item"
            v-for="v in variables"
            @click="insertVariableAt(v)"
          >
            <span class="var-name">ctx.{{ v.name }}</span>
            <span class="var-type">({{ v.valueType }})</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 预览区 -->
    <div class="preview-area" v-if="showPreview">
      <div class="preview-title">执行预览</div>
      <div class="preview-input">
        <div v-for="(value, key) in sampleData">
          <label>{{ key }}</label>
          <input v-model="sampleData[key]" />
        </div>
      </div>
      <div class="preview-result">
        结果: {{ previewResult }}
      </div>
    </div>
  </div>
</template>
```

---

## 五、完整流程设计

### 5.1 模板创建流程

```
┌─────────────────────────────────────────────────────────────┐
│                     模板创建向导                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  步骤1: 基本信息                                            │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  模板名称: [________________]                        │   │
│  │  模板代码: [________________]                        │   │
│  │  描    述: [________________________________]        │   │
│  │  适用范围: [班级检查 ▼]                              │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  步骤2: 选择计分策略                                        │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐             │   │
│  │  │ 扣分制  │  │ 加分制  │  │ 基准分制│  ...        │   │
│  │  │ ✓选中   │  │         │  │         │             │   │
│  │  │ 基准分-Σ │  │ 0+Σ加分 │  │基准±调整 │             │   │
│  │  └─────────┘  └─────────┘  └─────────┘             │   │
│  │                                                     │   │
│  │  [+ 展开更多策略]                                    │   │
│  │                                                     │   │
│  │  策略参数:                                          │   │
│  │  基准分: [100____]   最高分: [120____]              │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  步骤3: 配置检查类别                                        │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  [+ 添加类别]                                        │   │
│  │                                                     │   │
│  │  ┌─ 卫生类 ─────────────────────────────────────┐   │   │
│  │  │  权重: 30%   检查项: 8个   [编辑] [删除]      │   │   │
│  │  └───────────────────────────────────────────────┘   │   │
│  │  ┌─ 纪律类 ─────────────────────────────────────┐   │   │
│  │  │  权重: 40%   检查项: 12个  [编辑] [删除]      │   │   │
│  │  └───────────────────────────────────────────────┘   │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  步骤4: 配置检查项 (每个类别)                               │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  当前类别: 卫生类                                    │   │
│  │                                                     │   │
│  │  [+ 添加检查项]  [批量导入]                          │   │
│  │                                                     │   │
│  │  ┌─────────────────────────────────────────────┐   │   │
│  │  │ 地面不清洁          扣分: -2   数值输入      │   │   │
│  │  │ HYG001             [编辑] [规则] [删除]      │   │   │
│  │  └─────────────────────────────────────────────┘   │   │
│  │  ┌─────────────────────────────────────────────┐   │   │
│  │  │ 桌椅不整齐          扣分: -1   档位选择      │   │   │
│  │  │ HYG002             [编辑] [规则] [删除]      │   │   │
│  │  └─────────────────────────────────────────────┘   │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  步骤5: 高级规则 (可选)                                     │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  [x] 启用分数封顶   最高分: [120]                    │   │
│  │  [x] 启用分数保底   最低分: [0]                      │   │
│  │  [ ] 启用一票否决   否决项: [选择...]               │   │
│  │  [ ] 启用累进惩罚   倍率: [1.5]                      │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│                    [上一步]  [下一步]  [保存]               │
└─────────────────────────────────────────────────────────────┘
```

### 5.2 检查项编辑流程

```
┌─────────────────────────────────────────────────────────────┐
│                     检查项编辑                               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─ 基本信息 ───────────────────────────────────────────┐   │
│  │                                                       │   │
│  │  名    称        代    码                             │   │
│  │  [地面不清洁___] [HYG001____]                         │   │
│  │                                                       │   │
│  │  描    述                                             │   │
│  │  [教室或宿舍地面有明显垃圾、污渍或积尘__________]     │   │
│  │                                                       │   │
│  └───────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌─ 计分配置 ───────────────────────────────────────────┐   │
│  │                                                       │   │
│  │  打分方式                    分值设置                 │   │
│  │  ┌─────────────────────┐    ┌────────────────────┐   │   │
│  │  │ ○ 数值输入          │    │ 扣分值: [-2_____] │   │   │
│  │  │ ● 档位选择          │    │                    │   │   │
│  │  │ ○ 勾选完成          │    │ 或配置档位:        │   │   │
│  │  │ ─────────────────   │    │ 轻微  [-1]        │   │   │
│  │  │ ○ 计数输入          │    │ 一般  [-2]        │   │   │
│  │  │ ○ 星级点选          │    │ 严重  [-3]        │   │   │
│  │  │ ○ 等级选择          │    │ [+ 添加档位]       │   │   │
│  │  └─────────────────────┘    └────────────────────┘   │   │
│  │                                                       │   │
│  └───────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌─ 高级规则 (可选) ────────────────────────────────────┐   │
│  │                                                       │   │
│  │  [ ] 累进惩罚    重复违规时: 第n次 = 基础 × [1.5]^n   │   │
│  │  [ ] 一票否决    触发时整体归零                       │   │
│  │  [ ] 关联个人    可记录到个人档案                     │   │
│  │  [ ] 需要拍照    打分时必须上传照片                   │   │
│  │                                                       │   │
│  └───────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌─ 自定义公式 (高级) ──────────────────────────────────┐   │
│  │                                                       │   │
│  │  [x] 使用自定义公式                                   │   │
│  │                                                       │   │
│  │  ┌─────────────────────────────────────────────────┐ │   │
│  │  │ // 根据档位返回不同扣分                          │ │   │
│  │  │ const levels = {轻微: -1, 一般: -2, 严重: -3};  │ │   │
│  │  │ return levels[ctx.value] || 0;                  │ │   │
│  │  └─────────────────────────────────────────────────┘ │   │
│  │                                                       │   │
│  │  [验证公式] [预览结果]                                │   │
│  │                                                       │   │
│  └───────────────────────────────────────────────────────┘   │
│                                                             │
│                              [取消]  [保存]                  │
└─────────────────────────────────────────────────────────────┘
```

---

## 六、实现计划

### 阶段1: 数据库与基础API
1. 创建数据库迁移脚本
2. 实现领域模型
3. 实现Repository层
4. 实现基础CRUD API

### 阶段2: 公式引擎
1. 集成GraalJS
2. 实现公式执行器
3. 实现公式验证器
4. 实现安全沙箱

### 阶段3: 前端基础
1. 实现策略选择器组件
2. 实现打分方式选择器组件
3. 实现规则配置组件

### 阶段4: 公式编辑器
1. 集成Monaco Editor
2. 实现函数/变量自动补全
3. 实现实时预览

### 阶段5: 模板管理整合
1. 整合到模板创建流程
2. 整合到检查项编辑
3. 整合到打分执行

### 阶段6: 系统配置页面
1. 计分策略配置页面
2. 打分方式配置页面
3. 计算规则配置页面
