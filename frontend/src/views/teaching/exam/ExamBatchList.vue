<template>
  <div class="flex h-full flex-col bg-gray-50">
    <!-- Header -->
    <div class="flex items-center justify-between border-b border-gray-200 bg-white px-6 py-4">
      <div>
        <h1 class="text-lg font-semibold text-gray-900">考试管理</h1>
        <p class="mt-0.5 text-sm text-gray-500">管理考试批次、考场安排与监考分配</p>
      </div>
      <button
        class="inline-flex items-center gap-1.5 rounded-lg bg-blue-600 px-3.5 py-2 text-sm font-medium text-white transition-colors hover:bg-blue-700"
        @click="showBatchDialog()"
      >
        <Plus class="h-4 w-4" />
        新建考试批次
      </button>
    </div>

    <!-- Stats Bar -->
    <div class="flex items-center gap-4 border-b border-gray-200 bg-white px-6 py-2.5">
      <span class="text-sm text-gray-500">批次总数 <span class="font-semibold text-gray-900">{{ total }}</span></span>
      <div class="h-3 w-px bg-gray-200" />
      <span class="text-sm text-gray-500">草稿 <span class="font-semibold text-gray-900">{{ statusCounts.draft }}</span></span>
      <div class="h-3 w-px bg-gray-200" />
      <span class="text-sm text-gray-500">已发布 <span class="font-semibold text-gray-900">{{ statusCounts.published }}</span></span>
      <div class="h-3 w-px bg-gray-200" />
      <span class="text-sm text-gray-500">进行中 <span class="font-semibold text-gray-900">{{ statusCounts.ongoing }}</span></span>
      <div class="h-3 w-px bg-gray-200" />
      <span class="text-sm text-gray-500">已结束 <span class="font-semibold text-gray-900">{{ statusCounts.finished }}</span></span>
    </div>

    <!-- Filter Bar -->
    <div class="flex items-center gap-3 border-b border-gray-200 bg-white px-6 py-3">
      <el-select v-model="queryParams.semesterId" placeholder="选择学期" clearable style="width: 200px" @change="loadBatches">
        <el-option v-for="sem in semesters" :key="sem.id" :value="sem.id" :label="sem.semesterName" />
      </el-select>
      <el-select v-model="queryParams.examType" placeholder="考试类型" clearable style="width: 140px" @change="loadBatches">
        <el-option :value="1" label="期中考试" />
        <el-option :value="2" label="期末考试" />
        <el-option :value="3" label="补考" />
        <el-option :value="4" label="重修考试" />
      </el-select>
      <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 120px" @change="loadBatches">
        <el-option :value="0" label="草稿" />
        <el-option :value="1" label="已发布" />
        <el-option :value="2" label="进行中" />
        <el-option :value="3" label="已结束" />
      </el-select>
    </div>

    <!-- Content -->
    <div class="flex-1 overflow-y-auto px-6 pt-5 pb-6">
      <!-- Loading -->
      <div v-if="loading" class="flex items-center justify-center py-20">
        <div class="h-8 w-8 animate-spin rounded-full border-2 border-blue-600 border-t-transparent"></div>
      </div>

      <!-- Table Card -->
      <div v-else class="rounded-xl border border-gray-200 bg-white">
        <el-table :data="batches" class="w-full">
          <el-table-column prop="name" label="批次名称" min-width="200" />
          <el-table-column prop="examType" label="考试类型" width="110" align="center">
            <template #default="{ row }">
              <span
                class="inline-flex rounded px-1.5 py-0.5 text-xs font-medium"
                :class="examTypeBadgeClass(row.examType)"
              >
                {{ getExamTypeName(row.examType) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="考试时间" width="210">
            <template #default="{ row }">
              <span class="text-sm text-gray-600">{{ row.startDate }} ~ {{ row.endDate }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="{ row }">
              <span
                class="inline-flex rounded px-1.5 py-0.5 text-xs font-medium"
                :class="statusBadgeClass(row.status)"
              >
                {{ getStatusName(row.status) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <div class="flex items-center gap-1">
                <button class="rounded px-2 py-1 text-xs font-medium text-blue-600 hover:bg-blue-50" @click="emit('select', row)">安排</button>
                <button class="rounded px-2 py-1 text-xs font-medium text-gray-600 hover:bg-gray-100" @click="showBatchDialog(row)">编辑</button>
                <button
                  v-if="row.status === 0"
                  class="rounded px-2 py-1 text-xs font-medium text-emerald-600 hover:bg-emerald-50"
                  @click="publishBatch(row)"
                >发布</button>
                <button
                  v-if="row.status === 0"
                  class="rounded px-2 py-1 text-xs font-medium text-red-500 hover:bg-red-50"
                  @click="deleteBatch(row)"
                >删除</button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <!-- Pagination -->
        <div class="flex items-center justify-end border-t border-gray-100 px-4 py-3">
          <el-pagination
            v-model:current-page="queryParams.page"
            v-model:page-size="queryParams.size"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @size-change="loadBatches"
            @current-change="loadBatches"
          />
        </div>
      </div>
    </div>

    <!-- Batch Dialog -->
    <el-dialog
      v-model="batchDialogVisible"
      :title="batchForm.id ? '编辑考试批次' : '新建考试批次'"
      width="600px"
    >
      <el-form ref="batchFormRef" :model="batchForm" :rules="batchRules" label-width="100px">
        <el-form-item label="批次名称" prop="name">
          <el-input v-model="batchForm.name" placeholder="如：2025-2026学年第一学期期末考试" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="学期" prop="semesterId">
              <el-select v-model="batchForm.semesterId" style="width: 100%">
                <el-option v-for="sem in semesters" :key="sem.id" :value="sem.id" :label="sem.semesterName" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="考试类型" prop="examType">
              <el-select v-model="batchForm.examType" style="width: 100%">
                <el-option :value="1" label="期中考试" />
                <el-option :value="2" label="期末考试" />
                <el-option :value="3" label="补考" />
                <el-option :value="4" label="重修考试" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="开始日期" prop="startDate">
              <el-date-picker v-model="batchForm.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束日期" prop="endDate">
              <el-date-picker v-model="batchForm.endDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="batchForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveBatch">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Plus } from 'lucide-vue-next'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { examApi } from '@/api/teaching'
import { semesterApi } from '@/api/calendar'
import type { ExamBatch, Semester, ExamBatchQueryParams } from '@/types/teaching'
import { examTypeBadgeClass, statusBadgeClass, getExamTypeName, getStatusName } from './examUtils'

const emit = defineEmits<{
  (e: 'select', batch: ExamBatch): void
}>()

// State
const loading = ref(false)
const saving = ref(false)
const batches = ref<ExamBatch[]>([])
const semesters = ref<Semester[]>([])
const total = ref(0)

// Dialog visibility
const batchDialogVisible = ref(false)

// Forms
const batchFormRef = ref<FormInstance>()
const batchForm = ref<Partial<ExamBatch>>({})

const queryParams = reactive<ExamBatchQueryParams>({
  semesterId: undefined,
  examType: undefined,
  status: undefined,
  page: 1,
  size: 10,
})

// Computed
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

// Validation rules
const batchRules: FormRules = {
  name: [{ required: true, message: '请输入批次名称', trigger: 'blur' }],
  semesterId: [{ required: true, message: '请选择学期', trigger: 'change' }],
  examType: [{ required: true, message: '请选择考试类型', trigger: 'change' }],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  endDate: [{ required: true, message: '请选择结束日期', trigger: 'change' }],
}

// Data loading
const loadSemesters = async () => {
  try {
    const res: any = await semesterApi.list()
    semesters.value = res.data || res
    if (semesters.value.length > 0) {
      const current = semesters.value.find(s => s.isCurrent)
      if (current) {
        queryParams.semesterId = current.id
      }
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
const showBatchDialog = (batch?: ExamBatch) => {
  batchForm.value = batch
    ? { ...batch }
    : { semesterId: queryParams.semesterId, examType: 2 }
  batchDialogVisible.value = true
}

const saveBatch = async () => {
  await batchFormRef.value?.validate()
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

// Init
onMounted(async () => {
  await loadSemesters()
  loadBatches()
})
</script>
