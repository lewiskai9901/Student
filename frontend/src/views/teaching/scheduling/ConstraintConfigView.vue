<template>
  <div class="flex h-full flex-col bg-gray-50">
    <!-- Header -->
    <div class="flex items-center justify-between border-b border-gray-200 bg-white px-6 py-4">
      <div>
        <h1 class="text-lg font-semibold text-gray-900">排课约束配置</h1>
        <p class="mt-0.5 text-sm text-gray-500">配置全局、教师、班级、课程四级排课约束</p>
      </div>
      <el-select v-model="semesterId" placeholder="选择学期" class="w-48" @change="onSemesterChange">
        <el-option v-for="s in semesters" :key="s.id" :value="s.id" :label="s.name" />
      </el-select>
    </div>

    <!-- Level Tabs -->
    <div class="flex items-center border-b border-gray-200 bg-white px-6">
      <button
        v-for="lvl in CONSTRAINT_LEVELS"
        :key="lvl.value"
        class="relative mr-1 px-4 py-2.5 text-sm font-medium transition-colors"
        :class="activeLevel === lvl.value
          ? 'text-blue-600 after:absolute after:bottom-0 after:left-0 after:h-0.5 after:w-full after:bg-blue-600'
          : 'text-gray-500 hover:text-gray-700'"
        @click="switchLevel(lvl.value)"
      >
        {{ lvl.label }}约束
      </button>
    </div>

    <!-- Target Selector (level 2-4 only) -->
    <div v-if="activeLevel > 1" class="flex items-center gap-3 border-b border-gray-200 bg-white px-6 py-2.5">
      <span class="text-sm text-gray-500">{{ targetLabel }}：</span>
      <el-select
        v-model="targetId"
        :placeholder="`选择${targetLabel}`"
        class="w-60"
        filterable
        clearable
        @change="onTargetChange"
      >
        <el-option v-for="t in targetOptions" :key="t.value" :value="t.value" :label="t.label" />
      </el-select>
      <span v-if="!targetId" class="text-xs text-gray-400">请先选择{{ targetLabel }}以查看约束</span>
    </div>

    <!-- Stat Bar -->
    <div class="flex items-center gap-4 border-b border-gray-200 bg-white px-6 py-2.5">
      <span class="text-sm text-gray-500">总约束 <span class="font-semibold text-gray-900">{{ constraints.length }}</span></span>
      <div class="h-3 w-px bg-gray-200" />
      <span class="text-sm text-gray-500">硬约束 <span class="font-semibold text-gray-900">{{ hardConstraints.length }}</span></span>
      <div class="h-3 w-px bg-gray-200" />
      <span class="text-sm text-gray-500">软约束 <span class="font-semibold text-gray-900">{{ softConstraints.length }}</span></span>
      <div class="h-3 w-px bg-gray-200" />
      <span class="text-sm text-gray-500">已启用 <span class="font-semibold text-green-600">{{ enabledCount }}</span></span>
      <div class="flex-1" />
      <button
        class="inline-flex items-center gap-1.5 rounded-lg bg-blue-600 px-3 py-1.5 text-xs font-medium text-white transition-colors hover:bg-blue-700 disabled:opacity-50"
        :disabled="activeLevel > 1 && !targetId"
        @click="openAddDialog"
      >
        <Plus class="h-3.5 w-3.5" />
        新增约束
      </button>
    </div>

    <!-- Main Content -->
    <div class="flex-1 overflow-y-auto px-6 pt-5 pb-6">
      <!-- Loading -->
      <div v-if="loading" class="flex items-center justify-center py-20">
        <div class="h-8 w-8 animate-spin rounded-full border-2 border-blue-600 border-t-transparent" />
      </div>

      <!-- Empty hint for level 2-4 without target -->
      <div v-else-if="activeLevel > 1 && !targetId" class="flex flex-col items-center justify-center py-20 text-gray-400">
        <Settings class="h-12 w-12" />
        <p class="mt-3 text-sm">请先在上方选择{{ targetLabel }}</p>
      </div>

      <!-- Two columns: constraint list + time matrix -->
      <div v-else class="grid grid-cols-1 gap-5 lg:grid-cols-2">
        <!-- Constraint List Card -->
        <div class="rounded-xl border border-gray-200 bg-white">
          <div class="border-b border-gray-100 px-5 py-3">
            <h2 class="text-sm font-semibold text-gray-700">约束列表</h2>
          </div>
          <div class="max-h-[calc(100vh-340px)] overflow-y-auto p-4">
            <!-- Empty -->
            <div v-if="constraints.length === 0" class="py-12 text-center text-sm text-gray-400">暂无约束配置</div>

            <!-- Hard Constraints Section -->
            <template v-if="hardConstraints.length > 0">
              <div class="mb-2 flex items-center gap-2">
                <div class="h-px flex-1 bg-red-100" />
                <span class="text-xs font-medium text-red-500">硬约束 ({{ hardConstraints.length }})</span>
                <div class="h-px flex-1 bg-red-100" />
              </div>
              <div v-for="c in hardConstraints" :key="c.id" class="mb-2.5">
                <!-- Constraint Card Inline -->
                <div
                  class="rounded-lg border px-3.5 py-2.5 transition-colors"
                  :class="c.enabled ? 'border-gray-200 bg-white' : 'border-gray-100 bg-gray-50 opacity-60'"
                >
                  <div class="flex items-start justify-between">
                    <div class="min-w-0 flex-1">
                      <div class="flex items-center gap-2">
                        <span class="text-sm text-red-500">&#9679;</span>
                        <span class="truncate text-sm font-medium text-gray-900">{{ c.constraintName }}</span>
                      </div>
                      <div class="mt-1 flex flex-wrap items-center gap-2 text-xs text-gray-500">
                        <span class="rounded bg-gray-100 px-1.5 py-0.5">{{ getTypeLabel(c.constraintType) }}</span>
                        <span v-if="getParamSummary(c)">{{ getParamSummary(c) }}</span>
                      </div>
                    </div>
                    <div class="ml-2 flex flex-shrink-0 items-center gap-1">
                      <button class="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-600" @click="openEditDialog(c)">
                        <Edit3 class="h-3.5 w-3.5" />
                      </button>
                      <button class="rounded p-1 text-gray-400 hover:bg-red-50 hover:text-red-500" @click="handleDelete(c)">
                        <Trash2 class="h-3.5 w-3.5" />
                      </button>
                      <el-switch :model-value="c.enabled" size="small" class="ml-1" @change="handleToggle(c)" />
                    </div>
                  </div>
                </div>
              </div>
            </template>

            <!-- Soft Constraints Section -->
            <template v-if="softConstraints.length > 0">
              <div class="mb-2 mt-4 flex items-center gap-2">
                <div class="h-px flex-1 bg-yellow-100" />
                <span class="text-xs font-medium text-yellow-600">软约束 ({{ softConstraints.length }})</span>
                <div class="h-px flex-1 bg-yellow-100" />
              </div>
              <div v-for="c in softConstraints" :key="c.id" class="mb-2.5">
                <div
                  class="rounded-lg border px-3.5 py-2.5 transition-colors"
                  :class="c.enabled ? 'border-gray-200 bg-white' : 'border-gray-100 bg-gray-50 opacity-60'"
                >
                  <div class="flex items-start justify-between">
                    <div class="min-w-0 flex-1">
                      <div class="flex items-center gap-2">
                        <span class="text-sm text-yellow-500">&#9675;</span>
                        <span class="truncate text-sm font-medium text-gray-900">{{ c.constraintName }}</span>
                      </div>
                      <div class="mt-1 flex flex-wrap items-center gap-2 text-xs text-gray-500">
                        <span class="rounded bg-gray-100 px-1.5 py-0.5">{{ getTypeLabel(c.constraintType) }}</span>
                        <span v-if="getParamSummary(c)">{{ getParamSummary(c) }}</span>
                      </div>
                      <div class="mt-1.5 flex items-center gap-2">
                        <div class="h-1.5 w-24 overflow-hidden rounded-full bg-gray-200">
                          <div class="h-full rounded-full bg-blue-500 transition-all" :style="{ width: c.priority + '%' }" />
                        </div>
                        <span class="text-xs text-gray-500">{{ c.priority }}</span>
                      </div>
                    </div>
                    <div class="ml-2 flex flex-shrink-0 items-center gap-1">
                      <button class="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-600" @click="openEditDialog(c)">
                        <Edit3 class="h-3.5 w-3.5" />
                      </button>
                      <button class="rounded p-1 text-gray-400 hover:bg-red-50 hover:text-red-500" @click="handleDelete(c)">
                        <Trash2 class="h-3.5 w-3.5" />
                      </button>
                      <el-switch :model-value="c.enabled" size="small" class="ml-1" @change="handleToggle(c)" />
                    </div>
                  </div>
                </div>
              </div>
            </template>
          </div>
        </div>

        <!-- Time Matrix Card -->
        <div class="rounded-xl border border-gray-200 bg-white">
          <div class="border-b border-gray-100 px-5 py-3">
            <h2 class="text-sm font-semibold text-gray-700">时间矩阵</h2>
          </div>
          <div class="p-4">
            <div v-if="matrixLoading" class="flex items-center justify-center py-16">
              <div class="h-6 w-6 animate-spin rounded-full border-2 border-blue-600 border-t-transparent" />
            </div>
            <div v-else>
              <!-- Legend -->
              <div class="mb-3 flex flex-wrap items-center gap-3 text-xs text-gray-500">
                <span class="flex items-center gap-1"><span class="inline-block h-3 w-3 rounded border border-gray-200 bg-white" /> 可用</span>
                <span class="flex items-center gap-1"><span class="inline-block h-3 w-3 rounded bg-red-200" /> 禁排</span>
                <span class="flex items-center gap-1"><span class="inline-block h-3 w-3 rounded bg-blue-200" /> 偏好</span>
                <span class="flex items-center gap-1"><span class="inline-block h-3 w-3 rounded bg-yellow-200" /> 回避</span>
              </div>
              <!-- Grid Table -->
              <table class="w-full border-collapse text-xs">
                <thead>
                  <tr>
                    <th class="border border-gray-200 bg-gray-50 px-2 py-1.5 text-center font-medium text-gray-500">节次</th>
                    <th
                      v-for="wd in displayWeekdays"
                      :key="wd.value"
                      class="border border-gray-200 bg-gray-50 px-2 py-1.5 text-center font-medium text-gray-500"
                    >{{ wd.label }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="period in DEFAULT_PERIODS" :key="period.period">
                    <td class="whitespace-nowrap border border-gray-200 bg-gray-50 px-2 py-1.5 text-center font-medium text-gray-500">
                      {{ period.name }}
                    </td>
                    <td
                      v-for="wd in displayWeekdays"
                      :key="wd.value"
                      class="border border-gray-200 px-1 py-1.5 text-center"
                      :class="getSlotClass(wd.value, period.period)"
                    >
                      <el-tooltip
                        v-if="getSlotReasons(wd.value, period.period).length > 0"
                        :content="getSlotReasons(wd.value, period.period).join('; ')"
                        placement="top"
                      >
                        <span class="cursor-help">{{ getSlotSymbol(wd.value, period.period) }}</span>
                      </el-tooltip>
                      <span v-else>{{ getSlotSymbol(wd.value, period.period) }}</span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Add/Edit Constraint Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingConstraint ? '编辑约束' : '新增约束'"
      width="560px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px" label-position="right">
        <el-form-item label="约束名称" prop="constraintName">
          <el-input v-model="form.constraintName" placeholder="输入约束名称" maxlength="50" />
        </el-form-item>

        <el-form-item label="约束类型" prop="constraintType">
          <el-select v-model="form.constraintType" placeholder="选择约束类型" class="w-full" @change="onConstraintTypeChange">
            <el-option v-for="ct in CONSTRAINT_TYPES" :key="ct.value" :value="ct.value" :label="ct.label" />
          </el-select>
        </el-form-item>

        <el-form-item label="约束强度" prop="isHard">
          <el-radio-group v-model="form.isHard">
            <el-radio :value="true">硬约束（必须满足）</el-radio>
            <el-radio :value="false">软约束（尽量满足）</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="!form.isHard" label="优先权重" prop="priority">
          <div class="flex w-full items-center gap-3">
            <el-slider v-model="form.priority" :min="1" :max="100" class="flex-1" />
            <span class="w-8 text-right text-sm font-medium text-gray-700">{{ form.priority }}</span>
          </div>
        </el-form-item>

        <el-form-item label="生效周次">
          <el-input v-model="form.effectiveWeeks" placeholder="如 1-16，留空表示全部周次" />
        </el-form-item>

        <!-- Dynamic Params Section -->
        <el-divider content-position="left">参数配置</el-divider>

        <!-- TIME_FORBIDDEN / TIME_PREFERRED / TIME_AVOIDED -->
        <template v-if="isTimeType">
          <el-form-item label="星期">
            <el-checkbox-group v-model="paramDays">
              <el-checkbox v-for="wd in WEEKDAYS" :key="wd.value" :value="wd.value" :label="wd.label" />
            </el-checkbox-group>
          </el-form-item>
          <el-form-item label="节次">
            <el-checkbox-group v-model="paramPeriods">
              <el-checkbox v-for="p in DEFAULT_PERIODS" :key="p.period" :value="p.period" :label="p.name" />
            </el-checkbox-group>
          </el-form-item>
        </template>

        <!-- TIME_FIXED -->
        <template v-if="form.constraintType === 'TIME_FIXED'">
          <el-form-item label="固定星期">
            <el-select v-model="paramFixedDay" placeholder="选择星期" class="w-full">
              <el-option v-for="wd in WEEKDAYS" :key="wd.value" :value="wd.value" :label="wd.label" />
            </el-select>
          </el-form-item>
          <el-form-item label="固定节次">
            <el-checkbox-group v-model="paramPeriods">
              <el-checkbox v-for="p in DEFAULT_PERIODS" :key="p.period" :value="p.period" :label="p.name" />
            </el-checkbox-group>
          </el-form-item>
        </template>

        <!-- MAX_DAILY -->
        <template v-if="form.constraintType === 'MAX_DAILY'">
          <el-form-item label="每日上限">
            <div class="flex items-center gap-2">
              <el-input-number v-model="paramMaxPeriods" :min="1" :max="10" />
              <span class="text-sm text-gray-500">节</span>
            </div>
          </el-form-item>
        </template>

        <!-- MAX_CONSECUTIVE -->
        <template v-if="form.constraintType === 'MAX_CONSECUTIVE'">
          <el-form-item label="最大连排">
            <div class="flex items-center gap-2">
              <el-input-number v-model="paramMaxPeriods" :min="1" :max="6" />
              <span class="text-sm text-gray-500">节</span>
            </div>
          </el-form-item>
        </template>

        <!-- SPREAD_EVEN -->
        <template v-if="form.constraintType === 'SPREAD_EVEN'">
          <el-form-item label="最小间隔">
            <div class="flex items-center gap-2">
              <el-input-number v-model="paramMinGapDays" :min="1" :max="5" />
              <span class="text-sm text-gray-500">天</span>
            </div>
          </el-form-item>
        </template>

        <!-- COMPACT_SCHEDULE -->
        <template v-if="form.constraintType === 'COMPACT_SCHEDULE'">
          <el-form-item label="最多排课">
            <div class="flex items-center gap-2">
              <el-input-number v-model="paramMaxDays" :min="1" :max="7" />
              <span class="text-sm text-gray-500">天</span>
            </div>
          </el-form-item>
        </template>

        <!-- MIN_GAP -->
        <template v-if="form.constraintType === 'MIN_GAP'">
          <el-form-item label="最小间隔">
            <div class="flex items-center gap-2">
              <el-input-number v-model="paramMinGapDays" :min="1" :max="5" />
              <span class="text-sm text-gray-500">天</span>
            </div>
          </el-form-item>
        </template>

        <!-- ROOM_REQUIRED / ROOM_PREFERRED -->
        <template v-if="form.constraintType === 'ROOM_REQUIRED' || form.constraintType === 'ROOM_PREFERRED'">
          <el-form-item label="教室类型">
            <el-input v-model="paramRoomType" placeholder="如：多媒体教室、实验室" />
          </el-form-item>
          <el-form-item label="最小容量">
            <div class="flex items-center gap-2">
              <el-input-number v-model="paramMinCapacity" :min="0" :max="500" />
              <span class="text-sm text-gray-500">人</span>
            </div>
          </el-form-item>
        </template>

        <!-- MORNING_PRIORITY -->
        <template v-if="form.constraintType === 'MORNING_PRIORITY'">
          <el-form-item label="优先节次">
            <el-checkbox-group v-model="paramPeriods">
              <el-checkbox v-for="p in DEFAULT_PERIODS.slice(0, 4)" :key="p.period" :value="p.period" :label="p.name" />
            </el-checkbox-group>
          </el-form-item>
        </template>

        <!-- No params hint -->
        <div v-if="!form.constraintType" class="py-4 text-center text-sm text-gray-400">
          请先选择约束类型以配置参数
        </div>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Plus, Settings, Edit3, Trash2 } from 'lucide-vue-next'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { constraintApi, semesterApi } from '@/api/teaching'
