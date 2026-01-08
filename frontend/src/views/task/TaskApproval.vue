<template>
  <div class="p-6 bg-gray-50 min-h-full">
    <!-- 页面标题 -->
    <div class="mb-6">
      <h1 class="text-xl font-semibold text-gray-900">任务审批</h1>
      <p class="mt-1 text-sm text-gray-500">审核下属提交的任务</p>
    </div>

    <!-- 统计信息 -->
    <div class="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
      <StatCard
        title="待审批"
        :value="pendingCount"
        :icon="Clock"
        subtitle="待处理"
        color="orange"
      />
      <StatCard
        title="今日已审"
        :value="todayApprovedCount"
        :icon="CheckCircle2"
        subtitle="已通过"
        color="emerald"
      />
      <StatCard
        title="今日打回"
        :value="todayRejectedCount"
        :icon="XCircle"
        subtitle="已拒绝"
        color="purple"
      />
    </div>

    <!-- 待审批列表 -->
    <div class="rounded-lg border border-gray-200 bg-white">
      <div class="border-b border-gray-200 px-4 py-3">
        <h3 class="font-medium text-gray-900">待审批任务</h3>
      </div>

      <div class="divide-y divide-gray-200">
        <div
          v-for="task in taskList"
          :key="task.recordId"
          class="p-4 hover:bg-gray-50 cursor-pointer"
          @click="viewDetail(task)"
        >
          <div class="flex items-start justify-between">
            <div class="flex-1">
              <div class="flex items-center gap-2">
                <h4 class="font-medium text-gray-900">{{ task.title }}</h4>
                <span :class="getPriorityClass(task.priority)" class="rounded px-2 py-0.5 text-xs">
                  {{ task.priorityText }}
                </span>
                <span v-if="isOverdue(task.deadline)" class="text-xs text-red-500">[超期]</span>
              </div>
              <p class="mt-1 text-sm text-gray-500 line-clamp-1">{{ task.description || '暂无描述' }}</p>
              <div class="mt-2 flex items-center gap-4 text-xs text-gray-400">
                <span>编号: {{ task.taskCode }}</span>
                <span>提交人: {{ task.submitterName }}</span>
                <span>部门: {{ task.departmentName }}</span>
                <span>提交时间: {{ formatDate(task.submittedAt) }}</span>
              </div>
            </div>
            <div class="ml-4 flex items-center gap-2">
              <button
                class="rounded-md border border-red-300 bg-white px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50"
                @click.stop="quickReject(task)"
              >
                打回
              </button>
              <button
                class="rounded-md bg-green-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-green-700"
                @click.stop="quickApprove(task)"
              >
                通过
              </button>
            </div>
          </div>
        </div>

        <div v-if="taskList.length === 0" class="py-12 text-center">
          <BadgeCheck class="mx-auto h-12 w-12 text-green-300" />
          <p class="mt-2 text-sm text-gray-500">暂无待审批任务</p>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="total > 0" class="flex items-center justify-between border-t border-gray-200 px-4 py-3">
        <p class="text-sm text-gray-500">共 {{ total }} 条待审批任务</p>
        <div class="flex gap-2">
          <button
            :disabled="queryParams.pageNum === 1"
            class="rounded border px-3 py-1 text-sm disabled:opacity-50"
            @click="queryParams.pageNum--; loadTaskList()"
          >
            上一页
          </button>
          <button
            :disabled="queryParams.pageNum * queryParams.pageSize >= total"
            class="rounded border px-3 py-1 text-sm disabled:opacity-50"
            @click="queryParams.pageNum++; loadTaskList()"
          >
            下一页
          </button>
        </div>
      </div>
    </div>

    <!-- 任务详情对话框 -->
    <TaskDetailDialog
      v-model:visible="detailDialogVisible"
      :task-id="selectedTaskId"
      @refresh="loadTaskList"
    />

    <!-- 快速打回对话框 -->
    <div v-if="showQuickRejectDialog" class="fixed inset-0 z-50 flex items-center justify-center">
      <div class="absolute inset-0 bg-black/50" @click="showQuickRejectDialog = false"></div>
      <div class="relative w-full max-w-md rounded-lg bg-white p-6 shadow-xl">
        <h4 class="mb-4 text-lg font-semibold">打回任务</h4>
        <div class="space-y-4">
          <div>
            <label class="mb-1 block text-sm font-medium text-gray-700">打回原因 *</label>
            <textarea
              v-model="rejectReason"
              rows="3"
              placeholder="请输入打回原因..."
              class="w-full rounded-md border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none"
            ></textarea>
          </div>
          <div class="flex justify-end gap-3">
            <button
              class="rounded-md border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
              @click="showQuickRejectDialog = false"
            >
              取消
            </button>
            <button
              class="rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700"
              :disabled="!rejectReason || submitting"
              @click="confirmReject"
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
import { ref, reactive, onMounted } from 'vue'
import { Clock, CheckCircle2, XCircle, BadgeCheck } from 'lucide-vue-next'
import { getMyPendingApprovals, approveTaskByRecord, type TaskApprovalDTO } from '@/api/task/approval'
import StatCard from '@/components/design-system/cards/StatCard.vue'
import TaskDetailDialog from './components/TaskDetailDialog.vue'

