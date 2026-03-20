-- V63.0.0: Design B — 多层目标派生
-- 每个分区可配目标实体类型、来源模式、类型过滤

-- 分区目标配置（target_type 已存在，改名为 target_entity_type 语义更清晰）
-- 保留 target_type 列兼容，新增 target_source_mode 和 target_type_filter
ALTER TABLE insp_template_sections ADD COLUMN target_source_mode VARCHAR(20) NULL COMMENT 'INDEPENDENT=根分区从项目范围/PARENT_ASSOCIATED=从父目标派生';
ALTER TABLE insp_template_sections ADD COLUMN target_type_filter VARCHAR(100) NULL COMMENT '类型过滤: org_type=班级, place_category=宿舍, user_type=学生';

-- Submission 加根目标ID（用于按部门分组显示）
ALTER TABLE insp_submissions ADD COLUMN root_target_id BIGINT NULL COMMENT '根目标ID(用于按部门/系分组)';
ALTER TABLE insp_submissions ADD COLUMN root_target_name VARCHAR(100) NULL COMMENT '根目标名称';
CREATE INDEX idx_submissions_root_target ON insp_submissions(root_target_id);
