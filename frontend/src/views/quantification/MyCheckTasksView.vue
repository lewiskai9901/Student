<template>
  <div class="my-check-tasks">
    <!-- 页面标题 -->
    <header class="page-header">
      <h1 class="page-title">
        <ClipboardCheck :size="24" />
        我的检查任务
      </h1>
      <div class="header-actions">
        <span class="pending-badge" v-if="pendingCount > 0">
          {{ pendingCount }} 个待处理任务
        </span>
      </div>
    </header>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-select v-model="filterStatus" placeholder="任务状态" clearable style="width: 150px;">
        <el-option label="待处理" :value="0" />
        <el-option label="进行中" :value="1" />
        <el-option label="已完成" :value="2" />
      </el-select>
      <el-button @click="loadTasks">
        <RefreshCw :size="16" />
        刷新
      </el-button>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <Loader2 class="spinner" />
      <span>加载中...</span>
    </div>

    <!-- 空状态 -->
    <div v-else-if="tasks.length === 0" class="empty-state">
      <ClipboardCheck :size="48" class="empty-icon" />
      <p>暂无检查任务</p>
    </div>

    <!-- 任务列表 -->
    <div v-else class="task-list">
      <div
        v-for="task in tasks"
        :key="task.id"
        class="task-card"
        :class="{ 'task-pending': task.status === 0, 'task-progress': task.status === 1, 'task-completed': task.status === 2 }"
      >
        <div class="task-header">
          <div class="task-info">
            <span class="task-check-name">{{ task.checkName }}</span>
            <span class="task-plan-name">{{ task.planName }}</span>
          </div>
          <span class="task-status-badge" :class="getStatusClass(task.status)">
            {{ getStatusText(task.status) }}
          </span>
        </div>

        <div class="task-meta">
          <div class="meta-item">
            <Calendar :size="14" />
            <span>检查日期：{{ task.checkDate }}</span>
          </div>
          <div class="meta-item" v-if="task.startedAt">
            <Clock :size="14" />
            <span>开始时间：{{ formatDateTime(task.startedAt) }}</span>
          </div>
          <div class="meta-item" v-if="task.completedAt">
            <CheckCircle :size="14" />
            <span>完成时间：{{ formatDateTime(task.completedAt) }}</span>
          </div>
        </div>

        <div class="task-scope">
          <div class="scope-item">
            <FolderOpen :size="14" />
            <span>检查类别：{{ parseCount(task.categoryIds) }}个类别</span>
          </div>
          <div class="scope-item">
            <Users :size="14" />
            <span>检查班级：{{ parseCount(task.classIds) }}个班级</span>
          </div>
        </div>

        <div class="task-actions">
          <button
            v-if="task.status === 0"
            class="btn btn-primary"
            @click="handleStartTask(task)"
          >
            <Play :size="16" />
            开始任务
          </button>
          <button
            v-if="task.status === 1"
            class="btn btn-success"
            @click="handleCompleteTask(task)"
          >
            <CheckCircle :size="16" />
            完成任务
          </button>
          <button
            v-if="task.status === 1"
            class="btn btn-default"
            @click="goToScoring(task)"
          >
            <Edit :size="16" />
            去打分
          </button>
          <button
            v-if="task.status === 2"
            class="btn btn-default"
            @click="viewCheckDetail(task)"
          >
            <Eye :size="16" />
            查看详情
          </button>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrap" v-if="total > pageSize">
      <el-pagination
        v-model:current-page="pageNum"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadTasks"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ClipboardCheck,
  RefreshCw,
  Loader2,
  Calendar,
  Clock,
  CheckCircle,
  FolderOpen,
  Users,
  Play,
  Edit,
  Eye
} from 'lucide-vue-next'
import {
  getMyTasks,
  startTask,
  completeTask,
  getPendingTaskCount,
  type CheckTaskAssignment
} from '@/api/inspector'

const router = useRouter()

// 状态
const loading = ref(false)
const tasks = ref<CheckTaskAssignment[]>([])
const filterStatus = ref<number | null>(null)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const pendingCount = ref(0)

