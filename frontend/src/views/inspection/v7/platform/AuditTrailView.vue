<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from 'lucide-vue-next'
import { useInspPlatformStore } from '@/stores/insp/inspPlatformStore'
import type { AuditTrailEntry } from '@/types/insp/platform'

const store = useInspPlatformStore()

const loading = ref(false)
const entries = ref<AuditTrailEntry[]>([])

const filters = reactive({
  resourceType: '' as string,
  userId: undefined as number | undefined,
  dateRange: null as [string, string] | null,
})

const resourceTypeOptions = [
  { value: '', label: '全部类型' },
  { value: 'PROJECT', label: '项目' },
  { value: 'TASK', label: '任务' },
  { value: 'TEMPLATE', label: '模板' },
  { value: 'CORRECTIVE_CASE', label: '整改案例' },
  { value: 'SCORING_PROFILE', label: '评分配置' },
  { value: 'NOTIFICATION_RULE', label: '通知规则' },
  { value: 'WEBHOOK', label: 'Webhook' },
  { value: 'REPORT_TEMPLATE', label: '报表模板' },
]

const actionLabels: Record<string, string> = {
  CREATE: '创建',
  UPDATE: '更新',
  DELETE: '删除',
  PUBLISH: '发布',
  ARCHIVE: '归档',
  ENABLE: '启用',
  DISABLE: '停用',
  SUBMIT: '提交',
  APPROVE: '通过',
  REJECT: '驳回',
  EXECUTE: '执行',
}

async function loadData() {
  loading.value = true
  try {
    const params: Record<string, any> = {}
    if (filters.resourceType) params.entityType = filters.resourceType
    if (filters.userId) params.userId = filters.userId
    if (filters.dateRange) {
      params.startDate = filters.dateRange[0]
      params.endDate = filters.dateRange[1]
    }
    await store.fetchAuditEntries(params)
    entries.value = store.auditEntries
  } catch (e: any) {
    ElMessage.error(e.message || '加载审计日志失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  loadData()
}

function resetFilters() {
  filters.resourceType = ''
  filters.userId = undefined
  filters.dateRange = null
  loadData()
}

function getActionLabel(action: string): string {
  return actionLabels[action] ?? action
}

function getActionTagType(action: string): string {
  switch (action) {
    case 'CREATE': return 'success'
    case 'DELETE': return 'danger'
    case 'UPDATE': return 'warning'
    case 'APPROVE': return 'success'
    case 'REJECT': return 'danger'
    default: return 'info'
  }
}

function formatDetails(details?: string): string {
  if (!details) return '-'
  try {
    const obj = JSON.parse(details)
    return JSON.stringify(obj, null, 2)
  } catch {
    return details
  }
}

onMounted(() => loadData())
</script>

<template>
  <div class="p-5 space-y-4">
    <h2 class="text-lg font-semibold">审计日志</h2>

    <!-- Filters -->
    <div class="flex items-center gap-3 flex-wrap">
      <el-select v-model="filters.resourceType" placeholder="资源类型" clearable class="w-40">
        <el-option v-for="opt in resourceTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-date-picker
        v-model="filters.dateRange"
        type="daterange"
        value-format="YYYY-MM-DD"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        class="w-64"
      />
      <el-button type="primary" @click="handleSearch">
        <Search class="w-4 h-4 mr-1" />搜索
      </el-button>
      <el-button @click="resetFilters">重置</el-button>
    </div>

    <!-- Results Table -->
    <el-table :data="entries" v-loading="loading" stripe>
      <el-table-column label="时间" width="170">
        <template #default="{ row }">
          {{ row.occurredAt?.slice(0, 19).replace('T', ' ') }}
        </template>
      </el-table-column>
      <el-table-column prop="userName" label="用户" width="120" />
      <el-table-column label="操作" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="getActionTagType(row.action) as any" size="small">
            {{ getActionLabel(row.action) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="resourceType" label="资源类型" width="120" />
      <el-table-column prop="resourceName" label="资源名称" min-width="150" show-overflow-tooltip />
      <el-table-column label="详情" min-width="200">
        <template #default="{ row }">
          <el-popover v-if="row.details" trigger="hover" placement="left" width="400">
            <template #reference>
              <span class="text-xs text-blue-500 cursor-pointer hover:underline">查看详情</span>
            </template>
            <pre class="text-xs whitespace-pre-wrap max-h-60 overflow-auto">{{ formatDetails(row.details) }}</pre>
          </el-popover>
          <span v-else class="text-gray-400 text-xs">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="ipAddress" label="IP 地址" width="130" />
    </el-table>

    <div v-if="!loading && entries.length === 0" class="text-center py-8 text-gray-400 text-sm">
      暂无审计记录
    </div>
  </div>
</template>
