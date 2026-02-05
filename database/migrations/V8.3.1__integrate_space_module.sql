-- =============================================
-- V8.3.1: 场所模块改造 - 集成类型系统和多归属关系
-- =============================================

-- 1. 添加场所类型编码字段（引用 space_types 表）
ALTER TABLE space
ADD COLUMN space_type_code VARCHAR(50) COMMENT '场所类型编码（引用space_types）' AFTER space_type;

-- 2. 添加主组织关系ID字段（引用 space_org_relations 表）
ALTER TABLE space
ADD COLUMN primary_org_relation_id BIGINT COMMENT '主组织关系ID（引用space_org_relations）' AFTER org_unit_id;

-- 3. 添加索引
ALTER TABLE space
ADD INDEX idx_space_type_code (space_type_code);

-- 4. 数据迁移：将旧的 spaceType+roomType 组合转换为 space_type_code
-- CAMPUS 类型
UPDATE space SET space_type_code = 'CAMPUS' WHERE space_type = 'CAMPUS';

-- BUILDING 类型根据 building_type 细分
UPDATE space SET space_type_code = 'TEACHING' WHERE space_type = 'BUILDING' AND building_type = 'TEACHING';
UPDATE space SET space_type_code = 'DORMITORY' WHERE space_type = 'BUILDING' AND building_type = 'DORMITORY';
UPDATE space SET space_type_code = 'OFFICE' WHERE space_type = 'BUILDING' AND building_type = 'OFFICE';
UPDATE space SET space_type_code = 'ACTIVITY' WHERE space_type = 'BUILDING' AND building_type = 'ACTIVITY';
UPDATE space SET space_type_code = 'BUILDING' WHERE space_type = 'BUILDING' AND space_type_code IS NULL;

-- FLOOR 类型
UPDATE space SET space_type_code = 'FLOOR' WHERE space_type = 'FLOOR';

-- ROOM 类型根据 room_type 细分
UPDATE space SET space_type_code = 'CLASSROOM' WHERE space_type = 'ROOM' AND room_type = 'CLASSROOM';
UPDATE space SET space_type_code = 'LECTURE_HALL' WHERE space_type = 'ROOM' AND room_type = 'LECTURE_HALL';
UPDATE space SET space_type_code = 'LAB' WHERE space_type = 'ROOM' AND room_type = 'LAB';
UPDATE space SET space_type_code = 'COMPUTER_ROOM' WHERE space_type = 'ROOM' AND room_type = 'COMPUTER_ROOM';
UPDATE space SET space_type_code = 'DORM_ROOM' WHERE space_type = 'ROOM' AND room_type = 'DORM_ROOM';
UPDATE space SET space_type_code = 'OFFICE_ROOM' WHERE space_type = 'ROOM' AND room_type = 'OFFICE';
UPDATE space SET space_type_code = 'MEETING_ROOM' WHERE space_type = 'ROOM' AND room_type = 'MEETING';
UPDATE space SET space_type_code = 'STORAGE' WHERE space_type = 'ROOM' AND room_type = 'STORAGE';
UPDATE space SET space_type_code = 'ROOM' WHERE space_type = 'ROOM' AND space_type_code IS NULL;

-- 5. 将现有的 org_unit_id 迁移到 space_org_relations 表
-- 为每个有 org_unit_id 的场所创建主归属关系
INSERT INTO space_org_relations (space_id, org_unit_id, relation_type, is_primary, can_use, can_manage, can_assign, can_inspect, weight_ratio, created_by)
SELECT
    id,
    org_unit_id,
    'PRIMARY',
    TRUE,
    TRUE,
    TRUE,
    TRUE,
    TRUE,
    100.00,
    1
FROM space
WHERE org_unit_id IS NOT NULL AND deleted = 0
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- 6. 更新 primary_org_relation_id
UPDATE space s
INNER JOIN space_org_relations r ON s.id = r.space_id AND r.is_primary = 1 AND r.deleted = 0
SET s.primary_org_relation_id = r.id
WHERE s.deleted = 0;
