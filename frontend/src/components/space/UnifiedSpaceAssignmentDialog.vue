<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="visible" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="fixed inset-0 bg-black/50" @click="handleClose"></div>
        <div class="relative flex max-h-[90vh] w-full max-w-6xl flex-col rounded-xl bg-white shadow-2xl">
          <!-- Header -->
          <div class="flex items-center justify-between border-b border-gray-200 px-6 py-4">
            <div>
              <h3 class="text-lg font-semibold text-gray-900">{{ buildingName }} - 空间分配</h3>
              <p class="mt-0.5 text-sm text-gray-500">从左侧选择空间，拖拽或点击箭头分配到右侧部门</p>
            </div>
            <button @click="handleClose" class="rounded-lg p-2 text-gray-400 hover:bg-gray-100 hover:text-gray-600">
              <X class="h-5 w-5" />
            </button>
          </div>

          <!-- Room Type Tabs -->
          <div class="flex items-center gap-1 border-b border-gray-100 bg-gray-50 px-4 py-2">
            <button
              v-for="tab in roomTypeTabs"
              :key="tab.value"
              @click="filterRoomType = tab.value"
              class="flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm font-medium transition-all"
              :class="filterRoomType === tab.value
                ? 'bg-white text-gray-900 shadow-sm'
                : 'text-gray-500 hover:bg-white/50 hover:text-gray-700'"
            >
              <component :is="getRoomTypeIcon(tab.value)" class="h-4 w-4" />
              {{ tab.label }}
              <span
                class="rounded-full px-1.5 py-0.5 text-[10px]"
                :class="filterRoomType === tab.value ? 'bg-gray-100 text-gray-600' : 'bg-gray-200/50 text-gray-500'"
              >
                {{ getRoomTypeCount(tab.value) }}
              </span>
            </button>
          </div>

          <!-- Main Content: Dual Panel Layout -->
          <div class="flex flex-1 overflow-hidden">
            <!-- Left Panel: Space List -->
            <div class="flex w-1/2 flex-col border-r border-gray-200">
              <!-- Left Header -->
              <div class="border-b border-gray-100 bg-gray-50 px-4 py-3">
                <div class="flex items-center justify-between">
                  <div class="flex items-center gap-2">
                    <LayoutGrid class="h-5 w-5 text-indigo-600" />
                    <span class="font-medium text-gray-900">空间列表</span>
                    <span class="rounded-full bg-gray-200 px-2 py-0.5 text-xs text-gray-600">
                      {{ filteredSpaces.length }} 间
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
                <!-- Filters -->
                <div class="mt-2 flex items-center gap-2">
                  <select
                    v-model="filterFloor"
                    class="h-8 rounded-lg border border-gray-200 px-2 text-sm focus:border-indigo-500 focus:outline-none"
                  >
                    <option :value="null">全部楼层</option>
                    <option v-for="floor in floorList" :key="floor" :value="floor">
                      {{ floor }}层
                    </option>
                  </select>
                  <select
                    v-model="filterAssigned"
                    class="h-8 rounded-lg border border-gray-200 px-2 text-sm focus:border-indigo-500 focus:outline-none"
                  >
                    <option value="all">全部状态</option>
                    <option value="unassigned">未分配</option>
                    <option value="assigned">已分配</option>
                  </select>
                </div>
              </div>

              <!-- Left Content: Space List (Compact Layout) -->
              <div class="flex-1 overflow-y-auto p-3">
                <div v-if="loading" class="flex items-center justify-center py-20">
                  <Loader2 class="h-8 w-8 animate-spin text-indigo-500" />
                </div>

                <div v-else-if="filteredSpaces.length === 0" class="py-20 text-center text-gray-400">
                  <LayoutGrid class="mx-auto h-12 w-12" />
                  <p class="mt-2">暂无空间</p>
                </div>

                <div v-else class="space-y-2">
                  <div
                    v-for="floor in groupedSpaces"
                    :key="floor.floor"
                    class="rounded-lg border border-gray-200 bg-white"
                  >
                    <!-- Floor Header (Compact) -->
                    <div
                      class="flex cursor-pointer items-center justify-between bg-gray-50 px-2 py-1.5"
                      @click="toggleFloorSelection(floor.floor)"
                    >
                      <div class="flex items-center gap-1.5">
                        <input
                          type="checkbox"
                          :checked="isFloorSelected(floor.floor)"
                          :indeterminate="isFloorIndeterminate(floor.floor)"
                          class="h-3.5 w-3.5 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                          @click.stop="toggleFloorSelection(floor.floor)"
                        />
                        <span class="rounded bg-indigo-100 px-1.5 py-0.5 text-[10px] font-bold text-indigo-700">
                          {{ floor.floor }}F
                        </span>
                        <span class="text-xs text-gray-500">{{ floor.spaces.length }}间</span>
                      </div>
                      <div class="flex items-center gap-1.5 text-[10px]">
                        <span class="text-blue-600">{{ floor.assignedCount }}已分</span>
                        <span class="text-gray-400">{{ floor.spaces.length - floor.assignedCount }}未分</span>
                      </div>
                    </div>

                    <!-- Space Tags (Compact Grid) -->
                    <div class="flex flex-wrap gap-1 p-2">
                      <div
                        v-for="space in floor.spaces"
                        :key="space.id"
                        :draggable="true"
                        class="group relative flex cursor-pointer select-none items-center gap-1 rounded px-1.5 py-0.5 text-xs font-medium transition-all"
                        :class="getSpaceTagClass(space)"
                        :title="getSpaceTooltip(space)"
                        @dragstart="handleDragStart($event, space)"
                        @dragend="handleDragEnd"
                        @click="toggleSpaceSelection(space.id)"
                      >
                        <component :is="getSpaceIcon(space)" class="h-3 w-3" />
                        {{ space.spaceName || space.spaceCode }}
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Left Footer: Selected Hint -->
              <div v-if="selectedSpaceIds.length > 0" class="border-t border-gray-200 bg-indigo-50 px-4 py-3">
                <div class="flex items-center justify-between">
                  <span class="text-sm text-indigo-700">
                    已选择 <strong>{{ selectedSpaceIds.length }}</strong> 间
                    <template v-if="selectedTypeStats.length > 0">
                      (<span v-for="(stat, idx) in selectedTypeStats" :key="stat.type">
                        {{ stat.label }}:{{ stat.count }}<span v-if="idx < selectedTypeStats.length - 1">, </span>
                      </span>)
                    </template>
                  </span>
                  <button
                    @click="clearSelection"
                    class="text-xs text-indigo-600 hover:text-indigo-700"
                  >
                    清除选择
                  </button>
                </div>
              </div>
            </div>

            <!-- Middle: Arrow Operation Area -->
            <div class="flex w-16 flex-col items-center justify-center gap-3 bg-gray-50">
              <button
                @click="assignToSelectedDepartment"
                :disabled="selectedSpaceIds.length === 0 || !targetOrgUnitId"
                class="flex h-10 w-10 items-center justify-center rounded-full bg-indigo-600 text-white shadow-lg transition-all hover:bg-indigo-700 disabled:cursor-not-allowed disabled:bg-gray-300 disabled:shadow-none"
                title="分配到选中部门"
              >
                <ChevronRight class="h-5 w-5" />
              </button>
              <button
                @click="unassignSelected"
                :disabled="selectedSpaceIds.length === 0"
                class="flex h-10 w-10 items-center justify-center rounded-full border-2 border-gray-300 bg-white text-gray-500 transition-all hover:border-red-300 hover:bg-red-50 hover:text-red-500 disabled:cursor-not-allowed disabled:opacity-50"
                title="取消分配"
              >
                <ChevronLeft class="h-5 w-5" />
              </button>
            </div>

            <!-- Right Panel: Department List -->
            <div class="flex w-1/2 flex-col">
              <!-- Right Header -->
              <div class="border-b border-gray-100 bg-gray-50 px-4 py-3">
                <div class="flex items-center gap-2">
                  <Building2 class="h-5 w-5 text-blue-600" />
                  <span class="font-medium text-gray-900">部门</span>
                  <span class="rounded-full bg-gray-200 px-2 py-0.5 text-xs text-gray-600">
                    {{ orgUnitOptions.length }} 个
                  </span>
                </div>
                <p class="mt-1 text-xs text-gray-500">点击部门展开空间列表，拖放空间到部门区域完成分配</p>
              </div>

              <!-- Right Content: Department Cards -->
              <div class="flex-1 overflow-y-auto">
                <!-- Unassigned Area (Sticky at Top) -->
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
                          拖拽到此处取消分配 | {{ unassignedSpacesCount }} 间
                        </div>
                      </div>
                    </div>
                    <!-- Drop Hint -->
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
                    v-for="orgUnit in orgUnitOptions"
                    :key="orgUnit.id"
                    class="rounded-lg border-2 transition-all"
                    :class="[
                      getOrgUnitCardClass(orgUnit.id),
                      dragOverOrgUnitId === orgUnit.id ? 'scale-[1.01] border-indigo-500 bg-indigo-50 shadow-lg' : ''
                    ]"
                    @dragover.prevent="handleDragOver($event, orgUnit.id)"
                    @dragleave="handleDragLeave"
                    @drop="handleDrop($event, orgUnit.id)"
                  >
                    <!-- Department Header (Clickable to Select/Expand) -->
                    <div
                      class="flex cursor-pointer items-center justify-between px-3 py-2"
                      @click="toggleOrgUnitExpand(orgUnit.id)"
                    >
                      <div class="flex items-center gap-2">
                        <div
                          class="flex h-8 w-8 items-center justify-center rounded-lg text-sm font-bold"
                          :class="targetOrgUnitId === orgUnit.id ? 'bg-blue-600 text-white' : 'bg-blue-100 text-blue-600'"
                        >
                          {{ orgUnit.unitName.charAt(0) }}
                        </div>
                        <div>
                          <div class="text-sm font-medium text-gray-900">{{ orgUnit.unitName }}</div>
                          <div class="text-[10px] text-gray-500">
                            {{ getOrgUnitSpaceSummary(orgUnit.id) }}
                          </div>
                        </div>
                      </div>
                      <div class="flex items-center gap-2">
                        <div v-if="targetOrgUnitId === orgUnit.id" class="flex items-center gap-1 rounded-full bg-blue-100 px-2 py-0.5 text-blue-600">
                          <Check class="h-3.5 w-3.5" />
                          <span class="text-[10px] font-medium">目标</span>
                        </div>
                        <component
                          :is="expandedOrgUnitIds.includes(orgUnit.id) ? ChevronUp : ChevronDown"
                          class="h-4 w-4 text-gray-400"
                        />
                      </div>
                    </div>

                    <!-- Expanded Space List (Grouped by Floor) -->
                    <div
                      v-if="expandedOrgUnitIds.includes(orgUnit.id)"
                      class="border-t border-gray-100 bg-gray-50/50"
                    >
                      <div v-if="getOrgUnitSpacesByFloor(orgUnit.id).length > 0" class="space-y-0.5">
                        <!-- Grouped by Floor -->
                        <div
                          v-for="floor in getOrgUnitSpacesByFloor(orgUnit.id)"
                          :key="floor.floor"
                          class="border-b border-gray-100 last:border-b-0"
                        >
                          <!-- Floor Title -->
                          <div class="flex items-center gap-2 bg-blue-50/50 px-2 py-1">
                            <span class="rounded bg-blue-200 px-1.5 py-0.5 text-[10px] font-bold text-blue-700">
                              {{ floor.floor }}F
                            </span>
                            <span class="text-[10px] text-gray-500">{{ floor.spaces.length }}间</span>
                          </div>
                          <!-- Floor Space List -->
                          <div class="flex flex-wrap gap-1 p-1.5">
                            <div
                              v-for="space in floor.spaces"
                              :key="space.id"
                              :draggable="true"
                              class="flex cursor-pointer items-center gap-1 rounded px-1.5 py-0.5 text-xs font-medium transition-colors"
                              :class="[
                                isSpaceSelected(space.id)
                                  ? 'ring-2 ring-indigo-400 ring-offset-1 bg-indigo-500 text-white'
                                  : getSpaceInOrgUnitClass(space)
                              ]"
                              :title="getSpaceTooltip(space)"
                              @dragstart="handleDragStart($event, space)"
                              @dragend="handleDragEnd"
                              @click.stop="toggleSpaceSelection(space.id)"
                            >
                              <component :is="getSpaceIcon(space)" class="h-3 w-3" />
                              {{ space.spaceName || space.spaceCode }}
                            </div>
                          </div>
                        </div>
                      </div>
                      <div v-else class="py-2 text-center text-xs text-gray-400">
                        暂无空间
                      </div>
                    </div>

                    <!-- Drop Hint -->
                    <div
                      v-if="isDragging && dragOverOrgUnitId === orgUnit.id"
                      class="pointer-events-none border-t border-indigo-200 bg-indigo-100 py-2 text-center text-xs font-medium text-indigo-600"
                    >
                      松开鼠标完成分配
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Footer -->
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
                <div class="h-3 w-3 rounded bg-indigo-500"></div>
                <span>选中</span>
              </div>
              <div class="flex items-center gap-2">
                <div class="h-3 w-3 rounded bg-orange-300 opacity-60"></div>
                <span>锁定</span>
              </div>
            </div>
            <div class="flex items-center gap-3">
              <span class="text-sm text-gray-500">
                共 {{ spaces.length }} 间 | 已分配 {{ assignedSpacesCount }} 间
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
  LayoutGrid,
  ChevronRight,
  ChevronLeft,
  ChevronUp,
  ChevronDown,
  Check,
  Bed,
  School,
  FlaskConical,
  Monitor,
  Wrench,
  Users,
  Briefcase,
  Package,
  MoreHorizontal
} from 'lucide-vue-next'
import { querySpaces, batchAssignOrgUnit } from '@/api/space'
import { getOrgUnitsByType } from '@/api/organization'
import type { SpaceDTO, RoomType } from '@/types/space'
import { ROOM_TYPE_CONFIG, getRoomTypeConfig } from '@/types/space'

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

