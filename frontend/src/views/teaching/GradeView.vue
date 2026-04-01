<template>
  <div class="flex h-full flex-col bg-gray-50">
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
        <el-option v-for="sem in semesters" :key="sem.id" :value="sem.id" :label="sem.name" />
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
              <el-button size="small" text @click="enterGrades(row)">录入</el-button>
              <el-button size="small" text @click="viewStatistics(row)">统计</el-button>
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
              <el-button size="small" text @click="exportGrades(row)">导出</el-button>
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
                <el-option v-for="sem in semesters" :key="sem.id" :value="sem.id" :label="sem.name" />
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
                  :label="`${c.code} - ${c.name}`"
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

    <!-- Grade Entry Drawer -->
    <el-drawer v-model="gradeEntryDrawerVisible" title="成绩录入" size="80%">
      <div class="flex items-start justify-between">
        <div>
          <h3 class="text-base font-semibold text-gray-900">{{ currentBatch?.batchName }}</h3>
          <div class="mt-1 flex items-center gap-3 text-sm text-gray-500">
            <span>{{ currentBatch?.courseName }}</span>
            <div class="h-3 w-px bg-gray-200" />
            <span>{{ currentBatch?.className }}</span>
            <div class="h-3 w-px bg-gray-200" />
            <el-tag :type="getGradeTypeTag(currentBatch?.gradeType || 0)" size="small">
              {{ getGradeTypeName(currentBatch?.gradeType || 0) }}
            </el-tag>
          </div>
        </div>
        <div class="flex items-center gap-2">
          <el-button @click="showImportDialog">批量导入</el-button>
          <el-button type="primary" :loading="saving" @click="saveAllGrades">保存全部</el-button>
        </div>
      </div>

      <div v-if="gradeEntryLoading" class="flex items-center justify-center py-20">
        <div class="h-8 w-8 animate-spin rounded-full border-2 border-blue-600 border-t-transparent"></div>
      </div>

      <el-table v-else :data="studentGrades" border class="mt-4">
        <el-table-column prop="studentNo" label="学号" width="120" />
        <el-table-column prop="studentName" label="姓名" width="100" />
        <el-table-column label="成绩" width="120">
          <template #default="{ row }">
            <el-input-number
              v-model="row.totalScore"
              :min="0"
              :max="100"
              :precision="1"
              size="small"
              style="width: 100px"
            />
          </template>
        </el-table-column>
        <el-table-column prop="gradeLevel" label="等级" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.gradeLevel" :type="getGradeLevelTag(row.gradeLevel) as any" size="small">
              {{ row.gradeLevel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="gradePoint" label="绩点" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '已录入' : '未录入' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注">
          <template #default="{ row }">
            <el-input v-model="row.remark" size="small" placeholder="备注..." />
          </template>
        </el-table-column>
      </el-table>

      <div
        v-if="!gradeEntryLoading && studentGrades.length === 0"
        class="flex flex-col items-center justify-center py-12 text-gray-400"
      >
        <p class="text-sm">暂无学生成绩数据</p>
      </div>
    </el-drawer>

    <!-- Statistics Drawer -->
    <el-drawer v-model="statisticsDrawerVisible" title="成绩统计" size="60%">
      <div v-if="statisticsLoading" class="flex items-center justify-center py-20">
        <div class="h-8 w-8 animate-spin rounded-full border-2 border-blue-600 border-t-transparent"></div>
      </div>

      <template v-else-if="statistics">
        <!-- Stats summary cards -->
        <div class="grid grid-cols-4 gap-4">
          <div class="rounded-lg border border-gray-200 bg-gray-50 px-4 py-3">
            <div class="text-xs text-gray-500">总人数</div>
            <div class="mt-1 text-xl font-semibold text-gray-900">{{ statistics.totalCount }}</div>
          </div>
          <div class="rounded-lg border border-gray-200 bg-gray-50 px-4 py-3">
            <div class="text-xs text-gray-500">及格人数</div>
            <div class="mt-1 text-xl font-semibold text-gray-900">
              {{ statistics.passCount }}
              <span class="ml-1 text-xs font-normal text-gray-400">({{ (statistics.passRate * 100).toFixed(1) }}%)</span>
            </div>
          </div>
          <div class="rounded-lg border border-gray-200 bg-gray-50 px-4 py-3">
            <div class="text-xs text-gray-500">优秀人数</div>
            <div class="mt-1 text-xl font-semibold text-gray-900">
              {{ statistics.excellentCount }}
              <span class="ml-1 text-xs font-normal text-gray-400">({{ (statistics.excellentRate * 100).toFixed(1) }}%)</span>
            </div>
          </div>
          <div class="rounded-lg border border-gray-200 bg-gray-50 px-4 py-3">
            <div class="text-xs text-gray-500">平均分</div>
            <div class="mt-1 text-xl font-semibold text-gray-900">{{ statistics.averageScore?.toFixed(1) }}</div>
          </div>
        </div>

        <!-- Distribution & Overview -->
        <div class="mt-5 grid grid-cols-2 gap-4">
          <!-- Distribution -->
          <div class="rounded-xl border border-gray-200 bg-white">
            <div class="border-b border-gray-100 px-5 py-3 text-sm font-medium text-gray-900">成绩分布</div>
            <div class="space-y-3 px-5 py-4">
              <div
                v-for="item in statistics.distribution"
                :key="item.range"
                class="flex items-center gap-3"
              >
                <span class="w-14 text-xs text-gray-500">{{ item.range }}</span>
                <div class="flex-1 overflow-hidden rounded bg-gray-100">
                  <div
                    class="h-5 rounded bg-blue-500 transition-all duration-300"
                    :style="{ width: `${item.percentage}%` }"
                  ></div>
                </div>
                <span class="w-24 text-right text-xs text-gray-500">{{ item.count }}人 ({{ item.percentage.toFixed(1) }}%)</span>
              </div>
            </div>
          </div>

          <!-- Overview -->
          <div class="rounded-xl border border-gray-200 bg-white">
            <div class="border-b border-gray-100 px-5 py-3 text-sm font-medium text-gray-900">成绩概览</div>
            <div class="px-5 py-4">
              <el-descriptions :column="2" border>
                <el-descriptions-item label="最高分">{{ statistics.maxScore }}</el-descriptions-item>
                <el-descriptions-item label="最低分">{{ statistics.minScore }}</el-descriptions-item>
                <el-descriptions-item label="及格率">{{ (statistics.passRate * 100).toFixed(1) }}%</el-descriptions-item>
                <el-descriptions-item label="优秀率">{{ (statistics.excellentRate * 100).toFixed(1) }}%</el-descriptions-item>
              </el-descriptions>
            </div>
          </div>
        </div>
      </template>

      <div
        v-else
        class="flex flex-col items-center justify-center py-16 text-gray-400"
      >
        <p class="text-sm">暂无统计数据</p>
      </div>
    </el-drawer>

    <!-- Import Dialog -->
    <el-dialog v-model="importDialogVisible" title="批量导入成绩" width="500px" :close-on-click-modal="false">
      <el-alert type="info" :closable="false" class="mb-5">
        请先下载导入模板，按模板格式填写成绩后上传。
      </el-alert>
      <el-form label-width="100px">
        <el-form-item label="下载模板">
          <el-button @click="downloadTemplate">下载模板</el-button>
        </el-form-item>
        <el-form-item label="上传文件">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".xlsx,.xls"
            :on-change="handleFileChange"
          >
            <template #trigger>
              <el-button type="primary">选择文件</el-button>
            </template>
            <template #tip>
              <div class="mt-1 text-xs text-gray-400">只能上传 xlsx/xls 文件</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="importing" @click="importGrades">导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type UploadInstance } from 'element-plus'
import { Plus, FileText } from 'lucide-vue-next'
import { gradeApi, semesterApi, courseApi } from '@/api/teaching'
import { schoolClassApi } from '@/api/organization'
import type { GradeBatch, StudentGrade, GradeStatistics, Semester, Course, GradeQueryParams } from '@/types/teaching'

// State
const loading = ref(false)
const saving = ref(false)
const importing = ref(false)
const exporting = ref(false)
const gradeEntryLoading = ref(false)
const statisticsLoading = ref(false)
const classOptionsLoading = ref(false)
const batches = ref<GradeBatch[]>([])
const studentGrades = ref<StudentGrade[]>([])
const statistics = ref<GradeStatistics>()
const semesters = ref<Semester[]>([])
const courseOptions = ref<Course[]>([])
const classOptions = ref<{ id: number | string; name: string }[]>([])
const total = ref(0)
const currentBatch = ref<GradeBatch>()
const selectedFile = ref<File>()

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

// Dialog / Drawer state
const batchDialogVisible = ref(false)
const gradeEntryDrawerVisible = ref(false)
const statisticsDrawerVisible = ref(false)
const importDialogVisible = ref(false)

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
    const res: any = await courseApi.list({ keyword: query, page: 1, size: 20 })
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

// ---- Grade Entry ----

const enterGrades = async (batch: GradeBatch) => {
  currentBatch.value = batch
  gradeEntryDrawerVisible.value = true
  gradeEntryLoading.value = true
  try {
    const res: any = await gradeApi.getGrades(batch.id)
    studentGrades.value = res.data || res
    if (!Array.isArray(studentGrades.value)) {
      studentGrades.value = []
    }
  } catch (error) {
    console.error('Failed to load grades:', error)
    ElMessage.error('加载学生成绩数据失败')
    studentGrades.value = []
  } finally {
    gradeEntryLoading.value = false
  }
}

const saveAllGrades = async () => {
  if (!currentBatch.value) return
  saving.value = true
  try {
    await gradeApi.batchRecordGrades(
      currentBatch.value.id,
      studentGrades.value.map(g => ({
        studentId: g.studentId,
        totalScore: g.totalScore || 0,
        remark: g.remark,
      }))
    )
    ElMessage.success('保存成功')
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

// ---- Statistics ----

const viewStatistics = async (batch: GradeBatch) => {
  currentBatch.value = batch
  statisticsDrawerVisible.value = true
  statisticsLoading.value = true
  statistics.value = undefined
  try {
    const res: any = await gradeApi.getStatistics({ batchId: batch.id })
    statistics.value = res.data || res
  } catch (error) {
    console.error('Failed to load statistics:', error)
    ElMessage.error('加载统计数据失败')
    statistics.value = undefined
  } finally {
    statisticsLoading.value = false
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

// ---- Import / Export ----

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

const exportGrades = async (batch: GradeBatch) => {
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

const showImportDialog = () => {
  selectedFile.value = undefined
  importDialogVisible.value = true
}

const downloadTemplate = async () => {
  if (!currentBatch.value) return
  try {
    const res = await gradeApi.getImportTemplate(currentBatch.value.id)
    const blob = new Blob([res as any], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '成绩导入模板.xlsx'
    link.click()
    window.URL.revokeObjectURL(url)
  } catch (error) {
    ElMessage.error('下载失败')
  }
}

const handleFileChange = (file: any) => {
  selectedFile.value = file.raw
}

const importGrades = async () => {
  if (!currentBatch.value || !selectedFile.value) {
    ElMessage.warning('请选择文件')
    return
  }
  importing.value = true
  try {
    await gradeApi.importGrades(currentBatch.value.id, selectedFile.value)
    ElMessage.success('导入成功')
    importDialogVisible.value = false
    const res: any = await gradeApi.getGrades(currentBatch.value.id)
    studentGrades.value = res.data || res
    if (!Array.isArray(studentGrades.value)) {
      studentGrades.value = []
    }
  } catch (error) {
    ElMessage.error('导入失败')
  } finally {
    importing.value = false
  }
}

// ---- Helpers ----

const getGradeTypeName = (type: number) => {
  const names: Record<number, string> = { 1: '平时成绩', 2: '期中成绩', 3: '期末成绩', 4: '总评成绩' }
  return names[type] || '未知'
}

const getGradeTypeTag = (type: number) => {
  const types: Record<number, '' | 'success' | 'warning' | 'danger' | 'info'> = {
    1: 'info',
    2: 'warning',
    3: '',
    4: 'success',
  }
  return types[type] || 'info'
}

const getStatusName = (status: number) => {
  const names: Record<number, string> = { 0: '草稿', 1: '已提交', 2: '已审核', 3: '已发布' }
  return names[status] || '未知'
}

const getStatusTag = (status: number) => {
  const types: Record<number, '' | 'success' | 'warning' | 'danger' | 'info'> = {
    0: 'info',
    1: 'warning',
    2: '',
    3: 'success',
  }
  return types[status] || 'info'
}

const getGradeLevelTag = (level: string) => {
  if (level.startsWith('A')) return 'success'
  if (level.startsWith('B')) return ''
  if (level.startsWith('C')) return 'warning'
  if (level.startsWith('D')) return 'danger'
  return 'info'
}

// ---- Init ----

onMounted(async () => {
  await loadSemesters()
  loadBatches()
  loadClassOptions()
})
</script>
