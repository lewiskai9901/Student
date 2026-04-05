# 事件触发器系统完整设计方案 v2

> 通用底座：事件存储 + 触发器引擎 + 通知路由
> 行业模块：只需注册触发点 + 埋一行调用
> UI风格：参考CourseListView.vue的课程目录设计

---

## 一、数据模型

### 1.1 新增表：trigger_points（触发点注册表）

```sql
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
```

context_schema示例：
```json
{
  "studentId": {"type":"Long","label":"学生ID"},
  "studentName": {"type":"String","label":"学生姓名"},
  "result": {"type":"String","label":"结果"},
  "score": {"type":"Number","label":"分数"},
  "eventTypeHint": {"type":"String","label":"事件类型提示(动态)"},
  "classId": {"type":"Long","label":"班级ID"},
  "className": {"type":"String","label":"班级名称"}
}
```

### 1.2 新增表：event_triggers（触发器配置表）

```sql
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
```

event_type_mode 说明：
- FIXED: event_type_code 填固定值（如 "DISCIPLINE_EXCELLENT"）
- DYNAMIC: event_type_source 填context字段名（如 "eventTypeHint"），运行时从context取值

condition_json 支持的操作：
```json
{"result": "扣分"}                    -- 精确匹配
{"grade": {"$in": ["A","B"]}}         -- 多值匹配
{"score": {"$lt": 60}}                -- 小于
{"score": {"$gt": 80}}                -- 大于
{"score": {"$lte": 60}}               -- 小于等于
{"score": {"$gte": 80}}               -- 大于等于
{"result": {"$ne": "正常"}}            -- 不等于
null 或 {}                            -- 无条件，始终触发
```

### 1.3 修改表：entity_event_types

```sql
ALTER TABLE entity_event_types
    ADD COLUMN IF NOT EXISTS category_polarity VARCHAR(10) DEFAULT 'NEUTRAL'
    COMMENT '大类极性: POSITIVE/NEGATIVE/NEUTRAL';
```

### 1.4 修改表：msg_notifications

```sql
ALTER TABLE msg_notifications
    ADD COLUMN IF NOT EXISTS event_id BIGINT COMMENT '触发此通知的事件ID';
```

### 1.5 修改表：insp_template_items

```sql
ALTER TABLE insp_template_items
    ADD COLUMN IF NOT EXISTS linked_event_type_code VARCHAR(50)
    COMMENT '关联事件类型编码(检查扣分时作为eventTypeHint传入触发器)';
```

### 1.6 修改表：insp_grade_definitions

```sql
ALTER TABLE insp_grade_definitions
    ADD COLUMN IF NOT EXISTS linked_event_type_code VARCHAR(50)
    COMMENT '关联事件类型编码(达到此等级时触发)';
```

---

## 二、种子数据

### 2.1 触发点预置（17个，覆盖7个模块）

```sql
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
```

### 2.2 事件类型种子数据（教育行业，6分类30子类型）

```sql
-- 先清理旧数据
DELETE FROM entity_event_types WHERE tenant_id = 1;

-- 违纪 (NEGATIVE)
INSERT INTO entity_event_types (tenant_id, category_code, category_name, category_polarity, type_code, type_name, icon, color, applicable_subjects, is_system, sort_order) VALUES
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

-- 学业 (NEUTRAL，部分子类型有覆盖极性)
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
```

### 2.3 默认触发器预置

