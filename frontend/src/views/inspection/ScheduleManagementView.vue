<template>
  <div class="schedule-management">
    <!-- Tabs -->
    <el-tabs v-model="activeTab" type="border-card">
      <!-- Tab 1: 策略列表 -->
      <el-tab-pane label="排班策略" name="policies">
        <div class="tab-toolbar">
          <el-button type="primary" @click="showCreateDialog = true">
            <el-icon><Plus /></el-icon> 新建策略
          </el-button>
        </div>
        <el-table :data="policies" stripe v-loading="loadingPolicies">
          <el-table-column prop="policyCode" label="策略编码" width="180" />
          <el-table-column prop="policyName" label="策略名称" min-width="150" />
          <el-table-column prop="policyType" label="策略类型" width="100">
            <template #default="{ row }">
              <el-tag :type="PolicyTypeConfig[row.policyType as PolicyType]?.type || 'info'" size="small">
                {{ PolicyTypeConfig[row.policyType as PolicyType]?.label || row.policyType }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="rotationAlgorithm" label="轮询算法" width="120">
            <template #default="{ row }">
              {{ RotationAlgorithmConfig[row.rotationAlgorithm as RotationAlgorithm]?.label || row.rotationAlgorithm }}
            </template>
          </el-table-column>
          <el-table-column prop="enabled" label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
                {{ row.enabled ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="inspectorPool" label="检查员数" width="100">
            <template #default="{ row }">{{ row.inspectorPool?.length || 0 }}人</template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="160">
            <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="240" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="showDetail(row)">详情</el-button>
              <el-button link :type="row.enabled ? 'warning' : 'success'" @click="toggleEnabled(row)">
                {{ row.enabled ? '禁用' : '启用' }}
              </el-button>
              <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- Tab 2: 执行历史 -->
      <el-tab-pane label="执行历史" name="executions">
        <div class="tab-toolbar">
          <el-date-picker
            v-model="executionDateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            @change="loadExecutions"
          />
          <el-button type="primary" style="margin-left: 12px" @click="showTriggerDialog = true">
            手动触发
          </el-button>
        </div>
        <el-table :data="executions" stripe v-loading="loadingExecutions">
          <el-table-column prop="policyId" label="策略ID" width="120" />
          <el-table-column prop="executionDate" label="执行日期" width="120" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="ExecutionStatusConfig[row.status as ExecutionStatus]?.type || 'info'" size="small">
                {{ ExecutionStatusConfig[row.status as ExecutionStatus]?.label || row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="assignedInspectors" label="检查员" min-width="150">
            <template #default="{ row }">
              {{ row.assignedInspectors?.join(', ') || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="sessionId" label="会话ID" width="120">
            <template #default="{ row }">{{ row.sessionId || '-' }}</template>
          </el-table-column>
          <el-table-column prop="failureReason" label="失败原因" min-width="200" show-overflow-tooltip />
          <el-table-column prop="createdAt" label="创建时间" width="160">
            <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- Create Policy Dialog -->
    <el-dialog v-model="showCreateDialog" title="新建排班策略" width="600px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="策略名称" required>
          <el-input v-model="createForm.policyName" placeholder="请输入策略名称" />
        </el-form-item>
        <el-form-item label="策略类型" required>
          <el-select v-model="createForm.policyType" placeholder="请选择">
            <el-option v-for="(cfg, key) in PolicyTypeConfig" :key="key" :label="cfg.label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="轮询算法" required>
          <el-select v-model="createForm.rotationAlgorithm" placeholder="请选择">
            <el-option v-for="(cfg, key) in RotationAlgorithmConfig" :key="key" :label="cfg.label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="检查员ID" required>
          <el-input v-model="inspectorPoolInput" placeholder="输入检查员ID,逗号分隔" />
        </el-form-item>
        <el-form-item label="排班配置">
          <el-input v-model="createForm.scheduleConfig" type="textarea" :rows="2" placeholder="JSON配置" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="submitting">创建</el-button>
      </template>
    </el-dialog>

    <!-- Detail Dialog -->
    <el-dialog v-model="detailVisible" title="策略详情" width="600px">
      <template v-if="currentPolicy">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="策略编码">{{ currentPolicy.policyCode }}</el-descriptions-item>
          <el-descriptions-item label="策略名称">{{ currentPolicy.policyName }}</el-descriptions-item>
          <el-descriptions-item label="策略类型">
            <el-tag :type="PolicyTypeConfig[currentPolicy.policyType as PolicyType]?.type" size="small">
              {{ PolicyTypeConfig[currentPolicy.policyType as PolicyType]?.label }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="轮询算法">
            {{ RotationAlgorithmConfig[currentPolicy.rotationAlgorithm as RotationAlgorithm]?.label }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="currentPolicy.enabled ? 'success' : 'info'" size="small">
              {{ currentPolicy.enabled ? '启用' : '禁用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="检查员数">{{ currentPolicy.inspectorPool?.length || 0 }}人</el-descriptions-item>
          <el-descriptions-item label="检查员ID" :span="2">{{ currentPolicy.inspectorPool?.join(', ') || '-' }}</el-descriptions-item>
          <el-descriptions-item label="排班配置" :span="2">{{ currentPolicy.scheduleConfig || '-' }}</el-descriptions-item>
          <el-descriptions-item label="排除日期" :span="2">{{ currentPolicy.excludedDates?.join(', ') || '-' }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>

    <!-- Trigger Dialog -->
    <el-dialog v-model="showTriggerDialog" title="手动触发排班" width="400px">
      <el-form label-width="100px">
        <el-form-item label="选择策略" required>
          <el-select v-model="triggerPolicyId" placeholder="请选择策略">
            <el-option v-for="p in policies" :key="p.id" :label="p.policyName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="执行日期" required>
          <el-date-picker v-model="triggerDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showTriggerDialog = false">取消</el-button>
        <el-button type="primary" @click="handleTrigger" :loading="submitting">触发</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { listPolicies, createPolicy, enablePolicy, disablePolicy, deletePolicy, listExecutions, triggerExecution } from '@/api/schedule'
import type { SchedulePolicy, ScheduleExecution, CreatePolicyRequest, PolicyType, RotationAlgorithm, ExecutionStatus } from '@/types/schedule'
import { PolicyTypeConfig, RotationAlgorithmConfig, ExecutionStatusConfig } from '@/types/schedule'

const activeTab = ref('policies')
const loadingPolicies = ref(false)
const loadingExecutions = ref(false)
const submitting = ref(false)
const policies = ref<SchedulePolicy[]>([])
const executions = ref<ScheduleExecution[]>([])
const detailVisible = ref(false)
const currentPolicy = ref<SchedulePolicy | null>(null)
const showCreateDialog = ref(false)
const showTriggerDialog = ref(false)
const inspectorPoolInput = ref('')
const triggerPolicyId = ref<number>()
const triggerDate = ref('')

const today = new Date()
const thirtyDaysAgo = new Date(today)
thirtyDaysAgo.setDate(today.getDate() - 30)
const executionDateRange = ref<string[]>([
  thirtyDaysAgo.toISOString().slice(0, 10),
  today.toISOString().slice(0, 10)
])

const createForm = ref<CreatePolicyRequest>({
  policyName: '',
  policyType: 'DAILY',
  rotationAlgorithm: 'ROUND_ROBIN',
  inspectorPool: [],
  scheduleConfig: ''
})

function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

async function loadPolicies() {
  loadingPolicies.value = true
  try {
    policies.value = await listPolicies()
  } catch (e: any) {
    ElMessage.error(e.message || '加载策略失败')
  } finally {
    loadingPolicies.value = false
  }
}

async function loadExecutions() {
  if (!executionDateRange.value || executionDateRange.value.length !== 2) return
  loadingExecutions.value = true
  try {
    executions.value = await listExecutions({
      startDate: executionDateRange.value[0],
      endDate: executionDateRange.value[1]
    })
  } catch (e: any) {
    ElMessage.error(e.message || '加载执行记录失败')
  } finally {
    loadingExecutions.value = false
  }
}

function showDetail(policy: SchedulePolicy) {
  currentPolicy.value = policy
  detailVisible.value = true
}

async function handleCreate() {
  if (!createForm.value.policyName.trim()) {
    ElMessage.warning('请输入策略名称')
    return
  }
  const ids = inspectorPoolInput.value.split(',').map(s => parseInt(s.trim())).filter(n => !isNaN(n))
  if (ids.length === 0) {
    ElMessage.warning('请输入至少一个检查员ID')
    return
  }
  createForm.value.inspectorPool = ids
  submitting.value = true
  try {
    await createPolicy(createForm.value)
    ElMessage.success('创建成功')
    showCreateDialog.value = false
    createForm.value = { policyName: '', policyType: 'DAILY', rotationAlgorithm: 'ROUND_ROBIN', inspectorPool: [] }
    inspectorPoolInput.value = ''
    await loadPolicies()
  } catch (e: any) {
    ElMessage.error(e.message || '创建失败')
  } finally {
    submitting.value = false
  }
}

async function toggleEnabled(policy: SchedulePolicy) {
  try {
    if (policy.enabled) {
      await disablePolicy(policy.id)
      ElMessage.success('已禁用')
    } else {
      await enablePolicy(policy.id)
      ElMessage.success('已启用')
    }
    await loadPolicies()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function handleDelete(policy: SchedulePolicy) {
  try {
    await ElMessageBox.confirm(`确定删除策略 "${policy.policyName}" 吗?`, '确认删除', { type: 'warning' })
    await deletePolicy(policy.id)
    ElMessage.success('删除成功')
    await loadPolicies()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '删除失败')
  }
}

async function handleTrigger() {
  if (!triggerPolicyId.value || !triggerDate.value) {
    ElMessage.warning('请选择策略和日期')
    return
  }
  submitting.value = true
  try {
    await triggerExecution(triggerPolicyId.value, triggerDate.value)
    ElMessage.success('触发成功')
    showTriggerDialog.value = false
    await loadExecutions()
  } catch (e: any) {
    ElMessage.error(e.message || '触发失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadPolicies()
  loadExecutions()
})
</script>

<style scoped>
.schedule-management {
  padding: 0;
}
.tab-toolbar {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
}
</style>
