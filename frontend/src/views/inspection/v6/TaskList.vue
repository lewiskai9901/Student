<template>
  <div class="v6-task-list">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <div class="title-section">
          <h1 class="page-title">检查任务</h1>
          <p class="page-subtitle">查看和管理所有检查任务</p>
        </div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-section">
      <div class="stat-card">
        <div class="stat-icon pending">
          <el-icon><Clock /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ pendingCount }}</span>
          <span class="stat-label">待开始</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon progress">
          <el-icon><Loading /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ inProgressCount }}</span>
          <span class="stat-label">进行中</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon submitted">
          <el-icon><Upload /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ submittedCount }}</span>
          <span class="stat-label">待审核</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon published">
          <el-icon><CircleCheck /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ publishedCount }}</span>
          <span class="stat-label">已发布</span>
        </div>
      </div>
    </div>

    <!-- 筛选区域 -->
    <div class="filter-section">
      <div class="filter-left">
        <div class="filter-tabs">
          <button
            v-for="tab in statusTabs"
            :key="tab.value"
            class="filter-tab"
            :class="{ active: queryParams.status === tab.value }"
            @click="handleTabChange(tab.value)"
          >
            {{ tab.label }}
          </button>
        </div>
      </div>
      <div class="filter-right">
        <el-date-picker
          v-model="queryParams.taskDate"
          type="date"
          placeholder="选择日期"
          value-format="YYYY-MM-DD"
          class="date-picker"
          @change="handleSearch"
          clearable
        />
      </div>
    </div>

    <!-- 任务列表 -->
    <div class="task-section" v-loading="loading">
      <div v-if="taskList.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无任务" />
      </div>

      <div class="task-table-wrapper" v-else>
        <table class="modern-table">
          <thead>
            <tr>
              <th>任务</th>
              <th>日期</th>
              <th>进度</th>
              <th>状态</th>
              <th>检查员</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="task in taskList" :key="task.id" @click="viewTask(task)" class="task-row">
              <td>
                <div class="task-info">
                  <span class="task-code">{{ task.taskCode }}</span>
                  <span class="project-name">{{ task.projectName }}</span>
                </div>
              </td>
              <td>
                <div class="date-info">
                  <span class="date-value">{{ formatDate(task.taskDate) }}</span>
                  <span class="date-day">{{ getDayOfWeek(task.taskDate) }}</span>
                </div>
              </td>
              <td>
                <div class="progress-cell">
                  <div class="progress-bar">
                    <div class="progress-fill" :style="{ width: getProgressPercent(task) + '%' }"></div>
                  </div>
                  <span class="progress-text">{{ task.completedCount || 0 }}/{{ task.targetCount || 0 }}</span>
                </div>
              </td>
              <td>
                <span class="status-badge" :class="task.status.toLowerCase()">
                  {{ getStatusLabel(task.status) }}
                </span>
              </td>
              <td>
                <div class="inspector-info" v-if="task.inspectorName">
                  <div class="avatar">{{ task.inspectorName.charAt(0) }}</div>
                  <span class="name">{{ task.inspectorName }}</span>
                </div>
                <span v-else class="no-inspector">未分配</span>
              </td>
              <td @click.stop>
                <div class="action-buttons">
                  <el-button
                    v-if="task.status === 'IN_PROGRESS'"
                    type="primary"
                    size="small"
                    round
                    @click="executeTask(task)"
                  >
                    执行检查
                  </el-button>
                  <el-button
                    v-else-if="task.status === 'SUBMITTED'"
                    type="warning"
                    size="small"
                    round
                    @click="reviewTask(task)"
                  >
                    审核
                  </el-button>
                  <el-button
                    v-else-if="task.status === 'REVIEWED'"
                    type="success"
                    size="small"
                    round
                    @click="publishTask(task)"
                  >
                    发布
                  </el-button>
                  <el-button
                    v-else
                    text
                    type="primary"
                    @click="viewTask(task)"
                  >
                    查看详情
                  </el-button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination-section" v-if="total > queryParams.size">
      <el-pagination
        v-model:current-page="queryParams.page"
        v-model:page-size="queryParams.size"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @size-change="loadData"
        @current-change="loadData"
        background
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Clock, Loading, Upload, CircleCheck } from '@element-plus/icons-vue'
import { v6TaskApi } from '@/api/v6Inspection'
import type { InspectionTask, TaskQueryParams } from '@/types/v6Inspection'

