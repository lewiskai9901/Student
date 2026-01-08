-- ============================================================================
-- 学生管理系统 - 数据迁移与初始化脚本 V3.0.1
-- 创建时间: 2024-12-29
-- 描述:
--   1. 自动生成grades表数据(从classes表推导)
--   2. 填充classes.grade_id字段
--   3. 填充students.grade_id字段
--   4. 计算并初始化标准班级人数
--   5. 补录历史人数快照
--   6. 数据验证和修正
-- ============================================================================

USE student_management;

-- ============================================================================
-- 第一部分: 生成年级数据
-- ============================================================================

-- 1. 从classes表自动生成grades数据
INSERT INTO grades (
    grade_name,
    grade_code,
    grade_level,
    enrollment_year,
    department_id,
    grade_director_id,
    total_classes,
    total_students,
    standard_class_size,
    status,
    graduation_year,
    created_at,
    updated_at
)
SELECT
    CONCAT(
        c.enrollment_year, '级',
        CASE c.grade_level
            WHEN 7 THEN '初一'
            WHEN 8 THEN '初二'
            WHEN 9 THEN '初三'
            WHEN 10 THEN '高一'
            WHEN 11 THEN '高二'
            WHEN 12 THEN '高三'
            WHEN 1 THEN '大一'
            WHEN 2 THEN '大二'
            WHEN 3 THEN '大三'
            WHEN 4 THEN '大四'
            ELSE CONCAT('年级', c.grade_level)
        END
    ) as grade_name,
    CONCAT('GRADE_', c.enrollment_year, '_', c.grade_level) as grade_code,
    c.grade_level,
    c.enrollment_year,
    c.department_id,
    NULL as grade_director_id, -- 需要后续手动指定
    COUNT(*) as total_classes,
    SUM(COALESCE(
        (SELECT COUNT(*) FROM students s WHERE s.class_id = c.id AND s.deleted = 0),
        0
    )) as total_students,
    ROUND(AVG(COALESCE(
        (SELECT COUNT(*) FROM students s WHERE s.class_id = c.id AND s.deleted = 0),
        0
    ))) as standard_class_size,
    CASE
        WHEN c.status = 1 THEN 1 -- 在读
        ELSE 1
    END as status,
    c.graduation_year,
    NOW(),
    NOW()
FROM classes c
WHERE c.deleted = 0
GROUP BY c.enrollment_year, c.grade_level, c.department_id
ON DUPLICATE KEY UPDATE
    total_classes = VALUES(total_classes),
    total_students = VALUES(total_students),
    standard_class_size = VALUES(standard_class_size),
    updated_at = NOW();

SELECT CONCAT('✅ 已生成 ', ROW_COUNT(), ' 个年级') as step1;

-- ============================================================================
-- 第二部分: 填充classes.grade_id字段
-- ============================================================================

-- 2. 更新classes表的grade_id
UPDATE classes c
JOIN grades g ON
    c.grade_level = g.grade_level
    AND c.enrollment_year = g.enrollment_year
    AND c.department_id = g.department_id
    AND g.deleted = 0
SET c.grade_id = g.id
WHERE c.deleted = 0 AND c.grade_id IS NULL;

-- 验证匹配结果
SELECT
    COUNT(*) as total_classes,
    SUM(CASE WHEN grade_id IS NOT NULL THEN 1 ELSE 0 END) as matched_classes,
    SUM(CASE WHEN grade_id IS NULL THEN 1 ELSE 0 END) as unmatched_classes
FROM classes
WHERE deleted = 0;

-- 如果有未匹配的班级,输出警告
SELECT
    id, class_name, grade_level, enrollment_year, department_id
FROM classes
WHERE deleted = 0 AND grade_id IS NULL
LIMIT 10;

SELECT '✅ classes.grade_id 字段已填充' as step2;

-- ============================================================================
-- 第三部分: 填充students.grade_id字段
-- ============================================================================

-- 3. 更新students表的grade_id(通过class_id关联)
UPDATE students s
JOIN classes c ON s.class_id = c.id
SET s.grade_id = c.grade_id
WHERE s.deleted = 0 AND s.grade_id IS NULL;

SELECT CONCAT('✅ 已更新 ', ROW_COUNT(), ' 个学生的grade_id') as step3;

-- ============================================================================
-- 第四部分: 初始化标准班级人数
-- ============================================================================

