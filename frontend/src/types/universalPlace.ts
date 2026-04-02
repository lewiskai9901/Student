/**
 * 通用空间管理类型定义
 */

import type { ConfigurableType, TypeTreeNode } from './configurableType'

// ==================== 属性 Schema ====================

export interface AttributeSelectOption {
  value: string
  label: string
}

export interface AttributeFieldDefinition {
  key: string
  label: string
  type: 'string' | 'number' | 'select' | 'textarea' | 'boolean' | 'date'
  required?: boolean
  placeholder?: string
  defaultValue?: any
  maxLength?: number
  min?: number
  max?: number
  step?: number
  precision?: number
  pattern?: string
  rows?: number
  format?: string
  multiple?: boolean
  options?: AttributeSelectOption[]
  showInTree?: boolean
  showInDetail?: boolean
  sortOrder?: number
  builtIn?: boolean
}

export interface AttributeSchema {
  fields: AttributeFieldDefinition[]
}

// ==================== 基础分类 ====================

/**
 * 场所分类（BaseCategory 枚举值）
 */
export interface PlaceCategory {
  code: string
  label: string
  defaultFeatures: Record<string, boolean>
  allowedChildCategories: string[]
  leaf: boolean
  root: boolean
}

// ==================== 空间类型 ====================

/**
 * 场所类型配置（统一类型系统 Phase 3）
 */
export interface UniversalPlaceType extends ConfigurableType {
  // 跨领域关联（PlaceType 特有）
  defaultUserTypeCodes: string[] | null
  defaultOrgTypeCodes: string[] | null

  // 场所特有
  rootType: boolean
  capacityUnit?: string
  defaultCapacity?: number
}

/**
 * 场所类型树节点
 */
export type PlaceTypeTreeNode = TypeTreeNode<UniversalPlaceType>

/**
 * 创建场所类型请求
 */
export interface CreatePlaceTypeRequest {
  typeCode?: string
  typeName: string
  category?: string
  parentTypeCode?: string
  description?: string
  features?: Record<string, boolean>
  metadataSchema?: string
  allowedChildTypeCodes?: string[]
  maxDepth?: number
  defaultUserTypeCodes?: string[]
  defaultOrgTypeCodes?: string[]
  capacityUnit?: string
  defaultCapacity?: number
  sortOrder?: number
}

/**
 * 更新场所类型请求
 */
export interface UpdatePlaceTypeRequest {
  typeName?: string
  category?: string
  description?: string
  features?: Record<string, boolean>
  metadataSchema?: string
  allowedChildTypeCodes?: string[]
  maxDepth?: number
  defaultUserTypeCodes?: string[]
  defaultOrgTypeCodes?: string[]
  capacityUnit?: string
  defaultCapacity?: number
  sortOrder?: number
}

// ==================== 空间实例 ====================

/**
 * 空间状态
 */
export enum PlaceStatus {
  DISABLED = 0,
  NORMAL = 1,
  MAINTENANCE = 2
}

/**
 * 空间状态标签
 */
export const PlaceStatusLabels: Record<PlaceStatus, string> = {
  [PlaceStatus.DISABLED]: '停用',
  [PlaceStatus.NORMAL]: '正常',
  [PlaceStatus.MAINTENANCE]: '维护中'
}

/**
 * 空间状态颜色
 */
export const PlaceStatusColors: Record<PlaceStatus, string> = {
  [PlaceStatus.DISABLED]: 'info',
  [PlaceStatus.NORMAL]: 'success',
  [PlaceStatus.MAINTENANCE]: 'warning'
}

/**
 * 空间实例
 */
export interface UniversalPlace {
  id: number | string
  placeCode: string
  placeName: string
  typeCode: string
  typeName?: string
  description?: string

  parentId?: number | string
  parentName?: string
  path?: string
  level: number

  capacity?: number
  currentOccupancy?: number

  orgUnitId?: number | string
  orgUnitName?: string

  // V23: NULL继承模型
  parentOrgUnitId?: number | string      // 父级的组织单元ID
  isOrgInherited?: boolean      // 是否继承父级组织（orgUnitId为null时为true）
  effectiveOrgUnitId?: number | string   // 有效的组织单元ID（经过继承计算）
  effectiveOrgUnitName?: string // 有效组织名称（继承时填充）

  responsibleUserId?: number | string
  responsibleUserName?: string

  // 负责人继承
  effectiveResponsibleUserId?: number | string
  effectiveResponsibleUserName?: string
  isResponsibleInherited?: boolean

  gender?: string          // MALE / FEMALE / MIXED / null
  effectiveGender?: string // 继承后的有效性别

  status: PlaceStatus
  attributes?: Record<string, any>
}

/**
 * 空间树节点
 */
export interface PlaceTreeNode extends UniversalPlace {
  children?: PlaceTreeNode[]

  // 类型特性（从类型配置继承）
  typeIcon?: string
  hasCapacity?: boolean
  bookable?: boolean
  assignable?: boolean
  occupiable?: boolean
  capacityUnit?: string
  leaf?: boolean

