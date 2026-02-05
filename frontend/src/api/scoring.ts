/**
 * 计分系统API
 */
import request from '@/utils/request'
import type {
  ScoringStrategy,
  CreateScoringStrategyCommand,
  UpdateScoringStrategyCommand,
  InputType,
  CreateInputTypeCommand,
  UpdateInputTypeCommand,
  CalculationRule,
  CreateCalculationRuleCommand,
  UpdateCalculationRuleCommand,
  FormulaFunction,
  FormulaVariable,
  FormulaValidationResult
} from '@/types/scoring'

// ==================== 计分策略 API ====================

/**
 * 获取所有策略（按分类分组）
 */
export function getStrategiesGrouped() {
  return request<Record<string, ScoringStrategy[]>>({
    url: '/scoring/strategies/grouped',
    method: 'get'
  })
}

/**
 * 获取所有策略
 */
export function getAllStrategies() {
  return request<ScoringStrategy[]>({
    url: '/scoring/strategies',
    method: 'get'
  })
}

/**
 * 按分类获取策略
 */
export function getStrategiesByCategory(category: string) {
  return request<ScoringStrategy[]>({
    url: `/scoring/strategies/category/${category}`,
    method: 'get'
  })
}

/**
 * 根据ID获取策略
 */
export function getStrategyById(id: number) {
  return request<ScoringStrategy>({
    url: `/scoring/strategies/${id}`,
    method: 'get'
  })
}

/**
 * 根据代码获取策略
 */
export function getStrategyByCode(code: string) {
  return request<ScoringStrategy>({
    url: `/scoring/strategies/code/${code}`,
    method: 'get'
  })
}

/**
 * 创建策略
 */
export function createStrategy(data: CreateScoringStrategyCommand) {
  return request<ScoringStrategy>({
    url: '/scoring/strategies',
    method: 'post',
    data
  })
}

/**
 * 更新策略
 */
