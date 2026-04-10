<template>
  <div>
    <!-- Node Row -->
    <div
      class="group flex cursor-pointer items-center gap-1 rounded-md px-2 py-1.5 transition-colors"
      :class="[
        isSelected
          ? 'bg-blue-50 text-blue-700'
          : 'text-gray-700 hover:bg-gray-100',
        node.status !== 'ACTIVE' && 'opacity-60'
      ]"
      :style="{ paddingLeft: `${level * 16 + 8}px` }"
      @click="emit('select', node)"
    >
      <!-- Expand/Collapse Toggle -->
      <button
        v-if="hasChildren"
        class="flex h-5 w-5 flex-shrink-0 items-center justify-center rounded transition-colors hover:bg-gray-200/60"
        @click.stop="toggleExpand"
      >
        <ChevronRight
          class="h-3.5 w-3.5 transition-transform duration-150"
          :class="isExpanded ? 'rotate-90' : ''"
        />
      </button>
      <span v-else class="w-5 flex-shrink-0" />

      <!-- Type Icon -->
      <span
        class="flex h-6 w-6 flex-shrink-0 items-center justify-center rounded"
        :style="iconStyle"
      >
        <Users v-if="isClassNode" class="h-3.5 w-3.5" />
        <Building2 v-else class="h-3.5 w-3.5" />
      </span>

      <!-- Node Name -->
      <span
        class="min-w-0 flex-1 truncate text-sm font-medium"
        :title="node.unitName"
      >
        <template v-if="searchKeyword && highlightSegments.length > 0">
          <template v-for="(seg, i) in highlightSegments" :key="i">
            <span
              v-if="seg.highlight"
              class="rounded bg-yellow-200 px-0.5"
            >{{ seg.text }}</span>
            <span v-else>{{ seg.text }}</span>
          </template>
        </template>
        <template v-else>{{ node.unitName }}</template>
      </span>

      <!-- Student count badge (CLASS nodes) -->
      <span
        v-if="isClassNode && node.attributes?.studentCount != null"
        class="flex-shrink-0 rounded bg-blue-100 px-1 py-0.5 text-[10px] leading-none font-medium text-blue-600"
      >
        {{ node.attributes.studentCount }}人
      </span>

      <!-- Disabled Indicator -->
      <span
        v-if="node.status && node.status !== 'ACTIVE'"
        :class="[
          'flex-shrink-0 rounded px-1 py-0.5 text-[10px] leading-none',
          node.status === 'FROZEN' ? 'bg-orange-100 text-orange-600' :
          node.status === 'DISSOLVED' ? 'bg-red-100 text-red-600' :
          'bg-gray-200 text-gray-500'
        ]"
      >
        {{ node.statusLabel || node.status }}
      </span>

      <!-- Quick add child button (hover) -->
      <button
        v-if="node.status === 'ACTIVE'"
        class="flex-shrink-0 rounded p-0.5 text-gray-400 opacity-0 transition-all hover:bg-blue-100 hover:text-blue-600 group-hover:opacity-100"
        title="添加子组织"
        @click.stop="emit('addChild', node)"
      >
        <Plus class="h-3.5 w-3.5" />
      </button>
    </div>

    <!-- Children -->
    <div v-if="hasChildren && isExpanded">
      <OrgSidebarNode
        v-for="child in node.children"
        :key="child.id"
        :node="child"
        :level="level + 1"
        :selected-id="selectedId"
        :search-keyword="searchKeyword"
        @select="(n) => emit('select', n)"
        @add-child="(n) => emit('addChild', n)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ChevronRight, Building2, Users, Plus } from 'lucide-vue-next'
import type { DepartmentResponse } from '@/api/organization'

interface Props {
  node: DepartmentResponse
  level: number
  selectedId: number | null
  searchKeyword: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  select: [node: DepartmentResponse]
  addChild: [node: DepartmentResponse]
}>()

const isExpanded = ref(true)

const isClassNode = computed(() => props.node.category === 'GROUP')

const hasChildren = computed(() => {
  return props.node.children && props.node.children.length > 0
})

const isSelected = computed(() => {
  return props.selectedId === props.node.id
})

// Color mapping by category
const OrgCategoryColorMap: Record<string, string> = {
  ROOT: '#3b82f6',
  BRANCH: '#8b5cf6',
  FUNCTIONAL: '#10b981',
  GROUP: '#ef4444',
  CONTAINER: '#f59e0b',
}

// Icon styling based on category color
const iconStyle = computed(() => {
  const color = OrgCategoryColorMap[props.node.category || ''] || '#6b7280'
  return {
    backgroundColor: `${color}18`,
    color: color
  }
})

// Search keyword highlighting
const highlightSegments = computed(() => {
  if (!props.searchKeyword) return []
  const name = props.node.unitName
  const lower = name.toLowerCase()
  const keyword = props.searchKeyword.toLowerCase()
  const index = lower.indexOf(keyword)
  if (index === -1) return [{ text: name, highlight: false }]

  const segments: { text: string; highlight: boolean }[] = []
  if (index > 0) {
    segments.push({ text: name.slice(0, index), highlight: false })
  }
  segments.push({ text: name.slice(index, index + props.searchKeyword.length), highlight: true })
  if (index + props.searchKeyword.length < name.length) {
    segments.push({ text: name.slice(index + props.searchKeyword.length), highlight: false })
  }
  return segments
})

const toggleExpand = () => {
  isExpanded.value = !isExpanded.value
}

// Auto-expand when searching
watch(() => props.searchKeyword, (val) => {
  if (val) {
    isExpanded.value = true
  }
})
</script>
