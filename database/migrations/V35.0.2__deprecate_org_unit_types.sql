-- Phase 1.3: Unify type configuration to entity_type_configs as single source
-- org_unit_types data already merged via V20260408_4
-- Ensure completeness then deprecate old table

-- 1. Ensure any remaining org_unit_types entries are in entity_type_configs
INSERT IGNORE INTO entity_type_configs (entity_type, type_code, type_name, category, parent_type_code,
  metadata_schema, features, allowed_child_type_codes, is_system, is_enabled, sort_order)
SELECT 'ORG_UNIT', ot.type_code, ot.type_name, ot.category, ot.parent_type_code,
  COALESCE(ot.metadata_schema, '{"fields": []}'),
  COALESCE(ot.features, '{}'),
  ot.allowed_child_type_codes,
  ot.is_system, ot.is_enabled, ot.sort_order
FROM org_unit_types ot
WHERE ot.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM entity_type_configs etc
    WHERE etc.entity_type = 'ORG_UNIT' AND etc.type_code = ot.type_code AND etc.deleted = 0
  );

-- 2. Rename old table (safe deprecation, not drop)
RENAME TABLE org_unit_types TO _org_unit_types_deprecated;
