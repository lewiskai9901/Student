# V5 重构方案 - 前端重构指南

> **版本**: 3.0
> **日期**: 2026-01-31
> **关联文档**: [V5_ARCHITECTURE.md](./V5_ARCHITECTURE.md)

---

## 一、重构概述

### 1.1 重构目标

将现有前端代码适配 V5 后端架构变更，主要包括：

1. **API 层重构** - 适配新的检查系统 API（Template → Project → Task）
2. **类型定义更新** - 更新 TypeScript 类型以匹配新数据模型
3. **路由结构调整** - 优化检查模块路由
4. **组件迁移** - 复用/重构现有组件
5. **权限组件** - 新增数据权限配置组件

### 1.2 影响范围

| 模块 | 影响程度 | 说明 |
|------|----------|------|
| 检查系统 | 高 | 核心业务逻辑变更 |
| 权限系统 | 中 | 新增数据权限配置 |
| 组织管理 | 低 | API 路径小调整 |
| 其他模块 | 无 | 保持不变 |

---

## 二、API 层重构

### 2.1 现有 API 结构

```
frontend/src/api/
├── inspection.ts              # 模板、记录、申诉 API
├── inspectionSession.ts       # V4 会话 API
├── inspectionConfig.ts        # 配置 API
├── inspectionAnalytics.ts     # 分析 API
├── inspectionExport.ts        # 导出 API
├── correctiveAction.ts        # 整改 API
├── appeal.ts                  # 申诉 API（冗余）
├── behavior.ts                # 行为记录 API
└── ...
```

### 2.2 目标 API 结构

```
frontend/src/api/
├── inspection/
│   ├── index.ts               # 统一导出
│   ├── template.ts            # 检查模板 API
│   ├── project.ts             # 检查项目 API（新）
│   ├── task.ts                # 检查任务 API（新）
│   ├── record.ts              # 检查记录 API（重构）
│   ├── appeal.ts              # 申诉 API
│   ├── corrective.ts          # 整改 API
│   ├── summary.ts             # 汇总排名 API（新）
│   └── analytics.ts           # 分析 API
├── permission/
│   ├── index.ts               # 统一导出
│   ├── role.ts                # 角色 API
│   └── dataPermission.ts      # 数据权限 API（新）
└── ...（其他模块保持不变）
```

### 2.3 API 迁移对照表

| 现有 API | 新 API | 变更说明 |
|----------|--------|----------|
| `inspectionSession.ts` | `inspection/task.ts` | Session → Task，概念重命名 |
| `inspection.ts` (模板部分) | `inspection/template.ts` | 拆分，路径不变 |
| `inspection.ts` (记录部分) | `inspection/record.ts` | 重构，字段变更 |
| `inspection.ts` (申诉部分) | `inspection/appeal.ts` | 拆分，逻辑不变 |
| `correctiveAction.ts` | `inspection/corrective.ts` | 移入子目录 |
| 无 | `inspection/project.ts` | 新增项目管理 |
| 无 | `inspection/summary.ts` | 新增汇总排名 |
| 无 | `permission/dataPermission.ts` | 新增数据权限 |

### 2.4 新增 API 文件

#### 2.4.1 检查项目 API

```typescript
// frontend/src/api/inspection/project.ts
import { http } from '@/utils/request'
import type { PageResult, PageParams } from '@/types'

export interface InspectionProject {
  id: number
  projectCode: string
  projectName: string
  templateId: number
  templateSnapshot: object
  inspectionLevel: 'SCHOOL' | 'DEPARTMENT' | 'CUSTOM'
  targetOrgUnitIds: number[]
  startDate: string
  endDate: string
  semesterId: number
  entryMode: 'SPACE' | 'PERSON' | 'CLASS' | 'ITEM' | 'CHECKLIST'
  fairWeightEnabled: boolean
  fairWeightMode: 'DIVIDE' | 'BENCHMARK'
  benchmarkCount: number | null
  mixedDormitoryStrategy: 'RATIO' | 'AVERAGE' | 'FULL' | 'MAIN'
  status: 'DRAFT' | 'ACTIVE' | 'PAUSED' | 'COMPLETED' | 'ARCHIVED'
  createdBy: number
  createdAt: string
}

export interface CreateProjectRequest {
  projectCode: string
  projectName: string
  templateId: number
  inspectionLevel: string
  targetOrgUnitIds: number[]
  startDate: string
  endDate: string
  semesterId: number
  entryMode: string
  fairWeightEnabled?: boolean
  fairWeightMode?: string
  benchmarkCount?: number
  mixedDormitoryStrategy?: string
}

const BASE_URL = '/inspection/projects'

export function getProjects(params?: PageParams & {
  status?: string
  semesterId?: number
}): Promise<PageResult<InspectionProject>> {
  return http.get(BASE_URL, { params })
}

export function getProject(id: number): Promise<InspectionProject> {
  return http.get(`${BASE_URL}/${id}`)
}

export function createProject(data: CreateProjectRequest): Promise<InspectionProject> {
  return http.post(BASE_URL, data)
}

export function updateProject(id: number, data: Partial<CreateProjectRequest>): Promise<InspectionProject> {
  return http.put(`${BASE_URL}/${id}`, data)
}

export function deleteProject(id: number): Promise<void> {
  return http.delete(`${BASE_URL}/${id}`)
}

export function activateProject(id: number): Promise<InspectionProject> {
  return http.post(`${BASE_URL}/${id}/activate`)
}

export function pauseProject(id: number): Promise<InspectionProject> {
  return http.post(`${BASE_URL}/${id}/pause`)
}

export function completeProject(id: number): Promise<InspectionProject> {
  return http.post(`${BASE_URL}/${id}/complete`)
}

export const projectApi = {
  getList: getProjects,
  getById: getProject,
  create: createProject,
  update: updateProject,
  delete: deleteProject,
  activate: activateProject,
  pause: pauseProject,
  complete: completeProject
}
```

