<template>
  <div class="notification-center-tab compact">
    <!-- 快速通报区域 -->
    <div class="quick-section">
      <div class="section-header-compact">
        <h3><FileOutput :size="16" /> 快速生成通报</h3>
        <button class="btn-text" @click="showTemplateManager = true">管理模板</button>
      </div>

      <div class="steps-container">
        <!-- 步骤1: 选择检查 -->
        <div class="step-row">
          <div class="step-label"><span class="step-num">1</span> 选择检查</div>
          <div class="step-options">
            <div
              v-for="check in recentChecks"
              :key="check.id"
              class="option-chip"
              :class="{ selected: selectedCheckIds.includes(check.id) }"
              @click="toggleCheckSelection(check)"
            >
              <span class="chip-main">{{ formatDate(check.checkDate) }}</span>
              <span class="chip-sub">{{ check.checkName }} · {{ check.totalRounds }}轮</span>
            </div>
            <div v-if="recentChecks.length === 0" class="empty-text">暂无检查记录</div>
          </div>
        </div>

        <!-- 步骤2: 选择轮次 -->
        <div class="step-row" v-if="selectedCheckIds.length === 1">
          <div class="step-label"><span class="step-num">2</span> 选择轮次</div>
          <div class="step-options">
            <label
              v-for="(round, index) in availableRounds"
              :key="index"
              class="option-chip"
              :class="{ selected: selectedRounds.includes(index + 1) }"
            >
              <input type="checkbox" :value="index + 1" v-model="selectedRounds" hidden>
              <span class="chip-main">{{ round }}</span>
            </label>
          </div>
        </div>

        <!-- 步骤3: 筛选检查类别（可选） -->
        <div class="step-row" v-if="canShowDeductionFilter">
          <div class="step-label">
            <span class="step-num">{{ selectedCheckIds.length === 1 ? '3' : '2' }}</span>
            筛选类别
            <span class="step-optional">可选</span>
          </div>
          <div class="step-options category-filter">
            <label class="option-chip select-all" :class="{ selected: isAllDeductionSelected }">
              <input type="checkbox" v-model="isAllDeductionSelected" @change="toggleAllDeductions" hidden>
              <span class="chip-main">全部类别</span>
            </label>
            <!-- 按类别分组显示 -->
            <template v-for="(items, categoryName) in groupedDeductionItems" :key="categoryName">
              <div class="category-group">
                <div class="category-name">{{ categoryName }}</div>
                <div class="category-items">
                  <label
                    v-for="item in items"
                    :key="item.id"
                    class="option-chip small"
                    :class="{ selected: selectedDeductionItemIds.includes(item.id) }"
                  >
                    <input type="checkbox" :value="item.id" v-model="selectedDeductionItemIds" hidden>
                    <span class="chip-main">{{ item.name }}</span>
                  </label>
                </div>
              </div>
            </template>
          </div>
        </div>

        <!-- 步骤4: 选择模板 -->
        <div class="step-row" v-if="selectedCheckIds.length > 0 && (selectedCheckIds.length > 1 || selectedRounds.length > 0)">
          <div class="step-label">
            <span class="step-num">{{ selectedCheckIds.length === 1 ? '4' : '3' }}</span>
            选择模板
          </div>
          <div class="step-options">
            <div
              v-for="template in templates"
              :key="template.id"
              class="option-chip template"
              :class="{ selected: selectedTemplateId === template.id }"
              @click="selectedTemplateId = template.id"
            >
              <FileText :size="14" />
              <span class="chip-main">{{ template.templateName }}</span>
            </div>
            <div class="option-chip add-new" @click="handleCreateTemplate">
              <Plus :size="14" />
              <span class="chip-main">新建</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="action-row" v-if="canGenerate && selectedTemplateId">
        <span class="action-info">
          {{ selectedCheckIds.length === 1 ? `1个检查，${selectedRounds.length}个轮次` : `${selectedCheckIds.length}个检查汇总` }}
          <template v-if="selectedDeductionItemIds.length > 0">，{{ selectedDeductionItemIds.length }}个扣分项</template>
        </span>
        <button class="btn-primary" @click="handleGenerateDraft" :disabled="generating">
          <Loader2 v-if="generating" :size="14" class="spin" />
          <FileEdit v-else :size="14" />
          生成通报
        </button>
      </div>
    </div>

    <!-- 历史通报 -->
    <div class="history-section">
      <div class="section-header-compact">
        <h3><Clock :size="16" /> 历史通报</h3>
      </div>

      <div v-if="loadingHistory" class="loading-compact">
        <Loader2 :size="16" class="spin" /> 加载中...
      </div>

      <div v-else-if="notificationHistory.length === 0" class="empty-compact">
        暂无通报记录
      </div>

      <div v-else class="history-list-compact">
        <div v-for="record in notificationHistory" :key="record.id" class="history-row">
          <FileText :size="14" class="row-icon" />
          <span class="row-title">{{ record.title || '通报' }}</span>
          <span class="row-meta">{{ formatDateTime(record.createdAt) }} · {{ record.totalCount }}人</span>
          <span :class="['tag', record.publishStatus === 1 ? 'published' : 'draft']">
            {{ getPublishStatusText(record.publishStatus || 0) }}
          </span>
          <span v-if="record.status === 0" class="tag warning">生成中</span>
          <span v-else-if="record.status === 2" class="tag error">失败</span>
          <div class="row-actions">
            <button class="btn-icon" @click="handleEdit(record)" :title="record.publishStatus === 1 ? '查看' : '编辑'">
              <Pencil :size="14" />
            </button>
            <button class="btn-icon danger" @click="handleDelete(record)" title="删除">
              <Trash2 :size="14" />
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 模板管理对话框 -->
    <el-dialog
      v-model="showTemplateManager"
      title="通报模板管理"
      width="900px"
      destroy-on-close
    >
      <ExportTemplateTab
        :plan-id="planId"
        :deduction-items="deductionItems"
      />
    </el-dialog>

    <!-- 预览对话框 -->
    <el-dialog
      v-model="showPreviewDialog"
      :title="previewTitle || '通报预览'"
      width="800px"
      destroy-on-close
    >
      <div class="preview-container" v-html="sanitizedPreviewHtml"></div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import DOMPurify from 'dompurify'
