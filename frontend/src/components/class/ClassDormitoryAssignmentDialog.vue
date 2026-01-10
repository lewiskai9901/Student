<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="visible" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="fixed inset-0 bg-black/50" @click="handleClose"></div>
        <div class="relative flex max-h-[90vh] w-full max-w-5xl flex-col rounded-lg bg-white shadow-xl">
          <!-- 头部 -->
          <div class="flex items-center justify-between border-b border-gray-200 px-6 py-4">
            <div>
              <h3 class="text-lg font-semibold text-gray-900">{{ className }} - 宿舍分配</h3>
              <p class="mt-0.5 text-sm text-gray-500">点击楼层标签可整层分配，点击单个房间可单独分配/取消</p>
            </div>
            <button @click="handleClose" class="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-600">
              <X class="h-5 w-5" />
            </button>
          </div>

          <!-- 楼宇选择器和图例 -->
          <div class="border-b border-gray-200 bg-gray-50 px-6 py-3">
            <div class="flex flex-wrap items-center gap-4">
              <div class="flex items-center gap-2">
                <label class="text-sm font-medium text-gray-700">选择楼宇：</label>
                <select
                  v-model="selectedBuildingId"
                  class="h-9 w-48 rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                  @change="loadDormitoriesByBuilding"
                >
                  <option :value="null">请选择楼宇</option>
                  <option v-for="building in buildingOptions" :key="building.id" :value="building.id">
                    {{ building.buildingName }}
                  </option>
                </select>
              </div>
              <div class="flex items-center gap-3 text-xs">
                <div class="flex items-center gap-1.5 rounded-full border border-gray-200 bg-white px-2.5 py-1">
                  <div class="h-2.5 w-2.5 rounded-full border-2 border-gray-300 bg-white"></div>
                  <span class="text-gray-600">空</span>
                </div>
                <div class="flex items-center gap-1.5 rounded-full bg-emerald-100 px-2.5 py-1">
                  <div class="h-2.5 w-2.5 rounded-full bg-emerald-500"></div>
                  <span class="font-medium text-emerald-700">本班</span>
                </div>
                <div class="flex items-center gap-1.5 rounded-full bg-orange-100 px-2.5 py-1">
                  <div class="h-2.5 w-2.5 rounded-full bg-orange-500"></div>
                  <span class="font-medium text-orange-700">他班</span>
                </div>
                <div class="flex items-center gap-1.5 rounded-full bg-purple-100 px-2.5 py-1">
                  <div class="flex h-2.5 w-2.5 items-center justify-center rounded-full bg-purple-500">
                    <div class="h-1 w-1 rounded-full bg-white"></div>
                  </div>
                  <span class="font-medium text-purple-700">混合</span>
                </div>
              </div>
              <div class="ml-auto text-sm text-gray-500">
                共 {{ totalRooms }} 间 | 已分配本班 {{ assignedToCurrentClass }} 间
              </div>
            </div>
          </div>

          <!-- 楼层内容 -->
          <div class="flex-1 overflow-y-auto p-6">
            <div v-if="loading" class="py-20 text-center">
              <Loader2 class="mx-auto h-8 w-8 animate-spin text-blue-500" />
              <p class="mt-2 text-sm text-gray-500">加载中...</p>
            </div>

            <div v-else-if="!selectedBuildingId" class="py-20 text-center">
              <Building2 class="mx-auto h-12 w-12 text-gray-300" />
              <p class="mt-2 text-sm text-gray-500">请选择楼宇查看宿舍</p>
            </div>

            <div v-else-if="floorData.length > 0" class="space-y-2">
              <div
                v-for="floor in floorData"
                :key="floor.floor"
                class="overflow-hidden rounded-lg border border-gray-200 bg-white"
              >
                <!-- 楼层标题（可点击整层分配） -->
                <div
                  class="flex cursor-pointer items-center justify-between bg-gradient-to-r from-gray-50 to-white px-4 py-2 transition-colors hover:from-blue-50 hover:to-blue-25"
                  @click="handleFloorClick(floor.floor)"
                >
                  <div class="flex items-center gap-3">
                    <div class="flex h-8 w-8 items-center justify-center rounded-lg bg-blue-100 text-sm font-bold text-blue-700">
                      {{ floor.floor }}F
                    </div>
                    <span class="text-sm font-medium text-gray-700">
                      {{ floor.rooms.length }} 间房间
                    </span>
                    <span class="text-xs text-gray-400">|</span>
                    <span class="text-xs text-gray-500">
                      本班 {{ floor.currentClassCount }} 间
                    </span>
                  </div>
                  <div class="flex items-center gap-2">
                    <button
                      @click.stop="handleFloorClick(floor.floor)"
                      class="inline-flex items-center gap-1 rounded-lg border border-blue-200 bg-blue-50 px-3 py-1 text-xs font-medium text-blue-600 transition-colors hover:bg-blue-100"
                    >
                      <Layers class="h-3.5 w-3.5" />
                      整层分配
                    </button>
                  </div>
                </div>

                <!-- 房间列表 -->
                <div class="flex flex-wrap gap-2 border-t border-gray-100 p-3">
                  <div
                    v-for="room in floor.rooms"
                    :key="room.id"
                    class="group relative min-w-[52px] cursor-pointer rounded-lg px-2.5 py-2 text-center transition-all hover:scale-105 hover:shadow-lg"
                    :class="getRoomClass(room)"
                    @click="handleRoomClick(room)"
                  >
                    <!-- 状态指示点 -->
                    <div class="absolute -right-1 -top-1 h-2.5 w-2.5 rounded-full shadow-sm" :class="getStatusDotClass(room)"></div>
                    <div class="text-sm font-bold" :class="getRoomTextClass(room)">
                      {{ room.dormitoryNo }}
                    </div>
                    <!-- 悬浮提示 -->
                    <div class="pointer-events-none absolute bottom-full left-1/2 z-10 mb-2 hidden -translate-x-1/2 whitespace-nowrap rounded-lg bg-gray-900 px-3 py-2 text-xs text-white shadow-lg group-hover:block">
                      <div class="font-medium">{{ room.dormitoryNo }}</div>
                      <div class="mt-1 text-gray-300">
                        {{ room.bedCapacity || room.bedCount }}人间 · {{ room.occupiedBeds || 0 }}人入住
                      </div>
                      <div v-if="isAssignedToCurrentClass(room) && isAssignedToOtherClass(room)" class="mt-1 text-purple-400">
                        混合宿舍 (本班 + {{ getOtherClassNames(room) }})
                      </div>
                      <div v-else-if="isAssignedToCurrentClass(room)" class="mt-1 text-emerald-400">
                        已分配给本班
                      </div>
                      <div v-else-if="isAssignedToOtherClass(room)" class="mt-1 text-orange-400">
                        已分配给: {{ getOtherClassNames(room) }}
                      </div>
                      <div class="absolute left-1/2 top-full -translate-x-1/2 border-4 border-transparent border-t-gray-900"></div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div v-else class="py-20 text-center">
              <Building2 class="mx-auto h-12 w-12 text-gray-300" />
              <p class="mt-2 text-sm text-gray-500">该楼宇暂无宿舍房间</p>
            </div>
          </div>

          <!-- 底部 -->
          <div class="flex items-center justify-between border-t border-gray-200 px-6 py-4">
            <div class="text-sm text-gray-500">
              <span>点击房间进行分配或取消分配</span>
            </div>
            <div class="flex items-center gap-3">
              <button
                @click="handleClose"
                class="h-9 rounded-lg border border-gray-300 px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
              >
                关闭
              </button>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>

  <!-- 确认分配对话框 -->
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="confirmDialogVisible" class="fixed inset-0 z-[60] flex items-center justify-center">
        <div class="fixed inset-0 bg-black/50" @click="confirmDialogVisible = false"></div>
        <div class="relative w-full max-w-md rounded-lg bg-white p-6 shadow-xl">
          <div class="mb-4 flex items-center gap-3">
            <div class="flex h-10 w-10 items-center justify-center rounded-full" :class="confirmAction === 'add' ? 'bg-blue-100' : 'bg-red-100'">
              <Home class="h-5 w-5" :class="confirmAction === 'add' ? 'text-blue-600' : 'text-red-600'" />
            </div>
            <div>
              <h4 class="text-lg font-semibold text-gray-900">{{ confirmAction === 'add' ? '确认分配' : '确认取消分配' }}</h4>
              <p class="text-sm text-gray-500">{{ confirmMessage }}</p>
            </div>
          </div>

          <div class="mb-6 rounded-lg bg-gray-50 p-4">
            <div class="mb-2 text-sm text-gray-600">
              <strong>操作：</strong>
              <span :class="confirmAction === 'add' ? 'text-blue-600' : 'text-red-600'">
                {{ confirmAction === 'add' ? '分配宿舍' : '取消分配' }}
              </span>
            </div>
            <div class="text-sm text-gray-600">
              <strong>受影响房间：</strong>
              <span class="text-gray-900">{{ pendingRoomIds.length }} 间</span>
            </div>
          </div>

          <div class="flex justify-end gap-3">
            <button
              @click="confirmDialogVisible = false"
              class="h-9 rounded-lg border border-gray-300 px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
            >
              取消
            </button>
            <button
              @click="handleConfirmAction"
              :disabled="submitLoading"
              class="inline-flex h-9 items-center gap-2 rounded-lg px-4 text-sm font-medium text-white disabled:opacity-50"
              :class="confirmAction === 'add' ? 'bg-blue-600 hover:bg-blue-700' : 'bg-red-600 hover:bg-red-700'"
            >
              <Loader2 v-if="submitLoading" class="h-4 w-4 animate-spin" />
              确认
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { X, Loader2, Building2, Layers, Home } from 'lucide-vue-next'
import { getDormitoriesByDepartment, addDormitory, removeDormitory, getClassDormitories } from '@/api/v2/organization'