-- 4. 为当前学期计算并插入标准班级人数
INSERT INTO class_size_standards (
    semester_id,
    department_id,
    grade_level,
    standard_size,
    calculation_method,
    calculation_date,
    calculation_base_count,
    locked,
    locked_at,
    remarks,
    created_at,
    updated_at
)
SELECT
    (SELECT id FROM semesters WHERE is_current = 1 LIMIT 1) as semester_id,
    c.department_id,
    c.grade_level,
    ROUND(AVG(COALESCE(
        (SELECT COUNT(*) FROM students s
         WHERE s.class_id = c.id
         AND s.deleted = 0
         AND s.student_status IN (0, 1)),
        0
    ))) as standard_size,
    'AUTO' as calculation_method,
    CURDATE() as calculation_date,
    COUNT(*) as calculation_base_count,
    1 as locked, -- 默认锁定
    NOW() as locked_at,
    CONCAT('系统自动计算,基于', COUNT(*), '个班级的平均人数') as remarks,
    NOW(),
    NOW()
FROM classes c
WHERE c.deleted = 0
  AND c.status = 1
GROUP BY c.department_id, c.grade_level
ON DUPLICATE KEY UPDATE
    standard_size = VALUES(standard_size),
    calculation_method = VALUES(calculation_method),
    calculation_date = VALUES(calculation_date),
    calculation_base_count = VALUES(calculation_base_count),
    updated_at = NOW();

SELECT CONCAT('✅ 已初始化 ', ROW_COUNT(), ' 个标准人数配置') as step4;

-- 显示标准人数配置
SELECT
    s.semester_name,
    d.dept_name,
    css.grade_level,
    css.standard_size,
    css.calculation_base_count,
    css.locked
FROM class_size_standards css
JOIN semesters s ON css.semester_id = s.id
JOIN departments d ON css.department_id = d.id
ORDER BY d.dept_name, css.grade_level;

-- ============================================================================
-- 第五部分: 补录历史人数快照
-- ============================================================================

-- 5. 为最近30天的检查记录补录人数快照
-- 注意: 这里使用当前人数作为历史快照(不完全准确,但总比没有好)

INSERT INTO class_size_snapshots (
    class_id,
    snapshot_date,
    student_count,
    active_count,
    snapshot_source,
    created_at
)
SELECT DISTINCT
    s.class_id,
    r.check_date as snapshot_date,
    COALESCE(
        (SELECT COUNT(*) FROM students st
         WHERE st.class_id = s.class_id
         AND st.deleted = 0
         AND st.student_status IN (0, 1)),
        0
    ) as student_count,
    COALESCE(
        (SELECT COUNT(*) FROM students st
         WHERE st.class_id = s.class_id
         AND st.deleted = 0
         AND st.student_status = 0), -- 只计算在读
        0
    ) as active_count,
    'MIGRATION' as snapshot_source,
    NOW()
FROM check_record_class_stats s
JOIN check_records_v3 r ON s.record_id = r.id
WHERE r.check_date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
  AND NOT EXISTS (
      SELECT 1 FROM class_size_snapshots css
      WHERE css.class_id = s.class_id
      AND css.snapshot_date = r.check_date
  )
ON DUPLICATE KEY UPDATE
    student_count = VALUES(student_count),
    active_count = VALUES(active_count);

SELECT CONCAT('✅ 已补录 ', ROW_COUNT(), ' 条历史人数快照') as step5;

-- ============================================================================
-- 第六部分: 更新check_record_class_stats的人数快照
-- ============================================================================

-- 6. 填充check_record_class_stats.student_count_snapshot字段
UPDATE check_record_class_stats s
JOIN check_records_v3 r ON s.record_id = r.id
LEFT JOIN class_size_snapshots css ON
    s.class_id = css.class_id
    AND r.check_date = css.snapshot_date
SET s.student_count_snapshot = COALESCE(
    css.student_count,
    (SELECT COUNT(*) FROM students st
     WHERE st.class_id = s.class_id
     AND st.deleted = 0
     AND st.student_status IN (0, 1))
)
WHERE s.student_count_snapshot IS NULL;

SELECT CONCAT('✅ 已更新 ', ROW_COUNT(), ' 条班级统计的人数快照') as step6;

-- ============================================================================
-- 第七部分: 初始化加权相关字段
-- ============================================================================

