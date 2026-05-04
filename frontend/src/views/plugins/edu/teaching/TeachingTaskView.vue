<template>
  <div class="tm-page" style="flex-direction: row;">
    <DeptTree @select="onTreeSelect" />
    <div style="flex: 1; display: flex; flex-direction: column; overflow: hidden;">
    <!-- Header -->
    <div class="tm-header">
      <div>
        <h1 class="tm-title">教学任务</h1>
        <div class="tm-stats">
          <span>总数 <b>{{ total }}</b></span>
          <span class="sep" />
          <span><span class="dot dot-gray" />待落实 <b>{{ statusCounts[0] }}</b></span>
          <span class="sep" />
          <span><span class="dot dot-blue" />已分配教师 <b>{{ statusCounts[1] }}</b></span>
          <span class="sep" />
          <span><span class="dot dot-green" />已排课 <b>{{ statusCounts[2] }}</b></span>
          <span class="sep" />
          <span>进行中 <b>{{ statusCounts[3] }}</b></span>
          <span class="sep" />
          <span>已结束 <b>{{ statusCounts[4] }}</b></span>
        </div>
      </div>
      <div style="display: flex; align-items: center; gap: 8px;">
        <button class="tm-btn tm-btn-workflow" @click="handleGenerateFromOfferings">从开课计划生成</button>
        <button class="tm-btn tm-btn-secondary" @click="showBatchCreateDialog">批量生成</button>
        <button class="tm-btn tm-btn-primary" @click="showTaskDialog()">新建任务</button>
      </div>
    </div>

    <!-- pipeline removed: navigation via sidebar + contextual buttons -->

    <!-- Filter Bar -->
    <div class="tm-filters">
      <select v-model="queryParams.semesterId" class="tm-select">
        <option :value="undefined" disabled>选择学期</option>
        <option v-for="sem in semesters" :key="sem.id" :value="sem.id">{{ sem.semesterName }}</option>
      </select>
      <select v-model="queryParams.status" class="tm-select">
        <option :value="undefined">全部状态</option>
        <option :value="0">待落实</option>
        <option :value="1">已分配教师</option>
        <option :value="2">已排课</option>
        <option :value="3">进行中</option>
        <option :value="4">已结束</option>
        <option :value="9">已取消</option>
      </select>
      <button class="tm-btn tm-btn-primary" style="padding: 7px 16px;" @click="search">查询</button>
      <button class="tm-btn-reset" @click="resetQuery">重置</button>
    </div>

    <!-- Table -->
    <div class="tm-table-wrap">
      <table class="tm-table">
        <colgroup>
          <col style="width: 14%;" />
          <col style="width: 7%;" />
          <col style="width: 9%;" />
          <col style="width: 5%;" />
          <col style="width: 13%;" />
          <col style="width: 5%;" />
          <col style="width: 7%;" />
          <col style="width: 8%;" />
          <col style="width: 5%;" />
          <col style="width: 5%;" />
          <col style="width: 7%;" />
          <col style="width: 15%;" />
        </colgroup>
        <thead>
          <tr>
            <th class="text-left">课程名称</th>
            <th>编码</th>
            <th>班级</th>
            <th>人数</th>
            <th>教师</th>
            <th>周学时</th>
            <th>教学周</th>
            <th>教室需求</th>
            <th>连排</th>
            <th>性质</th>
            <th>考核</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="13" class="tm-empty">
              <span class="tm-spin" style="display: inline-block; width: 16px; height: 16px; border: 2px solid #e5e7eb; border-top-color: #2563eb; border-radius: 50;" />
              加载中...
            </td>
          </tr>
          <tr v-else-if="tasks.length === 0">
            <td colspan="13" class="tm-empty">暂无教学任务数据</td>
          </tr>
          <tr v-for="row in tasks" :key="row.id">
            <td class="text-left">{{ row.courseName }}</td>
            <td><span class="tm-code">{{ row.courseCode }}</span></td>
            <td>{{ row.className }}</td>
            <td class="tm-mono">{{ row.studentCount }}</td>
            <td>
              <template v-if="row.teachers?.length">
                <span
                  v-for="t in row.teachers"
                  :key="t.teacher_id || t.teacherId"
                  :class="['tm-chip', (t.teacher_role || t.role) === 1 ? 'tm-chip-blue' : 'tm-chip-gray']"
                  style="margin: 1px 2px;"
                >
                  {{ t.real_name || t.teacherName }}{{ (t.teacher_role || t.role) === 1 ? '(主)' : '' }}
                </span>
              </template>
              <span v-else-if="row.teacherName" class="tm-chip tm-chip-blue" style="margin: 1px 2px;">{{ row.teacherName }}</span>
              <span v-else style="color: #9ca3af; font-size: 12px;">未分配</span>
            </td>
            <td class="tm-mono">{{ row.weeklyHours }}</td>
            <td class="tm-mono">{{ row.startWeek }}-{{ row.endWeek }}周</td>
            <td>{{ row.roomTypeName || (row.roomTypeRequired ? row.roomTypeRequired : '不限') }}</td>
            <td class="tm-mono">{{ getConsecutiveLabel(row.consecutivePeriods) }}</td>
            <td>{{ getCourseNatureLabel(row.courseNature) }}</td>
            <td>{{ getAssessmentLabel(row.assessmentMethod) }}</td>
            <td>
              <span :class="['tm-chip', statusChipClass(row.taskStatus)]">
                {{ getStatusName(row.taskStatus) }}
              </span>
            </td>
            <td>
              <button class="tm-action" @click="showTaskDialog(row)">编辑</button>
              <button class="tm-action" @click="showAssignDialog(row)">分配</button>
              <button class="tm-action tm-action-danger" @click="handleDelete(row)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div class="tm-pagination">
      <span class="tm-page-info">共 {{ total }} 条</span>
      <div class="tm-page-controls">
        <select
          class="tm-page-select"
          :value="queryParams.size ?? 10"
          @change="queryParams.size = Number(($event.target as HTMLSelectElement).value); queryParams.page = 1; loadTasks()"
        >
          <option :value="10">10 条/页</option>
          <option :value="20">20 条/页</option>
          <option :value="50">50 条/页</option>
          <option :value="100">100 条/页</option>
        </select>
        <button
          class="tm-page-btn"
          :disabled="(queryParams.page ?? 1) <= 1"
          @click="queryParams.page = (queryParams.page ?? 1) - 1; loadTasks()"
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
        </button>
        <span class="tm-page-current">{{ queryParams.page ?? 1 }}</span>
        <button
          class="tm-page-btn"
          :disabled="(queryParams.page ?? 1) >= Math.ceil(total / (queryParams.size ?? 10))"
          @click="queryParams.page = (queryParams.page ?? 1) + 1; loadTasks()"
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>
        </button>
      </div>
    </div>

    <!-- Task Drawer -->
    <Teleport to="body">
      <Transition name="tm-drawer">
        <div v-if="taskDialogVisible" class="tm-drawer-overlay" @click.self="taskDialogVisible = false">
          <div class="tm-drawer">
            <div class="tm-drawer-header">
              <h2 class="tm-drawer-title">{{ taskForm.id ? '编辑教学任务' : '新建教学任务' }}</h2>
              <button class="tm-drawer-close" @click="taskDialogVisible = false">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
            <div class="tm-drawer-body">
              <div class="tm-section">
                <h3 class="tm-section-title">基本信息</h3>
                <div class="tm-fields tm-cols-2">
                  <div class="tm-field">
                    <label class="tm-label">学期 <span class="req">*</span></label>
                    <select v-model="taskForm.semesterId" class="tm-field-select">
                      <option :value="undefined" disabled>选择学期</option>
                      <option v-for="sem in semesters" :key="sem.id" :value="sem.id">{{ sem.semesterName }}</option>
                    </select>
                  </div>
                  <div class="tm-field">
                    <label class="tm-label">课程 <span class="req">*</span></label>
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
                        :label="`${c.courseCode} - ${c.courseName}`"
                      />
                    </el-select>
                  </div>
                </div>
                <div class="tm-fields tm-cols-2">
                  <div class="tm-field">
                    <label class="tm-label">班级 <span class="req">*</span></label>
                    <el-select v-model="taskForm.orgUnitId" filterable placeholder="选择班级" style="width: 100%">
                      <el-option v-for="cls in classOptions" :key="cls.id" :value="cls.id" :label="cls.className" />
                    </el-select>
                  </div>
                  <div class="tm-field">
                    <label class="tm-label">学生数 <span class="req">*</span></label>
                    <input v-model.number="taskForm.studentCount" type="number" min="1" class="tm-input" placeholder="输入学生数" />
                  </div>
                </div>
              </div>
              <div class="tm-section">
                <h3 class="tm-section-title">教学安排</h3>
                <div class="tm-fields tm-cols-3">
                  <div class="tm-field">
                    <label class="tm-label">周学时 <span class="req">*</span></label>
                    <input v-model.number="taskForm.weeklyHours" type="number" min="1" max="20" class="tm-input" placeholder="周学时" />
                  </div>
                  <div class="tm-field">
                    <label class="tm-label">开始周 <span class="req">*</span></label>
                    <input v-model.number="taskForm.startWeek" type="number" min="1" max="20" class="tm-input" placeholder="开始周" />
                  </div>
                  <div class="tm-field">
                    <label class="tm-label">结束周 <span class="req">*</span></label>
                    <input v-model.number="taskForm.endWeek" type="number" min="1" max="20" class="tm-input" placeholder="结束周" />
                  </div>
                </div>
              </div>
              <div class="tm-section">
                <h3 class="tm-section-title">备注</h3>
                <textarea v-model="taskForm.remark" class="tm-textarea" rows="2" placeholder="选填备注信息"></textarea>
              </div>
            </div>
            <div class="tm-drawer-footer">
              <button class="tm-btn tm-btn-secondary" @click="taskDialogVisible = false">取消</button>
              <button class="tm-btn tm-btn-primary" :disabled="saving" @click="saveTask">
                <span v-if="saving" class="tm-spin" style="display: inline-block; width: 14px; height: 14px; border: 2px solid rgba(255,255,255,0.3); border-top-color: #fff; border-radius: 50%;" />
                保存
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- Assign Teacher Drawer -->
    <Teleport to="body">
      <Transition name="tm-drawer">
        <div v-if="assignDialogVisible" class="tm-drawer-overlay" @click.self="assignDialogVisible = false">
          <div class="tm-drawer" style="width: 480px;">
            <div class="tm-drawer-header">
              <h2 class="tm-drawer-title">分配教师</h2>
              <button class="tm-drawer-close" @click="assignDialogVisible = false">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
            <div class="tm-drawer-body">
              <div v-if="currentTask" class="tm-section" style="background: #f9fafb;">
                <div style="font-size: 13px; color: #374151;">
                  <p><span style="font-weight: 600; color: #111827;">课程：</span>{{ currentTask.courseName }}</p>
                  <p style="margin-top: 4px;"><span style="font-weight: 600; color: #111827;">班级：</span>{{ currentTask.className }}</p>
                </div>
              </div>
              <div class="tm-section">
                <h3 class="tm-section-title">教师选择</h3>
                <div class="tm-field">
                  <label class="tm-label">选择教师 <span class="req">*</span></label>
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
                </div>
                <div class="tm-field" style="margin-top: 12px;">
                  <label class="tm-label">主讲教师 <span class="req">*</span></label>
                  <select v-model="assignForm.mainTeacherId" class="tm-field-select">
                    <option :value="undefined" disabled>选择主讲教师</option>
                    <option v-for="tid in assignForm.teacherIds" :key="tid" :value="tid">{{ getTeacherName(tid) }}</option>
                  </select>
                </div>
              </div>
            </div>
            <div class="tm-drawer-footer">
              <button class="tm-btn tm-btn-secondary" @click="assignDialogVisible = false">取消</button>
              <button class="tm-btn tm-btn-primary" :disabled="saving" @click="assignTeachers">
                <span v-if="saving" class="tm-spin" style="display: inline-block; width: 14px; height: 14px; border: 2px solid rgba(255,255,255,0.3); border-top-color: #fff; border-radius: 50%;" />
                确定分配
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- Batch Create Drawer -->
    <Teleport to="body">
      <Transition name="tm-drawer">
        <div v-if="batchDialogVisible" class="tm-drawer-overlay" @click.self="batchDialogVisible = false">
          <div class="tm-drawer" style="width: 480px;">
            <div class="tm-drawer-header">
              <h2 class="tm-drawer-title">批量生成教学任务</h2>
              <button class="tm-drawer-close" @click="batchDialogVisible = false">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
            <div class="tm-drawer-body">
              <div class="tm-section">
                <h3 class="tm-section-title">生成配置</h3>
                <div class="tm-field">
                  <label class="tm-label">学期 <span class="req">*</span></label>
                  <select v-model="batchForm.semesterId" class="tm-field-select">
                    <option :value="undefined" disabled>选择学期</option>
                    <option v-for="sem in semesters" :key="sem.id" :value="sem.id">{{ sem.semesterName }}</option>
                  </select>
                </div>
                <div class="tm-field" style="margin-top: 12px;">
                  <label class="tm-label">培养方案 <span class="req">*</span></label>
                  <el-select v-model="batchForm.planId" filterable placeholder="选择培养方案" style="width: 100%">
                    <el-option v-for="p in planOptions" :key="p.id" :value="p.id" :label="p.planName" />
                  </el-select>
                </div>
                <div class="tm-field" style="margin-top: 12px;">
                  <label class="tm-label">班级 <span class="req">*</span></label>
                  <el-select
                    v-model="batchForm.orgUnitIds"
                    multiple
                    filterable
                    placeholder="选择班级(可多选)"
                    style="width: 100%"
                  >
                    <el-option v-for="cls in classOptions" :key="cls.id" :value="cls.id" :label="cls.className" />
                  </el-select>
                </div>
              </div>
            </div>
            <div class="tm-drawer-footer">
              <button class="tm-btn tm-btn-secondary" @click="batchDialogVisible = false">取消</button>
              <button class="tm-btn tm-btn-primary" :disabled="saving" @click="batchCreate">
                <span v-if="saving" class="tm-spin" style="display: inline-block; width: 14px; height: 14px; border: 2px solid rgba(255,255,255,0.3); border-top-color: #fff; border-radius: 50%;" />
                生成
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div><!-- end wrapper -->
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import DeptTree from '@/components/teaching/DeptTree.vue'
import { teachingTaskApi, workflowApi } from '@/api/teaching'
import { semesterApi } from '@/api/calendar'
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
const selectedTreeOrg = ref<{ type: string; id: number | string; name: string }>({ type: '', id: '', name: '' })
function onTreeSelect(node: { type: string; id: number | string; name: string }) {
  selectedTreeOrg.value = node
}

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
const batchFormRef = ref<FormInstance>()

