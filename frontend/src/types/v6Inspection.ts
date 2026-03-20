/**
 * V6检查系统类型定义
 */

// ========== 枚举类型 ==========

export type ProjectStatus = 'DRAFT' | 'ACTIVE' | 'PAUSED' | 'COMPLETED' | 'ARCHIVED'
export type TaskStatus = 'PENDING' | 'IN_PROGRESS' | 'SUBMITTED' | 'REVIEWED' | 'PUBLISHED' | 'CANCELLED'
export type TargetStatus = 'PENDING' | 'LOCKED' | 'COMPLETED' | 'SKIPPED'
export type ScopeType = 'ORG' | 'PLACE' | 'USER'
export type CycleType = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'CUSTOM'
export type SharedPlaceStrategy = 'RATIO' | 'AVERAGE' | 'FULL' | 'MAIN_ONLY'
export type InspectorAssignmentMode = 'FREE' | 'ASSIGNED' | 'HYBRID'
export type TargetType = 'ORG' | 'PLACE' | 'USER'

// ========== 检查项目 ==========

export interface InspectionProject {
  id: number | string
  projectCode: string
  projectName: string
  description?: string
  templateId: number | string
  templateSnapshot?: string
  scopeType: ScopeType
  scopeConfig?: string
  startDate: string
  endDate?: string
  cycleType: CycleType
  cycleConfig?: string
  timeSlots?: string
  skipHolidays: boolean
  sharedPlaceStrategy: SharedPlaceStrategy
  inspectorAssignmentMode: InspectorAssignmentMode
  status: ProjectStatus
  statusLabel: string
  publishedAt?: string
  totalTasks: number
  completedTasks: number
  createdAt: string
}

export interface CreateProjectRequest {
  projectCode: string
  projectName: string
  description?: string
  templateId?: number | string
  scopeType?: 'ORGANIZATION' | 'PLACE' | 'USER'
  entityTypeCode?: string
  scopeIds?: string
  scoringMode?: string
  baseScore?: number
  maxScore?: number
  minScore?: number
  allowDecimal?: boolean
  cycleType?: string
  startDate?: string
  endDate?: string
  inspectorAssignmentMode?: string
  autoCreateTasks?: boolean
  enableReminder?: boolean
}

export interface UpdateProjectConfigRequest {
  scopeType: ScopeType
  scopeConfig: string
  startDate: string
  endDate?: string
  cycleType: CycleType
  cycleConfig?: string
  timeSlots?: string
  skipHolidays: boolean
  sharedPlaceStrategy: SharedPlaceStrategy
  inspectorAssignmentMode: InspectorAssignmentMode
}

export interface PublishProjectRequest {
  templateSnapshot?: string
}

// ========== 检查任务 ==========

export interface InspectionTask {
  id: number | string
  taskCode: string
  projectId: number | string
  projectName?: string
  taskDate: string
  timeSlot?: string
  status: TaskStatus
  statusLabel: string
  inspectorId?: number | string
  inspectorName?: string
  claimedAt?: string
  startedAt?: string
  submittedAt?: string
  publishedAt?: string
  totalTargets: number
  completedTargets: number
  skippedTargets: number
  createdAt: string
}

// ========== 检查目标 ==========

export interface InspectionTarget {
  id: number | string
  taskId: number | string
  targetType: TargetType
  targetId: number | string
  targetName: string
  targetCode?: string
  orgUnitId?: number | string
  orgUnitName?: string
  classId?: number | string
  className?: string
  weightRatio: number
  status: TargetStatus
  statusLabel: string
  baseScore: number
  finalScore?: number
  deductionTotal: number
  bonusTotal: number
}

// ========== 通用响应 ==========

export interface PagedResponse<T> {
  list: T[]
  total: number
  page: number
  size: number
}

export interface EnumOption {
  code: string
  label: string
  description?: string
}

export interface ProjectOptions {
  scopeTypes: EnumOption[]
  cycleTypes: EnumOption[]
  sharedPlaceStrategies: EnumOption[]
  inspectorAssignmentModes: EnumOption[]
  projectStatuses: EnumOption[]
}

// ========== 查询参数 ==========

export interface ProjectQueryParams {
  page?: number
  size?: number
  status?: ProjectStatus
  keyword?: string
}

export interface TaskQueryParams {
  page?: number
  size?: number
  projectId?: number | string
  status?: TaskStatus
  startDate?: string
  endDate?: string
  inspectorId?: number | string
}
