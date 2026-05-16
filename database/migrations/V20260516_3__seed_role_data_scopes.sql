-- ============================================================================
-- V20260516_3: 补 7 个 0 规则角色 + 修 DEPT_ADMIN 配错的 role_data_scopes
-- ============================================================================
-- 当前 25 个 production 角色中, 7 个完全没规则 (SCHOOL_ADMIN / ACADEMIC_DIRECTOR /
-- COUNSELOR / DORMITORY_MANAGER / PARENT / STUDENT / TENANT_ADMIN),
-- 加上 DEPT_ADMIN.student 误配 ALL — 这一批集中修.
--
-- BY_CLASS / BY_GRADE / BY_MAJOR 插件维度 Resolver class 当前不存在 (P2 阶段补),
-- 此 seed 先用 DEPARTMENT_AND_BELOW 作为桥接, P2 后再升级.
--
-- 设计原则:
--   - 全校管理岗 (SCHOOL_ADMIN / ACADEMIC_DIRECTOR / TENANT_ADMIN) → ALL on managed
--   - 部门管理岗 (DEPT_ADMIN) → DEPARTMENT_AND_BELOW
--   - 业务执行岗 (COUNSELOR / DORMITORY_MANAGER) → DEPARTMENT_AND_BELOW (P2 后改 BY_*)
--   - 末端用户 (STUDENT / PARENT) → SELF
-- ============================================================================

-- 修 DEPT_ADMIN.student=ALL 错配
UPDATE role_data_scopes rds
JOIN roles r ON r.id = rds.role_id
SET rds.scope_type = 'DEPARTMENT_AND_BELOW'
WHERE r.role_code = 'DEPT_ADMIN'
  AND rds.resource_code = 'student'
  AND rds.scope_type = 'ALL';

-- 通用 INSERT 辅助 (幂等):同角色 + 同资源 + 同租户 已存在则跳过
-- 用 INSERT IGNORE (依赖 uk_role_res 唯一键) — 这是该表唯一性约束的设计

-- ====================================================
-- SCHOOL_ADMIN (学校管理员) — 全校管理岗
-- ====================================================
INSERT IGNORE INTO role_data_scopes (role_id, resource_code, scope_type, priority, tenant_id, deleted)
SELECT r.id, rc.resource_code, rc.scope_type, 100, 1, 0
FROM roles r
CROSS JOIN (
    SELECT 'org_unit'                AS resource_code, 'ALL'                  AS scope_type UNION ALL
    SELECT 'student',                  'ALL'                                   UNION ALL
    SELECT 'school_class',             'ALL'                                   UNION ALL
    SELECT 'teaching_task',            'ALL'                                   UNION ALL
    SELECT 'class_course_assignment',  'ALL'                                   UNION ALL
    SELECT 'dashboard',                'ALL'                                   UNION ALL
    SELECT 'attendance',               'ALL'                                   UNION ALL
    SELECT 'student_grade',            'ALL'                                   UNION ALL
    SELECT 'grade_batch',              'ALL'                                   UNION ALL
    SELECT 'exam',                     'ALL'                                   UNION ALL
    SELECT 'exam_batch',               'ALL'                                   UNION ALL
    SELECT 'place',                    'ALL'                                   UNION ALL
    SELECT 'user',                     'ALL'                                   UNION ALL
    SELECT 'role',                     'ALL'                                   UNION ALL
    SELECT 'inspection_project',       'ALL'                                   UNION ALL
    SELECT 'inspection_record',        'ALL'                                   UNION ALL
    SELECT 'inspection_task',          'ALL'                                   UNION ALL
    SELECT 'inspection_corrective',    'ALL'                                   UNION ALL
    SELECT 'inspection_appeal',        'ALL'                                   UNION ALL
    SELECT 'inspection_summary',       'ALL'                                   UNION ALL
    SELECT 'inspection_template',      'ALL'                                   UNION ALL
    SELECT 'inspection_alert',         'ALL'                                   UNION ALL
    SELECT 'inspection_observation',   'ALL'                                   UNION ALL
    SELECT 'inspection_violation',     'ALL'                                   UNION ALL
    SELECT 'notification',             'ALL'                                   UNION ALL
    SELECT 'dormitory_building',       'ALL'                                   UNION ALL
    SELECT 'dormitory_room',           'ALL'                                   UNION ALL
    SELECT 'dormitory_checkin',        'ALL'
) rc
WHERE r.role_code = 'SCHOOL_ADMIN';

