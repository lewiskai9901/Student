<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Trophy, Medal } from 'lucide-vue-next'
import { inspRatingDimensionApi } from '@/api/insp/project'
import type { RatingDimension, RatingResult } from '@/types/insp/template'

const route = useRoute()
const router = useRouter()
const dimensionId = Number(route.params.id)

// ========== State ==========
const loading = ref(false)
const dimension = ref<RatingDimension | null>(null)
const rankings = ref<RatingResult[]>([])
const cycleDate = ref('')

// ========== Load ==========
async function loadDimension() {
  try {
    dimension.value = await inspRatingDimensionApi.getById(dimensionId)
  } catch (e: any) {
    ElMessage.error(e.message || '加载维度信息失败')
  }
}

async function loadRankings() {
  loading.value = true
  try {
    rankings.value = await inspRatingDimensionApi.getRankings(dimensionId, cycleDate.value ? { cycleDate: cycleDate.value } : undefined)
  } catch (e: any) {
    ElMessage.error(e.message || '加载排名失败')
  } finally {
    loading.value = false
  }
}

function getRankIcon(rank: number): typeof Trophy | typeof Medal | null {
  if (rank === 1) return Trophy
  if (rank <= 3) return Medal
  return null
}

function getRankColor(rank: number): string {
  if (rank === 1) return '#f59e0b'
  if (rank === 2) return '#94a3b8'
  if (rank === 3) return '#b45309'
  return '#6b7280'
}

function getGradeColor(grade: string | null): string {
  if (!grade) return '#6b7280'
  const map: Record<string, string> = {
    'A+': '#10b981', 'A': '#22c55e', 'B': '#3b82f6', 'C': '#f59e0b', 'D': '#ef4444', 'F': '#dc2626',
  }
  return map[grade] || '#6b7280'
}

function getAwardName(): string {
  return dimension.value?.awardName || ''
}

function goBack() {
  router.back()
}

watch(cycleDate, () => {
  loadRankings()
})

onMounted(() => {
  loadDimension()
  loadRankings()
})
</script>

<template>
  <div class="p-6 max-w-[900px] mx-auto">
    <!-- Header -->
    <div class="flex items-center gap-3 mb-5">
      <el-button link @click="goBack"><ArrowLeft class="w-4 h-4" /></el-button>
      <h2 class="text-xl font-bold text-gray-900">{{ dimension?.dimensionName || '...' }} - 排名</h2>
    </div>

    <!-- Dimension info -->
    <div v-if="dimension" class="rounded-xl bg-gradient-to-r from-amber-50 to-orange-50 border border-amber-200 p-4 mb-5">
      <div class="flex items-center gap-4 text-sm">
        <div>
          <span class="text-xs text-gray-400">维度名称</span>
          <div class="font-semibold text-gray-800">{{ dimension.dimensionName }}</div>
        </div>
        <div class="h-8 w-px bg-amber-200" />
        <div>
          <span class="text-xs text-gray-400">聚合方式</span>
          <div class="font-semibold text-gray-800">{{ dimension.aggregation }}</div>
        </div>
        <div v-if="dimension.awardName" class="h-8 w-px bg-amber-200" />
        <div v-if="dimension.awardName">
          <span class="text-xs text-gray-400">奖项</span>
          <div class="font-semibold text-amber-700">{{ dimension.awardName }}</div>
        </div>
      </div>
    </div>

    <!-- Date picker -->
    <div class="flex items-center gap-3 mb-5">
      <span class="text-sm text-gray-600">周期日期:</span>
      <el-date-picker
        v-model="cycleDate"
        type="date"
        value-format="YYYY-MM-DD"
        placeholder="选择日期（留空显示最新）"
        clearable
        class="!w-48"
      />
    </div>

    <!-- Rankings table -->
    <div v-loading="loading" class="bg-white border border-gray-200 rounded-xl shadow-sm overflow-hidden">
      <div v-if="rankings.length === 0 && !loading" class="py-16 text-center text-gray-400">
        <Trophy class="w-12 h-12 mx-auto mb-3 text-gray-300" />
        <div class="text-sm">暂无排名数据</div>
        <div class="text-xs text-gray-300 mt-1">请先在维度配置中触发「计算」</div>
      </div>
      <el-table v-else :data="rankings" size="default">
        <el-table-column label="排名" width="80" align="center">
          <template #default="{ row }">
            <div class="flex items-center justify-center gap-1">
              <component
                :is="getRankIcon(row.rankNo)"
                v-if="getRankIcon(row.rankNo)"
                class="w-4 h-4"
                :style="{ color: getRankColor(row.rankNo) }"
              />
              <span class="font-bold" :style="{ color: getRankColor(row.rankNo) }">{{ row.rankNo }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="targetName" label="对象名称" min-width="200">
          <template #default="{ row }">
            <div class="flex items-center gap-2">
              <span class="font-medium text-gray-800">{{ row.targetName }}</span>
              <el-tag size="small" type="info" round effect="plain">{{ row.targetType }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="得分" width="120" align="center">
          <template #default="{ row }">
            <span class="text-lg font-bold" :style="{ color: getGradeColor(row.grade) }">{{ row.score?.toFixed(1) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="等级" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.grade" size="small" round effect="dark" :color="getGradeColor(row.grade)" style="border: none; color: #fff">
              {{ row.grade }}
            </el-tag>
            <span v-else class="text-gray-300">-</span>
          </template>
        </el-table-column>
        <el-table-column label="奖项" width="120" align="center" v-if="getAwardName()">
          <template #default="{ row }">
            <span v-if="row.rankNo === 1" class="text-amber-600 font-medium">{{ getAwardName() }}</span>
            <span v-else class="text-gray-300">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="cycleDate" label="周期日期" width="120" />
      </el-table>
    </div>
  </div>
</template>
