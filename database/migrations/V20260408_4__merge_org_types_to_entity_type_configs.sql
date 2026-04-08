-- 合并 org_unit_types → entity_type_configs
-- 插件类型由代码注册(SCHOOL/DEPARTMENT/GRADE/CLASS已有)
-- 管理员类型手动迁入(ADMIN_OFFICE/TEACHING_GROUP/SECTION)

-- 迁入管理员创建的类型(无插件支撑)
INSERT INTO entity_type_configs (entity_type, type_code, type_name, category, parent_type_code,
  allowed_child_type_codes, metadata_schema, features, ui_config,
  is_plugin_registered, is_enabled, deleted)
SELECT 'ORG_UNIT', ot.type_code, ot.type_name, ot.category, ot.parent_type_code,
  COALESCE(ot.allowed_child_type_codes, '[]'),
  COALESCE(ot.metadata_schema, '{"fields":[]}'),
  COALESCE(ot.features, '{}'),
  '{}',
  0, ot.is_enabled, 0
FROM org_unit_types ot
WHERE ot.deleted = 0
AND NOT EXISTS (
  SELECT 1 FROM entity_type_configs etc
  WHERE etc.entity_type = 'ORG_UNIT' AND etc.type_code = ot.type_code AND etc.deleted = 0
);

-- 清理 org_unit_types 中的重复数据(UT_MLNB7BUT/UT_MLNB8R9H 是旧的自定义类型,已被插件替代)
-- 不删表,只标记弃用
