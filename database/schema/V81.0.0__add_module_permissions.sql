-- =====================================================
-- V81.0.0 - Add permissions for Teaching, Student, and Asset modules
-- Date: 2026-04-01
-- Description: Inserts permission records for all new controllers
--              that now use @CasbinAccess annotations
-- =====================================================

SET NAMES utf8mb4;

-- =====================================================
-- 1. Teaching Module Permissions
-- =====================================================

-- Teaching Calendar (academic years, semesters, weeks, events)
INSERT IGNORE INTO permissions (id, permission_code, permission_name, parent_id, permission_type, sort_order, status, deleted)
VALUES
(8101, 'teaching:calendar', '教学日历管理', 0, 1, 200, 1, 0),
(8102, 'teaching:calendar:view', '查看教学日历', 8101, 3, 1, 1, 0),
(8103, 'teaching:calendar:edit', '编辑教学日历', 8101, 3, 2, 1, 0);

-- Teaching Course
INSERT IGNORE INTO permissions (id, permission_code, permission_name, parent_id, permission_type, sort_order, status, deleted)
VALUES
(8111, 'teaching:course', '课程管理', 0, 1, 201, 1, 0),
(8112, 'teaching:course:view', '查看课程', 8111, 3, 1, 1, 0),
(8113, 'teaching:course:edit', '编辑课程', 8111, 3, 2, 1, 0);

-- Teaching Curriculum
INSERT IGNORE INTO permissions (id, permission_code, permission_name, parent_id, permission_type, sort_order, status, deleted)
VALUES
(8121, 'teaching:curriculum', '培养方案管理', 0, 1, 202, 1, 0),
(8122, 'teaching:curriculum:view', '查看培养方案', 8121, 3, 1, 1, 0),
(8123, 'teaching:curriculum:edit', '编辑培养方案', 8121, 3, 2, 1, 0);

-- Teaching Task
INSERT IGNORE INTO permissions (id, permission_code, permission_name, parent_id, permission_type, sort_order, status, deleted)
VALUES
(8131, 'teaching:task', '教学任务管理', 0, 1, 203, 1, 0),
(8132, 'teaching:task:view', '查看教学任务', 8131, 3, 1, 1, 0),
(8133, 'teaching:task:edit', '编辑教学任务', 8131, 3, 2, 1, 0);

-- Teaching Schedule (schedules + adjustments)
INSERT IGNORE INTO permissions (id, permission_code, permission_name, parent_id, permission_type, sort_order, status, deleted)
VALUES
(8141, 'teaching:schedule', '排课管理', 0, 1, 204, 1, 0),
(8142, 'teaching:schedule:view', '查看课表', 8141, 3, 1, 1, 0),
(8143, 'teaching:schedule:edit', '编辑课表/调课', 8141, 3, 2, 1, 0);

-- Teaching Constraint
INSERT IGNORE INTO permissions (id, permission_code, permission_name, parent_id, permission_type, sort_order, status, deleted)
VALUES
(8151, 'teaching:constraint', '排课约束管理', 0, 1, 205, 1, 0),
(8152, 'teaching:constraint:view', '查看排课约束', 8151, 3, 1, 1, 0),
(8153, 'teaching:constraint:edit', '编辑排课约束', 8151, 3, 2, 1, 0);

-- Teaching Offering (开课管理 + 班级选课分配)
INSERT IGNORE INTO permissions (id, permission_code, permission_name, parent_id, permission_type, sort_order, status, deleted)
VALUES
(8161, 'teaching:offering', '开课管理', 0, 1, 206, 1, 0),
(8162, 'teaching:offering:view', '查看开课计划', 8161, 3, 1, 1, 0),
(8163, 'teaching:offering:edit', '编辑开课计划', 8161, 3, 2, 1, 0);

-- Teaching Class (教学班)
INSERT IGNORE INTO permissions (id, permission_code, permission_name, parent_id, permission_type, sort_order, status, deleted)
VALUES
(8171, 'teaching:class', '教学班管理', 0, 1, 207, 1, 0),
(8172, 'teaching:class:view', '查看教学班', 8171, 3, 1, 1, 0),
(8173, 'teaching:class:edit', '编辑教学班', 8171, 3, 2, 1, 0);

-- Teaching Exam
INSERT IGNORE INTO permissions (id, permission_code, permission_name, parent_id, permission_type, sort_order, status, deleted)
VALUES
(8181, 'teaching:exam', '考试管理', 0, 1, 208, 1, 0),
(8182, 'teaching:exam:view', '查看考试', 8181, 3, 1, 1, 0),
(8183, 'teaching:exam:edit', '编辑考试', 8181, 3, 2, 1, 0);

