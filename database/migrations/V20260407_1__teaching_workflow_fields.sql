-- 补全教务工作流串联所需的字段
-- 确保 teaching_tasks → offering 和 exam_arrangements → task 的关联完整

-- teaching_tasks: 添加 offering_id (关联开课计划) 和 teaching_class_id (关联教学班)
ALTER TABLE teaching_tasks ADD COLUMN IF NOT EXISTS offering_id BIGINT COMMENT '关联开课计划ID' AFTER class_id;
ALTER TABLE teaching_tasks ADD COLUMN IF NOT EXISTS teaching_class_id BIGINT COMMENT '关联教学班ID' AFTER offering_id;
ALTER TABLE teaching_tasks ADD INDEX IF NOT EXISTS idx_offering (offering_id);
ALTER TABLE teaching_tasks ADD INDEX IF NOT EXISTS idx_teaching_class (teaching_class_id);

-- exam_arrangements: 添加 task_id (关联教学任务) 和 class_id (关联班级)
ALTER TABLE exam_arrangements ADD COLUMN IF NOT EXISTS task_id BIGINT COMMENT '关联教学任务ID' AFTER course_id;
ALTER TABLE exam_arrangements ADD COLUMN IF NOT EXISTS class_id BIGINT COMMENT '关联班级ID' AFTER task_id;
ALTER TABLE exam_arrangements ADD INDEX IF NOT EXISTS idx_task (task_id);
