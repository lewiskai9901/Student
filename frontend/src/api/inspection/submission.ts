/**
 * 检查平台 - 提交 API
 */
import type { LongId } from '@/types/common'
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

const BASE = '/inspection/submissions'

// ==================== 提交 CRUD ====================

export function getSubmissions(params?: {
  taskId?: LongId
  targetId?: LongId
}): Promise<InspSubmission[]> {
  return http.get<InspSubmission[]>(BASE, { params })
}

export function getSubmission(id: LongId): Promise<InspSubmission> {
  return http.get<InspSubmission>(`${BASE}/${id}`)
}

export function createSubmission(data: CreateSubmissionRequest): Promise<InspSubmission> {
  return http.post<InspSubmission>(BASE, data)
}

// ==================== 提交生命周期 ====================

export function lockSubmission(id: LongId): Promise<InspSubmission> {
  return http.post<InspSubmission>(`${BASE}/${id}/lock`)
}

export function unlockSubmission(id: LongId): Promise<InspSubmission> {
  return http.post<InspSubmission>(`${BASE}/${id}/unlock`)
}

export function startFilling(id: LongId): Promise<InspSubmission> {
  return http.post<InspSubmission>(`${BASE}/${id}/start-filling`)
}

export function saveFormData(id: LongId, data: SaveFormDataRequest): Promise<InspSubmission> {
  return http.put<InspSubmission>(`${BASE}/${id}/form-data`, data)
}

export function completeSubmission(id: LongId, data: CompleteSubmissionRequest): Promise<InspSubmission> {
  return http.post<InspSubmission>(`${BASE}/${id}/complete`, data)
}

export function skipSubmission(id: LongId): Promise<InspSubmission> {
  return http.post<InspSubmission>(`${BASE}/${id}/skip`)
}

// ==================== 明细 ====================

export function getDetails(submissionId: LongId): Promise<SubmissionDetail[]> {
  return http.get<SubmissionDetail[]>(`${BASE}/${submissionId}/details`)
}

export function getFlaggedDetails(submissionId: LongId): Promise<SubmissionDetail[]> {
  return http.get<SubmissionDetail[]>(`${BASE}/${submissionId}/details/flagged`)
}

export function createDetail(submissionId: LongId, data: CreateDetailRequest): Promise<SubmissionDetail> {
  return http.post<SubmissionDetail>(`${BASE}/${submissionId}/details`, data)
}

export function updateDetailResponse(detailId: LongId, data: UpdateDetailResponseRequest): Promise<SubmissionDetail> {
  return http.put<SubmissionDetail>(`${BASE}/details/${detailId}/response`, data)
}

export function updateDetailRemark(detailId: LongId, remark: string): Promise<SubmissionDetail> {
  return http.put<SubmissionDetail>(`${BASE}/details/${detailId}/remark`, { remark })
}

export function flagDetail(detailId: LongId, data: FlagDetailRequest): Promise<SubmissionDetail> {
  return http.post<SubmissionDetail>(`${BASE}/details/${detailId}/flag`, data)
}

export function unflagDetail(detailId: LongId): Promise<SubmissionDetail> {
  return http.post<SubmissionDetail>(`${BASE}/details/${detailId}/unflag`)
}

export function deleteDetail(detailId: LongId): Promise<void> {
  return http.delete(`${BASE}/details/${detailId}`)
}

// ==================== 证据 ====================

export function getEvidence(submissionId: LongId): Promise<InspEvidence[]> {
  return http.get<InspEvidence[]>(`${BASE}/${submissionId}/evidences`)
}

export function addEvidence(submissionId: LongId, data: AddEvidenceRequest): Promise<InspEvidence> {
  return http.post<InspEvidence>(`${BASE}/${submissionId}/evidences`, data)
}

export function deleteEvidence(evidenceId: LongId): Promise<void> {
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