// State
const loading = ref(false)
const spaces = ref<SpaceDTO[]>([])
const orgUnitOptions = ref<Array<{ id: number; unitName: string }>>([])

// Selection state
const selectedSpaceIds = ref<(number | string)[]>([])
const targetOrgUnitId = ref<number | null>(null)
const expandedOrgUnitIds = ref<number[]>([])

// Filter state
const filterFloor = ref<number | null>(null)
const filterAssigned = ref<'all' | 'unassigned' | 'assigned'>('all')
const filterRoomType = ref<RoomType | 'ALL'>('ALL')

// Drag state
const isDragging = ref(false)
const dragOverOrgUnitId = ref<number | null>(null)
const dragOverUnassigned = ref(false)
const draggedSpaceIds = ref<number[]>([])

// Track changes
const hasChanges = ref(false)

// Room type tabs
const roomTypeTabs = computed(() => {
  const tabs: Array<{ value: RoomType | 'ALL'; label: string }> = [
    { value: 'ALL', label: '全部' }
  ]
  // Add only room types that exist in the building
  const existingTypes = new Set(spaces.value.map(s => s.roomType).filter(Boolean))
  for (const [type, config] of Object.entries(ROOM_TYPE_CONFIG)) {
    if (existingTypes.has(type as RoomType)) {
      tabs.push({ value: type as RoomType, label: config.label })
    }
  }
  return tabs
})

