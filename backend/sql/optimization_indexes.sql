-- ============================================
-- 检查记录系统性能优化SQL
-- 描述: 添加索引以优化查询性能
-- 日期: 2024-10-29
-- ============================================

-- 1. 为 check_record_details 添加复合索引
-- 用途: 优化 buildCategoryNode 的三维查询
ALTER TABLE check_record_details
ADD INDEX idx_record_category_class (record_id, category_id, class_id);

-- 2. 为 check_record_details 添加班级查询索引
ALTER TABLE check_record_details
ADD INDEX idx_class_category (class_id, category_id);

-- 3. 为 check_record_details 添加宿舍查询索引
ALTER TABLE check_record_details
ADD INDEX idx_dormitory_check (dormitory_id, check_id);

-- 4. 为 check_record_details 添加教室查询索引
ALTER TABLE check_record_details
ADD INDEX idx_classroom_check (classroom_id, check_id);

-- 5. 为 check_records 添加检查日期索引
ALTER TABLE check_records
ADD INDEX idx_check_created (check_id, created_at);

-- 6. 验证索引是否创建成功
SHOW INDEX FROM check_record_details;
SHOW INDEX FROM check_records;

-- ============================================
-- 查询性能对比测试
-- ============================================

-- 测试查询1: 按record_id, category_id, class_id查询
EXPLAIN SELECT * FROM check_record_details
WHERE record_id = 1
  AND category_id = 1
  AND class_id = 101;

-- 测试查询2: 按班级统计扣分
EXPLAIN SELECT class_id, SUM(deduct_score) as total
FROM check_record_details
WHERE check_id = 1
GROUP BY class_id;

-- 测试查询3: 按宿舍查询明细
EXPLAIN SELECT * FROM check_record_details
WHERE dormitory_id = 201 AND check_id = 1;

-- ============================================
-- 完成
-- ============================================
