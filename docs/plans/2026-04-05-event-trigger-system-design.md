# 事件触发器系统完整设计方案

> 通用底座：事件存储 + 触发器引擎 + 通知路由
> 行业模块：只需注册触发点 + 埋一行调用

---

## 一、数据模型

### 1.1 新增表：trigger_points（触发点注册表）

各模块声明自己有哪些动作可以触发事件。

```sql
CREATE TABLE IF NOT EXISTS trigger_points (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    module_code VARCHAR(30) NOT NULL COMMENT '模块编码(inspection/attendance/dormitory...)',
    module_name VARCHAR(50) NOT NULL COMMENT '模块名称',
    point_code VARCHAR(50) NOT NULL COMMENT '触发点编码(全局唯一)',
    point_name VARCHAR(100) NOT NULL COMMENT '触发点名称',
    description VARCHAR(500) COMMENT '说明',
    context_schema JSON COMMENT '上下文字段定义(字段名→类型+说明)',
    is_enabled TINYINT DEFAULT 1,
    sort_order INT DEFAULT 0,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_point_code (point_code, tenant_id),
    INDEX idx_module (module_code)
) COMMENT='触发点注册表';
```

context_schema示例：
```json
{
  "inspectedPersonId": {"type": "Long", "label": "被检查人ID"},
  "inspectedPersonName": {"type": "String", "label": "被检查人姓名"},
  "result": {"type": "String", "label": "检查结果"},
  "score": {"type": "Number", "label": "分数"},
  "placeId": {"type": "Long", "label": "场所ID"},
  "placeName": {"type": "String", "label": "场所名称"}
}
```

### 1.2 新增表：event_triggers（触发器配置表）

管理员配置：什么条件下触发什么事件。

```sql
CREATE TABLE IF NOT EXISTS event_triggers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '触发器名称',
    trigger_point_code VARCHAR(50) NOT NULL COMMENT '关联触发点编码',
    condition_json JSON COMMENT '触发条件(字段名→期望值)',
    event_type_code VARCHAR(50) NOT NULL COMMENT '要生成的事件类型编码',
    subject_source VARCHAR(50) NOT NULL COMMENT '事件主体ID取自上下文哪个字段',
    subject_name_source VARCHAR(50) COMMENT '事件主体名称取自上下文哪个字段',
    subject_type VARCHAR(20) DEFAULT 'USER' COMMENT '主体类型(USER/ORG_UNIT/PLACE)',
    related_sources JSON COMMENT '关联实体配置[{source,nameSource,type}]',
    description VARCHAR(500) COMMENT '说明',
    is_enabled TINYINT DEFAULT 1,
    sort_order INT DEFAULT 0,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_point (trigger_point_code),
    INDEX idx_event_type (event_type_code),
    INDEX idx_tenant (tenant_id)
) COMMENT='事件触发器配置表';
```

condition_json示例：
```json
{"result": "不合格"}           -- 精确匹配
{"score": {"$lt": 60}}         -- 数值比较（小于60）
{"result": {"$in": ["迟到","旷课"]}}  -- 多值匹配
```

related_sources示例：
```json
[
  {"source": "placeId", "nameSource": "placeName", "type": "PLACE"},
  {"source": "classId", "nameSource": "className", "type": "ORG_UNIT"}
]
```

### 1.3 修改表：entity_event_types

```sql
ALTER TABLE entity_event_types ADD COLUMN IF NOT EXISTS category_polarity VARCHAR(10) DEFAULT 'NEUTRAL'
    COMMENT '大类极性: POSITIVE/NEGATIVE/NEUTRAL';
```

### 1.4 修改表：msg_notifications

```sql
ALTER TABLE msg_notifications ADD COLUMN IF NOT EXISTS event_id BIGINT COMMENT '触发此通知的事件ID';
```

---

## 二、后端核心引擎

### 2.1 TriggerService（触发器引擎）

```
位置: application/event/TriggerService.java

核心方法:
  void fire(String pointCode, Map<String, Object> context)

执行流程:
  1. 查询 event_triggers WHERE trigger_point_code = pointCode AND is_enabled = 1
  2. 对每个触发器：
     a. 评估 condition_json 与 context 是否匹配
     b. 匹配则从 context 中提取 subject_id/subject_name
     c. 调用 entityEventApplicationService.createEvent(...) 创建事件
     d. 事件创建后自动触发 MessageDispatcher（已有机制）
```

