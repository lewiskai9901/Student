/**
 * 通用类型定义
 *
 * 注意: ApiResponse 和 PageResult 在 @/types 中有对应版本
 * - PageResult -> PageResponse (字段一致)
 * - PageRequest -> PageParams (字段一致)
 *
 * MenuItem 和 RouteMeta 为前端通用类型，无需迁移
 */

// 通用类型定义

/**
 * 后端 Long 字段的前端类型.
 *
 * 后端 `JacksonConfig.java:37-38` 把所有 Long 序列化为 JSON string,
 * 防止 JS 53-bit 大数精度丢失 (雪花 ID 超 Number.MAX_SAFE_INTEGER).
 * 所有 `id / xxxId` 字段实际收到的是 string, 不是 number.
 *
 * 用法:
 *   import type { LongId } from '@/types/common'
 *   interface Foo { id: LongId; projectId: LongId; count: number }
 *
 * 真 number 字段不要用 LongId: count / score / weight / total / pageNum / pageSize / expiresIn / status code.
 *
 * 比较运算 (===, !==) 字符串两端等价工作, 不影响业务逻辑.
 * 算术运算 (id + 1) 会变字符串拼接, 这种地方往往是真 bug, 应改为业务相关字段或 parseInt 后操作.
 */
export type LongId = string

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
  group?: string
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