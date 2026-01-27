<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import {
  TrendingDown, TrendingUp, Minus, Award, AlertCircle, Clock,
  BarChart3, User, ChevronRight, ArrowUpRight, ArrowDownRight,
  FileWarning, Shield, Eye, MessageSquare, ExternalLink, Target
} from 'lucide-vue-next'
import { getOverview, getDeductions, getTopIssues, getStudentViolations, getImprovement } from '@/api/teacherDashboard'
import type { TopIssueItem, StudentViolationItem, ImprovementData, DeductionDetailRecord } from '@/types/inspectionSession'

// ─── Types ───
interface TrendPoint {
  week: string
  myScore: number
  gradeAvg: number
}

interface TopIssueDisplay {
  rank: number
  issueName: string
  occurrenceCount: number
  totalDeduction: number
  trend: 'persistent' | 'improving' | 'significantly_improved' | 'new'
  maxCount: number
}

interface PendingAction {
  id: number
  title: string
  severity: 'MINOR' | 'MODERATE' | 'SEVERE' | 'CRITICAL'
  dueDate: string
  daysTillDue: number
}

interface StudentAlert {
  studentId: number
  studentName: string
  violationCount: number
  violationTypes: string[]
  trend: 'worsening' | 'stable' | 'improving'
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
}

// ─── Date helpers ───
function getWeekRange(): { start: string; end: string } {
  const now = new Date()
  const day = now.getDay() || 7
  const monday = new Date(now)
  monday.setDate(now.getDate() - day + 1)
  const sunday = new Date(monday)
  sunday.setDate(monday.getDate() + 6)
  return {
    start: monday.toISOString().slice(0, 10),
    end: sunday.toISOString().slice(0, 10)
  }
}

function getPreviousWeekRange(): { start: string; end: string } {
  const now = new Date()
  const day = now.getDay() || 7
  const monday = new Date(now)
  monday.setDate(now.getDate() - day + 1 - 7)
  const sunday = new Date(monday)
  sunday.setDate(monday.getDate() + 6)
  return {
    start: monday.toISOString().slice(0, 10),
    end: sunday.toISOString().slice(0, 10)
  }
}

// ─── Reactive State ───
// TODO: Replace classId with actual class from user context / route params
const classId = ref<number>(0)
const loading = ref(false)
const dateRangeMode = ref<'week' | 'month' | 'semester'>('week')

const overview = ref({
  className: '',
  teacherName: '',
  weeklyDeduction: 0,
  weeklyDiff: 0,
  gradeRanking: 0,
  totalClassesInGrade: 0,
  rankingChange: 0,
  currentRating: '待评',
  ratingStars: 0,
  pendingActions: 0,
  pendingAppeals: 0,
})

const trendData = ref<TrendPoint[]>([])

const categoryDistribution = ref<{ name: string; myValue: number; avgValue: number; max: number }[]>([])

const topIssues = ref<TopIssueDisplay[]>([])

const pendingActions = ref<PendingAction[]>([])

const studentAlerts = ref<StudentAlert[]>([])

