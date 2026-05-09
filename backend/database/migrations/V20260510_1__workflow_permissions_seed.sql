-- 工作流权限 seed (Phase 2)
-- 实际 permissions 表列: id, permission_code, permission_name, parent_id,
--   permission_type, permission_scope, path, component, icon, sort_order,
--   status, created_at, updated_at, deleted, tenant_id
-- 没有 module_code / description 列, 模块关系靠 permission_code 前缀 'workflow:*'
-- INSERT IGNORE 依赖 uk_permission_code 唯一索引保证幂等

INSERT IGNORE INTO `permissions`
    (`permission_code`, `permission_name`, `parent_id`, `permission_type`,
     `permission_scope`, `sort_order`, `status`, `tenant_id`, `created_at`, `updated_at`, `deleted`)
VALUES
    ('workflow:definition:view',   '查看流程定义', 0, 3, 'MANAGEMENT', 1, 1, 1, NOW(), NOW(), 0),
    ('workflow:definition:deploy', '部署流程',     0, 3, 'MANAGEMENT', 2, 1, 1, NOW(), NOW(), 0),
    ('workflow:definition:delete', '删除流程部署', 0, 3, 'MANAGEMENT', 3, 1, 1, NOW(), NOW(), 0),
    ('workflow:definition:manage', '管理流程定义', 0, 3, 'MANAGEMENT', 4, 1, 1, NOW(), NOW(), 0),
    ('workflow:instance:view',     '查看流程实例', 0, 3, 'MANAGEMENT', 5, 1, 1, NOW(), NOW(), 0),
    ('workflow:instance:start',    '启动流程实例', 0, 3, 'MANAGEMENT', 6, 1, 1, NOW(), NOW(), 0),
    ('workflow:instance:cancel',   '取消流程实例', 0, 3, 'MANAGEMENT', 7, 1, 1, NOW(), NOW(), 0),
    ('workflow:history:view',      '查看流程历史', 0, 3, 'MANAGEMENT', 8, 1, 1, NOW(), NOW(), 0);
