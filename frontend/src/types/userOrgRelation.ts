/**
 * 用户-组织关系类型定义
 */

/**
 * 关系类型枚举
 */
export type RelationType = 'PRIMARY' | 'SECONDARY' | 'TEMPORARY' | 'SUPERVISING'

/**
 * 关系类型标签映射
 */
export const RelationTypeLabels: Record<RelationType, string> = {
  PRIMARY: '主归属',
  SECONDARY: '副职',
  TEMPORARY: '临时借调',
  SUPERVISING: '分管'
}

/**
 * 用户-组织关系
 */
export interface UserOrgRelation {
  id: number
  userId: number
  orgUnitId: number
  relationType: RelationType
  relationTypeLabel: string
  positionTitle: string | null
  positionLevel: number | null
  isPrimary: boolean
  isLeader: boolean
  canManage: boolean
  canApprove: boolean
  startDate: string | null
  endDate: string | null
  weightRatio: number
  sortOrder: number
  remark: string | null
  isActive: boolean
  isExpired: boolean
  isExpiringSoon: boolean
}

/**
 * 添加关系请求
 */
export interface AddUserOrgRelationRequest {
  userId: number
  orgUnitId: number
  relationType: RelationType
  positionTitle?: string
  positionLevel?: number
  isPrimary?: boolean
  isLeader?: boolean
  canManage?: boolean
  canApprove?: boolean
  startDate?: string
  endDate?: string
  weightRatio?: number
  sortOrder?: number
  remark?: string
}

/**
 * 更新关系请求
 */
export interface UpdateUserOrgRelationRequest {
  positionTitle?: string
  positionLevel?: number
  isLeader?: boolean
  canManage?: boolean
  canApprove?: boolean
  startDate?: string
  endDate?: string
  weightRatio?: number
  sortOrder?: number
  remark?: string
}

/**
 * 用户组织关系表单数据
 */
export interface UserOrgRelationFormData {
  userId: number | null
  orgUnitId: number | null
  relationType: RelationType
  positionTitle: string
  positionLevel: number | null
  isPrimary: boolean
  isLeader: boolean
  canManage: boolean
  canApprove: boolean
  startDate: string
  endDate: string
  weightRatio: number
  sortOrder: number
  remark: string
}

/**
 * 创建默认表单数据
 */
export function createDefaultUserOrgRelationFormData(): UserOrgRelationFormData {
  return {
    userId: null,
    orgUnitId: null,
    relationType: 'PRIMARY',
    positionTitle: '',
    positionLevel: null,
    isPrimary: false,
    isLeader: false,
    canManage: false,
    canApprove: false,
    startDate: '',
    endDate: '',
    weightRatio: 100,
    sortOrder: 0,
    remark: ''
  }
}

/**
 * 关系类型选项
 */
export const relationTypeOptions = [
  { value: 'PRIMARY', label: '主归属', description: '用户的主要所属组织' },
  { value: 'SECONDARY', label: '副职', description: '用户在其他组织的兼职' },
  { value: 'TEMPORARY', label: '临时借调', description: '临时调派到其他组织' },
  { value: 'SUPERVISING', label: '分管', description: '分管某个组织但不属于该组织' }
]
