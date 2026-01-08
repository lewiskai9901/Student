-- =====================================================
-- 权限颗粒度优化 - 添加细粒度权限
-- 版本: V1.1
-- 创建时间: 2025-11-18
-- 说明: 将粗粒度权限拆分为细粒度权限,支持更精确的权限控制
-- =====================================================

-- 1. 宿舍管理细粒度权限
-- 原权限: student:dormitory:view (统一控制所有宿舍功能)
-- 新权限: 分别控制宿舍楼、宿舍房间、宿舍总览

INSERT INTO permissions (permission_code, permission_name, permission_desc, resource_type, parent_id, sort_order)
VALUES
-- 宿舍楼管理权限 (resource_type: 1=菜单, 2=按钮)
('dormitory:building:view', '查看宿舍楼', '宿舍楼列表查看权限', 1, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'student' LIMIT 1) AS p1), 41),
('dormitory:building:add', '新增宿舍楼', '新增宿舍楼按钮权限', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'student' LIMIT 1) AS p2), 42),
('dormitory:building:edit', '编辑宿舍楼', '编辑宿舍楼按钮权限', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'student' LIMIT 1) AS p3), 43),
('dormitory:building:delete', '删除宿舍楼', '删除宿舍楼按钮权限', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'student' LIMIT 1) AS p4), 44),

-- 宿舍房间管理权限
('dormitory:room:view', '查看宿舍房间', '宿舍房间列表查看权限', 1, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'student' LIMIT 1) AS p5), 45),
('dormitory:room:add', '新增宿舍房间', '新增宿舍房间按钮权限', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'student' LIMIT 1) AS p6), 46),
('dormitory:room:edit', '编辑宿舍房间', '编辑宿舍房间按钮权限', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'student' LIMIT 1) AS p7), 47),
('dormitory:room:delete', '删除宿舍房间', '删除宿舍房间按钮权限', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'student' LIMIT 1) AS p8), 48),
('dormitory:room:assign', '分配宿舍', '分配宿舍床位权限', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'student' LIMIT 1) AS p9), 49),

-- 宿舍总览权限
('dormitory:overview:view', '查看宿舍总览', '宿舍总览查看权限', 1, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'student' LIMIT 1) AS p10), 50),
('dormitory:overview:export', '导出宿舍数据', '导出宿舍统计数据权限', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'student' LIMIT 1) AS p11), 51);

-- 2. 教学楼和教室细粒度权限
-- 原权限: teaching:building:list, teaching:classroom:list
-- 优化: 添加完整的CRUD权限

INSERT INTO permissions (permission_code, permission_name, permission_desc, resource_type, parent_id, sort_order)
VALUES
-- 教学楼权限
('teaching:building:view', '查看教学楼', '教学楼列表查看权限', 1, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'system' LIMIT 1) AS p12), 91),
('teaching:building:add', '新增教学楼', '新增教学楼按钮权限', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'system' LIMIT 1) AS p13), 92),
('teaching:building:edit', '编辑教学楼', '编辑教学楼按钮权限', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'system' LIMIT 1) AS p14), 93),
('teaching:building:delete', '删除教学楼', '删除教学楼按钮权限', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'system' LIMIT 1) AS p15), 94),

-- 教室权限
('teaching:classroom:view', '查看教室', '教室列表查看权限', 1, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'system' LIMIT 1) AS p16), 95),
('teaching:classroom:add', '新增教室', '新增教室按钮权限', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'system' LIMIT 1) AS p17), 96),
('teaching:classroom:edit', '编辑教室', '编辑教室按钮权限', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'system' LIMIT 1) AS p18), 97),
('teaching:classroom:delete', '删除教室', '删除教室按钮权限', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'system' LIMIT 1) AS p19), 98),
('teaching:classroom:schedule', '教室排课', '教室排课权限', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'system' LIMIT 1) AS p20), 99);

-- 3. 量化管理细粒度权限
-- 为每个子功能添加完整的操作权限

INSERT INTO permissions (permission_code, permission_name, permission_desc, resource_type, parent_id, sort_order)
VALUES
-- 检查模板权限
('quantification:template:add', '新增检查模板', '新增检查模板按钮权限', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'quantification' LIMIT 1) AS p21), 102),
('quantification:template:edit', '编辑检查模板', '编辑检查模板按钮权限', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'quantification' LIMIT 1) AS p22), 103),
('quantification:template:delete', '删除检查模板', '删除检查模板按钮权限', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'quantification' LIMIT 1) AS p23), 104),

-- 检查记录权限
('quantification:record:v3:add', '新增检查记录', '新增检查记录按钮权限', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'quantification' LIMIT 1) AS p24), 106),
('quantification:record:v3:edit', '编辑检查记录', '编辑检查记录按钮权限', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'quantification' LIMIT 1) AS p25), 107),
('quantification:record:v3:delete', '删除检查记录', '删除检查记录按钮权限', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'quantification' LIMIT 1) AS p26), 108),
('quantification:record:v3:export', '导出检查记录', '导出检查记录按钮权限', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'quantification' LIMIT 1) AS p27), 109),

-- 申诉管理权限
('quantification:appeal:v3:add', '新增申诉', '新增申诉按钮权限', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'quantification' LIMIT 1) AS p28), 111),
('quantification:appeal:v3:approve', '审批申诉', '审批申诉按钮权限', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'quantification' LIMIT 1) AS p29), 112),
('quantification:appeal:v3:reject', '驳回申诉', '驳回申诉按钮权限', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'quantification' LIMIT 1) AS p30), 113),

-- 统计分析权限
('quantification:statistics:export', '导出统计数据', '导出统计数据按钮权限', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'quantification' LIMIT 1) AS p31), 115);

-- 4. 为超级管理员角色自动分配所有新权限
-- 假设超级管理员角色ID为1
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
-- 将 student:dormitory:view 替换为细粒度权限
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

-- 6. 创建权限迁移记录表(可选,用于追踪权限变更)
CREATE TABLE IF NOT EXISTS permission_migrations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  version VARCHAR(20) NOT NULL COMMENT '迁移版本',
  description VARCHAR(500) COMMENT '迁移说明',
  applied_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '应用时间',
  UNIQUE KEY uk_version (version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限迁移记录表';

INSERT INTO permission_migrations (version, description)
VALUES ('V1.1', '权限颗粒度优化 - 添加宿舍、教学、量化管理的细粒度权限');

-- 7. 添加注释说明
-- 注意: 保留旧权限是为了兼容性,在确认所有功能正常后可以手动删除
-- 旧权限: student:dormitory:view, teaching:building:list, teaching:classroom:list
-- 建议在生产环境运行此脚本前先备份数据库!

COMMIT;
