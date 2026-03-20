-- =====================================================
-- 场所批量操作Job系统 - 异步任务管理
-- 对标: AWS S3 Batch Operations, Google Cloud Tasks
-- 创建时间: 2026-02-13
-- 解决缺陷: P0.3 批量操作事务冲突
-- =====================================================

-- 1. 创建批量任务表
CREATE TABLE IF NOT EXISTS place_batch_jobs (
  -- ===== 唯一标识 =====
  job_id VARCHAR(64) PRIMARY KEY COMMENT '任务ID (UUID)',

  -- ===== 任务类型 =====
  job_type VARCHAR(50) NOT NULL COMMENT '任务类型: BATCH_ASSIGN_ORG/BATCH_CHECK_IN/BATCH_UPDATE_STATUS/BATCH_SET_CAPACITY',
  job_name VARCHAR(200) COMMENT '任务名称（用户自定义）',

  -- ===== 任务状态 =====
  job_status VARCHAR(30) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态: PENDING/RUNNING/COMPLETED/FAILED/PARTIALLY_COMPLETED/CANCELLED',

  -- ===== 进度信息 =====
  total_items INT NOT NULL COMMENT '总项目数',
  processed_items INT NOT NULL DEFAULT 0 COMMENT '已处理项目数',
  success_count INT NOT NULL DEFAULT 0 COMMENT '成功数量',
  failure_count INT NOT NULL DEFAULT 0 COMMENT '失败数量',
  skipped_count INT NOT NULL DEFAULT 0 COMMENT '跳过数量（如重复操作）',

  -- ===== 执行详情 =====
  failure_details JSON COMMENT '失败详情 [{itemId, itemName, errorCode, errorMessage}]',
  request_parameters JSON NOT NULL COMMENT '请求参数（完整输入）',

  -- ===== 时间信息 =====
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  started_at DATETIME COMMENT '开始执行时间',
  completed_at DATETIME COMMENT '完成时间',
  estimated_completion DATETIME COMMENT '预计完成时间',

  -- ===== 用户信息 =====
  created_by BIGINT NOT NULL COMMENT '创建用户ID',
  created_by_name VARCHAR(100) COMMENT '创建用户名（冗余）',

  -- ===== 重试控制 =====
  retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
  max_retries INT NOT NULL DEFAULT 3 COMMENT '最大重试次数',
  last_error TEXT COMMENT '最后一次错误信息',

  -- ===== 结果存储 =====
  result_summary JSON COMMENT '执行摘要 {duration, avgItemTime, peakMemory}',
  result_file_url VARCHAR(500) COMMENT '详细结果文件URL（大批量任务）',

  -- ===== 计算列 =====
  progress_percentage DECIMAL(5,2) GENERATED ALWAYS AS (
    CASE
      WHEN total_items = 0 THEN 0
      ELSE (processed_items * 100.0 / total_items)
    END
  ) STORED COMMENT '进度百分比（自动计算）',

  -- ===== 索引优化 =====
  INDEX idx_status_created (job_status, created_at DESC) COMMENT '按状态查询待处理任务',
  INDEX idx_created_by (created_by, created_at DESC) COMMENT '按创建用户查询任务历史',
  INDEX idx_job_type (job_type, created_at DESC) COMMENT '按任务类型查询',
  INDEX idx_created_at (created_at DESC) COMMENT '按创建时间倒序查询'

) ENGINE=InnoDB COMMENT='场所批量操作任务表（对标AWS Batch Operations）';

-- 2. 创建批量任务项明细表（可选，用于超大批量任务的断点续传）
CREATE TABLE IF NOT EXISTS place_batch_job_items (
  -- ===== 唯一标识 =====
  item_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '明细ID',
  job_id VARCHAR(64) NOT NULL COMMENT '所属任务ID',

  -- ===== 项目信息 =====
  item_index INT NOT NULL COMMENT '项目索引（处理顺序）',
  resource_type VARCHAR(50) NOT NULL COMMENT '资源类型: PLACE/OCCUPANT',
  resource_id BIGINT NOT NULL COMMENT '资源ID',
  resource_name VARCHAR(200) COMMENT '资源名称（冗余）',

  -- ===== 处理状态 =====
  item_status VARCHAR(30) NOT NULL DEFAULT 'PENDING' COMMENT '项目状态: PENDING/SUCCESS/FAILED/SKIPPED',

  -- ===== 执行详情 =====
  operation_data JSON COMMENT '操作数据（如 {orgUnitId: 123}）',
  error_code VARCHAR(50) COMMENT '错误代码',
  error_message TEXT COMMENT '错误消息',

  -- ===== 时间信息 =====
  processed_at DATETIME COMMENT '处理时间',
  retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',

  -- ===== 索引优化 =====
  INDEX idx_job_id (job_id, item_index) COMMENT '按任务ID查询明细',
  INDEX idx_job_status (job_id, item_status) COMMENT '按任务ID+状态查询失败项',

  FOREIGN KEY (job_id) REFERENCES place_batch_jobs(job_id) ON DELETE CASCADE

) ENGINE=InnoDB COMMENT='批量任务项明细表（断点续传支持）';

