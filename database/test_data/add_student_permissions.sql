-- 添加学生管理所需的权限
-- 运行命令: mysql -u root -p student_management < database/test_data/add_student_permissions.sql

-- 先查询父权限ID (student:info)
SET @parent_id = (SELECT id FROM permissions WHERE permission_code = 'student:info' AND deleted = 0 LIMIT 1);

-- 如果父权限不存在，创建它
INSERT IGNORE INTO permissions (permission_code, permission_name, permission_desc, resource_type, parent_id, sort_order, status, created_at, updated_at)
VALUES ('student:info', '学生信息管理', '学生信息管理菜单', 1, 0, 1, 1, NOW(), NOW());

SET @parent_id = (SELECT id FROM permissions WHERE permission_code = 'student:info' AND deleted = 0 LIMIT 1);

-- 添加学生信息相关的细粒度权限 (resource_type: 1=菜单, 2=按钮, 3=API)
INSERT IGNORE INTO permissions (permission_code, permission_name, permission_desc, resource_type, parent_id, sort_order, status, created_at, updated_at)
VALUES
    ('student:info:view', '查看学生信息', '查看学生列表和详情', 2, @parent_id, 1, 1, NOW(), NOW()),
    ('student:create', '创建学生', '创建新学生', 2, @parent_id, 2, 1, NOW(), NOW()),
    ('student:update', '更新学生', '修改学生信息', 2, @parent_id, 3, 1, NOW(), NOW()),
    ('student:delete', '删除学生', '删除学生', 2, @parent_id, 4, 1, NOW(), NOW()),
    ('student:info:export', '导出学生', '导出学生数据', 2, @parent_id, 5, 1, NOW(), NOW()),
    ('student:info:import', '导入学生', '导入学生数据', 2, @parent_id, 6, 1, NOW(), NOW());

-- 获取超级管理员角色ID
SET @admin_role_id = (SELECT id FROM roles WHERE role_code = 'SUPER_ADMIN' OR role_code = 'super_admin' AND deleted = 0 LIMIT 1);

-- 如果没有找到，尝试使用ID=1（通常是管理员角色）
SET @admin_role_id = COALESCE(@admin_role_id, 1);

-- 为管理员角色添加这些权限
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT @admin_role_id, id FROM permissions
WHERE permission_code IN (
    'student:info',
    'student:info:view',
    'student:create',
    'student:update',
    'student:delete',
    'student:info:export',
    'student:info:import'
) AND deleted = 0;

-- 验证添加结果
SELECT 'Added permissions:' as message;
SELECT permission_code, permission_name FROM permissions
WHERE permission_code LIKE 'student:%' AND deleted = 0;

SELECT 'Role permissions count:' as message;
SELECT COUNT(*) as count FROM role_permissions WHERE role_id = @admin_role_id;
