<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  BarChart3, TrendingUp, TrendingDown, PieChart, AlertTriangle,
  Calendar, Building2, Filter, Download, ChevronDown, Info,
  Target, CheckCircle2, ArrowUpRight, ArrowDownRight, Minus
} from 'lucide-vue-next'
import { ElSelect, ElOption, ElProgress } from 'element-plus'

// ─── Types ───
interface KPICard {
  label: string
  value: string
  unit: string
  diff: number
  diffLabel: string
  color: string
}

interface WeeklyTrend {
  week: string
  schoolAvg: number
  departments: { name: string; value: number; color: string }[]
}

interface CategoryDistribution {
  name: string
  count: number
  percentage: number
  color: string
}

interface TopIssue {
  rank: number
  name: string
  category: string
  count: number
  totalDeduction: number
  trend: 'up' | 'down' | 'stable'
  trendPercent: number
}

interface DepartmentRanking {
  rank: number
  name: string
  classAvgScore: number
  departmentScore: number
  compositeScore: number
  level: string
  levelColor: string
  trend: 'up' | 'down' | 'stable'
}

interface AlertItem {
  type: 'warning' | 'danger' | 'info'
  icon: 'alert' | 'target' | 'info'
  message: string
}

// ─── State ───
const timeRange = ref('month')
const scope = ref('school')
const compareWith = ref('lastMonth')

// ─── Mock Data ───
const kpiCards: KPICard[] = [
  { label: '检查覆盖率', value: '95.2', unit: '%', diff: 2.1, diffLabel: '较上月', color: 'emerald' },
  { label: '平均扣分', value: '12.3', unit: '分', diff: -1.5, diffLabel: '较上月', color: 'blue' },
  { label: '整改完成率', value: '78.5', unit: '%', diff: 5.2, diffLabel: '较上月', color: 'amber' },
  { label: '优秀率', value: '35.7', unit: '%', diff: 3.1, diffLabel: '较上月', color: 'violet' },
]

const weeklyTrends: WeeklyTrend[] = [
  { week: '第1周', schoolAvg: 18.5, departments: [{ name: '信工系', value: 15.2, color: '#3b82f6' }, { name: '经管系', value: 20.1, color: '#f59e0b' }, { name: '财经系', value: 19.8, color: '#10b981' }] },
  { week: '第2周', schoolAvg: 16.2, departments: [{ name: '信工系', value: 13.5, color: '#3b82f6' }, { name: '经管系', value: 18.5, color: '#f59e0b' }, { name: '财经系', value: 16.0, color: '#10b981' }] },
  { week: '第3周', schoolAvg: 14.8, departments: [{ name: '信工系', value: 11.2, color: '#3b82f6' }, { name: '经管系', value: 17.0, color: '#f59e0b' }, { name: '财经系', value: 15.5, color: '#10b981' }] },
  { week: '第4周', schoolAvg: 12.3, departments: [{ name: '信工系', value: 9.8, color: '#3b82f6' }, { name: '经管系', value: 14.2, color: '#f59e0b' }, { name: '财经系', value: 12.5, color: '#10b981' }] },
]

const categoryDistribution: CategoryDistribution[] = [
  { name: '卫生', count: 156, percentage: 45, color: '#3b82f6' },
  { name: '纪律', count: 87, percentage: 25, color: '#f59e0b' },
  { name: '考勤', count: 52, percentage: 15, color: '#10b981' },
  { name: '宿舍', count: 35, percentage: 10, color: '#8b5cf6' },
  { name: '其他', count: 17, percentage: 5, color: '#6b7280' },
]

