# 教学任务全面重构 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 彻底重构教学任务模块：多教师课时分配、教室类型需求关联场所管理、连排要求、状态修复、UI 全面对标正方教务。

**Architecture:** 数据库加列（teaching_tasks 加 room_type_required/consecutive_periods，teaching_task_teachers 加 weekly_hours）；重写 TaskFulfillmentTab UI 为正方风格的任务卡片+教师分配面板；重写后端 enrichWithNames 补全所有字段。

**Tech Stack:** Spring Boot 3.2 + MyBatis Plus / Vue 3 + TypeScript

---

## 现状问题清单

| # | 问题 | 影响 |
|---|------|------|
| 1 | teaching_task_teachers 没有 weekly_hours 字段 | 无法实现"A带2课时B带4课时" |
| 2 | teaching_tasks 没有 room_type_required 字段 | 排课不知道需要什么类型教室 |
| 3 | teaching_tasks 没有 consecutive_periods 字段 | 排课不知道是否需要连排 |
| 4 | TaskStatus 只有 DRAFT/CONFIRMED/CANCELLED | 缺少"已分配教师""已排课"状态 |
| 5 | 前端状态显示"未知" | statusChipClass/getStatusName 映射错误 |
| 6 | 教师分配 UI 是单选下拉 | 不支持多教师+各自课时 |
| 7 | 任务落实 tab 判断教师用 row.teachers 而非 row.teacherName | 已修但不彻底 |

## 场所类型数据（来自 entity_type_configs）

```
TYPE_CLASSROOM    普通教室
TYPE_MULTIMEDIA   多媒体教室
TYPE_COMPUTER_LAB 计算机房
TYPE_LAB          实验室
TYPE_SMART_CLASS  智慧教室
TYPE_TRAINING     实训室
```

---

## Task 1: 数据库改表

**Files:**
- Create: `database/migrations/V20260411_1__teaching_task_enhancements.sql`
- Modify: `database/schema/teaching_academic_complete_schema.sql`

**迁移 SQL:**
```sql
-- 1. teaching_task_teachers 加课时字段
ALTER TABLE teaching_task_teachers
    ADD COLUMN weekly_hours INT NULL COMMENT '该教师承担的周课时数' AFTER teacher_role;

-- 2. teaching_tasks 加教室需求和连排要求
ALTER TABLE teaching_tasks
    ADD COLUMN room_type_required VARCHAR(50) NULL COMMENT '需要的教室类型(关联场所type_code)' AFTER end_week,
    ADD COLUMN consecutive_periods INT DEFAULT 2 COMMENT '连排节数(1=不连排,2=2节连排,3=3节连排,4=4节连排)' AFTER room_type_required,
    ADD COLUMN course_nature TINYINT DEFAULT 1 COMMENT '课程性质(1=理论,2=实验,3=实践,4=理论+实验)' AFTER consecutive_periods;

-- 3. 清空临时数据
DELETE FROM teaching_task_teachers;
DELETE FROM teaching_tasks;
DELETE FROM class_course_assignments;
DELETE FROM semester_course_offerings;
```

同步更新 `teaching_academic_complete_schema.sql` 权威 schema。

执行迁移后验证: `DESCRIBE teaching_tasks;` `DESCRIBE teaching_task_teachers;`

---

## Task 2: 后端领域模型更新

**Files:**
- Modify: `backend/.../domain/teaching/model/task/TeachingTask.java`
- Modify: `backend/.../domain/teaching/model/task/TaskTeacher.java`
- Modify: `backend/.../domain/teaching/model/task/TaskStatus.java`

### TeachingTask.java 加字段:
```java
private String roomTypeRequired;    // 教室类型需求 (关联场所 type_code)
private Integer consecutivePeriods; // 连排节数
private Integer courseNature;       // 课程性质 1=理论 2=实验 3=实践 4=理论+实验
```

