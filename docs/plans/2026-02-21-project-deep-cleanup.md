# Project Deep Cleanup Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Eliminate all dead code, unify coding styles, and resolve historical inconsistencies accumulated from multiple AI model refactors across the entire codebase.

**Architecture:** DDD hexagonal architecture (domain/application/infrastructure/interfaces). Frontend is Vue 3 + TypeScript + Pinia + Element Plus + Tailwind CSS. Backend is Spring Boot 3.2 + MyBatis Plus + Redis + MySQL.

**Tech Stack:** Java 17, Spring Boot 3.2, MyBatis Plus 3.5, Vue 3, TypeScript, Vite, Tailwind CSS, Element Plus

---

## Analysis Summary

| Category | Count | Impact |
|----------|-------|--------|
| Dead frontend files (components, views, API, types, stores, utils) | ~95 files | Build bloat, confusion |
| Dead backend files (domain, infra, config, rest) | ~45 files | Spring bean pollution, startup overhead |
| Dead DB tables (V3/V4 inspection, old audit, etc.) | ~34 tables | Schema confusion |
| Missing tables in canonical schema | ~34 tables | Schema out of sync |
| Duplicate migration versions | 13+ collisions | Flyway startup failure |
| Router redirect bloat | ~70 redirects | Router noise |
| Style inconsistencies (13 getCurrentUserId copies, mixed DI, etc.) | High | Bug risk, maintenance cost |

---

## Phase 1: Delete Dead Frontend Files (~95 files)

### Task 1.1: Delete dead `shared/` directory (21 files)

**Files:** Delete entire `frontend/src/shared/` directory

**Evidence:** Zero `@/shared/` imports anywhere in the codebase. Contains duplicate copies of `utils/token.ts`, `utils/request.ts`, `views/DashboardView.vue`, etc.

**Step 1:** Verify no imports exist
```bash
grep -rn "@/shared/" frontend/src/ --include="*.ts" --include="*.vue"
# Expected: zero results
```

**Step 2:** Delete directory
```bash
rm -rf frontend/src/shared/
```

**Step 3:** Build verify
```bash
cd frontend && npx vite build
```

---

### Task 1.2: Delete dead component libraries (`v2/`, `ui/`, `design-system.ts`) (~29 files)

**Files:**
- Delete: `frontend/src/components/v2/` (8 files)
- Delete: `frontend/src/components/ui/` (20 files)
- Delete: `frontend/src/plugins/design-system.ts`

**Evidence:** Zero imports from any view/component.

**Step 1:** Verify no imports
```bash
grep -rn "components/v2\|components/ui\|plugins/design-system" frontend/src/ --include="*.ts" --include="*.vue" | grep -v "^Binary"
# Expected: zero results (or only self-references)
```

**Step 2:** Delete
```bash
rm -rf frontend/src/components/v2/
rm -rf frontend/src/components/ui/
rm -f frontend/src/plugins/design-system.ts
```

**Step 3:** Build verify

---

### Task 1.3: Delete dead dormitory components (11 files)

**Files:** Delete `frontend/src/components/dormitory/` directory

**Evidence:** All 11 components have zero external imports. Dormitory UI migrated to Place management.

**Step 1:** Verify, delete, build verify

---

### Task 1.4: Delete dead quantification components (8 files)

**Files:** Delete `frontend/src/components/quantification/` directory

**Evidence:** All 8 files reference non-existent `@/api/quantification`. Zero external imports. V4 quantification replaced by V6 inspection.

---

### Task 1.5: Delete dead classroom components (3 files)

**Files:** Delete `frontend/src/components/classroom/` directory

**Evidence:** `ClassroomControlPanel.vue`, `ClassroomFormDialog.vue`, `ClassAssignmentDialog.vue` - zero imports. Classroom UI migrated to Place management.

---

### Task 1.6: Delete dead inspection/permission barrel components (7 files)

**Files:**
- Delete: `frontend/src/components/inspection/ClassRankingTable.vue`
- Delete: `frontend/src/components/inspection/CorrectiveOrderCard.vue`
- Delete: `frontend/src/components/inspection/DeductionItemPicker.vue`
- Delete: `frontend/src/components/inspection/PersonalInspectionRecords.vue`
- Delete: `frontend/src/components/inspection/index.ts`
- Delete: `frontend/src/components/permission/RoleDataPermissionDialog.vue`
- Delete: `frontend/src/components/permission/CustomScopeItemDialog.vue`
- Delete: `frontend/src/components/permission/index.ts`