const topIssues: TopIssue[] = [
  { rank: 1, name: '地面不洁', category: '卫生', count: 23, totalDeduction: 46.0, trend: 'down', trendPercent: 12 },
  { rank: 2, name: '迟到', category: '考勤', count: 18, totalDeduction: 36.0, trend: 'down', trendPercent: 8 },
  { rank: 3, name: '物品乱放', category: '卫生', count: 12, totalDeduction: 12.0, trend: 'stable', trendPercent: 0 },
  { rank: 4, name: '被褥未叠', category: '卫生', count: 10, totalDeduction: 5.0, trend: 'down', trendPercent: 25 },
  { rank: 5, name: '课间打闹', category: '纪律', count: 8, totalDeduction: 16.0, trend: 'up', trendPercent: 15 },
  { rank: 6, name: '违规电器', category: '安全', count: 5, totalDeduction: 25.0, trend: 'up', trendPercent: 40 },
  { rank: 7, name: '窗户未关', category: '安全', count: 4, totalDeduction: 4.0, trend: 'down', trendPercent: 50 },
  { rank: 8, name: '课堂玩手机', category: '纪律', count: 3, totalDeduction: 9.0, trend: 'stable', trendPercent: 0 },
  { rank: 9, name: '垃圾未倒', category: '卫生', count: 3, totalDeduction: 3.0, trend: 'down', trendPercent: 33 },
  { rank: 10, name: '早退', category: '考勤', count: 2, totalDeduction: 4.0, trend: 'up', trendPercent: 100 },
]

const departmentRankings: DepartmentRanking[] = [
  { rank: 1, name: '信息工程系', classAvgScore: 92.5, departmentScore: 95.0, compositeScore: 93.3, level: '优秀', levelColor: 'emerald', trend: 'up' },
  { rank: 2, name: '财经系', classAvgScore: 88.2, departmentScore: 90.0, compositeScore: 88.8, level: '良好', levelColor: 'blue', trend: 'stable' },
  { rank: 3, name: '经济管理系', classAvgScore: 85.1, departmentScore: 82.0, compositeScore: 84.1, level: '良好', levelColor: 'blue', trend: 'down' },
]

const alerts: AlertItem[] = [
  { type: 'danger', icon: 'alert', message: '物流3班连续3周排名末位，建议重点关注' },
  { type: 'warning', icon: 'alert', message: '宿舍卫生类扣分本月上升23%，建议加强检查频次' },
  { type: 'warning', icon: 'target', message: '2号宿舍楼3层检查覆盖率仅60%，存在检查盲区' },
  { type: 'info', icon: 'info', message: '整改工单超期5项，涉及3个班级，请及时跟进' },
]

const totalIssueCount = computed(() => topIssues.reduce((sum, i) => sum + i.count, 0))

// ─── Chart Helpers ───
const chartMaxY = 25
const chartHeight = 200
const chartWidth = 500
const chartPadding = { top: 20, right: 20, bottom: 30, left: 40 }
const plotWidth = chartWidth - chartPadding.left - chartPadding.right
const plotHeight = chartHeight - chartPadding.top - chartPadding.bottom

function yToPixel(value: number): number {
  return chartPadding.top + plotHeight - (value / chartMaxY) * plotHeight
}

function xToPixel(index: number): number {
  return chartPadding.left + (index / (weeklyTrends.length - 1)) * plotWidth
}

function buildLinePath(values: number[]): string {
  return values.map((v, i) => `${i === 0 ? 'M' : 'L'}${xToPixel(i)},${yToPixel(v)}`).join(' ')
}

const schoolAvgPath = computed(() => buildLinePath(weeklyTrends.map(w => w.schoolAvg)))

function departmentPath(deptName: string): string {
  return buildLinePath(weeklyTrends.map(w => w.departments.find(d => d.name === deptName)?.value ?? 0))
}
</script>

