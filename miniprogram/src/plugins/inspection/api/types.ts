import type { LongId } from '@core/types'

// Inspection API types — manually maintained TS mirror of backend DTOs.
// Phase E may switch to openapi-typescript for auto-generation.

export type TaskStatus =
  | 'PENDING' | 'CLAIMED' | 'IN_PROGRESS' | 'SUBMITTED'
  | 'APPROVED' | 'REJECTED' | 'CANCELLED'

export type CaseStatus =
  | 'PENDING' | 'ASSIGNED' | 'IN_PROGRESS' | 'SUBMITTED'
  | 'VERIFIED' | 'REJECTED' | 'CLOSED' | 'ESCALATED'

export type AppealStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'WITHDRAWN'

export type EvidenceType = 'PHOTO' | 'VIDEO' | 'DOCUMENT'

export interface InspEvidence {
  id: LongId
  submissionId: LongId
  detailId?: LongId
  evidenceType: EvidenceType
  fileName: string
  fileUrl: string
  fileSize?: number
  capturedAt?: string
}

export interface InspTask {
  id: LongId
  projectId: LongId
  projectName?: string
  templateId: LongId
  templateVersion?: number
  inspectorId?: LongId
  inspectorName?: string
  status: TaskStatus
  type?: string
  title?: string
  targetType?: string
  targetId?: LongId
  targetName?: string
  orgUnitId?: LongId
  orgUnitName?: string
  scheduledStartAt?: string
  scheduledEndAt?: string
  deadline?: string
  totalScore?: number
  createdAt?: string
  updatedAt?: string
}

export interface CorrectiveCase {
  id: LongId
  caseCode?: string
  projectId: LongId
  projectName?: string
  taskId?: LongId
  submissionId?: LongId
  detailId?: LongId
  itemName?: string
  observation?: string
  rootCause?: string
  status: CaseStatus
  assigneeId?: LongId
  assigneeName?: string
  deadline?: string
  rejectCount?: number
  escalationLevel?: number
  createdAt?: string
  submittedAt?: string
  verifiedAt?: string
}

export interface InspAppeal {
  id: LongId
  submissionDetailId: LongId
  submitterId: LongId
  submitterName?: string
  reason: string
  attachments?: string
  expectedAdjustment?: number
  finalAdjustment?: number
  status: AppealStatus
  reviewerId?: LongId
  reviewerName?: string
  reviewComment?: string
  createdAt?: string
  reviewedAt?: string
}

export type ScoringMode =
  | 'DEDUCTION' | 'ADDITION' | 'DIRECT' | 'PASS_FAIL' | 'LEVEL' | 'SCORE_TABLE'
  | 'CUMULATIVE' | 'TIERED_DEDUCTION' | 'RATING_SCALE' | 'WEIGHTED_MULTI'
  | 'RISK_MATRIX' | 'THRESHOLD' | 'FORMULA'

export interface SubmissionDetail {
  id: LongId
  submissionId: LongId
  templateItemId: LongId
  itemCode: string
  itemName: string
  sectionName?: string
  itemType?: string
  responseValue?: string
  scoringMode?: ScoringMode
  score?: number
  dimensions?: string
  scoringConfig?: string
  validationRules?: string
  itemWeight?: number
  isFlagged?: boolean
  flagReason?: string
  remark?: string
  createdAt?: string
  updatedAt?: string
}

export type SubmissionStatus =
  | 'DRAFT' | 'IN_PROGRESS' | 'COMPLETED' | 'LOCKED' | 'CANCELLED'

export interface InspSubmission {
  id: LongId
  tenantId?: LongId
  taskId: LongId
  targetType?: string
  targetId?: LongId
  targetName?: string
  status: SubmissionStatus
  totalScore?: number
  itemCount?: number
  completedCount?: number
  flaggedCount?: number
  startedAt?: string
  submittedAt?: string
  createdAt?: string
  updatedAt?: string
}
