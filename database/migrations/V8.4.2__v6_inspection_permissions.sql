-- =============================================
-- V8.4.2: V6检查系统 - 权限配置
-- =============================================

-- 1. 添加V6检查系统权限
INSERT INTO permissions (id, permission_code, permission_name, description, module, created_at) VALUES
-- 检查项目权限
(8401, 'inspection:project:view', '查看检查项目', '查看V6检查项目列表和详情', 'inspection_v6', NOW()),
(8402, 'inspection:project:create', '创建检查项目', '创建新的V6检查项目', 'inspection_v6', NOW()),
(8403, 'inspection:project:update', '更新检查项目', '更新V6检查项目配置', 'inspection_v6', NOW()),
(8404, 'inspection:project:delete', '删除检查项目', '删除V6检查项目', 'inspection_v6', NOW()),
(8405, 'inspection:project:publish', '发布检查项目', '发布V6检查项目', 'inspection_v6', NOW()),

-- 检查任务权限
(8411, 'inspection:task:view', '查看检查任务', '查看V6检查任务列表和详情', 'inspection_v6', NOW()),
(8412, 'inspection:task:generate', '生成检查任务', '手动生成V6检查任务', 'inspection_v6', NOW()),
(8413, 'inspection:task:assign', '分配检查任务', '分配检查员到任务', 'inspection_v6', NOW()),
(8414, 'inspection:task:execute', '执行检查任务', '执行V6检查任务（领取、开始、提交）', 'inspection_v6', NOW()),
(8415, 'inspection:task:review', '审核检查任务', '审核V6检查任务', 'inspection_v6', NOW()),
(8416, 'inspection:task:publish', '发布检查任务', '发布V6检查任务结果', 'inspection_v6', NOW()),

-- 汇总统计权限
(8421, 'inspection:summary:view', '查看汇总统计', '查看V6检查汇总和排名', 'inspection_v6', NOW()),
(8422, 'inspection:summary:generate', '生成汇总统计', '手动生成V6检查汇总', 'inspection_v6', NOW())
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name);

-- 2. 为管理员角色分配V6权限
INSERT INTO role_permissions (role_id, permission_id)
SELECT 1, id FROM permissions WHERE module = 'inspection_v6'
ON DUPLICATE KEY UPDATE role_id = role_id;

-- 3. 为检查管理员角色分配V6权限（假设角色ID为4）
INSERT IGNORE INTO roles (id, role_code, role_name, description, created_at) VALUES
(4, 'inspection_admin', '检查管理员', 'V6检查系统管理员', NOW());

INSERT INTO role_permissions (role_id, permission_id)
SELECT 4, id FROM permissions WHERE module = 'inspection_v6'
ON DUPLICATE KEY UPDATE role_id = role_id;

-- 4. 为检查员角色分配执行权限（假设角色ID为5）
INSERT IGNORE INTO roles (id, role_code, role_name, description, created_at) VALUES
(5, 'inspector', '检查员', 'V6检查执行人员', NOW());

INSERT INTO role_permissions (role_id, permission_id)
SELECT 5, id FROM permissions WHERE permission_code IN (
    'inspection:project:view',
    'inspection:task:view',
    'inspection:task:execute',
    'inspection:summary:view'
)
ON DUPLICATE KEY UPDATE role_id = role_id;

-- 5. 为普通用户角色分配查看权限（假设角色ID为3）
INSERT INTO role_permissions (role_id, permission_id)
SELECT 3, id FROM permissions WHERE permission_code IN (
    'inspection:summary:view'
)
ON DUPLICATE KEY UPDATE role_id = role_id;
