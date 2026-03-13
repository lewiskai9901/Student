<script setup lang="ts">
import { ref, watch, computed, reactive, nextTick } from 'vue'
import {
  Save, HelpCircle, Plus, Trash2,
  BarChart3, Clipboard, Info, ShieldCheck, Zap,
  GripVertical, AlertTriangle, ChevronDown,
} from 'lucide-vue-next'
import {
  ItemTypeConfig, ItemTypeGroups, type ItemType,
  ScoringModeConfig, type ScoringMode,
} from '@/types/insp/enums'
import type { TemplateItem, ResponseSet, ResponseSetOption } from '@/types/insp/template'
import { responseSetApi } from '@/api/insp/responseSet'
import ConditionBuilder from '@/components/insp/ConditionBuilder.vue'

const props = defineProps<{
  item: TemplateItem
  responseSets: ResponseSet[]
  allItems?: TemplateItem[]
}>()

const emit = defineEmits<{
  save: [data: Partial<TemplateItem>]
  cancel: []
}>()

// ==================== Refs ====================
const itemNameInput = ref<HTMLInputElement | null>(null)

// ==================== Accordion State ====================
const expandedSections = ref<Set<string>>(new Set(['info']))

function toggleSection(key: string) {
  if (expandedSections.value.has(key)) {
    expandedSections.value.delete(key)
  } else {
    expandedSections.value.add(key)
  }
  expandedSections.value = new Set(expandedSections.value) // trigger reactivity
}

function isSectionOpen(key: string) {
  return expandedSections.value.has(key)
}

// ==================== Form State ====================

const form = ref({
  itemName: '',
  description: '',
  itemType: 'TEXT' as ItemType,
  responseSetId: null as number | null,
  helpContent: '',
  validationRules: '',
  scoringConfig: '',
  conditionLogic: '',
})

// Category: 'scored' or 'capture'
type ItemCategory = 'scored' | 'capture'
const itemCategory = ref<ItemCategory>('scored')

// ==================== Category Badge ====================

const categoryLabel = computed(() => {
  if (itemCategory.value === 'scored') {
    return ScoringModeConfig[scoring.mode]?.label || '评分项'
  }
  return ItemTypeConfig[form.value.itemType]?.label || '采集项'
})

const categoryColor = computed(() => itemCategory.value === 'scored' ? '#2563eb' : '#059669')

// ==================== Scoring Config Visual State ====================

interface LevelItem { label: string; score: number }
interface ScoreTableOption { label: string; description: string; score: number }
interface TierItem { label: string; score: number }
interface Dimension { key: string; label: string; weight: number; maxScore: number }
interface ProbImpact { label: string; value: number }
interface ThresholdItem { max: number | null; label: string; score: number }
interface FormulaInput { key: string; label: string }

interface ScoringConfigData {
  mode: ScoringMode
  passScore: number
  failScore: number
  maxDeduction: number
  deductionStep: number
  maxBonus: number
  bonusStep: number
  minScore: number
  maxScore: number
  levels: LevelItem[]
  options: ScoreTableOption[]
  cumulativeLabel: string
  scorePerCount: number
  maxCount: number
  tiers: TierItem[]
  maxStars: number
  scorePerStar: number
  weightedMultiMaxScore: number
  dimensions: Dimension[]
  probabilities: ProbImpact[]
  impacts: ProbImpact[]
  unit: string
  thresholds: ThresholdItem[]
  formulaType: string
  formulaInputs: FormulaInput[]
  formulaMaxScore: number
  formulaMinScore: number
}

const scoring = reactive<ScoringConfigData>({
  mode: 'PASS_FAIL',
  passScore: 0,
  failScore: -2,
  maxDeduction: -10,
  deductionStep: 1,
  maxBonus: 5,
  bonusStep: 1,
  minScore: 0,
  maxScore: 10,
  levels: [
    { label: '优', score: 10 },
    { label: '良', score: 7 },
    { label: '中', score: 4 },
    { label: '差', score: 0 },
  ],
  options: [
    { label: '优秀', description: '', score: 10 },
    { label: '良好', description: '', score: 7 },
    { label: '一般', description: '', score: 3 },
    { label: '差', description: '', score: 0 },
  ],
  cumulativeLabel: '违规次数',
  scorePerCount: -2,
  maxCount: 10,
  tiers: [
    { label: '轻微', score: -1 },
    { label: '一般', score: -3 },
    { label: '严重', score: -5 },
    { label: '重大', score: -10 },
  ],
  maxStars: 5,
  scorePerStar: 2,
  weightedMultiMaxScore: 10,
  dimensions: [
    { key: 'dim1', label: '维度1', weight: 50, maxScore: 10 },
    { key: 'dim2', label: '维度2', weight: 50, maxScore: 10 },
  ],
  probabilities: [
    { label: '罕见', value: 1 },
    { label: '不太可能', value: 2 },
    { label: '可能', value: 3 },
    { label: '很可能', value: 4 },
    { label: '几乎确定', value: 5 },
  ],
  impacts: [
    { label: '微小', value: 1 },
    { label: '较小', value: 2 },
    { label: '中等', value: 3 },
    { label: '较大', value: 4 },
    { label: '严重', value: 5 },
  ],
  unit: '',
  thresholds: [
    { max: 25, label: '合格', score: 10 },
    { max: 30, label: '注意', score: 5 },
    { max: null, label: '不合格', score: 0 },
  ],
  formulaType: 'ratio',
  formulaInputs: [
    { key: 'actual', label: '实测值' },
    { key: 'standard', label: '标准值' },
  ],
  formulaMaxScore: 10,
  formulaMinScore: 0,
})

// Scoring mode summary for collapsed view
const scoringSummary = computed(() => {
  const label = ScoringModeConfig[scoring.mode]?.label || scoring.mode
  switch (scoring.mode) {
    case 'PASS_FAIL': return `${label}  通过${scoring.passScore} / 不通过${scoring.failScore}`
    case 'DEDUCTION': return `${label}  最大扣${scoring.maxDeduction}，步长${scoring.deductionStep}`
    case 'ADDITION': return `${label}  最大加${scoring.maxBonus}，步长${scoring.bonusStep}`
    case 'DIRECT': return `${label}  ${scoring.minScore}~${scoring.maxScore}分`
    case 'LEVEL': return `${label}  ${scoring.levels.length}个等级`
    case 'SCORE_TABLE': return `${label}  ${scoring.options.length}个档位`
    case 'CUMULATIVE': return `${label}  每次${scoring.scorePerCount}分，最多${scoring.maxCount}次`
    case 'TIERED_DEDUCTION': return `${label}  ${scoring.tiers.length}个档位`
    case 'RATING_SCALE': return `${label}  ${scoring.maxStars}星，满分${scoring.maxStars * scoring.scorePerStar}`
    case 'WEIGHTED_MULTI': return `${label}  ${scoring.dimensions.length}个维度`
    case 'RISK_MATRIX': return `${label}  ${scoring.probabilities.length}×${scoring.impacts.length}矩阵`
    case 'THRESHOLD': return `${label}  ${scoring.thresholds.length}个区间`
    case 'FORMULA': return `${label}  ${scoring.formulaType}`
    default: return label
  }
})

// Scoring config dialog
const showScoringDialog = ref(false)
const maskMouseDownTarget = ref<EventTarget | null>(null)
function onMaskMouseDown(e: MouseEvent) { maskMouseDownTarget.value = e.target }
function onMaskClick(e: MouseEvent) {
  if (e.target === e.currentTarget && maskMouseDownTarget.value === e.currentTarget) showScoringDialog.value = false
  maskMouseDownTarget.value = null
}

