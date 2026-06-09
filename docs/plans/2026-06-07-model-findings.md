# Model findings: students ↔ classes ↔ grades ↔ org_units

Investigation date: 2026-06-09. READ-ONLY. Purpose: decide how to filter `user_student`
when a role's CUSTOM data scope grants a specific class or grade, on the Zanzibar
`access_relations` substrate.

TL;DR — **Classes and grades are NOT separate entity tables. They ARE rows in `org_units`**
(`type_code = 'CLASS'` / `'GRADE'`). The `classes` "table" is a **VIEW** over `org_units`.
**`user_student` has NO `class_id` / `grade_id` / `org_unit_id` column** — those were
deliberately dropped. A student reaches its class **only** via an `access_relations` tuple
`{resource_type='org_unit', resource_id=<class org_unit id>, relation='member', subject_type='user', subject_id=student.user_id}`.

---

## Table map (the linking columns, with citations)

| Table | Kind | Link columns | Citation |
|---|---|---|---|
| `user_student` | real table | **NO class/grade/org_unit FK.** Has `id`, `student_no`, `user_id`, `dormitory_id`, `status`… | `database/schema/baseline_v3.sql:12975-13011` |
| `org_units` | real table | `id`, `unit_type`, `type_code` (=unit_type denorm), `parent_id`, `tree_path`, `attributes` (JSON) | `database/schema/baseline_v3.sql:8900-8938` |
| `classes` | **VIEW** over `org_units WHERE type_code='CLASS'` | `id`=org_unit.id, `org_unit_id`=`parent_id`, `grade_id`=`attributes.gradeId`, `teacher_id`=`attributes.headTeacher` | `database/schema/V113.0.0__restore_dropped_tables_and_classes_view.sql:108-140` |
| `grades` | real table | `id`, `grade_code`, `enrollment_year`, `leader_id`/`grade_director_id` — standalone master table, **not** referenced by any `user_student` FK | `database/schema/baseline_v3.sql:5914-5943` |
| `grade_directors` | real table | `org_unit_id` (the GRADE org_unit), `director_id`, `deputy_director_ids` (JSON), `counselor_ids` (JSON) — used by BY_GRADE resolver | referenced in `GradeDataScopeResolver.java:59-65` |
| `student_grades` | real table | course-score rows; `student_id` (→user_student.id), `org_unit_id` (=class), `course_id`, `task_id`. NOTE: this is *exam/course scores*, NOT "grade level / 年级". | `database/schema/baseline_v3.sql:11820-11861` |
| `access_relations` | real table | `resource_type`, `resource_id`, `relation`, `subject_type`, `subject_id`, `valid_to`, `metadata`; unique membership lock on `(member,user,org_unit)` | `database/schema/baseline_v3.sql:247-279` |

Key deletion event — membership FKs were intentionally removed:
`database/migrations/V20260531_4__drop_redundant_membership_columns.sql:38-64` drops
`user_student.org_unit_id` (and its index, historically named `idx_class_id`). The migration
header states: *"组织归属…已统一表达为 access_relations 的 {member|user|org_unit} 关系…
这三个旧的直连归属列彻底退役"* (membership unified to access_relations; the old direct
columns are retired). `users.primary_org_unit_id` and `user_teacher.org_unit_id` dropped too.

---

## Are class/grade org_units? (definitive yes/no + evidence)

**YES — definitively.** Both classes and grades are `org_units` rows distinguished by `type_code`.

- **Class = `org_units` row with `type_code='CLASS'`.** The `classes` object is literally a
  `CREATE OR REPLACE VIEW classes AS SELECT … FROM org_units o WHERE o.type_code = 'CLASS'`
  (`V113.0.0…:108,140`). The view exposes `grade_id`/`major_id`/`teacher_id` by extracting
  JSON from `org_units.attributes` (`$.gradeId`, `$.majorId`, `$.headTeacher`), and the class's
  parent department as `org_unit_id = o.parent_id`.
- **Grade = `org_units` row with `type_code='GRADE'`.** `GradeDataScopeResolver.java:23-24`
  doc: *"年级 = org_unit 类型 = GRADE"*; `findUserGradeOrgUnitIds` selects `grade_directors.org_unit_id`
  (the GRADE org_unit id). The `ClassDataScopeResolver.java:18-19` likewise treats the resource
  as *"org_unit 是 CLASS 类型"*.
