/**
 * 类型定义索引
 */

export * from './organization'
export * from './position'
export * from './access'
export * from './student'
export * from './dormitory'
export * from './task'
export * from './user'
export * from './semester'
export * from './myClass'
export * from './schedule'

// 通用响应类型
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

// 分页响应
export interface PageResponse<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

// 分页请求参数
export interface PageParams {
  pageNum?: number
  pageSize?: number
}

// 通用ID响应
export interface IdResponse {
  id: number | string
}

// 批量操作响应
export interface BatchResponse {
  successCount: number
  failedCount: number
  failedIds?: (number | string)[]
  message?: string
}