function parseScoringConfig(json: string) {
  try {
    const c = JSON.parse(json)
    scoring.mode = c.mode || 'PASS_FAIL'
    switch (scoring.mode) {
      case 'PASS_FAIL':
        scoring.passScore = c.passScore ?? 0
        scoring.failScore = c.failScore ?? -2
        break
      case 'DEDUCTION':
        scoring.maxDeduction = c.maxDeduction ?? -10
        scoring.deductionStep = c.deductionStep ?? 1
        break
      case 'ADDITION':
        scoring.maxBonus = c.maxBonus ?? 5
        scoring.bonusStep = c.bonusStep ?? 1
        break
      case 'DIRECT':
        scoring.minScore = c.minScore ?? 0
        scoring.maxScore = c.maxScore ?? 10
        break
      case 'LEVEL':
        if (Array.isArray(c.levels) && c.levels.length > 0) scoring.levels = c.levels
        break
      case 'SCORE_TABLE':
        if (Array.isArray(c.options) && c.options.length > 0) scoring.options = c.options
        break
      case 'CUMULATIVE':
        scoring.cumulativeLabel = c.label ?? '违规次数'
        scoring.scorePerCount = c.scorePerCount ?? -2
        scoring.maxCount = c.maxCount ?? 10
        break
      case 'TIERED_DEDUCTION':
        if (Array.isArray(c.tiers) && c.tiers.length > 0) scoring.tiers = c.tiers
        break
      case 'RATING_SCALE':
        scoring.maxStars = c.maxStars ?? 5
        scoring.scorePerStar = c.scorePerStar ?? 2
        break
      case 'WEIGHTED_MULTI':
        scoring.weightedMultiMaxScore = c.maxScore ?? 10
        if (Array.isArray(c.dimensions) && c.dimensions.length > 0) scoring.dimensions = c.dimensions
        break
      case 'RISK_MATRIX':
        if (Array.isArray(c.probabilities) && c.probabilities.length > 0) scoring.probabilities = c.probabilities
        if (Array.isArray(c.impacts) && c.impacts.length > 0) scoring.impacts = c.impacts
        break
      case 'THRESHOLD':
        scoring.unit = c.unit ?? ''
        if (Array.isArray(c.thresholds) && c.thresholds.length > 0) scoring.thresholds = c.thresholds
        break
      case 'FORMULA':
        scoring.formulaType = c.formulaType ?? 'ratio'
        if (Array.isArray(c.inputs) && c.inputs.length > 0) scoring.formulaInputs = c.inputs
        scoring.formulaMaxScore = c.maxScore ?? 10
        scoring.formulaMinScore = c.minScore ?? 0
        break
    }
  } catch {
    scoring.mode = 'PASS_FAIL'
  }
}

function serializeScoringConfig(): string {
  const obj: Record<string, any> = { mode: scoring.mode }
  switch (scoring.mode) {
    case 'PASS_FAIL':
      obj.passScore = scoring.passScore
      obj.failScore = scoring.failScore
      break
    case 'DEDUCTION':
      obj.maxDeduction = scoring.maxDeduction
      obj.deductionStep = scoring.deductionStep
      break
    case 'ADDITION':
      obj.maxBonus = scoring.maxBonus
      obj.bonusStep = scoring.bonusStep
      break
    case 'DIRECT':
      obj.minScore = scoring.minScore
      obj.maxScore = scoring.maxScore
      break
    case 'LEVEL':
      obj.levels = scoring.levels
      break
    case 'SCORE_TABLE':
      obj.options = scoring.options
      break
    case 'CUMULATIVE':
      obj.label = scoring.cumulativeLabel
      obj.scorePerCount = scoring.scorePerCount
      obj.maxCount = scoring.maxCount
      break
    case 'TIERED_DEDUCTION':
      obj.tiers = scoring.tiers
      break
    case 'RATING_SCALE':
      obj.maxStars = scoring.maxStars
      obj.scorePerStar = scoring.scorePerStar
      break
    case 'WEIGHTED_MULTI':
      obj.maxScore = scoring.weightedMultiMaxScore
      obj.dimensions = scoring.dimensions
      break
    case 'RISK_MATRIX':
      obj.probabilities = scoring.probabilities
      obj.impacts = scoring.impacts
      break
    case 'THRESHOLD':
      obj.unit = scoring.unit
      obj.thresholds = scoring.thresholds.map(t => ({
        ...t,
        max: (t.max === '' || t.max === undefined || (typeof t.max === 'number' && isNaN(t.max))) ? null : t.max,
      }))
      break
    case 'FORMULA':
      obj.formulaType = scoring.formulaType
      obj.inputs = scoring.formulaInputs
      obj.maxScore = scoring.formulaMaxScore
      obj.minScore = scoring.formulaMinScore
      break
  }
  return JSON.stringify(obj)
}

// ==================== Validation Rules Visual State ====================

type RuleType = 'required' | 'minLength' | 'maxLength' | 'range' | 'minPhotos' | 'maxPhotos' | 'pattern' | 'requiredIfFail'

interface ValidationRule {
  type: RuleType
  value?: number | string
  min?: number
  max?: number
  message: string
}

const validationRulesList = ref<ValidationRule[]>([])

const RULE_TYPE_OPTIONS: { value: RuleType; label: string }[] = [
  { value: 'required', label: '必填' },
  { value: 'requiredIfFail', label: '不通过时必填' },
  { value: 'minLength', label: '最小长度' },
  { value: 'maxLength', label: '最大长度' },
  { value: 'range', label: '数值范围' },
  { value: 'minPhotos', label: '最少照片数' },
  { value: 'maxPhotos', label: '最多照片数' },
  { value: 'pattern', label: '正则匹配' },
]

// Scoring mode → allowed validation rule types
const SCORING_MODE_ALLOWED_RULES: Record<string, RuleType[]> = {
  PASS_FAIL: ['required', 'requiredIfFail', 'minPhotos', 'maxPhotos'],
  DEDUCTION: ['required', 'minPhotos', 'maxPhotos'],
  ADDITION: ['required', 'minPhotos', 'maxPhotos'],
  DIRECT: ['required', 'minPhotos', 'maxPhotos'],
  LEVEL: ['required', 'minPhotos', 'maxPhotos'],
  SCORE_TABLE: ['required', 'minPhotos', 'maxPhotos'],
  CUMULATIVE: ['required', 'minPhotos', 'maxPhotos'],
  TIERED_DEDUCTION: ['required', 'minPhotos', 'maxPhotos'],
  RATING_SCALE: ['required', 'minPhotos', 'maxPhotos'],
  WEIGHTED_MULTI: ['required', 'minPhotos', 'maxPhotos'],
  RISK_MATRIX: ['required', 'minPhotos', 'maxPhotos'],
  THRESHOLD: ['required', 'range', 'minPhotos', 'maxPhotos'],
  FORMULA: ['required', 'minPhotos', 'maxPhotos'],
}

// Capture type → allowed validation rule types
const CAPTURE_TYPE_ALLOWED_RULES: Record<string, RuleType[]> = {
  TEXT: ['required', 'minLength', 'maxLength', 'pattern'],
  TEXTAREA: ['required', 'minLength', 'maxLength'],
  RICH_TEXT: ['required', 'minLength', 'maxLength'],
  NUMBER: ['required', 'range'],
  SLIDER: ['required', 'range'],
  SELECT: ['required'],
  MULTI_SELECT: ['required'],
  CHECKBOX: ['required'],
  RADIO: ['required'],
  DATE: ['required'],
  TIME: ['required'],
  DATETIME: ['required'],
  PHOTO: ['required', 'minPhotos', 'maxPhotos'],
  VIDEO: ['required'],
  SIGNATURE: ['required'],
  FILE_UPLOAD: ['required'],
  GPS: ['required'],
  BARCODE: ['required', 'pattern'],
}

const filteredRuleOptions = computed(() => {
  if (itemCategory.value === 'scored') {
    const allowed = SCORING_MODE_ALLOWED_RULES[scoring.mode]
    if (!allowed) return RULE_TYPE_OPTIONS
    return RULE_TYPE_OPTIONS.filter(opt => allowed.includes(opt.value))
  } else {
    const allowed = CAPTURE_TYPE_ALLOWED_RULES[form.value.itemType]
    if (!allowed) return RULE_TYPE_OPTIONS.filter(opt => opt.value === 'required')
    return RULE_TYPE_OPTIONS.filter(opt => allowed.includes(opt.value))
  }
})

