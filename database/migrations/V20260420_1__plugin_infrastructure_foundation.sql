-- ============================================================
-- Phase -1.1  Plugin 基础设施冷启动 DDL
-- 日期: 2026-04-20
-- 目的: 补齐所有 Registrar 启动时假设存在但未进迁移的表 / 列
--   让"DROP DATABASE + 重新 import schema + 跑这个迁移"能冷启动成功
--
-- 涵盖:
--   A. CREATE TABLE IF NOT EXISTS plugin_packages     (PluginPackageRegistrar 写入)
--   B. CREATE TABLE IF NOT EXISTS data_scope_dims     (DataScopeRegistrar 写入)
--   C. 为 event_triggers 加 industry + plugin_class 列
--   D. 为 relation_types 加 plugin_class 列
--   (其他 5 张表已经有这两列, 保留现状)
--
-- 幂等性: 所有语句 IF NOT EXISTS, 可安全重复执行
-- ============================================================

-- ─────────────────────────────────────────────────────────
-- A. plugin_packages — 行业包元信息注册表
-- ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `plugin_packages` (
  `industry_code`    VARCHAR(20)  NOT NULL COMMENT '行业包代码,如 CORE/EDU/HEALTH',
  `industry_name`    VARCHAR(100) NOT NULL COMMENT '中文名,如 通用核心/教育行业',
  `version`          VARCHAR(20)  NOT NULL DEFAULT '1.0.0' COMMENT 'SemVer 版本号',
  `depends_on`       JSON         NULL     COMMENT '依赖行业包数组 [\"CORE\"]',
  `manifest_class`   VARCHAR(200) NOT NULL COMMENT 'Manifest 实现类全限定名',
  `enabled`          TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用(1=启用/0=禁用)',
  `uninstall_policy` VARCHAR(20)  NOT NULL DEFAULT 'SOFT' COMMENT 'SOFT=软删/HARD=硬删',
  `installed_at`    DATETIME      NULL     COMMENT '首次注册时间',
  `last_started_at` DATETIME      NULL     COMMENT '最近一次启动时间',
  PRIMARY KEY (`industry_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行业插件包元信息注册表';

-- ─────────────────────────────────────────────────────────
-- B. data_scope_dims — 数据范围维度字典
-- ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `data_scope_dims` (
  `dim_code`      VARCHAR(30)  NOT NULL COMMENT '维度代码, 如 BY_MAJOR/BY_GRADE',
  `dim_name`      VARCHAR(100) NOT NULL COMMENT '中文名',
  `description`   VARCHAR(300) NULL     COMMENT '说明',
  `domain_code`   VARCHAR(30)  NULL     COMMENT '所属业务域',
  `resolver_type` VARCHAR(200) NULL     COMMENT '范围解析器类全限定名',
  `industry`      VARCHAR(20)  NULL     COMMENT '所属行业包',
  `plugin_class`  VARCHAR(200) NULL     COMMENT '声明插件全限定类名',
  `is_enabled`    TINYINT      NULL DEFAULT 1,
  `created_at`    DATETIME     NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`dim_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据权限维度字典';

-- ─────────────────────────────────────────────────────────
-- C+D. event_triggers + relation_types 补缺失的列
-- MySQL 8 没有 ADD COLUMN IF NOT EXISTS, 用 information_schema 做幂等守卫
-- ─────────────────────────────────────────────────────────
DROP PROCEDURE IF EXISTS plugin_infra_add_col;
DELIMITER //
CREATE PROCEDURE plugin_infra_add_col(
    IN p_table   VARCHAR(64),
    IN p_column  VARCHAR(64),
    IN p_ddl     TEXT
)
BEGIN
    DECLARE col_exists INT DEFAULT 0;
    SELECT COUNT(*) INTO col_exists
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name   = p_table
       AND column_name  = p_column;
    IF col_exists = 0 THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN ', p_ddl);
        PREPARE s FROM @sql;
        EXECUTE s;
        DEALLOCATE PREPARE s;
    END IF;
END //
DELIMITER ;

CALL plugin_infra_add_col('event_triggers', 'industry',
    '`industry` VARCHAR(20) NULL COMMENT ''所属行业包'' AFTER `deleted`');
CALL plugin_infra_add_col('event_triggers', 'plugin_class',
    '`plugin_class` VARCHAR(200) NULL COMMENT ''声明插件全限定类名'' AFTER `industry`');
CALL plugin_infra_add_col('relation_types', 'plugin_class',
    '`plugin_class` VARCHAR(200) NULL COMMENT ''声明插件全限定类名'' AFTER `industry`');

DROP PROCEDURE plugin_infra_add_col;

-- ─────────────────────────────────────────────────────────
-- E. 验证 — 7 张贡献表都该有 industry + plugin_class
-- ─────────────────────────────────────────────────────────
-- 执行以下 SELECT 应返回 14 行 (7 表 × 2 列), 若少于 14 行则本迁移未完全生效
SELECT table_name, column_name
  FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name IN ('permissions','roles','entity_type_configs','entity_event_types',
                      'trigger_points','event_triggers','relation_types')
   AND column_name IN ('industry','plugin_class')
 ORDER BY table_name, column_name;