-- 7. 为历史检查记录设置默认加权配置
UPDATE check_records_v3
SET
    weight_config_id = (SELECT id FROM class_weight_configs WHERE is_default = 1 LIMIT 1),
    weight_enabled = 0, -- 历史记录不启用加权,避免影响已有排名
    appeal_config_id = (SELECT id FROM appeal_configs WHERE is_default = 1 LIMIT 1)
WHERE weight_config_id IS NULL;

SELECT CONCAT('✅ 已为 ', ROW_COUNT(), ' 条检查记录设置默认配置') as step7;

-- 8. 初始化check_record_class_stats的加权字段
-- 对于未启用加权的历史记录,raw_score = total_score, weight_factor = 1.0
UPDATE check_record_class_stats s
JOIN check_records_v3 r ON s.record_id = r.id
SET
    s.raw_total_score = s.total_score,
    s.weight_factor = 1.0000,
    s.adjusted_total_score = s.total_score
WHERE r.weight_enabled = 0
  AND s.raw_total_score IS NULL;

SELECT CONCAT('✅ 已初始化 ', ROW_COUNT(), ' 条班级统计的加权字段') as step8;

-- ============================================================================
-- 第八部分: 数据验证
-- ============================================================================

-- 9. 验证grades表
SELECT '========== 年级表验证 ==========' as title;
SELECT
    COUNT(*) as total_grades,
    SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) as active_grades,
    SUM(total_classes) as total_classes,
    SUM(total_students) as total_students
FROM grades WHERE deleted = 0;

-- 10. 验证classes.grade_id
SELECT '========== 班级年级关联验证 ==========' as title;
SELECT
    COUNT(*) as total_classes,
    SUM(CASE WHEN grade_id IS NOT NULL THEN 1 ELSE 0 END) as matched,
    SUM(CASE WHEN grade_id IS NULL THEN 1 ELSE 0 END) as unmatched
FROM classes WHERE deleted = 0;

-- 如果有未匹配的班级,显示警告
SELECT '⚠️ 警告:以下班级未匹配到年级,请手动处理:' as warning
WHERE EXISTS (SELECT 1 FROM classes WHERE deleted = 0 AND grade_id IS NULL);

SELECT id, class_name, grade_level, enrollment_year, department_id
FROM classes
WHERE deleted = 0 AND grade_id IS NULL;

-- 11. 验证标准人数配置
SELECT '========== 标准人数配置验证 ==========' as title;
SELECT
    COUNT(*) as total_standards,
    SUM(CASE WHEN locked = 1 THEN 1 ELSE 0 END) as locked_standards,
    ROUND(AVG(standard_size), 2) as avg_standard_size,
    MIN(standard_size) as min_size,
    MAX(standard_size) as max_size
FROM class_size_standards;

-- 12. 验证人数快照
SELECT '========== 人数快照验证 ==========' as title;
SELECT
    COUNT(*) as total_snapshots,
    COUNT(DISTINCT class_id) as unique_classes,
    COUNT(DISTINCT snapshot_date) as unique_dates,
    MIN(snapshot_date) as earliest_date,
    MAX(snapshot_date) as latest_date
FROM class_size_snapshots;

-- 13. 验证student_count字段一致性
SELECT '========== 班级人数一致性验证 ==========' as title;
SELECT
    c.id,
    c.class_name,
    c.student_count as stored_count,
    COUNT(s.id) as actual_count,
    c.student_count - COUNT(s.id) as difference
FROM classes c
LEFT JOIN students s ON c.id = s.class_id AND s.deleted = 0 AND s.student_status IN (0, 1)
WHERE c.deleted = 0
GROUP BY c.id
HAVING ABS(c.student_count - COUNT(s.id)) > 0
LIMIT 10;

-- ============================================================================
-- 第九部分: 生成初始权限数据(可选)
-- ============================================================================

-- 14. 插入申诉管理相关权限
INSERT INTO permissions (permission_code, permission_name, permission_desc, parent_id, sort_order, status, created_at, updated_at)
VALUES
-- 申诉管理模块
('appeal', '申诉管理', '检查项目申诉管理', NULL, 70, 1, NOW(), NOW()),
('appeal:view', '查看申诉', '查看申诉记录', (SELECT id FROM permissions WHERE permission_code='appeal' LIMIT 1), 1, 1, NOW(), NOW()),
('appeal:submit', '提交申诉', '提交申诉申请', (SELECT id FROM permissions WHERE permission_code='appeal' LIMIT 1), 2, 1, NOW(), NOW()),
('appeal:review', '审核申诉', '审核申诉申请', (SELECT id FROM permissions WHERE permission_code='appeal' LIMIT 1), 3, 1, NOW(), NOW()),
('appeal:withdraw', '撤销申诉', '撤销自己的申诉', (SELECT id FROM permissions WHERE permission_code='appeal' LIMIT 1), 4, 1, NOW(), NOW()),
('appeal:config', '申诉配置', '配置申诉规则', (SELECT id FROM permissions WHERE permission_code='appeal' LIMIT 1), 5, 1, NOW(), NOW()),

