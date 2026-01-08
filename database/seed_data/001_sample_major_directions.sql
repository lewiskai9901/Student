-- ================================================================
-- 专业方向示例数据
-- 用于验证表结构和业务逻辑
-- ================================================================

-- 1. 插入专业方向(假设majors表中已有计算机网络应用专业,ID=1)
INSERT INTO major_directions (
  major_id, direction_name, direction_code,
  education_system, skill_level, duration,
  default_class_size, status, sort_order
) VALUES
  -- 计算机网络应用专业的不同方向
  (1, '3+2高级工', 'CN_3P2_ADV', '3+2', '高级工', 5, 45, 1, 1),
  (1, '3年制预备技师', 'CN_3Y_PRE', '3年', '预备技师', 3, 40, 1, 2),
  (1, '5年制技师', 'CN_5Y_TECH', '5年', '技师', 5, 35, 1, 3),
  (1, '2+2国际班', 'CN_2P2_INT', '2+2', '高级工', 4, 25, 1, 4);

-- 2. 为2024级配置专业方向(假设grades表中已有2024级,ID=1)
-- 先获取刚插入的专业方向ID
SET @dir_3p2 = (SELECT id FROM major_directions WHERE direction_code = 'CN_3P2_ADV');
SET @dir_3y = (SELECT id FROM major_directions WHERE direction_code = 'CN_3Y_PRE');
SET @dir_5y = (SELECT id FROM major_directions WHERE direction_code = 'CN_5Y_TECH');
SET @dir_2p2 = (SELECT id FROM major_directions WHERE direction_code = 'CN_2P2_INT');

INSERT INTO grade_major_directions (
  grade_id, major_direction_id, major_id, major_name,
  education_system, skill_level, duration,
  planned_class_count, planned_student_count,
  is_new_direction
) VALUES
  -- 2024级开设的专业方向
  (1, @dir_3p2, 1, '计算机网络应用', '3+2', '高级工', 5, 2, 90, 0),
  (1, @dir_3y, 1, '计算机网络应用', '3年', '预备技师', 3, 1, 40, 0),
  (1, @dir_2p2, 1, '计算机网络应用', '2+2', '高级工', 4, 1, 25, 1);  -- 新增方向

-- 3. 查询验证
SELECT '========== 专业方向列表 ==========' AS '';
SELECT
  md.id,
  m.major_name,
  md.direction_name,
  md.education_system,
  md.skill_level,
  md.duration,
  md.default_class_size,
  CASE md.status WHEN 1 THEN '招生中' ELSE '停招' END AS status_text
FROM major_directions md
LEFT JOIN majors m ON md.major_id = m.id
ORDER BY m.major_name, md.sort_order;

SELECT '========== 2024级开设的专业方向 ==========' AS '';
SELECT * FROM v_grade_major_direction_summary;

SELECT 'Sample data inserted successfully!' AS status;
