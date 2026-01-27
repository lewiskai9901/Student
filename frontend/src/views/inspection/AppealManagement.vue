<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  MessageSquareWarning, Clock, CheckCircle2, XCircle, Eye, Send,
  Filter, Search, ChevronRight, Camera, FileText, User, Calendar,
  AlertTriangle, ThumbsUp, ThumbsDown, RotateCcw, History, MessageCircle
} from 'lucide-vue-next'
import { ElSelect, ElOption } from 'element-plus'

// ─── Types ───
type AppealStatus = 'PENDING' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED' | 'WITHDRAWN' | 'RESUBMITTED' | 'FINAL_REJECTED'

interface Appeal {
  id: number
  appealCode: string
  className: string
  classTeacher: string
  sessionName: string
  checkDate: string
  deductionItemName: string
  deductionCategory: string
  originalScore: number
  appealReason: string
  evidencePhotos: number
  status: AppealStatus
  createdAt: string
  reviewerName?: string
  reviewComment?: string
  reviewedAt?: string
  resultScore?: number
}

// ─── State ───
const activeTab = ref<AppealStatus | 'ALL'>('ALL')
const searchQuery = ref('')
const selectedAppeal = ref<Appeal | null>(null)
const showDetail = ref(false)

// ─── Mock Data ───
const appeals: Appeal[] = [
  {
    id: 1, appealCode: 'APL-20260127-001', className: '计算机2班', classTeacher: '李老师',
    sessionName: '日常检查 2026-01-27', checkDate: '2026-01-27',
    deductionItemName: '地面不洁', deductionCategory: '卫生', originalScore: 2.0,
    appealReason: '检查时教室正在进行大扫除，地面未干，并非未清扫。已拍照为证。',
    evidencePhotos: 2, status: 'PENDING', createdAt: '2026-01-27 10:30',
  },
  {
    id: 2, appealCode: 'APL-20260126-003', className: '电商1班', classTeacher: '王老师',
    sessionName: '日常检查 2026-01-26', checkDate: '2026-01-26',
    deductionItemName: '迟到', deductionCategory: '纪律', originalScore: 2.0,
    appealReason: '该学生已提前请假，有辅导员签字的请假条。请核实。',
    evidencePhotos: 1, status: 'UNDER_REVIEW', createdAt: '2026-01-26 14:20',
    reviewerName: '张检查员',
  },
  {
    id: 3, appealCode: 'APL-20260125-002', className: '会计1班', classTeacher: '孙老师',
    sessionName: '日常检查 2026-01-25', checkDate: '2026-01-25',
    deductionItemName: '物品乱放', deductionCategory: '卫生', originalScore: 1.0,
    appealReason: '检查时学生正在收拾物品，非日常状态。',
    evidencePhotos: 0, status: 'REJECTED', createdAt: '2026-01-25 11:00',
    reviewerName: '王检查员', reviewComment: '检查时间为规定时段内，物品状态以检查时为准。', reviewedAt: '2026-01-25 15:30',
  },
  {
    id: 4, appealCode: 'APL-20260124-001', className: '软件1班', classTeacher: '赵老师',
    sessionName: '日常检查 2026-01-24', checkDate: '2026-01-24',
    deductionItemName: '被褥未叠', deductionCategory: '卫生', originalScore: 1.5,
    appealReason: '该宿舍有2人当天有早课，出门较早，确实未来得及整理。但已在检查结束前完成整理。',
    evidencePhotos: 2, status: 'APPROVED', createdAt: '2026-01-24 09:50',
    reviewerName: '张检查员', reviewComment: '核实属实，接受申诉，扣分减半。', reviewedAt: '2026-01-24 16:00',
    resultScore: 0.75,
  },
  {
    id: 5, appealCode: 'APL-20260123-004', className: '物流2班', classTeacher: '周老师',
    sessionName: '日常检查 2026-01-23', checkDate: '2026-01-23',
    deductionItemName: '课间打闹', deductionCategory: '纪律', originalScore: 2.0,
    appealReason: '学生是在进行体育活动练习，并非打闹，有体育老师可以证明。',
    evidencePhotos: 0, status: 'RESUBMITTED', createdAt: '2026-01-23 13:15',
  },
  {
    id: 6, appealCode: 'APL-20260122-002', className: '计算机3班', classTeacher: '吴老师',
    sessionName: '日常检查 2026-01-22', checkDate: '2026-01-22',
    deductionItemName: '违规电器', deductionCategory: '安全', originalScore: 5.0,
    appealReason: '该电器为学校统一配发的小型台灯，功率在允许范围内。',
    evidencePhotos: 1, status: 'FINAL_REJECTED', createdAt: '2026-01-22 08:30',
    reviewerName: '刘检查员', reviewComment: '经复核，该电器非学校配发，功率超标。维持原判。', reviewedAt: '2026-01-23 10:00',
  },
  {
    id: 7, appealCode: 'APL-20260121-001', className: '电商2班', classTeacher: '陈老师',
    sessionName: '日常检查 2026-01-21', checkDate: '2026-01-21',
    deductionItemName: '地面不洁', deductionCategory: '卫生', originalScore: 2.0,
    appealReason: '当天下雨，走廊进水导致地面不洁，非班级卫生问题。',
    evidencePhotos: 3, status: 'WITHDRAWN', createdAt: '2026-01-21 14:00',
  },
]

