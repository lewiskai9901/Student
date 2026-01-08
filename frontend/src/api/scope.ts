import { http } from '@/utils/request'

/**
 * Casbin 数据范围 API
 * 对接后端 /api/v2/scopes/* 接口
 */

// ==================== 类型定义 ====================

/**
 * 范围类型枚举
 */
export type ScopeTypeCode = 'ALL' | 'DEPT' | 'GRADE' | 'DEPT_GRADE' | 'CLASS' | 'SELF'

/**
 * 范围分配DTO
 */
export interface ScopeAssignmentDTO {
  id?: number
  userId: number
  username?: string
  realName?: string
  scopeType: ScopeTypeCode
  scopeExpression: string
  displayName?: string
  remark?: string
  expiresAt?: string
  assignedBy?: number
  assignedByName?: string
  assignedAt?: string
}

/**
 * 范围元数据DTO
 */
export interface ScopeMetadataDTO {
  id: number
  scopeExpression: string
  displayName: string
  scopeType: ScopeTypeCode
  scopeTypeName?: string
  refId?: number
  refType?: string
  parentScope?: string
  sortOrder?: number
}

/**
 * 权限检查DTO
 */
export interface PermissionCheckDTO {
  userId: number
  scopeExpression: string
  resource: string
  action: string
  allowed?: boolean
}

/**
 * 分配范围请求
 */
export interface AssignScopeRequest {
  userId: number
  scopeType: ScopeTypeCode
  scopeExpression: string
  displayName?: string
  remark?: string
}

/**
 * 批量分配请求
 */
export interface BatchAssignRequest {
  userId: number
  scopes: string[]
}

/**
 * 撤销范围请求
 */
export interface RevokeScopeRequest {
  userId: number
  scopeExpression: string
}

// ==================== 范围类型配置 ====================

/**
 * 范围类型配置
 */
export const SCOPE_TYPES: Record<ScopeTypeCode, { name: string; prefix: string; level: number }> = {
  ALL: { name: '全部数据', prefix: 'scope:*', level: 0 },
  DEPT: { name: '部门', prefix: 'scope:dept:', level: 1 },
  GRADE: { name: '年级', prefix: 'scope:grade:', level: 1 },
  DEPT_GRADE: { name: '部门+年级', prefix: 'scope:dept_grade:', level: 2 },
  CLASS: { name: '班级', prefix: 'scope:class:', level: 3 },
  SELF: { name: '仅本人', prefix: 'scope:self', level: 4 }
}

/**
 * 构建范围表达式
 */
export function buildScopeExpression(type: ScopeTypeCode, ...ids: (number | string)[]): string {
  switch (type) {
    case 'ALL':
      return 'scope:*'
    case 'SELF':
      return 'scope:self'
    case 'DEPT':
      return `scope:dept:${ids[0]}`
    case 'GRADE':
      return `scope:grade:${ids[0]}`
    case 'DEPT_GRADE':
      return `scope:dept_grade:${ids[0]}:${ids[1]}`
    case 'CLASS':
      return `scope:class:${ids[0]}`
    default:
      return ''
  }
}

/**
 * 解析范围表达式
 */
export function parseScopeExpression(expression: string): { type: ScopeTypeCode; ids: number[] } | null {
  if (expression === 'scope:*') {
    return { type: 'ALL', ids: [] }
  }
  if (expression === 'scope:self') {
    return { type: 'SELF', ids: [] }
  }
  if (expression.startsWith('scope:dept_grade:')) {
    const parts = expression.replace('scope:dept_grade:', '').split(':')
    return { type: 'DEPT_GRADE', ids: parts.map(Number) }
  }
  if (expression.startsWith('scope:dept:')) {
    return { type: 'DEPT', ids: [Number(expression.replace('scope:dept:', ''))] }
  }
  if (expression.startsWith('scope:grade:')) {
    return { type: 'GRADE', ids: [Number(expression.replace('scope:grade:', ''))] }
  }
  if (expression.startsWith('scope:class:')) {
    return { type: 'CLASS', ids: [Number(expression.replace('scope:class:', ''))] }
  }
  return null
}

/**
 * 获取范围类型显示名称
 */
export function getScopeTypeName(type: ScopeTypeCode): string {
  return SCOPE_TYPES[type]?.name || type
}

// ==================== API 方法 ====================

/**
 * 获取用户的数据范围列表
 */
export function getUserScopes(userId: number) {
  return http.get<ScopeAssignmentDTO[]>(`/v2/scopes/user/${userId}`)
}

/**
 * 分配用户数据范围
 */
export function assignScope(data: AssignScopeRequest) {
  return http.post<ScopeAssignmentDTO>('/v2/scopes/assign', data)
}

/**
 * 批量分配用户数据范围
 */
export function batchAssignScopes(data: BatchAssignRequest) {
  return http.post<ScopeAssignmentDTO[]>('/v2/scopes/batch-assign', data)
}

/**
 * 撤销用户数据范围
 */
export function revokeScope(data: RevokeScopeRequest) {
  return http.delete('/v2/scopes/revoke', { data })
}

/**
 * 撤销用户所有数据范围
 */
export function revokeAllScopes(userId: number) {
  return http.delete(`/v2/scopes/user/${userId}/all`)
}

/**
 * 获取所有范围元数据
 */
export function getScopeMetadata() {
  return http.get<ScopeMetadataDTO[]>('/v2/scopes/metadata')
}

/**
 * 按类型获取范围元数据
 */
export function getScopeMetadataByType(scopeType: ScopeTypeCode) {
  return http.get<ScopeMetadataDTO[]>('/v2/scopes/metadata', {
    params: { scopeType }
  })
}

/**
 * 检查权限
 */
export function checkPermission(data: PermissionCheckDTO) {
  return http.post<PermissionCheckDTO>('/v2/scopes/check', data)
}

/**
 * 获取用户可访问的班级ID列表
 */
export function getAccessibleClassIds(userId: number) {
  return http.get<number[]>(`/v2/scopes/accessible/classes/${userId}`)
}
