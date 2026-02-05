/**
 * 计分系统类型定义
 */

// 计分策略
export interface ScoringStrategy {
  id: number
  code: string
  name: string
  description: string
  category: string
  categoryName: string
  formulaTemplate: string
  formulaDescription: string
  parametersSchema: Record<string, any>
  defaultParameters: Record<string, any>
  supportedInputTypes: string[]
  supportedRuleTypes: string[]
  isSystem: boolean
  isEnabled: boolean
  sortOrder: number
}

export interface CreateScoringStrategyCommand {
  code: string
  name: string
  description?: string
  category: string
  formulaTemplate: string
  formulaDescription?: string
  parametersSchema?: Record<string, any>
  defaultParameters?: Record<string, any>
  supportedInputTypes?: string[]
}

export interface UpdateScoringStrategyCommand {
  name: string
  description?: string
  category: string
  formulaTemplate: string
  formulaDescription?: string
  parametersSchema?: Record<string, any>
  defaultParameters?: Record<string, any>
  supportedInputTypes?: string[]
}

// 打分方式
export interface InputType {
  id: number
  code: string
  name: string
  description: string
  category: string
  componentType: string
  componentTypeName: string
  componentConfig: Record<string, any>
  valueType: string
  valueMapping: Record<string, any>
  validationRules: Record<string, any>
  isSystem: boolean
  isEnabled: boolean
  sortOrder: number
}

export interface CreateInputTypeCommand {
  code: string
  name: string
  description?: string
  category: string
  componentType: string
  componentConfig?: Record<string, any>
  valueType: string
  valueMapping?: Record<string, any>
  validationRules?: Record<string, any>
}

export interface UpdateInputTypeCommand {
  name: string
  description?: string
  category: string
  componentType: string
  componentConfig?: Record<string, any>
  valueType: string
  valueMapping?: Record<string, any>
  validationRules?: Record<string, any>
}

// 计算规则
export interface CalculationRule {
  id: number
  code: string
  name: string
  description: string
  ruleType: string
  ruleTypeName: string
  conditionFormula: string
  actionFormula: string
  parametersSchema: Record<string, any>
  defaultParameters: Record<string, any>
  priority: number
  stopOnMatch: boolean
  isSystem: boolean
  isEnabled: boolean
}

export interface CreateCalculationRuleCommand {
  code: string
  name: string
  description?: string
  ruleType: string
  conditionFormula: string
  actionFormula: string
  parametersSchema?: Record<string, any>
  defaultParameters?: Record<string, any>
  priority?: number
  stopOnMatch?: boolean
}

export interface UpdateCalculationRuleCommand {
  name: string
  description?: string
  ruleType: string
  conditionFormula: string
  actionFormula: string
  parametersSchema?: Record<string, any>
  defaultParameters?: Record<string, any>
  priority?: number
  stopOnMatch?: boolean
}

// 内置函数
export interface FormulaFunction {
  id: number
  name: string
  description: string
  category: string
  parametersDef: Array<{
    name: string
    type: string
    variadic?: boolean
    default?: any
  }>
  returnType: string
  implementation: string
  examples: Array<{
    input: string
    output: string
    description?: string
  }>
  isSystem: boolean
  isEnabled: boolean
}

// 内置变量
export interface FormulaVariable {
  id: number
  name: string
  description: string
  category: string
  valueType: string
  defaultValue: string
  sourceDescription: string
  isSystem: boolean
  isEnabled: boolean
}

// 公式验证结果
export interface FormulaValidationResult {
  formula: string
  valid: boolean
  errorMessage?: string
  usedFunctions?: string[]
  usedVariables?: string[]
}

// 策略分类
export const StrategyCategories = {
  basic: '基础策略',
  grade: '等级策略',
  advanced: '高级策略',
  time: '时间策略'
} as const

// 组件类型
export const ComponentTypes = {
  number: '数字输入',
  select: '下拉选择',
  checkbox: '勾选确认',
  slider: '滑动条',
  star: '星级评分',
  grade: '等级选择',
  textarea: '文本输入'
} as const

// 值类型
export const ValueTypes = {
  number: '数字',
  string: '字符串',
  boolean: '布尔值',
  array: '数组',
  object: '对象',
  any: '任意类型'
} as const

// 规则类型
export const RuleTypes = {
  ceiling: '封顶规则',
  floor: '保底规则',
  veto: '一票否决',
  progressive: '递进扣分',
  bonus: '加分限制',
  penalty: '惩罚系数'
} as const