#### 2.4.2 检查任务 API

```typescript
// frontend/src/api/inspection/task.ts
import { http } from '@/utils/request'
import type { PageResult, PageParams } from '@/types'

export interface InspectionTask {
  id: number
  projectId: number
  taskDate: string
  status: 'DRAFT' | 'IN_PROGRESS' | 'SUBMITTED' | 'REVIEWED' | 'PUBLISHED'
  inspectorIds: number[]
  targetCount: number
  completedCount: number
  createdAt: string
  submittedAt: string | null
  reviewedAt: string | null
  publishedAt: string | null
}

export interface CreateTaskRequest {
  projectId: number
  taskDate: string
  inspectorIds?: number[]
}

export interface TargetInspectionRecord {
  id: number
  taskId: number
  targetType: 'DORMITORY' | 'CLASSROOM' | 'STUDENT'
  targetId: number
  targetSnapshot: object
  classId: number
  className: string
  orgUnitId: number
  orgUnitName: string
  baseScore: number
  rawScore: number
  weightedScore: number
  fairAdjustedScore: number
  totalDeduction: number
  deductionCount: number
  inspectorId: number
  inspectedAt: string
}

export interface DeductionRecord {
  id: number
  targetRecordId: number
  categoryId: number
  categoryName: string
  scoreItemId: number
  itemName: string
  score: number
  scoreType: 'DEDUCTION' | 'ADDITION'
  quantity: number
  studentId: number | null
  remark: string
  evidenceUrls: string[]
}

const BASE_URL = '/inspection/tasks'

export function getTasks(params?: PageParams & {
  projectId?: number
  status?: string
  startDate?: string
  endDate?: string
}): Promise<PageResult<InspectionTask>> {
  return http.get(BASE_URL, { params })
}

export function getTask(id: number): Promise<InspectionTask> {
  return http.get(`${BASE_URL}/${id}`)
}

export function createTask(data: CreateTaskRequest): Promise<InspectionTask> {
  return http.post(BASE_URL, data)
}

export function generateTasks(projectId: number, startDate: string, endDate: string): Promise<InspectionTask[]> {
  return http.post(`${BASE_URL}/generate`, { projectId, startDate, endDate })
}

export function assignInspectors(taskId: number, inspectorIds: number[]): Promise<InspectionTask> {
  return http.put(`${BASE_URL}/${taskId}/inspectors`, { inspectorIds })
}

// 检查记录相关
export function getTaskRecords(taskId: number): Promise<TargetInspectionRecord[]> {
  return http.get(`${BASE_URL}/${taskId}/records`)
}

export function createRecord(taskId: number, data: {
  targetType: string
  targetId: number
  deductions?: { scoreItemId: number; quantity?: number; remark?: string }[]
}): Promise<TargetInspectionRecord> {
  return http.post(`${BASE_URL}/${taskId}/records`, data)
}

export function addDeduction(recordId: number, data: {
  scoreItemId: number
  quantity?: number
  studentId?: number
  remark?: string
  evidenceUrls?: string[]
}): Promise<DeductionRecord> {
  return http.post(`/inspection/records/${recordId}/deductions`, data)
}

export function removeDeduction(recordId: number, deductionId: number): Promise<void> {
  return http.delete(`/inspection/records/${recordId}/deductions/${deductionId}`)
}

// 任务状态变更
export function submitTask(taskId: number): Promise<InspectionTask> {
  return http.post(`${BASE_URL}/${taskId}/submit`)
}

export function reviewTask(taskId: number, approved: boolean, comment?: string): Promise<InspectionTask> {
  return http.post(`${BASE_URL}/${taskId}/review`, { approved, comment })
}

export function publishTask(taskId: number): Promise<InspectionTask> {
  return http.post(`${BASE_URL}/${taskId}/publish`)
}

export const taskApi = {
  getList: getTasks,
  getById: getTask,
  create: createTask,
  generate: generateTasks,
  assignInspectors,
  getRecords: getTaskRecords,
  createRecord,
  addDeduction,
  removeDeduction,
  submit: submitTask,
  review: reviewTask,
  publish: publishTask
}
```

#### 2.4.3 汇总排名 API