function parseValidationRules(json: string) {
  try {
    const arr = JSON.parse(json)
    if (Array.isArray(arr)) {
      validationRulesList.value = arr.map((r: any) => ({
        type: r.type || 'required',
        value: r.value,
        min: r.min,
        max: r.max,
        message: r.message || '',
      }))
      return
    }
  } catch { /* ignore */ }
  validationRulesList.value = []
}

function serializeValidationRules(): string {
  if (validationRulesList.value.length === 0) return ''
  return JSON.stringify(validationRulesList.value.map(r => {
    const obj: Record<string, any> = { type: r.type }
    if (r.type === 'range') {
      if (r.min != null) obj.min = r.min
      if (r.max != null) obj.max = r.max
    } else if (['minLength', 'maxLength', 'minPhotos', 'maxPhotos'].includes(r.type)) {
      obj.value = Number(r.value) || 0
    } else if (r.type === 'pattern') {
      obj.value = r.value || ''
    }
    if (r.message) obj.message = r.message
    return obj
  }))
}

function addRule() {
  validationRulesList.value.push({ type: 'required', message: '' })
  if (!expandedSections.value.has('validation')) {
    expandedSections.value.add('validation')
    expandedSections.value = new Set(expandedSections.value)
  }
}

function removeRule(idx: number) {
  validationRulesList.value.splice(idx, 1)
}

function ruleNeedsValue(type: RuleType): boolean {
  return ['minLength', 'maxLength', 'minPhotos', 'maxPhotos', 'pattern'].includes(type)
}

function ruleNeedsRange(type: RuleType): boolean {
  return type === 'range'
}

// ==================== Condition Logic ====================

const conditionItems = computed(() =>
  (props.allItems || []).filter(i => i.id !== props.item?.id).map(i => {
    let scoringMode: ScoringMode | undefined
    if (i.isScored && i.scoringConfig) {
      try { scoringMode = JSON.parse(i.scoringConfig).mode } catch { /* ignore */ }
    }
    return {
      itemCode: i.itemCode,
      itemName: i.itemName,
      itemType: i.itemType,
      isScored: i.isScored,
      scoringMode,
    }
  })
)

// ==================== Sync ====================

/** Reset ALL scoring fields to clean defaults */
function resetScoringDefaults() {
  scoring.mode = 'PASS_FAIL'
  scoring.passScore = 0
  scoring.failScore = -2
  scoring.maxDeduction = -10
  scoring.deductionStep = 1
  scoring.maxBonus = 5
  scoring.bonusStep = 1
  scoring.minScore = 0
  scoring.maxScore = 10
  scoring.levels = [{ label: '优', score: 10 }, { label: '良', score: 7 }, { label: '中', score: 4 }, { label: '差', score: 0 }]
  scoring.options = [{ label: '优秀', description: '', score: 10 }, { label: '良好', description: '', score: 7 }, { label: '一般', description: '', score: 3 }, { label: '差', description: '', score: 0 }]
  scoring.cumulativeLabel = '违规次数'
  scoring.scorePerCount = -2
  scoring.maxCount = 10
  scoring.tiers = [{ label: '轻微', score: -1 }, { label: '一般', score: -3 }, { label: '严重', score: -5 }, { label: '重大', score: -10 }]
  scoring.maxStars = 5
  scoring.scorePerStar = 2
  scoring.weightedMultiMaxScore = 10
  scoring.dimensions = [{ key: 'dim1', label: '维度1', weight: 50, maxScore: 10 }, { key: 'dim2', label: '维度2', weight: 50, maxScore: 10 }]
  scoring.probabilities = [{ label: '罕见', value: 1 }, { label: '不太可能', value: 2 }, { label: '可能', value: 3 }, { label: '很可能', value: 4 }, { label: '几乎确定', value: 5 }]
  scoring.impacts = [{ label: '微小', value: 1 }, { label: '较小', value: 2 }, { label: '中等', value: 3 }, { label: '较大', value: 4 }, { label: '严重', value: 5 }]
  scoring.unit = ''
  scoring.thresholds = [{ max: 25, label: '合格', score: 10 }, { max: 30, label: '注意', score: 5 }, { max: null, label: '不合格', score: 0 }]
  scoring.formulaType = 'ratio'
  scoring.formulaInputs = [{ key: 'actual', label: '实测值' }, { key: 'standard', label: '标准值' }]
  scoring.formulaMaxScore = 10
  scoring.formulaMinScore = 0
}

function inferCategory(item: TemplateItem): ItemCategory {
  return item.isScored ? 'scored' : 'capture'
}

watch(() => props.item, (item) => {
  if (item) {
    form.value = {
      itemName: item.itemName || '',
      description: item.description || '',
      itemType: item.itemType,
      responseSetId: item.responseSetId,
      helpContent: item.helpContent || '',
      validationRules: item.validationRules || '',
      scoringConfig: item.scoringConfig || '',
      conditionLogic: item.conditionLogic || '',
    }
    itemCategory.value = inferCategory(item)
    resetScoringDefaults()
    if (itemCategory.value === 'scored') {
      parseScoringConfig(item.scoringConfig || '{}')
    }
    parseValidationRules(item.validationRules || '[]')
    // Reset accordion: open info section
    expandedSections.value = new Set(['info'])

    // Auto-focus name input for newly added items (empty name)
    if (!item.itemName) {
      nextTick(() => {
        itemNameInput.value?.focus()
      })
    }
  }
}, { immediate: true })

