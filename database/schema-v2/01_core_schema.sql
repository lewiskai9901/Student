-- =====================================================
-- 学生管理系统 - 核心基础表结构
-- 版本: 2.0 (合并后)
-- 创建日期: 2026-01-06
-- 包含: 用户、组织、权限、基础数据
-- =====================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================
-- 第一部分: 用户与权限
-- =====================================================

-- ---------------------------------------------------
-- 1.1 用户表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码(BCrypt)',
    `real_name` VARCHAR(50) COMMENT '真实姓名',
    `phone` VARCHAR(20) COMMENT '手机号',
    `email` VARCHAR(100) COMMENT '邮箱',
    `avatar` VARCHAR(255) COMMENT '头像URL',
    `gender` TINYINT DEFAULT 0 COMMENT '性别:0未知,1男,2女',
    `status` TINYINT DEFAULT 1 COMMENT '状态:0禁用,1启用',
    `last_login_at` DATETIME COMMENT '最后登录时间',
    `last_login_ip` VARCHAR(50) COMMENT '最后登录IP',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` BIGINT COMMENT '创建人',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by` BIGINT COMMENT '更新人',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除:0未删除,1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    INDEX `idx_phone` (`phone`),
    INDEX `idx_status` (`status`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ---------------------------------------------------
-- 1.2 角色表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `roles`;
CREATE TABLE `roles` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码',
    `role_name` VARCHAR(100) NOT NULL COMMENT '角色名称',
    `description` VARCHAR(500) COMMENT '角色描述',
    `data_scope` TINYINT DEFAULT 1 COMMENT '数据权限范围:1全部,2本部门,3本部门及子部门,4仅本人',
    `status` TINYINT DEFAULT 1 COMMENT '状态:0禁用,1启用',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` BIGINT COMMENT '创建人',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by` BIGINT COMMENT '更新人',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`),
    INDEX `idx_status` (`status`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- ---------------------------------------------------
-- 1.3 权限表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `permissions`;
CREATE TABLE `permissions` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `permission_code` VARCHAR(100) NOT NULL COMMENT '权限编码',
    `permission_name` VARCHAR(100) NOT NULL COMMENT '权限名称',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父权限ID',
    `permission_type` TINYINT DEFAULT 1 COMMENT '类型:1目录,2菜单,3按钮',
    `path` VARCHAR(255) COMMENT '路由路径',
    `component` VARCHAR(255) COMMENT '组件路径',
    `icon` VARCHAR(100) COMMENT '图标',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT DEFAULT 1 COMMENT '状态:0禁用,1启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_permission_code` (`permission_code`),
    INDEX `idx_parent_id` (`parent_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- ---------------------------------------------------
-- 1.4 用户角色关联表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `user_roles`;
CREATE TABLE `user_roles` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- ---------------------------------------------------
-- 1.5 角色权限关联表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `role_permissions`;
CREATE TABLE `role_permissions` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `permission_id` BIGINT NOT NULL COMMENT '权限ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
    INDEX `idx_role_id` (`role_id`),
    INDEX `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- ---------------------------------------------------
-- 1.6 用户数据权限范围表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `user_data_scopes`;
CREATE TABLE `user_data_scopes` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `scope_type` VARCHAR(20) NOT NULL COMMENT '范围类型:DEPARTMENT,GRADE,CLASS',
    `scope_id` BIGINT NOT NULL COMMENT '范围ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_scope` (`user_id`, `scope_type`, `scope_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_scope_type` (`scope_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户数据权限范围表';

-- =====================================================
-- 第二部分: 组织架构
-- =====================================================

-- ---------------------------------------------------
-- 2.1 组织单元表(系部/学院)
-- ---------------------------------------------------
DROP TABLE IF EXISTS `org_units`;
CREATE TABLE `org_units` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `unit_code` VARCHAR(50) NOT NULL COMMENT '单元编码',
    `unit_name` VARCHAR(100) NOT NULL COMMENT '单元名称',
    `unit_type` VARCHAR(30) NOT NULL COMMENT '类型:SCHOOL,COLLEGE,DEPARTMENT,TEACHING_GROUP',
    `parent_id` BIGINT COMMENT '父单元ID',
    `tree_path` VARCHAR(500) COMMENT '树形路径',
    `tree_level` INT DEFAULT 1 COMMENT '层级',
    `leader_id` BIGINT COMMENT '负责人ID',
    `deputy_leader_ids` JSON COMMENT '副负责人ID列表',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT DEFAULT 1 COMMENT '状态:0禁用,1启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` BIGINT COMMENT '创建人',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by` BIGINT COMMENT '更新人',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_unit_code` (`unit_code`),
    INDEX `idx_parent_id` (`parent_id`),
    INDEX `idx_unit_type` (`unit_type`),
    INDEX `idx_status` (`status`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组织单元表';

-- ---------------------------------------------------
-- 2.2 年级表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `grades`;
CREATE TABLE `grades` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `grade_code` VARCHAR(50) NOT NULL COMMENT '年级编码',
    `grade_name` VARCHAR(100) NOT NULL COMMENT '年级名称',
    `enrollment_year` INT NOT NULL COMMENT '入学年份',
    `schooling_years` INT DEFAULT 3 COMMENT '学制(年)',
    `expected_graduation_year` INT COMMENT '预计毕业年份',
    `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态:ACTIVE,GRADUATED,DISSOLVED',
    `leader_id` BIGINT COMMENT '年级主任ID',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` BIGINT COMMENT '创建人',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by` BIGINT COMMENT '更新人',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_grade_code` (`grade_code`),
    INDEX `idx_enrollment_year` (`enrollment_year`),
    INDEX `idx_status` (`status`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='年级表';

-- ---------------------------------------------------
-- 2.3 专业表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `majors`;
CREATE TABLE `majors` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `major_code` VARCHAR(50) NOT NULL COMMENT '专业编码',
    `major_name` VARCHAR(100) NOT NULL COMMENT '专业名称',
    `org_unit_id` BIGINT NOT NULL COMMENT '所属组织单元ID',
    `schooling_years` INT DEFAULT 3 COMMENT '学制(年)',
    `description` VARCHAR(500) COMMENT '专业描述',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT DEFAULT 1 COMMENT '状态:0禁用,1启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` BIGINT COMMENT '创建人',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by` BIGINT COMMENT '更新人',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_major_code` (`major_code`),
    INDEX `idx_org_unit_id` (`org_unit_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专业表';

-- ---------------------------------------------------
-- 2.4 专业方向表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `major_directions`;
CREATE TABLE `major_directions` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `direction_code` VARCHAR(50) NOT NULL COMMENT '方向编码',
    `direction_name` VARCHAR(100) NOT NULL COMMENT '方向名称',
    `major_id` BIGINT NOT NULL COMMENT '所属专业ID',
    `description` VARCHAR(500) COMMENT '方向描述',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT DEFAULT 1 COMMENT '状态:0禁用,1启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` BIGINT COMMENT '创建人',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by` BIGINT COMMENT '更新人',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_direction_code` (`direction_code`),
    INDEX `idx_major_id` (`major_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专业方向表';

-- ---------------------------------------------------
-- 2.5 班级表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `classes`;
CREATE TABLE `classes` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `class_code` VARCHAR(50) NOT NULL COMMENT '班级编码',
    `class_name` VARCHAR(100) NOT NULL COMMENT '班级名称',
    `short_name` VARCHAR(50) COMMENT '班级简称',
    `org_unit_id` BIGINT NOT NULL COMMENT '所属组织单元ID',
    `grade_id` BIGINT COMMENT '所属年级ID',
    `enrollment_year` INT NOT NULL COMMENT '入学年份',
    `grade_level` INT DEFAULT 1 COMMENT '年级级别(1-6)',
    `major_direction_id` BIGINT COMMENT '专业方向ID',
    `schooling_years` INT DEFAULT 3 COMMENT '学制(年)',
    `standard_size` INT DEFAULT 50 COMMENT '标准人数',
    `current_size` INT DEFAULT 0 COMMENT '当前人数',
    `status` VARCHAR(20) DEFAULT 'PREPARING' COMMENT '状态:PREPARING,ACTIVE,GRADUATED,DISSOLVED',
    `head_teacher_id` BIGINT COMMENT '班主任ID',
    `classroom_id` BIGINT COMMENT '教室ID',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` BIGINT COMMENT '创建人',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by` BIGINT COMMENT '更新人',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_class_code` (`class_code`),
    INDEX `idx_org_unit_id` (`org_unit_id`),
    INDEX `idx_grade_id` (`grade_id`),
    INDEX `idx_enrollment_year` (`enrollment_year`),
    INDEX `idx_status` (`status`),
    INDEX `idx_head_teacher_id` (`head_teacher_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级表';

-- ---------------------------------------------------
-- 2.6 教师任职表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `teacher_assignments`;
CREATE TABLE `teacher_assignments` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `class_id` BIGINT NOT NULL COMMENT '班级ID',
    `teacher_id` BIGINT NOT NULL COMMENT '教师ID',
    `teacher_name` VARCHAR(50) COMMENT '教师姓名',
    `role_type` VARCHAR(30) NOT NULL COMMENT '角色类型:HEAD_TEACHER,DEPUTY_HEAD_TEACHER,SUBJECT_TEACHER',
    `subject_id` BIGINT COMMENT '科目ID(科任教师)',
    `start_date` DATE NOT NULL COMMENT '任职开始日期',
    `end_date` DATE COMMENT '任职结束日期',
    `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态:ACTIVE,TRANSFERRED,RESIGNED,EXPIRED',
    `transfer_reason` VARCHAR(500) COMMENT '变更原因',
    `handover_teacher_id` BIGINT COMMENT '交接教师ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` BIGINT COMMENT '创建人',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by` BIGINT COMMENT '更新人',
    PRIMARY KEY (`id`),
    INDEX `idx_class_id` (`class_id`),
    INDEX `idx_teacher_id` (`teacher_id`),
    INDEX `idx_role_type` (`role_type`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教师任职表';

-- =====================================================
-- 第三部分: 学生管理
-- =====================================================

-- ---------------------------------------------------
-- 3.1 学生表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `students`;
CREATE TABLE `students` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `student_no` VARCHAR(50) NOT NULL COMMENT '学号',
    `name` VARCHAR(50) NOT NULL COMMENT '姓名',
    `gender` TINYINT DEFAULT 0 COMMENT '性别:0未知,1男,2女',
    `id_card` VARCHAR(18) COMMENT '身份证号',
    `phone` VARCHAR(20) COMMENT '手机号',
    `email` VARCHAR(100) COMMENT '邮箱',
    `class_id` BIGINT COMMENT '班级ID',
    `enrollment_date` DATE COMMENT '入学日期',
    `graduation_date` DATE COMMENT '毕业日期',
    `status` TINYINT DEFAULT 1 COMMENT '状态:1在读,2休学,3退学,4毕业',
    `user_id` BIGINT COMMENT '关联用户ID',
    `avatar` VARCHAR(255) COMMENT '头像URL',
    `address` VARCHAR(255) COMMENT '家庭住址',
    `emergency_contact` VARCHAR(50) COMMENT '紧急联系人',
    `emergency_phone` VARCHAR(20) COMMENT '紧急联系电话',
    `remark` VARCHAR(500) COMMENT '备注',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` BIGINT COMMENT '创建人',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by` BIGINT COMMENT '更新人',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_student_no` (`student_no`),
    INDEX `idx_class_id` (`class_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生表';

-- =====================================================
-- 第四部分: 资产管理(楼栋/宿舍)
-- =====================================================

-- ---------------------------------------------------
-- 4.1 楼栋表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `buildings`;
CREATE TABLE `buildings` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `building_code` VARCHAR(50) NOT NULL COMMENT '楼栋编码',
    `building_name` VARCHAR(100) NOT NULL COMMENT '楼栋名称',
    `building_type` VARCHAR(20) NOT NULL COMMENT '类型:DORMITORY,TEACHING,OFFICE',
    `total_floors` INT DEFAULT 1 COMMENT '总楼层数',
    `address` VARCHAR(255) COMMENT '地址',
    `manager_id` BIGINT COMMENT '管理员ID',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT DEFAULT 1 COMMENT '状态:0禁用,1启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` BIGINT COMMENT '创建人',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by` BIGINT COMMENT '更新人',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_building_code` (`building_code`),
    INDEX `idx_building_type` (`building_type`),
    INDEX `idx_status` (`status`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='楼栋表';

-- ---------------------------------------------------
-- 4.2 宿舍表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `dormitories`;
CREATE TABLE `dormitories` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `dormitory_no` VARCHAR(50) NOT NULL COMMENT '宿舍号',
    `building_id` BIGINT NOT NULL COMMENT '楼栋ID',
    `floor` INT NOT NULL COMMENT '楼层',
    `room_type` VARCHAR(20) DEFAULT 'NORMAL' COMMENT '房间类型:NORMAL,SUITE',
    `gender_type` VARCHAR(10) NOT NULL COMMENT '性别类型:MALE,FEMALE,MIXED',
    `bed_count` INT DEFAULT 6 COMMENT '床位数',
    `occupied_count` INT DEFAULT 0 COMMENT '已住人数',
    `status` VARCHAR(20) DEFAULT 'AVAILABLE' COMMENT '状态:AVAILABLE,FULL,MAINTENANCE,DISABLED',
    `dorm_leader_id` BIGINT COMMENT '宿舍长ID',
    `remark` VARCHAR(500) COMMENT '备注',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` BIGINT COMMENT '创建人',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by` BIGINT COMMENT '更新人',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_building_dormitory` (`building_id`, `dormitory_no`),
    INDEX `idx_building_id` (`building_id`),
    INDEX `idx_floor` (`floor`),
    INDEX `idx_gender_type` (`gender_type`),
    INDEX `idx_status` (`status`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='宿舍表';

-- ---------------------------------------------------
-- 4.3 学生宿舍分配表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `student_dormitories`;
CREATE TABLE `student_dormitories` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `student_id` BIGINT NOT NULL COMMENT '学生ID',
    `dormitory_id` BIGINT NOT NULL COMMENT '宿舍ID',
    `bed_no` VARCHAR(10) COMMENT '床位号',
    `check_in_date` DATE COMMENT '入住日期',
    `check_out_date` DATE COMMENT '退宿日期',
    `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态:ACTIVE,CHECKED_OUT,TRANSFERRED',
    `is_dorm_leader` TINYINT DEFAULT 0 COMMENT '是否宿舍长',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` BIGINT COMMENT '创建人',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by` BIGINT COMMENT '更新人',
    PRIMARY KEY (`id`),
    INDEX `idx_student_id` (`student_id`),
    INDEX `idx_dormitory_id` (`dormitory_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生宿舍分配表';

-- =====================================================
-- 第五部分: 基础配置
-- =====================================================

-- ---------------------------------------------------
-- 5.1 学期表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `semesters`;
CREATE TABLE `semesters` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `semester_code` VARCHAR(20) NOT NULL COMMENT '学期编码:2024-2025-1',
    `semester_name` VARCHAR(50) NOT NULL COMMENT '学期名称',
    `academic_year` VARCHAR(20) NOT NULL COMMENT '学年:2024-2025',
    `semester_type` TINYINT NOT NULL COMMENT '学期类型:1第一学期,2第二学期',
    `start_date` DATE NOT NULL COMMENT '开始日期',
    `end_date` DATE NOT NULL COMMENT '结束日期',
    `is_current` TINYINT DEFAULT 0 COMMENT '是否当前学期',
    `status` TINYINT DEFAULT 1 COMMENT '状态:1启用,0禁用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` BIGINT COMMENT '创建人',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by` BIGINT COMMENT '更新人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_semester_code` (`semester_code`),
    INDEX `idx_academic_year` (`academic_year`),
    INDEX `idx_current` (`is_current`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学期表';

-- ---------------------------------------------------
-- 5.2 系统配置表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `system_configs`;
CREATE TABLE `system_configs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `config_key` VARCHAR(100) NOT NULL COMMENT '配置键',
    `config_value` TEXT COMMENT '配置值',
    `config_type` VARCHAR(20) DEFAULT 'STRING' COMMENT '值类型:STRING,NUMBER,BOOLEAN,JSON',
    `description` VARCHAR(500) COMMENT '配置说明',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- ---------------------------------------------------
-- 5.3 公告表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `announcements`;
CREATE TABLE `announcements` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `title` VARCHAR(200) NOT NULL COMMENT '公告标题',
    `content` TEXT COMMENT '公告内容',
    `announcement_type` VARCHAR(20) DEFAULT 'notice' COMMENT '类型:notice,announcement,warning',
    `priority` TINYINT DEFAULT 1 COMMENT '优先级:1普通,2重要,3紧急',
    `publisher_id` BIGINT COMMENT '发布人ID',
    `publisher_name` VARCHAR(50) COMMENT '发布人姓名',
    `publish_time` DATETIME COMMENT '发布时间',
    `start_time` DATETIME COMMENT '生效开始时间',
    `end_time` DATETIME COMMENT '生效结束时间',
    `target_type` VARCHAR(20) DEFAULT 'all' COMMENT '目标类型:all,role,user',
    `target_ids` JSON COMMENT '目标ID列表',
    `attachment_url` VARCHAR(500) COMMENT '附件URL',
    `is_published` TINYINT DEFAULT 0 COMMENT '是否已发布',
    `is_pinned` TINYINT DEFAULT 0 COMMENT '是否置顶',
    `view_count` INT DEFAULT 0 COMMENT '浏览次数',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` BIGINT COMMENT '创建人',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by` BIGINT COMMENT '更新人',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    INDEX `idx_publish_time` (`publish_time`),
    INDEX `idx_is_published` (`is_published`),
    INDEX `idx_is_pinned` (`is_pinned`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告表';

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================
-- 核心基础表结构 - 完成
-- =====================================================
