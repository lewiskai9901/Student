/** Backend standard response wrapper */
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

/** Pagination request */
export interface PageQuery {
  page?: number
  size?: number
}

/** Pagination response */
export interface PageResult<T> {
  records: T[]
  total: number
  pages: number
  current: number
  size: number
}
