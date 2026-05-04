-- ============================================================
-- 注册 teacher_preference 数据资源 (2026-05-04)
-- ============================================================
INSERT INTO data_resources
    (tenant_id, resource_code, resource_name, domain_code, domain_name, industry,
     access_resource_type, org_unit_field, creator_field, registered_by, sort_order, enabled, plugin_enabled)
VALUES
    (1, 'teacher_preference', '教师排课偏好', 'teaching', '教务管理', 'EDUCATION',
     NULL, '', 'teacher_id', 'PLUGIN', 35, 1, 1)
ON DUPLICATE KEY UPDATE
    resource_name  = VALUES(resource_name),
    creator_field  = VALUES(creator_field),
    enabled        = 1,
    plugin_enabled = 1;

-- TEACHER 默认 SELF (只看自己的偏好)
INSERT INTO role_data_scopes (tenant_id, role_id, resource_code, scope_type)
VALUES (1, 2022900002094850049, 'teacher_preference', 'SELF')
ON DUPLICATE KEY UPDATE scope_type = VALUES(scope_type);

-- DEPT_ADMIN 默认 ALL (排课管理需看全部教师偏好)
INSERT INTO role_data_scopes (tenant_id, role_id, resource_code, scope_type)
VALUES (1, 2022900002094850053, 'teacher_preference', 'ALL')
ON DUPLICATE KEY UPDATE scope_type = VALUES(scope_type);
