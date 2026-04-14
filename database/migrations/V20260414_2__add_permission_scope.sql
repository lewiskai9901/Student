-- Phase 1: Add permission_scope to permissions table
--
-- Purpose: Partition permissions into three scopes so UI/API can tell apart
--   - PUBLIC:     any logged-in user (dashboard notices, public lookups)
--   - SELF:       "my space" only (/my/*) — user operating on own data
--   - MANAGEMENT: admin backoffice (/admin/*, /dashboard/*) — requires role-granted access
--
-- The Casbin interceptor gains a scope check:
--   SELF        -> caller must be the resource owner (enforced by @DataPermission SELF scope)
--   MANAGEMENT  -> caller must hold at least one MANAGEMENT-scope permission via their roles
--   PUBLIC      -> no further check beyond authentication
--
-- See docs/plans/2026-04-14-access-control-refactor-design.md Phase 1
-- See docs/security/access-control-guide.md

ALTER TABLE `permissions`
    ADD COLUMN `permission_scope` VARCHAR(20) NOT NULL DEFAULT 'MANAGEMENT'
        COMMENT '权限作用域: PUBLIC=公共, SELF=个人空间, MANAGEMENT=管理后台'
        AFTER `permission_type`;

CREATE INDEX `idx_permission_scope` ON `permissions` (`permission_scope`);

-- Seed classification by permission_code prefix:
--   my:*     -> SELF        (teacher/student personal dashboards)
--   public:* -> PUBLIC      (homepage announcements, public lookups)
--   others   -> MANAGEMENT  (default, already set above)
UPDATE `permissions` SET `permission_scope` = 'SELF'   WHERE `permission_code` LIKE 'my:%';
UPDATE `permissions` SET `permission_scope` = 'PUBLIC' WHERE `permission_code` LIKE 'public:%';
