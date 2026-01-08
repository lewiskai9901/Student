-- ============================================
-- 学生管理系统 - 核心初始化数据
-- ============================================

SET NAMES utf8mb4;

-- ----------------------------
-- 初始化角色
-- ----------------------------
INSERT INTO `roles` (`id`, `role_name`, `role_code`, `description`, `status`, `sort_order`) VALUES
(1, '超级管理员', 'SUPER_ADMIN', '系统超级管理员，拥有所有权限', 1, 1),
(2, '系统管理员', 'ADMIN', '系统管理员', 1, 2),
(3, '教师', 'TEACHER', '普通教师', 1, 3),
(4, '班主任', 'HEAD_TEACHER', '班主任', 1, 4),
(5, '学生', 'STUDENT', '学生', 1, 5),
(6, '检查员', 'INSPECTOR', '量化检查员', 1, 6)
ON DUPLICATE KEY UPDATE `role_name` = VALUES(`role_name`);

-- ----------------------------
-- 初始化管理员用户 (密码: admin123)
-- BCrypt: $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EHsM
-- ----------------------------
INSERT INTO `users` (`id`, `username`, `password`, `real_name`, `user_type`, `status`) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKh1O2eq', '系统管理员', 1, 1)
ON DUPLICATE KEY UPDATE `real_name` = VALUES(`real_name`);

-- ----------------------------
-- 管理员角色关联
-- ----------------------------
INSERT INTO `user_roles` (`id`, `user_id`, `role_id`) VALUES
(1, 1, 1)
ON DUPLICATE KEY UPDATE `role_id` = VALUES(`role_id`);

-- ----------------------------
-- 初始化部门
-- ----------------------------
INSERT INTO `departments` (`id`, `department_name`, `department_code`, `parent_id`, `sort_order`) VALUES
(1, '学校', 'SCHOOL', 0, 1),
(2, '信息技术系', 'IT', 1, 1),
(3, '机电工程系', 'ME', 1, 2),
(4, '经济管理系', 'EM', 1, 3)
ON DUPLICATE KEY UPDATE `department_name` = VALUES(`department_name`);

-- ----------------------------
-- 初始化系统配置
-- ----------------------------
INSERT INTO `system_configs` (`id`, `config_key`, `config_value`, `config_type`, `config_group`, `config_label`, `config_desc`, `is_system`, `sort_order`, `status`) VALUES
(1, 'system.name', '学生管理系统', 'string', 'system', '系统名称', '系统显示名称', 1, 1, 1),
(2, 'system.logo', '/logo.png', 'string', 'system', '系统Logo', '系统Logo图片路径', 1, 2, 1),
(3, 'system.copyright', '© 2024 学生管理系统', 'string', 'system', '版权信息', '页面底部版权信息', 1, 3, 1),
(4, 'upload.max_size', '10485760', 'number', 'system', '上传大小限制', '文件上传最大大小(字节)', 1, 4, 1),
(5, 'upload.allowed_types', 'jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx', 'string', 'system', '允许上传类型', '允许上传的文件类型', 1, 5, 1),
(6, 'check.base_score', '100', 'number', 'business', '基础分数', '量化检查基础分数', 1, 10, 1),
(7, 'check.appeal_days', '3', 'number', 'business', '申诉天数', '允许申诉的天数', 1, 11, 1)
ON DUPLICATE KEY UPDATE `config_value` = VALUES(`config_value`);

-- ----------------------------
-- 初始化权限
-- ----------------------------
INSERT INTO `permissions` (`id`, `permission_name`, `permission_code`, `parent_id`, `type`, `path`, `sort_order`, `status`) VALUES
-- 一级菜单
(1, '首页', 'dashboard', 0, 1, '/dashboard', 1, 1),
(2, '用户管理', 'user', 0, 1, '/user', 2, 1),
(3, '系统管理', 'system', 0, 1, '/system', 3, 1),
(4, '组织管理', 'organization', 0, 1, '/organization', 4, 1),
(5, '量化管理', 'quantification', 0, 1, '/quantification', 5, 1),
-- 用户管理子菜单
(21, '用户列表', 'user:list', 2, 1, '/user/list', 1, 1),
(22, '角色管理', 'user:role', 2, 1, '/user/role', 2, 1),
(23, '权限管理', 'user:permission', 2, 1, '/user/permission', 3, 1),
-- 系统管理子菜单
(31, '系统配置', 'system:config', 3, 1, '/system/config', 1, 1),
(32, '操作日志', 'system:log', 3, 1, '/system/log', 2, 1),
-- 组织管理子菜单
(41, '部门管理', 'organization:department', 4, 1, '/organization/department', 1, 1),
(42, '班级管理', 'organization:class', 4, 1, '/organization/class', 2, 1),
(43, '学生管理', 'organization:student', 4, 1, '/organization/student', 3, 1)
ON DUPLICATE KEY UPDATE `permission_name` = VALUES(`permission_name`);

-- ----------------------------
-- 超级管理员拥有所有权限
-- ----------------------------
INSERT INTO `role_permissions` (`id`, `role_id`, `permission_id`)
SELECT (@row := @row + 1) as id, 1 as role_id, id as permission_id
FROM `permissions`, (SELECT @row := 100) r
ON DUPLICATE KEY UPDATE `permission_id` = VALUES(`permission_id`);
