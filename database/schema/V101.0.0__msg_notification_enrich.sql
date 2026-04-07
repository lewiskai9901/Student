-- ============================================================
-- V101.0.0 Enrich msg_notifications with subject/category/module
-- ============================================================

ALTER TABLE msg_notifications
  ADD COLUMN subject_type VARCHAR(20) COMMENT '事件主体类型(USER/ORG_UNIT/PLACE)' AFTER source_ref_id,
  ADD COLUMN subject_id BIGINT COMMENT '事件主体ID' AFTER subject_type,
  ADD COLUMN subject_name VARCHAR(100) COMMENT '事件主体名称' AFTER subject_id,
  ADD COLUMN event_category VARCHAR(30) COMMENT '事件大类(DISCIPLINE/AWARD/INSPECTION等)' AFTER subject_name,
  ADD COLUMN source_module VARCHAR(30) COMMENT '来源模块(inspection/attendance/dormitory等)' AFTER event_category;

-- 添加索引（忽略已存在的情况）
SET @idx1 = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'msg_notifications' AND index_name = 'idx_msg_subject');
SET @sql1 = IF(@idx1 = 0, 'CREATE INDEX idx_msg_subject ON msg_notifications (subject_type, subject_id)', 'SELECT 1');
PREPARE s1 FROM @sql1; EXECUTE s1; DEALLOCATE PREPARE s1;

SET @idx2 = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'msg_notifications' AND index_name = 'idx_msg_category');
SET @sql2 = IF(@idx2 = 0, 'CREATE INDEX idx_msg_category ON msg_notifications (event_category)', 'SELECT 1');
PREPARE s2 FROM @sql2; EXECUTE s2; DEALLOCATE PREPARE s2;
