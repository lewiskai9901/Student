-- V20260619_1: 统一数据归属架构 R1 — 资源关系注册表 + data_resources.resource_kind
-- baseline_v3.sql 已含同等 DDL; 本迁移仅供已存在的开发库追平。幂等, 可重复执行。

-- 1. resource_relations 注册表 (行由 PluginPackage.contribute() 启动期 UPSERT, 无 seed)
CREATE TABLE IF NOT EXISTS `resource_relations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `resource_code` varchar(50) NOT NULL COMMENT 'FK -> data_resources.resource_code',
  `relation_code` varchar(50) NOT NULL COMMENT '关系码: creator/owner_org/inspector/inspected/reviewer',
  `relation_name` varchar(50) NOT NULL COMMENT '人话显示名(UI)',
  `subject_type` varchar(20) NOT NULL COMMENT '指向主体: USER/ORG_UNIT/PLACE/ASSET/ANY',
  `cardinality` varchar(10) NOT NULL COMMENT 'SINGLE/MULTI',
  `storage_kind` varchar(20) NOT NULL COMMENT 'SUBJECT_GRAPH/COLUMN/RECORD_RELATION',
  `column_name` varchar(50) DEFAULT NULL COMMENT 'COLUMN: 业务表列名',
  `type_column` varchar(50) DEFAULT NULL COMMENT '多态主体的类型列 (配 column_name)',
  `ar_relation` varchar(30) DEFAULT NULL COMMENT 'SUBJECT_GRAPH/RECORD_RELATION: relation 值',
  `auto_fill` tinyint(1) NOT NULL DEFAULT '0' COMMENT '写入是否自动填列',
  `grants_by_default` tinyint(1) NOT NULL DEFAULT '0' COMMENT '无显式授予时默认参与可见性',
  `industry` varchar(20) DEFAULT NULL COMMENT '贡献插件 CORE/EDU/...',
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `tenant_id` bigint NOT NULL DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_resource_relation` (`resource_code`,`relation_code`,`tenant_id`),
  KEY `idx_resource` (`resource_code`,`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资源关系注册表 — 统一锚定模型唯一真相';

-- 2. data_resources.resource_kind (条件化 ADD COLUMN, 参考 V97/V104 信息架构幂等模式)
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'data_resources' AND COLUMN_NAME = 'resource_kind');
SET @sql := IF(@col = 0,
  "ALTER TABLE `data_resources` ADD COLUMN `resource_kind` varchar(10) NOT NULL DEFAULT 'PLAIN' COMMENT '资源种类 SUBJECT/PLAIN (统一锚定模型第一根轴)'",
  "DO 0");
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. 标记主体型资源 (幂等): 记录本身是主体, 归属走 access_relations 非列
UPDATE `data_resources` SET `resource_kind`='SUBJECT'
 WHERE `resource_code` IN ('user','system_user','org_unit','place','student');
