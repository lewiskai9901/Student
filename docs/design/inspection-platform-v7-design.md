# 企业级检查平台模块 V7 - 详细设计文档

> **版本**: 3.0
> **日期**: 2026-02-25
> **状态**: 设计评审 (v3 - 多维度审计修订)
> **变更**: 拆分InspSubmission为独立聚合、新增2张读模型表、补全24个前端视图+51个组件+6个Store+完整路由权限、修复事件基础设施缺陷

---

## 目录

1. [设计概述](#1-设计概述)
2. [限界上下文分解](#2-限界上下文分解)
3. [领域模型详细设计](#3-领域模型详细设计)
4. [数据库设计](#4-数据库设计)
5. [API 设计](#5-api-设计)
6. [事件目录](#6-事件目录)
7. [前端架构](#7-前端架构)
8. [实施阶段](#8-实施阶段)
9. [关键架构决策](#9-关键架构决策)
10. [复用现有基础设施](#10-复用现有基础设施)

---

## 1. 设计概述

### 1.1 背景与动机

当前 V6 检查模块存在以下架构问题：

| 问题 | 影响 |
|------|------|
| CorrectiveAction 无权限控制 | 任何用户可操作整改 |
| 公式引擎（JavaScriptFormulaEngine）已删除 | 评分无法自定义 |
| 应用层穿透 DDD 分层 | InspectorAssignmentService 直接用 Mapper |
| 模板贫血模型 | InspectionTemplate 是纯 POJO，无领域行为 |
| 汇总服务使用 raw SQL | InspectionSummaryService 无领域模型 |
| V6 模板不支持版本快照 | 修改模板会影响历史数据 |
| 字段类型单一 | 仅 DEDUCTION/ADDITION/FIXED/PER_PERSON/RANGE |
| 无人数归一化 | 10人班和50人班扣同样分数不公平 |
| 整改无根因分析 | 只有自由文本描述，无法结构化分析 |
| 无PDF报告生成 | 业界标配功能缺失 |
| 无审计日志 | 旧AuditLog已删除，合规风险 |

### 1.2 设计理念

参考 SafetyCulture/iAuditor、Fulcrum、Form.io、MasterControl、GoAudits、KPA Flex 等业界 SaaS 检查平台：

- **模板即蓝图 + 不可变版本快照** — 历史数据永不受模板修改影响
- **动态表单引擎** — 22 种字段类型，条件逻辑，自定义验证
- **多维度可编程评分引擎** — 公式 + 规则链 + 等级映射 + 人数归一化
- **企业级 CAPA** — 根因分析(5Why/鱼骨图) + 效果验证闭环 + 预防措施
- **事件编排 > 中央编排** — 通过现有 outbox 模式实现跨聚合解耦
- **CQRS 读写分离** — 写侧事务优先，读侧星型 schema 预聚合
- **可配置自动化** — 通知规则引擎 + SLA自动升级 + Webhook外部集成
- **合规审计** — 不可变审计日志 + PDF报告自动生成

### 1.3 与 V6 的共存策略

- 新表统一 `insp_` 前缀，与旧表 `inspection_*` 区分
- 新 API 统一 `/api/v7/insp/` 前缀
- V6 前端路由 `/inspection/v6/*` 保持不动，V7 新增 `/inspection/v7/*`
- 渐进迁移：V7 稳定后可逐步关闭 V6 端点

---

## 2. 限界上下文分解

### 2.1 子域架构

```
+=====================================================================+
|                    INSPECTION PLATFORM MODULE                        |
|                                                                      |
|  +------------------+   +-------------------+   +------------------+ |
|  |  TEMPLATE BC     |   |  EXECUTION BC     |   |  CORRECTIVE BC   | |
|  |  (表单引擎)      |   |  (检查执行)       |   |  (CAPA管理)      | |
|  |                  |   |                   |   |                  | |
|  |  InspTemplate    |   |  InspProject      |   |  CorrectiveCase  | |
|  |  ResponseSet     |   |  InspTask         |   |  IssueCategory   | |
|  |  TemplateCatalog |   |  InspSubmission   |   |                  | |
|  |  TemplateVersion |   |  SubmissionDetail |   |                  | |
|  +--------+---------+   +--------+----------+   +--------+---------+ |
|           |                      |                       |           |
|           v                      v                       v           |
|  +------------------+   +-------------------+   +------------------+ |
|  |  SCORING BC      |   |  ANALYTICS BC     |   |  PLATFORM BC     | |
|  |  (评分引擎)      |   |  (CQRS 读侧)     |   |  (平台服务)      | |
|  |                  |   |                   |   |                  | |
|  |  ScoringProfile  |   |  DailySummary     |   |  NotificationRule| |
|  |  ScoreDimension  |   |  PeriodSummary    |   |  ReportTemplate  | |
|  |  GradeBand       |   |  InspectorSummary |   |  WebhookSub      | |
|  |  CalcRule        |   |                   |   |  AuditTrail      | |
|  +------------------+   +-------------------+   +------------------+ |
+=====================================================================+
```

### 2.2 跨 BC 集成（全部通过领域事件）

```
ProjectPublishedEvent      ──→  TaskGenerationService（自动生成任务）
TaskPublishedEvent         ──→  AnalyticsProjectionService（更新汇总）
                           ──→  NotificationRuleEngine（触发通知规则）
SubmissionCompletedEvent   ──→  CorrectiveAutoCreationHandler（flagged→自动创建整改）
CaseEscalatedEvent         ──→  NotificationRuleEngine（发送告警）
                           ──→  WebhookDispatcher（外部集成）
CaseVerifiedEvent          ──→  EffectivenessScheduler（安排效果验证）
PeriodSummaryCalculatedEvent ─→  RatingCalculationService（触发评级）
所有写操作                  ──→  AuditTrailAppender（不可变审计日志）
```

---

## 3. 领域模型详细设计

> **编码规范**：遵循现有 V6 模式 — 领域模型手写 Builder（不用 Lombok），`private Long id` 字段，
> 工厂方法 `create(...)`，状态机方法抛 `IllegalStateException`。PO 用 Lombok `@Data`。

### 3.1 Template BC — 表单引擎

#### 包结构

```
domain/inspection/model/v7/template/
├── InspTemplate.java          (AggregateRoot)
├── TemplateVersion.java       (Entity — 不可变快照)
├── TemplateSection.java       (Entity — 层级节点)
├── TemplateItem.java          (Entity — 表单字段)
├── ResponseSet.java           (AggregateRoot)
├── ResponseSetOption.java     (Entity)
├── TemplateCatalog.java       (AggregateRoot — 树形分类)
├── ItemType.java              (Enum — 22种)
└── TemplateStatus.java        (Enum)
```

#### InspTemplate（聚合根）

```java
public class InspTemplate extends AggregateRoot<Long> {
    private Long id;
    private String templateCode;
    private String templateName;
    private String description;
    private Long catalogId;
    private String tags;                 // JSON: ["安全","卫生"]
    private String applicableTargetTypes;// JSON: ["ORG","PLACE","USER"]
    private Integer latestVersion;
    private TemplateStatus status;       // DRAFT|PUBLISHED|DEPRECATED|ARCHIVED
    private Boolean isDefault;
    private Integer useCount;
    private LocalDateTime lastUsedAt;

    // 状态机: DRAFT→PUBLISHED→DEPRECATED→ARCHIVED
    public static InspTemplate create(...) { ... }
    public TemplateVersion publish(List<TemplateSection> sections,
                                    List<TemplateItem> items,
                                    String scoringProfileSnapshot) { ... }
    public void deprecate() { ... }
    public void archive() { ... }
    public void incrementUseCount() { ... }
}
```

#### TemplateVersion（实体 — 不可变快照）

```java
public class TemplateVersion implements Entity<Long> {
    private Long id;
    private Long templateId;
    private Integer version;              // 1, 2, 3...
    private String structureSnapshot;     // JSON: 完整 sections+items 树
    private String scoringProfileSnapshot;// JSON: 评分配置快照
    // 构造后不可变，无 setter
}
```

#### TemplateSection（实体 — 层级节点）

```java
public class TemplateSection implements Entity<Long> {
    private Long id;
    private Long templateId;
    private Long parentSectionId;    // null = 根节点
    private String sectionCode;
    private String sectionName;
    private Integer sortOrder;
    private Integer weight;          // 0-100
    private Boolean isRepeatable;
    private String conditionLogic;   // JSON: 条件逻辑 V2（见下方 Schema）
}
```

#### TemplateItem（实体 — 表单字段）

```java
public class TemplateItem implements Entity<Long> {
    private Long id;
    private Long sectionId;
    private String itemCode;
    private String itemName;
    private String description;
    private ItemType itemType;        // 22种类型
    private String config;            // JSON: 类型特定配置
    private String validationRules;   // JSON: 验证规则数组
    private Long responseSetId;       // → ResponseSet
    private String scoringConfig;     // JSON: 含评分+归一化配置（见3.7节）
    private String helpContent;       // ★ 新增：帮助文本/参考图片URL
    private Boolean isRequired;
    private Boolean isScored;
    private Boolean requireEvidence;  // ★ 新增：非媒体类字段是否强制要求附证据
    private Integer sortOrder;
    private String conditionLogic;    // JSON: 条件逻辑 V2（见下方 Schema）
}
```

#### 条件逻辑 V2 JSON Schema

条件逻辑支持 AND/OR 嵌套条件组 + 多动作。通过 `version: 2` 识别。

```json
{
  "version": 2,
  "conditions": {
    "logicOp": "and",
    "rules": [
      { "field": "Q01", "operator": "equals", "value": "FAIL" },
      {
        "logicOp": "or",
        "rules": [
          { "field": "Q02", "operator": "greaterOrEqual", "value": "3" },
          { "field": "Q03", "operator": "isEmpty" }
        ]
      }
    ]
  },
  "actions": [
    { "type": "hide" },
    { "type": "setScore", "value": 0 }
  ]
}
```

**操作符（11个）：** equals, notEquals, greaterThan, lessThan, greaterOrEqual, lessOrEqual, contains, in, between, isEmpty, isNotEmpty

**动作（6个）：** show, hide, require, disable, setScore（需 value）, clearValue

**嵌套限制：** 最多 3 层。分区和 Item 均支持条件逻辑。分区隐藏时其下所有 Item 强制隐藏。

**执行引擎：** 前端纯函数，拓扑排序处理级联依赖。后端透传不解析。
```

#### ItemType 枚举（22 种）

```java
public enum ItemType {
    TEXT, TEXTAREA, RICH_TEXT,                    // 文本
    NUMBER, SLIDER, CALCULATED,                  // 数值
    SELECT, MULTI_SELECT, CHECKBOX, RADIO,       // 选择
    DATE, TIME, DATETIME,                        // 日期时间
    PHOTO, VIDEO, SIGNATURE, FILE_UPLOAD,        // 媒体/文件
    GPS, BARCODE,                                // 特殊
    RATING, PASS_FAIL, CHECKLIST;                // 评分专用
}
```

#### ResponseSet（聚合根 — 可复用选项集）

```java
public class ResponseSet extends AggregateRoot<Long> {
    private Long id;
    private String setCode;
    private String setName;
    private Boolean isGlobal;
    private Boolean isEnabled;
}

public class ResponseSetOption implements Entity<Long> {
    private Long id;
    private Long responseSetId;
    private String optionValue;
    private String optionLabel;
    private String optionColor;     // hex
    private BigDecimal score;       // 关联分数
    private Boolean isFlagged;      // 选中时标记为问题项→触发整改
    private Integer sortOrder;
}
```

#### TemplateCatalog（聚合根 — 树形分类）

```java
public class TemplateCatalog extends AggregateRoot<Long> {
    private Long id;
    private Long parentId;
    private String catalogCode;
    private String catalogName;
    private String description;
    private String icon;
    private Integer sortOrder;
    private Boolean isEnabled;
}
```

---

### 3.2 Scoring BC — 评分引擎

#### 包结构

```
domain/inspection/model/v7/scoring/
├── ScoringProfile.java      (AggregateRoot)
├── ScoreDimension.java      (Entity)
├── GradeBand.java           (Entity)
├── CalculationRule.java     (Entity)
├── RuleType.java            (Enum)
├── AggregationMethod.java   (Enum)
└── NormalizationMode.java   (Enum) ★ 新增
```

#### ScoringProfile（聚合根）

```java
public class ScoringProfile extends AggregateRoot<Long> {
    private Long id;
    private Long templateId;
    private BigDecimal baseScore;        // 默认100
    private BigDecimal maxScore;         // 默认100
    private BigDecimal minScore;         // 默认0
    private Boolean allowNegative;
    private Integer precision;           // 小数位数，默认2
    private AggregationMethod aggregationMethod; // SUM|WEIGHTED_AVG|MIN|MAX
    private String formulaEngine;        // JAVASCRIPT|EXPRESSION
    private String defaultNormalization; // ★ JSON: 全局默认归一化配置
}
```

#### ScoreDimension（实体 — 评分维度）

```java
public class ScoreDimension implements Entity<Long> {
    private Long id;
    private Long scoringProfileId;
    private String dimensionCode;     // safety, hygiene, discipline
    private String dimensionName;
    private Integer weight;           // 0-100, 所有维度之和 = 100
    private BigDecimal baseScore;
    private BigDecimal passThreshold;
    private String formula;           // JS公式（可选）
    private Integer sortOrder;
}
```

#### GradeBand（实体 — 等级映射）

```java
public class GradeBand implements Entity<Long> {
    private Long id;
    private Long scoringProfileId;
    private Long dimensionId;      // null = 总分等级
    private String gradeCode;      // A, B, C, D
    private String gradeName;      // 优秀, 良好, 合格, 不合格
    private BigDecimal minScore;
    private BigDecimal maxScore;
    private String color;
    private String icon;
    private Integer sortOrder;
}
```

#### CalculationRule（实体 — 后处理规则链）

```java
public class CalculationRule implements Entity<Long> {
    private Long id;
    private Long scoringProfileId;
    private String ruleCode;
    private String ruleName;
    private Integer priority;      // 执行顺序
    private RuleType ruleType;
    private String config;         // JSON: 规则参数
    private Boolean isEnabled;
}
```

#### FormulaEvaluator（领域接口）

```
FormulaEvaluator (Domain Interface)
  └── evaluate(formula: String, variables: Map<String,Object>): BigDecimal

ScoreCalculationDomainService 依赖 FormulaEvaluator 接口，不直接使用 GraalVM
GraalVmFormulaEvaluator (Infrastructure Implementation) — 在基础设施层实现
```

```java
// domain/inspection/service/FormulaEvaluator.java
public interface FormulaEvaluator {
    BigDecimal evaluate(String formula, Map<String, Object> variables);
}
```

#### RuleType 枚举（9 种） ★ 从7种扩展到9种

```java
public enum RuleType {
    CEILING,              // 封顶 {"maxScore":100}
    FLOOR,                // 保底 {"minScore":0}
    VETO,                 // 一票否决 {"vetoItems":["fire_exit_blocked"],"vetoScore":0}
    PROGRESSIVE,          // 累进扣分 {"thresholds":[{"count":3,"penalty":5}]}
    BONUS,                // 奖励加分 {"bonusItems":["extra_clean"],"bonusScore":5}
    TIME_DECAY,           // 时效衰减 {"decayDays":7,"decayRate":0.1}
    POPULATION_NORMALIZE, // ★ 人数归一化(全局兜底) {"mode":"PER_CAPITA","baseline":40}
    REPEAT_OFFENSE,       // ★ 重复违规累进 (见3.7节)
    CUSTOM;               // 自定义JS公式 {"formula":"..."}
}
```

---

### 3.3 Execution BC — 检查执行

#### 包结构

```
domain/inspection/model/v7/execution/
├── InspProject.java           (AggregateRoot)
├── InspTask.java              (AggregateRoot)
├── InspSubmission.java        (AggregateRoot — 独立聚合根，通过 taskId 跨聚合引用)
├── SubmissionDetail.java      (Entity — InspSubmission 内部)
├── InspEvidence.java          (Entity — InspSubmission 内部)
├── ProjectStatus.java         (Enum)
├── TaskStatus.java            (Enum)
├── SubmissionStatus.java      (Enum)
├── AssignmentMode.java        (Enum)
├── PopulationSource.java      (Enum) ★ 新增
└── CycleType.java             (Enum)
```

#### InspProject（聚合根）

```java
public class InspProject extends AggregateRoot<Long> {
    private Long id;
    private String projectCode;
    private String projectName;
    private String description;
    private Long templateId;
    private Long templateVersionId;
    private Long scoringProfileId;

    // 范围配置
    private ScopeType scopeType;
    private String scopeConfig;        // JSON
    private TargetType targetType;

    // 周期配置
    private LocalDate startDate;
    private LocalDate endDate;
    private CycleType cycleType;
    private String cycleConfig;        // JSON
    private String timeSlots;          // JSON
    private Boolean skipHolidays;
    private Long holidayCalendarId;
    private String excludedDates;      // JSON

    // 分配配置
    private AssignmentMode assignmentMode;
    private Boolean reviewRequired;
    private Boolean autoPublish;

    // ★ 人数归一化配置
    private PopulationSource populationSource; // AUTO|MANUAL|FIXED
    private String populationConfig;           // JSON: {"fixedValue":40,"autoField":"student_count"}

    // ★ SLA 配置
    private String slaConfig;          // JSON: 响应时限+解决时限+升级链

    // 状态
    private ProjectStatus status;      // DRAFT→ACTIVE→PAUSED→COMPLETED→ARCHIVED
    private LocalDateTime publishedAt;
    private LocalDateTime pausedAt;
    private LocalDateTime completedAt;
    private Integer totalTasks;
    private Integer completedTasks;

    // 状态机行为: create, publish, pause, resume, complete, archive, updateConfig
}
```

**PopulationSource 枚举：**

```java
public enum PopulationSource {
    AUTO,      // 自动从 org_units/school_classes 表拉取
    MANUAL,    // 检查员执行时手动输入
    FIXED      // 项目级别统一配置固定值
}
```

#### InspTask（聚合根）

> **注意**: InspSubmission 为独立聚合根，通过 taskId 引用。InspTask 的 completedTargets/skippedTargets 通过 SubmissionCompletedEvent 异步更新。

```java
public class InspTask extends AggregateRoot<Long> {
    private Long id;
    private String taskCode;
    private Long projectId;
    private LocalDate taskDate;
    private String timeSlotCode;
    private LocalTime timeSlotStart;
    private LocalTime timeSlotEnd;
    private Long inspectorId;
    private String inspectorName;
    private Long reviewerId;
    private String reviewerName;
    private TaskStatus status;
    // PENDING→CLAIMED→IN_PROGRESS→SUBMITTED→UNDER_REVIEW→REVIEWED→PUBLISHED|CANCELLED|EXPIRED
    private Integer totalTargets;
    private Integer completedTargets;    // 通过 SubmissionCompletedEvent 异步更新
    private Integer skippedTargets;      // 通过 SubmissionCompletedEvent 异步更新
    private LocalDateTime claimedAt, startedAt, submittedAt, reviewedAt, publishedAt;

    // 状态机: claim, start, submit, review, approveReview, publish, cancel, expire
}
```

#### InspSubmission（独立聚合根 — 每个检查目标的提交）

```java
public class InspSubmission extends AggregateRoot<Long> {
    private Long id;
    private Long taskId;
    private TargetType targetType;
    private Long targetId;
    private String targetName;
    private Long orgUnitId;
    private String orgUnitName;
    private BigDecimal weightRatio;
    private Integer population;          // ★ 新增：目标实际人数
    private SubmissionStatus status;     // PENDING→LOCKED→IN_PROGRESS→COMPLETED|SKIPPED
    private String formData;             // JSON: 完整表单响应
    private String scoreBreakdown;       // JSON: 各维度分数
    private BigDecimal baseScore;
    private BigDecimal finalScore;
    private BigDecimal rawScore;         // ★ 新增：归一化前原始分
    private BigDecimal deductionTotal;
    private BigDecimal bonusTotal;
    private String grade;
    private Boolean passed;
    private Integer syncVersion;         // 离线同步版本号
    private Long lockedBy;
    private LocalDateTime lockedAt;
    private LocalDateTime completedAt;

    // 行为: lock, unlock, startEditing, saveFormData, complete, skip
}
```

#### SubmissionDetail（实体）

```java
public class SubmissionDetail implements Entity<Long> {
    private Long id;
    private Long submissionId;
    private Long templateItemId;
    private String itemCode;
    private String itemName;
    private Long sectionId;
    private String sectionName;
    private ItemType itemType;
    private String responseValue;     // JSON
    private String scoringMode;
    private BigDecimal score;
    private BigDecimal rawScore;      // ★ 归一化前原始分
    private BigDecimal normFactor;    // ★ 归一化系数
    private String dimensions;        // JSON
    private Boolean isFlagged;
    private String flagReason;
    private String remark;
}
```

#### InspEvidence（实体）

```java
public class InspEvidence implements Entity<Long> {
    private Long id;
    private Long submissionId;
    private Long detailId;
    private String evidenceType;     // PHOTO|VIDEO|DOCUMENT|SIGNATURE|GPS_POINT
    private String fileName;
    private String filePath;
    private String fileUrl;
    private Long fileSize;
    private String mimeType;
    private String thumbnailUrl;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private LocalDateTime capturedAt;
    private String metadata;         // JSON
}
```

---

### 3.4 Corrective BC — CAPA 管理 ★ 大幅增强

#### 包结构

```
domain/inspection/model/v7/corrective/
├── CorrectiveCase.java        (AggregateRoot)
├── IssueCategory.java         (AggregateRoot) ★ 新增
├── CaseStatus.java            (Enum)
├── CasePriority.java          (Enum)
├── RcaMethod.java             (Enum) ★ 新增
└── EffectivenessStatus.java   (Enum) ★ 新增
```

#### CorrectiveCase（聚合根 — 完整 CAPA）

```java
public class CorrectiveCase extends AggregateRoot<Long> {
    private Long id;
    private String caseCode;
    private Long submissionId;
    private Long detailId;
    private Long projectId;
    private Long taskId;
    private TargetType targetType;
    private Long targetId;
    private String targetName;

    // 问题描述
    private String issueDescription;
    private String requiredAction;
    private String issueCategoryId;       // ★ → IssueCategory
    private String deficiencyCode;        // ★ 缺陷代码

    // ★ 根因分析 (RCA)
    private RcaMethod rcaMethod;          // NONE|FIVE_WHYS|FISHBONE|FREE_TEXT
    private String rcaData;               // JSON: 结构化根因数据

    // ★ 预防措施 (CAPA 的 PA)
    private String preventiveAction;

    // 优先级与时限
    private CasePriority priority;        // LOW|MEDIUM|HIGH|CRITICAL
    private LocalDate deadline;
    private Long assigneeId;
    private String assigneeName;
    private Integer escalationLevel;

    // 状态机 (扩展)
    private CaseStatus status;
    // OPEN→ASSIGNED→IN_PROGRESS→SUBMITTED→VERIFIED|REJECTED→CLOSED
    //                                                        →EFFECTIVENESS_PENDING
    //                                                        →EFFECTIVENESS_CONFIRMED|EFFECTIVENESS_FAILED

    // 整改证据
    private String correctionNote;
    private String correctionEvidenceIds; // JSON
    private LocalDateTime correctedAt;

    // 验证
    private Long verifierId;
    private String verifierName;
    private LocalDateTime verifiedAt;
    private String verificationNote;

    // ★ 效果验证
    private LocalDate effectivenessCheckDate;
    private EffectivenessStatus effectivenessStatus; // PENDING|CONFIRMED|FAILED
    private String effectivenessNote;

    // === 状态机行为 ===
    public static CorrectiveCase create(...) { ... }
    public void assign(...) { ... }
    public void startWork() { ... }
    public void submitCorrection(...) { ... }
    public void verify(...) {
        // SUBMITTED → VERIFIED
        // 自动设置 effectivenessCheckDate = now + SLA天数(按priority)
    }
    public void reject(...) { ... }
    public void resubmit(...) { ... }
    public void close() {
        // VERIFIED → CLOSED
        // effectivenessStatus = PENDING
        // registerEvent(CaseClosedEvent → EffectivenessScheduler)
    }
    public void confirmEffectiveness(String note) {
        // CLOSED(effectivenessPENDING) → effectivenessCONFIRMED
    }
    public void failEffectiveness(String note) {
        // CLOSED(effectivenessPENDING) → effectivenessFAILED
        // 重新打开: status → OPEN, escalationLevel++
        // registerEvent(EffectivenessFailedEvent)
    }
    public void escalate() { ... }
    public void setRootCauseAnalysis(RcaMethod method, String rcaData) { ... }
    public boolean isOverdue() { ... }
}
```

**状态机：**
```
OPEN → ASSIGNED → IN_PROGRESS → SUBMITTED → VERIFIED → CLOSED
                                     │           │         │
                                     │       REJECTED      ├→ effectiveness PENDING
                                     │           │         │     ├→ CONFIRMED (终态)
                                     │       resubmit()    │     └→ FAILED → 重新OPEN
                                     └── escalate() ──────→│
```

**RcaMethod 枚举：**

```java
public enum RcaMethod {
    NONE,           // 无根因分析
    FIVE_WHYS,      // 5个为什么
    FISHBONE,       // 鱼骨图/石川图 (6M分类)
    FREE_TEXT        // 自由文本
}
```

**rcaData JSON 结构：**

```jsonc
// FIVE_WHYS
{
  "chain": [
    { "level": 1, "question": "为什么地面有垃圾？", "answer": "值日生没有打扫" },
    { "level": 2, "question": "为什么值日生没打扫？", "answer": "值日表没更新" },
    { "level": 3, "question": "根本原因", "answer": "缺少值日管理容错机制" }
  ],
  "rootCause": "缺少值日管理容错机制"
}

// FISHBONE (6M)
{
  "categories": {
    "人员(Man)": ["值日生责任心不足", "缺少监督"],
    "方法(Method)": ["值日表更新流程缺失"],
    "环境(Environment)": ["垃圾桶数量不足"],
    "管理(Management)": ["班主任出差无替代方案"],
    "材料(Material)": [],
    "设备(Machine)": []
  },
  "rootCause": "值日管理流程缺少容错机制"
}
```

#### IssueCategory（聚合根 — 问题分类字典）★ 新增

```java
public class IssueCategory extends AggregateRoot<Long> {
    private Long id;
    private Long parentId;           // 树形
    private String categoryCode;     // ENV-001, SAFETY-002
    private String categoryName;     // 环境卫生-地面清洁
    private String description;
    private String severity;         // LOW|MEDIUM|HIGH|CRITICAL
    private Integer sortOrder;
    private Boolean isEnabled;
}
```

#### CorrectiveCase 证据存储

整改证据复用 `insp_evidences` 表的多态模式：`entity_type` + `entity_id` 区分关联实体。

- 检查提交证据：`entity_type='SUBMISSION'`, `entity_id=submissionId`
- 整改证据：`entity_type='CORRECTIVE_CASE'`, `entity_id=caseId`
- 效果验证证据：`entity_type='EFFECTIVENESS_REVIEW'`, `entity_id=caseId`

> `insp_evidences` 表原有的 `submission_id`+`detail_id` 列保留用于检查提交场景，整改场景使用 `entity_type`+`entity_id` 通用字段。

---

### 3.5 Analytics BC — CQRS 读侧 ★ 扩展

#### 包结构

```
domain/inspection/model/v7/analytics/
├── DailySummary.java          (读模型)
├── PeriodSummary.java         (读模型)
└── InspectorSummary.java      (读模型) ★ 新增
```

#### DailySummary（读模型）

```java
public class DailySummary {
    private Long id;
    private Long projectId;
    private LocalDate summaryDate;
    private TargetType targetType;
    private Long targetId;
    private String targetName;
    private Long orgUnitId;
    private String orgUnitName;
    private Integer population;          // ★ 目标人数
    private Integer inspectionCount;
    private BigDecimal avgScore;
    private BigDecimal rawAvgScore;      // ★ 归一化前原始均分
    private BigDecimal minScore;
    private BigDecimal maxScore;
    private BigDecimal totalDeductions;
    private BigDecimal totalBonuses;
    private Integer passCount;
    private Integer failCount;
    private Integer ranking;
    private String dimensionScores;     // JSON
    private String grade;
    private LocalDateTime calculatedAt;
}
```

#### PeriodSummary（读模型）

```java
public class PeriodSummary {
    private Long id;
    private Long projectId;
    private String periodType;        // WEEKLY|MONTHLY|QUARTERLY|YEARLY
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private TargetType targetType;
    private Long targetId;
    private String targetName;
    private Long orgUnitId;
    private String orgUnitName;
    private Integer inspectionDays;
    private BigDecimal avgScore;
    private BigDecimal minScore;
    private BigDecimal maxScore;
    private BigDecimal scoreStdDev;
    private String trendDirection;     // UP|DOWN|STABLE
    private BigDecimal trendPercent;
    private Integer ranking;
    private String dimensionScores;    // JSON
    private String grade;
    private Integer correctiveCount;
    private Integer correctiveClosedCount;
    private LocalDateTime calculatedAt;
}
```

#### InspectorSummary（读模型）★ 新增

```java
public class InspectorSummary {
    private Long id;
    private Long projectId;
    private String periodType;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Long inspectorId;
    private String inspectorName;
    private Integer totalTasks;
    private Integer completedTasks;
    private BigDecimal completionRate;      // 完成率%
    private Integer avgDurationMinutes;     // 平均检查用时
    private Integer totalSubmissions;
    private Integer totalFlaggedItems;
    private BigDecimal avgScoreGiven;       // 平均给出分数
    private BigDecimal scoreStdDev;         // 给分标准差(越小越稳定)
    private BigDecimal onTimeRate;          // 按时提交率%
    private LocalDateTime calculatedAt;
}
```

---

### 3.6 Platform BC — 平台服务 ★ 新增

#### 包结构

```
domain/inspection/model/v7/platform/
├── NotificationRule.java      (AggregateRoot)
├── ReportTemplate.java        (AggregateRoot)
├── WebhookSubscription.java   (AggregateRoot)
├── AuditTrailEntry.java       (不可变数据记录 — 有id，按DDD为Entity，但无行为)
└── InspV7DomainEvent.java     (标记接口 — 所有V7领域事件实现)
```

#### NotificationRule（聚合根 — 可配置通知规则）

```java
public class NotificationRule extends AggregateRoot<Long> {
    private Long id;
    private String ruleName;
    private String triggerEvent;       // 触发事件类型
    private String conditions;         // JSON: 条件 {"finalScore":{"lt":60}}
    private String recipientType;      // ROLE|USER|INSPECTOR|REVIEWER|ASSIGNEE
    private String recipientConfig;    // JSON: 具体接收人
    private String channels;           // JSON: ["IN_APP","EMAIL","SMS","WECHAT"]
    private String messageTemplate;    // 消息模板(支持变量 {{xxx}})
    private Boolean isEnabled;
    private Long projectId;            // null = 全局规则
}
```

#### ReportTemplate（聚合根 — PDF报告模板）

```java
public class ReportTemplate extends AggregateRoot<Long> {
    private Long id;
    private String templateName;
    private String reportType;         // SUBMISSION|DAILY|PERIOD|CORRECTIVE
    private String layoutConfig;       // JSON: 页眉页脚/logo/颜色/字体
    private String sections;           // JSON: 报告包含的区块列表
    private Boolean isDefault;
}
```

**layoutConfig 示例：**
```jsonc
{
  "pageSize": "A4",
  "orientation": "portrait",
  "header": { "logo": "/uploads/logo.png", "title": "{{projectName}} 检查报告" },
  "footer": { "text": "{{orgName}} - 机密文件", "showPageNumber": true },
  "theme": { "primaryColor": "#1890ff", "fontFamily": "SimSun" },
  "sections": [
    "BASIC_INFO", "SCORE_SUMMARY", "DIMENSION_RADAR",
    "SECTION_BREAKDOWN", "FLAGGED_ITEMS", "EVIDENCE_GALLERY",
    "CORRECTIVE_ACTIONS", "INSPECTOR_SIGNATURE", "REVIEWER_SIGNATURE"
  ]
}
```

#### WebhookSubscription（聚合根 — 外部集成）

```java
public class WebhookSubscription extends AggregateRoot<Long> {
    private Long id;
    private String name;
    private String url;                // 回调URL
    private String secret;             // HMAC签名密钥
    private String eventTypes;         // JSON: ["TaskPublishedEvent","CaseCreatedEvent"]
    private Boolean isActive;
    private LocalDateTime lastTriggeredAt;
    private Integer failureCount;
}
```

#### InspV7DomainEvent（标记接口）

```java
/**
 * 所有 V7 领域事件实现此标记接口。
 * NotificationRuleEngine 和 WebhookDispatcher 仅监听 InspV7DomainEvent 子类型。
 */
public interface InspV7DomainEvent {
    // marker interface
}
```

#### AuditTrailEntry（不可变数据记录）

> 有 id，按 DDD 严格定义是 Entity，但无行为方法，作为普通不可变数据记录处理。

```java
public class AuditTrailEntry {
    private Long id;
    private Long actorId;
    private String actorName;
    private String action;            // CREATE|UPDATE|DELETE|STATUS_CHANGE|PUBLISH|VERIFY
    private String entityType;        // TEMPLATE|PROJECT|TASK|SUBMISSION|CORRECTIVE_CASE
    private Long entityId;
    private String entityCode;
    private String oldValue;          // JSON
    private String newValue;          // JSON
    private String ipAddress;
    private String userAgent;
    private LocalDateTime occurredAt;
    // 无 setter，仅追加，不可修改/删除
}
```

---

### 3.7 人数归一化评分引擎 ★ 新增

#### 概念

某些检查项（如"未穿校服人数"）受组织人数影响，需要按人数归一化消除不公平。
每个 TemplateItem 的 `scoringConfig` 可独立配置是否启用归一化。

#### scoringConfig JSON 完整结构

```jsonc
{
  "dimensionId": 1,
  "scoringMode": "DEDUCTION",
  "score": -2,
  "weight": 10,
  "normalization": {
    "enabled": true,
    "mode": "PER_CAPITA",        // NONE|PER_CAPITA|RATE_BASED|SQRT_ADJUSTED|CUSTOM
    "baselinePopulation": 40,    // 基准人数
    "cappedAt": 3.0,             // 系数上限
    "floorAt": 0.3,              // 系数下限
    "customFormula": null         // CUSTOM模式时的JS公式
  }
}
```

#### NormalizationMode 枚举（5 种）

```java
public enum NormalizationMode {
    NONE,           // 不归一化: score × quantity
    PER_CAPITA,     // 人均比例: score × quantity × (baseline / population)
    RATE_BASED,     // 违规率: score × (quantity / population) × baseline
    SQRT_ADJUSTED,  // 平方根折中: score × quantity × sqrt(baseline / population)
    CUSTOM          // 自定义JS公式: f(quantity, population, baseline, score)
}
```

**计算效果对比（基准40人，扣分-2/人，3人违规）：**

| 模式 | 10人班 | 50人班 | 公平性 |
|------|--------|--------|--------|
| NONE | -6 | -6 | 不公平 |
| PER_CAPITA | -24 | -4.8 | 公平 |
| RATE_BASED | -24 | -4.8 | 公平 |
| SQRT_ADJUSTED | -12 | -5.37 | 折中 |

#### 优先级链

```
TemplateItem.scoringConfig.normalization > ScoringProfile.defaultNormalization > 不归一化
```

#### REPEAT_OFFENSE 规则 ★ 新增

```java
// CalculationRule config:
{
  "lookbackDays": 30,           // 回溯30天
  "matchBy": "item_code",       // 按检查项匹配
  "thresholds": [
    { "occurrences": 2, "multiplier": 1.5 },   // 第2次: 扣分×1.5
    { "occurrences": 3, "multiplier": 2.0 },   // 第3次: 扣分×2.0
    { "occurrences": 5, "multiplier": 3.0 }    // 第5次: 扣分×3.0
  ]
}
```

#### ScoreCalculationDomainService 完整流程

```
1. 各 item 按 scoringConfig 计算原始分
2. 对启用 normalization 的 item，计算归一化系数
3. 系数裁剪到 [floorAt, cappedAt] 区间
4. 按 ScoreDimension 聚合各维度分数 (SUM/WEIGHTED_AVG/MIN/MAX)
5. 按 CalculationRule.priority 顺序执行规则链:
   - POPULATION_NORMALIZE: 全局兜底归一化
   - REPEAT_OFFENSE: 查历史数据，乘累进系数
   - VETO: 一票否决检查
   - PROGRESSIVE: 累进扣分检查
   - CEILING/FLOOR: 封顶保底
   - BONUS/TIME_DECAY/CUSTOM: 其他规则
6. 按 GradeBand 映射等级
7. 返回 ScoreResult (finalScore, rawScore, dimensionScores, grade, normFactors, ruleApplications)
```

---

## 4. 数据库设计

### 4.1 表清单（29 张表）

所有表包含 `tenant_id BIGINT NOT NULL DEFAULT 0` 和 `deleted TINYINT DEFAULT 0`。
表前缀统一 `insp_`。

| # | 表名 | 所属 BC | 用途 |
|---|------|--------|------|
| 1 | `insp_template_catalogs` | Template | 模板分类目录（树形） |
| 2 | `insp_templates` | Template | 模板定义 |
| 3 | `insp_template_versions` | Template | 不可变版本快照 |
| 4 | `insp_template_sections` | Template | 模板分区（层级） |
| 5 | `insp_template_items` | Template | 模板字段（22 种） |
| 6 | `insp_response_sets` | Template | 可复用选项集 |
| 7 | `insp_response_set_options` | Template | 选项集选项 |
| 8 | `insp_scoring_profiles` | Scoring | 评分配置 |
| 9 | `insp_score_dimensions` | Scoring | 评分维度 |
| 10 | `insp_grade_bands` | Scoring | 等级映射 |
| 11 | `insp_calculation_rules` | Scoring | 计算规则链 |
| 12 | `insp_projects` | Execution | 检查项目 |
| 13 | `insp_tasks` | Execution | 检查任务 |
| 14 | `insp_submissions` | Execution | 提交（每目标） |
| 15 | `insp_submission_details` | Execution | 明细（每评分项） |
| 16 | `insp_evidences` | Execution | 证据文件 |
| 17 | `insp_project_inspectors` | Execution | 检查员池 |
| 18 | `insp_holiday_calendars` | Execution | 假日日历 |
| 19 | `insp_corrective_cases` | Corrective | CAPA 整改案例 |
| 20 | `insp_issue_categories` | Corrective | ★ 问题分类字典 |
| 21 | `insp_daily_summaries` | Analytics | 日汇总 |
| 22 | `insp_period_summaries` | Analytics | 周期汇总 |
| 23 | `insp_inspector_summaries` | Analytics | ★ 检查员绩效 |
| 24 | `insp_item_frequency_summaries` | Analytics | ★ item频次汇总(Pareto分析) |
| 25 | `insp_corrective_summaries` | Analytics | ★ 整改汇总 |
| 26 | `insp_notification_rules` | Platform | ★ 通知规则 |
| 27 | `insp_report_templates` | Platform | ★ PDF报告模板 |
| 28 | `insp_webhook_subscriptions` | Platform | ★ Webhook订阅 |
| 29 | `insp_audit_trail` | Platform | ★ 不可变审计日志 |

### 4.2 建表 DDL

```sql
-- ============================================================
-- Migration: V30.0.0__insp_v7_schema.sql
-- Description: V7 检查平台模块完整建表 (29张表)
-- ============================================================

-- -----------------------------------------------------------
-- 1. Template BC (7 tables)
-- -----------------------------------------------------------

CREATE TABLE IF NOT EXISTS `insp_template_catalogs` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`    BIGINT       NOT NULL DEFAULT 0,
    `parent_id`    BIGINT       NULL     COMMENT '父分类ID，NULL=根分类',
    `catalog_code` VARCHAR(50)  NOT NULL,
    `catalog_name` VARCHAR(100) NOT NULL,
    `description`  VARCHAR(500) NULL,
    `icon`         VARCHAR(50)  NULL,
    `sort_order`   INT          NOT NULL DEFAULT 0,
    `is_enabled`   TINYINT      NOT NULL DEFAULT 1,
    `created_by`   BIGINT       NULL,
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_by`   BIGINT       NULL,
    `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_tenant_parent` (`tenant_id`, `parent_id`),
    UNIQUE INDEX `uk_tenant_code` (`tenant_id`, `catalog_code`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 模板分类目录';

CREATE TABLE IF NOT EXISTS `insp_templates` (
    `id`                      BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`               BIGINT       NOT NULL DEFAULT 0,
    `template_code`           VARCHAR(50)  NOT NULL,
    `template_name`           VARCHAR(200) NOT NULL,
    `description`             TEXT         NULL,
    `catalog_id`              BIGINT       NULL,
    `tags`                    JSON         NULL     COMMENT '["安全","卫生"]',
    `applicable_target_types` JSON         NULL     COMMENT '["ORG","PLACE"]',
    `latest_version`          INT          NOT NULL DEFAULT 0,
    `status`                  VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    `is_default`              TINYINT      NOT NULL DEFAULT 0,
    `use_count`               INT          NOT NULL DEFAULT 0,
    `last_used_at`            DATETIME     NULL,
    `created_by`              BIGINT       NULL,
    `created_at`              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_by`              BIGINT       NULL,
    `updated_at`              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`                 TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_tenant_status` (`tenant_id`, `status`),
    UNIQUE INDEX `uk_tenant_code` (`tenant_id`, `template_code`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 检查模板';

CREATE TABLE IF NOT EXISTS `insp_template_versions` (
    `id`                        BIGINT   NOT NULL AUTO_INCREMENT,
    `tenant_id`                 BIGINT   NOT NULL DEFAULT 0,
    `template_id`               BIGINT   NOT NULL,
    `version`                   INT      NOT NULL,
    `structure_snapshot`        JSON     NOT NULL COMMENT '完整 sections+items 树',
    `scoring_profile_snapshot`  JSON     NULL     COMMENT '评分配置快照',
    `created_by`                BIGINT   NULL,
    `created_at`                DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted`                   TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_template_version` (`template_id`, `version`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 模板版本快照（不可变）';

CREATE TABLE IF NOT EXISTS `insp_template_sections` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`         BIGINT       NOT NULL DEFAULT 0,
    `template_id`       BIGINT       NOT NULL,
    `parent_section_id` BIGINT       NULL,
    `section_code`      VARCHAR(50)  NOT NULL,
    `section_name`      VARCHAR(200) NOT NULL,
    `sort_order`        INT          NOT NULL DEFAULT 0,
    `weight`            INT          NOT NULL DEFAULT 100,
    `is_repeatable`     TINYINT      NOT NULL DEFAULT 0,
    `condition_logic`   JSON         NULL,
    `created_by`        BIGINT       NULL,
    `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_by`        BIGINT       NULL,
    `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`           TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_template` (`template_id`),
    INDEX `idx_parent` (`parent_section_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 模板分区（层级）';

CREATE TABLE IF NOT EXISTS `insp_template_items` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`       BIGINT       NOT NULL DEFAULT 0,
    `section_id`      BIGINT       NOT NULL,
    `item_code`       VARCHAR(50)  NOT NULL,
    `item_name`       VARCHAR(200) NOT NULL,
    `description`     TEXT         NULL,
    `item_type`       VARCHAR(30)  NOT NULL,
    `config`          JSON         NULL     COMMENT '类型特定配置',
    `validation_rules`JSON         NULL     COMMENT '验证规则数组',
    `response_set_id` BIGINT       NULL,
    `scoring_config`  JSON         NULL     COMMENT '评分+归一化配置',
    `help_content`    TEXT         NULL     COMMENT '帮助文本/参考图片URL',
    `is_required`     TINYINT      NOT NULL DEFAULT 0,
    `is_scored`       TINYINT      NOT NULL DEFAULT 0,
    `require_evidence`TINYINT      NOT NULL DEFAULT 0 COMMENT '非媒体字段是否强制附证据',
    `sort_order`      INT          NOT NULL DEFAULT 0,
    `condition_logic` JSON         NULL,
    `created_by`      BIGINT       NULL,
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_by`      BIGINT       NULL,
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_section` (`section_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 模板字段（22种类型）';

CREATE TABLE IF NOT EXISTS `insp_response_sets` (
    `id`        BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT       NOT NULL DEFAULT 0,
    `set_code`  VARCHAR(50)  NOT NULL,
    `set_name`  VARCHAR(200) NOT NULL,
    `is_global` TINYINT      NOT NULL DEFAULT 0,
    `is_enabled`TINYINT      NOT NULL DEFAULT 1,
    `created_by`BIGINT       NULL,
    `created_at`DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_by`BIGINT       NULL,
    `updated_at`DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`   TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_tenant_code` (`tenant_id`, `set_code`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 可复用选项集';

CREATE TABLE IF NOT EXISTS `insp_response_set_options` (
    `id`              BIGINT         NOT NULL AUTO_INCREMENT,
    `tenant_id`       BIGINT         NOT NULL DEFAULT 0,
    `response_set_id` BIGINT         NOT NULL,
    `option_value`    VARCHAR(100)   NOT NULL,
    `option_label`    VARCHAR(200)   NOT NULL,
    `option_color`    VARCHAR(20)    NULL,
    `score`           DECIMAL(10,2)  NULL,
    `is_flagged`      TINYINT        NOT NULL DEFAULT 0,
    `sort_order`      INT            NOT NULL DEFAULT 0,
    `created_at`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_response_set` (`response_set_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 选项集选项';

-- -----------------------------------------------------------
-- 2. Scoring BC (4 tables)
-- -----------------------------------------------------------

CREATE TABLE IF NOT EXISTS `insp_scoring_profiles` (
    `id`                      BIGINT         NOT NULL AUTO_INCREMENT,
    `tenant_id`               BIGINT         NOT NULL DEFAULT 0,
    `template_id`             BIGINT         NOT NULL,
    `base_score`              DECIMAL(10,2)  NOT NULL DEFAULT 100.00,
    `max_score`               DECIMAL(10,2)  NOT NULL DEFAULT 100.00,
    `min_score`               DECIMAL(10,2)  NOT NULL DEFAULT 0.00,
    `allow_negative`          TINYINT        NOT NULL DEFAULT 0,
    `precision_digits`        INT            NOT NULL DEFAULT 2,
    `aggregation_method`      VARCHAR(20)    NOT NULL DEFAULT 'WEIGHTED_AVG',
    `formula_engine`          VARCHAR(20)    NOT NULL DEFAULT 'EXPRESSION',
    `default_normalization`   JSON           NULL     COMMENT '全局默认归一化配置',
    `created_by`              BIGINT         NULL,
    `created_at`              DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_by`              BIGINT         NULL,
    `updated_at`              DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`                 TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_template` (`template_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 评分配置';

CREATE TABLE IF NOT EXISTS `insp_score_dimensions` (
    `id`                 BIGINT         NOT NULL AUTO_INCREMENT,
    `tenant_id`          BIGINT         NOT NULL DEFAULT 0,
    `scoring_profile_id` BIGINT         NOT NULL,
    `dimension_code`     VARCHAR(50)    NOT NULL,
    `dimension_name`     VARCHAR(100)   NOT NULL,
    `weight`             INT            NOT NULL DEFAULT 100,
    `base_score`         DECIMAL(10,2)  NOT NULL DEFAULT 100.00,
    `pass_threshold`     DECIMAL(10,2)  NULL,
    `formula`            TEXT           NULL,
    `sort_order`         INT            NOT NULL DEFAULT 0,
    `created_at`         DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`         DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`            TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_profile` (`scoring_profile_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 评分维度';

CREATE TABLE IF NOT EXISTS `insp_grade_bands` (
    `id`                 BIGINT         NOT NULL AUTO_INCREMENT,
    `tenant_id`          BIGINT         NOT NULL DEFAULT 0,
    `scoring_profile_id` BIGINT         NOT NULL,
    `dimension_id`       BIGINT         NULL,
    `grade_code`         VARCHAR(10)    NOT NULL,
    `grade_name`         VARCHAR(50)    NOT NULL,
    `min_score`          DECIMAL(10,2)  NOT NULL,
    `max_score`          DECIMAL(10,2)  NOT NULL,
    `color`              VARCHAR(20)    NULL,
    `icon`               VARCHAR(50)    NULL,
    `sort_order`         INT            NOT NULL DEFAULT 0,
    `created_at`         DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`         DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`            TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_profile` (`scoring_profile_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 等级映射';

CREATE TABLE IF NOT EXISTS `insp_calculation_rules` (
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`          BIGINT       NOT NULL DEFAULT 0,
    `scoring_profile_id` BIGINT       NOT NULL,
    `rule_code`          VARCHAR(50)  NOT NULL,
    `rule_name`          VARCHAR(100) NOT NULL,
    `priority`           INT          NOT NULL DEFAULT 0,
    `rule_type`          VARCHAR(30)  NOT NULL COMMENT '9种规则类型',
    `config`             JSON         NOT NULL,
    `is_enabled`         TINYINT      NOT NULL DEFAULT 1,
    `created_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`            TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_profile_priority` (`scoring_profile_id`, `priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 计算规则链（9种类型）';

-- -----------------------------------------------------------
-- 3. Execution BC (7 tables)
-- -----------------------------------------------------------

CREATE TABLE IF NOT EXISTS `insp_projects` (
    `id`                    BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`             BIGINT       NOT NULL DEFAULT 0,
    `project_code`          VARCHAR(50)  NOT NULL,
    `project_name`          VARCHAR(200) NOT NULL,
    `description`           TEXT         NULL,
    `template_id`           BIGINT       NOT NULL,
    `template_version_id`   BIGINT       NULL,
    `scoring_profile_id`    BIGINT       NULL,
    `scope_type`            VARCHAR(20)  NOT NULL,
    `scope_config`          JSON         NULL,
    `target_type`           VARCHAR(20)  NOT NULL,
    `start_date`            DATE         NOT NULL,
    `end_date`              DATE         NULL,
    `cycle_type`            VARCHAR(20)  NOT NULL DEFAULT 'DAILY',
    `cycle_config`          JSON         NULL,
    `time_slots`            JSON         NULL,
    `skip_holidays`         TINYINT      NOT NULL DEFAULT 0,
    `holiday_calendar_id`   BIGINT       NULL,
    `excluded_dates`        JSON         NULL,
    `assignment_mode`       VARCHAR(20)  NOT NULL DEFAULT 'FREE',
    `review_required`       TINYINT      NOT NULL DEFAULT 1,
    `auto_publish`          TINYINT      NOT NULL DEFAULT 0,
    `population_source`     VARCHAR(20)  NOT NULL DEFAULT 'AUTO' COMMENT 'AUTO|MANUAL|FIXED',
    `population_config`     JSON         NULL     COMMENT '人数配置',
    `sla_config`            JSON         NULL     COMMENT 'SLA时限+升级链配置',
    `status`                VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    `published_at`          DATETIME     NULL,
    `paused_at`             DATETIME     NULL,
    `completed_at`          DATETIME     NULL,
    `total_tasks`           INT          NOT NULL DEFAULT 0,
    `completed_tasks`       INT          NOT NULL DEFAULT 0,
    `created_by`            BIGINT       NULL,
    `created_at`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_by`            BIGINT       NULL,
    `updated_at`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`               TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_tenant_code` (`tenant_id`, `project_code`, `deleted`),
    INDEX `idx_tenant_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 检查项目';

CREATE TABLE IF NOT EXISTS `insp_tasks` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`         BIGINT       NOT NULL DEFAULT 0,
    `task_code`         VARCHAR(50)  NOT NULL,
    `project_id`        BIGINT       NOT NULL,
    `task_date`         DATE         NOT NULL,
    `time_slot_code`    VARCHAR(50)  NULL,
    `time_slot_start`   TIME         NULL,
    `time_slot_end`     TIME         NULL,
    `inspector_id`      BIGINT       NULL,
    `inspector_name`    VARCHAR(100) NULL,
    `reviewer_id`       BIGINT       NULL,
    `reviewer_name`     VARCHAR(100) NULL,
    `status`            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    `total_targets`     INT          NOT NULL DEFAULT 0,
    `completed_targets` INT          NOT NULL DEFAULT 0,
    `skipped_targets`   INT          NOT NULL DEFAULT 0,
    `claimed_at`        DATETIME     NULL,
    `started_at`        DATETIME     NULL,
    `submitted_at`      DATETIME     NULL,
    `reviewed_at`       DATETIME     NULL,
    `published_at`      DATETIME     NULL,
    `created_by`        BIGINT       NULL,
    `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_by`        BIGINT       NULL,
    `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`           TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_task_code` (`task_code`, `deleted`),
    INDEX `idx_project_date` (`project_id`, `task_date`),
    INDEX `idx_inspector` (`inspector_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 检查任务';

CREATE TABLE IF NOT EXISTS `insp_submissions` (
    `id`               BIGINT         NOT NULL AUTO_INCREMENT,
    `tenant_id`        BIGINT         NOT NULL DEFAULT 0,
    `task_id`          BIGINT         NOT NULL,
    `target_type`      VARCHAR(20)    NOT NULL,
    `target_id`        BIGINT         NOT NULL,
    `target_name`      VARCHAR(200)   NULL,
    `org_unit_id`      BIGINT         NULL,
    `org_unit_name`    VARCHAR(200)   NULL,
    `weight_ratio`     DECIMAL(5,2)   NOT NULL DEFAULT 1.00,
    `population`       INT            NULL     COMMENT '目标实际人数（归一化用）',
    `status`           VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    `form_data`        JSON           NULL,
    `score_breakdown`  JSON           NULL,
    `base_score`       DECIMAL(10,2)  NULL,
    `final_score`      DECIMAL(10,2)  NULL,
    `raw_score`        DECIMAL(10,2)  NULL     COMMENT '归一化前原始分',
    `deduction_total`  DECIMAL(10,2)  NOT NULL DEFAULT 0.00,
    `bonus_total`      DECIMAL(10,2)  NOT NULL DEFAULT 0.00,
    `grade`            VARCHAR(10)    NULL,
    `passed`           TINYINT        NULL,
    `sync_version`     INT            NOT NULL DEFAULT 0,
    `locked_by`        BIGINT         NULL,
    `locked_at`        DATETIME       NULL,
    `completed_at`     DATETIME       NULL,
    `created_by`       BIGINT         NULL,
    `created_at`       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_by`       BIGINT         NULL,
    `updated_at`       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`          TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_task` (`task_id`),
    INDEX `idx_target` (`target_type`, `target_id`),
    INDEX `idx_org_unit` (`org_unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 检查提交（每目标）';

CREATE TABLE IF NOT EXISTS `insp_submission_details` (
    `id`                BIGINT         NOT NULL AUTO_INCREMENT,
    `tenant_id`         BIGINT         NOT NULL DEFAULT 0,
    `submission_id`     BIGINT         NOT NULL,
    `template_item_id`  BIGINT         NOT NULL,
    `item_code`         VARCHAR(50)    NOT NULL,
    `item_name`         VARCHAR(200)   NOT NULL,
    `section_id`        BIGINT         NULL,
    `section_name`      VARCHAR(200)   NULL,
    `item_type`         VARCHAR(30)    NOT NULL,
    `response_value`    JSON           NULL,
    `scoring_mode`      VARCHAR(20)    NULL,
    `score`             DECIMAL(10,2)  NULL,
    `raw_score`         DECIMAL(10,2)  NULL     COMMENT '归一化前原始分',
    `norm_factor`       DECIMAL(10,4)  NULL     COMMENT '归一化系数',
    `dimensions`        JSON           NULL,
    `is_flagged`        TINYINT        NOT NULL DEFAULT 0,
    `flag_reason`       VARCHAR(500)   NULL,
    `remark`            TEXT           NULL,
    `created_at`        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`           TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_submission` (`submission_id`),
    INDEX `idx_flagged` (`is_flagged`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 提交明细（每评分项）';

CREATE TABLE IF NOT EXISTS `insp_evidences` (
    `id`             BIGINT         NOT NULL AUTO_INCREMENT,
    `tenant_id`      BIGINT         NOT NULL DEFAULT 0,
    `submission_id`  BIGINT         NOT NULL,
    `detail_id`      BIGINT         NULL,
    `evidence_type`  VARCHAR(20)    NOT NULL,
    `file_name`      VARCHAR(300)   NOT NULL,
    `file_path`      VARCHAR(500)   NOT NULL,
    `file_url`       VARCHAR(500)   NULL,
    `file_size`      BIGINT         NULL,
    `mime_type`      VARCHAR(100)   NULL,
    `thumbnail_url`  VARCHAR(500)   NULL,
    `latitude`       DECIMAL(10,7)  NULL,
    `longitude`      DECIMAL(10,7)  NULL,
    `captured_at`    DATETIME       NULL,
    `metadata`       JSON           NULL,
    `created_by`     BIGINT         NULL,
    `created_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted`        TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_submission` (`submission_id`),
    INDEX `idx_detail` (`detail_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 证据文件';

CREATE TABLE IF NOT EXISTS `insp_project_inspectors` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`      BIGINT       NOT NULL DEFAULT 0,
    `project_id`     BIGINT       NOT NULL,
    `user_id`        BIGINT       NOT NULL,
    `user_name`      VARCHAR(100) NULL,
    `role`           VARCHAR(20)  NOT NULL DEFAULT 'INSPECTOR',
    `is_active`      TINYINT      NOT NULL DEFAULT 1,
    `assigned_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted`        TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_project_user_role` (`project_id`, `user_id`, `role`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 检查员池';

CREATE TABLE IF NOT EXISTS `insp_holiday_calendars` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`     BIGINT       NOT NULL DEFAULT 0,
    `calendar_name` VARCHAR(100) NOT NULL,
    `year`          INT          NOT NULL,
    `holidays`      JSON         NOT NULL,
    `is_default`    TINYINT      NOT NULL DEFAULT 0,
    `created_by`    BIGINT       NULL,
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_by`    BIGINT       NULL,
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_tenant_year` (`tenant_id`, `year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 假日日历';

-- -----------------------------------------------------------
-- 4. Corrective BC (2 tables)
-- -----------------------------------------------------------

CREATE TABLE IF NOT EXISTS `insp_corrective_cases` (
    `id`                       BIGINT         NOT NULL AUTO_INCREMENT,
    `tenant_id`                BIGINT         NOT NULL DEFAULT 0,
    `case_code`                VARCHAR(50)    NOT NULL,
    `submission_id`            BIGINT         NOT NULL,
    `detail_id`                BIGINT         NULL,
    `project_id`               BIGINT         NOT NULL,
    `task_id`                  BIGINT         NOT NULL,
    `target_type`              VARCHAR(20)    NOT NULL,
    `target_id`                BIGINT         NOT NULL,
    `target_name`              VARCHAR(200)   NULL,
    `issue_description`        TEXT           NOT NULL,
    `required_action`          TEXT           NULL,
    `preventive_action`        TEXT           NULL     COMMENT '预防措施(CAPA的PA)',
    `issue_category_id`        BIGINT         NULL     COMMENT '问题分类',
    `deficiency_code`          VARCHAR(50)    NULL     COMMENT '缺陷代码',
    `rca_method`               VARCHAR(20)    NULL     COMMENT 'NONE|FIVE_WHYS|FISHBONE|FREE_TEXT',
    `rca_data`                 JSON           NULL     COMMENT '结构化根因分析数据',
    `priority`                 VARCHAR(20)    NOT NULL DEFAULT 'MEDIUM',
    `deadline`                 DATE           NULL,
    `assignee_id`              BIGINT         NULL,
    `assignee_name`            VARCHAR(100)   NULL,
    `escalation_level`         INT            NOT NULL DEFAULT 0,
    `status`                   VARCHAR(20)    NOT NULL DEFAULT 'OPEN',
    `correction_note`          TEXT           NULL,
    `correction_evidence_ids`  JSON           NULL,
    `corrected_at`             DATETIME       NULL,
    `verifier_id`              BIGINT         NULL,
    `verifier_name`            VARCHAR(100)   NULL,
    `verified_at`              DATETIME       NULL,
    `verification_note`        TEXT           NULL,
    `effectiveness_check_date` DATE           NULL     COMMENT '效果验证日期',
    `effectiveness_status`     VARCHAR(20)    NULL     COMMENT 'PENDING|CONFIRMED|FAILED',
    `effectiveness_note`       TEXT           NULL,
    `created_by`               BIGINT         NULL,
    `created_at`               DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_by`               BIGINT         NULL,
    `updated_at`               DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`                  TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_case_code` (`case_code`, `deleted`),
    INDEX `idx_submission` (`submission_id`),
    INDEX `idx_project` (`project_id`),
    INDEX `idx_assignee_status` (`assignee_id`, `status`),
    INDEX `idx_deadline` (`deadline`, `status`),
    INDEX `idx_issue_category` (`issue_category_id`),
    INDEX `idx_effectiveness` (`effectiveness_check_date`, `effectiveness_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 CAPA整改案例';

CREATE TABLE IF NOT EXISTS `insp_issue_categories` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`     BIGINT       NOT NULL DEFAULT 0,
    `parent_id`     BIGINT       NULL,
    `category_code` VARCHAR(50)  NOT NULL,
    `category_name` VARCHAR(100) NOT NULL,
    `description`   VARCHAR(500) NULL,
    `severity`      VARCHAR(20)  NULL,
    `sort_order`    INT          NOT NULL DEFAULT 0,
    `is_enabled`    TINYINT      NOT NULL DEFAULT 1,
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_tenant_parent` (`tenant_id`, `parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 问题分类字典（帕累托分析基础）';

-- -----------------------------------------------------------
-- 5. Analytics BC (5 tables)
-- -----------------------------------------------------------

CREATE TABLE IF NOT EXISTS `insp_daily_summaries` (
    `id`               BIGINT         NOT NULL AUTO_INCREMENT,
    `tenant_id`        BIGINT         NOT NULL DEFAULT 0,
    `project_id`       BIGINT         NOT NULL,
    `summary_date`     DATE           NOT NULL,
    `target_type`      VARCHAR(20)    NOT NULL,
    `target_id`        BIGINT         NOT NULL,
    `target_name`      VARCHAR(200)   NULL,
    `org_unit_id`      BIGINT         NULL,
    `org_unit_name`    VARCHAR(200)   NULL,
    `org_unit_path`    VARCHAR(500)   NULL     COMMENT '组织路径(层级查询优化)',
    `population`       INT            NULL,
    `inspection_count` INT            NOT NULL DEFAULT 0,
    `avg_score`        DECIMAL(10,2)  NULL,
    `raw_avg_score`    DECIMAL(10,2)  NULL     COMMENT '归一化前原始均分',
    `min_score`        DECIMAL(10,2)  NULL,
    `max_score`        DECIMAL(10,2)  NULL,
    `total_deductions` DECIMAL(10,2)  NOT NULL DEFAULT 0.00,
    `total_bonuses`    DECIMAL(10,2)  NOT NULL DEFAULT 0.00,
    `pass_count`       INT            NOT NULL DEFAULT 0,
    `fail_count`       INT            NOT NULL DEFAULT 0,
    `ranking`          INT            NULL,
    `dimension_scores` JSON           NULL,
    `grade`            VARCHAR(10)    NULL,
    `last_event_id`    VARCHAR(50)    NULL     COMMENT '最后处理的事件ID(幂等保障)',
    `calculated_at`    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted`          TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_summary` (`project_id`, `summary_date`, `target_type`, `target_id`, `deleted`),
    INDEX `idx_tenant_date` (`tenant_id`, `summary_date`),
    INDEX `idx_org_unit` (`org_unit_id`, `summary_date`),
    INDEX `idx_daily_ranking` (`project_id`, `summary_date`, `ranking`),
    INDEX `idx_daily_org_path` (`org_unit_path`(100), `summary_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 日汇总（读模型）';

CREATE TABLE IF NOT EXISTS `insp_period_summaries` (
    `id`                       BIGINT         NOT NULL AUTO_INCREMENT,
    `tenant_id`                BIGINT         NOT NULL DEFAULT 0,
    `project_id`               BIGINT         NOT NULL,
    `period_type`              VARCHAR(20)    NOT NULL,
    `period_start`             DATE           NOT NULL,
    `period_end`               DATE           NOT NULL,
    `target_type`              VARCHAR(20)    NOT NULL,
    `target_id`                BIGINT         NOT NULL,
    `target_name`              VARCHAR(200)   NULL,
    `org_unit_id`              BIGINT         NULL,
    `org_unit_name`            VARCHAR(200)   NULL,
    `org_unit_path`            VARCHAR(500)   NULL     COMMENT '组织路径(层级查询优化)',
    `inspection_days`          INT            NOT NULL DEFAULT 0,
    `avg_score`                DECIMAL(10,2)  NULL,
    `min_score`                DECIMAL(10,2)  NULL,
    `max_score`                DECIMAL(10,2)  NULL,
    `score_std_dev`            DECIMAL(10,2)  NULL,
    `trend_direction`          VARCHAR(10)    NULL,
    `trend_percent`            DECIMAL(10,2)  NULL,
    `ranking`                  INT            NULL,
    `dimension_scores`         JSON           NULL,
    `grade`                    VARCHAR(10)    NULL,
    `corrective_count`         INT            NOT NULL DEFAULT 0,
    `corrective_closed_count`  INT            NOT NULL DEFAULT 0,
    `last_event_id`            VARCHAR(50)    NULL     COMMENT '最后处理的事件ID(幂等保障)',
    `calculated_at`            DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted`                  TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_summary` (`project_id`, `period_type`, `period_start`, `target_type`, `target_id`, `deleted`),
    INDEX `idx_tenant_period` (`tenant_id`, `period_type`, `period_start`),
    INDEX `idx_period_ranking` (`project_id`, `period_type`, `period_start`, `ranking`),
    INDEX `idx_period_org_path` (`org_unit_path`(100), `period_start`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 周期汇总（读模型）';

CREATE TABLE IF NOT EXISTS `insp_inspector_summaries` (
    `id`                   BIGINT         NOT NULL AUTO_INCREMENT,
    `tenant_id`            BIGINT         NOT NULL DEFAULT 0,
    `project_id`           BIGINT         NOT NULL,
    `period_type`          VARCHAR(20)    NOT NULL,
    `period_start`         DATE           NOT NULL,
    `period_end`           DATE           NOT NULL,
    `inspector_id`         BIGINT         NOT NULL,
    `inspector_name`       VARCHAR(100)   NULL,
    `total_tasks`          INT            NOT NULL DEFAULT 0,
    `completed_tasks`      INT            NOT NULL DEFAULT 0,
    `cancelled_tasks`      INT            NOT NULL DEFAULT 0,
    `completion_rate`      DECIMAL(5,2)   NULL,
    `avg_duration_minutes` INT            NULL,
    `total_submissions`    INT            NOT NULL DEFAULT 0,
    `total_flagged_items`  INT            NOT NULL DEFAULT 0,
    `avg_score_given`      DECIMAL(10,2)  NULL,
    `score_std_dev`        DECIMAL(10,2)  NULL,
    `on_time_rate`         DECIMAL(5,2)   NULL,
    `last_event_id`        VARCHAR(50)    NULL     COMMENT '最后处理的事件ID(幂等保障)',
    `calculated_at`        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted`              TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_inspector_summary` (`project_id`, `period_type`, `period_start`, `inspector_id`, `deleted`),
    INDEX `idx_inspector_period` (`inspector_id`, `period_type`, `period_start`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 检查员绩效汇总';

-- 28. item频次汇总 (Pareto分析+累犯追踪)
CREATE TABLE IF NOT EXISTS `insp_item_frequency_summaries` (
    `id`                   BIGINT         NOT NULL AUTO_INCREMENT,
    `tenant_id`            BIGINT         NOT NULL,
    `project_id`           BIGINT         NOT NULL,
    `period_type`          VARCHAR(20)    NOT NULL COMMENT 'WEEKLY|MONTHLY',
    `period_start`         DATE           NOT NULL,
    `period_end`           DATE           NOT NULL,
    `template_item_id`     BIGINT         NOT NULL,
    `item_code`            VARCHAR(50)    NOT NULL,
    `item_name`            VARCHAR(100)   NOT NULL,
    `issue_category_id`    BIGINT         NULL,
    `issue_category_name`  VARCHAR(100)   NULL,
    `target_type`          VARCHAR(20)    NULL,
    `target_id`            BIGINT         NULL,
    `occurrence_count`     INT            NOT NULL DEFAULT 0,
    `flagged_count`        INT            NOT NULL DEFAULT 0,
    `total_deduction`      DECIMAL(10,2)  NOT NULL DEFAULT 0,
    `corrective_case_count`INT            NOT NULL DEFAULT 0,
    `ranking`              INT            NULL,
    `cumulative_percent`   DECIMAL(5,2)   NULL,
    `last_event_id`        VARCHAR(50)    NULL,
    `calculated_at`        DATETIME       NOT NULL,
    `created_at`           DATETIME       DEFAULT CURRENT_TIMESTAMP,
    `updated_at`           DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`              TINYINT        DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_item_freq` (`project_id`, `period_type`, `period_start`, `template_item_id`, `target_id`, `deleted`),
    INDEX `idx_tenant_period` (`tenant_id`, `period_start`),
    INDEX `idx_category` (`issue_category_id`, `period_start`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 item频次汇总(Pareto分析)';

-- 29. 整改汇总
CREATE TABLE IF NOT EXISTS `insp_corrective_summaries` (
    `id`                      BIGINT         NOT NULL AUTO_INCREMENT,
    `tenant_id`               BIGINT         NOT NULL,
    `project_id`              BIGINT         NOT NULL,
    `period_type`             VARCHAR(20)    NOT NULL,
    `period_start`            DATE           NOT NULL,
    `period_end`              DATE           NOT NULL,
    `total_cases`             INT            NOT NULL DEFAULT 0,
    `open_cases`              INT            NOT NULL DEFAULT 0,
    `closed_cases`            INT            NOT NULL DEFAULT 0,
    `verified_cases`          INT            NOT NULL DEFAULT 0,
    `rejected_cases`          INT            NOT NULL DEFAULT 0,
    `escalated_cases`         INT            NOT NULL DEFAULT 0,
    `avg_resolution_days`     DECIMAL(5,1)   NULL,
    `sla_compliance_rate`     DECIMAL(5,2)   NULL,
    `effectiveness_pending`   INT            NOT NULL DEFAULT 0,
    `effectiveness_confirmed` INT            NOT NULL DEFAULT 0,
    `effectiveness_failed`    INT            NOT NULL DEFAULT 0,
    `priority_breakdown`      JSON           NULL     COMMENT '{"LOW":5,"MEDIUM":3,"HIGH":1,"CRITICAL":0}',
    `category_breakdown`      JSON           NULL     COMMENT 'top categories by count',
    `last_event_id`           VARCHAR(50)    NULL,
    `calculated_at`           DATETIME       NOT NULL,
    `created_at`              DATETIME       DEFAULT CURRENT_TIMESTAMP,
    `updated_at`              DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`                 TINYINT        DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_corrective_summary` (`project_id`, `period_type`, `period_start`, `deleted`),
    INDEX `idx_tenant_period` (`tenant_id`, `period_start`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 整改汇总';

-- -----------------------------------------------------------
-- 6. Platform BC (4 tables)
-- -----------------------------------------------------------

CREATE TABLE IF NOT EXISTS `insp_notification_rules` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`        BIGINT       NOT NULL DEFAULT 0,
    `rule_name`        VARCHAR(200) NOT NULL,
    `trigger_event`    VARCHAR(50)  NOT NULL,
    `conditions`       JSON         NULL     COMMENT '触发条件',
    `recipient_type`   VARCHAR(20)  NOT NULL COMMENT 'ROLE|USER|INSPECTOR|REVIEWER|ASSIGNEE',
    `recipient_config` JSON         NOT NULL,
    `channels`         JSON         NOT NULL COMMENT '["IN_APP","EMAIL","SMS","WECHAT"]',
    `message_template` TEXT         NULL,
    `is_enabled`       TINYINT      NOT NULL DEFAULT 1,
    `project_id`       BIGINT       NULL     COMMENT 'NULL=全局规则',
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`          TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_tenant_event` (`tenant_id`, `trigger_event`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 可配置通知规则';

CREATE TABLE IF NOT EXISTS `insp_report_templates` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`     BIGINT       NOT NULL DEFAULT 0,
    `template_name` VARCHAR(200) NOT NULL,
    `report_type`   VARCHAR(20)  NOT NULL COMMENT 'SUBMISSION|DAILY|PERIOD|CORRECTIVE',
    `layout_config` JSON         NOT NULL COMMENT '布局:页眉页脚/logo/颜色/字体',
    `sections`      JSON         NOT NULL COMMENT '包含的区块列表',
    `is_default`    TINYINT      NOT NULL DEFAULT 0,
    `created_by`    BIGINT       NULL,
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_by`    BIGINT       NULL,
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_tenant_type` (`tenant_id`, `report_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 PDF报告模板';

CREATE TABLE IF NOT EXISTS `insp_webhook_subscriptions` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`         BIGINT       NOT NULL DEFAULT 0,
    `name`              VARCHAR(200) NOT NULL,
    `url`               VARCHAR(500) NOT NULL,
    `secret`            VARCHAR(200) NULL     COMMENT 'HMAC签名密钥',
    `event_types`       JSON         NOT NULL,
    `is_active`         TINYINT      NOT NULL DEFAULT 1,
    `last_triggered_at` DATETIME     NULL,
    `failure_count`     INT          NOT NULL DEFAULT 0,
    `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`           TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 Webhook订阅（外部集成）';

-- 注意: 此表无 deleted 列, 无 UPDATE/DELETE 权限 — 仅追加, 保证不可篡改
CREATE TABLE IF NOT EXISTS `insp_audit_trail` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`   BIGINT       NOT NULL DEFAULT 0,
    `actor_id`    BIGINT       NOT NULL,
    `actor_name`  VARCHAR(100) NOT NULL,
    `action`      VARCHAR(50)  NOT NULL,
    `entity_type` VARCHAR(50)  NOT NULL,
    `entity_id`   BIGINT       NOT NULL,
    `entity_code` VARCHAR(50)  NULL,
    `old_value`   JSON         NULL,
    `new_value`   JSON         NULL,
    `ip_address`  VARCHAR(50)  NULL,
    `user_agent`  VARCHAR(500) NULL,
    `occurred_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_entity` (`entity_type`, `entity_id`),
    INDEX `idx_actor` (`actor_id`),
    INDEX `idx_time` (`occurred_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='V7 不可变审计日志（仅INSERT+SELECT，无UPDATE/DELETE）';
```

---

## 5. API 设计

统一前缀 `/api/v7/insp/`，15 个 Controller（原12 + 3新增）。

### 5.1 模板 API

```
POST/GET/PUT/DELETE  /templates
POST  /templates/{id}/publish|deprecate|archive|duplicate
POST  /templates/{id}/export
POST  /templates/import
GET   /templates/{id}/versions
GET   /templates/{id}/versions/{ver}
```

### 5.2 模板分区 API

```
CRUD  /templates/{templateId}/sections
PUT   /templates/{templateId}/sections/reorder
```

### 5.3 模板字段 API

```
POST/GET  /sections/{sectionId}/items
PUT/DELETE /items/{itemId}
PUT   /sections/{sectionId}/items/reorder
```

### 5.4 评分 API

```
CRUD  /scoring-profiles
GET   /scoring-profiles/by-template/{templateId}
CRUD  /scoring-profiles/{id}/dimensions
CRUD  /scoring-profiles/{id}/grade-bands
CRUD  /scoring-profiles/{id}/calculation-rules
POST  /scoring-profiles/{id}/test
```

### 5.5 项目 API

```
CRUD  /projects
POST  /projects/{id}/publish|pause|resume|complete|archive
CRUD  /projects/{id}/inspectors
```

### 5.6 任务 API

```
POST  /tasks/generate
GET   /tasks, /tasks/available, /tasks/my-tasks
GET   /tasks/{id}
POST  /tasks/{id}/claim|start|submit|review|approve|publish|cancel|assign
```

### 5.7 提交 API

```
GET   /tasks/{taskId}/submissions
GET   /submissions/{id}
POST  /submissions/{id}/lock|unlock|complete|skip
PUT   /submissions/{id}/form-data
CRUD  /submissions/{id}/details
POST  /submissions/{id}/evidences
DELETE /evidences/{evidenceId}
```

### 5.8 CAPA 整改 API ★ 大幅扩展

```
POST/GET  /corrective-cases
GET   /corrective-cases/{id}
GET   /corrective-cases/overdue|my-cases|statistics

POST  /corrective-cases/{id}/assign
POST  /corrective-cases/{id}/start
POST  /corrective-cases/{id}/submit-correction
POST  /corrective-cases/{id}/verify
POST  /corrective-cases/{id}/reject
POST  /corrective-cases/{id}/close
POST  /corrective-cases/{id}/escalate

★ POST  /corrective-cases/{id}/rca               设置根因分析
★ POST  /corrective-cases/{id}/confirm-effectiveness   确认效果
★ POST  /corrective-cases/{id}/fail-effectiveness      效果验证失败
★ GET   /corrective-cases/effectiveness-pending   待效果验证列表
```

### 5.9 分析 API ★ 扩展

```
GET   /analytics/daily-ranking|daily-summary|period-summary
GET   /analytics/trend|comparison|dimension-breakdown
GET   /analytics/export
POST  /analytics/rebuild-daily|rebuild-period

★ GET  /analytics/inspector-performance      检查员绩效
★ GET  /analytics/pareto                     帕累托分析(问题分类Top N)
★ GET  /analytics/repeat-offenders           重复违规统计
★ GET  /analytics/normalization-impact       归一化前后对比
★ GET  /analytics/corrective-summary         整改统计(含效果验证率)
★ GET  /analytics/item-frequency             Pareto/item频次分析
★ GET  /analytics/rebuild-status/{jobId}     重建进度查询
```

### 5.10 辅助 API

```
CRUD  /catalogs
CRUD  /response-sets
CRUD  /holiday-calendars
★ CRUD  /issue-categories          问题分类字典
```

### 5.11 通知规则 API ★ 新增

```
CRUD  /notification-rules
POST  /notification-rules/{id}/test    测试发送
```

### 5.12 报告 API ★ 新增

```
CRUD  /report-templates
POST  /reports/generate                生成PDF报告
GET   /reports/{id}/download           下载PDF
GET   /reports/by-submission/{id}      按提交查报告
```

### 5.13 Webhook API ★ 新增

```
CRUD  /webhook-subscriptions
POST  /webhook-subscriptions/{id}/test    测试回调
GET   /webhook-subscriptions/{id}/logs    调用日志
```

### 5.14 审计日志 API ★ 新增

```
GET   /audit-trail                    查询审计日志(只读)
GET   /audit-trail/by-entity/{type}/{id}  按实体查
GET   /audit-trail/by-actor/{actorId}     按操作人查
```

---

## 6. 事件目录

### 6.1 完整事件列表（25+ 领域事件）

| 事件 | 触发时机 | 消费者 |
|------|---------|--------|
| `TemplatePublishedEvent` | 模板发布 | 缓存失效 |
| `ProjectCreatedEvent` | 项目创建 | — |
| `ProjectPublishedEvent` | 项目激活 | TaskGenerationService |
| `ProjectPausedEvent` | 项目暂停 | 停止任务生成 |
| `ProjectResumedEvent` | 项目恢复 | 恢复任务生成 |
| `ProjectCompletedEvent` | 项目完成 | Analytics, Rating |
| `TaskCreatedEvent` | 任务自动生成 | NotificationRuleEngine |
| `TaskClaimedEvent` | 检查员领取 | — |
| `TaskStartedEvent` | 检查开始 | — |
| `TaskSubmittedEvent` | 检查员提交 | NotificationRuleEngine |
| `TaskReviewedEvent` | 审核完成 | NotificationRuleEngine |
| `TaskPublishedEvent` | 结果发布 | Analytics, Corrective, NotificationRuleEngine, Webhook |
| `SubmissionCompletedEvent` | 提交评分完成 | CorrectiveAutoCreation (flagged items) |
| `CorrectiveCaseCreatedEvent` | 整改创建 | NotificationRuleEngine |
| `CaseAssignedEvent` | 整改分配 | NotificationRuleEngine |
| `CorrectionSubmittedEvent` | 提交整改 | NotificationRuleEngine |
| `CaseVerifiedEvent` | 整改验证通过 | Analytics, EffectivenessScheduler |
| `CaseRejectedEvent` | 整改被拒 | NotificationRuleEngine |
| `CaseEscalatedEvent` | 整改升级 | NotificationRuleEngine, WebhookDispatcher |
| `CaseClosedEvent` | ★ 整改关闭 | EffectivenessScheduler |
| `EffectivenessConfirmedEvent` | ★ 效果验证通过 | Analytics |
| `EffectivenessFailedEvent` | ★ 效果验证失败 | 重新打开Case, NotificationRuleEngine |
| `DailySummaryUpdatedEvent` | 日汇总更新 | Rating 触发 |
| `PeriodSummaryCalculatedEvent` | 周期汇总完成 | Rating 评级计算 |
| `SlaBreachedEvent` | ★ SLA超时 | SlaAutoEscalationJob, NotificationRuleEngine |
| `TaskCancelledEvent` | 任务取消 | InspectorSummary(cancelledTasks++) |

> **所有 V7 事件实现 `InspV7DomainEvent` 标记接口。`NotificationRuleEngine` 和 `WebhookDispatcher` 仅监听 `InspV7DomainEvent` 子类型。**

### 6.2 事件→投影映射（读模型更新）

| 事件 | 更新的读模型 |
|------|-------------|
| `SubmissionCompletedEvent` | DailySummary, PeriodSummary, InspectorSummary, ItemFrequencySummary |
| `TaskPublishedEvent` | DailySummary(ranking recalc), InspectorSummary |
| `CorrectiveCaseCreatedEvent` | PeriodSummary(correctiveCount++), CorrectiveSummary, ItemFrequencySummary |
| `CaseClosedEvent` | PeriodSummary(correctiveClosedCount++), CorrectiveSummary |
| `EffectivenessFailedEvent` | PeriodSummary(correctiveClosedCount--), CorrectiveSummary |
| `TaskCancelledEvent` | InspectorSummary(cancelledTasks++) |
| `ProjectCompletedEvent` | 触发完整 PeriodSummary 重新计算 |
| `CaseEscalatedEvent` | CorrectiveSummary(escalatedCases++) |
| `CaseVerifiedEvent` | CorrectiveSummary(verifiedCases++) |
| `EffectivenessConfirmedEvent` | CorrectiveSummary(effectiveness_confirmed++) |

### 6.3 事件处理器

```java
@Component
public class InspV7EventHandler {

    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void onProjectPublished(ProjectPublishedEvent e) {
        // → TaskGenerationService
    }

    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void onTaskPublished(TaskPublishedEvent e) {
        // → AnalyticsProjectionService
        // → CorrectiveAutoCreationHandler (flagged items)
        // → NotificationRuleEngine
        // → WebhookDispatcher
    }

    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void onCaseVerified(CaseVerifiedEvent e) {
        // → EffectivenessScheduler (设置效果验证日期)
        // → Analytics (更新整改统计)
    }

    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void onEffectivenessFailed(EffectivenessFailedEvent e) {
        // → 重新打开Case + 升级
        // → NotificationRuleEngine
    }
}

// ★ 通知规则引擎 — 可配置，非硬编码
@Component
public class NotificationRuleEngine {
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void onAnyEvent(DomainEvent event) {
        // 1. 查 insp_notification_rules WHERE trigger_event = event.eventType
        // 2. 评估 conditions 是否满足
        // 3. 解析 recipientConfig → 实际用户列表
        // 4. 渲染 messageTemplate
        // 5. 按 channels 分发 (IN_APP / EMAIL / SMS / WECHAT)
    }
}

// ★ Webhook 分发器
@Component
public class WebhookDispatcher {
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void onAnyEvent(DomainEvent event) {
        // 1. 查 insp_webhook_subscriptions WHERE event_types CONTAINS event.eventType
        // 2. POST JSON payload to subscription.url
        // 3. HMAC签名 (secret)
        // 4. 记录调用结果, 失败累加 failure_count
    }
}

// ★ 审计日志追加器
@Component
public class AuditTrailAppender {
    // AOP切面: 拦截所有 @CasbinAccess 标注的方法
    // 记录: actorId, action, entityType, entityId, oldValue, newValue, ip, userAgent
    // INSERT ONLY — 不可修改
}

// ★ SLA自动升级定时任务
@Component
public class SlaAutoEscalationJob {
    @Scheduled(fixedDelay = 300000) // 每5分钟
    public void checkSlaBreaches() {
        // 1. 查询超过响应时限但仍为OPEN/ASSIGNED的案例
        // 2. 查询超过解决时限但未VERIFIED的案例
        // 3. 按 slaConfig.escalationChain 自动 escalate()
        // 4. 发布 SlaBreachedEvent
    }
}

// ★ 效果验证定时任务
@Component
public class EffectivenessCheckJob {
    @Scheduled(cron = "0 0 8 * * ?") // 每天8am
    public void checkPendingEffectivenessReviews() {
        // 查 effectiveness_check_date <= today AND effectiveness_status = 'PENDING'
        // 生成待办通知
    }
}
```

---

## 7. 前端架构

### 7.1 目录结构

```
frontend/src/
├── api/insp/                        # 15 个 API 模块
│   ├── template.ts                  # 模板 CRUD + 发布/归档
│   ├── responseSet.ts               # 选项集 CRUD
│   ├── scoringProfile.ts            # 评分配置 CRUD + 测试
│   ├── catalog.ts                   # 模板分类 CRUD
│   ├── project.ts                   # 项目 CRUD + 状态操作
│   ├── task.ts                      # 任务操作(领取/提交/审核等)
│   ├── submission.ts                # 提交表单数据+证据
│   ├── correctiveCase.ts            # CAPA 整改全流程
│   ├── analytics.ts                 # 分析查询+导出+重建
│   ├── holidayCalendar.ts           # 假日日历 CRUD
│   ├── issueCategory.ts             # 问题分类字典 CRUD
│   ├── notificationRule.ts          # 通知规则 CRUD + 测试
│   ├── reportTemplate.ts            # 报告模板 CRUD + 生成
│   ├── webhook.ts                   # Webhook CRUD + 测试
│   └── auditTrail.ts               # 审计日志只读查询
│
├── types/insp/                      # 7 个类型模块
│   ├── template.ts                  # Template, TemplateVersion, Section, Item, ResponseSet
│   ├── scoring.ts                   # ScoringProfile, Dimension, GradeBand, CalcRule
│   ├── project.ts                   # Project, Task, Submission, SubmissionDetail, Evidence
│   ├── correctiveCase.ts            # CorrectiveCase, IssueCategory, RCA types
│   ├── analytics.ts                 # DailySummary, PeriodSummary, InspectorSummary, ItemFrequency, CorrectiveSummary
│   ├── enums.ts                     # 所有枚举 (ItemType, TemplateStatus, TaskStatus, etc.)
│   └── platform.ts                  # NotificationRule, ReportTemplate, Webhook, AuditTrailEntry
│
├── stores/insp/                     # 6 个 Pinia Store
│   ├── inspTemplateStore.ts         # 模板目录、模板、版本、选项集
│   ├── inspScoringStore.ts          # 评分配置、维度、等级、规则
│   ├── inspProjectStore.ts          # 项目、任务、提交 (合并项目+执行)
│   ├── inspCorrectiveStore.ts       # 整改案例、问题分类、统计
│   ├── inspAnalyticsStore.ts        # 日汇总、周期汇总、检查员汇总
│   └── inspPlatformStore.ts         # 通知规则、报告模板、Webhook、假日日历
│
├── composables/insp/                # 9 个 Composable
│   ├── useTemplateEditor.ts         # ★ 自动保存、undo/redo、section/item CRUD
│   ├── useInspectionForm.ts         # 动态表单(核心)
│   ├── useScoring.ts                # 客户端评分预览(含归一化)
│   ├── useTaskExecution.ts          # 任务执行流程
│   ├── useAnalyticsFilters.ts       # 分析筛选器
│   ├── useOfflineSync.ts            # Phase 7 离线同步
│   ├── useCorrectiveCase.ts         # ★ 状态机操作、SLA跟踪、时间线
│   ├── usePlatformSettings.ts       # ★ 通知规则、Webhook、日历、审计日志
│   └── useInspPermission.ts         # 权限检查 helper
│
└── views/inspection/v7/
    ├── V7DashboardView.vue          # ★ 入口仪表盘 (NEW)
    │
    ├── templates/                   # 模板引擎
    │   ├── TemplateListView.vue
    │   ├── TemplateEditorView.vue
    │   ├── TemplateCatalogView.vue  # ★ 模板分类管理 (NEW)
    │   ├── ResponseSetListView.vue  # ★ 选项集管理 (NEW)
    │   └── components/
    │       ├── SectionTree.vue, ItemEditor.vue, ItemTypeSelector.vue
    │       ├── ConditionalLogicEditor.vue, ValidationRuleEditor.vue
    │       ├── ScoringConfigPanel.vue       # 含归一化配置面板
    │       ├── NormalizationPanel.vue       # 归一化模式选择+示例计算
    │       ├── TemplatePreview.vue, ResponseSetManager.vue
    │       ├── HelpContentEditor.vue        # 帮助文本/参考图片编辑
    │       ├── VersionDiffViewer.vue        # ★ 版本差异对比 (NEW)
    │       └── ImportPreviewDialog.vue      # ★ 导入预览确认 (NEW)
    │
    ├── scoring/                     # 评分配置
    │   ├── ScoringProfileListView.vue  # ★ 评分配置列表 (NEW)
    │   ├── ScoringProfileEditor.vue
    │   └── components/
    │       ├── DimensionTable.vue, GradeBandEditor.vue
    │       ├── CalcRuleChain.vue          # 含POPULATION_NORMALIZE/REPEAT_OFFENSE
    │       ├── FormulaEditor.vue, ScoreTestPanel.vue
    │       └── NormalizationDefaultPanel.vue  # 全局默认归一化
    │
    ├── projects/                    # 项目管理
    │   ├── ProjectListView.vue, ProjectDetailView.vue, ProjectWizardView.vue
    │   └── components/
    │       ├── ScopeSelector.vue            # ★ (NEW)
    │       ├── CycleConfigPanel.vue         # ★ (NEW)
    │       ├── InspectorPoolManager.vue, TimeSlotEditor.vue
    │       ├── PopulationConfigPanel.vue    # 人数来源配置
    │       └── SlaConfigPanel.vue           # ★ SLA时限+升级链配置 (NEW)
    │
    ├── tasks/                       # 任务执行
    │   ├── TaskListView.vue, TaskExecutionView.vue
    │   ├── TaskReviewWorkbenchView.vue  # ★ 审核工作台 (NEW)
    │   └── components/
    │       ├── TargetList.vue, InspectionForm.vue, FormItemRenderer.vue
    │       ├── EvidenceCapture.vue, ScoreSummary.vue, TaskReviewPanel.vue
    │       ├── PopulationInput.vue          # 手动输入人数(MANUAL模式)
    │       └── BatchTaskOperationDialog.vue # ★ 批量任务操作 (NEW)
    │
    ├── corrective/                  # CAPA 管理
    │   ├── CorrectiveCaseListView.vue
    │   ├── CorrectiveCaseDetailView.vue
    │   └── components/
    │       ├── CaseTimeline.vue, CorrectionForm.vue, VerificationForm.vue
    │       ├── RcaEditor.vue                # 根因分析编辑器(5Why/鱼骨图)
    │       ├── FishboneDiagram.vue          # 鱼骨图可视化
    │       ├── FiveWhysChain.vue            # 5Why链式编辑器
    │       ├── IssueCategorySelector.vue    # 问题分类选择器(树形)
    │       ├── EffectivenessReviewForm.vue  # 效果验证表单
    │       └── PreventiveActionEditor.vue   # 预防措施编辑器
    │
    ├── analytics/                   # 分析报表
    │   ├── AnalyticsDashboardView.vue
    │   └── components/
    │       ├── RankingTable.vue, ScoreTrendChart.vue
    │       ├── DimensionRadarChart.vue, ComparisonBarChart.vue
    │       ├── CorrectivePieChart.vue, ExportDialog.vue
    │       ├── InspectorPerformanceTable.vue  # 检查员绩效表
    │       ├── ParetoChart.vue                # 帕累托图
    │       ├── NormalizationImpactChart.vue   # 归一化前后对比
    │       └── RepeatOffenderTable.vue        # 重复违规列表
    │
    ├── platform/                    # 平台设置
    │   ├── NotificationRuleListView.vue
    │   ├── NotificationRuleEditor.vue
    │   ├── ReportTemplateListView.vue
    │   ├── ReportTemplateEditor.vue
    │   ├── WebhookListView.vue
    │   ├── AuditTrailView.vue
    │   ├── HolidayCalendarView.vue  # ★ (NEW)
    │   └── IssueCategoryManagement.vue
    │
    └── shared/                      # ★ 共享组件 (NEW)
        ├── EvidenceLightbox.vue     # ★ 证据灯箱预览 (NEW)
        ├── InspErrorState.vue       # ★ 统一错误状态 (NEW)
        └── InspEmptyState.vue       # ★ 统一空状态 (NEW)
```

### 7.2 视图与组件清单

#### 视图（24 个）

| # | 视图 | 路径 | 说明 |
|---|------|------|------|
| 1 | V7DashboardView | `/inspection/v7` | 入口仪表盘 (NEW) |
| 2 | TemplateListView | `/inspection/v7/templates` | 模板列表 |
| 3 | TemplateEditorView | `/inspection/v7/templates/:id/edit` | 模板编辑器 |
| 4 | TemplateCatalogView | `/inspection/v7/catalogs` | 模板分类管理 (NEW) |
| 5 | ResponseSetListView | `/inspection/v7/response-sets` | 选项集管理 (NEW) |
| 6 | ScoringProfileListView | `/inspection/v7/scoring` | 评分配置列表 (NEW) |
| 7 | ScoringProfileEditor | `/inspection/v7/scoring/:id` | 评分配置编辑 |
| 8 | ProjectListView | `/inspection/v7/projects` | 项目列表 |
| 9 | ProjectWizardView | `/inspection/v7/projects/create` | 项目创建向导 |
| 10 | ProjectDetailView | `/inspection/v7/projects/:id` | 项目详情 |
| 11 | TaskListView | `/inspection/v7/tasks` | 任务列表 |
| 12 | TaskExecutionView | `/inspection/v7/tasks/:id/execute` | 任务执行 |
| 13 | TaskReviewWorkbenchView | `/inspection/v7/tasks/review` | 审核工作台 (NEW) |
| 14 | CorrectiveCaseListView | `/inspection/v7/corrective` | 整改列表 |
| 15 | CorrectiveCaseDetailView | `/inspection/v7/corrective/:id` | 整改详情 |
| 16 | AnalyticsDashboardView | `/inspection/v7/analytics` | 分析仪表盘 |
| 17 | NotificationRuleListView | `/inspection/v7/notification-rules` | 通知规则列表 |
| 18 | NotificationRuleEditor | `/inspection/v7/notification-rules/:id` | 通知规则编辑 |
| 19 | ReportTemplateListView | `/inspection/v7/report-templates` | 报告模板列表 |
| 20 | ReportTemplateEditor | `/inspection/v7/report-templates/:id` | 报告模板编辑 |
| 21 | WebhookListView | `/inspection/v7/webhooks` | Webhook列表 |
| 22 | AuditTrailView | `/inspection/v7/audit-trail` | 审计日志 |
| 23 | HolidayCalendarView | `/inspection/v7/holiday-calendars` | 假日日历 (NEW) |
| 24 | IssueCategoryManagement | `/inspection/v7/issue-categories` | 问题分类管理 |

#### 组件（51 个）

**Template BC (12):**
SectionTree, ItemEditor, ItemTypeSelector, ConditionalLogicEditor, ValidationRuleEditor, ScoringConfigPanel, NormalizationPanel, TemplatePreview, ResponseSetManager, HelpContentEditor, VersionDiffViewer(NEW), ImportPreviewDialog(NEW)

**Scoring BC (6):**
DimensionTable, GradeBandEditor, CalcRuleChain, FormulaEditor, ScoreTestPanel, NormalizationDefaultPanel

**Project BC (6):**
ScopeSelector(NEW), CycleConfigPanel(NEW), InspectorPoolManager, TimeSlotEditor, PopulationConfigPanel, SlaConfigPanel(NEW)

**Task BC (8):**
TargetList, InspectionForm, FormItemRenderer, EvidenceCapture, ScoreSummary, TaskReviewPanel, PopulationInput, BatchTaskOperationDialog(NEW)

**Corrective BC (9):**
CaseTimeline, CorrectionForm, VerificationForm, RcaEditor, FishboneDiagram, FiveWhysChain, IssueCategorySelector, EffectivenessReviewForm, PreventiveActionEditor

**Analytics BC (7):**
RankingTable, ScoreTrendChart, DimensionRadarChart, ComparisonBarChart, CorrectivePieChart, ExportDialog, InspectorPerformanceTable, ParetoChart, NormalizationImpactChart, RepeatOffenderTable

**Shared (3):**
EvidenceLightbox(NEW), InspErrorState(NEW), InspEmptyState(NEW)

### 7.3 Pinia Stores

#### inspTemplateStore

```typescript
export const useInspTemplateStore = defineStore('inspTemplate', () => {
  // State
  const catalogs = ref<TemplateCatalog[]>([])
  const templates = ref<InspTemplate[]>([])
  const currentTemplate = ref<InspTemplate | null>(null)
  const versions = ref<TemplateVersion[]>([])
  const responseSets = ref<ResponseSet[]>([])

  // Actions
  async function loadTemplates(params: TemplateQueryParams): Promise<PageResult<InspTemplate>>
  async function loadTemplate(id: number): Promise<InspTemplate>
  async function createTemplate(cmd: CreateTemplateCmd): Promise<InspTemplate>
  async function updateTemplate(id: number, cmd: UpdateTemplateCmd): Promise<void>
  async function publishTemplate(id: number): Promise<TemplateVersion>
  async function deprecateTemplate(id: number): Promise<void>
  async function archiveTemplate(id: number): Promise<void>
  async function duplicateTemplate(id: number): Promise<InspTemplate>
  async function loadVersions(templateId: number): Promise<TemplateVersion[]>
  async function loadCatalogs(): Promise<TemplateCatalog[]>
  async function loadResponseSets(): Promise<ResponseSet[]>

  return { catalogs, templates, currentTemplate, versions, responseSets, /* actions */ }
})
```

#### inspScoringStore

```typescript
export const useInspScoringStore = defineStore('inspScoring', () => {
  const profiles = ref<ScoringProfile[]>([])
  const currentProfile = ref<ScoringProfile | null>(null)
  const dimensions = ref<ScoreDimension[]>([])
  const gradeBands = ref<GradeBand[]>([])
  const calcRules = ref<CalculationRule[]>([])

  async function loadProfiles(params?: ScoringQueryParams): Promise<ScoringProfile[]>
  async function loadProfile(id: number): Promise<ScoringProfile>
  async function loadByTemplate(templateId: number): Promise<ScoringProfile>
  async function saveDimensions(profileId: number, dims: ScoreDimension[]): Promise<void>
  async function saveGradeBands(profileId: number, bands: GradeBand[]): Promise<void>
  async function saveCalcRules(profileId: number, rules: CalculationRule[]): Promise<void>
  async function testScoring(profileId: number, testData: ScoreTestInput): Promise<ScoreResult>

  return { profiles, currentProfile, dimensions, gradeBands, calcRules, /* actions */ }
})
```

#### inspProjectStore

```typescript
export const useInspProjectStore = defineStore('inspProject', () => {
  // 合并 project + execution 状态
  const projects = ref<InspProject[]>([])
  const currentProject = ref<InspProject | null>(null)
  const tasks = ref<InspTask[]>([])
  const currentTask = ref<InspTask | null>(null)
  const submissions = ref<InspSubmission[]>([])

  // Project actions
  async function loadProjects(params: ProjectQueryParams): Promise<PageResult<InspProject>>
  async function loadProject(id: number): Promise<InspProject>
  async function createProject(cmd: CreateProjectCmd): Promise<InspProject>
  async function publishProject(id: number): Promise<void>
  async function pauseProject(id: number): Promise<void>
  async function resumeProject(id: number): Promise<void>

  // Task actions
  async function loadTasks(params: TaskQueryParams): Promise<PageResult<InspTask>>
  async function claimTask(taskId: number): Promise<void>
  async function submitTask(taskId: number): Promise<void>
  async function reviewTask(taskId: number, approved: boolean, note?: string): Promise<void>

  // Submission actions
  async function loadSubmissions(taskId: number): Promise<InspSubmission[]>
  async function saveFormData(submissionId: number, formData: any): Promise<void>
  async function completeSubmission(submissionId: number): Promise<void>

  return { projects, currentProject, tasks, currentTask, submissions, /* actions */ }
})
```

#### inspCorrectiveStore

```typescript
export const useInspCorrectiveStore = defineStore('inspCorrective', () => {
  const cases = ref<CorrectiveCase[]>([])
  const currentCase = ref<CorrectiveCase | null>(null)
  const issueCategories = ref<IssueCategory[]>([])
  const stats = ref<CorrectiveStats | null>(null)

  async function loadCases(params: CaseQueryParams): Promise<PageResult<CorrectiveCase>>
  async function loadCase(id: number): Promise<CorrectiveCase>
  async function assignCase(id: number, assigneeId: number): Promise<void>
  async function submitCorrection(id: number, data: CorrectionData): Promise<void>
  async function verifyCase(id: number, data: VerificationData): Promise<void>
  async function rejectCase(id: number, reason: string): Promise<void>
  async function setRca(id: number, method: RcaMethod, data: any): Promise<void>
  async function confirmEffectiveness(id: number, note: string): Promise<void>
  async function failEffectiveness(id: number, note: string): Promise<void>
  async function loadIssueCategories(): Promise<IssueCategory[]>
  async function loadStats(projectId?: number): Promise<CorrectiveStats>

  return { cases, currentCase, issueCategories, stats, /* actions */ }
})
```

#### inspAnalyticsStore

```typescript
export const useInspAnalyticsStore = defineStore('inspAnalytics', () => {
  const dailySummaries = ref<DailySummary[]>([])
  const periodSummaries = ref<PeriodSummary[]>([])
  const inspectorSummaries = ref<InspectorSummary[]>([])
  const itemFrequencies = ref<ItemFrequencySummary[]>([])
  const correctiveSummary = ref<CorrectiveSummary | null>(null)

  async function loadDailyRanking(params: DailyRankingParams): Promise<DailySummary[]>
  async function loadPeriodSummary(params: PeriodSummaryParams): Promise<PeriodSummary[]>
  async function loadInspectorPerformance(params: InspectorParams): Promise<InspectorSummary[]>
  async function loadItemFrequency(params: ItemFreqParams): Promise<ItemFrequencySummary[]>
  async function loadCorrectiveSummary(params: CorrectiveSummaryParams): Promise<CorrectiveSummary>
  async function loadTrend(params: TrendParams): Promise<TrendData[]>
  async function exportData(params: ExportParams): Promise<Blob>
  async function rebuildDaily(projectId: number, dateRange: [string, string]): Promise<string>
  async function rebuildStatus(jobId: string): Promise<RebuildStatus>

  return { dailySummaries, periodSummaries, inspectorSummaries, itemFrequencies, correctiveSummary, /* actions */ }
})
```

#### inspPlatformStore

```typescript
export const useInspPlatformStore = defineStore('inspPlatform', () => {
  const notificationRules = ref<NotificationRule[]>([])
  const reportTemplates = ref<ReportTemplate[]>([])
  const webhooks = ref<WebhookSubscription[]>([])
  const holidayCalendars = ref<HolidayCalendar[]>([])

  // Notification rules
  async function loadNotificationRules(projectId?: number): Promise<NotificationRule[]>
  async function saveNotificationRule(rule: NotificationRule): Promise<void>
  async function testNotificationRule(id: number): Promise<void>

  // Report templates
  async function loadReportTemplates(): Promise<ReportTemplate[]>
  async function generateReport(type: string, params: any): Promise<string>

  // Webhooks
  async function loadWebhooks(): Promise<WebhookSubscription[]>
  async function saveWebhook(webhook: WebhookSubscription): Promise<void>
  async function testWebhook(id: number): Promise<void>

  // Holiday calendars
  async function loadHolidayCalendars(year?: number): Promise<HolidayCalendar[]>
  async function saveHolidayCalendar(cal: HolidayCalendar): Promise<void>

  return { notificationRules, reportTemplates, webhooks, holidayCalendars, /* actions */ }
})
```

### 7.4 Composables

#### useTemplateEditor (NEW)

```typescript
export function useTemplateEditor(templateId: Ref<number>) {
  return {
    // State
    template: Ref<InspTemplate>,
    sections: Ref<TemplateSection[]>,
    items: Ref<Map<number, TemplateItem[]>>,
    isDirty: Ref<boolean>,
    isSaving: Ref<boolean>,
    undoStack: Ref<EditAction[]>,
    redoStack: Ref<EditAction[]>,

    // Section CRUD
    addSection(parentId?: number): TemplateSection,
    updateSection(sectionId: number, data: Partial<TemplateSection>): void,
    removeSection(sectionId: number): void,
    reorderSections(sectionIds: number[]): void,

    // Item CRUD
    addItem(sectionId: number, itemType: ItemType): TemplateItem,
    updateItem(itemId: number, data: Partial<TemplateItem>): void,
    removeItem(itemId: number): void,
    reorderItems(sectionId: number, itemIds: number[]): void,

    // Auto-save & undo/redo
    save(): Promise<void>,          // 手动保存
    undo(): void,
    redo(): void,
    canUndo: ComputedRef<boolean>,
    canRedo: ComputedRef<boolean>,
  }
}
```

#### useCorrectiveCase (NEW)

```typescript
export function useCorrectiveCase(caseId: Ref<number>) {
  return {
    // State
    caseData: Ref<CorrectiveCase>,
    timeline: Ref<TimelineEvent[]>,
    slaStatus: Ref<SlaStatus>,       // { responseOverdue, resolutionOverdue, hoursRemaining }
    isLoading: Ref<boolean>,

    // State machine actions (按当前 status 决定可用操作)
    availableActions: ComputedRef<CaseAction[]>,
    assign(assigneeId: number): Promise<void>,
    startWork(): Promise<void>,
    submitCorrection(data: CorrectionData): Promise<void>,
    verify(data: VerificationData): Promise<void>,
    reject(reason: string): Promise<void>,
    close(): Promise<void>,
    escalate(): Promise<void>,
    setRca(method: RcaMethod, data: any): Promise<void>,
    confirmEffectiveness(note: string): Promise<void>,
    failEffectiveness(note: string): Promise<void>,

    // SLA tracking
    refreshSlaStatus(): void,
  }
}
```

#### usePlatformSettings (NEW)

```typescript
export function usePlatformSettings() {
  return {
    // Notification rules
    rules: Ref<NotificationRule[]>,
    loadRules(projectId?: number): Promise<void>,
    saveRule(rule: NotificationRule): Promise<void>,
    deleteRule(id: number): Promise<void>,
    testRule(id: number): Promise<void>,

    // Webhooks
    webhooks: Ref<WebhookSubscription[]>,
    loadWebhooks(): Promise<void>,
    saveWebhook(wh: WebhookSubscription): Promise<void>,
    deleteWebhook(id: number): Promise<void>,
    testWebhook(id: number): Promise<void>,

    // Holiday calendars
    calendars: Ref<HolidayCalendar[]>,
    loadCalendars(year?: number): Promise<void>,
    saveCalendar(cal: HolidayCalendar): Promise<void>,

    // Audit trail (read-only)
    auditLogs: Ref<AuditTrailEntry[]>,
    loadAuditLogs(params: AuditQueryParams): Promise<PageResult<AuditTrailEntry>>,
  }
}
```

#### useInspectionForm (核心)

```typescript
export function useInspectionForm(options: {
  templateVersion: Ref<TemplateVersionSnapshot>
  submissionId: Ref<number>
  scoringProfile: Ref<ScoringProfileSnapshot>
  population?: Ref<number>         // 目标人数(归一化用)
  autoSaveInterval?: number        // 默认30秒
}) {
  return {
    sections, formData, errors, visibleItems,
    scoreResult,    // 含 rawScore, normFactors, ruleApplications
    isDirty, isSaving,
    setItemValue, validateItem, validateAll,
    saveFormData, completeSubmission, addEvidence, removeEvidence,
  }
}
```

### 7.5 路由配置（27 条路由）

```typescript
{
  path: '/inspection/v7',
  redirect: '/inspection/v7/dashboard',
  meta: { title: 'V7检查平台', permission: 'insp:v7:view' },
  children: [
    // 仪表盘
    { path: 'dashboard', component: V7DashboardView, meta: { title: '检查概览', permission: 'insp:v7:view' } },

    // 模板 (5)
    { path: 'templates', component: TemplateListView, meta: { title: '模板列表', permission: 'insp:template:view' } },
    { path: 'templates/create', component: TemplateEditorView, meta: { title: '创建模板', permission: 'insp:template:create' } },
    { path: 'templates/:id/edit', component: TemplateEditorView, meta: { title: '编辑模板', permission: 'insp:template:update' } },
    { path: 'catalogs', component: TemplateCatalogView, meta: { title: '模板分类', permission: 'insp:catalog:view' } },
    { path: 'response-sets', component: ResponseSetListView, meta: { title: '选项集', permission: 'insp:response-set:view' } },

    // 评分 (2)
    { path: 'scoring', component: ScoringProfileListView, meta: { title: '评分配置', permission: 'insp:scoring:view' } },
    { path: 'scoring/:id', component: ScoringProfileEditor, meta: { title: '评分编辑', permission: 'insp:scoring:update' } },

    // 项目 (4)
    { path: 'projects', component: ProjectListView, meta: { title: '项目列表', permission: 'insp:project:view' } },
    { path: 'projects/create', component: ProjectWizardView, meta: { title: '创建项目', permission: 'insp:project:create' } },
    { path: 'projects/:id', component: ProjectDetailView, meta: { title: '项目详情', permission: 'insp:project:view' } },
    { path: 'projects/:id/edit', component: ProjectWizardView, meta: { title: '编辑项目', permission: 'insp:project:update' } },

    // 任务 (3)
    { path: 'tasks', component: TaskListView, meta: { title: '任务列表', permission: 'insp:task:view' } },
    { path: 'tasks/:id/execute', component: TaskExecutionView, meta: { title: '执行检查', permission: 'insp:task:execute' } },
    { path: 'tasks/review', component: TaskReviewWorkbenchView, meta: { title: '审核工作台', permission: 'insp:task:review' } },

    // CAPA整改 (2)
    { path: 'corrective', component: CorrectiveCaseListView, meta: { title: '整改列表', permission: 'insp:corrective:view' } },
    { path: 'corrective/:id', component: CorrectiveCaseDetailView, meta: { title: '整改详情', permission: 'insp:corrective:view' } },

    // 分析 (1)
    { path: 'analytics', component: AnalyticsDashboardView, meta: { title: '分析报表', permission: 'insp:analytics:view' } },

    // 平台设置 (8)
    { path: 'notification-rules', component: NotificationRuleListView, meta: { title: '通知规则', permission: 'insp:notification-rule:view' } },
    { path: 'notification-rules/create', component: NotificationRuleEditor, meta: { title: '创建通知规则', permission: 'insp:notification-rule:create' } },
    { path: 'notification-rules/:id', component: NotificationRuleEditor, meta: { title: '编辑通知规则', permission: 'insp:notification-rule:update' } },
    { path: 'report-templates', component: ReportTemplateListView, meta: { title: '报告模板', permission: 'insp:report-template:view' } },
    { path: 'report-templates/:id', component: ReportTemplateEditor, meta: { title: '编辑报告模板', permission: 'insp:report-template:update' } },
    { path: 'webhooks', component: WebhookListView, meta: { title: 'Webhook', permission: 'insp:webhook:view' } },
    { path: 'audit-trail', component: AuditTrailView, meta: { title: '审计日志', permission: 'insp:audit-trail:view' } },
    { path: 'holiday-calendars', component: HolidayCalendarView, meta: { title: '假日日历', permission: 'insp:holiday-calendar:view' } },
    { path: 'issue-categories', component: IssueCategoryManagement, meta: { title: '问题分类', permission: 'insp:issue-category:view' } },
  ]
}
```

### 7.6 空状态/错误状态标准

#### InspEmptyState 组件

通用空状态组件，每个列表视图配置对应的空状态消息：

| 视图 | 空状态标题 | 空状态描述 | 操作按钮 |
|------|-----------|-----------|---------|
| TemplateListView | 暂无模板 | 创建第一个检查模板开始使用 | 创建模板 |
| TemplateCatalogView | 暂无分类 | 创建模板分类来组织模板 | 创建分类 |
| ResponseSetListView | 暂无选项集 | 创建可复用选项集供模板使用 | 创建选项集 |
| ScoringProfileListView | 暂无评分配置 | 在模板中创建评分配置 | — |
| ProjectListView | 暂无项目 | 创建检查项目开始检查 | 创建项目 |
| TaskListView | 暂无任务 | 发布项目后将自动生成任务 | — |
| TaskReviewWorkbenchView | 暂无待审核任务 | 所有任务已审核完成 | — |
| CorrectiveCaseListView | 暂无整改案例 | 检查中发现问题将自动创建整改 | — |
| AnalyticsDashboardView | 暂无分析数据 | 完成检查后将自动生成分析报表 | — |
| NotificationRuleListView | 暂无通知规则 | 配置通知规则实现自动通知 | 创建规则 |
| ReportTemplateListView | 暂无报告模板 | 创建报告模板用于生成PDF报告 | 创建模板 |
| WebhookListView | 暂无Webhook | 配置Webhook实现外部系统集成 | 添加Webhook |
| AuditTrailView | 暂无审计记录 | 系统操作将自动记录 | — |
| HolidayCalendarView | 暂无假日日历 | 配置假日日历用于跳过节假日 | 创建日历 |
| IssueCategoryManagement | 暂无问题分类 | 创建问题分类用于整改归类 | 创建分类 |

#### InspErrorState 组件

```typescript
interface InspErrorStateProps {
  title?: string        // 默认: "加载失败"
  message?: string      // 错误详情
  retryable?: boolean   // 是否显示重试按钮, 默认 true
  onRetry?: () => void  // 重试回调
}
```

#### Loading 骨架屏模式

| 视图类型 | 骨架屏模式 |
|---------|-----------|
| 列表视图 | 表格骨架屏(5行) + 工具栏骨架 |
| 详情视图 | 卡片骨架屏(标题+3行描述) + 侧栏骨架 |
| 编辑器视图 | 左侧树骨架 + 右侧表单骨架 |
| 仪表盘视图 | 统计卡片骨架(4个) + 图表骨架(2个) |

### 7.7 确认对话框清单（17 个）

| # | 触发操作 | 对话框标题 | 确认文案 | 严重级别 |
|---|---------|-----------|---------|---------|
| 1 | 发布模板 | 确认发布模板 | 发布后将创建不可变版本快照，确认发布？ | warning |
| 2 | 废弃模板 | 确认废弃模板 | 废弃后新项目无法使用此模板，确认废弃？ | warning |
| 3 | 归档模板 | 确认归档模板 | 归档后模板将不可见，确认归档？ | danger |
| 4 | 删除模板分区/字段 | 确认删除 | 删除后分区下所有字段将一并删除，确认？ | danger |
| 5 | 发布项目 | 确认发布项目 | 发布后将自动生成检查任务，确认发布？ | warning |
| 6 | 暂停项目 | 确认暂停项目 | 暂停后将停止生成新任务，确认暂停？ | warning |
| 7 | 完成项目 | 确认完成项目 | 完成后将触发周期汇总计算，确认完成？ | warning |
| 8 | 取消任务 | 确认取消任务 | 取消后检查员将无法继续执行，确认取消？ | danger |
| 9 | 批量取消任务 | 确认批量取消 | 将取消选中的 N 个任务，确认？ | danger |
| 10 | 提交检查 | 确认提交检查 | 提交后将进入审核流程，无法再修改，确认提交？ | warning |
| 11 | 发布检查结果 | 确认发布结果 | 发布后结果将对所有人可见，确认发布？ | warning |
| 12 | 整改升级 | 确认升级整改 | 升级后将通知上级负责人，确认升级？ | warning |
| 13 | 效果验证失败 | 确认效果验证失败 | 验证失败将重新打开整改案例，确认？ | danger |
| 14 | 重建日汇总 | 确认重建数据 | 重建将重新计算指定日期范围的汇总数据，确认？ | warning |
| 15 | 删除Webhook | 确认删除Webhook | 删除后将停止向该URL推送事件，确认删除？ | danger |
| 16 | 删除通知规则 | 确认删除通知规则 | 删除后将停止该规则的自动通知，确认删除？ | danger |
| 17 | 导入模板 | 确认导入 | 将导入 N 个分区、M 个字段，确认导入？ | info |

---

## 8. 实施阶段

### Phase 1: 模板引擎核心 (3周)

**后端:** 7张模板表 + 6个领域模型 + Repository + ApplicationService + Controller
**前端:** types + api + TemplateListView + TemplateEditorView + 全部编辑器组件(含HelpContentEditor)
**交付:** 完整模板创建/编辑/发布/版本管理

### Phase 2: 评分引擎 (2周)

**后端:** 4张评分表 + 4个领域模型 + NormalizationMode枚举 + ScoreCalculationDomainService(GraalVM JS) + 人数归一化计算 + REPEAT_OFFENSE规则
**前端:** ScoringProfileEditor + 全部子组件 + NormalizationPanel + ScoreTestPanel(支持population参数)
**交付:** 多维度评分 + 归一化 + 规则链 + 公式编辑 + 测试计算

### Phase 3: 执行引擎 (3周)

**后端:** 7张执行表 + 5个领域模型 + Project/Task/Submission Service + PopulationSource逻辑 + 全部领域事件 + @Scheduled定时任务
**前端:** ProjectWizard(含PopulationConfig/SlaConfig) + TaskList + TaskExecution + InspectionForm(22种字段) + PopulationInput
**交付:** 完整检查生命周期 + 人数自动/手动采集

### Phase 4: CAPA 整改管理 (2周) ★ 大幅扩展

**后端:** 2张整改表(cases+categories) + CorrectiveCase聚合根(含RCA/效果验证/预防措施) + IssueCategory + 自动创建Handler + SlaAutoEscalationJob + EffectivenessCheckJob + Controller(全端点@CasbinAccess)
**前端:** CorrectiveCaseList + CaseDetail + RcaEditor + FishboneDiagram + FiveWhysChain + IssueCategorySelector + EffectivenessReviewForm + PreventiveActionEditor
**交付:** 企业级CAPA: 自动创建 + RCA(5Why/鱼骨图) + 效果验证闭环 + SLA自动升级

### Phase 5: 分析报表 (2周)

**后端:** 3张汇总表(daily+period+inspector) + AnalyticsProjectionService + AnalyticsQueryService + 帕累托分析 + 归一化前后对比 + 重复违规统计
**前端:** AnalyticsDashboard + 全部图表(含ParetoChart/NormalizationImpact/InspectorPerformance/RepeatOffender)
**交付:** 日/周/月汇总 + 排名 + 趋势 + 检查员绩效 + 帕累托 + 导出

### Phase 5.5: PDF报告生成 (1周) ★ 新增

**后端:** 1张报告模板表 + ReportTemplate聚合根 + ReportGenerationService(PDF渲染) + Controller
**前端:** ReportTemplateListView + ReportTemplateEditor(拖拽区块配置)
**交付:** 提交/日报/周期/整改 四种报告类型自动生成

### Phase 5.6: 通知规则 + Webhook (1周) ★ 新增

**后端:** 2张表(rules+webhooks) + NotificationRuleEngine + WebhookDispatcher + Controller
**前端:** NotificationRuleList + Editor + WebhookList
**交付:** 可配置通知 + 外部系统集成

### Phase 5.7: 审计日志 (0.5周) ★ 新增

**后端:** 1张审计表(仅INSERT) + AuditTrailAppender(AOP切面) + 只读Controller
**前端:** AuditTrailView(搜索+过滤+时间线)
**交付:** 不可变审计日志

### Phase 6: 评级集成 (1周)

**后端:** 连接 PeriodSummaryCalculatedEvent → RatingCalculationService
**前端:** 扩展现有 rating 视图
**交付:** 检查分数自动评级

### Phase 7: 移动优化 & 离线 (2周)

**后端:** 离线同步端点 + syncVersion冲突解决
**前端:** 移动端TaskExecution优化 + 相机/GPS + useOfflineSync + Service Worker
**交付:** 移动端可用 + 离线数据设计就绪

---

## 9. 关键架构决策

### 9.1 模板不可变快照

发布时 sections+items+scoring 完整序列化为 JSON 存入 `insp_template_versions`。项目锁定特定版本。

### 9.2 formData as JSON

每次提交完整表单响应存为 JSON blob。仅评分项生成 `submission_details` 行。

### 9.3 CQRS 读写分离

写侧优化事务一致性，读侧星型 schema 由领域事件异步驱动重建。

### 9.4 GraalVM JS 评分引擎

`ScoreCalculationDomainService` 使用 GraalVM polyglot API 执行 JS 公式。

### 9.5 事件编排策略

V7 所有聚合根使用 `registerEvent()`，通过 outbox 模式发布。消费者使用 `@TransactionalEventListener(phase = AFTER_COMMIT)`。

### 9.6 人数归一化

每个 TemplateItem 独立配置是否启用归一化。5种模式(NONE/PER_CAPITA/RATE_BASED/SQRT_ADJUSTED/CUSTOM)。优先级链: Item级 > Profile默认 > 不归一化。

### 9.7 企业级 CAPA

整合 ISO 9001 / FDA CAPA 最佳实践: 结构化RCA(5Why/鱼骨图) + 问题分类编码(帕累托基础) + 预防措施 + 效果验证闭环 + SLA自动升级。

### 9.8 可配置通知

通知规则引擎替代硬编码通知。管理员通过UI配置: 触发事件 + 条件 + 接收人 + 渠道 + 消息模板。

### 9.9 不可变审计日志

`insp_audit_trail` 表无 deleted 列、无 UPDATE/DELETE 权限。AOP切面自动记录所有 `@CasbinAccess` 标注方法的操作。

### 9.10 离线友好数据模型

`syncVersion` 字段支持乐观并发控制。

### 9.11 InspSubmission 独立聚合根

InspSubmission 从 InspTask 内部实体升级为独立聚合根。理由：
- **独立生命周期**：提交有自己的状态机(PENDING->LOCKED->IN_PROGRESS->COMPLETED)
- **并发安全**：不同提交可并行编辑，不需锁定整个任务
- **加载性能**：操作任务元数据不需加载所有提交+明细+证据
- **事务范围**：保存表单数据不需锁定整个任务

InspTask 的 `completedTargets`/`skippedTargets` 通过 `SubmissionCompletedEvent` 异步更新，而非同步修改。

### 9.12 投影幂等策略

所有 CQRS 读模型投影采用"全量替换 upsert"策略：
- 收到事件后，从写侧源数据重新计算完整汇总
- 使用唯一键 UPSERT（`INSERT ON DUPLICATE KEY UPDATE`）
- 同一事件处理 N 次产生相同结果
- 每张汇总表增加 `last_event_id` 列作为二级幂等保障

### 9.13 事件基础设施修复清单

> **Implementation 前必须修复以下问题：**

1. `DomainEventStore.store()` 必须抛出异常（当前吞异常，导致事件静默丢失）
2. `SpringDomainEventPublisher.recordInOutbox()` 必须抛出异常（当前吞异常）
3. `DomainEvent.getEventId()` 默认实现必须移除（当前每次调用生成新 UUID，破坏幂等性）
4. 所有 V7 事件类型必须在启动时注册到 `OutboxProcessor.eventTypeRegistry`
5. 所有 V7 事件必须实现 `InspV7DomainEvent` 标记接口

### 9.14 FormulaEvaluator 接口分层

领域层定义 `FormulaEvaluator` 接口，基础设施层提供 `GraalVmFormulaEvaluator` 实现。`ScoreCalculationDomainService` 依赖接口而非具体实现，遵循依赖倒置原则（DIP）。

```
domain/inspection/service/FormulaEvaluator.java        — 接口(领域层)
infrastructure/scoring/GraalVmFormulaEvaluator.java    — 实现(基础设施层)
```

### 9.15 投影重建策略

异步重建 + 进度跟踪：
- `POST /analytics/rebuild-daily` 返回 jobId
- `GET /analytics/rebuild-status/{jobId}` 查询进度
- 按日期逐天重建，每天一个 checkpoint
- 重建与实时投影使用 `calculated_at` 时间戳 last-writer-wins

### 9.16 一致性模型

- **最终一致**，典型延迟 <1 秒
- `@TransactionalEventListener(AFTER_COMMIT)` 同步处理提供实际的"读自己写"语义
- 反规范化名称(targetName, orgUnitName)反映投影时名称，重建时更新为当前值

---

## 10. 复用现有基础设施

| 基础设施 | 文件路径 | V7 用法 |
|---------|---------|---------|
| `AggregateRoot<Long>` | `domain/shared/AggregateRoot.java` | 所有聚合根继承 |
| `Entity<Long>` | `domain/shared/Entity.java` | 所有实体实现 |
| `BaseDomainEvent` | `domain/shared/event/BaseDomainEvent.java` | 所有事件继承 |
| `SpringDomainEventPublisher` | `infrastructure/event/SpringDomainEventPublisher.java` | 事件发布+outbox |
| `@DataPermission` | `infrastructure/access/DataPermission.java` | Mapper行级过滤 |
| `@CasbinAccess` | 控制器权限注解 | 所有端点 |
| `UserContextHolder` | `infrastructure/access/UserContextHolder.java` | 获取当前用户 |
| `CacheService` | `infrastructure/cache/CacheService.java` | Redis缓存 |
| `DddArchitectureConfig` | `config/DddArchitectureConfig.java` | 无需修改 |
| `Result<T>` | `common/result/Result.java` | 统一响应包装 |
| `BusinessException` | `exception/BusinessException.java` | 业务异常 |
| `useTable`/`useDialog` | `frontend/src/composables/` | 前端CRUD模式 |
| `request` (Axios) | `frontend/src/utils/request.ts` | API调用 |
| `FormulaEvaluator` | `domain/inspection/service/FormulaEvaluator.java` | 评分公式接口(领域层) |
| `GraalVmFormulaEvaluator` | `infrastructure/scoring/GraalVmFormulaEvaluator.java` | GraalVM JS 实现 |

---

## 附录 A: 数据量预估

| 表 | 预估行数/年 | 热点查询 |
|----|------------|---------|
| `insp_templates` | ~100 | 按 status 过滤 |
| `insp_projects` | ~50 | 按 status 过滤 |
| `insp_tasks` | ~18,000 | project_id+date, inspector_id |
| `insp_submissions` | ~180,000 | task_id, target_id |
| `insp_submission_details` | ~1,800,000 | submission_id |
| `insp_daily_summaries` | ~3,650,000 | 考虑按月分区 |
| `insp_corrective_cases` | ~5,000 | status+assignee |
| `insp_audit_trail` | ~500,000 | 考虑按月分区, 定期归档 |

## 附录 B: 权限码清单

```
insp:template:view|create|update|delete|publish
insp:scoring:view|create|update
insp:project:view|create|update|delete|publish|pause|resume|complete
insp:task:view|generate|claim|execute|review|publish|cancel|assign
insp:submission:view|edit|complete
insp:corrective:view|create|assign|submit|verify|reject|escalate|close|rca
insp:analytics:view|export|rebuild
insp:catalog:view|create|update|delete
insp:response-set:view|create|update|delete
insp:holiday-calendar:view|create|update|delete
insp:issue-category:view|create|update|delete
insp:notification-rule:view|create|update|delete
insp:report-template:view|create|update|delete|generate
insp:webhook:view|create|update|delete
insp:audit-trail:view
```

## 附录 C: SLA 配置示例

```jsonc
{
  "responseTime": {
    "CRITICAL": 2,     // 小时
    "HIGH": 8,
    "MEDIUM": 24,
    "LOW": 72
  },
  "resolutionTime": {
    "CRITICAL": 24,
    "HIGH": 72,
    "MEDIUM": 168,
    "LOW": 336
  },
  "autoEscalateOnBreach": true,
  "escalationChain": [
    { "level": 1, "notifyRole": "department_head", "afterHours": 0 },
    { "level": 2, "notifyRole": "director", "afterHours": 24 },
    { "level": 3, "notifyRole": "principal", "afterHours": 48 }
  ],
  "effectivenessCheckDays": {
    "CRITICAL": 7,
    "HIGH": 14,
    "MEDIUM": 30,
    "LOW": 60
  }
}
```
