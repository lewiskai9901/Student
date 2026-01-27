-- =====================================================
-- 量化权限清理脚本
-- 日期: 2026-01-11
-- 说明: 清理重复和废弃的量化管理权限
-- =====================================================

-- 开始事务
START TRANSACTION;

-- =====================================================
-- 第一步: 删除完全未使用的 V3 权限 (8条)
-- =====================================================
DELETE FROM role_permissions WHERE permission_id IN (
    SELECT id FROM permissions WHERE permission_code LIKE 'quantification:record:v3:%'
);

DELETE FROM permissions WHERE permission_code LIKE 'quantification:record:v3:%';

-- 验证: 应该删除 8 条
SELECT '删除 V3 权限后' AS step, COUNT(*) AS remaining
FROM permissions WHERE permission_code LIKE 'quantification:%';

-- =====================================================
-- 第二步: 删除未使用的 quantification:check-record:* 权限
-- 只保留 publish 和 review (这两个被 rating 控制器使用)
-- =====================================================
DELETE FROM role_permissions WHERE permission_id IN (
    SELECT id FROM permissions
    WHERE permission_code LIKE 'quantification:check-record:%'
    AND permission_code NOT IN (
        'quantification:check-record:publish',
        'quantification:check-record:review'
    )
);

DELETE FROM permissions
WHERE permission_code LIKE 'quantification:check-record:%'
AND permission_code NOT IN (
    'quantification:check-record:publish',
    'quantification:check-record:review'
);

-- 删除父级 quantification:check-record (保留子权限)
DELETE FROM role_permissions WHERE permission_id IN (
    SELECT id FROM permissions WHERE permission_code = 'quantification:check-record'
);
DELETE FROM permissions WHERE permission_code = 'quantification:check-record';

-- 验证: 应该只剩下 2 条 check-record 权限
SELECT '删除未使用 check-record 权限后' AS step, COUNT(*) AS remaining
FROM permissions WHERE permission_code LIKE 'quantification:check-record%';

-- =====================================================
-- 第三步: 删除 appeal:v3:view (未使用)
-- =====================================================
DELETE FROM role_permissions WHERE permission_id IN (
    SELECT id FROM permissions WHERE permission_code = 'quantification:appeal:v3:view'
);
DELETE FROM permissions WHERE permission_code = 'quantification:appeal:v3:view';

-- =====================================================
-- 第四步: 统一权限命名 (修复乱码)
-- =====================================================
UPDATE permissions SET permission_name = '检查记录' WHERE permission_code = 'quantification:record';
UPDATE permissions SET permission_name = '录入记录' WHERE permission_code = 'quantification:record:add';
UPDATE permissions SET permission_name = '归档记录' WHERE permission_code = 'quantification:record:archive';
UPDATE permissions SET permission_name = '转换记录' WHERE permission_code = 'quantification:record:convert';
UPDATE permissions SET permission_name = '创建记录' WHERE permission_code = 'quantification:record:create';
UPDATE permissions SET permission_name = '删除记录' WHERE permission_code = 'quantification:record:delete';
UPDATE permissions SET permission_name = '查看记录详情' WHERE permission_code = 'quantification:record:detail';
UPDATE permissions SET permission_name = '编辑记录' WHERE permission_code = 'quantification:record:edit';
UPDATE permissions SET permission_name = '导出记录' WHERE permission_code = 'quantification:record:export';
UPDATE permissions SET permission_name = '查看记录列表' WHERE permission_code = 'quantification:record:list';
UPDATE permissions SET permission_name = '查看本班记录' WHERE permission_code = 'quantification:record:my-class';
UPDATE permissions SET permission_name = '发布记录' WHERE permission_code = 'quantification:record:publish';
UPDATE permissions SET permission_name = '查看记录' WHERE permission_code = 'quantification:record:view';

UPDATE permissions SET permission_name = '发布评级结果' WHERE permission_code = 'quantification:check-record:publish';
UPDATE permissions SET permission_name = '审核评级结果' WHERE permission_code = 'quantification:check-record:review';

UPDATE permissions SET permission_name = '量化管理' WHERE permission_code = 'quantification';
UPDATE permissions SET permission_name = '量化配置' WHERE permission_code = 'quantification:config';
UPDATE permissions SET permission_name = '量化字典' WHERE permission_code = 'quantification:dictionary';
UPDATE permissions SET permission_name = '日常检查' WHERE permission_code = 'quantification:check';
UPDATE permissions SET permission_name = '检查模板' WHERE permission_code = 'quantification:template';
UPDATE permissions SET permission_name = '检查计划' WHERE permission_code = 'quantification:plan';
UPDATE permissions SET permission_name = '申诉管理' WHERE permission_code = 'quantification:appeal';
UPDATE permissions SET permission_name = '统计分析' WHERE permission_code = 'quantification:analysis';
UPDATE permissions SET permission_name = '统计分析' WHERE permission_code = 'quantification:statistics';
UPDATE permissions SET permission_name = '加权配置' WHERE permission_code = 'quantification:weight-config';
UPDATE permissions SET permission_name = '评级管理' WHERE permission_code = 'quantification:rating';
UPDATE permissions SET permission_name = '年级管理' WHERE permission_code = 'quantification:grade';

-- =====================================================
-- 第五步: 验证最终结果
-- =====================================================
SELECT '清理后总权限数' AS info, COUNT(*) AS count
FROM permissions WHERE permission_code LIKE 'quantification:%';

-- 提交事务
COMMIT;

-- 显示清理后的权限列表
SELECT permission_code, permission_name
FROM permissions
WHERE permission_code LIKE 'quantification:%'
ORDER BY permission_code;
