import { http } from '@/utils/request'

// V2 API 基础路径
const BASE_URL = '/v2/permissions'

export interface Permission {
  id: number
  permissionCode: string
  permissionName: string
  description?: string
  resource?: string
  action?: string
  type?: string
  parentId?: number
  sortOrder?: number
  isEnabled: boolean
  createdAt?: string
  children?: Permission[]
}

// 兼容V1字段
// V1: status -> V2: isEnabled
// V1: permissionType -> V2: type

/**
 * 获取权限树
 */
export function getPermissionTree() {
  return http.get<Permission[]>(`${BASE_URL}/tree`)
}

/**
 * 获取所有权限列表
 */
export function getAllPermissions() {
  return http.get<Permission[]>(BASE_URL)
}

/**
 * 获取权限详情
 */
export function getPermissionDetail(id: number) {
  return http.get<Permission>(`${BASE_URL}/${id}`)
}

/**
 * 创建权限
 */
export function createPermission(data: Partial<Permission>) {
  return http.post<number>(BASE_URL, data)
}

/**
 * 更新权限
 */
export function updatePermission(id: number, data: Partial<Permission>) {
  return http.put(`${BASE_URL}/${id}`, data)
}

/**
 * 删除权限
 */
export function deletePermission(id: number) {
  return http.delete(`${BASE_URL}/${id}`)
}

/**
 * 根据类型获取权限列表
 */
export function getPermissionsByType(type: string) {
  return http.get<Permission[]>(BASE_URL, {
    params: { type }
  })
}
