<template>
  <div class="p-4">
    <!-- 加载状态 -->
    <div v-if="loading" class="flex items-center justify-center py-12">
      <Loader2 class="h-6 w-6 animate-spin text-blue-600" />
      <span class="ml-2 text-gray-500">加载中...</span>
    </div>

    <template v-else>
      <!-- 头部信息 -->
      <div class="mb-6 text-center">
        <h3 class="text-lg font-semibold text-gray-900">床位分配管理</h3>
        <p class="mt-1 text-sm text-gray-500">
          {{ dormitoryInfo?.buildingName }} {{ dormitoryInfo?.dormitoryNo }}
        </p>
      </div>

      <!-- 床位网格 -->
      <div class="grid grid-cols-2 gap-4 mb-6">
        <div
          v-for="bed in bedList"
          :key="bed.bedNumber"
          class="rounded-lg border-2 p-4 transition-colors"
          :class="bed.isAssigned ? 'border-green-300 bg-green-50' : 'border-dashed border-gray-300 bg-gray-50'"
        >
          <div class="flex items-center justify-between mb-3">
            <span class="text-sm font-semibold text-gray-900">{{ bed.bedNumber }}号床</span>
            <span
              :class="bed.isAssigned ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'"
              class="rounded px-2 py-0.5 text-xs font-medium"
            >
              {{ bed.isAssigned ? '已入住' : '空床位' }}
            </span>
          </div>

          <div v-if="bed.isAssigned" class="mb-3">
            <p class="text-sm font-medium text-gray-900">{{ bed.studentName }}</p>
            <p class="text-xs text-gray-500">{{ bed.studentNo }}</p>
            <p class="text-xs text-gray-400">入住: {{ formatDate(bed.assignedAt) }}</p>
          </div>
          <div v-else class="mb-3 flex flex-col items-center py-4 text-gray-400">
            <BedDouble class="h-8 w-8" />
            <span class="mt-2 text-sm">空床位</span>
          </div>

          <div class="text-center">
            <button
              v-if="!bed.isAssigned"
              @click="handleAssign(bed)"
              class="inline-flex h-8 items-center gap-1.5 rounded-lg bg-blue-600 px-3 text-xs font-medium text-white hover:bg-blue-700"
            >
              <UserPlus class="h-3.5 w-3.5" />
              分配学生
            </button>
            <button
              v-else
              @click="handleRelease(bed)"
              class="inline-flex h-8 items-center gap-1.5 rounded-lg bg-red-50 px-3 text-xs font-medium text-red-600 hover:bg-red-100"
            >
              <UserMinus class="h-3.5 w-3.5" />
              释放床位
            </button>
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="flex justify-end border-t border-gray-200 pt-4">
        <button
          @click="$emit('close')"
          class="h-9 rounded-lg border border-gray-300 px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
        >
          关闭
        </button>
      </div>
    </template>

    <!-- 分配学生对话框 -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="assignDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center">
          <div class="fixed inset-0 bg-black/50" @click="assignDialogVisible = false"></div>
          <div class="relative w-full max-w-lg rounded-lg bg-white p-6 shadow-xl">
            <div class="mb-4 flex items-center justify-between">
              <h3 class="text-lg font-semibold text-gray-900">分配学生到 {{ assignForm.bedNo }}号床</h3>
              <button @click="assignDialogVisible = false" class="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-600">
                <X class="h-5 w-5" />
              </button>
            </div>

            <!-- 宿舍信息 -->
            <div class="mb-4 rounded-lg border border-blue-200 bg-blue-50 p-3">
              <p class="text-sm text-blue-800">
                <span class="font-medium">{{ dormitoryInfo?.buildingName || '' }} - {{ dormitoryInfo?.dormitoryNo || '' }}</span>
                <span class="mx-2">|</span>
                床位号: <strong>{{ assignForm.bedNo }}号床</strong>
                <span class="mx-2">|</span>
                当前入住: {{ dormitoryInfo?.occupiedBeds || 0 }}/{{ dormitoryInfo?.bedCapacity || 0 }}人
              </p>
            </div>

            <!-- 搜索框 -->
            <div class="mb-4">
              <div class="relative">
                <Search class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                <input
                  v-model="studentSearchKeyword"
                  type="text"
                  placeholder="输入学号或姓名搜索学生"
                  class="h-9 w-full rounded-lg border border-gray-300 pl-10 pr-3 text-sm focus:border-blue-500 focus:outline-none"
                />
              </div>
            </div>

            <!-- 过滤提示 -->
            <div class="mb-4 flex items-center gap-2 rounded-lg border border-blue-100 bg-blue-50 px-3 py-2 text-xs text-blue-600">
              <Info class="h-4 w-4" />
              已自动过滤: 未分配宿舍、符合性别要求、且班级属于本宿舍绑定班级的学生
            </div>

            <!-- 绑定班级信息 -->
            <div v-if="dormitoryInfo?.assignedClassNames" class="mb-4 rounded-lg border border-green-200 bg-green-50 px-3 py-2 text-xs text-green-700">
              <span class="font-medium">绑定班级: </span>{{ dormitoryInfo.assignedClassNames }}
            </div>

            <!-- 学生列表 -->
            <div class="max-h-64 overflow-y-auto rounded-lg border border-gray-200">
              <div v-if="filteredStudents.length > 0" class="divide-y divide-gray-100">
                <div
                  v-for="student in filteredStudents"
                  :key="student.id"
                  @click="selectStudent(student.id)"
                  class="flex cursor-pointer items-center justify-between p-3 hover:bg-gray-50"
                  :class="{ 'bg-blue-50': assignForm.studentId === student.id }"
                >
                  <div class="flex items-center gap-3">
                    <div class="flex h-9 w-9 items-center justify-center rounded-full bg-blue-100 text-sm font-medium text-blue-600">
                      {{ student.name?.charAt(0) || '?' }}
                    </div>
                    <div>
                      <p class="text-sm font-medium text-gray-900">{{ student.name }}</p>
                      <p class="text-xs text-gray-500">
                        学号: {{ student.studentNo }} | {{ student.gender === 1 ? '男' : '女' }}
                        <span v-if="student.className"> | {{ student.className }}</span>
                      </p>
                    </div>
                  </div>
                  <div v-if="assignForm.studentId === student.id" class="text-green-600">
                    <CheckCircle class="h-5 w-5" />
                  </div>
                </div>
              </div>
              <div v-else class="py-8 text-center text-gray-400">
                <Users class="mx-auto h-10 w-10" />
                <p class="mt-2 text-sm">没有符合条件的学生</p>
              </div>
            </div>

            <!-- 操作按钮 -->
            <div class="mt-4 flex justify-end gap-3">
              <button
                @click="assignDialogVisible = false"
                class="h-9 rounded-lg border border-gray-300 px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
              >
                取消
              </button>
              <button
                @click="confirmAssign"
                :disabled="!assignForm.studentId"
                class="h-9 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
              >
                确定分配
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loader2, BedDouble, UserPlus, UserMinus, X, Search, Info, CheckCircle, Users } from 'lucide-vue-next'
import { formatDate } from '@/utils/date'
// V2 DDD API
import { getDormitory, getBedAllocations, assignBed, releaseBed } from '@/api/dormitory'
import { getStudents } from '@/api/student'

