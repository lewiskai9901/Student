# 评级引擎完美架构 — 实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 把检查平台评分/评级模型重构为 3 轴正交体系,Indicator 成为唯一评级权威 + 跨分区灵活组合 + 多触发模式 + 版本化评级结果,真正企业级评级引擎。

**Architecture:** 3 个实体,3 个独立职责轴,零交叉耦合。

```
ScoringProfile (per project × section)   InspectionPlan (per project)
  「算分」                                    「调度」
  raw answers → finalScore                   when/how-many/sections/inspectors
  + trend/decay/calibration/multi-rater     + ratersPerTarget (no scoring)

                        Indicator (per project, N 个)
                          「评级」 — 唯一权威
                          sourceSections[] × triggerMode × aggregation
                          → IndicatorResult (版本化快照)
```

**Tech Stack:** Spring Boot 3.2 DDD · MyBatis-Plus · MySQL · Vue 3 + TS

---

## 关键架构决策

| 决策 | 选择 | 理由 |
|---|---|---|
| ScoringProfile 还要不要 GradeBand? | **删除** | 评级唯一权威是 Indicator,ScoringProfile 纯算分 |
| InspectionPlan 还要不要 scoringProfileId? | **删除** | 评分挂 section,与调度无关 |
| InspProject 还要不要 defaultScoringProfileId? | **删除** | 通过 section 直接查找,无需项目兜底 |
| Indicator 唯一性约束 | (project, name) | 删除原 (project,section) 唯一,允许多个跨分区 indicator |
| Indicator.sourceSectionIds | JSON array | 1 个 = 单分区, N 个 = 跨分区组合 |
| triggerMode | enum: TIME_WINDOW / COUNT / MANUAL | 三种业务场景 |
| 评级结果实体化 | **是,新表 indicator_results** | 已发布版本不可覆盖,补做生成 SUPERSEDED 修订版 |
| missingPolicy | per Indicator | IGNORE / ZERO / MAX / WAIT |
| latePolicy | per Indicator | REVISE_ORIGINAL / CARRY_FORWARD / EXCLUDE |
| submissionDateField | per Indicator,默认 taskDate | 任务计划日归属 |

## 撤销之前两轮的部分

本计划是对之前**评分下沉调度组**和**项目-owned scoring profile**两轮重构的"再矫正"。撤销的部分:
- `inspection_plans.scoring_profile_id` 列 → 删
- `insp_projects.default_scoring_profile_id` 列 → 删
- `cloneForProject` 服务里 plan.scoringProfileId 映射 → 删
- 调度组表单评分方案下拉 → 删
- 项目设置「默认评分方案」入口 → 删
- ScoringProfile 项目克隆里 plan.scoringProfileId 重映射 → 删

保留的(还正确的):
- ScoringProfile 项目-owned 隔离(`scoring_profile_id` 字段在 ScoringProfile 表)
- 项目克隆功能(只是 plan 那部分简化)
- ratersPerTarget 留在 InspectionPlan(那是真调度问题)

---

## Phase 1 · DB 迁移

**Files:** Create `database/migrations/V20260523_4__evaluation_engine_perfect.sql`

### 1.1 Indicator 表扩展
- 加 `source_section_ids JSON` (从单 sectionId 演进, 兼容 step: 既有 sectionId 数据 → `[sectionId]` JSON array; 旧 section_id 字段保留过渡期, Phase 2 数据迁移后删)
- 加 `trigger_mode VARCHAR(20) NOT NULL DEFAULT 'TIME_WINDOW'` (TIME_WINDOW / COUNT / MANUAL)
- 加 `count_threshold INT NULL`
- 加 `weights_by_section JSON NULL` (e.g., `{"section_a_id": 0.4, "section_b_id": 0.6}`)
- 加 `rank_direction VARCHAR(4) NULL` (ASC / DESC)
- 加 `missing_policy VARCHAR(20) NOT NULL DEFAULT 'IGNORE'` (IGNORE/ZERO/MAX/WAIT)
- 加 `late_policy VARCHAR(20) NOT NULL DEFAULT 'REVISE_ORIGINAL'` (REVISE_ORIGINAL/CARRY_FORWARD/EXCLUDE)
- 加 `submission_date_field VARCHAR(20) NOT NULL DEFAULT 'taskDate'` (taskDate/completedAt)
- 加 `name VARCHAR(100) NULL`(给 indicator 起名,如"卫生综合红旗")
- 删旧的 (project_id, section_id) 唯一约束 (若有), 加 (project_id, name) 唯一约束

