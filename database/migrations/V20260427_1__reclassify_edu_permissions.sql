-- V20260427_1: 修正 86 条错归为 CORE 的教育行业权限
--
-- 背景: 插件化重构前, 权限全部堆在 CorePermissionProvider 里. 后来拆 EDU 插件时,
-- permissions.industry 字段部分权限未修正, 导致 82 条 quantification:* (学校德育量化),
-- 2 条 calendar:* (校历), 2 条 schedule:policy:* (教师排班) 仍归 CORE.
--
-- 这违反"通用核心 + 行业插件"原则: 医院装 HEALTH 时看到一堆"量化/校历"核心权限.
-- 修正 industry + origin 字段, 让禁用 EDU 时级联失效这 86 条.
--
-- 幂等: 判断 industry='CORE' 过滤, 已跑过则 0 rows affected.

UPDATE permissions
SET industry = 'EDU',
    origin = 'PLUGIN:EDU@1.0.0'
WHERE industry = 'CORE'
  AND deleted = 0
  AND (permission_code LIKE 'quantification:%'
    OR permission_code LIKE 'calendar:%'
    OR permission_code LIKE 'schedule:policy:%');

-- 验证
SELECT 'After reclassify:' AS status;
SELECT industry, COUNT(*) AS n
FROM permissions WHERE deleted=0
GROUP BY industry ORDER BY n DESC;
