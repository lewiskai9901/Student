-- =====================================================
-- 数据库修复迁移 - 添加外键约束
-- 版本: 100
-- 创建日期: 2025-12-31
-- 说明: 为关键表添加外键约束，增强数据完整性
-- =====================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================
-- 第一部分: 任务管理模块外键约束
-- =====================================================

-- 任务表添加外键约束
-- 注意: 先检查约束是否存在再添加
DELIMITER //
CREATE PROCEDURE add_fk_if_not_exists()
BEGIN
    -- 检查并添加 tasks.assigner_id -> users.id 外键
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
        AND TABLE_NAME = 'tasks'
        AND CONSTRAINT_NAME = 'fk_tasks_assigner'
    ) THEN
        ALTER TABLE `tasks`
            ADD CONSTRAINT `fk_tasks_assigner`
            FOREIGN KEY (`assigner_id`) REFERENCES `users` (`id`)
            ON DELETE RESTRICT ON UPDATE CASCADE;
    END IF;

    -- 检查并添加 tasks.assignee_id -> users.id 外键
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
        AND TABLE_NAME = 'tasks'
        AND CONSTRAINT_NAME = 'fk_tasks_assignee'
    ) THEN
        ALTER TABLE `tasks`
            ADD CONSTRAINT `fk_tasks_assignee`
            FOREIGN KEY (`assignee_id`) REFERENCES `users` (`id`)
            ON DELETE SET NULL ON UPDATE CASCADE;
    END IF;

    -- 检查并添加 tasks.department_id -> departments.id 外键
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
        AND TABLE_NAME = 'tasks'
        AND CONSTRAINT_NAME = 'fk_tasks_department'
    ) THEN
        ALTER TABLE `tasks`
            ADD CONSTRAINT `fk_tasks_department`
            FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`)
            ON DELETE SET NULL ON UPDATE CASCADE;
    END IF;

    -- 检查并添加 task_assignees.task_id -> tasks.id 外键
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
        AND TABLE_NAME = 'task_assignees'
        AND CONSTRAINT_NAME = 'fk_task_assignees_task'
    ) THEN
        ALTER TABLE `task_assignees`
            ADD CONSTRAINT `fk_task_assignees_task`
            FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`)
            ON DELETE CASCADE ON UPDATE CASCADE;
    END IF;

    -- 检查并添加 task_assignees.assignee_id -> users.id 外键
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
        AND TABLE_NAME = 'task_assignees'
        AND CONSTRAINT_NAME = 'fk_task_assignees_user'
    ) THEN
        ALTER TABLE `task_assignees`
            ADD CONSTRAINT `fk_task_assignees_user`
            FOREIGN KEY (`assignee_id`) REFERENCES `users` (`id`)
            ON DELETE CASCADE ON UPDATE CASCADE;
    END IF;

    -- 检查并添加 task_submissions.task_id -> tasks.id 外键
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
        AND TABLE_NAME = 'task_submissions'
        AND CONSTRAINT_NAME = 'fk_task_submissions_task'
    ) THEN
        ALTER TABLE `task_submissions`
            ADD CONSTRAINT `fk_task_submissions_task`
            FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`)
            ON DELETE CASCADE ON UPDATE CASCADE;
    END IF;

    -- 检查并添加 task_approval_records.task_id -> tasks.id 外键
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
        AND TABLE_NAME = 'task_approval_records'
        AND CONSTRAINT_NAME = 'fk_task_approval_records_task'
    ) THEN
        ALTER TABLE `task_approval_records`
            ADD CONSTRAINT `fk_task_approval_records_task`
            FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`)
            ON DELETE CASCADE ON UPDATE CASCADE;
    END IF;

    -- 检查并添加 task_logs.task_id -> tasks.id 外键
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
        AND TABLE_NAME = 'task_logs'
        AND CONSTRAINT_NAME = 'fk_task_logs_task'
    ) THEN
        ALTER TABLE `task_logs`
            ADD CONSTRAINT `fk_task_logs_task`
            FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`)
            ON DELETE CASCADE ON UPDATE CASCADE;
    END IF;

    -- 检查并添加 system_messages.receiver_id -> users.id 外键
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
        AND TABLE_NAME = 'system_messages'
        AND CONSTRAINT_NAME = 'fk_system_messages_receiver'
    ) THEN
        ALTER TABLE `system_messages`
            ADD CONSTRAINT `fk_system_messages_receiver`
            FOREIGN KEY (`receiver_id`) REFERENCES `users` (`id`)
            ON DELETE CASCADE ON UPDATE CASCADE;
    END IF;
END //
DELIMITER ;

-- 执行存储过程
CALL add_fk_if_not_exists();

-- 删除临时存储过程
DROP PROCEDURE IF EXISTS add_fk_if_not_exists;

-- =====================================================
-- 第二部分: 学生管理模块外键约束
-- =====================================================

DELIMITER //
CREATE PROCEDURE add_student_fks()
BEGIN
    -- students.class_id -> classes.id
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
        AND TABLE_NAME = 'students'
        AND CONSTRAINT_NAME = 'fk_students_class'
    ) THEN
        ALTER TABLE `students`
            ADD CONSTRAINT `fk_students_class`
            FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`)
            ON DELETE SET NULL ON UPDATE CASCADE;
    END IF;

    -- classes.department_id -> departments.id
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
        AND TABLE_NAME = 'classes'
        AND CONSTRAINT_NAME = 'fk_classes_department'
    ) THEN
        ALTER TABLE `classes`
            ADD CONSTRAINT `fk_classes_department`
            FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`)
            ON DELETE SET NULL ON UPDATE CASCADE;
    END IF;

    -- classes.teacher_id -> users.id
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
        AND TABLE_NAME = 'classes'
        AND CONSTRAINT_NAME = 'fk_classes_teacher'
    ) THEN
        ALTER TABLE `classes`
            ADD CONSTRAINT `fk_classes_teacher`
            FOREIGN KEY (`teacher_id`) REFERENCES `users` (`id`)
            ON DELETE SET NULL ON UPDATE CASCADE;
    END IF;

    -- users.department_id -> departments.id
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
        AND TABLE_NAME = 'users'
        AND CONSTRAINT_NAME = 'fk_users_department'
    ) THEN
        ALTER TABLE `users`
            ADD CONSTRAINT `fk_users_department`
            FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`)
            ON DELETE SET NULL ON UPDATE CASCADE;
    END IF;
