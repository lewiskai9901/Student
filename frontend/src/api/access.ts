/**
 * 权限管理 API - DDD架构适配
 *
 * 注意: 响应拦截器已解包 ApiResponse，API 直接返回 data 内容
 */
import { http } from '@/utils/request'
import type {
  Permission,
  CreatePermissionRequest,
  UpdatePermissionRequest,
  Role,
  CreateRoleRequest,
  UpdateRoleRequest,
  SetPermissionsRequest,
  UserRole,
  AssignRoleWithScopeRequest,
  SetUserRolesRequest,
  RoleQueryParams,
  PermissionQueryParams
} from '@/types'
import type { PageResponse, PageParams } from '@/types'

// 后端API路径 - 对应后端Controller
const PERMISSION_URL = '/permissions'
const ROLE_URL = '/roles'
const USER_URL = '/users'

// ==================== 权限 API ====================

/**
 * 获取权限列表
 */
export function getPermissions(params?: PermissionQueryParams): Promise<Permission[]> {
  return http.get<Permission[]>(PERMISSION_URL, { params })
}

/**
 * 获取权限树
 */
export function getPermissionTree(): Promise<Permission[]> {
  return http.get<Permission[]>(`${PERMISSION_URL}/tree`)
}

/**
 * 获取权限详情
 */
export function getPermission(id: number | string): Promise<Permission> {
  return http.get<Permission>(`${PERMISSION_URL}/${id}`)
}

/**
 * 创建权限
 */
export function createPermission(data: CreatePermissionRequest): Promise<Permission> {
  return http.post<Permission>(PERMISSION_URL, data)
}

/**
 * 更新权限
 */
export function updatePermission(id: number | string, data: UpdatePermissionRequest): Promise<Permission> {
  return http.put<Permission>(`${PERMISSION_URL}/${id}`, data)
}

/**
 * 删除权限
 */
export function deletePermission(id: number | string): Promise<void> {
  return http.delete(`${PERMISSION_URL}/${id}`)
}

/**
 * 启用权限
 */
export function enablePermission(id: number | string): Promise<void> {
  return http.post(`${PERMISSION_URL}/${id}/enable`)
}

/**
 * 禁用权限
 */
export function disablePermission(id: number | string): Promise<void> {
  return http.post(`${PERMISSION_URL}/${id}/disable`)
}

// ==================== 角色 API ====================

/**
 * 角色响应类型（后端返回格式）
 * 注意: id 为 string 类型，因为 Snowflake ID 超过 JavaScript 的 MAX_SAFE_INTEGER
 */
export interface RoleResponse {
  id: string | number  // 后端序列化为字符串，防止精度丢失
  roleCode: string
  roleName: string
  description?: string
  roleType?: string
  level?: number
  isSystem?: boolean
  isEnabled?: boolean
  dataScope?: number
  permissionIds?: (string | number)[]  // 权限ID也可能是字符串
  createdAt?: string
}

/**
 * 获取角色列表
 * 注意: V2 API 返回数组，需要前端分页
 */
export function getRoles(params?: RoleQueryParams): Promise<RoleResponse[]> {
  return http.get<RoleResponse[]>(ROLE_URL, { params: { roleType: params?.roleType, enabled: params?.enabled, keyword: params?.keyword } })
}

/**
 * 获取角色分页列表（前端分页）
 */
export function getRolesPage(params: RoleQueryParams & { pageNum: number; pageSize: number }) {
  return getRoles(params).then(roles => {
    const start = (params.pageNum - 1) * params.pageSize
    const end = start + params.pageSize
    return {
      records: roles.slice(start, end),
      total: roles.length,
      current: params.pageNum,
      size: params.pageSize
    }
  })
}

/**
 * 获取所有角色（不分页）
 */
export function getAllRoles(): Promise<RoleResponse[]> {
  return http.get<RoleResponse[]>(ROLE_URL)
}

/**
 * 获取角色详情
 */
export function getRole(id: string | number): Promise<RoleResponse> {
  return http.get<RoleResponse>(`${ROLE_URL}/${id}`)
}

/**
 * 创建角色
 */