import {
  FileOutput, FileText, Plus, Download, Clock,
  Loader2, RefreshCw, Trash2, FileEdit, Pencil
} from 'lucide-vue-next'
import ExportTemplateTab from './ExportTemplateTab.vue'
import { getExportTemplatesByPlan } from '@/api/exportTemplate'
import {
  generateNotification,
  getNotificationHistory,
  downloadNotification,
  regenerateNotification,
  deleteNotification,
  getPublishStatusText,
  type NotificationRecord
} from '@/api/notification'

const router = useRouter()

const props = defineProps<{
  planId: string | number
  dailyChecks: any[]
  checkRecords: any[]
  deductionItems?: any[]
}>()

// 状态
const showTemplateManager = ref(false)
const showPreviewDialog = ref(false)
const previewHtml = ref('')
const previewTitle = ref('')

// XSS防护：对预览HTML进行消毒
const sanitizedPreviewHtml = computed(() => {
  if (!previewHtml.value) return ''
  return DOMPurify.sanitize(previewHtml.value, {
    ALLOWED_TAGS: ['p', 'br', 'strong', 'em', 'u', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
                   'table', 'thead', 'tbody', 'tr', 'th', 'td', 'ul', 'ol', 'li',
                   'span', 'div', 'a', 'img', 'style'],
    ALLOWED_ATTR: ['class', 'style', 'href', 'src', 'alt', 'title', 'colspan', 'rowspan', 'width', 'height'],
    ALLOW_DATA_ATTR: false
  })
})
const selectedCheckIds = ref<number[]>([])
const selectedRounds = ref<number[]>([])
const selectedDeductionItemIds = ref<number[]>([])
const selectedTemplateId = ref<number | null>(null)
const templates = ref<any[]>([])
const notificationHistory = ref<NotificationRecord[]>([])
const loadingHistory = ref(false)
const generating = ref(false)

// 获取最近的检查（进行中及以上状态）
const recentChecks = computed(() => {
  // 优先使用检查记录，如果没有则使用日常检查中进行中及以上状态的
  if (props.checkRecords.length > 0) {
    return props.checkRecords.slice(0, 10)
  }
  // status >= 1 表示进行中、已提交、已发布的检查都可以生成通报
  return props.dailyChecks.filter(c => c.status >= 1).slice(0, 10)
})

// 获取选中检查的轮次
const availableRounds = computed(() => {
  if (selectedCheckIds.value.length !== 1) return []
  const check = recentChecks.value.find(c => c.id === selectedCheckIds.value[0])
  if (!check) return []
  try {
    return JSON.parse(check.roundNames || '[]')
  } catch {
    return Array.from({ length: check.totalRounds || 1 }, (_, i) => `第${i + 1}轮`)
  }
})

// 是否可以生成通报
const canGenerate = computed(() => {
  if (selectedCheckIds.value.length === 0) return false
  if (selectedCheckIds.value.length === 1 && selectedRounds.value.length === 0) return false
  return true
})

