# 校历表 V2：weekType + 工具栏标记模式 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 给校历表加周类型自动推算，重写 UI 为工具栏标记模式（点击/拖选日期直接标记事件）。

**Architecture:** 后端 CalendarWeekDTO 加 weekType 计算字段。前端 CalendarGrid.vue 完全重写：顶部工具栏选标记模式+事件名→点击单元格标记→调 API 创建/删除事件→刷新 grid。

**Tech Stack:** Spring Boot 3.2 + MyBatis Plus / Vue 3 + TypeScript

---

## Task 1: 后端 — CalendarWeekDTO 加 weekType

**Files:**
- Modify: `backend/src/main/java/com/school/management/application/calendar/query/CalendarWeekDTO.java`
- Modify: `backend/src/main/java/com/school/management/application/calendar/CalendarApplicationService.java`
- Modify: `frontend/src/types/teaching.ts`

**改动：**

1. CalendarWeekDTO 加字段 `private String weekType; // TEACHING / EXAM / VACATION`

2. CalendarApplicationService.buildCalendarGrid() 中，每个 week 构建完 days 后，根据工作日（周一~周五）的 dayType 分布推算 weekType：
   - 工作日全是 HOLIDAY → "VACATION"
   - 工作日全是 EXAM → "EXAM"
   - 其余 → "TEACHING"

3. frontend types/teaching.ts CalendarWeek 加 `weekType?: 'TEACHING' | 'EXAM' | 'VACATION'`

---

## Task 2: 前端 — CalendarGrid.vue 完全重写

**Files:**
- Rewrite: `frontend/src/views/teaching/calendar-new/CalendarGrid.vue`

**核心设计：**

### 工具栏区域
```
[统计] 教学88天 | 假期24天 | 补课2天 | 考试5天
[标记] 模式: ○查看  ●假期  ○补课  ○考试  ○清除   事件名: [________]  按周几: [选择]
[图例] □教学日  ■休息日  ■假期  ■补课  ■考试
```

- `markMode`: 'view' | 'holiday' | 'makeup' | 'exam' | 'clear'
- `eventName`: string — 标记时的事件名称
- `followWeekday`: number — 补课模式时按周几课表
- 选择非 view 模式时工具栏高亮，格子变为可点击

### 网格区域
- 周次列：显示周号 + 周类型标签（教/考/假）
- 单元格：日期 + 事件名（如有）+ 补课标注
- 点击单元格：
  - view 模式：无操作
  - holiday/exam 模式：调 API 创建事件（startDate=endDate=该日期, affectType 对应模式, eventName 取工具栏输入）
  - makeup 模式：同上 + substituteWeekday
  - clear 模式：查找该日期的事件，调 delete API
- 点击后自动刷新 grid

### API 调用
- 标记：`academicEventApi.create({ semesterId, yearId, eventName, eventType, startDate, endDate, affectType, substituteWeekday })`
- 清除：需要先知道日期上的事件 ID → 后端 CalendarDayDTO 加 `eventId` 字段
- 刷新：`semesterApi.getCalendarGrid(semesterId)`

### 需要后端配合
- CalendarDayDTO 加 `private Long eventId;` — 有事件覆盖时返回事件 ID，用于清除操作