// Get room type count
const getRoomTypeCount = (type: RoomType | 'ALL') => {
  if (type === 'ALL') return spaces.value.length
  return spaces.value.filter(s => s.roomType === type).length
}

// Get room type icon component
const getRoomTypeIcon = (type: RoomType | 'ALL') => {
  if (type === 'ALL') return LayoutGrid
  const iconMap: Record<string, any> = {
    Bed, School, FlaskConical, Monitor, Wrench, Users, Briefcase, Package, MoreHorizontal
  }
  const config = ROOM_TYPE_CONFIG[type]
  return config ? iconMap[config.icon] || LayoutGrid : LayoutGrid
}

// Floor list
const floorList = computed(() => {
  const floors = new Set<number>()
  spaces.value.forEach(space => {
    const floor = space.floorNumber || 1
    floors.add(floor)
  })
  return Array.from(floors).sort((a, b) => a - b)
})

// Filtered spaces
const filteredSpaces = computed(() => {
  let result = [...spaces.value]

  // Filter by room type
  if (filterRoomType.value !== 'ALL') {
    result = result.filter(s => s.roomType === filterRoomType.value)
  }

  // Filter by floor
  if (filterFloor.value !== null) {
    result = result.filter(s => (s.floorNumber || 1) === filterFloor.value)
  }

  // Filter by assigned status
  if (filterAssigned.value === 'unassigned') {
    result = result.filter(s => !s.orgUnitId)
  } else if (filterAssigned.value === 'assigned') {
    result = result.filter(s => s.orgUnitId)
  }

  return result
})

