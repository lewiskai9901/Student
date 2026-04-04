/**
 * 用户管理 API
 *
 * 统一使用 /users 端点（UserController + UserRoleController）
 */
import { http } from '@/utils/request'
import type {
  User,
  CreateUserRequest,
  UpdateUserRequest,
  UserQueryParams,
  SimpleUser,
  UserFormData,
  UserListItem
} from '@/types/user'
import type { PageResponse } from '@/types'

// 后端API路径 - 统一使用 /users
const USER_URL = '/users'

// ==================== 分页查询 API ====================

/**
 * 获取用户分页列表
 */
export function getUserPage(params: UserQueryParams): Promise<PageResponse<UserListItem>> {
  return http.get<PageResponse<UserListItem>>(`/domain/users/page`, { params })
}

/**
 * 获取所有用户（不分页）
 */
export function getAllUsers(): Promise<User[]> {
  return http.get<User[]>(USER_URL)
}

/**
 * 获取简单用户列表（用于选择器）
 */
export function getSimpleUserList(keyword?: string): Promise<SimpleUser[]> {
  return http.get<SimpleUser[]>(`${USER_URL}/simple`, { params: { keyword } })
}

/**
 * 获取带部门信息的用户列表
 */
export function getUsersWithDepartments(keyword?: string): Promise<UserListItem[]> {
  return http.get<UserListItem[]>(`${USER_URL}/with-departments`, { params: { keyword } })
}

// ==================== 用户 CRUD API ====================

/**
 * 创建用户
 */
export function createUser(data: CreateUserRequest): Promise<User> {
  return http.post<User>(USER_URL, data)
}

/**
 * 更新用户
 */
export function updateUser(id: number | string, data: UpdateUserRequest): Promise<User> {
  return http.put<User>(`${USER_URL}/${id}`, data)
}

/**
 * 删除用户
 */
export function deleteUser(id: number | string): Promise<void> {
  return http.delete(`${USER_URL}/${id}`)
}

/**
 * 批量删除用户
 */
export function batchDeleteUsers(ids: (number | string)[]): Promise<void> {
  return http.delete(`${USER_URL}/batch`, { data: ids })
}

/**
 * 获取用户详情
 */
export function getUser(id: number | string): Promise<User> {
  return http.get<User>(`${USER_URL}/${id}`)
}

/**
 * 根据用户名获取用户
 */
export function getUserByUsername(username: string): Promise<User> {
  return http.get<User>(`${USER_URL}/by-username/${username}`)
}

/**
 * 根据组织单元获取用户列表
 */
export function getUsersByOrgUnit(orgUnitId: number | string): Promise<User[]> {
  return http.get<User[]>(`${USER_URL}/by-org-unit/${orgUnitId}`)
}

/**
 * 检查用户名是否存在
 */
export function checkUsernameExists(username: string, excludeId?: number | string): Promise<boolean> {
  return http.get<boolean>(`${USER_URL}/exists`, {
    params: { username, excludeId }
  })
}

// ==================== 状态操作 API ====================

/**
 * 启用用户
 */
export function enableUser(id: number | string): Promise<User> {
  return http.post<User>(`${USER_URL}/${id}/enable`)
}

/**
 * 禁用用户
 */
export function disableUser(id: number | string): Promise<User> {
  return http.post<User>(`${USER_URL}/${id}/disable`)
}

/**
 * 更新用户状态（兼容旧接口）
 * @param status 1=启用, 2=禁用
 */
export async function updateUserStatus(id: number | string, status: number): Promise<User> {
  if (status === 1) {
    return enableUser(id)
  } else {
    return disableUser(id)
  }
}

// ==================== 密码操作 API ====================

/**
 * 重置用户密码
 */
export function resetPassword(id: number | string): Promise<string> {
  return http.post<string>(`${USER_URL}/${id}/reset-password`)
}

// ==================== 角色操作 API ====================
// 角色 scope API 由 UserRoleController 提供，路径同为 /users
const ROLE_ASSIGN_URL = '/users'

/**
 * 获取用户角色ID列表
 */
export function getUserRoleIds(id: number | string): Promise<(number | string)[]> {
  return http.get<any[]>(`${ROLE_ASSIGN_URL}/${id}/roles`).then(roles => {
    if (!Array.isArray(roles)) return []
    return [...new Set(roles.map((r: any) => r.roleId || r.id))]
  })
}

/**
 * 获取用户角色分配（含作用域信息）
 */
export function getUserRoleAssignments(id: number | string): Promise<any[]> {
  return http.get<any[]>(`${ROLE_ASSIGN_URL}/${id}/roles`)
}

/**
 * 为用户分配角色（批量，带作用域、过期时间、原因）
 */
export function assignRoles(
  id: number | string,
  assignments: { roleId: number | string; scopeType?: string; scopeId?: number | string; expiresAt?: string; reason?: string }[]
): Promise<void> {
  const normalized = assignments.map(a => ({
    roleId: a.roleId,
    scopeType: a.scopeType || 'ALL',
    scopeId: a.scopeId || 0,
    expiresAt: a.expiresAt || undefined,
    reason: a.reason || undefined
  }))
  return http.put(`${ROLE_ASSIGN_URL}/${id}/roles`, { assignments: normalized })
}

// ==================== 微信绑定 API ====================

/**
 * 绑定微信
 */