export function updateStrategy(id: number, data: UpdateScoringStrategyCommand) {
  return request<ScoringStrategy>({
    url: `/scoring/strategies/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除策略
 */
export function deleteStrategy(id: number) {
  return request<void>({
    url: `/scoring/strategies/${id}`,
    method: 'delete'
  })
}

/**
 * 启用/禁用策略
 */
export function toggleStrategy(id: number, enabled: boolean) {
  return request<void>({
    url: `/scoring/strategies/${id}/toggle`,
    method: 'patch',
    params: { enabled }
  })
}

// ==================== 打分方式 API ====================

/**
 * 获取所有打分方式（按分类分组）
 */
export function getInputTypesGrouped() {
  return request<Record<string, InputType[]>>({
    url: '/scoring/input-types/grouped',
    method: 'get'
  })
}

/**
 * 获取所有打分方式
 */
export function getAllInputTypes() {
  return request<InputType[]>({
    url: '/scoring/input-types',
    method: 'get'
  })
}

/**
 * 按分类获取打分方式
 */
export function getInputTypesByCategory(category: string) {
  return request<InputType[]>({
    url: `/scoring/input-types/category/${category}`,
    method: 'get'
  })
}

/**
 * 根据ID获取打分方式
 */
export function getInputTypeById(id: number) {
  return request<InputType>({
    url: `/scoring/input-types/${id}`,
    method: 'get'
  })
}

/**
 * 根据代码获取打分方式
 */
export function getInputTypeByCode(code: string) {
  return request<InputType>({
    url: `/scoring/input-types/code/${code}`,
    method: 'get'
  })
}

/**
 * 创建打分方式
 */
export function createInputType(data: CreateInputTypeCommand) {
  return request<InputType>({
    url: '/scoring/input-types',
    method: 'post',
    data
  })
}

/**
 * 更新打分方式
 */
export function updateInputType(id: number, data: UpdateInputTypeCommand) {
  return request<InputType>({
    url: `/scoring/input-types/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除打分方式
 */
export function deleteInputType(id: number) {
  return request<void>({
    url: `/scoring/input-types/${id}`,
    method: 'delete'
  })
}

/**
 * 启用/禁用打分方式
 */
export function toggleInputType(id: number, enabled: boolean) {
  return request<void>({
    url: `/scoring/input-types/${id}/toggle`,
    method: 'patch',
    params: { enabled }
  })
}

// ==================== 计算规则 API ====================

/**
 * 获取所有规则（按类型分组）
 */
export function getRulesGrouped() {
  return request<Record<string, CalculationRule[]>>({
    url: '/scoring/rules/grouped',
    method: 'get'
  })
}

/**
 * 获取所有规则
 */
export function getAllRules() {
  return request<CalculationRule[]>({
    url: '/scoring/rules',
    method: 'get'
  })
}

/**
 * 按类型获取规则
 */
export function getRulesByType(ruleType: string) {
  return request<CalculationRule[]>({
    url: `/scoring/rules/type/${ruleType}`,
    method: 'get'
  })
}

/**
 * 根据ID获取规则
 */
export function getRuleById(id: number) {
  return request<CalculationRule>({
    url: `/scoring/rules/${id}`,
    method: 'get'
  })
}

/**
 * 根据代码获取规则
 */
export function getRuleByCode(code: string) {
  return request<CalculationRule>({
    url: `/scoring/rules/code/${code}`,
    method: 'get'
  })
}

/**
 * 创建规则
 */
export function createRule(data: CreateCalculationRuleCommand) {
  return request<CalculationRule>({
    url: '/scoring/rules',
    method: 'post',
    data
  })
}

/**
 * 更新规则
 */
export function updateRule(id: number, data: UpdateCalculationRuleCommand) {
  return request<CalculationRule>({
    url: `/scoring/rules/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除规则
 */
export function deleteRule(id: number) {
  return request<void>({
    url: `/scoring/rules/${id}`,
    method: 'delete'
  })
}

/**
 * 启用/禁用规则
 */
export function toggleRule(id: number, enabled: boolean) {
  return request<void>({
    url: `/scoring/rules/${id}/toggle`,
    method: 'patch',
    params: { enabled }
  })
}

/**
 * 调整规则优先级
 */
export function updateRulePriority(id: number, priority: number) {
  return request<void>({
    url: `/scoring/rules/${id}/priority`,
    method: 'patch',
    params: { priority }
  })
}

// ==================== 公式资源 API ====================

/**
 * 获取所有公式资源（函数+变量）
 */
export function getAllFormulaResources() {
  return request<{
    functions: Record<string, FormulaFunction[]>
    variables: Record<string, FormulaVariable[]>
  }>({
    url: '/scoring/formula/resources',
    method: 'get'
  })
}

/**
 * 获取所有内置函数（按分类分组）
 */
export function getFunctionsGrouped() {
  return request<Record<string, FormulaFunction[]>>({
    url: '/scoring/formula/functions/grouped',
    method: 'get'
  })
}

/**
 * 获取所有内置函数
 */
export function getAllFunctions() {
  return request<FormulaFunction[]>({
    url: '/scoring/formula/functions',
    method: 'get'
  })
}

/**
 * 根据名称获取函数
 */
export function getFunctionByName(name: string) {
  return request<FormulaFunction>({
    url: `/scoring/formula/functions/${name}`,
    method: 'get'
  })
}

/**
 * 获取所有内置变量（按分类分组）
 */
export function getVariablesGrouped() {
  return request<Record<string, FormulaVariable[]>>({
    url: '/scoring/formula/variables/grouped',
    method: 'get'
  })
}

/**
 * 获取所有内置变量
 */
export function getAllVariables() {
  return request<FormulaVariable[]>({
    url: '/scoring/formula/variables',
    method: 'get'
  })
}

/**
 * 根据名称获取变量
 */
export function getVariableByName(name: string) {
  return request<FormulaVariable>({
    url: `/scoring/formula/variables/${name}`,
    method: 'get'
  })
}

/**
 * 验证公式语法
 */
export function validateFormula(formula: string) {
  return request<FormulaValidationResult>({
    url: '/scoring/formula/validate',
    method: 'post',
    data: { formula }
  })
}

// ==================== 导出所有API ====================

export const scoringApi = {
  // 策略
  getStrategiesGrouped,
  getAllStrategies,
  getStrategiesByCategory,
  getStrategyById,
  getStrategyByCode,
  createStrategy,
  updateStrategy,
  deleteStrategy,
  toggleStrategy,

  // 打分方式
  getInputTypesGrouped,
  getAllInputTypes,
  getInputTypesByCategory,
  getInputTypeById,
  getInputTypeByCode,
  createInputType,
  updateInputType,
  deleteInputType,
  toggleInputType,

  // 规则
  getRulesGrouped,
  getAllRules,
  getRulesByType,
  getRuleById,
  getRuleByCode,
  createRule,
  updateRule,
  deleteRule,
  toggleRule,
  updateRulePriority,

  // 公式资源
  getAllFormulaResources,
  getFunctionsGrouped,
  getAllFunctions,
  getFunctionByName,
  getVariablesGrouped,
  getAllVariables,
  getVariableByName,
  validateFormula
}

export default scoringApi
