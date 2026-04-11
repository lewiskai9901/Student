<template>
  <div>
    <!-- Stats bar + Actions -->
    <div class="tm-filters" style="justify-content: space-between;">
      <div class="tm-stats" style="margin: 0;">
        <span>共 <b>{{ totalCount }}</b> 条</span>
        <span class="sep" />
        <span>已分配 <b>{{ assignedCount }}/{{ totalCount }}</b></span>
        <span class="sep" />
        <span>未排课 <b>{{ unscheduledCount }}</b></span>
      </div>
      <div style="display: flex; align-items: center; gap: 8px;">
        <div class="view-toggle">
          <button :class="['vt-btn', viewMode === 'card' && 'vt-active']" @click="viewMode = 'card'" title="卡片视图">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></svg>
          </button>
          <button :class="['vt-btn', viewMode === 'table' && 'vt-active']" @click="viewMode = 'table'" title="表格视图">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="18" x2="21" y2="18"/></svg>
          </button>
        </div>
        <button class="tm-btn tm-btn-workflow" :disabled="!semesterId" @click="handleGenerateTasks">从开课计划生成任务</button>
        <button class="tm-btn tm-btn-secondary" :disabled="!semesterId || filteredTasks.length === 0" @click="handleBatchAssign">批量分配教师</button>
      </div>
    </div>

    <!-- Loading / Empty -->
    <div v-if="loading" class="tm-empty" style="padding: 40px;">
      <span class="tm-spin" style="display: inline-block; width: 16px; height: 16px; border: 2px solid #e5e7eb; border-top-color: #2563eb; border-radius: 50%;" />
      加载中...
    </div>
    <div v-else-if="!semesterId" class="tm-empty" style="padding: 40px;">请先选择学期</div>
    <div v-else-if="filteredTasks.length === 0" class="tm-empty" style="padding: 40px;">暂无教学任务，点击"从开课计划生成任务"开始</div>

    <!-- ========== TABLE VIEW ========== -->
    <div v-else-if="viewMode === 'table'" class="tm-table-wrap compact-table-wrap">
      <table class="tm-table compact-table">
        <thead>
          <tr>
            <th class="text-left" style="width: 14%;">课程</th>
            <th style="width: 11%;">班级</th>
            <th style="width: 4%;">人数</th>
            <th style="width: 5%;">周课时</th>
            <th style="width: 7%;">教学周</th>
            <th style="width: 10%;">教室需求</th>
            <th style="width: 6%;">连排</th>
            <th style="width: 6%;">性质</th>
            <th style="width: 5%;">考核</th>
            <th style="width: 13%;">教师</th>
            <th style="width: 7%;">状态</th>
            <th style="width: 12%;">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="task in filteredTasks" :key="'t-'+task.id" class="ct-row">
            <td class="text-left">
              <span class="ct-course">{{ task.courseName }}</span>
              <span class="ct-code">{{ task.courseCode }}</span>
            </td>
            <td>{{ (task.className || '').replace(/班$/, '') }}</td>
            <td class="tm-mono">{{ task.studentCount }}</td>
            <td class="tm-mono">{{ task.weeklyHours }}</td>
            <td class="tm-mono">{{ task.startWeek }}-{{ task.endWeek }}</td>
            <td>
              <select class="ct-select" :value="task.roomTypeRequired || ''" @change="updateTaskField(task, 'roomTypeRequired', ($event.target as HTMLSelectElement).value)">
                <option value="">不限</option>
                <option v-for="rt in roomTypes" :key="rt.typeCode" :value="rt.typeCode">{{ rt.typeName }}</option>
              </select>
            </td>
            <td>
              <select class="ct-select" :value="task.consecutivePeriods ?? 2" @change="updateTaskField(task, 'consecutivePeriods', Number(($event.target as HTMLSelectElement).value))">
                <option :value="1">不连排</option>
                <option :value="2">2节连</option>
                <option :value="3">3节连</option>
                <option :value="4">4节连</option>
              </select>
            </td>
            <td>
              <select class="ct-select" :value="task.courseNature ?? 1" @change="updateTaskField(task, 'courseNature', Number(($event.target as HTMLSelectElement).value))">
                <option :value="1">理论</option>
                <option :value="2">实验</option>
                <option :value="3">实践</option>
                <option :value="4">理+实</option>
              </select>
            </td>
            <td>
              <span class="ct-assess" :class="'ct-a-' + (task.assessmentMethod ?? 1)">{{ getAssessmentLabel(task.assessmentMethod) }}</span>
            </td>
            <td>
              <template v-if="task.teacherName">
                <span class="ct-teacher" :title="task.teacherName">{{ task.teacherName }}</span>
                <button class="ct-reassign" title="重新分配" @click="quickReassign(task)">换</button>
              </template>
              <el-select
                v-else
                :model-value="undefined"
                filterable
                placeholder="搜索教师"
                size="small"
                style="width: 120px;"
                @change="(v: any) => quickAssign(task, v)"
              >
                <el-option v-for="u in teachers" :key="u.id" :value="u.id" :label="u.realName || u.username" />
              </el-select>
            </td>
            <td>
              <span :class="['ct-status', 'ct-s-' + task.taskStatus]">{{ getStatusName(task.taskStatus) }}</span>
            </td>
            <td>
              <button class="tm-action tm-action-danger" style="font-size: 11px;" @click="handleDelete(task)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- ========== CARD VIEW ========== -->
    <div v-else class="task-cards">
      <div v-for="task in filteredTasks" :key="task.id" class="task-card">
        <!-- Card Header -->
        <div class="task-card-header">
          <div class="task-card-title">
            <span class="course-name">{{ task.courseName || '未知课程' }}</span>
            <span v-if="task.courseCode" class="course-code">{{ task.courseCode }}</span>
          </div>
          <span :class="['tm-chip', statusChipClass(task.taskStatus)]">
            {{ getStatusName(task.taskStatus) }}
          </span>
        </div>

        <!-- Info Row -->
        <div class="task-card-info">
          <span>{{ task.className || '未知班级' }}</span>
          <span class="sep" />
          <span>{{ task.studentCount }}人</span>
          <span class="sep" />
          <span>{{ task.weeklyHours }}课时/周</span>
          <span class="sep" />
          <span>{{ task.startWeek }}-{{ task.endWeek }}周</span>
        </div>

        <!-- Config Row: room type / consecutive / nature -->
        <div class="task-card-config">
          <div class="config-item">
            <label>教室需求</label>
            <select
              class="tm-input config-select"
              :value="task.roomTypeRequired || ''"
              @change="updateTaskField(task, 'roomTypeRequired', ($event.target as HTMLSelectElement).value)"
            >
              <option value="">不限</option>
              <option v-for="rt in roomTypes" :key="rt.typeCode" :value="rt.typeCode">{{ rt.typeName }}</option>
            </select>
          </div>
          <div class="config-item">
            <label>连排</label>
            <select
              class="tm-input config-select"
              :value="task.consecutivePeriods ?? 2"
              @change="updateTaskField(task, 'consecutivePeriods', Number(($event.target as HTMLSelectElement).value))"
            >
              <option :value="1">不连排</option>
              <option :value="2">2节连排</option>
              <option :value="3">3节连排</option>
              <option :value="4">4节连排</option>
            </select>
          </div>
          <div class="config-item">
            <label>性质</label>
            <select
              class="tm-input config-select"
              :value="task.courseNature ?? 1"
              @change="updateTaskField(task, 'courseNature', Number(($event.target as HTMLSelectElement).value))"
            >
              <option :value="1">理论课</option>
              <option :value="2">实验课</option>
              <option :value="3">实践课</option>
              <option :value="4">理论+实验</option>
            </select>
          </div>
        </div>

        <!-- Teacher Assignment Area -->
        <div class="task-card-teachers">
          <div class="teacher-label">教师分配</div>
          <template v-if="getTaskEditState(task).teachers.length > 0">
            <div v-for="(t, idx) in getTaskEditState(task).teachers" :key="idx" class="teacher-row">
              <el-select
                :model-value="t.teacherId || undefined"
                filterable
                placeholder="搜索教师"
                size="small"
                style="width: 150px;"
                @change="(v: any) => onEditTeacher(task, idx, 'teacherId', v)"
              >
                <el-option v-for="u in teachers" :key="u.id" :value="u.id" :label="u.realName || u.username" />
              </el-select>
              <span
                class="role-toggle"
                :class="{ 'is-main': t.role === 1 }"
                @click="onEditTeacher(task, idx, 'role', t.role === 1 ? 2 : 1)"
              >
                {{ t.role === 1 ? '主讲' : '辅助' }}
              </span>
              <span class="hours-group">
                <label>周课时</label>
                <input
                  type="number"
                  class="tm-input hours-input"
                  :value="t.weeklyHours"
                  min="0"
                  @input="onEditTeacher(task, idx, 'weeklyHours', Number(($event.target as HTMLInputElement).value))"
                />
              </span>
              <button class="tm-action tm-action-danger" @click="removeEditTeacher(task, idx)">删除</button>
            </div>
          </template>
          <div v-else class="no-teacher">未分配</div>

          <div class="teacher-footer">
            <button class="tm-action" @click="addEditTeacher(task)">+ 添加教师</button>
            <span v-if="getTaskEditState(task).teachers.length > 0" class="hours-summary" :class="hoursMatch(task) ? 'hours-ok' : 'hours-warn'">
              课时合计: {{ totalTeacherHours(task) }}/{{ task.weeklyHours }}
              {{ hoursMatch(task) ? '' : '(不匹配)' }}
            </span>
            <button
              v-if="isTaskDirty(task)"
              class="tm-btn tm-btn-primary tm-btn-sm"
              :disabled="savingTaskId === task.id"
              @click="saveTeacherAssignment(task)"
            >
              {{ savingTaskId === task.id ? '保存中...' : '保存分配' }}
            </button>
          </div>
        </div>

        <!-- Card Actions -->
        <div class="task-card-actions">
          <button class="tm-action tm-action-danger" @click="handleDelete(task)">删除任务</button>
        </div>
      </div>
    </div>

    <!-- Footer -->
    <div v-if="filteredTasks.length > 0" style="display: flex; align-items: center; justify-content: space-between; padding: 16px 0;">
      <div class="tm-stats" style="margin: 0;">
        <span>进度</span>
        <span class="sep" />
        <span>
          <span style="display: inline-block; width: 120px; height: 6px; background: #e5e7eb; border-radius: 3px; vertical-align: middle; margin-right: 6px; overflow: hidden;">
            <span :style="{ display: 'block', width: progressPercent + '%', height: '100%', background: allAssigned ? '#059669' : '#2563eb', borderRadius: '3px', transition: 'width 0.3s' }" />
          </span>
          {{ assignedCount }}/{{ totalCount }} 已分配
        </span>
      </div>
      <button v-if="allAssigned" class="tm-btn tm-btn-primary" @click="router.push('/teaching/scheduling')">
        全部已就绪 — 进入排课 →
      </button>
    </div>

    <!-- Batch Assign Drawer -->
    <Teleport to="body">
      <Transition name="tm-drawer">
        <div v-if="batchAssignVisible" class="tm-drawer-overlay" @click.self="batchAssignVisible = false">
          <div class="tm-drawer" style="width: 480px;">
            <div class="tm-drawer-header">
              <h2 class="tm-drawer-title">批量分配教师</h2>
              <button class="tm-drawer-close" @click="batchAssignVisible = false">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
            <div class="tm-drawer-body">
              <div class="tm-section">
                <h3 class="tm-section-title">选择教师</h3>
                <div class="tm-field">
                  <label class="tm-label">教师 <span class="req">*</span></label>
                  <el-select v-model="batchTeacherId" filterable placeholder="搜索教师" style="width: 100%;">
                    <el-option v-for="t in teachers" :key="t.id" :value="t.id" :label="t.realName || t.username" />
                  </el-select>
                </div>
                <div class="tm-field" style="margin-top: 12px;">
                  <label class="tm-label">周课时</label>
                  <input v-model.number="batchWeeklyHours" type="number" class="tm-input" style="width: 100%;" min="0" placeholder="留空则使用任务总周课时" />
                </div>
                <p style="font-size: 12px; color: #6b7280; margin-top: 8px;">
                  将为当前范围内所有未分配教师的任务（共 {{ unassignedCount }} 条）统一分配选中的教师为主讲。
                </p>
              </div>
            </div>
            <div class="tm-drawer-footer">
              <button class="tm-btn tm-btn-secondary" @click="batchAssignVisible = false">取消</button>
              <button class="tm-btn tm-btn-primary" :disabled="!batchTeacherId || batchAssigning" @click="executeBatchAssign">
                <span v-if="batchAssigning" class="tm-spin" style="display: inline-block; width: 14px; height: 14px; border: 2px solid rgba(255,255,255,0.3); border-top-color: #fff; border-radius: 50%;" />
                确定分配 ({{ unassignedCount }} 条)
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { teachingTaskApi, workflowApi } from '@/api/teaching'
import { getSimpleUserList } from '@/api/user'
import { http } from '@/utils/request'
import type { TeachingTask } from '@/types/teaching'
import type { SimpleUser } from '@/types/user'

