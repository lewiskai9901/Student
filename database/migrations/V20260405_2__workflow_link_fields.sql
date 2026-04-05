-- 补充工作流链路字段: 让各模块之间有显式关联

-- 教学任务 ← 开课计划
ALTER TABLE teaching_tasks ADD COLUMN IF NOT EXISTS offering_id BIGINT COMMENT '关联开课计划ID';
ALTER TABLE teaching_tasks ADD INDEX IF NOT EXISTS idx_offering (offering_id);

-- 考试安排 ← 教学任务
ALTER TABLE exam_arrangements ADD COLUMN IF NOT EXISTS task_id BIGINT COMMENT '关联教学任务ID';
ALTER TABLE exam_arrangements ADD INDEX IF NOT EXISTS idx_task (task_id);

-- 成绩批次 ← 考试批次
ALTER TABLE grade_batches ADD COLUMN IF NOT EXISTS exam_batch_id BIGINT COMMENT '关联考试批次ID';
ALTER TABLE grade_batches ADD INDEX IF NOT EXISTS idx_exam_batch (exam_batch_id);
