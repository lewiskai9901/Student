<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Trophy, Medal, RefreshCw } from 'lucide-vue-next'
import { getProjects, listDimensions, getRankings } from '@/api/insp/project'
import type { InspProject, RatingDimension, RatingResult } from '@/types/insp/project'

// ========== State ==========
const projects = ref<InspProject[]>([])
const selectedProjectId = ref<number | null>(null)
const selectedDate = ref('')
const dimensions = ref<RatingDimension[]>([])
const resultsByDimension = ref<Map<number, RatingResult[]>>(new Map())
const loadingProjects = ref(false)
const loadingDimensions = ref(false)
const loadingResults = ref<Set<number>>(new Set())

// ========== Load projects ==========
async function loadProjects() {
  loadingProjects.value = true
  try {
    projects.value = await getProjects()
    if (projects.value.length > 0 && !selectedProjectId.value) {
      selectedProjectId.value = projects.value[0].id
    }
  } catch (e: any) {
    ElMessage.error(e.message || '加载项目列表失败')
  } finally {
    loadingProjects.value = false
  }
}

// ========== Load dimensions for selected project ==========
async function loadDimensions() {
  if (!selectedProjectId.value) {
    dimensions.value = []
    resultsByDimension.value = new Map()
    return
  }
  loadingDimensions.value = true
  try {
    dimensions.value = await listDimensions(selectedProjectId.value)
    await loadAllResults()
  } catch (e: any) {
    ElMessage.error(e.message || '加载评级维度失败')
  } finally {
    loadingDimensions.value = false
  }
}

// ========== Load results for all dimensions ==========
async function loadAllResults() {
  const newMap = new Map<number, RatingResult[]>()
  const promises = dimensions.value.map(async (dim) => {
    loadingResults.value.add(dim.id)
    try {
      const results = await getRankings(dim.id, { cycleDate: selectedDate.value || undefined })
      newMap.set(dim.id, results)
    } catch {
      newMap.set(dim.id, [])
    } finally {
      loadingResults.value.delete(dim.id)
    }
  })
  await Promise.all(promises)
  resultsByDimension.value = newMap
}

// ========== Helpers ==========
function getRankColor(rank: number | null): string {
  if (rank === 1) return '#f59e0b'
  if (rank === 2) return '#94a3b8'
  if (rank === 3) return '#b45309'
  return '#6b7280'
}

function getRankIcon(rank: number | null): typeof Trophy | typeof Medal | null {
  if (rank === 1) return Trophy
  if (rank != null && rank <= 3) return Medal
  return null
}

function getGradeColor(grade: string | null): string {
  if (!grade) return '#6b7280'
  const map: Record<string, string> = {
    'A+': '#10b981', 'A': '#22c55e', 'B': '#3b82f6', 'C': '#f59e0b', 'D': '#ef4444', 'F': '#dc2626',
  }
  return map[grade] || '#6b7280'
}

function isDimensionLoading(dimId: number): boolean {
  return loadingResults.value.has(dimId)
}

function getDimensionResults(dimId: number): RatingResult[] {
  return resultsByDimension.value.get(dimId) || []
}

function handleRefresh() {
  loadDimensions()
}

// ========== Watchers ==========
watch(selectedProjectId, () => {
  loadDimensions()
})

watch(selectedDate, () => {
  if (dimensions.value.length > 0) {
    loadAllResults()
  }
})

// ========== Init ==========
onMounted(() => {
  loadProjects()
})
</script>

