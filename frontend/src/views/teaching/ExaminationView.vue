<template>
  <div class="examination-view">
    <el-card class="filter-card">
      <el-form :inline="true" :model="queryParams">
        <el-form-item label="学期">
          <el-select v-model="queryParams.semesterId" placeholder="选择学期" @change="loadBatches">
            <el-option v-for="sem in semesters" :key="sem.id" :value="sem.id" :label="sem.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="考试类型">
          <el-select v-model="queryParams.examType" placeholder="全部" clearable @change="loadBatches">
            <el-option :value="1" label="期中考试" />
            <el-option :value="2" label="期末考试" />
            <el-option :value="3" label="补考" />
            <el-option :value="4" label="重修考试" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable @change="loadBatches">
            <el-option :value="0" label="草稿" />
            <el-option :value="1" label="已发布" />
            <el-option :value="2" label="进行中" />
            <el-option :value="3" label="已结束" />
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>考试批次列表</span>
          <el-button type="primary" @click="showBatchDialog()">新建考试批次</el-button>
        </div>
      </template>

      <el-table :data="batches" v-loading="loading" border stripe>
        <el-table-column prop="name" label="批次名称" min-width="200" />
        <el-table-column prop="examType" label="考试类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getExamTypeTag(row.examType)">
              {{ getExamTypeName(row.examType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="考试时间" width="200">
          <template #default="{ row }">
            {{ row.startDate }} ~ {{ row.endDate }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.status)">{{ getStatusName(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text @click="viewArrangements(row)">安排</el-button>
            <el-button size="small" text @click="showBatchDialog(row)">编辑</el-button>
            <el-button
              v-if="row.status === 0"
              size="small"
              text
              type="success"
              @click="publishBatch(row)"
            >发布</el-button>
            <el-button
              v-if="row.status === 0"
              size="small"
              text
              type="danger"
              @click="deleteBatch(row)"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="queryParams.page"
        v-model:page-size="queryParams.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @size-change="loadBatches"
        @current-change="loadBatches"
      />
    </el-card>

    <!-- 考试批次对话框 -->
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
          <el-input v-model="batchForm.remark" type="textarea" rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveBatch">保存</el-button>
      </template>
    </el-dialog>

    <!-- 考试安排抽屉 -->
    <el-drawer v-model="arrangementsDrawerVisible" title="考试安排" size="70%">
      <div class="arrangements-header">
        <div class="batch-info">
          <h3>{{ currentBatch?.name }}</h3>
          <div class="batch-meta">
            <span>{{ getExamTypeName(currentBatch?.examType || 0) }}</span>
            <span>{{ currentBatch?.startDate }} ~ {{ currentBatch?.endDate }}</span>
            <el-tag :type="getStatusTag(currentBatch?.status || 0)">
              {{ getStatusName(currentBatch?.status || 0) }}
            </el-tag>
          </div>
        </div>
        <el-button v-if="currentBatch?.status === 0" type="primary" @click="showArrangementDialog()">
          添加考试安排
        </el-button>
      </div>

      <el-table :data="arrangements" border style="margin-top: 16px">
        <el-table-column prop="courseName" label="课程" min-width="150" />
        <el-table-column label="班级" width="150">
          <template #default="{ row }">
            <el-tag v-for="name in row.classNames" :key="name" size="small" style="margin: 2px">
              {{ name }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="examDate" label="考试日期" width="120" />
        <el-table-column label="考试时间" width="150">
          <template #default="{ row }">
            {{ row.startTime }} - {{ row.endTime }}
          </template>
        </el-table-column>
        <el-table-column prop="duration" label="时长(分钟)" width="100" align="center" />
        <el-table-column label="考场" width="150">
          <template #default="{ row }">
            <div v-if="row.examRooms?.length">
              <div v-for="room in row.examRooms" :key="room.id" class="room-info">
                {{ room.classroomName }} ({{ room.actualCount }}/{{ room.capacity }})
              </div>
            </div>
            <span v-else class="text-muted">未分配</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button size="small" text @click="showArrangementDialog(row)">编辑</el-button>
            <el-button size="small" text type="primary" @click="showRoomAssignDialog(row)">分配考场</el-button>
            <el-button
              v-if="currentBatch?.status === 0"
              size="small"
              text
              type="danger"
              @click="deleteArrangement(row)"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>

    <!-- 考试安排对话框 -->
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
              :label="`${c.code} - ${c.name}`"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="班级" prop="classIds">
          <el-select v-model="arrangementForm.classIds" multiple placeholder="可多选" style="width: 100%">
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
              <span style="margin-left: 8px; color: #909399">分钟</span>
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

    <!-- 分配考场对话框 -->
    <el-dialog v-model="roomAssignDialogVisible" title="分配考场" width="600px">
      <div v-if="currentArrangement" class="arrangement-info">
        <p><strong>课程：</strong>{{ currentArrangement.courseName }}</p>
        <p><strong>考试时间：</strong>{{ currentArrangement.examDate }} {{ currentArrangement.startTime }}-{{ currentArrangement.endTime }}</p>
      </div>

      <el-divider />

      <div class="room-assign-section">
        <div class="section-header">
          <span>已分配考场</span>
          <el-button type="primary" size="small" @click="addRoom">添加考场</el-button>
        </div>

        <el-table :data="assignedRooms" border style="margin-top: 12px">
          <el-table-column prop="classroomName" label="教室" />
          <el-table-column prop="capacity" label="容量" width="80" align="center" />
          <el-table-column prop="actualCount" label="实际人数" width="100" align="center">
            <template #default="{ row }">
              <el-input-number v-model="row.actualCount" :min="0" :max="row.capacity" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="监考教师" min-width="150">
            <template #default="{ row }">
              <el-select v-model="row.invigilatorIds" multiple size="small" placeholder="选择监考" style="width: 100%">
                <el-option v-for="t in teacherOptions" :key="t.id" :value="t.id" :label="t.name" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ $index }">
              <el-button size="small" text type="danger" @click="removeRoom($index)">移除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button @click="roomAssignDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRoomAssignment">保存</el-button>
      </template>
    </el-dialog>

    <!-- 添加考场对话框 -->
    <el-dialog v-model="addRoomDialogVisible" title="选择考场" width="400px">
      <el-form :model="newRoomForm" label-width="80px">
        <el-form-item label="教室">
          <el-select v-model="newRoomForm.classroomId" placeholder="选择教室" style="width: 100%">
            <el-option v-for="c in classroomOptions" :key="c.id" :value="c.id" :label="c.name">
              <span>{{ c.name }}</span>
              <span style="float: right; color: #909399">容量: {{ c.capacity }}</span>
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { examApi, semesterApi, courseApi } from '@/api/teaching'
import type { ExamBatch, ExamArrangement, Semester, Course, ExamBatchQueryParams } from '@/types/teaching'

// 状态
const loading = ref(false)
const saving = ref(false)
const batches = ref<ExamBatch[]>([])
const arrangements = ref<ExamArrangement[]>([])
const semesters = ref<Semester[]>([])
const courseOptions = ref<Course[]>([])
const total = ref(0)
const currentBatch = ref<ExamBatch>()
const currentArrangement = ref<ExamArrangement>()

// 对话框状态
const batchDialogVisible = ref(false)
const arrangementsDrawerVisible = ref(false)
const arrangementDialogVisible = ref(false)
const roomAssignDialogVisible = ref(false)
const addRoomDialogVisible = ref(false)

// 表单
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

// 选项数据
const classOptions = ref([
  { id: 1, name: '计算机2024-1班' },
  { id: 2, name: '计算机2024-2班' },
  { id: 3, name: '软件2024-1班' },
])
const teacherOptions = ref([
  { id: 1, name: '张老师' },
  { id: 2, name: '李老师' },
  { id: 3, name: '王老师' },
])
const classroomOptions = ref([
  { id: 1, name: '教学楼A-101', capacity: 60 },
  { id: 2, name: '教学楼A-102', capacity: 60 },
  { id: 3, name: '教学楼B-201', capacity: 80 },
])

// 验证规则
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

const loadBatches = async () => {
  loading.value = true
  try {
    const res = await examApi.listBatches(queryParams)
    const data = res.data || res
    batches.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    console.error('Failed to load batches:', error)
  } finally {
    loading.value = false
  }
}

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

const viewArrangements = async (batch: ExamBatch) => {
  currentBatch.value = batch
  try {
    const res = await examApi.getArrangements(batch.id)
    arrangements.value = res.data || res
  } catch (error) {
    console.error('Failed to load arrangements:', error)
  }
  arrangementsDrawerVisible.value = true
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
    const res = await examApi.getArrangements(currentBatch.value.id)
    arrangements.value = res.data || res
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
    const res = await examApi.getArrangements(currentBatch.value.id)
    arrangements.value = res.data || res
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

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
  const classroom = classroomOptions.value.find(c => c.id === newRoomForm.value.classroomId)
  if (!classroom) return
  if (assignedRooms.value.some(r => r.classroomId === classroom.id)) {
    ElMessage.warning('该教室已添加')
    return
  }
  assignedRooms.value.push({
    classroomId: classroom.id,
    classroomName: classroom.name,
    capacity: classroom.capacity,
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
    await examApi.assignRooms(
      currentArrangement.value.id,
      assignedRooms.value.map(r => ({
        classroomId: r.classroomId,
        capacity: r.capacity,
      }))
    )
    // TODO: 保存监考教师
    ElMessage.success('保存成功')
    roomAssignDialogVisible.value = false
    if (currentBatch.value) {
      const res = await examApi.getArrangements(currentBatch.value.id)
      arrangements.value = res.data || res
    }
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const getExamTypeName = (type: number) => {
  const names: Record<number, string> = { 1: '期中考试', 2: '期末考试', 3: '补考', 4: '重修考试' }
  return names[type] || '未知'
}

const getExamTypeTag = (type: number) => {
  const types: Record<number, '' | 'success' | 'warning' | 'danger' | 'info'> = {
    1: 'info',
    2: '',
    3: 'warning',
    4: 'danger',
  }
  return types[type] || 'info'
}

const getStatusName = (status: number) => {
  const names: Record<number, string> = { 0: '草稿', 1: '已发布', 2: '进行中', 3: '已结束' }
  return names[status] || '未知'
}

const getStatusTag = (status: number) => {
  const types: Record<number, '' | 'success' | 'warning' | 'danger' | 'info'> = {
    0: 'info',
    1: 'success',
    2: 'warning',
    3: 'danger',
  }
  return types[status] || 'info'
}

onMounted(async () => {
  await loadSemesters()
  loadBatches()
})
</script>

<style scoped lang="scss">
.examination-view {
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

.arrangements-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;

  .batch-info {
    h3 {
      margin: 0 0 8px 0;
    }

    .batch-meta {
      display: flex;
      gap: 16px;
      color: #606266;
      font-size: 14px;
      align-items: center;
    }
  }
}

.room-info {
  font-size: 12px;
  color: #606266;
  margin-bottom: 4px;
}

.text-muted {
  color: #909399;
  font-size: 12px;
}

.arrangement-info {
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
  margin-bottom: 16px;

  p {
    margin: 4px 0;
  }
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
