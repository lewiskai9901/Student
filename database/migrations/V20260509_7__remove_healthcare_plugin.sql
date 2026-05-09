-- 卸载医疗(healthcare)行业插件 — 不删数据,标 enabled=0 防回归;旧关系数据保留作历史

-- 1. 关系类型字典 4 条 HEALTH 关系禁用
UPDATE relation_types SET is_enabled=0,
  description=CONCAT(IFNULL(description,''), ' [REMOVED 2026-05-09 — healthcare plugin uninstalled]')
WHERE relation_code IN ('attending_of', 'nurse_of', 'in_ward')
  AND tier='DOMAIN' AND industry='HEALTH';

-- 注: family_of 已在 W3.2 上移到 COMMON_EXT, HEALTH tier 的 family_of 已 DEPRECATED, 此处不重复

-- 2. plugin_packages 表的 HEALTH 行禁用
UPDATE plugin_packages SET enabled=0,
  industry_name=CONCAT(industry_name, ' [REMOVED]')
WHERE industry_code='HEALTH';

-- 3. tenant_plugin_enablement 表所有租户的 HEALTH 启用关闭
UPDATE tenant_plugin_enablement SET enabled=0,
  notes=CONCAT(IFNULL(notes,''), ' [auto-disabled by V20260509_7 healthcare uninstall]')
WHERE plugin_code='HEALTH';

-- 4. access_relations 历史记录里所有 HEALTH 域关系软删 (保留行供审计)
UPDATE access_relations SET deleted=1, deleted_at=NOW(),
  remark=CONCAT(IFNULL(remark,''), ' [auto-archived: healthcare plugin uninstalled]')
WHERE relation IN ('attending_of', 'nurse_of', 'in_ward') AND deleted=0;

-- 5. data_resources 表 HEALTH industry 资源禁用
UPDATE data_resources SET enabled=0
WHERE industry_code='HEALTH';
