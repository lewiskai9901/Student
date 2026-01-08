<template>
  <div class="check-plan-list">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">检查计划</h1>
        <p class="page-desc">管理量化检查计划，每个计划关联一个模板并包含多次日常检查</p>
      </div>
      <div class="header-actions">
        <button class="btn btn-primary" @click="handleCreate">
          <Plus class="icon" />
          新建计划
        </button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-section">
      <div class="stat-card">
        <div class="stat-icon total">
          <ClipboardList class="icon" />
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ statistics.totalPlans }}</span>
          <span class="stat-label">总计划数</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon draft">
          <FileEdit class="icon" />
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ statistics.draftCount }}</span>
          <span class="stat-label">草稿</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon progress">
          <Play class="icon" />
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ statistics.inProgressCount }}</span>
          <span class="stat-label">进行中</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon finished">
          <CheckCircle class="icon" />
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ statistics.finishedCount }}</span>
          <span class="stat-label">已结束</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon archived">
          <Archive class="icon" />
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ statistics.archivedCount }}</span>
          <span class="stat-label">已归档</span>
        </div>
      </div>
    </div>

    <!-- 筛选条件 -->
    <div class="filter-section">
      <div class="filter-row">
        <div class="filter-item">
          <label>计划名称</label>
          <input
            type="text"
            v-model="queryParams.planName"
            placeholder="搜索计划名称"
            @keyup.enter="loadData"
          />
        </div>
        <div class="filter-item">
          <label>状态</label>
          <select v-model="queryParams.status">
            <option :value="undefined">全部</option>
            <option :value="0">草稿</option>
            <option :value="1">进行中</option>
            <option :value="2">已结束</option>
            <option :value="3">已归档</option>
          </select>
        </div>
        <div class="filter-item">
          <label>开始日期</label>
          <div class="date-range">
            <input type="date" v-model="queryParams.startDateFrom" />
            <span class="separator">-</span>
            <input type="date" v-model="queryParams.startDateTo" />
          </div>
        </div>
        <div class="filter-actions">
          <button class="btn btn-default" @click="resetQuery">重置</button>
          <button class="btn btn-primary" @click="loadData">查询</button>
        </div>
      </div>
    </div>

    <!-- 计划列表 -->
    <div class="plan-list">
      <!-- 加载状态 -->
      <div v-if="loading" class="loading-state">
        <Loader2 class="spinner" />
        <span>加载中...</span>
      </div>

      <!-- 空状态 -->
      <div v-else-if="planList.length === 0" class="empty-state">
        <ClipboardList class="empty-icon" />
        <p>暂无检查计划</p>
        <button class="btn btn-primary" @click="handleCreate">创建第一个计划</button>
      </div>

      <!-- 计划卡片列表 -->
      <div v-else class="plan-cards">
        <div
          v-for="plan in planList"
          :key="plan.id"
          class="plan-card"
          @click="viewDetail(plan.id)"
        >
          <div class="card-header">
            <div class="plan-info">
              <h3 class="plan-name">{{ plan.planName }}</h3>
              <span class="plan-code">{{ plan.planCode }}</span>
            </div>
            <span class="status-badge" :class="getStatusClass(plan.status)">
              {{ getStatusText(plan.status) }}
            </span>
          </div>

          <div class="card-body">
            <div class="plan-meta">
              <div class="meta-item">
                <FileText class="icon-sm" />
                <span>{{ plan.templateName }}</span>
              </div>
              <div class="meta-item">
                <Calendar class="icon-sm" />
                <span>{{ formatDateRange(plan.startDate, plan.endDate) }}</span>
              </div>
              <div class="meta-item" v-if="plan.enableWeight === 1">
                <Scale class="icon-sm" />
                <span>已启用加权</span>
              </div>
            </div>

            <div class="stat-grid">
              <div class="stat-item">
                <span class="stat-value">{{ plan.totalChecks }}</span>
                <span class="stat-label">检查次数</span>
              </div>
              <div class="stat-item">
                <span class="stat-value">{{ plan.totalRecords }}</span>
                <span class="stat-label">检查记录</span>
              </div>
              <div class="stat-item">
                <span class="stat-value deduct">{{ formatScore(plan.totalDeductionScore) }}</span>
                <span class="stat-label">总扣分</span>
              </div>
            </div>
          </div>

          <div class="card-footer">
            <div class="footer-meta">
              <span class="creator" v-if="plan.creatorName">
                <User class="icon-sm" />
                {{ plan.creatorName }}
              </span>
              <span class="create-time">{{ formatDateTime(plan.createdAt) }}</span>
            </div>
            <div class="card-actions" @click.stop>
              <button class="action-btn" @click="viewDetail(plan.id)" title="查看详情">
                <Eye class="icon-sm" />
              </button>
              <!-- 草稿 -> 开始 -->
              <button
                v-if="plan.status === 0"
                class="action-btn success"
                @click="handleStart(plan)"
                title="开始计划"
              >
                <Play class="icon-sm" />
              </button>
              <!-- 进行中 -> 结束 -->
              <button
                v-if="plan.status === 1"
                class="action-btn warning"
                @click="handleFinish(plan)"
                title="结束计划"
              >
                <Square class="icon-sm" />
              </button>
              <!-- 已结束 -> 归档 -->
              <button
                v-if="plan.status === 2"
                class="action-btn"
                @click="handleArchive(plan)"
                title="归档"
              >
                <Archive class="icon-sm" />
              </button>
              <!-- 只有草稿可删除 -->
              <button
                v-if="plan.status === 0"
                class="action-btn danger"
                @click="handleDelete(plan)"
                title="删除"
              >
                <Trash2 class="icon-sm" />
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="total > 0" class="pagination">
        <span class="total-info">共 {{ total }} 条记录</span>
        <div class="page-controls">
          <button
            class="page-btn"
            :disabled="queryParams.pageNum <= 1"
            @click="changePage(queryParams.pageNum - 1)"
          >
            上一页
          </button>
          <span class="page-info">{{ queryParams.pageNum }} / {{ totalPages }}</span>
          <button
            class="page-btn"
            :disabled="queryParams.pageNum >= totalPages"
            @click="changePage(queryParams.pageNum + 1)"
          >
            下一页
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ClipboardList,
  FileEdit,
  Play,
  CheckCircle,
  Archive,
  Plus,
  Loader2,
  FileText,
  Calendar,
  Scale,
  User,
  Eye,
  Square,
  Trash2
} from 'lucide-vue-next'
import {
  getCheckPlanPage,
  getCheckPlanStatistics,
  startPlan,
  finishPlan,
  archivePlan,
  deleteCheckPlan,
  PLAN_STATUS,
  PLAN_STATUS_LABELS
} from '@/api/checkPlan'
import type { CheckPlanListVO, CheckPlanStatisticsVO } from '@/api/checkPlan'

