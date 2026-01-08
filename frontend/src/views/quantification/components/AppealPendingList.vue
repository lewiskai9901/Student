<template>
  <div class="p-4">
    <!-- 提示信息 -->
    <div class="mb-4 flex items-start gap-3 rounded-xl bg-amber-50 p-4">
      <AlertCircle class="mt-0.5 h-5 w-5 flex-shrink-0 text-amber-600" />
      <div>
        <p class="font-medium text-amber-800">
          您有 <span class="text-lg font-bold text-amber-600">{{ pendingList.length }}</span> 个待审核的申诉
        </p>
        <p class="mt-1 text-sm text-amber-600">请及时处理，避免申诉超期</p>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="overflow-hidden rounded-xl border border-gray-200">
      <!-- 加载状态 -->
      <div v-if="loading" class="flex items-center justify-center py-16">
        <Loader2 class="h-8 w-8 animate-spin text-amber-600" />
      </div>

      <!-- 空状态 -->
      <div v-else-if="pendingList.length === 0" class="flex flex-col items-center justify-center py-16 text-gray-400">
        <CheckCircle class="h-12 w-12 text-green-400" />
        <p class="mt-3 text-sm text-green-600">暂无待审核的申诉</p>
      </div>

      <!-- 表格 -->
      <table v-else class="w-full">
        <thead>
          <tr class="bg-gray-50 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
            <th class="px-4 py-3">申诉编号</th>
            <th class="px-4 py-3">年级</th>
            <th class="px-4 py-3">班级</th>
            <th class="px-4 py-3">申请人</th>
            <th class="px-4 py-3">申诉原因</th>
            <th class="px-4 py-3 text-center">原始分数</th>
            <th class="px-4 py-3 text-center">期望分数</th>
            <th class="px-4 py-3">申请时间</th>
            <th class="px-4 py-3 text-center">操作</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100">
          <tr
            v-for="(row, index) in pendingList"
            :key="row.id"
            class="transition-colors hover:bg-gray-50"
            :style="{ animationDelay: `${index * 30}ms` }"
          >
            <td class="px-4 py-3 text-sm font-medium text-gray-900">{{ row.appealCode }}</td>
            <td class="px-4 py-3 text-sm text-gray-600">{{ row.gradeName }}</td>
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
              <span class="rounded-full bg-amber-100 px-2 py-1 text-xs font-semibold text-amber-700">
                {{ row.expectedScore }}
              </span>
            </td>
            <td class="px-4 py-3 text-sm text-gray-500">{{ row.createdAt }}</td>
            <td class="px-4 py-3 text-center">
              <button
                @click="handleReview(row)"
                class="rounded-lg bg-amber-500 px-3 py-1.5 text-xs font-medium text-white transition-colors hover:bg-amber-600"
              >
                审核
              </button>
            </td>
          </tr>
        </tbody>
      </table>
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
            <h3 class="flex items-center gap-2 text-lg font-semibold text-gray-900">
              <ClipboardCheck class="h-5 w-5 text-amber-600" />
              审核申诉
            </h3>
            <button
              @click="reviewDialogVisible = false"
              class="rounded-lg p-2 text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-600"
            >
              <X class="h-5 w-5" />
            </button>
          </div>

          <div class="max-h-[70vh] overflow-y-auto p-6">
            <!-- 申诉信息 -->
            <div class="mb-6 rounded-xl bg-gray-50 p-4">
              <h4 class="mb-3 font-medium text-gray-900">申诉信息</h4>
              <div class="grid grid-cols-2 gap-3 text-sm">
                <div>
                  <span class="text-gray-500">申诉编号：</span>
                  <span class="font-medium text-gray-900">{{ currentAppeal?.appealCode }}</span>
                </div>
                <div>
                  <span class="text-gray-500">班级：</span>
                  <span class="font-medium text-gray-900">{{ currentAppeal?.className }}</span>
                </div>
                <div>
                  <span class="text-gray-500">申请人：</span>
                  <span class="font-medium text-gray-900">{{ currentAppeal?.applicantName }}</span>
                </div>
                <div>
                  <span class="text-gray-500">原始分数：</span>
                  <span class="rounded-full bg-red-100 px-2 py-0.5 text-xs font-semibold text-red-700">
                    {{ currentAppeal?.originalScore }}
                  </span>
                </div>
                <div class="col-span-2">
                  <span class="text-gray-500">期望分数：</span>
                  <span class="rounded-full bg-amber-100 px-2 py-0.5 text-xs font-semibold text-amber-700">
                    {{ currentAppeal?.expectedScore }}
                  </span>
                </div>
                <div class="col-span-2">
                  <span class="text-gray-500">申诉原因：</span>
                  <span class="font-medium text-gray-900">{{ currentAppeal?.reason }}</span>
                </div>
              </div>
            </div>

            <!-- 审核表单 -->
            <div class="space-y-4">
              <div>
                <label class="mb-2 block text-sm font-medium text-gray-700">审核结果</label>
                <div class="flex gap-4">
                  <label
                    class="flex cursor-pointer items-center gap-2 rounded-lg border-2 px-4 py-2 transition-all"
                    :class="reviewForm.approvalStatus === 2 ? 'border-green-500 bg-green-50' : 'border-gray-200 hover:border-gray-300'"
                  >
                    <input type="radio" v-model="reviewForm.approvalStatus" :value="2" class="hidden" />
                    <CheckCircle class="h-5 w-5" :class="reviewForm.approvalStatus === 2 ? 'text-green-600' : 'text-gray-400'" />
                    <span :class="reviewForm.approvalStatus === 2 ? 'font-medium text-green-700' : 'text-gray-600'">通过</span>
                  </label>
                  <label
                    class="flex cursor-pointer items-center gap-2 rounded-lg border-2 px-4 py-2 transition-all"
                    :class="reviewForm.approvalStatus === 3 ? 'border-red-500 bg-red-50' : 'border-gray-200 hover:border-gray-300'"
                  >
                    <input type="radio" v-model="reviewForm.approvalStatus" :value="3" class="hidden" />
                    <XCircle class="h-5 w-5" :class="reviewForm.approvalStatus === 3 ? 'text-red-600' : 'text-gray-400'" />
                    <span :class="reviewForm.approvalStatus === 3 ? 'font-medium text-red-700' : 'text-gray-600'">驳回</span>
                  </label>
                </div>
              </div>

              <div v-if="reviewForm.approvalStatus === 2">
                <label class="mb-1.5 block text-sm font-medium text-gray-700">
                  调整后分数 <span class="text-red-500">*</span>
                </label>
                <div class="flex items-center gap-3">
                  <input
                    type="number"
                    v-model.number="reviewForm.adjustedScore"
                    step="0.5"
                    class="w-32 rounded-lg border border-gray-300 px-4 py-2 text-center text-gray-900 transition-colors focus:border-amber-500 focus:outline-none focus:ring-2 focus:ring-amber-500/20"
                  />
                  <span class="text-sm text-gray-500">原始分数: {{ currentAppeal?.originalScore }}</span>
                </div>
              </div>

              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">审核意见</label>
                <textarea
                  v-model="reviewForm.approvalOpinion"
                  rows="4"
                  maxlength="500"
                  placeholder="请输入审核意见(选填)"
                  class="w-full rounded-lg border border-gray-300 px-4 py-2.5 text-gray-900 transition-colors focus:border-amber-500 focus:outline-none focus:ring-2 focus:ring-amber-500/20"
                ></textarea>
                <div class="mt-1 text-right text-xs text-gray-400">
                  {{ reviewForm.approvalOpinion.length }}/500
                </div>
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
              @click="submitReview"
              :disabled="submitting"
              class="flex items-center gap-2 rounded-lg bg-amber-500 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-amber-600 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <Loader2 v-if="submitting" class="h-4 w-4 animate-spin" />
              提交审核
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getPendingAppeals, reviewAppeal } from '@/api/appeal'
import { ElMessage } from 'element-plus'
import {
  AlertCircle,
  Loader2,
  CheckCircle,
  ClipboardCheck,
  X,
  XCircle
} from 'lucide-vue-next'

