<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Pencil, Trash2, FileDown } from 'lucide-vue-next'
import { useInspPlatformStore } from '@/stores/insp/inspPlatformStore'
import type { ReportTemplate } from '@/types/insp/platform'

const store = useInspPlatformStore()

const loading = ref(false)
const templates = ref<ReportTemplate[]>([])
const showDialog = ref(false)
const editingId = ref<number | null>(null)
const generating = ref<number | null>(null)

const form = ref({
  templateName: '',
  templateCode: '',
  reportType: 'DAILY_SUMMARY',
  isDefault: false,
})

const reportTypeOptions = [
  { value: 'DAILY_SUMMARY', label: '日报汇总' },
  { value: 'PERIOD_REPORT', label: '周期报表' },
  { value: 'CORRECTIVE_REPORT', label: '整改报表' },
  { value: 'INSPECTOR_REPORT', label: '检查员报表' },
  { value: 'CUSTOM', label: '自定义' },
]

function getReportTypeLabel(type: string): string {
  return reportTypeOptions.find(o => o.value === type)?.label ?? type
}

async function loadData() {
  loading.value = true
  try {
    await store.fetchReportTemplates()
    templates.value = store.reportTemplates
  } catch (e: any) {
    ElMessage.error(e.message || '加载报表模板失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.value = { templateName: '', templateCode: '', reportType: 'DAILY_SUMMARY', isDefault: false }
  showDialog.value = true
}

function openEdit(tpl: ReportTemplate) {
  editingId.value = tpl.id
  form.value = {
    templateName: tpl.templateName,
    templateCode: tpl.templateCode,
    reportType: tpl.reportType,
    isDefault: tpl.isDefault,
  }
  showDialog.value = true
}

async function handleSave() {
  if (!form.value.templateName || !form.value.templateCode) {
    ElMessage.warning('请填写模板名称和编码')
    return
  }
  try {
    if (editingId.value) {
      await store.updateReportTemplate(editingId.value, {
        templateName: form.value.templateName,
        reportType: form.value.reportType,
      })
      ElMessage.success('更新成功')
    } else {
      await store.createReportTemplate({
        templateName: form.value.templateName,
        templateCode: form.value.templateCode,
        reportType: form.value.reportType,
        isDefault: form.value.isDefault,
      })
      ElMessage.success('创建成功')
    }
    showDialog.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  }
}

async function handleDelete(tpl: ReportTemplate) {
  try {
    await ElMessageBox.confirm(`确认删除报表模板「${tpl.templateName}」？`, '确认删除', { type: 'warning' })
    await store.deleteReportTemplate(tpl.id)
    ElMessage.success('删除成功')
    loadData()
  } catch { /* cancelled */ }
}

async function handleGenerate(tpl: ReportTemplate) {
  generating.value = tpl.id
  try {
    await store.generateReport(tpl.id)
    ElMessage.success('报表生成成功')
  } catch (e: any) {
    ElMessage.error(e.message || '生成失败')
  } finally {
    generating.value = null
  }
}

onMounted(() => loadData())
</script>

<template>
  <div class="p-5 space-y-4">
    <div class="flex items-center justify-between">
      <h2 class="text-lg font-semibold">报表模板</h2>
      <el-button type="primary" @click="openCreate">
        <Plus class="w-4 h-4 mr-1" />新建模板
      </el-button>
    </div>

    <el-table :data="templates" v-loading="loading" stripe>
      <el-table-column prop="templateName" label="模板名称" min-width="160" />
      <el-table-column prop="templateCode" label="编码" width="140" />
      <el-table-column label="报表类型" width="120">
        <template #default="{ row }">
          <el-tag size="small" type="info">{{ getReportTypeLabel(row.reportType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="默认" width="80" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isDefault" type="success" size="small">是</el-tag>
          <span v-else class="text-gray-400 text-xs">否</span>
        </template>
      </el-table-column>
      <el-table-column label="启用" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.isEnabled ? 'success' : 'info'" size="small">
            {{ row.isEnabled ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170" show-overflow-tooltip />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <div class="flex items-center gap-1">
            <el-button link type="primary" size="small" @click="openEdit(row)">
              <Pencil class="w-3.5 h-3.5" />
            </el-button>
            <el-button link type="success" size="small" :loading="generating === row.id" @click="handleGenerate(row)">
              <FileDown class="w-3.5 h-3.5" />
            </el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">
              <Trash2 class="w-3.5 h-3.5" />
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="!loading && templates.length === 0" class="text-center py-8 text-gray-400 text-sm">
      暂无报表模板
    </div>

    <!-- Create/Edit Dialog -->
    <el-dialog v-model="showDialog" :title="editingId ? '编辑报表模板' : '新建报表模板'" width="480px">
      <el-form label-width="100px">
        <el-form-item label="模板名称" required>
          <el-input v-model="form.templateName" placeholder="输入模板名称" />
        </el-form-item>
        <el-form-item label="模板编码" required>
          <el-input v-model="form.templateCode" placeholder="如 DAILY_REPORT_V1" :disabled="!!editingId" />
        </el-form-item>
        <el-form-item label="报表类型" required>
          <el-select v-model="form.reportType" class="w-full">
            <el-option v-for="opt in reportTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="form.isDefault" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
