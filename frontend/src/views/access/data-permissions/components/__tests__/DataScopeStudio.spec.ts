import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'

// ── Mock 数据权限 API (getConfig / saveConfig) ──────────────────────
const getConfigMock = vi.fn()
const saveConfigMock = vi.fn()
vi.mock('@/api/access', () => ({
  dataPermissionApi: {
    getConfig: (...a: any[]) => getConfigMock(...a),
    saveConfig: (...a: any[]) => saveConfigMock(...a),
  },
}))

// 关系/类型字典 (预览标签来自 API, 无行业硬编码)
const relationListMock = vi.fn()
const entityListMock = vi.fn()
vi.mock('@/api/relationType', () => ({
  relationTypeApi: { list: (...a: any[]) => relationListMock(...a) },
}))
vi.mock('@/api/entityType', () => ({
  entityTypeApi: { list: (...a: any[]) => entityListMock(...a) },
}))
// CustomScopeTreePicker 拉组织树 — 存根
vi.mock('@/api/organization', () => ({
  getOrgUnitTree: vi.fn().mockResolvedValue([]),
}))

// plugins store — 仅暴露 codes (无插件启用 → 模板只用核心 scope), 免装 pinia
vi.mock('@/stores/plugins', () => ({
  usePluginsStore: () => ({ codes: [] }),
}))

// ElMessage / ElMessageBox 无副作用 stub
vi.mock('element-plus', () => ({
  ElMessage: { success: vi.fn(), warning: vi.fn(), error: vi.fn(), info: vi.fn() },
  ElMessageBox: { confirm: vi.fn().mockResolvedValue(true) },
}))

import DataScopeStudio, { type ModuleCapability } from '../DataScopeStudio.vue'

// ── 测试数据 ────────────────────────────────────────────────────────
const ROLE = { id: '7', roleCode: 'tenant_admin', roleName: '租户管理员', pluginEnabled: true } as any

const MODULES: ModuleCapability[] = [
  { code: 'org_unit', name: '组织单元', industry: 'CORE', allowedScopes: null, pluginEnabled: true },
  {
    code: 'user',
    name: '用户',
    industry: 'CORE',
    allowedScopes: null,
    relationFilterable: true,
    typeEntity: 'USER',
    pluginEnabled: true,
  },
  { code: 'place', name: '场所', industry: 'CORE', allowedScopes: null, pluginEnabled: true },
  {
    code: 'inspection_record',
    name: '检查记录',
    industry: 'CORE',
    allowedScopes: ['SELF', 'DEPARTMENT', 'DEPARTMENT_AND_BELOW'],
    pluginEnabled: true,
  },
]

// 已存配置: 多数资源 PRIMARY_ORG+subtree (=默认), user 带轴②③ (=例外)
const SAVED_CONFIG = {
  modulePermissions: [
    { moduleCode: 'org_unit', scopeCode: 'DEPARTMENT_AND_BELOW', orgAnchor: 'PRIMARY_ORG', includeSubtree: true },
    { moduleCode: 'place', scopeCode: 'DEPARTMENT_AND_BELOW', orgAnchor: 'PRIMARY_ORG', includeSubtree: true },
    {
      moduleCode: 'inspection_record',
      scopeCode: 'DEPARTMENT_AND_BELOW',
      orgAnchor: 'PRIMARY_ORG',
      includeSubtree: true,
    },
    {
      moduleCode: 'user',
      scopeCode: 'MANAGED_ORGS',
      orgAnchor: 'RELATION',
      anchorParam: 'admin',
      subjectRelExclude: ['admin'],
      typeFilter: ['STAFF'],
    },
  ],
}

