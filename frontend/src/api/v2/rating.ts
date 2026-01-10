/**
import { http } from '@/utils/request'
 * 评优管理综合 API - DDD架构适配
 *
 * 整合 checkPlanRating, ratingConfig, ratingResult, badge, frequency, notification, statistics
 */
/**
 * 检查计划评级相关接口
 */

// ==================== 类型定义 ====================

// 评级等级配置
export interface RatingLevelDTO {
  levelOrder: number           // 等级顺序（1最高）
  levelName: string            // 等级名称
  levelColor?: string          // 等级颜色
  levelIcon?: string           // 等级图标
  minScore?: number            // 扣分下限（分数段）
  maxScore?: number            // 扣分上限（分数段）
  rankCount?: number           // 名次数量
  percentage?: number          // 百分比
}

// 创建评级规则请求
export interface RatingRuleCreateDTO {
  checkPlanId: string | number // 检查计划ID
  ruleName: string             // 规则名称
  ruleType: 'DAILY' | 'SUMMARY' // 评级类型
  scoreSource: 'TOTAL' | 'CATEGORY' // 评分来源
  categoryId?: string | number // 类别ID（按类别评级时）
  useWeightedScore?: number    // 是否使用加权分数 0否 1是
  divisionMethod: 'SCORE_RANGE' | 'RANK_COUNT' | 'PERCENTAGE' // 划分方式
  summaryMethod?: 'AVERAGE' | 'SUM' // 汇总方式（SUMMARY类型）
  sortOrder?: number           // 排序序号
  enabled?: number             // 是否启用 0否 1是
  description?: string         // 规则描述
  levels: RatingLevelDTO[]     // 等级配置列表
}

// 更新评级规则请求
export interface RatingRuleUpdateDTO {
  ruleName: string
  ruleType: 'DAILY' | 'SUMMARY'
  scoreSource: 'TOTAL' | 'CATEGORY'
  categoryId?: string | number
  useWeightedScore?: number
  divisionMethod: 'SCORE_RANGE' | 'RANK_COUNT' | 'PERCENTAGE'
  summaryMethod?: 'AVERAGE' | 'SUM'
  sortOrder?: number
  enabled?: number
  description?: string
  levels: RatingLevelDTO[]
}

// 评级等级视图
export interface RatingLevelVO {
  id: string | number
  ruleId: string | number
  levelOrder: number
  levelName: string
  levelColor?: string
  levelIcon?: string
  minScore?: number
  maxScore?: number
  rankCount?: number
  percentage?: number
  createdAt: string
}

// 评级规则视图
export interface RatingRuleVO {
  id: string | number
  checkPlanId: string | number
  checkPlanName?: string
  ruleName: string
  ruleType: 'DAILY' | 'SUMMARY'
  ruleTypeText?: string
  scoreSource: 'TOTAL' | 'CATEGORY'
  scoreSourceText?: string
  categoryId?: string | number
  categoryName?: string
  useWeightedScore: number
  divisionMethod: 'SCORE_RANGE' | 'RANK_COUNT' | 'PERCENTAGE'
  divisionMethodText?: string
  summaryMethod?: 'AVERAGE' | 'SUM'
  summaryMethodText?: string
  sortOrder: number
  enabled: number
  description?: string
  createdBy?: string | number
  creatorName?: string
  createdAt: string
  updatedAt: string
  levels: RatingLevelVO[]
}

// 评级结果视图
export interface RatingResultVO {
  id: string | number
  ruleId: string | number
  ruleName?: string
  checkPlanId: string | number
  checkPlanName?: string
  checkRecordId?: string | number
  checkDate?: string
  classId: string | number
  className: string
  gradeId?: string | number
  gradeName?: string
  levelId: string | number
  levelName: string
  levelOrder: number
  levelColor?: string
  ranking: number
  totalClasses: number
  score: number
  periodStart?: string
  periodEnd?: string
  recordCount?: number
  // 审核相关字段
  approvalStatus?: number          // 审核状态: 0待审核 1已通过 2已驳回
  approvalStatusText?: string      // 审核状态文本
  approvedBy?: string | number     // 审核人ID
  approvedAt?: string              // 审核时间
  approvalRemark?: string          // 审核备注
  publishStatus?: number           // 发布状态: 0未发布 1已发布
  publishStatusText?: string       // 发布状态文本
  publishedAt?: string             // 发布时间
  version?: number                 // 版本号
  originalLevelId?: string | number // 原始等级ID(修改前)
  createdAt: string
}

