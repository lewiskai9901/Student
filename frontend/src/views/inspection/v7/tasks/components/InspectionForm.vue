<script setup lang="ts">
/**
 * InspectionForm - Dynamic inspection form renderer
 *
 * Renders a multi-section inspection form based on template structure.
 * Each section is collapsible, and each item within is rendered by
 * the FormItemRenderer component.
 */
import { ref } from 'vue'
import { ChevronDown, ChevronRight } from 'lucide-vue-next'
import FormItemRenderer from './FormItemRenderer.vue'

interface SectionDef {
  id: number
  sectionName: string
  sectionCode?: string
  weight?: number
  items?: ItemDef[]
}

interface ItemDef {
  id: number
  itemCode: string
  itemName: string
  itemType: string
  description?: string | null
  helpContent?: string | null
  isRequired?: boolean
  isScored?: boolean
  scoringConfig?: string | null
  validationRules?: string | null
  config?: string | null
  responseSetId?: number | null
}

const props = withDefaults(defineProps<{
  sections: SectionDef[]
  formData: Record<string, any>
  readonly?: boolean
}>(), {
  readonly: false,
})

const emit = defineEmits<{
  'update:formData': [data: Record<string, any>]
}>()

// ---------- Collapsed State ----------

const collapsedSections = ref<Set<number>>(new Set())

function toggleSection(sectionId: number) {
  if (collapsedSections.value.has(sectionId)) {
    collapsedSections.value.delete(sectionId)
  } else {
    collapsedSections.value.add(sectionId)
  }
  // Trigger reactivity
  collapsedSections.value = new Set(collapsedSections.value)
}

function isSectionCollapsed(sectionId: number): boolean {
  return collapsedSections.value.has(sectionId)
}

// ---------- Item Count per Section ----------

function getSectionItemCount(section: SectionDef): number {
  return section.items?.length ?? 0
}

function getSectionFilledCount(section: SectionDef): number {
  if (!section.items) return 0
  return section.items.filter(item => {
    const val = props.formData[item.itemCode]
    return val != null && val !== '' && val !== false
  }).length
}

// ---------- Field Update ----------

function handleFieldUpdate(itemCode: string, value: any) {
  const updated = { ...props.formData, [itemCode]: value }
  emit('update:formData', updated)
}

// ---------- Section Validation State ----------

function hasSectionErrors(section: SectionDef): boolean {
  // Simple check: any required field that is empty
  if (!section.items) return false
  return section.items.some(item => {
    if (!item.isRequired) return false
    const val = props.formData[item.itemCode]
    return val == null || val === ''
  })
}
</script>

<template>
  <div class="inspection-form space-y-3">
    <div
      v-for="section in sections"
      :key="section.id"
      class="rounded-lg border border-gray-200 bg-white overflow-hidden"
    >
      <!-- Section Header -->
      <div
        class="flex items-center justify-between px-4 py-2.5 bg-gray-50 cursor-pointer select-none hover:bg-gray-100 transition"
        @click="toggleSection(section.id)"
      >
        <div class="flex items-center gap-2">
          <component
            :is="isSectionCollapsed(section.id) ? ChevronRight : ChevronDown"
            class="w-4 h-4 text-gray-400"
          />
          <span class="text-sm font-medium text-gray-700">
            {{ section.sectionName }}
          </span>
          <el-tag
            v-if="section.weight && section.weight !== 1"
            size="small"
            type="info"
          >
            权重 {{ section.weight }}
          </el-tag>
        </div>
        <div class="flex items-center gap-2">
          <span class="text-xs text-gray-400">
            {{ getSectionFilledCount(section) }}/{{ getSectionItemCount(section) }}
          </span>
          <span
            v-if="hasSectionErrors(section) && !readonly"
            class="w-2 h-2 rounded-full bg-red-400"
          />
        </div>
      </div>

      <!-- Section Body -->
      <div
        v-show="!isSectionCollapsed(section.id)"
        class="px-4 py-3 space-y-4"
      >
        <div
          v-for="item in section.items"
          :key="item.id"
          class="form-item"
        >
          <!-- Item Label -->
          <div class="flex items-center gap-1 mb-1">
            <span class="text-sm text-gray-700">{{ item.itemName }}</span>
            <span v-if="item.isRequired" class="text-red-400 text-xs">*</span>
            <el-tooltip
              v-if="item.helpContent"
              :content="item.helpContent"
              placement="top"
            >
              <span class="text-xs text-gray-300 cursor-help">[?]</span>
            </el-tooltip>
          </div>

          <!-- Item Description -->
          <p v-if="item.description" class="text-xs text-gray-400 mb-1.5">
            {{ item.description }}
          </p>

          <!-- Field Renderer -->
          <FormItemRenderer
            :item="item"
            :model-value="formData[item.itemCode]"
            :readonly="readonly"
            @update:model-value="(val: any) => handleFieldUpdate(item.itemCode, val)"
          />
        </div>

        <!-- Empty Section -->
        <div
          v-if="!section.items || section.items.length === 0"
          class="py-4 text-center text-sm text-gray-400"
        >
          此分区暂无检查项
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div
      v-if="sections.length === 0"
      class="py-8 text-center text-sm text-gray-400 border border-dashed border-gray-200 rounded-lg"
    >
      暂无表单内容
    </div>
  </div>
</template>
