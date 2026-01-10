<template>
  <div class="flex">
    <!-- 左侧：表单区域 -->
    <div class="flex-1 p-6">
      <form @submit.prevent="handleSubmit" class="space-y-5">
        <!-- 步骤指示器 (新增模式) -->
        <div v-if="mode === 'add'" class="mb-6">
          <div class="flex items-center justify-between">
            <div
              v-for="(step, idx) in steps"
              :key="idx"
              class="flex flex-1 items-center"
            >
              <div class="flex items-center gap-2">
                <div
                  class="flex h-8 w-8 items-center justify-center rounded-full text-sm font-medium transition-all"
                  :class="currentStep > idx ? 'bg-teal-600 text-white' : currentStep === idx ? 'bg-teal-100 text-teal-700 ring-2 ring-teal-600' : 'bg-gray-100 text-gray-400'"
                >
                  <Check v-if="currentStep > idx" class="h-4 w-4" />
                  <span v-else>{{ idx + 1 }}</span>
                </div>
                <span
                  class="text-sm font-medium"
                  :class="currentStep >= idx ? 'text-gray-900' : 'text-gray-400'"
                >
                  {{ step }}
                </span>
              </div>
              <div
                v-if="idx < steps.length - 1"
                class="mx-3 h-0.5 flex-1"
                :class="currentStep > idx ? 'bg-teal-600' : 'bg-gray-200'"
              ></div>
            </div>
          </div>
        </div>

        <!-- 第一步：选择宿舍楼 -->
        <div>
          <label class="mb-2 block text-sm font-medium text-gray-700">
            <Building2 class="mr-1.5 inline-block h-4 w-4 text-gray-400" />
            宿舍楼 <span class="text-red-500">*</span>
          </label>
          <div class="grid grid-cols-2 gap-2">
            <button
              v-for="building in buildingList"
              :key="building.id"
              type="button"
              @click="handleBuildingSelect(building)"
              class="flex items-center gap-3 rounded-lg border-2 p-3 text-left transition-all"
              :class="formData.buildingId === building.id
                ? 'border-teal-500 bg-teal-50 ring-1 ring-teal-500'
                : 'border-gray-200 hover:border-teal-300 hover:bg-gray-50'"
            >
              <div
                class="flex h-10 w-10 items-center justify-center rounded-lg"
                :class="formData.buildingId === building.id ? 'bg-teal-100 text-teal-600' : 'bg-gray-100 text-gray-500'"
              >
                <Home class="h-5 w-5" />
              </div>
              <div class="flex-1 min-w-0">
                <div class="text-sm font-medium text-gray-900 truncate">
                  {{ building.buildingName || building.buildingNo + '号楼' }}
                </div>
                <div class="text-xs text-gray-500">
                  {{ building.genderType === 1 ? '男生楼' : building.genderType === 2 ? '女生楼' : '混合楼' }}
                </div>
              </div>
              <CheckCircle2
                v-if="formData.buildingId === building.id"
                class="h-5 w-5 flex-shrink-0 text-teal-600"
              />
            </button>
          </div>
          <p v-if="buildingList.length === 0" class="mt-2 text-sm text-gray-500">
            暂无可用宿舍楼，请先创建宿舍楼
          </p>
        </div>

        <!-- 第二步：选择楼层 -->
        <div v-if="formData.buildingId">
          <label class="mb-2 block text-sm font-medium text-gray-700">
            <Layers class="mr-1.5 inline-block h-4 w-4 text-gray-400" />
            楼层 <span class="text-red-500">*</span>
          </label>
          <div class="flex flex-wrap gap-2">
            <button
              v-for="floor in availableFloors"
              :key="floor"
              type="button"
              @click="handleFloorSelect(floor)"
              class="relative flex h-12 w-12 items-center justify-center rounded-lg border-2 text-sm font-medium transition-all"
              :class="formData.floor === floor
                ? 'border-teal-500 bg-teal-50 text-teal-700'
                : 'border-gray-200 text-gray-600 hover:border-teal-300 hover:bg-gray-50'"
            >
              {{ floor }}F
              <span
                v-if="floorRoomCounts[floor]"
                class="absolute -right-1 -top-1 flex h-4 min-w-[16px] items-center justify-center rounded-full bg-gray-500 px-1 text-[10px] text-white"
              >
                {{ floorRoomCounts[floor] }}
              </span>
            </button>
          </div>
          <p class="mt-2 text-xs text-gray-500">
            数字气泡表示该楼层已有的房间数
          </p>
        </div>

        <!-- 第三步：房间信息 -->
        <div v-if="formData.floor" class="space-y-4">
          <!-- 房间号 -->
          <div>
            <label class="mb-2 block text-sm font-medium text-gray-700">
              <DoorOpen class="mr-1.5 inline-block h-4 w-4 text-gray-400" />
              房间号 <span class="text-red-500">*</span>
            </label>
            <div class="flex items-center gap-3">
              <input
                v-model="formData.dormitoryNo"
                type="text"
                required
                placeholder="请输入房间号"
                maxlength="10"
                class="h-10 flex-1 rounded-lg border border-gray-300 px-4 text-sm focus:border-teal-500 focus:outline-none focus:ring-1 focus:ring-teal-500"
              />
              <button
                v-if="mode === 'add'"
                type="button"
                @click="autoGenerateRoomNo"
                class="h-10 whitespace-nowrap rounded-lg border border-teal-300 bg-teal-50 px-3 text-sm font-medium text-teal-700 hover:bg-teal-100"
              >
                <Sparkles class="mr-1 inline-block h-4 w-4" />
                自动生成
              </button>
            </div>
            <p v-if="suggestedRoomNo && mode === 'add'" class="mt-1.5 text-xs text-gray-500">
              建议房间号: <span class="font-medium text-teal-600">{{ suggestedRoomNo }}</span>
              （该楼层最大房间号 + 1）
            </p>
          </div>

          <!-- 床位容量和房间用途 -->
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="mb-2 block text-sm font-medium text-gray-700">
                <BedDouble class="mr-1.5 inline-block h-4 w-4 text-gray-400" />
                床位容量 <span class="text-red-500">*</span>
              </label>
              <div class="flex gap-2">
                <button
                  v-for="capacity in [4, 6, 8, 0]"
                  :key="capacity"
                  type="button"
                  @click="formData.bedCapacity = capacity"
                  class="flex-1 rounded-lg border-2 py-2 text-sm font-medium transition-all"
                  :class="formData.bedCapacity === capacity
                    ? 'border-teal-500 bg-teal-50 text-teal-700'
                    : 'border-gray-200 text-gray-600 hover:border-teal-300'"
                >
                  {{ capacity === 0 ? '无' : capacity + '人' }}
                </button>
              </div>
            </div>

            <div>
              <label class="mb-2 block text-sm font-medium text-gray-700">
                <Tag class="mr-1.5 inline-block h-4 w-4 text-gray-400" />
                房间用途 <span class="text-red-500">*</span>
              </label>
              <select
                v-model="formData.roomUsageType"
                required
                class="h-10 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-teal-500 focus:outline-none focus:ring-1 focus:ring-teal-500"
              >
                <option :value="1">学生宿舍</option>
                <option :value="2">教职工宿舍</option>
                <option :value="3">配电室</option>
                <option :value="4">卫生间</option>
                <option :value="5">杂物间</option>
                <option :value="6">其他</option>
              </select>
            </div>
          </div>

          <!-- 状态 -->
          <div>
            <label class="mb-2 block text-sm font-medium text-gray-700">
              <Settings class="mr-1.5 inline-block h-4 w-4 text-gray-400" />
              状态
            </label>
            <div class="flex gap-3">
              <label
                v-for="status in statusOptions"
                :key="status.value"
                class="flex flex-1 cursor-pointer items-center justify-center gap-2 rounded-lg border-2 py-2.5 transition-all"
                :class="formData.status === status.value
                  ? `${status.activeClass} border-current`
                  : 'border-gray-200 hover:border-gray-300'"
              >
                <input
                  type="radio"
                  v-model="formData.status"
                  :value="status.value"
                  class="sr-only"
                />
                <component :is="status.icon" class="h-4 w-4" />
                <span class="text-sm font-medium">{{ status.label }}</span>
              </label>
            </div>
          </div>
        </div>

        <!-- 床位分配情况 (编辑模式) -->
        <div v-if="mode === 'edit' && bedAllocations.length > 0" class="rounded-lg border border-gray-200 overflow-hidden">
          <div class="bg-gray-50 px-4 py-2 border-b border-gray-200">
            <span class="text-sm font-medium text-gray-700">床位分配情况</span>
          </div>
          <div class="divide-y divide-gray-100">
            <div
              v-for="bed in bedAllocations"
              :key="bed.bedNumber"
              class="flex items-center gap-3 px-4 py-2.5"
            >
              <div
                class="flex h-8 w-8 items-center justify-center rounded-full text-sm font-medium"
                :class="bed.isAssigned ? 'bg-teal-100 text-teal-700' : 'bg-gray-100 text-gray-400'"
              >
                {{ bed.bedNumber }}
              </div>
              <div class="flex-1">
                <span v-if="bed.isAssigned" class="text-sm font-medium text-gray-900">
                  {{ bed.studentName }}
                </span>
                <span v-else class="text-sm text-gray-400">空床位</span>
              </div>
              <span v-if="bed.studentNo" class="text-xs text-gray-500">{{ bed.studentNo }}</span>
            </div>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="flex items-center justify-end gap-3 border-t border-gray-200 pt-5">
          <button
            type="button"
            @click="$emit('close')"
            class="h-10 rounded-lg border border-gray-300 px-5 text-sm font-medium text-gray-700 hover:bg-gray-50"
          >
            取消
          </button>
          <button
            type="submit"
            :disabled="loading || !canSubmit"
            class="inline-flex h-10 items-center gap-2 rounded-lg bg-teal-600 px-5 text-sm font-medium text-white hover:bg-teal-700 disabled:cursor-not-allowed disabled:opacity-50"
          >
            <Loader2 v-if="loading" class="h-4 w-4 animate-spin" />
            <Check v-else class="h-4 w-4" />
            {{ mode === 'add' ? '确定新增' : '确定修改' }}
          </button>
        </div>
      </form>
    </div>

    <!-- 右侧：楼栋信息预览 (新增模式) -->
    <div
      v-if="mode === 'add' && selectedBuildingInfo"
      class="w-72 flex-shrink-0 border-l border-gray-200 bg-gray-50/50 p-4"
    >
      <div class="sticky top-4">
        <!-- 楼栋信息头部 -->
        <div class="mb-4 rounded-lg bg-white p-4 shadow-sm">
          <div class="flex items-center gap-3">
            <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-teal-100">
              <Building2 class="h-6 w-6 text-teal-600" />
            </div>
            <div>
              <h3 class="text-base font-semibold text-gray-900">
                {{ selectedBuildingInfo.buildingName }}
              </h3>
              <p class="text-xs text-gray-500">
                {{ selectedBuildingInfo.genderType === 1 ? '男生宿舍楼' : selectedBuildingInfo.genderType === 2 ? '女生宿舍楼' : '混合宿舍楼' }}
              </p>
            </div>
          </div>

          <!-- 统计信息 -->
          <div class="mt-4 grid grid-cols-2 gap-3">
            <div class="rounded-lg bg-gray-50 p-2.5 text-center">
              <div class="text-lg font-bold text-gray-900">{{ buildingStats.totalRooms }}</div>
              <div class="text-xs text-gray-500">总房间数</div>
            </div>
            <div class="rounded-lg bg-gray-50 p-2.5 text-center">
              <div class="text-lg font-bold text-teal-600">{{ buildingStats.occupancyRate }}%</div>
              <div class="text-xs text-gray-500">入住率</div>
            </div>
          </div>
        </div>

        <!-- 楼层分布 -->
        <div class="rounded-lg bg-white p-4 shadow-sm">
          <h4 class="mb-3 flex items-center gap-1.5 text-sm font-medium text-gray-700">
            <Layers class="h-4 w-4 text-gray-400" />
            楼层房间分布
          </h4>

          <div class="space-y-2 max-h-64 overflow-y-auto">
            <div
              v-for="floor in buildingFloorStats"
              :key="floor.floor"
              class="rounded-lg border p-2.5 transition-all"
              :class="formData.floor === floor.floor
                ? 'border-teal-300 bg-teal-50'
                : 'border-gray-200'"
            >
              <div class="flex items-center justify-between mb-1.5">
                <span class="text-sm font-medium text-gray-900">{{ floor.floor }}层</span>
                <span class="text-xs text-gray-500">{{ floor.roomCount }}间</span>
              </div>
              <!-- 房间号列表 -->
              <div class="flex flex-wrap gap-1">
                <span
                  v-for="room in floor.rooms.slice(0, 8)"
                  :key="room.id"
                  class="rounded px-1.5 py-0.5 text-[10px] font-medium"
                  :class="room.currentOccupancy >= room.maxOccupancy
                    ? 'bg-emerald-100 text-emerald-700'
                    : room.currentOccupancy > 0
                      ? 'bg-blue-100 text-blue-700'
                      : 'bg-gray-100 text-gray-600'"
                >
                  {{ room.dormitoryNo || room.roomNo }}
                </span>
                <span
                  v-if="floor.rooms.length > 8"
                  class="rounded bg-gray-100 px-1.5 py-0.5 text-[10px] text-gray-500"
                >
                  +{{ floor.rooms.length - 8 }}
                </span>
              </div>
            </div>
          </div>

          <!-- 图例 -->
          <div class="mt-3 flex flex-wrap gap-3 border-t border-gray-100 pt-3 text-[10px]">
            <div class="flex items-center gap-1">
              <div class="h-2 w-2 rounded bg-emerald-500"></div>
              <span class="text-gray-500">满员</span>
            </div>
            <div class="flex items-center gap-1">
              <div class="h-2 w-2 rounded bg-blue-500"></div>
              <span class="text-gray-500">部分入住</span>
            </div>
            <div class="flex items-center gap-1">
              <div class="h-2 w-2 rounded bg-gray-300"></div>
              <span class="text-gray-500">空置</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Loader2,
  Check,
  Building2,
  Home,
  Layers,
  DoorOpen,
  BedDouble,
  Tag,
  Settings,
  CheckCircle2,
  Sparkles,
  CircleCheck,
  Wrench,
  XCircle
} from 'lucide-vue-next'
// V2 DDD API
import { getDormitory, createDormitory, updateDormitory, getBedAllocations, getDormitories, getAllEnabledBuildings } from '@/api/v2/dormitory'

