-- V20260428_1: data_resources 加 industry + plugin_enabled 列, 对齐 industry 体系 + 级联禁用
--
-- 背景:
--   data_resources 存数据权限模块 (resource_code/domain_code), 但未与插件平台 industry 体系打通:
--     1. 无 industry 列, PluginLifecycleService 级联表里没它
--     2. domain_code 大小写混用 (CORE/education/inspection)
--     3. 禁用 EDU 后, 学生/班级模块照样显示并可选 (无灰显)
--   本迁移加 industry + plugin_enabled 两列. domain_code 保留不改 (代码里有硬编码映射).
--
-- 可重复执行 (information_schema 条件判断).

-- 1. industry 列
SET @col1 = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='data_resources' AND column_name='industry');
SET @sql = IF(@col1 = 0,
    'ALTER TABLE data_resources ADD COLUMN industry VARCHAR(20) DEFAULT NULL COMMENT ''所属行业 CORE/EDU/HEALTH/CARE/CUSTOM'' AFTER domain_code',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. plugin_enabled 列 (插件生命周期级联用)
SET @col2 = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='data_resources' AND column_name='plugin_enabled');
SET @sql = IF(@col2 = 0,
    'ALTER TABLE data_resources ADD COLUMN plugin_enabled TINYINT NOT NULL DEFAULT 1',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. 反查并修正 industry: domain_code 映射
--    CORE → CORE, education → EDU, inspection → CORE (inspection 是 core 平台子域), healthcare → HEALTH
UPDATE data_resources SET industry='CORE'
  WHERE domain_code='CORE' AND (industry IS NULL OR industry='');
UPDATE data_resources SET industry='EDU'
  WHERE domain_code='education' AND (industry IS NULL OR industry='');
UPDATE data_resources SET industry='CORE'
  WHERE domain_code='inspection' AND (industry IS NULL OR industry='');
UPDATE data_resources SET industry='HEALTH'
  WHERE domain_code IN ('healthcare','health') AND (industry IS NULL OR industry='');
UPDATE data_resources SET industry='CARE'
  WHERE domain_code IN ('care','elderly') AND (industry IS NULL OR industry='');
-- 兜底: 仍为空的置 CUSTOM
UPDATE data_resources SET industry='CUSTOM'
  WHERE industry IS NULL OR industry='';

-- 验证 (日志, 不影响 apply)
SELECT domain_code, industry, COUNT(*) n FROM data_resources GROUP BY domain_code, industry ORDER BY industry, domain_code;
