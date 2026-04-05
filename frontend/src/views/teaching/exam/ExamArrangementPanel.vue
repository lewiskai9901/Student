<template>
  <el-drawer v-model="drawerVisible" title="" size="72%" :with-header="false" @close="onDrawerClose">
    <div class="flex h-full flex-col">
      <!-- Drawer Header -->
      <div class="flex items-center justify-between border-b border-gray-200 px-6 py-4">
        <div>
          <h2 class="text-lg font-semibold text-gray-900">{{ batch?.name }}</h2>
          <div class="mt-1 flex items-center gap-3 text-sm text-gray-500">
            <span>{{ getExamTypeName(batch?.examType || 0) }}</span>
            <div class="h-3 w-px bg-gray-200" />
            <span>{{ batch?.startDate }} ~ {{ batch?.endDate }}</span>
            <span
              class="inline-flex rounded px-1.5 py-0.5 text-xs font-medium"
              :class="statusBadgeClass(batch?.status || 0)"
            >
              {{ getStatusName(batch?.status || 0) }}
            </span>
          </div>
        </div>
        <div class="flex items-center gap-2">
          <button
            v-if="batch?.status === 0"
            class="inline-flex items-center gap-1.5 rounded-lg bg-blue-600 px-3.5 py-2 text-sm font-medium text-white transition-colors hover:bg-blue-700"
            @click="showArrangementDialog()"
          >
            <Plus class="h-4 w-4" />
            添加考试安排
          </button>
          <button
            class="inline-flex h-8 w-8 items-center justify-center rounded-lg border border-gray-200 text-gray-400 hover:bg-gray-50"
            @click="drawerVisible = false"
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
                  <button class="rounded px-2 py-1 text-xs font-medium text-blue-600 hover:bg-blue-50" @click="emit('selectArrangement', row)">分配考场</button>
                  <button
                    v-if="batch?.status === 0"
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
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Plus, X } from 'lucide-vue-next'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { examApi } from '@/api/teaching'
import { courseApi } from '@/api/academic'
import { schoolClassApi } from '@/api/organization'
import type { ExamBatch, ExamArrangement, Course } from '@/types/teaching'
import { getExamTypeName, getStatusName, statusBadgeClass } from './examUtils'

const props = defineProps<{
  batch: ExamBatch | undefined
}>()

const emit = defineEmits<{
  selectArrangement: [arrangement: ExamArrangement]
  close: []
}>()

// Drawer visibility driven by batch prop
const drawerVisible = ref(false)

watch(() => props.batch, (val) => {
  if (val) {
    drawerVisible.value = true
    loadArrangements()
  } else {
    drawerVisible.value = false
  }
})

function onDrawerClose() {
  emit('close')
}

// State
const saving = ref(false)
const arrangements = ref<ExamArrangement[]>([])
const courseOptions = ref<Course[]>([])
const classOptions = ref<{ id: number | string; name: string }[]>([])

// Dialog
const arrangementDialogVisible = ref(false)
const arrangementFormRef = ref<FormInstance>()
const arrangementForm = ref<Partial<ExamArrangement>>({})

const arrangementsWithRooms = computed(() => {
  return arrangements.value.filter(a => a.examRooms && a.examRooms.length > 0).length
})

const arrangementRules: FormRules = {
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  classIds: [{ required: true, type: 'array', min: 1, message: '请选择班级', trigger: 'change' }],
  examDate: [{ required: true, message: '请选择考试日期', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  duration: [{ required: true, message: '请输入考试时长', trigger: 'blur' }],
}

// Data loading
async function loadArrangements() {
  if (!props.batch) return
  try {
    const res: any = await examApi.getArrangements(props.batch.id)
    arrangements.value = res.data || res
  } catch (error) {
    console.error('Failed to load arrangements:', error)
    arrangements.value = []
  }
}

async function loadClassOptions() {
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

// Operations
const showArrangementDialog = (arrangement?: ExamArrangement) => {
  arrangementForm.value = arrangement
    ? { ...arrangement }
    : { duration: 120 }
  arrangementDialogVisible.value = true
}

const saveArrangement = async () => {
  await arrangementFormRef.value?.validate()
  if (!props.batch) return
  saving.value = true
  try {
    if (arrangementForm.value.id) {
      await examApi.updateArrangement(props.batch.id, arrangementForm.value.id, arrangementForm.value)
    } else {
      await examApi.createArrangement(props.batch.id, arrangementForm.value)
    }
    ElMessage.success('保存成功')
    arrangementDialogVisible.value = false
    loadArrangements()
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const deleteArrangement = async (arrangement: ExamArrangement) => {
  if (!props.batch) return
  await ElMessageBox.confirm('确定删除该考试安排吗？', '警告', { type: 'warning' })
  try {
    await examApi.deleteArrangement(props.batch.id, arrangement.id)
    ElMessage.success('删除成功')
    loadArrangements()
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

// Load class options on mount
loadClassOptions()
</script>
