-- V62.5.0: 持续检查模式
-- SNAPSHOT = 检查一次提交；CONTINUOUS = 时间段内持续记录

ALTER TABLE insp_template_sections ADD COLUMN inspection_mode VARCHAR(20) DEFAULT 'SNAPSHOT' COMMENT 'SNAPSHOT/CONTINUOUS';
ALTER TABLE insp_template_sections ADD COLUMN continuous_start VARCHAR(10) NULL COMMENT '持续模式开始时间 HH:mm';
ALTER TABLE insp_template_sections ADD COLUMN continuous_end VARCHAR(10) NULL COMMENT '持续模式结束时间 HH:mm';

-- Submission 加 OPEN 状态支持（status 字段已是 VARCHAR，直接支持新值 OPEN）
