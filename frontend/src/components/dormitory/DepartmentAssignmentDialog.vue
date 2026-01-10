<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="visible" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="fixed inset-0 bg-black/50" @click="handleClose"></div>
        <div class="relative flex max-h-[90vh] w-full max-w-5xl flex-col rounded-lg bg-white shadow-xl">
          <!-- 头部 -->
          <div class="flex items-center justify-between border-b border-gray-200 px-6 py-4">
            <div>
              <h3 class="text-lg font-semibold text-gray-900">{{ buildingName }} - 院系分配</h3>
              <p class="mt-0.5 text-sm text-gray-500">点击楼层标签可整层分配，点击单个房间可单独分配</p>
            </div>
            <button @click="handleClose" class="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-600">
              <X class="h-5 w-5" />
            </button>
          </div>

          <!-- 院系选择器和图例 -->
          <div class="border-b border-gray-200 bg-gray-50 px-6 py-3">
            <div class="flex flex-wrap items-center gap-4">
              <div class="flex items-center gap-2">
                <label class="text-sm font-medium text-gray-700">选择院系：</label>
                <select
                  v-model="selectedDepartmentId"
                  class="h-9 w-48 rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                >
                  <option :value="null">取消分配</option>
                  <option v-for="dept in departmentOptions" :key="dept.id" :value="dept.id">
                    {{ dept.deptName }}
                  </option>
                </select>
              </div>
              <div class="flex items-center gap-4 text-xs">
                <span class="font-medium text-gray-600">图例：</span>
                <div class="flex items-center gap-1">
                  <div class="h-3 w-3 rounded border border-gray-300 bg-white"></div>
                  <span class="text-gray-600">未分配</span>
                </div>
                <div class="flex items-center gap-1">
                  <div class="h-3 w-3 rounded bg-blue-500"></div>
                  <span class="text-gray-600">已分配</span>
                </div>
                <div class="flex items-center gap-1">
                  <div class="h-3 w-3 rounded bg-emerald-500"></div>
                  <span class="text-gray-600">当前选中院系</span>
                </div>
              </div>
              <div class="ml-auto text-sm text-gray-500">
                共 {{ totalRooms }} 间 | 已分配 {{ assignedRooms }} 间
              </div>
            </div>
          </div>

          <!-- 楼层内容 -->
          <div class="flex-1 overflow-y-auto p-6">
            <div v-if="loading" class="py-20 text-center">
              <Loader2 class="mx-auto h-8 w-8 animate-spin text-blue-500" />
              <p class="mt-2 text-sm text-gray-500">加载中...</p>
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
                      已分配 {{ floor.assignedCount }} 间
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
                    class="group relative cursor-pointer rounded-lg border-2 px-3 py-2 text-center transition-all hover:shadow-md"
                    :class="getRoomClass(room)"
                    @click="handleRoomClick(room)"
                  >
                    <div class="text-sm font-medium" :class="getRoomTextClass(room)">
                      {{ room.dormitoryNo || room.roomNo }}
                    </div>
                    <div class="text-[10px]" :class="getRoomSubTextClass(room)">
                      {{ room.departmentName || '未分配' }}
                    </div>
                    <!-- 悬浮提示 -->
                    <div class="pointer-events-none absolute bottom-full left-1/2 z-10 mb-2 hidden -translate-x-1/2 whitespace-nowrap rounded-lg bg-gray-900 px-3 py-2 text-xs text-white shadow-lg group-hover:block">
                      <div class="font-medium">{{ room.dormitoryNo || room.roomNo }}</div>
                      <div class="mt-1 text-gray-300">
                        {{ room.bedCount || room.maxOccupancy || 0 }}人间 · {{ room.occupiedBeds || room.currentOccupancy || 0 }}人入住
                      </div>
                      <div v-if="room.departmentName" class="mt-1 text-emerald-400">
                        院系: {{ room.departmentName }}
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
              <span v-if="selectedRooms.length > 0">
                已选择 <strong class="text-blue-600">{{ selectedRooms.length }}</strong> 间房间
              </span>
              <span v-else>点击房间或楼层进行分配</span>
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
            <div class="flex h-10 w-10 items-center justify-center rounded-full bg-blue-100">
              <Building2 class="h-5 w-5 text-blue-600" />
            </div>
            <div>
              <h4 class="text-lg font-semibold text-gray-900">确认分配</h4>
              <p class="text-sm text-gray-500">{{ confirmMessage }}</p>
            </div>
          </div>

          <div class="mb-6 rounded-lg bg-gray-50 p-4">
            <div class="mb-2 text-sm text-gray-600">
              <strong>目标院系：</strong>
              <span :class="selectedDepartmentId ? 'text-blue-600' : 'text-amber-600'">
                {{ selectedDepartmentId ? getDepartmentName(selectedDepartmentId) : '取消分配' }}
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
              @click="handleConfirmAssign"
              :disabled="submitLoading"
              class="inline-flex h-9 items-center gap-2 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
            >
              <Loader2 v-if="submitLoading" class="h-4 w-4 animate-spin" />
              确认分配
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
import { X, Loader2, Building2, Layers } from 'lucide-vue-next'
// V2 DDD API
import { getDormitoriesByBuilding, batchUpdateDepartment, batchUpdateDepartmentByFloor } from '@/api/v2/dormitory'
import { getOrgUnitsByType } from '@/api/v2/organization'

