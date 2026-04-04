/**
 * 类型定义索引
 */

export * from './organization'
export * from './position'
export * from './access'
export * from './student'
export * from './dormitory'
export * from './user'
export * from './teaching'
export * from './myClass'


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

// Alias for backward compatibility
export type PageResult<T> = PageResponse<T>

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
