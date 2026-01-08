-- ════════════════════════════════════════════════════════════════════════════
-- 重命名旧量化模块表
-- 创建时间: 2025-11-23
-- 说明: 将V1/V2/V3旧表重命名为_deprecated后缀,便于后期彻底删除
-- ════════════════════════════════════════════════════════════════════════════

SET FOREIGN_KEY_CHECKS = 0;

-- ════════════════════════════════════════════════════════════════════════════
-- 第一部分: 重命名V1版本表
-- ════════════════════════════════════════════════════════════════════════════

-- V1核心表
RENAME TABLE quantification_types TO quantification_types_deprecated;
SELECT '✓ quantification_types → quantification_types_deprecated' AS status;

RENAME TABLE quantification_records TO quantification_records_deprecated;
SELECT '✓ quantification_records → quantification_records_deprecated' AS status;

RENAME TABLE quantification_appeals TO quantification_appeals_deprecated;
SELECT '✓ quantification_appeals → quantification_appeals_deprecated' AS status;


-- ════════════════════════════════════════════════════════════════════════════
-- 第二部分: 重命名V2/V3混合表
-- ════════════════════════════════════════════════════════════════════════════

-- V3检查记录表
RENAME TABLE check_records_v3 TO check_records_v3_deprecated;
SELECT '✓ check_records_v3 → check_records_v3_deprecated' AS status;

RENAME TABLE check_record_items_v3 TO check_record_items_v3_deprecated;
SELECT '✓ check_record_items_v3 → check_record_items_v3_deprecated' AS status;

RENAME TABLE check_record_category_stats TO check_record_category_stats_deprecated;
SELECT '✓ check_record_category_stats → check_record_category_stats_deprecated' AS status;

-- 每日检查表
RENAME TABLE daily_checks TO daily_checks_deprecated;
SELECT '✓ daily_checks → daily_checks_deprecated' AS status;

-- V3可见性配置
RENAME TABLE check_record_visibility TO check_record_visibility_deprecated;
SELECT '✓ check_record_visibility → check_record_visibility_deprecated' AS status;


-- ════════════════════════════════════════════════════════════════════════════
-- 第三部分: 重命名评级相关表
-- ════════════════════════════════════════════════════════════════════════════

-- V1评级表
RENAME TABLE rating_configs TO rating_configs_deprecated;
SELECT '✓ rating_configs → rating_configs_deprecated' AS status;

RENAME TABLE rating_levels TO rating_levels_deprecated;
SELECT '✓ rating_levels → rating_levels_deprecated' AS status;

RENAME TABLE rating_results TO rating_results_deprecated;
SELECT '✓ rating_results → rating_results_deprecated' AS status;

RENAME TABLE rating_periods TO rating_periods_deprecated;
SELECT '✓ rating_periods → rating_periods_deprecated' AS status;

RENAME TABLE rating_publicity TO rating_publicity_deprecated;
SELECT '✓ rating_publicity → rating_publicity_deprecated' AS status;

-- V2评级表
RENAME TABLE check_rating_configs TO check_rating_configs_deprecated;
SELECT '✓ check_rating_configs → check_rating_configs_deprecated' AS status;

RENAME TABLE check_rating_levels TO check_rating_levels_deprecated;
SELECT '✓ check_rating_levels → check_rating_levels_deprecated' AS status;

RENAME TABLE check_rating_results TO check_rating_results_deprecated;
SELECT '✓ check_rating_results → check_rating_results_deprecated' AS status;

-- V3评级表
RENAME TABLE check_record_ratings TO check_record_ratings_deprecated;
SELECT '✓ check_record_ratings → check_record_ratings_deprecated' AS status;


-- ════════════════════════════════════════════════════════════════════════════
-- 第四部分: 重命名申诉相关表
-- ════════════════════════════════════════════════════════════════════════════

-- V1申诉表
RENAME TABLE check_appeals TO check_appeals_deprecated;
SELECT '✓ check_appeals → check_appeals_deprecated' AS status;


-- ════════════════════════════════════════════════════════════════════════════
-- 第五部分: 保留的表 (不重命名)
-- ════════════════════════════════════════════════════════════════════════════

-- 以下表保留不重命名:
-- 1. class_weight_configs - 加权配置方案 (V3继续使用)
-- 2. check_templates - 检查模板 (V3继续使用)
-- 3. template_categories - 模板类别关联 (V3继续使用)
-- 4. check_record_class_stats - 班级统计表 (V3重用)
-- 5. check_item_appeals - 申诉表 (V3重用)
-- 6. 所有新创建的V3.0表

SELECT 'ℹ️ 保留 class_weight_configs (加权配置)' AS info;
SELECT 'ℹ️ 保留 check_templates (检查模板)' AS info;
SELECT 'ℹ️ 保留 template_categories (模板类别关联)' AS info;
SELECT 'ℹ️ 保留 check_record_class_stats (V3重用)' AS info;
SELECT 'ℹ️ 保留 check_item_appeals (V3重用)' AS info;


-- ════════════════════════════════════════════════════════════════════════════
-- 第六部分: 统计结果
-- ════════════════════════════════════════════════════════════════════════════

SELECT
    '重命名完成!' AS status,
    '已重命名18张旧表为 _deprecated 后缀' AS message,
    '保留5张表供V3继续使用' AS kept_tables;


SET FOREIGN_KEY_CHECKS = 1;

-- ────────────────────────────────────────────────────────────────────────────
-- 使用说明:
-- ────────────────────────────────────────────────────────────────────────────
-- 1. 本脚本将18张旧表重命名为 _deprecated 后缀
-- 2. 便于后期彻底删除这些表
-- 3. 如需恢复,可反向重命名
-- 4. 执行命令:
--    mysql -u root -p student_management < 007_rename_old_tables.sql
-- 5. 后期确认无问题后，可执行 DROP TABLE xxx_deprecated 删除
-- ────────────────────────────────────────────────────────────────────────────
