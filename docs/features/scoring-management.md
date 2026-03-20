# V7 评分管理功能说明

## 概述

评分管理是量化检查系统的核心模块，负责定义"检查结果如何转化为分数"。每个检查模板可以关联一个**评分配置（ScoringProfile）**，配置下包含**评分维度**、**计算规则**和**等级区间**三大子模块。

### 计算流程

```
检查项打分 → 归一化调整 → 维度内求和(SUM) → 跨维度加权平均(WEIGHTED_AVG) → 规则链处理 → 裁剪到[minScore, maxScore] → 等级映射
```

---

## 一、评分配置（ScoringProfile）

每个检查模板对应一个评分配置，定义全局评分约束。

| 字段 | 说明 | 默认值 |
|------|------|--------|
| `maxScore` | 最高分上限 | 100 |
| `minScore` | 最低分下限 | 0 |
| `precisionDigits` | 小数精度（0-10位） | 2 |

**设置方式：** 进入检查模板 → 评分配置 → 编辑基础参数。

**作用：** 最终计算出的分数会被裁剪到 `[minScore, maxScore]` 区间内，并按 `precisionDigits` 四舍五入。

---

## 二、评分维度（ScoreDimension）

维度是对检查项的逻辑分组。例如"卫生"、"纪律"、"安全"可以是三个独立维度，每个维度独立计算后再加权平均得到总分。

| 字段 | 说明 | 默认值 |
|------|------|--------|
| `dimensionCode` | 维度编码（唯一标识） | — |
| `dimensionName` | 维度名称 | — |
| `weight` | 维度权重（整数） | 100 |
| `baseScore` | 维度基础分 | 100 |
| `passThreshold` | 及格线（可选） | null |
| `sortOrder` | 排序序号 | 0 |

### 维度内聚合方式：SUM

维度最终分 = `baseScore` + 所有检查项分数之和

**典型场景：** 基础分 100，扣分项每扣 2 分，维度得分 = 100 - 2 - 2 - ... = 最终分。

### 跨维度聚合方式：加权平均（WEIGHTED_AVG）

```
总分 = Σ(维度分 × 维度权重) / Σ(维度权重)
```

**举例：**
- 卫生维度：得分 85，权重 60
- 纪律维度：得分 92，权重 40
- 总分 = (85×60 + 92×40) / (60+40) = 87.8

### 及格判定

如果设置了 `passThreshold`，维度分低于此值视为不及格。**任何一个维度不及格，总评即为不及格。**

---

## 三、检查项计分模式

每个检查项（TemplateItem）在评分时有 4 种计分模式：

| 模式 | 说明 | 计算方式 |
|------|------|----------|
| `DEDUCTION` | 扣分 | `-(|configScore| × 数量)` |
| `ADDITION` | 加分 | `|configScore| × 数量` |
| `FIXED` | 固定分 | 直接使用 configScore |
| `RESPONSE_MAPPED` | 响应映射 | 使用检查人填写的数值 |

**设置方式：** 在检查模板的检查项编辑中，为每个项配置 `scoringMode` 和 `configScore`。

---

## 四、归一化（Normalization）

归一化用于消除不同组织单元之间人数差异造成的不公平。例如 50 人宿舍和 20 人宿舍，同样扣 3 次，对 20 人宿舍的影响理应更大。

### 归一化模式

| 模式 | 公式 | 适用场景 |
|------|------|----------|
| `NONE` | 系数 = 1（不归一化） | 人数相近或不需要比较 |
| `PER_CAPITA` | 系数 = 基准人数 / 实际人数 | 线性人均调整 |
| `SQRT_ADJUSTED` | 系数 = √(基准人数 / 实际人数) | 平方根折中，避免小人数组织系数过大 |

### 系数裁剪

归一化系数可以设置上下限，防止极端值：
- `floorAt`：系数下限（如 0.5，即最多削弱一半）
- `cappedAt`：系数上限（如 2.0，即最多放大两倍）

### 计算方式

```
归一化后分数 = 原始分数 × 归一化系数
```

**设置方式：** 在检查项的评分配置 JSON 中启用归一化并指定模式、基准人数、上下限。

---

## 五、计算规则链（CalculationRule）

规则链在总分计算完成后按 `priority` 顺序依次执行，对总分做额外调整。

| 字段 | 说明 |
|------|------|
| `ruleCode` | 规则编码（唯一标识） |
| `ruleName` | 规则名称 |
| `priority` | 执行优先级（数值小的先执行） |
| `ruleType` | 规则类型（见下表） |
| `config` | 规则参数（JSON 格式） |
| `isEnabled` | 是否启用 |

### 规则类型

#### 1. VETO — 一票否决

特定检查项只要触发（分数≠0），总分直接变为指定值（通常为 0）。

```json
{
  "vetoItems": ["FIRE_SAFETY", "CRITICAL_VIOLATION"],
  "vetoScore": 0
}
```

**场景：** 消防安全不合格，无论其他项多好，总分归零。

#### 2. PROGRESSIVE — 累进扣分

根据扣分项数量，阶梯式额外扣分。违规越多，额外惩罚越重。

```json
{
  "thresholds": [
    { "count": 3, "penalty": 5 },
    { "count": 5, "penalty": 15 },
    { "count": 10, "penalty": 30 }
  ]
}
```

**场景：** 3 项违规额外扣 5 分，5 项违规额外扣 15 分，10 项违规额外扣 30 分。匹配最高满足的阈值。

