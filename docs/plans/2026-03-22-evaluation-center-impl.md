# Evaluation Center Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build an independent evaluation center module with multi-condition multi-level campaign engine, supporting cross-system data sources (inspection scores, entity events, evaluation history).

**Architecture:** New `domain/evaluation/` package independent of inspection. Adapter pattern for condition evaluators. Campaign → Levels → Conditions hierarchy with cascading UI. Results include per-condition breakdown and upgrade hints.

**Tech Stack:** Spring Boot 3.2, MyBatis Plus, Vue 3 Composition API, scoped CSS (no Tailwind/Element Plus)

---

### Task 1: Database Migration + Domain Models

**Files:**
- Create: `database/migrations/V67.0.0__evaluation_center.sql`
- Create: `backend/src/main/java/com/school/management/domain/evaluation/model/EvalCampaign.java`
- Create: `backend/src/main/java/com/school/management/domain/evaluation/model/EvalLevel.java`
- Create: `backend/src/main/java/com/school/management/domain/evaluation/model/EvalCondition.java`
- Create: `backend/src/main/java/com/school/management/domain/evaluation/model/EvalBatch.java`
- Create: `backend/src/main/java/com/school/management/domain/evaluation/model/EvalResult.java`
- Create: `backend/src/main/java/com/school/management/domain/evaluation/repository/EvalCampaignRepository.java`
- Create: `backend/src/main/java/com/school/management/domain/evaluation/repository/EvalBatchRepository.java`
- Create: `backend/src/main/java/com/school/management/domain/evaluation/repository/EvalResultRepository.java`

**Step 1: Create DB migration**
- 5 tables: eval_campaigns, eval_levels, eval_conditions, eval_batches, eval_results
- Copy exact SQL from design doc

**Step 2: Create domain models**
- EvalCampaign: @Getter @Builder, fields match DB, factory method `create(name, targetType, createdBy)`
- EvalLevel: @Getter @Builder, fields match DB
- EvalCondition: @Getter @Builder, fields match DB, `sourceConfig` and `threshold` are String (JSON)
- EvalBatch: @Getter @Builder
- EvalResult: @Getter @Builder

**Step 3: Create repository interfaces**
- EvalCampaignRepository: save, findById, findAll, findByStatus, deleteById
- EvalBatchRepository: save, findByCampaignId, findById
- EvalResultRepository: save, findByBatchId, findByCampaignAndTarget, deleteByBatchId

**Step 4: Verify** `mvn compile -DskipTests` passes

**Step 5: Commit** `feat: evaluation center - database migration + domain models`

---

### Task 2: Infrastructure Layer (PO + Mapper + RepositoryImpl)

**Files:**
- Create: `backend/.../infrastructure/persistence/evaluation/EvalCampaignPO.java`
- Create: `backend/.../infrastructure/persistence/evaluation/EvalCampaignMapper.java`
- Create: `backend/.../infrastructure/persistence/evaluation/EvalCampaignRepositoryImpl.java`
- Create: same pattern for EvalLevel, EvalCondition, EvalBatch, EvalResult (15 files total)

**Step 1: Create PO classes**
- @Data @TableName("eval_campaigns") etc.
- Fields match domain models, @TableId(type = IdType.AUTO)
- EvalCondition: sourceConfig, threshold are String

**Step 2: Create Mapper interfaces**
- Extend BaseMapper<XxxPO>
- EvalLevelMapper: @Select findByCampaignId
- EvalConditionMapper: @Select findByLevelId
- EvalBatchMapper: @Select findByCampaignId ordered by cycle_start DESC
- EvalResultMapper: @Select findByBatchId ordered by rank_no

**Step 3: Create RepositoryImpl**
- Standard toDomain/toPO mapping
- EvalCampaignRepositoryImpl.findById should also load levels and conditions (eager or separate queries)

**Step 4: Verify** `mvn compile -DskipTests`

**Step 5: Commit** `feat: evaluation center - infrastructure persistence layer`

---

### Task 3: Condition Evaluators (Engine)

**Files:**
- Create: `backend/.../domain/evaluation/engine/ConditionEvaluator.java` (interface)
- Create: `backend/.../domain/evaluation/engine/ConditionResult.java` (value object)
- Create: `backend/.../domain/evaluation/engine/InspectionEvaluator.java`
- Create: `backend/.../domain/evaluation/engine/EventEvaluator.java`
- Create: `backend/.../domain/evaluation/engine/HistoryEvaluator.java`
- Create: `backend/.../domain/evaluation/engine/EvaluationEngine.java`

