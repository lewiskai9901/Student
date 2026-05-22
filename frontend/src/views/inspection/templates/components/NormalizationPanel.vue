<script setup lang="ts">
import { computed, ref } from 'vue'
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

function updateField<K extends keyof NormalizationConfig>(field: K, value: NormalizationConfig[K]) {
  emit('update:modelValue', { ...props.modelValue, [field]: value })
}

// ==================== Example Calculation ====================

// 计算示例的初始演示值 — 用户可在面板中实时编辑, 仅作交互预览用途
const exampleRawScore = ref(-10)
const examplePopulation = ref(30)

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
  <div class="np-root">
    <h3 class="np-title">归一化配置</h3>

    <!-- Mode selector -->
    <div>
      <label class="np-label">归一化模式</label>
      <div class="np-mode-list">
        <label
          v-for="mode in MODES"
          :key="mode.value"
          class="np-mode"
          :class="{ 'np-mode--active': config.mode === mode.value }"
        >
          <input
            type="radio"
            name="normalization-mode"
            :value="mode.value"
            :checked="config.mode === mode.value"
            class="np-radio"
            @change="updateField('mode', mode.value)"
          />
          <div class="np-mode-body">
            <div class="np-mode-label">{{ mode.label }}</div>
            <div class="np-mode-desc">{{ mode.description }}</div>
            <code class="np-mode-formula">{{ mode.formula }}</code>
          </div>
        </label>
      </div>
    </div>

    <!-- Population source (for PER_CAPITA, SQRT_ADJUSTED) -->
    <div v-if="showPopulationSource">
      <label class="np-label">人口数据来源</label>
      <select
        :value="config.populationSource"
        class="np-select"
        @change="updateField('populationSource', ($event.target as HTMLSelectElement).value)"
      >
        <option v-for="src in POPULATION_SOURCES" :key="src.value" :value="src.value">
          {{ src.label }}
        </option>
      </select>
    </div>

    <!-- Divisor (for RATE_BASED, CUSTOM) -->
    <div v-if="showDivisor">
      <label class="np-label">
        {{ config.mode === 'RATE_BASED' ? '基准值' : '自定义除数' }}
      </label>
      <input
        type="number"
        :value="config.divisor"
        class="np-input np-input--narrow"
        :min="1"
        placeholder="1"
        @input="updateField('divisor', Number(($event.target as HTMLInputElement).value) || null)"
      />
      <p class="np-hint">
        {{ config.mode === 'RATE_BASED' ? '原始分 / 基准值 * 100' : '原始分 / 自定义除数' }}
      </p>
    </div>

    <!-- Floor / Cap bounds -->
    <div v-if="showBounds" class="np-grid-2">
      <div>
        <label class="np-label">下限 (Floor)</label>
        <input
          type="number"
          :value="config.floorAt"
          class="np-input"
          placeholder="不限"
          @input="updateField('floorAt', ($event.target as HTMLInputElement).value ? Number(($event.target as HTMLInputElement).value) : null)"
        />
        <p class="np-hint">归一化后不低于此值</p>
      </div>
      <div>
        <label class="np-label">上限 (Cap)</label>
        <input
          type="number"
          :value="config.cappedAt"
          class="np-input"
          placeholder="不限"
          @input="updateField('cappedAt', ($event.target as HTMLInputElement).value ? Number(($event.target as HTMLInputElement).value) : null)"
        />
        <p class="np-hint">归一化后不超过此值</p>
      </div>
    </div>

    <!-- Example calculation -->
    <div class="np-example">
      <div class="np-example-head">
        <Calculator :size="14" />
        <span>计算示例</span>
      </div>

      <div class="np-grid-2">
        <div>
          <label class="np-example-label">原始分</label>
          <input v-model.number="exampleRawScore" type="number" class="np-example-input" />
        </div>
        <div v-if="showPopulationSource">
          <label class="np-example-label">人数</label>
          <input v-model.number="examplePopulation" type="number" :min="1" class="np-example-input" />
        </div>
      </div>

      <div class="np-example-steps">
        <div v-for="(step, idx) in exampleSteps" :key="idx" class="np-example-step">
          {{ step }}
        </div>
      </div>

      <div class="np-example-result">
        = {{ exampleResult }}
      </div>
    </div>
  </div>