// EP 组件本地存根 (全局 setup 把它们存根为 true=不渲染 slot; 本测试需渲染默认插槽)
const localStubs = {
  'el-select': { props: ['modelValue'], emits: ['update:model-value'], template: '<div><slot /></div>' },
  'el-option': { props: ['label', 'value'], template: '<div>{{ label }}<slot /></div>' },
  'el-radio-group': { props: ['modelValue'], emits: ['update:model-value'], template: '<div><slot /></div>' },
  'el-radio-button': { props: ['value'], template: '<div><slot /></div>' },
  'el-checkbox': { props: ['modelValue'], emits: ['update:model-value'], template: '<div><slot /></div>' },
  'el-input': { props: ['modelValue'], emits: ['update:model-value'], template: '<input />' },
  'el-dialog': { props: ['modelValue'], template: '<div class="el-dialog-stub"><slot /></div>' },
  'el-dropdown': { template: '<div><slot /><slot name="dropdown" /></div>' },
  'el-dropdown-menu': { template: '<div><slot /></div>' },
  'el-dropdown-item': { template: '<div><slot /></div>' },
  // 模板库子组件 — 仅透传 apply 事件, 不渲染内部
  TemplateLibraryDialog: { template: '<div class="tpl-dialog-stub" />' },
}

function mountStudio() {
  return mount(DataScopeStudio, {
    props: { currentRole: ROLE, modules: MODULES },
    global: { stubs: localStubs },
  })
}

beforeEach(() => {
  getConfigMock.mockResolvedValue(SAVED_CONFIG)
  saveConfigMock.mockResolvedValue(undefined)
  relationListMock.mockResolvedValue([
    { relationCode: 'admin', relationName: '管理', toType: 'ORG_UNIT', fromType: 'USER' },
  ])
  entityListMock.mockResolvedValue([{ typeCode: 'STAFF', typeName: '职员' }])
})

describe('DataScopeStudio — 加载推断', () => {
  it('载入 → 默认范围 + 例外卡片被推断并渲染', async () => {
    const wrapper = mountStudio()
    await flushPromises()

    expect(getConfigMock).toHaveBeenCalledWith('7')
    const vm: any = wrapper.vm
    // 众数默认 = PRIMARY_ORG + subtree (3 个资源)
    expect(vm.defaultSpec.orgAnchor).toBe('PRIMARY_ORG')
    expect(vm.defaultSpec.includeSubtree).toBe(true)
    // user 是唯一例外 (轴① 不同 + 带轴②③)
    expect(vm.exceptions).toHaveLength(1)
    expect(vm.exceptions[0].moduleCode).toBe('user')

    // 默认卡片 + 例外卡片 (含资源名) 渲染
    const html = wrapper.html()
    expect(html).toContain('默认范围')
    expect(html).toContain('资源例外')
    expect(html).toContain('用户')
  })
})

describe('DataScopeStudio — 例外增删', () => {
  it('通过选择器添加例外 → 新卡片从默认 seed', async () => {
    const wrapper = mountStudio()
    await flushPromises()
    const vm: any = wrapper.vm

    expect(vm.exceptions).toHaveLength(1)
    vm.addException('place')
    await flushPromises()

    expect(vm.exceptions).toHaveLength(2)
    const added = vm.exceptions.find((e: any) => e.moduleCode === 'place')
    expect(added).toBeTruthy()
    // 从默认 seed
    expect(added.spec.orgAnchor).toBe('PRIMARY_ORG')
    expect(added.spec.includeSubtree).toBe(true)
  })

  it('移除例外 → 该资源回到跟随默认', async () => {
    const wrapper = mountStudio()
    await flushPromises()
    const vm: any = wrapper.vm

    expect(vm.exceptions).toHaveLength(1)
    vm.removeException(0)
    await flushPromises()
    expect(vm.exceptions).toHaveLength(0)
  })
})

