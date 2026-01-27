<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  UserCircle, AlertTriangle, Award, TrendingUp, TrendingDown, Minus,
  Calendar, Clock, MapPin, BookOpen, Users, Phone, ChevronRight,
  Shield, Target, Bell, CheckCircle2, XCircle, Eye, FileText,
  ArrowLeft, MessageSquare, History
} from 'lucide-vue-next'
import { ElProgress } from 'element-plus'

// ─── Types ───
type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
type BehaviorType = 'VIOLATION' | 'COMMENDATION'
type Category = 'DISCIPLINE' | 'HYGIENE' | 'SAFETY' | 'ATTENDANCE' | 'OTHER'
type Trend = 'IMPROVING' | 'STABLE' | 'DETERIORATING'

interface BehaviorRecord {
  id: number
  date: string
  type: BehaviorType
  category: Category
  description: string
  severity: string
  source: string
  status: string
  handler?: string
  resolution?: string
}

interface StudentProfile {
  id: number
  name: string
  studentNo: string
  gender: string
  className: string
  gradeName: string
  departmentName: string
  dormitoryNo: string
  phone: string
  avatarUrl?: string
  enrollYear: string
}

interface BehaviorSummary {
  totalViolations: number
  totalCommendations: number
  recent30DayViolations: number
  riskLevel: RiskLevel
  trend: Trend
  frequentCategories: { category: Category; count: number }[]
  monthlyTrend: { month: string; violations: number; commendations: number }[]
}

// ─── Mock Data ───
const student: StudentProfile = {
  id: 10001, name: '张三', studentNo: '2024010001', gender: '男',
  className: '计算机2班', gradeName: '2024级', departmentName: '信息工程系',
  dormitoryNo: '1号楼301', phone: '138****1234', enrollYear: '2024',
}

const summary: BehaviorSummary = {
  totalViolations: 8, totalCommendations: 2, recent30DayViolations: 3,
  riskLevel: 'MEDIUM', trend: 'DETERIORATING',
  frequentCategories: [
    { category: 'HYGIENE', count: 4 },
    { category: 'DISCIPLINE', count: 2 },
    { category: 'ATTENDANCE', count: 1 },
    { category: 'SAFETY', count: 1 },
  ],
  monthlyTrend: [
    { month: '10月', violations: 1, commendations: 1 },
    { month: '11月', violations: 2, commendations: 0 },
    { month: '12月', violations: 2, commendations: 1 },
    { month: '1月', violations: 3, commendations: 0 },
  ],
}

const records: BehaviorRecord[] = [
  { id: 1, date: '2026-01-27', type: 'VIOLATION', category: 'HYGIENE', description: '301宿舍地面不洁，个人区域未打扫', severity: 'MODERATE', source: '日常检查', status: '已通知', handler: '李老师' },
  { id: 2, date: '2026-01-22', type: 'VIOLATION', category: 'DISCIPLINE', description: '课间在走廊大声喧哗', severity: 'MINOR', source: '教师报告', status: '已处理', handler: '李老师', resolution: '口头教育提醒' },
  { id: 3, date: '2026-01-18', type: 'VIOLATION', category: 'ATTENDANCE', description: '迟到15分钟', severity: 'MINOR', source: '日常检查', status: '已处理', handler: '李老师', resolution: '写情况说明' },
  { id: 4, date: '2026-01-10', type: 'COMMENDATION', category: 'HYGIENE', description: '宿舍卫生评比获"最整洁宿舍"称号', severity: '-', source: '月度评比', status: '已记录' },
  { id: 5, date: '2025-12-28', type: 'VIOLATION', category: 'HYGIENE', description: '301宿舍物品乱放', severity: 'MINOR', source: '日常检查', status: '已处理', handler: '李老师', resolution: '当日整改完成' },
  { id: 6, date: '2025-12-20', type: 'VIOLATION', category: 'SAFETY', description: '301宿舍违规使用吹风机', severity: 'SEVERE', source: '专项检查', status: '已处理', handler: '李老师', resolution: '没收违规电器，签署安全承诺书' },
  { id: 7, date: '2025-12-15', type: 'VIOLATION', category: 'HYGIENE', description: '被褥未叠', severity: 'MINOR', source: '日常检查', status: '已处理', handler: '李老师', resolution: '口头提醒' },
  { id: 8, date: '2025-11-25', type: 'COMMENDATION', category: 'OTHER', description: '参加校园文化节志愿者服务', severity: '-', source: '教师推荐', status: '已记录' },
  { id: 9, date: '2025-11-15', type: 'VIOLATION', category: 'DISCIPLINE', description: '课堂玩手机', severity: 'MODERATE', source: '教师报告', status: '已处理', handler: '李老师', resolution: '手机暂时代管一天' },
  { id: 10, date: '2025-10-20', type: 'VIOLATION', category: 'HYGIENE', description: '垃圾未倒', severity: 'MINOR', source: '日常检查', status: '已处理', handler: '李老师', resolution: '口头提醒' },
]