### 2.2 ConditionMatcher（条件匹配器）

```
位置: application/event/ConditionMatcher.java

支持的条件操作:
  精确匹配: {"field": "value"}          → context.get("field").equals("value")
  多值匹配: {"field": {"$in": [...]}}   → list.contains(context.get("field"))
  不等于:   {"field": {"$ne": "value"}} → !equals
  数值比较: {"field": {"$lt": 60}}      → number < 60
            {"field": {"$gt": 80}}      → number > 80
            {"field": {"$lte": 60}}     → number <= 60
            {"field": {"$gte": 80}}     → number >= 80
  空条件:   null 或 {}                  → 始终匹配（无条件触发）
```

### 2.3 后端Controller

```
位置: interfaces/rest/event/

TriggerPointController.java (/event/trigger-points):
  GET    /                    列表（可按module筛选）
  GET    /{id}                详情
  POST   /                   创建（通常由迁移SQL预置）
  PUT    /{id}                更新
  DELETE /{id}                删除
  PUT    /{id}/enable         启用
  PUT    /{id}/disable        禁用

EventTriggerController.java (/event/triggers):
  GET    /                    列表（可按pointCode/eventType筛选）
  GET    /{id}                详情
  POST   /                   创建
  PUT    /{id}                更新
  DELETE /{id}                删除
  PUT    /{id}/enable         启用
  PUT    /{id}/disable        禁用
  POST   /test                测试触发（传入模拟context，返回会触发哪些事件）

EntityEventTypeController.java (/event/types) — 已有，补充:
  确保CRUD完整
  确保返回 category_polarity

EntityEventController.java (/event/events) — 已有，补充:
  GET /by-subject?type=USER&id=123  → 查某人的事件流
  GET /by-subject?type=PLACE&id=456 → 查某场所的事件流
  GET /statistics?subjectType=USER&polarity=NEGATIVE → 统计
```

---

## 三、各模块触发点预置数据

### 3.1 检查平台

```
INSP_ITEM_RESULT         检查项结果提交
  context: inspectedPersonId, inspectedPersonName, itemName, result, score, placeId, placeName, inspectorId, inspectorName

INSP_RECORD_COMPLETE     检查记录完成
  context: targetType, targetId, targetName, totalScore, grade, inspectorId, inspectorName

INSP_CORRECTIVE_CREATED  整改任务创建
  context: caseId, caseName, assigneeId, assigneeName, deadline
```

### 3.2 考勤模块

```
ATTENDANCE_RECORDED      考勤记录
  context: studentId, studentName, classId, className, courseId, courseName, status(1出勤/2迟到/3早退/4请假/5旷课), date, period

LEAVE_REQUEST_SUBMITTED  请假申请提交
  context: studentId, studentName, leaveType, startDate, endDate, reason
```

### 3.3 宿舍模块

```
DORM_CHECKIN             入住
  context: occupantId, occupantName, placeId, placeName, positionNo

DORM_CHECKOUT            退宿
  context: occupantId, occupantName, placeId, placeName, reason
```

### 3.4 组织模块

```
ORG_MEMBER_JOIN          人员加入
  context: userId, userName, orgUnitId, orgUnitName, role

ORG_MEMBER_LEAVE         人员离开
  context: userId, userName, orgUnitId, orgUnitName, reason
```

### 3.5 学生模块

```
STUDENT_STATUS_CHANGE    学籍变更
  context: studentId, studentName, changeType, fromStatus, toStatus, classId, className, reason

STUDENT_ENROLLED         学生入学
  context: studentId, studentName, majorName, directionName, className
```

### 3.6 招生模块

```
ENROLLMENT_ADMITTED      录取
  context: applicantName, majorName, directionName

ENROLLMENT_REGISTERED    报到注册
  context: studentId, studentName, className, majorName
```

### 3.7 教学模块

```
SCHEDULE_PUBLISHED       课表发布
  context: semesterId, semesterName, publishedBy

GRADE_PUBLISHED          成绩发布
  context: batchId, batchName, courseId, courseName, classId, className
```

---

## 四、事件类型预置数据（教育行业）

