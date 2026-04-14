-- =====================================================
-- V96.0.0: 标准组织类型、用户类型初始数据 + 权限标准化
-- =====================================================

-- =====================================================
-- 1. 组织类型种子数据
-- =====================================================
-- UNIQUE KEY on (type_code, deleted)

-- Step 1a: Rename legacy 'ORGANIZATION' -> 'SCHOOL' first (before inserting SCHOOL)
UPDATE org_unit_types SET type_code = 'SCHOOL', type_name = '学校', category = 'ROOT',
    icon = 'School', description = '学校/校区',
    features = '{"canHaveChildren":true,"canBeInspected":true,"isAcademic":false}',
    allowed_child_type_codes = '["DEPARTMENT","ADMIN_OFFICE"]',
    is_academic = 0, is_system = 1, sort_order = 1
WHERE type_code = 'ORGANIZATION' AND deleted = 0;

-- Step 1b: Upsert standard org types (SCHOOL may already exist from rename above)
INSERT INTO org_unit_types (type_code, type_name, category, parent_type_code, icon, description, features, allowed_child_type_codes, is_academic, is_system, is_enabled, sort_order, deleted, tenant_id)
VALUES
('SCHOOL', '学校', 'ROOT', NULL, 'School', '学校/校区', '{"canHaveChildren":true,"canBeInspected":true,"isAcademic":false}', '["DEPARTMENT","ADMIN_OFFICE"]', 0, 1, 1, 1, 0, 1),
('DEPARTMENT', '系部', 'BRANCH', 'SCHOOL', 'Building2', '教学系部/学院', '{"canHaveChildren":true,"canBeInspected":true,"isAcademic":true}', '["TEACHING_GROUP"]', 1, 1, 1, 2, 0, 1),
('ADMIN_OFFICE', '行政处室', 'FUNCTIONAL', 'SCHOOL', 'Briefcase', '行政管理部门', '{"canHaveChildren":true,"canBeInspected":false,"isAcademic":false}', '["SECTION"]', 0, 1, 1, 3, 0, 1),
('TEACHING_GROUP', '教研室', 'GROUP', 'DEPARTMENT', 'Users', '教研室/教学组', '{"canHaveChildren":false,"canBeInspected":true,"isAcademic":true}', '[]', 1, 1, 1, 4, 0, 1),
('SECTION', '科室', 'GROUP', 'ADMIN_OFFICE', 'FolderOpen', '行政科室', '{"canHaveChildren":false,"canBeInspected":false,"isAcademic":false}', '[]', 0, 1, 1, 5, 0, 1)
ON DUPLICATE KEY UPDATE
    type_name = VALUES(type_name),
    category = VALUES(category),
    parent_type_code = VALUES(parent_type_code),
    icon = VALUES(icon),
    description = VALUES(description),
    features = VALUES(features),
    allowed_child_type_codes = VALUES(allowed_child_type_codes),
    is_academic = VALUES(is_academic),
    is_system = VALUES(is_system),
    sort_order = VALUES(sort_order);

-- Step 1c: Fix any org_units referencing old 'ORGANIZATION' type code
UPDATE org_units SET type_code = 'SCHOOL' WHERE type_code = 'ORGANIZATION';

-- Step 1d: Update DEPARTMENT parent from legacy code to SCHOOL
UPDATE org_unit_types SET parent_type_code = 'SCHOOL'
WHERE type_code = 'DEPARTMENT' AND deleted = 0 AND (parent_type_code IS NULL OR parent_type_code = 'ORGANIZATION');

-- =====================================================
-- 2. 用户类型种子数据
-- =====================================================
-- UNIQUE KEY on type_code alone

INSERT INTO user_types (type_code, type_name, category, parent_type_code, icon, description, features, is_system, is_enabled, sort_order, deleted, tenant_id)
VALUES
('SUPER_ADMIN', '超级管理员', 'ADMIN', NULL, 'Shield', '系统最高权限管理员', '{"canLogin":true,"canManageAll":true}', 1, 1, 1, 0, 1),
('ADMIN', '管理员', 'ADMIN', NULL, 'ShieldCheck', '学校管理员', '{"canLogin":true,"canManageAll":false}', 1, 1, 2, 0, 1),
('TEACHER', '教师', 'STAFF', NULL, 'GraduationCap', '教职工/授课教师', '{"canLogin":true,"requiresOrg":true,"canTeach":true,"canViewOwnSchedule":true,"canViewOwnStudents":true,"canRecordAttendance":true,"canBeSubstitute":true,"canSubmitGrades":true}', 1, 1, 3, 0, 1),
('COUNSELOR', '辅导员', 'STAFF', NULL, 'UserCheck', '年级辅导员/学管人员', '{"canLogin":true,"requiresOrg":true,"canManageStudents":true}', 1, 1, 4, 0, 1),
('STUDENT', '学生', 'MEMBER', NULL, 'User', '在校学生', '{"canLogin":true,"requiresOrg":true,"requiresClass":true}', 1, 1, 5, 0, 1),
('STAFF', '职工', 'STAFF', NULL, 'Briefcase', '行政/后勤职工', '{"canLogin":true,"requiresOrg":true}', 1, 1, 6, 0, 1),
('PARENT', '家长', 'EXTERNAL', NULL, 'Users', '学生家长', '{"canLogin":true,"requiresOrg":false}', 0, 1, 7, 0, 1),
('GUEST', '访客', 'EXTERNAL', NULL, 'UserPlus', '临时访客', '{"canLogin":false,"requiresOrg":false}', 0, 1, 8, 0, 1)
ON DUPLICATE KEY UPDATE
    type_name = VALUES(type_name),
    category = VALUES(category),
    icon = VALUES(icon),
    description = VALUES(description),
    features = VALUES(features),
    is_system = VALUES(is_system),
    is_enabled = VALUES(is_enabled),
    sort_order = VALUES(sort_order),
    deleted = VALUES(deleted);

-- =====================================================
-- 3. 权限标准化: view + edit only (remove add/create/update/delete variants)
-- =====================================================
-- permissions.deleted is tinyint (0=active, 1=deleted)
-- permissions.resource_type: 1=menu, 2=button, 3=api

-- 3a. system:org permissions - rename create -> edit, soft-delete update/delete
UPDATE permissions SET permission_code = 'system:org:edit', permission_name = '编辑组织类型'
WHERE permission_code = 'system:org:create' AND deleted = 0;

UPDATE permissions SET deleted = 1
WHERE permission_code IN ('system:org:update', 'system:org:delete') AND deleted = 0;

-- 3b. system:user permissions - soft-delete add/delete (edit already exists as id=13)
UPDATE permissions SET deleted = 1
WHERE permission_code IN ('system:user:add', 'system:user:delete') AND deleted = 0;

-- Update system:user:edit name for consistency
UPDATE permissions SET permission_name = '编辑用户类型'
WHERE permission_code = 'system:user:edit' AND deleted = 0;

-- 3c. system:place-type permissions - soft-delete add/delete (edit already exists)
UPDATE permissions SET deleted = 1
WHERE permission_code IN ('system:place-type:add', 'system:place-type:delete') AND deleted = 0;

-- Update system:place-type:edit name for consistency
UPDATE permissions SET permission_name = '编辑场所类型'
WHERE permission_code = 'system:place-type:edit' AND deleted = 0;
