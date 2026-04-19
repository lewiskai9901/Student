-- ============================================================
-- grade_batch 数据权限配置
-- 1. 注册 grade_batch 到 data_modules
-- 2. 为 TEACHER 角色配置 SELF 范围（只看自己创建的成绩批次）
-- 3. 为 DEPT_ADMIN 配置 DEPARTMENT_AND_BELOW（看部门及下级的批次）
-- 4. 同时补齐 exam_batch 在 TEACHER / DEPT_ADMIN 的配置
-- ============================================================

-- 1. 注册 grade_batch 模块
INSERT INTO data_modules (tenant_id, module_code, module_name, domain_code, domain_name, org_unit_field, creator_field, enabled, sort_order)
VALUES (1, 'grade_batch', '成绩批次', 'teaching', '教务管理', 'org_unit_id', 'created_by', 1, 20)
ON DUPLICATE KEY UPDATE module_name = VALUES(module_name), org_unit_field = VALUES(org_unit_field), creator_field = VALUES(creator_field), enabled = 1;

-- 2. TEACHER (id=2022900002094850049) - grade_batch = SELF
INSERT INTO role_data_permissions_v5 (tenant_id, role_id, module_code, scope_code, description)
VALUES (1, 2022900002094850049, 'grade_batch', 'SELF', '教师只能看自己创建的成绩批次')
ON DUPLICATE KEY UPDATE scope_code = VALUES(scope_code);

-- 3. DEPT_ADMIN (id=2022900002094850053) - grade_batch = DEPARTMENT_AND_BELOW
INSERT INTO role_data_permissions_v5 (tenant_id, role_id, module_code, scope_code, description)
VALUES (1, 2022900002094850053, 'grade_batch', 'DEPARTMENT_AND_BELOW', '系部管理员看本部门及下级的成绩批次')
ON DUPLICATE KEY UPDATE scope_code = VALUES(scope_code);

-- 4. 同理补齐 exam_batch（已注册但没给 TEACHER/DEPT_ADMIN 配过）
INSERT INTO role_data_permissions_v5 (tenant_id, role_id, module_code, scope_code, description)
VALUES
  (1, 2022900002094850049, 'exam_batch', 'SELF',                 '教师只看自己创建的考试批次'),
  (1, 2022900002094850053, 'exam_batch', 'DEPARTMENT_AND_BELOW', '系部管理员看本部门及下级考试批次')
ON DUPLICATE KEY UPDATE scope_code = VALUES(scope_code);