export function createRole(data: CreateRoleRequest): Promise<RoleResponse> {
  return http.post<RoleResponse>(ROLE_URL, data)
}

/**
 * 更新角色
 */
export function updateRole(id: string | number, data: UpdateRoleRequest): Promise<RoleResponse> {
  return http.put<RoleResponse>(`${ROLE_URL}/${id}`, data)
}

/**
 * 删除角色
 */
export function deleteRole(id: string | number): Promise<void> {
  return http.delete(`${ROLE_URL}/${id}`)
}

/**
 * 批量删除角色
 */
export function batchDeleteRoles(ids: (string | number)[]): Promise<void[]> {
  return Promise.all(ids.map(id => deleteRole(id)))
}

/**
 * 启用角色
 */
export function enableRole(id: string | number): Promise<void> {
  return http.post(`${ROLE_URL}/${id}/enable`)
}

/**
 * 禁用角色
 */
export function disableRole(id: string | number): Promise<void> {
  return http.post(`${ROLE_URL}/${id}/disable`)
}

/**
 * 设置角色权限
 */
export function setRolePermissions(id: string | number, permissionIds: (string | number)[]): Promise<RoleResponse> {
  return http.put<RoleResponse>(`${ROLE_URL}/${id}/permissions`, { permissionIds })
}

/**
 * 获取角色权限ID列表
 */
export function getRolePermissionIds(id: string | number): Promise<(string | number)[]> {
  return getRole(id).then(role => role.permissionIds || [])
}

/**
 * 获取角色权限详情
 */
export function getRolePermissions(id: string | number): Promise<Permission[]> {
  return http.get<Permission[]>(`${ROLE_URL}/${id}/permissions`)
}

/**
 * 获取角色下的用户
 */
export function getRoleUsers(id: string | number, params?: PageParams): Promise<PageResponse<UserRole>> {
  return http.get<PageResponse<UserRole>>(`${ROLE_URL}/${id}/users`, { params })
}

// ==================== 用户角色 API ====================

/**
 * 获取用户的角色
 */
export function getUserRoles(userId: number | string): Promise<UserRole[]> {
  return http.get<UserRole[]>(`${USER_URL}/${userId}/roles`)
}

/**
 * 分配角色给用户（全局作用域）
 */
export function assignRoleToUser(userId: number | string, roleId: number | string): Promise<void> {
  return http.post(`${USER_URL}/${userId}/roles/${roleId}`)
}

/**
 * 分配角色给用户（带作用域）
 */
export function assignRoleToUserWithScope(userId: number | string, roleId: number | string, data: AssignRoleWithScopeRequest): Promise<void> {
  return http.post(`${USER_URL}/${userId}/roles/${roleId}/scoped`, data)
}

/**
 * 设置用户角色（批量，带作用域）
 */
export function setUserRoles(userId: number | string, data: SetUserRolesRequest): Promise<void> {
  return http.put(`${USER_URL}/${userId}/roles`, data)
}

/**
 * 移除用户角色（所有作用域）
 */
export function removeUserRole(userId: number | string, roleId: number | string): Promise<void> {
  return http.delete(`${USER_URL}/${userId}/roles/${roleId}`)
}

/**
 * 移除用户角色（指定作用域）
 */
export function removeUserRoleWithScope(userId: number | string, roleId: number | string, scopeType: string, scopeId: number | string): Promise<void> {
  return http.delete(`${USER_URL}/${userId}/roles/${roleId}`, { params: { scopeType, scopeId } })
}

/**
 * 获取当前用户权限
 */
export function getCurrentUserPermissions(): Promise<Permission[]> {
  return http.get<Permission[]>(`${USER_URL}/current/permissions`)
}

/**
 * 获取当前用户角色
 */
export function getCurrentUserRoles(): Promise<UserRole[]> {
  return http.get<UserRole[]>(`${USER_URL}/current/roles`)
}

/**
 * 检查当前用户权限
 */
export function checkPermission(permissionCode: string): Promise<boolean> {
  return http.get<boolean>(`${USER_URL}/current/check`, { params: { permissionCode } })
}

