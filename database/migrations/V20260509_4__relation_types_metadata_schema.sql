-- ============================================================
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
-- V20260509_4: relation_types 加 metadata_schema 列承载 JSON Schema
--
-- 背景: access_relations.metadata 是 JSON 字段, 各业务乱塞
--   (isPrimary / dorm_room / seat_no / weightRatio / role 等)。
-- 没有 schema 校验, 数据脏。
--
-- 方案: 在关系类型字典 relation_types 上预留 metadata_schema JSON 列,
-- 后续可承载完整 JSON Schema。当前 (Phase 4 W4.3) 仅建列, 应用层
-- MetadataSchemaValidator 用简化版 required-keys 校验。
--
-- 条件化 (项目惯例) — 重复执行幂等。
-- ============================================================

SET @stmt := IF(
    (SELECT COUNT(1) FROM information_schema.columns
       WHERE table_schema = DATABASE()
         AND table_name   = 'relation_types'
         AND column_name  = 'metadata_schema') = 0,
    'ALTER TABLE relation_types
        ADD COLUMN metadata_schema JSON NULL
            COMMENT ''关系记录 metadata 的 JSON Schema (可选, Phase 5 启用)''
            AFTER implied_relations',
    'SELECT 1 -- metadata_schema already exists, skip');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;
