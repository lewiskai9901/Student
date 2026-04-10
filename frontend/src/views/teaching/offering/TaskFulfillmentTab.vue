<template>
  <div>
    <!-- Stats bar + Actions -->
    <div class="tm-filters" style="justify-content: space-between;">
      <div class="tm-stats" style="margin: 0;">
        <span>共 <b>{{ totalCount }}</b> 条</span>
        <span class="sep" />
        <span>已分配教师 <b>{{ assignedCount }}/{{ totalCount }}</b></span>
        <span class="sep" />
        <span>已排课 <b>{{ scheduledCount }}/{{ totalCount }}</b></span>
      </div>
      <div style="display: flex; align-items: center; gap: 8px;">
        <button class="tm-btn tm-btn-workflow" :disabled="!semesterId" @click="handleGenerateTasks">从开课计划生成任务</button>
        <button class="tm-btn tm-btn-secondary" :disabled="!semesterId || filteredTasks.length === 0" @click="handleBatchAssign">批量分配教师</button>
      </div>
    </div>

    <!-- Table -->
    <div class="tm-table-wrap">
      <table class="tm-table">
        <colgroup>
          <col style="width: 20%;" />
          <col style="width: 12%;" />
          <col style="width: 8%;" />
          <col style="width: 20%;" />
          <col style="width: 8%;" />
          <col style="width: 10%;" />
          <col style="width: 8%;" />
          <col style="width: 14%;" />
        </colgroup>
        <thead>
          <tr>
            <th class="text-left">课程名称</th>
            <th>班级</th>
            <th>学生数</th>
            <th>教师</th>
            <th>周课时</th>
            <th>教学周</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="8" class="tm-empty">
              <span class="tm-spin" style="display: inline-block; width: 16px; height: 16px; border: 2px solid #e5e7eb; border-top-color: #2563eb; border-radius: 50%;" />
              加载中...
            </td>
          </tr>
          <tr v-else-if="!semesterId">
            <td colspan="8" class="tm-empty">请先选择学期</td>
          </tr>
          <tr v-else-if="filteredTasks.length === 0">
            <td colspan="8" class="tm-empty">暂无教学任务，点击"从开课计划生成任务"开始</td>
          </tr>
          <tr v-for="row in filteredTasks" :key="row.id">
            <td class="text-left">{{ row.courseName }}</td>
            <td>{{ row.className }}</td>
            <td class="tm-mono">{{ row.studentCount }}</td>
            <td>
              <template v-if="row.teacherName">
                <span class="tm-chip tm-chip-green">{{ row.teacherName }}</span>
              </template>
              <template v-else>
                <select
                  class="tm-input"
                  style="width: 130px; height: 28px; font-size: 12px; padding: 2px 6px;"
                  @change="onTeacherChange(row, ($event.target as HTMLSelectElement).value); ($event.target as HTMLSelectElement).value = ''"
                >
                  <option value="">选择教师</option>
                  <option v-for="t in teachers" :key="t.id" :value="t.id">{{ t.realName || t.username }}</option>
                </select>
              </template>
            </td>
            <td class="tm-mono">{{ row.weeklyHours }}</td>
            <td class="tm-mono">{{ row.startWeek }}-{{ row.endWeek }}周</td>
            <td>
              <span :class="['tm-chip', statusChipClass(row.status)]">
                {{ getStatusName(row.status) }}
              </span>
            </td>
            <td>
              <button v-if="row.teacherName" class="tm-action" @click="handleReassign(row)">换教师</button>
              <button class="tm-action tm-action-danger" @click="handleDelete(row)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
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
                  <select v-model="batchTeacherId" class="tm-input" style="width: 100%;">
                    <option value="">选择教师</option>
                    <option v-for="t in teachers" :key="t.id" :value="t.id">{{ t.realName || t.username }}</option>
                  </select>
                </div>
                <p style="font-size: 12px; color: #6b7280; margin-top: 8px;">
                  将为当前筛选范围内所有未分配教师的任务（共 {{ unassignedCount }} 条）统一分配选中的教师。
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
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { teachingTaskApi, workflowApi } from '@/api/teaching'
import { getSimpleUserList } from '@/api/user'
import type { TeachingTask } from '@/types/teaching'
import type { SimpleUser } from '@/types/user'

