<template>
  <div v-if="visible" class="fixed inset-0 z-50 flex items-center justify-center">
    <!-- 遮罩 -->
    <div class="absolute inset-0 bg-black/50" @click="close"></div>

    <!-- 对话框 -->
    <div class="relative w-full max-w-4xl rounded-lg bg-white shadow-xl">
      <!-- 标题 -->
      <div class="flex items-center justify-between border-b px-6 py-4">
        <div class="flex items-center gap-3">
          <h3 class="text-lg font-semibold">任务详情</h3>
          <span v-if="task" :class="getStatusClass(task.status)" class="rounded px-2 py-0.5 text-xs">
            {{ task.statusText }}
          </span>
        </div>
        <button class="text-gray-400 hover:text-gray-600" @click="close">
          <XMarkIcon class="h-5 w-5" />
        </button>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="flex h-64 items-center justify-center">
        <div class="text-gray-500">加载中...</div>
      </div>

      <!-- 内容 -->
      <div v-else-if="task" class="max-h-[75vh] overflow-y-auto">
        <!-- 基本信息 -->
        <div class="border-b px-6 py-4">
          <h4 class="mb-3 font-medium text-gray-900">基本信息</h4>
          <div class="grid grid-cols-2 gap-4 text-sm">
            <div>
              <span class="text-gray-500">任务编号：</span>
              <span class="text-blue-600">{{ task.taskCode }}</span>
            </div>
            <div>
              <span class="text-gray-500">优先级：</span>
              <span :class="getPriorityClass(task.priority)" class="rounded px-2 py-0.5 text-xs">
                {{ task.priorityText }}
              </span>
            </div>
            <div>
              <span class="text-gray-500">创建人：</span>
              <span>{{ task.assignerName }}</span>
            </div>
            <div v-if="!isMultiAssigneeTask">
              <span class="text-gray-500">执行人：</span>
              <span>{{ task.assigneeName || '-' }}</span>
            </div>
            <div v-else>
              <span class="text-gray-500">执行人：</span>
              <span class="text-blue-600">{{ task.assignees?.length || 0 }}人</span>
            </div>
            <div>
              <span class="text-gray-500">创建时间：</span>
              <span>{{ formatDate(task.createdAt) }}</span>
            </div>
            <div>
              <span class="text-gray-500">截止时间：</span>
              <span :class="{ 'text-red-500': task.overdue }">
                {{ formatDate(task.dueDate) }}
                <span v-if="task.overdue" class="ml-1">[超期]</span>
              </span>
            </div>
            <div v-if="!isMultiAssigneeTask && task.acceptedAt">
              <span class="text-gray-500">接收时间：</span>
              <span>{{ formatDate(task.acceptedAt) }}</span>
            </div>
            <div v-if="!isMultiAssigneeTask && task.completedAt">
              <span class="text-gray-500">完成时间：</span>
              <span>{{ formatDate(task.completedAt) }}</span>
            </div>
          </div>
        </div>

        <!-- 任务内容 -->
        <div class="border-b px-6 py-4">
          <h4 class="mb-2 font-medium text-gray-900">{{ task.title }}</h4>
          <p class="whitespace-pre-wrap text-sm text-gray-600">{{ task.description || '暂无描述' }}</p>
        </div>

        <!-- 多人任务：选座式执行人网格 -->
        <div v-if="isMultiAssigneeTask" class="border-b px-6 py-4">
          <h4 class="mb-4 font-medium text-gray-900">执行人状态</h4>
          <p class="mb-3 text-sm text-gray-500">点击姓名查看详细流程</p>
          <AssigneeSeatGrid :assignees="task.assignees || []" />
        </div>

        <!-- 单人任务：流程进度时间线 -->
        <div v-if="!isMultiAssigneeTask" class="border-b px-6 py-4">
          <h4 class="mb-4 font-medium text-gray-900">流程进度</h4>
          <TaskProgressTimeline v-if="taskProgress.length > 0" :progress-nodes="taskProgress" />
          <div v-else class="text-center py-8 text-gray-400">
            <p>暂无流程进度信息</p>
          </div>
        </div>

        <!-- 单人任务：提交记录 -->
        <div v-if="!isMultiAssigneeTask && task.submission" class="border-b px-6 py-4">
          <h4 class="mb-3 font-medium text-gray-900">提交记录</h4>
          <div class="rounded-lg border border-gray-200 bg-gray-50 p-4">
            <div class="mb-2 flex items-center justify-between">
              <span class="text-sm text-gray-500">
                {{ task.submission.submitterName }} 提交于 {{ formatDate(task.submission.submittedAt) }}
              </span>
              <span :class="getReviewStatusClass(task.submission.reviewStatus)" class="rounded px-2 py-0.5 text-xs">
                {{ task.submission.reviewStatusText }}
              </span>
            </div>
            <p class="whitespace-pre-wrap text-sm text-gray-700">{{ task.submission.content || '无提交内容' }}</p>

            <!-- 附件列表 -->
            <div v-if="task.submission.attachmentUrls?.length" class="mt-3">
              <p class="mb-2 text-sm font-medium text-gray-700">附件：</p>
              <AttachmentPreview
                :attachments="task.submission.attachmentUrls.map((url, idx) => ({ url, name: `附件${idx + 1}` }))"
              />
            </div>
          </div>
        </div>

        <!-- 单人任务：审批记录 -->
        <div v-if="!isMultiAssigneeTask && task.approvalRecords?.length" class="border-b px-6 py-4">
          <h4 class="mb-3 font-medium text-gray-900">审批记录</h4>
          <div class="space-y-3">
            <div
              v-for="record in task.approvalRecords"
              :key="record.id"
              class="flex items-start gap-3 rounded-lg border border-gray-200 p-3"
            >
              <div class="flex-shrink-0">
                <div class="flex h-8 w-8 items-center justify-center rounded-full bg-blue-100 text-sm font-medium text-blue-600">
                  {{ record.approverName?.charAt(0) }}
                </div>
              </div>
              <div class="flex-1">
                <div class="flex items-center gap-2">
                  <span class="font-medium text-gray-900">{{ record.approverName }}</span>
                  <span class="text-xs text-gray-500">{{ record.nodeName }}</span>
                  <span :class="getApprovalStatusClass(record.approvalStatus)" class="rounded px-2 py-0.5 text-xs">
                    {{ record.approvalStatusText }}
                  </span>
                </div>
                <p v-if="record.approvalComment" class="mt-1 text-sm text-gray-600">{{ record.approvalComment }}</p>
                <p v-if="record.rejectReason" class="mt-1 text-sm text-red-600">打回原因：{{ record.rejectReason }}</p>
                <p class="mt-1 text-xs text-gray-400">{{ formatDate(record.approvalTime) }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- 操作区域 -->
        <div class="px-6 py-4">
          <!-- 接收任务 -->
          <div v-if="canAccept" class="flex justify-center">
            <button
              class="rounded-md bg-blue-600 px-6 py-2 text-sm font-medium text-white hover:bg-blue-700"
              :disabled="submitting"
              @click="handleAccept"
            >
              {{ submitting ? '处理中...' : '接收任务' }}
            </button>
          </div>

          <!-- 提交任务 -->
          <div v-else-if="canSubmit">
            <h4 class="mb-3 font-medium text-gray-900">提交任务</h4>
            <div class="space-y-3">
              <textarea
                v-model="submitForm.content"
                rows="4"
                placeholder="请输入任务完成情况说明..."
                class="w-full rounded-md border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none"
              ></textarea>
              <!-- TODO: 文件上传 -->
              <div class="flex justify-end">
                <button
                  class="rounded-md bg-green-600 px-6 py-2 text-sm font-medium text-white hover:bg-green-700"
                  :disabled="submitting"
                  @click="handleSubmit"
                >
                  {{ submitting ? '提交中...' : '提交任务' }}
                </button>
              </div>
            </div>
          </div>

          <!-- 审批任务 -->
          <div v-else-if="canApprove">
            <h4 class="mb-3 font-medium text-gray-900">审批操作</h4>
            <div class="space-y-3">
              <textarea
                v-model="approveForm.comment"
                rows="3"
                placeholder="请输入审批意见（选填）..."
                class="w-full rounded-md border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none"
              ></textarea>
              <div class="flex justify-end gap-3">
                <button
                  class="rounded-md border border-red-300 bg-white px-4 py-2 text-sm font-medium text-red-600 hover:bg-red-50"
                  :disabled="submitting"
                  @click="showRejectDialog = true"
                >
                  打回
                </button>
                <button
                  class="rounded-md bg-green-600 px-6 py-2 text-sm font-medium text-white hover:bg-green-700"
                  :disabled="submitting"
                  @click="handleApprove(1)"
                >
                  {{ submitting ? '处理中...' : '通过' }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 打回确认对话框 -->
    <div v-if="showRejectDialog" class="fixed inset-0 z-60 flex items-center justify-center">
      <div class="absolute inset-0 bg-black/30" @click="showRejectDialog = false"></div>
      <div class="relative w-full max-w-md rounded-lg bg-white p-6 shadow-xl">
        <h4 class="mb-4 text-lg font-semibold">打回任务</h4>
        <div class="space-y-4">
          <div>
            <label class="mb-1 block text-sm font-medium text-gray-700">打回原因 *</label>
            <textarea
              v-model="approveForm.comment"
              rows="3"
              placeholder="请输入打回原因..."
              class="w-full rounded-md border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none"
            ></textarea>
          </div>
          <div v-if="rejectableNodes.length > 0">
            <label class="mb-1 block text-sm font-medium text-gray-700">回退节点</label>
            <select
              v-model="approveForm.rejectToNode"
              class="w-full rounded-md border border-gray-300 px-3 py-2"
            >
              <option value="">直接打回给执行人</option>
              <option v-for="node in rejectableNodes" :key="node.nodeId" :value="node.nodeId">
                {{ node.nodeName }}
              </option>
            </select>
          </div>
          <div class="flex justify-end gap-3">
            <button
              class="rounded-md border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
              @click="showRejectDialog = false"
            >
              取消
            </button>
            <button
              class="rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700"
              :disabled="!approveForm.comment || submitting"
              @click="handleApprove(2)"
            >
              {{ submitting ? '处理中...' : '确认打回' }}
            </button>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { XMarkIcon } from '@heroicons/vue/24/outline'
import { getTaskDetail, getTaskCardDetail, acceptTask, submitTask, approveTask, getTaskProgress, type TaskDTO, type TaskDetailDTO, type TaskProgressNodeDTO, type TaskAssigneeDTO } from '@/api/task'
import { getRejectableNodes, getProcessProgress, type RejectableNode, type ProcessProgress } from '@/api/task/workflow'
import { useAuthStore } from '@/stores/auth'
import TaskProgressTimeline from './TaskProgressTimeline.vue'
import AttachmentPreview from './AttachmentPreview.vue'
import AssigneeSeatGrid from './AssigneeSeatGrid.vue'

const props = defineProps<{
  visible: boolean
  taskId?: string | number
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  refresh: []
}>()

const authStore = useAuthStore()

// 数据
const loading = ref(false)
const submitting = ref(false)
const task = ref<TaskDTO | null>(null)
const taskCardDetail = ref<TaskDetailDTO | null>(null)
const showRejectDialog = ref(false)
const rejectableNodes = ref<RejectableNode[]>([])
const processProgress = ref<ProcessProgress | null>(null)
const taskProgress = ref<TaskProgressNodeDTO[]>([])

// 是否为多人任务
const isMultiAssigneeTask = computed(() => {
  return (task.value?.assignees?.length ?? 0) > 0
})

// 表单
const submitForm = ref({
  content: '',
  attachmentIds: [] as number[]
})

const approveForm = ref({
  comment: '',
  rejectToNode: ''
})

// 判断是否可以接收任务
const canAccept = computed(() => {
  if (!task.value) return false
  const userId = authStore.user?.userId
  return task.value.status === 0 && task.value.assigneeId === userId
})

// 判断是否可以提交任务
const canSubmit = computed(() => {
  if (!task.value) return false
  const userId = authStore.user?.userId
  // 状态为进行中或已打回，且是执行人
  return (task.value.status === 1 || task.value.status === 4) && task.value.assigneeId === userId
})

// 判断是否可以审批
const canApprove = computed(() => {
  if (!task.value) return false
  const userId = authStore.user?.userId
  // 状态为待审核，且当前用户是审批人
  return task.value.status === 2 && task.value.currentApprovers?.includes(userId as number)
})

// 加载任务详情
const loadTask = async () => {
  if (!props.taskId) return

  loading.value = true
  processProgress.value = null
  taskProgress.value = []
  taskCardDetail.value = null

  try {
    // 并行加载基础详情和卡片详情
    const [basicDetail, cardDetail] = await Promise.all([
      getTaskDetail(props.taskId),
      getTaskCardDetail(props.taskId).catch(() => null)
    ])

    task.value = basicDetail
    taskCardDetail.value = cardDetail

    // 仅单人任务加载流程进度
    if (!isMultiAssigneeTask.value) {
      try {
        taskProgress.value = await getTaskProgress(props.taskId)
      } catch (e) {
        console.error('加载任务进度失败', e)
      }
    }

    // 如果有流程实例，加载流程进度（保留旧逻辑以防需要）
    if (task.value?.processInstanceId) {
      try {
        processProgress.value = await getProcessProgress(task.value.processInstanceId)
      } catch (e) {
        console.error('加载流程进度失败', e)
      }
    }

    // 如果可以审批，加载可回退节点
    if (canApprove.value && task.value?.submission?.id) {
      // TODO: 根据flowableTaskId加载可回退节点
    }
  } catch (error) {
    console.error('加载任务详情失败', error)
  } finally {
    loading.value = false
  }
}

// 接收任务
const handleAccept = async () => {
  if (!task.value) return

  submitting.value = true
  try {
    await acceptTask(task.value.id)
    emit('refresh')
    await loadTask()
  } catch (error) {
    console.error('接收任务失败', error)
    alert('接收任务失败')
  } finally {
    submitting.value = false
  }
}

// 提交任务
const handleSubmit = async () => {
  if (!task.value) return

  submitting.value = true
  try {
    await submitTask({
      taskId: task.value.id,
      content: submitForm.value.content,
      attachmentIds: submitForm.value.attachmentIds
    })
    emit('refresh')
    await loadTask()
    submitForm.value.content = ''
    submitForm.value.attachmentIds = []
  } catch (error) {
    console.error('提交任务失败', error)
    alert('提交任务失败')
  } finally {
    submitting.value = false
  }
}

// 审批任务
const handleApprove = async (action: number) => {
  if (!task.value || !task.value.submission) return

  submitting.value = true
  try {
    await approveTask({
      taskId: task.value.id,
      submissionId: task.value.submission.id,
      action,
      comment: approveForm.value.comment,
      rejectToNode: approveForm.value.rejectToNode || undefined
    })
    showRejectDialog.value = false
    emit('refresh')
    await loadTask()
    approveForm.value.comment = ''
    approveForm.value.rejectToNode = ''
  } catch (error) {
    console.error('审批失败', error)
    alert('审批失败')
  } finally {
    submitting.value = false
  }
}

// 关闭对话框
const close = () => {
  emit('update:visible', false)
}

// 格式化日期
const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  return dateStr.substring(0, 16).replace('T', ' ')
}

// 状态样式
const getStatusClass = (status: number) => {
  const classes: Record<number, string> = {
    0: 'bg-orange-100 text-orange-800',
    1: 'bg-blue-100 text-blue-800',
    2: 'bg-yellow-100 text-yellow-800',
    3: 'bg-green-100 text-green-800',
    4: 'bg-red-100 text-red-800',
    5: 'bg-gray-100 text-gray-800',
    6: 'bg-purple-100 text-purple-800'
  }
  return classes[status] || 'bg-gray-100 text-gray-800'
}

// 优先级样式
const getPriorityClass = (priority: number) => {
  const classes: Record<number, string> = {
    1: 'bg-red-100 text-red-800',
    2: 'bg-blue-100 text-blue-800',
    3: 'bg-gray-100 text-gray-800'
  }
  return classes[priority] || 'bg-gray-100 text-gray-800'
}

// 审核状态样式
const getReviewStatusClass = (status: number) => {
  const classes: Record<number, string> = {
    0: 'bg-yellow-100 text-yellow-800',
    1: 'bg-green-100 text-green-800',
    2: 'bg-red-100 text-red-800'
  }
  return classes[status] || 'bg-gray-100 text-gray-800'
}

// 审批状态样式
const getApprovalStatusClass = (status: number) => {
  const classes: Record<number, string> = {
    0: 'bg-yellow-100 text-yellow-800',
    1: 'bg-green-100 text-green-800',
    2: 'bg-red-100 text-red-800',
    3: 'bg-blue-100 text-blue-800'
  }
  return classes[status] || 'bg-gray-100 text-gray-800'
}

// 监听对话框打开
watch(() => props.visible, (val) => {
  if (val && props.taskId) {
    loadTask()
  }
})
</script>
