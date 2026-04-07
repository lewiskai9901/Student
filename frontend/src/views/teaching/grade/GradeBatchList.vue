<template>
  <div>
    <!-- Header row -->
    <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px;">
      <div class="tm-stats" style="margin-top: 0;">
        <span>批次总数 <b>{{ total }}</b></span>
        <span class="sep" />
        <span>草稿 <b>{{ statusCounts.draft }}</b></span>
        <span class="sep" />
        <span>已提交 <b>{{ statusCounts.submitted }}</b></span>
        <span class="sep" />
        <span>已审核 <b>{{ statusCounts.approved }}</b></span>
        <span class="sep" />
        <span><span class="dot dot-green" />已发布 <b>{{ statusCounts.published }}</b></span>
      </div>
      <div style="display: flex; align-items: center; gap: 8px;">
        <button
          class="tm-btn tm-btn-secondary"
          :disabled="!queryParams.semesterId || exporting"
          @click="doExportGrades"
        >{{ exporting ? '导出中...' : '导出Excel' }}</button>
        <button class="tm-btn tm-btn-primary" @click="showBatchDialog()">新建批次</button>
      </div>
    </div>

    <!-- Table -->
    <table class="tm-table">
      <colgroup>
        <col />
        <col style="width: 130px" />
        <col style="width: 110px" />
        <col style="width: 90px" />
        <col style="width: 80px" />
        <col style="width: 110px" />
        <col style="width: 85px" />
        <col style="width: 220px" />
      </colgroup>
      <thead>
        <tr>
          <th class="text-left">批次名称</th>
          <th>课程</th>
          <th>班级</th>
          <th>成绩类型</th>
          <th>状态</th>
          <th>录入截止</th>
          <th>创建人</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="loading">
          <td colspan="8" class="tm-empty">
            <span class="tm-spin" style="display:inline-block;width:16px;height:16px;border:2px solid #e5e7eb;border-top-color:#2563eb;border-radius:50%;" /> 加载中...
          </td>
        </tr>
        <tr v-else-if="batches.length === 0">
          <td colspan="8" class="tm-empty">暂无成绩批次数据</td>
        </tr>
        <tr v-for="row in batches" :key="row.id">
          <td class="text-left" style="font-weight: 500;">{{ row.batchName }}</td>
          <td>{{ row.courseName }}</td>
          <td>{{ row.className }}</td>
          <td><span :class="['tm-chip', gradeTypeChipClass(row.gradeType)]">{{ getGradeTypeName(row.gradeType) }}</span></td>
          <td><span :class="['tm-chip', statusChipClass(row.status)]">{{ getStatusName(row.status) }}</span></td>
          <td style="font-size: 12px; color: #6b7280;">{{ row.inputDeadline }}</td>
          <td>{{ row.createdByName }}</td>
          <td>
            <button class="tm-action" @click="emit('enterGrades', row)">录入</button>
            <button class="tm-action" @click="emit('viewStatistics', row)">统计</button>
            <button v-if="row.status === 0" class="tm-action" style="color: #2563eb;" @click="submitBatch(row)">提交</button>
            <button v-if="row.status === 1" class="tm-action" style="color: #16a34a;" @click="approveBatch(row)">审核</button>
            <button v-if="row.status === 2" class="tm-action" style="color: #d97706;" @click="publishBatch(row)">发布</button>
            <button class="tm-action" @click="exportBatchGrades(row)">导出</button>
          </td>
        </tr>
      </tbody>
    </table>

    <!-- Pagination -->
    <div v-if="total > (queryParams.size || 10)" class="tm-pagination">
      <span class="tm-page-info">共 {{ total }} 条</span>
      <div class="tm-page-controls">
        <select v-model="queryParams.size" class="tm-page-select" @change="queryParams.page = 1; loadBatches()">
          <option :value="10">10条/页</option>
          <option :value="20">20条/页</option>
          <option :value="50">50条/页</option>
        </select>
        <button class="tm-page-btn" :disabled="(queryParams.page || 1) <= 1" @click="queryParams.page!--; loadBatches()">‹</button>
        <span class="tm-page-current">{{ queryParams.page }}</span>
        <button class="tm-page-btn" :disabled="(queryParams.page || 1) * (queryParams.size || 10) >= total" @click="queryParams.page!++; loadBatches()">›</button>
      </div>
    </div>
  </div>

  <!-- Batch Drawer -->
  <Transition name="tm-drawer">
    <div v-if="batchDialogVisible" class="tm-drawer-overlay" @click.self="batchDialogVisible = false">
      <div class="tm-drawer">
        <div class="tm-drawer-header">
          <h3 class="tm-drawer-title">{{ batchForm.id ? '编辑批次' : '新建成绩批次' }}</h3>
          <button class="tm-drawer-close" @click="batchDialogVisible = false">&times;</button>
        </div>
        <div class="tm-drawer-body">
          <div class="tm-section">
            <h4 class="tm-section-title">基本信息</h4>
            <div class="tm-field" :class="{ 'tm-error': formErrors.batchName }">
              <label class="tm-label">批次名称 <span class="req">*</span></label>
              <input v-model="batchForm.batchName" class="tm-input" placeholder="如：高等数学-期末成绩" />
            </div>
            <div class="tm-fields tm-cols-2">
              <div class="tm-field" :class="{ 'tm-error': formErrors.semesterId }">
                <label class="tm-label">学期 <span class="req">*</span></label>
                <select v-model="batchForm.semesterId" class="tm-field-select">
                  <option :value="undefined" disabled>选择学期</option>
                  <option v-for="sem in semesters" :key="sem.id" :value="sem.id">{{ sem.semesterName }}</option>
                </select>
              </div>
              <div class="tm-field" :class="{ 'tm-error': formErrors.gradeType }">
                <label class="tm-label">成绩类型 <span class="req">*</span></label>
                <select v-model="batchForm.gradeType" class="tm-field-select">
                  <option :value="undefined" disabled>选择类型</option>
                  <option :value="1">平时成绩</option>
                  <option :value="2">期中成绩</option>
                  <option :value="3">期末成绩</option>
                  <option :value="4">总评成绩</option>
                </select>
              </div>
            </div>
            <div class="tm-fields tm-cols-2">
              <div class="tm-field" :class="{ 'tm-error': formErrors.courseId }">
                <label class="tm-label">课程 <span class="req">*</span></label>
                <select v-model="batchForm.courseId" class="tm-field-select">
                  <option :value="undefined" disabled>选择课程</option>
                  <option v-for="c in courseOptions" :key="c.id" :value="c.id">{{ c.courseCode }} - {{ c.courseName }}</option>
                </select>
              </div>
              <div class="tm-field" :class="{ 'tm-error': formErrors.classId }">
                <label class="tm-label">班级 <span class="req">*</span></label>
                <select v-model="batchForm.classId" class="tm-field-select">
                  <option :value="undefined" disabled>选择班级</option>
                  <option v-for="cls in classOptions" :key="cls.id" :value="cls.id">{{ cls.name }}</option>
                </select>
              </div>
            </div>
            <div class="tm-field">
              <label class="tm-label">录入截止</label>
              <input v-model="batchForm.inputDeadline" type="datetime-local" class="tm-input" />
            </div>
          </div>
        </div>
        <div class="tm-drawer-footer">
          <button class="tm-btn tm-btn-secondary" @click="batchDialogVisible = false">取消</button>
          <button class="tm-btn tm-btn-primary" :disabled="saving" @click="saveBatch">{{ saving ? '保存中...' : '保存' }}</button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { gradeApi } from '@/api/teaching'
