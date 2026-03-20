-- V34.0.0: Scoped Role Assignment
-- Adds scope_type + scope_id to user_roles for scoped RBAC
-- Also adds missing columns (assigned_at, assigned_by, expires_at, is_active) that code references but DB lacks

-- 1. Add missing columns + new scope columns
ALTER TABLE user_roles ADD COLUMN scope_type VARCHAR(20) NOT NULL DEFAULT 'ALL';
ALTER TABLE user_roles ADD COLUMN scope_id BIGINT NOT NULL DEFAULT 0;
ALTER TABLE user_roles ADD COLUMN assigned_at DATETIME DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE user_roles ADD COLUMN assigned_by BIGINT DEFAULT NULL;
ALTER TABLE user_roles ADD COLUMN expires_at DATETIME DEFAULT NULL;
ALTER TABLE user_roles ADD COLUMN is_active TINYINT(1) NOT NULL DEFAULT 1;

-- 2. Drop old org_unit_id column (replaced by scope_type + scope_id)
ALTER TABLE user_roles DROP COLUMN org_unit_id;

-- 3. Update unique constraint: allow same role assigned to different scopes
ALTER TABLE user_roles DROP INDEX uk_user_role;
ALTER TABLE user_roles ADD UNIQUE INDEX uk_user_role_scope (user_id, role_id, scope_type, scope_id);

-- 4. Index for scope queries
CREATE INDEX idx_user_roles_scope ON user_roles(scope_type, scope_id);
CREATE INDEX idx_user_roles_active ON user_roles(is_active, expires_at);
