-- ==========================================
-- 评级模块彻底重构：删除旧表，创建新表
-- 版本: 4.4.0
-- 作者: System
-- 日期: 2025-12-23
-- 说明: 完全重构评级系统，支持灵活的排名方式和组合评级
-- ==========================================

SET NAMES utf8mb4;
SET CHARACTER_SET_CLIENT = utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ==========================================
-- 第一部分：删除所有旧的评级相关表
-- ==========================================

DROP TABLE IF EXISTS rating_honor_badge;
DROP TABLE IF EXISTS class_badge_record;
DROP TABLE IF EXISTS rating_notification_template;
DROP TABLE IF EXISTS rating_notification_history;
DROP TABLE IF EXISTS check_plan_rating_rule;
DROP TABLE IF EXISTS check_plan_rating_level;
DROP TABLE IF EXISTS check_plan_rating_result;
DROP TABLE IF EXISTS rating_frequency_statistics;

-- 删除旧的评级相关权限
DELETE FROM role_permissions WHERE permission_id IN (
    SELECT id FROM permissions WHERE permission_code LIKE 'quantification:rating%'
    OR permission_code LIKE 'quantification:badge%'
    OR permission_code LIKE 'quantification:notification%'
);

DELETE FROM permissions WHERE permission_code LIKE 'quantification:rating%'
    OR permission_code LIKE 'quantification:badge%'
    OR permission_code LIKE 'quantification:notification%';

SELECT '✅ 旧评级表和权限已删除' AS message;

-- ==========================================
-- 第二部分：创建新的评级表结构
-- ==========================================

USE student_management;

