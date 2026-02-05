<template>
  <div class="v6-project-list">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <div class="title-section">
          <h1 class="page-title">检查项目</h1>
          <p class="page-subtitle">管理和监控所有量化检查项目</p>
        </div>
        <el-button type="primary" size="large" class="create-btn" @click="handleCreate">
          <el-icon><Plus /></el-icon>
          创建项目
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-section">
      <div class="stat-card">
        <div class="stat-icon active">
          <el-icon><VideoPlay /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ activeCount }}</span>
          <span class="stat-label">进行中</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon draft">
          <el-icon><EditPen /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ draftCount }}</span>
          <span class="stat-label">草稿</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon completed">
          <el-icon><CircleCheck /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ completedCount }}</span>
          <span class="stat-label">已完成</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon total">
          <el-icon><Folder /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ total }}</span>
          <span class="stat-label">全部项目</span>
        </div>
      </div>
    </div>

    <!-- 筛选和搜索 -->
    <div class="filter-section">
      <div class="filter-tabs">
        <button
          v-for="tab in statusTabs"
          :key="tab.value"
          class="filter-tab"
          :class="{ active: queryParams.status === tab.value }"
          @click="handleTabChange(tab.value)"
        >
          {{ tab.label }}
          <span class="tab-count" v-if="tab.count !== undefined">{{ tab.count }}</span>
        </button>
      </div>
      <div class="search-box">
        <el-input
          v-model="queryParams.projectName"
          placeholder="搜索项目名称..."
          prefix-icon="Search"
          clearable
          class="search-input"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
      </div>
    </div>

    <!-- 项目列表 -->
    <div class="project-grid" v-loading="loading">
      <div v-if="projectList.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无项目">
          <el-button type="primary" @click="handleCreate">创建第一个项目</el-button>
        </el-empty>
      </div>

      <div
        v-for="project in projectList"
        :key="project.id"
        class="project-card"
        @click="handleView(project)"
      >
        <div class="card-header">
          <div class="project-status" :class="project.status.toLowerCase()">
            {{ getStatusLabel(project.status) }}
          </div>
          <el-dropdown trigger="click" @click.stop>
            <button class="more-btn" @click.stop>
              <el-icon><MoreFilled /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleView(project)">
                  <el-icon><View /></el-icon> 查看详情
                </el-dropdown-item>
                <el-dropdown-item v-if="project.status === 'DRAFT'" @click="handleConfig(project)">
                  <el-icon><Setting /></el-icon> 配置
                </el-dropdown-item>
                <el-dropdown-item v-if="project.status === 'DRAFT'" @click="handlePublish(project)" divided>
                  <el-icon><VideoPlay /></el-icon> 发布
                </el-dropdown-item>
                <el-dropdown-item v-if="project.status === 'ACTIVE'" @click="handlePause(project)">
                  <el-icon><VideoPause /></el-icon> 暂停
                </el-dropdown-item>
                <el-dropdown-item v-if="project.status === 'PAUSED'" @click="handleResume(project)">
                  <el-icon><VideoPlay /></el-icon> 恢复
                </el-dropdown-item>
                <el-dropdown-item v-if="project.status === 'DRAFT'" @click="handleDelete(project)" divided class="danger">
                  <el-icon><Delete /></el-icon> 删除
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>

        <div class="card-body">
          <h3 class="project-name">{{ project.projectName }}</h3>
          <p class="project-code">{{ project.projectCode }}</p>

          <div class="project-meta">
            <div class="meta-item">
              <el-icon><Calendar /></el-icon>
              <span>{{ project.startDate }} - {{ project.endDate || '长期' }}</span>
            </div>
            <div class="meta-item">
              <el-icon><Clock /></el-icon>
              <span>{{ getCycleTypeLabel(project.cycleType) }}</span>
            </div>
          </div>
        </div>

        <div class="card-footer">
          <div class="progress-info" v-if="project.status === 'ACTIVE'">
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: getProjectProgress(project) + '%' }"></div>
            </div>
            <span class="progress-text">{{ getProjectProgress(project) }}% 完成</span>
          </div>
          <div class="action-hint" v-else>
            <span v-if="project.status === 'DRAFT'">点击配置并发布</span>
            <span v-else-if="project.status === 'COMPLETED'">项目已完成</span>
            <span v-else-if="project.status === 'PAUSED'">项目已暂停</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination-section" v-if="total > queryParams.size">
      <el-pagination
        v-model:current-page="queryParams.page"
        v-model:page-size="queryParams.size"
        :page-sizes="[12, 24, 48]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        background
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, MoreFilled, View, Setting, VideoPlay, VideoPause, Delete,
  Calendar, Clock, EditPen, CircleCheck, Folder
} from '@element-plus/icons-vue'
import { v6ProjectApi } from '@/api/v6Inspection'
import type { InspectionProject, ProjectQueryParams } from '@/types/v6Inspection'