const alerts = [
  { id: 1, type: 'FREQUENCY', condition: '30天内违纪3次', riskLevel: 'MEDIUM' as RiskLevel, status: 'ACTIVE', createdAt: '2026-01-27' },
  { id: 2, type: 'TREND', condition: '连续3个月违纪数上升', riskLevel: 'MEDIUM' as RiskLevel, status: 'ACTIVE', createdAt: '2026-01-20' },
]

// ─── Helpers ───
const riskLabels: Record<RiskLevel, string> = { LOW: '低风险', MEDIUM: '中风险', HIGH: '高风险', CRITICAL: '极高风险' }
const riskColors: Record<RiskLevel, { bg: string; text: string; border: string }> = {
  LOW: { bg: 'bg-emerald-50', text: 'text-emerald-600', border: 'border-emerald-200' },
  MEDIUM: { bg: 'bg-amber-50', text: 'text-amber-600', border: 'border-amber-200' },
  HIGH: { bg: 'bg-orange-50', text: 'text-orange-600', border: 'border-orange-200' },
  CRITICAL: { bg: 'bg-red-50', text: 'text-red-600', border: 'border-red-200' },
}
const trendLabels: Record<Trend, string> = { IMPROVING: '好转', STABLE: '稳定', DETERIORATING: '恶化' }
const trendColors: Record<Trend, string> = { IMPROVING: 'text-emerald-600', STABLE: 'text-gray-500', DETERIORATING: 'text-red-600' }
const categoryLabels: Record<Category, string> = { DISCIPLINE: '纪律', HYGIENE: '卫生', SAFETY: '安全', ATTENDANCE: '考勤', OTHER: '其他' }
const categoryColors: Record<Category, string> = { DISCIPLINE: '#f59e0b', HYGIENE: '#3b82f6', SAFETY: '#ef4444', ATTENDANCE: '#10b981', OTHER: '#6b7280' }

const maxCategoryCount = computed(() => Math.max(...summary.frequentCategories.map(c => c.count)))

// Chart helpers
const chartHeight = 120
const chartWidth = 320
const barWidth = 24
const barGap = 8
const groupWidth = (barWidth * 2 + barGap)
const maxBarValue = 4

function barHeight(value: number): number {
  return (value / maxBarValue) * (chartHeight - 20)
}
</script>

