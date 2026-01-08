/**
 * 用户管理 API - DDD架构适配
 *
 * 使用纯 DDD 端点: /v2/domain/users
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
} from '@/types/v2/user'
import type { PageResponse } from '@/types/v2'

// 后端API路径 - 纯DDD端点
const USER_URL = '/v2/domain/users'

// ==================== 分页查询 API ====================

/**
 * 获取用户分页列表
 */
export function getUserPage(params: UserQueryParams): Promise<PageResponse<UserListItem>> {
  return http.get<PageResponse<UserListItem>>(`${USER_URL}/page`, { params })
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
 * 根据部门获取用户列表
 */
export function getUsersByDepartment(departmentId: number | string): Promise<User[]> {
  return http.get<User[]>(`${USER_URL}/by-department/${departmentId}`)
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

/**
 * 获取用户角色ID列表
 */
export function getUserRoleIds(id: number | string): Promise<number[]> {
  return http.get<number[]>(`${USER_URL}/${id}/roles`)
}

/**
 * 为用户分配角色
 */
export function assignRoles(id: number | string, roleIds: number[]): Promise<void> {
  return http.post(`${USER_URL}/${id}/roles`, roleIds)
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
  getByDepartment: getUsersByDepartment,
  checkUsernameExists,

  // 状态
  enable: enableUser,
  disable: disableUser,
  updateStatus: updateUserStatus,

  // 密码
  resetPassword,

  // 角色
  getRoleIds: getUserRoleIds,
  assignRoles,

  // 微信
  bindWechat,
  unbindWechat
}

export default userApi
