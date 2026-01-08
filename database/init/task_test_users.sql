-- =====================================================
-- 任务管理模块 - 测试账号初始化
-- 创建日期: 2025-12-27
-- 说明: 创建用于测试任务工作流的测试账号
-- =====================================================

SET NAMES utf8mb4;

-- =====================================================
-- 1. 创建测试用户
-- 密码统一为: admin123 (BCrypt加密后，与admin账号相同)
-- 注意：初始创建后需要运行 fix_test_passwords.sql 修正密码
-- =====================================================

-- 学工处领导 (任务分配者)
INSERT INTO `users` (
    `id`, `username`, `password`, `real_name`, `employee_no`,
    `department_id`, `user_type`, `status`, `gender`
) VALUES (
    1001,
    'xuegong_leader',
    '$2a$10$N.zmdr9k7uOILNIjp7b8keTfVcvXCPZcMz5s7VnhDOr.UzJc8Nmx6',  -- test123
    '学工处张主任',
    'TASK_EMP001',
    1,  -- 学校总部
    1,  -- 教职工
    1,  -- 启用
    1   -- 男
);

-- 系部主任 (领导1 - 初审)
INSERT INTO `users` (
    `id`, `username`, `password`, `real_name`, `employee_no`,
    `department_id`, `user_type`, `status`, `gender`
) VALUES (
    2001,
    'dept_director',
    '$2a$10$N.zmdr9k7uOILNIjp7b8keTfVcvXCPZcMz5s7VnhDOr.UzJc8Nmx6',  -- test123
    '信息工程系刘主任',
    'TASK_EMP002',
    100,  -- 信息工程学院
    1,
    1,
    1
);

-- 校领导 (领导2 - 终审)
INSERT INTO `users` (
    `id`, `username`, `password`, `real_name`, `employee_no`,
    `department_id`, `user_type`, `status`, `gender`
) VALUES (
    3001,
    'school_leader',
    '$2a$10$N.zmdr9k7uOILNIjp7b8keTfVcvXCPZcMz5s7VnhDOr.UzJc8Nmx6',  -- test123
    '李副校长',
    'TASK_EMP003',
    1,  -- 学校总部
    1,
    1,
    1
);

-- 班主任1 (计算机2023级1班)
INSERT INTO `users` (
    `id`, `username`, `password`, `real_name`, `employee_no`,
    `department_id`, `managed_class_id`, `user_type`, `status`, `gender`
) VALUES (
    1101,
    'teacher_wang',
    '$2a$10$N.zmdr9k7uOILNIjp7b8keTfVcvXCPZcMz5s7VnhDOr.UzJc8Nmx6',  -- test123
    '王老师',
    'TASK_T001',
    100,    -- 信息工程学院
    10011,  -- 计算机2023级1班
    1,
    1,
    2  -- 女
);

-- 班主任2 (计算机2023级2班)
INSERT INTO `users` (
    `id`, `username`, `password`, `real_name`, `employee_no`,
    `department_id`, `managed_class_id`, `user_type`, `status`, `gender`
) VALUES (
    1102,
    'teacher_li',
    '$2a$10$N.zmdr9k7uOILNIjp7b8keTfVcvXCPZcMz5s7VnhDOr.UzJc8Nmx6',  -- test123
    '李老师',
    'TASK_T002',
    100,
    10012,  -- 计算机2023级2班
    1,
    1,
    1  -- 男
);

-- 班主任3 (计算机2023级3班)
INSERT INTO `users` (
    `id`, `username`, `password`, `real_name`, `employee_no`,
    `department_id`, `managed_class_id`, `user_type`, `status`, `gender`
) VALUES (
    1103,
    'teacher_zhang',
    '$2a$10$N.zmdr9k7uOILNIjp7b8keTfVcvXCPZcMz5s7VnhDOr.UzJc8Nmx6',  -- test123
    '张老师',
    'TASK_T003',
    100,
    10013,  -- 计算机2023级3班
    1,
    1,
    2  -- 女
);

-- 班主任4 (计算机2023级4班)
INSERT INTO `users` (
    `id`, `username`, `password`, `real_name`, `employee_no`,
    `department_id`, `managed_class_id`, `user_type`, `status`, `gender`
) VALUES (
    1104,
    'teacher_zhao',
    '$2a$10$N.zmdr9k7uOILNIjp7b8keTfVcvXCPZcMz5s7VnhDOr.UzJc8Nmx6',  -- test123
    '赵老师',
    'TASK_T004',
    100,
    10014,  -- 计算机2023级4班
    1,
    1,
    1  -- 男
);

-- =====================================================
-- 2. 分配角色
-- =====================================================

-- 学工处领导 -> 学工处角色
INSERT INTO `user_roles` (`user_id`, `role_id`) VALUES (1001, 4);  -- 假设4是学工处角色ID

-- 系部主任 -> 系主任角色
INSERT INTO `user_roles` (`user_id`, `role_id`) VALUES (2001, 3);  -- 假设3是系主任角色ID

-- 校领导 -> 校领导角色
INSERT INTO `user_roles` (`user_id`, `role_id`) VALUES (3001, 2);  -- 假设2是校领导角色ID

-- 班主任 -> 班主任角色
INSERT INTO `user_roles` (`user_id`, `role_id`) VALUES (1101, 5);  -- 假设5是班主任角色ID
INSERT INTO `user_roles` (`user_id`, `role_id`) VALUES (1102, 5);
INSERT INTO `user_roles` (`user_id`, `role_id`) VALUES (1103, 5);
INSERT INTO `user_roles` (`user_id`, `role_id`) VALUES (1104, 5);

-- =====================================================
-- 3. 测试账号清单
-- =====================================================

/*
测试账号清单:
┌────────────────┬──────────────────┬──────────┬────────────────┐
│ 角色           │ 用户名           │ 密码     │ 姓名           │
├────────────────┼──────────────────┼──────────┼────────────────┤
│ 学工处领导     │ xuegong_leader   │ admin123 │ 学工处张主任   │
│ 系部主任(领导1)│ dept_director    │ admin123 │ 信息工程系刘主任│
│ 校领导(领导2)  │ school_leader    │ admin123 │ 李副校长       │
│ 班主任1        │ teacher_wang     │ admin123 │ 王老师         │
│ 班主任2        │ teacher_li       │ admin123 │ 李老师         │
│ 班主任3        │ teacher_zhang    │ admin123 │ 张老师         │
│ 班主任4        │ teacher_zhao     │ admin123 │ 赵老师         │
└────────────────┴──────────────────┴──────────┴────────────────┘
*/

-- =====================================================
-- 说明:
-- 1. 所有测试账号密码统一为: admin123
-- 2. 如果角色ID不匹配,请根据实际roles表中的ID调整user_roles部分
-- 3. 这些账号仅用于测试任务管理工作流功能
-- 4. 创建后需要运行 fix_test_passwords.sql 修正密码哈希
-- =====================================================
