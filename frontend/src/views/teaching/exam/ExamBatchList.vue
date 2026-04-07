<template>
  <div>
    <!-- Header row -->
    <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px;">
      <div class="tm-stats" style="margin-top: 0;">
        <span>批次总数 <b>{{ total }}</b></span>
        <span class="sep" />
        <span>草稿 <b>{{ statusCounts.draft }}</b></span>
        <span class="sep" />
        <span><span class="dot dot-green" />已发布 <b>{{ statusCounts.published }}</b></span>
        <span class="sep" />
        <span><span class="dot dot-green" />进行中 <b>{{ statusCounts.ongoing }}</b></span>
        <span class="sep" />
        <span>已结束 <b>{{ statusCounts.finished }}</b></span>
      </div>
      <button class="tm-btn tm-btn-primary" @click="showBatchDialog()">新建考试批次</button>
    </div>

    <!-- Table -->
    <table class="tm-table">
      <colgroup>
        <col />
        <col style="width: 100px" />
        <col style="width: 200px" />
        <col style="width: 90px" />
        <col style="width: 200px" />
      </colgroup>
      <thead>
        <tr>
          <th class="text-left">批次名称</th>
          <th>考试类型</th>
          <th>考试时间</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="loading">
          <td colspan="5" class="tm-empty">
            <span class="tm-spin" style="display:inline-block;width:16px;height:16px;border:2px solid #e5e7eb;border-top-color:#2563eb;border-radius:50%;" /> 加载中...
          </td>
        </tr>
        <tr v-else-if="batches.length === 0">
          <td colspan="5" class="tm-empty">暂无考试批次数据</td>
        </tr>
        <tr v-for="row in batches" :key="row.id" @click="emit('select', row)" style="cursor: pointer;">
          <td class="text-left" style="font-weight: 500;">{{ row.name }}</td>
          <td><span :class="['tm-chip', examTypeChipClass(row.examType)]">{{ getExamTypeName(row.examType) }}</span></td>
          <td style="font-size: 12px; color: #6b7280;">{{ row.startDate }} ~ {{ row.endDate }}</td>
          <td><span :class="['tm-chip', statusChipClass(row.status)]">{{ getStatusName(row.status) }}</span></td>
          <td>
            <button class="tm-action" @click.stop="emit('select', row)">安排</button>
            <button class="tm-action" @click.stop="showBatchDialog(row)">编辑</button>
            <button v-if="row.status === 0" class="tm-action" style="color: #16a34a;" @click.stop="publishBatch(row)">发布</button>
            <button v-if="row.status === 0" class="tm-action tm-action-danger" @click.stop="deleteBatch(row)">删除</button>
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
          <h3 class="tm-drawer-title">{{ batchForm.id ? '编辑考试批次' : '新建考试批次' }}</h3>
          <button class="tm-drawer-close" @click="batchDialogVisible = false">&times;</button>
        </div>
        <div class="tm-drawer-body">
          <div class="tm-section">
            <h4 class="tm-section-title">基本信息</h4>
            <div class="tm-field" :class="{ 'tm-error': formErrors.name }">
              <label class="tm-label">批次名称 <span class="req">*</span></label>
              <input v-model="batchForm.name" class="tm-input" placeholder="如：2025-2026学年第一学期期末考试" />
            </div>
            <div class="tm-fields tm-cols-2">
              <div class="tm-field" :class="{ 'tm-error': formErrors.semesterId }">
                <label class="tm-label">学期 <span class="req">*</span></label>
                <select v-model="batchForm.semesterId" class="tm-field-select">
                  <option :value="undefined" disabled>选择学期</option>
                  <option v-for="sem in semesters" :key="sem.id" :value="sem.id">{{ sem.semesterName }}</option>
                </select>
              </div>
              <div class="tm-field" :class="{ 'tm-error': formErrors.examType }">
                <label class="tm-label">考试类型 <span class="req">*</span></label>
                <select v-model="batchForm.examType" class="tm-field-select">
                  <option :value="undefined" disabled>选择类型</option>
                  <option :value="1">期中考试</option>
                  <option :value="2">期末考试</option>
                  <option :value="3">补考</option>
                  <option :value="4">重修考试</option>
                </select>
              </div>
            </div>
            <div class="tm-fields tm-cols-2">
              <div class="tm-field" :class="{ 'tm-error': formErrors.startDate }">
                <label class="tm-label">开始日期 <span class="req">*</span></label>
                <input v-model="batchForm.startDate" type="date" class="tm-input" />
              </div>
              <div class="tm-field" :class="{ 'tm-error': formErrors.endDate }">
                <label class="tm-label">结束日期 <span class="req">*</span></label>
                <input v-model="batchForm.endDate" type="date" class="tm-input" />
              </div>
            </div>
            <div class="tm-field">
              <label class="tm-label">备注</label>
              <textarea v-model="batchForm.remark" class="tm-textarea" rows="2"></textarea>
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
import { examApi } from '@/api/teaching'
import { semesterApi } from '@/api/calendar'
import type { ExamBatch, Semester, ExamBatchQueryParams } from '@/types/teaching'
import { examTypeChipClass, statusChipClass, getExamTypeName, getStatusName } from './examUtils'