const props = defineProps<{
  semesterId: number | string | undefined
  selectedOrg?: { type: string; id: number | string; name: string; classIds?: (number | string)[] }
}>()

const router = useRouter()

// ==================== State ====================

const tasks = ref<TeachingTask[]>([])
const teachers = ref<SimpleUser[]>([])
const roomTypes = ref<{ typeCode: string; typeName: string }[]>([])
const viewMode = ref<'card' | 'table'>('table')
const loading = ref(false)
const savingTaskId = ref<number | string | null>(null)
const batchAssignVisible = ref(false)
const batchTeacherId = ref<number | string>('')
const batchWeeklyHours = ref<number | null>(null)
const batchAssigning = ref(false)

// Per-task edit state for teacher assignments
interface TeacherEditRow {
  teacherId: number | string
  role: number
  weeklyHours: number | null
}

const taskEditStates = reactive<Record<string, { teachers: TeacherEditRow[]; dirty: boolean }>>({})

function getTaskEditState(task: TeachingTask) {
  const key = String(task.id)
  if (!taskEditStates[key]) {
    // Initialize from server data
    const serverTeachers = task.teachers || []
    taskEditStates[key] = {
      teachers: serverTeachers.map(t => ({
        teacherId: t.teacher_id || t.teacherId || '',
        role: t.teacher_role || t.role || 2,
        weeklyHours: t.weekly_hours ?? t.weeklyHours ?? null,
      })),
      dirty: false,
    }
  }
  return taskEditStates[key]
}

