/**
 * V62 检查平台 - 执行引擎类型定义
 *
 * 核心变更：Project 不再持有 targetType/cycleType/cycleConfig 等调度字段，
 * templateId 改为 rootSectionId。调度由 InspectionPlan 管理。
 */
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
  id: number
  tenantId: number
  projectCode: string
  projectName: string
  rootSectionId: number
  templateVersionId: number | null
  scoringProfileId: number | null
  scopeType: ScopeType | null
  scopeConfig: string | null
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
  rootSectionId: number
  startDate: string
}

export interface UpdateProjectRequest {
  projectName?: string
  rootSectionId?: number
  scoringProfileId?: number | null
  scopeType?: ScopeType
  scopeConfig?: string
  startDate?: string
  endDate?: string | null
  assignmentMode?: AssignmentMode
  reviewRequired?: boolean
  autoPublish?: boolean
}

export interface PublishProjectRequest {
  templateVersionId: number
}

// ==================== 项目检查员 ====================

export interface ProjectInspector {
  id: number
  tenantId: number
  projectId: number
  userId: number
  userName: string
  role: InspectorRole
  isActive: boolean
  createdAt: string
  updatedAt: string
}

export interface AddInspectorRequest {
  userId: number
  userName: string
  role: InspectorRole
}

// ==================== 检查任务 ====================

export interface InspTask {
  id: number
  tenantId: number
  taskCode: string
  projectId: number
  taskDate: string
  timeSlotCode: string | null
  timeSlotStart: string | null
  timeSlotEnd: string | null
  inspectorId: number | null
  inspectorName: string | null
  reviewerId: number | null
  reviewerName: string | null
  assignedSectionIds: string | null
  assignedTargetIds: string | null
  inspectionPlanId: number | null
  status: TaskStatus
  totalTargets: number
  completedTargets: number
  skippedTargets: number
  collaborationMode: string | null  // SINGLE / COLLABORATIVE
  reviewComment: string | null
  submittedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface CreateTaskRequest {
  projectId: number
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
  inspectorId: number
  inspectorName: string
}

// ==================== 提交 ====================

export interface InspSubmission {
  id: number
  tenantId: number
  taskId: number
  sectionId: number | null
  targetType: TargetType
  targetId: number
  targetName: string
  rootTargetId: number | null
  rootTargetName: string | null
  orgUnitId: number | null
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
  taskId: number
  targetType: TargetType
  targetId: number
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
  id: number
  tenantId: number
  submissionId: number
  templateItemId: number
  itemCode: string
  itemName: string
  sectionId: number | null
  sectionName: string | null
  itemType: string
  responseValue: string | null
  scoringMode: ScoringMode | null
  score: number | null
  dimensions: string | null
  scoringConfig: string | null
  validationRules: string | null
  conditionLogic: string | null
  isFlagged: boolean
  flagReason: string | null
  remark: string | null
  createdAt: string
  updatedAt: string
}

export interface CreateDetailRequest {
  templateItemId: number
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
  id: number
  tenantId: number
  submissionId: number
  detailId: number | null
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
  detailId?: number
  evidenceType: EvidenceType
  fileName: string
  fileUrl: string
}

// ==================== 项目分数 ====================

export interface ProjectScore {
  id: number
  tenantId: number
  projectId: number
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
  id: number
  projectId: number
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
  projectId: number
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
  id: number
  projectId: number
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
  projectId: number
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
  id: number
  dimensionId: number
  targetId: number
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
  id: number
  submissionId: number
  submissionDetailId: number
  sectionId: number | null
  itemId: number | null
  userId: number
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
  submissionId: number
  submissionDetailId: number
  sectionId?: number
  itemId?: number
  userId: number
  userName: string
  classInfo?: string
  occurredAt: string
  severity?: string
  description?: string
  evidenceUrls?: string
  score?: number
}
