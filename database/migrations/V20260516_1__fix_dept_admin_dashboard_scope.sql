-- ============================================================================
-- V20260516_1: 修复 DEPT_ADMIN 的 dashboard scope (SELF → DEPARTMENT_AND_BELOW)
-- ============================================================================
-- 历史 V20260416_3 migration 将系部管理员的 dashboard DataScope 配为
-- DEPARTMENT_AND_BELOW (在旧表 role_data_permissions_v5 里).
-- 但 V20260419_4 把数据搬到 role_data_scopes 时, 已存在的
-- (DEPT_ADMIN, dashboard, SELF) 行 (id=27, created_at 2026-04-18) 把目标行挡住了
-- (WHERE NOT EXISTS 条件), 导致最终 scope 被锁在 SELF.
--
-- 结果: DashboardOverviewQueryService 看到 SELF → 走 default 分支 → 返回空,
-- teacher01 dashboard studentCount = 0, e2e dashboard-critical 长期红.
--
-- 此 migration 把所有 DEPT_ADMIN 角色的 dashboard scope 强制改为 DEPARTMENT_AND_BELOW,
-- 与 V20260416_3 原意一致.
-- ============================================================================

UPDATE role_data_scopes rds
JOIN roles r ON r.id = rds.role_id
SET rds.scope_type = 'DEPARTMENT_AND_BELOW'
WHERE r.role_code = 'DEPT_ADMIN'
  AND rds.resource_code = 'dashboard'
  AND rds.scope_type = 'SELF';

-- 兜底: 如果有 DEPT_ADMIN 但没有 dashboard scope 行, 补一条
INSERT INTO role_data_scopes (role_id, resource_code, scope_type, priority, tenant_id, deleted)
SELECT r.id, 'dashboard', 'DEPARTMENT_AND_BELOW', 0, 1, 0
FROM roles r
WHERE r.role_code = 'DEPT_ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM role_data_scopes rds
    WHERE rds.role_id = r.id
      AND rds.resource_code = 'dashboard'
      AND rds.deleted = 0
  );
