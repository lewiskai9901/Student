-- ============================================================
-- 推荐数据库索引脚本
-- 用于优化常用查询性能
--
-- 执行前请备份数据库
-- 部分索引可能已存在，请根据实际情况执行
--
-- @author system
-- @version 1.0.0
-- @since 2024-12-31
-- ============================================================

-- ==================== 学生相关表索引 ====================

-- students表索引
CREATE INDEX IF NOT EXISTS idx_students_class_id ON students(class_id);
CREATE INDEX IF NOT EXISTS idx_students_grade_id ON students(grade_id);
CREATE INDEX IF NOT EXISTS idx_students_major_id ON students(major_id);
CREATE INDEX IF NOT EXISTS idx_students_user_id ON students(user_id);
CREATE INDEX IF NOT EXISTS idx_students_dormitory_id ON students(dormitory_id);
CREATE INDEX IF NOT EXISTS idx_students_student_no ON students(student_no);
CREATE INDEX IF NOT EXISTS idx_students_student_status ON students(student_status);
CREATE INDEX IF NOT EXISTS idx_students_deleted ON students(deleted);

-- users表索引
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_department_id ON users(department_id);
CREATE INDEX IF NOT EXISTS idx_users_class_id ON users(class_id);
CREATE INDEX IF NOT EXISTS idx_users_user_type ON users(user_type);
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);
CREATE INDEX IF NOT EXISTS idx_users_deleted ON users(deleted);
CREATE INDEX IF NOT EXISTS idx_users_phone ON users(phone);

-- ==================== 班级年级部门索引 ====================

-- classes表索引
CREATE INDEX IF NOT EXISTS idx_classes_grade_id ON classes(grade_id);
CREATE INDEX IF NOT EXISTS idx_classes_major_id ON classes(major_id);
CREATE INDEX IF NOT EXISTS idx_classes_department_id ON classes(department_id);
CREATE INDEX IF NOT EXISTS idx_classes_teacher_id ON classes(teacher_id);
CREATE INDEX IF NOT EXISTS idx_classes_assistant_teacher_id ON classes(assistant_teacher_id);
CREATE INDEX IF NOT EXISTS idx_classes_status ON classes(status);
CREATE INDEX IF NOT EXISTS idx_classes_deleted ON classes(deleted);

-- grades表索引
CREATE INDEX IF NOT EXISTS idx_grades_enrollment_year ON grades(enrollment_year);
CREATE INDEX IF NOT EXISTS idx_grades_grade_director_id ON grades(grade_director_id);
CREATE INDEX IF NOT EXISTS idx_grades_status ON grades(status);
CREATE INDEX IF NOT EXISTS idx_grades_deleted ON grades(deleted);

-- departments表索引
CREATE INDEX IF NOT EXISTS idx_departments_parent_id ON departments(parent_id);
CREATE INDEX IF NOT EXISTS idx_departments_leader_id ON departments(leader_id);
CREATE INDEX IF NOT EXISTS idx_departments_dept_path ON departments(dept_path);
CREATE INDEX IF NOT EXISTS idx_departments_status ON departments(status);
CREATE INDEX IF NOT EXISTS idx_departments_deleted ON departments(deleted);

-- ==================== 宿舍相关表索引 ====================

-- dormitories表索引
CREATE INDEX IF NOT EXISTS idx_dormitories_building_id ON dormitories(building_id);
CREATE INDEX IF NOT EXISTS idx_dormitories_floor ON dormitories(floor);
CREATE INDEX IF NOT EXISTS idx_dormitories_room_type ON dormitories(room_type);
CREATE INDEX IF NOT EXISTS idx_dormitories_status ON dormitories(status);
CREATE INDEX IF NOT EXISTS idx_dormitories_deleted ON dormitories(deleted);

-- buildings表索引
CREATE INDEX IF NOT EXISTS idx_buildings_building_type ON buildings(building_type);
CREATE INDEX IF NOT EXISTS idx_buildings_status ON buildings(status);
CREATE INDEX IF NOT EXISTS idx_buildings_deleted ON buildings(deleted);

-- ==================== 检查记录相关表索引 ====================

-- daily_checks表索引
CREATE INDEX IF NOT EXISTS idx_daily_checks_check_date ON daily_checks(check_date);
CREATE INDEX IF NOT EXISTS idx_daily_checks_plan_id ON daily_checks(plan_id);
CREATE INDEX IF NOT EXISTS idx_daily_checks_template_id ON daily_checks(template_id);
CREATE INDEX IF NOT EXISTS idx_daily_checks_status ON daily_checks(status);
CREATE INDEX IF NOT EXISTS idx_daily_checks_semester_id ON daily_checks(semester_id);
CREATE INDEX IF NOT EXISTS idx_daily_checks_deleted ON daily_checks(deleted);
-- 复合索引用于按日期范围查询
CREATE INDEX IF NOT EXISTS idx_daily_checks_date_status ON daily_checks(check_date, status);

-- daily_check_details表索引
CREATE INDEX IF NOT EXISTS idx_daily_check_details_check_id ON daily_check_details(check_id);
CREATE INDEX IF NOT EXISTS idx_daily_check_details_class_id ON daily_check_details(class_id);
CREATE INDEX IF NOT EXISTS idx_daily_check_details_category_id ON daily_check_details(category_id);
CREATE INDEX IF NOT EXISTS idx_daily_check_details_deduction_item_id ON daily_check_details(deduction_item_id);
CREATE INDEX IF NOT EXISTS idx_daily_check_details_link_type ON daily_check_details(link_type);
CREATE INDEX IF NOT EXISTS idx_daily_check_details_link_id ON daily_check_details(link_id);
CREATE INDEX IF NOT EXISTS idx_daily_check_details_deleted ON daily_check_details(deleted);

