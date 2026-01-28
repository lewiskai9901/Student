# 前后端架构适配分析报告

## 一、当前架构对比

### 1.1 后端 DDD 领域划分 (8个领域)

| 领域 | 英文名 | V2 API 路径 | 核心实体 |
|------|--------|------------|---------|
| 权限访问 | Access | `/v2/permissions`, `/v2/roles` | Role, Permission, UserRole |
| 组织架构 | Organization | `/v2/org-units`, `/v2/organization/classes`, `/v2/grades` | OrgUnit, SchoolClass, Grade |
| 量化检查 | Inspection | `/v2/inspection-templates`, `/v2/inspection-records`, `/v2/appeals` | Template, Record, Appeal |
| 资产管理 | Asset | `/v2/dormitory/rooms` | Building, Dormitory |
| 学生管理 | Student | `/v2/students` | Student |
| 任务管理 | Task | `/task/tasks`, `/task/workflow-templates` | Task, WorkflowTemplate |
| 评比管理 | Rating | `/v2/rating` (待完善) | RatingConfig, RatingResult |
| 共享内核 | Shared | - | Entity, AggregateRoot, ValueObject |

### 1.2 前端路由结构 (当前)

| 模块 | 路径前缀 | 子路由数 | 对应后端领域 |
|------|---------|---------|-------------|
| 学生事务 | `/student-affairs/` | 3 | Student + Organization (混合) |
| 任务管理 | `/task/` | 4 | Task ✓ |
| 宿舍管理 | `/dormitory/` | 4 | Asset ✓ |
| 教学管理 | `/academic/` | 2 | Organization (部分) |
| 量化检查 | `/quantification/` | **18** | Inspection + Rating (混合) |
| 综合测评 | `/evaluation/` | 10 | (无直接对应，V1遗留) |
| 教学设施 | `/teaching/` | 2 | Asset (部分) |
| 系统配置 | `/config/` | 2 | (配置杂项) |
| 系统管理 | `/system/` | 7 | Access + Organization (混合) |

---

## 二、发现的问题

### 2.1 命名不一致

| 问题 | 前端 | 后端 | 影响 |
|------|------|------|------|
| 学生模块 | `student-affairs` | `student` | 语义不匹配 |
| 量化检查 | `quantification` | `inspection` | 命名冲突 |
| 权限管理 | `system/roles, system/permissions` | `access` | 归属不清 |
| 组织架构 | 分散在多个模块 | `organization` | 功能分散 |

### 2.2 API 版本混乱

```
当前状态:
├── V1 API (58个模块) - 大部分页面仍在使用
├── V2 API (7个模块) - 已创建但使用率低
└── 混合调用 - 同一页面可能混用V1/V2
```

**问题实例**：
- `TaskList.vue` 使用 `/task/tasks` (V2风格路径，V1实现)
- `StudentList.vue` 使用 `/student/` (V1 API)
- `RolesView.vue` 使用 `/system/roles` (V1)，但V2已有 `/v2/roles`

### 2.3 量化检查模块过于复杂

`/quantification/` 包含 **18个子路由**，混合了：
- 检查配置 (config)
- 检查计划 (check-plan)
- 检查记录 (check-record-v3)
- 申诉管理 (appeals-v3)
- 评级管理 (rating-*)
- 统计分析 (analysis-*, smart-statistics)
- 徽章管理 (badge-management)
- 通知管理 (notification)

**应拆分为**：
- Inspection (检查)
- Rating (评比)
- Analysis (统计分析)

### 2.4 遗留代码和隐藏路由

存在多个隐藏/废弃路由：
- `/quantification/daily-checks` (hidden: true)
- `/quantification/check-record-v3` (hidden: true)
- `/quantification/analysis-configs` (hidden: true)
- `/system/roles` (hidden: true, 推荐用V2)
- `/system/permissions` (hidden: true, 推荐用V2)

### 2.5 V2 视图未完全创建

已创建的 V2 视图 (`/frontend/src/views/v2/`)：
- organization/ (OrgUnitsView, SchoolClassesView)
- inspection/ (TemplateListView, RecordListView, AppealListView)
- access/ (RoleListView, PermissionListView)

但主路由未指向这些V2视图。

---

## 三、优化建议

### 3.1 新的导航结构（与DDD对齐）