import { courseApi } from '@/api/academic'
import { http } from '@/utils/request'
import type { SchedulingConstraint, Semester, TimeSlotStatus, Course } from '@/types/teaching'
import { CONSTRAINT_LEVELS, CONSTRAINT_TYPES, WEEKDAYS, DEFAULT_PERIODS } from '@/types/teaching'

// ==================== State ====================

const semesters = ref<Semester[]>([])
const semesterId = ref<number | string>('')
const activeLevel = ref<1 | 2 | 3 | 4>(1)
const targetId = ref<number | string>('')
const constraints = ref<SchedulingConstraint[]>([])
const loading = ref(false)
const matrixLoading = ref(false)

// Target option lists
const teachers = ref<{ value: number | string; label: string }[]>([])
const classes = ref<{ value: number | string; label: string }[]>([])
const courses = ref<{ value: number | string; label: string }[]>([])

// Time matrix
const timeMatrix = ref<TimeSlotStatus[][]>([])
const flatMatrix = computed<Map<string, TimeSlotStatus>>(() => {
  const map = new Map<string, TimeSlotStatus>()
  for (const row of timeMatrix.value) {
    if (!Array.isArray(row)) continue
    for (const slot of row) {
      if (slot) map.set(`${slot.day}-${slot.period}`, slot)
    }
  }
  return map
})