function handleSave() {
  if (itemCategory.value === 'scored') {
    // === Scored item save ===

    // Validate scoring config constraints
    if (scoring.mode === 'PASS_FAIL' && scoring.failScore > 0) scoring.failScore = 0
    if (scoring.mode === 'DEDUCTION' && scoring.maxDeduction > 0) scoring.maxDeduction = 0
    if (scoring.mode === 'DIRECT' && scoring.minScore >= scoring.maxScore) scoring.maxScore = scoring.minScore + 1
    if (scoring.mode === 'LEVEL' && scoring.levels.length < 2)
      scoring.levels = [{ label: '优', score: 10 }, { label: '差', score: 0 }]
    if (scoring.mode === 'SCORE_TABLE' && scoring.options.length < 2)
      scoring.options = [{ label: '优秀', description: '', score: 10 }, { label: '差', description: '', score: 0 }]
    if (scoring.mode === 'TIERED_DEDUCTION')
      scoring.tiers = scoring.tiers.map(t => ({ ...t, score: Math.min(0, t.score) }))
    if (scoring.mode === 'RATING_SCALE') {
      scoring.maxStars = Math.max(3, Math.min(10, scoring.maxStars))
      scoring.scorePerStar = Math.max(1, scoring.scorePerStar)
    }
    if (scoring.mode === 'WEIGHTED_MULTI') {
      if (scoring.dimensions.length < 1)
        scoring.dimensions = [{ key: 'dim1', label: '维度1', weight: 100, maxScore: 10 }]
      const weightSum = scoring.dimensions.reduce((s, d) => s + d.weight, 0)
      if (weightSum !== 100 && weightSum > 0) {
        const factor = 100 / weightSum
        scoring.dimensions = scoring.dimensions.map(d => ({ ...d, weight: Math.round(d.weight * factor) }))
        const newSum = scoring.dimensions.reduce((s, d) => s + d.weight, 0)
        if (newSum !== 100 && scoring.dimensions.length > 0)
          scoring.dimensions[scoring.dimensions.length - 1].weight += 100 - newSum
      }
    }
    if (scoring.mode === 'THRESHOLD' && scoring.thresholds.length < 1)
      scoring.thresholds = [{ max: null, label: '默认', score: 10 }]
    if (scoring.mode === 'RISK_MATRIX') {
      if (scoring.probabilities.length < 2) scoring.probabilities = [{ label: '低', value: 1 }, { label: '高', value: 5 }]
      if (scoring.impacts.length < 2) scoring.impacts = [{ label: '小', value: 1 }, { label: '大', value: 5 }]
    }
    if (scoring.mode === 'FORMULA' && scoring.formulaInputs.length < 1)
      scoring.formulaInputs = [{ key: 'value', label: '值' }]
    if (scoring.mode === 'FORMULA' && scoring.formulaMaxScore <= scoring.formulaMinScore)
      scoring.formulaMaxScore = scoring.formulaMinScore + 1

    // Data hardening
    if (scoring.mode === 'LEVEL') {
      scoring.levels = scoring.levels.filter(l => l.label.trim() !== '').map(l => ({ ...l, score: isNaN(l.score) ? 0 : l.score }))
      if (scoring.levels.length < 2) scoring.levels = [{ label: '优', score: 10 }, { label: '差', score: 0 }]
    }
    if (scoring.mode === 'SCORE_TABLE') {
      scoring.options = scoring.options.filter(o => o.label.trim() !== '').map(o => ({ ...o, score: isNaN(o.score) ? 0 : o.score }))
      if (scoring.options.length < 2) scoring.options = [{ label: '优秀', description: '', score: 10 }, { label: '差', description: '', score: 0 }]
    }
    if (scoring.mode === 'TIERED_DEDUCTION') {
      scoring.tiers = scoring.tiers.filter(t => t.label.trim() !== '').map(t => ({ ...t, score: isNaN(t.score) ? 0 : Math.min(0, t.score) }))
      if (scoring.tiers.length < 1) scoring.tiers = [{ label: '轻微', score: -1 }]
    }
    if (scoring.mode === 'WEIGHTED_MULTI') {
      scoring.dimensions = scoring.dimensions.filter(d => d.weight > 0 && d.maxScore > 0)
      if (scoring.dimensions.length < 1) scoring.dimensions = [{ key: 'dim1', label: '维度1', weight: 100, maxScore: 10 }]
    }
    if (scoring.mode === 'THRESHOLD') {
      const hasNullMax = scoring.thresholds.some(t => t.max == null)
      if (!hasNullMax) scoring.thresholds.push({ max: null, label: '其他', score: 0 })
    }
    if (scoring.mode === 'RISK_MATRIX') {
      scoring.probabilities = scoring.probabilities.filter(p => p.value > 0).sort((a, b) => a.value - b.value)
      scoring.impacts = scoring.impacts.filter(i => i.value > 0).sort((a, b) => a.value - b.value)
      if (scoring.probabilities.length < 2) scoring.probabilities = [{ label: '低', value: 1 }, { label: '高', value: 5 }]
      if (scoring.impacts.length < 2) scoring.impacts = [{ label: '小', value: 1 }, { label: '大', value: 5 }]
    }

    // Auto-remove validation rules not applicable to current scoring mode
    const allowedRules = SCORING_MODE_ALLOWED_RULES[scoring.mode]
    if (allowedRules) {
      validationRulesList.value = validationRulesList.value.filter(r => allowedRules.includes(r.type))
    }

    // Serialize
    let scoringJson: string
    if (scoringFromResponseSet.value) {
      scoringJson = JSON.stringify({
        mode: 'SCORE_TABLE',
        sourceType: 'RESPONSE_SET',
        responseSetId: form.value.responseSetId,
        options: rsOptions.value.map(o => ({
          label: o.optionLabel,
          value: o.optionValue,
          score: o.score ?? 0,
          isFlagged: o.isFlagged,
        })),
      })
    } else {
      scoringJson = serializeScoringConfig()
    }
    const validationJson = serializeValidationRules()

    emit('save', {
      itemName: form.value.itemName,
      description: form.value.description || undefined,
      responseSetId: form.value.responseSetId,
      helpContent: form.value.helpContent || undefined,
      validationRules: validationJson || undefined,
      scoringConfig: scoringJson || undefined,
      conditionLogic: form.value.conditionLogic || undefined,
      isScored: true,
    } as Partial<TemplateItem>)
  } else {
    // === Capture item save ===
    const allowedRules = CAPTURE_TYPE_ALLOWED_RULES[form.value.itemType]
    if (allowedRules) {
      validationRulesList.value = validationRulesList.value.filter(r => allowedRules.includes(r.type))
    }
    const validationJson = serializeValidationRules()

    emit('save', {
      itemName: form.value.itemName,
      itemType: form.value.itemType,
      description: form.value.description || undefined,
      responseSetId: captureNeedsResponseSet.value ? form.value.responseSetId : null,
      helpContent: form.value.helpContent || undefined,
      validationRules: validationJson || undefined,
      scoringConfig: undefined,
      conditionLogic: form.value.conditionLogic || undefined,
      isScored: false,
    } as Partial<TemplateItem>)
  }
}

// Whether current scoring mode uses choice-type with response set
const needsResponseSet = computed(() =>
  ['SELECT', 'MULTI_SELECT', 'CHECKBOX', 'RADIO'].includes(form.value.itemType)
)

// Whether capture item type needs response set
const captureNeedsResponseSet = computed(() =>
  itemCategory.value === 'capture' && ['SELECT', 'MULTI_SELECT', 'CHECKBOX', 'RADIO'].includes(form.value.itemType)
)

// ==================== Response Set Options (for scoring) ====================

const rsOptions = ref<ResponseSetOption[]>([])
const rsOptionsLoading = ref(false)

watch(() => form.value.responseSetId, async (newId) => {
  if (!newId) {
    rsOptions.value = []
    return
  }
  rsOptionsLoading.value = true
  try {
    rsOptions.value = await responseSetApi.getOptions(newId)
  } catch {
    rsOptions.value = []
  } finally {
    rsOptionsLoading.value = false
  }
}, { immediate: true })

// Whether scoring is driven by response set
const scoringFromResponseSet = computed(() =>
  itemCategory.value === 'scored' && needsResponseSet.value && !!form.value.responseSetId && rsOptions.value.length > 0
)
</script>

