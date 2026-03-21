-- V66.0.0: 多模板支持 — 检查项目支持通过计划关联不同模板
--
-- 核心变更：
-- 1. insp_projects.template_id 改为可空（项目不再强制绑定单个模板）
-- 2. insp_inspection_plans 增加 root_section_id（每个计划绑定自己的模板）

-- 1. insp_projects: template_id 改为 NULL 允许
ALTER TABLE insp_projects
    MODIFY COLUMN template_id BIGINT NULL COMMENT '向后兼容保留，可空；新项目通过计划关联模板';

-- 2. insp_inspection_plans: 增加 root_section_id 列（计划级模板绑定）
ALTER TABLE insp_inspection_plans
    ADD COLUMN root_section_id BIGINT NULL COMMENT 'V66: 该计划使用的模板根分区ID' AFTER plan_name;

-- 3. 数据迁移：将现有计划的 root_section_id 从关联项目的 template_id 继承
UPDATE insp_inspection_plans ip
    JOIN insp_projects p ON p.id = ip.project_id
SET ip.root_section_id = p.template_id
WHERE ip.root_section_id IS NULL
  AND p.template_id IS NOT NULL
  AND ip.deleted = 0;
