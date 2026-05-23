# 评分方案项目-owned + 项目克隆 — 实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** `ScoringProfile` 从"模板/跨项目共享"变为"项目-owned",评分规则配置统一在项目内,跨项目共享通过显式「基于现有项目克隆」实现。

**Architecture:** `ScoringProfile` 加 `project_id NOT NULL`,与项目同生命周期;查询/创建强制按项目隔离;删除项目级联清理 owned profiles。新增 `POST /inspection/projects/{id}/clone` 深拷贝端点(project + owned profiles + bands/rules/dimensions + plans + indicators,**不**拷贝执行数据 tasks/submissions/scores)。前端移除独立「评分方案」菜单,合并入项目配置;创建项目向导加「克隆自现有项目」分支。

**Tech Stack:** Spring Boot 3.2 DDD · MyBatis-Plus · MySQL · Vue 3 + TypeScript。

---

## 关键设计决策

| 决策 | 选择 | 理由 |
|---|---|---|
| 数据模型 | `ScoringProfile.project_id NOT NULL` | 物理隔离 > 隐式共享 |
| 唯一性 | `(project_id, section_id)` 唯一索引 | 每项目每分区至多一套 |
| 关联实体(GradeBand/CalculationRule/ScoreDimension) | 不改,自然跟随 ScoringProfile | 它们已经 `scoring_profile_id` FK |
| 共享场景 | 通过「克隆项目」实现 | 显式 > 隐式 |
| 模板编辑器 | **不**再含评分配置 UI | 模板=结构,项目=规则 |
| 全局「评分方案」菜单 | **删除** | 入口只在项目内 |
| 克隆范围 | 项目设置 + ScoringProfile 全套 + 调度组 + Indicator | 执行数据(任务/提交/分数)不克隆 |

## 存量数据策略

memory 原则「开发阶段不考虑旧数据兼容」+ 当前 dev DB 数据极少:
- 迁移时为每个项目 × 它引用过的每个 ScoringProfile,**写新副本**(含关联 bands/rules/dimensions),并把项目/调度组的引用指向新副本。
- 没有任何项目引用的 orphan ScoringProfile **直接删除**。
- 复杂逻辑放在 Java `@PostConstruct` 一次性迁移(`OncePerSchemaUpgrade` 风格 idempotent 标记表),不用纯 SQL,避免 LAST_INSERT_ID 循环噩梦。

---

## Phase 1 · DB 列与索引

**Files:** Create `database/migrations/V20260523_2__scoring_profile_project_owned.sql`

- `ALTER TABLE insp_scoring_profiles ADD COLUMN project_id BIGINT NULL AFTER section_id` (条件化)。
- 删除旧索引 `uk_scoring_profile_section`(若有),建临时索引 `idx_scoring_profile_section` 便于后续 join。
- **不**在此 phase 加 NOT NULL — 等 Phase 2 Java 端把数据迁完再补 NOT NULL 约束(Phase 5)。
- 验证:`SHOW COLUMNS FROM insp_scoring_profiles` 含 project_id。

## Phase 2 · 后端域 + 持久 + 数据迁移

**Files:**
- `domain/inspection/model/scoring/ScoringProfile.java` + `projectId` 字段、builder、getter。
- `infrastructure/persistence/inspection/scoring/ScoringProfilePO.java` + `projectId`。
- `ScoringProfileRepositoryImpl` toPO/toDomain 同步。
- `ScoringProfileMapper` 加 `findByProjectId(Long)`。
- Create `infrastructure/migration/ScoringProfileOwnershipMigration.java` —— `@Component @ConditionalOnProperty` 启动一次性迁移,用 `migration_locks` 表幂等标记。
- `ScoringProfileApplicationService.createProfile` 必传 projectId;`list*` 全部按 projectId 过滤;新增 `cloneForProject(profileId, newProjectId)` 内部用。
- `InspProjectApplicationService.deleteProject` 之前是"有任务禁删";仍保留,**额外**:删项目前清理 owned ScoringProfiles + 关联 bands/rules/dimensions(级联,因 plans/indicators 也已 owned)。
- 验证:`mvn -q compile -DskipTests` + 真启动 + 启动后查 DB:每项目的 scoring_profile_id 落在 owned profile 上。