// Grouped spaces by floor
const groupedSpaces = computed(() => {
  const floorMap = new Map<number, { floor: number; spaces: SpaceDTO[]; assignedCount: number }>()

  filteredSpaces.value.forEach(space => {
    const floorNum = space.floorNumber || 1
    if (!floorMap.has(floorNum)) {
      floorMap.set(floorNum, { floor: floorNum, spaces: [], assignedCount: 0 })
    }
    const floor = floorMap.get(floorNum)!
    floor.spaces.push(space)
    if (space.orgUnitId) {
      floor.assignedCount++
    }
  })

  const result = Array.from(floorMap.values())
  result.sort((a, b) => b.floor - a.floor)

  result.forEach(floor => {
    floor.spaces.sort((a, b) => {
      const aName = a.spaceName || a.spaceCode || ''
      const bName = b.spaceName || b.spaceCode || ''
      return aName.localeCompare(bName, 'zh-CN', { numeric: true })
    })
  })

  return result
})

// Statistics
const assignedSpacesCount = computed(() => spaces.value.filter(s => s.orgUnitId).length)
const unassignedSpacesCount = computed(() => spaces.value.filter(s => !s.orgUnitId).length)

// Selected type statistics
const selectedTypeStats = computed(() => {
  const stats: Record<string, { type: string; label: string; count: number }> = {}
  selectedSpaceIds.value.forEach(id => {
    const space = spaces.value.find(s => String(s.id) === String(id))
    if (space?.roomType) {
      const config = getRoomTypeConfig(space.roomType)
      if (!stats[space.roomType]) {
        stats[space.roomType] = { type: space.roomType, label: config.label, count: 0 }
      }
      stats[space.roomType].count++
    }
  })
  return Object.values(stats)
})

