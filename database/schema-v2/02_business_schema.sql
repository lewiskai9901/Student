-- =====================================================
-- 学生管理系统 - 业务表结构
-- 版本: 2.0 (合并后)
-- 创建日期: 2026-01-06
-- 包含: 量化检查、评级系统、任务工作流
-- =====================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================
-- 第一部分: 量化检查系统
-- =====================================================

-- ---------------------------------------------------
-- 1.1 检查模板表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `check_templates`;
CREATE TABLE `check_templates` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `template_code` VARCHAR(50) NOT NULL COMMENT '模板编码',
    `template_name` VARCHAR(100) NOT NULL COMMENT '模板名称',
    `description` TEXT COMMENT '模板描述',
    `template_type` VARCHAR(20) DEFAULT 'DAILY' COMMENT '类型:DAILY,WEEKLY,SPECIAL',
    `base_score` DECIMAL(6,2) DEFAULT 100.00 COMMENT '基础分数',
    `version` INT DEFAULT 1 COMMENT '版本号',
    `status` VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态:DRAFT,PUBLISHED,DEPRECATED',
    `is_default` TINYINT DEFAULT 0 COMMENT '是否默认模板',
    `applicable_scope` VARCHAR(20) DEFAULT 'ALL' COMMENT '适用范围:ALL,DEPARTMENT,GRADE',
    `applicable_config` JSON COMMENT '适用范围配置',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` BIGINT COMMENT '创建人',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by` BIGINT COMMENT '更新人',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_code` (`template_code`),
    INDEX `idx_status` (`status`),
    INDEX `idx_is_default` (`is_default`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查模板表';

-- ---------------------------------------------------
-- 1.2 检查类别表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `check_categories`;
CREATE TABLE `check_categories` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `template_id` BIGINT NOT NULL COMMENT '模板ID',
    `category_code` VARCHAR(50) NOT NULL COMMENT '类别编码',
    `category_name` VARCHAR(100) NOT NULL COMMENT '类别名称',
    `description` VARCHAR(500) COMMENT '类别描述',
    `weight` DECIMAL(5,2) DEFAULT 1.00 COMMENT '权重',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_template_id` (`template_id`),
    INDEX `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查类别表';

-- ---------------------------------------------------
-- 1.3 扣分项表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `deduction_items`;
CREATE TABLE `deduction_items` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `category_id` BIGINT NOT NULL COMMENT '类别ID',
    `item_code` VARCHAR(50) NOT NULL COMMENT '项目编码',
    `item_name` VARCHAR(200) NOT NULL COMMENT '项目名称',
    `description` TEXT COMMENT '项目描述',
    `deduction_mode` VARCHAR(20) NOT NULL COMMENT '扣分模式:FIXED,PER_PERSON,RANGE,FORMULA',
    `fixed_score` DECIMAL(6,2) COMMENT '固定扣分',
    `base_score` DECIMAL(6,2) COMMENT '基础扣分(按人数)',
    `per_person_score` DECIMAL(6,2) COMMENT '每人扣分',
    `max_persons` INT COMMENT '最大人数',
    `range_config` JSON COMMENT '区间配置',
    `min_score` DECIMAL(6,2) DEFAULT 0 COMMENT '最小扣分',
    `max_score` DECIMAL(6,2) COMMENT '最大扣分',
    `requires_photo` TINYINT DEFAULT 0 COMMENT '是否需要拍照',
    `requires_student_select` TINYINT DEFAULT 0 COMMENT '是否需要选择学生',
    `check_points` JSON COMMENT '检查要点',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT DEFAULT 1 COMMENT '状态:0禁用,1启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` BIGINT COMMENT '创建人',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by` BIGINT COMMENT '更新人',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    INDEX `idx_category_id` (`category_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='扣分项表';

