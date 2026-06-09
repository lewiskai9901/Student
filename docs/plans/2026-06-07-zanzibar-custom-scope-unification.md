# CUSTOM Data-Scope Unification (org-membership model) — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: superpowers:executing-plans / subagent-driven-development.
> This is the row-level security path — a bug = data leak. Per memory `feedback_unit_test_is_not_enough`,
> every backend phase ends with a REAL gate (mvn startup + real INSERT + non-admin query + general-log SQL),
> NOT green unit tests alone.

**Goal:** Make CUSTOM data scope generic and correct: CUSTOM = a set of `org_unit` ids (a class/grade IS an
`org_unit`), applied uniformly per each target table's EXISTING membership model. Remove all education
vocabulary (班级/年级/cohort/class) from the generic-core data-permission UI and interceptor.

**Architecture (revised after P0 model reconciliation — see `2026-06-07-model-findings.md`):**
- Classes and grades are `org_units` (`type_code='CLASS'/'GRADE'`) in ONE tree (`parent_id`/`tree_path`).
- Students are NOT org_units and carry NO `org_unit_id`/`class_id` (dropped in
  `V20260531_4__drop_redundant_membership_columns.sql`); student↔class is ONLY an `access_relations` `member`
  tuple (`subject=user`, `relation='member'`, `resource_type='org_unit'`, `resource_id=class org id`).
- The interceptor already knows each table's membership model: `DddStudentMapper` is
  `@DataPermission(module="student", viaMembership=true, membershipSubjectColumn="user_id")` → membership path;
  almost every other edu table is `orgUnitField="org_unit_id"` → org-field path.
- Therefore CUSTOM is uniformly "a set of org_unit ids (subtree-expanded)". **No discovery-rule SPI, no
  ListObjects closure extension, no typed-item storage migration** — those were over-engineering from the
  pre-P0 assumption that classes were separate entities.

**Two backend gaps to close:**
1. **Case bug**: `DataPermissionPolicyService.loadCustomOrgUnitIds` builds items with `itemTypeCode="org_unit"`
   (lowercase); `MergedDataScope.getOrgUnitIds()` reads `"ORG_UNIT"` → empty → `buildCustomCondition` emits
   `1 = 0` → CUSTOM denies all. (Confirmed; likely never exercised — dev DB has no CUSTOM configs.)
