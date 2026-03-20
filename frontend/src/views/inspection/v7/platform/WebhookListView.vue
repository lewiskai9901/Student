<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Pencil, Trash2, Zap } from 'lucide-vue-next'
import { useInspPlatformStore } from '@/stores/insp/inspPlatformStore'
import type { WebhookSubscription } from '@/types/insp/platform'

const store = useInspPlatformStore()

const loading = ref(false)
const webhooks = ref<WebhookSubscription[]>([])
const showDialog = ref(false)
const editingId = ref<number | null>(null)
const testing = ref<number | null>(null)

const form = ref({
  subscriptionName: '',
  targetUrl: '',
  secret: '',
  eventTypes: '["TASK_SUBMITTED"]',
  retryCount: 3,
})

async function loadData() {
  loading.value = true
  try {
    await store.fetchWebhooks()
    webhooks.value = store.webhooks
  } catch (e: any) {
    ElMessage.error(e.message || '加载 Webhook 列表失败')
  } finally {
    loading.value = false
  }
}

function parseEventTypes(eventTypes: string): string {
  try {
    const arr = JSON.parse(eventTypes)
    return Array.isArray(arr) ? arr.join(', ') : eventTypes
  } catch {
    return eventTypes
  }
}

function openCreate() {
  editingId.value = null
  form.value = { subscriptionName: '', targetUrl: '', secret: '', eventTypes: '["TASK_SUBMITTED"]', retryCount: 3 }
  showDialog.value = true
}

function openEdit(wh: WebhookSubscription) {
  editingId.value = wh.id
  form.value = {
    subscriptionName: wh.subscriptionName,
    targetUrl: wh.targetUrl,
    secret: wh.secret ?? '',
    eventTypes: wh.eventTypes,
    retryCount: wh.retryCount,
  }
  showDialog.value = true
}

async function handleSave() {
  try {
    if (editingId.value) {
      await store.updateWebhook(editingId.value, { ...form.value })
      ElMessage.success('更新成功')
    } else {
      await store.createWebhook({ ...form.value, isEnabled: true })
      ElMessage.success('创建成功')
    }
    showDialog.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  }
}

async function handleDelete(wh: WebhookSubscription) {
  try {
    await ElMessageBox.confirm(`确认删除 Webhook「${wh.subscriptionName}」？`, '确认删除', { type: 'warning' })
    await store.deleteWebhook(wh.id)
    ElMessage.success('删除成功')
    loadData()
  } catch { /* cancelled */ }
}

async function handleToggle(wh: WebhookSubscription) {
  try {
    if (wh.isEnabled) {
      await store.disableWebhook(wh.id)
      ElMessage.success('已停用')
    } else {
      await store.enableWebhook(wh.id)
      ElMessage.success('已启用')
    }
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function handleTest(wh: WebhookSubscription) {
  testing.value = wh.id
  try {
    await store.testWebhook(wh.id)
    ElMessage.success('测试请求已发送')
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '测试失败')
  } finally {
    testing.value = null
  }
}

onMounted(() => loadData())
</script>

<template>
  <div class="p-5 space-y-4">
    <div class="flex items-center justify-between">
      <h2 class="text-lg font-semibold">Webhook 订阅</h2>
      <el-button type="primary" @click="openCreate">
        <Plus class="w-4 h-4 mr-1" />新建 Webhook
      </el-button>
    </div>

    <el-table :data="webhooks" v-loading="loading" stripe>
      <el-table-column prop="subscriptionName" label="名称" min-width="150" />
      <el-table-column prop="targetUrl" label="目标 URL" min-width="250" show-overflow-tooltip />
      <el-table-column label="事件类型" width="200">
        <template #default="{ row }">
          <span class="text-xs">{{ parseEventTypes(row.eventTypes) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="启用" width="80" align="center">
        <template #default="{ row }">
          <el-switch :model-value="row.isEnabled" @change="handleToggle(row)" />
        </template>
      </el-table-column>
      <el-table-column label="最近状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.lastStatus === 'SUCCESS'" type="success" size="small">成功</el-tag>
          <el-tag v-else-if="row.lastStatus === 'FAILED'" type="danger" size="small">失败</el-tag>
          <span v-else class="text-gray-400 text-xs">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="retryCount" label="重试次数" width="90" align="center" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <div class="flex items-center gap-1">
            <el-button link type="primary" size="small" @click="handleTest(row)" :loading="testing === row.id">
              <Zap class="w-3.5 h-3.5" />
            </el-button>
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
    <el-dialog v-model="showDialog" :title="editingId ? '编辑 Webhook' : '新建 Webhook'" width="550px">
      <el-form label-width="100px">
        <el-form-item label="名称" required>
          <el-input v-model="form.subscriptionName" placeholder="输入 Webhook 名称" />
        </el-form-item>
        <el-form-item label="目标 URL" required>
          <el-input v-model="form.targetUrl" placeholder="https://example.com/webhook" />
        </el-form-item>
        <el-form-item label="密钥">
          <el-input v-model="form.secret" placeholder="可选，用于签名验证" show-password />
        </el-form-item>
        <el-form-item label="事件类型" required>
          <el-input v-model="form.eventTypes" placeholder='["TASK_SUBMITTED"]' />
          <div class="text-xs text-gray-400 mt-1">JSON 数组格式，如 ["TASK_SUBMITTED", "CORRECTIVE_CREATED"]</div>
        </el-form-item>
        <el-form-item label="重试次数">
          <el-input-number v-model="form.retryCount" :min="0" :max="10" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
