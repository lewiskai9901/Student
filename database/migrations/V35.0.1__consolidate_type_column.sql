-- Phase 1.2: Consolidate type_code / unit_type into single column
-- Keep unit_type as canonical (already mapped in OrgUnitPO.java)
-- Drop type_code column, rebuild classes VIEW

-- 1. Sync: copy type_code into unit_type where unit_type is empty
UPDATE org_units SET unit_type = type_code
WHERE (unit_type IS NULL OR unit_type = '') AND type_code IS NOT NULL AND type_code != '';

-- 2. Sync reverse: ensure type_code matches unit_type
UPDATE org_units SET type_code = unit_type
WHERE (type_code IS NULL OR type_code = '') AND unit_type IS NOT NULL AND unit_type != '';

-- 3. For any remaining rows, prefer type_code (newer, more accurate)
UPDATE org_units SET unit_type = type_code
WHERE type_code IS NOT NULL AND type_code != '' AND unit_type != type_code;

-- 4. Drop index on type_code (ignore error if not exists)
-- ALTER TABLE org_units DROP INDEX idx_org_units_type_code;

-- 5. Drop the type_code column (this also drops the index)
ALTER TABLE org_units DROP COLUMN type_code;

-- 6. Rebuild classes VIEW using unit_type instead of type_code
DROP VIEW IF EXISTS classes;
CREATE VIEW classes AS
SELECT
  o.id,
  o.unit_name AS class_name,
  o.unit_code AS class_code,
  o.id AS org_unit_id,
  COALESCE(CAST(JSON_UNQUOTE(JSON_EXTRACT(o.attributes, '$.gradeLevel')) AS UNSIGNED), 1) AS grade_level,
  CAST(JSON_UNQUOTE(JSON_EXTRACT(o.attributes, '$.gradeId')) AS UNSIGNED) AS grade_id,
  CAST(JSON_UNQUOTE(JSON_EXTRACT(o.attributes, '$.majorId')) AS UNSIGNED) AS major_id,
  CAST(JSON_UNQUOTE(JSON_EXTRACT(o.attributes, '$.headTeacher')) AS UNSIGNED) AS teacher_id,
  COALESCE(CAST(JSON_UNQUOTE(JSON_EXTRACT(o.attributes, '$.studentCount')) AS UNSIGNED), 0) AS student_count,
  COALESCE(CAST(JSON_UNQUOTE(JSON_EXTRACT(o.attributes, '$.classType')) AS UNSIGNED), 1) AS class_type,
  CASE o.status WHEN 'ACTIVE' THEN 1 ELSE 0 END AS status,
  o.created_at,
  o.updated_at,
  o.deleted,
  o.tenant_id
FROM org_units o WHERE o.unit_type = 'CLASS';
