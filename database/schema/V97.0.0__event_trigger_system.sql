-- ============================================================
-- V97.0.0 Event Trigger System
-- trigger_points + event_triggers + ALTER existing tables + seed data
-- ============================================================

-- 1. CREATE TABLE trigger_points
CREATE TABLE IF NOT EXISTS trigger_points (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    module_code VARCHAR(30) NOT NULL COMMENT '模块编码',
    module_name VARCHAR(50) NOT NULL COMMENT '模块名称',
    point_code VARCHAR(50) NOT NULL COMMENT '触发点编码(全局唯一)',
    point_name VARCHAR(100) NOT NULL COMMENT '触发点名称',
    description VARCHAR(500) COMMENT '说明',
    context_schema JSON COMMENT '上下文字段定义',
    is_enabled TINYINT DEFAULT 1,
    sort_order INT DEFAULT 0,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_point_code (point_code, tenant_id),
    INDEX idx_module (module_code),
    INDEX idx_tenant (tenant_id)
) COMMENT='触发点注册表';

-- 2. CREATE TABLE event_triggers
CREATE TABLE IF NOT EXISTS event_triggers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '触发器名称',
    trigger_point_code VARCHAR(50) NOT NULL COMMENT '关联触发点编码',
    condition_json JSON COMMENT '触发条件',
    event_type_mode VARCHAR(10) NOT NULL DEFAULT 'FIXED' COMMENT 'FIXED=固定类型/DYNAMIC=从context取',
    event_type_code VARCHAR(50) COMMENT '固定事件类型编码(FIXED模式)',
    event_type_source VARCHAR(50) COMMENT 'context字段名(DYNAMIC模式)',
    subject_type VARCHAR(20) DEFAULT 'USER' COMMENT '主体类型(USER/ORG_UNIT/PLACE)',
    subject_source VARCHAR(50) NOT NULL COMMENT '主体ID取自context哪个字段',
    subject_name_source VARCHAR(50) COMMENT '主体名称取自context哪个字段',
    related_sources JSON COMMENT '关联实体[{source,nameSource,type}]',
    payload_fields JSON COMMENT '从context中提取哪些字段存入事件payload',
    description VARCHAR(500),
    is_enabled TINYINT DEFAULT 1,
    sort_order INT DEFAULT 0,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_point (trigger_point_code),
    INDEX idx_tenant (tenant_id)
) COMMENT='事件触发器配置表';