// Props
const props = defineProps<{
  visible: boolean
  classId: number
  className: string
  departmentId: number
}>()

// Emits
const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'success'): void
}>()

// 状态
const loading = ref(false)
const submitLoading = ref(false)
const rooms = ref<any[]>([])
const allDepartmentRooms = ref<any[]>([]) // 该部门的所有宿舍
const currentClassDormitories = ref<any[]>([])
const buildingOptions = ref<Array<{ id: number; buildingName: string }>>([])
const selectedBuildingId = ref<number | null>(null)

// 确认对话框
const confirmDialogVisible = ref(false)
const confirmMessage = ref('')
const confirmAction = ref<'add' | 'remove'>('add')
const pendingRoomIds = ref<number[]>([])
const pendingFloor = ref<number | null>(null)

// 计算属性
const totalRooms = computed(() => rooms.value.length)
const assignedToCurrentClass = computed(() =>
  rooms.value.filter(r => isAssignedToCurrentClass(r)).length
)

// 当前班级已分配的宿舍ID集合
const currentClassDormitoryIds = computed(() =>
  new Set(currentClassDormitories.value.map(d => d.dormitoryId || d.id))
)

// 按楼层分组数据
const floorData = computed(() => {
  const floorMap = new Map<number, {
    floor: number
    rooms: any[]
    currentClassCount: number
  }>()

  rooms.value.forEach(room => {
    const floorNum = room.floorNumber || room.floor || 1
    if (!floorMap.has(floorNum)) {
      floorMap.set(floorNum, {
        floor: floorNum,
        rooms: [],
        currentClassCount: 0
      })
    }
    const floor = floorMap.get(floorNum)!
    floor.rooms.push(room)
    if (isAssignedToCurrentClass(room)) {
      floor.currentClassCount++
    }
  })

  // 排序：楼层从高到低
  const result = Array.from(floorMap.values())
  result.sort((a, b) => b.floor - a.floor)

  // 每层房间按房间号排序
  result.forEach(floor => {
    floor.rooms.sort((a, b) => {
      const aNo = a.dormitoryNo || ''
      const bNo = b.dormitoryNo || ''
      return aNo.localeCompare(bNo, 'zh-CN', { numeric: true })
    })
  })

  return result
})

