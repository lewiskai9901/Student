<template>
  <div class="p-4">
    <!-- 筛选区域 -->
    <div class="mb-4 flex flex-wrap items-center gap-4">
      <div class="flex items-center gap-2">
        <span class="text-sm text-gray-600">年级</span>
        <select
          v-model="query.gradeId"
          class="rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 transition-colors focus:border-emerald-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/20"
        >
          <option :value="undefined">全部年级</option>
          <option v-for="grade in gradeList" :key="grade.id" :value="grade.id">
            {{ grade.gradeName }}
          </option>
        </select>
      </div>

      <div class="flex items-center gap-2">
        <span class="text-sm text-gray-600">班级</span>
        <select
          v-model="query.classId"
          class="rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 transition-colors focus:border-emerald-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/20"
        >
          <option :value="undefined">全部班级</option>
          <option v-for="cls in classList" :key="cls.id" :value="cls.id">
            {{ cls.className }}
          </option>
        </select>
      </div>

      <div class="flex items-center gap-2">
        <span class="text-sm text-gray-600">状态</span>
        <select
          v-model="query.status"
          class="rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 transition-colors focus:border-emerald-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/20"
        >
          <option :value="undefined">全部状态</option>
          <option :value="1">待审核</option>
          <option :value="2">通过</option>
          <option :value="3">驳回</option>
          <option :value="6">公示中</option>
          <option :value="7">已生效</option>
        </select>
      </div>

      <div class="flex items-center gap-2">
        <button
          @click="loadAppeals"
          class="flex items-center gap-2 rounded-lg bg-emerald-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-emerald-700"
        >
          <Search class="h-4 w-4" />
          查询
        </button>
        <button
          @click="handleReset"
          class="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50"
        >
          重置
        </button>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="overflow-hidden rounded-xl border border-gray-200">
      <!-- 加载状态 -->
      <div v-if="loading" class="flex items-center justify-center py-16">
        <Loader2 class="h-8 w-8 animate-spin text-emerald-600" />
      </div>

      <!-- 空状态 -->
      <div v-else-if="appealList.length === 0" class="flex flex-col items-center justify-center py-16 text-gray-400">
        <FileSearch class="h-12 w-12" />
        <p class="mt-3 text-sm">暂无申诉记录</p>
      </div>

      <!-- 表格 -->
      <table v-else class="w-full">
        <thead>
          <tr class="bg-gray-50 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
            <th class="px-4 py-3">申诉编号</th>
            <th class="px-4 py-3">班级</th>
            <th class="px-4 py-3">申请人</th>
            <th class="px-4 py-3">申诉原因</th>
            <th class="px-4 py-3 text-center">原始分数</th>
            <th class="px-4 py-3 text-center">调整后分数</th>
            <th class="px-4 py-3 text-center">状态</th>
            <th class="px-4 py-3">申请时间</th>
            <th class="px-4 py-3 text-center">操作</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100">
          <tr
            v-for="(row, index) in appealList"
            :key="row.id"
            class="transition-colors hover:bg-gray-50"
            :style="{ animationDelay: `${index * 30}ms` }"
          >
            <td class="px-4 py-3 text-sm font-medium text-gray-900">{{ row.appealCode }}</td>
            <td class="px-4 py-3 text-sm text-gray-600">{{ row.className }}</td>
            <td class="px-4 py-3 text-sm text-gray-600">{{ row.applicantName }}</td>
            <td class="px-4 py-3 text-sm text-gray-600">
              <span class="line-clamp-1" :title="row.reason">{{ row.reason }}</span>
            </td>
            <td class="px-4 py-3 text-center">
              <span class="rounded-full bg-red-100 px-2 py-1 text-xs font-semibold text-red-700">
                {{ row.originalScore }}
              </span>
            </td>
            <td class="px-4 py-3 text-center">
              <span
                v-if="row.adjustedScore !== null"
                class="rounded-full bg-green-100 px-2 py-1 text-xs font-semibold text-green-700"
              >
                {{ row.adjustedScore }}
              </span>
              <span v-else class="text-gray-400">-</span>
            </td>
            <td class="px-4 py-3 text-center">
              <span
                class="inline-flex rounded-full px-2 py-1 text-xs font-medium"
                :class="getStatusClass(row.status)"
              >
                {{ getStatusText(row.status) }}
              </span>
            </td>
            <td class="px-4 py-3 text-sm text-gray-500">{{ row.createdAt }}</td>
            <td class="px-4 py-3 text-center">
              <div class="flex items-center justify-center gap-2">
                <button
                  @click="handleView(row)"
                  class="text-sm text-emerald-600 hover:text-emerald-700"
                >
                  详情
                </button>
                <button
                  v-if="row.status === 1 && canReview"
                  @click="handleReview(row)"
                  class="text-sm text-amber-600 hover:text-amber-700"
                >
                  审核
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 分页 -->
    <div v-if="total > 0" class="mt-4 flex items-center justify-between">
      <span class="text-sm text-gray-500">共 {{ total }} 条记录</span>
      <div class="flex items-center gap-2">
        <select
          v-model="query.pageSize"
          @change="loadAppeals"
          class="rounded-lg border border-gray-300 bg-white px-2 py-1.5 text-sm"
        >
          <option :value="10">10条/页</option>
          <option :value="20">20条/页</option>
          <option :value="50">50条/页</option>
          <option :value="100">100条/页</option>
        </select>

        <div class="flex items-center gap-1">
          <button
            @click="query.pageNum = Math.max(1, query.pageNum - 1); loadAppeals()"
            :disabled="query.pageNum <= 1"
            class="rounded-lg border border-gray-300 p-1.5 transition-colors hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
          >
            <ChevronLeft class="h-4 w-4" />
          </button>
          <span class="px-3 text-sm text-gray-600">{{ query.pageNum }} / {{ Math.ceil(total / query.pageSize) }}</span>
          <button
            @click="query.pageNum = Math.min(Math.ceil(total / query.pageSize), query.pageNum + 1); loadAppeals()"
            :disabled="query.pageNum >= Math.ceil(total / query.pageSize)"
            class="rounded-lg border border-gray-300 p-1.5 transition-colors hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
          >
            <ChevronRight class="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>

    <!-- 审核对话框 -->
    <Teleport to="body">
      <div
        v-if="reviewDialogVisible"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm"
        @click.self="reviewDialogVisible = false"
      >
        <div class="animate-modal-in w-full max-w-xl rounded-2xl bg-white shadow-2xl">
          <div class="flex items-center justify-between border-b border-gray-100 px-6 py-4">
            <h3 class="text-lg font-semibold text-gray-900">审核申诉</h3>
            <button
              @click="reviewDialogVisible = false"
              class="rounded-lg p-2 text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-600"
            >
              <X class="h-5 w-5" />
            </button>
          </div>

          <div class="max-h-[70vh] overflow-y-auto p-6">
            <!-- 申诉信息 -->
            <div v-if="currentReviewAppeal" class="mb-6 space-y-4">
              <!-- 基本信息 -->
              <div class="rounded-xl bg-gray-50 p-4">
                <h4 class="mb-3 text-sm font-semibold text-gray-700">申诉信息</h4>
                <div class="grid grid-cols-2 gap-3 text-sm">
                  <div>
                    <span class="text-gray-500">申诉编号：</span>
                    <span class="font-medium text-gray-900">{{ currentReviewAppeal.appealCode }}</span>
                  </div>
                  <div>
                    <span class="text-gray-500">班级：</span>
                    <span class="font-medium text-gray-900">{{ currentReviewAppeal.className }}</span>
                  </div>
                  <div>
                    <span class="text-gray-500">申诉人：</span>
                    <span class="font-medium text-gray-900">{{ currentReviewAppeal.applicantName || '-' }}</span>
                  </div>
                  <div>
                    <span class="text-gray-500">申诉时间：</span>
                    <span class="font-medium text-gray-900">{{ currentReviewAppeal.createdAt }}</span>
                  </div>
                </div>
              </div>

              <!-- 申诉原因 -->
              <div class="rounded-xl bg-amber-50 p-4">
                <h4 class="mb-2 text-sm font-semibold text-amber-700">申诉原因</h4>
                <p class="whitespace-pre-wrap text-sm text-gray-700">{{ currentReviewAppeal.reason || '无' }}</p>
              </div>

              <!-- 分数信息 -->
              <div class="grid grid-cols-2 gap-4">
                <div class="rounded-xl border border-red-200 bg-red-50 p-4 text-center">
                  <div class="mb-1 text-sm text-red-600">原始扣分</div>
                  <div class="text-2xl font-bold text-red-700">{{ currentReviewAppeal.originalScore ?? '-' }}</div>
                </div>
                <div class="rounded-xl border border-blue-200 bg-blue-50 p-4 text-center">
                  <div class="mb-1 text-sm text-blue-600">期望扣分</div>
                  <div class="text-2xl font-bold text-blue-700">{{ currentReviewAppeal.expectedScore ?? '-' }}</div>
                </div>
              </div>
            </div>

            <!-- 审核表单 -->
            <div class="space-y-4">
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">
                  调整后扣分
                </label>
                <input
                  v-model.number="reviewForm.adjustedScore"
                  type="number"
                  step="0.5"
                  min="0"
                  :placeholder="currentReviewAppeal?.expectedScore !== null ? `建议值: ${currentReviewAppeal?.expectedScore}` : '请输入调整后的扣分'"
                  class="w-full rounded-lg border border-gray-300 px-4 py-2.5 text-gray-900 transition-colors focus:border-emerald-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/20"
                />
                <p class="mt-1 text-xs text-gray-500">若通过申诉，可修改扣分值；若不修改则保持原扣分</p>
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">
                  审核意见 <span class="text-red-500">*</span>
                </label>
                <textarea
                  v-model="reviewForm.opinion"
                  rows="3"
                  placeholder="请输入审核意见..."
                  class="w-full rounded-lg border border-gray-300 px-4 py-2.5 text-gray-900 transition-colors focus:border-emerald-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/20"
                ></textarea>
              </div>
            </div>
          </div>

          <div class="flex justify-end gap-3 border-t border-gray-100 px-6 py-4">
            <button
              @click="reviewDialogVisible = false"
              class="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50"
            >
              取消
            </button>
            <button
              @click="handleReject"
              class="rounded-lg bg-red-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-red-700"
            >
              驳回
            </button>
            <button
              @click="handleApprove"
              class="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-emerald-700"
            >
              通过
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 详情对话框 -->
    <Teleport to="body">
      <div
        v-if="detailDialogVisible"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm"
        @click.self="detailDialogVisible = false"
      >
        <div class="animate-modal-in w-full max-w-2xl rounded-2xl bg-white shadow-2xl">
          <!-- 头部 -->
          <div class="flex items-center justify-between border-b border-gray-100 px-6 py-4">
            <div class="flex items-center gap-3">
              <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-100">
                <Eye class="h-5 w-5 text-emerald-600" />
              </div>
              <div>
                <h3 class="text-lg font-semibold text-gray-900">申诉详情</h3>
                <p v-if="currentAppealDetail" class="text-sm text-gray-500">{{ currentAppealDetail.appealCode }}</p>
              </div>
            </div>
            <button
              @click="detailDialogVisible = false"
              class="rounded-lg p-2 text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-600"
            >
              <X class="h-5 w-5" />
            </button>
          </div>

          <!-- 内容 -->
          <div class="max-h-[70vh] overflow-y-auto p-6">
            <!-- 加载状态 -->
            <div v-if="detailLoading" class="flex items-center justify-center py-12">
              <Loader2 class="h-8 w-8 animate-spin text-emerald-600" />
            </div>

            <template v-else-if="currentAppealDetail">
              <!-- 状态和类型 -->
              <div class="mb-6 flex items-center gap-3">
                <span
                  class="inline-flex rounded-full px-3 py-1 text-sm font-medium"
                  :class="getStatusClass(currentAppealDetail.status)"
                >
                  {{ getStatusText(currentAppealDetail.status) }}
                </span>
                <span
                  class="inline-flex rounded-full px-3 py-1 text-sm font-medium"
                  :class="getAppealTypeClass(currentAppealDetail.appealType)"
                >
                  {{ getAppealTypeText(currentAppealDetail.appealType) }}
                </span>
              </div>

              <!-- 基本信息 -->
              <div class="mb-6 grid grid-cols-2 gap-4">
                <div class="rounded-xl bg-gray-50 p-4">
                  <div class="mb-1 flex items-center gap-2 text-sm text-gray-500">
                    <User class="h-4 w-4" />
                    班级
                  </div>
                  <div class="font-medium text-gray-900">{{ currentAppealDetail.className || '-' }}</div>
                </div>
                <div class="rounded-xl bg-gray-50 p-4">
                  <div class="mb-1 flex items-center gap-2 text-sm text-gray-500">
                    <User class="h-4 w-4" />
                    申请人
                  </div>
                  <div class="font-medium text-gray-900">{{ currentAppealDetail.applicantName || '-' }}</div>
                </div>
                <div class="rounded-xl bg-gray-50 p-4">
                  <div class="mb-1 flex items-center gap-2 text-sm text-gray-500">
                    <Calendar class="h-4 w-4" />
                    申请时间
                  </div>
                  <div class="font-medium text-gray-900">{{ currentAppealDetail.createdAt || '-' }}</div>
                </div>
                <div class="rounded-xl bg-gray-50 p-4">
                  <div class="mb-1 flex items-center gap-2 text-sm text-gray-500">
                    <Phone class="h-4 w-4" />
                    联系电话
                  </div>
                  <div class="font-medium text-gray-900">{{ currentAppealDetail.contactPhone || '-' }}</div>
                </div>
              </div>

              <!-- 分数信息 -->
              <div class="mb-6">
                <h4 class="mb-3 flex items-center gap-2 text-sm font-semibold text-gray-700">
                  <FileText class="h-4 w-4" />
                  分数信息
                </h4>
                <div class="grid grid-cols-3 gap-4">
                  <div class="rounded-xl border border-red-200 bg-red-50 p-4 text-center">
                    <div class="mb-1 text-sm text-red-600">原始分数</div>
                    <div class="text-2xl font-bold text-red-700">{{ currentAppealDetail.originalScore }}</div>
                  </div>
                  <div class="rounded-xl border border-blue-200 bg-blue-50 p-4 text-center">
                    <div class="mb-1 text-sm text-blue-600">期望分数</div>
                    <div class="text-2xl font-bold text-blue-700">{{ currentAppealDetail.expectedScore ?? '-' }}</div>
                  </div>
                  <div class="rounded-xl border border-green-200 bg-green-50 p-4 text-center">
                    <div class="mb-1 text-sm text-green-600">调整后分数</div>
                    <div class="text-2xl font-bold text-green-700">{{ currentAppealDetail.adjustedScore ?? '-' }}</div>
                  </div>
                </div>
              </div>

              <!-- 申诉理由 -->
              <div class="mb-6">
                <h4 class="mb-3 flex items-center gap-2 text-sm font-semibold text-gray-700">
                  <MessageSquare class="h-4 w-4" />
                  申诉理由
                </h4>
                <div class="rounded-xl bg-gray-50 p-4">
                  <p class="whitespace-pre-wrap text-gray-700">{{ currentAppealDetail.reason || '无' }}</p>
                </div>
              </div>

              <!-- 证据材料 -->
              <div v-if="currentAppealDetail.evidence" class="mb-6">
                <h4 class="mb-3 flex items-center gap-2 text-sm font-semibold text-gray-700">
                  <ImageIcon class="h-4 w-4" />
                  证据材料
                </h4>
                <div class="rounded-xl bg-gray-50 p-4">
                  <p class="whitespace-pre-wrap text-gray-700">{{ currentAppealDetail.evidence }}</p>
                </div>
              </div>

              <!-- 审核信息 -->
              <div v-if="currentAppealDetail.status > 1 && currentAppealDetail.rejectionReason" class="mb-6">
                <h4 class="mb-3 flex items-center gap-2 text-sm font-semibold text-gray-700">
                  <MessageSquare class="h-4 w-4" />
                  审核意见
                </h4>
                <div class="rounded-xl bg-amber-50 p-4">
                  <p class="whitespace-pre-wrap text-gray-700">{{ currentAppealDetail.rejectionReason }}</p>
                </div>
              </div>

              <!-- 公示信息 -->
              <div v-if="currentAppealDetail.publicityEndTime" class="mb-6">
                <h4 class="mb-3 flex items-center gap-2 text-sm font-semibold text-gray-700">
                  <Calendar class="h-4 w-4" />
                  公示截止时间
                </h4>
                <div class="rounded-xl bg-blue-50 p-4">
                  <p class="text-blue-700">{{ currentAppealDetail.publicityEndTime }}</p>
                </div>
              </div>
            </template>
          </div>

          <!-- 底部 -->
          <div class="flex justify-end border-t border-gray-100 px-6 py-4">
            <button
              @click="detailDialogVisible = false"
              class="rounded-lg bg-gray-100 px-4 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-200"
            >
              关闭
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { listAppeals, reviewAppeal } from '@/api/v2/appeal'
import { getAllGrades } from '@/api/v2/organization'
import { getAllClasses } from '@/api/v2/organization'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import {
  Search,
  Loader2,
  FileSearch,
  ChevronLeft,
  ChevronRight,
  X,
  Eye,
  User,
  Phone,
  Calendar,
  FileText,
  MessageSquare,
  Image as ImageIcon
} from 'lucide-vue-next'
import { getAppeal } from '@/api/v2/appeal'

