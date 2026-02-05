-- ============================================================================
-- 场所管理测试数据
-- 包含：校区、宿舍楼、教学楼、楼层、宿舍房间、教室
-- ============================================================================

-- 清理已有测试数据（可选）
-- DELETE FROM space WHERE deleted = 0;

-- ============================================================================
-- 1. 校区数据
-- ============================================================================
INSERT INTO space (id, space_code, space_name, space_type, parent_id, path, level, capacity, status, deleted) VALUES
(1, 'CAMPUS-001', '主校区', 'CAMPUS', NULL, '/1', 1, 5000, 1, 0),
(2, 'CAMPUS-002', '南校区', 'CAMPUS', NULL, '/2', 1, 3000, 1, 0);

-- ============================================================================
-- 2. 宿舍楼数据 (building_type = 'DORMITORY')
-- ============================================================================
INSERT INTO space (id, space_code, space_name, space_type, building_type, building_no, parent_id, path, level, campus_id, capacity, status, deleted) VALUES
(10, 'DORM-BLD-001', '宿舍1号楼', 'BUILDING', 'DORMITORY', '1', 1, '/1/10', 2, 1, 400, 1, 0),
(11, 'DORM-BLD-002', '宿舍2号楼', 'BUILDING', 'DORMITORY', '2', 1, '/1/11', 2, 1, 400, 1, 0),
(12, 'DORM-BLD-003', '宿舍3号楼', 'BUILDING', 'DORMITORY', '3', 1, '/1/12', 2, 1, 400, 1, 0),
(13, 'DORM-BLD-004', '南区宿舍楼', 'BUILDING', 'DORMITORY', '1', 2, '/2/13', 2, 2, 300, 1, 0);

-- ============================================================================
-- 3. 教学楼数据 (building_type = 'TEACHING')
-- ============================================================================
INSERT INTO space (id, space_code, space_name, space_type, building_type, building_no, parent_id, path, level, campus_id, capacity, status, deleted) VALUES
(20, 'TEACH-BLD-001', '教学1号楼', 'BUILDING', 'TEACHING', 'A', 1, '/1/20', 2, 1, 1000, 1, 0),
(21, 'TEACH-BLD-002', '教学2号楼', 'BUILDING', 'TEACHING', 'B', 1, '/1/21', 2, 1, 800, 1, 0),
(22, 'TEACH-BLD-003', '实验楼', 'BUILDING', 'TEACHING', 'C', 1, '/1/22', 2, 1, 500, 1, 0);

-- ============================================================================
-- 4. 宿舍楼层数据
-- ============================================================================
-- 1号楼楼层
INSERT INTO space (id, space_code, space_name, space_type, parent_id, path, level, campus_id, building_id, floor_number, capacity, status, deleted) VALUES
(100, 'DORM-1-F1', '1号楼1层', 'FLOOR', 10, '/1/10/100', 3, 1, 10, 1, 80, 1, 0),
(101, 'DORM-1-F2', '1号楼2层', 'FLOOR', 10, '/1/10/101', 3, 1, 10, 2, 80, 1, 0),
(102, 'DORM-1-F3', '1号楼3层', 'FLOOR', 10, '/1/10/102', 3, 1, 10, 3, 80, 1, 0),
(103, 'DORM-1-F4', '1号楼4层', 'FLOOR', 10, '/1/10/103', 3, 1, 10, 4, 80, 1, 0),
(104, 'DORM-1-F5', '1号楼5层', 'FLOOR', 10, '/1/10/104', 3, 1, 10, 5, 80, 1, 0);

-- 2号楼楼层
INSERT INTO space (id, space_code, space_name, space_type, parent_id, path, level, campus_id, building_id, floor_number, capacity, status, deleted) VALUES
(110, 'DORM-2-F1', '2号楼1层', 'FLOOR', 11, '/1/11/110', 3, 1, 11, 1, 80, 1, 0),
(111, 'DORM-2-F2', '2号楼2层', 'FLOOR', 11, '/1/11/111', 3, 1, 11, 2, 80, 1, 0),
(112, 'DORM-2-F3', '2号楼3层', 'FLOOR', 11, '/1/11/112', 3, 1, 11, 3, 80, 1, 0),
(113, 'DORM-2-F4', '2号楼4层', 'FLOOR', 11, '/1/11/113', 3, 1, 11, 4, 80, 1, 0),
(114, 'DORM-2-F5', '2号楼5层', 'FLOOR', 11, '/1/11/114', 3, 1, 11, 5, 80, 1, 0);