-- 1. 检查计划周期配置表
CREATE TABLE check_plan_period_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  check_plan_id BIGINT NOT NULL COMMENT '检查计划ID',
  week_start_day TINYINT DEFAULT 1 COMMENT '周起始日：1-7（1=周一，7=周日）',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_check_plan (check_plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查计划周期配置表';

-- 2. 评级配置表
CREATE TABLE rating_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  check_plan_id BIGINT NOT NULL COMMENT '检查计划ID',
  rating_name VARCHAR(50) NOT NULL COMMENT '评级名称（如：优秀班级、卫生班级）',
  rating_type VARCHAR(20) NOT NULL COMMENT '评级周期类型：DAILY/WEEKLY/MONTHLY',

  -- 显示设置
  icon VARCHAR(50) COMMENT '图标',
  color VARCHAR(20) COMMENT '颜色（十六进制）',
  priority INT DEFAULT 999 COMMENT '显示优先级（数字越小越靠前）',

  -- 划分规则
  division_method VARCHAR(30) NOT NULL COMMENT '划分方式：TOP_N（前N名）/TOP_PERCENT（前X%）/BOTTOM_N（后N名）/BOTTOM_PERCENT（后X%）',
  division_value DECIMAL(10,2) NOT NULL COMMENT '划分值（3名或10%）',

  -- 审核发布
  require_approval TINYINT DEFAULT 1 COMMENT '是否需要审核：0否 1是',
  auto_publish TINYINT DEFAULT 0 COMMENT '审核通过后自动发布：0否 1是',

  -- 状态
  enabled TINYINT DEFAULT 1 COMMENT '是否启用：0否 1是',
  sort_order INT DEFAULT 0 COMMENT '排序序号',
  description TEXT COMMENT '规则说明',

  -- 审计字段
  created_by BIGINT COMMENT '创建人ID',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',

  INDEX idx_check_plan (check_plan_id, deleted),
  INDEX idx_rating_type (rating_type, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级配置表';

-- 3. 评级排名数据源配置表（支持组合排名）
CREATE TABLE rating_ranking_source (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  rating_config_id BIGINT NOT NULL COMMENT '评级配置ID',

  -- 数据来源
  source_type VARCHAR(30) NOT NULL COMMENT '数据源类型：TOTAL_SCORE（总分）/CATEGORY（类别）/DEDUCTION_ITEM（扣分项）',
  source_id BIGINT COMMENT '来源ID（类别ID或扣分项ID，TOTAL_SCORE时为NULL）',
  use_weighted TINYINT DEFAULT 1 COMMENT '是否使用加权分：0否 1是',

  -- 组合排名权重
  weight DECIMAL(5,4) DEFAULT 1.0000 COMMENT '权重（组合排名时使用，总和必须为1）',

  -- 缺失数据处理策略
  missing_data_strategy VARCHAR(20) DEFAULT 'ZERO' COMMENT '缺失数据策略：ZERO（按0分计）/SKIP（跳过该班级）',

  sort_order INT DEFAULT 0 COMMENT '排序序号',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  INDEX idx_rating_config (rating_config_id),
  UNIQUE KEY uk_rating_source (rating_config_id, source_type, source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级排名数据源配置表';

-- 4. 每日班级汇总表（性能优化：避免每次从检查记录聚合）
CREATE TABLE daily_class_summary (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  check_plan_id BIGINT NOT NULL COMMENT '检查计划ID',
  class_id BIGINT NOT NULL COMMENT '班级ID',
  check_date DATE NOT NULL COMMENT '检查日期',

  -- 汇总分数
  total_score DECIMAL(10,2) DEFAULT 0 COMMENT '当天总扣分',
  weighted_total_score DECIMAL(10,2) DEFAULT 0 COMMENT '当天加权总扣分',

  -- 按类别的分数（JSON格式：{"categoryId": {"score": xx, "weightedScore": xx}}）
  category_scores JSON COMMENT '各类别扣分',

  -- 按扣分项的分数（JSON格式：{"itemId": {"score": xx, "weightedScore": xx}}）
  item_scores JSON COMMENT '各扣分项扣分',

  -- 统计信息
  check_count INT DEFAULT 0 COMMENT '当天检查次数',
  participated TINYINT DEFAULT 1 COMMENT '是否参与检查：0否 1是',
  is_finalized TINYINT DEFAULT 0 COMMENT '是否已确认（申诉期结束）：0否 1是',

  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  UNIQUE KEY uk_plan_class_date (check_plan_id, class_id, check_date),
  INDEX idx_check_date (check_date),
  INDEX idx_finalized (is_finalized)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日班级汇总表';

-- 5. 评级结果表
CREATE TABLE rating_result (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  rating_config_id BIGINT NOT NULL COMMENT '评级配置ID',
  check_plan_id BIGINT NOT NULL COMMENT '检查计划ID',
  class_id BIGINT NOT NULL COMMENT '班级ID',

  -- 评级周期
  period_type VARCHAR(20) NOT NULL COMMENT '周期类型：DAILY/WEEKLY/MONTHLY',
  period_value VARCHAR(30) NOT NULL COMMENT '周期值：2025-12-22（日）/2025-W51（周）/2025-12（月）',
  period_start DATE NOT NULL COMMENT '周期开始日期',
  period_end DATE NOT NULL COMMENT '周期结束日期',

  -- 评级结果
  final_score DECIMAL(10,2) NOT NULL COMMENT '最终得分（扣分）',
  ranking INT NOT NULL COMMENT '排名',
  total_classes INT NOT NULL COMMENT '参与评级的总班级数',

  -- 状态管理
  result_status VARCHAR(20) DEFAULT 'DRAFT' COMMENT '结果状态：DRAFT（草稿）/PENDING_APPROVAL（待审核）/APPROVED（已审核）/PUBLISHED（已发布）/ARCHIVED（已归档）',

  -- 审核信息
  approved_by BIGINT COMMENT '审核人ID',
  approved_at DATETIME COMMENT '审核时间',
  approval_remark VARCHAR(500) COMMENT '审核备注',

  -- 发布信息
  published_by BIGINT COMMENT '发布人ID',
  published_at DATETIME COMMENT '发布时间',

  -- 版本控制
  version INT DEFAULT 1 COMMENT '版本号（每次重新计算递增）',

  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',

  INDEX idx_config_period (rating_config_id, period_value),
  INDEX idx_class_period (class_id, period_type, period_value),
  INDEX idx_status (result_status),
  UNIQUE KEY uk_config_class_period (rating_config_id, class_id, period_value, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级结果表';

-- 6. 评级计算明细表（追溯评级计算来源）
CREATE TABLE rating_calculation_detail (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  rating_result_id BIGINT NOT NULL COMMENT '评级结果ID',

  -- 计算来源
  source_type VARCHAR(30) NOT NULL COMMENT '来源类型：CHECK_RECORD（检查记录）/DAILY_SUMMARY（每日汇总）',
  source_id BIGINT NOT NULL COMMENT '来源ID（检查记录ID或每日汇总ID）',
  check_date DATE COMMENT '检查日期',

  -- 分数贡献
  score_contribution DECIMAL(10,2) COMMENT '对最终分数的贡献',
  weight_used DECIMAL(5,4) COMMENT '使用的权重',

  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  INDEX idx_rating_result (rating_result_id),
  INDEX idx_source (source_type, source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级计算明细表';

-- 7. 评级变更日志表
CREATE TABLE rating_change_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  rating_result_id BIGINT NOT NULL COMMENT '评级结果ID',
  class_id BIGINT NOT NULL COMMENT '班级ID',

  -- 变更类型
  change_type VARCHAR(30) NOT NULL COMMENT '变更类型：SCORE_CHANGE（分数变更）/RANKING_CHANGE（排名变更）/STATUS_CHANGE（状态变更）/CREATED（新建）/REVOKED（撤销）',

  -- 变更内容
  old_score DECIMAL(10,2) COMMENT '原分数',
  new_score DECIMAL(10,2) COMMENT '新分数',
  old_ranking INT COMMENT '原排名',
  new_ranking INT COMMENT '新排名',
  old_status VARCHAR(20) COMMENT '原状态',
  new_status VARCHAR(20) COMMENT '新状态',

  -- 变更原因
  change_reason VARCHAR(50) NOT NULL COMMENT '变更原因：APPEAL（申诉）/RECALCULATE（重新计算）/MANUAL_ADJUST（手动调整）',
  related_appeal_id BIGINT COMMENT '关联的申诉ID',
  remark TEXT COMMENT '备注',

  changed_by BIGINT COMMENT '操作人ID',
  changed_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',

  INDEX idx_rating_result (rating_result_id),
  INDEX idx_change_type (change_type),
  INDEX idx_changed_at (changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级变更日志表';

-- 8. 评级统计汇总表（性能优化：预计算统计数据）
CREATE TABLE rating_statistics (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  check_plan_id BIGINT NOT NULL COMMENT '检查计划ID',
  class_id BIGINT NOT NULL COMMENT '班级ID',
  rating_config_id BIGINT NOT NULL COMMENT '评级配置ID',

  -- 统计周期（可选，NULL表示全部）
  period_type VARCHAR(20) COMMENT '统计周期类型：DAILY/WEEKLY/MONTHLY/ALL',
  period_value VARCHAR(30) COMMENT '周期值',

  -- 统计数据
  rating_count INT DEFAULT 0 COMMENT '获得该评级的次数',
  first_rating_date DATE COMMENT '首次获得日期',
  last_rating_date DATE COMMENT '最近获得日期',

  -- 排名统计
  avg_ranking DECIMAL(10,2) COMMENT '平均排名',
  best_ranking INT COMMENT '最佳排名',

  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  UNIQUE KEY uk_stats (check_plan_id, class_id, rating_config_id, period_type, period_value),
  INDEX idx_class (class_id),
  INDEX idx_rating_config (rating_config_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级统计汇总表';

-- 9. 评级配置版本表（支持规则变更追溯）
CREATE TABLE rating_config_version (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  rating_config_id BIGINT NOT NULL COMMENT '评级配置ID',
  version_number INT NOT NULL COMMENT '版本号',

  -- 生效时间
  effective_from DATETIME NOT NULL COMMENT '生效开始时间',
  effective_to DATETIME COMMENT '生效结束时间（NULL表示当前版本）',

  -- 配置快照（完整的配置JSON，包含所有数据源配置）
  config_snapshot JSON NOT NULL COMMENT '配置快照',

  change_reason VARCHAR(200) COMMENT '变更原因',
  created_by BIGINT COMMENT '创建人ID',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  INDEX idx_config (rating_config_id, version_number),
  INDEX idx_effective (effective_from, effective_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级配置版本表';

-- ==========================================
-- 第三部分：初始化数据
-- ==========================================

-- 为所有现有检查计划创建默认周期配置（周一开始）
INSERT INTO check_plan_period_config (check_plan_id, week_start_day)
SELECT id, 1 FROM check_plans WHERE deleted = 0;

-- ==========================================
-- 第四部分：配置新权限
-- ==========================================

-- 获取quantification模块ID
SET @quantification_module_id = (SELECT id FROM permissions WHERE permission_code = 'quantification' AND deleted = 0 LIMIT 1);

-- 创建评级管理父级权限
INSERT INTO permissions (permission_code, permission_name, parent_id, resource_type, permission_desc, sort_order, status, created_at, updated_at, deleted)
VALUES ('quantification:rating', '评级管理', @quantification_module_id, 1, '检查计划评级配置和结果管理', 10, 1, NOW(), NOW(), 0);

SET @rating_parent_id = (SELECT id FROM permissions WHERE permission_code = 'quantification:rating' AND deleted = 0 LIMIT 1);

-- 评级配置和操作权限
INSERT INTO permissions (permission_code, permission_name, parent_id, resource_type, permission_desc, sort_order, status, created_at, updated_at, deleted) VALUES
('quantification:rating:config:view', '查看评级配置', @rating_parent_id, 2, '查看评级配置列表', 1, 1, NOW(), NOW(), 0),
('quantification:rating:config:create', '创建评级配置', @rating_parent_id, 2, '创建新的评级配置', 2, 1, NOW(), NOW(), 0),
('quantification:rating:config:update', '修改评级配置', @rating_parent_id, 2, '修改评级配置', 3, 1, NOW(), NOW(), 0),
('quantification:rating:config:delete', '删除评级配置', @rating_parent_id, 2, '删除评级配置', 4, 1, NOW(), NOW(), 0),
('quantification:rating:calculate', '计算评级', @rating_parent_id, 2, '手动触发评级计算', 5, 1, NOW(), NOW(), 0),
('quantification:rating:view', '查看评级结果', @rating_parent_id, 2, '查看评级结果和统计', 6, 1, NOW(), NOW(), 0),
('quantification:rating:approve', '审核评级结果', @rating_parent_id, 2, '审核评级结果', 7, 1, NOW(), NOW(), 0),
('quantification:rating:publish', '发布评级结果', @rating_parent_id, 2, '发布评级结果', 8, 1, NOW(), NOW(), 0),
('quantification:rating:export', '导出评级通报', @rating_parent_id, 2, '导出评级通报和证书', 9, 1, NOW(), NOW(), 0);

-- 为管理员角色授予所有权限
SET @admin_role_id = (SELECT id FROM roles WHERE role_name IN ('ADMIN', '管理员', 'admin') AND deleted = 0 LIMIT 1);

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT @admin_role_id, id, NOW()
FROM permissions
WHERE permission_code LIKE 'quantification:rating%'
AND deleted = 0
AND @admin_role_id IS NOT NULL
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = @admin_role_id
    AND rp.permission_id = permissions.id
);

SET FOREIGN_KEY_CHECKS = 1;

-- ==========================================
-- 完成提示
-- ==========================================
SELECT '🎉 评级模块重构完成！' AS message;

SELECT CONCAT('新建表数量: ', COUNT(*)) AS result FROM information_schema.tables
WHERE table_schema = 'student_management'
AND table_name IN ('rating_config', 'rating_ranking_source', 'daily_class_summary',
                   'rating_result', 'rating_calculation_detail', 'rating_change_log',
                   'rating_statistics', 'rating_config_version', 'check_plan_period_config');

SELECT CONCAT('新增权限数量: ', COUNT(*)) AS result FROM permissions WHERE permission_code LIKE 'quantification:rating%';

SELECT CONCAT('已初始化检查计划: ', COUNT(*)) AS result FROM check_plan_period_config;
