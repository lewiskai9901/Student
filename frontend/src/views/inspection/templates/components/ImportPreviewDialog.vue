<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Upload, AlertTriangle, Check, X, FolderTree, ListTree } from 'lucide-vue-next'

// ==================== Types ====================

interface ImportSection {
  sectionCode: string
  sectionName: string
  itemCount: number
  conflict: boolean
}

interface ImportSummary {
  templateName: string
  sections: ImportSection[]
  totalItems: number
  conflictCount: number
}

type ConflictResolution = 'skip' | 'overwrite' | 'rename'

// ==================== Props & Emits ====================

const props = defineProps<{
  visible: boolean
  importData: any
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  confirm: [data: { importData: any; resolution: ConflictResolution }]
}>()

// ==================== State ====================

const resolution = ref<ConflictResolution>('skip')
const confirmed = ref(false)

// ==================== Computed ====================

const summary = computed<ImportSummary>(() => {
  const data = props.importData
  if (!data) {
    return { templateName: '', sections: [], totalItems: 0, conflictCount: 0 }
  }

  const sections: ImportSection[] = (data.sections || []).map((s: any) => ({
    sectionCode: s.sectionCode || s.code || '',
    sectionName: s.sectionName || s.name || '未命名分区',
    itemCount: Array.isArray(s.items) ? s.items.length : 0,
    conflict: !!s.conflict,
  }))

  return {
    templateName: data.templateName || data.name || '导入模板',
    sections,
    totalItems: sections.reduce((sum, s) => sum + s.itemCount, 0),
    conflictCount: sections.filter(s => s.conflict).length,
  }
})

const hasConflicts = computed(() => summary.value.conflictCount > 0)

const RESOLUTION_OPTIONS: { value: ConflictResolution; label: string; description: string }[] = [
  { value: 'skip', label: '跳过冲突', description: '保留现有数据，跳过冲突的分区和字段' },
  { value: 'overwrite', label: '覆盖现有', description: '用导入数据覆盖已存在的分区和字段' },
  { value: 'rename', label: '重命名导入', description: '冲突项自动添加后缀，两者都保留' },
]

// ==================== Methods ====================

function handleClose() {
  emit('update:visible', false)
  confirmed.value = false
}

function handleConfirm() {
  emit('confirm', { importData: props.importData, resolution: resolution.value })
  handleClose()
}

watch(() => props.visible, (val) => {
  if (val) {
    resolution.value = 'skip'
    confirmed.value = false
  }
})
</script>

