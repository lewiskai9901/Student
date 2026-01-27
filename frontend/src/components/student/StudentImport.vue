<template>
  <div class="p-6">
    <!-- 步骤指示器 -->
    <div class="mb-6">
      <div class="flex items-center justify-center">
        <div v-for="(step, index) in steps" :key="index" class="flex items-center">
          <div class="flex items-center">
            <div
              :class="[
                'flex h-8 w-8 items-center justify-center rounded-full text-sm font-medium transition-colors',
                currentStep > index
                  ? 'bg-green-500 text-white'
                  : currentStep === index
                  ? 'bg-blue-600 text-white'
                  : 'bg-gray-200 text-gray-500'
              ]"
            >
              <Check v-if="currentStep > index" class="h-5 w-5" />
              <span v-else>{{ index + 1 }}</span>
            </div>
            <span
              :class="[
                'ml-2 text-sm font-medium',
                currentStep >= index ? 'text-gray-900' : 'text-gray-500'
              ]"
            >
              {{ step }}
            </span>
          </div>
          <div
            v-if="index < steps.length - 1"
            :class="[
              'mx-4 h-0.5 w-16',
              currentStep > index ? 'bg-green-500' : 'bg-gray-200'
            ]"
          ></div>
        </div>
      </div>
    </div>

    <!-- 步骤1: 上传文件 -->
    <div v-if="currentStep === 0" class="space-y-4">
      <div class="rounded-lg border-2 border-dashed border-gray-300 p-8 text-center">
        <input
          ref="fileInputRef"
          type="file"
          accept=".xlsx,.xls"
          class="hidden"
          @change="handleFileSelect"
        />
        <Upload class="mx-auto h-12 w-12 text-gray-400" />
        <p class="mt-4 text-sm text-gray-600">
          点击或拖拽Excel文件到此处上传
        </p>
        <p class="mt-1 text-xs text-gray-400">
          支持 .xlsx, .xls 格式，文件大小不超过 10MB
        </p>
        <button
          class="mt-4 rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
          @click="fileInputRef?.click()"
        >
          选择文件
        </button>
      </div>

      <!-- 下载模板 -->
      <div class="flex items-center justify-between rounded-lg bg-blue-50 p-4">
        <div class="flex items-center gap-3">
          <FileSpreadsheet class="h-8 w-8 text-blue-600" />
          <div>
            <p class="text-sm font-medium text-gray-900">下载导入模板</p>
            <p class="text-xs text-gray-500">
              包含所有字段说明和示例数据，班级等字段提供下拉选择
            </p>
          </div>
        </div>
        <button
          class="rounded-md border border-blue-600 px-3 py-1.5 text-sm font-medium text-blue-600 hover:bg-blue-100"
          :disabled="downloadingTemplate"
          @click="handleDownloadTemplate"
        >
          <Loader2 v-if="downloadingTemplate" class="h-4 w-4 animate-spin" />
          <span v-else>下载模板</span>
        </button>
      </div>

      <!-- 导入说明 -->
      <div class="rounded-lg border border-gray-200 p-4">
        <h4 class="mb-2 text-sm font-medium text-gray-900">导入说明</h4>
        <ul class="space-y-1 text-xs text-gray-600">
          <li class="flex items-start gap-2">
            <span class="mt-0.5 h-1.5 w-1.5 flex-shrink-0 rounded-full bg-gray-400"></span>
            带 * 号的字段为必填项（姓名、学号、身份证号、出生年月、性别、民族、政治面貌、联系方式、招生年度、班级）
          </li>
          <li class="flex items-start gap-2">
            <span class="mt-0.5 h-1.5 w-1.5 flex-shrink-0 rounded-full bg-gray-400"></span>
            班级请从下拉列表中选择，或输入准确的班级名称
          </li>
          <li class="flex items-start gap-2">
            <span class="mt-0.5 h-1.5 w-1.5 flex-shrink-0 rounded-full bg-gray-400"></span>
            学号不能重复，系统会自动校验
          </li>
          <li class="flex items-start gap-2">
            <span class="mt-0.5 h-1.5 w-1.5 flex-shrink-0 rounded-full bg-gray-400"></span>
            日期格式为 YYYY-MM-DD 或 YYYY/MM/DD，如 2024-09-01
          </li>
          <li class="flex items-start gap-2">
            <span class="mt-0.5 h-1.5 w-1.5 flex-shrink-0 rounded-full bg-gray-400"></span>
            民族需填写完整名称（如：汉族、满族）
          </li>
          <li class="flex items-start gap-2">
            <span class="mt-0.5 h-1.5 w-1.5 flex-shrink-0 rounded-full bg-gray-400"></span>
            政治面貌可选：群众、共青团员、中共党员、中共预备党员、民主党派、无党派人士
          </li>
        </ul>
      </div>
    </div>

    <!-- 步骤2: 数据预览与校验 -->
    <div v-if="currentStep === 1" class="space-y-4">
      <!-- 统计信息 -->
      <div class="flex gap-4">
        <div class="flex-1 rounded-lg bg-gray-50 p-3 text-center">
          <p class="text-2xl font-semibold text-gray-900">{{ previewData.length }}</p>
          <p class="text-xs text-gray-500">总记录数</p>
        </div>
        <div class="flex-1 rounded-lg bg-green-50 p-3 text-center">
          <p class="text-2xl font-semibold text-green-600">{{ validCount }}</p>
          <p class="text-xs text-gray-500">校验通过</p>
        </div>
        <div class="flex-1 rounded-lg bg-red-50 p-3 text-center">
          <p class="text-2xl font-semibold text-red-600">{{ errorCount }}</p>
          <p class="text-xs text-gray-500">校验失败</p>
        </div>
      </div>

      <!-- 筛选 -->
      <div class="flex items-center gap-2">
        <span class="text-sm text-gray-600">显示:</span>
        <button
          :class="[
            'rounded px-3 py-1 text-sm',
            filterType === 'all' ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
          ]"
          @click="filterType = 'all'"
        >
          全部
        </button>
        <button
          :class="[
            'rounded px-3 py-1 text-sm',
            filterType === 'valid' ? 'bg-green-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
          ]"
          @click="filterType = 'valid'"
        >
          通过
        </button>
        <button
          :class="[
            'rounded px-3 py-1 text-sm',
            filterType === 'error' ? 'bg-red-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
          ]"
          @click="filterType = 'error'"
        >
          失败
        </button>
      </div>

      <!-- 数据表格 -->
      <div class="max-h-80 overflow-auto rounded-lg border border-gray-200">
        <table class="w-full text-sm">
          <thead class="sticky top-0 bg-gray-50">
            <tr>
              <th class="whitespace-nowrap px-3 py-2 text-left text-xs font-medium text-gray-500">行号</th>
              <th class="whitespace-nowrap px-3 py-2 text-left text-xs font-medium text-gray-500">状态</th>
              <th class="whitespace-nowrap px-3 py-2 text-left text-xs font-medium text-gray-500">姓名*</th>
              <th class="whitespace-nowrap px-3 py-2 text-left text-xs font-medium text-gray-500">学号*</th>
              <th class="whitespace-nowrap px-3 py-2 text-left text-xs font-medium text-gray-500">性别*</th>
              <th class="whitespace-nowrap px-3 py-2 text-left text-xs font-medium text-gray-500">身份证号*</th>
              <th class="whitespace-nowrap px-3 py-2 text-left text-xs font-medium text-gray-500">民族*</th>
              <th class="whitespace-nowrap px-3 py-2 text-left text-xs font-medium text-gray-500">班级*</th>
              <th class="whitespace-nowrap px-3 py-2 text-left text-xs font-medium text-gray-500">联系电话*</th>
              <th class="whitespace-nowrap px-3 py-2 text-left text-xs font-medium text-gray-500">错误信息</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-100">
            <tr
              v-for="(row, index) in filteredData"
              :key="index"
              :class="row.hasError ? 'bg-red-50' : ''"
            >
              <td class="whitespace-nowrap px-3 py-2 text-gray-500">{{ row.rowNum }}</td>
              <td class="whitespace-nowrap px-3 py-2">
                <span
                  :class="[
                    'inline-flex items-center rounded px-1.5 py-0.5 text-xs font-medium',
                    row.hasError ? 'bg-red-100 text-red-700' : 'bg-green-100 text-green-700'
                  ]"
                >
                  {{ row.hasError ? '失败' : '通过' }}
                </span>
              </td>
              <td class="whitespace-nowrap px-3 py-2">{{ row.realName }}</td>
              <td class="whitespace-nowrap px-3 py-2">{{ row.studentNo }}</td>
              <td class="whitespace-nowrap px-3 py-2">{{ row.gender }}</td>
              <td class="whitespace-nowrap px-3 py-2">{{ maskIdCard(row.identityCard) }}</td>
              <td class="whitespace-nowrap px-3 py-2">{{ row.ethnicity || '-' }}</td>
              <td class="whitespace-nowrap px-3 py-2">{{ row.className }}</td>
              <td class="whitespace-nowrap px-3 py-2">{{ row.phone || '-' }}</td>
              <td class="max-w-xs truncate px-3 py-2 text-red-600" :title="row.errorMessage">
                {{ row.errorMessage || '-' }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 步骤3: 导入结果 -->
    <div v-if="currentStep === 2" class="space-y-4">
      <div class="flex flex-col items-center justify-center py-8">
        <div
          :class="[
            'flex h-16 w-16 items-center justify-center rounded-full',
            importResult.failCount === 0 ? 'bg-green-100' : 'bg-amber-100'
          ]"
        >
          <CheckCircle v-if="importResult.failCount === 0" class="h-10 w-10 text-green-600" />
          <AlertCircle v-else class="h-10 w-10 text-amber-600" />
        </div>
        <h3 class="mt-4 text-lg font-medium text-gray-900">
          {{ importResult.failCount === 0 ? '导入完成' : '部分导入成功' }}
        </h3>
        <p class="mt-1 text-sm text-gray-500">
          成功导入 {{ importResult.successCount }} 条，失败 {{ importResult.failCount }} 条
        </p>
      </div>

      <!-- 失败数据 -->
      <div v-if="importResult.failCount > 0" class="space-y-3">
        <div class="flex items-center justify-between">
          <h4 class="text-sm font-medium text-gray-900">失败记录</h4>
          <button
            class="rounded-md border border-gray-300 px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50"
            @click="handleExportFailed"
          >
            <Download class="mr-1.5 inline-block h-4 w-4" />
            导出失败数据
          </button>
        </div>
        <div class="max-h-48 overflow-auto rounded-lg border border-gray-200">
          <table class="w-full text-sm">
            <thead class="sticky top-0 bg-gray-50">
              <tr>
                <th class="whitespace-nowrap px-3 py-2 text-left text-xs font-medium text-gray-500">行号</th>
                <th class="whitespace-nowrap px-3 py-2 text-left text-xs font-medium text-gray-500">学号</th>
                <th class="whitespace-nowrap px-3 py-2 text-left text-xs font-medium text-gray-500">姓名</th>
                <th class="whitespace-nowrap px-3 py-2 text-left text-xs font-medium text-gray-500">错误原因</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-100">
              <tr v-for="(row, index) in importResult.failedRows" :key="index" class="bg-red-50">
                <td class="whitespace-nowrap px-3 py-2 text-gray-500">{{ row.rowNum }}</td>
                <td class="whitespace-nowrap px-3 py-2">{{ row.studentNo }}</td>
                <td class="whitespace-nowrap px-3 py-2">{{ row.realName }}</td>
                <td class="px-3 py-2 text-red-600">{{ row.errorMessage }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- 底部按钮 -->
    <div class="mt-6 flex items-center justify-between border-t border-gray-200 pt-4">
      <button
        v-if="currentStep > 0 && currentStep < 2"
        class="rounded-md border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
        @click="handlePrevStep"
      >
        上一步
      </button>
      <div v-else></div>
      <div class="flex gap-2">
        <button
          class="rounded-md border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
          @click="$emit('close')"
        >
          {{ currentStep === 2 ? '关闭' : '取消' }}
        </button>
        <button
          v-if="currentStep === 1"
          :disabled="validCount === 0 || importing"
          class="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
          @click="handleConfirmImport"
        >
          <Loader2 v-if="importing" class="mr-1.5 inline-block h-4 w-4 animate-spin" />
          确认导入 ({{ validCount }} 条)
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Upload,
  FileSpreadsheet,
  Check,
  CheckCircle,
  AlertCircle,
  Download,
  Loader2
} from 'lucide-vue-next'
import { downloadImportTemplate, importStudents } from '@/api/student'
import { getClassList } from '@/api/organization'
import * as XLSX from 'xlsx'

