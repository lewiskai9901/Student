<template>
  <div class="border rounded-lg">
    <!-- Header -->
    <div class="flex items-center justify-between px-4 py-2.5 bg-white border-b">
      <span class="text-sm font-medium text-gray-700">字段 ({{ items.length }})</span>
      <button
        v-if="!readonly"
        class="text-xs text-blue-600 hover:text-blue-800 flex items-center gap-1"
        @click="$emit('add-item', sectionId)"
      >
        <span class="text-base leading-none">+</span> 添加字段
      </button>
    </div>

    <!-- Empty state -->
    <div v-if="items.length === 0" class="px-4 py-8 text-center text-sm text-gray-400">
      暂无字段，点击"添加字段"开始配置
    </div>

    <!-- Field rows -->
    <div v-for="item in sortedItems" :key="item.id">
      <!-- Collapsed row -->
      <div
        class="flex items-center gap-3 px-4 py-2.5 text-sm border-b last:border-b-0 cursor-pointer transition-colors"
        :class="expandedId === item.id ? 'bg-gray-50' : 'hover:bg-gray-50'"
        @click="toggleExpand(item)"
      >
        <span class="font-medium text-gray-800 truncate min-w-0 flex-1">{{ item.itemName }}</span>
        <span class="text-xs text-gray-400 shrink-0">{{ getTypeLabel(item) }}</span>
        <span class="text-xs text-gray-400 shrink-0">权重{{ item.itemWeight }}</span>
        <button
          v-if="!readonly"
          class="text-xs text-red-400 hover:text-red-600 shrink-0"
          @click.stop="$emit('delete-item', item)"
        >删除</button>
      </div>

      <!-- Inline editor -->
      <div v-if="expandedId === item.id && !readonly" class="px-4 py-4 bg-gray-50 border-b">
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="block text-xs text-gray-500 mb-1">名称</label>
            <input
              v-model="editForm.itemName"
              class="w-full px-2.5 py-1.5 text-sm border rounded focus:outline-none focus:ring-1 focus:ring-blue-400"
            />
          </div>
          <div>
            <label class="block text-xs text-gray-500 mb-1">描述</label>
            <input
              v-model="editForm.description"
              class="w-full px-2.5 py-1.5 text-sm border rounded focus:outline-none focus:ring-1 focus:ring-blue-400"
              placeholder="可选"
            />
          </div>
        </div>

        <div class="grid grid-cols-3 gap-3 mt-3">
          <div>
            <label class="block text-xs text-gray-500 mb-1">字段类型</label>
            <select
              v-model="editForm.itemType"
              class="w-full px-2.5 py-1.5 text-sm border rounded bg-white focus:outline-none focus:ring-1 focus:ring-blue-400"
            >
              <option v-for="(info, key) in ItemTypeConfig" :key="key" :value="key">
                {{ info.label }}
              </option>
            </select>
          </div>
          <div>
            <label class="block text-xs text-gray-500 mb-1">评分模式</label>
            <select
              v-if="editForm.isScored"
              v-model="editForm.scoringMode"
              class="w-full px-2.5 py-1.5 text-sm border rounded bg-white focus:outline-none focus:ring-1 focus:ring-blue-400"
            >
              <option v-for="(info, key) in ScoringModeConfig" :key="key" :value="key">
                {{ info.label }}
              </option>
            </select>
            <span v-else class="text-xs text-gray-400 leading-8">不评分</span>
          </div>
          <div>
            <label class="block text-xs text-gray-500 mb-1">权重</label>
            <input
              v-model.number="editForm.itemWeight"
              type="number"
              step="0.1"
              min="0"
              class="w-full px-2.5 py-1.5 text-sm border rounded focus:outline-none focus:ring-1 focus:ring-blue-400"
            />
          </div>
        </div>

        <div class="flex gap-5 mt-3">
          <label class="flex items-center gap-1.5 text-sm text-gray-600 cursor-pointer">
            <input v-model="editForm.isRequired" type="checkbox" class="rounded" /> 必填
          </label>
          <label class="flex items-center gap-1.5 text-sm text-gray-600 cursor-pointer">
            <input v-model="editForm.isScored" type="checkbox" class="rounded" /> 评分
          </label>
          <label class="flex items-center gap-1.5 text-sm text-gray-600 cursor-pointer">
            <input v-model="editForm.requireEvidence" type="checkbox" class="rounded" /> 需要证据
          </label>
        </div>

        <div class="mt-3">
          <label class="block text-xs text-gray-500 mb-1">帮助内容</label>
          <input
            v-model="editForm.helpContent"
            class="w-full px-2.5 py-1.5 text-sm border rounded focus:outline-none focus:ring-1 focus:ring-blue-400"
            placeholder="可选，供检查员参考"
          />
        </div>

        <div class="flex gap-2 mt-4">
          <button
            class="px-3 py-1.5 text-xs text-white bg-blue-600 rounded hover:bg-blue-700"
            @click="handleSave"
          >保存</button>
          <button
            class="px-3 py-1.5 text-xs text-gray-600 bg-white border rounded hover:bg-gray-50"
            @click="expandedId = null"
          >收起</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { TemplateItem, ResponseSet } from '@/types/insp/template'
