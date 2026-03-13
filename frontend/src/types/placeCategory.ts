/**
 * V10 空间分类类型定义
 * 固定层级 + 可配置分类设计
 */

// ========== 分类层级 ==========

/** 分类适用层级 */
export type SpaceCategoryLevel = 'BUILDING' | 'ROOM'

/** 分类层级选项 */
export const SpaceCategoryLevelOptions = [
  { value: 'BUILDING', label: '楼栋' },
  { value: 'ROOM', label: '房间' }
] as const

// ========== 空间分类DTO ==========

/** 空间分类DTO */
export interface SpaceCategoryDTO {
  id: number
  categoryCode: string
  categoryName: string
  applyToLevel: SpaceCategoryLevel
  applyToLevelDesc?: string
  icon?: string
  color?: string
  description?: string

  // 行为特性
  hasCapacity: boolean
  capacityUnit?: string
  defaultCapacity?: number
  bookable: boolean
  assignable: boolean
  occupiable: boolean
  hasGender: boolean

  // 系统字段
  isSystem: boolean
  isEnabled: boolean
  sortOrder: number

  // 审计字段
  createdBy?: number
  createdAt?: string
  updatedBy?: number
  updatedAt?: string
}

// ========== 请求类型 ==========

/** 创建空间分类请求 */
export interface CreateSpaceCategoryRequest {
  categoryCode: string
  categoryName: string
  applyToLevel: SpaceCategoryLevel
  icon?: string
  color?: string
  description?: string

  // 行为特性
  hasCapacity?: boolean
  capacityUnit?: string
  defaultCapacity?: number
  bookable?: boolean
  assignable?: boolean
  occupiable?: boolean
  hasGender?: boolean

  sortOrder?: number
}

/** 更新空间分类请求 */
export interface UpdateSpaceCategoryRequest {
  categoryName?: string
  icon?: string
  color?: string
  description?: string

  // 行为特性
  hasCapacity?: boolean
  capacityUnit?: string
  defaultCapacity?: number
  bookable?: boolean
  assignable?: boolean
  occupiable?: boolean
  hasGender?: boolean

  sortOrder?: number
}

// ========== 辅助函数 ==========

/** 获取分类层级名称 */
export function getSpaceCategoryLevelName(level: SpaceCategoryLevel): string {
  const names: Record<SpaceCategoryLevel, string> = {
    BUILDING: '楼栋',
    ROOM: '房间'
  }
  return names[level] || level
}

/** 判断是否为楼栋分类 */
export function isBuildingCategory(category: SpaceCategoryDTO): boolean {
  return category.applyToLevel === 'BUILDING'
}

/** 判断是否为房间分类 */
export function isRoomCategory(category: SpaceCategoryDTO): boolean {
  return category.applyToLevel === 'ROOM'
}

/** 判断是否为宿舍分类 */
export function isDormitoryCategory(category: SpaceCategoryDTO): boolean {
  return isRoomCategory(category) && category.occupiable && category.hasGender
}

/** 判断是否为教室分类 */
export function isClassroomCategory(category: SpaceCategoryDTO): boolean {
  return isRoomCategory(category) && category.hasCapacity && category.bookable && !category.occupiable
}

// ========== 预置分类编码 ==========

/** 预置楼栋分类编码 */
export const BuildingCategoryCodes = {
  TEACHING_BUILDING: 'TEACHING_BUILDING',       // 教学楼
  DORMITORY_BUILDING: 'DORMITORY_BUILDING',     // 宿舍楼
  OFFICE_BUILDING: 'OFFICE_BUILDING',           // 办公楼
  COMPLEX_BUILDING: 'COMPLEX_BUILDING',         // 综合楼
  LAB_BUILDING: 'LAB_BUILDING'                  // 实验楼
} as const

/** 预置房间分类编码 */
export const RoomCategoryCodes = {
  DORMITORY: 'DORMITORY',           // 学生宿舍
  CLASSROOM: 'CLASSROOM',           // 普通教室
  MULTIMEDIA_ROOM: 'MULTIMEDIA_ROOM', // 多媒体教室
  LAB: 'LAB',                       // 实验室
  COMPUTER_ROOM: 'COMPUTER_ROOM',   // 机房
  MEETING_ROOM: 'MEETING_ROOM',     // 会议室
  OFFICE: 'OFFICE',                 // 办公室
  WAREHOUSE: 'WAREHOUSE',           // 仓库
  ACTIVITY_ROOM: 'ACTIVITY_ROOM'    // 活动室
} as const
