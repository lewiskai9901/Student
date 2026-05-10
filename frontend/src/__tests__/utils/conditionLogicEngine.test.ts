/**
 * F2: conditionLogicEngine 单测.
 *
 * 这是检查平台条件逻辑核心引擎 — 操作符、组合、V1->V2 迁移、
 * 拓扑排序、自然语言翻译, 全是纯函数, 高 ROI 单测覆盖.
 */
import { describe, it, expect, vi } from 'vitest'
import {
  evaluateConditionLogic,
  resolveItemState,
  evaluateSectionVisibility,
  extractFieldRefs,
  buildDependencyGraph,
  topologicalSort,
  evaluateAllConditions,
  normalizeV1toV2,
  humanizeCondition,
  humanizeConditionJson,
} from '@/utils/insp/conditionLogicEngine'
import type { ConditionLogicV2 } from '@/types/insp/conditionLogic'

const v2 = (rules: any[], action: any = { type: 'show' }, op: 'and' | 'or' = 'and'): string =>
  JSON.stringify({
    version: 2,
    conditions: { logicOp: op, rules },
    actions: [action],
  } satisfies ConditionLogicV2)

describe('evaluateConditionLogic', () => {
  it('null/undefined/空 → null', () => {
    const get = () => null
    expect(evaluateConditionLogic(null, get)).toBeNull()
    expect(evaluateConditionLogic(undefined, get)).toBeNull()
    expect(evaluateConditionLogic('', get)).toBeNull()
  })

  it('非法 JSON → null (try/catch)', () => {
    expect(evaluateConditionLogic('{ not json', () => null)).toBeNull()
  })

  it('equals 命中返回 actions', () => {
    const json = v2([{ field: 'q1', operator: 'equals', value: 'A' }])
    const result = evaluateConditionLogic(json, (k) => (k === 'q1' ? 'A' : null))
    expect(result).toEqual([{ type: 'show' }])
  })

  it('equals 不命中返回 null', () => {
    const json = v2([{ field: 'q1', operator: 'equals', value: 'A' }])
    expect(evaluateConditionLogic(json, () => 'B')).toBeNull()
  })

  it('notEquals 操作符', () => {
    const json = v2([{ field: 'q1', operator: 'notEquals', value: 'A' }])
    expect(evaluateConditionLogic(json, () => 'B')).not.toBeNull()
    expect(evaluateConditionLogic(json, () => 'A')).toBeNull()
  })

  it('greaterThan / lessThan', () => {
    const j1 = v2([{ field: 'q', operator: 'greaterThan', value: '10' }])
    expect(evaluateConditionLogic(j1, () => '20')).not.toBeNull()
    expect(evaluateConditionLogic(j1, () => '5')).toBeNull()
    const j2 = v2([{ field: 'q', operator: 'lessThan', value: '10' }])
    expect(evaluateConditionLogic(j2, () => '5')).not.toBeNull()
  })

  it('greaterOrEqual / lessOrEqual 边界', () => {
    const j1 = v2([{ field: 'q', operator: 'greaterOrEqual', value: '10' }])
    expect(evaluateConditionLogic(j1, () => '10')).not.toBeNull()
    const j2 = v2([{ field: 'q', operator: 'lessOrEqual', value: '10' }])
    expect(evaluateConditionLogic(j2, () => '10')).not.toBeNull()
  })

  it('contains 子串', () => {
    const json = v2([{ field: 'q', operator: 'contains', value: 'foo' }])
    expect(evaluateConditionLogic(json, () => 'xfoox')).not.toBeNull()
    expect(evaluateConditionLogic(json, () => 'bar')).toBeNull()
  })

  it('in 集合包含', () => {
    const json = v2([{ field: 'q', operator: 'in', value: 'A, B, C' }])
    expect(evaluateConditionLogic(json, () => 'B')).not.toBeNull()
    expect(evaluateConditionLogic(json, () => 'D')).toBeNull()
  })

  it('between 数值区间', () => {
    const json = v2([{ field: 'q', operator: 'between', value: '5,10' }])
    expect(evaluateConditionLogic(json, () => '7')).not.toBeNull()
    expect(evaluateConditionLogic(json, () => '12')).toBeNull()
    expect(evaluateConditionLogic(json, () => '')).toBeNull()
  })

  it('between 缺逗号 fallback false', () => {
    const json = v2([{ field: 'q', operator: 'between', value: 'invalid' }])
    expect(evaluateConditionLogic(json, () => '5')).toBeNull()
  })

  it('isEmpty / isNotEmpty', () => {
    const e = v2([{ field: 'q', operator: 'isEmpty' }])
    expect(evaluateConditionLogic(e, () => '')).not.toBeNull()
    expect(evaluateConditionLogic(e, () => 'x')).toBeNull()
    const ne = v2([{ field: 'q', operator: 'isNotEmpty' }])
    expect(evaluateConditionLogic(ne, () => 'x')).not.toBeNull()
  })

  it('and 组合 — 全部命中', () => {
    const json = v2([
      { field: 'a', operator: 'equals', value: '1' },
      { field: 'b', operator: 'equals', value: '2' },
    ], { type: 'show' }, 'and')
    expect(evaluateConditionLogic(json, (k) => (k === 'a' ? '1' : '2'))).not.toBeNull()
    expect(evaluateConditionLogic(json, (k) => (k === 'a' ? '1' : 'x'))).toBeNull()
  })

  it('or 组合 — 任一命中', () => {
    const json = v2([
      { field: 'a', operator: 'equals', value: '1' },
      { field: 'b', operator: 'equals', value: '2' },
    ], { type: 'show' }, 'or')
    expect(evaluateConditionLogic(json, (k) => (k === 'a' ? 'x' : '2'))).not.toBeNull()
    expect(evaluateConditionLogic(json, () => 'x')).toBeNull()
  })

  it('空 rules → 视为命中 (true)', () => {
    const json = v2([])
    expect(evaluateConditionLogic(json, () => null)).not.toBeNull()
  })
})

