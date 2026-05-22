# 评分配置下沉调度组 — 实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 把检查平台散落 3 处的评分配置收敛为「ScoringProfile(可复用规则)+ InspectionPlan 调度组(执行单元引用规则)」两层清晰模型。

**Architecture:** 评分"怎么算"的唯一权威是 `ScoringProfile`;调度组通过 `scoringProfileId` 引用它,并自带 `ratersPerTarget`(取代 `evaluationMode` 枚举);项目只保留 `defaultScoringProfileId` 作为非计划任务的兜底 + 新调度组预填。评分引擎按"任务来源调度组"解析配置。

**Tech Stack:** Spring Boot 3.2 DDD + MyBatis-Plus + MySQL + Vue 3 + TypeScript。

---

## 设计决策(权威)

| 概念 | 职责 | 评分相关字段 |
|---|---|---|
| `ScoringProfile` | 可复用评分规则 | gradeScheme(分区级)、calcRules、`multiRaterMode`(合并算法)、trend/decay/calibration 全参数、`evaluationPeriod` |
| `InspectionPlan`(调度组) | 执行单元 | **新增** `scoringProfileId`、`ratersPerTarget`(int,1=单人 >1=多人) |
| `InspProject` | 项目骨架 | **仅保留** `defaultScoringProfileId`;删 8+ 评分列 |

**评分解析规则(唯一):** `task.planId != null ? (plan.scoringProfileId, plan.ratersPerTarget) : (project.defaultScoringProfileId, 1)`

**硬校验:** `ratersPerTarget ≤ 调度组可用检查员数`。

**废弃:** `evaluationMode` 枚举/字段、项目上的 multi_rater/trend/decay/calibration 列、每分区 evalForm 的独立 gradeSchemeId(并入 ScoringProfile)。

---

## Phase 1 — DB 迁移

**Files:** Create `database/migrations/V20260523_1__scoring_config_to_plan.sql`

- `inspection_plans` ADD `scoring_profile_id BIGINT NULL`、`raters_per_target INT NOT NULL DEFAULT 1`。
- `insp_projects` ADD `default_scoring_profile_id BIGINT NULL`。
- 存量迁移:
  - `UPDATE inspection_plans p JOIN insp_projects pr ON p.project_id=pr.id SET p.scoring_profile_id=pr.scoring_profile_id, p.raters_per_target=IF(pr.evaluation_mode='MULTI',2,1)`
  - `UPDATE insp_projects SET default_scoring_profile_id=scoring_profile_id`
- `insp_projects` DROP:`scoring_profile_id`、`evaluation_mode`、`multi_rater_mode`、`rater_weight_by`、`consensus_threshold`、`trend_enabled`、`trend_lookback_days`、`decay_enabled`、`decay_mode`、`calibration_enabled`、`calibration_method`、`split_strategy`。
- 全部 information_schema 条件化,可重复执行。
- **验证:** apply 到 student_management,SHOW COLUMNS 确认。

## Phase 2 — 后端领域 + 持久层

**Files:** `domain/inspection/model/execution/InspectionPlan.java`、`InspProject.java`、对应 PO + RepositoryImpl + Mapper。

- `InspectionPlan`:加 `scoringProfileId`(Long)、`ratersPerTarget`(int,默认 1);builder + `updateScoringConfig(profileId, ratersPerTarget)` 方法,校验 `ratersPerTarget ≥ 1`。
- `InspProject`:删 `evaluationMode` 及 trend/decay/calibration 等字段;`scoringProfileId` 语义改为 `defaultScoringProfileId`(字段重命名)。
- `InspectionPlanPO` + `scoring_profile_id`/`raters_per_target`;`InspProjectPO` 删列 + 重命名;两个 RepositoryImpl 的 toPO/toDomain 同步。
- 删 `EvaluationMode` 相关枚举/常量(若有)。
- **验证:** `mvn -q compile -DskipTests`。

## Phase 3 — 后端服务 / 控制器 / 评分引擎

**Files:** `application/inspection/*`(InspProjectApplicationService、InspectionPlanApplicationService、ScoreAggregationService、ScoreCalculationDomainService、任务生成相关)、`interfaces/rest/inspection/InspProjectController.java`、`InspectionPlanController`。

- 评分解析:评分引擎/聚合服务读"配置"处,从 `project.xxx` 改为按 `task.planId` 解析(planId 非空取 plan,否则取 project.defaultScoringProfileId)。
- 任务生成:调度组生成任务时按 `ratersPerTarget` 为每目标生成 N 份 submission;校验检查员数。
- Plan create/update DTO + 端点:加 `scoringProfileId`、`ratersPerTarget`。
- Project DTO:`UpdateProjectRequest`/`CreateProjectRequest` 用 `defaultScoringProfileId`;删 evaluationMode。
- `updateInfo` 同步(部分更新语义保持)。
- **验证:** `mvn -q compile -DskipTests` + `mvn test-compile`。

## Phase 4 — 前端

**Files:** `views/inspection/projects/ProjectDetailView.vue`、`components/SectionConfigView.vue`、`ProjectWizardView.vue`、`types/insp/*`、`api/inspection/project.ts`。

- ProjectDetailView 设置 tab:**删除「高级评分设置卡」**;运营配置或基本信息区加一个轻量「默认评分方案」选择器(`defaultScoringProfileId`)。
- SectionConfigView 调度组表单:加「评分方案」下拉 +「去编辑方案」链接(跳 ScoringProfileEditor)+ `ratersPerTarget` 数字框(实时校验 ≤ `inspectorIds.length`,超了红字禁止保存)。
- ProjectWizard:建默认调度组时带上 scoringProfileId(取项目默认)。
- 类型:`InspectionPlan`/`CreatePlanRequest` 加字段;`InspProject` 字段调整;删 evaluationMode。
- **验证:** type-check / lint / build 三闸。

## Phase 5 — 测试 / 守护 / 全量验证

- 修因字段移除而失败的既有测试(evaluationMode、项目评分列相关)。
- 新增单测:评分解析规则(planId 分支)、`ratersPerTarget` 校验、任务生成 N 份。
- ArchUnit 守护:防项目上重新出现评分配置字段。
- **全量验证:** `mvn clean test`(全绿)+ **后端真启动**(memory 铁律)+ 前端 4 闸 + curl 实测创建调度组带评分方案。

---

## 注意事项(memory 教训)

- 后端改动 commit 前**必须真 `mvn spring-boot:run` 启动验证**,不能只信 `mvn test`。
- 派 fix-agent 必须要求跑 `mvn test-compile`,行为改了同步改测试。
- 雪花 ID 是 LongId(string),前端勿 `Number()`。
- 迁移 apply 前查存量重复/约束冲突。
- 每个 Phase 独立 commit + 验证通过才推进。