// Dialog
const dialogVisible = ref(false)
const editingConstraint = ref<SchedulingConstraint | null>(null)
const saving = ref(false)
const formRef = ref<FormInstance>()

const form = ref({
  constraintName: '',
  constraintType: '' as string,
  isHard: true,
  priority: 50,
  effectiveWeeks: '',
})

// Dynamic params
const paramDays = ref<number[]>([])
const paramPeriods = ref<number[]>([])
const paramFixedDay = ref<number | undefined>()
const paramMaxPeriods = ref(4)
const paramMinGapDays = ref(1)
const paramMaxDays = ref(5)
const paramRoomType = ref('')
const paramMinCapacity = ref(0)

const formRules: FormRules = {
  constraintName: [{ required: true, message: '请输入约束名称', trigger: 'blur' }],
  constraintType: [{ required: true, message: '请选择约束类型', trigger: 'change' }],
}

// ==================== Computed ====================

const hardConstraints = computed(() => constraints.value.filter(c => c.isHard))
const softConstraints = computed(() => constraints.value.filter(c => !c.isHard))
const enabledCount = computed(() => constraints.value.filter(c => c.enabled).length)

const targetLabel = computed(() => {
  switch (activeLevel.value) {
    case 2: return '教师'
    case 3: return '班级'
    case 4: return '课程'
    default: return ''
  }
})

