<template>
  <div class="export-center">
    <!-- Page Header -->
    <el-card shadow="never" class="header-card">
      <div class="page-header">
        <h3>导出中心</h3>
        <span class="header-desc">选择导出场景，配置筛选条件，预估并导出数据</span>
      </div>
    </el-card>

    <!-- Scenario Tabs -->
    <el-card shadow="never" class="main-card">
      <el-tabs v-model="activeScenario" @tab-change="onScenarioChange">
        <el-tab-pane
          v-for="(config, key) in ExportScenarioConfig"
          :key="key"
          :label="config.label"
          :name="key"
        />
      </el-tabs>

      <!-- Scenario Description -->
      <el-alert
        :title="currentScenarioConfig.label"
        :description="currentScenarioConfig.description"
        type="info"
        :closable="false"
        show-icon
        class="scenario-desc"
      />

      <!-- Filter Panel -->
      <div class="filter-panel">
        <el-form :model="filterForm" label-width="80px" class="filter-form">
          <el-row :gutter="16">
            <!-- Date Range (all scenarios) -->
            <el-col :span="10">
              <el-form-item label="日期范围">
                <el-date-picker
                  v-model="filterForm.dateRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  value-format="YYYY-MM-DD"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>

            <!-- Session Selector (for DEDUCTION_DETAIL and RATING_REPORT) -->
            <el-col :span="7" v-if="activeScenario !== 'STATISTICS_REPORT'">
              <el-form-item label="检查批次">
                <el-select
                  v-model="filterForm.sessionId"
                  placeholder="全部批次"
                  clearable
                  filterable
                  style="width: 100%"
                >
                  <el-option
                    v-for="session in sessionList"
                    :key="session.id"
                    :label="session.name"
                    :value="session.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>

            <!-- Class Filter -->
            <el-col :span="7" v-if="activeScenario !== 'STATISTICS_REPORT'">
              <el-form-item label="班级筛选">
                <el-select
                  v-model="filterForm.classIds"
                  placeholder="全部班级"
                  clearable
                  multiple
                  collapse-tags
                  collapse-tags-tooltip
                  filterable
                  style="width: 100%"
                >
                  <el-option
                    v-for="cls in classList"
                    :key="cls.id"
                    :label="cls.name"
                    :value="cls.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>

            <!-- Category Filter (for DEDUCTION_DETAIL) -->
            <el-col :span="7" v-if="activeScenario === 'DEDUCTION_DETAIL'">
              <el-form-item label="扣分分类">
                <el-select
                  v-model="filterForm.categoryIds"
                  placeholder="全部分类"
                  clearable
                  multiple
                  collapse-tags
                  collapse-tags-tooltip
                  style="width: 100%"
                >
                  <el-option
                    v-for="cat in categoryList"
                    :key="cat.id"
                    :label="cat.name"
                    :value="cat.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </div>

      <!-- Action Bar -->
      <div class="action-bar">
        <el-button @click="handleEstimate" :loading="estimating">
          <el-icon><DataAnalysis /></el-icon>
          预估数据量
        </el-button>
        <el-button type="primary" @click="handleExport" :loading="exporting" :disabled="!hasDateRange">
          <el-icon><Download /></el-icon>
          导出数据
        </el-button>

        <!-- Estimate Result -->
        <transition name="el-fade-in">
          <el-tag v-if="estimateResult" size="large" class="estimate-tag" effect="plain">
            预估记录数：<strong>{{ estimateResult.estimatedCount.toLocaleString() }}</strong> 条
            <span v-if="estimateResult.async" class="async-hint">（数据量较大，将异步处理）</span>
          </el-tag>
        </transition>
      </div>
    </el-card>

    <!-- Export History -->
    <el-card shadow="never" class="history-card">
      <template #header>
        <div class="card-header">
          <span>导出历史</span>
          <el-button link type="primary" @click="loadExportTasks">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>
      </template>

      <el-table :data="exportTasks" stripe v-loading="loadingTasks" empty-text="暂无导出记录">
        <el-table-column prop="taskCode" label="任务编号" width="180" />
        <el-table-column prop="exportType" label="导出类型" width="120">
          <template #default="{ row }">
            {{ ExportScenarioConfig[row.exportType as ExportScenario]?.label || row.exportType }}
          </template>
        </el-table-column>
        <el-table-column prop="exportFormat" label="格式" width="80" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="ExportStatusConfig[row.status as ExportStatus]?.type || 'info'" size="small">
              {{ ExportStatusConfig[row.status as ExportStatus]?.label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="progress" label="进度" width="140">
          <template #default="{ row }">
            <el-progress
              :percentage="row.progress || 0"
              :status="row.status === 'COMPLETED' ? 'success' : row.status === 'FAILED' ? 'exception' : undefined"
              :stroke-width="8"
            />
          </template>
        </el-table-column>
        <el-table-column prop="fileName" label="文件名" min-width="200" show-overflow-tooltip />
        <el-table-column prop="fileSize" label="大小" width="100">
          <template #default="{ row }">
            {{ row.fileSize ? formatFileSize(row.fileSize) : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="errorMessage" label="错误信息" min-width="180" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="160">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'COMPLETED' && row.downloadUrl"
              link
              type="primary"
              @click="handleDownload(row)"
            >
              下载
            </el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, DataAnalysis, Refresh } from '@element-plus/icons-vue'
import {
  exportCenterApi,
  ExportScenarioConfig,
  type ExportScenario,
  type ExportRequest,
  type ExportEstimate
} from '@/api/exportCenter'
import { listExportTasks } from '@/api/inspectionExport'
import { ExportStatusConfig, type ExportTask, type ExportStatus } from '@/types/analytics'

// ==================== State ====================

const activeScenario = ref<ExportScenario>('DEDUCTION_DETAIL')

const filterForm = reactive({
  dateRange: null as [string, string] | null,
  sessionId: undefined as number | undefined,
  classIds: [] as number[],
  categoryIds: [] as number[]
})

const estimating = ref(false)
const exporting = ref(false)
const loadingTasks = ref(false)
const estimateResult = ref<ExportEstimate | null>(null)
const exportTasks = ref<ExportTask[]>([])

// Filter options (populated from backend or defaults)
const sessionList = ref<{ id: number; name: string }[]>([])
const classList = ref<{ id: number; name: string }[]>([])
const categoryList = ref<{ id: number; name: string }[]>([])

// ==================== Computed ====================

const currentScenarioConfig = computed(() => {
  return ExportScenarioConfig[activeScenario.value]
})

const hasDateRange = computed(() => {
  return filterForm.dateRange && filterForm.dateRange.length === 2
})

// ==================== Methods ====================

function buildExportRequest(): ExportRequest {
  const req: ExportRequest = {
    scenario: activeScenario.value,
    startDate: filterForm.dateRange?.[0] || '',
    endDate: filterForm.dateRange?.[1] || ''
  }
  if (filterForm.sessionId) {
    req.sessionId = filterForm.sessionId
  }
  if (filterForm.classIds.length > 0) {
    req.classIds = filterForm.classIds
  }
  if (filterForm.categoryIds.length > 0) {
    req.categoryIds = filterForm.categoryIds
  }
  return req
}

function onScenarioChange() {
  estimateResult.value = null
}

async function handleEstimate() {
  if (!hasDateRange.value) {
    ElMessage.warning('请先选择日期范围')
    return
  }
  estimating.value = true
  estimateResult.value = null
  try {
    const result = await exportCenterApi.estimateCount(buildExportRequest())
    estimateResult.value = result
  } catch (e: any) {
    ElMessage.error(e.message || '预估数据量失败')
  } finally {
    estimating.value = false
  }
}

async function handleExport() {
  if (!hasDateRange.value) {
    ElMessage.warning('请先选择日期范围')
    return
  }
  exporting.value = true
  try {
    const result = await exportCenterApi.exportData(buildExportRequest())
    ElMessage.success(`导出成功：${result.fileName}，共 ${result.recordCount} 条记录`)
    if (result.downloadUrl) {
      window.open(result.downloadUrl, '_blank')
    }
    // Refresh task list
    await loadExportTasks()
  } catch (e: any) {
    ElMessage.error(e.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

async function loadExportTasks() {
  loadingTasks.value = true
  try {
    exportTasks.value = await listExportTasks()
  } catch (e: any) {
    ElMessage.error(e.message || '加载导出任务失败')
  } finally {
    loadingTasks.value = false
  }
}

function handleDownload(task: ExportTask) {
  if (task.downloadUrl) {
    window.open(task.downloadUrl, '_blank')
  }
}

// ==================== Formatters ====================

function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

// ==================== Lifecycle ====================

onMounted(() => {
  loadExportTasks()
})
</script>

<style scoped>
.export-center {
  padding: 0;
}

.header-card {
  margin-bottom: 16px;
}

.page-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.page-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.header-desc {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.main-card {
  margin-bottom: 16px;
}

.scenario-desc {
  margin-bottom: 20px;
}

.filter-panel {
  margin-bottom: 16px;
}

.filter-form :deep(.el-form-item) {
  margin-bottom: 12px;
}

.action-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-top: 8px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.estimate-tag {
  margin-left: 8px;
}

.estimate-tag strong {
  color: var(--el-color-primary);
  font-size: 15px;
}

.async-hint {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  margin-left: 4px;
}

.history-card {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
