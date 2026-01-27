-- 修复权限层级关系脚本
-- 将 parent_id 为 0 或 NULL 的权限正确挂载到父节点

-- ========== 1. 创建缺失的父节点权限 ==========

-- 创建 scope (数据范围) 父节点
INSERT IGNORE INTO permissions (id, permission_code, permission_name, permission_desc, resource_type, parent_id, sort_order, status, deleted)
SELECT 1992425216839577860, 'scope', '数据范围管理', '数据范围相关权限', 1, 0, 100, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_code = 'scope' AND deleted = 0);

-- 创建 wechat (微信管理) 父节点
INSERT IGNORE INTO permissions (id, permission_code, permission_name, permission_desc, resource_type, parent_id, sort_order, status, deleted)
SELECT 1992425216839577861, 'wechat', '微信管理', '微信推送相关权限', 1, 0, 80, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_code = 'wechat' AND deleted = 0);

-- ========== 2. 修复 quantification (量化管理) 下的权限 ==========

-- 获取 quantification 的 ID
SET @quantification_id = (SELECT id FROM permissions WHERE permission_code = 'quantification' AND deleted = 0 LIMIT 1);

-- 修复直接子权限 (quantification:xxx 格式，且 parent_id 为 0 或 NULL)
UPDATE permissions
SET parent_id = @quantification_id
WHERE permission_code LIKE 'quantification:%'
  AND permission_code NOT LIKE 'quantification:%:%'
  AND (parent_id IS NULL OR parent_id = 0)
  AND deleted = 0;

-- 修复 quantification:check:* 权限 - 先找到或创建 quantification:check 父节点
SET @quant_check_id = (SELECT id FROM permissions WHERE permission_code = 'quantification:check' AND deleted = 0 LIMIT 1);
-- 如果不存在，创建它
INSERT IGNORE INTO permissions (id, permission_code, permission_name, permission_desc, resource_type, parent_id, sort_order, status, deleted)
SELECT 1992425216839577870, 'quantification:check', '日常检查', '日常检查管理', 1, @quantification_id, 10, 1, 0
WHERE @quant_check_id IS NULL AND @quantification_id IS NOT NULL;
SET @quant_check_id = COALESCE(@quant_check_id, 1992425216839577870);

UPDATE permissions
SET parent_id = @quant_check_id
WHERE permission_code LIKE 'quantification:check:%'
  AND (parent_id IS NULL OR parent_id = 0)
  AND deleted = 0;

-- 修复 quantification:template:* 权限
SET @quant_template_id = (SELECT id FROM permissions WHERE permission_code = 'quantification:template' AND deleted = 0 LIMIT 1);
INSERT IGNORE INTO permissions (id, permission_code, permission_name, permission_desc, resource_type, parent_id, sort_order, status, deleted)
SELECT 1992425216839577871, 'quantification:template', '检查模板', '检查模板管理', 1, @quantification_id, 20, 1, 0
WHERE @quant_template_id IS NULL AND @quantification_id IS NOT NULL;
SET @quant_template_id = COALESCE(@quant_template_id, 1992425216839577871);

UPDATE permissions
SET parent_id = @quant_template_id
WHERE permission_code LIKE 'quantification:template:%'
  AND (parent_id IS NULL OR parent_id = 0)
  AND deleted = 0;

-- 修复 quantification:config:* 权限
SET @quant_config_id = (SELECT id FROM permissions WHERE permission_code = 'quantification:config' AND deleted = 0 LIMIT 1);
INSERT IGNORE INTO permissions (id, permission_code, permission_name, permission_desc, resource_type, parent_id, sort_order, status, deleted)
SELECT 1992425216839577872, 'quantification:config', '量化配置', '量化配置管理', 1, @quantification_id, 30, 1, 0
WHERE @quant_config_id IS NULL AND @quantification_id IS NOT NULL;
SET @quant_config_id = COALESCE(@quant_config_id, 1992425216839577872);

UPDATE permissions
SET parent_id = @quant_config_id
WHERE permission_code LIKE 'quantification:config:%'
  AND (parent_id IS NULL OR parent_id = 0)
  AND deleted = 0;

-- 修复 quantification:statistics:* 权限
SET @quant_stats_id = (SELECT id FROM permissions WHERE permission_code = 'quantification:statistics' AND deleted = 0 LIMIT 1);
INSERT IGNORE INTO permissions (id, permission_code, permission_name, permission_desc, resource_type, parent_id, sort_order, status, deleted)
SELECT 1992425216839577873, 'quantification:statistics', '统计分析', '统计分析管理', 1, @quantification_id, 40, 1, 0
WHERE @quant_stats_id IS NULL AND @quantification_id IS NOT NULL;
SET @quant_stats_id = COALESCE(@quant_stats_id, 1992425216839577873);

UPDATE permissions
SET parent_id = @quant_stats_id
WHERE permission_code LIKE 'quantification:statistics:%'
  AND (parent_id IS NULL OR parent_id = 0)
  AND deleted = 0;

-- 修复 quantification:appeal:* 权限 (V3的也要处理)
SET @quant_appeal_id = (SELECT id FROM permissions WHERE permission_code = 'quantification:appeal' AND deleted = 0 LIMIT 1);
INSERT IGNORE INTO permissions (id, permission_code, permission_name, permission_desc, resource_type, parent_id, sort_order, status, deleted)
SELECT 1992425216839577874, 'quantification:appeal', '申诉管理', '申诉管理', 1, @quantification_id, 50, 1, 0
WHERE @quant_appeal_id IS NULL AND @quantification_id IS NOT NULL;
SET @quant_appeal_id = COALESCE(@quant_appeal_id, 1992425216839577874);

