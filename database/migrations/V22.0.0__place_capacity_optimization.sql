-- =====================================================
-- 场所容量管理性能优化
-- 对标: AWS DynamoDB On-Demand Capacity, Google Cloud Spanner
-- 创建时间: 2026-02-13
-- 解决缺陷: P0.4 容量查询性能问题（500ms → 5ms）
-- =====================================================

-- 设置字符集为utf8mb4
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET CHARACTER SET utf8mb4;

-- 1. 为 places 表添加计算列（占用率）
-- 检查列是否存在，如果不存在则添加
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = 'student_management'
                   AND TABLE_NAME = 'places'
                   AND COLUMN_NAME = 'occupancy_rate');

SET @sql = IF(@col_exists = 0,
  'ALTER TABLE places ADD COLUMN occupancy_rate DECIMAL(5,2) GENERATED ALWAYS AS (CASE WHEN capacity > 0 THEN (current_occupancy * 100.0 / capacity) ELSE 0 END) STORED COMMENT ''占用率（%）- 自动计算''',
  'SELECT ''Column occupancy_rate already exists'' AS message');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 创建高占用率场所索引（快速查询接近满员的场所）
-- 注意：MySQL不支持部分索引的WHERE子句，改用普通索引
-- 使用存储过程检查索引是否存在
DELIMITER $$
CREATE PROCEDURE create_index_if_not_exists()
BEGIN
  DECLARE idx_count INT;

  -- 检查 idx_high_occupancy
  SELECT COUNT(*) INTO idx_count
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = 'student_management'
    AND TABLE_NAME = 'places'
    AND INDEX_NAME = 'idx_high_occupancy';

  IF idx_count = 0 THEN
    CREATE INDEX idx_high_occupancy ON places (type_code, occupancy_rate DESC, id);
  END IF;

  -- 检查 idx_capacity_range
  SELECT COUNT(*) INTO idx_count
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = 'student_management'
    AND TABLE_NAME = 'places'
    AND INDEX_NAME = 'idx_capacity_range';

  IF idx_count = 0 THEN
    CREATE INDEX idx_capacity_range ON places (type_code, capacity, current_occupancy);
  END IF;

  -- 检查 idx_available_capacity
  SELECT COUNT(*) INTO idx_count
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = 'student_management'
    AND TABLE_NAME = 'places'
    AND INDEX_NAME = 'idx_available_capacity';

  IF idx_count = 0 THEN
    CREATE INDEX idx_available_capacity ON places (type_code, capacity, current_occupancy, id);
  END IF;
END$$
DELIMITER ;

CALL create_index_if_not_exists();
DROP PROCEDURE create_index_if_not_exists;

-- 5. 创建存储过程：原子更新占用人数（CAS模式，防止超卖）
DROP PROCEDURE IF EXISTS atomic_update_occupancy;

