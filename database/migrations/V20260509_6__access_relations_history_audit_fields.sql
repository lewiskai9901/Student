-- Phase 5 W5.3: history 表补审计字段
SET @stmt := IF(
  (SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='access_relations_history' AND column_name='operator_ip') = 0,
  'ALTER TABLE access_relations_history
     ADD COLUMN operator_ip VARCHAR(45) NULL AFTER archived_by,
     ADD COLUMN operator_user_agent VARCHAR(500) NULL AFTER operator_ip,
     ADD COLUMN operation VARCHAR(20) NULL AFTER operator_user_agent
       COMMENT ''GRANT/REVOKE/MODIFY''',
  'SELECT 1');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

-- 索引: 按时间查变更
SET @stmt := IF(
  (SELECT COUNT(1) FROM information_schema.statistics
    WHERE table_schema=DATABASE() AND table_name='access_relations_history' AND index_name='idx_history_archived_at') = 0,
  'ALTER TABLE access_relations_history ADD INDEX idx_history_archived_at (archived_at)',
  'SELECT 1');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;
