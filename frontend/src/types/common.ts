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