// Load data
const loadData = async () => {
  loading.value = true
  try {
    const [spacesRes, orgUnitsRes] = await Promise.all([
      querySpaces({ buildingId: props.buildingId, spaceType: 'ROOM', pageSize: 1000 }),
      getOrgUnitsByType('DEPARTMENT')
    ])
    spaces.value = spacesRes?.list || []
    orgUnitOptions.value = (orgUnitsRes || []).map((d: any) => ({
      id: d.id,
      unitName: d.unitName
    }))
  } catch (error: any) {
    ElMessage.error(error.message || '加载数据失败')
  } finally {
    loading.value = false
  }
}

// Check if space can be selected
const canSelectSpace = (space: SpaceDTO): boolean => {
  if (!targetOrgUnitId.value) return true
  if (!space.orgUnitId) return true
  if (space.orgUnitId === targetOrgUnitId.value) return true
  return false
}

// Get locked reason
const getLockedReason = (space: SpaceDTO): string | null => {
  if (!targetOrgUnitId.value || !space.orgUnitId) return null
  if (space.orgUnitId === targetOrgUnitId.value) return null
  const orgUnitName = orgUnitOptions.value.find(d => d.id === space.orgUnitId)?.unitName || '其他部门'
  return `已分配给${orgUnitName}，需先取消分配`
}

