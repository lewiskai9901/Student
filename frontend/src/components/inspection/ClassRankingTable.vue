<!--
  班级排名表格组件
  展示班级检查排名数据，支持多维度排序
-->
<template>
  <div class="class-ranking-table">
    <!-- 表头信息 -->
    <div class="flex items-center justify-between mb-4">
      <div class="flex items-center gap-3">
        <Trophy class="h-6 w-6 text-amber-500" />
        <h3 class="text-lg font-semibold text-gray-900">{{ title }}</h3>
        <span class="text-sm text-gray-400">{{ period }}</span>
      </div>

      <div class="flex items-center gap-2">
        <!-- 排序方式 -->
        <el-select v-model="sortBy" size="small" class="w-28" @change="handleSort">
          <el-option label="综合得分" value="totalScore" />
          <el-option label="扣分数" value="deductionScore" />
          <el-option label="加分数" value="bonusScore" />
          <el-option label="整改率" value="correctiveRate" />
        </el-select>

        <!-- 导出按钮 -->
        <el-button size="small" @click="$emit('export')">
          <Download class="h-4 w-4 mr-1" />
          导出
        </el-button>
      </div>
    </div>

    <!-- 表格 -->
    <div class="bg-white rounded-xl border border-gray-200 overflow-hidden">
      <table class="w-full">
        <thead>
          <tr class="bg-gray-50 border-b border-gray-200">
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-600 w-16">排名</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-600">班级</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-600 w-24">
              <span class="cursor-pointer hover:text-gray-900" @click="toggleSort('totalScore')">
                综合得分
                <SortIcon field="totalScore" :current="sortBy" :asc="sortAsc" />
              </span>
            </th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-600 w-20">
              <span class="cursor-pointer hover:text-gray-900" @click="toggleSort('deductionScore')">
                扣分
                <SortIcon field="deductionScore" :current="sortBy" :asc="sortAsc" />
              </span>
            </th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-600 w-20">
              <span class="cursor-pointer hover:text-gray-900" @click="toggleSort('bonusScore')">
                加分
                <SortIcon field="bonusScore" :current="sortBy" :asc="sortAsc" />
              </span>
            </th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-600 w-24">
              <span class="cursor-pointer hover:text-gray-900" @click="toggleSort('correctiveRate')">
                整改率
                <SortIcon field="correctiveRate" :current="sortBy" :asc="sortAsc" />
              </span>
            </th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-600 w-20">评级</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-600 w-16">趋势</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="(item, index) in sortedData"
            :key="item.classId"
            class="border-b border-gray-100 hover:bg-gray-50 transition-colors"
            :class="{ 'bg-amber-50': index < 3 }"
          >
            <!-- 排名 -->
            <td class="px-4 py-3">
              <div class="flex items-center justify-center">
                <div
                  v-if="index < 3"
                  class="w-7 h-7 rounded-full flex items-center justify-center text-white font-bold text-sm"
                  :class="medalColors[index]"
                >
                  {{ index + 1 }}
                </div>
                <span v-else class="text-gray-500 font-medium">{{ index + 1 }}</span>
              </div>
            </td>

            <!-- 班级信息 -->
            <td class="px-4 py-3">
              <div class="flex items-center gap-3">
                <div class="w-8 h-8 rounded-lg bg-blue-100 flex items-center justify-center">
                  <Users class="h-4 w-4 text-blue-600" />
                </div>
                <div>
                  <div class="font-medium text-gray-900">{{ item.className }}</div>
                  <div class="text-xs text-gray-400">{{ item.gradeName }} · {{ item.orgUnitName || item.departmentName }}</div>
                </div>
              </div>
            </td>

            <!-- 综合得分 -->
            <td class="px-4 py-3 text-center">
              <span class="text-lg font-bold" :class="getScoreColor(item.totalScore)">
                {{ item.totalScore.toFixed(1) }}
              </span>
            </td>

            <!-- 扣分 -->
            <td class="px-4 py-3 text-center">
              <span class="text-red-600">-{{ item.deductionScore.toFixed(1) }}</span>
            </td>

            <!-- 加分 -->
            <td class="px-4 py-3 text-center">
              <span class="text-green-600">+{{ item.bonusScore.toFixed(1) }}</span>
            </td>

            <!-- 整改率 -->
            <td class="px-4 py-3 text-center">
              <div class="flex items-center justify-center gap-2">
                <div class="w-16 h-2 bg-gray-200 rounded-full overflow-hidden">
                  <div
                    class="h-full rounded-full transition-all"
                    :class="item.correctiveRate >= 80 ? 'bg-green-500' : item.correctiveRate >= 60 ? 'bg-amber-500' : 'bg-red-500'"
                    :style="{ width: `${item.correctiveRate}%` }"
                  />
                </div>
                <span class="text-sm text-gray-600">{{ item.correctiveRate }}%</span>
              </div>
            </td>

            <!-- 评级 -->
            <td class="px-4 py-3 text-center">
              <span
                class="px-2.5 py-1 rounded-full text-xs font-medium"
                :class="getRatingClass(item.rating)"
              >
                {{ item.rating }}
              </span>
            </td>

            <!-- 趋势 -->
            <td class="px-4 py-3 text-center">
              <div class="flex items-center justify-center">
                <TrendingUp v-if="item.trend > 0" class="h-4 w-4 text-green-500" />
                <TrendingDown v-else-if="item.trend < 0" class="h-4 w-4 text-red-500" />
                <Minus v-else class="h-4 w-4 text-gray-400" />
                <span
                  v-if="item.trend !== 0"
                  class="ml-1 text-xs"
                  :class="item.trend > 0 ? 'text-green-600' : 'text-red-600'"
                >
                  {{ item.trend > 0 ? '+' : '' }}{{ item.trend }}
                </span>
              </div>
            </td>
          </tr>

          <!-- 空状态 -->
          <tr v-if="sortedData.length === 0">
            <td colspan="8" class="px-4 py-12 text-center text-gray-400">
              <BarChart3 class="h-12 w-12 mx-auto mb-3 text-gray-300" />
              <p>暂无排名数据</p>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 底部说明 -->
    <div class="mt-3 flex items-center justify-between text-xs text-gray-400">
      <div class="flex items-center gap-4">
        <span>综合得分 = 基准分 - 扣分 + 加分</span>
        <span>|</span>
        <span>整改率 = 已整改数 / 待整改总数 × 100%</span>
      </div>
      <span>数据更新时间：{{ updateTime }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  Trophy,
  Download,
  Users,
  TrendingUp,
  TrendingDown,
  Minus,
  BarChart3,
  ChevronUp,
  ChevronDown
} from 'lucide-vue-next'

