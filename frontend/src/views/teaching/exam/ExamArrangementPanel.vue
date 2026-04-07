<template>
  <!-- Panel (inline, not drawer) -->
  <div v-if="batch" style="border: 1px solid #e5e7eb; border-radius: 10px; background: #fff; overflow: hidden;">
    <!-- Panel Header -->
    <div style="display: flex; align-items: center; justify-content: space-between; padding: 14px 20px; border-bottom: 1px solid #e8eaed;">
      <div>
        <h3 style="font-size: 15px; font-weight: 700; color: #111827; margin: 0;">{{ batch.name }}</h3>
        <div class="tm-stats" style="margin-top: 4px;">
          <span>{{ getExamTypeName(batch.examType || 0) }}</span>
          <span class="sep" />
          <span>{{ batch.startDate }} ~ {{ batch.endDate }}</span>
          <span class="sep" />
          <span :class="['tm-chip', statusChipClass(batch.status || 0)]">{{ getStatusName(batch.status || 0) }}</span>
        </div>
      </div>
      <div style="display: flex; align-items: center; gap: 8px;">
        <button v-if="batch.status === 0" class="tm-btn tm-btn-primary" @click="showArrangementDialog()">添加考试安排</button>
        <button class="tm-drawer-close" @click="emit('close')">&times;</button>
      </div>
    </div>

    <!-- Stats -->
    <div class="tm-filters" style="border-top: none;">
      <span style="font-size: 12.5px; color: #6b7280;">安排数 <b>{{ arrangements.length }}</b></span>
      <i class="tm-sep" />
      <span style="font-size: 12.5px; color: #6b7280;">已分配考场 <b>{{ arrangementsWithRooms }}</b></span>
    </div>

    <!-- Table -->
    <div style="padding: 12px 20px 16px;">
      <table class="tm-table">
        <colgroup>
          <col />
          <col style="width: 140px" />
          <col style="width: 100px" />
          <col style="width: 120px" />
          <col style="width: 70px" />
          <col style="width: 140px" />
          <col style="width: 160px" />
        </colgroup>
        <thead>
          <tr>
            <th class="text-left">课程</th>
            <th class="text-left">班级</th>
            <th>考试日期</th>
            <th>考试时间</th>
            <th>时长</th>
            <th class="text-left">考场</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="arrangements.length === 0">
            <td colspan="7" class="tm-empty">暂无考试安排</td>
          </tr>
          <tr v-for="row in arrangements" :key="row.id">
            <td class="text-left">{{ row.courseName }}</td>
            <td class="text-left" style="white-space: normal !important;">
              <span v-for="name in row.classNames" :key="name" class="tm-chip tm-chip-gray" style="margin: 1px 2px;">{{ name }}</span>
            </td>
            <td>{{ row.examDate }}</td>
            <td style="font-size: 12px; color: #6b7280;">{{ row.startTime }} - {{ row.endTime }}</td>
            <td class="tm-mono">{{ row.duration }}</td>
            <td class="text-left" style="white-space: normal !important;">
              <template v-if="row.examRooms?.length">
                <div v-for="room in row.examRooms" :key="room.id" style="font-size: 11px; color: #6b7280;">
                  {{ room.classroomName }} ({{ room.actualCount }}/{{ room.capacity }})
                </div>
              </template>
              <span v-else style="font-size: 11px; color: #9ca3af;">未分配</span>
            </td>
            <td>
              <button class="tm-action" @click="showArrangementDialog(row)">编辑</button>
              <button class="tm-action" style="color: #2563eb;" @click="emit('selectArrangement', row)">考场</button>
              <button v-if="batch.status === 0" class="tm-action tm-action-danger" @click="deleteArrangement(row)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>

  <!-- Arrangement Drawer -->
  <Transition name="tm-drawer">
    <div v-if="arrangementDialogVisible" class="tm-drawer-overlay" @click.self="arrangementDialogVisible = false">
      <div class="tm-drawer">
        <div class="tm-drawer-header">
          <h3 class="tm-drawer-title">{{ arrangementForm.id ? '编辑考试安排' : '添加考试安排' }}</h3>
          <button class="tm-drawer-close" @click="arrangementDialogVisible = false">&times;</button>
        </div>
        <div class="tm-drawer-body">
          <div class="tm-section">
            <h4 class="tm-section-title">考试信息</h4>
            <div class="tm-field" :class="{ 'tm-error': formErrors.courseId }">
              <label class="tm-label">课程 <span class="req">*</span></label>
              <select v-model="arrangementForm.courseId" class="tm-field-select">
                <option :value="undefined" disabled>选择课程</option>
                <option v-for="c in courseOptions" :key="c.id" :value="c.id">{{ c.courseCode }} - {{ c.courseName }}</option>
              </select>
            </div>
            <div class="tm-field" :class="{ 'tm-error': formErrors.classIds }">
              <label class="tm-label">班级 <span class="req">*</span>（按住Ctrl多选）</label>
              <select v-model="arrangementForm.classIds" class="tm-field-select" multiple style="min-height: 80px;">
                <option v-for="cls in classOptions" :key="cls.id" :value="cls.id">{{ cls.name }}</option>
              </select>
            </div>
            <div class="tm-fields tm-cols-2">
              <div class="tm-field" :class="{ 'tm-error': formErrors.examDate }">
                <label class="tm-label">考试日期 <span class="req">*</span></label>
                <input v-model="arrangementForm.examDate" type="date" class="tm-input" />
              </div>
              <div class="tm-field" :class="{ 'tm-error': formErrors.duration }">
                <label class="tm-label">时长(分钟) <span class="req">*</span></label>
                <input v-model.number="arrangementForm.duration" type="number" min="30" max="300" step="30" class="tm-input" />
              </div>
            </div>
            <div class="tm-fields tm-cols-2">
              <div class="tm-field" :class="{ 'tm-error': formErrors.startTime }">
                <label class="tm-label">开始时间 <span class="req">*</span></label>
                <input v-model="arrangementForm.startTime" type="time" class="tm-input" />
              </div>
              <div class="tm-field" :class="{ 'tm-error': formErrors.endTime }">
                <label class="tm-label">结束时间 <span class="req">*</span></label>
                <input v-model="arrangementForm.endTime" type="time" class="tm-input" />
              </div>
            </div>
          </div>
        </div>
        <div class="tm-drawer-footer">
          <button class="tm-btn tm-btn-secondary" @click="arrangementDialogVisible = false">取消</button>
          <button class="tm-btn tm-btn-primary" :disabled="saving" @click="saveArrangement">{{ saving ? '保存中...' : '保存' }}</button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { ref, computed, watch, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { examApi } from '@/api/teaching'
import { courseApi } from '@/api/academic'
import { schoolClassApi } from '@/api/organization'
import type { ExamBatch, ExamArrangement, Course } from '@/types/teaching'
import { getExamTypeName, getStatusName, statusChipClass } from './examUtils'

const props = defineProps<{
  batch: ExamBatch | undefined
}>()

const emit = defineEmits<{
  selectArrangement: [arrangement: ExamArrangement]
  close: []
}>()

// State
const saving = ref(false)
const arrangements = ref<ExamArrangement[]>([])
const courseOptions = ref<Course[]>([])
const classOptions = ref<{ id: number | string; name: string }[]>([])

const arrangementDialogVisible = ref(false)
const arrangementForm = ref<Partial<ExamArrangement>>({})
const formErrors = reactive({ courseId: false, classIds: false, examDate: false, startTime: false, endTime: false, duration: false })

const arrangementsWithRooms = computed(() => arrangements.value.filter(a => a.examRooms && a.examRooms.length > 0).length)

watch(() => props.batch, (val) => {
  if (val) loadArrangements()
})

// Data loading
async function loadArrangements() {
  if (!props.batch) return
  try {
    const res: any = await examApi.getArrangements(props.batch.id)
    arrangements.value = res.data || res
  } catch (error) {
    console.error('Failed to load arrangements:', error)
    arrangements.value = []
  }
}

async function loadClassOptions() {
  try {
    const res = await schoolClassApi.getAll()
    const data: any = res
    classOptions.value = (Array.isArray(data) ? data : data.records || []).map((c: any) => ({ id: c.id, name: c.name || c.className }))
  } catch { /* */ }
}

async function loadCourseOptions() {
  try {
    courseOptions.value = await courseApi.listAll()
  } catch { /* */ }
}

// Validation
function validate() {
  const f = arrangementForm.value
  formErrors.courseId = !f.courseId
  formErrors.classIds = !f.classIds?.length
  formErrors.examDate = !f.examDate
  formErrors.startTime = !f.startTime
  formErrors.endTime = !f.endTime
  formErrors.duration = !f.duration
  return !Object.values(formErrors).some(Boolean)
}

// Operations
const showArrangementDialog = (arrangement?: ExamArrangement) => {
  Object.keys(formErrors).forEach(k => (formErrors as any)[k] = false)
  arrangementForm.value = arrangement ? { ...arrangement } : { duration: 120 }
  arrangementDialogVisible.value = true
}

const saveArrangement = async () => {
  if (!validate() || !props.batch) return
  saving.value = true
  try {
    if (arrangementForm.value.id) {
      await examApi.updateArrangement(props.batch.id, arrangementForm.value.id, arrangementForm.value)
    } else {
      await examApi.createArrangement(props.batch.id, arrangementForm.value)
    }
    ElMessage.success('保存成功')
    arrangementDialogVisible.value = false
    loadArrangements()
  } catch { ElMessage.error('保存失败') } finally { saving.value = false }
}

const deleteArrangement = async (arrangement: ExamArrangement) => {
  if (!props.batch) return
  await ElMessageBox.confirm('确定删除该考试安排吗？', '警告', { type: 'warning' })
  try {
    await examApi.deleteArrangement(props.batch.id, arrangement.id)
    ElMessage.success('删除成功')
    loadArrangements()
  } catch { ElMessage.error('删除失败') }
}

loadClassOptions()
loadCourseOptions()
</script>

<style>
@import '@/styles/teaching-ui.css';
.tm-sep { display: inline-block; width: 1px; height: 10px; background: #d1d5db; vertical-align: middle; margin: 0 4px; }
</style>
