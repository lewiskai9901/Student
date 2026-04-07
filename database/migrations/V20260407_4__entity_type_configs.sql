-- 统一实体类型注册表
-- 替代 org_unit_types，支持 ORG_UNIT / PLACE / USER 三种实体类型

CREATE TABLE IF NOT EXISTS entity_type_configs (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    entity_type VARCHAR(20) NOT NULL COMMENT 'ORG_UNIT/PLACE/USER',
    type_code VARCHAR(50) NOT NULL,
    type_name VARCHAR(50) NOT NULL,
    category VARCHAR(30) COMMENT '大类',
    parent_type_code VARCHAR(50),
    allowed_child_type_codes JSON,
    metadata_schema JSON NOT NULL DEFAULT (JSON_OBJECT('fields', JSON_ARRAY())),
    features JSON DEFAULT (JSON_OBJECT()),
    ui_config JSON DEFAULT (JSON_OBJECT()),
    is_plugin_registered TINYINT DEFAULT 0 COMMENT '是否由插件注册',
    plugin_class VARCHAR(200) COMMENT '插件实现类全名',
    is_system TINYINT DEFAULT 0,
    is_enabled TINYINT DEFAULT 1,
    sort_order INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_entity_type (entity_type, type_code)
) COMMENT '统一实体类型注册表';

-- users 表加 attributes JSON 字段
SET @col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='users' AND COLUMN_NAME='attributes');
SET @s = IF(@col=0, 'ALTER TABLE users ADD COLUMN attributes JSON COMMENT ''扩展属性''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
