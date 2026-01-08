-- ====================================
-- 量化管理系统 2.0 - 权限配置
-- ====================================

-- 1. 量化任务管理权限
INSERT INTO permissions (permission_code, permission_name, parent_id, permission_type, sort_order) VALUES
('quantification', '量化管理', NULL, 1, 60),
('quantification:task', '量化任务', (SELECT id FROM permissions WHERE permission_code = 'quantification'), 1, 1),
('quantification:task:view', '查看任务', (SELECT id FROM permissions WHERE permission_code = 'quantification:task'), 2, 1),
('quantification:task:create', '创建任务', (SELECT id FROM permissions WHERE permission_code = 'quantification:task'), 2, 2),
('quantification:task:update', '更新任务', (SELECT id FROM permissions WHERE permission_code = 'quantification:task'), 2, 3),
('quantification:task:delete', '删除任务', (SELECT id FROM permissions WHERE permission_code = 'quantification:task'), 2, 4);

-- 2. 量化检查权限
INSERT INTO permissions (permission_code, permission_name, parent_id, permission_type, sort_order) VALUES
('quantification:check', '量化检查', (SELECT id FROM permissions WHERE permission_code = 'quantification'), 1, 2),
('quantification:check:view', '查看检查', (SELECT id FROM permissions WHERE permission_code = 'quantification:check'), 2, 1),
('quantification:check:create', '创建检查', (SELECT id FROM permissions WHERE permission_code = 'quantification:check'), 2, 2),
('quantification:check:update', '更新检查', (SELECT id FROM permissions WHERE permission_code = 'quantification:check'), 2, 3),
('quantification:check:deduct', '录入扣分', (SELECT id FROM permissions WHERE permission_code = 'quantification:check'), 2, 4),
('quantification:check:delete', '删除扣分', (SELECT id FROM permissions WHERE permission_code = 'quantification:check'), 2, 5);

-- 3. 量化模板权限
INSERT INTO permissions (permission_code, permission_name, parent_id, permission_type, sort_order) VALUES
('quantification:template', '量化模板', (SELECT id FROM permissions WHERE permission_code = 'quantification'), 1, 3),
('quantification:template:view', '查看模板', (SELECT id FROM permissions WHERE permission_code = 'quantification:template'), 2, 1),
('quantification:template:create', '创建模板', (SELECT id FROM permissions WHERE permission_code = 'quantification:template'), 2, 2),
('quantification:template:update', '更新模板', (SELECT id FROM permissions WHERE permission_code = 'quantification:template'), 2, 3),
('quantification:template:delete', '删除模板', (SELECT id FROM permissions WHERE permission_code = 'quantification:template'), 2, 4);

-- 4. 量化汇总权限
INSERT INTO permissions (permission_code, permission_name, parent_id, permission_type, sort_order) VALUES
('quantification:summary', '量化汇总', (SELECT id FROM permissions WHERE permission_code = 'quantification'), 1, 4),
('quantification:summary:view', '查看汇总', (SELECT id FROM permissions WHERE permission_code = 'quantification:summary'), 2, 1),
('quantification:summary:create', '创建汇总', (SELECT id FROM permissions WHERE permission_code = 'quantification:summary'), 2, 2),
('quantification:summary:execute', '执行汇总', (SELECT id FROM permissions WHERE permission_code = 'quantification:summary'), 2, 3),
('quantification:summary:delete', '删除汇总', (SELECT id FROM permissions WHERE permission_code = 'quantification:summary'), 2, 4);

-- 5. 量化公告权限
INSERT INTO permissions (permission_code, permission_name, parent_id, permission_type, sort_order) VALUES
('quantification:announcement', '量化公告', (SELECT id FROM permissions WHERE permission_code = 'quantification'), 1, 5),
('quantification:announcement:view', '查看公告', (SELECT id FROM permissions WHERE permission_code = 'quantification:announcement'), 2, 1),
('quantification:announcement:create', '创建公告', (SELECT id FROM permissions WHERE permission_code = 'quantification:announcement'), 2, 2),
('quantification:announcement:publish', '发布公告', (SELECT id FROM permissions WHERE permission_code = 'quantification:announcement'), 2, 3),
('quantification:announcement:delete', '删除公告', (SELECT id FROM permissions WHERE permission_code = 'quantification:announcement'), 2, 4);

-- 6. 学期管理权限(如果不存在)
INSERT INTO permissions (permission_code, permission_name, parent_id, permission_type, sort_order) VALUES
('system:semester', '学期管理', (SELECT id FROM permissions WHERE permission_code = 'system'), 1, 10),
('system:semester:view', '查看学期', (SELECT id FROM permissions WHERE permission_code = 'system:semester'), 2, 1),
('system:semester:create', '创建学期', (SELECT id FROM permissions WHERE permission_code = 'system:semester'), 2, 2),
('system:semester:update', '更新学期', (SELECT id FROM permissions WHERE permission_code = 'system:semester'), 2, 3),
('system:semester:delete', '删除学期', (SELECT id FROM permissions WHERE permission_code = 'system:semester'), 2, 4);

-- ====================================
-- 为管理员角色分配量化管理权限
-- ====================================

-- 获取管理员角色ID (假设是 role_id = 1)
-- 为管理员分配所有量化管理权限

INSERT INTO role_permissions (role_id, permission_id)
SELECT 1, id FROM permissions WHERE permission_code LIKE 'quantification%'
ON DUPLICATE KEY UPDATE role_id = role_id;

-- 为管理员分配学期管理权限
INSERT INTO role_permissions (role_id, permission_id)
SELECT 1, id FROM permissions WHERE permission_code LIKE 'system:semester%'
ON DUPLICATE KEY UPDATE role_id = role_id;

-- ====================================
-- 为检查员角色分配权限 (假设 role_id = 3)
-- ====================================

-- 检查员权限:查看任务、创建检查、录入扣分
INSERT INTO role_permissions (role_id, permission_id)
SELECT 3, id FROM permissions WHERE permission_code IN (
    'quantification',
    'quantification:task',
    'quantification:task:view',
    'quantification:check',
    'quantification:check:view',
    'quantification:check:create',
    'quantification:check:update',
    'quantification:check:deduct',
    'quantification:summary',
    'quantification:summary:view',
    'quantification:announcement',
    'quantification:announcement:view'
)
ON DUPLICATE KEY UPDATE role_id = role_id;

-- ====================================
-- 为班主任角色分配权限 (假设 role_id = 4)
-- ====================================

-- 班主任权限:查看检查、查看汇总、查看公告
INSERT INTO role_permissions (role_id, permission_id)
SELECT 4, id FROM permissions WHERE permission_code IN (
    'quantification',
    'quantification:check',
    'quantification:check:view',
    'quantification:summary',
    'quantification:summary:view',
    'quantification:announcement',
    'quantification:announcement:view'
)
ON DUPLICATE KEY UPDATE role_id = role_id;

-- ====================================
-- 完成
-- ====================================
