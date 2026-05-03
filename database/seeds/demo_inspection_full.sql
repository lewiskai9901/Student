-- ============================================================
-- 检查平台全流程演示数据 — 30 学生 / 3 班 / 3 班主任 / 5 检查员 / 6 宿舍
-- 所有用户密码均为 admin123 (BCrypt $2a$10$NBILuC13J0f71JBhs8sr5uiB02XDpR4BF9uhki3dDsXEaIGanmR6e)
-- 班级用现有: 经济2024-1班 / 汽车2024-1班 / 艺术2024-1班
-- ============================================================

SET @PWD = '$2a$10$NBILuC13J0f71JBhs8sr5uiB02XDpR4BF9uhki3dDsXEaIGanmR6e';

SET @CLS_JING = 2041867776338956290;  -- 经济2024-1班
SET @CLS_QC   = 2041867778582908930;  -- 汽车2024-1班
SET @CLS_YS   = 2041867780436791297;  -- 艺术2024-1班

-- ============================================================
-- 1. 创建 3 个班主任
-- ============================================================
INSERT INTO users (username, password, real_name, phone, email, primary_org_unit_id, user_type_code, status, tenant_id)
VALUES
('teacher_zhang', @PWD, '张老师', '13800000001', 'zhang@demo.local', @CLS_JING, 'TEACHER', 1, 1),
('teacher_li',    @PWD, '李老师', '13800000002', 'li@demo.local',    @CLS_QC,   'TEACHER', 1, 1),
('teacher_wang',  @PWD, '王老师', '13800000003', 'wang@demo.local',  @CLS_YS,   'TEACHER', 1, 1);

SET @T_ZHANG = (SELECT id FROM users WHERE username='teacher_zhang');
SET @T_LI    = (SELECT id FROM users WHERE username='teacher_li');
SET @T_WANG  = (SELECT id FROM users WHERE username='teacher_wang');

INSERT INTO user_teacher (user_id, employee_no, title, org_unit_id, status)
VALUES
(@T_ZHANG, 'T-2026-001', '中级', @CLS_JING, 1),
(@T_LI,    'T-2026-002', '中级', @CLS_QC,   1),
(@T_WANG,  'T-2026-003', '初级', @CLS_YS,   1);

-- 班主任角色绑定
INSERT INTO user_roles (user_id, role_id) VALUES
(@T_ZHANG, 5), (@T_LI, 5), (@T_WANG, 5);

-- 班主任 admin_of 班级 (BY_RELATION 推送的关键)
INSERT INTO access_relations (resource_type, resource_id, relation, subject_type, subject_id, access_level, created_by)
VALUES
('org_unit', @CLS_JING, 'admin', 'user', @T_ZHANG, 'FULL', 1),
('org_unit', @CLS_QC,   'admin', 'user', @T_LI,    'FULL', 1),
('org_unit', @CLS_YS,   'admin', 'user', @T_WANG,  'FULL', 1);

-- ============================================================
-- 2. 创建 5 个检查员
-- ============================================================
INSERT INTO users (username, password, real_name, phone, email, user_type_code, status, tenant_id)
VALUES
('insp_hygiene',    @PWD, '陈卫生', '13900000001', 'insp_h@demo.local', 'TEACHER', 1, 1),
('insp_discipline', @PWD, '刘纪律', '13900000002', 'insp_d@demo.local', 'TEACHER', 1, 1),
('insp_dorm',       @PWD, '孙宿舍', '13900000003', 'insp_o@demo.local', 'TEACHER', 1, 1),
('insp_safety',     @PWD, '周安全', '13900000004', 'insp_s@demo.local', 'TEACHER', 1, 1),
('insp_morning',    @PWD, '吴早操', '13900000005', 'insp_m@demo.local', 'TEACHER', 1, 1);