interface Props {
  dormitoryId: number | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  success: []
  close: []
}>()

const loading = ref(false)
const dormitoryInfo = ref<any>(null)
const bedList = ref<any[]>([])
const availableStudents = ref<any[]>([])
const studentSearchKeyword = ref('')

const assignDialogVisible = ref(false)
const assignForm = reactive({
  bedNo: '',
  studentId: null as number | null
})

// 过滤后的学生列表
const filteredStudents = computed(() => {
  if (!studentSearchKeyword.value) {
    return availableStudents.value
  }
  const keyword = studentSearchKeyword.value.toLowerCase()
  return availableStudents.value.filter((student: any) =>
    student.name?.toLowerCase().includes(keyword) ||
    student.studentNo?.toLowerCase().includes(keyword)
  )
})

// 选择学生
const selectStudent = (studentId: number) => {
  assignForm.studentId = studentId
}

const loadDormitoryInfo = async () => {
  if (!props.dormitoryId) return

  loading.value = true
  try {
    dormitoryInfo.value = await getDormitory(props.dormitoryId)
    const allocations = await getBedAllocations(props.dormitoryId)
    bedList.value = allocations || []
    await loadAvailableStudents()
  } catch (error: any) {
    ElMessage.error('加载宿舍信息失败')
  } finally {
    loading.value = false
  }
}

