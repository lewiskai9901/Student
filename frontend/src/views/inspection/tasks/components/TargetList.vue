<script setup lang="ts">
/**
 * TargetList - Inspection target list (submission navigation)
 *
 * Displays a vertical list of inspection targets with their status,
 * score, and completion state. Supports navigation between targets.
 */
import { computed } from 'vue'
import { Check, Clock, AlertCircle, SkipForward, Lock } from 'lucide-vue-next'
import { SubmissionStatusConfig, type SubmissionStatus } from '@/types/insp/enums'

const props = withDefaults(defineProps<{
  submissions: any[]
  currentIndex: number
}>(), {
  currentIndex: 0,
})

const emit = defineEmits<{
  select: [index: number]
}>()

// ---------- Computed ----------

const totalCount = computed(() => props.submissions.length)

const completedCount = computed(
  () => props.submissions.filter(s => s.status === 'COMPLETED').length,
)

const skippedCount = computed(
  () => props.submissions.filter(s => s.status === 'SKIPPED').length,
)

const progressPercent = computed(() => {
  if (totalCount.value === 0) return 0
  return Math.round(
    ((completedCount.value + skippedCount.value) / totalCount.value) * 100,
  )
})

// ---------- Helpers ----------

function getStatusIcon(status: string) {
  switch (status) {
    case 'COMPLETED': return Check
    case 'SKIPPED': return SkipForward
    case 'IN_PROGRESS': return Clock
    case 'LOCKED': return Lock
    default: return AlertCircle
  }
}

function getStatusColor(status: string): string {
  switch (status) {
    case 'COMPLETED': return 'text-green-500'
    case 'SKIPPED': return 'text-amber-500'
    case 'IN_PROGRESS': return 'text-blue-500'
    case 'LOCKED': return 'text-gray-400'
    default: return 'text-gray-400'
  }
}

function getScoreColor(score: number | null): string {
  if (score == null) return 'text-gray-400'
  if (score >= 90) return 'text-green-600'
  if (score >= 70) return 'text-blue-600'
  if (score >= 60) return 'text-amber-600'
  return 'text-red-600'
}

function handleSelect(index: number) {
  emit('select', index)
}
</script>

<template>
  <div class="target-list flex flex-col h-full">
    <!-- Progress Header -->
    <div class="p-3 border-b border-gray-200 bg-white shrink-0">
      <div class="flex items-center justify-between mb-2">
        <span class="text-sm font-medium text-gray-700">检查目标</span>
        <span class="text-xs text-gray-500">
          {{ completedCount }}/{{ totalCount }}
        </span>
      </div>
      <el-progress
        :percentage="progressPercent"
        :stroke-width="6"
        :show-text="false"
        :color="progressPercent >= 100 ? '#67C23A' : '#409EFF'"
      />
      <div class="flex items-center justify-between mt-1">
        <span class="text-xs text-gray-400">{{ progressPercent }}%</span>
        <span v-if="skippedCount > 0" class="text-xs text-amber-500">
          {{ skippedCount }} 已跳过
        </span>
      </div>
    </div>

    <!-- Target List -->
    <div class="flex-1 overflow-auto">
      <div
        v-for="(submission, index) in submissions"
        :key="submission.id ?? index"
        class="flex items-center gap-2 px-3 py-2.5 cursor-pointer border-b border-gray-50 transition"
        :class="[
          index === currentIndex
            ? 'bg-blue-50 border-l-2 border-l-blue-500'
            : 'hover:bg-gray-50 border-l-2 border-l-transparent',
        ]"
        @click="handleSelect(index)"
      >
        <!-- Status Icon -->
        <component
          :is="getStatusIcon(submission.status)"
          class="w-4 h-4 shrink-0"
          :class="getStatusColor(submission.status)"
        />

        <!-- Target Info -->
        <div class="flex-1 min-w-0">
          <div class="flex items-center justify-between">
            <span
              class="text-sm truncate"
              :class="index === currentIndex ? 'font-medium text-blue-700' : 'text-gray-700'"
            >
              {{ submission.targetName || `目标 ${index + 1}` }}
            </span>
            <span
              v-if="submission.finalScore != null"
              class="text-sm font-bold shrink-0 ml-2"
              :class="getScoreColor(submission.finalScore)"
            >
              {{ submission.finalScore }}
            </span>
          </div>
          <div class="flex items-center gap-2 mt-0.5">
            <span v-if="submission.orgUnitName" class="text-xs text-gray-400 truncate">
              {{ submission.orgUnitName }}
            </span>
            <el-tag
              v-if="submission.status"
              size="small"
              :type="(SubmissionStatusConfig[submission.status as SubmissionStatus]?.type as any) || 'info'"
              class="!h-4 !leading-4 !text-[10px]"
            >
              {{ SubmissionStatusConfig[submission.status as SubmissionStatus]?.label || submission.status }}
            </el-tag>
          </div>
        </div>

        <!-- Index Badge -->
        <span class="text-xs text-gray-300 shrink-0">{{ index + 1 }}</span>
      </div>

      <!-- Empty State -->
      <div
        v-if="submissions.length === 0"
        class="py-12 text-center text-sm text-gray-400"
      >
        暂无检查目标
      </div>
    </div>
  </div>
</template>
