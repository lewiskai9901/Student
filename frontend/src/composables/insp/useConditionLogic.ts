/**
 * V7 检查平台 - 条件逻辑求值引擎 Composable
 *
 * 根据表单数据对 ConditionLogicV2 进行求值，
 * 输出每个字段的可见性、必填性、禁用状态等。
 */
import { ref } from 'vue'
import type {
  ConditionLogicV2,
  ConditionRule,
  ConditionGroup,
  ItemConditionState,
} from '@/types/insp/conditionLogic'
import {
  isConditionGroup,
  DEFAULT_CONDITION_STATE,
} from '@/types/insp/conditionLogic'

export function useConditionLogic() {
  const visibilityMap = ref<Record<string, boolean>>({})
  const requiredMap = ref<Record<string, boolean>>({})
  const disabledMap = ref<Record<string, boolean>>({})
  const scoreOverrideMap = ref<Record<string, number | null>>({})
  const clearValueSet = ref<Set<string>>(new Set())

  /**
   * 对所有条件进行求值，更新各状态 map
   * @param conditions 字段 ID -> ConditionLogicV2 的映射
   * @param formData   当前表单数据 (字段 ID/code -> 值)
   */
  function evaluateAll(
    conditions: Record<string, ConditionLogicV2>,
    formData: Record<string, any>,
  ) {
    const visibility: Record<string, boolean> = {}
    const required: Record<string, boolean> = {}
    const disabled: Record<string, boolean> = {}
    const scores: Record<string, number | null> = {}
    const clears = new Set<string>()

    for (const [itemId, logic] of Object.entries(conditions)) {
      if (!logic || !logic.conditions || !logic.actions) {
        visibility[itemId] = true
        continue
      }

      const match = evaluateGroup(logic.conditions, formData)

      for (const action of logic.actions) {
        switch (action.type) {
          case 'show':
            visibility[itemId] = match
            break
          case 'hide':
            visibility[itemId] = !match
            break
          case 'require':
            required[itemId] = match
            break
          case 'disable':
            disabled[itemId] = match
            break
          case 'setScore':
            scores[itemId] = match ? (action.value as number ?? null) : null
            break
          case 'clearValue':
            if (match) clears.add(itemId)
            break
        }
      }

      // 没有显式设置可见性时默认为可见
      if (visibility[itemId] === undefined) {
        visibility[itemId] = true
      }
    }

    visibilityMap.value = visibility
    requiredMap.value = required
    disabledMap.value = disabled
    scoreOverrideMap.value = scores
    clearValueSet.value = clears
  }

  /**
   * 获取单个字段的完整条件状态
   */
  function getItemState(itemId: string): ItemConditionState {
    return {
      visible: visibilityMap.value[itemId] ?? DEFAULT_CONDITION_STATE.visible,
      required: requiredMap.value[itemId] ?? DEFAULT_CONDITION_STATE.required,
      disabled: disabledMap.value[itemId] ?? DEFAULT_CONDITION_STATE.disabled,
      scoreOverride: scoreOverrideMap.value[itemId] ?? DEFAULT_CONDITION_STATE.scoreOverride,
      clearValue: clearValueSet.value.has(itemId),
    }
  }

  /**
   * 求值一个条件组 (AND / OR 逻辑)
   */
  function evaluateGroup(group: ConditionGroup, formData: Record<string, any>): boolean {
    if (!group.rules || group.rules.length === 0) return true

    if (group.logicOp === 'and') {
      return group.rules.every((node) =>
        isConditionGroup(node)
          ? evaluateGroup(node, formData)
          : evaluateRule(node, formData),
      )
    } else {
      return group.rules.some((node) =>
        isConditionGroup(node)
          ? evaluateGroup(node, formData)
          : evaluateRule(node, formData),
      )
    }
  }

  /**
   * 求值单条规则
   */
  function evaluateRule(rule: ConditionRule, formData: Record<string, any>): boolean {
    const fieldValue = formData[rule.field]
    const targetValue = rule.value

    switch (rule.operator) {
      case 'equals':
        return String(fieldValue) === String(targetValue)
      case 'notEquals':
        return String(fieldValue) !== String(targetValue)
      case 'greaterThan':
        return Number(fieldValue) > Number(targetValue)
      case 'lessThan':
        return Number(fieldValue) < Number(targetValue)
      case 'greaterOrEqual':
        return Number(fieldValue) >= Number(targetValue)
      case 'lessOrEqual':
        return Number(fieldValue) <= Number(targetValue)
      case 'contains':
        return String(fieldValue ?? '').includes(String(targetValue ?? ''))
      case 'in': {
        // targetValue 可能是逗号分隔的字符串或 JSON 数组
        let list: string[]
        try {
          list = JSON.parse(targetValue ?? '[]')
        } catch {
          list = String(targetValue ?? '').split(',').map(s => s.trim())
        }
        return list.includes(String(fieldValue))
      }
      case 'between': {
        // targetValue 格式: "min,max" 或 JSON [min, max]
        let bounds: [number, number]
        try {
          const parsed = JSON.parse(targetValue ?? '[]')
          bounds = [Number(parsed[0]), Number(parsed[1])]
        } catch {
          const parts = String(targetValue ?? '').split(',')
          bounds = [Number(parts[0]), Number(parts[1])]
        }
        const num = Number(fieldValue)
        return num >= bounds[0] && num <= bounds[1]
      }
      case 'isEmpty':
        return fieldValue === null || fieldValue === undefined || fieldValue === ''
      case 'isNotEmpty':
        return fieldValue !== null && fieldValue !== undefined && fieldValue !== ''
      default:
        return false
    }
  }

  return {
    visibilityMap,
    requiredMap,
    disabledMap,
    scoreOverrideMap,
    clearValueSet,
    evaluateAll,
    evaluateGroup,
    evaluateRule,
    getItemState,
  }
}
