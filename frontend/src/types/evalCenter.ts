// ==================== 评级中心类型定义 ====================
// 独立于检查平台，通用多条件评选引擎

// ==================== 基础枚举 ====================

export type EvalCampaignStatus = 'DRAFT' | 'ACTIVE' | 'PAUSED' | 'ARCHIVED'
export type EvalTargetType = 'ORG' | 'PLACE' | 'USER'
export type EvalPeriod = 'WEEKLY' | 'MONTHLY' | 'QUARTERLY'
export type ConditionLogic = 'AND' | 'OR'
export type ConditionSourceType = 'INSPECTION' | 'EVENT' | 'HISTORY'
export type ConditionScope = 'SELF' | 'MEMBERS' | 'SPECIFIC_ROLE'
export type ConditionTimeRange = 'CYCLE' | 'CUSTOM'

// ==================== 指标定义 ====================

export type InspectionMetric =
  | 'SCORE_AVG'    // 平均分
  | 'SCORE_MIN'    // 最低分
  | 'SCORE_MAX'    // 最高分
  | 'SCORE_EVERY'  // 每次都达标
  | 'GRADE_EVERY'  // 每次评级达标
  | 'GRADE_COUNT'  // 达标次数
  | 'FAIL_COUNT'   // 不通过次数

export type EventMetric =
  | 'COUNT'        // 事件次数
  | 'SCORE_SUM'    // 事件分值总和

export type HistoryMetric =
  | 'PREV_LEVEL'      // 上期级别
  | 'CONSECUTIVE'     // 连续达标期数
  | 'RANK_PERCENTILE' // 排名百分位
  | 'TREND'           // 趋势（分差）

export type ConditionMetric = InspectionMetric | EventMetric | HistoryMetric

export interface MetricConfig {
  label: string
  description: string
  supportedOperators: string[]
  thresholdType: 'number' | 'grade_list' | 'level_num' | 'percent'
}

export const INSPECTION_METRICS: Record<InspectionMetric, MetricConfig> = {
  SCORE_AVG: {
    label: '平均分',
    description: '时间范围内检查分数的平均值',
    supportedOperators: ['>=', '<=', '='],
    thresholdType: 'number',
  },
  SCORE_MIN: {
    label: '最低分',
    description: '时间范围内每次检查的最低分',
    supportedOperators: ['>='],
    thresholdType: 'number',
  },
  SCORE_MAX: {
    label: '最高分',
    description: '时间范围内每次检查的最高分',
    supportedOperators: ['<='],
    thresholdType: 'number',
  },
  SCORE_EVERY: {
    label: '每次都达标',
    description: '要求每次检查分数都不低于阈值',
    supportedOperators: ['>='],
    thresholdType: 'number',
  },
  GRADE_EVERY: {
    label: '每次评级达标',
    description: '要求每次检查评级都在指定等级列表中',
    supportedOperators: ['IN'],
    thresholdType: 'grade_list',
  },
  GRADE_COUNT: {
    label: '达标次数',
    description: '评级达标的检查次数',
    supportedOperators: ['>='],
    thresholdType: 'number',
  },
  FAIL_COUNT: {
    label: '不通过次数',
    description: '检查不通过的次数',
    supportedOperators: ['=', '<='],
    thresholdType: 'number',
  },
}

export const EVENT_METRICS: Record<EventMetric, MetricConfig> = {
  COUNT: {
    label: '事件次数',
    description: '时间范围内该类事件的发生次数',
    supportedOperators: ['=', '>=', '<='],
    thresholdType: 'number',
  },
  SCORE_SUM: {
    label: '事件分值总和',
    description: '事件分值的累计总和',
    supportedOperators: ['>=', '<='],
    thresholdType: 'number',
  },
}

export const HISTORY_METRICS: Record<HistoryMetric, MetricConfig> = {
  PREV_LEVEL: {
    label: '上期级别',
    description: '上期评选达到的级别序号（1=最高）',
    supportedOperators: ['>=', '<=', '='],
    thresholdType: 'level_num',
  },
  CONSECUTIVE: {
    label: '连续达标期数',
    description: '连续达到指定级别的期数',
    supportedOperators: ['>='],
    thresholdType: 'number',
  },
  RANK_PERCENTILE: {
    label: '排名百分位',
    description: '排名在前百分之N（如 20 = 前20%）',
    supportedOperators: ['<='],
    thresholdType: 'percent',
  },
  TREND: {
    label: '趋势（分差）',
    description: '与上期相比的分数变化',
    supportedOperators: ['>='],
    thresholdType: 'number',
  },
}

// 所有指标的联合配置（按数据源分组）
export const SOURCE_METRICS: Record<ConditionSourceType, Record<string, MetricConfig>> = {
  INSPECTION: INSPECTION_METRICS as Record<string, MetricConfig>,
  EVENT: EVENT_METRICS as Record<string, MetricConfig>,
  HISTORY: HISTORY_METRICS as Record<string, MetricConfig>,
}

