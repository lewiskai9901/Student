/**
 * V7 检查平台 - 分析报表筛选器 Composable
 *
 * 职责：
 * 1. 管理分析报表的筛选参数（项目、周期、日期范围、目标类型等）
 * 2. 监听周期类型变更自动重置起始日期
 * 3. 构建 API 查询参数
 * 4. 构建导出参数
 */
import { ref, computed, watch } from 'vue'
import type { PeriodType } from '@/types/insp/enums'

/**
 * Get the default period start date based on the period type.
 * Returns an ISO date string (YYYY-MM-DD).
 */
function getDefaultPeriodStart(type: string): string {
  const now = new Date()
  switch (type) {
    case 'WEEKLY': {
      // Start of the current week (Monday)
      const day = now.getDay()
      const diff = day === 0 ? 6 : day - 1 // Monday = 0 offset
      const monday = new Date(now)
      monday.setDate(now.getDate() - diff)
      return formatDate(monday)
    }
    case 'MONTHLY': {
      // Start of the current month
      return formatDate(new Date(now.getFullYear(), now.getMonth(), 1))
    }
    case 'QUARTERLY': {
      // Start of the current quarter
      const quarterMonth = Math.floor(now.getMonth() / 3) * 3
      return formatDate(new Date(now.getFullYear(), quarterMonth, 1))
    }
    case 'YEARLY': {
      // Start of the current year
      return formatDate(new Date(now.getFullYear(), 0, 1))
    }
    default:
      return formatDate(now)
  }
}

function formatDate(date: Date): string {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

function getDefaultDateRange(): [string, string] {
  const now = new Date()
  const sevenDaysAgo = new Date(now)
  sevenDaysAgo.setDate(now.getDate() - 7)
  return [formatDate(sevenDaysAgo), formatDate(now)]
}

export function useAnalyticsFilters() {
  // ==================== State ====================

  const projectId = ref<number | null>(null)
  const periodType = ref<PeriodType>('WEEKLY')
  const periodStart = ref<string>(getDefaultPeriodStart('WEEKLY'))
  const dateRange = ref<[string, string]>(getDefaultDateRange())
  const targetType = ref<string>('')
  const targetId = ref<number | null>(null)

  // ==================== Watchers ====================

  // When periodType changes, reset periodStart to the correct default
  watch(periodType, (newType) => {
    periodStart.value = getDefaultPeriodStart(newType)
  })

  // ==================== Computed ====================

  /** Build query params for period-based API calls */
  const queryParams = computed(() => {
    const params: Record<string, any> = {}
    if (projectId.value != null) params.projectId = projectId.value
    if (periodType.value) params.periodType = periodType.value
    if (periodStart.value) params.periodStart = periodStart.value
    return params
  })

  /** Build query params for trend/date-range API calls */
  const dateRangeParams = computed(() => {
    const params: Record<string, any> = {}
    if (projectId.value != null) params.projectId = projectId.value
    if (dateRange.value[0]) params.startDate = dateRange.value[0]
    if (dateRange.value[1]) params.endDate = dateRange.value[1]
    if (targetType.value) params.targetType = targetType.value
    if (targetId.value != null) params.targetId = targetId.value
    return params
  })

  // ==================== Actions ====================

  /** Reset all filters to default values */
  function resetFilters() {
    projectId.value = null
    periodType.value = 'WEEKLY'
    periodStart.value = getDefaultPeriodStart('WEEKLY')
    dateRange.value = getDefaultDateRange()
    targetType.value = ''
    targetId.value = null
  }

  /** Build export params for file download */
  function buildExportParams(): Record<string, any> {
    return {
      projectId: projectId.value,
      periodType: periodType.value,
      periodStart: periodStart.value,
      startDate: dateRange.value[0],
      endDate: dateRange.value[1],
      targetType: targetType.value || undefined,
      targetId: targetId.value || undefined,
    }
  }

  return {
    // State
    projectId,
    periodType,
    periodStart,
    dateRange,
    targetType,
    targetId,
    // Computed
    queryParams,
    dateRangeParams,
    // Actions
    resetFilters,
    buildExportParams,
  }
}