const queryParams = reactive<TeachingTaskQueryParams>({
  semesterId: undefined,
  courseId: undefined,
  orgUnitId: undefined,
  status: undefined,
  page: 1,
  size: 10,
})

const taskForm = ref<Partial<TeachingTask>>({})
const assignForm = ref({ teacherIds: [] as (number | string)[], mainTeacherId: undefined as number | string | undefined })
const batchForm = ref({ semesterId: undefined as number | string | undefined, planId: undefined as number | string | undefined, orgUnitIds: [] as (number | string)[] })

// Computed stats
const statusCounts = computed(() => {
  const counts: Record<number, number> = { 0: 0, 1: 0, 2: 0, 3: 0, 4: 0, 9: 0 }
  tasks.value.forEach(t => {
    const s = t.taskStatus ?? 0
    if (counts[s] !== undefined) counts[s]++
  })
  return counts
})

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
    const res: any = await curriculumPlanApi.list({ pageNum: 1, pageSize: 500 })
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
  queryParams.orgUnitId = undefined
  queryParams.status = undefined
  queryParams.page = 1
  loadTasks()
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
    teacherIds: task.teachers?.map(t => t.teacher_id || t.teacherId) || [],
    mainTeacherId: task.teachers?.find(t => (t.teacher_role || t.role) === 1)?.teacher_id
      || task.teachers?.find(t => (t.teacher_role || t.role) === 1)?.teacherId,
  }
  assignDialogVisible.value = true
}

