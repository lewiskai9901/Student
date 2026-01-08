-- ============================================================================
-- 学生管理系统 - 架构重构迁移脚本 V5.0.1
-- 数据迁移: departments -> org_units
-- 日期: 2026-01-02
-- 说明: 将现有部门数据迁移到新的组织单元表
-- ============================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- 1. 迁移 departments 数据到 org_units
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
    COALESCE(d.`department_code`, CONCAT('DEPT_', d.`id`)) AS `unit_code`,
    d.`name` AS `unit_name`,
    'DEPARTMENT' AS `unit_type`,
    d.`parent_id`,
    d.`tree_path`,
    COALESCE(d.`level`, 1) AS `tree_level`,
    d.`leader_id`,
    COALESCE(d.`sort`, 0) AS `sort_order`,
    COALESCE(d.`status`, 1) AS `status`,
    COALESCE(d.`created_time`, NOW()) AS `created_at`,
    COALESCE(d.`update_time`, NOW()) AS `updated_at`,
    d.`create_by` AS `created_by`,
    COALESCE(d.`deleted`, 0) AS `deleted`
FROM `departments` d
WHERE NOT EXISTS (
    SELECT 1 FROM `org_units` o WHERE o.`id` = d.`id`
);

-- ---------------------------------------------------------------------------
-- 2. 更新 tree_path 格式 (如果需要)
-- ---------------------------------------------------------------------------
UPDATE `org_units`
SET `tree_path` = CONCAT('/', `id`, '/')
WHERE `parent_id` IS NULL AND (`tree_path` IS NULL OR `tree_path` = '');

-- 递归更新子节点的 tree_path (需要多次执行直到没有更新)
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

-- Level 4
UPDATE `org_units` child
JOIN `org_units` parent ON child.`parent_id` = parent.`id`
SET child.`tree_path` = CONCAT(parent.`tree_path`, child.`id`, '/'),
    child.`tree_level` = parent.`tree_level` + 1
WHERE child.`parent_id` IS NOT NULL;

-- ---------------------------------------------------------------------------
-- 3. 迁移现有班主任分配到 teacher_assignments
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
    c.`id` * 10 + 1 AS `id`,  -- 生成唯一ID
    c.`id` AS `class_id`,
    c.`teacher_id` AS `teacher_id`,
    'HEAD_TEACHER' AS `role_type`,
    1 AS `is_primary`,
    COALESCE(DATE(c.`created_time`), CURDATE()) AS `start_date`,
    'ACTIVE' AS `status`,
    COALESCE(c.`created_time`, NOW()) AS `created_at`
FROM `classes` c
WHERE c.`teacher_id` IS NOT NULL
  AND c.`deleted` = 0
  AND NOT EXISTS (
      SELECT 1 FROM `teacher_assignments` ta
      WHERE ta.`class_id` = c.`id`
        AND ta.`teacher_id` = c.`teacher_id`
        AND ta.`role_type` = 'HEAD_TEACHER'
  );

-- 迁移副班主任
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
    COALESCE(DATE(c.`created_time`), CURDATE()) AS `start_date`,
    'ACTIVE' AS `status`,
    COALESCE(c.`created_time`, NOW()) AS `created_at`
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
-- 4. 创建学年数据 (基于现有班级的入学年份)
-- ---------------------------------------------------------------------------
INSERT INTO `academic_years` (`id`, `year_code`, `year_name`, `start_date`, `end_date`, `is_current`)
SELECT
    enrollment_year AS `id`,
    CONCAT(enrollment_year, '-', enrollment_year + 1) AS `year_code`,
    CONCAT(enrollment_year, '-', enrollment_year + 1, '学年') AS `year_name`,
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

-- ---------------------------------------------------------------------------
-- 5. 验证迁移结果
-- ---------------------------------------------------------------------------
-- 可以通过以下查询验证迁移是否成功:
-- SELECT COUNT(*) AS org_units_count FROM org_units;
-- SELECT COUNT(*) AS departments_count FROM departments WHERE deleted = 0;
-- SELECT COUNT(*) AS teacher_assignments_count FROM teacher_assignments;
-- SELECT COUNT(*) AS academic_years_count FROM academic_years;

-- ============================================================================
-- 迁移完成标记
-- ============================================================================
-- 数据迁移完成，旧表暂时保留以便回滚
-- 后续将在确认迁移成功后执行 V5.0.9__cleanup_legacy_tables.sql 删除旧表
