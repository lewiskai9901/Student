<template>
  <div class="p-6">
    <!-- 导出范围选择 -->
    <div class="mb-6">
      <h3 class="text-sm font-medium text-gray-900 mb-3">导出范围</h3>
      <div class="flex gap-4">
        <label class="flex items-center cursor-pointer">
          <input
            type="radio"
            v-model="exportScope"
            value="selected"
            :disabled="selectedStudents.length === 0"
            class="h-4 w-4 text-blue-600 focus:ring-blue-500"
          />
          <span class="ml-2 text-sm text-gray-700">
            导出选中 ({{ selectedStudents.length }} 条)
          </span>
        </label>
        <label class="flex items-center cursor-pointer">
          <input
            type="radio"
            v-model="exportScope"
            value="filtered"
            class="h-4 w-4 text-blue-600 focus:ring-blue-500"
          />
          <span class="ml-2 text-sm text-gray-700">
            导出筛选结果 ({{ totalFiltered }} 条)
          </span>
        </label>
        <label class="flex items-center cursor-pointer">
          <input
            type="radio"
            v-model="exportScope"
            value="all"
            class="h-4 w-4 text-blue-600 focus:ring-blue-500"
          />
          <span class="ml-2 text-sm text-gray-700">导出全部</span>
        </label>
      </div>
    </div>

    <!-- 字段选择和排序 -->
    <div class="mb-6">
      <div class="flex items-center justify-between mb-3">
        <h3 class="text-sm font-medium text-gray-900">导出字段 (拖拽可调整顺序)</h3>
        <div class="flex gap-2">
          <button
            class="text-xs text-blue-600 hover:text-blue-700"
            @click="selectAllFields"
          >
            全选
          </button>
          <span class="text-gray-300">|</span>
          <button
            class="text-xs text-blue-600 hover:text-blue-700"
            @click="deselectAllFields"
          >
            取消全选
          </button>
          <span class="text-gray-300">|</span>
          <button
            class="text-xs text-blue-600 hover:text-blue-700"
            @click="resetFields"
          >
            恢复默认
          </button>
        </div>
      </div>

      <div class="border border-gray-200 rounded-lg p-3 bg-gray-50 max-h-64 overflow-y-auto">
        <div
          v-for="(field, index) in exportFields"
          :key="field.key"
          class="flex items-center gap-3 py-2 px-2 bg-white rounded mb-1 border border-gray-100 hover:border-blue-300 cursor-move"
          draggable="true"
          @dragstart="handleDragStart($event, index)"
          @dragover.prevent
          @dragenter="handleDragEnter($event, index)"
          @dragleave="handleDragLeave($event)"
          @drop="handleDrop($event, index)"
        >
          <GripVertical class="h-4 w-4 text-gray-400" />
          <input
            type="checkbox"
            v-model="field.selected"
            class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
          />
          <span class="text-sm text-gray-700 flex-1">{{ field.label }}</span>
          <span class="text-xs text-gray-400">{{ field.key }}</span>
        </div>
      </div>
      <p class="mt-2 text-xs text-gray-500">
        已选择 {{ selectedFieldCount }} 个字段
      </p>
    </div>

    <!-- 文件名设置 -->
    <div class="mb-6">
      <h3 class="text-sm font-medium text-gray-900 mb-3">文件名</h3>
      <div class="flex items-center gap-2">
        <input
          v-model="fileName"
          type="text"
          class="flex-1 h-9 rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          placeholder="请输入文件名"
        />
        <span class="text-sm text-gray-500">.xlsx</span>
      </div>
    </div>

    <!-- 预览 -->
    <div class="mb-6">
      <div class="flex items-center justify-between mb-3">
        <h3 class="text-sm font-medium text-gray-900">数据预览</h3>
        <span class="text-xs text-gray-500">显示前 5 条数据</span>
      </div>
      <div class="border border-gray-200 rounded-lg overflow-hidden">
        <div class="overflow-x-auto">
          <table class="min-w-full divide-y divide-gray-200">
            <thead class="bg-gray-50">
              <tr>
                <th
                  v-for="field in selectedFields"
                  :key="field.key"
                  class="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase whitespace-nowrap"
                >
                  {{ field.label }}
                </th>
              </tr>
            </thead>
            <tbody class="bg-white divide-y divide-gray-100">
              <tr v-for="(row, idx) in previewData" :key="idx">
                <td
                  v-for="field in selectedFields"
                  :key="field.key"
                  class="px-3 py-2 text-xs text-gray-600 whitespace-nowrap"
                >
                  {{ formatFieldValue(row, field) }}
                </td>
              </tr>
              <tr v-if="previewData.length === 0">
                <td
                  :colspan="selectedFields.length"
                  class="px-3 py-8 text-center text-sm text-gray-500"
                >
                  暂无数据
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- 操作按钮 -->
    <div class="flex items-center justify-end gap-3 pt-4 border-t border-gray-200">
      <button
        class="h-9 rounded-md border border-gray-300 bg-white px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
        @click="$emit('close')"
      >
        取消
      </button>
      <button
        :disabled="selectedFieldCount === 0 || exporting"
        class="h-9 rounded-md bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
        @click="handleExport"
      >
        <span v-if="exporting" class="inline-flex items-center">
          <span class="mr-2 h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent"></span>
          导出中...
        </span>
        <span v-else>确认导出</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { GripVertical } from 'lucide-vue-next'