const router = useRouter()

// 查询参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 12,
  planName: '',
  status: undefined as number | undefined,
  startDateFrom: '',
  startDateTo: ''
})

// 数据状态
const loading = ref(false)
const planList = ref<CheckPlanListVO[]>([])
const total = ref(0)
const totalPages = computed(() => Math.ceil(total.value / queryParams.pageSize) || 1)

// 统计数据
const statistics = ref<CheckPlanStatisticsVO>({
  totalPlans: 0,
  draftCount: 0,
  inProgressCount: 0,
  finishedCount: 0,
  archivedCount: 0,
  totalChecks: 0,
  totalRecords: 0,
  totalDeductionScore: 0
})

// 格式化日期范围
const formatDateRange = (start: string, end: string) => {
  if (!start || !end) return '-'
  const formatDate = (d: string) => {
    const date = new Date(d)
    return `${date.getMonth() + 1}/${date.getDate()}`
  }
  return `${formatDate(start)} - ${formatDate(end)}`
}

// 格式化分数
const formatScore = (score: number | undefined | null) => {
  if (score === undefined || score === null) return '0'
  return Number(score).toFixed(1)
}

// 格式化日期时间
const formatDateTime = (dateTime: string) => {
  if (!dateTime) return '-'
  const d = new Date(dateTime)
  return `${d.getMonth() + 1}/${d.getDate()} ${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
}

// 状态样式
const getStatusClass = (status: number) => {
  const map: Record<number, string> = {
    [PLAN_STATUS.DRAFT]: 'draft',
    [PLAN_STATUS.IN_PROGRESS]: 'progress',
    [PLAN_STATUS.FINISHED]: 'finished',
    [PLAN_STATUS.ARCHIVED]: 'archived'
  }
  return map[status] || 'draft'
}

// 状态文本
const getStatusText = (status: number) => {
  return PLAN_STATUS_LABELS[status] || '未知'
}

// 加载统计数据
const loadStatistics = async () => {
  try {
    const res = await getCheckPlanStatistics()
    if (res) {
      statistics.value = res
    }
  } catch (error) {
    console.error('加载统计数据失败', error)
  }
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const response = await getCheckPlanPage({
      ...queryParams,
      planName: queryParams.planName || undefined,
      startDateFrom: queryParams.startDateFrom || undefined,
      startDateTo: queryParams.startDateTo || undefined
    })
    if (response && response.records !== undefined) {
      planList.value = response.records || []
      total.value = Number(response.total) || 0
    } else {
      planList.value = []
      total.value = 0
    }
  } catch (error) {
    console.error('加载数据失败', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// 重置查询
const resetQuery = () => {
  queryParams.pageNum = 1
  queryParams.planName = ''
  queryParams.status = undefined
  queryParams.startDateFrom = ''
  queryParams.startDateTo = ''
  loadData()
}

// 翻页
const changePage = (page: number) => {
  queryParams.pageNum = page
  loadData()
}

// 创建计划
const handleCreate = () => {
  router.push('/quantification/check-plan/create')
}

// 查看详情
const viewDetail = (id: string | number) => {
  router.push(`/quantification/check-plan/${id}`)
}

// 开始计划
const handleStart = async (plan: CheckPlanListVO) => {
  try {
    await ElMessageBox.confirm(
      `确定要开始计划"${plan.planName}"吗？开始后可以创建日常检查。`,
      '开始计划',
      {
        confirmButtonText: '确定开始',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    await startPlan(plan.id)
    ElMessage.success('计划已开始')
    loadData()
    loadStatistics()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '操作失败')
    }
  }
}

// 结束计划
const handleFinish = async (plan: CheckPlanListVO) => {
  try {
    await ElMessageBox.confirm(
      `确定要结束计划"${plan.planName}"吗？结束后将无法再添加检查。`,
      '结束计划',
      {
        confirmButtonText: '确定结束',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await finishPlan(plan.id)
    ElMessage.success('计划已结束')
    loadData()
    loadStatistics()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '操作失败')
    }
  }
}

// 归档计划
const handleArchive = async (plan: CheckPlanListVO) => {
  try {
    await ElMessageBox.confirm(
      `确定要归档计划"${plan.planName}"吗？`,
      '归档确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    await archivePlan(plan.id)
    ElMessage.success('归档成功')
    loadData()
    loadStatistics()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '归档失败')
    }
  }
}

// 删除计划
const handleDelete = async (plan: CheckPlanListVO) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除计划"${plan.planName}"吗？此操作不可恢复！`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await deleteCheckPlan(plan.id)
    ElMessage.success('删除成功')
    loadData()
    loadStatistics()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

onMounted(() => {
  loadData()
  loadStatistics()
})
</script>

<style scoped>
.check-plan-list {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
}

.page-desc {
  font-size: 14px;
  color: #666;
  margin: 4px 0 0;
}

/* 按钮 */
.btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
}

