<script setup lang="ts">
/**
 * PopulationInput - Manual population input (MANUAL mode)
 *
 * A focused input component for entering the actual headcount
 * when the population source is set to MANUAL mode.
 */
import { computed } from 'vue'
import { Users } from 'lucide-vue-next'

const props = withDefaults(defineProps<{
  modelValue: number
  label?: string
  min?: number
  max?: number
  placeholder?: string
}>(), {
  label: '实际人数',
  min: 0,
  max: 99999,
  placeholder: '请输入当前实际人数',
})

const emit = defineEmits<{
  'update:modelValue': [value: number]
}>()

// ---------- Computed ----------

const value = computed({
  get: () => props.modelValue ?? 0,
  set: (val: number) => emit('update:modelValue', val),
})

const isValid = computed(() => value.value > 0)
</script>

<template>
  <div class="population-input rounded-md border border-gray-200 bg-white p-3">
    <div class="flex items-center gap-2 mb-2">
      <Users class="w-4 h-4 text-gray-500" />
      <span class="text-sm font-medium text-gray-700">{{ label }}</span>
    </div>

    <el-input-number
      v-model="value"
      :min="min"
      :max="max"
      :step="1"
      :placeholder="placeholder"
      controls-position="right"
      class="!w-full"
    />

    <p class="mt-1.5 text-xs" :class="isValid ? 'text-green-500' : 'text-gray-400'">
      <template v-if="isValid">
        当前人数: {{ value }} 人
      </template>
      <template v-else>
        请输入大于 0 的实际人数，用于人口归一化计算
      </template>
    </p>
  </div>
</template>