// ─── Data Fetching ───
async function fetchDashboardData() {
  if (!classId.value) return
  loading.value = true
  try {
    const week = getWeekRange()
    const prevWeek = getPreviousWeekRange()

    const [overviewData, topIssuesData, studentData, improvementData] = await Promise.all([
      getOverview(classId.value, week.start, week.end),
      getTopIssues(classId.value, week.start, week.end, 5),
      getStudentViolations(classId.value, week.start, week.end),
      getImprovement(classId.value, week.start, week.end, prevWeek.start, prevWeek.end)
    ])

    // Map overview
    overview.value.weeklyDeduction = overviewData.weeklyDeduction ?? 0
    overview.value.weeklyDiff = improvementData.change ?? 0

    // Map top issues
    const maxCount = Math.max(...topIssuesData.map(i => i.occurrenceCount), 1)
    topIssues.value = topIssuesData.map((item, idx) => ({
      rank: idx + 1,
      issueName: item.issueName,
      occurrenceCount: item.occurrenceCount,
      totalDeduction: item.totalDeduction,
      trend: 'persistent' as const,
      maxCount
    }))

    // Map student violations to alerts
    studentAlerts.value = studentData.map(s => ({
      studentId: s.studentId,
      studentName: s.studentName,
      violationCount: s.violationCount,
      violationTypes: s.violationTypes,
      trend: 'worsening' as const,
      riskLevel: s.violationCount >= 3 ? 'HIGH' as const : 'MEDIUM' as const
    }))
  } catch (e) {
    console.error('Failed to fetch dashboard data:', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  // If classId is available (e.g., from route or user context), fetch data
  if (classId.value) {
    fetchDashboardData()
  }
})

// ─── Computed ───
const statCards = computed(() => [
  {
    label: '本周扣分',
    value: overview.value.weeklyDeduction,
    unit: '分',
    diff: overview.value.weeklyDiff,
    diffLabel: overview.value.weeklyDiff < 0 ? '较上周减少' : '较上周增加',
    isGood: overview.value.weeklyDiff < 0,
    color: 'blue',
    icon: BarChart3,
  },
  {
    label: '年级排名',
    value: overview.value.gradeRanking,
    unit: `/${overview.value.totalClassesInGrade}`,
    diff: overview.value.rankingChange,
    diffLabel: overview.value.rankingChange > 0 ? `上升${overview.value.rankingChange}名` : overview.value.rankingChange < 0 ? `下降${Math.abs(overview.value.rankingChange)}名` : '持平',
    isGood: overview.value.rankingChange > 0,
    color: 'purple',
    icon: Award,
  },
  {
    label: '当前评级',
    value: overview.value.currentRating,
    unit: '',
    diff: null,
    diffLabel: '★'.repeat(overview.value.ratingStars) + '☆'.repeat(5 - overview.value.ratingStars),
    isGood: true,
    color: 'amber',
    icon: Target,
  },
  {
    label: '待处理',
    value: overview.value.pendingActions + overview.value.pendingAppeals,
    unit: '项',
    diff: null,
    diffLabel: `${overview.value.pendingActions}整改 + ${overview.value.pendingAppeals}申诉`,
    isGood: false,
    color: 'red',
    icon: AlertCircle,
  },
])

// ─── Helpers ───
const colorMap: Record<string, { bg: string; text: string; border: string; iconBg: string }> = {
  blue:   { bg: 'bg-blue-50',   text: 'text-blue-600',   border: 'border-l-blue-500',   iconBg: 'bg-blue-100' },
  purple: { bg: 'bg-purple-50', text: 'text-purple-600', border: 'border-l-purple-500', iconBg: 'bg-purple-100' },
  amber:  { bg: 'bg-amber-50',  text: 'text-amber-600',  border: 'border-l-amber-500',  iconBg: 'bg-amber-100' },
  red:    { bg: 'bg-red-50',    text: 'text-red-600',    border: 'border-l-red-500',    iconBg: 'bg-red-100' },
}

function getTrendLabel(trend: TopIssue['trend']) {
  const map: Record<string, { label: string; class: string }> = {
    persistent: { label: '持续问题', class: 'text-red-600 bg-red-50' },
    improving: { label: '有好转', class: 'text-amber-600 bg-amber-50' },
    significantly_improved: { label: '明显改善', class: 'text-emerald-600 bg-emerald-50' },
    new: { label: '新出现', class: 'text-blue-600 bg-blue-50' },
  }
  return map[trend] || map.persistent
}

function getSeverityStyle(severity: string) {
  const map: Record<string, string> = {
    CRITICAL: 'bg-red-100 text-red-700',
    SEVERE: 'bg-red-100 text-red-700',
    MODERATE: 'bg-amber-100 text-amber-700',
    MINOR: 'bg-gray-100 text-gray-600',
  }
  return map[severity] || map.MINOR
}

function getSeverityLabel(severity: string) {
  const map: Record<string, string> = { CRITICAL: '紧急', SEVERE: '紧急', MODERATE: '一般', MINOR: '轻微' }
  return map[severity] || '未知'
}

function getRiskStyle(risk: string) {
  const map: Record<string, string> = {
    CRITICAL: 'bg-red-100 text-red-700',
    HIGH: 'bg-red-100 text-red-700',
    MEDIUM: 'bg-amber-100 text-amber-700',
    LOW: 'bg-emerald-100 text-emerald-700',
  }
  return map[risk] || map.LOW
}

// Trend chart scaling
const trendMax = computed(() => Math.max(...trendData.flatMap(d => [d.myScore, d.gradeAvg])) * 1.2)
function trendY(val: number) {
  return 100 - (val / trendMax.value) * 100
}
</script>

<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <!-- Header -->
    <div class="mb-6">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-gray-900">班主任工作台</h1>
          <p class="mt-1 text-sm text-gray-500">{{ overview.className }} | 班主任: {{ overview.teacherName }}</p>
        </div>
        <div class="flex items-center gap-2">
          <select
            v-model="dateRangeMode"
            class="h-9 rounded-lg border border-gray-300 px-3 text-sm text-gray-700 focus:border-blue-500 focus:outline-none"
            @change="fetchDashboardData"
          >
            <option value="week">本周</option>
            <option value="month">本月</option>
            <option value="semester">本学期</option>
          </select>
        </div>
      </div>
    </div>

    <!-- Statistics Cards -->
    <div class="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <div
        v-for="card in statCards"
        :key="card.label"
        :class="[
          'rounded-xl border border-gray-200 bg-white p-5 shadow-sm transition-shadow hover:shadow-md',
          'border-l-4',
          colorMap[card.color].border
        ]"
      >
        <div class="flex items-start justify-between">
          <div>
            <p class="text-xs font-medium text-gray-500">{{ card.label }}</p>
            <div class="mt-1.5 flex items-baseline gap-1">
              <span class="text-2xl font-bold text-gray-900">{{ card.value }}</span>
              <span class="text-sm text-gray-400">{{ card.unit }}</span>
            </div>
            <div class="mt-2 flex items-center gap-1">
              <template v-if="card.diff !== null && card.diff !== 0">
                <ArrowDownRight v-if="card.isGood" class="h-3.5 w-3.5 text-emerald-500" />
                <ArrowUpRight v-else class="h-3.5 w-3.5 text-red-500" />
              </template>
              <Minus v-else-if="card.diff === 0" class="h-3.5 w-3.5 text-gray-400" />
              <span :class="['text-xs font-medium', card.isGood ? 'text-emerald-600' : card.diff === null ? 'text-gray-500' : 'text-red-600']">
                {{ card.diffLabel }}
              </span>
            </div>
          </div>
          <div :class="['flex h-11 w-11 items-center justify-center rounded-xl', colorMap[card.color].iconBg]">
            <component :is="card.icon" :class="['h-5 w-5', colorMap[card.color].text]" />
          </div>
        </div>
      </div>
    </div>

    <!-- Charts Row -->
    <div class="mb-6 grid grid-cols-1 gap-6 lg:grid-cols-2">
      <!-- Trend Chart -->
      <div class="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
        <div class="mb-4 flex items-center justify-between">
          <h3 class="text-sm font-semibold text-gray-800">扣分趋势 (近4周)</h3>
          <div class="flex items-center gap-4 text-xs text-gray-500">
            <span class="flex items-center gap-1.5">
              <span class="inline-block h-0.5 w-4 rounded bg-blue-500"></span> 我的班级
            </span>
            <span class="flex items-center gap-1.5">
              <span class="inline-block h-0.5 w-4 rounded bg-gray-300" style="border-top: 2px dashed #d1d5db; height: 0;"></span> 年级平均
            </span>
          </div>
        </div>
        <!-- Simple CSS chart -->
        <div class="relative h-48">
          <svg class="h-full w-full" viewBox="0 0 400 200" preserveAspectRatio="none">
            <!-- Grid lines -->
            <line v-for="i in 4" :key="i" :x1="0" :y1="i * 40" :x2="400" :y2="i * 40" stroke="#f3f4f6" stroke-width="1" />
            <!-- My class line -->
            <polyline
              :points="trendData.map((d, i) => `${i * 120 + 40},${trendY(d.myScore) * 2}`).join(' ')"
              fill="none" stroke="#3b82f6" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"
            />
            <!-- Grade avg line (dashed) -->
            <polyline
              :points="trendData.map((d, i) => `${i * 120 + 40},${trendY(d.gradeAvg) * 2}`).join(' ')"
              fill="none" stroke="#d1d5db" stroke-width="2" stroke-dasharray="6,4" stroke-linecap="round"
            />
            <!-- Data points - my class -->
            <circle
              v-for="(d, i) in trendData" :key="'mc'+i"
              :cx="i * 120 + 40" :cy="trendY(d.myScore) * 2"
              r="4" fill="#3b82f6" stroke="white" stroke-width="2"
            />
            <!-- Data points - avg -->
            <circle
              v-for="(d, i) in trendData" :key="'avg'+i"
              :cx="i * 120 + 40" :cy="trendY(d.gradeAvg) * 2"
              r="3" fill="#d1d5db" stroke="white" stroke-width="2"
            />
          </svg>
          <!-- X axis labels -->
          <div class="absolute bottom-0 left-0 flex w-full justify-between px-6 text-[10px] text-gray-400">
            <span v-for="d in trendData" :key="d.week">{{ d.week }}</span>
          </div>
        </div>
        <p class="mt-2 text-center text-xs text-emerald-600">扣分持续减少，班级管理趋势良好</p>
      </div>

      <!-- Category Distribution -->
      <div class="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
        <div class="mb-4 flex items-center justify-between">
          <h3 class="text-sm font-semibold text-gray-800">扣分类别对比</h3>
          <div class="flex items-center gap-4 text-xs text-gray-500">
            <span class="flex items-center gap-1.5">
              <span class="inline-block h-3 w-3 rounded bg-blue-500"></span> 我的班级
            </span>
            <span class="flex items-center gap-1.5">
              <span class="inline-block h-3 w-3 rounded bg-gray-300"></span> 年级平均
            </span>
          </div>
        </div>
        <div class="space-y-4">
          <div v-for="cat in categoryDistribution" :key="cat.name">
            <div class="mb-1.5 flex items-center justify-between text-xs">
              <span class="font-medium text-gray-700">{{ cat.name }}</span>
              <span class="text-gray-500">{{ cat.myValue }} / {{ cat.avgValue }}</span>
            </div>
            <div class="relative h-5 overflow-hidden rounded-full bg-gray-100">
              <div
                class="absolute inset-y-0 left-0 rounded-full bg-blue-500/80 transition-all duration-500"
                :style="{ width: `${(cat.myValue / cat.max) * 100}%` }"
              ></div>
              <div
                class="absolute inset-y-0 left-0 h-full border-r-2 border-dashed border-gray-400/50"
                :style="{ width: `${(cat.avgValue / cat.max) * 100}%` }"
              ></div>
            </div>
          </div>
        </div>
        <p class="mt-3 text-xs text-gray-500">虚线为年级平均水平 | 卫生类扣分接近平均，纪律类优于平均</p>
      </div>
    </div>

    <!-- Top Issues -->
    <div class="mb-6 rounded-xl border border-gray-200 bg-white shadow-sm">
      <div class="border-b border-gray-100 px-5 py-4">
        <h3 class="text-sm font-semibold text-gray-800">高频问题 TOP5</h3>
      </div>
      <div class="overflow-x-auto">
        <table class="w-full">
          <thead>
            <tr class="border-b border-gray-100 bg-gray-50/50">
              <th class="px-5 py-3 text-left text-xs font-medium text-gray-500">#</th>
              <th class="px-5 py-3 text-left text-xs font-medium text-gray-500">问题项</th>
              <th class="px-5 py-3 text-center text-xs font-medium text-gray-500">次数</th>
              <th class="px-5 py-3 text-center text-xs font-medium text-gray-500">累计扣分</th>
              <th class="px-5 py-3 text-left text-xs font-medium text-gray-500">频率</th>
              <th class="px-5 py-3 text-left text-xs font-medium text-gray-500">状态</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-100">
            <tr v-for="issue in topIssues" :key="issue.rank" class="hover:bg-gray-50/50">
              <td class="px-5 py-3.5 text-sm font-bold text-gray-400">{{ issue.rank }}</td>
              <td class="px-5 py-3.5 text-sm font-medium text-gray-800">{{ issue.issueName }}</td>
              <td class="px-5 py-3.5 text-center text-sm font-semibold text-gray-700">{{ issue.occurrenceCount }}次</td>
              <td class="px-5 py-3.5 text-center text-sm font-semibold text-red-600">-{{ issue.totalDeduction }}</td>
              <td class="px-5 py-3.5">
                <div class="h-2 w-24 overflow-hidden rounded-full bg-gray-100">
                  <div
                    class="h-full rounded-full transition-all duration-500"
                    :class="issue.trend === 'persistent' ? 'bg-red-400' : issue.trend === 'new' ? 'bg-blue-400' : issue.trend === 'improving' ? 'bg-amber-400' : 'bg-emerald-400'"
                    :style="{ width: `${(issue.occurrenceCount / issue.maxCount) * 100}%` }"
                  ></div>
                </div>
              </td>
              <td class="px-5 py-3.5">
                <span :class="['rounded-full px-2.5 py-1 text-[10px] font-semibold', getTrendLabel(issue.trend).class]">
                  {{ getTrendLabel(issue.trend).label }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Bottom row: Actions + Student Alerts -->
    <div class="grid grid-cols-1 gap-6 lg:grid-cols-2">
      <!-- Pending Actions -->
      <div class="rounded-xl border border-gray-200 bg-white shadow-sm">
        <div class="flex items-center justify-between border-b border-gray-100 px-5 py-4">
          <h3 class="text-sm font-semibold text-gray-800">待处理整改工单</h3>
          <button class="flex items-center gap-1 text-xs font-medium text-blue-600 hover:text-blue-700">
            查看全部 <ChevronRight class="h-3.5 w-3.5" />
          </button>
        </div>
        <div v-if="pendingActions.length" class="divide-y divide-gray-100">
          <div v-for="action in pendingActions" :key="action.id" class="px-5 py-4">
            <div class="flex items-start justify-between">
              <div class="flex items-center gap-2">
                <FileWarning class="h-4 w-4 text-gray-400" />
                <span class="text-sm font-medium text-gray-800">{{ action.title }}</span>
              </div>
              <span :class="['rounded-full px-2 py-0.5 text-[10px] font-semibold', getSeverityStyle(action.severity)]">
                {{ getSeverityLabel(action.severity) }}
              </span>
            </div>
            <div class="mt-2 flex items-center gap-3 text-xs text-gray-500">
              <span class="flex items-center gap-1">
                <Clock class="h-3 w-3" />
                截止: {{ action.dueDate }}
              </span>
              <span :class="action.daysTillDue <= 1 ? 'font-semibold text-red-500' : ''">
                ({{ action.daysTillDue <= 0 ? '已超期' : `剩余${action.daysTillDue}天` }})
              </span>
            </div>
            <div class="mt-3 flex gap-2">
              <button class="rounded-lg border border-gray-300 px-3 py-1.5 text-xs font-medium text-gray-600 hover:bg-gray-50">
                查看详情
              </button>
              <button class="rounded-lg bg-blue-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-blue-700">
                开始处理
              </button>
            </div>
          </div>
        </div>
        <div v-else class="px-5 py-12 text-center">
          <Shield class="mx-auto mb-2 h-10 w-10 text-emerald-300" />
          <p class="text-sm text-gray-500">暂无待处理的整改工单</p>
        </div>
      </div>

      <!-- Student Alerts -->
      <div class="rounded-xl border border-gray-200 bg-white shadow-sm">
        <div class="flex items-center justify-between border-b border-gray-100 px-5 py-4">
          <h3 class="text-sm font-semibold text-gray-800">学生违纪关注</h3>
          <button class="flex items-center gap-1 text-xs font-medium text-blue-600 hover:text-blue-700">
            查看全部 <ChevronRight class="h-3.5 w-3.5" />
          </button>
        </div>
        <div v-if="studentAlerts.length" class="divide-y divide-gray-100">
          <div v-for="alert in studentAlerts" :key="alert.studentId" class="px-5 py-4">
            <div class="flex items-start justify-between">
              <div class="flex items-center gap-2.5">
                <div class="flex h-9 w-9 items-center justify-center rounded-full bg-gray-100 text-sm font-semibold text-gray-600">
                  {{ alert.studentName.charAt(0) }}
                </div>
                <div>
                  <div class="flex items-center gap-2">
                    <span class="text-sm font-semibold text-gray-800">{{ alert.studentName }}</span>
                    <span :class="['rounded-full px-2 py-0.5 text-[10px] font-semibold', getRiskStyle(alert.riskLevel)]">
                      {{ alert.riskLevel === 'HIGH' || alert.riskLevel === 'CRITICAL' ? '高风险' : '关注' }}
                    </span>
                  </div>
                  <p class="mt-0.5 text-xs text-gray-500">
                    近30天 {{ alert.violationCount }} 次违纪:
                    {{ alert.violationTypes.join(', ') }}
                  </p>
                </div>
              </div>
              <div class="flex items-center gap-1">
                <ArrowUpRight v-if="alert.trend === 'worsening'" class="h-4 w-4 text-red-500" />
                <Minus v-else-if="alert.trend === 'stable'" class="h-4 w-4 text-gray-400" />
                <ArrowDownRight v-else class="h-4 w-4 text-emerald-500" />
              </div>
            </div>
            <div class="mt-3 flex gap-2">
              <button class="flex items-center gap-1 rounded-lg border border-gray-300 px-3 py-1.5 text-xs font-medium text-gray-600 hover:bg-gray-50">
                <Eye class="h-3 w-3" /> 查看画像
              </button>
              <button class="flex items-center gap-1 rounded-lg bg-blue-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-blue-700">
                <MessageSquare class="h-3 w-3" /> 记录处理
              </button>
            </div>
          </div>
        </div>
        <div v-else class="px-5 py-12 text-center">
          <Shield class="mx-auto mb-2 h-10 w-10 text-emerald-300" />
          <p class="text-sm text-gray-500">暂无需要关注的学生</p>
        </div>
      </div>
    </div>
  </div>
</template>
