<template>
  <div class="overflow-hidden rounded-xl border border-gray-200 bg-white">
    <!-- Building Header -->
    <div
      class="flex cursor-pointer items-center justify-between bg-gradient-to-r from-teal-50 to-cyan-50 px-4 py-3"
      @click="$emit('toggle')"
    >
      <div class="flex items-center gap-3">
        <component
          :is="expanded ? ChevronDown : ChevronRight"
          class="h-5 w-5 text-teal-600"
        />
        <Building2 class="h-5 w-5 text-teal-600" />
        <span class="text-base font-semibold text-gray-900">{{ building.buildingName }}</span>
        <span
          class="rounded-full px-2 py-0.5 text-xs font-medium"
          :class="genderTypeClass"
        >
          {{ genderTypeLabel }}
        </span>
      </div>
      <div class="flex items-center gap-4 text-sm">
        <span class="text-gray-500">
          共 <strong class="text-gray-900">{{ building.totalRooms }}</strong> 间
        </span>
        <span class="text-gray-500">
          入住率 <strong class="text-teal-600">{{ building.occupancyRate }}%</strong>
        </span>
      </div>
    </div>

    <!-- Floors Content -->
    <div v-show="expanded" class="divide-y divide-gray-100">
      <div
        v-for="floor in building.floors"
        :key="floor.floor"
        class="flex items-stretch"
      >
        <!-- Floor Label -->
        <div class="flex w-20 flex-shrink-0 items-center justify-center border-r border-gray-100 bg-gray-50">
          <div class="text-center">
            <div class="text-lg font-bold text-gray-700">{{ floor.floor }}F</div>
            <div class="text-[10px] text-gray-400">{{ floor.rooms.length }}间</div>
          </div>
        </div>
        <!-- Rooms List -->
        <div class="flex flex-1 flex-wrap gap-1.5 p-3">
          <div
            v-for="room in floor.rooms"
            :key="room.id"
            class="group relative cursor-pointer rounded-md border px-2 py-1.5 text-center transition-all hover:shadow-md"
            :class="getRoomStatusClass(room)"
            :title="`${getFullRoomNo(room)} - ${room.currentOccupancy}/${room.maxOccupancy}人`"
            @click="$emit('room-click', room)"
          >
            <div class="text-xs font-medium" :class="getRoomTextClass(room)">
              {{ room.roomNo || room.dormitoryNo }}
            </div>
            <div class="text-[10px]" :class="getRoomSubTextClass(room)">
              {{ room.currentOccupancy }}/{{ room.maxOccupancy }}
            </div>
            <!-- Hover Tooltip -->
            <div class="pointer-events-none absolute bottom-full left-1/2 z-10 mb-2 hidden -translate-x-1/2 whitespace-nowrap rounded-lg bg-gray-900 px-3 py-2 text-xs text-white shadow-lg group-hover:block">
              <div class="font-medium">{{ getFullRoomNo(room) }}</div>
              <div class="mt-1 text-gray-300">
                {{ room.maxOccupancy }}人间 · {{ room.currentOccupancy }}人入住
              </div>
              <div v-if="room.students && room.students.length > 0" class="mt-1 border-t border-gray-700 pt-1">
                <div v-for="s in room.students.slice(0, 4)" :key="s.id" class="text-gray-300">
                  {{ s.bedNumber || '-' }}号床: {{ s.realName }}
                </div>
                <div v-if="room.students.length > 4" class="text-gray-400">...</div>
              </div>
              <div class="absolute left-1/2 top-full -translate-x-1/2 border-4 border-transparent border-t-gray-900"></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Building2, ChevronDown, ChevronRight } from 'lucide-vue-next'

interface Room {
  id: string | number
  roomNo?: string
  dormitoryNo?: string
  floor?: number
  floorNumber?: number
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

interface Floor {
  floor: number
  rooms: Room[]
}

interface Building {
  buildingName: string
  genderType: number
  totalRooms: number
  occupiedBeds: number
  totalBeds: number
  occupancyRate: number
  floors: Floor[]
}

interface Props {
  building: Building
  expanded: boolean
}

defineProps<Props>()
defineEmits<{
  toggle: []
  'room-click': [room: Room]
}>()

const genderTypeClass = computed(() => {
  return {
    1: 'bg-blue-100 text-blue-700',
    2: 'bg-pink-100 text-pink-700',
    3: 'bg-gray-100 text-gray-700'
  }[1] || 'bg-gray-100 text-gray-700'
})

const genderTypeLabel = computed(() => {
  const props = defineProps<Props>()
  return {
    1: '男生楼',
    2: '女生楼',
    3: '混合楼'
  }[props.building?.genderType] || '混合楼'
})

// Room status styling
const getRoomStatusClass = (room: Room) => {
  if (room.status === 0) {
    return 'border-gray-300 bg-gray-200'
  }
  const rate = room.currentOccupancy / room.maxOccupancy
  if (rate === 0) return 'border-gray-300 bg-white hover:border-blue-400'
  if (rate < 1) return 'border-blue-300 bg-blue-100 hover:border-blue-500'
  return 'border-emerald-300 bg-emerald-100 hover:border-emerald-500'
}

const getRoomTextClass = (room: Room) => {
  if (room.status === 0) return 'text-gray-500'
  const rate = room.currentOccupancy / room.maxOccupancy
  if (rate === 0) return 'text-gray-700'
  if (rate < 1) return 'text-blue-700'
  return 'text-emerald-700'
}

const getRoomSubTextClass = (room: Room) => {
  if (room.status === 0) return 'text-gray-400'
  const rate = room.currentOccupancy / room.maxOccupancy
  if (rate === 0) return 'text-gray-500'
  if (rate < 1) return 'text-blue-600'
  return 'text-emerald-600'
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
