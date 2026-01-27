<template>
  <div
    class="rounded-lg border-2 p-3 cursor-pointer transition-all"
    :class="[
      selected ? 'border-blue-500 bg-blue-50' : 'border-gray-200 bg-white hover:border-blue-300 hover:shadow-sm'
    ]"
    @click="emit('click')"
  >
    <!-- 楼栋名称和类型 -->
    <div class="flex items-center justify-between mb-2">
      <div class="flex items-center gap-2 min-w-0">
        <div
          class="w-8 h-8 rounded-lg flex items-center justify-center text-sm font-bold flex-shrink-0"
          :class="getBuildingTypeIconClass(building.buildingType)"
        >
          {{ getBuildingTypeIcon(building.buildingType) }}
        </div>
        <div class="min-w-0">
          <div class="font-medium text-gray-900 truncate">{{ building.spaceName }}</div>
          <div class="text-xs text-gray-500">{{ building.campusName }}</div>
        </div>
      </div>
      <span
        class="px-1.5 py-0.5 text-xs rounded flex-shrink-0"
        :class="getBuildingTypeClass(building.buildingType)"
      >
        {{ getBuildingTypeName(building.buildingType) }}
      </span>
    </div>

    <!-- 统计信息 -->
    <div class="text-xs text-gray-500 mb-2">
      {{ floorCount }} 层 · {{ roomCount }} 间房间
    </div>

    <!-- 分配比例条 -->
    <div class="h-2 bg-gray-100 rounded-full overflow-hidden flex">
      <template v-for="(segment, index) in allocationSegments" :key="index">
        <div
          class="h-full transition-all"
          :class="segment.colorClass"
          :style="{ width: segment.percentage + '%' }"
          :title="`${segment.name}: ${segment.count}间 (${segment.percentage.toFixed(1)}%)`"
        ></div>
      </template>
    </div>

    <!-- 分配图例 -->
    <div class="mt-2 flex flex-wrap gap-x-3 gap-y-1 text-xs">
      <template v-for="(segment, index) in allocationSegments.slice(0, 3)" :key="index">
        <div class="flex items-center gap-1">
          <span class="w-2 h-2 rounded-full" :class="segment.dotClass"></span>
          <span class="text-gray-600 truncate max-w-[60px]" :title="segment.name">
            {{ segment.name }}
          </span>
          <span class="text-gray-400">{{ segment.count }}</span>
        </div>
      </template>
      <div v-if="allocationSegments.length > 3" class="text-gray-400">
        +{{ allocationSegments.length - 3 }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { SpaceDTO, BuildingType } from '@/types/v2/space'
import { getBuildingTypeName } from '@/types/v2/space'

const props = defineProps<{
  building: SpaceDTO
  selected?: boolean
  orgUnitColors: Record<string, string>
}>()

const emit = defineEmits<{
  (e: 'click'): void
}>()

// 颜色映射
const colorClasses: Record<string, { bg: string; dot: string }> = {
  blue: { bg: 'bg-blue-500', dot: 'bg-blue-500' },
  green: { bg: 'bg-green-500', dot: 'bg-green-500' },
  purple: { bg: 'bg-purple-500', dot: 'bg-purple-500' },
  orange: { bg: 'bg-orange-500', dot: 'bg-orange-500' },
  cyan: { bg: 'bg-cyan-500', dot: 'bg-cyan-500' },
  pink: { bg: 'bg-pink-500', dot: 'bg-pink-500' },
  teal: { bg: 'bg-teal-500', dot: 'bg-teal-500' },
  indigo: { bg: 'bg-indigo-500', dot: 'bg-indigo-500' },
  amber: { bg: 'bg-amber-500', dot: 'bg-amber-500' },
  rose: { bg: 'bg-rose-500', dot: 'bg-rose-500' },
  gray: { bg: 'bg-gray-300', dot: 'bg-gray-400' }
}

// 楼层数
const floorCount = computed(() => props.building.children?.length || 0)

// 房间数
const roomCount = computed(() => {
  if (!props.building.children) return 0
  return props.building.children.reduce((sum, floor) =>
    sum + (floor.children?.length || 0), 0
  )
})

// 分配统计段
const allocationSegments = computed(() => {
  const segments: Array<{
    orgUnitId: string
    name: string
    count: number
    percentage: number
    colorClass: string
    dotClass: string
  }> = []

  const stats: Record<string, { name: string; count: number }> = {}
  let unassigned = 0
  let total = 0

  props.building.children?.forEach(floor => {
    floor.children?.forEach(room => {
      total++
      if (!room.orgUnitId) {
        unassigned++
      } else {
        const key = String(room.orgUnitId)
        if (!stats[key]) {
          stats[key] = { name: room.orgUnitName || '未知部门', count: 0 }
        }
        stats[key].count++
      }
    })
  })

  if (total === 0) return []

  // 已分配的部门
  Object.entries(stats).forEach(([orgUnitId, data]) => {
    const color = props.orgUnitColors[orgUnitId] || 'gray'
    const colorClass = colorClasses[color] || colorClasses.gray
    segments.push({
      orgUnitId,
      name: data.name,
      count: data.count,
      percentage: (data.count / total) * 100,
      colorClass: colorClass.bg,
      dotClass: colorClass.dot
    })
  })

  // 未分配
  if (unassigned > 0) {
    segments.push({
      orgUnitId: '0',
      name: '未分配',
      count: unassigned,
      percentage: (unassigned / total) * 100,
      colorClass: 'bg-gray-200',
      dotClass: 'bg-gray-400'
    })
  }

  // 按数量排序
  segments.sort((a, b) => b.count - a.count)

  return segments
})

// 楼栋类型样式
function getBuildingTypeClass(type?: BuildingType) {
  const classes: Record<string, string> = {
    DORMITORY: 'bg-teal-100 text-teal-700',
    TEACHING: 'bg-blue-100 text-blue-700',
    OFFICE: 'bg-slate-100 text-slate-700',
    MIXED: 'bg-purple-100 text-purple-700'
  }
  return classes[type || ''] || 'bg-gray-100 text-gray-600'
}

function getBuildingTypeIconClass(type?: BuildingType) {
  const classes: Record<string, string> = {
    DORMITORY: 'bg-teal-100 text-teal-600',
    TEACHING: 'bg-blue-100 text-blue-600',
    OFFICE: 'bg-slate-100 text-slate-600',
    MIXED: 'bg-purple-100 text-purple-600'
  }
  return classes[type || ''] || 'bg-gray-100 text-gray-600'
}

function getBuildingTypeIcon(type?: BuildingType) {
  const icons: Record<string, string> = {
    DORMITORY: '宿',
    TEACHING: '教',
    OFFICE: '办',
    MIXED: '综'
  }
  return icons[type || ''] || '楼'
}
</script>
