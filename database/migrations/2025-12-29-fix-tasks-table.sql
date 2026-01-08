-- =====================================================
-- 修复 tasks 表结构
-- 日期: 2025-12-29
-- 说明: 添加缺失的 status 字段和相关字段
-- =====================================================

-- 检查并添加 status 字段
SET @dbname = 'student_management';
SET @tablename = 'tasks';
SET @columnname = 'status';

SET @preparedStatement = (SELECT IF(
    (
        SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @dbname
        AND TABLE_NAME = @tablename
        AND COLUMN_NAME = @columnname
    ) > 0,
    'SELECT "Column status already exists"',
    'ALTER TABLE tasks ADD COLUMN status TINYINT DEFAULT 0 COMMENT "状态: 0-待接收, 1-进行中, 2-待审核, 3-已完成, 4-已打回, 5-已取消, 6-审批中" AFTER assign_type'
));

PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加索引（如果不存在）
SET @indexname = 'idx_status';
SET @preparedStatement = (SELECT IF(
    (
        SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = @dbname
        AND TABLE_NAME = @tablename
        AND INDEX_NAME = @indexname
    ) > 0,
    'SELECT "Index idx_status already exists"',
    'ALTER TABLE tasks ADD INDEX idx_status (status)'
));

PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 为 task_approval_records 添加 department_id 字段（如果不存在）
SET @tablename = 'task_approval_records';
SET @columnname = 'department_id';

SET @preparedStatement = (SELECT IF(
    (
        SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @dbname
        AND TABLE_NAME = @tablename
        AND COLUMN_NAME = @columnname
    ) > 0,
    'SELECT "Column department_id already exists in task_approval_records"',
    'ALTER TABLE task_approval_records ADD COLUMN department_id BIGINT COMMENT "关联系部ID" AFTER submission_id'
));

PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 验证结果
SELECT 'tasks表结构:' as info;
SHOW COLUMNS FROM tasks WHERE Field IN ('status', 'assign_type');

SELECT 'task_approval_records表结构:' as info;
SHOW COLUMNS FROM task_approval_records WHERE Field IN ('department_id', 'submission_id');
