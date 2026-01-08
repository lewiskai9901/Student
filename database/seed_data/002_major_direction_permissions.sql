-- ================================================================
-- 专业方向管理权限数据
-- 为专业方向和年级专业方向功能添加权限
-- ================================================================

-- 1. 添加专业方向管理权限
-- 先插入父权限
INSERT INTO permissions (permission_code, permission_name, permission_desc, parent_id, sort_order, created_at, updated_at) VALUES
('major:direction', '专业方向管理', NULL, NULL, 30, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  permission_name = VALUES(permission_name),
  updated_at = NOW();

-- 获取父权限ID
SET @major_direction_parent_id = (SELECT id FROM permissions WHERE permission_code = 'major:direction' LIMIT 1);

-- 插入子权限
INSERT INTO permissions (permission_code, permission_name, permission_desc, parent_id, sort_order, created_at, updated_at) VALUES
('major:direction:list', '查看专业方向列表', NULL, @major_direction_parent_id, 1, NOW(), NOW()),
('major:direction:info', '查看专业方向详情', NULL, @major_direction_parent_id, 2, NOW(), NOW()),
('major:direction:add', '新增专业方向', NULL, @major_direction_parent_id, 3, NOW(), NOW()),
('major:direction:edit', '编辑专业方向', NULL, @major_direction_parent_id, 4, NOW(), NOW()),
('major:direction:delete', '删除专业方向', NULL, @major_direction_parent_id, 5, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  permission_name = VALUES(permission_name),
  updated_at = NOW();

-- 2. 添加年级专业方向管理权限
-- 先插入父权限
INSERT INTO permissions (permission_code, permission_name, permission_desc, parent_id, sort_order, created_at, updated_at) VALUES
('grade:direction', '年级专业方向管理', NULL, NULL, 31, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  permission_name = VALUES(permission_name),
  updated_at = NOW();

-- 获取父权限ID
SET @grade_direction_parent_id = (SELECT id FROM permissions WHERE permission_code = 'grade:direction' LIMIT 1);

-- 插入子权限
INSERT INTO permissions (permission_code, permission_name, permission_desc, parent_id, sort_order, created_at, updated_at) VALUES
('grade:direction:list', '查看年级专业方向列表', NULL, @grade_direction_parent_id, 1, NOW(), NOW()),
('grade:direction:info', '查看年级专业方向详情', NULL, @grade_direction_parent_id, 2, NOW(), NOW()),
('grade:direction:add', '添加年级专业方向', NULL, @grade_direction_parent_id, 3, NOW(), NOW()),
('grade:direction:edit', '编辑年级专业方向', NULL, @grade_direction_parent_id, 4, NOW(), NOW()),
('grade:direction:delete', '删除年级专业方向', NULL, @grade_direction_parent_id, 5, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  permission_name = VALUES(permission_name),
  updated_at = NOW();

-- 3. 为管理员角色(role_id=1)分配权限
-- 获取所有专业方向相关权限ID并分配给管理员
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT 1, id, NOW(), NOW()
FROM permissions
WHERE permission_code LIKE 'major:direction%' OR permission_code LIKE 'grade:direction%'
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 4. 查询验证
SELECT '========== 专业方向权限列表 ==========' AS '';
SELECT
  p.id,
  p.permission_code,
  p.permission_name,
  p.description,
  CASE WHEN rp.role_id = 1 THEN '是' ELSE '否' END AS '已分配给管理员'
FROM permissions p
LEFT JOIN role_permissions rp ON p.id = rp.permission_id AND rp.role_id = 1
WHERE p.permission_code LIKE 'major:direction%' OR p.permission_code LIKE 'grade:direction%'
ORDER BY p.permission_code;

SELECT 'Permissions added successfully!' AS status;
