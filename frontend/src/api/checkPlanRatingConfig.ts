import { http } from '@/utils/request'

/**
 * 评级配置管理 API
 * @module checkPlanRatingConfig
 */

// ============ 类型定义 ============

/**
 * 排名数据源 DTO
 */
export interface RatingRankingSourceDTO {
  sourceType: 'TOTAL_SCORE' | 'CATEGORY' | 'DEDUCTION_ITEM'
  sourceId?: string | number
  useWeighted: number
  weight?: number
  missingDataStrategy: 'ZERO' | 'SKIP'
  sortOrder?: number
}

/**
 * 评级配置创建 DTO
 */
export interface RatingConfigCreateDTO {
  checkPlanId: string | number
  ratingName: string
  ratingType: 'DAILY' | 'WEEKLY' | 'MONTHLY'
  icon?: string
  color?: string
  priority?: number
  divisionMethod: 'TOP_N' | 'TOP_PERCENT' | 'BOTTOM_N' | 'BOTTOM_PERCENT' | 'OTHER'
  divisionValue?: number | null
  requireApproval?: number
  autoPublish?: number
  enabled?: number
  sortOrder?: number
  description?: string
  rankingSources: RatingRankingSourceDTO[]
}

/**
 * 评级配置更新 DTO
 */
export interface RatingConfigUpdateDTO {
  id: string | number
  ratingName: string
  icon?: string
  color?: string
  priority?: number
  divisionMethod: 'TOP_N' | 'TOP_PERCENT' | 'BOTTOM_N' | 'BOTTOM_PERCENT' | 'OTHER'
  divisionValue?: number | null
  requireApproval?: number
  autoPublish?: number
  enabled?: number
  sortOrder?: number
  description?: string
  rankingSources: RatingRankingSourceDTO[]
  changeDescription?: string
}

/**
 * 排名数据源 VO
 */
export interface RatingRankingSourceVO {
  id: string | number
  ratingConfigId: string | number
  sourceType: string
  sourceTypeText: string
  sourceId?: string | number
  sourceName?: string
  useWeighted: number
  weight?: number
  missingDataStrategy: string
  missingDataStrategyText: string
  sortOrder: number
}

/**
 * 评级配置 VO
 */
export interface RatingConfigVO {
  id: string | number
  checkPlanId: string | number
  checkPlanName: string
  ratingName: string
  ratingType: string
  ratingTypeText: string
  icon?: string
  color?: string
  priority: number
  divisionMethod: string
  divisionMethodText: string
  divisionValue: number
  requireApproval: number
  autoPublish: number
  enabled: number
  sortOrder: number
  description?: string
  createdBy: string | number
  createdByName: string
  createdAt: string
  updatedAt: string
  rankingSources: RatingRankingSourceVO[]
  currentVersion: number
}

/**
 * 评级配置查询 DTO
 */
export interface RatingConfigQueryDTO {
  checkPlanId?: string | number
  ratingName?: string
  ratingType?: string
  enabled?: number
  pageNum?: number
  pageSize?: number
}

// ============ API 方法 ============

/**
 * 创建评级配置
 */
export function createRatingConfig(data: RatingConfigCreateDTO) {
  return http.post('/quantification/rating/config', data)
}

/**
 * 更新评级配置
 */
export function updateRatingConfig(data: RatingConfigUpdateDTO) {
  return http.put('/quantification/rating/config', data)
}

/**
 * 删除评级配置
 */
export function deleteRatingConfig(id: string | number) {
  return http.delete(`/quantification/rating/config/${id}`)
}

/**
 * 启用/禁用评级配置
 */
export function toggleRatingConfigEnabled(id: string | number, enabled: boolean) {
  return http.put(`/quantification/rating/config/${id}/toggle`, null, {
    params: { enabled }
  })
}

/**
 * 获取评级配置详情
 */
export function getRatingConfigDetail(id: string | number) {
  return http.get<RatingConfigVO>(`/quantification/rating/config/${id}`)
}

/**
 * 分页查询评级配置
 */
export function getRatingConfigPage(params: RatingConfigQueryDTO) {
  return http.get('/quantification/rating/config/page', { params })
}

/**
 * 获取检查计划的所有评级配置
 */
export function getRatingConfigsByPlan(checkPlanId: string | number) {
  return http.get<RatingConfigVO[]>(`/quantification/rating/config/plan/${checkPlanId}`)
}
