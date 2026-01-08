-- ============================================================================
-- Student Management System V2 - Index Definitions
-- ============================================================================
-- Version: 2.0
-- Description: Consolidated index definitions for performance optimization
-- Created: 2026-01-06
-- ============================================================================
--
-- NOTE: Most indexes are defined within table DDL in the main schema files.
-- This file contains additional composite indexes and performance-critical
-- indexes that may need to be added after data migration.
-- ============================================================================

SET NAMES utf8mb4;

-- ============================================================================
-- PART 1: CORE TABLES - Additional Performance Indexes
-- ============================================================================

-- ---------------------------------------------------------------------------
-- Users & Authentication
-- ---------------------------------------------------------------------------
-- Covering index for login authentication
CREATE INDEX IF NOT EXISTS idx_users_login_cover
    ON users (username, password, status, deleted);

-- User search optimization
CREATE INDEX IF NOT EXISTS idx_users_search
    ON users (real_name, phone, status);

-- ---------------------------------------------------------------------------
-- Roles & Permissions
-- ---------------------------------------------------------------------------
-- Role hierarchy lookup
CREATE INDEX IF NOT EXISTS idx_roles_parent_status
    ON roles (parent_id, status, deleted);

-- Permission tree traversal
CREATE INDEX IF NOT EXISTS idx_permissions_tree
    ON permissions (parent_id, permission_type, sort_order);

-- ---------------------------------------------------------------------------
-- Organization Units
-- ---------------------------------------------------------------------------
-- Org unit hierarchy (for tree queries)
CREATE INDEX IF NOT EXISTS idx_org_units_hierarchy
    ON org_units (parent_id, unit_level, status, deleted);

-- Org unit by type and code
CREATE INDEX IF NOT EXISTS idx_org_units_type_code
    ON org_units (unit_type, unit_code, deleted);

-- ---------------------------------------------------------------------------
-- Classes
-- ---------------------------------------------------------------------------
-- Class by org unit and status (common query pattern)
CREATE INDEX IF NOT EXISTS idx_classes_org_status
    ON classes (org_unit_id, status, deleted);

-- Class by grade and enrollment year
CREATE INDEX IF NOT EXISTS idx_classes_grade_year
    ON classes (grade_id, enrollment_year, deleted);

-- Full-text search on class name (if MySQL 5.7+)
-- CREATE FULLTEXT INDEX IF NOT EXISTS ft_classes_name ON classes (class_name);

-- ---------------------------------------------------------------------------
-- Students
-- ---------------------------------------------------------------------------
-- Student by class (most common lookup)
CREATE INDEX IF NOT EXISTS idx_students_class
    ON students (class_id, status, deleted);

-- Student search optimization
CREATE INDEX IF NOT EXISTS idx_students_search
    ON students (name, student_no, id_card, phone);

-- Student by dormitory
CREATE INDEX IF NOT EXISTS idx_students_dormitory
    ON students (dormitory_id, bed_no, deleted);

-- ---------------------------------------------------------------------------
-- Dormitories
-- ---------------------------------------------------------------------------
-- Dormitory by building and floor
CREATE INDEX IF NOT EXISTS idx_dormitories_location
    ON dormitories (building_id, floor, status);

-- Dormitory capacity management
CREATE INDEX IF NOT EXISTS idx_dormitories_capacity
    ON dormitories (capacity, current_count, building_id);

-- ============================================================================
-- PART 2: INSPECTION TABLES - Performance Indexes
-- ============================================================================

-- ---------------------------------------------------------------------------
-- Check Templates
-- ---------------------------------------------------------------------------
-- Template by category and status
CREATE INDEX IF NOT EXISTS idx_templates_category_status
    ON check_templates (category_id, status, deleted);

-- Active templates for dropdown
CREATE INDEX IF NOT EXISTS idx_templates_active
    ON check_templates (status, deleted, sort_order);

-- ---------------------------------------------------------------------------
-- Check Plans
-- ---------------------------------------------------------------------------
-- Plans by date range (most common query)
CREATE INDEX IF NOT EXISTS idx_plans_date_range
    ON check_plans (start_date, end_date, status, deleted);

-- Plans by semester
CREATE INDEX IF NOT EXISTS idx_plans_semester
    ON check_plans (semester_id, status, deleted);

