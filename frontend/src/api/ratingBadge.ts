import { http } from '@/utils/request'

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
