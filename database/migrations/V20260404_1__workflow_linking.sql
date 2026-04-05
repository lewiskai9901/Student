-- 工作流串联: 在教学任务和考试安排之间建立显式引用链
-- 培养方案 → 开课(已有plan_id) → 教学任务(新增offering_id) → 考试安排(新增task_id)

-- 教学任务关联开课计划
ALTER TABLE teaching_tasks ADD COLUMN IF NOT EXISTS offering_id BIGINT COMMENT '关联开课计划ID';
ALTER TABLE teaching_tasks ADD INDEX idx_offering (offering_id);

-- 考试安排关联教学任务
ALTER TABLE exam_arrangements ADD COLUMN IF NOT EXISTS task_id BIGINT COMMENT '关联教学任务ID';
ALTER TABLE exam_arrangements ADD INDEX idx_task (task_id);