function resetEditState(task: TeachingTask) {
  const key = String(task.id)
  delete taskEditStates[key]
}

function isTaskDirty(task: TeachingTask) {
  const state = taskEditStates[String(task.id)]
  return state?.dirty ?? false
}

function totalTeacherHours(task: TeachingTask) {
  const state = getTaskEditState(task)
  return state.teachers.reduce((sum, t) => sum + (t.weeklyHours || 0), 0)
}

function hoursMatch(task: TeachingTask) {
  return totalTeacherHours(task) === task.weeklyHours
}

// ==================== Edit Operations ====================

function addEditTeacher(task: TeachingTask) {
  const state = getTaskEditState(task)
  state.teachers.push({ teacherId: '', role: 2, weeklyHours: null })
  state.dirty = true
}

function removeEditTeacher(task: TeachingTask, idx: number) {
  const state = getTaskEditState(task)
  state.teachers.splice(idx, 1)
  state.dirty = true
}

function onEditTeacher(task: TeachingTask, idx: number, field: string, value: any) {
  const state = getTaskEditState(task)
  const row = state.teachers[idx]
  if (field === 'teacherId') row.teacherId = value
  else if (field === 'role') row.role = value
  else if (field === 'weeklyHours') row.weeklyHours = value
  state.dirty = true
}

