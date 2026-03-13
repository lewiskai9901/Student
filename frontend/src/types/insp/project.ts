/**
 * V7 检查平台 - 执行引擎类型定义
 */
import type {
  ProjectStatus,
  TaskStatus,
  SubmissionStatus,
  ScopeType,
  TargetType,
  CycleType,
  AssignmentMode,
  InspectorRole,
  ScoringMode,
  EvidenceType,
} from './enums'

// ==================== 检查项目 ====================

export interface InspProject {
  id: number
  tenantId: number
  parentProjectId: number | null
  projectCode: string
  projectName: string
  templateId: number
  templateVersionId: number | null
  scoringProfileId: number | null
  scopeType: ScopeType | null
  scopeConfig: string | null
  targetType: TargetType | null
  startDate: string
  endDate: string | null
  cycleType: CycleType | null
  cycleConfig: string | null
  timeSlots: string | null
  skipHolidays: boolean
  holidayCalendarId: number | null
  excludedDates: string | null
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
  templateId: number
  startDate: string
}

export interface UpdateProjectRequest {
  projectName?: string
  templateId?: number
  scoringProfileId?: number | null
  scopeType?: ScopeType
  scopeConfig?: string
  targetType?: TargetType
  startDate?: string
  endDate?: string | null
  cycleType?: CycleType
  cycleConfig?: string
  timeSlots?: string
  skipHolidays?: boolean
  holidayCalendarId?: number | null
  excludedDates?: string
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
  status: TaskStatus
  totalTargets: number
  completedTargets: number
  skippedTargets: number
  reviewComment: string | null
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
  targetType: TargetType
  targetId: number
  targetName: string
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
