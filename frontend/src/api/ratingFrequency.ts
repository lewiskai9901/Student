import { http } from '@/utils/request'

/**
 * 评级频次统计API
 */

// ==================== 类型定义 ====================

/**
 * 评级频次查询参数
 */
export interface RatingFrequencyQueryDTO {
  checkPlanId?: number | string
  ruleId?: number | string
  levelIds?: (number | string)[]
  classIds?: (number | string)[]
  gradeIds?: (number | string)[]
  periodType?: string
  periodStart?: string
  periodEnd?: string
  sortBy?: string
  sortOrder?: string
  limit?: number
}

/**
 * 评级频次视图
 */
export interface RatingFrequencyVO {
  id: number | string
  checkPlanId: number | string
  classId: number | string
  className: string
  gradeId?: number | string
  gradeName?: string
  ruleId: number | string
  ruleName: string
  levelId: number | string
  levelName: string
  levelColor?: string
  levelOrder?: number
  frequency: number
  totalRatings: number
  frequencyRate: number
  ranking?: number
  periodType: string
  periodStart: string
  periodEnd: string
  periodLabel?: string
  lastRatingDate?: string
}

/**
 * 等级分布统计
 */
export interface LevelDistributionVO {
  levelId: number | string
  levelName: string
  levelColor?: string
  levelOrder?: number
  classCount: number
  totalFrequency: number
  percentage: number
}

/**
 * TOP班级信息
 */
export interface TopClassVO {
  classId: number | string
  className: string
  gradeName?: string
  frequency: number
  ranking: number
}

/**
 * 等级TOP班级
 */
export interface LevelTopClassesVO {
  levelId: number | string
  levelName: string
  levelColor?: string
  topClasses: TopClassVO[]
}

/**
 * 评级频次汇总统计
 */
export interface RatingFrequencySummaryVO {
  checkPlanId: number | string
  checkPlanName?: string
  ruleId?: number | string
  ruleName?: string
  periodType: string
  periodStart: string
  periodEnd: string
  periodLabel?: string
  totalClasses: number
  totalRatings: number
  levelDistribution: LevelDistributionVO[]
  levelTopClasses: LevelTopClassesVO[]
}

// ==================== API接口 ====================

/**
 * 计算指定周期的评级频次
 */
export function calculateFrequency(
  checkPlanId: number | string,
  periodType: string,
  periodStart: string,
  periodEnd: string
) {
  return http.post<void>('/rating-frequency/calculate', null, {
    params: { checkPlanId, periodType, periodStart, periodEnd }
  })
}

/**
 * 计算当前周期的评级频次
 */
export function calculateCurrentPeriodFrequency(
  checkPlanId: number | string,
  periodType: string
) {
  return http.post<void>('/rating-frequency/calculate/current', null, {
    params: { checkPlanId, periodType }
  })
}

/**
 * 查询评级频次列表
 */
export function queryFrequency(query: RatingFrequencyQueryDTO) {
  return http.get<RatingFrequencyVO[]>('/rating-frequency/list', { params: query })
}

/**
 * 获取评级频次汇总统计
 */
export function getFrequencySummary(
  checkPlanId: number | string,
  periodType: string,
  periodStart: string,
  periodEnd: string,
  ruleId?: number | string
) {
  return http.get<RatingFrequencySummaryVO>('/rating-frequency/summary', {
    params: { checkPlanId, periodType, periodStart, periodEnd, ruleId }
  })
}

/**
 * 获取班级评级频次历史
 */
export function getClassFrequencyHistory(
  classId: number | string,
  ruleId: number | string,
  periodType: string,
  limit?: number
) {
  return http.get<RatingFrequencyVO[]>(`/rating-frequency/class/${classId}/history`, {
    params: { ruleId, periodType, limit }
  })
}

/**
 * 获取指定等级的TOP班级
 */
export function getTopClassesByLevel(
  ruleId: number | string,
  levelId: number | string,
  periodType: string,
  periodStart: string,
  topN?: number
) {
  return http.get<RatingFrequencyVO[]>('/rating-frequency/top-classes', {
    params: { ruleId, levelId, periodType, periodStart, topN }
  })
}

// ==================== 常量 ====================

/**
 * 周期类型
 */
export const PERIOD_TYPES = {
  WEEK: 'WEEK',
  MONTH: 'MONTH',
  QUARTER: 'QUARTER',
  SEMESTER: 'SEMESTER',
  YEAR: 'YEAR',
  CUSTOM: 'CUSTOM'
} as const

export const PERIOD_TYPE_LABELS: Record<string, string> = {
  [PERIOD_TYPES.WEEK]: '周',
  [PERIOD_TYPES.MONTH]: '月',
  [PERIOD_TYPES.QUARTER]: '季度',
  [PERIOD_TYPES.SEMESTER]: '学期',
  [PERIOD_TYPES.YEAR]: '年',
  [PERIOD_TYPES.CUSTOM]: '自定义'
}

// ==================== 导出API ====================

/**
 * 评级频次导出请求参数
 */
export interface RatingFrequencyExportRequest {
  checkPlanId?: number | string
  ruleId?: number | string
  levelIds?: (number | string)[]
  classIds?: (number | string)[]
  gradeIds?: (number | string)[]
  periodType?: string
  periodStart?: string
  periodEnd?: string
  format?: string
}

/**
 * 导出评级频次统计
 */
export function exportFrequency(data: RatingFrequencyExportRequest) {
  return http.post<Blob>('/rating-frequency/export', data, {
    responseType: 'blob'
  })
}
