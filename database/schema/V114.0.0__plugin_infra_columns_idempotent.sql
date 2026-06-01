-- V114.0.0 插件基础设施列冷启动补齐 (修 fresh-init 债, 归属重构无关)
-- 背景: PluginRegistrar 等启动组件用裸 JdbcTemplate 查 entity_type_configs.industry /
--   relation_types.plugin_class / *.origin 等列(不走 PO, 故列级对账未覆盖)。
--   原 V20260420_1 / V20260421_1 用 'ADD COLUMN ... AFTER `industry`' 形式, 但回放中
--   industry/plugin_class 在目标表上不存在 → AFTER 引用不存在列 → 整条 ALTER 失败 → 启动崩。
-- 本迁移无 AFTER 子句、逐列 information_schema 条件化, 顺序无关、幂等。
-- 覆盖 7 张贡献表 + data_scope_dims 的 industry / plugin_class / origin。

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS _v114_add_col;
DELIMITER //
CREATE PROCEDURE _v114_add_col(IN p_table VARCHAR(64), IN p_col VARCHAR(64), IN p_def TEXT)
BEGIN
    DECLARE n INT DEFAULT 0;
    SELECT COUNT(*) INTO n FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = p_table AND column_name = p_col;
    IF n = 0 THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_col, '` ', p_def);
        PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
    END IF;
END //
DELIMITER ;

-- industry + plugin_class + origin for the 7 contribution tables
CALL _v114_add_col('permissions',        'industry',     "VARCHAR(20) NULL COMMENT '所属行业包'");
CALL _v114_add_col('permissions',        'plugin_class', "VARCHAR(200) NULL COMMENT '声明插件全限定类名'");
CALL _v114_add_col('permissions',        'origin',       "VARCHAR(128) NULL COMMENT '统一来源'");
CALL _v114_add_col('roles',              'industry',     "VARCHAR(20) NULL COMMENT '所属行业包'");
CALL _v114_add_col('roles',              'plugin_class', "VARCHAR(200) NULL COMMENT '声明插件全限定类名'");
CALL _v114_add_col('roles',              'origin',       "VARCHAR(128) NULL COMMENT '统一来源'");
CALL _v114_add_col('entity_type_configs','industry',     "VARCHAR(20) NULL COMMENT '所属行业包'");
CALL _v114_add_col('entity_type_configs','plugin_class', "VARCHAR(200) NULL COMMENT '声明插件全限定类名'");
CALL _v114_add_col('entity_type_configs','origin',       "VARCHAR(128) NULL COMMENT '统一来源'");
CALL _v114_add_col('entity_event_types', 'industry',     "VARCHAR(20) NULL COMMENT '所属行业包'");
CALL _v114_add_col('entity_event_types', 'plugin_class', "VARCHAR(200) NULL COMMENT '声明插件全限定类名'");
CALL _v114_add_col('entity_event_types', 'origin',       "VARCHAR(128) NULL COMMENT '统一来源'");
CALL _v114_add_col('trigger_points',     'industry',     "VARCHAR(20) NULL COMMENT '所属行业包'");
CALL _v114_add_col('trigger_points',     'plugin_class', "VARCHAR(200) NULL COMMENT '声明插件全限定类名'");
CALL _v114_add_col('trigger_points',     'origin',       "VARCHAR(128) NULL COMMENT '统一来源'");
CALL _v114_add_col('event_triggers',     'industry',     "VARCHAR(20) NULL COMMENT '所属行业包'");
CALL _v114_add_col('event_triggers',     'plugin_class', "VARCHAR(200) NULL COMMENT '声明插件全限定类名'");
CALL _v114_add_col('event_triggers',     'origin',       "VARCHAR(128) NULL COMMENT '统一来源'");
CALL _v114_add_col('relation_types',     'industry',     "VARCHAR(20) NULL COMMENT '所属行业包'");
CALL _v114_add_col('relation_types',     'plugin_class', "VARCHAR(200) NULL COMMENT '声明插件全限定类名'");
CALL _v114_add_col('relation_types',     'origin',       "VARCHAR(128) NULL COMMENT '统一来源'");

DROP PROCEDURE _v114_add_col;
