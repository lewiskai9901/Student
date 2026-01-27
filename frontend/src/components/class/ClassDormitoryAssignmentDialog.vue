<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="visible" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="fixed inset-0 bg-black/50" @click="handleClose"></div>
        <div class="relative flex max-h-[90vh] w-full max-w-6xl flex-col rounded-xl bg-white shadow-2xl">
          <!-- 头部 -->
          <div class="flex items-center justify-between border-b border-gray-200 px-6 py-4">
            <div>
              <h3 class="text-lg font-semibold text-gray-900">{{ departmentName }} - 宿舍分配给班级</h3>
              <p class="mt-0.5 text-sm text-gray-500">从左侧选择宿舍，拖拽或点击箭头分配到右侧班级</p>
            </div>
            <button @click="handleClose" class="rounded-lg p-2 text-gray-400 hover:bg-gray-100 hover:text-gray-600">
              <X class="h-5 w-5" />
            </button>
          </div>

          <!-- 主体：双面板布局 -->
          <div class="flex flex-1 overflow-hidden">
            <!-- 左侧：宿舍列表 -->
            <div class="flex w-1/2 flex-col border-r border-gray-200">
              <!-- 左侧头部 -->
              <div class="border-b border-gray-100 bg-gray-50 px-4 py-3">
                <div class="flex items-center justify-between">
                  <div class="flex items-center gap-2">
                    <DoorOpen class="h-5 w-5 text-teal-600" />
                    <span class="font-medium text-gray-900">部门宿舍</span>
                    <span class="rounded-full bg-gray-200 px-2 py-0.5 text-xs text-gray-600">
                      {{ rooms.length }} 间
                    </span>
                  </div>
                  <div class="flex items-center gap-2">
                    <button
                      @click="selectAllUnassigned"
                      class="text-xs text-blue-600 hover:text-blue-700"
                    >
                      选择全部未分配
                    </button>
                    <span class="text-gray-300">|</span>
                    <button
                      @click="clearSelection"
                      class="text-xs text-gray-500 hover:text-gray-700"
                    >
                      清除选择
                    </button>
                  </div>
                </div>
                <!-- 筛选 -->
                <div class="mt-2 flex items-center gap-2">
                  <select
                    v-model="selectedBuildingId"
                    class="min-w-[120px] rounded-lg border border-gray-200 px-2 py-1.5 text-sm focus:border-teal-500 focus:outline-none"
                  >
                    <option value="">全部楼栋</option>
                    <option v-for="building in buildingOptions" :key="building.id" :value="building.id">
                      {{ building.buildingName }}
                    </option>
                  </select>
                  <select
                    v-model="filterAssigned"
                    class="min-w-[100px] rounded-lg border border-gray-200 px-2 py-1.5 text-sm focus:border-teal-500 focus:outline-none"
                  >
                    <option value="all">全部状态</option>
                    <option value="unassigned">未分配</option>
                    <option value="assigned">已分配</option>
                  </select>
                </div>
              </div>

              <!-- 左侧内容：房间列表 -->
              <div class="flex-1 overflow-y-auto p-3">
                <div v-if="loading" class="flex items-center justify-center py-20">
                  <Loader2 class="h-8 w-8 animate-spin text-teal-500" />
                </div>

                <div v-else-if="filteredRooms.length === 0" class="py-20 text-center text-gray-400">
                  <DoorOpen class="mx-auto h-12 w-12" />
                  <p class="mt-2">暂无宿舍房间</p>
                </div>

                <div v-else class="space-y-2">
                  <div
                    v-for="floor in groupedRooms"
                    :key="floor.floor"
                    class="rounded-lg border border-gray-200 bg-white"
                  >
                    <!-- 楼层标题 -->
                    <div
                      class="flex cursor-pointer items-center justify-between bg-gray-50 px-2 py-1.5"
                      @click="toggleFloorSelection(floor.floor)"
                    >
                      <div class="flex items-center gap-1.5">
                        <input
                          type="checkbox"
                          :checked="isFloorSelected(floor.floor)"
                          :indeterminate="isFloorIndeterminate(floor.floor)"
                          class="h-3.5 w-3.5 rounded border-gray-300 text-teal-600 focus:ring-teal-500"
                          @click.stop="toggleFloorSelection(floor.floor)"
                        />
                        <span class="rounded bg-teal-100 px-1.5 py-0.5 text-[10px] font-bold text-teal-700">
                          {{ floor.floor }}F
                        </span>
                        <span class="text-xs text-gray-500">{{ floor.rooms.length }}间</span>
                        <span v-if="floor.buildingName" class="text-[10px] text-gray-400">
                          · {{ floor.buildingName }}
                        </span>
                      </div>
                      <div class="flex items-center gap-1.5 text-[10px]">
                        <span class="text-emerald-600">{{ floor.assignedCount }}已分</span>
                        <span class="text-gray-400">{{ floor.rooms.length - floor.assignedCount }}未分</span>
                      </div>
                    </div>

                    <!-- 房间标签 -->
                    <div class="flex flex-wrap gap-1 p-2">
                      <div
                        v-for="room in floor.rooms"
                        :key="room.id"
                        :draggable="true"
                        class="group relative cursor-pointer select-none rounded px-1.5 py-0.5 text-xs font-medium transition-all"
                        :class="getRoomTagClass(room)"
                        :title="getRoomTooltip(room)"
                        @dragstart="handleDragStart($event, room)"
                        @dragend="handleDragEnd"
                        @click="toggleRoomSelection(room.id)"
                      >
                        {{ room.dormitoryNo }}
                        <!-- 已分配班级数量徽标 -->
                        <span
                          v-if="getAssignedClassCount(room) > 0"
                          class="absolute -right-1 -top-1 flex h-3.5 w-3.5 items-center justify-center rounded-full bg-emerald-500 text-[8px] font-bold text-white"
                        >
                          {{ getAssignedClassCount(room) }}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 左侧底部：已选择提示 -->
              <div v-if="selectedRoomIds.length > 0" class="border-t border-gray-200 bg-teal-50 px-4 py-3">
                <div class="flex items-center justify-between">
                  <span class="text-sm text-teal-700">
                    已选择 <strong>{{ selectedRoomIds.length }}</strong> 间宿舍
                  </span>
                  <button
                    @click="clearSelection"
                    class="text-xs text-teal-600 hover:text-teal-700"
                  >
                    清除选择
                  </button>
                </div>
              </div>
            </div>

            <!-- 中间：箭头操作区 -->
            <div class="flex w-16 flex-col items-center justify-center gap-3 bg-gray-50">
              <button
                @click="assignToSelectedClass"
                :disabled="selectedRoomIds.length === 0 || !targetClassId"
                class="flex h-10 w-10 items-center justify-center rounded-full bg-teal-600 text-white shadow-lg transition-all hover:bg-teal-700 disabled:cursor-not-allowed disabled:bg-gray-300 disabled:shadow-none"
                title="分配到选中班级"
              >
                <ChevronRight class="h-5 w-5" />
              </button>
              <button
                @click="unassignFromSelectedClass"
                :disabled="selectedRoomIds.length === 0 || !targetClassId"
                class="flex h-10 w-10 items-center justify-center rounded-full border-2 border-gray-300 bg-white text-gray-500 transition-all hover:border-red-300 hover:bg-red-50 hover:text-red-500 disabled:cursor-not-allowed disabled:opacity-50"
                title="从选中班级取消分配"
              >
                <ChevronLeft class="h-5 w-5" />
              </button>
            </div>

            <!-- 右侧：班级列表 -->
            <div class="flex w-1/2 flex-col">
              <!-- 右侧头部 -->
              <div class="border-b border-gray-100 bg-gray-50 px-4 py-3">
                <div class="flex items-center gap-2">
                  <Users class="h-5 w-5 text-blue-600" />
                  <span class="font-medium text-gray-900">班级</span>
                  <span class="rounded-full bg-gray-200 px-2 py-0.5 text-xs text-gray-600">
                    {{ classOptions.length }} 个
                  </span>
                </div>
                <p class="mt-1 text-xs text-gray-500">点击班级展开宿舍列表，拖放宿舍到班级区域完成分配</p>
              </div>

              <!-- 右侧内容：班级卡片 -->
              <div class="flex-1 overflow-y-auto">
                <!-- 未分配区域（固定在顶部） -->
                <div
                  class="sticky top-0 z-10 bg-white p-3 pb-2"
                  @dragover.prevent="handleDragOverUnassigned"
                  @dragleave="handleDragLeaveUnassigned"
                  @drop="handleDropToUnassigned"
                >
                  <div
                    class="rounded-lg border-2 border-dashed transition-all"
                    :class="[
                      dragOverUnassigned ? 'border-red-400 bg-red-50 scale-[1.01] shadow-lg' : 'border-gray-300 bg-gray-50 hover:border-gray-400'
                    ]"
                  >
                    <div class="flex items-center justify-center gap-2 px-3 py-2">
                      <div class="flex h-7 w-7 items-center justify-center rounded-lg bg-gray-200 text-gray-500">
                        <X class="h-4 w-4" />
                      </div>
                      <div>
                        <div class="text-sm font-medium text-gray-600">取消分配</div>
                        <div class="text-[10px] text-gray-400">
                          拖拽到此处从所有班级取消分配
                        </div>
                      </div>
                    </div>
                    <!-- 拖放提示 -->
                    <div
                      v-if="isDragging && dragOverUnassigned"
                      class="pointer-events-none border-t border-red-200 bg-red-100 py-1.5 text-center text-xs font-medium text-red-600"
                    >
                      松开鼠标取消分配
                    </div>
                  </div>
                </div>

                <div class="space-y-2 px-3 pb-3">
                  <div
                    v-for="cls in classOptions"
                    :key="cls.id"
                    class="rounded-lg border-2 transition-all"
                    :class="[
                      getClassCardClass(cls.id),
                      dragOverClassId === cls.id ? 'scale-[1.01] border-teal-500 bg-teal-50 shadow-lg' : ''
                    ]"
                    @dragover.prevent="handleDragOver($event, cls.id)"
                    @dragleave="handleDragLeave"
                    @drop="handleDrop($event, cls.id)"
                  >
                    <!-- 班级头部 -->
                    <div
                      class="flex cursor-pointer items-center justify-between px-3 py-2"
                      @click="toggleClassExpand(cls.id)"
                    >
                      <div class="flex items-center gap-2">
                        <div
                          class="flex h-8 w-8 items-center justify-center rounded-lg text-sm font-bold"
                          :class="targetClassId === cls.id ? 'bg-blue-600 text-white' : 'bg-blue-100 text-blue-600'"
                        >
                          {{ cls.className.charAt(0) }}
                        </div>
                        <div>
                          <div class="text-sm font-medium text-gray-900">{{ cls.className }}</div>
                          <div class="text-[10px] text-gray-500">
                            {{ getClassRoomCount(cls.id) }} 间宿舍
                          </div>
                        </div>
                      </div>
                      <div class="flex items-center gap-2">
                        <div v-if="targetClassId === cls.id" class="flex items-center gap-1 rounded-full bg-blue-100 px-2 py-0.5 text-blue-600">
                          <Check class="h-3.5 w-3.5" />
                          <span class="text-[10px] font-medium">目标</span>
                        </div>
                        <component
                          :is="expandedClassIds.includes(cls.id) ? ChevronUp : ChevronDown"
                          class="h-4 w-4 text-gray-400"
                        />
                      </div>
                    </div>

                    <!-- 展开的宿舍列表 -->
                    <div
                      v-if="expandedClassIds.includes(cls.id)"
                      class="border-t border-gray-100 bg-gray-50/50"
                    >
                      <div v-if="getClassRoomsByFloor(cls.id).length > 0" class="space-y-0.5">
                        <div
                          v-for="floor in getClassRoomsByFloor(cls.id)"
                          :key="floor.floor"
                          class="border-b border-gray-100 last:border-b-0"
                        >
                          <!-- 楼层标题 -->
                          <div class="flex items-center gap-2 bg-blue-50/50 px-2 py-1">
                            <span class="rounded bg-blue-200 px-1.5 py-0.5 text-[10px] font-bold text-blue-700">
                              {{ floor.floor }}F
                            </span>
                            <span class="text-[10px] text-gray-500">{{ floor.rooms.length }}间</span>
                          </div>
                          <!-- 宿舍列表 -->
                          <div class="flex flex-wrap gap-1 p-1.5">
                            <div
                              v-for="room in floor.rooms"
                              :key="room.id"
                              :draggable="true"
                              class="cursor-pointer rounded bg-blue-100 px-1.5 py-0.5 text-xs font-medium text-blue-700 transition-colors hover:bg-blue-200"
                              :class="isRoomSelected(room.id) ? 'ring-2 ring-teal-400 ring-offset-1 bg-teal-500 text-white' : ''"
                              :title="getRoomTooltip(room)"
                              @dragstart="handleDragStart($event, room)"
                              @dragend="handleDragEnd"
                              @click.stop="toggleRoomSelection(room.id)"
                            >
                              {{ room.dormitoryNo }}
                            </div>
                          </div>
                        </div>
                      </div>
                      <div v-else class="py-2 text-center text-xs text-gray-400">
                        暂无宿舍
                      </div>
                    </div>

                    <!-- 拖放提示 -->
                    <div
                      v-if="isDragging && dragOverClassId === cls.id"
                      class="pointer-events-none border-t border-teal-200 bg-teal-100 py-2 text-center text-xs font-medium text-teal-600"
                    >
                      松开鼠标完成分配
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 底部 -->
          <div class="flex items-center justify-between border-t border-gray-200 bg-gray-50 px-6 py-4">
            <div class="flex items-center gap-4 text-sm text-gray-500">
              <div class="flex items-center gap-2">
                <div class="h-3 w-3 rounded border border-gray-300 bg-white"></div>
                <span>未分配</span>
              </div>
              <div class="flex items-center gap-2">
                <div class="h-3 w-3 rounded bg-emerald-400"></div>
                <span>已分配</span>
              </div>
              <div class="flex items-center gap-2">
                <div class="h-3 w-3 rounded bg-teal-500"></div>
                <span>选中</span>
              </div>
            </div>
            <div class="flex items-center gap-3">
              <span class="text-sm text-gray-500">
                共 {{ rooms.length }} 间 | 已分配 {{ assignedRoomsCount }} 间
              </span>
              <button
                @click="handleClose"
                class="h-9 rounded-lg border border-gray-300 bg-white px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
              >
                关闭
              </button>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  X,
  Loader2,
  Users,
  DoorOpen,
  ChevronRight,
  ChevronLeft,
  ChevronUp,
  ChevronDown,
  Check
} from 'lucide-vue-next'
import { getDormitoriesByOrgUnit, addDormitory, removeDormitory, getClassesByOrgUnit } from '@/api/organization'

