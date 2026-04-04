# 指标树（Indicator Tree）重构设计

> **日期**: 2026-03-28
> **状态**: 设计中
> **目标**: 将评价体系从检查内容和执行层独立出来，引入指标树作为项目级评价配置

---

## 1. 核心问题

当前三个关注点混在一起：
- **检查内容**（模板/分区）混入了等级方案绑定
- **评价逻辑**（汇总/周期/缺失策略）硬编码在 ScoreAggregationService
- **多层评级**（EvaluationRule 和评级中心）功能重叠

导致：根分区等级无法按周期汇总、不同频次的子分区无法对齐、模板包含了不该有的时间维度配置。

## 2. 设计原则

```
模板（Template）    →  定义"查什么"（纯静态，不含评价配置）
指标树（Indicator）  →  定义"怎么评"（项目级，含来源/汇总/周期/等级映射）
等级方案（GradeScheme）→  定义"叫什么"（分数→名称映射，被指标引用）
评级中心（EvalCampaign）→  跨项目多条件评选（消费指标树产出的分数/等级）
```

## 3. 数据模型

### 3.1 新增：Indicator（评价指标）

```
Indicator（聚合根，项目级配置）
├── id: Long                       PK
├── tenantId: Long
├── projectId: Long                所属项目
├── parentIndicatorId: Long        null=顶级指标（根指标）
├── name: String(100)              "卫生指标"
├── indicatorType: String          LEAF | COMPOSITE
│
│  ── LEAF 属性（从分区取分）──
├── sourceSectionId: Long          数据来源分区ID
├── sourceAggregation: String      周期内多次检查合并：AVG | MAX | MIN | LATEST | SUM
│
│  ── COMPOSITE 属性（从子指标汇总）──
├── compositeAggregation: String   子指标合并：WEIGHTED_AVG | SUM | AVG | MIN | MAX
├── missingPolicy: String          缺失策略：SKIP | CARRY_FORWARD | MARK_INCOMPLETE
│
│  ── 通用属性 ──
├── weight: Integer                权重 0-100（在父指标中的占比）
├── evaluationPeriod: String       PER_TASK | DAILY | WEEKLY | MONTHLY
├── gradeSchemeId: Long            等级方案（可选）FK → GradeScheme
├── sortOrder: Integer
├── createdAt: LocalDateTime
├── updatedAt: LocalDateTime
```

### 3.2 新增：IndicatorScore（指标得分记录）

替代现有 ProjectScore、RatingResult。

```
IndicatorScore
├── id: Long                       PK
├── tenantId: Long
├── indicatorId: Long              FK → Indicator
├── targetId: Long                 检查目标ID
├── targetName: String             检查目标名称
├── targetType: String             ORG | PLACE | USER
├── periodStart: LocalDate         评估周期开始日期
├── periodEnd: LocalDate           评估周期结束日期
├── score: BigDecimal              汇总得分
├── gradeCode: String              等级编码（从 GradeScheme 映射）
├── gradeName: String              等级名称
├── gradeColor: String             等级颜色
├── sourceCount: Integer           本周期内数据条数（几次检查取的平均）
├── detail: String                 JSON 明细（子指标分数等）
├── createdAt: LocalDateTime
├── updatedAt: LocalDateTime
```

### 3.3 修改：TemplateSection

**移除** `gradeSchemeId` 字段 — 模板不再绑定等级方案，由指标树引用。

### 3.4 删除的模型

| 模型 | 原因 |
|------|------|
| EvaluationRule / EvaluationLevel / EvaluationResult / EvaluationCondition | 功能被评级中心覆盖 |
| RatingDimension / RatingResult | 功能被指标树覆盖 |
| ProjectScore | 被 IndicatorScore 替代 |

### 3.5 保留的模型

| 模型 | 原因 |
|------|------|
| GradeScheme / GradeDefinition | 被指标引用（从模板移到指标） |
| ScoringProfile / ScoreDimension / GradeBand / CalculationRule | 打分引擎，与评价体系无关 |
| ScoringPolicy / PolicyGradeBand | 预设评分策略 |

