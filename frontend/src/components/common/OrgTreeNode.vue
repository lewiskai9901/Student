<template>
  <div class="org-tree-node">
    <div
      class="flex items-center gap-1 rounded px-2 py-1.5 hover:bg-white"
      :class="{ 'bg-blue-50': selectedIds.includes(node.id) }"
    >
      <!-- 展开/折叠按钮 -->
      <button
        v-if="hasChildren"
        @click="$emit('toggle', node.id)"
        class="flex h-5 w-5 items-center justify-center rounded hover:bg-gray-200"
      >
        <ChevronRight
          class="h-4 w-4 text-gray-400 transition-transform"
          :class="{ 'rotate-90': isExpanded }"
        />
      </button>
      <div v-else class="w-5"></div>

      <!-- 复选框 -->
      <input
        type="checkbox"
        :checked="selectedIds.includes(node.id)"
        @change="handleCheck"
        class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
      />

      <!-- 图标 -->
      <component :is="nodeIcon" class="h-4 w-4" :class="iconClass" />

      <!-- 名称 -->
      <span class="text-sm text-gray-700">{{ node.unitName || node.name }}</span>

      <!-- 类型标签 -->
      <span
        v-if="nodeType"
        class="ml-1 rounded px-1 py-0.5 text-xs"
        :class="typeTagClass"
      >
        {{ typeLabel }}
      </span>
    </div>

    <!-- 子节点 -->
    <div v-if="hasChildren && isExpanded" class="ml-5 border-l border-gray-200 pl-1">
      <OrgTreeNode
        v-for="child in node.children"
        :key="child.id"
        :node="child"
        :selected-ids="selectedIds"
        :expanded-keys="expandedKeys"
        @toggle="$emit('toggle', $event)"
        @check="$emit('check', $event.id, $event.checked)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ChevronRight, Building2, Users, GraduationCap, Home, Folder } from 'lucide-vue-next'
import type { OrgUnitTreeNode } from '@/types'

const props = defineProps<{
  node: OrgUnitTreeNode
  selectedIds: (number | string)[]
  expandedKeys: (number | string)[]
}>()

const emit = defineEmits<{
  toggle: [id: number | string]
  check: [payload: { id: number | string; checked: boolean }]
}>()

const hasChildren = computed(() => props.node.children && props.node.children.length > 0)
const isExpanded = computed(() => props.expandedKeys.includes(props.node.id))

// 获取节点类型（兼容多种字段名）
const nodeType = computed(() => {
  const type = (props.node as any).unitType || (props.node as any).type || ''
  return type.toLowerCase()
})

// 节点图标
const nodeIcon = computed(() => {
  const iconMap: Record<string, any> = {
    school: Building2,
    college: Building2,
    department: Building2,
    teaching_group: Users,
    grade: GraduationCap,
    class: Users,
    dormitory_building: Home
  }
  return iconMap[nodeType.value] || Folder
})

// 图标样式
const iconClass = computed(() => {
  const classMap: Record<string, string> = {
    school: 'text-blue-500',
    college: 'text-indigo-500',
    department: 'text-purple-500',
    teaching_group: 'text-teal-500',
    grade: 'text-green-500',
    class: 'text-orange-500',
    dormitory_building: 'text-cyan-500'
  }
  return classMap[nodeType.value] || 'text-gray-400'
})

// 类型标签
const typeLabel = computed(() => {
  const labelMap: Record<string, string> = {
    school: '学校',
    college: '学院',
    department: '系部',
    teaching_group: '教研组',
    grade: '年级',
    class: '班级',
    dormitory_building: '宿舍楼'
  }
  return labelMap[nodeType.value] || nodeType.value
})

// 类型标签样式
const typeTagClass = computed(() => {
  const classMap: Record<string, string> = {
    school: 'bg-blue-100 text-blue-600',
    college: 'bg-indigo-100 text-indigo-600',
    department: 'bg-purple-100 text-purple-600',
    teaching_group: 'bg-teal-100 text-teal-600',
    grade: 'bg-green-100 text-green-600',
    class: 'bg-orange-100 text-orange-600',
    dormitory_building: 'bg-cyan-100 text-cyan-600'
  }
  return classMap[nodeType.value] || 'bg-gray-100 text-gray-600'
})

const handleCheck = (e: Event) => {
  const checked = (e.target as HTMLInputElement).checked
  emit('check', { id: props.node.id, checked })
}
</script>
