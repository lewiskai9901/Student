-- ============================================
-- 检查记录V3 权限配置
-- 创建日期: 2025-11-01
-- ============================================

-- 1. 插入权限数据
INSERT INTO permissions (permission_code, permission_name, parent_id, permission_type, sort_order, created_at)
VALUES
-- 主菜单
('quantification:record:v3', '检查记录V3', NULL, 1, 30, NOW()),

-- 子权限
('quantification:record:v3:list', '查看记录列表',
    (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'quantification:record:v3') AS temp),
    2, 1, NOW()),

('quantification:record:v3:detail', '查看记录详情',
    (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'quantification:record:v3') AS temp),
    2, 2, NOW()),

('quantification:record:v3:my-class', '查看本班详情',
    (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'quantification:record:v3') AS temp),
    2, 3, NOW()),

('quantification:record:v3:export', '导出记录',
    (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'quantification:record:v3') AS temp),
    2, 4, NOW()),

('quantification:record:v3:convert', '手动转换',
    (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'quantification:record:v3') AS temp),
    2, 5, NOW());

-- 2. 分配给管理员角色(role_id=1)
INSERT INTO role_permissions (role_id, permission_id)
SELECT 1, id FROM permissions
WHERE permission_code LIKE 'quantification:record:v3%'
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = 1 AND rp.permission_id = permissions.id
);

-- 3. 分配给班主任角色(假设role_id=3,请根据实际情况修改)
-- 班主任只有查看列表和本班详情的权限
INSERT INTO role_permissions (role_id, permission_id)
SELECT 3, id FROM permissions
WHERE permission_code IN (
    'quantification:record:v3',
    'quantification:record:v3:list',
    'quantification:record:v3:my-class'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = 3 AND rp.permission_id = permissions.id
);

-- 4. 查询验证
SELECT
    p.id,
    p.permission_code,
    p.permission_name,
    p.permission_type,
    p.sort_order
FROM permissions p
WHERE p.permission_code LIKE 'quantification:record:v3%'
ORDER BY p.sort_order;

-- 5. 查询角色权限分配情况
SELECT
    r.id as role_id,
    r.role_name,
    p.permission_code,
    p.permission_name
FROM roles r
JOIN role_permissions rp ON r.id = rp.role_id
JOIN permissions p ON rp.permission_id = p.id
WHERE p.permission_code LIKE 'quantification:record:v3%'
ORDER BY r.id, p.sort_order;

COMMIT;
