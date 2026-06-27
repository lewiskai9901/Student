-- 完美重构 B-1 守护揪出的既有潜在 bug: EDU 的 dormitory / enrollment 资源码 dr() 声明了(EducationManifest)
-- 但 data_resources 无对应行 → 任何 @DataPermission 查询这两资源时 getModuleConfig 返 null →
-- @Cacheable("dynamicModules") RedisCache 拒 null → 422 (与 inspection_submission 同类, 早于本次重构存在)。
-- 修: 补 data_resources 行(DataResourceUpserter 只 UPDATE 不 INSERT)。镜像 attendance: domain=education, PLAIN。
-- scopes 取自 EducationManifest dr() 声明。

INSERT INTO data_resources
  (resource_code, resource_name, domain_code, domain_name, enabled, sort_order, registered_by,
   allowed_scopes, subject_relation_filterable, resource_kind, tenant_id, plugin_enabled)
VALUES
  ('dormitory',  '宿舍管理', 'education', '教育', 1, 50, 'EducationPlugin',
   '["ALL","DEPARTMENT_AND_BELOW","DEPARTMENT","SELF","CUSTOM"]', 0, 'PLAIN', 1, 1),
  ('enrollment', '招生管理', 'education', '教育', 1, 51, 'EducationPlugin',
   '["ALL","DEPARTMENT_AND_BELOW","DEPARTMENT","SELF","CUSTOM"]', 0, 'PLAIN', 1, 1);
