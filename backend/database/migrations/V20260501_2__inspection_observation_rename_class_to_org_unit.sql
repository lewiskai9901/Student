-- ============================================================
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
-- V20260501_2: 检查平台 ObservationContext 行业字段去耦
--
-- inspection 是通用核心模块, 不应耦合"班级"概念. 重命名:
--   insp_submission_observations.class_name → org_unit_name
--
-- 同步更新 trigger_points.context_schema 为通用键集 (移除 studentId/className).
--
-- 标准 information_schema 条件化, 可重复执行.
-- ============================================================

-- ==================== 1. 列重命名 (幂等) ====================

SET @col_old = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='insp_submission_observations' AND column_name='class_name');
SET @col_new = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='insp_submission_observations' AND column_name='org_unit_name');
SET @sql = IF(@col_old = 1 AND @col_new = 0,
    'ALTER TABLE insp_submission_observations CHANGE COLUMN class_name org_unit_name VARCHAR(100) NULL COMMENT ''主体所属组织名称(冗余, 通用核心)''',
    IF(@col_old = 0 AND @col_new = 0,
        'ALTER TABLE insp_submission_observations ADD COLUMN org_unit_name VARCHAR(100) NULL COMMENT ''主体所属组织名称(冗余, 通用核心)''',
        'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ==================== 2. trigger_points.context_schema 通用化 ====================
-- 移除 studentId/studentName/classId/className 等行业耦合键,
-- 改为 subject* + orgUnit* 通用约定.
-- 下游订阅规则需同步更新 (字段名映射在 msg_subscription_rules.target_config).

UPDATE trigger_points SET context_schema = '{
  "isNegative":      {"type":"Boolean","label":"是否负面","role":"standard"},
  "severity":        {"type":"String","label":"严重程度","role":"standard"},
  "eventTypeHint":   {"type":"String","label":"事件类型","role":"eventType"},
  "subjectUserId":   {"type":"Long","label":"主体用户ID","subject":"USER","role":"id"},
  "subjectUserName": {"type":"String","label":"主体用户姓名","subject":"USER","role":"name"},
  "orgUnitId":       {"type":"Long","label":"组织ID","subject":"ORG_UNIT","role":"id"},
  "orgUnitName":     {"type":"String","label":"组织名称","subject":"ORG_UNIT","role":"name"},
  "placeId":         {"type":"Long","label":"场所ID","subject":"PLACE","role":"id"},
  "placeName":       {"type":"String","label":"场所名称","subject":"PLACE","role":"name"},
  "itemName":        {"type":"String","label":"检查项"},
  "score":           {"type":"Number","label":"分数"},
  "projectName":     {"type":"String","label":"项目名称"},
  "description":     {"type":"String","label":"描述"}
}' WHERE point_code = 'INSP_ITEM_RESULT' AND deleted = 0;

UPDATE trigger_points SET context_schema = '{
  "isNegative":   {"type":"Boolean","label":"是否负面","role":"standard"},
  "severity":     {"type":"String","label":"严重程度","role":"standard"},
  "eventTypeHint":{"type":"String","label":"事件类型","role":"eventType"},
  "targetId":     {"type":"Long","label":"目标ID","role":"id"},
  "targetName":   {"type":"String","label":"目标名称","role":"name"},
  "targetType":   {"type":"String","label":"目标类型(USER/ORG/PLACE/ASSET)"},
  "score":        {"type":"Number","label":"得分"},
  "grade":        {"type":"String","label":"等级"},
  "passed":       {"type":"Boolean","label":"是否合格"},
  "projectName":  {"type":"String","label":"项目名称"}
}' WHERE point_code = 'INSP_GRADE_RESULT' AND deleted = 0;