**Evidence:** `inspection/index.ts` and `permission/index.ts` barrels never imported. Individual components only referenced from those barrels.

---

### Task 1.7: Delete other dead components (4 files)

**Files:**
- Delete: `frontend/src/components/relation/AccessRelationManager.vue` (zero imports)
- Delete: `frontend/src/components/user/UserDataScopeManager.vue` (broken dep on non-existent api)
- Delete: `frontend/src/components/settings/FeatureListEditor.vue` (zero imports)
- Delete: `frontend/src/views/organization/structure/components/OrgTreeCard.vue` (only self-recursive, zero external)

---

### Task 1.8: Delete dead views (5 files + design previews)

**Files:**
- Delete: `frontend/src/views/organization/structure/OrgStructureV2.vue` (not in router)
- Delete: `frontend/src/views/system/ScoringStrategyConfig.vue` (not in router)
- Delete: `frontend/src/views/task/components/AssigneeCardGrid.vue` (zero imports)
- Delete: `frontend/src/views/organization/designs/OrgDesignA.vue`
- Delete: `frontend/src/views/organization/designs/OrgDesignB.vue`
- Delete: `frontend/src/views/organization/designs/OrgDesignC.vue`
- Delete: `frontend/src/views/organization/structure/components/DepartmentForm.vue` (verify dead first)
- Delete: `frontend/src/views/organization/structure/components/DeptTableRow.vue` (verify dead first)
- Delete: `frontend/src/views/organization/structure/components/DeptTreeNode.vue` (verify dead first)

Remove design preview routes from `router/index.ts` (lines 172-190).

---

### Task 1.9: Delete dead API files (9 files)

**Files:**
- Delete: `frontend/src/api/orgRanking.ts`
- Delete: `frontend/src/api/departmentRanking.ts`
- Delete: `frontend/src/api/placeCategory.ts`
- Delete: `frontend/src/api/rating.ts` (~1400 lines of dead code)
- Delete: `frontend/src/api/bonusItem.ts`
- Delete: `frontend/src/api/teacherDashboard.ts`
- Delete: `frontend/src/api/inspection.ts` (stub, only used by dead store)
- Delete: `frontend/src/api/task/message.ts`
- Delete: `frontend/src/api/task.ts` (duplicate of `api/task/index.ts`, only used by dead store)

Clean up `frontend/src/api/index.ts` - remove all dead re-exports, or delete entirely if barrel becomes empty.

---

### Task 1.10: Delete dead type files (6 files)

**Files:**
- Delete: `frontend/src/types/building.ts`
- Delete: `frontend/src/types/buildingDepartmentAssignment.ts`
- Delete: `frontend/src/types/buildingDormitory.ts`
- Delete: `frontend/src/types/analytics.ts`
- Delete: `frontend/src/types/placeCategory.ts`
- Delete: `frontend/src/types/inspection.ts` (stub)

Clean up `frontend/src/types/index.ts` - remove references to deleted type files.

---

### Task 1.11: Delete dead stores (7 files)

**Files:**
- Delete: `frontend/src/stores/access.ts`
- Delete: `frontend/src/stores/dormitory.ts`
- Delete: `frontend/src/stores/inspection.ts`
- Delete: `frontend/src/stores/organization.ts`
- Delete: `frontend/src/stores/student.ts`
- Delete: `frontend/src/stores/task.ts`
- Delete: `frontend/src/stores/index.ts` (barrel, never imported)

**Caution:** Verify `stores/auth.ts` and `stores/config.ts` are NOT in this list (they ARE actively used).

---

### Task 1.12: Build verification after all frontend deletions

**Step 1:** Full build
```bash
cd frontend && npx vite build
```

**Step 2:** Type check
```bash
cd frontend && npx vue-tsc --noEmit
```

**Step 3:** Fix any broken imports that surface.

---

## Phase 2: Delete Dead Backend Files (~45 files)

### Task 2.1: Delete dead domain packages (15 files)

**Files:**
- Delete: `backend/.../domain/behavior/` (12 files) - entire behavior domain, zero references
- Delete: `backend/.../domain/analytics/` (3 files) - entire analytics domain, zero references

---

