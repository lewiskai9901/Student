-- ============================================================
-- 注册 5 个 teaching 模块到 data_resources + 配置默认角色范围 (2026-05-04)
--
-- 依赖: V20260504_1__teaching_data_permission_columns.sql (补列)
--
-- 注: V3 后表名为 data_resources (不是 data_modules), 主键 resource_code (VARCHAR)
--     角色范围表是 role_data_scopes (不是 role_data_permissions_v5)
--
-- 模块对应关系:
--   schedule_entry            → schedule_entries           (org_unit + created_by)
--   class_course_assignment   → class_course_assignments   (org_unit + created_by, created_by 新增)
--   semester_offering         → semester_course_offerings  (org_unit 新增 + created_by; org_unit_id NULL 表示全校级)
--   scheduling_constraint     → scheduling_constraints     (org_unit 新增 + created_by; org_unit_id NULL 表示全校级)
--   schedule_conflict_record  → schedule_conflict_records  (org_unit 派生自 entry_id_1, created_by 派生)
-- ============================================================

-- -----------------------------------------------------------
-- 1. data_resources 注册 (主键: tenant_id + resource_code)
-- -----------------------------------------------------------
INSERT INTO data_resources
    (tenant_id, resource_code, resource_name, domain_code, domain_name, industry,
     access_resource_type, org_unit_field, creator_field, registered_by, sort_order, enabled, plugin_enabled)
VALUES
    (1, 'schedule_entry',           '排课条目',     'teaching', '教务管理', 'EDUCATION', NULL, 'org_unit_id', 'created_by', 'PLUGIN', 30, 1, 1),
    (1, 'class_course_assignment',  '班级开课',     'teaching', '教务管理', 'EDUCATION', NULL, 'org_unit_id', 'created_by', 'PLUGIN', 31, 1, 1),
    (1, 'semester_offering',        '学期开课计划', 'teaching', '教务管理', 'EDUCATION', NULL, 'org_unit_id', 'created_by', 'PLUGIN', 32, 1, 1),
    (1, 'scheduling_constraint',    '排课约束',     'teaching', '教务管理', 'EDUCATION', NULL, 'org_unit_id', 'created_by', 'PLUGIN', 33, 1, 1),
    (1, 'schedule_conflict_record', '排课冲突记录', 'teaching', '教务管理', 'EDUCATION', NULL, 'org_unit_id', 'created_by', 'PLUGIN', 34, 1, 1)
ON DUPLICATE KEY UPDATE
    resource_name        = VALUES(resource_name),
    domain_code          = VALUES(domain_code),
    domain_name          = VALUES(domain_name),
    industry             = VALUES(industry),
    org_unit_field       = VALUES(org_unit_field),
    creator_field        = VALUES(creator_field),
    enabled              = 1,
    plugin_enabled       = 1;

-- -----------------------------------------------------------
-- 2. 默认角色数据权限 (TEACHER / DEPT_ADMIN)
--    TEACHER     id=2022900002094850049  → SELF
--    DEPT_ADMIN  id=2022900002094850053  → DEPARTMENT_AND_BELOW
--
-- 注: SUPER_ADMIN 短路, 不需要配; 其他角色未配置 → 自动 SELF (interceptor 默认行为)
-- -----------------------------------------------------------
INSERT INTO role_data_scopes (tenant_id, role_id, resource_code, scope_type)
VALUES
    -- TEACHER = SELF
    (1, 2022900002094850049, 'schedule_entry',           'SELF'),
    (1, 2022900002094850049, 'class_course_assignment',  'SELF'),
    (1, 2022900002094850049, 'semester_offering',        'SELF'),
    (1, 2022900002094850049, 'scheduling_constraint',    'SELF'),
    (1, 2022900002094850049, 'schedule_conflict_record', 'SELF'),
    -- DEPT_ADMIN = DEPARTMENT_AND_BELOW
    (1, 2022900002094850053, 'schedule_entry',           'DEPARTMENT_AND_BELOW'),
    (1, 2022900002094850053, 'class_course_assignment',  'DEPARTMENT_AND_BELOW'),
    (1, 2022900002094850053, 'semester_offering',        'DEPARTMENT_AND_BELOW'),
    (1, 2022900002094850053, 'scheduling_constraint',    'DEPARTMENT_AND_BELOW'),
    (1, 2022900002094850053, 'schedule_conflict_record', 'DEPARTMENT_AND_BELOW')
ON DUPLICATE KEY UPDATE
    scope_type = VALUES(scope_type);
