-- V6 检查项目权限
INSERT IGNORE INTO permissions (permission_code, permission_name, resource, action, type, sort_order, is_enabled) VALUES
('inspection:project:view', 'View Project', 'inspection:project', 'view', 'BUTTON', 0, 1),
('inspection:project:create', 'Create Project', 'inspection:project', 'create', 'BUTTON', 0, 1),
('inspection:project:update', 'Update Project', 'inspection:project', 'update', 'BUTTON', 0, 1),
('inspection:project:publish', 'Publish Project', 'inspection:project', 'publish', 'BUTTON', 0, 1),
('inspection:project:archive', 'Archive Project', 'inspection:project', 'archive', 'BUTTON', 0, 1);

-- V6 检查任务权限
INSERT IGNORE INTO permissions (permission_code, permission_name, resource, action, type, sort_order, is_enabled) VALUES
('inspection:task:view', 'View Task', 'inspection:task', 'view', 'BUTTON', 0, 1),
('inspection:task:claim', 'Claim Task', 'inspection:task', 'claim', 'BUTTON', 0, 1),
('inspection:task:execute', 'Execute Task', 'inspection:task', 'execute', 'BUTTON', 0, 1),
('inspection:task:submit', 'Submit Task', 'inspection:task', 'submit', 'BUTTON', 0, 1),
('inspection:task:review', 'Review Task', 'inspection:task', 'review', 'BUTTON', 0, 1);

-- V6 检查执行权限
INSERT IGNORE INTO permissions (permission_code, permission_name, resource, action, type, sort_order, is_enabled) VALUES
('inspection:execution:deduct', 'Deduct Score', 'inspection:execution', 'deduct', 'BUTTON', 0, 1),
('inspection:execution:bonus', 'Bonus Score', 'inspection:execution', 'bonus', 'BUTTON', 0, 1),
('inspection:execution:evidence', 'Upload Evidence', 'inspection:execution', 'evidence', 'BUTTON', 0, 1);

-- V6 检查汇总权限
INSERT IGNORE INTO permissions (permission_code, permission_name, resource, action, type, sort_order, is_enabled) VALUES
('inspection:summary:view', 'View Summary', 'inspection:summary', 'view', 'BUTTON', 0, 1),
('inspection:summary:generate', 'Generate Summary', 'inspection:summary', 'generate', 'BUTTON', 0, 1);

-- 获取新添加的权限ID
SELECT @proj_view := id FROM permissions WHERE permission_code = 'inspection:project:view';
SELECT @proj_create := id FROM permissions WHERE permission_code = 'inspection:project:create';
SELECT @proj_update := id FROM permissions WHERE permission_code = 'inspection:project:update';
SELECT @proj_publish := id FROM permissions WHERE permission_code = 'inspection:project:publish';
SELECT @proj_archive := id FROM permissions WHERE permission_code = 'inspection:project:archive';
SELECT @task_view := id FROM permissions WHERE permission_code = 'inspection:task:view';
SELECT @task_claim := id FROM permissions WHERE permission_code = 'inspection:task:claim';
SELECT @task_execute := id FROM permissions WHERE permission_code = 'inspection:task:execute';
SELECT @task_submit := id FROM permissions WHERE permission_code = 'inspection:task:submit';
SELECT @task_review := id FROM permissions WHERE permission_code = 'inspection:task:review';
SELECT @exec_deduct := id FROM permissions WHERE permission_code = 'inspection:execution:deduct';
SELECT @exec_bonus := id FROM permissions WHERE permission_code = 'inspection:execution:bonus';
SELECT @exec_evidence := id FROM permissions WHERE permission_code = 'inspection:execution:evidence';
SELECT @summary_view := id FROM permissions WHERE permission_code = 'inspection:summary:view';
SELECT @summary_generate := id FROM permissions WHERE permission_code = 'inspection:summary:generate';

-- 给超级管理员(role_id=1)分配V6权限
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT 1, id FROM permissions WHERE permission_code LIKE 'inspection:project:%'
   OR permission_code LIKE 'inspection:task:%'
   OR permission_code LIKE 'inspection:execution:%'
   OR permission_code LIKE 'inspection:summary:%';

-- 给学校管理员(role_id=2)分配V6权限
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT 2, id FROM permissions WHERE permission_code LIKE 'inspection:project:%'
   OR permission_code LIKE 'inspection:task:%'
   OR permission_code LIKE 'inspection:execution:%'
   OR permission_code LIKE 'inspection:summary:%';

-- 给检查员(role_id=9)分配V6执行相关权限
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT 9, id FROM permissions WHERE permission_code IN (
   'inspection:project:view',
   'inspection:task:view',
   'inspection:task:claim',
   'inspection:task:execute',
   'inspection:task:submit',
   'inspection:execution:deduct',
   'inspection:execution:bonus',
   'inspection:execution:evidence',
   'inspection:summary:view'
);

-- 给班主任(role_id=5)分配V6查看权限
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT 5, id FROM permissions WHERE permission_code IN (
   'inspection:project:view',
   'inspection:task:view',
   'inspection:summary:view'
);
