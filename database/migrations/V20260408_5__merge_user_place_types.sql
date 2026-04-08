-- 合并 user_types + place_types → entity_type_configs
-- 插件已注册的(TEACHER/STUDENT/CLASSROOM)跳过，只迁管理员类型

-- ========== USER 类型迁入 ==========
INSERT INTO entity_type_configs (entity_type, type_code, type_name, category,
  metadata_schema, features, is_plugin_registered, is_enabled, deleted)
SELECT 'USER', ut.type_code, ut.type_name, COALESCE(ut.category, 'OTHER'),
  '{"fields":[]}', '{}', 0, 1, 0
FROM user_types ut
WHERE ut.deleted = 0
AND NOT EXISTS (
  SELECT 1 FROM entity_type_configs etc
  WHERE etc.entity_type = 'USER' AND etc.type_code = ut.type_code AND etc.deleted = 0
);

-- ========== PLACE 类型迁入 ==========
INSERT INTO entity_type_configs (entity_type, type_code, type_name, category,
  parent_type_code, metadata_schema, features, is_plugin_registered, is_enabled, deleted)
SELECT 'PLACE', pt.type_code, pt.type_name, 'SPACE',
  pt.parent_type_code,
  '{"fields":[]}', '{}', 0, 1, 0
FROM place_types pt
WHERE pt.deleted = 0
AND NOT EXISTS (
  SELECT 1 FROM entity_type_configs etc
  WHERE etc.entity_type = 'PLACE' AND etc.type_code = pt.type_code AND etc.deleted = 0
);

-- place_type_config 的额外配置也迁入(作为 features)
UPDATE entity_type_configs etc
JOIN place_type_config ptc ON ptc.type_code = etc.type_code
SET etc.features = JSON_OBJECT(
  'hasCapacity', ptc.has_capacity = 1,
  'hasOccupancy', ptc.has_occupancy = 1,
  'hasGender', ptc.has_gender = 1
),
etc.ui_config = JSON_OBJECT(
  'icon', COALESCE(ptc.icon, 'building'),
  'color', COALESCE(ptc.color, 'gray')
)
WHERE etc.entity_type = 'PLACE' AND etc.deleted = 0;