// ==================== Data Loading ====================

async function loadTasks() {
  if (!props.semesterId) {
    tasks.value = []
    return
  }
  loading.value = true
  try {
    const res: any = await teachingTaskApi.list({
      semesterId: props.semesterId,
      page: 1,
      size: 500,
    })
    const data = res.data || res
    tasks.value = data.records || data || []
    // Reset edit states to sync with server
    for (const key of Object.keys(taskEditStates)) {
      delete taskEditStates[key]
    }
  } catch {
    tasks.value = []
  } finally {
    loading.value = false
  }
}

async function loadTeachers() {
  try {
    const list = await getSimpleUserList()
    // 非学生排前面，学生排后面（有 filterable 搜索所以保留全部）
    teachers.value = (list || []).sort((a, b) => {
      const aIsStudent = a.userType === 'STUDENT' ? 1 : 0
      const bIsStudent = b.userType === 'STUDENT' ? 1 : 0
      return aIsStudent - bIsStudent
    })
  } catch {
    teachers.value = []
  }
}

async function loadRoomTypes() {
  try {
    const res: any = await http.get('/entity-type-configs', { params: { entityType: 'PLACE' } })
    const data = res.data || res
    roomTypes.value = (Array.isArray(data) ? data : data?.records || []).map((r: any) => ({
      typeCode: r.typeCode || r.type_code,
      typeName: r.typeName || r.type_name,
    }))
  } catch {
    roomTypes.value = []
  }
}

