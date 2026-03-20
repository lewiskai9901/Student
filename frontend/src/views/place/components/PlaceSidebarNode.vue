<template>
  <div>
    <!-- Node Row -->
    <div
      class="group flex cursor-pointer items-center gap-1 rounded-md px-2 py-1.5 transition-colors"
      :class="[
        isSelected
          ? 'bg-blue-50 text-blue-700'
          : 'text-gray-700 hover:bg-gray-100',
        node.status === 0 && 'opacity-60'
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

      <!-- Node Name -->
      <span
        class="min-w-0 flex-1 truncate text-sm font-medium"
        :title="node.placeName"
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
        <template v-else>{{ node.placeName }}</template>
      </span>

      <!-- Tree Display Attributes -->
      <span
        v-for="attr in node.treeDisplayAttributes"
        :key="attr.key"
        class="flex-shrink-0 rounded bg-blue-50 px-1 py-0.5 text-[10px] leading-none text-blue-600"
        :title="attr.label"
      >
        {{ attr.value }}
      </span>

      <!-- Gender Indicator -->
      <span
        v-if="genderIndicator"
        class="flex-shrink-0 text-xs font-bold leading-none"
        :style="{ color: genderIndicator.color }"
        :title="{ 'MALE': '男', 'FEMALE': '女', 'MIXED': '混合' }[node.effectiveGender || node.gender || ''] || ''"
      >{{ genderIndicator.icon }}</span>

      <!-- Capacity Badge -->
      <span
        v-if="node.hasCapacity && node.capacity"
        class="flex-shrink-0 rounded bg-gray-100 px-1 py-0.5 text-[10px] leading-none text-gray-500"
      >
        {{ node.currentOccupancy || 0 }}/{{ node.capacity }}
      </span>

      <!-- Status Indicator -->
      <span
        v-if="node.status === 0"
        class="flex-shrink-0 rounded bg-gray-200 px-1 py-0.5 text-[10px] leading-none text-gray-500"
      >
        停
      </span>
      <span
        v-else-if="node.status === 2"
        class="flex-shrink-0 rounded bg-amber-100 px-1 py-0.5 text-[10px] leading-none text-amber-600"
      >
        维
      </span>
    </div>

    <!-- Children -->
    <div v-if="hasChildren && isExpanded">
      <PlaceSidebarNode
        v-for="child in node.children"
        :key="child.id"
        :node="child"
        :level="level + 1"
        :selected-id="selectedId"
        :search-keyword="searchKeyword"
        @select="(n) => emit('select', n)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ChevronRight } from 'lucide-vue-next'
import type { PlaceTreeNode } from '@/types/universalPlace'

interface Props {
  node: PlaceTreeNode
  level: number
  selectedId: number | null
  searchKeyword: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  select: [node: PlaceTreeNode]
}>()

const isExpanded = ref(true)

const hasChildren = computed(() => {
  return props.node.children && props.node.children.length > 0
})

const isSelected = computed(() => {
  return props.selectedId === props.node.id
})

// Gender indicator
const genderIndicator = computed(() => {
  const g = props.node.effectiveGender || props.node.gender
  if (!g) return null
  const map: Record<string, { icon: string; color: string }> = {
    'MALE': { icon: '♂', color: '#3b82f6' },
    'FEMALE': { icon: '♀', color: '#ec4899' },
    'MIXED': { icon: '⚥', color: '#8b5cf6' }
  }
  return map[g] || null
})

// Search keyword highlighting
const highlightSegments = computed(() => {
  if (!props.searchKeyword) return []
  const name = props.node.placeName
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
