-- ============================================================
-- 注册学生评教 data_resources (2026-05-04)
-- ============================================================
INSERT INTO data_resources
    (tenant_id, resource_code, resource_name, domain_code, domain_name, industry,
     access_resource_type, org_unit_field, creator_field, registered_by, sort_order, enabled, plugin_enabled)
VALUES
    (1, 'course_evaluation',  '评教活动', 'teaching', '教务管理', 'EDUCATION',
     NULL, 'org_unit_id', 'created_by', 'PLUGIN', 40, 1, 1),
    (1, 'evaluation_response', '评教提交', 'teaching', '教务管理', 'EDUCATION',
     NULL, 'org_unit_id', 'student_id', 'PLUGIN', 41, 1, 1)
ON DUPLICATE KEY UPDATE
    resource_name  = VALUES(resource_name),
    org_unit_field = VALUES(org_unit_field),
    creator_field  = VALUES(creator_field),
    enabled        = 1,
    plugin_enabled = 1;

-- 默认角色范围
INSERT INTO role_data_scopes (tenant_id, role_id, resource_code, scope_type)
VALUES
    -- TEACHER: 评教活动看本部门, 提交看不到 (只有汇总走应用层 SQL)
    (1, 2022900002094850049, 'course_evaluation',  'DEPARTMENT'),
    (1, 2022900002094850049, 'evaluation_response', 'SELF'),
    -- DEPT_ADMIN: 都看本部门及以下
    (1, 2022900002094850053, 'course_evaluation',  'DEPARTMENT_AND_BELOW'),
    (1, 2022900002094850053, 'evaluation_response', 'DEPARTMENT_AND_BELOW')
ON DUPLICATE KEY UPDATE scope_type = VALUES(scope_type);
