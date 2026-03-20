-- V62.6.0: 任务拆分策略
-- NONE=不拆分(一人一个完整任务), BY_TARGET=按目标均分, BY_SECTION=按分区分配, MANUAL=手动指定
ALTER TABLE insp_projects ADD COLUMN split_strategy VARCHAR(20) DEFAULT 'NONE' COMMENT 'NONE/BY_TARGET/BY_SECTION/MANUAL';
