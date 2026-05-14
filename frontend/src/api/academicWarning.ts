import type { LongId } from '@/types/common'
import { http as request } from '@/utils/request'
import type { WarningRule, AcademicWarning, WarningStatistics } from '@/types/academicWarning'

const BASE = '/academic-warnings'

// ==================== 预警规则 ====================

export function getRules(): Promise<WarningRule[]> {
  return request.get(`${BASE}/rules`)
}

export function createRule(data: Partial<WarningRule>): Promise<number> {
  return request.post(`${BASE}/rules`, data)
}

export function updateRule(id: LongId, data: Partial<WarningRule>): Promise<void> {
  return request.put(`${BASE}/rules/${id}`, data)
}

export function deleteRule(id: LongId): Promise<void> {
  return request.delete(`${BASE}/rules/${id}`)
}

export function toggleRule(id: LongId): Promise<void> {
  return request.post(`${BASE}/rules/${id}/toggle`)
}

// ==================== 预警扫描 ====================

export function scanWarnings(semesterId: LongId): Promise<{ totalWarnings: number; rulesScanned: number }> {
  return request.post(`${BASE}/scan`, null, { params: { semesterId } })
}

export function previewScan(semesterId: LongId): Promise<AcademicWarning[]> {
  return request.get(`${BASE}/scan/preview`, { params: { semesterId } })
}

// ==================== 预警记录 ====================

export function getWarnings(params: {
  warningLevel?: number
  status?: number
  orgUnitId?: LongId
  studentId?: LongId
  warningType?: string
  semesterId?: LongId
  pageNum?: number
  pageSize?: number
}): Promise<{ records: AcademicWarning[]; total: number }> {
  return request.get(BASE, { params })
}

export function getWarningDetail(id: LongId): Promise<AcademicWarning> {
  return request.get(`${BASE}/${id}`)
}

export function confirmWarning(id: LongId): Promise<void> {
  return request.post(`${BASE}/${id}/confirm`)
}

export function interveneWarning(id: LongId, note: string): Promise<void> {
  return request.post(`${BASE}/${id}/intervene`, null, { params: { note } })
}

export function dismissWarning(id: LongId, note: string): Promise<void> {
  return request.post(`${BASE}/${id}/dismiss`, null, { params: { note } })
}

export function getStatistics(semesterId?: LongId): Promise<WarningStatistics> {
  return request.get(`${BASE}/statistics`, { params: { semesterId } })
}

export function getStudentWarnings(studentId: LongId): Promise<AcademicWarning[]> {
  return request.get(`${BASE}/by-student/${studentId}`)
}
