-- 检查模块测试数据
-- 添加检查分类、检查计划、检查记录

SET NAMES utf8mb4;

-- ==============================
-- 1. 添加检查分类
-- ==============================
INSERT INTO check_categories (id, category_code, category_name, category_type, default_max_score, description, sort_order, status, deleted) VALUES
(1, 'CAT-HEALTH', '卫生检查', 'CLASS', 100.00, '班级卫生检查分类', 1, 1, 0),
(2, 'CAT-DISCIPLINE', '纪律检查', 'CLASS', 100.00, '班级纪律检查分类', 2, 1, 0),
(3, 'CAT-SAFETY', '安全检查', 'DORMITORY', 100.00, '宿舍安全检查分类', 3, 1, 0),
(4, 'CAT-ATTENDANCE', '考勤检查', 'CLASS', 100.00, '班级考勤检查分类', 4, 1, 0)
ON DUPLICATE KEY UPDATE category_name = VALUES(category_name);

-- ==============================
-- 2. 更新检查项目关联分类
-- ==============================
UPDATE check_items SET category_id = 1 WHERE category_id = 0 OR category_id IS NULL;

-- ==============================
-- 3. 添加检查计划
-- ==============================
INSERT INTO check_plans (id, plan_code, plan_name, description, template_id, template_name, template_snapshot, start_date, end_date, status, total_checks, total_records, deleted) VALUES
(2016500000000000001, 'PLAN-2026-001', '2026年春季学期日常检查计划', '2026年春季学期班级日常卫生检查', 2016389395661209601, 'DailyCheckTemplate', '{}', '2026-01-01', '2026-06-30', 1, 0, 0, 0),
(2016500000000000002, 'PLAN-2026-002', '2026年春季宿舍安全检查计划', '每周宿舍安全检查', 2016389395661209601, 'DailyCheckTemplate', '{}', '2026-01-01', '2026-06-30', 1, 0, 0, 0),
(2016500000000000003, 'PLAN-2026-003', '1月份班级纪律检查', '1月份班级纪律专项检查', 2016389395661209601, 'DailyCheckTemplate', '{}', '2026-01-01', '2026-01-31', 1, 0, 0, 0)
ON DUPLICATE KEY UPDATE plan_name = VALUES(plan_name);

-- ==============================
-- 4. 添加检查记录
-- ==============================
INSERT INTO check_records (id, plan_id, record_code, check_name, check_date, check_type, checker_id, checker_name, total_classes, total_score, avg_score, status, check_time, deleted) VALUES
(2016600000000000001, 2016500000000000001, 'REC-20260128-001', '1月28日卫生检查', '2026-01-28', 1, 1, '王浩宇', 2, 185.00, 92.50, 2, '2026-01-28 09:00:00', 0),
(2016600000000000002, 2016500000000000001, 'REC-20260127-001', '1月27日卫生检查', '2026-01-27', 1, 1, '王浩宇', 2, 178.00, 89.00, 2, '2026-01-27 09:00:00', 0),
(2016600000000000003, 2016500000000000001, 'REC-20260126-001', '1月26日卫生检查', '2026-01-26', 1, 1, '王浩宇', 2, 190.00, 95.00, 2, '2026-01-26 09:00:00', 0),
(2016600000000000004, 2016500000000000002, 'REC-20260128-002', '1月28日宿舍检查', '2026-01-28', 2, 1, '王浩宇', 3, 270.00, 90.00, 2, '2026-01-28 14:00:00', 0),
(2016600000000000005, 2016500000000000003, 'REC-20260129-001', '1月29日纪律检查', CURDATE(), 1, 1, '王浩宇', 2, 0.00, 0.00, 0, NOW(), 0)
ON DUPLICATE KEY UPDATE check_name = VALUES(check_name);

-- ==============================
-- 5. 添加检查记录班级统计
-- ==============================
INSERT INTO check_record_class_stats (id, record_id, class_id, class_name, grade_name, total_items, total_score, hygiene_score, discipline_score, final_score, ranking, deduction_count, created_at) VALUES
(1, 2016600000000000001, 1, '计算机2024-1班', '2024级', 10, 95.00, 95.00, 0.00, 95.00, 1, 2, NOW()),
(2, 2016600000000000001, 2, '计算机2024-2班', '2024级', 10, 90.00, 90.00, 0.00, 90.00, 2, 3, NOW()),
(3, 2016600000000000002, 1, '计算机2024-1班', '2024级', 10, 92.00, 92.00, 0.00, 92.00, 1, 2, NOW()),
(4, 2016600000000000002, 2, '计算机2024-2班', '2024级', 10, 86.00, 86.00, 0.00, 86.00, 2, 4, NOW()),
(5, 2016600000000000003, 1, '计算机2024-1班', '2024级', 10, 98.00, 98.00, 0.00, 98.00, 1, 1, NOW()),
(6, 2016600000000000003, 2, '计算机2024-2班', '2024级', 10, 92.00, 92.00, 0.00, 92.00, 2, 2, NOW())
ON DUPLICATE KEY UPDATE total_score = VALUES(total_score);

-- ==============================
-- 6. 添加检查记录分类统计
-- ==============================
INSERT INTO check_record_category_stats (id, record_id, class_stat_id, class_id, category_id, category_code, category_name, check_round, deduction_count, item_count, total_score, created_at) VALUES
(1, 2016600000000000001, 1, 1, 1, 'CAT-HEALTH', '卫生检查', 1, 2, 5, 95.00, NOW()),
(2, 2016600000000000001, 2, 2, 1, 'CAT-HEALTH', '卫生检查', 1, 3, 5, 90.00, NOW()),
(3, 2016600000000000002, 3, 1, 1, 'CAT-HEALTH', '卫生检查', 1, 2, 5, 92.00, NOW()),
(4, 2016600000000000002, 4, 2, 1, 'CAT-HEALTH', '卫生检查', 1, 4, 5, 86.00, NOW()),
(5, 2016600000000000003, 5, 1, 1, 'CAT-HEALTH', '卫生检查', 1, 1, 5, 98.00, NOW()),
(6, 2016600000000000003, 6, 2, 1, 'CAT-HEALTH', '卫生检查', 1, 2, 5, 92.00, NOW())
ON DUPLICATE KEY UPDATE total_score = VALUES(total_score);

-- ==============================
-- 7. 更新检查计划统计
-- ==============================
UPDATE check_plans SET total_records = 3, total_checks = 3 WHERE id = 2016500000000000001;
UPDATE check_plans SET total_records = 1, total_checks = 1 WHERE id = 2016500000000000002;
UPDATE check_plans SET total_records = 1, total_checks = 1 WHERE id = 2016500000000000003;

SELECT 'Inspection test data added successfully!' as result;