## Phase 3 · 项目克隆功能(后端)

**Files:**
- `interfaces/rest/inspection/InspProjectController.java` 加 `POST /{id}/clone`,DTO `CloneProjectRequest { projectName(NotBlank), orgUnitId(NotNull), startDate(NotNull) }`。
- `InspProjectApplicationService.cloneProject(sourceId, request, userId)`:
  - `@Transactional`,深拷贝顺序:Project → ScoringProfiles(+bands+rules+dimensions)→ InspectionPlans(scoringProfileId 重映射)→ Indicators(按 section 重映射,如需)。
  - 新项目状态 `DRAFT`,新 projectCode 雪花生成。
  - 不拷贝:tasks / submissions / evidences / audit logs / scores / inspectors(检查员名单是否拷贝?**初版克隆**)。
- 验证:`mvn clean test` 全绿 + `curl POST /clone` 实测:新项目独立,改 source 不影响 clone。

## Phase 4 · 前端

**Files:**
- `src/router/modules/inspection.ts` —— 删除 `/inspection/scoring-profiles` 主菜单(保留路由可供项目内跳转,但 menu hidden)。
- `src/views/inspection/projects/ProjectDetailView.vue` config tab —— 加「评分方案」子区(列出本项目 profiles + 跳详细编辑)。
- `src/views/inspection/projects/ProjectWizardView.vue` Step 0 —— 加「克隆自现有项目」分支:选源项目 + 填新名/起始日/orgUnitId → POST `/clone`,跳转新项目详情。
- `src/views/inspection/projects/components/SectionConfigView.vue` 调度组 profile 下拉 —— 限定 `where projectId = currentProjectId`。
- `src/api/inspection/project.ts` 加 `cloneProject(sourceId, data)`。
- `src/api/inspection/scoring.ts` `getProfiles` 加 `projectId` 必填参数。
- 验证:type-check 0 / lint 0 / id-number 0 / handwritten-api 不超基线 / build 绿。

## Phase 5 · 守护 + NOT NULL + 全量验证

**Files:**
- Create `database/migrations/V20260523_3__scoring_profile_project_id_not_null.sql` —— `ALTER COLUMN project_id BIGINT NOT NULL` + `CREATE UNIQUE INDEX uk_scoring_profile_project_section ON insp_scoring_profiles (project_id, section_id)`(条件化)。
- Create `backend/src/test/.../ScoringProfileMustHaveProjectIdTest.java` —— 反射守护 + 一个 PO 级断言"project_id 字段存在且必非 null mapping"。
- 全量验证:
  - `mvn clean test` 全绿(2014 + 新增 ≈ 2020)。
  - 后端真启动。
  - `curl POST /clone` 端到端:source 不变,clone 独立 profiles。
  - 前端 5 闸全绿。
  - **冒烟脚本**:同一 section 在 A、B 两项目下编辑各自 ScoringProfile,互不影响。

---

## 注意事项(memory 教训)

- 后端 commit 前必须真 `mvn spring-boot:run` 启动验证。
- 派 fix-agent 必须要求 `mvn test-compile`,行为改了同步改测试。
- 雪花 LongId 前端勿 `Number()`。
- 数据迁移用 `migration_locks` 表幂等标记,反复重启不重做。
- 克隆功能默认**不**拷检查员名单(每项目独立配),最小可用。

## 验证完成标准

✅ 同一 section 在两个项目下独立 ScoringProfile · ✅ 创建项目向导可选「克隆」· ✅ 全局「评分方案」菜单消失 · ✅ 模板编辑器无评分 UI · ✅ 全量测试 + 真启动 + 5 闸绿。
