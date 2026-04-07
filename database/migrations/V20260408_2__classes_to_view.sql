-- =============================================================
-- 彻底清理: 将 classes 表转为 org_units 的 VIEW
-- 实现单一数据源，47张表的 class_id 无需改动
-- =============================================================

-- Step 1: 删除之前偏移 ID 创建的 org_units
DELETE FROM org_units WHERE id >= 9000000000000000000;

-- Step 2: 将 classes 数据插入 org_units (使用相同的 ID)
INSERT INTO org_units (id, unit_code, unit_name, unit_type, type_code, parent_id, tree_level,
  status, attributes, created_at, updated_at, deleted, tenant_id)
SELECT
  c.id,
  c.class_code,
  c.class_name,
  'CLASS',
  'CLASS',
  COALESCE(c.org_unit_id, (SELECT id FROM org_units WHERE parent_id IS NULL AND deleted=0 LIMIT 1)),
  3,
  CASE c.status WHEN 1 THEN 'ACTIVE' WHEN 2 THEN 'FROZEN' ELSE 'ACTIVE' END,
  JSON_OBJECT(
    'enrollmentYear', CAST(c.enrollment_year AS UNSIGNED),
    'majorId', c.major_id,
    'majorDirectionId', c.major_direction_id,
    'headTeacher', c.teacher_id,
    'assistantTeacher', c.assistant_teacher_id,
    'duration', COALESCE(c.duration, 3),
    'classType', COALESCE(c.class_type, 1),
    'gradeLevel', c.grade_level,
    'gradeId', c.grade_id,
    'studentCount', COALESCE(c.student_count, 0),
    'classroomLocation', c.classroom_location,
    'educationSystem', c.education_system,
    'graduationYear', CAST(c.graduation_year AS UNSIGNED)
  ),
  COALESCE(c.created_at, NOW()),
  COALESCE(c.updated_at, NOW()),
  c.deleted,
  c.tenant_id
FROM classes c
WHERE NOT EXISTS (SELECT 1 FROM org_units o WHERE o.id = c.id);

-- Step 3: 重命名原 classes 表为备份
RENAME TABLE classes TO _classes_backup_20260408;

-- Step 4: 创建 classes VIEW (兼容所有现有查询)
CREATE VIEW classes AS
SELECT
  o.id,
  o.unit_name AS class_name,
  o.unit_code AS class_code,
  COALESCE(CAST(JSON_UNQUOTE(JSON_EXTRACT(o.attributes, '$.gradeLevel')) AS UNSIGNED), 1) AS grade_level,
  o.parent_id AS org_unit_id,
  CAST(JSON_UNQUOTE(JSON_EXTRACT(o.attributes, '$.gradeId')) AS UNSIGNED) AS grade_id,
  CAST(JSON_UNQUOTE(JSON_EXTRACT(o.attributes, '$.majorId')) AS UNSIGNED) AS major_id,
  CAST(JSON_UNQUOTE(JSON_EXTRACT(o.attributes, '$.majorDirectionId')) AS UNSIGNED) AS major_direction_id,
  NULL AS class_sequence,
  CAST(JSON_UNQUOTE(JSON_EXTRACT(o.attributes, '$.headTeacher')) AS UNSIGNED) AS teacher_id,
  CAST(JSON_UNQUOTE(JSON_EXTRACT(o.attributes, '$.assistantTeacher')) AS UNSIGNED) AS assistant_teacher_id,
  COALESCE(CAST(JSON_UNQUOTE(JSON_EXTRACT(o.attributes, '$.studentCount')) AS UNSIGNED), 0) AS student_count,
  JSON_UNQUOTE(JSON_EXTRACT(o.attributes, '$.classroomLocation')) AS classroom_location,
  YEAR(NOW()) AS enrollment_year,
  JSON_UNQUOTE(JSON_EXTRACT(o.attributes, '$.educationSystem')) AS education_system,
  NULL AS skill_level,
  CAST(JSON_UNQUOTE(JSON_EXTRACT(o.attributes, '$.duration')) AS UNSIGNED) AS duration,
  CAST(JSON_UNQUOTE(JSON_EXTRACT(o.attributes, '$.graduationYear')) AS UNSIGNED) AS graduation_year,
  COALESCE(CAST(JSON_UNQUOTE(JSON_EXTRACT(o.attributes, '$.classType')) AS UNSIGNED), 1) AS class_type,
  CASE o.status WHEN 'ACTIVE' THEN 1 WHEN 'FROZEN' THEN 0 ELSE 1 END AS status,
  0 AS is_international,
  0 AS is_experimental,
  0 AS is_oriented,
  o.created_at,
  o.updated_at,
  o.created_by,
  o.updated_by,
  o.deleted,
  o.tenant_id
FROM org_units o
WHERE o.type_code = 'CLASS';
