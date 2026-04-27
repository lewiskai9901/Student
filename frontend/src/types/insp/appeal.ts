import type { AppealStatus } from './enums'

/** 检查申诉 — 与后端 InspAppeal 聚合根字段对齐 */
export interface InspAppeal {
  id: number
  tenantId?: number
  orgUnitId?: number
  appealCode: string
  submissionDetailId: number
  submissionId?: number
  taskId?: number
  projectId?: number
  subjectType?: string
  subjectId?: number
  submitterUserId: number
  submitterName?: string
  reason: string
  attachments?: string
  expectedAdjustment?: number
  finalAdjustment?: number
  status: AppealStatus
  reviewerId?: number
  reviewerName?: string
  reviewerComment?: string
  reviewedAt?: string
  createdAt: string
  updatedAt?: string
}

export interface SubmitAppealRequest {
  submissionDetailId: number
  submitterName?: string
  reason: string
  attachments?: string
  expectedAdjustment?: number
}

export interface ApproveAppealRequest {
  reviewerName?: string
  comment?: string
  finalAdjustment?: number
}

export interface RejectAppealRequest {
  reviewerName?: string
  comment: string
}
