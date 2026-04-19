-- ============================================================
-- Phase 1.1  统一 origin 字段 (取代 industry + tier + registered_by 三兄弟)
-- 日期: 2026-04-21
--
-- origin 值格式 (可解析):
--   "PLUGIN:<code>@<version>"        — 插件主流声明, 如 "PLUGIN:CORE@1.0.0"
--   "PLUGIN:<code>@<version>:legacy" — 插件遗留子流 (已合并, 暂保留关键字)
--   "TENANT:CUSTOM#<tenantId>"        — 租户自定义, 如 "TENANT:CUSTOM#1"
--
-- 策略:
--   A. 为 7 张贡献表加 origin VARCHAR(128) 列
--   B. 数据回填: origin = synthesize(industry, plugin_class)
--      - industry=CUSTOM → 'TENANT:CUSTOM#' || tenant_id (若有) 或 'TENANT:CUSTOM#1'
--      - industry=<code> + plugin_class 非空 → 'PLUGIN:<code>@<version>'
--        版本从 plugin_packages 查, 默认 '1.0.0'
--      - 无 industry 无 plugin_class → NULL (后续 Phase 识别为脏数据)
--
--   C. 旧字段 industry + plugin_class + tier + registered_by 保留 (Phase 1.5 再 DROP)
--
-- 幂等: IF NOT EXISTS 守卫, 重复执行安全
-- ============================================================

-- ─────────────────────────────────────────────────────────
-- A. 为 7 张贡献表加 origin 列 (MySQL 8 无 ADD IF NOT EXISTS, 用 information_schema 守卫)
-- ─────────────────────────────────────────────────────────
DROP PROCEDURE IF EXISTS _add_origin_col;
DELIMITER //
CREATE PROCEDURE _add_origin_col(IN p_table VARCHAR(64))
BEGIN
    DECLARE col_exists INT DEFAULT 0;
    SELECT COUNT(*) INTO col_exists
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name   = p_table
       AND column_name  = 'origin';
    IF col_exists = 0 THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `origin` VARCHAR(128) NULL COMMENT ''统一来源: PLUGIN:<code>@<ver> / TENANT:CUSTOM#<id>'' AFTER `plugin_class`');
        PREPARE s FROM @sql;
        EXECUTE s;
        DEALLOCATE PREPARE s;
    END IF;
END //
DELIMITER ;

CALL _add_origin_col('permissions');
CALL _add_origin_col('roles');
CALL _add_origin_col('entity_type_configs');
CALL _add_origin_col('entity_event_types');
CALL _add_origin_col('trigger_points');
CALL _add_origin_col('event_triggers');
CALL _add_origin_col('relation_types');
CALL _add_origin_col('data_scope_dims');

DROP PROCEDURE _add_origin_col;

-- ─────────────────────────────────────────────────────────
-- B. 数据回填: 从 industry+plugin_class 合成 origin
-- ─────────────────────────────────────────────────────────
-- 先查每个 industry 对应的版本号 (plugin_packages.version), 缓存到临时表
-- 版本映射临时表 (显式 COLLATE 对齐避免 JOIN 报 illegal mix)
DROP TEMPORARY TABLE IF EXISTS _origin_version_map;
CREATE TEMPORARY TABLE _origin_version_map (
  industry_code VARCHAR(20) COLLATE utf8mb4_unicode_ci,
  version       VARCHAR(20) COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (industry_code)
);
INSERT INTO _origin_version_map (industry_code, version)
  SELECT CONVERT(industry_code USING utf8mb4) COLLATE utf8mb4_unicode_ci,
         CONVERT(version       USING utf8mb4) COLLATE utf8mb4_unicode_ci
    FROM plugin_packages;

-- permissions
UPDATE permissions p
  LEFT JOIN _origin_version_map v ON v.industry_code = p.industry COLLATE utf8mb4_unicode_ci
   SET p.origin = CASE
        WHEN p.industry = 'CUSTOM' THEN 'TENANT:CUSTOM#1'
        WHEN p.industry IS NOT NULL THEN
            CONCAT('PLUGIN:', p.industry, '@', COALESCE(v.version, '1.0.0'))
        ELSE NULL
   END
 WHERE p.origin IS NULL;

-- roles
UPDATE roles r
  LEFT JOIN _origin_version_map v ON v.industry_code = r.industry COLLATE utf8mb4_unicode_ci
   SET r.origin = CASE
        WHEN r.industry = 'CUSTOM' THEN 'TENANT:CUSTOM#1'
        WHEN r.industry IS NOT NULL THEN
            CONCAT('PLUGIN:', r.industry, '@', COALESCE(v.version, '1.0.0'))
        ELSE NULL
   END
 WHERE r.origin IS NULL;