更新 `create()` 和 `reconstruct()` 方法签名。
更新 `update()` 加入新字段。

### TaskTeacher.java 加字段:
```java
private Integer weeklyHours; // 该教师承担的周课时数
```

更新 `create()` 签名加 `weeklyHours` 参数。
更新 `reconstruct()` 加入 `weeklyHours`。

### TaskStatus.java 改为:
```java
PENDING(0, "待落实"),
TEACHER_ASSIGNED(1, "已分配教师"),
SCHEDULED(2, "已排课"),
IN_PROGRESS(3, "进行中"),
FINISHED(4, "已结束"),
CANCELLED(9, "已取消");
```

编译验证。

---

## Task 3: 后端 PO + Mapper + RepositoryImpl 更新

**Files:**
- Modify: `backend/.../infrastructure/persistence/teaching/task/TeachingTaskPO.java`
- Modify: `backend/.../infrastructure/persistence/teaching/task/TaskTeacherPO.java`
- Modify: `backend/.../infrastructure/persistence/teaching/task/TeachingTaskRepositoryImpl.java`
- Modify: `backend/.../infrastructure/persistence/teaching/task/TaskTeacherRepositoryImpl.java`

PO 加对应字段。RepositoryImpl 的 toPO/toDomain 映射更新。

编译验证。

---

## Task 4: 后端 Service + Controller 更新

**Files:**
- Modify: `backend/.../application/teaching/TeachingTaskApplicationService.java`
- Modify: `backend/.../interfaces/rest/teaching/TaskController.java`

### enrichWithNames 更新:
已有 teacherName 查询，加：
- `roomTypeRequired` → 查 entity_type_configs 获取类型名称放入 `roomTypeName`
- `consecutivePeriods` 直接返回
- `courseNature` 直接返回
- 教师列表改为返回数组（含各自课时数）:
```java
// 改为返回教师数组
List<Map<String, Object>> teacherList = jdbc.queryForList(
    "SELECT ttt.teacher_id, u.real_name, ttt.teacher_role, ttt.weekly_hours " +
    "FROM teaching_task_teachers ttt " +
    "JOIN users u ON u.id = ttt.teacher_id " +
    "WHERE ttt.task_id = ?", taskId);
record.put("teachers", teacherList);
// 保留 teacherName 兼容
String names = teacherList.stream().map(t -> (String) t.get("real_name")).collect(joining(", "));
record.put("teacherName", names.isEmpty() ? null : names);
```

### assignTeachers 改为支持多教师+课时:
请求体从 `{ teacherIds, mainTeacherId }` 改为:
```json
{
  "teachers": [
    { "teacherId": "xxx", "role": 1, "weeklyHours": 2 },
    { "teacherId": "yyy", "role": 2, "weeklyHours": 4 }
  ]
}
```

### create/update 传入新字段:
`roomTypeRequired`, `consecutivePeriods`, `courseNature`

编译+重启验证。

---

## Task 5: 前端 — TaskFulfillmentTab.vue 彻底重写

**Files:**
- Rewrite: `frontend/src/views/teaching/offering/TaskFulfillmentTab.vue`

### UI 设计（对标正方）

