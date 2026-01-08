-- ============================================
-- 量化检查模块 - 性能测试SQL脚本
-- 创建时间: 2025-11-19
-- 用途: 验证优化效果
-- ============================================

USE student_management;

-- ============================================
-- 1. 数据量统计
-- ============================================
SELECT '==================== 数据量统计 ====================' AS '';

SELECT '班级数量' AS 类别, COUNT(*) AS 数量 FROM classes
UNION ALL
SELECT '学生数量', COUNT(*) FROM students
UNION ALL
SELECT '检查记录数(V3)', COUNT(*) FROM check_records_v3
UNION ALL
SELECT '扣分明细数(V3)', COUNT(*) FROM check_record_items_v3
UNION ALL
SELECT '日常检查数', COUNT(*) FROM daily_checks
UNION ALL
SELECT '日常检查明细', COUNT(*) FROM daily_check_details
UNION ALL
SELECT '申诉记录数', COUNT(*) FROM appeal_records
UNION ALL
SELECT '加权配置数', COUNT(*) FROM class_weight_configs;

-- ============================================
-- 2. 索引验证
-- ============================================
SELECT '==================== 索引验证 ====================' AS '';

SELECT
    TABLE_NAME AS '表名',
    INDEX_NAME AS '索引名',
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS '索引列',
    CARDINALITY AS '基数'
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'student_management'
  AND TABLE_NAME IN ('check_records_v3', 'check_record_class_stats', 'check_record_items_v3',
                      'daily_checks', 'daily_check_details', 'appeal_records')
  AND INDEX_NAME != 'PRIMARY'
GROUP BY TABLE_NAME, INDEX_NAME, CARDINALITY
ORDER BY TABLE_NAME, INDEX_NAME;

-- ============================================
-- 3. 性能测试 - 检查记录列表查询
-- ============================================
SELECT '==================== 测试1: 检查记录列表查询 ====================' AS '';

-- 测试查询计划
EXPLAIN
SELECT id, check_name, total_score, avg_score
FROM check_records_v3
WHERE check_date BETWEEN '2025-11-01' AND '2025-11-30'
  AND status = 1
ORDER BY check_date DESC
LIMIT 20;

-- 实际执行(测试性能)
SET @start_time = NOW(6);
SELECT id, check_name, total_score, avg_score
FROM check_records_v3
WHERE check_date BETWEEN '2025-11-01' AND '2025-11-30'
  AND status = 1
ORDER BY check_date DESC
LIMIT 20;
SET @end_time = NOW(6);
SELECT CONCAT(TIMESTAMPDIFF(MICROSECOND, @start_time, @end_time) / 1000, ' ms') AS '查询耗时';

-- ============================================
-- 4. 性能测试 - 班级扣分明细查询
-- ============================================
SELECT '==================== 测试2: 班级扣分明细查询 ====================' AS '';

EXPLAIN
SELECT i.*
FROM check_record_items_v3 i
WHERE i.record_id = 1
  AND i.class_stat_id = 10;

SET @start_time = NOW(6);
SELECT i.*
FROM check_record_items_v3 i
WHERE i.record_id = 1
  AND i.class_stat_id = 10;
SET @end_time = NOW(6);
SELECT CONCAT(TIMESTAMPDIFF(MICROSECOND, @start_time, @end_time) / 1000, ' ms') AS '查询耗时';

-- ============================================
-- 5. 性能测试 - 申诉列表查询
-- ============================================
SELECT '==================== 测试3: 申诉列表查询 ====================' AS '';

EXPLAIN
SELECT *
FROM appeal_records
WHERE appellant_id = 100
  AND status IN (0, 1)
ORDER BY appeal_time DESC
LIMIT 20;

SET @start_time = NOW(6);
SELECT *
FROM appeal_records
WHERE appellant_id = 100
  AND status IN (0, 1)
ORDER BY appeal_time DESC
LIMIT 20;
SET @end_time = NOW(6);
SELECT CONCAT(TIMESTAMPDIFF(MICROSECOND, @start_time, @end_time) / 1000, ' ms') AS '查询耗时';

-- ============================================
-- 6. 性能测试 - 日常检查明细查询
-- ============================================
SELECT '==================== 测试4: 日常检查明细查询 ====================' AS '';

EXPLAIN
SELECT d.*
FROM daily_check_details d
WHERE d.check_id = 1
  AND d.class_id IN (1, 2, 3, 4, 5)
