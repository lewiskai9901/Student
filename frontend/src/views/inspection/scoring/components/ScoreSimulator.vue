<template>
  <div class="sim-root">
    <!-- Header -->
    <div class="sim-header">
      <div class="sim-header-left">
        <Calculator :size="14" class="sim-icon" />
        <span class="sim-title">评分模拟</span>
      </div>
      <button class="sim-btn-ghost" @click="resetAll" title="重置">
        <RotateCcw :size="12" />
      </button>
    </div>

    <!-- Loading -->
    <div v-if="loadingItems" class="sim-empty">加载检查项...</div>

    <!-- Load error (区分于"空数据") -->
    <div v-else-if="loadError" class="sim-error">
      <AlertTriangle :size="20" class="sim-error-icon" />
      <span class="sim-error-text">检查项加载失败</span>
      <button class="sim-error-retry" @click="loadTemplateItems">重试</button>
    </div>

    <!-- No scored items -->
    <div v-else-if="scoredItems.length === 0" class="sim-empty">
      无可评分的检查项
    </div>

    <!-- Simulator body -->
    <template v-else>
      <!-- Result banner (sticky at top of simulator) -->
      <div class="sim-result-banner">
        <div class="sim-result-main">
          <span class="sim-result-score">{{ displayTotal }}</span>
          <span class="sim-result-unit">分</span>
        </div>
        <div
          v-if="displayGrade"
          class="sim-result-grade"
          :style="{ backgroundColor: displayGradeColor }"
        >
          {{ displayGrade }}
        </div>
        <div v-if="!allDimensionsPassed" class="sim-result-fail">不及格</div>
      </div>

      <!-- Dimension groups -->
      <div class="sim-dims">
        <div
          v-for="group in dimensionGroups"
          :key="group.dimensionId ?? 'unassigned'"
          class="sim-dim-group"
        >
          <!-- Dimension header -->
          <div class="sim-dim-header">
            <div class="sim-dim-name">{{ group.dimensionName }}</div>
            <div class="sim-dim-meta">
              <span>权重 {{ group.weight }}%</span>
              <span class="sim-dim-sep">|</span>
              <span>基础分 {{ group.baseScore }}</span>
              <span class="sim-dim-sep">|</span>
              <span class="sim-dim-score" :class="group.passed ? '' : 'is-fail'">
                {{ group.score.toFixed(precision) }}
              </span>
            </div>
          </div>

          <!-- Items in this dimension -->
          <div class="sim-items">
            <div
              v-for="item in group.items"
              :key="item.id"
              class="sim-item"
            >
              <div class="sim-item-left">
                <span class="sim-item-mode" :class="'mode-' + item.mode.toLowerCase()">
                  {{ modeLabel(item.mode) }}
                </span>
                <span class="sim-item-name">{{ item.name }}</span>
                <span v-if="item.itemWeight !== 1" class="sim-item-weight">w{{ item.itemWeight }}</span>
              </div>
              <div class="sim-item-right">
                <input
                  v-model.number="item.mockQuantity"
                  type="number"
                  class="sim-input sim-input-qty"
                  min="0"
                  title="数量"
                  @input="recalculate"
                />
                <span class="sim-item-x">x</span>
                <span class="sim-item-config">{{ item.configScore }}</span>
                <span class="sim-item-eq">=</span>
                <span class="sim-item-result" :class="item.itemScore < 0 ? 'is-neg' : item.itemScore > 0 ? 'is-pos' : ''">
                  {{ item.itemScore >= 0 ? '+' : '' }}{{ item.itemScore.toFixed(precision) }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Calculation steps (collapsible) -->
      <details class="sim-steps-detail">
        <summary class="sim-steps-summary">计算步骤</summary>
        <div class="sim-steps">
          <div v-for="(step, idx) in calcSteps" :key="idx" class="sim-step">
            <span class="sim-step-num">{{ idx + 1 }}</span>
            <span class="sim-step-text">{{ step }}</span>
          </div>
        </div>
      </details>
    </template>
  </div>
</template>

<script setup lang="ts">
import type { LongId } from '@/types/common'
import { ref, computed, watch, onMounted } from 'vue'
import { Calculator, RotateCcw, AlertTriangle } from 'lucide-vue-next'
import { inspTemplateApi } from '@/api/inspection/template'
import type { TemplateItem } from '@/types/insp/template'
import type { ScoringProfile, ScoreDimension, CalculationRule } from '@/types/insp/scoring'

const props = defineProps<{
  profile: ScoringProfile
  dimensions: ScoreDimension[]
  rules: CalculationRule[]
  templateId: LongId
}>()

// ==================== State ====================

interface SimItem {
  id: LongId
  itemCode: string
  name: string
  mode: string       // DEDUCTION, ADDITION, FIXED, RESPONSE_MAPPED
  configScore: number
  dimensionId: LongId | null
  itemWeight: number  // 项目权重(分区内), 默认1.0
  mockQuantity: number
  itemScore: number
}

// 与 RuleConfigForm 写入的 config.conditions 结构对齐
interface SimCondition {
  itemCode: string
  operator: string
  value: string | number
  minCount?: number
}

interface DimGroup {
  dimensionId: LongId | null
  dimensionName: string
  weight: number
  baseScore: number
  score: number
  passed: boolean
  items: SimItem[]
}

const loadingItems = ref(true)
const loadError = ref(false)
const scoredItems = ref<SimItem[]>([])
const calcSteps = ref<string[]>([])

const displayTotal = ref('0.00')
const displayGrade = ref('')
const displayGradeColor = ref('#909399')
const allDimensionsPassed = ref(true)

const precision = computed(() => props.profile?.precisionDigits ?? 2)

// ==================== Load template items ====================

async function loadTemplateItems() {
  loadingItems.value = true
  loadError.value = false
  try {
    // Load sections for this template
    const sections = await inspTemplateApi.getSections(props.templateId)
    // 并发加载各分区检查项, 避免串行 N+1
    const itemLists = await Promise.all(
      sections.map(section => inspTemplateApi.getItems(section.id))
    )
    const allItems: TemplateItem[] = itemLists.flat()
    // Filter scored items and build SimItem list
    scoredItems.value = allItems
      .filter(it => it.isScored)
      .map(it => {
        const sc = parseScoringConfig(it.scoringConfig)
        return {
          id: it.id,
          itemCode: it.itemCode,
          name: it.itemName,
          mode: sc.mode,
          configScore: sc.configScore,
          dimensionId: it.dimensionId ?? null,
          itemWeight: it.itemWeight ?? 1.0,
          mockQuantity: 0,
          itemScore: 0,
        }
      })
    recalculate()
  } catch (e) {
    console.error('Failed to load template items for simulator', e)
    loadError.value = true
    scoredItems.value = []
  } finally {
    loadingItems.value = false
  }
}

function parseScoringConfig(json: string | null): { mode: string; configScore: number } {
  if (!json) return { mode: 'DEDUCTION', configScore: 0 }
  try {
    const c = JSON.parse(json)
    return {
      mode: c.scoringMode || c.mode || 'DEDUCTION',
      configScore: c.configScore ?? c.score ?? 0,
    }
  } catch {
    return { mode: 'DEDUCTION', configScore: 0 }
  }
}

// ==================== Dimension groups ====================

const dimensionGroups = computed<DimGroup[]>(() => {
  const groups: DimGroup[] = []
  const dimMap = new Map(props.dimensions.map(d => [d.id, d]))

  // Group items by dimensionId
  const groupedItems = new Map<LongId | null, SimItem[]>()
  for (const item of scoredItems.value) {
    const key = item.dimensionId
    if (!groupedItems.has(key)) groupedItems.set(key, [])
    groupedItems.get(key)!.push(item)
  }

  // Build groups for each dimension
  for (const dim of props.dimensions) {
    const items = groupedItems.get(dim.id) || []
    groups.push({
      dimensionId: dim.id,
      dimensionName: dim.dimensionName,
      weight: dim.weight,
      baseScore: dim.baseScore,
      score: 0,
      passed: true,
      items,
    })
  }

  // Unassigned items
  const unassigned = groupedItems.get(null)
  if (unassigned && unassigned.length > 0) {
    groups.push({
      dimensionId: null,
      dimensionName: '未分配分区',
      weight: 0,
      baseScore: 0,
      score: 0,
      passed: true,
      items: unassigned,
    })
  }

  return groups
})

// ==================== Calculation ====================

// ==================== Rule condition evaluation ====================
// 模拟器按 mockQuantity / itemScore 估算条件是否满足。
// 注意: 历史累计 (minCount > 1) 无法在客户端模拟, 此处仅按"当次"判断。

function compareNumber(actual: number, operator: string, target: number): boolean {
  switch (operator) {
    case 'eq': return actual === target
    case 'neq': return actual !== target
    case 'gt': return actual > target
    case 'gte': return actual >= target
    case 'lt': return actual < target
    case 'lte': return actual <= target
    default: return false
  }
}

function evalCondition(cond: SimCondition): boolean {
  const item = scoredItems.value.find(it => it.itemCode === cond.itemCode)
  if (!item) return false
  // 数值条件: 对扣/加分项, 用 mockQuantity (违规次数) 作为比较基准
  const numTarget = typeof cond.value === 'number' ? cond.value : Number(cond.value)
  if (!Number.isNaN(numTarget)) {
    return compareNumber(item.mockQuantity || 0, cond.operator, numTarget)
  }
  // 文本条件 (通过/不通过、等级): 模拟器无法取真实结果, 视为未触发
  return false
}

function evalConditionGroup(conditions: SimCondition[], logic: string): boolean {
  if (conditions.length === 0) return false
  return logic === 'ALL'
    ? conditions.every(evalCondition)
    : conditions.some(evalCondition)
}

function recalculate() {
  const steps: string[] = []

  // Step 1: Calculate each item's score
  for (const item of scoredItems.value) {
    const qty = item.mockQuantity || 0
    const cs = item.configScore
    switch (item.mode) {
      case 'DEDUCTION':
        item.itemScore = -(Math.abs(cs) * qty)
        break
      case 'ADDITION':
        item.itemScore = Math.abs(cs) * qty
        break
      case 'FIXED':
        item.itemScore = qty > 0 ? cs : 0
        break
      case 'RESPONSE_MAPPED':
        item.itemScore = qty // qty doubles as the response value
        break
      default:
        item.itemScore = cs * qty
    }
  }

  // Step 2: Aggregate per dimension (baseScore + SUM)
  const dimScores: { code: string; name: string; weight: number; score: number; passed: boolean }[] = []
  let allPassed = true

  for (const group of dimensionGroups.value) {
    if (group.dimensionId === null) continue
    const totalItemWeight = group.items.reduce((sum, it) => sum + (it.itemWeight || 1), 0)
    const weightedItemSum = group.items.reduce((sum, it) => sum + it.itemScore * (it.itemWeight || 1), 0)
    const itemSum = totalItemWeight > 0 ? weightedItemSum / totalItemWeight : 0
    const dimScore = group.baseScore + itemSum
    group.score = dimScore

    const dim = props.dimensions.find(d => d.id === group.dimensionId)
    const passed = !dim?.passThreshold || dimScore >= dim.passThreshold
    group.passed = passed
    if (!passed) allPassed = false

    dimScores.push({
      code: dim?.dimensionCode || '?',
      name: group.dimensionName,
      weight: group.weight,
      score: dimScore,
      passed,
    })

    const hasWeights = group.items.some(it => it.itemWeight !== 1)
    const itemDetail = hasWeights
      ? `Σ(分×权)/${totalItemWeight.toFixed(1)} = ${itemSum >= 0 ? '+' : ''}${itemSum.toFixed(precision.value)}`
      : `${itemSum >= 0 ? '+' : ''}${itemSum.toFixed(precision.value)}`
    steps.push(`${group.dimensionName}: ${group.baseScore} + (${itemDetail}) = ${dimScore.toFixed(precision.value)}`)
  }

  allDimensionsPassed.value = allPassed

  // Step 3: WEIGHTED_AVG across dimensions
  const totalWeight = dimScores.reduce((s, d) => s + d.weight, 0)
  let total = 0
  if (totalWeight > 0) {
    const weightedSum = dimScores.reduce((s, d) => s + d.score * d.weight, 0)
    total = weightedSum / totalWeight
    steps.push(`加权平均: (${dimScores.map(d => `${d.score.toFixed(1)}*${d.weight}`).join(' + ')}) / ${totalWeight} = ${total.toFixed(precision.value)}`)
  }

  // Step 4: Apply rules (simplified client-side simulation)
  // 与 RuleConfigForm 数据契约统一: 规则配置使用 config.conditions 数组
  for (const rule of props.rules) {
    if (!rule.isEnabled) continue
    try {
      const config = JSON.parse(rule.config)
      const conditions: SimCondition[] = config.conditions || []
      const logic: string = config.conditionLogic || 'ANY'
      switch (rule.ruleType) {
        case 'VETO': {
          if (evalConditionGroup(conditions, logic)) {
            const vetoScore = config.vetoScore ?? 0
            steps.push(`${rule.ruleName}: 触发一票否决，设为 ${vetoScore} 分`)
            total = vetoScore
          }
          break
        }
        case 'PENALTY': {
          if (evalConditionGroup(conditions, logic)) {
            const penalty = config.penaltyScore ?? 5
            steps.push(`${rule.ruleName}: 触发额外扣分 -${penalty}`)
            total -= penalty
          }
          break
        }
        case 'BONUS': {
          if (evalConditionGroup(conditions, logic)) {
            const bonus = config.bonusScore ?? 5
            steps.push(`${rule.ruleName}: 触发加分 +${bonus}`)
            total += bonus
          }
          break
        }
        case 'PROGRESSIVE': {
          const thresholds = (config.thresholds || []) as { count: number; penalty: number }[]
          // 累计满足监控条件的项数
          const hitCount = conditions.filter(c => evalCondition(c)).length
          let penalty = 0
          for (const t of [...thresholds].sort((a, b) => a.count - b.count)) {
            if (hitCount >= t.count) penalty = t.penalty
          }
          if (penalty > 0) {
            steps.push(`${rule.ruleName}: ${hitCount} 项违规，扣 ${penalty} 分`)
            total -= penalty
          }
          break
        }
        case 'PROGRESSIVE_BONUS': {
          const thresholds = (config.thresholds || []) as { count: number; bonus: number }[]
          const hitCount = conditions.filter(c => evalCondition(c)).length
          let bonus = 0
          for (const t of [...thresholds].sort((a, b) => a.count - b.count)) {
            if (hitCount >= t.count) bonus = t.bonus
          }
          if (bonus > 0) {
            steps.push(`${rule.ruleName}: ${hitCount} 项达标，加 ${bonus} 分`)
            total += bonus
          }
          break
        }
      }
    } catch { /* skip bad config */ }
  }

  // Step 5: Clamp
  const min = props.profile.minScore
  const max = props.profile.maxScore
  if (total < min) { total = min; steps.push(`低于下限，保底 ${min}`) }
  if (total > max) { total = max; steps.push(`超过上限，封顶 ${max}`) }

  // Step 6: Precision
  const finalScore = Number(total.toFixed(precision.value))
  displayTotal.value = finalScore.toFixed(precision.value)

  // Step 7: Grade mapping — 已移除 (评级引擎重构, 评级走「评级」Tab 的 Indicator + GradeScheme)
  displayGrade.value = ''
  displayGradeColor.value = '#909399'

  calcSteps.value = steps
}

function modeLabel(mode: string): string {
  switch (mode) {
    case 'DEDUCTION': return '扣'
    case 'ADDITION': return '加'
    case 'FIXED': return '固'
    case 'RESPONSE_MAPPED': return '映'
    default: return '?'
  }
}

function resetAll() {
  for (const item of scoredItems.value) {
    item.mockQuantity = 0
  }
  recalculate()
}

// ==================== Watchers ====================

watch(() => [props.dimensions, props.rules], () => {
  recalculate()
}, { deep: true })

watch(() => props.templateId, () => {
  if (props.templateId) loadTemplateItems()
})

onMounted(() => {
  if (props.templateId) loadTemplateItems()
})
</script>

<style scoped>
.sim-root { display:flex; flex-direction:column; gap:0; }

.sim-header { display:flex; align-items:center; justify-content:space-between; padding:12px 16px; border-bottom:1px solid var(--insp-border-subtle); }
.sim-header-left { display:flex; align-items:center; gap:6px; }
.sim-icon { color:var(--insp-ink-tertiary); }
.sim-title { font-size:13px; font-weight:600; color:var(--insp-ink-primary); }

.sim-btn-ghost { background:none; border:1px solid var(--insp-border-default); border-radius:6px; padding:4px 8px; cursor:pointer; color:var(--insp-ink-tertiary); display:flex; align-items:center; }
.sim-btn-ghost:hover { color:var(--insp-accent); border-color:var(--insp-accent); }

.sim-empty { text-align:center; padding:24px 16px; color:var(--insp-ink-quaternary); font-size:12px; }

/* Load error state */
.sim-error { display:flex; flex-direction:column; align-items:center; gap:8px; padding:24px 16px; }
.sim-error-icon { color:var(--insp-fail); }
.sim-error-text { font-size:12px; color:var(--insp-ink-secondary); }
.sim-error-retry {
  font-size:11px; padding:4px 14px; border:1px solid var(--insp-border-default);
  border-radius:6px; background:var(--insp-bg-surface); color:var(--insp-ink-secondary);
  cursor:pointer; transition:border-color var(--insp-t-fast), color var(--insp-t-fast);
}
.sim-error-retry:hover { border-color:var(--insp-accent); color:var(--insp-accent); }

/* ===== Result banner ===== */
.sim-result-banner { display:flex; align-items:center; gap:10px; padding:14px 16px; background:var(--insp-bg-subtle); border-bottom:1px solid var(--insp-border-subtle); }
.sim-result-main { display:flex; align-items:baseline; gap:2px; }
.sim-result-score { font-size:28px; font-weight:700; color:var(--insp-ink-primary); font-variant-numeric:tabular-nums; }
.sim-result-unit { font-size:12px; color:var(--insp-ink-tertiary); }
.sim-result-grade { font-size:11px; font-weight:600; color:#fff; padding:3px 10px; border-radius:99px; }
.sim-result-fail { font-size:11px; color:var(--insp-fail); font-weight:500; margin-left:auto; }

/* ===== Dimension groups ===== */
.sim-dims { display:flex; flex-direction:column; }
.sim-dim-group { border-bottom:1px solid var(--insp-border-subtle); }
.sim-dim-header { display:flex; align-items:center; justify-content:space-between; padding:10px 16px; background:var(--insp-bg-subtle); }
.sim-dim-name { font-size:12px; font-weight:600; color:var(--insp-ink-primary); }
.sim-dim-meta { display:flex; align-items:center; gap:4px; font-size:11px; color:var(--insp-ink-tertiary); }
.sim-dim-sep { color:var(--insp-border-default); }
.sim-dim-score { font-weight:600; color:var(--insp-accent); }
.sim-dim-score.is-fail { color:var(--insp-fail); }

/* ===== Items ===== */
.sim-items { display:flex; flex-direction:column; }
.sim-item { display:flex; align-items:center; justify-content:space-between; padding:6px 16px; gap:8px; border-top:1px solid var(--insp-border-subtle); }
.sim-item:hover { background:var(--insp-bg-subtle); }

.sim-item-left { display:flex; align-items:center; gap:6px; flex:1; min-width:0; overflow:hidden; }
.sim-item-mode { font-size:10px; font-weight:700; width:18px; height:18px; border-radius:4px; display:flex; align-items:center; justify-content:center; color:#fff; flex-shrink:0; }
.mode-deduction { background:var(--insp-fail); }
.mode-addition { background:var(--insp-pass); }
.mode-fixed { background:var(--insp-accent); }
.mode-response_mapped { background:var(--insp-info, #a855f7); }
.sim-item-name { font-size:12px; color:var(--insp-ink-secondary); overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.sim-item-weight { font-size:10px; color:var(--insp-ink-tertiary); background:var(--insp-bg-subtle); padding:1px 4px; border-radius:3px; flex-shrink:0; }

.sim-item-right { display:flex; align-items:center; gap:4px; flex-shrink:0; }
.sim-input { border:1px solid var(--insp-border-default); border-radius:6px; padding:3px 6px; font-size:12px; outline:none; color:var(--insp-ink-primary); background:var(--insp-bg-surface); text-align:center; }
.sim-input:focus { border-color:var(--insp-accent); box-shadow:0 0 0 2px var(--insp-accent-paler); }
.sim-input-qty { width:44px; }
.sim-item-x { font-size:10px; color:var(--insp-ink-quaternary); }
.sim-item-config { font-size:12px; color:var(--insp-ink-secondary); font-variant-numeric:tabular-nums; min-width:24px; text-align:right; }
.sim-item-eq { font-size:10px; color:var(--insp-ink-quaternary); }
.sim-item-result { font-size:12px; font-weight:600; font-variant-numeric:tabular-nums; min-width:48px; text-align:right; }
.sim-item-result.is-neg { color:var(--insp-fail); }
.sim-item-result.is-pos { color:var(--insp-pass); }

/* ===== Calc steps ===== */
.sim-steps-detail { border-top:1px solid var(--insp-border-subtle); }
.sim-steps-summary { padding:10px 16px; font-size:11px; color:var(--insp-ink-tertiary); cursor:pointer; user-select:none; }
.sim-steps-summary:hover { color:var(--insp-ink-secondary); }
.sim-steps { padding:0 16px 12px; display:flex; flex-direction:column; gap:4px; }
.sim-step { display:flex; align-items:baseline; gap:6px; }
.sim-step-num { font-size:9px; font-weight:700; color:#fff; background:var(--insp-accent); width:16px; height:16px; border-radius:50%; display:flex; align-items:center; justify-content:center; flex-shrink:0; }
.sim-step-text { font-size:11px; color:var(--insp-ink-secondary); font-family:var(--insp-font-mono, monospace); word-break:break-all; }
</style>
