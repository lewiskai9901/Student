-- ============================================================================
-- 学生管理系统 - 终极架构数据库设计
-- 版本: v1.0
-- 日期: 2026-01-02
-- 设计原则: 业界最佳实践，DDD领域模型，规范化设计
-- ============================================================================

-- 设置字符集
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================================
-- 第一部分: 组织架构领域
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1.1 组织单元表 (树状结构)
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `org_units`;
CREATE TABLE `org_units` (
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
    KEY `idx_tree_path` (`tree_path`),
    KEY `idx_type` (`unit_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组织单元表';

-- ---------------------------------------------------------------------------
-- 1.2 学年表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `academic_years`;
CREATE TABLE `academic_years` (
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
    UNIQUE KEY `uk_year_code` (`year_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学年表';

-- ---------------------------------------------------------------------------
-- 1.3 专业表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `majors`;
CREATE TABLE `majors` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `major_code` VARCHAR(50) NOT NULL COMMENT '专业代码',
    `major_name` VARCHAR(100) NOT NULL COMMENT '专业名称',
    `org_unit_id` BIGINT NOT NULL COMMENT '所属组织单元(系/院)',
    `education_level` ENUM('SECONDARY', 'JUNIOR_COLLEGE', 'UNDERGRADUATE', 'POSTGRADUATE') NOT NULL COMMENT '学历层次',
    `duration_years` DECIMAL(3,1) NOT NULL COMMENT '学制年限',
    `description` TEXT DEFAULT NULL COMMENT '专业介绍',
    `status` TINYINT DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_major_code` (`major_code`),
    KEY `idx_org_unit` (`org_unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专业表';

-- ---------------------------------------------------------------------------
-- 1.4 专业方向表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `major_directions`;
CREATE TABLE `major_directions` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `major_id` BIGINT NOT NULL COMMENT '所属专业',
    `direction_code` VARCHAR(50) NOT NULL COMMENT '方向代码',
    `direction_name` VARCHAR(100) NOT NULL COMMENT '方向名称',
    `is_segmented` TINYINT DEFAULT 0 COMMENT '是否分段培养',
    `segment_config` JSON DEFAULT NULL COMMENT '分段配置',
    `status` TINYINT DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_direction_code` (`direction_code`),
    KEY `idx_major` (`major_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专业方向表';

-- ---------------------------------------------------------------------------
-- 1.5 班级表 (聚合根)
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `classes`;
CREATE TABLE `classes` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `class_code` VARCHAR(50) NOT NULL COMMENT '班级编码',
    `class_name` VARCHAR(100) NOT NULL COMMENT '班级全称',
    `short_name` VARCHAR(50) DEFAULT NULL COMMENT '班级简称',
    `org_unit_id` BIGINT NOT NULL COMMENT '所属组织单元(系/院)',
    `enrollment_year` INT NOT NULL COMMENT '入学年份',
    `grade_level` INT NOT NULL COMMENT '当前年级',
    `major_direction_id` BIGINT DEFAULT NULL COMMENT '专业方向',
    `class_type` ENUM('NORMAL', 'KEY', 'EXPERIMENTAL', 'INTERNATIONAL') DEFAULT 'NORMAL' COMMENT '班级类型',
    `max_capacity` INT DEFAULT 50 COMMENT '最大容量',
    `current_count` INT DEFAULT 0 COMMENT '当前人数',
    `expected_graduation_year` INT DEFAULT NULL COMMENT '预计毕业年份',
    `actual_graduation_date` DATE DEFAULT NULL COMMENT '实际毕业日期',
    `graduation_status` ENUM('STUDYING', 'GRADUATED', 'DISSOLVED') DEFAULT 'STUDYING' COMMENT '毕业状态',
    `classroom_location` VARCHAR(100) DEFAULT NULL COMMENT '固定教室',
    `status` TINYINT DEFAULT 1,
    `version` INT DEFAULT 0 COMMENT '乐观锁版本',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by` BIGINT DEFAULT NULL,
    `updated_by` BIGINT DEFAULT NULL,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_class_code` (`class_code`),
    KEY `idx_org_unit` (`org_unit_id`),
    KEY `idx_enrollment` (`enrollment_year`),
    KEY `idx_grade` (`grade_level`),
    KEY `idx_major` (`major_direction_id`),
    KEY `idx_composite` (`org_unit_id`, `enrollment_year`, `grade_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级表';

-- ---------------------------------------------------------------------------
-- 1.6 教师任职记录表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `teacher_assignments`;
CREATE TABLE `teacher_assignments` (
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
    KEY `idx_role_type` (`role_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教师任职记录表';

-- ---------------------------------------------------------------------------
-- 1.7 年级主任配置表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `grade_directors`;
CREATE TABLE `grade_directors` (
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
    UNIQUE KEY `uk_org_year` (`org_unit_id`, `enrollment_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='年级主任配置表';

-- ============================================================================
-- 第二部分: 量化检查领域
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 2.1 检查模板表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `inspection_templates`;
CREATE TABLE `inspection_templates` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `template_code` VARCHAR(50) NOT NULL COMMENT '模板编码',
    `template_name` VARCHAR(100) NOT NULL COMMENT '模板名称',
    `description` TEXT DEFAULT NULL COMMENT '模板描述',
    `current_version` INT DEFAULT 1 COMMENT '当前版本号',
    `applicable_scope` ENUM('ALL', 'DEPARTMENT', 'GRADE', 'CUSTOM') DEFAULT 'ALL' COMMENT '适用范围',
    `applicable_config` JSON DEFAULT NULL COMMENT '范围配置',
    `default_weight_scheme_id` BIGINT DEFAULT NULL COMMENT '默认加权方案',
    `default_appeal_config_id` BIGINT DEFAULT NULL COMMENT '默认申诉配置',
    `use_count` INT DEFAULT 0 COMMENT '使用次数',
    `last_used_at` DATETIME DEFAULT NULL COMMENT '最后使用时间',
    `status` ENUM('DRAFT', 'PUBLISHED', 'DEPRECATED') DEFAULT 'DRAFT' COMMENT '状态',
    `is_default` TINYINT DEFAULT 0 COMMENT '是否默认模板',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by` BIGINT DEFAULT NULL,
    `updated_by` BIGINT DEFAULT NULL,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_code` (`template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查模板表';

-- ---------------------------------------------------------------------------
-- 2.2 模板版本表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `template_versions`;
CREATE TABLE `template_versions` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `template_id` BIGINT NOT NULL COMMENT '模板ID',
    `version_number` INT NOT NULL COMMENT '版本号',
    `version_name` VARCHAR(50) DEFAULT NULL COMMENT '版本名称',
    `snapshot` JSON NOT NULL COMMENT '完整模板快照',
    `change_summary` TEXT DEFAULT NULL COMMENT '变更摘要',
    `change_detail` JSON DEFAULT NULL COMMENT '变更明细',
    `published_at` DATETIME DEFAULT NULL COMMENT '发布时间',
    `published_by` BIGINT DEFAULT NULL COMMENT '发布人',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `created_by` BIGINT DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_version` (`template_id`, `version_number`),
    KEY `idx_template` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模板版本表';

-- ---------------------------------------------------------------------------
-- 2.3 检查轮次表 (正规化)
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `inspection_rounds`;
CREATE TABLE `inspection_rounds` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `template_id` BIGINT NOT NULL COMMENT '模板ID',
    `round_code` VARCHAR(30) NOT NULL COMMENT '轮次编码',
    `round_name` VARCHAR(50) NOT NULL COMMENT '轮次名称',
    `scheduled_start_time` TIME DEFAULT NULL COMMENT '计划开始时间',
    `scheduled_end_time` TIME DEFAULT NULL COMMENT '计划结束时间',
    `weight` DECIMAL(5,4) DEFAULT 1.0000 COMMENT '轮次权重',
    `is_required` TINYINT DEFAULT 1 COMMENT '是否必检轮次',
    `applicable_days` JSON DEFAULT NULL COMMENT '适用星期',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_round` (`template_id`, `round_code`),
    KEY `idx_template` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查轮次表';

-- ---------------------------------------------------------------------------
-- 2.4 检查类别表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `inspection_categories`;
CREATE TABLE `inspection_categories` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `template_id` BIGINT NOT NULL COMMENT '模板ID',
    `category_code` VARCHAR(50) NOT NULL COMMENT '类别编码',
    `category_name` VARCHAR(100) NOT NULL COMMENT '类别名称',
    `category_type` ENUM('HYGIENE', 'DISCIPLINE', 'ATTENDANCE', 'SAFETY', 'OTHER') NOT NULL COMMENT '类别类型',
    `max_score` DECIMAL(6,2) DEFAULT 100.00 COMMENT '满分',
    `weight` DECIMAL(5,4) DEFAULT 1.0000 COMMENT '类别权重',
    `link_type` ENUM('NONE', 'DORMITORY', 'CLASSROOM', 'STUDENT') DEFAULT 'NONE' COMMENT '关联类型',
    `applicable_round_ids` JSON DEFAULT NULL COMMENT '适用轮次ID列表',
    `icon` VARCHAR(100) DEFAULT NULL COMMENT '图标',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_category` (`template_id`, `category_code`),
    KEY `idx_template` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查类别表';

-- ---------------------------------------------------------------------------
-- 2.5 扣分项表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `deduction_items`;
CREATE TABLE `deduction_items` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `category_id` BIGINT NOT NULL COMMENT '类别ID',
    `item_code` VARCHAR(50) NOT NULL COMMENT '扣分项编码',
    `item_name` VARCHAR(200) NOT NULL COMMENT '扣分项名称',
    `description` TEXT DEFAULT NULL COMMENT '详细描述',
    `deduction_mode` ENUM('FIXED', 'PER_PERSON', 'RANGE', 'FORMULA') NOT NULL COMMENT '扣分模式',
    `fixed_score` DECIMAL(6,2) DEFAULT NULL COMMENT '固定扣分值',
    `base_score` DECIMAL(6,2) DEFAULT NULL COMMENT '基础扣分',
    `per_person_score` DECIMAL(6,2) DEFAULT NULL COMMENT '每人额外扣分',
    `max_persons` INT DEFAULT NULL COMMENT '最大人数上限',
    `range_config` JSON DEFAULT NULL COMMENT '区间配置',
    `formula_expression` VARCHAR(500) DEFAULT NULL COMMENT '公式表达式',
    `min_score` DECIMAL(6,2) DEFAULT 0 COMMENT '最小扣分',
    `max_score` DECIMAL(6,2) DEFAULT NULL COMMENT '最大扣分',
    `requires_photo` TINYINT DEFAULT 0 COMMENT '是否必须拍照',
    `requires_student_select` TINYINT DEFAULT 0 COMMENT '是否需要选择学生',
    `allows_remark` TINYINT DEFAULT 1 COMMENT '是否允许备注',
    `check_points` JSON DEFAULT NULL COMMENT '检查要点',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_category_item` (`category_id`, `item_code`),
    KEY `idx_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='扣分项表';

-- ---------------------------------------------------------------------------
-- 2.6 加权方案表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `weight_schemes`;
CREATE TABLE `weight_schemes` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `scheme_code` VARCHAR(50) NOT NULL COMMENT '方案编码',
    `scheme_name` VARCHAR(100) NOT NULL COMMENT '方案名称',
    `description` TEXT DEFAULT NULL COMMENT '方案描述',
    `weight_mode` ENUM('STANDARD', 'PER_CAPITA', 'SEGMENT', 'CUSTOM') NOT NULL COMMENT '加权模式',
    `standard_size_mode` ENUM('FIXED', 'TARGET_AVERAGE', 'RANGE_AVERAGE') DEFAULT NULL COMMENT '标准人数计算方式',
    `standard_size` INT DEFAULT NULL COMMENT '固定标准人数',
    `weight_formula` VARCHAR(200) DEFAULT NULL COMMENT '权重计算公式',
    `min_weight` DECIMAL(5,4) DEFAULT 0.5000 COMMENT '最小权重',
    `max_weight` DECIMAL(5,4) DEFAULT 2.0000 COMMENT '最大权重',
    `segment_rules` JSON DEFAULT NULL COMMENT '分段规则',
    `visibility` ENUM('GLOBAL', 'DEPARTMENT', 'PRIVATE') DEFAULT 'GLOBAL' COMMENT '可见范围',
    `owner_dept_id` BIGINT DEFAULT NULL COMMENT '所属部门',
    `owner_user_id` BIGINT DEFAULT NULL COMMENT '创建者',
    `use_count` INT DEFAULT 0 COMMENT '使用次数',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by` BIGINT DEFAULT NULL,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_scheme_code` (`scheme_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='加权方案表';

-- ---------------------------------------------------------------------------
-- 2.7 检查记录表 (聚合根)
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `inspection_records`;
CREATE TABLE `inspection_records` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `record_code` VARCHAR(50) NOT NULL COMMENT '记录编码',
    `record_name` VARCHAR(100) NOT NULL COMMENT '记录名称',
    `check_date` DATE NOT NULL COMMENT '检查日期',
    `check_type` ENUM('DAILY', 'WEEKLY', 'SPECIAL', 'RANDOM') NOT NULL COMMENT '检查类型',
    `template_id` BIGINT NOT NULL COMMENT '模板ID',
    `template_version` INT NOT NULL COMMENT '模板版本',
    `template_snapshot` JSON NOT NULL COMMENT '模板快照',
    `weight_scheme_snapshot` JSON DEFAULT NULL COMMENT '加权方案快照',
    `scope_type` ENUM('ALL', 'DEPARTMENT', 'GRADE', 'CUSTOM') NOT NULL COMMENT '检查范围类型',
    `scope_config` JSON DEFAULT NULL COMMENT '范围配置',
    `total_classes` INT DEFAULT NULL COMMENT '参与班级数',
    `total_deduction` DECIMAL(10,2) DEFAULT NULL COMMENT '总扣分',
    `avg_deduction` DECIMAL(10,2) DEFAULT NULL COMMENT '平均扣分',
    `max_deduction` DECIMAL(10,2) DEFAULT NULL COMMENT '最高扣分',
    `min_deduction` DECIMAL(10,2) DEFAULT NULL COMMENT '最低扣分',
    `status` ENUM('DRAFT', 'CHECKING', 'SUBMITTED', 'REVIEWING', 'PUBLISHED', 'APPEALING', 'FINALIZED', 'ARCHIVED') NOT NULL DEFAULT 'DRAFT' COMMENT '状态',
    `check_started_at` DATETIME DEFAULT NULL COMMENT '检查开始时间',
    `check_ended_at` DATETIME DEFAULT NULL COMMENT '检查结束时间',
    `submitted_at` DATETIME DEFAULT NULL COMMENT '提交时间',
    `reviewed_at` DATETIME DEFAULT NULL COMMENT '审核时间',
    `published_at` DATETIME DEFAULT NULL COMMENT '发布时间',
    `finalized_at` DATETIME DEFAULT NULL COMMENT '定稿时间',
    `archived_at` DATETIME DEFAULT NULL COMMENT '归档时间',
    `checker_id` BIGINT DEFAULT NULL COMMENT '检查人',
    `submitter_id` BIGINT DEFAULT NULL COMMENT '提交人',
    `reviewer_id` BIGINT DEFAULT NULL COMMENT '审核人',
    `publisher_id` BIGINT DEFAULT NULL COMMENT '发布人',
    `version` INT DEFAULT 0 COMMENT '乐观锁版本',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by` BIGINT DEFAULT NULL,
    `updated_by` BIGINT DEFAULT NULL,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_record_code` (`record_code`),
    KEY `idx_date` (`check_date`),
    KEY `idx_status` (`status`),
    KEY `idx_template` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查记录表';

-- ---------------------------------------------------------------------------
-- 2.8 班级得分表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `class_scores`;
CREATE TABLE `class_scores` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `record_id` BIGINT NOT NULL COMMENT '检查记录ID',
    `class_id` BIGINT NOT NULL COMMENT '班级ID',
    `class_code` VARCHAR(50) NOT NULL COMMENT '班级编码快照',
    `class_name` VARCHAR(100) NOT NULL COMMENT '班级名称快照',
    `class_size` INT NOT NULL COMMENT '班级人数快照',
    `org_unit_id` BIGINT NOT NULL COMMENT '所属组织单元',
    `org_unit_name` VARCHAR(100) DEFAULT NULL COMMENT '组织名称快照',
    `enrollment_year` INT DEFAULT NULL COMMENT '入学年份',
    `grade_level` INT DEFAULT NULL COMMENT '年级',
    `original_deduction` DECIMAL(10,2) DEFAULT 0 COMMENT '原始扣分',
    `weight_coefficient` DECIMAL(5,4) DEFAULT 1.0000 COMMENT '权重系数',
    `weighted_deduction` DECIMAL(10,2) DEFAULT NULL COMMENT '加权后扣分',
    `final_score` DECIMAL(10,2) DEFAULT NULL COMMENT '最终得分',
    `final_deduction` DECIMAL(10,2) DEFAULT NULL COMMENT '最终扣分',
    `overall_rank` INT DEFAULT NULL COMMENT '总排名',
    `dept_rank` INT DEFAULT NULL COMMENT '系内排名',
    `grade_rank` INT DEFAULT NULL COMMENT '年级排名',
    `vs_prev_score` DECIMAL(10,2) DEFAULT NULL COMMENT '与上次对比',
    `vs_avg_score` DECIMAL(10,2) DEFAULT NULL COMMENT '与平均对比',
    `appeal_count` INT DEFAULT 0 COMMENT '申诉数量',
    `appeal_approved` INT DEFAULT 0 COMMENT '申诉通过数',
    `appeal_score_change` DECIMAL(10,2) DEFAULT 0 COMMENT '申诉调整分数',
    `score_version` INT DEFAULT 1 COMMENT '得分版本',
    `is_latest` TINYINT DEFAULT 1 COMMENT '是否最新版本',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_record_class_version` (`record_id`, `class_id`, `score_version`),
    KEY `idx_record` (`record_id`),
    KEY `idx_class` (`class_id`),
    KEY `idx_rank` (`record_id`, `overall_rank`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级得分表';

-- ---------------------------------------------------------------------------
-- 2.9 扣分明细表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `deduction_details`;
CREATE TABLE `deduction_details` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `record_id` BIGINT NOT NULL COMMENT '检查记录ID',
    `class_score_id` BIGINT NOT NULL COMMENT '班级得分ID',
    `category_id` BIGINT NOT NULL COMMENT '类别ID',
    `category_name` VARCHAR(100) NOT NULL COMMENT '类别名称快照',
    `item_id` BIGINT NOT NULL COMMENT '扣分项ID',
    `item_name` VARCHAR(200) NOT NULL COMMENT '扣分项名称快照',
    `deduction_mode` VARCHAR(20) NOT NULL COMMENT '扣分模式',
    `round_id` BIGINT DEFAULT NULL COMMENT '检查轮次ID',
    `round_name` VARCHAR(50) DEFAULT NULL COMMENT '轮次名称',
    `link_type` ENUM('NONE', 'DORMITORY', 'CLASSROOM', 'STUDENT') DEFAULT NULL COMMENT '关联类型',
    `link_id` BIGINT DEFAULT NULL COMMENT '关联ID',
    `link_info` VARCHAR(200) DEFAULT NULL COMMENT '关联信息描述',
    `deduct_score` DECIMAL(6,2) NOT NULL COMMENT '扣分值',
    `person_count` INT DEFAULT NULL COMMENT '涉及人数',
    `student_ids` JSON DEFAULT NULL COMMENT '涉及学生ID列表',
    `photo_urls` JSON DEFAULT NULL COMMENT '照片URL列表',
    `video_urls` JSON DEFAULT NULL COMMENT '视频URL列表',
    `remark` TEXT DEFAULT NULL COMMENT '备注说明',
    `inspector_id` BIGINT DEFAULT NULL COMMENT '检查人ID',
    `inspector_name` VARCHAR(50) DEFAULT NULL COMMENT '检查人姓名',
    `inspected_at` DATETIME DEFAULT NULL COMMENT '检查时间',
    `appeal_status` ENUM('NONE', 'PENDING', 'APPROVED', 'REJECTED') DEFAULT 'NONE' COMMENT '申诉状态',
    `appeal_id` BIGINT DEFAULT NULL COMMENT '关联申诉ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_record` (`record_id`),
    KEY `idx_class_score` (`class_score_id`),
    KEY `idx_category` (`category_id`),
    KEY `idx_appeal` (`appeal_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='扣分明细表';

-- ---------------------------------------------------------------------------
-- 2.10 申诉表 (聚合根)
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `appeals`;
CREATE TABLE `appeals` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `appeal_code` VARCHAR(50) NOT NULL COMMENT '申诉编码',
    `record_id` BIGINT NOT NULL COMMENT '检查记录ID',
    `class_score_id` BIGINT NOT NULL COMMENT '班级得分ID',
    `deduction_detail_id` BIGINT NOT NULL COMMENT '扣分明细ID',
    `class_id` BIGINT NOT NULL COMMENT '班级ID',
    `class_name` VARCHAR(100) NOT NULL COMMENT '班级名称快照',
    `original_score` DECIMAL(6,2) NOT NULL COMMENT '原扣分',
    `category_name` VARCHAR(100) NOT NULL COMMENT '类别名称快照',
    `item_name` VARCHAR(200) NOT NULL COMMENT '扣分项名称快照',
    `original_evidence` JSON DEFAULT NULL COMMENT '原始证据',
    `appeal_type` ENUM('SCORE_DISPUTE', 'FACT_DISPUTE', 'PROCEDURE_DISPUTE') NOT NULL COMMENT '申诉类型',
    `appeal_reason` TEXT NOT NULL COMMENT '申诉理由',
    `expected_score` DECIMAL(6,2) DEFAULT NULL COMMENT '期望扣分',
    `evidence_urls` JSON DEFAULT NULL COMMENT '证据URL列表',
    `evidence_description` TEXT DEFAULT NULL COMMENT '证据说明',
    `appellant_id` BIGINT NOT NULL COMMENT '申诉人ID',
    `appellant_name` VARCHAR(50) NOT NULL COMMENT '申诉人姓名',
    `appellant_role` VARCHAR(50) DEFAULT NULL COMMENT '申诉人角色',
    `appeal_time` DATETIME NOT NULL COMMENT '申诉时间',
    `deadline` DATETIME NOT NULL COMMENT '申诉截止时间',
    `status` ENUM('PENDING', 'LEVEL1_REVIEWING', 'LEVEL2_REVIEWING', 'APPROVED', 'REJECTED', 'WITHDRAWN', 'EXPIRED', 'PUBLICIZING', 'EFFECTIVE') NOT NULL DEFAULT 'PENDING' COMMENT '状态',
    `current_node` INT DEFAULT 1 COMMENT '当前审批节点',
    `current_approver_ids` JSON DEFAULT NULL COMMENT '当前审批人ID列表',
    `final_decision` ENUM('APPROVED', 'REJECTED', 'PARTIAL') DEFAULT NULL COMMENT '最终决定',
    `adjusted_score` DECIMAL(6,2) DEFAULT NULL COMMENT '调整后扣分',
    `score_change` DECIMAL(6,2) DEFAULT NULL COMMENT '分数变动',
    `final_comment` TEXT DEFAULT NULL COMMENT '最终审批意见',
    `publicity_start` DATETIME DEFAULT NULL COMMENT '公示开始时间',
    `publicity_end` DATETIME DEFAULT NULL COMMENT '公示结束时间',
    `publicity_days` INT DEFAULT 3 COMMENT '公示天数',
    `effective_at` DATETIME DEFAULT NULL COMMENT '生效时间',
    `version` INT DEFAULT 0 COMMENT '乐观锁版本',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_appeal_code` (`appeal_code`),
    KEY `idx_record` (`record_id`),
    KEY `idx_class` (`class_id`),
    KEY `idx_status` (`status`),
    KEY `idx_deadline` (`deadline`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='申诉表';

-- ---------------------------------------------------------------------------
-- 2.11 申诉审批记录表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `appeal_approvals`;
CREATE TABLE `appeal_approvals` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `appeal_id` BIGINT NOT NULL COMMENT '申诉ID',
    `node_order` INT NOT NULL COMMENT '节点顺序',
    `node_name` VARCHAR(50) DEFAULT NULL COMMENT '节点名称',
    `approver_id` BIGINT NOT NULL COMMENT '审批人ID',
    `approver_name` VARCHAR(50) NOT NULL COMMENT '审批人姓名',
    `approver_role` VARCHAR(50) DEFAULT NULL COMMENT '审批人角色',
    `decision` ENUM('PENDING', 'APPROVED', 'REJECTED', 'TRANSFERRED') NOT NULL DEFAULT 'PENDING' COMMENT '审批决定',
    `comment` TEXT DEFAULT NULL COMMENT '审批意见',
    `adjusted_score` DECIMAL(6,2) DEFAULT NULL COMMENT '本节点调整分数',
    `transfer_to_id` BIGINT DEFAULT NULL COMMENT '转交给谁',
    `transfer_reason` TEXT DEFAULT NULL COMMENT '转交原因',
    `assigned_at` DATETIME DEFAULT NULL COMMENT '分配时间',
    `approved_at` DATETIME DEFAULT NULL COMMENT '审批时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_appeal` (`appeal_id`),
    KEY `idx_approver` (`approver_id`),
    KEY `idx_status` (`decision`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='申诉审批记录表';

-- ---------------------------------------------------------------------------
-- 2.12 评级模板表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `rating_templates`;
CREATE TABLE `rating_templates` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `template_code` VARCHAR(50) NOT NULL COMMENT '模板编码',
    `template_name` VARCHAR(100) NOT NULL COMMENT '模板名称',
    `description` TEXT DEFAULT NULL COMMENT '描述',
    `inspection_template_id` BIGINT DEFAULT NULL COMMENT '关联的检查模板',
    `is_default` TINYINT DEFAULT 0 COMMENT '是否默认',
    `status` TINYINT DEFAULT 1 COMMENT '状态',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_code` (`template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级模板表';

-- ---------------------------------------------------------------------------
-- 2.13 评级规则表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `rating_rules`;
CREATE TABLE `rating_rules` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `template_id` BIGINT NOT NULL COMMENT '评级模板ID',
    `rule_code` VARCHAR(50) NOT NULL COMMENT '规则编码',
    `rule_name` VARCHAR(100) NOT NULL COMMENT '规则名称',
    `rating_basis` ENUM('TOTAL', 'SINGLE_CATEGORY', 'MULTI_CATEGORY', 'CUSTOM') NOT NULL COMMENT '评级依据',
    `category_ids` JSON DEFAULT NULL COMMENT '依据的类别ID列表',
    `score_type` ENUM('DEDUCTION', 'WEIGHTED', 'FINAL') NOT NULL COMMENT '使用的分数类型',
    `rating_method` ENUM('ABSOLUTE', 'PERCENTAGE', 'RANKING') NOT NULL COMMENT '评级方式',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `is_enabled` TINYINT DEFAULT 1 COMMENT '是否启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_rule` (`template_id`, `rule_code`),
    KEY `idx_template` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级规则表';

-- ---------------------------------------------------------------------------
-- 2.14 评级等级表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `rating_levels`;
CREATE TABLE `rating_levels` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `rule_id` BIGINT NOT NULL COMMENT '规则ID',
    `level_code` VARCHAR(20) NOT NULL COMMENT '等级代码',
    `level_name` VARCHAR(50) NOT NULL COMMENT '等级名称',
    `min_score` DECIMAL(10,2) DEFAULT NULL COMMENT '最小分数',
    `max_score` DECIMAL(10,2) DEFAULT NULL COMMENT '最大分数',
    `min_percent` DECIMAL(5,2) DEFAULT NULL COMMENT '最小百分比',
    `max_percent` DECIMAL(5,2) DEFAULT NULL COMMENT '最大百分比',
    `top_n` INT DEFAULT NULL COMMENT '前N名',
    `top_percent` DECIMAL(5,2) DEFAULT NULL COMMENT '前N%',
    `color` VARCHAR(20) DEFAULT NULL COMMENT '颜色代码',
    `icon` VARCHAR(100) DEFAULT NULL COMMENT '图标',
    `reward_points` INT DEFAULT 0 COMMENT '奖励积分',
    `penalty_points` INT DEFAULT 0 COMMENT '惩罚积分',
    `level_order` INT NOT NULL COMMENT '等级顺序',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_rule` (`rule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级等级表';

-- ---------------------------------------------------------------------------
-- 2.15 评级结果表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `rating_results`;
CREATE TABLE `rating_results` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `record_id` BIGINT NOT NULL COMMENT '检查记录ID',
    `class_score_id` BIGINT NOT NULL COMMENT '班级得分ID',
    `class_id` BIGINT NOT NULL COMMENT '班级ID',
    `rule_id` BIGINT NOT NULL COMMENT '评级规则ID',
    `level_id` BIGINT NOT NULL COMMENT '评级等级ID',
    `rule_name` VARCHAR(100) NOT NULL COMMENT '规则名称快照',
    `level_code` VARCHAR(20) NOT NULL COMMENT '等级代码快照',
    `level_name` VARCHAR(50) NOT NULL COMMENT '等级名称快照',
    `calculated_score` DECIMAL(10,2) DEFAULT NULL COMMENT '计算分数',
    `calculated_percent` DECIMAL(5,2) DEFAULT NULL COMMENT '百分比',
    `calculated_rank` INT DEFAULT NULL COMMENT '排名',
    `reward_points` INT DEFAULT 0 COMMENT '奖励积分',
    `result_version` INT DEFAULT 1 COMMENT '结果版本',
    `is_latest` TINYINT DEFAULT 1 COMMENT '是否最新',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_record_class_rule_version` (`record_id`, `class_id`, `rule_id`, `result_version`),
    KEY `idx_record` (`record_id`),
    KEY `idx_class` (`class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级结果表';

-- ============================================================================
-- 第三部分: 权限系统
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 3.1 用户表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码',
    `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
    `employee_no` VARCHAR(50) DEFAULT NULL COMMENT '工号/学号',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    `user_type` ENUM('ADMIN', 'TEACHER', 'STUDENT', 'PARENT', 'STAFF') NOT NULL COMMENT '用户类型',
    `primary_org_unit_id` BIGINT DEFAULT NULL COMMENT '主要归属组织',
    `status` ENUM('ACTIVE', 'LOCKED', 'DISABLED', 'PENDING') DEFAULT 'ACTIVE' COMMENT '状态',
    `password_changed_at` DATETIME DEFAULT NULL COMMENT '密码修改时间',
    `last_login_at` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
    `login_fail_count` INT DEFAULT 0 COMMENT '登录失败次数',
    `locked_until` DATETIME DEFAULT NULL COMMENT '锁定截止时间',
    `wechat_openid` VARCHAR(100) DEFAULT NULL COMMENT '微信OpenID',
    `wechat_unionid` VARCHAR(100) DEFAULT NULL COMMENT '微信UnionID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by` BIGINT DEFAULT NULL,
    `updated_by` BIGINT DEFAULT NULL,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`),
    UNIQUE KEY `uk_email` (`email`),
    KEY `idx_org_unit` (`primary_org_unit_id`),
    KEY `idx_user_type` (`user_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ---------------------------------------------------------------------------
-- 3.2 角色表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `roles`;
CREATE TABLE `roles` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码',
    `role_name` VARCHAR(100) NOT NULL COMMENT '角色名称',
    `description` TEXT DEFAULT NULL COMMENT '角色描述',
    `role_type` ENUM('SYSTEM', 'CUSTOM') DEFAULT 'CUSTOM' COMMENT '角色类型',
    `role_level` INT DEFAULT 0 COMMENT '角色级别',
    `parent_role_id` BIGINT DEFAULT NULL COMMENT '父角色ID',
    `default_data_scope` ENUM('ALL', 'DEPARTMENT', 'DEPARTMENT_AND_CHILD', 'SELF_DEPARTMENT', 'SELF', 'CUSTOM') DEFAULT 'SELF' COMMENT '默认数据范围',
    `status` TINYINT DEFAULT 1 COMMENT '状态',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- ---------------------------------------------------------------------------
-- 3.3 权限表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `permissions`;
CREATE TABLE `permissions` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `permission_code` VARCHAR(100) NOT NULL COMMENT '权限编码',
    `permission_name` VARCHAR(100) NOT NULL COMMENT '权限名称',
    `permission_type` ENUM('MENU', 'BUTTON', 'API', 'DATA') NOT NULL COMMENT '权限类型',
    `parent_id` BIGINT DEFAULT NULL COMMENT '父权限ID',
    `tree_path` VARCHAR(500) DEFAULT NULL COMMENT '路径',
    `tree_level` INT DEFAULT 1 COMMENT '层级',
    `resource_type` VARCHAR(50) DEFAULT NULL COMMENT '资源类型',
    `resource_action` VARCHAR(50) DEFAULT NULL COMMENT '资源操作',
    `route_path` VARCHAR(200) DEFAULT NULL COMMENT '前端路由',
    `component` VARCHAR(200) DEFAULT NULL COMMENT '前端组件',
    `icon` VARCHAR(100) DEFAULT NULL COMMENT '图标',
    `api_path` VARCHAR(200) DEFAULT NULL COMMENT 'API路径',
    `api_method` VARCHAR(10) DEFAULT NULL COMMENT 'HTTP方法',
    `status` TINYINT DEFAULT 1 COMMENT '状态',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `is_visible` TINYINT DEFAULT 1 COMMENT '是否显示',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_permission_code` (`permission_code`),
    KEY `idx_parent` (`parent_id`),
    KEY `idx_type` (`permission_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- ---------------------------------------------------------------------------
-- 3.4 用户角色关联表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `user_roles`;
CREATE TABLE `user_roles` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `start_date` DATE DEFAULT NULL COMMENT '生效日期',
    `end_date` DATE DEFAULT NULL COMMENT '失效日期',
    `scope_type` ENUM('GLOBAL', 'ORG_UNIT', 'CLASS') DEFAULT NULL COMMENT '角色范围类型',
    `scope_id` BIGINT DEFAULT NULL COMMENT '范围ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `created_by` BIGINT DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role_scope` (`user_id`, `role_id`, `scope_type`, `scope_id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- ---------------------------------------------------------------------------
-- 3.5 角色权限关联表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `role_permissions`;
CREATE TABLE `role_permissions` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `permission_id` BIGINT NOT NULL COMMENT '权限ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `created_by` BIGINT DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
    KEY `idx_role` (`role_id`),
    KEY `idx_permission` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- ---------------------------------------------------------------------------
-- 3.6 数据权限配置表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `data_permissions`;
CREATE TABLE `data_permissions` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `principal_type` ENUM('USER', 'ROLE') NOT NULL COMMENT '主体类型',
    `principal_id` BIGINT NOT NULL COMMENT '主体ID',
    `resource_module` VARCHAR(50) NOT NULL COMMENT '资源模块',
    `data_scope` ENUM('ALL', 'DEPARTMENT', 'DEPARTMENT_AND_CHILD', 'SELF_DEPARTMENT', 'GRADE', 'CLASS', 'SELF', 'CUSTOM') NOT NULL COMMENT '数据范围',
    `custom_org_unit_ids` JSON DEFAULT NULL COMMENT '自定义组织ID列表',
    `custom_class_ids` JSON DEFAULT NULL COMMENT '自定义班级ID列表',
    `custom_user_ids` JSON DEFAULT NULL COMMENT '自定义用户ID列表',
    `allowed_actions` JSON DEFAULT '["read"]' COMMENT '允许的操作',
    `conditions` JSON DEFAULT NULL COMMENT 'ABAC条件表达式',
    `priority` INT DEFAULT 0 COMMENT '优先级',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by` BIGINT DEFAULT NULL,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_principal` (`principal_type`, `principal_id`),
    KEY `idx_module` (`resource_module`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据权限配置表';

-- ---------------------------------------------------------------------------
-- 3.7 Casbin策略表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `casbin_rule`;
CREATE TABLE `casbin_rule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `ptype` VARCHAR(100) NOT NULL COMMENT '策略类型',
    `v0` VARCHAR(100) DEFAULT NULL COMMENT '字段0',
    `v1` VARCHAR(100) DEFAULT NULL COMMENT '字段1',
    `v2` VARCHAR(100) DEFAULT NULL COMMENT '字段2',
    `v3` VARCHAR(100) DEFAULT NULL COMMENT '字段3',
    `v4` VARCHAR(100) DEFAULT NULL COMMENT '字段4',
    `v5` VARCHAR(100) DEFAULT NULL COMMENT '字段5',
    PRIMARY KEY (`id`),
    KEY `idx_ptype` (`ptype`),
    KEY `idx_v0` (`v0`),
    KEY `idx_v1` (`v1`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Casbin策略表';

-- ============================================================================
-- 第四部分: 审计与事件
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 4.1 操作审计日志表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `audit_logs`;
CREATE TABLE `audit_logs` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `operation_type` VARCHAR(50) NOT NULL COMMENT '操作类型',
    `operation_module` VARCHAR(50) NOT NULL COMMENT '操作模块',
    `operation_desc` VARCHAR(500) DEFAULT NULL COMMENT '操作描述',
    `target_type` VARCHAR(50) DEFAULT NULL COMMENT '目标类型',
    `target_id` BIGINT DEFAULT NULL COMMENT '目标ID',
    `target_name` VARCHAR(200) DEFAULT NULL COMMENT '目标名称',
    `operator_id` BIGINT NOT NULL COMMENT '操作人ID',
    `operator_name` VARCHAR(50) NOT NULL COMMENT '操作人姓名',
    `operator_ip` VARCHAR(50) DEFAULT NULL COMMENT '操作人IP',
    `operator_ua` VARCHAR(500) DEFAULT NULL COMMENT 'User-Agent',
    `old_value` JSON DEFAULT NULL COMMENT '变更前值',
    `new_value` JSON DEFAULT NULL COMMENT '变更后值',
    `diff_value` JSON DEFAULT NULL COMMENT '差异',
    `request_url` VARCHAR(500) DEFAULT NULL COMMENT '请求URL',
    `request_method` VARCHAR(10) DEFAULT NULL COMMENT 'HTTP方法',
    `request_params` JSON DEFAULT NULL COMMENT '请求参数',
    `response_code` INT DEFAULT NULL COMMENT '响应码',
    `execution_time` BIGINT DEFAULT NULL COMMENT '执行耗时(ms)',
    `success` TINYINT DEFAULT 1 COMMENT '是否成功',
    `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_operator` (`operator_id`),
    KEY `idx_module` (`operation_module`),
    KEY `idx_target` (`target_type`, `target_id`),
    KEY `idx_time` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作审计日志表';

-- ---------------------------------------------------------------------------
-- 4.2 领域事件存储表 (可选,用于事件溯源)
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `domain_events`;
CREATE TABLE `domain_events` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `event_id` VARCHAR(36) NOT NULL COMMENT '事件UUID',
    `event_type` VARCHAR(100) NOT NULL COMMENT '事件类型',
    `aggregate_type` VARCHAR(100) NOT NULL COMMENT '聚合类型',
    `aggregate_id` VARCHAR(100) NOT NULL COMMENT '聚合ID',
    `aggregate_version` INT NOT NULL COMMENT '聚合版本',
    `payload` JSON NOT NULL COMMENT '事件数据',
    `metadata` JSON DEFAULT NULL COMMENT '元数据',
    `occurred_at` DATETIME(3) NOT NULL COMMENT '发生时间',
    `created_at` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_event_id` (`event_id`),
    KEY `idx_aggregate` (`aggregate_type`, `aggregate_id`),
    KEY `idx_occurred` (`occurred_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='领域事件存储表';

-- ---------------------------------------------------------------------------
-- 4.3 事件发布记录表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `event_publications`;
CREATE TABLE `event_publications` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `event_id` VARCHAR(36) NOT NULL COMMENT '事件ID',
    `event_type` VARCHAR(100) NOT NULL COMMENT '事件类型',
    `status` ENUM('PENDING', 'PUBLISHED', 'FAILED') DEFAULT 'PENDING' COMMENT '状态',
    `retry_count` INT DEFAULT 0 COMMENT '重试次数',
    `last_error` TEXT DEFAULT NULL COMMENT '最后错误',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `published_at` DATETIME DEFAULT NULL COMMENT '发布时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_event_id` (`event_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='事件发布记录表';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- 结束
-- ============================================================================
