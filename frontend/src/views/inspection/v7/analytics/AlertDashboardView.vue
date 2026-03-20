<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Pencil, Trash2 } from 'lucide-vue-next'
import {
  getAlertRules,
  createAlertRule,
  updateAlertRule,
  deleteAlertRule,
  toggleAlertRule,
  getAlerts,
  acknowledgeAlert,
  resolveAlert,
  dismissAlert,
} from '@/api/insp/alert'
import type {
  AlertRule,
  CreateAlertRuleRequest,
  Alert,
  AlertStatus,
  AlertSeverity,
  MetricType,
} from '@/types/insp/alert'

// ==================== State ====================

const loading = ref(false)
const rules = ref<AlertRule[]>([])
const alerts = ref<Alert[]>([])
const activeTab = ref<'alerts' | 'rules'>('alerts')
const statusFilter = ref<AlertStatus | ''>('')

// Dialog
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const dialogTitle = computed(() => (editingId.value ? '编辑预警规则' : '新建预警规则'))

const form = ref<{
  ruleName: string
  metricType: MetricType
  thresholdConfig: string
  severity: AlertSeverity
  notificationChannels: string
  projectId: number | null
}>({
  ruleName: '',
  metricType: 'SCORE_DROP',
  thresholdConfig: '{}',
  severity: 'WARNING',
  notificationChannels: '',
  projectId: null,
})

// ==================== Metric / Severity Config ====================

const metricTypeOptions: { value: MetricType; label: string }[] = [
  { value: 'SCORE_DROP', label: '分数骤降' },
  { value: 'CONSECUTIVE_FAIL', label: '连续不合格' },
  { value: 'HIGH_DEVIATION', label: '高偏差' },
  { value: 'LOW_COMPLIANCE', label: '低合规率' },
  { value: 'OVERDUE_CORRECTION', label: '整改逾期' },
]

const severityOptions: { value: AlertSeverity; label: string }[] = [
  { value: 'INFO', label: '信息' },
  { value: 'WARNING', label: '警告' },
  { value: 'CRITICAL', label: '严重' },
]

const statusOptions: { value: AlertStatus; label: string }[] = [
  { value: 'OPEN', label: '待处理' },
  { value: 'ACKNOWLEDGED', label: '已确认' },
  { value: 'RESOLVED', label: '已解决' },
  { value: 'DISMISSED', label: '已忽略' },
]

function severityTagType(severity: AlertSeverity): string {
  switch (severity) {
    case 'INFO': return ''
    case 'WARNING': return 'warning'
    case 'CRITICAL': return 'danger'
  }
}

function statusTagType(status: AlertStatus): string {
  switch (status) {
    case 'OPEN': return 'danger'
    case 'ACKNOWLEDGED': return 'warning'
    case 'RESOLVED': return 'success'
    case 'DISMISSED': return 'info'
  }
}

function statusLabel(status: AlertStatus): string {
  return statusOptions.find(o => o.value === status)?.label ?? status
}

// ==================== Data Loading ====================

const filteredAlerts = computed(() => {
  if (!statusFilter.value) return alerts.value
  return alerts.value.filter(a => a.status === statusFilter.value)
})

async function loadRules() {
  try {
    rules.value = await getAlertRules()
  } catch (e: any) {
    ElMessage.error(e.message || '加载预警规则失败')
  }
}

async function loadAlerts() {
  try {
    alerts.value = await getAlerts()
  } catch (e: any) {
    ElMessage.error(e.message || '加载预警列表失败')
  }
}

async function loadData() {
  loading.value = true
  try {
    await Promise.all([loadRules(), loadAlerts()])
  } finally {
    loading.value = false
  }
}

// ==================== Rule CRUD ====================

function openCreate() {
  editingId.value = null
  form.value = {
    ruleName: '',
    metricType: 'SCORE_DROP',
    thresholdConfig: '{}',
    severity: 'WARNING',
    notificationChannels: '',
    projectId: null,
  }
  dialogVisible.value = true
}

