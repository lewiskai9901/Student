-- =====================================================
-- V29.0.0 - users 表清理与 schema 对齐
-- 删除废弃的 managed_class_id 列
-- 确保 complete_schema_v2.sql 与实际 DB 一致
-- =====================================================

-- 删除废弃列 (UserPO 已移除该字段)
ALTER TABLE `users` DROP COLUMN IF EXISTS `managed_class_id`;
