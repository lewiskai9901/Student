<template>
  <div class="flex h-full min-h-screen bg-gray-50">
    <!-- Left Panel: Department Dormitories -->
    <div class="w-96 flex-shrink-0 border-r border-gray-200 bg-white">
      <!-- Header -->
      <div class="border-b border-gray-100 p-4">
        <div class="flex items-center justify-between">
          <h2 class="flex items-center gap-2 text-lg font-semibold text-gray-900">
            <Building2 class="h-5 w-5 text-teal-600" />
            组织宿舍
          </h2>
          <button
            @click="refreshData"
            :disabled="loading"
            class="rounded-lg p-1.5 text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-600"
          >
            <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': loading }" />
          </button>
        </div>
        <!-- Search -->
        <div class="mt-3">
          <div class="relative">
            <Search class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
            <input
              v-model="dormitorySearchQuery"
              type="text"
              placeholder="搜索宿舍..."
              class="w-full rounded-lg border border-gray-200 py-2 pl-9 pr-3 text-sm focus:border-teal-500 focus:outline-none focus:ring-1 focus:ring-teal-500"
            />
          </div>
        </div>
        <!-- Stats -->
        <div class="mt-3 grid grid-cols-2 gap-2">
          <div class="rounded-lg bg-teal-50 px-3 py-2">
            <div class="text-xs text-teal-600">组织宿舍</div>
            <div class="text-lg font-bold text-teal-700">{{ statistics.totalDormitories }}</div>
          </div>
          <div class="rounded-lg bg-blue-50 px-3 py-2">
            <div class="text-xs text-blue-600">空床位</div>
            <div class="text-lg font-bold text-blue-700">{{ statistics.totalBeds - statistics.occupiedBeds }}</div>
          </div>
        </div>
      </div>

      <!-- Dormitory List -->
      <div class="h-[calc(100vh-220px)] overflow-y-auto">
        <div v-if="loading" class="flex items-center justify-center py-12">
          <Loader2 class="h-6 w-6 animate-spin text-teal-500" />
        </div>
        <div v-else-if="filteredDormitories.length === 0" class="px-4 py-12 text-center text-gray-400">
          <DoorOpen class="mx-auto h-10 w-10" />
          <p class="mt-2 text-sm">暂无分配给组织的宿舍</p>
          <p class="mt-1 text-xs">请联系宿舍管理员分配</p>
        </div>
        <div v-else class="divide-y divide-gray-100">
          <div
            v-for="dormitory in filteredDormitories"
            :key="dormitory.id"
            class="cursor-pointer px-4 py-3 transition-colors"
            :class="isSelectedDormitory(dormitory.id) ? 'bg-teal-50' : 'hover:bg-gray-50'"
            @click="toggleDormitorySelection(dormitory)"
          >
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-3">
                <!-- Checkbox -->
                <div
                  class="flex h-5 w-5 items-center justify-center rounded border-2 transition-colors"
                  :class="isSelectedDormitory(dormitory.id) ? 'border-teal-600 bg-teal-600' : 'border-gray-300'"
                >
                  <Check v-if="isSelectedDormitory(dormitory.id)" class="h-3 w-3 text-white" />
                </div>
                <div>
                  <div class="text-sm font-medium text-gray-900">
                    {{ dormitory.buildingName }} - {{ dormitory.dormitoryNo }}
                  </div>
                  <div class="text-xs text-gray-500">
                    {{ dormitory.floorNumber }}层 · {{ dormitory.occupiedBeds || 0 }}/{{ dormitory.bedCapacity }}床
                  </div>
                </div>
              </div>
              <div class="flex items-center gap-2">
                <!-- Gender Badge -->
                <span
                  class="rounded px-1.5 py-0.5 text-[10px] font-medium"
                  :class="getGenderTypeClass(dormitory.genderType)"
                >
                  {{ getGenderTypeLabel(dormitory.genderType) }}
                </span>
                <!-- Status Indicator -->
                <div
                  class="h-2 w-2 rounded-full"
                  :class="getDormitoryStatusClass(dormitory)"
                ></div>
              </div>
            </div>
            <!-- Assigned Class Info -->
            <div v-if="getDormitoryBindings(dormitory.id).length > 0" class="mt-2 flex items-center gap-1">
              <GraduationCap class="h-3 w-3 text-gray-400" />
              <span class="text-xs text-gray-500">
                已分配: {{ getDormitoryBindings(dormitory.id).map(b => b.className).join(', ') }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Right Panel: Class Assignment -->
    <div class="flex-1 overflow-auto">
      <!-- Header -->
      <div class="border-b border-gray-200 bg-white px-6 py-4">
        <div class="flex items-center justify-between">
          <div>
            <h2 class="text-xl font-semibold text-gray-900">班级宿舍分配</h2>
            <p class="mt-1 text-sm text-gray-500">
              将组织宿舍分配给各班级使用
            </p>
          </div>
          <div class="flex items-center gap-2">
            <span class="text-sm text-gray-500">
              已选择 {{ selectedDormitories.length }} 间宿舍
            </span>
          </div>
        </div>
      </div>

      <!-- Content -->
      <div class="p-6">
        <!-- Class List -->
        <div class="mb-6">
          <h3 class="mb-4 flex items-center gap-2 text-sm font-medium text-gray-700">
            <GraduationCap class="h-4 w-4" />
            组织班级
          </h3>

          <div v-if="classLoading" class="flex items-center justify-center py-8">
            <Loader2 class="h-6 w-6 animate-spin text-teal-500" />
          </div>

          <div v-else-if="classes.length === 0" class="rounded-lg border-2 border-dashed border-gray-200 py-8 text-center">
            <GraduationCap class="mx-auto h-10 w-10 text-gray-400" />
            <p class="mt-2 text-sm text-gray-500">暂无班级</p>
          </div>

          <div v-else class="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
            <div
              v-for="classItem in classes"
              :key="classItem.id"
              class="rounded-xl border border-gray-200 bg-white p-4 transition-all hover:shadow-md"
            >
              <div class="flex items-start justify-between">
                <div>
                  <h4 class="font-medium text-gray-900">{{ classItem.className }}</h4>
                  <p class="mt-0.5 text-xs text-gray-500">{{ classItem.classCode }}</p>
                </div>
                <span class="rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-600">
                  {{ classItem.studentCount || 0 }}人
                </span>
              </div>

              <!-- Assigned Dormitories -->
              <div class="mt-3">
                <div class="flex items-center gap-1 text-xs text-gray-500">
                  <DoorOpen class="h-3 w-3" />
                  已分配宿舍:
                </div>
                <div v-if="getClassBindings(classItem.id).length > 0" class="mt-2 flex flex-wrap gap-1">
                  <span
                    v-for="binding in getClassBindings(classItem.id)"
                    :key="binding.id"
                    class="inline-flex items-center gap-1 rounded-full bg-teal-100 px-2 py-0.5 text-xs text-teal-700"
                  >
                    {{ binding.buildingName }}-{{ binding.dormitoryNo }}
                    <button
                      @click.stop="handleUnassign(binding)"
                      class="ml-0.5 rounded-full p-0.5 hover:bg-teal-200"
                    >
                      <X class="h-3 w-3" />
                    </button>
                  </span>
                </div>
                <div v-else class="mt-2 text-xs text-gray-400">
                  未分配宿舍
                </div>
              </div>

              <!-- Action Button -->
              <button
                @click="handleAssignToClass(classItem)"
                :disabled="selectedDormitories.length === 0"
                class="mt-3 w-full rounded-lg border border-teal-600 py-2 text-sm font-medium text-teal-600 transition-colors hover:bg-teal-50 disabled:cursor-not-allowed disabled:border-gray-300 disabled:text-gray-400"
              >
                <Plus class="mr-1 inline-block h-4 w-4" />
                分配所选宿舍
              </button>
            </div>
          </div>
        </div>

        <!-- Binding Summary -->
        <div class="mt-8">
          <h3 class="mb-4 flex items-center gap-2 text-sm font-medium text-gray-700">
            <LinkIcon class="h-4 w-4" />
            分配记录
          </h3>

          <div v-if="bindings.length === 0" class="rounded-lg border-2 border-dashed border-gray-200 py-8 text-center">
            <LinkIcon class="mx-auto h-10 w-10 text-gray-400" />
            <p class="mt-2 text-sm text-gray-500">暂无分配记录</p>
          </div>

          <div v-else class="overflow-hidden rounded-lg border border-gray-200 bg-white">
            <table class="w-full">
              <thead>
                <tr class="border-b border-gray-100 bg-gray-50/50">
                  <th class="px-4 py-3 text-left text-xs font-semibold uppercase text-gray-500">班级</th>
                  <th class="px-4 py-3 text-left text-xs font-semibold uppercase text-gray-500">宿舍</th>
                  <th class="px-4 py-3 text-center text-xs font-semibold uppercase text-gray-500">床位情况</th>
                  <th class="px-4 py-3 text-left text-xs font-semibold uppercase text-gray-500">分配时间</th>
                  <th class="px-4 py-3 text-center text-xs font-semibold uppercase text-gray-500">操作</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-100">
                <tr
                  v-for="binding in bindings"
                  :key="binding.id"
                  class="transition-colors hover:bg-gray-50/50"
                >
                  <td class="px-4 py-3">
                    <div class="font-medium text-gray-900">{{ binding.className }}</div>
                    <div class="text-xs text-gray-500">{{ binding.classCode }}</div>
                  </td>
                  <td class="px-4 py-3">
                    <div class="font-medium text-gray-900">{{ binding.buildingName }}-{{ binding.dormitoryNo }}</div>
                    <div class="text-xs text-gray-500">{{ binding.floorNumber }}层</div>
                  </td>
                  <td class="px-4 py-3 text-center">
                    <div class="flex items-center justify-center gap-2">
                      <div class="h-2 w-16 overflow-hidden rounded-full bg-gray-200">
                        <div
                          class="h-full rounded-full bg-teal-500"
                          :style="{ width: `${(binding.occupiedBeds / binding.bedCapacity) * 100}%` }"
                        ></div>
                      </div>
                      <span class="text-sm text-gray-600">{{ binding.occupiedBeds || 0 }}/{{ binding.bedCapacity }}</span>
                    </div>
                  </td>
                  <td class="px-4 py-3 text-sm text-gray-600">
                    {{ formatDate(binding.createdTime) }}
                  </td>
                  <td class="px-4 py-3 text-center">
                    <button
                      @click="handleUnassign(binding)"
                      class="rounded-lg p-1.5 text-red-500 transition-colors hover:bg-red-50"
                      title="取消分配"
                    >
                      <Trash2 class="h-4 w-4" />
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <!-- Confirm Dialog -->
    <el-dialog
      v-model="confirmDialogVisible"
      title="确认分配"
      width="400"
      :close-on-click-modal="false"
    >
      <div class="text-gray-600">
        <p>确定将以下 {{ selectedDormitories.length }} 间宿舍分配给 <strong>{{ targetClass?.className }}</strong> 吗？</p>
        <ul class="mt-3 list-inside list-disc text-sm text-gray-500">
          <li v-for="d in selectedDormitories" :key="d.id">
            {{ d.buildingName }} - {{ d.dormitoryNo }}
          </li>
        </ul>
      </div>
      <template #footer>
        <div class="flex justify-end gap-2">
          <el-button @click="confirmDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="assigning" @click="confirmAssign">
            确认分配
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Building2,
  RefreshCw,
  Search,
  Check,
  DoorOpen,
  GraduationCap,
  Plus,
  X,
  Trash2,
  Link as LinkIcon,
  Loader2
} from 'lucide-vue-next'
import {
  getMyDepartmentDormitories,
  getMyDepartmentClasses,
  getBindings,
  batchAssignDormitoriesToClass,
  unassignDormitoryFromClass,
  getDepartmentDormitoryStatistics,
  type ClassDormitoryBinding,
  type ClassInfo,
  type DepartmentDormitoryStatistics
} from '@/api/departmentDormitory'
import type { Dormitory } from '@/types/dormitory'

