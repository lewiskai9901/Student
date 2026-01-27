/**
 * 量化检查扩展 API - DDD架构适配
 *
 * 包含: 分析配置、统计、加权、评分、检查员、范围等
 */
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
  orgUnitIds?: number[]
  includeOrgUnitChildren?: boolean
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
  orgUnitId?: number
  orgUnitName?: string
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
// From analysisConfig.ts


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
// From checkPlanStatistics.ts

/**
 * 班级加权配置API
 * @author system
 * @version 3.0.0
 */


export interface ClassWeightConfig {
  id?: number
  semesterId: number
  configName: string
  configDescription?: string
  enableWeight: number // 0=关闭,1=开启
  weightMode: string // STANDARD/PER_CAPITA/SEGMENT/NONE
  minWeight?: number
  maxWeight?: number
  segmentRules?: string // JSON格式的分段规则
  effectiveDate: string
  expiryDate?: string
  applyToAllClasses: number
  specificClassIds?: string
  createdAt?: string
  updatedAt?: string
}

export interface ClassSizeStandard {
  id?: number
  semesterId: number
  gradeLevel: number
  standardName: string
  standardSize: number
  locked: number // 0=否,1=是
  lockedDate?: string
  description?: string
  createdAt?: string
  updatedAt?: string
}

export interface ClassWeightResult {
  classId: number
  originalScore: number
  weightedScore: number
  weightFactor: number
  actualSize: number
  standardSize: number
  weightMode: string
  enableWeight: boolean
  calculationDetails: string
}

export interface WeightConfigQuery {
  pageNum: number
  pageSize: number
  semesterId?: number
  enableWeight?: number
  weightMode?: string
}

export interface StandardSizeQuery {
  pageNum: number
  pageSize: number
  semesterId?: number
  gradeLevel?: number
  locked?: number
}

// 查询加权配置列表
export const listWeightConfigs = (params: WeightConfigQuery) => {
  return http.get<any>('/quantification/weight-config/configs', { params })
}

// 查询加权配置详情
export const getWeightConfig = (id: number) => {
  return http.get<ClassWeightConfig>(`/quantification/weight-config/configs/${id}`)
}

// 创建加权配置
export const createWeightConfig = (data: ClassWeightConfig) => {
  return http.post<ClassWeightConfig>('/quantification/weight-config/configs', data)
}

// 更新加权配置
export const updateWeightConfig = (id: number, data: Partial<ClassWeightConfig>) => {
  return http.put<ClassWeightConfig>(`/quantification/weight-config/configs/${id}`, data)
}

// 删除加权配置
export const deleteWeightConfig = (id: number) => {
  return http.delete(`/quantification/weight-config/configs/${id}`)
}

// 设置默认加权配置
export const setDefaultWeightConfig = (id: number) => {
  return http.put(`/quantification/weight-config/configs/${id}/default`)
}

// 计算加权分数(预览)
export const calculateWeightedScore = (classId: number, originalScore: number, checkDate: string) => {
  return http.post<ClassWeightResult>('/quantification/weight-config/calculate', {
    classId,
    originalScore,
    checkDate
  })
}

// 批量重新计算加权
export const recalculateWeights = (recordId: number) => {
  return http.post(`/quantification/weight-config/recalculate/${recordId}`)
}
// From classWeight.ts


/**
 * 新架构 - 日常检查打分和申诉API
 * 对接后端重构后的API
 * @since 1.0.6
 */

// ==================== 打分相关类型定义 ====================

/**
 * 扣分明细请求
 * 注意：ID字段使用 string | number 类型以避免JavaScript大整数精度丢失问题
 */
export interface ScoringDetailRequest {
  categoryId: string | number
  classId: string | number
  deductionItemId: string | number
  deductionItemName?: string
  deductMode?: number
  linkType: number  // 1=宿舍, 2=教室, 0=通用
  linkId: string | number
  deductScore: number
  personCount?: number
  description?: string
  remark?: string
  photoUrls?: string | null
  dormitoryId?: string | number
  dormitoryNo?: string
  classroomId?: string | number
  classroomNo?: string
  students?: any[]
}

/**
 * 日常检查打分请求
 */
export interface DailyScoringRequest {
  checkId: string | number
  checkerId: string | number
  checkerName: string
  details: ScoringDetailRequest[]
}

/**
 * 扣分明细响应
 */
