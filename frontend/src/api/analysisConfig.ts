import { http } from '@/utils/request'

// ==================== 类型定义 ====================

/**
 * 分析配置
 */
export interface AnalysisConfig {
  id?: number | string
  planId: number | string
  planName?: string
  configName: string
  configDesc?: string

  // 范围配置
  scopeType: 'time' | 'record' | 'mixed'
  scopeConfig?: ScopeConfig

  // 目标配置
  targetType: 'all' | 'department' | 'grade' | 'custom'
  targetConfig?: TargetConfig

  // 更新模式
  updateMode: 'static' | 'dynamic'
  autoRefresh?: boolean
  refreshInterval?: number
  lastRefreshTime?: string

  // 缺检策略
  missingStrategy: 'avg' | 'weighted' | 'full_only' | 'penalty' | 'exempt'
  missingStrategyConfig?: Record<string, any>

  // 类别映射
  categoryMappings?: CategoryMapping[]

  // 布局配置
  layoutConfig?: Record<string, any>

  // 状态
  isEnabled?: boolean
  isPublic?: boolean
  isDefault?: boolean
  sortOrder?: number

  // 指标配置
  metrics?: AnalysisMetric[]

  // 元数据
  creatorId?: number
  creatorName?: string
  createdAt?: string
  updatedAt?: string
  metricCount?: number
  snapshotCount?: number
}

/**
 * 范围配置
 */
export interface ScopeConfig {
  mode?: 'fixed' | 'dynamic'
  startDate?: string
  endDate?: string
  includeStartDay?: boolean
  includeEndDay?: boolean
  selectedRecordIds?: number[]
  excludeRecordIds?: number[]
  conditions?: FilterConditions
}

/**
 * 筛选条件
 */
export interface FilterConditions {
  checkTypes?: string[]
  templateIds?: number[]
  statusList?: number[]
}

/**
 * 目标配置
 */
export interface TargetConfig {
  departmentIds?: number[]
  includeDepartmentChildren?: boolean
  gradeIds?: number[]
  classIds?: number[]
  excludeClassIds?: number[]
  dynamicConditions?: DynamicConditions
}

/**
 * 动态条件
 */
export interface DynamicConditions {
  minStudentCount?: number
  maxStudentCount?: number
  onlyActiveClasses?: boolean
}

/**
 * 类别映射
 */
export interface CategoryMapping {
  id?: number
  templateCategoryId: number
  templateCategoryName: string
  unifiedCategoryId: number
  unifiedCategoryName: string
}

/**
 * 分析指标
 */
export interface AnalysisMetric {
  id?: number
  configId?: number
  metricCode: string
  metricName: string
  metricDesc?: string
  metricType: MetricType
  sourceType?: 'all' | 'category' | 'item'
  sourceCategoryIds?: number[]
  sourceItemIds?: number[]
  aggregation?: 'sum' | 'avg' | 'max' | 'min' | 'count'
  groupBy?: 'class' | 'grade' | 'department' | 'category' | 'date'
  customFormula?: string
  displayFormat?: string
  decimalPlaces?: number
  unit?: string
  chartType: ChartType
  chartConfig?: Record<string, any>
  sortField?: string
  sortOrder?: 'asc' | 'desc'
  topN?: number
  highlightRules?: HighlightRule[]
  isEnabled?: boolean
  displayOrder?: number
  gridPosition?: GridPosition
  createdAt?: string
  updatedAt?: string
}

/**
 * 指标类型
 */
export type MetricType =
  | 'total_score'
  | 'avg_score'
  | 'check_count'
  | 'coverage_rate'
  | 'item_count'
  | 'ranking'
  | 'trend'
  | 'distribution'
  | 'overview'
  | 'class_ranking'
  | 'category_distribution'
  | 'grade_comparison'
  | 'department_comparison'
  | 'custom'

/**
 * 图表类型
 */
export type ChartType = 'number' | 'bar' | 'line' | 'pie' | 'table' | 'rank'

/**
 * 高亮规则
 */
export interface HighlightRule {
  condition: 'gt' | 'gte' | 'lt' | 'lte' | 'eq' | 'between'
  threshold?: number
  minValue?: number
  maxValue?: number
  color?: string
  backgroundColor?: string
  label?: string
}

/**
 * 网格位置
 */
export interface GridPosition {
  x: number
  y: number
  w: number
  h: number
}

/**
 * 分析结果
 */
export interface AnalysisResult {
  configId: number
  configName: string
  dateRangeStart?: string
  dateRangeEnd?: string
  generatedAt: string
  isDynamic: boolean
  overview: AnalysisOverview
  metricResults: MetricResult[]
}

