/**
 * 用户-场所关系类型定义
 */

/**
 * 关系类型枚举
 */
export type UserSpaceRelationType = 'ASSIGNED' | 'MANAGED' | 'TEMPORARY'

/**
 * 关系类型标签映射
 */
export const UserSpaceRelationTypeLabels: Record<UserSpaceRelationType, string> = {
  ASSIGNED: '分配',
  MANAGED: '管理',
  TEMPORARY: '临时'
}

/**
 * 用户-场所关系
 */
export interface UserSpaceRelation {
  id: number
  userId: number
  spaceId: number
  relationType: UserSpaceRelationType
  relationTypeLabel: string
  positionCode: string | null
  positionName: string | null
  isPrimary: boolean
  canUse: boolean
  canManage: boolean
  startDate: string | null
  endDate: string | null
  feeAmount: number | null
  feePaid: boolean
  sortOrder: number
  remark: string | null
  isActive: boolean
  isExpired: boolean
  isExpiringSoon: boolean
  needsPayment: boolean
}

/**
 * 添加关系请求
 */
export interface AddUserSpaceRelationRequest {
  userId: number
  spaceId: number
  relationType: UserSpaceRelationType
  positionCode?: string
  positionName?: string
  isPrimary?: boolean
  canUse?: boolean
  canManage?: boolean
  startDate?: string
  endDate?: string
  feeAmount?: number
  feePaid?: boolean
  sortOrder?: number
  remark?: string
}

/**
 * 更新关系请求
 */
export interface UpdateUserSpaceRelationRequest {
  positionCode?: string
  positionName?: string
  canUse?: boolean
  canManage?: boolean
  startDate?: string
  endDate?: string
  feeAmount?: number
  feePaid?: boolean
  sortOrder?: number
  remark?: string
}

/**
 * 用户场所关系表单数据
 */
export interface UserSpaceRelationFormData {
  userId: number | null
  spaceId: number | null
  relationType: UserSpaceRelationType
  positionCode: string
  positionName: string
  isPrimary: boolean
  canUse: boolean
  canManage: boolean
  startDate: string
  endDate: string
  feeAmount: number | null
  feePaid: boolean
  sortOrder: number
  remark: string
}

/**
 * 创建默认表单数据
 */
export function createDefaultUserSpaceRelationFormData(): UserSpaceRelationFormData {
  return {
    userId: null,
    spaceId: null,
    relationType: 'ASSIGNED',
    positionCode: '',
    positionName: '',
    isPrimary: false,
    canUse: true,
    canManage: false,
    startDate: '',
    endDate: '',
    feeAmount: null,
    feePaid: false,
    sortOrder: 0,
    remark: ''
  }
}

/**
 * 关系类型选项
 */
export const userSpaceRelationTypeOptions = [
  { value: 'ASSIGNED', label: '分配', description: '正式分配的场所（如宿舍床位）' },
  { value: 'MANAGED', label: '管理', description: '负责管理的场所（如宿管员）' },
  { value: 'TEMPORARY', label: '临时', description: '临时使用的场所' }
]