export interface ScoringDetailResponse {
  id: number
  checkId: number
  categoryId: number
  categoryName?: string
  classId: number
  className?: string
  deductionItemId: number
  deductionItemName?: string
  linkType: number
  linkTypeName?: string
  linkId: number
  linkName?: string
  deductScore: number
  remark?: string
  photoUrls?: string
  appealStatus: number
  createdAt: string
}

/**
 * 日常检查打分响应
 */
export interface DailyScoringResponse {
  checkId: number
  checkName?: string
  checkDate?: string
  checkerId?: number
  checkerName?: string
  totalDeductScore: number
  detailCount: number
  appealStatus: number
  appealCount: number
  details?: ScoringDetailResponse[]
}

// ==================== 申诉相关类型定义 ====================

/**
 * 申诉创建请求
 */
export interface AppealCreateRequest {
  detailId: number
  appealReason: string
  appealUserId: number
  appealUserName: string
  appealPhotoUrls?: string
}

/**
 * 申诉审核请求
 */
export interface AppealReviewRequest {
  appealId: number
  status: number  // 2=通过, 3=驳回
  revisedScore?: number
  reviewOpinion?: string
  reviewerId: number
  reviewerName: string
}

/**
 * 申诉响应
 */
export interface AppealResponse {
  id: number
  checkId: number
  detailId: number
  categoryName?: string
  className?: string
  deductionItemName?: string
  originalScore: number
  appealReason: string
  appealUserId: number
  appealUserName: string
  appealTime: string
  appealPhotoUrls?: string
  status: number  // 0=待处理, 1=处理中, 2=通过, 3=驳回
  statusName?: string
  revisedScore?: number
  reviewOpinion?: string
  reviewerId?: number
  reviewerName?: string
  reviewTime?: string
}

// ==================== 打分API ====================

/**
 * 保存打分数据(增量更新)
 */
export function saveScoring(checkId: number, data: DailyScoringRequest) {
  return http.post<void>(`/quantification/daily-checks/${checkId}/scoring`, data)
}

/**
 * 获取检查的打分数据
 */
export function getScoringByCheckId(checkId: number) {
  return http.get<DailyScoringResponse>(`/quantification/daily-checks/${checkId}/scoring/init`)
}

/**
 * 获取检查某个班级的扣分明细
 */
export function getDetailsByCheckIdAndClassId(checkId: number, classId: number) {
  return http.get<ScoringDetailResponse[]>(`/quantification/daily-checks/${checkId}/classes/${classId}/details`)
}

/**
 * 获取检查某个类别某个班级的扣分明细
 */
export function getDetailsByCheckIdAndCategoryIdAndClassId(
  checkId: number,
  categoryId: number,
  classId: number
) {
  return http.get<ScoringDetailResponse[]>(
    `/quantification/daily-checks/${checkId}/categories/${categoryId}/classes/${classId}/details`
  )
}

/**
 * 删除扣分明细
 */
export function deleteDetail(detailId: number) {
  return http.delete<void>(`/quantification/daily-checks/details/${detailId}`)
}

/**
 * 批量删除扣分明细
 */
export function batchDeleteDetails(detailIds: number[]) {
  return http.delete<void>('/quantification/daily-checks/details', { data: detailIds })
}

// ==================== 申诉API ====================

/**
 * 创建申诉
 */
export function createAppeal(data: AppealCreateRequest) {
  return http.post<number>('/quantification/daily-check-appeals', data)
}

/**
 * 审核申诉
 */
export function reviewAppeal(appealId: number, data: AppealReviewRequest) {
  return http.put<void>(`/quantification/daily-check-appeals/${appealId}/review`, data)
}

/**
 * 根据检查ID查询所有申诉
 */
export function getAppealsByCheckId(checkId: number) {
  return http.get<AppealResponse[]>(`/quantification/daily-check-appeals/check/${checkId}`)
}

/**
 * 根据检查ID和状态查询申诉
 */
export function getAppealsByCheckIdAndStatus(checkId: number, status: number) {
  return http.get<AppealResponse[]>(`/quantification/daily-check-appeals/check/${checkId}/status/${status}`)
}

/**
 * 根据班级ID查询申诉
 */
export function getAppealsByClassId(classId: number) {
  return http.get<AppealResponse[]>(`/quantification/daily-check-appeals/class/${classId}`)
}

/**
 * 根据明细ID查询申诉
 */
export function getAppealByDetailId(detailId: number) {
  return http.get<AppealResponse>(`/quantification/daily-check-appeals/detail/${detailId}`)
}

/**
 * 撤销申诉
 */
export function withdrawAppeal(appealId: number) {
  return http.delete<void>(`/quantification/daily-check-appeals/${appealId}`)
}
// From dailyCheckScoring.ts


