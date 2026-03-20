<template>
  <div class="space-y-4">
    <!-- Type Distribution -->
    <div class="rounded-xl border border-gray-200 bg-white">
      <div class="flex items-center justify-between px-5 py-2.5">
        <h3 class="text-xs font-semibold text-gray-700">类型分布</h3>
      </div>
      <div class="border-t border-gray-100 px-5 py-3">
        <div v-if="typeDistribution.length > 0" class="flex flex-wrap items-center gap-x-4 gap-y-1.5 text-xs">
          <div
            v-for="(item, index) in typeDistribution"
            :key="item.typeCode"
            class="flex items-center gap-1"
          >
            <span class="text-gray-500">{{ item.typeName }}</span>
            <span class="font-semibold text-gray-900">{{ item.count }}</span>
            <span v-if="index < typeDistribution.length - 1" class="ml-2 text-gray-200">|</span>
          </div>
        </div>
        <div v-else class="py-2 text-center text-xs text-gray-400">
          暂无类型数据
        </div>
      </div>
    </div>

    <!-- Org Distribution -->
    <div v-if="orgStats.length > 0" class="rounded-xl border border-gray-200 bg-white">
      <div class="flex items-center justify-between px-5 py-2.5">
        <h3 class="text-xs font-semibold text-gray-700">
          按组织统计 <span class="text-gray-400">({{ orgStats.length }})</span>
        </h3>
      </div>
      <div class="border-t border-gray-100">
        <table class="w-full text-xs">
          <thead>
            <tr class="border-b border-gray-100 bg-gray-50/50">
              <th class="px-4 py-2 text-left font-medium text-gray-500">组织</th>
              <th class="px-4 py-2 text-right font-medium text-gray-500">场所数</th>
              <th class="px-4 py-2 text-right font-medium text-gray-500">容量</th>
              <th class="px-4 py-2 text-right font-medium text-gray-500">占用</th>
              <th class="px-4 py-2 text-right font-medium text-gray-500">占用率</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="org in orgStats" :key="org.orgUnitId" class="border-b border-gray-50 hover:bg-gray-50/50">
              <td class="px-4 py-2 font-medium text-gray-800">{{ org.orgUnitName || '-' }}</td>
              <td class="px-4 py-2 text-right text-gray-600">{{ org.placeCount }}</td>
              <td class="px-4 py-2 text-right text-gray-600">{{ org.totalCapacity || '-' }}</td>
              <td class="px-4 py-2 text-right text-gray-600">{{ org.totalOccupancy || '-' }}</td>
              <td class="px-4 py-2 text-right">
                <span v-if="org.totalCapacity > 0" :class="rateClass(org.occupancyRate)">{{ org.occupancyRate.toFixed(1) }}%</span>
                <span v-else class="text-gray-300">-</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Getting Started Tip -->
    <div v-if="stats.totalCount === 0" class="rounded-xl border border-dashed border-gray-300 bg-gray-50 px-6 py-8 text-center">
      <h3 class="text-sm font-semibold text-gray-700">开始创建场所架构</h3>
      <p class="mt-1 text-xs text-gray-500">
        点击左侧底部"新增场所"按钮创建第一个场所。
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { PlaceTreeNode } from '@/types/universalPlace'
import type { PlaceStatistics, OrgPlaceStats } from '@/api/universalPlace'

interface Props {
  treeData: PlaceTreeNode[]
  statistics: PlaceStatistics | null
}

const props = defineProps<Props>()

const stats = computed(() => props.statistics || {
  totalCount: 0,
  totalCapacity: 0,
  totalOccupancy: 0,
  occupancyRate: 0
})

// Type distribution from tree
const typeDistribution = computed(() => {
  const typeMap = new Map<string, { typeCode: string; typeName: string; count: number }>()

  const traverse = (nodes: PlaceTreeNode[]) => {
    for (const node of nodes) {
      const key = node.typeCode
      const existing = typeMap.get(key)
      if (existing) {
        existing.count++
      } else {
        typeMap.set(key, {
          typeCode: key,
          typeName: node.typeName || key,
          count: 1
        })
      }
      if (node.children) traverse(node.children)
    }
  }
  traverse(props.treeData)

  return Array.from(typeMap.values()).sort((a, b) => b.count - a.count)
})

const orgStats = computed<OrgPlaceStats[]>(() => props.statistics?.statsByOrg || [])

function rateClass(rate: number): string {
  if (rate >= 90) return 'font-medium text-red-500'
  if (rate >= 70) return 'font-medium text-amber-500'
  return 'text-gray-600'
}
</script>
