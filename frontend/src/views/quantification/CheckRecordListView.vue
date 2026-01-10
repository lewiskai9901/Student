<template>
  <div class="check-record-list">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">检查记录</h1>
        <p class="page-desc">查看各次检查的班级扣分详情</p>
      </div>
      <div class="header-actions" v-if="isAdmin">
        <button class="btn btn-primary" @click="handleManualConvert">
          <Plus class="icon" />
          生成记录
        </button>
      </div>
    </div>

    <!-- 筛选条件 -->
    <div class="filter-section">
      <div class="filter-row">
        <div class="filter-item">
          <label>检查日期</label>
          <div class="date-range">
            <input type="date" v-model="queryParams.startDate" />
            <span class="separator">-</span>
            <input type="date" v-model="queryParams.endDate" />
          </div>
        </div>
        <div class="filter-item" v-if="isAdmin">
          <label>状态</label>
          <select v-model="queryParams.status">
            <option :value="null">全部</option>
            <option :value="1">已发布</option>
            <option :value="2">已归档</option>
          </select>
        </div>
        <div class="filter-item" v-if="isAdmin">
          <label>检查计划</label>
          <select v-model="queryParams.planId">
            <option :value="null">全部</option>
            <option v-for="plan in planList" :key="plan.id" :value="plan.id">
              {{ plan.planName }}
            </option>
          </select>
        </div>
        <div class="filter-actions">
          <button class="btn btn-default" @click="resetQuery">重置</button>
          <button class="btn btn-primary" @click="loadData">查询</button>
        </div>
      </div>
    </div>

    <!-- 记录列表 -->
    <div class="record-list">
      <!-- 加载状态 -->
      <div v-if="loading" class="loading-state">
        <Loader2 class="spinner" />
        <span>加载中...</span>
      </div>

      <!-- 空状态 -->
      <div v-else-if="recordList.length === 0" class="empty-state">
        <FileText class="empty-icon" />
        <p>暂无检查记录</p>
      </div>

      <!-- 记录卡片列表 -->
      <div v-else class="record-cards">
        <div
          v-for="record in recordList"
          :key="record.id"
          class="record-card"
          @click="viewDetail(record.id)"
        >
          <div class="card-header">
            <div class="check-info">
              <h3 class="check-name">{{ record.checkName }}</h3>
              <span class="check-date">{{ formatDate(record.checkDate) }}</span>
            </div>
            <span class="status-badge" :class="getStatusClass(record.status)">
              {{ getStatusText(record.status) }}
            </span>
          </div>

          <div class="card-body">
            <div class="stat-grid">
              <div class="stat-item">
                <span class="stat-value">{{ record.totalClasses }}</span>
                <span class="stat-label">涉及班级</span>
              </div>
              <div class="stat-item">
                <span class="stat-value deduct">{{ formatScore(record.totalDeductionScore || record.totalScore) }}</span>
                <span class="stat-label">总扣分</span>
              </div>
              <div class="stat-item">
                <span class="stat-value">{{ formatScore(record.avgScore) }}</span>
                <span class="stat-label">平均扣分</span>
              </div>
              <div class="stat-item">
                <span class="stat-value">{{ record.totalDeductionCount || '-' }}</span>
                <span class="stat-label">扣分项数</span>
              </div>
            </div>
          </div>

          <div class="card-footer">
            <div class="meta-info">
              <span class="checker">
                <User class="icon-sm" />
                {{ record.checkerName }}
              </span>
              <span class="code">{{ record.recordCode }}</span>
            </div>
            <div class="card-actions" @click.stop>
              <button class="action-btn" @click="viewDetail(record.id)" title="查看详情">
                <Eye class="icon-sm" />
              </button>
              <button
                v-if="isAdmin && record.status === 1"
                class="action-btn"
                @click="handleArchive(record)"
                title="归档"
              >
                <Archive class="icon-sm" />
              </button>
              <button
                v-if="isAdmin"
                class="action-btn danger"
                @click="handleDelete(record)"
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

    <!-- 生成记录对话框 -->
    <Teleport to="body">
      <div v-if="convertDialogVisible" class="modal-overlay" @click.self="convertDialogVisible = false">
        <div class="modal-content">
          <div class="modal-header">
            <h3>生成检查记录</h3>
            <button class="close-btn" @click="convertDialogVisible = false">
              <X class="icon" />
            </button>
          </div>
          <div class="modal-body">
            <div class="form-item">
              <label>日常检查ID</label>
              <input
                v-model="convertForm.dailyCheckId"
                type="text"
                placeholder="输入已完成的日常检查ID"
              />
            </div>
            <div class="tip-box">
              <Info class="icon-sm" />
              <p>从已完成的日常检查生成检查记录，生成后可查看各班扣分详情</p>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn btn-default" @click="convertDialogVisible = false">取消</button>
            <button class="btn btn-primary" :disabled="converting" @click="confirmConvert">
              <Loader2 v-if="converting" class="spinner-sm" />
              确认生成
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  FileText,
  Loader2,
  Plus,
  Eye,
  Archive,
  Trash2,
  User,
  X,
  Info
} from 'lucide-vue-next'
import {
  getCheckRecordsList,
  convertDailyCheck,
  deleteCheckRecord,
  publishCheckRecord
} from '@/api/v2/quantification'
import type { CheckRecord } from '@/api/v2/quantification'
import { getCheckPlanPage } from '@/api/v2/quantification'
import type { CheckPlanListVO } from '@/api/v2/quantification'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

// 权限判断
const isAdmin = computed(() => {
  return authStore.hasRole('SUPER_ADMIN') || authStore.hasRole('SCHOOL_ADMIN') ||
         authStore.hasRole('admin') || authStore.hasRole('管理员')
})