// Selection operations
const toggleSpaceSelection = (spaceId: number | string) => {
  const space = spaces.value.find(s => String(s.id) === String(spaceId))

  if (space && !canSelectSpace(space)) {
    const reason = getLockedReason(space)
    ElMessage.warning(reason || '该空间已分配给其他部门，不可选中')
    return
  }

  const idStr = String(spaceId)
  const index = selectedSpaceIds.value.findIndex(id => String(id) === idStr)
  if (index === -1) {
    selectedSpaceIds.value.push(spaceId)
  } else {
    selectedSpaceIds.value.splice(index, 1)
  }
}

const toggleFloorSelection = (floor: number) => {
  const floorSpaces = filteredSpaces.value.filter(s => (s.floorNumber || 1) === floor)
  const selectableIds = floorSpaces.filter(s => canSelectSpace(s)).map(s => s.id)

  if (selectableIds.length === 0) {
    ElMessage.warning('该楼层没有可选中的空间')
    return
  }

  const allSelected = selectableIds.every(id => selectedSpaceIds.value.some(sid => String(sid) === String(id)))

  if (allSelected) {
    const floorIdStrs = selectableIds.map(id => String(id))
    selectedSpaceIds.value = selectedSpaceIds.value.filter(id => !floorIdStrs.includes(String(id)))
  } else {
    selectableIds.forEach(id => {
      if (!selectedSpaceIds.value.some(sid => String(sid) === String(id))) {
        selectedSpaceIds.value.push(id)
      }
    })
  }
}

const isFloorSelected = (floor: number) => {
  const floorIds = filteredSpaces.value
    .filter(s => (s.floorNumber || 1) === floor)
    .map(s => s.id)
  return floorIds.length > 0 && floorIds.every(id => selectedSpaceIds.value.some(sid => String(sid) === String(id)))
}

const isFloorIndeterminate = (floor: number) => {
  const floorIds = filteredSpaces.value
    .filter(s => (s.floorNumber || 1) === floor)
    .map(s => s.id)
  const selectedCount = floorIds.filter(id => selectedSpaceIds.value.some(sid => String(sid) === String(id))).length
  return selectedCount > 0 && selectedCount < floorIds.length
}

const selectAllUnassigned = () => {
  let unassigned = spaces.value.filter(s => !s.orgUnitId)
  if (filterRoomType.value !== 'ALL') {
    unassigned = unassigned.filter(s => s.roomType === filterRoomType.value)
  }
  selectedSpaceIds.value = unassigned.map(s => s.id)
}

const clearSelection = () => {
  selectedSpaceIds.value = []
}

const isSpaceSelected = (spaceId: number | string) => {
  return selectedSpaceIds.value.some(id => String(id) === String(spaceId))
}

const toggleOrgUnitExpand = (orgUnitId: number) => {
  const index = expandedOrgUnitIds.value.indexOf(orgUnitId)
  if (index === -1) {
    expandedOrgUnitIds.value.push(orgUnitId)
    targetOrgUnitId.value = orgUnitId
  } else {
    expandedOrgUnitIds.value.splice(index, 1)
  }
}

// OrgUnit related
const getOrgUnitSpaceCount = (orgUnitId: number) => {
  return spaces.value.filter(s => s.orgUnitId === orgUnitId).length
}

const getOrgUnitSpaceSummary = (orgUnitId: number) => {
  const orgSpaces = spaces.value.filter(s => s.orgUnitId === orgUnitId)
  if (orgSpaces.length === 0) return '暂无空间'

  const typeStats: Record<string, number> = {}
  orgSpaces.forEach(s => {
    if (s.roomType) {
      const config = getRoomTypeConfig(s.roomType)
      typeStats[config.label] = (typeStats[config.label] || 0) + 1
    }
  })

  return Object.entries(typeStats)
    .map(([label, count]) => `${count}间${label}`)
    .join(' ')
}