describe('normalizeV1toV2', () => {
  it('转换基础 V1 格式', () => {
    const v1 = { when: 'q1', operator: 'equals', value: 'A', action: 'show' }
    const result = normalizeV1toV2(v1)
    expect(result.version).toBe(2)
    expect(result.conditions.logicOp).toBe('and')
    expect(result.conditions.rules).toEqual([{ field: 'q1', operator: 'equals', value: 'A' }])
    expect(result.actions).toEqual([{ type: 'show' }])
  })

  it('value 为空时省略 value 字段', () => {
    const v1 = { when: 'q1', operator: 'isEmpty', action: 'hide' }
    const result = normalizeV1toV2(v1 as any)
    expect((result.conditions.rules[0] as any).value).toBeUndefined()
  })
})

describe('resolveItemState', () => {
  it('null logic + sectionVisible=true 返回默认 state', () => {
    const state = resolveItemState(null, () => null, true)
    expect(state.visible).toBe(true)
    expect(state.required).toBe(false)
    expect(state.scoreOverride).toBeNull()
  })

  it('sectionVisible=false 强制隐藏', () => {
    const state = resolveItemState(null, () => null, false)
    expect(state.visible).toBe(false)
  })

  it('show 动作 + 条件不满足 → 隐藏', () => {
    const json = v2([{ field: 'q', operator: 'equals', value: 'X' }], { type: 'show' })
    const state = resolveItemState(json, () => 'Y')
    expect(state.visible).toBe(false)
  })

  it('show 动作 + 条件满足 → visible=true', () => {
    const json = v2([{ field: 'q', operator: 'equals', value: 'X' }], { type: 'show' })
    const state = resolveItemState(json, () => 'X')
    expect(state.visible).toBe(true)
  })

  it('hide 动作 + 满足 → visible=false', () => {
    const json = v2([{ field: 'q', operator: 'equals', value: 'X' }], { type: 'hide' })
    const state = resolveItemState(json, () => 'X')
    expect(state.visible).toBe(false)
  })

  it('require 动作', () => {
    const json = v2([{ field: 'q', operator: 'equals', value: 'X' }], { type: 'require' })
    const state = resolveItemState(json, () => 'X')
    expect(state.required).toBe(true)
  })

  it('disable 动作', () => {
    const json = v2([{ field: 'q', operator: 'equals', value: 'X' }], { type: 'disable' })
    const state = resolveItemState(json, () => 'X')
    expect(state.disabled).toBe(true)
  })

  it('setScore 动作 — 数值', () => {
    const json = v2([{ field: 'q', operator: 'equals', value: 'X' }], { type: 'setScore', value: 5 })
    const state = resolveItemState(json, () => 'X')
    expect(state.scoreOverride).toBe(5)
  })

  it('setScore 动作 — 字符串数值转 number', () => {
    const json = v2([{ field: 'q', operator: 'equals', value: 'X' }], { type: 'setScore', value: '8' })
    const state = resolveItemState(json, () => 'X')
    expect(state.scoreOverride).toBe(8)
  })

  it('clearValue 动作', () => {
    const json = v2([{ field: 'q', operator: 'equals', value: 'X' }], { type: 'clearValue' })
    const state = resolveItemState(json, () => 'X')
    expect(state.clearValue).toBe(true)
  })

  it('非法 JSON 视为无条件 → 默认 state', () => {
    const state = resolveItemState('{not json', () => null, true)
    expect(state.visible).toBe(true)
  })
})

