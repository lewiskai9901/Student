-- 数据库索引优化脚本（安全版本）
-- 创建时间: 2025-11-18
-- 说明: 为常用查询字段添加索引，如果索引已存在则跳过

USE student_management;

DELIMITER $$

-- 创建存储过程：安全地添加索引
DROP PROCEDURE IF EXISTS AddIndexIfNotExists$$
CREATE PROCEDURE AddIndexIfNotExists(
    IN tableName VARCHAR(128),
    IN indexName VARCHAR(128),
    IN indexColumns VARCHAR(255)
)
BEGIN
    DECLARE indexExists INT DEFAULT 0;

    -- 检查索引是否存在
    SELECT COUNT(*) INTO indexExists
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = tableName
        AND INDEX_NAME = indexName;

    -- 如果索引不存在，则创建
    IF indexExists = 0 THEN
        SET @sql = CONCAT('CREATE INDEX ', indexName, ' ON ', tableName, '(', indexColumns, ')');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SELECT CONCAT('✓ 已创建索引: ', indexName, ' on ', tableName) as result;
    ELSE
        SELECT CONCAT('- 索引已存在: ', indexName, ' on ', tableName) as result;
    END IF;
END$$

DELIMITER ;

-- 学生表索引
CALL AddIndexIfNotExists('students', 'idx_student_no', 'student_no');
CALL AddIndexIfNotExists('students', 'idx_class_id', 'class_id');
CALL AddIndexIfNotExists('students', 'idx_user_id', 'user_id');

-- 班级表索引
CALL AddIndexIfNotExists('classes', 'idx_grade_id', 'grade_id');
CALL AddIndexIfNotExists('classes', 'idx_major_direction_id', 'major_direction_id');

-- 检查记录V3索引
CALL AddIndexIfNotExists('check_records_v3', 'idx_check_date', 'check_date');
CALL AddIndexIfNotExists('check_records_v3', 'idx_semester_id', 'semester_id');
CALL AddIndexIfNotExists('check_records_v3', 'idx_status_v3', 'status');

-- 检查记录项V3索引（包括复合索引）
CALL AddIndexIfNotExists('check_record_items_v3', 'idx_check_record_id', 'check_record_id');
CALL AddIndexIfNotExists('check_record_items_v3', 'idx_item_class_id', 'class_id');
CALL AddIndexIfNotExists('check_record_items_v3', 'idx_deduction_item_id', 'deduction_item_id');
CALL AddIndexIfNotExists('check_record_items_v3', 'idx_record_class', 'check_record_id, class_id');

-- 申诉表索引（包括复合索引）
CALL AddIndexIfNotExists('check_item_appeals', 'idx_appeal_status', 'status');
CALL AddIndexIfNotExists('check_item_appeals', 'idx_appeal_grade_id', 'grade_id');
CALL AddIndexIfNotExists('check_item_appeals', 'idx_appeal_record_id', 'record_id');
CALL AddIndexIfNotExists('check_item_appeals', 'idx_appeal_time', 'appeal_time');
CALL AddIndexIfNotExists('check_item_appeals', 'idx_grade_status', 'grade_id, status');

-- 宿舍表索引
CALL AddIndexIfNotExists('dormitories', 'idx_dormitory_building_id', 'dormitory_building_id');
CALL AddIndexIfNotExists('dormitories', 'idx_dormitory_floor', 'floor');

-- 用户角色表索引
CALL AddIndexIfNotExists('user_roles', 'idx_user_role_user_id', 'user_id');
CALL AddIndexIfNotExists('user_roles', 'idx_user_role_role_id', 'role_id');

-- 角色权限表索引
CALL AddIndexIfNotExists('role_permissions', 'idx_role_perm_role_id', 'role_id');
CALL AddIndexIfNotExists('role_permissions', 'idx_role_perm_perm_id', 'permission_id');

-- 清理存储过程
DROP PROCEDURE IF EXISTS AddIndexIfNotExists;

-- 完成统计
SELECT '=============================' as '分隔线';
SELECT '索引优化完成！' as '状态';
SELECT COUNT(DISTINCT INDEX_NAME) as '索引总数'
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'student_management'
  AND INDEX_NAME LIKE 'idx_%';

SELECT TABLE_NAME as '表名',
       INDEX_NAME as '索引名',
       GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) as '索引列'
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'student_management'
  AND INDEX_NAME LIKE 'idx_%'
  AND TABLE_NAME IN ('students', 'classes', 'check_records_v3', 'check_record_items_v3',
                     'check_item_appeals', 'dormitories', 'user_roles', 'role_permissions')
GROUP BY TABLE_NAME, INDEX_NAME
ORDER BY TABLE_NAME, INDEX_NAME;
