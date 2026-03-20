<template>
  <div class="flex h-full flex-col">
    <!-- Header -->
    <div class="flex-shrink-0 border-b border-gray-200 px-4 py-3">
      <h2 class="text-sm font-semibold text-gray-900">场所树</h2>
    </div>

    <!-- Search -->
    <div class="flex-shrink-0 px-3 py-3">
      <div class="relative">
        <Search class="absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
        <input
          v-model="searchKeyword"
          type="text"
          placeholder="搜索场所..."
          class="h-8 w-full rounded-md border border-gray-300 bg-gray-50 pl-8 pr-3 text-sm text-gray-900 placeholder-gray-400 transition-colors focus:border-blue-500 focus:bg-white focus:outline-none focus:ring-1 focus:ring-blue-500"
        />
      </div>
    </div>

    <!-- Tree -->
    <div class="flex-1 overflow-y-auto px-2 pb-3">
      <div v-if="filteredTree.length > 0">
        <PlaceSidebarNode
          v-for="node in filteredTree"
          :key="node.id"
          :node="node"
          :level="0"
          :selected-id="selectedId"
          :search-keyword="searchKeyword"
          @select="(n) => emit('select', n)"
        />
      </div>
      <div v-else class="flex flex-col items-center py-10 text-gray-400">
        <MapPin class="h-8 w-8" />
        <p class="mt-2 text-xs">
          {{ searchKeyword ? '未找到匹配的场所' : '暂无场所数据' }}
        </p>
      </div>
    </div>

    <!-- Add Root Button -->
    <div class="flex-shrink-0 border-t border-gray-200 px-3 py-3">
      <button
        class="flex h-9 w-full items-center justify-center gap-1.5 rounded-lg border border-dashed border-gray-300 text-sm font-medium text-gray-600 transition-colors hover:border-blue-400 hover:bg-blue-50 hover:text-blue-600"
        @click="emit('addRoot')"
      >
        <Plus class="h-4 w-4" />
        新增场所
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Search, Plus, MapPin } from 'lucide-vue-next'
import PlaceSidebarNode from './PlaceSidebarNode.vue'
import type { PlaceTreeNode } from '@/types/universalPlace'

interface Props {
  treeData: PlaceTreeNode[]
  selectedId: number | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  select: [node: PlaceTreeNode]
  addRoot: []
}>()

const searchKeyword = ref('')

// Filter tree nodes by search keyword
const filterTree = (nodes: PlaceTreeNode[], keyword: string): PlaceTreeNode[] => {
  if (!keyword) return nodes
  const lower = keyword.toLowerCase()
  return nodes.reduce<PlaceTreeNode[]>((acc, node) => {
    const matchSelf =
      node.placeName.toLowerCase().includes(lower) ||
      node.placeCode.toLowerCase().includes(lower)
    const filteredChildren = node.children ? filterTree(node.children, keyword) : []

    if (matchSelf || filteredChildren.length > 0) {
      acc.push({
        ...node,
        children: filteredChildren.length > 0 ? filteredChildren : node.children
      })
    }
    return acc
  }, [])
}

const filteredTree = computed(() => filterTree(props.treeData, searchKeyword.value))
</script>
