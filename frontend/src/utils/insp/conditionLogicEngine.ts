import type {
  ConditionRule,
  ConditionGroup,
  ConditionAction,
  ConditionLogicV1,
  ConditionLogicV2,
  ItemConditionState,
  Operator,
} from '@/types/insp/conditionLogic'
import {
  isConditionGroup,
  isV2,
  isV1,
  DEFAULT_CONDITION_STATE,
  OPERATORS,
  ACTION_TYPES,
} from '@/types/insp/conditionLogic'

// =========================================================
// 操作符求值
// =========================================================

function evaluateOperator(operator: Operator, actual: string | null | undefined, expected?: string): boolean {
  const av = actual ?? ''

  switch (operator) {
    case 'isEmpty':
      return av === ''
    case 'isNotEmpty':
      return av !== ''
    case 'equals':
      return av === (expected ?? '')
    case 'notEquals':
      return av !== (expected ?? '')
    case 'greaterThan':
      return av !== '' && Number(av) > Number(expected)
    case 'lessThan':
      return av !== '' && Number(av) < Number(expected)
    case 'greaterOrEqual':
      return av !== '' && Number(av) >= Number(expected)
    case 'lessOrEqual':
      return av !== '' && Number(av) <= Number(expected)
    case 'contains':
      return av.includes(expected ?? '')
    case 'in': {
      const set = (expected ?? '').split(',').map(s => s.trim())
      return set.includes(av)
    }
    case 'between': {
      const parts = (expected ?? '').split(',').map(s => s.trim())
      if (parts.length !== 2 || av === '') return false
      const num = Number(av)
      return num >= Number(parts[0]) && num <= Number(parts[1])
    }
    default:
      return false
  }
}

// =========================================================
// 递归求值
// =========================================================

type ValueResolver = (fieldCode: string) => string | null | undefined

function evaluateRule(rule: ConditionRule, getVal: ValueResolver): boolean {
  const actual = getVal(rule.field)
  return evaluateOperator(rule.operator, actual, rule.value)
}

function evaluateNode(node: ConditionRule | ConditionGroup, getVal: ValueResolver): boolean {
  if (isConditionGroup(node)) {
    const { logicOp, rules } = node
    if (rules.length === 0) return true
    if (logicOp === 'and') {
      return rules.every(r => evaluateNode(r, getVal))
    } else {
      return rules.some(r => evaluateNode(r, getVal))
    }
  }
  return evaluateRule(node as ConditionRule, getVal)
}

// =========================================================
// V1 → V2 转换
// =========================================================

export function normalizeV1toV2(v1: ConditionLogicV1): ConditionLogicV2 {
  const rule: ConditionRule = {
    field: v1.when,
    operator: v1.operator as Operator,
  }
  if (v1.value !== undefined && v1.value !== '') {
    rule.value = v1.value
  }
  return {
    version: 2,
    conditions: { logicOp: 'and', rules: [rule] },
    actions: [{ type: v1.action as ConditionAction['type'] }],
  }
}

// =========================================================
// 条件逻辑评估入口
// =========================================================

/**
 * 评估条件逻辑 JSON 字符串，返回满足时的动作列表。
 * - null → 无条件或条件未满足
 * - ConditionAction[] → 条件满足，返回要执行的动作
 */
export function evaluateConditionLogic(
  json: string | null | undefined,
  getVal: ValueResolver,
): ConditionAction[] | null {
  if (!json) return null
  try {
    const parsed = JSON.parse(json)
    if (isV2(parsed)) {
      const matches = evaluateNode(parsed.conditions, getVal)
      return matches ? parsed.actions : null
    }
    if (isV1(parsed)) {
      console.warn('[ConditionLogic] V1 format detected at runtime, should have been migrated:', json)
      const v2 = normalizeV1toV2(parsed)
      const matches = evaluateNode(v2.conditions, getVal)
      return matches ? v2.actions : null
    }
    return null
  } catch {
    return null
  }
}

// =========================================================
// Item 状态解析
// =========================================================

/**
 * 解析 Item 条件状态。
 * @param conditionLogicJson  item 自身的 conditionLogic JSON
 * @param getVal              取其他 item 值的函数
 * @param sectionVisible      所属分区是否可见
 */