export interface PermissionConfig {
  categoryId: string
  categoryName: string
  classIds?: number[]
}

export interface InspectorCreateRequest {
  userId: number
  permissions: PermissionConfig[]
  remark?: string
}

export interface InspectorUpdateRequest {
  id: number
  status?: number
  permissions: PermissionConfig[]
  remark?: string
}

export interface PermissionDTO {
  id: number
  categoryId: string
  categoryName: string
  classIds?: number[]
  classNames?: string[]
}

export interface InspectorDTO {
  id: number
  planId: number
  userId: number
  userName: string
  username: string
  departmentName: string
  status: number
  remark: string
  permissions: PermissionDTO[]
  createdAt: string
}

export interface CheckTaskAssignment {
  id: number
  dailyCheckId: number
  planId: number
  userId: number
  categoryIds: string
  classIds: string
  status: number // 0待处理 1进行中 2已完成
  notified: number
  notifiedAt: string
  startedAt: string
  completedAt: string
  createdAt: string
  userName: string
  checkName: string
  checkDate: string
  planName: string
}

// 获取计划的打分人员列表
export function getInspectorsByPlanId(planId: string | number) {
  return http.get<InspectorDTO[]>(`/check-plans/${planId}/inspectors`)
}

// 获取打分人员详情
export function getInspectorById(planId: string | number, id: number) {
  return http.get<InspectorDTO>(`/check-plans/${planId}/inspectors/${id}`)
}

// 添加打分人员
export function addInspector(planId: string | number, data: InspectorCreateRequest) {
  return http.post<number>(`/check-plans/${planId}/inspectors`, data)
}

// 更新打分人员
export function updateInspector(planId: string | number, id: number, data: InspectorUpdateRequest) {
  return http.put<void>(`/check-plans/${planId}/inspectors/${id}`, data)
}

// 删除打分人员
export function deleteInspector(planId: string | number, id: number) {
  return http.delete<void>(`/check-plans/${planId}/inspectors/${id}`)
}

// 获取我的检查任务列表
export function getMyTasks(params: {
  status?: number
  planId?: number
  pageNum?: number
  pageSize?: number
}) {
  return http.get<{
    records: CheckTaskAssignment[]
    total: number
    pages: number
    current: number
    size: number
  }>('/my-check-tasks', { params })
}

// 获取任务详情
export function getTaskDetail(taskId: number) {
  return http.get<CheckTaskAssignment>(`/my-check-tasks/${taskId}`)
}

// 开始任务
export function startTask(taskId: number) {
  return http.put<void>(`/my-check-tasks/${taskId}/start`)
}

// 完成任务
export function completeTask(taskId: number) {
  return http.put<void>(`/my-check-tasks/${taskId}/complete`)
}

// 获取待处理任务数量
export function getPendingTaskCount() {
  return http.get<number>('/my-check-tasks/pending-count')
}
// From inspector.ts


/**
 * Casbin 数据范围 API
 * 对接后端 /api/scopes/* 接口
 */

// ==================== 类型定义 ====================

/**
 * 范围类型枚举
 */
export type ScopeTypeCode = 'ALL' | 'DEPT' | 'GRADE' | 'DEPT_GRADE' | 'CLASS' | 'SELF'

/**
 * 范围分配DTO
 */
export interface ScopeAssignmentDTO {
  id?: number
  userId: number
  username?: string
  realName?: string
  scopeType: ScopeTypeCode
  scopeExpression: string
  displayName?: string
  remark?: string
  expiresAt?: string
  assignedBy?: number
  assignedByName?: string
  assignedAt?: string
}

/**
 * 范围元数据DTO
 */
export interface ScopeMetadataDTO {
  id: number
  scopeExpression: string
  displayName: string
  scopeType: ScopeTypeCode
  scopeTypeName?: string
  refId?: number
  refType?: string
  parentScope?: string
  sortOrder?: number
}

/**
 * 权限检查DTO
 */
export interface PermissionCheckDTO {
  userId: number
  scopeExpression: string
  resource: string
  action: string
  allowed?: boolean
}

/**
 * 分配范围请求
 */
export interface AssignScopeRequest {
  userId: number
  scopeType: ScopeTypeCode
  scopeExpression: string
  displayName?: string
  remark?: string
}

/**
 * 批量分配请求
 */
export interface BatchAssignRequest {
  userId: number
  scopes: string[]
}

/**
 * 撤销范围请求
 */