```typescript
// frontend/src/api/inspection/summary.ts
import { http } from '@/utils/request'

export interface ClassSummary {
  classId: number
  className: string
  orgUnitId: number
  orgUnitName: string
  averageScore: number
  totalDeduction: number
  inspectionCount: number
  rank: number
  trend: 'UP' | 'DOWN' | 'STABLE'
  trendValue: number
}

export interface DepartmentSummary {
  orgUnitId: number
  orgUnitName: string
  averageScore: number
  classCount: number
  rank: number
}

export interface DailySummary {
  summaryDate: string
  projectId: number
  classSummaries: ClassSummary[]
}

const BASE_URL = '/inspection/summaries'

export function getDailySummary(projectId: number, date: string): Promise<DailySummary> {
  return http.get(`${BASE_URL}/daily`, { params: { projectId, date } })
}

export function getWeeklySummary(projectId: number, startDate: string, endDate: string): Promise<ClassSummary[]> {
  return http.get(`${BASE_URL}/weekly`, { params: { projectId, startDate, endDate } })
}

export function getMonthlySummary(projectId: number, year: number, month: number): Promise<ClassSummary[]> {
  return http.get(`${BASE_URL}/monthly`, { params: { projectId, year, month } })
}

export function getSemesterSummary(projectId: number): Promise<ClassSummary[]> {
  return http.get(`${BASE_URL}/semester`, { params: { projectId } })
}

export function getDepartmentRanking(projectId: number, period: 'daily' | 'weekly' | 'monthly', date?: string): Promise<DepartmentSummary[]> {
  return http.get(`${BASE_URL}/department-ranking`, { params: { projectId, period, date } })
}

export function regenerateSummary(projectId: number, date: string): Promise<void> {
  return http.post(`${BASE_URL}/regenerate`, { projectId, date })
}

export const summaryApi = {
  getDaily: getDailySummary,
  getWeekly: getWeeklySummary,
  getMonthly: getMonthlySummary,
  getSemester: getSemesterSummary,
  getDepartmentRanking,
  regenerate: regenerateSummary
}
```

---

## 三、类型定义更新

### 3.1 新增类型文件

```typescript
// frontend/src/types/inspection.ts

// ==================== 检查模板 ====================

export type ScoringMode = 'DEDUCTION' | 'ADDITION' | 'CHECKLIST' | 'BONUS_ONLY'
export type TemplateVisibility = 'PRIVATE' | 'DEPARTMENT' | 'PUBLIC'
export type TargetType = 'DORMITORY' | 'CLASSROOM' | 'STUDENT'

export interface ScoreItem {
  id: number
  itemCode: string
  itemName: string
  score: number
  requireEvidence: boolean
  maxQuantity: number
  sortOrder: number
}

export interface ScoreCategory {
  id: number
  categoryCode: string
  categoryName: string
  weight: number
  sortOrder: number
  items: ScoreItem[]
}

export interface InspectionTemplate {
  id: number
  templateCode: string
  templateName: string
  targetType: TargetType
  baseScore: number
  scoringMode: ScoringMode
  visibility: TemplateVisibility
  description: string
  categories: ScoreCategory[]
  status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
  isDefault: boolean
  createdBy: number
  createdAt: string
}

// ==================== 检查项目 ====================

export type EntryMode = 'SPACE' | 'PERSON' | 'CLASS' | 'ITEM' | 'CHECKLIST'
export type InspectionLevel = 'SCHOOL' | 'DEPARTMENT' | 'CUSTOM'
export type FairWeightMode = 'DIVIDE' | 'BENCHMARK'
export type MixedDormitoryStrategy = 'RATIO' | 'AVERAGE' | 'FULL' | 'MAIN'
export type ProjectStatus = 'DRAFT' | 'ACTIVE' | 'PAUSED' | 'COMPLETED' | 'ARCHIVED'

export interface InspectionProject {
  id: number
  projectCode: string
  projectName: string
  templateId: number
  templateSnapshot: InspectionTemplate
  inspectionLevel: InspectionLevel
  targetOrgUnitIds: number[]
  startDate: string
  endDate: string
  semesterId: number
  entryMode: EntryMode
  fairWeightEnabled: boolean
  fairWeightMode: FairWeightMode
  benchmarkCount: number | null
  mixedDormitoryStrategy: MixedDormitoryStrategy
  status: ProjectStatus
  createdBy: number
  createdAt: string
}

// ==================== 检查任务 ====================

export type TaskStatus = 'DRAFT' | 'IN_PROGRESS' | 'SUBMITTED' | 'REVIEWED' | 'PUBLISHED'

export interface InspectionTask {
  id: number
  projectId: number
  project?: InspectionProject
  taskDate: string
  status: TaskStatus
  inspectorIds: number[]
  targetCount: number
  completedCount: number
  createdAt: string
  submittedAt: string | null
  reviewedAt: string | null
  publishedAt: string | null
}

// ==================== 检查记录 ====================

export interface TargetSnapshot {
  id: number
  name: string
  code: string
  // 其他快照字段根据 targetType 不同而变化
  [key: string]: unknown
}

export interface TargetInspectionRecord {
  id: number
  taskId: number
  targetType: TargetType
  targetId: number
  targetSnapshot: TargetSnapshot
  classId: number
  className: string
  orgUnitId: number
  orgUnitName: string
  baseScore: number
  rawScore: number
  weightedScore: number
  fairAdjustedScore: number
  totalDeduction: number
  deductionCount: number
  inspectorId: number
  inspectorName?: string
  inspectedAt: string
  deductions?: DeductionRecord[]
}

export interface DeductionRecord {
  id: number
  targetRecordId: number
  categoryId: number
  categoryName: string
  scoreItemId: number
  itemName: string
  score: number
  scoreType: 'DEDUCTION' | 'ADDITION'
  quantity: number
  studentId: number | null
  studentName?: string
  remark: string
  evidenceUrls: string[]
  isRevoked: boolean
  revokedByAppealId: number | null
}

// ==================== 申诉 ====================

export type AppealStatus =
  | 'PENDING'
  | 'PENDING_LEVEL1_REVIEW'
  | 'PENDING_LEVEL2_REVIEW'
  | 'APPROVED'
  | 'REJECTED'
  | 'WITHDRAWN'

export type AppealType = 'FACT_ERROR' | 'PROCEDURE_ERROR' | 'STANDARD_DISPUTE' | 'OTHER'

export interface Appeal {
  id: number
  deductionId: number
  targetRecordId: number
  appealType: AppealType
  appealReason: string
  evidenceUrls: string[]
  status: AppealStatus
  // 快照归属信息
  classId: number
  className: string
  orgUnitId: number
  orgUnitName: string
  // 审核信息
  level1ReviewerId: number | null
  level1ReviewerName?: string
  level1ReviewedAt: string | null
  level1Comment: string | null
  level2ReviewerId: number | null
  level2ReviewerName?: string
  level2ReviewedAt: string | null
  level2Comment: string | null
  // 创建信息
  createdBy: number
  createdByName?: string
  createdAt: string
}

// ==================== 整改 ====================

export type CorrectiveStatus =
  | 'PENDING'
  | 'IN_PROGRESS'
  | 'SUBMITTED'
  | 'VERIFIED'
  | 'REJECTED'
  | 'COMPLETED'
  | 'OVERDUE'

export interface CorrectiveOrder {
  id: number
  deductionId: number
  targetRecordId: number
  status: CorrectiveStatus
  deadline: string
  currentRound: number
  maxRounds: number
  requirement: string
  // 快照归属
  classId: number
  className: string
  orgUnitId: number
  orgUnitName: string
  // 整改记录
  submissions: CorrectiveSubmission[]
  createdBy: number
  createdAt: string
}

export interface CorrectiveSubmission {
  id: number
  correctiveOrderId: number
  roundNumber: number
  submittedContent: string
  evidenceUrls: string[]
  submittedAt: string
  verifiedBy: number | null
  verifiedAt: string | null
  verifyResult: 'PASSED' | 'FAILED' | null
  verifyComment: string | null
}

// ==================== 汇总 ====================

export interface ClassSummary {
  classId: number
  className: string
  orgUnitId: number
  orgUnitName: string
  averageScore: number
  totalDeduction: number
  inspectionCount: number
  rank: number
  trend: 'UP' | 'DOWN' | 'STABLE'
  trendValue: number
}
```

