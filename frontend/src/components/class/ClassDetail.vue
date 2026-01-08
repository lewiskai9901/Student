<template>
  <div class="p-6">
    <!-- Loading -->
    <div v-if="loading" class="flex items-center justify-center py-12">
      <div class="h-8 w-8 animate-spin rounded-full border-2 border-blue-600 border-t-transparent"></div>
    </div>

    <div v-else-if="classInfo" class="space-y-6">
      <!-- 基本信息 -->
      <div class="rounded-lg border border-gray-200 bg-white">
        <div class="border-b border-gray-200 px-4 py-3">
          <h3 class="text-sm font-semibold text-gray-900">班级基本信息</h3>
        </div>
        <div class="p-4">
          <div class="grid grid-cols-3 gap-x-8 gap-y-4">
            <div><span class="text-sm text-gray-500">班级名称:</span><span class="ml-2 text-sm text-gray-900">{{ classInfo.className }}</span></div>
            <div><span class="text-sm text-gray-500">班级编码:</span><span class="ml-2 text-sm text-gray-900">{{ classInfo.classCode }}</span></div>
            <div><span class="text-sm text-gray-500">年级:</span><span class="ml-2 text-sm text-gray-900">{{ classInfo.grade }}</span></div>
            <div><span class="text-sm text-gray-500">所属部门:</span><span class="ml-2 text-sm text-gray-900">{{ classInfo.departmentName }}</span></div>
            <div><span class="text-sm text-gray-500">专业:</span><span class="ml-2 text-sm text-gray-900">{{ classInfo.majorName }}</span></div>
            <div><span class="text-sm text-gray-500">班主任:</span><span class="ml-2 text-sm text-gray-900">{{ classInfo.teacherName || '未分配' }}</span></div>
            <div><span class="text-sm text-gray-500">学生人数:</span><span class="ml-2 text-sm text-gray-900">{{ classInfo.studentCount }}人</span></div>
            <div>
              <span class="text-sm text-gray-500">状态:</span>
              <span :class="['ml-2 inline-flex rounded px-2 py-0.5 text-xs font-medium', classInfo.status === 1 ? 'bg-green-50 text-green-700' : classInfo.status === 2 ? 'bg-amber-50 text-amber-700' : 'bg-gray-100 text-gray-700']">
                {{ getStatusText(classInfo.status) }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 教室信息 -->
      <div class="rounded-lg border border-gray-200 bg-white">
        <div class="flex items-center justify-between border-b border-gray-200 px-4 py-3">
          <h3 class="text-sm font-semibold text-gray-900">教室信息</h3>
          <button v-if="!classroomInfo" class="inline-flex h-7 items-center gap-1 rounded bg-blue-600 px-2.5 text-xs text-white hover:bg-blue-700" @click="showAssignClassroomDialog">
            <Plus class="h-3.5 w-3.5" />分配教室
          </button>
          <button v-else class="inline-flex h-7 items-center gap-1 rounded bg-red-600 px-2.5 text-xs text-white hover:bg-red-700" @click="handleRemoveClassroom">
            <X class="h-3.5 w-3.5" />取消分配
          </button>
        </div>
        <div class="p-4">
          <div v-if="classroomInfo" class="grid grid-cols-3 gap-x-8 gap-y-4">
            <div><span class="text-sm text-gray-500">教室名称:</span><span class="ml-2 text-sm text-gray-900">{{ classroomInfo.classroomName }}</span></div>
            <div><span class="text-sm text-gray-500">教室编号:</span><span class="ml-2 text-sm text-gray-900">{{ classroomInfo.classroomCode }}</span></div>
            <div><span class="text-sm text-gray-500">楼层:</span><span class="ml-2 text-sm text-gray-900">{{ classroomInfo.floor }}楼</span></div>
            <div><span class="text-sm text-gray-500">房间号:</span><span class="ml-2 text-sm text-gray-900">{{ classroomInfo.roomNumber }}</span></div>
            <div><span class="text-sm text-gray-500">容纳人数:</span><span class="ml-2 text-sm text-gray-900">{{ classroomInfo.capacity }}人</span></div>
            <div><span class="text-sm text-gray-500">教室类型:</span><span class="ml-2 text-sm text-gray-900">{{ classroomInfo.classroomType || '普通教室' }}</span></div>
          </div>
          <div v-else class="flex flex-col items-center justify-center py-8 text-gray-400">
            <Building2 class="mb-2 h-10 w-10" />
            <span class="text-sm">未分配教室</span>
          </div>
        </div>
      </div>

      <!-- 宿舍信息 -->
      <div class="rounded-lg border border-gray-200 bg-white">
        <div class="flex items-center justify-between border-b border-gray-200 px-4 py-3">
          <h3 class="text-sm font-semibold text-gray-900">宿舍信息</h3>
          <button class="inline-flex h-7 items-center gap-1 rounded bg-blue-600 px-2.5 text-xs text-white hover:bg-blue-700" @click="showAddDormitoryDialog">
            <Plus class="h-3.5 w-3.5" />添加宿舍
          </button>
        </div>
        <div class="p-4">
          <div v-if="dormitories && dormitories.length > 0" class="overflow-hidden rounded-lg border border-gray-200">
            <table class="min-w-full divide-y divide-gray-200">
              <thead class="bg-gray-50">
                <tr>
                  <th class="px-3 py-2 text-left text-xs font-medium text-gray-500">楼号</th>
                  <th class="px-3 py-2 text-left text-xs font-medium text-gray-500">楼宇名称</th>
                  <th class="px-3 py-2 text-left text-xs font-medium text-gray-500">房间号</th>
                  <th class="px-3 py-2 text-left text-xs font-medium text-gray-500">楼层</th>
                  <th class="px-3 py-2 text-left text-xs font-medium text-gray-500">容量</th>
                  <th class="px-3 py-2 text-left text-xs font-medium text-gray-500">已分配</th>
                  <th class="px-3 py-2 text-left text-xs font-medium text-gray-500">已入住</th>
                  <th class="px-3 py-2 text-left text-xs font-medium text-gray-500">性别</th>
                  <th class="px-3 py-2 text-left text-xs font-medium text-gray-500">状态</th>
                  <th class="px-3 py-2 text-center text-xs font-medium text-gray-500">操作</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-200 bg-white">
                <tr v-for="row in dormitories" :key="row.id" class="hover:bg-gray-50">
                  <td class="whitespace-nowrap px-3 py-2 text-sm text-gray-600">{{ row.buildingNo || '-' }}</td>
                  <td class="whitespace-nowrap px-3 py-2 text-sm text-gray-900">{{ row.buildingName }}</td>
                  <td class="whitespace-nowrap px-3 py-2 text-sm text-gray-600">{{ row.dormitoryNo }}</td>
                  <td class="whitespace-nowrap px-3 py-2 text-sm text-gray-600">{{ row.floorNumber }}楼</td>
                  <td class="whitespace-nowrap px-3 py-2 text-sm text-gray-600">{{ row.bedCapacity }}人间</td>
                  <td class="whitespace-nowrap px-3 py-2 text-sm text-gray-600">{{ row.allocatedBeds }}个</td>
                  <td class="whitespace-nowrap px-3 py-2 text-sm text-gray-600">{{ row.occupiedBeds }}个</td>
                  <td class="whitespace-nowrap px-3 py-2">
                    <span :class="['rounded px-1.5 py-0.5 text-xs font-medium', row.genderType === 1 ? 'bg-blue-50 text-blue-700' : row.genderType === 2 ? 'bg-pink-50 text-pink-700' : 'bg-gray-100 text-gray-700']">
                      {{ row.genderType === 1 ? '男' : row.genderType === 2 ? '女' : '混合' }}
                    </span>
                  </td>
                  <td class="whitespace-nowrap px-3 py-2">
                    <span :class="['rounded px-1.5 py-0.5 text-xs font-medium', row.status === 1 ? 'bg-green-50 text-green-700' : 'bg-gray-100 text-gray-600']">
                      {{ row.status === 1 ? '正常' : row.status === 2 ? '维修' : '停用' }}
                    </span>
                  </td>
                  <td class="whitespace-nowrap px-3 py-2 text-center">
                    <button class="text-xs text-red-600 hover:text-red-800" @click="handleRemoveDormitory(row)">移除</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="flex flex-col items-center justify-center py-8 text-gray-400">
            <Home class="mb-2 h-10 w-10" />
            <span class="text-sm">未分配宿舍</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="flex flex-col items-center justify-center py-16 text-gray-400">
      <AlertCircle class="mb-2 h-12 w-12" />
      <span class="text-sm">班级信息不存在</span>
    </div>

    <!-- 操作按钮 -->
    <div class="mt-6 flex justify-end border-t border-gray-200 pt-4">
      <button class="h-9 rounded-lg border border-gray-300 bg-white px-4 text-sm font-medium text-gray-700 hover:bg-gray-50" @click="$emit('close')">关闭</button>
    </div>

    <!-- 教室分配对话框 -->
    <Teleport to="body">
      <div v-if="classroomDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="fixed inset-0 bg-black/50" @click="classroomDialogVisible = false"></div>
        <div class="relative w-full max-w-md rounded-lg bg-white shadow-xl">
          <div class="flex items-center justify-between border-b border-gray-200 px-4 py-3">
            <h3 class="text-base font-semibold text-gray-900">分配教室</h3>
            <button class="text-gray-400 hover:text-gray-600" @click="classroomDialogVisible = false"><X class="h-5 w-5" /></button>
          </div>
          <div class="p-4">
            <label class="mb-1.5 block text-sm font-medium text-gray-700">选择教室</label>
            <select v-model="classroomForm.classroomId" class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none">
              <option :value="null">请选择教室</option>
              <option v-for="item in classroomList" :key="item.id" :value="item.id">{{ item.classroomName }} ({{ item.classroomCode }}) - {{ item.capacity }}人</option>
            </select>
          </div>
          <div class="flex justify-end gap-3 border-t border-gray-200 px-4 py-3">
            <button class="h-9 rounded-lg border border-gray-300 bg-white px-4 text-sm text-gray-700 hover:bg-gray-50" @click="classroomDialogVisible = false">取消</button>
            <button class="h-9 rounded-lg bg-blue-600 px-4 text-sm text-white hover:bg-blue-700 disabled:opacity-50" :disabled="submitting" @click="handleAssignClassroom">
              <span v-if="submitting" class="mr-1.5 inline-block h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent"></span>确定
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 宿舍分配对话框 -->
    <ClassDormitoryAssignmentDialog
      v-model:visible="dormitoryDialogVisible"
      :class-id="classId!"
      :class-name="classInfo?.className || ''"
      :department-id="classInfo?.departmentId || 0"
      @success="loadDormitories"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, X, Building2, Home, AlertCircle } from 'lucide-vue-next'
