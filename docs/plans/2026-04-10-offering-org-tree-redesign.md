# 开课管理左树右表重构 + 教学任务落实 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 给开课管理和教学任务页面加左侧组织树筛选（系部→年级→班级），并在开课管理中新增"教学任务落实"tab（课程+班级+教师一步确定）。

**Architecture:** 新建 DeptTree.vue 共享组件（系部/年级树），嵌入开课管理和教学任务页面左侧。开课管理 tab 从 3 个变为 3 个（开课计划 / 任务落实 / 教学班），"任务落实" tab 合并班级分配+教师分配+生成任务为一体化操作。

**Tech Stack:** Vue 3 + TypeScript

---

## 现状

- 组织树 API 已有: `orgUnitApi.getTree()`
- 组织层级: 学校 → 系部(4个) → 年级 → 班级(18个)
- 开课管理: 3 tabs（开课计划/班级分配/教学班）— 班级分配 tab 功能弱
- 教学任务: 平铺列表，无院系筛选

## Task 1: DeptTree.vue 共享组件

**Files:**
- Create: `frontend/src/components/teaching/DeptTree.vue`

左侧系部/年级/班级树，可复用于开课管理和教学任务页。

**Props:**
```typescript
props<{
  semesterId?: number | string
}>()
```

**Emits:**
```typescript
emit('select', { nodeType: 'department' | 'grade' | 'class', nodeId, nodeName, path })
```

**数据加载:** 调用 `orgUnitApi.getTree()` 获取完整组织树，过滤出 SCHOOL→DEPARTMENT→GRADE→CLASS 层级。

**UI:** 简洁的缩进列表，点击节点高亮，emit 选中事件。样式参考现有 tm-* 体系。

宽度固定 200px，带折叠按钮。

---

## Task 2: 开课管理重构 — 左树右表布局

**Files:**
- Modify: `frontend/src/views/teaching/OfferingManagementView.vue`

**改动:**
1. 整体布局改为 flex 水平：左侧 DeptTree + 右侧内容区
2. tabs 改为: 开课计划 / 任务落实 / 教学班
3. "班级分配" tab 改名为 "任务落实"
4. 选中左树节点后，右侧列表按 departmentId 或 gradeId 筛选
5. 保留顶部学期选择器和流水线

**布局结构:**
```html
<div style="display: flex; height: 100%;">
  <DeptTree @select="onTreeSelect" />
  <div style="flex: 1; overflow-y: auto;">
    <!-- header + pipeline + tabs + content -->
  </div>
</div>
```

传递选中节点给各 tab 组件作为过滤条件。

---

## Task 3: 教学任务落实 tab（核心）

**Files:**
- Create: `frontend/src/views/teaching/offering/TaskFulfillmentTab.vue`
- Modify: `frontend/src/views/teaching/OfferingManagementView.vue` (替换 ClassAssignmentTab)

**这是新的核心 tab，合并"班级分配+教师分配+任务生成"为一体化视图。**

**UI 设计:**
```
┌─────────────────────────────────────────────────────┐
│ 任务落实 — 经济信息管理系 / 2025级                     │
│ [从开课计划导入] [批量分配教师]                         │
│─────────────────────────────────────────────────────│
│ 课程    班级       学生  教师        周课时 状态  操作  │
│ 高等数学 经济25-1  36   [选择教师▼]  4    待分配  删除 │
│ 高等数学 经济25-2  38   [选择教师▼]  4    待分配  删除 │
│ 大学英语 经济25-1  36   [李老师]     3    已分配  删除 │
│ ...                                                 │
│─────────────────────────────────────────────────────│
│ 共 12 条 | 已分配教师 1/12 | [提交排课→]              │
└─────────────────────────────────────────────────────┘
```

**数据来源:** 教学任务表 (teaching_tasks)，按选中的系部/年级过滤

**关键功能:**
1. "从开课计划导入" — 调用 generate-tasks API，为选中范围的班级生成任务
2. 教师列 — 内联下拉选择器，选择后立即调 assignTeachers API
3. 状态实时显示 — 待分配/已分配/已排课
4. 底部进度 + "提交排课→" 引导

**教师数据:** 调用用户 API 获取教师列表（角色为教师的用户）

---

## Task 4: 教学任务页面加左树筛选

**Files:**
- Modify: `frontend/src/views/teaching/TeachingTaskView.vue`

**改动:**
1. 布局改为 flex: 左侧 DeptTree + 右侧现有内容
2. 选中左树节点后，查询参数加 orgUnitId 过滤
3. 保留现有的表格和筛选功能

改动最小 — 只是外面包一层左树。

---

## 执行顺序

```
Task 1 (DeptTree组件) → Task 2 (开课管理左树布局) → Task 3 (任务落实tab) → Task 4 (教学任务左树)
```
