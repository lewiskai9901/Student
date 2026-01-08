-- ================================================================
-- 楼宇管理系统重构
-- 创建统一的楼宇管理表,整合教学楼和宿舍楼管理
-- ================================================================

-- 创建楼宇表 (buildings)
CREATE TABLE IF NOT EXISTS buildings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '楼宇ID',
    building_no VARCHAR(20) NOT NULL UNIQUE COMMENT '楼号(纯数字,如: 1, 2, 3)',
    building_name VARCHAR(100) NOT NULL COMMENT '楼宇名称',
    building_type TINYINT NOT NULL COMMENT '楼宇类型: 1-教学楼, 2-宿舍楼, 3-办公楼',
    total_floors INT NOT NULL DEFAULT 1 COMMENT '总楼层数',
    location VARCHAR(200) COMMENT '地理位置',
    construction_year INT COMMENT '建造年份',
    manager_id BIGINT COMMENT '楼管理员ID',
    description TEXT COMMENT '楼宇描述',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-停用, 1-启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    INDEX idx_building_no (building_no),
    INDEX idx_building_type (building_type),
    INDEX idx_status (status),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='楼宇表';

-- 迁移现有教学楼数据 (从 teaching_buildings 表)
INSERT INTO buildings (building_no, building_name, building_type, total_floors, location, status, created_at, updated_at, deleted)
SELECT
    CAST(id AS CHAR) as building_no,
    building_name,
    1 as building_type,
    COALESCE(floor_count, 5) as total_floors,
    location,
    status,
    created_at,
    updated_at,
    deleted
FROM teaching_buildings
WHERE id IS NOT NULL;

-- 为宿舍楼创建记录 (从现有宿舍的building_name中提取唯一楼栋)
INSERT INTO buildings (building_no, building_name, building_type, total_floors, status)
SELECT DISTINCT
    SUBSTRING_INDEX(building_name, '栋', 1) as building_no,
    building_name,
    2 as building_type,
    10 as total_floors,
    1 as status
FROM dormitories
WHERE building_name IS NOT NULL
  AND building_name != ''
  AND NOT EXISTS (
      SELECT 1 FROM buildings b
      WHERE b.building_name = dormitories.building_name
  );

-- 修改 classrooms 表
-- 1. 检查是否已经有 building_id 列
SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'classrooms'
    AND COLUMN_NAME = 'building_id'
);

-- 2. 如果没有 building_id 列,则添加
SET @sql = IF(@column_exists = 0,
    'ALTER TABLE classrooms ADD COLUMN building_id BIGINT COMMENT ''楼宇ID'' AFTER id',
    'SELECT ''Column building_id already exists'' as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 更新 classrooms 表的 building_id (关联到新的 buildings 表)
UPDATE classrooms c
INNER JOIN buildings b ON c.building_id = b.id AND b.building_type = 1
SET c.building_id = b.id
WHERE c.building_id IS NOT NULL;

-- 4. 添加房间号唯一性约束 (同一楼内房间号不能重复)
-- 先删除可能存在的旧约束
SET @constraint_exists = (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'classrooms'
    AND CONSTRAINT_NAME = 'uk_building_room'
);

SET @sql = IF(@constraint_exists > 0,
    'ALTER TABLE classrooms DROP INDEX uk_building_room',
    'SELECT ''Constraint uk_building_room does not exist'' as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加新约束
ALTER TABLE classrooms
ADD UNIQUE INDEX uk_building_room (building_id, room_number, deleted);

-- 修改 dormitories 表
-- 1. 更新 building_id 字段 (关联到新的 buildings 表)
UPDATE dormitories d
INNER JOIN buildings b ON d.building_name = b.building_name AND b.building_type = 2
SET d.building_id = b.id
WHERE d.building_name IS NOT NULL;

-- 2. 添加房间号字段 (如果不存在)
SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'dormitories'
    AND COLUMN_NAME = 'room_number'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE dormitories ADD COLUMN room_number VARCHAR(20) COMMENT ''房间号(如: 101, 201)'' AFTER floor_number',
    'SELECT ''Column room_number already exists'' as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 根据现有 dormitory_no 生成房间号 (提取数字部分)
UPDATE dormitories
SET room_number = REGEXP_REPLACE(dormitory_no, '[^0-9]', '')
WHERE room_number IS NULL AND dormitory_no IS NOT NULL;

-- 4. 添加房间号唯一性约束 (同一楼内房间号不能重复)
SET @constraint_exists = (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'dormitories'
    AND CONSTRAINT_NAME = 'uk_building_room_dormitory'
);

SET @sql = IF(@constraint_exists > 0,
    'ALTER TABLE dormitories DROP INDEX uk_building_room_dormitory',
    'SELECT ''Constraint uk_building_room_dormitory does not exist'' as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加新约束
ALTER TABLE dormitories
ADD UNIQUE INDEX uk_building_room_dormitory (building_id, room_number, deleted);

-- 插入权限数据
-- 楼宇管理权限
INSERT INTO permissions (id, permission_code, permission_name, parent_id, permission_type, sort_order, status, created_at, updated_at, deleted)
VALUES
    -- 主菜单
    (IFNULL((SELECT MAX(id) FROM permissions p) + 1, 1000), 'system:building', '楼宇管理', NULL, 1, 100, 1, NOW(), NOW(), 0),
    -- 子权限
    (IFNULL((SELECT MAX(id) FROM permissions p) + 2, 1001), 'system:building:view', '查看楼宇', (SELECT id FROM permissions WHERE permission_code = 'system:building'), 2, 1, 1, NOW(), NOW(), 0),
    (IFNULL((SELECT MAX(id) FROM permissions p) + 3, 1002), 'system:building:add', '添加楼宇', (SELECT id FROM permissions WHERE permission_code = 'system:building'), 2, 2, 1, NOW(), NOW(), 0),
    (IFNULL((SELECT MAX(id) FROM permissions p) + 4, 1003), 'system:building:edit', '编辑楼宇', (SELECT id FROM permissions WHERE permission_code = 'system:building'), 2, 3, 1, NOW(), NOW(), 0),
    (IFNULL((SELECT MAX(id) FROM permissions p) + 5, 1004), 'system:building:delete', '删除楼宇', (SELECT id FROM permissions WHERE permission_code = 'system:building'), 2, 4, 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name);

-- 为管理员角色分配楼宇管理权限
INSERT INTO role_permissions (role_id, permission_id)
SELECT 1, id FROM permissions
WHERE permission_code LIKE 'system:building%'
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = 1 AND rp.permission_id = permissions.id
);

COMMIT;