<template>
  <Teleport to="body">
    <div
      v-if="visible"
      class="fixed inset-0 bg-black/30 flex items-center justify-center z-50"
      @click.self="handleClose"
    >
      <div class="bg-white rounded-xl shadow-xl w-[560px] max-h-[80vh] flex flex-col">
        <!-- Header -->
        <div class="flex items-center justify-between px-5 py-4 border-b border-gray-200">
          <div class="flex items-center gap-2">
            <Upload :size="18" class="text-blue-500" />
            <h3 class="text-base font-semibold text-gray-800">导入预览</h3>
          </div>
          <button class="p-1 rounded hover:bg-gray-100" @click="handleClose">
            <X :size="16" class="text-gray-400" />
          </button>
        </div>

        <!-- Body -->
        <div class="flex-1 overflow-y-auto px-5 py-4 space-y-4">
          <!-- Template name -->
          <div class="flex items-center gap-2">
            <FolderTree :size="14" class="text-gray-400" />
            <span class="text-sm font-medium text-gray-700">{{ summary.templateName }}</span>
          </div>

          <!-- Stats -->
          <div class="grid grid-cols-3 gap-3">
            <div class="rounded-lg border border-gray-200 p-2.5 text-center">
              <div class="text-lg font-semibold text-gray-700">{{ summary.sections.length }}</div>
              <div class="text-xs text-gray-400">分区</div>
            </div>
            <div class="rounded-lg border border-gray-200 p-2.5 text-center">
              <div class="text-lg font-semibold text-gray-700">{{ summary.totalItems }}</div>
              <div class="text-xs text-gray-400">字段</div>
            </div>
            <div
              class="rounded-lg border p-2.5 text-center"
              :class="hasConflicts ? 'border-yellow-300 bg-yellow-50' : 'border-gray-200'"
            >
              <div
                class="text-lg font-semibold"
                :class="hasConflicts ? 'text-yellow-600' : 'text-green-600'"
              >
                {{ summary.conflictCount }}
              </div>
              <div class="text-xs" :class="hasConflicts ? 'text-yellow-500' : 'text-gray-400'">冲突</div>
            </div>
          </div>

          <!-- Section list -->
          <div>
            <div class="flex items-center gap-1 mb-2">
              <ListTree :size="14" class="text-gray-400" />
              <span class="text-xs font-medium text-gray-600">分区列表</span>
            </div>
            <div class="space-y-1">
              <div
                v-for="section in summary.sections"
                :key="section.sectionCode"
                class="flex items-center gap-3 rounded border px-3 py-2"
                :class="section.conflict
                  ? 'border-yellow-300 bg-yellow-50/50'
                  : 'border-gray-200'"
              >
                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-2">
                    <span class="text-sm text-gray-700 truncate">{{ section.sectionName }}</span>
                    <span class="text-[10px] font-mono text-gray-400">{{ section.sectionCode }}</span>
                  </div>
                </div>
                <span class="text-xs text-gray-500 shrink-0">{{ section.itemCount }} 字段</span>
                <span
                  v-if="section.conflict"
                  class="flex items-center gap-0.5 text-[10px] text-yellow-600 shrink-0"
                >
                  <AlertTriangle :size="10" /> 冲突
                </span>
                <Check v-else :size="14" class="text-green-500 shrink-0" />
              </div>
            </div>
          </div>

          <!-- Conflict resolution -->
          <div v-if="hasConflicts" class="space-y-2">
            <div class="flex items-center gap-1">
              <AlertTriangle :size="14" class="text-yellow-500" />
              <span class="text-xs font-medium text-yellow-700">冲突处理方式</span>
            </div>
            <div class="space-y-1.5">
              <label
                v-for="opt in RESOLUTION_OPTIONS"
                :key="opt.value"
                class="flex items-start gap-2 rounded-lg border p-2.5 cursor-pointer transition"
                :class="resolution === opt.value
                  ? 'border-blue-400 bg-blue-50/50'
                  : 'border-gray-200 hover:border-gray-300'"
              >
                <input
                  type="radio"
                  name="conflict-resolution"
                  :value="opt.value"
                  v-model="resolution"
                  class="mt-0.5 accent-blue-500"
                />
                <div>
                  <div class="text-sm font-medium text-gray-700">{{ opt.label }}</div>
                  <div class="text-xs text-gray-400">{{ opt.description }}</div>
                </div>
              </label>
            </div>
          </div>

          <!-- JSON preview -->
          <details class="text-xs">
            <summary class="cursor-pointer text-gray-400 hover:text-gray-600">查看原始数据</summary>
            <pre class="mt-1 rounded bg-gray-50 border border-gray-200 p-2 text-[10px] font-mono text-gray-500 overflow-x-auto max-h-32">{{ JSON.stringify(importData, null, 2) }}</pre>
          </details>
        </div>

        <!-- Footer -->
        <div class="flex items-center justify-between px-5 py-3 border-t border-gray-200 bg-gray-50 rounded-b-xl">
          <label class="flex items-center gap-2 text-xs text-gray-500">
            <input v-model="confirmed" type="checkbox" class="rounded accent-blue-500" />
            我已确认导入内容
          </label>
          <div class="flex gap-2">
            <button
              class="px-4 py-2 text-sm text-gray-600 hover:bg-gray-100 rounded"
              @click="handleClose"
            >
              取消
            </button>
            <button
              class="px-4 py-2 text-sm bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
              :disabled="!confirmed"
              @click="handleConfirm"
            >
              确认导入
            </button>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>
