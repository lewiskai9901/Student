<script setup lang="ts">
import type { LongId } from '@/types/common'
import { ref, watch, computed, reactive, nextTick, onMounted, onUnmounted } from 'vue'
import {
  Save, Plus, Trash2,
  BarChart3, Clipboard, Info, ShieldCheck, Zap,
  ChevronDown, ChevronUp, Keyboard,
} from 'lucide-vue-next'
import {
  ItemTypeConfig, ItemTypeGroups, type ItemType,
  ScoringModeConfig, type ScoringMode,
} from '@/types/insp/enums'
import type { TemplateItem, ResponseSet, ResponseSetOption } from '@/types/insp/template'
import { ElMessage, ElMessageBox } from 'element-plus'
import { responseSetApi } from '@/api/inspection/responseSet'
import { useKbdHint } from '@/composables/useKbdHint'
import { eventTypeApi } from '@/api/event'
import type { EventType } from '@/types/event'
import ConditionBuilder from '@/components/insp/ConditionBuilder.vue'
// 架构 E: CorrectiveOverrideEditor 已废弃, 整改规则上提至项目级
// import CorrectiveOverrideEditor from './CorrectiveOverrideEditor.vue'

const props = defineProps<{
  item: TemplateItem
  responseSets: ResponseSet[]
  allItems?: TemplateItem[]
  /** 2026-05-24 Bug#1: 模板 PUBLISHED 时禁用编辑 + 隐藏保存按钮 */
  readonly?: boolean
}>()

const emit = defineEmits<{
  save: [data: Partial<TemplateItem>]
  cancel: []
}>()

// ==================== Refs ====================
const itemNameInput = ref<HTMLInputElement | null>(null)
const availableEventTypes = ref<EventType[]>([])
const eventTypesLoading = ref(false)
const eventTypesError = ref(false)

// Load event types for association dropdown (标准 loading/error 模式)
async function loadEventTypes() {
  eventTypesLoading.value = true
  eventTypesError.value = false
  try {
    availableEventTypes.value = (await eventTypeApi.list()) as any || []
  } catch {
    availableEventTypes.value = []
    eventTypesError.value = true
  } finally {
    eventTypesLoading.value = false
  }
}

// ==================== Tab State ====================
const activeTab = ref<'scoring' | 'validation' | 'condition'>('scoring')

// ==================== A+ 优化: 评分模式分组 + 折叠 ====================
// 90% 用户只用前 3 个 (PASS_FAIL/DEDUCTION/ADDITION), 其它折叠到"更多"
const COMMON_MODES: ScoringMode[] = ['PASS_FAIL', 'DEDUCTION', 'ADDITION']
// 仅使用 ScoringMode 类型中真实存在的枚举值, 与参数渲染区 (ie-params) 一一对应
const ADVANCED_MODES_BY_GROUP: Record<string, ScoringMode[]> = {
  '直接打分类': ['DIRECT', 'LEVEL', 'SCORE_TABLE'],
  '量表 / 评级': ['CUMULATIVE', 'TIERED_DEDUCTION', 'RATING_SCALE', 'WEIGHTED_MULTI'],
  '高级算法': ['RISK_MATRIX', 'THRESHOLD', 'FORMULA'],
}
const showAdvancedModes = ref(false)
// 如果当前模式是高级模式, 自动展开
watch(() => activeTab.value, () => {
  // tab change reset
})

// ==================== A+ 优化: 全局键盘快捷键 ====================
function onItemEditorKey(e: KeyboardEvent) {
  const t = e.target as HTMLElement
  const tag = t?.tagName
  if (tag === 'INPUT' || tag === 'TEXTAREA' || t?.isContentEditable) return
  if (e.metaKey || e.ctrlKey || e.altKey) return
  // Tab 切 stage (1=评分配置 / 2=验证规则 / 3=条件逻辑)
  if (e.key === '1') { activeTab.value = 'scoring'; e.preventDefault(); return }
  if (e.key === '2') { activeTab.value = 'validation'; e.preventDefault(); return }
  if (e.key === '3') { activeTab.value = 'condition'; e.preventDefault(); return }
  // 数字键 4-9 / 0 / - / = 快捷选评分模式
  if (activeTab.value !== 'scoring') return
  const allModes: ScoringMode[] = [
    ...COMMON_MODES,
    ...Object.values(ADVANCED_MODES_BY_GROUP).flat(),
  ]
  const numKey = ['4','5','6','7','8','9','0','-','='].indexOf(e.key)
  if (numKey >= 0 && numKey + 3 < allModes.length) {
    const target = allModes[numKey + 3]
    if (target && allModes.includes(target)) {
      scoring.mode = target as any
      // 选高级模式时自动展开
      const isAdvanced = !COMMON_MODES.includes(target)
      if (isAdvanced) showAdvancedModes.value = true
      e.preventDefault()
    }
  }
}

// J7: composable
const { showKbdHint, dismissKbdHint } = useKbdHint('insp_ie_kbd_hint_dismissed')

onMounted(() => {
  window.addEventListener('keydown', onItemEditorKey)
  loadEventTypes()
})
onUnmounted(() => window.removeEventListener('keydown', onItemEditorKey))

// ==================== Form State ====================

const form = ref({
  itemName: '',
  description: '',
  itemType: 'TEXT' as ItemType,
  responseSetId: null as LongId | null,
  helpContent: '',
  validationRules: '',
  scoringConfig: '',
  conditionLogic: '',
  inputMode: 'INLINE' as 'INLINE' | 'EVENT_STREAM',
  eventTypeCode: undefined as string | undefined,
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
/** WEIGHTED_MULTI 子模式: 后端 WeightedMultiNormalizer 按 dim.mode 分发 sub-normalizer.
 *  DIRECT = 数值 slider (0..maxScore, 走 DirectNormalizer); LEVEL = 等级标签; PASS_FAIL = 通过/不通过. */
type DimMode = 'DIRECT' | 'LEVEL' | 'PASS_FAIL'
interface Dimension { key: string; label: string; weight: number; maxScore: number; mode: DimMode }
interface ProbImpact { label: string; value: number }
/** THRESHOLD 档: 后端 ItemScoreEvaluator.threshold 读 cfg.thresholds[{upTo, score}].
 *  upTo=null 为兜底档 (超过所有上限时命中). UI/序列化字段名统一为 upTo. */
interface ThresholdItem { upTo: number | null; label: string; score: number }
interface FormulaInput { key: string; label: string }
/** RISK_MATRIX 等级 → severity 映射项. 后端 RiskMatrixNormalizer 读 cfg.levelToSeverity (0..1). */
interface LevelSeverity { level: string; severity: number }

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
  /** RISK_MATRIX 矩阵: matrix[probIdx][impactIdx] = level 字符串 (L/M/H/VH). 行=可能性, 列=影响. */
  matrix: string[][]
  /** RISK_MATRIX 等级→severity 映射 (后端 cfg.levelToSeverity). */
  levelToSeverity: LevelSeverity[]
  /** RISK_MATRIX maxScore (扣分语义: severity × maxScore). */
  riskMaxScore: number
  unit: string
  thresholds: ThresholdItem[]
  formulaType: string
  formulaInputs: FormulaInput[]
  /** FORMULA 表达式 (后端 cfg.formula, 绑变量 value/score/response). */
  formula: string
  formulaMaxScore: number
  formulaMinScore: number
}

/** RISK_MATRIX 默认等级集 (与 RiskMatrixNormalizer DEFAULT_LEVEL_MAP 对齐). */
const RISK_LEVELS = ['L', 'M', 'H', 'VH'] as const
const DEFAULT_LEVEL_SEVERITY: LevelSeverity[] = [
  { level: 'L', severity: 0 },
  { level: 'M', severity: 0.4 },
  { level: 'H', severity: 0.75 },
  { level: 'VH', severity: 1 },
]

/**
 * 按 probabilities×impacts 维度生成默认矩阵.
 * 行=可能性 (低→高), 列=影响 (低→高). level 按"行索引+列索引"归一化到 L/M/H/VH 四级,
 * 左上角(低概率低影响)=L, 右下角(高概率高影响)=VH. 已有 oldMatrix 时尽量保留对应格子.
 */
function buildDefaultMatrix(rows: number, cols: number, oldMatrix?: string[][]): string[][] {
  const result: string[][] = []
  for (let r = 0; r < rows; r++) {
    const row: string[] = []
    for (let c = 0; c < cols; c++) {
      const existing = oldMatrix?.[r]?.[c]
      if (existing && (RISK_LEVELS as readonly string[]).includes(existing)) {
        row.push(existing)
        continue
      }
      // 归一化 (r/(rows-1) + c/(cols-1)) / 2 ∈ [0,1] → 四级
      const rp = rows > 1 ? r / (rows - 1) : 0
      const cp = cols > 1 ? c / (cols - 1) : 0
      const t = (rp + cp) / 2
      const idx = t >= 0.75 ? 3 : t >= 0.5 ? 2 : t >= 0.25 ? 1 : 0
      row.push(RISK_LEVELS[idx])
    }
    result.push(row)
  }
  return result
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
    { key: 'dim1', label: '维度1', weight: 50, maxScore: 100, mode: 'DIRECT' },
    { key: 'dim2', label: '维度2', weight: 50, maxScore: 100, mode: 'DIRECT' },
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
  matrix: buildDefaultMatrix(5, 5),
  levelToSeverity: DEFAULT_LEVEL_SEVERITY.map(x => ({ ...x })),
  riskMaxScore: 20,
  unit: '',
  thresholds: [
    { upTo: 25, label: '合格', score: 0 },
    { upTo: 30, label: '注意', score: -5 },
    { upTo: null, label: '不合格', score: -10 },
  ],
  formulaType: 'ratio',
  formulaInputs: [
    { key: 'actual', label: '实测值' },
    { key: 'standard', label: '标准值' },
  ],
  formula: '',
  formulaMaxScore: 10,
  formulaMinScore: 0,
})

