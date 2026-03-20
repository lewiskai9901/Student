import { http } from '@/utils/request'
import type { Major, MajorQueryParams, MajorFormData } from '@/types/major'

// 获取专业列表(分页)
export const getMajorList = (params: MajorQueryParams) => {
  return http.get<{
    records: Major[]
    total: number
    pages: number
  }>('/majors', { params })
}

// 获取所有启用的专业
export const getAllEnabledMajors = () => {
  return http.get<Major[]>('/majors/enabled')
}

// 根据组织单元ID获取专业列表
export const getMajorsByOrgUnit = (orgUnitId: number | string) => {
  return http.get<Major[]>(`/majors/org-unit/${orgUnitId}`)
}

// 获取专业详情
export const getMajorDetail = (id: number | string) => {
  return http.get<Major>(`/majors/${id}`)
}

// 新增专业
export const addMajor = (data: MajorFormData) => {
  return http.post('/majors', data)
}

// 更新专业
export const updateMajor = (id: number | string, data: MajorFormData) => {
  return http.put(`/majors/${id}`, data)
}

// 删除专业
export const deleteMajor = (id: number | string) => {
  return http.delete(`/majors/${id}`)
}

// 批量删除专业
export const batchDeleteMajors = (ids: (number | string)[]) => {
  return http.delete('/majors/batch', { data: { ids } })
}

// 导出专业列表
export const exportMajors = (params: MajorQueryParams) => {
  return http.get('/majors/export', {
    params,
    responseType: 'blob'
  })
}
