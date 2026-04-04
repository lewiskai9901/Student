-- V68.0.0: 检查计划指定检查员
-- 空=项目所有检查员都可领取，非空=仅指定人员可领取/被分配

ALTER TABLE insp_inspection_plans
    ADD COLUMN inspector_ids TEXT DEFAULT NULL COMMENT 'JSON: 指定检查员ID列表，空=不限';