// 是否显示扣分项筛选
const canShowDeductionFilter = computed(() => {
  return selectedCheckIds.value.length > 0 &&
         (selectedCheckIds.value.length > 1 || selectedRounds.value.length > 0) &&
         availableDeductionItems.value.length > 0
})

// 可用的扣分项列表（从props.deductionItems获取）
const availableDeductionItems = computed(() => {
  return props.deductionItems || []
})

// 按检查类别分组的扣分项
const groupedDeductionItems = computed(() => {
  const items = availableDeductionItems.value
  const groups: Record<string, any[]> = {}

  for (const item of items) {
    const categoryName = item.categoryName || item.category || '其他'
    if (!groups[categoryName]) {
      groups[categoryName] = []
    }
    groups[categoryName].push(item)
  }

  return groups
})

// 是否全选扣分项
const isAllDeductionSelected = computed({
  get() {
    return selectedDeductionItemIds.value.length === 0 ||
           selectedDeductionItemIds.value.length === availableDeductionItems.value.length
  },
  set(val: boolean) {
    if (val) {
      selectedDeductionItemIds.value = []
    }
  }
})

// 切换全选扣分项
function toggleAllDeductions() {
  if (isAllDeductionSelected.value) {
    selectedDeductionItemIds.value = []
  }
}

// 切换检查选择
function toggleCheckSelection(check: any) {
  const index = selectedCheckIds.value.indexOf(check.id)
  if (index >= 0) {
    selectedCheckIds.value.splice(index, 1)
  } else {
    selectedCheckIds.value.push(check.id)
  }
  // 重置轮次选择和扣分项选择
  selectedRounds.value = []
  selectedDeductionItemIds.value = []
}

// 加载模板
async function loadTemplates() {
  try {
    templates.value = await getExportTemplatesByPlan(props.planId)
  } catch (error) {
    console.error('加载模板失败', error)
  }
}

// 加载历史通报
async function loadHistory() {
  loadingHistory.value = true
  try {
    const res = await getNotificationHistory(props.planId)
    notificationHistory.value = res || []
  } catch (error) {
    console.error('加载历史通报失败', error)
    notificationHistory.value = []
  } finally {
    loadingHistory.value = false
  }
}

// 创建模板
function handleCreateTemplate() {
  showTemplateManager.value = true
}

// 生成通报草稿并跳转到编辑页面
async function handleGenerateDraft() {
  if (!selectedTemplateId.value) {
    ElMessage.warning('请先选择模板')
    return
  }

  try {
    generating.value = true
    const record = await generateNotification(props.planId, {
      templateId: selectedTemplateId.value,
      dailyCheckIds: selectedCheckIds.value,
      checkRounds: selectedRounds.value.length > 0 ? selectedRounds.value : undefined,
      deductionItemIds: selectedDeductionItemIds.value.length > 0 ? selectedDeductionItemIds.value : undefined
    })

    ElMessage.success('通报生成成功，正在跳转到编辑页面...')

    // 重置选择
    selectedCheckIds.value = []
    selectedRounds.value = []
    selectedDeductionItemIds.value = []
    selectedTemplateId.value = null

    // 跳转到编辑页面
    router.push(`/quantification/notification/${record.id}/edit`)
  } catch (error: any) {
    ElMessage.error(error.message || '生成失败')
  } finally {
    generating.value = false
  }
}

// 编辑通报
function handleEdit(record: NotificationRecord) {
  router.push(`/quantification/notification/${record.id}/edit`)
}

// 下载
async function handleDownload(record: NotificationRecord) {
  try {
    const blob = await downloadNotification(record.id)
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = record.fileName || 'notification.pdf'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch (error: any) {
    ElMessage.error(error.message || '下载失败')
  }
}

// 重新生成
async function handleRegenerate(record: NotificationRecord) {
  try {
    await ElMessageBox.confirm(
      '确定要重新生成此通报吗？将使用相同的参数重新生成。',
      '确认重新生成',
      { type: 'info' }
    )

    const newRecord = await regenerateNotification(record.id)
    ElMessage.success('重新生成成功')
    loadHistory()

    // 自动下载新文件
    await handleDownload(newRecord)
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '重新生成失败')
    }
  }
}

