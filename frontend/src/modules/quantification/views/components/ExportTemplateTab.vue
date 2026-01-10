<template>
  <div class="export-template-tab">
    <!-- 顶部操作栏 -->
    <div class="tab-header">
      <h3 class="section-title">
        <FileOutput :size="18" />
        导出模板管理
      </h3>
      <button class="btn btn-primary" @click="openCreateDialog">
        <Plus :size="16" />
        创建模板
      </button>
    </div>

    <!-- 说明文字 -->
    <div class="tab-description">
      导出模板用于将日常检查的数据导出为PDF、Word或Excel文件。您可以自定义筛选条件、表格格式和文档模板。
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <Loader2 class="spinner" />
      <span>加载中...</span>
    </div>

    <!-- 空状态 -->
    <div v-else-if="templates.length === 0" class="empty-state">
      <FileOutput :size="48" class="empty-icon" />
      <p>暂无导出模板</p>
      <button class="btn btn-primary" @click="openCreateDialog">
        创建第一个模板
      </button>
    </div>

    <!-- 模板列表 -->
    <div v-else class="template-list">
      <div
        v-for="template in templates"
        :key="template.id"
        class="template-card"
      >
        <div class="template-main">
          <div class="template-info">
            <h4 class="template-name">{{ template.templateName }}</h4>
            <p class="template-desc">{{ template.description || '暂无描述' }}</p>
          </div>
          <div class="template-meta">
            <span class="format-badge" :class="getFormatClass(template.outputFormat)">
              {{ template.outputFormat }}
            </span>
            <span class="create-time">
              创建于 {{ formatDate(template.createdAt) }}
            </span>
          </div>
        </div>
        <div class="template-actions">
          <button class="btn btn-ghost btn-sm" @click="editTemplate(template)">
            <Pencil :size="14" />
            编辑
          </button>
          <button class="btn btn-ghost btn-sm btn-danger" @click="deleteTemplate(template)">
            <Trash2 :size="14" />
            删除
          </button>
        </div>
      </div>
    </div>

    <!-- 模板编辑器对话框 -->
    <ExportTemplateEditor
      v-model="editorVisible"
      :plan-id="planId"
      :template="editingTemplate"
      :deduction-items="deductionItems"
      @success="handleEditorSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { FileOutput, Plus, Pencil, Trash2, Loader2 } from 'lucide-vue-next'
import ExportTemplateEditor from './ExportTemplateEditor.vue'
import {
  getExportTemplatesByPlan,
  getExportTemplate,
  deleteExportTemplate,
  type ExportTemplateDTO
} from '@/api/v2/exportTemplate'

const props = defineProps<{
  planId: string | number
  deductionItems?: Array<{ id: string | number; itemName: string }>
}>()

const loading = ref(false)
const templates = ref<ExportTemplateDTO[]>([])

// 编辑器状态
const editorVisible = ref(false)
const editingTemplate = ref<ExportTemplateDTO | null>(null)

// 加载模板列表
const loadTemplates = async () => {
  if (!props.planId) return

  loading.value = true
  try {
    templates.value = await getExportTemplatesByPlan(props.planId)
  } catch (error: any) {
    console.error('加载导出模板失败', error)
  } finally {
    loading.value = false
  }
}

// 打开创建对话框
const openCreateDialog = () => {
  editingTemplate.value = null
  editorVisible.value = true
}

// 编辑模板
const editTemplate = async (template: ExportTemplateDTO) => {
  try {
    // 获取模板完整数据
    const fullTemplate = await getExportTemplate(template.id!)
    editingTemplate.value = fullTemplate
    editorVisible.value = true
  } catch (error: any) {
    ElMessage.error('获取模板数据失败')
    console.error(error)
  }
}

// 删除模板
const deleteTemplate = async (template: ExportTemplateDTO) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除模板"${template.templateName}"吗？`,
      '删除确认',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消'
      }
    )

    await deleteExportTemplate(template.id!)
    ElMessage.success('删除成功')
    loadTemplates()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 编辑器成功回调
const handleEditorSuccess = () => {
  loadTemplates()
}

// 获取格式样式类
const getFormatClass = (format?: string) => {
  const classes: Record<string, string> = {
    PDF: 'format-pdf',
    WORD: 'format-word',
    EXCEL: 'format-excel'
  }
  return classes[format || 'PDF'] || ''
}

// 格式化日期
const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN')
}

// 监听planId变化
watch(
  () => props.planId,
  (newId) => {
    if (newId) {
      loadTemplates()
    }
  },
  { immediate: true }
)
</script>

<style scoped lang="scss">
.export-template-tab {
  padding: 0;
}

.tab-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;

  .section-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 16px;
    font-weight: 600;
    color: #1f2937;
    margin: 0;
  }
}

.tab-description {
  padding: 12px 16px;
  background: #f0f7ff;
  border-radius: 8px;
  color: #1e40af;
  font-size: 14px;
  margin-bottom: 20px;
}

.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #6b7280;

  .spinner {
    width: 32px;
    height: 32px;
    animation: spin 1s linear infinite;
  }

  .empty-icon {
    color: #d1d5db;
    margin-bottom: 16px;
  }

  p {
    margin-bottom: 16px;
  }
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.template-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.template-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  transition: all 0.2s;

  &:hover {
    border-color: #3b82f6;
    box-shadow: 0 2px 8px rgba(59, 130, 246, 0.1);
  }

  .template-main {
    flex: 1;
    display: flex;
    align-items: center;
    gap: 20px;
  }

  .template-info {
    flex: 1;

    .template-name {
      font-size: 15px;
      font-weight: 600;
      color: #1f2937;
      margin: 0 0 4px 0;
    }

    .template-desc {
      font-size: 13px;
      color: #6b7280;
      margin: 0;
    }
  }

  .template-meta {
    display: flex;
    align-items: center;
    gap: 12px;

    .format-badge {
      padding: 4px 10px;
      border-radius: 4px;
      font-size: 12px;
      font-weight: 600;

      &.format-pdf {
        background: #fef2f2;
        color: #dc2626;
      }

      &.format-word {
        background: #eff6ff;
        color: #2563eb;
      }

      &.format-excel {
        background: #f0fdf4;
        color: #16a34a;
      }
    }

    .create-time {
      font-size: 12px;
      color: #9ca3af;
    }
  }

  .template-actions {
    display: flex;
    gap: 8px;
  }
}

// 按钮样式
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;

  &.btn-primary {
    background: #3b82f6;
    color: #fff;

    &:hover {
      background: #2563eb;
    }
  }

  &.btn-ghost {
    background: transparent;
    color: #6b7280;

    &:hover {
      background: #f3f4f6;
      color: #1f2937;
    }

    &.btn-danger:hover {
      background: #fef2f2;
      color: #dc2626;
    }
  }

  &.btn-sm {
    padding: 6px 12px;
    font-size: 13px;
  }
}
</style>