**Step 1: Create ConditionEvaluator interface**
```java
public interface ConditionEvaluator {
    ConditionResult evaluate(EvalCondition condition, Long targetId, String targetType,
                             LocalDate cycleStart, LocalDate cycleEnd);
}

public record ConditionResult(boolean passed, String actualValue, String description) {}
```

**Step 2: Create InspectionEvaluator**
- Inject InspSubmissionRepository, InspTaskRepository, GradeBandRepository
- Parse sourceConfig JSON for projectId/sectionId
- Implement metrics: SCORE_AVG, SCORE_MIN, SCORE_EVERY, GRADE_EVERY, FAIL_COUNT
- Handle scope: SELF queries submissions where targetId matches, MEMBERS queries via AccessRelation

**Step 3: Create EventEvaluator**
- Inject EntityEventRepository
- Parse sourceConfig for eventType
- Implement metrics: COUNT, SCORE_SUM
- Handle scope: SELF/MEMBERS/SPECIFIC_ROLE

**Step 4: Create HistoryEvaluator**
- Inject EvalResultRepository
- Implement metrics: PREV_LEVEL, CONSECUTIVE, RANK_PERCENTILE, TREND
- CONSECUTIVE: count consecutive batches where level_num <= threshold

**Step 5: Create EvaluationEngine**
- @Component, inject all evaluators + repositories
- `execute(campaignId, cycleStart, cycleEnd, executedBy)`:
  1. Load campaign + levels + conditions
  2. Resolve targets from scopeOrgIds + targetType (use TargetPopulationService)
  3. For each target: evaluate levels top-down, record condition details
  4. Generate upgrade hints
  5. Rank by score
  6. Save batch + results
  7. Publish EntityEvent for top-level results

**Step 6: Verify** `mvn compile -DskipTests`

**Step 7: Commit** `feat: evaluation center - condition evaluators + engine`

---

### Task 4: Application Service + Controllers

**Files:**
- Create: `backend/.../application/evaluation/EvalCampaignApplicationService.java`
- Create: `backend/.../application/evaluation/EvalConditionOptionsService.java`
- Create: `backend/.../interfaces/rest/evaluation/EvalCampaignController.java`
- Create: `backend/.../interfaces/rest/evaluation/EvalResultController.java`

**Step 1: Create EvalCampaignApplicationService**
- CRUD: createCampaign, updateCampaign, deleteCampaign, getCampaign, listCampaigns
- Levels: saveLevels(campaignId, List<LevelWithConditions>)
- Execute: executeCampaign(campaignId, cycleStart, cycleEnd, userId)
- Batch: listBatches(campaignId)

**Step 2: Create EvalConditionOptionsService**
- listProjects(): from InspProjectRepository
- listSections(projectId): from TemplateSectionRepository
- listGradeBands(sectionId): from GradeBandRepository
- listEventTypes(): from EntityEventTypeRepository

**Step 3: Create EvalCampaignController**
- REST endpoints matching API design doc
- Request DTOs as inner records
- @CasbinAccess(resource = "eval:campaign")

**Step 4: Create EvalResultController**
- GET /eval/batches/:batchId/results
- GET /eval/results/target/:type/:id

**Step 5: Verify** `mvn compile -DskipTests`

**Step 6: Commit** `feat: evaluation center - application services + REST controllers`

---

### Task 5: Run DB Migration

**Step 1:** Execute SQL migration against database
```bash
mysql -u root -p123456 student_management < database/migrations/V67.0.0__evaluation_center.sql
```

**Step 2:** Verify tables created
```bash
mysql -u root -p123456 student_management -e "SHOW TABLES LIKE 'eval_%';"
```

**Step 3:** Restart backend, verify no startup errors

**Step 4: Commit** (if any fixes needed)

---

### Task 6: Frontend Types + API

**Files:**
- Create: `frontend/src/types/evaluation.ts`
- Create: `frontend/src/api/evaluation.ts`

**Step 1: Create TypeScript types**
- EvalCampaign, EvalLevel, EvalCondition, EvalBatch, EvalResult
- ConditionSourceType, ConditionMetric configs (label + description per metric)
- CascadingConfig: which metrics are available per source type