2. **Membership CUSTOM branch doesn't subtree-expand**: `buildMembershipCondition` CUSTOM branch (interceptor
   L518) does raw `ar.resource_id IN (customOrgIds)`. A GRADE grant finds 0 students (they're `member`s of the
   grade's child CLASS orgs, not the grade). Must expand granted org ids to their subtrees.

**Tech Stack:** MyBatis-Plus `DataPermissionInterceptor` / `DataPermissionPolicyService` (JdbcTemplate) /
`access_relations` + `org_units.tree_path` / Vue 3 + TS (`CustomScopeTreePicker.vue`, `useSceneTemplate.ts`).

**Status of related work (done, do NOT redo):** frontend decoupling P1+P2+P3a committed (`974be8f3`/P3a) —
`dataScopeSpecializations`, EDU "我的学生" specialization, generic `useSceneTemplate`, plugin templates,
`inferIndustry`. This plan is the final **P3b** (CUSTOM path).

**UX decision (user-confirmed):** CUSTOM picker = a SINGLE org-unit tree; nodes show a type label/icon driven
by `org_unit.type_code` + a backend type dictionary (NOT hardcoded "班级"/"年级" in core).

---

## Phase 0 — DONE (gate passed)
- `docs/plans/2026-06-07-model-findings.md` written. Conclusions above. ✅

Remaining P0 task before P1: **seed fixtures** (needed to verify P1/P3).

### Task 0.2: Seed test fixtures
**Files:** Create `database/seeds/dev_custom_scope_fixtures.sql`
**Step 1:** Idempotent INSERTs under tenant 1 using sentinel ids ≥ `9000000000000000000`:
- org_units: 1 SCHOOL/dept root, 2 GRADE org_units (type_code='GRADE') as children, 3 CLASS org_units
  (type_code='CLASS') under the grades, with correct `parent_id` + `tree_path`.
- users + `user_student` rows (~6), each linked to a class via an `access_relations` `member` tuple
  (`subject_type='user'`, `subject_id=user_id`, `relation='member'`, `resource_type='org_unit'`,
  `resource_id=class org id`, `tenant_id=1`, `deleted=0`).
- (Mirror the exact column set proven in `2026-06-07-model-findings.md` "Test-data requirements".)
**Step 2:** Apply `mysql -u root -p123456 student_management < database/seeds/dev_custom_scope_fixtures.sql`;
re-run = no-op.
**Step 3:** Verify: each class org has its expected member students; grade orgs have classes as children.
**Step 4:** Commit.

---

## Phase 1 — Backend: fix case bug + subtree-expand membership CUSTOM (verifiable alone)

### Task 1.1: Characterization test (red) — case bug denies all
**Files:** Create `backend/.../test/.../infrastructure/access/DataPermissionPolicyServiceCustomScopeTest.java`
**Step 1:** Failing test: after `saveRolePermission(role, CUSTOM, [ORG_UNIT:orgX])`,
`getMergedScope(...).getOrgUnitIds()` contains orgX. Today → empty (case bug) → FAIL.
**Step 2:** `cd backend && mvn test -Dtest=DataPermissionPolicyServiceCustomScopeTest` → FAIL. Commit (red).

### Task 1.2: Fix the case mismatch
**Files:** Modify `backend/.../infrastructure/access/DataPermissionPolicyService.java` (`loadCustomOrgUnitIds`
L218–244: emit `"ORG_UNIT"` upper); belt-and-suspenders make
`DataScopeItem.isOrgUnitType()`/`isType()` use `equalsIgnoreCase`.
**Step 2:** Re-run 1.1 → PASS. **Step 3:** Commit.

### Task 1.3: Subtree-expand granted org ids in the membership CUSTOM branch
**Files:**
- Modify `DataPermissionInterceptor.buildMembershipCondition` (L518 CUSTOM branch): replace raw
  `ar.resource_id IN (csv)` with a subtree expansion:
  `ar.resource_id IN (SELECT id FROM org_units WHERE tenant_id=? AND deleted=0 AND (tree_path LIKE ? OR id=?) ...)`
  for each granted org id — i.e. each granted org PLUS its descendants. (Parameterized; reuse the tree_path
  pattern already used elsewhere in this file.)
- Test: `backend/.../test/.../infrastructure/access/DataPermissionInterceptorCustomTest.java`
**Step 1:** Failing unit test: membership CUSTOM with granted GRADE org `g1` (whose child class is `c1`)
produces SQL whose `ar.resource_id IN (...)` includes the subtree (so members of `c1` match). FAIL.
**Step 2:** Implement. **Step 3:** Run → PASS. **Step 4:** `mvn test` access package green. Commit.

### Task 1.4: REAL gate
**Steps (evidence, no new code):**
1. Apply 0.2 fixtures. Restart backend (kill PID + `mvn spring-boot:run -DskipTests`), confirm "Started".
2. `PUT /api/roles/{roleId}/data-permissions` for a test role: `student` module = CUSTOM with one seeded CLASS
   org id; also a second role with a GRADE org id.
3. Log in as a user holding that role; `GET` the real student-list endpoint.
4. Enable MySQL general log; confirm the emitted SQL is the member-subquery with the subtree IN (NOT `1 = 0`),
   and the returned students == the class's seeded students (CLASS role) / all child-class students (GRADE role).
5. Record evidence in commit message. **STOP if rows are wrong** — data-leak surface.

**GATE 1:** CUSTOM org/class/grade grants filter students correctly via membership. Commit.

---

## Phase 2 — Frontend: CUSTOM picker → single generic org-unit tree

### Task 2.1: Make CustomScopeTreePicker a single org tree (data-driven type labels)
**Files:**
- Modify `frontend/src/views/access/data-permissions/components/CustomScopeTreePicker.vue`:
  - props/emit: just `orgIds` (drop `gradeIds`/`classIds`).
  - Build from `getOrgUnitTree()` ONLY. Remove `getAllCohorts`/`getAllClasses` imports + the grade/class
    sections + `GraduationCap`/`Users` grade/class markup.
  - Each node shows a small type label from `node.typeCode`/`unitType` via a type-label lookup. Fetch the org
    type dictionary (existing `/org-units/by-type` family or an org-types endpoint — implementer to locate) so
    "班级"/"年级" come from data, not literals. Generic fallback = the raw type code.
  - Keep includeChildren semantics implicitly = always subtree on save (see 2.2).
- Modify `composables/useSceneTemplate.ts`:
  - `SceneDecision`: drop `customGradeIds`/`customClassIds`; keep `customOrgIds`.
  - `sceneToModuleScopes` (L104–115): emit ScopeItems only for ORG_UNIT, `includeChildren: true`. Remove the
    GRADE/CLASS literal blocks.
  - `moduleScopesToScene` (L188–204): collect all `scopeItems` (any itemTypeCode) into `customOrgIds` (they're
    all org ids now). Remove the GRADE/CLASS branches.
- Modify consumers referencing the dropped fields: `PermissionConfigurator.vue`, `PreviewPanel.vue`,
  `DataPermissionsLayout.vue` — search `customGradeIds|customClassIds` and reduce to `customOrgIds`.
**Step 1:** Implement. **Step 2:** `cd frontend && npm run type-check` → 0; `npm run lint` → 0 errors. Commit.

### Task 2.2: Browser verification
**Steps:** data-permissions page → pick role → primary=CUSTOM → picker is one org tree containing dept/grade/
class nodes with type labels; select a class node; Save; reload → persists (round-trips via `customOrgIds` →
ScopeItems ORG_UNIT). Toggle EDU off → tree still works (orgs are core); no class/grade-specific code path.
Record screens. **GATE 2.**

---

## Phase 3 — End-to-end correctness (the decisive gate)
(Backend already gated in 1.4; this re-runs the full stack through the UI.)
**Steps:**
1. As admin, via UI assign a non-admin role CUSTOM `student` scope = one class node; save.
2. Log in as a user with that role; open the students list page; assert exactly that class's students show.
3. Change to a grade node; assert all that grade's classes' students show.
4. Change to a dept/root node; assert subtree students show.
5. Org-field table check: assign CUSTOM on `student_grade` (has `org_unit_id`) = a class node; assert the
   org-field path filters correctly (regression that org-field tables still work).
6. General log spot-check for one of each. Record evidence. **GATE 3 — stop if any row set wrong.**

---

## Phase 4 — Guards, cleanup, docs

### Task 4.1: Guards
- Frontend: vitest/grep guard — no `getAllClasses|getAllCohorts|班级|年级|gradeIds|classIds` in
  `frontend/src/views/access/data-permissions/**` (core stays generic).
- Backend: test asserting `DataPermissionInterceptor` source has no `class`/`grade` literals.

### Task 4.2: Cleanup
- Remove the now-dead `customGradeIds/customClassIds` types anywhere lingering.
- Note (do NOT fix here unless trivial): `GradeDataScopeResolver.queryStudentIdsByGrades` references a
  non-existent `user_student.grade_id` (stale BY_GRADE plugin path) — file a follow-up; out of P3b scope.

### Task 4.3: Docs + memory
- Update `project_data_permission_decoupling.md`: P3b DONE — CUSTOM unified on org-membership (class/grade are
  org_units; case bug fixed; membership subtree-expand). Remove the "遗留 P3b" line.
- Short ADR `docs/adr/`: "CUSTOM data scope = org_unit set, applied per table membership model".

**GATE 4:** type-check 0 / lint 0 / `mvn test` green / guards green / full browser smoke + one non-admin
custom-scope student-list filter.

---

## Risk register
- **Data leak (row filter)**: gated by real non-admin queries in 1.4 and Phase 3, not unit-green.
- **Subtree IN size**: a root grant expands to a large subtree — but it's a single `SELECT id FROM org_units
  WHERE tree_path LIKE ?` subquery (index-backed), not inlined ids. Fine.
- **Org tree may not surface CLASS/GRADE nodes**: verify in 2.1 that `getOrgUnitTree()` returns them (they're
  org_units in the parent_id hierarchy, so it should). If the tree is type-filtered, lift the filter for this
  picker.
- **No new abstractions**: if a FUTURE plugin table relates to students by something that's neither org-field
  nor user-membership, IT declares its own `@DataPermission` model — still no core change.
