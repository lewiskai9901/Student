-- =============================================================================
-- V20260417_1 扩充 entity_type_configs 表结构，为 P5 收敛做准备
--
-- 目标: 把 user_types / place_types / org_unit_types 的独有顶列并入 entity_type_configs
-- 策略: 通用列进顶层，领域布尔进 features JSON，场所特有进 ui_config JSON
-- =============================================================================

-- 1. 补通用顶列
ALTER TABLE entity_type_configs
    ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID' AFTER id,
    ADD COLUMN description VARCHAR(500) DEFAULT NULL COMMENT '类型描述' AFTER type_name,
    ADD COLUMN icon VARCHAR(50) DEFAULT NULL COMMENT '图标名' AFTER description,
    ADD COLUMN max_depth INT DEFAULT NULL COMMENT '允许的最大子级深度' AFTER allowed_child_type_codes,
    ADD COLUMN default_role_codes JSON DEFAULT NULL COMMENT '关联默认角色编码 (user)' AFTER max_depth,
    ADD COLUMN default_user_type_codes JSON DEFAULT NULL COMMENT '关联默认用户类型编码 (org/place)' AFTER default_role_codes,
    ADD COLUMN default_org_type_codes JSON DEFAULT NULL COMMENT '关联默认组织类型编码 (user/place)' AFTER default_user_type_codes,
    ADD COLUMN default_place_type_codes JSON DEFAULT NULL COMMENT '关联默认场所类型编码 (org/user)' AFTER default_org_type_codes;

-- 2. 保险: 现有行 tenant_id 明确填 1 (DEFAULT 已应用，但显式兜底)
UPDATE entity_type_configs SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;

-- 3. UNIQUE KEY 加入 tenant_id 作为前缀
ALTER TABLE entity_type_configs DROP KEY uk_entity_type;
ALTER TABLE entity_type_configs ADD UNIQUE KEY uk_tenant_entity_type (tenant_id, entity_type, type_code);

-- 4. 常用索引
CREATE INDEX idx_entity_type_configs_tenant ON entity_type_configs(tenant_id);
CREATE INDEX idx_entity_type_configs_parent ON entity_type_configs(parent_type_code);
CREATE INDEX idx_entity_type_configs_enabled ON entity_type_configs(is_enabled);
