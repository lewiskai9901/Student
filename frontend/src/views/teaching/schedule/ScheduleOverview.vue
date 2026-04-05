<template>
  <div>
    <!-- Progress bar -->
    <div class="mb-5 rounded-xl border border-gray-200 bg-white p-5">
      <div class="mb-3 flex items-center justify-between">
        <span class="text-sm font-semibold text-gray-700">排课进度</span>
        <span class="text-sm font-semibold text-blue-600">{{ overviewProgress }}%</span>
      </div>
      <div class="mb-4 h-2.5 w-full overflow-hidden rounded-full bg-gray-100">
        <div
          class="h-full rounded-full bg-blue-500 transition-all duration-500"
          :style="{ width: overviewProgress + '%' }"
        />
      </div>

      <!-- Stat cards -->
      <div class="grid grid-cols-4 gap-4">
        <div class="rounded-lg border border-gray-100 bg-gray-50 px-4 py-3 text-center">
          <div class="text-xl font-bold text-gray-900">{{ taskStats.total }}</div>
          <div class="mt-0.5 text-xs text-gray-500">教学任务</div>
        </div>
        <div class="rounded-lg border border-emerald-100 bg-emerald-50 px-4 py-3 text-center">
          <div class="text-xl font-bold text-emerald-600">{{ taskStats.scheduled }}</div>
          <div class="mt-0.5 text-xs text-gray-500">已排完</div>
        </div>
        <div class="rounded-lg border border-amber-100 bg-amber-50 px-4 py-3 text-center">
          <div class="text-xl font-bold text-amber-600">{{ taskStats.partial }}</div>
          <div class="mt-0.5 text-xs text-gray-500">部分排</div>
        </div>
        <div class="rounded-lg border border-rose-100 bg-rose-50 px-4 py-3 text-center">
          <div class="text-xl font-bold text-rose-600">{{ taskStats.unscheduled }}</div>
          <div class="mt-0.5 text-xs text-gray-500">未排课</div>
        </div>
      </div>
    </div>

    <!-- Action buttons -->
    <div class="mb-5 flex items-center gap-3">
      <el-button type="primary" @click="handleAutoSchedule">
        <Play class="mr-1 h-4 w-4" /> 智能排课
      </el-button>
      <el-button @click="handleFeasibilityCheck">
        <ClipboardCheck class="mr-1 h-4 w-4" /> 可行性检测
      </el-button>
      <el-button @click="handlePublish">
        <Megaphone class="mr-1 h-4 w-4" /> 发布课表
      </el-button>
    </div>

    <!-- Teaching task table -->
    <div class="rounded-xl border border-gray-200 bg-white">
      <div class="flex items-center justify-between px-5 py-3">
        <h2 class="text-sm font-semibold text-gray-700">教学任务列表</h2>
        <div class="flex items-center gap-2">
          <el-input
            v-model="taskFilter.keyword"
            placeholder="搜索课程/教师"
            class="w-48"
            size="small"
            clearable
            @clear="loadTasks"
            @keyup.enter="loadTasks"
          >
            <template #prefix><Search class="h-3.5 w-3.5 text-gray-400" /></template>
          </el-input>
          <el-select v-model="taskFilter.status" placeholder="状态" class="w-28" size="small" clearable @change="loadTasks">
            <el-option :value="0" label="待分配" />
            <el-option :value="1" label="已分配" />
            <el-option :value="2" label="已排课" />
            <el-option :value="3" label="进行中" />
          </el-select>
        </div>
      </div>
      <div class="border-t border-gray-100">
        <el-table :data="tasks" v-loading="taskLoading" stripe>
          <el-table-column prop="className" label="教学班" min-width="120" />
          <el-table-column prop="courseName" label="课程" min-width="140" />
          <el-table-column label="教师" min-width="100">
            <template #default="{ row }">
              {{ getMainTeacher(row) || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="weeklyHours" label="周课时" width="80" align="center" />
          <el-table-column label="已排/总需" width="100" align="center">
            <template #default="{ row }">
              {{ getScheduledHours(row) }}/{{ row.weeklyHours }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="getTaskStatusTag(row.status)">{{ getTaskStatusName(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="showManualEntryDialog(row)">手动排</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="flex justify-end px-4 py-3">
          <el-pagination
            v-model:current-page="taskPagination.page"
            v-model:page-size="taskPagination.size"
            :total="taskPagination.total"
            :page-sizes="[20, 50, 100]"
            layout="total, sizes, prev, pager, next"
            small
            @size-change="loadTasks"
            @current-change="loadTasks"
          />
        </div>
      </div>
    </div>

    <!-- Auto Schedule Dialog -->
    <el-dialog v-model="autoScheduleDialogVisible" title="智能排课" width="520px">
      <el-alert type="info" :closable="false" class="mb-4">
        系统将基于约束规则自动排课。已手动排定的课程不会被调整。
      </el-alert>
      <el-form :model="autoScheduleParams" label-width="120px">
        <el-form-item label="排课方案">
          <el-select v-model="autoScheduleParams.scheduleId" style="width: 100%">
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

    <!-- Manual Entry Dialog -->
    <el-dialog
      v-model="entryDialogVisible"
      :title="entryForm.id ? '编辑排课' : '手动排课'"
      width="600px"
    >
      <el-form ref="entryFormRef" :model="entryForm" :rules="entryRules" label-width="100px">
        <el-form-item label="教学任务" prop="taskId">
          <el-select v-model="entryForm.taskId" placeholder="选择教学任务" style="width: 100%" filterable disabled>
            <el-option
              v-for="t in taskOptionsForEntry"
              :key="t.id"
              :value="t.id"
              :label="`${t.courseName} - ${t.className}`"
            />
          </el-select>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="星期" prop="dayOfWeek">
              <el-select v-model="entryForm.dayOfWeek" style="width: 100%">
                <el-option v-for="d in weekdays" :key="d.value" :value="d.value" :label="d.label" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="教室" prop="classroomId">
              <el-select v-model="entryForm.classroomId" placeholder="选择教室" style="width: 100%" filterable>
                <el-option v-for="c in classrooms" :key="c.id" :value="c.id" :label="c.name" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="开始节次" prop="periodStart">
              <el-select v-model="entryForm.periodStart" style="width: 100%">
                <el-option v-for="p in periods" :key="p.period" :value="p.period" :label="p.name" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束节次" prop="periodEnd">
              <el-select v-model="entryForm.periodEnd" style="width: 100%">
                <el-option v-for="p in periods" :key="p.period" :value="p.period" :label="p.name" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="开始周" prop="weekStart">
              <el-input-number v-model="entryForm.weekStart" :min="1" :max="20" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="结束周" prop="weekEnd">
              <el-input-number v-model="entryForm.weekEnd" :min="1" :max="20" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="周类型">
              <el-select v-model="entryForm.weekType" style="width: 100%">
                <el-option :value="0" label="每周" />
                <el-option :value="1" label="单周" />
                <el-option :value="2" label="双周" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="entryDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="entrySaving" @click="saveEntry">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Play, ClipboardCheck, Megaphone } from 'lucide-vue-next'
import { http as request } from '@/utils/request'
import {
  scheduleApi, teachingTaskApi, conflictApi,
} from '@/api/teaching'
import type {
  CourseSchedule, ScheduleEntry, TeachingTask,
} from '@/types/teaching'
import { WEEKDAYS, DEFAULT_PERIODS } from '@/types/teaching'

// ==================== Props ====================

const props = defineProps<{
  semesterId: number | string | undefined
}>()

// ==================== Constants ====================

const weekdays = WEEKDAYS
const periods = DEFAULT_PERIODS

// ==================== State ====================

const tasks = ref<TeachingTask[]>([])
const taskLoading = ref(false)
const taskFilter = reactive({ keyword: '', status: undefined as number | undefined })
const taskPagination = reactive({ page: 1, size: 20, total: 0 })

const scheduleList = ref<CourseSchedule[]>([])
const classrooms = ref<{ id: number; name: string }[]>([])

const taskStats = computed(() => {
  const total = taskPagination.total
  const scheduled = tasks.value.filter(t => t.status >= 2).length
  const partial = tasks.value.filter(t => t.status === 1).length
  const unscheduled = tasks.value.filter(t => t.status === 0).length
  return { total, scheduled, partial, unscheduled }
})

const overviewProgress = computed(() => {
  if (taskStats.value.total === 0) return 0
  return Math.round((taskStats.value.scheduled / taskStats.value.total) * 100)
})

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

// ==================== Manual Entry Dialog ====================

const entryDialogVisible = ref(false)
const entrySaving = ref(false)
const entryFormRef = ref<FormInstance>()
const entryForm = ref<Partial<ScheduleEntry>>({})
const taskOptionsForEntry = ref<{ id: number | string; courseName: string; className: string }[]>([])

const entryRules: FormRules = {
  taskId: [{ required: true, message: '请选择教学任务', trigger: 'change' }],
  dayOfWeek: [{ required: true, message: '请选择星期', trigger: 'change' }],
  classroomId: [{ required: true, message: '请选择教室', trigger: 'change' }],
  periodStart: [{ required: true, message: '请选择开始节次', trigger: 'change' }],
  periodEnd: [{ required: true, message: '请选择结束节次', trigger: 'change' }],
  weekStart: [{ required: true, message: '请输入开始周', trigger: 'blur' }],
  weekEnd: [{ required: true, message: '请输入结束周', trigger: 'blur' }],
}

// ==================== Data Loading ====================

async function loadTasks() {
  if (!props.semesterId) return
  taskLoading.value = true
  try {
    const res = await teachingTaskApi.list({
      semesterId: props.semesterId,
      status: taskFilter.status,
      page: taskPagination.page,
      size: taskPagination.size,
    })
    const data = (res as any).data || res
    if (data.records) {
      tasks.value = data.records
      taskPagination.total = data.total || 0
    } else if (Array.isArray(data)) {
      tasks.value = data
      taskPagination.total = data.length
    }
  } catch (e) {
    console.error('Failed to load tasks:', e)
  } finally {
    taskLoading.value = false
  }
}

async function loadScheduleList() {
  if (!props.semesterId) return
  try {
    const res = await scheduleApi.list({ semesterId: props.semesterId })
    scheduleList.value = (res as any).data || res
    if (!Array.isArray(scheduleList.value)) scheduleList.value = []
  } catch (e) {
    console.error('Failed to load schedules:', e)
  }
}

async function loadClassrooms() {
  try {
    const res = await request.get('/v9/places', { params: { typeCode: 'CLASSROOM' } })
    const data = (res as any).data || res
    const items = Array.isArray(data) ? data : data.records || []
    classrooms.value = items.map((p: any) => ({ id: p.id, name: p.placeName || p.name }))
  } catch (e) {
    console.error('Failed to load classrooms:', e)
  }
}

// ==================== Actions ====================

function handleAutoSchedule() {
  if (scheduleList.value.length === 0) {
    ElMessage.warning('当前学期暂无排课方案，请先创建排课方案')
    return
  }
  autoScheduleParams.value = {
    scheduleId: scheduleList.value[0].id,
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
      loadTasks()
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

async function handleFeasibilityCheck() {
  if (!props.semesterId) return
  try {
    const res = await conflictApi.feasibilityCheck(props.semesterId)
    const report = (res as any).data || res
    ElMessage.success('可行性检测完成')
    // Report is now handled locally; could add inline display if needed
    console.log('Feasibility report:', report)
  } catch (e) {
    ElMessage.error('可行性检测失败')
  }
}

async function handlePublish() {
  const draftSchedules = scheduleList.value.filter(s => s.status === 0)
  if (draftSchedules.length === 0) {
    ElMessage.info('暂无待发布的排课方案')
    return
  }
  await ElMessageBox.confirm(
    `将发布 ${draftSchedules.length} 个排课方案，发布后课表对所有用户可见，确定发布吗？`,
    '发布确认',
    { type: 'warning' },
  )
  try {
    for (const s of draftSchedules) {
      await scheduleApi.publish(s.id)
    }
    ElMessage.success('发布成功')
    loadScheduleList()
  } catch (e) {
    ElMessage.error('发布失败')
  }
}

function getMainTeacher(task: TeachingTask): string {
  if (task.teachers && task.teachers.length > 0) {
    const main = task.teachers.find(t => t.isMain)
    return main?.teacherName || task.teachers[0]?.teacherName || ''
  }
  return ''
}

function getScheduledHours(task: TeachingTask): number {
  if (task.status >= 2) return task.weeklyHours
  if (task.status === 1) return Math.floor(task.weeklyHours / 2)
  return 0
}

function showManualEntryDialog(task: TeachingTask) {
  if (scheduleList.value.length === 0) {
    ElMessage.warning('请先创建排课方案')
    return
  }
  taskOptionsForEntry.value = [{
    id: task.id,
    courseName: task.courseName || '',
    className: task.className || '',
  }]
  entryForm.value = {
    scheduleId: scheduleList.value[0].id,
    taskId: task.id,
    weekStart: task.startWeek || 1,
    weekEnd: task.endWeek || 16,
    weekType: 0,
  }
  entryDialogVisible.value = true
}

async function saveEntry() {
  await entryFormRef.value?.validate()
  if (!entryForm.value.scheduleId) return
  entrySaving.value = true
  try {
    if (entryForm.value.id) {
      await scheduleApi.updateEntry(entryForm.value.scheduleId, entryForm.value.id, entryForm.value)
    } else {
      await scheduleApi.addEntry(entryForm.value.scheduleId, entryForm.value)
    }
    ElMessage.success('保存成功')
    entryDialogVisible.value = false
    loadTasks()
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    entrySaving.value = false
  }
}

function getTaskStatusName(status: number) {
  const m: Record<number, string> = { 0: '待分配', 1: '已分配', 2: '已排课', 3: '进行中', 4: '已结束' }
  return m[status] || '未知'
}

function getTaskStatusTag(status: number) {
  const m: Record<number, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    0: 'info', 1: 'warning', 2: 'success', 3: '', 4: 'info',
  }
  return m[status] || 'info'
}

// ==================== Watchers ====================

watch(() => props.semesterId, () => {
  loadTasks()
  loadScheduleList()
  loadClassrooms()
}, { immediate: true })

// ==================== Expose ====================

defineExpose({ loadTasks })
</script>