#### 3. BONUS — 奖励加分

特定检查项满足条件（分数 > 0）时，每项额外加固定分。

```json
{
  "bonusItems": ["EXCELLENT_HYGIENE", "INNOVATION"],
  "bonusScore": 3
}
```

**场景：** 卫生优秀加 3 分，创新亮点加 3 分，两项都满足共加 6 分。

#### 4. CUSTOM — 自定义公式

使用自定义表达式对总分进行变换。公式中可使用变量 `score`（当前总分）。

```json
{
  "formula": "score * 0.9 + 10"
}
```

**场景：** 对总分做线性变换，适用于特殊评分策略。

### 规则执行顺序

1. 按 `priority` 从小到大排序
2. 仅执行 `isEnabled = true` 的规则
3. 每条规则产生一个 `adjustment`（调整量），累加到总分
4. 全部规则执行完后，再做 `[minScore, maxScore]` 裁剪

---

## 六、等级区间（GradeBand）

将数值分数映射为可读等级（如 A/B/C/D 或 优/良/中/差）。

| 字段 | 说明 |
|------|------|
| `gradeCode` | 等级编码（如 `A`） |
| `gradeName` | 等级名称（如 `优秀`） |
| `minScore` | 分数下限（含） |
| `maxScore` | 分数上限（含） |
| `color` | 展示颜色（可选） |
| `icon` | 展示图标（可选） |
| `dimensionId` | 关联维度（null 表示总分等级） |

**设置示例：**

| 等级 | 分数区间 | 颜色 |
|------|----------|------|
| A 优秀 | 90 ~ 100 | #67C23A |
| B 良好 | 75 ~ 89 | #409EFF |
| C 合格 | 60 ~ 74 | #E6A23C |
| D 不合格 | 0 ~ 59 | #F56C6C |

**特性：**
- 可以为总分设置等级（`dimensionId = null`）
- 也可以为单个维度设置独立的等级体系

---

## 七、完整示例

### 场景：宿舍卫生检查评分配置

**评分配置：**
- 满分 100，最低 0 分，保留 1 位小数

**维度设置：**

| 维度 | 权重 | 基础分 | 及格线 |
|------|------|--------|--------|
| 室内卫生 | 60 | 100 | 60 |
| 公共区域 | 40 | 100 | 60 |

**检查项（室内卫生维度）：**
- 床铺不整：DEDUCTION，configScore = 3
- 地面脏污：DEDUCTION，configScore = 5
- 窗台积灰：DEDUCTION，configScore = 2

**计算规则：**
1. VETO（priority=1）：消防通道堵塞 → 总分归 0
2. PROGRESSIVE（priority=2）：≥5项违规额外扣 10 分
3. BONUS（priority=3）：整洁标兵额外加 5 分

**等级区间：** A(90-100) / B(75-89) / C(60-74) / D(0-59)

**计算过程：**
1. 室内卫生维度：100 + (-3) + (-5) = 92
2. 公共区域维度：100 + (-2) = 98
3. 总分 = (92×60 + 98×40) / 100 = 94.4
4. 无规则触发
5. 裁剪：94.4（在 [0,100] 内，不变）
6. 精度：94.4
7. 等级：A 优秀

---

## API 端点

基础路径：`/v7/insp/scoring-profiles`

| 操作 | 方法 | 路径 |
|------|------|------|
| 创建评分配置 | POST | `/` |
| 列表 | GET | `/` |
| 查询单个 | GET | `/{id}` |
| 按模板查询 | GET | `/by-template/{templateId}` |
| 更新 | PUT | `/{id}` |
| 删除 | DELETE | `/{id}` |
| 创建维度 | POST | `/{id}/dimensions` |
| 列表维度 | GET | `/{id}/dimensions` |
| 更新维度 | PUT | `/{id}/dimensions/{dimensionId}` |
| 删除维度 | DELETE | `/{id}/dimensions/{dimensionId}` |
| 创建等级 | POST | `/{id}/grade-bands` |
| 列表等级 | GET | `/{id}/grade-bands` |
| 更新等级 | PUT | `/{id}/grade-bands/{bandId}` |
| 删除等级 | DELETE | `/{id}/grade-bands/{bandId}` |
| 创建规则 | POST | `/{id}/calculation-rules` |
| 列表规则 | GET | `/{id}/calculation-rules` |
| 更新规则 | PUT | `/{id}/calculation-rules/{ruleId}` |
| 删除规则 | DELETE | `/{id}/calculation-rules/{ruleId}` |

---

## 前端页面

| 页面/组件 | 功能 |
|-----------|------|
| `ScoringProfileListView.vue` | 评分配置列表，展示所有模板的评分概览 |
| `ScoringProfileEditor.vue` | 评分配置编辑器，配置 maxScore/minScore/precisionDigits |
| `DimensionTable.vue` | 维度管理，增删改维度及其权重、基础分、及格线 |
| `CalcRuleChain.vue` | 规则链编辑器，管理计算规则及执行顺序 |
| `RuleConfigForm.vue` | 规则参数表单，根据规则类型展示不同的配置界面 |
| `GradeBandEditor.vue` | 等级区间编辑器，配置分数与等级的映射关系 |
| `ScoreTestPanel.vue` | 评分测试面板，模拟输入数据实时预览计算结果 |
| `FormulaEditor.vue` | 公式编辑器，用于 CUSTOM 规则类型的公式输入 |