import { getClassDetail, getClassClassroom, getClassDormitories, assignClassroom, removeClassroom, removeDormitory, getClassroomList } from '@/api/class'
import ClassDormitoryAssignmentDialog from './ClassDormitoryAssignmentDialog.vue'

interface Props { classId: number | null }
const props = defineProps<Props>()
const emit = defineEmits<{ close: [] }>()

const loading = ref(false)
const classInfo = ref<any>(null)
const classroomInfo = ref<any>(null)
const dormitories = ref<any[]>([])
const classroomDialogVisible = ref(false)
const dormitoryDialogVisible = ref(false)
const submitting = ref(false)
const classroomList = ref<any[]>([])
const classroomForm = ref({ classroomId: null as number | null })

const getStatusText = (status: number) => ({ 1: '正常', 2: '停招', 3: '毕业' }[status] || '未知')

const loadClassDetail = async () => {
  if (!props.classId) return
  loading.value = true
  try {
    classInfo.value = await getClassDetail(props.classId)
    await loadClassroom()
    await loadDormitories()
  } catch (error) {
    console.error('加载班级详情失败:', error)
    ElMessage.error('加载班级详情失败')
  } finally {
    loading.value = false
  }
}

const loadClassroom = async () => {
  if (!props.classId) return
  try { classroomInfo.value = await getClassClassroom(props.classId) } catch (error) { console.error('加载教室信息失败:', error) }
}

