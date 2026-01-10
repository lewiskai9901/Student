<template>
  <div class="p-6">
    <!-- 统计卡片 - 优化版 -->
    <div class="mb-6 grid grid-cols-4 gap-5">
      <!-- 总申诉数卡片 -->
      <div class="group relative overflow-hidden rounded-2xl bg-white p-6 shadow-md ring-1 ring-gray-900/5 transition-all hover:-translate-y-2 hover:shadow-2xl hover:shadow-blue-500/10">
        <!-- 背景装饰 -->
        <div class="absolute -right-8 -top-8 h-32 w-32 rounded-full bg-gradient-to-br from-blue-400/10 to-indigo-400/10 blur-3xl transition-all group-hover:scale-150"></div>

        <div class="relative flex items-start justify-between">
          <div class="flex-1">
            <p class="text-sm font-medium text-gray-500">总申诉数</p>
            <div class="mt-2 flex items-baseline gap-2">
              <p class="text-3xl font-bold text-gray-900">{{ statistics.totalCount }}</p>
              <span class="flex items-center gap-0.5 text-xs font-semibold text-emerald-600">
                <TrendingUp class="h-3 w-3"/> +12%
              </span>
            </div>
            <!-- 进度条 -->
            <div class="mt-4 h-1.5 w-full bg-gray-100 rounded-full overflow-hidden">
              <div class="h-full bg-gradient-to-r from-blue-500 to-indigo-500 rounded-full transition-all duration-500" style="width: 73%"></div>
            </div>
          </div>

          <!-- 图标 - 增强版 -->
          <div class="relative flex h-14 w-14 flex-shrink-0 items-center justify-center">
            <div class="absolute inset-0 rounded-2xl bg-gradient-to-br from-blue-500 to-indigo-600 opacity-10 blur-xl group-hover:opacity-30 transition-opacity"></div>
            <div class="relative flex h-full w-full items-center justify-center rounded-2xl bg-gradient-to-br from-blue-500 to-indigo-600 shadow-lg shadow-blue-500/30">
              <FileText class="h-6 w-6 text-white" />
            </div>
          </div>
        </div>

        <!-- 底部装饰条 - 增强 -->
        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-blue-500 via-indigo-500 to-purple-500 transform scale-x-0 group-hover:scale-x-100 transition-transform origin-left duration-500"></div>
      </div>

      <!-- 待审核卡片 -->
      <div class="group relative overflow-hidden rounded-2xl bg-white p-6 shadow-md ring-1 ring-gray-900/5 transition-all hover:-translate-y-2 hover:shadow-2xl hover:shadow-amber-500/10">
        <div class="absolute -right-8 -top-8 h-32 w-32 rounded-full bg-gradient-to-br from-amber-400/10 to-yellow-400/10 blur-3xl transition-all group-hover:scale-150"></div>

        <div class="relative flex items-start justify-between">
          <div class="flex-1">
            <p class="text-sm font-medium text-gray-500">待审核</p>
            <div class="mt-2 flex items-baseline gap-2">
              <p class="text-3xl font-bold text-gray-900">{{ statistics.pendingCount }}</p>
              <span class="flex items-center gap-0.5 text-xs font-semibold text-amber-600">
                <Clock class="h-3 w-3"/> 处理中
              </span>
            </div>
            <div class="mt-4 h-1.5 w-full bg-gray-100 rounded-full overflow-hidden">
              <div class="h-full bg-gradient-to-r from-amber-500 to-yellow-500 rounded-full transition-all duration-500" style="width: 45%"></div>
            </div>
          </div>

          <div class="relative flex h-14 w-14 flex-shrink-0 items-center justify-center">
            <div class="absolute inset-0 rounded-2xl bg-gradient-to-br from-amber-500 to-yellow-600 opacity-10 blur-xl group-hover:opacity-30 transition-opacity"></div>
            <div class="relative flex h-full w-full items-center justify-center rounded-2xl bg-gradient-to-br from-amber-500 to-yellow-600 shadow-lg shadow-amber-500/30">
              <Clock class="h-6 w-6 text-white" />
            </div>
          </div>
        </div>

        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-amber-500 via-yellow-500 to-orange-500 transform scale-x-0 group-hover:scale-x-100 transition-transform origin-left duration-500"></div>
      </div>

      <!-- 通过率卡片 -->
      <div class="group relative overflow-hidden rounded-2xl bg-white p-6 shadow-md ring-1 ring-gray-900/5 transition-all hover:-translate-y-2 hover:shadow-2xl hover:shadow-green-500/10">
        <div class="absolute -right-8 -top-8 h-32 w-32 rounded-full bg-gradient-to-br from-green-400/10 to-emerald-400/10 blur-3xl transition-all group-hover:scale-150"></div>

        <div class="relative flex items-start justify-between">
          <div class="flex-1">
            <p class="text-sm font-medium text-gray-500">通过率</p>
            <div class="mt-2 flex items-baseline gap-2">
              <p class="text-3xl font-bold text-gray-900">{{ statistics.successRate.toFixed(1) }}%</p>
              <span class="flex items-center gap-0.5 text-xs font-semibold text-green-600">
                <TrendingUp class="h-3 w-3"/> +5.2%
              </span>
            </div>
            <div class="mt-4 h-1.5 w-full bg-gray-100 rounded-full overflow-hidden">
              <div class="h-full bg-gradient-to-r from-green-500 to-emerald-500 rounded-full transition-all duration-500" :style="{width: statistics.successRate + '%'}"></div>
            </div>
          </div>

          <div class="relative flex h-14 w-14 flex-shrink-0 items-center justify-center">
            <div class="absolute inset-0 rounded-2xl bg-gradient-to-br from-green-500 to-emerald-600 opacity-10 blur-xl group-hover:opacity-30 transition-opacity"></div>
            <div class="relative flex h-full w-full items-center justify-center rounded-2xl bg-gradient-to-br from-green-500 to-emerald-600 shadow-lg shadow-green-500/30">
              <CheckCircle class="h-6 w-6 text-white" />
            </div>
          </div>
        </div>

        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-green-500 via-emerald-500 to-teal-500 transform scale-x-0 group-hover:scale-x-100 transition-transform origin-left duration-500"></div>
      </div>

      <!-- 已生效卡片 -->
      <div class="group relative overflow-hidden rounded-2xl bg-white p-6 shadow-md ring-1 ring-gray-900/5 transition-all hover:-translate-y-2 hover:shadow-2xl hover:shadow-purple-500/10">
        <div class="absolute -right-8 -top-8 h-32 w-32 rounded-full bg-gradient-to-br from-purple-400/10 to-violet-400/10 blur-3xl transition-all group-hover:scale-150"></div>

        <div class="relative flex items-start justify-between">
          <div class="flex-1">
            <p class="text-sm font-medium text-gray-500">已生效</p>
            <div class="mt-2 flex items-baseline gap-2">
              <p class="text-3xl font-bold text-gray-900">{{ statistics.effectiveCount }}</p>
              <span class="flex items-center gap-0.5 text-xs font-semibold text-purple-600">
                <CheckSquare class="h-3 w-3"/> 完成
              </span>
            </div>
            <div class="mt-4 h-1.5 w-full bg-gray-100 rounded-full overflow-hidden">
              <div class="h-full bg-gradient-to-r from-purple-500 to-violet-500 rounded-full transition-all duration-500" style="width: 88%"></div>
            </div>
          </div>

          <div class="relative flex h-14 w-14 flex-shrink-0 items-center justify-center">
            <div class="absolute inset-0 rounded-2xl bg-gradient-to-br from-purple-500 to-violet-600 opacity-10 blur-xl group-hover:opacity-30 transition-opacity"></div>
            <div class="relative flex h-full w-full items-center justify-center rounded-2xl bg-gradient-to-br from-purple-500 to-violet-600 shadow-lg shadow-purple-500/30">
              <CheckSquare class="h-6 w-6 text-white" />
            </div>
          </div>
        </div>

        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-purple-500 via-violet-500 to-fuchsia-500 transform scale-x-0 group-hover:scale-x-100 transition-transform origin-left duration-500"></div>
      </div>
    </div>

    <!-- 筛选区域 - 优化 -->
    <div class="mb-6 rounded-2xl bg-white/80 backdrop-blur-sm p-5 shadow-md ring-1 ring-gray-900/5">
      <div class="flex flex-wrap items-center gap-4">
        <div class="flex items-center gap-3">
          <span class="text-sm font-medium text-gray-700">统计范围</span>
          <select
            v-model="queryForm.scope"
            @change="handleQuery"
            class="rounded-xl border-2 border-gray-200 bg-white px-4 py-2.5 text-sm font-medium text-gray-900 transition-all hover:border-blue-300 focus:border-blue-500 focus:outline-none focus:ring-4 focus:ring-blue-500/10"
          >
            <option value="school">🏫 全校</option>
            <option value="grade">📚 年级</option>
            <option value="class">👥 班级</option>
          </select>
        </div>

        <div v-if="queryForm.scope === 'grade'" class="flex items-center gap-3">
          <span class="text-sm font-medium text-gray-700">年级</span>
          <select
            v-model="queryForm.scopeId"
            @change="handleQuery"
            class="rounded-xl border-2 border-gray-200 bg-white px-4 py-2.5 text-sm font-medium text-gray-900 transition-all hover:border-blue-300 focus:border-blue-500 focus:outline-none focus:ring-4 focus:ring-blue-500/10"
          >
            <option v-for="grade in gradeList" :key="grade.id" :value="grade.id">
              {{ grade.gradeName }}
            </option>
          </select>
        </div>

        <div class="flex items-center gap-3">
          <span class="text-sm font-medium text-gray-700">统计周期</span>
          <select
            v-model="queryForm.period"
            @change="handleQuery"
            class="rounded-xl border-2 border-gray-200 bg-white px-4 py-2.5 text-sm font-medium text-gray-900 transition-all hover:border-blue-300 focus:border-blue-500 focus:outline-none focus:ring-4 focus:ring-blue-500/10"
          >
            <option value="today">📅 今日</option>
            <option value="week">🗓️ 本周</option>
            <option value="month">📆 本月</option>
            <option value="semester">📖 本学期</option>
          </select>
        </div>

        <button
          @click="handleQuery"
          class="group flex items-center gap-2 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 px-5 py-2.5 text-sm font-semibold text-white shadow-lg shadow-blue-500/30 transition-all hover:shadow-xl hover:shadow-blue-500/40 hover:-translate-y-0.5"
        >
          <Search class="h-4 w-4 transition-transform group-hover:scale-110" />
          查询统计
        </button>
      </div>
    </div>

    <!-- 图表区域 - 优化 -->
    <div class="grid grid-cols-2 gap-6">
      <!-- 状态分布图 -->
      <div class="rounded-2xl bg-white p-6 shadow-md ring-1 ring-gray-900/5">
        <h3 class="mb-4 flex items-center gap-2 text-lg font-semibold text-gray-900">
          <PieChart class="h-5 w-5 text-emerald-600" />
          申诉状态分布
        </h3>
        <div ref="statusChartRef" class="h-80"></div>
      </div>

      <!-- 趋势图 -->
      <div class="rounded-xl bg-white p-6 shadow-sm">
        <h3 class="mb-4 flex items-center gap-2 text-lg font-semibold text-gray-900">
          <TrendingUp class="h-5 w-5 text-emerald-600" />
          申诉趋势
        </h3>
        <div ref="trendChartRef" class="h-80"></div>
      </div>
    </div>

    <!-- 详细统计 -->
    <div class="mt-6 rounded-xl bg-white p-6 shadow-sm">
      <h3 class="mb-4 flex items-center gap-2 text-lg font-semibold text-gray-900">
        <BarChart2 class="h-5 w-5 text-emerald-600" />
        详细统计
      </h3>
      <div class="grid grid-cols-5 gap-4">
        <div class="rounded-xl bg-amber-50 p-4 text-center">
          <p class="text-sm text-amber-600">待审核</p>
          <p class="mt-1 text-2xl font-bold text-amber-700">{{ statistics.pendingCount }}</p>
        </div>
        <div class="rounded-xl bg-green-50 p-4 text-center">
          <p class="text-sm text-green-600">已通过</p>
          <p class="mt-1 text-2xl font-bold text-green-700">{{ statistics.approvedCount }}</p>
        </div>
        <div class="rounded-xl bg-red-50 p-4 text-center">
          <p class="text-sm text-red-600">已驳回</p>
          <p class="mt-1 text-2xl font-bold text-red-700">{{ statistics.rejectedCount }}</p>
        </div>
        <div class="rounded-xl bg-blue-50 p-4 text-center">
          <p class="text-sm text-blue-600">公示中</p>
          <p class="mt-1 text-2xl font-bold text-blue-700">{{ statistics.publicityCount }}</p>
        </div>
        <div class="rounded-xl bg-purple-50 p-4 text-center">
          <p class="text-sm text-purple-600">已生效</p>
          <p class="mt-1 text-2xl font-bold text-purple-700">{{ statistics.effectiveCount }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { getAppealStatistics } from '@/api/v2/appeal'
import { getAllGrades } from '@/api/v2/organization'
import { ElMessage } from 'element-plus'
import {
  FileText,
  Clock,
  CheckCircle,
  CheckSquare,
  Search,
  PieChart,
  TrendingUp,
  BarChart2
} from 'lucide-vue-next'
// ECharts 按需导入
import { use } from 'echarts/core'
import { PieChart as EchartsPie, LineChart } from 'echarts/charts'
import { CanvasRenderer } from 'echarts/renderers'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import type { ECharts } from 'echarts/core'

use([EchartsPie, LineChart, CanvasRenderer, GridComponent, TooltipComponent, LegendComponent])
import * as echarts from 'echarts/core'

const statistics = ref({
  totalCount: 0,
  pendingCount: 0,
  approvedCount: 0,
  rejectedCount: 0,
  effectiveCount: 0,
  publicityCount: 0,
  successRate: 0
})

const queryForm = reactive({
  scope: 'school',
  scopeId: 0,
  period: 'month'
})

const gradeList = ref<any[]>([])
const statusChartRef = ref<HTMLElement>()
const trendChartRef = ref<HTMLElement>()
let statusChart: ECharts | null = null
let trendChart: ECharts | null = null

// 加载统计数据
const loadStatistics = async () => {
  try {
    const res = await getAppealStatistics(
      queryForm.scope,
      queryForm.scopeId,
      queryForm.period
    )
    statistics.value = res || {
      totalCount: 0,
      pendingCount: 0,
      approvedCount: 0,
      rejectedCount: 0,
      effectiveCount: 0,
      publicityCount: 0,
      successRate: 0
    }

    renderStatusChart()
    renderTrendChart()
  } catch (error) {
    console.error('加载统计数据失败:', error)
    ElMessage.error('加载统计数据失败')
  }
}

// 加载年级列表
const loadGrades = async () => {
  try {
    const res = await getAllGrades()
    gradeList.value = res || []
  } catch (error) {
    console.error('加载年级列表失败:', error)
  }
}

// 渲染状态分布图
const renderStatusChart = () => {
  if (!statusChartRef.value) return

  if (!statusChart) {
    statusChart = echarts.init(statusChartRef.value)
  }

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      top: 'center'
    },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['60%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 18,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: [
          { value: statistics.value.pendingCount, name: '待审核', itemStyle: { color: '#F59E0B' } },
          { value: statistics.value.approvedCount, name: '通过', itemStyle: { color: '#10B981' } },
          { value: statistics.value.rejectedCount, name: '驳回', itemStyle: { color: '#EF4444' } },
          { value: statistics.value.publicityCount, name: '公示中', itemStyle: { color: '#3B82F6' } },
          { value: statistics.value.effectiveCount, name: '已生效', itemStyle: { color: '#8B5CF6' } }
        ]
      }
    ]
  }

  statusChart.setOption(option)
}

