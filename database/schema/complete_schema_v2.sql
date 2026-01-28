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
-- =====================================================
-- 学生管理系统 - 业务表结构
-- 版本: 2.0 (合并后)
-- 创建日期: 2026-01-06
-- 包含: 量化检查、评级系统、任务工作流
-- =====================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================
-- 第一部分: 量化检查系统
-- =====================================================

-- ---------------------------------------------------
-- 1.1 检查模板表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `check_templates`;
CREATE TABLE `check_templates` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `template_code` VARCHAR(50) NOT NULL COMMENT '模板编码',
    `template_name` VARCHAR(100) NOT NULL COMMENT '模板名称',
    `description` TEXT COMMENT '模板描述',
    `template_type` VARCHAR(20) DEFAULT 'DAILY' COMMENT '类型:DAILY,WEEKLY,SPECIAL',
    `base_score` DECIMAL(6,2) DEFAULT 100.00 COMMENT '基础分数',
    `version` INT DEFAULT 1 COMMENT '版本号',
    `status` VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态:DRAFT,PUBLISHED,DEPRECATED',
    `is_default` TINYINT DEFAULT 0 COMMENT '是否默认模板',
    `applicable_scope` VARCHAR(20) DEFAULT 'ALL' COMMENT '适用范围:ALL,DEPARTMENT,GRADE',
    `applicable_config` JSON COMMENT '适用范围配置',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` BIGINT COMMENT '创建人',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by` BIGINT COMMENT '更新人',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_code` (`template_code`),
    INDEX `idx_status` (`status`),
    INDEX `idx_is_default` (`is_default`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查模板表';

-- ---------------------------------------------------
-- 1.2 检查类别表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `check_categories`;
CREATE TABLE `check_categories` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `template_id` BIGINT NOT NULL COMMENT '模板ID',
    `category_code` VARCHAR(50) NOT NULL COMMENT '类别编码',
    `category_name` VARCHAR(100) NOT NULL COMMENT '类别名称',
    `description` VARCHAR(500) COMMENT '类别描述',
    `weight` DECIMAL(5,2) DEFAULT 1.00 COMMENT '权重',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_template_id` (`template_id`),
    INDEX `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查类别表';

-- ---------------------------------------------------
-- 1.3 扣分项表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `deduction_items`;
CREATE TABLE `deduction_items` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `category_id` BIGINT NOT NULL COMMENT '类别ID',
    `item_code` VARCHAR(50) NOT NULL COMMENT '项目编码',
    `item_name` VARCHAR(200) NOT NULL COMMENT '项目名称',
    `description` TEXT COMMENT '项目描述',
    `deduction_mode` VARCHAR(20) NOT NULL COMMENT '扣分模式:FIXED,PER_PERSON,RANGE,FORMULA',
    `fixed_score` DECIMAL(6,2) COMMENT '固定扣分',
    `base_score` DECIMAL(6,2) COMMENT '基础扣分(按人数)',
    `per_person_score` DECIMAL(6,2) COMMENT '每人扣分',
    `max_persons` INT COMMENT '最大人数',
    `range_config` JSON COMMENT '区间配置',
    `min_score` DECIMAL(6,2) DEFAULT 0 COMMENT '最小扣分',
    `max_score` DECIMAL(6,2) COMMENT '最大扣分',
    `requires_photo` TINYINT DEFAULT 0 COMMENT '是否需要拍照',
    `requires_student_select` TINYINT DEFAULT 0 COMMENT '是否需要选择学生',
    `check_points` JSON COMMENT '检查要点',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT DEFAULT 1 COMMENT '状态:0禁用,1启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` BIGINT COMMENT '创建人',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by` BIGINT COMMENT '更新人',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    INDEX `idx_category_id` (`category_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='扣分项表';

