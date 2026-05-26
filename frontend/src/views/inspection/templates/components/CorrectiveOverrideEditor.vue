<template>
  <details class="cor-editor" :open="hasRule">
    <summary>
      <span class="cor-title">整改规则</span>
      <span v-if="hasRule" class="cor-active">{{ summaryLabel }}</span>
      <span v-else class="cor-inactive">默认 (跟随项目策略)</span>
    </summary>

    <div class="cor-body">
      <div v-if="loadError" class="cor-error">
        <span>整改规则加载失败，为避免覆盖既有规则，保存已禁用。</span>
        <el-button size="small" text type="primary" :disabled="!itemId"
                   @click="itemId && loadOverride(itemId)">重试</el-button>
      </div>

      <div v-if="dirtyByModeChange" class="cor-mode-change-warning">
        <strong>⚠ 评分模式已变更</strong> — 整改规则已自动回退为"默认". 请确认新模式下的整改设置, 并点"保存规则"同步到数据库 (或点"清除"完全恢复默认).
      </div>

      <!-- 智能默认建议 (rule.mode = DEFAULT 且未配过) -->
      <div v-if="showSmartSuggestion" class="cor-smart-suggestion">
        <div class="cor-smart-title">💡 推荐默认规则</div>
        <div class="cor-smart-desc">{{ smartSuggestion.desc }}</div>
        <el-button type="primary" plain size="small" @click="applySmartSuggestion">一键应用</el-button>
      </div>

      <div class="cor-row">
        <label class="cor-label">建单条件</label>
        <el-radio-group v-model="rule.mode" class="cor-radio">
          <el-radio value="DEFAULT">默认 (跟随项目策略)</el-radio>
          <el-radio value="CUSTOM_THRESHOLD" :disabled="!supportsCustomThreshold">
            按本题量表配置
            <span v-if="!supportsCustomThreshold" class="cor-disabled-hint"> (本评分模式暂未支持逐题配置, 用"任何不达标都建单"或在响应集级别配置)</span>
          </el-radio>
          <el-radio value="ALWAYS">任何不达标都建单</el-radio>
          <el-radio value="NEVER">永不建整改单</el-radio>
        </el-radio-group>
      </div>

      <div class="cor-mode-hint">
        <template v-if="rule.mode === 'DEFAULT'">
          引擎按项目阈值判定本题严重度. 99% 的题用这个就够.
        </template>
        <template v-else-if="rule.mode === 'CUSTOM_THRESHOLD'">
          只对本题生效, 覆盖项目阈值. 适合"5星题 ≤3星整改 / 10星题 ≤4星整改 / 扣分题扣 ≥10分整改"这类逐题独立的判定.
        </template>
        <template v-else-if="rule.mode === 'ALWAYS'">
          不论分数多少, 只要不达标就建严重单. 用于"红线题" (消防/卫生关键项).
        </template>
        <template v-else>
          本题永不参与整改判定. 用于"备注栏"/"签字栏"类不评分的题.
        </template>
      </div>

      <!-- 按本题量表配置: 动态输入框 -->
      <div v-if="rule.mode === 'CUSTOM_THRESHOLD'" class="cor-threshold-panel">
        <!-- 离散选项模式 (LEVEL/SCORE_TABLE/TIERED_DEDUCTION/THRESHOLD/RISK_MATRIX): 每个选项独立配严重度 -->
        <div v-if="isDiscreteMode" class="cor-discrete-grid">
          <div v-if="scoringMode === 'RISK_MATRIX'" class="cor-risk-note">
            <b>说明</b>: 风险矩阵把"可能性 × 影响度" (输入维度) 通过矩阵格子塌缩到 <b>4 级风险等级</b> (L/M/H/VH).
            您在此为每级风险设置触发严重度. <br/>
            <span class="cor-risk-tip">例: 假设可能性 5 档 × 影响度 5 档 = 25 格, 最终都落到这 4 级里; "可能严重 + 影响严重" 落进 VH 格 → 触发您下面配的 VH 严重度.</span>
          </div>
          <div class="cor-discrete-head">
            <span>{{ scoringMode === 'RISK_MATRIX' ? '风险等级' : '本题响应' }}</span><span>触发严重度</span>
          </div>
          <div v-for="opt in currentDiscreteList" :key="opt" class="cor-discrete-row">
            <span class="cor-discrete-label">
              {{ opt }}
              <span v-if="scoringMode === 'RISK_MATRIX'" class="cor-risk-cn">{{ riskLevelLabel(opt) }}</span>
            </span>
            <span class="cor-discrete-arrow">→</span>
            <el-select v-model="rule.optionMap[opt]" size="small" style="width:120px"
                       @change="onOptionMapChange">
              <el-option label="不触发" value="NONE" />
              <el-option label="轻微" value="LOW" />
              <el-option label="中度" value="MEDIUM" />
              <el-option label="严重" value="HIGH" />
            </el-select>
          </div>
          <div class="cor-threshold-explain">
            引擎判定: 本题响应命中上表对应 severity 时建单 ("不触发"表示该响应不建单). 优先级高于项目阈值.
          </div>
        </div>
        <!-- 二元模式 (PASS_FAIL): 只配严重度 -->
        <div v-else-if="isBinaryMode" class="cor-threshold-row">
          <label class="cor-threshold-label">触发条件:</label>
          <span class="cor-prefix">不通过 (FAIL) 时建</span>
          <el-select v-model="rule.triggerSeverity" size="small" style="width:120px">
            <el-option label="严重" value="HIGH" />
            <el-option label="中度" value="MEDIUM" />
            <el-option label="轻微" value="LOW" />
          </el-select>
          <span class="cor-suffix">单</span>
        </div>
        <!-- 连续模式: 阈值 + 严重度 -->
        <div v-else-if="isThresholdMode" class="cor-threshold-row">
          <label class="cor-threshold-label">触发条件:</label>
          <span class="cor-threshold-input">
            <span v-if="thresholdPrefix" class="cor-prefix">{{ thresholdPrefix }}</span>
            <el-input-number v-model="rule.triggerValue"
                             :min="thresholdMin" :max="thresholdMax" :step="thresholdStep"
                             :precision="thresholdPrecision"
                             size="small" style="width:120px" />
            <span class="cor-suffix">{{ thresholdUnit }}</span>
            <span v-if="maxScoreHint" class="cor-max-hint">/ 满分 {{ maxScoreHint }}</span>
          </span>
          <span class="cor-threshold-arrow">→</span>
          <el-select v-model="rule.triggerSeverity" size="small" style="width:120px">
            <el-option label="严重" value="HIGH" />
            <el-option label="中度" value="MEDIUM" />
            <el-option label="轻微" value="LOW" />
          </el-select>
        </div>
        <div v-if="isThresholdMode" class="cor-threshold-explain">
          实际差距比例 (sev) = {{ sevPreview }}. 引擎判定: 本题 sev ≥ {{ sevPreview }} → {{ sevLabel(rule.triggerSeverity) }}, 否则不建单.
        </div>
        <div v-else-if="isBinaryMode" class="cor-threshold-explain">
          引擎判定: 本题响应为 FAIL → {{ sevLabel(rule.triggerSeverity) }} 单, 否则不建单. 与"任何不达标都建单"的区别是这里可选中度/轻微 (不锁定严重).
        </div>
      </div>

      <div class="cor-row">
        <label class="cor-label">完成时限</label>
        <el-input-number v-model="rule.deadlineDays"
                         :min="1" :max="60" size="small" style="width:140px"
                         :disabled="rule.mode === 'NEVER'"
                         placeholder="留空=用项目默认" />
        <span class="cor-tip">天 · 留空走项目默认时限</span>
      </div>

      <div class="cor-actions">
        <el-button size="small" :disabled="loadError" @click="clear">清除</el-button>
        <el-button type="primary" size="small" :loading="saving" :disabled="loadError || !canSave" @click="save">
          保存规则
        </el-button>
      </div>

      <!-- WhatIf 模拟器 -->
      <RuleSimulator
        v-if="scoringMode"
        :scoring-mode="scoringMode"
        :max-score="maxScore"
        :discrete-options="discreteOptions"
        :risk-levels="riskLevels"
        :scoring-config-json="scoringConfigJson"
        :item-rule-json="draftItemRuleJson"
      />
    </div>
  </details>
