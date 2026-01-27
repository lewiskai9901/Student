<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  CalendarDays, Plus, Search, Filter, Eye, Edit3, Copy, Trash2,
  Clock, Users, CheckCircle2, AlertCircle, Play, Pause, MoreHorizontal,
  Building2, ChevronRight, MapPin, ClipboardList, FileText,
  ArrowUpRight, LayoutGrid, List
} from 'lucide-vue-next'
import { ElSelect, ElOption, ElInput } from 'element-plus'

// ─── Types ───
type PlanStatus = 'DRAFT' | 'ACTIVE' | 'PAUSED' | 'COMPLETED' | 'ARCHIVED'
type InspectionLevel = 'CLASS' | 'DEPARTMENT' | 'SPECIAL'

interface Inspector {
  id: number
  name: string
  avatar?: string
}

interface Plan {
  id: number
  name: string
  templateName: string
  inspectionLevel: InspectionLevel
  scoringMode: string
  status: PlanStatus
  startDate: string
  endDate: string
  totalSessions: number
  completedSessions: number
  inspectors: Inspector[]
  scheduleType: string
  lastCheckDate?: string
  nextCheckDate?: string
  avgDeduction?: number
  createdAt: string
}

// ─── State ───
const searchQuery = ref('')
const statusFilter = ref<PlanStatus | 'ALL'>('ALL')
const viewMode = ref<'card' | 'list'>('card')

// ─── Mock Data ───
const plans: Plan[] = [
  {
    id: 1, name: '2025-2026第二学期日常检查', templateName: '日常宿舍卫生检查模板',
    inspectionLevel: 'CLASS', scoringMode: '基准分制(100分)', status: 'ACTIVE',
    startDate: '2026-01-06', endDate: '2026-07-15', totalSessions: 120, completedSessions: 15,
    inspectors: [{ id: 1, name: '张检查员' }, { id: 2, name: '王检查员' }, { id: 3, name: '刘检查员' }],
    scheduleType: '周期性(工作日)', lastCheckDate: '2026-01-26', nextCheckDate: '2026-01-27',
    avgDeduction: 12.3, createdAt: '2026-01-05',
  },
  {
    id: 2, name: '月度宿舍安全专项检查', templateName: '安全专项检查模板',
    inspectionLevel: 'SPECIAL', scoringMode: '纯扣分制', status: 'ACTIVE',
    startDate: '2026-01-15', endDate: '2026-01-31', totalSessions: 4, completedSessions: 2,
    inspectors: [{ id: 4, name: '李安全员' }, { id: 5, name: '陈安全员' }],
    scheduleType: '周期性(每周一)', lastCheckDate: '2026-01-22', nextCheckDate: '2026-01-29',
    avgDeduction: 8.5, createdAt: '2026-01-14',
  },
  {
    id: 3, name: '系部管理水平评估', templateName: '系部评估模板',
    inspectionLevel: 'DEPARTMENT', scoringMode: '双轨制(100分)', status: 'DRAFT',
    startDate: '2026-02-01', endDate: '2026-06-30', totalSessions: 0, completedSessions: 0,
    inspectors: [],
    scheduleType: '周期性(月度)', createdAt: '2026-01-20',
  },
  {
    id: 4, name: '上学期日常检查', templateName: '日常宿舍卫生检查模板',
    inspectionLevel: 'CLASS', scoringMode: '基准分制(100分)', status: 'COMPLETED',
    startDate: '2025-09-01', endDate: '2026-01-05', totalSessions: 80, completedSessions: 80,
    inspectors: [{ id: 1, name: '张检查员' }, { id: 2, name: '王检查员' }],
    scheduleType: '周期性(工作日)', lastCheckDate: '2026-01-03',
    avgDeduction: 14.1, createdAt: '2025-08-28',
  },
  {
    id: 5, name: '开学第一周纪律专项', templateName: '课堂纪律检查模板',
    inspectionLevel: 'CLASS', scoringMode: '纯扣分制', status: 'PAUSED',
    startDate: '2026-01-06', endDate: '2026-01-10', totalSessions: 5, completedSessions: 3,
    inspectors: [{ id: 6, name: '赵检查员' }],
    scheduleType: '每日', lastCheckDate: '2026-01-08',
    avgDeduction: 6.2, createdAt: '2026-01-04',
  },
]