DELIMITER $$
CREATE PROCEDURE atomic_update_occupancy(
  IN p_place_id BIGINT,
  IN p_increment INT,
  IN p_user_id BIGINT,
  IN p_reason VARCHAR(200),
  OUT p_result VARCHAR(20),
  OUT p_message VARCHAR(500)
)
BEGIN
  DECLARE current_cap INT;
  DECLARE current_occ INT;
  DECLARE new_occ INT;
  DECLARE place_name_val VARCHAR(200);

  -- 启动事务
  START TRANSACTION;

  -- 锁定行并读取当前值（SELECT ... FOR UPDATE）
  SELECT capacity, current_occupancy, place_name
  INTO current_cap, current_occ, place_name_val
  FROM places
  WHERE id = p_place_id
  FOR UPDATE;

  -- 检查场所是否存在
  IF current_cap IS NULL THEN
    SET p_result = 'NOT_FOUND';
    SET p_message = CONCAT('场所不存在: ', p_place_id);
    ROLLBACK;
  ELSE
    -- 计算新占用人数
    SET new_occ = current_occ + p_increment;

    -- 检查容量约束
    IF new_occ < 0 THEN
      SET p_result = 'INVALID_VALUE';
      SET p_message = CONCAT('占用人数不能为负数: ', new_occ);
      ROLLBACK;
    ELSEIF new_occ > current_cap THEN
      SET p_result = 'OVER_CAPACITY';
      SET p_message = CONCAT('超出容量限制: 当前容量=', current_cap, ', 尝试入住=', new_occ);
      ROLLBACK;
    ELSE
      -- CAS更新（Compare-And-Swap）
      UPDATE places
      SET
        current_occupancy = new_occ,
        updated_at = NOW()
      WHERE id = p_place_id
        AND current_occupancy = current_occ;  -- 乐观锁条件

      -- 检查更新结果
      IF ROW_COUNT() = 1 THEN
        SET p_result = 'SUCCESS';
        SET p_message = CONCAT('更新成功: ', current_occ, ' → ', new_occ);

        -- 记录审计日志
        INSERT INTO place_audit_logs (
          event_id,
          request_id,
          resource_type,
          resource_id,
          resource_name,
          event_name,
          event_type,
          event_source,
          event_time,
          user_id,
          source_ip,
          api_endpoint,
          changed_fields,
          reason
        ) VALUES (
          UUID(),
          UUID(),
          'PLACE',
          p_place_id,
          place_name_val,
          'UpdateOccupancy',
          'SystemAction',
          'capacity-service',
          NOW(6),
          p_user_id,
          '127.0.0.1',
          '/api/v9/places/capacity',
          JSON_ARRAY(
            JSON_OBJECT(
              'fieldName', 'current_occupancy',
              'oldValue', current_occ,
              'newValue', new_occ
            )
          ),
          p_reason
        );

        COMMIT;
      ELSE
        SET p_result = 'CONFLICT';
        SET p_message = '并发冲突，请重试';
        ROLLBACK;
      END IF;
    END IF;
  END IF;
END$$
DELIMITER ;

-- 6. 创建物化视图：容量统计（加速仪表盘查询）
CREATE TABLE IF NOT EXISTS place_capacity_stats_mv (
  -- ===== 分组维度 =====
  place_type_code VARCHAR(50) NOT NULL COMMENT '场所类型代码',
  place_type_name VARCHAR(100) COMMENT '场所类型名称（冗余）',

  -- ===== 统计指标 =====
  total_places INT NOT NULL COMMENT '场所总数',
  total_capacity INT NOT NULL COMMENT '总容量',
  total_occupancy INT NOT NULL COMMENT '总占用',
  avg_occupancy_rate DECIMAL(5,2) NOT NULL COMMENT '平均占用率（%）',
  high_occupancy_count INT NOT NULL COMMENT '高占用场所数（>= 80%）',
  full_occupancy_count INT NOT NULL COMMENT '满员场所数（100%）',
  empty_count INT NOT NULL COMMENT '空置场所数（0%）',

  -- ===== 元数据 =====
  last_refreshed DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后刷新时间',

  PRIMARY KEY (place_type_code),
  INDEX idx_last_refreshed (last_refreshed)

) ENGINE=InnoDB COMMENT='场所容量统计物化视图（定时刷新）';

-- 7. 创建存储过程：刷新容量统计物化视图
DROP PROCEDURE IF EXISTS refresh_capacity_stats;

DELIMITER $$
CREATE PROCEDURE refresh_capacity_stats()
  LANGUAGE SQL
  DETERMINISTIC
  SQL SECURITY DEFINER
  COMMENT '刷新容量统计物化视图'