.btn-primary {
  background: #2563eb;
  color: white;
}

.btn-primary:hover {
  background: #1d4ed8;
}

.btn-default {
  background: white;
  color: #374151;
  border: 1px solid #d1d5db;
}

.btn-default:hover {
  background: #f9fafb;
}

.icon {
  width: 18px;
  height: 18px;
}

.icon-sm {
  width: 16px;
  height: 16px;
}

/* 统计卡片区 */
.stats-section {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: white;
  border-radius: 10px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  border: 1px solid #e5e7eb;
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon.total {
  background: #eff6ff;
  color: #2563eb;
}

.stat-icon.draft {
  background: #f3f4f6;
  color: #6b7280;
}

.stat-icon.progress {
  background: #fef3c7;
  color: #d97706;
}

.stat-icon.finished {
  background: #d1fae5;
  color: #059669;
}

.stat-icon.archived {
  background: #e5e7eb;
  color: #6b7280;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-info .stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #1a1a1a;
}

.stat-info .stat-label {
  font-size: 13px;
  color: #6b7280;
}

/* 筛选区 */
.filter-section {
  background: white;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 24px;
  border: 1px solid #e5e7eb;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 16px;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.filter-item label {
  font-size: 13px;
  color: #666;
  font-weight: 500;
}

.filter-item input,
.filter-item select {
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
  min-width: 140px;
}

.filter-item input:focus,
.filter-item select:focus {
  outline: none;
  border-color: #2563eb;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.1);
}

.date-range {
  display: flex;
  align-items: center;
  gap: 8px;
}

.date-range .separator {
  color: #9ca3af;
}

.filter-actions {
  display: flex;
  gap: 8px;
  margin-left: auto;
}

/* 计划列表 */
.plan-list {
  min-height: 400px;
}

.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  color: #9ca3af;
}

