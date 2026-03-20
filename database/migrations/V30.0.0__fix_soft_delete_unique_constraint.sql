-- =====================================================
-- V30.0.0 - 修复逻辑删除与唯一约束冲突
-- 问题: deleted 列为 TINYINT(0/1)，(username, deleted) 唯一索引
--        导致同用户名多次软删除时冲突
-- 方案: deleted 改为 BIGINT，删除时存 id 值，保证唯一
-- =====================================================

-- 1. 扩大 deleted 列类型（TINYINT → BIGINT），兼容存储 id
ALTER TABLE `users` MODIFY COLUMN `deleted` BIGINT DEFAULT 0 COMMENT '逻辑删除:0未删除,删除时存id';

-- 2. 修复已有冲突数据：将 deleted=1 的记录改为 deleted=id
UPDATE `users` SET `deleted` = `id` WHERE `deleted` = 1;