```
建议的一级导航:
├── 首页 (Dashboard)
├── 组织管理 (Organization)     ← 对应 organization 领域
│   ├── 组织架构
│   ├── 年级管理
│   ├── 班级管理
│   └── 部门管理
├── 学生管理 (Student)          ← 对应 student 领域
│   ├── 学生列表
│   ├── 学生导入
│   └── 学生统计
├── 量化检查 (Inspection)       ← 对应 inspection 领域
│   ├── 检查计划
│   ├── 检查记录
│   ├── 申诉管理
│   └── 检查模板
├── 评比管理 (Rating)           ← 对应 rating 领域 (新拆分)
│   ├── 评比配置
│   ├── 评比结果
│   ├── 评级统计
│   └── 徽章管理
├── 资产管理 (Asset)            ← 对应 asset 领域
│   ├── 楼宇管理
│   ├── 宿舍管理
│   ├── 教室管理
│   └── 院系分配
├── 任务中心 (Task)             ← 对应 task 领域
│   ├── 任务列表
│   ├── 我的任务
│   ├── 任务审批
│   └── 流程模板
├── 统计分析 (Analysis)         ← 新增独立模块
│   ├── 智能统计
│   ├── 分析配置
│   └── 数据报表
├── 系统管理 (System)           ← 对应 access 领域 + 系统配置
│   ├── 用户管理
│   ├── 角色管理 (V2)
│   ├── 权限管理 (V2)
│   ├── 操作日志
│   └── 系统配置
└── 综合测评 (Evaluation)       ← 保留，待重构
```

### 3.2 路由路径重构

| 当前路径 | 建议路径 | 对应后端API |
|---------|---------|------------|
| `/student-affairs/students` | `/student/list` | `/v2/students` |
| `/student-affairs/classes` | `/organization/classes` | `/v2/organization/classes` |
| `/student-affairs/departments` | `/organization/departments` | `/v2/org-units` |
| `/academic/grades` | `/organization/grades` | `/v2/grades` |
| `/academic/majors` | `/organization/majors` | `/v2/org-units?type=major` |
| `/quantification/config` | `/inspection/config` | `/v2/inspection-templates` |
| `/quantification/check-plan` | `/inspection/plans` | `/v2/inspection-records` |
| `/quantification/appeals-v3` | `/inspection/appeals` | `/v2/appeals` |
| `/quantification/rating-*` | `/rating/*` | `/v2/rating/*` |
| `/quantification/analysis-*` | `/analysis/*` | `/v2/analysis/*` |
| `/system/roles` | `/system/roles` | `/v2/roles` |
| `/system/permissions` | `/system/permissions` | `/v2/permissions` |
| `/dormitory/*` | `/asset/dormitory/*` | `/v2/dormitory/*` |
| `/teaching/*` | `/asset/teaching/*` | `/v2/teaching/*` |

### 3.3 API 迁移策略

```
Phase 1 - 权限模块 (已有V2 API)
├── 将 /system/roles 页面切换到 /v2/roles API
└── 将 /system/permissions 页面切换到 /v2/permissions API

Phase 2 - 组织模块 (已有V2 API)
├── 将 /student-affairs/departments 切换到 /v2/org-units
├── 将 /student-affairs/classes 切换到 /v2/organization/classes
└── 将 /academic/grades 切换到 /v2/grades

Phase 3 - 检查模块 (已有V2 API)
├── 将 /quantification/check-plan 切换到 /v2/inspection-records
├── 将 /quantification/appeals-v3 切换到 /v2/appeals
└── 创建新的检查模板管理页面

Phase 4 - 学生模块 (已有V2 API)
└── 将 /student-affairs/students 切换到 /v2/students

Phase 5 - 评比模块 (需完善后端)
└── 完善 rating 领域的 V2 API，然后迁移前端

Phase 6 - 清理
├── 移除 V1 API 调用
├── 删除废弃的页面和路由
└── 更新权限配置
```

---

## 四、具体实施方案

### 4.1 路由配置重构

创建新的路由配置文件结构：
```
frontend/src/router/
├── index.ts              # 主路由入口
├── modules/
│   ├── organization.ts   # 组织管理路由
│   ├── student.ts        # 学生管理路由
│   ├── inspection.ts     # 量化检查路由
│   ├── rating.ts         # 评比管理路由
│   ├── asset.ts          # 资产管理路由
│   ├── task.ts           # 任务中心路由
│   ├── analysis.ts       # 统计分析路由
│   ├── system.ts         # 系统管理路由
│   └── evaluation.ts     # 综合测评路由
└── guards/
    └── permission.ts     # 权限守卫
```

### 4.2 API 模块重构

