# 教务系统 UI 彻底重构方案

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将教务管理（开课/排课/教学任务）合并为统一的流水线式工作台，对标正方/强智教务系统。

---

## 一、现状问题总结

### 1. 架构问题

| 问题 | 现状 | 影响 |
|------|------|------|
| 功能分裂 | 开课管理、教学任务、排课中心是 3 个独立页面 | 用户要在 3 个页面来回跳 |
| 重复代码 | ScheduleView.vue(165行) 和 ScheduleCenter.vue(327行) 功能重复 | 维护困难 |
| 巨型组件 | TeachingTaskView.vue 687行含3个抽屉 | 无法复用 |
| 树组件重复 | DeptTree(132行) 和 ScheduleTree(251行) 功能重叠 | 统一成一个 |

### 2. 流程断裂

用户的完整教务流程：
```
校历设置 → 开课计划 → 教学任务(分配教师) → 排课约束 → 自动排课 → 审核发布 → 实况管理
```
但当前分散在 5 个页面：校历管理、开课管理、教学任务、排课中心、调课管理

### 3. 缺失功能

- **排课约束**：没有教师个人约束、班级约束、全局约束的配置 UI
- **自习课填充**：空课位不能自动填充自习课
- **审批流程**：没有排课审批环节
- **冲突可视化**：冲突只是文字列表，没有在课表上标红

---

## 二、重构目标

### 核心理念：**一个工作台，流水线操作**

参考正方教务系统 + 强智教务系统的设计：
- 左侧固定导航树
- 右侧工作区根据流程步骤切换
- 所有操作在同一页面完成，不跳转

### 目标架构

```
/teaching/workbench (教务工作台 - 唯一入口)
├── 左侧: 组织树 (统一的 ScheduleTree, 3种模式)
├── 顶部: 学期选择器 + 流程步骤指示器
└── 右侧: 工作区 (根据步骤切换内容)
    ├── Step 1: 学期准备 (校历+作息表)
    ├── Step 2: 开课计划 (开课+班级分配)
    ├── Step 3: 教学任务 (任务生成+教师分配)
    ├── Step 4: 排课约束 (全局/教师/班级/课程约束)
    ├── Step 5: 智能排课 (执行+手动调整)
    ├── Step 6: 课表查看 (基准课表/实况课表/总览)
    └── Step 7: 调课管理 (申请/审批/执行)
```

---

## 三、新页面结构

### 3.1 统一工作台布局

```
┌──────────────────────────────────────────────────────────────────┐
│ 教务工作台     [2025-2026第一学期 ▼]              [导出] [设置] │
│──────────────────────────────────────────────────────────────────│
│ ① 学期  ② 开课  ③ 任务  ④ 约束  ⑤ 排课  ⑥ 课表  ⑦ 调课     │
│    ✓       ✓       ●                                           │
│──────────────────────────────────────────────────────────────────│
│ ┌─────────┐ ┌───────────────────────────────────────────────┐   │
│ │ 组织树   │ │ 工作区内容（根据当前步骤变化）                  │   │
│ │          │ │                                               │   │
│ │ 经济系   │ │ ┌─ 教学任务 ──────────────────────────────┐   │   │
│ │  2024级  │ │ │ [表格视图] [卡片视图] [总览]             │   │   │
│ │   1班   │ │ │                                           │   │   │
│ │   2班   │ │ │ 课程 | 班级 | 教师 | 课时 | 教室 | 状态  │   │   │
│ │  2025级  │ │ │ ...                                      │   │   │
│ │          │ │ └──────────────────────────────────────────┘   │   │
│ │ 汽车系   │ │                                               │   │
│ │  ...     │ │                                               │   │
│ └─────────┘ └───────────────────────────────────────────────┘   │
│──────────────────────────────────────────────────────────────────│
│ 统计: 开课13门 | 任务85条(已分配85) | 排课145条 | 实况2257条     │
└──────────────────────────────────────────────────────────────────┘
```

### 3.2 各步骤详细设计

#### Step 1: 学期准备
- 校历概览（当前学期信息、教学周数、假期）
- 作息表配置（8节制/10节制，拖拽调整时间）
- **不再是独立页面**，嵌入工作台