const targetOptions = computed(() => {
  switch (activeLevel.value) {
    case 2: return teachers.value
    case 3: return classes.value
    case 4: return courses.value
    default: return []
  }
})

const displayWeekdays = computed(() => WEEKDAYS.filter(w => w.value <= 5))

const isTimeType = computed(() =>
  ['TIME_FORBIDDEN', 'TIME_PREFERRED', 'TIME_AVOIDED'].includes(form.value.constraintType)
)

// ==================== Data loading ====================

async function loadSemesters() {
  try {
    const data = await semesterApi.list()
    semesters.value = data
    const current = data.find(s => s.isCurrent)
    if (current) {
      semesterId.value = current.id
    } else if (data.length > 0) {
      semesterId.value = data[0].id
    }
  } catch {
    ElMessage.error('加载学期列表失败')
  }
}

async function loadTargetLists() {
  try {
    const [teacherRes, classRes, courseRes] = await Promise.allSettled([
      http.get<any>('/users', { params: { role: 'TEACHER', pageSize: 500 } }),
      http.get<any>('/organization/classes/list'),
      courseApi.listAll(),
    ])

    if (teacherRes.status === 'fulfilled') {
      const list = Array.isArray(teacherRes.value) ? teacherRes.value : (teacherRes.value?.records || [])
      teachers.value = list.map((t: any) => ({
        value: t.id,
        label: t.realName || t.name || t.username || `教师${t.id}`,
      }))
    }

    if (classRes.status === 'fulfilled') {
      const list = Array.isArray(classRes.value) ? classRes.value : (classRes.value?.records || [])
      classes.value = list.map((c: any) => ({
        value: c.id,
        label: c.name || c.className || `班级${c.id}`,
      }))
    }

    if (courseRes.status === 'fulfilled') {
      const list = Array.isArray(courseRes.value) ? courseRes.value : []
      courses.value = list.map((c: Course) => ({
        value: c.id,
        label: `${c.courseName} (${c.courseCode})`,
      }))
    }
  } catch {
    // Target lists are not critical
  }
}

