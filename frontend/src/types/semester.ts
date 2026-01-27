/**
 * 学期管理类型定义 - DDD架构适配
 */

/**
 * 学期类型枚举
 */
export type SemesterType = '第一学期' | '第二学期'

/**
 * 学期状态枚举
 */
export type SemesterStatus = '正常' | '已结束'

/**
 * 学期领域实体
 */
export interface Semester {
  id: number | string
  semesterName: string
  semesterCode: string
  startDate: string
  endDate: string
  startYear?: number
  semesterType?: SemesterType
  isCurrent?: boolean
  status?: SemesterStatus
  durationDays?: number
  isOngoing?: boolean
  createdAt?: string
  updatedAt?: string
}

/**
 * 创建学期请求
 */
export interface CreateSemesterRequest {
  semesterName: string
  startDate: string
  endDate: string
  startYear: number
  semesterType: number
}

/**
 * 更新学期请求
 */
export interface UpdateSemesterRequest {
  semesterName?: string
  startDate?: string
  endDate?: string
}

/**
 * 学期查询参数
 */
export interface SemesterQueryParams {
  pageNum?: number
  pageSize?: number
  semesterName?: string
  status?: number
}
