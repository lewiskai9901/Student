-- ============================================================================
-- V20260416_2: Seed `dashboard:view` permission
-- ============================================================================
-- `/dashboard/overview` 由 @CasbinAccess(resource="dashboard", action="view") 门控。
-- Casbin policy 从 role_permissions JOIN permissions 组装，所以 permissions 里必须
-- 有一条 permission_code='dashboard:view' 的记录，否则：
--   - 后端 enforcer 无从得知此资源存在
--   - 前端"角色-权限"配置页看不到它，无法授予
--
-- 不在本迁移里把权限自动绑到任何业务角色——SUPER_ADMIN 由拦截器短路放行，
-- 其他角色应由管理员按需在 UI 中显式勾选。
--
-- 幂等：permission_code 有 UNIQUE 索引，使用 ON DUPLICATE KEY UPDATE
-- ============================================================================

INSERT INTO permissions
    (permission_code, permission_name, permission_desc,
     resource_type, parent_id, path, component, icon,
     sort_order, status, tenant_id, deleted)
VALUES
    ('dashboard:view', '查看首页仪表盘',
     '访问 /dashboard/overview 聚合统计；数据范围由 dashboard 模块的 DataPermission 配置决定',
     1, 0, '/dashboard', 'dashboard/DashboardView', 'DataBoard',
     0, 1, 1, 0)
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name),
    permission_desc = VALUES(permission_desc),
    resource_type   = VALUES(resource_type),
    parent_id       = VALUES(parent_id),
    path            = VALUES(path),
    component       = VALUES(component),
    icon            = VALUES(icon),
    sort_order      = VALUES(sort_order),
    status          = VALUES(status),
    deleted         = 0;
