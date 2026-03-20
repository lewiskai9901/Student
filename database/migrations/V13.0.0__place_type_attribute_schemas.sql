-- V13.0.0: 为预置场所类型填充默认 attribute_schema
-- 利用已有的 place_types.attribute_schema 列存储类型级动态属性定义
-- 注意：building_no/building_usage/room_number/room_usage/gender 等已由具体类型名称或通用字段替代
SET NAMES utf8mb4;

-- SITE（校区/园区）
UPDATE place_types SET attribute_schema = JSON_OBJECT(
    'fields', JSON_ARRAY(
        JSON_OBJECT('key', 'address', 'label', '地址', 'type', 'string', 'required', false, 'placeholder', '详细地址', 'maxLength', 200, 'showInTree', false, 'showInDetail', true, 'sortOrder', 1, 'builtIn', true),
        JSON_OBJECT('key', 'postal_code', 'label', '邮编', 'type', 'string', 'required', false, 'placeholder', '如: 100000', 'maxLength', 10, 'pattern', '^[0-9]{6}$', 'showInTree', false, 'showInDetail', true, 'sortOrder', 2, 'builtIn', true),
        JSON_OBJECT('key', 'area', 'label', '面积(m²)', 'type', 'number', 'required', false, 'min', 0, 'step', 0.1, 'precision', 1, 'showInTree', false, 'showInDetail', true, 'sortOrder', 3, 'builtIn', true),
        JSON_OBJECT('key', 'contact_phone', 'label', '联系电话', 'type', 'string', 'required', false, 'placeholder', '联系电话', 'maxLength', 20, 'showInTree', false, 'showInDetail', true, 'sortOrder', 4, 'builtIn', true)
    )
) WHERE type_code = 'SITE' AND deleted = 0;

-- BUILDING（楼栋）— 移除 building_no（由 placeCode 替代）、building_usage（由具体类型名替代）
UPDATE place_types SET attribute_schema = JSON_OBJECT(
    'fields', JSON_ARRAY(
        JSON_OBJECT('key', 'total_floors', 'label', '总层数', 'type', 'number', 'required', false, 'min', 1, 'max', 200, 'showInTree', false, 'showInDetail', true, 'sortOrder', 1, 'builtIn', true),
        JSON_OBJECT('key', 'built_year', 'label', '建造年份', 'type', 'number', 'required', false, 'min', 1900, 'max', 2100, 'showInTree', false, 'showInDetail', true, 'sortOrder', 2, 'builtIn', true),
        JSON_OBJECT('key', 'area', 'label', '面积(m²)', 'type', 'number', 'required', false, 'min', 0, 'step', 0.1, 'precision', 1, 'showInTree', false, 'showInDetail', true, 'sortOrder', 3, 'builtIn', true)
    )
) WHERE type_code = 'BUILDING' AND deleted = 0;

-- FLOOR（楼层）— 移除 floor_number（由 placeCode 替代）
UPDATE place_types SET attribute_schema = JSON_OBJECT(
    'fields', JSON_ARRAY(
        JSON_OBJECT('key', 'area', 'label', '面积(m²)', 'type', 'number', 'required', false, 'min', 0, 'step', 0.1, 'precision', 1, 'showInTree', false, 'showInDetail', true, 'sortOrder', 1, 'builtIn', true)
    )
) WHERE type_code = 'FLOOR' AND deleted = 0;

-- ROOM（房间）— 移除 room_number（由 placeCode 替代）、room_usage（由具体类型名替代）、gender（提升为通用字段）
UPDATE place_types SET attribute_schema = JSON_OBJECT(
    'fields', JSON_ARRAY(
        JSON_OBJECT('key', 'area', 'label', '面积(m²)', 'type', 'number', 'required', false, 'min', 0, 'step', 0.1, 'precision', 1, 'showInTree', false, 'showInDetail', true, 'sortOrder', 1, 'builtIn', true)
    )
) WHERE type_code = 'ROOM' AND deleted = 0;

-- AREA（区域）— 移除 address（重复，SITE 已有）
UPDATE place_types SET attribute_schema = JSON_OBJECT(
    'fields', JSON_ARRAY(
        JSON_OBJECT('key', 'area', 'label', '面积(m²)', 'type', 'number', 'required', false, 'min', 0, 'step', 0.1, 'precision', 1, 'showInTree', false, 'showInDetail', true, 'sortOrder', 1, 'builtIn', true),
        JSON_OBJECT('key', 'usage_desc', 'label', '用途说明', 'type', 'textarea', 'required', false, 'rows', 3, 'maxLength', 500, 'showInTree', false, 'showInDetail', true, 'sortOrder', 2, 'builtIn', true)
    )
) WHERE type_code = 'AREA' AND deleted = 0;

-- POINT（点位，原 STATION）— 移除 station_no（由 placeCode 替代）
UPDATE place_types SET attribute_schema = JSON_OBJECT(
    'fields', JSON_ARRAY(
        JSON_OBJECT('key', 'equipment_desc', 'label', '设备说明', 'type', 'textarea', 'required', false, 'rows', 3, 'maxLength', 500, 'showInTree', false, 'showInDetail', true, 'sortOrder', 1, 'builtIn', true)
    )
) WHERE type_code IN ('STATION', 'POINT') AND deleted = 0;
