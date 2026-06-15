-- 数据范围: 把扁平 scope_type 重建为可组合三轴列 + 读写分离(apply_to)
-- axis① 组织锚点(org_anchor + anchor_param + include_subtree + custom_org_ids)
-- axis② 主体关系(subject_rel_include / subject_rel_exclude)
-- axis③ 类型过滤(type_filter, 已存在, 此处不动)
-- apply_to: 同一(role,resource) 可拆 READ / WRITE 两条规则; 默认 BOTH
-- 让已升级到 baseline_v3 的开发库追上 baseline。新库直接由 baseline_v3.sql 建出。
--
-- 本次为附加迁移: 新增列 + 翻译现有行。scope_type / custom_org_unit_ids 列暂保留
-- (当前在运行的旧后端仍读 scope_type), 待 T9 在代码停止读取后再 DROP。

ALTER TABLE `role_data_scopes`
  ADD COLUMN `apply_to` ENUM('READ','WRITE','BOTH') NOT NULL DEFAULT 'BOTH' COMMENT '读写分离: READ 只读范围 / WRITE 可写范围 / BOTH 两者' AFTER `resource_code`,
  ADD COLUMN `org_anchor` VARCHAR(20) DEFAULT NULL COMMENT 'axis① 组织锚点: ALL/SELF/PRIMARY_ORG/RELATION/CUSTOM_ORG/PLUGIN_DIM' AFTER `type_filter`,
  ADD COLUMN `anchor_param` VARCHAR(50) DEFAULT NULL COMMENT 'RELATION→关系码(admin); PLUGIN_DIM→维度码(BY_CLASS…)' AFTER `org_anchor`,
  ADD COLUMN `include_subtree` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '组织锚点是否含子树' AFTER `anchor_param`,
  ADD COLUMN `custom_org_ids` json DEFAULT NULL COMMENT 'axis① CUSTOM_ORG 时的组织 id 集' AFTER `include_subtree`,
  ADD COLUMN `subject_rel_include` json DEFAULT NULL COMMENT 'axis② 主体关系白名单(关系码集)' AFTER `custom_org_ids`,
  ADD COLUMN `subject_rel_exclude` json DEFAULT NULL COMMENT 'axis② 主体关系黑名单(关系码集)' AFTER `subject_rel_include`;

-- ── scope_type → 三轴翻译 (设计 §2 权威映射) ──
-- 翻译所有行 (deleted=0 与 deleted!=0 一并), 不留不一致行。apply_to 取默认 'BOTH'。
UPDATE `role_data_scopes` SET `org_anchor`='ALL',         `anchor_param`=NULL,    `include_subtree`=0 WHERE `scope_type`='ALL';
UPDATE `role_data_scopes` SET `org_anchor`='SELF',        `anchor_param`=NULL,    `include_subtree`=0 WHERE `scope_type`='SELF';
UPDATE `role_data_scopes` SET `org_anchor`='PRIMARY_ORG', `anchor_param`=NULL,    `include_subtree`=0 WHERE `scope_type`='DEPARTMENT';
UPDATE `role_data_scopes` SET `org_anchor`='PRIMARY_ORG', `anchor_param`=NULL,    `include_subtree`=1 WHERE `scope_type`='DEPARTMENT_AND_BELOW';
UPDATE `role_data_scopes` SET `org_anchor`='RELATION',    `anchor_param`='admin', `include_subtree`=0 WHERE `scope_type`='MANAGED_ORGS';
UPDATE `role_data_scopes` SET `org_anchor`='RELATION',    `anchor_param`='admin', `include_subtree`=1 WHERE `scope_type`='MANAGED_ORGS_AND_BELOW';
UPDATE `role_data_scopes` SET `org_anchor`='CUSTOM_ORG',  `anchor_param`=NULL,    `include_subtree`=0, `custom_org_ids`=`custom_org_unit_ids` WHERE `scope_type`='CUSTOM';
-- 插件维度 (BY_CLASS / BY_MAJOR / BY_GRADE 等): 原 scope_type 串作为 anchor_param 留存
UPDATE `role_data_scopes` SET `org_anchor`='PLUGIN_DIM',  `anchor_param`=`scope_type`, `include_subtree`=0
  WHERE `scope_type` NOT IN ('ALL','SELF','DEPARTMENT','DEPARTMENT_AND_BELOW','MANAGED_ORGS','MANAGED_ORGS_AND_BELOW','CUSTOM');

-- ── 唯一键: 纳入 apply_to (同一 role+resource 可拆读/写两条) ──
ALTER TABLE `role_data_scopes`
  DROP INDEX `uk_role_res`,
  ADD UNIQUE KEY `uk_role_res` (`role_id`,`resource_code`,`apply_to`,`tenant_id`);
-- idx_role(role_id,deleted) 保持不变。

-- 注: scope_type 与 custom_org_unit_ids 列在本迁移中保留, 待 T9 (代码停止读取后) DROP。
