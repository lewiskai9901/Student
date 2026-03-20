/**
 * V7 检查平台 - 分析报表 API
 */
import { http } from '@/utils/request'
import type {
  DailySummary,
  PeriodSummary,
  CorrectiveSummary,
  RebuildDailyRequest,
  RebuildPeriodRequest,
  InspectorSummary,
  ItemFrequencySummary,
  CorrectivePeriodSummary,
} from '@/types/insp/analytics'
import type { PeriodType } from '@/types/insp/enums'

const BASE = '/v7/insp/analytics'

// ==================== Daily ====================

export function getDailyRanking(projectId: number, date: string): Promise<DailySummary[]> {
  return http.get<DailySummary[]>(`${BASE}/daily-ranking`, { params: { projectId, date } })
}

export function getDailySummary(projectId: number, date: string): Promise<DailySummary[]> {
  return http.get<DailySummary[]>(`${BASE}/daily-summary`, { params: { projectId, date } })
}

// ==================== Period ====================

export function getPeriodSummary(projectId: number, periodType: PeriodType, periodStart: string): Promise<PeriodSummary[]> {
  return http.get<PeriodSummary[]>(`${BASE}/period-summary`, { params: { projectId, periodType, periodStart } })
}

// ==================== Trend ====================

export function getTrend(projectId: number, targetType: string, targetId: number, startDate: string, endDate: string): Promise<DailySummary[]> {
  return http.get<DailySummary[]>(`${BASE}/trend`, { params: { projectId, targetType, targetId, startDate, endDate } })
}

// ==================== Comparison ====================

export function getComparison(projectId: number, date: string): Promise<DailySummary[]> {
  return http.get<DailySummary[]>(`${BASE}/comparison`, { params: { projectId, date } })
}

// ==================== Dimension Breakdown ====================

export function getDimensionBreakdown(projectId: number, startDate: string, endDate: string): Promise<DailySummary[]> {
  return http.get<DailySummary[]>(`${BASE}/dimension-breakdown`, { params: { projectId, startDate, endDate } })
}

// ==================== Inspector Performance ====================

export function getInspectorPerformance(projectId: number, periodType: PeriodType, periodStart: string): Promise<PeriodSummary[]> {
  return http.get<PeriodSummary[]>(`${BASE}/inspector-performance`, { params: { projectId, periodType, periodStart } })
}

// ==================== Corrective Summary ====================

export function getCorrectiveSummary(projectId: number, periodType?: string, periodStart?: string): Promise<CorrectiveSummary> {
  if (periodType && periodStart) {
    return http.get<CorrectiveSummary>(`${BASE}/corrective-summary`, { params: { projectId, periodType, periodStart } })
  }
  // Use live summary when no period params provided
  return http.get<CorrectiveSummary>(`${BASE}/corrective-summary-live`, { params: { projectId } })
}

// ==================== Rebuild (Admin) ====================

export function rebuildDaily(data: RebuildDailyRequest): Promise<void> {
  return http.post(`${BASE}/rebuild-daily`, data)
}

export function rebuildPeriod(data: RebuildPeriodRequest): Promise<void> {
  return http.post(`${BASE}/rebuild-period`, data)
}

// ==================== Inspector Summaries ====================

export function getInspectorSummaries(projectId: number, periodType: string, periodStart: string): Promise<InspectorSummary[]> {
  return http.get<InspectorSummary[]>(`${BASE}/inspector-summaries`, { params: { projectId, periodType, periodStart } })
}

// ==================== Item Frequency / Pareto ====================

export function getItemFrequencies(projectId: number, periodType: string, periodStart: string): Promise<ItemFrequencySummary[]> {
  return http.get<ItemFrequencySummary[]>(`${BASE}/item-frequency`, { params: { projectId, periodType, periodStart } })
}

export function getPareto(projectId: number, periodType: string, periodStart: string, limit?: number): Promise<ItemFrequencySummary[]> {
  return http.get<ItemFrequencySummary[]>(`${BASE}/pareto`, { params: { projectId, periodType, periodStart, limit } })
}

// ==================== Corrective Period Summary ====================

export function getCorrectiveSummaryByPeriod(projectId: number, periodType: string, periodStart: string): Promise<CorrectivePeriodSummary> {
  return http.get<CorrectivePeriodSummary>(`${BASE}/corrective-summary`, { params: { projectId, periodType, periodStart } })
}

export function getCorrectiveSummaries(projectId: number): Promise<CorrectivePeriodSummary[]> {
  return http.get<CorrectivePeriodSummary[]>(`${BASE}/corrective-summaries`, { params: { projectId } })
}

// ==================== API 对象 ====================

export const inspAnalyticsApi = {
  getDailyRanking,
  getDailySummary,
  getPeriodSummary,
  getTrend,
  getComparison,
  getDimensionBreakdown,
  getInspectorPerformance,
  getCorrectiveSummary,
  rebuildDaily,
  rebuildPeriod,
  getInspectorSummaries,
  getItemFrequencies,
  getPareto,
  getCorrectiveSummaryByPeriod,
  getCorrectiveSummaries,
}