</template>

<script setup lang="ts">
import type { LongId } from '@/types/common'
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getItemOverride, saveItemOverride } from '@/api/inspection/correctiveCase'
import RuleSimulator from './RuleSimulator.vue'

interface Props {
  itemId: LongId | null
  scoringMode?: string
  maxScore?: number
  discreteOptions?: string[]   // LEVEL/SCORE_TABLE/TIERED_DEDUCTION/THRESHOLD 模式的选项标签
  riskLevels?: string[]        // RISK_MATRIX 模式的风险等级 (L/M/H/VH)
  scoringConfigJson?: string   // 题目的完整 scoring config JSON
}
const props = defineProps<Props>()

type Mode = 'DEFAULT' | 'CUSTOM_THRESHOLD' | 'ALWAYS' | 'NEVER'
type Sev = 'HIGH' | 'MEDIUM' | 'LOW'
type OptSev = 'NONE' | Sev

/** 严重度 内部值 → 中文显示 */
const SEV_LABEL: Record<string, string> = { HIGH: '严重', MEDIUM: '中度', LOW: '轻微', NONE: '不触发' }
function sevLabel(s: string | null | undefined): string { return s ? (SEV_LABEL[s] ?? s) : '—' }

/** 风险等级 → 中文显示 */
const RISK_LEVEL_LABEL: Record<string, string> = { L: '低风险', M: '中风险', H: '高风险', VH: '极高风险', E: '极端' }
function riskLevelLabel(s: string): string { return RISK_LEVEL_LABEL[s.toUpperCase()] ?? '' }

