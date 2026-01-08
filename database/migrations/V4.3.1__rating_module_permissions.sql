-- ==========================================
-- 评级模块权限数据初始化脚本
-- ==========================================
-- 功能: 插入评级统计、荣誉徽章、通报生成相关权限
-- 作者: Claude Code
-- 日期: 2025-12-22
-- 版本: 4.3.1
-- ==========================================

SET NAMES utf8mb4;
SET CHARACTER_SET_CLIENT = utf8mb4;

USE student_management;

-- ==========================================
-- 1. 添加评级统计权限（基于现有 quantification:rating）
-- ==========================================

-- 检查是否存在父级权限 quantification:rating
SET @rating_parent_id = (SELECT id FROM permissions WHERE permission_code = 'quantification:rating' AND deleted = 0 LIMIT 1);

-- 如果不存在，先创建 quantification:rating 权限
-- 获取 quantification 模块ID
SET @quantification_module_id = (SELECT id FROM permissions WHERE permission_code = 'quantification' AND deleted = 0 LIMIT 1);

-- 如果 quantification:rating 不存在，创建它
INSERT INTO permissions (permission_code, permission_name, parent_id, resource_type, permission_desc, sort_order, status, created_at, updated_at, deleted)
SELECT 'quantification:rating', '评级管理', @quantification_module_id, 1, '评级规则和统计管理', 10, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_code = 'quantification:rating' AND deleted = 0);

-- 重新获取 quantification:rating ID
SET @rating_parent_id = (SELECT id FROM permissions WHERE permission_code = 'quantification:rating' AND deleted = 0 LIMIT 1);

-- ==========================================
-- 2. 添加评级统计相关权限
-- ==========================================

-- 检查并添加：查看评级统计
INSERT INTO permissions (permission_code, permission_name, parent_id, resource_type, permission_desc, sort_order, status, created_at, updated_at, deleted)
SELECT 'quantification:rating:view', '查看评级统计', @rating_parent_id, 2, '查看评级频次统计和趋势', 1, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_code = 'quantification:rating:view' AND deleted = 0);

-- 检查并添加：导出评级统计
INSERT INTO permissions (permission_code, permission_name, parent_id, resource_type, permission_desc, sort_order, status, created_at, updated_at, deleted)
SELECT 'quantification:rating:export', '导出评级报表', @rating_parent_id, 2, '导出评级统计报表', 2, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_code = 'quantification:rating:export' AND deleted = 0);

-- 检查并添加：刷新统计数据
INSERT INTO permissions (permission_code, permission_name, parent_id, resource_type, permission_desc, sort_order, status, created_at, updated_at, deleted)
SELECT 'quantification:rating:refresh', '刷新统计数据', @rating_parent_id, 2, '重新计算评级统计', 3, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_code = 'quantification:rating:refresh' AND deleted = 0);

-- ==========================================
-- 3. 添加荣誉徽章相关权限
-- ==========================================

-- 添加徽章管理父级权限
INSERT INTO permissions (permission_code, permission_name, parent_id, resource_type, permission_desc, sort_order, status, created_at, updated_at, deleted)
SELECT 'quantification:badge', '荣誉徽章管理', @quantification_module_id, 1, '荣誉徽章配置和授予', 11, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_code = 'quantification:badge' AND deleted = 0);

SET @badge_parent_id = (SELECT id FROM permissions WHERE permission_code = 'quantification:badge' AND deleted = 0 LIMIT 1);

-- 添加徽章管理操作权限
INSERT INTO permissions (permission_code, permission_name, parent_id, resource_type, permission_desc, sort_order, status, created_at, updated_at, deleted)
SELECT 'quantification:badge:manage', '管理徽章配置', @badge_parent_id, 2, '创建、编辑、删除徽章配置', 1, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_code = 'quantification:badge:manage' AND deleted = 0);

INSERT INTO permissions (permission_code, permission_name, parent_id, resource_type, permission_desc, sort_order, status, created_at, updated_at, deleted)
SELECT 'quantification:badge:grant', '授予徽章', @badge_parent_id, 2, '手动或自动授予徽章', 2, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_code = 'quantification:badge:grant' AND deleted = 0);

