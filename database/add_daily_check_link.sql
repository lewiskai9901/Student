-- 为inspection_sessions表添加daily_check_id字段,用于关联原始的日常检查记录

ALTER TABLE inspection_sessions
ADD COLUMN daily_check_id BIGINT NULL COMMENT '关联的日常检查ID' AFTER id,
ADD INDEX idx_daily_check (daily_check_id);

-- 添加注释说明
ALTER TABLE inspection_sessions COMMENT='检查批次表 - 从已结束的日常检查转换而来';