-- ====================================================
-- ACADEMIC_DIRECTOR (教务主任) — 教务主导
-- ====================================================
INSERT IGNORE INTO role_data_scopes (role_id, resource_code, scope_type, priority, tenant_id, deleted)
SELECT r.id, rc.resource_code, rc.scope_type, 90, 1, 0
FROM roles r
CROSS JOIN (
    SELECT 'org_unit'                AS resource_code, 'ALL'                  AS scope_type UNION ALL
    SELECT 'student',                  'DEPARTMENT_AND_BELOW'                  UNION ALL
    SELECT 'school_class',             'DEPARTMENT_AND_BELOW'                  UNION ALL
    SELECT 'teaching_task',            'ALL'                                   UNION ALL
    SELECT 'class_course_assignment',  'ALL'                                   UNION ALL
    SELECT 'semester_offering',        'ALL'                                   UNION ALL
    SELECT 'schedule_entry',           'ALL'                                   UNION ALL
    SELECT 'scheduling_constraint',    'ALL'                                   UNION ALL
    SELECT 'schedule_conflict_record', 'ALL'                                   UNION ALL
    SELECT 'student_grade',            'ALL'                                   UNION ALL
    SELECT 'grade_batch',              'ALL'                                   UNION ALL
    SELECT 'exam',                     'ALL'                                   UNION ALL
    SELECT 'exam_batch',               'ALL'                                   UNION ALL
    SELECT 'course_evaluation',        'ALL'                                   UNION ALL
    SELECT 'evaluation_response',      'ALL'                                   UNION ALL
    SELECT 'teacher_preference',       'ALL'                                   UNION ALL
    SELECT 'teaching_progress',        'ALL'                                   UNION ALL
    SELECT 'attendance',               'DEPARTMENT_AND_BELOW'                  UNION ALL
    SELECT 'dashboard',                'ALL'
) rc
WHERE r.role_code = 'ACADEMIC_DIRECTOR';

-- ====================================================
-- COUNSELOR (辅导员) — 学生工作
-- (P2 后改 BY_CLASS, 目前用 DEPARTMENT_AND_BELOW 桥接)
-- ====================================================
INSERT IGNORE INTO role_data_scopes (role_id, resource_code, scope_type, priority, tenant_id, deleted)
SELECT r.id, rc.resource_code, rc.scope_type, 80, 1, 0
FROM roles r
CROSS JOIN (
    SELECT 'student'                 AS resource_code, 'DEPARTMENT_AND_BELOW' AS scope_type UNION ALL
    SELECT 'attendance',               'DEPARTMENT_AND_BELOW'                  UNION ALL
    SELECT 'school_class',             'DEPARTMENT_AND_BELOW'                  UNION ALL
    SELECT 'inspection_record',        'DEPARTMENT_AND_BELOW'                  UNION ALL
    SELECT 'inspection_appeal',        'DEPARTMENT_AND_BELOW'                  UNION ALL
    SELECT 'dormitory_checkin',        'DEPARTMENT_AND_BELOW'                  UNION ALL
    SELECT 'dashboard',                'DEPARTMENT_AND_BELOW'                  UNION ALL
    SELECT 'notification',             'DEPARTMENT_AND_BELOW'
) rc
WHERE r.role_code = 'COUNSELOR';