INSERT INTO permissions (permission_code, permission_name, parent_id, resource_type, permission_desc, sort_order, status, created_at, updated_at, deleted)
SELECT 'quantification:badge:revoke', '撤销徽章', @badge_parent_id, 2, '撤销已授予的徽章', 3, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_code = 'quantification:badge:revoke' AND deleted = 0);

INSERT INTO permissions (permission_code, permission_name, parent_id, resource_type, permission_desc, sort_order, status, created_at, updated_at, deleted)
SELECT 'quantification:badge:view', '查看徽章记录', @badge_parent_id, 2, '查看班级徽章记录', 4, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_code = 'quantification:badge:view' AND deleted = 0);

-- ==========================================
-- 4. 添加通报生成相关权限
-- ==========================================

-- 添加通报管理父级权限
INSERT INTO permissions (permission_code, permission_name, parent_id, resource_type, permission_desc, sort_order, status, created_at, updated_at, deleted)
SELECT 'quantification:notification', '评级通报管理', @quantification_module_id, 1, '评级通报和证书生成', 12, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_code = 'quantification:notification' AND deleted = 0);

SET @notification_parent_id = (SELECT id FROM permissions WHERE permission_code = 'quantification:notification' AND deleted = 0 LIMIT 1);

-- 添加通报生成操作权限
INSERT INTO permissions (permission_code, permission_name, parent_id, resource_type, permission_desc, sort_order, status, created_at, updated_at, deleted)
SELECT 'quantification:notification:generate', '生成通报', @notification_parent_id, 2, '生成荣誉通报、预警通报', 1, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_code = 'quantification:notification:generate' AND deleted = 0);

INSERT INTO permissions (permission_code, permission_name, parent_id, resource_type, permission_desc, sort_order, status, created_at, updated_at, deleted)
SELECT 'quantification:notification:certificate', '生成证书', @notification_parent_id, 2, '批量生成荣誉证书', 2, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_code = 'quantification:notification:certificate' AND deleted = 0);

INSERT INTO permissions (permission_code, permission_name, parent_id, resource_type, permission_desc, sort_order, status, created_at, updated_at, deleted)
SELECT 'quantification:notification:publish', '发布通报', @notification_parent_id, 2, '发布或撤销通报', 3, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_code = 'quantification:notification:publish' AND deleted = 0);

INSERT INTO permissions (permission_code, permission_name, parent_id, resource_type, permission_desc, sort_order, status, created_at, updated_at, deleted)
SELECT 'quantification:notification:view', '查看通报历史', @notification_parent_id, 2, '查看历史通报记录', 4, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_code = 'quantification:notification:view' AND deleted = 0);

-- ==========================================
-- 5. 为管理员角色授予所有新权限
-- ==========================================

-- 获取管理员角色ID（假设角色名称为'ADMIN'或'管理员'）
SET @admin_role_id = (SELECT id FROM roles WHERE role_name IN ('ADMIN', '管理员', 'admin') AND deleted = 0 LIMIT 1);

-- 如果存在管理员角色，授予所有新权限
INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT @admin_role_id, id, NOW()
FROM permissions
WHERE permission_code IN (
    'quantification:rating:view',
    'quantification:rating:export',
    'quantification:rating:refresh',
    'quantification:badge',
    'quantification:badge:manage',
    'quantification:badge:grant',
    'quantification:badge:revoke',
    'quantification:badge:view',
    'quantification:notification',
    'quantification:notification:generate',
    'quantification:notification:certificate',
    'quantification:notification:publish',
    'quantification:notification:view'
)
AND deleted = 0
AND @admin_role_id IS NOT NULL
AND NOT EXISTS (
    SELECT 1 FROM role_permissions
    WHERE role_id = @admin_role_id
    AND permission_id = permissions.id
);

-- ==========================================
-- 完成提示
-- ==========================================

SELECT '评级模块权限初始化完成！' AS message;
SELECT COUNT(*) AS '新增权限数量' FROM permissions WHERE permission_code LIKE 'quantification:rating%' OR permission_code LIKE 'quantification:badge%' OR permission_code LIKE 'quantification:notification%';
