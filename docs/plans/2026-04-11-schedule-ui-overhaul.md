# 排课中心 UI 重构 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 对标正方教务系统重构排课中心课表网格 UI：连排合并、分段分隔、课程卡片优化、周次翻页。

**Architecture:** 重写 TimetableGrid.vue 的渲染逻辑（用 rowspan 合并连排）；优化 TimetableViewer.vue 的筛选交互。纯前端改动。

**Tech Stack:** Vue 3 + TypeScript + Tailwind CSS

---

## Task 1: TimetableGrid 连排合并 + 分段分隔

**Files:**
- Rewrite: `frontend/src/views/teaching/scheduling/TimetableGrid.vue`

### 核心改动:

**1. 连排课 rowspan 合并**

当前每节课都重复渲染同一个 entry，需要改为：
- 遍历 periods 时，检测当前格子是否被上方的连排课占据（已被 rowspan 覆盖）
- 如果是连排课的起始节次，用 `rowspan` 撑开
- 如果是连排课的中间节次，跳过（不渲染 `<td>`）

**2. 上午/下午/晚上分隔行**

在第4节和第5节之间插入"午休"分隔行，在第8节和第9节之间插入"晚间"分隔行。

**3. 课程卡片样式增强**

- 背景色改为半透明渐变（不再纯白色文字）
- 连排课卡片撑满整个合并格子
- 周次信息小字显示

---

## Task 2: TimetableViewer 周次翻页 + 级联选择

**Files:**
- Modify: `frontend/src/views/teaching/schedule/TimetableViewer.vue`

### 改动:

**1. 周次翻页**

将周次下拉改为 `← 第3周 →` 翻页形式，更直观。

**2. 班级列表改为按系分组**

select 的 option 按系分组（`<optgroup>`），便于找到班级。

---

## 执行顺序

```
Task 1 (Grid 重写) → Task 2 (Viewer 优化)
```
