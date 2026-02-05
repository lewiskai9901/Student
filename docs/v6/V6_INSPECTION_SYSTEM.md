# V6 通用检查系统设计文档

## 概述

V6检查系统是一个通用化、高度可配置的检查评分系统，支持对组织（ORG）、场所（SPACE）、用户（USER）三种类型目标进行检查。系统采用项目-任务-目标三层架构，支持灵活的周期配置、检查员分配策略、以及多维度数据汇总统计。

## 核心概念

### 1. 检查项目 (InspectionProject)

检查项目是系统的顶层配置单元，定义了检查的范围、周期、规则等。

**主要属性：**
- `projectCode`: 项目编号（唯一标识）
- `projectName`: 项目名称
- `templateId`: 检查模板ID
- `scopeType`: 范围类型（ORG/SPACE/USER）
- `scopeIds`: 范围ID列表
- `cycleType`: 周期类型（DAILY/WEEKLY/MONTHLY/CUSTOM）
- `baseScore`: 基础分
- `status`: 项目状态

**项目状态流转：**
```
DRAFT → ACTIVE → PAUSED → ACTIVE → COMPLETED → ARCHIVED
         ↑___________|
```

### 2. 检查任务 (InspectionTask)

检查任务是项目在特定日期的执行实例。

**主要属性：**
- `taskCode`: 任务编号
- `taskDate`: 任务日期
- `status`: 任务状态
- `inspectorId`: 检查员ID

**任务状态流转：**
```
PENDING → IN_PROGRESS → SUBMITTED → REVIEWED → PUBLISHED
              ↓
          CANCELLED
```

### 3. 检查目标 (InspectionTarget)

检查目标是任务中的具体检查对象。

**主要属性：**
- `targetType`: 目标类型
- `targetId`: 目标ID
- `baseScore`: 基础分
- `deductionTotal`: 总扣分
- `bonusTotal`: 总加分
- `finalScore`: 最终得分
- `status`: 目标状态

## 系统架构

### 数据库设计

```
inspection_projects
    ├── inspection_tasks (1:N)
    │       └── inspection_targets (1:N)
    │               ├── inspection_deductions (1:N)
    │               └── inspection_bonuses (1:N)
    └── project_inspector_configs (1:N)

task_inspector_assignments (多对多关系表)

inspection_daily_summaries
inspection_weekly_summaries
inspection_monthly_summaries
```

### 后端服务层

1. **InspectionProjectApplicationService** - 项目管理服务
   - 创建、配置、发布、暂停、恢复、完成、归档项目

2. **TaskGenerationService** - 任务生成引擎
   - 定时生成检查任务（每日凌晨2点）
   - 支持手动批量生成

3. **InspectionTaskApplicationService** - 任务执行服务
   - 任务领取、开始、提交、审核、发布
   - 目标锁定、检查、扣分/加分记录

4. **InspectorAssignmentService** - 检查员分配服务
   - 项目级检查员配置
   - 任务级检查员分配
   - 自动分配功能

5. **InspectionSummaryService** - 汇总统计服务
   - 日/周/月汇总生成（每日凌晨3点）
   - 排名计算（1224排名法）
   - 趋势分析

### 前端页面

| 路由 | 组件 | 功能 |
|------|------|------|
| /inspection/v6/projects | ProjectList.vue | 项目列表管理 |
| /inspection/v6/projects/create | ProjectCreate.vue | 创建项目向导 |
| /inspection/v6/projects/:id | ProjectDetail.vue | 项目详情 |
| /inspection/v6/projects/:id/config | ProjectConfig.vue | 项目配置 |
| /inspection/v6/tasks | TaskList.vue | 任务列表 |
| /inspection/v6/tasks/:id | TaskDetail.vue | 任务详情 |
| /inspection/v6/tasks/:id/execute | TaskExecute.vue | 执行检查 |
| /inspection/v6/my-tasks | MyTasks.vue | 我的任务 |
| /inspection/v6/summary | SummaryDashboard.vue | 汇总统计 |
| /inspection/v6/ranking | RankingView.vue | 排名榜 |

## API 端点