const props = defineProps<{
  queryType: 'my' | 'all'
}>()

const authStore = useAuthStore()
const loading = ref(false)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  gradeId: undefined as number | undefined,
  classId: undefined as number | undefined,
  status: undefined as number | undefined
})

const appealList = ref<any[]>([])
const total = ref(0)
const gradeList = ref<any[]>([])
const classList = ref<any[]>([])
const canReview = ref(authStore.hasPermission('quantification:appeal:review'))

// 审核相关
const reviewDialogVisible = ref(false)
const currentReviewAppeal = ref<any>(null)
const reviewForm = reactive({
  opinion: '',
  adjustedScore: null as number | null
})

// 详情弹窗相关
const detailDialogVisible = ref(false)
const detailLoading = ref(false)
const currentAppealDetail = ref<any>(null)

// 加载申诉列表
const loadAppeals = async () => {
  loading.value = true
  try {
    const params: any = { ...query }
    if (props.queryType === 'my' && authStore.user) {
      params.appellantId = authStore.user.id
    }
    const res = await listAppeals(params)
    appealList.value = res.records || []
    total.value = Number(res.total) || 0
  } catch (error) {
    console.error('加载申诉列表失败:', error)
    ElMessage.error('加载申诉列表失败')
  } finally {
    loading.value = false
  }
}

