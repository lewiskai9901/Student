-- ============================================================
-- V102.0.0 Submission Observations + Standard Context Fields
-- Strategy + 归一化表 方案
-- ============================================================

-- 1. 评分观察记录表（检查平台归一化读模型）
CREATE TABLE IF NOT EXISTS insp_submission_observations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,

    -- 关联
    submission_id BIGINT NOT NULL COMMENT '所属提交',
    detail_id BIGINT NOT NULL COMMENT '所属检查项明细',
    project_id BIGINT COMMENT '项目ID(冗余)',
    task_id BIGINT COMMENT '任务ID(冗余)',

    -- 检查项信息
    item_code VARCHAR(50),
    item_name VARCHAR(200),
    item_type VARCHAR(30),
    section_name VARCHAR(200),

    -- 主体
    subject_type VARCHAR(20) NOT NULL COMMENT 'USER/ORG_UNIT/PLACE',
    subject_id BIGINT NOT NULL,
    subject_name VARCHAR(100),
    class_id BIGINT COMMENT '主体所属班级(冗余)',
    class_name VARCHAR(100),

    -- 评分结果
    score DECIMAL(10,2) NOT NULL DEFAULT 0,
    is_negative TINYINT NOT NULL DEFAULT 0,
    severity VARCHAR(10) COMMENT 'LOW/MEDIUM/HIGH/CRITICAL',
    is_flagged TINYINT NOT NULL DEFAULT 0,

    -- 事件关联
    linked_event_type_code VARCHAR(50),

    -- 原始数据
    response_value TEXT,
    description VARCHAR(500),

    -- 时间
    observed_at DATETIME NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,

    INDEX idx_obs_submission (submission_id),
    INDEX idx_obs_detail (detail_id),
    INDEX idx_obs_subject (subject_type, subject_id),
    INDEX idx_obs_class (class_id),
    INDEX idx_obs_project (project_id),
    INDEX idx_obs_negative (is_negative, severity),
    INDEX idx_obs_event_type (linked_event_type_code),
    INDEX idx_obs_observed (observed_at)
) COMMENT='评分观察记录(检查平台归一化读模型)';

-- 2. 更新 INSP_ITEM_RESULT 触发点 contextSchema（加标准字段）
UPDATE trigger_points SET context_schema = '{
  "isNegative":    {"type":"Boolean","label":"是否负面","role":"standard"},
  "severity":      {"type":"String","label":"严重程度","role":"standard"},
  "eventTypeHint": {"type":"String","label":"事件类型","role":"eventType"},
  "studentId":     {"type":"Long","label":"学生ID","subject":"USER","role":"id"},
  "studentName":   {"type":"String","label":"学生姓名","subject":"USER","role":"name"},
  "classId":       {"type":"Long","label":"班级ID","subject":"ORG_UNIT","role":"id"},
  "className":     {"type":"String","label":"班级名称","subject":"ORG_UNIT","role":"name"},
  "placeId":       {"type":"Long","label":"场所ID","subject":"PLACE","role":"id"},
  "placeName":     {"type":"String","label":"场所名称","subject":"PLACE","role":"name"},
  "itemName":      {"type":"String","label":"检查项"},
  "score":         {"type":"Number","label":"分数"},
  "projectName":   {"type":"String","label":"项目名称"},
  "description":   {"type":"String","label":"描述"}
}' WHERE point_code = 'INSP_ITEM_RESULT' AND deleted = 0;

-- 3. 更新 INSP_GRADE_RESULT contextSchema
UPDATE trigger_points SET context_schema = '{
  "isNegative":    {"type":"Boolean","label":"是否负面","role":"standard"},
  "severity":      {"type":"String","label":"严重程度","role":"standard"},
  "eventTypeHint": {"type":"String","label":"事件类型","role":"eventType"},
  "targetId":      {"type":"Long","label":"目标ID","subject":"ORG_UNIT","role":"id"},
  "targetName":    {"type":"String","label":"目标名称","subject":"ORG_UNIT","role":"name"},
  "targetType":    {"type":"String","label":"目标类型"},
  "score":         {"type":"Number","label":"得分"},
  "grade":         {"type":"String","label":"等级"},
  "gradeName":     {"type":"String","label":"等级名称"},
  "passed":        {"type":"Boolean","label":"是否合格"},
  "projectName":   {"type":"String","label":"项目名称"}
}' WHERE point_code = 'INSP_GRADE_RESULT' AND deleted = 0;

-- 4. 更新默认触发器条件: result=="扣分" → isNegative==true
UPDATE event_triggers
SET condition_json = '{"isNegative": true}'
WHERE trigger_point_code = 'INSP_ITEM_RESULT' AND deleted = 0;
