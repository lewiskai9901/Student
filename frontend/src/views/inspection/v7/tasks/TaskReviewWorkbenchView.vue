<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, X, Eye } from 'lucide-vue-next'
import { useInspExecutionStore } from '@/stores/insp/inspExecutionStore'

const store = useInspExecutionStore()

const loading = ref(false)

interface SubmittedTask {
  id: number
  taskCode: string
  targetName: string
  submittedBy: string
  submittedAt: string
  score: number | null
  status: string
}

const submittedTasks = ref<SubmittedTask[]>([])
const selectedTaskId = ref<number | null>(null)
const reviewComment = ref('')
const reviewing = ref(false)

const selectedTask = computed(() =>
  submittedTasks.value.find(t => t.id === selectedTaskId.value) ?? null
)

async function loadSubmittedTasks() {
  loading.value = true
  try {
    // Load all tasks, then filter for SUBMITTED status client-side
    const allTasks = await store.loadTasks()
    const submitted = (allTasks ?? []).filter((t: any) => t.status === 'SUBMITTED')
    submittedTasks.value = submitted.map((t: any) => ({
      id: t.id,
      taskCode: t.taskCode ?? `T-${t.id}`,
      targetName: t.targetName ?? '未知',
      submittedBy: t.inspectorName ?? '未知',
      submittedAt: t.updatedAt ?? t.createdAt ?? '',
      score: t.totalScore ?? null,
      status: t.status,
    }))
  } catch (e: any) {
    ElMessage.error(e.message || '加载待审核任务失败')
  } finally {
    loading.value = false
  }
}

function selectTask(task: SubmittedTask) {
  selectedTaskId.value = task.id
  reviewComment.value = ''
}

async function handleApprove() {
  if (!selectedTaskId.value) return
  reviewing.value = true
  try {
    await store.reviewTask(selectedTaskId.value, {
      reviewerName: 'current_user',
      comment: reviewComment.value || '审核通过',
    })
    ElMessage.success('已通过')
    submittedTasks.value = submittedTasks.value.filter(t => t.id !== selectedTaskId.value)
    selectedTaskId.value = null
    reviewComment.value = ''
  } catch (e: any) {
    ElMessage.error(e.message || '审批失败')
  } finally {
    reviewing.value = false
  }
}

async function handleReject() {
  if (!selectedTaskId.value) return
  if (!reviewComment.value.trim()) {
    ElMessage.warning('驳回时请填写审核意见')
    return
  }
  reviewing.value = true
  try {
    // Cancel task to reject it
    await store.cancelTask(selectedTaskId.value)
    ElMessage.success('已驳回')
    submittedTasks.value = submittedTasks.value.filter(t => t.id !== selectedTaskId.value)
    selectedTaskId.value = null
    reviewComment.value = ''
  } catch (e: any) {
    ElMessage.error(e.message || '驳回失败')
  } finally {
    reviewing.value = false
  }
}

onMounted(() => {
  loadSubmittedTasks()
})
</script>

<template>
  <div class="p-5 h-full">
    <h2 class="text-lg font-semibold mb-4">审核工作台</h2>

    <el-container class="border border-gray-200 rounded-lg overflow-hidden" style="height: calc(100vh - 180px)">
      <!-- Left: Task List -->
      <el-aside width="400px" class="border-r border-gray-200 bg-gray-50">
        <div class="p-3 border-b border-gray-200 bg-white">
          <span class="text-sm font-medium text-gray-600">待审核任务 ({{ submittedTasks.length }})</span>
        </div>
        <div v-loading="loading" class="overflow-auto" style="height: calc(100% - 45px)">
          <div v-if="submittedTasks.length === 0" class="py-16 text-center text-gray-400 text-sm">
            暂无待审核任务
          </div>
          <div
            v-for="task in submittedTasks"
            :key="task.id"
            class="p-3 border-b border-gray-100 cursor-pointer transition hover:bg-blue-50"
            :class="{ 'bg-blue-50 border-l-2 border-l-blue-500': selectedTaskId === task.id }"
            @click="selectTask(task)"
          >
            <div class="flex items-center justify-between mb-1">
              <span class="text-sm font-medium">{{ task.taskCode }}</span>
              <span v-if="task.score !== null" class="text-sm font-bold" :class="task.score >= 80 ? 'text-green-600' : 'text-orange-500'">
                {{ task.score }}分
              </span>
            </div>
            <div class="text-xs text-gray-500">{{ task.targetName }}</div>
            <div class="flex items-center justify-between mt-1">
              <span class="text-xs text-gray-400">提交人: {{ task.submittedBy }}</span>
              <span class="text-xs text-gray-400">{{ task.submittedAt?.slice(0, 16) }}</span>
            </div>
          </div>
        </div>
      </el-aside>

      <!-- Right: Review Panel -->
      <el-main class="p-0">
        <div v-if="!selectedTask" class="flex items-center justify-center h-full text-gray-400">
          <div class="text-center">
            <Eye class="w-12 h-12 mx-auto mb-3 text-gray-300" />
            <p>选择左侧任务查看详情</p>
          </div>
        </div>
        <div v-else class="p-5 space-y-4">
          <div class="flex items-center justify-between">
            <h3 class="text-base font-semibold">{{ selectedTask.taskCode }}</h3>
            <el-tag type="warning" size="small">待审核</el-tag>
          </div>

          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="检查目标">{{ selectedTask.targetName }}</el-descriptions-item>
            <el-descriptions-item label="提交人">{{ selectedTask.submittedBy }}</el-descriptions-item>
            <el-descriptions-item label="提交时间">{{ selectedTask.submittedAt }}</el-descriptions-item>
            <el-descriptions-item label="得分">
              <span class="font-bold" :class="(selectedTask.score ?? 0) >= 80 ? 'text-green-600' : 'text-orange-500'">
                {{ selectedTask.score ?? '-' }}
              </span>
            </el-descriptions-item>
          </el-descriptions>

          <div class="p-4 bg-gray-50 rounded text-sm text-gray-500">
            检查明细将在后续版本展示。当前可基于得分和基本信息进行审核。
          </div>

          <!-- Review Form -->
          <div>
            <label class="text-sm font-medium text-gray-700 mb-2 block">审核意见</label>
            <el-input
              v-model="reviewComment"
              type="textarea"
              :rows="3"
              placeholder="输入审核意见（驳回时必填）"
            />
          </div>

          <div class="flex gap-3">
            <el-button type="success" :loading="reviewing" @click="handleApprove">
              <Check class="w-4 h-4 mr-1" />通过
            </el-button>
            <el-button type="danger" :loading="reviewing" @click="handleReject">
              <X class="w-4 h-4 mr-1" />驳回
            </el-button>
          </div>
        </div>
      </el-main>
    </el-container>
  </div>
</template>