// 加载可分配的学生列表
const loadAvailableStudents = async () => {
  try {
    const dormitoryType = dormitoryInfo.value?.dormitoryType
    const roomUsageType = dormitoryInfo.value?.roomUsageType

    if (roomUsageType !== 1) {
      ElMessage.warning('该房间不是学生宿舍,无法分配学生')
      availableStudents.value = []
      return
    }

    if (dormitoryType !== 1 && dormitoryType !== 2) {
      const typeName = dormitoryInfo.value?.dormitoryTypeName || '该类型宿舍楼'
      ElMessage.warning(`${typeName}不能分配学生`)
      availableStudents.value = []
      return
    }

    // 获取该宿舍绑定的班级ID列表
    const assignedClassIds = dormitoryInfo.value?.assignedClassIds
    let allowedClassIds: string[] = []
    if (assignedClassIds) {
      allowedClassIds = String(assignedClassIds).split(',').map(id => id.trim()).filter(id => id)
    }

    // 如果宿舍没有绑定任何班级，则不允许分配学生
    if (allowedClassIds.length === 0) {
      ElMessage.warning('该宿舍尚未绑定班级，请先在班级管理中分配宿舍')
      availableStudents.value = []
      return
    }

    // V2: status=0 表示在读
    const result = await getStudents({
      pageNum: 1,
      pageSize: 1000,
      status: 0
    })

    let students = (result.records || []).filter((s: any) => !s.dormitoryId)

    // 按性别过滤
    if (dormitoryType === 1) {
      students = students.filter((s: any) => s.gender === 1)
    } else if (dormitoryType === 2) {
      students = students.filter((s: any) => s.gender === 2)
    }

    // 按班级过滤 - 只显示班级属于该宿舍绑定班级的学生
    students = students.filter((s: any) => {
      if (!s.classId) return false
      return allowedClassIds.includes(String(s.classId))
    })

    // V2: realName → name
    availableStudents.value = students.map((s: any) => ({
      id: s.id,
      name: s.name,
      studentNo: s.studentNo,
      gender: s.gender,
      className: s.className
    }))
  } catch (error: any) {
    ElMessage.error('加载学生列表失败')
  }
}

const handleAssign = (bed: any) => {
  assignForm.bedNo = bed.bedNumber
  assignForm.studentId = null
  assignDialogVisible.value = true
}

const confirmAssign = async () => {
  if (!assignForm.studentId) {
    ElMessage.warning('请选择学生')
    return
  }

  try {
    const student = availableStudents.value.find(s => s.id === assignForm.studentId)
    const genderType = dormitoryInfo.value?.genderType

    if (genderType === 1 && student?.gender === 2) {
      ElMessage.error('女生不能分配到男生宿舍')
      return
    }
    if (genderType === 2 && student?.gender === 1) {
      ElMessage.error('男生不能分配到女生宿舍')
      return
    }

    await assignBed(props.dormitoryId!, {
      bedNo: assignForm.bedNo,
      studentId: assignForm.studentId
    })

    ElMessage.success('床位分配成功')
    assignDialogVisible.value = false
    await loadDormitoryInfo()
    emit('success')
  } catch (error: any) {
    const message = error.response?.data?.message || '床位分配失败'
    ElMessage.error(message)
  }
}

const handleRelease = async (bed: any) => {
  try {
    await ElMessageBox.confirm(
      `确定要释放 ${bed.bedNumber}号床位吗？学生 "${bed.studentName}" 将被移出宿舍。`,
      '释放床位',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await releaseBed(props.dormitoryId!, bed.bedNumber)
    ElMessage.success('床位释放成功')
    await loadDormitoryInfo()
    emit('success')
  } catch (error: any) {
    if (error !== 'cancel') {
      const message = error.response?.data?.message || '床位释放失败'
      ElMessage.error(message)
    }
  }
}

onMounted(() => {
  loadDormitoryInfo()
})
</script>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
</style>
