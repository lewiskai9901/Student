<script setup lang="ts">
/**
 * ScoreSummary - Scoring summary panel
 *
 * Displays a breakdown of inspection scores across dimensions,
 * a total score with progress bar, and an optional grade badge.
 */
import { computed } from 'vue'

interface DimensionScore {
  name: string
  score: number
  maxScore: number
}

const props = withDefaults(defineProps<{
  dimensions: DimensionScore[]
  totalScore: number
  maxTotalScore?: number
  grade?: string
}>(), {
  maxTotalScore: 100,
})

// ---------- Computed ----------

const totalPercent = computed(() => {
  if (props.maxTotalScore <= 0) return 0
  return Math.min(100, Math.round((props.totalScore / props.maxTotalScore) * 100))
})

const totalScoreColor = computed(() => {
  const pct = totalPercent.value
  if (pct >= 90) return '#67C23A'
  if (pct >= 70) return '#409EFF'
  if (pct >= 60) return '#E6A23C'
  return '#F56C6C'
})

const totalScoreClass = computed(() => {
  const pct = totalPercent.value
  if (pct >= 90) return 'text-green-600'
  if (pct >= 70) return 'text-blue-600'
  if (pct >= 60) return 'text-amber-600'
  return 'text-red-600'
})

const gradeColor = computed(() => {
  if (!props.grade) return ''
  const g = props.grade.toUpperCase()
  if (g === 'A' || g === '优' || g === '优秀') return 'bg-green-100 text-green-700 border-green-200'
  if (g === 'B' || g === '良' || g === '良好') return 'bg-blue-100 text-blue-700 border-blue-200'
  if (g === 'C' || g === '中' || g === '合格') return 'bg-amber-100 text-amber-700 border-amber-200'
  return 'bg-red-100 text-red-700 border-red-200'
})

// Dimension percentage
function dimPercent(dim: DimensionScore): number {
  if (dim.maxScore <= 0) return 0
  return Math.min(100, Math.round((dim.score / dim.maxScore) * 100))
}

function dimBarColor(dim: DimensionScore): string {
  const pct = dimPercent(dim)
  if (pct >= 90) return '#67C23A'
  if (pct >= 70) return '#409EFF'
  if (pct >= 60) return '#E6A23C'
  return '#F56C6C'
}
</script>

<template>
  <div class="score-summary">
    <!-- Total Score Card -->
    <div class="rounded-lg border border-gray-200 bg-white p-4 mb-4">
      <div class="flex items-center justify-between mb-3">
        <span class="text-sm font-medium text-gray-700">总分</span>
        <span
          v-if="grade"
          class="px-2 py-0.5 text-xs font-medium rounded border"
          :class="gradeColor"
        >
          {{ grade }}
        </span>
      </div>

      <div class="flex items-end gap-2 mb-3">
        <span class="text-3xl font-bold" :class="totalScoreClass">
          {{ totalScore }}
        </span>
        <span class="text-sm text-gray-400 pb-1">
          / {{ maxTotalScore }}
        </span>
      </div>

      <el-progress
        :percentage="totalPercent"
        :stroke-width="10"
        :show-text="false"
        :color="totalScoreColor"
      />
      <div class="flex items-center justify-between mt-1">
        <span class="text-xs text-gray-400">{{ totalPercent }}%</span>
        <span class="text-xs" :class="totalScoreClass">
          {{ totalPercent >= 90 ? '优秀' : totalPercent >= 70 ? '良好' : totalPercent >= 60 ? '合格' : '不合格' }}
        </span>
      </div>
    </div>

    <!-- Dimension Breakdown -->
    <div v-if="dimensions.length > 0" class="rounded-lg border border-gray-200 bg-white overflow-hidden">
      <div class="px-4 py-2.5 bg-gray-50 border-b border-gray-200">
        <span class="text-sm font-medium text-gray-700">维度明细</span>
      </div>

      <div class="divide-y divide-gray-100">
        <div
          v-for="(dim, index) in dimensions"
          :key="index"
          class="px-4 py-3"
        >
          <div class="flex items-center justify-between mb-1.5">
            <span class="text-sm text-gray-600">{{ dim.name }}</span>
            <span class="text-sm font-medium" :class="dimPercent(dim) >= 60 ? 'text-gray-700' : 'text-red-600'">
              {{ dim.score }} / {{ dim.maxScore }}
            </span>
          </div>
          <el-progress
            :percentage="dimPercent(dim)"
            :stroke-width="6"
            :show-text="false"
            :color="dimBarColor(dim)"
          />
        </div>
      </div>
    </div>

    <!-- No Dimensions -->
    <div
      v-else
      class="rounded-lg border border-dashed border-gray-200 py-6 text-center text-sm text-gray-400"
    >
      暂无维度评分数据
    </div>
  </div>
</template>
