-- =====================================================
-- 任务管理模块 - core 基础权限绑定
-- 创建日期: 2025-12-27
-- 最近更新: 2026-04-20 (W2.3 拆分 core vs EDU)
-- 说明: 给 role 2/3/4 分配通用核心 (system:*) 权限
--       EDU 相关 (student:* / quantification:*) 已拆到
--       database/init/plugins/education/edu_basic_permissions.sql
--       仅在 EDU 插件启用时执行
-- =====================================================

SET NAMES utf8mb4;

-- =====================================================
-- 为学校管理员角色添加基础权限 (role_id = 2)
-- =====================================================

INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT 2, id FROM permissions WHERE permission_code IN (
    -- 部门相关
    'system:department',        -- 部门管理
    'system:department:view',   -- 查看部门

    -- 用户相关
    'system:user',              -- 用户管理
    'system:user:view',         -- 查看用户
    'system:user:list',         -- 用户列表

    -- 角色相关
    'system:role',              -- 角色管理
    'system:role:view'          -- 查看角色
);

-- =====================================================
-- 为教务主任角色添加基础权限 (role_id = 3)
-- =====================================================

INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT 3, id FROM permissions WHERE permission_code IN (
    -- 部门相关
    'system:department:view',   -- 查看部门

    -- 用户相关
    'system:user:view',         -- 查看用户
    'system:user:list'          -- 用户列表
);

-- =====================================================
-- 为年级主任角色添加基础权限 (role_id = 4)
-- =====================================================

INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT 4, id FROM permissions WHERE permission_code IN (
    -- 部门相关
    'system:department:view',   -- 查看部门

    -- 用户相关
    'system:user:view',         -- 查看用户
    'system:user:list'          -- 用户列表
);

-- =====================================================
-- 注: role 5 (班主任) 所需权限全部为 EDU 插件专属
--     (student:department:view / student:class:view / student:info*),
--     因此 core seed 不给 role 5 分配任何权限.
--     见 database/init/plugins/education/edu_basic_permissions.sql
-- =====================================================

-- =====================================================
-- 验证配置结果
-- =====================================================

SELECT '=== core 基础权限配置完成 ===' as info;

-- 统计各角色的权限数量
SELECT
    r.role_name as '角色',
    COUNT(DISTINCT rp.permission_id) as '总权限数',
    COUNT(DISTINCT CASE WHEN p.permission_code LIKE 'task%' THEN p.id END) as '任务权限',
    COUNT(DISTINCT CASE WHEN p.permission_code LIKE 'system%' THEN p.id END) as 'core 基础权限'
FROM roles r
LEFT JOIN role_permissions rp ON r.id = rp.role_id
LEFT JOIN permissions p ON rp.permission_id = p.id
WHERE r.id IN (2, 3, 4, 5)
GROUP BY r.id, r.role_name
ORDER BY r.id;

-- =====================================================
-- 说明:
-- 1. 仅包含 core (system:*) 基础查看权限
-- 2. 这些是任务管理中选择执行人时需要的基础权限
-- 3. EDU 插件 (student:* / quantification:*) 的补充绑定
--    见同目录 plugins/education/edu_basic_permissions.sql
-- =====================================================