const emit = defineEmits(['close', 'success'])

const steps = ['上传文件', '数据预览', '导入结果']
const currentStep = ref(0)

const fileInputRef = ref<HTMLInputElement | null>(null)
const selectedFile = ref<File | null>(null)
const downloadingTemplate = ref(false)
const importing = ref(false)

// 预览数据
const previewData = ref<any[]>([])
const filterType = ref<'all' | 'valid' | 'error'>('all')

// 班级列表（用于校验）
const classList = ref<any[]>([])

// 导入结果
const importResult = ref({
  successCount: 0,
  failCount: 0,
  failedRows: [] as any[]
})

// 民族列表
const ethnicities = [
  '汉族', '蒙古族', '回族', '藏族', '维吾尔族', '苗族', '彝族', '壮族', '布依族', '朝鲜族',
  '满族', '侗族', '瑶族', '白族', '土家族', '哈尼族', '哈萨克族', '傣族', '黎族', '傈僳族',
  '佤族', '畲族', '高山族', '拉祜族', '水族', '东乡族', '纳西族', '景颇族', '柯尔克孜族',
  '土族', '达斡尔族', '仫佬族', '羌族', '布朗族', '撒拉族', '毛南族', '仡佬族', '锡伯族',
  '阿昌族', '普米族', '塔吉克族', '怒族', '乌孜别克族', '俄罗斯族', '鄂温克族', '德昂族',
  '保安族', '裕固族', '京族', '塔塔尔族', '独龙族', '鄂伦春族', '赫哲族', '门巴族', '珞巴族', '基诺族'
]

