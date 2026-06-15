-- 闸2 / 2b: 数据范围"类型过滤"(关系∩类型)
-- data_resources: 声明资源表的类型码字段 + 类型选项来源实体
-- role_data_scopes: 角色对该资源的类型过滤集 (与组织范围 AND 组合)
-- 让已升级到 baseline_v3 的开发库追上 baseline。新库直接由 baseline_v3.sql 建出。

ALTER TABLE `data_resources`
  ADD COLUMN `type_field` varchar(50) DEFAULT NULL COMMENT '业务表里指向类型码的字段(类型过滤用)' AFTER `creator_field`,
  ADD COLUMN `type_entity` varchar(20) DEFAULT NULL COMMENT '类型选项取自 entity_type_configs 的 entity_type(USER/PLACE/ORG_UNIT)' AFTER `type_field`;

ALTER TABLE `role_data_scopes`
  ADD COLUMN `type_filter` json DEFAULT NULL COMMENT '类型过滤: 类型码数组, 与组织范围 AND 组合; NULL=不限' AFTER `custom_org_unit_ids`;

-- user 资源支持按 user_type_code 过滤 (选项取自 entity_type_configs entity_type=USER)
UPDATE `data_resources` SET `type_field`='user_type_code', `type_entity`='USER'
  WHERE `resource_code` IN ('user','system_user');