async function loadConstraints() {
  if (!semesterId.value) return
  // For level > 1 without target, show empty
  if (activeLevel.value > 1 && !targetId.value) {
    constraints.value = []
    timeMatrix.value = []
    return
  }
  loading.value = true
  try {
    const params: any = { semesterId: semesterId.value, level: activeLevel.value }
    if (activeLevel.value > 1 && targetId.value) {
      params.targetId = targetId.value
    }
    const data = await constraintApi.list(params)
    constraints.value = Array.isArray(data) ? data : []
  } catch {
    ElMessage.error('加载约束列表失败')
    constraints.value = []
  } finally {
    loading.value = false
  }
  loadTimeMatrix()
}

async function loadTimeMatrix() {
  if (!semesterId.value) return
  if (activeLevel.value > 1 && !targetId.value) {
    timeMatrix.value = []
    return
  }
  matrixLoading.value = true
  try {
    const params: any = { semesterId: semesterId.value, level: activeLevel.value }
    if (activeLevel.value > 1 && targetId.value) {
      params.targetId = targetId.value
    }
    const data = await constraintApi.getTimeMatrix(params)
    timeMatrix.value = Array.isArray(data) ? data : []
  } catch {
    timeMatrix.value = []
  } finally {
    matrixLoading.value = false
  }
}