// 加载年级列表
const loadGrades = async () => {
  try {
    const res = await getAllGrades()
    // http.ts已解包响应数据,res就是实际数据
    if (res) {
      gradeList.value = (Array.isArray(res) ? res : [])
        .filter((g: any) => g && (g.id !== undefined || g.gradeLevel !== undefined))
        .map((g: any) => ({
          ...g,
          id: g.id || g.gradeLevel,
          gradeName: g.gradeName || `${g.gradeLevel}年级`
        }))
    } else {
      gradeList.value = []
    }
  } catch (error) {
    console.error('加载年级列表失败:', error)
    gradeList.value = []
  }
}

// 加载班级列表
const loadClasses = async () => {
  try {
    const res = await getAllClasses()
    classList.value = res || []
  } catch (error) {
    console.error('加载班级列表失败:', error)
  }
}

// 重置查询
const handleReset = () => {
  query.gradeId = undefined
  query.classId = undefined
  query.status = undefined
  query.pageNum = 1
  loadAppeals()
}

// 查看详情
const handleView = async (row: any) => {
  detailDialogVisible.value = true
  detailLoading.value = true
  currentAppealDetail.value = null

  try {
    const res = await getAppeal(row.id)
    currentAppealDetail.value = res || row
  } catch (error) {
    console.error('获取申诉详情失败:', error)
    // 如果API失败，使用列表数据
    currentAppealDetail.value = row
  } finally {
    detailLoading.value = false
  }
}