// 整改规则按本题量表配置时所需的离散选项 (LEVEL/SCORE_TABLE 模式)
const itemDiscreteOptions = computed<string[]>(() => {
  switch (scoring.mode) {
    case 'LEVEL':         return scoring.levels.map(l => l.label).filter(Boolean)
    case 'SCORE_TABLE':   return scoring.options.map(o => o.label).filter(Boolean)
    case 'TIERED_DEDUCTION': return scoring.tiers.map(t => t.label).filter(Boolean)
    case 'THRESHOLD':     return scoring.thresholds.map(t => t.label).filter(Boolean)
    default: return []
  }
})

// RISK_MATRIX 模式的等级列表 (取自 levelToSeverity 配置, 回退默认四级)
const itemRiskLevels = computed<string[]>(() => {
  if (scoring.mode !== 'RISK_MATRIX') return []
  const cfg = scoring.levelToSeverity.map(x => x.level).filter(Boolean)
  return cfg.length > 0 ? cfg : [...RISK_LEVELS]
})

// 编辑期: 可能性/影响数量变化时同步重建矩阵 (保留已编辑格子)
watch(
  () => scoring.mode === 'RISK_MATRIX'
    ? `${scoring.probabilities.length}x${scoring.impacts.length}`
    : '',
  (sig) => {
    if (!sig) return
    const needRows = scoring.probabilities.length
    const needCols = scoring.impacts.length
    const sizeOk = scoring.matrix.length === needRows && scoring.matrix.every(r => r.length === needCols)
    if (!sizeOk) scoring.matrix = buildDefaultMatrix(needRows, needCols, scoring.matrix)
  }
)

// 整改规则按本题量表配置时所需的"满分"参考值 (按 scoringMode 取对应字段)
const itemMaxScoreForCorrective = computed(() => {
  switch (scoring.mode) {
    case 'RATING_SCALE':      return scoring.maxStars || 5
    case 'DIRECT':            return scoring.maxScore || 10
    case 'DEDUCTION':         return Math.abs(scoring.maxDeduction) || 10
    case 'WEIGHTED_MULTI':    return scoring.weightedMultiMaxScore || 10
    case 'FORMULA':           return scoring.formulaMaxScore || 10
    case 'CUMULATIVE':        return scoring.maxCount || 10
    case 'ADDITION':          return scoring.maxBonus || 5
    default:                  return 10
  }
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
        if (Array.isArray(c.dimensions) && c.dimensions.length > 0) {
          scoring.dimensions = c.dimensions.map((d: any, i: number) => ({
            key: d.key ?? `dim${i + 1}`,
            label: d.label ?? '',
            weight: d.weight ?? 0,
            maxScore: d.maxScore ?? 100,
            mode: (['DIRECT', 'LEVEL', 'PASS_FAIL'].includes(d.mode) ? d.mode : 'DIRECT') as DimMode,
          }))
        }
        break
      case 'RISK_MATRIX':
        if (Array.isArray(c.probabilities) && c.probabilities.length > 0) scoring.probabilities = c.probabilities
        if (Array.isArray(c.impacts) && c.impacts.length > 0) scoring.impacts = c.impacts
        scoring.riskMaxScore = c.maxScore ?? 20
        // 矩阵: 后端结构 matrix[r][c] = {level} 或 字符串; 统一解析为 string[][]
        if (Array.isArray(c.matrix) && c.matrix.length > 0) {
          scoring.matrix = c.matrix.map((row: any) =>
            Array.isArray(row) ? row.map((cell: any) =>
              typeof cell === 'string' ? cell : (cell?.level ?? 'L')) : [])
        } else {
          scoring.matrix = buildDefaultMatrix(scoring.probabilities.length, scoring.impacts.length)
        }
        // levelToSeverity: 后端 {L:0,M:0.4,...} → UI 数组
        if (c.levelToSeverity && typeof c.levelToSeverity === 'object') {
          scoring.levelToSeverity = Object.entries(c.levelToSeverity).map(([level, severity]) => ({
            level, severity: Number(severity),
          }))
        } else {
          scoring.levelToSeverity = DEFAULT_LEVEL_SEVERITY.map(x => ({ ...x }))
        }
        break
      case 'THRESHOLD':
        scoring.unit = c.unit ?? ''
        if (Array.isArray(c.thresholds) && c.thresholds.length > 0) {
          // 兼容旧字段名 max → upTo
          scoring.thresholds = c.thresholds.map((t: any) => ({
            upTo: t.upTo ?? t.max ?? null,
            label: t.label ?? '',
            score: t.score ?? 0,
          }))
        }
        break
      case 'FORMULA':
        scoring.formulaType = c.formulaType ?? 'ratio'
        if (Array.isArray(c.inputs) && c.inputs.length > 0) scoring.formulaInputs = c.inputs
        scoring.formula = c.formula ?? ''
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
      // 后端 WeightedMultiNormalizer 读 dimensions[{key, weight, mode}]; weight 比例无关 (内部 Σ(w×s)/Σw 归一)
      obj.dimensions = scoring.dimensions.map(d => ({
        key: d.key,
        label: d.label,
        weight: d.weight,
        maxScore: d.maxScore,
        mode: d.mode,
      }))
      break
    case 'RISK_MATRIX':
      obj.probabilities = scoring.probabilities
      obj.impacts = scoring.impacts
      obj.maxScore = scoring.riskMaxScore
      // 后端 RiskMatrixNormalizer 读 matrix[probIdx][impactIdx] = {level} + levelToSeverity{level: 0..1}
      obj.matrix = scoring.matrix.map(row => row.map(level => ({ level })))
      obj.levelToSeverity = scoring.levelToSeverity.reduce((acc, x) => {
        acc[x.level] = x.severity
        return acc
      }, {} as Record<string, number>)
      break
    case 'THRESHOLD':
      obj.unit = scoring.unit
      // 后端 ItemScoreEvaluator.threshold 读 thresholds[{upTo, score}], upTo=null 为兜底档
      obj.thresholds = scoring.thresholds.map(t => ({
        upTo: ((t.upTo as any) === '' || t.upTo === undefined || t.upTo === null || (typeof t.upTo === 'number' && isNaN(t.upTo))) ? null : t.upTo,
        label: t.label,
        score: t.score,
      }))
      break
    case 'FORMULA':
      obj.formulaType = scoring.formulaType
      obj.inputs = scoring.formulaInputs
      // 后端 ItemScoreEvaluator.formula 读 cfg.formula 表达式 (绑变量 value/score/response)
      obj.formula = scoring.formula
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

// Scoring mode > allowed validation rule types
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

// Capture type > allowed validation rule types
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
      obj.value = r.value || 0
    } else if (r.type === 'pattern') {
      obj.value = r.value || ''
    }
    if (r.message) obj.message = r.message
    return obj
  }))
}

