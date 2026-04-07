-- Phase 1: 校历×排课基础设施
-- 1. 作息表 period_configs
-- 2. 实况课表 schedule_instances
-- 3. 扩展 academic_event

-- 作息表(按学期配置节次时间)
CREATE TABLE IF NOT EXISTS period_configs (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  semester_id BIGINT NOT NULL,
  config_name VARCHAR(50) NOT NULL,
  periods_per_day INT NOT NULL DEFAULT 8,
  schedule_days JSON NOT NULL,
  periods JSON NOT NULL COMMENT '[{period,name,startTime,endTime}]',
  is_default TINYINT DEFAULT 1,
  created_by BIGINT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  INDEX idx_semester (semester_id)
) COMMENT '作息表/节次配置';

-- 实况课表(每日每节实例)
CREATE TABLE IF NOT EXISTS schedule_instances (
  id BIGINT NOT NULL PRIMARY KEY,
  entry_id BIGINT COMMENT '来源基准条目',
  semester_id BIGINT NOT NULL,
  actual_date DATE NOT NULL,
  weekday TINYINT NOT NULL COMMENT '1-7',
  week_number INT COMMENT '教学周',
  start_slot INT NOT NULL,
  end_slot INT NOT NULL,
  course_id BIGINT NOT NULL,
  class_id BIGINT NOT NULL,
  teacher_id BIGINT,
  original_teacher_id BIGINT COMMENT '代课时原教师',
  classroom_id BIGINT,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0正常 1已取消 2已调走 3补课 4代课',
  cancel_reason VARCHAR(200),
  source_type TINYINT NOT NULL DEFAULT 0 COMMENT '0基准展开 1调课 2补课日 3临时',
  source_id BIGINT COMMENT '来源ID',
  actual_hours DECIMAL(3,1) DEFAULT 1.0,
  remark VARCHAR(500),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  INDEX idx_sem_date (semester_id, actual_date),
  INDEX idx_teacher (teacher_id, actual_date),
  INDEX idx_class (class_id, actual_date),
  INDEX idx_classroom (classroom_id, actual_date),
  INDEX idx_entry (entry_id),
  INDEX idx_week (semester_id, week_number)
) COMMENT '实况课表';

-- 扩展 academic_event: 添加排课影响字段
SET @col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='academic_event' AND COLUMN_NAME='affect_type');
SET @s = IF(@col=0, 'ALTER TABLE academic_event ADD COLUMN affect_type TINYINT DEFAULT 0 COMMENT ''0无影响 1全天停课 2半天停课 3补课日 4考试周''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='academic_event' AND COLUMN_NAME='affect_scope');
SET @s = IF(@col=0, 'ALTER TABLE academic_event ADD COLUMN affect_scope VARCHAR(50) DEFAULT ''all'' COMMENT ''影响范围: all/grade:N/class:N''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='academic_event' AND COLUMN_NAME='substitute_weekday');
SET @s = IF(@col=0, 'ALTER TABLE academic_event ADD COLUMN substitute_weekday TINYINT COMMENT ''补课日按周几上课''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='academic_event' AND COLUMN_NAME='affect_slots');
SET @s = IF(@col=0, 'ALTER TABLE academic_event ADD COLUMN affect_slots VARCHAR(50) COMMENT ''影响节次范围''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
