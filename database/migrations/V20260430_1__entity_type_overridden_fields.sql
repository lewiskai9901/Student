-- V20260430_1: entity_type_configs 加 overridden_fields JSON 列
--
-- 用途: 记录管理员 UI 上"显式覆写"的字段集合, 让 PluginRegistrar
-- 启动期 UPSERT 时对这些字段保留管理员值, 其他字段仍跟插件声明走.
--
-- 可覆写字段白名单: typeName / category / uiConfig
-- 不可覆写: typeCode (主键) / allowedChildTypeCodes (结构契约) /
--           metadataSchema 系统字段 (数据兼容性) / industry / pluginClass / origin
--
-- NULL 或 '[]' = 无覆写, 完全受插件控制 (等同现状)

SET @col = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'entity_type_configs'
      AND column_name = 'overridden_fields');
SET @sql = IF(@col = 0,
    'ALTER TABLE entity_type_configs ADD COLUMN overridden_fields JSON NULL COMMENT ''管理员显式覆写的字段名数组, 如 ["typeName","category"]''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT '=== entity_type_configs.overridden_fields added ===' AS stage;
SELECT COUNT(*) AS total_types,
       SUM(CASE WHEN overridden_fields IS NOT NULL AND JSON_LENGTH(overridden_fields) > 0 THEN 1 ELSE 0 END) AS with_override
FROM entity_type_configs WHERE deleted = 0;