const statusLabels: Record<PlanStatus, string> = {
  DRAFT: '草稿',
  ACTIVE: '进行中',
  PAUSED: '已暂停',
  COMPLETED: '已完成',
  ARCHIVED: '已归档',
}

const statusStyles: Record<PlanStatus, string> = {
  DRAFT: 'bg-gray-100 text-gray-500',
  ACTIVE: 'bg-emerald-50 text-emerald-600',
  PAUSED: 'bg-amber-50 text-amber-600',
  COMPLETED: 'bg-blue-50 text-blue-600',
  ARCHIVED: 'bg-gray-100 text-gray-400',
}

const levelLabels: Record<InspectionLevel, string> = {
  CLASS: '班级检查',
  DEPARTMENT: '系部评估',
  SPECIAL: '专项检查',
}

const levelColors: Record<InspectionLevel, string> = {
  CLASS: 'bg-blue-50 text-blue-600',
  DEPARTMENT: 'bg-violet-50 text-violet-600',
  SPECIAL: 'bg-amber-50 text-amber-600',
}

const filteredPlans = computed(() => {
  return plans.filter(p => {
    if (statusFilter.value !== 'ALL' && p.status !== statusFilter.value) return false
    if (searchQuery.value && !p.name.includes(searchQuery.value)) return false
    return true
  })
})

const statusCounts = computed(() => {
  const counts: Record<string, number> = { ALL: plans.length }
  plans.forEach(p => {
    counts[p.status] = (counts[p.status] || 0) + 1
  })
  return counts
})
</script>

