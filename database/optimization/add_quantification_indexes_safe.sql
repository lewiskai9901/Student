-- ============================================
-- 量化检查模块 - 安全索引优化脚本
-- 创建时间: 2025-11-19
-- 说明: 先检查索引是否存在，不存在才创建
-- ============================================

USE student_management;

-- 创建存储过程来安全添加索引
DELIMITER $$

CREATE PROCEDURE IF NOT EXISTS add_index_if_not_exists(
    IN table_name VARCHAR(64),
    IN index_name VARCHAR(64),
    IN index_sql TEXT
)
BEGIN
    DECLARE index_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO index_exists
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = table_name
      AND INDEX_NAME = index_name;

    IF index_exists = 0 THEN
        SET @sql = index_sql;
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SELECT CONCAT('✓ 索引已创建: ', table_name, '.', index_name) AS result;
    ELSE
        SELECT CONCAT('⊙ 索引已存在: ', table_name, '.', index_name) AS result;
    END IF;
END$$

DELIMITER ;

-- ============================================
-- 执行索引创建
-- ============================================

-- check_records_v3
CALL add_index_if_not_exists('check_records_v3', 'idx_date_status',
    'ALTER TABLE check_records_v3 ADD INDEX idx_date_status (check_date, status)');

CALL add_index_if_not_exists('check_records_v3', 'idx_checker_date',
    'ALTER TABLE check_records_v3 ADD INDEX idx_checker_date (checker_id, check_date DESC)');

CALL add_index_if_not_exists('check_records_v3', 'idx_publish_time',
    'ALTER TABLE check_records_v3 ADD INDEX idx_publish_time (publish_time)');

-- check_record_class_stats
CALL add_index_if_not_exists('check_record_class_stats', 'idx_record_class',
    'ALTER TABLE check_record_class_stats ADD INDEX idx_record_class (record_id, class_id)');

CALL add_index_if_not_exists('check_record_class_stats', 'idx_class_record',
    'ALTER TABLE check_record_class_stats ADD INDEX idx_class_record (class_id, record_id)');

CALL add_index_if_not_exists('check_record_class_stats', 'idx_grade_score',
    'ALTER TABLE check_record_class_stats ADD INDEX idx_grade_score (grade_id, total_score DESC)');

-- check_record_category_stats
CALL add_index_if_not_exists('check_record_category_stats', 'idx_record_category',
    'ALTER TABLE check_record_category_stats ADD INDEX idx_record_category (record_id, category_id)');

-- check_record_items_v3
CALL add_index_if_not_exists('check_record_items_v3', 'idx_record_class_stat',
    'ALTER TABLE check_record_items_v3 ADD INDEX idx_record_class_stat (record_id, class_stat_id)');

CALL add_index_if_not_exists('check_record_items_v3', 'idx_category_class',
    'ALTER TABLE check_record_items_v3 ADD INDEX idx_category_class (category_id, class_stat_id)');

CALL add_index_if_not_exists('check_record_items_v3', 'idx_appeal',
    'ALTER TABLE check_record_items_v3 ADD INDEX idx_appeal (appeal_status, appeal_id)');

CALL add_index_if_not_exists('check_record_items_v3', 'idx_link',
    'ALTER TABLE check_record_items_v3 ADD INDEX idx_link (link_type, link_id)');

-- daily_checks
CALL add_index_if_not_exists('daily_checks', 'idx_date_type',
    'ALTER TABLE daily_checks ADD INDEX idx_date_type (check_date, check_type)');

CALL add_index_if_not_exists('daily_checks', 'idx_checker',
    'ALTER TABLE daily_checks ADD INDEX idx_checker (checker_id)');

-- daily_check_details
CALL add_index_if_not_exists('daily_check_details', 'idx_check_class',
    'ALTER TABLE daily_check_details ADD INDEX idx_check_class (check_id, class_id)');

CALL add_index_if_not_exists('daily_check_details', 'idx_category',
    'ALTER TABLE daily_check_details ADD INDEX idx_category (category_id)');

-- appeal_records
CALL add_index_if_not_exists('appeal_records', 'idx_record_item',
    'ALTER TABLE appeal_records ADD INDEX idx_record_item (record_item_id, status)');

CALL add_index_if_not_exists('appeal_records', 'idx_applicant_status',
    'ALTER TABLE appeal_records ADD INDEX idx_applicant_status (applicant_id, status, created_at DESC)');

CALL add_index_if_not_exists('appeal_records', 'idx_status_time',
    'ALTER TABLE appeal_records ADD INDEX idx_status_time (status, created_at DESC)');

-- 删除临时存储过程
DROP PROCEDURE IF EXISTS add_index_if_not_exists;

-- 查看创建的索引
SELECT '========== 索引创建完成 ==========' AS message;

SELECT
    TABLE_NAME AS '表名',
    INDEX_NAME AS '索引名',
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS '索引列',
    INDEX_TYPE AS '索引类型',
    CARDINALITY AS '基数'
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'student_management'
  AND TABLE_NAME IN ('check_records_v3', 'check_record_class_stats', 'check_record_items_v3',
                      'daily_checks', 'daily_check_details', 'appeal_records')
  AND INDEX_NAME != 'PRIMARY'
GROUP BY TABLE_NAME, INDEX_NAME, INDEX_TYPE, CARDINALITY
ORDER BY TABLE_NAME, INDEX_NAME;
