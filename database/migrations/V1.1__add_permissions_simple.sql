-- =====================================================
-- 权限颗粒度优化 - 简化版本
-- 版本: V1.1
-- 创建时间: 2025-11-18
-- =====================================================

-- 获取parent_id (先查询,后使用)
SET @student_parent_id = (SELECT id FROM permissions WHERE permission_code = 'student' LIMIT 1);
SET @system_parent_id = (SELECT id FROM permissions WHERE permission_code = 'system' LIMIT 1);
SET @quantification_parent_id = (SELECT id FROM permissions WHERE permission_code = 'quantification' LIMIT 1);

-- 1. 宿舍管理细粒度权限
INSERT INTO permissions (permission_code, permission_name, permission_desc, resource_type, parent_id, sort_order)
VALUES
-- 宿舍楼管理权限 (resource_type: 1=菜单, 2=按钮)
('dormitory:building:view', '宿舍楼查看', '宿舍楼列表查看权限', 1, @student_parent_id, 41),
('dormitory:building:add', '宿舍楼新增', '新增宿舍楼按钮权限', 2, @student_parent_id, 42),
('dormitory:building:edit', '宿舍楼编辑', '编辑宿舍楼按钮权限', 2, @student_parent_id, 43),
('dormitory:building:delete', '宿舍楼删除', '删除宿舍楼按钮权限', 2, @student_parent_id, 44),

-- 宿舍房间管理权限
('dormitory:room:view', '宿舍房间查看', '宿舍房间列表查看权限', 1, @student_parent_id, 45),
('dormitory:room:add', '宿舍房间新增', '新增宿舍房间按钮权限', 2, @student_parent_id, 46),
('dormitory:room:edit', '宿舍房间编辑', '编辑宿舍房间按钮权限', 2, @student_parent_id, 47),
('dormitory:room:delete', '宿舍房间删除', '删除宿舍房间按钮权限', 2, @student_parent_id, 48),
('dormitory:room:assign', '宿舍分配', '分配宿舍床位权限', 2, @student_parent_id, 49),

-- 宿舍总览权限
('dormitory:overview:view', '宿舍总览查看', '宿舍总览查看权限', 1, @student_parent_id, 50),
('dormitory:overview:export', '宿舍数据导出', '导出宿舍统计数据权限', 2, @student_parent_id, 51);

-- 2. 教学楼和教室细粒度权限
INSERT INTO permissions (permission_code, permission_name, permission_desc, resource_type, parent_id, sort_order)
VALUES
-- 教学楼权限
('teaching:building:view', '教学楼查看', '教学楼列表查看权限', 1, @system_parent_id, 91),
('teaching:building:add', '教学楼新增', '新增教学楼按钮权限', 2, @system_parent_id, 92),
('teaching:building:edit', '教学楼编辑', '编辑教学楼按钮权限', 2, @system_parent_id, 93),
('teaching:building:delete', '教学楼删除', '删除教学楼按钮权限', 2, @system_parent_id, 94),

-- 教室权限
('teaching:classroom:view', '教室查看', '教室列表查看权限', 1, @system_parent_id, 95),
('teaching:classroom:add', '教室新增', '新增教室按钮权限', 2, @system_parent_id, 96),
('teaching:classroom:edit', '教室编辑', '编辑教室按钮权限', 2, @system_parent_id, 97),
('teaching:classroom:delete', '教室删除', '删除教室按钮权限', 2, @system_parent_id, 98),
('teaching:classroom:schedule', '教室排课', '教室排课权限', 2, @system_parent_id, 99);

-- 3. 量化管理细粒度权限
INSERT INTO permissions (permission_code, permission_name, permission_desc, resource_type, parent_id, sort_order)
VALUES
-- 检查模板权限
('quantification:template:add', '模板新增', '新增检查模板按钮权限', 2, @quantification_parent_id, 102),
('quantification:template:edit', '模板编辑', '编辑检查模板按钮权限', 2, @quantification_parent_id, 103),
('quantification:template:delete', '模板删除', '删除检查模板按钮权限', 2, @quantification_parent_id, 104),

