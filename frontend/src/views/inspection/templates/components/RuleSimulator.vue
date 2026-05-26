<template>
  <div class="sim-root">
    <div class="sim-head">
      <span class="sim-title">🔬 模拟器</span>
      <span class="sim-subtitle">不发布项目, 即时验证规则</span>
    </div>

    <div class="sim-body">
      <!-- 输入区: 按 scoringMode 渲染 -->
      <div class="sim-input-row">
        <label class="sim-label">假设检查员填:</label>
        <!-- 二元 -->
        <el-select v-if="scoringMode === 'PASS_FAIL'" v-model="testValue" size="small" style="width: 160px">
          <el-option label="不通过 (FAIL)" value="FAIL" />
          <el-option label="通过 (PASS)" value="PASS" />
        </el-select>
        <!-- 离散标签 -->
        <el-select v-else-if="isDiscrete" v-model="testValue" size="small" style="width: 180px">
          <el-option v-for="opt in discreteList" :key="opt" :label="opt" :value="opt" />
        </el-select>
        <!-- 风险矩阵 -->
        <el-select v-else-if="scoringMode === 'RISK_MATRIX'" v-model="testValue" size="small" style="width: 200px">
          <el-option v-for="lv in riskLevels" :key="lv" :label="`${lv} ${riskLabel(lv)}`" :value="lv" />
        </el-select>
        <!-- 连续数值 -->
        <template v-else>
          <el-input-number v-model="testNumber"
                           :min="0" :max="maxScore || 100" :step="thresholdStep"
                           :precision="thresholdPrecision"
                           size="small" style="width: 140px" />
          <span class="sim-unit">{{ unit }}</span>
        </template>

        <el-button size="small" @click="runSim" :loading="simulating">模拟判定</el-button>
      </div>

      <!-- 输出区: verdict + trace -->
      <div v-if="result" class="sim-output">
        <div class="sim-verdict-row">
          <span class="sim-verdict-label">引擎判定:</span>
          <span :class="['sim-verdict-badge', `sim-badge--${(result.severity || 'NONE').toLowerCase()}`]">
            {{ severityLabel(result.severity) }}
          </span>
          <span v-if="result.severity !== 'NONE'" class="sim-must">
            ({{ result.mustCorrect ? '自动建单' : '候选确认' }} · {{ result.deadlineDays || '默认' }}天)
          </span>
        </div>
        <div v-if="result.reason" class="sim-reason">原因: {{ result.reason }}</div>
        <div v-if="result.trace?.length" class="sim-trace">
          <div class="sim-trace-head">判定链路:</div>
          <div v-for="(t, i) in (result.trace as any[])" :key="i" class="sim-trace-row">
            <span class="sim-trace-step">{{ (i as number) + 1 }}.</span>
            <span class="sim-trace-layer">[{{ t.layer }}]</span>
            <span class="sim-trace-rule">{{ t.rule }}</span>
            <span v-if="t.input && t.input !== '—'" class="sim-trace-arrow">·</span>
            <span v-if="t.input && t.input !== '—'" class="sim-trace-input">{{ t.input }}</span>
            <span class="sim-trace-arrow">→</span>
            <span class="sim-trace-output">{{ t.output }}</span>
          </div>
        </div>
      </div>

      <div v-if="error" class="sim-error">{{ error }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { LongId } from '@/types/common'
import { ref, computed, watch } from 'vue'
import { http } from '@/utils/request'

interface Props {
  /** 当前项目 ID (可选, 用于加载项目策略) */
  projectId?: LongId | null
  scoringMode: string
  maxScore?: number
  discreteOptions?: string[]
  riskLevels?: string[]
  scoringConfigJson?: string
  /** 当前编辑中的 itemRule (草稿态 JSON) */
  itemRuleJson: string
}
const props = defineProps<Props>()

const testValue = ref<string>('FAIL')
const testNumber = ref<number | null>(null)
const result = ref<any | null>(null)
const simulating = ref(false)
const error = ref('')

const SEV_LABEL: Record<string, string> = { HIGH: '严重', MEDIUM: '中度', LOW: '轻微', NONE: '不建单' }
function severityLabel(s: string) { return SEV_LABEL[s] ?? s }

const RISK_LABEL: Record<string, string> = { L: '低风险', M: '中风险', H: '高风险', VH: '极高风险', E: '极端' }
function riskLabel(lv: string) { return RISK_LABEL[lv.toUpperCase()] ?? '' }

const isDiscrete = computed(() => {
  return ['LEVEL', 'SCORE_TABLE', 'TIERED_DEDUCTION', 'THRESHOLD'].includes(props.scoringMode)
      && (props.discreteOptions?.length ?? 0) > 0
})
const discreteList = computed(() => props.discreteOptions ?? [])
const riskLevels = computed(() => props.riskLevels ?? [])

const unit = computed(() => {
  switch (props.scoringMode) {
    case 'RATING_SCALE': return '星'
    case 'DIRECT':       return '分'
    case 'DEDUCTION':    return '分扣分'
    case 'CUMULATIVE':   return '次'
    case 'ADDITION':     return '分加分'
    case 'WEIGHTED_MULTI': return '分'
    case 'FORMULA':      return ''
    default: return ''
  }
})
const thresholdStep = computed(() => (['RATING_SCALE', 'DIRECT', 'ADDITION'].includes(props.scoringMode) ? 0.5 : 1))
const thresholdPrecision = computed(() => (['RATING_SCALE', 'DIRECT', 'ADDITION'].includes(props.scoringMode) ? 1 : 0))

/** 把用户输入转成 simulate 接口的 (responseValue, score) */
function buildPayload() {
  const m = props.scoringMode
  if (m === 'PASS_FAIL') return { responseValue: testValue.value, score: null }
  if (isDiscrete.value) return { responseValue: testValue.value, score: null }
  if (m === 'RISK_MATRIX') {
    // 假设坐标: 用 level 名称作为 responseValue, normalizer 不直接消费这个,
    // 但 resolveLabel 会回退到 responseValue, 所以 baseSeverityMap 能命中
    return { responseValue: testValue.value, score: null, scoringConfigJson: props.scoringConfigJson }
  }
  // 连续数值
  const v = testNumber.value
  if (v == null) return null
  switch (m) {
    case 'RATING_SCALE':
    case 'DIRECT':
    case 'ADDITION':
    case 'WEIGHTED_MULTI':
    case 'FORMULA':
      // 得分模式: score = v
      return { responseValue: String(v), score: v }
    case 'DEDUCTION':
    case 'CUMULATIVE':
      // 扣分/计次模式: score = -v (扣分为负数, 引擎用 abs)
      return { responseValue: String(v), score: -v, itemWeight: props.maxScore }
    default: return { responseValue: String(v), score: v }
  }
}

async function runSim() {
  error.value = ''
  result.value = null
  const p = buildPayload()
  if (!p) { error.value = '请填入测试值'; return }
  simulating.value = true
  try {
    const body: any = {
      projectId: props.projectId ?? null,
      scoringMode: props.scoringMode,
      responseValue: p.responseValue,
      score: p.score,
      itemWeight: (p as any).itemWeight,
      scoringConfigJson: (p as any).scoringConfigJson || props.scoringConfigJson,
      itemRuleJson: props.itemRuleJson,
      itemCode: 'SIM',
      itemName: '模拟',
    }
    const resp = await http.post<any>('/inspection/corrective/simulate', body)
    result.value = resp
  } catch (e: any) {
    error.value = '模拟失败: ' + (e?.message || '未知错误')
  } finally {
    simulating.value = false
  }
}

// 初始化默认输入
watch(() => props.scoringMode, () => {
  result.value = null
  const m = props.scoringMode
  if (m === 'PASS_FAIL') { testValue.value = 'FAIL' }
  else if (isDiscrete.value && discreteList.value.length) { testValue.value = discreteList.value[discreteList.value.length - 1] }
  else if (m === 'RISK_MATRIX' && riskLevels.value.length) { testValue.value = 'H' }
  else { testNumber.value = props.maxScore ? Math.floor((props.maxScore as number) / 2) : 5 }
}, { immediate: true })
</script>

<style scoped>
.sim-root {
  margin-top: 12px;
  padding: 10px 14px;
  background: #f0fdf4;
  border: 1px solid #86efac;
  border-radius: 6px;
}
.sim-head { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.sim-title { font-size: 13px; font-weight: 600; color: #166534; }
.sim-subtitle { font-size: 11px; color: #15803d; }
.sim-body { display: flex; flex-direction: column; gap: 8px; }
.sim-input-row { display: flex; align-items: center; gap: 8px; }
.sim-label { font-size: 12px; color: #374151; min-width: 80px; }
.sim-unit { font-size: 12px; color: #6b7280; }
.sim-output {
  padding: 8px 10px;
  background: #fff;
  border: 1px solid #d1fae5;
  border-radius: 4px;
}
.sim-verdict-row { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.sim-verdict-label { color: #6b7280; }
.sim-verdict-badge {
  padding: 2px 10px;
  border-radius: 3px;
  font-size: 12px;
  font-weight: 600;
}
.sim-badge--high { color: #dc2626; background: #fef2f2; }
.sim-badge--medium { color: #d97706; background: #fef3c7; }
.sim-badge--low { color: #2563eb; background: #eff6ff; }
.sim-badge--none { color: #6b7280; background: #f3f4f6; }
.sim-must { font-size: 11px; color: #9ca3af; }
.sim-reason { margin-top: 4px; font-size: 11px; color: #4b5563; }
.sim-trace { margin-top: 8px; padding-top: 8px; border-top: 1px dashed #d1fae5; }
.sim-trace-head { font-size: 11px; color: #6b7280; margin-bottom: 4px; font-weight: 500; }
.sim-trace-row { display: flex; align-items: center; gap: 6px; font-size: 11px; padding: 2px 0; }
.sim-trace-step { color: #9ca3af; min-width: 16px; }
.sim-trace-layer { color: #16a34a; font-family: monospace; font-size: 10px; min-width: 80px; }
.sim-trace-rule { color: #374151; }
.sim-trace-input { color: #6b7280; font-family: monospace; font-size: 10px; }
.sim-trace-arrow { color: #9ca3af; }
.sim-trace-output { color: #15803d; font-weight: 500; }
.sim-error { font-size: 12px; color: #dc2626; padding: 6px; background: #fef2f2; border-radius: 4px; }
</style>
