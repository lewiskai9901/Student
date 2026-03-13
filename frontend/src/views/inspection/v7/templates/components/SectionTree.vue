<script setup lang="ts">
import { computed } from 'vue'
import { Plus, Trash2, FolderOpen, GripVertical } from 'lucide-vue-next'
import type { TemplateSection } from '@/types/insp/template'

const props = defineProps<{
  sections: TemplateSection[]
  selectedSectionId?: number | null
  readonly?: boolean
}>()

const emit = defineEmits<{
  select: [sectionId: number]
  add: []
  remove: [sectionId: number]
}>()

const sortedSections = computed(() =>
  [...props.sections].sort((a, b) => a.sortOrder - b.sortOrder)
)
</script>

<template>
  <div class="space-y-0.5">
    <div class="mb-3 flex items-center justify-between border-b border-gray-100 pb-2">
      <span class="text-xs font-medium uppercase tracking-wider text-gray-400">分区结构</span>
      <button
        v-if="!readonly"
        class="rounded p-1 text-gray-400 hover:bg-blue-50 hover:text-blue-500"
        title="添加分区"
        @click="emit('add')"
      >
        <Plus :size="14" />
      </button>
    </div>

    <template v-if="sortedSections.length === 0">
      <div class="py-6 text-center text-xs text-gray-400">
        暂无分区，点击上方 + 添加
      </div>
    </template>

    <div
      v-for="node in sortedSections"
      :key="node.id"
      class="section-node group"
      :class="selectedSectionId === node.id ? 'selected' : ''"
      @click="emit('select', node.id)"
    >
      <GripVertical :size="12" class="shrink-0 text-gray-300" />
      <FolderOpen :size="14" class="shrink-0 text-gray-400" />
      <span class="section-name">{{ node.sectionName }}</span>
      <span v-if="node.conditionLogic" class="section-cond-dot" title="已配置条件逻辑"></span>
      <div v-if="!readonly" class="section-actions">
        <button class="rounded p-0.5 text-gray-400 hover:text-red-500" title="删除" @click.stop="emit('remove', node.id)">
          <Trash2 :size="12" />
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.section-node {
  position: relative;
  display: flex;
  cursor: pointer;
  align-items: center;
  gap: 4px;
  border-radius: 6px;
  padding: 6px 8px;
  transition: all 0.15s;
  color: #374151;
}
.section-node:hover { background: #f3f4f6; }
.section-node.selected { background: #eff6ff; color: #2563eb; }

.section-name {
  flex: 1;
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  min-width: 0;
}

.section-cond-dot {
  width: 6px; height: 6px; border-radius: 50%; background: #f59e0b;
  flex-shrink: 0; margin-left: 2px;
}

.section-actions {
  position: absolute;
  right: 4px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.15s;
  background: inherit;
  padding-left: 4px;
}
.section-node:hover .section-actions { opacity: 1; }
.section-node.selected .section-actions { background: #eff6ff; }
.section-node:not(.selected):hover .section-actions { background: #f3f4f6; }
</style>