-- 3号楼楼层
INSERT INTO space (id, space_code, space_name, space_type, parent_id, path, level, campus_id, building_id, floor_number, capacity, status, deleted) VALUES
(120, 'DORM-3-F1', '3号楼1层', 'FLOOR', 12, '/1/12/120', 3, 1, 12, 1, 80, 1, 0),
(121, 'DORM-3-F2', '3号楼2层', 'FLOOR', 12, '/1/12/121', 3, 1, 12, 2, 80, 1, 0),
(122, 'DORM-3-F3', '3号楼3层', 'FLOOR', 12, '/1/12/122', 3, 1, 12, 3, 80, 1, 0);

-- ============================================================================
-- 5. 宿舍房间数据 (room_type = 'DORMITORY')
-- ============================================================================
-- 1号楼1层房间
INSERT INTO space (id, space_code, space_name, space_type, room_type, room_no, parent_id, path, level, campus_id, building_id, floor_number, capacity, current_occupancy, gender_type, status, deleted) VALUES
(1001, 'DORM-1-101', '1号楼101室', 'ROOM', 'DORMITORY', '101', 100, '/1/10/100/1001', 4, 1, 10, 1, 8, 6, 1, 1, 0),
(1002, 'DORM-1-102', '1号楼102室', 'ROOM', 'DORMITORY', '102', 100, '/1/10/100/1002', 4, 1, 10, 1, 8, 8, 1, 1, 0),
(1003, 'DORM-1-103', '1号楼103室', 'ROOM', 'DORMITORY', '103', 100, '/1/10/100/1003', 4, 1, 10, 1, 8, 7, 1, 1, 0),
(1004, 'DORM-1-104', '1号楼104室', 'ROOM', 'DORMITORY', '104', 100, '/1/10/100/1004', 4, 1, 10, 1, 8, 5, 1, 1, 0),
(1005, 'DORM-1-105', '1号楼105室', 'ROOM', 'DORMITORY', '105', 100, '/1/10/100/1005', 4, 1, 10, 1, 8, 4, 1, 1, 0);

-- 1号楼2层房间
INSERT INTO space (id, space_code, space_name, space_type, room_type, room_no, parent_id, path, level, campus_id, building_id, floor_number, capacity, current_occupancy, gender_type, status, deleted) VALUES
(1011, 'DORM-1-201', '1号楼201室', 'ROOM', 'DORMITORY', '201', 101, '/1/10/101/1011', 4, 1, 10, 2, 8, 8, 2, 1, 0),
(1012, 'DORM-1-202', '1号楼202室', 'ROOM', 'DORMITORY', '202', 101, '/1/10/101/1012', 4, 1, 10, 2, 8, 7, 2, 1, 0),
(1013, 'DORM-1-203', '1号楼203室', 'ROOM', 'DORMITORY', '203', 101, '/1/10/101/1013', 4, 1, 10, 2, 8, 6, 2, 1, 0),
(1014, 'DORM-1-204', '1号楼204室', 'ROOM', 'DORMITORY', '204', 101, '/1/10/101/1014', 4, 1, 10, 2, 8, 8, 2, 1, 0),
(1015, 'DORM-1-205', '1号楼205室', 'ROOM', 'DORMITORY', '205', 101, '/1/10/101/1015', 4, 1, 10, 2, 8, 5, 2, 1, 0);

-- 2号楼房间
INSERT INTO space (id, space_code, space_name, space_type, room_type, room_no, parent_id, path, level, campus_id, building_id, floor_number, capacity, current_occupancy, gender_type, status, deleted) VALUES
(1101, 'DORM-2-101', '2号楼101室', 'ROOM', 'DORMITORY', '101', 110, '/1/11/110/1101', 4, 1, 11, 1, 6, 5, 1, 1, 0),
(1102, 'DORM-2-102', '2号楼102室', 'ROOM', 'DORMITORY', '102', 110, '/1/11/110/1102', 4, 1, 11, 1, 6, 6, 1, 1, 0),
(1103, 'DORM-2-103', '2号楼103室', 'ROOM', 'DORMITORY', '103', 110, '/1/11/110/1103', 4, 1, 11, 1, 6, 4, 1, 1, 0),
(1104, 'DORM-2-201', '2号楼201室', 'ROOM', 'DORMITORY', '201', 111, '/1/11/111/1104', 4, 1, 11, 2, 6, 6, 2, 1, 0),
(1105, 'DORM-2-202', '2号楼202室', 'ROOM', 'DORMITORY', '202', 111, '/1/11/111/1105', 4, 1, 11, 2, 6, 5, 2, 1, 0);

