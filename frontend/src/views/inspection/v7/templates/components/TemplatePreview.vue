<script setup lang="ts">
import { computed } from 'vue'
import { Eye, FileText } from 'lucide-vue-next'
import { ItemTypeConfig, ScoringModeConfig } from '@/types/insp/enums'
import type { TemplateSection, TemplateItem } from '@/types/insp/template'

const props = defineProps<{
  sections: TemplateSection[]
  itemsBySection: Map<string, TemplateItem[]>
}>()

const emit = defineEmits<{
  close: []
}>()

interface SectionNode {
  section: TemplateSection
  items: TemplateItem[]
}

const sortedSections = computed<SectionNode[]>(() =>
  [...props.sections]
    .sort((a, b) => a.sortOrder - b.sortOrder)
    .map(s => ({ section: s, items: props.itemsBySection.get(s.id) || [] }))
)

const totalItems = computed(() => {
  let count = 0
  for (const items of props.itemsBySection.values()) {
    count += items.length
  }
  return count
})
</script>

<template>
  <div class="flex h-full flex-col">
    <!-- Header -->
    <div class="flex items-center justify-between border-b border-gray-200 bg-gray-50 px-4 py-3">
      <div class="flex items-center gap-2">
        <Eye :size="16" class="text-blue-500" />
        <span class="text-sm font-medium text-gray-700">模板预览</span>
        <span class="text-xs text-gray-400">{{ sections.length }} 个分区 / {{ totalItems }} 个字段</span>
      </div>
      <button
        class="rounded-md border border-gray-300 px-3 py-1 text-sm text-gray-600 hover:bg-white"
        @click="emit('close')"
      >
        关闭预览
      </button>
    </div>

    <!-- Preview content -->
    <div class="flex-1 overflow-y-auto p-6">
      <div v-if="sortedSections.length === 0" class="py-12 text-center text-sm text-gray-400">
        暂无分区和字段
      </div>
      <div v-else class="mx-auto max-w-2xl space-y-6">
        <template v-for="node in sortedSections" :key="node.section.id">
          <!-- Section card -->
          <div class="rounded-lg border border-gray-200 bg-white">
            <div class="border-b border-gray-100 px-4 py-3">
              <div class="flex items-center gap-2">
                <h3 class="text-sm font-semibold text-gray-700">{{ node.section.sectionName }}</h3>
              </div>
              <span v-if="node.section.weight > 0" class="text-xs text-gray-400">权重: {{ node.section.weight }}%</span>
            </div>
            <div class="divide-y divide-gray-50 px-4">
              <div
                v-for="item in node.items"
                :key="item.id"
                class="flex items-start gap-3 py-3"
              >
                <div class="flex-1">
                  <div class="flex items-center gap-2">
                    <span class="text-sm text-gray-700">{{ item.itemName }}</span>
                  </div>
                  <div v-if="item.description" class="mt-0.5 text-xs text-gray-400">{{ item.description }}</div>
                  <!-- Mock input area -->
                  <div class="mt-2">
                    <!-- Scored item: show scoring mode placeholder -->
                    <template v-if="item.isScored">
                      <div class="flex items-center gap-2 rounded border border-dashed border-blue-200 bg-blue-50 px-3 py-1.5 text-xs text-blue-600">
                        评分项 · {{ (() => { try { const c = JSON.parse(item.scoringConfig || '{}'); return ScoringModeConfig[c.mode as keyof typeof ScoringModeConfig]?.label || '评分' } catch { return '评分' } })() }}
                      </div>
                    </template>
                    <!-- Capture item: show input type placeholder -->
                    <template v-else>
                      <div
                        v-if="['TEXT', 'NUMBER', 'BARCODE'].includes(item.itemType)"
                        class="h-8 rounded border border-dashed border-gray-300 bg-gray-50"
                      />
                      <div
                        v-else-if="['TEXTAREA', 'RICH_TEXT'].includes(item.itemType)"
                        class="h-16 rounded border border-dashed border-gray-300 bg-gray-50"
                      />
                      <div
                        v-else-if="['SELECT', 'MULTI_SELECT', 'RADIO', 'CHECKBOX'].includes(item.itemType)"
                        class="flex items-center gap-2"
                      >
                        <div class="h-8 flex-1 rounded border border-dashed border-gray-300 bg-gray-50" />
                      </div>
                      <div
                        v-else-if="['PHOTO', 'VIDEO', 'FILE_UPLOAD', 'SIGNATURE'].includes(item.itemType)"
                        class="flex h-16 items-center justify-center rounded border border-dashed border-gray-300 bg-gray-50 text-xs text-gray-400"
                      >
                        <FileText :size="14" class="mr-1" /> {{ ItemTypeConfig[item.itemType]?.label }}
                      </div>
                      <div
                        v-else
                        class="h-8 rounded border border-dashed border-gray-300 bg-gray-50"
                      />
                    </template>
                  </div>
                </div>
                <div class="flex shrink-0 items-center gap-1">
                  <span v-if="item.conditionLogic" class="rounded bg-amber-100 px-1.5 py-0.5 text-[10px] text-amber-600">条件</span>
                  <span
                    class="rounded px-1.5 py-0.5 text-xs"
                    :class="item.isScored ? 'bg-blue-50 text-blue-600' : 'bg-gray-100 text-gray-500'"
                  >
                    {{ item.isScored ? '评分' : (ItemTypeConfig[item.itemType]?.label || item.itemType) }}
                  </span>
                </div>
              </div>
              <div v-if="node.items.length === 0" class="py-4 text-center text-xs text-gray-400">
                此分区暂无字段
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>