### 3.2 数据权限类型

```typescript
// frontend/src/types/permission.ts

export type DataScopeType = 'ALL' | 'DEPARTMENT_AND_BELOW' | 'DEPARTMENT' | 'CUSTOM' | 'SELF'
export type ScopeItemType = 'ORG_UNIT' | 'CLASS' | 'GRADE' | 'BUILDING' | 'MAJOR'

export interface DataModule {
  code: string
  name: string
  domain: string
  filterFields: Record<string, string>
}

export interface DataScope {
  code: DataScopeType
  name: string
  priority: number
  calcType: string
}

export interface ScopeItem {
  itemTypeCode: ScopeItemType
  scopeId: number
  scopeName: string
  includeChildren: boolean
}

export interface ModulePermission {
  moduleCode: string
  scopeCode: DataScopeType
  scopeItems: ScopeItem[]
}

export interface RoleDataPermission {
  roleId: number
  roleName: string
  modulePermissions: ModulePermission[]
}

export interface ScopeItemOption {
  id: number
  name: string
  parentName?: string
}
```

---

## 四、路由结构调整

### 4.1 现有路由

```
/inspection
├── /config                    # 量化配置
├── /check-plan                # 检查计划列表
├── /check-plan/create         # 新建计划
├── /check-plan/:id            # 计划详情
├── /check-plan/:id/smart-statistics
├── /daily-checks              # 日常检查（隐藏）
├── /appeals                   # 申诉管理
├── /correctives               # 整改管理
├── /rankings                  # 排名结果
├── /analytics                 # 数据分析
├── /behavior                  # 行为记录
└── /teacher-dashboard         # 教师工作台
```

### 4.2 目标路由

```
/inspection
├── /config                    # 检查配置（模板管理）
│   └── /config/templates/:id  # 模板详情
├── /projects                  # 检查项目列表（原 check-plan）
│   ├── /projects/create       # 新建项目
│   └── /projects/:id          # 项目详情
│       ├── /projects/:id/tasks        # 任务列表
│       ├── /projects/:id/execute      # 检查执行
│       ├── /projects/:id/statistics   # 统计分析
│       └── /projects/:id/export       # 数据导出
├── /tasks                     # 我的检查任务（检查员视角）
│   └── /tasks/:id/execute     # 任务执行
├── /appeals                   # 申诉管理
├── /correctives               # 整改管理
├── /rankings                  # 排名结果
├── /analytics                 # 数据分析中心
├── /behavior                  # 学生行为档案
└── /dashboard                 # 教师工作台
```

### 4.3 路由迁移对照

| 现有路由 | 新路由 | 说明 |
|----------|--------|------|
| `/inspection/check-plan` | `/inspection/projects` | 语义更准确 |
| `/inspection/check-plan/:id` | `/inspection/projects/:id` | - |
| `/inspection/check-plan/create` | `/inspection/projects/create` | - |
| `/inspection/daily-checks` | `/inspection/tasks` | 检查员入口 |
| `/inspection/teacher-dashboard` | `/inspection/dashboard` | 路径简化 |

---

## 五、组件重构

### 5.1 需要重构的组件

| 组件 | 路径 | 重构内容 |
|------|------|----------|
| InspectionPlanList.vue | views/inspection/ | 适配 Project API |
| InspectionPlanDetail.vue | views/inspection/ | 适配 Project + Task API |
| InspectionExecute.vue | views/inspection/ | 适配 Task + Record API |
| AppealManagement.vue | views/inspection/ | 适配新 Appeal API |
| CorrectiveActions.vue | views/inspection/ | 适配新 Corrective API |
| RankingResults.vue | views/inspection/ | 适配 Summary API |
| DataAnalyticsCenter.vue | views/inspection/ | 适配新统计 API |

