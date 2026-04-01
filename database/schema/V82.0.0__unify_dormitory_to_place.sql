-- =====================================================
-- V82.0.0: 统一宿舍管理到场所管理体系
-- 废弃旧的 dormitories / student_dormitories 表
-- 废弃 students.dormitory_id / students.bed_number 字段
-- 所有宿舍数据通过 universal_places + place_occupants 管理
-- =====================================================

-- 1. 添加宿舍专用场所类型（如果不存在）
INSERT IGNORE INTO place_types (type_code, type_name, category, description, occupiable, has_capacity, can_have_children, max_depth, icon, sort_order, status, created_at, updated_at)
VALUES
('DORM_BUILDING', '宿舍楼', 'DORMITORY', '学生宿舍楼', 0, 0, 1, 3, 'Building', 10, 1, NOW(), NOW()),
('DORM_FLOOR', '宿舍楼层', 'DORMITORY', '宿舍楼层', 0, 0, 1, 0, 'Layers', 11, 1, NOW(), NOW()),
('DORM_ROOM', '宿舍房间', 'DORMITORY', '学生宿舍房间，可入住', 1, 1, 0, 0, 'Bed', 12, 1, NOW(), NOW());

-- 2. 删除 students 表中的旧宿舍字段（开发阶段不保留旧列）
ALTER TABLE students DROP COLUMN IF EXISTS dormitory_id;
ALTER TABLE students DROP COLUMN IF EXISTS bed_number;

-- 3. 废弃旧的 place_occupant 表（单数，旧版），数据已迁移到 place_occupants（复数，新版）
-- 开发阶段直接删除
DROP TABLE IF EXISTS place_occupant;

-- 4. 废弃旧的宿舍表
DROP TABLE IF EXISTS student_dormitories;
DROP TABLE IF EXISTS dormitories;
