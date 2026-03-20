-- =====================================================
-- V39 评分引擎冗余清理
-- 删除已废弃的列和枚举值
-- =====================================================

-- 1. insp_scoring_profiles: 删除废弃列
ALTER TABLE `insp_scoring_profiles`
    DROP COLUMN `base_score`,
    DROP COLUMN `allow_negative`,
    DROP COLUMN `aggregation_method`,
    DROP COLUMN `formula_engine`,
    DROP COLUMN `default_normalization`;

-- 2. insp_score_dimensions: 删除 formula 列
ALTER TABLE `insp_score_dimensions`
    DROP COLUMN `formula`;

-- 3. insp_calculation_rules: 删除 CEILING/FLOOR 类型的规则数据
DELETE FROM `insp_calculation_rules`
    WHERE `rule_type` IN ('CEILING', 'FLOOR')
      AND `deleted` = 0;

-- 4. 更新 rule_type 列注释
ALTER TABLE `insp_calculation_rules`
    MODIFY COLUMN `rule_type` VARCHAR(30) NOT NULL COMMENT 'VETO|PROGRESSIVE|BONUS|CUSTOM';
