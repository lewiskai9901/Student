<template>
  <div>
    <!-- Action buttons -->
    <div class="mb-4 flex items-center gap-3">
      <el-button type="primary" :loading="feasibilityLoading" @click="runFeasibilityCheck">
        <ShieldCheck class="mr-1 h-4 w-4" /> 运行可行性检测
      </el-button>
      <el-button :loading="conflictDetecting" @click="runConflictDetection">
        <SearchCheck class="mr-1 h-4 w-4" /> 检测排课冲突
      </el-button>
    </div>

    <!-- Feasibility report -->
    <div v-if="feasibilityReport" class="mb-5 rounded-xl border border-gray-200 bg-white p-5">
      <h3 class="mb-3 text-sm font-semibold text-gray-700">可行性报告</h3>
      <div class="mb-4 flex items-center gap-6 text-sm">
        <span class="flex items-center gap-1.5">
          <CheckCircle2 class="h-4 w-4 text-emerald-500" />
          通过 <span class="font-semibold text-emerald-600">{{ feasibilityReport.passedChecks }}</span> 项
        </span>
        <span class="flex items-center gap-1.5">
          <XCircle class="h-4 w-4 text-rose-500" />
          阻塞 <span class="font-semibold text-rose-600">{{ feasibilityReport.blockingIssues.length }}</span> 个
        </span>
        <span class="flex items-center gap-1.5">
          <AlertTriangle class="h-4 w-4 text-amber-500" />
          警告 <span class="font-semibold text-amber-600">{{ feasibilityReport.warnings.length }}</span> 个
        </span>
      </div>

      <!-- Blocking issues -->
      <div v-if="feasibilityReport.blockingIssues.length > 0" class="mb-3">
        <div class="mb-1.5 text-xs font-medium text-rose-600">阻塞项</div>
        <div
          v-for="(issue, idx) in feasibilityReport.blockingIssues"
          :key="'block-' + idx"
          class="mb-1.5 rounded-lg border border-rose-100 bg-rose-50 px-3 py-2 text-xs text-rose-700"
        >
          <span class="font-medium">[{{ issue.type }}]</span> {{ issue.target }}: {{ issue.description }}
          <span v-if="issue.suggestion" class="ml-2 text-rose-500">-- {{ issue.suggestion }}</span>
        </div>
      </div>

      <!-- Warnings -->
      <div v-if="feasibilityReport.warnings.length > 0">
        <div class="mb-1.5 text-xs font-medium text-amber-600">警告项</div>
        <div
          v-for="(issue, idx) in feasibilityReport.warnings"
          :key="'warn-' + idx"
          class="mb-1.5 rounded-lg border border-amber-100 bg-amber-50 px-3 py-2 text-xs text-amber-700"
        >
          <span class="font-medium">[{{ issue.type }}]</span> {{ issue.target }}: {{ issue.description }}
        </div>
      </div>
    </div>

    <!-- Conflict list -->
    <div class="rounded-xl border border-gray-200 bg-white">
      <div class="flex items-center justify-between px-5 py-3">
        <h2 class="text-sm font-semibold text-gray-700">冲突列表</h2>
        <el-select v-model="conflictStatusFilter" placeholder="状态" class="w-28" size="small" clearable @change="loadConflicts">
          <el-option :value="0" label="未处理" />
          <el-option :value="1" label="已解决" />
          <el-option :value="2" label="已忽略" />
        </el-select>
      </div>
      <div class="border-t border-gray-100">
        <el-table :data="conflicts" v-loading="conflictsLoading" stripe>
          <el-table-column label="类型" width="100" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="getConflictCategoryTag(row.conflictCategory)">
                {{ getConflictCategoryName(row.conflictCategory) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="严重度" width="90" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="getSeverityTag(row.severity)">
                {{ getSeverityName(row.severity) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="conflictType" label="冲突类别" width="120" />
          <el-table-column prop="description" label="描述" min-width="240" show-overflow-tooltip />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="getResolutionStatusTag(row.resolutionStatus)">
                {{ getResolutionStatusName(row.resolutionStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <template v-if="row.resolutionStatus === 0">
                <el-button size="small" text type="success" @click="resolveConflict(row)">解决</el-button>
                <el-button size="small" text type="warning" @click="ignoreConflict(row)">忽略</el-button>
              </template>
              <span v-else class="text-xs text-gray-400">{{ row.resolutionNote || '-' }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- Resolve conflict dialog -->
    <el-dialog v-model="resolveDialogVisible" title="处理冲突" width="400px">
      <el-input v-model="resolveNote" type="textarea" :rows="3" :placeholder="resolveAction === 'resolve' ? '请描述解决方案' : '请说明忽略原因'" />
      <template #footer>
        <el-button @click="resolveDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="resolveSaving" @click="confirmResolve">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { ShieldCheck, SearchCheck, CheckCircle2, XCircle, AlertTriangle } from 'lucide-vue-next'
import { conflictApi } from '@/api/teaching'
import type { DetectedConflict, FeasibilityReport } from '@/types/teaching'

// ==================== Props ====================

const props = defineProps<{
  semesterId: number | string | undefined
}>()

// ==================== State ====================

const conflicts = ref<DetectedConflict[]>([])
const conflictsLoading = ref(false)
const conflictDetecting = ref(false)
const feasibilityLoading = ref(false)
const feasibilityReport = ref<FeasibilityReport | null>(null)
const conflictStatusFilter = ref<number | undefined>(undefined)

const resolveDialogVisible = ref(false)
const resolveNote = ref('')
const resolveAction = ref<'resolve' | 'ignore'>('resolve')
const resolveTargetId = ref<number>()
const resolveSaving = ref(false)

// ==================== Data Loading ====================

async function loadConflicts() {
  if (!props.semesterId) return
  conflictsLoading.value = true
  try {
    const res = await conflictApi.list({
      semesterId: props.semesterId,
      status: conflictStatusFilter.value,
    })
    conflicts.value = (res as any).data || res
  } catch (e) {
    console.error('Failed to load conflicts:', e)
  } finally {
    conflictsLoading.value = false
  }
}

// ==================== Actions ====================

async function runFeasibilityCheck() {
  if (!props.semesterId) return
  feasibilityLoading.value = true
  try {
    const res = await conflictApi.feasibilityCheck(props.semesterId)
    feasibilityReport.value = (res as any).data || res
    ElMessage.success('可行性检测完成')
  } catch (e) {
    ElMessage.error('检测失败')
  } finally {
    feasibilityLoading.value = false
  }
}

async function runConflictDetection() {
  if (!props.semesterId) return
  conflictDetecting.value = true
  try {
    const res = await conflictApi.detect(props.semesterId)
    const detected = (res as any).data || res
    if (Array.isArray(detected)) {
      ElMessage.success(`检测完成，发现 ${detected.length} 个冲突`)
    }
    loadConflicts()
  } catch (e) {
    ElMessage.error('检测失败')
  } finally {
    conflictDetecting.value = false
  }
}

function resolveConflict(conflict: DetectedConflict) {
  resolveAction.value = 'resolve'
  resolveTargetId.value = conflict.id
  resolveNote.value = ''
  resolveDialogVisible.value = true
}

function ignoreConflict(conflict: DetectedConflict) {
  resolveAction.value = 'ignore'
  resolveTargetId.value = conflict.id
  resolveNote.value = ''
  resolveDialogVisible.value = true
}

async function confirmResolve() {
  if (!resolveTargetId.value) return
  if (!resolveNote.value.trim()) {
    ElMessage.warning('请填写备注')
    return
  }
  resolveSaving.value = true
  try {
    if (resolveAction.value === 'resolve') {
      await conflictApi.resolve(resolveTargetId.value, resolveNote.value)
    } else {
      await conflictApi.ignore(resolveTargetId.value, resolveNote.value)
    }
    ElMessage.success('操作成功')
    resolveDialogVisible.value = false
    loadConflicts()
  } catch (e) {
    ElMessage.error('操作失败')
  } finally {
    resolveSaving.value = false
  }
}

// ==================== Helpers ====================

function getConflictCategoryName(cat: number) {
  const m: Record<number, string> = { 1: '资源冲突', 2: '约束违反', 3: '时间冲突' }
  return m[cat] || '未知'
}

function getConflictCategoryTag(cat: number) {
  const m: Record<number, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    1: 'danger', 2: 'warning', 3: '',
  }
  return m[cat] || 'info'
}

function getSeverityName(s: number) {
  const m: Record<number, string> = { 1: '低', 2: '中', 3: '高' }
  return m[s] || '-'
}

function getSeverityTag(s: number) {
  const m: Record<number, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    1: 'info', 2: 'warning', 3: 'danger',
  }
  return m[s] || 'info'
}

function getResolutionStatusName(s: number) {
  const m: Record<number, string> = { 0: '未处理', 1: '已解决', 2: '已忽略' }
  return m[s] || '-'
}

function getResolutionStatusTag(s: number) {
  const m: Record<number, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    0: 'danger', 1: 'success', 2: 'info',
  }
  return m[s] || 'info'
}

// ==================== Watchers ====================

watch(() => props.semesterId, () => {
  loadConflicts()
}, { immediate: true })
</script>
