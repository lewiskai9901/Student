/**
 * API 公共类型定义
 */

// 分页结果
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

// 分页查询参数
export interface PageParams {
  pageNum?: number
  pageSize?: number
}
