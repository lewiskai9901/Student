import { http } from '@/utils/request'

const BASE = '/enrollment'

export const enrollmentPlanApi = {
  list: (params: any) => http.get(`${BASE}/plans`, { params }),
  create: (data: any) => http.post(`${BASE}/plans`, data),
  update: (id: any, data: any) => http.put(`${BASE}/plans/${id}`, data),
  delete: (id: any) => http.delete(`${BASE}/plans/${id}`),
  publish: (id: any) => http.post(`${BASE}/plans/${id}/publish`),
  statistics: (params?: any) => http.get(`${BASE}/plans/statistics`, { params }),
}

export const enrollmentApplicationApi = {
  list: (params: any) => http.get(`${BASE}/applications`, { params }),
  create: (data: any) => http.post(`${BASE}/applications`, data),
  update: (id: any, data: any) => http.put(`${BASE}/applications/${id}`, data),
  delete: (id: any) => http.delete(`${BASE}/applications/${id}`),
  admit: (id: any) => http.post(`${BASE}/applications/${id}/admit`),
  reject: (id: any, comment?: string) => http.post(`${BASE}/applications/${id}/reject`, { comment }),
  register: (id: any, data: { classId: number }) => http.post(`${BASE}/applications/${id}/register`, data),
  batchAdmit: (ids: number[]) => http.post(`${BASE}/applications/batch-admit`, { ids }),
  export: (params: any) => http.get(`${BASE}/applications/export`, { params, responseType: 'blob' }),
}
