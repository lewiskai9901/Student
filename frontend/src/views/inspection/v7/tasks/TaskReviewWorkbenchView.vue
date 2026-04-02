<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, X, Eye, ChevronRight } from 'lucide-vue-next'
import { getTasks, reviewTask, rejectTask, publishTask } from '@/api/insp/task'
import { getSubmissions } from '@/api/insp/submission'
import { getDetails } from '@/api/insp/submission'
import type { InspTask, InspSubmission, SubmissionDetail } from '@/types/insp/project'
import { useAuthStore } from '@/stores/auth'
import { TaskStatusConfig, type TaskStatus } from '@/types/insp/enums'

const authStore = useAuthStore()
const loading = ref(false)
const submittedTasks = ref<InspTask[]>([])
const selectedTaskId = ref<number | null>(null)
const reviewComment = ref('')
const reviewing = ref(false)

// Detail data for selected task
const taskSubmissions = ref<InspSubmission[]>([])
const submissionDetails = ref<Map<number, SubmissionDetail[]>>(new Map())
const loadingDetails = ref(false)

const selectedTask = computed(() =>
  submittedTasks.value.find(t => t.id === selectedTaskId.value) ?? null
)

async function loadSubmittedTasks() {
  loading.value = true
  try {
    const allTasks = await getTasks()
    submittedTasks.value = allTasks.filter(t => t.status === 'SUBMITTED' || t.status === 'UNDER_REVIEW')
  } catch (e: any) {
    ElMessage.error(e.message || '加载待审核任务失败')
  } finally {
    loading.value = false
  }
}

async function selectTask(task: InspTask) {
  selectedTaskId.value = task.id
  reviewComment.value = ''
  // Load submissions and details for this task
  loadingDetails.value = true
  try {
    const subs = await getSubmissions(task.projectId)
    taskSubmissions.value = subs.filter(s => s.taskId === task.id)
    // Load details for each submission
    const detailMap = new Map<number, SubmissionDetail[]>()
    for (const sub of taskSubmissions.value) {
      try {
        const details = await getDetails(sub.id)
        detailMap.set(sub.id, details)
      } catch { /* ignore */ }
    }
    submissionDetails.value = detailMap
  } catch (e: any) {
    console.warn('Load details failed', e)
  } finally {
    loadingDetails.value = false
  }
}

async function handleApprove() {
  if (!selectedTaskId.value) return
  reviewing.value = true
  try {
    const userName = authStore.userName || 'admin'
    // Start review + complete review + publish in sequence
    await reviewTask(selectedTaskId.value, {
      reviewerName: userName,
      comment: reviewComment.value || '审核通过',
    })
    // After startReview (SUBMITTED→UNDER_REVIEW) + review (→REVIEWED), then publish
    await publishTask(selectedTaskId.value)
    ElMessage.success('已通过并发布')
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
    await rejectTask(selectedTaskId.value, reviewComment.value)
    ElMessage.success('已驳回，任务退回给检查员修改')
    submittedTasks.value = submittedTasks.value.filter(t => t.id !== selectedTaskId.value)
    selectedTaskId.value = null
    reviewComment.value = ''
  } catch (e: any) {
    ElMessage.error(e.message || '驳回失败')
  } finally {
    reviewing.value = false
  }
}

function getStatusLabel(status: string): string {
  return TaskStatusConfig[status as TaskStatus]?.label || status
}

function getScoreColor(score: number): string {
  if (score >= 85) return '#10b981'
  if (score >= 60) return '#f59e0b'
  return '#ef4444'
}

onMounted(() => loadSubmittedTasks())
</script>

