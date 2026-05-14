/**
 * 权限管理 API - DDD架构适配
 *
 * 注意: 响应拦截器已解包 ApiResponse，API 直接返回 data 内容
 */
import type { LongId } from '@/types/common'
import { http } from '@/utils/request'
import type {
  Permission,
  CreatePermissionRequest,
  UpdatePermissionRequest,
  CreateRoleRequest,
  UpdateRoleRequest,
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
 * @param includeDisabled 管理员视角: true 时返回所属插件被禁的权限 (pluginEnabled=false).
 */
export function getPermissions(params?: PermissionQueryParams & { includeDisabled?: boolean }): Promise<Permission[]> {
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
export function getPermission(id: LongId | string): Promise<Permission> {
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
export function updatePermission(id: LongId | string, data: UpdatePermissionRequest): Promise<Permission> {
  return http.put<Permission>(`${PERMISSION_URL}/${id}`, data)
}

/**
 * 删除权限
 */
export function deletePermission(id: LongId | string): Promise<void> {
  return http.delete(`${PERMISSION_URL}/${id}`)
}

/**
 * 启用权限
 */
export function enablePermission(id: LongId | string): Promise<void> {
  return http.post(`${PERMISSION_URL}/${id}/enable`)
}

/**
 * 禁用权限
 */
export function disablePermission(id: LongId | string): Promise<void> {
  return http.post(`${PERMISSION_URL}/${id}/disable`)
}

// ==================== 角色 API ====================

/**
 * 角色响应类型（后端返回格式）
 * 注意: id 为 string 类型，因为 Snowflake ID 超过 JavaScript 的 MAX_SAFE_INTEGER
 */
export interface RoleResponse {
  id: LongId  // 后端序列化为字符串，防止精度丢失
  roleCode: string
  roleName: string
  description?: string
  roleType?: string
  level?: number
  isSystem?: boolean
  isEnabled?: boolean
  /**
   * 插件级启用状态 (两状态模型):
   *   - isEnabled:     管理员手动开关
   *   - pluginEnabled: 所属插件级开关 (PluginLifecycleService 级联维护)
   * false = 所属插件被禁 > 前端应灰显 + "插件禁用"徽章 + tooltip
   */
  pluginEnabled?: boolean
  industry?: string
  pluginClass?: string
  origin?: string
  permissionIds?: (string | number)[]  // 权限ID也可能是字符串
  createdAt?: string
}

/**
 * 获取角色列表
 * 注意: V2 API 返回数组，需要前端分页
 * @param includeDisabled 管理员视角: true 时返回所属插件被禁的角色 (pluginEnabled=false).
 *                        权限计算链不应传 true.
 */
export function getRoles(params?: RoleQueryParams & { includeDisabled?: boolean }): Promise<RoleResponse[]> {
  return http.get<RoleResponse[]>(ROLE_URL, { params: {
    roleType: params?.roleType,
    enabled: params?.enabled,
    keyword: params?.keyword,
    includeDisabled: params?.includeDisabled
  }})
}

/**
 * 获取角色分页列表（前端分页）
 */
export function getRolesPage(params: RoleQueryParams & { pageNum: number; pageSize: number; includeDisabled?: boolean }) {
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
export function getRole(id: LongId): Promise<RoleResponse> {
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
export function updateRole(id: LongId, data: UpdateRoleRequest): Promise<RoleResponse> {
  return http.put<RoleResponse>(`${ROLE_URL}/${id}`, data)
}

/**
 * 删除角色
 */
export function deleteRole(id: LongId): Promise<void> {
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
export function enableRole(id: LongId): Promise<void> {
  return http.post(`${ROLE_URL}/${id}/enable`)
}

/**
 * 禁用角色
 */
export function disableRole(id: LongId): Promise<void> {
  return http.post(`${ROLE_URL}/${id}/disable`)
}

/**
 * 设置角色权限
 */
export function setRolePermissions(id: LongId, permissionIds: (string | number)[]): Promise<RoleResponse> {
  return http.put<RoleResponse>(`${ROLE_URL}/${id}/permissions`, { permissionIds })
}

/**
 * 获取角色权限ID列表
 */
export function getRolePermissionIds(id: LongId): Promise<(string | number)[]> {
  return getRole(id).then(role => role.permissionIds || [])
}

/**
 * 获取角色权限详情
 */
export function getRolePermissions(id: LongId): Promise<Permission[]> {
  return http.get<Permission[]>(`${ROLE_URL}/${id}/permissions`)
}

/**
 * 获取角色下的用户
 */
export function getRoleUsers(id: LongId, params?: PageParams): Promise<PageResponse<UserRole>> {
  return http.get<PageResponse<UserRole>>(`${ROLE_URL}/${id}/users`, { params })
}

// ==================== 用户角色 API ====================

/**
 * 获取用户的角色
 */
export function getUserRoles(userId: LongId | string): Promise<UserRole[]> {
  return http.get<UserRole[]>(`${USER_URL}/${userId}/roles`)
}

/**
 * 分配角色给用户（全局作用域）
 */
export function assignRoleToUser(userId: LongId | string, roleId: LongId | string): Promise<void> {
  return http.post(`${USER_URL}/${userId}/roles/${roleId}`)
}

/**
 * 分配角色给用户（带作用域）
 */
export function assignRoleToUserWithScope(userId: LongId | string, roleId: LongId | string, data: AssignRoleWithScopeRequest): Promise<void> {
  return http.post(`${USER_URL}/${userId}/roles/${roleId}/scoped`, data)
}

/**
 * 设置用户角色（批量，带作用域）
 */
export function setUserRoles(userId: LongId | string, data: SetUserRolesRequest): Promise<void> {
  return http.put(`${USER_URL}/${userId}/roles`, data)
}

/**
 * 移除用户角色（所有作用域）
 */
export function removeUserRole(userId: LongId | string, roleId: LongId | string): Promise<void> {
  return http.delete(`${USER_URL}/${userId}/roles/${roleId}`)
}

/**
 * 移除用户角色（指定作用域）
 */
export function removeUserRoleWithScope(userId: LongId | string, roleId: LongId | string, scopeType: string, scopeId: LongId | string): Promise<void> {
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

// ==================== 权限同步检查 API ====================

/**
 * 检查代码注解中的权限与数据库权限的同步状态
 */
export function checkPermissionSync(): Promise<{
  codeAnnotationCount: number
  dbPermissionCount: number
  missingInDb: string[]
  missingInDbCount: number
  potentiallyObsoleteInDb: string[]
  potentiallyObsoleteCount: number
  codeResourceActions: Record<string, string[]>
}> {
  return http.get('/system/permission-sync/check')
}

// ==================== 数据权限 API（动态化） ====================

import type {
  DataScopeOption,
  RolePermissionConfig,
  DataModuleDTO
} from '@/types/access'

const DATA_MODULE_URL = '/data-modules'

/**
 * 获取角色数据权限配置
 */
export function getRoleDataPermissions(roleId: LongId): Promise<RolePermissionConfig> {
  return http.get<RolePermissionConfig>(`${ROLE_URL}/${roleId}/data-permissions`)
}

/**
 * 保存角色数据权限配置
 */
export function saveRoleDataPermissions(roleId: LongId, config: RolePermissionConfig): Promise<void> {
  return http.put(`${ROLE_URL}/${roleId}/data-permissions`, config)
}

/**
 * 获取所有数据模块（动态，按领域分组）.
 * @param includeDisabled true=返回所有, 包括所属插件已禁用的模块 (前端可灰显).
 */
export function getDataModules(includeDisabled = false): Promise<DataModuleDTO[]> {
  return http.get<DataModuleDTO[]>(DATA_MODULE_URL, {
    params: { includeDisabled }
  })
}

/**
 * 按角色智能过滤的数据模块 (3 层规则: SUPER_ADMIN 豁免 + industry 对齐 + 权限反查)
 *
 * - SUPER_ADMIN / 无 roleId > relevant=全部, advanced=[], meta.filtered=false
 * - 其他角色 > relevant=本行业 + 权限匹配; advanced=跨行业或无对应权限 (折叠展示)
 */
export interface ForRoleModulesResponse {
  relevant: DataModuleDTO[]
  advanced: (DataModuleDTO & { reason?: string })[]
  meta: {
    filtered: boolean
    filterRule?: string
    totalRelevant?: number
    totalAdvanced?: number
    roleIndustry?: string
    roleType?: string
    rolePermModules?: string[]
  }
}

export function getDataModulesForRole(params: {
  roleId?: LongId
  includeDisabled?: boolean
}): Promise<ForRoleModulesResponse> {
  return http.get<ForRoleModulesResponse>(`${DATA_MODULE_URL}/for-role`, {
    params: {
      roleId: params.roleId != null ? String(params.roleId) : undefined,
      includeDisabled: params.includeDisabled ?? false,
    },
  })
}

/**
 * 获取所有数据范围选项
 */
export function getDataScopes(): Promise<DataScopeOption[]> {
  return http.get<DataScopeOption[]>(`${ROLE_URL}/data-permissions/scopes`)
}

/**
 * 数据权限 API 对象 (v3: 无 scope-item-types, CUSTOM 直接用 org_unit_id 列表)
 */
export const dataPermissionApi = {
  getConfig: getRoleDataPermissions,
  saveConfig: saveRoleDataPermissions,
  getModules: getDataModules,
  getModulesForRole: getDataModulesForRole,
  getScopes: getDataScopes
}

// ==================== 数据权限模拟预览 ====================

/**
 * 模拟单个模块在某用户上下文下的可访问数据快照
 */
export interface SimulateModulePermission {
  moduleCode: string
  scopeCode: string
  /** 兼容 List<Long> 或 List<{scopeId}> 两种格式 */
  scopeItems?: (number | string | { scopeId?: LongId | string; id?: LongId | string })[]
}

export interface SimulateRequest {
  userId: LongId | string
  modulePermissions: SimulateModulePermission[]
}

export interface SimulateSample {
  id: string
  name?: string
}

export interface SimulateResult {
  moduleCode: string
  scopeCode: string
  accessibleCount: number
  samples?: SimulateSample[]
  note?: string
}

export interface SimulateResponse {
  userId: string
  userOrgUnitId?: string | null
  results: SimulateResult[]
  error?: string
}

/**
 * 数据权限模拟预览 API
 *
 * 用于 /access/data-permissions 右栏 "模拟用户" 功能:
 * 管理员未保存的配置快照即可预览.
 */
export const dataPermissionSimulateApi = {
  simulate: (req: SimulateRequest): Promise<SimulateResponse> =>
    http.post<SimulateResponse>('/access/data-permissions/simulate', req)
}
