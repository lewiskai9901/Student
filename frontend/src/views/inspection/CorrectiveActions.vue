<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  ClipboardCheck, Clock, AlertTriangle, CheckCircle2, XCircle,
  ChevronRight, Camera, Send, Filter, Plus, Eye, RotateCcw,
  ArrowUpRight, FileWarning, Timer, User
} from 'lucide-vue-next'

// ─── Types ───
type ActionStatus = 'OPEN' | 'IN_PROGRESS' | 'REVIEW' | 'CLOSED' | 'OVERDUE' | 'ESCALATED'
type Severity = 'MINOR' | 'MODERATE' | 'SEVERE' | 'CRITICAL'

interface CorrectiveAction {
  id: number
  actionCode: string
  title: string
  description: string
  severity: Severity
  category: string
  className: string
  assigneeName: string
  dueDate: string
  daysTillDue: number
  status: ActionStatus
  sourceName: string
  sourceDate: string
  inspectorName: string
  photoCount: number
  resolutionPhotoCount: number
  resolution?: string
  createdAt: string
}

// ─── State ───
const activeTab = ref<ActionStatus | 'ALL'>('ALL')
const selectedSeverity = ref('')
const selectedAction = ref<CorrectiveAction | null>(null)
const showDetailPanel = ref(false)

// ─── Mock Data ───
const actions: CorrectiveAction[] = [
  { id: 1, actionCode: 'CA-20260127-001', title: '301宿舍地面不洁', description: '301宿舍地面存在明显灰尘和纸屑，未按要求清扫', severity: 'SEVERE', category: '卫生', className: '计算机2班', assigneeName: '李老师', dueDate: '2026-01-28', daysTillDue: 1, status: 'OPEN', sourceName: '日常检查 2026-01-27', sourceDate: '2026-01-27', inspectorName: '张检查员', photoCount: 2, resolutionPhotoCount: 0, createdAt: '2026-01-27 08:30' },
  { id: 2, actionCode: 'CA-20260126-003', title: '教室黑板未擦', description: '教学楼A301教室黑板课后未擦，影响下一节课使用', severity: 'MINOR', category: '卫生', className: '电商1班', assigneeName: '王老师', dueDate: '2026-01-29', daysTillDue: 2, status: 'REVIEW', sourceName: '日常检查 2026-01-26', sourceDate: '2026-01-26', inspectorName: '张检查员', photoCount: 1, resolutionPhotoCount: 1, resolution: '已安排值日生重新清扫，并提醒班级值日生注意', createdAt: '2026-01-26 09:15' },
  { id: 3, actionCode: 'CA-20260125-002', title: '205宿舍物品乱放', description: '205宿舍桌面物品杂乱，衣物随意堆放', severity: 'MODERATE', category: '卫生', className: '软件1班', assigneeName: '赵老师', dueDate: '2026-01-27', daysTillDue: 0, status: 'IN_PROGRESS', sourceName: '日常检查 2026-01-25', sourceDate: '2026-01-25', inspectorName: '王检查员', photoCount: 2, resolutionPhotoCount: 0, createdAt: '2026-01-25 10:00' },
  { id: 4, actionCode: 'CA-20260124-001', title: '违规使用大功率电器', description: '303宿舍发现大功率吹风机，存在安全隐患', severity: 'CRITICAL', category: '安全', className: '会计1班', assigneeName: '孙老师', dueDate: '2026-01-25', daysTillDue: -2, status: 'OVERDUE', sourceName: '专项检查 2026-01-24', sourceDate: '2026-01-24', inspectorName: '刘检查员', photoCount: 1, resolutionPhotoCount: 0, createdAt: '2026-01-24 14:20' },
  { id: 5, actionCode: 'CA-20260123-005', title: '教室窗户未关', description: '教学楼B201教室放学后窗户未关闭', severity: 'MINOR', category: '安全', className: '物流1班', assigneeName: '周老师', dueDate: '2026-01-25', daysTillDue: -2, status: 'CLOSED', sourceName: '日常检查 2026-01-23', sourceDate: '2026-01-23', inspectorName: '张检查员', photoCount: 1, resolutionPhotoCount: 1, resolution: '已关闭窗户，并与班级安全委员沟通加强检查', createdAt: '2026-01-23 16:30' },
  { id: 6, actionCode: 'CA-20260122-002', title: '宿舍走廊垃圾堆积', description: '1号楼3层走廊垃圾桶满溢，未及时清理', severity: 'MODERATE', category: '卫生', className: '计算机3班', assigneeName: '李老师', dueDate: '2026-01-24', daysTillDue: -3, status: 'CLOSED', sourceName: '日常检查 2026-01-22', sourceDate: '2026-01-22', inspectorName: '王检查员', photoCount: 1, resolutionPhotoCount: 2, resolution: '已清理垃圾并更换垃圾桶内衬，安排宿舍长每日检查', createdAt: '2026-01-22 08:45' },
]

