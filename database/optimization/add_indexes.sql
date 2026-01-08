-- 数据库索引优化脚本
-- 创建时间: 2025-11-18
-- 说明: 为常用查询字段添加索引，提升查询性能

USE student_management;

-- 学生表索引
CREATE INDEX idx_student_no ON students(student_no);
CREATE INDEX idx_class_id ON students(class_id);
CREATE INDEX idx_user_id ON students(user_id);

-- 班级表索引
CREATE INDEX idx_grade_id ON classes(grade_id);
CREATE INDEX idx_major_direction_id ON classes(major_direction_id);

-- 检查记录V3索引
CREATE INDEX idx_check_date ON check_records_v3(check_date);
CREATE INDEX idx_semester_id ON check_records_v3(semester_id);
CREATE INDEX idx_status_v3 ON check_records_v3(status);

-- 检查记录项V3索引（复合索引）
CREATE INDEX idx_check_record_id ON check_record_items_v3(check_record_id);
CREATE INDEX idx_item_class_id ON check_record_items_v3(class_id);
CREATE INDEX idx_deduction_item_id ON check_record_items_v3(deduction_item_id);
CREATE INDEX idx_record_class ON check_record_items_v3(check_record_id, class_id);

-- 申诉表索引
CREATE INDEX idx_appeal_status ON check_item_appeals(status);
CREATE INDEX idx_appeal_grade_id ON check_item_appeals(grade_id);
CREATE INDEX idx_appeal_record_id ON check_item_appeals(record_id);
CREATE INDEX idx_appeal_time ON check_item_appeals(appeal_time);
CREATE INDEX idx_grade_status ON check_item_appeals(grade_id, status);

-- 宿舍表索引
CREATE INDEX idx_dormitory_building_id ON dormitories(dormitory_building_id);
CREATE INDEX idx_dormitory_floor ON dormitories(floor);

-- 用户角色表索引
CREATE INDEX idx_user_role_user_id ON user_roles(user_id);
CREATE INDEX idx_user_role_role_id ON user_roles(role_id);

-- 角色权限表索引
CREATE INDEX idx_role_perm_role_id ON role_permissions(role_id);
CREATE INDEX idx_role_perm_perm_id ON role_permissions(permission_id);

-- 完成
SELECT '索引创建完成' as status, COUNT(*) as index_count
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'student_management'
  AND INDEX_NAME LIKE 'idx_%';
