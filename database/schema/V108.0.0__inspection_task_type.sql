-- ============================================================
-- V108: 检查任务多类型支持 (SCHEDULED/AD_HOC/TRIGGERED/SELF_CHECK/COMPLAINT/CROSS_AUDIT)
-- 解耦"计划检查"硬模型, 支持随机抽查/突击/举报核查/自查/互查
--
-- 向后兼容: 现有所有 task 默认 task_type='SCHEDULED', 行为不变
-- ============================================================

-- 1) insp_tasks 加任务类型 + 来源溯源 + 灵活 deadline policy
SET @col := (SELECT COUNT(*) FROM information_schema.columns
             WHERE table_schema=DATABASE() AND table_name='insp_tasks' AND column_name='task_type');
SET @sql := IF(@col=0,
  'ALTER TABLE insp_tasks ADD COLUMN task_type VARCHAR(20) NOT NULL DEFAULT ''SCHEDULED'' COMMENT ''SCHEDULED/AD_HOC/TRIGGERED/SELF_CHECK/COMPLAINT/CROSS_AUDIT''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.columns
             WHERE table_schema=DATABASE() AND table_name='insp_tasks' AND column_name='source_type');
SET @sql := IF(@col=0,
  'ALTER TABLE insp_tasks ADD COLUMN source_type VARCHAR(20) COMMENT ''SCHEDULER/MANUAL/EVENT/IMPORT''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.columns
             WHERE table_schema=DATABASE() AND table_name='insp_tasks' AND column_name='source_actor_id');
SET @sql := IF(@col=0,
  'ALTER TABLE insp_tasks ADD COLUMN source_actor_id BIGINT COMMENT ''触发用户ID''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.columns
             WHERE table_schema=DATABASE() AND table_name='insp_tasks' AND column_name='source_reason');
SET @sql := IF(@col=0,
  'ALTER TABLE insp_tasks ADD COLUMN source_reason VARCHAR(500) COMMENT ''发起原因/说明''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.columns
             WHERE table_schema=DATABASE() AND table_name='insp_tasks' AND column_name='source_ref_type');
SET @sql := IF(@col=0,
  'ALTER TABLE insp_tasks ADD COLUMN source_ref_type VARCHAR(40) COMMENT ''来源单据类型 Appeal/Alert/Complaint''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.columns
             WHERE table_schema=DATABASE() AND table_name='insp_tasks' AND column_name='source_ref_id');
SET @sql := IF(@col=0,
  'ALTER TABLE insp_tasks ADD COLUMN source_ref_id BIGINT COMMENT ''来源单据ID''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.columns
             WHERE table_schema=DATABASE() AND table_name='insp_tasks' AND column_name='deadline_policy');
SET @sql := IF(@col=0,
  'ALTER TABLE insp_tasks ADD COLUMN deadline_policy VARCHAR(20) NOT NULL DEFAULT ''STRICT'' COMMENT ''STRICT(逾期硬扣)/RELAXED(软提醒)/NONE(永不逾期)''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- task_date 改为可空 (AD_HOC/SELF_CHECK 无明确日期)
SET @col := (SELECT IS_NULLABLE FROM information_schema.columns
             WHERE table_schema=DATABASE() AND table_name='insp_tasks' AND column_name='task_date');
SET @sql := IF(@col='NO',
  'ALTER TABLE insp_tasks MODIFY COLUMN task_date DATE NULL COMMENT ''计划检查日期 (SCHEDULED 必填, AD_HOC/SELF_CHECK 可空)''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 类型 + 状态联合索引 (前端 type filter 高频)
SET @idx := (SELECT COUNT(*) FROM information_schema.statistics
             WHERE table_schema=DATABASE() AND table_name='insp_tasks' AND index_name='idx_type_status');
SET @sql := IF(@idx=0,
  'ALTER TABLE insp_tasks ADD INDEX idx_type_status (task_type, status)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 来源单据反查索引 (从申诉/告警找触发的 task)
SET @idx := (SELECT COUNT(*) FROM information_schema.statistics
             WHERE table_schema=DATABASE() AND table_name='insp_tasks' AND index_name='idx_source_ref');
SET @sql := IF(@idx=0,
  'ALTER TABLE insp_tasks ADD INDEX idx_source_ref (source_ref_type, source_ref_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) insp_projects 加检查模式
SET @col := (SELECT COUNT(*) FROM information_schema.columns
             WHERE table_schema=DATABASE() AND table_name='insp_projects' AND column_name='inspection_mode');
SET @sql := IF(@col=0,
  'ALTER TABLE insp_projects ADD COLUMN inspection_mode VARCHAR(20) NOT NULL DEFAULT ''PLANNED'' COMMENT ''PLANNED/SPOT_CHECK/HYBRID/SELF_AUDIT/EMERGENCY''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.columns
             WHERE table_schema=DATABASE() AND table_name='insp_projects' AND column_name='allow_ad_hoc');
SET @sql := IF(@col=0,
  'ALTER TABLE insp_projects ADD COLUMN allow_ad_hoc TINYINT NOT NULL DEFAULT 0 COMMENT ''是否允许检查员临时抽查''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.columns
             WHERE table_schema=DATABASE() AND table_name='insp_projects' AND column_name='allow_self_check');
SET @sql := IF(@col=0,
  'ALTER TABLE insp_projects ADD COLUMN allow_self_check TINYINT NOT NULL DEFAULT 0 COMMENT ''是否允许受检主体自查''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.columns
             WHERE table_schema=DATABASE() AND table_name='insp_projects' AND column_name='ad_hoc_quota_per_inspector');
SET @sql := IF(@col=0,
  'ALTER TABLE insp_projects ADD COLUMN ad_hoc_quota_per_inspector INT COMMENT ''月度抽查配额, NULL=无限''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 验证
SELECT 'V108 done' AS msg,
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema=DATABASE() AND table_name='insp_tasks' AND column_name='task_type') AS task_type_added,
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema=DATABASE() AND table_name='insp_projects' AND column_name='inspection_mode') AS mode_added;
