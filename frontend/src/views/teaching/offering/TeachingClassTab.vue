<template>
  <!-- Stats bar -->
  <div class="flex items-center gap-4 border-b border-gray-200 bg-white px-6 py-2.5">
    <span class="text-sm text-gray-500">教学班总数 <span class="font-semibold text-gray-900">{{ teachingClasses.length }}</span></span>
    <div class="h-3 w-px bg-gray-200" />
    <span class="text-sm text-gray-500">普通 <span class="font-semibold text-gray-900">{{ tcNormalCount }}</span></span>
    <div class="h-3 w-px bg-gray-200" />
    <span class="text-sm text-gray-500">合堂 <span class="font-semibold text-gray-900">{{ tcCombinedCount }}</span></span>
    <div class="h-3 w-px bg-gray-200" />
    <span class="text-sm text-gray-500">走班 <span class="font-semibold text-gray-900">{{ tcWalkingCount }}</span></span>
  </div>

  <div class="flex-1 overflow-y-auto px-6 pt-5 pb-6">
    <!-- Action bar -->
    <div class="mb-4 flex items-center gap-2">
      <button
        class="inline-flex items-center gap-1.5 rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
        @click="autoGenerateTeachingClasses"
      >
        自动生成教学班
      </button>
      <button
        class="inline-flex items-center gap-1.5 rounded-lg bg-blue-600 px-3 py-2 text-sm font-medium text-white hover:bg-blue-700"
        @click="showTeachingClassDialog()"
      >
        手动创建
      </button>
    </div>

    <!-- Table -->
    <div class="overflow-hidden rounded-xl border border-gray-200 bg-white">
      <el-table :data="teachingClasses" v-loading="tcLoading" stripe>
        <el-table-column prop="className" label="教学班名称" min-width="160" />
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="getClassTypeTagType(row.classType)">
              {{ getClassTypeName(row.classType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="courseName" label="课程" min-width="140" />
        <el-table-column label="包含班级" min-width="160">
          <template #default="{ row }">
            <span v-if="row.members && row.members.length > 0" class="text-sm text-gray-600">
              {{ row.members.filter((m: TeachingClassMember) => m.memberType === 1).map((m: TeachingClassMember) => m.adminClassName).join(', ') || '-' }}
            </span>
            <span v-else class="text-sm text-gray-400">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="studentCount" label="学生人数" width="100" align="center" />
        <el-table-column prop="weeklyHours" label="周课时" width="80" align="center" />
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <button class="text-sm text-blue-600 hover:text-blue-800" @click="showTeachingClassDialog(row)">编辑</button>
            <button class="ml-3 text-sm text-cyan-600 hover:text-cyan-800" @click="showMembersDialog(row)">成员</button>
            <button class="ml-3 text-sm text-red-600 hover:text-red-800" @click="deleteTeachingClass(row)">删除</button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div v-if="!tcLoading && teachingClasses.length === 0" class="mt-10 text-center text-sm text-gray-400">
      暂无教学班，可点击"自动生成教学班"快速创建
    </div>
  </div>

  <!-- Teaching Class Create/Edit Dialog -->
  <el-dialog
    v-model="tcDialogVisible"
    :title="editingTC ? '编辑教学班' : '创建教学班'"
    width="560px"
    destroy-on-close
  >
    <el-form ref="tcFormRef" :model="tcForm" :rules="tcRules" label-width="100px" class="pr-4">
      <el-form-item label="教学班名称" prop="className">
        <el-input v-model="tcForm.className" placeholder="如: 高一(1,2)班数学" />
      </el-form-item>
      <el-form-item label="课程" prop="courseId">
        <el-select v-model="tcForm.courseId" placeholder="选择课程" filterable class="w-full">
          <el-option v-for="c in allCourses" :key="c.id" :value="c.id" :label="`${c.courseCode} - ${c.courseName}`" />
        </el-select>
      </el-form-item>
      <el-form-item label="教学班类型" prop="classType">
        <el-radio-group v-model="tcForm.classType">
          <el-radio :value="1">普通</el-radio>
          <el-radio :value="2">合堂</el-radio>
          <el-radio :value="3">走班</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="周课时" prop="weeklyHours">
        <el-input-number v-model="tcForm.weeklyHours" :min="1" :max="20" />
      </el-form-item>
      <el-form-item label="起始周" prop="startWeek">
        <el-input-number v-model="tcForm.startWeek" :min="1" :max="30" />
      </el-form-item>
      <el-form-item label="结束周">
        <el-input-number v-model="tcForm.endWeek" :min="tcForm.startWeek" :max="30" />
      </el-form-item>
      <el-form-item label="教室类型">
        <el-input v-model="tcForm.requiredRoomType" placeholder="如: 多媒体教室、实验室" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="tcForm.remark" type="textarea" :rows="2" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="tcDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="tcSaving" @click="saveTeachingClass">保存</el-button>
    </template>
  </el-dialog>

  <!-- Members Dialog -->
  <el-dialog v-model="membersDialogVisible" title="教学班成员" width="640px" destroy-on-close>
    <div v-if="selectedTC" class="mb-4 text-sm text-gray-500">
      {{ selectedTC.className }} - {{ selectedTC.courseName }}
      <span class="ml-2">( {{ selectedTC.studentCount }} 人 )</span>
    </div>

    <!-- Add member -->
    <div class="mb-4 flex items-center gap-2">
      <el-select v-model="newMemberClassId" placeholder="选择行政班级" filterable class="flex-1">
        <el-option v-for="c in classes" :key="c.id" :value="c.id" :label="c.className" />
      </el-select>
      <el-button type="primary" size="small" :disabled="!newMemberClassId" @click="addClassMember">
        添加整班
      </el-button>
    </div>

    <!-- Members list -->
    <el-table :data="currentMembers" v-loading="membersLoading" stripe max-height="360">
      <el-table-column label="类型" width="80" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="(row.memberType === 1 ? '' : 'success') as any">
            {{ row.memberType === 1 ? '整班' : '个人' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="名称" min-width="160">
        <template #default="{ row }">
          {{ row.memberType === 1 ? row.adminClassName : row.studentName }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80" align="center">
        <template #default="{ row }">
          <button class="text-sm text-red-600 hover:text-red-800" @click="removeMember(row)">移除</button>
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { teachingClassApi } from '@/api/teaching'
import { courseApi } from '@/api/academic'
import { schoolClassApi } from '@/api/organization'
import type { TeachingClass, TeachingClassMember, Course } from '@/types/teaching'
import type { SchoolClass } from '@/types/organization'

const props = defineProps<{
  semesterId: number | string | null
}>()

// Data
const teachingClasses = ref<TeachingClass[]>([])
const tcLoading = ref(false)
const allCourses = ref<Course[]>([])
const classes = ref<SchoolClass[]>([])

// Computed
const tcNormalCount = computed(() => teachingClasses.value.filter(t => t.classType === 1).length)
const tcCombinedCount = computed(() => teachingClasses.value.filter(t => t.classType === 2).length)
const tcWalkingCount = computed(() => teachingClasses.value.filter(t => t.classType === 3).length)

// Dialog state
const tcDialogVisible = ref(false)
const tcSaving = ref(false)
const editingTC = ref<TeachingClass | null>(null)
const tcFormRef = ref<FormInstance>()

const tcForm = ref({
  className: '',
  courseId: undefined as number | undefined,
  classType: 1 as 1 | 2 | 3,
  weeklyHours: 2,
  startWeek: 1,
  endWeek: undefined as number | undefined,
  requiredRoomType: '',
  remark: '',
})

const tcRules: FormRules = {
  className: [{ required: true, message: '请填写教学班名称', trigger: 'blur' }],
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  classType: [{ required: true, message: '请选择类型', trigger: 'change' }],
  weeklyHours: [{ required: true, message: '请填写周课时', trigger: 'blur' }],
  startWeek: [{ required: true, message: '请填写起始周', trigger: 'blur' }],
}

// Members dialog
const membersDialogVisible = ref(false)
const membersLoading = ref(false)
const selectedTC = ref<TeachingClass | null>(null)
const currentMembers = ref<TeachingClassMember[]>([])
const newMemberClassId = ref<number | string>('')

// Helpers
function getClassTypeName(type: number) {
  const map: Record<number, string> = { 1: '普通', 2: '合堂', 3: '走班' }
  return map[type] || '-'
}

function getClassTypeTagType(type: number) {
  const map: Record<number, '' | 'success' | 'warning' | 'danger' | 'info'> = { 1: '', 2: 'warning', 3: 'success' }
  return map[type] || 'info'
}

// Data loading
async function loadCourses() {
  try {
    allCourses.value = await courseApi.listAll()
  } catch {
    // non-critical
  }
}

async function loadClasses() {
  try {
    classes.value = await schoolClassApi.getAll()
  } catch {
    // non-critical
  }
}

async function loadTeachingClasses() {
  if (!props.semesterId) {
    teachingClasses.value = []
    return
  }
  tcLoading.value = true
  try {
    teachingClasses.value = await teachingClassApi.list(props.semesterId)
  } catch {
    ElMessage.error('加载教学班列表失败')
    teachingClasses.value = []
  } finally {
    tcLoading.value = false
  }
}

// Teaching Class CRUD
function showTeachingClassDialog(row?: TeachingClass) {
  editingTC.value = row || null
  if (row) {
    tcForm.value = {
      className: row.className,
      courseId: row.courseId,
      classType: row.classType,
      weeklyHours: row.weeklyHours,
      startWeek: row.startWeek,
      endWeek: row.endWeek,
      requiredRoomType: row.requiredRoomType || '',
      remark: row.remark || '',
    }
  } else {
    tcForm.value = {
      className: '',
      courseId: undefined,
      classType: 1,
      weeklyHours: 2,
      startWeek: 1,
      endWeek: undefined,
      requiredRoomType: '',
      remark: '',
    }
  }
  tcDialogVisible.value = true
}

async function saveTeachingClass() {
  if (!tcFormRef.value) return
  const valid = await tcFormRef.value.validate().catch(() => false)
  if (!valid) return

  tcSaving.value = true
  try {
    const payload: Partial<TeachingClass> = {
      semesterId: Number(props.semesterId),
      className: tcForm.value.className,
      courseId: tcForm.value.courseId,
      classType: tcForm.value.classType,
      weeklyHours: tcForm.value.weeklyHours,
      startWeek: tcForm.value.startWeek,
      endWeek: tcForm.value.endWeek,
      requiredRoomType: tcForm.value.requiredRoomType || undefined,
      remark: tcForm.value.remark || undefined,
    }

    if (editingTC.value) {
      await teachingClassApi.update(editingTC.value.id, payload)
      ElMessage.success('更新成功')
    } else {
      await teachingClassApi.create(payload)
      ElMessage.success('创建成功')
    }
    tcDialogVisible.value = false
    loadTeachingClasses()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    tcSaving.value = false
  }
}

async function deleteTeachingClass(row: TeachingClass) {
  try {
    await ElMessageBox.confirm('确定删除该教学班?', '删除确认', { type: 'warning' })
    await teachingClassApi.delete(row.id)
    ElMessage.success('已删除')
    loadTeachingClasses()
  } catch {
    // cancelled or error
  }
}

async function autoGenerateTeachingClasses() {
  if (!props.semesterId) {
    ElMessage.warning('请先选择学期')
    return
  }
  try {
    await ElMessageBox.confirm(
      '将根据已确认的班级课程分配自动生成教学班。合堂课程会自动合并，走班课程会单独建班。是否继续?',
      '自动生成',
      { type: 'info' }
    )
    tcLoading.value = true
    await teachingClassApi.autoGenerate(props.semesterId)
    ElMessage.success('自动生成完成')
    loadTeachingClasses()
  } catch {
    // cancelled or error
  } finally {
    tcLoading.value = false
  }
}

// Members
async function showMembersDialog(row: TeachingClass) {
  selectedTC.value = row
  membersDialogVisible.value = true
  newMemberClassId.value = ''
  await loadMembers(row.id)
}

async function loadMembers(tcId: number) {
  membersLoading.value = true
  try {
    currentMembers.value = await teachingClassApi.getMembers(tcId)
  } catch {
    currentMembers.value = []
  } finally {
    membersLoading.value = false
  }
}

async function addClassMember() {
  if (!selectedTC.value || !newMemberClassId.value) return
  try {
    await teachingClassApi.addMembers(selectedTC.value.id, [
      { teachingClassId: selectedTC.value.id, memberType: 1, adminClassId: Number(newMemberClassId.value) },
    ])
    ElMessage.success('已添加')
    newMemberClassId.value = ''
    loadMembers(selectedTC.value.id)
    loadTeachingClasses() // refresh student count
  } catch {
    ElMessage.error('添加失败')
  }
}

async function removeMember(row: TeachingClassMember) {
  if (!selectedTC.value) return
  try {
    await ElMessageBox.confirm('确定移除该成员?', '确认', { type: 'warning' })
    await teachingClassApi.removeMembers(selectedTC.value.id, [row.id])
    ElMessage.success('已移除')
    loadMembers(selectedTC.value.id)
    loadTeachingClasses()
  } catch {
    // cancelled or error
  }
}

// Watch semester changes
watch(() => props.semesterId, () => {
  loadTeachingClasses()
})

onMounted(() => {
  loadCourses()
  loadClasses()
  loadTeachingClasses()
})
</script>