<template>
  <div class="review-page">
    <header class="review-header">
      <span class="review-title">审核工作台</span>
      <span class="review-count" v-if="submittedTasks.length > 0">{{ submittedTasks.length }} 个待审核</span>
    </header>

    <div class="review-body">
      <!-- Left: Task List -->
      <div class="review-sidebar">
        <div v-loading="loading" class="review-task-list">
          <div v-if="submittedTasks.length === 0 && !loading" class="review-empty">
            <Check class="w-8 h-8 text-green-300 mx-auto mb-2" />
            <div>暂无待审核任务</div>
          </div>
          <div
            v-for="task in submittedTasks" :key="task.id"
            class="review-task-item"
            :class="{ selected: selectedTaskId === task.id }"
            @click="selectTask(task)"
          >
            <div class="rti-top">
              <span class="rti-code">{{ task.taskCode }}</span>
              <span class="rti-status">{{ getStatusLabel(task.status) }}</span>
            </div>
            <div class="rti-meta">
              <span>{{ task.inspectorName || '未知' }}</span>
              <span>{{ task.taskDate }}</span>
              <span>{{ task.completedTargets }}/{{ task.totalTargets }}目标</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Right: Review Panel -->
      <div class="review-main">
        <div v-if="!selectedTask" class="review-placeholder">
          <Eye class="w-10 h-10 text-gray-300 mx-auto mb-3" />
          <div class="text-gray-400">选择左侧任务查看打分详情</div>
        </div>

        <div v-else class="review-detail" v-loading="loadingDetails">
          <!-- Task Info -->
          <div class="rd-header">
            <div>
              <span class="rd-code">{{ selectedTask.taskCode }}</span>
              <span class="rd-inspector">检查员: {{ selectedTask.inspectorName || '-' }}</span>
            </div>
            <span class="rd-date">{{ selectedTask.taskDate }}</span>
          </div>

          <!-- Submissions & Scores -->
          <div class="rd-submissions">
            <div v-for="sub in taskSubmissions" :key="sub.id" class="rd-sub-card">
              <div class="rd-sub-header">
                <span class="rd-sub-target">{{ sub.targetName || `目标#${sub.targetId}` }}</span>
                <span v-if="sub.finalScore != null" class="rd-sub-score" :style="{ color: getScoreColor(sub.finalScore) }">
                  {{ sub.finalScore }}分
                </span>
                <span v-if="sub.grade" class="rd-sub-grade">{{ sub.grade }}</span>
                <span class="rd-sub-status" :class="sub.passed === true ? 'pass' : sub.passed === false ? 'fail' : ''">
                  {{ sub.passed === true ? '达标' : sub.passed === false ? '未达标' : '' }}
                </span>
              </div>

              <!-- Detail Items -->
              <div v-if="submissionDetails.get(sub.id)?.length" class="rd-detail-list">
                <div v-for="d in submissionDetails.get(sub.id)" :key="d.id" class="rd-detail-row">
                  <span class="rd-detail-name">{{ d.itemName }}</span>
                  <span class="rd-detail-mode" v-if="d.scoringMode">{{ d.scoringMode }}</span>
                  <span class="rd-detail-response">{{ d.responseValue || '-' }}</span>
                  <span class="rd-detail-score" v-if="d.score != null" :style="{ color: getScoreColor(d.score) }">
                    {{ d.score }}分
                  </span>
                </div>
              </div>
            </div>

            <div v-if="taskSubmissions.length === 0" class="review-empty" style="padding: 24px">
              暂无打分数据
            </div>
          </div>

          <!-- Review Form -->
          <div class="rd-review-form">
            <label class="rd-label">审核意见</label>
            <el-input
              v-model="reviewComment"
              type="textarea" :rows="2"
              placeholder="输入审核意见（驳回时必填）"
            />
            <div class="rd-actions">
              <el-button type="success" :loading="reviewing" @click="handleApprove">
                <Check class="w-4 h-4 mr-1" />通过并发布
              </el-button>
              <el-button type="danger" plain :loading="reviewing" @click="handleReject">
                <X class="w-4 h-4 mr-1" />驳回
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.review-page { background: #f7f8fa; min-height: 100vh; }
.review-header {
  position: sticky; top: 0; z-index: 10;
  height: 48px; background: #fff; border-bottom: 1px solid #e5e7eb;
  display: flex; align-items: center; gap: 10px; padding: 0 24px;
}
.review-title { font-size: 16px; font-weight: 700; color: #1a1a2e; }
.review-count { font-size: 12px; color: #f59e0b; background: #fffbeb; padding: 2px 10px; border-radius: 10px; font-weight: 600; }

.review-body {
  display: flex; height: calc(100vh - 48px);
}

/* Sidebar */
.review-sidebar {
  width: 320px; flex-shrink: 0;
  background: #fff; border-right: 1px solid #e5e7eb;
  overflow-y: auto;
}
.review-task-list { padding: 8px; }
.review-task-item {
  padding: 10px 12px; border-radius: 8px;
  cursor: pointer; margin-bottom: 4px;
  transition: background 0.15s;
}
.review-task-item:hover { background: #f3f4f6; }
.review-task-item.selected { background: #e8f0fe; border-left: 3px solid #1a6dff; }

.rti-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 4px; }
.rti-code { font-size: 13px; font-weight: 600; color: #1a1a2e; }
.rti-status { font-size: 11px; color: #f59e0b; background: #fffbeb; padding: 1px 8px; border-radius: 8px; }
.rti-meta { display: flex; gap: 8px; font-size: 11px; color: #9ca3af; }

/* Main */
.review-main { flex: 1; overflow-y: auto; padding: 16px 20px; }
.review-placeholder { text-align: center; padding: 80px 20px; }
.review-empty { text-align: center; padding: 40px; color: #9ca3af; font-size: 13px; }

/* Detail */
.rd-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 16px; background: #fff; border: 1px solid #e5e7eb; border-radius: 8px;
  margin-bottom: 12px;
}
.rd-code { font-size: 14px; font-weight: 700; color: #1a1a2e; margin-right: 12px; }
.rd-inspector { font-size: 12px; color: #6b7280; }
.rd-date { font-size: 12px; color: #9ca3af; }

.rd-submissions { margin-bottom: 16px; }
.rd-sub-card {
  background: #fff; border: 1px solid #e5e7eb; border-radius: 8px;
  margin-bottom: 8px; overflow: hidden;
}
.rd-sub-header {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 14px; background: #fafbfc; border-bottom: 1px solid #f3f4f6;
}
.rd-sub-target { font-size: 13px; font-weight: 600; color: #1a1a2e; flex: 1; }
.rd-sub-score { font-size: 14px; font-weight: 700; }
.rd-sub-grade { font-size: 11px; padding: 1px 8px; border-radius: 8px; background: #f3f4f6; color: #6b7280; }
.rd-sub-status { font-size: 11px; font-weight: 600; }
.rd-sub-status.pass { color: #10b981; }
.rd-sub-status.fail { color: #ef4444; }

.rd-detail-list { padding: 6px 14px; }
.rd-detail-row {
  display: flex; align-items: center; gap: 8px;
  padding: 5px 0; border-bottom: 1px solid #f9fafb;
  font-size: 12px;
}
.rd-detail-row:last-child { border-bottom: none; }
.rd-detail-name { flex: 1; color: #374151; }
.rd-detail-mode { font-size: 10px; color: #9ca3af; background: #f3f4f6; padding: 1px 6px; border-radius: 4px; }
.rd-detail-response { color: #6b7280; min-width: 40px; text-align: right; }
.rd-detail-score { font-weight: 700; min-width: 40px; text-align: right; }

.rd-review-form {
  background: #fff; border: 1px solid #e5e7eb; border-radius: 8px;
  padding: 14px 16px;
}
.rd-label { font-size: 13px; font-weight: 600; color: #374151; margin-bottom: 8px; display: block; }
.rd-actions { display: flex; gap: 10px; margin-top: 12px; }
</style>