### 1.2 新表 indicator_results
```sql
CREATE TABLE indicator_results (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  org_unit_id BIGINT NULL,
  indicator_id BIGINT NOT NULL,
  target_id BIGINT NOT NULL,
  target_name VARCHAR(200),
  period_key VARCHAR(50) NOT NULL,
  value DECIMAL(12,4) NULL,
  rank_position INT NULL,
  grade VARCHAR(50) NULL,
  status VARCHAR(20) NOT NULL,
  computed_at DATETIME NOT NULL,
  published_at DATETIME NULL,
  revision_of BIGINT NULL,
  source_submission_ids JSON NULL,
  source_section_ids JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  INDEX idx_indicator_target_period (indicator_id, target_id, period_key),
  INDEX idx_indicator_status (indicator_id, status, deleted)
);
```

### 1.3 撤销字段
- `inspection_plans` DROP COLUMN `scoring_profile_id`
- `insp_projects` DROP COLUMN `default_scoring_profile_id`

### 1.4 ScoringProfile 删 GradeBand 关联
- 不删 `insp_grade_bands` 表本身(Phase 2 代码迁移用),只让 ScoringProfile 不再 require GradeBand 配置就能保存

**全部 information_schema 条件化,可重复执行。**

## Phase 2 · 后端域 + 持久 + 数据迁移

**Files:** 
- `domain/inspection/model/Indicator.java`(假设位置,实际找一下)
- `domain/inspection/model/scoring/ScoringProfile.java`
- `domain/inspection/model/execution/InspectionPlan.java`
- `domain/inspection/model/execution/InspProject.java`
- 对应 PO + Repository + Mapper
- 新建 `domain/inspection/model/evaluation/IndicatorResult.java`(聚合根) + PO + Repository

### 2.1 Indicator 升级
- `sourceSectionIds: List<Long>` 取代 `sectionId`
- `triggerMode: TriggerMode` enum
- `countThreshold: Integer`
- `weightsBySection: Map<Long, BigDecimal>`
- `rankDirection: RankDirection` enum (ASC/DESC)
- `missingPolicy: MissingPolicy` enum
- `latePolicy: LatePolicy` enum
- `submissionDateField: SubmissionDateField` enum
- `name: String`
- 业务规则:
  - 至少 1 个 sourceSectionIds
  - weightsBySection 的 key 必须是 sourceSectionIds 的子集
  - countThreshold 仅 COUNT 模式需要

### 2.2 ScoringProfile 删 GradeBand
- 删 ScoringProfile.gradeBands 关联
- ScoringProfile 编辑器后端不再有 GradeBand CRUD 端点(保留 entity 仅过渡期)

### 2.3 InspectionPlan 删 scoringProfileId
- 字段 + builder + getter 全删
- `updateScoringConfig` 改名 `updateRatersPerTarget`(只保留 raters 部分)

### 2.4 InspProject 删 defaultScoringProfileId
- 字段全删,所有 service 引用同步

### 2.5 IndicatorResult 新聚合根
```java
public class IndicatorResult extends AggregateRoot<Long> {
  Long indicatorId;
  Long targetId;
  String targetName;
  String periodKey;          // e.g., "2026-W22" / "COUNT#7" / "MANUAL:2026-05-01_2026-05-31"
  BigDecimal value;
  Integer rankPosition;
  String grade;
  ResultStatus status;       // DRAFT / PUBLISHED / SUPERSEDED
  LocalDateTime computedAt;
  LocalDateTime publishedAt;
  Long revisionOf;           // 前一版本 id
  List<Long> sourceSubmissionIds;
  List<Long> sourceSectionIds;

  // 业务方法
  void publish();
  IndicatorResult supersedeWith(...);  // 生成 SUPERSEDED 链
}
```

