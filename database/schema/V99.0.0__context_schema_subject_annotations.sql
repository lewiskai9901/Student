-- ============================================================
-- V99.0.0 Context Schema Subject Annotations
-- 给 trigger_points.context_schema 的 ID/Name 字段添加
-- subject (USER/ORG_UNIT/PLACE) 和 role (id/name) 注解
-- 前端可据此自动识别可用主体，避免用户错配
-- ============================================================

-- 检查平台: INSP_ITEM_RESULT (3个主体: 学生/班级/场所)
UPDATE trigger_points SET context_schema = '{
  "studentId":{"type":"Long","label":"学生ID","subject":"USER","role":"id"},
  "studentName":{"type":"String","label":"学生姓名","subject":"USER","role":"name"},
  "classId":{"type":"Long","label":"班级ID","subject":"ORG_UNIT","role":"id"},
  "className":{"type":"String","label":"班级名称","subject":"ORG_UNIT","role":"name"},
  "placeId":{"type":"Long","label":"场所ID","subject":"PLACE","role":"id"},
  "placeName":{"type":"String","label":"场所名称","subject":"PLACE","role":"name"},
  "itemName":{"type":"String","label":"检查项名称"},
  "result":{"type":"String","label":"结果"},
  "score":{"type":"Number","label":"分数"},
  "eventTypeHint":{"type":"String","label":"事件类型(动态)"}
}' WHERE point_code = 'INSP_ITEM_RESULT' AND deleted = 0;

-- 检查平台: INSP_GRADE_RESULT (目标通常是组织)
UPDATE trigger_points SET context_schema = '{
  "targetId":{"type":"Long","label":"目标ID","subject":"ORG_UNIT","role":"id"},
  "targetName":{"type":"String","label":"目标名称","subject":"ORG_UNIT","role":"name"},
  "targetType":{"type":"String","label":"目标类型"},
  "score":{"type":"Number","label":"得分"},
  "grade":{"type":"String","label":"等级"},
  "gradeName":{"type":"String","label":"等级名称"}
}' WHERE point_code = 'INSP_GRADE_RESULT' AND deleted = 0;

-- 检查平台: INSP_RECORD_COMPLETE
UPDATE trigger_points SET context_schema = '{
  "taskId":{"type":"Long","label":"任务ID"},
  "targetId":{"type":"Long","label":"目标ID","subject":"ORG_UNIT","role":"id"},
  "targetName":{"type":"String","label":"目标名称","subject":"ORG_UNIT","role":"name"},
  "score":{"type":"Number","label":"总分"},
  "inspectorName":{"type":"String","label":"检查员"}
}' WHERE point_code = 'INSP_RECORD_COMPLETE' AND deleted = 0;

-- 考勤: ATTENDANCE_RECORDED (学生+班级)
UPDATE trigger_points SET context_schema = '{
  "studentId":{"type":"Long","label":"学生ID","subject":"USER","role":"id"},
  "studentName":{"type":"String","label":"学生姓名","subject":"USER","role":"name"},
  "classId":{"type":"Long","label":"班级ID","subject":"ORG_UNIT","role":"id"},
  "className":{"type":"String","label":"班级名称","subject":"ORG_UNIT","role":"name"},
  "status":{"type":"Number","label":"状态(2迟到3早退5旷课)"},
  "statusName":{"type":"String","label":"状态名称"},
  "date":{"type":"String","label":"日期"},
  "courseName":{"type":"String","label":"课程"}
}' WHERE point_code = 'ATTENDANCE_RECORDED' AND deleted = 0;

-- 考勤: LEAVE_REQUEST_SUBMITTED (学生)
UPDATE trigger_points SET context_schema = '{
  "studentId":{"type":"Long","label":"学生ID","subject":"USER","role":"id"},
  "studentName":{"type":"String","label":"学生姓名","subject":"USER","role":"name"},
  "leaveType":{"type":"String","label":"请假类型"},
  "startDate":{"type":"String","label":"开始"},
  "endDate":{"type":"String","label":"结束"}
}' WHERE point_code = 'LEAVE_REQUEST_SUBMITTED' AND deleted = 0;

-- 宿舍: DORM_CHECKIN (用户+场所)
UPDATE trigger_points SET context_schema = '{
  "occupantId":{"type":"Long","label":"入住人ID","subject":"USER","role":"id"},
  "occupantName":{"type":"String","label":"入住人","subject":"USER","role":"name"},
  "placeId":{"type":"Long","label":"房间ID","subject":"PLACE","role":"id"},
  "placeName":{"type":"String","label":"房间","subject":"PLACE","role":"name"}
}' WHERE point_code = 'DORM_CHECKIN' AND deleted = 0;