</template>

<style scoped>
.np-root { display: flex; flex-direction: column; gap: var(--insp-sp-4); }
.np-title {
  margin: 0;
  font-size: var(--insp-text-sm);
  font-weight: var(--insp-fw-medium);
  color: var(--insp-ink-secondary);
}
.np-label {
  display: block;
  margin-bottom: var(--insp-sp-1);
  font-size: var(--insp-text-xs);
  color: var(--insp-ink-tertiary);
}
.np-hint {
  margin: 2px 0 0;
  font-size: 10px;
  color: var(--insp-ink-quaternary);
}

/* Mode selector */
.np-mode-list { display: flex; flex-direction: column; gap: 6px; }
.np-mode {
  display: flex;
  align-items: flex-start;
  gap: var(--insp-sp-2);
  padding: 10px;
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-md);
  cursor: pointer;
  transition: border-color var(--insp-t-fast), background var(--insp-t-fast);
}
.np-mode:hover { border-color: var(--insp-border-strong); }
.np-mode--active {
  border-color: var(--insp-accent);
  background: var(--insp-accent-paler);
}
.np-radio { margin-top: 2px; accent-color: var(--insp-accent); }
.np-mode-body { flex: 1; min-width: 0; }
.np-mode-label {
  font-size: var(--insp-text-sm);
  font-weight: var(--insp-fw-medium);
  color: var(--insp-ink-secondary);
}
.np-mode-desc { font-size: var(--insp-text-xs); color: var(--insp-ink-quaternary); }
.np-mode-formula {
  display: block;
  margin-top: 2px;
  font-size: 10px;
  font-family: var(--insp-font-mono);
  color: var(--insp-ink-tertiary);
}

/* Inputs */
.np-input, .np-select {
  width: 100%;
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-md);
  padding: 6px 12px;
  font-size: var(--insp-text-sm);
  color: var(--insp-ink-primary);
  background: var(--insp-bg-surface);
  outline: none;
  transition: border-color var(--insp-t-fast);
}
.np-input:focus, .np-select:focus { border-color: var(--insp-accent); }
.np-input--narrow { width: 160px; }
.np-grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: var(--insp-sp-3); }

/* Example box */
.np-example {
  display: flex;
  flex-direction: column;
  gap: var(--insp-sp-2);
  padding: 12px;
  border: 1px solid var(--insp-info-border);
  background: var(--insp-info-pale);
  border-radius: var(--insp-radius-md);
}
.np-example-head {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--insp-text-xs);
  font-weight: var(--insp-fw-medium);
  color: var(--insp-info);
}
.np-example-label {
  display: block;
  margin-bottom: 2px;
  font-size: 10px;
  color: var(--insp-info);
}
.np-example-input {
  width: 100%;
  border: 1px solid var(--insp-info-border);
  border-radius: var(--insp-radius-sm);
  padding: 4px 8px;
  font-size: var(--insp-text-xs);
  outline: none;
  background: var(--insp-bg-surface);
  color: var(--insp-ink-primary);
}
.np-example-input:focus { border-color: var(--insp-accent); }
.np-example-steps { display: flex; flex-direction: column; gap: 2px; }
.np-example-step {
  font-size: var(--insp-text-xs);
  font-family: var(--insp-font-mono);
  color: var(--insp-info);
}
.np-example-result {
  text-align: right;
  font-size: var(--insp-text-sm);
  font-weight: var(--insp-fw-bold);
  color: var(--insp-info);
}
</style>