interface Props {
  mode: 'add' | 'edit'
  dormitoryId?: number | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  success: []
  close: []
}>()

const loading = ref(false)
const buildingList = ref<any[]>([])
const bedAllocations = ref<any[]>([])
const buildingRooms = ref<any[]>([]) // 当前选中楼栋的所有房间

const steps = ['选择宿舍楼', '选择楼层', '填写房间信息']

const formData = reactive({
  buildingId: null as number | null,
  dormitoryNo: '',
  floor: null as number | null,
  bedCapacity: 4,
  roomUsageType: 1,
  status: 1
})

const statusOptions = [
  { value: 1, label: '正常', icon: CircleCheck, activeClass: 'bg-green-50 text-green-700' },
  { value: 2, label: '维修', icon: Wrench, activeClass: 'bg-amber-50 text-amber-700' },
  { value: 3, label: '停用', icon: XCircle, activeClass: 'bg-red-50 text-red-700' }
]

// 当前步骤
const currentStep = computed(() => {
  if (!formData.buildingId) return 0
  if (!formData.floor) return 1
  return 2
})

// 可提交
const canSubmit = computed(() => {
  return formData.buildingId && formData.floor && formData.dormitoryNo
})

// 可选楼层 (1-10层)
const availableFloors = computed(() => {
  return Array.from({ length: 10 }, (_, i) => i + 1)
})

