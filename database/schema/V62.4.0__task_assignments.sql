-- V62.4.0: 任务分配增强 + Submission 分区上下文

-- 任务可以只分配部分分区和部分目标
ALTER TABLE insp_tasks ADD COLUMN assigned_section_ids TEXT NULL COMMENT 'JSON: 分配的分区ID（空=全部）';
ALTER TABLE insp_tasks ADD COLUMN assigned_target_ids TEXT NULL COMMENT 'JSON: 分配的目标ID（空=全部）';
ALTER TABLE insp_tasks ADD COLUMN inspection_plan_id BIGINT NULL COMMENT '关联的检查计划';

-- Submission 加分区上下文
ALTER TABLE insp_submissions ADD COLUMN section_id BIGINT NULL COMMENT '所属一级分区（有targetType的）';

-- 索引
CREATE INDEX idx_tasks_plan ON insp_tasks(inspection_plan_id);
CREATE INDEX idx_submissions_section ON insp_submissions(section_id);