// ==================== Filtering ====================

const filteredTasks = computed(() => {
  if (!props.selectedOrg?.id) return tasks.value
  const org = props.selectedOrg
  // Use classIds set for precise filtering at any tree level
  if (org.classIds && org.classIds.length > 0) {
    const idSet = new Set(org.classIds.map(String))
    return tasks.value.filter(t => idSet.has(String(t.orgUnitId)))
  }
  return tasks.value
})

// ==================== Stats ====================

const totalCount = computed(() => filteredTasks.value.length)
const assignedCount = computed(() => filteredTasks.value.filter(t => t.teacherName || (t.teachers && t.teachers.length > 0)).length)
const unscheduledCount = computed(() => filteredTasks.value.filter(t => (t.taskStatus ?? 0) < 2).length)
const unassignedCount = computed(() => filteredTasks.value.filter(t => !t.teacherName && (!t.teachers || t.teachers.length === 0)).length)
const allAssigned = computed(() => totalCount.value > 0 && assignedCount.value === totalCount.value)
const progressPercent = computed(() => totalCount.value > 0 ? Math.round((assignedCount.value / totalCount.value) * 100) : 0)

// ==================== Actions ====================

async function quickAssign(task: TeachingTask, teacherId: string) {
  if (!teacherId) return
  try {
    await teachingTaskApi.assignTeachers(task.id, [{
      teacherId,
      role: 1,
      weeklyHours: task.weeklyHours,
    }])
    ElMessage.success('已分配教师')
    await loadTasks()
  } catch {
    ElMessage.error('分配失败')
  }
}

async function quickReassign(task: TeachingTask) {
  try {
    await ElMessageBox.confirm(
      `确定要重新分配「${task.courseName}」的教师吗？将清除当前分配。`,
      '重新分配', { type: 'warning' }
    )
    await teachingTaskApi.assignTeachers(task.id, [])
    ElMessage.success('已清除教师，请重新选择')
    await loadTasks()
  } catch { /* cancelled */ }
}

async function updateTaskField(task: TeachingTask, field: string, value: any) {
  try {
    await teachingTaskApi.update(task.id, { [field]: value })
    // Update local state
    ;(task as any)[field] = value
  } catch {
    ElMessage.error('更新失败')
  }
}

async function saveTeacherAssignment(task: TeachingTask) {
  const state = getTaskEditState(task)
  const validTeachers = state.teachers.filter(t => t.teacherId)
  savingTaskId.value = task.id
  try {
    await teachingTaskApi.assignTeachers(
      task.id,
      validTeachers.map(t => ({
        teacherId: t.teacherId,
        role: t.role,
        weeklyHours: t.weeklyHours ?? undefined,
      }))
    )
    ElMessage.success('教师分配已保存')
    resetEditState(task)
    await loadTasks()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    savingTaskId.value = null
  }
}

async function handleDelete(task: TeachingTask) {
  try {
    await ElMessageBox.confirm(`确定删除教学任务「${task.courseName} - ${task.className}」？`, '删除确认', { type: 'warning' })
    await teachingTaskApi.delete(task.id)
    ElMessage.success('已删除')
    await loadTasks()
  } catch {
    // cancelled or error
  }
}

