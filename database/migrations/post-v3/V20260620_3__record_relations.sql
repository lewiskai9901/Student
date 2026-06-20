-- ============================================================================
-- V20260620_3: 统一锚定 R4 地基 — 新建 record_relations (记录↔主体一等表)
--
-- 设计稿 §5.2。与 access_relations (纯主体图) 分立: 无主体锁键生成列、无传递 BFS、
-- record_id 是业务记录 (非主体)。承载 RECORD_RELATION storage_kind 的多值关系
-- (reviewer / 多被检查 / shared_with 等)。
--
-- ⚠ 本迁移仅建表 (additive 地基)。引擎 (ScopeEvaluator) 尚未读它 —— RECORD_RELATION
-- 分支接线 + 写入泛化 (RelationColumnFiller) 是 R4 后续高风险步, 另做。
-- ============================================================================

CREATE TABLE IF NOT EXISTS `record_relations` (
  `id`            bigint NOT NULL AUTO_INCREMENT,
  `resource_code` varchar(50) NOT NULL COMMENT '哪个资源, 对应 data_resources.resource_code',
  `record_id`     bigint      NOT NULL COMMENT '业务记录 id (非主体)',
  `relation_code` varchar(50) NOT NULL COMMENT '关系码: reviewer / inspected / shared_with ...',
  `subject_type`  varchar(20) NOT NULL COMMENT '主体类型: USER/ORG_UNIT/PLACE/ASSET',
  `subject_id`    bigint      NOT NULL COMMENT '主体 id',
  `access_level`  varchar(20) NOT NULL DEFAULT 'READ_ONLY',
  `valid_from`    datetime DEFAULT CURRENT_TIMESTAMP,
  `valid_to`      datetime DEFAULT NULL,
  `metadata`      json DEFAULT NULL,
  `created_by`    bigint DEFAULT NULL,
  `created_at`    datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       tinyint(1) NOT NULL DEFAULT 0,
  `tenant_id`     bigint NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_record_relation` (`resource_code`,`record_id`,`relation_code`,`subject_type`,`subject_id`,`tenant_id`,`deleted`),
  KEY `idx_by_subject` (`subject_type`,`subject_id`,`relation_code`,`resource_code`,`deleted`),
  KEY `idx_by_record`  (`resource_code`,`record_id`,`relation_code`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='记录↔主体关系表 (扁平, 无传递; 与 access_relations 主体图分立)';