// ==================== Time Matrix helpers ====================

function getSlotClass(day: number, period: number): string {
  const slot = flatMatrix.value.get(`${day}-${period}`)
  if (!slot) return 'bg-white'
  switch (slot.status) {
    case 'forbidden': return 'bg-red-100 text-red-600'
    case 'preferred': return 'bg-blue-100 text-blue-600'
    case 'avoided': return 'bg-yellow-100 text-yellow-600'
    default: return 'bg-white'
  }
}

function getSlotSymbol(day: number, period: number): string {
  const slot = flatMatrix.value.get(`${day}-${period}`)
  if (!slot) return ''
  switch (slot.status) {
    case 'forbidden': return '禁'
    case 'preferred': return '优'
    case 'avoided': return '避'
    default: return ''
  }
}

function getSlotReasons(day: number, period: number): string[] {
  const slot = flatMatrix.value.get(`${day}-${period}`)
  return slot?.reasons || []
}

// ==================== Tab / selector switching ====================

function onSemesterChange() {
  loadConstraints()
}

function switchLevel(level: number) {
  activeLevel.value = level as 1 | 2 | 3 | 4
  targetId.value = ''
  if (level === 1) {
    loadConstraints()
  } else {
    constraints.value = []
    timeMatrix.value = []
  }
}

