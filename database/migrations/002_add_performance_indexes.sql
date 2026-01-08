-- ====================================================================
-- 添加性能优化索引
--
-- 目的: 优化量化功能模块的查询性能
-- 作者: Claude Code
-- 日期: 2025-11-23
-- 优先级: 🟡 中 - 性能优化
-- ====================================================================

-- 1. check_records_v3 表索引优化
-- 说明: 优化检查记录的常见查询场景

-- 检查日期和状态组合索引 (用于列表查询和筛选)
CREATE INDEX IF NOT EXISTS idx_check_date
ON check_records_v3(check_date, status);

-- 检查人和日期组合索引 (用于"我的检查记录"查询)
CREATE INDEX IF NOT EXISTS idx_checker
ON check_records_v3(checker_id, check_date);

-- 发布时间索引 (用于已发布记录的时间范围查询)
CREATE INDEX IF NOT EXISTS idx_publish_time
ON check_records_v3(publish_time);

-- 权重配置ID索引 (用于按配置筛选)
CREATE INDEX IF NOT EXISTS idx_weight_config
ON check_records_v3(weight_config_id);

-- 逻辑删除和状态组合索引 (用于过滤已删除记录)
CREATE INDEX IF NOT EXISTS idx_deleted_status
ON check_records_v3(deleted, status);


-- 2. check_record_class_stats 表索引优化
-- 说明: 优化班级统计数据的查询性能

-- 检查记录ID和班级ID组合索引 (用于获取特定记录的班级数据)
CREATE INDEX IF NOT EXISTS idx_record_class
ON check_record_class_stats(record_id, class_id);

-- 排名索引 (用于排行榜查询)
CREATE INDEX IF NOT EXISTS idx_ranking
ON check_record_class_stats(record_id, ranking);

-- 班级ID索引 (用于查询某个班级的所有检查记录)
CREATE INDEX IF NOT EXISTS idx_class_id
ON check_record_class_stats(class_id);

-- 系部ID索引 (用于按系部统计)
CREATE INDEX IF NOT EXISTS idx_department
ON check_record_class_stats(department_id);


-- 3. check_record_items_v3 表索引优化
-- 说明: 优化扣分明细的查询性能

-- 班级统计ID索引 (用于获取某个班级的所有扣分项)
CREATE INDEX IF NOT EXISTS idx_class_stat
ON check_record_items_v3(class_stat_id);

-- 分类ID和班级统计ID组合索引 (用于按类别筛选扣分项)
CREATE INDEX IF NOT EXISTS idx_category
ON check_record_items_v3(category_id, class_stat_id);

-- 申诉状态索引 (用于查询待处理/已处理的申诉)
CREATE INDEX IF NOT EXISTS idx_appeal
ON check_record_items_v3(appeal_status, class_stat_id);

-- 逻辑删除索引
CREATE INDEX IF NOT EXISTS idx_deleted
ON check_record_items_v3(deleted);


-- 4. class_weight_configs 表索引优化
-- 说明: 优化加权配置的查询

-- 状态和删除标记组合索引 (用于获取可用配置)
CREATE INDEX IF NOT EXISTS idx_status_deleted
ON class_weight_configs(status, deleted);

-- 默认配置索引
CREATE INDEX IF NOT EXISTS idx_is_default
ON class_weight_configs(is_default);


-- 5. 验证索引创建结果
SELECT
    TABLE_NAME,
    INDEX_NAME,
    COLUMN_NAME,
    SEQ_IN_INDEX,
    INDEX_TYPE
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = 'student_management'
  AND TABLE_NAME IN (
    'check_records_v3',
    'check_record_class_stats',
    'check_record_items_v3',
    'class_weight_configs'
  )
  AND INDEX_NAME LIKE 'idx_%'
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;


-- ====================================================================
-- 执行说明:
-- 1. 此脚本使用 CREATE INDEX IF NOT EXISTS 语法，可重复执行
-- 2. 执行完成后会显示所有新增索引的信息
-- 3. 如果索引已存在，将跳过创建
-- 4. 建议在低峰期执行，避免影响业务
-- ====================================================================

-- 性能测试建议:
-- 执行前后对比以下查询的执行计划和时间:
--
-- EXPLAIN SELECT * FROM check_records_v3
-- WHERE check_date BETWEEN '2024-01-01' AND '2024-12-31'
-- AND status = 'PUBLISHED' ORDER BY check_date DESC;
--
-- EXPLAIN SELECT * FROM check_record_class_stats
-- WHERE record_id = 1 ORDER BY ranking;
--
-- EXPLAIN SELECT * FROM check_record_items_v3
-- WHERE class_stat_id = 1 AND category_id = 1;

-- ====================================================================
-- 回滚脚本 (如果需要)
-- DROP INDEX IF EXISTS idx_check_date ON check_records_v3;
-- DROP INDEX IF EXISTS idx_checker ON check_records_v3;
-- DROP INDEX IF EXISTS idx_publish_time ON check_records_v3;
-- DROP INDEX IF EXISTS idx_weight_config ON check_records_v3;
-- DROP INDEX IF EXISTS idx_deleted_status ON check_records_v3;
-- DROP INDEX IF EXISTS idx_record_class ON check_record_class_stats;
-- DROP INDEX IF EXISTS idx_ranking ON check_record_class_stats;
-- DROP INDEX IF EXISTS idx_class_id ON check_record_class_stats;
-- DROP INDEX IF EXISTS idx_department ON check_record_class_stats;
-- DROP INDEX IF EXISTS idx_class_stat ON check_record_items_v3;
-- DROP INDEX IF EXISTS idx_category ON check_record_items_v3;
-- DROP INDEX IF EXISTS idx_appeal ON check_record_items_v3;
-- DROP INDEX IF EXISTS idx_deleted ON check_record_items_v3;
-- DROP INDEX IF EXISTS idx_status_deleted ON class_weight_configs;
-- DROP INDEX IF EXISTS idx_is_default ON class_weight_configs;
-- ====================================================================
