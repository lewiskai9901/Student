<template>
  <div v-if="jobs.length > 0" class="rounded-xl border border-gray-200 bg-white">
    <div class="flex items-center justify-between px-5 py-2.5">
      <div class="flex items-center gap-2">
        <h3 class="text-xs font-semibold text-gray-700">批量任务</h3>
        <span v-if="runningCount > 0" class="rounded-full bg-blue-100 px-1.5 py-px text-[10px] font-medium text-blue-600">{{ runningCount }} 执行中</span>
        <span class="text-[11px] text-gray-400">({{ jobs.length }})</span>
      </div>
      <button
        v-if="collapsible"
        class="text-[11px] font-medium text-gray-400 hover:text-gray-600"
        @click="expanded = !expanded"
      >{{ expanded ? '收起' : '展开' }}</button>
    </div>

    <div v-if="expanded" class="border-t border-gray-100">
      <div
        v-for="job in jobs"
        :key="job.jobId"
        class="flex items-center gap-3 border-b border-gray-50 px-5 py-2 text-xs last:border-b-0 hover:bg-gray-50/50"
      >
        <!-- Status badge -->
        <span class="flex-shrink-0 rounded px-1.5 py-0.5 text-[10px] font-medium" :class="statusClass(job.jobStatus)">
          {{ statusLabel(job.jobStatus) }}
        </span>

        <!-- Job type -->
        <span class="flex-shrink-0 text-gray-500">{{ jobTypeLabel(job.jobType) }}</span>

        <!-- Progress -->
        <div class="flex flex-1 items-center gap-2">
          <div class="h-1.5 flex-1 rounded-full bg-gray-100">
            <div
              class="h-1.5 rounded-full transition-all"
              :class="job.jobStatus === 'FAILED' ? 'bg-red-400' : 'bg-blue-500'"
              :style="{ width: (job.progressPercentage || 0) + '%' }"
            />
          </div>
          <span class="flex-shrink-0 text-[11px] text-gray-500">
            {{ job.successCount }}<span v-if="job.failureCount > 0" class="text-red-500">/{{ job.failureCount }}失败</span>/{{ job.totalItems }}
          </span>
        </div>

        <!-- Creator & Time -->
        <span class="flex-shrink-0 text-gray-400">{{ job.createdByName }}</span>
        <span class="flex-shrink-0 text-gray-300">{{ shortTime(job.createdAt) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { getMyRecentJobs } from '@/api/batchJob'
import type { BatchJobDTO } from '@/types/batchJob'

const props = withDefaults(defineProps<{
  collapsible?: boolean
  defaultExpanded?: boolean
  autoRefresh?: boolean
  refreshInterval?: number
}>(), {
  collapsible: true,
  defaultExpanded: false,
  autoRefresh: true,
  refreshInterval: 15
})

const jobs = ref<BatchJobDTO[]>([])
const expanded = ref(props.defaultExpanded)
const loading = ref(false)
let timer: ReturnType<typeof setInterval> | null = null

const runningCount = computed(() =>
  jobs.value.filter(j => j.jobStatus === 'RUNNING' || j.jobStatus === 'PENDING').length
)

const loadJobs = async () => {
  try {
    jobs.value = await getMyRecentJobs(10)
    // Auto-expand if there are running jobs
    if (runningCount.value > 0 && !expanded.value) {
      expanded.value = true
    }
  } catch (e) {
    console.error('加载批量任务失败:', e)
  }
}

const statusClass = (s: string) => ({
  'bg-blue-50 text-blue-600': s === 'RUNNING',
  'bg-gray-100 text-gray-500': s === 'PENDING',
  'bg-emerald-50 text-emerald-600': s === 'COMPLETED',
  'bg-red-50 text-red-500': s === 'FAILED',
  'bg-gray-100 text-gray-400': s === 'CANCELLED'
})

const statusLabel = (s: string) =>
  ({ PENDING: '待处理', RUNNING: '执行中', COMPLETED: '已完成', FAILED: '失败', CANCELLED: '已取消' }[s] || s)

const jobTypeLabel = (t: string) =>
  ({ BATCH_ASSIGN_ORG: '分配组织', BATCH_CHANGE_STATUS: '状态变更', BATCH_ASSIGN_RESPONSIBLE: '分配负责人' }[t] || t)

const shortTime = (d?: string) => {
  if (!d) return ''
  const date = new Date(d)
  return `${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

onMounted(() => {
  loadJobs()
  if (props.autoRefresh) {
    timer = setInterval(loadJobs, props.refreshInterval * 1000)
  }
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

defineExpose({ loadJobs })
</script>
