import { http } from '@/utils/request'
import type { ViolationRecord, CreateViolationRecordRequest } from '@/types/insp/project'

const BASE = '/v7/insp/violation-records'

export function getViolationRecordsBySubmission(submissionId: number): Promise<ViolationRecord[]> {
  return http.get<ViolationRecord[]>(BASE, { params: { submissionId } })
}

export function getViolationRecordsByDetail(detailId: number): Promise<ViolationRecord[]> {
  return http.get<ViolationRecord[]>(BASE, { params: { detailId } })
}

export function createViolationRecord(data: CreateViolationRecordRequest): Promise<ViolationRecord> {
  return http.post<ViolationRecord>(BASE, data)
}

export function deleteViolationRecord(id: number): Promise<void> {
  return http.delete(`${BASE}/${id}`)
}
