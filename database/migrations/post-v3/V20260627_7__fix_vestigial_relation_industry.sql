-- P0-H1 配套: 修补 vestigial 关系种子行的 industry (dev 库追赶)。
-- guardian_of/supervisor_of (概念已合并入 family_of) + advisor_of (DEPRECATED+禁用) 是历史遗留种子行,
-- registered_by 标了插件却未被任何 PluginPackage.contribute() 贡献 → industry=NULL → 禁用插件治理盲区。
-- 设为其 tier 对应行业, 使 PluginLifecycleService.disable 的 WHERE industry=? 能统一治理。
-- 注: 被 contribute() 贡献的关系 (family_of/emergency_contact 等) 的 industry 已由代码修复 (P0-H1:
--   ContributionDispatcher 传 pkg.metadata().industryCode() 给 RelationTypeUpserter, 启动期自愈), 不在此处。

UPDATE `relation_types` SET `industry`='COMMON_EXT'
  WHERE `relation_code` IN ('guardian_of','supervisor_of') AND `industry` IS NULL;
UPDATE `relation_types` SET `industry`='EDU'
  WHERE `relation_code`='advisor_of' AND `industry` IS NULL;