-- Teaching Grade
INSERT IGNORE INTO permissions (id, permission_code, permission_name, parent_id, permission_type, sort_order, status, deleted)
VALUES
(8191, 'teaching:grade', '成绩管理', 0, 1, 209, 1, 0),
(8192, 'teaching:grade:view', '查看成绩', 8191, 3, 1, 1, 0),
(8193, 'teaching:grade:edit', '编辑成绩', 8191, 3, 2, 1, 0);

-- =====================================================
-- 2. Student Module Permissions
-- =====================================================

-- Student Attendance
INSERT IGNORE INTO permissions (id, permission_code, permission_name, parent_id, permission_type, sort_order, status, deleted)
VALUES
(8201, 'student:attendance', '考勤管理', 0, 1, 210, 1, 0),
(8202, 'student:attendance:view', '查看考勤', 8201, 3, 1, 1, 0),
(8203, 'student:attendance:edit', '编辑考勤', 8201, 3, 2, 1, 0);

-- Student Academic Warning
INSERT IGNORE INTO permissions (id, permission_code, permission_name, parent_id, permission_type, sort_order, status, deleted)
VALUES
(8211, 'student:warning', '学业预警管理', 0, 1, 211, 1, 0),
(8212, 'student:warning:view', '查看学业预警', 8211, 3, 1, 1, 0),
(8213, 'student:warning:edit', '编辑学业预警', 8211, 3, 2, 1, 0);

-- =====================================================
-- 3. Asset Module Permissions
-- =====================================================

-- Asset Management (core CRUD, categories, codes, depreciation, excel, alerts)
INSERT IGNORE INTO permissions (id, permission_code, permission_name, parent_id, permission_type, sort_order, status, deleted)
VALUES
(8301, 'asset:manage', '资产管理', 0, 1, 300, 1, 0),
(8302, 'asset:manage:view', '查看资产', 8301, 3, 1, 1, 0),
(8303, 'asset:manage:edit', '编辑资产', 8301, 3, 2, 1, 0);

-- Asset Borrow
INSERT IGNORE INTO permissions (id, permission_code, permission_name, parent_id, permission_type, sort_order, status, deleted)
VALUES
(8311, 'asset:borrow', '资产借用管理', 0, 1, 301, 1, 0),
(8312, 'asset:borrow:view', '查看资产借用', 8311, 3, 1, 1, 0),
(8313, 'asset:borrow:edit', '编辑资产借用', 8311, 3, 2, 1, 0);

-- Asset Inventory (stocktaking)
INSERT IGNORE INTO permissions (id, permission_code, permission_name, parent_id, permission_type, sort_order, status, deleted)
VALUES
(8321, 'asset:inventory', '资产盘点管理', 0, 1, 302, 1, 0),
(8322, 'asset:inventory:view', '查看资产盘点', 8321, 3, 1, 1, 0),
(8323, 'asset:inventory:edit', '编辑资产盘点', 8321, 3, 2, 1, 0);

-- Asset Approval
INSERT IGNORE INTO permissions (id, permission_code, permission_name, parent_id, permission_type, sort_order, status, deleted)
VALUES
(8331, 'asset:approval', '资产审批管理', 0, 1, 303, 1, 0),
(8332, 'asset:approval:view', '查看资产审批', 8331, 3, 1, 1, 0),
(8333, 'asset:approval:edit', '审批资产申请', 8331, 3, 2, 1, 0);

-- =====================================================
-- 4. Grant all new permissions to admin role (role_id = 1)
-- =====================================================

INSERT IGNORE INTO role_permissions (id, role_id, permission_id)
SELECT id + 900000, 1, id FROM permissions
WHERE permission_code IN (
    'teaching:calendar', 'teaching:calendar:view', 'teaching:calendar:edit',
    'teaching:course', 'teaching:course:view', 'teaching:course:edit',
    'teaching:curriculum', 'teaching:curriculum:view', 'teaching:curriculum:edit',
    'teaching:task', 'teaching:task:view', 'teaching:task:edit',
    'teaching:schedule', 'teaching:schedule:view', 'teaching:schedule:edit',
    'teaching:constraint', 'teaching:constraint:view', 'teaching:constraint:edit',
    'teaching:offering', 'teaching:offering:view', 'teaching:offering:edit',
    'teaching:class', 'teaching:class:view', 'teaching:class:edit',
    'teaching:exam', 'teaching:exam:view', 'teaching:exam:edit',
    'teaching:grade', 'teaching:grade:view', 'teaching:grade:edit',
    'student:attendance', 'student:attendance:view', 'student:attendance:edit',
    'student:warning', 'student:warning:view', 'student:warning:edit',
    'asset:manage', 'asset:manage:view', 'asset:manage:edit',
    'asset:borrow', 'asset:borrow:view', 'asset:borrow:edit',
    'asset:inventory', 'asset:inventory:view', 'asset:inventory:edit',
    'asset:approval', 'asset:approval:view', 'asset:approval:edit'
);
