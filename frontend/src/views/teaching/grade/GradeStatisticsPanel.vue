<template>
  <div class="rounded-xl border border-gray-200 bg-white">
    <!-- Header with tabs -->
    <div class="flex items-center justify-between border-b border-gray-100 px-5 py-3">
      <div class="flex items-center gap-4">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          class="text-sm font-medium transition-colors"
          :class="activeTab === tab.key ? 'text-blue-600' : 'text-gray-500 hover:text-gray-700'"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
        </button>
      </div>
      <button
        class="inline-flex items-center gap-1.5 rounded-lg bg-gray-100 px-3 py-1.5 text-xs font-medium text-gray-700 transition-colors hover:bg-gray-200 disabled:opacity-50"
        :disabled="!semesterId || exporting"
        @click="doExport"
      >
        {{ exporting ? '导出中...' : '导出Excel' }}
      </button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex items-center justify-center py-16">
      <div class="h-8 w-8 animate-spin rounded-full border-2 border-blue-600 border-t-transparent" />
    </div>

    <!-- Statistics Summary Tab -->
    <template v-else-if="activeTab === 'summary'">
      <template v-if="statistics">
        <!-- Stats cards -->
        <div class="grid grid-cols-4 gap-4 px-5 pt-4">
          <div class="rounded-lg border border-gray-200 bg-gray-50 px-4 py-3">
            <div class="text-xs text-gray-500">总人数</div>
            <div class="mt-1 text-xl font-semibold text-gray-900">{{ statistics.totalCount }}</div>
          </div>
          <div class="rounded-lg border border-gray-200 bg-gray-50 px-4 py-3">
            <div class="text-xs text-gray-500">及格人数</div>
            <div class="mt-1 text-xl font-semibold text-gray-900">
              {{ statistics.passCount }}
              <span class="ml-1 text-xs font-normal text-gray-400">({{ (statistics.passRate * 100).toFixed(1) }}%)</span>
            </div>
          </div>
          <div class="rounded-lg border border-gray-200 bg-gray-50 px-4 py-3">
            <div class="text-xs text-gray-500">优秀人数</div>
            <div class="mt-1 text-xl font-semibold text-gray-900">
              {{ statistics.excellentCount }}
              <span class="ml-1 text-xs font-normal text-gray-400">({{ (statistics.excellentRate * 100).toFixed(1) }}%)</span>
            </div>
          </div>
          <div class="rounded-lg border border-gray-200 bg-gray-50 px-4 py-3">
            <div class="text-xs text-gray-500">平均分</div>
            <div class="mt-1 text-xl font-semibold text-gray-900">{{ statistics.averageScore?.toFixed(1) }}</div>
          </div>
        </div>

        <!-- Distribution & Overview -->
        <div class="grid grid-cols-2 gap-4 px-5 py-4">
          <!-- Distribution -->
          <div class="rounded-xl border border-gray-200 bg-white">
            <div class="border-b border-gray-100 px-5 py-3 text-sm font-medium text-gray-900">成绩分布</div>
            <div class="space-y-3 px-5 py-4">
              <div
                v-for="item in statistics.distribution"
                :key="item.range"
                class="flex items-center gap-3"
              >
                <span class="w-14 text-xs text-gray-500">{{ item.range }}</span>
                <div class="flex-1 overflow-hidden rounded bg-gray-100">
                  <div
                    class="h-5 rounded bg-blue-500 transition-all duration-300"
                    :style="{ width: `${item.percentage}%` }"
                  />
                </div>
                <span class="w-24 text-right text-xs text-gray-500">{{ item.count }}人 ({{ item.percentage.toFixed(1) }}%)</span>
              </div>
            </div>
          </div>

          <!-- Overview -->
          <div class="rounded-xl border border-gray-200 bg-white">
            <div class="border-b border-gray-100 px-5 py-3 text-sm font-medium text-gray-900">成绩概览</div>
            <div class="px-5 py-4">
              <el-descriptions :column="2" border>
                <el-descriptions-item label="最高分">{{ statistics.maxScore }}</el-descriptions-item>
                <el-descriptions-item label="最低分">{{ statistics.minScore }}</el-descriptions-item>
                <el-descriptions-item label="及格率">{{ (statistics.passRate * 100).toFixed(1) }}%</el-descriptions-item>
                <el-descriptions-item label="优秀率">{{ (statistics.excellentRate * 100).toFixed(1) }}%</el-descriptions-item>
              </el-descriptions>
            </div>
          </div>
        </div>
      </template>
      <div v-else class="flex flex-col items-center justify-center py-16 text-gray-400">
        <p class="text-sm">暂无统计数据</p>
      </div>
    </template>

    <!-- Ranking Tab -->
    <template v-else-if="activeTab === 'ranking'">
      <div v-if="rankingLoading" class="flex items-center justify-center py-16">
        <div class="h-8 w-8 animate-spin rounded-full border-2 border-blue-600 border-t-transparent" />
      </div>
      <div v-else-if="ranking.length > 0" class="px-5 py-4">
        <el-table :data="ranking" stripe class="rounded-xl">
          <el-table-column prop="rank" label="排名" width="80" align="center" />
          <el-table-column prop="studentName" label="姓名" min-width="120" />
          <el-table-column prop="totalScore" label="总分" width="100" align="center">
            <template #default="{ row }">
              <span class="font-semibold">{{ row.totalScore }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div v-else class="flex flex-col items-center justify-center py-16 text-gray-400">
        <p class="text-sm">暂无排名数据</p>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { gradeApi } from '@/api/teaching'
import type { GradeStatistics } from '@/types/teaching'

const props = defineProps<{
  semesterId?: number | string | null
  batchId?: number | string | null
}>()

const tabs = [
  { key: 'summary', label: '统计概览' },
  { key: 'ranking', label: '班级排名' },
] as const

type TabKey = (typeof tabs)[number]['key']
const activeTab = ref<TabKey>('summary')

// State
const loading = ref(false)
const rankingLoading = ref(false)
const exporting = ref(false)
const statistics = ref<GradeStatistics>()
const ranking = ref<{ studentId: number | string; studentName: string; totalScore: number; rank: number }[]>([])

// Load statistics when semesterId or batchId changes
watch(
  () => [props.semesterId, props.batchId],
  () => {
    if (props.semesterId || props.batchId) {
      loadStatistics()
    } else {
      statistics.value = undefined
    }
  },
  { immediate: true }
)

// Reload ranking when tab switches to ranking
watch(activeTab, (tab) => {
  if (tab === 'ranking' && props.semesterId) {
    loadRanking()
  }
})

async function loadStatistics() {
  loading.value = true
  statistics.value = undefined
  try {
    const params: Record<string, any> = {}
    if (props.batchId) params.batchId = props.batchId
    if (props.semesterId) params.semesterId = props.semesterId
    const res: any = await gradeApi.getStatistics(params)
    statistics.value = res.data || res
  } catch (error) {
    console.error('Failed to load statistics:', error)
    ElMessage.error('加载统计数据失败')
    statistics.value = undefined
  } finally {
    loading.value = false
  }
}

async function loadRanking() {
  if (!props.semesterId) return
  rankingLoading.value = true
  ranking.value = []
  try {
    const params: any = { semesterId: props.semesterId }
    // classId is not available at this level; API will return overall ranking
    const res: any = await gradeApi.getRanking(params)
    ranking.value = res.data || res || []
  } catch (error) {
    console.error('Failed to load ranking:', error)
    ranking.value = []
  } finally {
    rankingLoading.value = false
  }
}

async function doExport() {
  if (!props.semesterId) {
    ElMessage.warning('请先选择学期')
    return
  }
  exporting.value = true
  try {
    let res: any
    if (props.batchId) {
      res = await gradeApi.exportGrades(props.batchId)
    } else {
      res = await gradeApi.exportGradesByFilter({ semesterId: props.semesterId })
    }
    const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '成绩导出.xlsx'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}
</script>
