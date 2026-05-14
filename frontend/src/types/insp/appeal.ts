import type { LongId } from '@/types/common'
import type { AppealStatus } from './enums'

/** 检查申诉 — 与后端 InspAppeal 聚合根字段对齐 */
export interface InspAppeal {
  id: LongId
  tenantId?: LongId
  orgUnitId?: LongId
  appealCode: string
  submissionDetailId: LongId
  submissionId?: LongId
  taskId?: LongId
  projectId?: LongId
  subjectType?: string
  subjectId?: LongId
  submitterUserId: LongId
  submitterName?: string
  reason: string
  attachments?: string
  expectedAdjustment?: number
  finalAdjustment?: number
  status: AppealStatus
  reviewerId?: LongId
  reviewerName?: string
  reviewerComment?: string
  reviewedAt?: string
  createdAt: string
  updatedAt?: string
}

export interface SubmitAppealRequest {
  submissionDetailId: LongId
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
