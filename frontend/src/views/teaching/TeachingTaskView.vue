<template>
  <div class="flex h-full flex-col bg-gray-50">
    <!-- Header -->
    <div class="flex items-center justify-between border-b border-gray-200 bg-white px-6 py-4">
      <div>
        <h1 class="text-lg font-semibold text-gray-900">教学任务</h1>
        <p class="mt-0.5 text-sm text-gray-500">管理课程教学任务分配与教师安排</p>
      </div>
      <div class="flex items-center gap-2">
        <button
          class="inline-flex items-center gap-1.5 rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50"
          @click="showBatchCreateDialog"
        >
          批量生成
        </button>
        <button
          class="inline-flex items-center gap-1.5 rounded-lg bg-blue-600 px-3 py-2 text-sm font-medium text-white transition-colors hover:bg-blue-700"
          @click="showTaskDialog()"
        >
          新建任务
        </button>
      </div>
    </div>

    <!-- Stats Bar -->
    <div class="flex items-center gap-4 border-b border-gray-200 bg-white px-6 py-2.5">
      <span class="text-sm text-gray-500">总数 <span class="font-semibold text-gray-900">{{ total }}</span></span>
      <div class="h-3 w-px bg-gray-200" />
      <span class="text-sm text-gray-500">待分配 <span class="font-semibold text-gray-900">{{ statusCounts[0] }}</span></span>
      <div class="h-3 w-px bg-gray-200" />
      <span class="text-sm text-gray-500">已分配 <span class="font-semibold text-gray-900">{{ statusCounts[1] }}</span></span>
      <div class="h-3 w-px bg-gray-200" />
      <span class="text-sm text-gray-500">已排课 <span class="font-semibold text-gray-900">{{ statusCounts[2] }}</span></span>
      <div class="h-3 w-px bg-gray-200" />
      <span class="text-sm text-gray-500">进行中 <span class="font-semibold text-gray-900">{{ statusCounts[3] }}</span></span>
      <div class="h-3 w-px bg-gray-200" />
      <span class="text-sm text-gray-500">已结束 <span class="font-semibold text-gray-900">{{ statusCounts[4] }}</span></span>
    </div>

    <!-- Content Area -->
    <div class="flex-1 overflow-y-auto px-6 pt-5 pb-6">
      <!-- Filter Bar -->
      <div class="mb-4 rounded-xl border border-gray-200 bg-white px-5 py-3">
        <el-form :inline="true" :model="queryParams" class="flex flex-wrap items-center gap-x-4 gap-y-2">
          <el-form-item label="学期" class="!mb-0">
            <el-select v-model="queryParams.semesterId" placeholder="选择学期" clearable style="width: 180px">
              <el-option
                v-for="sem in semesters"
                :key="sem.id"
                :value="sem.id"
                :label="sem.name"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="状态" class="!mb-0">
            <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 120px">
              <el-option :value="0" label="待分配" />
              <el-option :value="1" label="已分配" />
              <el-option :value="2" label="已排课" />
              <el-option :value="3" label="进行中" />
              <el-option :value="4" label="已结束" />
            </el-select>
          </el-form-item>
          <el-form-item class="!mb-0">
            <el-button type="primary" @click="search">查询</el-button>
            <el-button @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- Table -->
      <div class="rounded-xl border border-gray-200 bg-white">
        <el-table :data="tasks" v-loading="loading" class="w-full">
          <el-table-column prop="courseName" label="课程名称" min-width="150" />
          <el-table-column prop="courseCode" label="课程编码" width="100" />
          <el-table-column prop="className" label="班级" width="120" />
          <el-table-column prop="studentCount" label="学生数" width="80" align="center" />
          <el-table-column label="教师" width="150">
            <template #default="{ row }">
              <div v-if="row.teachers?.length" class="flex flex-wrap gap-1">
                <el-tag
                  v-for="t in row.teachers"
                  :key="t.teacherId"
                  :type="(t.isMain ? '' : 'info') as any"
                  size="small"
                >
                  {{ t.teacherName }}{{ t.isMain ? '(主)' : '' }}
                </el-tag>
              </div>
              <span v-else class="text-xs text-gray-400">未分配</span>
            </template>
          </el-table-column>
          <el-table-column prop="weeklyHours" label="周学时" width="80" align="center" />
          <el-table-column label="教学周" width="100" align="center">
            <template #default="{ row }">
              {{ row.startWeek }}-{{ row.endWeek }}周
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getStatusTag(row.status)" size="small">
                {{ getStatusName(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text @click="showTaskDialog(row)">编辑</el-button>
              <el-button size="small" text type="primary" @click="showAssignDialog(row)">分配教师</el-button>
              <el-button size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- Pagination -->
        <div class="border-t border-gray-100 px-5 py-3">
          <el-pagination
            v-model:current-page="queryParams.page"
            v-model:page-size="queryParams.size"
            :total="total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            class="justify-end"
            @size-change="loadTasks"
            @current-change="loadTasks"
          />
        </div>
      </div>
    </div>

    <!-- Task Dialog -->
    <el-dialog
      v-model="taskDialogVisible"
      :title="taskForm.id ? '编辑教学任务' : '新建教学任务'"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form ref="taskFormRef" :model="taskForm" :rules="taskRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="学期" prop="semesterId">
              <el-select v-model="taskForm.semesterId" placeholder="选择学期" style="width: 100%">
                <el-option v-for="sem in semesters" :key="sem.id" :value="sem.id" :label="sem.name" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程" prop="courseId">
              <el-select
                v-model="taskForm.courseId"
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
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="班级" prop="classId">
              <el-select v-model="taskForm.classId" filterable placeholder="选择班级" style="width: 100%">
                <el-option v-for="cls in classOptions" :key="cls.id" :value="cls.id" :label="cls.className" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学生数" prop="studentCount">
              <el-input-number v-model="taskForm.studentCount" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="周学时" prop="weeklyHours">
              <el-input-number v-model="taskForm.weeklyHours" :min="1" :max="20" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="开始周" prop="startWeek">
              <el-input-number v-model="taskForm.startWeek" :min="1" :max="20" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="结束周" prop="endWeek">
              <el-input-number v-model="taskForm.endWeek" :min="1" :max="20" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="taskForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="taskDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveTask">保存</el-button>
      </template>
    </el-dialog>

    <!-- Assign Teacher Dialog -->
    <el-dialog v-model="assignDialogVisible" title="分配教师" width="500px" :close-on-click-modal="false">
      <div v-if="currentTask" class="mb-4 rounded-lg bg-gray-50 p-3 text-sm text-gray-700">
        <p><span class="font-medium text-gray-900">课程：</span>{{ currentTask.courseName }}</p>
        <p class="mt-1"><span class="font-medium text-gray-900">班级：</span>{{ currentTask.className }}</p>
      </div>
      <el-form ref="assignFormRef" :model="assignForm" :rules="assignRules" label-width="100px">
        <el-form-item label="选择教师" prop="teacherIds">
          <el-select
            v-model="assignForm.teacherIds"
            multiple
            filterable
            placeholder="可多选"
            style="width: 100%"
          >
            <el-option
              v-for="t in teacherOptions"
              :key="t.id"
              :value="t.id"
              :label="t.realName"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="主讲教师" prop="mainTeacherId">
          <el-select
            v-model="assignForm.mainTeacherId"
            placeholder="选择主讲教师"
            style="width: 100%"
          >
            <el-option
              v-for="tid in assignForm.teacherIds"
              :key="tid"
              :value="tid"
              :label="getTeacherName(tid)"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="assignTeachers">确定分配</el-button>
      </template>
    </el-dialog>

    <!-- Batch Create Dialog -->
    <el-dialog v-model="batchDialogVisible" title="批量生成教学任务" width="500px" :close-on-click-modal="false">
      <el-form ref="batchFormRef" :model="batchForm" :rules="batchRules" label-width="100px">
        <el-form-item label="学期" prop="semesterId">
          <el-select v-model="batchForm.semesterId" placeholder="选择学期" style="width: 100%">
            <el-option v-for="sem in semesters" :key="sem.id" :value="sem.id" :label="sem.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="培养方案" prop="planId">
          <el-select v-model="batchForm.planId" filterable placeholder="选择培养方案" style="width: 100%">
            <el-option v-for="p in planOptions" :key="p.id" :value="p.id" :label="p.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级" prop="classIds">
          <el-select
            v-model="batchForm.classIds"
            multiple
            filterable
            placeholder="选择班级(可多选)"
            style="width: 100%"
          >
            <el-option v-for="cls in classOptions" :key="cls.id" :value="cls.id" :label="cls.className" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="batchCreate">生成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { teachingTaskApi, semesterApi } from '@/api/teaching'
import { courseApi, curriculumPlanApi } from '@/api/academic'
import { schoolClassApi } from '@/api/organization'
import { getSimpleUserList } from '@/api/user'
import type { TeachingTask, Course, Semester, TeachingTaskQueryParams, CurriculumPlan } from '@/types/teaching'
import type { SimpleUser } from '@/types/user'

// State
const loading = ref(false)
const saving = ref(false)
const tasks = ref<TeachingTask[]>([])
const total = ref(0)
const semesters = ref<Semester[]>([])
const courseOptions = ref<Course[]>([])
const classOptions = ref<{ id: number | string; className: string }[]>([])
const teacherOptions = ref<SimpleUser[]>([])
const planOptions = ref<CurriculumPlan[]>([])
const currentTask = ref<TeachingTask>()

// Dialog visibility
const taskDialogVisible = ref(false)
const assignDialogVisible = ref(false)
const batchDialogVisible = ref(false)

// Form refs
const taskFormRef = ref<FormInstance>()
const assignFormRef = ref<FormInstance>()
const batchFormRef = ref<FormInstance>()

const queryParams = reactive<TeachingTaskQueryParams>({
  semesterId: undefined,
  courseId: undefined,
  classId: undefined,
  status: undefined,
  page: 1,
  size: 10,
})

const taskForm = ref<Partial<TeachingTask>>({})
const assignForm = ref({ teacherIds: [] as (number | string)[], mainTeacherId: undefined as number | string | undefined })
const batchForm = ref({ semesterId: undefined as number | string | undefined, planId: undefined as number | string | undefined, classIds: [] as (number | string)[] })

// Computed stats
const statusCounts = computed(() => {
  const counts: Record<number, number> = { 0: 0, 1: 0, 2: 0, 3: 0, 4: 0 }
  tasks.value.forEach(t => {
    if (counts[t.status] !== undefined) counts[t.status]++
  })
  return counts
})

// Validation rules
const taskRules: FormRules = {
  semesterId: [{ required: true, message: '请选择学期', trigger: 'change' }],
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  classId: [{ required: true, message: '请选择班级', trigger: 'change' }],
  studentCount: [{ required: true, message: '请输入学生数', trigger: 'blur' }],
  weeklyHours: [{ required: true, message: '请输入周学时', trigger: 'blur' }],
  startWeek: [{ required: true, message: '请输入开始周', trigger: 'blur' }],
  endWeek: [{ required: true, message: '请输入结束周', trigger: 'blur' }],
}

const assignRules: FormRules = {
  teacherIds: [{ required: true, type: 'array', min: 1, message: '请选择教师', trigger: 'change' }],
  mainTeacherId: [{ required: true, message: '请选择主讲教师', trigger: 'change' }],
}

const batchRules: FormRules = {
  semesterId: [{ required: true, message: '请选择学期', trigger: 'change' }],
  planId: [{ required: true, message: '请选择培养方案', trigger: 'change' }],
  classIds: [{ required: true, type: 'array', min: 1, message: '请选择班级', trigger: 'change' }],
}

// Data loading
const loadTasks = async () => {
  loading.value = true
  try {
    const res: any = await teachingTaskApi.list(queryParams)
    const data = res.data || res
    tasks.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    console.error('Failed to load tasks:', error)
  } finally {
    loading.value = false
  }
}

const loadSemesters = async () => {
  try {
    const res: any = await semesterApi.list()
    semesters.value = res.data || res || []
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

const loadClasses = async () => {
  try {
    const list = await schoolClassApi.getClassList()
    classOptions.value = (list || []).map((c: any) => ({
      id: c.id,
      className: c.className || c.name || c.unitName || '',
    }))
  } catch (error) {
    console.error('Failed to load classes:', error)
  }
}

const loadTeachers = async () => {
  try {
    const list = await getSimpleUserList()
    teacherOptions.value = list || []
  } catch (error) {
    console.error('Failed to load teachers:', error)
  }
}

const loadPlans = async () => {
  try {
    const res: any = await curriculumPlanApi.list({ page: 1, size: 500 })
    const data = res.data || res
    planOptions.value = data.records || data || []
  } catch (error) {
    console.error('Failed to load plans:', error)
  }
}

// Actions
const search = () => {
  queryParams.page = 1
  loadTasks()
}

const resetQuery = () => {
  queryParams.courseId = undefined
  queryParams.classId = undefined
  queryParams.status = undefined
  queryParams.page = 1
  loadTasks()
}

const searchCourses = async (query: string) => {
  if (query.length < 2) return
  try {
    const res: any = await courseApi.list({ keyword: query, page: 1, size: 20 })
    const data = res.data || res
    courseOptions.value = data.records || []
  } catch (error) {
    console.error('Failed to search courses:', error)
  }
}

const showTaskDialog = (task?: TeachingTask) => {
  taskForm.value = task
    ? { ...task }
    : { semesterId: queryParams.semesterId, weeklyHours: 4, startWeek: 1, endWeek: 16 }
  taskDialogVisible.value = true
}

const saveTask = async () => {
  await taskFormRef.value?.validate()
  saving.value = true
  try {
    if (taskForm.value.id) {
      await teachingTaskApi.update(taskForm.value.id, taskForm.value)
    } else {
      await teachingTaskApi.create(taskForm.value)
    }
    ElMessage.success('保存成功')
    taskDialogVisible.value = false
    loadTasks()
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const showAssignDialog = (task: TeachingTask) => {
  currentTask.value = task
  assignForm.value = {
    teacherIds: task.teachers?.map(t => t.teacherId) || [],
    mainTeacherId: task.teachers?.find(t => t.isMain)?.teacherId,
  }
  assignDialogVisible.value = true
}

const assignTeachers = async () => {
  await assignFormRef.value?.validate()
  if (!currentTask.value || !assignForm.value.mainTeacherId) return
  saving.value = true
  try {
    await teachingTaskApi.assignTeachers(
      currentTask.value.id,
      assignForm.value.teacherIds,
      assignForm.value.mainTeacherId
    )
    ElMessage.success('分配成功')
    assignDialogVisible.value = false
    loadTasks()
  } catch (error) {
    ElMessage.error('分配失败')
  } finally {
    saving.value = false
  }
}

const getTeacherName = (id: number | string) => {
  return teacherOptions.value.find(t => t.id === id)?.realName || ''
}

const showBatchCreateDialog = () => {
  batchForm.value = { semesterId: queryParams.semesterId, planId: undefined, classIds: [] }
  batchDialogVisible.value = true
}

const batchCreate = async () => {
  await batchFormRef.value?.validate()
  if (!batchForm.value.semesterId || !batchForm.value.planId) return
  saving.value = true
  try {
    await teachingTaskApi.batchCreate(
      batchForm.value.semesterId,
      batchForm.value.planId,
      batchForm.value.classIds
    )
    ElMessage.success('生成成功')
    batchDialogVisible.value = false
    loadTasks()
  } catch (error) {
    ElMessage.error('生成失败')
  } finally {
    saving.value = false
  }
}

const handleDelete = async (task: TeachingTask) => {
  await ElMessageBox.confirm('确定删除该教学任务吗？', '警告', { type: 'warning' })
  try {
    await teachingTaskApi.delete(task.id)
    ElMessage.success('删除成功')
    loadTasks()
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const getStatusName = (status: number) => {
  const names: Record<number, string> = {
    0: '待分配',
    1: '已分配',
    2: '已排课',
    3: '进行中',
    4: '已结束',
  }
  return names[status] || '未知'
}

const getStatusTag = (status: number) => {
  const types: Record<number, '' | 'success' | 'warning' | 'danger' | 'info'> = {
    0: 'info',
    1: 'warning',
    2: '',
    3: 'success',
    4: 'danger',
  }
  return types[status] || 'info'
}

// Init
onMounted(async () => {
  await loadSemesters()
  loadTasks()
  loadClasses()
  loadTeachers()
  loadPlans()
})
</script>