export interface RevokeScopeRequest {
  userId: number
  scopeExpression: string
}

// ==================== 范围类型配置 ====================

/**
 * 范围类型配置
 */
export const SCOPE_TYPES: Record<ScopeTypeCode, { name: string; prefix: string; level: number }> = {
  ALL: { name: '全部数据', prefix: 'scope:*', level: 0 },
  DEPT: { name: '部门', prefix: 'scope:dept:', level: 1 },
  GRADE: { name: '年级', prefix: 'scope:grade:', level: 1 },
  DEPT_GRADE: { name: '部门+年级', prefix: 'scope:dept_grade:', level: 2 },
  CLASS: { name: '班级', prefix: 'scope:class:', level: 3 },
  SELF: { name: '仅本人', prefix: 'scope:self', level: 4 }
}

/**
 * 构建范围表达式
 */
export function buildScopeExpression(type: ScopeTypeCode, ...ids: (number | string)[]): string {
  switch (type) {
    case 'ALL':
      return 'scope:*'
    case 'SELF':
      return 'scope:self'
    case 'DEPT':
      return `scope:dept:${ids[0]}`
    case 'GRADE':
      return `scope:grade:${ids[0]}`
    case 'DEPT_GRADE':
      return `scope:dept_grade:${ids[0]}:${ids[1]}`
    case 'CLASS':
      return `scope:class:${ids[0]}`
    default:
      return ''
  }
}

/**
 * 解析范围表达式
 */
export function parseScopeExpression(expression: string): { type: ScopeTypeCode; ids: number[] } | null {
  if (expression === 'scope:*') {
    return { type: 'ALL', ids: [] }
  }
  if (expression === 'scope:self') {
    return { type: 'SELF', ids: [] }
  }
  if (expression.startsWith('scope:dept_grade:')) {
    const parts = expression.replace('scope:dept_grade:', '').split(':')
    return { type: 'DEPT_GRADE', ids: parts.map(Number) }
  }
  if (expression.startsWith('scope:dept:')) {
    return { type: 'DEPT', ids: [Number(expression.replace('scope:dept:', ''))] }
  }
  if (expression.startsWith('scope:grade:')) {
    return { type: 'GRADE', ids: [Number(expression.replace('scope:grade:', ''))] }
  }
  if (expression.startsWith('scope:class:')) {
    return { type: 'CLASS', ids: [Number(expression.replace('scope:class:', ''))] }
  }
  return null
}

/**
 * 获取范围类型显示名称
 */
export function getScopeTypeName(type: ScopeTypeCode): string {
  return SCOPE_TYPES[type]?.name || type
}

// ==================== API 方法 ====================

/**
 * 获取用户的数据范围列表
 */
export function getUserScopes(userId: number) {
  return http.get<ScopeAssignmentDTO[]>(`/scopes/user/${userId}`)
}

/**
 * 分配用户数据范围
 */
export function assignScope(data: AssignScopeRequest) {
  return http.post<ScopeAssignmentDTO>('/scopes/assign', data)
}

/**
 * 批量分配用户数据范围
 */
export function batchAssignScopes(data: BatchAssignRequest) {
  return http.post<ScopeAssignmentDTO[]>('/scopes/batch-assign', data)
}

/**
 * 撤销用户数据范围
 */
export function revokeScope(data: RevokeScopeRequest) {
  return http.delete('/scopes/revoke', { data })
}

/**
 * 撤销用户所有数据范围
 */
export function revokeAllScopes(userId: number) {
  return http.delete(`/scopes/user/${userId}/all`)
}

/**
 * 获取所有范围元数据
 */
export function getScopeMetadata() {
  return http.get<ScopeMetadataDTO[]>('/scopes/metadata')
}

/**
 * 按类型获取范围元数据
 */
export function getScopeMetadataByType(scopeType: ScopeTypeCode) {
  return http.get<ScopeMetadataDTO[]>('/scopes/metadata', {
    params: { scopeType }
  })
}

/**
 * 检查权限
 */
export function checkPermission(data: PermissionCheckDTO) {
  return http.post<PermissionCheckDTO>('/scopes/check', data)
}

/**
 * 获取用户可访问的班级ID列表
 */
export function getAccessibleClassIds(userId: number) {
  return http.get<number[]>(`/scopes/accessible/classes/${userId}`)
}
// From scope.ts


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
  orgUnitId?: number
  orgUnitName?: string
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
  orgUnitRanking?: number
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
  orgUnitIds?: number[]
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
  orgUnitIds?: number[]
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
// From smartStatistics.ts