- **Seed proof:** `database/init/e2e_seed_v3.sql:35-41` inserts a class as
  `org_units(unit_type='CLASS', type_code='CLASS', attributes='{"headTeacher":…, "enrollmentYear":2024}')`.

The standalone `grades` table (`baseline_v3.sql:5914`) is a *master/registry* of grade metadata
(enrollment year, schooling years, director ids). It is **not** the access substrate — the
GRADE *org_unit* is. There is no `cohorts` table in baseline_v3.

---

## Student → class → grade reachability

**Answer: (c) member relation in `access_relations`.** Neither (a) an org field on `user_student`
(it has none) nor (b) an FK column (dropped). The ONE proven path — taken verbatim from the
production `ClassDataScopeResolver.queryStudentIdsByClasses` (`ClassDataScopeResolver.java:88-95`):

```sql
SELECT s.id
FROM user_student s
JOIN access_relations mar
  ON mar.subject_id   = s.user_id        -- student's USER id, not student.id
 AND mar.relation     = 'member'
 AND mar.resource_type= 'org_unit'
 AND mar.subject_type = 'user'
 AND mar.deleted      = 0
 AND (mar.valid_to IS NULL OR mar.valid_to > NOW())
WHERE mar.resource_id IN (:classOrgUnitIds)   -- class = org_unit ids
  AND s.deleted = 0
```