// Props - 使用 string | number 因为大数字 ID 可能超过 JS 安全整数范围
const props = defineProps<{
  visible: boolean
  departmentId: number | string
  departmentName: string
}>()

// Emits
const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'close'): void
  (e: 'success'): void
}>()

// 状态
const loading = ref(false)
const rooms = ref<any[]>([])
// 使用 string 类型存储 ID，避免大数字精度丢失
const classOptions = ref<Array<{ id: string; className: string }>>([])
const buildingOptions = ref<Array<{ id: string; buildingName: string }>>([])

// 选择状态
const selectedRoomIds = ref<string[]>([])
const targetClassId = ref<string | null>(null)
const expandedClassIds = ref<string[]>([])

// 筛选状态
const selectedBuildingId = ref<number | string>('')
const filterAssigned = ref<'all' | 'unassigned' | 'assigned'>('all')

// 拖拽状态
const isDragging = ref(false)
const dragOverClassId = ref<string | null>(null)
const dragOverUnassigned = ref(false)
const draggedRoomIds = ref<string[]>([])

// 标记是否有数据变更
const hasChanges = ref(false)

// 计算属性
const filteredRooms = computed(() => {
  let result = [...rooms.value]

  // 使用字符串比较建筑 ID
  if (selectedBuildingId.value !== '' && selectedBuildingId.value !== null) {
    result = result.filter(r => String(r.buildingId) === String(selectedBuildingId.value))
  }

  if (filterAssigned.value === 'unassigned') {
    result = result.filter(r => !r.assignedClassIds)
  } else if (filterAssigned.value === 'assigned') {
    result = result.filter(r => r.assignedClassIds)
  }

  return result
})

