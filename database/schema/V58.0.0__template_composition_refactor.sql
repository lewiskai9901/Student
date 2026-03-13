-- V58.0.0 模板组合重构：统一模板 + 多目标类型 + 模块权重
-- 设计原则：不区分"叶子模板"和"组合模板"，所有模板统一，既可以有自己的分区/字段，也可以引用子模板

-- 1. 模板增加 target_type（单值，替代 applicable_target_types JSON 数组）
ALTER TABLE insp_templates ADD COLUMN target_type VARCHAR(20) DEFAULT 'ORG' COMMENT '检查目标类型: ORG/PLACE/USER/ASSET';

-- 2. 从 applicable_target_types 迁移数据到 target_type（取第一个值）
UPDATE insp_templates
SET target_type = COALESCE(
    JSON_UNQUOTE(JSON_EXTRACT(applicable_target_types, '$[0]')),
    'ORG'
)
WHERE applicable_target_types IS NOT NULL AND applicable_target_types != '';

-- 3. 删除旧字段
ALTER TABLE insp_templates DROP COLUMN applicable_target_types;
ALTER TABLE insp_templates DROP COLUMN is_module;
ALTER TABLE insp_templates DROP COLUMN is_composite;

-- 4. 模块引用增加 weight（百分比权重，默认100）
ALTER TABLE insp_template_module_refs ADD COLUMN weight INT DEFAULT 100 COMMENT '权重(百分比)';