// 每层房间数量
const floorRoomCounts = computed(() => {
  const counts: Record<number, number> = {}
  buildingRooms.value.forEach(room => {
    const floor = room.floorNumber || room.floor || 1
    counts[floor] = (counts[floor] || 0) + 1
  })
  return counts
})

// 选中的宿舍楼信息
const selectedBuildingInfo = computed(() => {
  if (!formData.buildingId) return null
  return buildingList.value.find(b => b.id === formData.buildingId)
})

// 楼栋统计
const buildingStats = computed(() => {
  const rooms = buildingRooms.value
  const totalRooms = rooms.length
  const totalBeds = rooms.reduce((sum, r) => sum + (r.bedCapacity || r.maxOccupancy || 0), 0)
  const occupiedBeds = rooms.reduce((sum, r) => sum + (r.occupiedBeds || r.currentOccupancy || 0), 0)
  const occupancyRate = totalBeds > 0 ? Math.round((occupiedBeds / totalBeds) * 100) : 0

  return { totalRooms, totalBeds, occupiedBeds, occupancyRate }
})

// 楼层统计
const buildingFloorStats = computed(() => {
  const floorMap = new Map<number, { floor: number; roomCount: number; rooms: any[] }>()

  buildingRooms.value.forEach(room => {
    const floor = room.floorNumber || room.floor || 1
    if (!floorMap.has(floor)) {
      floorMap.set(floor, { floor, roomCount: 0, rooms: [] })
    }
    const floorData = floorMap.get(floor)!
    floorData.roomCount++
    floorData.rooms.push(room)
  })

  // 排序
  const result = Array.from(floorMap.values())
  result.sort((a, b) => a.floor - b.floor)
  result.forEach(f => {
    f.rooms.sort((a, b) => {
      const aNo = a.dormitoryNo || a.roomNo || ''
      const bNo = b.dormitoryNo || b.roomNo || ''
      return aNo.localeCompare(bNo, 'zh-CN', { numeric: true })
    })
  })

  return result
})