import * as XLSX from 'xlsx'
import type { Student } from '@/types/student'

interface ExportField {
  key: string
  label: string
  selected: boolean
  format?: (value: any, row: any) => string
}

interface Props {
  selectedStudents: Student[]
  filteredStudents: Student[]
  totalFiltered: number
  searchParams: Record<string, any>
}

const props = defineProps<Props>()

const emit = defineEmits<{
  close: []
  success: []
}>()

// 导出范围
const exportScope = ref<'selected' | 'filtered' | 'all'>('filtered')

// 文件名
const fileName = ref(`学生列表_${new Date().toLocaleDateString().replace(/\//g, '-')}`)

// 导出状态
const exporting = ref(false)

// 拖拽相关
const dragIndex = ref<number | null>(null)

// 默认导出字段配置
const defaultFields: ExportField[] = [
  { key: 'studentNo', label: '学号', selected: true },
  { key: 'realName', label: '姓名', selected: true },
  { key: 'gender', label: '性别', selected: true, format: (v) => v === 1 ? '男' : v === 2 ? '女' : '-' },
  { key: 'identityCard', label: '身份证号', selected: true },
  { key: 'birthDate', label: '出生日期', selected: false },
  { key: 'ethnicity', label: '民族', selected: false },
  { key: 'politicalStatus', label: '政治面貌', selected: false },
  { key: 'phone', label: '联系电话', selected: true },
  { key: 'gradeName', label: '年级', selected: true },
  { key: 'className', label: '班级', selected: true },
  { key: 'majorName', label: '专业', selected: true },
  { key: 'educationLevel', label: '层次', selected: false },
  { key: 'studyLength', label: '学制', selected: false },
  { key: 'degreeType', label: '入学前学历', selected: false },
  { key: 'studentStatus', label: '学生状态', selected: true, format: (v) => {
    const map: Record<number, string> = { 1: '在校', 2: '休学', 3: '毕业', 4: '退学' }
    return map[v] || '-'
  }},
  { key: 'admissionDate', label: '入学日期', selected: true },
  { key: 'graduationDate', label: '预计毕业日期', selected: false },
  { key: 'hukouProvince', label: '户口所在省', selected: false },
  { key: 'hukouCity', label: '户口所在市', selected: false },
  { key: 'hukouDistrict', label: '户口所在区', selected: false },
  { key: 'hukouAddress', label: '户口详细地址', selected: false },
  { key: 'hukouType', label: '户口性质', selected: false },
  { key: 'homeAddress', label: '家庭住址', selected: false },
  { key: 'postalCode', label: '邮政编码', selected: false },
  { key: 'isPovertyRegistered', label: '是否建档立卡', selected: false, format: (v) => v === 1 ? '是' : '否' },
  { key: 'financialAidType', label: '资助类型', selected: false },
  { key: 'fatherName', label: '父亲姓名', selected: false },
  { key: 'fatherPhone', label: '父亲电话', selected: false },
  { key: 'fatherIdCard', label: '父亲身份证', selected: false },
  { key: 'motherName', label: '母亲姓名', selected: false },
  { key: 'motherPhone', label: '母亲电话', selected: false },
  { key: 'motherIdCard', label: '母亲身份证', selected: false },
  { key: 'guardianName', label: '监护人姓名', selected: false },
  { key: 'guardianPhone', label: '监护人电话', selected: false },
  { key: 'guardianRelation', label: '与学生关系', selected: false },
  { key: 'buildingNo', label: '楼号', selected: false },
  { key: 'roomNo', label: '房间号', selected: false },
  { key: 'bedNumber', label: '床位号', selected: false },
  { key: 'healthStatus', label: '健康状况', selected: false },
  { key: 'allergies', label: '过敏史', selected: false },
  { key: 'specialNotes', label: '备注', selected: false }
]

