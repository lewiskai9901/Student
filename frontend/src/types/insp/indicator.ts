import type { LongId } from '@/types/common'

/**
 * 检查平台 - 评价指标类型定义
 *
 * Phase 4 评级引擎完美架构 (2026-05-23) 新增字段:
 *   sourceSectionIds[] / triggerMode / countThreshold / weightsBySection /
 *   rankDirection / missingPolicy(枚举化) / latePolicy / submissionDateField
 * 评级结果聚合根 IndicatorResult (DRAFT/PUBLISHED/SUPERSEDED 三态机) 类型也在此.
 */

export type TriggerMode = 'TIME_WINDOW' | 'COUNT' | 'MANUAL'
export type RankDirection = 'ASC' | 'DESC' | 'NONE'
export type MissingPolicyEnum = 'IGNORE' | 'ZERO' | 'MAX' | 'WAIT'
export type LatePolicy = 'REVISE_ORIGINAL' | 'CARRY_FORWARD' | 'EXCLUDE'
export type SubmissionDateField = 'TASK_DATE' | 'COMPLETED_AT'
export type ResultStatus = 'DRAFT' | 'PUBLISHED' | 'SUPERSEDED'

export interface Indicator {
  id: LongId
  tenantId: LongId
  projectId: LongId
  parentIndicatorId: LongId | null
  name: string
  indicatorType: 'LEAF' | 'COMPOSITE'
  // LEAF fields — 旧单值 (向后兼容, sourceSectionIds 优先)
  sourceSectionId: LongId | null
  /** Phase 4 多分区: 一个 LEAF 可跨多个分区. */
  sourceSectionIds?: LongId[]
  sourceAggregation: string | null
  // COMPOSITE fields
  compositeAggregation: string | null
  missingPolicy: string | null
  // Normalization
  normalization: string | null
  normalizationConfig: string | null
  // Common
  evaluationPeriod: string
  gradeSchemeId: LongId | null
  evaluationMethod: string | null
  gradeThresholds: string | null
  sortOrder: number
  // ── Phase 4 评级引擎新字段 ──
  triggerMode?: TriggerMode | null
  countThreshold?: number | null
  weightsBySection?: Record<string, number> | null
  rankDirection?: RankDirection | null
  latePolicy?: LatePolicy | null
  submissionDateField?: SubmissionDateField | null
  createdAt: string
  updatedAt: string | null
  // Client-side tree
  children?: Indicator[]
}

