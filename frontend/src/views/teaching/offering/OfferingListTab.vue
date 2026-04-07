<template>
  <!-- Stats bar -->
  <div class="tm-filters">
    <span style="font-size: 12.5px; color: #6b7280;">共 <b>{{ offerings.length }}</b> 门课</span>
    <i class="tm-sep" />
    <span style="font-size: 12.5px; color: #6b7280;">必修 <b>{{ offeringRequiredCount }}</b></span>
    <i class="tm-sep" />
    <span style="font-size: 12.5px; color: #6b7280;">选修 <b>{{ offeringElectiveCount }}</b></span>
    <i class="tm-sep" />
    <span style="font-size: 12.5px; color: #6b7280;">总周课时 <b>{{ offeringTotalWeeklyHours }}</b></span>
  </div>

  <div class="tm-table-wrap">
    <!-- Action bar -->
    <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 12px;">
      <button class="tm-btn tm-btn-secondary" @click="showImportFromPlanDialog">从培养方案导入</button>
      <button class="tm-btn tm-btn-primary" @click="showOfferingDialog()">手动添加</button>
    </div>

    <!-- Table -->
    <table class="tm-table">
      <colgroup>
        <col style="width: 110px" />
        <col />
        <col style="width: 90px" />
        <col style="width: 70px" />
        <col style="width: 90px" />
        <col style="width: 65px" />
        <col style="width: 65px" />
        <col style="width: 80px" />
        <col style="width: 160px" />
      </colgroup>
      <thead>
        <tr>
          <th>课程代码</th>
          <th class="text-left">课程名称</th>
          <th>适用年级</th>
          <th>周课时</th>
          <th>课程类别</th>
          <th>合堂</th>
          <th>走班</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="offeringLoading">
          <td colspan="9" class="tm-empty">
            <span class="tm-spin" style="display:inline-block;width:16px;height:16px;border:2px solid #e5e7eb;border-top-color:#2563eb;border-radius:50%;" /> 加载中...
          </td>
        </tr>
        <tr v-else-if="offerings.length === 0">
          <td colspan="9" class="tm-empty">暂无开课计划，请选择学期后添加</td>
        </tr>
        <tr v-for="row in offerings" :key="row.id">
          <td><span class="tm-code">{{ row.courseCode }}</span></td>
          <td class="text-left">{{ row.courseName }}</td>
          <td>{{ row.applicableGrade }}</td>
          <td class="tm-mono">{{ row.weeklyHours }}</td>
          <td>
            <span :class="['tm-chip', getCourseTypeChip(row.courseType)]">
              {{ getCourseTypeName(row.courseType) }}
            </span>
          </td>
          <td :style="{ color: row.allowCombined ? '#2563eb' : '#d1d5db', fontSize: '12px' }">{{ row.allowCombined ? '是' : '否' }}</td>
          <td :style="{ color: row.allowWalking ? '#2563eb' : '#d1d5db', fontSize: '12px' }">{{ row.allowWalking ? '是' : '否' }}</td>
          <td>
            <span :class="['tm-chip', row.status === 1 ? 'tm-chip-green' : 'tm-chip-amber']">
              {{ getOfferingStatusName(row.status) }}
            </span>
          </td>
          <td>
            <button class="tm-action" @click="showOfferingDialog(row)">编辑</button>
            <button v-if="row.status === 0" class="tm-action" @click="confirmOffering(row)">确认</button>
            <button class="tm-action tm-action-danger" @click="deleteOffering(row)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>

  <!-- Offering Add/Edit Drawer -->
  <Transition name="tm-drawer">
    <div v-if="offeringDialogVisible" class="tm-drawer-overlay" @click.self="offeringDialogVisible = false">
      <div class="tm-drawer">
        <div class="tm-drawer-header">
          <h3 class="tm-drawer-title">{{ editingOffering ? '编辑开课' : '添加开课' }}</h3>
          <button class="tm-drawer-close" @click="offeringDialogVisible = false">&times;</button>
        </div>
        <div class="tm-drawer-body">
          <div class="tm-section">
            <h4 class="tm-section-title">基本信息</h4>
            <div class="tm-field" :class="{ 'tm-error': errors.courseId }">
              <label class="tm-label">课程 <span class="req">*</span></label>
              <select v-model="offeringForm.courseId" class="tm-field-select" @change="onOfferingCourseChange($event)">
                <option :value="undefined" disabled>选择课程</option>
                <option v-for="c in allCourses" :key="c.id" :value="c.id">{{ c.courseCode }} - {{ c.courseName }}</option>
              </select>
            </div>
            <div class="tm-field" :class="{ 'tm-error': errors.applicableGrade }">
              <label class="tm-label">适用年级 <span class="req">*</span></label>
              <input v-model="offeringForm.applicableGrade" class="tm-input" placeholder="如: 2024级、全年级" />
            </div>
            <div class="tm-fields tm-cols-2">
              <div class="tm-field" :class="{ 'tm-error': errors.weeklyHours }">
                <label class="tm-label">周课时 <span class="req">*</span></label>
                <input v-model.number="offeringForm.weeklyHours" type="number" min="1" max="20" class="tm-input" />
              </div>
              <div class="tm-field">
                <label class="tm-label">课程类型</label>
                <select v-model="offeringForm.courseType" class="tm-field-select">
                  <option :value="1">必修</option>
                  <option :value="2">选修</option>
                  <option :value="3">通识</option>
                </select>
              </div>
            </div>
            <div class="tm-fields tm-cols-2">
              <div class="tm-field" :class="{ 'tm-error': errors.startWeek }">
                <label class="tm-label">起始周 <span class="req">*</span></label>
                <input v-model.number="offeringForm.startWeek" type="number" min="1" max="30" class="tm-input" />
              </div>
              <div class="tm-field">
                <label class="tm-label">结束周</label>
                <input v-model.number="offeringForm.endWeek" type="number" :min="offeringForm.startWeek" max="30" class="tm-input" />
              </div>
            </div>
          </div>
          <div class="tm-section">
            <h4 class="tm-section-title">排课选项</h4>
            <div class="tm-fields tm-cols-2">
              <div class="tm-field">
                <label class="tm-label">允许合堂</label>
                <label style="display:flex;align-items:center;gap:8px;font-size:13px;cursor:pointer;">
                  <input type="checkbox" v-model="offeringForm.allowCombined" /> {{ offeringForm.allowCombined ? '是' : '否' }}
                  <input v-if="offeringForm.allowCombined" v-model.number="offeringForm.maxCombinedClasses" type="number" min="2" max="10" class="tm-input" style="width:80px;" placeholder="最大数" />
                </label>
              </div>
              <div class="tm-field">
                <label class="tm-label">允许走班</label>
                <label style="display:flex;align-items:center;gap:8px;font-size:13px;cursor:pointer;">
                  <input type="checkbox" v-model="offeringForm.allowWalking" /> {{ offeringForm.allowWalking ? '是' : '否' }}
                </label>
              </div>
            </div>
            <div class="tm-field">
              <label class="tm-label">备注</label>
              <textarea v-model="offeringForm.remark" class="tm-textarea" rows="2"></textarea>
            </div>
          </div>
        </div>
        <div class="tm-drawer-footer">
          <button class="tm-btn tm-btn-secondary" @click="offeringDialogVisible = false">取消</button>
          <button class="tm-btn tm-btn-primary" :disabled="offeringSaving" @click="saveOffering">
            {{ offeringSaving ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </Transition>

  <!-- Import from Curriculum Plan Drawer -->
  <Transition name="tm-drawer">
    <div v-if="importDialogVisible" class="tm-drawer-overlay" @click.self="importDialogVisible = false">
      <div class="tm-drawer" style="width: 440px;">
        <div class="tm-drawer-header">
          <h3 class="tm-drawer-title">从培养方案导入</h3>
          <button class="tm-drawer-close" @click="importDialogVisible = false">&times;</button>
        </div>
        <div class="tm-drawer-body">
          <div class="tm-section">
            <div class="tm-field">
              <label class="tm-label">培养方案</label>
              <select v-model="importForm.planId" class="tm-field-select">
                <option :value="undefined" disabled>选择培养方案</option>
                <option v-for="p in curriculumPlans" :key="p.id" :value="p.id">{{ p.planName }} (v{{ p.version }})</option>
              </select>
            </div>
            <div class="tm-field">
              <label class="tm-label">限定班级（可选）</label>
              <select v-model="importForm.orgUnitIds" class="tm-field-select" multiple style="min-height: 100px;">
                <option v-for="c in classes" :key="c.id" :value="c.id">{{ c.className }}</option>
              </select>
            </div>
          </div>
        </div>
        <div class="tm-drawer-footer">
          <button class="tm-btn tm-btn-secondary" @click="importDialogVisible = false">取消</button>
          <button class="tm-btn tm-btn-primary" :disabled="importLoading" @click="doImportFromPlan">
            {{ importLoading ? '导入中...' : '导入' }}
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { ref, computed, watch, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
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

const errors = reactive({ courseId: false, applicableGrade: false, weeklyHours: false, startWeek: false })

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

// Import dialog
const importDialogVisible = ref(false)
const importLoading = ref(false)
const importForm = ref({
  planId: undefined as number | undefined,
  orgUnitIds: [] as number[],
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

function getCourseTypeChip(type: number) {
  const map: Record<number, string> = { 1: 'tm-chip-red', 2: 'tm-chip-blue', 3: 'tm-chip-gray' }
  return map[type] || 'tm-chip-gray'
}

function getOfferingStatusName(status: number) {
  return status === 1 ? '已确认' : '待确认'
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

function onOfferingCourseChange(event: Event) {
  const courseId = Number((event.target as HTMLSelectElement).value)
  const course = allCourses.value.find(c => Number(c.id) === courseId)
  if (course) {
    offeringForm.value.courseType = course.courseType
  }
}

function showOfferingDialog(row?: SemesterOffering) {
  Object.keys(errors).forEach(k => (errors as any)[k] = false)
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

function validate() {
  const f = offeringForm.value
  errors.courseId = !f.courseId
  errors.applicableGrade = !f.applicableGrade?.trim()
  errors.weeklyHours = !f.weeklyHours
  errors.startWeek = !f.startWeek
  return !Object.values(errors).some(Boolean)
}

async function saveOffering() {
  if (!validate()) return

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
  importForm.value = { planId: undefined, orgUnitIds: [] }
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
      orgUnitIds: importForm.value.orgUnitIds.length > 0 ? importForm.value.orgUnitIds : undefined,
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

<style>
@import '@/styles/teaching-ui.css';
.tm-sep { display: inline-block; width: 1px; height: 10px; background: #d1d5db; vertical-align: middle; margin: 0 4px; }
</style>
