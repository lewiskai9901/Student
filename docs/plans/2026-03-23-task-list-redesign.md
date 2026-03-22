# 任务列表重构计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将任务列表从纯列表形式重构为横向时间轴视图，按天分组，显示领取人信息和协作模式。

**Architecture:** 前端纯 UI 重构，不改后端。TaskListView 重写为时间轴布局：顶部日期选择器（横向滚动），主区域显示选中日期的任务卡片。每张卡片包含任务编号、状态、检查员信息、协作模式、进度。

**Tech Stack:** Vue 3 Composition API, scoped CSS (无 Tailwind/Element Plus)

---

### Task 1: 重写 TaskListView

**Files:**
- Rewrite: `frontend/src/views/inspection/v7/tasks/TaskListView.vue`

**核心改动：**

1. **日期时间轴**：横向滚动的日期 pill 条，点击筛选当天任务，支持"全部"
2. **任务卡片**：每个任务一张卡片，显示：
   - 任务编号 + 状态标签
   - 检查员头像/姓名（谁领取的）
   - 协作模式标签（单评/协作）
   - 进度条 + 完成数/总数
   - 操作按钮
3. **Tab 切换**：全部 / 我的 / 可领取
4. **项目分组**：按项目折叠分组（保留）

**InspTask 类型需添加字段（前端类型）：**
- `collaborationMode` 已存在于后端但前端类型缺失

**Step 1: 更新前端类型定义**
在 `InspTask` 中添加 `collaborationMode` 字段

**Step 2: 重写 TaskListView**
完整重写，包含时间轴 + 卡片布局

**Step 3: 验证** `npm run build` 或 HMR 热更新
