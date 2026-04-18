-- V20260419_4 INSERT 数据时 charset 不对,导致 data_resources 部分中文乱码
-- 本迁移重写所有受影响字段

UPDATE data_resources SET resource_name = '班级',     domain_name = '教育'     WHERE resource_code = 'school_class';
UPDATE data_resources SET resource_name = '教学任务', domain_name = '教育'     WHERE resource_code = 'teaching_task';
UPDATE data_resources SET resource_name = '学生成绩', domain_name = '教育'     WHERE resource_code = 'student_grade';
UPDATE data_resources SET resource_name = '考试批次', domain_name = '教育'     WHERE resource_code = 'exam_batch';
UPDATE data_resources SET                           domain_name = '教育'     WHERE resource_code IN ('dormitory_building','dormitory_checkin','dormitory_room');
UPDATE data_resources SET resource_name = '宿舍楼',   domain_name = '教育'     WHERE resource_code = 'dormitory_building';
UPDATE data_resources SET resource_name = '住宿登记', domain_name = '教育'     WHERE resource_code = 'dormitory_checkin';
UPDATE data_resources SET resource_name = '宿舍房间', domain_name = '教育'     WHERE resource_code = 'dormitory_room';

UPDATE data_resources SET domain_name = '检查平台' WHERE domain_code = 'inspection';
UPDATE data_resources SET resource_name = '检查申诉' WHERE resource_code = 'inspection_appeal';
UPDATE data_resources SET resource_name = '整改任务' WHERE resource_code = 'inspection_corrective';
UPDATE data_resources SET resource_name = '个人评级' WHERE resource_code = 'inspection_personal';
UPDATE data_resources SET resource_name = '检查项目' WHERE resource_code = 'inspection_project';
UPDATE data_resources SET resource_name = '检查记录' WHERE resource_code = 'inspection_record';
UPDATE data_resources SET resource_name = '检查汇总' WHERE resource_code = 'inspection_summary';
UPDATE data_resources SET resource_name = '检查任务' WHERE resource_code = 'inspection_task';
UPDATE data_resources SET resource_name = '检查模板' WHERE resource_code = 'inspection_template';

UPDATE data_resources SET resource_name = '系统角色' WHERE resource_code = 'system_role';
UPDATE data_resources SET resource_name = '系统用户' WHERE resource_code = 'system_user';

-- CORE 域下乱码修复
UPDATE data_resources SET domain_name = '核心' WHERE domain_code = 'CORE' AND (domain_name LIKE '%?%' OR domain_name LIKE '%鏍%');
UPDATE data_resources SET resource_name = '仪表盘' WHERE resource_code = 'dashboard';
