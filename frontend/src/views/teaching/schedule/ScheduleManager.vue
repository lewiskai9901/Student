<template>
  <div>
    <!-- Schedule plan list -->
    <div class="mb-5 rounded-xl border border-gray-200 bg-white">
      <div class="flex items-center justify-between px-5 py-3">
        <h2 class="text-sm font-semibold text-gray-700">排课方案</h2>
        <el-button type="primary" size="small" @click="showCreateDialog">
          <Plus class="mr-1 h-3.5 w-3.5" /> 新建方案
        </el-button>
      </div>
      <div class="border-t border-gray-100">
        <el-table :data="scheduleList" v-loading="loading" stripe>
          <el-table-column prop="name" label="方案名称" min-width="160" />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="getStatusTag(row.status)">{{ getStatusName(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="160" />
          <el-table-column label="条目数" width="90" align="center">
            <template #default="{ row }">
              {{ row.entryCount ?? '-' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="handleAutoSchedule(row)">智能排课</el-button>
              <el-button v-if="row.status === 0" size="small" text type="success" @click="handlePublish(row)">发布</el-button>
              <el-button v-if="row.status === 0" size="small" text type="warning" @click="handleArchive(row)">归档</el-button>
              <el-button v-if="row.status === 0" size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- Create Schedule Dialog -->
    <el-dialog v-model="createDialogVisible" title="新建排课方案" width="480px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-form-item label="方案名称" prop="name">
          <el-input v-model="createForm.name" placeholder="例如：2025春季排课方案" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="createForm.description" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="createSaving" @click="submitCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- Auto Schedule Dialog -->
    <el-dialog v-model="autoScheduleDialogVisible" title="智能排课" width="520px">
      <el-alert type="info" :closable="false" class="mb-4">
        系统将基于约束规则自动排课。已手动排定的课程不会被调整。
      </el-alert>
      <el-form :model="autoScheduleParams" label-width="120px">
        <el-form-item label="排课方案">
          <el-select v-model="autoScheduleParams.scheduleId" style="width: 100%" disabled>
            <el-option v-for="s in scheduleList" :key="s.id" :value="s.id" :label="s.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="最大迭代次数">
          <el-input-number v-model="autoScheduleParams.maxIterations" :min="100" :max="5000" :step="100" />
        </el-form-item>
        <el-form-item label="种群大小">
          <el-input-number v-model="autoScheduleParams.populationSize" :min="10" :max="100" :step="10" />
        </el-form-item>
        <el-form-item label="变异率">
          <el-slider v-model="autoScheduleParams.mutationRate" :min="0.01" :max="0.5" :step="0.01" show-input />
        </el-form-item>
      </el-form>
      <div v-if="scheduling" class="my-4 rounded-lg bg-gray-50 p-4">
        <div class="mb-2 flex justify-between text-sm"><span>正在排课...</span></div>
        <el-progress :percentage="autoProgress" />
      </div>
      <div v-if="autoResult" class="my-4 rounded-lg border p-4" :class="autoResult.success ? 'border-green-200 bg-green-50' : 'border-red-200 bg-red-50'">
        <p class="font-medium" :class="autoResult.success ? 'text-green-600' : 'text-red-600'">
          {{ autoResult.success ? '排课完成' : '排课失败' }}
        </p>
        <p class="mt-1 text-sm text-gray-600">生成: {{ autoResult.entriesGenerated || 0 }} 条 | 耗时: {{ ((autoResult.executionTime || 0) / 1000).toFixed(1) }}s</p>
      </div>
      <template #footer>
        <el-button @click="autoScheduleDialogVisible = false" :disabled="scheduling">取消</el-button>
        <el-button type="primary" :loading="scheduling" @click="runAutoSchedule">
          {{ scheduling ? '排课中...' : '开始排课' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from 'lucide-vue-next'
import { scheduleApi } from '@/api/teaching'
import type { CourseSchedule } from '@/types/teaching'

// ==================== Props ====================

const props = defineProps<{
  semesterId: number | string | undefined
}>()

// ==================== State ====================

const loading = ref(false)
const scheduleList = ref<CourseSchedule[]>([])

// ==================== Create Dialog ====================

const createDialogVisible = ref(false)
const createSaving = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = ref({ name: '', description: '' })

const createRules: FormRules = {
  name: [{ required: true, message: '请输入方案名称', trigger: 'blur' }],
}

// ==================== Auto Schedule Dialog ====================

const autoScheduleDialogVisible = ref(false)
const scheduling = ref(false)
const autoProgress = ref(0)
const autoResult = ref<any>(null)
const autoScheduleParams = ref({
  scheduleId: undefined as number | string | undefined,
  maxIterations: 1000,
  populationSize: 100,
  mutationRate: 0.1,
})

// ==================== Data Loading ====================

async function loadScheduleList() {
  if (!props.semesterId) return
  loading.value = true
  try {
    const res = await scheduleApi.list({ semesterId: props.semesterId })
    scheduleList.value = (res as any).data || res
    if (!Array.isArray(scheduleList.value)) scheduleList.value = []
  } catch (e) {
    console.error('Failed to load schedules:', e)
  } finally {
    loading.value = false
  }
}

// ==================== Actions ====================

function showCreateDialog() {
  createForm.value = { name: '', description: '' }
  createDialogVisible.value = true
}

async function submitCreate() {
  await createFormRef.value?.validate()
  if (!props.semesterId) return
  createSaving.value = true
  try {
    await scheduleApi.create({
      semesterId: props.semesterId,
      name: createForm.value.name,
      description: createForm.value.description,
    })
    ElMessage.success('创建成功')
    createDialogVisible.value = false
    loadScheduleList()
  } catch (e) {
    ElMessage.error('创建失败')
  } finally {
    createSaving.value = false
  }
}

function handleAutoSchedule(schedule: CourseSchedule) {
  autoScheduleParams.value = {
    scheduleId: schedule.id,
    maxIterations: 500,
    populationSize: 30,
    mutationRate: 0.1,
  }
  autoProgress.value = 0
  autoResult.value = null
  autoScheduleDialogVisible.value = true
}

async function runAutoSchedule() {
  if (!props.semesterId) {
    ElMessage.warning('请先选择学期')
    return
  }
  scheduling.value = true
  autoProgress.value = 0
  autoResult.value = null
  const timer = setInterval(() => {
    if (autoProgress.value < 90) autoProgress.value += Math.random() * 15
  }, 500)
  try {
    const result = await scheduleApi.autoSchedule({
      semesterId: props.semesterId,
      scheduleId: autoScheduleParams.value.scheduleId,
      maxIterations: autoScheduleParams.value.maxIterations,
      populationSize: autoScheduleParams.value.populationSize,
      mutationRate: autoScheduleParams.value.mutationRate,
    })
    autoProgress.value = 100
    const data = (result as any).data || result
    autoResult.value = data
    if (data.success) {
      ElMessage.success(`排课完成！共生成 ${data.entriesGenerated} 条记录`)
      if (data.conflicts?.length > 0) {
        ElMessage.warning(`存在 ${data.conflicts.length} 个冲突，请检查`)
      }
      loadScheduleList()
    } else {
      ElMessage.error('排课失败')
    }
  } catch (e: any) {
    ElMessage.error('排课失败: ' + (e.message || '请检查约束配置'))
  } finally {
    clearInterval(timer)
    scheduling.value = false
  }
}

async function handlePublish(schedule: CourseSchedule) {
  await ElMessageBox.confirm(
    `确定发布方案「${schedule.name}」？发布后课表对所有用户可见。`,
    '发布确认',
    { type: 'warning' },
  )
  try {
    await scheduleApi.publish(schedule.id)
    ElMessage.success('发布成功')
    loadScheduleList()
  } catch (e) {
    ElMessage.error('发布失败')
  }
}

async function handleArchive(schedule: CourseSchedule) {
  await ElMessageBox.confirm(
    `确定归档方案「${schedule.name}」？`,
    '归档确认',
  )
  try {
    await scheduleApi.archive(schedule.id)
    ElMessage.success('归档成功')
    loadScheduleList()
  } catch (e) {
    ElMessage.error('归档失败')
  }
}

async function handleDelete(schedule: CourseSchedule) {
  await ElMessageBox.confirm(
    `确定删除方案「${schedule.name}」？此操作不可撤销。`,
    '删除确认',
    { type: 'warning' },
  )
  try {
    await scheduleApi.delete(schedule.id)
    ElMessage.success('删除成功')
    loadScheduleList()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

function getStatusName(status: number) {
  const m: Record<number, string> = { 0: '草稿', 1: '已发布', 2: '已归档' }
  return m[status] || '未知'
}

function getStatusTag(status: number) {
  const m: Record<number, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    0: 'info', 1: 'success', 2: 'warning',
  }
  return m[status] || 'info'
}

// ==================== Watchers ====================

watch(() => props.semesterId, () => {
  loadScheduleList()
}, { immediate: true })

// ==================== Expose ====================

defineExpose({ loadScheduleList })
</script>