```sql
INSERT IGNORE INTO event_triggers (name, trigger_point_code, condition_json, event_type_mode, event_type_code, event_type_source, subject_type, subject_source, subject_name_source, description, tenant_id) VALUES
('检查扣分→学生事件', 'INSP_ITEM_RESULT', '{"result":"扣分"}', 'DYNAMIC', NULL, 'eventTypeHint', 'USER', 'studentId', 'studentName', '检查项扣分时根据检查项关联的事件类型生成学生事件', 1),
('检查优秀→班级表扬', 'INSP_GRADE_RESULT', '{"grade":{"$in":["A"]}}', 'FIXED', 'DISCIPLINE_EXCELLENT', NULL, 'ORG_UNIT', 'targetId', 'targetName', '检查等级为A时给目标(班级)记录纪律优秀', 1),
('检查不合格→班级警告', 'INSP_GRADE_RESULT', '{"grade":{"$in":["D"]}}', 'FIXED', 'DISCIPLINE_FAIL', NULL, 'ORG_UNIT', 'targetId', 'targetName', '检查等级为D时给目标(班级)记录纪律不合格', 1),
('考勤异常→学生事件', 'ATTENDANCE_RECORDED', '{"status":{"$in":[2,3,5]}}', 'DYNAMIC', NULL, 'eventTypeHint', 'USER', 'studentId', 'studentName', '迟到/早退/旷课时根据状态生成对应事件', 1);
```

---

## 三、后端实现

### 3.1 核心引擎

#### TriggerService.java
```
位置: backend/src/main/java/com/school/management/application/event/TriggerService.java

@Service @Transactional @Slf4j
public class TriggerService {
    private final JdbcTemplate jdbcTemplate;
    private final EntityEventApplicationService eventService;

    public void fire(String pointCode, Map<String, Object> context) {
        // 1. 查找匹配的触发器
        List<Map<String,Object>> triggers = jdbcTemplate.queryForList(
            "SELECT * FROM event_triggers WHERE trigger_point_code=? AND is_enabled=1 AND deleted=0",
            pointCode);
        
        // 2. 对每个触发器评估条件+生成事件
        for (Map<String,Object> trigger : triggers) {
            try {
                String condJson = (String) trigger.get("condition_json");
                if (!ConditionMatcher.matches(condJson, context)) continue;
                
                // 确定事件类型
                String eventType;
                if ("DYNAMIC".equals(trigger.get("event_type_mode"))) {
                    eventType = String.valueOf(context.get((String) trigger.get("event_type_source")));
                } else {
                    eventType = (String) trigger.get("event_type_code");
                }
                if (eventType == null || eventType.equals("null")) continue;
                
                // 提取主体
                String subjectType = (String) trigger.get("subject_type");
                Long subjectId = toLong(context.get((String) trigger.get("subject_source")));
                String subjectName = String.valueOf(context.getOrDefault(
                    (String) trigger.get("subject_name_source"), ""));
                if (subjectId == null) continue;
                
                // 查事件类型的分类
                String category = jdbcTemplate.queryForObject(
                    "SELECT category_code FROM entity_event_types WHERE type_code=? AND deleted=0 LIMIT 1",
                    String.class, eventType);
                
                // 创建事件
                eventService.createEvent(
                    subjectType, subjectId, subjectName,
                    eventType, getEventLabel(eventType),
                    toJson(context), // payload
                    getModuleFromPoint(pointCode),
                    null, null, null,
                    getCurrentUserId(), getCurrentUserName()
                );
            } catch (Exception e) {
                log.warn("触发器执行失败 trigger={}: {}", trigger.get("name"), e.getMessage());
                // 不影响业务
            }
        }
    }
}
```

