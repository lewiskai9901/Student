-- =====================================================
-- 任务管理模块 - 角色权限配置
-- 创建日期: 2025-12-27
-- 说明: 为不同角色配置任务管理权限，并为测试账号分配角色
-- =====================================================

SET NAMES utf8mb4;

-- =====================================================
-- 第一部分: 为角色分配任务管理权限
-- =====================================================

-- 学校管理员角色权限 (可以创建任务、审批任务、管理任务)
INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT 2, id FROM permissions WHERE permission_code IN (
    'task:menu',           -- 任务管理菜单
    'task:list',           -- 任务列表页
    'task:my',             -- 我的任务页
    'task:approval',       -- 任务审批页
    'task:view',           -- 查看任务
    'task:create',         -- 创建任务
    'task:execute',        -- 执行任务
    'task:approve',        -- 审批任务
    'task:manage',         -- 管理任务
    'task:workflow:manage' -- 流程管理
);

-- 教务主任角色权限 (可以创建任务、审批任务)
INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT 3, id FROM permissions WHERE permission_code IN (
    'task:menu',           -- 任务管理菜单
    'task:list',           -- 任务列表页
    'task:my',             -- 我的任务页
    'task:approval',       -- 任务审批页
    'task:view',           -- 查看任务
    'task:create',         -- 创建任务
    'task:execute',        -- 执行任务
    'task:approve',        -- 审批任务
    'task:manage'          -- 管理任务
);

-- 年级主任角色权限 (可以创建任务、审批任务)
INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT 4, id FROM permissions WHERE permission_code IN (
    'task:menu',           -- 任务管理菜单
    'task:list',           -- 任务列表页
    'task:my',             -- 我的任务页
    'task:approval',       -- 任务审批页
    'task:view',           -- 查看任务
    'task:create',         -- 创建任务
    'task:execute',        -- 执行任务
    'task:approve'         -- 审批任务
);

-- 班主任角色权限 (可以执行任务、提交材料)
INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT 5, id FROM permissions WHERE permission_code IN (
    'task:menu',           -- 任务管理菜单
    'task:list',           -- 任务列表页
    'task:my',             -- 我的任务页
    'task:view',           -- 查看任务
    'task:execute'         -- 执行任务
);

-- =====================================================
-- 第二部分: 为测试账号分配角色
-- =====================================================

-- 清空测试账号的旧角色
DELETE FROM `user_roles` WHERE user_id IN (1001, 2001, 3001, 1101, 1102, 1103, 1104);

-- 学工处领导 -> 学校管理员 + 年级主任
INSERT INTO `user_roles` (`user_id`, `role_id`) VALUES
(1001, 2),  -- 学校管理员
(1001, 4);  -- 年级主任

-- 系部主任 -> 教务主任
INSERT INTO `user_roles` (`user_id`, `role_id`) VALUES
(2001, 3);  -- 教务主任

-- 校领导 -> 学校管理员
INSERT INTO `user_roles` (`user_id`, `role_id`) VALUES
(3001, 2);  -- 学校管理员

-- 班主任1-4 -> 班主任
INSERT INTO `user_roles` (`user_id`, `role_id`) VALUES
(1101, 5),  -- 班主任
(1102, 5),  -- 班主任
(1103, 5),  -- 班主任
(1104, 5);  -- 班主任

-- =====================================================
-- 第三部分: 验证配置结果
-- =====================================================

-- 查看角色权限配置
SELECT '=== 角色权限配置 ===' as info;
SELECT
    r.role_name as '角色',
    COUNT(DISTINCT rp.permission_id) as '权限数量',
    GROUP_CONCAT(DISTINCT p.permission_name SEPARATOR ', ') as '任务相关权限'
FROM roles r
LEFT JOIN role_permissions rp ON r.id = rp.role_id
LEFT JOIN permissions p ON rp.permission_id = p.id AND p.permission_code LIKE 'task%'
WHERE r.id IN (2, 3, 4, 5)
GROUP BY r.id, r.role_name
ORDER BY r.id;

-- 查看测试账号角色分配
SELECT '=== 测试账号角色分配 ===' as info;
SELECT
    u.id as 'ID',
    u.username as '用户名',
    u.real_name as '姓名',
    GROUP_CONCAT(r.role_name SEPARATOR ', ') as '角色'
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
LEFT JOIN roles r ON ur.role_id = r.id
WHERE u.id IN (1001, 2001, 3001, 1101, 1102, 1103, 1104)
GROUP BY u.id, u.username, u.real_name
ORDER BY u.id;

-- =====================================================
-- 说明:
-- 1. 学校管理员: 拥有所有任务管理权限(创建、审批、管理、流程管理)
-- 2. 教务主任: 拥有创建、审批、管理权限
-- 3. 年级主任: 拥有创建、审批权限
-- 4. 班主任: 拥有查看、执行权限
-- 5. 测试账号已按角色分配对应权限
-- =====================================================