// 判断是否分配给当前班级
const isAssignedToCurrentClass = (room: any) => {
  return currentClassDormitoryIds.value.has(room.id)
}

// 判断是否分配给其他班级（不包括当前班级）
const isAssignedToOtherClass = (room: any) => {
  if (!room.assignedClassIds) return false
  const classIds = String(room.assignedClassIds).split(',').map(id => parseInt(id.trim())).filter(id => !isNaN(id))
  const currentClassIdNum = Number(props.classId)
  // 如果只分配给当前班级，不算其他班级
  if (classIds.length === 1 && classIds[0] === currentClassIdNum) return false
  // 如果分配给了其他班级（可能也包括当前班级）
  return classIds.some(id => id !== currentClassIdNum)
}

// 获取其他班级名称（排除当前班级）
const getOtherClassNames = (room: any) => {
  if (!room.assignedClassNames || !room.assignedClassIds) return ''
  const classIds = String(room.assignedClassIds).split(',').map(id => parseInt(id.trim()))
  const classNames = String(room.assignedClassNames).split(', ')
  const currentClassIdNum = Number(props.classId)
  // 过滤掉当前班级
  const otherNames = classNames.filter((_, index) => classIds[index] !== currentClassIdNum)
  return otherNames.join(', ')
}

// 获取房间状态文本
const getRoomStatusText = (room: any) => {
  if (isAssignedToCurrentClass(room)) {
    if (isAssignedToOtherClass(room)) {
      return '混合'  // 同时分配给当前班级和其他班级
    }
    return '本班'
  }
  if (isAssignedToOtherClass(room)) {
    const otherNames = getOtherClassNames(room)
    return otherNames.length > 6 ? otherNames.substring(0, 6) + '...' : otherNames
  }
  return '可分配'
}