UPDATE permissions
SET parent_id = @quant_appeal_id
WHERE (permission_code LIKE 'quantification:appeal:%')
  AND (parent_id IS NULL OR parent_id = 0)
  AND deleted = 0;

-- 修复 quantification:record:* 和 quantification:record:v3:*
SET @quant_record_id = (SELECT id FROM permissions WHERE permission_code = 'quantification:record' AND deleted = 0 LIMIT 1);

UPDATE permissions
SET parent_id = @quant_record_id
WHERE (permission_code LIKE 'quantification:record:v3:%')
  AND (parent_id IS NULL OR parent_id = 0)
  AND deleted = 0
  AND @quant_record_id IS NOT NULL;

-- 修复 quantification:weight:* 权限
UPDATE permissions
SET parent_id = @quantification_id
WHERE permission_code LIKE 'quantification:weight:%'
  AND (parent_id IS NULL OR parent_id = 0)
  AND deleted = 0;

-- ========== 3. 修复 dormitory (宿舍管理) 下的权限 ==========

SET @dormitory_id = (SELECT id FROM permissions WHERE permission_code = 'dormitory:manage' AND deleted = 0 LIMIT 1);

UPDATE permissions
SET parent_id = @dormitory_id
WHERE permission_code LIKE 'dormitory:%'
  AND permission_code != 'dormitory:manage'
  AND (parent_id IS NULL OR parent_id = 0)
  AND deleted = 0;

-- ========== 4. 修复 system (系统管理) 下的权限 ==========

SET @system_id = (SELECT id FROM permissions WHERE permission_code = 'system:manage' AND deleted = 0 LIMIT 1);

-- system:config 挂到 system:manage 下
UPDATE permissions
SET parent_id = @system_id
WHERE permission_code = 'system:config'
  AND (parent_id IS NULL OR parent_id = 0)
  AND deleted = 0;

-- system:operlog 挂到 system:manage 下
UPDATE permissions
SET parent_id = @system_id
WHERE permission_code = 'system:operlog'
  AND (parent_id IS NULL OR parent_id = 0)
  AND deleted = 0;

-- system:announcement 挂到 system:manage 下
UPDATE permissions
SET parent_id = @system_id
WHERE permission_code = 'system:announcement'
  AND (parent_id IS NULL OR parent_id = 0)
  AND deleted = 0;

-- ========== 5. 修复 student (学生管理) 下的权限 ==========

SET @student_id = (SELECT id FROM permissions WHERE permission_code = 'student:manage' AND deleted = 0 LIMIT 1);

UPDATE permissions
SET parent_id = @student_id
WHERE permission_code = 'student:view'
  AND (parent_id IS NULL OR parent_id = 0)
  AND deleted = 0;

-- ========== 6. 修复 scope (数据范围) 下的权限 ==========

SET @scope_id = (SELECT id FROM permissions WHERE permission_code = 'scope' AND deleted = 0 LIMIT 1);

UPDATE permissions
SET parent_id = @scope_id
WHERE permission_code LIKE 'scope:%'
  AND (parent_id IS NULL OR parent_id = 0)
  AND deleted = 0
  AND @scope_id IS NOT NULL;

-- ========== 7. 修复 wechat (微信) 下的权限 ==========

SET @wechat_id = (SELECT id FROM permissions WHERE permission_code = 'wechat' AND deleted = 0 LIMIT 1);

UPDATE permissions
SET parent_id = @wechat_id
WHERE permission_code LIKE 'wechat:%'
  AND (parent_id IS NULL OR parent_id = 0)
  AND deleted = 0
  AND @wechat_id IS NOT NULL;

-- ========== 8. 修复 major (专业管理) 下的权限 ==========

SET @major_id = (SELECT id FROM permissions WHERE permission_code = 'major:manage' AND deleted = 0 LIMIT 1);

UPDATE permissions
SET parent_id = @major_id
WHERE permission_code = 'major:direction'
  AND (parent_id IS NULL OR parent_id = 0)
  AND deleted = 0
  AND @major_id IS NOT NULL;

-- ========== 9. 修复 grade (年级管理) 下的权限 ==========
-- grade:direction 目前没有父节点，创建 grade 父节点

INSERT IGNORE INTO permissions (id, permission_code, permission_name, permission_desc, resource_type, parent_id, sort_order, status, deleted)
SELECT 1992425216839577865, 'grade', '年级管理', '年级相关权限', 1, 21, 40, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_code = 'grade' AND deleted = 0);

SET @grade_id = (SELECT id FROM permissions WHERE permission_code = 'grade' AND deleted = 0 LIMIT 1);

UPDATE permissions
SET parent_id = @grade_id
WHERE permission_code LIKE 'grade:%'
  AND (parent_id IS NULL OR parent_id = 0)
  AND deleted = 0
  AND @grade_id IS NOT NULL;

-- ========== 验证结果 ==========
SELECT
  p.id,
  p.permission_code,
  p.permission_name,
  p.parent_id,
  pp.permission_code as parent_code,
  pp.permission_name as parent_name
FROM permissions p
LEFT JOIN permissions pp ON p.parent_id = pp.id AND pp.deleted = 0
WHERE p.deleted = 0
ORDER BY p.permission_code;
