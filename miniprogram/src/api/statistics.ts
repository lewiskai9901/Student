/**
 * 数据统计 API
 */
import { get } from '@/utils/request'

// 统计概览数据
export interface StatisticsOverview {
  // 检查统计
  totalChecks: number          // 总检查次数
  weekChecks: number           // 本周检查次数
  monthChecks: number          // 本月检查次数
  todayChecks: number          // 今日检查次数

  // 扣分统计
  totalDeductions: number      // 总扣分
  avgDeduction: number         // 平均扣分
  maxDeduction: number         // 最大扣分
  minDeduction: number         // 最小扣分

  // 申诉统计
  totalAppeals: number         // 总申诉数
  pendingAppeals: number       // 待处理申诉
  approvedAppeals: number      // 已通过申诉
  rejectedAppeals: number      // 已驳回申诉

  // 评级分布
  ratingDistribution: RatingDistribution[]
}

// 评级分布
export interface RatingDistribution {
  rating: string              // A/B/C/D/E
  ratingName: string          // 优秀/良好/中等/及格/不及格
  count: number
  percentage: number
}

// 班级排名
export interface ClassRanking {
  rank: number
  classId: number
  className: string
  departmentName?: string
  gradeName?: string
  totalScore: number
  avgScore: number
  deductionTotal: number
  checkCount: number
  rating: string
  ratingName?: string
  trend?: number              // 排名变化: 正数上升,负数下降
}

// 趋势数据
export interface TrendData {
  date: string
  value: number
  label?: string
}

// 问题分类统计
export interface CategoryStats {
  categoryId: number
  categoryName: string
  count: number
  totalDeduction: number
  percentage: number
}

// 查询参数
export interface StatisticsQueryParams {
  startDate?: string
  endDate?: string
  departmentId?: number
  gradeId?: number
  classId?: number
  period?: 'day' | 'week' | 'month' | 'year'
}

/**
 * 获取统计概览
 */
export function getStatisticsOverview(params?: StatisticsQueryParams) {
  return get<StatisticsOverview>('/quantification/statistics/overview', params)
}

/**
 * 获取班级排名
 */
export function getClassRankings(params?: {
  pageNum?: number
  pageSize?: number
  departmentId?: number
  gradeId?: number
  startDate?: string
  endDate?: string
}) {
  return get<{
    records: ClassRanking[]
    total: number
  }>('/quantification/statistics/class-rankings', {
    pageNum: params?.pageNum || 1,
    pageSize: params?.pageSize || 20,
    ...params
  })
}

/**
 * 获取扣分趋势
 */
export function getDeductionTrend(params?: StatisticsQueryParams) {
  return get<TrendData[]>('/quantification/statistics/deduction-trend', params)
}

/**
 * 获取检查次数趋势
 */
export function getCheckCountTrend(params?: StatisticsQueryParams) {
  return get<TrendData[]>('/quantification/statistics/check-count-trend', params)
}

/**
 * 获取问题分类统计
 */
export function getCategoryStatistics(params?: StatisticsQueryParams) {
  return get<CategoryStats[]>('/quantification/statistics/category-stats', params)
}

/**
 * 获取评级分布
 */
export function getRatingDistribution(params?: StatisticsQueryParams) {
  return get<RatingDistribution[]>('/quantification/statistics/rating-distribution', params)
}

/**
 * 获取班级统计详情
 */
export function getClassStatistics(classId: number, params?: StatisticsQueryParams) {
  return get<{
    classInfo: any
    totalChecks: number
    avgScore: number
    deductionTotal: number
    ranking: number
    ratingDistribution: RatingDistribution[]
    trend: TrendData[]
    topProblems: CategoryStats[]
  }>(`/quantification/statistics/class/${classId}`, params)
}

/**
 * 获取学生量化统计
 */
export function getStudentStatistics(studentId: number, params?: StatisticsQueryParams) {
  return get<{
    studentInfo: any
    totalDeductions: number
    avgDeduction: number
    checkCount: number
    problemCategories: CategoryStats[]
    trend: TrendData[]
  }>(`/quantification/statistics/student/${studentId}`, params)
}

/**
 * 导出统计报表
 */
export function exportStatisticsReport(params?: StatisticsQueryParams & { type: 'overview' | 'ranking' | 'detail' }) {
  return get<Blob>('/quantification/statistics/export', {
    ...params,
    responseType: 'blob'
  })
}

/**
 * 获取首页统计卡片数据
 */
export function getDashboardStats() {
  return get<{
    todayChecks: number
    pendingScoring: number
    pendingAppeals: number
    weekDeduction: number
    monthRanking: number
    recentRecords: any[]
  }>('/quantification/statistics/dashboard')
}