// 获取房间样式类
const getRoomClass = (room: any) => {
  const isCurrent = isAssignedToCurrentClass(room)
  const isOther = isAssignedToOtherClass(room)

  if (isCurrent && isOther) {
    // 混合宿舍：当前班级和其他班级都有分配
    return 'border-2 border-purple-400 bg-purple-50 hover:border-purple-500 hover:bg-purple-100'
  }
  if (isCurrent) {
    return 'border-2 border-emerald-400 bg-emerald-50 hover:border-emerald-500 hover:bg-emerald-100'
  }
  if (isOther) {
    return 'border-2 border-orange-400 bg-orange-50 hover:border-orange-500 hover:bg-orange-100'
  }
  return 'border border-gray-300 bg-white hover:border-blue-400 hover:bg-blue-50'
}

const getRoomTextClass = (room: any) => {
  const isCurrent = isAssignedToCurrentClass(room)
  const isOther = isAssignedToOtherClass(room)

  if (isCurrent && isOther) {
    return 'text-purple-700'
  }
  if (isCurrent) {
    return 'text-emerald-700'
  }
  if (isOther) {
    return 'text-orange-700'
  }
  return 'text-gray-700'
}

// 获取状态指示点样式
const getStatusDotClass = (room: any) => {
  const isCurrent = isAssignedToCurrentClass(room)
  const isOther = isAssignedToOtherClass(room)

  if (isCurrent && isOther) {
    return 'bg-purple-500 ring-2 ring-purple-200'
  }
  if (isCurrent) {
    return 'bg-emerald-500 ring-2 ring-emerald-200'
  }
  if (isOther) {
    return 'bg-orange-500 ring-2 ring-orange-200'
  }
  return 'bg-gray-300 ring-1 ring-gray-200'
}

// 加载该部门的所有宿舍，并从中提取楼宇列表
const loadDepartmentDormitories = async () => {
  try {
    allDepartmentRooms.value = await getDormitoriesByDepartment(props.departmentId) || []

    // 从宿舍数据中提取楼宇信息（去重）
    const buildingMap = new Map<number, { id: number; buildingName: string }>()
    allDepartmentRooms.value.forEach((room: any) => {
      if (room.buildingId && !buildingMap.has(room.buildingId)) {
        buildingMap.set(room.buildingId, {
          id: room.buildingId,
          buildingName: room.buildingName || `楼宇${room.buildingId}`
        })
      }
    })
    buildingOptions.value = Array.from(buildingMap.values())
  } catch (error: any) {
    console.error('加载部门宿舍失败:', error)
    allDepartmentRooms.value = []
    buildingOptions.value = []
  }
}

