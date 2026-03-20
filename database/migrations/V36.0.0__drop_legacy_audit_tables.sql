-- V36.0.0: Drop legacy audit/log tables replaced by unified activity_events
-- These tables are no longer used after the Unified Activity Event Framework migration

-- 1. org_change_logs - replaced by activity_events with module='organization'
DROP TABLE IF EXISTS org_change_logs;

-- 2. audit_logs - replaced by activity_events with @AuditEvent annotation
DROP TABLE IF EXISTS audit_logs;

-- 3. operation_logs - never had a working backend, now unified
DROP TABLE IF EXISTS operation_logs;

-- 4. place_audit_logs - replaced by activity_events with module='place'
DROP TABLE IF EXISTS place_audit_logs;

-- 5. check_audit_logs - orphan table (schema only, no code)
DROP TABLE IF EXISTS check_audit_logs;

-- 6. appeal_audit_logs - orphan table (schema only, no code)
DROP TABLE IF EXISTS appeal_audit_logs;

-- 7. rating_change_logs - orphan table (schema only, no code)
DROP TABLE IF EXISTS rating_change_logs;