SET @I_H = (SELECT id FROM users WHERE username='insp_hygiene');
SET @I_D = (SELECT id FROM users WHERE username='insp_discipline');
SET @I_O = (SELECT id FROM users WHERE username='insp_dorm');
SET @I_S = (SELECT id FROM users WHERE username='insp_safety');
SET @I_M = (SELECT id FROM users WHERE username='insp_morning');

INSERT INTO user_roles (user_id, role_id) VALUES
(@I_H, 9), (@I_D, 9), (@I_O, 9), (@I_S, 9), (@I_M, 9);

-- ============================================================
-- 3. 创建 6 个宿舍 (每班 2 间, 男女各分)
-- ============================================================
INSERT INTO places (place_code, place_name, type_code, room_type, parent_id, capacity, current_occupancy, gender, org_unit_id, status, tenant_id, created_by)
VALUES
('DORM-A101', 'A栋101 (经济男寝)', 'DORM_ROOM', 'STUDENT', NULL, 6, 0, '男', @CLS_JING, 1, 1, 1),
('DORM-A102', 'A栋102 (经济女寝)', 'DORM_ROOM', 'STUDENT', NULL, 6, 0, '女', @CLS_JING, 1, 1, 1),
('DORM-B201', 'B栋201 (汽车男寝)', 'DORM_ROOM', 'STUDENT', NULL, 6, 0, '男', @CLS_QC,   1, 1, 1),
('DORM-B202', 'B栋202 (汽车女寝)', 'DORM_ROOM', 'STUDENT', NULL, 6, 0, '女', @CLS_QC,   1, 1, 1),
('DORM-C301', 'C栋301 (艺术男寝)', 'DORM_ROOM', 'STUDENT', NULL, 6, 0, '男', @CLS_YS,   1, 1, 1),
('DORM-C302', 'C栋302 (艺术女寝)', 'DORM_ROOM', 'STUDENT', NULL, 6, 0, '女', @CLS_YS,   1, 1, 1);

SET @D_A101 = (SELECT id FROM places WHERE place_code='DORM-A101');
SET @D_A102 = (SELECT id FROM places WHERE place_code='DORM-A102');
SET @D_B201 = (SELECT id FROM places WHERE place_code='DORM-B201');
SET @D_B202 = (SELECT id FROM places WHERE place_code='DORM-B202');
SET @D_C301 = (SELECT id FROM places WHERE place_code='DORM-C301');
SET @D_C302 = (SELECT id FROM places WHERE place_code='DORM-C302');

-- 宿舍管理员关系 (insp_dorm 管所有 6 个宿舍)
INSERT INTO access_relations (resource_type, resource_id, relation, subject_type, subject_id, access_level, created_by)
VALUES
('place', @D_A101, 'admin', 'user', @I_O, 'FULL', 1),
('place', @D_A102, 'admin', 'user', @I_O, 'FULL', 1),
('place', @D_B201, 'admin', 'user', @I_O, 'FULL', 1),
('place', @D_B202, 'admin', 'user', @I_O, 'FULL', 1),
('place', @D_C301, 'admin', 'user', @I_O, 'FULL', 1),
('place', @D_C302, 'admin', 'user', @I_O, 'FULL', 1);

-- ============================================================
-- 4. 创建 30 个学生 — 经济 12 / 汽车 10 / 艺术 8 (随机不均)
-- 真实常见姓名, 性别均衡分布给宿舍
-- ============================================================

