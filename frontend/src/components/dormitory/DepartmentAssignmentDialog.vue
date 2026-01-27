<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="visible" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="fixed inset-0 bg-black/50" @click="handleClose"></div>
        <div class="relative flex max-h-[90vh] w-full max-w-6xl flex-col rounded-xl bg-white shadow-2xl">
          <!-- 头部 -->
          <div class="flex items-center justify-between border-b border-gray-200 px-6 py-4">
            <div>
              <h3 class="text-lg font-semibold text-gray-900">{{ buildingName }} - 宿舍分配给部门</h3>
              <p class="mt-0.5 text-sm text-gray-500">从左侧选择宿舍，拖拽或点击箭头分配到右侧部门</p>
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
                    <span class="font-medium text-gray-900">宿舍房间</span>
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
                    v-model="filterFloor"
                    class="h-8 rounded-lg border border-gray-200 px-2 text-sm focus:border-teal-500 focus:outline-none"
                  >
                    <option :value="null">全部楼层</option>
                    <option v-for="floor in floorList" :key="floor" :value="floor">
                      {{ floor }}层
                    </option>
                  </select>
                  <select
                    v-model="filterAssigned"
                    class="h-8 rounded-lg border border-gray-200 px-2 text-sm focus:border-teal-500 focus:outline-none"
                  >
                    <option value="all">全部状态</option>
                    <option value="unassigned">未分配</option>
                    <option value="assigned">已分配</option>
                  </select>
                </div>
              </div>

              <!-- 左侧内容：房间列表（紧凑布局） -->
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
                    <!-- 楼层标题（紧凑） -->
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
                      </div>
                      <div class="flex items-center gap-1.5 text-[10px]">
                        <span class="text-blue-600">{{ floor.assignedCount }}已分</span>
                        <span class="text-gray-400">{{ floor.rooms.length - floor.assignedCount }}未分</span>
                      </div>
                    </div>

                    <!-- 房间标签（紧凑网格） -->
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
                        {{ room.dormitoryNo || room.roomNo }}
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
                @click="assignToSelectedDepartment"
                :disabled="selectedRoomIds.length === 0 || !targetDepartmentId"
                class="flex h-10 w-10 items-center justify-center rounded-full bg-teal-600 text-white shadow-lg transition-all hover:bg-teal-700 disabled:cursor-not-allowed disabled:bg-gray-300 disabled:shadow-none"
                title="分配到选中部门"
              >
                <ChevronRight class="h-5 w-5" />
              </button>
              <button
                @click="unassignSelected"
                :disabled="selectedRoomIds.length === 0"
                class="flex h-10 w-10 items-center justify-center rounded-full border-2 border-gray-300 bg-white text-gray-500 transition-all hover:border-red-300 hover:bg-red-50 hover:text-red-500 disabled:cursor-not-allowed disabled:opacity-50"
                title="取消分配"
              >
                <ChevronLeft class="h-5 w-5" />
              </button>
            </div>

            <!-- 右侧：部门列表 -->
            <div class="flex w-1/2 flex-col">
              <!-- 右侧头部 -->
              <div class="border-b border-gray-100 bg-gray-50 px-4 py-3">
                <div class="flex items-center gap-2">
                  <Building2 class="h-5 w-5 text-blue-600" />
                  <span class="font-medium text-gray-900">部门</span>
                  <span class="rounded-full bg-gray-200 px-2 py-0.5 text-xs text-gray-600">
                    {{ departmentOptions.length }} 个
                  </span>
                </div>
                <p class="mt-1 text-xs text-gray-500">点击部门展开宿舍列表，拖放宿舍到部门区域完成分配</p>
              </div>

              <!-- 右侧内容：部门卡片 -->
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
                        <div class="text-sm font-medium text-gray-600">未分配</div>
                        <div class="text-[10px] text-gray-400">
                          拖拽到此处取消分配 | {{ unassignedRoomsCount }} 间
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
                    v-for="dept in departmentOptions"
                    :key="dept.id"
                    class="rounded-lg border-2 transition-all"
                    :class="[
                      getDepartmentCardClass(dept.id),
                      dragOverDeptId === dept.id ? 'scale-[1.01] border-teal-500 bg-teal-50 shadow-lg' : ''
                    ]"
                    @dragover.prevent="handleDragOver($event, dept.id)"
                    @dragleave="handleDragLeave"
                    @drop="handleDrop($event, dept.id)"
                  >
                    <!-- 部门头部（可点击选中/展开） -->
                    <div
                      class="flex cursor-pointer items-center justify-between px-3 py-2"
                      @click="toggleDepartmentExpand(dept.id)"
                    >
                      <div class="flex items-center gap-2">
                        <div
                          class="flex h-8 w-8 items-center justify-center rounded-lg text-sm font-bold"
                          :class="targetDepartmentId === dept.id ? 'bg-blue-600 text-white' : 'bg-blue-100 text-blue-600'"
                        >
                          {{ dept.deptName.charAt(0) }}
                        </div>
                        <div>
                          <div class="text-sm font-medium text-gray-900">{{ dept.deptName }}</div>
                          <div class="text-[10px] text-gray-500">
                            {{ getDepartmentRoomCount(dept.id) }} 间宿舍
                          </div>
                        </div>
                      </div>
                      <div class="flex items-center gap-2">
                        <div v-if="targetDepartmentId === dept.id" class="flex items-center gap-1 rounded-full bg-blue-100 px-2 py-0.5 text-blue-600">
                          <Check class="h-3.5 w-3.5" />
                          <span class="text-[10px] font-medium">目标</span>
                        </div>
                        <component
                          :is="expandedDeptIds.includes(dept.id) ? ChevronUp : ChevronDown"
                          class="h-4 w-4 text-gray-400"
                        />
                      </div>
                    </div>

                    <!-- 展开的宿舍列表（按楼层分组显示） -->
                    <div
                      v-if="expandedDeptIds.includes(dept.id)"
                      class="border-t border-gray-100 bg-gray-50/50"
                    >
                      <div v-if="getDepartmentRoomsByFloor(dept.id).length > 0" class="space-y-0.5">
                        <!-- 按楼层分组 -->
                        <div
                          v-for="floor in getDepartmentRoomsByFloor(dept.id)"
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
                          <!-- 该楼层宿舍列表 -->
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
                              {{ room.dormitoryNo || room.roomNo }}
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
                      v-if="isDragging && dragOverDeptId === dept.id"
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
                <div class="h-3 w-3 rounded bg-blue-400"></div>
                <span>已分配</span>
              </div>
              <div class="flex items-center gap-2">
                <div class="h-3 w-3 rounded bg-teal-500"></div>
                <span>选中</span>
              </div>
              <div class="flex items-center gap-2">
                <div class="h-3 w-3 rounded bg-orange-300 opacity-60"></div>
                <span>锁定</span>
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
  Building2,
  DoorOpen,
  ChevronRight,
  ChevronLeft,
  ChevronUp,
  ChevronDown,
  Check
} from 'lucide-vue-next'
import { getDormitoriesByBuilding, batchUpdateDepartment } from '@/api/dormitory'
import { getOrgUnitsByType } from '@/api/organization'