---

## 4. 数据库 Schema

### 4.1 新表：insp_indicators

```sql
CREATE TABLE insp_indicators (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id             BIGINT NOT NULL DEFAULT 0,
    project_id            BIGINT NOT NULL,
    parent_indicator_id   BIGINT DEFAULT NULL,
    name                  VARCHAR(100) NOT NULL COMMENT '指标名称',
    indicator_type        VARCHAR(20) NOT NULL DEFAULT 'LEAF' COMMENT 'LEAF|COMPOSITE',
    -- LEAF
    source_section_id     BIGINT DEFAULT NULL COMMENT '数据来源分区ID',
    source_aggregation    VARCHAR(20) DEFAULT 'AVG' COMMENT 'AVG|MAX|MIN|LATEST|SUM',
    -- COMPOSITE
    composite_aggregation VARCHAR(20) DEFAULT 'WEIGHTED_AVG' COMMENT 'WEIGHTED_AVG|SUM|AVG|MIN|MAX',
    missing_policy        VARCHAR(20) DEFAULT 'SKIP' COMMENT 'SKIP|CARRY_FORWARD|MARK_INCOMPLETE',
    -- Common
    weight                INT NOT NULL DEFAULT 100,
    evaluation_period     VARCHAR(20) NOT NULL DEFAULT 'PER_TASK' COMMENT 'PER_TASK|DAILY|WEEKLY|MONTHLY',
    grade_scheme_id       BIGINT DEFAULT NULL COMMENT 'FK → insp_grade_schemes',
    sort_order            INT NOT NULL DEFAULT 0,
    created_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted               TINYINT(1) NOT NULL DEFAULT 0,
    INDEX idx_project (project_id),
    INDEX idx_parent (parent_indicator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价指标';
```

### 4.2 新表：insp_indicator_scores

```sql
CREATE TABLE insp_indicator_scores (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    indicator_id    BIGINT NOT NULL,
    target_id       BIGINT NOT NULL,
    target_name     VARCHAR(200) DEFAULT NULL,
    target_type     VARCHAR(20) DEFAULT NULL,
    period_start    DATE NOT NULL,
    period_end      DATE NOT NULL,
    score           DECIMAL(10,2) DEFAULT NULL,
    grade_code      VARCHAR(50) DEFAULT NULL,
    grade_name      VARCHAR(100) DEFAULT NULL,
    grade_color     VARCHAR(20) DEFAULT NULL,
    source_count    INT DEFAULT 0,
    detail          TEXT DEFAULT NULL COMMENT 'JSON',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT(1) NOT NULL DEFAULT 0,
    INDEX idx_indicator (indicator_id),
    INDEX idx_target (target_id),
    INDEX idx_period (period_start, period_end),
    UNIQUE KEY uk_indicator_target_period (indicator_id, target_id, period_start)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='指标得分记录';
```

### 4.3 迁移：TemplateSection 移除 grade_scheme_id

```sql
ALTER TABLE insp_template_sections DROP COLUMN grade_scheme_id;
```

### 4.4 删表（标记 deprecated，暂不 DROP）

```sql
-- 以下表被指标树/评级中心替代，标记废弃
-- insp_evaluation_rules
-- insp_evaluation_levels
-- insp_evaluation_results
-- insp_rating_dimensions
-- insp_rating_results
-- insp_project_scores（被 insp_indicator_scores 替代）
```

---

## 5. 后端设计

### 5.1 新增领域模型

**Indicator** — `domain/inspection/model/v7/scoring/Indicator.java`
- 聚合根，Builder 模式
- 工厂方法：`createLeaf(projectId, name, sourceSectionId, ...)` / `createComposite(projectId, name, ...)`
- 查询方法：`isLeaf()` / `isComposite()` / `isRoot()`

**IndicatorScore** — `domain/inspection/model/v7/scoring/IndicatorScore.java`
- 实体，Builder 模式
- 工厂方法：`create(indicatorId, targetId, periodStart, periodEnd, ...)`
- 更新方法：`updateScore(score, gradeCode, gradeName, gradeColor, sourceCount, detail)`

