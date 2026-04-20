-- =====================================================
-- 任务管理模块 - EDU 插件基础权限绑定
-- 创建日期: 2026-04-20 (W2.3 从 core seed 拆出)
-- 说明: EDU 插件启用时执行, 给 role 2/3/4/5 补充分配
--       student:* 与 quantification:* 权限
--       core 通用权限见 database/init/task_basic_permissions.sql
-- 前置: 确保 EDU 对应 permissions 行已 seed 到 permissions 表
-- =====================================================

SET NAMES utf8mb4;

-- =====================================================
-- 为学校管理员角色补充 EDU 权限 (role_id = 2)
-- =====================================================

INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT 2, id FROM permissions WHERE permission_code IN (
    -- 学生部门相关
    'student:department',        -- 学生部门管理
    'student:department:view',   -- 查看学生部门

    -- 班级相关
    'student:class',             -- 班级管理
    'student:class:view',        -- 查看班级

    -- 年级相关
    'quantification:grade:view', -- 年级查看

    -- 学生信息
    'student:info',              -- 学生信息
    'student:info:view'          -- 查看学生
);

-- =====================================================
-- 为教务主任角色补充 EDU 权限 (role_id = 3)
-- =====================================================

INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT 3, id FROM permissions WHERE permission_code IN (
    -- 学生部门相关
    'student:department:view',   -- 查看学生部门

    -- 班级相关
    'student:class',             -- 班级管理
    'student:class:view',        -- 查看班级

    -- 年级相关
    'quantification:grade:view', -- 年级查看

    -- 学生信息
    'student:info:view'          -- 查看学生
);

-- =====================================================
-- 为年级主任角色补充 EDU 权限 (role_id = 4)
-- =====================================================

INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT 4, id FROM permissions WHERE permission_code IN (
    -- 学生部门相关
    'student:department:view',   -- 查看学生部门

    -- 班级相关
    'student:class:view',        -- 查看班级

    -- 年级相关
    'quantification:grade:view', -- 年级查看

    -- 学生信息
    'student:info:view'          -- 查看学生
);

-- =====================================================
-- 为班主任角色添加 EDU 权限 (role_id = 5)
-- 注: 班主任所有基础权限均为 EDU 专属
-- =====================================================

INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT 5, id FROM permissions WHERE permission_code IN (
    -- 学生部门相关
    'student:department:view',   -- 查看学生部门

    -- 班级相关
    'student:class:view',        -- 查看班级

    -- 学生信息
    'student:info',              -- 学生信息
    'student:info:view',         -- 查看学生
    'student:info:list'          -- 学生列表
);

-- =====================================================
-- 验证配置结果
-- =====================================================

SELECT '=== EDU 基础权限配置完成 ===' as info;

-- 统计各角色的 EDU 权限数量
SELECT
    r.role_name as '角色',
    COUNT(DISTINCT CASE WHEN p.permission_code LIKE 'student%' THEN p.id END) as 'student 权限',
    COUNT(DISTINCT CASE WHEN p.permission_code LIKE 'quantification%' THEN p.id END) as 'quantification 权限'
FROM roles r
LEFT JOIN role_permissions rp ON r.id = rp.role_id
LEFT JOIN permissions p ON rp.permission_id = p.id
WHERE r.id IN (2, 3, 4, 5)
GROUP BY r.id, r.role_name
ORDER BY r.id;

-- =====================================================
-- 说明:
-- 1. 仅在 EDU 插件启用时执行
-- 2. 未装 EDU 的部署不应执行本文件 (对应 permissions 行不存在,
--    INSERT IGNORE 会静默跳过, 但仍建议按启用状态有条件执行)
-- 3. core 通用权限绑定见 database/init/task_basic_permissions.sql
-- =====================================================