const router = useRouter()
const loading = ref(false)
const taskList = ref<InspectionTask[]>([])
const total = ref(0)

const queryParams = reactive<TaskQueryParams>({
  page: 1,
  size: 10,
  taskDate: '',
  status: ''
})

// 统计数据
const pendingCount = computed(() => taskList.value.filter(t => t.status === 'PENDING').length)
const inProgressCount = computed(() => taskList.value.filter(t => t.status === 'IN_PROGRESS').length)
const submittedCount = computed(() => taskList.value.filter(t => t.status === 'SUBMITTED').length)
const publishedCount = computed(() => taskList.value.filter(t => t.status === 'PUBLISHED').length)

// 状态标签
const statusTabs = [
  { label: '全部', value: '' },
  { label: '待开始', value: 'PENDING' },
  { label: '进行中', value: 'IN_PROGRESS' },
  { label: '已提交', value: 'SUBMITTED' },
  { label: '已审核', value: 'REVIEWED' },
  { label: '已发布', value: 'PUBLISHED' }
]

const getStatusLabel = (status: string) => {
  const labels: Record<string, string> = {
    PENDING: '待开始',
    IN_PROGRESS: '进行中',
    SUBMITTED: '已提交',
    REVIEWED: '已审核',
    PUBLISHED: '已发布',
    CANCELLED: '已取消'
  }
  return labels[status] || status
}

const formatDate = (date: string) => {
  if (!date) return '-'
  const d = new Date(date)
  return `${d.getMonth() + 1}月${d.getDate()}日`
}

const getDayOfWeek = (date: string) => {
  if (!date) return ''
  const days = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return days[new Date(date).getDay()]
}

const getProgressPercent = (task: InspectionTask) => {
  const total = task.targetCount || 0
  const completed = task.completedCount || 0
  if (total === 0) return 0
  return Math.round((completed / total) * 100)
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await v6TaskApi.list(queryParams)
    taskList.value = res.list || res.records || []
    total.value = Number(res.total) || 0
  } catch (error) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const handleTabChange = (status: string) => {
  queryParams.status = status
  queryParams.page = 1
  loadData()
}

const handleSearch = () => {
  queryParams.page = 1
  loadData()
}

const viewTask = (row: InspectionTask) => router.push(`/inspection/v6/tasks/${row.id}`)
const executeTask = (row: InspectionTask) => router.push(`/inspection/v6/tasks/${row.id}/execute`)

const reviewTask = async (row: InspectionTask) => {
  try {
    await v6TaskApi.review(row.id)
    ElMessage.success('审核通过')
    loadData()
  } catch (error) {
    ElMessage.error('审核失败')
  }
}

const publishTask = async (row: InspectionTask) => {
  try {
    await v6TaskApi.publish(row.id)
    ElMessage.success('发布成功')
    loadData()
  } catch (error) {
    ElMessage.error('发布失败')
  }
}

onMounted(() => loadData())
</script>

<style scoped lang="scss">
.v6-task-list {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
  padding-bottom: 32px;
}

/* 页面头部 */
.page-header {
  background: white;
  padding: 32px 40px;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
}

