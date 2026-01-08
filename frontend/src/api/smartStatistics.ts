import { http } from '@/utils/request'

// ==================== 类型定义 ====================

// 检查覆盖率
export interface CheckCoverageVO {
  planTargetClasses: number
  actualCheckedClasses: number
  coverageRate: number
  fullCoverageRecords: number
  partialCoverageRecords: number
  missedClasses: number
  missedClassList?: MissedClassVO[]
  classCheckCounts?: ClassCheckCountVO[]
}

export interface MissedClassVO {
  classId: number
  className: string
  gradeName: string
  reason?: string
}

export interface ClassCheckCountVO {
  classId: number
  className: string
  checkCount: number
  totalRounds?: number
}

// 趋势信息
export interface TrendInfo {
  direction: 'up' | 'down' | 'stable'
  percentage: number
  changeValue: number
  compareBase: string
}

// 智能统计概览
export interface SmartStatisticsOverviewVO {
  planId: number
  planName: string
  status: number
  startDate: string
  endDate: string
  totalChecks: number
  totalClasses: number
  totalRounds: number
  coverage: CheckCoverageVO
  totalScore: number
  avgScorePerCheck: number
  avgScorePerRound: number
  maxScore: number
  minScore: number
  totalItems: number
  totalPersons: number
  trend?: TrendInfo
  lastCheckDate?: string
  weightEnabled: boolean
  weightConfigName?: string
  weightedTotalScore?: number
  warnings: string[]
  insights: string[]
}

// 类别得分明细
export interface CategoryScoreDetailVO {
  categoryId: number
  categoryName: string
  categoryCode: string
  score: number
  percentage: number
  itemCount: number
}

// 轮次得分
export interface RoundScoreVO {
  round: number
  score: number
  vsPreRound: number
  improvement: 'improved' | 'worsened' | 'stable' | 'first'
}

// 智能班级排名
export interface SmartClassRankingVO {
  classId: number
  className: string
  gradeId: number
  gradeName: string
  departmentId?: number
  departmentName?: string
  teacherId?: number
  teacherName?: string
  studentCount?: number
  checkCount: number
  totalRounds: number
  participationRate?: number
  totalScore: number
  weightedScore?: number
  avgScorePerCheck: number
  avgScorePerRound: number
  normalizedScore?: number
  ranking: number
  gradeRanking?: number
  departmentRanking?: number
  scoreLevel: string
  vsAvg: number
  vsGradeAvg?: number
  trend?: 'up' | 'down' | 'stable'
  trendValue?: number
  roundImprovementRate?: number
  categoryScores: CategoryScoreDetailVO[]
  roundScores?: RoundScoreVO[]
}

// 可比较集合
export interface ComparableSetVO {
  commonClassCount: number
  commonCategoryCount: number
  commonMinRounds: number
  fullyComparable: boolean
  nonComparableReasons: string[]
}

// 年级平均分
export interface GradeAvgVO {
  gradeId: number
  gradeName: string
  avgScore: number
  classCount: number
}

// 智能排名结果
export interface SmartRankingResultVO {
  rankings: SmartClassRankingVO[]
  total: number
  pageNum: number
  pageSize: number
  coverage?: CheckCoverageVO
  comparableSet?: ComparableSetVO
  overallAvgScore: number
  gradeAvgScores: GradeAvgVO[]
  compareMode: string
  sortBy: string
  sortOrder: string
  warnings: string[]
  tips: string[]
}

// 识别到的类别
export interface DetectedCategoryVO {
  categoryId: number
  categoryCode: string
  categoryName: string
  categoryType: string
  checkCount: number
}

// 高频扣分项
export interface TopItemVO {
  itemId: number
  itemName: string
  triggerCount: number
  totalScore: number
  classCount: number
}

// 轮次分布
export interface RoundDistVO {
  round: number
  score: number
  count: number
}

// 类别详细统计
export interface CategoryStatsDetailVO {
  categoryId: number
  categoryCode: string
  categoryName: string
  categoryType: string
  totalScore: number
  percentage: number
  triggerCount: number
  affectedClasses: number
  personCount: number
  topItems: TopItemVO[]
  roundDistribution?: RoundDistVO[]
}