### 项目管理
- `POST /v6/inspection-projects` - 创建项目
- `PUT /v6/inspection-projects/{id}/config` - 更新配置
- `POST /v6/inspection-projects/{id}/publish` - 发布项目
- `POST /v6/inspection-projects/{id}/pause` - 暂停项目
- `POST /v6/inspection-projects/{id}/resume` - 恢复项目
- `POST /v6/inspection-projects/{id}/complete` - 完成项目
- `POST /v6/inspection-projects/{id}/archive` - 归档项目

### 任务管理
- `POST /v6/inspection-tasks/generate` - 生成任务
- `GET /v6/inspection-tasks/available` - 获取可领取任务
- `GET /v6/inspection-tasks/my-tasks` - 获取我的任务
- `POST /v6/inspection-tasks/{id}/claim` - 领取任务
- `POST /v6/inspection-tasks/{id}/start` - 开始任务
- `POST /v6/inspection-tasks/{id}/submit` - 提交任务
- `POST /v6/inspection-tasks/{id}/review` - 审核任务
- `POST /v6/inspection-tasks/{id}/publish` - 发布任务

### 检查员分配
- `GET /v6/inspector-assignments/projects/{projectId}/inspectors` - 项目检查员
- `POST /v6/inspector-assignments/tasks/{taskId}/auto-assign` - 自动分配

### 汇总统计
- `GET /v6/inspection-summaries/projects/{projectId}/daily-ranking` - 日排名
- `GET /v6/inspection-summaries/projects/{projectId}/org-summary` - 组织汇总
- `GET /v6/inspection-summaries/projects/{projectId}/class-summary` - 班级汇总
- `GET /v6/inspection-summaries/projects/{projectId}/trend` - 趋势数据

## 权限设计

| 权限代码 | 描述 |
|---------|------|
| inspection:project:view | 查看检查项目 |
| inspection:project:create | 创建检查项目 |
| inspection:project:update | 更新检查项目 |
| inspection:project:delete | 删除检查项目 |
| inspection:project:publish | 发布检查项目 |
| inspection:task:view | 查看检查任务 |
| inspection:task:generate | 生成检查任务 |
| inspection:task:assign | 分配检查任务 |
| inspection:task:execute | 执行检查任务 |
| inspection:task:review | 审核检查任务 |
| inspection:task:publish | 发布检查任务 |
| inspection:summary:view | 查看汇总统计 |
| inspection:summary:generate | 生成汇总统计 |

## 高级特性

### 1. 检查员分配模式
- **FREE**: 自由领取模式，任何有权限的检查员都可以领取任务
- **ASSIGNED**: 指定分配模式，任务只分配给指定检查员
- **HYBRID**: 混合模式，结合两种模式

### 2. 共享场所策略
当多个组织共用同一场所时：
- **RATIO**: 按比例分摊扣分
- **AVERAGE**: 平均分摊
- **FULL**: 全部计入各组织
- **MAIN_ONLY**: 仅计入主要负责组织

### 3. 双周期模式
支持同时启用两种检查周期（如每日检查+每周汇总检查）

### 4. 检查项加权
支持为不同检查项设置不同权重

## 数据库迁移

相关迁移脚本：
- `V8.4.0__v6_inspection_project.sql` - 核心表结构
- `V8.4.1__v6_inspection_summary.sql` - 汇总统计表
- `V8.4.2__v6_inspection_permissions.sql` - 权限配置

## 使用指南

### 创建检查项目

1. 进入"V6检查项目"页面
2. 点击"创建项目"
3. 填写基本信息（项目编号、名称、模板）
4. 配置检查范围（选择组织/场所/用户）
5. 设置周期（每日/每周/每月）
6. 保存并发布

### 执行检查任务

1. 进入"我的任务"页面
2. 领取可用任务（FREE模式）或查看分配任务
3. 点击"开始"进入执行页面
4. 逐个检查目标，记录扣分/加分
5. 完成后提交任务

### 查看统计数据

1. 进入"汇总统计"页面
2. 选择项目和日期范围
3. 查看组织/班级汇总
4. 进入"排名榜"查看每日排名

## 注意事项

1. 项目发布后不可删除，只能归档
2. 任务生成后自动按配置的周期执行
3. 汇总数据每日凌晨自动生成
4. 建议定期归档已完成的项目以保持系统性能