// 加载当前班级的宿舍
const loadCurrentClassDormitories = async () => {
  try {
    currentClassDormitories.value = await getClassDormitories(props.classId) || []
  } catch (error: any) {
    console.error('加载班级宿舍失败:', error)
    currentClassDormitories.value = []
  }
}

// 根据楼宇过滤宿舍（从已加载的部门宿舍中过滤）
const loadDormitoriesByBuilding = () => {
  if (!selectedBuildingId.value) {
    rooms.value = []
    return
  }
  // 从已加载的部门宿舍中过滤选中楼宇的宿舍
  rooms.value = allDepartmentRooms.value.filter((r: any) => r.buildingId === selectedBuildingId.value)
}

// 初始化加载
const loadData = async () => {
  loading.value = true
  selectedBuildingId.value = null
  rooms.value = []
  try {
    await Promise.all([
      loadDepartmentDormitories(),
      loadCurrentClassDormitories()
    ])
  } finally {
    loading.value = false
  }
}

// 点击楼层（整层分配）
const handleFloorClick = (floor: number) => {
  const floorRooms = rooms.value.filter(r => (r.floorNumber || r.floor || 1) === floor)
  if (floorRooms.length === 0) {
    ElMessage.warning('该楼层没有房间')
    return
  }

  // 筛选出未分配给任何班级的房间（纯空房间）
  const unassignedRooms = floorRooms.filter(r => !isAssignedToCurrentClass(r) && !isAssignedToOtherClass(r))

  if (unassignedRooms.length === 0) {
    ElMessage.warning('该楼层没有可分配的空房间')
    return
  }

  pendingFloor.value = floor
  pendingRoomIds.value = unassignedRooms.map(r => r.id)
  confirmAction.value = 'add'
  confirmMessage.value = `将 ${floor} 楼的 ${unassignedRooms.length} 间可用房间分配给本班`
  confirmDialogVisible.value = true
}

// 点击单个房间
const handleRoomClick = (room: any) => {
  pendingFloor.value = null
  pendingRoomIds.value = [room.id]

  const isCurrent = isAssignedToCurrentClass(room)
  const isOther = isAssignedToOtherClass(room)

  if (isCurrent) {
    // 已分配给当前班级，取消分配
    confirmAction.value = 'remove'
    if (isOther) {
      confirmMessage.value = `取消房间 ${room.dormitoryNo} 对本班的分配（保留其他班级分配）`
    } else {
      confirmMessage.value = `取消房间 ${room.dormitoryNo} 的分配`
    }
  } else if (isOther) {
    // 仅分配给其他班级（混合宿舍允许追加分配）
    const otherNames = getOtherClassNames(room)
    confirmAction.value = 'add'
    confirmMessage.value = `该房间已分配给 ${otherNames}，是否追加分配给本班？`
  } else {
    // 未分配，添加分配
    confirmAction.value = 'add'
    confirmMessage.value = `将房间 ${room.dormitoryNo} 分配给本班`
  }

  confirmDialogVisible.value = true
}

// 确认操作
const handleConfirmAction = async () => {
  submitLoading.value = true
  try {
    if (confirmAction.value === 'add') {
      // 批量添加
      for (const roomId of pendingRoomIds.value) {
        const room = rooms.value.find(r => r.id === roomId)
        await addDormitory(props.classId, roomId, room?.bedCapacity || room?.bedCount || 6)
      }
      ElMessage.success(`成功分配 ${pendingRoomIds.value.length} 间宿舍`)
    } else {
      // 批量移除
      for (const roomId of pendingRoomIds.value) {
        await removeDormitory(props.classId, roomId)
      }
      ElMessage.success(`成功取消 ${pendingRoomIds.value.length} 间宿舍的分配`)
    }

    confirmDialogVisible.value = false

    // 重新加载数据
    await loadCurrentClassDormitories()
    loadDormitoriesByBuilding()
    emit('success')
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

// 关闭对话框
const handleClose = () => {
  emit('update:visible', false)
}

// 监听visible变化
watch(() => props.visible, (val) => {
  if (val) {
    loadData()
  }
})

// 组件挂载时加载数据
onMounted(() => {
  if (props.visible) {
    loadData()
  }
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
