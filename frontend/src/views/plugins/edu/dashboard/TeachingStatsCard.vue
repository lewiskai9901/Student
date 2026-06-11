<template>
  <!-- 本学期教学卡 — 教育插件向总览看板贡献的 teaching 分区
       (后端 TeachingDashboardContributor 产出; 原硬编码在核心 DashboardView, 2026-06-12 组件注册化) -->
  <div class="rounded-lg border border-gray-200 bg-white px-5 py-4">
    <div class="mb-3 flex items-center justify-between">
      <span class="text-sm font-medium text-gray-500">本学期教学</span>
      <span class="rounded bg-blue-50 px-2 py-0.5 text-xs font-medium text-blue-600">{{ stats.currentSemester ?? '--' }}</span>
    </div>
    <div class="flex items-baseline gap-6 text-sm">
      <div>
        <span class="text-gray-500">开设课程</span>
        <span class="ml-1.5 text-lg font-semibold text-gray-900">{{ stats.courseCount ?? 0 }}</span>
      </div>
      <span class="text-gray-300">|</span>
      <div>
        <span class="text-gray-500">教学任务</span>
        <span class="ml-1.5 text-lg font-semibold text-gray-900">{{ stats.taskCount ?? 0 }}</span>
      </div>
      <span class="text-gray-300">|</span>
      <div>
        <span class="text-gray-500">未排课</span>
        <span class="ml-1.5 text-lg font-semibold" :class="(stats.unscheduledCount ?? 0) > 0 ? 'text-amber-600' : 'text-gray-900'">{{ stats.unscheduledCount ?? 0 }}</span>
      </div>
    </div>
    <!-- 排课进度条 -->
    <div class="mt-3">
      <div class="mb-1 flex items-center justify-between text-xs">
        <span class="text-gray-500">排课进度</span>
        <span class="font-medium text-gray-700">{{ stats.scheduledRate ?? 0 }}%</span>
      </div>
      <div class="h-2 w-full overflow-hidden rounded-full bg-gray-100">
        <div
          class="h-full rounded-full transition-all duration-700"
          :class="(stats.scheduledRate ?? 0) >= 90 ? 'bg-green-500' : (stats.scheduledRate ?? 0) >= 60 ? 'bg-blue-500' : 'bg-amber-500'"
          :style="{ width: (stats.scheduledRate ?? 0) + '%' }"
        ></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface TeachingStats {
  currentSemester?: string
  courseCount?: number
  taskCount?: number
  scheduledRate?: number
  unscheduledCount?: number
}

const props = defineProps<{ data?: Record<string, unknown> }>()
const stats = computed<TeachingStats>(() => (props.data ?? {}) as TeachingStats)
</script>
