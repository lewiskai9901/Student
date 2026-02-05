/**
 * V6 用户类型 API
 */
import { http } from '@/utils/request'
import type { UserType, UserTypeTreeNode, CreateUserTypeRequest, UpdateUserTypeRequest } from '@/types/userType'

const BASE_URL = '/v6/user-types'

/**
 * 获取所有用户类型
 */
export function getAllUserTypes(): Promise<UserType[]> {
  return http.get<UserType[]>(BASE_URL)
}

/**
 * 获取所有启用的用户类型
 */
export function getEnabledUserTypes(): Promise<UserType[]> {
  return http.get<UserType[]>(`${BASE_URL}/enabled`)
}

/**
 * 获取用户类型树
 */
export function getUserTypeTree(): Promise<UserTypeTreeNode[]> {
  return http.get<UserTypeTreeNode[]>(`${BASE_URL}/tree`)
}

/**
 * 获取可登录的类型
 */
export function getLoginableUserTypes(): Promise<UserType[]> {
  return http.get<UserType[]>(`${BASE_URL}/loginable`)
}

/**
 * 获取可作为检查员的类型
 */
export function getInspectorUserTypes(): Promise<UserType[]> {
  return http.get<UserType[]>(`${BASE_URL}/inspector`)
}

/**
 * 获取可被检查的类型
 */
export function getInspectableUserTypes(): Promise<UserType[]> {
  return http.get<UserType[]>(`${BASE_URL}/inspectable`)
}

/**
 * 获取用户类型详情
 */
export function getUserTypeById(id: number): Promise<UserType> {
  return http.get<UserType>(`${BASE_URL}/${id}`)
}

/**
 * 根据编码获取用户类型
 */
export function getUserTypeByCode(typeCode: string): Promise<UserType> {
  return http.get<UserType>(`${BASE_URL}/code/${typeCode}`)
}

/**
 * 创建用户类型
 */
export function createUserType(data: CreateUserTypeRequest): Promise<UserType> {
  return http.post<UserType>(BASE_URL, data)
}

/**
 * 更新用户类型
 */
export function updateUserType(id: number, data: UpdateUserTypeRequest): Promise<UserType> {
  return http.put<UserType>(`${BASE_URL}/${id}`, data)
}

/**
 * 删除用户类型
 */
export function deleteUserType(id: number): Promise<void> {
  return http.delete(`${BASE_URL}/${id}`)
}

/**
 * 启用用户类型
 */
export function enableUserType(id: number): Promise<UserType> {
  return http.put<UserType>(`${BASE_URL}/${id}/enable`)
}

/**
 * 禁用用户类型
 */
export function disableUserType(id: number): Promise<UserType> {
  return http.put<UserType>(`${BASE_URL}/${id}/disable`)
}

export const userTypeApi = {
  getAll: getAllUserTypes,
  getEnabled: getEnabledUserTypes,
  getTree: getUserTypeTree,
  getLoginable: getLoginableUserTypes,
  getInspector: getInspectorUserTypes,
  getInspectable: getInspectableUserTypes,
  getById: getUserTypeById,
  getByCode: getUserTypeByCode,
  create: createUserType,
  update: updateUserType,
  delete: deleteUserType,
  enable: enableUserType,
  disable: disableUserType
}