const groupedRooms = computed(() => {
  const floorMap = new Map<string, { floor: number; buildingName: string; rooms: any[]; assignedCount: number }>()

  filteredRooms.value.forEach(room => {
    const floorNum = room.floorNumber || room.floor || 1
    const buildingName = room.buildingName || ''
    const key = `${room.buildingId || 0}-${floorNum}`

    if (!floorMap.has(key)) {
      floorMap.set(key, { floor: floorNum, buildingName, rooms: [], assignedCount: 0 })
    }
    const floor = floorMap.get(key)!
    floor.rooms.push(room)
    if (room.assignedClassIds) {
      floor.assignedCount++
    }
  })

  const result = Array.from(floorMap.values())
  result.sort((a, b) => {
    if (a.buildingName !== b.buildingName) {
      return a.buildingName.localeCompare(b.buildingName, 'zh-CN')
    }
    return b.floor - a.floor
  })

  result.forEach(floor => {
    floor.rooms.sort((a, b) => {
      const aNo = a.dormitoryNo || ''
      const bNo = b.dormitoryNo || ''
      return aNo.localeCompare(bNo, 'zh-CN', { numeric: true })
    })
  })

  return result
})

const assignedRoomsCount = computed(() => rooms.value.filter(r => r.assignedClassIds).length)

