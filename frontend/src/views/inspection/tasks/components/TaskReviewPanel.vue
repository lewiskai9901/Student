<script setup lang="ts">
/**
 * TaskReviewPanel - Review panel (approve/reject + notes)
 *
 * Provides a review interface for inspectors/reviewers to approve
 * or reject submitted inspection tasks.
 */
import { ref, computed } from 'vue'
import { Check, X, Eye, FileText, AlertTriangle } from 'lucide-vue-next'
import { ElMessageBox } from 'element-plus'
import { TaskStatusConfig, type TaskStatus } from '@/types/insp/enums'

const props = defineProps<{
  task: any
}>()

const emit = defineEmits<{
  approve: []
  reject: [reason: string]
}>()

// ---------- State ----------

const reviewNotes = ref('')
const isSubmitting = ref(false)

// ---------- Computed ----------

const taskStatus = computed(() => props.task?.status ?? 'SUBMITTED')
const isReviewable = computed(() => taskStatus.value === 'SUBMITTED' || taskStatus.value === 'UNDER_REVIEW')

const taskScore = computed(() => {
  const scores: { label: string; value: string | number }[] = []
  if (props.task?.totalTargets != null) {
    scores.push({ label: '总目标数', value: props.task.totalTargets })
  }
  if (props.task?.completedTargets != null) {
    scores.push({ label: '已完成', value: props.task.completedTargets })
  }
  if (props.task?.skippedTargets != null && props.task.skippedTargets > 0) {
    scores.push({ label: '已跳过', value: props.task.skippedTargets })
  }
  return scores
})

const canReject = computed(() => reviewNotes.value.trim().length > 0)

// ---------- Actions ----------

async function handleApprove() {
  try {
    await ElMessageBox.confirm(
      '确定通过审核？审核通过后结果将正式发布。',
      '确认审核通过',
      { confirmButtonText: '确认通过', cancelButtonText: '取消', type: 'success' },
    )
    isSubmitting.value = true
    emit('approve')
  } catch {
    // cancelled
  } finally {
    isSubmitting.value = false
  }
}

async function handleReject() {
  if (!canReject.value) return
  try {
    await ElMessageBox.confirm(
      '确定驳回该任务？检查员需要重新执行。',
      '确认驳回',
      { confirmButtonText: '确认驳回', cancelButtonText: '取消', type: 'warning' },
    )
    isSubmitting.value = true
    emit('reject', reviewNotes.value.trim())
  } catch {
    // cancelled
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <div class="task-review-panel rounded-lg border border-gray-200 bg-white overflow-hidden">
    <!-- Header -->
    <div class="px-4 py-3 bg-gray-50 border-b border-gray-200">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-2">
          <Eye class="w-4 h-4 text-gray-500" />
          <span class="text-sm font-medium text-gray-700">审核面板</span>
        </div>
        <el-tag
          v-if="task"
          :type="(TaskStatusConfig[taskStatus as TaskStatus]?.type as any) || 'info'"
          size="small"
        >
          {{ TaskStatusConfig[taskStatus as TaskStatus]?.label || taskStatus }}
        </el-tag>
      </div>
    </div>

    <div class="p-4 space-y-4">
      <!-- Task Info -->
      <div v-if="task" class="space-y-2">
        <div class="grid grid-cols-2 gap-x-4 gap-y-2">
          <div>
            <span class="text-xs text-gray-400">任务编码</span>
            <div class="text-sm text-gray-700">{{ task.taskCode || '-' }}</div>
          </div>
          <div>
            <span class="text-xs text-gray-400">检查员</span>
            <div class="text-sm text-gray-700">{{ task.inspectorName || '-' }}</div>
          </div>
          <div>
            <span class="text-xs text-gray-400">检查日期</span>
            <div class="text-sm text-gray-700">{{ task.taskDate || '-' }}</div>
          </div>
          <div>
            <span class="text-xs text-gray-400">时间段</span>
            <div class="text-sm text-gray-700">
              {{ task.timeSlotStart && task.timeSlotEnd
                ? `${task.timeSlotStart} - ${task.timeSlotEnd}`
                : '-' }}
            </div>
          </div>
        </div>

        <!-- Score Stats -->
        <div v-if="taskScore.length > 0" class="flex items-center gap-4 pt-2 border-t border-gray-100">
          <div v-for="item in taskScore" :key="item.label" class="text-center">
            <div class="text-lg font-bold text-gray-700">{{ item.value }}</div>
            <div class="text-xs text-gray-400">{{ item.label }}</div>
          </div>
        </div>
      </div>

      <!-- Previous Review Comment -->
      <div v-if="task?.reviewComment" class="rounded-md bg-amber-50 px-3 py-2">
        <div class="flex items-center gap-1 mb-1">
          <FileText class="w-3.5 h-3.5 text-amber-500" />
          <span class="text-xs font-medium text-amber-600">上次审核意见</span>
        </div>
        <p class="text-sm text-amber-700">{{ task.reviewComment }}</p>
      </div>

      <!-- Review Notes Input -->
      <div>
        <label class="text-sm font-medium text-gray-700 mb-1.5 block">审核意见</label>
        <el-input
          v-model="reviewNotes"
          type="textarea"
          :rows="3"
          :disabled="!isReviewable"
          placeholder="输入审核意见（驳回时必填）..."
        />
      </div>

      <!-- Reject without notes warning -->
      <div
        v-if="reviewNotes.trim().length === 0"
        class="flex items-center gap-1.5 text-xs text-gray-400"
      >
        <AlertTriangle class="w-3.5 h-3.5" />
        <span>驳回时必须填写审核意见</span>
      </div>

      <!-- Action Buttons -->
      <div v-if="isReviewable" class="flex items-center gap-3 pt-2">
        <el-button
          type="success"
          :loading="isSubmitting"
          @click="handleApprove"
        >
          <Check class="w-4 h-4 mr-1" />通过
        </el-button>
        <el-button
          type="danger"
          :loading="isSubmitting"
          :disabled="!canReject"
          @click="handleReject"
        >
          <X class="w-4 h-4 mr-1" />驳回
        </el-button>
      </div>

      <!-- Not reviewable message -->
      <div v-else class="rounded-md bg-gray-50 px-3 py-2 text-xs text-gray-400">
        当前任务状态不可审核
      </div>
    </div>
  </div>
</template>