-- entity_type_configs
UPDATE entity_type_configs e
  LEFT JOIN _origin_version_map v ON v.industry_code = e.industry COLLATE utf8mb4_unicode_ci
   SET e.origin = CASE
        WHEN e.industry = 'CUSTOM' THEN 'TENANT:CUSTOM#1'
        WHEN e.industry IS NOT NULL THEN
            CONCAT('PLUGIN:', e.industry, '@', COALESCE(v.version, '1.0.0'))
        ELSE NULL
   END
 WHERE e.origin IS NULL;

-- entity_event_types
UPDATE entity_event_types et
  LEFT JOIN _origin_version_map v ON v.industry_code = et.industry COLLATE utf8mb4_unicode_ci
   SET et.origin = CASE
        WHEN et.industry = 'CUSTOM' THEN 'TENANT:CUSTOM#1'
        WHEN et.industry IS NOT NULL THEN
            CONCAT('PLUGIN:', et.industry, '@', COALESCE(v.version, '1.0.0'))
        ELSE NULL
   END
 WHERE et.origin IS NULL;

-- trigger_points
UPDATE trigger_points tp
  LEFT JOIN _origin_version_map v ON v.industry_code = tp.industry COLLATE utf8mb4_unicode_ci
   SET tp.origin = CASE
        WHEN tp.industry = 'CUSTOM' THEN 'TENANT:CUSTOM#1'
        WHEN tp.industry IS NOT NULL THEN
            CONCAT('PLUGIN:', tp.industry, '@', COALESCE(v.version, '1.0.0'))
        ELSE NULL
   END
 WHERE tp.origin IS NULL;

-- event_triggers (无 industry 的行保留 NULL, 后续 MessagingRegistrar 会写)
UPDATE event_triggers et
  LEFT JOIN _origin_version_map v ON v.industry_code = et.industry COLLATE utf8mb4_unicode_ci
   SET et.origin = CASE
        WHEN et.industry = 'CUSTOM' THEN 'TENANT:CUSTOM#1'
        WHEN et.industry IS NOT NULL THEN
            CONCAT('PLUGIN:', et.industry, '@', COALESCE(v.version, '1.0.0'))
        ELSE NULL
   END
 WHERE et.origin IS NULL;

-- relation_types (industry 字段已存在, plugin_class 字段可能为空, 优先 industry)
UPDATE relation_types r
  LEFT JOIN _origin_version_map v ON v.industry_code = r.industry COLLATE utf8mb4_unicode_ci
   SET r.origin = CASE
        WHEN r.industry = 'CUSTOM' THEN 'TENANT:CUSTOM#1'
        WHEN r.industry IS NOT NULL THEN
            CONCAT('PLUGIN:', r.industry, '@', COALESCE(v.version, '1.0.0'))
        ELSE NULL
   END
 WHERE r.origin IS NULL;

-- data_scope_dims
UPDATE data_scope_dims d
  LEFT JOIN _origin_version_map v ON v.industry_code = d.industry COLLATE utf8mb4_unicode_ci
   SET d.origin = CASE
        WHEN d.industry = 'CUSTOM' THEN 'TENANT:CUSTOM#1'
        WHEN d.industry IS NOT NULL THEN
            CONCAT('PLUGIN:', d.industry, '@', COALESCE(v.version, '1.0.0'))
        ELSE NULL
   END
 WHERE d.origin IS NULL;

DROP TEMPORARY TABLE _origin_version_map;

-- ─────────────────────────────────────────────────────────
-- C. 验证 — 8 张表的 origin 字段填充率应该 = 插件声明的行数
-- ─────────────────────────────────────────────────────────
SELECT 'permissions' AS tbl, COUNT(*) AS total, SUM(origin IS NOT NULL) AS has_origin
  FROM permissions WHERE deleted = 0
UNION ALL SELECT 'roles', COUNT(*), SUM(origin IS NOT NULL) FROM roles WHERE deleted = 0
UNION ALL SELECT 'entity_type_configs', COUNT(*), SUM(origin IS NOT NULL) FROM entity_type_configs WHERE deleted = 0
UNION ALL SELECT 'entity_event_types', COUNT(*), SUM(origin IS NOT NULL) FROM entity_event_types WHERE deleted = 0
UNION ALL SELECT 'trigger_points', COUNT(*), SUM(origin IS NOT NULL) FROM trigger_points WHERE deleted = 0
UNION ALL SELECT 'event_triggers', COUNT(*), SUM(origin IS NOT NULL) FROM event_triggers WHERE deleted = 0
UNION ALL SELECT 'relation_types', COUNT(*), SUM(origin IS NOT NULL) FROM relation_types
UNION ALL SELECT 'data_scope_dims', COUNT(*), SUM(origin IS NOT NULL) FROM data_scope_dims;
