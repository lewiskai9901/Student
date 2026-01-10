/**
 * 共享类型定义
 */

// 分页响应
export interface PageResponse<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages?: number
}

// 分页请求
export interface PageRequest {
  pageNum?: number
  pageSize?: number
}

// API 响应
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

// 选项类型
export interface SelectOption {
  label: string
  value: string | number
  disabled?: boolean
  children?: SelectOption[]
}

// 树节点类型
export interface TreeNode {
  id: number | string
  label: string
  children?: TreeNode[]
  disabled?: boolean
  isLeaf?: boolean
}
