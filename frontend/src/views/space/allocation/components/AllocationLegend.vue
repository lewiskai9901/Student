<template>
  <div class="flex flex-wrap items-center gap-4 text-sm">
    <!-- 总数 -->
    <div class="flex items-center gap-2 pr-4 border-r border-gray-200">
      <span class="text-gray-500">总计:</span>
      <span class="font-bold text-gray-900">{{ statistics.total }} 间</span>
    </div>

    <!-- 各部门统计 -->
    <template v-for="(data, orgUnitId) in statistics.byOrgUnit" :key="orgUnitId">
      <div class="flex items-center gap-1.5">
        <span
          class="w-3 h-3 rounded-full"
          :class="getDotClass(orgUnitId)"
        ></span>
        <span class="text-gray-600 max-w-[100px] truncate" :title="data.name">
          {{ data.name }}
        </span>
        <span class="text-gray-900 font-medium">{{ data.count }}</span>
        <span class="text-gray-400">({{ getPercentage(data.count) }}%)</span>
      </div>
    </template>

    <!-- 未分配 -->
    <div v-if="statistics.unassigned > 0" class="flex items-center gap-1.5">
      <span class="w-3 h-3 rounded-full bg-gray-300"></span>
      <span class="text-gray-500">未分配</span>
      <span class="text-gray-700 font-medium">{{ statistics.unassigned }}</span>
      <span class="text-gray-400">({{ getPercentage(statistics.unassigned) }}%)</span>
    </div>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{
  statistics: {
    total: number
    unassigned?: number
    byOrgUnit: Record<string, { name: string; count: number }>
  }
  orgUnitColors: Record<string, string>
}>()

// 颜色映射
const dotClasses: Record<string, string> = {
  blue: 'bg-blue-500',
  green: 'bg-green-500',
  purple: 'bg-purple-500',
  orange: 'bg-orange-500',
  cyan: 'bg-cyan-500',
  pink: 'bg-pink-500',
  teal: 'bg-teal-500',
  indigo: 'bg-indigo-500',
  amber: 'bg-amber-500',
  rose: 'bg-rose-500'
}

function getDotClass(orgUnitId: string): string {
  const color = props.orgUnitColors[orgUnitId]
  return dotClasses[color] || 'bg-gray-400'
}

function getPercentage(count: number): string {
  if (props.statistics.total === 0) return '0'
  return ((count / props.statistics.total) * 100).toFixed(1)
}
</script>
