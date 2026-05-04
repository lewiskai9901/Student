/**
 * 检查平台 - 整改管理 API
 */
import { http } from '@/utils/request'
import type {
  CorrectiveCase,
  CreateCaseRequest,
  AssignCaseRequest,
  SubmitCorrectionRequest,
  VerifyCaseRequest,
  RejectCaseRequest,
} from '@/types/insp/corrective'
import type { CaseStatus } from '@/types/insp/enums'

const BASE = '/inspection/corrective-cases'

// ==================== CRUD ====================

export function getCases(params?: {
  projectId?: number
  submissionId?: number
  taskId?: number
  status?: CaseStatus
}): Promise<CorrectiveCase[]> {
  return http.get<CorrectiveCase[]>(BASE, { params })
}

export function getCase(id: number): Promise<CorrectiveCase> {
  return http.get<CorrectiveCase>(`${BASE}/${id}`)
}

export function createCase(data: CreateCaseRequest): Promise<CorrectiveCase> {
  return http.post<CorrectiveCase>(BASE, data)
}

export function getMyCases(): Promise<CorrectiveCase[]> {
  return http.get<CorrectiveCase[]>(`${BASE}/my-cases`)
}

export function getOverdueCases(): Promise<CorrectiveCase[]> {
  return http.get<CorrectiveCase[]>(`${BASE}/overdue`)
}

export function deleteCase(id: number): Promise<void> {
  return http.delete(`${BASE}/${id}`)
}

/** P1#6: 责任人离职批量重派 — 返回受影响案例数 */
export function reassignDepartedUser(
  userId: number,
  data: { reason: string; fallbackAssigneeId?: number; fallbackAssigneeName?: string },
): Promise<number> {
  return http.post<number>(`${BASE}/reassign-departed-user/${userId}`, data)
}

// ==================== Lifecycle ====================

export function assignCase(id: number, data: AssignCaseRequest): Promise<CorrectiveCase> {
  return http.post<CorrectiveCase>(`${BASE}/${id}/assign`, data)
}

export function startWork(id: number): Promise<CorrectiveCase> {
  return http.post<CorrectiveCase>(`${BASE}/${id}/start-work`)
}

export function submitCorrection(id: number, data: SubmitCorrectionRequest): Promise<CorrectiveCase> {
  return http.post<CorrectiveCase>(`${BASE}/${id}/submit-correction`, data)
}

export function verifyCase(id: number, data: VerifyCaseRequest): Promise<CorrectiveCase> {
  return http.post<CorrectiveCase>(`${BASE}/${id}/verify`, data)
}

export function rejectCase(id: number, data: RejectCaseRequest): Promise<CorrectiveCase> {
  return http.post<CorrectiveCase>(`${BASE}/${id}/reject`, data)
}

export function closeCase(id: number): Promise<CorrectiveCase> {
  return http.post<CorrectiveCase>(`${BASE}/${id}/close`)
}

export function escalateCase(id: number): Promise<CorrectiveCase> {
  return http.post<CorrectiveCase>(`${BASE}/${id}/escalate`)
}

// ==================== V110 引擎: 整改候选 ====================

export interface CorrectiveCandidate {
  detailId: number
  itemCode: string
  itemName: string
  severity: 'HIGH' | 'MEDIUM' | 'LOW' | 'NONE'
  severityScore: number
  mustCorrect: boolean
  suggestedDeadlineDays: number
  reason: string
}

/** 拉取某 submission 的引擎建议候选项. */
export function getCorrectiveCandidates(submissionId: number): Promise<CorrectiveCandidate[]> {
  return http.get<CorrectiveCandidate[]>('/inspection/corrective/candidates', {
    params: { submissionId },
  })
}

/** 批量确认建单. */
export function confirmCorrectiveCandidates(
  submissionId: number,
  detailIds: number[],
): Promise<number[]> {
  return http.post<number[]>('/inspection/corrective/candidates/confirm', {
    submissionId,
    detailIds,
  })
}

// ==================== 复发警示 ====================

export interface RecurrenceItem {
  itemCode: string
  itemName: string
  recurCount: number
  lastSeenAt: string | null
}

/** 拉过去 30 天该项目+该主体的复发计数 (按 itemCode 聚合). */
export function getRecurrenceForSubject(
  projectId: number,
  subjectId: number,
): Promise<RecurrenceItem[]> {
  return http.get<RecurrenceItem[]>('/inspection/corrective/recurrence', {
    params: { projectId, subjectId },
  })
}

export const inspCorrectiveCaseApi = {
  getCases,
  getCase,
  createCase,
  getMyCases,
  getOverdueCases,
  deleteCase,
  assignCase,
  startWork,
  submitCorrection,
  verifyCase,
  rejectCase,
  closeCase,
  escalateCase,
}