-- 3. ALTER entity_event_types ADD category_polarity
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'entity_event_types' AND column_name = 'category_polarity');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE entity_event_types ADD COLUMN category_polarity VARCHAR(10) DEFAULT ''NEUTRAL'' COMMENT ''大类极性: POSITIVE/NEGATIVE/NEUTRAL''',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. ALTER msg_notifications ADD event_id
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'msg_notifications' AND column_name = 'event_id');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE msg_notifications ADD COLUMN event_id BIGINT COMMENT ''触发此通知的事件ID''',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5. ALTER insp_template_items ADD linked_event_type_code
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'insp_template_items' AND column_name = 'linked_event_type_code');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE insp_template_items ADD COLUMN linked_event_type_code VARCHAR(50) COMMENT ''关联事件类型编码(检查扣分时作为eventTypeHint传入触发器)''',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 6. ALTER insp_grade_definitions ADD linked_event_type_code
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'insp_grade_definitions' AND column_name = 'linked_event_type_code');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE insp_grade_definitions ADD COLUMN linked_event_type_code VARCHAR(50) COMMENT ''关联事件类型编码(达到此等级时触发)''',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- 7. INSERT trigger_points seed data (17 rows for 7 modules)
-- ============================================================
INSERT IGNORE INTO trigger_points (module_code, module_name, point_code, point_name, description, context_schema, tenant_id) VALUES
-- 检查平台
('inspection', '检查平台', 'INSP_ITEM_RESULT', '检查项结果提交', '每个被扣分的检查项触发一次', '{"studentId":{"type":"Long","label":"学生ID"},"studentName":{"type":"String","label":"学生姓名"},"itemName":{"type":"String","label":"检查项名称"},"result":{"type":"String","label":"结果"},"score":{"type":"Number","label":"分数"},"eventTypeHint":{"type":"String","label":"事件类型(动态)"},"classId":{"type":"Long","label":"班级ID"},"className":{"type":"String","label":"班级名称"},"placeId":{"type":"Long","label":"场所ID"},"placeName":{"type":"String","label":"场所名称"}}', 1),
('inspection', '检查平台', 'INSP_GRADE_RESULT', '检查等级结果', '检查完成后的最终等级', '{"targetId":{"type":"Long","label":"目标ID"},"targetName":{"type":"String","label":"目标名称"},"targetType":{"type":"String","label":"目标类型"},"score":{"type":"Number","label":"得分"},"grade":{"type":"String","label":"等级"},"gradeName":{"type":"String","label":"等级名称"}}', 1),
('inspection', '检查平台', 'INSP_RECORD_COMPLETE', '检查记录完成', '整个检查提交完成', '{"taskId":{"type":"Long","label":"任务ID"},"targetId":{"type":"Long","label":"目标ID"},"targetName":{"type":"String","label":"目标名称"},"score":{"type":"Number","label":"总分"},"inspectorName":{"type":"String","label":"检查员"}}', 1),
-- 考勤模块
('attendance', '考勤管理', 'ATTENDANCE_RECORDED', '考勤记录', '每条异常考勤触发', '{"studentId":{"type":"Long","label":"学生ID"},"studentName":{"type":"String","label":"学生姓名"},"classId":{"type":"Long","label":"班级ID"},"className":{"type":"String","label":"班级名称"},"status":{"type":"Number","label":"状态(2迟到3早退5旷课)"},"statusName":{"type":"String","label":"状态名称"},"date":{"type":"String","label":"日期"},"courseName":{"type":"String","label":"课程"}}', 1),
('attendance', '考勤管理', 'LEAVE_REQUEST_SUBMITTED', '请假申请', '学生提交请假申请', '{"studentId":{"type":"Long","label":"学生ID"},"studentName":{"type":"String","label":"学生姓名"},"leaveType":{"type":"String","label":"请假类型"},"startDate":{"type":"String","label":"开始"},"endDate":{"type":"String","label":"结束"}}', 1),
-- 宿舍模块
('dormitory', '宿舍管理', 'DORM_CHECKIN', '入住', '学生入住宿舍', '{"occupantId":{"type":"Long","label":"入住人ID"},"occupantName":{"type":"String","label":"入住人"},"placeId":{"type":"Long","label":"房间ID"},"placeName":{"type":"String","label":"房间"}}', 1),
('dormitory', '宿舍管理', 'DORM_CHECKOUT', '退宿', '学生退出宿舍', '{"occupantId":{"type":"Long","label":"退宿人ID"},"occupantName":{"type":"String","label":"退宿人"},"placeId":{"type":"Long","label":"房间ID"},"placeName":{"type":"String","label":"房间"},"reason":{"type":"String","label":"原因"}}', 1),
-- 组织模块
('organization', '组织管理', 'ORG_MEMBER_JOIN', '人员加入', '人员加入组织', '{"userId":{"type":"Long","label":"用户ID"},"userName":{"type":"String","label":"用户姓名"},"orgUnitId":{"type":"Long","label":"组织ID"},"orgUnitName":{"type":"String","label":"组织名称"}}', 1),
('organization', '组织管理', 'ORG_MEMBER_LEAVE', '人员离开', '人员离开组织', '{"userId":{"type":"Long","label":"用户ID"},"userName":{"type":"String","label":"用户姓名"},"orgUnitId":{"type":"Long","label":"组织ID"},"orgUnitName":{"type":"String","label":"组织名称"},"reason":{"type":"String","label":"原因"}}', 1),
-- 学生模块
('student', '学生管理', 'STUDENT_STATUS_CHANGE', '学籍变更', '学生状态变更', '{"studentId":{"type":"Long","label":"学生ID"},"studentName":{"type":"String","label":"学生姓名"},"changeType":{"type":"String","label":"变更类型"},"fromStatus":{"type":"String","label":"原状态"},"toStatus":{"type":"String","label":"新状态"}}', 1),
('student', '学生管理', 'STUDENT_ENROLLED', '学生入学', '新生入学', '{"studentId":{"type":"Long","label":"学生ID"},"studentName":{"type":"String","label":"学生姓名"},"className":{"type":"String","label":"班级"},"majorName":{"type":"String","label":"专业"}}', 1),
-- 招生模块
('enrollment', '招生管理', 'ENROLLMENT_ADMITTED', '录取', '学生被录取', '{"applicantName":{"type":"String","label":"姓名"},"majorName":{"type":"String","label":"专业"},"directionName":{"type":"String","label":"方向"}}', 1),
('enrollment', '招生管理', 'ENROLLMENT_REGISTERED', '报到注册', '学生报到', '{"studentId":{"type":"Long","label":"学生ID"},"studentName":{"type":"String","label":"学生姓名"},"className":{"type":"String","label":"班级"}}', 1),
-- 教学模块
('teaching', '教学管理', 'SCHEDULE_PUBLISHED', '课表发布', '课表发布', '{"semesterName":{"type":"String","label":"学期"},"publishedBy":{"type":"String","label":"发布人"}}', 1),
('teaching', '教学管理', 'GRADE_PUBLISHED', '成绩发布', '成绩批次发布', '{"batchName":{"type":"String","label":"批次"},"courseName":{"type":"String","label":"课程"},"className":{"type":"String","label":"班级"}}', 1),
-- 资产模块
('asset', '资产管理', 'ASSET_CHECK_RESULT', '资产巡检结果', '资产巡检', '{"assetId":{"type":"Long","label":"资产ID"},"assetName":{"type":"String","label":"资产名称"},"result":{"type":"String","label":"结果"},"placeId":{"type":"Long","label":"场所ID"}}', 1),
('asset', '资产管理', 'ASSET_DAMAGE_FOUND', '资产损坏', '发现资产损坏', '{"assetId":{"type":"Long","label":"资产ID"},"assetName":{"type":"String","label":"资产名称"},"description":{"type":"String","label":"损坏描述"}}', 1);

