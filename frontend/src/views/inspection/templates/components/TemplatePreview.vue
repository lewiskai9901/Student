<script setup lang="ts">
import { computed, ref, watch, onMounted } from 'vue'
import { Eye, FileText } from 'lucide-vue-next'
import { ItemTypeConfig, ScoringModeConfig } from '@/types/insp/enums'
import type { TemplateSection, TemplateItem, ResponseSet, ResponseSetOption } from '@/types/insp/template'
import { responseSetApi } from '@/api/inspection/responseSet'

const props = defineProps<{
  sections: TemplateSection[]
  itemsBySection: Map<number | string, TemplateItem[]>
  /** V20260524 Bug#4: 父组件传入选项集列表, 用于 RADIO 题渲染实际选项 */
  responseSets?: ResponseSet[]
}>()

// V20260524 Bug#4: 按需加载 ResponseSet 的选项 (responseSet 列表只有元信息, options 要单独 API)
const optionsByRsId = ref<Map<string, ResponseSetOption[]>>(new Map())
async function loadOptionsForUsedSets() {
  const usedSetIds = new Set<string>()
  for (const items of props.itemsBySection.values()) {
    for (const item of items) {
      if (item.responseSetId) usedSetIds.add(String(item.responseSetId))
    }
  }
  await Promise.all(Array.from(usedSetIds).map(async sid => {
    if (optionsByRsId.value.has(sid)) return
    try { optionsByRsId.value.set(sid, await responseSetApi.getOptions(sid as any)) }
    catch { optionsByRsId.value.set(sid, []) }
  }))
  // 触发响应式更新
  optionsByRsId.value = new Map(optionsByRsId.value)
}
onMounted(loadOptionsForUsedSets)
watch(() => props.itemsBySection, loadOptionsForUsedSets, { deep: true })

function getItemOptions(item: TemplateItem): ResponseSetOption[] {
  if (!item.responseSetId) return []
  return optionsByRsId.value.get(String(item.responseSetId)) || []
}

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

    <!-- Preview content (V20260524 Bug#4: 左侧分区目录 + 主区域真实预览) -->
    <div v-if="sortedSections.length === 0" class="flex-1 flex items-center justify-center text-sm text-gray-400">
      暂无分区和字段
    </div>
    <div v-else class="flex-1 flex overflow-hidden">
      <!-- 左侧分区目录 -->
      <aside class="w-56 border-r border-gray-200 bg-gray-50 overflow-y-auto py-3 px-2 flex-shrink-0">
        <div class="text-xs font-semibold text-gray-500 px-2 pb-2 uppercase tracking-wide">分区目录</div>
        <a v-for="node in sortedSections" :key="`nav-${node.section.id}`"
           :href="`#preview-section-${node.section.id}`"
           class="block px-2 py-1.5 rounded text-sm text-gray-700 hover:bg-blue-50 hover:text-blue-600">
          {{ node.section.sectionName }}
          <span class="text-xs text-gray-400 ml-1">({{ node.items.length }})</span>
        </a>
      </aside>

      <!-- 主预览区 -->
      <div class="flex-1 overflow-y-auto p-6">
      <div class="mx-auto max-w-2xl space-y-6">
        <template v-for="node in sortedSections" :key="node.section.id">
          <!-- Section card -->
          <div :id="`preview-section-${node.section.id}`" class="rounded-lg border border-gray-200 bg-white scroll-mt-4">
            <div class="border-b border-gray-100 px-4 py-3">
              <div class="flex items-center gap-2">
                <h3 class="text-sm font-semibold text-gray-700">{{ node.section.sectionName }}</h3>
              </div>
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
                    <span v-if="item.isRequired" class="text-red-500 text-xs">*</span>
                  </div>
                  <div v-if="item.description" class="mt-0.5 text-xs text-gray-400">{{ item.description }}</div>
                  <!-- Mock input area -->
                  <div class="mt-2">
                    <!-- V20260524 Bug#4: RADIO/SELECT 等绑了 ResponseSet 时显示真实选项 -->
                    <template v-if="item.responseSetId && getItemOptions(item).length > 0">
                      <div class="flex flex-wrap gap-1.5">
                        <span v-for="opt in getItemOptions(item)" :key="opt.id"
                              class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full border text-xs"
                              :style="opt.optionColor ? `border-color:${opt.optionColor}; color:${opt.optionColor}; background:${opt.optionColor}10` : ''">
                          <span v-if="opt.optionColor" class="w-1.5 h-1.5 rounded-full" :style="`background:${opt.optionColor}`"></span>
                          {{ opt.optionLabel }}
                          <span v-if="opt.score !== null && item.isScored" class="text-gray-400">· {{ opt.score }}分</span>
                        </span>
                      </div>
                    </template>
                    <!-- Scored item: show scoring mode placeholder -->
                    <template v-else-if="item.isScored">
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
  </div>
</template>
