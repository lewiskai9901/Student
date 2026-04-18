-- ============================================================
-- Role permissions 迁移: role_data_permissions_v5 + data_modules 数据
--                        → role_data_scopes + data_resources
--
-- role_data_scopes 是 v3 统一权限表, role_data_permissions_v5 是 v5 遗留。
-- 为让未来的 DataPermissionPolicyService 重写能直接读新表,先同步数据。
--
-- data_modules.module_code 和 data_resources.resource_code 多数同名,
-- 缺失的通过 INSERT ... 补齐以保证 1:1 映射不丢数据。
-- ============================================================

-- ------------------------------------------------------------
-- Step 1: 补齐 data_resources 中缺失的 resource_code
--         让所有 role_data_permissions_v5.module_code 都有对应 resource
-- ------------------------------------------------------------
INSERT IGNORE INTO data_resources
  (resource_code, resource_name, domain_code, domain_name, registered_by, sort_order, enabled, tenant_id)
VALUES
  -- 教育业务补齐
  ('school_class',      '班级',        'education',  '教育',     'EducationPlugin',  11, 1, 1),
  ('teaching_task',     '教学任务',    'education',  '教育',     'EducationPlugin',  23, 1, 1),
  ('student_grade',     '学生成绩',    'education',  '教育',     'EducationPlugin',  24, 1, 1),
  ('exam_batch',        '考试批次',    'education',  '教育',     'EducationPlugin',  25, 1, 1),
  ('dashboard',         '仪表盘',      'CORE',        '核心',     'CORE',             40, 1, 1),
  -- 宿舍子模块
  ('dormitory_building','宿舍楼',      'education',  '教育',     'EducationPlugin',  50, 1, 1),
  ('dormitory_checkin', '住宿登记',    'education',  '教育',     'EducationPlugin',  51, 1, 1),
  ('dormitory_room',    '宿舍房间',    'education',  '教育',     'EducationPlugin',  52, 1, 1),
  -- 检查平台子模块
  ('inspection_appeal',     '检查申诉',    'inspection', '检查平台', 'InspectionPlugin', 31, 1, 1),
  ('inspection_corrective', '整改任务',    'inspection', '检查平台', 'InspectionPlugin', 32, 1, 1),
  ('inspection_personal',   '个人评级',    'inspection', '检查平台', 'InspectionPlugin', 33, 1, 1),
  ('inspection_project',    '检查项目',    'inspection', '检查平台', 'InspectionPlugin', 34, 1, 1),
  ('inspection_record',     '检查记录',    'inspection', '检查平台', 'InspectionPlugin', 35, 1, 1),
  ('inspection_summary',    '检查汇总',    'inspection', '检查平台', 'InspectionPlugin', 36, 1, 1),
  ('inspection_task',       '检查任务',    'inspection', '检查平台', 'InspectionPlugin', 37, 1, 1),
  ('inspection_template',   '检查模板',    'inspection', '检查平台', 'InspectionPlugin', 38, 1, 1),
  -- 系统管理
  ('system_role',           '系统角色',    'CORE',       '核心',     'CORE',             45, 1, 1),
  ('system_user',           '系统用户',    'CORE',       '核心',     'CORE',             46, 1, 1);

-- ------------------------------------------------------------
-- Step 2: 迁移数据 role_data_permissions_v5 → role_data_scopes
--   module_code → resource_code (同名)
--   scope_code  → scope_type    (同名)
--   没有 custom_org_unit_ids (v5 用独立 role_data_scope_items 表, 此处暂丢)
-- ------------------------------------------------------------
-- v5 表使用 utf8mb4_0900_ai_ci, 新表使用 utf8mb4_unicode_ci, 需显式 COLLATE 对齐
INSERT INTO role_data_scopes
  (role_id, resource_code, scope_type, priority, tenant_id, created_at, deleted)
SELECT
  v5.role_id,
  CONVERT(v5.module_code USING utf8mb4) COLLATE utf8mb4_unicode_ci AS resource_code,
  CONVERT(v5.scope_code  USING utf8mb4) COLLATE utf8mb4_unicode_ci AS scope_type,
  0 AS priority,
  COALESCE(v5.tenant_id, 1),
  NOW(),
  0
FROM role_data_permissions_v5 v5
WHERE NOT EXISTS (
  SELECT 1 FROM role_data_scopes ts
  WHERE ts.role_id = v5.role_id
    AND ts.resource_code COLLATE utf8mb4_unicode_ci =
        CONVERT(v5.module_code USING utf8mb4) COLLATE utf8mb4_unicode_ci
    AND ts.tenant_id = COALESCE(v5.tenant_id, 1)
);

-- ------------------------------------------------------------
-- 验证
-- ------------------------------------------------------------
-- SELECT 'role_data_scopes' AS t, COUNT(*) FROM role_data_scopes
-- UNION ALL SELECT 'data_resources', COUNT(*) FROM data_resources;
-- 预期: role_data_scopes >= 34, data_resources >= 28
