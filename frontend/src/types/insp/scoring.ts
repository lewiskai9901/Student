import type { LongId } from '@/types/common'

/**
 * 检查平台 - 评分引擎类型定义
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

// 1.13 章节级归一化 (规模公平性)
export type NormalizeBy = 'NONE' | 'PER_MEMBER' | 'PER_PLACE' | 'PER_SUB_ORG'
export type NormalizationMode = 'NONE' | 'PER_CAPITA' | 'SQRT_ADJUSTED'

export const NormalizeByOptions: { value: NormalizeBy; label: string }[] = [
  { value: 'NONE', label: '不归一' },
  { value: 'PER_MEMBER', label: '按成员数' },
  { value: 'PER_PLACE', label: '按场所数' },
  { value: 'PER_SUB_ORG', label: '按子组织数' },
]

export const NormalizationModeOptions: { value: NormalizationMode; label: string }[] = [
  { value: 'NONE', label: '不归一' },
  { value: 'PER_CAPITA', label: '人均' },
  { value: 'SQRT_ADJUSTED', label: '开方折中' },
]

// ==================== 评分配置 ====================

export interface ScoringProfile {
  id: LongId
  tenantId: LongId
  /** 2026-05-23 评级引擎完美架构: profile 项目-owned, projectId NOT NULL */
  projectId: LongId
  /** 检查模板分区 ID — 每 (project, section) 对应一套 profile */
  sectionId: LongId
  maxScore: number
  minScore: number
  precisionDigits: number
  currentVersion: number
  // 1.13 章节级归一化 (规模公平性)
  normalizeBy: NormalizeBy
  normalizationMode: NormalizationMode
  baselinePopulation: number
  normFloor: number | null
  normCap: number | null

  createdBy: number | null
  createdAt: string
  updatedBy: number | null
  updatedAt: string | null
}

export interface CreateProfileRequest {
  sectionId: LongId
  /** 项目-owned: 创建时必传, 评分方案与项目同生命周期. */
  projectId: LongId
}

export interface UpdateProfileRequest {
  maxScore: number
  minScore: number
  precisionDigits: number
  // 1.13 章节级归一化 (规模公平性). 可空 — 后端缺省回落 NONE/1.
  normalizeBy?: NormalizeBy
  normalizationMode?: NormalizationMode
  baselinePopulation?: number
  normFloor?: number | null
  normCap?: number | null
}

// ==================== 评分维度 ====================

export interface ScoreDimension {
  id: LongId
  tenantId: LongId
  scoringProfileId: LongId
  dimensionCode: string
  dimensionName: string
  weight: number
  baseScore: number
  passThreshold: number | null
  sourceType: 'SECTION' | 'MODULE' | 'ITEM'
  moduleTemplateId: LongId | null
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
  id: LongId
  tenantId: LongId
  scoringProfileId: LongId
  dimensionId: LongId | null
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
  dimensionId?: LongId | null
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
  id: LongId
  tenantId: LongId
  scoringProfileId: LongId
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
  id: LongId
  tenantId: LongId
  profileId: LongId
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

