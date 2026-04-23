-- V20260429_1: data_resources 加 allowed_scopes JSON 列
-- 每个数据模块声明自己支持哪些 scope, 前端下拉按模块动态渲染
--
-- NOTE: 本项目 DataScope 枚举使用 DEPARTMENT_AND_BELOW / DEPARTMENT
--       (不是 DEPT_AND_BELOW), 请勿混淆
-- 插件贡献维度 (data_scope_dims): BY_GRADE / BY_CLASS / BY_MAJOR (education),
--                                  BY_WARD / BY_ATTENDING_DOCTOR (healthcare)

-- ==================== 1. 加列 (幂等) ====================
SET @col = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='data_resources' AND column_name='allowed_scopes');
SET @sql = IF(@col = 0,
    'ALTER TABLE data_resources ADD COLUMN allowed_scopes JSON NULL COMMENT ''本模块支持的 scope 代码数组''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ==================== 2. 按业务规则批量 seed ====================

-- 重置所有行 (便于重跑)
UPDATE data_resources SET allowed_scopes = NULL;

-- ---- CORE 组织向: 5 种常规 scope ----
UPDATE data_resources SET allowed_scopes = JSON_ARRAY('ALL','DEPARTMENT_AND_BELOW','DEPARTMENT','SELF','CUSTOM')
WHERE resource_code IN ('user','org_unit','role','place','system_role','system_user');

-- ---- CORE 个人向: 只能 SELF ----
UPDATE data_resources SET allowed_scopes = JSON_ARRAY('SELF')
WHERE resource_code IN ('notification','dashboard');

-- ---- EDU 学生类: 加年级/班级/专业维度 ----
UPDATE data_resources SET allowed_scopes = JSON_ARRAY('ALL','BY_GRADE','BY_CLASS','BY_MAJOR','SELF','CUSTOM')
WHERE resource_code IN ('student','attendance');

-- ---- EDU 成绩/考试类: 没有年级维度 (批次本身跨年级) ----
UPDATE data_resources SET allowed_scopes = JSON_ARRAY('ALL','BY_CLASS','SELF','CUSTOM')
WHERE resource_code IN ('grade_batch','student_grade','exam','exam_batch');

-- ---- EDU 教学任务/班级: 按班级/专业 ----
UPDATE data_resources SET allowed_scopes = JSON_ARRAY('ALL','BY_CLASS','BY_MAJOR','SELF','CUSTOM')
WHERE resource_code IN ('teaching_task','school_class');

-- ---- EDU 宿舍/enrollment 等: 组织向 ----
UPDATE data_resources SET allowed_scopes = JSON_ARRAY('ALL','DEPARTMENT_AND_BELOW','DEPARTMENT','SELF','CUSTOM')
WHERE domain_code='education' AND allowed_scopes IS NULL;

-- ---- Inspection 全部: 5 种常规 scope ----
UPDATE data_resources SET allowed_scopes = JSON_ARRAY('ALL','DEPARTMENT_AND_BELOW','DEPARTMENT','SELF','CUSTOM')
WHERE domain_code='inspection' AND allowed_scopes IS NULL;

-- ---- HEALTH 病人向 (为未来 patient 插件准备, 若存在): BY_WARD / BY_ATTENDING_DOCTOR ----
UPDATE data_resources SET allowed_scopes = JSON_ARRAY('ALL','BY_WARD','BY_ATTENDING_DOCTOR','SELF','CUSTOM')
WHERE industry='HEALTH' AND resource_code IN ('patient','medical_record','prescription');

-- ---- HEALTH 其他: 组织向 ----
UPDATE data_resources SET allowed_scopes = JSON_ARRAY('ALL','DEPARTMENT_AND_BELOW','DEPARTMENT','SELF','CUSTOM')
WHERE industry='HEALTH' AND allowed_scopes IS NULL;

-- ---- 兜底: 其余给 5 个默认 ----
UPDATE data_resources SET allowed_scopes = JSON_ARRAY('ALL','DEPARTMENT_AND_BELOW','DEPARTMENT','SELF','CUSTOM')
WHERE allowed_scopes IS NULL;

-- ==================== 3. 报告 ====================
SELECT '=== allowed_scopes seeded ===' AS stage;
SELECT resource_code, domain_code, industry, JSON_EXTRACT(allowed_scopes, '$') AS scopes
FROM data_resources
ORDER BY domain_code, resource_code;