// 导出字段
const exportFields = ref<ExportField[]>(JSON.parse(JSON.stringify(defaultFields)))

// 选中的字段
const selectedFields = computed(() => exportFields.value.filter(f => f.selected))

// 选中字段数量
const selectedFieldCount = computed(() => selectedFields.value.length)

// 预览数据
const previewData = computed(() => {
  let data: Student[] = []
  if (exportScope.value === 'selected') {
    data = props.selectedStudents.slice(0, 5)
  } else {
    data = props.filteredStudents.slice(0, 5)
  }
  return data
})

// 全选字段
const selectAllFields = () => {
  exportFields.value.forEach(f => f.selected = true)
}

// 取消全选
const deselectAllFields = () => {
  exportFields.value.forEach(f => f.selected = false)
}

// 恢复默认
const resetFields = () => {
  exportFields.value = JSON.parse(JSON.stringify(defaultFields))
}

// 格式化字段值
const formatFieldValue = (row: any, field: ExportField) => {
  const value = row[field.key]
  if (value === null || value === undefined) return '-'
  if (field.format) return field.format(value, row)
  return String(value)
}

// 拖拽开始
const handleDragStart = (e: DragEvent, index: number) => {
  dragIndex.value = index
  if (e.dataTransfer) {
    e.dataTransfer.effectAllowed = 'move'
  }
}

// 拖拽进入
const handleDragEnter = (e: DragEvent, index: number) => {
  if (dragIndex.value === null || dragIndex.value === index) return
  const target = e.currentTarget as HTMLElement
  target.classList.add('border-blue-500', 'bg-blue-50')
}

// 拖拽离开
const handleDragLeave = (e: DragEvent) => {
  const target = e.currentTarget as HTMLElement
  target.classList.remove('border-blue-500', 'bg-blue-50')
}

// 放置
const handleDrop = (e: DragEvent, index: number) => {
  const target = e.currentTarget as HTMLElement
  target.classList.remove('border-blue-500', 'bg-blue-50')

  if (dragIndex.value === null || dragIndex.value === index) return

  const fields = [...exportFields.value]
  const [removed] = fields.splice(dragIndex.value, 1)
  fields.splice(index, 0, removed)
  exportFields.value = fields
  dragIndex.value = null
}

// 执行导出
const handleExport = async () => {
  if (selectedFieldCount.value === 0) {
    ElMessage.warning('请至少选择一个导出字段')
    return
  }

  exporting.value = true

  try {
    // 获取要导出的数据
    let dataToExport: Student[] = []

    if (exportScope.value === 'selected') {
      dataToExport = props.selectedStudents
    } else if (exportScope.value === 'filtered') {
      // 使用当前筛选的数据
      dataToExport = props.filteredStudents
    } else {
      // 导出全部 - 这里需要重新请求全部数据
      dataToExport = props.filteredStudents
    }

    if (dataToExport.length === 0) {
      ElMessage.warning('没有可导出的数据')
      exporting.value = false
      return
    }

    // 准备Excel数据
    const headers = selectedFields.value.map(f => f.label)
    const rows = dataToExport.map(row => {
      return selectedFields.value.map(field => formatFieldValue(row, field))
    })

    // 创建工作簿
    const wb = XLSX.utils.book_new()
    const ws = XLSX.utils.aoa_to_sheet([headers, ...rows])

    // 设置列宽
    const colWidths = selectedFields.value.map(f => ({ wch: Math.max(f.label.length * 2, 12) }))
    ws['!cols'] = colWidths

    XLSX.utils.book_append_sheet(wb, ws, '学生列表')

    // 下载文件
    XLSX.writeFile(wb, `${fileName.value}.xlsx`)

    ElMessage.success(`成功导出 ${dataToExport.length} 条数据`)
    emit('success')
  } catch (error: any) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exporting.value = false
  }
}

// 初始化时如果有选中的学生，默认选择"导出选中"
onMounted(() => {
  if (props.selectedStudents.length > 0) {
    exportScope.value = 'selected'
  }
})

// 监听选中学生变化
watch(() => props.selectedStudents.length, (newLen) => {
  if (newLen === 0 && exportScope.value === 'selected') {
    exportScope.value = 'filtered'
  }
})
</script>