-- ---------------------------------------------------
-- 1.4 检查计划表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `check_plans`;
CREATE TABLE `check_plans` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `plan_code` VARCHAR(50) NOT NULL COMMENT '计划编码',
    `plan_name` VARCHAR(100) NOT NULL COMMENT '计划名称',
    `template_id` BIGINT NOT NULL COMMENT '模板ID',
    `semester_id` BIGINT COMMENT '学期ID',
    `check_type` VARCHAR(20) DEFAULT 'DAILY' COMMENT '检查类型:DAILY,WEEKLY,SPECIAL',
    `start_date` DATE COMMENT '开始日期',
    `end_date` DATE COMMENT '结束日期',
    `check_time_config` JSON COMMENT '检查时间配置',
    `scope_type` VARCHAR(20) DEFAULT 'ALL' COMMENT '范围类型:ALL,DEPARTMENT,GRADE',
    `scope_config` JSON COMMENT '范围配置',
    `status` VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态:DRAFT,ACTIVE,PAUSED,COMPLETED',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` BIGINT COMMENT '创建人',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by` BIGINT COMMENT '更新人',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_plan_code` (`plan_code`),
    INDEX `idx_template_id` (`template_id`),
    INDEX `idx_semester_id` (`semester_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查计划表';

-- ---------------------------------------------------
-- 1.5 检查记录表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `check_records`;
CREATE TABLE `check_records` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `record_code` VARCHAR(50) NOT NULL COMMENT '记录编码',
    `plan_id` BIGINT NOT NULL COMMENT '计划ID',
    `template_id` BIGINT NOT NULL COMMENT '模板ID',
    `template_version` INT DEFAULT 1 COMMENT '模板版本',
    `check_date` DATE NOT NULL COMMENT '检查日期',
    `check_round` VARCHAR(20) COMMENT '检查轮次:MORNING,NOON,EVENING',
    `template_snapshot` JSON COMMENT '模板快照',
    `status` VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态:DRAFT,CHECKING,SUBMITTED,REVIEWING,PUBLISHED,VOIDED',
    `checker_id` BIGINT COMMENT '检查人ID',
    `checker_name` VARCHAR(50) COMMENT '检查人姓名',
    `checked_at` DATETIME COMMENT '检查时间',
    `reviewer_id` BIGINT COMMENT '审核人ID',
    `reviewed_at` DATETIME COMMENT '审核时间',
    `published_at` DATETIME COMMENT '发布时间',
    `remark` VARCHAR(500) COMMENT '备注',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` BIGINT COMMENT '创建人',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by` BIGINT COMMENT '更新人',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_record_code` (`record_code`),
    INDEX `idx_plan_id` (`plan_id`),
    INDEX `idx_check_date` (`check_date`),
    INDEX `idx_status` (`status`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查记录表';

-- ---------------------------------------------------
-- 1.6 班级得分表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `class_scores`;
CREATE TABLE `class_scores` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `record_id` BIGINT NOT NULL COMMENT '检查记录ID',
    `class_id` BIGINT NOT NULL COMMENT '班级ID',
    `class_code` VARCHAR(50) COMMENT '班级编码',
    `class_name` VARCHAR(100) COMMENT '班级名称',
    `class_size` INT COMMENT '班级人数',
    `org_unit_id` BIGINT COMMENT '组织单元ID',
    `org_unit_name` VARCHAR(100) COMMENT '组织单元名称',
    `base_score` DECIMAL(10,2) DEFAULT 100.00 COMMENT '基础分',
    `total_deduction` DECIMAL(10,2) DEFAULT 0 COMMENT '总扣分',
    `weight_coefficient` DECIMAL(5,4) DEFAULT 1.0000 COMMENT '加权系数',
    `weighted_deduction` DECIMAL(10,2) COMMENT '加权扣分',
    `final_score` DECIMAL(10,2) COMMENT '最终得分',
    `overall_rank` INT COMMENT '总排名',
    `dept_rank` INT COMMENT '部门排名',
    `grade_rank` INT COMMENT '年级排名',
    `score_version` INT DEFAULT 1 COMMENT '分数版本',
    `is_latest` TINYINT DEFAULT 1 COMMENT '是否最新版本',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_record_class_version` (`record_id`, `class_id`, `score_version`),
    INDEX `idx_record_id` (`record_id`),
    INDEX `idx_class_id` (`class_id`),
    INDEX `idx_org_unit_id` (`org_unit_id`),
    INDEX `idx_final_score` (`final_score`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级得分表';

-- ---------------------------------------------------
-- 1.7 扣分明细表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `deduction_details`;
CREATE TABLE `deduction_details` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `class_score_id` BIGINT NOT NULL COMMENT '班级得分ID',
    `record_id` BIGINT NOT NULL COMMENT '检查记录ID',
    `class_id` BIGINT NOT NULL COMMENT '班级ID',
    `deduction_item_id` BIGINT NOT NULL COMMENT '扣分项ID',
    `item_name` VARCHAR(200) COMMENT '扣分项名称',
    `category_id` BIGINT COMMENT '类别ID',
    `category_name` VARCHAR(100) COMMENT '类别名称',
    `count` INT DEFAULT 1 COMMENT '数量/人数',
    `deduction_amount` DECIMAL(6,2) NOT NULL COMMENT '扣分分值',
    `student_ids` JSON COMMENT '涉及学生ID列表',
    `evidence_urls` JSON COMMENT '证据图片URL列表',
    `location` VARCHAR(100) COMMENT '位置',
    `remark` VARCHAR(500) COMMENT '备注',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` BIGINT COMMENT '创建人',
    PRIMARY KEY (`id`),
    INDEX `idx_class_score_id` (`class_score_id`),
    INDEX `idx_record_id` (`record_id`),
    INDEX `idx_class_id` (`class_id`),
    INDEX `idx_deduction_item_id` (`deduction_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='扣分明细表';

-- ---------------------------------------------------
-- 1.8 申诉表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `check_item_appeals`;
CREATE TABLE `check_item_appeals` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `appeal_code` VARCHAR(50) NOT NULL COMMENT '申诉编号',
    `record_id` BIGINT NOT NULL COMMENT '检查记录ID',
    `class_id` BIGINT NOT NULL COMMENT '班级ID',
    `class_score_id` BIGINT COMMENT '班级得分ID',
    `deduction_detail_id` BIGINT COMMENT '扣分明细ID',
    `appeal_type` VARCHAR(20) NOT NULL COMMENT '申诉类型:DEDUCTION,SCORE,OTHER',
    `appeal_reason` TEXT NOT NULL COMMENT '申诉理由',
    `evidence_urls` JSON COMMENT '证据图片URL列表',
    `expected_score_change` DECIMAL(6,2) COMMENT '期望分数变更',
    `status` VARCHAR(30) DEFAULT 'PENDING' COMMENT '状态:PENDING,LEVEL1_REVIEWING,LEVEL2_REVIEWING,APPROVED,REJECTED,WITHDRAWN,EXPIRED',
    `current_level` INT DEFAULT 1 COMMENT '当前审核级别',
    `submitter_id` BIGINT NOT NULL COMMENT '提交人ID',
    `submitter_name` VARCHAR(50) COMMENT '提交人姓名',
    `submitted_at` DATETIME NOT NULL COMMENT '提交时间',
    `final_result` VARCHAR(20) COMMENT '最终结果:APPROVED,REJECTED',
    `actual_score_change` DECIMAL(6,2) COMMENT '实际分数变更',
    `final_reviewer_id` BIGINT COMMENT '最终审核人ID',
    `final_reviewed_at` DATETIME COMMENT '最终审核时间',
    `final_remark` VARCHAR(500) COMMENT '最终审核备注',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_appeal_code` (`appeal_code`),
    INDEX `idx_record_id` (`record_id`),
    INDEX `idx_class_id` (`class_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_submitter_id` (`submitter_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='申诉表';

-- ---------------------------------------------------
-- 1.9 申诉审批记录表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `appeal_approval_records`;
CREATE TABLE `appeal_approval_records` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `appeal_id` BIGINT NOT NULL COMMENT '申诉ID',
    `approval_level` INT NOT NULL COMMENT '审批级别',
    `approver_id` BIGINT NOT NULL COMMENT '审批人ID',
    `approver_name` VARCHAR(50) COMMENT '审批人姓名',
    `action` VARCHAR(20) NOT NULL COMMENT '操作:APPROVE,REJECT,FORWARD',
    `opinion` VARCHAR(500) COMMENT '审批意见',
    `approved_score_change` DECIMAL(6,2) COMMENT '同意的分数变更',
    `approved_at` DATETIME NOT NULL COMMENT '审批时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_appeal_id` (`appeal_id`),
    INDEX `idx_approver_id` (`approver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='申诉审批记录表';

-- =====================================================
-- 第二部分: 评级系统
-- =====================================================

-- ---------------------------------------------------
-- 2.1 评级规则表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `check_plan_rating_rules`;
CREATE TABLE `check_plan_rating_rules` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `check_plan_id` BIGINT NOT NULL COMMENT '检查计划ID',
    `rule_name` VARCHAR(100) NOT NULL COMMENT '规则名称',
    `rule_type` VARCHAR(20) NOT NULL COMMENT '类型:DAILY,SUMMARY',
    `score_source` VARCHAR(20) DEFAULT 'TOTAL' COMMENT '评分来源:TOTAL,CATEGORY',
    `category_id` BIGINT COMMENT '类别ID',
    `category_name` VARCHAR(100) COMMENT '类别名称',
    `use_weighted_score` TINYINT DEFAULT 0 COMMENT '是否使用加权分',
    `division_method` VARCHAR(20) NOT NULL COMMENT '划分方式:SCORE_RANGE,RANK_COUNT,PERCENTAGE',
    `summary_method` VARCHAR(20) DEFAULT 'AVERAGE' COMMENT '汇总方式:AVERAGE,SUM',
    `rating_cycle` VARCHAR(20) DEFAULT 'DAILY' COMMENT '评级周期:DAILY,WEEKLY,MONTHLY',
    `cycle_config` JSON COMMENT '周期配置',
    `require_approval` TINYINT DEFAULT 0 COMMENT '是否需要审核',
    `auto_calculate` TINYINT DEFAULT 1 COMMENT '是否自动计算',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `enabled` TINYINT DEFAULT 1 COMMENT '是否启用',
    `description` VARCHAR(500) COMMENT '描述',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` BIGINT COMMENT '创建人',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    INDEX `idx_check_plan_id` (`check_plan_id`),
    INDEX `idx_rule_type` (`rule_type`),
    INDEX `idx_enabled` (`enabled`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级规则表';

-- ---------------------------------------------------
-- 2.2 评级等级表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `check_plan_rating_levels`;
CREATE TABLE `check_plan_rating_levels` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `rule_id` BIGINT NOT NULL COMMENT '规则ID',
    `level_order` INT NOT NULL COMMENT '等级顺序',
    `level_name` VARCHAR(50) NOT NULL COMMENT '等级名称',
    `level_color` VARCHAR(20) COMMENT '等级颜色',
    `level_icon` VARCHAR(100) COMMENT '等级图标',
    `min_score` DECIMAL(10,2) COMMENT '最小分数',
    `max_score` DECIMAL(10,2) COMMENT '最大分数',
    `rank_count` INT COMMENT '名次数量',
    `percentage` DECIMAL(5,2) COMMENT '百分比',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_rule_id` (`rule_id`),
    INDEX `idx_level_order` (`level_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级等级表';

-- ---------------------------------------------------
-- 2.3 评级结果表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `check_plan_rating_results`;
CREATE TABLE `check_plan_rating_results` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `rule_id` BIGINT NOT NULL COMMENT '规则ID',
    `check_plan_id` BIGINT NOT NULL COMMENT '检查计划ID',
    `check_record_id` BIGINT COMMENT '检查记录ID',
    `check_date` DATE COMMENT '检查日期',
    `class_id` BIGINT NOT NULL COMMENT '班级ID',
    `class_name` VARCHAR(100) COMMENT '班级名称',
    `grade_id` BIGINT COMMENT '年级ID',
    `grade_name` VARCHAR(50) COMMENT '年级名称',
    `level_id` BIGINT NOT NULL COMMENT '等级ID',
    `level_name` VARCHAR(50) NOT NULL COMMENT '等级名称',
    `level_order` INT NOT NULL COMMENT '等级顺序',
    `ranking` INT COMMENT '排名',
    `total_classes` INT COMMENT '参评班级数',
    `score` DECIMAL(10,2) NOT NULL COMMENT '得分/扣分',
    `period_start` DATE COMMENT '周期开始',
    `period_end` DATE COMMENT '周期结束',
    `record_count` INT COMMENT '记录数',
    `approval_status` TINYINT DEFAULT 0 COMMENT '审核状态:0待审,1通过,2驳回',
    `approved_by` BIGINT COMMENT '审核人ID',
    `approved_at` DATETIME COMMENT '审核时间',
    `approval_remark` VARCHAR(500) COMMENT '审核备注',
    `publish_status` TINYINT DEFAULT 0 COMMENT '发布状态:0未发布,1已发布',
    `published_at` DATETIME COMMENT '发布时间',
    `version` INT DEFAULT 1 COMMENT '版本号',
    `original_level_id` BIGINT COMMENT '原始等级ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_rule_id` (`rule_id`),
    INDEX `idx_check_plan_id` (`check_plan_id`),
    INDEX `idx_check_record_id` (`check_record_id`),
    INDEX `idx_class_id` (`class_id`),
    INDEX `idx_check_date` (`check_date`),
    INDEX `idx_period` (`period_start`, `period_end`),
    UNIQUE KEY `uk_daily_result` (`rule_id`, `check_record_id`, `class_id`),
    UNIQUE KEY `uk_summary_result` (`rule_id`, `period_start`, `period_end`, `class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级结果表';

-- ---------------------------------------------------
-- 2.4 评级频次统计表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `check_plan_rating_frequency`;
CREATE TABLE `check_plan_rating_frequency` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `check_plan_id` BIGINT NOT NULL COMMENT '检查计划ID',
    `class_id` BIGINT NOT NULL COMMENT '班级ID',
    `class_name` VARCHAR(100) COMMENT '班级名称',
    `grade_id` BIGINT COMMENT '年级ID',
    `grade_name` VARCHAR(100) COMMENT '年级名称',
    `rule_id` BIGINT NOT NULL COMMENT '规则ID',
    `rule_name` VARCHAR(100) COMMENT '规则名称',
    `level_id` BIGINT NOT NULL COMMENT '等级ID',
    `level_name` VARCHAR(50) NOT NULL COMMENT '等级名称',
    `level_color` VARCHAR(20) COMMENT '等级颜色',
    `frequency` INT DEFAULT 0 COMMENT '频次',
    `total_ratings` INT DEFAULT 0 COMMENT '总评级次数',
    `frequency_rate` DECIMAL(5,2) DEFAULT 0 COMMENT '频次占比',
    `period_type` VARCHAR(20) NOT NULL COMMENT '周期类型',
    `period_start` DATE NOT NULL COMMENT '周期开始',
    `period_end` DATE NOT NULL COMMENT '周期结束',
    `period_label` VARCHAR(50) COMMENT '周期标签',
    `last_rating_date` DATE COMMENT '最近评级日期',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_plan_period` (`check_plan_id`, `period_type`, `period_start`, `period_end`),
    INDEX `idx_class_rule` (`class_id`, `rule_id`),
    UNIQUE KEY `uk_class_rule_level_period` (`class_id`, `rule_id`, `level_id`, `period_type`, `period_start`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级频次统计表';

-- =====================================================
-- 第三部分: 任务工作流
-- =====================================================

-- ---------------------------------------------------
-- 3.1 流程模板表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `workflow_templates`;
CREATE TABLE `workflow_templates` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `template_name` VARCHAR(100) NOT NULL COMMENT '模板名称',
    `template_code` VARCHAR(50) NOT NULL COMMENT '模板编码',
    `template_type` VARCHAR(30) DEFAULT 'TASK' COMMENT '类型:TASK,LEAVE,OTHER',
    `description` VARCHAR(500) COMMENT '描述',
    `process_definition_id` VARCHAR(64) COMMENT 'Flowable流程定义ID',
    `process_definition_key` VARCHAR(64) COMMENT 'Flowable流程Key',
    `bpmn_xml` LONGTEXT COMMENT 'BPMN定义XML',
    `form_config` JSON COMMENT '表单配置',
    `node_config` JSON COMMENT '节点配置',
    `is_default` TINYINT DEFAULT 0 COMMENT '是否默认',
    `status` TINYINT DEFAULT 1 COMMENT '状态:0禁用,1启用',
    `version` INT DEFAULT 1 COMMENT '版本号',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` BIGINT COMMENT '创建人',
    `created_by_name` VARCHAR(50) COMMENT '创建人姓名',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by` BIGINT COMMENT '更新人',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_code` (`template_code`),
    INDEX `idx_template_type` (`template_type`),
    INDEX `idx_status` (`status`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程模板表';

-- ---------------------------------------------------
-- 3.2 任务表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `tasks`;
CREATE TABLE `tasks` (
    `id` BIGINT NOT NULL COMMENT '任务ID',
    `task_code` VARCHAR(50) NOT NULL COMMENT '任务编号',
    `title` VARCHAR(200) NOT NULL COMMENT '任务标题',
    `description` TEXT COMMENT '任务描述',
    `priority` TINYINT DEFAULT 2 COMMENT '优先级:1紧急,2普通,3低',
    `status` TINYINT DEFAULT 0 COMMENT '状态:0待接收,1进行中,2待审核,3已完成,4已打回,5已取消,6审批中',
    `assigner_id` BIGINT NOT NULL COMMENT '分配人ID',
    `assigner_name` VARCHAR(50) COMMENT '分配人姓名',
    `assign_type` TINYINT DEFAULT 1 COMMENT '分配类型:1指定个人,2批量分配',
    `assignee_id` BIGINT COMMENT '执行人ID',
    `assignee_name` VARCHAR(50) COMMENT '执行人姓名',
    `department_id` BIGINT COMMENT '部门ID',
    `department_name` VARCHAR(100) COMMENT '部门名称',
    `target_ids` JSON COMMENT '目标ID列表',
    `due_date` DATETIME COMMENT '截止时间',
    `accepted_at` DATETIME COMMENT '接收时间',
    `submitted_at` DATETIME COMMENT '提交时间',
    `completed_at` DATETIME COMMENT '完成时间',
    `workflow_template_id` BIGINT COMMENT '流程模板ID',
    `process_instance_id` VARCHAR(64) COMMENT 'Flowable流程实例ID',
    `current_node` VARCHAR(100) COMMENT '当前节点',
    `current_approvers` JSON COMMENT '当前审批人列表',
    `attachment_ids` JSON COMMENT '附件ID列表',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_code` (`task_code`),
    INDEX `idx_assigner_id` (`assigner_id`),
    INDEX `idx_assignee_id` (`assignee_id`),
    INDEX `idx_department_id` (`department_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_due_date` (`due_date`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务表';

-- ---------------------------------------------------
-- 3.3 任务执行人表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `task_assignees`;
CREATE TABLE `task_assignees` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `task_id` BIGINT NOT NULL COMMENT '任务ID',
    `assignee_id` BIGINT NOT NULL COMMENT '执行人ID',
    `assignee_name` VARCHAR(50) COMMENT '执行人姓名',
    `status` TINYINT DEFAULT 0 COMMENT '状态',
    `accepted_at` DATETIME COMMENT '接收时间',
    `submitted_at` DATETIME COMMENT '提交时间',
    `completed_at` DATETIME COMMENT '完成时间',
    `process_instance_id` VARCHAR(64) COMMENT '流程实例ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_assignee` (`task_id`, `assignee_id`),
    INDEX `idx_task_id` (`task_id`),
    INDEX `idx_assignee_id` (`assignee_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务执行人表';

-- ---------------------------------------------------
-- 3.4 任务提交记录表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `task_submissions`;
CREATE TABLE `task_submissions` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `task_id` BIGINT NOT NULL COMMENT '任务ID',
    `task_assignee_id` BIGINT COMMENT '执行人记录ID',
    `submitter_id` BIGINT NOT NULL COMMENT '提交人ID',
    `submitter_name` VARCHAR(50) COMMENT '提交人姓名',
    `content` TEXT COMMENT '完成情况',
    `attachment_ids` JSON COMMENT '附件ID列表',
    `submitted_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    `review_status` TINYINT DEFAULT 0 COMMENT '审核状态:0待审,1通过,2驳回',
    `reviewer_id` BIGINT COMMENT '审核人ID',
    `reviewer_name` VARCHAR(50) COMMENT '审核人姓名',
    `reviewed_at` DATETIME COMMENT '审核时间',
    `review_comment` TEXT COMMENT '审核意见',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_task_id` (`task_id`),
    INDEX `idx_submitter_id` (`submitter_id`),
    INDEX `idx_review_status` (`review_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务提交记录表';

-- ---------------------------------------------------
-- 3.5 任务审批记录表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `task_approval_records`;
CREATE TABLE `task_approval_records` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `task_id` BIGINT NOT NULL COMMENT '任务ID',
    `submission_id` BIGINT COMMENT '提交记录ID',
    `approval_level` INT NOT NULL COMMENT '审批级别',
    `approver_id` BIGINT NOT NULL COMMENT '审批人ID',
    `approver_name` VARCHAR(50) COMMENT '审批人姓名',
    `action` VARCHAR(20) NOT NULL COMMENT '操作:APPROVE,REJECT,FORWARD',
    `comment` TEXT COMMENT '审批意见',
    `approved_at` DATETIME NOT NULL COMMENT '审批时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_task_id` (`task_id`),
    INDEX `idx_submission_id` (`submission_id`),
    INDEX `idx_approver_id` (`approver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务审批记录表';

-- ---------------------------------------------------
-- 3.6 任务审批配置表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `task_approval_configs`;
CREATE TABLE `task_approval_configs` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `task_id` BIGINT NOT NULL COMMENT '任务ID',
    `department_id` BIGINT NOT NULL COMMENT '部门ID',
    `department_name` VARCHAR(100) NOT NULL COMMENT '部门名称',
    `approval_level` TINYINT NOT NULL COMMENT '审批级别',
    `approver_id` BIGINT NOT NULL COMMENT '审批人ID',
    `approver_name` VARCHAR(50) NOT NULL COMMENT '审批人姓名',
    `approver_role` VARCHAR(50) COMMENT '审批人角色',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_dept_level` (`task_id`, `department_id`, `approval_level`),
    INDEX `idx_task_id` (`task_id`),
    INDEX `idx_department_id` (`department_id`),
    INDEX `idx_approver_id` (`approver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务审批配置表';

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================
-- 业务表结构 - 完成
-- =====================================================