// 获取宿舍已分配的班级数量
const getAssignedClassCount = (room: any) => {
  if (!room.assignedClassIds) return 0
  return String(room.assignedClassIds).split(',').filter(id => id.trim()).length
}

// 获取班级下的宿舍数量
const getClassRoomCount = (classId: string) => {
  const count = rooms.value.filter(r => isRoomAssignedToClass(r, classId)).length
  return count
}

// 判断宿舍是否分配给某班级（使用字符串比较避免大数字精度问题）
const isRoomAssignedToClass = (room: any, classId: string) => {
  if (!room || !room.assignedClassIds) return false
  const classIds = String(room.assignedClassIds).split(',').map(id => id.trim()).filter(id => id)
  const result = classIds.includes(String(classId))
  return result
}

// 获取班级下按楼层分组的宿舍
const getClassRoomsByFloor = (classId: string) => {
  const classRooms = rooms.value.filter(r => isRoomAssignedToClass(r, classId))
  if (classRooms.length === 0) return []

  const floorMap = new Map<number, { floor: number; rooms: any[] }>()

  classRooms.forEach(room => {
    const floorNum = room.floorNumber || room.floor || 1
    if (!floorMap.has(floorNum)) {
      floorMap.set(floorNum, { floor: floorNum, rooms: [] })
    }
    floorMap.get(floorNum)!.rooms.push(room)
  })

  const result = Array.from(floorMap.values())
  result.sort((a, b) => b.floor - a.floor)

  result.forEach(floor => {
    floor.rooms.sort((a, b) => {
      const aNo = a.dormitoryNo || ''
      const bNo = b.dormitoryNo || ''
      return aNo.localeCompare(bNo, 'zh-CN', { numeric: true })
    })
  })

  return result
}

