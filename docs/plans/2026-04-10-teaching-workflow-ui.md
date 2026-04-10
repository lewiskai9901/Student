# 教务流水线 UI 优化 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 给教务管理添加流水线状态条 + 步骤引导 + 教师内联分配，让操作流程清晰可见。

**Architecture:** 新建 TeachingPipeline.vue 共享组件（流水线状态条），嵌入开课管理和教学任务页面顶部。后端新增 `/teaching/workflow/stats` API 返回各环节完成数。教学任务列表的教师列改为内联下拉选择。

**Tech Stack:** Vue 3 + TypeScript / Spring Boot 3.2

---

## Task 1: 后端 — 流水线统计 API

**Files:**
- Create: `backend/src/main/java/com/school/management/interfaces/rest/teaching/WorkflowStatsController.java`

新增 `GET /teaching/workflow/stats?semesterId=xxx` 返回各环节数量：

```json
{
  "offerings": { "total": 6, "confirmed": 6 },
  "assignments": { "total": 6, "confirmed": 6 },
  "tasks": { "total": 6, "teacherAssigned": 0, "scheduled": 0 },
  "currentStep": "teacher_assign"
}
```

用 JdbcTemplate 直接查 3 张表的 COUNT，不需要 Service 层。

`currentStep` 规则：
- offerings.confirmed < offerings.total → "offering_confirm"
- assignments.total == 0 → "class_assign"
- tasks.total == 0 → "generate_tasks"
- tasks.teacherAssigned < tasks.total → "teacher_assign"
- tasks.scheduled < tasks.total → "scheduling"
- else → "done"

---

## Task 2: 前端 — TeachingPipeline.vue 流水线组件

**Files:**
- Create: `frontend/src/components/teaching/TeachingPipeline.vue`

共享组件，显示流水线状态条。

**Props:**
```typescript
props<{ semesterId: number | string | undefined }>
```

**UI 设计：**
```
开课计划    →    班级分配    →    教学任务    →    分配教师    →    排课
 6/6 ✅          6/6 ✅         6/6 ✅         0/6 ⬤         0/6 ○
```

每个节点：
- 圆圈 + 连接线
- 已完成（绿色✅）/ 当前步骤（蓝色⬤闪烁）/ 未开始（灰色○）
- 下方显示数量 "6/6"
- 点击可跳转到对应页面（用 router.push）

**跳转映射：**
- 开课计划 → `/teaching/offerings` (tab=offerings)
- 班级分配 → `/teaching/offerings` (tab=assignments)
- 教学任务 → `/teaching/tasks`
- 分配教师 → `/teaching/tasks`
- 排课 → `/teaching/scheduling`

**数据加载：**
调用 `GET /teaching/workflow/stats?semesterId=xxx`

样式：横向 flex 布局，圆圈 24px，连接线 2px 高，宽度自适应。用 tm-* 类的颜色体系。

---

## Task 3: 前端 — 开课管理嵌入流水线 + 步骤引导

**Files:**
- Modify: `frontend/src/views/teaching/OfferingManagementView.vue`

**改动：**
1. Header 下方加 `<TeachingPipeline :semester-id="semesterId" />`
2. 每个 tab 内容底部加"下一步"引导按钮：
   - 开课计划 tab：当全部已确认时显示 `[下一步: 班级分配 →]` 按钮，点击切到 assignments tab
   - 班级分配 tab：当已确认且有数据时显示 `[生成教学任务 →]` 按钮（复用已有的绿色按钮），加 `[下一步: 教学任务 →]` 跳转按钮
   - 这些按钮加在各 tab 组件的 emit 事件里

需要给 OfferingListTab 和 ClassAssignmentTab 加 emit:
- `OfferingListTab` emit `allConfirmed` 事件
- `ClassAssignmentTab` emit `tasksGenerated` 事件

---

## Task 4: 前端 — 教学任务页嵌入流水线 + 教师内联分配

**Files:**
- Modify: `frontend/src/views/teaching/TeachingTaskView.vue`

**改动 1：** Header 下方加 `<TeachingPipeline :semester-id="queryParams.semesterId" />`

**改动 2：** 教师列从 "未分配" 文字改为内联下拉选择器：

```html
<td>
  <select v-if="!row.teacherName" class="tm-input" style="width:120px;height:28px;font-size:12px;"
          @change="onTeacherSelect(row, $event)">
    <option value="">选择教师</option>
    <option v-for="t in teachers" :key="t.id" :value="t.id">{{ t.realName }}</option>
  </select>
  <span v-else>{{ row.teacherName }}</span>
</td>
```

选择教师后调用 `teachingTaskApi.assignTeachers(taskId, [teacherId], teacherId)` 并刷新列表。

**需要的数据：** 教师列表。调用用户 API 获取教师角色的用户列表（或直接查 users 表 where role 包含教师）。

**改动 3：** 底部加排课入口引导：
```html
<div v-if="allTeachersAssigned" style="text-align:center;padding:20px;">
  <button class="tm-btn tm-btn-primary" @click="$router.push('/teaching/scheduling')">
    ✅ 全部任务已就绪 — 进入排课 →
  </button>
</div>
```

---

## 执行顺序

```
Task 1 (后端 stats API) → Task 2 (Pipeline 组件) → Task 3 (开课管理集成) → Task 4 (教学任务集成)
```

后端 → 共享组件 → 两个页面集成。
