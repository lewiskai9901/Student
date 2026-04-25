<template>
  <el-dialog
    :model-value="visible"
    title="导出数据"
    width="480px"
    :close-on-click-modal="false"
    @update:model-value="$emit('update:visible', $event)"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="90px"
      label-position="right"
    >
      <el-form-item label="导出格式" prop="format">
        <el-select v-model="formData.format" class="w-full">
          <el-option
            v-for="opt in formatOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="日期范围" prop="dateRange">
        <el-date-picker
          v-model="formData.dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          class="w-full"
          :shortcuts="dateShortcuts"
        />
      </el-form-item>

      <el-form-item label="包含图表">
        <el-switch v-model="formData.includeCharts" />
        <span class="ml-2 text-xs text-gray-400">
          导出文件中嵌入图表截图（仅PDF格式支持）
        </span>
      </el-form-item>

      <el-form-item v-if="formData.includeCharts && formData.format !== 'PDF'" label="">
        <el-alert
          title="图表嵌入仅在 PDF 格式下生效"
          type="warning"
          :closable="false"
          show-icon
          class="w-full"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="flex justify-end gap-2">
        <el-button @click="$emit('update:visible', false)">取消</el-button>
        <el-button type="primary" :loading="exporting" @click="handleExport">
          导出
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'

export interface ExportParams {
  format: string
  dateRange: [string, string]
  includeCharts: boolean
}

defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  'update:visible': [val: boolean]
  export: [params: ExportParams]
}>()

const formRef = ref<FormInstance>()
const exporting = ref(false)

const formatOptions = [
  { value: 'EXCEL', label: 'Excel (.xlsx)' },
  { value: 'CSV', label: 'CSV (.csv)' },
  { value: 'PDF', label: 'PDF (.pdf)' },
]

const dateShortcuts = [
  {
    text: '最近一周',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setDate(start.getDate() - 7)
      return [start, end]
    },
  },
  {
    text: '最近一月',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setMonth(start.getMonth() - 1)
      return [start, end]
    },
  },
  {
    text: '最近三月',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setMonth(start.getMonth() - 3)
      return [start, end]
    },
  },
  {
    text: '本学期',
    value: () => {
      const now = new Date()
      const month = now.getMonth()
      const year = now.getFullYear()
      // Semester start: Feb or Sep
      const start = month >= 1 && month < 8
        ? new Date(year, 1, 1)
        : new Date(year, 8, 1)
      return [start, now]
    },
  },
]

const formData = reactive({
  format: 'EXCEL',
  dateRange: null as [string, string] | null,
  includeCharts: false,
})

const rules: FormRules = {
  format: [{ required: true, message: '请选择导出格式', trigger: 'change' }],
  dateRange: [{ required: true, message: '请选择日期范围', trigger: 'change' }],
}

async function handleExport() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid || !formData.dateRange) return

  exporting.value = true
  try {
    emit('export', {
      format: formData.format,
      dateRange: formData.dateRange,
      includeCharts: formData.includeCharts,
    })
  } finally {
    exporting.value = false
  }
}
</script>

<style scoped>
.w-full {
  width: 100%;
}
</style>
