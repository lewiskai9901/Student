/**
 * V7 检查平台 - 评价指标类型定义
 */

export interface Indicator {
  id: number
  tenantId: number
  projectId: number
  parentIndicatorId: number | null
  name: string
  indicatorType: 'LEAF' | 'COMPOSITE'
  // LEAF fields
  sourceSectionId: number | null
  sourceAggregation: string | null
  // COMPOSITE fields
  compositeAggregation: string | null
  missingPolicy: string | null
  // Normalization
  normalization: string | null
  normalizationConfig: string | null
  // Common
  evaluationPeriod: string
  gradeSchemeId: number | null
  evaluationMethod: string | null
  gradeThresholds: string | null
  sortOrder: number
  createdAt: string
  updatedAt: string | null
  // Client-side tree
  children?: Indicator[]
}

export interface IndicatorScore {
  id: number
  indicatorId: number
  targetId: number
  targetName: string | null
  targetType: string | null
  periodStart: string
  periodEnd: string
  score: number | null
  gradeCode: string | null
  gradeName: string | null
  gradeColor: string | null
  sourceCount: number
  detail: string | null
}

export interface CreateLeafIndicatorRequest {
  projectId: number
  parentIndicatorId: number | null
  name: string
  sourceSectionId: number | null
  sourceAggregation: string
  evaluationPeriod: string
  gradeSchemeId?: number | null
  normalization?: string
  normalizationConfig?: string
  evaluationMethod?: string
  gradeThresholds?: string
  sortOrder?: number
}

export interface CreateCompositeIndicatorRequest {
  projectId: number
  parentIndicatorId: number | null
  name: string
  compositeAggregation: string
  missingPolicy: string
  evaluationPeriod: string
  sourceSectionId?: number
  gradeSchemeId?: number | null
  normalization?: string
  normalizationConfig?: string
  evaluationMethod?: string
  gradeThresholds?: string
  sortOrder?: number
}

export interface UpdateIndicatorRequest {
  name?: string
  evaluationPeriod?: string
  gradeSchemeId?: number | null
  sourceSectionId?: number | null
  sourceAggregation?: string
  compositeAggregation?: string
  missingPolicy?: string
  normalization?: string
  normalizationConfig?: string
  evaluationMethod?: string
  gradeThresholds?: string
  sortOrder?: number
}

export const SOURCE_AGG_OPTIONS = [
  { value: 'AVG', label: '取平均' },
  { value: 'MAX', label: '取最高' },
  { value: 'MIN', label: '取最低' },
  { value: 'LATEST', label: '取最新' },
  { value: 'SUM', label: '求和' },
]

export const COMPOSITE_AGG_OPTIONS = [
  { value: 'WEIGHTED_AVG', label: '加权平均' },
  { value: 'SUM', label: '求和' },
  { value: 'AVG', label: '平均' },
  { value: 'MAX', label: '取最高' },
  { value: 'MIN', label: '取最低' },
]

export const MISSING_POLICY_OPTIONS = [
  { value: 'SKIP', label: '跳过（重新分配权重）' },
  { value: 'CARRY_FORWARD', label: '沿用上期' },
  { value: 'MARK_INCOMPLETE', label: '标记不完整' },
]

export const EVAL_PERIOD_OPTIONS = [
  { value: 'PER_TASK', label: '每次检查' },
  { value: 'DAILY', label: '每天' },
  { value: 'WEEKLY', label: '每周' },
  { value: 'MONTHLY', label: '每月' },
]

export const EVALUATION_METHOD_OPTIONS = [
  { value: 'SCORE_RANGE', label: '绝对分数', description: '按原始分数划分等级' },
  { value: 'PERCENT_RANGE', label: '得分率', description: '按得分百分比划分（适配不同满分）' },
  { value: 'RANK_COUNT', label: '排名前N', description: '按排名位次划分（如前3名）' },
  { value: 'RANK_PERCENT', label: '排名百分比', description: '按排名百分位划分（如前20%）' },
]

export const NORMALIZATION_OPTIONS = [
  { value: 'NONE', label: '不归一化', description: '使用原始分数直接比较', icon: 'equal' },
  { value: 'RELATION_COUNT', label: '按关联数量', description: '除以目标关联的实体数量（如成员人数）', icon: 'users' },
  { value: 'FIXED_VALUE', label: '固定除数', description: '除以一个固定的数值', icon: 'hash' },
  { value: 'PERCENTAGE', label: '转百分比', description: '转换为百分比（分数÷满分×100）', icon: 'percent' },
]