const assignTeachers = async () => {
  if (!currentTask.value || !assignForm.value.mainTeacherId) return
  saving.value = true
  try {
    const teachers = assignForm.value.teacherIds.map(tid => ({
      teacherId: tid,
      role: String(tid) === String(assignForm.value.mainTeacherId) ? 1 : 2,
    }))
    await teachingTaskApi.assignTeachers(currentTask.value.id, teachers)
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

const handleGenerateFromOfferings = async () => {
  if (!queryParams.semesterId) {
    ElMessage.warning('请先选择学期')
    return
  }
  try {
    await ElMessageBox.confirm(
      '将根据已确认的班级分配(开课计划)批量生成教学任务。已存在的任务不会重复创建。',
      '从开课计划生成',
      { type: 'info' }
    )
    const res = await workflowApi.generateTasks(queryParams.semesterId)
    ElMessage.success(`成功生成 ${res.generated} 条教学任务`)
    loadTasks()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '生成失败')
  }
}

const showBatchCreateDialog = () => {
  batchForm.value = { semesterId: queryParams.semesterId, planId: undefined, orgUnitIds: [] }
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
      batchForm.value.orgUnitIds
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
    0: '待落实',
    1: '已分配教师',
    2: '已排课',
    3: '进行中',
    4: '已结束',
    9: '已取消',
  }
  return names[status] ?? '未知'
}

const statusChipClass = (status: number) => {
  const map: Record<number, string> = {
    0: 'tm-chip-gray',
    1: 'tm-chip-blue',
    2: 'tm-chip-green',
    3: 'tm-chip-amber',
    4: 'tm-chip-red',
    9: 'tm-chip-red',
  }
  return map[status] ?? 'tm-chip-gray'
}

const getConsecutiveLabel = (val?: number) => {
  const map: Record<number, string> = { 1: '不连排', 2: '2节连', 3: '3节连', 4: '4节连' }
  return map[val ?? 2] ?? `${val}节`
}

const getAssessmentLabel = (val?: number) => {
  const map: Record<number, string> = { 1: '考试', 2: '考查', 3: '技能考试', 4: '考试+考查' }
  return map[val ?? 1] ?? '考试'
}

const getCourseNatureLabel = (val?: number) => {
  const map: Record<number, string> = { 1: '理论', 2: '实验', 3: '实践', 4: '理论+实验' }
  return map[val ?? 1] ?? '未知'
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

<style>
@import '@/styles/teaching-ui.css';
</style>

<style scoped>
/* Status chip helper used in template */
</style>