-- ---------------------------------------------------------------------------
-- Check Records
-- ---------------------------------------------------------------------------
-- Records by plan and status (dashboard queries)
CREATE INDEX IF NOT EXISTS idx_records_plan_status
    ON check_records (plan_id, status, deleted);

-- Records by check date (date-based queries)
CREATE INDEX IF NOT EXISTS idx_records_check_date
    ON check_records (check_date, status, deleted);

-- Records by inspector (my tasks)
CREATE INDEX IF NOT EXISTS idx_records_inspector
    ON check_records (inspector_id, status, deleted);

-- Records by template and date
CREATE INDEX IF NOT EXISTS idx_records_template_date
    ON check_records (template_id, check_date, deleted);

-- ---------------------------------------------------------------------------
-- Class Scores
-- ---------------------------------------------------------------------------
-- Scores by record (record detail page)
CREATE INDEX IF NOT EXISTS idx_scores_record
    ON class_scores (record_id, class_id);

-- Scores by class and date range (class statistics)
CREATE INDEX IF NOT EXISTS idx_scores_class_date
    ON class_scores (class_id, check_date, deleted);

-- Ranking query optimization
CREATE INDEX IF NOT EXISTS idx_scores_ranking
    ON class_scores (record_id, final_score DESC, deleted);

-- Rating distribution
CREATE INDEX IF NOT EXISTS idx_scores_rating
    ON class_scores (record_id, rating, deleted);

-- ---------------------------------------------------------------------------
-- Deduction Details
-- ---------------------------------------------------------------------------
-- Deductions by score record
CREATE INDEX IF NOT EXISTS idx_deductions_score
    ON deduction_details (score_id, deleted);

-- Deductions by item (item statistics)
CREATE INDEX IF NOT EXISTS idx_deductions_item
    ON deduction_details (item_id, record_id, deleted);

-- Deductions by class (class history)
CREATE INDEX IF NOT EXISTS idx_deductions_class
    ON deduction_details (class_id, check_date, deleted);

-- ---------------------------------------------------------------------------
-- Appeals
-- ---------------------------------------------------------------------------
-- Appeals by status (pending review list)
CREATE INDEX IF NOT EXISTS idx_appeals_status
    ON check_item_appeals (status, deleted, created_at DESC);

-- Appeals by class (class appeal history)
CREATE INDEX IF NOT EXISTS idx_appeals_class
    ON check_item_appeals (class_id, status, deleted);

-- Appeals by reviewer (my reviews)
CREATE INDEX IF NOT EXISTS idx_appeals_reviewer
    ON check_item_appeals (reviewer_id, status, deleted);

-- ============================================================================
-- PART 3: RATING TABLES - Performance Indexes
-- ============================================================================

-- ---------------------------------------------------------------------------
-- Rating Configs
-- ---------------------------------------------------------------------------
-- Active config lookup
CREATE INDEX IF NOT EXISTS idx_rating_configs_active
    ON rating_configs (status, is_default, deleted);

-- ---------------------------------------------------------------------------
-- Check Plan Ratings
-- ---------------------------------------------------------------------------
-- Ratings by plan and class (result lookup)
CREATE INDEX IF NOT EXISTS idx_plan_ratings_lookup
    ON check_plan_ratings (plan_id, class_id);

-- Ratings by class (class history)
CREATE INDEX IF NOT EXISTS idx_plan_ratings_class
    ON check_plan_ratings (class_id, plan_id);

-- Rating distribution by plan
CREATE INDEX IF NOT EXISTS idx_plan_ratings_distribution
    ON check_plan_ratings (plan_id, final_rating);

-- ============================================================================
-- PART 4: TASK TABLES - Performance Indexes
-- ============================================================================

-- ---------------------------------------------------------------------------
-- Tasks
-- ---------------------------------------------------------------------------
-- Tasks by status (task list queries)
CREATE INDEX IF NOT EXISTS idx_tasks_status
    ON tasks (status, deleted, deadline DESC);

-- Tasks by creator
CREATE INDEX IF NOT EXISTS idx_tasks_creator
    ON tasks (created_by, status, deleted);

-- Tasks by department
CREATE INDEX IF NOT EXISTS idx_tasks_department
    ON tasks (department_id, status, deleted);