#### ConditionMatcher.java
```
位置: backend/src/main/java/com/school/management/application/event/ConditionMatcher.java

public class ConditionMatcher {
    public static boolean matches(String conditionJson, Map<String, Object> context) {
        if (conditionJson == null || conditionJson.isBlank() || "{}".equals(conditionJson.trim())) 
            return true; // 无条件=始终匹配
        
        Map<String,Object> conditions = parseJson(conditionJson);
        for (Map.Entry<String,Object> entry : conditions.entrySet()) {
            String field = entry.getKey();
            Object expected = entry.getValue();
            Object actual = context.get(field);
            
            if (expected instanceof Map) {
                // 操作符匹配: $in, $ne, $lt, $gt, $lte, $gte
                if (!matchOperator((Map)expected, actual)) return false;
            } else {
                // 精确匹配
                if (!String.valueOf(expected).equals(String.valueOf(actual))) return false;
            }
        }
        return true;
    }
    
    private static boolean matchOperator(Map<String,Object> operators, Object actual) {
        for (Map.Entry<String,Object> op : operators.entrySet()) {
            switch (op.getKey()) {
                case "$in": return ((List)op.getValue()).contains(toComparable(actual));
                case "$ne": return !String.valueOf(op.getValue()).equals(String.valueOf(actual));
                case "$lt": return toDouble(actual) < toDouble(op.getValue());
                case "$gt": return toDouble(actual) > toDouble(op.getValue());
                case "$lte": return toDouble(actual) <= toDouble(op.getValue());
                case "$gte": return toDouble(actual) >= toDouble(op.getValue());
            }
        }
        return true;
    }
}
```

### 3.2 Controller

#### TriggerPointController.java
```
位置: interfaces/rest/event/TriggerPointController.java
路径: /event/trigger-points

GET    /                         列表（可按module筛选）
GET    /{id}                     详情
POST   /                         创建
PUT    /{id}                     更新
DELETE /{id}                     删除
PUT    /{id}/enable              启用
PUT    /{id}/disable             禁用
```

#### EventTriggerController.java
```
位置: interfaces/rest/event/EventTriggerController.java
路径: /event/triggers

GET    /                         列表（可按pointCode/eventType筛选）
GET    /{id}                     详情
POST   /                         创建
PUT    /{id}                     更新
DELETE /{id}                     删除
PUT    /{id}/enable              启用
PUT    /{id}/disable             禁用
POST   /test                     测试（传入模拟context，返回匹配结果）
```

#### EntityEventTypeController.java（已有，增强）
```
位置: interfaces/rest/event/EntityEventTypeController.java
路径: /event/types

GET    /                         列表（按分类分组）
GET    /categories               分类列表（含极性）
POST   /                         创建类型
PUT    /{id}                     更新
DELETE /{id}                     删除
POST   /categories               创建分类
```

#### EntityEventController.java（已有，增强）
```
位置: interfaces/rest/event/EntityEventController.java

GET    /by-subject               按主体查事件流（type+id+polarity筛选）
GET    /statistics               统计（按主体/分类/极性聚合）
```

### 3.3 各模块埋点（7个模块，精确到文件和方法）

```
1. 检查平台 — InspSubmissionApplicationService.java
   方法: 提交完成后(completeSubmission或onSubmissionCompleted)
   代码: 遍历details，有linked_event_type_code且score<0的fire("INSP_ITEM_RESULT")
         最终等级fire("INSP_GRADE_RESULT")

2. 考勤 — AttendanceController.java  
   方法: batchRecord
   代码: status!=1时fire("ATTENDANCE_RECORDED", {eventTypeHint: mapStatusToEventType(status)})

3. 宿舍 — UniversalPlaceApplicationService.java
   方法: checkIn/checkOut
   代码: fire("DORM_CHECKIN")/fire("DORM_CHECKOUT")

4. 学生 — StudentApplicationService.java
   方法: changeStatus/enrollStudent
   代码: fire("STUDENT_STATUS_CHANGE")/fire("STUDENT_ENROLLED")

5. 招生 — EnrollmentController.java
   方法: admit/register
   代码: fire("ENROLLMENT_ADMITTED")/fire("ENROLLMENT_REGISTERED")

6. 教学 — TeachingScheduleController.java + GradeController.java
   方法: publish
   代码: fire("SCHEDULE_PUBLISHED")/fire("GRADE_PUBLISHED")

7. 组织 — OrgUnitApplicationService.java
   方法: 人员关系变更处
   代码: fire("ORG_MEMBER_JOIN")/fire("ORG_MEMBER_LEAVE")
```

---

## 四、前端实现

### 4.1 Types

