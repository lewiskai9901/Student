/**
 * V62 检查平台 - 执行引擎类型定义
 *
 * 核心变更：Project 不再持有 targetType/cycleType/cycleConfig 等调度字段，
 * templateId 改为 rootSectionId。调度由 InspectionPlan 管理。
 */
import type { LongId } from '@/types/common'
import type {
  ProjectStatus,
  TaskStatus,
  SubmissionStatus,
  ScopeType,
  TargetType,
  AssignmentMode,
  InspectorRole,
  ScoringMode,
  EvidenceType,
} from './enums'

// ==================== 检查项目 ====================

export interface InspProject {
  id: LongId
  tenantId: LongId
  projectCode: string
  projectName: string
  rootSectionId: LongId
  templateVersionId: LongId | null
  scoringProfileId: LongId | null
  scopeType: ScopeType | null
  scopeConfig: string | null
  /** 目标类型(STUDENT/CLASS/...) — 决定每个 scope 节点展开成多少个检查目标 */
  targetType?: string | null
  startDate: string
  endDate: string | null
  assignmentMode: AssignmentMode
  reviewRequired: boolean
  autoPublish: boolean
  status: ProjectStatus
  createdBy: number | null
  updatedBy: number | null
  createdAt: string
  updatedAt: string
}

export interface CreateProjectRequest {
  projectName: string
  rootSectionId: LongId
  startDate: string
}

export interface UpdateProjectRequest {
  projectName?: string
  rootSectionId?: LongId
  scoringProfileId?: LongId | null
  scopeType?: ScopeType
  scopeConfig?: string
  startDate?: string
  endDate?: string | null
  assignmentMode?: AssignmentMode
  reviewRequired?: boolean
  autoPublish?: boolean
}

export interface PublishProjectRequest {
  templateVersionId: LongId
}

// ==================== 项目检查员 ====================

export interface ProjectInspector {
  id: LongId
  tenantId: LongId
  projectId: LongId
  userId: LongId
  userName: string
  role: InspectorRole
  isActive: boolean
  createdAt: string
  updatedAt: string
}

export interface AddInspectorRequest {
  userId: LongId
  userName: string
  role: InspectorRole
}

// ==================== 检查任务 ====================

export interface InspTask {
  id: LongId
  tenantId: LongId
  taskCode: string
  projectId: LongId
  taskDate: string
  timeSlotCode: string | null
  timeSlotStart: string | null
  timeSlotEnd: string | null
  inspectorId: LongId | null
  inspectorName: string | null
  reviewerId: LongId | null
  reviewerName: string | null
  assignedSectionIds: string | null
  assignedTargetIds: string | null
  inspectionPlanId: LongId | null
  status: TaskStatus
  totalTargets: number
  completedTargets: number
  skippedTargets: number
  collaborationMode: string | null  // SINGLE / COLLABORATIVE
  reviewComment: string | null
  submittedAt: string | null
  /** P2: 提交时是否已延迟 (today > effective_due_date) */
  lateSubmission?: boolean | null
  /** P2: 延迟天数 (0=按时, >0=延迟天数) */
  lateDays?: number | null
  /** P1#5: 累计驳回次数 */
  rejectionCount?: number | null
  /** P1#5: 驳回延期到的有效日期 (YYYY-MM-DD) */
  extendedTo?: string | null
  /** V108: 任务类型 SCHEDULED/AD_HOC/TRIGGERED/SELF_CHECK/COMPLAINT/CROSS_AUDIT */
  taskType?: 'SCHEDULED' | 'AD_HOC' | 'TRIGGERED' | 'SELF_CHECK' | 'COMPLAINT' | 'CROSS_AUDIT' | null
  /** V108: 逾期策略 STRICT/RELAXED/NONE */
  deadlinePolicy?: 'STRICT' | 'RELAXED' | 'NONE' | null
  /** V108: 来源溯源 */
  sourceType?: string | null
  sourceActorId?: LongId | null
  sourceReason?: string | null
  sourceRefType?: string | null
  sourceRefId?: LongId | null
  createdAt: string
  updatedAt: string
}

export interface CreateTaskRequest {
  projectId: LongId
  taskDate: string
  timeSlotCode?: string
  timeSlotStart?: string
  timeSlotEnd?: string
}

export interface ClaimTaskRequest {
  inspectorName: string
}

export interface ReviewTaskRequest {
  reviewerName: string
  comment: string
}

export interface AssignTaskRequest {
  inspectorId: LongId
  inspectorName: string
}

// ==================== 提交 ====================