<template>
  <div class="p-6 max-w-[1100px] mx-auto">
    <!-- Page header -->
    <h2 class="text-xl font-bold text-gray-900 mb-5">评级排名</h2>

    <!-- Filters -->
    <div class="flex items-center gap-4 flex-wrap mb-5">
      <div class="flex items-center gap-2">
        <span class="text-sm text-gray-500">项目</span>
        <el-select
          v-model="selectedProjectId"
          placeholder="选择项目"
          class="w-52"
          :loading="loadingProjects"
          filterable
        >
          <el-option
            v-for="p in projects"
            :key="p.id"
            :label="p.projectName"
            :value="p.id"
          />
        </el-select>
      </div>
      <div class="flex items-center gap-2">
        <span class="text-sm text-gray-500">日期</span>
        <el-date-picker
          v-model="selectedDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="留空显示最新"
          clearable
          class="!w-48"
        />
      </div>
      <el-button :icon="RefreshCw" @click="handleRefresh" :loading="loadingDimensions">
        刷新
      </el-button>
    </div>

    <!-- Loading state -->
    <div v-if="loadingDimensions && dimensions.length === 0" class="py-16 text-center text-gray-400">
      <div class="text-sm">加载中...</div>
    </div>

    <!-- No project selected -->
    <div v-else-if="!selectedProjectId" class="py-16 text-center text-gray-400">
      <div class="text-sm">请选择一个项目</div>
    </div>

    <!-- No dimensions -->
    <div v-else-if="dimensions.length === 0 && !loadingDimensions" class="py-16 text-center text-gray-400">
      <Trophy class="w-12 h-12 mx-auto mb-3 text-gray-300" />
      <div class="text-sm">该项目暂无评级维度</div>
      <div class="text-xs text-gray-300 mt-1">请先在项目详情中配置评级维度</div>
    </div>

    <!-- Dimension cards -->
    <div v-else class="space-y-5">
      <div
        v-for="dim in dimensions"
        :key="dim.id"
        class="bg-white border border-gray-200 rounded-xl shadow-sm overflow-hidden"
      >
        <!-- Dimension header -->
        <div class="px-5 py-3 bg-gradient-to-r from-amber-50 to-orange-50 border-b border-amber-200">
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-3">
              <Trophy class="w-4 h-4 text-amber-600" />
              <span class="font-semibold text-gray-800">{{ dim.dimensionName }}</span>
            </div>
            <div v-if="dim.awardName" class="text-sm text-amber-700">
              奖项: {{ dim.awardName }}
            </div>
          </div>
        </div>

        <!-- Ranking content -->
        <div v-loading="isDimensionLoading(dim.id)" class="min-h-[80px]">
          <!-- Empty state -->
          <div
            v-if="getDimensionResults(dim.id).length === 0 && !isDimensionLoading(dim.id)"
            class="py-8 text-center text-gray-400 text-sm"
          >
            (暂无数据)
          </div>

          <!-- Ranking rows -->
          <div v-else>
            <div
              v-for="result in getDimensionResults(dim.id)"
              :key="result.id"
              class="flex items-center gap-4 px-5 py-3 border-b border-gray-100 last:border-b-0 hover:bg-gray-50 transition-colors"
            >
              <!-- Rank number -->
              <div class="w-10 flex items-center justify-center">
                <div class="flex items-center gap-1">
                  <component
                    :is="getRankIcon(result.rankNo)"
                    v-if="getRankIcon(result.rankNo)"
                    class="w-4 h-4"
                    :style="{ color: getRankColor(result.rankNo) }"
                  />
                  <span
                    class="font-bold text-sm"
                    :style="{ color: getRankColor(result.rankNo) }"
                  >
                    #{{ result.rankNo }}
                  </span>
                </div>
              </div>

              <!-- Target name -->
              <div class="flex-1 min-w-0">
                <span class="font-medium text-gray-800 truncate">{{ result.targetName }}</span>
              </div>

              <!-- Score -->
              <div class="w-20 text-right">
                <span
                  class="text-base font-bold"
                  :style="{ color: getGradeColor(result.grade) }"
                >
                  {{ result.score != null ? result.score.toFixed(1) + '分' : '-' }}
                </span>
              </div>

              <!-- Grade -->
              <div class="w-14 text-center">
                <el-tag
                  v-if="result.grade"
                  size="small"
                  round
                  effect="dark"
                  :color="getGradeColor(result.grade)"
                  style="border: none; color: #fff"
                >
                  {{ result.grade }}
                </el-tag>
                <span v-else class="text-gray-300 text-sm">-</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