const tabs = [
  { key: 'ALL', label: '全部', count: actions.length },
  { key: 'OPEN', label: '待处理', count: actions.filter(a => a.status === 'OPEN').length },
  { key: 'IN_PROGRESS', label: '进行中', count: actions.filter(a => a.status === 'IN_PROGRESS').length },
  { key: 'REVIEW', label: '待复核', count: actions.filter(a => a.status === 'REVIEW').length },
  { key: 'CLOSED', label: '已完成', count: actions.filter(a => a.status === 'CLOSED').length },
  { key: 'OVERDUE', label: '已超期', count: actions.filter(a => a.status === 'OVERDUE').length },
]

const filteredActions = computed(() => {
  let result = actions
  if (activeTab.value !== 'ALL') result = result.filter(a => a.status === activeTab.value)
  if (selectedSeverity.value) result = result.filter(a => a.severity === selectedSeverity.value)
  return result
})

// ─── Helpers ───
function getStatusStyle(status: ActionStatus) {
  const map: Record<ActionStatus, { bg: string; text: string; label: string }> = {
    OPEN: { bg: 'bg-amber-100', text: 'text-amber-700', label: '待处理' },
    IN_PROGRESS: { bg: 'bg-blue-100', text: 'text-blue-700', label: '进行中' },
    REVIEW: { bg: 'bg-purple-100', text: 'text-purple-700', label: '待复核' },
    CLOSED: { bg: 'bg-emerald-100', text: 'text-emerald-700', label: '已完成' },
    OVERDUE: { bg: 'bg-red-100', text: 'text-red-700', label: '已超期' },
    ESCALATED: { bg: 'bg-red-100', text: 'text-red-700', label: '已升级' },
  }
  return map[status]
}

function getSeverityConfig(severity: Severity) {
  const map: Record<Severity, { bg: string; text: string; label: string; dot: string }> = {
    CRITICAL: { bg: 'bg-red-100', text: 'text-red-700', label: '紧急', dot: 'bg-red-500' },
    SEVERE: { bg: 'bg-red-100', text: 'text-red-700', label: '严重', dot: 'bg-red-500' },
    MODERATE: { bg: 'bg-amber-100', text: 'text-amber-700', label: '一般', dot: 'bg-amber-500' },
    MINOR: { bg: 'bg-gray-100', text: 'text-gray-600', label: '轻微', dot: 'bg-gray-400' },
  }
  return map[severity]
}

function getDueDateStyle(days: number) {
  if (days < 0) return 'text-red-600 font-semibold'
  if (days <= 1) return 'text-amber-600 font-semibold'
  return 'text-gray-500'
}

function getDueDateLabel(days: number) {
  if (days < 0) return `已超期${Math.abs(days)}天`
  if (days === 0) return '今天截止'
  return `剩余${days}天`
}

function openDetail(action: CorrectiveAction) {
  selectedAction.value = action
  showDetailPanel.value = true
}
</script>