const rule = ref({
  mode: 'DEFAULT' as Mode,
  triggerValue: null as number | null,
  triggerSeverity: 'HIGH' as Sev,
  deadlineDays: null as number | null,
  // 离散模式: 选项标签 → 严重度 (NONE 表示不触发)
  optionMap: {} as Record<string, OptSev>,
})
const saving = ref(false)
const loadError = ref(false)
const dirtyByModeChange = ref(false)

// 哪些 scoringMode 支持自定义阈值 (按模式语义选择存储方式)
// - 连续阈值模式: singleThreshold (sev 阈值 + severity)
// - 二元模式 (PASS_FAIL): baseSeverityMap.FAIL
// - 离散标签模式: baseSeverityMap[label]
// - 风险矩阵: baseSeverityMap[level] (引擎按 resolveLabel 查)
const supportsCustomThreshold = computed(() => {
  const m = props.scoringMode
  if (!m) return false
  if (['RATING_SCALE', 'DIRECT', 'DEDUCTION', 'CUMULATIVE', 'ADDITION', 'WEIGHTED_MULTI', 'FORMULA'].includes(m)) return true
  if (m === 'PASS_FAIL') return true
  if (['LEVEL', 'SCORE_TABLE', 'TIERED_DEDUCTION', 'THRESHOLD'].includes(m) && (props.discreteOptions?.length ?? 0) > 0) return true
  if (m === 'RISK_MATRIX' && (props.riskLevels?.length ?? 0) > 0) return true
  return false
})

/** 是否为"二元"模式 (只有达/不达, 无连续阈值, 但仍需可配严重度) */
const isBinaryMode = computed(() => props.scoringMode === 'PASS_FAIL')

/** 是否为"离散选项/风险等级"模式 — 每个标签独立配 severity */
const isDiscreteMode = computed(() => {
  const m = props.scoringMode
  if (['LEVEL', 'SCORE_TABLE', 'TIERED_DEDUCTION', 'THRESHOLD'].includes(m ?? '') && (props.discreteOptions?.length ?? 0) > 0) return true
  if (m === 'RISK_MATRIX' && (props.riskLevels?.length ?? 0) > 0) return true
  return false
})

/** 当前 isDiscreteMode 下实际的选项列表 (RISK_MATRIX 用 riskLevels, 其他用 discreteOptions) */
const currentDiscreteList = computed<string[]>(() => {
  if (props.scoringMode === 'RISK_MATRIX') return props.riskLevels ?? []
  return props.discreteOptions ?? []
})

