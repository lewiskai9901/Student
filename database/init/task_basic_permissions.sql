-- =====================================================
-- 任务管理模块 - 补充基础权限
-- 创建日期: 2025-12-27
-- 说明: 为测试账号添加必要的基础查看权限，解决403错误
-- =====================================================

SET NAMES utf8mb4;

-- =====================================================
-- 为学校管理员角色添加基础权限
-- =====================================================

INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT 2, id FROM permissions WHERE permission_code IN (
    -- 部门相关
    'system:department',        -- 部门管理
    'system:department:view',   -- 查看部门
    'student:department',       -- 学生部门管理
    'student:department:view',  -- 查看学生部门

    -- 班级相关
    'student:class',            -- 班级管理
    'student:class:view',       -- 查看班级

    -- 年级相关
    'quantification:grade:view', -- 年级查看

    -- 用户相关
    'system:user',              -- 用户管理
    'system:user:view',         -- 查看用户
    'system:user:list',         -- 用户列表

    -- 角色相关
    'system:role',              -- 角色管理
    'system:role:view',         -- 查看角色

    -- 学生相关
    'student:info',             -- 学生信息
    'student:info:view'         -- 查看学生
);

-- =====================================================
-- 为教务主任角色添加基础权限
-- =====================================================

INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT 3, id FROM permissions WHERE permission_code IN (
    -- 部门相关
    'system:department:view',   -- 查看部门
    'student:department:view',  -- 查看学生部门

    -- 班级相关
    'student:class',            -- 班级管理
    'student:class:view',       -- 查看班级

    -- 年级相关
    'quantification:grade:view', -- 年级查看

    -- 用户相关
    'system:user:view',         -- 查看用户
    'system:user:list',         -- 用户列表

    -- 学生相关
    'student:info:view'         -- 查看学生
);

-- =====================================================
-- 为年级主任角色添加基础权限
-- =====================================================

INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT 4, id FROM permissions WHERE permission_code IN (
    -- 部门相关
    'system:department:view',   -- 查看部门
    'student:department:view',  -- 查看学生部门

    -- 班级相关
    'student:class:view',       -- 查看班级

    -- 年级相关
    'quantification:grade:view', -- 年级查看

    -- 用户相关
    'system:user:view',         -- 查看用户
    'system:user:list',         -- 用户列表

    -- 学生相关
    'student:info:view'         -- 查看学生
);

-- =====================================================
-- 为班主任角色添加基础权限
-- =====================================================

INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT 5, id FROM permissions WHERE permission_code IN (
    -- 部门相关
    'student:department:view',  -- 查看学生部门

    -- 班级相关
    'student:class:view',       -- 查看班级

    -- 学生相关
    'student:info',             -- 学生信息
    'student:info:view',        -- 查看学生
    'student:info:list'         -- 学生列表
);

-- =====================================================
-- 验证配置结果
-- =====================================================

SELECT '=== 基础权限配置完成 ===' as info;

-- 统计各角色的权限数量
SELECT
    r.role_name as '角色',
    COUNT(DISTINCT rp.permission_id) as '总权限数',
    COUNT(DISTINCT CASE WHEN p.permission_code LIKE 'task%' THEN p.id END) as '任务权限',
    COUNT(DISTINCT CASE WHEN p.permission_code LIKE 'student%' OR p.permission_code LIKE 'system%' THEN p.id END) as '基础权限'
FROM roles r
LEFT JOIN role_permissions rp ON r.id = rp.role_id
LEFT JOIN permissions p ON rp.permission_id = p.id
WHERE r.id IN (2, 3, 4, 5)
GROUP BY r.id, r.role_name
ORDER BY r.id;

-- =====================================================
-- 说明:
-- 1. 添加了部门、班级、年级、用户的查看权限
-- 2. 这些是任务管理中选择执行人时需要的基础权限
-- 3. 不同角色根据职责分配不同范围的权限
-- =====================================================
