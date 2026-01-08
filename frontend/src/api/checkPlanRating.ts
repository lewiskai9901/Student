import { http } from '@/utils/request'

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
