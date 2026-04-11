-- V26.0.0: 教学任务增强 — 多教师课时分配、教室需求、连排要求、课程性质
-- 2026-04-11

-- 1. teaching_task_teachers 加课时字段
ALTER TABLE teaching_task_teachers
    ADD COLUMN weekly_hours INT NULL COMMENT '该教师承担的周课时数' AFTER teacher_role;

-- 2. teaching_tasks 加教室需求、连排要求、课程性质
ALTER TABLE teaching_tasks
    ADD COLUMN room_type_required VARCHAR(50) NULL COMMENT '需要的教室类型(关联场所type_code)' AFTER end_week,
    ADD COLUMN consecutive_periods INT DEFAULT 2 COMMENT '连排节数(1=不连排,2=2节连排,3=3节连排,4=4节连排)' AFTER room_type_required,
    ADD COLUMN course_nature TINYINT DEFAULT 1 COMMENT '课程性质(1=理论,2=实验,3=实践,4=理论+实验)' AFTER consecutive_periods;

-- 3. 更新 task_status 注释（新状态体系）
ALTER TABLE teaching_tasks
    MODIFY COLUMN task_status TINYINT DEFAULT 0 COMMENT '0=待落实 1=已分配教师 2=已排课 3=进行中 4=已结束 9=已取消';

-- 4. 清空开发数据（开发阶段）
DELETE FROM teaching_task_teachers;
DELETE FROM teaching_tasks;
DELETE FROM class_course_assignments;
DELETE FROM semester_course_offerings;
