-- 数据权限缺口修复 (2026-06-27): 7 处裸奔业务读路径补 @DataPermission + 资源码。
-- data_resources 行须显式 seed (DataResourceUpserter 只 UPDATE 不 INSERT); resource_relations 锚点行
-- 由 contribution 启动期写 (CoreManifest/EducationManifest)。无 created_by 表不含 SELF (默认 SELF→DENY 安全)。
-- 幂等: 先删同码再插, 可重复 apply。

DELETE FROM `data_resources` WHERE `resource_code` IN
  ('indicator_result','inspection_corrective_rule','inspection_item_override',
   'inspection_audit_trail','rating_result','teaching_class','teaching_class_member');

INSERT INTO `data_resources`
  (`resource_code`,`resource_name`,`domain_code`,`domain_name`,`enabled`,`sort_order`,`registered_by`,
   `allowed_scopes`,`subject_relation_filterable`,`resource_kind`,`tenant_id`,`plugin_enabled`)
VALUES
('indicator_result','指标结果','inspection','检查平台',1,43,'CORE','["ALL","DEPARTMENT_AND_BELOW","MANAGED_ORGS_AND_BELOW","DEPARTMENT","MANAGED_ORGS","CUSTOM"]',0,'PLAIN',1,1),
('inspection_corrective_rule','整改规则','inspection','检查平台',1,44,'CORE','["ALL","DEPARTMENT_AND_BELOW","MANAGED_ORGS_AND_BELOW","DEPARTMENT","MANAGED_ORGS","SELF","CUSTOM"]',0,'PLAIN',1,1),
('inspection_item_override','项目项覆盖','inspection','检查平台',1,45,'CORE','["ALL","DEPARTMENT_AND_BELOW","MANAGED_ORGS_AND_BELOW","DEPARTMENT","MANAGED_ORGS","SELF","CUSTOM"]',0,'PLAIN',1,1),
('inspection_audit_trail','检查审计轨迹','inspection','检查平台',1,46,'CORE','["ALL","DEPARTMENT_AND_BELOW","MANAGED_ORGS_AND_BELOW","DEPARTMENT","MANAGED_ORGS","CUSTOM"]',0,'PLAIN',1,1),
('rating_result','评级结果','rating','评级',1,47,'CORE','["ALL","DEPARTMENT_AND_BELOW","MANAGED_ORGS_AND_BELOW","DEPARTMENT","MANAGED_ORGS","CUSTOM"]',0,'PLAIN',1,1),
('teaching_class','教学班','education','教育',1,68,'EducationPlugin','["ALL","SELF"]',0,'PLAIN',1,1),
('teaching_class_member','教学班成员','education','教育',1,69,'EducationPlugin','["ALL","BY_CLASS","BY_MAJOR","CUSTOM"]',0,'PLAIN',1,1);
