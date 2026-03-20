<template>
  <el-dialog
    :model-value="visible"
    title="批量导入"
    width="720px"
    :close-on-click-modal="false"
    @update:model-value="$emit('update:visible', $event)"
    @closed="handleClosed"
  >
    <!-- 导入模式选择 -->
    <div class="mb-4">
      <span class="mr-3 text-sm text-gray-600">导入类型：</span>
      <el-radio-group v-model="importMode" :disabled="step !== 'upload'" size="small">
        <el-radio value="member">批量添加成员</el-radio>
        <el-radio value="appoint">批量任命到岗位</el-radio>
      </el-radio-group>
    </div>

    <!-- Step 1: Upload -->
    <div v-if="step === 'upload'" class="space-y-4">
      <el-upload
        ref="uploadRef"
        drag
        :auto-upload="false"
        accept=".xlsx,.xls"
        :limit="1"
        :on-change="handleFileChange"
        :on-exceed="() => ElMessage.warning('请先移除已选文件')"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖拽文件到此处，或 <em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">仅支持 .xlsx / .xls 格式</div>
        </template>
      </el-upload>

      <div class="flex items-center justify-between rounded bg-gray-50 px-4 py-2.5">
        <span class="text-sm text-gray-600">
          {{ importMode === 'member' ? '模板列: 用户名/工号, 目标组织编码' : '模板列: 用户名/工号, 岗位编码, 任命类型' }}
        </span>
        <el-button type="primary" link @click="handleDownloadTemplate">
          <el-icon class="mr-1"><Download /></el-icon>下载模板
        </el-button>
      </div>
    </div>

    <!-- Step 2: Preview -->
    <div v-else-if="step === 'preview'" class="space-y-3">
      <!-- Stats -->
      <div class="flex gap-3 text-sm">
        <span class="text-gray-600">共 {{ rows.length }} 条</span>
        <span class="text-green-600">通过 {{ validRows.length }}</span>
        <span class="text-red-500">异常 {{ errorRows.length }}</span>
      </div>

      <el-table :data="rows" size="small" max-height="360" border>
        <el-table-column label="#" type="index" width="50" />
        <el-table-column label="状态" width="70">
          <template #default="{ row }">
            <el-tag :type="row._valid ? 'success' : 'danger'" size="small">
              {{ row._valid ? '通过' : '异常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名/工号" min-width="120" />
        <el-table-column v-if="importMode === 'member'" prop="orgCode" label="目标组织编码" min-width="140" />
        <el-table-column v-if="importMode === 'appoint'" prop="positionCode" label="岗位编码" min-width="130" />
        <el-table-column v-if="importMode === 'appoint'" prop="appointmentType" label="任命类型" width="100" />
        <el-table-column prop="_matchInfo" label="匹配信息" min-width="160" show-overflow-tooltip />
        <el-table-column prop="_error" label="错误" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="text-red-500">{{ row._error || '' }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Step 3: Result -->
    <div v-else-if="step === 'result'" class="py-4 text-center">
      <el-icon :size="48" :class="failCount === 0 ? 'text-green-500' : 'text-orange-500'">
        <CircleCheckFilled v-if="failCount === 0" />
        <WarningFilled v-else />
      </el-icon>
      <p class="mt-3 text-lg font-medium">
        成功 {{ successCount }} 条，失败 {{ failCount }} 条
      </p>
      <!-- Failure details -->
      <div v-if="failDetails.length > 0" class="mx-auto mt-4 max-w-lg text-left">
        <el-table :data="failDetails" size="small" max-height="240" border>
          <el-table-column prop="username" label="用户名/工号" width="140" />
          <el-table-column prop="reason" label="失败原因" />
        </el-table>
      </div>
    </div>

    <template #footer>
      <div class="flex items-center justify-between">
        <el-button v-if="step === 'preview'" @click="handleReset">重新上传</el-button>
        <span v-else />
        <div class="flex gap-2">
          <el-button @click="$emit('update:visible', false)">
            {{ step === 'result' ? '关闭' : '取消' }}
          </el-button>
          <el-button
            v-if="step === 'preview'"
            type="primary"
            :loading="importing"
            :disabled="validRows.length === 0"
            @click="handleImport"
          >
            确认导入 ({{ validRows.length }})
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Download, CircleCheckFilled, WarningFilled } from '@element-plus/icons-vue'
import * as XLSX from 'xlsx'
import { userApi } from '@/api/user'
import { getOrgUnits } from '@/api/organization'
import { positionApi, userPositionApi } from '@/api/position'
import type { OrgUnit } from '@/types'
import type { SimpleUser } from '@/types/user'
import type { Position, AppointmentType } from '@/types/position'

const props = defineProps<{
  visible: boolean
  orgUnitId: number | string
}>()

const emit = defineEmits<{
  'update:visible': [val: boolean]
  'imported': []
}>()

type ImportMode = 'member' | 'appoint'
type Step = 'upload' | 'preview' | 'result'

interface ParsedRow {
  username: string
  orgCode?: string
  positionCode?: string
  appointmentType?: string
  // resolved
  _userId?: number | string
  _orgUnitId?: number | string
  _positionId?: number | string
  _valid: boolean
  _error: string
  _matchInfo: string
}

const importMode = ref<ImportMode>('member')
const step = ref<Step>('upload')
const rows = ref<ParsedRow[]>([])
const importing = ref(false)
const uploadRef = ref()

// Result
const successCount = ref(0)
const failCount = ref(0)
const failDetails = ref<{ username: string; reason: string }[]>([])

// Caches
let userCache: SimpleUser[] = []
let orgCache: OrgUnit[] = []
let positionCache: Position[] = []

const validRows = computed(() => rows.value.filter(r => r._valid))
const errorRows = computed(() => rows.value.filter(r => !r._valid))

const validAppointmentTypes = ['FORMAL', 'ACTING', 'CONCURRENT', 'PROBATION']
const appointmentTypeLabels: Record<string, string> = {
  FORMAL: '正式任命',
  ACTING: '代理',
  CONCURRENT: '兼任',
  PROBATION: '试用',
}

watch(() => props.visible, (v) => {
  if (v) handleReset()
})

// Download template
function handleDownloadTemplate() {
  const wb = XLSX.utils.book_new()
  let headers: string[]
  let example: (string | undefined)[]

  if (importMode.value === 'member') {
    headers = ['用户名/工号', '目标组织编码']
    example = ['zhangsan', 'ORG001']
  } else {
    headers = ['用户名/工号', '岗位编码', '任命类型']
    example = ['zhangsan', 'POS001', 'FORMAL']
  }

  const ws = XLSX.utils.aoa_to_sheet([headers, example])
  // Set column widths
  ws['!cols'] = headers.map(() => ({ wch: 20 }))
  XLSX.utils.book_append_sheet(wb, ws, '导入数据')

  // If appoint mode, add a reference sheet for appointment types
  if (importMode.value === 'appoint') {
    const refData = [
      ['任命类型编码', '说明'],
      ['FORMAL', '正式任命'],
      ['ACTING', '代理'],
      ['CONCURRENT', '兼任'],
      ['PROBATION', '试用'],
    ]
    const refWs = XLSX.utils.aoa_to_sheet(refData)
    refWs['!cols'] = [{ wch: 18 }, { wch: 18 }]
    XLSX.utils.book_append_sheet(wb, refWs, '任命类型说明')
  }

  const fileName = importMode.value === 'member' ? '批量添加成员模板.xlsx' : '批量任命岗位模板.xlsx'
  XLSX.writeFile(wb, fileName)
  ElMessage.success('模板已下载')
}

// File selected
async function handleFileChange(uploadFile: any) {
  const raw = uploadFile.raw as File
  if (!raw) return
  if (!/\.(xlsx|xls)$/i.test(raw.name)) {
    ElMessage.error('请选择 Excel 文件 (.xlsx/.xls)')
    uploadRef.value?.clearFiles()
    return
  }

  try {
    ElMessage.info('正在解析文件...')
    const buf = await raw.arrayBuffer()
    const wb = XLSX.read(buf, { type: 'array' })
    const ws = wb.Sheets[wb.SheetNames[0]]
    const json = XLSX.utils.sheet_to_json<any>(ws, { header: 1 }) as any[][]

    if (json.length < 2) {
      ElMessage.warning('文件内容为空（至少需要表头 + 1行数据）')
      uploadRef.value?.clearFiles()
      return
    }

    // Parse rows (skip header)
    const dataRows = json.slice(1).filter(r => r.some((c: any) => c !== undefined && c !== null && String(c).trim()))

    const parsed: ParsedRow[] = dataRows.map(r => {
      if (importMode.value === 'member') {
        return {
          username: String(r[0] ?? '').trim(),
          orgCode: String(r[1] ?? '').trim(),
          _valid: false,
          _error: '',
          _matchInfo: '',
        }
      } else {
        return {
          username: String(r[0] ?? '').trim(),
          positionCode: String(r[1] ?? '').trim(),
          appointmentType: String(r[2] ?? '').trim().toUpperCase(),
          _valid: false,
          _error: '',
          _matchInfo: '',
        }
      }
    })

    // Load reference data & validate
    await loadCaches()
    validateRows(parsed)
    rows.value = parsed
    step.value = 'preview'
  } catch (e: any) {
    ElMessage.error('文件解析失败: ' + (e.message || e))
    uploadRef.value?.clearFiles()
  }
}

async function loadCaches() {
  const [users, orgs] = await Promise.all([
    userApi.getSimpleList(),
    getOrgUnits(),
  ])
  userCache = users
  orgCache = orgs

  // For appoint mode, load all positions visible to current org
  if (importMode.value === 'appoint') {
    // Collect unique position codes from parsed rows to preload
    // We load positions from orgUnitId as a starting point
    try {
      positionCache = await positionApi.getByOrgUnit(props.orgUnitId)
    } catch {
      positionCache = []
    }
  }
}

function validateRows(parsed: ParsedRow[]) {
  // Build lookup maps
  const userByUsername = new Map<string, SimpleUser>()
  const userByEmployeeNo = new Map<string, SimpleUser>()
  for (const u of userCache) {
    userByUsername.set(u.username.toLowerCase(), u)
  }
  // SimpleUser may not have employeeNo, also check username as fallback
  const orgByCode = new Map<string, OrgUnit>()
  for (const o of orgCache) {
    orgByCode.set(o.unitCode.toLowerCase(), o)
  }
  const posByCode = new Map<string, Position>()
  for (const p of positionCache) {
    posByCode.set(p.positionCode.toLowerCase(), p)
  }

  for (const row of parsed) {
    const errors: string[] = []
    const infos: string[] = []

    // Validate username
    if (!row.username) {
      errors.push('用户名/工号为空')
    } else {
      const key = row.username.toLowerCase()
      const user = userByUsername.get(key)
      if (user) {
        row._userId = user.id
        infos.push(`用户: ${user.realName}`)
      } else {
        errors.push(`用户 "${row.username}" 不存在`)
      }
    }

    if (importMode.value === 'member') {
      if (!row.orgCode) {
        errors.push('目标组织编码为空')
      } else {
        const org = orgByCode.get(row.orgCode.toLowerCase())
        if (org) {
          row._orgUnitId = org.id
          infos.push(`组织: ${org.unitName}`)
        } else {
          errors.push(`组织编码 "${row.orgCode}" 不存在`)
        }
      }
    } else {
      if (!row.positionCode) {
        errors.push('岗位编码为空')
      } else {
        const pos = posByCode.get(row.positionCode.toLowerCase())
        if (pos) {
          row._positionId = pos.id
          infos.push(`岗位: ${pos.positionName}`)
        } else {
          errors.push(`岗位编码 "${row.positionCode}" 不存在`)
        }
      }
      if (!row.appointmentType) {
        errors.push('任命类型为空')
      } else if (!validAppointmentTypes.includes(row.appointmentType)) {
        errors.push(`任命类型 "${row.appointmentType}" 无效 (可选: ${validAppointmentTypes.join('/')})`)
      } else {
        infos.push(`类型: ${appointmentTypeLabels[row.appointmentType] || row.appointmentType}`)
      }
    }

    row._error = errors.join('; ')
    row._matchInfo = infos.join(' | ')
    row._valid = errors.length === 0
  }
}

// Execute import
async function handleImport() {
  const toImport = validRows.value
  if (toImport.length === 0) return

  importing.value = true
  successCount.value = 0
  failCount.value = 0
  failDetails.value = []

  const today = new Date().toISOString().slice(0, 10)

  for (const row of toImport) {
    try {
      if (importMode.value === 'member') {
        const targetOrg = row._orgUnitId ?? props.orgUnitId
        await userPositionApi.addMember(targetOrg, row._userId!)
      } else {
        await userPositionApi.appoint({
          userId: row._userId!,
          positionId: row._positionId!,
          appointmentType: row.appointmentType as AppointmentType,
          startDate: today,
        })
      }
      successCount.value++
    } catch (e: any) {
      failCount.value++
      const reason = e?.response?.data?.message || e?.message || '未知错误'
      failDetails.value.push({ username: row.username, reason })
    }
  }

  importing.value = false
  step.value = 'result'

  if (failCount.value === 0) {
    ElMessage.success(`全部 ${successCount.value} 条导入成功`)
  } else {
    ElMessage.warning(`成功 ${successCount.value} 条，失败 ${failCount.value} 条`)
  }

  emit('imported')
}

function handleReset() {
  step.value = 'upload'
  rows.value = []
  successCount.value = 0
  failCount.value = 0
  failDetails.value = []
  uploadRef.value?.clearFiles()
}

function handleClosed() {
  handleReset()
}
</script>