// 评级审核请求
export interface RatingApprovalDTO {
  resultIds: (string | number)[]   // 评级结果ID列表
  action: 'APPROVE' | 'REJECT'     // 审核操作
  remark?: string                  // 审核备注
}

// 评级修改请求
export interface RatingModifyDTO {
  resultId: string | number        // 评级结果ID
  newLevelId: string | number      // 新等级ID
  reason: string                   // 修改原因
}

// 等级统计
export interface LevelStatistics {
  levelId: string | number
  levelName: string
  levelColor?: string
  levelOrder: number
  classCount: number
  percentage: number
  classes: {
    classId: string | number
    className: string
    gradeName?: string
    ranking: number
    score: number
  }[]
}

// 评级统计视图
export interface RatingStatisticsVO {
  ruleId: string | number
  ruleName: string
  totalClasses: number
  levelStatistics: LevelStatistics[]
}

// ==================== API接口 ====================

/**
 * 创建评级规则
 */
export function createRatingRule(data: RatingRuleCreateDTO) {
  return http.post<string | number>('/check-plan-rating/rules', data)
}

/**
 * 更新评级规则
 */
export function updateRatingRule(ruleId: string | number, data: RatingRuleUpdateDTO) {
  return http.put<void>(`/check-plan-rating/rules/${ruleId}`, data)
}

/**
 * 删除评级规则
 */
export function deleteRatingRule(ruleId: string | number) {
  return http.delete<void>(`/check-plan-rating/rules/${ruleId}`)
}

/**
 * 获取规则详情
 */
export function getRatingRuleDetail(ruleId: string | number) {
  return http.get<RatingRuleVO>(`/check-plan-rating/rules/${ruleId}`)
}

/**
 * 获取检查计划的所有评级规则
 */
export function getRatingRulesByPlanId(checkPlanId: string | number) {
  return http.get<RatingRuleVO[]>(`/check-plan-rating/plans/${checkPlanId}/rules`)
}

/**
 * 获取检查计划的所有评级规则（别名）
 */
export function getRatingRulesByPlan(checkPlanId: string | number) {
  return getRatingRulesByPlanId(checkPlanId)
}

/**
 * 获取评级规则的所有等级
 */
export function getRatingLevelsByRule(ruleId: string | number) {
  return http.get<RatingLevelVO[]>(`/check-plan-rating/rules/${ruleId}/levels`)
}

/**
 * 启用/禁用规则
 */
export function toggleRatingRuleEnabled(ruleId: string | number, enabled: boolean) {
  return http.put<void>(`/check-plan-rating/rules/${ruleId}/toggle`, null, {
    params: { enabled }
  })
}

/**
 * 计算单次检查评级
 */
export function calculateDailyRating(
  checkRecordId: string | number,
  ruleId?: string | number,
  forceRecalculate = false
) {
  return http.post<RatingResultVO[]>(
    `/check-plan-rating/calculate/daily/${checkRecordId}`,
    null,
    { params: { ruleId, forceRecalculate } }
  )
}

/**
 * 计算汇总评级
 */
export function calculateSummaryRating(
  checkPlanId: string | number,
  periodStart?: string,
  periodEnd?: string,
  ruleId?: string | number,
  forceRecalculate = false
) {
  return http.post<RatingResultVO[]>(
    `/check-plan-rating/calculate/summary/${checkPlanId}`,
    null,
    { params: { periodStart, periodEnd, ruleId, forceRecalculate } }
  )
}

/**
 * 重新计算规则的所有评级结果
 */
export function recalculateAllByRule(ruleId: string | number) {
  return http.post<void>(`/check-plan-rating/rules/${ruleId}/recalculate`)
}

/**
 * 获取单次检查的评级结果
 */
export function getRatingResultsByRecord(checkRecordId: string | number) {
  return http.get<RatingResultVO[]>(`/check-plan-rating/records/${checkRecordId}/results`)
}

/**
 * 获取检查计划的汇总评级结果
 */