// 方法
const loadData = async () => {
  loading.value = true
  console.log('[ClassDormitoryAssignmentDialog] Loading data for departmentId:', props.departmentId)
  try {
    const dormitories = await getDormitoriesByOrgUnit(props.departmentId) || []
    console.log('[ClassDormitoryAssignmentDialog] Dormitories loaded:', dormitories.length, dormitories)
    rooms.value = dormitories

    // 从宿舍数据中提取楼栋信息 - 使用字符串存储 ID
    const buildingMap = new Map<string, { id: string; buildingName: string }>()
    dormitories.forEach((room: any) => {
      if (room.buildingId) {
        const buildingIdStr = String(room.buildingId)
        if (!buildingMap.has(buildingIdStr)) {
          buildingMap.set(buildingIdStr, {
            id: buildingIdStr,
            buildingName: room.buildingName || `楼栋${room.buildingId}`
          })
        }
      }
    })
    buildingOptions.value = Array.from(buildingMap.values())

    // 从 API 获取部门下的所有班级
    try {
      const classes = await getClassesByOrgUnit(props.departmentId) || []
      console.log('[ClassDormitoryAssignmentDialog] Classes loaded:', classes.length, classes)
      // 使用字符串存储 ID，避免大数字精度丢失
      classOptions.value = classes.map((cls: any) => ({
        id: String(cls.id),
        className: cls.className || cls.name || `班级${cls.id}`
      }))
    } catch (error) {
      console.warn('获取班级列表失败，尝试从宿舍数据中提取:', error)
      // 从宿舍数据中提取班级信息作为备选方案 - 使用字符串存储 ID
      const classMap = new Map<string, { id: string; className: string }>()
      dormitories.forEach((room: any) => {
        if (room.assignedClassIds && room.assignedClassNames) {
          const classIds = String(room.assignedClassIds).split(',').map(id => id.trim()).filter(id => id)
          const classNames = String(room.assignedClassNames).split(',').map(name => name.trim())
          classIds.forEach((id, index) => {
            if (!classMap.has(id)) {
              classMap.set(id, { id, className: classNames[index] || `班级${id}` })
            }
          })
        }
      })
      classOptions.value = Array.from(classMap.values())
    }
  } catch (error: any) {
    ElMessage.error(error.message || '加载数据失败')
  } finally {
    loading.value = false
  }
}