END //
DELIMITER ;

CALL add_student_fks();
DROP PROCEDURE IF EXISTS add_student_fks;

-- =====================================================
-- 第三部分: 量化检查模块外键约束
-- =====================================================

DELIMITER //
CREATE PROCEDURE add_check_fks()
BEGIN
    -- daily_check_details.check_id -> daily_checks.id
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
        AND TABLE_NAME = 'daily_check_details'
        AND CONSTRAINT_NAME = 'fk_check_details_check'
    ) THEN
        ALTER TABLE `daily_check_details`
            ADD CONSTRAINT `fk_check_details_check`
            FOREIGN KEY (`check_id`) REFERENCES `daily_checks` (`id`)
            ON DELETE CASCADE ON UPDATE CASCADE;
    END IF;

    -- daily_checks.plan_id -> check_plans.id
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
        AND TABLE_NAME = 'daily_checks'
        AND CONSTRAINT_NAME = 'fk_daily_checks_plan'
    ) THEN
        ALTER TABLE `daily_checks`
            ADD CONSTRAINT `fk_daily_checks_plan`
            FOREIGN KEY (`plan_id`) REFERENCES `check_plans` (`id`)
            ON DELETE SET NULL ON UPDATE CASCADE;
    END IF;
END //
DELIMITER ;

CALL add_check_fks();
DROP PROCEDURE IF EXISTS add_check_fks;

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================
-- 说明:
-- 1. 此迁移使用存储过程检查约束是否存在，避免重复添加
-- 2. 使用 ON DELETE CASCADE 确保父记录删除时子记录也被删除
-- 3. 使用 ON DELETE SET NULL 对于可选的关联关系
-- 4. 运行前请备份数据库
-- =====================================================
