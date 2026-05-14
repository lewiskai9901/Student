/**
 * 检查平台 - 分析报表类型
 */
import type { LongId } from '@/types/common'
import type { PeriodType, TrendDirection } from './enums'

export interface DailySummary {
  id: LongId
  tenantId: LongId
  projectId: LongId
  summaryDate: string
  targetType: string | null
  targetId: LongId | null
  targetName: string | null
  orgUnitId: LongId | null
  orgUnitName: string | null
  inspectionCount: number
  avgScore: number | null
  minScore: number | null
  maxScore: number | null
  totalDeductions: number | null
  totalBonuses: number | null
  passCount: number
  failCount: number
  ranking: number | null
  dimensionScores: string | null
  grade: string | null
  createdAt: string
  updatedAt: string | null
}

export interface PeriodSummary {
  id: LongId
  tenantId: LongId
  projectId: LongId
  periodType: PeriodType
  periodStart: string
  periodEnd: string
  targetType: string | null
  targetId: LongId | null
  targetName: string | null
  orgUnitId: LongId | null
  orgUnitName: string | null
  inspectionDays: number
  avgScore: number | null
  minScore: number | null
  maxScore: number | null
  scoreStdDev: number | null
  trendDirection: TrendDirection | null
  trendPercent: number | null
  ranking: number | null
  dimensionScores: string | null
  grade: string | null
  correctiveCount: number
  correctiveClosedCount: number
  prevPeriodScore: number | null
  momChange: number | null
  yoyScore: number | null
  yoyChange: number | null
  createdAt: string
  updatedAt: string | null
}

export interface CorrectiveSummary {
  total: number
  open: number
  assigned: number
  inProgress: number
  submitted: number
  verified: number
  rejected: number
  closed: number
  escalated: number
  overdue: number
  critical: number
  high: number
}

export interface RebuildDailyRequest {
  projectId: LongId
  date: string
}

export interface RebuildPeriodRequest {
  projectId: LongId
  periodType: PeriodType
  periodStart: string
}

// ==================== Inspector Performance 读模型 ====================

export interface InspectorSummary {
  id: LongId
  tenantId?: LongId
  projectId: LongId
  inspectorId: LongId
  inspectorName?: string
  periodType: string
  periodStart: string
  periodEnd?: string
  totalTasks: number
  completedTasks: number
  cancelledTasks: number
  expiredTasks: number
  avgCompletionTimeMinutes?: number
  avgScore?: number
  totalSubmissions: number
  flaggedSubmissions: number
  complianceRate?: number
  createdAt?: string
  updatedAt?: string
}

// ==================== 检查项频率统计 (Pareto 分析) ====================

export interface ItemFrequencySummary {
  id: LongId
  tenantId?: LongId
  projectId: LongId
  periodType: string
  periodStart: string
  periodEnd?: string
  itemCode?: string
  itemName?: string
  sectionId?: LongId
  sectionName?: string
  occurrenceCount: number
  flaggedCount: number
  totalDeduction: number
  avgDeduction: number
  cumulativePercentage?: number
  createdAt?: string
  updatedAt?: string
}

// ==================== 整改周期汇总 (读模型) ====================

export interface CorrectivePeriodSummary {
  id: LongId
  tenantId?: LongId
  projectId: LongId
  periodType: string
  periodStart: string
  periodEnd?: string
  totalCases: number
  openCases: number
  inProgressCases: number
  closedCases: number
  overdueCases: number
  escalatedCases: number
  avgResolutionDays?: number
  onTimeRate?: number
  effectivenessConfirmed: number
  effectivenessFailed: number
  createdAt?: string
  updatedAt?: string
}
