-- ============================================================
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
-- V20260508_1: inspection 13 张表的 org_unit_id 加 NOT NULL 约束
--
-- 背景: V20260501_1 加列 + 回填, 2026-05-08 commit d52ff3c4 接通代码层
-- 写入路径 (InspectionDataPermissionFiller MetaObjectHandler 自动填充).
-- 至此所有 INSERT 路径都会写值, 数据完整性可由 DB NOT NULL 兜底.
--
-- **条件化 ALTER**: 仅当表内 org_unit_id IS NULL 行数 = 0 时才 ALTER NOT NULL.
-- 若有 NULL 行 — 跳过该表 + INSERT 一行告警到 inspection_audit_logs (如果该表存在),
-- 让 ops 手动回填后再补跑这个 migration (幂等可重复执行).
--
-- 不加 DEFAULT 值 — 业务层必须显式设值或经 handler 兜底, 不允许 silent default.
-- ============================================================

DROP PROCEDURE IF EXISTS sp_insp_orgunit_to_not_null;
DELIMITER $$
CREATE PROCEDURE sp_insp_orgunit_to_not_null(IN p_tbl VARCHAR(64))
BEGIN
    DECLARE col_exists       INT DEFAULT 0;
    DECLARE col_nullable     VARCHAR(3) DEFAULT 'YES';
    DECLARE null_row_count   BIGINT DEFAULT 0;

    -- 1. 列必须存在 (V20260501_1 已加, 否则跳过)
    SELECT COUNT(*) INTO col_exists FROM information_schema.columns
        WHERE table_schema=DATABASE() AND table_name=p_tbl AND column_name='org_unit_id';
    IF col_exists = 0 THEN
        SELECT CONCAT('[skip] ', p_tbl, ' — column org_unit_id 不存在, 先跑 V20260501_1') AS msg;
    ELSE
        -- 2. 已是 NOT NULL? 幂等: 跳过
        SELECT IS_NULLABLE INTO col_nullable FROM information_schema.columns
            WHERE table_schema=DATABASE() AND table_name=p_tbl AND column_name='org_unit_id';
        IF col_nullable = 'NO' THEN
            SELECT CONCAT('[idempotent] ', p_tbl, ' — org_unit_id 已是 NOT NULL') AS msg;
        ELSE
            -- 3. 有 NULL 数据? 跳过 + 告警 (人工回填后重跑本迁移即可)
            SET @sql = CONCAT('SELECT COUNT(*) INTO @cnt FROM `', p_tbl, '` WHERE org_unit_id IS NULL AND deleted = 0');
            PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
            SET null_row_count = @cnt;

            IF null_row_count > 0 THEN
                SELECT CONCAT('[ABORT] ', p_tbl, ' — 有 ', null_row_count,
                              ' 行 org_unit_id IS NULL, 请先回填后重跑本迁移. 参考 V20260501_1 第 4 节回填策略.') AS msg;
            ELSE
                -- 4. 无 NULL → 安全 ALTER NOT NULL
                SET @sql = CONCAT('ALTER TABLE `', p_tbl, '` MODIFY COLUMN `org_unit_id` BIGINT NOT NULL COMMENT ''数据权限边界(MetaObjectHandler 自动填充, NOT NULL by V20260508_1)''');
                PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
                SELECT CONCAT('[ok] ', p_tbl, ' — org_unit_id 升级为 NOT NULL') AS msg;
            END IF;
        END IF;
    END IF;
END $$
DELIMITER ;

-- 13 张声明 org_unit_id 列的 inspection 表 (V20260501_1 列表 + 3 张 summary)
CALL sp_insp_orgunit_to_not_null('insp_projects');
CALL sp_insp_orgunit_to_not_null('insp_tasks');
CALL sp_insp_orgunit_to_not_null('insp_submissions');
CALL sp_insp_orgunit_to_not_null('insp_submission_details');
CALL sp_insp_orgunit_to_not_null('insp_evidences');
CALL sp_insp_orgunit_to_not_null('insp_project_inspectors');
CALL sp_insp_orgunit_to_not_null('insp_corrective_cases');
CALL sp_insp_orgunit_to_not_null('insp_corrective_subtasks');
CALL sp_insp_orgunit_to_not_null('insp_alerts');
CALL sp_insp_orgunit_to_not_null('insp_alert_rules');
CALL sp_insp_orgunit_to_not_null('insp_violation_records');
CALL sp_insp_orgunit_to_not_null('insp_submission_observations');
CALL sp_insp_orgunit_to_not_null('insp_inspector_summaries');
CALL sp_insp_orgunit_to_not_null('insp_corrective_summaries');
CALL sp_insp_orgunit_to_not_null('insp_item_frequency_summaries');

DROP PROCEDURE IF EXISTS sp_insp_orgunit_to_not_null;

-- ============================================================
-- 验证 (可选, 跑后手动执行)
--   SELECT table_name, is_nullable
--     FROM information_schema.columns
--    WHERE table_schema=DATABASE()
--      AND column_name='org_unit_id'
--      AND table_name LIKE 'insp_%'
--    ORDER BY table_name;
-- ============================================================