.spinner {
  width: 32px;
  height: 32px;
  animation: spin 1s linear infinite;
  margin-bottom: 12px;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.empty-icon {
  width: 64px;
  height: 64px;
  margin-bottom: 16px;
  color: #d1d5db;
}

.empty-state p {
  margin-bottom: 16px;
  font-size: 16px;
}

/* 计划卡片 */
.plan-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 16px;
}

.plan-card {
  background: white;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;
}

.plan-card:hover {
  border-color: #2563eb;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.card-header {
  padding: 16px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  border-bottom: 1px solid #f3f4f6;
}

.plan-name {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 4px;
}

.plan-code {
  font-size: 12px;
  color: #9ca3af;
  font-family: monospace;
}

.status-badge {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  flex-shrink: 0;
}

.status-badge.draft {
  background: #f3f4f6;
  color: #6b7280;
}

.status-badge.progress {
  background: #fef3c7;
  color: #d97706;
}

.status-badge.finished {
  background: #d1fae5;
  color: #059669;
}

.status-badge.archived {
  background: #e5e7eb;
  color: #6b7280;
}

.card-body {
  padding: 16px;
}

.plan-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 16px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #6b7280;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.stat-item {
  text-align: center;
  padding: 8px;
  background: #f9fafb;
  border-radius: 6px;
}

.stat-item .stat-value {
  display: block;
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
}

.stat-item .stat-value.deduct {
  color: #dc2626;
}

.stat-item .stat-label {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 2px;
}

.card-footer {
  padding: 12px 16px;
  background: #f9fafb;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.footer-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: #6b7280;
}

.creator {
  display: flex;
  align-items: center;
  gap: 4px;
}

.create-time {
  font-size: 12px;
  color: #9ca3af;
}

.card-actions {
  display: flex;
  gap: 4px;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.15s;
}

.action-btn:hover {
  background: #e5e7eb;
  color: #1a1a1a;
}

.action-btn.success:hover {
  background: #d1fae5;
  color: #059669;
}

.action-btn.warning:hover {
  background: #fef3c7;
  color: #d97706;
}

.action-btn.danger:hover {
  background: #fee2e2;
  color: #dc2626;
}

/* 分页 */
.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 24px;
  padding: 16px;
  background: white;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.total-info {
  font-size: 14px;
  color: #6b7280;
}

.page-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-btn {
  padding: 6px 14px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: white;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.15s;
}

.page-btn:hover:not(:disabled) {
  border-color: #2563eb;
  color: #2563eb;
}

.page-btn:disabled {
  color: #d1d5db;
  cursor: not-allowed;
}

.page-info {
  font-size: 14px;
  color: #6b7280;
}

/* 响应式 */
@media (max-width: 1200px) {
  .stats-section {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .check-plan-list {
    padding: 16px;
  }

  .page-header {
    flex-direction: column;
    gap: 16px;
  }

  .stats-section {
    grid-template-columns: repeat(2, 1fr);
  }

  .filter-row {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-actions {
    margin-left: 0;
    justify-content: flex-end;
  }

  .plan-cards {
    grid-template-columns: 1fr;
  }

  .pagination {
    flex-direction: column;
    gap: 12px;
  }
}
</style>
