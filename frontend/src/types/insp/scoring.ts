/**
 * V7 检查平台 - 评分引擎类型定义
 */

// ==================== 枚举 ====================

export type RuleType =
  | 'VETO' | 'PENALTY' | 'PROGRESSIVE' | 'PROGRESSIVE_BONUS'
  | 'BONUS' | 'CUSTOM'

export const RuleTypeConfig: Record<RuleType, { label: string; description: string; color: string }> = {
  VETO: { label: '一票否决', description: '特定项不合格则直接归零', color: '#F56C6C' },
  PENALTY: { label: '额外扣分', description: '满足条件额外扣分', color: '#E6A23C' },
  PROGRESSIVE: { label: '累进扣分', description: '重复违规扣分递增', color: '#F0A020' },
  PROGRESSIVE_BONUS: { label: '累进加分', description: '持续达标奖励递增', color: '#95D475' },
  BONUS: { label: '奖励加分', description: '满足条件额外加分', color: '#67C23A' },
  CUSTOM: { label: '自定义', description: '自定义公式规则', color: '#909399' },
}

// ==================== 评分配置 ====================

export interface ScoringProfile {
  id: number
  tenantId: number
  sectionId: number
  maxScore: number
  minScore: number
  precisionDigits: number
  currentVersion: number
  // 1.9 趋势因子
  trendFactorEnabled: boolean
  trendLookbackDays: number
  trendBonusPerPercent: number | null
  trendPenaltyPerPercent: number | null
  trendMaxAdjustment: number | null
  // 1.10 分数衰减
  decayEnabled: boolean
  decayMode: string | null
  decayRatePerDay: number | null
  decayFloor: number | null
  // 1.11 多评审员聚合
  multiRaterMode: string | null
  raterWeightBy: string | null
  consensusThreshold: number | null
  // 1.12 分布校准
  calibrationEnabled: boolean
  calibrationMethod: string | null
  calibrationPeriodDays: number | null
  calibrationMinSamples: number | null

  createdBy: number | null
  createdAt: string
  updatedBy: number | null
  updatedAt: string | null
}

export interface CreateProfileRequest {
  sectionId: number
}

export interface UpdateProfileRequest {
  maxScore: number
  minScore: number
  precisionDigits: number
}

export interface UpdateAdvancedSettingsRequest {
  trendFactorEnabled?: boolean
  trendLookbackDays?: number
  trendBonusPerPercent?: number
  trendPenaltyPerPercent?: number
  trendMaxAdjustment?: number
  decayEnabled?: boolean
  decayMode?: string
  decayRatePerDay?: number
  decayFloor?: number
  multiRaterMode?: string
  raterWeightBy?: string
  consensusThreshold?: number
  calibrationEnabled?: boolean
  calibrationMethod?: string
  calibrationPeriodDays?: number
  calibrationMinSamples?: number
}

// ==================== 评分维度 ====================

export interface ScoreDimension {
  id: number
  tenantId: number
  scoringProfileId: number
  dimensionCode: string
  dimensionName: string
  weight: number
  baseScore: number
  passThreshold: number | null
  sourceType: 'SECTION' | 'MODULE' | 'ITEM'
  moduleTemplateId: number | null
  sortOrder: number
  createdAt: string
  updatedAt: string | null
}

export interface CreateDimensionRequest {
  dimensionCode: string
  dimensionName: string
  weight?: number
  baseScore?: number
  passThreshold?: number | null
  sortOrder?: number
}

export interface UpdateDimensionRequest {
  dimensionName: string
  weight: number
  baseScore: number
  passThreshold?: number | null
}

// ==================== 等级区间 ====================

export interface GradeBand {
  id: number
  tenantId: number
  scoringProfileId: number
  dimensionId: number | null
  gradeCode: string
  gradeName: string
  minScore: number
  maxScore: number
  color: string | null
  icon: string | null
  sortOrder: number
  createdAt: string
  updatedAt: string | null
}

export interface CreateGradeBandRequest {
  dimensionId?: number | null
  gradeCode: string
  gradeName: string
  minScore: number
  maxScore: number
  color?: string | null
  icon?: string | null
  sortOrder?: number
}

export interface UpdateGradeBandRequest {
  gradeName: string
  minScore: number
  maxScore: number
  color?: string | null
  icon?: string | null
}

// ==================== 计算规则 ====================

export type ScopeType = 'GLOBAL' | 'DIMENSION' | 'CROSS_DIMENSION'

export interface CalculationRule {
  id: number
  tenantId: number
  scoringProfileId: number
  ruleCode: string
  ruleName: string
  priority: number
  ruleType: RuleType
  config: string
  isEnabled: boolean
  scopeType: ScopeType
  targetDimensionIds: string | null
  activationCondition: string | null  // 1.4: 条件触发
  appliesTo: string | null            // 1.4: 适用范围
  effectiveFrom: string | null        // 1.5: 生效起始日
  effectiveUntil: string | null       // 1.5: 生效截止日
  exclusionGroup: string | null       // 1.6: 互斥组名
  createdAt: string
  updatedAt: string | null
}

export interface CreateRuleRequest {
  ruleCode: string
  ruleName: string
  priority?: number
  ruleType: RuleType
  config: string
  isEnabled?: boolean
  scopeType?: ScopeType
  targetDimensionIds?: string
  activationCondition?: string
  appliesTo?: string
  effectiveFrom?: string
  effectiveUntil?: string
  exclusionGroup?: string
}

export interface UpdateRuleRequest {
  ruleName: string
  priority: number
  ruleType: RuleType
  config: string
  isEnabled: boolean
  scopeType?: ScopeType
  targetDimensionIds?: string
  activationCondition?: string
  appliesTo?: string
  effectiveFrom?: string
  effectiveUntil?: string
  exclusionGroup?: string
}

// ==================== 版本快照 (1.7) ====================

export interface ScoringProfileVersion {
  id: number
  tenantId: number
  profileId: number
  version: number
  snapshot: string  // JSON
  publishedAt: string
  publishedBy: number | null
  changeSummary: string | null
  createdAt: string
}

export interface PublishVersionRequest {
  changeSummary?: string
}