/** 是否为"连续阈值"模式 */
const isThresholdMode = computed(() => {
  const m = props.scoringMode
  return ['RATING_SCALE', 'DIRECT', 'DEDUCTION', 'CUMULATIVE', 'ADDITION', 'WEIGHTED_MULTI', 'FORMULA'].includes(m ?? '')
})

const thresholdPrefix = computed(() => {
  switch (props.scoringMode) {
    case 'DEDUCTION':       return '扣 ≥'
    case 'CUMULATIVE':      return '≥'
    case 'ADDITION':        return '加分 ≤'
    default:                return '≤'
  }
})
const thresholdUnit = computed(() => {
  switch (props.scoringMode) {
    case 'RATING_SCALE':    return '星'
    case 'DIRECT':          return '分'
    case 'DEDUCTION':       return '分'
    case 'CUMULATIVE':      return '次'
    case 'ADDITION':        return '分'
    case 'WEIGHTED_MULTI':  return '分 (加权总分)'
    case 'FORMULA':         return '(计算值)'
    default: return ''
  }
})
const maxScoreHint = computed(() => {
  if (isThresholdMode.value) return props.maxScore ?? null
  return null
})
const thresholdMin = computed(() => 0)
const thresholdMax = computed(() => props.maxScore ?? 100)
const thresholdStep = computed(() => {
  const m = props.scoringMode
  if (m === 'RATING_SCALE' || m === 'DIRECT' || m === 'ADDITION') return 0.5
  if (m === 'CUMULATIVE') return 1
  return 1
})
const thresholdPrecision = computed(() => {
  const m = props.scoringMode
  if (m === 'RATING_SCALE' || m === 'DIRECT' || m === 'ADDITION') return 1
  return 0
})

/** 由用户输入值换算 sev (0~1) — UI 仅展示, 真正存储在 save() 时计算 */
const sevPreview = computed(() => {
  const sev = computeSev(rule.value.triggerValue)
  return sev == null ? '—' : sev.toFixed(3)
})

function computeSev(v: number | null): number | null {
  if (v == null || isNaN(v)) return null
  const max = props.maxScore ?? 10
  if (max <= 0) return null
  switch (props.scoringMode) {
    case 'RATING_SCALE':
    case 'DIRECT':
    case 'ADDITION':
    case 'WEIGHTED_MULTI':
    case 'FORMULA':
      // "得分 ≤ v" → sev = 1 - v/max
      return Math.max(0, Math.min(1, 1 - v / max))
    case 'DEDUCTION':
    case 'CUMULATIVE':
      // "扣分 ≥ v" / "次数 ≥ v" → sev = v/max
      return Math.max(0, Math.min(1, v / max))
    default: return null
  }
}

function sevToValue(sev: number): number | null {
  const max = props.maxScore ?? 10
  if (max <= 0) return null
  switch (props.scoringMode) {
    case 'RATING_SCALE':
    case 'DIRECT':
    case 'ADDITION':
    case 'WEIGHTED_MULTI':
    case 'FORMULA':
      return parseFloat((max * (1 - sev)).toFixed(1))
    case 'DEDUCTION':
    case 'CUMULATIVE':
      return parseFloat((sev * max).toFixed(1))
    default: return null
  }
}

const hasRule = computed(() =>
  rule.value.mode !== 'DEFAULT' || rule.value.deadlineDays != null
)
const summaryLabel = computed(() => {
  if (rule.value.mode === 'ALWAYS') return '任何不达标都建单'
  if (rule.value.mode === 'NEVER')  return '永不建单'
  if (rule.value.mode === 'CUSTOM_THRESHOLD') {
    if (isBinaryMode.value) {
      return `不通过 → ${sevLabel(rule.value.triggerSeverity)}`
    }
    if (isDiscreteMode.value) {
      const active = Object.entries(rule.value.optionMap).filter(([, sev]) => sev && sev !== 'NONE')
      if (active.length === 0) return '按本题量表配置 (未填)'
      if (active.length <= 2) return active.map(([l, s]) => `${l}→${sevLabel(s)}`).join(', ')
      return `${active.length} 个选项已配`
    }
    const v = rule.value.triggerValue
    if (v == null) return '按本题量表配置 (未填)'
    const pre = thresholdPrefix.value, unit = thresholdUnit.value
    return `${pre}${v}${unit} → ${sevLabel(rule.value.triggerSeverity)}`
  }
  if (rule.value.deadlineDays != null) return `自定义时限 ${rule.value.deadlineDays} 天`
  return '已配置'
})
/** 模拟器用的 draft itemRule JSON — 反映当前 UI 上未保存的规则 */
const draftItemRuleJson = computed(() => JSON.stringify(buildPayload()))