#### Step 2: 开课计划
- 当前 OfferingListTab 的功能
- 加 "一键从培养方案导入" 按钮
- 表格+卡片双视图

#### Step 3: 教学任务
- 当前 TaskFulfillmentTab 的功能（紧凑表格+卡片）
- 教师分配用 el-select filterable
- "从开课计划生成任务" 按钮
- 批量分配教师

#### Step 4: 排课约束（新增）

```
┌────────────────────────────────────────────────────────┐
│ 排课约束                                                │
│ [全局约束] [教师约束] [班级约束] [课程约束]              │
│────────────────────────────────────────────────────────│
│                                                        │
│ 全局约束:                                               │
│ ┌──────────────────────────────────────────────────┐   │
│ │ ☑ 周五下午不排课                    [删除]        │   │
│ │ ☑ 每天最多排6节课                   [删除]        │   │
│ │ ☑ 同一教师每天最多连续3节            [删除]        │   │
│ │ ☑ 空课位自动填充自习课              [删除]        │   │
│ │ [+ 添加全局约束]                                  │   │
│ └──────────────────────────────────────────────────┘   │
│                                                        │
│ 教师约束:                                               │
│ ┌──────────────────────────────────────────────────┐   │
│ │ 张明: 周二上午不可用(门诊)           [编辑][删除]  │   │
│ │ 李华: 周四全天不可用(教研)           [编辑][删除]  │   │
│ │ [+ 添加教师约束]                                  │   │
│ └──────────────────────────────────────────────────┘   │
│                                                        │
│ 可视化: 时间矩阵                                        │
│ ┌──────────────────────────────────────────────────┐   │
│ │     周一  周二  周三  周四  周五                    │   │
│ │ 1节  ✓    ✓    ✓    ✓    ✓                       │   │
│ │ 2节  ✓    ✓    ✓    ✓    ✓                       │   │
│ │ 3节  ✓    ✗    ✓    ✓    ✓    ← 红色=禁排       │   │
│ │ 4节  ✓    ✗    ✓    ✓    ✓                       │   │
│ │ 5节  ✓    ✓    ✓    ✗    ✗    ← 周四五下午禁排   │   │
│ │ ...                                               │   │
│ └──────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────┘
```

约束类型：
- **TIME_FORBIDDEN**: 时间禁排（某天某节不排课）
- **MAX_DAILY**: 每日上限（每天最多N节）
- **MAX_CONSECUTIVE**: 最大连排（连续上课不超过N节）
- **ROOM_REQUIRED**: 教室需求（特定课程需要特定类型教室）
- **SPREAD_EVEN**: 均匀分布（同一课程不集中在某天）
- **SELF_STUDY_FILL**: 自习课填充（空课位自动填充）
- **TIME_PREFERRED**: 时间偏好（主课尽量安排上午）

#### Step 5: 智能排课
- 参数配置（迭代次数、种群大小、变异率）
- 就绪检查（教师/教室/约束全部就绪才能排）
- 执行排课 + 进度条
- 结果展示（生成数、冲突数、跳过数）

#### Step 6: 课表查看
- 当前 TimetableViewer 的全部功能
- 基准课表 / 实况课表 / 总览矩阵 三合一
- 班级/教师/教室 左树切换

#### Step 7: 调课管理
- 申请调课（调课/停课/补课）
- 审批列表
- 执行记录

---

## 四、自习课自动填充

### 规则
1. 排课完成后，扫描所有班级的空课位
2. 对于连续空的节次（如第3-4节都空），合并为一个"自习"块
3. 自习课不占用教室（或分配原教室）
4. 自习课在实况课表中显示为灰色/虚线

### 实现
```sql
-- 找到所有班级的空课位
SELECT org_unit_id, weekday, period
FROM (
  SELECT c.id AS org_unit_id, d.day AS weekday, p.period
  FROM org_units c
  CROSS JOIN (SELECT 1 AS day UNION ... SELECT 5) d
  CROSS JOIN (SELECT 1 AS period UNION ... SELECT 8) p
  WHERE c.unit_type = 'CLASS'
) all_slots
LEFT JOIN schedule_entries se ON se.org_unit_id = all_slots.org_unit_id 
  AND se.weekday = all_slots.weekday AND se.start_slot <= all_slots.period AND se.end_slot >= all_slots.period
WHERE se.id IS NULL;

-- 插入自习课条目
INSERT INTO schedule_entries (type, course_name, ...) VALUES ('SELF_STUDY', '自习', ...);
```