const statusLabels: Record<AppealStatus, string> = {
  PENDING: '待审核',
  UNDER_REVIEW: '审核中',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  WITHDRAWN: '已撤回',
  RESUBMITTED: '重新提交',
  FINAL_REJECTED: '终审驳回',
}

const statusStyles: Record<AppealStatus, { bg: string; text: string; dot: string }> = {
  PENDING: { bg: 'bg-amber-50', text: 'text-amber-600', dot: 'bg-amber-400' },
  UNDER_REVIEW: { bg: 'bg-blue-50', text: 'text-blue-600', dot: 'bg-blue-400' },
  APPROVED: { bg: 'bg-emerald-50', text: 'text-emerald-600', dot: 'bg-emerald-400' },
  REJECTED: { bg: 'bg-red-50', text: 'text-red-600', dot: 'bg-red-400' },
  WITHDRAWN: { bg: 'bg-gray-50', text: 'text-gray-500', dot: 'bg-gray-400' },
  RESUBMITTED: { bg: 'bg-violet-50', text: 'text-violet-600', dot: 'bg-violet-400' },
  FINAL_REJECTED: { bg: 'bg-red-100', text: 'text-red-700', dot: 'bg-red-500' },
}

const tabs: { key: AppealStatus | 'ALL'; label: string }[] = [
  { key: 'ALL', label: '全部' },
  { key: 'PENDING', label: '待审核' },
  { key: 'UNDER_REVIEW', label: '审核中' },
  { key: 'APPROVED', label: '已通过' },
  { key: 'REJECTED', label: '已驳回' },
  { key: 'RESUBMITTED', label: '重新提交' },
]

const filteredAppeals = computed(() => {
  return appeals.filter(a => {
    if (activeTab.value !== 'ALL' && a.status !== activeTab.value) return false
    if (searchQuery.value && !a.className.includes(searchQuery.value) && !a.deductionItemName.includes(searchQuery.value)) return false
    return true
  })
})

const tabCounts = computed(() => {
  const counts: Record<string, number> = { ALL: appeals.length }
  appeals.forEach(a => { counts[a.status] = (counts[a.status] || 0) + 1 })
  return counts
})

function viewDetail(appeal: Appeal) {
  selectedAppeal.value = appeal
  showDetail.value = true
}
</script>

