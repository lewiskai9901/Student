-- V59.0.0 评分维度来源类型：支持维度关联到子模板
-- sourceType: SECTION(来自模板分区,默认) / MODULE(来自子模板引用)
ALTER TABLE insp_score_dimensions ADD COLUMN source_type VARCHAR(20) DEFAULT 'SECTION' COMMENT '维度来源: SECTION/MODULE';
ALTER TABLE insp_score_dimensions ADD COLUMN module_template_id BIGINT NULL COMMENT '关联的子模板ID(source_type=MODULE时)';