import { semesterApi } from '@/api/calendar'
import { courseApi } from '@/api/academic'
import { schoolClassApi } from '@/api/organization'
import type { GradeBatch, Semester, GradeQueryParams } from '@/types/teaching'
import type { Course } from '@/types/academic'
import { getGradeTypeName, getStatusName } from './gradeHelpers'

const emit = defineEmits<{
  enterGrades: [batch: GradeBatch]
  viewStatistics: [batch: GradeBatch]
}>()

// Chip class helpers
function gradeTypeChipClass(type: number) {
  const map: Record<number, string> = { 1: 'tm-chip-gray', 2: 'tm-chip-amber', 3: 'tm-chip-blue', 4: 'tm-chip-green' }
  return map[type] || 'tm-chip-gray'
}

function statusChipClass(status: number) {
  const map: Record<number, string> = { 0: 'tm-chip-gray', 1: 'tm-chip-amber', 2: 'tm-chip-blue', 3: 'tm-chip-green' }
  return map[status] || 'tm-chip-gray'
}

// State
const loading = ref(false)
const saving = ref(false)
const exporting = ref(false)
const batches = ref<GradeBatch[]>([])
const semesters = ref<Semester[]>([])
const courseOptions = ref<Course[]>([])
const classOptions = ref<{ id: number | string; name: string }[]>([])
const total = ref(0)

const statusCounts = computed(() => {
  const counts = { draft: 0, submitted: 0, approved: 0, published: 0 }
  batches.value.forEach(b => {
    if (b.status === 0) counts.draft++
    else if (b.status === 1) counts.submitted++
    else if (b.status === 2) counts.approved++
    else if (b.status === 3) counts.published++
  })
  return counts
})

