-- ============================================================================
-- V20260613_4: 彻底删除 access_relations / access_relations_history 的 include_children 列
--
-- 背景: ADR-002 早已把"关系传递性"的真相收敛到类型层 (RelationTypeDef.isTransitive
-- + impliedRelations); 记录级 include_children 字段被标 @Deprecated, BFS 从不读它,
-- 写库/回传纯属死重量。baseline_v3 注释里写"V26 大版本计划 DROP", 但本项目开发期
-- 原则是"删字段即同步删 DB 列, 不留旧列", 故现在就铲除, 不等大版本。
--
-- 安全性: 该列不被任何生成列 / 唯一键引用 (membership_lock_key / place_belongs_lock_key
-- / place_responsible_lock_key / active_uniq 均不含 include_children), 直接 DROP 无依赖。
-- 后端 insertDirect / 归档 INSERT...SELECT 已同步移除该列 (同提交)。
--
-- 幂等: information_schema 条件化。baseline_v3.sql 已同步删列。
-- ============================================================================

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'access_relations' AND COLUMN_NAME = 'include_children');
SET @s := IF(@col = 1,
    "ALTER TABLE access_relations DROP COLUMN include_children",
    "SELECT 'access_relations.include_children already dropped' AS msg");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'access_relations_history' AND COLUMN_NAME = 'include_children');
SET @s := IF(@col = 1,
    "ALTER TABLE access_relations_history DROP COLUMN include_children",
    "SELECT 'access_relations_history.include_children already dropped' AS msg");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
