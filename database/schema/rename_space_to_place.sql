-- =====================================================
-- Migration: Rename "space" to "place" across all tables
-- Date: 2026-02-08
-- Description:
--   Renames all tables and columns that use "space" to
--   use "place" instead, to better reflect the domain
--   concept of physical places/locations.
--
-- Note: This script is designed to be run once. Running it
-- a second time will produce errors (tables/columns already
-- renamed).
-- =====================================================

SET NAMES utf8mb4;

-- =====================================================
-- Part A: Legacy tables (from complete_schema_v2.sql)
-- =====================================================

-- A1. Rename columns in `space` table
ALTER TABLE `space`
    CHANGE COLUMN `space_code` `place_code` VARCHAR(50) COMMENT '场所编码',
    CHANGE COLUMN `space_name` `place_name` VARCHAR(100) NOT NULL COMMENT '场所名称',
    CHANGE COLUMN `space_type` `place_type` VARCHAR(20) COMMENT '场所类型';

-- A2. Rename indexes on `space` table (MySQL needs DROP+ADD)
ALTER TABLE `space` DROP INDEX IF EXISTS `idx_space_code`, ADD INDEX `idx_place_code` (`place_code`);
ALTER TABLE `space` DROP INDEX IF EXISTS `idx_space_type`, ADD INDEX `idx_place_type` (`place_type`);

-- A3. Rename columns in `space_occupant` table
ALTER TABLE `space_occupant`
    CHANGE COLUMN `space_id` `place_id` BIGINT NOT NULL COMMENT '场所ID';
ALTER TABLE `space_occupant` DROP INDEX IF EXISTS `idx_space_id`, ADD INDEX `idx_place_id` (`place_id`);

-- A4. Rename columns in `space_class_assignment` table
ALTER TABLE `space_class_assignment`
    CHANGE COLUMN `space_id` `place_id` BIGINT NOT NULL COMMENT '场所ID';
ALTER TABLE `space_class_assignment` DROP INDEX IF EXISTS `uk_space_class`,
    ADD UNIQUE KEY `uk_place_class` (`place_id`, `class_id`);

-- A5. Rename columns in `inspection_deductions` table (table not renamed)
ALTER TABLE `inspection_deductions`
    CHANGE COLUMN `space_type` `place_type` VARCHAR(20) COMMENT '场所类型',
    CHANGE COLUMN `space_id` `place_id` BIGINT COMMENT '场所ID',
    CHANGE COLUMN `space_name` `place_name` VARCHAR(100) COMMENT '场所名称';

-- A6. Rename legacy tables
ALTER TABLE `space` RENAME TO `place`;
ALTER TABLE `space_occupant` RENAME TO `place_occupant`;
ALTER TABLE `space_class_assignment` RENAME TO `place_class_assignment`;


-- =====================================================
-- Part B: Universal/V10 tables (if they exist)
-- Run these only if your database has the v10 tables.
-- Comment out any that don't apply to your environment.
-- =====================================================

-- B1. Rename `spaces` -> `places` (universal places table)
ALTER TABLE `spaces`
    CHANGE COLUMN `space_code` `place_code` VARCHAR(50) COMMENT '场所编码',
    CHANGE COLUMN `space_name` `place_name` VARCHAR(100) COMMENT '场所名称';
ALTER TABLE `spaces` RENAME TO `places`;

-- B2. Rename `space_types` -> `place_types`
ALTER TABLE `space_types` RENAME TO `place_types`;

-- B3. Rename `space_type_config` -> `place_type_config`
ALTER TABLE `space_type_config` RENAME TO `place_type_config`;

-- B4. Rename `space_categories` -> `place_categories`
ALTER TABLE `space_categories` RENAME TO `place_categories`;

-- B5. Rename `space_occupants` -> `place_occupants` (universal occupants)
ALTER TABLE `space_occupants`
    CHANGE COLUMN `space_id` `place_id` BIGINT NOT NULL COMMENT '场所ID';
ALTER TABLE `space_occupants` RENAME TO `place_occupants`;

-- B6. Rename `space_bookings` -> `place_bookings`
ALTER TABLE `space_bookings`
    CHANGE COLUMN `space_id` `place_id` BIGINT NOT NULL COMMENT '场所ID';
ALTER TABLE `space_bookings` RENAME TO `place_bookings`;

