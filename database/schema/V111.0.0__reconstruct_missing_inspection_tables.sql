-- ============================================================
-- Migration: V111.0.0__reconstruct_missing_inspection_tables.sql
-- Description: 重建 6 张缺失 DDL 的检查平台表
--
-- 背景: 这 6 张表的线上库由从未提交的临时 SQL 建出, 而该线上库已丢失,
--        仓库中没有任何 CREATE TABLE 定义。它们仅以 MyBatis-Plus PO Java 类
--        的形式存在。本文件依据 PO 类 (唯一真相来源) + 对应 Mapper 的
--        @Select/@Update 列名 + 既有 insp_* 表 DDL 风格 反推重建。
--
-- 来源 PO:
--   insp_audit_trail       <- AuditTrailPO.java
--   insp_holiday_calendars <- HolidayCalendarPO.java
--   insp_issue_categories  <- IssueCategoryPO.java
--   insp_policy_calc_rules <- PolicyCalcRulePO.java
--   insp_policy_grade_bands<- PolicyGradeBandPO.java
--   insp_scoring_policies  <- ScoringPolicyPO.java
--
-- 约定:
--   - PK 全部 @TableId(type=IdType.AUTO) -> BIGINT AUTO_INCREMENT
--   - 列名 = PO 字段 snake_case (mapUnderscoreToCamelCase=true)
--   - @TableLogic deleted -> TINYINT NOT NULL DEFAULT 0
--   - 引擎/字符集与既有 insp_* 表保持一致
--   - 这些是检查平台表, 受 @DataPermission 横切过滤, 故统一补 org_unit_id
--     (PO 未声明, 见 inspection orgUnitId 横切关注点架构)
-- ============================================================

SET NAMES utf8mb4;

