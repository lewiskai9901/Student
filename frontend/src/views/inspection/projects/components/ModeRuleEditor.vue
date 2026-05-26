<template>
  <div class="mre-root">
    <!-- 简化设计: 只配阈值, 不再有三态 radio. 由父级 (题目 Switch) 控制开/关 -->

    <!-- 二元 PASS_FAIL -->
    <div v-if="scoringMode === 'PASS_FAIL'" class="mre-input-row">
      <span class="mre-prefix">不通过时建</span>
      <el-select v-model="severity" size="small" style="width:110px" @change="onChange">
        <el-option label="严重" value="HIGH" />
        <el-option label="中度" value="MEDIUM" />
        <el-option label="轻微" value="LOW" />
      </el-select>
      <span class="mre-suffix">单</span>
      <span class="mre-deadline">
        · 时限 <el-input-number v-model="deadlineDays" :min="1" :max="60" size="small" style="width:90px" @change="onChange" /> 天
      </span>
    </div>

    <!-- 离散模式 -->
    <div v-else-if="isDiscreteMode" class="mre-discrete-grid">
      <div class="mre-discrete-row" v-for="opt in displayOptions" :key="opt">
        <span class="mre-discrete-label">{{ opt }}</span>
        <span class="mre-arrow">→</span>
        <el-select v-model="optionMap[opt]" size="small" style="width:110px" @change="onChange">
          <el-option label="不触发" value="NONE" />
          <el-option label="轻微" value="LOW" />
          <el-option label="中度" value="MEDIUM" />
          <el-option label="严重" value="HIGH" />
        </el-select>
      </div>
      <div class="mre-deadline-block">
        时限 <el-input-number v-model="deadlineDays" :min="1" :max="60" size="small" style="width:90px" @change="onChange" /> 天
      </div>
    </div>

    <!-- 连续模式 -->
    <div v-else class="mre-input-row">
      <span class="mre-prefix">{{ continuousPrefix }}</span>
      <el-input-number v-model="triggerValue"
        :min="0" :max="maxScore || 100"
        :step="continuousStep" :precision="continuousPrecision"
        size="small" style="width:130px" @change="onChange" />
      <span class="mre-suffix">{{ continuousUnit }} 时建</span>
      <el-select v-model="severity" size="small" style="width:110px" @change="onChange">
        <el-option label="严重" value="HIGH" />
        <el-option label="中度" value="MEDIUM" />
        <el-option label="轻微" value="LOW" />
      </el-select>
      <span class="mre-suffix">单</span>
      <span class="mre-deadline">
        · 时限 <el-input-number v-model="deadlineDays" :min="1" :max="60" size="small" style="width:90px" @change="onChange" /> 天
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'

interface Props {
  scoringMode: string
  maxScore?: number
  discreteOptions?: string[]
  riskLevels?: string[]
  ruleJson?: string
  enabledMode?: boolean
}
const props = defineProps<Props>()
const emit = defineEmits<{ (e: 'change', json: string): void }>()

const severity = ref<'HIGH' | 'MEDIUM' | 'LOW'>('HIGH')
const triggerValue = ref<number | null>(null)
const optionMap = ref<Record<string, string>>({})
const deadlineDays = ref<number | null>(3)

const isDiscreteMode = computed(() => {
  const m = props.scoringMode
  return ['LEVEL', 'SCORE_TABLE', 'TIERED_DEDUCTION', 'THRESHOLD', 'RISK_MATRIX'].includes(m)
})

const displayOptions = computed(() => {
  if (props.scoringMode === 'RISK_MATRIX') return props.riskLevels ?? ['L', 'M', 'H', 'VH']
  return props.discreteOptions ?? []
})

const continuousPrefix = computed(() => {
  switch (props.scoringMode) {
    case 'DEDUCTION': return '扣 ≥'
    case 'CUMULATIVE': return '≥'
    default: return '≤'
  }
})
const continuousUnit = computed(() => {
  switch (props.scoringMode) {
    case 'RATING_SCALE': return '星'
    case 'DIRECT': case 'ADDITION': return '分'
    case 'DEDUCTION': return '分扣分'
    case 'CUMULATIVE': return '次'
    case 'WEIGHTED_MULTI': return '分'
    case 'FORMULA': return ''
    default: return ''
  }
})
const continuousStep = computed(() => (
  ['RATING_SCALE', 'DIRECT', 'ADDITION'].includes(props.scoringMode) ? 0.5 : 1
))
const continuousPrecision = computed(() => (
  ['RATING_SCALE', 'DIRECT', 'ADDITION'].includes(props.scoringMode) ? 1 : 0
))

