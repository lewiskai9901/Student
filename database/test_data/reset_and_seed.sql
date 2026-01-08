-- =====================================================
-- 学生管理系统测试数据重置脚本
-- 清空并重新插入模拟学校数据
-- =====================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. 清空相关表数据 (按外键依赖顺序)
DELETE FROM students WHERE id > 0;
DELETE FROM classes WHERE id > 0;
DELETE FROM grade_major_directions WHERE id > 0;
DELETE FROM major_directions WHERE id > 0;
DELETE FROM majors WHERE id > 0;
DELETE FROM grades WHERE id > 0;
DELETE FROM departments WHERE id > 0;
-- 清理测试学生用户 (user_type = 3 或 ID范围1001-1100)
DELETE FROM users WHERE user_type = 3 OR (id >= 1001 AND id <= 1100);

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================
-- 2. 插入部门/系部数据
-- =====================================================
INSERT INTO departments (id, dept_name, dept_code, dept_desc, parent_id, dept_level, dept_path, sort_order, status, created_at, updated_at) VALUES
(1, '学校', 'SCHOOL', '学校本部', NULL, 1, '/1/', 1, 1, NOW(), NOW()),
(2, '信息系', 'IT', '信息技术系', 1, 2, '/1/2/', 1, 1, NOW(), NOW()),
(3, '机械系', 'ME', '机械工程系', 1, 2, '/1/3/', 2, 1, NOW(), NOW()),
(4, '商务系', 'BM', '商务管理系', 1, 2, '/1/4/', 3, 1, NOW(), NOW()),
(5, '艺术系', 'AD', '艺术设计系', 1, 2, '/1/5/', 4, 1, NOW(), NOW());

-- =====================================================
-- 3. 插入年级数据
-- =====================================================
INSERT INTO grades (id, grade_name, grade_code, enrollment_year, graduation_year, total_classes, total_students, standard_class_size, status, created_at, updated_at) VALUES
(1, '2022级', 'G2022', 2022, 2025, 0, 0, 45, 1, NOW(), NOW()),
(2, '2023级', 'G2023', 2023, 2026, 0, 0, 45, 1, NOW(), NOW()),
(3, '2024级', 'G2024', 2024, 2027, 0, 0, 45, 1, NOW(), NOW()),
(4, '2025级', 'G2025', 2025, 2028, 0, 0, 45, 1, NOW(), NOW());

-- =====================================================
-- 4. 插入专业数据
-- =====================================================
INSERT INTO majors (id, major_name, major_code, department_id, description, status, created_at, updated_at) VALUES
(1, '计算机应用', 'CS', 2, '计算机软件开发', 1, NOW(), NOW()),
(2, '网络技术', 'NT', 2, '网络工程', 1, NOW(), NOW()),
(3, '电子商务', 'EC', 2, '电商运营', 1, NOW(), NOW()),
(4, '数控技术', 'CNC', 3, '数控加工', 1, NOW(), NOW()),
(5, '机电一体化', 'MT', 3, '机电设备', 1, NOW(), NOW()),
(6, '汽车维修', 'AR', 3, '汽车检修', 1, NOW(), NOW()),
(7, '会计', 'ACC', 4, '财务会计', 1, NOW(), NOW()),
(8, '市场营销', 'MKT', 4, '市场策划', 1, NOW(), NOW()),
(9, '室内设计', 'ID', 5, '室内装饰', 1, NOW(), NOW()),
(10, '平面设计', 'GD', 5, '平面广告', 1, NOW(), NOW());