### Task 2.2: Delete dead infrastructure files (~15 files)

**Files:**
- Delete: `backend/.../infrastructure/persistence/analytics/` (3 files)
- Delete: `backend/.../infrastructure/persistence/system/QuantificationDictCategoryMapper.java`
- Delete: `backend/.../infrastructure/persistence/system/QuantificationDictCategoryPO.java`
- Delete: `backend/.../infrastructure/persistence/query/QueryMapper.java`
- Delete: `backend/.../infrastructure/context/ContextPropagatingTaskDecorator.java`
- Delete: `backend/.../infrastructure/query/QueryOptimizationService.java`
- Delete: `backend/.../infrastructure/access/PermissionChecker.java`
- Delete: `backend/.../infrastructure/cache/InspectionCacheService.java`
- Delete: `backend/.../infrastructure/scoring/JavaScriptFormulaEngine.java`
- Delete: `backend/.../domain/scoring/service/FormulaEngineService.java` (interface for above)
- Delete: `backend/.../infrastructure/event/outbox/` (3 files) - broken outbox pattern
- Delete: `backend/.../common/util/DataPermissionContextHolder.java`

---

### Task 2.3: Delete dead config/security files (5 files)

**Files:**
- Delete: `backend/.../config/FlowableConfig.java`
- Delete: `backend/.../config/FeatureToggleConfig.java`
- Delete: `backend/.../security/V2ApiFeatureFilter.java`
- Delete: `backend/.../config/WechatConfig.java`
- Delete temp files: `config/DomainEventConfig.java.tmp.*`, `interfaces/rest/access/RoleController.java.tmp.*`

Also remove Flowable dependency from `pom.xml` (if present and unused).

---

### Task 2.4: Delete dead REST/application layer files (6 files)

**Files:**
- Delete: `backend/.../application/export/` (2 files)
- Delete: `backend/.../interfaces/rest/export/` (2 files)
- Delete: `backend/.../interfaces/rest/analytics/AnalyticsResponse.java`
- Migrate: `SystemModuleController.java` - replace `ApiResponse` import with `Result<>`, then delete `common/ApiResponse.java`

---

### Task 2.5: Backend compilation verify

```bash
cd backend && JAVA_HOME="/c/Program Files/Java/jdk-17" mvn compile -DskipTests
```

Fix any broken imports. Run tests:
```bash
cd backend && JAVA_HOME="/c/Program Files/Java/jdk-17" mvn test
```

---

## Phase 3: Unify Backend Code Style

### Task 3.1: Unify `getCurrentUserId()` — extract to shared utility

**Problem:** 13 separate copies with 3 different implementations (some throw, some fall back to 1L).

**Solution:** Create one `SecurityUtils.requireCurrentUserId()` method. Delete all 13 private copies.

**Files:**
- Modify: `backend/.../common/util/SecurityUtils.java` — add `requireCurrentUserId()` that throws a clear exception
- Modify: All 13 controllers that have private `getCurrentUserId()` — replace with `SecurityUtils.requireCurrentUserId()`
- **Critical fix:** `SchoolClassController.getCurrentUserId()` currently returns `1L` as fallback — this must be fixed

---

### Task 3.2: Unify controller DI — `@RequiredArgsConstructor` everywhere

**Problem:** 13 controllers use manual constructors, ~20 use `@RequiredArgsConstructor`.

**Solution:** Convert all manual constructors to `@RequiredArgsConstructor` + `private final` fields.

**Files to modify:** All controllers with manual constructors:
- `RoleController`, `PermissionController`, `OrgUnitController`, `SchoolClassController`, `GradeController`, `MajorController`, `StudentController`, `RatingControllerV2`, `ScheduleController`, `SemesterController`, `SemesterDomainController`, `InspectionProjectController`, `InspectionTaskController`

---

### Task 3.3: Unify logger — `@Slf4j` everywhere

**Problem:** Mix of `@Slf4j` and manual `LoggerFactory.getLogger()`.

**Solution:** Replace all manual logger declarations with `@Slf4j`. Delete the manual `private static final Logger` lines.

---

### Task 3.4: Fix `SchoolClassController` DDD layering violation

**Problem:** Controller directly injects infrastructure persistence mappers (`MajorDirectionPersistenceMapper`, `MajorPersistenceMapper`) and does manual pagination with `subList`.