### 5.2 新增仓储

**IndicatorRepository**
```java
Indicator save(Indicator indicator);
Optional<Indicator> findById(Long id);
List<Indicator> findByProjectId(Long projectId);
List<Indicator> findByParentIndicatorId(Long parentId);
List<Indicator> findRootIndicators(Long projectId); // parentId=null
void deleteById(Long id);
void deleteByProjectId(Long projectId);
```

**IndicatorScoreRepository**
```java
IndicatorScore save(IndicatorScore score);
Optional<IndicatorScore> findByIndicatorAndTargetAndPeriod(Long indicatorId, Long targetId, LocalDate periodStart);
List<IndicatorScore> findByIndicatorId(Long indicatorId);
List<IndicatorScore> findByIndicatorIdAndPeriod(Long indicatorId, LocalDate periodStart, LocalDate periodEnd);
List<IndicatorScore> findByProjectAndPeriod(Long projectId, LocalDate periodStart, LocalDate periodEnd);
```

### 5.3 新增应用服务

**IndicatorApplicationService** — 指标树 CRUD
- `createIndicator(...)` / `updateIndicator(...)` / `deleteIndicator(...)`
- `getIndicatorTree(projectId)` — 返回完整树结构
- `initDefaultIndicators(projectId, rootSectionId)` — 项目创建时自动生成默认指标树

**IndicatorScoreService** — 分数汇总引擎（替代 ScoreAggregationService 的评价部分）
- `computeLeafScore(indicatorId, targetId, periodStart, periodEnd)` — 从 submissions 取分，按 sourceAggregation 聚合
- `computeCompositeScore(indicatorId, targetId, periodStart, periodEnd)` — 从子指标取分，按 compositeAggregation + weight 汇总
- `computeAllForProject(projectId, periodStart, periodEnd)` — 整个项目全量计算
- `computeOnSubmissionComplete(submissionId)` — 单次提交后增量计算相关叶子指标

### 5.4 新增 REST 控制器

**IndicatorController** — `/v7/insp/indicators`
```
GET    /v7/insp/indicators?projectId={id}        列表（含树结构）
POST   /v7/insp/indicators                       创建指标
PUT    /v7/insp/indicators/{id}                   更新
DELETE /v7/insp/indicators/{id}                   删除
POST   /v7/insp/indicators/init?projectId={id}    自动初始化默认指标树

GET    /v7/insp/indicator-scores?projectId={id}&periodStart=&periodEnd=  查询得分
POST   /v7/insp/indicator-scores/compute?projectId={id}&periodStart=&periodEnd=  手动触发计算
```

### 5.5 修改 ScoreAggregationService

- **保留**：submission 级别的分数计算（computeScoreFields、recalculateFromSubmission）
- **移除**：gradeProjectScore、determineGrade、determineGradeFromScheme — 这些职责转移到 IndicatorScoreService
- **新增**：提交完成后触发叶子指标增量计算

### 5.6 删除的后端文件（29 个）

```
# EvaluationRule 系列（16 个）
domain/.../scoring/EvaluationRule.java
domain/.../scoring/EvaluationLevel.java
domain/.../scoring/EvaluationResult.java
domain/.../scoring/EvaluationCondition.java
domain/.../repository/v7/EvaluationRuleRepository.java
domain/.../repository/v7/EvaluationLevelRepository.java
domain/.../repository/v7/EvaluationResultRepository.java
infrastructure/.../scoring/EvaluationRule{PO,Mapper,RepositoryImpl}.java
infrastructure/.../scoring/EvaluationLevel{PO,Mapper,RepositoryImpl}.java
infrastructure/.../scoring/EvaluationResult{PO,Mapper,RepositoryImpl}.java
interfaces/.../v7/EvaluationRuleController.java
application/.../v7/evaluation/EvaluationEngine.java

# RatingDimension 系列（13 个）
domain/.../scoring/RatingDimension.java
domain/.../scoring/RatingResult.java
domain/.../repository/v7/RatingDimensionRepository.java
domain/.../repository/v7/RatingResultRepository.java
infrastructure/.../scoring/RatingDimension{PO,Mapper,RepositoryImpl}.java
infrastructure/.../scoring/RatingResult{PO,Mapper,RepositoryImpl}.java
interfaces/.../v7/RatingDimensionController.java
application/.../v7/RatingDimensionApplicationService.java

# ProjectScore 系列（替代，不删文件，重构为 IndicatorScore）
```

