-- ============================================================================
-- V20260619_4: 统一锚定 R2.4 T1b — DROP data_resources 冗余锚点列 org_unit_field / creator_field
--
-- 背景: 锚点 (组织/创建者) 真相已迁至 resource_relations 注册表 (R2.1 登记 + R2.2 引擎改读)。
-- T1a 起 DataPermissionInterceptor.buildMeta 只读注册表 (注解/列兜底已删, forResource 缺则
-- fail-fast)。data_resources.org_unit_field / creator_field 至此为死列, DynamicModuleService /
-- DataModulePO / DataResourcePO / toMap 对应字段同步删除。
--
-- 破坏性直接 DROP (项目无真实数据, 不考虑兼容)。baseline_v3 的 CREATE + INSERT 已同步去除这两列;
-- 本迁移仅供已升级开发库追平。
-- ============================================================================

ALTER TABLE `data_resources` DROP COLUMN `org_unit_field`, DROP COLUMN `creator_field`;
