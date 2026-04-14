-- ============================================================================
-- V20260414_3: Phase 2.3 — 扩展 TEACHER UserType 的 capability features
--               并种子化 5 条 SELF 权限 (my:*)
-- ============================================================================
-- 目的：
--   1. TEACHER features 从 {canLogin, requiresOrg, canTeach} 扩展到包含 5 个细粒度
--      canXxx 能力标志，用于 CapabilityPermissionService 映射到 SELF 权限。
--   2. 在 permissions 表中预置对应的 5 条 SELF 权限，scope='SELF'，用于个人空间
--      路由/UI 按钮级权限控制。
--
-- 约定：features 中的 canXxx key → permission_code 的映射由
--       CapabilityPermissionService 维护（硬编码 Map）。本迁移只负责种子数据。
-- ============================================================================

-- Step 1: 扩展 TEACHER 的 features（保留原有 canLogin/requiresOrg/canTeach）
UPDATE user_types
SET features = '{"canLogin":true,"requiresOrg":true,"canTeach":true,"canViewOwnSchedule":true,"canViewOwnStudents":true,"canRecordAttendance":true,"canBeSubstitute":true,"canSubmitGrades":true}'
WHERE type_code = 'TEACHER';

-- Step 2: 种子化 5 条 SELF 权限
-- 权限编码约定：my:{resource}:{action}
-- 重复执行安全：使用 ON DUPLICATE KEY UPDATE（permission_code 有唯一索引）
INSERT INTO permissions
    (permission_code, permission_name, description, resource, action, permission_type, permission_scope, sort_order, is_enabled, deleted, tenant_id)
VALUES
    ('my:schedule:view',    '查看个人课表',     '教师查看自己任课的课表',        'my:schedule',    'view',    'API', 'SELF', 101, 1, 0, 1),
    ('my:students:view',    '查看所授学生',     '教师查看自己所授班级的学生列表', 'my:students',    'view',    'API', 'SELF', 102, 1, 0, 1),
    ('my:attendance:record','录入考勤',         '教师为自己所授课堂录考勤',       'my:attendance',  'record',  'API', 'SELF', 103, 1, 0, 1),
    ('my:substitute:view',  '查看代课任务',     '教师查看分配给自己的代课任务',   'my:substitute',  'view',    'API', 'SELF', 104, 1, 0, 1),
    ('my:grades:submit',    '提交成绩',         '教师提交/录入自己所授课程的成绩','my:grades',      'submit',  'API', 'SELF', 105, 1, 0, 1)
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name),
    description     = VALUES(description),
    resource        = VALUES(resource),
    action          = VALUES(action),
    permission_type = VALUES(permission_type),
    permission_scope= VALUES(permission_scope),
    sort_order      = VALUES(sort_order),
    is_enabled      = VALUES(is_enabled);
