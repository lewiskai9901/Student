-- V92.0.0: Add tenant_id to all application tables for multi-tenant support
-- Generated: 2026-04-03
-- Tables affected: 250 (excluding Activiti/Flowable system tables, casbin_rule, tenants)
-- Uses safe IF NOT EXISTS pattern via INFORMATION_SCHEMA check

-- Default tenant_id = 1 for existing data (single-tenant baseline)

-- ============================================================
-- Part 1: Add tenant_id column to all tables
-- ============================================================

-- academic_event
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'academic_event' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `academic_event` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- academic_warning_rules
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'academic_warning_rules' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `academic_warning_rules` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- academic_warnings
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'academic_warnings' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `academic_warnings` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- academic_weeks
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'academic_weeks' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `academic_weeks` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- academic_years
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'academic_years' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `academic_years` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- analysis_config_templates
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'analysis_config_templates' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `analysis_config_templates` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- analysis_configs
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'analysis_configs' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `analysis_configs` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- analysis_metrics
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'analysis_metrics' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `analysis_metrics` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- analysis_snapshots
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'analysis_snapshots' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `analysis_snapshots` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- announcement_reads
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'announcement_reads' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `announcement_reads` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- announcements
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'announcements' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `announcements` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- appeal_approval_configs
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'appeal_approval_configs' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `appeal_approval_configs` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- appeal_approval_records
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'appeal_approval_records' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `appeal_approval_records` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- appeal_audit_logs
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'appeal_audit_logs' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `appeal_audit_logs` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- appeal_configs
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'appeal_configs' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `appeal_configs` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- appeal_records
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'appeal_records' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `appeal_records` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- appeals_v2
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'appeals_v2' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `appeals_v2` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- archive_rules
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'archive_rules' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `archive_rules` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- asset
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'asset' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `asset` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- asset_alert
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'asset_alert' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `asset_alert` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- asset_approval
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'asset_approval' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `asset_approval` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- asset_borrow
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'asset_borrow' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `asset_borrow` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- asset_category
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'asset_category' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `asset_category` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- asset_depreciation
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'asset_depreciation' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `asset_depreciation` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- asset_history
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'asset_history' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `asset_history` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- asset_inventory
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'asset_inventory' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `asset_inventory` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- asset_inventory_detail
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'asset_inventory_detail' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `asset_inventory_detail` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- asset_maintenance
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'asset_maintenance' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `asset_maintenance` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- attendance_records
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'attendance_records' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `attendance_records` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- audit_logs
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'audit_logs' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `audit_logs` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- behavior_types
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'behavior_types' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `behavior_types` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- booking_seat_assignments
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'booking_seat_assignments' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `booking_seat_assignments` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- building_department_assignments
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'building_department_assignments' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `building_department_assignments` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- building_departments
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'building_departments' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `building_departments` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- building_dormitories
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'building_dormitories' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `building_dormitories` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- building_teachings
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'building_teachings' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `building_teachings` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- buildings
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'buildings' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `buildings` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- calculation_rules
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'calculation_rules' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `calculation_rules` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- category_mappings
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'category_mappings' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `category_mappings` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- category_weight_rules
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'category_weight_rules' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `category_weight_rules` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- check_appeals_deprecated
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_appeals_deprecated' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `check_appeals_deprecated` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- check_audit_logs
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_audit_logs' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `check_audit_logs` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- check_categories
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_categories' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `check_categories` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- check_export_templates
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_export_templates' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `check_export_templates` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- check_item_appeals
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_item_appeals' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `check_item_appeals` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- check_items
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_items' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `check_items` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- check_plan_inspectors
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_plan_inspectors' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `check_plan_inspectors` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- check_plan_period_config
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_plan_period_config' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `check_plan_period_config` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- check_plan_rating_audit_log
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_plan_rating_audit_log' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `check_plan_rating_audit_log` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- check_plan_rating_frequency
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_plan_rating_frequency' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `check_plan_rating_frequency` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- check_plan_rating_levels
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_plan_rating_levels' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `check_plan_rating_levels` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- check_plan_rating_results
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_plan_rating_results' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `check_plan_rating_results` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- check_plan_rating_rules
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_plan_rating_rules' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `check_plan_rating_rules` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- check_plan_rating_schedule
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_plan_rating_schedule' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `check_plan_rating_schedule` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- check_plan_rating_summary
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_plan_rating_summary' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `check_plan_rating_summary` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- check_plans
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_plans' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `check_plans` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- check_rating_configs
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_rating_configs' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `check_rating_configs` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- check_rating_configs_deprecated
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_rating_configs_deprecated' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `check_rating_configs_deprecated` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- check_rating_levels_deprecated
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_rating_levels_deprecated' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `check_rating_levels_deprecated` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- check_rating_results_deprecated
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_rating_results_deprecated' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `check_rating_results_deprecated` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- check_record_appeals_new
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_record_appeals_new' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `check_record_appeals_new` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- check_record_category_stats_deprecated
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_record_category_stats_deprecated' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `check_record_category_stats_deprecated` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- check_record_category_stats_new
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_record_category_stats_new' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `check_record_category_stats_new` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- check_record_class_stats
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_record_class_stats' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `check_record_class_stats` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- check_record_class_stats_new
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_record_class_stats_new' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `check_record_class_stats_new` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- check_record_deductions_new
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_record_deductions_new' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `check_record_deductions_new` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- check_record_items
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_record_items' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `check_record_items` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- check_record_items_old
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_record_items_old' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `check_record_items_old` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- check_record_items_v3_deprecated
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_record_items_v3_deprecated' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `check_record_items_v3_deprecated` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- check_record_rating_results
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_record_rating_results' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `check_record_rating_results` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- check_record_ratings_deprecated
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_record_ratings_deprecated' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `check_record_ratings_deprecated` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- check_record_visibility_deprecated
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_record_visibility_deprecated' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `check_record_visibility_deprecated` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- check_record_weight_configs
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_record_weight_configs' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `check_record_weight_configs` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- check_records
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_records' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `check_records` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- check_records_new
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_records_new' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `check_records_new` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- check_records_old
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_records_old' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `check_records_old` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- check_records_v3_deprecated
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_records_v3_deprecated' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `check_records_v3_deprecated` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- check_task_assignments
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_task_assignments' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `check_task_assignments` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- check_templates
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_templates' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `check_templates` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- class_course_assignments
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'class_course_assignments' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `class_course_assignments` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- class_dormitory_bindings
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'class_dormitory_bindings' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `class_dormitory_bindings` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- class_quantification_summary
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'class_quantification_summary' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `class_quantification_summary` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- class_size_snapshots
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'class_size_snapshots' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `class_size_snapshots` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- class_size_standards
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'class_size_standards' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `class_size_standards` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- class_weight_configs
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'class_weight_configs' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `class_weight_configs` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- classrooms
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'classrooms' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `classrooms` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- courses
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'courses' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `courses` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- curriculum_plan_courses
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'curriculum_plan_courses' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `curriculum_plan_courses` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- curriculum_plans
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'curriculum_plans' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `curriculum_plans` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- daily_check_appeals
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'daily_check_appeals' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `daily_check_appeals` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- daily_check_categories
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'daily_check_categories' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `daily_check_categories` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- daily_check_details
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'daily_check_details' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `daily_check_details` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- daily_check_targets
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'daily_check_targets' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `daily_check_targets` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- daily_check_weight_configs
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'daily_check_weight_configs' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `daily_check_weight_configs` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- daily_checks
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'daily_checks' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `daily_checks` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- daily_checks_deprecated
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'daily_checks_deprecated' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `daily_checks_deprecated` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- daily_class_summary
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'daily_class_summary' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `daily_class_summary` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- data_permissions
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'data_permissions' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `data_permissions` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- data_scope_types
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'data_scope_types' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `data_scope_types` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- deduction_items_v2
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'deduction_items_v2' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `deduction_items_v2` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- departments
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'departments' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `departments` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- domain_events
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'domain_events' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `domain_events` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- dormitories
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dormitories' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `dormitories` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- dormitory_building_managers
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dormitory_building_managers' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `dormitory_building_managers` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- dormitory_buildings
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dormitory_buildings' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `dormitory_buildings` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- entity_event_relations
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'entity_event_relations' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `entity_event_relations` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- eval_conditions
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eval_conditions' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `eval_conditions` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- eval_levels
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eval_levels' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `eval_levels` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- eval_results
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eval_results' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `eval_results` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- evaluation_dimensions
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'evaluation_dimensions' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `evaluation_dimensions` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- evaluation_periods
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'evaluation_periods' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `evaluation_periods` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- event_publications
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'event_publications' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `event_publications` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- exam_arrangements
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam_arrangements' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `exam_arrangements` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- exam_batches
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam_batches' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `exam_batches` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- exam_invigilators
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam_invigilators' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `exam_invigilators` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- exam_rooms
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam_rooms' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `exam_rooms` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- export_tasks
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'export_tasks' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `export_tasks` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- formula_functions
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'formula_functions' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `formula_functions` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- formula_variables
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'formula_variables' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `formula_variables` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- functional_dept_modules
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'functional_dept_modules' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `functional_dept_modules` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- functional_dept_scope
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'functional_dept_scope' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `functional_dept_scope` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- grade_batches
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'grade_batches' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `grade_batches` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- grade_directors
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'grade_directors' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `grade_directors` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- grade_major_directions
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'grade_major_directions' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `grade_major_directions` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- grades
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'grades' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `grades` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- honor_types
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'honor_types' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `honor_types` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- input_types
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'input_types' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `input_types` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- insp_evaluation_levels
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'insp_evaluation_levels' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `insp_evaluation_levels` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- insp_grade_definitions
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'insp_grade_definitions' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `insp_grade_definitions` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- insp_policy_calc_rules
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'insp_policy_calc_rules' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `insp_policy_calc_rules` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- insp_policy_grade_bands
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'insp_policy_grade_bands' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `insp_policy_grade_bands` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- insp_sensor_readings
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'insp_sensor_readings' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `insp_sensor_readings` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- inspection_daily_summaries
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'inspection_daily_summaries' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `inspection_daily_summaries` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- inspection_details
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'inspection_details' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `inspection_details` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- inspection_evidences
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'inspection_evidences' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `inspection_evidences` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- inspection_monthly_summaries
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'inspection_monthly_summaries' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `inspection_monthly_summaries` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- inspection_projects
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'inspection_projects' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `inspection_projects` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- inspection_targets
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'inspection_targets' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `inspection_targets` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- inspection_tasks
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'inspection_tasks' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `inspection_tasks` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- inspection_templates
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'inspection_templates' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `inspection_templates` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- inspection_weekly_summaries
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'inspection_weekly_summaries' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `inspection_weekly_summaries` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- inspector_permissions
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'inspector_permissions' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `inspector_permissions` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- leave_requests
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'leave_requests' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `leave_requests` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- major_categories
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'major_categories' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `major_categories` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- major_directions
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'major_directions' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `major_directions` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- majors
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'majors' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `majors` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- notification_records
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notification_records' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `notification_records` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- operation_logs
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'operation_logs' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `operation_logs` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- org_unit_types
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'org_unit_types' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `org_unit_types` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- permission_audit_log
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'permission_audit_log' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `permission_audit_log` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- place
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'place' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `place` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- place_assignments
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'place_assignments' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `place_assignments` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- place_audit_logs
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'place_audit_logs' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `place_audit_logs` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- place_batch_job_items
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'place_batch_job_items' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `place_batch_job_items` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- place_batch_jobs
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'place_batch_jobs' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `place_batch_jobs` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- place_bookings
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'place_bookings' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `place_bookings` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- place_capacity_stats_mv
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'place_capacity_stats_mv' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `place_capacity_stats_mv` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- place_categories
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'place_categories' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `place_categories` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- place_class_assignment
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'place_class_assignment' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `place_class_assignment` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- place_classroom_ext
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'place_classroom_ext' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `place_classroom_ext` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- place_dormitory_ext
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'place_dormitory_ext' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `place_dormitory_ext` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- place_lab_ext
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'place_lab_ext' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `place_lab_ext` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- place_occupant
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'place_occupant' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `place_occupant` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- place_occupants
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'place_occupants' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `place_occupants` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- place_office_ext
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'place_office_ext' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `place_office_ext` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- place_org_assignment
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'place_org_assignment' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `place_org_assignment` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- place_org_relation_history
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'place_org_relation_history' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `place_org_relation_history` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- place_org_relations
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'place_org_relations' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `place_org_relations` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- place_type_config
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'place_type_config' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `place_type_config` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- place_types
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'place_types' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `place_types` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- places
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'places' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `places` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- project_inspector_configs
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'project_inspector_configs' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `project_inspector_configs` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- quantification_appeals_deprecated
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quantification_appeals_deprecated' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `quantification_appeals_deprecated` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- quantification_details
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quantification_details' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `quantification_details` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- quantification_dict_categories
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quantification_dict_categories' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `quantification_dict_categories` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- quantification_records_deprecated
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quantification_records_deprecated' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `quantification_records_deprecated` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- quantification_types_deprecated
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quantification_types_deprecated' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `quantification_types_deprecated` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- rating_calculation_detail
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rating_calculation_detail' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `rating_calculation_detail` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- rating_change_log
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rating_change_log' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `rating_change_log` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- rating_config
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rating_config' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `rating_config` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- rating_config_version
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rating_config_version' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `rating_config_version` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- rating_configs
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rating_configs' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `rating_configs` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- rating_configs_deprecated
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rating_configs_deprecated' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `rating_configs_deprecated` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- rating_levels
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rating_levels' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `rating_levels` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- rating_levels_deprecated
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rating_levels_deprecated' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `rating_levels_deprecated` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- rating_periods_deprecated
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rating_periods_deprecated' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `rating_periods_deprecated` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- rating_publicity_deprecated
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rating_publicity_deprecated' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `rating_publicity_deprecated` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- rating_ranking_source
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rating_ranking_source' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `rating_ranking_source` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- rating_result
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rating_result' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `rating_result` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- rating_results
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rating_results' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `rating_results` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- rating_results_deprecated
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rating_results_deprecated' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `rating_results_deprecated` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- rating_rules
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rating_rules' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `rating_rules` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- rating_statistics
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rating_statistics' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `rating_statistics` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- rating_templates
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rating_templates' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `rating_templates` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- revision_records
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'revision_records' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `revision_records` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- role_custom_scope
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'role_custom_scope' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `role_custom_scope` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- role_data_permissions
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'role_data_permissions' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `role_data_permissions` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- role_permission_templates
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'role_permission_templates' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `role_permission_templates` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- schedule_adjustments
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'schedule_adjustments' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `schedule_adjustments` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- schedule_conflict_records
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'schedule_conflict_records' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `schedule_conflict_records` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- schedule_entries
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'schedule_entries' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `schedule_entries` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- schedule_executions
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'schedule_executions' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `schedule_executions` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- schedule_policies
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'schedule_policies' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `schedule_policies` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- scheduling_constraints
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scheduling_constraints' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `scheduling_constraints` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- scope_metadata
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scope_metadata' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `scope_metadata` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- scoring_strategies
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scoring_strategies' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `scoring_strategies` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- semester_course_offerings
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'semester_course_offerings' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `semester_course_offerings` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- semesters
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'semesters' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `semesters` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- stat_analysis_configs
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stat_analysis_configs' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `stat_analysis_configs` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- stat_analysis_metrics
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stat_analysis_metrics' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `stat_analysis_metrics` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- stat_analysis_snapshots
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stat_analysis_snapshots' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `stat_analysis_snapshots` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- stat_category_mappings
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stat_category_mappings' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `stat_category_mappings` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- student_behavior_alerts
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'student_behavior_alerts' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `student_behavior_alerts` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- student_dormitory
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'student_dormitory' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `student_dormitory` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- student_evaluation_results
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'student_evaluation_results' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `student_evaluation_results` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- student_grades
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'student_grades' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `student_grades` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- student_honor_application
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'student_honor_application' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `student_honor_application` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- student_scores
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'student_scores' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `student_scores` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- system_configs
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'system_configs' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `system_configs` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- system_messages
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'system_messages' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `system_messages` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- system_modules
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'system_modules' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `system_modules` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- task_approval_configs
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'task_approval_configs' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `task_approval_configs` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- task_approval_records
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'task_approval_records' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `task_approval_records` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- task_assignees
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'task_assignees' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `task_assignees` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- task_category_configs
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'task_category_configs' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `task_category_configs` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- task_inspector_assignments
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'task_inspector_assignments' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `task_inspector_assignments` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- task_logs
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'task_logs' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `task_logs` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- task_submissions
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'task_submissions' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `task_submissions` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- tasks
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tasks' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `tasks` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- teacher_assignments
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'teacher_assignments' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `teacher_assignments` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- teaching_class_members
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'teaching_class_members' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `teaching_class_members` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- teaching_classes
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'teaching_classes' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `teaching_classes` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- teaching_task_teachers
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'teaching_task_teachers' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `teaching_task_teachers` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- teaching_tasks
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'teaching_tasks' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `teaching_tasks` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- template_categories
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'template_categories' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `template_categories` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- template_score_items
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'template_score_items' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `template_score_items` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- user_data_scopes
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_data_scopes' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `user_data_scopes` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- user_departments
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_departments' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `user_departments` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- user_org_relation_history
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_org_relation_history' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `user_org_relation_history` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- user_org_relations
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_org_relations' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `user_org_relations` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- user_place_relation_history
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_place_relation_history' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `user_place_relation_history` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- user_place_relations
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_place_relations' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `user_place_relations` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- user_scope_assignments
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_scope_assignments' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `user_scope_assignments` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- user_types
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_types' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `user_types` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- v6_corrective_actions
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'v6_corrective_actions' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `v6_corrective_actions` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- wechat_push_record
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wechat_push_record' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `wechat_push_record` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- weight_configs
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'weight_configs' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `weight_configs` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- weight_schemes
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'weight_schemes' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `weight_schemes` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- workflow_templates
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'workflow_templates' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `workflow_templates` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- activity_events (was incorrectly excluded by LIKE 'act_%' pattern)
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'activity_events' AND COLUMN_NAME = 'tenant_id') = 0,
    'ALTER TABLE `activity_events` ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- Part 2: Add indexes for tenant_id on all tables