```typescript
// types/event.ts

export interface TriggerPoint {
  id: number
  moduleCode: string
  moduleName: string
  pointCode: string
  pointName: string
  description?: string
  contextSchema?: Record<string, {type: string; label: string}>
  isEnabled: boolean
}

export interface EventTrigger {
  id: number
  name: string
  triggerPointCode: string
  triggerPointName?: string
  moduleName?: string
  conditionJson?: Record<string, any>
  eventTypeMode: 'FIXED' | 'DYNAMIC'
  eventTypeCode?: string
  eventTypeName?: string
  eventTypeSource?: string
  subjectType: string
  subjectSource: string
  subjectNameSource?: string
  relatedSources?: {source: string; nameSource: string; type: string}[]
  payloadFields?: string[]
  description?: string
  isEnabled: boolean
}

export interface EventType {
  id: number
  categoryCode: string
  categoryName: string
  categoryPolarity: 'POSITIVE' | 'NEGATIVE' | 'NEUTRAL'
  typeCode: string
  typeName: string
  icon?: string
  color?: string
  applicableSubjects?: string
  isSystem: boolean
}

export interface EntityEvent {
  id: number
  subjectType: string
  subjectId: number
  subjectName: string
  eventCategory: string
  eventType: string
  eventLabel: string
  payload?: Record<string, any>
  categoryPolarity?: string
  typeName?: string
  typeIcon?: string
  typeColor?: string
  occurredAt: string
}

export const POLARITY_CONFIG = {
  POSITIVE: { label: '正向', color: '#22c55e', bgColor: '#f0fdf4', icon: '🟢' },
  NEGATIVE: { label: '负向', color: '#ef4444', bgColor: '#fef2f2', icon: '🔴' },
  NEUTRAL:  { label: '中性', color: '#6b7280', bgColor: '#f9fafb', icon: '⚪' },
} as const

export const SUBJECT_TYPES = [
  { value: 'USER', label: '用户' },
  { value: 'ORG_UNIT', label: '组织' },
  { value: 'PLACE', label: '场所' },
] as const

export const CONDITION_OPERATORS = [
  { value: '$eq', label: '等于' },
  { value: '$ne', label: '不等于' },
  { value: '$in', label: '包含于' },
  { value: '$lt', label: '小于' },
  { value: '$gt', label: '大于' },
  { value: '$lte', label: '小于等于' },
  { value: '$gte', label: '大于等于' },
] as const
```

### 4.2 API

```typescript
// api/event.ts

export const triggerPointApi = {
  list: (params?: any) => http.get('/event/trigger-points', { params }),
  create: (data: any) => http.post('/event/trigger-points', data),
  update: (id: any, data: any) => http.put(`/event/trigger-points/${id}`, data),
  delete: (id: any) => http.delete(`/event/trigger-points/${id}`),
  enable: (id: any) => http.put(`/event/trigger-points/${id}/enable`),
  disable: (id: any) => http.put(`/event/trigger-points/${id}/disable`),
}

export const eventTriggerApi = {
  list: (params?: any) => http.get('/event/triggers', { params }),
  create: (data: any) => http.post('/event/triggers', data),
  update: (id: any, data: any) => http.put(`/event/triggers/${id}`, data),
  delete: (id: any) => http.delete(`/event/triggers/${id}`),
  enable: (id: any) => http.put(`/event/triggers/${id}/enable`),
  disable: (id: any) => http.put(`/event/triggers/${id}/disable`),
  test: (data: any) => http.post('/event/triggers/test', data),
}

export const eventTypeApi = {
  list: () => http.get('/event/types'),
  categories: () => http.get('/event/types/categories'),
  create: (data: any) => http.post('/event/types', data),
  update: (id: any, data: any) => http.put(`/event/types/${id}`, data),
  delete: (id: any) => http.delete(`/event/types/${id}`),
  createCategory: (data: any) => http.post('/event/types/categories', data),
}

export const entityEventApi = {
  bySubject: (params: {subjectType: string; subjectId: number; polarity?: string; page?: number; size?: number}) =>
    http.get('/event/events/by-subject', { params }),
  statistics: (params?: any) => http.get('/event/events/statistics', { params }),
}
```

