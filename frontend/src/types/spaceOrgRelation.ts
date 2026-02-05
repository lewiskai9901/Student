/**
 * 场所-组织关系类型定义
 */

/**
 * 关系类型枚举
 */
export type SpaceRelationType = 'PRIMARY' | 'SHARED' | 'MANAGED'

/**
 * 关系类型标签映射
 */
export const SpaceRelationTypeLabels: Record<SpaceRelationType, string> = {
  PRIMARY: '主归属',
  SHARED: '共用',
  MANAGED: '托管'
}

/**
 * 场所-组织关系
 */
export interface SpaceOrgRelation {
  id: number
  spaceId: number
  orgUnitId: number
  relationType: SpaceRelationType
  relationTypeLabel: string
  isPrimary: boolean
  priorityLevel: number | null
  canUse: boolean
  canManage: boolean
  canAssign: boolean
  canInspect: boolean
  useSchedule: string | null
  startDate: string | null
  endDate: string | null
  allocatedCapacity: number | null
  weightRatio: number
  sortOrder: number
  remark: string | null
  isActive: boolean
  isExpired: boolean
  isExpiringSoon: boolean
  hasFullManagementRights: boolean
}

/**
 * 添加关系请求
 */
export interface AddSpaceOrgRelationRequest {
  spaceId: number
  orgUnitId: number
  relationType: SpaceRelationType
  isPrimary?: boolean
  priorityLevel?: number
  canUse?: boolean
  canManage?: boolean
  canAssign?: boolean
  canInspect?: boolean
  useSchedule?: string
  startDate?: string
  endDate?: string
  allocatedCapacity?: number
  weightRatio?: number
  sortOrder?: number
  remark?: string
}

/**
 * 更新关系请求
 */
export interface UpdateSpaceOrgRelationRequest {
  priorityLevel?: number
  canUse?: boolean
  canManage?: boolean
  canAssign?: boolean
  canInspect?: boolean
  useSchedule?: string
  startDate?: string
  endDate?: string
  allocatedCapacity?: number
  weightRatio?: number
  sortOrder?: number
  remark?: string
}

/**
 * 场所组织关系表单数据
 */
export interface SpaceOrgRelationFormData {
  spaceId: number | null
  orgUnitId: number | null
  relationType: SpaceRelationType
  isPrimary: boolean
  priorityLevel: number
  canUse: boolean
  canManage: boolean
  canAssign: boolean
  canInspect: boolean
  useSchedule: string
  startDate: string
  endDate: string
  allocatedCapacity: number | null
  weightRatio: number
  sortOrder: number
  remark: string
}

/**
 * 创建默认表单数据
 */
export function createDefaultSpaceOrgRelationFormData(): SpaceOrgRelationFormData {
  return {
    spaceId: null,
    orgUnitId: null,
    relationType: 'PRIMARY',
    isPrimary: false,
    priorityLevel: 1,
    canUse: true,
    canManage: false,
    canAssign: false,
    canInspect: false,
    useSchedule: '',
    startDate: '',
    endDate: '',
    allocatedCapacity: null,
    weightRatio: 100,
    sortOrder: 0,
    remark: ''
  }
}

/**
 * 关系类型选项
 */
export const spaceRelationTypeOptions = [
  { value: 'PRIMARY', label: '主归属', description: '场所的主要归属组织' },
  { value: 'SHARED', label: '共用', description: '多个组织共同使用的场所' },
  { value: 'MANAGED', label: '托管', description: '由其他组织代为管理' }
]

/**
 * 使用时间安排格式（JSON结构）
 */
export interface UseSchedule {
  weekdays?: number[]  // 周几可用 [1-7]
  timeSlots?: {        // 时间段
    start: string      // HH:mm
    end: string        // HH:mm
  }[]
  description?: string // 描述
}