function openEdit(rule: AlertRule) {
  editingId.value = rule.id
  form.value = {
    ruleName: rule.ruleName,
    metricType: rule.metricType,
    thresholdConfig: rule.thresholdConfig,
    severity: rule.severity,
    notificationChannels: rule.notificationChannels ?? '',
    projectId: rule.projectId,
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.value.ruleName.trim()) {
    ElMessage.warning('请输入规则名称')
    return
  }
  try {
    if (editingId.value) {
      await updateAlertRule(editingId.value, { ...form.value })
      ElMessage.success('更新成功')
    } else {
      const req: CreateAlertRuleRequest = {
        ruleName: form.value.ruleName,
        metricType: form.value.metricType,
        thresholdConfig: form.value.thresholdConfig,
        severity: form.value.severity,
        notificationChannels: form.value.notificationChannels || undefined,
        projectId: form.value.projectId ?? undefined,
      }
      await createAlertRule(req)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await loadRules()
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  }
}

async function handleDeleteRule(rule: AlertRule) {
  try {
    await ElMessageBox.confirm(`确认删除规则「${rule.ruleName}」？`, '确认删除', { type: 'warning' })
    await deleteAlertRule(rule.id)
    ElMessage.success('删除成功')
    await loadRules()
  } catch { /* cancelled */ }
}

async function handleToggleRule(rule: AlertRule) {
  try {
    await toggleAlertRule(rule.id, !rule.isEnabled)
    ElMessage.success(rule.isEnabled ? '已停用' : '已启用')
    await loadRules()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

// ==================== Alert Actions ====================

async function handleAcknowledge(alert: Alert) {
  try {
    await acknowledgeAlert(alert.id)
    ElMessage.success('已确认')
    await loadAlerts()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function handleResolve(alert: Alert) {
  try {
    await resolveAlert(alert.id)
    ElMessage.success('已解决')
    await loadAlerts()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function handleDismiss(alert: Alert) {
  try {
    await dismissAlert(alert.id)
    ElMessage.success('已忽略')
    await loadAlerts()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

// ==================== Lifecycle ====================

onMounted(() => loadData())
</script>

<template>
  <div class="p-5 space-y-4">
    <div class="flex items-center justify-between">
      <h2 class="text-lg font-semibold">预警中心</h2>
    </div>

    <!-- Tab Switch -->
    <el-radio-group v-model="activeTab" class="mb-2">
      <el-radio-button value="alerts">预警列表</el-radio-button>
      <el-radio-button value="rules">预警规则</el-radio-button>
    </el-radio-group>

    <!-- ==================== Alerts Tab ==================== -->
    <template v-if="activeTab === 'alerts'">
      <!-- Filter -->
      <el-card shadow="never" class="mb-4">
        <div class="flex items-center gap-4">
          <span class="text-sm text-gray-500">状态筛选</span>
          <el-select v-model="statusFilter" clearable placeholder="全部" class="w-36">
            <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <span class="text-sm text-gray-400 ml-auto">共 {{ filteredAlerts.length }} 条</span>
        </div>
      </el-card>

      <el-card shadow="never">
        <el-table :data="filteredAlerts" v-loading="loading" stripe>
          <el-table-column label="严重级别" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="severityTagType(row.severity)" size="small">
                {{ row.severity }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="message" label="预警消息" min-width="250" show-overflow-tooltip />
          <el-table-column prop="targetName" label="目标" width="140" show-overflow-tooltip />
          <el-table-column label="指标值" width="90" align="right">
            <template #default="{ row }">
              {{ row.metricValue != null ? row.metricValue : '-' }}
            </template>
          </el-table-column>
          <el-table-column label="阈值" width="90" align="right">
            <template #default="{ row }">
              {{ row.thresholdValue != null ? row.thresholdValue : '-' }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">
                {{ statusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="触发时间" width="160">
            <template #default="{ row }">
              <span class="text-xs text-gray-500">{{ row.triggeredAt }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <div class="flex items-center gap-1">
                <el-button
                  v-if="row.status === 'OPEN'"
                  link type="primary" size="small"
                  @click="handleAcknowledge(row)"
                >确认</el-button>
                <el-button
                  v-if="row.status === 'OPEN' || row.status === 'ACKNOWLEDGED'"
                  link type="success" size="small"
                  @click="handleResolve(row)"
                >解决</el-button>
                <el-button
                  v-if="row.status === 'OPEN' || row.status === 'ACKNOWLEDGED'"
                  link type="info" size="small"
                  @click="handleDismiss(row)"
                >忽略</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>

    <!-- ==================== Rules Tab ==================== -->
    <template v-if="activeTab === 'rules'">
      <div class="flex justify-end mb-2">
        <el-button type="primary" @click="openCreate">
          <Plus class="w-4 h-4 mr-1" />新建规则
        </el-button>
      </div>

      <el-card shadow="never">
        <el-table :data="rules" v-loading="loading" stripe>
          <el-table-column prop="ruleName" label="规则名称" min-width="150" />
          <el-table-column label="指标类型" width="120">
            <template #default="{ row }">
              {{ metricTypeOptions.find(o => o.value === row.metricType)?.label ?? row.metricType }}
            </template>
          </el-table-column>
          <el-table-column label="严重级别" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="severityTagType(row.severity)" size="small">{{ row.severity }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="阈值配置" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="text-xs text-gray-500 font-mono">{{ row.thresholdConfig }}</span>
            </template>
          </el-table-column>
          <el-table-column label="启用" width="80" align="center">
            <template #default="{ row }">
              <el-switch :model-value="row.isEnabled" @change="handleToggleRule(row)" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <div class="flex items-center gap-1">
                <el-button link type="primary" size="small" @click="openEdit(row)">
                  <Pencil class="w-3.5 h-3.5" />
                </el-button>
                <el-button link type="danger" size="small" @click="handleDeleteRule(row)">
                  <Trash2 class="w-3.5 h-3.5" />
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>

    <!-- ==================== Rule Dialog ==================== -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="550px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="规则名称" required>
          <el-input v-model="form.ruleName" placeholder="输入规则名称" />
        </el-form-item>
        <el-form-item label="指标类型" required>
          <el-select v-model="form.metricType" class="w-full">
            <el-option
              v-for="opt in metricTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="阈值配置" required>
          <el-input
            v-model="form.thresholdConfig"
            type="textarea"
            :rows="3"
            placeholder='例如: {"threshold": 70, "consecutiveDays": 3}'
          />
          <div class="text-xs text-gray-400 mt-1">JSON 格式的阈值参数</div>
        </el-form-item>
        <el-form-item label="严重级别" required>
          <el-select v-model="form.severity" class="w-full">
            <el-option
              v-for="opt in severityOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="通知渠道">
          <el-input
            v-model="form.notificationChannels"
            placeholder='例如: ["IN_APP", "EMAIL"]'
          />
          <div class="text-xs text-gray-400 mt-1">JSON 数组格式，可选</div>
        </el-form-item>
        <el-form-item label="关联项目ID">
          <el-input-number v-model="form.projectId" :min="0" controls-position="right" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
