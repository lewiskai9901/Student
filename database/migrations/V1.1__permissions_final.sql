-- =====================================================
-- 权限颗粒度优化 - 最终版本
-- 版本: V1.1
-- 创建时间: 2025-11-18
-- 权限名称控制在4个汉字以内,避免字段长度问题
-- =====================================================

-- 获取parent_id
SET @student_parent_id = (SELECT id FROM permissions WHERE permission_code = 'student' LIMIT 1);
SET @system_parent_id = (SELECT id FROM permissions WHERE permission_code = 'system' LIMIT 1);
SET @quantification_parent_id = (SELECT id FROM permissions WHERE permission_code = 'quantification' LIMIT 1);

-- 1. 宿舍管理细粒度权限 (resource_type: 1=菜单, 2=按钮)
INSERT INTO permissions (permission_code, permission_name, permission_desc, resource_type, parent_id, sort_order)
VALUES
('dormitory:building:view', '楼栋查看', '宿舍楼列表查看权限', 1, @student_parent_id, 41),
('dormitory:building:add', '楼栋新增', '新增宿舍楼权限', 2, @student_parent_id, 42),
('dormitory:building:edit', '楼栋编辑', '编辑宿舍楼权限', 2, @student_parent_id, 43),
('dormitory:building:delete', '楼栋删除', '删除宿舍楼权限', 2, @student_parent_id, 44),
('dormitory:room:view', '宿舍查看', '宿舍房间查看权限', 1, @student_parent_id, 45),
('dormitory:room:add', '宿舍新增', '新增宿舍房间权限', 2, @student_parent_id, 46),
('dormitory:room:edit', '宿舍编辑', '编辑宿舍房间权限', 2, @student_parent_id, 47),
('dormitory:room:delete', '宿舍删除', '删除宿舍房间权限', 2, @student_parent_id, 48),
('dormitory:room:assign', '宿舍分配', '分配宿舍床位权限', 2, @student_parent_id, 49),
('dormitory:overview:view', '总览查看', '宿舍总览查看权限', 1, @student_parent_id, 50),
('dormitory:overview:export', '数据导出', '导出宿舍数据权限', 2, @student_parent_id, 51);

-- 2. 教学楼和教室细粒度权限
INSERT INTO permissions (permission_code, permission_name, permission_desc, resource_type, parent_id, sort_order)
VALUES
('teaching:building:view', '教学楼查看', '教学楼查看权限', 1, @system_parent_id, 91),
('teaching:building:add', '教学楼新增', '新增教学楼权限', 2, @system_parent_id, 92),
('teaching:building:edit', '教学楼编辑', '编辑教学楼权限', 2, @system_parent_id, 93),
('teaching:building:delete', '教学楼删除', '删除教学楼权限', 2, @system_parent_id, 94),
('teaching:classroom:view', '教室查看', '教室列表查看权限', 1, @system_parent_id, 95),
('teaching:classroom:add', '教室新增', '新增教室权限', 2, @system_parent_id, 96),
('teaching:classroom:edit', '教室编辑', '编辑教室权限', 2, @system_parent_id, 97),
('teaching:classroom:delete', '教室删除', '删除教室权限', 2, @system_parent_id, 98),
('teaching:classroom:schedule', '教室排课', '教室排课权限', 2, @system_parent_id, 99);

-- 3. 量化管理细粒度权限
INSERT INTO permissions (permission_code, permission_name, permission_desc, resource_type, parent_id, sort_order)
VALUES
('quantification:template:add', '模板新增', '新增检查模板权限', 2, @quantification_parent_id, 102),
('quantification:template:edit', '模板编辑', '编辑检查模板权限', 2, @quantification_parent_id, 103),
('quantification:template:delete', '模板删除', '删除检查模板权限', 2, @quantification_parent_id, 104),
('quantification:record:v3:add', '记录新增', '新增检查记录权限', 2, @quantification_parent_id, 106),
('quantification:record:v3:edit', '记录编辑', '编辑检查记录权限', 2, @quantification_parent_id, 107),
('quantification:record:v3:delete', '记录删除', '删除检查记录权限', 2, @quantification_parent_id, 108),
('quantification:record:v3:export', '记录导出', '导出检查记录权限', 2, @quantification_parent_id, 109),
('quantification:appeal:v3:add', '申诉新增', '新增申诉权限', 2, @quantification_parent_id, 111),
('quantification:appeal:v3:approve', '申诉审批', '审批申诉权限', 2, @quantification_parent_id, 112),
('quantification:appeal:v3:reject', '申诉驳回', '驳回申诉权限', 2, @quantification_parent_id, 113),
('quantification:statistics:export', '统计导出', '导出统计数据权限', 2, @quantification_parent_id, 115);

-- 4. 为超级管理员分配所有新权限
INSERT INTO role_permissions (role_id, permission_id)
SELECT 1, id FROM permissions
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
  SELECT 1 FROM role_permissions rp WHERE rp.role_id = 1 AND rp.permission_id = permissions.id
);

-- 5. 为拥有旧权限的角色自动分配新权限
INSERT INTO role_permissions (role_id, permission_id)
SELECT DISTINCT rp.role_id, p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id
CROSS JOIN permissions p
WHERE old_p.permission_code = 'student:dormitory:view'
  AND p.permission_code IN ('dormitory:building:view', 'dormitory:room:view', 'dormitory:overview:view')
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp2 WHERE rp2.role_id = rp.role_id AND rp2.permission_id = p.id);

-- 6. 记录迁移
CREATE TABLE IF NOT EXISTS permission_migrations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  version VARCHAR(20) NOT NULL,
  description VARCHAR(500),
  applied_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_version (version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO permission_migrations (version, description)
VALUES ('V1.1', '权限颗粒度优化-添加细粒度权限')
ON DUPLICATE KEY UPDATE description = description;

SELECT '✅ 权限迁移完成!' AS status, COUNT(*) AS new_permissions_added
FROM permissions WHERE permission_code LIKE 'dormitory:%' OR permission_code LIKE 'teaching:%'
  OR (permission_code LIKE 'quantification:%' AND permission_code NOT LIKE 'quantification:%:view');