// ==================== 数据源配置 ====================

export interface InspectionSourceConfig {
  projectId?: number | null
  projectName?: string
  sectionId: number
  sectionName?: string
}

export interface EventSourceConfig {
  eventType: string
  eventTypeName?: string
}

export interface HistorySourceConfig {
  campaignId?: number | null
  campaignName?: string
}

export type SourceConfig = InspectionSourceConfig | EventSourceConfig | HistorySourceConfig

// ==================== 核心领域类型 ====================

export interface EvalCondition {
  id?: number
  levelId?: number
  sourceType: ConditionSourceType
  sourceConfig: string | SourceConfig  // JSON string or parsed object
  metric: ConditionMetric
  operator: string
  threshold: string  // JSON string or number string
  scope: ConditionScope
  scopeRole?: string | null
  timeRange: ConditionTimeRange
  timeRangeDays?: number | null
  description?: string
  sortOrder?: number
}

export interface EvalLevel {
  id?: number
  campaignId?: number
  levelNum: number
  levelName: string
  conditionLogic: ConditionLogic
  sortOrder?: number
  conditions: EvalCondition[]
}

export interface EvalCampaign {
  id?: number
  tenantId?: number
  campaignName: string
  campaignDescription?: string | null
  targetType: EvalTargetType
  scopeOrgIds?: string | null  // JSON array of org IDs
  evaluationPeriod: EvalPeriod
  status: EvalCampaignStatus
  isAutoExecute?: boolean
  lastExecutedAt?: string | null
  nextExecuteAt?: string | null
  sortOrder?: number
  createdBy?: number
  createdAt?: string
  updatedBy?: number
  updatedAt?: string
  // Eager-loaded
  levels?: EvalLevel[]
}

export interface EvalBatch {
  id: number
  tenantId?: number
  campaignId: number
  cycleStart: string
  cycleEnd: string
  totalTargets: number
  executedAt: string
  executedBy?: number
  status: string
  summary?: string | null  // JSON: {level1Count, level2Count, ...}
  createdAt?: string
  // Parsed
  summaryParsed?: Record<string, number>
  campaignName?: string
}

export interface ConditionDetail {
  conditionId: number
  passed: boolean
  actual: string
  threshold: string
  description: string
}

export interface EvalResult {
  id: number
  batchId: number
  campaignId: number
  targetType: EvalTargetType
  targetId: number
  targetName?: string | null
  levelNum?: number | null
  levelName?: string | null
  rankNo?: number | null
  score?: number | null
  conditionDetails?: string | null  // JSON array
  upgradeHint?: string | null
  createdAt?: string
  // Parsed
  conditionDetailsParsed?: ConditionDetail[]
}

// ==================== 辅助选项类型 ====================

export interface InspProject {
  id: number
  name: string
}

export interface InspSection {
  id: number
  sectionName: string
  projectId?: number
}

export interface GradeBand {
  gradeCode: string
  gradeName: string
  sortOrder?: number
}

export interface EventType {
  typeCode: string
  typeName: string
}

// ==================== 配置常量 ====================

export const EvalCampaignStatusConfig: Record<EvalCampaignStatus, { label: string; color: string }> = {
  DRAFT: { label: '草稿', color: '#94a3b8' },
  ACTIVE: { label: '运行中', color: '#10b981' },
  PAUSED: { label: '已暂停', color: '#f59e0b' },
  ARCHIVED: { label: '已归档', color: '#d1d5db' },
}

export const EvalTargetTypeConfig: Record<EvalTargetType, { label: string; icon: string }> = {
  ORG: { label: '组织单元', icon: '🏢' },
  PLACE: { label: '场所', icon: '🏠' },
  USER: { label: '用户', icon: '👤' },
}

export const EvalPeriodConfig: Record<EvalPeriod, { label: string }> = {
  WEEKLY: { label: '每周' },
  MONTHLY: { label: '每月' },
  QUARTERLY: { label: '每季' },
}

export const ConditionSourceConfig: Record<ConditionSourceType, { label: string; description: string }> = {
  INSPECTION: { label: '检查分数', description: '基于量化检查的分数和评级数据' },
  EVENT: { label: '事件记录', description: '基于实体事件系统的记录数据' },
  HISTORY: { label: '评选历史', description: '基于本活动或其他评选活动的历史结果' },
}

export const ConditionScopeConfig: Record<ConditionScope, { label: string }> = {
  SELF: { label: '目标自身' },
  MEMBERS: { label: '关联成员' },
  SPECIFIC_ROLE: { label: '指定角色' },
}

export const OperatorLabels: Record<string, string> = {
  '>=': '≥（不低于）',
  '<=': '≤（不超过）',
  '=': '＝（等于）',
  '!=': '≠（不等于）',
  'IN': '包含于',
}
