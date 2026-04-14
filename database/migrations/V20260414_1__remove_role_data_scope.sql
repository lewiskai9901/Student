-- Phase 0.1: Remove legacy roles.data_scope column
--
-- Reason: The `roles.data_scope` column is a zombie field:
--   - DB column exists
--   - Role aggregate and API still carry the field
--   - But RolePO does NOT map this column (persistence is broken)
--   - RoleRepositoryImpl.toDomainWithPermissions() hardcodes DataScope.ALL
--   - CasbinAuthorizationService.getDataScope() always returns ALL
--   - Actual data permission is fully handled by V5 system
--     (role_data_permissions_v5 + DataPermissionPolicyService)
--
-- See docs/plans/2026-04-14-access-control-refactor-design.md Phase 0.1
-- See docs/plans/phase0-legacy-cleanup-report.md for audit details

ALTER TABLE `roles` DROP COLUMN `data_scope`;