  // 性别
  gender?: string
  effectiveGender?: string

  // V23: 继承模型（已在UniversalPlace中定义，这里继承）
  parentOrgUnitId?: number | string
  isOrgInherited?: boolean
  effectiveOrgUnitId?: number | string

  // 占用率（用于告警显示）
  occupancyRate?: number

  // 树节点展示属性
  treeDisplayAttributes?: { key: string; label: string; value: string }[]
}

/**
 * 创建空间请求
 */
export interface CreatePlaceRequest {
  placeCode: string
  placeName: string
  typeCode: string
  description?: string
  parentId?: number | string
  status?: number
  capacity?: number
  gender?: string
  orgUnitId?: number | string
  responsibleUserId?: number | string
  attributes?: Record<string, any>
}

/**
 * 更新空间请求
 */
export interface UpdatePlaceRequest {
  placeCode?: string
  placeName?: string
  description?: string
  status?: number
  capacity?: number
  gender?: string
  orgUnitId?: number | string
  responsibleUserId?: number | string
  attributes?: Record<string, any>
}

// ==================== 占用记录 ====================

/**
 * 空间占用记录
 */
export interface PlaceOccupant {
  id: number | string
  placeId: number | string
  placeName?: string
  occupantType: string
  occupantId: number | string
  occupantName?: string
  username?: string
  orgUnitName?: string
  userTypeName?: string
  gender?: number
  positionNo?: string
  checkInTime: string
  checkOutTime?: string
  status: number
  remark?: string
}

/**
 * 占用记录（带场所信息）
 */
export interface PlaceOccupantWithPlace extends PlaceOccupant {
  placeName?: string
  placeCode?: string
  buildingName?: string
}

/**
 * 入住请求
 */
export interface CheckInRequest {
  occupantType: string
  occupantId: number | string
  occupantName?: string
  username?: string
  orgUnitName?: string
  gender?: number
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
export interface BookingAttendeeInfo {
  userId: number | string
  username: string
  realName: string
}

export interface PlaceBooking {
  id: number | string
  placeId: number | string
  placeName?: string
  bookerId: number | string
  bookerName?: string
  title?: string
  startTime: string
  endTime: string
  attendeeIds?: (number | string)[]
  attendees?: BookingAttendeeInfo[]
  status: BookingStatus
  remark?: string
}

export interface BookingSeatAssignment {
  id: number | string
  bookingId: number | string
  positionNo: string
  userId: number | string
  userName: string
}

export interface SaveSeatAssignmentRequest {
  positionNo: string
  userId: number | string
  userName: string
}

/**
 * 创建预订请求
 */
export interface CreateBookingRequest {
  title?: string
  startTime: string
  endTime: string
  attendeeIds?: (number | string)[]
  remark?: string
}

// ==================== 平面图布局 ====================

export type FloorPlanElementType =
  | 'seat' | 'desk' | 'wall' | 'door' | 'text'
  | 'podium' | 'blackboard' | 'window'
  | 'pillar' | 'partition' | 'round-table' | 'seat-desk'
  | 'rectangle' | 'circle-shape' | 'line-shape' | 'area'

export interface FloorPlanElement {
  id: string
  type: FloorPlanElementType
  x: number
  y: number
  width?: number
  height?: number
  rotation?: number
  positionNo?: string  // 仅 seat，关联 PlaceOccupant
  label?: string       // desk/podium/blackboard 标签
  text?: string        // text 内容
  fontSize?: number    // text 字号
  fontFamily?: string  // 字体
  zIndex?: number      // Z轴排序
  locked?: boolean     // 锁定防误拖
  opacity?: number     // 透明度 0-1
  fillColor?: string   // 自定义填充色（座位=空位背景）
  occupiedColor?: string // 入座后背景色（仅 seat/seat-desk）
  strokeColor?: string // 自定义边框色
  occupiedStrokeColor?: string // 入座后边框色（仅 seat/seat-desk）
  textColor?: string   // 自定义文字颜色
  occupiedTextColor?: string // 入座后文字颜色（仅 seat/seat-desk）
  radius?: number      // 圆桌/柱子半径
  rowLabel?: string    // 行标签 (A, B, C...)
  categoryColor?: string // 分类颜色
  seatShape?: 'circle' | 'square' | 'rounded' // 座位形状，默认 circle
}

export type FloorStyleType = 'grid' | 'dots' | 'plain'

export interface FloorPlanLayout {
  version: 1 | 2
  stageWidth: number
  stageHeight: number
  elements: FloorPlanElement[]
  backgroundImage?: string | null
  floorStyle?: FloorStyleType
}

// ==================== 辅助函数 ====================

/**
 * 获取空间状态标签
 */
export function getPlaceStatusLabel(status: PlaceStatus): string {
  return PlaceStatusLabels[status] || '未知'
}

/**
 * 获取空间状态颜色
 */
export function getPlaceStatusColor(status: PlaceStatus): string {
  return PlaceStatusColors[status] || 'default'
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
