-- ==========================================
-- V7: 清理和重建楼宇管理系统
-- 目标: 统一所有楼宇管理,删除冗余表和数据
-- ==========================================

-- 步骤1: 禁用外键检查
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS=0;

-- 步骤2: 清空现有数据（测试数据可删除）
TRUNCATE TABLE classrooms;
TRUNCATE TABLE dormitories;
TRUNCATE TABLE students;
TRUNCATE TABLE class_dormitories;
TRUNCATE TABLE buildings;

-- 步骤3: 删除旧的楼宇表
DROP TABLE IF EXISTS teaching_buildings;
DROP TABLE IF EXISTS dormitory_buildings;

-- 步骤4: 删除冗余的building_name字段
ALTER TABLE dormitories DROP COLUMN building_name;

-- 步骤5: 确保字段类型正确
ALTER TABLE classrooms
    MODIFY COLUMN building_id BIGINT NOT NULL COMMENT '所属楼宇ID（关联buildings表）',
    MODIFY COLUMN floor INT NOT NULL COMMENT '楼层',
    MODIFY COLUMN room_number VARCHAR(20) NOT NULL COMMENT '房间号（如: 101, 201）';

ALTER TABLE dormitories
    MODIFY COLUMN building_id BIGINT NOT NULL COMMENT '所属楼宇ID（关联buildings表）',
    MODIFY COLUMN room_number VARCHAR(20) NOT NULL COMMENT '房间号（如: 101, 201）';

-- 步骤6: 外键约束已在V6迁移中创建,跳过此步骤

-- 恢复外键检查
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;

-- 步骤7: 唯一约束和索引已在V6迁移中创建,跳过此步骤

-- 步骤8: 统一状态值注释
ALTER TABLE buildings
    MODIFY COLUMN status TINYINT NOT NULL DEFAULT 1
    COMMENT '状态: 0-停用, 1-启用';

ALTER TABLE classrooms
    MODIFY COLUMN status TINYINT NOT NULL DEFAULT 1
    COMMENT '状态: 0-停用, 1-启用';

ALTER TABLE dormitories
    MODIFY COLUMN status TINYINT NOT NULL DEFAULT 1
    COMMENT '状态: 0-停用, 1-启用, 2-维修';

-- 步骤9: 插入示例楼宇数据（方便测试）
INSERT INTO buildings (id, building_no, building_name, building_type, total_floors, location, status, created_at, updated_at, deleted)
VALUES
    (1, '1', '第一教学楼', 1, 5, '校区东侧', 1, NOW(), NOW(), 0),
    (2, '2', '第二教学楼', 1, 6, '校区西侧', 1, NOW(), NOW(), 0),
    (3, '10', '男生宿舍楼1号', 2, 6, '宿舍区北侧', 1, NOW(), NOW(), 0),
    (4, '11', '女生宿舍楼1号', 2, 6, '宿舍区南侧', 1, NOW(), NOW(), 0),
    (5, '20', '行政办公楼', 3, 5, '校区中心', 1, NOW(), NOW(), 0);

-- 步骤10: 插入示例教室数据
INSERT INTO classrooms (id, building_id, classroom_name, classroom_code, floor, room_number, capacity, classroom_type, facilities, status, deleted, created_at, updated_at)
VALUES
    (1001, 1, '101教室', 'JXL1-101', 1, '101', 50, '普通教室', '投影仪、音响', 1, 0, NOW(), NOW()),
    (1002, 1, '102教室', 'JXL1-102', 1, '102', 50, '普通教室', '投影仪、音响', 1, 0, NOW(), NOW()),
    (1003, 1, '201教室', 'JXL1-201', 2, '201', 60, '多媒体教室', '电脑、投影仪、音响', 1, 0, NOW(), NOW()),
    (1004, 2, '101教室', 'JXL2-101', 1, '101', 45, '实验室', '实验设备', 1, 0, NOW(), NOW());

-- 步骤11: 插入示例宿舍数据
INSERT INTO dormitories (id, building_id, dormitory_no, dormitory_name, floor, room_number, room_type, occupied_beds, gender_type, status, dormitory_type, deleted, created_at, updated_at)
VALUES
    (2001, 3, 'NS1-101', '男1#101', 1, '101', 6, 0, 1, 1, 1, 0, NOW(), NOW()),
    (2002, 3, 'NS1-102', '男1#102', 1, '102', 6, 0, 1, 1, 1, 0, NOW(), NOW()),
    (2003, 3, 'NS1-201', '男1#201', 2, '201', 6, 0, 1, 1, 1, 0, NOW(), NOW()),
    (2004, 4, 'NS2-101', '女1#101', 1, '101', 6, 0, 2, 1, 1, 0, NOW(), NOW()),
    (2005, 4, 'NS2-102', '女1#102', 1, '102', 6, 0, 2, 1, 1, 0, NOW(), NOW());

-- 完成
