-- ========================================
-- RANGE_AVERAGE 模式支持迁移脚本
-- 版本: V1.1
-- 日期: 2025-01-16
-- 说明: 添加范围平均人数模式支持,重命名 DYNAMIC 为 TARGET_AVERAGE
-- ========================================

-- ----------------------------------------
-- 1. 为 class_weight_configs 表添加 RANGE_AVERAGE 相关字段
-- ----------------------------------------

-- 添加范围选择字段
ALTER TABLE class_weight_configs
ADD COLUMN range_departments TEXT COMMENT '选择的部门范围(JSON数组,如:[1,2,3])' AFTER custom_standard_rules,
ADD COLUMN range_grades TEXT COMMENT '选择的年级范围(JSON数组,如:["2024","2023"])' AFTER range_departments,
ADD COLUMN range_classes TEXT COMMENT '选择的班级范围(JSON数组,如:[101,102,103])' AFTER range_grades;

-- 添加索引
ALTER TABLE class_weight_configs
ADD INDEX idx_standard_size_mode (standard_size_mode);

-- ----------------------------------------
-- 2. 更新已有数据:将 DYNAMIC 改名为 TARGET_AVERAGE
-- ----------------------------------------

-- 更新 class_weight_configs 表中的模式名称
UPDATE class_weight_configs
SET standard_size_mode = 'TARGET_AVERAGE'
WHERE standard_size_mode = 'DYNAMIC';

-- 更新 check_records_v3 表中的快照模式名称
UPDATE check_records_v3
SET standard_size_mode_snapshot = 'TARGET_AVERAGE'
WHERE standard_size_mode_snapshot = 'DYNAMIC';

-- ----------------------------------------
-- 3. 添加注释说明
-- ----------------------------------------

-- 标准人数模式说明:
-- FIXED: 固定标准人数(使用 standard_size 字段)
-- TARGET_AVERAGE: 目标平均人数(根据检查目标自动计算)
-- RANGE_AVERAGE: 范围平均人数(根据预设范围计算,使用 range_departments/range_grades/range_classes 字段)
-- CUSTOM: 自定义规则(使用 custom_standard_rules 字段,已废弃)

-- ----------------------------------------
-- 4. 验证数据一致性
-- ----------------------------------------

-- 检查是否还有 DYNAMIC 模式的数据
SELECT COUNT(*) as remaining_dynamic_count
FROM class_weight_configs
WHERE standard_size_mode = 'DYNAMIC';

SELECT COUNT(*) as remaining_dynamic_snapshot_count
FROM check_records_v3
WHERE standard_size_mode_snapshot = 'DYNAMIC';

COMMIT;
