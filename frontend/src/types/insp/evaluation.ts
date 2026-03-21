// ==================== 评分方案 ====================
export interface ScoringPolicy {
  id: number
  tenantId: number
  policyCode: string
  policyName: string
  description: string | null
  precisionDigits: number
  isSystem: boolean
  isEnabled: boolean
  sortOrder: number
  createdAt: string
  updatedAt: string | null
}

export interface PolicyGradeBand {
  id: number
  policyId: number
  gradeCode: string
  gradeName: string
  minPercent: number
  maxPercent: number
  sortOrder: number
}

export interface PolicyCalcRule {
  id: number
  policyId: number
  ruleCode: string
  ruleName: string
  ruleType: 'VETO' | 'PENALTY' | 'BONUS' | 'PROGRESSIVE' | 'CUSTOM'
  priority: number
  config: string | null
  isEnabled: boolean
}

// ==================== 评选规则 ====================
export interface EvaluationRule {
  id: number
  tenantId: number
  projectId: number
  ruleName: string
  ruleDescription: string | null
  targetType: string
  evaluationPeriod: string
  awardName: string | null
  rankingEnabled: boolean
  sortOrder: number
  isEnabled: boolean
  levels?: EvaluationLevel[]
  createdAt: string
  updatedAt: string | null
}

export interface EvaluationLevel {
  id?: number
  ruleId?: number
  levelNum: number
  levelName: string
  levelIcon: string | null
  levelColor: string | null
  conditionLogic: 'AND' | 'OR'
  conditions: EvaluationCondition[]
}

export type ConditionType =
  | 'SCORE_AVG' | 'SCORE_MIN' | 'SCORE_MAX'
  | 'SCORE_EVERY' | 'SCORE_COUNT' | 'SCORE_SECTION_ZERO'
  | 'EVENT_COUNT' | 'EVENT_ROLE' | 'EVENT_SCORE_SUM'
  | 'PREV_EVAL' | 'RANK_PERCENTILE' | 'TREND_IMPROVE'

export const ConditionTypeConfig: Record<ConditionType, { label: string; group: string; description: string }> = {
  SCORE_AVG: { label: '平均分', group: '检查分数', description: '检查分数的平均值' },
  SCORE_MIN: { label: '最低分', group: '检查分数', description: '每次检查的最低分' },
  SCORE_MAX: { label: '最高分', group: '检查分数', description: '每次检查的最高分' },
  SCORE_EVERY: { label: '每次都达标', group: '检查分数', description: '要求每次检查都不低于阈值' },
  SCORE_COUNT: { label: '达标次数', group: '检查分数', description: '达到阈值的检查次数' },
  SCORE_SECTION_ZERO: { label: '零扣分', group: '检查分数', description: '指定分区零扣分' },
  EVENT_COUNT: { label: '事件计数', group: '事件记录', description: '某类事件的发生次数' },
  EVENT_ROLE: { label: '角色事件', group: '事件记录', description: '特定角色的事件计数' },
  EVENT_SCORE_SUM: { label: '事件扣分', group: '事件记录', description: '事件的扣分总和' },
  PREV_EVAL: { label: '历史评选', group: '历史评选', description: '要求历史评选达到某等级' },
  RANK_PERCENTILE: { label: '排名百分比', group: '历史评选', description: '排名在前百分之N' },
  TREND_IMPROVE: { label: '趋势改善', group: '历史评选', description: '比上期提升指定分数' },
}

export interface EvaluationCondition {
  conditionType: ConditionType
  operator: '>=' | '<=' | '=' | '>' | '<' | '!='
  threshold: number | string
  parameters: Record<string, any>
  description?: string
}

export interface EvaluationResult {
  id: number
  ruleId: number
  targetType: string
  targetId: number
  targetName: string | null
  cycleDate: string
  levelNum: number | null
  levelName: string | null
  score: number | null
  rankNo: number | null
  details: string | null
}