async function handleGenerateTasks() {
  if (!props.semesterId) return
  try {
    await ElMessageBox.confirm(
      '将从已确认的开课计划批量生成教学任务，已存在的不会重复创建。',
      '生成教学任务',
      { type: 'info' }
    )
    const res: any = await workflowApi.generateTasks(props.semesterId)
    const count = res?.generated ?? res?.data?.generated ?? 0
    ElMessage.success(`已生成 ${count} 个教学任务`)
    await loadTasks()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e?.message || '生成失败')
  }
}

function handleBatchAssign() {
  batchTeacherId.value = ''
  batchWeeklyHours.value = null
  batchAssignVisible.value = true
}

async function executeBatchAssign() {
  if (!batchTeacherId.value) return
  const unassigned = filteredTasks.value.filter(t => !t.teacherName && (!t.teachers || t.teachers.length === 0))
  if (unassigned.length === 0) {
    ElMessage.info('没有需要分配的任务')
    batchAssignVisible.value = false
    return
  }
  batchAssigning.value = true
  let success = 0
  let failed = 0
  try {
    for (const task of unassigned) {
      try {
        const hours = batchWeeklyHours.value ?? task.weeklyHours
        await teachingTaskApi.assignTeachers(task.id, [{
          teacherId: batchTeacherId.value,
          role: 1,
          weeklyHours: hours,
        }])
        success++
      } catch {
        failed++
      }
    }
    ElMessage.success(`批量分配完成：成功 ${success} 条${failed > 0 ? `，失败 ${failed} 条` : ''}`)
    batchAssignVisible.value = false
    await loadTasks()
  } finally {
    batchAssigning.value = false
  }
}

// ==================== Helpers ====================

function getStatusName(status: number) {
  const names: Record<number, string> = {
    0: '待落实',
    1: '已分配教师',
    2: '已排课',
    3: '进行中',
    4: '已结束',
    9: '已取消',
  }
  return names[status] ?? '未知'
}

function getAssessmentLabel(val?: number) {
  const map: Record<number, string> = { 1: '考试', 2: '考查', 3: '技能考试', 4: '考试+考查' }
  return map[val ?? 1] ?? '考试'
}

function statusChipClass(status: number) {
  const map: Record<number, string> = {
    0: 'tm-chip-gray',
    1: 'tm-chip-blue',
    2: 'tm-chip-green',
    3: 'tm-chip-amber',
    4: 'tm-chip-red',
    9: 'tm-chip-red',
  }
  return map[status] ?? 'tm-chip-gray'
}

// ==================== Watchers & Lifecycle ====================

watch(() => props.semesterId, () => {
  loadTasks()
})

onMounted(() => {
  loadTasks()
  loadTeachers()
  loadRoomTypes()
})

defineExpose({ reload: loadTasks })
</script>

<style>
@import '@/styles/teaching-ui.css';
</style>

<style scoped>
/* ========== Task Card Layout ========== */
.task-cards {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 4px 0;
}

.task-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 16px 20px;
}

.task-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.task-card-title {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.course-name {
  font-weight: 600;
  font-size: 15px;
  color: #111827;
}

.course-code {
  font-size: 12px;
  color: #9ca3af;
  font-family: 'JetBrains Mono', monospace;
}

/* Info Row */
.task-card-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
  color: #6b7280;
  margin-bottom: 10px;
}
.task-card-info .sep {
  display: block;
  width: 1px;
  height: 10px;
  background: #d1d5db;
}

/* Config Row */
.task-card-config {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
  padding: 8px 12px;
  background: #f9fafb;
  border-radius: 6px;
}

.config-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
}

.config-item label {
  color: #6b7280;
  white-space: nowrap;
}

.config-select {
  height: 28px;
  font-size: 12px;
  padding: 2px 6px;
  min-width: 100px;
}

/* Teacher Assignment */
.task-card-teachers {
  border-top: 1px solid #f3f4f6;
  padding-top: 10px;
  margin-bottom: 8px;
}