/** 智能默认建议 — 按 scoringMode 推荐合理规则 */
interface SmartSuggestion { desc: string; apply: () => void }
const smartSuggestion = computed<SmartSuggestion>(() => {
  const m = props.scoringMode
  switch (m) {
    case 'PASS_FAIL':
      return {
        desc: '通过/不通过题: 推荐"不通过 → 严重", 1 天内整改 (适合卫生/安全等关键项).',
        apply: () => {
          rule.value.mode = 'CUSTOM_THRESHOLD'
          rule.value.triggerSeverity = 'HIGH'
          rule.value.deadlineDays = 1
        }
      }
    case 'RATING_SCALE':
      return {
        desc: '星级评分题: 推荐"≤2 星 → 严重", 5 星制下最差 40% 即触发.',
        apply: () => {
          rule.value.mode = 'CUSTOM_THRESHOLD'
          rule.value.triggerValue = Math.max(1, Math.floor((props.maxScore ?? 5) * 0.4))
          rule.value.triggerSeverity = 'HIGH'
        }
      }
    case 'DIRECT':
      return {
        desc: '直接打分题: 推荐"≤ 满分 40% → 严重" (例如 10 分制 ≤4 分).',
        apply: () => {
          rule.value.mode = 'CUSTOM_THRESHOLD'
          rule.value.triggerValue = Math.max(1, Math.floor((props.maxScore ?? 10) * 0.4))
          rule.value.triggerSeverity = 'HIGH'
        }
      }
    case 'DEDUCTION':
      return {
        desc: '扣分题: 推荐"扣 ≥ 满扣 50% → 严重".',
        apply: () => {
          rule.value.mode = 'CUSTOM_THRESHOLD'
          rule.value.triggerValue = Math.max(1, Math.floor((props.maxScore ?? 10) * 0.5))
          rule.value.triggerSeverity = 'HIGH'
        }
      }
    case 'CUMULATIVE':
      return {
        desc: '累计计次题: 推荐"≥ 3 次 → 严重" (违纪记录类适合此默认).',
        apply: () => {
          rule.value.mode = 'CUSTOM_THRESHOLD'
          rule.value.triggerValue = 3
          rule.value.triggerSeverity = 'HIGH'
        }
      }
    case 'LEVEL':
    case 'SCORE_TABLE':
    case 'TIERED_DEDUCTION':
    case 'THRESHOLD': {
      const opts = props.discreteOptions ?? []
      if (opts.length === 0) return { desc: '', apply: () => {} }
      // 假设最后 1-2 个选项 (通常是最差档) → 严重
      const lastIdx = opts.length - 1
      const secondLastIdx = lastIdx - 1
      return {
        desc: `离散等级题: 推荐"${opts[lastIdx]} → 严重${secondLastIdx >= 0 ? ', ' + opts[secondLastIdx] + ' → 中度' : ''}" (按最差 1-2 档触发).`,
        apply: () => {
          rule.value.mode = 'CUSTOM_THRESHOLD'
          const map: Record<string, OptSev> = {}
          for (const o of opts) map[o] = 'NONE'
          map[opts[lastIdx]] = 'HIGH'
          if (secondLastIdx >= 0) map[opts[secondLastIdx]] = 'MEDIUM'
          rule.value.optionMap = map
        }
      }
    }
    case 'RISK_MATRIX':
      return {
        desc: '风险矩阵: 推荐"H 高风险 → 严重, VH 极高风险 → 严重", 符合 ISO 31000 实践.',
        apply: () => {
          rule.value.mode = 'CUSTOM_THRESHOLD'
          rule.value.optionMap = { L: 'NONE', M: 'NONE', H: 'HIGH', VH: 'HIGH' }
        }
      }
    default:
      return { desc: '', apply: () => {} }
  }
})
const showSmartSuggestion = computed(() => {
  // 只在 mode=DEFAULT (用户还没配过任何规则) 且有可建议时显示
  return rule.value.mode === 'DEFAULT'
      && rule.value.deadlineDays == null
      && smartSuggestion.value.desc !== ''
      && supportsCustomThreshold.value
})
function applySmartSuggestion() {
  smartSuggestion.value.apply()
}