<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <!-- Header -->
    <div class="mb-6 flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">整改工单管理</h1>
        <p class="mt-1 text-sm text-gray-500">跟踪检查发现的问题，确保整改闭环</p>
      </div>
      <button class="flex items-center gap-1.5 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700">
        <Plus class="h-4 w-4" />
        手动创建工单
      </button>
    </div>

    <!-- Summary cards -->
    <div class="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <div class="rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-xs text-gray-500">待处理</p>
            <p class="mt-1 text-2xl font-bold text-amber-600">{{ actions.filter(a => a.status === 'OPEN').length }}</p>
          </div>
          <div class="rounded-xl bg-amber-100 p-2.5"><Clock class="h-5 w-5 text-amber-600" /></div>
        </div>
      </div>
      <div class="rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-xs text-gray-500">进行中</p>
            <p class="mt-1 text-2xl font-bold text-blue-600">{{ actions.filter(a => a.status === 'IN_PROGRESS').length }}</p>
          </div>
          <div class="rounded-xl bg-blue-100 p-2.5"><RotateCcw class="h-5 w-5 text-blue-600" /></div>
        </div>
      </div>
      <div class="rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-xs text-gray-500">已超期</p>
            <p class="mt-1 text-2xl font-bold text-red-600">{{ actions.filter(a => a.status === 'OVERDUE').length }}</p>
          </div>
          <div class="rounded-xl bg-red-100 p-2.5"><AlertTriangle class="h-5 w-5 text-red-600" /></div>
        </div>
      </div>
      <div class="rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-xs text-gray-500">本月完成率</p>
            <p class="mt-1 text-2xl font-bold text-emerald-600">78.5%</p>
          </div>
          <div class="rounded-xl bg-emerald-100 p-2.5"><CheckCircle2 class="h-5 w-5 text-emerald-600" /></div>
        </div>
      </div>
    </div>

    <!-- Tabs & Filters -->
    <div class="mb-4 flex items-center justify-between">
      <div class="flex gap-1 rounded-lg border border-gray-200 bg-gray-100 p-1">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          :class="[
            'flex items-center gap-1.5 rounded-md px-3 py-1.5 text-xs font-medium transition-all',
            activeTab === tab.key
              ? 'bg-white text-gray-900 shadow-sm'
              : 'text-gray-500 hover:text-gray-700'
          ]"
          @click="activeTab = tab.key as any"
        >
          {{ tab.label }}
          <span v-if="tab.count > 0 && tab.key !== 'ALL'" :class="[
            'rounded-full px-1.5 py-0.5 text-[10px] font-bold',
            activeTab === tab.key ? 'bg-blue-100 text-blue-700' : 'bg-gray-200 text-gray-500'
          ]">
            {{ tab.count }}
          </span>
        </button>
      </div>
      <div class="flex items-center gap-2">
        <select v-model="selectedSeverity" class="h-8 rounded-lg border border-gray-300 px-2.5 text-xs text-gray-600 focus:border-blue-500 focus:outline-none">
          <option value="">全部严重程度</option>
          <option value="CRITICAL">紧急</option>
          <option value="SEVERE">严重</option>
          <option value="MODERATE">一般</option>
          <option value="MINOR">轻微</option>
        </select>
      </div>
    </div>

    <!-- Action Cards -->
    <div class="space-y-3">
      <div
        v-for="action in filteredActions"
        :key="action.id"
        class="group rounded-xl border border-gray-200 bg-white shadow-sm transition-all hover:shadow-md"
      >
        <div class="p-5">
          <!-- Header row -->
          <div class="flex items-start justify-between">
            <div class="flex items-start gap-3">
              <div :class="['mt-0.5 h-2.5 w-2.5 flex-shrink-0 rounded-full', getSeverityConfig(action.severity).dot]"></div>
              <div>
                <div class="flex items-center gap-2">
                  <h3 class="text-sm font-semibold text-gray-900">{{ action.title }}</h3>
                  <span :class="['rounded-full px-2 py-0.5 text-[10px] font-semibold', getSeverityConfig(action.severity).bg, getSeverityConfig(action.severity).text]">
                    {{ getSeverityConfig(action.severity).label }}
                  </span>
                </div>
                <p class="mt-1 text-xs text-gray-500">{{ action.actionCode }} | {{ action.className }} | {{ action.category }}</p>
              </div>
            </div>
            <span :class="['rounded-full px-2.5 py-1 text-[10px] font-bold', getStatusStyle(action.status).bg, getStatusStyle(action.status).text]">
              {{ getStatusStyle(action.status).label }}
            </span>
          </div>

          <!-- Description -->
          <p class="mt-3 text-sm text-gray-600">{{ action.description }}</p>

          <!-- Resolution (if exists) -->
          <div v-if="action.resolution" class="mt-3 rounded-lg bg-emerald-50 px-3 py-2">
            <p class="text-xs font-medium text-emerald-700">整改措施: {{ action.resolution }}</p>
          </div>

          <!-- Meta info -->
          <div class="mt-3 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-gray-500">
            <span class="flex items-center gap-1">
              <User class="h-3 w-3" /> 责任人: {{ action.assigneeName }}
            </span>
            <span class="flex items-center gap-1">
              <Timer class="h-3 w-3" />
              截止: {{ action.dueDate }}
              <span :class="getDueDateStyle(action.daysTillDue)">({{ getDueDateLabel(action.daysTillDue) }})</span>
            </span>
            <span class="flex items-center gap-1">
              <Camera class="h-3 w-3" />
              问题照片: {{ action.photoCount }}张
              <template v-if="action.resolutionPhotoCount > 0">
                | 整改照片: {{ action.resolutionPhotoCount }}张
              </template>
            </span>
            <span>来源: {{ action.sourceName }}</span>
          </div>

          <!-- Actions -->
          <div class="mt-4 flex items-center gap-2">
            <button
              class="flex items-center gap-1 rounded-lg border border-gray-300 px-3 py-1.5 text-xs font-medium text-gray-600 hover:bg-gray-50"
              @click="openDetail(action)"
            >
              <Eye class="h-3.5 w-3.5" /> 查看详情
            </button>
            <template v-if="action.status === 'OPEN'">
              <button class="flex items-center gap-1 rounded-lg bg-blue-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-blue-700">
                <ClipboardCheck class="h-3.5 w-3.5" /> 开始处理
              </button>
            </template>
            <template v-if="action.status === 'IN_PROGRESS'">
              <button class="flex items-center gap-1 rounded-lg bg-blue-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-blue-700">
                <Send class="h-3.5 w-3.5" /> 提交整改
              </button>
            </template>
            <template v-if="action.status === 'REVIEW'">
              <button class="flex items-center gap-1 rounded-lg bg-emerald-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-emerald-700">
                <CheckCircle2 class="h-3.5 w-3.5" /> 通过复核
              </button>
              <button class="flex items-center gap-1 rounded-lg border border-red-300 px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50">
                <XCircle class="h-3.5 w-3.5" /> 驳回重做
              </button>
            </template>
            <template v-if="action.status === 'OVERDUE'">
              <button class="flex items-center gap-1 rounded-lg bg-red-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-red-700">
                <ArrowUpRight class="h-3.5 w-3.5" /> 升级处理
              </button>
            </template>
          </div>
        </div>
      </div>

      <!-- Empty state -->
      <div v-if="!filteredActions.length" class="rounded-xl border border-gray-200 bg-white py-16 text-center shadow-sm">
        <FileWarning class="mx-auto mb-3 h-12 w-12 text-gray-300" />
        <p class="text-sm text-gray-500">暂无符合条件的工单</p>
      </div>
    </div>
  </div>
</template>
