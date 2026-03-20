<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Pencil, Trash2 } from 'lucide-vue-next'
import { useInspPlatformStore } from '@/stores/insp/inspPlatformStore'
import type { NotificationRule } from '@/types/insp/platform'

const store = useInspPlatformStore()

const loading = ref(false)
const rules = ref<NotificationRule[]>([])
const showDialog = ref(false)
const editingId = ref<number | null>(null)

const form = ref({
  ruleName: '',
  eventType: '',
  channels: '["IN_APP"]',
  recipientType: 'ROLE',
  priority: 0,
})

const eventTypeOptions = [
  { value: 'TASK_CREATED', label: '任务创建' },
  { value: 'TASK_SUBMITTED', label: '任务提交' },
  { value: 'TASK_REVIEWED', label: '任务审核' },
  { value: 'CORRECTIVE_CREATED', label: '整改创建' },
  { value: 'CORRECTIVE_OVERDUE', label: '整改逾期' },
  { value: 'SCORE_PUBLISHED', label: '成绩发布' },
]

const channelOptions = [
  { value: 'IN_APP', label: '站内信' },
  { value: 'EMAIL', label: '邮件' },
  { value: 'SMS', label: '短信' },
  { value: 'WECHAT', label: '微信' },
]

async function loadData() {
  loading.value = true
  try {
    await store.fetchNotificationRules()
    rules.value = store.notificationRules
  } catch (e: any) {
    ElMessage.error(e.message || '加载通知规则失败')
  } finally {
    loading.value = false
  }
}

function parseChannels(channels: string): string[] {
  try { return JSON.parse(channels) } catch { return [] }
}

function getChannelLabels(channels: string): string {
  const arr = parseChannels(channels)
  return arr.map(ch => channelOptions.find(o => o.value === ch)?.label ?? ch).join(', ')
}

function getEventLabel(eventType: string): string {
  return eventTypeOptions.find(o => o.value === eventType)?.label ?? eventType
}

function openCreate() {
  editingId.value = null
  form.value = { ruleName: '', eventType: '', channels: '["IN_APP"]', recipientType: 'ROLE', priority: 0 }
  showDialog.value = true
}

function openEdit(rule: NotificationRule) {
  editingId.value = rule.id
  form.value = {
    ruleName: rule.ruleName,
    eventType: rule.eventType,
    channels: rule.channels,
    recipientType: rule.recipientType,
    priority: rule.priority,
  }
  showDialog.value = true
}

async function handleSave() {
  try {
    if (editingId.value) {
      await store.updateNotificationRule(editingId.value, { ...form.value })
      ElMessage.success('更新成功')
    } else {
      await store.createNotificationRule({ ...form.value, isEnabled: true })
      ElMessage.success('创建成功')
    }
    showDialog.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  }
}

async function handleDelete(rule: NotificationRule) {
  try {
    await ElMessageBox.confirm(`确认删除通知规则「${rule.ruleName}」？`, '确认删除', { type: 'warning' })
    await store.deleteNotificationRule(rule.id)
    ElMessage.success('删除成功')
    loadData()
  } catch { /* cancelled */ }
}

async function handleToggle(rule: NotificationRule) {
  try {
    if (rule.isEnabled) {
      await store.disableNotificationRule(rule.id)
      ElMessage.success('已停用')
    } else {
      await store.enableNotificationRule(rule.id)
      ElMessage.success('已启用')
    }
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

onMounted(() => loadData())
</script>

<template>
  <div class="p-5 space-y-4">
    <div class="flex items-center justify-between">
      <h2 class="text-lg font-semibold">通知规则</h2>
      <el-button type="primary" @click="openCreate">
        <Plus class="w-4 h-4 mr-1" />新建规则
      </el-button>
    </div>

    <el-table :data="rules" v-loading="loading" stripe>
      <el-table-column prop="ruleName" label="规则名称" min-width="150" />
      <el-table-column label="事件类型" width="120">
        <template #default="{ row }">
          {{ getEventLabel(row.eventType) }}
        </template>
      </el-table-column>
      <el-table-column label="通知渠道" width="180">
        <template #default="{ row }">
          {{ getChannelLabels(row.channels) }}
        </template>
      </el-table-column>
      <el-table-column prop="recipientType" label="接收人类型" width="110" />
      <el-table-column prop="priority" label="优先级" width="80" align="center" />
      <el-table-column label="启用" width="80" align="center">
        <template #default="{ row }">
          <el-switch :model-value="row.isEnabled" @change="handleToggle(row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <div class="flex items-center gap-1">
            <el-button link type="primary" size="small" @click="openEdit(row)">
              <Pencil class="w-3.5 h-3.5" />
            </el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">
              <Trash2 class="w-3.5 h-3.5" />
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- Create/Edit Dialog -->
    <el-dialog v-model="showDialog" :title="editingId ? '编辑通知规则' : '新建通知规则'" width="500px">
      <el-form label-width="100px">
        <el-form-item label="规则名称" required>
          <el-input v-model="form.ruleName" placeholder="输入规则名称" />
        </el-form-item>
        <el-form-item label="事件类型" required>
          <el-select v-model="form.eventType" placeholder="选择事件类型" class="w-full">
            <el-option v-for="opt in eventTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="通知渠道" required>
          <el-select v-model="form.channels" placeholder="选择渠道" class="w-full">
            <el-option v-for="opt in channelOptions" :key="opt.value" :label="opt.label" :value="JSON.stringify([opt.value])" />
          </el-select>
        </el-form-item>
        <el-form-item label="接收人类型">
          <el-select v-model="form.recipientType" class="w-full">
            <el-option label="角色" value="ROLE" />
            <el-option label="用户" value="USER" />
            <el-option label="动态" value="DYNAMIC" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="form.priority" :min="0" :max="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
