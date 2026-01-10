/**
 * 通用类型定义
 *
 * 注意: ApiResponse 和 PageResult 在 @/types/v2 中有对应版本
 * - PageResult -> PageResponse (字段一致)
 * - PageRequest -> PageParams (字段一致)
 *
 * MenuItem 和 RouteMeta 为前端通用类型，无需迁移
 */

// 通用类型定义

export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface PageRequest {
  pageNum: number
  pageSize: number
}

export interface Option {
  label: string
  value: any
  disabled?: boolean
  children?: Option[]
}

export interface MenuItem {
  id: string
  name: string
  path: string
  icon?: string
  title: string
  hidden?: boolean
  children?: MenuItem[]
  order?: number
  permission?: string
}

/**
 * 路由Meta扩展类型
 * 用于从路由自动生成菜单
 */
export interface RouteMeta {
  title?: string
  icon?: string
  requiresAuth?: boolean
  permission?: string
  hidden?: boolean
  order?: number
  group?: string
  breadcrumb?: boolean
  activeMenu?: string
  noCache?: boolean
  affix?: boolean
}