const router = useRouter()
const loading = ref(false)
const projectList = ref<InspectionProject[]>([])
const total = ref(0)

const queryParams = reactive<ProjectQueryParams>({
  page: 1,
  size: 12,
  projectName: '',
  status: ''
})

// 统计数据
const activeCount = computed(() => projectList.value.filter(p => p.status === 'ACTIVE').length)
const draftCount = computed(() => projectList.value.filter(p => p.status === 'DRAFT').length)
const completedCount = computed(() => projectList.value.filter(p => p.status === 'COMPLETED').length)

// 状态标签
const statusTabs = computed(() => [
  { label: '全部', value: '', count: total.value },
  { label: '进行中', value: 'ACTIVE' },
  { label: '草稿', value: 'DRAFT' },
  { label: '已暂停', value: 'PAUSED' },
  { label: '已完成', value: 'COMPLETED' }
])

const getStatusLabel = (status: string) => {
  const labels: Record<string, string> = {
    DRAFT: '草稿',
    ACTIVE: '进行中',
    PAUSED: '已暂停',
    COMPLETED: '已完成',
    ARCHIVED: '已归档'
  }
  return labels[status] || status
}

const getCycleTypeLabel = (cycleType: string) => {
  const labels: Record<string, string> = {
    DAILY: '每日检查',
    WEEKLY: '每周检查',
    MONTHLY: '每月检查',
    CUSTOM: '自定义周期'
  }
  return labels[cycleType] || cycleType
}

const getProjectProgress = (project: InspectionProject) => {
  // 模拟进度计算
  if (project.status === 'COMPLETED') return 100
  if (project.status === 'DRAFT') return 0
  return Math.floor(Math.random() * 60) + 20
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await v6ProjectApi.list(queryParams)
    projectList.value = res.list || res.records || []
    total.value = Number(res.total) || 0
  } catch (error) {
    console.error('加载项目列表失败:', error)
    ElMessage.error('加载项目列表失败')
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

const handleSizeChange = () => {
  queryParams.page = 1
  loadData()
}

const handleCurrentChange = () => {
  loadData()
}

const handleCreate = () => {
  router.push('/inspection/v6/projects/create')
}

const handleView = (row: InspectionProject) => {
  router.push(`/inspection/v6/projects/${row.id}`)
}

const handleConfig = (row: InspectionProject) => {
  router.push(`/inspection/v6/projects/${row.id}/config`)
}

const handlePublish = async (row: InspectionProject) => {
  try {
    await ElMessageBox.confirm('确定要发布该项目吗？发布后将开始生成检查任务。', '发布项目', {
      confirmButtonText: '确定发布',
      cancelButtonText: '取消',
      type: 'info'
    })
    await v6ProjectApi.publish(row.id)
    ElMessage.success('发布成功')
    loadData()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('发布失败')
    }
  }
}

const handlePause = async (row: InspectionProject) => {
  try {
    await ElMessageBox.confirm('确定要暂停该项目吗？', '暂停项目')
    await v6ProjectApi.pause(row.id)
    ElMessage.success('暂停成功')
    loadData()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('暂停失败')
    }
  }
}

