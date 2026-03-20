-- V62.0.0: 统一分区模型 — 分区替代模板成为唯一内容组织单元
-- TemplateSection 升级为聚合根，根分区(template_id=NULL)即原来的模板

-- 给 insp_template_sections 加字段，使其能替代 insp_templates
ALTER TABLE insp_template_sections ADD COLUMN target_type VARCHAR(20) NULL COMMENT '检查目标类型: ORG/PLACE/USER (仅一级分区)';
ALTER TABLE insp_template_sections ADD COLUMN description TEXT NULL;
ALTER TABLE insp_template_sections ADD COLUMN tags VARCHAR(500) NULL;
ALTER TABLE insp_template_sections ADD COLUMN catalog_id BIGINT NULL;
ALTER TABLE insp_template_sections ADD COLUMN status VARCHAR(20) DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/DEPRECATED/ARCHIVED';
ALTER TABLE insp_template_sections ADD COLUMN latest_version INT DEFAULT 0;
ALTER TABLE insp_template_sections ADD COLUMN grade_bands TEXT NULL COMMENT 'JSON: 等级映射 [{code,name,minScore,maxScore,color}]';
ALTER TABLE insp_template_sections ADD COLUMN ref_section_id BIGINT NULL COMMENT '引用的分区ID（替代ref_template_id）';

-- template_id 改为可空（根分区的 template_id=null，自己就是根）
ALTER TABLE insp_template_sections MODIFY COLUMN template_id BIGINT NULL;

-- 迁移数据：每个 template 变成一个根分区
INSERT INTO insp_template_sections
  (id, tenant_id, template_id, parent_section_id, section_code, section_name,
   description, tags, catalog_id, target_type, status, latest_version,
   sort_order, weight, scoring_config, created_by, created_at, updated_at, deleted)
SELECT
  id + 100000,       -- 避免 ID 冲突
  tenant_id,
  NULL,               -- 根分区没有 template_id
  NULL,               -- 根分区没有 parent
  template_code,
  template_name,
  description,
  tags,
  catalog_id,
  target_type,
  status,
  latest_version,
  0,                  -- sort_order
  100,                -- weight
  NULL,               -- scoring_config
  created_by,
  created_at,
  updated_at,
  deleted
FROM insp_templates WHERE deleted = 0;

-- 更新子分区的 parent_section_id 指向新的根分区（之前直属模板的顶层分区）
UPDATE insp_template_sections s
SET s.parent_section_id = s.template_id + 100000
WHERE s.template_id IS NOT NULL
  AND s.parent_section_id IS NULL
  AND s.deleted = 0;

-- 迁移模块引用：将 template_module_refs 转为 ref_section_id
UPDATE insp_template_sections s
SET s.ref_section_id = s.ref_template_id + 100000
WHERE s.ref_template_id IS NOT NULL AND s.deleted = 0;

-- 添加索引
CREATE INDEX idx_sections_catalog ON insp_template_sections(catalog_id);
CREATE INDEX idx_sections_status ON insp_template_sections(status);
CREATE INDEX idx_sections_ref ON insp_template_sections(ref_section_id);
