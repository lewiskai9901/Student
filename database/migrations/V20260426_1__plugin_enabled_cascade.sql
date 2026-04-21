-- V20260426_1: 9 张贡献表加 plugin_enabled 列 (两状态生命周期模型)
-- 两状态模型:
--   is_enabled     : 管理员手动开关 (单条)
--   plugin_enabled : 所属插件级开关 (批量, 由 PluginLifecycleService 维护)
-- 实际生效 = is_enabled AND plugin_enabled
-- 所有列默认 1 (兼容现有数据), PluginEnabledSyncRunner 启动时按 plugin_packages.enabled 同步.

-- ============================================================
-- entity_type_configs
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='entity_type_configs' AND column_name='plugin_enabled');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE entity_type_configs ADD COLUMN plugin_enabled TINYINT NOT NULL DEFAULT 1 COMMENT ''插件级启用状态 (0=所属插件被禁)''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- relation_types
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='relation_types' AND column_name='plugin_enabled');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE relation_types ADD COLUMN plugin_enabled TINYINT NOT NULL DEFAULT 1 COMMENT ''插件级启用状态''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- entity_event_types
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='entity_event_types' AND column_name='plugin_enabled');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE entity_event_types ADD COLUMN plugin_enabled TINYINT NOT NULL DEFAULT 1 COMMENT ''插件级启用状态''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- trigger_points
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='trigger_points' AND column_name='plugin_enabled');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE trigger_points ADD COLUMN plugin_enabled TINYINT NOT NULL DEFAULT 1 COMMENT ''插件级启用状态''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- event_triggers
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='event_triggers' AND column_name='plugin_enabled');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE event_triggers ADD COLUMN plugin_enabled TINYINT NOT NULL DEFAULT 1 COMMENT ''插件级启用状态''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- roles
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='roles' AND column_name='plugin_enabled');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE roles ADD COLUMN plugin_enabled TINYINT NOT NULL DEFAULT 1 COMMENT ''插件级启用状态''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- permissions
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='permissions' AND column_name='plugin_enabled');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE permissions ADD COLUMN plugin_enabled TINYINT NOT NULL DEFAULT 1 COMMENT ''插件级启用状态''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- data_scope_dims
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='data_scope_dims' AND column_name='plugin_enabled');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE data_scope_dims ADD COLUMN plugin_enabled TINYINT NOT NULL DEFAULT 1 COMMENT ''插件级启用状态''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- msg_subscription_rules (无 industry 列, 但加上 plugin_enabled 用于按 event_type 级联)
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='msg_subscription_rules' AND column_name='plugin_enabled');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE msg_subscription_rules ADD COLUMN plugin_enabled TINYINT NOT NULL DEFAULT 1 COMMENT ''插件级启用状态 (级联自订阅的 event_type 的 plugin)''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- plugin_packages: 加 last_disabled_at
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='plugin_packages' AND column_name='last_disabled_at');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE plugin_packages ADD COLUMN last_disabled_at DATETIME NULL COMMENT ''最近禁用时间''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
