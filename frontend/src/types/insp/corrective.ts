/**
 * V7 检查平台 - 整改管理类型
 */
import type { CaseStatus, CasePriority } from './enums'

export interface CorrectiveCase {
  id: number
  tenantId: number
  caseCode: string
  submissionId: number | null
  detailId: number | null
  projectId: number | null
  taskId: number | null
  targetType: string | null
  targetId: number | null
  targetName: string | null
  issueDescription: string
  requiredAction: string | null
  priority: CasePriority
  deadline: string | null
  assigneeId: number | null
  assigneeName: string | null
  escalationLevel: number
  status: CaseStatus
  correctionNote: string | null
  correctionEvidenceIds: number[] | null
  correctedAt: string | null
  verifierId: number | null
  verifierName: string | null
  verifiedAt: string | null
  verificationNote: string | null
  createdBy: number | null
  createdAt: string
  updatedAt: string | null
}

export interface CreateCaseRequest {
  caseCode: string
  issueDescription: string
  priority: CasePriority
  submissionId?: number
  detailId?: number
  projectId?: number
  taskId?: number
  targetType?: string
  targetId?: number
  targetName?: string
  requiredAction?: string
  deadline?: string
}

export interface AssignCaseRequest {
  assigneeId: number
  assigneeName: string
}

export interface SubmitCorrectionRequest {
  correctionNote: string
  evidenceIds: number[]
}

export interface VerifyCaseRequest {
  verifierName: string
  note: string
}

export interface RejectCaseRequest {
  verifierName: string
  reason: string
}
