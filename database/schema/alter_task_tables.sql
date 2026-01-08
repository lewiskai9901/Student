-- ============================================
-- 任务管理系统重构 - 表结构修改
-- 功能: 将任务从1对1模式改为1对多模式，支持多人任务
-- 说明: 表结构已经在创建时就是目标状态，此脚本添加索引
-- ============================================

-- 1. 表结构说明（已在建表时完成）
-- tasks 表已移除: assignee_id, assignee_name, department_id, department_name, status
-- task_assignees 表已添加: department_id, department_name, approval_config, current_approval_level

-- 2. 添加索引以优化查询（如果不存在则创建）
-- 使用存储过程实现幂等性

DELIMITER $$

DROP PROCEDURE IF EXISTS CreateIndexIfNotExists$$
CREATE PROCEDURE CreateIndexIfNotExists(
    IN p_table_name VARCHAR(64),
    IN p_index_name VARCHAR(64),
    IN p_column_name VARCHAR(64)
)
BEGIN
    DECLARE index_exists INT DEFAULT 0;

    -- 检查索引是否存在
    SELECT COUNT(*) INTO index_exists
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = p_table_name
      AND index_name = p_index_name;

    -- 如果索引不存在，则创建
    IF index_exists = 0 THEN
        SET @sql = CONCAT('CREATE INDEX ', p_index_name, ' ON ', p_table_name, ' (', p_column_name, ')');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

-- 添加部门索引
CALL CreateIndexIfNotExists('task_assignees', 'idx_department', 'department_id');

-- 添加状态索引
CALL CreateIndexIfNotExists('task_assignees', 'idx_status', 'status');

-- 添加当前审批级别索引
CALL CreateIndexIfNotExists('task_assignees', 'idx_current_level', 'current_approval_level');

-- 清理存储过程
DROP PROCEDURE IF EXISTS CreateIndexIfNotExists;

-- ============================================
-- 警告: 以下操作将清空所有任务数据！
-- ============================================
-- 仅用于测试环境！绝对不要在生产环境执行！
-- 如需清空测试数据，请手动取消以下注释：
-- ============================================

-- SET FOREIGN_KEY_CHECKS = 0;
-- TRUNCATE TABLE task_approval_configs;
-- TRUNCATE TABLE task_assignees;
-- TRUNCATE TABLE tasks;
-- SET FOREIGN_KEY_CHECKS = 1;