// 类型定义
interface RankingItem {
  classId: number
  className: string
  gradeName: string
  orgUnitName?: string
  /** @deprecated 使用 orgUnitName */
  departmentName?: string
  totalScore: number
  deductionScore: number
  bonusScore: number
  correctiveRate: number
  rating: string
  trend: number  // 与上期相比排名变化，正数表示上升
}

// Props
const props = withDefaults(defineProps<{
  title?: string
  period?: string
  data: RankingItem[]
  updateTime?: string
}>(), {
  title: '班级排名',
  period: '本周',
  updateTime: '-'
})

// Emits
defineEmits<{
  export: []
}>()

// 排序
const sortBy = ref<'totalScore' | 'deductionScore' | 'bonusScore' | 'correctiveRate'>('totalScore')
const sortAsc = ref(false)

const sortedData = computed(() => {
  const data = [...props.data]
  return data.sort((a, b) => {
    const aVal = a[sortBy.value]
    const bVal = b[sortBy.value]
    // 扣分越少越好
    if (sortBy.value === 'deductionScore') {
      return sortAsc.value ? bVal - aVal : aVal - bVal
    }
    // 其他指标越高越好
    return sortAsc.value ? aVal - bVal : bVal - aVal
  })
})

const medalColors = ['bg-amber-400', 'bg-gray-400', 'bg-amber-600']

// 方法
function handleSort() {
  sortAsc.value = false
}

function toggleSort(field: typeof sortBy.value) {
  if (sortBy.value === field) {
    sortAsc.value = !sortAsc.value
  } else {
    sortBy.value = field
    sortAsc.value = false
  }
}

function getScoreColor(score: number): string {
  if (score >= 95) return 'text-green-600'
  if (score >= 85) return 'text-blue-600'
  if (score >= 70) return 'text-amber-600'
  return 'text-red-600'
}

function getRatingClass(rating: string): string {
  const classes: Record<string, string> = {
    '优秀': 'bg-green-100 text-green-700',
    '良好': 'bg-blue-100 text-blue-700',
    '中等': 'bg-amber-100 text-amber-700',
    '及格': 'bg-orange-100 text-orange-700',
    '不及格': 'bg-red-100 text-red-700'
  }
  return classes[rating] || 'bg-gray-100 text-gray-700'
}

// 排序图标组件
const SortIcon = ({ field, current, asc }: { field: string; current: string; asc: boolean }) => {
  if (field !== current) return null
  return asc
    ? h(ChevronUp, { class: 'h-3 w-3 inline-block ml-0.5' })
    : h(ChevronDown, { class: 'h-3 w-3 inline-block ml-0.5' })
}
</script>

<script lang="ts">
import { h } from 'vue'
</script>