-- =====================================================
-- 5. 插入专业方向数据
-- =====================================================
INSERT INTO major_directions (id, major_id, direction_name, direction_code, level, years, is_segmented, phase1_level, phase1_years, phase2_level, phase2_years, remarks, created_at, updated_at) VALUES
(1, 1, '3年制中级', 'CS_3Y', '中级工', 3, 0, NULL, NULL, NULL, NULL, '初中起点', NOW(), NOW()),
(2, 1, '3+2高级', 'CS_3P2', '高级工', 5, 1, '中级工', 3, '高级工', 2, '分段培养', NOW(), NOW()),
(3, 1, '5年技师', 'CS_5Y', '技师', 5, 0, NULL, NULL, NULL, NULL, '一贯制', NOW(), NOW()),
(4, 2, '3年制中级', 'NT_3Y', '中级工', 3, 0, NULL, NULL, NULL, NULL, '初中起点', NOW(), NOW()),
(5, 2, '3+2高级', 'NT_3P2', '高级工', 5, 1, '中级工', 3, '高级工', 2, '分段培养', NOW(), NOW()),
(6, 3, '3年制中级', 'EC_3Y', '中级工', 3, 0, NULL, NULL, NULL, NULL, '初中起点', NOW(), NOW()),
(7, 4, '3年制中级', 'CNC_3Y', '中级工', 3, 0, NULL, NULL, NULL, NULL, '初中起点', NOW(), NOW()),
(8, 4, '3+2高级', 'CNC_3P2', '高级工', 5, 1, '中级工', 3, '高级工', 2, '分段培养', NOW(), NOW()),
(9, 5, '3年制中级', 'MT_3Y', '中级工', 3, 0, NULL, NULL, NULL, NULL, '初中起点', NOW(), NOW()),
(10, 5, '5年技师', 'MT_5Y', '技师', 5, 0, NULL, NULL, NULL, NULL, '一贯制', NOW(), NOW()),
(11, 6, '3年制中级', 'AR_3Y', '中级工', 3, 0, NULL, NULL, NULL, NULL, '初中起点', NOW(), NOW()),
(12, 7, '3年制中级', 'ACC_3Y', '中级工', 3, 0, NULL, NULL, NULL, NULL, '初中起点', NOW(), NOW()),
(13, 8, '3年制中级', 'MKT_3Y', '中级工', 3, 0, NULL, NULL, NULL, NULL, '初中起点', NOW(), NOW()),
(14, 9, '3年制中级', 'ID_3Y', '中级工', 3, 0, NULL, NULL, NULL, NULL, '初中起点', NOW(), NOW()),
(15, 9, '3+2高级', 'ID_3P2', '高级工', 5, 1, '中级工', 3, '高级工', 2, '分段培养', NOW(), NOW()),
(16, 10, '3年制中级', 'GD_3Y', '中级工', 3, 0, NULL, NULL, NULL, NULL, '初中起点', NOW(), NOW());

-- =====================================================
-- 6. 插入年级开设专业方向数据
-- =====================================================
INSERT INTO grade_major_directions (id, academic_year, major_direction_id, remarks, created_at, updated_at) VALUES
(1, 2024, 1, '2024级', NOW(), NOW()),
(2, 2024, 2, '2024级', NOW(), NOW()),
(3, 2024, 4, '2024级', NOW(), NOW()),
(4, 2024, 7, '2024级', NOW(), NOW()),
(5, 2024, 9, '2024级', NOW(), NOW()),
(6, 2024, 12, '2024级', NOW(), NOW()),
(7, 2023, 1, '2023级', NOW(), NOW()),
(8, 2023, 2, '2023级', NOW(), NOW()),
(9, 2023, 5, '2023级', NOW(), NOW()),
(10, 2023, 7, '2023级', NOW(), NOW()),
(11, 2022, 1, '2022级', NOW(), NOW()),
(12, 2022, 4, '2022级', NOW(), NOW()),
(13, 2022, 11, '2022级', NOW(), NOW());

