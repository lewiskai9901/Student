# Access Control Matrix (Cheat Sheet)

Quick-reference for "which tool do I use?" Last updated: 2026-04-14.
Full explanations: `access-control-guide.md`.

## Decision table

| I want to... | Use | Annotation / API |
|--------------|-----|------------------|
| Gate an HTTP endpoint by role | Casbin | `@CasbinAccess(resource, action)` on controller method |
| Filter list query to user's org unit | V5 Data Permission | `@DataPermission(module=...)` on mapper method |
| Grant one user access to one specific resource | AccessRelations | `POST /api/access-relations` |
| Grant a whole org unit access to resources | AccessRelations | `subjectType=org_unit`, optionally `includeChildren=true` |
| Check in-code "can user X do Y?" | AuthorizationService | `authSvc.hasPermission(userId, "student:view")` |
| Run system job without data scoping | Escape hatch | `UserContextHolder.executeWithoutDataPermission(() -> ...)` |

## Annotation quick reference

### `@CasbinAccess`
```java
@CasbinAccess(resource = "student", action = "view")
```
| Field | Required | Typical values |
|-------|----------|----------------|
| `resource` | yes | `student`, `system:role`, `inspection_template`, … |
| `action` | yes | `view`, `add`, `edit`, `delete` |

Super-admin automatically bypasses.

### `@DataPermission`
```java
@DataPermission(
    module = "student",
    tableAlias = "s",
    orgUnitField = "org_unit_id",
    creatorField = "created_by",
    resourceType = "student"
)
```
| Field | Default | Notes |
|-------|---------|-------|
| `module` | **required** | matches `data_module.code` seeded in DB |
| `tableAlias` | `""` | use when SQL has a table alias |
| `orgUnitField` | `"org_unit_id"` | set to `""` if no org unit column |
| `creatorField` | `"created_by"` | used for `SELF` scope |
| `resourceType` | `""` | set to enable ReBAC OR-join on `access_relations` |
| `enabled` | `true` | flip to `false` for a one-off bypass (rare) |

## Scope values (V5)

Returned by `DataPermissionPolicyService.getMergedScope()` and stored in `role_data_permissions_v5.data_scope`:

| Code | Meaning | SQL rewrite |
|------|---------|-------------|
| `ALL` | Everything | no filter |
| `DEPARTMENT_AND_BELOW` | User's org unit + descendants | `org_unit_id IN (user_org_path)` |
| `DEPARTMENT` | User's own org unit only | `org_unit_id = user.orgUnitId` |
| `CUSTOM` | Explicit org unit list | `org_unit_id IN (data_scopes_v5 list)` |
| `SELF` | Only rows the user created | `created_by = user.id` |

Multiple roles: merged to the **broadest** scope.

## AccessRelation fields

| Field | Values |
|-------|--------|
| `subjectType` | `user`, `org_unit` |
| `subjectId` | user id or org unit id |
| `relation` | `owner`, `manager`, `user`, `member`, `viewer`, `responsible`, `occupant` |
| `resourceType` | `place`, `org_unit`, `student`, `class`, `inspection_template`, … (must match `@DataPermission.resourceType`) |
| `resourceId` | id of the specific resource |
| `accessLevel` | `1`=read, `2`=read-write |
| `includeChildren` | `true` for org-unit subjects means grant cascades to descendants |

## Key tables

| Table | Layer | What it stores |
|-------|-------|----------------|
| `roles` | Casbin | Role definition (code, name, type, level) |
| `role_permissions` | Casbin | Role ↔ permission link |
| `permissions` | Casbin | Permission definition (menu / button / API) |
| `user_roles` | Casbin | User ↔ role, scoped (ALL or by org unit) |
| `role_data_permissions_v5` | V5 | Role × module → scope |
| `data_scopes_v5` | V5 | CUSTOM scope: role × module × org unit list |
| `access_relations` | ReBAC | Subject × relation × resource grants |

## Key classes

| Concern | File |
|---------|------|
| Casbin annotation | `infrastructure/casbin/CasbinAccess.java` |
| Casbin aspect | `infrastructure/casbin/CasbinAccessInterceptor.java` |
| Casbin enforcer service | `infrastructure/access/CasbinAuthorizationService.java` |
| V5 annotation | `infrastructure/access/DataPermission.java` |
| V5 MyBatis interceptor | `infrastructure/access/DataPermissionInterceptor.java` |
| V5 policy service | `infrastructure/access/DataPermissionPolicyService.java` |
| ReBAC domain model | `domain/access/model/entity/AccessRelation.java` |
| ReBAC repository | `domain/access/repository/AccessRelationRepository.java` |
| ReBAC controller | `interfaces/rest/access/AccessRelationController.java` |
| UserContext | `infrastructure/access/UserContext.java` + `UserContextHolder.java` |
| Context population | `security/JwtAuthenticationFilter.java` |

## Don'ts

- ❌ `@PreAuthorize` — replaced by `@CasbinAccess`
- ❌ New `user_place` / `user_org` / `user_resource` tables — use `access_relations`
- ❌ Storing `data_scope` on `roles` table — use `role_data_permissions_v5`
- ❌ Calling `AuthorizationService.getDataScope()` — removed
- ❌ Reading `UserContextHolder.getContext()` in an async thread — ThreadLocal does not propagate
