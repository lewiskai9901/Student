-- ============================================================
-- V85.0.0: Restructure permissions for academic domain
-- Migrates old permission codes to new domain-aligned structure:
--   teaching:course   -> academic:course
--   teaching:curriculum -> academic:curriculum
--   major:*           -> academic:major
--   quantification:grade -> academic:grade-direction
--   system:grade      -> student:grade
-- ============================================================

-- 1. Create "academic" top-level permission node
INSERT IGNORE INTO permissions (permission_code, permission_name, parent_id, resource_type, sort_order, status, created_at)
VALUES ('academic', '学术管理', 0, 1, 3, 1, NOW());

SET @academic_id = (SELECT id FROM permissions WHERE permission_code = 'academic' AND deleted = 0 LIMIT 1);

-- 2. Academic domain children

-- academic:major (parent group)
INSERT IGNORE INTO permissions (permission_code, permission_name, parent_id, resource_type, sort_order, status, created_at)
VALUES ('academic:major', '专业管理', @academic_id, 1, 1, 1, NOW());
SET @academic_major_id = (SELECT id FROM permissions WHERE permission_code = 'academic:major' AND deleted = 0 LIMIT 1);

INSERT IGNORE INTO permissions (permission_code, permission_name, parent_id, resource_type, sort_order, status, created_at)
VALUES ('academic:major:view', '查看专业', @academic_major_id, 2, 1, 1, NOW());
INSERT IGNORE INTO permissions (permission_code, permission_name, parent_id, resource_type, sort_order, status, created_at)
VALUES ('academic:major:edit', '编辑专业', @academic_major_id, 2, 2, 1, NOW());

-- academic:course (parent group)
INSERT IGNORE INTO permissions (permission_code, permission_name, parent_id, resource_type, sort_order, status, created_at)
VALUES ('academic:course', '课程管理', @academic_id, 1, 2, 1, NOW());
SET @academic_course_id = (SELECT id FROM permissions WHERE permission_code = 'academic:course' AND deleted = 0 LIMIT 1);

INSERT IGNORE INTO permissions (permission_code, permission_name, parent_id, resource_type, sort_order, status, created_at)
VALUES ('academic:course:view', '查看课程', @academic_course_id, 2, 1, 1, NOW());
INSERT IGNORE INTO permissions (permission_code, permission_name, parent_id, resource_type, sort_order, status, created_at)
VALUES ('academic:course:edit', '编辑课程', @academic_course_id, 2, 2, 1, NOW());

-- academic:curriculum (parent group)
INSERT IGNORE INTO permissions (permission_code, permission_name, parent_id, resource_type, sort_order, status, created_at)
VALUES ('academic:curriculum', '培养方案管理', @academic_id, 1, 3, 1, NOW());
SET @academic_curriculum_id = (SELECT id FROM permissions WHERE permission_code = 'academic:curriculum' AND deleted = 0 LIMIT 1);

INSERT IGNORE INTO permissions (permission_code, permission_name, parent_id, resource_type, sort_order, status, created_at)
VALUES ('academic:curriculum:view', '查看培养方案', @academic_curriculum_id, 2, 1, 1, NOW());
INSERT IGNORE INTO permissions (permission_code, permission_name, parent_id, resource_type, sort_order, status, created_at)
VALUES ('academic:curriculum:edit', '编辑培养方案', @academic_curriculum_id, 2, 2, 1, NOW());

-- academic:grade-direction (parent group)
INSERT IGNORE INTO permissions (permission_code, permission_name, parent_id, resource_type, sort_order, status, created_at)
VALUES ('academic:grade-direction', '年级专业方向', @academic_id, 1, 4, 1, NOW());
SET @academic_gd_id = (SELECT id FROM permissions WHERE permission_code = 'academic:grade-direction' AND deleted = 0 LIMIT 1);

INSERT IGNORE INTO permissions (permission_code, permission_name, parent_id, resource_type, sort_order, status, created_at)
VALUES ('academic:grade-direction:view', '查看年级专业方向', @academic_gd_id, 2, 1, 1, NOW());
INSERT IGNORE INTO permissions (permission_code, permission_name, parent_id, resource_type, sort_order, status, created_at)
VALUES ('academic:grade-direction:edit', '编辑年级专业方向', @academic_gd_id, 2, 2, 1, NOW());

-- 3. Student domain: add student:grade permissions (under student:manage, id=21)

INSERT IGNORE INTO permissions (permission_code, permission_name, parent_id, resource_type, sort_order, status, created_at)
VALUES ('student:grade', '年级管理', 21, 1, 1, 1, NOW());
SET @student_grade_id = (SELECT id FROM permissions WHERE permission_code = 'student:grade' AND deleted = 0 LIMIT 1);

INSERT IGNORE INTO permissions (permission_code, permission_name, parent_id, resource_type, sort_order, status, created_at)
VALUES ('student:grade:view', '查看年级', @student_grade_id, 2, 1, 1, NOW());
INSERT IGNORE INTO permissions (permission_code, permission_name, parent_id, resource_type, sort_order, status, created_at)
VALUES ('student:grade:edit', '编辑年级', @student_grade_id, 2, 2, 1, NOW());

-- 4. Grant all new academic and student:grade permissions to admin role (role_id=1)
INSERT IGNORE INTO role_permissions (role_id, permission_id, created_at, tenant_id)
SELECT 1, id, NOW(), 1 FROM permissions
WHERE permission_code LIKE 'academic%' AND deleted = 0
  AND id NOT IN (SELECT permission_id FROM role_permissions WHERE role_id = 1);

INSERT IGNORE INTO role_permissions (role_id, permission_id, created_at, tenant_id)
SELECT 1, id, NOW(), 1 FROM permissions
WHERE permission_code LIKE 'student:grade%' AND deleted = 0
  AND id NOT IN (SELECT permission_id FROM role_permissions WHERE role_id = 1);

-- 5. Soft-delete old permission codes that are now superseded
-- (We keep them as deleted=1 so old role_permissions rows don't break FK constraints)
UPDATE permissions SET deleted = 1, updated_at = NOW()
WHERE permission_code IN (
    'major:manage', 'major:list', 'major:add', 'major:edit', 'major:delete',
    'major:info', 'major:direction', 'major:direction:list', 'major:direction:info',
    'major:direction:add', 'major:direction:edit', 'major:direction:delete'
) AND deleted = 0;

UPDATE permissions SET deleted = 1, updated_at = NOW()
WHERE permission_code IN (
    'grade:direction', 'grade:direction:list', 'grade:direction:info',
    'grade:direction:add', 'grade:direction:edit', 'grade:direction:delete'
) AND deleted = 0;

-- Soft-delete the old 'grade' node under student:manage (id=1992425216839577865)
-- since it's being replaced by 'student:grade'
UPDATE permissions SET deleted = 1, updated_at = NOW()
WHERE permission_code = 'grade' AND deleted = 0;

-- Soft-delete old quantification:grade:* permissions
UPDATE permissions SET deleted = 1, updated_at = NOW()
WHERE permission_code LIKE 'quantification:grade:%' AND deleted = 0;
