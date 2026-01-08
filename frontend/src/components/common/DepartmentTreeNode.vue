<template>
  <div class="department-tree-node">
    <div
      class="flex items-center py-1.5 px-2 rounded cursor-pointer hover:bg-gray-100"
      :class="{ 'bg-blue-100': department.id === selectedId }"
      @click="$emit('select', department.id)"
    >
      <!-- 展开/折叠按钮 -->
      <button
        v-if="hasChildren"
        type="button"
        class="w-4 h-4 flex items-center justify-center mr-1 text-gray-400 hover:text-gray-600"
        @click.stop="$emit('toggle', department.id)"
      >
        <svg
          class="w-3 h-3 transition-transform"
          :class="{ 'rotate-90': isExpanded }"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
        </svg>
      </button>
      <span v-else class="w-4 mr-1"></span>

      <!-- 文件夹图标 -->
      <svg class="w-4 h-4 mr-2 text-yellow-500" fill="currentColor" viewBox="0 0 20 20">
        <path d="M2 6a2 2 0 012-2h5l2 2h5a2 2 0 012 2v6a2 2 0 01-2 2H4a2 2 0 01-2-2V6z" />
      </svg>

      <!-- 部门名称 -->
      <span class="text-sm text-gray-700 truncate">{{ department.deptName }}</span>
    </div>

    <!-- 子部门 -->
    <div v-if="hasChildren && isExpanded" class="pl-4">
      <DepartmentTreeNode
        v-for="child in department.children"
        :key="child.id"
        :department="child"
        :selected-id="selectedId"
        :expanded-ids="expandedIds"
        @select="$emit('select', $event)"
        @toggle="$emit('toggle', $event)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { DepartmentResponse } from '@/api/department'

const props = defineProps<{
  department: DepartmentResponse
  selectedId: number | string | null
  expandedIds: Set<number | string>
}>()

defineEmits<{
  select: [id: number]
  toggle: [id: number]
}>()

const hasChildren = computed(() => {
  return props.department.children && props.department.children.length > 0
})

const isExpanded = computed(() => {
  return props.expandedIds.has(props.department.id)
})
</script>
