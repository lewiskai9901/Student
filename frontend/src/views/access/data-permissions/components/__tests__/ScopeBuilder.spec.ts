import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'

// ── Mock APIs: 关系/类型选项全来自 API (无行业硬编码) ───────────────
const relationListMock = vi.fn()
const entityListMock = vi.fn()

vi.mock('@/api/relationType', () => ({
  relationTypeApi: {
    list: (...args: any[]) => relationListMock(...args),
  },
}))
vi.mock('@/api/entityType', () => ({
  entityTypeApi: {
    list: (...args: any[]) => entityListMock(...args),
  },
}))

// CustomScopeTreePicker 拉组织树 (getOrgUnitTree) — 存根掉, 本组件测试不关心其内部
vi.mock('@/api/organization', () => ({
  getOrgUnitTree: vi.fn().mockResolvedValue([]),
}))

import ScopeBuilder, { type ScopeSpecVM } from '../ScopeBuilder.vue'

const RELATIONS = [
  { relationCode: 'admin', relationName: '管理', toType: 'ORG_UNIT', fromType: 'USER' },
  { relationCode: 'member', relationName: '成员', toType: 'ORG_UNIT', fromType: 'USER' },
  { relationCode: 'mentor', relationName: '指导', toType: 'USER', fromType: 'USER' },
]

const ENTITY_TYPES = [
  { typeCode: 'TYPE_A', typeName: '类型甲' },
  { typeCode: 'TYPE_B', typeName: '类型乙' },
]

// 项目全局 setup 把 el-select/el-option 等存根为 `true` (不渲染 label/slot).
// 本测试需要 option 标签出现在 DOM 且事件能透传, 故用本地存根覆盖:
//  - el-select / el-radio-group / el-checkbox: 渲染默认插槽 + 透传 update:model-value
//  - el-option / el-radio-button: 把 label/插槽文本渲染出来 (断言"标签来自 API")
const localStubs = {
  'el-select': {
    name: 'el-select',
    props: ['modelValue'],
    emits: ['update:model-value'],
    template: '<div class="el-select-stub"><slot /></div>',
  },
  'el-option': {
    name: 'el-option',
    props: ['label', 'value'],
    template: '<div class="el-option-stub">{{ label }}<slot /></div>',
  },
  'el-radio-group': {
    name: 'el-radio-group',
    props: ['modelValue'],
    emits: ['update:model-value'],
    template: '<div class="el-radio-group-stub"><slot /></div>',
  },
  'el-radio-button': {
    name: 'el-radio-button',
    props: ['value'],
    template: '<div class="el-radio-button-stub"><slot /></div>',
  },
  'el-checkbox': {
    name: 'el-checkbox',
    props: ['modelValue'],
    emits: ['update:model-value'],
    template: '<div class="el-checkbox-stub"><slot /></div>',
  },
}

function mountBuilder(opts: {
  modelValue?: ScopeSpecVM
  capabilities?: any
  axisOnlyOrg?: boolean
  disabled?: boolean
}) {
  return mount(ScopeBuilder, {
    props: {
      modelValue: opts.modelValue ?? {},
      capabilities: opts.capabilities,
      axisOnlyOrg: opts.axisOnlyOrg,
      disabled: opts.disabled,
    },
    global: {
      stubs: localStubs,
    },
  })
}

/** 所有 el-select 存根 (顺序: axis① 锚点 → [关系] → [类型]) */
function selects(wrapper: any) {
  return wrapper.findAllComponents({ name: 'el-select' })
}

/** 渲染后的 HTML, 去掉 <!-- 注释 -->（模板里的中文注释会污染 substring 断言）。 */
function visibleHtml(wrapper: any): string {
  return wrapper.html().replace(/<!--[\s\S]*?-->/g, '')
}

/** 轴② 是否渲染: 看 el-radio-group 存根是否存在 */
function hasRelationAxis(wrapper: any): boolean {
  return wrapper.findAllComponents({ name: 'el-radio-group' }).length > 0
}

beforeEach(() => {
  relationListMock.mockResolvedValue(RELATIONS)
  entityListMock.mockResolvedValue(ENTITY_TYPES)
})

describe('ScopeBuilder — axis gating', () => {
  it('axisOnlyOrg=true → 轴② 和 轴③ 不渲染', async () => {
    const wrapper = mountBuilder({
      modelValue: { orgAnchor: 'PRIMARY_ORG' },
      capabilities: { relationFilterable: true, typeEntity: 'USER' },
      axisOnlyOrg: true,
    })
    await flushPromises()
    // 仅轴① — 不渲染 轴② (radio-group) / 轴③ (类型标题文本)
    expect(visibleHtml(wrapper)).toContain('组织锚点')
    expect(hasRelationAxis(wrapper)).toBe(false)
    expect(visibleHtml(wrapper)).not.toContain('类型过滤')
  })

  it('relationFilterable=false → 轴② 不渲染', async () => {
    const wrapper = mountBuilder({
      modelValue: { orgAnchor: 'PRIMARY_ORG' },
      capabilities: { relationFilterable: false, typeEntity: 'USER' },
    })
    await flushPromises()
    expect(hasRelationAxis(wrapper)).toBe(false)
    // 轴③ 仍在 (typeEntity 非空)
    expect(visibleHtml(wrapper)).toContain('类型过滤')
  })

  it('typeEntity=null → 轴③ 不渲染', async () => {
    const wrapper = mountBuilder({
      modelValue: { orgAnchor: 'PRIMARY_ORG' },
      capabilities: { relationFilterable: true, typeEntity: null },
    })
    await flushPromises()
    expect(hasRelationAxis(wrapper)).toBe(true)
    expect(visibleHtml(wrapper)).not.toContain('类型过滤')
  })
})

