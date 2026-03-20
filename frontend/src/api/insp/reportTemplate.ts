/**
 * V7 检查平台 - 报表模板 API
 */
import { http } from '@/utils/request'
import type { ReportTemplate, CreateReportTemplateRequest, UpdateReportTemplateRequest } from '@/types/insp/platform'

const BASE = '/v7/insp/report-templates'

// ==================== CRUD ====================

export function listReportTemplates(reportType?: string): Promise<ReportTemplate[]> {
  return http.get<ReportTemplate[]>(BASE, { params: { reportType } })
}

export function getReportTemplate(id: number): Promise<ReportTemplate> {
  return http.get<ReportTemplate>(`${BASE}/${id}`)
}

export function createReportTemplate(data: CreateReportTemplateRequest): Promise<ReportTemplate> {
  return http.post<ReportTemplate>(BASE, data)
}

export function updateReportTemplate(id: number, data: UpdateReportTemplateRequest): Promise<ReportTemplate> {
  return http.put<ReportTemplate>(`${BASE}/${id}`, data)
}

export function deleteReportTemplate(id: number): Promise<void> {
  return http.delete(`${BASE}/${id}`)
}

// ==================== 操作 ====================

export function generateReport(id: number, params: Record<string, unknown>): Promise<Blob> {
  return http.post<Blob>(`${BASE}/${id}/generate`, params, { responseType: 'blob' })
}

// ==================== API 对象 ====================

export const reportTemplateApi = {
  list: listReportTemplates,
  getById: getReportTemplate,
  create: createReportTemplate,
  update: updateReportTemplate,
  delete: deleteReportTemplate,
  generate: generateReport,
}