### 5.2 需要新增的组件

| 组件 | 路径 | 说明 |
|------|------|------|
| ProjectForm.vue | components/inspection/ | 项目创建/编辑表单 |
| TaskList.vue | components/inspection/ | 任务列表组件 |
| TaskExecutePanel.vue | components/inspection/ | 任务执行面板 |
| RecordCard.vue | components/inspection/ | 检查记录卡片 |
| DeductionPicker.vue | components/inspection/ | 扣分项选择器 |
| SummaryChart.vue | components/inspection/ | 汇总图表 |
| RoleDataPermissionDialog.vue | components/permission/ | 数据权限配置（已有） |
| CustomScopeItemDialog.vue | components/permission/ | 自定义范围选择（已有） |

### 5.3 组件重构示例

#### InspectionPlanList → ProjectList

```vue
<!-- 主要变更点 -->
<script setup lang="ts">
// 原来
import { getCheckPlans } from '@/api/inspectionSession'

// 现在
import { projectApi } from '@/api/inspection/project'
import type { InspectionProject, ProjectStatus } from '@/types/inspection'

const projects = ref<InspectionProject[]>([])

async function loadProjects() {
  const res = await projectApi.getList({
    status: statusFilter.value,
    semesterId: semesterFilter.value,
    pageNum: pagination.currentPage,
    pageSize: pagination.pageSize
  })
  projects.value = res.records
  pagination.total = res.total
}

// 状态操作
async function handleActivate(project: InspectionProject) {
  await projectApi.activate(project.id)
  ElMessage.success('项目已启用')
  loadProjects()
}
</script>

<template>
  <!-- 状态标签变更 -->
  <el-tag :type="getStatusType(row.status)">
    {{ getStatusLabel(row.status) }}
  </el-tag>

  <!-- 操作按钮变更 -->
  <el-button v-if="row.status === 'DRAFT'" @click="handleActivate(row)">
    启用
  </el-button>
</template>
```

---

## 六、状态管理

### 6.1 新增 Store

```typescript
// frontend/src/stores/inspection.ts
import { defineStore } from 'pinia'
import { projectApi } from '@/api/inspection/project'
import { taskApi } from '@/api/inspection/task'
import type { InspectionProject, InspectionTask } from '@/types/inspection'

export const useInspectionStore = defineStore('inspection', {
  state: () => ({
    currentProject: null as InspectionProject | null,
    currentTask: null as InspectionTask | null,
    projects: [] as InspectionProject[],
    tasks: [] as InspectionTask[]
  }),

  getters: {
    activeProjects: (state) => state.projects.filter(p => p.status === 'ACTIVE'),
    myTasks: (state) => state.tasks.filter(t => t.status !== 'PUBLISHED')
  },

  actions: {
    async loadProjects() {
      const res = await projectApi.getList({ pageSize: 100 })
      this.projects = res.records
    },

    async loadProject(id: number) {
      this.currentProject = await projectApi.getById(id)
    },

    async loadTasks(projectId: number) {
      const res = await taskApi.getList({ projectId, pageSize: 100 })
      this.tasks = res.records
    },

    async loadTask(id: number) {
      this.currentTask = await taskApi.getById(id)
    }
  }
})
```

### 6.2 权限 Store 扩展

```typescript
// frontend/src/stores/permission.ts (扩展)
import { getDataModules, getRoleDataPermissions } from '@/api/permission/dataPermission'
import type { DataModule, RoleDataPermission } from '@/types/permission'

// 在现有 permission store 中添加
export const usePermissionStore = defineStore('permission', {
  state: () => ({
    // ... 现有状态
    dataModules: {} as Record<string, DataModule[]>,
    roleDataPermissions: {} as Record<number, RoleDataPermission>
  }),

  actions: {
    async loadDataModules() {
      this.dataModules = await getDataModules()
    },

    async loadRoleDataPermissions(roleId: number) {
      this.roleDataPermissions[roleId] = await getRoleDataPermissions(roleId)
    }
  }
})
```

---

## 七、迁移步骤

### 7.1 阶段一：API 层（1周）

1. 创建 `src/api/inspection/` 目录结构
2. 实现新 API 文件（project.ts, task.ts, summary.ts）
3. 重构现有 API 文件
4. 更新类型定义

### 7.2 阶段二：路由和页面（1周）

1. 更新路由配置
2. 重命名现有页面组件
3. 调整页面 API 调用
4. 更新导航菜单

### 7.3 阶段三：组件重构（2周）

1. 重构检查项目相关组件
2. 重构检查任务相关组件
3. 重构检查记录相关组件
4. 新增汇总排名组件

### 7.4 阶段四：数据权限（1周）

1. 实现数据权限 API
2. 完善权限配置组件
3. 集成到角色管理页面

### 7.5 阶段五：测试和优化（1周）

1. 功能测试
2. 兼容性测试
3. 性能优化
4. 文档更新

---

## 八、兼容性处理

### 8.1 API 兼容层（可选）

如需保持旧 API 兼容，可创建适配层：

```typescript
// frontend/src/api/compat/inspectionSession.ts
import { taskApi } from '@/api/inspection/task'

// 将旧 API 调用映射到新 API
export function createSession(data: any) {
  return taskApi.create({
    projectId: data.planId,
    taskDate: data.checkDate
  })
}

export function getSession(id: number) {
  return taskApi.getById(id)
}

// ... 其他兼容方法
```

