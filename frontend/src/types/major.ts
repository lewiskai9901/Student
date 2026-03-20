// 专业相关类型定义

export interface Major {
  id: number | string
  majorName: string
  majorCode: string
  orgUnitId: number | string
  orgUnitName?: string
  description?: string
  status: number
  statusName?: string
  createdAt: string
  updatedAt?: string
}

export interface MajorQueryParams {
  majorName?: string
  majorCode?: string
  orgUnitId?: number | string | null
  status?: number | null
  pageNum?: number
  pageSize?: number
}

export interface MajorFormData {
  majorName: string
  majorCode: string
  orgUnitId: number | string | null
  description?: string
  status: number
}
