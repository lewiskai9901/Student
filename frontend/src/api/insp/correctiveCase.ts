/**
 * V7 检查平台 - 整改管理 API
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

const BASE = '/v7/insp/corrective-cases'

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