### 8.2 渐进式迁移

1. 新页面使用新 API
2. 旧页面逐步迁移
3. 确认无引用后移除旧代码

---

## 九、遗漏项补充 - 完整 API 清单

### 9.1 需适配的辅助 API

除核心 API 外，以下 API 文件也需要适配 V5 架构：

| 现有文件 | 重构方式 | 说明 |
|----------|----------|------|
| `inspectionConfig.ts` | 迁移到 `inspection/config.ts` | 权重方案、评级规则配置 |
| `inspectionAnalytics.ts` | 迁移到 `inspection/analytics.ts` | 需适配新的 projectId 参数 |
| `inspectionExport.ts` | 迁移到 `inspection/export.ts` | 需适配新的导出接口 |
| `quickEntry.ts` | 重构为 `inspection/quickEntry.ts` | API路径从 `/quantification/daily-checks` 改为 `/inspection/tasks` |
| `schedule.ts` | 保留，适配 projectId | 排班策略绑定到项目 |
| `behavior.ts` | 保留，添加 taskId 关联 | 行为记录关联检查任务 |
| `departmentRanking.ts` | 合并到 `inspection/summary.ts` | 使用新的汇总 API |
| `teacherDashboard.ts` | 重构，适配新数据结构 | 工作台数据来源变更 |
| `bonusItem.ts` | 迁移到 `inspection/bonus.ts` | 加分项管理 |
| `quantification.ts` | 废弃 | 功能已整合到新 API |
| `quantification-extra.ts` | 废弃 | 功能已整合到新 API |

### 9.2 快捷录入 API 重构

```typescript
// frontend/src/api/inspection/quickEntry.ts
import { http } from '@/utils/request'

const BASE_URL = '/inspection/tasks'

export interface QuickEntryDeductionItem {
  id: number
  itemName: string
  categoryId: number
  categoryName: string
  score: number
  requireEvidence: boolean
}

export interface QuickEntryStudent {
  id: number
  studentNo: string
  realName: string
  classId: number
  className: string
}

export interface QuickEntrySubmitRequest {
  scoreItemId: number
  studentId?: number
  quantity?: number
  remark?: string
  evidenceUrls?: string[]
}

// 获取任务可用扣分项
export function getDeductionItems(taskId: number) {
  return http.get<QuickEntryDeductionItem[]>(`${BASE_URL}/${taskId}/quick-entry/items`)
}

// 搜索学生
export function searchStudents(taskId: number, keyword: string) {
  return http.get<QuickEntryStudent[]>(`${BASE_URL}/${taskId}/quick-entry/students`, {
    params: { keyword }
  })
}

// 提交快捷录入
export function submitQuickEntry(taskId: number, targetId: number, data: QuickEntrySubmitRequest) {
  return http.post(`${BASE_URL}/${taskId}/quick-entry`, { targetId, ...data })
}

export const quickEntryApi = {
  getDeductionItems,
  searchStudents,
  submit: submitQuickEntry
}
```

### 9.3 排班 API 适配

```typescript
// frontend/src/api/inspection/schedule.ts (重构)
import { http } from '@/utils/request'

const BASE_URL = '/inspection/schedules'

export interface SchedulePolicy {
  id: number
  projectId: number           // 新增：绑定到项目
  policyName: string
  algorithm: string
  enabled: boolean
  config: Record<string, unknown>
}

// 获取项目的排班策略
export function getPoliciesByProject(projectId: number) {
  return http.get<SchedulePolicy[]>(`${BASE_URL}/project/${projectId}`)
}

// 创建排班策略
export function createPolicy(data: Omit<SchedulePolicy, 'id'>) {
  return http.post<SchedulePolicy>(BASE_URL, data)
}

// 执行排班（生成任务）
export function executeSchedule(policyId: number, startDate: string, endDate: string) {
  return http.post(`${BASE_URL}/${policyId}/execute`, { startDate, endDate })
}

export const scheduleApi = {
  getByProject: getPoliciesByProject,
  create: createPolicy,
  execute: executeSchedule
}
```

---

## 十、完整 Views 清单

### 10.1 检查模块 Views 重构对照

| 现有 View | 目标 View | 重构内容 |
|-----------|-----------|----------|
| `InspectionConfig.vue` | 保留 | 适配模板新字段（visibility等）|
| `InspectionPlanList.vue` | `ProjectList.vue` | 完全重写，适配 Project API |
| `InspectionPlanDetail.vue` | `ProjectDetail.vue` | 完全重写，包含任务管理 |
| `InspectionExecute.vue` | `TaskExecute.vue` | 重构，适配 Task + Record API |
| `AppealManagement.vue` | 保留 | 适配新申诉状态和两级审核 |
| `CorrectiveActions.vue` | 保留 | 适配新整改状态和多轮验收 |
| `CorrectiveActionListView.vue` | 合并到上面 | 重复组件，删除 |
| `RankingResults.vue` | 保留 | 适配 Summary API |
| `DataAnalyticsCenter.vue` | 保留 | 适配新的统计 API |
| `DataAnalyticsCenterView.vue` | 合并到上面 | 重复组件，删除 |
| `ExportCenterView.vue` | 保留 | 适配新的导出 API |
| `ScheduleManagementView.vue` | 保留 | 适配项目级排班 |
| `BehaviorRecordListView.vue` | 保留 | 添加任务关联 |
| `StudentBehaviorProfile.vue` | 保留 | 数据来源适配 |
| `TeacherDashboard.vue` | 保留 | 数据结构重构 |

