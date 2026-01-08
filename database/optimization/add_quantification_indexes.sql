-- ============================================
-- 量化检查模块 - 性能优化索引脚本
-- 创建时间: 2025-11-19
-- 优化目标: 提升查询性能5-10倍
-- ============================================

USE student_management;

-- ============================================
-- 1. check_records_v3 主表索引优化
-- ============================================

-- 组合索引: 日期+状态 (最常用的查询组合)
ALTER TABLE check_records_v3
ADD INDEX idx_date_status (check_date, status)
COMMENT '日期状态组合索引-用于列表查询';

-- 组合索引: 检查员+日期 (按检查员查询历史记录)
ALTER TABLE check_records_v3
ADD INDEX idx_checker_date (checker_id, check_date DESC)
COMMENT '检查员日期索引-用于个人历史查询';

-- 单列索引: 发布时间 (用于时间范围查询)
ALTER TABLE check_records_v3
ADD INDEX idx_publish_time (publish_time)
COMMENT '发布时间索引-用于时间范围筛选';

-- 覆盖索引: 列表页常用字段 (避免回表查询)
ALTER TABLE check_records_v3
ADD INDEX idx_list_cover (check_date, status, id, check_name, total_score, avg_score)
COMMENT '列表页覆盖索引-包含常用显示字段';

-- ============================================
-- 2. check_record_class_stats 班级统计表索引
-- ============================================

-- 组合索引: 记录ID+班级ID (最常用的关联查询)
ALTER TABLE check_record_class_stats
ADD INDEX idx_record_class (record_id, class_id)
COMMENT '记录班级组合索引-用于关联查询';

-- 组合索引: 班级ID+日期 (查询某班级的历史记录，需要JOIN主表)
-- 注意: 这需要先添加check_date字段或通过主表JOIN
ALTER TABLE check_record_class_stats
ADD INDEX idx_class_record (class_id, record_id)
COMMENT '班级记录索引-用于班级历史查询';

-- 组合索引: 年级ID+分数 (年级排名查询)
ALTER TABLE check_record_class_stats
ADD INDEX idx_grade_score (grade_id, total_score DESC)
COMMENT '年级分数索引-用于年级排名';

-- ============================================
-- 3. check_record_category_stats 类别统计表索引
-- ============================================

-- 组合索引: 记录ID+类别ID
ALTER TABLE check_record_category_stats
ADD INDEX idx_record_category (record_id, category_id)
COMMENT '记录类别索引-用于类别统计查询';

-- 组合索引: 记录ID+班级ID+类别ID (最常用的查询)
ALTER TABLE check_record_category_stats
ADD INDEX idx_record_class_category (record_id, class_id, category_id)
COMMENT '记录班级类别索引-用于明细查询';

-- ============================================
-- 4. check_record_items_v3 扣分明细表索引
-- ============================================

-- 组合索引: 记录ID+班级统计ID (最常用的关联查询)
ALTER TABLE check_record_items_v3
ADD INDEX idx_record_class_stat (record_id, class_stat_id)
COMMENT '记录班级统计索引-用于明细查询';

-- 组合索引: 类别ID+班级统计ID (按类别查询明细)
ALTER TABLE check_record_items_v3
ADD INDEX idx_category_class (category_id, class_stat_id)
COMMENT '类别班级索引-用于类别明细查询';

-- 组合索引: 申诉状态+申诉ID (申诉查询)
ALTER TABLE check_record_items_v3
ADD INDEX idx_appeal (appeal_status, appeal_id)
COMMENT '申诉状态索引-用于申诉查询';

-- 组合索引: 关联类型+关联ID (宿舍/教室查询)
ALTER TABLE check_record_items_v3
ADD INDEX idx_link (link_type, link_id)
COMMENT '关联资源索引-用于宿舍教室查询';

-- 组合索引: 扣分项ID (按扣分项统计)
ALTER TABLE check_record_items_v3
ADD INDEX idx_item (item_id, deduct_score)
COMMENT '扣分项索引-用于扣分项统计';

-- ============================================
-- 5. daily_checks V2日常检查表索引
-- ============================================

-- 组合索引: 检查日期+类型
ALTER TABLE daily_checks
ADD INDEX idx_date_type (check_date, check_type)
COMMENT '日期类型索引-用于日常检查查询';

-- 单列索引: 检查员ID
ALTER TABLE daily_checks
ADD INDEX idx_checker (checker_id)
COMMENT '检查员索引-用于检查员历史';

-- 组合索引: 加权配置ID (用于加权查询)
ALTER TABLE daily_checks
ADD INDEX idx_weight_config (enable_weight, weight_config_id)
COMMENT '加权配置索引-用于加权查询';

-- ============================================
-- 6. daily_check_details V2扣分明细表索引
-- ============================================

-- 组合索引: 检查ID+班级ID (最常用查询)
ALTER TABLE daily_check_details
ADD INDEX idx_check_class (check_id, class_id)
COMMENT '检查班级索引-用于扣分明细查询';

