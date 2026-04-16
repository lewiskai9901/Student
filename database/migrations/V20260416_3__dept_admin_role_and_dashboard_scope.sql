-- ============================================================================
-- V20260416_3: Create DEPT_ADMIN role + dashboard permission + DataScope
-- ============================================================================
-- 验证 Step 2 DashboardOverviewQueryService 的 DEPARTMENT_AND_BELOW 收敛逻辑。
-- 系部管理员登录后看到的 /dashboard/overview 只包含其 org_unit 子树的统计。
--
-- 包含：
--   1. DEPT_ADMIN 角色（系部管理员）
--   2. dashboard:view 权限绑定 → role_permissions
--   3. dashboard 模块 DataScope = DEPARTMENT_AND_BELOW → role_data_permissions_v5
--   4. teacher01 额外绑定 DEPT_ADMIN 角色 → user_roles
--
-- 幂等：所有 INSERT 使用 IGNORE 或 ON DUPLICATE KEY UPDATE
-- ============================================================================

-- 1. 创建角色（role_code UNIQUE 约束保证幂等）
INSERT IGNORE INTO roles
    (role_name, role_code, role_desc, role_type, sort_order, status, tenant_id, deleted)
VALUES
    ('系部管理员', 'DEPT_ADMIN', '系/院级管理角色，可查看本部门及下级数据', 'CUSTOM', 10, 1, 1, 0);

-- 2. 绑定 dashboard:view 权限
INSERT IGNORE INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.id, p.id, 1
FROM roles r, permissions p
WHERE r.role_code = 'DEPT_ADMIN'
  AND p.permission_code = 'dashboard:view';

-- 3. 配置 dashboard 模块 DataScope = DEPARTMENT_AND_BELOW
INSERT INTO role_data_permissions_v5
    (tenant_id, role_id, module_code, scope_code, description)
SELECT 1, r.id, 'dashboard', 'DEPARTMENT_AND_BELOW', '系部管理员看本部门及下级聚合数据'
FROM roles r WHERE r.role_code = 'DEPT_ADMIN'
ON DUPLICATE KEY UPDATE
    scope_code  = VALUES(scope_code),
    description = VALUES(description);

-- 4. teacher01 绑定 DEPT_ADMIN 角色（保留 TEACHER 角色不变，额外加一条）
--    scope_type=ORG_UNIT 表示此角色绑定仅在指定 org_unit 上生效
INSERT IGNORE INTO user_roles (user_id, role_id, tenant_id, scope_type, scope_id, is_active, reason)
SELECT 2041870507300622337, r.id, 1, 'ORG_UNIT', 2040636269108707330, 1, '系部管理员：经济与信息技术系'
FROM roles r WHERE r.role_code = 'DEPT_ADMIN';