-- ============================================================

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'academic_event' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `academic_event` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'academic_warning_rules' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `academic_warning_rules` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'academic_warnings' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `academic_warnings` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'academic_weeks' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `academic_weeks` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'academic_years' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `academic_years` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'analysis_config_templates' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `analysis_config_templates` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'analysis_configs' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `analysis_configs` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'analysis_metrics' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `analysis_metrics` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'analysis_snapshots' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `analysis_snapshots` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'announcement_reads' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `announcement_reads` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'announcements' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `announcements` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'appeal_approval_configs' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `appeal_approval_configs` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'appeal_approval_records' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `appeal_approval_records` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'appeal_audit_logs' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `appeal_audit_logs` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'appeal_configs' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `appeal_configs` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'appeal_records' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `appeal_records` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'appeals_v2' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `appeals_v2` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'archive_rules' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `archive_rules` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'asset' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `asset` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'asset_alert' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `asset_alert` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'asset_approval' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `asset_approval` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'asset_borrow' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `asset_borrow` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'asset_category' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `asset_category` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'asset_depreciation' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `asset_depreciation` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'asset_history' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `asset_history` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'asset_inventory' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `asset_inventory` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'asset_inventory_detail' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `asset_inventory_detail` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'asset_maintenance' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `asset_maintenance` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'attendance_records' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `attendance_records` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'audit_logs' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `audit_logs` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'behavior_types' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `behavior_types` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'booking_seat_assignments' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `booking_seat_assignments` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'building_department_assignments' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `building_department_assignments` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'building_departments' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `building_departments` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'building_dormitories' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `building_dormitories` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'building_teachings' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `building_teachings` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'buildings' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `buildings` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'calculation_rules' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `calculation_rules` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'category_mappings' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `category_mappings` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'category_weight_rules' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `category_weight_rules` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_appeals_deprecated' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `check_appeals_deprecated` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_audit_logs' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `check_audit_logs` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_categories' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `check_categories` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_export_templates' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `check_export_templates` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_item_appeals' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `check_item_appeals` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_items' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `check_items` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_plan_inspectors' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `check_plan_inspectors` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_plan_period_config' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `check_plan_period_config` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_plan_rating_audit_log' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `check_plan_rating_audit_log` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_plan_rating_frequency' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `check_plan_rating_frequency` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_plan_rating_levels' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `check_plan_rating_levels` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_plan_rating_results' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `check_plan_rating_results` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_plan_rating_rules' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `check_plan_rating_rules` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_plan_rating_schedule' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `check_plan_rating_schedule` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_plan_rating_summary' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `check_plan_rating_summary` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_plans' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `check_plans` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_rating_configs' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `check_rating_configs` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_rating_configs_deprecated' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `check_rating_configs_deprecated` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_rating_levels_deprecated' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `check_rating_levels_deprecated` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_rating_results_deprecated' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `check_rating_results_deprecated` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_record_appeals_new' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `check_record_appeals_new` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_record_category_stats_deprecated' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `check_record_category_stats_deprecated` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_record_category_stats_new' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `check_record_category_stats_new` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_record_class_stats' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `check_record_class_stats` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_record_class_stats_new' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `check_record_class_stats_new` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_record_deductions_new' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `check_record_deductions_new` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_record_items' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `check_record_items` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_record_items_old' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `check_record_items_old` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_record_items_v3_deprecated' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `check_record_items_v3_deprecated` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_record_rating_results' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `check_record_rating_results` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_record_ratings_deprecated' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `check_record_ratings_deprecated` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_record_visibility_deprecated' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `check_record_visibility_deprecated` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_record_weight_configs' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `check_record_weight_configs` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_records' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `check_records` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_records_new' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `check_records_new` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_records_old' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `check_records_old` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_records_v3_deprecated' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `check_records_v3_deprecated` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_task_assignments' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `check_task_assignments` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'check_templates' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `check_templates` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'class_course_assignments' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `class_course_assignments` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'class_dormitory_bindings' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `class_dormitory_bindings` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'class_quantification_summary' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `class_quantification_summary` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'class_size_snapshots' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `class_size_snapshots` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'class_size_standards' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `class_size_standards` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'class_weight_configs' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `class_weight_configs` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'classrooms' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `classrooms` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'courses' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `courses` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'curriculum_plan_courses' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `curriculum_plan_courses` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'curriculum_plans' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `curriculum_plans` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'daily_check_appeals' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `daily_check_appeals` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'daily_check_categories' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `daily_check_categories` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'daily_check_details' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `daily_check_details` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'daily_check_targets' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `daily_check_targets` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'daily_check_weight_configs' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `daily_check_weight_configs` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'daily_checks' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `daily_checks` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'daily_checks_deprecated' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `daily_checks_deprecated` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'daily_class_summary' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `daily_class_summary` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'data_permissions' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `data_permissions` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'data_scope_types' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `data_scope_types` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'deduction_items_v2' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `deduction_items_v2` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'departments' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `departments` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'domain_events' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `domain_events` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dormitories' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `dormitories` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dormitory_building_managers' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `dormitory_building_managers` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dormitory_buildings' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `dormitory_buildings` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'entity_event_relations' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `entity_event_relations` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eval_conditions' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `eval_conditions` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eval_levels' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `eval_levels` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eval_results' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `eval_results` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'evaluation_dimensions' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `evaluation_dimensions` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'evaluation_periods' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `evaluation_periods` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'event_publications' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `event_publications` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam_arrangements' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `exam_arrangements` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam_batches' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `exam_batches` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam_invigilators' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `exam_invigilators` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam_rooms' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `exam_rooms` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'export_tasks' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `export_tasks` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'formula_functions' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `formula_functions` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'formula_variables' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `formula_variables` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'functional_dept_modules' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `functional_dept_modules` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'functional_dept_scope' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `functional_dept_scope` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'grade_batches' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `grade_batches` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'grade_directors' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `grade_directors` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'grade_major_directions' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `grade_major_directions` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'grades' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `grades` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'honor_types' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `honor_types` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'input_types' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `input_types` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'insp_evaluation_levels' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `insp_evaluation_levels` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'insp_grade_definitions' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `insp_grade_definitions` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'insp_policy_calc_rules' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `insp_policy_calc_rules` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'insp_policy_grade_bands' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `insp_policy_grade_bands` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'insp_sensor_readings' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `insp_sensor_readings` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'inspection_daily_summaries' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `inspection_daily_summaries` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'inspection_details' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `inspection_details` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'inspection_evidences' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `inspection_evidences` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'inspection_monthly_summaries' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `inspection_monthly_summaries` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'inspection_projects' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `inspection_projects` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'inspection_targets' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `inspection_targets` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'inspection_tasks' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `inspection_tasks` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'inspection_templates' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `inspection_templates` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'inspection_weekly_summaries' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `inspection_weekly_summaries` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'inspector_permissions' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `inspector_permissions` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'leave_requests' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `leave_requests` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'major_categories' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `major_categories` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'major_directions' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `major_directions` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'majors' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `majors` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notification_records' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `notification_records` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'operation_logs' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `operation_logs` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'org_unit_types' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `org_unit_types` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'permission_audit_log' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `permission_audit_log` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'place' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `place` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'place_assignments' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `place_assignments` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'place_audit_logs' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `place_audit_logs` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'place_batch_job_items' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `place_batch_job_items` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'place_batch_jobs' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `place_batch_jobs` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'place_bookings' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `place_bookings` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'place_capacity_stats_mv' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `place_capacity_stats_mv` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'place_categories' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `place_categories` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'place_class_assignment' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `place_class_assignment` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'place_classroom_ext' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `place_classroom_ext` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'place_dormitory_ext' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `place_dormitory_ext` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'place_lab_ext' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `place_lab_ext` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'place_occupant' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `place_occupant` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'place_occupants' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `place_occupants` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'place_office_ext' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `place_office_ext` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'place_org_assignment' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `place_org_assignment` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'place_org_relation_history' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `place_org_relation_history` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'place_org_relations' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `place_org_relations` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'place_type_config' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `place_type_config` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'place_types' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `place_types` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'places' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `places` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'project_inspector_configs' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `project_inspector_configs` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quantification_appeals_deprecated' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `quantification_appeals_deprecated` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quantification_details' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `quantification_details` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quantification_dict_categories' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `quantification_dict_categories` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quantification_records_deprecated' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `quantification_records_deprecated` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quantification_types_deprecated' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `quantification_types_deprecated` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rating_calculation_detail' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `rating_calculation_detail` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rating_change_log' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `rating_change_log` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rating_config' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `rating_config` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rating_config_version' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `rating_config_version` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rating_configs' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `rating_configs` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rating_configs_deprecated' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `rating_configs_deprecated` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rating_levels' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `rating_levels` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rating_levels_deprecated' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `rating_levels_deprecated` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rating_periods_deprecated' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `rating_periods_deprecated` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rating_publicity_deprecated' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `rating_publicity_deprecated` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rating_ranking_source' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `rating_ranking_source` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rating_result' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `rating_result` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rating_results' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `rating_results` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rating_results_deprecated' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `rating_results_deprecated` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rating_rules' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `rating_rules` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rating_statistics' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `rating_statistics` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rating_templates' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `rating_templates` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'revision_records' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `revision_records` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'role_custom_scope' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `role_custom_scope` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'role_data_permissions' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `role_data_permissions` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'role_permission_templates' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `role_permission_templates` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'schedule_adjustments' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `schedule_adjustments` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'schedule_conflict_records' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `schedule_conflict_records` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'schedule_entries' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `schedule_entries` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'schedule_executions' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `schedule_executions` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'schedule_policies' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `schedule_policies` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scheduling_constraints' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `scheduling_constraints` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scope_metadata' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `scope_metadata` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scoring_strategies' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `scoring_strategies` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'semester_course_offerings' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `semester_course_offerings` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'semesters' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `semesters` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stat_analysis_configs' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `stat_analysis_configs` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stat_analysis_metrics' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `stat_analysis_metrics` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stat_analysis_snapshots' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `stat_analysis_snapshots` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stat_category_mappings' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `stat_category_mappings` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'student_behavior_alerts' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `student_behavior_alerts` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'student_dormitory' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `student_dormitory` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'student_evaluation_results' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `student_evaluation_results` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'student_grades' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `student_grades` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'student_honor_application' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `student_honor_application` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'student_scores' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `student_scores` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'system_configs' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `system_configs` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'system_messages' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `system_messages` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'system_modules' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `system_modules` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'task_approval_configs' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `task_approval_configs` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'task_approval_records' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `task_approval_records` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'task_assignees' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `task_assignees` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'task_category_configs' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `task_category_configs` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'task_inspector_assignments' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `task_inspector_assignments` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'task_logs' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `task_logs` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'task_submissions' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `task_submissions` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tasks' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `tasks` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'teacher_assignments' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `teacher_assignments` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'teaching_class_members' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `teaching_class_members` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'teaching_classes' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `teaching_classes` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'teaching_task_teachers' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `teaching_task_teachers` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'teaching_tasks' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `teaching_tasks` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'template_categories' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `template_categories` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'template_score_items' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `template_score_items` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_data_scopes' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `user_data_scopes` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_departments' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `user_departments` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_org_relation_history' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `user_org_relation_history` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_org_relations' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `user_org_relations` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_place_relation_history' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `user_place_relation_history` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_place_relations' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `user_place_relations` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_scope_assignments' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `user_scope_assignments` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_types' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `user_types` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'v6_corrective_actions' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `v6_corrective_actions` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wechat_push_record' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `wechat_push_record` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'weight_configs' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `weight_configs` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'weight_schemes' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `weight_schemes` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'workflow_templates' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `workflow_templates` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'activity_events' AND INDEX_NAME = 'idx_tenant') = 0,
    'ALTER TABLE `activity_events` ADD INDEX idx_tenant (tenant_id)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
