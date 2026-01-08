-- =====================================================
-- 验证所有测试账号权限
-- =====================================================

SET NAMES utf8mb4;

-- 1. 查看所有测试账号的角色
SELECT '======== 测试账号角色分配 ========' as info;
SELECT
    u.id,
    u.username as '用户名',
    u.real_name as '姓名',
    GROUP_CONCAT(r.role_name SEPARATOR ', ') as '角色'
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
LEFT JOIN roles r ON ur.role_id = r.id
WHERE u.id IN (1001, 2001, 3001, 1101, 1102, 1103, 1104)
GROUP BY u.id, u.username, u.real_name
ORDER BY u.id;

-- 2. 查看各角色的权限统计
SELECT '======== 角色权限统计 ========' as info;
SELECT
    r.role_name as '角色',
    COUNT(DISTINCT rp.permission_id) as '总权限数',
    COUNT(DISTINCT CASE WHEN p.permission_code LIKE 'task%' THEN p.id END) as '任务权限',
    COUNT(DISTINCT CASE WHEN p.permission_code LIKE 'workflow%' THEN p.id END) as '流程权限',
    COUNT(DISTINCT CASE WHEN p.permission_code LIKE '%:view' THEN p.id END) as '查看权限'
FROM roles r
LEFT JOIN role_permissions rp ON r.id = rp.role_id
LEFT JOIN permissions p ON rp.permission_id = p.id
WHERE r.id IN (2, 3, 4, 5)
GROUP BY r.id, r.role_name
ORDER BY r.id;

-- 3. 查看学工处领导的详细权限
SELECT '======== 学工处领导 (xuegong_leader) 权限明细 ========' as info;
SELECT DISTINCT
    CASE
        WHEN p.permission_code LIKE 'task%' THEN '任务管理'
        WHEN p.permission_code LIKE 'workflow%' THEN '流程管理'
        WHEN p.permission_code LIKE '%department%' THEN '部门管理'
        WHEN p.permission_code LIKE '%class%' THEN '班级管理'
        WHEN p.permission_code LIKE '%user%' THEN '用户管理'
        WHEN p.permission_code LIKE '%student%' THEN '学生管理'
        ELSE '其他'
    END as '权限分类',
    p.permission_code as '权限代码',
    p.permission_name as '权限名称'
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN role_permissions rp ON ur.role_id = rp.role_id
JOIN permissions p ON rp.permission_id = p.id
WHERE u.username = 'xuegong_leader'
  AND (
    p.permission_code LIKE 'task%'
    OR p.permission_code LIKE 'workflow%'
    OR p.permission_code LIKE '%:department%'
    OR p.permission_code LIKE '%:class%'
    OR p.permission_code LIKE '%:user%'
    OR p.permission_code LIKE '%:student%'
    OR p.permission_code LIKE 'quantification:grade%'
  )
ORDER BY 1, p.permission_code;

-- 4. 检查关键权限是否配置
SELECT '======== 关键权限检查 ========' as info;
SELECT
    p.permission_code as '权限代码',
    p.permission_name as '权限名称',
    GROUP_CONCAT(r.role_name SEPARATOR ', ') as '拥有此权限的角色'
FROM permissions p
LEFT JOIN role_permissions rp ON p.id = rp.permission_id
LEFT JOIN roles r ON rp.role_id = r.id AND r.id IN (2, 3, 4, 5)
WHERE p.permission_code IN (
    'task:menu',
    'task:create',
    'task:execute',
    'task:approve',
    'workflow:view',
    'system:department:view',
    'student:class:view',
    'system:user:list'
)
GROUP BY p.id, p.permission_code, p.permission_name
ORDER BY p.permission_code;

-- 5. 验证密码
SELECT '======== 密码验证 ========' as info;
SELECT
    u.id,
    u.username as '用户名',
    CASE
        WHEN u.password = (SELECT password FROM users WHERE username = 'admin')
        THEN '✓ 密码正确 (与admin相同)'
        ELSE '✗ 密码错误'
    END as '密码状态'
FROM users u
WHERE u.id IN (1001, 2001, 3001, 1101, 1102, 1103, 1104)
ORDER BY u.id;

-- 完成
SELECT '======== 验证完成 ========' as info;
SELECT '所有测试账号权限配置已验证完成！' as message;
