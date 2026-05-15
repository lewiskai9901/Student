/**
 * 检查平台 - 整改管理类型
 */
import type { LongId } from '@/types/common'
import type { CaseStatus, CasePriority } from './enums'

export interface CorrectiveCase {
  id: LongId
  tenantId: LongId
  caseCode: string
  /** 整改标题(后端 JOIN 出的派生字段) */
  title?: string | null
  submissionId: LongId | null
  detailId: LongId | null
  projectId: LongId | null
  taskId: LongId | null
  targetType: string | null
  targetId: LongId | null
  targetName: string | null
  issueDescription: string
  requiredAction: string | null
  priority: CasePriority
  deadline: string | null
  assigneeId: LongId | null
  assigneeName: string | null
  escalationLevel: number
  status: CaseStatus
  correctionNote: string | null
  correctionEvidenceIds: number[] | null
  correctedAt: string | null
  verifierId: LongId | null
  verifierName: string | null
  verifiedAt: string | null
  verificationNote: string | null
  createdBy: number | null
  createdAt: string
  updatedAt: string | null
  // V110 引擎产物
  suggestedBySystem?: number | null
  suggestionReason?: string | null
  severityScore?: number | null
  explainTraceJson?: string | null
}

export interface CreateCaseRequest {
  caseCode: string
  issueDescription: string
  priority: CasePriority
  submissionId?: LongId
  detailId?: LongId
  projectId?: LongId
  taskId?: LongId
  targetType?: string
  targetId?: LongId
  targetName?: string
  requiredAction?: string
  deadline?: string
}

export interface AssignCaseRequest {
  assigneeId: LongId
  assigneeName: string
}

export interface SubmitCorrectionRequest {
  correctionNote: string
  evidenceIds: LongId[]
}

export interface VerifyCaseRequest {
  verifierName: string
  note: string
}

export interface RejectCaseRequest {
  verifierName: string
  reason: string
}
