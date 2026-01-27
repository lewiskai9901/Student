<template>
  <div class="teaching-task-view">
    <el-card class="filter-card">
      <el-form :inline="true" :model="queryParams">
        <el-form-item label="学期">
          <el-select v-model="queryParams.semesterId" placeholder="选择学期" clearable>
            <el-option
              v-for="sem in semesters"
              :key="sem.id"
              :value="sem.id"
              :label="sem.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable>
            <el-option :value="0" label="待分配" />
            <el-option :value="1" label="已分配" />
            <el-option :value="2" label="已排课" />
            <el-option :value="3" label="进行中" />
            <el-option :value="4" label="已结束" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>教学任务列表</span>
          <div class="header-actions">
            <el-button @click="showBatchCreateDialog">批量生成</el-button>
            <el-button type="primary" @click="showTaskDialog()">新建任务</el-button>
          </div>
        </div>
      </template>

      <el-table :data="tasks" v-loading="loading" border stripe>
        <el-table-column prop="courseName" label="课程名称" min-width="150" />
        <el-table-column prop="courseCode" label="课程编码" width="100" />
        <el-table-column prop="className" label="班级" width="120" />
        <el-table-column prop="studentCount" label="学生数" width="80" align="center" />
        <el-table-column label="教师" width="150">
          <template #default="{ row }">
            <div v-if="row.teachers?.length">
              <el-tag
                v-for="t in row.teachers"
                :key="t.teacherId"
                :type="t.isMain ? '' : 'info'"
                size="small"
                style="margin-right: 4px"
              >
                {{ t.teacherName }}{{ t.isMain ? '(主)' : '' }}
              </el-tag>
            </div>
            <span v-else class="text-muted">未分配</span>
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
            <el-tag :type="getStatusTag(row.status)">
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

      <el-pagination
        v-model:current-page="queryParams.page"
        v-model:page-size="queryParams.size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 16px; justify-content: flex-end"
        @size-change="loadTasks"
        @current-change="loadTasks"
      />
    </el-card>

    <!-- 任务对话框 -->
    <el-dialog
      v-model="taskDialogVisible"
      :title="taskForm.id ? '编辑教学任务' : '新建教学任务'"
      width="600px"
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
              <el-select v-model="taskForm.classId" placeholder="选择班级" style="width: 100%">
                <el-option v-for="cls in classOptions" :key="cls.id" :value="cls.id" :label="cls.name" />
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
          <el-input v-model="taskForm.remark" type="textarea" rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="taskDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveTask">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分配教师对话框 -->
    <el-dialog v-model="assignDialogVisible" title="分配教师" width="500px">
      <div v-if="currentTask" class="assign-info">
        <p><strong>课程：</strong>{{ currentTask.courseName }}</p>
        <p><strong>班级：</strong>{{ currentTask.className }}</p>
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
              :label="t.name"
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

    <!-- 批量生成对话框 -->
    <el-dialog v-model="batchDialogVisible" title="批量生成教学任务" width="500px">
      <el-form ref="batchFormRef" :model="batchForm" :rules="batchRules" label-width="100px">
        <el-form-item label="学期" prop="semesterId">
          <el-select v-model="batchForm.semesterId" placeholder="选择学期" style="width: 100%">
            <el-option v-for="sem in semesters" :key="sem.id" :value="sem.id" :label="sem.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="培养方案" prop="planId">
          <el-select v-model="batchForm.planId" placeholder="选择培养方案" style="width: 100%">
            <el-option v-for="p in planOptions" :key="p.id" :value="p.id" :label="p.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级" prop="classIds">
          <el-select
            v-model="batchForm.classIds"
            multiple
            placeholder="选择班级(可多选)"
            style="width: 100%"
          >
            <el-option v-for="cls in classOptions" :key="cls.id" :value="cls.id" :label="cls.name" />
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { teachingTaskApi, courseApi, semesterApi } from '@/api/teaching'
import type { TeachingTask, Course, Semester, TeachingTaskQueryParams } from '@/types/teaching'

// 状态
const loading = ref(false)
const saving = ref(false)
const tasks = ref<TeachingTask[]>([])
const total = ref(0)
const semesters = ref<Semester[]>([])
const courseOptions = ref<Course[]>([])
const classOptions = ref<{ id: number; name: string }[]>([])
const teacherOptions = ref<{ id: number; name: string }[]>([])
const planOptions = ref<{ id: number; name: string }[]>([])
const currentTask = ref<TeachingTask>()

// 对话框状态
const taskDialogVisible = ref(false)
const assignDialogVisible = ref(false)
const batchDialogVisible = ref(false)

// 表单
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
const assignForm = ref({ teacherIds: [] as number[], mainTeacherId: undefined as number | undefined })
const batchForm = ref({ semesterId: undefined as number | undefined, planId: undefined as number | undefined, classIds: [] as number[] })

// 验证规则
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

// 方法
const loadTasks = async () => {
  loading.value = true
  try {
    const res = await teachingTaskApi.list(queryParams)
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
    const res = await semesterApi.list()
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
    const res = await courseApi.list({ keyword: query, page: 1, size: 20 })
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

const getTeacherName = (id: number) => {
  return teacherOptions.value.find(t => t.id === id)?.name || ''
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

onMounted(async () => {
  await loadSemesters()
  loadTasks()
  // TODO: 加载班级和教师列表
  classOptions.value = [
    { id: 1, name: '计算机2024-1班' },
    { id: 2, name: '计算机2024-2班' },
    { id: 3, name: '软件2024-1班' },
  ]
  teacherOptions.value = [
    { id: 1, name: '张老师' },
    { id: 2, name: '李老师' },
    { id: 3, name: '王老师' },
  ]
  planOptions.value = [
    { id: 1, name: '计算机科学2024级培养方案v1.0' },
    { id: 2, name: '软件工程2024级培养方案v1.0' },
  ]
})
</script>

<style scoped lang="scss">
.teaching-task-view {
  padding: 20px;
}

.filter-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .header-actions {
    display: flex;
    gap: 8px;
  }
}

.text-muted {
  color: #909399;
  font-size: 12px;
}

.assign-info {
  margin-bottom: 20px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;

  p {
    margin: 4px 0;
  }
}
</style>
