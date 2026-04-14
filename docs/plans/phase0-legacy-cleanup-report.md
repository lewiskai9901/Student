# Phase 0 Legacy Cleanup Report

Date: 2026-04-14
Scope: Audit of zombie/legacy fields blocking the access-control refactor (see `2026-04-14-access-control-refactor-design.md`).

## Phase 0.1 — `roles.data_scope` (Completed)

**Classification**: Zombie field. DB column existed, domain model + API carried the field, but `RolePO` never mapped it. `RoleRepositoryImpl.toDomainWithPermissions()` hardcoded `DataScope.ALL`. Real data permissioning lives in the V5 system (`role_data_permissions_v5` + `DataPermissionPolicyService`).

**Actions taken**:
- Migration: `database/migrations/V20260414_1__remove_role_data_scope.sql`
- Schema source of truth: removed column from `database/schema/complete_schema_v2.sql`
- Domain: removed field, constructor arg, setter, getter, builder method from `Role.java`
- Application: removed `dataScope` from `CreateRoleCommand`, `UpdateRoleCommand`, dropped `setDataScope` calls in `AccessApplicationService`
- Interfaces: removed field from `CreateRoleRequest`, `UpdateRoleRequest`, `RoleResponse`; controllers no longer pipe it
- Infrastructure: removed hardcoded `.dataScope(DataScope.ALL)` stub in `RoleRepositoryImpl`; deleted `getDataScope / getAccessibleOrgUnitIds / canAccessOrgUnit` from `AuthorizationService` interface and `CasbinAuthorizationService` impl (no external callers)
- Frontend: removed from `types/access.ts`, `api/access.ts`
- Tests: removed 7 assertions from `RoleTest.java`, deleted `DataScopeTest` nested class

**Verification**: `mvn compile` + `mvn test-compile` both BUILD SUCCESS.

**Preserved**: The separate `role_data_permissions_v5.data_scope` column + `RoleDataPermissionPO.dataScope` field (live V5 system). Keyword `DataScope` enum is still referenced by `MergedDataScope`, `DataScopeItem`, `DataPermissionPolicyService`, etc. — intentional.

## Phase 0.2 — Other Suspect Patterns (Scan Only)

Findings catalogued but **not** touched this phase. Each is a candidate for later cleanup once the access-control refactor stabilizes.

| # | Pattern | Location | Risk | Recommendation |
|---|---------|----------|------|----------------|
| 1 | `Role.createdBy` not persisted | `Role.java:25` + `RolePO` missing column; `RoleRepositoryImpl:201` loads `null` | Medium — audit trail broken | Add `created_by` column + mapping, or drop field from domain |
| 2 | `Role.isSystem` not persisted | `Role.java:21`; inferred via `roleType` in `RoleRepositoryImpl:190` | Medium — behavior tied to magic string constants | Persist explicitly OR compute in domain consistently |
| 3 | `OrgUnitTypePO.icon / description / maxDepth / defaultUserTypeCodes / defaultPlaceTypeCodes` marked `@TableField(exist=false)` | `OrgUnitTypePO.java:33-60` | Medium — indicates abandoned config schema | Check ConfigurableType persistence path; either restore columns or delete fields |
| 4 | `UniversalPlaceBooking.attendeeIds` (List<Long>) vs `UniversalPlaceBookingPO.attendees` (String JSON) | diverging types, no converter | High — booking attendees may silently lose data | Add explicit JSON-List TypeHandler or align fields |
| 5 | `UniversalPlaceBookingPO.placeName / placeTypeCode` marked `@TableField(exist=false)` but no JOIN fetch logic | `UniversalPlaceBookingPO.java:96-103` | Low | Remove if unused, or wire JOIN |
| 6 | Column `role_desc` ↔ field `description` naming mismatch | `RolePO.java:37-38` vs `Role.java:18` | Low | Align naming in next schema pass |
| 7 | `CorrectiveCase` remark fields (preventiveAction, correctionNote, verificationNote, effectivenessNote) | `CorrectiveCasePO.java:29-49` | Low | Audit complete mapping — some may be missing columns |

## Phase 0.3 / 0.4

Documentation deliverables still pending:
- `docs/access-control-guide.md` — contributor-facing usage rules for Casbin / V5 / AccessRelations
- `docs/access-control-matrix.md` — quick-reference cheatsheet

## Conclusion

Zombie `roles.data_scope` fully eliminated end-to-end. The remaining 7 patterns are tracked but deferred — none blocks Phase 1 (`permission_scope`) or Phase 2 (UserType activation).