const getOrgUnitSpacesByFloor = (orgUnitId: number) => {
  const orgSpaces = spaces.value.filter(s => s.orgUnitId === orgUnitId)
  if (orgSpaces.length === 0) return []

  const floorMap = new Map<number, { floor: number; spaces: SpaceDTO[] }>()

  orgSpaces.forEach(space => {
    const floorNum = space.floorNumber || 1
    if (!floorMap.has(floorNum)) {
      floorMap.set(floorNum, { floor: floorNum, spaces: [] })
    }
    floorMap.get(floorNum)!.spaces.push(space)
  })

  const result = Array.from(floorMap.values())
  result.sort((a, b) => b.floor - a.floor)

  result.forEach(floor => {
    floor.spaces.sort((a, b) => {
      const aName = a.spaceName || a.spaceCode || ''
      const bName = b.spaceName || b.spaceCode || ''
      return aName.localeCompare(bName, 'zh-CN', { numeric: true })
    })
  })

  return result
}

// Drag operations
const handleDragStart = (event: DragEvent, space: SpaceDTO) => {
  isDragging.value = true

  if (isSpaceSelected(space.id)) {
    draggedSpaceIds.value = [...selectedSpaceIds.value] as number[]
  } else {
    draggedSpaceIds.value = [space.id]
  }

  event.dataTransfer?.setData('text/plain', JSON.stringify(draggedSpaceIds.value))
}

const handleDragEnd = () => {
  isDragging.value = false
  dragOverOrgUnitId.value = null
  dragOverUnassigned.value = false
  draggedSpaceIds.value = []
}

const handleDragOver = (event: DragEvent, orgUnitId: number) => {
  event.preventDefault()
  dragOverOrgUnitId.value = orgUnitId
  dragOverUnassigned.value = false
}

const handleDragLeave = (event: DragEvent) => {
  const target = event.currentTarget as HTMLElement
  const relatedTarget = event.relatedTarget as HTMLElement
  if (target && relatedTarget && target.contains(relatedTarget)) {
    return
  }
  dragOverOrgUnitId.value = null
}

const handleDrop = async (event: DragEvent, orgUnitId: number) => {
  event.preventDefault()
  dragOverOrgUnitId.value = null
  isDragging.value = false

  if (draggedSpaceIds.value.length === 0) return

  await assignSpacesToOrgUnit(draggedSpaceIds.value, orgUnitId)
  draggedSpaceIds.value = []
}

const handleDragOverUnassigned = (event: DragEvent) => {
  event.preventDefault()
  dragOverUnassigned.value = true
  dragOverOrgUnitId.value = null
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

  if (draggedSpaceIds.value.length === 0) return

  const assignedIds = draggedSpaceIds.value.filter(id => {
    const space = spaces.value.find(s => String(s.id) === String(id))
    return space && space.orgUnitId
  })

  if (assignedIds.length === 0) {
    ElMessage.warning('所选空间都未分配，无需取消')
    draggedSpaceIds.value = []
    return
  }

  await assignSpacesToOrgUnit(assignedIds, null)
  draggedSpaceIds.value = []
}

// Assignment operations
const assignToSelectedDepartment = async () => {
  if (selectedSpaceIds.value.length === 0 || !targetOrgUnitId.value) return
  await assignSpacesToOrgUnit(selectedSpaceIds.value, targetOrgUnitId.value)
}

const unassignSelected = async () => {
  if (selectedSpaceIds.value.length === 0) return
  await assignSpacesToOrgUnit(selectedSpaceIds.value, null)
}