export interface IndicatorScore {
  id: LongId
  indicatorId: LongId
  targetId: LongId
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

/** Phase 4 评级结果聚合根 — 版本化快照 + 三态机. */
export interface IndicatorResult {
  id: LongId
  tenantId: LongId
  indicatorId: LongId
  targetId: LongId
  targetName?: string | null
  targetType?: string | null
  periodKey: string
  periodStart?: string | null
  periodEnd?: string | null
  /** 同 (indicator,target,period) 修订版本号 — 0=首版, 后续递增. */
  revision: number
  status: ResultStatus
  value: number | null
  rankPosition: number | null
  totalRanked: number | null
  gradeCode?: string | null
  gradeName?: string | null
  gradeColor?: string | null
  sourceCount?: number | null
  /** 该版本的算分上下文快照 (JSON). */
  snapshot?: string | null
  /** 替代前一版本时, 该字段记录前一 result id; 首版为 null. */
  supersedesId?: LongId | null
  computedAt: string
  publishedAt?: string | null
  createdAt?: string
  updatedAt?: string | null
}

export interface CreateLeafIndicatorRequest {
  projectId: LongId
  parentIndicatorId: LongId | null
  name: string
  /** 单值 (向后兼容). 优先用 sourceSectionIds. */
  sourceSectionId?: LongId | null
  /** Phase 4 多分区 — 至少 1 个. */
  sourceSectionIds?: LongId[]
  sourceAggregation: string
  evaluationPeriod: string
  // ── Phase 4 评级引擎新字段 ──
  triggerMode?: TriggerMode
  countThreshold?: number
  weightsBySection?: Record<string, number>
  rankDirection?: RankDirection
  missingPolicy?: MissingPolicyEnum
  latePolicy?: LatePolicy
  submissionDateField?: SubmissionDateField
  // 等级 / 归一
  gradeSchemeId?: LongId | null
  normalization?: string
  normalizationConfig?: string
  evaluationMethod?: string
  gradeThresholds?: string
  sortOrder?: number
}

export interface CreateCompositeIndicatorRequest {
  projectId: LongId
  parentIndicatorId: LongId | null
  name: string
  compositeAggregation: string
  missingPolicy: string
  evaluationPeriod: string
  sourceSectionId?: LongId
  gradeSchemeId?: LongId | null
  normalization?: string
  normalizationConfig?: string
  evaluationMethod?: string
  gradeThresholds?: string
  sortOrder?: number
}

export interface UpdateIndicatorRequest {
  name?: string
  evaluationPeriod?: string
  gradeSchemeId?: LongId | null
  sourceSectionId?: LongId | null
  /** Phase 4 多分区. */
  sourceSectionIds?: LongId[]
  sourceAggregation?: string
  compositeAggregation?: string
  missingPolicy?: string
  normalization?: string
  normalizationConfig?: string
  evaluationMethod?: string
  gradeThresholds?: string
  sortOrder?: number
  // ── Phase 4 评级引擎新字段 ──
  triggerMode?: TriggerMode
  countThreshold?: number
  weightsBySection?: Record<string, number>
  rankDirection?: RankDirection
  latePolicy?: LatePolicy
  submissionDateField?: SubmissionDateField
}

export interface ManualEvaluateRequest {
  indicatorId: LongId
  /** ISO date YYYY-MM-DD. */
  startDate: string
  endDate: string
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

// ==================== Phase 4 新枚举选项 ====================

export const TRIGGER_MODE_OPTIONS: { value: TriggerMode; label: string; description: string }[] = [
  { value: 'TIME_WINDOW', label: '时间窗口', description: '按 评估周期 自动滚动评估 (每天/每周/每月)' },
  { value: 'COUNT', label: '次数', description: '同目标累计达到 N 次提交后触发一次评估' },
  { value: 'MANUAL', label: '手动', description: '管理员在 评级结果 页主动触发' },
]

export const RANK_DIRECTION_OPTIONS: { value: RankDirection; label: string }[] = [
  { value: 'DESC', label: '越大越好 (高分在前)' },
  { value: 'ASC', label: '越小越好 (低分在前)' },
  { value: 'NONE', label: '不排名' },
]

export const MISSING_POLICY_ENUM_OPTIONS: { value: MissingPolicyEnum; label: string; description: string }[] = [
  { value: 'IGNORE', label: '忽略', description: '该目标本期无数据则跳过, 不生成结果' },
  { value: 'ZERO', label: '记零分', description: '无数据时按 0 分入榜' },
  { value: 'MAX', label: '记满分', description: '无数据时按上限计入 (适合"未违规即满分"场景)' },
  { value: 'WAIT', label: '等待', description: '本期不评, 等下次数据补齐再发布' },
]

export const LATE_POLICY_OPTIONS: { value: LatePolicy; label: string; description: string }[] = [
  { value: 'REVISE_ORIGINAL', label: '修订原版', description: '迟到提交触发当期重算, 旧版置 SUPERSEDED' },
  { value: 'CARRY_FORWARD', label: '顺延下期', description: '迟到数据并入下一评估期' },
  { value: 'EXCLUDE', label: '排除', description: '迟到提交永久不进入评级' },
]

export const SUBMISSION_DATE_FIELD_OPTIONS: { value: SubmissionDateField; label: string }[] = [
  { value: 'TASK_DATE', label: '按任务日期' },
  { value: 'COMPLETED_AT', label: '按完成时间' },
]

export const RESULT_STATUS_OPTIONS: { value: ResultStatus; label: string; tone: string }[] = [
  { value: 'DRAFT', label: '草稿', tone: 'warn' },
  { value: 'PUBLISHED', label: '已发布', tone: 'pass' },
  { value: 'SUPERSEDED', label: '已被替代', tone: 'pending' },
]