-- ---------------------------------------------------
-- 1.4 检查计划表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `check_plans`;
CREATE TABLE `check_plans` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `plan_code` VARCHAR(50) NOT NULL COMMENT '计划编码',
    `plan_name` VARCHAR(100) NOT NULL COMMENT '计划名称',
    `template_id` BIGINT NOT NULL COMMENT '模板ID',
    `semester_id` BIGINT COMMENT '学期ID',
    `check_type` VARCHAR(20) DEFAULT 'DAILY' COMMENT '检查类型:DAILY,WEEKLY,SPECIAL',
    `start_date` DATE COMMENT '开始日期',
    `end_date` DATE COMMENT '结束日期',
    `check_time_config` JSON COMMENT '检查时间配置',
    `scope_type` VARCHAR(20) DEFAULT 'ALL' COMMENT '范围类型:ALL,DEPARTMENT,GRADE',
    `scope_config` JSON COMMENT '范围配置',
    `status` VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态:DRAFT,ACTIVE,PAUSED,COMPLETED',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` BIGINT COMMENT '创建人',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by` BIGINT COMMENT '更新人',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_plan_code` (`plan_code`),
    INDEX `idx_template_id` (`template_id`),
    INDEX `idx_semester_id` (`semester_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查计划表';

-- ---------------------------------------------------
-- 1.5 检查记录表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `check_records`;
CREATE TABLE `check_records` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `record_code` VARCHAR(50) NOT NULL COMMENT '记录编码',
    `plan_id` BIGINT NOT NULL COMMENT '计划ID',
    `template_id` BIGINT NOT NULL COMMENT '模板ID',
    `template_version` INT DEFAULT 1 COMMENT '模板版本',
    `check_date` DATE NOT NULL COMMENT '检查日期',
    `check_round` VARCHAR(20) COMMENT '检查轮次:MORNING,NOON,EVENING',
    `template_snapshot` JSON COMMENT '模板快照',
    `status` VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态:DRAFT,CHECKING,SUBMITTED,REVIEWING,PUBLISHED,VOIDED',
    `checker_id` BIGINT COMMENT '检查人ID',
    `checker_name` VARCHAR(50) COMMENT '检查人姓名',
    `checked_at` DATETIME COMMENT '检查时间',
    `reviewer_id` BIGINT COMMENT '审核人ID',
    `reviewed_at` DATETIME COMMENT '审核时间',
    `published_at` DATETIME COMMENT '发布时间',
    `remark` VARCHAR(500) COMMENT '备注',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` BIGINT COMMENT '创建人',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by` BIGINT COMMENT '更新人',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_record_code` (`record_code`),
    INDEX `idx_plan_id` (`plan_id`),
    INDEX `idx_check_date` (`check_date`),
    INDEX `idx_status` (`status`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查记录表';

-- ---------------------------------------------------
-- 1.6 班级得分表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `class_scores`;
CREATE TABLE `class_scores` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `record_id` BIGINT NOT NULL COMMENT '检查记录ID',
    `class_id` BIGINT NOT NULL COMMENT '班级ID',
    `class_code` VARCHAR(50) COMMENT '班级编码',
    `class_name` VARCHAR(100) COMMENT '班级名称',
    `class_size` INT COMMENT '班级人数',
    `org_unit_id` BIGINT COMMENT '组织单元ID',
    `org_unit_name` VARCHAR(100) COMMENT '组织单元名称',
    `base_score` DECIMAL(10,2) DEFAULT 100.00 COMMENT '基础分',
    `total_deduction` DECIMAL(10,2) DEFAULT 0 COMMENT '总扣分',
    `weight_coefficient` DECIMAL(5,4) DEFAULT 1.0000 COMMENT '加权系数',
    `weighted_deduction` DECIMAL(10,2) COMMENT '加权扣分',
    `final_score` DECIMAL(10,2) COMMENT '最终得分',
    `overall_rank` INT COMMENT '总排名',
    `dept_rank` INT COMMENT '部门排名',
    `grade_rank` INT COMMENT '年级排名',
    `score_version` INT DEFAULT 1 COMMENT '分数版本',
    `is_latest` TINYINT DEFAULT 1 COMMENT '是否最新版本',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_record_class_version` (`record_id`, `class_id`, `score_version`),
    INDEX `idx_record_id` (`record_id`),
    INDEX `idx_class_id` (`class_id`),
    INDEX `idx_org_unit_id` (`org_unit_id`),
    INDEX `idx_final_score` (`final_score`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级得分表';

-- ---------------------------------------------------
-- 1.7 扣分明细表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `deduction_details`;
CREATE TABLE `deduction_details` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `class_score_id` BIGINT NOT NULL COMMENT '班级得分ID',
    `record_id` BIGINT NOT NULL COMMENT '检查记录ID',
    `class_id` BIGINT NOT NULL COMMENT '班级ID',
    `deduction_item_id` BIGINT NOT NULL COMMENT '扣分项ID',
    `item_name` VARCHAR(200) COMMENT '扣分项名称',
    `category_id` BIGINT COMMENT '类别ID',
    `category_name` VARCHAR(100) COMMENT '类别名称',
    `count` INT DEFAULT 1 COMMENT '数量/人数',
    `deduction_amount` DECIMAL(6,2) NOT NULL COMMENT '扣分分值',
    `student_ids` JSON COMMENT '涉及学生ID列表',
    `evidence_urls` JSON COMMENT '证据图片URL列表',
    `location` VARCHAR(100) COMMENT '位置',
    `remark` VARCHAR(500) COMMENT '备注',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` BIGINT COMMENT '创建人',
    PRIMARY KEY (`id`),
    INDEX `idx_class_score_id` (`class_score_id`),
    INDEX `idx_record_id` (`record_id`),
    INDEX `idx_class_id` (`class_id`),
    INDEX `idx_deduction_item_id` (`deduction_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='扣分明细表';

-- ---------------------------------------------------
-- 1.8 申诉表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `check_item_appeals`;
CREATE TABLE `check_item_appeals` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `appeal_code` VARCHAR(50) NOT NULL COMMENT '申诉编号',
    `record_id` BIGINT NOT NULL COMMENT '检查记录ID',
    `class_id` BIGINT NOT NULL COMMENT '班级ID',
    `class_score_id` BIGINT COMMENT '班级得分ID',
    `deduction_detail_id` BIGINT COMMENT '扣分明细ID',
    `appeal_type` VARCHAR(20) NOT NULL COMMENT '申诉类型:DEDUCTION,SCORE,OTHER',
    `appeal_reason` TEXT NOT NULL COMMENT '申诉理由',
    `evidence_urls` JSON COMMENT '证据图片URL列表',
    `expected_score_change` DECIMAL(6,2) COMMENT '期望分数变更',
    `status` VARCHAR(30) DEFAULT 'PENDING' COMMENT '状态:PENDING,LEVEL1_REVIEWING,LEVEL2_REVIEWING,APPROVED,REJECTED,WITHDRAWN,EXPIRED',
    `current_level` INT DEFAULT 1 COMMENT '当前审核级别',
    `submitter_id` BIGINT NOT NULL COMMENT '提交人ID',
    `submitter_name` VARCHAR(50) COMMENT '提交人姓名',
    `submitted_at` DATETIME NOT NULL COMMENT '提交时间',
    `final_result` VARCHAR(20) COMMENT '最终结果:APPROVED,REJECTED',
    `actual_score_change` DECIMAL(6,2) COMMENT '实际分数变更',
    `final_reviewer_id` BIGINT COMMENT '最终审核人ID',
    `final_reviewed_at` DATETIME COMMENT '最终审核时间',
    `final_remark` VARCHAR(500) COMMENT '最终审核备注',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_appeal_code` (`appeal_code`),
    INDEX `idx_record_id` (`record_id`),
    INDEX `idx_class_id` (`class_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_submitter_id` (`submitter_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='申诉表';

-- ---------------------------------------------------
-- 1.9 申诉审批记录表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `appeal_approval_records`;
CREATE TABLE `appeal_approval_records` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `appeal_id` BIGINT NOT NULL COMMENT '申诉ID',
    `approval_level` INT NOT NULL COMMENT '审批级别',
    `approver_id` BIGINT NOT NULL COMMENT '审批人ID',
    `approver_name` VARCHAR(50) COMMENT '审批人姓名',
    `action` VARCHAR(20) NOT NULL COMMENT '操作:APPROVE,REJECT,FORWARD',
    `opinion` VARCHAR(500) COMMENT '审批意见',
    `approved_score_change` DECIMAL(6,2) COMMENT '同意的分数变更',
    `approved_at` DATETIME NOT NULL COMMENT '审批时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_appeal_id` (`appeal_id`),
    INDEX `idx_approver_id` (`approver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='申诉审批记录表';

-- =====================================================
-- 第二部分: 评级系统
-- =====================================================

-- ---------------------------------------------------
-- 2.1 评级规则表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `check_plan_rating_rules`;
CREATE TABLE `check_plan_rating_rules` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `check_plan_id` BIGINT NOT NULL COMMENT '检查计划ID',
    `rule_name` VARCHAR(100) NOT NULL COMMENT '规则名称',
    `rule_type` VARCHAR(20) NOT NULL COMMENT '类型:DAILY,SUMMARY',
    `score_source` VARCHAR(20) DEFAULT 'TOTAL' COMMENT '评分来源:TOTAL,CATEGORY',
    `category_id` BIGINT COMMENT '类别ID',
    `category_name` VARCHAR(100) COMMENT '类别名称',
    `use_weighted_score` TINYINT DEFAULT 0 COMMENT '是否使用加权分',
    `division_method` VARCHAR(20) NOT NULL COMMENT '划分方式:SCORE_RANGE,RANK_COUNT,PERCENTAGE',
    `summary_method` VARCHAR(20) DEFAULT 'AVERAGE' COMMENT '汇总方式:AVERAGE,SUM',
    `rating_cycle` VARCHAR(20) DEFAULT 'DAILY' COMMENT '评级周期:DAILY,WEEKLY,MONTHLY',
    `cycle_config` JSON COMMENT '周期配置',
    `require_approval` TINYINT DEFAULT 0 COMMENT '是否需要审核',
    `auto_calculate` TINYINT DEFAULT 1 COMMENT '是否自动计算',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `enabled` TINYINT DEFAULT 1 COMMENT '是否启用',
    `description` VARCHAR(500) COMMENT '描述',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` BIGINT COMMENT '创建人',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    INDEX `idx_check_plan_id` (`check_plan_id`),
    INDEX `idx_rule_type` (`rule_type`),
    INDEX `idx_enabled` (`enabled`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级规则表';

-- ---------------------------------------------------
-- 2.2 评级等级表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `check_plan_rating_levels`;
CREATE TABLE `check_plan_rating_levels` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `rule_id` BIGINT NOT NULL COMMENT '规则ID',
    `level_order` INT NOT NULL COMMENT '等级顺序',
    `level_name` VARCHAR(50) NOT NULL COMMENT '等级名称',
    `level_color` VARCHAR(20) COMMENT '等级颜色',
    `level_icon` VARCHAR(100) COMMENT '等级图标',
    `min_score` DECIMAL(10,2) COMMENT '最小分数',
    `max_score` DECIMAL(10,2) COMMENT '最大分数',
    `rank_count` INT COMMENT '名次数量',
    `percentage` DECIMAL(5,2) COMMENT '百分比',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_rule_id` (`rule_id`),
    INDEX `idx_level_order` (`level_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级等级表';

-- ---------------------------------------------------
-- 2.3 评级结果表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `check_plan_rating_results`;
CREATE TABLE `check_plan_rating_results` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `rule_id` BIGINT NOT NULL COMMENT '规则ID',
    `check_plan_id` BIGINT NOT NULL COMMENT '检查计划ID',
    `check_record_id` BIGINT COMMENT '检查记录ID',
    `check_date` DATE COMMENT '检查日期',
    `class_id` BIGINT NOT NULL COMMENT '班级ID',
    `class_name` VARCHAR(100) COMMENT '班级名称',
    `grade_id` BIGINT COMMENT '年级ID',
    `grade_name` VARCHAR(50) COMMENT '年级名称',
    `level_id` BIGINT NOT NULL COMMENT '等级ID',
    `level_name` VARCHAR(50) NOT NULL COMMENT '等级名称',
    `level_order` INT NOT NULL COMMENT '等级顺序',
    `ranking` INT COMMENT '排名',
    `total_classes` INT COMMENT '参评班级数',
    `score` DECIMAL(10,2) NOT NULL COMMENT '得分/扣分',
    `period_start` DATE COMMENT '周期开始',
    `period_end` DATE COMMENT '周期结束',
    `record_count` INT COMMENT '记录数',
    `approval_status` TINYINT DEFAULT 0 COMMENT '审核状态:0待审,1通过,2驳回',
    `approved_by` BIGINT COMMENT '审核人ID',
    `approved_at` DATETIME COMMENT '审核时间',
    `approval_remark` VARCHAR(500) COMMENT '审核备注',
    `publish_status` TINYINT DEFAULT 0 COMMENT '发布状态:0未发布,1已发布',
    `published_at` DATETIME COMMENT '发布时间',
    `version` INT DEFAULT 1 COMMENT '版本号',
    `original_level_id` BIGINT COMMENT '原始等级ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_rule_id` (`rule_id`),
    INDEX `idx_check_plan_id` (`check_plan_id`),
    INDEX `idx_check_record_id` (`check_record_id`),
    INDEX `idx_class_id` (`class_id`),
    INDEX `idx_check_date` (`check_date`),
    INDEX `idx_period` (`period_start`, `period_end`),
    UNIQUE KEY `uk_daily_result` (`rule_id`, `check_record_id`, `class_id`),
    UNIQUE KEY `uk_summary_result` (`rule_id`, `period_start`, `period_end`, `class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级结果表';

-- ---------------------------------------------------
-- 2.4 评级频次统计表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `check_plan_rating_frequency`;
CREATE TABLE `check_plan_rating_frequency` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `check_plan_id` BIGINT NOT NULL COMMENT '检查计划ID',
    `class_id` BIGINT NOT NULL COMMENT '班级ID',
    `class_name` VARCHAR(100) COMMENT '班级名称',
    `grade_id` BIGINT COMMENT '年级ID',
    `grade_name` VARCHAR(100) COMMENT '年级名称',
    `rule_id` BIGINT NOT NULL COMMENT '规则ID',
    `rule_name` VARCHAR(100) COMMENT '规则名称',
    `level_id` BIGINT NOT NULL COMMENT '等级ID',
    `level_name` VARCHAR(50) NOT NULL COMMENT '等级名称',
    `level_color` VARCHAR(20) COMMENT '等级颜色',
    `frequency` INT DEFAULT 0 COMMENT '频次',
    `total_ratings` INT DEFAULT 0 COMMENT '总评级次数',
    `frequency_rate` DECIMAL(5,2) DEFAULT 0 COMMENT '频次占比',
    `period_type` VARCHAR(20) NOT NULL COMMENT '周期类型',
    `period_start` DATE NOT NULL COMMENT '周期开始',
    `period_end` DATE NOT NULL COMMENT '周期结束',
    `period_label` VARCHAR(50) COMMENT '周期标签',
    `last_rating_date` DATE COMMENT '最近评级日期',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_plan_period` (`check_plan_id`, `period_type`, `period_start`, `period_end`),
    INDEX `idx_class_rule` (`class_id`, `rule_id`),
    UNIQUE KEY `uk_class_rule_level_period` (`class_id`, `rule_id`, `level_id`, `period_type`, `period_start`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级频次统计表';

-- =====================================================
-- 第三部分: 任务工作流
-- =====================================================

-- ---------------------------------------------------
-- 3.1 流程模板表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `workflow_templates`;
CREATE TABLE `workflow_templates` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `template_name` VARCHAR(100) NOT NULL COMMENT '模板名称',
    `template_code` VARCHAR(50) NOT NULL COMMENT '模板编码',
    `template_type` VARCHAR(30) DEFAULT 'TASK' COMMENT '类型:TASK,LEAVE,OTHER',
    `description` VARCHAR(500) COMMENT '描述',
    `process_definition_id` VARCHAR(64) COMMENT 'Flowable流程定义ID',
    `process_definition_key` VARCHAR(64) COMMENT 'Flowable流程Key',
    `bpmn_xml` LONGTEXT COMMENT 'BPMN定义XML',
    `form_config` JSON COMMENT '表单配置',
    `node_config` JSON COMMENT '节点配置',
    `is_default` TINYINT DEFAULT 0 COMMENT '是否默认',
    `status` TINYINT DEFAULT 1 COMMENT '状态:0禁用,1启用',
    `version` INT DEFAULT 1 COMMENT '版本号',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` BIGINT COMMENT '创建人',
    `created_by_name` VARCHAR(50) COMMENT '创建人姓名',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by` BIGINT COMMENT '更新人',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_code` (`template_code`),
    INDEX `idx_template_type` (`template_type`),
    INDEX `idx_status` (`status`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程模板表';

-- ---------------------------------------------------
-- 3.2 任务表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `tasks`;
CREATE TABLE `tasks` (
    `id` BIGINT NOT NULL COMMENT '任务ID',
    `task_code` VARCHAR(50) NOT NULL COMMENT '任务编号',
    `title` VARCHAR(200) NOT NULL COMMENT '任务标题',
    `description` TEXT COMMENT '任务描述',
    `priority` TINYINT DEFAULT 2 COMMENT '优先级:1紧急,2普通,3低',
    `status` TINYINT DEFAULT 0 COMMENT '状态:0待接收,1进行中,2待审核,3已完成,4已打回,5已取消,6审批中',
    `assigner_id` BIGINT NOT NULL COMMENT '分配人ID',
    `assigner_name` VARCHAR(50) COMMENT '分配人姓名',
    `assign_type` TINYINT DEFAULT 1 COMMENT '分配类型:1指定个人,2批量分配',
    `assignee_id` BIGINT COMMENT '执行人ID',
    `assignee_name` VARCHAR(50) COMMENT '执行人姓名',
    `department_id` BIGINT COMMENT '部门ID',
    `department_name` VARCHAR(100) COMMENT '部门名称',
    `target_ids` JSON COMMENT '目标ID列表',
    `due_date` DATETIME COMMENT '截止时间',
    `accepted_at` DATETIME COMMENT '接收时间',
    `submitted_at` DATETIME COMMENT '提交时间',
    `completed_at` DATETIME COMMENT '完成时间',
    `workflow_template_id` BIGINT COMMENT '流程模板ID',
    `process_instance_id` VARCHAR(64) COMMENT 'Flowable流程实例ID',
    `current_node` VARCHAR(100) COMMENT '当前节点',
    `current_approvers` JSON COMMENT '当前审批人列表',
    `attachment_ids` JSON COMMENT '附件ID列表',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_code` (`task_code`),
    INDEX `idx_assigner_id` (`assigner_id`),
    INDEX `idx_assignee_id` (`assignee_id`),
    INDEX `idx_department_id` (`department_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_due_date` (`due_date`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务表';

-- ---------------------------------------------------
-- 3.3 任务执行人表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `task_assignees`;
CREATE TABLE `task_assignees` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `task_id` BIGINT NOT NULL COMMENT '任务ID',
    `assignee_id` BIGINT NOT NULL COMMENT '执行人ID',
    `assignee_name` VARCHAR(50) COMMENT '执行人姓名',
    `status` TINYINT DEFAULT 0 COMMENT '状态',
    `accepted_at` DATETIME COMMENT '接收时间',
    `submitted_at` DATETIME COMMENT '提交时间',
    `completed_at` DATETIME COMMENT '完成时间',
    `process_instance_id` VARCHAR(64) COMMENT '流程实例ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_assignee` (`task_id`, `assignee_id`),
    INDEX `idx_task_id` (`task_id`),
    INDEX `idx_assignee_id` (`assignee_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务执行人表';

-- ---------------------------------------------------
-- 3.4 任务提交记录表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `task_submissions`;
CREATE TABLE `task_submissions` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `task_id` BIGINT NOT NULL COMMENT '任务ID',
    `task_assignee_id` BIGINT COMMENT '执行人记录ID',
    `submitter_id` BIGINT NOT NULL COMMENT '提交人ID',
    `submitter_name` VARCHAR(50) COMMENT '提交人姓名',
    `content` TEXT COMMENT '完成情况',
    `attachment_ids` JSON COMMENT '附件ID列表',
    `submitted_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    `review_status` TINYINT DEFAULT 0 COMMENT '审核状态:0待审,1通过,2驳回',
    `reviewer_id` BIGINT COMMENT '审核人ID',
    `reviewer_name` VARCHAR(50) COMMENT '审核人姓名',
    `reviewed_at` DATETIME COMMENT '审核时间',
    `review_comment` TEXT COMMENT '审核意见',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_task_id` (`task_id`),
    INDEX `idx_submitter_id` (`submitter_id`),
    INDEX `idx_review_status` (`review_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务提交记录表';

-- ---------------------------------------------------
-- 3.5 任务审批记录表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `task_approval_records`;
CREATE TABLE `task_approval_records` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `task_id` BIGINT NOT NULL COMMENT '任务ID',
    `submission_id` BIGINT COMMENT '提交记录ID',
    `approval_level` INT NOT NULL COMMENT '审批级别',
    `approver_id` BIGINT NOT NULL COMMENT '审批人ID',
    `approver_name` VARCHAR(50) COMMENT '审批人姓名',
    `action` VARCHAR(20) NOT NULL COMMENT '操作:APPROVE,REJECT,FORWARD',
    `comment` TEXT COMMENT '审批意见',
    `approved_at` DATETIME NOT NULL COMMENT '审批时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_task_id` (`task_id`),
    INDEX `idx_submission_id` (`submission_id`),
    INDEX `idx_approver_id` (`approver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务审批记录表';

-- ---------------------------------------------------
-- 3.6 任务审批配置表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `task_approval_configs`;
CREATE TABLE `task_approval_configs` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `task_id` BIGINT NOT NULL COMMENT '任务ID',
    `department_id` BIGINT NOT NULL COMMENT '部门ID',
    `department_name` VARCHAR(100) NOT NULL COMMENT '部门名称',
    `approval_level` TINYINT NOT NULL COMMENT '审批级别',
    `approver_id` BIGINT NOT NULL COMMENT '审批人ID',
    `approver_name` VARCHAR(50) NOT NULL COMMENT '审批人姓名',
    `approver_role` VARCHAR(50) COMMENT '审批人角色',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_dept_level` (`task_id`, `department_id`, `approval_level`),
    INDEX `idx_task_id` (`task_id`),
    INDEX `idx_department_id` (`department_id`),
    INDEX `idx_approver_id` (`approver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务审批配置表';

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================
-- 业务表结构 - 完成
-- =====================================================
-- ============================================================================
-- Student Management System V2 - Audit & Event Schema
-- ============================================================================
-- Version: 2.0
-- Description: Consolidated schema for audit logs, domain events, and snapshots
-- Created: 2026-01-06
-- ============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================================
-- PART 1: OPERATION & AUDIT LOGS
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1.1 Operation Logs - Basic request/response tracking
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `operation_logs`;
CREATE TABLE `operation_logs` (
    `id` BIGINT NOT NULL COMMENT 'Primary Key',
    `user_id` BIGINT NOT NULL COMMENT 'Operator User ID',
    `username` VARCHAR(50) NOT NULL COMMENT 'Username',
    `real_name` VARCHAR(50) DEFAULT NULL COMMENT 'Real Name',

    -- Operation info
    `operation_module` VARCHAR(50) NOT NULL COMMENT 'Module (student/class/dormitory/inspection/system)',
    `operation_type` VARCHAR(20) NOT NULL COMMENT 'Type (create/update/delete/query/export/login/logout)',
    `operation_name` VARCHAR(100) NOT NULL COMMENT 'Operation Name',

    -- Request info
    `request_method` VARCHAR(10) DEFAULT NULL COMMENT 'HTTP Method (GET/POST/PUT/DELETE)',
    `request_url` VARCHAR(500) DEFAULT NULL COMMENT 'Request URL',
    `request_params` TEXT DEFAULT NULL COMMENT 'Request Parameters (JSON)',

    -- Response info
    `response_status` INT DEFAULT NULL COMMENT 'HTTP Status Code',
    `response_time` INT DEFAULT NULL COMMENT 'Response Time (ms)',
    `error_message` TEXT DEFAULT NULL COMMENT 'Error Message',

    -- Client info
    `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'IP Address',
    `user_agent` VARCHAR(500) DEFAULT NULL COMMENT 'User Agent',
    `browser` VARCHAR(50) DEFAULT NULL COMMENT 'Browser',
    `os` VARCHAR(50) DEFAULT NULL COMMENT 'Operating System',

    -- Timestamps
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created At',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated At',
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'Soft Delete Flag',

    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_username` (`username`),
    KEY `idx_operation_module` (`operation_module`),
    KEY `idx_operation_type` (`operation_type`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Operation Logs';

-- ---------------------------------------------------------------------------
-- 1.2 Audit Logs - Detailed change tracking with before/after values
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `audit_logs`;
CREATE TABLE `audit_logs` (
    `id` BIGINT NOT NULL COMMENT 'Primary Key',

    -- Operation info
    `operation_type` VARCHAR(50) NOT NULL COMMENT 'Operation Type (CREATE/UPDATE/DELETE/STATUS_CHANGE)',
    `operation_module` VARCHAR(50) NOT NULL COMMENT 'Operation Module',
    `operation_desc` VARCHAR(500) DEFAULT NULL COMMENT 'Operation Description',

    -- Target entity
    `target_type` VARCHAR(50) DEFAULT NULL COMMENT 'Target Entity Type',
    `target_id` BIGINT DEFAULT NULL COMMENT 'Target Entity ID',
    `target_name` VARCHAR(200) DEFAULT NULL COMMENT 'Target Entity Name',

    -- Operator info
    `operator_id` BIGINT NOT NULL COMMENT 'Operator ID',
    `operator_name` VARCHAR(50) NOT NULL COMMENT 'Operator Name',
    `operator_ip` VARCHAR(50) DEFAULT NULL COMMENT 'Operator IP',
    `operator_ua` VARCHAR(500) DEFAULT NULL COMMENT 'User Agent',

    -- Change tracking
    `old_value` JSON DEFAULT NULL COMMENT 'Old Value (JSON)',
    `new_value` JSON DEFAULT NULL COMMENT 'New Value (JSON)',
    `diff_value` JSON DEFAULT NULL COMMENT 'Diff Value (JSON)',

    -- Request context
    `request_url` VARCHAR(500) DEFAULT NULL COMMENT 'Request URL',
    `request_method` VARCHAR(10) DEFAULT NULL COMMENT 'Request Method',
    `request_params` JSON DEFAULT NULL COMMENT 'Request Parameters',

    -- Result
    `response_code` INT DEFAULT NULL COMMENT 'Response Code',
    `execution_time` BIGINT DEFAULT NULL COMMENT 'Execution Time (ms)',
    `success` TINYINT DEFAULT 1 COMMENT 'Is Success (0/1)',
    `error_message` TEXT DEFAULT NULL COMMENT 'Error Message',

    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created At',

    PRIMARY KEY (`id`),
    KEY `idx_operator` (`operator_id`),
    KEY `idx_module` (`operation_module`),
    KEY `idx_target` (`target_type`, `target_id`),
    KEY `idx_time` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Detailed Audit Logs';

-- ---------------------------------------------------------------------------
-- 1.3 Check Audit Logs - Inspection-specific audit trail
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `check_audit_logs`;
CREATE TABLE `check_audit_logs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',

    -- Operation info
    `operation_type` VARCHAR(50) NOT NULL COMMENT 'Operation Type',
    `operation_desc` VARCHAR(200) DEFAULT NULL COMMENT 'Operation Description',

    -- Related entities
    `record_id` BIGINT DEFAULT NULL COMMENT 'Check Record ID',
    `template_id` BIGINT DEFAULT NULL COMMENT 'Template ID',
    `class_id` BIGINT DEFAULT NULL COMMENT 'Class ID',
    `deduction_id` BIGINT DEFAULT NULL COMMENT 'Deduction Detail ID',

    -- Operator info
    `operator_id` BIGINT NOT NULL COMMENT 'Operator ID',
    `operator_name` VARCHAR(50) DEFAULT NULL COMMENT 'Operator Name',
    `operator_ip` VARCHAR(50) DEFAULT NULL COMMENT 'IP Address',

    -- Change tracking
    `before_data` JSON DEFAULT NULL COMMENT 'Before Data (JSON)',
    `after_data` JSON DEFAULT NULL COMMENT 'After Data (JSON)',

    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created At',

    PRIMARY KEY (`id`),
    KEY `idx_record` (`record_id`),
    KEY `idx_template` (`template_id`),
    KEY `idx_class` (`class_id`),
    KEY `idx_operator` (`operator_id`),
    KEY `idx_time` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Inspection Audit Logs';

-- ---------------------------------------------------------------------------
-- 1.4 Appeal Audit Logs - Appeal process tracking
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `appeal_audit_logs`;
CREATE TABLE `appeal_audit_logs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `appeal_id` BIGINT NOT NULL COMMENT 'Appeal ID',

    -- Action info
    `action_type` TINYINT NOT NULL COMMENT 'Action Type (1=Submit,2=Review,3=Cancel,4=Modify,5=Publish,6=Effective)',
    `action_user_id` BIGINT NOT NULL COMMENT 'Action User ID',
    `action_user_name` VARCHAR(50) DEFAULT NULL COMMENT 'Action User Name',
    `action_time` DATETIME NOT NULL COMMENT 'Action Time',

    -- Status tracking
    `before_status` TINYINT DEFAULT NULL COMMENT 'Before Status',
    `after_status` TINYINT DEFAULT NULL COMMENT 'After Status',
    `before_score` DECIMAL(10,2) DEFAULT NULL COMMENT 'Before Score',
    `after_score` DECIMAL(10,2) DEFAULT NULL COMMENT 'After Score',

    -- Details
    `comment` TEXT DEFAULT NULL COMMENT 'Comment',
    `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'IP Address',
    `user_agent` VARCHAR(200) DEFAULT NULL COMMENT 'User Agent',

    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created At',

    PRIMARY KEY (`id`),
    KEY `idx_appeal` (`appeal_id`),
    KEY `idx_action_time` (`action_time`),
    KEY `idx_action_type` (`action_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Appeal Audit Logs';

-- ---------------------------------------------------------------------------
-- 1.5 Rating Change Log - Rating change tracking
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `rating_change_logs`;
CREATE TABLE `rating_change_logs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',

    -- Related entities
    `record_id` BIGINT NOT NULL COMMENT 'Check Record ID',
    `class_id` BIGINT NOT NULL COMMENT 'Class ID',
    `plan_id` BIGINT DEFAULT NULL COMMENT 'Check Plan ID',

    -- Change tracking
    `change_type` VARCHAR(20) NOT NULL COMMENT 'Change Type (INITIAL/APPEAL/CORRECTION/RECALCULATE)',
    `old_rating` VARCHAR(20) DEFAULT NULL COMMENT 'Old Rating',
    `new_rating` VARCHAR(20) NOT NULL COMMENT 'New Rating',
    `old_score` DECIMAL(10,2) DEFAULT NULL COMMENT 'Old Score',
    `new_score` DECIMAL(10,2) NOT NULL COMMENT 'New Score',

    -- Reason
    `reason` VARCHAR(500) DEFAULT NULL COMMENT 'Change Reason',
    `related_appeal_id` BIGINT DEFAULT NULL COMMENT 'Related Appeal ID',

    -- Operator
    `changed_by` BIGINT NOT NULL COMMENT 'Changed By User ID',
    `changed_by_name` VARCHAR(50) DEFAULT NULL COMMENT 'Changed By User Name',

    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created At',

    PRIMARY KEY (`id`),
    KEY `idx_record` (`record_id`),
    KEY `idx_class` (`class_id`),
    KEY `idx_plan` (`plan_id`),
    KEY `idx_time` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Rating Change Log';

-- ---------------------------------------------------------------------------
-- 1.6 Task Logs - Task operation tracking
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `task_logs`;
CREATE TABLE `task_logs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `task_id` BIGINT NOT NULL COMMENT 'Task ID',

    -- Operation info
    `action_type` VARCHAR(50) NOT NULL COMMENT 'Action Type (CREATE/ASSIGN/SUBMIT/APPROVE/REJECT/COMPLETE)',
    `action_desc` VARCHAR(500) DEFAULT NULL COMMENT 'Action Description',

    -- Status tracking
    `before_status` VARCHAR(20) DEFAULT NULL COMMENT 'Before Status',
    `after_status` VARCHAR(20) DEFAULT NULL COMMENT 'After Status',

    -- Operator
    `operator_id` BIGINT NOT NULL COMMENT 'Operator ID',
    `operator_name` VARCHAR(50) DEFAULT NULL COMMENT 'Operator Name',
    `operator_role` VARCHAR(50) DEFAULT NULL COMMENT 'Operator Role',

    -- Additional data
    `extra_data` JSON DEFAULT NULL COMMENT 'Extra Data (JSON)',
    `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'IP Address',

    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created At',

    PRIMARY KEY (`id`),
    KEY `idx_task` (`task_id`),
    KEY `idx_operator` (`operator_id`),
    KEY `idx_action_type` (`action_type`),
    KEY `idx_time` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Task Operation Logs';

-- ============================================================================
-- PART 2: DOMAIN EVENTS (Event Sourcing Support)
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 2.1 Domain Events - Core event store
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `domain_events`;
CREATE TABLE `domain_events` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `event_id` VARCHAR(36) NOT NULL COMMENT 'Event UUID',

    -- Event info
    `event_type` VARCHAR(100) NOT NULL COMMENT 'Event Type (e.g., ClassCreatedEvent)',
    `aggregate_type` VARCHAR(100) NOT NULL COMMENT 'Aggregate Type (e.g., SchoolClass)',
    `aggregate_id` VARCHAR(100) NOT NULL COMMENT 'Aggregate ID',
    `aggregate_version` INT NOT NULL COMMENT 'Aggregate Version',

    -- Event data
    `payload` JSON NOT NULL COMMENT 'Event Payload (JSON)',
    `metadata` JSON DEFAULT NULL COMMENT 'Event Metadata (JSON)',

    -- Timestamps
    `occurred_at` DATETIME(3) NOT NULL COMMENT 'When Event Occurred',
    `created_at` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'When Record Created',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_event_id` (`event_id`),
    KEY `idx_aggregate` (`aggregate_type`, `aggregate_id`),
    KEY `idx_occurred` (`occurred_at`),
    KEY `idx_type` (`event_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Domain Events Store';

-- ---------------------------------------------------------------------------
-- 2.2 Event Publications - Outbox pattern for reliable event publishing
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `event_publications`;
CREATE TABLE `event_publications` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `event_id` VARCHAR(36) NOT NULL COMMENT 'Event ID (FK to domain_events)',
    `event_type` VARCHAR(100) NOT NULL COMMENT 'Event Type',

    -- Publication status
    `status` ENUM('PENDING', 'PUBLISHED', 'FAILED') DEFAULT 'PENDING' COMMENT 'Publication Status',
    `retry_count` INT DEFAULT 0 COMMENT 'Retry Count',
    `last_error` TEXT DEFAULT NULL COMMENT 'Last Error Message',

    -- Target info
    `target_channel` VARCHAR(100) DEFAULT NULL COMMENT 'Target Channel/Queue',

    -- Timestamps
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created At',
    `published_at` DATETIME DEFAULT NULL COMMENT 'Published At',
    `next_retry_at` DATETIME DEFAULT NULL COMMENT 'Next Retry Time',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_event_id` (`event_id`),
    KEY `idx_status` (`status`),
    KEY `idx_next_retry` (`status`, `next_retry_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Event Publications Outbox';

-- ============================================================================
-- PART 3: BUSINESS SNAPSHOTS
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 3.1 Class Size Snapshots - Daily class population records
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `class_size_snapshots`;
CREATE TABLE `class_size_snapshots` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `class_id` BIGINT NOT NULL COMMENT 'Class ID',
    `snapshot_date` DATE NOT NULL COMMENT 'Snapshot Date',

    -- Population counts
    `student_count` INT NOT NULL COMMENT 'Total Student Count',
    `active_count` INT DEFAULT NULL COMMENT 'Active Students (excluding leave/suspension)',
    `male_count` INT DEFAULT NULL COMMENT 'Male Count',
    `female_count` INT DEFAULT NULL COMMENT 'Female Count',

    -- Metadata
    `snapshot_source` VARCHAR(20) DEFAULT 'AUTO' COMMENT 'Source (AUTO/MANUAL/PUBLISH)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created At',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_class_date` (`class_id`, `snapshot_date`),
    KEY `idx_snapshot_date` (`snapshot_date`),
    KEY `idx_class_id` (`class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Class Size Snapshots';

-- ---------------------------------------------------------------------------
-- 3.2 Student Relationship Snapshots - Student org assignments at point in time
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `student_relationship_snapshots`;
CREATE TABLE `student_relationship_snapshots` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `snapshot_date` DATE NOT NULL COMMENT 'Snapshot Date',
    `snapshot_type` VARCHAR(20) NOT NULL DEFAULT 'DAILY' COMMENT 'Type (DAILY/SEMESTER_START/SEMESTER_END)',

    -- Student info
    `student_id` BIGINT NOT NULL COMMENT 'Student ID',
    `student_no` VARCHAR(50) DEFAULT NULL COMMENT 'Student Number',
    `student_name` VARCHAR(50) DEFAULT NULL COMMENT 'Student Name',

    -- Organization assignments
    `class_id` BIGINT DEFAULT NULL COMMENT 'Class ID',
    `class_name` VARCHAR(100) DEFAULT NULL COMMENT 'Class Name',
    `grade_id` BIGINT DEFAULT NULL COMMENT 'Grade ID',
    `grade_name` VARCHAR(50) DEFAULT NULL COMMENT 'Grade Name',
    `department_id` BIGINT DEFAULT NULL COMMENT 'Department ID',
    `department_name` VARCHAR(100) DEFAULT NULL COMMENT 'Department Name',

    -- Dormitory assignments
    `dormitory_id` BIGINT DEFAULT NULL COMMENT 'Dormitory ID',
    `building_id` BIGINT DEFAULT NULL COMMENT 'Building ID',
    `building_name` VARCHAR(50) DEFAULT NULL COMMENT 'Building Name',
    `dormitory_no` VARCHAR(50) DEFAULT NULL COMMENT 'Dormitory Number',
    `bed_no` VARCHAR(20) DEFAULT NULL COMMENT 'Bed Number',
    `is_dorm_leader` TINYINT DEFAULT 0 COMMENT 'Is Dorm Leader (0/1)',

    -- Status
    `student_status` TINYINT DEFAULT NULL COMMENT 'Status (1=Active,2=Suspended,3=Withdrawn,4=Graduated)',

    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created At',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_date_student` (`snapshot_date`, `student_id`),
    KEY `idx_snapshot_date` (`snapshot_date`),
    KEY `idx_student` (`student_id`),
    KEY `idx_class` (`class_id`),
    KEY `idx_dormitory` (`dormitory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Student Relationship Snapshots';

-- ---------------------------------------------------------------------------
-- 3.3 Dormitory Member Snapshots - Dormitory composition at point in time
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `dormitory_member_snapshots`;
CREATE TABLE `dormitory_member_snapshots` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `snapshot_date` DATE NOT NULL COMMENT 'Snapshot Date',
    `record_id` BIGINT DEFAULT NULL COMMENT 'Related Check Record ID',

    -- Dormitory info
    `dormitory_id` BIGINT NOT NULL COMMENT 'Dormitory ID',
    `building_id` BIGINT DEFAULT NULL COMMENT 'Building ID',
    `building_name` VARCHAR(50) DEFAULT NULL COMMENT 'Building Name',
    `dormitory_no` VARCHAR(50) DEFAULT NULL COMMENT 'Dormitory Number',

    -- Members
    `members` JSON NOT NULL COMMENT 'Members Array [{studentId,studentNo,studentName,bedNo,isDormLeader}]',
    `member_count` INT DEFAULT 0 COMMENT 'Member Count',
    `leader_id` BIGINT DEFAULT NULL COMMENT 'Dorm Leader ID',
    `leader_name` VARCHAR(50) DEFAULT NULL COMMENT 'Dorm Leader Name',

    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created At',

    PRIMARY KEY (`id`),
    KEY `idx_snapshot_date` (`snapshot_date`),
    KEY `idx_dormitory` (`dormitory_id`),
    KEY `idx_record` (`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Dormitory Member Snapshots';

-- ---------------------------------------------------------------------------
-- 3.4 Analysis Snapshots - Statistical analysis point-in-time captures
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `analysis_snapshots`;
CREATE TABLE `analysis_snapshots` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `config_id` BIGINT NOT NULL COMMENT 'Analysis Config ID',
    `snapshot_name` VARCHAR(100) DEFAULT NULL COMMENT 'Snapshot Name',
    `snapshot_desc` VARCHAR(500) DEFAULT NULL COMMENT 'Snapshot Description',

    -- Scope info
    `record_ids` JSON DEFAULT NULL COMMENT 'Included Record IDs',
    `class_ids` JSON DEFAULT NULL COMMENT 'Included Class IDs',
    `date_range_start` DATE DEFAULT NULL COMMENT 'Data Start Date',
    `date_range_end` DATE DEFAULT NULL COMMENT 'Data End Date',

    -- Summary stats
    `record_count` INT DEFAULT 0 COMMENT 'Record Count',
    `class_count` INT DEFAULT 0 COMMENT 'Class Count',
    `total_score` DECIMAL(12,2) DEFAULT 0 COMMENT 'Total Deductions',
    `avg_score` DECIMAL(10,2) DEFAULT 0 COMMENT 'Average Deduction',

    -- Detailed data
    `overview_data` JSON DEFAULT NULL COMMENT 'Overview Data (JSON)',
    `metrics_data` JSON DEFAULT NULL COMMENT 'Metrics Results (JSON)',

    -- Metadata
    `generated_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Generated At',
    `generated_by` BIGINT DEFAULT NULL COMMENT 'Generated By User ID',
    `generated_by_name` VARCHAR(50) DEFAULT NULL COMMENT 'Generated By User Name',
    `is_auto` TINYINT(1) DEFAULT 0 COMMENT 'Is Auto-Generated (0/1)',
    `version` INT DEFAULT 1 COMMENT 'Version Number',

    PRIMARY KEY (`id`),
    KEY `idx_config_id` (`config_id`),
    KEY `idx_generated_at` (`generated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Analysis Snapshots';

-- ---------------------------------------------------------------------------
-- 3.5 Evaluation Result History - Historical evaluation records
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `evaluation_result_history`;
CREATE TABLE `evaluation_result_history` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',

    -- Original result reference
    `original_id` BIGINT NOT NULL COMMENT 'Original Result ID',
    `record_id` BIGINT NOT NULL COMMENT 'Check Record ID',

    -- Entity info
    `entity_type` VARCHAR(20) NOT NULL COMMENT 'Entity Type (CLASS/DORMITORY/STUDENT)',
    `entity_id` BIGINT NOT NULL COMMENT 'Entity ID',
    `entity_name` VARCHAR(100) DEFAULT NULL COMMENT 'Entity Name',

    -- Score info
    `base_score` DECIMAL(10,2) DEFAULT 100.00 COMMENT 'Base Score',
    `deduction_score` DECIMAL(10,2) DEFAULT 0.00 COMMENT 'Deduction Score',
    `final_score` DECIMAL(10,2) DEFAULT 100.00 COMMENT 'Final Score',
    `weighted_score` DECIMAL(10,2) DEFAULT NULL COMMENT 'Weighted Score',

    -- Rating
    `rating` VARCHAR(20) DEFAULT NULL COMMENT 'Rating (A/B/C/D/E)',

    -- Metadata
    `snapshot_reason` VARCHAR(100) DEFAULT NULL COMMENT 'Snapshot Reason',
    `snapshot_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Snapshot Time',
    `snapshot_by` BIGINT DEFAULT NULL COMMENT 'Snapshot By User ID',

    PRIMARY KEY (`id`),
    KEY `idx_original` (`original_id`),
    KEY `idx_record` (`record_id`),
    KEY `idx_entity` (`entity_type`, `entity_id`),
    KEY `idx_snapshot_at` (`snapshot_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Evaluation Result History';

-- ============================================================================
-- PART 4: LOGIN & SESSION TRACKING
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 4.1 Login Logs - User login/logout tracking
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `login_logs`;
CREATE TABLE `login_logs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `user_id` BIGINT DEFAULT NULL COMMENT 'User ID (NULL if login failed)',
    `username` VARCHAR(50) NOT NULL COMMENT 'Username',

    -- Login info
    `login_type` VARCHAR(20) NOT NULL DEFAULT 'PASSWORD' COMMENT 'Login Type (PASSWORD/WECHAT/SSO)',
    `login_status` TINYINT NOT NULL COMMENT 'Status (1=Success,0=Failed)',
    `login_message` VARCHAR(200) DEFAULT NULL COMMENT 'Login Message',

    -- Client info
    `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'IP Address',
    `user_agent` VARCHAR(500) DEFAULT NULL COMMENT 'User Agent',
    `browser` VARCHAR(50) DEFAULT NULL COMMENT 'Browser',
    `os` VARCHAR(50) DEFAULT NULL COMMENT 'Operating System',
    `device_type` VARCHAR(20) DEFAULT NULL COMMENT 'Device Type (PC/MOBILE/TABLET)',

    -- Location (optional)
    `location` VARCHAR(100) DEFAULT NULL COMMENT 'Location',

    -- Session
    `session_id` VARCHAR(100) DEFAULT NULL COMMENT 'Session ID',
    `logout_time` DATETIME DEFAULT NULL COMMENT 'Logout Time',
    `logout_type` VARCHAR(20) DEFAULT NULL COMMENT 'Logout Type (MANUAL/TIMEOUT/FORCED)',

    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Login Time',

    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_username` (`username`),
    KEY `idx_login_status` (`login_status`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_ip_address` (`ip_address`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Login Logs';

SET FOREIGN_KEY_CHECKS = 1;
-- ============================================================================
-- Student Management System V2 - Index Definitions
-- ============================================================================
-- Version: 2.0
-- Description: Consolidated index definitions for performance optimization
-- Created: 2026-01-06
-- ============================================================================
--
-- NOTE: Most indexes are defined within table DDL in the main schema files.
-- This file contains additional composite indexes and performance-critical
-- indexes that may need to be added after data migration.
-- ============================================================================

SET NAMES utf8mb4;

-- ============================================================================
-- PART 1: CORE TABLES - Additional Performance Indexes
-- ============================================================================

-- ---------------------------------------------------------------------------
-- Users & Authentication
-- ---------------------------------------------------------------------------
-- Covering index for login authentication
CREATE INDEX IF NOT EXISTS idx_users_login_cover
    ON users (username, password, status, deleted);

-- User search optimization
CREATE INDEX IF NOT EXISTS idx_users_search
    ON users (real_name, phone, status);

-- ---------------------------------------------------------------------------
-- Roles & Permissions
-- ---------------------------------------------------------------------------
-- Role hierarchy lookup
CREATE INDEX IF NOT EXISTS idx_roles_parent_status
    ON roles (parent_id, status, deleted);

-- Permission tree traversal
CREATE INDEX IF NOT EXISTS idx_permissions_tree
    ON permissions (parent_id, permission_type, sort_order);

-- ---------------------------------------------------------------------------
-- Organization Units
-- ---------------------------------------------------------------------------
-- Org unit hierarchy (for tree queries)
CREATE INDEX IF NOT EXISTS idx_org_units_hierarchy
    ON org_units (parent_id, unit_level, status, deleted);

-- Org unit by type and code
CREATE INDEX IF NOT EXISTS idx_org_units_type_code
    ON org_units (unit_type, unit_code, deleted);

-- ---------------------------------------------------------------------------
-- Classes
-- ---------------------------------------------------------------------------
-- Class by org unit and status (common query pattern)
CREATE INDEX IF NOT EXISTS idx_classes_org_status
    ON classes (org_unit_id, status, deleted);

-- Class by grade and enrollment year
CREATE INDEX IF NOT EXISTS idx_classes_grade_year
    ON classes (grade_id, enrollment_year, deleted);

-- Full-text search on class name (if MySQL 5.7+)
-- CREATE FULLTEXT INDEX IF NOT EXISTS ft_classes_name ON classes (class_name);

-- ---------------------------------------------------------------------------
-- Students
-- ---------------------------------------------------------------------------
-- Student by class (most common lookup)
CREATE INDEX IF NOT EXISTS idx_students_class
    ON students (class_id, status, deleted);

-- Student search optimization
CREATE INDEX IF NOT EXISTS idx_students_search
    ON students (name, student_no, id_card, phone);

-- Student by dormitory
CREATE INDEX IF NOT EXISTS idx_students_dormitory
    ON students (dormitory_id, bed_no, deleted);

-- ---------------------------------------------------------------------------
-- Dormitories
-- ---------------------------------------------------------------------------
-- Dormitory by building and floor
CREATE INDEX IF NOT EXISTS idx_dormitories_location
    ON dormitories (building_id, floor, status);

-- Dormitory capacity management
CREATE INDEX IF NOT EXISTS idx_dormitories_capacity
    ON dormitories (capacity, current_count, building_id);

-- ============================================================================
-- PART 2: INSPECTION TABLES - Performance Indexes
-- ============================================================================

-- ---------------------------------------------------------------------------
-- Check Templates
-- ---------------------------------------------------------------------------
-- Template by category and status
CREATE INDEX IF NOT EXISTS idx_templates_category_status
    ON check_templates (category_id, status, deleted);

-- Active templates for dropdown
CREATE INDEX IF NOT EXISTS idx_templates_active
    ON check_templates (status, deleted, sort_order);

-- ---------------------------------------------------------------------------
-- Check Plans
-- ---------------------------------------------------------------------------
-- Plans by date range (most common query)
CREATE INDEX IF NOT EXISTS idx_plans_date_range
    ON check_plans (start_date, end_date, status, deleted);

-- Plans by semester
CREATE INDEX IF NOT EXISTS idx_plans_semester
    ON check_plans (semester_id, status, deleted);

-- ---------------------------------------------------------------------------
-- Check Records
-- ---------------------------------------------------------------------------
-- Records by plan and status (dashboard queries)
CREATE INDEX IF NOT EXISTS idx_records_plan_status
    ON check_records (plan_id, status, deleted);

-- Records by check date (date-based queries)
CREATE INDEX IF NOT EXISTS idx_records_check_date
    ON check_records (check_date, status, deleted);

-- Records by inspector (my tasks)
CREATE INDEX IF NOT EXISTS idx_records_inspector
    ON check_records (inspector_id, status, deleted);

-- Records by template and date
CREATE INDEX IF NOT EXISTS idx_records_template_date
    ON check_records (template_id, check_date, deleted);

-- ---------------------------------------------------------------------------
-- Class Scores
-- ---------------------------------------------------------------------------
-- Scores by record (record detail page)
CREATE INDEX IF NOT EXISTS idx_scores_record
    ON class_scores (record_id, class_id);

-- Scores by class and date range (class statistics)
CREATE INDEX IF NOT EXISTS idx_scores_class_date
    ON class_scores (class_id, check_date, deleted);

-- Ranking query optimization
CREATE INDEX IF NOT EXISTS idx_scores_ranking
    ON class_scores (record_id, final_score DESC, deleted);

-- Rating distribution
CREATE INDEX IF NOT EXISTS idx_scores_rating
    ON class_scores (record_id, rating, deleted);

-- ---------------------------------------------------------------------------
-- Deduction Details
-- ---------------------------------------------------------------------------
-- Deductions by score record
CREATE INDEX IF NOT EXISTS idx_deductions_score
    ON deduction_details (score_id, deleted);

-- Deductions by item (item statistics)
CREATE INDEX IF NOT EXISTS idx_deductions_item
    ON deduction_details (item_id, record_id, deleted);

-- Deductions by class (class history)
CREATE INDEX IF NOT EXISTS idx_deductions_class
    ON deduction_details (class_id, check_date, deleted);

-- ---------------------------------------------------------------------------
-- Appeals
-- ---------------------------------------------------------------------------
-- Appeals by status (pending review list)
CREATE INDEX IF NOT EXISTS idx_appeals_status
    ON check_item_appeals (status, deleted, created_at DESC);

-- Appeals by class (class appeal history)
CREATE INDEX IF NOT EXISTS idx_appeals_class
    ON check_item_appeals (class_id, status, deleted);

-- Appeals by reviewer (my reviews)
CREATE INDEX IF NOT EXISTS idx_appeals_reviewer
    ON check_item_appeals (reviewer_id, status, deleted);

-- ============================================================================
-- PART 3: RATING TABLES - Performance Indexes
-- ============================================================================

-- ---------------------------------------------------------------------------
-- Rating Configs
-- ---------------------------------------------------------------------------
-- Active config lookup
CREATE INDEX IF NOT EXISTS idx_rating_configs_active
    ON rating_configs (status, is_default, deleted);

-- ---------------------------------------------------------------------------
-- Check Plan Ratings
-- ---------------------------------------------------------------------------
-- Ratings by plan and class (result lookup)
CREATE INDEX IF NOT EXISTS idx_plan_ratings_lookup
    ON check_plan_ratings (plan_id, class_id);

-- Ratings by class (class history)
CREATE INDEX IF NOT EXISTS idx_plan_ratings_class
    ON check_plan_ratings (class_id, plan_id);

-- Rating distribution by plan
CREATE INDEX IF NOT EXISTS idx_plan_ratings_distribution
    ON check_plan_ratings (plan_id, final_rating);

-- ============================================================================
-- PART 4: TASK TABLES - Performance Indexes
-- ============================================================================

-- ---------------------------------------------------------------------------
-- Tasks
-- ---------------------------------------------------------------------------
-- Tasks by status (task list queries)
CREATE INDEX IF NOT EXISTS idx_tasks_status
    ON tasks (status, deleted, deadline DESC);

-- Tasks by creator
CREATE INDEX IF NOT EXISTS idx_tasks_creator
    ON tasks (created_by, status, deleted);

-- Tasks by department
CREATE INDEX IF NOT EXISTS idx_tasks_department
    ON tasks (department_id, status, deleted);

-- Tasks by deadline (overdue alerts)
CREATE INDEX IF NOT EXISTS idx_tasks_deadline
    ON tasks (deadline, status, deleted);

-- ---------------------------------------------------------------------------
-- Task Assignees
-- ---------------------------------------------------------------------------
-- My tasks (most common query)
CREATE INDEX IF NOT EXISTS idx_assignees_user
    ON task_assignees (user_id, status, task_id);

-- Task members
CREATE INDEX IF NOT EXISTS idx_assignees_task
    ON task_assignees (task_id, assignee_type, status);

-- ---------------------------------------------------------------------------
-- Task Submissions
-- ---------------------------------------------------------------------------
-- Submissions by task and user
CREATE INDEX IF NOT EXISTS idx_submissions_task_user
    ON task_submissions (task_id, submitted_by, deleted);

-- Submissions by status
CREATE INDEX IF NOT EXISTS idx_submissions_status
    ON task_submissions (task_id, status, deleted);

-- ============================================================================
-- PART 5: AUDIT & LOG TABLES - Performance Indexes
-- ============================================================================

-- ---------------------------------------------------------------------------
-- Operation Logs
-- ---------------------------------------------------------------------------
-- Logs by user and time (user activity)
CREATE INDEX IF NOT EXISTS idx_oplogs_user_time
    ON operation_logs (user_id, created_at DESC, deleted);

-- Logs by module (module audit)
CREATE INDEX IF NOT EXISTS idx_oplogs_module_time
    ON operation_logs (operation_module, created_at DESC, deleted);

-- ---------------------------------------------------------------------------
-- Audit Logs
-- ---------------------------------------------------------------------------
-- Audit by target (entity audit trail)
CREATE INDEX IF NOT EXISTS idx_auditlogs_target_time
    ON audit_logs (target_type, target_id, created_at DESC);

-- ---------------------------------------------------------------------------
-- Domain Events
-- ---------------------------------------------------------------------------
-- Events by aggregate (aggregate history)
CREATE INDEX IF NOT EXISTS idx_events_aggregate_version
    ON domain_events (aggregate_type, aggregate_id, aggregate_version);

-- Events by time range (event replay)
CREATE INDEX IF NOT EXISTS idx_events_time_range
    ON domain_events (occurred_at, aggregate_type);

-- ---------------------------------------------------------------------------
-- Snapshots
-- ---------------------------------------------------------------------------
-- Class size by date range
CREATE INDEX IF NOT EXISTS idx_class_size_date_range
    ON class_size_snapshots (snapshot_date, class_id);

-- Student relationship by date
CREATE INDEX IF NOT EXISTS idx_student_rel_date
    ON student_relationship_snapshots (snapshot_date, student_id);

-- ============================================================================
-- PART 6: STATISTICS & REPORTING - Materialized View-like Indexes
-- ============================================================================

-- For daily statistics aggregation
CREATE INDEX IF NOT EXISTS idx_records_daily_stats
    ON check_records (check_date, template_id, status)
    WHERE deleted = 0;

-- For class ranking across multiple records
CREATE INDEX IF NOT EXISTS idx_scores_class_ranking
    ON class_scores (class_id, final_score, check_date);

-- For deduction item frequency analysis
CREATE INDEX IF NOT EXISTS idx_deductions_item_freq
    ON deduction_details (item_id, check_date, class_id);

-- ============================================================================
-- INDEX MAINTENANCE NOTES
-- ============================================================================
--
-- 1. Monitor index usage with:
--    SELECT * FROM sys.schema_index_statistics WHERE table_schema = 'student_management';
--
-- 2. Check for unused indexes:
--    SELECT * FROM sys.schema_unused_indexes WHERE object_schema = 'student_management';
--
-- 3. Analyze table statistics periodically:
--    ANALYZE TABLE table_name;
--
-- 4. Consider partitioning for large tables:
--    - operation_logs: RANGE partition by created_at (monthly)
--    - domain_events: RANGE partition by occurred_at (monthly)
--    - check_records: RANGE partition by check_date (yearly)
--
-- 5. For very large deployments, consider:
--    - Read replicas for reporting queries
--    - Elasticsearch for full-text search
--    - Redis caching for hot data
--
-- ============================================================================


-- =====================================================
-- V4 DDD 新增表结构
-- 版本: 4.0
-- 创建日期: 2026-01-28
-- 包含: 检查会话、班级检查记录、扣分/加分明细、
--       申诉V2、排程、分析、纠正行动、学生行为、
--       资产管理、空间管理、评级、系统模块/消息
-- =====================================================

-- =====================================================
-- V4-1: 检查领域 (Inspection Domain)
-- =====================================================

-- ---------------------------------------------------
-- 检查会话表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `inspection_sessions`;
CREATE TABLE `inspection_sessions` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `session_code` VARCHAR(50) NOT NULL COMMENT '会话编码',
    `template_id` BIGINT NOT NULL COMMENT '模板ID',
    `template_version` INT DEFAULT 1 COMMENT '模板版本',
    `inspection_date` DATE NOT NULL COMMENT '检查日期',
    `inspection_period` VARCHAR(20) COMMENT '检查时段',
    `input_mode` VARCHAR(20) COMMENT '输入模式',
    `scoring_mode` VARCHAR(20) COMMENT '评分模式',
    `base_score` INT DEFAULT 100 COMMENT '基础分',
    `inspection_level` VARCHAR(20) COMMENT '检查级别',
    `status` VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态',
    `inspector_id` BIGINT COMMENT '检查员ID',
    `inspector_name` VARCHAR(50) COMMENT '检查员姓名',
    `submitted_at` DATETIME COMMENT '提交时间',
    `published_at` DATETIME COMMENT '发布时间',
    `remarks` VARCHAR(500) COMMENT '备注',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` BIGINT COMMENT '创建人',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_session_code` (`session_code`),
    INDEX `idx_template_id` (`template_id`),
    INDEX `idx_inspection_date` (`inspection_date`),
    INDEX `idx_status` (`status`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查会话表';

-- ---------------------------------------------------
-- 检查记录表 (V4)
-- ---------------------------------------------------
DROP TABLE IF EXISTS `inspection_records`;
CREATE TABLE `inspection_records` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `record_code` VARCHAR(50) NOT NULL COMMENT '记录编码',
    `template_id` BIGINT NOT NULL COMMENT '模板ID',
    `template_version` INT DEFAULT 1 COMMENT '模板版本',
    `round_id` BIGINT COMMENT '轮次ID',
    `inspection_date` DATE NOT NULL COMMENT '检查日期',
    `inspection_period` VARCHAR(20) COMMENT '检查时段',
    `status` VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态',
    `inspector_id` BIGINT COMMENT '检查员ID',
    `inspector_name` VARCHAR(50) COMMENT '检查员姓名',
    `inspected_at` DATETIME COMMENT '检查时间',
    `reviewer_id` BIGINT COMMENT '审核人ID',
    `reviewed_at` DATETIME COMMENT '审核时间',
    `published_at` DATETIME COMMENT '发布时间',
    `remarks` VARCHAR(500) COMMENT '备注',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` BIGINT COMMENT '创建人',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_record_code` (`record_code`),
    INDEX `idx_template_id` (`template_id`),
    INDEX `idx_inspection_date` (`inspection_date`),
    INDEX `idx_status` (`status`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查记录表(V4)';

-- ---------------------------------------------------
-- 班级检查记录表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `class_inspection_records`;
CREATE TABLE `class_inspection_records` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `session_id` BIGINT NOT NULL COMMENT '会话ID',
    `class_id` BIGINT NOT NULL COMMENT '班级ID',
    `class_name` VARCHAR(100) COMMENT '班级名称',
    `org_unit_id` BIGINT COMMENT '组织单元ID',
    `org_unit_name` VARCHAR(100) COMMENT '组织单元名称',
    `base_score` INT DEFAULT 100 COMMENT '基础分',
    `total_deduction` DECIMAL(10,2) DEFAULT 0 COMMENT '总扣分',
    `bonus_score` DECIMAL(10,2) DEFAULT 0 COMMENT '加分',
    `final_score` DECIMAL(10,2) COMMENT '最终得分',
    `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态',
    `remarks` VARCHAR(500) COMMENT '备注',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    INDEX `idx_session_id` (`session_id`),
    INDEX `idx_class_id` (`class_id`),
    INDEX `idx_org_unit_id` (`org_unit_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级检查记录表';

-- ---------------------------------------------------
-- 检查分类表 (V4)
-- ---------------------------------------------------
DROP TABLE IF EXISTS `inspection_categories`;
CREATE TABLE `inspection_categories` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `template_id` BIGINT NOT NULL COMMENT '模板ID',
    `category_code` VARCHAR(50) COMMENT '分类编码',
    `category_name` VARCHAR(100) NOT NULL COMMENT '分类名称',
    `description` VARCHAR(500) COMMENT '描述',
    `base_score` INT DEFAULT 100 COMMENT '基础分',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    INDEX `idx_template_id` (`template_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查分类表(V4)';

-- ---------------------------------------------------
-- 检查扣分明细表 (V4)
-- ---------------------------------------------------
DROP TABLE IF EXISTS `inspection_deductions`;
CREATE TABLE `inspection_deductions` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `session_id` BIGINT NOT NULL COMMENT '会话ID',
    `class_record_id` BIGINT NOT NULL COMMENT '班级检查记录ID',
    `deduction_item_id` BIGINT NOT NULL COMMENT '扣分项ID',
    `item_name` VARCHAR(100) COMMENT '扣分项名称',
    `category_name` VARCHAR(100) COMMENT '分类名称',
    `space_type` VARCHAR(20) COMMENT '空间类型',
    `space_id` BIGINT COMMENT '空间ID',
    `space_name` VARCHAR(100) COMMENT '空间名称',
    `student_ids` JSON COMMENT '学生ID列表',
    `student_names` JSON COMMENT '学生姓名列表',
    `person_count` INT DEFAULT 0 COMMENT '涉及人数',
    `deduction_amount` DECIMAL(10,2) NOT NULL COMMENT '扣分金额',
    `input_source` VARCHAR(20) COMMENT '输入来源',
    `remark` VARCHAR(500) COMMENT '备注',
    `evidence_urls` JSON COMMENT '证据图片URL列表',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    INDEX `idx_session_id` (`session_id`),
    INDEX `idx_class_record_id` (`class_record_id`),
    INDEX `idx_deduction_item_id` (`deduction_item_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查扣分明细表(V4)';

-- ---------------------------------------------------
-- 加分项目表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `bonus_items`;
CREATE TABLE `bonus_items` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `category_id` BIGINT COMMENT '分类ID',
    `item_name` VARCHAR(100) NOT NULL COMMENT '加分项名称',
    `bonus_mode` VARCHAR(20) COMMENT '加分模式',
    `fixed_bonus` DECIMAL(10,2) COMMENT '固定加分值',
    `progressive_config` JSON COMMENT '递进配置',
    `improvement_coefficient` DECIMAL(5,2) COMMENT '提升系数',
    `description` VARCHAR(500) COMMENT '描述',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `status` INT DEFAULT 1 COMMENT '状态:0禁用,1启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    INDEX `idx_category_id` (`category_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='加分项目表';

-- ---------------------------------------------------
-- 检查加分记录表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `inspection_bonuses`;
CREATE TABLE `inspection_bonuses` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `class_record_id` BIGINT NOT NULL COMMENT '班级检查记录ID',
    `session_id` BIGINT NOT NULL COMMENT '会话ID',
    `class_id` BIGINT NOT NULL COMMENT '班级ID',
    `bonus_item_id` BIGINT NOT NULL COMMENT '加分项ID',
    `bonus_score` DECIMAL(10,2) NOT NULL COMMENT '加分值',
    `reason` VARCHAR(500) COMMENT '加分原因',
    `recorded_by` BIGINT COMMENT '记录人',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    INDEX `idx_class_record_id` (`class_record_id`),
    INDEX `idx_session_id` (`session_id`),
    INDEX `idx_class_id` (`class_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查加分记录表';

-- ---------------------------------------------------
-- 检查清单响应表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `checklist_responses`;
CREATE TABLE `checklist_responses` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `session_id` BIGINT NOT NULL COMMENT '会话ID',
    `class_record_id` BIGINT NOT NULL COMMENT '班级检查记录ID',
    `checklist_item_id` BIGINT NOT NULL COMMENT '检查项ID',
    `item_name` VARCHAR(100) COMMENT '检查项名称',
    `category_name` VARCHAR(100) COMMENT '分类名称',
    `result` VARCHAR(20) COMMENT '结果',
    `auto_deduction` DECIMAL(10,2) COMMENT '自动扣分',
    `inspector_note` VARCHAR(500) COMMENT '检查员备注',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_session_id` (`session_id`),
    INDEX `idx_class_record_id` (`class_record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查清单响应表';

-- ---------------------------------------------------
-- 申诉表 (V4)
-- ---------------------------------------------------
DROP TABLE IF EXISTS `appeals`;
CREATE TABLE `appeals` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `inspection_record_id` BIGINT COMMENT '检查记录ID',
    `deduction_detail_id` BIGINT COMMENT '扣分明细ID',
    `class_id` BIGINT COMMENT '班级ID',
    `appeal_code` VARCHAR(50) COMMENT '申诉编码',
    `reason` TEXT COMMENT '申诉原因',
    `attachments` JSON COMMENT '附件URL列表',
    `original_deduction` DECIMAL(10,2) COMMENT '原扣分',
    `requested_deduction` DECIMAL(10,2) COMMENT '申请扣分',
    `approved_deduction` DECIMAL(10,2) COMMENT '批准扣分',
    `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态',
    `applicant_id` BIGINT COMMENT '申请人ID',
    `applied_at` DATETIME COMMENT '申请时间',
    `level1_reviewer_id` BIGINT COMMENT '一级审核人ID',
    `level1_reviewed_at` DATETIME COMMENT '一级审核时间',
    `level1_comment` VARCHAR(500) COMMENT '一级审核意见',
    `level2_reviewer_id` BIGINT COMMENT '二级审核人ID',
    `level2_reviewed_at` DATETIME COMMENT '二级审核时间',
    `level2_comment` VARCHAR(500) COMMENT '二级审核意见',
    `effective_at` DATETIME COMMENT '生效时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_appeal_code` (`appeal_code`),
    INDEX `idx_inspection_record_id` (`inspection_record_id`),
    INDEX `idx_class_id` (`class_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='申诉表(V4)';

-- ---------------------------------------------------
-- 申诉审批表 (V4)
-- ---------------------------------------------------
DROP TABLE IF EXISTS `appeal_approvals`;
CREATE TABLE `appeal_approvals` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `appeal_id` BIGINT NOT NULL COMMENT '申诉ID',
    `reviewer_id` BIGINT NOT NULL COMMENT '审核人ID',
    `review_level` VARCHAR(20) COMMENT '审核级别',
    `action` VARCHAR(20) COMMENT '操作:APPROVE/REJECT',
    `comment` VARCHAR(500) COMMENT '审核意见',
    `reviewed_at` DATETIME COMMENT '审核时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_appeal_id` (`appeal_id`),
    INDEX `idx_reviewer_id` (`reviewer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='申诉审批表(V4)';

-- =====================================================
-- V4-2: 排程领域 (Schedule Domain)
-- =====================================================

-- ---------------------------------------------------
-- 排程策略表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `schedule_policies`;
CREATE TABLE `schedule_policies` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `policy_code` VARCHAR(50) NOT NULL COMMENT '策略编码',
    `policy_name` VARCHAR(100) NOT NULL COMMENT '策略名称',
    `policy_type` VARCHAR(20) COMMENT '策略类型',
    `rotation_algorithm` VARCHAR(20) COMMENT '轮换算法',
    `template_id` BIGINT COMMENT '关联模板ID',
    `inspector_pool` JSON COMMENT '检查员池',
    `schedule_config` JSON COMMENT '排程配置',
    `excluded_dates` JSON COMMENT '排除日期',
    `is_enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    `created_by` BIGINT COMMENT '创建人',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_policy_code` (`policy_code`),
    INDEX `idx_template_id` (`template_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排程策略表';

-- ---------------------------------------------------
-- 排程执行表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `schedule_executions`;
CREATE TABLE `schedule_executions` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `policy_id` BIGINT NOT NULL COMMENT '策略ID',
    `execution_date` DATE NOT NULL COMMENT '执行日期',
    `assigned_inspectors` JSON COMMENT '分配的检查员',
    `session_id` BIGINT COMMENT '关联会话ID',
    `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态',
    `failure_reason` VARCHAR(500) COMMENT '失败原因',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    INDEX `idx_policy_id` (`policy_id`),
    INDEX `idx_execution_date` (`execution_date`),
    INDEX `idx_status` (`status`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排程执行表';

-- =====================================================
-- V4-3: 分析领域 (Analytics Domain)
-- =====================================================

-- ---------------------------------------------------
-- 分析快照表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `analytics_snapshots`;
CREATE TABLE `analytics_snapshots` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `snapshot_type` VARCHAR(50) NOT NULL COMMENT '快照类型',
    `snapshot_scope` VARCHAR(50) COMMENT '快照范围',
    `scope_id` BIGINT COMMENT '范围ID',
    `snapshot_date` DATE NOT NULL COMMENT '快照日期',
    `data_json` JSON COMMENT '数据JSON',
    `generated_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
    PRIMARY KEY (`id`),
    INDEX `idx_snapshot_type` (`snapshot_type`),
    INDEX `idx_snapshot_date` (`snapshot_date`),
    INDEX `idx_scope` (`snapshot_scope`, `scope_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分析快照表';

-- =====================================================
-- V4-4: 纠正行动领域 (Corrective Domain)
-- =====================================================

-- ---------------------------------------------------
-- 纠正行动表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `corrective_actions`;
CREATE TABLE `corrective_actions` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `action_code` VARCHAR(50) NOT NULL COMMENT '行动编码',
    `title` VARCHAR(200) NOT NULL COMMENT '标题',
    `description` TEXT COMMENT '描述',
    `source` VARCHAR(50) COMMENT '来源类型',
    `source_id` BIGINT COMMENT '来源ID',
    `severity` VARCHAR(20) COMMENT '严重程度',
    `category` VARCHAR(50) COMMENT '分类',
    `status` VARCHAR(20) DEFAULT 'OPEN' COMMENT '状态',
    `class_id` BIGINT COMMENT '班级ID',
    `assignee_id` BIGINT COMMENT '负责人ID',
    `deadline` DATETIME COMMENT '截止时间',
    `resolution_note` TEXT COMMENT '解决说明',
    `resolution_attachments` JSON COMMENT '解决附件',
    `resolved_at` DATETIME COMMENT '解决时间',
    `verifier_id` BIGINT COMMENT '验证人ID',
    `verification_result` VARCHAR(20) COMMENT '验证结果',
    `verification_comment` VARCHAR(500) COMMENT '验证意见',
    `verified_at` DATETIME COMMENT '验证时间',
    `escalation_level` INT DEFAULT 0 COMMENT '升级级别',
    `created_by` BIGINT COMMENT '创建人',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_action_code` (`action_code`),
    INDEX `idx_class_id` (`class_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_assignee_id` (`assignee_id`),
    INDEX `idx_severity` (`severity`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='纠正行动表';

-- ---------------------------------------------------
-- 自动规则表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `auto_action_rules`;
CREATE TABLE `auto_action_rules` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `rule_code` VARCHAR(50) NOT NULL COMMENT '规则编码',
    `rule_name` VARCHAR(100) NOT NULL COMMENT '规则名称',
    `trigger_type` VARCHAR(50) COMMENT '触发类型',
    `trigger_condition` JSON COMMENT '触发条件',
    `severity` VARCHAR(20) COMMENT '严重程度',
    `category` VARCHAR(50) COMMENT '分类',
    `deadline_hours` INT COMMENT '截止时限(小时)',
    `auto_assign` TINYINT(1) DEFAULT 0 COMMENT '自动分配',
    `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rule_code` (`rule_code`),
    INDEX `idx_enabled` (`enabled`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='自动规则表';

-- =====================================================
-- V4-5: 学生行为领域 (Behavior Domain)
-- =====================================================

-- ---------------------------------------------------
-- 学生行为记录表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `student_behavior_records`;
CREATE TABLE `student_behavior_records` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `student_id` BIGINT NOT NULL COMMENT '学生ID',
    `class_id` BIGINT COMMENT '班级ID',
    `behavior_type` VARCHAR(20) COMMENT '行为类型',
    `source` VARCHAR(50) COMMENT '来源',
    `source_id` BIGINT COMMENT '来源ID',
    `category` VARCHAR(50) COMMENT '分类',
    `title` VARCHAR(200) COMMENT '标题',
    `detail` TEXT COMMENT '详情',
    `deduction_amount` DECIMAL(10,2) COMMENT '扣分金额',
    `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态',
    `recorded_by` BIGINT COMMENT '记录人',
    `recorded_at` DATETIME COMMENT '记录时间',
    `notified_at` DATETIME COMMENT '通知时间',
    `acknowledged_at` DATETIME COMMENT '确认时间',
    `resolved_at` DATETIME COMMENT '解决时间',
    `resolution_note` VARCHAR(500) COMMENT '解决说明',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    INDEX `idx_student_id` (`student_id`),
    INDEX `idx_class_id` (`class_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_behavior_type` (`behavior_type`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生行为记录表';

-- ---------------------------------------------------
-- 学生行为预警表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `student_behavior_alerts`;
CREATE TABLE `student_behavior_alerts` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `student_id` BIGINT NOT NULL COMMENT '学生ID',
    `class_id` BIGINT COMMENT '班级ID',
    `alert_type` VARCHAR(20) COMMENT '预警类型',
    `alert_level` VARCHAR(20) COMMENT '预警级别',
    `title` VARCHAR(200) COMMENT '标题',
    `description` TEXT COMMENT '描述',
    `trigger_data` JSON COMMENT '触发数据',
    `is_read` TINYINT(1) DEFAULT 0 COMMENT '是否已读',
    `is_handled` TINYINT(1) DEFAULT 0 COMMENT '是否已处理',
    `handled_by` BIGINT COMMENT '处理人',
    `handled_at` DATETIME COMMENT '处理时间',
    `handle_note` VARCHAR(500) COMMENT '处理说明',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    INDEX `idx_student_id` (`student_id`),
    INDEX `idx_class_id` (`class_id`),
    INDEX `idx_alert_level` (`alert_level`),
    INDEX `idx_is_handled` (`is_handled`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生行为预警表';

-- =====================================================
-- V4-6: 权限领域扩展 (Access Domain)
-- =====================================================

-- ---------------------------------------------------
-- 角色数据权限表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `role_data_permissions`;
CREATE TABLE `role_data_permissions` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `module_code` VARCHAR(50) NOT NULL COMMENT '模块编码',
    `data_scope` INT NOT NULL COMMENT '数据范围',
    `custom_dept_ids` VARCHAR(1000) COMMENT '自定义部门ID列表',
    `custom_class_ids` VARCHAR(1000) COMMENT '自定义班级ID列表',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_module` (`role_id`, `module_code`),
    INDEX `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色数据权限表';

-- ---------------------------------------------------
-- 系统模块表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `system_modules`;
CREATE TABLE `system_modules` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `module_code` VARCHAR(50) NOT NULL COMMENT '模块编码',
    `module_name` VARCHAR(100) NOT NULL COMMENT '模块名称',
    `module_desc` VARCHAR(500) COMMENT '模块描述',
    `parent_code` VARCHAR(50) COMMENT '父模块编码',
    `icon` VARCHAR(100) COMMENT '图标',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `status` INT DEFAULT 1 COMMENT '状态:0禁用,1启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_module_code` (`module_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统模块表';

-- =====================================================
-- V4-7: 评级领域 (Rating Domain)
-- =====================================================

-- ---------------------------------------------------
-- 评级配置表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `rating_configs`;
CREATE TABLE `rating_configs` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `check_plan_id` BIGINT COMMENT '检查计划ID',
    `rating_name` VARCHAR(100) NOT NULL COMMENT '评级名称',
    `period_type` VARCHAR(20) COMMENT '周期类型',
    `division_method` VARCHAR(20) COMMENT '划分方式',
    `division_value` DECIMAL(10,2) COMMENT '划分值',
    `icon` VARCHAR(100) COMMENT '图标',
    `color` VARCHAR(20) COMMENT '颜色',
    `priority` INT DEFAULT 0 COMMENT '优先级',
    `description` VARCHAR(500) COMMENT '描述',
    `require_approval` TINYINT(1) DEFAULT 0 COMMENT '是否需要审批',
    `auto_publish` TINYINT(1) DEFAULT 0 COMMENT '是否自动发布',
    `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by` BIGINT COMMENT '创建人',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    INDEX `idx_check_plan_id` (`check_plan_id`),
    INDEX `idx_enabled` (`enabled`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级配置表';

-- ---------------------------------------------------
-- 评级结果表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `rating_results`;
CREATE TABLE `rating_results` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `rating_config_id` BIGINT NOT NULL COMMENT '评级配置ID',
    `check_plan_id` BIGINT COMMENT '检查计划ID',
    `class_id` BIGINT NOT NULL COMMENT '班级ID',
    `class_name` VARCHAR(100) COMMENT '班级名称',
    `period_type` VARCHAR(20) COMMENT '周期类型',
    `period_start` DATE COMMENT '周期开始',
    `period_end` DATE COMMENT '周期结束',
    `ranking` INT COMMENT '排名',
    `final_score` DECIMAL(10,2) COMMENT '最终得分',
    `awarded` TINYINT(1) DEFAULT 0 COMMENT '是否获奖',
    `status` VARCHAR(20) DEFAULT 'CALCULATED' COMMENT '状态',
    `calculated_at` DATETIME COMMENT '计算时间',
    `submitted_at` DATETIME COMMENT '提交时间',
    `approved_by` BIGINT COMMENT '审批人',
    `approved_at` DATETIME COMMENT '审批时间',
    `approval_comment` VARCHAR(500) COMMENT '审批意见',
    `published_by` BIGINT COMMENT '发布人',
    `published_at` DATETIME COMMENT '发布时间',
    `revoked_by` BIGINT COMMENT '撤销人',
    `revoked_at` DATETIME COMMENT '撤销时间',
    `reject_reason` VARCHAR(500) COMMENT '驳回原因',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    INDEX `idx_rating_config_id` (`rating_config_id`),
    INDEX `idx_class_id` (`class_id`),
    INDEX `idx_period` (`period_start`, `period_end`),
    INDEX `idx_status` (`status`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评级结果表';

-- =====================================================
-- V4-8: 空间管理 (Space Domain)
-- =====================================================

-- ---------------------------------------------------
-- 空间表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `space`;
CREATE TABLE `space` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `space_code` VARCHAR(50) COMMENT '空间编码',
    `space_name` VARCHAR(100) NOT NULL COMMENT '空间名称',
    `space_type` VARCHAR(20) COMMENT '空间类型',
    `room_type` VARCHAR(20) COMMENT '房间类型',
    `building_type` VARCHAR(20) COMMENT '建筑类型',
    `building_no` VARCHAR(20) COMMENT '建筑编号',
    `room_no` VARCHAR(20) COMMENT '房间号',
    `parent_id` BIGINT COMMENT '父空间ID',
    `path` VARCHAR(500) COMMENT '树路径',
    `level` INT DEFAULT 0 COMMENT '层级',
    `campus_id` BIGINT COMMENT '校区ID',
    `building_id` BIGINT COMMENT '建筑ID',
    `floor_number` INT COMMENT '楼层',
    `capacity` INT DEFAULT 0 COMMENT '容量',
    `current_occupancy` INT DEFAULT 0 COMMENT '当前入住数',
    `org_unit_id` BIGINT COMMENT '组织单元ID',
    `class_id` BIGINT COMMENT '班级ID',
    `responsible_user_id` BIGINT COMMENT '负责人ID',
    `gender_type` INT COMMENT '性别类型',
    `status` INT DEFAULT 1 COMMENT '状态:0禁用,1启用',
    `attributes` JSON COMMENT '扩展属性',
    `description` VARCHAR(500) COMMENT '描述',
    `created_by` BIGINT COMMENT '创建人',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT COMMENT '更新人',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    INDEX `idx_space_code` (`space_code`),
    INDEX `idx_space_type` (`space_type`),
    INDEX `idx_parent_id` (`parent_id`),
    INDEX `idx_building_id` (`building_id`),
    INDEX `idx_org_unit_id` (`org_unit_id`),
    INDEX `idx_class_id` (`class_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='空间表';

-- ---------------------------------------------------
-- 空间入住者表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `space_occupant`;
CREATE TABLE `space_occupant` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `space_id` BIGINT NOT NULL COMMENT '空间ID',
    `occupant_type` VARCHAR(20) COMMENT '入住者类型',
    `occupant_id` BIGINT NOT NULL COMMENT '入住者ID',
    `position_no` INT COMMENT '位置编号(床号)',
    `check_in_date` DATE COMMENT '入住日期',
    `check_out_date` DATE COMMENT '退出日期',
    `status` INT DEFAULT 1 COMMENT '状态',
    `remark` VARCHAR(500) COMMENT '备注',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_space_id` (`space_id`),
    INDEX `idx_occupant` (`occupant_type`, `occupant_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='空间入住者表';

-- ---------------------------------------------------
-- 空间班级分配表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `space_class_assignment`;
CREATE TABLE `space_class_assignment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `space_id` BIGINT NOT NULL COMMENT '空间ID',
    `class_id` BIGINT NOT NULL COMMENT '班级ID',
    `org_unit_id` BIGINT COMMENT '组织单元ID',
    `assigned_beds` INT DEFAULT 0 COMMENT '分配床位数',
    `priority` INT DEFAULT 0 COMMENT '优先级',
    `status` INT DEFAULT 1 COMMENT '状态',
    `remark` VARCHAR(500) COMMENT '备注',
    `assigned_by` BIGINT COMMENT '分配人',
    `assigned_at` DATETIME COMMENT '分配时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_space_class` (`space_id`, `class_id`),
    INDEX `idx_class_id` (`class_id`),
    INDEX `idx_org_unit_id` (`org_unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='空间班级分配表';

-- ---------------------------------------------------
-- 班级宿舍绑定表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `class_dormitory_bindings`;
CREATE TABLE `class_dormitory_bindings` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `class_id` BIGINT NOT NULL COMMENT '班级ID',
    `dormitory_id` BIGINT NOT NULL COMMENT '宿舍ID',
    `student_count` INT DEFAULT 0 COMMENT '学生人数',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_class_dormitory` (`class_id`, `dormitory_id`),
    INDEX `idx_dormitory_id` (`dormitory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级宿舍绑定表';

-- =====================================================
-- V4-9: 资产管理领域 (Asset Domain)
-- =====================================================

-- ---------------------------------------------------
-- 资产分类表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `asset_category`;
CREATE TABLE `asset_category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `parent_id` BIGINT COMMENT '父分类ID',
    `category_code` VARCHAR(50) NOT NULL COMMENT '分类编码',
    `category_name` VARCHAR(100) NOT NULL COMMENT '分类名称',
    `category_type` INT COMMENT '分类类型',
    `default_management_mode` INT COMMENT '默认管理模式',
    `depreciation_years` INT COMMENT '折旧年限',
    `unit` VARCHAR(20) COMMENT '单位',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `remark` VARCHAR(500) COMMENT '备注',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_category_code` (`category_code`),
    INDEX `idx_parent_id` (`parent_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产分类表';

-- ---------------------------------------------------
-- 资产表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `asset`;
CREATE TABLE `asset` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `asset_code` VARCHAR(50) NOT NULL COMMENT '资产编码',
    `asset_name` VARCHAR(200) NOT NULL COMMENT '资产名称',
    `category_id` BIGINT COMMENT '分类ID',
    `brand` VARCHAR(100) COMMENT '品牌',
    `model` VARCHAR(100) COMMENT '型号',
    `unit` VARCHAR(20) COMMENT '单位',
    `quantity` INT DEFAULT 1 COMMENT '数量',
    `original_value` DECIMAL(12,2) COMMENT '原值',
    `net_value` DECIMAL(12,2) COMMENT '净值',
    `purchase_date` DATE COMMENT '购入日期',
    `warranty_date` DATE COMMENT '保修日期',
    `supplier` VARCHAR(200) COMMENT '供应商',
    `status` INT DEFAULT 1 COMMENT '状态:1正常,2维修,3报废,4借出',
    `management_mode` INT DEFAULT 1 COMMENT '管理模式:1固定资产,2耗材',
    `location_type` VARCHAR(20) COMMENT '位置类型',
    `location_id` BIGINT COMMENT '位置ID',
    `location_name` VARCHAR(200) COMMENT '位置名称',
    `responsible_user_id` BIGINT COMMENT '责任人ID',
    `responsible_user_name` VARCHAR(50) COMMENT '责任人姓名',
    `remark` VARCHAR(500) COMMENT '备注',
    `category_type` INT COMMENT '折旧分类类型',
    `depreciation_method` INT COMMENT '折旧方法',
    `residual_value` DECIMAL(12,2) COMMENT '残值',
    `accumulated_depreciation` DECIMAL(12,2) DEFAULT 0 COMMENT '累计折旧',
    `useful_life` INT COMMENT '使用年限(月)',
    `stock_warning_threshold` INT COMMENT '库存预警阈值',
    `created_by` BIGINT COMMENT '创建人',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_asset_code` (`asset_code`),
    INDEX `idx_category_id` (`category_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_location` (`location_type`, `location_id`),
    INDEX `idx_responsible_user_id` (`responsible_user_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产表';

-- ---------------------------------------------------
-- 资产历史表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `asset_history`;
CREATE TABLE `asset_history` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `asset_id` BIGINT NOT NULL COMMENT '资产ID',
    `change_type` VARCHAR(50) NOT NULL COMMENT '变更类型',
    `change_content` TEXT COMMENT '变更内容',
    `old_location_type` VARCHAR(20) COMMENT '原位置类型',
    `old_location_id` BIGINT COMMENT '原位置ID',
    `old_location_name` VARCHAR(200) COMMENT '原位置名称',
    `new_location_type` VARCHAR(20) COMMENT '新位置类型',
    `new_location_id` BIGINT COMMENT '新位置ID',
    `new_location_name` VARCHAR(200) COMMENT '新位置名称',
    `operator_id` BIGINT COMMENT '操作人ID',
    `operator_name` VARCHAR(50) COMMENT '操作人姓名',
    `operate_time` DATETIME COMMENT '操作时间',
    `remark` VARCHAR(500) COMMENT '备注',
    PRIMARY KEY (`id`),
    INDEX `idx_asset_id` (`asset_id`),
    INDEX `idx_change_type` (`change_type`),
    INDEX `idx_operate_time` (`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产历史表';

-- ---------------------------------------------------
-- 资产维修表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `asset_maintenance`;
CREATE TABLE `asset_maintenance` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `asset_id` BIGINT NOT NULL COMMENT '资产ID',
    `maintenance_type` INT COMMENT '维修类型',
    `fault_desc` TEXT COMMENT '故障描述',
    `start_date` DATE COMMENT '开始日期',
    `end_date` DATE COMMENT '结束日期',
    `cost` DECIMAL(10,2) COMMENT '维修费用',
    `maintainer` VARCHAR(100) COMMENT '维修人',
    `result` VARCHAR(500) COMMENT '维修结果',
    `status` INT DEFAULT 0 COMMENT '状态',
    `created_by` BIGINT COMMENT '创建人',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_asset_id` (`asset_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产维修表';

-- ---------------------------------------------------
-- 资产盘点表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `asset_inventory`;
CREATE TABLE `asset_inventory` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `inventory_code` VARCHAR(50) NOT NULL COMMENT '盘点编码',
    `inventory_name` VARCHAR(100) NOT NULL COMMENT '盘点名称',
    `scope_type` VARCHAR(20) COMMENT '范围类型',
    `scope_value` VARCHAR(500) COMMENT '范围值',
    `start_date` DATE COMMENT '开始日期',
    `end_date` DATE COMMENT '结束日期',
    `status` INT DEFAULT 0 COMMENT '状态',
    `total_count` INT DEFAULT 0 COMMENT '总数',
    `checked_count` INT DEFAULT 0 COMMENT '已盘点数',
    `profit_count` INT DEFAULT 0 COMMENT '盘盈数',
    `loss_count` INT DEFAULT 0 COMMENT '盘亏数',
    `created_by` BIGINT COMMENT '创建人',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_inventory_code` (`inventory_code`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产盘点表';

-- ---------------------------------------------------
-- 资产盘点明细表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `asset_inventory_detail`;
CREATE TABLE `asset_inventory_detail` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `inventory_id` BIGINT NOT NULL COMMENT '盘点ID',
    `asset_id` BIGINT NOT NULL COMMENT '资产ID',
    `expected_quantity` INT COMMENT '预期数量',
    `actual_quantity` INT COMMENT '实际数量',
    `difference` INT COMMENT '差异',
    `result_type` INT COMMENT '结果类型:1正常,2盘盈,3盘亏',
    `check_time` DATETIME COMMENT '盘点时间',
    `checker_id` BIGINT COMMENT '盘点人ID',
    `checker_name` VARCHAR(50) COMMENT '盘点人姓名',
    `remark` VARCHAR(500) COMMENT '备注',
    PRIMARY KEY (`id`),
    INDEX `idx_inventory_id` (`inventory_id`),
    INDEX `idx_asset_id` (`asset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产盘点明细表';

-- ---------------------------------------------------
-- 资产折旧表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `asset_depreciation`;
CREATE TABLE `asset_depreciation` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `asset_id` BIGINT NOT NULL COMMENT '资产ID',
    `asset_code` VARCHAR(50) COMMENT '资产编码',
    `depreciation_period` VARCHAR(6) COMMENT '折旧期间(YYYYMM)',
    `beginning_value` DECIMAL(12,2) COMMENT '期初原值',
    `beginning_accumulated_depreciation` DECIMAL(12,2) COMMENT '期初累计折旧',
    `beginning_net_value` DECIMAL(12,2) COMMENT '期初净值',
    `depreciation_amount` DECIMAL(12,2) COMMENT '本期折旧额',
    `ending_accumulated_depreciation` DECIMAL(12,2) COMMENT '期末累计折旧',
    `ending_net_value` DECIMAL(12,2) COMMENT '期末净值',
    `used_months` INT COMMENT '已使用月数',
    `remaining_months` INT COMMENT '剩余月数',
    `depreciation_method` INT COMMENT '折旧方法',
    `depreciation_date` DATE COMMENT '折旧日期',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `remark` VARCHAR(500) COMMENT '备注',
    PRIMARY KEY (`id`),
    INDEX `idx_asset_id` (`asset_id`),
    INDEX `idx_period` (`depreciation_period`),
    INDEX `idx_depreciation_date` (`depreciation_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产折旧表';

-- ---------------------------------------------------
-- 资产借用表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `asset_borrow`;
CREATE TABLE `asset_borrow` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `borrow_no` VARCHAR(50) NOT NULL COMMENT '借用单号',
    `borrow_type` INT COMMENT '借用类型',
    `asset_id` BIGINT NOT NULL COMMENT '资产ID',
    `asset_code` VARCHAR(50) COMMENT '资产编码',
    `asset_name` VARCHAR(200) COMMENT '资产名称',
    `quantity` INT DEFAULT 1 COMMENT '借用数量',
    `borrower_id` BIGINT COMMENT '借用人ID',
    `borrower_name` VARCHAR(50) COMMENT '借用人姓名',
    `borrower_dept` VARCHAR(100) COMMENT '借用人部门',
    `borrower_phone` VARCHAR(20) COMMENT '借用人电话',
    `borrow_date` DATETIME COMMENT '借用日期',
    `expected_return_date` DATE COMMENT '预计归还日期',
    `actual_return_date` DATETIME COMMENT '实际归还日期',
    `return_condition` VARCHAR(100) COMMENT '归还状况',
    `return_remark` VARCHAR(500) COMMENT '归还备注',
    `returner_id` BIGINT COMMENT '归还人ID',
    `returner_name` VARCHAR(50) COMMENT '归还人姓名',
    `purpose` VARCHAR(500) COMMENT '借用用途',
    `status` INT DEFAULT 0 COMMENT '状态',
    `operator_id` BIGINT COMMENT '操作人ID',
    `operator_name` VARCHAR(50) COMMENT '操作人姓名',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_borrow_no` (`borrow_no`),
    INDEX `idx_asset_id` (`asset_id`),
    INDEX `idx_borrower_id` (`borrower_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产借用表';

-- ---------------------------------------------------
-- 资产审批表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `asset_approval`;
CREATE TABLE `asset_approval` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `approval_no` VARCHAR(50) COMMENT '审批单号',
    `approval_type` INT COMMENT '审批类型',
    `business_id` BIGINT COMMENT '业务ID',
    `asset_id` BIGINT COMMENT '资产ID',
    `asset_name` VARCHAR(200) COMMENT '资产名称',
    `applicant_id` BIGINT COMMENT '申请人ID',
    `applicant_name` VARCHAR(50) COMMENT '申请人姓名',
    `applicant_dept` VARCHAR(100) COMMENT '申请人部门',
    `approver_id` BIGINT COMMENT '审批人ID',
    `approver_name` VARCHAR(50) COMMENT '审批人姓名',
    `status` INT DEFAULT 0 COMMENT '状态',
    `apply_reason` VARCHAR(500) COMMENT '申请原因',
    `apply_quantity` INT COMMENT '申请数量',
    `apply_amount` DECIMAL(12,2) COMMENT '申请金额',
    `approval_remark` VARCHAR(500) COMMENT '审批备注',
    `apply_time` DATETIME COMMENT '申请时间',
    `approval_time` DATETIME COMMENT '审批时间',
    `expire_time` DATETIME COMMENT '过期时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_approval_no` (`approval_no`),
    INDEX `idx_asset_id` (`asset_id`),
    INDEX `idx_applicant_id` (`applicant_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产审批表';

-- ---------------------------------------------------
-- 资产预警表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `asset_alert`;
CREATE TABLE `asset_alert` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `alert_type` INT NOT NULL COMMENT '预警类型',
    `asset_id` BIGINT COMMENT '资产ID',
    `asset_code` VARCHAR(50) COMMENT '资产编码',
    `asset_name` VARCHAR(200) COMMENT '资产名称',
    `borrow_id` BIGINT COMMENT '借用ID',
    `alert_content` VARCHAR(500) COMMENT '预警内容',
    `alert_level` INT DEFAULT 1 COMMENT '预警级别',
    `is_read` TINYINT(1) DEFAULT 0 COMMENT '是否已读',
    `is_handled` TINYINT(1) DEFAULT 0 COMMENT '是否已处理',
    `handle_remark` VARCHAR(500) COMMENT '处理备注',
    `handle_time` DATETIME COMMENT '处理时间',
    `handler_id` BIGINT COMMENT '处理人ID',
    `handler_name` VARCHAR(50) COMMENT '处理人姓名',
    `notify_user_id` BIGINT COMMENT '通知人ID',
    `notify_user_name` VARCHAR(50) COMMENT '通知人姓名',
    `alert_time` DATETIME COMMENT '预警时间',
    `expire_time` DATETIME COMMENT '过期时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_asset_id` (`asset_id`),
    INDEX `idx_alert_type` (`alert_type`),
    INDEX `idx_is_handled` (`is_handled`),
    INDEX `idx_notify_user_id` (`notify_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产预警表';

-- =====================================================
-- V4-10: 系统消息
-- =====================================================

-- ---------------------------------------------------
-- 系统消息表
-- ---------------------------------------------------
DROP TABLE IF EXISTS `system_messages`;
CREATE TABLE `system_messages` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `message_type` VARCHAR(20) COMMENT '消息类型',
    `title` VARCHAR(200) NOT NULL COMMENT '标题',
    `content` TEXT COMMENT '内容',
    `sender_id` BIGINT COMMENT '发送人ID',
    `sender_name` VARCHAR(50) COMMENT '发送人姓名',
    `receiver_id` BIGINT COMMENT '接收人ID',
    `receiver_name` VARCHAR(50) COMMENT '接收人姓名',
    `is_read` INT DEFAULT 0 COMMENT '是否已读:0未读,1已读',
    `read_time` DATETIME COMMENT '阅读时间',
    `business_type` VARCHAR(50) COMMENT '业务类型',
    `business_id` BIGINT COMMENT '业务ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    INDEX `idx_receiver_id` (`receiver_id`),
    INDEX `idx_is_read` (`is_read`),
    INDEX `idx_message_type` (`message_type`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统消息表';

-- =====================================================
-- V4 索引补充
-- =====================================================

-- 检查会话复合索引
CREATE INDEX `idx_session_date_status` ON `inspection_sessions` (`inspection_date`, `status`);

-- 班级检查记录复合索引
CREATE INDEX `idx_cir_session_class` ON `class_inspection_records` (`session_id`, `class_id`);

-- 学生行为记录复合索引
CREATE INDEX `idx_behavior_student_type` ON `student_behavior_records` (`student_id`, `behavior_type`);

-- 纠正行动复合索引
CREATE INDEX `idx_corrective_class_status` ON `corrective_actions` (`class_id`, `status`);

-- 排程执行复合索引
CREATE INDEX `idx_schedule_policy_date` ON `schedule_executions` (`policy_id`, `execution_date`);

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================
-- 结束 V4 DDD 表结构
-- =====================================================