<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <!-- Header -->
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900 mb-1">检查计划管理</h1>
        <p class="text-sm text-gray-500">创建和管理检查计划，配置检查范围与调度策略</p>
      </div>
      <button class="flex items-center gap-2 px-5 py-2.5 bg-gray-900 text-white text-sm font-medium rounded-xl hover:bg-gray-800 transition-colors shadow-sm">
        <Plus class="w-4 h-4" />
        新建计划
      </button>
    </div>

    <!-- Filters bar -->
    <div class="flex items-center gap-3 mb-6">
      <div class="relative flex-1 max-w-sm">
        <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
        <input
          v-model="searchQuery"
          type="text"
          placeholder="搜索计划名称..."
          class="w-full pl-10 pr-4 py-2 bg-white border border-gray-200 rounded-lg text-sm placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-gray-900/10 focus:border-gray-300 transition-all"
        />
      </div>

      <!-- Status tabs -->
      <div class="flex gap-1 bg-white rounded-lg p-1 border border-gray-200">
        <button
          v-for="status in (['ALL', 'ACTIVE', 'DRAFT', 'PAUSED', 'COMPLETED'] as const)"
          :key="status"
          class="px-3 py-1.5 rounded-md text-xs font-medium transition-colors"
          :class="[
            statusFilter === status
              ? 'bg-gray-900 text-white'
              : 'text-gray-500 hover:text-gray-700 hover:bg-gray-50'
          ]"
          @click="statusFilter = status"
        >
          {{ status === 'ALL' ? '全部' : statusLabels[status] }}
          <span v-if="statusCounts[status]" class="ml-1 opacity-70">({{ statusCounts[status] }})</span>
        </button>
      </div>

      <div class="flex-1" />

      <!-- View mode toggle -->
      <div class="flex bg-white rounded-lg p-1 border border-gray-200">
        <button
          class="p-1.5 rounded transition-colors"
          :class="viewMode === 'card' ? 'bg-gray-100 text-gray-700' : 'text-gray-400 hover:text-gray-600'"
          @click="viewMode = 'card'"
        >
          <LayoutGrid class="w-4 h-4" />
        </button>
        <button
          class="p-1.5 rounded transition-colors"
          :class="viewMode === 'list' ? 'bg-gray-100 text-gray-700' : 'text-gray-400 hover:text-gray-600'"
          @click="viewMode = 'list'"
        >
          <List class="w-4 h-4" />
        </button>
      </div>
    </div>

    <!-- Card View -->
    <div v-if="viewMode === 'card'" class="grid grid-cols-2 gap-4">
      <div
        v-for="plan in filteredPlans"
        :key="plan.id"
        class="bg-white rounded-xl p-5 shadow-sm border border-gray-100 hover:shadow-md hover:border-gray-200 transition-all cursor-pointer group"
      >
        <!-- Header -->
        <div class="flex items-start justify-between mb-3">
          <div class="flex-1">
            <div class="flex items-center gap-2 mb-1.5">
              <h3 class="text-sm font-semibold text-gray-900 group-hover:text-gray-700 transition-colors">{{ plan.name }}</h3>
              <span :class="['text-xs px-2 py-0.5 rounded-full font-medium', statusStyles[plan.status]]">
                {{ statusLabels[plan.status] }}
              </span>
            </div>
            <div class="flex items-center gap-3 text-xs text-gray-400">
              <span :class="['px-1.5 py-0.5 rounded', levelColors[plan.inspectionLevel]]">
                {{ levelLabels[plan.inspectionLevel] }}
              </span>
              <span>{{ plan.templateName }}</span>
              <span>{{ plan.scoringMode }}</span>
            </div>
          </div>
          <button class="p-1.5 text-gray-400 hover:text-gray-600 hover:bg-gray-50 rounded-lg opacity-0 group-hover:opacity-100 transition-all">
            <MoreHorizontal class="w-4 h-4" />
          </button>
        </div>

        <!-- Progress -->
        <div class="mb-4">
          <div class="flex items-center justify-between text-xs mb-1">
            <span class="text-gray-500">执行进度</span>
            <span class="text-gray-700 font-medium">{{ plan.completedSessions }}/{{ plan.totalSessions }}</span>
          </div>
          <div class="h-1.5 bg-gray-100 rounded-full overflow-hidden">
            <div
              class="h-full rounded-full transition-all duration-500"
              :class="[
                plan.status === 'COMPLETED' ? 'bg-blue-400' :
                plan.status === 'ACTIVE' ? 'bg-emerald-400' :
                plan.status === 'PAUSED' ? 'bg-amber-400' :
                'bg-gray-300'
              ]"
              :style="{ width: plan.totalSessions > 0 ? (plan.completedSessions / plan.totalSessions * 100) + '%' : '0%' }"
            />
          </div>
        </div>

        <!-- Details -->
        <div class="grid grid-cols-2 gap-3 mb-4 text-xs">
          <div class="flex items-center gap-2 text-gray-500">
            <CalendarDays class="w-3.5 h-3.5 text-gray-400" />
            <span>{{ plan.startDate }} ~ {{ plan.endDate }}</span>
          </div>
          <div class="flex items-center gap-2 text-gray-500">
            <Clock class="w-3.5 h-3.5 text-gray-400" />
            <span>{{ plan.scheduleType }}</span>
          </div>
          <div v-if="plan.lastCheckDate" class="flex items-center gap-2 text-gray-500">
            <CheckCircle2 class="w-3.5 h-3.5 text-gray-400" />
            <span>上次: {{ plan.lastCheckDate }}</span>
          </div>
          <div v-if="plan.nextCheckDate" class="flex items-center gap-2 text-gray-500">
            <ArrowUpRight class="w-3.5 h-3.5 text-gray-400" />
            <span>下次: {{ plan.nextCheckDate }}</span>
          </div>
        </div>

        <!-- Footer -->
        <div class="flex items-center justify-between pt-3 border-t border-gray-100">
          <div class="flex items-center gap-1">
            <Users class="w-3.5 h-3.5 text-gray-400" />
            <div class="flex -space-x-2">
              <div
                v-for="insp in plan.inspectors.slice(0, 3)"
                :key="insp.id"
                class="w-6 h-6 rounded-full bg-gray-200 border-2 border-white flex items-center justify-center text-xs font-medium text-gray-600"
              >{{ insp.name.charAt(0) }}</div>
              <div v-if="plan.inspectors.length > 3"
                class="w-6 h-6 rounded-full bg-gray-100 border-2 border-white flex items-center justify-center text-xs text-gray-400"
              >+{{ plan.inspectors.length - 3 }}</div>
            </div>
            <span v-if="plan.inspectors.length === 0" class="text-xs text-gray-400">未分配</span>
          </div>
          <div v-if="plan.avgDeduction" class="text-xs text-gray-500">
            平均扣分: <span class="font-medium text-red-500">{{ plan.avgDeduction }}分</span>
          </div>
        </div>
      </div>
    </div>

    <!-- List View -->
    <div v-if="viewMode === 'list'" class="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
      <table class="w-full">
        <thead>
          <tr class="text-xs text-gray-400 border-b border-gray-100 bg-gray-50/50">
            <th class="text-left py-3 px-4">计划名称</th>
            <th class="text-left py-3 px-4 w-20">级别</th>
            <th class="text-left py-3 px-4 w-20">状态</th>
            <th class="text-left py-3 px-4 w-32">时间范围</th>
            <th class="text-center py-3 px-4 w-24">进度</th>
            <th class="text-center py-3 px-4 w-20">检查员</th>
            <th class="text-center py-3 px-4 w-24">平均扣分</th>
            <th class="text-right py-3 px-4 w-20">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="plan in filteredPlans" :key="plan.id"
            class="border-b border-gray-50 hover:bg-gray-50/50 transition-colors cursor-pointer"
          >
            <td class="py-3 px-4">
              <div class="text-sm font-medium text-gray-800">{{ plan.name }}</div>
              <div class="text-xs text-gray-400 mt-0.5">{{ plan.templateName }} · {{ plan.scoringMode }}</div>
            </td>
            <td class="py-3 px-4">
              <span :class="['text-xs px-1.5 py-0.5 rounded', levelColors[plan.inspectionLevel]]">
                {{ levelLabels[plan.inspectionLevel] }}
              </span>
            </td>
            <td class="py-3 px-4">
              <span :class="['text-xs px-2 py-0.5 rounded-full font-medium', statusStyles[plan.status]]">
                {{ statusLabels[plan.status] }}
              </span>
            </td>
            <td class="py-3 px-4 text-xs text-gray-500">
              {{ plan.startDate.slice(5) }} ~ {{ plan.endDate.slice(5) }}
            </td>
            <td class="py-3 px-4 text-center">
              <span class="text-xs font-medium text-gray-700">{{ plan.completedSessions }}/{{ plan.totalSessions }}</span>
            </td>
            <td class="py-3 px-4 text-center">
              <span class="text-xs text-gray-500">{{ plan.inspectors.length }}人</span>
            </td>
            <td class="py-3 px-4 text-center">
              <span v-if="plan.avgDeduction" class="text-xs font-medium text-red-500">{{ plan.avgDeduction }}</span>
              <span v-else class="text-xs text-gray-300">-</span>
            </td>
            <td class="py-3 px-4 text-right">
              <div class="flex items-center justify-end gap-1">
                <button class="p-1.5 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded"><Eye class="w-3.5 h-3.5" /></button>
                <button class="p-1.5 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded"><Edit3 class="w-3.5 h-3.5" /></button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