describe('DataScopeStudio — 模板载入', () => {
  it('应用"本部门及以下"模板 → defaultSpec 变为 PRIMARY_ORG+subtree, 不自动保存', async () => {
    const wrapper = mountStudio()
    await flushPromises()
    const vm: any = wrapper.vm

    // 模板携带 SceneDecision: primary=DEPARTMENT_AND_BELOW, 业务跟随
    vm.applyTemplate({
      id: 'dept-manager',
      name: '部门经理',
      icon: 'Users',
      industry: 'CORE',
      description: '',
      scene: { primary: 'DEPARTMENT_AND_BELOW', bizAutoFollow: true },
    })
    await flushPromises()

    // 模板主决策落成默认范围: PRIMARY_ORG + 含子树
    expect(vm.defaultSpec.orgAnchor).toBe('PRIMARY_ORG')
    expect(vm.defaultSpec.includeSubtree).toBe(true)
    // 全资源同一范围 → 无例外
    expect(vm.exceptions).toHaveLength(0)
    // 不自动保存 — 用户复核后手动保存
    expect(saveConfigMock).not.toHaveBeenCalled()
  })

  it('应用"全部数据"模板 + 受限资源 → 默认 ALL, 受限资源因 allowedScopes 沦为例外', async () => {
    // inspection_record 仅允许 [SELF,DEPARTMENT,DEPARTMENT_AND_BELOW] → ALL 经 fallback 降级 → 成例外
    const wrapper = mountStudio()
    await flushPromises()
    const vm: any = wrapper.vm

    vm.applyTemplate({
      id: 'super-admin',
      name: '超级管理员',
      icon: 'Crown',
      industry: 'CORE',
      description: '',
      scene: { primary: 'ALL', bizAutoFollow: true },
    })
    await flushPromises()

    // 多数资源 ALL → 默认 ALL
    expect(vm.defaultSpec.orgAnchor).toBe('ALL')
    // inspection_record 不允许 ALL → fallback 到 DEPARTMENT_AND_BELOW → 轴① 不同 → 成例外
    expect(vm.exceptions.map((e: any) => e.moduleCode)).toContain('inspection_record')
    expect(saveConfigMock).not.toHaveBeenCalled()
  })
})

