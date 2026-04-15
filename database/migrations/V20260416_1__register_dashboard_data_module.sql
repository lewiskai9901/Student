-- ============================================================================
-- V20260416_1: Register 'dashboard' module in data_modules
-- ============================================================================
-- /dashboard/overview 由 DashboardOverviewQueryService 自行按 DataScope 收敛查询，
-- 并非一张业务表。这里把它当作一个"聚合视图"模块登记，让角色数据权限配置页面能
-- 为它配置 scope_code（ALL / DEPARTMENT_AND_BELOW / DEPARTMENT 等）。
--
-- 设计要点：
--   resource_type  NULL  — 仪表盘没有对应的 access_relations.resource_type
--   org_unit_field ''    — 多表聚合，无单一组织列
--   creator_field  ''    — 同上
--   sort_order     0     — 置顶，首页性质
--
-- 幂等：使用 INSERT ... ON DUPLICATE KEY UPDATE（uk_tenant_code 唯一约束）
-- ============================================================================

INSERT INTO data_modules
    (tenant_id, module_code, module_name, domain_code, domain_name,
     resource_type, org_unit_field, creator_field, sort_order, enabled)
VALUES
    (1, 'dashboard', '首页仪表盘', 'dashboard', '仪表盘',
     NULL, '', '', 0, 1)
ON DUPLICATE KEY UPDATE
    module_name = VALUES(module_name),
    domain_code = VALUES(domain_code),
    domain_name = VALUES(domain_name),
    sort_order  = VALUES(sort_order),
    enabled     = VALUES(enabled);
