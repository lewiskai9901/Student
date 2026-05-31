-- 补充工作流链路字段: 让各模块之间有显式关联

-- MySQL 8.0 不支持 ADD COLUMN/INDEX IF NOT EXISTS, 用 information_schema 条件化
-- 教学任务 ← 开课计划
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='teaching_tasks' AND COLUMN_NAME='offering_id');
SET @s := IF(@c=0, "ALTER TABLE teaching_tasks ADD COLUMN offering_id BIGINT COMMENT '关联开课计划ID'", "SELECT 1");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @x := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='teaching_tasks' AND INDEX_NAME='idx_offering');
SET @s := IF(@x=0, "ALTER TABLE teaching_tasks ADD INDEX idx_offering (offering_id)", "SELECT 1");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 考试安排 ← 教学任务
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='exam_arrangements' AND COLUMN_NAME='task_id');
SET @s := IF(@c=0, "ALTER TABLE exam_arrangements ADD COLUMN task_id BIGINT COMMENT '关联教学任务ID'", "SELECT 1");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @x := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='exam_arrangements' AND INDEX_NAME='idx_task');
SET @s := IF(@x=0, "ALTER TABLE exam_arrangements ADD INDEX idx_task (task_id)", "SELECT 1");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 成绩批次 ← 考试批次
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='grade_batches' AND COLUMN_NAME='exam_batch_id');
SET @s := IF(@c=0, "ALTER TABLE grade_batches ADD COLUMN exam_batch_id BIGINT COMMENT '关联考试批次ID'", "SELECT 1");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @x := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='grade_batches' AND INDEX_NAME='idx_exam_batch');
SET @s := IF(@x=0, "ALTER TABLE grade_batches ADD INDEX idx_exam_batch (exam_batch_id)", "SELECT 1");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