const assignSpacesToOrgUnit = async (spaceIds: (number | string)[], orgUnitId: number | null) => {
  try {
    // Convert to numbers for API
    const numericIds = spaceIds.map(id => Number(id))

    if (orgUnitId) {
      await batchAssignOrgUnit({
        spaceIds: numericIds,
        orgUnitId: orgUnitId
      })
    } else {
      // For unassigning, we need to call with orgUnitId = 0 or use a different endpoint
      // Based on the existing API, we'll set orgUnitId to 0 to clear
      await batchAssignOrgUnit({
        spaceIds: numericIds,
        orgUnitId: 0
      })
    }

    // Update local data
    const orgUnitName = orgUnitId
      ? orgUnitOptions.value.find(d => d.id === orgUnitId)?.unitName
      : undefined

    const spaceIdStrs = spaceIds.map(id => String(id))
    spaces.value = spaces.value.map(space => {
      if (spaceIdStrs.includes(String(space.id))) {
        return {
          ...space,
          orgUnitId: orgUnitId || undefined,
          orgUnitName: orgUnitName
        }
      }
      return space
    })

    // Clear selection
    selectedSpaceIds.value = selectedSpaceIds.value.filter(id => !spaceIdStrs.includes(String(id)))

    hasChanges.value = true

    ElMessage.success(orgUnitId ? `成功分配 ${spaceIds.length} 间空间` : `成功取消 ${spaceIds.length} 间空间的分配`)
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

// Styles
const getSpaceTagClass = (space: SpaceDTO) => {
  if (isSpaceSelected(space.id)) {
    return 'bg-indigo-500 text-white ring-2 ring-indigo-300 ring-offset-1'
  }

  if (!canSelectSpace(space)) {
    return 'bg-orange-100 text-orange-400 cursor-not-allowed opacity-60'
  }

  if (space.orgUnitId) {
    const config = space.roomType ? getRoomTypeConfig(space.roomType) : null
    return config ? `${config.bgColor} ${config.textColor} hover:opacity-80` : 'bg-blue-100 text-blue-700 hover:bg-blue-200'
  }

  return 'bg-gray-100 text-gray-600 hover:bg-gray-200'
}

const getSpaceInOrgUnitClass = (space: SpaceDTO) => {
  const config = space.roomType ? getRoomTypeConfig(space.roomType) : null
  return config ? `${config.bgColor} ${config.textColor} hover:opacity-80` : 'bg-blue-100 text-blue-700 hover:bg-blue-200'
}

const getSpaceTooltip = (space: SpaceDTO) => {
  const name = space.spaceName || space.spaceCode
  const type = space.roomType ? getRoomTypeConfig(space.roomType).label : '未知'
  const capacity = space.capacity || 0
  const occupancy = space.currentOccupancy || 0
  const orgUnit = space.orgUnitName || '未分配'

  const lockedReason = getLockedReason(space)
  if (lockedReason) {
    return `${name} | ${lockedReason}`
  }

  return `${name} | ${type} | 容量${capacity} | 已用${occupancy} | ${orgUnit}`
}

const getSpaceIcon = (space: SpaceDTO) => {
  if (!space.roomType) return LayoutGrid
  const iconMap: Record<string, any> = {
    Bed, School, FlaskConical, Monitor, Wrench, Users, Briefcase, Package, MoreHorizontal
  }
  const config = ROOM_TYPE_CONFIG[space.roomType]
  return config ? iconMap[config.icon] || LayoutGrid : LayoutGrid
}

const getOrgUnitCardClass = (orgUnitId: number) => {
  if (targetOrgUnitId.value === orgUnitId) {
    return 'border-blue-500 bg-blue-50 cursor-pointer'
  }
  return 'border-gray-200 bg-white hover:border-blue-300 cursor-pointer'
}

// Close handler
const handleClose = () => {
  emit('update:visible', false)
  emit('close')
  if (hasChanges.value) {
    emit('success')
    hasChanges.value = false
  }
}

// Watchers
watch(() => props.visible, (val) => {
  if (val) {
    loadData()
    selectedSpaceIds.value = []
    targetOrgUnitId.value = null
    expandedOrgUnitIds.value = []
    filterRoomType.value = 'ALL'
    filterFloor.value = null
    filterAssigned.value = 'all'
    hasChanges.value = false
  }
})

watch(targetOrgUnitId, () => {
  selectedSpaceIds.value = selectedSpaceIds.value.filter(id => {
    const space = spaces.value.find(s => String(s.id) === String(id))
    return space && canSelectSpace(space)
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
