-- =============================================
-- 综合素质测评模块权限数据
-- 创建时间: 2025-11-28
-- =============================================

-- 获取综测模块父权限ID（如果不存在则创建）
SET @evaluation_parent_id = NULL;
SELECT id INTO @evaluation_parent_id FROM permissions WHERE permission_code = 'evaluation' LIMIT 1;

-- 如果不存在则创建综测模块父权限
INSERT INTO permissions (permission_code, permission_name, parent_id, type, sort_order, status, description)
SELECT 'evaluation', '综合测评', 0, 0, 70, 1, '综合素质测评模块'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_code = 'evaluation');

SET @evaluation_parent_id = (SELECT id FROM permissions WHERE permission_code = 'evaluation');

-- =============================================
-- 综测周期管理权限
-- =============================================
INSERT INTO permissions (permission_code, permission_name, parent_id, type, sort_order, status, description) VALUES
('evaluation:period', '综测周期管理', @evaluation_parent_id, 0, 1, 1, '综测周期管理菜单'),
('evaluation:period:list', '查看周期列表', (SELECT id FROM permissions WHERE permission_code = 'evaluation:period'), 1, 1, 1, '查看综测周期列表'),
('evaluation:period:create', '创建周期', (SELECT id FROM permissions WHERE permission_code = 'evaluation:period'), 1, 2, 1, '创建综测周期'),
('evaluation:period:update', '更新周期', (SELECT id FROM permissions WHERE permission_code = 'evaluation:period'), 1, 3, 1, '更新综测周期'),
('evaluation:period:delete', '删除周期', (SELECT id FROM permissions WHERE permission_code = 'evaluation:period'), 1, 4, 1, '删除综测周期'),
('evaluation:period:manage', '管理周期状态', (SELECT id FROM permissions WHERE permission_code = 'evaluation:period'), 1, 5, 1, '管理综测周期状态（开始/结束各阶段）'),
('evaluation:period:lock', '锁定/解锁周期', (SELECT id FROM permissions WHERE permission_code = 'evaluation:period'), 1, 6, 1, '锁定或解锁综测周期');

-- =============================================
-- 荣誉申报管理权限
-- =============================================
INSERT INTO permissions (permission_code, permission_name, parent_id, type, sort_order, status, description) VALUES
('evaluation:honor', '荣誉申报管理', @evaluation_parent_id, 0, 2, 1, '荣誉申报管理菜单'),
('evaluation:honor:list', '查看申报列表', (SELECT id FROM permissions WHERE permission_code = 'evaluation:honor'), 1, 1, 1, '查看荣誉申报列表'),
('evaluation:honor:apply', '提交申报', (SELECT id FROM permissions WHERE permission_code = 'evaluation:honor'), 1, 2, 1, '提交荣誉申报'),
('evaluation:honor:class-review', '班级审核', (SELECT id FROM permissions WHERE permission_code = 'evaluation:honor'), 1, 3, 1, '班级级别审核荣誉申报'),
('evaluation:honor:dept-review', '院系审核', (SELECT id FROM permissions WHERE permission_code = 'evaluation:honor'), 1, 4, 1, '院系级别审核荣誉申报'),
('evaluation:honor:school-review', '学校审核', (SELECT id FROM permissions WHERE permission_code = 'evaluation:honor'), 1, 5, 1, '学校级别审核荣誉申报'),
('evaluation:honor:export', '导出申报数据', (SELECT id FROM permissions WHERE permission_code = 'evaluation:honor'), 1, 6, 1, '导出荣誉申报数据');

-- =============================================
-- 课程管理权限
-- =============================================
INSERT INTO permissions (permission_code, permission_name, parent_id, type, sort_order, status, description) VALUES
('evaluation:course', '课程管理', @evaluation_parent_id, 0, 3, 1, '课程管理菜单'),
('evaluation:course:list', '查看课程列表', (SELECT id FROM permissions WHERE permission_code = 'evaluation:course'), 1, 1, 1, '查看课程列表'),
('evaluation:course:create', '创建课程', (SELECT id FROM permissions WHERE permission_code = 'evaluation:course'), 1, 2, 1, '创建课程'),
('evaluation:course:update', '更新课程', (SELECT id FROM permissions WHERE permission_code = 'evaluation:course'), 1, 3, 1, '更新课程'),
('evaluation:course:delete', '删除课程', (SELECT id FROM permissions WHERE permission_code = 'evaluation:course'), 1, 4, 1, '删除课程'),
('evaluation:course:import', '导入课程', (SELECT id FROM permissions WHERE permission_code = 'evaluation:course'), 1, 5, 1, '导入课程数据');

