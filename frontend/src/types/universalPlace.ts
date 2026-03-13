/**
 * 通用空间管理类型定义
 */

// ==================== 空间类型 ====================

/**
 * 空间类型配置
 */
export interface UniversalSpaceType {
  id: number
  typeCode: string
  typeName: string
  icon?: string
  description?: string
  sortOrder: number
  system: boolean      // JSON: 'system' (Jackson strips 'is' prefix from boolean fields)
  enabled: boolean     // JSON: 'enabled'

  // 层级关系
  allowedChildTypes: string[]
  rootType: boolean    // JSON: 'rootType'
  leafType?: boolean   // JSON: 'leafType'

  // 行为特性
  hasCapacity: boolean
  bookable: boolean
  assignable: boolean
  occupiable: boolean

  // 容量配置
  capacityUnit?: string
  defaultCapacity?: number

  // 扩展属性
  attributeSchema?: string
}

/**
 * 空间类型树节点
 */
export interface SpaceTypeTreeNode {
  typeCode: string
  typeName: string
  icon?: string
  hasCapacity: boolean
  bookable: boolean
  assignable: boolean
  occupiable: boolean
  leaf: boolean
  children?: SpaceTypeTreeNode[]
}

/**
 * 创建空间类型请求
 */
export interface CreateSpaceTypeRequest {
  typeName: string
  icon?: string
  description?: string
  sortOrder?: number
  rootType?: boolean
  allowedChildTypes?: string[]
  hasCapacity?: boolean
  bookable?: boolean
  assignable?: boolean
  occupiable?: boolean
  capacityUnit?: string
  defaultCapacity?: number
}

/**
 * 更新空间类型请求
 */
export interface UpdateSpaceTypeRequest {
  typeName?: string
  icon?: string
  description?: string
  sortOrder?: number
  allowedChildTypes?: string[]
  hasCapacity?: boolean
  bookable?: boolean
  assignable?: boolean
  occupiable?: boolean
  capacityUnit?: string
  defaultCapacity?: number
}

// ==================== 空间实例 ====================

/**
 * 空间状态
 */
export enum SpaceStatus {
  DISABLED = 0,
  NORMAL = 1,
  MAINTENANCE = 2
}

/**
 * 空间状态标签
 */
export const SpaceStatusLabels: Record<SpaceStatus, string> = {
  [SpaceStatus.DISABLED]: '停用',
  [SpaceStatus.NORMAL]: '正常',
  [SpaceStatus.MAINTENANCE]: '维护中'
}

/**
 * 空间状态颜色
 */
export const SpaceStatusColors: Record<SpaceStatus, string> = {
  [SpaceStatus.DISABLED]: 'info',
  [SpaceStatus.NORMAL]: 'success',
  [SpaceStatus.MAINTENANCE]: 'warning'
}

/**
 * 空间实例
 */
export interface UniversalSpace {
  id: number
  spaceCode: string
  spaceName: string
  typeCode: string
  typeName?: string
  description?: string

  parentId?: number
  parentName?: string
  path?: string
  level: number

  capacity?: number
  currentOccupancy?: number

  orgUnitId?: number
  orgUnitName?: string
  responsibleUserId?: number
  responsibleUserName?: string

  status: SpaceStatus
  attributes?: Record<string, any>
}

/**
 * 空间树节点
 */
export interface SpaceTreeNode extends UniversalSpace {
  children?: SpaceTreeNode[]

  // 类型特性（从类型配置继承）
  typeIcon?: string
  hasCapacity?: boolean
  bookable?: boolean
  assignable?: boolean
  occupiable?: boolean
  capacityUnit?: string
  leaf?: boolean
}

/**
 * 创建空间请求
 */
export interface CreateSpaceRequest {
  spaceName: string
  typeCode: string
  description?: string
  parentId?: number
  capacity?: number
  orgUnitId?: number
  responsibleUserId?: number
  attributes?: Record<string, any>
}

/**
 * 更新空间请求
 */
export interface UpdateSpaceRequest {
  spaceName?: string
  description?: string
  capacity?: number
  orgUnitId?: number
  responsibleUserId?: number
  attributes?: Record<string, any>
}

// ==================== 占用记录 ====================

/**
 * 空间占用记录
 */
export interface SpaceOccupant {
  id: number
  spaceId: number
  spaceName?: string
  occupantType: string
  occupantId: number
  occupantName?: string
  positionNo?: string
  checkInTime: string
  checkOutTime?: string
  status: number
  remark?: string
}

/**
 * 入住请求
 */
export interface CheckInRequest {
  occupantType: string
  occupantId: number
  occupantName?: string
  positionNo?: string
  remark?: string
}

// ==================== 预订记录 ====================

/**
 * 预订状态
 */
export enum BookingStatus {
  CANCELLED = 0,
  PENDING = 1,
  IN_USE = 2,
  COMPLETED = 3
}

/**
 * 预订状态标签
 */
export const BookingStatusLabels: Record<BookingStatus, string> = {
  [BookingStatus.CANCELLED]: '已取消',
  [BookingStatus.PENDING]: '待使用',
  [BookingStatus.IN_USE]: '使用中',
  [BookingStatus.COMPLETED]: '已完成'
}

/**
 * 空间预订记录
 */
export interface SpaceBooking {
  id: number
  spaceId: number
  spaceName?: string
  bookerId: number
  bookerName?: string
  title?: string
  startTime: string
  endTime: string
  attendeeIds?: number[]
  status: BookingStatus
  remark?: string
}

/**
 * 创建预订请求
 */
export interface CreateBookingRequest {
  title?: string
  startTime: string
  endTime: string
  attendeeIds?: number[]
  remark?: string
}

// ==================== 辅助函数 ====================

/**
 * 获取空间状态标签
 */
export function getSpaceStatusLabel(status: SpaceStatus): string {
  return SpaceStatusLabels[status] || '未知'
}

/**
 * 获取空间状态颜色
 */
export function getSpaceStatusColor(status: SpaceStatus): string {
  return SpaceStatusColors[status] || 'default'
}

/**
 * 获取预订状态标签
 */
export function getBookingStatusLabel(status: BookingStatus): string {
  return BookingStatusLabels[status] || '未知'
}

/**
 * 计算占用率
 */
export function calculateOccupancyRate(current: number, capacity: number): number {
  if (!capacity || capacity === 0) return 0
  return Math.round((current / capacity) * 100)
}

/**
 * 获取占用率颜色
 */
export function getOccupancyRateColor(rate: number): string {
  if (rate >= 90) return 'danger'
  if (rate >= 70) return 'warning'
  return 'success'
}
