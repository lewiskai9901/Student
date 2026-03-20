// ===== 条件逻辑 V2 类型定义 =====

// --- 操作符 ---
export type Operator =
  | 'equals'
  | 'notEquals'
  | 'greaterThan'
  | 'lessThan'
  | 'greaterOrEqual'
  | 'lessOrEqual'
  | 'contains'
  | 'in'
  | 'between'
  | 'isEmpty'
  | 'isNotEmpty'

export interface OperatorMeta {
  value: Operator
  label: string
  needsValue: boolean
}

export const OPERATORS: OperatorMeta[] = [
  { value: 'equals', label: '等于', needsValue: true },
  { value: 'notEquals', label: '不等于', needsValue: true },
  { value: 'greaterThan', label: '大于', needsValue: true },
  { value: 'lessThan', label: '小于', needsValue: true },
  { value: 'greaterOrEqual', label: '大于等于', needsValue: true },
  { value: 'lessOrEqual', label: '小于等于', needsValue: true },
  { value: 'contains', label: '包含', needsValue: true },
  { value: 'in', label: '属于', needsValue: true },
  { value: 'between', label: '介于', needsValue: true },
  { value: 'isEmpty', label: '为空', needsValue: false },
  { value: 'isNotEmpty', label: '不为空', needsValue: false },
]

export function operatorNeedsValue(op: Operator): boolean {
  return op !== 'isEmpty' && op !== 'isNotEmpty'
}

// --- 动作 ---
export type ActionType = 'show' | 'hide' | 'require' | 'disable' | 'setScore' | 'clearValue'

export interface ActionMeta {
  value: ActionType
  label: string
  hasExtra: boolean
}

export const ACTION_TYPES: ActionMeta[] = [
  { value: 'show', label: '显示此字段', hasExtra: false },
  { value: 'hide', label: '隐藏此字段', hasExtra: false },
  { value: 'require', label: '设为必填', hasExtra: false },
  { value: 'disable', label: '禁用此字段', hasExtra: false },
  { value: 'setScore', label: '设置分值', hasExtra: true },
  { value: 'clearValue', label: '清空响应值', hasExtra: false },
]

// --- 条件规则 ---
export interface ConditionRule {
  field: string
  operator: Operator
  value?: string
}

// --- 条件组 ---
export type LogicOp = 'and' | 'or'

export interface ConditionGroup {
  logicOp: LogicOp
  rules: (ConditionRule | ConditionGroup)[]
}

// --- 动作定义 ---
export interface ConditionAction {
  type: ActionType
  value?: number | string
}

// --- V1 格式（旧，仅用于迁移检测） ---
export interface ConditionLogicV1 {
  when: string
  operator: string
  value?: string
  action: string
}

// --- V2 格式 ---
export interface ConditionLogicV2 {
  version: 2
  conditions: ConditionGroup
  actions: ConditionAction[]
}

// --- 类型守卫 ---
export function isConditionGroup(node: ConditionRule | ConditionGroup): node is ConditionGroup {
  return 'logicOp' in node && 'rules' in node
}

export function isConditionRule(node: ConditionRule | ConditionGroup): node is ConditionRule {
  return 'field' in node && 'operator' in node
}

export function isV2(json: any): json is ConditionLogicV2 {
  return json && json.version === 2 && json.conditions && json.actions
}

export function isV1(json: any): json is ConditionLogicV1 {
  return json && json.when && !json.version
}

// --- Item 条件状态（引擎求值结果） ---
export interface ItemConditionState {
  visible: boolean
  required: boolean
  disabled: boolean
  scoreOverride: number | null
  clearValue: boolean
}

export const DEFAULT_CONDITION_STATE: ItemConditionState = {
  visible: true,
  required: false,
  disabled: false,
  scoreOverride: null,
  clearValue: false,
}

// --- 嵌套深度限制 ---
export const MAX_NESTING_DEPTH = 3

// --- 空条件模板 ---
export function createEmptyRule(): ConditionRule {
  return { field: '', operator: 'equals', value: '' }
}

export function createEmptyGroup(logicOp: LogicOp = 'and'): ConditionGroup {
  return { logicOp, rules: [createEmptyRule()] }
}

export function createEmptyV2(): ConditionLogicV2 {
  return {
    version: 2,
    conditions: createEmptyGroup('and'),
    actions: [{ type: 'show' }],
  }
}
