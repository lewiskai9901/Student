-- ============================================================
-- V100.0.0 Event Type Role Annotation
-- 给支持动态事件类型的触发点 contextSchema 添加 role:"eventType" 注解
-- 前端据此自动识别，无需用户手动选择来源字段
-- ============================================================

-- INSP_ITEM_RESULT: eventTypeHint 加 role:"eventType"
UPDATE trigger_points SET context_schema = JSON_SET(
  context_schema,
  '$.eventTypeHint.role', 'eventType'
) WHERE point_code = 'INSP_ITEM_RESULT' AND deleted = 0;

-- ATTENDANCE_RECORDED: 补充 eventTypeHint 字段（代码传了但 schema 未声明）
UPDATE trigger_points SET context_schema = JSON_SET(
  context_schema,
  '$.eventTypeHint', JSON_OBJECT('type', 'String', 'label', '事件类型(动态)', 'role', 'eventType')
) WHERE point_code = 'ATTENDANCE_RECORDED' AND deleted = 0;