-- =====================================================
-- 7. 插入班级数据
-- =====================================================
INSERT INTO classes (id, class_name, class_code, grade_level, department_id, grade_id, major_id, major_direction_id, teacher_id, student_count, enrollment_year, graduation_year, status, created_at, updated_at) VALUES
-- 2024级班级
(1, '计算机241班', 'CS2401', 1, 2, 3, 1, 1, NULL, 0, 2024, 2027, 1, NOW(), NOW()),
(2, '计算机242班', 'CS2402', 1, 2, 3, 1, 1, NULL, 0, 2024, 2027, 1, NOW(), NOW()),
(3, '计算机3+2班', 'CS24P2', 1, 2, 3, 1, 2, NULL, 0, 2024, 2029, 1, NOW(), NOW()),
(4, '网络241班', 'NT2401', 1, 2, 3, 2, 4, NULL, 0, 2024, 2027, 1, NOW(), NOW()),
(5, '数控241班', 'CNC2401', 1, 3, 3, 4, 7, NULL, 0, 2024, 2027, 1, NOW(), NOW()),
(6, '机电241班', 'MT2401', 1, 3, 3, 5, 9, NULL, 0, 2024, 2027, 1, NOW(), NOW()),
(7, '会计241班', 'ACC2401', 1, 4, 3, 7, 12, NULL, 0, 2024, 2027, 1, NOW(), NOW()),
-- 2023级班级
(8, '计算机231班', 'CS2301', 2, 2, 2, 1, 1, NULL, 0, 2023, 2026, 1, NOW(), NOW()),
(9, '计算机3+2班', 'CS23P2', 2, 2, 2, 1, 2, NULL, 0, 2023, 2028, 1, NOW(), NOW()),
(10, '网络3+2班', 'NT23P2', 2, 2, 2, 2, 5, NULL, 0, 2023, 2028, 1, NOW(), NOW()),
(11, '数控231班', 'CNC2301', 2, 3, 2, 4, 7, NULL, 0, 2023, 2026, 1, NOW(), NOW()),
-- 2022级班级
(12, '计算机221班', 'CS2201', 3, 2, 1, 1, 1, NULL, 0, 2022, 2025, 1, NOW(), NOW()),
(13, '网络221班', 'NT2201', 3, 2, 1, 2, 4, NULL, 0, 2022, 2025, 1, NOW(), NOW()),
(14, '汽修221班', 'AR2201', 3, 3, 1, 6, 11, NULL, 0, 2022, 2025, 1, NOW(), NOW());

-- =====================================================
-- 8. 插入学生用户和学生数据
-- 默认密码: 123456 (BCrypt加密)
-- =====================================================
SET @pwd = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi';

-- 创建学生用户
INSERT INTO users (id, username, password, real_name, phone, gender, user_type, department_id, class_id, status, created_at, updated_at) VALUES
-- 2024级计算机241班
(1001, 'S2024010101', @pwd, '张三', '13800001001', 1, 3, 2, 1, 1, NOW(), NOW()),
(1002, 'S2024010102', @pwd, '李四', '13800001002', 1, 3, 2, 1, 1, NOW(), NOW()),
(1003, 'S2024010103', @pwd, '王五', '13800001003', 0, 3, 2, 1, 1, NOW(), NOW()),
(1004, 'S2024010104', @pwd, '赵六', '13800001004', 1, 3, 2, 1, 1, NOW(), NOW()),
-- 2024级计算机242班
(1005, 'S2024010201', @pwd, '钱七', '13800001005', 0, 3, 2, 2, 1, NOW(), NOW()),
(1006, 'S2024010202', @pwd, '孙八', '13800001006', 1, 3, 2, 2, 1, NOW(), NOW()),
(1007, 'S2024010203', @pwd, '周九', '13800001007', 1, 3, 2, 2, 1, NOW(), NOW()),
-- 2024级计算机3+2班
(1008, 'S2024010301', @pwd, '吴十', '13800001008', 1, 3, 2, 3, 1, NOW(), NOW()),
(1009, 'S2024010302', @pwd, '郑一', '13800001009', 0, 3, 2, 3, 1, NOW(), NOW()),
-- 2024级网络241班
(1010, 'S2024020101', @pwd, '冯二', '13800001010', 1, 3, 2, 4, 1, NOW(), NOW()),
(1011, 'S2024020102', @pwd, '陈三', '13800001011', 0, 3, 2, 4, 1, NOW(), NOW()),
-- 2024级数控241班
(1012, 'S2024030101', @pwd, '褚四', '13800001012', 1, 3, 3, 5, 1, NOW(), NOW()),
(1013, 'S2024030102', @pwd, '卫五', '13800001013', 1, 3, 3, 5, 1, NOW(), NOW()),
-- 2024级会计241班
(1014, 'S2024040101', @pwd, '蒋六', '13800001014', 0, 3, 4, 7, 1, NOW(), NOW()),
(1015, 'S2024040102', @pwd, '沈七', '13800001015', 0, 3, 4, 7, 1, NOW(), NOW()),
-- 2023级计算机231班
(1016, 'S2023010101', @pwd, '韩八', '13800001016', 1, 3, 2, 8, 1, NOW(), NOW()),
(1017, 'S2023010102', @pwd, '杨九', '13800001017', 0, 3, 2, 8, 1, NOW(), NOW()),
(1018, 'S2023010103', @pwd, '朱十', '13800001018', 1, 3, 2, 8, 1, NOW(), NOW()),
-- 2022级计算机221班
(1019, 'S2022010101', @pwd, '秦一一', '13800001019', 1, 3, 2, 12, 1, NOW(), NOW()),
(1020, 'S2022010102', @pwd, '尤一二', '13800001020', 0, 3, 2, 12, 1, NOW(), NOW());

