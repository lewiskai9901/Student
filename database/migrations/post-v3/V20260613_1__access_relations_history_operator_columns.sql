-- ============================================================================
-- V20260613_1: access_relations_history 补 Phase 7 W7.3 操作者上下文三列
--
-- 背景: AccessRelationRepositoryImpl.archiveAndSoftDelete (Phase 7 W7.3) 的归档
-- INSERT 写 operator_ip / operator_user_agent / operation 三列, 但 baseline_v3
-- squash 时漏掉了它们 → **所有 revoke 路径自 baseline 重建起即坏**
-- (Unknown column 'operator_ip'), 含归属变更(clearMembership/clearBelonging)、
-- 关系管理页解绑。2026-06-13 场所归属关系化 E2E 钓出此既有缺口。
--
-- 幂等: information_schema 条件化。baseline_v3.sql 已同步补列。
-- ============================================================================

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'access_relations_history' AND COLUMN_NAME = 'operator_ip');
SET @s := IF(@col = 0,
    "ALTER TABLE access_relations_history ADD COLUMN operator_ip VARCHAR(64) DEFAULT NULL COMMENT '操作者IP (Phase 7 W7.3)' AFTER archived_by",
    "SELECT 'operator_ip exists' AS msg");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'access_relations_history' AND COLUMN_NAME = 'operator_user_agent');
SET @s := IF(@col = 0,
    "ALTER TABLE access_relations_history ADD COLUMN operator_user_agent VARCHAR(500) DEFAULT NULL COMMENT '操作者UA (Phase 7 W7.3)' AFTER operator_ip",
    "SELECT 'operator_user_agent exists' AS msg");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'access_relations_history' AND COLUMN_NAME = 'operation');
SET @s := IF(@col = 0,
    "ALTER TABLE access_relations_history ADD COLUMN operation VARCHAR(20) NOT NULL DEFAULT 'REVOKE' COMMENT '归档操作类型 REVOKE/EXPIRE (Phase 7 W7.3)' AFTER operator_user_agent",
    "SELECT 'operation exists' AS msg");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
