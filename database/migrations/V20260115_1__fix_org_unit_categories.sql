-- ================================================================
-- 修复组织单元类别
-- 将职能部门标记为 'functional'，教学单位保持 'academic'
-- ================================================================

-- 更新职能部门的类别
UPDATE departments SET unit_category = 'functional'
WHERE dept_name IN ('学工处', '教务处', '财务处', '人事处', '后勤处', '保卫处', '招生办', '就业办', '办公室', '团委', '工会');

-- 确保教学系部是教学单位
UPDATE departments SET unit_category = 'academic'
WHERE dept_name LIKE '%系' OR dept_name LIKE '%学院' OR dept_name LIKE '%部';

-- 学校根节点设为教学单位
UPDATE departments SET unit_category = 'academic'
WHERE dept_name = '学校';

-- 查看更新结果
SELECT id, dept_name, unit_category, unit_type
FROM departments
WHERE deleted = 0
ORDER BY unit_category, dept_name;
