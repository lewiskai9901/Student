<template>
  <div class="bg-white border rounded-xl p-4">
    <div class="flex items-center justify-between mb-3">
      <span class="text-sm font-medium text-gray-800">分区属性</span>
      <div class="flex gap-2">
        <button
          v-if="isDirty && !readonly"
          class="text-xs px-3 py-1 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors"
          @click="handleSave"
        >保存</button>
        <button
          class="text-xs px-3 py-1 border border-gray-300 text-gray-600 rounded-lg hover:bg-gray-50 transition-colors"
          @click="emit('open-scoring')"
        >评分配置</button>
      </div>
    </div>

    <div class="space-y-3">
      <div>
        <label class="text-xs text-gray-500 mb-1 block">名称</label>
        <input v-model="form.sectionName" :disabled="readonly"
          class="w-full text-sm border rounded-lg px-3 py-1.5 outline-none focus:border-blue-400 disabled:bg-gray-50" />
      </div>

      <template v-if="showTargetFields">
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="text-xs text-gray-500 mb-1 block">目标类型</label>
            <select v-model="form.targetType" :disabled="readonly"
              class="w-full text-sm border rounded-lg px-3 py-1.5 outline-none focus:border-blue-400 disabled:bg-gray-50">
              <option value="">无</option>
              <option v-for="(cfg, key) in TargetTypeConfig" :key="key" :value="key">{{ cfg.label }}</option>
            </select>
          </div>
          <div>
            <label class="text-xs text-gray-500 mb-1 block">目标来源</label>
            <select v-model="form.targetSourceMode" :disabled="readonly"
              class="w-full text-sm border rounded-lg px-3 py-1.5 outline-none focus:border-blue-400 disabled:bg-gray-50">
              <option value="">无</option>
              <option value="INDEPENDENT">独立指定</option>
              <option value="PARENT_ASSOCIATED">父目标关联</option>
            </select>
          </div>
        </div>
        <div v-if="form.targetType">
          <label class="text-xs text-gray-500 mb-1 block">类型过滤</label>
          <input v-model="form.targetTypeFilter" :disabled="readonly" placeholder="如 place_category=宿舍"
            class="w-full text-sm border rounded-lg px-3 py-1.5 outline-none focus:border-blue-400 disabled:bg-gray-50" />
        </div>
      </template>

      <div class="grid grid-cols-2 gap-3 items-end">
        <div>
          <label class="text-xs text-gray-500 mb-1 block">权重</label>
          <input v-model.number="form.weight" type="number" :disabled="readonly" min="0" max="100"
            class="w-full text-sm border rounded-lg px-3 py-1.5 outline-none focus:border-blue-400 disabled:bg-gray-50" />
        </div>
        <label class="flex items-center gap-2 text-sm text-gray-700 pb-1">
          <input v-model="form.isRepeatable" type="checkbox" :disabled="readonly" class="rounded" />
          允许重复填写
        </label>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, computed, watch } from 'vue'
import type { TemplateSection } from '@/types/insp/template'
import { TargetTypeConfig } from '@/types/insp/enums'

const props = defineProps<{
  section: TemplateSection
  isFirstLevel: boolean
  readonly: boolean
}>()

const emit = defineEmits<{
  save: [data: { sectionName: string; targetType: string; targetSourceMode: string; targetTypeFilter: string; weight: number; isRepeatable: boolean }]
  'open-scoring': []
}>()

const form = reactive({
  sectionName: '',
  targetType: '',
  targetSourceMode: '',
  targetTypeFilter: '',
  weight: 0,
  isRepeatable: false,
})

function syncFromProps() {
  form.sectionName = props.section.sectionName
  form.targetType = props.section.targetType ?? ''
  form.targetSourceMode = props.section.targetSourceMode ?? ''
  form.targetTypeFilter = props.section.targetTypeFilter ?? ''
  form.weight = props.section.weight
  form.isRepeatable = props.section.isRepeatable
}

watch(() => props.section.id, syncFromProps, { immediate: true })

const showTargetFields = computed(() =>
  props.isFirstLevel || !!props.section.targetType
)

const isDirty = computed(() =>
  form.sectionName !== props.section.sectionName
  || form.targetType !== (props.section.targetType ?? '')
  || form.targetSourceMode !== (props.section.targetSourceMode ?? '')
  || form.targetTypeFilter !== (props.section.targetTypeFilter ?? '')
  || form.weight !== props.section.weight
  || form.isRepeatable !== props.section.isRepeatable
)

function handleSave() {
  emit('save', { ...form })
}
</script>
