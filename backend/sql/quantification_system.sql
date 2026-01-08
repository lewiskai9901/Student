-- ==========================================
-- 量化检查系统数据库设计
-- ==========================================

-- 1. 检查模板表
CREATE TABLE IF NOT EXISTS `check_templates` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `template_name` VARCHAR(100) NOT NULL COMMENT '模板名称',
  `template_code` VARCHAR(50) NOT NULL COMMENT '模板编码',
  `description` VARCHAR(500) COMMENT '模板描述',
  `is_default` TINYINT DEFAULT 0 COMMENT '是否默认模板 0否 1是',
  `status` TINYINT DEFAULT 1 COMMENT '状态 0禁用 1启用',
  `created_by` BIGINT COMMENT '创建人ID',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_code` (`template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检查模板表';

-- 2. 模板检查类别关联表
CREATE TABLE IF NOT EXISTS `template_categories` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `template_id` BIGINT NOT NULL COMMENT '模板ID',
  `category_id` BIGINT NOT NULL COMMENT '检查类别ID(check_categories表)',
  `link_type` TINYINT DEFAULT 0 COMMENT '关联类型 0不关联 1关联宿舍 2关联教室',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `is_required` TINYINT DEFAULT 1 COMMENT '是否必检 0否 1是',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_template_id` (`template_id`),
  KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板检查类别关联表';

-- 3. 每日检查表
CREATE TABLE IF NOT EXISTS `daily_checks` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `check_code` VARCHAR(50) NOT NULL COMMENT '检查编号',
  `check_name` VARCHAR(100) NOT NULL COMMENT '检查名称',
  `check_date` DATE NOT NULL COMMENT '检查日期',
  `template_id` BIGINT COMMENT '使用的模板ID',
  `target_type` TINYINT NOT NULL COMMENT '检查目标类型 1班级 2年级 3系部',
  `checker_id` BIGINT NOT NULL COMMENT '检查人ID',
  `checker_name` VARCHAR(50) COMMENT '检查人姓名',
  `status` TINYINT DEFAULT 0 COMMENT '检查状态 0待检查 1检查中 2已完成 3已发布',
  `publish_time` DATETIME COMMENT '发布时间',
  `description` VARCHAR(500) COMMENT '检查说明',
  `created_by` BIGINT COMMENT '创建人ID',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_check_code` (`check_code`),
  KEY `idx_check_date` (`check_date`),
  KEY `idx_template_id` (`template_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日检查表';

-- 4. 每日检查目标表 (记录检查哪些班级/年级)
CREATE TABLE IF NOT EXISTS `daily_check_targets` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `check_id` BIGINT NOT NULL COMMENT '检查ID',
  `target_type` TINYINT NOT NULL COMMENT '目标类型 1班级 2年级 3系部',
  `target_id` BIGINT NOT NULL COMMENT '目标ID(班级ID/年级ID/系部ID)',
  `target_name` VARCHAR(100) COMMENT '目标名称',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_check_id` (`check_id`),
  KEY `idx_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日检查目标表';

-- 5. 每日检查类别表
CREATE TABLE IF NOT EXISTS `daily_check_categories` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `check_id` BIGINT NOT NULL COMMENT '检查ID',
  `category_id` BIGINT NOT NULL COMMENT '检查类别ID',
  `category_name` VARCHAR(100) COMMENT '类别名称',
  `link_type` TINYINT DEFAULT 0 COMMENT '关联类型 0不关联 1关联宿舍 2关联教室',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_check_id` (`check_id`),
  KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日检查类别表';

-- 6. 检查记录表 (主表:一个班级在一次检查中的一个类别的记录)
CREATE TABLE IF NOT EXISTS `check_records` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `check_id` BIGINT NOT NULL COMMENT '每日检查ID',
  `category_id` BIGINT NOT NULL COMMENT '检查类别ID',
  `class_id` BIGINT NOT NULL COMMENT '班级ID',
  `class_name` VARCHAR(100) COMMENT '班级名称',
  `total_score` DECIMAL(10,2) DEFAULT 0 COMMENT '总扣分',
  `record_count` INT DEFAULT 0 COMMENT '扣分记录数',
  `appeal_status` TINYINT DEFAULT 0 COMMENT '申诉状态 0无申诉 1申诉中 2申诉通过 3申诉驳回',
  `original_score` DECIMAL(10,2) COMMENT '原始扣分(申诉前)',
  `revised_score` DECIMAL(10,2) COMMENT '修订后扣分',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_check_id` (`check_id`),
  KEY `idx_class_id` (`class_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_appeal_status` (`appeal_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检查记录表';

-- 7. 检查记录明细表 (扣分明细,包含宿舍/教室关联)
CREATE TABLE IF NOT EXISTS `check_record_details` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `record_id` BIGINT NOT NULL COMMENT '检查记录ID',
  `check_id` BIGINT NOT NULL COMMENT '每日检查ID',
  `category_id` BIGINT NOT NULL COMMENT '检查类别ID',
  `deduction_item_id` BIGINT NOT NULL COMMENT '扣分项ID',
  `deduction_item_name` VARCHAR(100) COMMENT '扣分项名称',
  `deduct_mode` TINYINT COMMENT '扣分模式 1固定 2按人数 3区间',
  `class_id` BIGINT NOT NULL COMMENT '班级ID',
  `class_name` VARCHAR(100) COMMENT '班级名称',
  `link_type` TINYINT DEFAULT 0 COMMENT '关联类型 0不关联 1宿舍 2教室',
  `dormitory_id` BIGINT COMMENT '宿舍ID',
  `dormitory_no` VARCHAR(50) COMMENT '宿舍号',
  `classroom_id` BIGINT COMMENT '教室ID',
  `classroom_no` VARCHAR(50) COMMENT '教室号',
  `deduct_score` DECIMAL(10,2) NOT NULL COMMENT '扣分分数',
  `person_count` INT COMMENT '人数(按人数扣分时)',
  `total_students` INT COMMENT '宿舍总人数(混寝计算用)',
  `class_students` INT COMMENT '本班级人数(混寝计算用)',
  `score_ratio` DECIMAL(5,4) COMMENT '分数比例(混寝)',
  `description` VARCHAR(500) COMMENT '备注说明',
  `checker_id` BIGINT COMMENT '检查人ID',
  `checker_name` VARCHAR(50) COMMENT '检查人姓名',
  `check_time` DATETIME COMMENT '检查时间',
  `images` TEXT COMMENT '图片URL列表(JSON)',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_record_id` (`record_id`),
  KEY `idx_check_id` (`check_id`),
  KEY `idx_class_id` (`class_id`),
  KEY `idx_dormitory_id` (`dormitory_id`),
  KEY `idx_classroom_id` (`classroom_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检查记录明细表';

-- 8. 申诉记录表
CREATE TABLE IF NOT EXISTS `appeal_records` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `record_id` BIGINT NOT NULL COMMENT '检查记录ID',
  `detail_id` BIGINT COMMENT '明细ID(可为空,整体申诉)',
  `check_id` BIGINT NOT NULL COMMENT '每日检查ID',
  `class_id` BIGINT NOT NULL COMMENT '班级ID',
  `appeal_reason` VARCHAR(1000) NOT NULL COMMENT '申诉理由',
  `appeal_images` TEXT COMMENT '申诉图片(JSON)',
  `appellant_id` BIGINT NOT NULL COMMENT '申诉人ID',
  `appellant_name` VARCHAR(50) COMMENT '申诉人姓名',
  `appeal_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申诉时间',
  `status` TINYINT DEFAULT 0 COMMENT '状态 0待审核 1通过 2驳回',
  `reviewer_id` BIGINT COMMENT '审核人ID',
  `reviewer_name` VARCHAR(50) COMMENT '审核人姓名',
  `review_time` DATETIME COMMENT '审核时间',
  `review_result` VARCHAR(1000) COMMENT '审核意见',
  `original_score` DECIMAL(10,2) COMMENT '原始分数',
  `revised_score` DECIMAL(10,2) COMMENT '修订后分数',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_record_id` (`record_id`),
  KEY `idx_check_id` (`check_id`),
  KEY `idx_class_id` (`class_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='申诉记录表';

-- 9. 修订记录表 (记录所有分数修改历史)
CREATE TABLE IF NOT EXISTS `revision_records` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `record_id` BIGINT NOT NULL COMMENT '检查记录ID',
  `detail_id` BIGINT COMMENT '明细ID',
  `appeal_id` BIGINT COMMENT '申诉ID',
  `revision_type` TINYINT NOT NULL COMMENT '修订类型 1申诉修订 2管理员修订',
  `original_score` DECIMAL(10,2) NOT NULL COMMENT '原始分数',
  `revised_score` DECIMAL(10,2) NOT NULL COMMENT '修订后分数',
  `revision_reason` VARCHAR(1000) COMMENT '修订原因',
  `reviser_id` BIGINT NOT NULL COMMENT '修订人ID',
  `reviser_name` VARCHAR(50) COMMENT '修订人姓名',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '修订时间',
  PRIMARY KEY (`id`),
  KEY `idx_record_id` (`record_id`),
  KEY `idx_detail_id` (`detail_id`),
  KEY `idx_appeal_id` (`appeal_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='修订记录表';

-- 10. 班级量化汇总表 (用于快速查询排名)
CREATE TABLE IF NOT EXISTS `class_quantification_summary` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `class_id` BIGINT NOT NULL COMMENT '班级ID',
  `summary_date` DATE NOT NULL COMMENT '统计日期',
  `summary_type` TINYINT NOT NULL COMMENT '统计类型 1日 2周 3月 4学期 5学年',
  `total_score` DECIMAL(10,2) DEFAULT 0 COMMENT '总扣分',
  `check_count` INT DEFAULT 0 COMMENT '检查次数',
  `appeal_count` INT DEFAULT 0 COMMENT '申诉次数',
  `revision_count` INT DEFAULT 0 COMMENT '修订次数',
  `rank_in_grade` INT COMMENT '年级排名',
  `rank_in_dept` INT COMMENT '系部排名',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_class_date_type` (`class_id`, `summary_date`, `summary_type`),
  KEY `idx_summary_date` (`summary_date`),
  KEY `idx_summary_type` (`summary_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级量化汇总表';

-- 11. 修改检查类别表(quantification_types),添加关联类型字段
ALTER TABLE `quantification_types`
ADD COLUMN `link_type` TINYINT DEFAULT 0 COMMENT '关联类型 0不关联 1可关联宿舍 2可关联教室 3可关联宿舍和教室' AFTER `type_name`;