export function bindWechat(id: number | string, openid: string): Promise<void> {
  return http.post(`${USER_URL}/${id}/bind-wechat`, null, { params: { openid } })
}

/**
 * 解绑微信
 */
export function unbindWechat(id: number | string): Promise<void> {
  return http.post(`${USER_URL}/${id}/unbind-wechat`)
}

// ==================== API 对象封装（供 Store 使用） ====================

/**
 * 用户 API 对象
 */
export const userApi = {
  // 查询
  getPage: getUserPage,
  getAll: getAllUsers,
  getSimpleList: getSimpleUserList,
  getWithDepartments: getUsersWithDepartments,

  // CRUD
  create: createUser,
  update: updateUser,
  delete: deleteUser,
  batchDelete: batchDeleteUsers,
  getById: getUser,
  getByUsername: getUserByUsername,
  getByOrgUnit: getUsersByOrgUnit,
  checkUsernameExists,

  // 状态
  enable: enableUser,
  disable: disableUser,
  updateStatus: updateUserStatus,

  // 密码
  resetPassword,

  // 角色
  getRoleIds: getUserRoleIds,
  getRoleAssignments: getUserRoleAssignments,
  assignRoles,

  // 微信
  bindWechat,
  unbindWechat
}

export default userApi

// ==================== 数据范围管理 API ====================

/**
 * 数据范围类型
 */
export interface ScopeType {
  code: string
  name: string
  level: number
}

/**
 * 用户数据范围DTO
 */
export interface UserDataScopeDTO {
  id: number
  userId: number
  username?: string
  realName?: string
  scopeType: string
  scopeTypeName?: string
  scopeId: number
  scopeName?: string
  includeChildren: number
  createdAt?: string
  createdBy?: number
  createdByName?: string
}

/**
 * 添加数据范围请求
 */
export interface AddScopeRequest {
  userId: number
  scopeType: string
  scopeId: number
  includeChildren?: number
}

/**
 * 批量添加数据范围请求
 */
export interface BatchAddScopeRequest {
  userId: number
  scopes: Array<{
    scopeType: string
    scopeId: number
    includeChildren?: number
  }>
}

const DATA_SCOPE_URL = '/user-data-scopes'

/**
 * 获取用户的数据范围列表
 */
export function getUserDataScopes(userId: number | string): Promise<UserDataScopeDTO[]> {
  return http.get<UserDataScopeDTO[]>(`${DATA_SCOPE_URL}/user/${userId}`)
}

/**
 * 添加用户数据范围
 */
export function addUserDataScope(data: AddScopeRequest): Promise<UserDataScopeDTO> {
  return http.post<UserDataScopeDTO>(DATA_SCOPE_URL, data)
}

/**
 * 批量添加用户数据范围
 */
export function batchAddUserDataScopes(data: BatchAddScopeRequest): Promise<UserDataScopeDTO[]> {
  return http.post<UserDataScopeDTO[]>(`${DATA_SCOPE_URL}/batch`, data)
}

/**
 * 删除用户数据范围
 */
export function deleteUserDataScope(id: number | string): Promise<void> {
  return http.delete(`${DATA_SCOPE_URL}/${id}`)
}

/**
 * 批量删除用户数据范围
 */
export function batchDeleteUserDataScopes(ids: (number | string)[]): Promise<void> {
  return http.delete(`${DATA_SCOPE_URL}/batch`, { data: { ids } })
}

/**
 * 删除用户所有数据范围
 */
export function deleteAllUserDataScopes(userId: number | string): Promise<void> {
  return http.delete(`${DATA_SCOPE_URL}/user/${userId}`)
}

/**
 * 获取拥有指定范围的用户列表
 */
export function getUsersByScopeTypeAndId(scopeType: string, scopeId: number | string): Promise<UserDataScopeDTO[]> {
  return http.get<UserDataScopeDTO[]>(`${DATA_SCOPE_URL}/scope`, {
    params: { scopeType, scopeId }
  })
}

/**
 * 检查用户是否有指定范围的权限
 */
export function checkUserScope(userId: number | string, scopeType: string, scopeId: number | string): Promise<boolean> {
  return http.get<boolean>(`${DATA_SCOPE_URL}/check`, {
    params: { userId, scopeType, scopeId }
  })
}

/**
 * 获取用户可访问的班级ID列表
 */
export function getAccessibleClassIds(userId: number | string): Promise<(number | string)[]> {
  return http.get<number[]>(`${DATA_SCOPE_URL}/accessible/classes/${userId}`)
}

/**
 * 获取所有范围类型
 */
export function getScopeTypes(): Promise<ScopeType[]> {
  return http.get<ScopeType[]>(`${DATA_SCOPE_URL}/scope-types`)
}

/**
 * 数据范围 API 对象
 */
export const userDataScopeApi = {
  getUserScopes: getUserDataScopes,
  addScope: addUserDataScope,
  batchAddScopes: batchAddUserDataScopes,
  deleteScope: deleteUserDataScope,
  batchDeleteScopes: batchDeleteUserDataScopes,
  deleteAllScopes: deleteAllUserDataScopes,
  getUsersByScope: getUsersByScopeTypeAndId,
  checkScope: checkUserScope,
  getAccessibleClasses: getAccessibleClassIds,
  getScopeTypes
}

// ==================== V1 兼容别名 ====================

export const getUserDetail = getUser
export const resetUserPassword = resetPassword
export const getUserRoles = getUserRoleIds
export const assignUserRoles = assignRoles