describe('evaluateSectionVisibility', () => {
  it('空 → 可见', () => {
    expect(evaluateSectionVisibility(null, () => null)).toBe(true)
  })

  it('show + 不满足 → 隐藏', () => {
    const json = v2([{ field: 'q', operator: 'equals', value: 'X' }], { type: 'show' })
    expect(evaluateSectionVisibility(json, () => 'Y')).toBe(false)
  })

  it('show + 满足 → 可见', () => {
    const json = v2([{ field: 'q', operator: 'equals', value: 'X' }], { type: 'show' })
    expect(evaluateSectionVisibility(json, () => 'X')).toBe(true)
  })

  it('hide + 满足 → 隐藏', () => {
    const json = v2([{ field: 'q', operator: 'equals', value: 'X' }], { type: 'hide' })
    expect(evaluateSectionVisibility(json, () => 'X')).toBe(false)
  })
})

describe('extractFieldRefs', () => {
  it('null/空 返回 []', () => {
    expect(extractFieldRefs(null)).toEqual([])
    expect(extractFieldRefs('')).toEqual([])
  })

  it('从 V2 单条件提取 field', () => {
    const json = v2([{ field: 'q1', operator: 'equals', value: 'X' }])
    expect(extractFieldRefs(json)).toContain('q1')
  })

  it('提取多条件 fields', () => {
    const json = v2([
      { field: 'q1', operator: 'equals', value: 'X' },
      { field: 'q2', operator: 'equals', value: 'Y' },
    ])
    const refs = extractFieldRefs(json)
    expect(refs).toContain('q1')
    expect(refs).toContain('q2')
  })

  it('非法 JSON 返回 []', () => {
    expect(extractFieldRefs('{not json')).toEqual([])
  })

  it('V1 格式 (when 字段)', () => {
    const v1 = JSON.stringify({ when: 'qX', operator: 'equals', value: 'A', action: 'show' })
    expect(extractFieldRefs(v1)).toContain('qX')
  })
})

describe('buildDependencyGraph / topologicalSort', () => {
  it('独立 items 拓扑排序保持顺序', () => {
    const details = [
      { itemCode: 'a', conditionLogic: null, responseValue: null },
      { itemCode: 'b', conditionLogic: null, responseValue: null },
    ]
    const sorted = topologicalSort(details)
    expect(sorted).toHaveLength(2)
    expect(sorted).toContain('a')
    expect(sorted).toContain('b')
  })

  it('依赖 item 排在被依赖 item 之后', () => {
    const details = [
      { itemCode: 'b', conditionLogic: v2([{ field: 'a', operator: 'equals', value: '1' }]), responseValue: null },
      { itemCode: 'a', conditionLogic: null, responseValue: '1' },
    ]
    const sorted = topologicalSort(details)
    expect(sorted.indexOf('a')).toBeLessThan(sorted.indexOf('b'))
  })

  it('循环依赖时回退到原顺序', () => {
    const details = [
      { itemCode: 'a', conditionLogic: v2([{ field: 'b', operator: 'equals', value: '1' }]), responseValue: null },
      { itemCode: 'b', conditionLogic: v2([{ field: 'a', operator: 'equals', value: '1' }]), responseValue: null },
    ]
    const warn = vi.spyOn(console, 'warn').mockImplementation(() => {})
    const sorted = topologicalSort(details)
    expect(sorted).toHaveLength(2)
    warn.mockRestore()
  })

  it('buildDependencyGraph 返回 itemCode → deps 映射', () => {
    const details = [
      { itemCode: 'b', conditionLogic: v2([{ field: 'a', operator: 'equals', value: '1' }]), responseValue: null },
    ]
    const g = buildDependencyGraph(details)
    expect(g.get('b')).toEqual(['a'])
  })
})