function addRule() {
  validationRulesList.value.push({ type: 'required', message: '' })
  activeTab.value = 'validation'
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
  scoring.dimensions = [{ key: 'dim1', label: '维度1', weight: 50, maxScore: 100, mode: 'DIRECT' }, { key: 'dim2', label: '维度2', weight: 50, maxScore: 100, mode: 'DIRECT' }]
  scoring.probabilities = [{ label: '罕见', value: 1 }, { label: '不太可能', value: 2 }, { label: '可能', value: 3 }, { label: '很可能', value: 4 }, { label: '几乎确定', value: 5 }]
  scoring.impacts = [{ label: '微小', value: 1 }, { label: '较小', value: 2 }, { label: '中等', value: 3 }, { label: '较大', value: 4 }, { label: '严重', value: 5 }]
  scoring.matrix = buildDefaultMatrix(5, 5)
  scoring.levelToSeverity = DEFAULT_LEVEL_SEVERITY.map(x => ({ ...x }))
  scoring.riskMaxScore = 20
  scoring.unit = ''
  scoring.thresholds = [{ upTo: 25, label: '合格', score: 0 }, { upTo: 30, label: '注意', score: -5 }, { upTo: null, label: '不合格', score: -10 }]
  scoring.formulaType = 'ratio'
  scoring.formulaInputs = [{ key: 'actual', label: '实测值' }, { key: 'standard', label: '标准值' }]
  scoring.formula = ''
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
      inputMode: item.inputMode || 'INLINE',
      eventTypeCode: (item as any).eventTypeCode || undefined,
    }
    itemCategory.value = inferCategory(item)
    resetScoringDefaults()
    if (itemCategory.value === 'scored') {
      parseScoringConfig(item.scoringConfig || '{}')
    }
    parseValidationRules(item.validationRules || '[]')
    // Reset tab to scoring
    activeTab.value = 'scoring'

    // Auto-focus name input for newly added items (empty name)
    if (!item.itemName) {
      nextTick(() => {
        itemNameInput.value?.focus()
      })
    }
  }
}, { immediate: true })

