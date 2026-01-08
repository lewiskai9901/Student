-- ============================================================================
-- Student Management System - Data Migration Script V5.0.1
-- Migrate departments to org_units
-- Date: 2026-01-02
-- ============================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- 1. Migrate departments data to org_units
-- ---------------------------------------------------------------------------
INSERT INTO `org_units` (
    `id`,
    `unit_code`,
    `unit_name`,
    `unit_type`,
    `parent_id`,
    `tree_path`,
    `tree_level`,
    `leader_id`,
    `sort_order`,
    `status`,
    `created_at`,
    `updated_at`,
    `created_by`,
    `deleted`
)
SELECT
    d.`id`,
    COALESCE(d.`dept_code`, CONCAT('DEPT_', d.`id`)) AS `unit_code`,
    d.`dept_name` AS `unit_name`,
    'DEPARTMENT' AS `unit_type`,
    d.`parent_id`,
    d.`dept_path`,
    COALESCE(d.`dept_level`, 1) AS `tree_level`,
    d.`leader_id`,
    COALESCE(d.`sort_order`, 0) AS `sort_order`,
    COALESCE(d.`status`, 1) AS `status`,
    COALESCE(d.`created_at`, NOW()) AS `created_at`,
    COALESCE(d.`updated_at`, NOW()) AS `updated_at`,
    d.`created_by` AS `created_by`,
    COALESCE(d.`deleted`, 0) AS `deleted`
FROM `departments` d
WHERE NOT EXISTS (
    SELECT 1 FROM `org_units` o WHERE o.`id` = d.`id`
);

-- ---------------------------------------------------------------------------
-- 2. Update tree_path if needed
-- ---------------------------------------------------------------------------
UPDATE `org_units`
SET `tree_path` = CONCAT('/', `id`, '/')
WHERE `parent_id` IS NULL AND (`tree_path` IS NULL OR `tree_path` = '');

-- Level 2
UPDATE `org_units` child
JOIN `org_units` parent ON child.`parent_id` = parent.`id`
SET child.`tree_path` = CONCAT(parent.`tree_path`, child.`id`, '/'),
    child.`tree_level` = parent.`tree_level` + 1
WHERE child.`parent_id` IS NOT NULL;

-- Level 3
UPDATE `org_units` child
JOIN `org_units` parent ON child.`parent_id` = parent.`id`
SET child.`tree_path` = CONCAT(parent.`tree_path`, child.`id`, '/'),
    child.`tree_level` = parent.`tree_level` + 1
WHERE child.`parent_id` IS NOT NULL;

-- ---------------------------------------------------------------------------
-- 3. Migrate class teachers to teacher_assignments
-- ---------------------------------------------------------------------------
INSERT INTO `teacher_assignments` (
    `id`,
    `class_id`,
    `teacher_id`,
    `role_type`,
    `is_primary`,
    `start_date`,
    `status`,
    `created_at`
)
SELECT
    c.`id` * 10 + 1 AS `id`,
    c.`id` AS `class_id`,
    c.`teacher_id` AS `teacher_id`,
    'HEAD_TEACHER' AS `role_type`,
    1 AS `is_primary`,
    COALESCE(DATE(c.`created_at`), CURDATE()) AS `start_date`,
    'ACTIVE' AS `status`,
    COALESCE(c.`created_at`, NOW()) AS `created_at`
FROM `classes` c
WHERE c.`teacher_id` IS NOT NULL
  AND c.`deleted` = 0
  AND NOT EXISTS (
      SELECT 1 FROM `teacher_assignments` ta
      WHERE ta.`class_id` = c.`id`
        AND ta.`teacher_id` = c.`teacher_id`
        AND ta.`role_type` = 'HEAD_TEACHER'
  );

-- Migrate deputy head teachers
INSERT INTO `teacher_assignments` (
    `id`,
    `class_id`,
    `teacher_id`,
    `role_type`,
    `is_primary`,
    `start_date`,
    `status`,
    `created_at`
)
SELECT
    c.`id` * 10 + 2 AS `id`,
    c.`id` AS `class_id`,
    c.`assistant_teacher_id` AS `teacher_id`,
    'DEPUTY_HEAD' AS `role_type`,
    0 AS `is_primary`,
    COALESCE(DATE(c.`created_at`), CURDATE()) AS `start_date`,
    'ACTIVE' AS `status`,
    COALESCE(c.`created_at`, NOW()) AS `created_at`
FROM `classes` c
WHERE c.`assistant_teacher_id` IS NOT NULL
  AND c.`deleted` = 0
  AND NOT EXISTS (
      SELECT 1 FROM `teacher_assignments` ta
      WHERE ta.`class_id` = c.`id`
        AND ta.`teacher_id` = c.`assistant_teacher_id`
        AND ta.`role_type` = 'DEPUTY_HEAD'
  );

-- ---------------------------------------------------------------------------
-- 4. Create academic years based on existing enrollment years
-- ---------------------------------------------------------------------------
INSERT INTO `academic_years` (`id`, `year_code`, `year_name`, `start_date`, `end_date`, `is_current`)
SELECT
    enrollment_year AS `id`,
    CONCAT(enrollment_year, '-', enrollment_year + 1) AS `year_code`,
    CONCAT(enrollment_year, '-', enrollment_year + 1, ' Academic Year') AS `year_name`,
    CONCAT(enrollment_year, '-09-01') AS `start_date`,
    CONCAT(enrollment_year + 1, '-08-31') AS `end_date`,
    CASE WHEN enrollment_year = YEAR(CURDATE()) THEN 1 ELSE 0 END AS `is_current`
FROM (
    SELECT DISTINCT enrollment_year
    FROM `classes`
    WHERE enrollment_year IS NOT NULL AND deleted = 0
) years
WHERE NOT EXISTS (
    SELECT 1 FROM `academic_years` ay WHERE ay.`id` = years.enrollment_year
);
