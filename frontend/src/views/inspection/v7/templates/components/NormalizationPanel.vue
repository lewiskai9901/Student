<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Calculator } from 'lucide-vue-next'

// ==================== Types ====================

type NormalizationMode = 'NONE' | 'PER_CAPITA' | 'RATE_BASED' | 'SQRT_ADJUSTED' | 'CUSTOM'

interface NormalizationConfig {
  mode: NormalizationMode
  populationSource: string
  divisor: number | null
  floorAt: number | null
  cappedAt: number | null
}

// ==================== Props & Emits ====================

const props = defineProps<{
  modelValue: NormalizationConfig
}>()

const emit = defineEmits<{
  'update:modelValue': [value: NormalizationConfig]
}>()

// ==================== Constants ====================

const MODES: { value: NormalizationMode; label: string; description: string; formula: string }[] = [
  { value: 'NONE', label: '不归一化', description: '直接使用原始分数，不做任何转换', formula: 'result = rawScore' },
  { value: 'PER_CAPITA', label: '人均归一化', description: '将总扣分除以人数，消除规模差异', formula: 'result = rawScore / population' },
  { value: 'RATE_BASED', label: '比率归一化', description: '将分数转换为百分比或比率', formula: 'result = rawScore / divisor * 100' },
  { value: 'SQRT_ADJUSTED', label: '平方根调整', description: '用平方根缓和人数影响，适合中等规模差异', formula: 'result = rawScore / sqrt(population)' },
  { value: 'CUSTOM', label: '自定义除数', description: '使用自定义固定除数进行归一化', formula: 'result = rawScore / customDivisor' },
]

const POPULATION_SOURCES: { value: string; label: string }[] = [
  { value: 'org_member_count', label: '组织成员人数' },
  { value: 'place_capacity', label: '场所容量' },
  { value: 'manual', label: '手动输入' },
]

// ==================== Computed ====================

const config = computed(() => props.modelValue)

const currentModeInfo = computed(() =>
  MODES.find(m => m.value === config.value.mode) || MODES[0]
)

const showPopulationSource = computed(() =>
  ['PER_CAPITA', 'SQRT_ADJUSTED'].includes(config.value.mode)
)

const showDivisor = computed(() =>
  ['RATE_BASED', 'CUSTOM'].includes(config.value.mode)
)

const showBounds = computed(() =>
  config.value.mode !== 'NONE'
)

// ==================== Helpers ====================

const inputCls = 'w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm outline-none focus:border-blue-400'
const selectCls = 'w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm outline-none focus:border-blue-400 bg-white'

function updateField<K extends keyof NormalizationConfig>(field: K, value: NormalizationConfig[K]) {
  emit('update:modelValue', { ...props.modelValue, [field]: value })
}

// ==================== Example Calculation ====================

const exampleRawScore = ref(-15)
const examplePopulation = ref(40)

const exampleResult = computed(() => {
  const raw = exampleRawScore.value
  const pop = examplePopulation.value || 1
  const div = config.value.divisor || 1
  const floor = config.value.floorAt
  const cap = config.value.cappedAt

  let result: number
  switch (config.value.mode) {
    case 'NONE':
      result = raw
      break
    case 'PER_CAPITA':
      result = raw / pop
      break
    case 'RATE_BASED':
      result = (raw / div) * 100
      break
    case 'SQRT_ADJUSTED':
      result = raw / Math.sqrt(pop)
      break
    case 'CUSTOM':
      result = raw / div
      break
    default:
      result = raw
  }

  if (floor != null && result < floor) result = floor
  if (cap != null && result > cap) result = cap

  return Math.round(result * 100) / 100
})

const exampleSteps = computed(() => {
  const raw = exampleRawScore.value
  const pop = examplePopulation.value || 1
  const div = config.value.divisor || 1
  const steps: string[] = []

  steps.push(`原始分: ${raw}`)

  switch (config.value.mode) {
    case 'NONE':
      steps.push(`不归一化: ${raw}`)
      break
    case 'PER_CAPITA':
      steps.push(`${raw} / ${pop} (人数) = ${Math.round((raw / pop) * 100) / 100}`)
      break
    case 'RATE_BASED':
      steps.push(`${raw} / ${div} * 100 = ${Math.round((raw / div * 100) * 100) / 100}`)
      break
    case 'SQRT_ADJUSTED':
      steps.push(`${raw} / sqrt(${pop}) = ${raw} / ${Math.round(Math.sqrt(pop) * 100) / 100} = ${Math.round((raw / Math.sqrt(pop)) * 100) / 100}`)
      break
    case 'CUSTOM':
      steps.push(`${raw} / ${div} = ${Math.round((raw / div) * 100) / 100}`)
      break
  }

  if (config.value.floorAt != null) steps.push(`下限: ${config.value.floorAt}`)
  if (config.value.cappedAt != null) steps.push(`上限: ${config.value.cappedAt}`)
  steps.push(`最终: ${exampleResult.value}`)

  return steps
})
</script>

