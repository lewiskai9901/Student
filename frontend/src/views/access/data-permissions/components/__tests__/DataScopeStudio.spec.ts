import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'

// ── Mock 数据权限 API (getConfig / saveConfig) ──────────────────────
const getConfigMock = vi.fn()
const saveConfigMock = vi.fn()
const simulateMock = vi.fn()
vi.mock('@/api/access', () => ({
  dataPermissionApi: {
    getConfig: (...a: any[]) => getConfigMock(...a),
    saveConfig: (...a: any[]) => saveConfigMock(...a),
  },
  dataPermissionSimulateApi: {
    simulate: (...a: any[]) => simulateMock(...a),
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
  simulateMock.mockReset()
  relationListMock.mockResolvedValue([
    { relationCode: 'admin', relationName: '管理', toType: 'ORG_UNIT', fromType: 'USER' },
  ])
  entityListMock.mockResolvedValue([{ typeCode: 'STAFF', typeName: '职员' }])
})

describe('DataScopeStudio — 加载 (扁平逐资源)', () => {
  it('载入 → 每资源独立 spec (无默认/例外分层)', async () => {
    const wrapper = mountStudio()
    await flushPromises()

    expect(getConfigMock).toHaveBeenCalledWith('7')
    const vm: any = wrapper.vm
    // 每个资源各有自己的 spec
    expect(vm.specOf('org_unit').orgAnchor).toBe('PRIMARY_ORG')
    expect(vm.specOf('org_unit').includeSubtree).toBe(true)
    expect(vm.specOf('place').orgAnchor).toBe('PRIMARY_ORG')
    expect(vm.specOf('user').orgAnchor).toBe('RELATION')
    expect(vm.specOf('user').subjectRelExclude).toEqual(['admin'])
    expect(vm.specOf('user').typeFilter).toEqual(['STAFF'])

    // 全部资源逐行渲染 (含资源名), 无"默认范围/资源例外"分层标题
    const html = wrapper.html()
    expect(html).toContain('逐资源配置')
    expect(html).not.toContain('默认范围')
    expect(html).not.toContain('资源例外')
    expect(html).toContain('用户')
    expect(html).toContain('组织单元')
  })
})

describe('DataScopeStudio — 逐资源编辑 + 多锚点切换', () => {
  it('updateModuleSpec → 改某资源 spec, 不影响其他', async () => {
    const wrapper = mountStudio()
    await flushPromises()
    const vm: any = wrapper.vm

    vm.updateModuleSpec('place', { orgAnchor: 'ALL' })
    await flushPromises()
    expect(vm.specOf('place').orgAnchor).toBe('ALL')
    // 其他资源不变
    expect(vm.specOf('org_unit').orgAnchor).toBe('PRIMARY_ORG')
  })

  it('convertToMulti → 该资源变多锚点 (>1 grant); simplifyToSingle 还原', async () => {
    const wrapper = mountStudio()
    await flushPromises()
    const vm: any = wrapper.vm

    vm.convertToMulti('place')
    await flushPromises()
    expect(vm.isMultiGrantSpec(vm.specOf('place'))).toBe(true)
    expect(vm.specOf('place').relationGrants.length).toBe(2)

    vm.simplifyToSingle('place')
    await flushPromises()
    expect(vm.isMultiGrantSpec(vm.specOf('place'))).toBe(false)
  })
})

describe('DataScopeStudio — 模板载入 (逐资源)', () => {
  it('应用"本部门及以下"模板 → 每资源 spec=PRIMARY_ORG+subtree, 不自动保存', async () => {
    const wrapper = mountStudio()
    await flushPromises()
    const vm: any = wrapper.vm

    vm.applyTemplate({
      id: 'dept-manager', name: '部门经理', icon: 'Users', industry: 'CORE', description: '',
      scene: { primary: 'DEPARTMENT_AND_BELOW', bizAutoFollow: true },
    })
    await flushPromises()

    // 模板逐资源落 specByCode: org_unit/user 等无限制资源 → PRIMARY_ORG + 含子树
    expect(vm.specOf('org_unit').orgAnchor).toBe('PRIMARY_ORG')
    expect(vm.specOf('org_unit').includeSubtree).toBe(true)
    expect(vm.specOf('user').orgAnchor).toBe('PRIMARY_ORG')
    expect(saveConfigMock).not.toHaveBeenCalled()
  })

  it('应用"全部数据"模板 + 受限资源 → 受限资源 scope 被 sceneToModuleScopes 钳到允许集内', async () => {
    const wrapper = mountStudio()
    await flushPromises()
    const vm: any = wrapper.vm

    vm.applyTemplate({
      id: 'super-admin', name: '超级管理员', icon: 'Crown', industry: 'CORE', description: '',
      scene: { primary: 'ALL', bizAutoFollow: true },
    })
    await flushPromises()

    // 无限制资源 → ALL
    expect(vm.specOf('user').orgAnchor).toBe('ALL')
    // inspection_record allowedScopes 不含 ALL → 模板降级到允许集 (非 ALL)
    expect(vm.specOf('inspection_record').orgAnchor).not.toBe('ALL')
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

  it('保存逐资源按 allowedScopes 钳制 (载入 ALL, 仅允许 SELF 的资源降到 SELF)', async () => {
    // 配置: 全部资源载入为 ALL; inspection_record 仅允许 [SELF]
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
    // 扁平模型: 每资源各自载入 ALL
    expect(vm.specOf('inspection_record').orgAnchor).toBe('ALL')

    await vm.handleSave()
    await flushPromises()
    const cmds: any[] = saveConfigMock.mock.calls.at(-1)![1].modulePermissions
    const insp = cmds.find(c => c.moduleCode === 'inspection_record')
    // 逐资源钳制: ALL 不在 [SELF] → 落到 SELF
    expect(insp.scopeCode).toBe('SELF')
    // 其它无限制资源仍 ALL
    expect(cmds.find(c => c.moduleCode === 'user').scopeCode).toBe('ALL')
  })

  it('PLUGIN_DIM (如 BY_CLASS) 不被钳制 (保金标准: 维度码非预设, 钳会腐蚀)', async () => {
    // inspection_record 载入 PLUGIN_DIM BY_CLASS, 但 allowedScopes 不含 BY_CLASS
    getConfigMock.mockResolvedValue({
      modulePermissions: [
        { moduleCode: 'inspection_record', scopeCode: 'BY_CLASS', orgAnchor: 'PLUGIN_DIM', anchorParam: 'BY_CLASS' },
      ],
    })
    const wrapper = mount(DataScopeStudio, {
      props: { currentRole: ROLE, modules: MODULES },  // inspection_record allowedScopes=[SELF,DEPARTMENT,DEPARTMENT_AND_BELOW]
      global: { stubs: localStubs },
    })
    await flushPromises()
    const vm: any = wrapper.vm

    await vm.handleSave()
    await flushPromises()
    const cmds: any[] = saveConfigMock.mock.calls.at(-1)![1].modulePermissions
    const insp = cmds.find(c => c.moduleCode === 'inspection_record')
    // PLUGIN_DIM 跳过钳制 → BY_CLASS 原样保留 (未被钳成 allowedScopes 里的预设)
    expect(insp.orgAnchor).toBe('PLUGIN_DIM')
    expect(insp.scopeCode).toBe('BY_CLASS')
  })
})

describe('DataScopeStudio — 模拟用户', () => {
  it('输入用户 ID + 点击模拟 → 用 (userId + 当前范围快照) 调 simulate 并渲染结果', async () => {
    simulateMock.mockResolvedValue({
      userId: '42',
      results: [
        { moduleCode: 'org_unit', scopeCode: 'DEPARTMENT_AND_BELOW', accessibleCount: 5 },
        {
          moduleCode: 'user',
          scopeCode: 'MANAGED_ORGS',
          accessibleCount: 3,
          samples: [{ id: '9', name: '张三' }],
        },
      ],
    })
    const wrapper = mountStudio()
    await flushPromises()
    const vm: any = wrapper.vm

    vm.simulateUserId = 42
    await vm.runSimulate()
    await flushPromises()

    // 用 userId + per-resource 快照 (默认+例外 展开) 调用
    expect(simulateMock).toHaveBeenCalledTimes(1)
    const req = simulateMock.mock.calls[0][0]
    expect(req.userId).toBe('42')
    expect(req.modulePermissions.map((m: any) => m.moduleCode).sort()).toEqual(
      ['inspection_record', 'org_unit', 'place', 'user'].sort()
    )
    // 例外 user 的范围进了快照
    const userSnap = req.modulePermissions.find((m: any) => m.moduleCode === 'user')
    expect(userSnap.scopeCode).toBe('MANAGED_ORGS')

    // 结果渲染: 资源名 + 条数 + 样本
    expect(vm.simulateResults).toHaveLength(2)
    const html = wrapper.html()
    expect(html).toContain('5 条')
    expect(html).toContain('3 条')
    expect(html).toContain('张三')
    expect(vm.simulateError).toBe('')
  })

  it('模拟失败 → 显示错误信息, 不抛出', async () => {
    simulateMock.mockRejectedValue({ message: '服务器错误' })
    const wrapper = mountStudio()
    await flushPromises()
    const vm: any = wrapper.vm

    vm.simulateUserId = 7
    await vm.runSimulate()
    await flushPromises()

    expect(simulateMock).toHaveBeenCalledTimes(1)
    expect(vm.simulateResults).toHaveLength(0)
    expect(vm.simulateError).toContain('服务器错误')
    expect(wrapper.html()).toContain('模拟失败')
  })

  it('未输入用户 ID → 不调用 simulate', async () => {
    const wrapper = mountStudio()
    await flushPromises()
    const vm: any = wrapper.vm

    await vm.runSimulate()
    await flushPromises()
    expect(simulateMock).not.toHaveBeenCalled()
  })
})
