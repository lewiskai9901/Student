-- =============================================
-- V8.7.0: 添加V6缺失的权限
-- =============================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 添加实体类型管理、评分策略管理、整改管理相关权限
INSERT INTO permissions (permission_code, permission_name, permission_desc, resource_type, parent_id, sort_order, status, created_at) VALUES
-- 实体类型管理
('inspection:entity-type:view', '查看实体类型', '查看V6实体类型列表', 3, 0, 8501, 1, NOW()),
('inspection:entity-type:create', '创建实体类型', '创建新的实体类型', 3, 0, 8502, 1, NOW()),
('inspection:entity-type:update', '更新实体类型', '更新实体类型配置', 3, 0, 8503, 1, NOW()),
('inspection:entity-type:delete', '删除实体类型', '删除实体类型', 3, 0, 8504, 1, NOW()),

-- 实体分组管理
('inspection:entity-group:view', '查看实体分组', '查看V6实体分组列表', 3, 0, 8511, 1, NOW()),
('inspection:entity-group:manage', '管理实体分组', '创建、更新、删除实体分组', 3, 0, 8512, 1, NOW()),

-- 评分策略管理
('inspection:scoring-strategy:view', '查看评分策略', '查看V6评分策略列表', 3, 0, 8521, 1, NOW()),
('inspection:scoring-strategy:create', '创建评分策略', '创建新的评分策略', 3, 0, 8522, 1, NOW()),
('inspection:scoring-strategy:update', '更新评分策略', '更新评分策略配置', 3, 0, 8523, 1, NOW()),
('inspection:scoring-strategy:delete', '删除评分策略', '删除评分策略', 3, 0, 8524, 1, NOW()),

-- 整改工单管理
('inspection:corrective:view', '查看整改工单', '查看V6整改工单列表', 3, 0, 8531, 1, NOW()),
('inspection:corrective:create', '创建整改工单', '创建新的整改工单', 3, 0, 8532, 1, NOW()),
('inspection:corrective:handle', '处理整改工单', '提交整改信息', 3, 0, 8533, 1, NOW()),
('inspection:corrective:verify', '验收整改工单', '验收/驳回整改结果', 3, 0, 8534, 1, NOW()),
('inspection:corrective:cancel', '取消整改工单', '取消整改工单', 3, 0, 8535, 1, NOW()),

-- 检查配置（用于访问实体类型和评分策略管理页面）
('inspection:config:view', '查看检查配置', '查看V6检查配置（实体类型、评分策略等）', 3, 0, 8541, 1, NOW())
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name);

-- 为超级管理员角色分配新权限
INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT 1, id, NOW()
FROM permissions
WHERE permission_code IN (
    'inspection:entity-type:view',
    'inspection:entity-type:create',
    'inspection:entity-type:update',
    'inspection:entity-type:delete',
    'inspection:entity-group:view',
    'inspection:entity-group:manage',
    'inspection:scoring-strategy:view',
    'inspection:scoring-strategy:create',
    'inspection:scoring-strategy:update',
    'inspection:scoring-strategy:delete',
    'inspection:corrective:view',
    'inspection:corrective:create',
    'inspection:corrective:handle',
    'inspection:corrective:verify',
    'inspection:corrective:cancel',
    'inspection:config:view'
)
ON DUPLICATE KEY UPDATE role_id = role_id;

-- 为学校管理员角色分配查看权限
INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT 2, id, NOW()
FROM permissions
WHERE permission_code IN (
    'inspection:entity-type:view',
    'inspection:entity-type:create',
    'inspection:entity-type:update',
    'inspection:entity-group:view',
    'inspection:entity-group:manage',
    'inspection:scoring-strategy:view',
    'inspection:scoring-strategy:create',
    'inspection:scoring-strategy:update',
    'inspection:corrective:view',
    'inspection:corrective:verify',
    'inspection:config:view'
)
ON DUPLICATE KEY UPDATE role_id = role_id;
