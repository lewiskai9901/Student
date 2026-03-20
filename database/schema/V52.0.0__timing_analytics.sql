-- V52.0.0 Feature 3.5: 计时分析

ALTER TABLE insp_submission_details ADD COLUMN time_spent_seconds INT DEFAULT NULL COMMENT '该项耗时(秒)';
ALTER TABLE insp_submissions ADD COLUMN total_time_seconds INT DEFAULT NULL COMMENT '总耗时(秒)';
ALTER TABLE insp_tasks ADD COLUMN execution_started_at DATETIME DEFAULT NULL;
ALTER TABLE insp_tasks ADD COLUMN execution_ended_at DATETIME DEFAULT NULL;
