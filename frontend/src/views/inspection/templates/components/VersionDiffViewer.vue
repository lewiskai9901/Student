<script setup lang="ts">
import { computed } from 'vue'
import { ArrowRight, Plus as PlusIcon, Minus, Pencil } from 'lucide-vue-next'

// ==================== Props ====================

const props = withDefaults(defineProps<{
  oldVersion: Record<string, any>
  newVersion: Record<string, any>
  versionLabels?: [string, string]
}>(), {
  versionLabels: () => ['旧版本', '新版本'],
})

// ==================== Types ====================

type DiffType = 'added' | 'removed' | 'changed' | 'unchanged'

interface DiffEntry {
  key: string
  type: DiffType
  oldValue: any
  newValue: any
  path: string
}

// ==================== Diff Logic ====================

function flattenObj(obj: Record<string, any>, prefix = ''): Record<string, any> {
  const result: Record<string, any> = {}
  for (const key of Object.keys(obj)) {
    const fullKey = prefix ? `${prefix}.${key}` : key
    const val = obj[key]
    if (val != null && typeof val === 'object' && !Array.isArray(val)) {
      Object.assign(result, flattenObj(val, fullKey))
    } else {
      result[fullKey] = val
    }
  }
  return result
}

function formatValue(val: any): string {
  if (val === undefined || val === null) return '(empty)'
  if (Array.isArray(val)) return JSON.stringify(val)
  if (typeof val === 'object') return JSON.stringify(val)
  return String(val)
}

const diffs = computed<DiffEntry[]>(() => {
  const oldFlat = flattenObj(props.oldVersion || {})
  const newFlat = flattenObj(props.newVersion || {})
  const allKeys = new Set([...Object.keys(oldFlat), ...Object.keys(newFlat)])

  const entries: DiffEntry[] = []
  for (const key of Array.from(allKeys).sort()) {
    const hasOld = key in oldFlat
    const hasNew = key in newFlat
    const oldVal = oldFlat[key]
    const newVal = newFlat[key]

    if (!hasOld && hasNew) {
      entries.push({ key, type: 'added', oldValue: undefined, newValue: newVal, path: key })
    } else if (hasOld && !hasNew) {
      entries.push({ key, type: 'removed', oldValue: oldVal, newValue: undefined, path: key })
    } else if (JSON.stringify(oldVal) !== JSON.stringify(newVal)) {
      entries.push({ key, type: 'changed', oldValue: oldVal, newValue: newVal, path: key })
    } else {
      entries.push({ key, type: 'unchanged', oldValue: oldVal, newValue: newVal, path: key })
    }
  }
  return entries
})

const stats = computed(() => ({
  added: diffs.value.filter(d => d.type === 'added').length,
  removed: diffs.value.filter(d => d.type === 'removed').length,
  changed: diffs.value.filter(d => d.type === 'changed').length,
  unchanged: diffs.value.filter(d => d.type === 'unchanged').length,
}))

const hasChanges = computed(() =>
  stats.value.added + stats.value.removed + stats.value.changed > 0
)

const changedDiffs = computed(() => diffs.value.filter(d => d.type !== 'unchanged'))

// ==================== Style Helpers ====================

function getTypeBg(type: DiffType): string {
  switch (type) {
    case 'added': return 'bg-green-50 border-green-200'
    case 'removed': return 'bg-red-50 border-red-200'
    case 'changed': return 'bg-yellow-50 border-yellow-200'
    default: return 'bg-white border-gray-200'
  }
}

function getTypeLabel(type: DiffType): string {
  switch (type) {
    case 'added': return '新增'
    case 'removed': return '移除'
    case 'changed': return '修改'
    default: return '无变化'
  }
}

function getTypeColor(type: DiffType): string {
  switch (type) {
    case 'added': return 'text-green-600'
    case 'removed': return 'text-red-600'
    case 'changed': return 'text-yellow-700'
    default: return 'text-gray-400'
  }
}
</script>

<template>
  <div class="space-y-3">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <h3 class="text-sm font-medium text-gray-700">版本差异对比</h3>
      <div class="flex items-center gap-3 text-xs">
        <span class="text-gray-500">{{ versionLabels[0] }}</span>
        <ArrowRight :size="12" class="text-gray-400" />
        <span class="text-gray-500">{{ versionLabels[1] }}</span>
      </div>
    </div>

    <!-- Stats bar -->
    <div class="flex items-center gap-4 text-xs">
      <span v-if="stats.added > 0" class="flex items-center gap-1 text-green-600">
        <PlusIcon :size="12" /> {{ stats.added }} 新增
      </span>
      <span v-if="stats.removed > 0" class="flex items-center gap-1 text-red-600">
        <Minus :size="12" /> {{ stats.removed }} 移除
      </span>
      <span v-if="stats.changed > 0" class="flex items-center gap-1 text-yellow-700">
        <Pencil :size="12" /> {{ stats.changed }} 修改
      </span>
      <span v-if="!hasChanges" class="text-gray-400">无变化</span>
    </div>

    <!-- No changes -->
    <div v-if="!hasChanges" class="text-center py-8 text-sm text-gray-400">
      两个版本完全一致，无任何差异
    </div>

    <!-- Diff list -->
    <div v-else class="space-y-1.5">
      <div
        v-for="diff in changedDiffs"
        :key="diff.key"
        class="rounded-lg border p-2.5"
        :class="getTypeBg(diff.type)"
      >
        <div class="flex items-center gap-2 mb-1">
          <span
            class="text-[10px] px-1.5 py-0.5 rounded-full font-medium"
            :class="getTypeColor(diff.type)"
          >
            {{ getTypeLabel(diff.type) }}
          </span>
          <span class="text-xs font-mono text-gray-600">{{ diff.path }}</span>
        </div>

        <div class="grid grid-cols-2 gap-2">
          <!-- Old value -->
          <div class="rounded bg-white/70 p-1.5">
            <div class="text-[10px] text-gray-400 mb-0.5">{{ versionLabels[0] }}</div>
            <div
              class="text-xs font-mono break-all"
              :class="diff.type === 'removed' ? 'text-red-700 line-through' : 'text-gray-600'"
            >
              {{ diff.oldValue !== undefined ? formatValue(diff.oldValue) : '--' }}
            </div>
          </div>
          <!-- New value -->
          <div class="rounded bg-white/70 p-1.5">
            <div class="text-[10px] text-gray-400 mb-0.5">{{ versionLabels[1] }}</div>
            <div
              class="text-xs font-mono break-all"
              :class="diff.type === 'added' ? 'text-green-700 font-medium' : 'text-gray-600'"
            >
              {{ diff.newValue !== undefined ? formatValue(diff.newValue) : '--' }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Summary -->
    <div class="rounded border border-dashed border-gray-300 bg-gray-50 p-2 text-xs text-gray-500">
      共 {{ diffs.length }} 个字段，
      {{ stats.added }} 新增 /
      {{ stats.removed }} 移除 /
      {{ stats.changed }} 修改 /
      {{ stats.unchanged }} 无变化
    </div>
  </div>
</template>
