-- 插入专业管理权限
INSERT INTO permissions (permission_code, permission_name, parent_id, resource_type, path, sort_order, icon, permission_desc) VALUES
('major', '专业管理', 0, 1, '/majors', 40, 'Reading', '专业管理模块'),
('major:list', '查看专业列表', (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'major') AS temp), 2, NULL, 1, NULL, '查看专业列表权限'),
('major:info', '查看专业详情', (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'major') AS temp), 2, NULL, 2, NULL, '查看专业详情权限'),
('major:add', '新增专业', (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'major') AS temp), 2, NULL, 3, NULL, '新增专业权限'),
('major:edit', '编辑专业', (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'major') AS temp), 2, NULL, 4, NULL, '编辑专业权限'),
('major:delete', '删除专业', (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'major') AS temp), 2, NULL, 5, NULL, '删除专业权限');

-- 为超级管理员角色(role_id=1)分配专业管理权限
INSERT INTO role_permissions (role_id, permission_id)
SELECT 1, id FROM permissions WHERE permission_code IN ('major', 'major:list', 'major:info', 'major:add', 'major:edit', 'major:delete');
