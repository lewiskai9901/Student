<script setup lang="ts">
/**
 * BatchTaskOperationDialog - Batch task operations dialog
 *
 * Allows performing bulk operations on multiple inspection tasks:
 * PUBLISH, CANCEL, or REASSIGN.
 */
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Play, Ban, UserCheck } from 'lucide-vue-next'
import { TaskStatusConfig, type TaskStatus } from '@/types/insp/enums'

const props = defineProps<{
  visible: boolean
  tasks: any[]
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  execute: [action: string, taskIds: number[]]
}>()

// ---------- State ----------

const selectedAction = ref<string>('PUBLISH')
const selectedTaskIds = ref<number[]>([])
const isExecuting = ref(false)

// ---------- Operations ----------

const operations = [
  {
    value: 'PUBLISH',
    label: '批量发布',
    description: '将已审核的任务批量发布为正式结果',
    icon: Play,
    applicableStatuses: ['REVIEWED'],
    type: 'success' as const,
  },
  {
    value: 'CANCEL',
    label: '批量取消',
    description: '取消选中的待处理或进行中的任务',
    icon: Ban,
    applicableStatuses: ['PENDING', 'CLAIMED', 'IN_PROGRESS'],
    type: 'danger' as const,
  },
  {
    value: 'REASSIGN',
    label: '批量重新分配',
    description: '将任务重新分配给其他检查员',
    icon: UserCheck,
    applicableStatuses: ['PENDING', 'CLAIMED'],
    type: 'warning' as const,
  },
]

// ---------- Computed ----------

const dialogVisible = computed({
  get: () => props.visible,
  set: (val: boolean) => emit('update:visible', val),
})

const currentOperation = computed(
  () => operations.find(op => op.value === selectedAction.value) ?? operations[0],
)

const eligibleTasks = computed(() => {
  const applicable = currentOperation.value.applicableStatuses
  return props.tasks.filter(t => applicable.includes(t.status))
})

const ineligibleTasks = computed(() => {
  const applicable = currentOperation.value.applicableStatuses
  return props.tasks.filter(t => !applicable.includes(t.status))
})

const allSelected = computed({
  get: () =>
    eligibleTasks.value.length > 0 &&
    eligibleTasks.value.every(t => selectedTaskIds.value.includes(t.id)),
  set: (val: boolean) => {
    if (val) {
      selectedTaskIds.value = eligibleTasks.value.map(t => t.id)
    } else {
      selectedTaskIds.value = []
    }
  },
})

const canExecute = computed(() => selectedTaskIds.value.length > 0 && !isExecuting.value)

// ---------- Watch ----------

// Reset selection when operation changes
watch(selectedAction, () => {
  selectedTaskIds.value = []
})

// Reset when dialog opens
watch(() => props.visible, (visible) => {
  if (visible) {
    selectedAction.value = 'PUBLISH'
    selectedTaskIds.value = []
    isExecuting.value = false
  }
})

// ---------- Actions ----------

function toggleTask(taskId: number) {
  const idx = selectedTaskIds.value.indexOf(taskId)
  if (idx >= 0) {
    selectedTaskIds.value.splice(idx, 1)
  } else {
    selectedTaskIds.value.push(taskId)
  }
}

function handleExecute() {
  if (!canExecute.value) return
  if (selectedTaskIds.value.length === 0) {
    ElMessage.warning('请至少选择一个任务')
    return
  }
  isExecuting.value = true
  emit('execute', selectedAction.value, [...selectedTaskIds.value])
  // The parent is responsible for closing the dialog after the operation completes
}
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    title="批量操作"
    width="640px"
    :close-on-click-modal="false"
  >
    <div class="space-y-4">
      <!-- Operation Selection -->
      <div>
        <label class="text-sm font-medium text-gray-700 mb-2 block">操作类型</label>
        <div class="flex gap-2">
          <div
            v-for="op in operations"
            :key="op.value"
            class="flex-1 rounded-md border px-3 py-2.5 cursor-pointer transition"
            :class="selectedAction === op.value
              ? 'border-blue-400 bg-blue-50'
              : 'border-gray-200 hover:border-gray-300'"
            @click="selectedAction = op.value"
          >
            <div class="flex items-center gap-1.5">
              <component :is="op.icon" class="w-4 h-4 text-gray-500" />
              <span class="text-sm font-medium text-gray-700">{{ op.label }}</span>
            </div>
            <p class="text-xs text-gray-400 mt-0.5">{{ op.description }}</p>
          </div>
        </div>
      </div>

      <!-- Task Selection -->
      <div>
        <div class="flex items-center justify-between mb-2">
          <label class="text-sm font-medium text-gray-700">
            选择任务
            <span class="font-normal text-gray-400">
              ({{ selectedTaskIds.length }}/{{ eligibleTasks.length }})
            </span>
          </label>
          <el-checkbox
            v-if="eligibleTasks.length > 0"
            v-model="allSelected"
            size="small"
          >
            全选
          </el-checkbox>
        </div>

        <div
          class="border border-gray-200 rounded-md overflow-auto"
          style="max-height: 280px"
        >
          <!-- Eligible Tasks -->
          <div
            v-for="task in eligibleTasks"
            :key="task.id"
            class="flex items-center gap-3 px-3 py-2 border-b border-gray-50 hover:bg-gray-50 cursor-pointer"
            @click="toggleTask(task.id)"
          >
            <el-checkbox
              :model-value="selectedTaskIds.includes(task.id)"
              @click.stop
              @update:model-value="toggleTask(task.id)"
            />
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2">
                <span class="text-sm text-gray-700">{{ task.taskCode || `Task #${task.id}` }}</span>
                <el-tag
                  size="small"
                  :type="(TaskStatusConfig[task.status as TaskStatus]?.type as any) || 'info'"
                  class="!h-4 !leading-4 !text-[10px]"
                >
                  {{ TaskStatusConfig[task.status as TaskStatus]?.label || task.status }}
                </el-tag>
              </div>
              <span class="text-xs text-gray-400">
                {{ task.inspectorName || '-' }} | {{ task.taskDate || '' }}
              </span>
            </div>
          </div>

          <!-- Ineligible Tasks -->
          <div
            v-for="task in ineligibleTasks"
            :key="'skip-' + task.id"
            class="flex items-center gap-3 px-3 py-2 border-b border-gray-50 opacity-40"
          >
            <el-checkbox disabled :model-value="false" />
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2">
                <span class="text-sm text-gray-500">{{ task.taskCode || `Task #${task.id}` }}</span>
                <el-tag
                  size="small"
                  type="info"
                  class="!h-4 !leading-4 !text-[10px]"
                >
                  {{ TaskStatusConfig[task.status as TaskStatus]?.label || task.status }}
                </el-tag>
              </div>
              <span class="text-xs text-gray-400">状态不适用于此操作</span>
            </div>
          </div>

          <!-- Empty -->
          <div
            v-if="eligibleTasks.length === 0 && ineligibleTasks.length === 0"
            class="py-8 text-center text-sm text-gray-400"
          >
            暂无任务
          </div>
          <div
            v-else-if="eligibleTasks.length === 0"
            class="py-4 text-center text-sm text-amber-500"
          >
            没有符合当前操作条件的任务
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="flex items-center justify-between">
        <span class="text-xs text-gray-400">
          已选中 {{ selectedTaskIds.length }} 个任务
        </span>
        <div class="flex items-center gap-2">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button
            :type="currentOperation.type"
            :loading="isExecuting"
            :disabled="!canExecute"
            @click="handleExecute"
          >
            {{ currentOperation.label }}
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>