// 动态类别统计
export interface DynamicCategoryStatsVO {
  detectedCategories: DetectedCategoryVO[]
  categoryStats: CategoryStatsDetailVO[]
  totalScore: number
  topCategory?: string
  topCategoryPercentage?: number
}

// 轮次分布
export interface RoundDistributionVO {
  roundCount: number
  recordCount: number
  classCount: number
  percentage: number
}

// 轮次对比
export interface RoundComparisonVO {
  round: number
  classCount: number
  avgScore: number
  totalScore: number
  vsPreRound: number
  improvedCount: number
  worsenedCount: number
  stableCount: number
  improvementRate: number
}

// 轮次分析
export interface RoundAnalysisVO {
  maxRounds: number
  avgRounds: number
  roundDistribution: RoundDistributionVO[]
  roundComparison: RoundComparisonVO[]
  overallImprovementRate: number
  improvedClasses: number
  worsenedClasses: number
  stableClasses: number
  insights: string[]
}

// 班级追踪
export interface ClassTrackingVO {
  classId: number
  className: string
  gradeName: string
  teacherName?: string
  studentCount?: number
  totalChecks: number
  totalRounds: number
  totalScore: number
  avgScorePerCheck: number
  avgScorePerRound: number
  ranking?: number
  gradeRanking?: number
  scoreLevel?: string
  vsOverallAvg?: number
  vsGradeAvg?: number
  performance?: string
}

// 筛选参数
export interface SmartStatisticsFilters {
  planId: number
  recordIds?: number[]
  startDate?: string
  endDate?: string
  classIds?: number[]
  gradeIds?: number[]
  departmentIds?: number[]
  categoryIds?: number[]
  compareMode?: 'total' | 'average' | 'normalized' | 'weighted'
  includePartial?: boolean
  missingStrategy?: 'exclude' | 'zero' | 'average' | 'penalty'
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
  trendGranularity?: 'day' | 'week' | 'month'
  topN?: number
  pageNum?: number
  pageSize?: number
}

// ==================== API 方法 ====================

/**
 * 获取智能统计概览
 */
export function getSmartOverview(planId: number | string, params?: {
  startDate?: string
  endDate?: string
  recordIds?: number[]
}) {
  return http.get<SmartStatisticsOverviewVO>(`/check-plans/${planId}/smart-statistics/overview`, { params })
}

/**
 * 获取智能班级排名
 */
export function getSmartRanking(planId: number | string, params?: {
  startDate?: string
  endDate?: string
  compareMode?: string
  includePartial?: boolean
  classIds?: number[]
  gradeIds?: number[]
  departmentIds?: number[]
  sortBy?: string
  sortOrder?: string
  pageNum?: number
  pageSize?: number
}) {
  return http.get<SmartRankingResultVO>(`/check-plans/${planId}/smart-statistics/ranking`, { params })
}

/**
 * 获取动态类别统计
 */
export function getDynamicCategoryStats(planId: number | string, params?: {
  startDate?: string
  endDate?: string
  categoryIds?: number[]
}) {
  return http.get<DynamicCategoryStatsVO>(`/check-plans/${planId}/smart-statistics/categories`, { params })
}

/**
 * 获取轮次分析
 */
export function getRoundAnalysis(planId: number | string, params?: {
  startDate?: string
  endDate?: string
}) {
  return http.get<RoundAnalysisVO>(`/check-plans/${planId}/smart-statistics/rounds`, { params })
}

/**
 * 获取班级追踪数据
 */
export function getClassTracking(planId: number | string, classId: number | string) {
  return http.get<ClassTrackingVO>(`/check-plans/${planId}/smart-statistics/class-tracking/${classId}`)
}

/**
 * 获取检查覆盖率
 */
export function getCheckCoverage(planId: number | string, params?: {
  startDate?: string
  endDate?: string
}) {
  return http.get<CheckCoverageVO>(`/check-plans/${planId}/smart-statistics/coverage`, { params })
}

/**
 * 获取统计洞察
 */
export function getInsights(planId: number | string, params?: {
  startDate?: string
  endDate?: string
}) {
  return http.get<string[]>(`/check-plans/${planId}/smart-statistics/insights`, { params })
}