const filterByBuilding = () => {
  // 筛选会通过 computed 自动处理
}

// 选择操作
const toggleRoomSelection = (roomId: number | string) => {
  const idStr = String(roomId)
  const index = selectedRoomIds.value.findIndex(id => id === idStr)
  if (index === -1) {
    selectedRoomIds.value.push(idStr)
  } else {
    selectedRoomIds.value.splice(index, 1)
  }
}

const toggleFloorSelection = (floor: number) => {
  const floorRooms = filteredRooms.value.filter(r => (r.floorNumber || r.floor || 1) === floor)
  const floorRoomIdStrs = floorRooms.map(r => String(r.id))

  const allSelected = floorRoomIdStrs.every(id => selectedRoomIds.value.includes(id))

  if (allSelected) {
    selectedRoomIds.value = selectedRoomIds.value.filter(id => !floorRoomIdStrs.includes(id))
  } else {
    floorRoomIdStrs.forEach(id => {
      if (!selectedRoomIds.value.includes(id)) {
        selectedRoomIds.value.push(id)
      }
    })
  }
}

const isFloorSelected = (floor: number) => {
  const floorRoomIdStrs = filteredRooms.value
    .filter(r => (r.floorNumber || r.floor || 1) === floor)
    .map(r => String(r.id))
  return floorRoomIdStrs.length > 0 && floorRoomIdStrs.every(id => selectedRoomIds.value.includes(id))
}

const isFloorIndeterminate = (floor: number) => {
  const floorRoomIdStrs = filteredRooms.value
    .filter(r => (r.floorNumber || r.floor || 1) === floor)
    .map(r => String(r.id))
  const selectedCount = floorRoomIdStrs.filter(id => selectedRoomIds.value.includes(id)).length
  return selectedCount > 0 && selectedCount < floorRoomIdStrs.length
}

const selectAllUnassigned = () => {
  const unassignedIdStrs = filteredRooms.value.filter(r => !r.assignedClassIds).map(r => String(r.id))
  selectedRoomIds.value = unassignedIdStrs
}

const clearSelection = () => {
  selectedRoomIds.value = []
}

const isRoomSelected = (roomId: number | string) => {
  return selectedRoomIds.value.some(id => String(id) === String(roomId))
}

const toggleClassExpand = (classId: string) => {
  const index = expandedClassIds.value.indexOf(classId)
  if (index === -1) {
    expandedClassIds.value.push(classId)
    targetClassId.value = classId
  } else {
    expandedClassIds.value.splice(index, 1)
  }
}

// 拖拽操作
const handleDragStart = (event: DragEvent, room: any) => {
  isDragging.value = true

  if (isRoomSelected(room.id)) {
    draggedRoomIds.value = [...selectedRoomIds.value]
  } else {
    draggedRoomIds.value = [String(room.id)]
  }

  event.dataTransfer?.setData('text/plain', JSON.stringify(draggedRoomIds.value))
}