export function getSummaryRatingResults(
  checkPlanId: string | number,
  periodStart?: string,
  periodEnd?: string
) {
  return http.get<RatingResultVO[]>(
    `/check-plan-rating/plans/${checkPlanId}/summary-results`,
    { params: { periodStart, periodEnd } }
  )
}

/**
 * 获取班级的评级历史
 */
export function getClassRatingHistory(classId: string | number, checkPlanId: string | number) {
  return http.get<RatingResultVO[]>(
    `/check-plan-rating/classes/${classId}/history`,
    { params: { checkPlanId } }
  )
}

/**
 * 获取评级统计信息
 */
export function getRatingStatistics(
  ruleId: string | number,
  checkRecordId?: string | number,
  periodStart?: string,
  periodEnd?: string
) {
  return http.get<RatingStatisticsVO>(
    `/check-plan-rating/rules/${ruleId}/statistics`,
    { params: { checkRecordId, periodStart, periodEnd } }
  )
}

// ==================== 常量 ====================

// 评级类型
export const RULE_TYPES = {
  DAILY: 'DAILY',
  SUMMARY: 'SUMMARY'
} as const

export const RULE_TYPE_LABELS: Record<string, string> = {
  [RULE_TYPES.DAILY]: '单次检查评级',
  [RULE_TYPES.SUMMARY]: '汇总评级'
}

// 评分来源
export const SCORE_SOURCES = {
  TOTAL: 'TOTAL',
  CATEGORY: 'CATEGORY'
} as const

export const SCORE_SOURCE_LABELS: Record<string, string> = {
  [SCORE_SOURCES.TOTAL]: '总分',
  [SCORE_SOURCES.CATEGORY]: '按类别'
}

// 划分方式
export const DIVISION_METHODS = {
  SCORE_RANGE: 'SCORE_RANGE',
  RANK_COUNT: 'RANK_COUNT',
  PERCENTAGE: 'PERCENTAGE'
} as const

export const DIVISION_METHOD_LABELS: Record<string, string> = {
  [DIVISION_METHODS.SCORE_RANGE]: '分数段',
  [DIVISION_METHODS.RANK_COUNT]: '名次数量',
  [DIVISION_METHODS.PERCENTAGE]: '百分比'
}

// 汇总方式
export const SUMMARY_METHODS = {
  AVERAGE: 'AVERAGE',
  SUM: 'SUM'
} as const

export const SUMMARY_METHOD_LABELS: Record<string, string> = {
  [SUMMARY_METHODS.AVERAGE]: '平均',
  [SUMMARY_METHODS.SUM]: '累加'
}

// 预设颜色
export const LEVEL_COLORS = [
  '#67C23A', // 绿色 - 优秀
  '#409EFF', // 蓝色 - 良好
  '#E6A23C', // 橙色 - 一般
  '#F56C6C', // 红色 - 较差
  '#909399'  // 灰色 - 其他
]

// 审核状态
export const APPROVAL_STATUS = {
  PENDING: 0,
  APPROVED: 1,
  REJECTED: 2
} as const

export const APPROVAL_STATUS_LABELS: Record<number, string> = {
  [APPROVAL_STATUS.PENDING]: '待审核',
  [APPROVAL_STATUS.APPROVED]: '已通过',
  [APPROVAL_STATUS.REJECTED]: '已驳回'
}

export const APPROVAL_STATUS_TYPES: Record<number, string> = {
  [APPROVAL_STATUS.PENDING]: 'warning',
  [APPROVAL_STATUS.APPROVED]: 'success',
  [APPROVAL_STATUS.REJECTED]: 'danger'
}

// 发布状态
export const PUBLISH_STATUS = {
  UNPUBLISHED: 0,
  PUBLISHED: 1
} as const

export const PUBLISH_STATUS_LABELS: Record<number, string> = {
  [PUBLISH_STATUS.UNPUBLISHED]: '未发布',
  [PUBLISH_STATUS.PUBLISHED]: '已发布'
}

// ==================== 审核管理API ====================

/**
 * 审核评级结果
 */
export function approveRatingResults(data: RatingApprovalDTO) {
  return http.post<void>('/check-plan-rating/results/approve', data)
}

/**
 * 修改评级结果
 */
export function modifyRatingResult(data: RatingModifyDTO) {
  return http.post<void>('/check-plan-rating/results/modify', data)
}