-- 宿舍: DORM_CHECKOUT (用户+场所)
UPDATE trigger_points SET context_schema = '{
  "occupantId":{"type":"Long","label":"退宿人ID","subject":"USER","role":"id"},
  "occupantName":{"type":"String","label":"退宿人","subject":"USER","role":"name"},
  "placeId":{"type":"Long","label":"房间ID","subject":"PLACE","role":"id"},
  "placeName":{"type":"String","label":"房间","subject":"PLACE","role":"name"},
  "reason":{"type":"String","label":"原因"}
}' WHERE point_code = 'DORM_CHECKOUT' AND deleted = 0;

-- 组织: ORG_MEMBER_JOIN (用户+组织)
UPDATE trigger_points SET context_schema = '{
  "userId":{"type":"Long","label":"用户ID","subject":"USER","role":"id"},
  "userName":{"type":"String","label":"用户姓名","subject":"USER","role":"name"},
  "orgUnitId":{"type":"Long","label":"组织ID","subject":"ORG_UNIT","role":"id"},
  "orgUnitName":{"type":"String","label":"组织名称","subject":"ORG_UNIT","role":"name"}
}' WHERE point_code = 'ORG_MEMBER_JOIN' AND deleted = 0;

-- 组织: ORG_MEMBER_LEAVE (用户+组织)
UPDATE trigger_points SET context_schema = '{
  "userId":{"type":"Long","label":"用户ID","subject":"USER","role":"id"},
  "userName":{"type":"String","label":"用户姓名","subject":"USER","role":"name"},
  "orgUnitId":{"type":"Long","label":"组织ID","subject":"ORG_UNIT","role":"id"},
  "orgUnitName":{"type":"String","label":"组织名称","subject":"ORG_UNIT","role":"name"},
  "reason":{"type":"String","label":"原因"}
}' WHERE point_code = 'ORG_MEMBER_LEAVE' AND deleted = 0;

-- 学生: STUDENT_STATUS_CHANGE (学生)
UPDATE trigger_points SET context_schema = '{
  "studentId":{"type":"Long","label":"学生ID","subject":"USER","role":"id"},
  "studentName":{"type":"String","label":"学生姓名","subject":"USER","role":"name"},
  "changeType":{"type":"String","label":"变更类型"},
  "fromStatus":{"type":"String","label":"原状态"},
  "toStatus":{"type":"String","label":"新状态"}
}' WHERE point_code = 'STUDENT_STATUS_CHANGE' AND deleted = 0;

-- 学生: STUDENT_ENROLLED (学生)
UPDATE trigger_points SET context_schema = '{
  "studentId":{"type":"Long","label":"学生ID","subject":"USER","role":"id"},
  "studentName":{"type":"String","label":"学生姓名","subject":"USER","role":"name"},
  "className":{"type":"String","label":"班级"},
  "majorName":{"type":"String","label":"专业"}
}' WHERE point_code = 'STUDENT_ENROLLED' AND deleted = 0;

-- 招生: ENROLLMENT_ADMITTED (无主体ID)
-- 保持不变，无 subject 注解

-- 招生: ENROLLMENT_REGISTERED (学生)
UPDATE trigger_points SET context_schema = '{
  "studentId":{"type":"Long","label":"学生ID","subject":"USER","role":"id"},
  "studentName":{"type":"String","label":"学生姓名","subject":"USER","role":"name"},
  "className":{"type":"String","label":"班级"}
}' WHERE point_code = 'ENROLLMENT_REGISTERED' AND deleted = 0;

-- 教学: SCHEDULE_PUBLISHED, GRADE_PUBLISHED (无主体ID)
-- 保持不变，无 subject 注解

-- 资产: ASSET_CHECK_RESULT (场所)
UPDATE trigger_points SET context_schema = '{
  "assetId":{"type":"Long","label":"资产ID"},
  "assetName":{"type":"String","label":"资产名称"},
  "result":{"type":"String","label":"结果"},
  "placeId":{"type":"Long","label":"场所ID","subject":"PLACE","role":"id"},
  "placeName":{"type":"String","label":"场所名称","subject":"PLACE","role":"name"}
}' WHERE point_code = 'ASSET_CHECK_RESULT' AND deleted = 0;

-- 资产: ASSET_DAMAGE_FOUND (无主体)
-- 保持不变
