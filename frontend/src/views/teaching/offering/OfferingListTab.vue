<template>
  <!-- Stats bar -->
  <div class="flex items-center gap-4 border-b border-gray-200 bg-white px-6 py-2.5">
    <span class="text-sm text-gray-500">共 <span class="font-semibold text-gray-900">{{ offerings.length }}</span> 门课</span>
    <div class="h-3 w-px bg-gray-200" />
    <span class="text-sm text-gray-500">必修 <span class="font-semibold text-gray-900">{{ offeringRequiredCount }}</span></span>
    <div class="h-3 w-px bg-gray-200" />
    <span class="text-sm text-gray-500">选修 <span class="font-semibold text-gray-900">{{ offeringElectiveCount }}</span></span>
    <div class="h-3 w-px bg-gray-200" />
    <span class="text-sm text-gray-500">总周课时 <span class="font-semibold text-gray-900">{{ offeringTotalWeeklyHours }}</span></span>
  </div>

  <div class="flex-1 overflow-y-auto px-6 pt-5 pb-6">
    <!-- Action bar -->
    <div class="mb-4 flex items-center gap-2">
      <button
        class="inline-flex items-center gap-1.5 rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
        @click="showImportFromPlanDialog"
      >
        从培养方案导入
      </button>
      <button
        class="inline-flex items-center gap-1.5 rounded-lg bg-blue-600 px-3 py-2 text-sm font-medium text-white hover:bg-blue-700"
        @click="showOfferingDialog()"
      >
        手动添加
      </button>
    </div>

    <!-- Table -->
    <div class="overflow-hidden rounded-xl border border-gray-200 bg-white">
      <el-table :data="offerings" v-loading="offeringLoading" stripe>
        <el-table-column prop="courseCode" label="课程代码" width="120" />
        <el-table-column prop="courseName" label="课程名称" min-width="150" />
        <el-table-column prop="applicableGrade" label="适用年级" width="100" />
        <el-table-column prop="weeklyHours" label="周课时" width="80" align="center" />
        <el-table-column label="课程类别" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="(row.courseType === 1 ? '' : row.courseType === 2 ? 'success' : 'info') as any">
              {{ getCourseTypeName(row.courseType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="合堂" width="70" align="center">
          <template #default="{ row }">
            <span :class="row.allowCombined ? 'text-blue-600' : 'text-gray-300'" class="text-sm">
              {{ row.allowCombined ? '是' : '否' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="走班" width="70" align="center">
          <template #default="{ row }">
            <span :class="row.allowWalking ? 'text-blue-600' : 'text-gray-300'" class="text-sm">
              {{ row.allowWalking ? '是' : '否' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <span
              class="inline-block rounded px-1.5 py-0.5 text-xs font-medium"
              :class="getOfferingStatusClass(row.status)"
            >
              {{ getOfferingStatusName(row.status) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <button class="text-sm text-blue-600 hover:text-blue-800" @click="showOfferingDialog(row)">编辑</button>
            <button
              v-if="row.status === 0"
              class="ml-3 text-sm text-emerald-600 hover:text-emerald-800"
              @click="confirmOffering(row)"
            >确认</button>
            <button class="ml-3 text-sm text-red-600 hover:text-red-800" @click="deleteOffering(row)">删除</button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Empty state -->
    <div v-if="!offeringLoading && offerings.length === 0" class="mt-10 text-center text-sm text-gray-400">
      暂无开课计划，请选择学期后添加
    </div>
  </div>

  <!-- Offering Add/Edit Dialog -->
  <el-dialog
    v-model="offeringDialogVisible"
    :title="editingOffering ? '编辑开课' : '添加开课'"
    width="560px"
    destroy-on-close
  >
    <el-form ref="offeringFormRef" :model="offeringForm" :rules="offeringRules" label-width="100px" class="pr-4">
      <el-form-item label="课程" prop="courseId">
        <el-select v-model="offeringForm.courseId" placeholder="选择课程" filterable class="w-full" @change="onOfferingCourseChange">
          <el-option v-for="c in allCourses" :key="c.id" :value="c.id" :label="`${c.courseCode} - ${c.courseName}`" />
        </el-select>
      </el-form-item>
      <el-form-item label="适用年级" prop="applicableGrade">
        <el-input v-model="offeringForm.applicableGrade" placeholder="如: 2024级、全年级" />
      </el-form-item>
      <el-form-item label="周课时" prop="weeklyHours">
        <el-input-number v-model="offeringForm.weeklyHours" :min="1" :max="20" />
      </el-form-item>
      <el-form-item label="起始周" prop="startWeek">
        <el-input-number v-model="offeringForm.startWeek" :min="1" :max="30" />
      </el-form-item>
      <el-form-item label="结束周">
        <el-input-number v-model="offeringForm.endWeek" :min="offeringForm.startWeek" :max="30" />
      </el-form-item>
      <el-form-item label="课程类型">
        <el-select v-model="offeringForm.courseType" class="w-full">
          <el-option :value="1" label="必修" />
          <el-option :value="2" label="选修" />
          <el-option :value="3" label="通识" />
        </el-select>
      </el-form-item>
      <el-form-item label="允许合堂">
        <el-switch v-model="offeringForm.allowCombined" />
        <el-input-number
          v-if="offeringForm.allowCombined"
          v-model="offeringForm.maxCombinedClasses"
          :min="2"
          :max="10"
          class="ml-3"
          placeholder="最大合堂数"
        />
      </el-form-item>
      <el-form-item label="允许走班">
        <el-switch v-model="offeringForm.allowWalking" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="offeringForm.remark" type="textarea" :rows="2" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="offeringDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="offeringSaving" @click="saveOffering">保存</el-button>
    </template>
  </el-dialog>

  <!-- Import from Curriculum Plan Dialog -->
  <el-dialog v-model="importDialogVisible" title="从培养方案导入" width="480px" destroy-on-close>
    <el-form label-width="100px" class="pr-4">
      <el-form-item label="培养方案">
        <el-select v-model="importForm.planId" placeholder="选择培养方案" filterable class="w-full">
          <el-option
            v-for="p in curriculumPlans"
            :key="p.id"
            :value="p.id"
            :label="`${p.planName} (v${p.version})`"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="限定班级">
        <el-select v-model="importForm.classIds" placeholder="可选，不选则全部导入" filterable multiple class="w-full">
          <el-option v-for="c in classes" :key="c.id" :value="c.id" :label="c.className" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="importDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="importLoading" @click="doImportFromPlan">导入</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { offeringApi } from '@/api/teaching'
import { courseApi, curriculumPlanApi } from '@/api/academic'
import { schoolClassApi } from '@/api/organization'
import type { SemesterOffering, Course, CurriculumPlan } from '@/types/teaching'
import type { SchoolClass } from '@/types/organization'

const props = defineProps<{
  semesterId: number | string | null
}>()

// ==================== State ====================

const allCourses = ref<Course[]>([])
const classes = ref<SchoolClass[]>([])
const curriculumPlans = ref<CurriculumPlan[]>([])

const offerings = ref<SemesterOffering[]>([])
const offeringLoading = ref(false)
const offeringDialogVisible = ref(false)
const offeringSaving = ref(false)
const editingOffering = ref<SemesterOffering | null>(null)
const offeringFormRef = ref<FormInstance>()

const offeringForm = ref({
  courseId: undefined as number | undefined,
  applicableGrade: '',
  weeklyHours: 2,
  startWeek: 1,
  endWeek: undefined as number | undefined,
  courseType: 1,
  allowCombined: false,
  maxCombinedClasses: 2,
  allowWalking: false,
  remark: '',
})

const offeringRules: FormRules = {
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  applicableGrade: [{ required: true, message: '请填写适用年级', trigger: 'blur' }],
  weeklyHours: [{ required: true, message: '请填写周课时', trigger: 'blur' }],
  startWeek: [{ required: true, message: '请填写起始周', trigger: 'blur' }],
}

// Import dialog
const importDialogVisible = ref(false)
const importLoading = ref(false)
const importForm = ref({
  planId: undefined as number | undefined,
  classIds: [] as number[],
})

// ==================== Computed ====================

const offeringRequiredCount = computed(() => offerings.value.filter(o => o.courseType === 1).length)
const offeringElectiveCount = computed(() => offerings.value.filter(o => o.courseType === 2).length)
const offeringTotalWeeklyHours = computed(() => offerings.value.reduce((sum, o) => sum + o.weeklyHours, 0))

// ==================== Helpers ====================

function getCourseTypeName(type: number) {
  const map: Record<number, string> = { 1: '必修', 2: '选修', 3: '通识' }
  return map[type] || '其他'
}

function getOfferingStatusName(status: number) {
  return status === 1 ? '已确认' : '待确认'
}

function getOfferingStatusClass(status: number) {
  return status === 1 ? 'bg-emerald-50 text-emerald-700' : 'bg-amber-50 text-amber-700'
}

// ==================== Data Loading ====================

async function loadCourses() {
  try {
    allCourses.value = await courseApi.listAll()
  } catch {
    // non-critical
  }
}

async function loadClasses() {
  try {
    classes.value = await schoolClassApi.getAll()
  } catch {
    // non-critical
  }
}

async function loadCurriculumPlans() {
  try {
    const res = await curriculumPlanApi.list({ status: 1, pageNum: 1, pageSize: 200 })
    curriculumPlans.value = res.records || []
  } catch {
    // non-critical
  }
}

async function loadOfferings() {
  if (!props.semesterId) {
    offerings.value = []
    return
  }
  offeringLoading.value = true
  try {
    offerings.value = await offeringApi.list(props.semesterId)
  } catch {
    ElMessage.error('加载开课计划失败')
    offerings.value = []
  } finally {
    offeringLoading.value = false
  }
}

// ==================== Offering CRUD ====================

function onOfferingCourseChange(courseId: number) {
  const course = allCourses.value.find(c => Number(c.id) === courseId)
  if (course) {
    offeringForm.value.courseType = course.courseType
  }
}

function showOfferingDialog(row?: SemesterOffering) {
  editingOffering.value = row || null
  if (row) {
    offeringForm.value = {
      courseId: row.courseId,
      applicableGrade: row.applicableGrade || '',
      weeklyHours: row.weeklyHours,
      startWeek: row.startWeek,
      endWeek: row.endWeek,
      courseType: row.courseType || 1,
      allowCombined: row.allowCombined,
      maxCombinedClasses: row.maxCombinedClasses || 2,
      allowWalking: row.allowWalking,
      remark: row.remark || '',
    }
  } else {
    offeringForm.value = {
      courseId: undefined,
      applicableGrade: '',
      weeklyHours: 2,
      startWeek: 1,
      endWeek: undefined,
      courseType: 1,
      allowCombined: false,
      maxCombinedClasses: 2,
      allowWalking: false,
      remark: '',
    }
  }
  offeringDialogVisible.value = true
}

async function saveOffering() {
  if (!offeringFormRef.value) return
  const valid = await offeringFormRef.value.validate().catch(() => false)
  if (!valid) return

  offeringSaving.value = true
  try {
    const payload: Partial<SemesterOffering> = {
      semesterId: Number(props.semesterId),
      courseId: offeringForm.value.courseId,
      applicableGrade: offeringForm.value.applicableGrade,
      weeklyHours: offeringForm.value.weeklyHours,
      startWeek: offeringForm.value.startWeek,
      endWeek: offeringForm.value.endWeek,
      courseType: offeringForm.value.courseType,
      allowCombined: offeringForm.value.allowCombined,
      maxCombinedClasses: offeringForm.value.maxCombinedClasses,
      allowWalking: offeringForm.value.allowWalking,
      remark: offeringForm.value.remark,
    }

    if (editingOffering.value) {
      await offeringApi.update(editingOffering.value.id, payload)
      ElMessage.success('更新成功')
    } else {
      await offeringApi.create(payload)
      ElMessage.success('添加成功')
    }
    offeringDialogVisible.value = false
    loadOfferings()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    offeringSaving.value = false
  }
}

async function confirmOffering(row: SemesterOffering) {
  try {
    await ElMessageBox.confirm('确认该开课记录?', '确认', { type: 'info' })
    await offeringApi.confirm(row.id)
    ElMessage.success('已确认')
    loadOfferings()
  } catch {
    // cancelled or error
  }
}

async function deleteOffering(row: SemesterOffering) {
  try {
    await ElMessageBox.confirm('确定删除该开课记录?', '删除确认', { type: 'warning' })
    await offeringApi.delete(row.id)
    ElMessage.success('已删除')
    loadOfferings()
  } catch {
    // cancelled or error
  }
}

// ==================== Import from Plan ====================

function showImportFromPlanDialog() {
  if (!props.semesterId) {
    ElMessage.warning('请先选择学期')
    return
  }
  importForm.value = { planId: undefined, classIds: [] }
  importDialogVisible.value = true
  loadCurriculumPlans()
}

async function doImportFromPlan() {
  if (!importForm.value.planId) {
    ElMessage.warning('请选择培养方案')
    return
  }
  importLoading.value = true
  try {
    await offeringApi.importFromPlan({
      semesterId: Number(props.semesterId),
      planId: importForm.value.planId,
      classIds: importForm.value.classIds.length > 0 ? importForm.value.classIds : undefined,
    })
    ElMessage.success('导入成功')
    importDialogVisible.value = false
    loadOfferings()
  } catch {
    ElMessage.error('导入失败')
  } finally {
    importLoading.value = false
  }
}

// ==================== Expose & Lifecycle ====================

watch(() => props.semesterId, () => {
  loadOfferings()
}, { immediate: true })

// Load reference data
loadCourses()
loadClasses()

defineExpose({ reload: loadOfferings })
</script>