const handleResume = async (row: InspectionProject) => {
  try {
    await ElMessageBox.confirm('确定要恢复该项目吗？', '恢复项目')
    await v6ProjectApi.resume(row.id)
    ElMessage.success('恢复成功')
    loadData()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('恢复失败')
    }
  }
}

const handleDelete = async (row: InspectionProject) => {
  try {
    await ElMessageBox.confirm('确定要删除该项目吗？此操作不可恢复。', '删除项目', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await v6ProjectApi.delete(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.v6-project-list {
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
  display: flex;
  justify-content: space-between;
  align-items: center;
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

.create-btn {
  height: 44px;
  padding: 0 24px;
  font-size: 15px;
  border-radius: 10px;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(64, 158, 255, 0.4);
  }
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

  &.active {
    background: linear-gradient(135deg, #10b981 0%, #059669 100%);
    color: white;
  }

  &.draft {
    background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
    color: white;
  }

  &.completed {
    background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
    color: white;
  }

  &.total {
    background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%);
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
  display: flex;
  align-items: center;
  gap: 8px;

  &:hover {
    color: #1f2937;
    background: #f3f4f6;
  }

  &.active {
    background: #409eff;
    color: white;
  }

  .tab-count {
    font-size: 12px;
    padding: 2px 8px;
    background: rgba(0, 0, 0, 0.06);
    border-radius: 10px;
  }

  &.active .tab-count {
    background: rgba(255, 255, 255, 0.2);
  }
}

.search-box {
  width: 320px;
}

.search-input {
  :deep(.el-input__wrapper) {
    border-radius: 10px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
    padding: 4px 16px;
  }
}

/* 项目网格 */
.project-grid {
  max-width: 1400px;
  margin: 24px auto 0;
  padding: 0 40px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 20px;
  min-height: 200px;
}

.empty-state {
  grid-column: 1 / -1;
  background: white;
  border-radius: 16px;
  padding: 80px 0;
}

.project-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12);
  }
}

.card-header {
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f3f4f6;
}

.project-status {
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;

  &.draft {
    background: #f3f4f6;
    color: #6b7280;
  }

  &.active {
    background: #d1fae5;
    color: #059669;
  }

  &.paused {
    background: #fef3c7;
    color: #d97706;
  }

  &.completed {
    background: #dbeafe;
    color: #2563eb;
  }

  &.archived {
    background: #f3f4f6;
    color: #9ca3af;
  }
}

.more-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
  transition: all 0.2s ease;

  &:hover {
    background: #f3f4f6;
    color: #1f2937;
  }
}

.card-body {
  padding: 20px;
}

.project-name {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.project-code {
  font-size: 13px;
  color: #9ca3af;
  margin: 0 0 16px;
}

.project-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #6b7280;

  .el-icon {
    color: #9ca3af;
  }
}

.card-footer {
  padding: 16px 20px;
  background: #f9fafb;
  border-top: 1px solid #f3f4f6;
}

.progress-info {
  display: flex;
  align-items: center;
  gap: 12px;
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
  background: linear-gradient(90deg, #10b981 0%, #059669 100%);
  border-radius: 3px;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 12px;
  color: #6b7280;
  white-space: nowrap;
}

.action-hint {
  font-size: 13px;
  color: #9ca3af;
}

/* 分页 */
.pagination-section {
  max-width: 1400px;
  margin: 32px auto 0;
  padding: 0 40px;
  display: flex;
  justify-content: center;
}

/* 下拉菜单 */
:deep(.el-dropdown-menu__item) {
  &.danger {
    color: #ef4444 !important;

    &:hover {
      background: #fef2f2 !important;
    }
  }
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

  .header-content {
    flex-direction: column;
    gap: 16px;
    text-align: center;
  }

  .stats-section,
  .filter-section,
  .project-grid,
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

  .search-box {
    width: 100%;
  }

  .project-grid {
    grid-template-columns: 1fr;
  }
}
</style>