<template>
  <div class="ie-root">
    <!-- ═══════ Header ═══════ -->
    <div class="ie-header">
      <div class="ie-header-left">
        <span class="ie-badge" :style="{ color: categoryColor, background: categoryColor + '0f', borderColor: categoryColor + '30' }">
          <component :is="itemCategory === 'scored' ? BarChart3 : Clipboard" :size="11" />
          {{ itemCategory === 'scored' ? '评分项' : '采集项' }}
        </span>
        <span class="ie-code">{{ item.itemCode }}</span>
      </div>
      <div class="ie-header-right">
        <button class="ie-btn-ghost" @click="emit('cancel')">取消</button>
        <button class="ie-btn-primary" @click="handleSave">
          <Save :size="12" /> 保存
        </button>
      </div>
    </div>

    <!-- ═══════ Scrollable Body ═══════ -->
    <div class="ie-body">

      <!-- ── 1. 基本信息 ── -->
      <div class="ie-acc" :class="{ open: isSectionOpen('info') }">
        <button class="ie-acc-head" @click="toggleSection('info')">
          <span class="ie-acc-title">基本信息</span>
          <ChevronDown :size="14" class="ie-acc-arrow" />
        </button>
        <div v-if="isSectionOpen('info')" class="ie-acc-body">
          <div class="ie-fld">
            <label>名称 <span class="ie-req">*</span></label>
            <input ref="itemNameInput" v-model="form.itemName" placeholder="检查项名称" />
          </div>
          <div class="ie-fld">
            <label>描述</label>
            <textarea v-model="form.description" rows="2" placeholder="可选的补充说明" />
          </div>
          <div class="ie-fld">
            <label>帮助提示</label>
            <textarea v-model="form.helpContent" rows="2" placeholder="检查员填写时看到的提示" />
          </div>
        </div>
      </div>

      <!-- ── 2. 评分配置（评分项）── -->
      <template v-if="itemCategory === 'scored'">
        <!-- Response set binding -->
        <div v-if="needsResponseSet" class="ie-acc" :class="{ open: isSectionOpen('rs') }">
          <button class="ie-acc-head" @click="toggleSection('rs')">
            <span class="ie-acc-title">选项集</span>
            <span v-if="form.responseSetId" class="ie-acc-tag blue">已绑定</span>
            <ChevronDown :size="14" class="ie-acc-arrow" />
          </button>
          <div v-if="isSectionOpen('rs')" class="ie-acc-body">
            <div class="ie-fld">
              <select v-model="form.responseSetId" class="ie-select">
                <option :value="null">-- 不使用选项集 --</option>
                <option v-for="rs in responseSets" :key="rs.id" :value="rs.id">{{ rs.setName }}</option>
              </select>
            </div>
            <!-- Preview -->
            <div v-if="scoringFromResponseSet" class="ie-rs-preview">
              <div v-for="opt in rsOptions" :key="opt.id" class="ie-rs-row">
                <div class="ie-rs-label">
                  <span v-if="opt.optionColor" class="ie-rs-dot" :style="{ background: opt.optionColor }" />
                  {{ opt.optionLabel }}
                  <span v-if="opt.isFlagged" class="ie-rs-flag">标记</span>
                </div>
                <span class="ie-rs-score">{{ opt.score ?? 0 }}分</span>
              </div>
            </div>
            <div v-else-if="form.responseSetId && rsOptionsLoading" class="ie-muted">加载选项中...</div>
          </div>
        </div>

        <!-- Scoring mode (only when NOT driven by response set) -->
        <div v-if="!scoringFromResponseSet" class="ie-acc" :class="{ open: isSectionOpen('scoring') }">
          <button class="ie-acc-head" @click="toggleSection('scoring')">
            <span class="ie-acc-title">评分配置</span>
            <span class="ie-acc-tag blue">{{ ScoringModeConfig[scoring.mode]?.label }}</span>
            <ChevronDown :size="14" class="ie-acc-arrow" />
          </button>
          <div v-if="isSectionOpen('scoring')" class="ie-acc-body">
            <!-- Scoring summary + open dialog button -->
            <div class="ie-scoring-row">
              <span class="ie-scoring-summary">{{ scoringSummary }}</span>
              <button class="ie-btn-sm" @click="showScoringDialog = true">编辑</button>
            </div>
          </div>
        </div>
      </template>

      <!-- ── 2b. 输入类型（采集项）── -->
      <template v-if="itemCategory === 'capture'">
        <div class="ie-acc" :class="{ open: isSectionOpen('capture') }">
          <button class="ie-acc-head" @click="toggleSection('capture')">
            <span class="ie-acc-title">输入类型</span>
            <span class="ie-acc-tag green">{{ ItemTypeConfig[form.itemType]?.label }}</span>
            <ChevronDown :size="14" class="ie-acc-arrow" />
          </button>
          <div v-if="isSectionOpen('capture')" class="ie-acc-body">
            <div class="ie-fld">
              <select v-model="form.itemType" class="ie-select">
                <optgroup v-for="group in ItemTypeGroups" :key="group" :label="group">
                  <option
                    v-for="(info, key) in ItemTypeConfig"
                    :key="key"
                    v-show="info.group === group"
                    :value="key"
                  >{{ info.label }}</option>
                </optgroup>
              </select>
            </div>
            <div v-if="ItemTypeConfig[form.itemType]?.description" class="ie-desc-box">
              <Info :size="11" />
              <span>{{ ItemTypeConfig[form.itemType].description }}</span>
            </div>
            <!-- Response set for choice-type capture -->
            <div v-if="captureNeedsResponseSet" class="ie-fld">
              <label>选项集</label>
              <select v-model="form.responseSetId" class="ie-select">
                <option :value="null">-- 不使用选项集 --</option>
                <option v-for="rs in responseSets" :key="rs.id" :value="rs.id">{{ rs.setName }}</option>
              </select>
            </div>
          </div>
        </div>
      </template>

      <!-- ── 3. 验证规则 ── -->
      <div class="ie-acc" :class="{ open: isSectionOpen('validation') }">
        <button class="ie-acc-head" @click="toggleSection('validation')">
          <span class="ie-acc-title">验证规则</span>
          <span v-if="validationRulesList.length > 0" class="ie-acc-count">{{ validationRulesList.length }}</span>
          <ChevronDown :size="14" class="ie-acc-arrow" />
        </button>
        <div v-if="isSectionOpen('validation')" class="ie-acc-body">
          <div v-if="validationRulesList.length === 0" class="ie-muted">暂无验证规则</div>
          <div v-for="(rule, idx) in validationRulesList" :key="idx" class="ie-rule">
            <div class="ie-rule-top">
              <span class="ie-rule-num">{{ idx + 1 }}</span>
              <select v-model="rule.type" class="ie-select ie-select-sm">
                <option v-for="opt in filteredRuleOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
              </select>
              <button class="ie-btn-icon-del" @click="removeRule(idx)"><Trash2 :size="12" /></button>
            </div>
            <div class="ie-rule-body">
              <div v-if="ruleNeedsValue(rule.type)" class="ie-fld">
                <label>值</label>
                <input v-model="rule.value" :type="rule.type === 'pattern' ? 'text' : 'number'" />
              </div>
              <template v-if="ruleNeedsRange(rule.type)">
                <div class="ie-row-2">
                  <div class="ie-fld"><label>最小</label><input v-model.number="rule.min" type="number" /></div>
                  <div class="ie-fld"><label>最大</label><input v-model.number="rule.max" type="number" /></div>
                </div>
              </template>
              <div class="ie-fld">
                <label>错误提示</label>
                <input v-model="rule.message" placeholder="校验失败时显示" />
              </div>
            </div>
          </div>
          <button class="ie-link-btn" @click="addRule"><Plus :size="12" /> 添加规则</button>
        </div>
      </div>

      <!-- ── 4. 条件逻辑 ── -->
      <div class="ie-acc" :class="{ open: isSectionOpen('condition') }">
        <button class="ie-acc-head" @click="toggleSection('condition')">
          <span class="ie-acc-title">条件逻辑</span>
          <span v-if="form.conditionLogic" class="ie-acc-tag orange">已配置</span>
          <ChevronDown :size="14" class="ie-acc-arrow" />
        </button>
        <div v-if="isSectionOpen('condition')" class="ie-acc-body">
          <ConditionBuilder
            v-model="form.conditionLogic"
            :all-items="conditionItems"
            :target-label="form.itemName"
          />
        </div>
      </div>

    </div>

    <!-- ═══════ Scoring Config Dialog ═══════ -->
    <Teleport to="body">
      <Transition name="ie-modal">
        <div v-if="showScoringDialog" class="ie-mask" @mousedown="onMaskMouseDown" @click="onMaskClick">
          <div class="ie-dialog">
            <div class="ie-dialog-head">
              <h3>评分配置</h3>
              <button class="ie-dialog-close" @click="showScoringDialog = false">&times;</button>
            </div>
            <div class="ie-dialog-body">
              <!-- Mode selector -->
              <div class="ie-fld">
                <label>评分模式</label>
                <div class="ie-mode-grid">
                  <button
                    v-for="(cfg, key) in ScoringModeConfig" :key="key"
                    :class="['ie-mode-chip', scoring.mode === key && 'active']"
                    @click="scoring.mode = key as any"
                  >
                    {{ cfg.label }}
                  </button>
                </div>
                <div class="ie-desc-box">
                  <Info :size="11" />
                  <span>{{ ScoringModeConfig[scoring.mode]?.description }}</span>
                </div>
              </div>

              <!-- Mode-specific params -->
              <div class="ie-params">
                <!-- PASS_FAIL -->
                <template v-if="scoring.mode === 'PASS_FAIL'">
                  <div class="ie-row-2">
                    <div class="ie-fld"><label>通过得分</label><input v-model.number="scoring.passScore" type="number" /></div>
                    <div class="ie-fld"><label>不通过扣分</label><input v-model.number="scoring.failScore" type="number" :max="0" /></div>
                  </div>
                </template>

                <!-- DEDUCTION -->
                <template v-if="scoring.mode === 'DEDUCTION'">
                  <div class="ie-row-2">
                    <div class="ie-fld"><label>最大扣分</label><input v-model.number="scoring.maxDeduction" type="number" :max="0" /></div>
                    <div class="ie-fld"><label>步长</label><input v-model.number="scoring.deductionStep" type="number" :min="1" /></div>
                  </div>
                </template>

                <!-- ADDITION -->
                <template v-if="scoring.mode === 'ADDITION'">
                  <div class="ie-row-2">
                    <div class="ie-fld"><label>最大加分</label><input v-model.number="scoring.maxBonus" type="number" :min="1" /></div>
                    <div class="ie-fld"><label>步长</label><input v-model.number="scoring.bonusStep" type="number" :min="1" /></div>
                  </div>
                </template>

                <!-- DIRECT -->
                <template v-if="scoring.mode === 'DIRECT'">
                  <div class="ie-row-2">
                    <div class="ie-fld"><label>最低分</label><input v-model.number="scoring.minScore" type="number" /></div>
                    <div class="ie-fld"><label>最高分</label><input v-model.number="scoring.maxScore" type="number" /></div>
                  </div>
                  <div v-if="scoring.minScore >= scoring.maxScore" class="ie-alert">
                    <AlertTriangle :size="12" /> 最低分必须小于最高分
                  </div>
                </template>

                <!-- LEVEL -->
                <template v-if="scoring.mode === 'LEVEL'">
                  <div class="ie-list-head">
                    <label>等级列表</label>
                    <div class="ie-presets">
                      <button @click="scoring.levels = [{label:'优',score:10},{label:'良',score:7},{label:'中',score:4},{label:'差',score:0}]">优良中差</button>
                      <button @click="scoring.levels = [{label:'A',score:10},{label:'B',score:7},{label:'C',score:4},{label:'D',score:0}]">ABCD</button>
                      <button @click="scoring.levels = [{label:'1级',score:2},{label:'2级',score:4},{label:'3级',score:6},{label:'4级',score:8},{label:'5级',score:10}]">1-5级</button>
                    </div>
                  </div>
                  <div class="ie-list">
                    <div v-for="(lv, i) in scoring.levels" :key="i" class="ie-list-row">
                      <input v-model="lv.label" class="ie-list-input w-flex" placeholder="标签" />
                      <input v-model.number="lv.score" type="number" class="ie-list-input w-60" placeholder="分值" />
                      <button class="ie-btn-icon-del" @click="scoring.levels.splice(i, 1)"><Trash2 :size="12" /></button>
                    </div>
                  </div>
                  <button class="ie-link-btn" @click="scoring.levels.push({ label: '', score: 0 })"><Plus :size="12" /> 添加等级</button>
                </template>

                <!-- SCORE_TABLE -->
                <template v-if="scoring.mode === 'SCORE_TABLE'">
                  <label class="ie-fld-label">评分档位</label>
                  <div class="ie-list">
                    <div v-for="(opt, i) in scoring.options" :key="i" class="ie-opt-card">
                      <div class="ie-opt-top">
                        <input v-model="opt.label" class="ie-list-input w-flex" placeholder="档位名" />
                        <input v-model.number="opt.score" type="number" class="ie-list-input w-60" placeholder="分值" />
                        <button class="ie-btn-icon-del" @click="scoring.options.splice(i, 1)"><Trash2 :size="12" /></button>
                      </div>
                      <input v-model="opt.description" class="ie-list-input-full" placeholder="描述..." />
                    </div>
                  </div>
                  <button class="ie-link-btn" @click="scoring.options.push({ label: '', description: '', score: 0 })"><Plus :size="12" /> 添加档位</button>
                </template>

                <!-- CUMULATIVE -->
                <template v-if="scoring.mode === 'CUMULATIVE'">
                  <div class="ie-fld"><label>标签名</label><input v-model="scoring.cumulativeLabel" placeholder="如: 违规次数" /></div>
                  <div class="ie-row-2">
                    <div class="ie-fld"><label>每次分值</label><input v-model.number="scoring.scorePerCount" type="number" /></div>
                    <div class="ie-fld"><label>最大次数</label><input v-model.number="scoring.maxCount" type="number" :min="1" /></div>
                  </div>
                </template>

                <!-- TIERED_DEDUCTION -->
                <template v-if="scoring.mode === 'TIERED_DEDUCTION'">
                  <div class="ie-list-head">
                    <label>扣分档位</label>
                    <div class="ie-presets">
                      <button @click="scoring.tiers = [{label:'轻微',score:-1},{label:'一般',score:-3},{label:'严重',score:-5},{label:'重大',score:-10}]">预设</button>
                    </div>
                  </div>
                  <div class="ie-list">
                    <div v-for="(t, i) in scoring.tiers" :key="i" class="ie-list-row">
                      <input v-model="t.label" class="ie-list-input w-flex" placeholder="档位名" />
                      <input v-model.number="t.score" type="number" :max="0" class="ie-list-input w-60" placeholder="扣分" />
                      <button class="ie-btn-icon-del" @click="scoring.tiers.splice(i, 1)"><Trash2 :size="12" /></button>
                    </div>
                  </div>
                  <button class="ie-link-btn" @click="scoring.tiers.push({ label: '', score: 0 })"><Plus :size="12" /> 添加档位</button>
                </template>

                <!-- RATING_SCALE -->
                <template v-if="scoring.mode === 'RATING_SCALE'">
                  <div class="ie-row-2">
                    <div class="ie-fld"><label>最大星数 (3-10)</label><input v-model.number="scoring.maxStars" type="number" :min="3" :max="10" /></div>
                    <div class="ie-fld"><label>每星分值</label><input v-model.number="scoring.scorePerStar" type="number" :min="1" /></div>
                  </div>
                  <div class="ie-calc">满分 = {{ scoring.maxStars }} × {{ scoring.scorePerStar }} = <strong>{{ scoring.maxStars * scoring.scorePerStar }}</strong></div>
                </template>

                <!-- WEIGHTED_MULTI -->
                <template v-if="scoring.mode === 'WEIGHTED_MULTI'">
                  <div class="ie-fld" style="max-width:120px"><label>总分</label><input v-model.number="scoring.weightedMultiMaxScore" type="number" :min="1" /></div>
                  <div class="ie-list-head">
                    <label>子维度</label>
                    <span :class="['ie-weight-tag', scoring.dimensions.reduce((s,d)=>s+d.weight,0) === 100 ? 'ok' : 'bad']">
                      {{ scoring.dimensions.reduce((s,d)=>s+d.weight,0) }}%
                    </span>
                  </div>
                  <div class="ie-list">
                    <div v-for="(dim, i) in scoring.dimensions" :key="i" class="ie-list-row">
                      <input v-model="dim.key" class="ie-list-input w-60" placeholder="key" />
                      <input v-model="dim.label" class="ie-list-input w-flex" placeholder="名称" />
                      <input v-model.number="dim.weight" type="number" class="ie-list-input w-50" placeholder="%" />
                      <span class="ie-unit">%</span>
                      <input v-model.number="dim.maxScore" type="number" class="ie-list-input w-50" placeholder="满分" />
                      <button class="ie-btn-icon-del" @click="scoring.dimensions.splice(i, 1)"><Trash2 :size="12" /></button>
                    </div>
                  </div>
                  <button class="ie-link-btn" @click="scoring.dimensions.push({ key: `dim${scoring.dimensions.length+1}`, label: '', weight: 0, maxScore: 10 })"><Plus :size="12" /> 添加维度</button>
                </template>

                <!-- RISK_MATRIX -->
                <template v-if="scoring.mode === 'RISK_MATRIX'">
                  <label class="ie-fld-label">可能性级别</label>
                  <div class="ie-list">
                    <div v-for="(p, i) in scoring.probabilities" :key="'p'+i" class="ie-list-row">
                      <input v-model="p.label" class="ie-list-input w-flex" placeholder="标签" />
                      <input v-model.number="p.value" type="number" class="ie-list-input w-60" placeholder="值" />
                      <button class="ie-btn-icon-del" @click="scoring.probabilities.splice(i, 1)"><Trash2 :size="12" /></button>
                    </div>
                  </div>
                  <button class="ie-link-btn" @click="scoring.probabilities.push({ label: '', value: scoring.probabilities.length + 1 })"><Plus :size="12" /> 添加</button>
                  <div class="ie-divider" />
                  <label class="ie-fld-label">影响级别</label>
                  <div class="ie-list">
                    <div v-for="(imp, i) in scoring.impacts" :key="'i'+i" class="ie-list-row">
                      <input v-model="imp.label" class="ie-list-input w-flex" placeholder="标签" />
                      <input v-model.number="imp.value" type="number" class="ie-list-input w-60" placeholder="值" />
                      <button class="ie-btn-icon-del" @click="scoring.impacts.splice(i, 1)"><Trash2 :size="12" /></button>
                    </div>
                  </div>
                  <button class="ie-link-btn" @click="scoring.impacts.push({ label: '', value: scoring.impacts.length + 1 })"><Plus :size="12" /> 添加</button>
                </template>

                <!-- THRESHOLD -->
                <template v-if="scoring.mode === 'THRESHOLD'">
                  <div class="ie-fld" style="max-width:140px"><label>单位</label><input v-model="scoring.unit" placeholder="如: °C, dB" /></div>
                  <label class="ie-fld-label">阈值区间</label>
                  <div class="ie-list">
                    <div v-for="(th, i) in scoring.thresholds" :key="i" class="ie-list-row">
                      <span class="ie-unit">≤</span>
                      <input v-model.number="th.max" type="number" class="ie-list-input w-60" :placeholder="i === scoring.thresholds.length - 1 ? '∞' : 'max'" />
                      <input v-model="th.label" class="ie-list-input w-flex" placeholder="标签" />
                      <input v-model.number="th.score" type="number" class="ie-list-input w-60" placeholder="分值" />
                      <button class="ie-btn-icon-del" @click="scoring.thresholds.splice(i, 1)"><Trash2 :size="12" /></button>
                    </div>
                  </div>
                  <button class="ie-link-btn" @click="scoring.thresholds.push({ max: null, label: '', score: 0 })"><Plus :size="12" /> 添加区间</button>
                </template>

                <!-- FORMULA -->
                <template v-if="scoring.mode === 'FORMULA'">
                  <div class="ie-fld">
                    <label>公式类型</label>
                    <select v-model="scoring.formulaType" class="ie-select">
                      <option value="ratio">比值 (actual / standard × max)</option>
                      <option value="difference">差值 (max - |actual - standard|)</option>
                      <option value="percentage">百分比 (actual / total × max)</option>
                      <option value="compliance_rate">达标率 (compliant / total × max)</option>
                    </select>
                  </div>
                  <div class="ie-row-2">
                    <div class="ie-fld"><label>最高分</label><input v-model.number="scoring.formulaMaxScore" type="number" :min="1" /></div>
                    <div class="ie-fld"><label>最低分</label><input v-model.number="scoring.formulaMinScore" type="number" :min="0" /></div>
                  </div>
                  <label class="ie-fld-label">输入参数</label>
                  <div class="ie-list">
                    <div v-for="(inp, i) in scoring.formulaInputs" :key="i" class="ie-list-row">
                      <input v-model="inp.key" class="ie-list-input w-flex" placeholder="key" />
                      <input v-model="inp.label" class="ie-list-input w-flex" placeholder="标签" />
                      <button class="ie-btn-icon-del" @click="scoring.formulaInputs.splice(i, 1)"><Trash2 :size="12" /></button>
                    </div>
                  </div>
                  <button class="ie-link-btn" @click="scoring.formulaInputs.push({ key: '', label: '' })"><Plus :size="12" /> 添加参数</button>
                </template>
              </div>
            </div>
            <div class="ie-dialog-foot">
              <button class="ie-btn-ghost" @click="showScoringDialog = false">关闭</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