// 渲染趋势图
const renderTrendChart = () => {
  if (!trendChartRef.value) return

  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }

  const option = {
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['提交', '通过', '驳回'],
      bottom: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '提交',
        type: 'line',
        data: [5, 8, 12, 7, 10, 6, 4],
        smooth: true,
        itemStyle: { color: '#3B82F6' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(59, 130, 246, 0.3)' },
            { offset: 1, color: 'rgba(59, 130, 246, 0.05)' }
          ])
        }
      },
      {
        name: '通过',
        type: 'line',
        data: [3, 6, 9, 5, 7, 4, 3],
        smooth: true,
        itemStyle: { color: '#10B981' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(16, 185, 129, 0.3)' },
            { offset: 1, color: 'rgba(16, 185, 129, 0.05)' }
          ])
        }
      },
      {
        name: '驳回',
        type: 'line',
        data: [2, 2, 3, 2, 3, 2, 1],
        smooth: true,
        itemStyle: { color: '#EF4444' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(239, 68, 68, 0.3)' },
            { offset: 1, color: 'rgba(239, 68, 68, 0.05)' }
          ])
        }
      }
    ]
  }

  trendChart.setOption(option)
}

// 查询
const handleQuery = () => {
  if (queryForm.scope !== 'grade') {
    queryForm.scopeId = 0
  }
  loadStatistics()
}

// 窗口大小改变时重新渲染图表
const handleResize = () => {
  statusChart?.resize()
  trendChart?.resize()
}

onMounted(() => {
  loadGrades()
  loadStatistics()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  statusChart?.dispose()
  trendChart?.dispose()
})
</script>

<style scoped>
</style>