**Solution:** Move logic to application service. Controller should only depend on application services.

---

### Task 3.5: Unify validation — `@Valid` on all `@RequestBody`

**Problem:** Some controllers use `@Valid`, others don't. Validation messages mix English and Chinese.

**Solution:**
- Add `@Valid` to all `@RequestBody` parameters in `OrgUnitController`, `SchoolClassController`
- Standardize all validation messages to Chinese (user-facing system)

---

## Phase 4: Unify Frontend Code Style

### Task 4.1: Fix `rating.ts` — broken import inside JSDoc + duplicate interfaces

**Problem:** Line 2 has `import` inside a `/**` comment block. Duplicate `RatingResultVO` at lines 99 and 680.

**Solution:** Since the entire file is dead (zero imports), delete it (done in Task 1.9). If needed later, rewrite from scratch.

---

### Task 4.2: Clean up `organization.ts` — unify function style

**Problem:** Top section uses `function` declarations, grade section uses arrow function expressions.

**Solution:** Standardize to arrow function expressions (the more common pattern in the codebase).

---

### Task 4.3: Fix `api/index.ts` and `types/index.ts` barrel exports

**Problem:** Both reference non-existent modules and dead exports.

**Solution:** After dead file deletion (Phase 1), clean up remaining barrel exports. Remove all commented-out TODO entries.

---

## Phase 5: Clean Router

### Task 5.1: Remove backward-compatibility redirects

**Problem:** ~70 redirect routes for paths like `/dormitory/*`, `/student-affairs/*`, `/access/*`, `/settings/*`, `/config/*`, `/academic/*`, `/facility/*`, `/quantification/*`, etc.

**Analysis:** These redirects served a purpose during migration but are pure noise now. The old paths were never bookmarked externally (this is an internal management system). All navigations now use the new paths.

**Solution:** Delete all redirect routes (lines 1016-1338 of `router/index.ts`), keeping only the actual page routes. This removes ~320 lines.

---

### Task 5.2: Remove duplicate `/grades` redirect

**Problem:** `/grades` redirect appears twice (line 1146 and 1320).

**Solution:** Handled by Task 5.1 (both get deleted).

---

## Phase 6: Database Schema Cleanup

### Task 6.1: Regenerate `complete_schema_v2.sql` from active state

**Problem:** The canonical schema file is massively out of sync — missing 34 active tables, contains 34 dead tables, has 9 broken index definitions.

**Solution:** Write a new `complete_schema_v3.sql` that reflects only the actually-used tables. Generate from the union of:
1. Tables referenced by `@TableName` annotations in Java code
2. Tables referenced in mapper XML files
3. Tables referenced in raw SQL in `DashboardReadModel.java` (fixing wrong table names)

Drop all dead tables from the schema.

---

### Task 6.2: Fix DashboardReadModel raw SQL

**Problem:** `DashboardReadModel.java` references non-existent tables: `daily_check`, `check_item_appeal`, `user_org_relations`, `task` (singular).

**Solution:** Fix table names or remove broken queries.

---

### Task 6.3: Consolidate migration files

**Problem:** 13+ duplicate version numbers would cause Flyway to abort.

**Solution:** Since this is development phase (not production), consolidate all migrations into a single clean `complete_schema_v3.sql`. Archive old migrations to `database/migrations/archive/`.

---

## Phase 7: Final Verification

### Task 7.1: Full frontend build + type check
```bash
cd frontend && npm run build && npm run type-check
```

### Task 7.2: Full backend compile + test
```bash
cd backend && mvn clean compile -DskipTests && mvn test
```

### Task 7.3: Start both services and smoke test
- Backend starts on :8080
- Frontend starts on :3000
- Login works
- Organization tree loads
- Place management works
- Inspection pages load

### Task 7.4: Git commit

Commit message: `chore: deep project cleanup - remove ~140 dead files, unify code style, fix schema`

---

## Estimated Impact

| Metric | Before | After |
|--------|--------|-------|
| Frontend files | ~383 | ~285 (-98) |
| Backend Java files | ~689 | ~645 (-44) |
| Router lines | ~1420 | ~1000 (-420) |
| Dead Spring beans | ~10 | 0 |
| getCurrentUserId copies | 13 | 1 |
| Schema tables (defined) | 88 | ~55 (active only) |
| Duplicate migrations | 13+ | 0 |