-- 3. 插入测试Job数据（用于开发验证）
INSERT INTO place_batch_jobs (
  job_id,
  job_type,
  job_name,
  job_status,
  total_items,
  processed_items,
  success_count,
  failure_count,
  request_parameters,
  created_by,
  created_by_name,
  created_at
) VALUES (
  UUID(),
  'BATCH_ASSIGN_ORG',
  '批量分配宿舍到新部门',
  'COMPLETED',
  100,
  100,
  98,
  2,
  JSON_OBJECT(
    'placeIds', JSON_ARRAY(1, 2, 3, 4, 5),
    'targetOrgUnitId', 200,
    'reason', '组织架构调整'
  ),
  1,
  'admin',
  NOW()
);

-- 4. 创建视图：任务监控看板
CREATE OR REPLACE VIEW v_batch_job_dashboard AS
SELECT
  job_id,
  job_type,
  job_name,
  job_status,
  total_items,
  processed_items,
  success_count,
  failure_count,
  progress_percentage,
  created_by_name,
  created_at,
  started_at,
  completed_at,
  TIMESTAMPDIFF(SECOND, started_at, COALESCE(completed_at, NOW())) AS duration_seconds
FROM place_batch_jobs
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
ORDER BY created_at DESC;

-- 5. 创建存储过程：提交批量任务
DELIMITER $$
CREATE PROCEDURE submit_batch_job(
  IN p_job_type VARCHAR(50),
  IN p_job_name VARCHAR(200),
  IN p_total_items INT,
  IN p_request_parameters JSON,
  IN p_created_by BIGINT,
  IN p_created_by_name VARCHAR(100),
  OUT p_job_id VARCHAR(64)
)
BEGIN
  -- 生成任务ID
  SET p_job_id = UUID();

  -- 插入任务记录
  INSERT INTO place_batch_jobs (
    job_id,
    job_type,
    job_name,
    job_status,
    total_items,
    request_parameters,
    created_by,
    created_by_name
  ) VALUES (
    p_job_id,
    p_job_type,
    p_job_name,
    'PENDING',
    p_total_items,
    p_request_parameters,
    p_created_by,
    p_created_by_name
  );

  -- 记录审计日志
  INSERT INTO place_audit_logs (
    event_id,
    request_id,
    resource_type,
    resource_id,
    event_name,
    event_type,
    event_source,
    event_time,
    user_id,
    user_name,
    source_ip,
    api_endpoint,
    request_parameters
  ) VALUES (
    UUID(),
    p_job_id,
    'BATCH_JOB',
    0,
    'SubmitBatchJob',
    'ApiCall',
    'place-batch-service',
    NOW(6),
    p_created_by,
    p_created_by_name,
    '127.0.0.1',
    '/api/v9/places/batch-jobs',
    JSON_OBJECT('jobId', p_job_id, 'jobType', p_job_type)
  );
END$$
DELIMITER ;

-- 6. 创建存储过程：更新任务进度
DELIMITER $$
CREATE PROCEDURE update_job_progress(
  IN p_job_id VARCHAR(64),
  IN p_processed_increment INT,
  IN p_success_increment INT,
  IN p_failure_increment INT
)
BEGIN
  DECLARE current_status VARCHAR(30);
  DECLARE current_processed INT;
  DECLARE current_total INT;

  -- 原子更新进度（避免并发冲突）
  UPDATE place_batch_jobs
  SET
    processed_items = processed_items + p_processed_increment,
    success_count = success_count + p_success_increment,
    failure_count = failure_count + p_failure_increment,
    started_at = COALESCE(started_at, NOW()),
    job_status = CASE
      WHEN job_status = 'PENDING' THEN 'RUNNING'
      ELSE job_status
    END
  WHERE job_id = p_job_id;

  -- 检查是否完成
  SELECT job_status, processed_items, total_items
  INTO current_status, current_processed, current_total
  FROM place_batch_jobs
  WHERE job_id = p_job_id;

  -- 如果已处理完所有项目，标记为完成
  IF current_processed >= current_total AND current_status = 'RUNNING' THEN
    UPDATE place_batch_jobs
    SET
      job_status = CASE
        WHEN failure_count = 0 THEN 'COMPLETED'
        WHEN failure_count < total_items THEN 'PARTIALLY_COMPLETED'
        ELSE 'FAILED'
      END,
      completed_at = NOW()
    WHERE job_id = p_job_id;
  END IF;
END$$
DELIMITER ;

-- 7. 创建存储过程：清理过期任务（保留30天）
DELIMITER $$
CREATE PROCEDURE cleanup_expired_jobs()
BEGIN
  DECLARE deleted_count INT DEFAULT 0;

  -- 删除30天前已完成的任务（级联删除明细）
  DELETE FROM place_batch_jobs
  WHERE job_status IN ('COMPLETED', 'FAILED', 'CANCELLED')
    AND completed_at < DATE_SUB(NOW(), INTERVAL 30 DAY);

  SET deleted_count = ROW_COUNT();

  SELECT CONCAT('已清理 ', deleted_count, ' 个过期批量任务') AS result;
END$$
DELIMITER ;

COMMIT;

-- 验证脚本
-- CALL submit_batch_job('BATCH_ASSIGN_ORG', '测试批量任务', 10, '{}', 1, 'admin', @job_id);
-- SELECT @job_id AS new_job_id;
-- CALL update_job_progress(@job_id, 5, 4, 1);
-- SELECT * FROM v_batch_job_dashboard WHERE job_id = @job_id;
-- CALL cleanup_expired_jobs();