<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <!-- Header -->
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-gray-900 mb-1">数据分析中心</h1>
      <p class="text-sm text-gray-500">多维度量化数据分析与趋势洞察</p>
    </div>

    <!-- Filters -->
    <div class="flex items-center gap-3 mb-6 bg-white rounded-xl px-4 py-3 shadow-sm border border-gray-100">
      <Filter class="w-4 h-4 text-gray-400" />
      <div class="flex items-center gap-2">
        <span class="text-sm text-gray-500">时间范围:</span>
        <ElSelect v-model="timeRange" size="small" style="width: 120px">
          <ElOption value="week" label="本周" />
          <ElOption value="month" label="本月" />
          <ElOption value="semester" label="本学期" />
          <ElOption value="custom" label="自定义" />
        </ElSelect>
      </div>
      <div class="w-px h-5 bg-gray-200" />
      <div class="flex items-center gap-2">
        <span class="text-sm text-gray-500">范围:</span>
        <ElSelect v-model="scope" size="small" style="width: 120px">
          <ElOption value="school" label="全校" />
          <ElOption value="dept-1" label="信息工程系" />
          <ElOption value="dept-2" label="经济管理系" />
          <ElOption value="dept-3" label="财经系" />
        </ElSelect>
      </div>
      <div class="w-px h-5 bg-gray-200" />
      <div class="flex items-center gap-2">
        <span class="text-sm text-gray-500">对比:</span>
        <ElSelect v-model="compareWith" size="small" style="width: 120px">
          <ElOption value="lastMonth" label="上月" />
          <ElOption value="lastWeek" label="上周" />
          <ElOption value="lastSemester" label="上学期" />
        </ElSelect>
      </div>
      <div class="flex-1" />
      <button class="flex items-center gap-1.5 text-sm text-gray-600 hover:text-gray-900 transition-colors">
        <Download class="w-4 h-4" />
        导出报告
      </button>
    </div>

    <!-- KPI Cards -->
    <div class="grid grid-cols-4 gap-4 mb-6">
      <div
        v-for="kpi in kpiCards"
        :key="kpi.label"
        class="bg-white rounded-xl p-5 shadow-sm border border-gray-100"
      >
        <div class="text-sm text-gray-500 mb-2">{{ kpi.label }}</div>
        <div class="flex items-end gap-1 mb-2">
          <span class="text-3xl font-bold text-gray-900">{{ kpi.value }}</span>
          <span class="text-sm text-gray-400 mb-1">{{ kpi.unit }}</span>
        </div>
        <div class="flex items-center gap-1">
          <component
            :is="kpi.diff > 0 ? (kpi.label === '平均扣分' ? ArrowDownRight : ArrowUpRight) : (kpi.label === '平均扣分' ? ArrowUpRight : ArrowDownRight)"
            class="w-3.5 h-3.5"
            :class="[
              (kpi.label === '平均扣分' ? kpi.diff < 0 : kpi.diff > 0) ? 'text-emerald-500' : 'text-red-500'
            ]"
          />
          <span
            class="text-xs font-medium"
            :class="[
              (kpi.label === '平均扣分' ? kpi.diff < 0 : kpi.diff > 0) ? 'text-emerald-600' : 'text-red-600'
            ]"
          >
            {{ Math.abs(kpi.diff) }}{{ kpi.unit === '分' ? '分' : '%' }}
          </span>
          <span class="text-xs text-gray-400">{{ kpi.diffLabel }}</span>
        </div>
      </div>
    </div>

    <!-- Charts Row -->
    <div class="grid grid-cols-2 gap-4 mb-6">
      <!-- Trend Chart -->
      <div class="bg-white rounded-xl p-5 shadow-sm border border-gray-100">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-sm font-semibold text-gray-800">扣分趋势对比</h3>
          <div class="flex items-center gap-4 text-xs">
            <span class="flex items-center gap-1.5">
              <span class="w-3 h-0.5 bg-gray-900 inline-block rounded" />
              全校平均
            </span>
            <span class="flex items-center gap-1.5">
              <span class="w-3 h-0.5 bg-blue-500 inline-block rounded" />
              信工系
            </span>
            <span class="flex items-center gap-1.5">
              <span class="w-3 h-0.5 bg-amber-500 inline-block rounded" />
              经管系
            </span>
            <span class="flex items-center gap-1.5">
              <span class="w-3 h-0.5 bg-emerald-500 inline-block rounded" />
              财经系
            </span>
          </div>
        </div>
        <svg :viewBox="`0 0 ${chartWidth} ${chartHeight}`" class="w-full" style="max-height: 220px">
          <!-- Y axis grid -->
          <line v-for="y in [0, 5, 10, 15, 20, 25]" :key="y"
            :x1="chartPadding.left" :y1="yToPixel(y)"
            :x2="chartWidth - chartPadding.right" :y2="yToPixel(y)"
            stroke="#f3f4f6" stroke-width="1"
          />
          <!-- Y axis labels -->
          <text v-for="y in [0, 5, 10, 15, 20, 25]" :key="'yl'+y"
            :x="chartPadding.left - 8" :y="yToPixel(y) + 4"
            text-anchor="end" fill="#9ca3af" font-size="10"
          >{{ y }}</text>
          <!-- X axis labels -->
          <text v-for="(w, i) in weeklyTrends" :key="'xl'+i"
            :x="xToPixel(i)" :y="chartHeight - 5"
            text-anchor="middle" fill="#9ca3af" font-size="10"
          >{{ w.week }}</text>
          <!-- School avg line -->
          <path :d="schoolAvgPath" fill="none" stroke="#111827" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" />
          <!-- Department lines -->
          <path :d="departmentPath('信工系')" fill="none" stroke="#3b82f6" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" stroke-dasharray="6,3" />
          <path :d="departmentPath('经管系')" fill="none" stroke="#f59e0b" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" stroke-dasharray="6,3" />
          <path :d="departmentPath('财经系')" fill="none" stroke="#10b981" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" stroke-dasharray="6,3" />
          <!-- Data points -->
          <circle v-for="(w, i) in weeklyTrends" :key="'sp'+i"
            :cx="xToPixel(i)" :cy="yToPixel(w.schoolAvg)" r="4"
            fill="#111827"
          />
          <circle v-for="(w, i) in weeklyTrends" :key="'bp'+i"
            :cx="xToPixel(i)" :cy="yToPixel(w.departments[0].value)" r="3"
            fill="#3b82f6"
          />
          <circle v-for="(w, i) in weeklyTrends" :key="'ap'+i"
            :cx="xToPixel(i)" :cy="yToPixel(w.departments[1].value)" r="3"
            fill="#f59e0b"
          />
          <circle v-for="(w, i) in weeklyTrends" :key="'ep'+i"
            :cx="xToPixel(i)" :cy="yToPixel(w.departments[2].value)" r="3"
            fill="#10b981"
          />
        </svg>
      </div>

      <!-- Category Distribution -->
      <div class="bg-white rounded-xl p-5 shadow-sm border border-gray-100">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-sm font-semibold text-gray-800">扣分类别分布</h3>
          <span class="text-xs text-gray-400">本月共 {{ categoryDistribution.reduce((s, c) => s + c.count, 0) }} 次扣分</span>
        </div>
        <div class="flex gap-6">
          <!-- Donut chart -->
          <div class="relative flex-shrink-0" style="width: 160px; height: 160px">
            <svg viewBox="0 0 160 160" class="w-full h-full">
              <circle v-for="(cat, i) in categoryDistribution" :key="cat.name"
                cx="80" cy="80" r="60" fill="none"
                :stroke="cat.color" stroke-width="24"
                :stroke-dasharray="`${cat.percentage * 3.77} ${377 - cat.percentage * 3.77}`"
                :stroke-dashoffset="-categoryDistribution.slice(0, i).reduce((s, c) => s + c.percentage * 3.77, 0)"
                :transform="`rotate(-90 80 80)`"
              />
            </svg>
            <div class="absolute inset-0 flex flex-col items-center justify-center">
              <span class="text-2xl font-bold text-gray-900">{{ categoryDistribution.reduce((s, c) => s + c.count, 0) }}</span>
              <span class="text-xs text-gray-400">总次数</span>
            </div>
          </div>
          <!-- Legend + bars -->
          <div class="flex-1 flex flex-col justify-center gap-3">
            <div v-for="cat in categoryDistribution" :key="cat.name" class="flex items-center gap-3">
              <span class="w-2.5 h-2.5 rounded-full flex-shrink-0" :style="{ background: cat.color }" />
              <span class="text-sm text-gray-700 w-10">{{ cat.name }}</span>
              <div class="flex-1 h-5 bg-gray-100 rounded-full overflow-hidden">
                <div
                  class="h-full rounded-full transition-all duration-500"
                  :style="{ width: cat.percentage + '%', background: cat.color }"
                />
              </div>
              <span class="text-xs text-gray-500 w-14 text-right">{{ cat.count }}次 {{ cat.percentage }}%</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Top Issues -->
    <div class="bg-white rounded-xl p-5 shadow-sm border border-gray-100 mb-6">
      <div class="flex items-center justify-between mb-4">
        <h3 class="text-sm font-semibold text-gray-800">高频问题项 TOP 10</h3>
        <span class="text-xs text-gray-400">按出现频次降序排列</span>
      </div>
      <table class="w-full">
        <thead>
          <tr class="text-xs text-gray-400 border-b border-gray-100">
            <th class="text-left pb-3 w-12">#</th>
            <th class="text-left pb-3">问题项</th>
            <th class="text-left pb-3 w-16">类别</th>
            <th class="text-left pb-3 w-16">次数</th>
            <th class="text-left pb-3 w-20">累计扣分</th>
            <th class="text-left pb-3 w-48">频次占比</th>
            <th class="text-left pb-3 w-28">趋势</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="issue in topIssues" :key="issue.rank" class="border-b border-gray-50 hover:bg-gray-50/50 transition-colors">
            <td class="py-3">
              <span
                class="inline-flex items-center justify-center w-6 h-6 rounded-full text-xs font-bold"
                :class="[
                  issue.rank <= 3 ? 'bg-red-100 text-red-700' : 'bg-gray-100 text-gray-500'
                ]"
              >{{ issue.rank }}</span>
            </td>
            <td class="py-3 text-sm font-medium text-gray-800">{{ issue.name }}</td>
            <td class="py-3">
              <span class="text-xs px-2 py-0.5 rounded-full bg-gray-100 text-gray-600">{{ issue.category }}</span>
            </td>
            <td class="py-3 text-sm text-gray-700 font-medium">{{ issue.count }}次</td>
            <td class="py-3 text-sm text-red-600 font-medium">-{{ issue.totalDeduction }}分</td>
            <td class="py-3">
              <div class="flex items-center gap-2">
                <div class="flex-1 h-4 bg-gray-100 rounded-full overflow-hidden">
                  <div
                    class="h-full rounded-full transition-all duration-700"
                    :class="[
                      issue.rank <= 3 ? 'bg-red-400' : issue.rank <= 6 ? 'bg-amber-400' : 'bg-gray-300'
                    ]"
                    :style="{ width: (issue.count / totalIssueCount * 100) + '%' }"
                  />
                </div>
                <span class="text-xs text-gray-400 w-10 text-right">{{ (issue.count / totalIssueCount * 100).toFixed(0) }}%</span>
              </div>
            </td>
            <td class="py-3">
              <div class="flex items-center gap-1">
                <component
                  :is="issue.trend === 'up' ? TrendingUp : issue.trend === 'down' ? TrendingDown : Minus"
                  class="w-3.5 h-3.5"
                  :class="[
                    issue.trend === 'up' ? 'text-red-500' : issue.trend === 'down' ? 'text-emerald-500' : 'text-gray-400'
                  ]"
                />
                <span
                  class="text-xs font-medium"
                  :class="[
                    issue.trend === 'up' ? 'text-red-600' : issue.trend === 'down' ? 'text-emerald-600' : 'text-gray-500'
                  ]"
                >
                  {{ issue.trend === 'up' ? '上升' : issue.trend === 'down' ? '下降' : '持平' }}
                  {{ issue.trendPercent > 0 ? issue.trendPercent + '%' : '' }}
                </span>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Department Ranking -->
    <div class="bg-white rounded-xl p-5 shadow-sm border border-gray-100 mb-6">
      <div class="flex items-center justify-between mb-4">
        <h3 class="text-sm font-semibold text-gray-800">系部对比排名</h3>
        <span class="text-xs text-gray-400">综合分 = 班级平均分 × 70% + 系部独立分 × 30%</span>
      </div>
      <table class="w-full">
        <thead>
          <tr class="text-xs text-gray-400 border-b border-gray-100">
            <th class="text-left pb-3 w-12">排名</th>
            <th class="text-left pb-3">系部</th>
            <th class="text-center pb-3 w-28">班级平均分</th>
            <th class="text-center pb-3 w-28">系部独立分</th>
            <th class="text-center pb-3 w-28">综合分</th>
            <th class="text-center pb-3 w-20">评级</th>
            <th class="text-center pb-3 w-20">趋势</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="dept in departmentRankings" :key="dept.rank" class="border-b border-gray-50">
            <td class="py-4">
              <span
                class="inline-flex items-center justify-center w-7 h-7 rounded-lg text-sm font-bold"
                :class="[
                  dept.rank === 1 ? 'bg-amber-100 text-amber-700' :
                  dept.rank === 2 ? 'bg-gray-100 text-gray-600' :
                  'bg-orange-50 text-orange-600'
                ]"
              >{{ dept.rank }}</span>
            </td>
            <td class="py-4">
              <span class="text-sm font-medium text-gray-800">{{ dept.name }}</span>
            </td>
            <td class="py-4 text-center">
              <span class="text-sm text-gray-700">{{ dept.classAvgScore.toFixed(1) }}</span>
            </td>
            <td class="py-4 text-center">
              <span class="text-sm text-gray-700">{{ dept.departmentScore.toFixed(1) }}</span>
            </td>
            <td class="py-4 text-center">
              <span class="text-base font-bold text-gray-900">{{ dept.compositeScore.toFixed(1) }}</span>
            </td>
            <td class="py-4 text-center">
              <span
                class="text-xs px-2.5 py-1 rounded-full font-medium"
                :class="[
                  dept.levelColor === 'emerald' ? 'bg-emerald-50 text-emerald-700' : 'bg-blue-50 text-blue-700'
                ]"
              >{{ dept.level }}</span>
            </td>
            <td class="py-4 text-center">
              <component
                :is="dept.trend === 'up' ? TrendingUp : dept.trend === 'down' ? TrendingDown : Minus"
                class="w-4 h-4 mx-auto"
                :class="[
                  dept.trend === 'up' ? 'text-emerald-500' : dept.trend === 'down' ? 'text-red-500' : 'text-gray-400'
                ]"
              />
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Alerts -->
    <div class="bg-white rounded-xl p-5 shadow-sm border border-gray-100">
      <div class="flex items-center gap-2 mb-4">
        <AlertTriangle class="w-4 h-4 text-amber-500" />
        <h3 class="text-sm font-semibold text-gray-800">智能预警</h3>
      </div>
      <div class="space-y-2">
        <div
          v-for="(alert, i) in alerts"
          :key="i"
          class="flex items-start gap-3 p-3 rounded-lg"
          :class="[
            alert.type === 'danger' ? 'bg-red-50' :
            alert.type === 'warning' ? 'bg-amber-50' :
            'bg-blue-50'
          ]"
        >
          <component
            :is="alert.icon === 'alert' ? AlertTriangle : alert.icon === 'target' ? Target : Info"
            class="w-4 h-4 mt-0.5 flex-shrink-0"
            :class="[
              alert.type === 'danger' ? 'text-red-500' :
              alert.type === 'warning' ? 'text-amber-500' :
              'text-blue-500'
            ]"
          />
          <span
            class="text-sm"
            :class="[
              alert.type === 'danger' ? 'text-red-700' :
              alert.type === 'warning' ? 'text-amber-700' :
              'text-blue-700'
            ]"
          >{{ alert.message }}</span>
        </div>
      </div>
    </div>
  </div>
</template>