/**
 * 批量检查权限
 */
export function checkPermissions(permissionCodes: string[]): Promise<Record<string, boolean>> {
  return http.post<Record<string, boolean>>(`${USER_URL}/current/check-batch`, { permissionCodes })
}

// ==================== API 对象封装（供 Store 使用） ====================

/**
 * 权限 API 对象
 */
export const permissionApi = {
  getList: getPermissions,
  getTree: getPermissionTree,
  getById: getPermission,
  create: createPermission,
  update: updatePermission,
  delete: deletePermission,
  enable: enablePermission,
  disable: disablePermission
}

/**
 * 角色 API 对象
 */
export const roleApi = {
  getList: getRoles,
  getPage: getRolesPage,
  getAll: getAllRoles,
  getById: getRole,
  create: createRole,
  update: updateRole,
  delete: deleteRole,
  batchDelete: batchDeleteRoles,
  enable: enableRole,
  disable: disableRole,
  setPermissions: setRolePermissions,
  getPermissionIds: getRolePermissionIds,
  getPermissions: getRolePermissions,
  getUsers: getRoleUsers
}

/**
 * 用户角色 API 对象
 */
export const userRoleApi = {
  getUserRoles,
  assignRole: assignRoleToUser,
  assignRoleWithScope: assignRoleToUserWithScope,
  setRoles: setUserRoles,
  removeRole: removeUserRole,
  removeRoleWithScope: removeUserRoleWithScope,
  getCurrentPermissions: getCurrentUserPermissions,
  getCurrentRoles: getCurrentUserRoles,
  checkPermission,
  checkPermissions
}

// ==================== 数据权限 API（动态化） ====================

import type {
  DataScopeOption,
  RolePermissionConfig,
  DataModuleDTO,
  ScopeItemTypeDTO,
  ModulePermission
} from '@/types/access'

const DATA_MODULE_URL = '/data-modules'

/**
 * 获取角色数据权限配置
 */
export function getRoleDataPermissions(roleId: string | number): Promise<RolePermissionConfig> {
  return http.get<RolePermissionConfig>(`${ROLE_URL}/${roleId}/data-permissions`)
}

/**
 * 保存角色数据权限配置
 */
export function saveRoleDataPermissions(roleId: string | number, config: RolePermissionConfig): Promise<void> {
  return http.put(`${ROLE_URL}/${roleId}/data-permissions`, config)
}

/**
 * 获取所有数据模块（动态，按领域分组）
 */
export function getDataModules(): Promise<DataModuleDTO[]> {
  return http.get<DataModuleDTO[]>(DATA_MODULE_URL)
}

/**
 * 获取所有数据范围选项
 */
export function getDataScopes(): Promise<DataScopeOption[]> {
  return http.get<DataScopeOption[]>(`${ROLE_URL}/data-permissions/scopes`)
}

/**
 * 获取所有范围项类型
 */
export function getScopeItemTypes(): Promise<ScopeItemTypeDTO[]> {
  return http.get<ScopeItemTypeDTO[]>(`${DATA_MODULE_URL}/scope-item-types`)
}

/**
 * 获取某模块可用的范围项类型
 */
export function getModuleScopeItemTypes(moduleCode: string): Promise<ScopeItemTypeDTO[]> {
  return http.get<ScopeItemTypeDTO[]>(`${DATA_MODULE_URL}/${moduleCode}/scope-item-types`)
}

/**
 * 搜索自定义范围可选项
 */
export function searchScopeItems(
  itemTypeCode: string,
  keyword: string = '',
  limit: number = 20
): Promise<{ id: number | string; name: string; parentId?: number | string }[]> {
  return http.get(`${DATA_MODULE_URL}/scope-items`, {
    params: { itemTypeCode, keyword, limit }
  })
}

/**
 * 数据权限 API 对象
 */
export const dataPermissionApi = {
  getConfig: getRoleDataPermissions,
  saveConfig: saveRoleDataPermissions,
  getModules: getDataModules,
  getScopes: getDataScopes,
  getScopeItemTypes,
  getModuleScopeItemTypes,
  searchScopeItems
}