const canSave = computed(() => {
  if (rule.value.mode === 'CUSTOM_THRESHOLD') {
    if (isBinaryMode.value) return !!rule.value.triggerSeverity
    if (isDiscreteMode.value) {
      // 离散模式至少有一个选项配置非 NONE
      return Object.values(rule.value.optionMap).some(v => v && v !== 'NONE')
    }
    return rule.value.triggerValue != null && !isNaN(rule.value.triggerValue)
  }
  return true
})

function onOptionMapChange() { /* el-select v-model 已直接修改 ref, 这里仅占位提供潜在 hook */ }

async function loadOverride(itemId: LongId) {
  resetRule()
  loadError.value = false
  try {
    const json = await getItemOverride(itemId)
    if (!json) return
    const obj = typeof json === 'string' ? JSON.parse(json) : json

    if (obj.neverCorrect) {
      rule.value.mode = 'NEVER'
    } else if (obj.singleThreshold && typeof obj.singleThreshold.sevThreshold === 'number') {
      rule.value.mode = 'CUSTOM_THRESHOLD'
      rule.value.triggerValue = sevToValue(obj.singleThreshold.sevThreshold)
      rule.value.triggerSeverity = (obj.singleThreshold.triggerSeverity || 'HIGH') as Sev
    } else if (obj.criticality === 'RED') {
      rule.value.mode = 'ALWAYS'
    } else if (obj.baseSeverityMap && Object.keys(obj.baseSeverityMap).length > 0
               && isBinaryMode.value
               && obj.baseSeverityMap.FAIL) {
      // PASS_FAIL 二元 CUSTOM
      rule.value.mode = 'CUSTOM_THRESHOLD'
      rule.value.triggerSeverity = (obj.baseSeverityMap.FAIL || 'HIGH') as Sev
    } else if (obj.baseSeverityMap && Object.keys(obj.baseSeverityMap).length > 0
               && isDiscreteMode.value) {
      // 离散 CUSTOM: baseSeverityMap 的 label → severity 映射 (无 criticality)
      // 适用 LEVEL/SCORE_TABLE/TIERED_DEDUCTION/THRESHOLD/RISK_MATRIX
      rule.value.mode = 'CUSTOM_THRESHOLD'
      const m: Record<string, OptSev> = {}
      for (const opt of currentDiscreteList.value) {
        const sev = obj.baseSeverityMap[opt]
        m[opt] = (sev && ['HIGH','MEDIUM','LOW'].includes(sev)) ? (sev as OptSev) : 'NONE'
      }
      rule.value.optionMap = m
    } else if (Array.isArray(obj.forceCorrect) && obj.forceCorrect.length > 0) {
      rule.value.mode = 'ALWAYS'
    } else if (obj.baseSeverityMap && Object.keys(obj.baseSeverityMap).length > 0) {
      rule.value.mode = 'ALWAYS'
    }

    if (typeof obj.deadlineOverrideDays === 'number') {
      rule.value.deadlineDays = obj.deadlineOverrideDays
    } else if (obj.deadlineOverride && typeof obj.deadlineOverride.high === 'number') {
      rule.value.deadlineDays = obj.deadlineOverride.high
    }
  } catch (e: unknown) {
    loadError.value = true
    ElMessage.error('整改规则加载失败: ' + ((e as Error)?.message || '未知错误'))
  }
}

watch(() => props.itemId, itemId => {
  resetRule()
  loadError.value = false
  if (!itemId) return
  loadOverride(itemId)
}, { immediate: true })

