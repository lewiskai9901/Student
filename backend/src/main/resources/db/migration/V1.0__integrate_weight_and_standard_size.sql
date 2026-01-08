-- ========================================
-- 加权配置与标准人数融合迁移脚本
-- 版本: V1.0
-- 日期: 2025-01-16
-- 说明: 将加权配置改为方案库模式,并融合标准人数配置
-- ========================================

-- ----------------------------------------
-- 1. 调整 class_weight_configs 表结构
-- ----------------------------------------

-- 删除全局自动应用相关的字段
ALTER TABLE class_weight_configs
DROP COLUMN IF EXISTS effective_start_date,
DROP COLUMN IF EXISTS effective_end_date,
DROP COLUMN IF EXISTS apply_scope,
DROP COLUMN IF EXISTS semester_id,
DROP COLUMN IF EXISTS department_id,
DROP COLUMN IF EXISTS grade_level,
DROP COLUMN IF EXISTS class_id;

-- 添加方案库模式相关字段
ALTER TABLE class_weight_configs
ADD COLUMN config_name VARCHAR(100) NOT NULL COMMENT '方案名称(如:宿舍加权方案)' AFTER config_code,
ADD COLUMN config_description TEXT COMMENT '方案说明' AFTER config_name,
ADD COLUMN is_default TINYINT DEFAULT 0 COMMENT '是否默认方案' AFTER config_description,
ADD COLUMN visible_scope VARCHAR(20) DEFAULT 'ALL' COMMENT '可见范围(ALL/DEPARTMENT/ROLE)' AFTER is_default,
ADD COLUMN visible_department_id BIGINT COMMENT '可见部门ID(当visible_scope=DEPARTMENT时)' AFTER visible_scope,
ADD COLUMN applicable_check_types TEXT COMMENT '适用的检查类型(JSON数组,如:["DORM_HYGIENE","CLASS_HYGIENE"])' AFTER visible_department_id,
ADD COLUMN creator_id BIGINT COMMENT '创建人ID' AFTER applicable_check_types,
ADD COLUMN creator_name VARCHAR(50) COMMENT '创建人姓名' AFTER creator_id,
ADD COLUMN use_count INT DEFAULT 0 COMMENT '使用次数统计' AFTER creator_name;

-- 添加标准人数配置相关字段
ALTER TABLE class_weight_configs
ADD COLUMN standard_size_mode VARCHAR(20) DEFAULT 'FIXED' COMMENT '标准人数模式(FIXED=固定,DYNAMIC=动态平均,CUSTOM=自定义)' AFTER use_count,
ADD COLUMN standard_size INTEGER COMMENT '固定标准人数(当mode=FIXED时使用)' AFTER standard_size_mode,
ADD COLUMN custom_standard_rules TEXT COMMENT '自定义标准人数规则(JSON格式,按班级/年级/部门设置)' AFTER standard_size;

-- 添加索引
ALTER TABLE class_weight_configs
ADD INDEX idx_visible_scope (visible_scope),
ADD INDEX idx_creator (creator_id),
ADD INDEX idx_default (is_default);

-- ----------------------------------------
-- 2. 调整 daily_checks 表结构
-- ----------------------------------------

-- 添加加权方案关联字段
ALTER TABLE daily_checks
ADD COLUMN weight_config_id BIGINT COMMENT '加权方案ID(可选)' AFTER description,
ADD COLUMN enable_weight TINYINT DEFAULT 0 COMMENT '是否启用加权(0=否,1=是)' AFTER weight_config_id,
ADD COLUMN custom_standard_size INTEGER COMMENT '自定义标准人数(覆盖方案配置,仅本次检查生效)' AFTER enable_weight;

-- 添加索引
ALTER TABLE daily_checks
ADD INDEX idx_weight_config (weight_config_id);

-- ----------------------------------------
-- 3. 调整 check_record_v3 表结构
-- ----------------------------------------

-- 添加加权配置快照字段
ALTER TABLE check_record_v3
ADD COLUMN weight_mode_snapshot VARCHAR(20) COMMENT '加权模式快照' AFTER weight_enabled,
ADD COLUMN standard_size_mode_snapshot VARCHAR(20) COMMENT '标准人数模式快照' AFTER weight_mode_snapshot,
ADD COLUMN standard_size_snapshot_value INTEGER COMMENT '标准人数值快照' AFTER standard_size_mode_snapshot;

-- ----------------------------------------
-- 4. 清空测试数据(根据需求)
-- ----------------------------------------

-- 清空 class_weight_configs 表
TRUNCATE TABLE class_weight_configs;

-- 清空 class_size_standards 表(将被废弃)
TRUNCATE TABLE class_size_standards;

-- 清空 daily_checks 表
TRUNCATE TABLE daily_checks;

-- 清空 check_record_v3 及相关表
TRUNCATE TABLE check_record_v3;
TRUNCATE TABLE check_record_class_stats;
TRUNCATE TABLE check_record_category_stats;
TRUNCATE TABLE check_record_item_v3;

-- ----------------------------------------
-- 5. 插入默认加权方案示例
-- ----------------------------------------

INSERT INTO class_weight_configs (
    config_code,
    config_name,
    config_description,
    weight_mode,
    enable_weight,
    enable_weight_limit,
    min_weight,
    max_weight,
    standard_size_mode,
    standard_size,
    is_default,
    visible_scope,
    applicable_check_types,
    status,
    created_at,
    updated_at,
    deleted
) VALUES (
    'DEFAULT_STANDARD_WEIGHT',
    '默认标准加权方案',
    '适用于大多数检查场景,按标准40人计算加权系数',
    'STANDARD',
    1,
    1,
    0.5,
    1.5,
    'FIXED',
    40,
    1,
    'ALL',
    '["DORM_HYGIENE","CLASS_HYGIENE","DISCIPLINE","ATTENDANCE"]',
    1,
    NOW(),
    NOW(),
    0
);

INSERT INTO class_weight_configs (
    config_code,
    config_name,
    config_description,
    weight_mode,
    enable_weight,
    enable_weight_limit,
    min_weight,
    max_weight,
    standard_size_mode,
    is_default,
    visible_scope,
    applicable_check_types,
    status,
    created_at,
    updated_at,
    deleted
) VALUES (
    'DYNAMIC_WEIGHT',
    '动态平均加权方案',
    '根据同年级同部门的实际平均人数动态计算加权系数',
    'STANDARD',
    1,
    1,
    0.5,
    1.5,
    'DYNAMIC',
    0,
    'ALL',
    '["DORM_HYGIENE","CLASS_HYGIENE"]',
    1,
    NOW(),
    NOW(),
    0
);

-- ----------------------------------------
-- 6. 备注说明
-- ----------------------------------------

-- class_size_standards 表将被废弃,但暂时保留以备查询历史数据
-- 新的加权方案将标准人数配置集成在 class_weight_configs 表中
-- 所有历史检查记录的快照数据保持不变

COMMIT;