Join key nuance: the tuple links `access_relations.subject_id → user_student.user_id`
(the student's *user account* id), and `resource_id` is the **class org_unit id**.

Class → grade: the class org_unit carries its grade in `org_units.attributes.$.gradeId`
(surfaced as `classes.grade_id`), and/or the grade is the class's ancestor in the org tree
(`tree_path` / `parent_id`). The BY_GRADE resolver currently goes grade-org-unit →
`grade_directors` (who manages it) rather than walking the tree.

> ⚠️ Latent inconsistency to be aware of for the rewrite: `GradeDataScopeResolver.queryStudentIdsByGrades`
> (`GradeDataScopeResolver.java:72-78`) does `SELECT id FROM user_student WHERE grade_id IN (...)` —
> but `user_student` has **no `grade_id` column** (never in baseline_v3, and `org_unit_id` was dropped).
> That BY_GRADE→student SQL is stale/broken and would throw "Unknown column 'grade_id'". The CLASS
> path is the only correct, currently-working one. Any grade→student filter must be re-expressed via
> the class layer (grade org_unit → child CLASS org_units → member students) or via `access_relations`.

---

## Decision for the filter rewrite

For target table `user_student`, to filter *"students whose class ∈ {granted class ids}"*, the
most correct + efficient predicate is an **EXISTS / IN sub-select against `access_relations`
member tuples keyed on the class org_unit ids** — identical in shape to the proven
`ClassDataScopeResolver` join:

```sql
-- injected into the user_student query (id = user_student PK)
user_student.id IN (
  SELECT s.id
  FROM user_student s
  JOIN access_relations ar
    ON  ar.subject_id    = s.user_id
    AND ar.subject_type  = 'user'
    AND ar.relation      = 'member'
    AND ar.resource_type = 'org_unit'
    AND ar.deleted       = 0
    AND (ar.valid_to IS NULL OR ar.valid_to > NOW())
  WHERE ar.resource_id IN ( :grantedClassOrgUnitIds )
)
-- equivalently, as a correlated EXISTS on the outer user_student alias:
-- EXISTS (SELECT 1 FROM access_relations ar
--         WHERE ar.subject_id = user_student.user_id AND ar.subject_type='user'
--           AND ar.relation='member' AND ar.resource_type='org_unit'
--           AND ar.deleted=0 AND (ar.valid_to IS NULL OR ar.valid_to>NOW())
--           AND ar.resource_id IN (:grantedClassOrgUnitIds))
```

Index support exists: `access_relations.idx_lookup (resource_type,relation,subject_type,subject_id,deleted)`
and `idx_resource (resource_type,resource_id,deleted)` (`baseline_v3.sql:272-274`).

**Is a `RelationDiscoveryRule` (class→student) needed?** Yes — keep it. Granting a class does
**NOT** cleanly collapse to an ORG_UNIT tree-path grant, because:
- Students are **not** org_units and carry **no org_unit_id** (dropped). So a tree-path predicate
  on `org_units.tree_path LIKE '<class path>%'` returns org_units, never student rows. There is no
  `user_student.org_unit_id` to compare a tree path against.
- The student↔class edge lives exclusively as a `member` tuple in `access_relations`. Therefore a
  class→student discovery rule (subject=class org_unit, follow reverse `member` to user, map
  `user_id`→`user_student.id`) is exactly the indirection required. A plain ORG_UNIT-scope grant
  only works for tables that themselves carry `org_unit_id` (e.g. `student_grades.org_unit_id`,
  inspection tables), not for `user_student`.

For **grade** grants: resolve granted GRADE org_unit → its child CLASS org_units
(`org_units WHERE type_code='CLASS' AND parent/tree under the grade`, or `classes.grade_id = grade`),
then apply the same class→member→student rule. Do **not** rely on `user_student.grade_id` (does not exist).

---

## Test-data requirements

A seed fixture that makes *"a class with N students"* queryable must populate:

1. **`org_units`** — one CLASS row:
   `(id=<CLASS_ID>, unit_type='CLASS', type_code='CLASS', parent_id=<dept org_unit>, tree_path=…,
   attributes='{"gradeId":<GRADE_ID>, "headTeacher":"<teacherUserId>"}', status='ACTIVE', deleted=0)`.
   (Optionally a `type_code='GRADE'` org_unit as the class's ancestor for grade tests.)
   Pattern: `e2e_seed_v3.sql:35-41`.

2. **`users`** — N student user accounts (and the teacher account) with `deleted=0`.
   Pattern: `e2e_seed_v3.sql:21-26`.

3. **`user_student`** — N student rows, each with `user_id` = the corresponding `users.id`
   (the join key!), `deleted=0`. No class/grade column to set.
   Schema: `baseline_v3.sql:12975-13011`.

4. **`access_relations`** — **N membership tuples, one per student** (this is what makes the
   class non-empty):
   `(resource_type='org_unit', resource_id=<CLASS_ID>, relation='member', subject_type='user',
   subject_id=<student user_id>, deleted=0, valid_to=NULL)`.
   Respect `uk_membership_unique` (one active `member|user|org_unit` per user) and `uk_relation`.
   Pattern: `e2e_seed_v3.sql:49-64` (note: the existing seed binds *teachers* to *departments*,
   not students to the class — a class-with-students fixture must add the per-student `member`
   rows to the CLASS org_unit, which the current e2e seed does NOT do).

5. **(For teacher-driven scope tests)** `access_relations` teacher tuple:
   `(resource_type='org_unit', resource_id=<CLASS_ID>, relation='CLASS_TEACHER'|'SUBJECT_TEACHER',
   subject_type='user', subject_id=<teacherUserId>)`, **or** legacy `classes.teacher_id`
   (= `org_units.attributes.headTeacher`). Pattern/relations: `ClassDataScopeResolver.java:56-70`,
   `EducationManifest.java:65-69` (TEACHES relation), data-module bindings
   `V20260504_2…` / `EducationDataResourceProvider.java:33-34`.

Minimal "class with N students" set = **rows in `org_units` (1) + `users` (N) + `user_student` (N) +
`access_relations` (N member tuples)**. The `grades`, `grade_directors`, `student_grades`,
`teacher_assignments` tables are only needed for grade-scope / score-scope / teacher-assignment tests.

---

## Appendix — resource_type / relation string inventory

- **resource_type strings** (`access_relations.resource_type` / `data_resources.resource_code`):
  a class is addressed as `resource_type='org_unit'` (NOT a dedicated `class`/`school_class`
  resource type at the relation layer). The data-scope *resource codes* are `student`,
  `school_class`, `attendance`, `student_grade`, `grade_batch`, `exam`, `teaching_task`,
  `dormitory`, `enrollment` (`EducationDataResourceProvider.java:21-39`). `school_class` is a
  scope/resource code, but its membership tuples still live under `resource_type='org_unit'`.
  There is **no separate `grade` resource_type** — grade scope is expressed as the `BY_GRADE`
  *DataScope* on `student`/`school_class`, resolved against GRADE org_units.
- **relation codes** seen for classes: `member` (student↔class), `CLASS_TEACHER`,
  `SUBJECT_TEACHER`, `admin` (class admin on a CLASS org_unit), plus legacy `classes.teacher_id`.
  Citations: `ClassDataScopeResolver.java:56-95`, `access_relations` comment `baseline_v3.sql:251`,
  seed `baseline_v3.sql:288` (member tuples), `EducationManifest.java:104-113`.