-- =============================================
-- 成绩管理权限
-- =============================================
INSERT INTO permissions (permission_code, permission_name, parent_id, type, sort_order, status, description) VALUES
('evaluation:score', '成绩管理', @evaluation_parent_id, 0, 4, 1, '成绩管理菜单'),
('evaluation:score:list', '查看成绩列表', (SELECT id FROM permissions WHERE permission_code = 'evaluation:score'), 1, 1, 1, '查看成绩列表'),
('evaluation:score:my', '查看我的成绩', (SELECT id FROM permissions WHERE permission_code = 'evaluation:score'), 1, 2, 1, '学生查看自己的成绩'),
('evaluation:score:input', '录入成绩', (SELECT id FROM permissions WHERE permission_code = 'evaluation:score'), 1, 3, 1, '录入学生成绩'),
('evaluation:score:update', '更新成绩', (SELECT id FROM permissions WHERE permission_code = 'evaluation:score'), 1, 4, 1, '更新学生成绩'),
('evaluation:score:delete', '删除成绩', (SELECT id FROM permissions WHERE permission_code = 'evaluation:score'), 1, 5, 1, '删除学生成绩'),
('evaluation:score:import', '导入成绩', (SELECT id FROM permissions WHERE permission_code = 'evaluation:score'), 1, 6, 1, '导入成绩数据'),
('evaluation:score:export', '导出成绩', (SELECT id FROM permissions WHERE permission_code = 'evaluation:score'), 1, 7, 1, '导出成绩数据'),
('evaluation:score:lock', '锁定/解锁成绩', (SELECT id FROM permissions WHERE permission_code = 'evaluation:score'), 1, 8, 1, '锁定或解锁成绩');

-- =============================================
-- 综测结果管理权限
-- =============================================
INSERT INTO permissions (permission_code, permission_name, parent_id, type, sort_order, status, description) VALUES
('evaluation:result', '综测结果管理', @evaluation_parent_id, 0, 5, 1, '综测结果管理菜单'),
('evaluation:result:list', '查看综测结果', (SELECT id FROM permissions WHERE permission_code = 'evaluation:result'), 1, 1, 1, '查看综测结果列表'),
('evaluation:result:my', '查看我的综测', (SELECT id FROM permissions WHERE permission_code = 'evaluation:result'), 1, 2, 1, '学生查看自己的综测结果'),
('evaluation:result:calculate', '计算综测', (SELECT id FROM permissions WHERE permission_code = 'evaluation:result'), 1, 3, 1, '计算学生/班级/年级综测'),
('evaluation:result:export', '导出综测结果', (SELECT id FROM permissions WHERE permission_code = 'evaluation:result'), 1, 4, 1, '导出综测结果数据'),
('evaluation:result:sync', '同步数据', (SELECT id FROM permissions WHERE permission_code = 'evaluation:result'), 1, 5, 1, '同步量化/荣誉数据到综测');

-- =============================================
-- 行为类型管理权限
-- =============================================
INSERT INTO permissions (permission_code, permission_name, parent_id, type, sort_order, status, description) VALUES
('evaluation:behavior', '行为类型管理', @evaluation_parent_id, 0, 6, 1, '行为类型管理菜单'),
('evaluation:behavior:list', '查看行为类型', (SELECT id FROM permissions WHERE permission_code = 'evaluation:behavior'), 1, 1, 1, '查看行为类型列表'),
('evaluation:behavior:create', '创建行为类型', (SELECT id FROM permissions WHERE permission_code = 'evaluation:behavior'), 1, 2, 1, '创建行为类型'),
('evaluation:behavior:update', '更新行为类型', (SELECT id FROM permissions WHERE permission_code = 'evaluation:behavior'), 1, 3, 1, '更新行为类型'),
('evaluation:behavior:delete', '删除行为类型', (SELECT id FROM permissions WHERE permission_code = 'evaluation:behavior'), 1, 4, 1, '删除行为类型');

-- =============================================
-- 为管理员角色分配所有综测权限
-- =============================================
INSERT INTO role_permissions (role_id, permission_id)
SELECT 1, id FROM permissions WHERE permission_code LIKE 'evaluation:%'
ON DUPLICATE KEY UPDATE role_id = role_id;

-- =============================================
-- 为学生角色分配基本综测权限
-- =============================================
-- 假设学生角色ID为3
INSERT INTO role_permissions (role_id, permission_id)
SELECT 3, id FROM permissions WHERE permission_code IN (
    'evaluation:honor:apply',
    'evaluation:score:my',
    'evaluation:result:my'
)
ON DUPLICATE KEY UPDATE role_id = role_id;

-- =============================================
-- 为班主任/辅导员角色分配班级审核权限
-- =============================================
-- 假设班主任角色ID为4
INSERT INTO role_permissions (role_id, permission_id)
SELECT 4, id FROM permissions WHERE permission_code IN (
    'evaluation:honor:list',
    'evaluation:honor:class-review',
    'evaluation:result:list',
    'evaluation:score:list'
)
ON DUPLICATE KEY UPDATE role_id = role_id;

-- =============================================
-- 为院系管理员角色分配院系审核权限
-- =============================================
-- 假设院系管理员角色ID为5
INSERT INTO role_permissions (role_id, permission_id)
SELECT 5, id FROM permissions WHERE permission_code IN (
    'evaluation:honor:list',
    'evaluation:honor:dept-review',
    'evaluation:result:list',
    'evaluation:result:calculate',
    'evaluation:score:list'
)
ON DUPLICATE KEY UPDATE role_id = role_id;

SELECT '综测模块权限数据初始化完成' AS message;