<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <!-- Header -->
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-gray-900 mb-1">申诉管理</h1>
      <p class="text-sm text-gray-500">审核和处理检查结果申诉，维护量化评价公平性</p>
    </div>

    <!-- Summary Cards -->
    <div class="grid grid-cols-4 gap-4 mb-6">
      <div class="bg-white rounded-xl p-4 shadow-sm border border-gray-100">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-lg bg-amber-50 flex items-center justify-center">
            <Clock class="w-5 h-5 text-amber-500" />
          </div>
          <div>
            <div class="text-2xl font-bold text-gray-900">{{ appeals.filter(a => a.status === 'PENDING' || a.status === 'RESUBMITTED').length }}</div>
            <div class="text-xs text-gray-400">待处理</div>
          </div>
        </div>
      </div>
      <div class="bg-white rounded-xl p-4 shadow-sm border border-gray-100">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-lg bg-blue-50 flex items-center justify-center">
            <Eye class="w-5 h-5 text-blue-500" />
          </div>
          <div>
            <div class="text-2xl font-bold text-gray-900">{{ appeals.filter(a => a.status === 'UNDER_REVIEW').length }}</div>
            <div class="text-xs text-gray-400">审核中</div>
          </div>
        </div>
      </div>
      <div class="bg-white rounded-xl p-4 shadow-sm border border-gray-100">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-lg bg-emerald-50 flex items-center justify-center">
            <CheckCircle2 class="w-5 h-5 text-emerald-500" />
          </div>
          <div>
            <div class="text-2xl font-bold text-gray-900">{{ appeals.filter(a => a.status === 'APPROVED').length }}</div>
            <div class="text-xs text-gray-400">已通过</div>
          </div>
        </div>
      </div>
      <div class="bg-white rounded-xl p-4 shadow-sm border border-gray-100">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-lg bg-gray-50 flex items-center justify-center">
            <History class="w-5 h-5 text-gray-500" />
          </div>
          <div>
            <div class="text-2xl font-bold text-gray-900">{{ appeals.length }}</div>
            <div class="text-xs text-gray-400">总申诉数</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Filters -->
    <div class="flex items-center gap-3 mb-6">
      <!-- Tabs -->
      <div class="flex gap-1 bg-white rounded-lg p-1 border border-gray-200">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          class="px-3 py-1.5 rounded-md text-xs font-medium transition-colors"
          :class="[
            activeTab === tab.key
              ? 'bg-gray-900 text-white'
              : 'text-gray-500 hover:text-gray-700 hover:bg-gray-50'
          ]"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
          <span v-if="tabCounts[tab.key]" class="ml-1 opacity-70">({{ tabCounts[tab.key] }})</span>
        </button>
      </div>
      <div class="flex-1" />
      <div class="relative max-w-xs">
        <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
        <input
          v-model="searchQuery"
          type="text"
          placeholder="搜索班级或扣分项..."
          class="w-full pl-10 pr-4 py-2 bg-white border border-gray-200 rounded-lg text-sm placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-gray-900/10 focus:border-gray-300 transition-all"
        />
      </div>
    </div>

    <!-- Appeals List -->
    <div class="space-y-3">
      <div
        v-for="appeal in filteredAppeals"
        :key="appeal.id"
        class="bg-white rounded-xl p-5 shadow-sm border border-gray-100 hover:shadow-md transition-shadow cursor-pointer"
        @click="viewDetail(appeal)"
      >
        <div class="flex items-start gap-4">
          <!-- Status indicator -->
          <div class="mt-1">
            <div :class="['w-3 h-3 rounded-full', statusStyles[appeal.status].dot]" />
          </div>

          <!-- Main content -->
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-3 mb-2">
              <span class="text-xs text-gray-400 font-mono">{{ appeal.appealCode }}</span>
              <span :class="['text-xs px-2 py-0.5 rounded-full font-medium', statusStyles[appeal.status].bg, statusStyles[appeal.status].text]">
                {{ statusLabels[appeal.status] }}
              </span>
            </div>

            <div class="flex items-center gap-4 mb-2">
              <span class="text-sm font-semibold text-gray-900">{{ appeal.className }}</span>
              <span class="text-xs text-gray-400">{{ appeal.classTeacher }}</span>
              <span class="text-xs px-2 py-0.5 rounded bg-gray-100 text-gray-500">{{ appeal.deductionCategory }}</span>
              <span class="text-sm text-red-600 font-medium">{{ appeal.deductionItemName }} -{{ appeal.originalScore }}分</span>
              <span v-if="appeal.resultScore !== undefined" class="text-sm text-emerald-600 font-medium">→ -{{ appeal.resultScore }}分</span>
            </div>

            <p class="text-sm text-gray-600 line-clamp-2 mb-2">{{ appeal.appealReason }}</p>

            <div class="flex items-center gap-4 text-xs text-gray-400">
              <span class="flex items-center gap-1">
                <Calendar class="w-3 h-3" />
                {{ appeal.sessionName }}
              </span>
              <span class="flex items-center gap-1">
                <Clock class="w-3 h-3" />
                提交于 {{ appeal.createdAt }}
              </span>
              <span v-if="appeal.evidencePhotos > 0" class="flex items-center gap-1">
                <Camera class="w-3 h-3" />
                {{ appeal.evidencePhotos }}张证据
              </span>
              <span v-if="appeal.reviewerName" class="flex items-center gap-1">
                <User class="w-3 h-3" />
                {{ appeal.reviewerName }}
              </span>
            </div>

            <!-- Review result -->
            <div v-if="appeal.reviewComment" class="mt-3 p-3 bg-gray-50 rounded-lg border border-gray-100">
              <div class="flex items-center gap-2 mb-1">
                <MessageCircle class="w-3.5 h-3.5 text-gray-400" />
                <span class="text-xs font-medium text-gray-600">审核意见</span>
                <span class="text-xs text-gray-400">{{ appeal.reviewedAt }}</span>
              </div>
              <p class="text-sm text-gray-600">{{ appeal.reviewComment }}</p>
            </div>
          </div>

          <!-- Actions -->
          <div class="flex flex-col gap-2">
            <button
              v-if="appeal.status === 'PENDING' || appeal.status === 'RESUBMITTED'"
              class="flex items-center gap-1.5 px-3 py-1.5 bg-emerald-50 text-emerald-600 text-xs font-medium rounded-lg hover:bg-emerald-100 transition-colors"
            >
              <ThumbsUp class="w-3.5 h-3.5" />
              通过
            </button>
            <button
              v-if="appeal.status === 'PENDING' || appeal.status === 'RESUBMITTED'"
              class="flex items-center gap-1.5 px-3 py-1.5 bg-red-50 text-red-600 text-xs font-medium rounded-lg hover:bg-red-100 transition-colors"
            >
              <ThumbsDown class="w-3.5 h-3.5" />
              驳回
            </button>
            <button
              v-if="appeal.status === 'UNDER_REVIEW'"
              class="flex items-center gap-1.5 px-3 py-1.5 bg-blue-50 text-blue-600 text-xs font-medium rounded-lg hover:bg-blue-100 transition-colors"
            >
              <FileText class="w-3.5 h-3.5" />
              处理
            </button>
            <button
              class="flex items-center gap-1.5 px-3 py-1.5 bg-gray-50 text-gray-500 text-xs font-medium rounded-lg hover:bg-gray-100 transition-colors"
            >
              <Eye class="w-3.5 h-3.5" />
              详情
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Empty state -->
    <div v-if="filteredAppeals.length === 0" class="text-center py-16">
      <MessageSquareWarning class="w-12 h-12 text-gray-300 mx-auto mb-3" />
      <p class="text-sm text-gray-400">暂无符合条件的申诉记录</p>
    </div>
  </div>
</template>
