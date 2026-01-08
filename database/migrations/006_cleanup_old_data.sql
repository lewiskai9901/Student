-- ════════════════════════════════════════════════════════════════════════════
-- 清空旧量化模块数据
-- 创建时间: 2025-11-23
-- 说明: 清空V1/V2/V3旧表的所有数据，但保留表结构
-- ════════════════════════════════════════════════════════════════════════════

SET FOREIGN_KEY_CHECKS = 0;

-- ════════════════════════════════════════════════════════════════════════════
-- 第一部分: 清空V3检查记录相关表
-- ════════════════════════════════════════════════════════════════════════════

-- 1. 清空检查记录明细
TRUNCATE TABLE check_record_items_v3;
SELECT '✓ 已清空 check_record_items_v3' AS status;

-- 2. 清空类别统计
TRUNCATE TABLE check_record_category_stats;
SELECT '✓ 已清空 check_record_category_stats' AS status;

-- 3. 清空班级统计 (旧版)
-- 注意: check_record_class_stats 已被新版本重用，不清空
-- TRUNCATE TABLE check_record_class_stats;

-- 4. 清空检查记录主表
TRUNCATE TABLE check_records_v3;
SELECT '✓ 已清空 check_records_v3' AS status;

-- 5. 清空每日检查表
TRUNCATE TABLE daily_checks;
SELECT '✓ 已清空 daily_checks' AS status;


-- ════════════════════════════════════════════════════════════════════════════
-- 第二部分: 清空评级相关表
-- ════════════════════════════════════════════════════════════════════════════

-- 6. 清空评级结果 (V3)
TRUNCATE TABLE check_record_ratings;
SELECT '✓ 已清空 check_record_ratings' AS status;

-- 7. 清空评级结果 (V2)
TRUNCATE TABLE check_rating_results;
SELECT '✓ 已清空 check_rating_results' AS status;

-- 8. 清空评级结果 (V1)
TRUNCATE TABLE rating_results;
SELECT '✓ 已清空 rating_results' AS status;

-- 9. 清空公示记录
TRUNCATE TABLE rating_publicity;
SELECT '✓ 已清空 rating_publicity' AS status;


-- ════════════════════════════════════════════════════════════════════════════
-- 第三部分: 清空申诉相关表
-- ════════════════════════════════════════════════════════════════════════════

-- 10. 清空申诉记录 (V1)
TRUNCATE TABLE check_appeals;
SELECT '✓ 已清空 check_appeals' AS status;

-- 11. 清空申诉记录 (V2 - 如果存在的话，可能已被重命名)
-- check_item_appeals 已被新版本重用，暂时不清空
-- TRUNCATE TABLE check_item_appeals;


-- ════════════════════════════════════════════════════════════════════════════
-- 第四部分: 清空配置表 (保留加权配置)
-- ════════════════════════════════════════════════════════════════════════════

-- 12. 清空评级配置 (V1)
TRUNCATE TABLE rating_configs;
SELECT '✓ 已清空 rating_configs' AS status;

-- 13. 清空评级等级 (V1)
TRUNCATE TABLE rating_levels;
SELECT '✓ 已清空 rating_levels' AS status;

-- 14. 清空检查评级等级 (V2)
TRUNCATE TABLE check_rating_levels;
SELECT '✓ 已清空 check_rating_levels' AS status;

-- 15. 清空检查评级配置 (V2)
TRUNCATE TABLE check_rating_configs;
SELECT '✓ 已清空 check_rating_configs' AS status;

-- 16. 清空评级周期
TRUNCATE TABLE rating_periods;
SELECT '✓ 已清空 rating_periods' AS status;

-- 17. 清空可见性配置
TRUNCATE TABLE check_record_visibility;
SELECT '✓ 已清空 check_record_visibility' AS status;


-- ════════════════════════════════════════════════════════════════════════════
-- 第五部分: 保留的数据
-- ════════════════════════════════════════════════════════════════════════════

-- 以下表保留数据:
-- 1. class_weight_configs - 加权配置方案 (保留2条)
-- 2. check_templates - 检查模板 (保留V3新模板)
-- 3. template_categories - 模板类别关联 (保留)

SELECT 'ℹ️ 保留 class_weight_configs (2条加权方案)' AS info;
SELECT 'ℹ️ 保留 check_templates (检查模板)' AS info;
SELECT 'ℹ️ 保留 template_categories (模板类别关联)' AS info;


-- ════════════════════════════════════════════════════════════════════════════
-- 第六部分: 统计清理结果
-- ════════════════════════════════════════════════════════════════════════════

SELECT
    '清理完成!' AS status,
    (SELECT COUNT(*) FROM check_records_v3) AS check_records_v3_count,
    (SELECT COUNT(*) FROM daily_checks) AS daily_checks_count,
    (SELECT COUNT(*) FROM check_record_items_v3) AS items_count,
    (SELECT COUNT(*) FROM check_record_ratings) AS ratings_count,
    (SELECT COUNT(*) FROM class_weight_configs) AS weight_configs_kept;


SET FOREIGN_KEY_CHECKS = 1;

-- ────────────────────────────────────────────────────────────────────────────
-- 使用说明:
-- ────────────────────────────────────────────────────────────────────────────
-- 1. 本脚本清空所有旧的量化数据
-- 2. 保留加权配置方案 (class_weight_configs)
-- 3. 保留检查模板配置 (check_templates, template_categories)
-- 4. 执行命令:
--    mysql -u root -p student_management < 006_cleanup_old_data.sql
-- ────────────────────────────────────────────────────────────────────────────
