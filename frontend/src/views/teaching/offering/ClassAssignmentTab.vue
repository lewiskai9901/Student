<template>
  <!-- Filters & Stats -->
  <div class="flex items-center gap-4 border-b border-gray-200 bg-white px-6 py-2.5">
    <el-select v-model="assignClassId" placeholder="选择班级" clearable class="w-48" @change="loadAssignments">
      <el-option v-for="c in classes" :key="c.id" :value="c.id" :label="c.className" />
    </el-select>
    <div class="h-3 w-px bg-gray-200" />
    <span class="text-sm text-gray-500">该班本学期: <span class="font-semibold text-gray-900">{{ assignments.length }}</span> 门课</span>
    <div class="h-3 w-px bg-gray-200" />
    <span class="text-sm" :class="assignmentTotalHours > 30 ? 'text-red-600 font-semibold' : 'text-gray-500'">
      总周课时: <span class="font-semibold" :class="assignmentTotalHours > 30 ? 'text-red-600' : 'text-gray-900'">{{ assignmentTotalHours }}</span>
      <span v-if="assignmentTotalHours > 30" class="ml-1 text-xs">(建议不超过30)</span>
    </span>
  </div>

  <div class="flex-1 overflow-y-auto px-6 pt-5 pb-6">
    <!-- Action bar -->
    <div class="mb-4 flex items-center gap-2">
      <button
        class="inline-flex items-center gap-1.5 rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
        :disabled="!assignClassId"
        @click="importAssignmentsFromPlan"
      >
        一键导入开课计划
      </button>
      <button
        class="inline-flex items-center gap-1.5 rounded-lg bg-blue-600 px-3 py-2 text-sm font-medium text-white hover:bg-blue-700"
        :disabled="!assignClassId || assignments.length === 0"
        @click="batchConfirmAssignments"
      >
        确认全部
      </button>
    </div>

    <!-- Table -->
    <div class="overflow-hidden rounded-xl border border-gray-200 bg-white">
      <el-table :data="assignments" v-loading="assignmentLoading" stripe>
        <el-table-column prop="courseName" label="课程名称" min-width="200" />
        <el-table-column label="周课时" width="120" align="center">
          <template #default="{ row }">
            <el-input-number
              v-model="row.weeklyHours"
              :min="1"
              :max="20"
              size="small"
              controls-position="right"
              class="!w-24"
            />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'warning'">
              {{ row.status === 1 ? '已确认' : '待确认' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <button class="text-sm text-red-600 hover:text-red-800" @click="deleteAssignment(row)">移除</button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div v-if="!assignmentLoading && !assignClassId" class="mt-10 text-center text-sm text-gray-400">
      请先选择班级查看课程分配
    </div>
    <div v-else-if="!assignmentLoading && assignClassId && assignments.length === 0" class="mt-10 text-center text-sm text-gray-400">
      该班暂无课程分配，点击"一键导入开课计划"开始
    </div>
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
    // Load offerings for the semester to know what to import
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
        semesterId: Number(props.semesterId),
        classId: Number(assignClassId.value),
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

// Load reference data
loadClasses()

defineExpose({ reload: loadAssignments })
</script>