-- 创建学生记录
INSERT INTO students (id, student_no, user_id, class_id, grade_id, major_id, major_direction_id, admission_date, student_status, created_at, updated_at) VALUES
-- 2024级计算机241班
(1, '2024010101', 1001, 1, 3, 1, 1, '2024-09-01', 1, NOW(), NOW()),
(2, '2024010102', 1002, 1, 3, 1, 1, '2024-09-01', 1, NOW(), NOW()),
(3, '2024010103', 1003, 1, 3, 1, 1, '2024-09-01', 1, NOW(), NOW()),
(4, '2024010104', 1004, 1, 3, 1, 1, '2024-09-01', 1, NOW(), NOW()),
-- 2024级计算机242班
(5, '2024010201', 1005, 2, 3, 1, 1, '2024-09-01', 1, NOW(), NOW()),
(6, '2024010202', 1006, 2, 3, 1, 1, '2024-09-01', 1, NOW(), NOW()),
(7, '2024010203', 1007, 2, 3, 1, 1, '2024-09-01', 1, NOW(), NOW()),
-- 2024级计算机3+2班
(8, '2024010301', 1008, 3, 3, 1, 2, '2024-09-01', 1, NOW(), NOW()),
(9, '2024010302', 1009, 3, 3, 1, 2, '2024-09-01', 1, NOW(), NOW()),
-- 2024级网络241班
(10, '2024020101', 1010, 4, 3, 2, 4, '2024-09-01', 1, NOW(), NOW()),
(11, '2024020102', 1011, 4, 3, 2, 4, '2024-09-01', 1, NOW(), NOW()),
-- 2024级数控241班
(12, '2024030101', 1012, 5, 3, 4, 7, '2024-09-01', 1, NOW(), NOW()),
(13, '2024030102', 1013, 5, 3, 4, 7, '2024-09-01', 1, NOW(), NOW()),
-- 2024级会计241班
(14, '2024040101', 1014, 7, 3, 7, 12, '2024-09-01', 1, NOW(), NOW()),
(15, '2024040102', 1015, 7, 3, 7, 12, '2024-09-01', 1, NOW(), NOW()),
-- 2023级计算机231班
(16, '2023010101', 1016, 8, 2, 1, 1, '2023-09-01', 1, NOW(), NOW()),
(17, '2023010102', 1017, 8, 2, 1, 1, '2023-09-01', 1, NOW(), NOW()),
(18, '2023010103', 1018, 8, 2, 1, 1, '2023-09-01', 1, NOW(), NOW()),
-- 2022级计算机221班
(19, '2022010101', 1019, 12, 1, 1, 1, '2022-09-01', 1, NOW(), NOW()),
(20, '2022010102', 1020, 12, 1, 1, 1, '2022-09-01', 1, NOW(), NOW());

-- =====================================================
-- 9. 更新统计数据
-- =====================================================
UPDATE classes c SET student_count = (
    SELECT COUNT(*) FROM students s WHERE s.class_id = c.id AND s.student_status = 1
);

UPDATE grades g SET
    total_classes = (SELECT COUNT(*) FROM classes c WHERE c.grade_id = g.id AND c.status = 1),
    total_students = (SELECT COALESCE(SUM(c.student_count), 0) FROM classes c WHERE c.grade_id = g.id AND c.status = 1);

-- =====================================================
-- 10. 验证数据
-- =====================================================
SELECT '数据重置完成!' AS message;
SELECT CONCAT('部门: ', COUNT(*)) AS stats FROM departments WHERE deleted = 0;
SELECT CONCAT('年级: ', COUNT(*)) AS stats FROM grades WHERE deleted = 0;
SELECT CONCAT('专业: ', COUNT(*)) AS stats FROM majors WHERE deleted = 0;
SELECT CONCAT('专业方向: ', COUNT(*)) AS stats FROM major_directions WHERE deleted = 0;
SELECT CONCAT('班级: ', COUNT(*)) AS stats FROM classes WHERE deleted = 0;
SELECT CONCAT('学生: ', COUNT(*)) AS stats FROM students WHERE deleted = 0;