const batchDialogVisible = ref(false)
const batchForm = ref<Partial<GradeBatch>>({})
const formErrors = reactive({ batchName: false, semesterId: false, gradeType: false, courseId: false, classId: false })

const queryParams = reactive<GradeQueryParams>({
  semesterId: undefined,
  gradeType: undefined,
  status: undefined,
  page: 1,
  size: 10,
})

// Data Loading
const loadSemesters = async () => {
  try {
    const res: any = await semesterApi.list()
    semesters.value = res.data || res
    if (semesters.value.length > 0) {
      const current = semesters.value.find(s => s.isCurrent)
      if (current) queryParams.semesterId = current.id
    }
  } catch { /* */ }
}

const loadClassOptions = async () => {
  try {
    const res = await schoolClassApi.getAll()
    const data: any = res
    classOptions.value = (Array.isArray(data) ? data : data.records || []).map((c: any) => ({ id: c.id, name: c.name || c.className }))
  } catch { classOptions.value = [] }
}

const loadCourseOptions = async () => {
  try { courseOptions.value = await courseApi.listAll() } catch { courseOptions.value = [] }
}

const loadBatches = async () => {
  loading.value = true
  try {
    const res: any = await gradeApi.listBatches(queryParams)
    const data = res.data || res
    batches.value = data.records || []
    total.value = data.total || 0
  } catch { batches.value = []; total.value = 0 } finally { loading.value = false }
}

// Validation
function validate() {
  const f = batchForm.value
  formErrors.batchName = !f.batchName?.trim()
  formErrors.semesterId = !f.semesterId
  formErrors.gradeType = !f.gradeType
  formErrors.courseId = !f.courseId
  formErrors.classId = !f.classId
  return !Object.values(formErrors).some(Boolean)
}

// Batch CRUD
const showBatchDialog = (batch?: GradeBatch) => {
  Object.keys(formErrors).forEach(k => (formErrors as any)[k] = false)
  batchForm.value = batch ? { ...batch } : { semesterId: queryParams.semesterId, gradeType: 3 }
  batchDialogVisible.value = true
}

const saveBatch = async () => {
  if (!validate()) return
  saving.value = true
  try {
    if (batchForm.value.id) { await gradeApi.updateBatch(batchForm.value.id, batchForm.value) }
    else { await gradeApi.createBatch(batchForm.value) }
    ElMessage.success('保存成功'); batchDialogVisible.value = false; loadBatches()
  } catch { ElMessage.error('保存失败') } finally { saving.value = false }
}

// Status actions
const submitBatch = async (batch: GradeBatch) => {
  await ElMessageBox.confirm('提交后成绩将进入审核流程，确定提交吗？', '提示', { type: 'info' })
  try { await gradeApi.submitBatch(batch.id); ElMessage.success('提交成功'); loadBatches() } catch { ElMessage.error('提交失败') }
}

const approveBatch = async (batch: GradeBatch) => {
  await ElMessageBox.confirm('确定审核通过该批次成绩吗？', '提示', { type: 'info' })
  try { await gradeApi.approveBatch(batch.id); ElMessage.success('审核通过'); loadBatches() } catch { ElMessage.error('审核失败') }
}

const publishBatch = async (batch: GradeBatch) => {
  await ElMessageBox.confirm('发布后学生将可查看成绩，确定发布吗？', '提示', { type: 'warning' })
  try { await gradeApi.publishBatch(batch.id); ElMessage.success('发布成功'); loadBatches() } catch { ElMessage.error('发布失败') }
}

// Export
const doExportGrades = async () => {
  if (!queryParams.semesterId) { ElMessage.warning('请先选择学期'); return }
  exporting.value = true
  try {
    const res = await gradeApi.exportGradesByFilter({ semesterId: queryParams.semesterId } as any)
    const blob = new Blob([res as any], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a'); a.href = url; a.download = '成绩导出.xlsx'; document.body.appendChild(a); a.click(); document.body.removeChild(a); window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch { ElMessage.error('导出失败') } finally { exporting.value = false }
}

const exportBatchGrades = async (batch: GradeBatch) => {
  try {
    const res = await gradeApi.exportGrades(batch.id)
    const blob = new Blob([res as any], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a'); link.href = url; link.download = `${batch.batchName}_成绩.xlsx`; link.click(); window.URL.revokeObjectURL(url)
  } catch { ElMessage.error('导出失败') }
}

onMounted(async () => {
  await loadSemesters()
  loadBatches()
  loadClassOptions()
  loadCourseOptions()
})
</script>

<style>
@import '@/styles/teaching-ui.css';
</style>
