<template>
  <div class="p-4">
    <!-- Legend -->
    <div v-if="showLegend" class="mb-4 flex flex-wrap items-center gap-4 rounded-lg bg-gray-50 px-4 py-2.5 text-xs">
      <span class="font-medium text-gray-600">图例：</span>
      <div class="flex items-center gap-1.5">
        <div class="h-3 w-3 rounded border-2 border-emerald-400 bg-emerald-50"></div>
        <span class="text-gray-600">满员</span>
      </div>
      <div class="flex items-center gap-1.5">
        <div class="h-3 w-3 rounded border-2 border-blue-400 bg-blue-50"></div>
        <span class="text-gray-600">部分入住</span>
      </div>
      <div class="flex items-center gap-1.5">
        <div class="h-3 w-3 rounded border-2 border-gray-300 bg-white"></div>
        <span class="text-gray-600">空置</span>
      </div>
      <div class="flex items-center gap-1.5">
        <div class="h-3 w-3 rounded border-2 border-gray-300 bg-gray-200 opacity-60"></div>
        <span class="text-gray-600">停用</span>
      </div>
      <div class="flex items-center gap-1.5">
        <div class="h-3 w-3 rounded border-2 border-amber-300 bg-amber-50"></div>
        <span class="text-gray-600">维修中</span>
      </div>
    </div>

    <!-- Room Grid -->
    <div
      v-if="rooms.length > 0"
      class="grid gap-2"
      :class="gridColsClass"
    >
      <div
        v-for="room in rooms"
        :key="room.id"
        class="group relative cursor-pointer rounded-lg border-2 p-2 transition-all hover:shadow-md hover:-translate-y-0.5"
        :class="getRoomCardClass(room)"
        @click="$emit('room-click', room)"
      >
        <!-- 停用/维修标签 -->
        <div
          v-if="room.status === 0 || room.status === 2"
          class="absolute -left-1 -top-1 z-10 flex h-4 items-center rounded-full px-1.5 text-[8px] font-medium text-white shadow-sm"
          :class="room.status === 0 ? 'bg-gray-500' : 'bg-amber-500'"
        >
          {{ room.status === 0 ? '停用' : '维修' }}
        </div>
        <!-- Room Number -->
        <div class="mb-1.5 flex items-center justify-between">
          <span class="text-sm font-semibold text-gray-900">{{ getFullRoomNo(room) }}</span>
          <span
            class="rounded px-1 py-0.5 text-[10px] font-medium"
            :class="getGenderBadgeClass(room)"
          >
            {{ getGenderLabel(room) }}
          </span>
        </div>

        <!-- Occupancy Bar -->
        <div class="mb-1.5 flex items-center gap-1">
          <div class="h-1.5 flex-1 overflow-hidden rounded-full bg-gray-200">
            <div
              class="h-full rounded-full transition-all"
              :class="getOccupancyBarClass(room)"
              :style="{ width: `${getOccupancyRate(room)}%` }"
            ></div>
          </div>
          <span class="text-[10px] font-medium text-gray-500">
            {{ room.currentOccupancy }}/{{ room.maxOccupancy }}
          </span>
        </div>

        <!-- Student List (Compact Mode) -->
        <div v-if="mode === 'compact' && room.students && room.students.length > 0" class="space-y-0.5">
          <div
            v-for="(student, idx) in room.students.slice(0, room.maxOccupancy)"
            :key="student.id || idx"
            class="flex items-center gap-1"
          >
            <div
              class="flex h-4 w-4 flex-shrink-0 items-center justify-center rounded-full text-[8px] font-medium"
              :class="getStudentBadgeClass(room)"
            >
              {{ extractBedNumber(student.bedNumber) || idx + 1 }}
            </div>
            <span class="truncate text-[11px] text-gray-700">{{ student.realName }}</span>
          </div>
        </div>

        <!-- Empty State -->
        <div
          v-else-if="mode === 'compact' && (!room.students || room.students.length === 0)"
          class="flex items-center justify-center gap-1 py-1 text-[10px] text-gray-400"
        >
          <UserCircle2 class="h-3.5 w-3.5" />
          <span>空置</span>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else class="py-20 text-center">
      <Home class="mx-auto h-12 w-12 text-gray-300" />
      <p class="mt-2 text-sm text-gray-500">暂无宿舍数据</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Home, UserCircle2 } from 'lucide-vue-next'