-- 单列索引: 类别ID
ALTER TABLE daily_check_details
ADD INDEX idx_category (category_id)
COMMENT '类别索引-用于类别筛选';

-- 组合索引: 扣分项ID
ALTER TABLE daily_check_details
ADD INDEX idx_item_score (item_id, deduct_score)
COMMENT '扣分项索引-用于扣分项统计';

-- ============================================
-- 7. appeal_records 申诉记录表索引
-- ============================================

-- 组合索引: 扣分明细ID
ALTER TABLE appeal_records
ADD INDEX idx_record_item (record_item_id, status)
COMMENT '扣分明细索引-用于申诉关联查询';

-- 组合索引: 申请人ID+状态
ALTER TABLE appeal_records
ADD INDEX idx_applicant_status (applicant_id, status, created_at DESC)
COMMENT '申请人状态索引-用于个人申诉列表';

-- 组合索引: 状态+创建时间 (待审核列表)
ALTER TABLE appeal_records
ADD INDEX idx_status_time (status, created_at DESC)
COMMENT '状态时间索引-用于待办列表';

-- ============================================
-- 8. check_rating_results 评级结果表索引
-- ============================================

-- 组合索引: 检查记录ID
ALTER TABLE check_rating_results
ADD INDEX idx_record_config (record_id, rating_config_id)
COMMENT '记录配置索引-用于评级查询';

-- 组合索引: 评级等级 (统计查询)
ALTER TABLE check_rating_results
ADD INDEX idx_rating_level (rating_level_id, record_id)
COMMENT '评级等级索引-用于评级统计';

-- ============================================
-- 验证索引创建
-- ============================================

-- 查看 check_records_v3 的所有索引
SELECT
    TABLE_NAME,
    INDEX_NAME,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS COLUMNS,
    INDEX_TYPE,
    INDEX_COMMENT
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'student_management'
  AND TABLE_NAME = 'check_records_v3'
GROUP BY TABLE_NAME, INDEX_NAME, INDEX_TYPE, INDEX_COMMENT
ORDER BY TABLE_NAME, INDEX_NAME;

-- ============================================
-- 性能测试 SQL
-- ============================================

-- 测试1: 日期范围查询 (应使用 idx_date_status)
EXPLAIN SELECT id, check_name, total_score, avg_score
FROM check_records_v3
WHERE check_date BETWEEN '2025-11-01' AND '2025-11-30'
  AND status = 1
ORDER BY check_date DESC;

-- 测试2: 检查员历史查询 (应使用 idx_checker_date)
EXPLAIN SELECT id, check_name, check_date, total_score
FROM check_records_v3
WHERE checker_id = 1
  AND check_date >= '2025-11-01'
ORDER BY check_date DESC;

-- 测试3: 班级扣分明细查询 (应使用 idx_record_class_stat)
EXPLAIN SELECT i.*
FROM check_record_items_v3 i
WHERE i.record_id = 1
  AND i.class_stat_id = 10;

-- 测试4: 申诉列表查询 (应使用 idx_applicant_status)
EXPLAIN SELECT *
FROM appeal_records
WHERE applicant_id = 100
  AND status IN (0, 1)
ORDER BY created_at DESC;

-- ============================================
-- 索引维护建议
-- ============================================

-- 1. 定期分析表 (每月执行)
ANALYZE TABLE check_records_v3;
ANALYZE TABLE check_record_class_stats;
ANALYZE TABLE check_record_items_v3;

-- 2. 检查索引使用情况
SELECT
    TABLE_NAME,
    INDEX_NAME,
    INDEX_TYPE,
    CARDINALITY
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'student_management'
  AND TABLE_NAME LIKE 'check%'
ORDER BY TABLE_NAME, INDEX_NAME;

-- 3. 检查慢查询 (需要开启慢查询日志)
-- SET GLOBAL slow_query_log = 'ON';
-- SET GLOBAL long_query_time = 1;

-- ============================================
-- 回滚脚本 (如需删除索引)
-- ============================================

/*
-- 删除 check_records_v3 索引
ALTER TABLE check_records_v3 DROP INDEX idx_date_status;
ALTER TABLE check_records_v3 DROP INDEX idx_checker_date;
ALTER TABLE check_records_v3 DROP INDEX idx_publish_time;
ALTER TABLE check_records_v3 DROP INDEX idx_list_cover;

-- 删除其他表索引...
-- (按需添加)
*/

-- ============================================
-- 执行说明
-- ============================================

/*
1. 在生产环境执行前请先备份数据库
2. 建议在业务低峰期执行（凌晨2-4点）
3. 大表添加索引可能需要较长时间，请耐心等待
4. 执行后使用 EXPLAIN 验证索引是否生效
5. 监控执行后的查询性能变化

预期效果:
- 列表查询: 响应时间从 200ms 降至 30ms (提升 6-7倍)
- 明细查询: 响应时间从 150ms 降至 20ms (提升 7-8倍)
- 统计查询: 响应时间从 500ms 降至 80ms (提升 6倍)
*/