// 切换 scoringMode 时, 检查整改规则与新模式的兼容性
watch(() => props.scoringMode, (newMode, oldMode) => {
  // 仅在 itemId 已加载且模式真实切换时触发 (避免初次加载误报)
  if (!props.itemId || !oldMode || newMode === oldMode) return

  // 当前是 CUSTOM_THRESHOLD 但新模式不支持 → 回退到 DEFAULT 并提示用户保存
  if (rule.value.mode === 'CUSTOM_THRESHOLD' && !supportsCustomThreshold.value) {
    rule.value.mode = 'DEFAULT'
    rule.value.triggerValue = null
    dirtyByModeChange.value = true
  }
})

function resetRule() {
  rule.value = {
    mode: 'DEFAULT',
    triggerValue: null,
    triggerSeverity: 'HIGH',
    deadlineDays: null,
    optionMap: initOptionMap(),
  }
  dirtyByModeChange.value = false
}

function initOptionMap(): Record<string, OptSev> {
  const m: Record<string, OptSev> = {}
  for (const opt of currentDiscreteList.value) m[opt] = 'NONE'
  return m
}

function buildPayload(): Record<string, unknown> {
  const out: Record<string, unknown> = {}
  if (rule.value.mode === 'NEVER') {
    out.neverCorrect = true
  } else if (rule.value.mode === 'ALWAYS') {
    out.criticality = 'RED'
  } else if (rule.value.mode === 'CUSTOM_THRESHOLD') {
    if (isBinaryMode.value) {
      out.baseSeverityMap = { FAIL: rule.value.triggerSeverity }
    } else if (isDiscreteMode.value) {
      // 离散模式: 取所有非 NONE 的映射
      const map: Record<string, string> = {}
      for (const [label, sev] of Object.entries(rule.value.optionMap)) {
        if (sev && sev !== 'NONE') map[label] = sev
      }
      if (Object.keys(map).length > 0) out.baseSeverityMap = map
    } else {
      const sev = computeSev(rule.value.triggerValue)
      if (sev != null) {
        out.singleThreshold = {
          sevThreshold: parseFloat(sev.toFixed(3)),
          triggerSeverity: rule.value.triggerSeverity,
        }
      }
    }
  }
  if (rule.value.mode !== 'NEVER' && rule.value.deadlineDays != null) {
    out.deadlineOverrideDays = rule.value.deadlineDays
  }
  return out
}

async function save() {
  if (!props.itemId) {
    ElMessage.warning('请先保存检查项, 再设置整改规则')
    return
  }
  saving.value = true
  try {
    const payload = buildPayload()
    await saveItemOverride(props.itemId, payload)
    dirtyByModeChange.value = false
    ElMessage.success(Object.keys(payload).length === 0 ? '已清除规则' : '已保存规则')
  } catch (e: unknown) {
    ElMessage.error('保存失败: ' + ((e as Error)?.message || '未知错误'))
  } finally {
    saving.value = false
  }
}

