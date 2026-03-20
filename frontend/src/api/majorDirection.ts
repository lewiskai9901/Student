import { http } from '@/utils/request'

export interface MajorDirection {
  id?: number
  majorId: number
  directionName: string
  directionCode: string
  level: string           // 层次：中级工/高级工/预备技师/技师
  years: number           // 学制（年）
  isSegmented?: number    // 是否分段注册：0-否，1-是
  phase1Level?: string    // 第一阶段层次
  phase1Years?: number    // 第一阶段年数
  phase2Level?: string    // 第二阶段层次
  phase2Years?: number    // 第二阶段年数
  remarks?: string
  createdAt?: string
  updatedAt?: string
  createdBy?: number
  updatedBy?: number
  majorName?: string      // 关联显示
  // 显示用辅助字段
  levelDisplay?: string   // 层次显示（如：中级工 → 高级工）
  yearsDisplay?: string   // 学制显示（如：3+2年）
}

export interface MajorDirectionQueryParams {
  pageNum?: number
  pageSize?: number
  majorId?: number
  directionName?: string
  level?: string
}

// 获取专业方向列表(分页)
export const getMajorDirectionList = (params: MajorDirectionQueryParams) => {
  return http.get<{
    records: MajorDirection[]
    total: number
    pages: number
  }>('/major-directions', { params })
}

// 获取所有专业方向
export const getAllDirections = () => {
  return http.get<MajorDirection[]>('/major-directions/all')
}

// 根据专业ID获取专业方向列表
export const getDirectionsByMajor = (majorId: number | string) => {
  return http.get<MajorDirection[]>(`/major-directions/major/${majorId}`)
}

// 获取专业方向详情
export const getMajorDirectionDetail = (id: number | string) => {
  return http.get<MajorDirection>(`/major-directions/${id}`)
}

// 新增专业方向
export const addMajorDirection = (data: MajorDirection) => {
  return http.post<MajorDirection>('/major-directions', data)
}

// 更新专业方向
export const updateMajorDirection = (id: number | string, data: MajorDirection) => {
  return http.put(`/major-directions/${id}`, data)
}

// 删除专业方向
export const deleteMajorDirection = (id: number | string) => {
  return http.delete(`/major-directions/${id}`)
}

// 批量删除专业方向
export const batchDeleteDirections = (ids: (number | string)[]) => {
  return http.delete('/major-directions/batch', { data: ids })
}