const handleDragEnd = () => {
  isDragging.value = false
  dragOverClassId.value = null
  dragOverUnassigned.value = false
  draggedRoomIds.value = []
}

const handleDragOver = (event: DragEvent, classId: string) => {
  event.preventDefault()
  dragOverClassId.value = classId
  dragOverUnassigned.value = false
}

const handleDragLeave = (event: DragEvent) => {
  const target = event.currentTarget as HTMLElement
  const relatedTarget = event.relatedTarget as HTMLElement
  if (target && relatedTarget && target.contains(relatedTarget)) {
    return
  }
  dragOverClassId.value = null
}

const handleDrop = async (event: DragEvent, classId: string) => {
  event.preventDefault()
  dragOverClassId.value = null
  isDragging.value = false

  if (draggedRoomIds.value.length === 0) return

  await assignRoomsToClass(draggedRoomIds.value, classId)
  draggedRoomIds.value = []
}

// 未分配区域拖拽处理
const handleDragOverUnassigned = (event: DragEvent) => {
  event.preventDefault()
  dragOverUnassigned.value = true
  dragOverClassId.value = null
}

const handleDragLeaveUnassigned = (event: DragEvent) => {
  const target = event.currentTarget as HTMLElement
  const relatedTarget = event.relatedTarget as HTMLElement
  if (target && relatedTarget && target.contains(relatedTarget)) {
    return
  }
  dragOverUnassigned.value = false
}

const handleDropToUnassigned = async (event: DragEvent) => {
  event.preventDefault()
  dragOverUnassigned.value = false
  isDragging.value = false

  if (draggedRoomIds.value.length === 0) return

  // 从所有班级取消分配
  await unassignRoomsFromAllClasses(draggedRoomIds.value)
  draggedRoomIds.value = []
}

// 分配操作
const assignToSelectedClass = async () => {
  if (selectedRoomIds.value.length === 0 || !targetClassId.value) return
  await assignRoomsToClass(selectedRoomIds.value, targetClassId.value)
}

const unassignFromSelectedClass = async () => {
  if (selectedRoomIds.value.length === 0 || !targetClassId.value) return
  await unassignRoomsFromClass(selectedRoomIds.value, targetClassId.value)
}

