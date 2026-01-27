<template>
  <div
    class="relative rounded-lg border-2 p-3 cursor-pointer transition-all select-none"
    :class="cardClass"
    @click="emit('click')"
  >
    <!-- 选中标记 -->
    <div
      v-if="selected"
      class="absolute -top-1 -right-1 w-5 h-5 bg-blue-500 rounded-full flex items-center justify-center shadow"
    >
      <el-icon class="text-white text-xs"><Check /></el-icon>
    </div>

    <!-- 房间号 -->
    <div class="text-center">
      <div class="font-bold text-gray-900">
        <template v-if="room.roomNo">{{ room.roomNo }}室</template>
        <template v-else>{{ room.spaceName }}</template>
      </div>
      <div v-if="room.roomNo && room.spaceName !== room.roomNo" class="text-xs text-gray-400">
        {{ room.spaceName }}
      </div>

      <!-- 房间类型和容量 -->
      <div class="text-xs text-gray-500 mt-1">
        <span>{{ getRoomTypeName(room.roomType) }}</span>
        <span v-if="room.capacity" class="ml-1">· {{ room.currentOccupancy || 0 }}/{{ room.capacity }}</span>
      </div>

      <!-- 分配信息 -->
      <div class="mt-2 text-xs">
        <!-- 部门信息 -->
        <div v-if="room.orgUnitName" class="truncate" :class="orgUnitTextClass">
          {{ room.orgUnitName }}
        </div>
        <div v-else class="text-gray-400">未分配部门</div>

        <!-- 班级信息（仅宿舍/教室显示） -->
        <div
          v-if="showClassInfo && room.className"
          class="truncate text-teal-600 mt-0.5"
        >
          {{ room.className }}
        </div>
      </div>

      <!-- 性别标识（宿舍） -->
      <div
        v-if="room.roomType === 'DORMITORY' && room.genderType && room.genderType !== 0"
        class="mt-1"
      >
        <span
          class="inline-block px-1.5 py-0.5 text-xs rounded"
          :class="room.genderType === 1 ? 'bg-blue-100 text-blue-600' : 'bg-pink-100 text-pink-600'"
        >
          {{ room.genderType === 1 ? '男' : '女' }}
        </span>
      </div>

      <!-- 入住率进度条（有容量的房间） -->
      <div v-if="room.capacity && room.capacity > 0" class="mt-2">
        <div class="h-1 bg-gray-200 rounded-full overflow-hidden">
          <div
            class="h-full transition-all"
            :class="occupancyBarClass"
            :style="{ width: occupancyRate + '%' }"
          ></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Check } from '@element-plus/icons-vue'
import type { SpaceDTO, RoomType } from '@/types/v2/space'
import { getRoomTypeName, ROOM_TYPE_ALLOCATION_RULES } from '@/types/v2/space'

const props = defineProps<{
  room: SpaceDTO
  selected?: boolean
  orgUnitColor?: string
}>()

const emit = defineEmits<{
  (e: 'click'): void
}>()

// 颜色映射
const colorBorderClasses: Record<string, string> = {
  blue: 'border-blue-300 bg-blue-50',
  green: 'border-green-300 bg-green-50',
  purple: 'border-purple-300 bg-purple-50',
  orange: 'border-orange-300 bg-orange-50',
  cyan: 'border-cyan-300 bg-cyan-50',
  pink: 'border-pink-300 bg-pink-50',
  teal: 'border-teal-300 bg-teal-50',
  indigo: 'border-indigo-300 bg-indigo-50',
  amber: 'border-amber-300 bg-amber-50',
  rose: 'border-rose-300 bg-rose-50'
}

const colorTextClasses: Record<string, string> = {
  blue: 'text-blue-600',
  green: 'text-green-600',
  purple: 'text-purple-600',
  orange: 'text-orange-600',
  cyan: 'text-cyan-600',
  pink: 'text-pink-600',
  teal: 'text-teal-600',
  indigo: 'text-indigo-600',
  amber: 'text-amber-600',
  rose: 'text-rose-600'
}

// 是否显示班级信息
const showClassInfo = computed(() => {
  const rule = ROOM_TYPE_ALLOCATION_RULES[props.room.roomType as RoomType]
  return rule?.needsClass ?? false
})

// 卡片样式
const cardClass = computed(() => {
  if (props.selected) {
    return 'border-blue-500 bg-blue-100 shadow-md'
  }

  if (props.orgUnitColor && colorBorderClasses[props.orgUnitColor]) {
    return colorBorderClasses[props.orgUnitColor] + ' hover:shadow-sm'
  }

  if (props.room.orgUnitId) {
    return 'border-green-300 bg-green-50 hover:shadow-sm'
  }

  return 'border-gray-200 bg-white hover:border-gray-300 hover:shadow-sm'
})

// 部门文字样式
const orgUnitTextClass = computed(() => {
  if (props.orgUnitColor && colorTextClasses[props.orgUnitColor]) {
    return colorTextClasses[props.orgUnitColor]
  }
  return 'text-green-600'
})

// 入住率
const occupancyRate = computed(() => {
  if (!props.room.capacity || props.room.capacity === 0) return 0
  return Math.round(((props.room.currentOccupancy || 0) / props.room.capacity) * 100)
})

// 入住率进度条样式
const occupancyBarClass = computed(() => {
  const rate = occupancyRate.value
  if (rate >= 90) return 'bg-red-500'
  if (rate >= 70) return 'bg-amber-500'
  if (rate >= 50) return 'bg-blue-500'
  return 'bg-green-500'
})
</script>