### 10.2 需要删除的重复 Views

```
frontend/src/views/inspection/
├── CorrectiveActionListView.vue  # 删除，与 CorrectiveActions.vue 重复
├── DataAnalyticsCenterView.vue   # 删除，与 DataAnalyticsCenter.vue 重复
```

---

## 十一、完整组件清单

### 11.1 现有组件重构

| 组件 | 路径 | 重构内容 |
|------|------|----------|
| `ClassRankingTable.vue` | components/inspection/ | 适配新的 ClassSummary 类型 |
| `CorrectiveOrderCard.vue` | components/inspection/ | 适配新的整改状态和多轮验收 |
| `DeductionItemPicker.vue` | components/inspection/ | 适配模板快照中的扣分项 |
| `PersonalInspectionRecords.vue` | components/inspection/ | 适配新的记录数据结构 |

### 11.2 新增组件（完整清单）

| 组件 | 说明 |
|------|------|
| **项目管理** | |
| `ProjectForm.vue` | 项目创建/编辑表单 |
| `ProjectStatusTag.vue` | 项目状态标签 |
| `ProjectCard.vue` | 项目卡片（用于列表） |
| **任务管理** | |
| `TaskList.vue` | 任务列表组件 |
| `TaskCalendar.vue` | 任务日历视图 |
| `TaskStatusTag.vue` | 任务状态标签 |
| `TaskExecutePanel.vue` | 任务执行面板 |
| `InspectorAssignDialog.vue` | 检查员分配对话框 |
| **检查记录** | |
| `RecordCard.vue` | 检查记录卡片 |
| `RecordDetailDrawer.vue` | 记录详情抽屉 |
| `DeductionList.vue` | 扣分明细列表 |
| `EvidenceUploader.vue` | 证据上传组件 |
| **汇总排名** | |
| `SummaryChart.vue` | 汇总图表 |
| `TrendChart.vue` | 趋势图表 |
| `RankChangeIndicator.vue` | 排名变化指示器 |
| **权限配置** | |
| `RoleDataPermissionDialog.vue` | 数据权限配置（已有） |
| `CustomScopeItemDialog.vue` | 自定义范围选择（已有） |
| `ModulePermissionTable.vue` | 模块权限表格 |

---

## 十二、Store 重构

### 12.1 现有 Store 分析

```typescript
// 现有 stores/inspection.ts 分析
// - 基于旧的 Template/Record/Appeal 结构
// - 需要完全重写以适配 Project/Task/Record 结构
```

### 12.2 重构后的 Store 结构

```typescript
// frontend/src/stores/inspection/index.ts
export * from './project'
export * from './task'
export * from './record'
export * from './appeal'
export * from './corrective'
export * from './summary'
```

```typescript
// frontend/src/stores/inspection/project.ts
import { defineStore } from 'pinia'
import { projectApi } from '@/api/inspection/project'
import type { InspectionProject } from '@/types/inspection'

export const useProjectStore = defineStore('inspection-project', () => {
  const projects = ref<InspectionProject[]>([])
  const currentProject = ref<InspectionProject | null>(null)
  const loading = ref(false)

  async function loadProjects(params?: any) {
    loading.value = true
    try {
      const res = await projectApi.getList(params)
      projects.value = res.records
    } finally {
      loading.value = false
    }
  }

  async function loadProject(id: number) {
    currentProject.value = await projectApi.getById(id)
  }

  return { projects, currentProject, loading, loadProjects, loadProject }
})
```

```typescript
// frontend/src/stores/inspection/task.ts
import { defineStore } from 'pinia'
import { taskApi } from '@/api/inspection/task'
import type { InspectionTask, TargetInspectionRecord } from '@/types/inspection'

export const useTaskStore = defineStore('inspection-task', () => {
  const tasks = ref<InspectionTask[]>([])
  const currentTask = ref<InspectionTask | null>(null)
  const records = ref<TargetInspectionRecord[]>([])
  const loading = ref(false)

  async function loadTasks(projectId: number) {
    loading.value = true
    try {
      const res = await taskApi.getList({ projectId })
      tasks.value = res.records
    } finally {
      loading.value = false
    }
  }

  async function loadTask(id: number) {
    currentTask.value = await taskApi.getById(id)
  }

  async function loadRecords(taskId: number) {
    records.value = await taskApi.getRecords(taskId)
  }

  return { tasks, currentTask, records, loading, loadTasks, loadTask, loadRecords }
})
```

---

## 十三、类型定义补充

### 13.1 遗漏的类型定义

