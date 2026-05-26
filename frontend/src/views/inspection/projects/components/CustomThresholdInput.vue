<template>
  <span class="ct-wrap">
    <span v-if="mode === 'RATING_SCALE' || mode === 'DIRECT_SCORE'" class="ct-prefix">≤</span>
    <span v-if="mode === 'DEDUCTION'" class="ct-prefix">扣 ≥</span>
    <el-input-number
      :model-value="displayValue"
      @update:model-value="onInput"
      :min="0"
      :max="maxInput"
      :step="step"
      :precision="precision"
      :disabled="disabled"
      size="small"
      placeholder="留空=用预设"
      style="width:130px"
    />
    <span class="ct-suffix">{{ unitLabel }}</span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  modelValue: number | null
  mode: string
  max: number
  disabled?: boolean
}>()

const emit = defineEmits<(e: 'update:modelValue', v: number | null) => void>()

// sev → 用户单位
const displayValue = computed<number | null>(() => {
  const sev = props.modelValue
  if (sev == null) return null
  switch (props.mode) {
    case 'RATING_SCALE':
    case 'DIRECT_SCORE':
      return parseFloat((props.max * (1 - sev)).toFixed(1))
    case 'DEDUCTION':
      return parseFloat((sev * 100).toFixed(0))
    case 'LEVEL':
      return parseFloat(sev.toFixed(2))
    default:
      return sev
  }
})

const unitLabel = computed(() => {
  switch (props.mode) {
    case 'RATING_SCALE':  return '星'
    case 'DIRECT_SCORE':  return '分'
    case 'DEDUCTION':     return '%'
    case 'LEVEL':         return '(0~1 等级 sev)'
    default: return ''
  }
})

const maxInput = computed(() => {
  switch (props.mode) {
    case 'RATING_SCALE':
    case 'DIRECT_SCORE':  return props.max
    case 'DEDUCTION':     return 100
    case 'LEVEL':         return 1
    default: return 1
  }
})

const step = computed(() => (props.mode === 'LEVEL' ? 0.05 : props.mode === 'DEDUCTION' ? 5 : 0.5))
const precision = computed(() => (props.mode === 'LEVEL' ? 2 : props.mode === 'DEDUCTION' ? 0 : 1))

function onInput(v: number | null | undefined) {
  if (v == null || isNaN(v as number)) {
    emit('update:modelValue', null)
    return
  }
  let sev: number
  switch (props.mode) {
    case 'RATING_SCALE':
    case 'DIRECT_SCORE':
      sev = Math.max(0, Math.min(1, 1 - (v as number) / props.max))
      break
    case 'DEDUCTION':
      sev = Math.max(0, Math.min(1, (v as number) / 100))
      break
    case 'LEVEL':
    default:
      sev = Math.max(0, Math.min(1, v as number))
  }
  emit('update:modelValue', parseFloat(sev.toFixed(3)))
}
</script>

<style scoped>
.ct-wrap { display: inline-flex; align-items: center; gap: 6px; }
.ct-prefix { color: #5a6474; font-size: 12px; }
.ct-suffix { color: #5a6474; font-size: 12px; }
</style>
