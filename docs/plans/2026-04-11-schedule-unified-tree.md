# 排课中心统一左树重构 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 排课中心三种课表（班级/教师/教室）统一使用左树导航，教室显示楼号+房间号。

**Architecture:** 创建通用 ScheduleTree 组件，根据 viewType 切换三种树数据源；教室名称从场所树路径拼接楼号；后端 enrichWithNames 返回完整教室路径名。

**Tech Stack:** Vue 3 + TypeScript / Spring Boot

---

## 现状问题

1. 班级课表用左树(DeptTree)，教师/教室课表用下拉 — 风格不统一
2. 教室名只有房间号（"101教室"），两栋楼都有101时无法区分
3. 教师列表从任务数据提取，不够完整

## 数据结构

### 班级树（已有）
```
经济与信息技术系
  2024级
    经济2024-1班
    经济2024-2班
```

### 教师树（新建）
```
经济与信息技术系
  张明
  刘强
  吴刚
汽车工程系
  李华
  陈静
```
数据源: `users` 表 WHERE `user_type_code='TEACHER'`，按 `primary_org_unit_id` 关联系

### 教室树（新建）
```
第一教学楼
  1楼
    一教-101 (50人)
    一教-102 (50人)
  2楼
    一教-201 (50人)
第二教学楼
  1楼
    二教-101 (50人)
```
数据源: `places` 表树形结构，type_code 含 TEACH/CLASSROOM

## 教室命名规则

当前: "101教室" → 歧义
改为: "一教-101" (楼名缩写 + 房间号)

缩写规则: "第一教学楼" → "一教", "第二教学楼" → "二教"
如果楼名不含"第X"则直接用楼名前2字

---

## Task 1: 创建 ScheduleTree 通用组件

**Files:**
- Create: `frontend/src/components/teaching/ScheduleTree.vue`

### 功能:
- Props: `mode: 'class' | 'teacher' | 'classroom'`, `semesterId`
- Emit: `select(node)` — 选中节点时触发
- 三种模式加载不同数据:
  - class: 复用 orgUnitApi.getTree()，过滤 DEPARTMENT/GRADE/CLASS
  - teacher: 从 /users/simple 加载教师，按所属系分组
  - classroom: 从 /places 加载教学场所，按楼/层/室三级树

### 教室节点显示:
- 楼: "第一教学楼"
- 层: "1楼" (可折叠)
- 室: "一教-101 (50人)" ← 楼名缩写+房间号+容量

---

## Task 2: TimetableViewer 切换使用 ScheduleTree

**Files:**
- Modify: `frontend/src/views/teaching/schedule/TimetableViewer.vue`

### 改动:
1. 去掉 DeptTree 引用，换成 ScheduleTree
2. 三种模式都显示左树（不再切换 select/tree）
3. ScheduleTree 的 mode 跟随 viewType
4. 点击树节点 → 设置 targetId + loadTimetable
5. 教师/教室模式下隐藏"对比"按钮（只有班级模式支持对比）

---

## Task 3: 后端 by-classroom API 返回完整教室路径名

**Files:**
- Modify: `backend/.../interfaces/rest/teaching/TeachingScheduleController.java`

### 改动:
所有 schedule_entries 查询的 classroomName 改为:
```sql
-- 当前
p.place_name AS classroomName

-- 改为: 拼接楼名缩写 + 房间号
CONCAT(
  COALESCE(
    (SELECT CASE 
       WHEN pb.place_name LIKE '第一%' THEN '一教'
       WHEN pb.place_name LIKE '第二%' THEN '二教'
       WHEN pb.place_name LIKE '第三%' THEN '三教'
       ELSE LEFT(pb.place_name, 2)
     END
     FROM places pb WHERE pb.id = (
       SELECT pp.parent_id FROM places pp WHERE pp.id = p.parent_id
     )
    ), ''
  ),
  '-', p.place_name
) AS classroomName
```

或更简单: 在 enrichWithNames / by-class 查询时用 JOIN 获取父节点(楼层)的父节点(教学楼)名称拼接。

---

## Task 4: AutoSchedulingService 写入教室时用完整路径名

**Files:**
- Modify: `backend/.../application/teaching/AutoSchedulingService.java`

排课引擎 saveResults 写入 schedule_entries 时，已经存了 classroom_id。
查询时通过 JOIN places 获取路径名即可，不需要改写入逻辑。
但需要确认 by-class/by-teacher/by-classroom 三个查询都 JOIN 了 places 获取完整名称。

---

## 执行顺序

```
Task 1 (ScheduleTree组件) → Task 2 (TimetableViewer集成)
                                      ↓
Task 3 (后端教室路径名) → Task 4 (验证三个维度查询)
```

Task 1-2 前端链，Task 3-4 后端链，可并行。
