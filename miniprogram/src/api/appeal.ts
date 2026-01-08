/**
 * 申诉管理API
 */
import { get, post, put } from '@/utils/request'

// 申诉状态
export enum AppealStatus {
  PENDING = 1,     // 待审核
  APPROVED = 2,    // 已通过
  REJECTED = 3,    // 已驳回
  WITHDRAWN = 4,   // 已撤销
  EXPIRED = 5,     // 已过期
  PUBLICITY = 6,   // 公示中
  EFFECTIVE = 7    // 已生效
}

// 申诉类型
export enum AppealType {
  SCORE = 1,       // 分数异议
  FACT = 2,        // 事实异议
  PROCEDURE = 3    // 程序异议
}

// 申诉信息
export interface Appeal {
  id: string | number
  appealCode: string
  recordId: number
  itemId: number
  classId: number
  className: string
  gradeId: number
  categoryId: number
  categoryName: string
  itemName: string
  originalScore: number
  linkInfo?: string
  originalPhotoUrls?: string[]
  originalRemark?: string

  // 申诉信息
  appealType: AppealType
  appealReason: string
  expectedScore?: number
  evidenceUrls?: string[]
  appellantId: number
  appellantName: string
  appellantRole: string
  appealTime: string
  appealDeadline?: string

  // 审核信息
  status: AppealStatus
  currentStep?: number
  finalReviewerId?: number
  finalReviewerName?: string
  finalReviewTime?: string
  finalReviewOpinion?: string

  // 分数调整
  adjustedScore?: number
  scoreChange?: number

  // 公示信息
  publicityStartTime?: string
  publicityEndTime?: string
  publicityDays?: number
  effectiveTime?: string
}

// 申诉统计
export interface AppealStats {
  myAppeals: number
  pendingReview: number
  inPublicity: number
  totalAppeals: number
}

// 分页结果
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

// 查询参数
export interface AppealQueryParams {
  status?: number
  classId?: number
  startDate?: string
  endDate?: string
  pageNum?: number
  pageSize?: number
}

// 创建申诉请求
export interface CreateAppealRequest {
  recordId: string | number
  itemId: string | number
  appealType: AppealType
  appealReason: string
  expectedScore?: number
  evidenceUrls?: string[]
}

// 审核申诉请求
export interface ReviewAppealRequest {
  approvalStatus: AppealStatus.APPROVED | AppealStatus.REJECTED
  approvalOpinion: string
  adjustedScore?: number
}

/**
 * 获取申诉列表
 */
export function getAppealList(params: AppealQueryParams = {}) {
  return get<PageResult<Appeal>>('/quantification/appeals', {
    pageNum: params.pageNum || 1,
    pageSize: params.pageSize || 10,
    ...params
  })
}

/**
 * 获取待审核申诉
 */
export function getPendingAppeals(params?: { pageNum?: number; pageSize?: number }) {
  return get<PageResult<Appeal>>('/quantification/appeals/pending', {
    pageNum: params?.pageNum || 1,
    pageSize: params?.pageSize || 10
  })
}

/**
 * 获取公示中的申诉
 */
export function getPublicityAppeals(params?: { pageNum?: number; pageSize?: number }) {
  return get<PageResult<Appeal>>('/quantification/appeals/publicity', {
    pageNum: params?.pageNum || 1,
    pageSize: params?.pageSize || 10
  })
}

/**
 * 获取申诉详情
 */
export function getAppealDetail(appealId: string | number) {
  return get<Appeal>(`/quantification/appeals/${appealId}`)
}

/**
 * 检查是否可以申诉
 */
export function checkCanAppeal(itemId: string | number) {
  return get<{ canAppeal: boolean; reason?: string }>(`/quantification/appeals/check/${itemId}`)
}

/**
 * 提交申诉
 */
export function createAppeal(data: CreateAppealRequest) {
  return post<Appeal>('/quantification/appeals', data)
}

/**
 * 审核申诉
 */
export function reviewAppeal(appealId: string | number, data: ReviewAppealRequest) {
  return put<Appeal>(`/quantification/appeals/${appealId}/review`, data)
}

/**
 * 撤销申诉
 */
export function withdrawAppeal(appealId: string | number, reason?: string) {
  return put<void>(`/quantification/appeals/${appealId}/withdraw`, { reason })
}

/**
 * 获取申诉统计
 */
export function getAppealStatistics(params?: { scope?: string; scopeId?: number; period?: string }) {
  return get<AppealStats>('/quantification/appeals/statistics', {
    scope: params?.scope || 'all',
    period: params?.period || 'month'
  })
}

/**
 * 获取我的申诉列表
 */
export function getMyAppeals(params?: { pageNum?: number; pageSize?: number }) {
  return get<PageResult<Appeal>>('/quantification/appeals', {
    pageNum: params?.pageNum || 1,
    pageSize: params?.pageSize || 10
  })
}

/**
 * 获取申诉摘要统计
 */
export function getAppealSummary() {
  return get<AppealStats>('/quantification/appeals/summary')
}