.page-title {
  font-size: 28px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.page-subtitle {
  font-size: 14px;
  color: #6b7280;
  margin: 8px 0 0;
}

/* 统计卡片 */
.stats-section {
  max-width: 1400px;
  margin: 24px auto 0;
  padding: 0 40px;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.stat-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  }
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;

  &.pending {
    background: linear-gradient(135deg, #94a3b8 0%, #64748b 100%);
    color: white;
  }

  &.progress {
    background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
    color: white;
  }

  &.submitted {
    background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
    color: white;
  }

  &.published {
    background: linear-gradient(135deg, #10b981 0%, #059669 100%);
    color: white;
  }
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1f2937;
}

.stat-label {
  font-size: 13px;
  color: #6b7280;
  margin-top: 2px;
}

/* 筛选区域 */
.filter-section {
  max-width: 1400px;
  margin: 24px auto 0;
  padding: 0 40px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
}

.filter-tabs {
  display: flex;
  gap: 8px;
  background: white;
  padding: 6px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.filter-tab {
  padding: 10px 20px;
  border: none;
  background: transparent;
  border-radius: 8px;
  font-size: 14px;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    color: #1f2937;
    background: #f3f4f6;
  }

  &.active {
    background: #409eff;
    color: white;
  }
}

.date-picker {
  width: 180px;

  :deep(.el-input__wrapper) {
    border-radius: 10px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  }
}

/* 任务列表 */
.task-section {
  max-width: 1400px;
  margin: 24px auto 0;
  padding: 0 40px;
}

.empty-state {
  background: white;
  border-radius: 16px;
  padding: 80px 0;
}

.task-table-wrapper {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.modern-table {
  width: 100%;
  border-collapse: collapse;

  th {
    padding: 16px 20px;
    text-align: left;
    font-size: 13px;
    font-weight: 600;
    color: #6b7280;
    background: #f9fafb;
    border-bottom: 1px solid #e5e7eb;
  }

  td {
    padding: 20px;
    border-bottom: 1px solid #f3f4f6;
  }
}

.task-row {
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: #f9fafb;
  }

  &:last-child td {
    border-bottom: none;
  }
}

.task-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.task-code {
  font-weight: 600;
  color: #1f2937;
}

.project-name {
  font-size: 13px;
  color: #6b7280;
}

.date-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.date-value {
  font-weight: 500;
  color: #1f2937;
}

.date-day {
  font-size: 12px;
  color: #9ca3af;
}

.progress-cell {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 140px;
}

.progress-bar {
  flex: 1;
  height: 6px;
  background: #e5e7eb;
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6 0%, #2563eb 100%);
  border-radius: 3px;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 13px;
  color: #6b7280;
  white-space: nowrap;
}

.status-badge {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;

  &.pending {
    background: #f3f4f6;
    color: #6b7280;
  }

  &.in_progress {
    background: #dbeafe;
    color: #2563eb;
  }

  &.submitted {
    background: #fef3c7;
    color: #d97706;
  }

  &.reviewed {
    background: #d1fae5;
    color: #059669;
  }

  &.published {
    background: #d1fae5;
    color: #059669;
  }

  &.cancelled {
    background: #fee2e2;
    color: #dc2626;
  }
}

.inspector-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
}

.name {
  font-size: 14px;
  color: #1f2937;
}

.no-inspector {
  font-size: 13px;
  color: #9ca3af;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

/* 分页 */
.pagination-section {
  max-width: 1400px;
  margin: 32px auto 0;
  padding: 0 40px;
  display: flex;
  justify-content: center;
}

/* 响应式 */
@media (max-width: 1200px) {
  .stats-section {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .page-header {
    padding: 24px 20px;
  }

  .stats-section,
  .filter-section,
  .task-section,
  .pagination-section {
    padding: 0 20px;
  }

  .stats-section {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .filter-section {
    flex-direction: column;
    gap: 16px;
  }

  .filter-tabs {
    width: 100%;
    overflow-x: auto;
  }

  .task-table-wrapper {
    overflow-x: auto;
  }

  .modern-table {
    min-width: 800px;
  }
}
</style>
