/**
 * 量化检查 API - 桩文件
 *
 * 这些 API 对象尚未完整实现，仅提供最小定义以支持 stores/inspection.ts 编译。
 * 完整实现时请替换此文件。
 */
import { http } from '@/utils/request'

const TEMPLATE_URL = '/inspection-templates'
const RECORD_URL = '/inspection-records'
const APPEAL_URL = '/inspection-appeals'

/**
 * 检查模板 API 对象 (桩)
 */
export const templateApi = {
  getList: (params?: any): Promise<any> => http.get(TEMPLATE_URL, { params }),
  getById: (id: number): Promise<any> => http.get(`${TEMPLATE_URL}/${id}`),
  create: (data: any): Promise<any> => http.post(TEMPLATE_URL, data),
  update: (id: number, data: any): Promise<any> => http.put(`${TEMPLATE_URL}/${id}`, data),
  delete: (id: number): Promise<any> => http.delete(`${TEMPLATE_URL}/${id}`),
  publish: (id: number): Promise<any> => http.post(`${TEMPLATE_URL}/${id}/publish`),
  archive: (id: number): Promise<any> => http.post(`${TEMPLATE_URL}/${id}/archive`),
  addCategory: (templateId: number, data: any): Promise<any> =>
    http.post(`${TEMPLATE_URL}/${templateId}/categories`, data)
}

/**
 * 检查记录 API 对象 (桩)
 */
export const recordApi = {
  getList: (params?: any): Promise<any> => http.get(RECORD_URL, { params }),
  getById: (id: number): Promise<any> => http.get(`${RECORD_URL}/${id}`),
  create: (data: any): Promise<any> => http.post(RECORD_URL, data),
  delete: (id: number): Promise<any> => http.delete(`${RECORD_URL}/${id}`),
  submit: (id: number): Promise<any> => http.post(`${RECORD_URL}/${id}/submit`),
  publish: (id: number): Promise<any> => http.post(`${RECORD_URL}/${id}/publish`),
  reject: (id: number, data: any): Promise<any> => http.post(`${RECORD_URL}/${id}/reject`, data),
  addClassScore: (recordId: number, data: any): Promise<any> =>
    http.post(`${RECORD_URL}/${recordId}/class-scores`, data),
  recordDeduction: (recordId: number, data: any): Promise<any> =>
    http.post(`${RECORD_URL}/${recordId}/deductions`, data)
}

/**
 * 申诉 API 对象 (桩)
 */
export const appealApi = {
  getList: (params?: any): Promise<any> => http.get(APPEAL_URL, { params }),
  getById: (id: number): Promise<any> => http.get(`${APPEAL_URL}/${id}`),
  create: (data: any): Promise<any> => http.post(APPEAL_URL, data),
  submitForReview: (id: number): Promise<any> => http.post(`${APPEAL_URL}/${id}/submit`),
  review: (id: number, data: any): Promise<any> => http.post(`${APPEAL_URL}/${id}/review`, data),
  finalReview: (id: number, data: any): Promise<any> =>
    http.post(`${APPEAL_URL}/${id}/final-review`, data),
  getStatistics: (): Promise<any> => http.get(`${APPEAL_URL}/statistics`),
  getMyAppeals: (params?: any): Promise<any> => http.get(`${APPEAL_URL}/my`, { params }),
  getPendingReview: (params?: any): Promise<any> =>
    http.get(`${APPEAL_URL}/pending-review`, { params })
}
