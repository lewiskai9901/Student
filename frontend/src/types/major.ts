// 专业相关类型定义

export interface Major {
  id: number
  majorName: string
  majorCode: string
  departmentId: number
  departmentName?: string
  description?: string
  status: number
  statusName?: string
  createdAt: string
  updatedAt?: string
}

export interface MajorQueryParams {
  majorName?: string
  majorCode?: string
  departmentId?: number | null
  status?: number | null
  pageNum?: number
  pageSize?: number
}

export interface MajorFormData {
  majorName: string
  majorCode: string
  departmentId: number | null
  description?: string
  status: number
}