export function resolveItemState(
  conditionLogicJson: string | null | undefined,
  getVal: ValueResolver,
  sectionVisible: boolean = true,
): ItemConditionState {
  // 分区隐藏 → 强制全隐藏
  if (!sectionVisible) {
    return { visible: false, required: false, disabled: false, scoreOverride: null, clearValue: false }
  }

  const actions = evaluateConditionLogic(conditionLogicJson, getVal)

  // 无条件 → 需要判断是否有 "show" 类型动作定义（show 语义：默认隐藏，满足时才显示）
  if (actions === null) {
    // 检查是否定义了条件但未满足
    if (conditionLogicJson) {
      try {
        const parsed = JSON.parse(conditionLogicJson)
        const v2 = isV2(parsed) ? parsed : isV1(parsed) ? normalizeV1toV2(parsed) : null
        if (v2) {
          const hasShowAction = v2.actions.some((a: ConditionAction) => a.type === 'show')
          if (hasShowAction) {
            // "show" 动作存在但条件不满足 → 隐藏
            return { visible: false, required: false, disabled: false, scoreOverride: null, clearValue: false }
          }
        }
      } catch { /* ignore */ }
    }
    return { ...DEFAULT_CONDITION_STATE }
  }

  // 条件满足，展开动作
  const state: ItemConditionState = { ...DEFAULT_CONDITION_STATE }
  for (const action of actions) {
    switch (action.type) {
      case 'show':
        state.visible = true
        break
      case 'hide':
        state.visible = false
        break
      case 'require':
        state.required = true
        break
      case 'disable':
        state.disabled = true
        break
      case 'setScore':
        state.scoreOverride = typeof action.value === 'number' ? action.value : Number(action.value)
        break
      case 'clearValue':
        state.clearValue = true
        break
    }
  }
  return state
}

// =========================================================
// 分区条件评估
// =========================================================

/**
 * 评估分区条件逻辑。
 * @returns true=可见, false=隐藏
 */
export function evaluateSectionVisibility(
  sectionConditionLogicJson: string | null | undefined,
  getVal: ValueResolver,
): boolean {
  const actions = evaluateConditionLogic(sectionConditionLogicJson, getVal)
  if (actions === null) {
    // 检查是否定义了 show 动作（未满足 → 隐藏）
    if (sectionConditionLogicJson) {
      try {
        const parsed = JSON.parse(sectionConditionLogicJson)
        const v2 = isV2(parsed) ? parsed : isV1(parsed) ? normalizeV1toV2(parsed) : null
        if (v2) {
          const hasShowAction = v2.actions.some((a: ConditionAction) => a.type === 'show')
          if (hasShowAction) return false
        }
      } catch { /* ignore */ }
    }
    return true
  }
  for (const action of actions) {
    if (action.type === 'hide') return false
    if (action.type === 'show') return true
  }
  return true
}

// =========================================================
// 拓扑排序级联求值
// =========================================================

interface DetailLike {
  itemCode: string
  conditionLogic: string | null
  responseValue: string | null | undefined
}

/** 从 conditionLogic JSON 中提取所有引用的字段 */
export function extractFieldRefs(json: string | null | undefined): string[] {
  if (!json) return []
  try {
    const parsed = JSON.parse(json)
    const refs: string[] = []
    const walk = (node: any) => {
      if (!node) return
      if (node.field) refs.push(node.field)
      if (node.when) refs.push(node.when)
      if (node.rules) node.rules.forEach(walk)
      if (node.conditions) walk(node.conditions)
    }
    walk(parsed)
    return refs
  } catch {
    return []
  }
}

/** 构建依赖图 */
export function buildDependencyGraph(details: DetailLike[]): Map<string, string[]> {
  const graph = new Map<string, string[]>()
  for (const d of details) {
    const deps = extractFieldRefs(d.conditionLogic)
    graph.set(d.itemCode, deps)
  }
  return graph
}

/** Kahn 算法拓扑排序，返回排序后的 itemCode 列表 */
export function topologicalSort(details: DetailLike[]): string[] {
  const graph = buildDependencyGraph(details)
  const allCodes = details.map(d => d.itemCode)

  // 构建正向图 dep → code（dep 先求值）
  const forward = new Map<string, string[]>()
  for (const code of allCodes) forward.set(code, [])

  for (const [code, deps] of graph) {
    for (const dep of deps) {
      if (forward.has(dep)) {
        forward.get(dep)!.push(code)
      }
    }
  }

  // 入度
  const deg = new Map<string, number>()
  for (const code of allCodes) deg.set(code, 0)
  for (const [_, targets] of forward) {
    for (const t of targets) {
      deg.set(t, (deg.get(t) || 0) + 1)
    }
  }

  // BFS
  const queue: string[] = []
  for (const [code, d] of deg) {
    if (d === 0) queue.push(code)
  }

  const sorted: string[] = []
  while (queue.length > 0) {
    const node = queue.shift()!
    sorted.push(node)
    for (const neighbor of (forward.get(node) || [])) {
      const nd = (deg.get(neighbor) || 0) - 1
      deg.set(neighbor, nd)
      if (nd === 0) queue.push(neighbor)
    }
  }

  // 检测循环
  if (sorted.length < allCodes.length) {
    console.warn('[ConditionLogic] Circular dependency detected, falling back to original order')
    const inSorted = new Set(sorted)
    for (const code of allCodes) {
      if (!inSorted.has(code)) sorted.push(code)
    }
  }

  return sorted
}