### 2.6 一次性数据迁移
Create `infrastructure/migration/EvaluationEngineMigration.java`:
- migration_locks key: `evaluation-engine-perfect-2026-05-23`
- 步骤:
  1. 每个 ScoringProfile 的 GradeBands → 生成对应 Indicator(triggerMode=TIME_WINDOW, period=PER_TASK,sourceSectionIds=[profile.sectionId],gradeScheme 从 GradeBands 转 → 新 GradeScheme 或复用既有)
  2. 删 plan.scoringProfileId 数据(已 NULL 不需操作,字段会被 DROP)
  3. 删 project.defaultScoringProfileId 数据(同上)
  4. 既有 Indicator 数据:`source_section_ids = JSON_ARRAY(section_id)`(从旧 section_id 单列演进)

### 2.7 验证
- `mvn clean test` 全绿
- 真启动验证迁移幂等(运行 2 次第 2 次跳过)

## Phase 3 · 评级引擎核心

**Files:**
- `application/inspection/evaluation/IndicatorEvaluationService.java`(新)
- `application/inspection/evaluation/TimeWindowTrigger.java`
- `application/inspection/evaluation/CountThresholdTrigger.java`
- `application/inspection/evaluation/ManualTrigger.java`
- `application/inspection/evaluation/IndicatorComputeEngine.java`(核心计算)

### 3.1 触发器
- **TimeWindowTrigger**: 定时任务扫边界(每日 00:05 扫 DAILY,周日扫 WEEKLY,月末扫 MONTHLY)→ 收集所有需评估的 (indicator, period) 对
- **CountThresholdTrigger**: hooks into `InspSubmission.completeSubmission` 事件 → 监听器查 target 累计 count → 达 threshold 触发评估
- **ManualTrigger**: REST endpoint `POST /inspection/indicators/{id}/evaluate-manual { startDate, endDate }`

### 3.2 IndicatorComputeEngine.computeFor(indicator, target, period)
- 按 indicator.sourceSectionIds + period + submissionDateField 查 submissions
- 应用 missingPolicy(若 target 在某 source section 下无 submissions)
- 按 aggregation(SUM/AVG/WEIGHTED_AVG)聚合
- 应用 weightsBySection(若有)
- 按 rankDirection 排名(跨所有 targets)
- 按 gradeScheme 挂等级
- 返回 IndicatorResult(status=DRAFT)

### 3.3 latePolicy 处理(补做事件)
- 当一个 submission 的 `taskDate` 落在已有 PUBLISHED IndicatorResult 的 period 内:
  - REVISE_ORIGINAL: 触发该 period 重算 → 新 IndicatorResult(status=DRAFT,revisionOf=原 published.id)
  - CARRY_FORWARD: submission 的归属 period 改为当前 period(改 submissionDateField 行为)
  - EXCLUDE: 该 submission 不参与任何 Indicator 评估

### 3.4 状态转换
- DRAFT → PUBLISHED(管理员或自动 publish)
- PUBLISHED + 新 revision → 原 PUBLISHED 标 SUPERSEDED

### 3.5 验证
- `mvn clean test` 全绿
- 真启动验证 + curl 端到端跑 3 触发模式

## Phase 4 · 控制器 / DTO / API

**Files:** `interfaces/rest/inspection/` 下相关 controller

### 4.1 IndicatorController CRUD
- 加 sourceSectionIds(数组验证 ≥1)、triggerMode、countThreshold、weightsBySection、policies
- 删除"必须有 sectionId"的旧校验

### 4.2 IndicatorResultController(新)
- `GET /inspection/indicator-results?indicatorId&targetId&status` 列表
- `POST /inspection/indicator-results/{id}/publish` 手动发布
- `GET /inspection/indicator-results/{id}/history` 查修订链