// 政治面貌列表
const politicalStatuses = ['群众', '共青团员', '中共党员', '中共预备党员', '民主党派', '无党派人士']

// 计算属性
const validCount = computed(() => previewData.value.filter(r => !r.hasError).length)
const errorCount = computed(() => previewData.value.filter(r => r.hasError).length)

const filteredData = computed(() => {
  if (filterType.value === 'all') return previewData.value
  if (filterType.value === 'valid') return previewData.value.filter(r => !r.hasError)
  return previewData.value.filter(r => r.hasError)
})

// 脱敏身份证号
const maskIdCard = (idCard?: string) => {
  if (!idCard) return '-'
  if (idCard.length >= 14) {
    return idCard.replace(/(\d{6})\d{8}(\d{4})/, '$1****$2')
  }
  return idCard
}

// 下载模板
const handleDownloadTemplate = async () => {
  try {
    downloadingTemplate.value = true
    const response = await downloadImportTemplate()
    // 从 axios 响应中获取 blob 数据
    const blob = response.data instanceof Blob
      ? response.data
      : new Blob([response.data || response], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '学生信息导入模板.xlsx'
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('模板下载成功')
  } catch (error: any) {
    ElMessage.error(error.message || '模板下载失败')
  } finally {
    downloadingTemplate.value = false
  }
}

// 加载班级列表
const loadClassList = async () => {
  try {
    const response = await getClassList({ pageSize: 1000 })
    classList.value = response.records || []
  } catch (error) {
    console.error('加载班级列表失败:', error)
  }
}

// 选择文件
const handleFileSelect = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  // 检查文件类型
  if (!file.name.endsWith('.xlsx') && !file.name.endsWith('.xls')) {
    ElMessage.error('请上传 Excel 文件 (.xlsx 或 .xls)')
    return
  }

  // 检查文件大小 (10MB)
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 10MB')
    return
  }

  selectedFile.value = file
  await loadClassList()
  await parseExcelFile(file)
  currentStep.value = 1
}

