-- ============================================================================
-- V20260622_1: resource_relations 加 resolver_bean 列 (统一锚定 R3c / P1)
--
-- 背景: storage_kind 新增第 5 档 PROVIDER (接口式关系) —— 关系逻辑由插件
-- RecordRelationResolver bean 算。本列存该 bean 名 (其它 storage_kind 为 NULL)。
-- ============================================================================

ALTER TABLE resource_relations
  ADD COLUMN resolver_bean VARCHAR(100) DEFAULT NULL
  COMMENT 'PROVIDER: RecordRelationResolver 的 Spring bean 名'
  AFTER ar_relation;