---

## 五、文件变更清单

### 删除
- `views/teaching/ScheduleView.vue` (重复)
- `components/teaching/DeptTree.vue` (被 ScheduleTree 替代)
- `components/teaching/TeachingPipeline.vue` (功能嵌入工作台步骤栏)

### 新建
```
views/teaching/
  TeachingWorkbench.vue          # 统一工作台壳
  workbench/
    StepSemester.vue             # Step 1: 学期准备
    StepOffering.vue             # Step 2: 开课计划 (复用 OfferingListTab)
    StepTask.vue                 # Step 3: 教学任务 (复用 TaskFulfillmentTab)
    StepConstraint.vue           # Step 4: 排课约束 (新)
    StepSchedule.vue             # Step 5: 智能排课 (复用 StepExecute)
    StepView.vue                 # Step 6: 课表查看 (复用 TimetableViewer)
    StepAdjustment.vue           # Step 7: 调课管理 (复用 AdjustmentPanel)
    ConstraintEditor.vue         # 约束编辑器
    ConstraintTimeMatrix.vue     # 约束时间矩阵可视化
    SelfStudyFiller.vue          # 自习课填充配置
    WorkbenchFooter.vue          # 底部统计栏
```

### 修改
```
components/teaching/ScheduleTree.vue  # 加自习课模式
router/index.ts                       # 路由合并
styles/teaching-ui.css               # 加步骤栏样式 + CSS变量
```

### 保留（组件复用）
```
scheduling/TimetableGrid.vue         # 课表网格
scheduling/TimetableMatrix.vue       # 总览矩阵
schedule/setup/StepPeriodConfig.vue   # 作息表配置
schedule/setup/StepDataReady.vue      # 就绪检查
schedule/setup/StepExecute.vue        # 排课执行
schedule/LiveTimetable.vue            # 实况课表
offering/TaskFulfillmentTab.vue       # 任务落实
offering/OfferingListTab.vue          # 开课计划
```

---

## 六、后端变更

### 新增 API
```
POST /teaching/constraints          # 创建约束
PUT  /teaching/constraints/{id}     # 更新约束
DELETE /teaching/constraints/{id}   # 删除约束
GET  /teaching/constraints?semesterId=&level=&targetId=  # 查询约束
POST /teaching/self-study/fill      # 自习课自动填充
DELETE /teaching/self-study/clear   # 清除自习课
```

### 约束数据表 `scheduling_constraints`（已有）
```sql
id, semester_id, constraint_name, constraint_level (1全局/2教师/3班级/4课程),
target_id, constraint_type, is_hard, priority, params (JSON), 
effective_weeks, enabled
```

---

## 七、执行计划

### Phase 1: 基础框架（1天）
1. 创建 TeachingWorkbench.vue 壳
2. 实现步骤导航栏
3. 路由合并（/teaching/workbench）
4. 将现有组件嵌入各步骤

### Phase 2: 约束管理（1天）
5. ConstraintEditor 组件
6. ConstraintTimeMatrix 可视化
7. 教师/班级/全局约束 CRUD
8. 排课引擎读取约束

### Phase 3: 自习课填充（半天）
9. SelfStudyFiller 组件
10. 后端填充 API
11. 课表显示自习课（灰色虚线）

### Phase 4: 清理收尾（半天）
12. 删除旧页面
13. 侧边栏导航简化
14. 统一 CSS 变量
15. 统计栏实时刷新

---

## 八、参考设计

### 正方教务系统
- 左树右表布局
- 步骤式排课流程
- 约束时间矩阵（红绿方格）
- 教师工作量统计

### 强智教务系统
- 流水线式操作（上一步→下一步）
- 排课结果预览
- 一键发布
- 课表对比

### 超星教务
- 拖拽式约束配置
- 智能冲突检测
- 移动端课表查看