<template>
  <div class="space-y-4">
    <h3 class="text-sm font-medium text-gray-700">归一化配置</h3>

    <!-- Mode selector -->
    <div>
      <label class="mb-1 block text-xs text-gray-500">归一化模式</label>
      <div class="space-y-1.5">
        <label
          v-for="mode in MODES"
          :key="mode.value"
          class="flex items-start gap-2 rounded-lg border p-2.5 cursor-pointer transition"
          :class="config.mode === mode.value
            ? 'border-blue-400 bg-blue-50/50'
            : 'border-gray-200 hover:border-gray-300'"
        >
          <input
            type="radio"
            name="normalization-mode"
            :value="mode.value"
            :checked="config.mode === mode.value"
            class="mt-0.5 accent-blue-500"
            @change="updateField('mode', mode.value)"
          />
          <div class="flex-1 min-w-0">
            <div class="text-sm font-medium text-gray-700">{{ mode.label }}</div>
            <div class="text-xs text-gray-400">{{ mode.description }}</div>
            <code class="text-[10px] text-gray-500 font-mono mt-0.5 block">{{ mode.formula }}</code>
          </div>
        </label>
      </div>
    </div>

    <!-- Population source (for PER_CAPITA, SQRT_ADJUSTED) -->
    <div v-if="showPopulationSource">
      <label class="mb-1 block text-xs text-gray-500">人口数据来源</label>
      <select
        :value="config.populationSource"
        :class="selectCls"
        @change="updateField('populationSource', ($event.target as HTMLSelectElement).value)"
      >
        <option v-for="src in POPULATION_SOURCES" :key="src.value" :value="src.value">
          {{ src.label }}
        </option>
      </select>
    </div>

    <!-- Divisor (for RATE_BASED, CUSTOM) -->
    <div v-if="showDivisor">
      <label class="mb-1 block text-xs text-gray-500">
        {{ config.mode === 'RATE_BASED' ? '基准值' : '自定义除数' }}
      </label>
      <input
        type="number"
        :value="config.divisor"
        :class="inputCls"
        class="!w-40"
        :min="1"
        placeholder="1"
        @input="updateField('divisor', Number(($event.target as HTMLInputElement).value) || null)"
      />
      <p class="mt-0.5 text-[10px] text-gray-400">
        {{ config.mode === 'RATE_BASED' ? '原始分 / 基准值 * 100' : '原始分 / 自定义除数' }}
      </p>
    </div>

    <!-- Floor / Cap bounds -->
    <div v-if="showBounds" class="grid grid-cols-2 gap-3">
      <div>
        <label class="mb-1 block text-xs text-gray-500">下限 (Floor)</label>
        <input
          type="number"
          :value="config.floorAt"
          :class="inputCls"
          placeholder="不限"
          @input="updateField('floorAt', ($event.target as HTMLInputElement).value ? Number(($event.target as HTMLInputElement).value) : null)"
        />
        <p class="mt-0.5 text-[10px] text-gray-400">归一化后不低于此值</p>
      </div>
      <div>
        <label class="mb-1 block text-xs text-gray-500">上限 (Cap)</label>
        <input
          type="number"
          :value="config.cappedAt"
          :class="inputCls"
          placeholder="不限"
          @input="updateField('cappedAt', ($event.target as HTMLInputElement).value ? Number(($event.target as HTMLInputElement).value) : null)"
        />
        <p class="mt-0.5 text-[10px] text-gray-400">归一化后不超过此值</p>
      </div>
    </div>

    <!-- Example calculation -->
    <div class="rounded-lg border border-blue-200 bg-blue-50/30 p-3 space-y-2">
      <div class="flex items-center gap-1.5">
        <Calculator :size="14" class="text-blue-500" />
        <span class="text-xs font-medium text-blue-700">计算示例</span>
      </div>

      <div class="grid grid-cols-2 gap-2">
        <div>
          <label class="mb-0.5 block text-[10px] text-blue-600">原始分</label>
          <input
            v-model.number="exampleRawScore"
            type="number"
            class="w-full rounded border border-blue-200 px-2 py-1 text-xs outline-none focus:border-blue-400"
          />
        </div>
        <div v-if="showPopulationSource">
          <label class="mb-0.5 block text-[10px] text-blue-600">人数</label>
          <input
            v-model.number="examplePopulation"
            type="number"
            :min="1"
            class="w-full rounded border border-blue-200 px-2 py-1 text-xs outline-none focus:border-blue-400"
          />
        </div>
      </div>

      <div class="space-y-0.5">
        <div v-for="(step, idx) in exampleSteps" :key="idx" class="text-xs text-blue-700 font-mono">
          {{ step }}
        </div>
      </div>

      <div class="text-right">
        <span class="text-sm font-bold text-blue-800">
          = {{ exampleResult }}
        </span>
      </div>
    </div>
  </div>
</template>
