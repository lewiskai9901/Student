/**
 * V7 检查平台 - 提交 API
 */
import { http } from '@/utils/request'
import type {
  InspSubmission,
  CreateSubmissionRequest,
  CompleteSubmissionRequest,
  SaveFormDataRequest,
  SubmissionDetail,
  CreateDetailRequest,
  UpdateDetailResponseRequest,
  FlagDetailRequest,
  InspEvidence,
  AddEvidenceRequest,
} from '@/types/insp/project'

const BASE = '/v7/insp/submissions'

// ==================== 提交 CRUD ====================

export function getSubmissions(params?: {
  taskId?: number
  targetId?: number
}): Promise<InspSubmission[]> {
  return http.get<InspSubmission[]>(BASE, { params })
}

export function getSubmission(id: number): Promise<InspSubmission> {
  return http.get<InspSubmission>(`${BASE}/${id}`)
}

export function createSubmission(data: CreateSubmissionRequest): Promise<InspSubmission> {
  return http.post<InspSubmission>(BASE, data)
}

// ==================== 提交生命周期 ====================

export function lockSubmission(id: number): Promise<InspSubmission> {
  return http.post<InspSubmission>(`${BASE}/${id}/lock`)
}

export function unlockSubmission(id: number): Promise<InspSubmission> {
  return http.post<InspSubmission>(`${BASE}/${id}/unlock`)
}

export function startFilling(id: number): Promise<InspSubmission> {
  return http.post<InspSubmission>(`${BASE}/${id}/start-filling`)
}

export function saveFormData(id: number, data: SaveFormDataRequest): Promise<InspSubmission> {
  return http.put<InspSubmission>(`${BASE}/${id}/form-data`, data)
}

export function completeSubmission(id: number, data: CompleteSubmissionRequest): Promise<InspSubmission> {
  return http.post<InspSubmission>(`${BASE}/${id}/complete`, data)
}

export function skipSubmission(id: number): Promise<InspSubmission> {
  return http.post<InspSubmission>(`${BASE}/${id}/skip`)
}

// ==================== 明细 ====================

export function getDetails(submissionId: number): Promise<SubmissionDetail[]> {
  return http.get<SubmissionDetail[]>(`${BASE}/${submissionId}/details`)
}

export function getFlaggedDetails(submissionId: number): Promise<SubmissionDetail[]> {
  return http.get<SubmissionDetail[]>(`${BASE}/${submissionId}/details/flagged`)
}

export function createDetail(submissionId: number, data: CreateDetailRequest): Promise<SubmissionDetail> {
  return http.post<SubmissionDetail>(`${BASE}/${submissionId}/details`, data)
}

export function updateDetailResponse(detailId: number, data: UpdateDetailResponseRequest): Promise<SubmissionDetail> {
  return http.put<SubmissionDetail>(`${BASE}/details/${detailId}/response`, data)
}

export function updateDetailRemark(detailId: number, remark: string): Promise<SubmissionDetail> {
  return http.put<SubmissionDetail>(`${BASE}/details/${detailId}/remark`, { remark })
}

export function flagDetail(detailId: number, data: FlagDetailRequest): Promise<SubmissionDetail> {
  return http.post<SubmissionDetail>(`${BASE}/details/${detailId}/flag`, data)
}

export function unflagDetail(detailId: number): Promise<SubmissionDetail> {
  return http.post<SubmissionDetail>(`${BASE}/details/${detailId}/unflag`)
}

export function deleteDetail(detailId: number): Promise<void> {
  return http.delete(`${BASE}/details/${detailId}`)
}

// ==================== 证据 ====================

export function getEvidence(submissionId: number): Promise<InspEvidence[]> {
  return http.get<InspEvidence[]>(`${BASE}/${submissionId}/evidences`)
}

export function addEvidence(submissionId: number, data: AddEvidenceRequest): Promise<InspEvidence> {
  return http.post<InspEvidence>(`${BASE}/${submissionId}/evidences`, data)
}

export function deleteEvidence(evidenceId: number): Promise<void> {
  return http.delete(`${BASE}/evidences/${evidenceId}`)
}

// ==================== API 对象 ====================

export const inspSubmissionApi = {
  getList: getSubmissions,
  getById: getSubmission,
  create: createSubmission,
  lock: lockSubmission,
  unlock: unlockSubmission,
  startFilling,
  saveFormData,
  complete: completeSubmission,
  skip: skipSubmission,
  getDetails,
  getFlaggedDetails,
  createDetail,
  updateDetailResponse,
  updateDetailRemark,
  flagDetail,
  unflagDetail,
  deleteDetail,
  getEvidence,
  addEvidence,
  deleteEvidence,
}