<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <!-- Back navigation -->
    <button class="flex items-center gap-1.5 text-sm text-gray-500 hover:text-gray-700 mb-4 transition-colors">
      <ArrowLeft class="w-4 h-4" />
      返回列表
    </button>

    <!-- Profile Header -->
    <div class="bg-white rounded-xl p-6 shadow-sm border border-gray-100 mb-6">
      <div class="flex items-start gap-6">
        <!-- Avatar -->
        <div class="w-20 h-20 rounded-2xl bg-gray-100 flex items-center justify-center flex-shrink-0">
          <UserCircle class="w-12 h-12 text-gray-300" />
        </div>

        <!-- Basic info -->
        <div class="flex-1">
          <div class="flex items-center gap-3 mb-2">
            <h1 class="text-xl font-bold text-gray-900">{{ student.name }}</h1>
            <span class="text-sm text-gray-400">{{ student.studentNo }}</span>
            <span
              :class="[
                'text-xs px-2.5 py-1 rounded-full font-medium border',
                riskColors[summary.riskLevel].bg,
                riskColors[summary.riskLevel].text,
                riskColors[summary.riskLevel].border,
              ]"
            >{{ riskLabels[summary.riskLevel] }}</span>
          </div>
          <div class="grid grid-cols-4 gap-4 text-sm text-gray-500">
            <div class="flex items-center gap-2">
              <BookOpen class="w-4 h-4 text-gray-400" />
              {{ student.departmentName }} · {{ student.className }}
            </div>
            <div class="flex items-center gap-2">
              <Calendar class="w-4 h-4 text-gray-400" />
              {{ student.gradeName }}
            </div>
            <div class="flex items-center gap-2">
              <MapPin class="w-4 h-4 text-gray-400" />
              {{ student.dormitoryNo }}
            </div>
            <div class="flex items-center gap-2">
              <Phone class="w-4 h-4 text-gray-400" />
              {{ student.phone }}
            </div>
          </div>
        </div>

        <!-- Quick stats -->
        <div class="flex gap-4">
          <div class="text-center">
            <div class="text-2xl font-bold text-red-500">{{ summary.totalViolations }}</div>
            <div class="text-xs text-gray-400">违纪总次数</div>
          </div>
          <div class="text-center">
            <div class="text-2xl font-bold text-emerald-500">{{ summary.totalCommendations }}</div>
            <div class="text-xs text-gray-400">表扬次数</div>
          </div>
          <div class="text-center">
            <div class="text-2xl font-bold" :class="trendColors[summary.trend]">
              {{ summary.recent30DayViolations }}
            </div>
            <div class="text-xs text-gray-400">近30天违纪</div>
          </div>
        </div>
      </div>
    </div>

    <div class="grid grid-cols-3 gap-6">
      <!-- Left Column: Charts & Alerts -->
      <div class="col-span-1 space-y-6">
        <!-- Risk Assessment -->
        <div class="bg-white rounded-xl p-5 shadow-sm border border-gray-100">
          <h3 class="text-sm font-semibold text-gray-800 mb-4">风险评估</h3>
          <div class="flex items-center gap-4 mb-4">
            <div
              class="w-16 h-16 rounded-2xl flex items-center justify-center"
              :class="riskColors[summary.riskLevel].bg"
            >
              <Shield class="w-8 h-8" :class="riskColors[summary.riskLevel].text" />
            </div>
            <div>
              <div class="text-lg font-bold" :class="riskColors[summary.riskLevel].text">
                {{ riskLabels[summary.riskLevel] }}
              </div>
              <div class="flex items-center gap-1 text-sm" :class="trendColors[summary.trend]">
                <component
                  :is="summary.trend === 'IMPROVING' ? TrendingDown : summary.trend === 'DETERIORATING' ? TrendingUp : Minus"
                  class="w-4 h-4"
                />
                趋势: {{ trendLabels[summary.trend] }}
              </div>
            </div>
          </div>

          <!-- Active alerts -->
          <div v-if="alerts.length > 0" class="space-y-2">
            <div class="text-xs font-medium text-gray-500 mb-1">活跃预警</div>
            <div
              v-for="alert in alerts"
              :key="alert.id"
              class="flex items-start gap-2 p-2.5 rounded-lg"
              :class="riskColors[alert.riskLevel].bg"
            >
              <Bell class="w-3.5 h-3.5 mt-0.5" :class="riskColors[alert.riskLevel].text" />
              <div>
                <div class="text-xs font-medium" :class="riskColors[alert.riskLevel].text">{{ alert.condition }}</div>
                <div class="text-xs text-gray-400 mt-0.5">{{ alert.createdAt }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- Category Distribution -->
        <div class="bg-white rounded-xl p-5 shadow-sm border border-gray-100">
          <h3 class="text-sm font-semibold text-gray-800 mb-4">违纪类别分布</h3>
          <div class="space-y-3">
            <div v-for="cat in summary.frequentCategories" :key="cat.category" class="flex items-center gap-3">
              <span class="w-2.5 h-2.5 rounded-full flex-shrink-0" :style="{ background: categoryColors[cat.category] }" />
              <span class="text-sm text-gray-700 w-10">{{ categoryLabels[cat.category] }}</span>
              <div class="flex-1 h-6 bg-gray-100 rounded-full overflow-hidden">
                <div
                  class="h-full rounded-full transition-all duration-500 flex items-center justify-end pr-2"
                  :style="{
                    width: (cat.count / maxCategoryCount * 100) + '%',
                    background: categoryColors[cat.category],
                    minWidth: '32px'
                  }"
                >
                  <span class="text-xs font-bold text-white">{{ cat.count }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Monthly Trend -->
        <div class="bg-white rounded-xl p-5 shadow-sm border border-gray-100">
          <h3 class="text-sm font-semibold text-gray-800 mb-4">月度趋势</h3>
          <div class="flex items-center gap-4 text-xs text-gray-400 mb-3">
            <span class="flex items-center gap-1.5">
              <span class="w-3 h-3 rounded-sm bg-red-400 inline-block" />
              违纪
            </span>
            <span class="flex items-center gap-1.5">
              <span class="w-3 h-3 rounded-sm bg-emerald-400 inline-block" />
              表扬
            </span>
          </div>
          <svg :viewBox="`0 0 ${chartWidth} ${chartHeight + 20}`" class="w-full">
            <!-- Bars -->
            <g v-for="(m, i) in summary.monthlyTrend" :key="m.month"
              :transform="`translate(${20 + i * (groupWidth + 20)}, 0)`"
            >
              <!-- Violation bar -->
              <rect
                :x="0" :y="chartHeight - barHeight(m.violations)"
                :width="barWidth" :height="barHeight(m.violations)"
                fill="#f87171" rx="4"
              />
              <text
                v-if="m.violations > 0"
                :x="barWidth / 2" :y="chartHeight - barHeight(m.violations) - 4"
                text-anchor="middle" fill="#ef4444" font-size="10" font-weight="bold"
              >{{ m.violations }}</text>
              <!-- Commendation bar -->
              <rect
                :x="barWidth + barGap" :y="chartHeight - barHeight(m.commendations)"
                :width="barWidth" :height="barHeight(m.commendations)"
                fill="#34d399" rx="4"
              />
              <text
                v-if="m.commendations > 0"
                :x="barWidth + barGap + barWidth / 2" :y="chartHeight - barHeight(m.commendations) - 4"
                text-anchor="middle" fill="#10b981" font-size="10" font-weight="bold"
              >{{ m.commendations }}</text>
              <!-- Label -->
              <text
                :x="(barWidth * 2 + barGap) / 2" :y="chartHeight + 14"
                text-anchor="middle" fill="#9ca3af" font-size="10"
              >{{ m.month }}</text>
            </g>
            <!-- Baseline -->
            <line x1="10" :y1="chartHeight" :x2="chartWidth - 10" :y2="chartHeight" stroke="#e5e7eb" stroke-width="1" />
          </svg>
        </div>
      </div>

      <!-- Right Column: Records Timeline -->
      <div class="col-span-2">
        <div class="bg-white rounded-xl p-5 shadow-sm border border-gray-100">
          <div class="flex items-center justify-between mb-5">
            <h3 class="text-sm font-semibold text-gray-800">行为记录时间线</h3>
            <div class="flex items-center gap-2 text-xs text-gray-400">
              <History class="w-3.5 h-3.5" />
              共 {{ records.length }} 条记录
            </div>
          </div>

          <div class="relative">
            <!-- Timeline line -->
            <div class="absolute left-6 top-0 bottom-0 w-px bg-gray-200" />

            <!-- Records -->
            <div class="space-y-1">
              <div
                v-for="record in records"
                :key="record.id"
                class="relative flex items-start gap-4 py-3 pl-2"
              >
                <!-- Timeline dot -->
                <div class="relative z-10 flex-shrink-0">
                  <div
                    class="w-5 h-5 rounded-full border-2 flex items-center justify-center"
                    :class="[
                      record.type === 'VIOLATION'
                        ? 'border-red-300 bg-red-50'
                        : 'border-emerald-300 bg-emerald-50'
                    ]"
                  >
                    <component
                      :is="record.type === 'VIOLATION' ? XCircle : Award"
                      class="w-3 h-3"
                      :class="record.type === 'VIOLATION' ? 'text-red-400' : 'text-emerald-400'"
                    />
                  </div>
                </div>

                <!-- Content -->
                <div class="flex-1 min-w-0 pb-3 border-b border-gray-50">
                  <div class="flex items-center gap-2 mb-1">
                    <span class="text-xs text-gray-400">{{ record.date }}</span>
                    <span
                      class="text-xs px-1.5 py-0.5 rounded"
                      :style="{
                        background: categoryColors[record.category] + '15',
                        color: categoryColors[record.category]
                      }"
                    >{{ categoryLabels[record.category] }}</span>
                    <span v-if="record.type === 'VIOLATION'"
                      class="text-xs px-1.5 py-0.5 rounded"
                      :class="[
                        record.severity === 'SEVERE' ? 'bg-red-100 text-red-600' :
                        record.severity === 'MODERATE' ? 'bg-amber-100 text-amber-600' :
                        record.severity === 'MINOR' ? 'bg-gray-100 text-gray-500' :
                        'bg-gray-50 text-gray-400'
                      ]"
                    >{{ record.severity === 'SEVERE' ? '严重' : record.severity === 'MODERATE' ? '一般' : '轻微' }}</span>
                    <span
                      class="text-xs px-1.5 py-0.5 rounded"
                      :class="record.type === 'VIOLATION' ? 'bg-red-50 text-red-500' : 'bg-emerald-50 text-emerald-500'"
                    >{{ record.type === 'VIOLATION' ? '违纪' : '表扬' }}</span>
                  </div>

                  <p class="text-sm text-gray-800 mb-1">{{ record.description }}</p>

                  <div class="flex items-center gap-3 text-xs text-gray-400">
                    <span>来源: {{ record.source }}</span>
                    <span>状态: {{ record.status }}</span>
                    <span v-if="record.handler">处理人: {{ record.handler }}</span>
                  </div>

                  <div v-if="record.resolution" class="mt-2 flex items-start gap-2 p-2 bg-gray-50 rounded-lg">
                    <MessageSquare class="w-3.5 h-3.5 text-gray-400 mt-0.5 flex-shrink-0" />
                    <span class="text-xs text-gray-600">处理措施: {{ record.resolution }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
