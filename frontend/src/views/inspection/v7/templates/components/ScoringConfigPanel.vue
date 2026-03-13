<script setup lang="ts">
import { computed } from 'vue'
import type { ScoringMode } from '@/types/insp/enums'
import { ScoringModeConfig } from '@/types/insp/enums'

// ==================== Types ====================

interface ScoringConfig {
  scoringMethod: ScoringMode
  maxScore: number
  weight: number
  deductionPerViolation: number
  normalization: NormalizationConfig
}

interface NormalizationConfig {
  mode: NormalizationMode
  populationSource: string
  divisor: number | null
  floorAt: number | null
  cappedAt: number | null
}

type NormalizationMode = 'NONE' | 'PER_CAPITA' | 'RATE_BASED' | 'SQRT_ADJUSTED' | 'CUSTOM'

// ==================== Props & Emits ====================

const props = defineProps<{
  modelValue: ScoringConfig
}>()

const emit = defineEmits<{
  'update:modelValue': [value: ScoringConfig]
}>()

// ==================== Computed ====================

const config = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

// ==================== Helpers ====================

const inputCls = 'w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm outline-none focus:border-blue-400'
const selectCls = 'w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm outline-none focus:border-blue-400 bg-white'

function update<K extends keyof ScoringConfig>(field: K, value: ScoringConfig[K]) {
  emit('update:modelValue', { ...props.modelValue, [field]: value })
}

function updateNormalization<K extends keyof NormalizationConfig>(field: K, value: NormalizationConfig[K]) {
  const norm = { ...props.modelValue.normalization, [field]: value }
  emit('update:modelValue', { ...props.modelValue, normalization: norm })
}

const NORMALIZATION_MODES: { value: NormalizationMode; label: string; description: string }[] = [
  { value: 'NONE', label: '不归一化', description: '使用原始分数' },
  { value: 'PER_CAPITA', label: '人均', description: '总分 / 人数' },
  { value: 'RATE_BASED', label: '比率', description: '总分 / 基准值 * 100' },
  { value: 'SQRT_ADJUSTED', label: '平方根调整', description: '总分 / sqrt(人数)' },
  { value: 'CUSTOM', label: '自定义', description: '自定义除数' },
]

function getNormLabel(mode: NormalizationMode): string {
  return NORMALIZATION_MODES.find(m => m.value === mode)?.label || mode
}

const scoringMethodEntries = computed(() =>
  Object.entries(ScoringModeConfig) as [ScoringMode, { label: string; description: string }][]
)
</script>

