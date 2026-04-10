-- Phase 1.1: Fix soft-delete + unique constraint conflict
-- Problem: uk_unit_code(unit_code) blocks reuse of deleted codes
-- Solution: Use deleted=id pattern (same as users table V30.0.0)

-- 1. Widen deleted column from TINYINT to BIGINT
ALTER TABLE org_units MODIFY COLUMN deleted BIGINT DEFAULT 0;

-- 2. Fix existing soft-deleted rows: set deleted = id
UPDATE org_units SET deleted = id WHERE deleted = 1;

-- 3. Drop old unique, recreate with deleted
ALTER TABLE org_units DROP INDEX uk_unit_code;
ALTER TABLE org_units ADD UNIQUE KEY uk_unit_code_deleted (unit_code, deleted);