function onTargetChange() {
  loadConstraints()
}

// ==================== Constraint type label / param summary ====================

function getTypeLabel(type: string): string {
  return CONSTRAINT_TYPES.find(t => t.value === type)?.label || type
}

function dayNames(days: number[]): string {
  return days.map(d => WEEKDAYS.find(w => w.value === d)?.label || `${d}`).join('、')
}

function periodNames(periods: number[]): string {
  if (!Array.isArray(periods) || periods.length === 0) return ''
  const sorted = [...periods].sort((a, b) => a - b)
  if (sorted.length <= 3) return sorted.map(p => `第${p}节`).join('、')
  return `第${sorted[0]}-${sorted[sorted.length - 1]}节`
}

function getParamSummary(c: SchedulingConstraint): string {
  const p = c.params || {}
  const ct = c.constraintType

  if (['TIME_FORBIDDEN', 'TIME_PREFERRED', 'TIME_AVOIDED'].includes(ct)) {
    const parts: string[] = []
    if (Array.isArray(p.days) && p.days.length) parts.push(dayNames(p.days))
    if (Array.isArray(p.periods) && p.periods.length) parts.push(periodNames(p.periods))
    return parts.join(' ') || ''
  }
  if (ct === 'TIME_FIXED') {
    const parts: string[] = []
    if (p.day) parts.push(WEEKDAYS.find(w => w.value === p.day)?.label || '')
    if (Array.isArray(p.periods) && p.periods.length) parts.push(periodNames(p.periods))
    return parts.join(' ') || ''
  }
  if (ct === 'MAX_DAILY') return `每日最多 ${p.maxPeriods ?? '-'} 节`
  if (ct === 'MAX_CONSECUTIVE') return `最多连排 ${p.maxPeriods ?? '-'} 节`
  if (ct === 'SPREAD_EVEN' || ct === 'MIN_GAP') return `最小间隔 ${p.minGapDays ?? '-'} 天`
  if (ct === 'COMPACT_SCHEDULE') return `最多 ${p.maxDays ?? '-'} 天排课`
  if (ct === 'ROOM_REQUIRED' || ct === 'ROOM_PREFERRED') {
    const parts: string[] = []
    if (p.roomType) parts.push(p.roomType)
    if (p.minCapacity) parts.push(`容量>=${p.minCapacity}`)
    return parts.join('，') || ''
  }
  if (ct === 'MORNING_PRIORITY') {
    if (Array.isArray(p.periods) && p.periods.length) return periodNames(p.periods)
    return '上午1-4节'
  }
  return ''
}

// ==================== Dialog helpers ====================

function resetParams() {
  paramDays.value = []
  paramPeriods.value = []
  paramFixedDay.value = undefined
  paramMaxPeriods.value = 4
  paramMinGapDays.value = 1
  paramMaxDays.value = 5
  paramRoomType.value = ''
  paramMinCapacity.value = 0
}

function onConstraintTypeChange() {
  const ct = CONSTRAINT_TYPES.find(t => t.value === form.value.constraintType)
  if (ct) {
    form.value.isHard = ct.isHardDefault
  }
  resetParams()
}

function buildParams(): Record<string, any> {
  const ct = form.value.constraintType
  const result: Record<string, any> = {}

  if (['TIME_FORBIDDEN', 'TIME_PREFERRED', 'TIME_AVOIDED'].includes(ct)) {
    result.days = paramDays.value
    result.periods = paramPeriods.value
  } else if (ct === 'TIME_FIXED') {
    result.day = paramFixedDay.value
    result.periods = paramPeriods.value
  } else if (ct === 'MAX_DAILY' || ct === 'MAX_CONSECUTIVE') {
    result.maxPeriods = paramMaxPeriods.value
  } else if (ct === 'SPREAD_EVEN' || ct === 'MIN_GAP') {
    result.minGapDays = paramMinGapDays.value
  } else if (ct === 'COMPACT_SCHEDULE') {
    result.maxDays = paramMaxDays.value
  } else if (ct === 'ROOM_REQUIRED' || ct === 'ROOM_PREFERRED') {
    result.roomType = paramRoomType.value
    result.minCapacity = paramMinCapacity.value
  } else if (ct === 'MORNING_PRIORITY') {
    result.periods = paramPeriods.value
  }
  return result
}