/* ═══════ Root ═══════ */
.ie-root { display:flex; flex-direction:column; height:100%; background:#f8f9fb; border-left:1px solid #e8ecf0; }

/* ═══════ Header ═══════ */
.ie-header { display:flex; align-items:center; justify-content:space-between; padding:10px 14px; background:#fff; border-bottom:1px solid #e8ecf0; }
.ie-header-left { display:flex; align-items:center; gap:8px; }
.ie-header-right { display:flex; align-items:center; gap:6px; }

.ie-badge { display:inline-flex; align-items:center; gap:4px; padding:3px 8px; border-radius:5px; font-size:11px; font-weight:600; border:1px solid; }
.ie-code { font-family:monospace; font-size:11px; color:#8c95a3; }

.ie-btn-primary { display:inline-flex; align-items:center; gap:4px; padding:5px 12px; background:#1a6dff; color:#fff; border:none; border-radius:6px; font-size:12px; font-weight:500; cursor:pointer; transition:background 0.15s; }
.ie-btn-primary:hover { background:#1558d6; }
.ie-btn-ghost { padding:5px 12px; background:none; border:1px solid #dce1e8; border-radius:6px; font-size:12px; color:#5a6474; cursor:pointer; transition:background 0.15s; }
.ie-btn-ghost:hover { background:#f4f6f9; }
.ie-btn-sm { padding:3px 10px; background:none; border:1px solid #dce1e8; border-radius:5px; font-size:11px; color:#1a6dff; cursor:pointer; font-weight:500; transition:all 0.15s; }
.ie-btn-sm:hover { background:#f0f4ff; border-color:#1a6dff40; }

/* ═══════ Scrollable body ═══════ */
.ie-body { flex:1; overflow-y:auto; padding:8px 12px; display:flex; flex-direction:column; gap:4px; }

/* ═══════ Accordion ═══════ */
.ie-acc { background:#fff; border:1px solid #e8ecf0; border-radius:8px; overflow:hidden; transition:border-color 0.15s; }
.ie-acc.open { border-color:#d0d5dd; }

.ie-acc-head { display:flex; align-items:center; gap:8px; width:100%; padding:9px 12px; background:none; border:none; cursor:pointer; text-align:left; transition:background 0.1s; }
.ie-acc-head:hover { background:#f8f9fb; }
.ie-acc-title { flex:1; font-size:12px; font-weight:600; color:#1e2a3a; }
.ie-acc-arrow { color:#8c95a3; transition:transform 0.2s; flex-shrink:0; }
.ie-acc.open .ie-acc-arrow { transform:rotate(180deg); }
.ie-acc-tag { font-size:10px; font-weight:500; padding:1px 6px; border-radius:4px; }
.ie-acc-tag.blue { color:#1a6dff; background:#eff6ff; }
.ie-acc-tag.green { color:#059669; background:#ecfdf5; }
.ie-acc-tag.orange { color:#d97706; background:#fffbeb; }
.ie-acc-count { font-size:10px; font-weight:600; color:#fff; background:#1a6dff; padding:0 5px; border-radius:8px; min-width:16px; text-align:center; line-height:16px; }

.ie-acc-body { padding:10px 12px; border-top:1px solid #f0f2f5; display:flex; flex-direction:column; gap:10px; }

/* ═══════ Form fields ═══════ */
.ie-fld { display:flex; flex-direction:column; gap:4px; }
.ie-fld label, .ie-fld-label { font-size:11px; font-weight:500; color:#5a6474; }
.ie-fld input, .ie-fld textarea { width:100%; border:1px solid #dce1e8; border-radius:6px; padding:6px 10px; font-size:12px; outline:none; transition:border-color 0.2s, box-shadow 0.2s; color:#1e2a3a; background:#fff; resize:vertical; }
.ie-fld input::placeholder, .ie-fld textarea::placeholder { color:#b8c0cc; }
.ie-fld input:focus, .ie-fld textarea:focus { border-color:#7aadff; box-shadow:0 0 0 2px rgba(26,109,255,0.08); }
.ie-req { color:#ef4444; }
.ie-muted { font-size:11px; color:#8c95a3; text-align:center; padding:8px 0; }

.ie-select { width:100%; border:1px solid #dce1e8; border-radius:6px; padding:6px 10px; font-size:12px; outline:none; color:#1e2a3a; background:#fff; cursor:pointer; appearance:none;
  background-image:url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%239ca3af' stroke-width='2'%3E%3Cpath d='M6 9l6 6 6-6'/%3E%3C/svg%3E");
  background-repeat:no-repeat; background-position:right 8px center; padding-right:28px; }
.ie-select:focus { border-color:#7aadff; box-shadow:0 0 0 2px rgba(26,109,255,0.08); }
.ie-select-sm { padding:4px 8px; font-size:11px; }

.ie-row-2 { display:grid; grid-template-columns:1fr 1fr; gap:8px; }
.ie-unit { font-size:11px; color:#8c95a3; flex-shrink:0; }

.ie-desc-box { display:flex; align-items:flex-start; gap:5px; font-size:11px; color:#5a6474; padding:6px 8px; background:#f8f9fb; border-radius:6px; line-height:1.4; }

/* ═══════ Response set preview ═══════ */
.ie-rs-preview { border:1px solid #f0f2f5; border-radius:6px; overflow:hidden; }
.ie-rs-row { display:flex; align-items:center; justify-content:space-between; padding:6px 10px; }
.ie-rs-row + .ie-rs-row { border-top:1px solid #f0f2f5; }
.ie-rs-row:hover { background:#f8f9fb; }
.ie-rs-label { display:flex; align-items:center; gap:6px; font-size:12px; color:#374151; }
.ie-rs-dot { width:7px; height:7px; border-radius:50%; flex-shrink:0; }
.ie-rs-flag { font-size:9px; padding:0 4px; border-radius:3px; background:#fef3c7; color:#92400e; font-weight:500; }
.ie-rs-score { font-size:11px; font-weight:600; color:#1a6dff; padding:1px 6px; background:#eff6ff; border-radius:3px; }

/* ═══════ Scoring summary row ═══════ */
.ie-scoring-row { display:flex; align-items:center; justify-content:space-between; gap:8px; }
.ie-scoring-summary { font-size:12px; color:#374151; font-family:monospace; background:#f8f9fb; padding:4px 8px; border-radius:4px; flex:1; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }

/* ═══════ Validation rules ═══════ */
.ie-rule { border:1px solid #e8ecf0; border-radius:6px; overflow:hidden; }
.ie-rule-top { display:flex; align-items:center; gap:6px; padding:6px 8px; background:#f8f9fb; border-bottom:1px solid #f0f2f5; }
.ie-rule-num { display:inline-flex; align-items:center; justify-content:center; width:18px; height:18px; border-radius:50%; background:#dbeafe; color:#1d4ed8; font-size:10px; font-weight:700; flex-shrink:0; }
.ie-rule-body { padding:8px; display:flex; flex-direction:column; gap:6px; }

.ie-btn-icon-del { display:inline-flex; align-items:center; justify-content:center; width:22px; height:22px; border-radius:4px; border:none; background:none; color:#b8c0cc; cursor:pointer; transition:all 0.12s; flex-shrink:0; }
.ie-btn-icon-del:hover { background:#fef2f2; color:#ef4444; }

.ie-link-btn { display:inline-flex; align-items:center; gap:3px; font-size:11px; font-weight:500; color:#1a6dff; background:none; border:none; cursor:pointer; padding:4px 0; transition:color 0.12s; }
.ie-link-btn:hover { color:#1558d6; }

/* ═══════ Modal Dialog ═══════ */
.ie-mask { position:fixed; inset:0; z-index:1000; display:flex; align-items:center; justify-content:center; background:rgba(15,23,42,0.4); backdrop-filter:blur(2px); }
.ie-dialog { width:560px; max-height:80vh; background:#fff; border-radius:14px; box-shadow:0 24px 64px rgba(0,0,0,0.18); display:flex; flex-direction:column; overflow:hidden; }
.ie-dialog-head { display:flex; align-items:center; justify-content:space-between; padding:18px 24px 0; }
.ie-dialog-head h3 { font-size:15px; font-weight:600; color:#1e2a3a; margin:0; }
.ie-dialog-close { background:none; border:none; font-size:22px; color:#b8c0cc; cursor:pointer; padding:0 4px; line-height:1; }
.ie-dialog-close:hover { color:#5a6474; }
.ie-dialog-body { flex:1; overflow-y:auto; padding:16px 24px; display:flex; flex-direction:column; gap:14px; }
.ie-dialog-foot { display:flex; justify-content:flex-end; gap:10px; padding:12px 24px; border-top:1px solid #f0f2f5; }

.ie-modal-enter-active { transition:all 0.2s ease-out; }
.ie-modal-leave-active { transition:all 0.15s ease-in; }
.ie-modal-enter-from { opacity:0; }
.ie-modal-enter-from .ie-dialog { transform:translateY(12px) scale(0.97); }
.ie-modal-leave-to { opacity:0; }
.ie-modal-leave-to .ie-dialog { transform:translateY(-8px) scale(0.98); }

/* ═══════ Mode grid ═══════ */
.ie-mode-grid { display:flex; flex-wrap:wrap; gap:4px; }
.ie-mode-chip { padding:4px 10px; font-size:11px; font-weight:500; color:#5a6474; background:#f4f6f9; border:1px solid #e8ecf0; border-radius:5px; cursor:pointer; transition:all 0.15s; }
.ie-mode-chip:hover { background:#e8ecf0; color:#1e2a3a; }
.ie-mode-chip.active { background:#eff6ff; border-color:#93c5fd; color:#1a6dff; font-weight:600; }

/* ═══════ Params area ═══════ */
.ie-params { display:flex; flex-direction:column; gap:12px; }
.ie-divider { height:1px; background:#f0f2f5; }
.ie-alert { display:flex; align-items:center; gap:5px; font-size:11px; color:#dc2626; padding:6px 8px; background:#fef2f2; border-radius:5px; border:1px solid #fecaca; }
.ie-calc { font-size:11px; color:#5a6474; padding:5px 8px; background:#f0fdf4; border-radius:5px; border:1px solid #dcfce7; }
.ie-calc strong { color:#16a34a; }
.ie-weight-tag { font-size:10px; font-weight:600; padding:1px 6px; border-radius:3px; }
.ie-weight-tag.ok { color:#16a34a; background:#f0fdf4; }
.ie-weight-tag.bad { color:#ef4444; background:#fef2f2; }

/* ═══════ List controls ═══════ */
.ie-list-head { display:flex; align-items:center; justify-content:space-between; gap:8px; }
.ie-list-head label { font-size:11px; font-weight:500; color:#5a6474; }
.ie-presets { display:flex; gap:3px; }
.ie-presets button { font-size:10px; padding:2px 7px; border-radius:4px; background:#f4f6f9; color:#5a6474; border:1px solid #e8ecf0; cursor:pointer; transition:all 0.12s; }
.ie-presets button:hover { background:#e8ecf0; color:#1e2a3a; }

.ie-list { display:flex; flex-direction:column; gap:3px; }
.ie-list-row { display:flex; align-items:center; gap:5px; }
.ie-list-input { border:1px solid #dce1e8; border-radius:5px; padding:4px 8px; font-size:11px; outline:none; color:#1e2a3a; transition:border-color 0.15s; }
.ie-list-input:focus { border-color:#7aadff; box-shadow:0 0 0 2px rgba(26,109,255,0.06); }
.ie-list-input.w-flex { flex:1; min-width:0; }
.ie-list-input.w-60 { width:60px; flex-shrink:0; }
.ie-list-input.w-50 { width:50px; flex-shrink:0; }
.ie-list-input-full { width:100%; border:1px solid #dce1e8; border-radius:5px; padding:4px 8px; font-size:11px; outline:none; color:#1e2a3a; margin-top:3px; }
.ie-list-input-full:focus { border-color:#7aadff; }

.ie-opt-card { padding:6px; border:1px solid #f0f2f5; border-radius:6px; background:#fafbfc; }
.ie-opt-top { display:flex; align-items:center; gap:5px; }
</style>
