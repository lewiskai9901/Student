<template>
  <!-- Filters & Stats -->
  <div class="tm-filters">
    <select v-model="assignClassId" class="tm-select" @change="loadAssignments">
      <option :value="''" disabled>选择班级</option>
      <option v-for="c in classes" :key="c.id" :value="c.id">{{ c.className }}</option>
    </select>
    <i class="tm-sep" />
    <span style="font-size: 12.5px; color: #6b7280;">该班本学期: <b>{{ assignments.length }}</b> 门课</span>
    <i class="tm-sep" />
    <span :style="{ fontSize: '12.5px', color: assignmentTotalHours > 30 ? '#dc2626' : '#6b7280', fontWeight: assignmentTotalHours > 30 ? '600' : '400' }">
      总周课时: <b>{{ assignmentTotalHours }}</b>
      <span v-if="assignmentTotalHours > 30" style="font-size: 11px; margin-left: 4px;">(建议不超过30)</span>
    </span>
  </div>

  <div class="tm-table-wrap">
    <!-- Action bar -->
    <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 12px;">
      <button class="tm-btn tm-btn-secondary" :disabled="!assignClassId" @click="importAssignmentsFromPlan">一键导入开课计划</button>
      <button class="tm-btn tm-btn-primary" :disabled="!assignClassId || assignments.length === 0" @click="batchConfirmAssignments">确认全部</button>
      <span style="flex: 1;" />
      <button class="tm-btn" style="background: #059669; color: #fff; border-color: #059669;" @click="generateTasks">生成教学任务</button>
    </div>

    <!-- Table -->
    <table class="tm-table">
      <colgroup>
        <col />
        <col style="width: 100px" />
        <col style="width: 100px" />
        <col style="width: 90px" />
      </colgroup>
      <thead>
        <tr>
          <th class="text-left">课程名称</th>
          <th>周课时</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="assignmentLoading">
          <td colspan="4" class="tm-empty">
            <span class="tm-spin" style="display:inline-block;width:16px;height:16px;border:2px solid #e5e7eb;border-top-color:#2563eb;border-radius:50%;" /> 加载中...
          </td>
        </tr>
        <tr v-else-if="!assignClassId">
          <td colspan="4" class="tm-empty">请先选择班级查看课程分配</td>
        </tr>
        <tr v-else-if="assignments.length === 0">
          <td colspan="4" class="tm-empty">该班暂无课程分配，点击"一键导入开课计划"开始</td>
        </tr>
        <tr v-for="row in assignments" :key="row.id">
          <td class="text-left">{{ row.courseName }}</td>
          <td class="tm-mono">{{ row.weeklyHours }}</td>
          <td>
            <span :class="['tm-chip', row.status === 1 ? 'tm-chip-green' : 'tm-chip-amber']">
              {{ row.status === 1 ? '已确认' : '待确认' }}
            </span>
          </td>
          <td>
            <button class="tm-action tm-action-danger" @click="deleteAssignment(row)">移除</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { offeringApi, classAssignmentApi } from '@/api/teaching'
import { schoolClassApi } from '@/api/organization'
import type { SemesterOffering, ClassCourseAssignment } from '@/types/teaching'
import type { SchoolClass } from '@/types/organization'

const props = defineProps<{
  semesterId: number | string | null
}>()

// ==================== State ====================

const classes = ref<SchoolClass[]>([])
const assignClassId = ref<number | string>('')
const assignments = ref<ClassCourseAssignment[]>([])
const assignmentLoading = ref(false)

// ==================== Computed ====================

const assignmentTotalHours = computed(() => assignments.value.reduce((sum, a) => sum + a.weeklyHours, 0))

// ==================== Data Loading ====================

async function loadClasses() {
  try {
    classes.value = await schoolClassApi.getAll()
  } catch {
    // non-critical
  }
}

async function loadAssignments() {
  if (!props.semesterId || !assignClassId.value) {
    assignments.value = []
    return
  }
  assignmentLoading.value = true
  try {
    assignments.value = await classAssignmentApi.list(props.semesterId, assignClassId.value)
  } catch {
    ElMessage.error('加载班级课程分配失败')
    assignments.value = []
  } finally {
    assignmentLoading.value = false
  }
}

// ==================== Assignment Operations ====================

async function importAssignmentsFromPlan() {
  if (!props.semesterId || !assignClassId.value) return
  try {
    await ElMessageBox.confirm('将从开课计划导入该班级的所有课程，是否继续?', '导入确认', { type: 'info' })
    assignmentLoading.value = true
    const offerings: SemesterOffering[] = await offeringApi.list(props.semesterId)
    const existingCourseIds = new Set(assignments.value.map(a => a.courseId))
    const toImport = offerings.filter(o => !existingCourseIds.has(o.courseId))
    if (toImport.length === 0) {
      ElMessage.info('所有课程已分配，无需导入')
      assignmentLoading.value = false
      return
    }
    for (const o of toImport) {
      await classAssignmentApi.create({
        semesterId: props.semesterId,
        orgUnitId: assignClassId.value,
        offeringId: o.id,
        courseId: o.courseId,
        weeklyHours: o.weeklyHours,
        status: 0,
      })
    }
    ElMessage.success(`已导入 ${toImport.length} 门课程`)
    loadAssignments()
  } catch {
    ElMessage.error('导入失败')
  } finally {
    assignmentLoading.value = false
  }
}

async function batchConfirmAssignments() {
  if (!props.semesterId || !assignClassId.value) return
  try {
    await ElMessageBox.confirm('确认该班级所有课程分配?', '批量确认', { type: 'info' })
    await classAssignmentApi.batchConfirm(props.semesterId, assignClassId.value)
    ElMessage.success('全部确认成功')
    loadAssignments()
  } catch {
    // cancelled or error
  }
}

async function generateTasks() {
  if (!props.semesterId) return
  try {
    await ElMessageBox.confirm('将从已确认的班级分配生成教学任务，已存在的不会重复创建。', '生成教学任务', { type: 'info' })
    const res = await offeringApi.generateTasks(props.semesterId)
    const count = (res as any)?.generated ?? (res as any)?.data?.generated ?? 0
    ElMessage.success(`已生成 ${count} 个教学任务`)
  } catch {
    // cancelled or error
  }
}

async function deleteAssignment(row: ClassCourseAssignment) {
  try {
    await ElMessageBox.confirm('确定移除该课程分配?', '移除确认', { type: 'warning' })
    await classAssignmentApi.delete(row.id)
    ElMessage.success('已移除')
    loadAssignments()
  } catch {
    // cancelled or error
  }
}

// ==================== Expose & Lifecycle ====================

watch(() => props.semesterId, () => {
  loadAssignments()
})

loadClasses()

defineExpose({ reload: loadAssignments })
</script>

<style>
@import '@/styles/teaching-ui.css';
.tm-sep { display: inline-block; width: 1px; height: 10px; background: #d1d5db; vertical-align: middle; margin: 0 4px; }
</style>