export interface InspSubmission {
  id: LongId
  tenantId: LongId
  taskId: LongId
  sectionId: LongId | null
  targetType: TargetType
  targetId: LongId
  targetName: string
  rootTargetId: LongId | null
  rootTargetName: string | null
  orgUnitId: LongId | null
  orgUnitName: string | null
  weightRatio: number
  status: SubmissionStatus
  formData: string | null
  scoreBreakdown: string | null
  baseScore: number | null
  finalScore: number | null
  deductionTotal: number
  bonusTotal: number
  grade: string | null
  passed: boolean | null
  syncVersion: number
  completedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface CreateSubmissionRequest {
  taskId: LongId
  targetType: TargetType
  targetId: LongId
  targetName: string
}

export interface CompleteSubmissionRequest {
  baseScore: number
  finalScore: number
  deductionTotal: number
  bonusTotal: number
  scoreBreakdown: string
  grade: string
  passed: boolean
}

export interface SaveFormDataRequest {
  formData: string
}

// ==================== 提交明细 ====================

export interface SubmissionDetail {
  id: LongId
  tenantId: LongId
  submissionId: LongId
  templateItemId: LongId
  itemCode: string
  itemName: string
  sectionId: LongId | null
  sectionName: string | null
  itemType: string
  responseValue: string | null
  scoringMode: ScoringMode | null
  score: number | null
  dimensions: string | null
  scoringConfig: string | null
  validationRules: string | null
  conditionLogic: string | null
  inputMode?: string
  isFlagged: boolean
  flagReason: string | null
  remark: string | null
  createdAt: string
  updatedAt: string
}

export interface CreateDetailRequest {
  templateItemId: LongId
  itemCode: string
  itemName: string
  itemType: string
}

export interface UpdateDetailResponseRequest {
  responseValue: string
  scoringMode?: ScoringMode
  score?: number
  dimensions?: string
}

export interface FlagDetailRequest {
  reason: string
}

// ==================== 证据 ====================

export interface InspEvidence {
  id: LongId
  tenantId: LongId
  submissionId: LongId
  detailId: LongId | null
  evidenceType: EvidenceType
  fileName: string
  filePath: string | null
  fileUrl: string
  fileSize: number | null
  mimeType: string | null
  thumbnailUrl: string | null
  latitude: number | null
  longitude: number | null
  capturedAt: string | null
  metadata: string | null
  createdAt: string
}

export interface AddEvidenceRequest {
  detailId?: LongId
  evidenceType: EvidenceType
  fileName: string
  fileUrl: string
}

// ==================== 项目分数 ====================

export interface ProjectScore {
  id: LongId
  tenantId: LongId
  projectId: LongId
  cycleDate: string
  score: number | null
  grade: string | null
  targetCount: number
  detail: string | null
  createdAt: string
  updatedAt: string | null
}

// ==================== 检查计划 ====================

export interface InspectionPlan {
  id: LongId
  projectId: LongId
  planName: string
  sectionIds: string  // JSON array
  scheduleMode: 'REGULAR' | 'ON_DEMAND'
  cycleType: string
  frequency: number
  scheduleDays: string | null
  timeSlots: string | null
  skipHolidays: boolean
  isEnabled: boolean
  sortOrder: number
  createdAt: string
  updatedAt: string
}

export interface CreateInspectionPlanRequest {
  projectId: LongId
  planName: string
  sectionIds: string
  scheduleMode?: string
  cycleType?: string
  frequency?: number
  scheduleDays?: string
  timeSlots?: string
  skipHolidays?: boolean
}

export interface UpdateInspectionPlanRequest {
  planName?: string
  sectionIds?: string
  scheduleMode?: string
  cycleType?: string
  frequency?: number
  scheduleDays?: string
  timeSlots?: string
  skipHolidays?: boolean
  isEnabled?: boolean
}

// ==================== 评级维度 ====================

export interface RatingDimension {
  id: LongId
  projectId: LongId
  dimensionName: string
  sectionIds: string  // JSON array
  aggregation: string
  gradeBands: string | null
  awardName: string | null
  rankingEnabled: boolean
  sortOrder: number
  createdAt: string
  updatedAt: string
}

export interface CreateRatingDimensionRequest {
  projectId: LongId
  dimensionName: string
  sectionIds: string
  aggregation?: string
  gradeBands?: string
  awardName?: string
  rankingEnabled?: boolean
}

export interface UpdateRatingDimensionRequest {
  dimensionName?: string
  sectionIds?: string
  aggregation?: string
  gradeBands?: string
  awardName?: string
  rankingEnabled?: boolean
}

export interface RatingResult {
  id: LongId
  dimensionId: LongId
  targetId: LongId
  targetName: string
  targetType: string
  cycleDate: string
  score: number | null
  grade: string | null
  rankNo: number | null
  createdAt: string
}

// ==================== 违纪记录 ====================

export interface ViolationRecord {
  id: LongId
  submissionId: LongId
  submissionDetailId: LongId
  sectionId: LongId | null
  itemId: LongId | null
  userId: LongId
  userName: string
  classInfo: string | null
  occurredAt: string
  severity: 'MINOR' | 'MODERATE' | 'SEVERE'
  description: string | null
  evidenceUrls: string | null
  score: number | null
  createdAt: string
}

export interface CreateViolationRecordRequest {
  submissionId: LongId
  submissionDetailId: LongId
  sectionId?: LongId
  itemId?: LongId
  userId: LongId
  userName: string
  classInfo?: string
  occurredAt: string
  severity?: string
  description?: string
  evidenceUrls?: string
  score?: number
}