-- 加权配置模块
('weight', '加权配置', '班级人数加权配置', NULL, 71, 1, NOW(), NOW()),
('weight:view', '查看配置', '查看加权配置', (SELECT id FROM permissions WHERE permission_code='weight' LIMIT 1), 1, 1, NOW(), NOW()),
('weight:manage', '管理配置', '管理加权规则', (SELECT id FROM permissions WHERE permission_code='weight' LIMIT 1), 2, 1, NOW(), NOW()),
('weight:standard', '标准人数', '管理标准班级人数', (SELECT id FROM permissions WHERE permission_code='weight' LIMIT 1), 3, 1, NOW(), NOW()),

-- 年级管理模块
('grade', '年级管理', '年级信息管理', NULL, 72, 1, NOW(), NOW()),
('grade:view', '查看年级', '查看年级信息', (SELECT id FROM permissions WHERE permission_code='grade' LIMIT 1), 1, 1, NOW(), NOW()),
('grade:manage', '管理年级', '创建修改年级', (SELECT id FROM permissions WHERE permission_code='grade' LIMIT 1), 2, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

SELECT '✅ 权限数据已插入' as step9;

-- 15. 为管理员角色分配权限
INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT
    (SELECT id FROM roles WHERE role_code = 'admin' LIMIT 1),
    p.id,
    NOW()
FROM permissions p
WHERE p.permission_code IN (
    'appeal', 'appeal:view', 'appeal:submit', 'appeal:review', 'appeal:withdraw', 'appeal:config',
    'weight', 'weight:view', 'weight:manage', 'weight:standard',
    'grade', 'grade:view', 'grade:manage'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = (SELECT id FROM roles WHERE role_code = 'admin' LIMIT 1)
    AND rp.permission_id = p.id
);

-- 16. 为班主任角色分配基本权限
INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT
    (SELECT id FROM roles WHERE role_code = 'class_teacher' LIMIT 1),
    p.id,
    NOW()
FROM permissions p
WHERE p.permission_code IN (
    'appeal:view', 'appeal:submit', 'appeal:withdraw'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = (SELECT id FROM roles WHERE role_code = 'class_teacher' LIMIT 1)
    AND rp.permission_id = p.id
);

SELECT '✅ 角色权限已分配' as step10;

-- ============================================================================
-- 第十部分: 性能优化建议
-- ============================================================================

-- 17. 分析表统计信息(优化查询性能)
ANALYZE TABLE grades;
ANALYZE TABLE class_size_standards;
ANALYZE TABLE class_size_snapshots;
ANALYZE TABLE class_weight_configs;
ANALYZE TABLE category_weight_rules;
ANALYZE TABLE check_item_appeals;
ANALYZE TABLE appeal_approval_records;
ANALYZE TABLE appeal_configs;
ANALYZE TABLE appeal_approval_configs;

SELECT '✅ 表统计信息已更新' as step11;

-- ============================================================================
-- 完成
-- ============================================================================

SELECT '
╔════════════════════════════════════════════════════════════════╗
║                    数据迁移完成报告                              ║
╠════════════════════════════════════════════════════════════════╣
║  ✅ 年级数据已生成                                               ║
║  ✅ 班级-年级关联已建立                                          ║
║  ✅ 标准人数已初始化                                             ║
║  ✅ 历史快照已补录                                               ║
║  ✅ 加权字段已初始化                                             ║
║  ✅ 权限数据已配置                                               ║
╠════════════════════════════════════════════════════════════════╣
║  ⚠️  请注意:                                                     ║
║  1. 检查是否有班级未匹配到年级                                    ║
║  2. 手动指定各年级的年级主任(grades.grade_director_id)           ║
║  3. 验证标准人数是否合理                                         ║
║  4. 历史检查记录默认未启用加权(weight_enabled=0)                 ║
╚════════════════════════════════════════════════════════════════╝
' as report;

COMMIT;