.teacher-label {
  font-size: 12px;
  color: #9ca3af;
  margin-bottom: 6px;
  font-weight: 500;
}

.teacher-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.teacher-select {
  height: 28px;
  font-size: 12px;
  padding: 2px 6px;
  width: 140px;
}

.role-toggle {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  font-size: 11px;
  border-radius: 4px;
  cursor: pointer;
  user-select: none;
  background: #f3f4f6;
  color: #6b7280;
  border: 1px solid #e5e7eb;
  transition: all 0.15s;
}

.role-toggle.is-main {
  background: #dbeafe;
  color: #2563eb;
  border-color: #bfdbfe;
}

.hours-group {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #6b7280;
}

.hours-input {
  width: 52px;
  height: 28px;
  font-size: 12px;
  padding: 2px 6px;
  text-align: center;
}

.no-teacher {
  font-size: 12.5px;
  color: #9ca3af;
  padding: 4px 0;
}

.teacher-footer {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 6px;
}

.hours-summary {
  font-size: 12px;
  font-weight: 500;
}

.hours-ok {
  color: #059669;
}

.hours-warn {
  color: #d97706;
}

.tm-btn-sm {
  padding: 3px 10px;
  font-size: 12px;
  height: 26px;
}

/* Card Actions */
.task-card-actions {
  display: flex;
  justify-content: flex-end;
  padding-top: 8px;
  border-top: 1px solid #f3f4f6;
}

/* ========== View Toggle ========== */
.view-toggle {
  display: inline-flex;
  border: 1px solid #d1d5db;
  border-radius: 5px;
  overflow: hidden;
}
.vt-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 26px;
  background: #fff;
  border: none;
  color: #9ca3af;
  cursor: pointer;
  transition: all 0.12s;
}
.vt-btn:not(:last-child) { border-right: 1px solid #d1d5db; }
.vt-btn:hover { background: #f3f4f6; color: #374151; }
.vt-active { background: #2563eb !important; color: #fff !important; }

/* ========== Compact Table ========== */
.compact-table-wrap { margin-top: 4px; }
.compact-table { font-size: 12px; }
.compact-table th {
  font-size: 11px;
  padding: 6px 4px;
  white-space: nowrap;
  background: #f9fafb;
}
.compact-table td { padding: 5px 4px; }
.ct-row:hover { background: #f8fafc; }
.ct-course { font-weight: 600; color: #111827; font-size: 12.5px; }
.ct-code { font-size: 10px; color: #9ca3af; margin-left: 4px; font-family: 'JetBrains Mono', monospace; }
.ct-select {
  height: 24px;
  font-size: 11px;
  padding: 1px 4px;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  background: #fff;
  color: #374151;
  width: 70px;
}
.ct-teacher {
  font-size: 11.5px;
  color: #2563eb;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: inline-block;
  max-width: 130px;
}
.ct-status {
  display: inline-block;
  font-size: 10.5px;
  padding: 1px 6px;
  border-radius: 3px;
  white-space: nowrap;
}
.ct-s-0 { background: #f3f4f6; color: #6b7280; }
.ct-s-1 { background: #dbeafe; color: #2563eb; }
.ct-s-2 { background: #d1fae5; color: #059669; }
.ct-s-3 { background: #fef3c7; color: #d97706; }
.ct-s-4 { background: #fee2e2; color: #dc2626; }
.ct-s-9 { background: #fee2e2; color: #dc2626; }
.ct-reassign {
  margin-left: 4px;
  font-size: 10px;
  padding: 0 4px;
  height: 18px;
  line-height: 18px;
  border: 1px solid #d1d5db;
  border-radius: 3px;
  background: #fff;
  color: #6b7280;
  cursor: pointer;
}
.ct-reassign:hover { border-color: #2563eb; color: #2563eb; }
.ct-assess {
  font-size: 10.5px;
  padding: 1px 5px;
  border-radius: 3px;
  white-space: nowrap;
}
.ct-a-1 { background: #eff6ff; color: #2563eb; }
.ct-a-2 { background: #f0fdf4; color: #16a34a; }
.ct-a-3 { background: #fef3c7; color: #d97706; }
.ct-a-4 { background: #f3e8ff; color: #7c3aed; }
</style>