// 获取申诉类型文本
const getAppealTypeText = (type: number) => {
  const map: Record<number, string> = {
    1: '分数异议',
    2: '事实不符',
    3: '程序不当',
    4: '其他'
  }
  return map[type] || '未知'
}

// 获取申诉类型样式
const getAppealTypeClass = (type: number) => {
  const map: Record<number, string> = {
    1: 'bg-blue-100 text-blue-700',
    2: 'bg-orange-100 text-orange-700',
    3: 'bg-purple-100 text-purple-700',
    4: 'bg-gray-100 text-gray-700'
  }
  return map[type] || 'bg-gray-100 text-gray-700'
}

// 打开审核对话框
const handleReview = (row: any) => {
  currentReviewAppeal.value = row
  reviewForm.opinion = ''
  reviewForm.adjustedScore = null
  reviewDialogVisible.value = true
}

// 审核通过
const handleApprove = async () => {
  if (!reviewForm.opinion || reviewForm.opinion.trim() === '') {
    ElMessage.warning('请输入审核意见')
    return
  }

  try {
    const requestData: any = {
      appealId: currentReviewAppeal.value.id,
      approvalStatus: 2,
      approvalOpinion: reviewForm.opinion
    }
    // 如果填写了调整后分数，则传递给后端
    if (reviewForm.adjustedScore !== null && reviewForm.adjustedScore !== undefined) {
      requestData.adjustedScore = reviewForm.adjustedScore
    }
    await reviewAppeal(requestData)
    ElMessage.success('审核通过')
    reviewDialogVisible.value = false
    loadAppeals()
  } catch (error) {
    ElMessage.error('审核失败')
  }
}