// 删除通报
async function handleDelete(record: NotificationRecord) {
  try {
    await ElMessageBox.confirm(
      '确定要删除此通报记录吗？删除后无法恢复。',
      '确认删除',
      { type: 'warning' }
    )

    await deleteNotification(record.id)
    ElMessage.success('删除成功')
    loadHistory()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 工具函数
function formatDate(dateStr?: string) {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}/${date.getDate()}`
}

function formatDateTime(dateStr?: string) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

function getStatusClass(status: number) {
  const classes: Record<number, string> = {
    0: 'default',
    1: 'progress',
    2: 'warning',
    3: 'success'
  }
  return classes[status] || 'default'
}

function getStatusText(status: number) {
  const texts: Record<number, string> = {
    0: '未开始',
    1: '进行中',
    2: '已提交',
    3: '已发布'
  }
  return texts[status] || '未知'
}

// 监听planId变化
watch(() => props.planId, () => {
  loadTemplates()
  loadHistory()
}, { immediate: true })

onMounted(() => {
  loadTemplates()
  loadHistory()
})
</script>

<style scoped lang="scss">
// 紧凑布局样式
.notification-center-tab.compact {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

// 区域头部
.section-header-compact {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;

  h3 {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 14px;
    font-weight: 600;
    color: #1f2937;
    margin: 0;
  }
}

// 快速通报区域
.quick-section {
  padding: 16px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

// 步骤容器
.steps-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.step-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.step-label {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 90px;
  padding-top: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #374151;
  white-space: nowrap;
}

.step-num {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  background: #3b82f6;
  color: #fff;
  border-radius: 50%;
  font-size: 11px;
  font-weight: 600;
}

.step-optional {
  font-size: 11px;
  color: #9ca3af;
  font-weight: normal;
}

.step-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  flex: 1;

  &.category-filter {
    flex-direction: column;
    gap: 10px;
  }
}

// 选项芯片
.option-chip {
  display: inline-flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  padding: 6px 12px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;

  &:hover {
    border-color: #93c5fd;
    background: #f8fafc;
  }

  &.selected {
    border-color: #3b82f6;
    background: #eff6ff;
  }

  &.small {
    padding: 4px 10px;
    flex-direction: row;
    align-items: center;
  }

  &.template {
    flex-direction: row;
    align-items: center;
    gap: 6px;
  }

  &.add-new {
    flex-direction: row;
    align-items: center;
    gap: 4px;
    border-style: dashed;
    color: #9ca3af;

    &:hover {
      color: #3b82f6;
      border-color: #3b82f6;
    }
  }

  &.select-all {
    background: #f9fafb;
    border-style: dashed;

    &.selected {
      border-style: solid;
    }
  }
}

.chip-main {
  font-size: 13px;
  font-weight: 500;
  color: #374151;
}

.chip-sub {
  font-size: 11px;
  color: #9ca3af;
}

// 类别分组
.category-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.category-name {
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
  padding-left: 2px;
}

.category-items {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

// 操作行
.action-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  margin-top: 12px;
  background: #f0f9ff;
  border-radius: 6px;
}

.action-info {
  font-size: 12px;
  color: #1e40af;
}

// 按钮样式
.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: #3b82f6;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;

  &:hover:not(:disabled) {
    background: #2563eb;
  }

  &:disabled {
    background: #93c5fd;
    cursor: not-allowed;
  }
}

.btn-text {
  background: transparent;
  border: none;
  color: #3b82f6;
  font-size: 13px;
  cursor: pointer;
  padding: 4px 8px;

  &:hover {
    text-decoration: underline;
  }
}

.btn-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  background: transparent;
  border: none;
  border-radius: 4px;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.15s;

  &:hover {
    background: #f3f4f6;
    color: #374151;
  }

  &.danger:hover {
    background: #fef2f2;
    color: #dc2626;
  }
}

// 历史区域
.history-section {
  padding: 16px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.history-list-compact {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.history-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  background: #f9fafb;
  border-radius: 6px;
  transition: background 0.15s;

  &:hover {
    background: #f3f4f6;

    .row-actions {
      opacity: 1;
    }
  }
}

.row-icon {
  color: #6b7280;
  flex-shrink: 0;
}

.row-title {
  flex: 1;
  font-size: 13px;
  font-weight: 500;
  color: #374151;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row-meta {
  font-size: 11px;
  color: #9ca3af;
  white-space: nowrap;
}

.row-actions {
  display: flex;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.15s;
}

// 标签
.tag {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
  white-space: nowrap;

  &.draft {
    background: #fef3c7;
    color: #d97706;
  }

  &.published {
    background: #d1fae5;
    color: #059669;
  }

  &.warning {
    background: #fef3c7;
    color: #d97706;
  }

  &.error {
    background: #fef2f2;
    color: #dc2626;
  }
}

// 空状态和加载
.empty-text {
  font-size: 13px;
  color: #9ca3af;
  padding: 8px 0;
}

.empty-compact,
.loading-compact {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 24px;
  color: #9ca3af;
  font-size: 13px;
}

// 动画
.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

// 预览容器
.preview-container {
  max-height: 600px;
  overflow: auto;
  padding: 20px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}
</style>