-- 检查记录权限
('quantification:record:v3:add', '记录新增', '新增检查记录按钮权限', 2, @quantification_parent_id, 106),
('quantification:record:v3:edit', '记录编辑', '编辑检查记录按钮权限', 2, @quantification_parent_id, 107),
('quantification:record:v3:delete', '记录删除', '删除检查记录按钮权限', 2, @quantification_parent_id, 108),
('quantification:record:v3:export', '记录导出', '导出检查记录按钮权限', 2, @quantification_parent_id, 109),

-- 申诉管理权限
('quantification:appeal:v3:add', '申诉新增', '新增申诉按钮权限', 2, @quantification_parent_id, 111),
('quantification:appeal:v3:approve', '申诉审批', '审批申诉按钮权限', 2, @quantification_parent_id, 112),
('quantification:appeal:v3:reject', '申诉驳回', '驳回申诉按钮权限', 2, @quantification_parent_id, 113),

-- 统计分析权限
('quantification:statistics:export', '统计导出', '导出统计数据按钮权限', 2, @quantification_parent_id, 115);

-- 4. 为超级管理员角色自动分配所有新权限 (角色ID=1)
INSERT INTO role_permissions (role_id, permission_id)
SELECT 1, id
FROM permissions
WHERE permission_code IN (
  'dormitory:building:view', 'dormitory:building:add', 'dormitory:building:edit', 'dormitory:building:delete',
  'dormitory:room:view', 'dormitory:room:add', 'dormitory:room:edit', 'dormitory:room:delete', 'dormitory:room:assign',
  'dormitory:overview:view', 'dormitory:overview:export',
  'teaching:building:view', 'teaching:building:add', 'teaching:building:edit', 'teaching:building:delete',
  'teaching:classroom:view', 'teaching:classroom:add', 'teaching:classroom:edit', 'teaching:classroom:delete', 'teaching:classroom:schedule',
  'quantification:template:add', 'quantification:template:edit', 'quantification:template:delete',
  'quantification:record:v3:add', 'quantification:record:v3:edit', 'quantification:record:v3:delete', 'quantification:record:v3:export',
  'quantification:appeal:v3:add', 'quantification:appeal:v3:approve', 'quantification:appeal:v3:reject',
  'quantification:statistics:export'
)
AND NOT EXISTS (
  SELECT 1 FROM role_permissions rp
  WHERE rp.role_id = 1 AND rp.permission_id = permissions.id
);

-- 5. 为拥有旧权限的角色自动分配对应的新权限
INSERT INTO role_permissions (role_id, permission_id)
SELECT DISTINCT rp.role_id, p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id
CROSS JOIN permissions p
WHERE old_p.permission_code = 'student:dormitory:view'
  AND p.permission_code IN (
    'dormitory:building:view',
    'dormitory:room:view',
    'dormitory:overview:view'
  )
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp2
    WHERE rp2.role_id = rp.role_id AND rp2.permission_id = p.id
  );

-- 6. 记录迁移
CREATE TABLE IF NOT EXISTS permission_migrations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  version VARCHAR(20) NOT NULL COMMENT '迁移版本',
  description VARCHAR(500) COMMENT '迁移说明',
  applied_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '应用时间',
  UNIQUE KEY uk_version (version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限迁移记录表';

INSERT INTO permission_migrations (version, description)
VALUES ('V1.1', '权限颗粒度优化 - 添加宿舍、教学、量化管理的细粒度权限')
ON DUPLICATE KEY UPDATE description = description;

SELECT '权限迁移完成!' AS status, COUNT(*) AS new_permissions_count
FROM permissions
WHERE permission_code LIKE 'dormitory:%'
   OR permission_code LIKE 'teaching:%'
   OR permission_code LIKE 'quantification:%';