describe('evaluateAllConditions', () => {
  it('简单链 a → b 求值', () => {
    const details = [
      { itemCode: 'a', conditionLogic: null, responseValue: 'X' },
      {
        itemCode: 'b',
        conditionLogic: v2([{ field: 'a', operator: 'equals', value: 'X' }], { type: 'show' }),
        responseValue: null,
      },
    ]
    const states = evaluateAllConditions(details)
    expect(states.get('a')?.visible).toBe(true)
    expect(states.get('b')?.visible).toBe(true)
  })

  it('a 隐藏后 b 视 a 值为 null (级联隐藏)', () => {
    const details = [
      {
        itemCode: 'a',
        conditionLogic: v2([{ field: 'x', operator: 'equals', value: 'YES' }], { type: 'show' }),
        responseValue: 'X',
      },
      {
        itemCode: 'b',
        conditionLogic: v2([{ field: 'a', operator: 'equals', value: 'X' }], { type: 'show' }),
        responseValue: null,
      },
    ]
    const states = evaluateAllConditions(details)
    expect(states.get('a')?.visible).toBe(false)
    expect(states.get('b')?.visible).toBe(false)
  })
})

describe('humanize', () => {
  it('humanizeCondition 输出 "当 X 等于 Y > 显示此字段"', () => {
    const v: ConditionLogicV2 = {
      version: 2,
      conditions: { logicOp: 'and', rules: [{ field: 'q1', operator: 'equals', value: 'A' }] },
      actions: [{ type: 'show' }],
    }
    const text = humanizeCondition(v)
    expect(text).toContain('q1')
    expect(text).toContain('等于')
    expect(text).toContain('A')
    expect(text).toContain('显示')
  })

  it('humanizeCondition 多 rules 用 "且" 连接', () => {
    const v: ConditionLogicV2 = {
      version: 2,
      conditions: {
        logicOp: 'and',
        rules: [
          { field: 'q1', operator: 'equals', value: 'A' },
          { field: 'q2', operator: 'equals', value: 'B' },
        ],
      },
      actions: [{ type: 'show' }],
    }
    expect(humanizeCondition(v)).toContain('且')
  })

  it('humanizeCondition logicOp=or 用 "或"', () => {
    const v: ConditionLogicV2 = {
      version: 2,
      conditions: {
        logicOp: 'or',
        rules: [
          { field: 'q1', operator: 'equals', value: 'A' },
          { field: 'q2', operator: 'equals', value: 'B' },
        ],
      },
      actions: [{ type: 'show' }],
    }
    expect(humanizeCondition(v)).toContain('或')
  })

  it('humanizeCondition setScore 显示 = 值', () => {
    const v: ConditionLogicV2 = {
      version: 2,
      conditions: { logicOp: 'and', rules: [{ field: 'q', operator: 'equals', value: 'A' }] },
      actions: [{ type: 'setScore', value: 5 }],
    }
    expect(humanizeCondition(v)).toContain('=5')
  })

  it('humanizeCondition isEmpty 不附加 value', () => {
    const v: ConditionLogicV2 = {
      version: 2,
      conditions: { logicOp: 'and', rules: [{ field: 'q', operator: 'isEmpty' }] },
      actions: [{ type: 'hide' }],
    }
    expect(humanizeCondition(v)).toContain('为空')
  })

  it('humanizeCondition getLabel 替换字段标签', () => {
    const v: ConditionLogicV2 = {
      version: 2,
      conditions: { logicOp: 'and', rules: [{ field: 'q1', operator: 'equals', value: 'A' }] },
      actions: [{ type: 'show' }],
    }
    expect(humanizeCondition(v, () => '问题1')).toContain('问题1')
  })

  it('humanizeConditionJson null/空返回空串', () => {
    expect(humanizeConditionJson(null)).toBe('')
    expect(humanizeConditionJson('')).toBe('')
  })

  it('humanizeConditionJson 非法 JSON 返回空串', () => {
    expect(humanizeConditionJson('{ bad')).toBe('')
  })

  it('humanizeConditionJson V1 自动迁移再翻译', () => {
    const v1 = JSON.stringify({ when: 'q1', operator: 'equals', value: 'A', action: 'show' })
    expect(humanizeConditionJson(v1)).toContain('q1')
  })
})
