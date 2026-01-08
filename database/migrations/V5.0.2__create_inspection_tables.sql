-- ============================================================================
-- 学生管理系统 - 架构重构迁移脚本 V5.0.2
-- 创建量化检查模块新表结构
-- 日期: 2026-01-02
-- 说明: 重构量化检查系统，统一 V3/V4/V5 为终极设计
-- ============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------------
-- 1. 检查模板表 (新设计)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `inspection_templates` (
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
    UNIQUE KEY `uk_template_code` (`template_code`),
    KEY `idx_status` (`status`),
    KEY `idx_default` (`is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查模板表';

-- ---------------------------------------------------------------------------
-- 2. 模板版本表 (支持版本控制)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `template_versions` (
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
-- 3. 检查轮次表 (正规化设计，替代 JSON 存储)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `inspection_rounds` (
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
-- 4. 检查类别表
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `inspection_categories` (
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
-- 5. 扣分项表 (新设计)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `deduction_items_v2` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='扣分项表(V2)';

-- ---------------------------------------------------------------------------
-- 6. 加权方案表
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `weight_schemes` (
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
    UNIQUE KEY `uk_scheme_code` (`scheme_code`),
    KEY `idx_visibility` (`visibility`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='加权方案表';

-- ---------------------------------------------------------------------------
-- 7. 检查记录表 (聚合根)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `inspection_records` (
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
-- 8. 班级得分表
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `class_scores` (
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
-- 9. 扣分明细表
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `deduction_details` (
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
-- 10. 申诉表 (聚合根，带状态机)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `appeals_v2` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='申诉表(V2)';

-- ---------------------------------------------------------------------------
-- 11. 申诉审批记录表
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `appeal_approvals` (
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

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- 迁移完成标记
-- ============================================================================
-- 执行完成后，请继续执行 V5.0.3__create_rating_tables.sql
