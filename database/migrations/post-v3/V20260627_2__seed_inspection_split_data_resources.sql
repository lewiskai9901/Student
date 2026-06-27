-- 完美重构 P1-P3 补漏: 新拆分的检查资源码需进 data_resources (DataPermissionInterceptor.getModuleConfig 读它;
-- 缺则 @Cacheable("dynamicModules") 命中 null → RedisCache 拒绝 null → 422)。
-- ⚠ DataResourceUpserter 只 UPDATE allowed_scopes 不 INSERT 新行(行由 migration 落库), 故 dr() contribution
-- 不会建行 —— 必须本迁移显式 INSERT。inspection_task 已在 baseline seed, 无需补。
-- 镜像 inspection_task: domain=inspection, resource_kind=PLAIN, 全 scope。

INSERT INTO data_resources
  (resource_code, resource_name, domain_code, domain_name, enabled, sort_order, registered_by,
   allowed_scopes, subject_relation_filterable, resource_kind, tenant_id, plugin_enabled)
VALUES
  ('inspection_submission',        '检查提交单',   'inspection', '检查平台', 1, 38, 'CORE',
   '["ALL","DEPARTMENT_AND_BELOW","MANAGED_ORGS_AND_BELOW","DEPARTMENT","MANAGED_ORGS","SELF","CUSTOM"]', 0, 'PLAIN', 1, 1),
  ('inspection_evidence',          '检查证据',     'inspection', '检查平台', 1, 39, 'CORE',
   '["ALL","DEPARTMENT_AND_BELOW","MANAGED_ORGS_AND_BELOW","DEPARTMENT","MANAGED_ORGS","SELF","CUSTOM"]', 0, 'PLAIN', 1, 1),
  ('inspection_submission_detail', '检查提交明细', 'inspection', '检查平台', 1, 40, 'CORE',
   '["ALL","DEPARTMENT_AND_BELOW","MANAGED_ORGS_AND_BELOW","DEPARTMENT","MANAGED_ORGS","SELF","CUSTOM"]', 0, 'PLAIN', 1, 1),
  ('inspection_project_inspector', '检查项目成员', 'inspection', '检查平台', 1, 41, 'CORE',
   '["ALL","DEPARTMENT_AND_BELOW","MANAGED_ORGS_AND_BELOW","DEPARTMENT","MANAGED_ORGS","SELF","CUSTOM"]', 0, 'PLAIN', 1, 1);
