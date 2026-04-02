/**
 * V7 检查平台 - 计时追踪 composable
 * 跟踪检查项/分区的耗时数据
 */
import { ref, reactive, computed } from 'vue'

interface ItemTiming {
  itemId: number
  startedAt: number // timestamp ms
  endedAt: number | null
  totalSeconds: number
}

export function useTimingTracker() {
  const itemTimings = reactive<Map<number, ItemTiming>>(new Map())
  const taskStartedAt = ref<number | null>(null)
  const taskEndedAt = ref<number | null>(null)
  const currentItemId = ref<number | null>(null)

  const totalTimeSeconds = computed(() => {
    if (!taskStartedAt.value) return 0
    const end = taskEndedAt.value || Date.now()
    return Math.round((end - taskStartedAt.value) / 1000)
  })

  function startTask() {
    taskStartedAt.value = Date.now()
    taskEndedAt.value = null
  }

  function endTask() {
    taskEndedAt.value = Date.now()
    // Finalize current item if any
    if (currentItemId.value !== null) {
      stopItem(currentItemId.value)
    }
  }

  function startItem(itemId: number) {
    // Stop the previous item first
    if (currentItemId.value !== null && currentItemId.value !== itemId) {
      stopItem(currentItemId.value)
    }

    const existing = itemTimings.get(itemId)
    if (existing && existing.endedAt === null) {
      // Already tracking this item
      return
    }

    const now = Date.now()
    if (existing) {
      // Resume: reset start but keep accumulated time
      existing.startedAt = now
      existing.endedAt = null
    } else {
      itemTimings.set(itemId, {
        itemId,
        startedAt: now,
        endedAt: null,
        totalSeconds: 0,
      })
    }
    currentItemId.value = itemId
  }

  function stopItem(itemId: number) {
    const timing = itemTimings.get(itemId)
    if (!timing || timing.endedAt !== null) return

    const now = Date.now()
    timing.endedAt = now
    timing.totalSeconds += Math.round((now - timing.startedAt) / 1000)

    if (currentItemId.value === itemId) {
      currentItemId.value = null
    }
  }

  function getItemTimeSeconds(itemId: number): number {
    const timing = itemTimings.get(itemId)
    if (!timing) return 0
    if (timing.endedAt === null) {
      // Still active
      return timing.totalSeconds + Math.round((Date.now() - timing.startedAt) / 1000)
    }
    return timing.totalSeconds
  }

  function getAllTimings(): Record<number, number> {
    const result: Record<number, number> = {}
    for (const [itemId, _timing] of itemTimings) {
      result[itemId] = getItemTimeSeconds(itemId)
    }
    return result
  }

  function reset() {
    itemTimings.clear()
    taskStartedAt.value = null
    taskEndedAt.value = null
    currentItemId.value = null
  }

  return {
    taskStartedAt,
    taskEndedAt,
    totalTimeSeconds,
    currentItemId,
    startTask,
    endTask,
    startItem,
    stopItem,
    getItemTimeSeconds,
    getAllTimings,
    reset,
  }
}
