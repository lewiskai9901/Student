<template>
  <div class="export-center">
    <!-- Create Export -->
    <el-card shadow="never" class="filter-card">
      <el-row :gutter="16" align="middle">
        <el-col :span="6">
          <el-select v-model="exportType" placeholder="导出类型">
            <el-option label="班级检查报表" value="CLASS_REPORT" />
            <el-option label="系部汇总" value="DEPARTMENT_SUMMARY" />
            <el-option label="检查明细" value="INSPECTION_DETAIL" />
            <el-option label="违规统计" value="VIOLATION_STATS" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-select v-model="exportFormat" placeholder="导出格式">
            <el-option label="Excel" value="EXCEL" />
            <el-option label="PDF" value="PDF" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-button type="primary" @click="handleCreateExport" :loading="creating">
            创建导出任务
          </el-button>
        </el-col>
        <el-col :span="4">
          <el-button @click="loadTasks">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- Task List -->
    <el-card shadow="never">
      <template #header>
        <span>导出任务列表</span>
      </template>
      <el-table :data="tasks" stripe v-loading="loading">
        <el-table-column prop="taskCode" label="任务编号" width="180" />
        <el-table-column prop="exportType" label="导出类型" width="140" />
        <el-table-column prop="exportFormat" label="格式" width="80" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="ExportStatusConfig[row.status as ExportStatus]?.type || 'info'" size="small">
              {{ ExportStatusConfig[row.status as ExportStatus]?.label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="progress" label="进度" width="120">
          <template #default="{ row }">
            <el-progress :percentage="row.progress || 0" :status="row.status === 'COMPLETED' ? 'success' : row.status === 'FAILED' ? 'exception' : undefined" :stroke-width="8" />
          </template>
        </el-table-column>
        <el-table-column prop="fileName" label="文件名" min-width="200" show-overflow-tooltip />
        <el-table-column prop="fileSize" label="大小" width="100">
          <template #default="{ row }">
            {{ row.fileSize ? formatFileSize(row.fileSize) : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="errorMessage" label="错误信息" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="160">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'COMPLETED' && row.downloadUrl"
              link type="primary"
              @click="handleDownload(row)"
            >下载</el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { createExportTask, listExportTasks } from '@/api/inspectionExport'
import type { ExportTask, ExportStatus, ExportFormat } from '@/types/analytics'
import { ExportStatusConfig } from '@/types/analytics'

const loading = ref(false)
const creating = ref(false)
const tasks = ref<ExportTask[]>([])
const exportType = ref('CLASS_REPORT')
const exportFormat = ref<ExportFormat>('EXCEL')

function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

async function loadTasks() {
  loading.value = true
  try {
    tasks.value = await listExportTasks()
  } catch (e: any) {
    ElMessage.error(e.message || '加载导出任务失败')
  } finally {
    loading.value = false
  }
}

async function handleCreateExport() {
  creating.value = true
  try {
    await createExportTask({
      exportType: exportType.value,
      exportFormat: exportFormat.value
    })
    ElMessage.success('导出任务已创建')
    await loadTasks()
  } catch (e: any) {
    ElMessage.error(e.message || '创建导出任务失败')
  } finally {
    creating.value = false
  }
}

function handleDownload(task: ExportTask) {
  if (task.downloadUrl) {
    window.open(task.downloadUrl, '_blank')
  }
}

onMounted(loadTasks)
</script>

<style scoped>
.export-center {
  padding: 0;
}
.filter-card {
  margin-bottom: 16px;
}
</style>