// Props
const props = defineProps<{
  visible: boolean
  buildingId: number
  buildingName: string
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
const departmentOptions = ref<Array<{ id: number; deptName: string }>>([])
const selectedDepartmentId = ref<number | null>(null)
const selectedRooms = ref<number[]>([])

// 确认对话框
const confirmDialogVisible = ref(false)
const confirmMessage = ref('')
const pendingRoomIds = ref<number[]>([])
const pendingFloor = ref<number | null>(null)

// 计算属性
const totalRooms = computed(() => rooms.value.length)
const assignedRooms = computed(() => rooms.value.filter(r => r.departmentId).length)

// 按楼层分组数据
const floorData = computed(() => {
  const floorMap = new Map<number, {
    floor: number
    rooms: any[]
    assignedCount: number
  }>()

  rooms.value.forEach(room => {
    const floorNum = room.floorNumber || room.floor || 1
    if (!floorMap.has(floorNum)) {
      floorMap.set(floorNum, {
        floor: floorNum,
        rooms: [],
        assignedCount: 0
      })
    }
    const floor = floorMap.get(floorNum)!
    floor.rooms.push(room)
    if (room.departmentId) {
      floor.assignedCount++
    }
  })

  // 排序：楼层从高到低
  const result = Array.from(floorMap.values())
  result.sort((a, b) => b.floor - a.floor)

  // 每层房间按房间号排序
  result.forEach(floor => {
    floor.rooms.sort((a, b) => {
      const aNo = a.dormitoryNo || a.roomNo || ''
      const bNo = b.dormitoryNo || b.roomNo || ''
      return aNo.localeCompare(bNo, 'zh-CN', { numeric: true })
    })
  })

  return result
})

// 获取房间样式类
const getRoomClass = (room: any) => {
  // 如果该房间的院系等于当前选中的院系
  if (room.departmentId && room.departmentId === selectedDepartmentId.value) {
    return 'border-emerald-400 bg-emerald-100 hover:border-emerald-500'
  }
  // 如果房间已分配院系
  if (room.departmentId) {
    return 'border-blue-300 bg-blue-100 hover:border-blue-400'
  }
  // 未分配
  return 'border-gray-300 bg-white hover:border-blue-400'
}

const getRoomTextClass = (room: any) => {
  if (room.departmentId && room.departmentId === selectedDepartmentId.value) {
    return 'text-emerald-700'
  }
  if (room.departmentId) {
    return 'text-blue-700'
  }
  return 'text-gray-700'
}

const getRoomSubTextClass = (room: any) => {
  if (room.departmentId && room.departmentId === selectedDepartmentId.value) {
    return 'text-emerald-600'
  }
  if (room.departmentId) {
    return 'text-blue-600'
  }
  return 'text-gray-500'
}

// 获取院系名称
const getDepartmentName = (deptId: number) => {
  const dept = departmentOptions.value.find(d => d.id === deptId)
  return dept?.deptName || '未知院系'
}

// 加载数据 - V2 API
const loadData = async () => {
  loading.value = true
  try {
    const [roomsRes, deptsRes] = await Promise.all([
      getDormitoriesByBuilding(props.buildingId),
      getOrgUnitsByType('DEPARTMENT')
    ])
    rooms.value = roomsRes || []
    // V2: unitName → deptName (兼容显示)
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

// 点击楼层（整层分配）
const handleFloorClick = (floor: number) => {
  const floorRooms = rooms.value.filter(r => (r.floorNumber || r.floor || 1) === floor)
  if (floorRooms.length === 0) {
    ElMessage.warning('该楼层没有房间')
    return
  }

  pendingFloor.value = floor
  pendingRoomIds.value = floorRooms.map(r => r.id)
  confirmMessage.value = `将 ${floor} 楼的 ${floorRooms.length} 间房间分配给选定院系`
  confirmDialogVisible.value = true
}

// 点击单个房间
const handleRoomClick = (room: any) => {
  pendingFloor.value = null
  pendingRoomIds.value = [room.id]
  confirmMessage.value = `将房间 ${room.dormitoryNo || room.roomNo} 分配给选定院系`
  confirmDialogVisible.value = true
}

// 确认分配
const handleConfirmAssign = async () => {
  submitLoading.value = true
  try {
    if (pendingFloor.value !== null) {
      // 按楼层分配
      await batchUpdateDepartmentByFloor({
        buildingId: props.buildingId,
        floor: pendingFloor.value,
        departmentId: selectedDepartmentId.value
      })
    } else {
      // 单个或多个房间分配
      await batchUpdateDepartment({
        dormitoryIds: pendingRoomIds.value,
        departmentId: selectedDepartmentId.value
      })
    }

    ElMessage.success('分配成功')
    confirmDialogVisible.value = false

    // 重新加载数据
    await loadData()
    emit('success')
  } catch (error: any) {
    ElMessage.error(error.message || '分配失败')
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
