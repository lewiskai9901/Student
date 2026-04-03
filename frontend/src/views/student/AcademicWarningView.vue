<template>
  <div class="flex h-full flex-col bg-gray-50">
    <!-- Header -->
    <div class="flex items-center justify-between border-b border-gray-200 bg-white px-6 py-4">
      <div>
        <h1 class="text-lg font-semibold text-gray-900">学业预警</h1>
        <p class="mt-0.5 text-sm text-gray-500">预警规则配置、预警扫描与处理</p>
      </div>
      <div class="flex items-center gap-2">
        <el-select
          v-model="currentSemesterId"
          placeholder="选择学期"
          class="w-44"
          @change="handleSemesterChange"
        >
          <el-option v-for="sem in semesters" :key="sem.id" :value="sem.id" :label="sem.name" />
        </el-select>
      </div>
    </div>

    <!-- Tabs -->
    <div class="border-b border-gray-200 bg-white px-6">
      <div class="flex gap-6">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          class="relative pb-2.5 pt-3 text-sm font-medium transition-colors"
          :class="activeTab === tab.key
            ? 'text-blue-600 after:absolute after:bottom-0 after:left-0 after:right-0 after:h-0.5 after:bg-blue-600'
            : 'text-gray-500 hover:text-gray-700'"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
        </button>
      </div>
    </div>

    <!-- Tab: 预警记录 -->
    <template v-if="activeTab === 'records'">
      <!-- Stats Bar -->
      <div class="flex items-center gap-4 border-b border-gray-200 bg-white px-6 py-2.5">
        <span class="text-sm text-gray-500">红色 <span class="font-semibold" style="color:#C45656">{{ stats.red }}</span></span>
        <div class="h-3 w-px bg-gray-200" />
        <span class="text-sm text-gray-500">橙色 <span class="font-semibold" style="color:#F56C6C">{{ stats.orange }}</span></span>
        <div class="h-3 w-px bg-gray-200" />
        <span class="text-sm text-gray-500">黄色 <span class="font-semibold" style="color:#E6A23C">{{ stats.yellow }}</span></span>
        <div class="h-3 w-px bg-gray-200" />
        <span class="text-sm text-gray-500">未处理 <span class="font-semibold text-red-600">{{ stats.pending }}</span></span>
        <div class="h-3 w-px bg-gray-200" />
        <span class="text-sm text-gray-500">已干预 <span class="font-semibold text-blue-600">{{ stats.intervened }}</span></span>
        <div class="flex-1" />
        <button
          class="inline-flex items-center gap-1.5 rounded-lg bg-red-600 px-3 py-1.5 text-sm font-medium text-white transition-colors hover:bg-red-700"
          :disabled="!currentSemesterId || scanning"
          @click="handleScan"
        >
          <RefreshCw v-if="!scanning" class="h-3.5 w-3.5" />
          <Loader2 v-else class="h-3.5 w-3.5 animate-spin" />
          触发预警扫描
        </button>
      </div>

      <!-- Filter Bar -->
      <div class="flex items-center gap-3 border-b border-gray-200 bg-white px-6 py-3">
        <el-select v-model="filters.warningLevel" placeholder="预警级别" clearable class="w-28" @change="loadWarnings">
          <el-option :value="1" label="黄色预警" />
          <el-option :value="2" label="橙色预警" />
          <el-option :value="3" label="红色预警" />
        </el-select>
        <el-select v-model="filters.status" placeholder="状态" clearable class="w-28" @change="loadWarnings">
          <el-option :value="0" label="未处理" />
          <el-option :value="1" label="已确认" />
          <el-option :value="2" label="已干预" />
          <el-option :value="3" label="已解除" />
        </el-select>
        <el-select v-model="filters.warningType" placeholder="预警类型" clearable class="w-28" @change="loadWarnings">
          <el-option value="GRADE_FAIL" label="挂科预警" />
          <el-option value="ATTENDANCE_LOW" label="出勤预警" />
          <el-option value="CREDIT_SHORT" label="学分不足" />
        </el-select>
        <el-select
          v-model="filters.classId"
          placeholder="班级"
          clearable
          filterable
          class="w-36"
          @change="loadWarnings"
        >
          <el-option v-for="c in classOptions" :key="c.id" :value="c.id" :label="c.name" />
        </el-select>
      </div>

      <!-- Content -->
      <div class="flex-1 overflow-y-auto px-6 pt-5 pb-6">
        <div v-if="warningLoading" class="flex items-center justify-center py-20">
          <div class="h-8 w-8 animate-spin rounded-full border-2 border-blue-600 border-t-transparent"></div>
        </div>
        <div v-else class="rounded-xl border border-gray-200 bg-white">
          <el-table :data="warnings" stripe class="rounded-xl">
            <el-table-column prop="studentName" label="学生" width="100">
              <template #default="{ row }">
                <div>
                  <div class="font-medium text-gray-900">{{ row.studentName }}</div>
                  <div class="text-xs text-gray-400">{{ row.studentNo }}</div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="className" label="班级" width="120" />
            <el-table-column prop="warningType" label="类型" width="100">
              <template #default="{ row }">
                {{ typeLabel(row.warningType) }}
              </template>
            </el-table-column>
            <el-table-column prop="warningLevel" label="级别" width="100" align="center">
              <template #default="{ row }">
                <span
                  class="inline-block rounded px-2 py-0.5 text-xs font-medium text-white"
                  :style="{ backgroundColor: levelColor(row.warningLevel) }"
                >
                  {{ levelLabel(row.warningLevel) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)" size="small">
                  {{ statusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="生成时间" width="160">
              <template #default="{ row }">
                <span class="text-sm text-gray-500">{{ formatTime(row.createdAt) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <div class="flex items-center gap-1">
                  <button
                    v-if="row.status === 0"
                    class="rounded px-2 py-1 text-xs text-orange-600 hover:bg-orange-50"
                    @click="handleConfirm(row)"
                  >确认</button>
                  <button
                    v-if="row.status === 0 || row.status === 1"
                    class="rounded px-2 py-1 text-xs text-blue-600 hover:bg-blue-50"
                    @click="handleIntervene(row)"
                  >干预</button>
                  <button
                    v-if="row.status !== 3"
                    class="rounded px-2 py-1 text-xs text-green-600 hover:bg-green-50"
                    @click="handleDismiss(row)"
                  >解除</button>
                </div>
              </template>
            </el-table-column>
          </el-table>
          <div class="flex items-center justify-end px-4 py-3">
            <el-pagination
              v-model:current-page="filters.pageNum"
              v-model:page-size="filters.pageSize"
              :total="warningTotal"
              :page-sizes="[20, 50, 100]"
              layout="total, sizes, prev, pager, next"
              small
              @size-change="loadWarnings"
              @current-change="loadWarnings"
            />
          </div>
        </div>
      </div>
    </template>

    <!-- Tab: 预警规则 -->
    <template v-if="activeTab === 'rules'">
      <div class="flex items-center justify-end border-b border-gray-200 bg-white px-6 py-2.5">
        <button
          class="inline-flex items-center gap-1.5 rounded-lg bg-blue-600 px-3 py-1.5 text-sm font-medium text-white transition-colors hover:bg-blue-700"
          @click="showRuleDialog()"
        >
          <Plus class="h-3.5 w-3.5" />
          新建规则
        </button>
      </div>
      <div class="flex-1 overflow-y-auto px-6 pt-5 pb-6">
        <div v-if="ruleLoading" class="flex items-center justify-center py-20">
          <div class="h-8 w-8 animate-spin rounded-full border-2 border-blue-600 border-t-transparent"></div>
        </div>
        <div v-else class="rounded-xl border border-gray-200 bg-white">
          <el-table :data="rules" stripe class="rounded-xl">
            <el-table-column prop="ruleName" label="规则名称" min-width="160" />
            <el-table-column prop="ruleType" label="类型" width="120">
              <template #default="{ row }">
                {{ typeLabel(row.ruleType) }}
              </template>
            </el-table-column>
            <el-table-column prop="warningLevel" label="级别" width="100" align="center">
              <template #default="{ row }">
                <span
                  class="inline-block rounded px-2 py-0.5 text-xs font-medium text-white"
                  :style="{ backgroundColor: levelColor(row.warningLevel) }"
                >
                  {{ levelLabel(row.warningLevel) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="条件" min-width="200">
              <template #default="{ row }">
                <span class="text-sm text-gray-500">{{ formatCondition(row) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="applicableGrades" label="适用年级" width="120">
              <template #default="{ row }">
                {{ row.applicableGrades || '全部' }}
              </template>
            </el-table-column>
            <el-table-column prop="enabled" label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-switch
                  :model-value="row.enabled === true || row.enabled === 1"
                  @change="handleToggleRule(row)"
                  size="small"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <div class="flex items-center gap-1">
                  <button class="rounded px-2 py-1 text-xs text-blue-600 hover:bg-blue-50" @click="showRuleDialog(row)">编辑</button>
                  <button class="rounded px-2 py-1 text-xs text-red-600 hover:bg-red-50" @click="handleDeleteRule(row)">删除</button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </template>

    <!-- Rule Dialog -->
    <el-dialog v-model="ruleDialogVisible" :title="ruleForm.id ? '编辑规则' : '新建规则'" width="520px" destroy-on-close>
      <el-form ref="ruleFormRef" :model="ruleForm" :rules="ruleRules" label-width="90px" class="pr-4">
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="ruleForm.ruleName" placeholder="如: 挂科2门以上" />
        </el-form-item>
        <el-form-item label="规则类型" prop="ruleType">
          <el-select v-model="ruleForm.ruleType" class="w-full" @change="handleRuleTypeChange">
            <el-option value="GRADE_FAIL" label="挂科预警" />
            <el-option value="ATTENDANCE_LOW" label="出勤预警" />
            <el-option value="CREDIT_SHORT" label="学分不足" />
          </el-select>
        </el-form-item>
        <el-form-item label="预警级别" prop="warningLevel">
          <el-radio-group v-model="ruleForm.warningLevel">
            <el-radio :value="1">黄色</el-radio>
            <el-radio :value="2">橙色</el-radio>
            <el-radio :value="3">红色</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- Dynamic params by type -->
        <template v-if="ruleForm.ruleType === 'GRADE_FAIL'">
          <el-form-item label="挂科门数">
            <el-input-number v-model="ruleForm.conditionParams.minFailCount" :min="1" :max="20" />
            <span class="ml-2 text-sm text-gray-500">门及以上触发</span>
          </el-form-item>
        </template>
        <template v-else-if="ruleForm.ruleType === 'ATTENDANCE_LOW'">
          <el-form-item label="出勤率低于">
            <el-input-number v-model="ruleForm.conditionParams.minAttendanceRate" :min="0" :max="100" />
            <span class="ml-2 text-sm text-gray-500">% 触发</span>
          </el-form-item>
        </template>
        <template v-else-if="ruleForm.ruleType === 'CREDIT_SHORT'">
          <el-form-item label="预期学分">
            <el-input-number v-model="ruleForm.conditionParams.expectedCredits" :min="1" />
          </el-form-item>
          <el-form-item label="实际低于">
            <el-input-number v-model="ruleForm.conditionParams.actualCreditsBelow" :min="0" />
            <span class="ml-2 text-sm text-gray-500">学分时触发</span>
          </el-form-item>
        </template>

        <el-form-item label="适用年级">
          <el-input v-model="ruleForm.applicableGrades" placeholder="空=全部，如: 2023,2024" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="ruleSaving" @click="handleSaveRule">保存</el-button>
      </template>
    </el-dialog>

    <!-- Intervene/Dismiss Note Dialog -->
    <el-dialog v-model="noteDialogVisible" :title="noteDialogTitle" width="420px" destroy-on-close>
      <el-input v-model="noteText" type="textarea" :rows="3" :placeholder="noteDialogTitle === '记录干预措施' ? '请输入干预措施详情...' : '解除原因（可选）'" />
      <template #footer>
        <el-button @click="noteDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="noteSaving" @click="submitNote">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus, RefreshCw, Loader2 } from 'lucide-vue-next'
import { semesterApi } from '@/api/calendar'
import { schoolClassApi } from '@/api/organization'
import {
  getRules, createRule, updateRule, deleteRule, toggleRule,
  scanWarnings, getWarnings, getStatistics,
  confirmWarning, interveneWarning, dismissWarning
} from '@/api/academicWarning'
import type { WarningRule, AcademicWarning } from '@/types/academicWarning'
import { WARNING_LEVELS, WARNING_TYPES, WARNING_STATUS } from '@/types/academicWarning'
import type { Semester } from '@/types/teaching'

const tabs = [
  { key: 'records', label: '预警记录' },
  { key: 'rules', label: '预警规则' },
]
const activeTab = ref('records')

// ==================== Shared State ====================
const semesters = ref<Semester[]>([])
const currentSemesterId = ref<number | string>()
const classOptions = ref<{ id: number; name: string }[]>([])

// ==================== Warning Records ====================
const warnings = ref<any[]>([])
const warningTotal = ref(0)
const warningLoading = ref(false)
const scanning = ref(false)
const filters = reactive({
  warningLevel: undefined as number | undefined,
  status: undefined as number | undefined,
  warningType: undefined as string | undefined,
  classId: undefined as number | undefined,
  pageNum: 1,
  pageSize: 20,
})
const stats = ref({ red: 0, orange: 0, yellow: 0, pending: 0, intervened: 0 })

// ==================== Rules ====================
const rules = ref<any[]>([])
const ruleLoading = ref(false)
const ruleDialogVisible = ref(false)
const ruleSaving = ref(false)
const ruleFormRef = ref<FormInstance>()

const defaultConditionParams = () => ({ minFailCount: 2, minAttendanceRate: 80, expectedCredits: 30, actualCreditsBelow: 20 })
const ruleForm = reactive({
  id: undefined as number | undefined,
  ruleName: '',
  ruleType: 'GRADE_FAIL',
  warningLevel: 1 as 1 | 2 | 3,
  conditionParams: defaultConditionParams(),
  applicableGrades: '',
})
const ruleRules: FormRules = {
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  ruleType: [{ required: true, message: '请选择规则类型', trigger: 'change' }],
  warningLevel: [{ required: true, message: '请选择预警级别', trigger: 'change' }],
}

// ==================== Note Dialog ====================
const noteDialogVisible = ref(false)
const noteDialogTitle = ref('')
const noteText = ref('')
const noteSaving = ref(false)
let noteTargetId = 0
let noteAction: 'intervene' | 'dismiss' = 'intervene'

// ==================== Helpers ====================
const levelColor = (level: number) => {
  const item = WARNING_LEVELS.find(l => l.value === level)
  return item?.color || '#909399'
}
const levelLabel = (level: number) => {
  const item = WARNING_LEVELS.find(l => l.value === level)
  return item?.label || '未知'
}
const typeLabel = (type: string) => {
  const item = WARNING_TYPES.find(t => t.value === type)
  return item?.label || type
}
const statusLabel = (status: number) => {
  const item = WARNING_STATUS.find(s => s.value === status)
  return item?.label || '未知'
}
const statusType = (status: number): '' | 'success' | 'warning' | 'info' | 'danger' => {
  const item = WARNING_STATUS.find(s => s.value === status)
  return (item?.type || 'info') as '' | 'success' | 'warning' | 'info' | 'danger'
}
const formatTime = (t: string) => {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 16)
}
const formatCondition = (rule: any) => {
  const p = rule.conditionParams || {}
  switch (rule.ruleType) {
    case 'GRADE_FAIL': return `挂科 >= ${p.minFailCount || 2} 门`
    case 'ATTENDANCE_LOW': return `出勤率 < ${p.minAttendanceRate || 80}%`
    case 'CREDIT_SHORT': return `学分 < ${p.actualCreditsBelow || 20} (预期${p.expectedCredits || 30})`
    default: return JSON.stringify(p)
  }
}

// ==================== Data Loading ====================
const loadSemesters = async () => {
  try {
    const res: any = await semesterApi.list()
    semesters.value = res.data || res
    if (semesters.value.length > 0) {
      const current = semesters.value.find(s => s.isCurrent)
      currentSemesterId.value = current ? current.id : semesters.value[0].id
    }
  } catch (e) {
    console.error('Failed to load semesters:', e)
  }
}

const loadClassOptions = async () => {
  try {
    const res = await schoolClassApi.getAll()
    const data: any = res
    classOptions.value = (Array.isArray(data) ? data : data.records || []).map((c: any) => ({
      id: c.id,
      name: c.name || c.className,
    }))
  } catch (e) {
    console.error('Failed to load classes:', e)
  }
}

const loadWarnings = async () => {
  if (!currentSemesterId.value) return
  warningLoading.value = true
  try {
    const res: any = await getWarnings({
      ...filters,
      semesterId: currentSemesterId.value as number,
    })
    const data = res.data || res
    warnings.value = data.records || []
    warningTotal.value = data.total || 0
  } catch (e) {
    console.error('Failed to load warnings:', e)
  } finally {
    warningLoading.value = false
  }
}

const loadStats = async () => {
  try {
    const res: any = await getStatistics(currentSemesterId.value as number)
    const data = res.data || res
    const byLevel = data.byLevel || []
    const byStatus = data.byStatus || []
    stats.value = {
      red: byLevel.find((l: any) => l.level === 3)?.count || 0,
      orange: byLevel.find((l: any) => l.level === 2)?.count || 0,
      yellow: byLevel.find((l: any) => l.level === 1)?.count || 0,
      pending: byStatus.find((s: any) => s.status === 0)?.count || 0,
      intervened: byStatus.find((s: any) => s.status === 2)?.count || 0,
    }
  } catch (e) {
    console.error('Failed to load statistics:', e)
  }
}

const loadRules = async () => {
  ruleLoading.value = true
  try {
    const res: any = await getRules()
    rules.value = res.data || res
  } catch (e) {
    console.error('Failed to load rules:', e)
  } finally {
    ruleLoading.value = false
  }
}

// ==================== Event Handlers ====================
const handleSemesterChange = () => {
  loadWarnings()
  loadStats()
}

const handleScan = async () => {
  if (!currentSemesterId.value) {
    ElMessage.warning('请先选择学期')
    return
  }
  try {
    await ElMessageBox.confirm('确定要触发预警扫描？将根据启用的规则扫描全体学生。', '确认扫描', { type: 'warning' })
  } catch {
    return
  }
  scanning.value = true
  try {
    const res: any = await scanWarnings(currentSemesterId.value as number)
    const data = res.data || res
    ElMessage.success(`扫描完成，共扫描${data.rulesScanned}条规则，生成${data.totalWarnings}条预警`)
    loadWarnings()
    loadStats()
  } catch (e: any) {
    ElMessage.error(e.message || '扫描失败')
  } finally {
    scanning.value = false
  }
}

const handleConfirm = async (row: any) => {
  try {
    await confirmWarning(row.id)
    ElMessage.success('已确认')
    loadWarnings()
    loadStats()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

const handleIntervene = (row: any) => {
  noteTargetId = row.id
  noteAction = 'intervene'
  noteDialogTitle.value = '记录干预措施'
  noteText.value = ''
  noteDialogVisible.value = true
}

const handleDismiss = (row: any) => {
  noteTargetId = row.id
  noteAction = 'dismiss'
  noteDialogTitle.value = '解除预警'
  noteText.value = ''
  noteDialogVisible.value = true
}

const submitNote = async () => {
  noteSaving.value = true
  try {
    if (noteAction === 'intervene') {
      if (!noteText.value.trim()) {
        ElMessage.warning('请输入干预措施')
        noteSaving.value = false
        return
      }
      await interveneWarning(noteTargetId, noteText.value)
      ElMessage.success('干预措施已记录')
    } else {
      await dismissWarning(noteTargetId, noteText.value)
      ElMessage.success('预警已解除')
    }
    noteDialogVisible.value = false
    loadWarnings()
    loadStats()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    noteSaving.value = false
  }
}

// ==================== Rules CRUD ====================
const showRuleDialog = (row?: any) => {
  if (row) {
    ruleForm.id = row.id
    ruleForm.ruleName = row.ruleName
    ruleForm.ruleType = row.ruleType
    ruleForm.warningLevel = row.warningLevel
    ruleForm.conditionParams = { ...defaultConditionParams(), ...(row.conditionParams || {}) }
    ruleForm.applicableGrades = row.applicableGrades || ''
  } else {
    ruleForm.id = undefined
    ruleForm.ruleName = ''
    ruleForm.ruleType = 'GRADE_FAIL'
    ruleForm.warningLevel = 1
    ruleForm.conditionParams = defaultConditionParams()
    ruleForm.applicableGrades = ''
  }
  ruleDialogVisible.value = true
}

const handleRuleTypeChange = () => {
  // Reset condition params when type changes
  ruleForm.conditionParams = defaultConditionParams()
}

const handleSaveRule = async () => {
  if (!ruleFormRef.value) return
  await ruleFormRef.value.validate()
  ruleSaving.value = true
  try {
    // Build conditionParams based on type
    let params: Record<string, any> = {}
    switch (ruleForm.ruleType) {
      case 'GRADE_FAIL':
        params = { minFailCount: ruleForm.conditionParams.minFailCount }
        break
      case 'ATTENDANCE_LOW':
        params = { minAttendanceRate: ruleForm.conditionParams.minAttendanceRate }
        break
      case 'CREDIT_SHORT':
        params = { expectedCredits: ruleForm.conditionParams.expectedCredits, actualCreditsBelow: ruleForm.conditionParams.actualCreditsBelow }
        break
    }

    const payload = {
      ruleName: ruleForm.ruleName,
      ruleType: ruleForm.ruleType,
      warningLevel: ruleForm.warningLevel,
      conditionParams: params,
      applicableGrades: ruleForm.applicableGrades || null,
    }

    if (ruleForm.id) {
      await updateRule(ruleForm.id, payload)
      ElMessage.success('规则已更新')
    } else {
      await createRule(payload)
      ElMessage.success('规则已创建')
    }
    ruleDialogVisible.value = false
    loadRules()
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    ruleSaving.value = false
  }
}

const handleToggleRule = async (row: any) => {
  try {
    await toggleRule(row.id)
    loadRules()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

const handleDeleteRule = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定删除规则"${row.ruleName}"？`, '确认删除', { type: 'warning' })
    await deleteRule(row.id)
    ElMessage.success('已删除')
    loadRules()
  } catch {
    // cancelled
  }
}

// ==================== Init ====================
onMounted(async () => {
  await Promise.all([loadSemesters(), loadClassOptions(), loadRules()])
  if (currentSemesterId.value) {
    loadWarnings()
    loadStats()
  }
})
</script>
