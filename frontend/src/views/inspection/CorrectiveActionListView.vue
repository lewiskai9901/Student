<template>
  <div class="corrective-action-list">
    <!-- Statistics Cards -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="4" v-for="(item, key) in statsItems" :key="key">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ statistics[key] || 0 }}</div>
          <div class="stat-label">{{ item.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Filters -->
    <el-card shadow="never" class="filter-card">
      <el-row :gutter="16">
        <el-col :span="6">
          <el-select v-model="filterStatus" placeholder="状态筛选" clearable @change="loadData">
            <el-option v-for="(cfg, key) in ActionStatusConfig" :key="key" :label="cfg.label" :value="key" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-select v-model="filterSeverity" placeholder="严重程度" clearable @change="loadData">
            <el-option v-for="(cfg, key) in ActionSeverityConfig" :key="key" :label="cfg.label" :value="key" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-button type="primary" @click="showCreateDialog = true">
            <el-icon><Plus /></el-icon> 新建工单
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- Table -->
    <el-card shadow="never">
      <el-table :data="filteredActions" stripe v-loading="loading">
        <el-table-column prop="actionCode" label="工单编号" width="180" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="severity" label="严重程度" width="100">
          <template #default="{ row }">
            <el-tag :type="ActionSeverityConfig[row.severity as ActionSeverity]?.type || 'info'" size="small">
              {{ ActionSeverityConfig[row.severity as ActionSeverity]?.label || row.severity }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="80">
          <template #default="{ row }">
            {{ ActionCategoryConfig[row.category as ActionCategory]?.label || row.category }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="ActionStatusConfig[row.status as ActionStatus]?.type || 'info'" size="small">
              {{ ActionStatusConfig[row.status as ActionStatus]?.label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="deadline" label="截止时间" width="160">
          <template #default="{ row }">
            <span :class="{ 'text-danger': isOverdue(row.deadline) }">
              {{ row.deadline ? formatDate(row.deadline) : '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">详情</el-button>
            <el-button link type="warning" v-if="row.status === 'OPEN'" @click="handleStart(row)">开始整改</el-button>
            <el-button link type="success" v-if="row.status === 'IN_PROGRESS'" @click="showResolveDialog(row)">提交整改</el-button>
            <el-button link type="primary" v-if="row.status === 'REVIEW'" @click="showVerifyDialog(row)">验证</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Detail Dialog -->
    <el-dialog v-model="detailVisible" title="工单详情" width="600px">
      <template v-if="currentAction">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="工单编号">{{ currentAction.actionCode }}</el-descriptions-item>
          <el-descriptions-item label="标题">{{ currentAction.title }}</el-descriptions-item>
          <el-descriptions-item label="来源">{{ ActionSourceConfig[currentAction.source as ActionSource]?.label }}</el-descriptions-item>
          <el-descriptions-item label="严重程度">
            <el-tag :type="ActionSeverityConfig[currentAction.severity as ActionSeverity]?.type" size="small">
              {{ ActionSeverityConfig[currentAction.severity as ActionSeverity]?.label }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="ActionStatusConfig[currentAction.status as ActionStatus]?.type" size="small">
              {{ ActionStatusConfig[currentAction.status as ActionStatus]?.label }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="截止时间">{{ currentAction.deadline ? formatDate(currentAction.deadline) : '-' }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ currentAction.description || '-' }}</el-descriptions-item>
          <el-descriptions-item label="整改说明" :span="2" v-if="currentAction.resolutionNote">{{ currentAction.resolutionNote }}</el-descriptions-item>
          <el-descriptions-item label="验证结果" v-if="currentAction.verificationResult">{{ currentAction.verificationResult }}</el-descriptions-item>
          <el-descriptions-item label="验证备注" v-if="currentAction.verificationComment">{{ currentAction.verificationComment }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>

    <!-- Create Dialog -->
    <el-dialog v-model="showCreateDialog" title="新建整改工单" width="500px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="标题" required>
          <el-input v-model="createForm.title" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="严重程度" required>
          <el-select v-model="createForm.severity">
            <el-option v-for="(cfg, key) in ActionSeverityConfig" :key="key" :label="cfg.label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类" required>
          <el-select v-model="createForm.category">
            <el-option v-for="(cfg, key) in ActionCategoryConfig" :key="key" :label="cfg.label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止时间">
          <el-date-picker v-model="createForm.deadline" type="datetime" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="submitting">创建</el-button>
      </template>
    </el-dialog>

    <!-- Resolve Dialog -->
    <el-dialog v-model="resolveVisible" title="提交整改" width="500px">
      <el-form label-width="100px">
        <el-form-item label="整改说明" required>
          <el-input v-model="resolveNote" type="textarea" :rows="4" placeholder="请描述整改内容..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resolveVisible = false">取消</el-button>
        <el-button type="primary" @click="handleResolve" :loading="submitting">提交</el-button>
      </template>
    </el-dialog>

    <!-- Verify Dialog -->
    <el-dialog v-model="verifyVisible" title="验证整改" width="500px">
      <el-form label-width="100px">
        <el-form-item label="验证结果" required>
          <el-radio-group v-model="verifyResult">
            <el-radio value="PASS">通过</el-radio>
            <el-radio value="FAIL">不通过</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="验证备注">
          <el-input v-model="verifyComment" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="verifyVisible = false">取消</el-button>
        <el-button type="primary" @click="handleVerify" :loading="submitting">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { listActions, createAction, startAction, resolveAction, verifyAction, getActionStatistics } from '@/api/correctiveAction'
import type { CorrectiveAction, CreateActionRequest, ActionStatus, ActionSeverity, ActionCategory, ActionSource } from '@/types/corrective'
import { ActionStatusConfig, ActionSeverityConfig, ActionCategoryConfig, ActionSourceConfig } from '@/types/corrective'

const loading = ref(false)
const submitting = ref(false)
const actions = ref<CorrectiveAction[]>([])
const statistics = ref<Record<string, number>>({})
const filterStatus = ref('')
const filterSeverity = ref('')

const detailVisible = ref(false)
const currentAction = ref<CorrectiveAction | null>(null)
const showCreateDialog = ref(false)
const resolveVisible = ref(false)
const verifyVisible = ref(false)
const resolveNote = ref('')
const verifyResult = ref('PASS')
const verifyComment = ref('')
const currentId = ref<number>(0)

const createForm = ref<CreateActionRequest>({
  title: '',
  description: '',
  source: 'MANUAL',
  severity: 'MINOR',
  category: 'OTHER'
})

const statsItems: Record<string, { label: string }> = {
  OPEN: { label: '待处理' },
  IN_PROGRESS: { label: '整改中' },
  REVIEW: { label: '待验证' },
  CLOSED: { label: '已关闭' },
  OVERDUE: { label: '已超期' },
  ESCALATED: { label: '已升级' }
}

const filteredActions = computed(() => {
  let result = actions.value
  if (filterSeverity.value) {
    result = result.filter(a => a.severity === filterSeverity.value)
  }
  return result
})

function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

function isOverdue(deadline: string | null): boolean {
  if (!deadline) return false
  return new Date(deadline) < new Date()
}

async function loadData() {
  loading.value = true
  try {
    const params: Record<string, any> = {}
    if (filterStatus.value) params.status = filterStatus.value
    actions.value = await listActions(params)
    statistics.value = await getActionStatistics()
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function showDetail(action: CorrectiveAction) {
  currentAction.value = action
  detailVisible.value = true
}

async function handleCreate() {
  submitting.value = true
  try {
    await createAction(createForm.value)
    ElMessage.success('创建成功')
    showCreateDialog.value = false
    createForm.value = { title: '', description: '', source: 'MANUAL', severity: 'MINOR', category: 'OTHER' }
    await loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '创建失败')
  } finally {
    submitting.value = false
  }
}

async function handleStart(row: CorrectiveAction) {
  try {
    await startAction(row.id)
    ElMessage.success('已开始整改')
    await loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

function showResolveDialog(row: CorrectiveAction) {
  currentId.value = row.id
  resolveNote.value = ''
  resolveVisible.value = true
}

async function handleResolve() {
  if (!resolveNote.value.trim()) {
    ElMessage.warning('请填写整改说明')
    return
  }
  submitting.value = true
  try {
    await resolveAction(currentId.value, { resolutionNote: resolveNote.value })
    ElMessage.success('整改已提交')
    resolveVisible.value = false
    await loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

function showVerifyDialog(row: CorrectiveAction) {
  currentId.value = row.id
  verifyResult.value = 'PASS'
  verifyComment.value = ''
  verifyVisible.value = true
}

async function handleVerify() {
  submitting.value = true
  try {
    await verifyAction(currentId.value, { result: verifyResult.value, comment: verifyComment.value })
    ElMessage.success('验证完成')
    verifyVisible.value = false
    await loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '验证失败')
  } finally {
    submitting.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.corrective-action-list {
  padding: 0;
}
.stats-row {
  margin-bottom: 16px;
}
.stat-card {
  text-align: center;
}
.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}
.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}
.filter-card {
  margin-bottom: 16px;
}
.text-danger {
  color: #f56c6c;
}
</style>