### 4.3 前端页面（4个）

所有页面UI风格参考CourseListView.vue：
- 顶部header用 `.page-title` + `.stats-row` + `.btn-create`
- 筛选栏用 `.filter-bar` + `.search-box`
- 表格用原生table + Tailwind样式
- 对话框用Teleport + Transition自定义modal
- 颜色：蓝色主色、绿色正向、红色负向、灰色中性

#### 页面1: EventTypeManagementView.vue
```
路径: /system/event-types
文件: frontend/src/views/system/EventTypeManagementView.vue
预计: 500-700行

内容:
  header: "事件类型管理" + 统计(分类数/类型数/正向/负向/中性) + [新增分类]按钮
  主体: 按分类折叠展示，每个分类是一个card
    分类card头部: 分类名 + 极性badge(🟢🔴⚪) + 子类型数量 + [编辑分类][添加子类型]
    card内部: 子类型列表，每行显示 icon + 类型名 + 编码 + 适用主体tags + [编辑][删除]
  对话框: 创建/编辑分类(name+polarity)、创建/编辑类型(code+name+icon+color+subjects)
```

#### 页面2: EventTriggerView.vue
```
路径: /system/event-triggers
文件: frontend/src/views/system/EventTriggerView.vue
预计: 600-800行

内容:
  header: "事件触发器" + 统计(总数/启用/禁用) + [新建触发器]按钮
  筛选: 模块选择 + 事件类型选择 + 搜索
  表格: 名称 | 触发点(模块+触发点名) | 条件摘要 | 事件类型(带极性色) | 主体类型 | 状态 | 操作

  新建/编辑对话框(核心):
    第一区: 触发来源
      模块下拉 → 触发点下拉（联动）
      选择触发点后显示"可用上下文字段"列表(从contextSchema读取)
    
    第二区: 触发条件
      条件编辑器: [字段选择▼] [操作符▼] [值输入] [+添加条件]
      字段选择从contextSchema动态加载
      
    第三区: 生成事件
      事件类型模式: ○固定 ○动态(从context取)
      固定: 事件类型下拉（显示极性色标）
      动态: context字段选择
      主体类型: USER/ORG_UNIT/PLACE
      主体ID来源: context字段选择
      主体名称来源: context字段选择
    
    第四区: 关联实体(可选，可添加多个)
      [+ 添加关联] → 类型 + ID来源 + 名称来源
```

#### 页面3: EntityEventTimelineComponent.vue
```
文件: frontend/src/components/event/EntityEventTimeline.vue
预计: 300-400行

这是一个可嵌入的组件，不是独立页面
嵌入到: 学生详情、组织详情、场所详情的Tab中

Props:
  subjectType: 'USER' | 'ORG_UNIT' | 'PLACE'
  subjectId: number

内容:
  筛选栏: [全部极性▼] [全部分类▼]  共X条
  统计: 🟢正向 X  🔴负向 X  ⚪中性 X

  时间线(按月分组):
    2025-12
    ├── 12-20  🟢 编程竞赛一等奖      [奖励/获奖]     ▸详情
    ├── 12-15  ⚪ 转宿舍(101→201)    [人员/调动]     ▸详情
    2025-11
    ├── 11-20  🔴 宿舍卫生不合格      [违纪/卫生]     ▸详情
    └── 11-05  🔴 迟到              [违纪/迟到]     ▸详情

  点击▸详情展开payload JSON的友好显示
```

#### 页面4: 检查模板编辑增强
```
文件: frontend/src/views/inspection/v7/templates/ 中的检查项编辑组件
改动: 检查项表单加一个"事件关联"下拉框（从eventTypeApi.list()加载）
      等级定义编辑加一个"事件关联"下拉框

这不是新页面，是对已有检查模板编辑页面的增强
```

### 4.4 路由