// State
const loading = ref(false)
const classLoading = ref(false)
const assigning = ref(false)
const dormitories = ref<Dormitory[]>([])
const classes = ref<ClassInfo[]>([])
const bindings = ref<ClassDormitoryBinding[]>([])
const statistics = ref<DepartmentDormitoryStatistics>({
  totalDormitories: 0,
  totalBeds: 0,
  occupiedBeds: 0,
  assignedDormitories: 0,
  totalClasses: 0
})
const dormitorySearchQuery = ref('')
const selectedDormitories = ref<Dormitory[]>([])
const confirmDialogVisible = ref(false)
const targetClass = ref<ClassInfo | null>(null)

// Computed
const filteredDormitories = computed(() => {
  if (!dormitorySearchQuery.value) return dormitories.value
  const query = dormitorySearchQuery.value.toLowerCase()
  return dormitories.value.filter(d =>
    d.dormitoryNo?.toLowerCase().includes(query) ||
    d.buildingName?.toLowerCase().includes(query)
  )
})

// Methods
const loadData = async () => {
  loading.value = true
  classLoading.value = true
  try {
    const [dormitoriesData, classesData, bindingsData, statsData] = await Promise.all([
      getMyDepartmentDormitories(),
      getMyDepartmentClasses(),
      getBindings(),
      getDepartmentDormitoryStatistics()
    ])
    dormitories.value = dormitoriesData
    classes.value = classesData
    bindings.value = bindingsData
    statistics.value = statsData
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '加载数据失败')
  } finally {
    loading.value = false
    classLoading.value = false
  }
}

