# Access Control Guide

How to correctly use the three-layer access control stack. Last updated: 2026-04-14.

## The three layers (pipeline, not alternatives)

```
HTTP request
    │
    ▼
┌─────────────────────┐
│ 1. Casbin (RBAC)    │  "Can this USER call this ENDPOINT?"
│    @CasbinAccess    │  Rejects → 403
└─────────────────────┘
    │ allow
    ▼
┌─────────────────────┐
│ 2. V5 Data Scope    │  "Which ORG UNITS can this user see?"
│    @DataPermission  │  Rewrites SQL WHERE clause
└─────────────────────┘
    │ filtered rows
    ▼
┌─────────────────────┐
│ 3. AccessRelations  │  "Which SPECIFIC RESOURCES was this user granted?"
│    (ReBAC)          │  Adds OR subquery for direct grants
└─────────────────────┘
    │
    ▼
Result set
```

They are **layered filters**, not interchangeable. Remove any one and you get a hole.

---

## Layer 1 — Casbin (API gate)

**Use when**: you need "does this user's role allow this action on this resource type?" — coarse-grained.

```java
@GetMapping("/students")
@CasbinAccess(resource = "student", action = "view")
public Result<List<Student>> list() { ... }
```

- Annotation: `infrastructure/casbin/CasbinAccess.java`
- Enforcement: `CasbinAccessInterceptor` (Spring @Around aspect)
- Data source: `roles` + `role_permissions` + `permissions` tables
- Super-admin bypass: automatic
- Do **not** use `@PreAuthorize` — removed project-wide

**Actions**: `view`, `add`, `edit`, `delete` (convention, not enforced). Resource names are flat strings (`student`, `system:role`, etc.), match what's seeded into Casbin policy.

---

## Layer 2 — V5 Data Permission (row filter)

**Use when**: list queries must only return rows the user is allowed to see, scoped by **org unit** or **creator**.

```java
@DataPermission(
    module = "student",
    tableAlias = "s",
    orgUnitField = "org_unit_id",
    creatorField = "created_by",
    resourceType = "student"  // optional: enables Layer 3 OR-join
)
@Select("SELECT s.* FROM students s WHERE s.deleted = 0")
List<StudentPO> selectAll();
```

- Annotation: `infrastructure/access/DataPermission.java`
- Enforcement: `DataPermissionInterceptor` (MyBatis `StatementHandler.prepare` interceptor) rewrites the SQL using JSqlParser
- Policy source: `role_data_permissions_v5` (per-role-per-module scope), `data_scopes_v5` (custom org unit lists)
- Skipped when: super-admin, or inside `UserContextHolder.executeWithoutDataPermission(() -> ...)`

**Scope values** resolved by `DataPermissionPolicyService.getMergedScope()`:
`ALL` · `DEPARTMENT_AND_BELOW` · `DEPARTMENT` · `CUSTOM` (uses `data_scopes_v5`) · `SELF`

**Rule of thumb**: every user-facing list query that touches `org_unit_id` or `created_by` should be annotated. If the table has no org unit column, set `orgUnitField = ""` and rely on `creatorField` / ReBAC only.

---

## Layer 3 — AccessRelations (ReBAC, direct grants)

**Use when**: a specific user (or org unit) has been explicitly given access to a specific resource instance — orthogonal to their role's org scope.

**Model** (`domain/access/model/entity/AccessRelation.java`):
- `subjectType` ∈ {`user`, `org_unit`}
- `subjectId` — who
- `relation` ∈ {`owner`, `manager`, `user`, `member`, `viewer`, `responsible`, `occupant`}
- `resourceType` — matches `@DataPermission.resourceType`
- `resourceId` — what
- `accessLevel` — 1 = read, 2 = read-write
- `includeChildren` — for org-unit subjects, grants cascade to descendant org units

**Wiring**: set `resourceType` on the `@DataPermission` annotation. Layer 2's interceptor then adds `OR resource_id IN (SELECT ... FROM access_relations ...)` to the SQL.

**API**: `/api/access-relations` (GET/POST/PUT/DELETE). Frontend manager: `AccessRelationManager.vue`.

**Do NOT** create parallel user-place / user-org / user-resource tables — they are the zombies we replaced.

---

## UserContext — the glue

`UserContextHolder` (ThreadLocal) is populated once per request by `JwtAuthenticationFilter`:

```java
UserContext ctx = UserContextHolder.getContext();
ctx.getUserId();
ctx.getOrgUnitId();
ctx.getRoleIds();       // for V5 policy lookup
ctx.getScopedRoles();   // V6: role + scope org unit
ctx.isSuperAdmin();     // bypass both Layer 2 & 3
```

Cleared in the same filter's `finally` block. If a background thread needs access control, it must `setContext()` explicitly — async dispatch does **not** propagate ThreadLocal.

**Escape hatch**: `UserContextHolder.executeWithoutDataPermission(() -> ...)` disables Layer 2 inside the lambda. Reserved for system jobs and cross-tenant reports. Never use it to paper over missing scope config.

---

## Common mistakes

| Symptom | Probable cause | Fix |
|---------|---------------|-----|
| User sees everyone's data | Missing `@DataPermission` on mapper | Add annotation + correct `module` code |
| User sees nothing after login | ThreadLocal not populated (async?) | Call `UserContextHolder.setContext()` or route through the filter |
| `@CasbinAccess` ignored | Aspect not triggered (called internally, not via HTTP) | Move check to caller, or call `AuthorizationService.hasPermission()` directly |
| ReBAC grants don't take effect | Missing `resourceType` on `@DataPermission` | Add it — matches `access_relations.resource_type` |
| Super-admin also filtered | Wrong — super-admin bypasses both Layer 2 & 3 | Check `isSuperAdmin()` flag is set in context |

## What was removed (2026-04-14)

- `roles.data_scope` column and `Role.dataScope` field — zombie. Data scope lives in `role_data_permissions_v5` keyed by module. See `docs/plans/phase0-legacy-cleanup-report.md`.
- `AuthorizationService.getDataScope() / getAccessibleOrgUnitIds() / canAccessOrgUnit()` — dead methods with no callers. V5 is the source of truth.