const assignRoomsToClass = async (roomIds: string[], classId: string) => {
  console.log('[assignRoomsToClass] Starting assignment:', { roomIds, classId })
  try {
    for (const roomId of roomIds) {
      const room = rooms.value.find(r => String(r.id) === String(roomId))
      console.log('[assignRoomsToClass] Processing room:', room?.id, 'already assigned:', isRoomAssignedToClass(room, classId))
      if (room && !isRoomAssignedToClass(room, classId)) {
        await addDormitory(classId, roomId, room?.bedCapacity || room?.bedCount || 6)
      }
    }

    // 更新本地数据 - 使用字符串比较
    const className = classOptions.value.find(c => String(c.id) === String(classId))?.className || ''
    console.log('[assignRoomsToClass] Updating local data, className:', className)

    const newRooms = rooms.value.map(room => {
      if (roomIds.map(id => String(id)).includes(String(room.id))) {
        const existingIds = room.assignedClassIds ? String(room.assignedClassIds).split(',').map(id => id.trim()) : []
        const existingNames = room.assignedClassNames ? String(room.assignedClassNames).split(',').map(n => n.trim()) : []

        if (!existingIds.includes(String(classId))) {
          existingIds.push(String(classId))
          existingNames.push(className)
        }

        const updatedRoom = {
          ...room,
          assignedClassIds: existingIds.join(','),
          assignedClassNames: existingNames.join(',')
        }
        console.log('[assignRoomsToClass] Updated room:', updatedRoom.id, 'assignedClassIds:', updatedRoom.assignedClassIds)
        return updatedRoom
      }
      return room
    })

    rooms.value = newRooms
    console.log('[assignRoomsToClass] rooms.value updated, total rooms:', rooms.value.length)

    selectedRoomIds.value = selectedRoomIds.value.filter(id => !roomIds.map(rid => String(rid)).includes(String(id)))
    hasChanges.value = true

    // 自动展开目标班级
    if (!expandedClassIds.value.includes(classId)) {
      expandedClassIds.value.push(classId)
    }

    ElMessage.success(`成功分配 ${roomIds.length} 间宿舍`)
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

const unassignRoomsFromClass = async (roomIds: string[], classId: string) => {
  try {
    for (const roomId of roomIds) {
      const room = rooms.value.find(r => String(r.id) === String(roomId))
      if (room && isRoomAssignedToClass(room, classId)) {
        await removeDormitory(classId, roomId)
      }
    }

    // 更新本地数据
    rooms.value = rooms.value.map(room => {
      if (roomIds.map(id => String(id)).includes(String(room.id))) {
        const existingIds = room.assignedClassIds ? String(room.assignedClassIds).split(',').map(id => id.trim()) : []
        const existingNames = room.assignedClassNames ? String(room.assignedClassNames).split(',').map(n => n.trim()) : []

        const classIndex = existingIds.indexOf(String(classId))
        if (classIndex !== -1) {
          existingIds.splice(classIndex, 1)
          existingNames.splice(classIndex, 1)
        }

        return {
          ...room,
          assignedClassIds: existingIds.length > 0 ? existingIds.join(',') : null,
          assignedClassNames: existingNames.length > 0 ? existingNames.join(',') : null
        }
      }
      return room
    })

    selectedRoomIds.value = selectedRoomIds.value.filter(id => !roomIds.map(rid => String(rid)).includes(String(id)))
    hasChanges.value = true

    ElMessage.success(`成功取消 ${roomIds.length} 间宿舍的分配`)
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

const unassignRoomsFromAllClasses = async (roomIds: string[]) => {
  try {
    for (const roomId of roomIds) {
      const room = rooms.value.find(r => String(r.id) === String(roomId))
      if (room && room.assignedClassIds) {
        const classIds = String(room.assignedClassIds).split(',').map(id => id.trim()).filter(id => id)
        for (const classId of classIds) {
          await removeDormitory(classId, roomId)
        }
      }
    }

    // 更新本地数据
    rooms.value = rooms.value.map(room => {
      if (roomIds.map(id => String(id)).includes(String(room.id))) {
        return {
          ...room,
          assignedClassIds: null,
          assignedClassNames: null
        }
      }
      return room
    })

    selectedRoomIds.value = selectedRoomIds.value.filter(id => !roomIds.map(rid => String(rid)).includes(String(id)))
    hasChanges.value = true

    ElMessage.success(`成功取消 ${roomIds.length} 间宿舍的分配`)
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

// 样式
const getRoomTagClass = (room: any) => {
  if (isRoomSelected(room.id)) {
    return 'bg-teal-500 text-white ring-2 ring-teal-300 ring-offset-1'
  }
  if (room.assignedClassIds) {
    return 'bg-emerald-100 text-emerald-700 hover:bg-emerald-200'
  }
  return 'bg-gray-100 text-gray-600 hover:bg-gray-200'
}

const getRoomTooltip = (room: any) => {
  const roomNo = room.dormitoryNo || ''
  const beds = room.bedCapacity || room.bedCount || 0
  const classes = room.assignedClassNames || '未分配'
  return `${roomNo} | ${beds}人间 | ${classes}`
}

const getClassCardClass = (classId: string) => {
  if (String(targetClassId.value) === String(classId)) {
    return 'border-blue-500 bg-blue-50 cursor-pointer'
  }
  return 'border-gray-200 bg-white hover:border-blue-300 cursor-pointer'
}

// 关闭
const handleClose = () => {
  emit('update:visible', false)
  emit('close')
  if (hasChanges.value) {
    emit('success')
    hasChanges.value = false
  }
}

// 监听
watch(() => props.visible, (val) => {
  if (val) {
    loadData()
    selectedRoomIds.value = []
    targetClassId.value = null
    expandedClassIds.value = []
    selectedBuildingId.value = ''
    filterAssigned.value = 'all'
    hasChanges.value = false
  }
})

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