describe('ScopeBuilder — axis① org anchor', () => {
  it('选 RELATION + 关系 → emit update:modelValue 带 orgAnchor=RELATION + anchorParam', async () => {
    const wrapper = mountBuilder({
      modelValue: { orgAnchor: 'SELF' },
      capabilities: {},
    })
    await flushPromises()

    // 第一个 el-select = 锚点选择器
    const anchorSelect = selects(wrapper)[0]
    anchorSelect.vm.$emit('update:model-value', 'RELATION')
    await flushPromises()

    let emitted = wrapper.emitted('update:modelValue')!
    expect(emitted.length).toBeGreaterThan(0)
    expect((emitted.at(-1)![0] as ScopeSpecVM).orgAnchor).toBe('RELATION')

    // 重新挂到 RELATION 态 (受控组件), 关系下拉应出现
    await wrapper.setProps({ modelValue: { orgAnchor: 'RELATION' } })
    await flushPromises()
    const relSelect = selects(wrapper)[1] // 锚点选择器之后是关系选择器
    relSelect.vm.$emit('update:model-value', 'admin')
    await flushPromises()

    emitted = wrapper.emitted('update:modelValue')!
    expect((emitted.at(-1)![0] as ScopeSpecVM).anchorParam).toBe('admin')
  })

  it('orgAnchorRelations 只含 toType=ORG_UNIT 的关系 (mentor 被排除)', async () => {
    const wrapper = mountBuilder({
      modelValue: { orgAnchor: 'RELATION' },
      capabilities: {},
    })
    await flushPromises()
    // 关系下拉的 option 标签来自 API; mentor (toType=USER) 不应出现
    const html = wrapper.html()
    expect(html).toContain('管理')
    expect(html).toContain('成员')
    expect(html).not.toContain('指导')
  })
})

describe('ScopeBuilder — axis② relation filter', () => {
  it('设排除关系 → emit subjectRelExclude', async () => {
    const wrapper = mountBuilder({
      modelValue: { orgAnchor: 'PRIMARY_ORG' },
      capabilities: { relationFilterable: true },
    })
    await flushPromises()

    // 找 radio-group, 切到 EXCLUDE
    const radioGroup = wrapper.findComponent({ name: 'el-radio-group' })
    radioGroup.vm.$emit('update:model-value', 'EXCLUDE')
    await flushPromises()
    // mode 改了但 values 还空 — emit 已发生
    expect(wrapper.emitted('update:modelValue')).toBeTruthy()

    // 受控回填 EXCLUDE 态 (有值才显示关系下拉)
    await wrapper.setProps({
      modelValue: { orgAnchor: 'PRIMARY_ORG', subjectRelExclude: ['admin'] },
    })
    await flushPromises()
    // EXCLUDE 态下的关系下拉 = 最后一个 select
    const all = selects(wrapper)
    const relSelect = all[all.length - 1]
    relSelect.vm.$emit('update:model-value', ['admin', 'member'])
    await flushPromises()

    const last = wrapper.emitted('update:modelValue')!.at(-1)![0] as ScopeSpecVM
    expect(last.subjectRelExclude).toEqual(['admin', 'member'])
    expect(last.subjectRelInclude).toEqual([])
  })
})

describe('ScopeBuilder — axis③ type filter', () => {
  it('设类型过滤 → emit typeFilter; 选项来自 entityTypeApi', async () => {
    const wrapper = mountBuilder({
      modelValue: { orgAnchor: 'PRIMARY_ORG' },
      capabilities: { typeEntity: 'USER' },
    })
    await flushPromises()
    expect(entityListMock).toHaveBeenCalledWith('USER')

    // 类型 select = 最后一个 (axisOnlyOrg false, relationFilterable false)
    const all = selects(wrapper)
    const typeSelect = all[all.length - 1]
    typeSelect.vm.$emit('update:model-value', ['TYPE_A'])
    await flushPromises()

    const last = wrapper.emitted('update:modelValue')!.at(-1)![0] as ScopeSpecVM
    expect(last.typeFilter).toEqual(['TYPE_A'])
    // 选项标签来自 API
    expect(wrapper.html()).toContain('类型甲')
  })
})

describe('ScopeBuilder — allowedScopes gating', () => {
  it('allowedScopes=[SELF] → 只提供 SELF 锚点 option', async () => {
    const wrapper = mountBuilder({
      modelValue: { orgAnchor: 'SELF' },
      capabilities: { allowedScopes: ['SELF'] },
    })
    await flushPromises()
    const html = wrapper.html()
    expect(html).toContain('仅本人')
    // 其它锚点 option 不出现
    expect(html).not.toContain('全部组织')
    expect(html).not.toContain('指定组织')
  })

  it('allowedScopes=null → 提供全部锚点', async () => {
    const wrapper = mountBuilder({
      modelValue: { orgAnchor: 'SELF' },
      capabilities: { allowedScopes: null },
    })
    await flushPromises()
    const html = wrapper.html()
    expect(html).toContain('全部组织')
    expect(html).toContain('指定组织')
  })
})
