/**
 * V2 类型定义索引 - DDD架构适配
 */

export * from './organization'
export * from './inspection'
export * from './access'
export * from './student'
export * from './dormitory'
export * from './task'
export * from './user'
export * from './semester'
export * from './myClass'
export * from './inspectionSession'
export * from './corrective'
export * from './behavior'
export * from './schedule'
export * from './analytics'

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
  id: number
}

// 批量操作响应
export interface BatchResponse {
  successCount: number
  failedCount: number
  failedIds?: number[]
  message?: string
}
