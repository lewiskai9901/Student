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
        <el-option v-for="sem in semesters" :key="sem.id" :value="sem.id" :label="sem.name" />
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
                <button class="rounded px-2 py-1 text-xs font-medium text-blue-600 hover:bg-blue-50" @click="viewArrangements(row)">安排</button>
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

    <!-- Arrangements Drawer -->
    <el-drawer v-model="arrangementsDrawerVisible" title="" size="72%" :with-header="false">
      <div class="flex h-full flex-col">
        <!-- Drawer Header -->
        <div class="flex items-center justify-between border-b border-gray-200 px-6 py-4">
          <div>
            <h2 class="text-lg font-semibold text-gray-900">{{ currentBatch?.name }}</h2>
            <div class="mt-1 flex items-center gap-3 text-sm text-gray-500">
              <span>{{ getExamTypeName(currentBatch?.examType || 0) }}</span>
              <div class="h-3 w-px bg-gray-200" />
              <span>{{ currentBatch?.startDate }} ~ {{ currentBatch?.endDate }}</span>
              <span
                class="inline-flex rounded px-1.5 py-0.5 text-xs font-medium"
                :class="statusBadgeClass(currentBatch?.status || 0)"
              >
                {{ getStatusName(currentBatch?.status || 0) }}
              </span>
            </div>
          </div>
          <div class="flex items-center gap-2">
            <button
              v-if="currentBatch?.status === 0"
              class="inline-flex items-center gap-1.5 rounded-lg bg-blue-600 px-3.5 py-2 text-sm font-medium text-white transition-colors hover:bg-blue-700"
              @click="showArrangementDialog()"
            >
              <Plus class="h-4 w-4" />
              添加考试安排
            </button>
            <button
              class="inline-flex h-8 w-8 items-center justify-center rounded-lg border border-gray-200 text-gray-400 hover:bg-gray-50"
              @click="arrangementsDrawerVisible = false"
            >
              <X class="h-4 w-4" />
            </button>
          </div>
        </div>

        <!-- Drawer Stats -->
        <div class="flex items-center gap-4 border-b border-gray-200 px-6 py-2.5">
          <span class="text-sm text-gray-500">安排数 <span class="font-semibold text-gray-900">{{ arrangements.length }}</span></span>
          <div class="h-3 w-px bg-gray-200" />
          <span class="text-sm text-gray-500">已分配考场 <span class="font-semibold text-gray-900">{{ arrangementsWithRooms }}</span></span>
        </div>

        <!-- Drawer Content -->
        <div class="flex-1 overflow-y-auto px-6 pt-5 pb-6">
          <div class="rounded-xl border border-gray-200 bg-white">
            <el-table :data="arrangements">
              <el-table-column prop="courseName" label="课程" min-width="150" />
              <el-table-column label="班级" width="160">
                <template #default="{ row }">
                  <div class="flex flex-wrap gap-1">
                    <span
                      v-for="name in row.classNames"
                      :key="name"
                      class="inline-flex rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-700"
                    >{{ name }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="examDate" label="考试日期" width="120" />
              <el-table-column label="考试时间" width="140">
                <template #default="{ row }">
                  <span class="text-sm text-gray-600">{{ row.startTime }} - {{ row.endTime }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="duration" label="时长(分钟)" width="100" align="center" />
              <el-table-column label="考场" width="160">
                <template #default="{ row }">
                  <div v-if="row.examRooms?.length" class="space-y-0.5">
                    <div v-for="room in row.examRooms" :key="room.id" class="text-xs text-gray-600">
                      {{ room.classroomName }} ({{ room.actualCount }}/{{ room.capacity }})
                    </div>
                  </div>
                  <span v-else class="text-xs text-gray-400">未分配</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="180">
                <template #default="{ row }">
                  <div class="flex items-center gap-1">
                    <button class="rounded px-2 py-1 text-xs font-medium text-gray-600 hover:bg-gray-100" @click="showArrangementDialog(row)">编辑</button>
                    <button class="rounded px-2 py-1 text-xs font-medium text-blue-600 hover:bg-blue-50" @click="showRoomAssignDialog(row)">分配考场</button>
                    <button
                      v-if="currentBatch?.status === 0"
                      class="rounded px-2 py-1 text-xs font-medium text-red-500 hover:bg-red-50"
                      @click="deleteArrangement(row)"
                    >删除</button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </div>
    </el-drawer>

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
                <el-option v-for="sem in semesters" :key="sem.id" :value="sem.id" :label="sem.name" />
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

    <!-- Arrangement Dialog -->
    <el-dialog
      v-model="arrangementDialogVisible"
      :title="arrangementForm.id ? '编辑考试安排' : '添加考试安排'"
      width="600px"
    >
      <el-form ref="arrangementFormRef" :model="arrangementForm" :rules="arrangementRules" label-width="100px">
        <el-form-item label="课程" prop="courseId">
          <el-select
            v-model="arrangementForm.courseId"
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
        <el-form-item label="班级" prop="classIds">
          <el-select v-model="arrangementForm.classIds" multiple filterable placeholder="可多选" style="width: 100%">
            <el-option v-for="cls in classOptions" :key="cls.id" :value="cls.id" :label="cls.name" />
          </el-select>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="考试日期" prop="examDate">
              <el-date-picker v-model="arrangementForm.examDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="考试时长" prop="duration">
              <el-input-number v-model="arrangementForm.duration" :min="30" :max="300" :step="30" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="开始时间" prop="startTime">
              <el-time-picker v-model="arrangementForm.startTime" format="HH:mm" value-format="HH:mm" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间" prop="endTime">
              <el-time-picker v-model="arrangementForm.endTime" format="HH:mm" value-format="HH:mm" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="arrangementDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveArrangement">保存</el-button>
      </template>
    </el-dialog>

    <!-- Room Assignment Dialog -->
    <el-dialog v-model="roomAssignDialogVisible" title="分配考场" width="700px">
      <div v-if="currentArrangement" class="rounded-lg border border-gray-200 bg-gray-50 px-4 py-3">
        <div class="flex items-center gap-4 text-sm">
          <span class="text-gray-500">课程 <span class="font-medium text-gray-900">{{ currentArrangement.courseName }}</span></span>
          <div class="h-3 w-px bg-gray-200" />
          <span class="text-gray-500">时间 <span class="font-medium text-gray-900">{{ currentArrangement.examDate }} {{ currentArrangement.startTime }}-{{ currentArrangement.endTime }}</span></span>
        </div>
      </div>

      <div class="mt-4">
        <div class="flex items-center justify-between">
          <span class="text-sm font-medium text-gray-700">已分配考场</span>
          <button
            class="inline-flex items-center gap-1 rounded-md border border-gray-200 px-2.5 py-1 text-xs font-medium text-gray-600 hover:bg-gray-50"
            @click="addRoom"
          >
            <Plus class="h-3 w-3" />
            添加考场
          </button>
        </div>

        <el-table :data="assignedRooms" border class="mt-3">
          <el-table-column prop="classroomName" label="教室" />
          <el-table-column prop="capacity" label="容量" width="80" align="center" />
          <el-table-column prop="actualCount" label="实际人数" width="120" align="center">
            <template #default="{ row }">
              <el-input-number v-model="row.actualCount" :min="0" :max="row.capacity" size="small" controls-position="right" />
            </template>
          </el-table-column>
          <el-table-column label="监考教师" min-width="180">
            <template #default="{ row }">
              <el-select v-model="row.invigilatorIds" multiple filterable size="small" placeholder="选择监考" style="width: 100%">
                <el-option v-for="t in teacherOptions" :key="t.id" :value="t.id" :label="t.realName || t.username" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70" align="center">
            <template #default="{ $index }">
              <button class="rounded px-2 py-1 text-xs font-medium text-red-500 hover:bg-red-50" @click="removeRoom($index)">移除</button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button @click="roomAssignDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRoomAssignment">保存</el-button>
      </template>
    </el-dialog>

    <!-- Add Room Dialog -->
    <el-dialog v-model="addRoomDialogVisible" title="选择考场" width="400px">
      <el-form :model="newRoomForm" label-width="80px">
        <el-form-item label="教室">
          <el-select v-model="newRoomForm.classroomId" filterable placeholder="搜索教室" style="width: 100%">
            <el-option
              v-for="c in classroomOptions"
              :key="c.id"
              :value="c.id"
              :label="c.placeName || c.name"
            >
              <div class="flex items-center justify-between">
                <span>{{ c.placeName || c.name }}</span>
                <span class="text-xs text-gray-400">容量: {{ c.capacity || c.attributes?.capacity || '-' }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addRoomDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAddRoom">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Plus, X } from 'lucide-vue-next'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { examApi } from '@/api/teaching'
import { semesterApi } from '@/api/calendar'
import { courseApi } from '@/api/academic'
import { schoolClassApi } from '@/api/organization'
import { http as request } from '@/utils/request'
import type { ExamBatch, ExamArrangement, Semester, Course, ExamBatchQueryParams } from '@/types/teaching'

// State
const loading = ref(false)
const saving = ref(false)
const batches = ref<ExamBatch[]>([])
const arrangements = ref<ExamArrangement[]>([])
const semesters = ref<Semester[]>([])
const courseOptions = ref<Course[]>([])
const total = ref(0)
const currentBatch = ref<ExamBatch>()
const currentArrangement = ref<ExamArrangement>()

// Dialog visibility
const batchDialogVisible = ref(false)
const arrangementsDrawerVisible = ref(false)
const arrangementDialogVisible = ref(false)
const roomAssignDialogVisible = ref(false)
const addRoomDialogVisible = ref(false)

// Forms
const batchFormRef = ref<FormInstance>()
const arrangementFormRef = ref<FormInstance>()
const batchForm = ref<Partial<ExamBatch>>({})
const arrangementForm = ref<Partial<ExamArrangement>>({})
const assignedRooms = ref<any[]>([])
const newRoomForm = ref({ classroomId: undefined as number | undefined })

const queryParams = reactive<ExamBatchQueryParams>({
  semesterId: undefined,
  examType: undefined,
  status: undefined,
  page: 1,
  size: 10,
})

// Dynamic options (fetched from API, NOT hardcoded)
const classOptions = ref<{ id: number | string; name: string }[]>([])
const teacherOptions = ref<{ id: number | string; username: string; realName?: string }[]>([])
const classroomOptions = ref<any[]>([])

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

const arrangementsWithRooms = computed(() => {
  return arrangements.value.filter(a => a.examRooms && a.examRooms.length > 0).length
})

// Validation rules
const batchRules: FormRules = {
  name: [{ required: true, message: '请输入批次名称', trigger: 'blur' }],
  semesterId: [{ required: true, message: '请选择学期', trigger: 'change' }],
  examType: [{ required: true, message: '请选择考试类型', trigger: 'change' }],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  endDate: [{ required: true, message: '请选择结束日期', trigger: 'change' }],
}

const arrangementRules: FormRules = {
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  classIds: [{ required: true, type: 'array', min: 1, message: '请选择班级', trigger: 'change' }],
  examDate: [{ required: true, message: '请选择考试日期', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  duration: [{ required: true, message: '请输入考试时长', trigger: 'blur' }],
}

// Badge style helpers
const examTypeBadgeClass = (type: number) => {
  const map: Record<number, string> = {
    1: 'bg-blue-50 text-blue-700',
    2: 'bg-indigo-50 text-indigo-700',
    3: 'bg-amber-50 text-amber-700',
    4: 'bg-red-50 text-red-700',
  }
  return map[type] || 'bg-gray-100 text-gray-600'
}

const statusBadgeClass = (status: number) => {
  const map: Record<number, string> = {
    0: 'bg-gray-100 text-gray-600',
    1: 'bg-emerald-50 text-emerald-700',
    2: 'bg-amber-50 text-amber-700',
    3: 'bg-red-50 text-red-600',
  }
  return map[status] || 'bg-gray-100 text-gray-600'
}

const getExamTypeName = (type: number) => {
  const names: Record<number, string> = { 1: '期中考试', 2: '期末考试', 3: '补考', 4: '重修考试' }
  return names[type] || '未知'
}

const getStatusName = (status: number) => {
  const names: Record<number, string> = { 0: '草稿', 1: '已发布', 2: '进行中', 3: '已结束' }
  return names[status] || '未知'
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

const loadClassOptions = async () => {
  try {
    const res = await schoolClassApi.getAll()
    const data: any = res
    classOptions.value = (Array.isArray(data) ? data : data.records || []).map((c: any) => ({
      id: c.id,
      name: c.name || c.className,
    }))
  } catch (error) {
    console.error('Failed to load class options:', error)
  }
}

const loadTeacherOptions = async () => {
  try {
    const res = await request.get('/users', { params: { pageSize: 500 } })
    const data = res.data || res
    teacherOptions.value = (Array.isArray(data) ? data : data.records || []).map((t: any) => ({
      id: t.id,
      username: t.username,
      realName: t.realName,
    }))
  } catch (error) {
    console.error('Failed to load teacher options:', error)
  }
}

const loadClassroomOptions = async () => {
  try {
    const res = await request.get('/v9/places', { params: { typeCode: 'CLASSROOM', pageSize: 500 } })
    const data = res.data || res
    classroomOptions.value = Array.isArray(data) ? data : data.records || []
  } catch (error) {
    console.error('Failed to load classroom options:', error)
  }
}

const searchCourses = async (query: string) => {
  if (query.length < 2) return
  try {
    const res: any = await courseApi.list({ keyword: query, pageNum: 1, pageSize: 20 })
    const data = res.data || res
    courseOptions.value = data.records || []
  } catch (error) {
    console.error('Failed to search courses:', error)
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

// Arrangement operations
const viewArrangements = async (batch: ExamBatch) => {
  currentBatch.value = batch
  try {
    const res: any = await examApi.getArrangements(batch.id)
    arrangements.value = res.data || res
  } catch (error) {
    console.error('Failed to load arrangements:', error)
  }
  arrangementsDrawerVisible.value = true
}

const showArrangementDialog = (arrangement?: ExamArrangement) => {
  arrangementForm.value = arrangement
    ? { ...arrangement }
    : { duration: 120 }
  arrangementDialogVisible.value = true
}

const saveArrangement = async () => {
  await arrangementFormRef.value?.validate()
  if (!currentBatch.value) return
  saving.value = true
  try {
    if (arrangementForm.value.id) {
      await examApi.updateArrangement(currentBatch.value.id, arrangementForm.value.id, arrangementForm.value)
    } else {
      await examApi.createArrangement(currentBatch.value.id, arrangementForm.value)
    }
    ElMessage.success('保存成功')
    arrangementDialogVisible.value = false
    const res2: any = await examApi.getArrangements(currentBatch.value.id)
    arrangements.value = res2.data || res2
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const deleteArrangement = async (arrangement: ExamArrangement) => {
  if (!currentBatch.value) return
  await ElMessageBox.confirm('确定删除该考试安排吗？', '警告', { type: 'warning' })
  try {
    await examApi.deleteArrangement(currentBatch.value.id, arrangement.id)
    ElMessage.success('删除成功')
    const res3: any = await examApi.getArrangements(currentBatch.value.id)
    arrangements.value = res3.data || res3
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

// Room assignment operations
const showRoomAssignDialog = (arrangement: ExamArrangement) => {
  currentArrangement.value = arrangement
  assignedRooms.value = (arrangement.examRooms || []).map(room => ({
    ...room,
    invigilatorIds: room.invigilators?.map(i => i.teacherId) || [],
  }))
  roomAssignDialogVisible.value = true
}

const addRoom = () => {
  newRoomForm.value = { classroomId: undefined }
  addRoomDialogVisible.value = true
}

const confirmAddRoom = () => {
  if (!newRoomForm.value.classroomId) {
    ElMessage.warning('请选择教室')
    return
  }
  const classroom = classroomOptions.value.find((c: any) => c.id === newRoomForm.value.classroomId)
  if (!classroom) return
  if (assignedRooms.value.some(r => r.classroomId === classroom.id)) {
    ElMessage.warning('该教室已添加')
    return
  }
  const capacity = classroom.capacity || classroom.attributes?.capacity || 0
  assignedRooms.value.push({
    classroomId: classroom.id,
    classroomName: classroom.placeName || classroom.name,
    capacity,
    actualCount: 0,
    invigilatorIds: [],
  })
  addRoomDialogVisible.value = false
}

const removeRoom = (index: number) => {
  assignedRooms.value.splice(index, 1)
}

const saveRoomAssignment = async () => {
  if (!currentArrangement.value) return
  saving.value = true
  try {
    // Save room assignments
    await examApi.assignRooms(
      currentArrangement.value.id,
      assignedRooms.value.map(r => ({
        classroomId: r.classroomId,
        capacity: r.capacity,
      }))
    )

    // Save invigilator assignments for each room that has invigilators
    for (const room of assignedRooms.value) {
      if (room.id && room.invigilatorIds?.length > 0) {
        const mainTeacherId = room.invigilatorIds[0]
        await examApi.assignInvigilators(room.id, room.invigilatorIds, mainTeacherId)
      }
    }

    ElMessage.success('保存成功')
    roomAssignDialogVisible.value = false
    if (currentBatch.value) {
      const res4: any = await examApi.getArrangements(currentBatch.value.id)
      arrangements.value = res4.data || res4
    }
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

// Init
onMounted(async () => {
  await loadSemesters()
  loadBatches()
  loadClassOptions()
  loadTeacherOptions()
  loadClassroomOptions()
})
</script>
