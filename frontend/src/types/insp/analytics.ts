/**
 * V7 检查平台 - 分析报表类型
 */
import type { PeriodType, TrendDirection } from './enums'

export interface DailySummary {
  id: number
  tenantId: number
  projectId: number
  summaryDate: string
  targetType: string | null
  targetId: number | null
  targetName: string | null
  orgUnitId: number | null
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
  id: number
  tenantId: number
  projectId: number
  periodType: PeriodType
  periodStart: string
  periodEnd: string
  targetType: string | null
  targetId: number | null
  targetName: string | null
  orgUnitId: number | null
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
  projectId: number
  date: string
}

export interface RebuildPeriodRequest {
  projectId: number
  periodType: PeriodType
  periodStart: string
}

// ==================== Inspector Performance 读模型 ====================

export interface InspectorSummary {
  id: number
  tenantId?: number
  projectId: number
  inspectorId: number
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
  id: number
  tenantId?: number
  projectId: number
  periodType: string
  periodStart: string
  periodEnd?: string
  itemCode?: string
  itemName?: string
  sectionId?: number
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
  id: number
  tenantId?: number
  projectId: number
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