-- -----------------------------------------------------------
-- 1. insp_scoring_policies (评分方案) <- ScoringPolicyPO
--    被 insp_policy_calc_rules / insp_policy_grade_bands 通过 policy_id 引用, 先建
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `insp_scoring_policies` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`        BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    `policy_code`      VARCHAR(50)  NOT NULL COMMENT '方案编码',
    `policy_name`      VARCHAR(200) NOT NULL COMMENT '方案名称',
    `description`      VARCHAR(500) NULL     COMMENT '方案描述',
    `precision_digits` INT          NOT NULL DEFAULT 2 COMMENT '小数精度位数',
    `is_system`        TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否系统内置',
    `is_enabled`       TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用',
    `sort_order`       INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `created_by`       BIGINT       NULL,
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_by`       BIGINT       NULL,
    `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `org_unit_id`      BIGINT       NULL     COMMENT '数据权限归属组织',
    `deleted`          TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_policy_code` (`tenant_id`, `policy_code`, `deleted`),
    INDEX `idx_scoring_policy_enabled` (`is_enabled`, `sort_order`),
    INDEX `idx_scoring_policy_org` (`org_unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='检查平台评分方案';

-- -----------------------------------------------------------
-- 2. insp_policy_calc_rules (评分方案-计算规则) <- PolicyCalcRulePO
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `insp_policy_calc_rules` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `policy_id`   BIGINT       NOT NULL COMMENT '所属评分方案ID',
    `rule_code`   VARCHAR(50)  NOT NULL COMMENT '规则编码',
    `rule_name`   VARCHAR(200) NOT NULL COMMENT '规则名称',
    `rule_type`   VARCHAR(50)  NOT NULL COMMENT '规则类型',
    `priority`    INT          NOT NULL DEFAULT 0 COMMENT '优先级(升序执行)',
    `config`      JSON         NULL     COMMENT '规则配置 JSON',
    `is_enabled`  TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `org_unit_id` BIGINT       NULL     COMMENT '数据权限归属组织',
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_calc_rule_policy` (`policy_id`, `priority`),
    INDEX `idx_calc_rule_org` (`org_unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='检查平台评分方案-计算规则';

-- -----------------------------------------------------------
-- 3. insp_policy_grade_bands (评分方案-等级映射) <- PolicyGradeBandPO
--    min_percent/max_percent 为百分比语义 (V65.1.0 由 min_score/max_score 改名而来)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `insp_policy_grade_bands` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT,
    `policy_id`   BIGINT        NOT NULL COMMENT '所属评分方案ID',
    `grade_code`  VARCHAR(50)   NOT NULL COMMENT '等级编码',
    `grade_name`  VARCHAR(100)  NOT NULL COMMENT '等级名称',
    `min_percent` DECIMAL(5,2)  NULL     COMMENT '区间下限(百分比, 90=90%)',
    `max_percent` DECIMAL(5,2)  NULL     COMMENT '区间上限(百分比, 90=90%)',
    `sort_order`  INT           NOT NULL DEFAULT 0 COMMENT '排序',
    `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `org_unit_id` BIGINT        NULL     COMMENT '数据权限归属组织',
    `deleted`     TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_grade_band_policy` (`policy_id`, `sort_order`),
    INDEX `idx_grade_band_org` (`org_unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='检查平台评分方案-等级映射';

-- -----------------------------------------------------------
-- 4. insp_issue_categories (问题分类 - 树形) <- IssueCategoryPO
--    注意: 列名以 PO 为准 (category_code/category_name/is_enabled),
--    与已废弃的 V40 migration (name/code/is_active) 不同, 此处采用 PO 定义
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `insp_issue_categories` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`     BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    `parent_id`     BIGINT       NULL     COMMENT '父分类ID, NULL=根分类',
    `category_code` VARCHAR(50)  NULL     COMMENT '分类编码',
    `category_name` VARCHAR(200) NOT NULL COMMENT '分类名称',
    `description`   VARCHAR(500) NULL     COMMENT '分类描述',
    `icon`          VARCHAR(50)  NULL     COMMENT '图标',
    `sort_order`    INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `is_enabled`    TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用',
    `created_by`    BIGINT       NULL,
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_by`    BIGINT       NULL,
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `org_unit_id`   BIGINT       NULL     COMMENT '数据权限归属组织',
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_issue_cat_parent` (`parent_id`, `sort_order`),
    INDEX `idx_issue_cat_code` (`category_code`),
    INDEX `idx_issue_cat_org` (`org_unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='检查平台问题分类';

-- -----------------------------------------------------------
-- 5. insp_holiday_calendars (假日日历) <- HolidayCalendarPO
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `insp_holiday_calendars` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`     BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    `calendar_name` VARCHAR(200) NOT NULL COMMENT '日历名称',
    `year`          INT          NOT NULL COMMENT '年份',
    `holidays`      TEXT         NULL     COMMENT '节假日 JSON 日期数组',
    `workdays`      TEXT         NULL     COMMENT '调休补班 JSON 日期数组',
    `is_default`    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否默认日历',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `org_unit_id`   BIGINT       NULL     COMMENT '数据权限归属组织',
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_holiday_year` (`year`),
    INDEX `idx_holiday_default` (`is_default`),
    INDEX `idx_holiday_org` (`org_unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='检查平台假日日历';

-- -----------------------------------------------------------
-- 6. insp_audit_trail (审计日志) <- AuditTrailPO
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `insp_audit_trail` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`     BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    `user_id`       BIGINT       NULL     COMMENT '操作用户ID',
    `user_name`     VARCHAR(100) NULL     COMMENT '操作用户名',
    `action`        VARCHAR(100) NOT NULL COMMENT '操作动作',
    `resource_type` VARCHAR(50)  NOT NULL COMMENT '资源类型',
    `resource_id`   BIGINT       NULL     COMMENT '资源ID',
    `resource_name` VARCHAR(200) NULL     COMMENT '资源名称',
    `details`       TEXT         NULL     COMMENT 'JSON 前后快照/明细',
    `ip_address`    VARCHAR(50)  NULL     COMMENT '客户端IP',
    `occurred_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
    `org_unit_id`   BIGINT       NULL     COMMENT '数据权限归属组织',
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_audit_resource` (`resource_type`, `resource_id`),
    INDEX `idx_audit_user` (`user_id`),
    INDEX `idx_audit_time` (`occurred_at`),
    INDEX `idx_audit_org` (`org_unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='检查平台审计日志';