// 加载任务列表
const loadTasks = async () => {
  loading.value = true
  try {
    const res = await getMyTasks({
      status: filterStatus.value ?? undefined,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    tasks.value = res.records || []
    total.value = res.total || 0
  } catch (error) {
    console.error('加载任务列表失败', error)
  } finally {
    loading.value = false
  }
}

// 加载待处理任务数量
const loadPendingCount = async () => {
  try {
    const count = await getPendingTaskCount()
    pendingCount.value = count || 0
  } catch (error) {
    console.error('加载待处理任务数量失败', error)
  }
}

// 开始任务
const handleStartTask = async (task: CheckTaskAssignment) => {
  try {
    await ElMessageBox.confirm(
      '确定要开始执行此检查任务吗？',
      '开始任务',
      { type: 'info' }
    )
    await startTask(task.id)
    ElMessage.success('任务已开始')
    loadTasks()
    loadPendingCount()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '操作失败')
    }
  }
}

// 完成任务
const handleCompleteTask = async (task: CheckTaskAssignment) => {
  try {
    await ElMessageBox.confirm(
      '确定要标记此任务为完成吗？请确保已完成所有检查打分。',
      '完成任务',
      { type: 'warning' }
    )
    await completeTask(task.id)
    ElMessage.success('任务已完成')
    loadTasks()
    loadPendingCount()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '操作失败')
    }
  }
}

// 去打分
const goToScoring = (task: CheckTaskAssignment) => {
  router.push({
    name: 'DailyScoringView',
    params: { id: task.dailyCheckId }
  })
}

// 查看详情
const viewCheckDetail = (task: CheckTaskAssignment) => {
  router.push({
    name: 'DailyCheckDetail',
    params: { id: task.dailyCheckId }
  })
}

// 获取状态样式类
const getStatusClass = (status: number) => {
  switch (status) {
    case 0: return 'status-pending'
    case 1: return 'status-progress'
    case 2: return 'status-completed'
    default: return ''
  }
}

// 获取状态文本
const getStatusText = (status: number) => {
  switch (status) {
    case 0: return '待处理'
    case 1: return '进行中'
    case 2: return '已完成'
    default: return '未知'
  }
}

// 格式化时间
const formatDateTime = (dateTime: string) => {
  if (!dateTime) return '-'
  return dateTime.replace('T', ' ').substring(0, 16)
}

// 解析JSON数组的数量
const parseCount = (jsonStr: string) => {
  if (!jsonStr) return 0
  try {
    const arr = JSON.parse(jsonStr)
    return Array.isArray(arr) ? arr.length : 0
  } catch {
    return 0
  }
}

onMounted(() => {
  loadTasks()
  loadPendingCount()
})
</script>

<style scoped>
.my-check-tasks {
  padding: 24px;
  max-width: 1000px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 22px;
  font-weight: 600;
  margin: 0;
  color: #1a1a1a;
}

.pending-badge {
  padding: 6px 14px;
  background: #fef3c7;
  color: #d97706;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 60px;
  color: #9ca3af;
}

.empty-icon {
  color: #d1d5db;
}

.spinner {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.task-card {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 20px;
  transition: all 0.2s;
}

.task-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.task-card.task-pending {
  border-left: 4px solid #f59e0b;
}

.task-card.task-progress {
  border-left: 4px solid #3b82f6;
}

.task-card.task-completed {
  border-left: 4px solid #10b981;
}

.task-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.task-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.task-check-name {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.task-plan-name {
  font-size: 13px;
  color: #6b7280;
}

.task-status-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.task-status-badge.status-pending {
  background: #fef3c7;
  color: #d97706;
}

.task-status-badge.status-progress {
  background: #dbeafe;
  color: #2563eb;
}

.task-status-badge.status-completed {
  background: #d1fae5;
  color: #059669;
}

.task-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 12px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #6b7280;
}

.task-scope {
  display: flex;
  gap: 20px;
  margin-bottom: 16px;
  padding: 12px;
  background: #f9fafb;
  border-radius: 8px;
}

.scope-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #4b5563;
}

.task-actions {
  display: flex;
  gap: 12px;
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 500;
  border-radius: 6px;
  border: none;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-primary {
  background: #2563eb;
  color: white;
}

.btn-primary:hover {
  background: #1d4ed8;
}

.btn-success {
  background: #059669;
  color: white;
}

.btn-success:hover {
  background: #047857;
}

.btn-default {
  background: #f3f4f6;
  color: #374151;
}

.btn-default:hover {
  background: #e5e7eb;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