-- 3号楼房间
INSERT INTO space (id, space_code, space_name, space_type, room_type, room_no, parent_id, path, level, campus_id, building_id, floor_number, capacity, current_occupancy, gender_type, status, deleted) VALUES
(1201, 'DORM-3-101', '3号楼101室', 'ROOM', 'DORMITORY', '101', 120, '/1/12/120/1201', 4, 1, 12, 1, 4, 3, 0, 1, 0),
(1202, 'DORM-3-102', '3号楼102室', 'ROOM', 'DORMITORY', '102', 120, '/1/12/120/1202', 4, 1, 12, 1, 4, 4, 0, 1, 0),
(1203, 'DORM-3-103', '3号楼103室', 'ROOM', 'DORMITORY', '103', 120, '/1/12/120/1203', 4, 1, 12, 1, 4, 2, 0, 1, 0);

-- ============================================================================
-- 6. 教学楼楼层数据
-- ============================================================================
INSERT INTO space (id, space_code, space_name, space_type, parent_id, path, level, campus_id, building_id, floor_number, capacity, status, deleted) VALUES
(200, 'TEACH-A-F1', '教学楼A栋1层', 'FLOOR', 20, '/1/20/200', 3, 1, 20, 1, 300, 1, 0),
(201, 'TEACH-A-F2', '教学楼A栋2层', 'FLOOR', 20, '/1/20/201', 3, 1, 20, 2, 300, 1, 0),
(202, 'TEACH-A-F3', '教学楼A栋3层', 'FLOOR', 20, '/1/20/202', 3, 1, 20, 3, 300, 1, 0),
(210, 'TEACH-B-F1', '教学楼B栋1层', 'FLOOR', 21, '/1/21/210', 3, 1, 21, 1, 250, 1, 0),
(211, 'TEACH-B-F2', '教学楼B栋2层', 'FLOOR', 21, '/1/21/211', 3, 1, 21, 2, 250, 1, 0);

-- ============================================================================
-- 7. 教室数据 (room_type = 'CLASSROOM')
-- ============================================================================
INSERT INTO space (id, space_code, space_name, space_type, room_type, room_no, parent_id, path, level, campus_id, building_id, floor_number, capacity, status, deleted) VALUES
(2001, 'CLASS-A-101', 'A101教室', 'ROOM', 'CLASSROOM', '101', 200, '/1/20/200/2001', 4, 1, 20, 1, 60, 1, 0),
(2002, 'CLASS-A-102', 'A102教室', 'ROOM', 'CLASSROOM', '102', 200, '/1/20/200/2002', 4, 1, 20, 1, 60, 1, 0),
(2003, 'CLASS-A-103', 'A103阶梯教室', 'ROOM', 'CLASSROOM', '103', 200, '/1/20/200/2003', 4, 1, 20, 1, 150, 1, 0),
(2004, 'CLASS-A-201', 'A201教室', 'ROOM', 'CLASSROOM', '201', 201, '/1/20/201/2004', 4, 1, 20, 2, 60, 1, 0),
(2005, 'CLASS-A-202', 'A202教室', 'ROOM', 'CLASSROOM', '202', 201, '/1/20/201/2005', 4, 1, 20, 2, 60, 1, 0),
(2006, 'CLASS-A-301', 'A301多媒体教室', 'ROOM', 'CLASSROOM', '301', 202, '/1/20/202/2006', 4, 1, 20, 3, 100, 1, 0),
(2007, 'CLASS-B-101', 'B101教室', 'ROOM', 'CLASSROOM', '101', 210, '/1/21/210/2007', 4, 1, 21, 1, 50, 1, 0),
(2008, 'CLASS-B-102', 'B102教室', 'ROOM', 'CLASSROOM', '102', 210, '/1/21/210/2008', 4, 1, 21, 1, 50, 1, 0);

-- ============================================================================
-- 8. 实验室数据 (room_type = 'LAB')
-- ============================================================================
INSERT INTO space (id, space_code, space_name, space_type, room_type, room_no, parent_id, path, level, campus_id, building_id, floor_number, capacity, status, deleted) VALUES
(3001, 'LAB-C-101', '计算机实验室1', 'ROOM', 'LAB', '101', 22, '/1/22/3001', 3, 1, 22, 1, 40, 1, 0),
(3002, 'LAB-C-102', '计算机实验室2', 'ROOM', 'LAB', '102', 22, '/1/22/3002', 3, 1, 22, 1, 40, 1, 0),
(3003, 'LAB-C-201', '物理实验室', 'ROOM', 'LAB', '201', 22, '/1/22/3003', 3, 1, 22, 2, 30, 1, 0),
(3004, 'LAB-C-202', '化学实验室', 'ROOM', 'LAB', '202', 22, '/1/22/3004', 3, 1, 22, 2, 30, 1, 0);

-- ============================================================================
-- 验证数据
-- ============================================================================
-- SELECT space_type, COUNT(*) FROM space WHERE deleted = 0 GROUP BY space_type;
-- SELECT * FROM space WHERE deleted = 0 ORDER BY path;