```
┌──────────────────────────────────────────────────────────────────┐
│ 任务落实                                                          │
│ 共 6 条 | 已分配 1/6 | 未排课 6                                    │
│ [从开课计划生成任务] [批量分配教师]                                    │
│──────────────────────────────────────────────────────────────────│
│                                                                  │
│ ┌─ 高等数学 ─────────────────────────────────────────────────┐   │
│ │ 经济2025-1班 | 36人 | 4课时/周 | 1-16周                     │   │
│ │ 教室需求: [多媒体教室▼] | 连排: [2节连排▼] | 性质: [理论▼]   │   │
│ │                                                             │   │
│ │ 教师分配:                                                    │   │
│ │ ┌──────────────────────────────────────────────────────┐    │   │
│ │ │ [选择教师▼ 金洁  ] 主讲 ● | 周课时 [2] | [删除]      │    │   │
│ │ │ [选择教师▼ 李明  ] 辅助 ○ | 周课时 [4] | [删除]      │    │   │
│ │ └──────────────────────────────────────────────────────┘    │   │
│ │ [+ 添加教师]  课时合计: 6/4 ⚠️(超出总课时)                   │   │
│ │                                                 状态: 已分配 │   │
│ └─────────────────────────────────────────────────────────────┘   │
│                                                                  │
│ ┌─ 大学英语 ─────────────────────────────────────────────────┐   │
│ │ 经济2025-1班 | 36人 | 3课时/周 | 1-16周                     │   │
│ │ 教室需求: [普通教室▼] | 连排: [不连排▼] | 性质: [理论▼]      │   │
│ │ 教师分配: 未分配                           [分配教师]         │   │
│ │                                                 状态: 待落实 │   │
│ └─────────────────────────────────────────────────────────────┘   │
│                                                                  │
│ ... (更多任务卡片)                                                │
│                                                                  │
│ ┌────────────────────────────────────────────────────────────┐   │
│ │ 全部已分配教师 (6/6)        [进入排课 →]                     │   │
│ └────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────┘
```

### 核心交互:

**1. 卡片式布局**（非表格）— 每个教学任务一张卡片:
- 顶部: 课程名 + 课程代码
- 信息行: 班级 | 学生数 | 周课时 | 教学周
- 配置行: 教室需求下拉（关联场所类型）| 连排选择 | 课程性质
- 教师区: 多行，每行 = 教师下拉 + 角色(主讲/辅助) + 周课时数 + 删除
- 底部: [+ 添加教师] + 课时合计验证 + 状态标签

**2. 教室需求下拉** — 选项从场所类型配置加载:
```
普通教室 / 多媒体教室 / 计算机房 / 实验室 / 智慧教室 / 实训室
```
调用 `http.get('/entity-type-configs', { params: { entityType: 'PLACE' } })` 获取

**3. 连排要求下拉**:
```
不连排(1节) / 2节连排 / 3节连排 / 4节连排
```

**4. 课程性质下拉**:
```
理论课 / 实验课 / 实践课 / 理论+实验
```

**5. 教师分配区** — 动态行:
- 每行: [选择教师 下拉] + [主讲●/辅助○ 切换] + [周课时 数字输入] + [删除]
- [+ 添加教师] 按钮添加新行
- 课时合计 vs 总周课时：匹配显示绿色✅，不匹配显示橙色⚠️
- 保存时调用新的 assignTeachers API

**6. 状态显示**:
- 待落实（灰色）— 无教师
- 已分配教师（蓝色）— 有教师且课时匹配
- 已排课（绿色）— 排课完成

---

## Task 6: 前端 — 教学任务页（TeachingTaskView）同步更新

**Files:**
- Modify: `frontend/src/views/teaching/TeachingTaskView.vue`

表格加列: 教室需求 | 连排 | 性质
状态列修复: 映射到新的 TaskStatus

---

## Task 7: 前端 API + 类型定义更新

**Files:**
- Modify: `frontend/src/api/teaching.ts`
- Modify: `frontend/src/types/teaching.ts`

TeachingTask 类型加: `roomTypeRequired`, `consecutivePeriods`, `courseNature`, `teachers[]`
assignTeachers API 改为新格式。
加载场所类型 API。

---

## 执行顺序

```
Task 1 (DB改表) → Task 2 (领域模型) → Task 3 (PO/Mapper) → Task 4 (Service/Controller)
                                                                    ↓
Task 7 (前端类型/API) → Task 5 (TaskFulfillmentTab 重写) → Task 6 (TeachingTaskView 更新)
```

Task 1-4 后端链，Task 5-7 前端链。Task 4 完成后两条线可并行。