/**
 * 发布评级结果
 */
export function publishRatingResult(resultId: string | number) {
  return http.post<void>(`/check-plan-rating/results/${resultId}/publish`)
}

/**
 * 取消发布评级结果
 */
export function unpublishRatingResult(resultId: string | number) {
  return http.post<void>(`/check-plan-rating/results/${resultId}/unpublish`)
}

/**
 * 批量发布检查记录的所有评级结果
 */
export function publishAllRatingResults(checkRecordId: string | number) {
  return http.post<void>(`/check-plan-rating/records/${checkRecordId}/publish-all`)
}

/**
 * 获取待审核的评级结果
 */
export function getPendingRatingResults(
  checkPlanId?: string | number,
  checkRecordId?: string | number
) {
  return http.get<RatingResultVO[]>('/check-plan-rating/results/pending', {
    params: { checkPlanId, checkRecordId }
  })
}

// ==================== 导出API ====================

/**
 * 评级导出请求参数
 */
export interface RatingExportRequest {
  exportType?: string
  checkPlanId?: string | number
  ruleId?: string | number
  levelIds?: (string | number)[]
  classIds?: (string | number)[]
  gradeIds?: (string | number)[]
  periodType?: string
  periodStart?: string
  periodEnd?: string
  approvalStatus?: number
  publishStatus?: number
  format?: string
}

/**
 * 导出评级结果
 */
export function exportRatingResults(data: RatingExportRequest) {
  return http.post<Blob>('/check-plan-rating/export/results', data, {
    responseType: 'blob'
  })
}

/**
 * 导出指定等级的班级列表
 */
export function exportClassesByLevel(data: RatingExportRequest) {
  return http.post<Blob>('/check-plan-rating/export/classes-by-level', data, {
    responseType: 'blob'
  })
}

/**
 * 导出班级排名
 */
export function exportClassRanking(data: RatingExportRequest) {
  return http.post<Blob>('/check-plan-rating/export/class-ranking', data, {
    responseType: 'blob'
  })
}
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
/**
 * 荣誉徽章API - 对接后端RatingBadgeController
 */

// ==================== 类型定义 ====================

/**
 * 授予条件VO
 */
export interface GrantConditionVO {
  conditionType: 'FREQUENCY' | 'CONSECUTIVE' | 'MULTI_RULE'
  frequencyThreshold?: number
  consecutiveThreshold?: number
  periodType?: string
  levelId?: number | string
  multiRuleCondition?: MultiRuleCondition
}

/**
 * 多规则条件
 */
export interface MultiRuleCondition {
  operator: 'AND' | 'OR'
  conditions: RuleConditionItem[]
}

/**
 * 规则条件项
 */
export interface RuleConditionItem {
  ruleId: number | string
  levelId: number | string
  frequencyThreshold?: number
  consecutiveThreshold?: number
  conditionType: 'FREQUENCY' | 'CONSECUTIVE'
}

/**
 * 徽章创建DTO
 */
export interface RatingBadgeCreateDTO {
  badgeName: string
  badgeIcon?: string
  badgeLevel: 'GOLD' | 'SILVER' | 'BRONZE'
  ruleId: number | string
  grantCondition: GrantConditionVO
  description?: string
  enabled?: boolean
  autoGrant?: boolean
  createdBy?: number | string
}

/**
 * 徽章VO
 */
export interface RatingBadgeVO {
  id: number | string
  badgeName: string
  badgeIcon?: string
  badgeLevel: string
  ruleId: number | string
  ruleName?: string
  grantCondition: GrantConditionVO
  description?: string
  enabled: boolean
  autoGrant: boolean
  createdAt?: string
  updatedAt?: string
}

/**
 * 徽章授予请求
 */
export interface BadgeGrantRequest {
  badgeId: number | string
  classIds: (number | string)[]
  periodStart: string
  periodEnd: string
  grantReason?: string
  grantedBy?: number | string
}

/**
 * 徽章授予结果VO
 */
export interface BadgeGrantResultVO {
  badgeId: number | string
  badgeName: string
  badgeLevel: string
  classId: number | string
  className: string
  grantedAt: string
  success: boolean
  message: string
}

/**
 * 班级徽章记录VO
 */
