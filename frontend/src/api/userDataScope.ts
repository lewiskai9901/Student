import { http } from '@/utils/request'

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
export interface BatchAddRequest {
  userId: number
  scopes: Array<{
    scopeType: string
    scopeId: number
    includeChildren?: number
  }>
}

/**
 * 获取用户的数据范围列表
 */
export function getUserDataScopes(userId: number) {
  return http.get<UserDataScopeDTO[]>(`/user-data-scopes/user/${userId}`)
}

/**
 * 添加用户数据范围
 */
export function addUserDataScope(data: AddScopeRequest) {
  return http.post<UserDataScopeDTO>('/user-data-scopes', data)
}

/**
 * 批量添加用户数据范围
 */
export function batchAddUserDataScopes(data: BatchAddRequest) {
  return http.post<UserDataScopeDTO[]>('/user-data-scopes/batch', data)
}

/**
 * 删除用户数据范围
 */
export function deleteUserDataScope(id: number) {
  return http.delete(`/user-data-scopes/${id}`)
}

/**
 * 批量删除用户数据范围
 */
export function batchDeleteUserDataScopes(ids: number[]) {
  return http.delete('/user-data-scopes/batch', { data: { ids } })
}

/**
 * 删除用户所有数据范围
 */
export function deleteAllUserDataScopes(userId: number) {
  return http.delete(`/user-data-scopes/user/${userId}`)
}

/**
 * 获取拥有指定范围的用户列表
 */
export function getUsersByScopeTypeAndId(scopeType: string, scopeId: number) {
  return http.get<UserDataScopeDTO[]>('/user-data-scopes/scope', {
    params: { scopeType, scopeId }
  })
}

/**
 * 检查用户是否有指定范围的权限
 */
export function checkUserScope(userId: number, scopeType: string, scopeId: number) {
  return http.get<boolean>('/user-data-scopes/check', {
    params: { userId, scopeType, scopeId }
  })
}

/**
 * 获取用户可访问的班级ID列表
 */
export function getAccessibleClassIds(userId: number) {
  return http.get<number[]>(`/user-data-scopes/accessible/classes/${userId}`)
}

/**
 * 获取所有范围类型
 */
export function getScopeTypes() {
  return http.get<ScopeType[]>('/user-data-scopes/scope-types')
}
