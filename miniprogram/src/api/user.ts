/**
 * 用户管理 API
 */
import { get, post, put, del } from '@/utils/request'

// 用户信息
export interface User {
  id: number | string
  username: string
  realName: string
  phone?: string
  email?: string
  avatar?: string
  employeeNo?: string
  gender?: number
  genderText?: string
  birthDate?: string
  identityCard?: string
  departmentId?: number
  departmentName?: string
  status: number
  statusText?: string
  lastLoginTime?: string
  lastLoginIp?: string
  roleIds?: number[]
  roleNames?: string[]
  createdAt?: string
  updatedAt?: string
}

// 查询参数
export interface UserQueryParams {
  pageNum?: number
  pageSize?: number
  keyword?: string
  username?: string
  realName?: string
  phone?: string
  departmentId?: number
  status?: number
}

// 创建/更新用户请求
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

// 分页结果
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/**
 * 获取用户分页列表
 */
export function getUserPage(params: UserQueryParams = {}) {
  return get<PageResult<User>>('/users', {
    pageNum: params.pageNum || 1,
    pageSize: params.pageSize || 10,
    ...params
  })
}

// getUserList 是 getUserPage 的别名
export const getUserList = getUserPage

/**
 * 获取用户详情
 */
export function getUserDetail(id: number | string) {
  return get<User>(`/users/${id}`)
}

/**
 * 创建用户
 */
export function createUser(data: UserFormData) {
  return post<User>('/users', data)
}

/**
 * 更新用户
 */
export function updateUser(id: number | string, data: UserFormData) {
  return put<User>(`/users/${id}`, data)
}

/**
 * 删除用户
 */
export function deleteUser(id: number | string) {
  return del<void>(`/users/${id}`)
}

/**
 * 批量删除用户
 */
export function batchDeleteUsers(ids: (number | string)[]) {
  return del<void>('/users/batch', ids)
}

/**
 * 重置用户密码
 */
export function resetUserPassword(id: number | string) {
  return post<void>(`/users/${id}/reset-password`)
}

/**
 * 更新用户状态
 */
export function updateUserStatus(id: number | string, status: number) {
  return post<void>(`/users/${id}/status`, { status })
}

/**
 * 检查用户名是否存在
 */
export function checkUsernameExists(username: string, excludeId?: number | string) {
  return get<boolean>('/users/exists', { username, excludeId })
}

/**
 * 获取用户的角色ID列表
 */
export function getUserRoles(id: number | string) {
  return get<number[]>(`/users/${id}/roles`)
}

/**
 * 为用户分配角色
 */
export function assignUserRoles(id: number | string, roleIds: number[]) {
  return post<void>(`/users/${id}/roles`, roleIds)
}

/**
 * 获取教师列表(用于班主任选择)
 */
export function getTeacherList() {
  return get<User[]>('/users/teachers')
}
