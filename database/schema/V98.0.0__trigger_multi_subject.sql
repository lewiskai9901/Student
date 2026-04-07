-- ============================================================
-- V98.0.0 Trigger Multi-Subject Support
-- - Remove dead columns: related_sources, payload_fields
-- - Replace single subject (subject_type/subject_source/subject_name_source)
--   with subjects_json array for multi-subject event generation
-- - Update existing seed triggers to new format
-- ============================================================

-- 1. Add subjects_json column
ALTER TABLE event_triggers
    ADD COLUMN subjects_json JSON COMMENT '主体配置数组 [{type,idSource,nameSource}]';

-- 2. Migrate existing data: single subject → subjects_json array
UPDATE event_triggers
SET subjects_json = JSON_ARRAY(
    JSON_OBJECT(
        'type', COALESCE(subject_type, 'USER'),
        'idSource', COALESCE(subject_source, ''),
        'nameSource', COALESCE(subject_name_source, '')
    )
)
WHERE subjects_json IS NULL AND deleted = 0;

-- 3. Drop old columns
ALTER TABLE event_triggers
    DROP COLUMN subject_type,
    DROP COLUMN subject_source,
    DROP COLUMN subject_name_source,
    DROP COLUMN related_sources,
    DROP COLUMN payload_fields;

-- 4. Update seed triggers to multi-subject where applicable
-- "检查扣分→学生事件" should also record to class
UPDATE event_triggers
SET name = '检查扣分→学生+班级事件',
    subjects_json = '[{"type":"USER","idSource":"studentId","nameSource":"studentName"},{"type":"ORG_UNIT","idSource":"classId","nameSource":"className"}]',
    description = '检查项扣分时同时记录到学生和班级'
WHERE name COLLATE utf8mb4_unicode_ci = '检查扣分→学生事件' AND deleted = 0;

-- "考勤异常→学生事件" should also record to class
UPDATE event_triggers
SET name = '考勤异常→学生+班级事件',
    subjects_json = '[{"type":"USER","idSource":"studentId","nameSource":"studentName"},{"type":"ORG_UNIT","idSource":"classId","nameSource":"className"}]',
    description = '迟到/早退/旷课时同时记录到学生和班级'
WHERE name COLLATE utf8mb4_unicode_ci = '考勤异常→学生事件' AND deleted = 0;