-- ====================================================
-- DORMITORY_MANAGER (宿管员) — 宿舍管理
-- ====================================================
INSERT IGNORE INTO role_data_scopes (role_id, resource_code, scope_type, priority, tenant_id, deleted)
SELECT r.id, rc.resource_code, rc.scope_type, 70, 1, 0
FROM roles r
CROSS JOIN (
    SELECT 'dormitory_building'      AS resource_code, 'ALL'                  AS scope_type UNION ALL
    SELECT 'dormitory_room',           'ALL'                                   UNION ALL
    SELECT 'dormitory_checkin',        'ALL'                                   UNION ALL
    SELECT 'place',                    'DEPARTMENT_AND_BELOW'                  UNION ALL
    SELECT 'student',                  'DEPARTMENT_AND_BELOW'                  UNION ALL
    SELECT 'inspection_record',        'DEPARTMENT_AND_BELOW'                  UNION ALL
    SELECT 'inspection_observation',   'DEPARTMENT_AND_BELOW'                  UNION ALL
    SELECT 'inspection_violation',     'DEPARTMENT_AND_BELOW'                  UNION ALL
    SELECT 'dashboard',                'DEPARTMENT_AND_BELOW'                  UNION ALL
    SELECT 'notification',             'DEPARTMENT_AND_BELOW'
) rc
WHERE r.role_code = 'DORMITORY_MANAGER';

-- ====================================================
-- PARENT (家长) — 末端只读
-- ====================================================
INSERT IGNORE INTO role_data_scopes (role_id, resource_code, scope_type, priority, tenant_id, deleted)
SELECT r.id, rc.resource_code, rc.scope_type, 10, 1, 0
FROM roles r
CROSS JOIN (
    SELECT 'student'                 AS resource_code, 'SELF'                 AS scope_type UNION ALL
    SELECT 'attendance',               'SELF'                                  UNION ALL
    SELECT 'student_grade',            'SELF'                                  UNION ALL
    SELECT 'notification',             'SELF'                                  UNION ALL
    SELECT 'inspection_record',        'SELF'                                  UNION ALL
    SELECT 'dashboard',                'SELF'
) rc
WHERE r.role_code = 'PARENT';

-- ====================================================
-- STUDENT (学生) — 末端只读
-- ====================================================
INSERT IGNORE INTO role_data_scopes (role_id, resource_code, scope_type, priority, tenant_id, deleted)
SELECT r.id, rc.resource_code, rc.scope_type, 10, 1, 0
FROM roles r
CROSS JOIN (
    SELECT 'student'                 AS resource_code, 'SELF'                 AS scope_type UNION ALL
    SELECT 'attendance',               'SELF'                                  UNION ALL
    SELECT 'student_grade',            'SELF'                                  UNION ALL
    SELECT 'notification',             'SELF'                                  UNION ALL
    SELECT 'inspection_personal',      'SELF'                                  UNION ALL
    SELECT 'inspection_appeal',        'SELF'                                  UNION ALL
    SELECT 'dashboard',                'SELF'
) rc
WHERE r.role_code = 'STUDENT';

-- ====================================================
-- TENANT_ADMIN (租户管理员) — 系统管理
-- ====================================================
INSERT IGNORE INTO role_data_scopes (role_id, resource_code, scope_type, priority, tenant_id, deleted)
SELECT r.id, rc.resource_code, rc.scope_type, 100, 1, 0
FROM roles r
CROSS JOIN (
    SELECT 'system_user'             AS resource_code, 'ALL'                  AS scope_type UNION ALL
    SELECT 'system_role',              'ALL'                                   UNION ALL
    SELECT 'org_unit',                 'ALL'                                   UNION ALL
    SELECT 'dashboard',                'ALL'                                   UNION ALL
    SELECT 'user',                     'ALL'                                   UNION ALL
    SELECT 'role',                     'ALL'
) rc
WHERE r.role_code = 'TENANT_ADMIN';
