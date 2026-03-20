-- V47.0.0 Feature 2.2: 模板组合 (Template Composition) + Feature 2.3: 动态检查项 (Dynamic Items)

-- 2.2 模板组合
ALTER TABLE insp_templates ADD COLUMN is_module TINYINT DEFAULT 0 COMMENT '是否为模块(可被其他模板引用)';
ALTER TABLE insp_templates ADD COLUMN is_composite TINYINT DEFAULT 0 COMMENT '是否为组合模板';

CREATE TABLE insp_template_module_refs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  composite_template_id BIGINT NOT NULL COMMENT '组合模板ID',
  module_template_id BIGINT NOT NULL COMMENT '被引用的模块模板ID',
  sort_order INT DEFAULT 0,
  override_config JSON COMMENT '覆盖配置(可选)',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  INDEX idx_composite (composite_template_id),
  INDEX idx_module (module_template_id)
) COMMENT='模板模块引用关系';

-- 2.3 动态检查项
ALTER TABLE insp_template_items ADD COLUMN visibility_logic JSON DEFAULT NULL COMMENT '显示条件(条件逻辑V2格式)';
ALTER TABLE insp_template_items ADD COLUMN scoring_logic JSON DEFAULT NULL COMMENT '计分条件(条件逻辑V2格式)';
