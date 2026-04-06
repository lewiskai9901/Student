// ==================== 触发点 ====================

/** 触发点 */
export interface TriggerPoint {
  id: number
  moduleCode: string
  moduleName: string
  pointCode: string
  pointName: string
  description?: string
  contextSchema?: Record<string, unknown> | string
  isEnabled: number
  sortOrder: number
  tenantId?: number
  createdAt?: string
  updatedAt?: string
}

// ==================== 事件触发器 ====================

/** 条件运算符 */
export type ConditionOperator = '==' | '!=' | '>' | '>=' | '<' | '<=' | 'in' | 'not_in' | 'contains' | 'starts_with'

/** 条件项 */
export interface ConditionItem {
  field: string
  operator: ConditionOperator
  value: unknown
}

/** 条件组 (AND 逻辑) */
export interface ConditionGroup {
  all?: ConditionItem[]
  any?: ConditionItem[]
}

/** 事件触发器 */
export interface EventTrigger {
  id: number
  name: string
  triggerPointCode: string
  conditionJson?: ConditionGroup | string | null
  eventTypeMode: 'FIXED' | 'DYNAMIC'
  eventTypeCode?: string | null
  eventTypeSource?: string | null
  subjectType: string
  subjectSource: string
  subjectNameSource?: string | null
  relatedSources?: Record<string, string> | string | null
  payloadFields?: string[] | string | null
  description?: string
  isEnabled: number
  sortOrder: number
  tenantId?: number
  createdAt?: string
  updatedAt?: string
  // JOIN fields
  trigger_point_name?: string
  module_code?: string
  module_name?: string
}

// ==================== 事件类型 ====================

/** 极性 */
export type Polarity = 'POSITIVE' | 'NEGATIVE' | 'NEUTRAL' | 'INFO'

/** 事件类��� */
export interface EventType {
  id: number
  typeCode: string
  typeName: string
  categoryCode: string
  categoryName?: string
  polarity: Polarity
  defaultSeverity: number
  iconName?: string
  colorHex?: string
  description?: string
  isEnabled: number
  sortOrder: number
  tenantId?: number
  createdAt?: string
  updatedAt?: string
}

/** 事件类型分类 */
export interface EventTypeCategory {
  categoryCode: string
  categoryName: string
  types: EventType[]
}

// ==================== 实体事件 ====================

/** 实体事件 */
export interface EntityEvent {
  id: number
  tenantId?: number
  subjectType: string
  subjectId: number
  subjectName: string
  eventCategory: string
  eventType: string
  eventLabel: string
  payload?: Record<string, unknown> | string | null
  sourceModule?: string
  createdBy?: number | null
  createdByName?: string | null
  occurredAt: string
  createdAt: string
}

/** 事件统计项 */
export interface EventStatItem {
  eventType: string
  eventLabel: string
  count: number
}

// ==================== 常量 ====================

/** 极性配色 */
export const POLARITY_CONFIG: Record<Polarity, { label: string; color: string; tagType: string }> = {
  POSITIVE: { label: '正面', color: '#67C23A', tagType: 'success' },
  NEGATIVE: { label: '负面', color: '#F56C6C', tagType: 'danger' },
  NEUTRAL: { label: '中性', color: '#909399', tagType: 'info' },
  INFO: { label: '信息', color: '#409EFF', tagType: '' },
}

/** 主体类型 */
export const SUBJECT_TYPES = [
  { value: 'USER', label: '用户' },
  { value: 'ORG_UNIT', label: '组织' },
  { value: 'PLACE', label: '场所' },
] as const

/** 条件运算符选项 */
export const CONDITION_OPERATORS: { value: ConditionOperator; label: string }[] = [
  { value: '==', label: '等于' },
  { value: '!=', label: '不等于' },
  { value: '>', label: '大于' },
  { value: '>=', label: '大于等于' },
  { value: '<', label: '小于' },
  { value: '<=', label: '小于等于' },
  { value: 'in', label: '包含于' },
  { value: 'not_in', label: '不包含于' },
  { value: 'contains', label: '包含' },
  { value: 'starts_with', label: '开头匹配' },
]

/** 事件类型模式选项 */
export const EVENT_TYPE_MODES = [
  { value: 'FIXED', label: '固定类型' },
  { value: 'DYNAMIC', label: '动态类型' },
] as const