-- 经济 2024-1 (12 人, 男 6 + 女 6)
INSERT INTO users (username, password, real_name, gender, primary_org_unit_id, user_type_code, status, tenant_id) VALUES
('stu_jing_01', @PWD, '张伟',  1, @CLS_JING, 'STUDENT', 1, 1),
('stu_jing_02', @PWD, '王芳',  2, @CLS_JING, 'STUDENT', 1, 1),
('stu_jing_03', @PWD, '李娜',  2, @CLS_JING, 'STUDENT', 1, 1),
('stu_jing_04', @PWD, '刘洋',  1, @CLS_JING, 'STUDENT', 1, 1),
('stu_jing_05', @PWD, '陈杰',  1, @CLS_JING, 'STUDENT', 1, 1),
('stu_jing_06', @PWD, '杨敏',  2, @CLS_JING, 'STUDENT', 1, 1),
('stu_jing_07', @PWD, '赵磊',  1, @CLS_JING, 'STUDENT', 1, 1),
('stu_jing_08', @PWD, '黄静',  2, @CLS_JING, 'STUDENT', 1, 1),
('stu_jing_09', @PWD, '周强',  1, @CLS_JING, 'STUDENT', 1, 1),
('stu_jing_10', @PWD, '吴丽',  2, @CLS_JING, 'STUDENT', 1, 1),
('stu_jing_11', @PWD, '徐军',  1, @CLS_JING, 'STUDENT', 1, 1),
('stu_jing_12', @PWD, '孙婷',  2, @CLS_JING, 'STUDENT', 1, 1);

-- 汽车 2024-1 (10 人, 男 7 + 女 3)
INSERT INTO users (username, password, real_name, gender, primary_org_unit_id, user_type_code, status, tenant_id) VALUES
('stu_qc_01', @PWD, '马龙',   1, @CLS_QC, 'STUDENT', 1, 1),
('stu_qc_02', @PWD, '朱涛',   1, @CLS_QC, 'STUDENT', 1, 1),
('stu_qc_03', @PWD, '胡浩',   1, @CLS_QC, 'STUDENT', 1, 1),
('stu_qc_04', @PWD, '林峰',   1, @CLS_QC, 'STUDENT', 1, 1),
('stu_qc_05', @PWD, '何斌',   1, @CLS_QC, 'STUDENT', 1, 1),
('stu_qc_06', @PWD, '高勇',   1, @CLS_QC, 'STUDENT', 1, 1),
('stu_qc_07', @PWD, '梁辉',   1, @CLS_QC, 'STUDENT', 1, 1),
('stu_qc_08', @PWD, '宋雪',   2, @CLS_QC, 'STUDENT', 1, 1),
('stu_qc_09', @PWD, '韩梅',   2, @CLS_QC, 'STUDENT', 1, 1),
('stu_qc_10', @PWD, '冯雨',   2, @CLS_QC, 'STUDENT', 1, 1);

-- 艺术 2024-1 (8 人, 男 3 + 女 5)
INSERT INTO users (username, password, real_name, gender, primary_org_unit_id, user_type_code, status, tenant_id) VALUES
('stu_ys_01', @PWD, '邓思', 2, @CLS_YS, 'STUDENT', 1, 1),
('stu_ys_02', @PWD, '曹梦', 2, @CLS_YS, 'STUDENT', 1, 1),
('stu_ys_03', @PWD, '彭瑶', 2, @CLS_YS, 'STUDENT', 1, 1),
('stu_ys_04', @PWD, '蒋琳', 2, @CLS_YS, 'STUDENT', 1, 1),
('stu_ys_05', @PWD, '袁萌', 2, @CLS_YS, 'STUDENT', 1, 1),
('stu_ys_06', @PWD, '谢辰', 1, @CLS_YS, 'STUDENT', 1, 1),
('stu_ys_07', @PWD, '余航', 1, @CLS_YS, 'STUDENT', 1, 1),
('stu_ys_08', @PWD, '潘宇', 1, @CLS_YS, 'STUDENT', 1, 1);

-- 学生角色 + user_student + member of class
INSERT INTO user_roles (user_id, role_id)
SELECT id, 2021993207935557634 FROM users WHERE username LIKE 'stu\_%';

