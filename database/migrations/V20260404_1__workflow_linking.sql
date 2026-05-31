-- 工作流串联: 在教学任务和考试安排之间建立显式引用链
-- 培养方案 → 开课(已有plan_id) → 教学任务(新增offering_id) → 考试安排(新增task_id)

-- 教学任务关联开课计划 (MySQL 8.0 条件化)
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='teaching_tasks' AND COLUMN_NAME='offering_id');
SET @s := IF(@c=0, "ALTER TABLE teaching_tasks ADD COLUMN offering_id BIGINT COMMENT '关联开课计划ID'", "SELECT 1");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @x := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='teaching_tasks' AND INDEX_NAME='idx_offering');
SET @s := IF(@x=0, "ALTER TABLE teaching_tasks ADD INDEX idx_offering (offering_id)", "SELECT 1");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 考试安排关联教学任务
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='exam_arrangements' AND COLUMN_NAME='task_id');
SET @s := IF(@c=0, "ALTER TABLE exam_arrangements ADD COLUMN task_id BIGINT COMMENT '关联教学任务ID'", "SELECT 1");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @x := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='exam_arrangements' AND INDEX_NAME='idx_task');
SET @s := IF(@x=0, "ALTER TABLE exam_arrangements ADD INDEX idx_task (task_id)", "SELECT 1");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
