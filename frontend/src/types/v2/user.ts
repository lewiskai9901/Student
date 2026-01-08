/**
 * 用户管理类型定义 - DDD架构适配
 */

/**
 * 用户类型枚举
 */
export type UserType = '管理员' | '教师' | '学生'

/**
 * 用户状态枚举
 * 支持字符串和数字格式以兼容 V1/V2 API
 */
export type UserStatus = '启用' | '禁用' | 1 | 2

/**
 * 用户领域实体
 */
export interface User {
  id: number | string
  username: string
  realName: string
  phone?: string
  email?: string
  avatar?: string
  employeeNo?: string
  gender?: number
  birthDate?: string
  departmentId?: number | string
  classId?: number | string
  managedClassId?: number | string
  userType?: UserType
  status?: UserStatus
  lastLoginTime?: string
  lastLoginIp?: string
  passwordChangedAt?: string
  wechatBound?: boolean
  allowMultipleDevices?: boolean
  roleIds?: number[]
  createdAt?: string
  updatedAt?: string
}

/**
 * 创建用户请求
 */
export interface CreateUserRequest {
  username: string
  password?: string
  realName: string
  phone?: string
  email?: string
  employeeNo?: string
  gender?: number
  birthDate?: string
  idCard?: string
  departmentId?: number
  userType?: number
  roleIds?: number[]
}

/**
 * 更新用户请求
 */
export interface UpdateUserRequest {
  realName?: string
  phone?: string
  email?: string
  employeeNo?: string
  gender?: number
  birthDate?: string
  idCard?: string
  departmentId?: number
  roleIds?: number[]
}

/**
 * 用户查询参数
 */
export interface UserQueryParams {
  pageNum?: number
  pageSize?: number
  username?: string
  realName?: string
  phone?: string
  departmentId?: number
  status?: number
}

/**
 * 简单用户信息（用于选择器）
 */
export interface SimpleUser {
  id: number | string
  username: string
  realName: string
  departmentName?: string
}

/**
 * 用户表单数据（用于新增/编辑）
 */
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
 * 用户列表响应（带额外字段）
 */
export interface UserListItem extends User {
  departmentName?: string
  roleNames?: string[]
}