```typescript
// 在 /system 下新增
{ path: '/system/event-types', name: 'EventTypes', component: () => import('@/views/system/EventTypeManagementView.vue'), meta: { title: '事件类型', order: 15 } },
{ path: '/system/event-triggers', name: 'EventTriggers', component: () => import('@/views/system/EventTriggerView.vue'), meta: { title: '事件触发器', order: 16 } },
```

---

## 五、实施步骤（10步）

```
Step 1:  数据库迁移（新表 + 字段修改 + 种子数据）
Step 2:  后端 TriggerService + ConditionMatcher
Step 3:  后端 TriggerPointController + EventTriggerController
Step 4:  后端 EntityEventTypeController 增强（加category_polarity支持）
Step 5:  后端 EntityEventController 增强（by-subject + statistics）
Step 6:  7个模块埋点 triggerService.fire()
Step 7:  前端 types/event.ts + api/event.ts
Step 8:  前端 EventTypeManagementView.vue
Step 9:  前端 EventTriggerView.vue + EntityEventTimeline.vue
Step 10: 前端路由注册 + 检查模板关联增强
```

---

## 六、完整性验证清单（34项）

### 数据库（6项）
- [ ] trigger_points 表创建成功
- [ ] event_triggers 表创建成功（含event_type_mode/event_type_source字段）
- [ ] entity_event_types.category_polarity 字段存在
- [ ] msg_notifications.event_id 字段存在
- [ ] insp_template_items.linked_event_type_code 字段存在
- [ ] insp_grade_definitions.linked_event_type_code 字段存在

### 种子数据（4项）
- [ ] trigger_points 有17条预置数据（7个模块）
- [ ] entity_event_types 有30+条（6分类，含极性）
- [ ] event_triggers 有4条默认触发器
- [ ] msg_templates + msg_subscription_rules 有数据

### 后端引擎（5项）
- [ ] TriggerService.fire() 正常执行
- [ ] ConditionMatcher 支持7种操作：精确/$in/$ne/$lt/$gt/$lte/$gte
- [ ] FIXED模式：从event_type_code取固定事件类型
- [ ] DYNAMIC模式：从context的event_type_source字段取动态事件类型
- [ ] fire()失败不影响业务（try/catch）

### 后端API（6项）
- [ ] GET/POST/PUT/DELETE /event/trigger-points 全部可用
- [ ] GET/POST/PUT/DELETE /event/triggers 全部可用
- [ ] POST /event/triggers/test 测试端点可用
- [ ] GET /event/types + /event/types/categories 返回含polarity的数据
- [ ] GET /event/events/by-subject 按主体查事件流
- [ ] GET /event/events/statistics 统计接口

### 模块埋点（7项）
- [ ] 检查平台：提交检查后fire INSP_ITEM_RESULT + INSP_GRADE_RESULT
- [ ] 考勤模块：异常考勤fire ATTENDANCE_RECORDED
- [ ] 宿舍模块：入住/退宿fire DORM_CHECKIN/CHECKOUT
- [ ] 学生模块：状态变更/入学fire STUDENT_STATUS_CHANGE/ENROLLED
- [ ] 招生模块：录取/报到fire ENROLLMENT_ADMITTED/REGISTERED
- [ ] 教学模块：发布fire SCHEDULE_PUBLISHED/GRADE_PUBLISHED
- [ ] 组织模块：人员变动fire ORG_MEMBER_JOIN/LEAVE

### 前端（6项）
- [ ] 事件类型管理页面：分类CRUD + 子类型CRUD + 极性显示
- [ ] 触发器配置页面：列表 + 新建/编辑对话框（动态contextSchema）
- [ ] 条件编辑器：字段+操作符+值的可视化编辑
- [ ] 事件时间线组件：时间分组 + 极性色标 + 分类筛选
- [ ] 路由注册：/system/event-types + /system/event-triggers
- [ ] 检查模板编辑：检查项+等级定义加事件类型关联下拉
```