async function clear() {
  if (!props.itemId) { resetRule(); return }
  try {
    await ElMessageBox.confirm(
      '确认清除此检查项的整改规则？清除后将改用项目级策略，此操作不可恢复。',
      '清除整改规则',
      { type: 'warning', confirmButtonText: '确认清除', cancelButtonText: '取消' }
    )
  } catch { return }
  resetRule()
  saving.value = true
  try {
    await saveItemOverride(props.itemId, {})
    ElMessage.success('已清除规则')
  } catch (e: unknown) {
    ElMessage.error('清除失败: ' + ((e as Error)?.message || '未知错误'))
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.cor-editor {
  margin-top: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fafbfc;
}
.cor-editor[open] { background: #fff; border-color: #d1d5db; }
.cor-editor summary {
  padding: 10px 14px;
  cursor: pointer;
  user-select: none;
  display: flex; align-items: center; gap: 10px;
  font-size: 13px; font-weight: 500;
  color: #1f2937;
}
.cor-title { flex: 1; }
.cor-active {
  background: #ede9fe; color: #6d28d9;
  padding: 1px 8px; border-radius: 3px;
  font-size: 11px; font-weight: 600;
}
.cor-inactive { color: #9ca3af; font-size: 12px; font-weight: 400; }
.cor-body {
  padding: 12px 14px 16px;
  border-top: 1px solid #f3f4f6;
}
.cor-error {
  display: flex; align-items: center; justify-content: space-between; gap: 8px;
  margin: 0 0 10px;
  padding: 8px 12px;
  background: #fef2f2;
  color: #dc2626;
  border: 1px solid #fecaca;
  border-radius: 4px;
  font-size: 12px; line-height: 1.5;
}
.cor-smart-suggestion {
  margin: 0 0 10px;
  padding: 8px 12px;
  background: #eff6ff;
  color: #1e40af;
  border: 1px solid #bfdbfe;
  border-radius: 4px;
  font-size: 12px; line-height: 1.5;
  display: flex; align-items: center; gap: 12px;
}
.cor-smart-title { font-weight: 600; color: #1e3a8a; white-space: nowrap; }
.cor-smart-desc { flex: 1; }
.cor-mode-change-warning {
  margin: 0 0 10px;
  padding: 8px 12px;
  background: #fef3c7;
  color: #92400e;
  border: 1px solid #fcd34d;
  border-radius: 4px;
  font-size: 12px; line-height: 1.5;
}
.cor-mode-change-warning strong { color: #b45309; margin-right: 6px; }
.cor-row {
  display: flex; align-items: center; gap: 12px;
  padding: 8px 0;
}
.cor-label {
  display: flex; align-items: center; gap: 8px;
  font-size: 13px; color: #374151; min-width: 100px;
  font-weight: 500;
}
.cor-radio { display: flex; flex-direction: column; align-items: flex-start; gap: 8px; }
.cor-radio :deep(.el-radio) { margin-right: 0; height: auto; }
.cor-radio :deep(.el-radio__label) { padding-left: 8px; }
.cor-disabled-hint { color: #9ca3af; font-size: 11px; margin-left: 4px; }
.cor-mode-hint {
  margin: 4px 0 8px 112px;
  padding: 6px 10px;
  background: #f5f3ff; color: #5b21b6;
  font-size: 12px; line-height: 1.5;
  border-radius: 4px;
}
.cor-threshold-panel {
  margin: 4px 0 8px 112px;
  padding: 10px 12px;
  background: #f0f9ff;
  border: 1px solid #bae6fd;
  border-radius: 6px;
}
.cor-threshold-row {
  display: flex; align-items: center; gap: 10px;
}
.cor-threshold-label {
  font-size: 13px; color: #374151; font-weight: 500;
}
.cor-threshold-input {
  display: inline-flex; align-items: center; gap: 6px;
}
.cor-prefix { color: #5a6474; font-size: 12px; font-weight: 500; }
.cor-suffix { color: #5a6474; font-size: 12px; }
.cor-max-hint { color: #9ca3af; font-size: 12px; margin-left: 4px; }
.cor-threshold-arrow { color: #0369a1; font-weight: 600; }
.cor-threshold-explain {
  margin-top: 8px;
  font-size: 11px; color: #075985;
  font-style: italic;
}
.cor-discrete-grid { display: flex; flex-direction: column; gap: 6px; }
.cor-risk-note {
  margin-bottom: 10px;
  padding: 8px 12px;
  background: #fffbeb;
  color: #92400e;
  border: 1px solid #fcd34d;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.6;
}
.cor-risk-note b { color: #b45309; font-weight: 600; }
.cor-risk-tip { display: block; margin-top: 4px; color: #6b7280; font-style: italic; }
.cor-risk-cn {
  margin-left: 6px;
  padding: 0 6px;
  font-size: 11px;
  color: #6b7280;
  background: #f3f4f6;
  border-radius: 3px;
}
.cor-discrete-head { display: flex; gap: 16px; padding: 4px 0;
  font-size: 11px; color: #6b7280; font-weight: 500;
  border-bottom: 1px solid #cbd5e1;
}
.cor-discrete-head span:first-child { min-width: 140px; }
.cor-discrete-row { display: flex; align-items: center; gap: 8px; padding: 2px 0; }
.cor-discrete-label { min-width: 140px; font-size: 13px; color: #1f2937; }
.cor-discrete-arrow { color: #0369a1; font-weight: 600; }
.cor-tip { font-size: 12px; color: #9ca3af; }
.cor-actions {
  display: flex; justify-content: flex-end; gap: 8px;
  margin-top: 12px;
  border-top: 1px dashed #e5e7eb;
  padding-top: 12px;
}
</style>