import type { ItemType, ScoringMode } from '@/types/insp/enums'
import { ItemTypeConfig, ScoringModeConfig } from '@/types/insp/enums'

interface EditForm {
  id: number
  sectionId: number
  itemName: string
  description: string
  itemType: ItemType
  scoringMode: ScoringMode | null
  isRequired: boolean
  isScored: boolean
  requireEvidence: boolean
  itemWeight: number
  helpContent: string
}

const props = defineProps<{
  items: TemplateItem[]
  sectionId: number
  readonly: boolean
  isLeaf: boolean
  responseSets: ResponseSet[]
  allItems: TemplateItem[]
}>()

const emit = defineEmits<{
  'add-item': [sectionId: number]
  'save-item': [data: Partial<TemplateItem>]
  'delete-item': [item: TemplateItem]
}>()

const expandedId = ref<number | null>(null)
const editForm = ref<EditForm>(createEmptyForm())

const sortedItems = computed(() =>
  [...props.items].sort((a, b) => a.sortOrder - b.sortOrder)
)

function createEmptyForm(): EditForm {
  return {
    id: 0,
    sectionId: props?.sectionId ?? 0,
    itemName: '',
    description: '',
    itemType: 'TEXT',
    scoringMode: null,
    isRequired: true,
    isScored: false,
    requireEvidence: false,
    itemWeight: 1,
    helpContent: '',
  }
}

function parseScoringMode(item: TemplateItem): ScoringMode | null {
  if (!item.scoringConfig) return null
  try {
    const cfg = JSON.parse(item.scoringConfig)
    return cfg.mode ?? null
  } catch {
    return null
  }
}

function getTypeLabel(item: TemplateItem): string {
  if (item.isScored) {
    const mode = parseScoringMode(item)
    return mode ? (ScoringModeConfig[mode]?.label ?? mode) : '评分'
  }
  return ItemTypeConfig[item.itemType]?.label ?? item.itemType
}

function toggleExpand(item: TemplateItem) {
  if (props.readonly) return
  if (expandedId.value === item.id) {
    expandedId.value = null
    return
  }
  expandedId.value = item.id
  editForm.value = {
    id: item.id,
    sectionId: item.sectionId,
    itemName: item.itemName,
    description: item.description ?? '',
    itemType: item.itemType,
    scoringMode: parseScoringMode(item),
    isRequired: item.isRequired,
    isScored: item.isScored,
    requireEvidence: item.requireEvidence,
    itemWeight: item.itemWeight,
    helpContent: item.helpContent ?? '',
  }
}

function handleSave() {
  const scoringConfig = editForm.value.isScored && editForm.value.scoringMode
    ? JSON.stringify({ mode: editForm.value.scoringMode })
    : null

  emit('save-item', {
    id: editForm.value.id,
    sectionId: editForm.value.sectionId,
    itemName: editForm.value.itemName,
    description: editForm.value.description || null,
    itemType: editForm.value.itemType,
    isRequired: editForm.value.isRequired,
    isScored: editForm.value.isScored,
    requireEvidence: editForm.value.requireEvidence,
    itemWeight: editForm.value.itemWeight,
    helpContent: editForm.value.helpContent || null,
    scoringConfig,
  })
  expandedId.value = null
}
</script>