<template>
  <div class="space-y-4">
    <h3 class="text-sm font-medium text-gray-700">评分配置</h3>

    <!-- Scoring method -->
    <div>
      <label class="mb-1 block text-xs text-gray-500">评分模式</label>
      <select :value="config.scoringMethod" :class="selectCls" @change="update('scoringMethod', ($event.target as HTMLSelectElement).value as ScoringMode)">
        <option v-for="[key, cfg] in scoringMethodEntries" :key="key" :value="key">
          {{ cfg.label }} ({{ key }})
        </option>
      </select>
      <p class="mt-1 text-xs text-gray-400">
        {{ ScoringModeConfig[config.scoringMethod]?.description }}
      </p>
    </div>

    <!-- Max score + weight row -->
    <div class="grid grid-cols-2 gap-3">
      <div>
        <label class="mb-1 block text-xs text-gray-500">满分</label>
        <input
          type="number"
          :value="config.maxScore"
          :class="inputCls"
          :min="1"
          @input="update('maxScore', Number(($event.target as HTMLInputElement).value))"
        />
      </div>
      <div>
        <label class="mb-1 block text-xs text-gray-500">每次违规扣分</label>
        <input
          type="number"
          :value="config.deductionPerViolation"
          :class="inputCls"
          :max="0"
          @input="update('deductionPerViolation', Number(($event.target as HTMLInputElement).value))"
        />
        <p class="mt-0.5 text-[10px] text-gray-400">一般为负数，如 -2</p>
      </div>
    </div>

    <!-- Weight slider -->
    <div>
      <div class="flex items-center justify-between mb-1">
        <label class="text-xs text-gray-500">权重</label>
        <span class="text-xs font-medium text-blue-600">{{ config.weight }}%</span>
      </div>
      <input
        type="range"
        :value="config.weight"
        class="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer accent-blue-500"
        :min="0"
        :max="100"
        :step="1"
        @input="update('weight', Number(($event.target as HTMLInputElement).value))"
      />
      <div class="flex justify-between text-[10px] text-gray-400 mt-0.5">
        <span>0%</span>
        <span>50%</span>
        <span>100%</span>
      </div>
    </div>

    <!-- Normalization sub-section -->
    <div class="rounded-lg border border-gray-200 bg-gray-50/50 p-3 space-y-3">
      <div class="flex items-center justify-between">
        <span class="text-xs font-medium text-gray-600">归一化模式</span>
        <span class="text-[10px] text-gray-400">{{ getNormLabel(config.normalization.mode) }}</span>
      </div>

      <div>
        <select
          :value="config.normalization.mode"
          :class="selectCls"
          @change="updateNormalization('mode', ($event.target as HTMLSelectElement).value as NormalizationMode)"
        >
          <option v-for="nm in NORMALIZATION_MODES" :key="nm.value" :value="nm.value">
            {{ nm.label }} - {{ nm.description }}
          </option>
        </select>
      </div>

      <!-- PER_CAPITA / RATE_BASED / SQRT_ADJUSTED fields -->
      <template v-if="config.normalization.mode !== 'NONE'">
        <div>
          <label class="mb-0.5 block text-xs text-gray-500">人口数据来源</label>
          <select
            :value="config.normalization.populationSource"
            :class="selectCls"
            @change="updateNormalization('populationSource', ($event.target as HTMLSelectElement).value)"
          >
            <option value="org_member_count">组织人数</option>
            <option value="place_capacity">场所容量</option>
            <option value="manual">手动输入</option>
          </select>
        </div>
      </template>

      <template v-if="config.normalization.mode === 'CUSTOM'">
        <div>
          <label class="mb-0.5 block text-xs text-gray-500">自定义除数</label>
          <input
            type="number"
            :value="config.normalization.divisor"
            :class="inputCls"
            class="!w-32"
            :min="1"
            placeholder="1"
            @input="updateNormalization('divisor', Number(($event.target as HTMLInputElement).value) || null)"
          />
        </div>
      </template>

      <template v-if="config.normalization.mode !== 'NONE'">
        <div class="grid grid-cols-2 gap-2">
          <div>
            <label class="mb-0.5 block text-xs text-gray-500">下限 (floor)</label>
            <input
              type="number"
              :value="config.normalization.floorAt"
              :class="inputCls"
              placeholder="不限"
              @input="updateNormalization('floorAt', ($event.target as HTMLInputElement).value ? Number(($event.target as HTMLInputElement).value) : null)"
            />
          </div>
          <div>
            <label class="mb-0.5 block text-xs text-gray-500">上限 (cap)</label>
            <input
              type="number"
              :value="config.normalization.cappedAt"
              :class="inputCls"
              placeholder="不限"
              @input="updateNormalization('cappedAt', ($event.target as HTMLInputElement).value ? Number(($event.target as HTMLInputElement).value) : null)"
            />
          </div>
        </div>
      </template>
    </div>

    <!-- Summary -->
    <div class="rounded border border-dashed border-gray-300 bg-gray-50 p-2">
      <p class="text-xs text-gray-400">配置摘要:</p>
      <p class="mt-1 text-xs text-gray-600">
        {{ ScoringModeConfig[config.scoringMethod]?.label }}，
        满分 {{ config.maxScore }}，
        权重 {{ config.weight }}%，
        归一化: {{ getNormLabel(config.normalization.mode) }}
      </p>
    </div>
  </div>
</template>