export interface ClassBadgeRecordVO {
  id: number | string
  badgeId: number | string
  badgeName: string
  badgeIcon?: string
  badgeLevel: string
  classId: number | string
  className: string
  grantedAt: string
  grantedBy?: number | string
  periodStart?: string
  periodEnd?: string
  achievementData?: {
    frequency?: number
    rank?: number
    rate?: number
    consecutiveCount?: number
  }
  certificateUrl?: string
  certificateGenerated?: boolean
  revoked: boolean
  revokedReason?: string
}

// ==================== API接口 ====================

/**
 * 创建徽章
 */
export function createBadge(data: RatingBadgeCreateDTO) {
  return http.post<number>('/rating/badge', data)
}

/**
 * 更新徽章
 */
export function updateBadge(badgeId: number | string, data: RatingBadgeCreateDTO) {
  return http.put<void>(`/rating/badge/${badgeId}`, data)
}

/**
 * 删除徽章
 */
export function deleteBadge(badgeId: number | string) {
  return http.delete<void>(`/rating/badge/${badgeId}`)
}

/**
 * 获取徽章详情
 */
export function getBadgeDetail(badgeId: number | string) {
  return http.get<RatingBadgeVO>(`/rating/badge/${badgeId}`)
}

/**
 * 获取检查计划的徽章列表
 */
export function getBadgesByPlan(checkPlanId: number | string) {
  return http.get<RatingBadgeVO[]>(`/rating/badge/plan/${checkPlanId}`)
}

/**
 * 切换徽章启用状态
 */
export function toggleBadgeEnabled(badgeId: number | string, enabled: boolean) {
  return http.put<void>(`/rating/badge/${badgeId}/toggle`, null, {
    params: { enabled }
  })
}

/**
 * 检查并自动授予徽章
 */
export function checkAndGrantBadges(params: {
  checkPlanId: number | string
  periodStart: string
  periodEnd: string
}) {
  return http.post<BadgeGrantResultVO[]>('/rating/badge/check-and-grant', null, { params })
}

/**
 * 手动授予徽章
 */
export function grantBadge(data: BadgeGrantRequest) {
  return http.post<void>('/rating/badge/grant', data)
}

/**
 * 撤销徽章
 */
export function revokeBadge(recordId: number | string, reason: string) {
  return http.delete<void>(`/rating/badge/record/${recordId}/revoke`, {
    params: { reason }
  })
}

/**
 * 获取班级的徽章记录
 */
export function getClassBadgeRecords(classId: number | string) {
  return http.get<ClassBadgeRecordVO[]>(`/rating/badge/class/${classId}/records`)
}

/**
 * 获取符合条件的班级数量
 */
export function getQualifiedClassCount(params: {
  badgeId: number | string
  periodStart: string
  periodEnd: string
}) {
  const { badgeId, ...restParams } = params
  return http.get<number>(`/rating/badge/${badgeId}/qualified-count`, {
    params: restParams
  })
}

/**
 * 获取符合条件的班级列表
 */
export function getQualifiedClassIds(params: {
  badgeId: number | string
  periodStart: string
  periodEnd: string
}) {
  const { badgeId, ...restParams } = params
  return http.get<number[]>(`/rating/badge/${badgeId}/qualified-classes`, {
    params: restParams
  })
}

// ==================== 常量 ====================

export const BADGE_LEVELS = {
  GOLD: 'GOLD',
  SILVER: 'SILVER',
  BRONZE: 'BRONZE'
} as const

export const BADGE_LEVEL_LABELS: Record<string, string> = {
  GOLD: '金牌',
  SILVER: '银牌',
  BRONZE: '铜牌'
}

export const BADGE_LEVEL_COLORS: Record<string, string> = {
  GOLD: '#FFD700',
  SILVER: '#C0C0C0',
  BRONZE: '#CD7F32'
}

export const CONDITION_TYPES = {
  FREQUENCY: 'FREQUENCY',
  CONSECUTIVE: 'CONSECUTIVE',
  MULTI_RULE: 'MULTI_RULE'
} as const

export const CONDITION_TYPE_LABELS: Record<string, string> = {
  FREQUENCY: '频次条件',
  CONSECUTIVE: '连续条件',
  MULTI_RULE: '多规则组合'
}
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
/**
 * 评级通报API - 对接后端RatingNotificationController
 */