```
分类: DISCIPLINE (违纪) — polarity=NEGATIVE
  LATE              迟到
  ABSENCE           旷课
  EARLY_LEAVE       早退
  CONTRABAND        违禁品
  DORM_VIOLATION    宿舍违纪
  HYGIENE_VIOLATION 卫生违纪
  SAFETY_VIOLATION  安全违纪
  CLASS_VIOLATION   课堂违纪
  FIGHT             打架斗殴

分类: AWARD (奖励) — polarity=POSITIVE
  HONOR             获奖
  PRAISE            表扬
  EXEMPLARY         先进个人
  EXEMPLARY_ORG     先进集体
  FULL_ATTENDANCE   全勤奖

分类: ACADEMIC (学业) — polarity=NEUTRAL
  EXAM_EXCELLENT    考试优秀 (polarity覆盖=POSITIVE)
  EXAM_FAIL         考试不及格 (polarity覆盖=NEGATIVE)
  CREDIT_WARNING    学分预警 (polarity覆盖=NEGATIVE)

分类: PERSONNEL (人员变动) — polarity=NEUTRAL
  MEMBER_JOIN       人员加入
  MEMBER_LEAVE      人员离开
  TRANSFER          调动
  CHECKIN           入住
  CHECKOUT          退宿

分类: INSPECTION (检查) — polarity=NEUTRAL
  INSP_PASS         检查合格 (polarity覆盖=POSITIVE)
  INSP_FAIL         检查不合格 (polarity覆盖=NEGATIVE)
  INSP_RECTIFIED    整改完成

分类: NOTIFICATION (通知) — polarity=NEUTRAL
  ANNOUNCEMENT      公告
  SCHEDULE_CHANGE   课表变更
  GRADE_RELEASE     成绩发布
  SYSTEM_NOTICE     系统通知
```

---

## 五、前端UI设计

### 5.1 事件类型管理页面 (/system/event-types)

```
┌─────────────────────────────────────────────────────────────┐
│ 事件类型管理                                     [+ 新增分类] │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌─ 违纪 (DISCIPLINE) ── 极性:🔴负向 ── 9个子类型 ────────┐ │
│  │                                                          │ │
│  │  🔴 迟到(LATE)  🔴 旷课(ABSENCE)  🔴 早退(EARLY_LEAVE) │ │
│  │  🔴 违禁品      🔴 宿舍违纪       🔴 卫生违纪           │ │
│  │  🔴 安全违纪    🔴 课堂违纪       🔴 打架斗殴           │ │
│  │                                           [+ 添加子类型] │ │
│  └──────────────────────────────────────────────────────────┘ │
│                                                               │
│  ┌─ 奖励 (AWARD) ── 极性:🟢正向 ── 5个子类型 ─────────────┐ │
│  │  🟢 获奖  🟢 表扬  🟢 先进个人  🟢 先进集体  🟢 全勤奖 │ │
│  └──────────────────────────────────────────────────────────┘ │
│                                                               │
│  ┌─ 人员变动 (PERSONNEL) ── 极性:⚪中性 ── 5个子类型 ──────┐ │
│  │  ...                                                      │ │
│  └──────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### 5.2 触发器配置页面 (/system/event-triggers)

```
┌─────────────────────────────────────────────────────────────┐
│ 事件触发器                                     [+ 新建触发器] │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  统计: 触发器 12个 | 已启用 10 | 已禁用 2                     │
│                                                               │
│  筛选: [全部模块▼] [全部事件类型▼]  🔍搜索                    │
│                                                               │
│  ┌──────┬──────────┬──────────┬────────┬──────┬──────┬──────┐│
│  │ 名称  │ 触发点    │ 条件      │ 事件类型 │ 主体  │ 状态  │ 操作 ││
│  ├──────┼──────────┼──────────┼────────┼──────┼──────┼──────┤│
│  │迟到记录│检查项结果 │result=不合格│🔴迟到  │被检查人│✅启用 │编辑  ││
│  │旷课记录│考勤记录   │status=5   │🔴旷课  │学生   │✅启用 │编辑  ││
│  │入住通知│宿舍入住   │(无条件)   │⚪入住  │入住人  │✅启用 │编辑  ││
│  └──────┴──────────┴──────────┴────────┴──────┴──────┴──────┘│
└─────────────────────────────────────────────────────────────┘

