<template>
  <div class="space-y-6">
    <!-- Stat Cards -->
    <div class="grid grid-cols-4 gap-3">
      <div class="flex items-center gap-3 rounded-lg border border-gray-200 bg-white px-4 py-3">
        <div class="flex h-8 w-8 items-center justify-center rounded-lg bg-blue-50">
          <Building2 class="h-4 w-4 text-blue-600" />
        </div>
        <div>
          <div class="text-lg font-bold text-gray-900">{{ stats.total }}</div>
          <div class="text-xs text-gray-500">组织总数</div>
        </div>
      </div>
      <div class="flex items-center gap-3 rounded-lg border border-gray-200 bg-white px-4 py-3">
        <div class="flex h-8 w-8 items-center justify-center rounded-lg bg-emerald-50">
          <CheckCircle class="h-4 w-4 text-emerald-600" />
        </div>
        <div>
          <div class="text-lg font-bold text-gray-900">{{ stats.active }}</div>
          <div class="text-xs text-gray-500">正常</div>
        </div>
      </div>
      <div class="flex items-center gap-3 rounded-lg border border-gray-200 bg-white px-4 py-3">
        <div class="flex h-8 w-8 items-center justify-center rounded-lg bg-orange-50">
          <XCircle class="h-4 w-4 text-orange-600" />
        </div>
        <div>
          <div class="text-lg font-bold text-gray-900">{{ stats.inactive }}</div>
          <div class="text-xs text-gray-500">冻结/撤销</div>
        </div>
      </div>
    </div>

    <!-- Type Distribution -->
    <div class="rounded-xl border border-gray-200 bg-white">
      <div class="border-b border-gray-100 px-6 py-4">
        <h3 class="text-sm font-semibold text-gray-900">类型分布</h3>
        <p class="mt-0.5 text-xs text-gray-500">各组织类型的数量统计</p>
      </div>
      <div class="px-6 py-4">
        <div v-if="typeDistribution.length > 0" class="flex flex-wrap gap-x-6 gap-y-2">
          <div
            v-for="(item, index) in typeDistribution"
            :key="item.type"
            class="flex items-center gap-1.5"
          >
            <span class="text-sm text-gray-600">{{ item.name }}</span>
            <span class="text-sm font-semibold text-gray-900">{{ item.count }}</span>
            <span v-if="index < typeDistribution.length - 1" class="ml-4 text-gray-200">|</span>
          </div>
        </div>
        <div v-else class="py-4 text-center text-sm text-gray-400">
          暂无类型数据
        </div>
      </div>
    </div>

    <!-- Getting Started Tip -->
    <div v-if="stats.total === 0" class="rounded-xl border border-dashed border-gray-300 bg-gray-50 px-6 py-10 text-center">
      <Building2 class="mx-auto h-12 w-12 text-gray-300" />
      <h3 class="mt-4 text-sm font-semibold text-gray-700">开始创建组织架构</h3>
      <p class="mt-1 text-sm text-gray-500">
        点击左侧底部"新增组织"按钮创建第一个组织单元，建立学校组织架构。
      </p>
    </div>

    <!-- Quick Guide -->
    <div v-else class="rounded-xl border border-blue-100 bg-blue-50/50 px-6 py-4">
      <div class="flex items-start gap-3">
        <Info class="mt-0.5 h-5 w-5 flex-shrink-0 text-blue-500" />
        <div>
          <p class="text-sm font-medium text-blue-800">使用提示</p>
          <p class="mt-1 text-sm text-blue-700">
            在左侧树形目录中点击任意组织节点，可查看详细信息和下级组织列表。
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  Building2,
  CheckCircle,
  XCircle,
  Info
} from 'lucide-vue-next'
import type { DepartmentResponse } from '@/api/organization'

interface Props {
  treeData: DepartmentResponse[]
}

const props = defineProps<Props>()

// Count stats from tree
const countNodes = (nodes: DepartmentResponse[]): {
  total: number; active: number; inactive: number
} => {
  let total = 0
  let active = 0
  let inactive = 0
  const traverse = (list: DepartmentResponse[]) => {
    for (const node of list) {
      total++
      if (node.status === 'ACTIVE') active++
      else inactive++
      if (node.children) traverse(node.children)
    }
  }
  traverse(nodes)
  return { total, active, inactive }
}

const stats = computed(() => countNodes(props.treeData))

// Type distribution
const typeDistribution = computed(() => {
  const typeMap = new Map<string, { name: string; color: string; count: number }>()

  const traverse = (nodes: DepartmentResponse[]) => {
    for (const node of nodes) {
      const key = node.unitType
      const existing = typeMap.get(key)
      if (existing) {
        existing.count++
      } else {
        typeMap.set(key, {
          name: node.typeName || node.unitType,
          color: ({ ROOT: '#3b82f6', BRANCH: '#8b5cf6', FUNCTIONAL: '#10b981', GROUP: '#ef4444', CONTAINER: '#f59e0b' } as Record<string, string>)[node.category || ''] || '#6b7280',
          count: 1
        })
      }
      if (node.children) traverse(node.children)
    }
  }
  traverse(props.treeData)

  return Array.from(typeMap.values()).sort((a, b) => b.count - a.count)
})

</script>
