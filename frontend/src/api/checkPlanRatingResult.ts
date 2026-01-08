import { http } from '@/utils/request'

/**
 * 评级结果管理 API
 * @module checkPlanRatingResult
 */

// ============ 类型定义 ============

/**
 * 评级结果 VO
 */
export interface RatingResultVO {
  id: string | number
  ratingConfigId: string | number
  ratingName: string
  icon?: string
  color?: string
  checkPlanId: string | number
  checkPlanName: string
  classId: string | number
  className: string
  periodType: string
  periodTypeText: string
  periodStart: string
  periodEnd: string
  periodText: string
  ranking: number
  finalScore: number
  awarded: number
  status: string
  statusText: string
  calculatedAt: string
  approvedBy?: string | number
  approvedByName?: string
  approvedAt?: string
  publishedBy?: string | number
  publishedByName?: string
  publishedAt?: string
  createdAt: string
  updatedAt: string
}

/**
 * 评级结果查询 DTO
 */
export interface RatingResultQueryDTO {
  checkPlanId?: string | number
  ratingConfigId?: string | number
  classId?: string | number
  periodType?: string
  periodStartFrom?: string
  periodStartTo?: string
  awarded?: number
  status?: string
  pageNum?: number
  pageSize?: number
}

// ============ API 方法 ============

/**
 * 审核评级结果
 */
export function approveRatingResult(id: string | number, approved: boolean) {
  return http.put(`/quantification/rating/result/${id}/approve`, null, {
    params: { approved }
  })
}

/**
 * 批量审核评级结果
 */
export function batchApproveRatingResults(resultIds: (string | number)[], approved: boolean) {
  return http.put('/quantification/rating/result/batch-approve', resultIds, {
    params: { approved }
  })
}

/**
 * 发布评级结果
 */
export function publishRatingResult(id: string | number) {
  return http.put(`/quantification/rating/result/${id}/publish`)
}

/**
 * 批量发布评级结果
 */
export function batchPublishRatingResults(resultIds: (string | number)[]) {
  return http.put('/quantification/rating/result/batch-publish', resultIds)
}

/**
 * 撤销发布
 */
export function revokeRatingResult(id: string | number) {
  return http.put(`/quantification/rating/result/${id}/revoke`)
}

/**
 * 获取评级结果详情
 */
export function getRatingResultDetail(id: string | number) {
  return http.get<RatingResultVO>(`/quantification/rating/result/${id}`)
}

/**
 * 分页查询评级结果
 */
export function getRatingResultPage(params: RatingResultQueryDTO) {
  return http.get('/quantification/rating/result/page', { params })
}

/**
 * 获取班级的评级历史
 */
export function getClassRatingHistory(classId: string | number, ratingConfigId?: string | number) {
  return http.get<RatingResultVO[]>(`/quantification/rating/result/class/${classId}`, {
    params: { ratingConfigId }
  })
}

// ============ 常量定义 ============

/**
 * 评级周期类型标签
 */
export const RATING_TYPE_LABELS: Record<string, string> = {
  DAILY: '日评级',
  WEEKLY: '周评级',
  MONTHLY: '月评级'
}

/**
 * 划分方式标签
 */
export const DIVISION_METHOD_LABELS: Record<string, string> = {
  TOP_N: '前N名',
  TOP_PERCENT: '前N%',
  BOTTOM_N: '后N名',
  BOTTOM_PERCENT: '后N%'
}

/**
 * 状态标签
 */
export const RATING_STATUS_LABELS: Record<string, string> = {
  DRAFT: '草稿',
  PENDING_APPROVAL: '待审核',
  PUBLISHED: '已发布'
}

/**
 * 数据源类型标签
 */
export const SOURCE_TYPE_LABELS: Record<string, string> = {
  TOTAL_SCORE: '总分',
  CATEGORY: '分类得分',
  DEDUCTION_ITEM: '扣分项得分'
}

/**
 * 缺失数据策略标签
 */
export const MISSING_DATA_STRATEGY_LABELS: Record<string, string> = {
  ZERO: '视为0分',
  SKIP: '跳过不计'
}