**Step 2: Create API module**
- Campaign CRUD, level save, execute, batch list, result list
- Options APIs: projects, sections, gradeBands, eventTypes

**Step 3: Verify** `npm run build`

**Step 4: Commit** `feat: evaluation center - frontend types + API`

---

### Task 7: Campaign List Page (评级中心首页)

**Files:**
- Create: `frontend/src/views/evaluation/EvalCampaignListView.vue`
- Modify: `frontend/src/router/index.ts` (add routes)

**Step 1: Add routes**
```
/evaluation → EvalCampaignListView (一级菜单，icon: Award, order: 8)
/evaluation/campaigns/create → EvalCampaignEditView (hidden)
/evaluation/campaigns/:id → EvalCampaignEditView (hidden)
/evaluation/batches/:batchId → EvalResultDetailView (hidden)
```

**Step 2: Build campaign list page**
- Header: "评级中心" + [新建评选] button
- Stats bar: 运行中 N | 草稿 N | 已结束 N
- Campaign cards: name, target type, period, level count, condition count, last result summary
- Actions: 查看结果, 立即执行, 编辑, 删除
- Scoped CSS, no Tailwind/Element Plus
- Design spec: #1a6dff primary, 8px card radius, 12px font

**Step 3: Verify** `npm run build`

**Step 4: Commit** `feat: evaluation center - campaign list page`

---

### Task 8: Campaign Edit Page (评选活动编辑)

**Files:**
- Create: `frontend/src/views/evaluation/EvalCampaignEditView.vue`
- Create: `frontend/src/views/evaluation/components/ConditionEditor.vue`

**Step 1: Build campaign edit page (two-column layout)**
- Left: configuration form
  - Basic info: name, targetType, period, scope (org tree selector)
  - Level definitions: add/remove levels, each with name + condition logic
  - Per level: condition list with add/remove
  - Each condition: natural language display, click to edit
- Right: live preview (read-only summary of all levels + conditions)
- Bottom: execution history table

**Step 2: Build ConditionEditor component (modal)**
- Cascading UI:
  - Source type selector (检查分数 / 事件记录 / 评选历史)
  - Source-specific params (project+section / event type / campaign)
  - Metric selector (filtered by source type)
  - Metric-specific threshold input (number / grade checkboxes / level selector)
  - Scope selector (SELF / MEMBERS / SPECIFIC_ROLE)
  - Time range (CYCLE / custom days)
- Auto-generate description preview at bottom
- Load options dynamically: projects → sections → grade bands (cascading API calls)

**Step 3: Verify** `npm run build`

**Step 4: Commit** `feat: evaluation center - campaign editor + condition builder`

---

### Task 9: Result Detail Page (评选结果详情)

**Files:**
- Create: `frontend/src/views/evaluation/EvalResultDetailView.vue`

**Step 1: Build result detail page**
- Header: campaign name + cycle date
- Summary stats: level distribution (五星×3 四星×12 ...)
- Result list: each target as a card
  - Rank number (#1, #2, #3 with medal icons for top 3)
  - Target name + level
  - Condition breakdown: ✓/✗ per condition with actual vs threshold
  - Upgrade hint (what to improve)
- Actions: 导出Excel, 发布荣誉事件

**Step 2: Verify** `npm run build`

**Step 3: Commit** `feat: evaluation center - result detail page with upgrade hints`

---

### Task 10: Integration + Migration

**Files:**
- Modify: `frontend/src/views/inspection/v7/projects/ProjectDetailView.vue`

**Step 1: Update inspection project detail**
- "评级规则" Tab → rename to "关联评选"
- Content: link to evaluation center + list of campaigns that reference this project's sections
- Remove old RatingDimensionConfig component usage

**Step 2: Verify** both `mvn compile -DskipTests` and `npm run build`

**Step 3: Commit** `feat: evaluation center - integrate with inspection project detail`

---

## Execution Order

Tasks 1-5: Backend (sequential, each builds on previous)
Tasks 6-9: Frontend (sequential, each builds on previous)
Task 10: Integration (after both backend and frontend)

Tasks 1-4 can run as one agent batch (backend only).
Tasks 6-9 can run as one agent batch (frontend only).

Total: ~10 tasks, backend and frontend can be parallelized.