```
frontend/src/api/
├── v2/                   # V2 API (主要使用)
│   ├── organization.ts   ✓ 已存在
│   ├── student.ts        ✓ 已存在
│   ├── inspection.ts     ✓ 已存在
│   ├── access.ts         ✓ 已存在
│   ├── task.ts           ✓ 已存在
│   ├── dormitory.ts      ✓ 已存在
│   ├── rating.ts         ✗ 需创建
│   └── analysis.ts       ✗ 需创建
├── _deprecated/          # 废弃的 V1 API (逐步删除)
└── index.ts              # 统一导出
```

### 4.3 视图目录重构

```
frontend/src/views/
├── dashboard/            # 首页
├── organization/         # 组织管理 (新)
│   ├── OrgUnitsView.vue
│   ├── ClassesView.vue
│   ├── GradesView.vue
│   └── DepartmentsView.vue
├── student/              # 学生管理 (重命名)
│   ├── ListView.vue
│   ├── ImportView.vue
│   └── StatisticsView.vue
├── inspection/           # 量化检查 (重组)
│   ├── PlansView.vue
│   ├── RecordsView.vue
│   ├── AppealsView.vue
│   └── TemplatesView.vue
├── rating/               # 评比管理 (新拆分)
│   ├── ConfigView.vue
│   ├── ResultsView.vue
│   ├── StatisticsView.vue
│   └── BadgesView.vue
├── asset/                # 资产管理 (合并)
│   ├── dormitory/
│   └── teaching/
├── task/                 # 任务中心 (保持)
├── analysis/             # 统计分析 (新)
├── system/               # 系统管理 (保持)
└── evaluation/           # 综合测评 (保持，待重构)
```

---

## 五、权限码重构

### 5.1 当前权限码命名 (不一致)

```
quantification:config:view
quantification:plan:view
quantification:appeal:v3:view
task:list
task:approve
system:user:view
```

### 5.2 建议的权限码命名 (与DDD对齐)

```
organization:orgunit:view|create|update|delete
organization:class:view|create|update|delete
organization:grade:view|create|update|delete

student:view|create|update|delete|import|export

inspection:template:view|create|update|delete|publish
inspection:record:view|create|submit|publish
inspection:appeal:view|create|review|final-review

rating:config:view|create|update
rating:result:view|publish
rating:statistics:view

asset:building:view|create|update|delete
asset:dormitory:view|create|update|delete|checkin|checkout

task:view|create|accept|submit|approve
task:workflow:view|create|update|delete

analysis:config:view|create|update
analysis:report:view|export

access:role:view|create|update|delete
access:permission:view|create|update|delete
access:user:view|create|update|delete
```

---

## 六、迁移风险和应对

| 风险 | 影响 | 应对措施 |
|------|------|---------|
| 路由变更导致旧链接失效 | 用户收藏的链接失效 | 添加重定向规则 |
| API 切换导致数据格式不兼容 | 页面报错 | 做好 DTO 映射 |
| 权限码变更导致权限丢失 | 用户无法访问 | 数据库迁移脚本 |
| 开发周期长 | 影响新功能开发 | 分阶段实施 |

### 重定向规则示例

```typescript
// router/redirects.ts
export const redirects = [
  { path: '/student-affairs/students', redirect: '/student/list' },
  { path: '/student-affairs/classes', redirect: '/organization/classes' },
  { path: '/student-affairs/departments', redirect: '/organization/departments' },
  { path: '/quantification/check-plan', redirect: '/inspection/plans' },
  { path: '/quantification/appeals-v3', redirect: '/inspection/appeals' },
  { path: '/quantification/rating-statistics', redirect: '/rating/statistics' },
  // ... 更多重定向
]
```

---

## 七、总结

### 核心改动
1. **导航重构**: 从功能导向改为领域导向
2. **API统一**: 全面迁移到 V2 DDD API
3. **权限对齐**: 权限码与后端领域保持一致
4. **代码清理**: 移除废弃的 V1 代码和隐藏路由

### 预期收益
1. **认知一致**: 前后端使用相同的领域语言
2. **维护简单**: 代码结构清晰，易于定位
3. **扩展方便**: 新功能按领域添加
4. **团队协作**: 减少前后端沟通成本

### 建议优先级
1. **高优先级**: 权限模块 (已有完整V2 API)
2. **高优先级**: 组织模块 (已有完整V2 API)
3. **中优先级**: 检查模块 (需要页面重构)
4. **中优先级**: 学生模块 (变化较小)
5. **低优先级**: 评比模块 (需完善后端)
6. **低优先级**: 综合测评 (大规模重构)