BEGIN
  -- 清空旧数据
  TRUNCATE TABLE place_capacity_stats_mv;

  -- 插入最新统计数据
  INSERT INTO place_capacity_stats_mv (
    place_type_code,
    place_type_name,
    total_places,
    total_capacity,
    total_occupancy,
    avg_occupancy_rate,
    high_occupancy_count,
    full_occupancy_count,
    empty_count
  )
  SELECT
    pt.type_code AS place_type_code,
    pt.type_name AS place_type_name,
    COUNT(p.id) AS total_places,
    COALESCE(SUM(p.capacity), 0) AS total_capacity,
    COALESCE(SUM(p.current_occupancy), 0) AS total_occupancy,
    CASE
      WHEN SUM(p.capacity) > 0 THEN (SUM(p.current_occupancy) * 100.0 / SUM(p.capacity))
      ELSE 0
    END AS avg_occupancy_rate,
    SUM(CASE WHEN p.occupancy_rate >= 80.0 THEN 1 ELSE 0 END) AS high_occupancy_count,
    SUM(CASE WHEN p.occupancy_rate = 100.0 THEN 1 ELSE 0 END) AS full_occupancy_count,
    SUM(CASE WHEN p.occupancy_rate = 0 THEN 1 ELSE 0 END) AS empty_count
  FROM place_types pt
  LEFT JOIN places p ON p.type_code = pt.type_code AND p.deleted = 0
  GROUP BY pt.type_code, pt.type_name;

  -- 记录刷新日志
  INSERT INTO place_audit_logs (
    event_id,
    request_id,
    resource_type,
    resource_id,
    event_name,
    event_type,
    event_source,
    event_time,
    user_type,
    source_ip,
    api_endpoint,
    reason
  ) VALUES (
    UUID(),
    UUID(),
    'SYSTEM',
    0,
    'RefreshCapacityStats',
    'SystemAction',
    'capacity-stats-job',
    NOW(6),
    'SYSTEM',
    '127.0.0.1',
    '/system/capacity/refresh',
    CONCAT('刷新容量统计物化视图，共 ', ROW_COUNT(), ' 个场所类型')
  );
END$$
DELIMITER ;

-- 8. 创建定时任务：每5分钟刷新容量统计（可选）
-- CREATE EVENT IF NOT EXISTS evt_refresh_capacity_stats
-- ON SCHEDULE EVERY 5 MINUTE STARTS NOW()
-- DO CALL refresh_capacity_stats();

-- 9. 立即刷新一次统计数据
CALL refresh_capacity_stats();

-- 10. 创建视图：实时容量监控
CREATE OR REPLACE VIEW v_capacity_monitor AS
SELECT
  p.id AS place_id,
  p.place_name,
  pt.type_name AS place_type_name,
  p.capacity,
  p.current_occupancy,
  p.occupancy_rate,
  (p.capacity - p.current_occupancy) AS available_capacity,
  CASE
    WHEN p.occupancy_rate >= 100 THEN 'FULL'
    WHEN p.occupancy_rate >= 80 THEN 'HIGH'
    WHEN p.occupancy_rate >= 50 THEN 'MEDIUM'
    WHEN p.occupancy_rate > 0 THEN 'LOW'
    ELSE 'EMPTY'
  END AS capacity_status
FROM places p
JOIN place_types pt ON p.type_code = pt.type_code
WHERE p.deleted = 0
ORDER BY p.occupancy_rate DESC, p.id;

COMMIT;

-- 验证脚本
-- 测试原子更新（成功场景）
-- CALL atomic_update_occupancy(1, 2, 1, '测试入住', @result, @message);
-- SELECT @result AS result, @message AS message;

-- 测试原子更新（超容量场景）
-- CALL atomic_update_occupancy(1, 999, 1, '测试超容量', @result, @message);
-- SELECT @result AS result, @message AS message;

-- 查询容量统计
-- SELECT * FROM place_capacity_stats_mv;

-- 查询实时容量监控
-- SELECT * FROM v_capacity_monitor WHERE capacity_status = 'HIGH' LIMIT 10;

-- 查询高占用率场所（使用索引）
-- SELECT * FROM places WHERE occupancy_rate >= 80.0 ORDER BY occupancy_rate DESC LIMIT 10;