const refreshData = () => {
  selectedDormitories.value = []
  loadData()
}

const isSelectedDormitory = (id: number | string) => {
  return selectedDormitories.value.some(d => d.id === id)
}

const toggleDormitorySelection = (dormitory: Dormitory) => {
  const index = selectedDormitories.value.findIndex(d => d.id === dormitory.id)
  if (index >= 0) {
    selectedDormitories.value.splice(index, 1)
  } else {
    selectedDormitories.value.push(dormitory)
  }
}

const getDormitoryBindings = (dormitoryId: number | string) => {
  return bindings.value.filter(b => b.dormitoryId === dormitoryId)
}

const getClassBindings = (orgUnitId: number | string) => {
  return bindings.value.filter(b => b.orgUnitId === orgUnitId)
}

const handleAssignToClass = (classItem: ClassInfo) => {
  if (selectedDormitories.value.length === 0) {
    ElMessage.warning('请先选择要分配的宿舍')
    return
  }
  targetClass.value = classItem
  confirmDialogVisible.value = true
}

const confirmAssign = async () => {
  if (!targetClass.value) return

  assigning.value = true
  try {
    const count = await batchAssignDormitoriesToClass({
      orgUnitId: Number(targetClass.value.id),
      dormitoryIds: selectedDormitories.value.map(d => Number(d.id))
    })
    ElMessage.success(`成功分配 ${count} 间宿舍`)
    confirmDialogVisible.value = false
    selectedDormitories.value = []
    loadData()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '分配失败')
  } finally {
    assigning.value = false
  }
}

