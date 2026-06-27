-- B-2 审计修复: org_unit_scores 表从共享码 inspection_project 拆出独立码 inspection_org_unit_score。
-- 原因: org_unit_scores 无 created_by 列, 却共享带 creator=created_by 锚的 inspection_project →
-- {creator,SELF}/默认 SELF 在该表上 created_by=me → SQL 1054 (与 inspection_record 同类多表错配)。
-- 拆出后该码仅 owner_org=org_unit_id (resource_relations 由 contribution 写); 无 SELF scope (表无 created_by,
-- SELF 在无 created_by 表的引擎级退化属已知遗留类, 同 student_grade/school_class, 此处先隔离)。
-- data_resources 行须显式 seed (DataResourceUpserter 只 UPDATE 不 INSERT)。

INSERT INTO data_resources
  (resource_code, resource_name, domain_code, domain_name, enabled, sort_order, registered_by,
   allowed_scopes, subject_relation_filterable, resource_kind, tenant_id, plugin_enabled)
VALUES
  ('inspection_org_unit_score', '组织检查得分', 'inspection', '检查平台', 1, 42, 'CORE',
   '["ALL","DEPARTMENT_AND_BELOW","MANAGED_ORGS_AND_BELOW","DEPARTMENT","MANAGED_ORGS","CUSTOM"]', 0, 'PLAIN', 1, 1);
