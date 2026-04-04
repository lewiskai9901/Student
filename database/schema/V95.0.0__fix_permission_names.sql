-- V95.0.0 Fix garbled permission names (mojibake from encoding mismatch)
-- These 7 permissions had names stored as double-encoded UTF-8

UPDATE permissions SET permission_name = '数据分析' WHERE permission_code = 'analytics:view' AND deleted = 0;
UPDATE permissions SET permission_name = '申诉创建' WHERE permission_code = 'inspection:appeal:create' AND deleted = 0;
UPDATE permissions SET permission_name = '申诉审核' WHERE permission_code = 'inspection:appeal:review' AND deleted = 0;
UPDATE permissions SET permission_name = '申诉查看' WHERE permission_code = 'inspection:appeal:view' AND deleted = 0;
UPDATE permissions SET permission_name = '创建导出' WHERE permission_code = 'inspection:export:create' AND deleted = 0;
UPDATE permissions SET permission_name = '排班管理' WHERE permission_code = 'schedule:policy:view' AND deleted = 0;
UPDATE permissions SET permission_name = '排班策略管理' WHERE permission_code = 'schedule:policy:manage' AND deleted = 0;

-- Also fix English-only names for V7 platform top-level permissions
UPDATE permissions SET permission_name = 'V7检查平台管理' WHERE permission_code = 'insp:platform:manage' AND deleted = 0;
UPDATE permissions SET permission_name = 'V7检查平台查看' WHERE permission_code = 'insp:platform:view' AND deleted = 0;