// =========================================================
// 批量求值所有 Item（含拓扑排序）
// =========================================================

interface SectionCondition {
  sectionId: number | string
  conditionLogic: string | null
}

/**
 * 评估所有 Item 的条件状态。
 * @param details           提交明细列表
 * @param sectionConditions 分区条件列表（可选）
 * @param detailSectionMap  detail.itemCode → sectionId 映射（可选，用于分区级条件）
 */
export function evaluateAllConditions(
  details: DetailLike[],
  sectionConditions?: SectionCondition[],
  detailSectionMap?: Map<string, number | string>,
): Map<string, ItemConditionState> {
  const states = new Map<string, ItemConditionState>()

  // 值解析器（考虑隐藏 item 的值视为 null）
  const getVal = (fieldCode: string): string | null | undefined => {
    const existing = states.get(fieldCode)
    if (existing && !existing.visible) return null
    const d = details.find(x => x.itemCode === fieldCode)
    return d?.responseValue ?? null
  }

  // 分区可见性
  const sectionVisibility = new Map<number | string, boolean>()
  if (sectionConditions) {
    for (const sc of sectionConditions) {
      sectionVisibility.set(sc.sectionId, evaluateSectionVisibility(sc.conditionLogic, getVal))
    }
  }

  // 拓扑排序
  const order = topologicalSort(details)
  const detailMap = new Map(details.map(d => [d.itemCode, d]))

  for (const code of order) {
    const detail = detailMap.get(code)
    if (!detail) continue

    let sectionVisible = true
    if (detailSectionMap && sectionVisibility.size > 0) {
      const sectionId = detailSectionMap.get(code)
      if (sectionId !== undefined) {
        sectionVisible = sectionVisibility.get(sectionId) ?? true
      }
    }

    const state = resolveItemState(detail.conditionLogic, getVal, sectionVisible)
    states.set(code, state)
  }

  return states
}

// =========================================================
// 自然语言翻译
// =========================================================

const OP_LABELS: Record<string, string> = {}
OPERATORS.forEach(o => { OP_LABELS[o.value] = o.label })

const ACTION_LABELS: Record<string, string> = {}
ACTION_TYPES.forEach(a => { ACTION_LABELS[a.value] = a.label })

/** 将字段代码翻译为可读名称 */
type FieldLabelResolver = (fieldCode: string) => string

function humanizeRule(rule: ConditionRule, getLabel: FieldLabelResolver): string {
  const field = getLabel(rule.field)
  const op = OP_LABELS[rule.operator] || rule.operator
  if (rule.operator === 'isEmpty' || rule.operator === 'isNotEmpty') {
    return `${field}${op}`
  }
  return `${field}${op}${rule.value ?? ''}`
}

function humanizeGroup(group: ConditionGroup, getLabel: FieldLabelResolver): string {
  if (group.rules.length === 0) return ''
  if (group.rules.length === 1) {
    const r = group.rules[0]
    return isConditionGroup(r) ? humanizeGroup(r, getLabel) : humanizeRule(r, getLabel)
  }
  const connector = group.logicOp === 'and' ? ' 且 ' : ' 或 '
  const parts = group.rules.map(r => {
    if (isConditionGroup(r)) {
      return `(${humanizeGroup(r, getLabel)})`
    }
    return humanizeRule(r, getLabel)
  })
  return parts.join(connector)
}

function humanizeActions(actions: ConditionAction[]): string {
  return actions.map(a => {
    const label = ACTION_LABELS[a.type] || a.type
    if (a.type === 'setScore' && a.value !== undefined) {
      return `${label}=${a.value}`
    }
    return label
  }).join(', ')
}

/**
 * 将条件逻辑 V2 对象翻译为可读的自然语言字符串。
 */
export function humanizeCondition(
  v2: ConditionLogicV2,
  getLabel?: FieldLabelResolver,
): string {
  const resolver = getLabel || ((code: string) => code)
  const condStr = humanizeGroup(v2.conditions, resolver)
  const actStr = humanizeActions(v2.actions)
  return `当 ${condStr} → ${actStr}`
}

/**
 * 从 JSON 字符串解析并翻译。
 */
export function humanizeConditionJson(
  json: string | null | undefined,
  getLabel?: FieldLabelResolver,
): string {
  if (!json) return ''
  try {
    const parsed = JSON.parse(json)
    if (isV2(parsed)) return humanizeCondition(parsed, getLabel)
    if (isV1(parsed)) return humanizeCondition(normalizeV1toV2(parsed), getLabel)
    return ''
  } catch {
    return ''
  }
}
