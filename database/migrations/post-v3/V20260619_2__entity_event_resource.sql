-- V20260619_2: 统一数据归属 R2.2 前置② — entity_events 拆出独立资源码 entity_event
-- entity_events 表无 org_unit_id, 此前复用 module=inspection_record + orgUnitField=created_by 的 workaround;
-- R2.2 注册表优先后会给它套 org_unit_id 致 SQL 报错, 故拆码。锚点照搬现状(created_by), 字节等价。
-- CLASS_TEACHER 的 inspection_record scope=SELF, 而无配置默认即 SELF, 故无需复制 role_data_scopes。
-- 幂等。

INSERT IGNORE INTO `data_resources`
  (`resource_code`,`resource_name`,`domain_code`,`industry`,`domain_name`,`creator_field`,`registered_by`,`sort_order`,`enabled`,`tenant_id`,`plugin_enabled`,`allowed_scopes`,`resource_kind`)
VALUES ('entity_event','实体事件','inspection','CORE','检查平台','created_by','CORE',47,1,1,1,'["ALL","SELF"]','PLAIN');