新建/编辑触发器对话框:
┌───────────────────────────────────────────────────┐
│ 新建触发器                                         │
│                                                     │
│  触发器名称  [迟到记录_________________]             │
│                                                     │
│  ┌─ 触发来源 ────────────────────────────────────┐ │
│  │ 模块:    [检查平台 ▼]                          │ │
│  │ 触发点:  [检查项结果提交 ▼]                     │ │
│  │                                                │ │
│  │ 可用上下文字段:                                 │ │
│  │   inspectedPersonId(被检查人ID)                │ │
│  │   inspectedPersonName(被检查人姓名)             │ │
│  │   result(检查结果)                              │ │
│  │   score(分数)                                   │ │
│  │   placeId(场所ID) placeName(场所名称)          │ │
│  └────────────────────────────────────────────────┘ │
│                                                     │
│  ┌─ 触发条件 ────────────────────────────────────┐ │
│  │ [result ▼] [等于 ▼] [不合格________]   [+条件] │ │
│  │                                                │ │
│  │ 空条件 = 每次都触发                             │ │
│  └────────────────────────────────────────────────┘ │
│                                                     │
│  ┌─ 生成事件 ────────────────────────────────────┐ │
│  │ 事件类型:    [🔴 迟到(LATE) ▼]                 │ │
│  │ 主体类型:    [用户 ▼]                          │ │
│  │ 主体ID来源:  [inspectedPersonId ▼]             │ │
│  │ 主体名称来源: [inspectedPersonName ▼]           │ │
│  └────────────────────────────────────────────────┘ │
│                                                     │
│  ┌─ 关联实体(可选) ──────────────────────────────┐ │
│  │ [+ 添加关联]                                   │ │
│  │ 类型: [场所▼] ID来源: [placeId▼] 名称: [placeName▼] │ │
│  └────────────────────────────────────────────────┘ │
│                                                     │
│                          [取消]  [保存]              │
└───────────────────────────────────────────────────┘
```

### 5.3 实体事件流页面（通用组件）

在学生详情、组织详情、场所详情页面中嵌入的"事件时间线"Tab：

```
┌─ 张三的事件记录 ───────────────────────────────────┐
│                                                      │
│  筛选: [全部极性▼] [全部分类▼]  共28条               │
│                                                      │
│  2025-12                                             │
│  ├── 12-20  🟢 编程竞赛一等奖         [奖励/获奖]    │
│  ├── 12-15  ⚪ 转宿舍(101→201)       [人员/调动]    │
│  │                                                   │
│  2025-11                                             │
│  ├── 11-20  🔴 宿舍卫生不合格         [违纪/卫生]    │
│  ├── 11-05  🔴 迟到                  [违纪/迟到]    │
│  │                                                   │
│  2025-10                                             │
│  ├── 10-15  🟢 获得优秀学生称号       [奖励/表扬]    │
│  │                                                   │
│  2025-09                                             │
│  ├── 09-01  ⚪ 入学报到               [人员/入学]    │
│                                                      │
│  统计: 🟢正向 3  🔴负向 2  ⚪中性 2                  │
└──────────────────────────────────────────────────────┘
```

### 5.4 路由结构

```
/system/event-types       事件类型管理
/system/event-triggers    触发器配置
（已有）/messages          消息通知列表
（已有）/messages/config   订阅规则+模板配置
```

---

## 六、各模块埋点代码清单

每个模块需要改动的具体文件和位置：

### 6.1 检查平台
```
文件: application/inspection/v7/InspSubmissionApplicationService.java
位置: 提交检查结果后
代码: triggerService.fire("INSP_ITEM_RESULT", Map.of(
        "inspectedPersonId", submission.getTargetId(),
        "inspectedPersonName", submission.getTargetName(),
        "result", detail.getResult(),
        "score", detail.getScore(),
        "placeId", submission.getPlaceId(),
        "placeName", submission.getPlaceName()
      ))

文件: application/inspection/v7/InspProjectApplicationService.java
位置: 检查项目完成后
代码: triggerService.fire("INSP_RECORD_COMPLETE", Map.of(...))
```

### 6.2 考勤模块
```
文件: interfaces/rest/student/AttendanceController.java
位置: batchRecord方法，每条考勤保存后
代码: if (status != 1) triggerService.fire("ATTENDANCE_RECORDED", Map.of(
        "studentId", studentId, "studentName", studentName,
        "status", status, "date", dateStr
      ))