// ==================== 类型定义 ====================

/**
 * 荣誉通报请求
 */
export interface HonorNotificationRequest {
  checkPlanId: number | string
  notificationType: 'HONOR' | 'FULL' | 'ALERT'
  ruleId?: number | string
  levelIds?: (number | string)[]
  periodStart: string
  periodEnd: string
  minFrequency?: number
  title?: string
  createdBy?: number | string
}

/**
 * 通报生成结果VO
 */
export interface NotificationGenerateResultVO {
  notificationId: number | string
  fileName: string
  filePath: string
  classCount: number
  generatedAt: string
}

/**
 * 证书生成请求
 */
export interface CertificateGenerateRequest {
  badgeRecordId: number | string
  classIds: (number | string)[]
  periodStart: string
  periodEnd: string
  templateType?: string
}

/**
 * 证书生成结果VO
 */
export interface CertificateGenerateResultVO {
  classId: number | string
  className: string
  certificatePath: string
  success: boolean
  message: string
}

/**
 * 海报生成请求
 */
export interface PosterGenerateRequest {
  checkPlanId: number | string
  posterType: 'HONOR' | 'RANKING' | 'COMPARISON'
  ruleId?: number | string
  levelIds?: (number | string)[]
  periodStart: string
  periodEnd: string
  minFrequency?: number
  title?: string
  subtitle?: string
}

/**
 * 海报生成结果VO
 */
export interface PosterGenerateResultVO {
  posterPath: string
  classCount: number
  generatedAt: string
}

/**
 * 通报历史VO
 */
export interface NotificationHistoryVO {
  notificationId: number | string
  notificationType: string
  title: string
  filePath: string
  periodStart: string
  periodEnd: string
  classCount: number
  createdAt: string
}

// ==================== API接口 ====================

/**
 * 生成荣誉通报
 */
export function generateHonorNotification(data: HonorNotificationRequest) {
  return http.post<NotificationGenerateResultVO>('/rating/notification/honor', data)
}

/**
 * 批量生成证书
 */
export function batchGenerateCertificates(data: CertificateGenerateRequest) {
  return http.post<CertificateGenerateResultVO[]>('/rating/notification/certificate/batch', data)
}

/**
 * 生成海报
 */
export function generatePoster(data: PosterGenerateRequest) {
  return http.post<PosterGenerateResultVO>('/rating/notification/poster', data)
}

/**
 * 获取通报历史
 */
export function getNotificationHistory(params: {
  checkPlanId: number | string
  notificationType?: string
}) {
  return http.get<NotificationHistoryVO[]>('/rating/notification/history', { params })
}

/**
 * 发布通报
 */
export function publishNotification(notificationId: number | string) {
  return http.post<void>(`/rating/notification/publish/${notificationId}`)
}

/**
 * 删除通报
 */
export function deleteNotification(notificationId: number | string) {
  return http.delete<void>(`/rating/notification/${notificationId}`)
}

/**
 * 下载通报文件
 */
export function downloadNotification(filePath: string) {
  return http.get<Blob>(filePath, {
    responseType: 'blob'
  })
}

// ==================== 常量 ====================

export const NOTIFICATION_TYPES = {
  HONOR: 'HONOR',
  FULL: 'FULL',
  ALERT: 'ALERT'
} as const

export const NOTIFICATION_TYPE_LABELS: Record<string, string> = {
  HONOR: '荣誉榜通报',
  FULL: '完整评级通报',
  ALERT: '预警通报'
}

export const NOTIFICATION_TYPE_DESCRIPTIONS: Record<string, string> = {
  HONOR: '仅列出获奖班级，适用于表彰公告',
  FULL: '包含所有班级排名，适用于全面通报',
  ALERT: '仅列出需要改进的班级，适用于预警提醒'
}

export const POSTER_TYPES = {
  HONOR: 'HONOR',
  RANKING: 'RANKING',
  COMPARISON: 'COMPARISON'
} as const

export const POSTER_TYPE_LABELS: Record<string, string> = {
  HONOR: '荣誉榜海报',
  RANKING: '排名榜海报',
  COMPARISON: '对比榜海报'
}
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

// ==================== 常量 (已在上方定义) ====================

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