/**
 * 概览数据
 */
export interface AnalysisOverview {
  recordCount: number
  classCount: number
  totalScore: number
  avgScore: number
  maxScore: number
  minScore: number
  totalItems: number
  totalPersons?: number
  avgCoverageRate?: number
  fullCoverageCount?: number
}

/**
 * 指标结果
 */
export interface MetricResult {
  metricId: number
  metricCode: string
  metricName: string
  metricType: string
  chartType: string
  data: any
  chartConfig?: Record<string, any>
  gridPosition?: GridPosition
}

/**
 * 班级排名项
 */
export interface ClassRankingItem {
  classId: number
  className: string
  gradeId?: number
  gradeName?: string
  departmentId?: number
  departmentName?: string
  teacherName?: string
  checkCount: number
  expectedCheckCount: number
  coverageRate: number
  totalScore: number
  avgScore: number
  weightedScore: number
  ranking: number
  gradeRanking?: number
  scoreLevel: string
  vsAvg: number
  categoryScores?: CategoryScoreItem[]
}

/**
 * 类别扣分项
 */
export interface CategoryScoreItem {
  categoryId: number
  categoryName: string
  score: number
  percentage: number
}

/**
 * 趋势数据点
 */
export interface TrendPoint {
  date?: string
  dateLabel: string
  checkCount: number
  totalScore: number
  avgScore: number
  classCount: number
  personCount?: number
}

/**
 * 分布数据项
 */
export interface DistributionItem {
  name: string
  value: number
  percentage: number
  count?: number
}

/**
 * 快照信息
 */
export interface AnalysisSnapshot {
  id: number
  configId: number
  generatedAt: string
  dateRangeStart?: string
  dateRangeEnd?: string
  recordCount: number
  classCount: number
  createdAt: string
}

// ==================== API 接口 ====================

/**
 * 分页查询配置
 */
export function getConfigPage(params: {
  planId?: number
  configName?: string
  isEnabled?: boolean
  pageNum?: number
  pageSize?: number
}) {
  return http.get<{
    records: AnalysisConfig[]
    total: number
    pages: number
    current: number
    size: number
  }>('/analysis/configs', { params })
}

/**
 * 获取计划下的配置列表
 */
export function getConfigsByPlanId(planId: number | string) {
  return http.get<AnalysisConfig[]>(`/analysis/configs/plan/${planId}`)
}

/**
 * 获取配置详情
 */
export function getConfigDetail(id: number | string) {
  return http.get<AnalysisConfig>(`/analysis/configs/${id}`)
}

/**
 * 创建配置
 */
export function createConfig(data: AnalysisConfig) {
  return http.post<AnalysisConfig>('/analysis/configs', data)
}

/**
 * 更新配置
 */
export function updateConfig(id: number | string, data: Partial<AnalysisConfig>) {
  return http.put<AnalysisConfig>(`/analysis/configs/${id}`, data)
}

/**
 * 删除配置
 */
export function deleteConfig(id: number | string) {
  return http.delete<void>(`/analysis/configs/${id}`)
}

/**
 * 启用/禁用配置
 */
export function toggleConfigEnabled(id: number | string, enabled: boolean) {
  return http.put<void>(`/analysis/configs/${id}/toggle`, undefined, { params: { enabled } })
}

/**
 * 设为默认配置
 */
export function setDefaultConfig(id: number | string) {
  return http.put<void>(`/analysis/configs/${id}/default`)
}

/**
 * 复制配置
 */
export function copyConfig(id: number | string, newName?: string) {
  return http.post<AnalysisConfig>(`/analysis/configs/${id}/copy`, undefined, {
    params: newName ? { newName } : undefined
  })
}

/**
 * 执行分析
 */
export function executeAnalysis(id: number | string, saveSnapshot = false) {
  return http.post<AnalysisResult>(`/analysis/configs/${id}/execute`, undefined, {
    params: { saveSnapshot }
  })
}

/**
 * 获取最新快照
 */
export function getLatestSnapshot(configId: number | string) {
  return http.get<AnalysisResult>(`/analysis/configs/${configId}/latest-snapshot`)
}

/**
 * 获取快照列表
 */
export function getSnapshots(configId: number | string) {
  return http.get<AnalysisSnapshot[]>(`/analysis/configs/${configId}/snapshots`)
}

/**
 * 获取快照详情
 */
export function getSnapshotDetail(snapshotId: number | string) {
  return http.get<AnalysisResult>(`/analysis/configs/snapshots/${snapshotId}`)
}