-- ============================================================
-- 8. INSERT entity_event_types seed data (32 rows, 6 categories with polarity)
-- ============================================================
DELETE FROM entity_event_types WHERE tenant_id = 1;

INSERT INTO entity_event_types (tenant_id, category_code, category_name, category_polarity, type_code, type_name, icon, color, applicable_subjects, is_system, sort_order) VALUES
-- 违纪 (NEGATIVE)
(1, 'DISCIPLINE', '违纪', 'NEGATIVE', 'LATE', '迟到', 'Clock', '#ef4444', 'USER', 1, 1),
(1, 'DISCIPLINE', '违纪', 'NEGATIVE', 'ABSENCE', '旷课', 'UserX', '#ef4444', 'USER', 1, 2),
(1, 'DISCIPLINE', '违纪', 'NEGATIVE', 'EARLY_LEAVE', '早退', 'LogOut', '#ef4444', 'USER', 1, 3),
(1, 'DISCIPLINE', '违纪', 'NEGATIVE', 'CONTRABAND', '违禁品', 'Ban', '#ef4444', 'USER', 1, 4),
(1, 'DISCIPLINE', '违纪', 'NEGATIVE', 'DORM_VIOLATION', '宿舍违纪', 'Bed', '#ef4444', 'USER,PLACE', 1, 5),
(1, 'DISCIPLINE', '违纪', 'NEGATIVE', 'HYGIENE_VIOLATION', '卫生违纪', 'Trash2', '#ef4444', 'USER,PLACE', 1, 6),
(1, 'DISCIPLINE', '违纪', 'NEGATIVE', 'SAFETY_VIOLATION', '安全违纪', 'ShieldAlert', '#ef4444', 'USER,PLACE', 1, 7),
(1, 'DISCIPLINE', '违纪', 'NEGATIVE', 'CLASS_VIOLATION', '课堂违纪', 'BookX', '#ef4444', 'USER', 1, 8),
(1, 'DISCIPLINE', '违纪', 'NEGATIVE', 'FIGHT', '打架斗殴', 'Swords', '#ef4444', 'USER', 1, 9),
-- 奖励 (POSITIVE)
(1, 'AWARD', '奖励', 'POSITIVE', 'HONOR', '获奖', 'Trophy', '#22c55e', 'USER,ORG_UNIT', 1, 10),
(1, 'AWARD', '奖励', 'POSITIVE', 'PRAISE', '表扬', 'ThumbsUp', '#22c55e', 'USER', 1, 11),
(1, 'AWARD', '奖励', 'POSITIVE', 'EXEMPLARY', '先进个人', 'Star', '#22c55e', 'USER', 1, 12),
(1, 'AWARD', '奖励', 'POSITIVE', 'EXEMPLARY_ORG', '先进集体', 'Award', '#22c55e', 'ORG_UNIT', 1, 13),
(1, 'AWARD', '奖励', 'POSITIVE', 'FULL_ATTENDANCE', '全勤奖', 'CheckCircle', '#22c55e', 'USER', 1, 14),
(1, 'AWARD', '奖励', 'POSITIVE', 'DISCIPLINE_EXCELLENT', '纪律优秀', 'Shield', '#22c55e', 'ORG_UNIT', 1, 15),
-- 学业 (NEUTRAL)
(1, 'ACADEMIC', '学业', 'NEUTRAL', 'EXAM_EXCELLENT', '考试优秀', 'GraduationCap', '#22c55e', 'USER', 1, 16),
(1, 'ACADEMIC', '学业', 'NEUTRAL', 'EXAM_FAIL', '考试不及格', 'AlertTriangle', '#ef4444', 'USER', 1, 17),
(1, 'ACADEMIC', '学业', 'NEUTRAL', 'CREDIT_WARNING', '学分预警', 'AlertCircle', '#f59e0b', 'USER', 1, 18),
-- 人员变动 (NEUTRAL)
(1, 'PERSONNEL', '人员变动', 'NEUTRAL', 'MEMBER_JOIN', '人员加入', 'UserPlus', '#6b7280', 'USER,ORG_UNIT', 1, 19),
(1, 'PERSONNEL', '人员变动', 'NEUTRAL', 'MEMBER_LEAVE', '人员离开', 'UserMinus', '#6b7280', 'USER,ORG_UNIT', 1, 20),
(1, 'PERSONNEL', '人员变动', 'NEUTRAL', 'TRANSFER', '调动', 'ArrowRightLeft', '#6b7280', 'USER', 1, 21),
(1, 'PERSONNEL', '人员变动', 'NEUTRAL', 'CHECKIN', '入住', 'Home', '#6b7280', 'USER,PLACE', 1, 22),
(1, 'PERSONNEL', '人员变动', 'NEUTRAL', 'CHECKOUT', '退宿', 'DoorOpen', '#6b7280', 'USER,PLACE', 1, 23),
(1, 'PERSONNEL', '人员变动', 'NEUTRAL', 'ENROLLED', '入学', 'School', '#6b7280', 'USER', 1, 24),
-- 检查 (NEUTRAL)
(1, 'INSPECTION', '检查', 'NEUTRAL', 'INSP_PASS', '检查合格', 'CheckSquare', '#22c55e', 'PLACE,ORG_UNIT', 1, 25),
(1, 'INSPECTION', '检查', 'NEUTRAL', 'INSP_FAIL', '检查不合格', 'XSquare', '#ef4444', 'PLACE,ORG_UNIT', 1, 26),
(1, 'INSPECTION', '检查', 'NEUTRAL', 'DISCIPLINE_FAIL', '纪律不合格', 'ShieldX', '#ef4444', 'ORG_UNIT', 1, 27),
(1, 'INSPECTION', '检查', 'NEUTRAL', 'INSP_RECTIFIED', '整改完成', 'CheckCircle2', '#3b82f6', 'PLACE,ORG_UNIT', 1, 28),
-- 通知 (NEUTRAL)
(1, 'NOTIFICATION', '通知', 'NEUTRAL', 'ANNOUNCEMENT', '公告', 'Megaphone', '#3b82f6', 'ORG_UNIT', 1, 29),
(1, 'NOTIFICATION', '通知', 'NEUTRAL', 'SCHEDULE_CHANGE', '课表变更', 'Calendar', '#3b82f6', 'ORG_UNIT', 1, 30),
(1, 'NOTIFICATION', '通知', 'NEUTRAL', 'GRADE_RELEASE', '成绩发布', 'FileText', '#3b82f6', 'ORG_UNIT', 1, 31),
(1, 'NOTIFICATION', '通知', 'NEUTRAL', 'SYSTEM_NOTICE', '系统通知', 'Bell', '#6b7280', 'USER,ORG_UNIT,PLACE', 0, 32);

