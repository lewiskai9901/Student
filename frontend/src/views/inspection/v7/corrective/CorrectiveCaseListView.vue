<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Eye, Trash2, AlertTriangle, Clock } from 'lucide-vue-next'
import { useInspCorrectiveStore } from '@/stores/insp/inspCorrectiveStore'
import { CaseStatusConfig, CasePriorityConfig, type CaseStatus, type CasePriority } from '@/types/insp/enums'
import type { CorrectiveCase } from '@/types/insp/corrective'
import InspEmptyState from '../shared/InspEmptyState.vue'

const router = useRouter()
const store = useInspCorrectiveStore()

// State
const loading = ref(false)
const cases = ref<CorrectiveCase[]>([])
const activeTab = ref<'all' | 'my' | 'overdue'>('all')

const queryParams = reactive({
  status: undefined as CaseStatus | undefined,
  projectId: undefined as number | undefined,
})

// Computed
const statusOptions = computed(() =>
  Object.entries(CaseStatusConfig).map(([key, val]) => ({
    value: key,
    label: val.label,
  }))
)

// Actions
async function loadData() {
  loading.value = true
  try {
    if (activeTab.value === 'my') {
      await store.fetchMyCases()
      cases.value = store.myCases
    } else if (activeTab.value === 'overdue') {
      await store.fetchOverdueCases()
      cases.value = store.overdueCases
    } else {
      await store.fetchCases({ status: queryParams.status, projectId: queryParams.projectId })
      cases.value = store.cases
    }
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  loadData()
}

function resetQuery() {
  queryParams.status = undefined
  queryParams.projectId = undefined
  loadData()
}

function handleTabChange() {
  loadData()
}

function goDetail(c: CorrectiveCase) {
  router.push(`/inspection/v7/corrective/${c.id}`)
}

async function handleDelete(c: CorrectiveCase) {
  try {
    await ElMessageBox.confirm(`确定删除整改案例「${c.caseCode}」？`, '确认删除', { type: 'warning' })
    await store.removeCase(c.id)
    ElMessage.success('删除成功')
    loadData()
  } catch { /* cancelled */ }
}

async function handleEscalate(c: CorrectiveCase) {
  try {
    await ElMessageBox.confirm(`确定升级案例「${c.caseCode}」？`, '确认升级', { type: 'warning' })
    await store.escalateCase(c.id)
    ElMessage.success('已升级')
    loadData()
  } catch { /* cancelled */ }
}

function getStatusConfig(status: CaseStatus) {
  return CaseStatusConfig[status] || { label: status, type: 'info', color: '#909399' }
}

function getPriorityConfig(priority: CasePriority) {
  return CasePriorityConfig[priority] || { label: priority, type: 'info', color: '#909399' }
}

function isOverdue(c: CorrectiveCase): boolean {
  if (!c.deadline) return false
  if (c.status === 'CLOSED' || c.status === 'VERIFIED') return false
  return new Date() > new Date(c.deadline)
}

onMounted(() => loadData())
</script>

<template>
  <div class="p-5">
    <!-- Header -->
    <div class="flex items-center justify-between mb-5">
      <h2 class="text-lg font-semibold">整改管理</h2>
      <el-button type="primary" @click="router.push('/inspection/v7/corrective/create')">
        <Plus class="w-4 h-4 mr-1" />新建整改
      </el-button>
    </div>

    <!-- Tabs -->
    <el-tabs v-model="activeTab" @tab-change="handleTabChange" class="mb-4">
      <el-tab-pane label="全部案例" name="all" />
      <el-tab-pane label="我的整改" name="my" />
      <el-tab-pane label="逾期案例" name="overdue" />
    </el-tabs>

    <!-- Filter (only for 'all' tab) -->
    <div v-if="activeTab === 'all'" class="flex items-center gap-3 mb-4">
      <el-select v-model="queryParams.status" placeholder="状态筛选" clearable class="w-40">
        <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-button type="primary" @click="handleSearch">
        <Search class="w-4 h-4 mr-1" />搜索
      </el-button>
      <el-button @click="resetQuery">重置</el-button>
    </div>

    <!-- Table -->
    <el-table :data="cases" v-loading="loading" stripe>
      <el-table-column prop="caseCode" label="案例编号" width="180" />
      <el-table-column label="优先级" width="80">
        <template #default="{ row }">
          <el-tag :type="getPriorityConfig(row.priority).type" size="small">
            {{ getPriorityConfig(row.priority).label }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusConfig(row.status).type" size="small">
            {{ getStatusConfig(row.status).label }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="targetName" label="检查对象" width="150" show-overflow-tooltip />
      <el-table-column prop="issueDescription" label="问题描述" min-width="200" show-overflow-tooltip />
      <el-table-column prop="assigneeName" label="责任人" width="100" />
      <el-table-column label="截止时间" width="160">
        <template #default="{ row }">
          <span :class="{ 'text-red-500 font-medium': isOverdue(row) }">
            {{ row.deadline ? new Date(row.deadline).toLocaleString() : '-' }}
          </span>
          <Clock v-if="isOverdue(row)" class="inline w-3 h-3 ml-1 text-red-500" />
        </template>
      </el-table-column>
      <el-table-column prop="escalationLevel" label="升级" width="60" align="center" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="goDetail(row)">
            <Eye class="w-4 h-4" />
          </el-button>
          <el-button v-if="row.status !== 'CLOSED' && row.status !== 'ESCALATED'"
                     link type="warning" @click="handleEscalate(row)">
            <AlertTriangle class="w-4 h-4" />
          </el-button>
          <el-button link type="danger" @click="handleDelete(row)">
            <Trash2 class="w-4 h-4" />
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <InspEmptyState v-if="!loading && cases.length === 0" message="暂无整改案例" />
  </div>
</template>
