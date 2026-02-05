# V6 通用检查系统 - API接口设计

> **版本**: 1.0
> **日期**: 2026-02-01
> **关联文档**:
> - [v6-universal-inspection-system.md](./2026-02-01-v6-universal-inspection-system.md)
> - [v6-database-schema.md](./2026-02-01-v6-database-schema.md)

---

## 目录

1. [API概述](#一api概述)
2. [实体类型与分组](#二实体类型与分组)
3. [检查模板](#三检查模板)
4. [检查项目](#四检查项目)
5. [检查任务](#五检查任务)
6. [检查执行](#六检查执行)
7. [汇总与排名](#七汇总与排名)
8. [通用规范](#八通用规范)

---

## 一、API概述

### 1.1 基础路径

```
/api/v6/inspection
```

### 1.2 模块划分

| 模块 | 路径前缀 | 说明 |
|------|---------|------|
| 实体类型 | `/entity-types` | 实体类型配置（系统级） |
| 动态分组 | `/entity-groups` | 动态分组管理 |
| 检查模板 | `/templates` | 模板CRUD和版本管理 |
| 检查项目 | `/projects` | 项目向导创建和管理 |
| 检查任务 | `/tasks` | 任务调度和分配 |
| 检查执行 | `/execution` | 检查执行流程 |
| 汇总统计 | `/summaries` | 汇总和排名 |
| 打分策略 | `/scoring-strategies` | 策略配置 |

### 1.3 认证与权限

所有接口需携带JWT Token:
```
Authorization: Bearer <access_token>
```

数据权限按模块配置，详见 [权限设计文档](../PERMISSION_DESIGN.md)

---

## 二、实体类型与分组

### 2.1 实体类型

#### GET /entity-types
获取实体类型列表

**请求参数:**
```typescript
{
  category?: 'ORGANIZATION' | 'SPACE' | 'USER'  // 筛选分类
  isEnabled?: boolean
}
```

**响应:**
```typescript
{
  code: 0,
  data: {
    organizations: EntityType[],
    spaces: EntityType[],
    users: EntityType[]
  }
}

interface EntityType {
  id: number
  category: 'ORGANIZATION' | 'SPACE' | 'USER'
  typeCode: string
  typeName: string
  parentTypeCode?: string
  isLeaf: boolean
  icon?: string
  color?: string
  attributesSchema?: Record<string, AttributeSchema>
  weightAttribute?: string
  sortOrder: number
  isEnabled: boolean
  isSystem: boolean
}
```

#### GET /entity-types/:typeCode/instances
获取某类型下的所有实例

**请求参数:**
```typescript
{
  parentId?: number       // 父级实体ID
  keyword?: string        // 搜索关键词
  attributes?: object     // 属性筛选条件
  page?: number
  size?: number
}
```

**响应:**
```typescript
{
  code: 0,
  data: {
    list: EntityInstance[],
    total: number
  }
}

interface EntityInstance {
  id: number
  typeCode: string
  name: string
  code?: string
  parentId?: number
  parentName?: string
  attributes: Record<string, any>  // 如 { member_count: 45, enrollment_year: 2024 }
  path: string[]                   // 层级路径
}
```

#### GET /entity-types/:typeCode/tree
获取层级树结构

**请求参数:**
```typescript
{
  rootId?: number         // 根节点ID（不传则从顶级开始）
  maxDepth?: number       // 最大深度
  includeSpaces?: boolean // 是否包含关联场所
  includeUsers?: boolean  // 是否包含关联用户
}
```

**响应:**
```typescript
{
  code: 0,
  data: TreeNode[]
}

interface TreeNode {
  id: number
  typeCode: string
  name: string
  attributes: Record<string, any>
  children?: TreeNode[]
  spaces?: EntityInstance[]   // 关联的场所
  users?: EntityInstance[]    // 关联的用户
}
```

---

### 2.2 动态分组

#### GET /entity-groups
获取分组列表

**请求参数:**
```typescript
{
  entityCategory?: 'ORGANIZATION' | 'SPACE' | 'USER'
  entityTypeCode?: string
  groupType?: 'DYNAMIC' | 'STATIC'
  keyword?: string
}
```

**响应:**
```typescript
{
  code: 0,
  data: EntityGroup[]
}

interface EntityGroup {
  id: number
  groupCode: string
  groupName: string
  description?: string
  groupType: 'DYNAMIC' | 'STATIC'
  entityCategory: string
  entityTypeCode?: string
  filterConditions?: FilterCondition
  cachedMemberCount: number
  cachedAt?: string
  isEnabled: boolean
}

interface FilterCondition {
  conditions: Array<{
    field: string
    operator: '=' | '!=' | '>' | '<' | '>=' | '<=' | 'in' | 'like'
    value: any
  }>
  logic: 'AND' | 'OR'
}
```

#### POST /entity-groups
创建动态分组

**请求体:**
```typescript
{
  groupCode: string
  groupName: string
  description?: string
  groupType: 'DYNAMIC' | 'STATIC'
  entityCategory: string
  entityTypeCode?: string
  filterConditions?: FilterCondition  // DYNAMIC类型必填
  staticMemberIds?: number[]          // STATIC类型必填
  autoRefresh?: boolean
  refreshInterval?: 'HOURLY' | 'DAILY' | 'WEEKLY'
}
```

#### GET /entity-groups/:id/members
获取分组成员

**请求参数:**
```typescript
{
  refresh?: boolean  // 是否强制刷新（DYNAMIC类型）
  page?: number
  size?: number
}
```

**响应:**
```typescript
{
  code: 0,
  data: {
    list: EntityInstance[],
    total: number,
    cachedAt: string
  }
}
```

#### POST /entity-groups/:id/refresh
刷新动态分组缓存

---

## 三、检查模板

### 3.1 模板管理

#### GET /templates
获取模板列表

**请求参数:**
```typescript
{
  visibility?: 'PRIVATE' | 'DEPARTMENT' | 'PUBLIC'
  applicableCategory?: string  // 适用的目标类型
  keyword?: string
  includeDisabled?: boolean
  page?: number
  size?: number
}
```

**响应:**
```typescript
{
  code: 0,
  data: {
    list: TemplateListItem[],
    total: number
  }
}

interface TemplateListItem {
  id: number
  templateCode: string
  templateName: string
  description?: string
  applicableCategories: string[]
  defaultScoringMode: ScoringMode
  defaultBaseScore: number
  visibility: string
  version: number
  useCount: number
  categoryCount: number      // 类别数量
  itemCount: number          // 检查项数量
  createdBy: number
  createdByName: string
  createdAt: string
  updatedAt: string
}
```

#### GET /templates/:id
获取模板详情

**响应:**
```typescript
{
  code: 0,
  data: {
    template: Template,
    categories: Category[],
    items: ScoreItem[]
  }
}

interface Template {
  id: number
  templateCode: string
  templateName: string
  description?: string
  applicableCategories: string[]
  applicableTypes?: string[]
  defaultScoringMode: ScoringMode
  defaultBaseScore: number
  defaultMinScore: number
  defaultAllowNegative: boolean
  defaultFormula?: string
  visibility: string
  version: number
  isLatest: boolean
  parentTemplateId?: number
  useCount: number
  orgUnitId: number
  createdBy: number
  isEnabled: boolean
}

interface Category {
  id: number
  templateId: number
  categoryCode: string
  categoryName: string
  description?: string
  weight: number
  icon?: string
  color?: string
  sortOrder: number
  items: ScoreItem[]  // 嵌套的检查项
}

interface ScoreItem {
  id: number
  templateId: number
  categoryId: number
  itemCode: string
  itemName: string
  description?: string
  applicableCategories?: string[]
  applicableTypes?: string[]
  scope: 'WHOLE' | 'INDIVIDUAL'
  individualType?: string
  scoringMode: ScoringMode
  score: number
  scoreUnit?: string
  gradeOptions?: GradeOption[]
  requireEvidence: boolean
  requireRemark: boolean
  itemWeight: number
  sortOrder: number
}

type ScoringMode = 'DEDUCTION' | 'ADDITION' | 'BASE_SCORE' | 'RATING' | 'GRADE' | 'PASS_FAIL' | 'CHECKLIST' | 'HYBRID'

interface GradeOption {
  code: string
  name: string
  score: number
}
```

#### POST /templates
创建模板

**请求体:**
```typescript
{
  templateCode: string
  templateName: string
  description?: string
  applicableCategories: string[]
  applicableTypes?: string[]
  defaultScoringMode: ScoringMode
  defaultBaseScore?: number
  defaultMinScore?: number
  defaultAllowNegative?: boolean
  defaultFormula?: string
  visibility: string
  categories: CategoryInput[]
}

interface CategoryInput {
  categoryCode: string
  categoryName: string
  description?: string
  weight: number
  icon?: string
  color?: string
  sortOrder: number
  items: ScoreItemInput[]
}

interface ScoreItemInput {
  itemCode: string
  itemName: string
  description?: string
  scope: 'WHOLE' | 'INDIVIDUAL'
  individualType?: string
  scoringMode: ScoringMode
  score: number
  scoreUnit?: string
  gradeOptions?: GradeOption[]
  requireEvidence?: boolean
  requireRemark?: boolean
  itemWeight?: number
  sortOrder: number
}
```

#### PUT /templates/:id
更新模板

#### DELETE /templates/:id
删除模板（逻辑删除）

#### POST /templates/:id/copy
复制模板

**请求体:**
```typescript
{
  newTemplateCode: string
  newTemplateName: string
}
```

#### POST /templates/:id/publish
发布模板（设置可见性为PUBLIC，需审核）

---

### 3.2 类别管理

#### POST /templates/:templateId/categories
添加类别

#### PUT /templates/:templateId/categories/:categoryId
更新类别

#### DELETE /templates/:templateId/categories/:categoryId
删除类别

#### PUT /templates/:templateId/categories/weights
批量更新类别权重

**请求体:**
```typescript
{
  weights: Array<{
    categoryId: number
    weight: number
  }>
}
```

---

### 3.3 检查项管理

#### POST /templates/:templateId/categories/:categoryId/items
添加检查项

#### PUT /templates/:templateId/items/:itemId
更新检查项

#### DELETE /templates/:templateId/items/:itemId
删除检查项

#### PUT /templates/:templateId/items/batch-sort
批量更新排序

**请求体:**
```typescript
{
  items: Array<{
    itemId: number
    sortOrder: number
  }>
}
```

---

## 四、检查项目

### 4.1 项目向导

#### GET /projects/wizard/templates
获取可用模板列表（向导第1步）

**响应:** 同 `GET /templates`，但只返回用户有权限使用的模板

#### GET /projects/wizard/targets
获取可选检查目标（向导第2步）

**请求参数:**
```typescript
{
  targetType: 'ORGANIZATION' | 'SPACE' | 'USER' | 'GROUP'
  parentId?: number
  keyword?: string
}
```

**响应:**
```typescript
{
  code: 0,
  data: {
    organizations: TreeNode[],  // 组织树
    spaces: TreeNode[],         // 场所树
    users: EntityInstance[],    // 用户列表
    groups: EntityGroup[]       // 动态分组
  }
}
```

#### POST /projects/wizard/preview-targets
预览检查目标（向导第2步确认）

**请求体:**
```typescript
{
  rootTargetType: string
  rootTargetIds: number[]
  inspectionDepth: InspectionDepth
}

interface InspectionDepth {
  [level: string]: {
    includeSelf: boolean
    includeSpaces: boolean
    includeUsers: boolean
    includeChildren: boolean
    scoringMode?: ScoringMode
  }
  maxDepth: number
}
```

**响应:**
```typescript
{
  code: 0,
  data: {
    totalTargets: number,
    breakdown: Array<{
      level: number
      category: string
      typeCode: string
      count: number
      sample: string[]  // 示例名称，最多5个
    }>
  }
}
```

#### GET /projects/wizard/scoring-strategies
获取打分策略列表（向导第3步）

**响应:**
```typescript
{
  code: 0,
  data: ScoringStrategy[]
}

interface ScoringStrategy {
  id: number
  strategyCode: string
  strategyName: string
  description?: string
  strategyType: ScoringMode
  config: object
  completionRules?: object
  resultFormat: string
  isSystem: boolean
}
```

---

### 4.2 项目管理

#### GET /projects
获取项目列表

**请求参数:**
```typescript
{
  status?: 'DRAFT' | 'ACTIVE' | 'PAUSED' | 'COMPLETED' | 'ARCHIVED'
  keyword?: string
  startDate?: string
  endDate?: string
  page?: number
  size?: number
}
```

**响应:**
```typescript
{
  code: 0,
  data: {
    list: ProjectListItem[],
    total: number
  }
}

interface ProjectListItem {
  id: number
  projectCode: string
  projectName: string
  templateName: string
  startDate: string
  endDate: string
  frequency: string
  status: string
  rootTargetType: string
  targetCount: number      // 检查目标数量
  totalTasks: number
  completedTasks: number
  progress: number         // 完成百分比
  createdBy: number
  createdByName: string
  createdAt: string
}
```

#### GET /projects/:id
获取项目详情

**响应:**
```typescript
{
  code: 0,
  data: {
    project: Project,
    templateSnapshot: object,  // 模板快照
    recentTasks: Task[],       // 最近5个任务
    statistics: ProjectStats
  }
}

interface Project {
  id: number
  projectCode: string
  projectName: string
  description?: string
  templateId: number
  startDate: string
  endDate: string
  frequency: string
  frequencyConfig?: object
  rootTargetType: string
  rootTargetIds: number[]
  inspectionDepth: InspectionDepth
  scoringMode?: ScoringMode
  baseScore?: number
  minScore?: number
  scoreFormula?: string
  categoryWeights?: Record<string, number>
  weightEnabled: boolean
  spaceWeightMode: string
  orgWeightMode: string
  userWeightMode: string
  fairWeightEnabled: boolean
  fairWeightMode: string
  fairWeightBenchmark: number
  status: string
  totalTasks: number
  completedTasks: number
  orgUnitId: number
  createdBy: number
}

interface ProjectStats {
  totalTasks: number
  completedTasks: number
  totalTargetChecks: number
  avgScore: number
  trend: 'UP' | 'DOWN' | 'STABLE'
}
```

#### POST /projects
创建项目（完整创建，支持向导流程提交）

**请求体:**
```typescript
{
  // 基本信息
  projectCode: string
  projectName: string
  description?: string

  // 模板选择
  templateId: number

  // 时间与频率
  startDate: string
  endDate: string
  frequency: 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'CUSTOM'
  frequencyConfig?: object

  // 检查范围
  rootTargetType: 'ORGANIZATION' | 'SPACE' | 'USER' | 'GROUP'
  rootTargetIds: number[]
  inspectionDepth: InspectionDepth

  // 打分配置
  scoringMode?: ScoringMode
  baseScore?: number
  minScore?: number
  scoreFormula?: string
  categoryWeights?: Record<string, number>

  // 加权配置
  weightEnabled: boolean
  spaceWeightMode?: string
  orgWeightMode?: string
  userWeightMode?: string
  fairWeightEnabled?: boolean
  fairWeightMode?: string
  fairWeightBenchmark?: number

  // 立即激活
  activateImmediately?: boolean
}
```

#### PUT /projects/:id
更新项目（仅DRAFT状态可完整更新）

#### PATCH /projects/:id/status
更新项目状态

**请求体:**
```typescript
{
  status: 'ACTIVE' | 'PAUSED' | 'COMPLETED' | 'ARCHIVED'
  reason?: string
}
```

#### DELETE /projects/:id
删除项目（逻辑删除，仅DRAFT状态可删除）

---

### 4.3 项目检查员管理

#### GET /projects/:id/inspectors
获取项目检查员列表

#### POST /projects/:id/inspectors
添加检查员

**请求体:**
```typescript
{
  userIds: number[]
}
```

#### DELETE /projects/:id/inspectors/:userId
移除检查员

---

## 五、检查任务

### 5.1 任务管理

#### GET /tasks
获取任务列表

**请求参数:**
```typescript
{
  projectId?: number
  status?: 'SCHEDULED' | 'IN_PROGRESS' | 'SUBMITTED' | 'REVIEWED' | 'PUBLISHED'
  scheduledDate?: string       // 精确日期
  startDate?: string           // 范围开始
  endDate?: string             // 范围结束
  inspectorId?: number         // 检查员ID
  page?: number
  size?: number
}
```

**响应:**
```typescript
{
  code: 0,
  data: {
    list: TaskListItem[],
    total: number
  }
}

interface TaskListItem {
  id: number
  taskCode: string
  taskName?: string
  projectId: number
  projectName: string
  scheduledDate: string
  startTime?: string
  endTime?: string
  status: string
  inspectorIds: number[]
  inspectorNames: string[]
  totalTargets: number
  completedTargets: number
  skippedTargets: number
  progress: number
  startedAt?: string
  submittedAt?: string
}
```

#### GET /tasks/:id
获取任务详情

**响应:**
```typescript
{
  code: 0,
  data: {
    task: Task,
    project: ProjectBrief,
    targets: TargetListItem[],
    statistics: TaskStats
  }
}

interface Task {
  id: number
  taskCode: string
  taskName?: string
  projectId: number
  scheduledDate: string
  startTime?: string
  endTime?: string
  status: string
  inspectorIds: number[]
  totalTargets: number
  completedTargets: number
  skippedTargets: number
  startedAt?: string
  submittedAt?: string
  reviewedAt?: string
  publishedAt?: string
  reviewerId?: number
  reviewRemark?: string
}

interface TaskStats {
  totalTargets: number
  pendingTargets: number
  inProgressTargets: number
  completedTargets: number
  skippedTargets: number
  avgScore: number
  minScore: number
  maxScore: number
}
```

#### POST /tasks/generate
生成任务（批量按频率生成）

**请求体:**
```typescript
{
  projectId: number
  startDate: string
  endDate: string
  skipExisting?: boolean  // 跳过已存在的日期
}
```

#### POST /tasks
手动创建任务

**请求体:**
```typescript
{
  projectId: number
  scheduledDate: string
  startTime?: string
  endTime?: string
  inspectorIds?: number[]
}
```

#### PUT /tasks/:id
更新任务

#### DELETE /tasks/:id
删除任务（仅SCHEDULED状态可删除）

---

### 5.2 任务流程

#### POST /tasks/:id/start
开始任务

**响应:**
```typescript
{
  code: 0,
  data: {
    task: Task,
    targets: TargetListItem[]  // 待检查目标列表
  }
}
```

#### POST /tasks/:id/submit
提交任务

**请求体:**
```typescript
{
  remark?: string
}
```

#### POST /tasks/:id/review
审核任务

**请求体:**
```typescript
{
  approved: boolean
  remark?: string
}
```

#### POST /tasks/:id/publish
发布任务

---

### 5.3 检查员分配

#### PUT /tasks/:id/inspectors
分配检查员

**请求体:**
```typescript
{
  inspectorIds: number[]
}
```

#### GET /tasks/:id/available-inspectors
获取可分配的检查员

**响应:**
```typescript
{
  code: 0,
  data: Array<{
    id: number
    name: string
    department: string
    assignedTasks: number  // 当日已分配任务数
  }>
}
```

---

## 六、检查执行

### 6.1 目标列表

#### GET /execution/tasks/:taskId/targets
获取任务的检查目标列表

**请求参数:**
```typescript
{
  status?: 'PENDING' | 'LOCKED' | 'IN_PROGRESS' | 'COMPLETED' | 'SKIPPED'
  category?: string
  typeCode?: string
  belongOrgId?: number
  keyword?: string
  page?: number
  size?: number
}
```

**响应:**
```typescript
{
  code: 0,
  data: {
    list: TargetListItem[],
    total: number,
    statistics: {
      pending: number
      locked: number
      inProgress: number
      completed: number
      skipped: number
    }
  }
}

interface TargetListItem {
  id: number
  taskId: number
  targetCategory: string
  targetTypeCode: string
  targetId: number
  targetName: string
  targetPath: string[]
  depthLevel: number
  belongOrgId: number
  belongOrgName: string
  targetAttributes: object
  status: string
  lockedBy?: number
  lockedByName?: string
  lockExpiresAt?: string
  rawScore?: number
  weightedScore?: number
  resultSummary?: string
  inspectorId?: number
  inspectorName?: string
  completedAt?: string
}
```

#### GET /execution/tasks/:taskId/targets/tree
获取检查目标树形结构（用于嵌套检查导航）

**响应:**
```typescript
{
  code: 0,
  data: TargetTreeNode[]
}

interface TargetTreeNode {
  id: number
  targetCategory: string
  targetTypeCode: string
  targetId: number
  targetName: string
  status: string
  resultSummary?: string
  children?: TargetTreeNode[]
}
```

---

### 6.2 检查执行

#### POST /execution/targets/:targetId/lock
锁定目标（开始检查）

**响应:**
```typescript
{
  code: 0,
  data: {
    target: TargetDetail,
    template: TemplateSnapshot,  // 检查模板快照
    existingDetails: DetailItem[] // 已有的检查明细（断点续查）
  }
}

interface TargetDetail {
  id: number
  targetCategory: string
  targetTypeCode: string
  targetId: number
  targetName: string
  targetPath: string[]
  targetAttributes: object
  belongOrgId: number
  belongOrgName: string
  scoringMode: ScoringMode
  baseScore: number
  status: string
  lockedBy: number
  lockExpiresAt: string

  // 关联实体（用于INDIVIDUAL类型检查项）
  relatedUsers?: EntityInstance[]
  relatedSpaces?: EntityInstance[]
}
```

#### POST /execution/targets/:targetId/unlock
解锁目标（放弃检查）

#### POST /execution/targets/:targetId/extend-lock
延长锁定时间

#### POST /execution/targets/:targetId/details
添加检查明细（单条）

**请求体:**
```typescript
{
  categoryId: number
  itemId: number
  scope: 'WHOLE' | 'INDIVIDUAL'
  individualId?: number      // 当scope=INDIVIDUAL时必填
  scoringMode: ScoringMode
  score: number
  quantity?: number
  gradeCode?: string         // 当scoringMode=GRADE时
  checklistChecked?: boolean // 当scoringMode=CHECKLIST时
  remark?: string
  evidenceUrls?: string[]
}
```

**响应:**
```typescript
{
  code: 0,
  data: {
    detail: DetailItem,
    currentScore: number,     // 当前累计得分
    remainingItems: number    // 剩余未检查项数
  }
}
```

#### POST /execution/targets/:targetId/details/batch
批量添加检查明细

**请求体:**
```typescript
{
  details: Array<{
    categoryId: number
    itemId: number
    scope: 'WHOLE' | 'INDIVIDUAL'
    individualId?: number
    scoringMode: ScoringMode
    score: number
    quantity?: number
    gradeCode?: string
    checklistChecked?: boolean
    remark?: string
    evidenceUrls?: string[]
  }>
}
```

#### PUT /execution/targets/:targetId/details/:detailId
更新检查明细

#### DELETE /execution/targets/:targetId/details/:detailId
删除检查明细

#### GET /execution/targets/:targetId/details
获取目标的检查明细列表

**响应:**
```typescript
{
  code: 0,
  data: {
    details: DetailItem[],
    summary: {
      baseScore: number
      totalDeduction: number
      totalBonus: number
      currentScore: number
      categoryScores: Record<string, CategoryScore>
    }
  }
}

interface DetailItem {
  id: number
  targetRecordId: number
  categoryId: number
  categoryCode: string
  categoryName: string
  itemId: number
  itemCode: string
  itemName: string
  scope: 'WHOLE' | 'INDIVIDUAL'
  individualType?: string
  individualId?: number
  individualName?: string
  scoringMode: string
  score: number
  quantity: number
  totalScore: number
  gradeCode?: string
  gradeName?: string
  checklistChecked?: boolean
  remark?: string
  evidenceUrls: string[]
  createdAt: string
}

interface CategoryScore {
  deduction: number
  bonus: number
  weightedScore: number
}
```

---

### 6.3 完成检查

#### POST /execution/targets/:targetId/complete
完成检查

**请求体:**
```typescript
{
  confirmNoIssues?: boolean  // 确认无问题（扣分模式下如果没有扣分项）
  remark?: string
}
```

**响应:**
```typescript
{
  code: 0,
  data: {
    target: TargetDetail,
    result: {
      scoringMode: string
      baseScore: number
      rawScore: number
      weightedScore: number
      resultSummary: string
      categoryScores: Record<string, CategoryScore>
    },
    nextTarget?: TargetListItem  // 下一个待检查目标（可选）
  }
}
```

#### POST /execution/targets/:targetId/skip
跳过检查

**请求体:**
```typescript
{
  reason: string  // 跳过原因，必填
}
```

---

### 6.4 证据管理

#### POST /execution/evidence/upload
上传证据文件

**请求体:** `multipart/form-data`
```
file: File
targetRecordId?: number
detailId?: number
latitude?: number
longitude?: number
```

**响应:**
```typescript
{
  code: 0,
  data: {
    id: number
    fileName: string
    filePath: string
    fileUrl: string
    fileSize: number
    fileType: string
  }
}
```

#### DELETE /execution/evidence/:id
删除证据文件

---

### 6.5 快捷操作

#### GET /execution/quick-items
获取常用检查项（基于历史使用频率）

**请求参数:**
```typescript
{
  templateId: number
  limit?: number  // 默认10
}
```

**响应:**
```typescript
{
  code: 0,
  data: Array<{
    item: ScoreItem
    useCount: number
  }>
}
```

#### POST /execution/targets/:targetId/quick-deduct
快捷扣分（常用项一键扣分）

**请求体:**
```typescript
{
  itemId: number
  quantity?: number
  remark?: string
}
```

---

## 七、汇总与排名

### 7.1 汇总查询

#### GET /summaries
获取汇总列表

**请求参数:**
```typescript
{
  projectId: number
  periodType: 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'SEMESTER' | 'YEARLY'
  periodStart?: string
  periodEnd?: string
  targetCategory?: string
  targetTypeCode?: string
  parentOrgId?: number   // 按上级组织筛选
  page?: number
  size?: number
  sortBy?: 'ranking' | 'score' | 'name'
  sortOrder?: 'asc' | 'desc'
}
```

**响应:**
```typescript
{
  code: 0,
  data: {
    list: SummaryItem[],
    total: number,
    periodLabel: string,
    statistics: {
      avgScore: number
      maxScore: number
      minScore: number
      participantCount: number
    }
  }
}

interface SummaryItem {
  id: number
  projectId: number
  periodType: string
  periodStart: string
  periodEnd: string
  periodLabel: string
  targetCategory: string
  targetTypeCode: string
  targetId: number
  targetName: string
  targetPath: string[]
  parentOrgId?: number
  parentOrgName?: string
  checkCount: number
  completedCount: number
  skippedCount: number
  avgRawScore: number
  minRawScore: number
  maxRawScore: number
  targetWeight: number
  fairWeightFactor: number
  avgWeightedScore: number
  categoryScores: Record<string, CategoryScoreDetail>
  ranking: number
  rankingTotal: number
  rankingChange: number
  rankingPercentile: number
  scoreChange: number
  trend: 'UP' | 'DOWN' | 'STABLE'
}

interface CategoryScoreDetail {
  avg: number
  weight: number
  weightedAvg: number
}
```

#### GET /summaries/:id
获取汇总详情

**响应:**
```typescript
{
  code: 0,
  data: {
    summary: SummaryItem,
    checkRecords: Array<{
      taskId: number
      taskName: string
      scheduledDate: string
      rawScore: number
      weightedScore: number
      deductionCount: number
    }>,
    categoryBreakdown: Array<{
      categoryCode: string
      categoryName: string
      avgScore: number
      weight: number
      topDeductions: Array<{
        itemName: string
        count: number
        totalScore: number
      }>
    }>
  }
}
```

---

### 7.2 排名查询

#### GET /summaries/ranking
获取排名榜单

**请求参数:**
```typescript
{
  projectId: number
  periodType: string
  periodStart: string
  targetCategory: string
  parentOrgId?: number  // 限定范围
  limit?: number        // 默认20
}
```

**响应:**
```typescript
{
  code: 0,
  data: {
    periodLabel: string,
    rankings: Array<{
      ranking: number
      targetId: number
      targetName: string
      targetPath: string[]
      avgWeightedScore: number
      checkCount: number
      rankingChange: number
      trend: 'UP' | 'DOWN' | 'STABLE'
    }>,
    myRanking?: {  // 当前用户关联目标的排名（如班主任查看自己班级）
      ranking: number
      targetId: number
      targetName: string
      avgWeightedScore: number
    }
  }
}
```

#### GET /summaries/trend
获取趋势数据

**请求参数:**
```typescript
{
  projectId: number
  targetCategory: string
  targetId: number
  periodType: string
  periods?: number  // 最近N个周期，默认10
}
```

**响应:**
```typescript
{
  code: 0,
  data: {
    targetName: string,
    periods: Array<{
      periodLabel: string
      periodStart: string
      avgScore: number
      ranking: number
      rankingTotal: number
    }>
  }
}
```

---

### 7.3 汇总计算

#### POST /summaries/calculate
触发汇总计算

**请求体:**
```typescript
{
  projectId: number
  periodType: string
  periodStart: string
  periodEnd: string
  force?: boolean  // 强制重新计算
}
```

#### POST /summaries/calculate/batch
批量计算汇总

**请求体:**
```typescript
{
  projectId: number
  periodType: string
  startDate: string
  endDate: string
}
```

---

### 7.4 导出

#### POST /summaries/export
导出汇总数据

**请求体:**
```typescript
{
  projectId: number
  periodType: string
  periodStart: string
  periodEnd?: string
  targetCategory?: string
  parentOrgId?: number
  format: 'EXCEL' | 'PDF'
  includeDetails?: boolean
}
```

**响应:**
```typescript
{
  code: 0,
  data: {
    taskId: string,     // 导出任务ID
    status: 'PENDING'
  }
}
```

#### GET /summaries/export/:taskId
获取导出状态/下载

---

## 八、通用规范

### 8.1 响应格式

#### 成功响应
```typescript
{
  code: 0,
  message: "success",
  data: T
}
```

#### 分页响应
```typescript
{
  code: 0,
  data: {
    list: T[],
    total: number,
    page: number,
    size: number,
    pages: number
  }
}
```

#### 错误响应
```typescript
{
  code: number,      // 错误码
  message: string,   // 错误信息
  details?: object   // 详细信息（可选）
}
```

### 8.2 错误码定义

| 错误码 | 说明 |
|-------|------|
| 0 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 409 | 资源冲突（如：目标已被锁定） |
| 422 | 业务规则校验失败 |
| 500 | 服务器内部错误 |

### 8.3 业务错误码

| 错误码 | 说明 |
|-------|------|
| 10001 | 模板不存在 |
| 10002 | 模板已被使用，无法删除 |
| 10003 | 类别权重之和必须为1 |
| 20001 | 项目不存在 |
| 20002 | 项目状态不允许此操作 |
| 20003 | 检查频率配置无效 |
| 30001 | 任务不存在 |
| 30002 | 任务状态不允许此操作 |
| 30003 | 无可用检查员 |
| 40001 | 目标不存在 |
| 40002 | 目标已被其他检查员锁定 |
| 40003 | 锁定已过期 |
| 40004 | 检查未完成，无法提交 |
| 40005 | 扣分模式需确认无问题 |
| 50001 | 汇总数据不存在 |
| 50002 | 汇总计算进行中 |

### 8.4 通用查询参数

| 参数 | 类型 | 说明 |
|------|------|------|
| page | number | 页码，从1开始 |
| size | number | 每页条数，默认20，最大100 |
| sortBy | string | 排序字段 |
| sortOrder | string | 排序方向：asc/desc |
| keyword | string | 搜索关键词 |

### 8.5 日期时间格式

| 类型 | 格式 | 示例 |
|------|------|------|
| 日期 | YYYY-MM-DD | 2026-02-01 |
| 时间 | HH:mm:ss | 14:30:00 |
| 日期时间 | YYYY-MM-DD HH:mm:ss | 2026-02-01 14:30:00 |

---

## 附录

### 附录A: 枚举值定义

#### ScoringMode - 打分模式
```typescript
type ScoringMode =
  | 'DEDUCTION'   // 扣分制
  | 'ADDITION'    // 加分制
  | 'BASE_SCORE'  // 基准分（扣分+加分）
  | 'RATING'      // 评分制（1-10分）
  | 'GRADE'       // 评级制（A/B/C/D）
  | 'PASS_FAIL'   // 通过/不通过
  | 'CHECKLIST'   // 清单制
  | 'HYBRID'      // 混合模式
```

#### TargetStatus - 目标状态
```typescript
type TargetStatus =
  | 'PENDING'      // 待检查
  | 'LOCKED'       // 已锁定
  | 'IN_PROGRESS'  // 检查中
  | 'COMPLETED'    // 已完成
  | 'SKIPPED'      // 已跳过
```

#### TaskStatus - 任务状态
```typescript
type TaskStatus =
  | 'SCHEDULED'    // 已计划
  | 'IN_PROGRESS'  // 进行中
  | 'SUBMITTED'    // 已提交
  | 'REVIEWED'     // 已审核
  | 'PUBLISHED'    // 已发布
```

#### ProjectStatus - 项目状态
```typescript
type ProjectStatus =
  | 'DRAFT'        // 草稿
  | 'ACTIVE'       // 进行中
  | 'PAUSED'       // 已暂停
  | 'COMPLETED'    // 已完成
  | 'ARCHIVED'     // 已归档
```

---

**文档版本**: 1.0
**最后更新**: 2026-02-01
