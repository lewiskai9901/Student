import { http } from '@/utils/request'

/**
 * 评级统计API - 对接后端RatingStatisticsController
 */

// ==================== 类型定义 ====================

/**
 * 等级频次统计VO
 */
export interface LevelFrequencyVO {
  levelId: number | string
  levelName: string
  levelColor?: string
  levelIcon?: string
  classCount: number
  totalFrequency: number
  percentage: number
  topClasses: ClassLevelDetailVO[]
}

/**
 * 院系对比VO
 */
export interface DepartmentComparisonVO {
  departmentId: number | string
  departmentName: string
  totalClasses: number
  awardedClasses: number
  totalFrequency: number
  awardRate: number
  levelDistribution: Record<string, number>
}

/**
 * 评级频次统计VO
 */
export interface RatingFrequencyStatisticsVO {
  checkPlanId: number | string
  ruleId?: number | string
  ruleName?: string
  periodStart?: string
  periodEnd?: string
  periodType?: string
  totalRatings: number
  awardedClasses: number
  totalClasses: number
  overallAwardRate: number
  levelStatistics: LevelFrequencyVO[]
  departmentComparison: DepartmentComparisonVO[]
}

/**
 * 班级等级详情VO
 */
export interface ClassLevelDetailVO {
  classId: number | string
  className: string
  gradeName?: string
  departmentName?: string
  frequency: number
  frequencyRate: number
  consecutiveCount: number
  bestStreak: number
  recentDates: string[]
  ranking?: number
}

/**
 * 班级评级历史VO
 */
export interface ClassRatingHistoryVO {
  classId: number | string
  className: string
  gradeName?: string
  departmentName?: string
  totalRatings: number
  awardedCount: number
  awardRate: number
  levelFrequencies: Record<string, number>
  recentResults: RatingResultItem[]
  trendData: RatingTrendVO[]
}

/**
 * 评级结果项
 */
export interface RatingResultItem {
  checkDate: string
  levelId: number | string
  levelName: string
  levelColor?: string
  score: number
  ranking?: number
  levelChange?: string
}

/**
 * 评级趋势VO
 */
export interface RatingTrendVO {
  periodLabel: string
  periodStart: string
  periodEnd: string
  levelCounts: Record<string, number>
  totalCount: number
}

// ==================== API接口 ====================

/**
 * 获取评级频次统计
 */
export function getFrequencyStatistics(params: {
  checkPlanId: number | string
  ruleId?: number | string
  levelIds?: (number | string)[]
  periodStart?: string
  periodEnd?: string
  periodType?: string
}) {
  return http.get<RatingFrequencyStatisticsVO>('/rating/statistics/frequency', { params })
}

/**
 * 获取班级评级历史
 */
export function getClassHistory(params: {
  classId: number | string
  checkPlanId: number | string
  ruleId?: number | string
  periodStart?: string
  periodEnd?: string
}) {
  const { classId, ...restParams } = params
  return http.get<ClassRatingHistoryVO>(`/rating/statistics/class/${classId}`, {
    params: restParams
  })
}

/**
 * 获取指定等级的班级列表
 */
export function getClassesByLevel(params: {
  levelId: number | string
  checkPlanId: number | string
  ruleId?: number | string
  periodStart?: string
  periodEnd?: string
  minFrequency?: number
}) {
  const { levelId, ...restParams } = params
  return http.get<ClassLevelDetailVO[]>(`/rating/statistics/level/${levelId}/classes`, {
    params: restParams
  })
}

/**
 * 获取院系对比数据
 */
export function getDepartmentComparison(params: {
  checkPlanId: number | string
  ruleId?: number | string
  levelIds?: (number | string)[]
  periodStart?: string
  periodEnd?: string
}) {
  return http.get<DepartmentComparisonVO[]>('/rating/statistics/department/comparison', {
    params
  })
}

/**
 * 获取评级趋势数据
 */
export function getRatingTrend(params: {
  checkPlanId: number | string
  ruleId?: number | string
  levelId?: number | string
  periodStart: string
  periodEnd: string
  granularity?: 'DAY' | 'WEEK' | 'MONTH'
}) {
  return http.get<RatingTrendVO[]>('/rating/statistics/trend', { params })
}

/**
 * 刷新统计数据
 */
export function refreshStatistics(params: {
  checkPlanId: number | string
  ruleId?: number | string
  periodStart?: string
  periodEnd?: string
}) {
  return http.post<void>('/rating/statistics/refresh', null, { params })
}

/**
 * 导出统计报表
 */
export function exportStatistics(params: {
  checkPlanId: number | string
  ruleId?: number | string
  levelIds?: (number | string)[]
  periodStart?: string
  periodEnd?: string
}) {
  return http.get<string>('/rating/statistics/export', { params })
}

// ==================== 常量 ====================

export const PERIOD_TYPES = {
  WEEK: 'WEEK',
  MONTH: 'MONTH',
  QUARTER: 'QUARTER',
  YEAR: 'YEAR'
} as const

export const PERIOD_TYPE_LABELS: Record<string, string> = {
  WEEK: '周',
  MONTH: '月',
  QUARTER: '季度',
  YEAR: '年'
}

export const TREND_GRANULARITIES = {
  DAY: 'DAY',
  WEEK: 'WEEK',
  MONTH: 'MONTH'
} as const

export const TREND_GRANULARITY_LABELS: Record<string, string> = {
  DAY: '按天',
  WEEK: '按周',
  MONTH: '按月'
}