-- ============================================================
-- 9. INSERT event_triggers default triggers (4 rows)
-- ============================================================
INSERT IGNORE INTO event_triggers (name, trigger_point_code, condition_json, event_type_mode, event_type_code, event_type_source, subject_type, subject_source, subject_name_source, description, tenant_id) VALUES
('检查扣分→学生事件', 'INSP_ITEM_RESULT', '{"result":"扣分"}', 'DYNAMIC', NULL, 'eventTypeHint', 'USER', 'studentId', 'studentName', '检查项扣分时根据检查项关联的事件类型生成学生事件', 1),
('检查优秀→班级表扬', 'INSP_GRADE_RESULT', '{"grade":{"$in":["A"]}}', 'FIXED', 'DISCIPLINE_EXCELLENT', NULL, 'ORG_UNIT', 'targetId', 'targetName', '检查等级为A时给目标(班级)记录纪律优秀', 1),
('检查不合格→班级警告', 'INSP_GRADE_RESULT', '{"grade":{"$in":["D"]}}', 'FIXED', 'DISCIPLINE_FAIL', NULL, 'ORG_UNIT', 'targetId', 'targetName', '检查等级为D时给目标(班级)记录纪律不合格', 1),
('考勤异常→学生事件', 'ATTENDANCE_RECORDED', '{"status":{"$in":[2,3,5]}}', 'DYNAMIC', NULL, 'eventTypeHint', 'USER', 'studentId', 'studentName', '迟到/早退/旷课时根据状态生成对应事件', 1);