---

## 6. 前端设计

### 6.1 新增类型

```typescript
// types/insp/indicator.ts
export interface Indicator {
  id: number
  projectId: number
  parentIndicatorId: number | null
  name: string
  indicatorType: 'LEAF' | 'COMPOSITE'
  sourceSectionId: number | null
  sourceAggregation: 'AVG' | 'MAX' | 'MIN' | 'LATEST' | 'SUM'
  compositeAggregation: 'WEIGHTED_AVG' | 'SUM' | 'AVG' | 'MIN' | 'MAX'
  missingPolicy: 'SKIP' | 'CARRY_FORWARD' | 'MARK_INCOMPLETE'
  weight: number
  evaluationPeriod: 'PER_TASK' | 'DAILY' | 'WEEKLY' | 'MONTHLY'
  gradeSchemeId: number | null
  sortOrder: number
  // Eagerly loaded
  children?: Indicator[]
  gradeScheme?: GradeScheme | null
}

export interface IndicatorScore {
  id: number
  indicatorId: number
  targetId: number
  targetName: string
  targetType: string
  periodStart: string
  periodEnd: string
  score: number | null
  gradeCode: string | null
  gradeName: string | null
  gradeColor: string | null
  sourceCount: number
}
```

### 6.2 新增组件

**IndicatorTreeEditor.vue** — 项目详情页新标签"评价指标"
```
┌─────────────────────────────────────────────────────────┐
│ 评价指标                            [自动初始化] [保存]  │
│─────────────────────────────────────────────────────────│
│                                                         │
│ ├─ 综合评价（复合 · 每周 · 综合之星）     [编辑] [删除]  │
│ │  汇总: 加权平均 · 缺失: 跳过                          │
│ │                                                       │
│ │  ├─ 卫生指标（叶子 · 每次 · 流动红旗）  [编辑] [删除]  │
│ │  │  来源: 卫生分区 · 聚合: 取平均 · 权重: 40%         │
│ │  │                                                    │
│ │  ├─ 教室指标（叶子 · 每次 · 星级评定）  [编辑] [删除]  │
│ │  │  来源: 教室分区 · 聚合: 取平均 · 权重: 30%         │
│ │  │                                                    │
│ │  └─ 安全指标（叶子 · 每周 · 安全等级）  [编辑] [删除]  │
│ │     来源: 安全分区 · 聚合: 取最新 · 权重: 30%         │
│ │                                                       │
│ └─ [+ 添加子指标]                                       │
│                                                         │
│ [+ 添加顶级指标]                                        │
└─────────────────────────────────────────────────────────┘
```

**IndicatorScoreView.vue** — 替代 ProjectDetailView 的成绩统计 tab
```
┌──────────────────────────────────────────────────────────┐
│ 评估周期:  [本周 ▾]   [2026-03-24 ~ 2026-03-30]         │
│──────────────────────────────────────────────────────────│
│                                                          │
│ 综合评价                                                 │
│ ┌────────┬──────────────┬──────────┬─────────┬─────────┐ │
│ │ 排名   │ 班级         │ 卫生红旗 │ 教室星级 │ 综合之星│ │
│ ├────────┼──────────────┼──────────┼─────────┼─────────┤ │
│ │ 🥇     │ 高一(1)班    │ 红旗 95  │ 五星 92 │ 金星    │ │
│ │ 🥈     │ 高一(2)班    │ 蓝旗 82  │ 四星 85 │ 银星    │ │
│ │ 🥉     │ 高一(3)班    │ 黄旗 65  │ 三星 70 │ 铜星    │ │
│ └────────┴──────────────┴──────────┴─────────┴─────────┘ │
│                                                          │
│ 表头 = 叶子指标的等级方案 displayName                     │
│ 最后一列 = 根指标的等级方案 displayName                   │
└──────────────────────────────────────────────────────────┘
```

