<template>
  <div class="schedule-view">
    <el-card class="filter-card">
      <el-form :inline="true" :model="queryParams">
        <el-form-item label="学期">
          <el-select v-model="queryParams.semesterId" placeholder="选择学期" @change="loadSchedules">
            <el-option v-for="sem in semesters" :key="sem.id" :value="sem.id" :label="sem.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="查看方式">
          <el-radio-group v-model="viewMode">
            <el-radio-button value="list">列表</el-radio-button>
            <el-radio-button value="timetable">课表</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="viewMode === 'timetable'" label="查看对象">
          <el-select v-model="timetableType" style="width: 100px">
            <el-option value="class" label="班级" />
            <el-option value="teacher" label="教师" />
            <el-option value="classroom" label="教室" />
          </el-select>
          <el-select v-model="timetableTargetId" placeholder="选择" style="width: 150px; margin-left: 8px">
            <el-option
              v-for="opt in timetableOptions"
              :key="opt.id"
              :value="opt.id"
              :label="opt.name"
            />
          </el-select>
          <el-button type="primary" style="margin-left: 8px" @click="loadTimetable">查看课表</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表模式 -->
    <el-card v-if="viewMode === 'list'" class="table-card">
      <template #header>
        <div class="card-header">
          <span>排课方案列表</span>
          <el-button type="primary" @click="showScheduleDialog()">新建排课</el-button>
        </div>
      </template>

      <el-table :data="schedules" v-loading="loading" border stripe>
        <el-table-column prop="name" label="方案名称" min-width="200" />
        <el-table-column prop="semesterName" label="学期" width="180" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getScheduleStatusTag(row.status)">
              {{ getScheduleStatusName(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="generatedAt" label="生成时间" width="160" />
        <el-table-column prop="publishedAt" label="发布时间" width="160" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text @click="viewScheduleEntries(row)">查看</el-button>
            <el-button size="small" text @click="showScheduleDialog(row)">编辑</el-button>
            <el-button
              v-if="row.status === 0"
              size="small"
              text
              type="warning"
              @click="autoSchedule(row)"
            >智能排课</el-button>
            <el-button
              v-if="row.status === 0"
              size="small"
              text
              type="success"
              @click="publishSchedule(row)"
            >发布</el-button>
            <el-button
              v-if="row.status === 0"
              size="small"
              text
              type="danger"
              @click="deleteSchedule(row)"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 课表模式 -->
    <el-card v-else class="timetable-card">
      <template #header>
        <div class="card-header">
          <span>{{ timetableTitle }}</span>
          <div>
            <el-button size="small" @click="exportTimetable">导出课表</el-button>
          </div>
        </div>
      </template>

      <div class="timetable-container">
        <table class="timetable">
          <thead>
            <tr>
              <th class="period-col">节次</th>
              <th v-for="day in weekdays" :key="day.value">{{ day.label }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="period in periods" :key="period.period">
              <td class="period-col">
                <div class="period-info">
                  <span class="period-name">{{ period.name }}</span>
                  <span class="period-time">{{ period.startTime }}-{{ period.endTime }}</span>
                </div>
              </td>
              <td v-for="day in weekdays" :key="day.value" class="schedule-cell">
                <div
                  v-for="entry in getEntriesForCell(day.value, period.period)"
                  :key="entry.id"
                  class="schedule-entry"
                  :class="getEntryClass(entry)"
                  @click="showEntryDetail(entry)"
                >
                  <div class="entry-course">{{ entry.courseName }}</div>
                  <div class="entry-info">{{ entry.classroomName }}</div>
                  <div class="entry-info">{{ entry.teacherName }}</div>
                  <div class="entry-weeks">{{ formatWeeks(entry) }}</div>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </el-card>

    <!-- 排课方案对话框 -->
    <el-dialog
      v-model="scheduleDialogVisible"
      :title="scheduleForm.id ? '编辑排课方案' : '新建排课方案'"
      width="500px"
    >
      <el-form ref="scheduleFormRef" :model="scheduleForm" :rules="scheduleRules" label-width="100px">
        <el-form-item label="方案名称" prop="name">
          <el-input v-model="scheduleForm.name" placeholder="如：2025-2026学年第一学期课表" />
        </el-form-item>
        <el-form-item label="学期" prop="semesterId">
          <el-select v-model="scheduleForm.semesterId" style="width: 100%">
            <el-option v-for="sem in semesters" :key="sem.id" :value="sem.id" :label="sem.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="scheduleForm.remark" type="textarea" rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="scheduleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveSchedule">保存</el-button>
      </template>
    </el-dialog>

    <!-- 智能排课对话框 -->
    <el-dialog v-model="autoScheduleDialogVisible" title="智能排课" width="500px">
      <el-alert type="info" :closable="false" style="margin-bottom: 20px">
        智能排课将根据教学任务自动生成课表，您可以调整以下参数优化排课效果。
      </el-alert>
      <el-form :model="autoScheduleParams" label-width="120px">
        <el-form-item label="最大迭代次数">
          <el-input-number v-model="autoScheduleParams.maxIterations" :min="100" :max="5000" :step="100" />
        </el-form-item>
        <el-form-item label="种群大小">
          <el-input-number v-model="autoScheduleParams.populationSize" :min="20" :max="500" :step="10" />
        </el-form-item>
        <el-form-item label="变异率">
          <el-slider v-model="autoScheduleParams.mutationRate" :min="0.01" :max="0.5" :step="0.01" show-input />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="autoScheduleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="scheduling" @click="runAutoSchedule">开始排课</el-button>
      </template>
    </el-dialog>

    <!-- 排课条目详情 -->
    <el-drawer v-model="entriesDrawerVisible" title="排课详情" size="60%">
      <div class="entries-header">
        <el-button type="primary" size="small" @click="showEntryDialog()">手动添加</el-button>
        <el-button size="small" @click="checkConflicts">检测冲突</el-button>
      </div>

      <el-table :data="scheduleEntries" border>
        <el-table-column prop="courseName" label="课程" width="150" />
        <el-table-column prop="className" label="班级" width="120" />
        <el-table-column prop="teacherName" label="教师" width="100" />
        <el-table-column prop="classroomName" label="教室" width="100" />
        <el-table-column label="时间" width="150">
          <template #default="{ row }">
            {{ getWeekdayName(row.dayOfWeek) }} 第{{ row.periodStart }}-{{ row.periodEnd }}节
          </template>
        </el-table-column>
        <el-table-column label="周次" width="120">
          <template #default="{ row }">
            {{ formatWeeks(row) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" text @click="showEntryDialog(row)">编辑</el-button>
            <el-button size="small" text type="danger" @click="deleteEntry(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>

    <!-- 添加/编辑条目对话框 -->
    <el-dialog
      v-model="entryDialogVisible"
      :title="entryForm.id ? '编辑排课' : '添加排课'"
      width="600px"
    >
      <el-form ref="entryFormRef" :model="entryForm" :rules="entryRules" label-width="100px">
        <el-form-item label="教学任务" prop="taskId">
          <el-select v-model="entryForm.taskId" placeholder="选择教学任务" style="width: 100%">
            <el-option
              v-for="t in availableTasks"
              :key="t.id"
              :value="t.id"
              :label="`${t.courseName} - ${t.className}`"
            />
          </el-select>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="星期" prop="dayOfWeek">
              <el-select v-model="entryForm.dayOfWeek" style="width: 100%">
                <el-option v-for="d in weekdays" :key="d.value" :value="d.value" :label="d.label" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="教室" prop="classroomId">
              <el-select v-model="entryForm.classroomId" placeholder="选择教室" style="width: 100%">
                <el-option v-for="c in classrooms" :key="c.id" :value="c.id" :label="c.name" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="开始节次" prop="periodStart">
              <el-select v-model="entryForm.periodStart" style="width: 100%">
                <el-option v-for="p in periods" :key="p.period" :value="p.period" :label="p.name" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束节次" prop="periodEnd">
              <el-select v-model="entryForm.periodEnd" style="width: 100%">
                <el-option v-for="p in periods" :key="p.period" :value="p.period" :label="p.name" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="开始周" prop="weekStart">
              <el-input-number v-model="entryForm.weekStart" :min="1" :max="20" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="结束周" prop="weekEnd">
              <el-input-number v-model="entryForm.weekEnd" :min="1" :max="20" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="周类型">
              <el-select v-model="entryForm.weekType" style="width: 100%">
                <el-option :value="0" label="每周" />
                <el-option :value="1" label="单周" />
                <el-option :value="2" label="双周" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="entryDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEntry">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { scheduleApi, semesterApi } from '@/api/teaching'
import type { CourseSchedule, ScheduleEntry, Semester } from '@/types/teaching'
import { WEEKDAYS, DEFAULT_PERIODS } from '@/types/teaching'

// 状态
const loading = ref(false)
const saving = ref(false)
const scheduling = ref(false)
const schedules = ref<CourseSchedule[]>([])
const scheduleEntries = ref<ScheduleEntry[]>([])
const timetableEntries = ref<ScheduleEntry[]>([])
const semesters = ref<Semester[]>([])
const currentSchedule = ref<CourseSchedule>()

// 视图状态
const viewMode = ref<'list' | 'timetable'>('list')
const timetableType = ref<'class' | 'teacher' | 'classroom'>('class')
const timetableTargetId = ref<number>()

// 常量
const weekdays = WEEKDAYS
const periods = DEFAULT_PERIODS

// 对话框状态
const scheduleDialogVisible = ref(false)
const autoScheduleDialogVisible = ref(false)
const entriesDrawerVisible = ref(false)
const entryDialogVisible = ref(false)

// 表单
const scheduleFormRef = ref<FormInstance>()
const entryFormRef = ref<FormInstance>()
const scheduleForm = ref<Partial<CourseSchedule>>({})
const entryForm = ref<Partial<ScheduleEntry>>({})
const autoScheduleParams = ref({ maxIterations: 1000, populationSize: 100, mutationRate: 0.1 })

const queryParams = reactive({ semesterId: undefined as number | undefined })

// 选项数据
const classrooms = ref<{ id: number; name: string }[]>([])
const availableTasks = ref<{ id: number; courseName: string; className: string }[]>([])

// 验证规则
const scheduleRules: FormRules = {
  name: [{ required: true, message: '请输入方案名称', trigger: 'blur' }],
  semesterId: [{ required: true, message: '请选择学期', trigger: 'change' }],
}

const entryRules: FormRules = {
  taskId: [{ required: true, message: '请选择教学任务', trigger: 'change' }],
  dayOfWeek: [{ required: true, message: '请选择星期', trigger: 'change' }],
  classroomId: [{ required: true, message: '请选择教室', trigger: 'change' }],
  periodStart: [{ required: true, message: '请选择开始节次', trigger: 'change' }],
  periodEnd: [{ required: true, message: '请选择结束节次', trigger: 'change' }],
  weekStart: [{ required: true, message: '请输入开始周', trigger: 'blur' }],
  weekEnd: [{ required: true, message: '请输入结束周', trigger: 'blur' }],
}

// 计算属性
const timetableOptions = computed(() => {
  // TODO: 根据 timetableType 返回不同选项
  if (timetableType.value === 'class') {
    return [
      { id: 1, name: '计算机2024-1班' },
      { id: 2, name: '计算机2024-2班' },
    ]
  } else if (timetableType.value === 'teacher') {
    return [
      { id: 1, name: '张老师' },
      { id: 2, name: '李老师' },
    ]
  } else {
    return [
      { id: 1, name: '教学楼A-101' },
      { id: 2, name: '教学楼A-102' },
    ]
  }
})

const timetableTitle = computed(() => {
  const target = timetableOptions.value.find(o => o.id === timetableTargetId.value)
  return target ? `${target.name} 课表` : '课表'
})

// 方法
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

const loadSchedules = async () => {
  if (!queryParams.semesterId) return
  loading.value = true
  try {
    const res = await scheduleApi.list({ semesterId: queryParams.semesterId })
    schedules.value = res.data || res
  } catch (error) {
    console.error('Failed to load schedules:', error)
  } finally {
    loading.value = false
  }
}

const loadTimetable = async () => {
  if (!timetableTargetId.value || !queryParams.semesterId) return
  loading.value = true
  try {
    let res
    if (timetableType.value === 'class') {
      res = await scheduleApi.getByClass(timetableTargetId.value, queryParams.semesterId)
    } else if (timetableType.value === 'teacher') {
      res = await scheduleApi.getByTeacher(timetableTargetId.value, queryParams.semesterId)
    } else {
      res = await scheduleApi.getByClassroom(timetableTargetId.value, queryParams.semesterId)
    }
    timetableEntries.value = res.data || res
  } catch (error) {
    console.error('Failed to load timetable:', error)
  } finally {
    loading.value = false
  }
}

const showScheduleDialog = (schedule?: CourseSchedule) => {
  scheduleForm.value = schedule
    ? { ...schedule }
    : { semesterId: queryParams.semesterId }
  scheduleDialogVisible.value = true
}

const saveSchedule = async () => {
  await scheduleFormRef.value?.validate()
  saving.value = true
  try {
    if (scheduleForm.value.id) {
      await scheduleApi.update(scheduleForm.value.id, scheduleForm.value)
    } else {
      await scheduleApi.create(scheduleForm.value)
    }
    ElMessage.success('保存成功')
    scheduleDialogVisible.value = false
    loadSchedules()
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const autoSchedule = (schedule: CourseSchedule) => {
  currentSchedule.value = schedule
  autoScheduleParams.value = { maxIterations: 1000, populationSize: 100, mutationRate: 0.1 }
  autoScheduleDialogVisible.value = true
}

const runAutoSchedule = async () => {
  if (!currentSchedule.value) return
  scheduling.value = true
  try {
    const result = await scheduleApi.autoSchedule({
      scheduleId: currentSchedule.value.id,
      ...autoScheduleParams.value,
    })
    const data = result.data || result
    if (data.success) {
      ElMessage.success(`排课完成，生成${data.entriesGenerated}条排课记录`)
      if (data.conflicts?.length > 0) {
        ElMessage.warning(`存在${data.conflicts.length}个冲突，请检查`)
      }
    } else {
      ElMessage.error('排课失败')
    }
    autoScheduleDialogVisible.value = false
    loadSchedules()
  } catch (error) {
    ElMessage.error('排课失败')
  } finally {
    scheduling.value = false
  }
}

const publishSchedule = async (schedule: CourseSchedule) => {
  await ElMessageBox.confirm('发布后课表将对所有用户可见，确定发布吗？', '提示', { type: 'warning' })
  try {
    await scheduleApi.publish(schedule.id)
    ElMessage.success('发布成功')
    loadSchedules()
  } catch (error) {
    ElMessage.error('发布失败')
  }
}

const deleteSchedule = async (schedule: CourseSchedule) => {
  await ElMessageBox.confirm('确定删除该排课方案吗？', '警告', { type: 'warning' })
  try {
    await scheduleApi.delete(schedule.id)
    ElMessage.success('删除成功')
    loadSchedules()
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const viewScheduleEntries = async (schedule: CourseSchedule) => {
  currentSchedule.value = schedule
  try {
    const res = await scheduleApi.getEntries(schedule.id)
    scheduleEntries.value = res.data || res
  } catch (error) {
    console.error('Failed to load entries:', error)
  }
  entriesDrawerVisible.value = true
}

const showEntryDialog = (entry?: ScheduleEntry) => {
  entryForm.value = entry
    ? { ...entry }
    : { weekStart: 1, weekEnd: 16, weekType: 0 }
  entryDialogVisible.value = true
}

const saveEntry = async () => {
  await entryFormRef.value?.validate()
  if (!currentSchedule.value) return
  saving.value = true
  try {
    if (entryForm.value.id) {
      await scheduleApi.updateEntry(currentSchedule.value.id, entryForm.value.id, entryForm.value)
    } else {
      await scheduleApi.addEntry(currentSchedule.value.id, entryForm.value)
    }
    ElMessage.success('保存成功')
    entryDialogVisible.value = false
    const res = await scheduleApi.getEntries(currentSchedule.value.id)
    scheduleEntries.value = res.data || res
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const deleteEntry = async (entry: ScheduleEntry) => {
  if (!currentSchedule.value) return
  await ElMessageBox.confirm('确定删除该排课吗？', '警告', { type: 'warning' })
  try {
    await scheduleApi.deleteEntry(currentSchedule.value.id, entry.id)
    ElMessage.success('删除成功')
    const res = await scheduleApi.getEntries(currentSchedule.value.id)
    scheduleEntries.value = res.data || res
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const checkConflicts = async () => {
  if (!currentSchedule.value) return
  try {
    const res = await scheduleApi.checkConflicts(currentSchedule.value.id)
    const data = res.data || res
    if (data.conflicts?.length > 0) {
      ElMessage.warning(`发现${data.conflicts.length}个冲突`)
    } else {
      ElMessage.success('未发现冲突')
    }
  } catch (error) {
    ElMessage.error('检测失败')
  }
}

const getEntriesForCell = (day: number, period: number) => {
  return timetableEntries.value.filter(
    e => e.dayOfWeek === day && e.periodStart <= period && e.periodEnd >= period
  )
}

const getEntryClass = (entry: ScheduleEntry) => {
  const colors = ['entry-blue', 'entry-green', 'entry-orange', 'entry-purple', 'entry-red']
  return colors[entry.taskId % colors.length]
}

const showEntryDetail = (entry: ScheduleEntry) => {
  ElMessage.info(`${entry.courseName} - ${entry.teacherName}`)
}

const formatWeeks = (entry: ScheduleEntry) => {
  const weekTypeText = entry.weekType === 1 ? '(单)' : entry.weekType === 2 ? '(双)' : ''
  return `${entry.weekStart}-${entry.weekEnd}周${weekTypeText}`
}

const getWeekdayName = (day: number) => {
  return weekdays.find(w => w.value === day)?.label || ''
}

const exportTimetable = () => {
  ElMessage.info('导出功能开发中...')
}

const getScheduleStatusName = (status: number) => {
  const names: Record<number, string> = { 0: '草稿', 1: '已发布', 2: '已归档' }
  return names[status] || '未知'
}

const getScheduleStatusTag = (status: number) => {
  const types: Record<number, '' | 'success' | 'warning' | 'danger' | 'info'> = {
    0: 'info',
    1: 'success',
    2: 'warning',
  }
  return types[status] || 'info'
}

onMounted(async () => {
  await loadSemesters()
  loadSchedules()
  // TODO: 加载教室列表
  classrooms.value = [
    { id: 1, name: '教学楼A-101' },
    { id: 2, name: '教学楼A-102' },
    { id: 3, name: '教学楼B-201' },
  ]
})
</script>

<style scoped lang="scss">
.schedule-view {
  padding: 20px;
}

.filter-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.timetable-container {
  overflow-x: auto;
}

.timetable {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;

  th, td {
    border: 1px solid #ebeef5;
    padding: 8px;
    text-align: center;
    vertical-align: top;
  }

  th {
    background: #f5f7fa;
    font-weight: 600;
  }

  .period-col {
    width: 80px;
  }

  .period-info {
    display: flex;
    flex-direction: column;
    gap: 4px;

    .period-name {
      font-weight: 600;
    }

    .period-time {
      font-size: 11px;
      color: #909399;
    }
  }

  .schedule-cell {
    min-height: 60px;
    padding: 4px;
  }

  .schedule-entry {
    padding: 6px;
    border-radius: 4px;
    margin-bottom: 4px;
    cursor: pointer;
    font-size: 12px;
    color: #fff;

    &.entry-blue { background: #409eff; }
    &.entry-green { background: #67c23a; }
    &.entry-orange { background: #e6a23c; }
    &.entry-purple { background: #9c27b0; }
    &.entry-red { background: #f56c6c; }

    .entry-course {
      font-weight: 600;
      margin-bottom: 2px;
    }

    .entry-info, .entry-weeks {
      font-size: 11px;
      opacity: 0.9;
    }
  }
}

.entries-header {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}
</style>
