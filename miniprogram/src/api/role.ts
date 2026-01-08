/**
 * 角色管理 API
 */
import { get, post, put, del } from '@/utils/request'

// 角色信息
export interface Role {
  id: number | string
  roleCode: string
  roleName: string
  description?: string
  sort: number
  status: number
  statusText?: string
  userCount?: number
  permissions?: Permission[]
  permissionIds?: number[]
  createdAt?: string
  updatedAt?: string
}

// 权限信息
export interface Permission {
  id: number | string
  permissionCode: string
  permissionName: string
  parentId?: number
  parentName?: string
  type: number            // 1:菜单 2:按钮 3:API
  typeName?: string
  path?: string
  icon?: string
  sort: number
  status: number
  statusText?: string
  children?: Permission[]
}

// 查询参数
export interface RoleQueryParams {
  pageNum?: number
  pageSize?: number
  keyword?: string
  roleName?: string
  roleCode?: string
  status?: number
}

// 创建/更新角色请求
export interface RoleFormData {
  roleCode: string
  roleName: string
  description?: string
  sort?: number
  status?: number
  permissionIds?: number[]
}

// 分页结果
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/**
 * 获取角色列表
 */
export function getRoleList(params: RoleQueryParams = {}) {
  return get<PageResult<Role>>('/roles', {
    pageNum: params.pageNum || 1,
    pageSize: params.pageSize || 100,
    ...params
  })
}

/**
 * 获取角色详情
 */
export function getRoleDetail(id: number | string) {
  return get<Role>(`/roles/${id}`)
}

/**
 * 创建角色
 */
export function createRole(data: RoleFormData) {
  return post<Role>('/roles', data)
}

/**
 * 更新角色
 */
export function updateRole(id: number | string, data: RoleFormData) {
  return put<Role>(`/roles/${id}`, data)
}

/**
 * 删除角色
 */
export function deleteRole(id: number | string) {
  return del<void>(`/roles/${id}`)
}

/**
 * 获取所有角色(用于下拉选择)
 */
export function getAllRoles() {
  return get<Role[]>('/roles/all')
}

/**
 * 获取角色的权限ID列表
 */
export function getRolePermissions(id: number | string) {
  return get<number[]>(`/roles/${id}/permissions`)
}

/**
 * 为角色分配权限
 */
export function assignRolePermissions(id: number | string, permissionIds: number[]) {
  return post<void>(`/roles/${id}/permissions`, permissionIds)
}

/**
 * 获取权限树
 */
export function getPermissionTree() {
  return get<Permission[]>('/permissions/tree')
}

/**
 * 获取所有权限(扁平列表)
 */
export function getAllPermissions() {
  return get<Permission[]>('/permissions')
}