function computeSev(v: number | null): number | null {
  if (v == null) return null
  const max = props.maxScore ?? 10
  if (max <= 0) return null
  switch (props.scoringMode) {
    case 'RATING_SCALE': case 'DIRECT': case 'ADDITION':
    case 'WEIGHTED_MULTI': case 'FORMULA':
      return Math.max(0, Math.min(1, 1 - v / max))
    case 'DEDUCTION': case 'CUMULATIVE':
      return Math.max(0, Math.min(1, v / max))
    default: return null
  }
}

/** 解析已有 ruleJson 还原 UI 状态 */
function loadFromJson(json?: string) {
  if (!json || json.trim() === '') return
  let obj: any
  try { obj = JSON.parse(json) } catch { return }
  if (!obj || obj._pending || obj._empty) return

  // PASS_FAIL
  if (props.scoringMode === 'PASS_FAIL' && obj.baseSeverityMap?.FAIL) {
    severity.value = obj.baseSeverityMap.FAIL
  }
  // 离散
  else if (isDiscreteMode.value && obj.baseSeverityMap) {
    const m: Record<string, string> = {}
    for (const k of displayOptions.value) m[k] = obj.baseSeverityMap[k] || 'NONE'
    optionMap.value = m
  }
  // 连续
  else if (obj.singleThreshold?.sevThreshold != null) {
    severity.value = obj.singleThreshold.triggerSeverity || 'HIGH'
    const max = props.maxScore ?? 10
    const sev = obj.singleThreshold.sevThreshold
    if (['DEDUCTION', 'CUMULATIVE'].includes(props.scoringMode)) {
      triggerValue.value = parseFloat((sev * max).toFixed(1))
    } else {
      triggerValue.value = parseFloat((max * (1 - sev)).toFixed(1))
    }
  }

  if (obj.deadlineOverrideDays != null) deadlineDays.value = obj.deadlineOverrideDays
}

watch(() => props.ruleJson, (j) => loadFromJson(j), { immediate: true })

// 初始化 optionMap
watch(displayOptions, () => {
  const m: Record<string, string> = {}
  for (const k of displayOptions.value) m[k] = optionMap.value[k] || 'NONE'
  optionMap.value = m
}, { immediate: true })

function buildPayload(): Record<string, unknown> {
  const out: Record<string, unknown> = {}
  if (props.scoringMode === 'PASS_FAIL') {
    out.baseSeverityMap = { FAIL: severity.value }
  } else if (isDiscreteMode.value) {
    const map: Record<string, string> = {}
    for (const [k, v] of Object.entries(optionMap.value)) {
      if (v && v !== 'NONE') map[k] = v
    }
    if (Object.keys(map).length) out.baseSeverityMap = map
  } else {
    const sev = computeSev(triggerValue.value)
    if (sev != null) out.singleThreshold = {
      sevThreshold: parseFloat(sev.toFixed(3)),
      triggerSeverity: severity.value,
    }
  }
  if (deadlineDays.value != null) out.deadlineOverrideDays = deadlineDays.value
  return out
}

function onChange() {
  // debounce 简单实现: 每次变更立即触发 (后端可吸收)
  emit('change', JSON.stringify(buildPayload()))
}
</script>

<style scoped>
.mre-root { display: flex; flex-direction: column; gap: 8px; }
.mre-input-row { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.mre-prefix, .mre-suffix { color: #5a6474; font-size: 12px; }
.mre-deadline { font-size: 12px; color: #6b7280; }
.mre-discrete-grid { display: flex; flex-direction: column; gap: 5px; }
.mre-discrete-row { display: flex; align-items: center; gap: 8px; }
.mre-discrete-label { min-width: 60px; font-size: 13px; }
.mre-arrow { color: #0369a1; font-weight: 600; }
.mre-deadline-block { margin-top: 6px; padding-top: 6px; border-top: 1px dashed #e5e7eb; font-size: 12px; color: #6b7280; }
</style>
