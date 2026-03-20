<template>
  <div class="analytics-dashboard">
    <!-- Filters -->
    <el-card shadow="never" class="mb-4">
      <div class="flex items-center gap-4 flex-wrap">
        <div class="flex items-center gap-2">
          <span class="text-sm text-gray-500">项目</span>
          <el-select v-model="filters.projectId" placeholder="选择项目" class="w-52" @change="onProjectChange">
            <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
        </div>
        <div class="flex items-center gap-2">
          <span class="text-sm text-gray-500">日期</span>
          <el-date-picker v-model="filters.date" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" class="w-40" />
        </div>
        <div class="flex items-center gap-2">
          <span class="text-sm text-gray-500">周期</span>
          <el-select v-model="filters.periodType" class="w-28">
            <el-option v-for="(cfg, key) in PeriodTypeConfig" :key="key" :label="cfg.label" :value="key" />
          </el-select>
        </div>
        <el-button type="primary" :icon="Refresh" @click="loadDashboard">刷新</el-button>
      </div>
    </el-card>

    <!-- Stats Bar -->
    <el-card shadow="never" class="mb-4" v-if="correctiveSummary">
      <div class="flex items-center gap-6 text-sm">
        <span>整改总数 <b>{{ correctiveSummary.total }}</b></span>
        <el-divider direction="vertical" />
        <span>待分配 <b class="text-gray-500">{{ correctiveSummary.open }}</b></span>
        <el-divider direction="vertical" />
        <span>整改中 <b class="text-warning">{{ correctiveSummary.inProgress }}</b></span>
        <el-divider direction="vertical" />
        <span>已验证 <b class="text-success">{{ correctiveSummary.verified }}</b></span>
        <el-divider direction="vertical" />
        <span>逾期 <b class="text-danger">{{ correctiveSummary.overdue }}</b></span>
        <el-divider direction="vertical" />
        <span>紧急 <b class="text-danger">{{ correctiveSummary.critical }}</b></span>
      </div>
    </el-card>

    <!-- Top Row: Ranking + Trend -->
    <div class="grid grid-cols-1 xl:grid-cols-2 gap-4 mb-4">
      <!-- Daily Ranking Table -->
      <el-card shadow="never">
        <template #header>
          <div class="flex items-center justify-between">
            <span class="font-medium">日排名</span>
            <span class="text-xs text-gray-400">{{ filters.date }}</span>
          </div>
        </template>
        <el-table :data="store.dailyRanking" size="small" max-height="360" v-loading="store.loading">
          <el-table-column label="排名" width="60" align="center">
            <template #default="{ row }">
              <span :class="rankingClass(row.ranking)">{{ row.ranking }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="targetName" label="检查目标" min-width="120" show-overflow-tooltip />
          <el-table-column prop="orgUnitName" label="所属组织" min-width="100" show-overflow-tooltip />
          <el-table-column label="平均分" width="80" align="right">
            <template #default="{ row }">
              <span :class="scoreClass(row.avgScore)">{{ row.avgScore?.toFixed(1) ?? '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="inspectionCount" label="检查次数" width="80" align="center" />
          <el-table-column label="通过率" width="80" align="right">
            <template #default="{ row }">
              {{ passRate(row) }}
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- Score Trend Chart -->
      <el-card shadow="never">
        <template #header>
          <div class="flex items-center justify-between">
            <span class="font-medium">分数趋势</span>
            <div class="flex items-center gap-2">
              <el-date-picker v-model="trendRange" type="daterange" value-format="YYYY-MM-DD"
                start-placeholder="开始" end-placeholder="结束" size="small" class="w-56" @change="loadTrend" />
            </div>
          </div>
        </template>
        <div ref="trendChartRef" class="chart-container" />
      </el-card>
    </div>

    <!-- Bottom Row: Comparison + Period + Corrective -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-4">
      <!-- Comparison Bar Chart -->
      <el-card shadow="never">
        <template #header>
          <span class="font-medium">得分对比</span>
        </template>
        <div ref="comparisonChartRef" class="chart-container" />
      </el-card>

      <!-- Period Summary Table -->
      <el-card shadow="never">
        <template #header>
          <div class="flex items-center justify-between">
            <span class="font-medium">周期汇总</span>
            <el-tag v-if="filters.periodType" size="small" type="info">
              {{ PeriodTypeConfig[filters.periodType]?.label }}
            </el-tag>
          </div>
        </template>
        <el-table :data="store.periodSummary" size="small" max-height="300" v-loading="store.loading">
          <el-table-column prop="targetName" label="目标" min-width="100" show-overflow-tooltip />
          <el-table-column label="均分" width="65" align="right">
            <template #default="{ row }">
              <span :class="scoreClass(row.avgScore)">{{ row.avgScore?.toFixed(1) ?? '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="趋势" width="60" align="center">
            <template #default="{ row }">
              <span v-if="row.trendDirection === 'UP'" class="text-success">↑</span>
              <span v-else-if="row.trendDirection === 'DOWN'" class="text-danger">↓</span>
              <span v-else class="text-gray-400">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="inspectionDays" label="检查天数" width="75" align="center" />
          <el-table-column prop="ranking" label="排名" width="55" align="center" />
        </el-table>
      </el-card>

      <!-- Corrective Pie Chart -->
      <el-card shadow="never">
        <template #header>
          <span class="font-medium">整改状态分布</span>
        </template>
        <div ref="correctiveChartRef" class="chart-container" />
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { use } from 'echarts/core'
import { LineChart, BarChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { ECharts, EChartsOption } from 'echarts/core'
import * as echarts from 'echarts/core'
import { useInspAnalyticsStore } from '@/stores/insp/inspAnalyticsStore'
import { useInspExecutionStore } from '@/stores/insp/inspExecutionStore'
import { PeriodTypeConfig } from '@/types/insp/enums'
import type { PeriodType } from '@/types/insp/enums'
import type { DailySummary } from '@/types/insp/analytics'
import { CaseStatusConfig } from '@/types/insp/enums'

use([LineChart, BarChart, PieChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const store = useInspAnalyticsStore()
const executionStore = useInspExecutionStore()

const projects = ref<{ id: number; projectName: string }[]>([])

const filters = reactive({
  projectId: null as number | null,
  date: new Date().toISOString().slice(0, 10),
  periodType: 'WEEKLY' as PeriodType,
})

const trendRange = ref<[string, string] | null>(null)

// Chart refs
const trendChartRef = ref<HTMLElement | null>(null)
const comparisonChartRef = ref<HTMLElement | null>(null)
const correctiveChartRef = ref<HTMLElement | null>(null)
let trendChart: ECharts | null = null
let comparisonChart: ECharts | null = null
let correctiveChart: ECharts | null = null

// ========== Lifecycle ==========

onMounted(async () => {
  await loadProjects()
  initCharts()
  window.addEventListener('resize', handleResize)

  // Set default trend range: last 14 days
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - 14)
  trendRange.value = [start.toISOString().slice(0, 10), end.toISOString().slice(0, 10)]

  if (filters.projectId) {
    await loadDashboard()
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  comparisonChart?.dispose()
  correctiveChart?.dispose()
})

// ========== Data Loading ==========

async function loadProjects() {
  try {
    await executionStore.loadProjects()
    projects.value = (executionStore.projects || []).map((p: any) => ({ id: p.id, projectName: p.projectName }))
    if (projects.value.length > 0 && !filters.projectId) {
      filters.projectId = projects.value[0].id
    }
  } catch {
    projects.value = []
  }
}

async function onProjectChange() {
  await loadDashboard()
}

async function loadDashboard() {
  if (!filters.projectId || !filters.date) return

  await Promise.all([
    store.fetchDailyRanking(filters.projectId, filters.date),
    store.fetchComparison(filters.projectId, filters.date),
    store.fetchPeriodSummary(filters.projectId, filters.periodType, calculatePeriodStart(filters.periodType, filters.date)),
    store.fetchCorrectiveSummary(filters.projectId),
    loadTrend(),
  ])

  await nextTick()
  updateTrendChart()
  updateComparisonChart()
  updateCorrectiveChart()
}

async function loadTrend() {
  if (!filters.projectId || !trendRange.value) return
  // Load trend for the top-ranked target, or all
  const ranking = store.dailyRanking
  if (ranking.length > 0 && ranking[0].targetType && ranking[0].targetId) {
    await store.fetchTrend(filters.projectId, ranking[0].targetType, ranking[0].targetId, trendRange.value[0], trendRange.value[1])
  } else {
    await store.fetchDimensionBreakdown(filters.projectId, trendRange.value[0], trendRange.value[1])
    store.trendData = store.dimensionBreakdown
  }
  await nextTick()
  updateTrendChart()
}

const correctiveSummary = ref(store.correctiveSummary)
watch(() => store.correctiveSummary, (v) => { correctiveSummary.value = v })

// ========== Charts ==========

function initCharts() {
  if (trendChartRef.value) trendChart = echarts.init(trendChartRef.value)
  if (comparisonChartRef.value) comparisonChart = echarts.init(comparisonChartRef.value)
  if (correctiveChartRef.value) correctiveChart = echarts.init(correctiveChartRef.value)
}

function updateTrendChart() {
  if (!trendChart) return
  const data = store.trendData
  if (!data.length) { trendChart.clear(); return }

  const dates = [...new Set(data.map(d => d.summaryDate))].sort()
  const scores = dates.map(date => {
    const items = data.filter(d => d.summaryDate === date)
    const avg = items.reduce((s, i) => s + (i.avgScore ?? 0), 0) / (items.length || 1)
    return +avg.toFixed(1)
  })

  const option: EChartsOption = {
    grid: { top: 20, right: 20, bottom: 30, left: 50 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: dates.map(d => d.slice(5)), axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', min: Math.max(0, Math.floor(Math.min(...scores) * 0.95)) },
    series: [{
      type: 'line',
      data: scores,
      smooth: true,
      itemStyle: { color: '#409EFF' },
      areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
        { offset: 0, color: 'rgba(64,158,255,0.3)' },
        { offset: 1, color: 'rgba(64,158,255,0.02)' },
      ])},
    }],
  }
  trendChart.setOption(option, true)
}

function updateComparisonChart() {
  if (!comparisonChart) return
  const data = store.comparisonData
  if (!data.length) { comparisonChart.clear(); return }

  const sorted = [...data].sort((a, b) => (b.avgScore ?? 0) - (a.avgScore ?? 0)).slice(0, 10)
  const names = sorted.map(d => d.targetName ?? '未命名')
  const scores = sorted.map(d => d.avgScore ?? 0)

  const option: EChartsOption = {
    grid: { top: 10, right: 20, bottom: 30, left: 80 },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: { type: 'value', min: Math.max(0, Math.floor(Math.min(...scores) * 0.9)) },
    yAxis: { type: 'category', data: names.reverse(), axisLabel: { fontSize: 11, width: 70, overflow: 'truncate' } },
    series: [{
      type: 'bar',
      data: scores.reverse(),
      itemStyle: {
        color: (params: any) => {
          const v = params.value
          if (v >= 90) return '#67C23A'
          if (v >= 80) return '#409EFF'
          if (v >= 60) return '#E6A23C'
          return '#F56C6C'
        },
      },
      barMaxWidth: 20,
    }],
  }
  comparisonChart.setOption(option, true)
}

function updateCorrectiveChart() {
  if (!correctiveChart) return
  const s = store.correctiveSummary
  if (!s) { correctiveChart.clear(); return }

  const statusData = [
    { name: '待分配', value: s.open, color: CaseStatusConfig.OPEN.color },
    { name: '已分配', value: s.assigned, color: CaseStatusConfig.ASSIGNED.color },
    { name: '整改中', value: s.inProgress, color: CaseStatusConfig.IN_PROGRESS.color },
    { name: '已提交', value: s.submitted, color: CaseStatusConfig.SUBMITTED.color },
    { name: '已验证', value: s.verified, color: CaseStatusConfig.VERIFIED.color },
    { name: '已驳回', value: s.rejected, color: CaseStatusConfig.REJECTED.color },
    { name: '已关闭', value: s.closed, color: CaseStatusConfig.CLOSED.color },
    { name: '已升级', value: s.escalated, color: CaseStatusConfig.ESCALATED.color },
  ].filter(d => d.value > 0)

  const option: EChartsOption = {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['50%', '55%'],
      data: statusData.map(d => ({ name: d.name, value: d.value, itemStyle: { color: d.color } })),
      label: { fontSize: 11 },
      emphasis: { itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.1)' } },
    }],
  }
  correctiveChart.setOption(option, true)
}

function handleResize() {
  trendChart?.resize()
  comparisonChart?.resize()
  correctiveChart?.resize()
}

// ========== Helpers ==========

function calculatePeriodStart(periodType: PeriodType, date: string): string {
  const d = new Date(date)
  switch (periodType) {
    case 'WEEKLY': {
      const day = d.getDay()
      d.setDate(d.getDate() - (day === 0 ? 6 : day - 1))
      return d.toISOString().slice(0, 10)
    }
    case 'MONTHLY':
      return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-01`
    case 'QUARTERLY': {
      const q = Math.floor(d.getMonth() / 3)
      return `${d.getFullYear()}-${String(q * 3 + 1).padStart(2, '0')}-01`
    }
    case 'YEARLY':
      return `${d.getFullYear()}-01-01`
  }
}

function rankingClass(ranking: number | null): string {
  if (!ranking) return ''
  if (ranking === 1) return 'font-bold text-warning'
  if (ranking === 2) return 'font-bold text-gray-500'
  if (ranking === 3) return 'font-bold text-amber-700'
  return ''
}

function scoreClass(score: number | null): string {
  if (score == null) return 'text-gray-400'
  if (score >= 90) return 'text-success font-medium'
  if (score >= 80) return 'text-primary'
  if (score >= 60) return 'text-warning'
  return 'text-danger font-medium'
}

function passRate(row: DailySummary): string {
  const total = row.passCount + row.failCount
  if (total === 0) return '-'
  return ((row.passCount / total) * 100).toFixed(0) + '%'
}
</script>

<style scoped>
.analytics-dashboard {
  padding: 0;
}

.chart-container {
  width: 100%;
  height: 280px;
}

.text-success { color: #67C23A; }
.text-warning { color: #E6A23C; }
.text-danger { color: #F56C6C; }
.text-primary { color: #409EFF; }
</style>