// 查询参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10
})

// 数据
const taskList = ref<TaskApprovalDTO[]>([])
const total = ref(0)
const pendingCount = ref(0)
const todayApprovedCount = ref(0)
const todayRejectedCount = ref(0)

// 对话框
const detailDialogVisible = ref(false)
const selectedTaskId = ref<string>()
const showQuickRejectDialog = ref(false)
const selectedTask = ref<TaskApprovalDTO | null>(null)
const rejectReason = ref('')
const submitting = ref(false)

// 加载任务列表
const loadTaskList = async () => {
  try {
    const res = await getMyPendingApprovals()
    taskList.value = res
    total.value = res.length
    pendingCount.value = res.length
  } catch (error) {
    console.error('加载待审批任务失败', error)
  }
}

// 查看详情
const viewDetail = (task: TaskApprovalDTO) => {
  selectedTaskId.value = String(task.taskId)
  detailDialogVisible.value = true
}

// 快速通过
const quickApprove = async (task: TaskApprovalDTO) => {
  if (!task.recordId) {
    alert('审批记录不存在')
    return
  }

  try {
    await approveTaskByRecord(task.recordId, {
      taskId: task.taskId,
      submissionId: task.submissionId,
      action: 1,
      comment: ''
    })
    todayApprovedCount.value++
    loadTaskList()
  } catch (error) {
    console.error('审批失败', error)
    alert('审批失败')
  }
}

// 快速打回
const quickReject = (task: TaskApprovalDTO) => {
  if (!task.recordId) {
    alert('审批记录不存在')
    return
  }
  selectedTask.value = task
  rejectReason.value = ''
  showQuickRejectDialog.value = true
}

// 确认打回
const confirmReject = async () => {
  if (!selectedTask.value || !selectedTask.value.recordId) return

  submitting.value = true
  try {
    await approveTaskByRecord(selectedTask.value.recordId, {
      taskId: selectedTask.value.taskId,
      submissionId: selectedTask.value.submissionId,
      action: 2,
      comment: rejectReason.value,
      rejectToNode: 'submitter'
    })
    todayRejectedCount.value++
    showQuickRejectDialog.value = false
    loadTaskList()
  } catch (error) {
    console.error('打回失败', error)
    alert('打回失败')
  } finally {
    submitting.value = false
  }
}

// 格式化日期
const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  return dateStr.substring(0, 16).replace('T', ' ')
}

// 判断是否超期
const isOverdue = (deadline?: string) => {
  if (!deadline) return false
  return new Date(deadline) < new Date()
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

onMounted(() => {
  loadTaskList()
})
</script>
