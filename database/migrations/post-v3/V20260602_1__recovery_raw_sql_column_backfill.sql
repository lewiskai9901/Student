-- V20260602_1 baseline_v3 后续: 回补运行时暴露的裸 SQL 缺列
-- 背景: baseline_v3 重建时列对账基于 PO @TableField, 漏了少数"仅在 RepositoryImpl/mapper
--   裸 SQL 里引用、PO 不声明"的列。这些只在实际调用对应接口时以 500 暴露。本文件累积回补。
-- 幂等: information_schema 条件化。

SET NAMES utf8mb4;

-- insp_corrective_cases.effectiveness_check_date — CorrectiveCase 列表查询裸 SQL 引用
-- (PO 不声明; 历史 DDL db/migration/V40 定义为 DATE)。缺它导致 GET /inspection/corrective-cases 500。
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'insp_corrective_cases' AND COLUMN_NAME = 'effectiveness_check_date');
SET @s := IF(@c = 0,
    "ALTER TABLE insp_corrective_cases ADD COLUMN effectiveness_check_date DATE NULL COMMENT '效果验证日期' AFTER verification_note",
    "SELECT 'effectiveness_check_date exists' AS msg");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