### 4.3 InspProjectController 撤销字段
- `UpdateProjectRequest` 删 defaultScoringProfileId
- 端点 `/advanced-scoring` 改写入路径(profile.gradeBand 字段已删)或弃用该端点

### 4.4 InspectionPlanController 撤销字段
- create/update DTO 删 scoringProfileId
- 旧 `updateScoringConfig` 重构为 `updateRatersPerTarget`

### 4.5 ScoringProfileController
- 仍可创建 profile,但创建时不再 require GradeBand
- 删 `/advanced-scoring` 里 gradeBand 相关字段

### 4.6 验证
- `mvn clean test`、`mvn test-compile`(memory 铁律)
- 真启动

## Phase 5 · 前端

**Files:** 大量

### 5.1 删:
- ProjectDetailView 设置 tab「评分方案」卡里「默认评分方案」下拉
- SectionConfigView 调度组表单「评分方案」下拉 + 「去编辑方案」链接
- ProjectWizard 克隆模式里 scoringProfileId 重映射逻辑(自动处理)
- ScoringProfileEditor 里「评分等级」(GradeBand)编辑区

### 5.2 新:「评级配置」tab(替代或并入既有「检查计划」tab 的分区评价配置)
- 列出项目所有 Indicators
- 新建/编辑表单:name / sourceSectionIds(多选)/ triggerMode / period(时间)或 countThreshold(次数)/ weightsBySection / aggregation / gradeSchemeId / rankDirection / missingPolicy / latePolicy
- 表单按 triggerMode 动态显隐字段

### 5.3 「评级结果」展示(新)
- 项目内某 indicator 的 IndicatorResult 列表(按 target × period 二维)
- 显示当前 PUBLISHED 版 + "查看修订链" 按钮
- 手动 publish DRAFT 按钮

### 5.4 ScoringProfileEditor 精简
- 不再有 GradeBand 编辑
- 只剩算分 + trend/decay/calibration + multiRater

### 5.5 验证
- type-check 0 / lint 0 / id-number 0 / handwritten-api 不超基线 / build 绿

## Phase 6 · 守护 / 测试 / 全量终验

### 6.1 ArchUnit 守护
- ScoringProfile 不得持有 GradeBand 字段(类似上次的 NoScoringConfigOnInspProject)
- InspectionPlan 不得持有 scoringProfileId
- InspProject 不得持有 defaultScoringProfileId

### 6.2 单测覆盖
- Indicator domain test:三 triggerMode 校验、weightsBySection 合法性、policies 行为
- IndicatorResult test:supersedeWith、状态转换
- IndicatorComputeEngine test:跨分区聚合、missing policy、late policy、rank direction
- Trigger 各自测

### 6.3 端到端冒烟(curl)
- 创建项目 → 创建 ScoringProfile(无 GradeBand)→ 创建 Indicator(跨分区)→ 跑 mock 任务 → 触发评估 → 查 IndicatorResult → publish → 补做 submission → 触发 supersede → 查修订链

### 6.4 真启动 2 次幂等
- 第 1 次跑迁移 + 评级引擎初始化
- 第 2 次跳过 + boot OK

---

## 注意事项(memory 教训)

- 后端 commit 前**必须真 `mvn spring-boot:run` 启动验证**
- 派 fix-agent 必须要求 `mvn test-compile`
- 雪花 LongId 前端勿 `Number()`
- 评级结果 IndicatorResult 是有版本快照的,补做触发新版,**不覆盖**
- 迁移用 migration_locks 表幂等标记

## 完成标准

- ✅ 3 实体(ScoringProfile / Indicator / InspectionPlan)零交叉耦合
- ✅ Indicator 支持跨分区任意组合
- ✅ 3 触发模式可工作(TIME / COUNT / MANUAL)
- ✅ missingPolicy / latePolicy 业务规则生效
- ✅ IndicatorResult 版本快照 + 修订链
- ✅ ScoringProfile 不再含评级职责
- ✅ InspectionPlan 不再含评分职责
- ✅ 全部测试绿 + 真启动 + 端到端 curl 冒烟通过