// Props
const props = defineProps<{
  visible: boolean
  buildingId: number
  buildingName: string
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
const departmentOptions = ref<Array<{ id: number; deptName: string }>>([])

// 选择状态 - 使用string避免大整数精度丢失
const selectedRoomIds = ref<(number | string)[]>([])
const targetDepartmentId = ref<number | null>(null)
const expandedDeptIds = ref<number[]>([])

// 筛选状态
const filterFloor = ref<number | null>(null)
const filterAssigned = ref<'all' | 'unassigned' | 'assigned'>('all')

// 拖拽状态
const isDragging = ref(false)
const dragOverDeptId = ref<number | null>(null)
const dragOverUnassigned = ref(false)
const draggedRoomIds = ref<number[]>([])

// 计算属性
const floorList = computed(() => {
  const floors = new Set<number>()
  rooms.value.forEach(room => {
    const floor = room.floorNumber || room.floor || 1
    floors.add(floor)
  })
  return Array.from(floors).sort((a, b) => a - b)
})

const filteredRooms = computed(() => {
  let result = [...rooms.value]

  if (filterFloor.value !== null) {
    result = result.filter(r => (r.floorNumber || r.floor || 1) === filterFloor.value)
  }

  if (filterAssigned.value === 'unassigned') {
    result = result.filter(r => !r.orgUnitId)
  } else if (filterAssigned.value === 'assigned') {
    result = result.filter(r => r.orgUnitId)
  }

  return result
})

const groupedRooms = computed(() => {
  const floorMap = new Map<number, { floor: number; rooms: any[]; assignedCount: number }>()

  filteredRooms.value.forEach(room => {
    const floorNum = room.floorNumber || room.floor || 1
    if (!floorMap.has(floorNum)) {
      floorMap.set(floorNum, { floor: floorNum, rooms: [], assignedCount: 0 })
    }
    const floor = floorMap.get(floorNum)!
    floor.rooms.push(room)
    if (room.orgUnitId) {
      floor.assignedCount++
    }
  })

  const result = Array.from(floorMap.values())
  result.sort((a, b) => b.floor - a.floor)

  result.forEach(floor => {
    floor.rooms.sort((a, b) => {
      const aNo = a.dormitoryNo || a.roomNo || ''
      const bNo = b.dormitoryNo || b.roomNo || ''
      return aNo.localeCompare(bNo, 'zh-CN', { numeric: true })
    })
  })

  return result
})

const assignedRoomsCount = computed(() => rooms.value.filter(r => r.orgUnitId).length)
const unassignedRoomsCount = computed(() => rooms.value.filter(r => !r.orgUnitId).length)

// 方法
const loadData = async () => {
  loading.value = true
  try {
    const [roomsRes, deptsRes] = await Promise.all([
      getDormitoriesByBuilding(props.buildingId),
      getOrgUnitsByType('DEPARTMENT')
    ])
    rooms.value = roomsRes || []
    departmentOptions.value = (deptsRes || []).map((d: any) => ({
      id: d.id,
      deptName: d.unitName
    }))
  } catch (error: any) {
    ElMessage.error(error.message || '加载数据失败')
  } finally {
    loading.value = false
  }
}

// 判断房间是否可以被选中
// 规则：当选中了目标部门时，已分配给其他部门的宿舍不可选中（需要先取消分配）
const canSelectRoom = (room: any): boolean => {
  // 如果没有选择目标部门，所有房间都可选（用于取消分配操作）
  if (!targetDepartmentId.value) return true

  // 未分配的房间可选（可以分配给目标部门）
  if (!room.orgUnitId) return true

  // 已分配给目标部门的房间可选（可以取消分配）
  if (room.orgUnitId === targetDepartmentId.value) return true

  // 已分配给其他部门的房间不可选
  return false
}

// 获取房间被锁定的原因
const getRoomLockedReason = (room: any): string | null => {
  if (!targetDepartmentId.value || !room.orgUnitId) return null
  if (room.orgUnitId === targetDepartmentId.value) return null

  const deptName = departmentOptions.value.find(d => d.id === room.orgUnitId)?.deptName || '其他部门'
  return `已分配给${deptName}，需先取消分配`
}

// 选择操作 - 保持ID原始类型，避免精度丢失
const toggleRoomSelection = (roomId: number | string) => {
  const room = rooms.value.find(r => String(r.id) === String(roomId))

  // 检查是否可以选中
  if (room && !canSelectRoom(room)) {
    const reason = getRoomLockedReason(room)
    ElMessage.warning(reason || '该宿舍已分配给其他部门，不可选中')
    return
  }

  const idStr = String(roomId)
  const index = selectedRoomIds.value.findIndex(id => String(id) === idStr)
  if (index === -1) {
    selectedRoomIds.value.push(roomId)
  } else {
    selectedRoomIds.value.splice(index, 1)
  }
}

const toggleFloorSelection = (floor: number) => {
  // 只处理可以选中的房间
  const floorRooms = rooms.value.filter(r => (r.floorNumber || r.floor || 1) === floor)
  const selectableRoomIds = floorRooms.filter(r => canSelectRoom(r)).map(r => r.id)

  if (selectableRoomIds.length === 0) {
    ElMessage.warning('该楼层没有可选中的宿舍')
    return
  }

  const allSelected = selectableRoomIds.every(id => selectedRoomIds.value.some(sid => String(sid) === String(id)))

  if (allSelected) {
    // 取消选择该层所有可选房间
    const floorIdStrs = selectableRoomIds.map(id => String(id))
    selectedRoomIds.value = selectedRoomIds.value.filter(id => !floorIdStrs.includes(String(id)))
  } else {
    // 选中该层所有可选房间
    selectableRoomIds.forEach(id => {
      if (!selectedRoomIds.value.some(sid => String(sid) === String(id))) {
        selectedRoomIds.value.push(id)
      }
    })
  }
}

const isFloorSelected = (floor: number) => {
  const floorRoomIds = rooms.value
    .filter(r => (r.floorNumber || r.floor || 1) === floor)
    .map(r => r.id)
  return floorRoomIds.length > 0 && floorRoomIds.every(id => selectedRoomIds.value.some(sid => String(sid) === String(id)))
}

const isFloorIndeterminate = (floor: number) => {
  const floorRoomIds = rooms.value
    .filter(r => (r.floorNumber || r.floor || 1) === floor)
    .map(r => r.id)
  const selectedCount = floorRoomIds.filter(id => selectedRoomIds.value.some(sid => String(sid) === String(id))).length
  return selectedCount > 0 && selectedCount < floorRoomIds.length
}

const selectAllUnassigned = () => {
  const unassignedIds = rooms.value.filter(r => !r.orgUnitId).map(r => r.id)
  selectedRoomIds.value = unassignedIds
}

const clearSelection = () => {
  selectedRoomIds.value = []
}

// 检查房间是否选中 - 使用字符串比较避免精度丢失
const isRoomSelected = (roomId: number | string) => {
  return selectedRoomIds.value.some(id => String(id) === String(roomId))
}

const selectDepartment = (deptId: number) => {
  targetDepartmentId.value = targetDepartmentId.value === deptId ? null : deptId
}

const toggleDepartmentExpand = (deptId: number) => {
  const index = expandedDeptIds.value.indexOf(deptId)
  if (index === -1) {
    expandedDeptIds.value.push(deptId)
    // 同时选中该部门作为目标
    targetDepartmentId.value = deptId
  } else {
    expandedDeptIds.value.splice(index, 1)
  }
}

// 部门相关
const getDepartmentRoomCount = (deptId: number) => {
  return rooms.value.filter(r => r.orgUnitId === deptId).length
}

const getDepartmentRooms = (deptId: number) => {
  return rooms.value.filter(r => r.orgUnitId === deptId)
}

// 获取部门下按楼层分组的宿舍
const getDepartmentRoomsByFloor = (deptId: number) => {
  const deptRooms = rooms.value.filter(r => r.orgUnitId === deptId)
  if (deptRooms.length === 0) return []

  const floorMap = new Map<number, { floor: number; rooms: any[] }>()

  deptRooms.forEach(room => {
    const floorNum = room.floorNumber || room.floor || 1
    if (!floorMap.has(floorNum)) {
      floorMap.set(floorNum, { floor: floorNum, rooms: [] })
    }
    floorMap.get(floorNum)!.rooms.push(room)
  })

  const result = Array.from(floorMap.values())
  // 按楼层从高到低排序
  result.sort((a, b) => b.floor - a.floor)

  // 每层内按房间号排序
  result.forEach(floor => {
    floor.rooms.sort((a, b) => {
      const aNo = a.dormitoryNo || a.roomNo || ''
      const bNo = b.dormitoryNo || b.roomNo || ''
      return aNo.localeCompare(bNo, 'zh-CN', { numeric: true })
    })
  })

  return result
}

// 拖拽操作
const handleDragStart = (event: DragEvent, room: any) => {
  isDragging.value = true

  // 如果拖拽的房间不在选中列表中，只拖拽当前房间
  if (isRoomSelected(room.id)) {
    draggedRoomIds.value = [...selectedRoomIds.value] as number[]
  } else {
    draggedRoomIds.value = [room.id]
  }

  // 设置拖拽数据
  event.dataTransfer?.setData('text/plain', JSON.stringify(draggedRoomIds.value))
}

const handleDragEnd = () => {
  isDragging.value = false
  dragOverDeptId.value = null
  dragOverUnassigned.value = false
  draggedRoomIds.value = []
}

const handleDragOver = (event: DragEvent, deptId: number) => {
  event.preventDefault()
  dragOverDeptId.value = deptId
  dragOverUnassigned.value = false
}

const handleDragLeave = (event: DragEvent) => {
  // 检查是否真的离开了容器（而不是移动到子元素）
  const target = event.currentTarget as HTMLElement
  const relatedTarget = event.relatedTarget as HTMLElement
  if (target && relatedTarget && target.contains(relatedTarget)) {
    return // 仍在容器内，不处理
  }
  dragOverDeptId.value = null
}

const handleDrop = async (event: DragEvent, deptId: number) => {
  event.preventDefault()
  dragOverDeptId.value = null
  isDragging.value = false

  if (draggedRoomIds.value.length === 0) return

  await assignRoomsToDepartment(draggedRoomIds.value, deptId)
  draggedRoomIds.value = []
}

// 未分配区域拖拽处理
const handleDragOverUnassigned = (event: DragEvent) => {
  event.preventDefault()
  dragOverUnassigned.value = true
  dragOverDeptId.value = null
}

const handleDragLeaveUnassigned = (event: DragEvent) => {
  // 检查是否真的离开了容器（而不是移动到子元素）
  const target = event.currentTarget as HTMLElement
  const relatedTarget = event.relatedTarget as HTMLElement
  if (target && relatedTarget && target.contains(relatedTarget)) {
    return // 仍在容器内，不处理
  }
  dragOverUnassigned.value = false
}

const handleDropToUnassigned = async (event: DragEvent) => {
  event.preventDefault()
  dragOverUnassigned.value = false
  isDragging.value = false

  if (draggedRoomIds.value.length === 0) return

  // 只取消已分配的宿舍
  const assignedRoomIds = draggedRoomIds.value.filter(id => {
    const room = rooms.value.find(r => String(r.id) === String(id))
    return room && room.orgUnitId
  })

  if (assignedRoomIds.length === 0) {
    ElMessage.warning('所选宿舍都未分配，无需取消')
    draggedRoomIds.value = []
    return
  }

  await assignRoomsToDepartment(assignedRoomIds, null)
  draggedRoomIds.value = []
}

// 分配操作
const assignToSelectedDepartment = async () => {
  if (selectedRoomIds.value.length === 0 || !targetDepartmentId.value) return
  await assignRoomsToDepartment(selectedRoomIds.value, targetDepartmentId.value)
}

const unassignSelected = async () => {
  if (selectedRoomIds.value.length === 0) return
  await assignRoomsToDepartment(selectedRoomIds.value, null)
}

// 标记是否有数据变更
const hasChanges = ref(false)

const assignRoomsToDepartment = async (roomIds: (number | string)[], deptId: number | null) => {
  try {
    // 将ID转换为字符串，避免大整数精度丢失
    // 后端BatchUpdateDepartmentRequest接收List<String>
    const stringIds = roomIds.map(id => String(id))
    await batchUpdateDepartment({
      dormitoryIds: stringIds,
      orgUnitId: deptId
    })

    // 更新本地数据
    const deptName = deptId
      ? departmentOptions.value.find(d => d.id === deptId)?.deptName
      : null

    const roomIdStrs = roomIds.map(id => String(id))
    rooms.value = rooms.value.map(room => {
      if (roomIdStrs.includes(String(room.id))) {
        return {
          ...room,
          orgUnitId: deptId,
          orgUnitName: deptName
        }
      }
      return room
    })

    // 清除选择
    selectedRoomIds.value = selectedRoomIds.value.filter(id => !roomIdStrs.includes(String(id)))

    // 标记有数据变更
    hasChanges.value = true

    ElMessage.success(deptId ? `成功分配 ${roomIds.length} 间宿舍` : `成功取消 ${roomIds.length} 间宿舍的分配`)
    // 不再立即触发 success，让用户可以继续操作
    // emit('success')
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

// 样式
const getRoomTagClass = (room: any) => {
  // 已选中状态
  if (isRoomSelected(room.id)) {
    return 'bg-teal-500 text-white ring-2 ring-teal-300 ring-offset-1'
  }

  // 不可选中状态（已分配给其他部门）
  if (!canSelectRoom(room)) {
    return 'bg-orange-100 text-orange-400 cursor-not-allowed opacity-60'
  }

  // 已分配状态
  if (room.orgUnitId) {
    return 'bg-blue-100 text-blue-700 hover:bg-blue-200'
  }

  // 未分配状态
  return 'bg-gray-100 text-gray-600 hover:bg-gray-200'
}

const getRoomTooltip = (room: any) => {
  const roomNo = room.dormitoryNo || room.roomNo
  const beds = room.bedCount || room.maxOccupancy || 0
  const occupied = room.occupiedBeds || room.currentOccupancy || 0
  const dept = room.orgUnitName || '未分配'

  // 如果不可选，显示锁定原因
  const lockedReason = getRoomLockedReason(room)
  if (lockedReason) {
    return `${roomNo} | ${lockedReason}`
  }

  return `${roomNo} | ${beds}人间 | 入住${occupied}人 | ${dept}`
}

const getRoomCardClass = (room: any) => {
  if (room.orgUnitId) {
    return 'border-blue-300 bg-blue-50 hover:border-blue-400'
  }
  return 'border-gray-200 bg-white hover:border-teal-400'
}

const getRoomTextClass = (room: any) => {
  if (room.orgUnitId) {
    return 'text-blue-700'
  }
  return 'text-gray-700'
}

const getDepartmentCardClass = (deptId: number) => {
  if (targetDepartmentId.value === deptId) {
    return 'border-blue-500 bg-blue-50 cursor-pointer'
  }
  return 'border-gray-200 bg-white hover:border-blue-300 cursor-pointer'
}

// 关闭
const handleClose = () => {
  emit('update:visible', false)
  emit('close')
  // 如果有数据变更，通知父组件刷新
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
    targetDepartmentId.value = null
    expandedDeptIds.value = []
    hasChanges.value = false  // 重置变更标记
  }
})

// 当目标部门改变时，清除无效的选择
watch(targetDepartmentId, () => {
  // 过滤掉不再可选的房间
  selectedRoomIds.value = selectedRoomIds.value.filter(id => {
    const room = rooms.value.find(r => String(r.id) === String(id))
    return room && canSelectRoom(room)
  })
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