const props = defineProps<{
  semesterId: number | string | undefined
  selectedOrg?: { type: string; id: number | string; name: string }
}>()

const router = useRouter()

// State
const tasks = ref<TeachingTask[]>([])
const teachers = ref<SimpleUser[]>([])
const loading = ref(false)
const batchAssignVisible = ref(false)
const batchTeacherId = ref<number | string>('')
const batchAssigning = ref(false)

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
  } catch {
    tasks.value = []
  } finally {
    loading.value = false
  }
}

async function loadTeachers() {
  try {
    const list = await getSimpleUserList()
    teachers.value = list || []
  } catch {
    teachers.value = []
  }
}

// ==================== Filtering ====================

const filteredTasks = computed(() => {
  if (!props.selectedOrg?.id) return tasks.value
  const org = props.selectedOrg
  if (org.type === 'CLASS') {
    return tasks.value.filter(t => String(t.orgUnitId) === String(org.id))
  }
  // For DEPARTMENT/GRADE, filter by className containing org name
  if (org.name) {
    const keyword = org.name.replace(/级$/, '').replace(/系$/, '').replace(/学院$/, '')
    if (keyword) {
      return tasks.value.filter(t => t.className && t.className.includes(keyword))
    }
  }
  return tasks.value
})

// ==================== Stats ====================

const totalCount = computed(() => filteredTasks.value.length)
const assignedCount = computed(() => filteredTasks.value.filter(t => t.teacherName).length)
const scheduledCount = computed(() => filteredTasks.value.filter(t => t.status >= 2).length)
const unassignedCount = computed(() => filteredTasks.value.filter(t => !t.teacherName).length)
const allAssigned = computed(() => totalCount.value > 0 && assignedCount.value === totalCount.value)
const progressPercent = computed(() => totalCount.value > 0 ? Math.round((assignedCount.value / totalCount.value) * 100) : 0)

// ==================== Actions ====================

async function onTeacherChange(task: TeachingTask, teacherId: string) {
  if (!teacherId || !task.id) return
  try {
    await teachingTaskApi.assignTeachers(task.id, [teacherId], teacherId)
    ElMessage.success('已分配教师')
    await loadTasks()
  } catch {
    ElMessage.error('分配失败')
  }
}

async function handleReassign(task: TeachingTask) {
  // Remove current teachers and show select
  try {
    await ElMessageBox.confirm(
      `确定要重新分配「${task.courseName}」的教师吗？将清除当前教师分配。`,
      '重新分配教师',
      { type: 'warning' }
    )
    if (task.teachers) {
      for (const t of task.teachers) {
        await teachingTaskApi.removeTeacher(task.id, t.teacherId)
      }
    }
    ElMessage.success('已清除教师分配，请重新选择')
    await loadTasks()
  } catch {
    // cancelled
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
  batchAssignVisible.value = true
}

async function executeBatchAssign() {
  if (!batchTeacherId.value) return
  const unassigned = filteredTasks.value.filter(t => !t.teachers || t.teachers.length === 0)
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
        await teachingTaskApi.assignTeachers(task.id, [Number(batchTeacherId.value)], Number(batchTeacherId.value))
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
    0: '待分配',
    1: '已分配',
    2: '已排课',
    3: '进行中',
    4: '已结束',
  }
  return names[status] || '未知'
}

function statusChipClass(status: number) {
  const map: Record<number, string> = {
    0: 'tm-chip-gray',
    1: 'tm-chip-amber',
    2: 'tm-chip-blue',
    3: 'tm-chip-green',
    4: 'tm-chip-red',
  }
  return map[status] || 'tm-chip-gray'
}

// ==================== Watchers & Lifecycle ====================

watch(() => props.semesterId, () => {
  loadTasks()
})

onMounted(() => {
  loadTasks()
  loadTeachers()
})

defineExpose({ reload: loadTasks })
</script>

<style>
@import '@/styles/teaching-ui.css';
</style>