### 6.3 修改现有组件

**ProjectDetailView.vue**
- 成绩统计 tab：替换为 IndicatorScoreView 组件
- 新增"评价指标" tab：嵌入 IndicatorTreeEditor

**InspectionResultsView.vue**
- 从 IndicatorScore 读取数据而非 submissions 直接聚合

**GradeSchemeSelector.vue / GradeSchemeEditor.vue**
- 保留，但不再从模板分区入口使用
- 改为从指标编辑器中引用

### 6.4 删除的前端文件

```
# 类型
frontend/src/types/insp/evaluation.ts

# API
frontend/src/api/insp/ratingDimension.ts

# 组件（GradeSchemeSelector 从模板分区中移除，但保留组件本身给指标编辑器用）
```

---

## 7. 指标自动初始化逻辑

项目创建并关联模板后，可一键生成默认指标树：

```
输入: projectId, rootSectionId（模板根分区）
逻辑:
  1. 读取模板的一级子分区列表
  2. 为每个子分区创建一个 LEAF 指标
     - name = 分区名称
     - sourceSectionId = 分区ID
     - sourceAggregation = AVG
     - evaluationPeriod = PER_TASK
     - weight = 均分（100/分区数）
  3. 创建一个 COMPOSITE 根指标
     - name = 模板名称 + "综合评价"
     - compositeAggregation = WEIGHTED_AVG
     - evaluationPeriod = WEEKLY
     - missingPolicy = SKIP
  4. 将所有叶子指标挂到根指标下
```

---

## 8. 分数计算流程

### 8.1 即时计算（检查员提交后）

```
检查员提交 submission
  → ScoreAggregationService.recalculateFromSubmission() [保留]
  → 计算 submission 的 finalScore
  → 触发 IndicatorScoreService.computeOnSubmissionComplete()
    → 找到该 submission.sectionId 关联的叶子指标
    → 如果叶子指标 evaluationPeriod = PER_TASK
      → 立即计算该指标本次得分 + 映射等级
      → 写入 IndicatorScore（periodStart=periodEnd=taskDate）
```

### 8.2 周期计算（定时任务）

```
每天凌晨 IndicatorScoreScheduler 执行：
  → 扫描所有活跃项目
  → 对每个项目：
    → 确定今天是否是某个周期的结束日（周日=WEEKLY结束，月末=MONTHLY结束）
    → 如果是：
      → 计算所有 DAILY 指标的当日分数
      → 计算所有 WEEKLY 指标的本周分数
      → 计算所有 MONTHLY 指标的本月分数
      → 计算所有 COMPOSITE 指标（递归，从叶子到根）
```

### 8.3 手动触发

```
管理员点"重新计算"按钮
  → POST /v7/insp/indicator-scores/compute?projectId=&periodStart=&periodEnd=
  → 全量重算指定周期的所有指标
```

---

## 9. 实现优先级

| 阶段 | 内容 | 文件数 |
|------|------|--------|
| P0 | DB迁移 + Indicator/IndicatorScore 领域模型 + 仓储 + 基础设施 | ~12 |
| P1 | IndicatorApplicationService + Controller | ~2 |
| P2 | IndicatorScoreService（计算引擎）| ~1 |
| P3 | TemplateSection 移除 gradeSchemeId + GradeScheme 绑定迁移 | ~5 |
| P4 | 前端 types + API + IndicatorTreeEditor | ~4 |
| P5 | 前端 IndicatorScoreView（成绩展示重构）| ~2 |
| P6 | 删除废弃文件（29个后端 + 2个前端）| ~31 |
| P7 | ScoreAggregationService 清理 + 集成 | ~1 |
| P8 | InspectionResultsView 重构 | ~1 |
