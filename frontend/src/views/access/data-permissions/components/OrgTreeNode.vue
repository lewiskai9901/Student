<template>
  <div>
    <div class="flex items-center gap-1.5 rounded px-1.5 py-1 hover:bg-gray-50">
      <button
        class="flex h-4 w-4 items-center justify-center text-gray-400 hover:text-gray-600"
        @click="emit('toggle-expand', node.id)"
      >
        <ChevronDown
          v-if="node.children.length"
          class="h-3.5 w-3.5 transition-transform"
          :class="{ '-rotate-90': !isExpanded }"
        />
      </button>
      <input
        type="checkbox"
        :checked="selected.has(String(node.id))"
        class="h-3.5 w-3.5"
        @change="emit('toggle-select', node.id)"
      />
      <Building2 class="h-3.5 w-3.5 text-blue-500" />
      <span class="text-xs text-gray-700">{{ node.name }}</span>
      <span v-if="node.typeLabel" class="text-[10px] text-gray-400">{{ node.typeLabel }}</span>
    </div>

    <div v-if="isExpanded && node.children.length" class="ml-5 border-l border-gray-200 pl-2">
      <OrgTreeNode
        v-for="child in node.children"
        :key="String(child.id)"
        :node="child"
        :selected="selected"
        :expanded="expanded"
        @toggle-select="(id) => emit('toggle-select', id)"
        @toggle-expand="(id) => emit('toggle-expand', id)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { LongId } from '@/types/common'
import type { ScopeTreeNode } from './CustomScopeTreePicker.vue'
import { computed } from 'vue'
import { ChevronDown, Building2 } from 'lucide-vue-next'

const props = defineProps<{
  node: ScopeTreeNode
  selected: Set<string>
  expanded: Set<string>
}>()

const emit = defineEmits<{
  'toggle-select': [id: LongId | string]
  'toggle-expand': [id: LongId | string]
}>()

const isExpanded = computed(() => props.expanded.has(String(props.node.id)))
</script>