async function handleSave() {
  if (!form.value.itemName.trim()) {
    ElMessage.warning('字段名称不能为空')
    return
  }
  if (itemCategory.value === 'scored') {
    // === Scored item save ===

    // #4: 保存前检测将被丢弃的不兼容验证规则, 弹确认告知用户
    const allowedRules = SCORING_MODE_ALLOWED_RULES[scoring.mode]
    if (allowedRules) {
      const dropped = validationRulesList.value.filter(r => !allowedRules.includes(r.type))
      if (dropped.length > 0) {
        const labels = dropped
          .map(r => RULE_TYPE_OPTIONS.find(o => o.value === r.type)?.label || r.type)
          .join('、')
        try {
          await ElMessageBox.confirm(
            `当前评分模式「${ScoringModeConfig[scoring.mode]?.label || scoring.mode}」不支持以下 ${dropped.length} 条验证规则：${labels}。\n继续保存将删除这些规则。`,
            '验证规则不兼容',
            { type: 'warning', confirmButtonText: '继续保存并删除', cancelButtonText: '返回修改' }
          )
        } catch {
          return // 用户取消, 不保存
        }
      }
    }

    // #5: 收集被自动修正的非法配置, 保存后 toast 告知
    const corrections: string[] = []

    // Validate scoring config constraints
    if (scoring.mode === 'PASS_FAIL' && scoring.failScore > 0) {
      corrections.push(`不通过扣分不能为正数, 已修正为 0`)
      scoring.failScore = 0
    }
    if (scoring.mode === 'DEDUCTION' && scoring.maxDeduction > 0) {
      corrections.push(`最大扣分不能为正数, 已修正为 0`)
      scoring.maxDeduction = 0
    }
    if (scoring.mode === 'DIRECT' && scoring.minScore >= scoring.maxScore) {
      scoring.maxScore = scoring.minScore + 1
      corrections.push(`最高分需大于最低分, 已修正最高分为 ${scoring.maxScore}`)
    }
    if (scoring.mode === 'LEVEL' && scoring.levels.length < 2) {
      scoring.levels = [{ label: '优', score: 10 }, { label: '差', score: 0 }]
      corrections.push(`等级评分至少需要 2 个等级, 已补全默认等级`)
    }
    if (scoring.mode === 'SCORE_TABLE' && scoring.options.length < 2) {
      scoring.options = [{ label: '优秀', description: '', score: 10 }, { label: '差', description: '', score: 0 }]
      corrections.push(`评分标准表至少需要 2 个档位, 已补全默认档位`)
    }
    if (scoring.mode === 'TIERED_DEDUCTION') {
      if (scoring.tiers.some(t => t.score > 0)) corrections.push(`分级扣分的扣分值不能为正数, 已修正`)
      scoring.tiers = scoring.tiers.map(t => ({ ...t, score: Math.min(0, t.score) }))
    }
    if (scoring.mode === 'RATING_SCALE') {
      const cs = Math.max(3, Math.min(10, scoring.maxStars))
      if (cs !== scoring.maxStars) { corrections.push(`星数需在 3~10 之间, 已修正为 ${cs}`); scoring.maxStars = cs }
      const cp = Math.max(1, scoring.scorePerStar)
      if (cp !== scoring.scorePerStar) { corrections.push(`每星分值至少为 1, 已修正为 ${cp}`); scoring.scorePerStar = cp }
    }
    if (scoring.mode === 'WEIGHTED_MULTI') {
      if (scoring.dimensions.length < 1) {
        scoring.dimensions = [{ key: 'dim1', label: '维度1', weight: 100, maxScore: 100, mode: 'DIRECT' }]
        corrections.push(`多维加权至少需要 1 个维度, 已补全默认维度`)
      }
      const weightSum = scoring.dimensions.reduce((s, d) => s + d.weight, 0)
      if (weightSum !== 100 && weightSum > 0) {
        const factor = 100 / weightSum
        scoring.dimensions = scoring.dimensions.map(d => ({ ...d, weight: Math.round(d.weight * factor) }))
        const newSum = scoring.dimensions.reduce((s, d) => s + d.weight, 0)
        if (newSum !== 100 && scoring.dimensions.length > 0)
          scoring.dimensions[scoring.dimensions.length - 1].weight += 100 - newSum
        corrections.push(`维度权重之和需为 100%, 已按比例归一化`)
      }
    }
    if (scoring.mode === 'THRESHOLD' && scoring.thresholds.length < 1) {
      scoring.thresholds = [{ upTo: null, label: '默认', score: 0 }]
      corrections.push(`区间评分至少需要 1 个区间, 已补全默认区间`)
    }
    if (scoring.mode === 'RISK_MATRIX') {
      if (scoring.probabilities.length < 2) { scoring.probabilities = [{ label: '低', value: 1 }, { label: '高', value: 5 }]; corrections.push(`可能性至少需要 2 项, 已补全`) }
      if (scoring.impacts.length < 2) { scoring.impacts = [{ label: '小', value: 1 }, { label: '大', value: 5 }]; corrections.push(`影响至少需要 2 项, 已补全`) }
      if (scoring.levelToSeverity.length < 1) { scoring.levelToSeverity = DEFAULT_LEVEL_SEVERITY.map(x => ({ ...x })); corrections.push(`风险等级映射为空, 已补全默认 L/M/H/VH`) }
    }
    if (scoring.mode === 'FORMULA' && scoring.formulaInputs.length < 1) {
      scoring.formulaInputs = [{ key: 'value', label: '值' }]
      corrections.push(`公式评分至少需要 1 个输入, 已补全`)
    }
    if (scoring.mode === 'FORMULA' && scoring.formulaMaxScore <= scoring.formulaMinScore) {
      scoring.formulaMaxScore = scoring.formulaMinScore + 1
      corrections.push(`公式最高分需大于最低分, 已修正最高分为 ${scoring.formulaMaxScore}`)
    }

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
      if (scoring.dimensions.length < 1) scoring.dimensions = [{ key: 'dim1', label: '维度1', weight: 100, maxScore: 100, mode: 'DIRECT' }]
    }
    if (scoring.mode === 'THRESHOLD') {
      const hasNullMax = scoring.thresholds.some(t => t.upTo == null)
      if (!hasNullMax) scoring.thresholds.push({ upTo: null, label: '其他', score: 0 })
    }
    if (scoring.mode === 'RISK_MATRIX') {
      scoring.probabilities = scoring.probabilities.filter(p => p.value > 0).sort((a, b) => a.value - b.value)
      scoring.impacts = scoring.impacts.filter(i => i.value > 0).sort((a, b) => a.value - b.value)
      if (scoring.probabilities.length < 2) scoring.probabilities = [{ label: '低', value: 1 }, { label: '高', value: 5 }]
      if (scoring.impacts.length < 2) scoring.impacts = [{ label: '小', value: 1 }, { label: '大', value: 5 }]
      // 矩阵尺寸须与 prob×impact 一致; 不匹配则重建 (尽量保留已编辑格子)
      const needRows = scoring.probabilities.length
      const needCols = scoring.impacts.length
      const sizeOk = scoring.matrix.length === needRows && scoring.matrix.every(r => r.length === needCols)
      if (!sizeOk) scoring.matrix = buildDefaultMatrix(needRows, needCols, scoring.matrix)
    }

    // Auto-remove validation rules not applicable to current scoring mode (已在上方确认)
    if (allowedRules) {
      validationRulesList.value = validationRulesList.value.filter(r => allowedRules.includes(r.type))
    }

    // #5: 告知自动修正结果
    if (corrections.length > 0) {
      ElMessage.warning({
        message: `已自动修正 ${corrections.length} 处配置：` + corrections.join('；'),
        duration: 5000,
      })
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
      itemType: form.value.itemType,
      description: form.value.description || undefined,
      responseSetId: form.value.responseSetId,
      helpContent: form.value.helpContent || undefined,
      validationRules: validationJson || undefined,
      scoringConfig: scoringJson || undefined,
      conditionLogic: form.value.conditionLogic || undefined,
      isScored: true,
      inputMode: form.value.inputMode || 'INLINE',
      eventTypeCode: form.value.eventTypeCode || undefined,
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
      inputMode: form.value.inputMode || 'INLINE',
      eventTypeCode: form.value.eventTypeCode || undefined,
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
const rsOptionsError = ref(false)

async function loadRsOptions(id: LongId | null) {
  if (!id) {
    rsOptions.value = []
    rsOptionsError.value = false
    return
  }
  rsOptionsLoading.value = true
  rsOptionsError.value = false
  try {
    rsOptions.value = await responseSetApi.getOptions(id)
  } catch {
    rsOptions.value = []
    rsOptionsError.value = true
  } finally {
    rsOptionsLoading.value = false
  }
}

watch(() => form.value.responseSetId, (newId) => { loadRsOptions(newId) }, { immediate: true })

// Whether scoring is driven by response set
const scoringFromResponseSet = computed(() =>
  itemCategory.value === 'scored' && needsResponseSet.value && !!form.value.responseSetId && rsOptions.value.length > 0
)
</script>

<template>
  <div class="ie-root" :class="{ 'ie-root--readonly': readonly }">
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
        <span v-if="readonly" class="ie-readonly-hint">只读 · 模板已发布</span>
        <button class="ie-btn-ghost" @click="emit('cancel')">{{ readonly ? '关闭' : '取消' }}</button>
        <button v-if="!readonly" class="ie-btn-primary" @click="handleSave">
          <Save :size="12" /> 保存
        </button>
      </div>
    </div>

    <!-- ═══════ Readonly overlay (V20260524: published 模板时禁用全部输入) ═══════ -->
    <div v-if="readonly" class="ie-readonly-shield" aria-hidden="true"></div>

    <!-- ═══════ Scrollable Body ═══════ -->
    <div class="ie-body">

      <!-- ── 基本信息 ── -->
      <div class="ie-section">
        <div class="ie-fld">
          <label>名称 <span class="ie-req">*</span></label>
          <input ref="itemNameInput" v-model="form.itemName" placeholder="检查项名称" />
        </div>
        <div class="ie-row-2">
          <div class="ie-fld">
            <label>描述</label>
            <input v-model="form.description" placeholder="可选" />
          </div>
          <div class="ie-fld">
            <label>帮助提示</label>
            <input v-model="form.helpContent" placeholder="检查员看到的提示" />
          </div>
        </div>
      </div>

      <!-- ── 录入模式（所有评分项） ── -->
      <div v-if="itemCategory === 'scored'" class="ie-section">
        <div class="ie-fld">
          <label>录入模式</label>
          <el-radio-group v-model="form.inputMode" size="small">
            <el-radio-button value="INLINE">结构化检查</el-radio-button>
            <el-radio-button value="EVENT_STREAM">巡查快速记录</el-radio-button>
          </el-radio-group>
          <div v-if="form.inputMode === 'EVENT_STREAM'" class="ie-hint">
            检查员搜索目标后快速打分，适合随机抽查、巡查等场景
          </div>
        </div>
      </div>

      <!-- ── 事件关联 ── -->
      <div class="ie-section">
        <div class="ie-fld">
          <label>事件关联</label>
          <select v-model="form.eventTypeCode" class="ie-event-select" :disabled="eventTypesError">
            <option :value="undefined">{{ eventTypesLoading ? '加载中…' : '不关联事件' }}</option>
            <option v-for="et in availableEventTypes" :key="et.typeCode" :value="et.typeCode">
              {{ et.categoryName }} / {{ et.typeName }}
            </option>
          </select>
          <button v-if="eventTypesError" type="button" class="ie-inline-retry" @click="loadEventTypes">
            事件类型加载失败，点击重试
          </button>
          <div v-else class="ie-hint-sm">检查此项不合格时自动生成对应事件</div>
        </div>
      </div>

      <!-- ── Tab 导航 ── -->
      <div class="ie-tabs">
        <!-- 评分项: 评分配置 tab -->
        <template v-if="itemCategory === 'scored'">
          <button
            :class="['ie-tab', activeTab === 'scoring' && 'active']"
            @click="activeTab = 'scoring'"
          >
            <BarChart3 :size="12" />
            评分配置
            <span v-if="!scoringFromResponseSet" class="ie-tab-tag">{{ ScoringModeConfig[scoring.mode]?.label }}</span>
          </button>
        </template>
        <!-- 采集项: 输入类型 tab（复用 scoring 位置）-->
        <template v-if="itemCategory === 'capture'">
          <button
            :class="['ie-tab', activeTab === 'scoring' && 'active']"
            @click="activeTab = 'scoring'"
          >
            <Clipboard :size="12" />
            输入类型
            <span class="ie-tab-tag green">{{ ItemTypeConfig[form.itemType]?.label }}</span>
          </button>
        </template>
        <button
          :class="['ie-tab', activeTab === 'validation' && 'active']"
          @click="activeTab = 'validation'"
        >
          <ShieldCheck :size="12" />
          验证规则
          <span v-if="validationRulesList.length > 0" class="ie-tab-count">{{ validationRulesList.length }}</span>
        </button>
        <button
          :class="['ie-tab', activeTab === 'condition' && 'active']"
          @click="activeTab = 'condition'"
        >
          <Zap :size="12" />
          条件逻辑
          <span v-if="form.conditionLogic" class="ie-tab-dot" />
        </button>
      </div>

      <!-- ── Tab 内容 ── -->
      <div class="ie-tab-content">

        <!-- Tab: 评分配置（评分项）-->
        <div v-if="activeTab === 'scoring' && itemCategory === 'scored'" class="ie-tab-pane">
          <!-- Response set binding -->
          <template v-if="needsResponseSet">
            <div class="ie-fld">
              <label>选项集</label>
              <select v-model="form.responseSetId" class="ie-select">
                <option :value="null">-- 不使用选项集 --</option>
                <option v-for="rs in responseSets" :key="rs.id" :value="rs.id">{{ rs.setName }}</option>
              </select>
            </div>
            <div v-if="scoringFromResponseSet" class="ie-rs-preview">
              <div v-for="opt in rsOptions" :key="opt.id" class="ie-rs-row">
                <div class="ie-rs-label">
                  <span v-if="opt.optionColor" class="ie-rs-dot" :style="{ background: opt.optionColor }" />
                  {{ opt.optionLabel }}
                  <span v-if="opt.isFlagged" class="ie-rs-flag">标记</span>
                </div>
                <span class="ie-rs-score" :class="{ 'ie-rs-score--flagged': opt.isFlagged }">{{ opt.score ?? 0 }}分</span>
              </div>
            </div>
            <!-- #7/#10: 选项集加载中 / 失败 / 空 三态分明 -->
            <div v-else-if="form.responseSetId && rsOptionsLoading" class="ie-muted">加载选项中…</div>
            <div v-else-if="form.responseSetId && rsOptionsError" class="ie-rs-state ie-rs-state--error">
              <span>选项集加载失败，已临时退回手动评分模式</span>
              <button type="button" class="ie-inline-retry" @click="loadRsOptions(form.responseSetId)">重试</button>
            </div>
            <div v-else-if="form.responseSetId && !rsOptionsLoading && rsOptions.length === 0" class="ie-rs-state">
              该选项集暂无选项，请先在选项集管理中添加选项，或改用手动评分模式
            </div>
          </template>
          <!-- Scoring mode inline (when NOT driven by response set) -->
          <template v-if="!scoringFromResponseSet">
            <!-- Mode selector — 分组折叠 (A+ 优化) -->
            <div class="ie-fld">
              <label>评分模式</label>

              <!-- 常用 3 个 (始终显示) -->
              <div class="ie-mode-section">
                <div class="ie-mode-section__head">
                  <span class="ie-mode-section__label">常用</span>
                </div>
                <div class="ie-mode-grid">
                  <button v-for="(key, idx) in COMMON_MODES" :key="key"
                    :class="['ie-mode-chip', scoring.mode === key && 'active']"
                    :title="`快捷键 ${idx + 1}`"
                    @click="scoring.mode = key as any">
                    {{ ScoringModeConfig[key]?.label }}
                    <kbd class="ie-mode-kbd">{{ idx + 1 }}</kbd>
                  </button>
                </div>
              </div>

              <!-- 高级模式 (折叠) -->
              <button class="ie-mode-toggle" @click="showAdvancedModes = !showAdvancedModes">
                <ChevronDown v-if="!showAdvancedModes" :size="12" />
                <ChevronUp v-else :size="12" />
                <span>{{ showAdvancedModes ? '收起高级模式' : '更多评分模式' }}</span>
                <span class="ie-mode-toggle__count">
                  {{ Object.values(ADVANCED_MODES_BY_GROUP).flat().length }} 种
                </span>
              </button>

              <Transition name="ie-mode-expand">
                <div v-if="showAdvancedModes" class="ie-mode-advanced">
                  <div v-for="(modes, groupName) in ADVANCED_MODES_BY_GROUP" :key="groupName"
                       class="ie-mode-section">
                    <div class="ie-mode-section__head">
                      <span class="ie-mode-section__label">{{ groupName }}</span>
                    </div>
                    <div class="ie-mode-grid">
                      <button v-for="key in modes" :key="key"
                        :class="['ie-mode-chip', scoring.mode === key && 'active']"
                        @click="scoring.mode = key as any">
                        {{ ScoringModeConfig[key]?.label }}
                      </button>
                    </div>
                  </div>
                </div>
              </Transition>

              <div class="ie-desc-box"><Info :size="11" /><span>{{ ScoringModeConfig[scoring.mode]?.description }}</span></div>
            </div>

            <!-- 键盘提示条 (仅 scoring tab) -->
            <div v-if="showKbdHint" class="ie-kbd-hint">
              <Keyboard :size="11" />
              <span class="ie-kbd-hint__group">
                <kbd class="insp-kbd">1</kbd><kbd class="insp-kbd">2</kbd><kbd class="insp-kbd">3</kbd> 切 Tab
              </span>
              <span class="ie-kbd-hint__group">
                <kbd class="insp-kbd">1</kbd>~<kbd class="insp-kbd">3</kbd> 选常用模式
              </span>
              <button class="ie-kbd-hint__close" @click="dismissKbdHint" title="不再显示">×</button>
            </div>
            <!-- Mode params inline -->
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
              </template>
              <!-- LEVEL -->
              <template v-if="scoring.mode === 'LEVEL'">
                <div class="ie-list-head">
                  <label>等级列表</label>
                  <div class="ie-presets">
                    <button @click="scoring.levels = [{label:'优',score:10},{label:'良',score:7},{label:'中',score:4},{label:'差',score:0}]">优良中差</button>
                    <button @click="scoring.levels = [{label:'A',score:10},{label:'B',score:7},{label:'C',score:4},{label:'D',score:0}]">ABCD</button>
                  </div>
                </div>
                <div class="ie-list">
                  <div v-for="(lv, i) in scoring.levels" :key="i" class="ie-list-row">
                    <input v-model="lv.label" class="ie-list-input w-flex" placeholder="标签" />
                    <input v-model.number="lv.score" type="number" class="ie-list-input w-60" placeholder="分值" />
                    <button class="ie-btn-icon-del" :disabled="scoring.levels.length <= 2"
                      :title="scoring.levels.length <= 2 ? '至少需保留 2 个等级' : '删除'"
                      @click="scoring.levels.splice(i, 1)"><Trash2 :size="11" /></button>
                  </div>
                </div>
                <button class="ie-link-btn" @click="scoring.levels.push({ label: '', score: 0 })"><Plus :size="11" /> 添加</button>
              </template>
              <!-- SCORE_TABLE -->
              <template v-if="scoring.mode === 'SCORE_TABLE'">
                <div class="ie-list">
                  <div v-for="(opt, i) in scoring.options" :key="i" class="ie-list-row">
                    <input v-model="opt.label" class="ie-list-input w-flex" placeholder="档位名" />
                    <input v-model.number="opt.score" type="number" class="ie-list-input w-60" placeholder="分值" />
                    <button class="ie-btn-icon-del" :disabled="scoring.options.length <= 2"
                      :title="scoring.options.length <= 2 ? '至少需保留 2 个档位' : '删除'"
                      @click="scoring.options.splice(i, 1)"><Trash2 :size="11" /></button>
                  </div>
                </div>
                <button class="ie-link-btn" @click="scoring.options.push({ label: '', description: '', score: 0 })"><Plus :size="11" /> 添加</button>
              </template>
              <!-- CUMULATIVE -->
              <template v-if="scoring.mode === 'CUMULATIVE'">
                <div class="ie-row-2">
                  <div class="ie-fld"><label>每次分值</label><input v-model.number="scoring.scorePerCount" type="number" /></div>
                  <div class="ie-fld"><label>最大次数</label><input v-model.number="scoring.maxCount" type="number" :min="1" /></div>
                </div>
              </template>
              <!-- TIERED_DEDUCTION -->
              <template v-if="scoring.mode === 'TIERED_DEDUCTION'">
                <div class="ie-list">
                  <div v-for="(t, i) in scoring.tiers" :key="i" class="ie-list-row">
                    <input v-model="t.label" class="ie-list-input w-flex" placeholder="档位名" />
                    <input v-model.number="t.score" type="number" :max="0" class="ie-list-input w-60" placeholder="扣分" />
                    <button class="ie-btn-icon-del" :disabled="scoring.tiers.length <= 1"
                      :title="scoring.tiers.length <= 1 ? '至少需保留 1 个档位' : '删除'"
                      @click="scoring.tiers.splice(i, 1)"><Trash2 :size="11" /></button>
                  </div>
                </div>
                <button class="ie-link-btn" @click="scoring.tiers.push({ label: '', score: 0 })"><Plus :size="11" /> 添加</button>
              </template>
              <!-- RATING_SCALE -->
              <template v-if="scoring.mode === 'RATING_SCALE'">
                <div class="ie-row-2">
                  <div class="ie-fld"><label>最大星数</label><input v-model.number="scoring.maxStars" type="number" :min="3" :max="10" /></div>
                  <div class="ie-fld"><label>每星分值</label><input v-model.number="scoring.scorePerStar" type="number" :min="1" /></div>
                </div>
                <div class="ie-calc">满分 = {{ scoring.maxStars }} × {{ scoring.scorePerStar }} = <strong>{{ scoring.maxStars * scoring.scorePerStar }}</strong></div>
              </template>
              <!-- WEIGHTED_MULTI -->
              <template v-if="scoring.mode === 'WEIGHTED_MULTI'">
                <div class="ie-list">
                  <div v-for="(dim, i) in scoring.dimensions" :key="i" class="ie-list-row">
                    <input v-model="dim.label" class="ie-list-input w-flex" placeholder="名称" />
                    <select v-model="dim.mode" class="ie-list-input w-90" title="该维度评分方式">
                      <option value="DIRECT">直接打分</option>
                      <option value="LEVEL">等级</option>
                      <option value="PASS_FAIL">通过/不通过</option>
                    </select>
                    <input v-model.number="dim.weight" type="number" class="ie-list-input w-50" placeholder="权重" />
                    <span class="ie-unit">权</span>
                    <button class="ie-btn-icon-del" :disabled="scoring.dimensions.length <= 1"
                      :title="scoring.dimensions.length <= 1 ? '至少需保留 1 个维度' : '删除'"
                      @click="scoring.dimensions.splice(i, 1)"><Trash2 :size="11" /></button>
                  </div>
                </div>
                <button class="ie-link-btn" @click="scoring.dimensions.push({ key: `dim${scoring.dimensions.length+1}`, label: '', weight: 1, maxScore: 100, mode: 'DIRECT' })"><Plus :size="11" /> 添加</button>
              </template>
              <!-- THRESHOLD -->
              <template v-if="scoring.mode === 'THRESHOLD'">
                <div class="ie-fld" style="max-width:120px"><label>单位</label><input v-model="scoring.unit" placeholder="°C" /></div>
                <div class="ie-list">
                  <div v-for="(th, i) in scoring.thresholds" :key="i" class="ie-list-row">
                    <span class="ie-unit">≤</span>
                    <input v-model.number="th.upTo" type="number" class="ie-list-input w-60" :placeholder="i === scoring.thresholds.length - 1 ? '∞' : '上限'" />
                    <input v-model="th.label" class="ie-list-input w-flex" placeholder="标签" />
                    <input v-model.number="th.score" type="number" class="ie-list-input w-60" placeholder="分值" />
                    <button class="ie-btn-icon-del" :disabled="scoring.thresholds.length <= 1"
                      :title="scoring.thresholds.length <= 1 ? '至少需保留 1 个区间' : '删除'"
                      @click="scoring.thresholds.splice(i, 1)"><Trash2 :size="11" /></button>
                  </div>
                </div>
                <button class="ie-link-btn" @click="scoring.thresholds.push({ upTo: null, label: '', score: 0 })"><Plus :size="11" /> 添加</button>
              </template>
              <!-- FORMULA -->
              <template v-if="scoring.mode === 'FORMULA'">
                <div class="ie-fld">
                  <label>公式类型</label>
                  <select v-model="scoring.formulaType" class="ie-select">
                    <option value="ratio">比值</option>
                    <option value="difference">差值</option>
                    <option value="percentage">百分比</option>
                    <option value="compliance_rate">达标率</option>
                  </select>
                </div>
                <div class="ie-fld">
                  <label>计算公式</label>
                  <textarea v-model="scoring.formula" class="ie-list-input" rows="2"
                    placeholder="如 value * 2 或 (actual/standard)*100 — 变量 value=录入值" style="width:100%;resize:vertical" />
                  <span class="ie-hint">支持变量 value / score / response(均为录入数值)及 sqrt/abs/min/max/round 等函数。留空则不计分。</span>
                </div>
                <div class="ie-row-2">
                  <div class="ie-fld"><label>最高分</label><input v-model.number="scoring.formulaMaxScore" type="number" /></div>
                  <div class="ie-fld"><label>最低分</label><input v-model.number="scoring.formulaMinScore" type="number" /></div>
                </div>
              </template>
              <!-- RISK_MATRIX -->
              <template v-if="scoring.mode === 'RISK_MATRIX'">
                <label class="ie-fld-label">可能性</label>
                <div class="ie-list">
                  <div v-for="(p, i) in scoring.probabilities" :key="'p'+i" class="ie-list-row">
                    <input v-model="p.label" class="ie-list-input w-flex" placeholder="标签" />
                    <input v-model.number="p.value" type="number" class="ie-list-input w-60" />
                    <button class="ie-btn-icon-del" :disabled="scoring.probabilities.length <= 2"
                      :title="scoring.probabilities.length <= 2 ? '至少需保留 2 项可能性' : '删除'"
                      @click="scoring.probabilities.splice(i, 1)"><Trash2 :size="11" /></button>
                  </div>
                </div>
                <label class="ie-fld-label">影响</label>
                <div class="ie-list">
                  <div v-for="(imp, i) in scoring.impacts" :key="'i'+i" class="ie-list-row">
                    <input v-model="imp.label" class="ie-list-input w-flex" placeholder="标签" />
                    <input v-model.number="imp.value" type="number" class="ie-list-input w-60" />
                    <button class="ie-btn-icon-del" :disabled="scoring.impacts.length <= 2"
                      :title="scoring.impacts.length <= 2 ? '至少需保留 2 项影响' : '删除'"
                      @click="scoring.impacts.splice(i, 1)"><Trash2 :size="11" /></button>
                  </div>
                </div>
                <label class="ie-fld-label">风险矩阵（行=可能性，列=影响，选等级）</label>
                <table class="ie-matrix">
                  <thead>
                    <tr><th class="ie-matrix-corner"></th><th v-for="(imp, c) in scoring.impacts" :key="'h'+c">{{ imp.label }}</th></tr>
                  </thead>
                  <tbody>
                    <tr v-for="(p, r) in scoring.probabilities" :key="'r'+r">
                      <th>{{ p.label }}</th>
                      <td v-for="(imp, c) in scoring.impacts" :key="'c'+c">
                        <select v-if="scoring.matrix[r]" v-model="scoring.matrix[r][c]" :class="['ie-matrix-cell', 'lvl-' + scoring.matrix[r][c]]">
                          <option v-for="lv in itemRiskLevels" :key="lv" :value="lv">{{ lv }}</option>
                        </select>
                      </td>
                    </tr>
                  </tbody>
                </table>
                <label class="ie-fld-label">等级 → 严重度（0~1，越高越严重）</label>
                <div class="ie-list">
                  <div v-for="(ls, i) in scoring.levelToSeverity" :key="'ls'+i" class="ie-list-row">
                    <input v-model="ls.level" class="ie-list-input w-60" placeholder="等级" />
                    <input v-model.number="ls.severity" type="number" step="0.05" :min="0" :max="1" class="ie-list-input w-60" placeholder="0~1" />
                    <button class="ie-btn-icon-del" :disabled="scoring.levelToSeverity.length <= 1"
                      :title="scoring.levelToSeverity.length <= 1 ? '至少保留 1 项' : '删除'"
                      @click="scoring.levelToSeverity.splice(i, 1)"><Trash2 :size="11" /></button>
                  </div>
                </div>
                <button class="ie-link-btn" @click="scoring.levelToSeverity.push({ level: '', severity: 0 })"><Plus :size="11" /> 添加等级</button>
                <div class="ie-fld" style="max-width:160px"><label>满分（扣分基数）</label><input v-model.number="scoring.riskMaxScore" type="number" :min="0" /></div>
                <span class="ie-hint">风险越高扣越多：扣分 = 严重度 × 满分。</span>
              </template>
            </div>
          </template>
        </div>

        <!-- Tab: 输入类型（采集项）-->
        <div v-if="activeTab === 'scoring' && itemCategory === 'capture'" class="ie-tab-pane">
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
          <div v-if="captureNeedsResponseSet" class="ie-fld">
            <label>选项集</label>
            <select v-model="form.responseSetId" class="ie-select">
              <option :value="null">-- 不使用选项集 --</option>
              <option v-for="rs in responseSets" :key="rs.id" :value="rs.id">{{ rs.setName }}</option>
            </select>
          </div>
        </div>

        <!-- Tab: 验证规则 -->
        <div v-if="activeTab === 'validation'" class="ie-tab-pane">
          <div v-if="validationRulesList.length === 0" class="ie-empty">
            <ShieldCheck :size="22" class="ie-empty__icon" />
            <p class="ie-empty__title">暂无验证规则</p>
            <p class="ie-empty__sub">添加规则可约束检查员的录入，如必填、照片数量、数值范围等</p>
          </div>
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

        <!-- Tab: 条件逻辑 -->
        <div v-if="activeTab === 'condition'" class="ie-tab-pane">
          <ConditionBuilder
            v-model="form.conditionLogic"
            :all-items="conditionItems"
            :target-label="form.itemName"
          />
        </div>

      </div>

      <!-- 架构 E (2026-05-25): 整改规则已上提到项目级配置, 模板不再含整改面板.
           参考: docs/plans/2026-05-25-corrective-architecture-E.md -->
      <!-- (CorrectiveOverrideEditor 已移除) -->
    </div>

  </div>
</template>

<style scoped>
/* ═══════ Root ═══════ */
.ie-root { display:flex; flex-direction:column; height:100%; position:relative; }
/* V20260524 Bug#1: readonly 透明遮罩拦截全部输入, 但不挡视觉 — 与 header 关闭按钮独立 */
.ie-root--readonly .ie-body { pointer-events: none; opacity: 0.7; }
.ie-readonly-shield { position: absolute; inset: 40px 0 0 0; z-index: 5; cursor: not-allowed; background: transparent; }
.ie-readonly-hint { font-size: 11px; color: #d97706; padding: 2px 8px; background: #fef3c7; border-radius: 4px; }

/* ═══════ Header ═══════ */
.ie-header { display:flex; align-items:center; justify-content:space-between; padding:6px 10px; border-bottom:1px solid #e8ecf0; }
.ie-header-left { display:flex; align-items:center; gap:6px; }
.ie-header-right { display:flex; align-items:center; gap:4px; }

.ie-badge { display:inline-flex; align-items:center; gap:3px; padding:2px 6px; border-radius:4px; font-size:10px; font-weight:600; border:1px solid; }
.ie-code { font-family:monospace; font-size:10px; color:#8c95a3; }

.ie-btn-primary { display:inline-flex; align-items:center; gap:3px; padding:3px 10px; background:#1a6dff; color:#fff; border:none; border-radius:5px; font-size:11px; font-weight:500; cursor:pointer; }
.ie-btn-primary:hover { background:#1558d6; }
.ie-btn-ghost { padding:3px 10px; background:none; border:1px solid #dce1e8; border-radius:5px; font-size:11px; color:#5a6474; cursor:pointer; }
.ie-btn-ghost:hover { background:#f4f6f9; }
.ie-btn-sm { padding:2px 8px; background:none; border:1px solid #dce1e8; border-radius:4px; font-size:10px; color:#1a6dff; cursor:pointer; font-weight:500; }
.ie-btn-sm:hover { background:#f0f4ff; }

/* ═══════ Scrollable body ═══════ */
.ie-body { flex:1; overflow-y:auto; padding:8px 10px; display:flex; flex-direction:column; gap:6px; min-height:0; }

/* ═══════ Basic info section ═══════ */
.ie-section { border:1px solid #e8ecf0; border-radius:6px; padding:8px; display:flex; flex-direction:column; gap:6px; background:#fff; }

/* ═══════ Tabs ═══════ */
.ie-tabs { display:flex; gap:0; background:#fff; border:1px solid #e8ecf0; border-radius:6px 6px 0 0; border-bottom:none; overflow:hidden; flex-shrink:0; }

.ie-tab { flex:1; display:inline-flex; align-items:center; justify-content:center; gap:3px; padding:5px 4px; font-size:10px; font-weight:500; color:#8c95a3; background:none; border:none; border-bottom:2px solid transparent; cursor:pointer; transition:all 0.12s; white-space:nowrap; }
.ie-tab:hover { color:#5a6474; background:#f8f9fb; }
.ie-tab.active { color:#1a6dff; border-bottom-color:#1a6dff; font-weight:600; }

.ie-tab-tag { font-size:9px; font-weight:500; padding:1px 5px; border-radius:3px; color:#1a6dff; background:#eff6ff; }
.ie-tab-tag.green { color:#059669; background:#ecfdf5; }
.ie-tab-count { font-size:9px; font-weight:600; color:#fff; background:#1a6dff; padding:0 4px; border-radius:7px; min-width:14px; text-align:center; line-height:14px; }
.ie-tab-dot { width:6px; height:6px; border-radius:50%; background:#f59e0b; flex-shrink:0; }

/* ═══════ Tab content ═══════ */
.ie-tab-content { background:#fff; border:1px solid #e8ecf0; border-top:1px solid #e8ecf0; border-radius:0 0 6px 6px; flex:1; min-height:0; overflow-y:auto; }
.ie-tab-pane { padding:8px; display:flex; flex-direction:column; gap:6px; }

/* ═══════ Form fields ═══════ */
.ie-fld { display:flex; flex-direction:column; gap:2px; }
.ie-fld label, .ie-fld-label { font-size:11px; font-weight:500; color:#5a6474; }
.ie-fld input, .ie-fld textarea { width:100%; border:1px solid #dce1e8; border-radius:5px; padding:4px 8px; font-size:12px; outline:none; transition:border-color 0.15s; color:#1e2a3a; background:#fff; resize:vertical; }
.ie-fld input::placeholder, .ie-fld textarea::placeholder { color:#b8c0cc; }
.ie-fld input:focus, .ie-fld textarea:focus { border-color:#7aadff; box-shadow:0 0 0 2px rgba(26,109,255,0.06); }
.ie-req { color:#ef4444; }
.ie-muted { font-size:10px; color:#8c95a3; text-align:center; padding:4px 0; }

.ie-radio-group { display:flex; flex-direction:column; gap:4px; }
.ie-radio-label { display:flex; align-items:center; gap:5px; font-size:12px; color:#1e2a3a; cursor:pointer; }
.ie-radio-label input[type="radio"] { margin:0; accent-color:#1a6dff; }
.ie-hint { font-size:12px; color:#9ca3af; margin-top:4px; line-height:1.4; }

.ie-select { width:100%; border:1px solid #dce1e8; border-radius:5px; padding:4px 8px; font-size:12px; outline:none; color:#1e2a3a; background:#fff; cursor:pointer; appearance:none;
  background-image:url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%239ca3af' stroke-width='2'%3E%3Cpath d='M6 9l6 6 6-6'/%3E%3C/svg%3E");
  background-repeat:no-repeat; background-position:right 6px center; padding-right:24px; }
.ie-select:focus { border-color:#7aadff; box-shadow:0 0 0 2px rgba(26,109,255,0.06); }
.ie-select-sm { padding:3px 6px; font-size:10px; }

.ie-row-2 { display:grid; grid-template-columns:1fr 1fr; gap:6px; }
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
/* V20260524 Bug#5: isFlagged 选项分数标红, 区分"差/不合格" 视觉权重 */
.ie-rs-score--flagged { color:#dc2626; background:#fef2f2; }

/* ═══════ Scoring summary row ═══════ */
.ie-scoring-row { display:flex; align-items:center; justify-content:space-between; gap:8px; }
.ie-scoring-summary { font-size:12px; color:#374151; font-family:monospace; background:#f8f9fb; padding:4px 8px; border-radius:4px; flex:1; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }

/* ═══════ Validation rules ═══════ */
.ie-rule { border:1px solid #e8ecf0; border-radius:6px; overflow:hidden; }
.ie-rule-top { display:flex; align-items:center; gap:6px; padding:6px 8px; background:#f8f9fb; border-bottom:1px solid #f0f2f5; }
.ie-rule-num { display:inline-flex; align-items:center; justify-content:center; width:18px; height:18px; border-radius:50%; background:#dbeafe; color:#1d4ed8; font-size:10px; font-weight:700; flex-shrink:0; }
.ie-rule-body { padding:8px; display:flex; flex-direction:column; gap:6px; }

.ie-btn-icon-del { display:inline-flex; align-items:center; justify-content:center; width:22px; height:22px; border-radius:4px; border:none; background:none; color:#b8c0cc; cursor:pointer; transition:all 0.12s; flex-shrink:0; }
.ie-btn-icon-del:hover:not(:disabled) { background:#fef2f2; color:#ef4444; }
.ie-btn-icon-del:disabled { opacity:0.35; cursor:not-allowed; }

.ie-link-btn { display:inline-flex; align-items:center; gap:3px; font-size:11px; font-weight:500; color:#1a6dff; background:none; border:none; cursor:pointer; padding:4px 0; transition:color 0.12s; }
.ie-link-btn:hover { color:#1558d6; }

/* ═══════ Modal Dialog ═══════ */
.ie-mask { position:fixed; inset:0; z-index:1000; display:flex; align-items:center; justify-content:center; background:rgba(15,23,42,0.35); backdrop-filter:blur(1px); }
.ie-dialog { width:480px; max-height:80vh; background:#fff; border-radius:10px; box-shadow:0 16px 48px rgba(0,0,0,0.15); display:flex; flex-direction:column; overflow:hidden; }
.ie-dialog-head { display:flex; align-items:center; justify-content:space-between; padding:10px 14px; border-bottom:1px solid #f0f1f3; }
.ie-dialog-head h3 { font-size:13px; font-weight:600; color:#1e2a3a; margin:0; }
.ie-dialog-close { background:none; border:none; font-size:18px; color:#9ca3af; cursor:pointer; line-height:1; }
.ie-dialog-body { flex:1; overflow-y:auto; padding:10px 14px; display:flex; flex-direction:column; gap:8px; }
.ie-dialog-foot { display:flex; justify-content:flex-end; gap:6px; padding:6px 14px 10px; border-top:1px solid #f0f2f5; }

/* ═══════ Width util ═══════ */
.w-90 { width:90px; flex:0 0 90px; }

/* ═══════ Risk matrix grid (RISK_MATRIX 复杂模式) ═══════ */
.ie-matrix { border-collapse:separate; border-spacing:3px; margin:4px 0 2px; font-size:10px; }
.ie-matrix th { font-weight:500; color:#8c95a3; font-size:10px; padding:2px 4px; white-space:nowrap; }
.ie-matrix thead th { text-align:center; vertical-align:bottom; }
.ie-matrix tbody th { text-align:right; padding-right:6px; color:#5a6474; font-weight:500; }
.ie-matrix-corner { background:none; }
.ie-matrix td { padding:0; text-align:center; }
.ie-matrix-cell { width:54px; padding:3px 4px 3px 6px; font-size:10px; font-weight:600; text-align:center;
  border:1px solid #dce1e8; border-radius:4px; outline:none; cursor:pointer; appearance:none; color:#5a6474; background:#fff;
  background-image:url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='10' height='10' viewBox='0 0 24 24' fill='none' stroke='%23b8c0cc' stroke-width='2'%3E%3Cpath d='M6 9l6 6 6-6'/%3E%3C/svg%3E");
  background-repeat:no-repeat; background-position:right 3px center; padding-right:16px; transition:box-shadow 0.12s; }
.ie-matrix-cell:focus { box-shadow:0 0 0 2px rgba(26,109,255,0.12); }
/* 风险等级配色 — 浅底, 非色块装饰, 让矩阵一眼可读 (L绿 M黄 H橙 VH红) */
.ie-matrix-cell.lvl-L  { background-color:#ecfdf5; border-color:#a7f3d0; color:#047857; }
.ie-matrix-cell.lvl-M  { background-color:#fffbeb; border-color:#fde68a; color:#b45309; }
.ie-matrix-cell.lvl-H  { background-color:#fff7ed; border-color:#fed7aa; color:#c2410c; }
.ie-matrix-cell.lvl-VH { background-color:#fef2f2; border-color:#fecaca; color:#b91c1c; }

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

/* Event type association select */
.ie-event-select {
  width:100%; padding:5px 8px; border:1px solid #dce1e8; border-radius:5px;
  font-size:11px; color:#1e2a3a; background:#fff; outline:none; appearance:auto; cursor:pointer;
}
.ie-event-select:focus { border-color:#7aadff; }

/* ═══════ A+ 优化: 评分模式分组 + 折叠 ═══════ */
.ie-mode-section { margin-bottom: 8px; }
.ie-mode-section__head {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}
.ie-mode-section__label {
  font-size: 9px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #94a3b8;
  font-weight: 600;
}

.ie-mode-kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 13px;
  height: 13px;
  margin-left: 4px;
  background: rgba(26, 109, 255, 0.08);
  border: 1px solid rgba(26, 109, 255, 0.2);
  border-radius: 2px;
  font-family: var(--insp-font-mono, monospace);
  font-size: 9px;
  font-weight: 700;
  color: #1a6dff;
  line-height: 1;
}
.ie-mode-chip.active .ie-mode-kbd {
  background: rgba(255, 255, 255, 0.25);
  border-color: rgba(255, 255, 255, 0.4);
  color: white;
}
.ie-mode-chip.active { background: #1a6dff; color: white; border-color: #1a6dff; }

.ie-mode-toggle {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 10px;
  background: transparent;
  border: 1px dashed #c7cfdb;
  border-radius: 5px;
  font-size: 11px;
  font-family: inherit;
  color: #64748b;
  cursor: pointer;
  transition: all 0.15s;
  margin: 6px 0 4px;
}
.ie-mode-toggle:hover {
  background: #f4f6f9;
  border-color: #94a3b8;
  color: #1e2a3a;
}
.ie-mode-toggle__count {
  margin-left: 4px;
  padding: 1px 6px;
  background: #f4f6f9;
  border-radius: 8px;
  font-size: 10px;
  color: #94a3b8;
  font-weight: 500;
}

.ie-mode-advanced {
  border-left: 2px solid #e8ecf0;
  padding-left: 10px;
  margin-top: 4px;
}

.ie-mode-expand-enter-active,
.ie-mode-expand-leave-active {
  transition: opacity 0.18s ease, max-height 0.25s ease;
  overflow: hidden;
}
.ie-mode-expand-enter-from,
.ie-mode-expand-leave-to {
  opacity: 0;
  max-height: 0;
}
.ie-mode-expand-enter-to,
.ie-mode-expand-leave-from {
  opacity: 1;
  max-height: 320px;
}

/* 键盘提示条 */
.ie-kbd-hint {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 5px 10px;
  margin: 8px 0 0;
  background: linear-gradient(90deg, rgba(26, 109, 255, 0.05) 0%, transparent 100%);
  border: 1px solid rgba(26, 109, 255, 0.15);
  border-radius: 5px;
  font-size: 10.5px;
  color: #64748b;
}
.ie-kbd-hint__group {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}
.ie-kbd-hint__close {
  margin-left: auto;
  width: 18px;
  height: 18px;
  border: 0;
  background: transparent;
  font-size: 14px;
  color: #c7cfdb;
  border-radius: 3px;
  cursor: pointer;
}
.ie-kbd-hint__close:hover { background: rgba(0,0,0,0.05); color: #1e2a3a; }

/* 响应式: 中屏 chip 折行 */
@media (max-width: 1366px) {
  .ie-mode-grid { flex-wrap: wrap; }
  .ie-mode-chip { flex: 0 0 auto; }
}

/* ═══════ 加载失败 / 重试 / 空状态 (token 化) ═══════ */
.ie-hint-sm { font-size: 10px; color: var(--insp-ink-quaternary); margin-top: 2px; }
.ie-inline-retry {
  margin-top: 3px;
  font-size: 10px;
  color: var(--insp-fail);
  background: none;
  border: none;
  padding: 0;
  cursor: pointer;
  text-decoration: underline;
  text-align: left;
}
.ie-inline-retry:hover { color: var(--insp-fail); filter: brightness(0.9); }

.ie-rs-state {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: var(--insp-ink-tertiary);
  padding: 8px 10px;
  background: var(--insp-bg-subtle);
  border: 1px dashed var(--insp-border-default);
  border-radius: var(--insp-radius-sm);
  line-height: 1.4;
}
.ie-rs-state--error {
  color: var(--insp-fail);
  background: var(--insp-fail-pale);
  border-style: solid;
  border-color: var(--insp-fail-border);
}

/* 验证规则空状态 (与 InspEmptyState 视觉对齐) */
.ie-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 20px 12px;
  color: var(--insp-ink-quaternary);
}
.ie-empty__icon { color: var(--insp-ink-quaternary); margin-bottom: 6px; }
.ie-empty__title {
  margin: 0;
  font-size: var(--insp-text-sm);
  font-weight: var(--insp-fw-semibold);
  color: var(--insp-ink-secondary);
}
.ie-empty__sub {
  margin: 3px 0 0;
  font-size: var(--insp-text-xs);
  color: var(--insp-ink-quaternary);
  max-width: 260px;
  line-height: 1.5;
}
</style>