const emit = defineEmits<{
  (e: 'select', batch: ExamBatch): void
}>()

// State
const loading = ref(false)
const saving = ref(false)
const batches = ref<ExamBatch[]>([])
const semesters = ref<Semester[]>([])
const total = ref(0)

const batchDialogVisible = ref(false)
const batchForm = ref<Partial<ExamBatch>>({})
const formErrors = reactive({ name: false, semesterId: false, examType: false, startDate: false, endDate: false })

const queryParams = reactive<ExamBatchQueryParams>({
  semesterId: undefined,
  examType: undefined,
  status: undefined,
  page: 1,
  size: 10,
})

const statusCounts = computed(() => {
  const counts = { draft: 0, published: 0, ongoing: 0, finished: 0 }
  batches.value.forEach(b => {
    if (b.status === 0) counts.draft++
    else if (b.status === 1) counts.published++
    else if (b.status === 2) counts.ongoing++
    else if (b.status === 3) counts.finished++
  })
  return counts
})

// Data loading
const loadSemesters = async () => {
  try {
    const res: any = await semesterApi.list()
    semesters.value = res.data || res
    if (semesters.value.length > 0) {
      const current = semesters.value.find(s => s.isCurrent)
      if (current) queryParams.semesterId = current.id
    }
  } catch (error) {
    console.error('Failed to load semesters:', error)
  }
}

const loadBatches = async () => {
  loading.value = true
  try {
    const res: any = await examApi.listBatches(queryParams)
    const data = res.data || res
    batches.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    console.error('Failed to load batches:', error)
  } finally {
    loading.value = false
  }
}

// Batch operations
function validate() {
  const f = batchForm.value
  formErrors.name = !f.name?.trim()
  formErrors.semesterId = !f.semesterId
  formErrors.examType = !f.examType
  formErrors.startDate = !f.startDate
  formErrors.endDate = !f.endDate
  return !Object.values(formErrors).some(Boolean)
}

const showBatchDialog = (batch?: ExamBatch) => {
  Object.keys(formErrors).forEach(k => (formErrors as any)[k] = false)
  batchForm.value = batch
    ? { ...batch }
    : { semesterId: queryParams.semesterId, examType: 2 }
  batchDialogVisible.value = true
}

const saveBatch = async () => {
  if (!validate()) return
  saving.value = true
  try {
    if (batchForm.value.id) {
      await examApi.updateBatch(batchForm.value.id, batchForm.value)
    } else {
      await examApi.createBatch(batchForm.value)
    }
    ElMessage.success('保存成功')
    batchDialogVisible.value = false
    loadBatches()
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const publishBatch = async (batch: ExamBatch) => {
  await ElMessageBox.confirm('发布后考试安排将对所有用户可见，确定发布吗？', '提示', { type: 'warning' })
  try {
    await examApi.publishBatch(batch.id)
    ElMessage.success('发布成功')
    loadBatches()
  } catch (error) {
    ElMessage.error('发布失败')
  }
}

const deleteBatch = async (batch: ExamBatch) => {
  await ElMessageBox.confirm('确定删除该考试批次吗？', '警告', { type: 'warning' })
  try {
    await examApi.deleteBatch(batch.id)
    ElMessage.success('删除成功')
    loadBatches()
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

onMounted(async () => {
  await loadSemesters()
  loadBatches()
})
</script>

<style>
@import '@/styles/teaching-ui.css';
</style>
