/**
 * 检查平台 - 分析报表 Store
 */
import type { LongId } from '@/types/common'
import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { DailySummary, PeriodSummary, CorrectiveSummary, InspectorSummary, ItemFrequencySummary, CorrectivePeriodSummary } from '@/types/insp/analytics'
import type { PeriodType } from '@/types/insp/enums'
import { inspAnalyticsApi } from '@/api/inspection/analytics'

export const useInspAnalyticsStore = defineStore('inspAnalytics', () => {
  const dailyRanking = ref<DailySummary[]>([])
  const dailySummary = ref<DailySummary[]>([])
  const periodSummary = ref<PeriodSummary[]>([])
  const trendData = ref<DailySummary[]>([])
  const comparisonData = ref<DailySummary[]>([])
  const dimensionBreakdown = ref<DailySummary[]>([])
  const inspectorPerformance = ref<PeriodSummary[]>([])
  const correctiveSummary = ref<CorrectiveSummary | null>(null)
  const inspectorSummaries = ref<InspectorSummary[]>([])
  const itemFrequencies = ref<ItemFrequencySummary[]>([])
  const correctivePeriodSummary = ref<CorrectivePeriodSummary | null>(null)
  const correctiveSummaries = ref<CorrectivePeriodSummary[]>([])
  const loading = ref(false)

  // ========== Daily ==========

  async function fetchDailyRanking(projectId: LongId, date: string) {
    loading.value = true
    try {
      dailyRanking.value = await inspAnalyticsApi.getDailyRanking(projectId, date)
    } finally {
      loading.value = false
    }
  }

  async function fetchDailySummary(projectId: LongId, date: string) {
    loading.value = true
    try {
      dailySummary.value = await inspAnalyticsApi.getDailySummary(projectId, date)
    } finally {
      loading.value = false
    }
  }

  // ========== Period ==========

  async function fetchPeriodSummary(projectId: LongId, periodType: PeriodType, periodStart: string) {
    loading.value = true
    try {
      periodSummary.value = await inspAnalyticsApi.getPeriodSummary(projectId, periodType, periodStart)
    } finally {
      loading.value = false
    }
  }

  // ========== Trend ==========

  async function fetchTrend(projectId: LongId, targetType: string, targetId: LongId, startDate: string, endDate: string) {
    loading.value = true
    try {
      trendData.value = await inspAnalyticsApi.getTrend(projectId, targetType, targetId, startDate, endDate)
    } finally {
      loading.value = false
    }
  }

  // ========== Comparison ==========

  async function fetchComparison(projectId: LongId, date: string) {
    loading.value = true
    try {
      comparisonData.value = await inspAnalyticsApi.getComparison(projectId, date)
    } finally {
      loading.value = false
    }
  }

  // ========== Dimension Breakdown ==========

  async function fetchDimensionBreakdown(projectId: LongId, startDate: string, endDate: string) {
    loading.value = true
    try {
      dimensionBreakdown.value = await inspAnalyticsApi.getDimensionBreakdown(projectId, startDate, endDate)
    } finally {
      loading.value = false
    }
  }

  // ========== Inspector Performance ==========

  async function fetchInspectorPerformance(projectId: LongId, periodType: PeriodType, periodStart: string) {
    loading.value = true
    try {
      inspectorPerformance.value = await inspAnalyticsApi.getInspectorPerformance(projectId, periodType, periodStart)
    } finally {
      loading.value = false
    }
  }

  // ========== Corrective Summary ==========

  async function fetchCorrectiveSummary(projectId: LongId) {
    loading.value = true
    try {
      correctiveSummary.value = await inspAnalyticsApi.getCorrectiveSummary(projectId)
    } finally {
      loading.value = false
    }
  }

  // ========== Rebuild ==========

  async function rebuildDaily(projectId: LongId, date: string) {
    await inspAnalyticsApi.rebuildDaily({ projectId, date })
  }

  async function rebuildPeriod(projectId: LongId, periodType: PeriodType, periodStart: string) {
    await inspAnalyticsApi.rebuildPeriod({ projectId, periodType, periodStart })
  }

  // ========== Inspector Summary ==========

  async function fetchInspectorSummaries(projectId: LongId, periodType: PeriodType, periodStart: string) {
    loading.value = true
    try {
      inspectorSummaries.value = await inspAnalyticsApi.getInspectorSummaries(projectId, periodType, periodStart)
    } finally {
      loading.value = false
    }
  }

  // ========== Item Frequency ==========

  async function fetchItemFrequencies(projectId: LongId, periodType: PeriodType, periodStart: string) {
    loading.value = true
    try {
      itemFrequencies.value = await inspAnalyticsApi.getItemFrequencies(projectId, periodType, periodStart)
    } finally {
      loading.value = false
    }
  }

  async function fetchPareto(projectId: LongId, periodType: PeriodType, periodStart: string, limit = 20) {
    loading.value = true
    try {
      itemFrequencies.value = await inspAnalyticsApi.getPareto(projectId, periodType, periodStart, limit)
    } finally {
      loading.value = false
    }
  }

  // ========== Corrective Period Summary ==========

  async function fetchCorrectivePeriodSummary(projectId: LongId, periodType: PeriodType, periodStart: string) {
    loading.value = true
    try {
      correctivePeriodSummary.value = await inspAnalyticsApi.getCorrectivePeriodSummary(projectId, periodType, periodStart)
    } finally {
      loading.value = false
    }
  }

  async function fetchCorrectiveSummaries(projectId: LongId) {
    loading.value = true
    try {
      correctiveSummaries.value = await inspAnalyticsApi.getCorrectiveSummaries(projectId)
    } finally {
      loading.value = false
    }
  }

  return {
    dailyRanking, dailySummary, periodSummary, trendData,
    comparisonData, dimensionBreakdown, inspectorPerformance,
    correctiveSummary, inspectorSummaries, itemFrequencies,
    correctivePeriodSummary, correctiveSummaries, loading,
    fetchDailyRanking, fetchDailySummary, fetchPeriodSummary,
    fetchTrend, fetchComparison, fetchDimensionBreakdown,
    fetchInspectorPerformance, fetchCorrectiveSummary,
    fetchInspectorSummaries, fetchItemFrequencies, fetchPareto,
    fetchCorrectivePeriodSummary, fetchCorrectiveSummaries,
    rebuildDaily, rebuildPeriod,
  }
})