```

### 6.3 宿舍模块
```
文件: application/place/UniversalPlaceApplicationService.java
位置: checkIn/checkOut方法后
代码: triggerService.fire("DORM_CHECKIN", Map.of(...))
      triggerService.fire("DORM_CHECKOUT", Map.of(...))
```

### 6.4 学生模块
```
文件: application/student/StudentApplicationService.java
位置: changeStatus/transferClass/enrollStudent后
代码: triggerService.fire("STUDENT_STATUS_CHANGE", Map.of(...))
      triggerService.fire("STUDENT_ENROLLED", Map.of(...))
```

### 6.5 招生模块
```
文件: interfaces/rest/student/EnrollmentController.java
位置: admit/register方法后
代码: triggerService.fire("ENROLLMENT_ADMITTED", Map.of(...))
      triggerService.fire("ENROLLMENT_REGISTERED", Map.of(...))
```

### 6.6 教学模块
```
文件: interfaces/rest/teaching/TeachingScheduleController.java
位置: publish方法后
代码: triggerService.fire("SCHEDULE_PUBLISHED", Map.of(...))

文件: interfaces/rest/teaching/GradeController.java
位置: publishBatch方法后
代码: triggerService.fire("GRADE_PUBLISHED", Map.of(...))
```

### 6.7 组织模块
```
文件: application/organization/OrgUnitApplicationService.java
位置: 人员关系变更后
代码: triggerService.fire("ORG_MEMBER_JOIN", Map.of(...))
      triggerService.fire("ORG_MEMBER_LEAVE", Map.of(...))
```

---

## 七、完整性验证清单

实施完成后必须通过的验证：

### 7.1 数据库验证
- [ ] trigger_points 表创建，含预置数据（17个触发点）
- [ ] event_triggers 表创建
- [ ] entity_event_types 有 category_polarity 字段
- [ ] entity_event_types 含教育行业种子数据（6分类30+子类型）
- [ ] msg_notifications 有 event_id 字段

### 7.2 后端验证
- [ ] TriggerService.fire() 方法可正常执行
- [ ] ConditionMatcher 支持 精确/多值/$lt/$gt/$ne 五种条件
- [ ] 触发器CRUD API全部可用（trigger-points + triggers）
- [ ] 事件类型CRUD API可用（含polarity）
- [ ] 事件流查询API可用（by-subject + statistics）
- [ ] 7个模块全部埋入 triggerService.fire() 调用
- [ ] fire()调用在try/catch中，不影响原业务

### 7.3 前端验证
- [ ] 事件类型管理页面：分类CRUD + 子类型CRUD + 极性显示
- [ ] 触发器配置页面：列表+筛选+新建/编辑对话框+启用禁用
- [ ] 对话框动态显示触发点的上下文字段（context_schema）
- [ ] 条件编辑器：字段选择+操作符选择+值输入
- [ ] 事件流组件：可嵌入学生详情/组织详情/场所详情
- [ ] 路由注册：/system/event-types + /system/event-triggers

### 7.4 端到端验证
- [ ] 创建事件类型"迟到"(LATE) → 类型列表显示
- [ ] 创建触发器"检查不合格→迟到" → 触发器列表显示
- [ ] 执行检查（标记不合格）→ entity_events自动产生记录
- [ ] 学生事件流页面显示"迟到"记录
- [ ] 消息中心收到通知（如配了订阅规则）
- [ ] 创建考勤记录（旷课）→ 事件自动产生
- [ ] 宿舍入住 → 事件自动产生

---

## 八、实施顺序

```
Step 1: 数据库迁移（表+种子数据）
Step 2: 后端 TriggerService + ConditionMatcher 引擎
Step 3: 后端 Controller（trigger-points + triggers + event-types增强 + events增强）
Step 4: 7个模块埋点（每个模块1-3行代码）
Step 5: 前端 types + API
Step 6: 前端 事件类型管理页面
Step 7: 前端 触发器配置页面
Step 8: 前端 事件流时间线组件
Step 9: 路由注册
Step 10: 端到端测试全部验证项
```
