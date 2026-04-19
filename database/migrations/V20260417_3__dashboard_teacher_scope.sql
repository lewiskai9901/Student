-- ============================================================
-- Dashboard 数据权限 —— 给 TEACHER 加 DEPARTMENT 范围
-- DashboardOverviewQueryService 在 MODULE_CODE="dashboard" 下读取范围；
-- 教师没有 scope 配置 → 默认进入 DENY 分支（全部归零）。
-- 业务期望教师能看到本部门级统计，因此在这里补齐。
-- ============================================================

INSERT INTO role_data_permissions_v5 (tenant_id, role_id, module_code, scope_code, description)
VALUES
  (1, 2022900002094850049, 'dashboard', 'DEPARTMENT', '教师在首页看本部门的统计数据')
ON DUPLICATE KEY UPDATE scope_code = VALUES(scope_code), description = VALUES(description);