const loading = ref(false)
const submitting = ref(false)
const pendingList = ref<any[]>([])
const reviewDialogVisible = ref(false)
const currentAppeal = ref<any>(null)

const reviewForm = reactive({
  appealId: null as number | null,
  approvalStatus: 2,
  approvalOpinion: '',
  adjustedScore: 0
})

// 加载待审核列表
const loadPending = async () => {
  loading.value = true
  try {
    const res = await getPendingAppeals()
    pendingList.value = res || []
  } catch (error) {
    console.error('加载待审核列表失败:', error)
    ElMessage.error('加载待审核列表失败')
  } finally {
    loading.value = false
  }
}

// 打开审核对话框
const handleReview = (row: any) => {
  currentAppeal.value = row
  Object.assign(reviewForm, {
    appealId: row.id,
    approvalStatus: 2,
    approvalOpinion: '',
    adjustedScore: row.expectedScore || row.originalScore
  })
  reviewDialogVisible.value = true
}

// 提交审核
const submitReview = async () => {
  if (reviewForm.approvalStatus === 2 && reviewForm.adjustedScore === undefined) {
    ElMessage.warning('请输入调整后分数')
    return
  }

  submitting.value = true
  try {
    await reviewAppeal(reviewForm)
    ElMessage.success('审核成功')
    reviewDialogVisible.value = false
    loadPending()
  } catch (error) {
    console.error('审核失败:', error)
    ElMessage.error('审核失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadPending()
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