// 审核驳回
const handleReject = async () => {
  if (!reviewForm.opinion || reviewForm.opinion.trim() === '') {
    ElMessage.warning('请输入驳回理由')
    return
  }

  try {
    await reviewAppeal({
      appealId: currentReviewAppeal.value.id,
      approvalStatus: 3,
      approvalOpinion: reviewForm.opinion
    })
    ElMessage.success('已驳回')
    reviewDialogVisible.value = false
    loadAppeals()
  } catch (error) {
    ElMessage.error('驳回失败')
  }
}

// 获取状态样式
const getStatusClass = (status: number) => {
  const map: Record<number, string> = {
    1: 'bg-amber-100 text-amber-700',
    2: 'bg-green-100 text-green-700',
    3: 'bg-red-100 text-red-700',
    4: 'bg-gray-100 text-gray-700',
    5: 'bg-gray-100 text-gray-700',
    6: 'bg-blue-100 text-blue-700',
    7: 'bg-green-100 text-green-700'
  }
  return map[status] || 'bg-gray-100 text-gray-700'
}

// 获取状态文本
const getStatusText = (status: number) => {
  const map: Record<number, string> = {
    1: '待审核',
    2: '通过',
    3: '驳回',
    4: '撤销',
    5: '过期',
    6: '公示中',
    7: '已生效'
  }
  return map[status] || '未知'
}

onMounted(() => {
  loadGrades()
  loadClasses()
  loadAppeals()
})
</script>

<style scoped>
@keyframes modal-in {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(-10px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.animate-modal-in {
  animation: modal-in 0.2s ease-out;
}
</style>
