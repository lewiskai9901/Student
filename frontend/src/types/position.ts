/**
 * 岗位与变更日志类型定义
 */

// 岗位
export interface Position {
  id: number | string
  positionCode: string
  positionName: string
  orgUnitId: number | string
  orgUnitName?: string
  jobLevel?: string
  headcount: number
  currentCount?: number
  reportsToId?: number | string
  reportsToName?: string
  responsibilities?: string
  requirements?: string
  sortOrder: number
  isKeyPosition: boolean
  enabled: boolean
  createdAt?: string
  updatedAt?: string
}

// 岗位编制情况
export interface PositionStaffing {
  positionId: number | string
  positionCode: string
  positionName: string
  headcount: number
  currentCount: number
  vacancies: number
  overstaffed: boolean
  holders: UserPositionSimple[]
}

// 简易在岗信息
export interface UserPositionSimple {
  id: number | string
  userId: number | string
  userName?: string
  isPrimary: boolean
  appointmentType?: string
  startDate?: string
}

// 用户岗位
export interface UserPosition {
  id: number | string
  userId: number | string
  userName?: string
  positionId: number | string
  positionName?: string
  orgUnitName?: string
  isPrimary: boolean
  appointmentType?: string
  startDate: string
  endDate?: string
  reason?: string
  isCurrent: boolean
  createdAt?: string
}

// 岗位级别
export type JobLevel = 'HIGH' | 'MIDDLE' | 'BASE' | 'EXECUTIVE'
export const JobLevelConfig: Record<JobLevel, { label: string }> = {
  HIGH: { label: '高级' },
  MIDDLE: { label: '中级' },
  BASE: { label: '初级' },
  EXECUTIVE: { label: '管理层' },
}

// 任职类型
export type AppointmentType = 'FORMAL' | 'ACTING' | 'CONCURRENT' | 'PROBATION'
export const AppointmentTypeConfig: Record<AppointmentType, { label: string }> = {
  FORMAL: { label: '正式任命' },
  ACTING: { label: '代理' },
  CONCURRENT: { label: '兼任' },
  PROBATION: { label: '试用' },
}

// 组织成员（基于岗位或归属）
export interface OrgMember {
  userPositionId?: string | number
  userId: string | number
  userName: string
  positionId?: string | number
  positionName?: string
  appointmentType?: string
  isPrimary: boolean
  startDate?: string
  jobLevel?: string
  isKeyPosition?: boolean
  userTypeCode?: string
  /** belonging = 归属成员, staff = 岗位人员 */
  membershipType?: string
  /** 用户归属组织ID */
  primaryOrgUnitId?: string | number
  /** 用户归属组织名称 */
  primaryOrgUnitName?: string
  /** 岗位所在组织ID（递归查询时用） */
  orgUnitId?: string | number
  /** 岗位所在组织名称 */
  orgUnitName?: string
}

// 组织统计
export interface OrgStatistics {
  orgUnitId: string | number
  belongingCount: number
  staffCount: number
  countByUserType: Record<string, number>
}

export const AppointmentTypeLabels: Record<string, string> = {
  FORMAL: '正式',
  ACTING: '代理',
  CONCURRENT: '兼职',
  PROBATION: '试用',
}

// 创建岗位请求
export interface CreatePositionRequest {
  positionCode: string
  positionName: string
  orgUnitId: number | string
  jobLevel?: string
  headcount?: number
  reportsToId?: number | string
  responsibilities?: string
  requirements?: string
  isKeyPosition?: boolean
}

// 更新岗位请求
export interface UpdatePositionRequest {
  positionName?: string
  jobLevel?: string
  headcount?: number
  reportsToId?: number | string
  responsibilities?: string
  requirements?: string
  isKeyPosition?: boolean
  sortOrder?: number
}

// 任命请求
export interface AppointRequest {
  userId: number | string
  positionId: number | string
  isPrimary?: boolean
  appointmentType?: string
  startDate?: string
  reason?: string
}

// 离任请求
export interface EndAppointmentRequest {
  endDate?: string
  reason?: string
}

// 调岗请求
export interface TransferRequest {
  userId: number | string
  fromPositionId: number | string
  toPositionId: number | string
  transferDate?: string
  reason?: string
}