// 建议的房间号
const suggestedRoomNo = computed(() => {
  if (!formData.buildingId || !formData.floor) return ''

  const floorRooms = buildingRooms.value.filter(
    r => (r.floorNumber || r.floor) === formData.floor
  )

  if (floorRooms.length === 0) {
    // 该楼层没有房间，生成第一个房间号
    return `${formData.floor}01`
  }

  // 找到最大的房间号
  let maxNo = 0
  floorRooms.forEach(room => {
    const roomNo = room.dormitoryNo || room.roomNo || ''
    // 尝试提取数字部分
    const match = roomNo.match(/(\d+)$/)
    if (match) {
      const num = parseInt(match[1], 10)
      if (num > maxNo) maxNo = num
    }
  })

  if (maxNo > 0) {
    return String(maxNo + 1)
  }

  // 默认生成
  return `${formData.floor}${String(floorRooms.length + 1).padStart(2, '0')}`
})

// 加载宿舍楼列表 - V2 API
const loadBuildingList = async () => {
  try {
    // V2: 使用 getAllEnabledBuildings，筛选宿舍楼类型 (2)
    const res = await getAllEnabledBuildings(2)
    buildingList.value = res || []
  } catch (error: any) {
    console.error('加载宿舍楼列表失败:', error)
    ElMessage.error('加载宿舍楼列表失败')
  }
}

