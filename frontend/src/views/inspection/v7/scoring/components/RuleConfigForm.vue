<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Plus, Trash2, X } from 'lucide-vue-next'
import type { RuleType } from '@/types/insp/scoring'
import { ScoringModeConfig, type ScoringMode } from '@/types/insp/enums'
import * as inspTemplateApi from '@/api/insp/template'
import FormulaEditor from './FormulaEditor.vue'

const props = defineProps<{
  ruleType: RuleType
  modelValue: string
  templateId?: number
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

// ==================== Parsed config ====================

function safeParse(json: string): Record<string, any> {
  try {
    const parsed = JSON.parse(json)
    return typeof parsed === 'object' && parsed !== null ? parsed : {}
  } catch {
    return {}
  }
}

const config = ref<Record<string, any>>(safeParse(props.modelValue))

watch(() => props.modelValue, (val) => {
  config.value = safeParse(val)
})

watch(() => props.ruleType, () => {
  config.value = getDefaultConfig(props.ruleType)
  emitConfig()
})

function emitConfig() {
  emit('update:modelValue', JSON.stringify(config.value))
}

function updateField(key: string, value: any) {
  config.value = { ...config.value, [key]: value }
  emitConfig()
}

// ==================== Default configs ====================

function getDefaultConfig(type: RuleType): Record<string, any> {
  switch (type) {
    case 'VETO': return { conditions: [], conditionLogic: 'ANY', lookbackDays: 30, vetoScore: 0 }
    case 'PENALTY': return { conditions: [], conditionLogic: 'ANY', lookbackDays: 30, penaltyScore: 5 }
    case 'PROGRESSIVE': return { conditions: [], conditionLogic: 'ANY', lookbackDays: 30, thresholds: [{ count: 3, penalty: 5 }] }
    case 'PROGRESSIVE_BONUS': return { conditions: [], conditionLogic: 'ANY', lookbackDays: 30, thresholds: [{ count: 3, bonus: 2 }] }
    case 'BONUS': return { conditions: [], conditionLogic: 'ANY', lookbackDays: 30, bonusScore: 5 }
    case 'CUSTOM': return { formula: '' }
    default: return {}
  }
}

// ==================== Template items with scoring info ====================

interface ScoredItem {
  id: number
  itemCode: string
  itemName: string
  sectionName: string
  scoringMode: ScoringMode | null
  scoringConfig: Record<string, any>
}

const scoredItems = ref<ScoredItem[]>([])

async function loadItems() {
  if (!props.templateId) return
  try {
    const sections = await inspTemplateApi.getSections(props.templateId)
    const items: ScoredItem[] = []
    for (const sec of sections) {
      const sectionItems = await inspTemplateApi.getItems(sec.id)
      for (const item of sectionItems) {
        if (!item.isScored) continue
        let scoringMode: ScoringMode | null = null
        let scoringCfg: Record<string, any> = {}
        if (item.scoringConfig) {
          try {
            scoringCfg = JSON.parse(item.scoringConfig)
            scoringMode = scoringCfg.mode || null
          } catch { /* ignore */ }
        }
        items.push({
          id: item.id,
          itemCode: item.itemCode,
          itemName: item.itemName || item.itemCode,
          sectionName: sec.sectionName || sec.sectionCode,
          scoringMode,
          scoringConfig: scoringCfg,
        })
      }
    }
    scoredItems.value = items
  } catch { /* ignore */ }
}

watch(() => props.templateId, () => loadItems(), { immediate: true })

// ==================== Condition-based triggers ====================

interface TriggerCondition {
  itemCode: string
  operator: string
  value: string | number
  minCount?: number  // ≥1, default 1 = current inspection only
}

function getConditions(): TriggerCondition[] {
  return config.value.conditions || []
}

function addCondition(item: ScoredItem) {
  const conditions = [...getConditions()]
  // Check if already added
  if (conditions.some(c => c.itemCode === item.itemCode)) return
  // Set default based on scoring mode
  const defaults = getDefaultCondition(item)
  conditions.push(defaults)
  updateField('conditions', conditions)
}

function removeCondition(idx: number) {
  const conditions = [...getConditions()]
  conditions.splice(idx, 1)
  updateField('conditions', conditions)
}

function updateCondition(idx: number, field: keyof TriggerCondition, value: any) {
  const conditions = [...getConditions()]
  conditions[idx] = { ...conditions[idx], [field]: value }
  updateField('conditions', conditions)
}

function getDefaultConditionBonus(item: ScoredItem): TriggerCondition {
  switch (item.scoringMode) {
    case 'PASS_FAIL':
      return { itemCode: item.itemCode, operator: 'eq', value: '通过' }
    case 'DEDUCTION':
      return { itemCode: item.itemCode, operator: 'eq', value: 0 }
    case 'ADDITION':
      return { itemCode: item.itemCode, operator: 'gte', value: 1 }
    case 'DIRECT':
      return { itemCode: item.itemCode, operator: 'gte', value: 90 }
    case 'LEVEL':
      return { itemCode: item.itemCode, operator: 'eq', value: '优秀' }
    case 'THRESHOLD':
      return { itemCode: item.itemCode, operator: 'eq', value: '合格' }
    default:
      return { itemCode: item.itemCode, operator: 'gte', value: 80 }
  }
}

function addConditionBonus(item: ScoredItem) {
  const conditions = [...getConditions()]
  if (conditions.some(c => c.itemCode === item.itemCode)) return
  conditions.push(getDefaultConditionBonus(item))
  updateField('conditions', conditions)
}

function getConditionDescriptionBonus(cond: TriggerCondition): string {
  const item = findItem(cond.itemCode)
  if (!item) return `${cond.itemCode}: ${cond.operator} ${cond.value}`
  switch (item.scoringMode) {
    case 'PASS_FAIL':
      return `当「${item.itemName}」结果为${cond.value}时计为达标`
    case 'DEDUCTION':
      return `当「${item.itemName}」扣分 ${operatorLabel(cond.operator)} ${cond.value} 时计为达标`
    case 'DIRECT':
    case 'SCORE_TABLE':
    case 'RATING_SCALE':
      return `当「${item.itemName}」分数 ${operatorLabel(cond.operator)} ${cond.value} 时计为达标`
    case 'LEVEL':
    case 'THRESHOLD':
      return `当「${item.itemName}」等级为${cond.value}时计为达标`
    default:
      return `当「${item.itemName}」${operatorLabel(cond.operator)} ${cond.value} 时计为达标`
  }
}

function addBonusThreshold() {
  const thresholds = [...getThresholds()]
  thresholds.push({ count: (thresholds[thresholds.length - 1]?.count || 0) + 3, bonus: 2 })
  updateField('thresholds', thresholds)
}

function updateBonusThreshold(idx: number, value: number) {
  const thresholds = [...getThresholds()]
  thresholds[idx] = { ...thresholds[idx], bonus: value }
  updateField('thresholds', thresholds)
}

function getDefaultCondition(item: ScoredItem): TriggerCondition {
  switch (item.scoringMode) {
    case 'PASS_FAIL':
      return { itemCode: item.itemCode, operator: 'eq', value: '不通过' }
    case 'DEDUCTION':
      return { itemCode: item.itemCode, operator: 'gte', value: 1 }
    case 'ADDITION':
      return { itemCode: item.itemCode, operator: 'gte', value: 1 }
    case 'DIRECT':
      return { itemCode: item.itemCode, operator: 'lte', value: 60 }
    case 'LEVEL':
      return { itemCode: item.itemCode, operator: 'eq', value: '' }
    case 'SCORE_TABLE':
      return { itemCode: item.itemCode, operator: 'lte', value: 60 }
    case 'CUMULATIVE':
      return { itemCode: item.itemCode, operator: 'gte', value: 3 }
    case 'TIERED_DEDUCTION':
      return { itemCode: item.itemCode, operator: 'gte', value: 1 }
    case 'RATING_SCALE':
      return { itemCode: item.itemCode, operator: 'lte', value: 2 }
    case 'THRESHOLD':
      return { itemCode: item.itemCode, operator: 'eq', value: '不合格' }
    default:
      return { itemCode: item.itemCode, operator: 'eq', value: '' }
  }
}

function findItem(code: string): ScoredItem | undefined {
  return scoredItems.value.find(i => i.itemCode === code)
}

// Check if any condition uses historical count (minCount > 1)
function hasHistoricalCondition(): boolean {
  return getConditions().some(c => (c.minCount || 1) > 1)
}

// Items not yet added as conditions
const availableItems = computed(() => {
  const usedCodes = new Set(getConditions().map(c => c.itemCode))
  return scoredItems.value.filter(i => !usedCodes.has(i.itemCode))
})

// ==================== Operator options per scoring mode ====================

const COMPARISON_OPERATORS = [
  { value: 'eq', label: '=' },
  { value: 'neq', label: '≠' },
  { value: 'gt', label: '>' },
  { value: 'gte', label: '≥' },
  { value: 'lt', label: '<' },
  { value: 'lte', label: '≤' },
]

function getOperatorsForMode(mode: ScoringMode | null | undefined): { value: string; label: string }[] {
  switch (mode) {
    case 'PASS_FAIL':
      return [{ value: 'eq', label: '=' }]
    case 'LEVEL':
    case 'THRESHOLD':
      return [{ value: 'eq', label: '=' }, { value: 'neq', label: '≠' }]
    default:
      return COMPARISON_OPERATORS
  }
}

function getValueInputType(mode: ScoringMode | null | undefined): 'select_pass_fail' | 'select_level' | 'number' {
  switch (mode) {
    case 'PASS_FAIL': return 'select_pass_fail'
    case 'LEVEL': return 'select_level'
    case 'THRESHOLD': return 'select_level'
    default: return 'number'
  }
}

function getLevelOptions(item: ScoredItem): { value: string; label: string }[] {
  // Try to extract from item's scoringConfig
  const levels = item.scoringConfig?.levels
  if (Array.isArray(levels)) {
    return levels.map((l: any) => ({ value: l.label || l.name, label: l.label || l.name }))
  }
  // Fallback generic options
  return [
    { value: '优秀', label: '优秀' },
    { value: '良好', label: '良好' },
    { value: '合格', label: '合格' },
    { value: '不合格', label: '不合格' },
  ]
}

function getConditionDescription(cond: TriggerCondition): string {
  const item = findItem(cond.itemCode)
  if (!item) return `${cond.itemCode}: ${cond.operator} ${cond.value}`
  const mode = item.scoringMode
  const modeLabel = mode ? (ScoringModeConfig[mode]?.label || mode) : '未知'
  switch (mode) {
    case 'PASS_FAIL':
      return `当「${item.itemName}」结果为${cond.value}时`
    case 'DEDUCTION':
      return `当「${item.itemName}」扣分 ${operatorLabel(cond.operator)} ${cond.value} 时`
    case 'ADDITION':
      return `当「${item.itemName}」加分 ${operatorLabel(cond.operator)} ${cond.value} 时`
    case 'DIRECT':
    case 'SCORE_TABLE':
    case 'RATING_SCALE':
      return `当「${item.itemName}」分数 ${operatorLabel(cond.operator)} ${cond.value} 时`
    case 'LEVEL':
    case 'THRESHOLD':
      return `当「${item.itemName}」等级为${cond.value}时`
    case 'CUMULATIVE':
      return `当「${item.itemName}」计次 ${operatorLabel(cond.operator)} ${cond.value} 时`
    default:
      return `当「${item.itemName}」${operatorLabel(cond.operator)} ${cond.value} 时`
  }
}

function operatorLabel(op: string): string {
  return COMPARISON_OPERATORS.find(o => o.value === op)?.label || op
}

function getConditionDescriptionEnhanced(cond: TriggerCondition): string {
  const base = getConditionDescription(cond)
  const count = cond.minCount || 1
  if (count <= 1) return base
  return base.replace('时', `累计 ≥ ${count} 次时`)
}

function condValueDesc(cond: TriggerCondition): string {
  const item = findItem(cond.itemCode)
  if (!item) return `${operatorLabel(cond.operator)} ${cond.value}`
  switch (item.scoringMode) {
    case 'PASS_FAIL': return `${cond.value}`
    case 'LEVEL': case 'THRESHOLD': return `等级为${cond.value}`
    default: return `${operatorLabel(cond.operator)} ${cond.value}`
  }
}

// ==================== Threshold table helpers (PROGRESSIVE) ====================

function getThresholds(): { count: number; penalty?: number; bonus?: number }[] {
  return config.value.thresholds || []
}

function addThreshold() {
  const thresholds = [...getThresholds()]
  thresholds.push({ count: (thresholds[thresholds.length - 1]?.count || 0) + 3, penalty: 5 })
  updateField('thresholds', thresholds)
}

function removeThreshold(idx: number) {
  const thresholds = [...getThresholds()]
  thresholds.splice(idx, 1)
  updateField('thresholds', thresholds)
}

function updateThreshold(idx: number, field: string, value: number) {
  const thresholds = [...getThresholds()]
  thresholds[idx] = { ...thresholds[idx], [field]: value }
  updateField('thresholds', thresholds)
}

// ==================== CUSTOM formula helpers ====================

const formulaValue = computed({
  get: () => config.value.formula || '',
  set: (val: string) => updateField('formula', val),
})

const customFormulaVariables = [
  { name: 'baseScore', description: '基础分', category: '分数' },
  { name: 'rawScore', description: '原始得分', category: '分数' },
  { name: 'deductions', description: '扣分合计', category: '分数' },
  { name: 'bonuses', description: '加分合计', category: '分数' },
  { name: 'netScore', description: '净分(基础-扣+加)', category: '分数' },
  { name: 'maxScore', description: '满分', category: '分数' },
  { name: 'itemCount', description: '检查项总数', category: '统计' },
  { name: 'passCount', description: '通过项数', category: '统计' },
  { name: 'failCount', description: '不通过项数', category: '统计' },
  { name: 'passRate', description: '通过率(0~1)', category: '统计' },
  { name: 'population', description: '人数', category: '上下文' },
  { name: 'checkCount', description: '历史检查次数', category: '上下文' },
]

// ==================== Item picker dropdown ====================

const showItemPicker = ref(false)
</script>

<template>
  <div class="sp-rule-config">
    <!-- VETO / PENALTY / BONUS: condition-based triggers -->
    <template v-if="ruleType === 'VETO' || ruleType === 'PENALTY' || ruleType === 'BONUS'">
      <div class="sp-fld">
        <!-- Label + Logic toggle -->
        <div class="sp-label-row">
          <label>触发条件</label>
          <div v-if="getConditions().length > 1" class="sp-logic-toggle">
            <button
              class="sp-logic-btn"
              :class="{ active: (config.conditionLogic || 'ANY') === 'ANY' }"
              @click="updateField('conditionLogic', 'ANY')"
            >任一满足</button>
            <button
              class="sp-logic-btn"
              :class="{ active: config.conditionLogic === 'ALL' }"
              @click="updateField('conditionLogic', 'ALL')"
            >全部满足</button>
          </div>
        </div>

        <!-- Existing conditions -->
        <div v-if="getConditions().length > 0" class="sp-condition-list">
          <div v-if="getConditions().length > 1" class="sp-logic-hint">
            {{ (config.conditionLogic || 'ANY') === 'ALL' ? '以下条件需全部满足才触发' : '以下条件满足任一即触发' }}
          </div>
          <div
            v-for="(cond, idx) in getConditions()"
            :key="idx"
            class="sp-condition-row"
          >
            <div class="sp-cond-item-label">
              {{ findItem(cond.itemCode)?.itemName || cond.itemCode }}
              <span class="sp-cond-mode-tag">{{ findItem(cond.itemCode)?.scoringMode ? ScoringModeConfig[findItem(cond.itemCode)!.scoringMode!]?.label : '' }}</span>
            </div>
            <div class="sp-cond-inputs">
              <!-- Operator -->
              <select
                :value="cond.operator"
                class="sp-cond-select sp-cond-op"
                @change="updateCondition(idx, 'operator', ($event.target as HTMLSelectElement).value)"
              >
                <option
                  v-for="op in getOperatorsForMode(findItem(cond.itemCode)?.scoringMode)"
                  :key="op.value"
                  :value="op.value"
                >{{ op.label }}</option>
              </select>

              <!-- Value: depends on scoring mode -->
              <template v-if="getValueInputType(findItem(cond.itemCode)?.scoringMode) === 'select_pass_fail'">
                <select
                  :value="cond.value"
                  class="sp-cond-select sp-cond-val"
                  @change="updateCondition(idx, 'value', ($event.target as HTMLSelectElement).value)"
                >
                  <option value="不通过">不通过</option>
                  <option value="通过">通过</option>
                </select>
              </template>
              <template v-else-if="getValueInputType(findItem(cond.itemCode)?.scoringMode) === 'select_level'">
                <select
                  :value="cond.value"
                  class="sp-cond-select sp-cond-val"
                  @change="updateCondition(idx, 'value', ($event.target as HTMLSelectElement).value)"
                >
                  <option
                    v-for="opt in getLevelOptions(findItem(cond.itemCode)!)"
                    :key="opt.value"
                    :value="opt.value"
                  >{{ opt.label }}</option>
                </select>
              </template>
              <template v-else>
                <input
                  type="number"
                  :value="cond.value"
                  class="sp-cond-input"
                  @input="updateCondition(idx, 'value', Number(($event.target as HTMLInputElement).value))"
                />
              </template>

              <!-- Min count -->
              <span class="sp-cond-count-label">×</span>
              <input
                type="number"
                :value="cond.minCount || 1"
                min="1"
                max="99"
                class="sp-cond-count"
                title="最低触发次数（1=当次即触发）"
                @input="updateCondition(idx, 'minCount', Number(($event.target as HTMLInputElement).value))"
              />
              <span class="sp-cond-count-suffix">次</span>

              <!-- Remove button -->
              <button class="sp-ic-s danger" @click="removeCondition(idx)">
                <X :size="12" />
              </button>
            </div>
            <div class="sp-cond-desc">{{ getConditionDescriptionEnhanced(cond) }}</div>
          </div>
        </div>

        <!-- Add item picker -->
        <div class="sp-add-cond-wrap">
          <button class="sp-text-btn" @click="showItemPicker = !showItemPicker">
            <Plus :size="12" /> 添加触发项
          </button>
          <div v-if="showItemPicker && availableItems.length > 0" class="sp-item-dropdown">
            <div
              v-for="item in availableItems"
              :key="item.id"
              class="sp-item-option"
              @click="addCondition(item); showItemPicker = false"
            >
              <span class="sp-item-name">{{ item.itemName }}</span>
              <span class="sp-item-mode">{{ item.scoringMode ? ScoringModeConfig[item.scoringMode]?.label : '未配置' }}</span>
              <span class="sp-item-section">{{ item.sectionName }}</span>
            </div>
          </div>
          <div v-if="showItemPicker && availableItems.length === 0" class="sp-item-dropdown">
            <div class="sp-item-empty">{{ scoredItems.length === 0 ? '暂无评分项' : '所有评分项已添加' }}</div>
          </div>
        </div>
      </div>

      <!-- Lookback window (shown when any condition has minCount > 1) -->
      <div v-if="hasHistoricalCondition()" class="sp-fld sp-w-48">
        <label>回溯窗口（天）</label>
        <input
          type="number"
          :value="config.lookbackDays ?? 30"
          min="1"
          max="365"
          @input="updateField('lookbackDays', Number(($event.target as HTMLInputElement).value))"
        />
        <p class="help">统计最近 N 天内的历史记录</p>
      </div>

      <!-- Score value -->
      <div class="sp-fld sp-w-40">
        <label>{{ ruleType === 'VETO' ? '否决分数' : ruleType === 'PENALTY' ? '扣分值' : '加分值' }}</label>
        <input
          type="number"
          :value="ruleType === 'VETO' ? (config.vetoScore ?? 0) : ruleType === 'PENALTY' ? (config.penaltyScore ?? 5) : (config.bonusScore ?? 5)"
          :min="ruleType === 'BONUS' ? 0 : ruleType === 'PENALTY' ? 0 : undefined"
          :step="0.5"
          @input="updateField(ruleType === 'VETO' ? 'vetoScore' : ruleType === 'PENALTY' ? 'penaltyScore' : 'bonusScore', Number(($event.target as HTMLInputElement).value))"
        />
        <p v-if="ruleType === 'VETO'" class="help">触发否决后直接设为此分数（通常为 0）</p>
        <p v-if="ruleType === 'PENALTY'" class="help">触发后额外扣除此分数</p>
      </div>

      <!-- Rule summary -->
      <div v-if="getConditions().length > 0" class="sp-progressive-example" :class="ruleType === 'BONUS' ? 'bonus' : ''">
        <div class="sp-example-title">规则说明</div>
        <div class="sp-example-text">
          {{ (config.conditionLogic || 'ANY') === 'ALL' ? '当' : '当以下任一满足时：' }}
          <template v-for="(cond, i) in getConditions()" :key="i">
            <strong>{{ findItem(cond.itemCode)?.itemName || cond.itemCode }}</strong>
            {{ condValueDesc(cond) }}
            <template v-if="(cond.minCount || 1) > 1">累计 ≥ <strong>{{ cond.minCount }}</strong> 次</template>
            <template v-if="(config.conditionLogic || 'ANY') === 'ALL' && i < getConditions().length - 1">，且</template>
            <template v-else-if="i < getConditions().length - 1">；</template>
          </template>
          <template v-if="hasHistoricalCondition()">（最近 <strong>{{ config.lookbackDays ?? 30 }}</strong> 天）</template>
          → {{ ruleType === 'VETO' ? `否决，设为 ${config.vetoScore ?? 0} 分` : ruleType === 'PENALTY' ? `额外扣 ${config.penaltyScore ?? 5} 分` : `加 ${config.bonusScore ?? 5} 分` }}
        </div>
      </div>
    </template>

    <!-- PROGRESSIVE -->
    <template v-else-if="ruleType === 'PROGRESSIVE'">
      <!-- 1. Monitored items -->
      <div class="sp-fld">
        <div class="sp-label-row">
          <label>监控项（重复违规的检查项）</label>
          <div v-if="getConditions().length > 1" class="sp-logic-toggle">
            <button class="sp-logic-btn" :class="{ active: (config.conditionLogic || 'ANY') === 'ANY' }" @click="updateField('conditionLogic', 'ANY')">任一违规</button>
            <button class="sp-logic-btn" :class="{ active: config.conditionLogic === 'ALL' }" @click="updateField('conditionLogic', 'ALL')">全部违规</button>
          </div>
        </div>

        <div v-if="getConditions().length > 0" class="sp-condition-list">
          <div v-if="getConditions().length > 1" class="sp-logic-hint">
            {{ (config.conditionLogic || 'ANY') === 'ALL' ? '以下监控项需全部违规才累计一次' : '以下监控项任一违规即累计一次' }}
          </div>
          <div
            v-for="(cond, idx) in getConditions()"
            :key="idx"
            class="sp-condition-row"
          >
            <div class="sp-cond-item-label">
              {{ findItem(cond.itemCode)?.itemName || cond.itemCode }}
              <span class="sp-cond-mode-tag">{{ findItem(cond.itemCode)?.scoringMode ? ScoringModeConfig[findItem(cond.itemCode)!.scoringMode!]?.label : '' }}</span>
            </div>
            <div class="sp-cond-inputs">
              <select
                :value="cond.operator"
                class="sp-cond-select sp-cond-op"
                @change="updateCondition(idx, 'operator', ($event.target as HTMLSelectElement).value)"
              >
                <option
                  v-for="op in getOperatorsForMode(findItem(cond.itemCode)?.scoringMode)"
                  :key="op.value"
                  :value="op.value"
                >{{ op.label }}</option>
              </select>
              <template v-if="getValueInputType(findItem(cond.itemCode)?.scoringMode) === 'select_pass_fail'">
                <select
                  :value="cond.value"
                  class="sp-cond-select sp-cond-val"
                  @change="updateCondition(idx, 'value', ($event.target as HTMLSelectElement).value)"
                >
                  <option value="不通过">不通过</option>
                  <option value="通过">通过</option>
                </select>
              </template>
              <template v-else-if="getValueInputType(findItem(cond.itemCode)?.scoringMode) === 'select_level'">
                <select
                  :value="cond.value"
                  class="sp-cond-select sp-cond-val"
                  @change="updateCondition(idx, 'value', ($event.target as HTMLSelectElement).value)"
                >
                  <option
                    v-for="opt in getLevelOptions(findItem(cond.itemCode)!)"
                    :key="opt.value"
                    :value="opt.value"
                  >{{ opt.label }}</option>
                </select>
              </template>
              <template v-else>
                <input
                  type="number"
                  :value="cond.value"
                  class="sp-cond-input"
                  @input="updateCondition(idx, 'value', Number(($event.target as HTMLInputElement).value))"
                />
              </template>
              <button class="sp-ic-s danger" @click="removeCondition(idx)">
                <X :size="12" />
              </button>
            </div>
            <div class="sp-cond-desc">{{ getConditionDescription(cond) }}</div>
          </div>
        </div>

        <div class="sp-add-cond-wrap">
          <button class="sp-text-btn" @click="showItemPicker = !showItemPicker">
            <Plus :size="12" /> 添加监控项
          </button>
          <div v-if="showItemPicker && availableItems.length > 0" class="sp-item-dropdown">
            <div
              v-for="item in availableItems"
              :key="item.id"
              class="sp-item-option"
              @click="addCondition(item); showItemPicker = false"
            >
              <span class="sp-item-name">{{ item.itemName }}</span>
              <span class="sp-item-mode">{{ item.scoringMode ? ScoringModeConfig[item.scoringMode]?.label : '未配置' }}</span>
              <span class="sp-item-section">{{ item.sectionName }}</span>
            </div>
          </div>
          <div v-if="showItemPicker && availableItems.length === 0" class="sp-item-dropdown">
            <div class="sp-item-empty">{{ scoredItems.length === 0 ? '暂无评分项' : '所有评分项已添加' }}</div>
          </div>
        </div>
        <p class="help">选择需要监控重复违规的检查项，满足条件即计为一次违规</p>
      </div>

      <!-- 2. Lookback window -->
      <div class="sp-fld sp-w-48">
        <label>回溯窗口（天）</label>
        <input
          type="number"
          :value="config.lookbackDays ?? 30"
          min="1"
          max="365"
          @input="updateField('lookbackDays', Number(($event.target as HTMLInputElement).value))"
        />
        <p class="help">统计最近 N 天内的违规次数</p>
      </div>

      <!-- 3. Threshold table -->
      <div class="sp-fld">
        <label>累进阈值表</label>
        <table class="sp-table">
          <thead>
            <tr>
              <th>累计违规 &ge;</th>
              <th>每次扣分</th>
              <th class="sp-th-action"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(t, idx) in getThresholds()" :key="idx">
              <td>
                <input
                  type="number"
                  :value="t.count"
                  min="1"
                  @input="updateThreshold(idx, 'count', Number(($event.target as HTMLInputElement).value))"
                />
              </td>
              <td>
                <input
                  type="number"
                  :value="t.penalty"
                  min="0"
                  step="0.5"
                  @input="updateThreshold(idx, 'penalty', Number(($event.target as HTMLInputElement).value))"
                />
              </td>
              <td>
                <button
                  v-if="getThresholds().length > 1"
                  class="sp-ic-s danger"
                  @click="removeThreshold(idx)"
                >
                  <Trash2 :size="12" />
                </button>
              </td>
            </tr>
          </tbody>
        </table>
        <button class="sp-text-btn" @click="addThreshold">
          <Plus :size="12" /> 添加阈值
        </button>
        <p class="help">同一监控项在回溯窗口内累计违规达到阈值后，扣分升级</p>
      </div>

      <!-- Example explanation -->
      <div v-if="getConditions().length > 0 && getThresholds().length > 0" class="sp-progressive-example">
        <div class="sp-example-title">规则说明</div>
        <div class="sp-example-text">
          <template v-for="(cond, i) in getConditions()" :key="i">
            <strong>{{ findItem(cond.itemCode)?.itemName || cond.itemCode }}</strong>
            <template v-if="getConditions().length > 1 && i < getConditions().length - 1">
              {{ (config.conditionLogic || 'ANY') === 'ALL' ? ' 且 ' : ' 或 ' }}
            </template>
          </template>
          违规，在最近 <strong>{{ config.lookbackDays ?? 30 }}</strong> 天内：
          <template v-for="(t, i) in getThresholds()" :key="i">
            累计 ≥ <strong>{{ t.count }}</strong> 次时每次扣 <strong>{{ t.penalty }}</strong> 分<template v-if="i < getThresholds().length - 1">；</template>
          </template>
        </div>
      </div>
    </template>

    <!-- PROGRESSIVE_BONUS -->
    <template v-else-if="ruleType === 'PROGRESSIVE_BONUS'">
      <!-- 1. Monitored items -->
      <div class="sp-fld">
        <div class="sp-label-row">
          <label>监控项（持续达标的检查项）</label>
          <div v-if="getConditions().length > 1" class="sp-logic-toggle">
            <button class="sp-logic-btn" :class="{ active: (config.conditionLogic || 'ANY') === 'ANY' }" @click="updateField('conditionLogic', 'ANY')">任一达标</button>
            <button class="sp-logic-btn" :class="{ active: config.conditionLogic === 'ALL' }" @click="updateField('conditionLogic', 'ALL')">全部达标</button>
          </div>
        </div>

        <div v-if="getConditions().length > 0" class="sp-condition-list">
          <div v-if="getConditions().length > 1" class="sp-logic-hint">
            {{ (config.conditionLogic || 'ANY') === 'ALL' ? '以下监控项需全部达标才累计一次' : '以下监控项任一达标即累计一次' }}
          </div>
          <div
            v-for="(cond, idx) in getConditions()"
            :key="idx"
            class="sp-condition-row"
          >
            <div class="sp-cond-item-label">
              {{ findItem(cond.itemCode)?.itemName || cond.itemCode }}
              <span class="sp-cond-mode-tag">{{ findItem(cond.itemCode)?.scoringMode ? ScoringModeConfig[findItem(cond.itemCode)!.scoringMode!]?.label : '' }}</span>
            </div>
            <div class="sp-cond-inputs">
              <select
                :value="cond.operator"
                class="sp-cond-select sp-cond-op"
                @change="updateCondition(idx, 'operator', ($event.target as HTMLSelectElement).value)"
              >
                <option
                  v-for="op in getOperatorsForMode(findItem(cond.itemCode)?.scoringMode)"
                  :key="op.value"
                  :value="op.value"
                >{{ op.label }}</option>
              </select>
              <template v-if="getValueInputType(findItem(cond.itemCode)?.scoringMode) === 'select_pass_fail'">
                <select
                  :value="cond.value"
                  class="sp-cond-select sp-cond-val"
                  @change="updateCondition(idx, 'value', ($event.target as HTMLSelectElement).value)"
                >
                  <option value="通过">通过</option>
                  <option value="不通过">不通过</option>
                </select>
              </template>
              <template v-else-if="getValueInputType(findItem(cond.itemCode)?.scoringMode) === 'select_level'">
                <select
                  :value="cond.value"
                  class="sp-cond-select sp-cond-val"
                  @change="updateCondition(idx, 'value', ($event.target as HTMLSelectElement).value)"
                >
                  <option
                    v-for="opt in getLevelOptions(findItem(cond.itemCode)!)"
                    :key="opt.value"
                    :value="opt.value"
                  >{{ opt.label }}</option>
                </select>
              </template>
              <template v-else>
                <input
                  type="number"
                  :value="cond.value"
                  class="sp-cond-input"
                  @input="updateCondition(idx, 'value', Number(($event.target as HTMLInputElement).value))"
                />
              </template>
              <button class="sp-ic-s danger" @click="removeCondition(idx)">
                <X :size="12" />
              </button>
            </div>
            <div class="sp-cond-desc">{{ getConditionDescriptionBonus(cond) }}</div>
          </div>
        </div>

        <div class="sp-add-cond-wrap">
          <button class="sp-text-btn" @click="showItemPicker = !showItemPicker">
            <Plus :size="12" /> 添加监控项
          </button>
          <div v-if="showItemPicker && availableItems.length > 0" class="sp-item-dropdown">
            <div
              v-for="item in availableItems"
              :key="item.id"
              class="sp-item-option"
              @click="addConditionBonus(item); showItemPicker = false"
            >
              <span class="sp-item-name">{{ item.itemName }}</span>
              <span class="sp-item-mode">{{ item.scoringMode ? ScoringModeConfig[item.scoringMode]?.label : '未配置' }}</span>
              <span class="sp-item-section">{{ item.sectionName }}</span>
            </div>
          </div>
          <div v-if="showItemPicker && availableItems.length === 0" class="sp-item-dropdown">
            <div class="sp-item-empty">{{ scoredItems.length === 0 ? '暂无评分项' : '所有评分项已添加' }}</div>
          </div>
        </div>
        <p class="help">选择需要监控持续达标的检查项，满足条件即计为一次达标</p>
      </div>

      <!-- 2. Lookback window -->
      <div class="sp-fld sp-w-48">
        <label>回溯窗口（天）</label>
        <input
          type="number"
          :value="config.lookbackDays ?? 30"
          min="1"
          max="365"
          @input="updateField('lookbackDays', Number(($event.target as HTMLInputElement).value))"
        />
        <p class="help">统计最近 N 天内的达标次数</p>
      </div>

      <!-- 3. Threshold table -->
      <div class="sp-fld">
        <label>累进奖励表</label>
        <table class="sp-table">
          <thead>
            <tr>
              <th>连续达标 &ge;</th>
              <th>每次加分</th>
              <th class="sp-th-action"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(t, idx) in getThresholds()" :key="idx">
              <td>
                <input
                  type="number"
                  :value="t.count"
                  min="1"
                  @input="updateThreshold(idx, 'count', Number(($event.target as HTMLInputElement).value))"
                />
              </td>
              <td>
                <input
                  type="number"
                  :value="t.bonus"
                  min="0"
                  step="0.5"
                  @input="updateBonusThreshold(idx, Number(($event.target as HTMLInputElement).value))"
                />
              </td>
              <td>
                <button
                  v-if="getThresholds().length > 1"
                  class="sp-ic-s danger"
                  @click="removeThreshold(idx)"
                >
                  <Trash2 :size="12" />
                </button>
              </td>
            </tr>
          </tbody>
        </table>
        <button class="sp-text-btn" @click="addBonusThreshold">
          <Plus :size="12" /> 添加阈值
        </button>
        <p class="help">同一监控项在回溯窗口内连续达标达到阈值后，额外奖励分递增</p>
      </div>

      <!-- Example explanation -->
      <div v-if="getConditions().length > 0 && getThresholds().length > 0" class="sp-progressive-example bonus">
        <div class="sp-example-title">规则说明</div>
        <div class="sp-example-text">
          <template v-for="(cond, i) in getConditions()" :key="i">
            <strong>{{ findItem(cond.itemCode)?.itemName || cond.itemCode }}</strong>
            <template v-if="getConditions().length > 1 && i < getConditions().length - 1">
              {{ (config.conditionLogic || 'ANY') === 'ALL' ? ' 且 ' : ' 或 ' }}
            </template>
          </template>
          达标，在最近 <strong>{{ config.lookbackDays ?? 30 }}</strong> 天内：
          <template v-for="(t, i) in getThresholds()" :key="i">
            累计 ≥ <strong>{{ t.count }}</strong> 次时每次加 <strong>{{ t.bonus }}</strong> 分<template v-if="i < getThresholds().length - 1">；</template>
          </template>
        </div>
      </div>
    </template>

    <!-- CUSTOM -->
    <template v-else-if="ruleType === 'CUSTOM'">
      <div class="sp-formula-wrap">
        <FormulaEditor
          v-model="formulaValue"
          :available-variables="customFormulaVariables"
        />
      </div>
    </template>

    <!-- Fallback for unknown types -->
    <template v-else>
      <div class="sp-fld">
        <label>配置 (JSON)</label>
        <textarea
          :value="modelValue"
          rows="4"
          class="sp-mono"
          @input="$emit('update:modelValue', ($event.target as HTMLTextAreaElement).value)"
        />
      </div>
    </template>
  </div>
</template>

<style scoped>
/* ========== Layout ========== */
.sp-rule-config { display:flex; flex-direction:column; gap:12px; }
.sp-w-40 { max-width:160px; }
.sp-w-48 { max-width:200px; }

/* ========== Form fields ========== */
.sp-fld label { display:block; font-size:12px; font-weight:500; color:#5a6474; margin-bottom:5px; }
.sp-fld input, .sp-fld select, .sp-fld textarea { width:100%; border:1px solid #dce1e8; border-radius:8px; padding:8px 12px; font-size:13px; outline:none; transition:border-color 0.2s, box-shadow 0.2s; color:#1e2a3a; background:#fff; box-sizing:border-box; }
.sp-fld input::placeholder, .sp-fld textarea::placeholder { color:#b8c0cc; }
.sp-fld input:focus, .sp-fld select:focus, .sp-fld textarea:focus { border-color:#7aadff; box-shadow:0 0 0 3px rgba(26,109,255,0.08); }
.sp-fld .help { font-size:11px; color:#8c95a3; margin-top:4px; }
.sp-mono { font-family:monospace; }

/* ========== Label row with logic toggle ========== */
.sp-label-row { display:flex; align-items:center; justify-content:space-between; margin-bottom:5px; }
.sp-label-row label { margin-bottom:0; }
.sp-logic-toggle { display:flex; border:1px solid #dce1e8; border-radius:6px; overflow:hidden; }
.sp-logic-btn { padding:3px 10px; font-size:11px; border:none; background:#fff; color:#8c95a3; cursor:pointer; transition:all 0.15s; }
.sp-logic-btn.active { background:#1a6dff; color:#fff; }
.sp-logic-btn:not(.active):hover { background:#f0f4ff; color:#5a6474; }
.sp-logic-hint { font-size:11px; color:#8c95a3; font-style:italic; padding:0 0 4px; }

/* ========== Condition list ========== */
.sp-condition-list { display:flex; flex-direction:column; gap:8px; margin-bottom:8px; }

.sp-condition-row {
  border:1px solid #e8ecf0; border-radius:8px; padding:10px 12px;
  background:#fff; transition:border-color 0.15s;
}
.sp-condition-row:hover { border-color:#c5d0dd; }

.sp-cond-item-label {
  font-size:13px; font-weight:500; color:#1e2a3a; margin-bottom:6px;
  display:flex; align-items:center; gap:6px;
}
.sp-cond-mode-tag {
  font-size:10px; padding:1px 6px; border-radius:99px;
  background:#f0f4ff; color:#1a6dff; font-weight:400;
}

.sp-cond-inputs {
  display:flex; align-items:center; gap:6px;
}
.sp-cond-select {
  border:1px solid #dce1e8; border-radius:6px; padding:5px 8px;
  font-size:12px; outline:none; color:#1e2a3a; background:#fff;
  transition:border-color 0.2s;
}
.sp-cond-select:focus { border-color:#7aadff; box-shadow:0 0 0 2px rgba(26,109,255,0.08); }
.sp-cond-op { width:52px; }
.sp-cond-val { min-width:80px; }
.sp-cond-input {
  width:72px; border:1px solid #dce1e8; border-radius:6px; padding:5px 8px;
  font-size:12px; outline:none; color:#1e2a3a; background:#fff;
}
.sp-cond-input:focus { border-color:#7aadff; box-shadow:0 0 0 2px rgba(26,109,255,0.08); }

.sp-cond-count-label { font-size:12px; color:#8c95a3; flex-shrink:0; }
.sp-cond-count { width:44px; border:1px solid #dce1e8; border-radius:6px; padding:5px 4px; font-size:12px; outline:none; text-align:center; color:#1e2a3a; }
.sp-cond-count:focus { border-color:#7aadff; box-shadow:0 0 0 2px rgba(26,109,255,0.08); }
.sp-cond-count-suffix { font-size:11px; color:#8c95a3; flex-shrink:0; }

.sp-cond-desc {
  font-size:11px; color:#8c95a3; margin-top:4px;
  font-style:italic;
}

/* ========== Add condition ========== */
.sp-add-cond-wrap { position:relative; }

.sp-item-dropdown {
  position:absolute; top:100%; left:0; z-index:10;
  margin-top:4px; min-width:320px; max-height:200px; overflow-y:auto;
  background:#fff; border:1px solid #dce1e8; border-radius:10px;
  box-shadow:0 8px 24px rgba(0,0,0,0.12);
}

.sp-item-option {
  display:flex; align-items:center; gap:8px;
  padding:8px 12px; cursor:pointer; transition:background 0.1s;
  font-size:12px; color:#1e2a3a;
}
.sp-item-option:hover { background:#f0f4ff; }
.sp-item-option:first-child { border-radius:10px 10px 0 0; }
.sp-item-option:last-child { border-radius:0 0 10px 10px; }

.sp-item-name { flex:1; min-width:0; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.sp-item-mode { font-size:10px; padding:1px 6px; border-radius:99px; background:#f0f4ff; color:#1a6dff; flex-shrink:0; }
.sp-item-section { font-size:10px; color:#b8c0cc; flex-shrink:0; }
.sp-item-empty { padding:12px; text-align:center; color:#b8c0cc; font-size:12px; }

/* ========== Table ========== */
.sp-table { width:100%; border-collapse:collapse; font-size:13px; }
.sp-table th { text-align:left; font-size:11px; font-weight:500; color:#8c95a3; padding:6px 8px; background:#f8f9fb; border-bottom:1px solid #e8ecf0; }
.sp-table td { padding:8px; border-bottom:1px solid #f0f2f5; }
.sp-table input { width:72px; border:1px solid #dce1e8; border-radius:6px; padding:5px 8px; font-size:12px; outline:none; }
.sp-table input:focus { border-color:#7aadff; box-shadow:0 0 0 2px rgba(26,109,255,0.08); }
.sp-th-action { width:32px; }

/* ========== Buttons ========== */
.sp-ic-s { background:none; border:none; padding:3px; color:#b8c0cc; cursor:pointer; border-radius:4px; display:flex; align-items:center; transition:all 0.12s; }
.sp-ic-s:hover { color:#1a6dff; }
.sp-ic-s.danger:hover { color:#d93025; }

.sp-text-btn { display:inline-flex; align-items:center; gap:4px; margin-top:4px; padding:0; background:none; border:none; font-size:12px; color:#1a6dff; cursor:pointer; transition:color 0.15s; }
.sp-text-btn:hover { color:#1558d6; }

/* ========== Formula wrapper ========== */
.sp-formula-wrap { border-radius:10px; overflow:hidden; }

/* ========== Progressive example ========== */
.sp-progressive-example {
  background:#f8faff; border:1px solid #e0eaff; border-left:3px solid #1a6dff;
  border-radius:0 8px 8px 0; padding:10px 14px;
}
.sp-progressive-example.bonus {
  background:#f6fff6; border-color:#c8ecc8; border-left-color:#67C23A;
}
.sp-example-title { font-size:11px; font-weight:600; color:#1a6dff; margin-bottom:4px; }
.sp-example-text { font-size:12px; color:#5a6474; line-height:1.6; }
.sp-example-text strong { color:#1e2a3a; }
</style>