-- Tasks by deadline (overdue alerts)
CREATE INDEX IF NOT EXISTS idx_tasks_deadline
    ON tasks (deadline, status, deleted);

-- ---------------------------------------------------------------------------
-- Task Assignees
-- ---------------------------------------------------------------------------
-- My tasks (most common query)
CREATE INDEX IF NOT EXISTS idx_assignees_user
    ON task_assignees (user_id, status, task_id);

-- Task members
CREATE INDEX IF NOT EXISTS idx_assignees_task
    ON task_assignees (task_id, assignee_type, status);

-- ---------------------------------------------------------------------------
-- Task Submissions
-- ---------------------------------------------------------------------------
-- Submissions by task and user
CREATE INDEX IF NOT EXISTS idx_submissions_task_user
    ON task_submissions (task_id, submitted_by, deleted);

-- Submissions by status
CREATE INDEX IF NOT EXISTS idx_submissions_status
    ON task_submissions (task_id, status, deleted);

-- ============================================================================
-- PART 5: AUDIT & LOG TABLES - Performance Indexes
-- ============================================================================

-- ---------------------------------------------------------------------------
-- Operation Logs
-- ---------------------------------------------------------------------------
-- Logs by user and time (user activity)
CREATE INDEX IF NOT EXISTS idx_oplogs_user_time
    ON operation_logs (user_id, created_at DESC, deleted);

-- Logs by module (module audit)
CREATE INDEX IF NOT EXISTS idx_oplogs_module_time
    ON operation_logs (operation_module, created_at DESC, deleted);

-- ---------------------------------------------------------------------------
-- Audit Logs
-- ---------------------------------------------------------------------------
-- Audit by target (entity audit trail)
CREATE INDEX IF NOT EXISTS idx_auditlogs_target_time
    ON audit_logs (target_type, target_id, created_at DESC);

-- ---------------------------------------------------------------------------
-- Domain Events
-- ---------------------------------------------------------------------------
-- Events by aggregate (aggregate history)
CREATE INDEX IF NOT EXISTS idx_events_aggregate_version
    ON domain_events (aggregate_type, aggregate_id, aggregate_version);

-- Events by time range (event replay)
CREATE INDEX IF NOT EXISTS idx_events_time_range
    ON domain_events (occurred_at, aggregate_type);

-- ---------------------------------------------------------------------------
-- Snapshots
-- ---------------------------------------------------------------------------
-- Class size by date range
CREATE INDEX IF NOT EXISTS idx_class_size_date_range
    ON class_size_snapshots (snapshot_date, class_id);

-- Student relationship by date
CREATE INDEX IF NOT EXISTS idx_student_rel_date
    ON student_relationship_snapshots (snapshot_date, student_id);

-- ============================================================================
-- PART 6: STATISTICS & REPORTING - Materialized View-like Indexes
-- ============================================================================

-- For daily statistics aggregation
CREATE INDEX IF NOT EXISTS idx_records_daily_stats
    ON check_records (check_date, template_id, status)
    WHERE deleted = 0;

-- For class ranking across multiple records
CREATE INDEX IF NOT EXISTS idx_scores_class_ranking
    ON class_scores (class_id, final_score, check_date);

-- For deduction item frequency analysis
CREATE INDEX IF NOT EXISTS idx_deductions_item_freq
    ON deduction_details (item_id, check_date, class_id);

-- ============================================================================
-- INDEX MAINTENANCE NOTES
-- ============================================================================
--
-- 1. Monitor index usage with:
--    SELECT * FROM sys.schema_index_statistics WHERE table_schema = 'student_management';
--
-- 2. Check for unused indexes:
--    SELECT * FROM sys.schema_unused_indexes WHERE object_schema = 'student_management';
--
-- 3. Analyze table statistics periodically:
--    ANALYZE TABLE table_name;
--
-- 4. Consider partitioning for large tables:
--    - operation_logs: RANGE partition by created_at (monthly)
--    - domain_events: RANGE partition by occurred_at (monthly)
--    - check_records: RANGE partition by check_date (yearly)
--
-- 5. For very large deployments, consider:
--    - Read replicas for reporting queries
--    - Elasticsearch for full-text search
--    - Redis caching for hot data
--
-- ============================================================================