// 加载楼栋的房间列表 - V2 API
const loadBuildingRooms = async (buildingId: number) => {
  try {
    const building = buildingList.value.find(b => b.id === buildingId)
    const buildingName = building?.buildingName || building?.buildingNo

    // V2: 使用 getDormitories 替代 getDormitoryList
    const res = await getDormitories({
      buildingId: buildingId,
      pageNum: 1,
      pageSize: 1000
    })
    buildingRooms.value = res.records || []
  } catch (error: any) {
    console.error('加载房间列表失败:', error)
    buildingRooms.value = []
  }
}

// 加载宿舍详情 - V2 API
const loadDormitoryDetail = async () => {
  if (!props.dormitoryId) return

  loading.value = true
  try {
    const detail = await getDormitory(props.dormitoryId)

    let buildingId = detail.buildingId
    if (!buildingId && (detail.buildingName || detail.dormitoryNo)) {
      const buildingNoFromDormitory = detail.dormitoryNo?.split('#')[0]
      const building = buildingList.value.find(b =>
        b.buildingName === detail.buildingName ||
        b.buildingNo === detail.buildingName ||
        b.buildingNo === buildingNoFromDormitory
      )
      if (building) {
        buildingId = building.id
      }
    }

    Object.assign(formData, {
      buildingId: buildingId || null,
      dormitoryNo: detail.dormitoryNo || '',
      floor: detail.floor || detail.floorNumber || 1,
      bedCapacity: detail.bedCapacity || 4,
      roomUsageType: detail.roomUsageType || 1,
      status: detail.status || 1
    })

    if (buildingId) {
      await loadBuildingRooms(buildingId)
    }

    await loadBedAllocations()
  } catch (error: any) {
    const message = error.response?.data?.message || '加载宿舍详情失败'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

// 加载床位分配信息
const loadBedAllocations = async () => {
  if (!props.dormitoryId) return

  try {
    const allocations = await getBedAllocations(props.dormitoryId)
    bedAllocations.value = allocations || []
  } catch (error: any) {
    bedAllocations.value = []
  }
}

// 选择宿舍楼
const handleBuildingSelect = async (building: any) => {
  formData.buildingId = building.id
  formData.floor = null
  formData.dormitoryNo = ''
  await loadBuildingRooms(building.id)
}

// 选择楼层
const handleFloorSelect = (floor: number) => {
  formData.floor = floor
  // 自动填充建议的房间号
  if (props.mode === 'add') {
    setTimeout(() => {
      if (suggestedRoomNo.value) {
        formData.dormitoryNo = suggestedRoomNo.value
      }
    }, 0)
  }
}

// 自动生成房间号
const autoGenerateRoomNo = () => {
  if (suggestedRoomNo.value) {
    formData.dormitoryNo = suggestedRoomNo.value
  }
}

const handleSubmit = async () => {
  if (!formData.buildingId) {
    ElMessage.warning('请选择宿舍楼')
    return
  }
  if (!formData.floor) {
    ElMessage.warning('请选择楼层')
    return
  }
  if (!formData.dormitoryNo) {
    ElMessage.warning('请输入房间号')
    return
  }

  loading.value = true
  try {
    const submitData: any = {
      buildingId: formData.buildingId!,
      dormitoryNo: formData.dormitoryNo,
      floorNumber: formData.floor,
      bedCapacity: formData.bedCapacity,
      roomUsageType: formData.roomUsageType,
      status: formData.status
    }

    if (props.mode === 'add') {
      await createDormitory(submitData)
      ElMessage.success('宿舍创建成功')
    } else {
      submitData.id = props.dormitoryId
      await updateDormitory(props.dormitoryId!, submitData)
      ElMessage.success('宿舍更新成功')
    }

    emit('success')
  } catch (error: any) {
    const message = error.response?.data?.message || '操作失败'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadBuildingList()

  if (props.mode === 'edit' && props.dormitoryId) {
    await loadDormitoryDetail()
  }
})
</script>
