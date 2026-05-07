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
  id: number
  submissionId: number
  detailId?: number
  evidenceType: EvidenceType
  fileName: string
  fileUrl: string
  fileSize?: number
  capturedAt?: string
}

export interface InspTask {
  id: number
  projectId: number
  projectName?: string
  templateId: number
  templateVersion?: number
  inspectorId?: number
  inspectorName?: string
  status: TaskStatus
  type?: string
  title?: string
  targetType?: string
  targetId?: number
  targetName?: string
  orgUnitId?: number
  orgUnitName?: string
  scheduledStartAt?: string
  scheduledEndAt?: string
  deadline?: string
  totalScore?: number
  createdAt?: string
  updatedAt?: string
}

export interface CorrectiveCase {
  id: number
  caseCode?: string
  projectId: number
  projectName?: string
  taskId?: number
  submissionId?: number
  detailId?: number
  itemName?: string
  observation?: string
  rootCause?: string
  status: CaseStatus
  assigneeId?: number
  assigneeName?: string
  deadline?: string
  rejectCount?: number
  escalationLevel?: number
  createdAt?: string
  submittedAt?: string
  verifiedAt?: string
}

export interface InspAppeal {
  id: number
  submissionDetailId: number
  submitterId: number
  submitterName?: string
  reason: string
  attachments?: string
  expectedAdjustment?: number
  finalAdjustment?: number
  status: AppealStatus
  reviewerId?: number
  reviewerName?: string
  reviewComment?: string
  createdAt?: string
  reviewedAt?: string
}