const loadDormitories = async () => {
  if (!props.classId) return
  try { dormitories.value = await getClassDormitories(props.classId) || [] } catch (error) { console.error('加载宿舍信息失败:', error) }
}

const showAssignClassroomDialog = async () => {
  try {
    classroomList.value = await getClassroomList()
    classroomForm.value.classroomId = null
    classroomDialogVisible.value = true
  } catch (error) { ElMessage.error('加载教室列表失败') }
}

const handleAssignClassroom = async () => {
  if (!classroomForm.value.classroomId) { ElMessage.warning('请选择教室'); return }
  submitting.value = true
  try {
    await assignClassroom(props.classId!, classroomForm.value.classroomId)
    ElMessage.success('教室分配成功')
    classroomDialogVisible.value = false
    await loadClassroom()
  } catch (error: any) { ElMessage.error(error.message || '分配教室失败') } finally { submitting.value = false }
}

const handleRemoveClassroom = async () => {
  try {
    await ElMessageBox.confirm('确定要取消教室分配吗?', '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    await removeClassroom(props.classId!)
    ElMessage.success('已取消教室分配')
    await loadClassroom()
  } catch (error: any) { if (error !== 'cancel') ElMessage.error(error.message || '取消教室分配失败') }
}

const showAddDormitoryDialog = () => {
  if (!classInfo.value?.departmentId) {
    ElMessage.warning('班级所属部门信息缺失')
    return
  }
  dormitoryDialogVisible.value = true
}

const handleRemoveDormitory = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定要移除宿舍 ${row.dormitoryNo} 吗?`, '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    await removeDormitory(props.classId!, row.dormitoryId)
    ElMessage.success('宿舍移除成功')
    await loadDormitories()
  } catch (error: any) { if (error !== 'cancel') ElMessage.error(error.message || '移除宿舍失败') }
}

onMounted(() => { loadClassDetail() })
</script>