-- ==================== 检查计划相关表索引 ====================

-- check_plans表索引
CREATE INDEX IF NOT EXISTS idx_check_plans_semester_id ON check_plans(semester_id);
CREATE INDEX IF NOT EXISTS idx_check_plans_status ON check_plans(status);
CREATE INDEX IF NOT EXISTS idx_check_plans_start_date ON check_plans(start_date);
CREATE INDEX IF NOT EXISTS idx_check_plans_end_date ON check_plans(end_date);
CREATE INDEX IF NOT EXISTS idx_check_plans_deleted ON check_plans(deleted);

-- check_templates表索引
CREATE INDEX IF NOT EXISTS idx_check_templates_check_type ON check_templates(check_type);
CREATE INDEX IF NOT EXISTS idx_check_templates_status ON check_templates(status);
CREATE INDEX IF NOT EXISTS idx_check_templates_deleted ON check_templates(deleted);

-- deduction_items表索引
CREATE INDEX IF NOT EXISTS idx_deduction_items_category_id ON deduction_items(category_id);
CREATE INDEX IF NOT EXISTS idx_deduction_items_template_id ON deduction_items(template_id);
CREATE INDEX IF NOT EXISTS idx_deduction_items_deduct_mode ON deduction_items(deduct_mode);
CREATE INDEX IF NOT EXISTS idx_deduction_items_status ON deduction_items(status);
CREATE INDEX IF NOT EXISTS idx_deduction_items_deleted ON deduction_items(deleted);

-- ==================== 申诉相关表索引 ====================

-- check_item_appeals表索引
CREATE INDEX IF NOT EXISTS idx_check_item_appeals_record_id ON check_item_appeals(record_id);
CREATE INDEX IF NOT EXISTS idx_check_item_appeals_class_id ON check_item_appeals(class_id);
CREATE INDEX IF NOT EXISTS idx_check_item_appeals_status ON check_item_appeals(status);
CREATE INDEX IF NOT EXISTS idx_check_item_appeals_appellant_id ON check_item_appeals(appellant_id);
CREATE INDEX IF NOT EXISTS idx_check_item_appeals_appeal_time ON check_item_appeals(appeal_time);
CREATE INDEX IF NOT EXISTS idx_check_item_appeals_deleted ON check_item_appeals(deleted);

-- ==================== 权限相关表索引 ====================

-- user_roles表索引
CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_role_id ON user_roles(role_id);

-- role_permissions表索引
CREATE INDEX IF NOT EXISTS idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX IF NOT EXISTS idx_role_permissions_permission_id ON role_permissions(permission_id);

-- role_data_permissions表索引
CREATE INDEX IF NOT EXISTS idx_role_data_permissions_role_id ON role_data_permissions(role_id);
CREATE INDEX IF NOT EXISTS idx_role_data_permissions_module_code ON role_data_permissions(module_code);

-- ==================== 日志相关表索引 ====================

-- operation_logs表索引
CREATE INDEX IF NOT EXISTS idx_operation_logs_user_id ON operation_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_operation_logs_operation_type ON operation_logs(operation_type);
CREATE INDEX IF NOT EXISTS idx_operation_logs_create_time ON operation_logs(create_time);
CREATE INDEX IF NOT EXISTS idx_operation_logs_module ON operation_logs(module);

-- ==================== 其他常用表索引 ====================

-- semesters表索引
CREATE INDEX IF NOT EXISTS idx_semesters_is_current ON semesters(is_current);
CREATE INDEX IF NOT EXISTS idx_semesters_start_date ON semesters(start_date);
CREATE INDEX IF NOT EXISTS idx_semesters_deleted ON semesters(deleted);

-- announcements表索引
CREATE INDEX IF NOT EXISTS idx_announcements_status ON announcements(status);
CREATE INDEX IF NOT EXISTS idx_announcements_publish_time ON announcements(publish_time);
CREATE INDEX IF NOT EXISTS idx_announcements_target_type ON announcements(target_type);
CREATE INDEX IF NOT EXISTS idx_announcements_deleted ON announcements(deleted);

-- majors表索引
CREATE INDEX IF NOT EXISTS idx_majors_department_id ON majors(department_id);
CREATE INDEX IF NOT EXISTS idx_majors_status ON majors(status);
CREATE INDEX IF NOT EXISTS idx_majors_deleted ON majors(deleted);

-- major_directions表索引
CREATE INDEX IF NOT EXISTS idx_major_directions_major_id ON major_directions(major_id);
CREATE INDEX IF NOT EXISTS idx_major_directions_status ON major_directions(status);
CREATE INDEX IF NOT EXISTS idx_major_directions_deleted ON major_directions(deleted);

-- ============================================================
-- 说明：
-- 1. IF NOT EXISTS 语法在某些MySQL版本不支持，请根据情况调整
-- 2. 索引会占用额外存储空间，请根据实际需要选择性创建
-- 3. 对于频繁更新的字段，索引可能影响写入性能
-- 4. 建议在低峰期执行索引创建
-- 5. 创建后可使用 EXPLAIN 验证索引是否被使用
-- ============================================================