-- B7. Rename `space_org_relations` -> `place_org_relations`
ALTER TABLE `space_org_relations`
    CHANGE COLUMN `space_id` `place_id` BIGINT NOT NULL COMMENT '场所ID';
ALTER TABLE `space_org_relations` RENAME TO `place_org_relations`;

-- B8. Rename `space_org_relation_history` -> `place_org_relation_history`
ALTER TABLE `space_org_relation_history`
    CHANGE COLUMN `space_id` `place_id` BIGINT NOT NULL COMMENT '场所ID';
ALTER TABLE `space_org_relation_history` RENAME TO `place_org_relation_history`;

-- B9. Rename `user_space_relations` -> `user_place_relations`
ALTER TABLE `user_space_relations`
    CHANGE COLUMN `space_id` `place_id` BIGINT NOT NULL COMMENT '场所ID';
ALTER TABLE `user_space_relations` RENAME TO `user_place_relations`;

-- B10. Rename `user_space_relation_history` -> `user_place_relation_history`
ALTER TABLE `user_space_relation_history`
    CHANGE COLUMN `space_id` `place_id` BIGINT NOT NULL COMMENT '场所ID';
ALTER TABLE `user_space_relation_history` RENAME TO `user_place_relation_history`;


-- =====================================================
-- Part C: Enum value updates
-- Update SPACE -> PLACE in inspection scope/target types
-- =====================================================

-- C1. Update scope_type enum values in inspection tables
UPDATE `inspection_sessions` SET `scope_type` = 'PLACE' WHERE `scope_type` = 'SPACE';
UPDATE `inspection_records` SET `target_type` = 'PLACE' WHERE `target_type` = 'SPACE';

-- C2. Update inspection_deductions if it has a type column
UPDATE `inspection_deductions` SET `individual_type` = 'PLACE' WHERE `individual_type` = 'SPACE';


-- =====================================================
-- Part D: Permission identifiers
-- Update permission codes in the `permissions` table
-- =====================================================

-- D1. Rename space:* -> place:* permission codes
UPDATE `permissions` SET `permission_code` = REPLACE(`permission_code`, 'space:', 'place:'),
                         `permission_name` = REPLACE(`permission_name`, '空间', '场所')
WHERE `permission_code` LIKE 'space:%';

-- D2. Rename system:space-type:* -> system:place-type:* permission codes
UPDATE `permissions` SET `permission_code` = REPLACE(`permission_code`, 'system:space-type', 'system:place-type'),
                         `permission_name` = REPLACE(`permission_name`, '空间类型', '场所类型')
WHERE `permission_code` LIKE 'system:space-type%';

-- D3. Rename system:space-category:* -> system:place-category:* permission codes
UPDATE `permissions` SET `permission_code` = REPLACE(`permission_code`, 'system:space-category', 'system:place-category'),
                         `permission_name` = REPLACE(`permission_name`, '空间分类', '场所分类')
WHERE `permission_code` LIKE 'system:space-category%';

-- D4. Rename system:space:* -> system:place:* permission codes (PlaceTypeController uses these)
UPDATE `permissions` SET `permission_code` = REPLACE(`permission_code`, 'system:space', 'system:place'),
                         `permission_name` = REPLACE(`permission_name`, '空间', '场所')
WHERE `permission_code` LIKE 'system:space:%';

-- D5. Also update the `permission` table (singular) if it exists from V9/V10 migrations
UPDATE `permission` SET `code` = REPLACE(`code`, 'space:', 'place:'),
                        `name` = REPLACE(`name`, '空间', '场所')
WHERE `code` LIKE 'space:%';

UPDATE `permission` SET `code` = REPLACE(`code`, 'system:space-type', 'system:place-type'),
                        `name` = REPLACE(`name`, '空间类型', '场所类型')
WHERE `code` LIKE 'system:space-type%';

UPDATE `permission` SET `code` = REPLACE(`code`, 'system:space-category', 'system:place-category'),
                        `name` = REPLACE(`name`, '空间分类', '场所分类')
WHERE `code` LIKE 'system:space-category%';


-- =====================================================
-- Verification queries (run manually after migration):
-- SHOW TABLES LIKE 'place%';
-- SHOW TABLES LIKE 'space%';  -- should return empty
-- SHOW TABLES LIKE '%place%';
-- SELECT * FROM permissions WHERE permission_code LIKE 'place%' OR permission_code LIKE 'system:place%';
-- =====================================================