const handleUnassign = async (binding: ClassDormitoryBinding) => {
  try {
    await ElMessageBox.confirm(
      `确定要取消 ${binding.className} 与 ${binding.buildingName}-${binding.dormitoryNo} 的分配关系吗？`,
      '确认取消',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await unassignDormitoryFromClass(binding.dormitoryId, binding.orgUnitId)
    ElMessage.success('已取消分配')
    loadData()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '操作失败')
    }
  }
}

// Helpers
const getGenderTypeClass = (type?: number) => {
  if (type === 1) return 'bg-blue-100 text-blue-700'
  if (type === 2) return 'bg-pink-100 text-pink-700'
  return 'bg-gray-100 text-gray-600'
}

const getGenderTypeLabel = (type?: number) => {
  if (type === 1) return '男'
  if (type === 2) return '女'
  return '混'
}

const getDormitoryStatusClass = (dormitory: Dormitory) => {
  const rate = (dormitory.occupiedBeds || 0) / (dormitory.bedCapacity || 1)
  if (rate === 0) return 'bg-gray-300'
  if (rate < 1) return 'bg-blue-500'
  return 'bg-emerald-500'
}

const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  try {
    const date = new Date(dateStr)
    return date.toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    })
  } catch {
    return dateStr
  }
}

// Lifecycle
onMounted(() => {
  loadData()
})
</script>
