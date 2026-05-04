-- V110: 整改判定引擎 (Sprint 1 MVP)
-- 1. insp_projects 增 corrective_strictness / corrective_severity_thresholds / corrective_default_deadlines
-- 2. insp_corrective_cases 增 suggested_by_system / suggestion_reason / severity_score / explain_trace_json

-- ============================================================
-- A. insp_projects: 项目级整改严格度策略
-- ============================================================
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'insp_projects'
    AND COLUMN_NAME = 'corrective_strictness');
SET @sql := IF(@col = 0,
  'ALTER TABLE insp_projects ADD COLUMN corrective_strictness VARCHAR(20) NOT NULL DEFAULT ''NORMAL''
   COMMENT ''整改严格度: STRICT/NORMAL/LENIENT/OFF''',
  'SELECT ''corrective_strictness exists''');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'insp_projects'
    AND COLUMN_NAME = 'corrective_severity_thresholds');
SET @sql := IF(@col = 0,
  'ALTER TABLE insp_projects ADD COLUMN corrective_severity_thresholds JSON NULL
   COMMENT ''严重度阈值覆盖, e.g. {"high":0.8,"medium":0.5,"low":0.3}''',
  'SELECT ''corrective_severity_thresholds exists''');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'insp_projects'
    AND COLUMN_NAME = 'corrective_default_deadlines');
SET @sql := IF(@col = 0,
  'ALTER TABLE insp_projects ADD COLUMN corrective_default_deadlines JSON NULL
   COMMENT ''默认 deadline 天数, e.g. {"high":3,"medium":7,"low":14}''',
  'SELECT ''corrective_default_deadlines exists''');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ============================================================
-- B. insp_template_items: 检查项级覆盖 (可选)
-- ============================================================
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'insp_template_items'
    AND COLUMN_NAME = 'corrective_override');
SET @sql := IF(@col = 0,
  'ALTER TABLE insp_template_items ADD COLUMN corrective_override JSON NULL
   COMMENT ''检查项级整改覆盖规则, JSON. neverCorrect/forceCorrect/thresholdOverride/deadlineOverride''',
  'SELECT ''corrective_override exists''');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ============================================================
-- C. insp_corrective_cases: 引擎产物字段
-- ============================================================
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'insp_corrective_cases'
    AND COLUMN_NAME = 'suggested_by_system');
SET @sql := IF(@col = 0,
  'ALTER TABLE insp_corrective_cases ADD COLUMN suggested_by_system TINYINT NOT NULL DEFAULT 0
   COMMENT ''是否由引擎建议生成 (1=引擎,0=手动)''',
  'SELECT ''suggested_by_system exists''');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'insp_corrective_cases'
    AND COLUMN_NAME = 'suggestion_reason');
SET @sql := IF(@col = 0,
  'ALTER TABLE insp_corrective_cases ADD COLUMN suggestion_reason VARCHAR(500) NULL
   COMMENT ''建议理由 (可读文本)''',
  'SELECT ''suggestion_reason exists''');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'insp_corrective_cases'
    AND COLUMN_NAME = 'severity_score');
SET @sql := IF(@col = 0,
  'ALTER TABLE insp_corrective_cases ADD COLUMN severity_score DECIMAL(4,3) NULL
   COMMENT ''标准化严重度分 0-1, 引擎计算''',
  'SELECT ''severity_score exists''');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'insp_corrective_cases'
    AND COLUMN_NAME = 'explain_trace_json');
SET @sql := IF(@col = 0,
  'ALTER TABLE insp_corrective_cases ADD COLUMN explain_trace_json JSON NULL
   COMMENT ''引擎决策审计链 [{layer,rule,output,...}]''',
  'SELECT ''explain_trace_json exists''');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SELECT 'V110 done — corrective trigger engine schema ready' AS msg;
