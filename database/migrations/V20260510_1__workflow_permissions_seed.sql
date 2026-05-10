-- E2E Tier 2 修复: workflow:* 权限码 seed 缺失, controller @PreAuthorize 拦 admin 403
-- 8 个权限对应 4 个 workflow controller 的 @PreAuthorize 字符串
-- 全部关联到 SUPER_ADMIN (role_id=1)

INSERT IGNORE INTO permissions (permission_name, permission_code, permission_desc, resource_type, status, industry, origin)
VALUES
  ('查看流程定义', 'workflow:definition:view',   '查看部署的 BPMN 流程定义列表/详情',  2, 1, 'CORE', 'workflow-controller'),
  ('部署流程',     'workflow:definition:deploy', '上传 BPMN 文件部署新流程版本',        2, 1, 'CORE', 'workflow-controller'),
  ('删除流程',     'workflow:definition:delete', '删除流程定义/部署',                    2, 1, 'CORE', 'workflow-controller'),
  ('管理流程定义', 'workflow:definition:manage', '挂起/激活流程定义',                    2, 1, 'CORE', 'workflow-controller'),
  ('启动流程',     'workflow:instance:start',    '启动流程实例',                         2, 1, 'CORE', 'workflow-controller'),
  ('查看流程实例', 'workflow:instance:view',     '查看运行中的流程实例',                 2, 1, 'CORE', 'workflow-controller'),
  ('取消流程',     'workflow:instance:cancel',   '取消运行中的流程实例',                 2, 1, 'CORE', 'workflow-controller'),
  ('查看流程历史', 'workflow:history:view',      '查看流程执行历史/审计轨迹',            2, 1, 'CORE', 'workflow-controller');

-- 关联到 SUPER_ADMIN (role_id=1)
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT 1, p.id FROM permissions p
WHERE p.permission_code IN (
  'workflow:definition:view','workflow:definition:deploy','workflow:definition:delete',
  'workflow:definition:manage','workflow:instance:start','workflow:instance:view',
  'workflow:instance:cancel','workflow:history:view'
);