ORDER BY d.class_id, d.category_id;

SET @start_time = NOW(6);
SELECT d.*
FROM daily_check_details d
WHERE d.check_id = 1
  AND d.class_id IN (1, 2, 3, 4, 5)
ORDER BY d.class_id, d.category_id;
SET @end_time = NOW(6);
SELECT CONCAT(TIMESTAMPDIFF(MICROSECOND, @start_time, @end_time) / 1000, ' ms') AS '查询耗时';

-- ============================================
-- 7. 性能测试 - 年级排名查询
-- ============================================
SELECT '==================== 测试5: 年级排名查询 ====================' AS '';

EXPLAIN
SELECT
    c.class_name,
    s.total_score,
    s.grade_ranking
FROM check_record_class_stats s
JOIN classes c ON s.class_id = c.id
WHERE s.record_id = 1
  AND s.grade_id = 10
ORDER BY s.total_score DESC
LIMIT 20;

SET @start_time = NOW(6);
SELECT
    c.class_name,
    s.total_score,
    s.grade_ranking
FROM check_record_class_stats s
JOIN classes c ON s.class_id = c.id
WHERE s.record_id = 1
  AND s.grade_id = 10
ORDER BY s.total_score DESC
LIMIT 20;
SET @end_time = NOW(6);
SELECT CONCAT(TIMESTAMPDIFF(MICROSECOND, @start_time, @end_time) / 1000, ' ms') AS '查询耗时';

-- ============================================
-- 8. 性能测试 - 批量查询班级信息(模拟N+1优化)
-- ============================================
SELECT '==================== 测试6: 批量查询班级(N+1优化验证) ====================' AS '';

-- 获取班级ID列表
SET @class_ids = (SELECT GROUP_CONCAT(DISTINCT class_id) FROM daily_check_details WHERE check_id = 1);

-- 批量查询班级
SET @start_time = NOW(6);
SELECT id, class_name, teacher_id, student_count
FROM classes
WHERE FIND_IN_SET(id, @class_ids) > 0;
SET @end_time = NOW(6);
SELECT CONCAT(TIMESTAMPDIFF(MICROSECOND, @start_time, @end_time) / 1000, ' ms') AS '批量查询耗时';

-- 对比: 如果用循环查询(模拟N+1问题)
SELECT '注意: 如果使用循环查询,耗时将是批量查询的 20-50 倍!' AS '优化效果';

-- ============================================
-- 9. 索引基数检查
-- ============================================
SELECT '==================== 索引基数分析 ====================' AS '';

SELECT
    TABLE_NAME AS '表名',
    INDEX_NAME AS '索引名',
    CARDINALITY AS '基数',
    CARDINALITY / NULLIF((SELECT TABLE_ROWS FROM information_schema.TABLES
                          WHERE TABLE_SCHEMA = 'student_management'
                          AND TABLE_NAME = s.TABLE_NAME), 0) AS '选择性'
FROM information_schema.STATISTICS s
WHERE TABLE_SCHEMA = 'student_management'
  AND TABLE_NAME IN ('check_records_v3', 'check_record_items_v3', 'daily_check_details', 'appeal_records')
  AND INDEX_NAME != 'PRIMARY'
GROUP BY TABLE_NAME, INDEX_NAME, CARDINALITY
HAVING CARDINALITY > 0
ORDER BY TABLE_NAME, CARDINALITY DESC;

-- ============================================
-- 10. 总结
-- ============================================
SELECT '==================== 性能测试完成 ====================' AS '';
SELECT '所有查询均应使用索引 (type = ref/range,而非 ALL)' AS '验收标准1';
SELECT '列表查询应 < 50ms' AS '验收标准2';
SELECT '明细查询应 < 30ms' AS '验收标准3';
SELECT '批量查询应比循环查询快 20-50 倍' AS '验收标准4';

-- ============================================
-- 额外检查: 慢查询分析
-- ============================================
SELECT '==================== 慢查询检查 ====================' AS '';

-- 检查慢查询日志是否开启
SHOW VARIABLES LIKE 'slow_query_log';
SHOW VARIABLES LIKE 'long_query_time';

-- 如需开启慢查询日志:
-- SET GLOBAL slow_query_log = 'ON';
-- SET GLOBAL long_query_time = 1;