```typescript
// frontend/src/types/inspection.ts (补充)

// ==================== 加分项 ====================

export type BonusMode = 'FIXED' | 'PROGRESSIVE' | 'IMPROVEMENT'

export interface BonusItem {
  id: number
  templateId: number
  categoryId: number
  itemName: string
  bonusMode: BonusMode
  fixedBonus: number
  description: string
  sortOrder: number
  enabled: boolean
}

export interface BonusRecord {
  id: number
  targetRecordId: number
  bonusItemId: number
  bonusScore: number
  reason: string
  createdAt: string
}

// ==================== 排班 ====================

export interface SchedulePolicy {
  id: number
  projectId: number
  policyName: string
  algorithm: 'ROUND_ROBIN' | 'RANDOM' | 'FIXED'
  inspectorIds: number[]
  skipHolidays: boolean
  skipWeekends: boolean
  enabled: boolean
}

export interface ScheduleExecution {
  id: number
  policyId: number
  executionDate: string
  generatedTaskIds: number[]
  status: 'SUCCESS' | 'PARTIAL' | 'FAILED'
  errorMessage: string | null
}

// ==================== 教师工作台 ====================

export interface TeacherDashboardData {
  classId: number
  className: string
  weeklyStats: {
    totalDeduction: number
    totalBonus: number
    inspectionCount: number
    rank: number
    rankChange: number
  }
  topIssues: {
    itemName: string
    categoryName: string
    count: number
    totalDeduction: number
  }[]
  studentAlerts: {
    studentId: number
    studentName: string
    violationCount: number
    totalDeduction: number
  }[]
  trendData: {
    date: string
    score: number
  }[]
}

// ==================== 分析报表 ====================

export interface AnalyticsQuery {
  projectId: number
  startDate: string
  endDate: string
  orgUnitIds?: number[]
  classIds?: number[]
}

export interface ClassRankingData {
  classId: number
  className: string
  orgUnitName: string
  averageScore: number
  rank: number
  trend: 'UP' | 'DOWN' | 'STABLE'
}

export interface ViolationDistribution {
  categoryName: string
  itemName: string
  count: number
  percentage: number
}

export interface InspectorWorkload {
  inspectorId: number
  inspectorName: string
  taskCount: number
  recordCount: number
  averagePerTask: number
}
```

---

## 十四、其他重构项

### 14.1 Composables 适配

```typescript
// frontend/src/composables/useInspectionProject.ts (新增)
import { ref, computed } from 'vue'
import { useProjectStore } from '@/stores/inspection/project'
import { useTaskStore } from '@/stores/inspection/task'

export function useInspectionProject(projectId: Ref<number>) {
  const projectStore = useProjectStore()
  const taskStore = useTaskStore()

  const project = computed(() => projectStore.currentProject)
  const tasks = computed(() => taskStore.tasks)

  async function refresh() {
    await projectStore.loadProject(projectId.value)
    await taskStore.loadTasks(projectId.value)
  }

  return { project, tasks, refresh }
}
```

### 14.2 Utils 适配

```typescript
// frontend/src/utils/inspection.ts (新增)

// 计算任务完成进度
export function calculateTaskProgress(task: InspectionTask): number {
  if (task.targetCount === 0) return 0
  return Math.round((task.completedCount / task.targetCount) * 100)
}

// 获取任务状态颜色
export function getTaskStatusColor(status: TaskStatus): string {
  const colors: Record<TaskStatus, string> = {
    DRAFT: 'info',
    IN_PROGRESS: 'warning',
    SUBMITTED: 'primary',
    REVIEWED: 'success',
    PUBLISHED: ''
  }
  return colors[status] || 'info'
}

// 格式化分数变化
export function formatScoreChange(change: number): string {
  if (change > 0) return `+${change.toFixed(1)}`
  if (change < 0) return change.toFixed(1)
  return '0'
}
```

### 14.3 权限指令（可选）

```typescript
// frontend/src/directives/dataPermission.ts
import type { Directive } from 'vue'
import { usePermissionStore } from '@/stores/permission'

// 数据权限指令：v-data-permission="{ module: 'inspection_record', action: 'view' }"
export const vDataPermission: Directive = {
  mounted(el, binding) {
    const { module, action } = binding.value
    const permissionStore = usePermissionStore()

    // 检查用户是否有该模块的数据权限
    const hasPermission = permissionStore.hasDataPermission(module, action)

    if (!hasPermission) {
      el.parentNode?.removeChild(el)
    }
  }
}
```

---

## 十五、测试计划

### 15.1 单元测试

| 测试对象 | 测试内容 |
|----------|----------|
| API 模块 | 请求参数、响应解析 |
| Store | 状态管理、异步操作 |
| Utils | 工具函数逻辑 |
| Composables | 组合函数行为 |

### 15.2 集成测试

| 测试场景 | 验证点 |
|----------|--------|
| 项目创建流程 | 表单验证 → API调用 → 列表刷新 |
| 任务生成流程 | 日期选择 → 生成 → 任务列表 |
| 检查录入流程 | 选择目标 → 录入扣分 → 提交 |
| 申诉流程 | 提交 → 一级审核 → 二级审核 |

### 15.3 回归测试清单

- [ ] 模板管理功能正常
- [ ] 项目创建/编辑/删除正常
- [ ] 任务生成/分配正常
- [ ] 检查录入各模式正常
- [ ] 申诉提交/审核正常
- [ ] 整改流程正常
- [ ] 排名展示正常
- [ ] 数据权限过滤正常
- [ ] 导出功能正常

---

## 十六、回滚方案

### 16.1 代码回滚

```bash
# 保留旧代码分支
git checkout -b backup/frontend-v4
git push origin backup/frontend-v4

# 回滚到旧版本
git checkout main
git revert HEAD~N  # N = 提交数
```

### 16.2 兼容层保留

在迁移期间保留兼容层，确保：
1. 旧 API 调用能映射到新 API
2. 旧组件能继续工作
3. 旧路由能重定向到新路由

### 16.3 功能开关

```typescript
// frontend/src/config/features.ts
export const FEATURES = {
  USE_V5_INSPECTION: import.meta.env.VITE_USE_V5_INSPECTION === 'true',
  USE_V5_PERMISSION: import.meta.env.VITE_USE_V5_PERMISSION === 'true'
}

// 使用示例
if (FEATURES.USE_V5_INSPECTION) {
  // 使用新的 V5 API
} else {
  // 使用旧的 V4 API
}
```

---

**文档版本**: 3.1
**最后更新**: 2026-01-31