INSERT INTO user_student (user_id, student_no, org_unit_id, grade_id, admission_date, student_status, tenant_id)
SELECT u.id, CONCAT('S2024-', LPAD(ROW_NUMBER() OVER (ORDER BY u.id), 4, '0')), u.primary_org_unit_id,
       (SELECT parent_id FROM org_units WHERE id = u.primary_org_unit_id),
       '2024-09-01', 1, 1
FROM users u WHERE u.username LIKE 'stu\_%';

INSERT INTO access_relations (resource_type, resource_id, relation, subject_type, subject_id, access_level, created_by)
SELECT 'org_unit', primary_org_unit_id, 'member', 'user', id, 'READ', 1
FROM users WHERE username LIKE 'stu\_%';

-- ============================================================
-- 5. 学生入住宿舍 (按 gender + 班级)
-- 经济: 男生 → A101, 女生 → A102
-- 汽车: 男生 → B201, 女生 → B202
-- 艺术: 男生 → C301, 女生 → C302
-- ============================================================

-- 用 access_relations 的 occupies 关系 + place_occupants 表
INSERT INTO place_occupants (place_id, occupant_type, occupant_id, occupant_name, username, gender, position_no, check_in_time, status)
SELECT
  CASE
    WHEN u.primary_org_unit_id = @CLS_JING AND u.gender = 1 THEN @D_A101
    WHEN u.primary_org_unit_id = @CLS_JING AND u.gender = 2 THEN @D_A102
    WHEN u.primary_org_unit_id = @CLS_QC AND u.gender = 1 THEN @D_B201
    WHEN u.primary_org_unit_id = @CLS_QC AND u.gender = 2 THEN @D_B202
    WHEN u.primary_org_unit_id = @CLS_YS AND u.gender = 1 THEN @D_C301
    WHEN u.primary_org_unit_id = @CLS_YS AND u.gender = 2 THEN @D_C302
  END AS place_id,
  'user', u.id, u.real_name, u.username, u.gender,
  CONCAT('B', LPAD((ROW_NUMBER() OVER (PARTITION BY u.primary_org_unit_id, u.gender ORDER BY u.id)), 2, '0')),
  NOW(), 1
FROM users u WHERE u.username LIKE 'stu\_%';

-- 同步 access_relations occupies
INSERT INTO access_relations (resource_type, resource_id, relation, subject_type, subject_id, access_level, created_by)
SELECT 'place', po.place_id, 'occupies', 'user', po.occupant_id, 'READ', 1
FROM place_occupants po WHERE po.occupant_type = 'user';

-- 更新 places.current_occupancy
UPDATE places p SET current_occupancy = (
  SELECT COUNT(*) FROM place_occupants WHERE place_id = p.id AND status = 1 AND deleted = 0
) WHERE p.place_code IN ('DORM-A101','DORM-A102','DORM-B201','DORM-B202','DORM-C301','DORM-C302');

-- 更新 user_student.dormitory_id
UPDATE user_student us
JOIN place_occupants po ON po.occupant_id = us.user_id AND po.occupant_type='user'
SET us.dormitory_id = po.place_id, us.bed_number = po.position_no;

-- ============================================================
-- 验证输出
-- ============================================================
SELECT '=== 班主任 ===' AS '';
SELECT u.username, u.real_name, ou.unit_name AS class_name FROM users u JOIN org_units ou ON ou.id = u.primary_org_unit_id WHERE u.username LIKE 'teacher\_%';
SELECT '=== 检查员 ===' AS '';
SELECT username, real_name FROM users WHERE username LIKE 'insp\_%';
SELECT '=== 宿舍 + 入住 ===' AS '';
SELECT place_code, place_name, current_occupancy, capacity FROM places WHERE place_code LIKE 'DORM-%' ORDER BY place_code;
SELECT '=== 学生分布 ===' AS '';
SELECT ou.unit_name, COUNT(*) AS student_count FROM users u JOIN org_units ou ON ou.id = u.primary_org_unit_id WHERE u.username LIKE 'stu\_%' GROUP BY ou.unit_name;
