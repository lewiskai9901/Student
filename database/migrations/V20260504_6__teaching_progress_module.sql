-- ============================================================
-- 注册 teaching_progress 数据资源 (2026-05-04)
-- ============================================================
INSERT INTO data_resources
    (tenant_id, resource_code, resource_name, domain_code, domain_name, industry,
     access_resource_type, org_unit_field, creator_field, registered_by, sort_order, enabled, plugin_enabled)
VALUES
    (1, 'teaching_progress', '教学进度', 'teaching', '教务管理', 'EDUCATION',
     NULL, 'org_unit_id', 'recorded_by', 'PLUGIN', 36, 1, 1)
ON DUPLICATE KEY UPDATE
    resource_name  = VALUES(resource_name),
    org_unit_field = VALUES(org_unit_field),
    creator_field  = VALUES(creator_field),
    enabled        = 1,
    plugin_enabled = 1;

-- TEACHER 默认 SELF (只看自己录入的进度)
INSERT INTO role_data_scopes (tenant_id, role_id, resource_code, scope_type)
VALUES (1, 2022900002094850049, 'teaching_progress', 'SELF')
ON DUPLICATE KEY UPDATE scope_type = VALUES(scope_type);

-- DEPT_ADMIN 默认 DEPARTMENT_AND_BELOW (看本部门及下级所有教师的进度)
INSERT INTO role_data_scopes (tenant_id, role_id, resource_code, scope_type)
VALUES (1, 2022900002094850053, 'teaching_progress', 'DEPARTMENT_AND_BELOW')
ON DUPLICATE KEY UPDATE scope_type = VALUES(scope_type);