function loadParamsFromConstraint(c: SchedulingConstraint) {
  resetParams()
  const p = c.params || {}
  const ct = c.constraintType

  if (['TIME_FORBIDDEN', 'TIME_PREFERRED', 'TIME_AVOIDED'].includes(ct)) {
    paramDays.value = Array.isArray(p.days) ? [...p.days] : []
    paramPeriods.value = Array.isArray(p.periods) ? [...p.periods] : []
  } else if (ct === 'TIME_FIXED') {
    paramFixedDay.value = p.day
    paramPeriods.value = Array.isArray(p.periods) ? [...p.periods] : []
  } else if (ct === 'MAX_DAILY' || ct === 'MAX_CONSECUTIVE') {
    paramMaxPeriods.value = p.maxPeriods ?? 4
  } else if (ct === 'SPREAD_EVEN' || ct === 'MIN_GAP') {
    paramMinGapDays.value = p.minGapDays ?? 1
  } else if (ct === 'COMPACT_SCHEDULE') {
    paramMaxDays.value = p.maxDays ?? 5
  } else if (ct === 'ROOM_REQUIRED' || ct === 'ROOM_PREFERRED') {
    paramRoomType.value = p.roomType ?? ''
    paramMinCapacity.value = p.minCapacity ?? 0
  } else if (ct === 'MORNING_PRIORITY') {
    paramPeriods.value = Array.isArray(p.periods) ? [...p.periods] : []
  }
}

function openAddDialog() {
  editingConstraint.value = null
  form.value = {
    constraintName: '',
    constraintType: '',
    isHard: true,
    priority: 50,
    effectiveWeeks: '',
  }
  resetParams()
  dialogVisible.value = true
}

function openEditDialog(c: SchedulingConstraint) {
  editingConstraint.value = c
  form.value = {
    constraintName: c.constraintName,
    constraintType: c.constraintType,
    isHard: c.isHard,
    priority: c.priority,
    effectiveWeeks: c.effectiveWeeks || '',
  }
  loadParamsFromConstraint(c)
  dialogVisible.value = true
}

// ==================== CRUD operations ====================

async function handleSave() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  const payload: Partial<SchedulingConstraint> = {
    semesterId: semesterId.value as number,
    constraintName: form.value.constraintName,
    constraintLevel: activeLevel.value,
    constraintType: form.value.constraintType,
    isHard: form.value.isHard,
    priority: form.value.isHard ? 100 : form.value.priority,
    params: buildParams(),
    effectiveWeeks: form.value.effectiveWeeks || undefined,
    enabled: true,
  }
  if (activeLevel.value > 1 && targetId.value) {
    payload.targetId = targetId.value as number
  }

  saving.value = true
  try {
    if (editingConstraint.value) {
      await constraintApi.update(editingConstraint.value.id, payload)
      ElMessage.success('约束已更新')
    } else {
      await constraintApi.create(payload)
      ElMessage.success('约束已创建')
    }
    dialogVisible.value = false
    loadConstraints()
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(c: SchedulingConstraint) {
  try {
    await ElMessageBox.confirm(`确定删除约束"${c.constraintName}"？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await constraintApi.delete(c.id)
    ElMessage.success('已删除')
    loadConstraints()
  } catch {
    // user cancelled
  }
}

async function handleToggle(c: SchedulingConstraint) {
  try {
    if (c.enabled) {
      await constraintApi.disable(c.id)
      ElMessage.success('已禁用')
    } else {
      await constraintApi.enable(c.id)
      ElMessage.success('已启用')
    }
    loadConstraints()
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  }
}

// ==================== Lifecycle ====================

onMounted(async () => {
  await loadSemesters()
  await loadTargetLists()
  if (semesterId.value) {
    loadConstraints()
  }
})
</script>
