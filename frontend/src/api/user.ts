import { http } from '@/utils/request'

export interface User {
  id: number | string  // 后端Long类型会序列化为字符串,避免JS精度丢失
  username: string
  realName: string
  phone?: string
  email?: string
  avatar?: string
  employeeNo?: string
  gender?: number
  birthDate?: string
  identityCard?: string
  departmentId?: number | string
  departmentName?: string
  status: number
  lastLoginTime?: string
  lastLoginIp?: string
  createdAt?: string
  updatedAt?: string
  roleNames?: string[]
}

export interface UserQueryParams {
  pageNum: number
  pageSize: number
  username?: string
  realName?: string
  phone?: string
  departmentId?: number
  status?: number
}

export interface UserFormData {
  username: string
  realName: string
  phone?: string
  email?: string
  avatar?: string
  employeeNo?: string
  gender?: number
  birthDate?: string
  identityCard?: string
  departmentId?: number
  status?: number
  password?: string
}

/**
 * 获取用户分页列表
 */
export function getUserPage(params: UserQueryParams) {
  return http.get<{
    records: User[]
    total: number
    current: number
    size: number
  }>('/users', { params })
}

/**
 * 获取用户详情
 */
export function getUserDetail(id: number) {
  return http.get<User>(`/users/${id}`)
}

/**
 * 创建用户
 */
export function createUser(data: UserFormData) {
  return http.post<User>('/users', data)
}

/**
 * 更新用户
 */
export function updateUser(id: number, data: UserFormData) {
  return http.put<User>(`/users/${id}`, data)
}

/**
 * 删除用户
 */
export function deleteUser(id: number) {
  return http.delete(`/users/${id}`)
}

/**
 * 批量删除用户
 */
export function batchDeleteUsers(ids: number[]) {
  return http.delete('/users/batch', { data: ids })
}

/**
 * 重置用户密码
 */
export function resetUserPassword(id: number) {
  return http.post(`/users/${id}/reset-password`)
}

/**
 * 更新用户状态
 */
export function updateUserStatus(id: number, status: number) {
  return http.post(`/users/${id}/status`, null, { params: { status } })
}

/**
 * 检查用户名是否存在
 */
export function checkUsernameExists(username: string, excludeId?: number) {
  return http.get<boolean>('/users/exists', {
    params: { username, excludeId }
  })
}

/**
 * 获取用户的角色ID列表
 */
export function getUserRoles(id: number) {
  return http.get<number[]>(`/users/${id}/roles`)
}

/**
 * 为用户分配角色
 */
export function assignUserRoles(id: number, roleIds: number[]) {
  return http.post(`/users/${id}/roles`, roleIds)
}

/**
 * 简单用户信息（用于选择器）
 */
export interface SimpleUser {
  id: number
  username: string
  realName: string
  departmentName?: string
}

/**
 * 获取简单用户列表（用于选择器，可按关键词搜索）
 */
export function getSimpleUserList(keyword?: string) {
  return http.get<SimpleUser[]>('/users/simple', {
    params: { keyword }
  })
}

/**
 * 按部门获取用户列表（用于任务分配选择器）
 */
export function getUsersByDepartment(
  departmentId: number | string,
  includeChildren = false,
  keyword?: string
) {
  return http.get<User[]>(`/users/by-department/${departmentId}`, {
    params: { includeChildren, keyword }
  })
}

/**
 * 获取带部门信息的用户列表
 */
export function getUsersWithDepartments(keyword?: string) {
  return http.get<User[]>('/users/with-departments', {
    params: { keyword }
  })
}
