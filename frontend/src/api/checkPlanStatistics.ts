import { http } from '@/utils/request'

// 统计筛选条件
export interface StatisticsFilters {
  planId?: number
  startDate?: string
  endDate?: string
  classIds?: number[]
  gradeIds?: number[]
  categoryIds?: number[]
  itemIds?: number[]
  useWeightedScore?: boolean
  sortBy?: 'totalScore' | 'avgScore' | 'weightedScore'
  sortOrder?: 'asc' | 'desc'
  trendGranularity?: 'day' | 'week' | 'month'
  topN?: number
}

// 统计概览
export interface PlanStatisticsOverview {
  planId: number
  planName: string
  totalChecks: number
  totalClasses: number
  totalScore: number
  avgScore: number
  maxScore: number
  minScore: number
  totalItems: number
  totalPersons: number
  weightEnabled: boolean
  weightConfigName?: string
  lastCheckDate?: string
  startDate?: string
  endDate?: string
  status: number
}

// 类别扣分明细
export interface CategoryScoreItem {
  categoryId: number
  categoryName: string
  score: number
  percentage: number
}

// 班级排名
export interface ClassRanking {
  classId: number
  className: string
  gradeId?: number
  gradeName?: string
  teacherId?: number
  teacherName?: string
  studentCount?: number
  checkCount: number
  totalScore: number
  weightedScore?: number
  avgScorePerCheck: number
  ranking: number
  gradeRanking?: number
  scoreLevel: string
  vsAvg: number
  categoryScores: CategoryScoreItem[]
}

// 扣分项简要信息
export interface ItemBrief {
  itemId: number
  itemName: string
  triggerCount: number
  totalScore: number
}

// 类别统计
export interface CategoryStatistics {
  categoryId: number
  categoryName: string
  categoryCode?: string
  totalScore: number
  percentage: number
  itemCount: number
  classCount: number
  personCount: number
  topItems: ItemBrief[]
}

// 班级扣分项信息
export interface ClassItemInfo {
  classId: number
  className: string
  triggerCount: number
  totalScore: number
}

// 扣分项统计
export interface ItemStatistics {
  itemId: number
  itemName: string
  categoryId: number
  categoryName: string
  deductMode: number
  deductModeDesc: string
  triggerCount: number
  personCount: number
  totalScore: number
  avgScore: number
  classCount: number
  topClasses: ClassItemInfo[]
}

// 趋势数据点
export interface TrendPoint {
  date: string
  dateLabel: string
  checkCount: number
  totalScore: number
  weightedScore?: number
  avgScore: number
  classCount: number
  personCount: number
  itemCount: number
}

// 趋势汇总
export interface TrendSummary {
  totalChecks: number
  avgChecksPerDay: number
  avgScorePerDay: number
  trend: 'up' | 'down' | 'stable'
  trendPercentage: number
  maxDailyScore: number
  maxScoreDate?: string
  minDailyScore: number
  minScoreDate?: string
}

// 趋势数据
export interface TrendData {
  trendPoints: TrendPoint[]
  summary: TrendSummary
}

// 获取统计概览
export function getStatisticsOverview(planId: string | number, params?: Partial<StatisticsFilters>) {
  return http.get<PlanStatisticsOverview>(`/check-plans/${planId}/statistics/overview`, { params })
}

// 获取班级排名
export function getClassRanking(planId: string | number, params?: Partial<StatisticsFilters>) {
  return http.get<ClassRanking[]>(`/check-plans/${planId}/statistics/class-ranking`, { params })
}

// 获取类别统计
export function getCategoryStatistics(planId: string | number, params?: Partial<StatisticsFilters>) {
  return http.get<CategoryStatistics[]>(`/check-plans/${planId}/statistics/category`, { params })
}

// 获取扣分项统计
export function getItemStatistics(planId: string | number, params?: Partial<StatisticsFilters>) {
  return http.get<ItemStatistics[]>(`/check-plans/${planId}/statistics/items`, { params })
}

// 获取趋势数据
export function getTrendData(planId: string | number, params?: Partial<StatisticsFilters>) {
  return http.get<TrendData>(`/check-plans/${planId}/statistics/trend`, { params })
}

// 导出统计报表
export function exportStatistics(planId: string | number, filters: StatisticsFilters) {
  return http.post<string>(`/check-plans/${planId}/statistics/export`, filters)
}