describe('DataScopeStudio — 保存展开+钳制', () => {
  it('保存 → saveConfig payload 覆盖全部资源码 + 例外三轴 + 钳制默认', async () => {
    const wrapper = mountStudio()
    await flushPromises()
    const vm: any = wrapper.vm

    await vm.handleSave()
    await flushPromises()

    expect(saveConfigMock).toHaveBeenCalledTimes(1)
    const [roleId, payload] = saveConfigMock.mock.calls[0]
    expect(roleId).toBe('7')
    expect(payload.roleId).toBe('7')
    expect(payload.roleName).toBe('租户管理员')

    const cmds: any[] = payload.modulePermissions
    // 覆盖全部 4 个资源码
    expect(cmds.map(c => c.moduleCode).sort()).toEqual(
      ['inspection_record', 'org_unit', 'place', 'user'].sort()
    )

    // 例外 user → 完整三轴原样下发
    const userCmd = cmds.find(c => c.moduleCode === 'user')
    expect(userCmd.orgAnchor).toBe('RELATION')
    expect(userCmd.anchorParam).toBe('admin')
    expect(userCmd.subjectRelExclude).toEqual(['admin'])
    expect(userCmd.typeFilter).toEqual(['STAFF'])

    // 非例外资源 → 默认 (PRIMARY_ORG+subtree) → scopeCode DEPARTMENT_AND_BELOW
    const orgCmd = cmds.find(c => c.moduleCode === 'org_unit')
    expect(orgCmd.orgAnchor).toBe('PRIMARY_ORG')
    expect(orgCmd.scopeCode).toBe('DEPARTMENT_AND_BELOW')

    // inspection_record allowedScopes 含 DEPARTMENT_AND_BELOW → 钳制后仍 DEPARTMENT_AND_BELOW
    const insp = cmds.find(c => c.moduleCode === 'inspection_record')
    expect(insp.scopeCode).toBe('DEPARTMENT_AND_BELOW')

    expect(wrapper.emitted('saved')).toBeTruthy()
  })

  it('保存保留未托管模块: getConfig 返回 props.modules 之外的码 (legacy_x) → payload 原样下发不丢', async () => {
    // 后端 saveRolePermissions 是 delete-all-then-insert: 不下发的码会被软删。
    // legacy_x 不在 props.modules 里 → 不被 expandToCommands 覆盖 → 必须以"未托管 pass-through"形式保留。
    getConfigMock.mockResolvedValue({
      modulePermissions: [
        ...SAVED_CONFIG.modulePermissions,
        {
          moduleCode: 'legacy_x',
          scopeCode: 'DEPARTMENT',
          orgAnchor: 'PRIMARY_ORG',
          includeSubtree: false,
          typeFilter: ['FOO'],
        },
      ],
    })
    const wrapper = mountStudio()
    await flushPromises()
    const vm: any = wrapper.vm

    await vm.handleSave()
    await flushPromises()

    expect(saveConfigMock).toHaveBeenCalledTimes(1)
    const cmds: any[] = saveConfigMock.mock.calls.at(-1)![1].modulePermissions

    // legacy_x 仍在 payload (未被删配)
    const legacy = cmds.find(c => c.moduleCode === 'legacy_x')
    expect(legacy).toBeTruthy()
    // 原样保留: 既不默认化也不钳制
    expect(legacy.scopeCode).toBe('DEPARTMENT')
    expect(legacy.orgAnchor).toBe('PRIMARY_ORG')
    expect(legacy.includeSubtree).toBe(false)
    expect(legacy.typeFilter).toEqual(['FOO'])

    // 托管的 4 个码仍正常展开 (de-dup: legacy_x 只出现一次)
    const counts = cmds.reduce((acc: Record<string, number>, c) => {
      acc[c.moduleCode] = (acc[c.moduleCode] || 0) + 1
      return acc
    }, {})
    expect(counts['legacy_x']).toBe(1)
    expect(cmds.map(c => c.moduleCode).sort()).toEqual(
      ['inspection_record', 'legacy_x', 'org_unit', 'place', 'user'].sort()
    )
  })

  it('props.modules 为空 → 防呆: 不调用 saveConfig (不下发 [] 防全删配)', async () => {
    const wrapper = mount(DataScopeStudio, {
      props: { currentRole: ROLE, modules: [] },
      global: { stubs: localStubs },
    })
    await flushPromises()
    const vm: any = wrapper.vm

    await vm.handleSave()
    await flushPromises()

    expect(saveConfigMock).not.toHaveBeenCalled()
  })

  it('默认范围被某资源 allowedScopes 钳制 (ALL 默认 → 只允许 SELF 的资源降到 SELF)', async () => {
    // 配置: 全部 ALL → 默认 ALL; inspection_record 仅允许 [SELF]
    getConfigMock.mockResolvedValue({
      modulePermissions: MODULES.map(m => ({
        moduleCode: m.code,
        scopeCode: 'ALL',
        orgAnchor: 'ALL',
      })),
    })
    const restricted = MODULES.map(m =>
      m.code === 'inspection_record' ? { ...m, allowedScopes: ['SELF'] } : m
    )
    const wrapper = mount(DataScopeStudio, {
      props: { currentRole: ROLE, modules: restricted },
      global: { stubs: localStubs },
    })
    await flushPromises()
    const vm: any = wrapper.vm
    expect(vm.defaultSpec.orgAnchor).toBe('ALL')

    await vm.handleSave()
    await flushPromises()
    const cmds: any[] = saveConfigMock.mock.calls.at(-1)![1].modulePermissions
    const insp = cmds.find(c => c.moduleCode === 'inspection_record')
    // 钳制: ALL 不在 [SELF] → 落到 SELF
    expect(insp.scopeCode).toBe('SELF')
    // 其它无限制资源仍 ALL
    expect(cmds.find(c => c.moduleCode === 'user').scopeCode).toBe('ALL')
  })
})