// Excel列名映射
const columnMapping: Record<string, string> = {
  '姓名': 'realName',
  '*姓名': 'realName',
  '学号': 'studentNo',
  '*学号': 'studentNo',
  '证件类型': 'idCardType',
  '身份证号': 'identityCard',
  '*身份证号': 'identityCard',
  '出生年月': 'birthDate',
  '*出生年月': 'birthDate',
  '性别': 'gender',
  '*性别': 'gender',
  '民族': 'ethnicity',
  '*民族': 'ethnicity',
  '政治面貌': 'politicalStatus',
  '*政治面貌': 'politicalStatus',
  '联系方式': 'phone',
  '*联系方式': 'phone',
  '招生年度': 'admissionYear',
  '*招生年度': 'admissionYear',
  '所在系': 'department',
  '*所在系': 'department',
  '所学专业': 'majorName',
  '专业代码': 'majorCode',
  '班级': 'className',
  '*班级': 'className',
  '班主任': 'teacherName',
  '*班主任': 'teacherName',
  '专业级别（层次）': 'educationLevel',
  '*专业级别（层次）': 'educationLevel',
  '学制': 'studyLength',
  '*学制': 'studyLength',
  '学历': 'degreeType',
  '*学历': 'degreeType',
  '户口所在地-省': 'hukouProvince',
  '*户口所在地-省': 'hukouProvince',
  '户口所在地-市': 'hukouCity',
  '*户口所在地-市': 'hukouCity',
  '户口所在地-区': 'hukouDistrict',
  '*户口所在地-区': 'hukouDistrict',
  '户口-地址': 'hukouAddress',
  '*户口-地址': 'hukouAddress',
  '户口性质': 'hukouType',
  '*户口性质': 'hukouType',
  '邮政编码': 'postalCode',
  '*邮政编码': 'postalCode',
  '是否建档立卡': 'isPovertyRegistered',
  '*是否建档立卡': 'isPovertyRegistered',
  '资助申请类型': 'financialAidType',
  '*资助申请类型': 'financialAidType',
  '父亲姓名': 'fatherName',
  '*父亲姓名': 'fatherName',
  '父亲身份证号': 'fatherIdCard',
  '*父亲身份证号': 'fatherIdCard',
  '母亲姓名': 'motherName',
  '*母亲姓名': 'motherName',
  '母亲身份证号': 'motherIdCard',
  '*母亲身份证号': 'motherIdCard',
  '其他监护人姓名': 'guardianName',
  '*其他监护人姓名': 'guardianName',
  '其他监护人身份证号': 'guardianIdCard',
  '*其他监护人身份证号': 'guardianIdCard',
  '备注': 'remark',
  '家庭住址': 'homeAddress',
  '*家庭住址': 'homeAddress'
}

