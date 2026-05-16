-- ============================================================================
-- V20260516_8: teacher_preference scope 三级化 (修 P6-2 子模块 1.7 二极化)
-- ============================================================================
-- 当前状态: SELF (teacher) / ALL (DEPT_ADMIN+ACADEMIC_DIRECTOR) - 缺 DEPARTMENT 中间层.
-- 真业务: 系部管理员应该看本系部教师的偏好 (排课/调度时参考),
--         而非全校 (ACADEMIC_DIRECTOR 才需要全校视角).
--
-- 三级:
--   TEACHER / SUBJECT_TEACHER / CLASS_TEACHER → SELF (老师只管自己偏好)
--   DEPT_ADMIN → DEPARTMENT_AND_BELOW (本系部教师)
--   ACADEMIC_DIRECTOR / SCHOOL_ADMIN → ALL (全校排课)
-- ============================================================================

-- 1) DEPT_ADMIN.teacher_preference 从 ALL 改 DEPARTMENT_AND_BELOW (修二极化)
UPDATE role_data_scopes rds
JOIN roles r ON r.id = rds.role_id
SET rds.scope_type = 'DEPARTMENT_AND_BELOW'
WHERE r.role_code = 'DEPT_ADMIN'
  AND rds.resource_code = 'teacher_preference'
  AND rds.scope_type = 'ALL';

-- 2) 补 SCHOOL_ADMIN.teacher_preference = ALL
INSERT IGNORE INTO role_data_scopes (role_id, resource_code, scope_type, priority, tenant_id, deleted)
SELECT r.id, 'teacher_preference', 'ALL', 100, 1, 0
FROM roles r WHERE r.role_code = 'SCHOOL_ADMIN';

-- 3) 补 SUBJECT_TEACHER / CLASS_TEACHER.teacher_preference = SELF
INSERT IGNORE INTO role_data_scopes (role_id, resource_code, scope_type, priority, tenant_id, deleted)
SELECT r.id, 'teacher_preference', 'SELF', 50, 1, 0
FROM roles r WHERE r.role_code IN ('SUBJECT_TEACHER', 'CLASS_TEACHER');