// 查询参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 12,
  startDate: '',
  endDate: '',
  status: null as number | null,
  planId: null as string | null
})

// 数据状态
const loading = ref(false)
const recordList = ref<CheckRecord[]>([])
const total = ref(0)

// 检查计划列表
const planList = ref<CheckPlanListVO[]>([])
const totalPages = computed(() => Math.ceil(total.value / queryParams.pageSize) || 1)

// 生成记录
const convertDialogVisible = ref(false)
const converting = ref(false)
const convertForm = reactive({
  dailyCheckId: ''
})

// 格式化日期
const formatDate = (date: string) => {
  if (!date) return '-'
  const d = new Date(date)
  const month = d.getMonth() + 1
  const day = d.getDate()
  const weekDay = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][d.getDay()]
  return `${month}月${day}日 ${weekDay}`
}

// 格式化分数
const formatScore = (score: number | undefined | null) => {
  if (score === undefined || score === null) return '0'
  return Number(score).toFixed(1)
}

// 状态样式
const getStatusClass = (status: number) => {
  return status === 2 ? 'archived' : 'published'
}

// 状态文本
const getStatusText = (status: number) => {
  return status === 2 ? '已归档' : '已发布'
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const response = await getCheckRecordsList(queryParams)
    if (response && response.records !== undefined) {
      recordList.value = response.records || []
      total.value = parseInt(response.total) || 0
    } else if (Array.isArray(response)) {
      recordList.value = response
      total.value = response.length
    } else {
      recordList.value = []
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
  queryParams.startDate = ''
  queryParams.endDate = ''
  queryParams.status = null
  queryParams.planId = null
  loadData()
}

// 加载检查计划列表
const loadPlanList = async () => {
  try {
    const res = await getCheckPlanPage({ pageNum: 1, pageSize: 100 })
    if (res && res.records) {
      planList.value = res.records
    }
  } catch (error) {
    console.error('加载检查计划列表失败', error)
  }
}

// 翻页
const changePage = (page: number) => {
  queryParams.pageNum = page
  loadData()
}

// 查看详情
const viewDetail = (id: string) => {
  router.push(`/quantification/check-record/${id}`)
}

// 生成记录
const handleManualConvert = () => {
  convertForm.dailyCheckId = ''
  convertDialogVisible.value = true
}

const confirmConvert = async () => {
  if (!convertForm.dailyCheckId.trim()) {
    ElMessage.warning('请输入日常检查ID')
    return
  }
  converting.value = true
  try {
    await convertDailyCheck(convertForm.dailyCheckId)
    ElMessage.success('生成成功')
    convertDialogVisible.value = false
    loadData()
  } catch (error: any) {
    ElMessage.error(error.message || '生成失败')
  } finally {
    converting.value = false
  }
}

// 归档
const handleArchive = async (record: CheckRecord) => {
  try {
    await ElMessageBox.confirm(`确定要归档"${record.checkName}"吗？`, '归档确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    })
    await publishCheckRecord(record.id)
    ElMessage.success('归档成功')
    loadData()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '归档失败')
    }
  }
}

// 删除
const handleDelete = async (record: CheckRecord) => {
  try {
    await ElMessageBox.confirm(`确定要删除"${record.checkName}"吗？此操作不可恢复！`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteCheckRecord(record.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

onMounted(() => {
  loadData()
  loadPlanList()
})
</script>

<style scoped>
.check-record-list {
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

.btn-primary:disabled {
  background: #93c5fd;
  cursor: not-allowed;
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

/* 记录列表 */
.record-list {
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

.spinner-sm {
  width: 16px;
  height: 16px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.empty-icon {
  width: 48px;
  height: 48px;
  margin-bottom: 12px;
  color: #d1d5db;
}

/* 记录卡片 */
.record-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 16px;
}

.record-card {
  background: white;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;
}

.record-card:hover {
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

.check-name {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 4px;
}

.check-date {
  font-size: 13px;
  color: #6b7280;
}

.status-badge {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.published {
  background: #dbeafe;
  color: #1d4ed8;
}

.status-badge.archived {
  background: #f3f4f6;
  color: #6b7280;
}

.card-body {
  padding: 16px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  display: block;
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
}

.stat-value.deduct {
  color: #dc2626;
}

.stat-label {
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

.meta-info {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: #6b7280;
}

.checker {
  display: flex;
  align-items: center;
  gap: 4px;
}

.code {
  font-family: monospace;
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

/* 模态框 */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 12px;
  width: 100%;
  max-width: 440px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e5e7eb;
}

.modal-header h3 {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

.close-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: #9ca3af;
  cursor: pointer;
}

.close-btn:hover {
  background: #f3f4f6;
  color: #1a1a1a;
}

.modal-body {
  padding: 20px;
}

.form-item {
  margin-bottom: 16px;
}

.form-item label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 6px;
}

.form-item input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
}

.form-item input:focus {
  outline: none;
  border-color: #2563eb;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.1);
}

.tip-box {
  display: flex;
  gap: 10px;
  padding: 12px;
  background: #eff6ff;
  border-radius: 8px;
}

.tip-box .icon-sm {
  flex-shrink: 0;
  color: #2563eb;
  margin-top: 2px;
}

.tip-box p {
  font-size: 13px;
  color: #1e40af;
  margin: 0;
  line-height: 1.5;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 20px;
  border-top: 1px solid #e5e7eb;
}

/* 响应式 */
@media (max-width: 768px) {
  .check-record-list {
    padding: 16px;
  }

  .page-header {
    flex-direction: column;
    gap: 16px;
  }

  .filter-row {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-actions {
    margin-left: 0;
    justify-content: flex-end;
  }

  .record-cards {
    grid-template-columns: 1fr;
  }

  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .pagination {
    flex-direction: column;
    gap: 12px;
  }
}
</style>