// 解析Excel文件
const parseExcelFile = async (file: File) => {
  try {
    const data = await file.arrayBuffer()
    const workbook = XLSX.read(data, { type: 'array' })
    const sheetName = workbook.SheetNames[0]
    const worksheet = workbook.Sheets[sheetName]
    const jsonData = XLSX.utils.sheet_to_json(worksheet, { header: 1 }) as any[][]

    // 获取表头
    const headers = jsonData[0] as string[]
    const headerMap: Record<number, string> = {}

    headers.forEach((header, index) => {
      const cleanHeader = header?.toString().trim()
      if (cleanHeader && columnMapping[cleanHeader]) {
        headerMap[index] = columnMapping[cleanHeader]
      }
    })

    // 跳过表头行
    const rows = jsonData.slice(1)

    // 用于检测学号重复
    const studentNoSet = new Set<string>()
    const duplicateStudentNos = new Set<string>()

    // 第一遍：收集所有学号，找出重复的
    rows.forEach(row => {
      const studentNoIndex = Object.entries(headerMap).find(([, v]) => v === 'studentNo')?.[0]
      if (studentNoIndex !== undefined) {
        const studentNo = row[parseInt(studentNoIndex)]?.toString().trim()
        if (studentNo) {
          if (studentNoSet.has(studentNo)) {
            duplicateStudentNos.add(studentNo)
          } else {
            studentNoSet.add(studentNo)
          }
        }
      }
    })

    // 第二遍：解析和校验
    previewData.value = rows.map((row, index) => {
      const rowData: any = { rowNum: index + 2 }

      // 解析每一列
      Object.entries(headerMap).forEach(([colIndex, fieldName]) => {
        let value = row[parseInt(colIndex)]
        if (value !== undefined && value !== null) {
          value = value.toString().trim()
          rowData[fieldName] = value
        }
      })

      // 校验
      const errors: string[] = []

      // 必填字段校验
      if (!rowData.realName) {
        errors.push('姓名不能为空')
      }

      if (!rowData.studentNo) {
        errors.push('学号不能为空')
      } else if (duplicateStudentNos.has(rowData.studentNo)) {
        errors.push('学号在文件中重复')
      }

      if (!rowData.gender) {
        errors.push('性别不能为空')
      } else if (rowData.gender !== '男' && rowData.gender !== '女') {
        errors.push('性别必须为"男"或"女"')
      }

      if (!rowData.identityCard) {
        errors.push('身份证号不能为空')
      } else if (!/^[1-9]\d{5}(18|19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[\dXx]$/.test(rowData.identityCard)) {
        errors.push('身份证号格式不正确')
      }

      if (!rowData.ethnicity) {
        errors.push('民族不能为空')
      } else if (!ethnicities.includes(rowData.ethnicity)) {
        errors.push(`民族"${rowData.ethnicity}"不在有效民族列表中`)
      }

      if (!rowData.politicalStatus) {
        errors.push('政治面貌不能为空')
      } else if (!politicalStatuses.includes(rowData.politicalStatus)) {
        errors.push(`政治面貌"${rowData.politicalStatus}"不在有效列表中`)
      }

      if (!rowData.phone) {
        errors.push('联系方式不能为空')
      } else if (!/^1[3-9]\d{9}$/.test(rowData.phone)) {
        errors.push('手机号格式不正确')
      }

      if (!rowData.className) {
        errors.push('班级不能为空')
      } else {
        // 检查班级是否存在
        const classExists = classList.value.some(c => c.className === rowData.className)
        if (!classExists) {
          errors.push(`班级"${rowData.className}"不存在`)
        } else {
          rowData.classId = classList.value.find(c => c.className === rowData.className)?.id
        }
      }

      // 处理招生年度为入学日期
      if (rowData.admissionYear) {
        const year = parseInt(rowData.admissionYear)
        if (!isNaN(year) && year >= 2000 && year <= 2100) {
          rowData.admissionDate = `${year}-09-01`
        }
      }

      // 处理是否建档立卡
      if (rowData.isPovertyRegistered) {
        rowData.isPovertyRegistered = rowData.isPovertyRegistered === '是' ? 1 : 0
      }

      rowData.hasError = errors.length > 0
      rowData.errorMessage = errors.join('；')

      return rowData
    }).filter(row => row.realName || row.studentNo) // 过滤空行
  } catch (error) {
    console.error('解析Excel文件失败:', error)
    ElMessage.error('文件解析失败，请检查文件格式')
  }
}

// 上一步
const handlePrevStep = () => {
  if (currentStep.value > 0) {
    currentStep.value--
    if (currentStep.value === 0) {
      previewData.value = []
      selectedFile.value = null
      if (fileInputRef.value) {
        fileInputRef.value.value = ''
      }
    }
  }
}

// 确认导入
const handleConfirmImport = async () => {
  if (!selectedFile.value) return

  try {
    importing.value = true
    const result = await importStudents(selectedFile.value)

    // 解析结果
    const successMatch = result.match(/成功导入\s*(\d+)\s*条/)
    const failMatch = result.match(/失败\s*(\d+)\s*条/)

    importResult.value.successCount = successMatch ? parseInt(successMatch[1]) : validCount.value
    importResult.value.failCount = failMatch ? parseInt(failMatch[1]) : 0

    // 提取失败行信息
    if (importResult.value.failCount > 0) {
      const lines = result.split('\n').slice(1)
      importResult.value.failedRows = lines
        .filter((line: string) => line.includes('行'))
        .map((line: string) => {
          const rowMatch = line.match(/第(\d+)行[：:](.*)/)
          if (rowMatch) {
            const rowNum = parseInt(rowMatch[1])
            const originalRow = previewData.value.find(r => r.rowNum === rowNum)
            return {
              rowNum,
              studentNo: originalRow?.studentNo || '',
              realName: originalRow?.realName || '',
              errorMessage: rowMatch[2].trim()
            }
          }
          return null
        })
        .filter(Boolean)
    }

    currentStep.value = 2

    if (importResult.value.failCount === 0) {
      ElMessage.success('导入成功')
    } else {
      ElMessage.warning(`导入完成，${importResult.value.failCount} 条记录失败`)
    }

    emit('success')
  } catch (error: any) {
    ElMessage.error(error.message || '导入失败')
  } finally {
    importing.value = false
  }
}

// 导出失败数据
const handleExportFailed = () => {
  const failedRows = previewData.value.filter(r => r.hasError)
  if (failedRows.length === 0) return

  // 创建工作簿
  const wb = XLSX.utils.book_new()
  const wsData = [
    ['*姓名', '学号', '证件类型', '*身份证号', '*出生年月', '*性别', '*民族', '*政治面貌', '*联系方式',
     '*招生年度', '*所在系', '所学专业', '专业代码', '班级', '*班主任', '*专业级别（层次）', '*学制', '*学历',
     '*户口所在地-省', '*户口所在地-市', '*户口所在地-区', '*户口-地址', '*户口性质', '*邮政编码',
     '*是否建档立卡', '*资助申请类型', '*父亲姓名', '*父亲身份证号', '*母亲姓名', '*母亲身份证号',
     '*其他监护人姓名', '*其他监护人身份证号', '备注', '错误原因'],
    ...failedRows.map(row => [
      row.realName,
      row.studentNo,
      row.idCardType || '身份证',
      row.identityCard,
      row.birthDate,
      row.gender,
      row.ethnicity,
      row.politicalStatus,
      row.phone,
      row.admissionYear,
      row.department,
      row.majorName,
      row.majorCode,
      row.className,
      row.teacherName,
      row.educationLevel,
      row.studyLength,
      row.degreeType,
      row.hukouProvince,
      row.hukouCity,
      row.hukouDistrict,
      row.hukouAddress,
      row.hukouType,
      row.postalCode,
      row.isPovertyRegistered === 1 ? '是' : '否',
      row.financialAidType,
      row.fatherName,
      row.fatherIdCard,
      row.motherName,
      row.motherIdCard,
      row.guardianName,
      row.guardianIdCard,
      row.remark,
      row.errorMessage
    ])
  ]
  const ws = XLSX.utils.aoa_to_sheet(wsData)
  XLSX.utils.book_append_sheet(wb, ws, '导入失败数据')

  // 下载
  XLSX.writeFile(wb, '学生导入失败数据.xlsx')
  ElMessage.success('导出成功')
}
</script>
