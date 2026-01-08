-- ============================================================================
-- 学生管理系统 - 架构重构迁移脚本 V5.0.0
-- 创建统一组织单元表 (org_units)
-- 日期: 2026-01-02
-- 说明: 将 departments + grades 统一为 org_units 树状结构
-- ============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------------
-- 1. 创建组织单元表 (不删除旧表，保持兼容)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `org_units` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `unit_code` VARCHAR(50) NOT NULL COMMENT '组织编码',
    `unit_name` VARCHAR(100) NOT NULL COMMENT '组织名称',
    `unit_type` ENUM('SCHOOL', 'COLLEGE', 'DEPARTMENT', 'TEACHING_GROUP') NOT NULL COMMENT '组织类型',
    `parent_id` BIGINT DEFAULT NULL COMMENT '父级ID',
    `tree_path` VARCHAR(500) DEFAULT NULL COMMENT '路径(如: /1/5/12/)',
    `tree_level` INT DEFAULT 1 COMMENT '层级深度',
    `leader_id` BIGINT DEFAULT NULL COMMENT '负责人ID',
    `deputy_leader_ids` JSON DEFAULT NULL COMMENT '副职ID列表',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0禁用 1启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人',
    `updated_by` BIGINT DEFAULT NULL COMMENT '更新人',
    `deleted` TINYINT DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_unit_code` (`unit_code`),
    KEY `idx_parent` (`parent_id`),
    KEY `idx_tree_path` (`tree_path`(255)),
    KEY `idx_type` (`unit_type`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组织单元表';

-- ---------------------------------------------------------------------------
-- 2. 创建学年表
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `academic_years` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `year_code` VARCHAR(20) NOT NULL COMMENT '学年编码(如: 2024-2025)',
    `year_name` VARCHAR(50) NOT NULL COMMENT '学年名称',
    `start_date` DATE NOT NULL COMMENT '开始日期',
    `end_date` DATE NOT NULL COMMENT '结束日期',
    `semesters` JSON DEFAULT NULL COMMENT '学期配置',
    `is_current` TINYINT DEFAULT 0 COMMENT '是否当前学年',
    `status` TINYINT DEFAULT 1 COMMENT '状态',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_year_code` (`year_code`),
    KEY `idx_current` (`is_current`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学年表';

-- ---------------------------------------------------------------------------
-- 3. 创建教师任职记录表
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `teacher_assignments` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `class_id` BIGINT NOT NULL COMMENT '班级ID',
    `teacher_id` BIGINT NOT NULL COMMENT '教师ID',
    `role_type` ENUM('HEAD_TEACHER', 'DEPUTY_HEAD', 'SUBJECT_TEACHER', 'COUNSELOR') NOT NULL COMMENT '角色类型',
    `subject_id` BIGINT DEFAULT NULL COMMENT '任教科目',
    `is_primary` TINYINT DEFAULT 0 COMMENT '是否主要负责人',
    `start_date` DATE NOT NULL COMMENT '任职开始日期',
    `end_date` DATE DEFAULT NULL COMMENT '任职结束日期',
    `status` ENUM('ACTIVE', 'TRANSFERRED', 'RESIGNED', 'EXPIRED') DEFAULT 'ACTIVE' COMMENT '状态',
    `transfer_reason` VARCHAR(500) DEFAULT NULL COMMENT '变动原因',
    `handover_teacher_id` BIGINT DEFAULT NULL COMMENT '交接教师ID',
    `workload_hours` DECIMAL(5,2) DEFAULT NULL COMMENT '周课时量',
    `remark` TEXT DEFAULT NULL COMMENT '备注',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by` BIGINT DEFAULT NULL,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_class` (`class_id`),
    KEY `idx_teacher` (`teacher_id`),
    KEY `idx_status` (`status`),
    KEY `idx_role_type` (`role_type`),
    KEY `idx_active_assignment` (`class_id`, `role_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教师任职记录表';

-- ---------------------------------------------------------------------------
-- 4. 创建年级主任配置表
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `grade_directors` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `org_unit_id` BIGINT NOT NULL COMMENT '组织单元(系/院)',
    `enrollment_year` INT NOT NULL COMMENT '入学年份',
    `director_id` BIGINT NOT NULL COMMENT '年级主任ID',
    `deputy_director_ids` JSON DEFAULT NULL COMMENT '副主任ID列表',
    `counselor_ids` JSON DEFAULT NULL COMMENT '辅导员ID列表',
    `max_class_count` INT DEFAULT NULL COMMENT '最大班级数',
    `enrollment_quota` INT DEFAULT NULL COMMENT '招生计划数',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_org_year` (`org_unit_id`, `enrollment_year`),
    KEY `idx_director` (`director_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='年级主任配置表';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- 迁移完成标记
-- ============================================================================
-- 执行完成后，请继续执行 V5.0.1__migrate_departments_to_org_units.sql
