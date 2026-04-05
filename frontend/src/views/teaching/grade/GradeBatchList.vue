<template>
  <div class="flex h-full flex-col">
    <!-- Header -->
    <div class="flex items-center justify-between border-b border-gray-200 bg-white px-6 py-4">
      <div>
        <h1 class="text-lg font-semibold text-gray-900">成绩管理</h1>
        <p class="mt-0.5 text-sm text-gray-500">管理课程成绩批次、录入与统计</p>
      </div>
      <button
        class="inline-flex items-center gap-1.5 rounded-lg bg-blue-600 px-3.5 py-2 text-sm font-medium text-white transition-colors hover:bg-blue-700"
        @click="showBatchDialog()"
      >
        <Plus class="h-4 w-4" />
        新建批次
      </button>
    </div>

    <!-- Stats Bar -->
    <div class="flex items-center gap-4 border-b border-gray-200 bg-white px-6 py-2.5">
      <span class="text-sm text-gray-500">批次总数 <span class="font-semibold text-gray-900">{{ total }}</span></span>
      <div class="h-3 w-px bg-gray-200" />
      <span class="text-sm text-gray-500">草稿 <span class="font-semibold text-gray-900">{{ statusCounts.draft }}</span></span>
      <div class="h-3 w-px bg-gray-200" />
      <span class="text-sm text-gray-500">已提交 <span class="font-semibold text-gray-900">{{ statusCounts.submitted }}</span></span>
      <div class="h-3 w-px bg-gray-200" />
      <span class="text-sm text-gray-500">已审核 <span class="font-semibold text-gray-900">{{ statusCounts.approved }}</span></span>
      <div class="h-3 w-px bg-gray-200" />
      <span class="text-sm text-gray-500">已发布 <span class="font-semibold text-gray-900">{{ statusCounts.published }}</span></span>
    </div>

    <!-- Filter Bar -->
    <div class="flex items-center gap-3 border-b border-gray-200 bg-white px-6 py-3">
      <el-select
        v-model="queryParams.semesterId"
        placeholder="选择学期"
        clearable
        class="w-44"
        @change="handleFilterChange"
      >
        <el-option v-for="sem in semesters" :key="sem.id" :value="sem.id" :label="sem.semesterName" />
      </el-select>
      <el-select
        v-model="queryParams.gradeType"
        placeholder="成绩类型"
        clearable
        class="w-32"
        @change="handleFilterChange"
      >
        <el-option :value="1" label="平时成绩" />
        <el-option :value="2" label="期中成绩" />
        <el-option :value="3" label="期末成绩" />
        <el-option :value="4" label="总评成绩" />
      </el-select>
      <el-select
        v-model="queryParams.status"
        placeholder="状态"
        clearable
        class="w-28"
        @change="handleFilterChange"
      >
        <el-option :value="0" label="草稿" />
        <el-option :value="1" label="已提交" />
        <el-option :value="2" label="已审核" />
        <el-option :value="3" label="已发布" />
      </el-select>
      <button
        class="ml-auto inline-flex items-center gap-1.5 rounded-lg bg-gray-100 px-3.5 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-200 disabled:opacity-50"
        :disabled="!queryParams.semesterId || exporting"
        @click="doExportGrades"
      >
        {{ exporting ? '导出中...' : '导出Excel' }}
      </button>
    </div>

    <!-- Content -->
    <div class="flex-1 overflow-y-auto px-6 pt-5 pb-6">
      <!-- Loading -->
      <div v-if="loading" class="flex items-center justify-center py-20">
        <div class="h-8 w-8 animate-spin rounded-full border-2 border-blue-600 border-t-transparent"></div>
      </div>

      <!-- Table Card -->
      <div v-else class="rounded-xl border border-gray-200 bg-white">
        <el-table :data="batches" stripe class="rounded-xl">
          <el-table-column prop="batchName" label="批次名称" min-width="180" />
          <el-table-column prop="courseName" label="课程" width="150" />
          <el-table-column prop="className" label="班级" width="120" />
          <el-table-column prop="gradeType" label="成绩类型" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getGradeTypeTag(row.gradeType)" size="small">
                {{ getGradeTypeName(row.gradeType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getStatusTag(row.status)" size="small">
                {{ getStatusName(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="inputDeadline" label="录入截止" width="120" />
          <el-table-column prop="createdByName" label="创建人" width="100" />
          <el-table-column label="操作" width="250" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text @click="emit('enterGrades', row)">录入</el-button>
              <el-button size="small" text @click="emit('viewStatistics', row)">统计</el-button>
              <el-button
                v-if="row.status === 0"
                size="small"
                text
                type="primary"
                @click="submitBatch(row)"
              >提交</el-button>
              <el-button
                v-if="row.status === 1"
                size="small"
                text
                type="success"
                @click="approveBatch(row)"
              >审核</el-button>
              <el-button
                v-if="row.status === 2"
                size="small"
                text
                type="warning"
                @click="publishBatch(row)"
              >发布</el-button>
              <el-button size="small" text @click="exportBatchGrades(row)">导出</el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- Pagination -->
        <div class="flex items-center justify-end border-t border-gray-100 px-5 py-3">
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

      <!-- Empty state -->
      <div
        v-if="!loading && batches.length === 0"
        class="mt-6 flex flex-col items-center justify-center py-16 text-gray-400"
      >
        <FileText class="mb-3 h-12 w-12 text-gray-300" />
        <p class="text-sm">暂无成绩批次数据</p>
      </div>
    </div>

    <!-- Batch Dialog -->
    <el-dialog
      v-model="batchDialogVisible"
      :title="batchForm.id ? '编辑批次' : '新建成绩批次'"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form ref="batchFormRef" :model="batchForm" :rules="batchRules" label-width="100px">
        <el-form-item label="批次名称" prop="batchName">
          <el-input v-model="batchForm.batchName" placeholder="如：高等数学-期末成绩" />
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
            <el-form-item label="成绩类型" prop="gradeType">
              <el-select v-model="batchForm.gradeType" style="width: 100%">
                <el-option :value="1" label="平时成绩" />
                <el-option :value="2" label="期中成绩" />
                <el-option :value="3" label="期末成绩" />
                <el-option :value="4" label="总评成绩" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="课程" prop="courseId">
              <el-select
                v-model="batchForm.courseId"
                filterable
                remote
                :remote-method="searchCourses"
                placeholder="搜索课程"
                style="width: 100%"
              >
                <el-option
                  v-for="c in courseOptions"
                  :key="c.id"
                  :value="c.id"
                  :label="`${c.courseCode} - ${c.courseName}`"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="班级" prop="classId">
              <el-select
                v-model="batchForm.classId"
                placeholder="选择班级"
                filterable
                style="width: 100%"
                :loading="classOptionsLoading"
              >
                <el-option v-for="cls in classOptions" :key="cls.id" :value="cls.id" :label="cls.name" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="录入截止">
          <el-date-picker v-model="batchForm.inputDeadline" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
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
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, FileText } from 'lucide-vue-next'
import { gradeApi } from '@/api/teaching'
import { semesterApi } from '@/api/calendar'
import { courseApi } from '@/api/academic'
import { schoolClassApi } from '@/api/organization'
import type { GradeBatch, Semester, GradeQueryParams } from '@/types/teaching'
import type { Course } from '@/types/academic'
import { getGradeTypeName, getGradeTypeTag, getStatusName, getStatusTag } from './gradeHelpers'

const emit = defineEmits<{
  enterGrades: [batch: GradeBatch]
  viewStatistics: [batch: GradeBatch]
}>()

// State
const loading = ref(false)
const saving = ref(false)
const exporting = ref(false)
const classOptionsLoading = ref(false)
const batches = ref<GradeBatch[]>([])
const semesters = ref<Semester[]>([])
const courseOptions = ref<Course[]>([])
const classOptions = ref<{ id: number | string; name: string }[]>([])
const total = ref(0)

// Computed status counts from batches data
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

// Dialog state
const batchDialogVisible = ref(false)

// Form refs
const batchFormRef = ref<FormInstance>()
const batchForm = ref<Partial<GradeBatch>>({})

const queryParams = reactive<GradeQueryParams>({
  semesterId: undefined,
  gradeType: undefined,
  status: undefined,
  page: 1,
  size: 10,
})

// Validation rules
const batchRules: FormRules = {
  batchName: [{ required: true, message: '请输入批次名称', trigger: 'blur' }],
  semesterId: [{ required: true, message: '请选择学期', trigger: 'change' }],
  gradeType: [{ required: true, message: '请选择成绩类型', trigger: 'change' }],
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  classId: [{ required: true, message: '请选择班级', trigger: 'change' }],
}

// ---- Data Loading ----

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
    ElMessage.error('加载学期列表失败')
  }
}

const loadClassOptions = async () => {
  classOptionsLoading.value = true
  try {
    const res = await schoolClassApi.getAll()
    const data: any = res
    classOptions.value = (Array.isArray(data) ? data : data.records || []).map((c: any) => ({
      id: c.id,
      name: c.name || c.className,
    }))
  } catch (error) {
    console.error('Failed to load classes:', error)
    classOptions.value = []
  } finally {
    classOptionsLoading.value = false
  }
}

const loadBatches = async () => {
  loading.value = true
  try {
    const res: any = await gradeApi.listBatches(queryParams)
    const data = res.data || res
    batches.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    console.error('Failed to load batches:', error)
    ElMessage.error('加载成绩批次失败')
    batches.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleFilterChange = () => {
  queryParams.page = 1
  loadBatches()
}

const searchCourses = async (query: string) => {
  if (query.length < 2) return
  try {
    const res: any = await courseApi.list({ keyword: query, pageNum: 1, pageSize: 20 })
    const data = res.data || res
    courseOptions.value = data.records || []
  } catch (error) {
    console.error('Failed to search courses:', error)
    courseOptions.value = []
  }
}

// ---- Batch CRUD ----

const showBatchDialog = (batch?: GradeBatch) => {
  batchForm.value = batch
    ? { ...batch }
    : { semesterId: queryParams.semesterId, gradeType: 3 }
  batchDialogVisible.value = true
}

const saveBatch = async () => {
  await batchFormRef.value?.validate()
  saving.value = true
  try {
    if (batchForm.value.id) {
      await gradeApi.updateBatch(batchForm.value.id, batchForm.value)
    } else {
      await gradeApi.createBatch(batchForm.value)
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

// ---- Batch Status Actions ----

const submitBatch = async (batch: GradeBatch) => {
  await ElMessageBox.confirm('提交后成绩将进入审核流程，确定提交吗？', '提示', { type: 'info' })
  try {
    await gradeApi.submitBatch(batch.id)
    ElMessage.success('提交成功')
    loadBatches()
  } catch (error) {
    ElMessage.error('提交失败')
  }
}

const approveBatch = async (batch: GradeBatch) => {
  await ElMessageBox.confirm('确定审核通过该批次成绩吗？', '提示', { type: 'info' })
  try {
    await gradeApi.approveBatch(batch.id)
    ElMessage.success('审核通过')
    loadBatches()
  } catch (error) {
    ElMessage.error('审核失败')
  }
}

const publishBatch = async (batch: GradeBatch) => {
  await ElMessageBox.confirm('发布后学生将可查看成绩，确定发布吗？', '提示', { type: 'warning' })
  try {
    await gradeApi.publishBatch(batch.id)
    ElMessage.success('发布成功')
    loadBatches()
  } catch (error) {
    ElMessage.error('发布失败')
  }
}

// ---- Export ----

const doExportGrades = async () => {
  if (!queryParams.semesterId) {
    ElMessage.warning('请先选择学期')
    return
  }
  exporting.value = true
  try {
    const params: any = { semesterId: queryParams.semesterId }
    const res = await gradeApi.exportGradesByFilter(params)
    const blob = new Blob([res as any], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '成绩导出.xlsx'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

const exportBatchGrades = async (batch: GradeBatch) => {
  try {
    const res = await gradeApi.exportGrades(batch.id)
    const blob = new Blob([res as any], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${batch.batchName}_成绩.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
  } catch (error) {
    ElMessage.error('导出失败')
  }
}

// ---- Init ----

onMounted(async () => {
  await loadSemesters()
  loadBatches()
  loadClassOptions()
})
</script>