interface Room {
  id: string | number
  roomNo?: string
  dormitoryNo?: string
  buildingName?: string
  buildingNo?: string
  genderType?: number
  currentOccupancy: number
  maxOccupancy: number
  status?: number
  students?: Array<{
    id: string | number
    realName: string
    bedNumber?: string | number
  }>
}

interface Props {
  rooms: Room[]
  mode?: 'compact' | 'simple'
  showLegend?: boolean
  cols?: 2 | 3 | 4 | 5 | 6 | 8
}

const props = withDefaults(defineProps<Props>(), {
  mode: 'compact',
  showLegend: false,
  cols: 6
})

defineEmits<{
  'room-click': [room: Room]
}>()

const gridColsClass = computed(() => {
  const colsMap = {
    2: 'grid-cols-2',
    3: 'sm:grid-cols-2 md:grid-cols-3',
    4: 'grid-cols-2 sm:grid-cols-3 md:grid-cols-4',
    5: 'grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5',
    6: 'grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6',
    8: 'grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 2xl:grid-cols-8'
  }
  return colsMap[props.cols]
})

const getOccupancyRate = (room: Room) => {
  return (room.currentOccupancy / room.maxOccupancy) * 100
}

const getRoomCardClass = (room: Room) => {
  // 停用状态
  if (room.status === 0) {
    return 'border-gray-300 bg-gray-200 opacity-60 cursor-not-allowed'
  }
  // 维修状态
  if (room.status === 2) {
    return 'border-amber-300 bg-amber-50 opacity-80'
  }
  // 正常状态根据入住情况显示
  if (room.currentOccupancy >= room.maxOccupancy) {
    return 'border-emerald-300 bg-emerald-50'
  }
  if (room.currentOccupancy > 0) {
    return 'border-blue-300 bg-blue-50'
  }
  return 'border-gray-200 bg-white'
}

const getOccupancyBarClass = (room: Room) => {
  if (room.currentOccupancy >= room.maxOccupancy) return 'bg-emerald-500'
  if (room.currentOccupancy > 0) return 'bg-blue-500'
  return 'bg-gray-300'
}

const getGenderBadgeClass = (room: Room) => {
  if (room.genderType === 1) return 'bg-blue-100 text-blue-600'
  if (room.genderType === 2) return 'bg-pink-100 text-pink-600'
  return 'bg-gray-100 text-gray-600'
}

const getGenderLabel = (room: Room) => {
  if (room.genderType === 1) return '男'
  if (room.genderType === 2) return '女'
  return '混'
}

const getStudentBadgeClass = (room: Room) => {
  if (room.genderType === 1) return 'bg-blue-200 text-blue-700'
  if (room.genderType === 2) return 'bg-pink-200 text-pink-700'
  return 'bg-gray-200 text-gray-700'
}

const extractBedNumber = (bedNumber: any): string | number => {
  if (!bedNumber) return ''
  if (typeof bedNumber === 'number') return bedNumber
  const match = String(bedNumber).match(/\d+/)
  return match ? match[0] : ''
}

const getFullRoomNo = (room: Room) => {
  if (room.buildingNo) {
    return `${room.buildingNo}-${room.roomNo || room.dormitoryNo}`
  }
  if (room.buildingName) {
    const match = room.buildingName.match(/^(\d+)/)
    if (match) {
      return `${match[1]}-${room.roomNo || room.dormitoryNo}`
    }
    const letterMatch = room.buildingName.match(/^([A-Za-z])/)
    if (letterMatch) {
      return `${letterMatch[1]}-${room.roomNo || room.dormitoryNo}`
    }
  }
  return room.roomNo || room.dormitoryNo
}
</script>
