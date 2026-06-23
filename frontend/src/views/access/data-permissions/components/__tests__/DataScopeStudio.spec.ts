import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'

// ── Mock 数据权限 API ──────────────────────
const getConfigMock = vi.fn()
const saveConfigMock = vi.fn()
const simulateMock = vi.fn()
vi.mock('@/api/access', () => ({
  dataPermissionApi: {
    getConfig: (...a: any[]) => getConfigMock(...a),
    saveConfig: (...a: any[]) => saveConfigMock(...a),
    getResourceRelations: vi.fn().mockResolvedValue([]),
  },
  dataPermissionSimulateApi: {
    simulate: (...a: any[]) => simulateMock(...a),
  },
}))
vi.mock('@/api/relationType', () => ({
  relationTypeApi: { list: vi.fn().mockResolvedValue([]) },
}))
vi.mock('@/api/organization', () => ({ getOrgUnitTree: vi.fn().mockResolvedValue([]) }))
vi.mock('@/stores/plugins', () => ({ usePluginsStore: () => ({ codes: [] }) }))
vi.mock('element-plus', () => ({
  ElMessage: { success: vi.fn(), warning: vi.fn(), error: vi.fn(), info: vi.fn() },
  ElMessageBox: { confirm: vi.fn().mockResolvedValue(true) },
}))

import DataScopeStudio, { type ModuleCapability } from '../DataScopeStudio.vue'

const ROLE = { id: '7', roleCode: 'tenant_admin', roleName: '租户管理员', pluginEnabled: true } as any

const MODULES: ModuleCapability[] = [
  { code: 'org_unit', name: '组织单元', industry: 'CORE', allowedScopes: null, pluginEnabled: true },
  { code: 'user', name: '用户', industry: 'CORE', allowedScopes: null, relationFilterable: true, typeEntity: 'USER', pluginEnabled: true },
  { code: 'place', name: '场所', industry: 'CORE', allowedScopes: null, pluginEnabled: true },
  { code: 'inspection_record', name: '检查记录', industry: 'CORE', allowedScopes: ['SELF', 'DEPARTMENT', 'DEPARTMENT_AND_BELOW'], pluginEnabled: true },
]

// 已存配置 (legacy 轴① 形态, 无 relationGrants): 多数 PRIMARY_ORG, user 带 RELATION admin + axis②③
const SAVED_CONFIG = {
  modulePermissions: [
    { moduleCode: 'org_unit', scopeCode: 'DEPARTMENT_AND_BELOW', orgAnchor: 'PRIMARY_ORG', includeSubtree: true },
    { moduleCode: 'place', scopeCode: 'DEPARTMENT_AND_BELOW', orgAnchor: 'PRIMARY_ORG', includeSubtree: true },
    { moduleCode: 'inspection_record', scopeCode: 'DEPARTMENT_AND_BELOW', orgAnchor: 'PRIMARY_ORG', includeSubtree: true },
    { moduleCode: 'user', scopeCode: 'MANAGED_ORGS', orgAnchor: 'RELATION', anchorParam: 'admin', subjectRelExclude: ['admin'], typeFilter: ['STAFF'] },
  ],
}

// MultiGrantEditor 存根 — Studio 逻辑测 vm 方法, 不渲染编辑器内部 (其映射在 scopeRelation.spec 测)
const localStubs = {
  MultiGrantEditor: { props: ['moduleCode', 'modelValue', 'disabled'], template: '<div class="mge-stub">{{ moduleCode }}</div>' },
  TemplateLibraryDialog: { template: '<div class="tpl-dialog-stub" />' },
  'el-dialog': { props: ['modelValue'], template: '<div><slot /></div>' },
}

function mountStudio(modules = MODULES) {
  return mount(DataScopeStudio, {
    props: { currentRole: ROLE, modules },
    global: { stubs: localStubs },
  })
}

beforeEach(() => {
  getConfigMock.mockResolvedValue(SAVED_CONFIG)
  saveConfigMock.mockResolvedValue(undefined)
  simulateMock.mockReset()
})

describe('DataScopeStudio — 加载 (纯关系逐资源)', () => {
  it('载入 → 每资源 grants (legacy 轴①→关系); axis②③ 透传保全', async () => {
    const wrapper = mountStudio()
    await flushPromises()
    expect(getConfigMock).toHaveBeenCalledWith('7')
    const vm: any = wrapper.vm

    // PRIMARY_ORG → 成员关系 grant (含下级)
    const orgGrants = vm.grantsOf('org_unit')
    expect(orgGrants).toHaveLength(1)
    expect(orgGrants[0]).toMatchObject({ relation: 'owner_org', subject: 'RELATION', subjectParam: 'member', subtree: true })
    // RELATION admin → 管理关系 grant
    expect(vm.grantsOf('user')[0]).toMatchObject({ relation: 'owner_org', subject: 'RELATION', subjectParam: 'admin' })
    // axis②③ 透传保全在 specByCode (不丢)
    expect(vm.specByCode['user'].subjectRelExclude).toEqual(['admin'])
    expect(vm.specByCode['user'].typeFilter).toEqual(['STAFF'])

    // 渲染: 逐资源标题 + 无默认/例外
    const html = wrapper.html()
    expect(html).toContain('按关系逐资源配置')
    expect(html).not.toContain('默认范围')
    expect(html).toContain('用户')
  })
})

describe('DataScopeStudio — setGrants', () => {
  it('改某资源 grants, 不影响其他 + 保留其 axis②③', async () => {
    const wrapper = mountStudio()
    await flushPromises()
    const vm: any = wrapper.vm

    vm.setGrants('user', [{ relation: 'owner_org', subject: 'ALL' }])
    await flushPromises()
    expect(vm.grantsOf('user')[0]).toMatchObject({ relation: 'owner_org', subject: 'ALL' })
    // axis②③ 仍在 (setGrants 只动 relationGrants)
    expect(vm.specByCode['user'].typeFilter).toEqual(['STAFF'])
    // 其他资源不变
    expect(vm.grantsOf('org_unit')[0].subjectParam).toBe('member')
  })
})

describe('DataScopeStudio — 模板载入', () => {
  it('应用"本部门及以下"模板 → 每资源 grants=成员关系, 不自动保存', async () => {
    const wrapper = mountStudio()
    await flushPromises()
    const vm: any = wrapper.vm

    vm.applyTemplate({
      id: 'dept', name: '部门经理', icon: 'Users', industry: 'CORE', description: '',
      scene: { primary: 'DEPARTMENT_AND_BELOW', bizAutoFollow: true },
    })
    await flushPromises()
    // DEPARTMENT_AND_BELOW preset → PRIMARY_ORG+subtree → 成员关系含下级
    expect(vm.grantsOf('org_unit')[0]).toMatchObject({ relation: 'owner_org', subject: 'RELATION', subjectParam: 'member', subtree: true })
    expect(saveConfigMock).not.toHaveBeenCalled()
  })
})

describe('DataScopeStudio — 保存', () => {
  it('保存 → 全部资源码 + user relationGrants(管理) + axis②③ 保全', async () => {
    const wrapper = mountStudio()
    await flushPromises()
    const vm: any = wrapper.vm

    await vm.handleSave()
    await flushPromises()
    expect(saveConfigMock).toHaveBeenCalledTimes(1)
    const [roleId, payload] = saveConfigMock.mock.calls[0]
    expect(roleId).toBe('7')
    const cmds: any[] = payload.modulePermissions
    expect(cmds.map(c => c.moduleCode).sort()).toEqual(['inspection_record', 'org_unit', 'place', 'user'].sort())

    const userCmd = cmds.find(c => c.moduleCode === 'user')
    expect(userCmd.relationGrants).toEqual([{ relation: 'owner_org', subject: 'RELATION', subjectParam: 'admin', subtree: false }])
    // axis②③ 随 spec 下发, 不丢
    expect(userCmd.subjectRelExclude).toEqual(['admin'])
    expect(userCmd.typeFilter).toEqual(['STAFF'])

    const orgCmd = cmds.find(c => c.moduleCode === 'org_unit')
    expect(orgCmd.relationGrants[0]).toMatchObject({ relation: 'owner_org', subject: 'RELATION', subjectParam: 'member' })
    expect(wrapper.emitted('saved')).toBeTruthy()
  })

  it('PLUGIN_DIM (BY_CLASS) 往返保全 (金标准红线)', async () => {
    getConfigMock.mockResolvedValue({
      modulePermissions: [
        { moduleCode: 'inspection_record', scopeCode: 'BY_CLASS',
          relationGrants: [{ relation: 'owner_org', subject: 'PLUGIN_DIM', subjectParam: 'BY_CLASS' }] },
      ],
    })
    const wrapper = mountStudio()
    await flushPromises()
    const vm: any = wrapper.vm
    // 载入: BY_CLASS 维度 grant 原样
    expect(vm.grantsOf('inspection_record')[0]).toMatchObject({ relation: 'owner_org', subject: 'PLUGIN_DIM', subjectParam: 'BY_CLASS' })

    await vm.handleSave()
    await flushPromises()
    const cmds: any[] = saveConfigMock.mock.calls.at(-1)![1].modulePermissions
    const insp = cmds.find(c => c.moduleCode === 'inspection_record')
    // 保存原样回写 BY_CLASS (未被腐蚀)
    expect(insp.relationGrants[0]).toMatchObject({ relation: 'owner_org', subject: 'PLUGIN_DIM', subjectParam: 'BY_CLASS' })
  })

  it('保留未托管模块 (legacy_x 直通不丢)', async () => {
    getConfigMock.mockResolvedValue({
      modulePermissions: [
        ...SAVED_CONFIG.modulePermissions,
        { moduleCode: 'legacy_x', scopeCode: 'DEPARTMENT', orgAnchor: 'PRIMARY_ORG', typeFilter: ['FOO'] },
      ],
    })
    const wrapper = mountStudio()
    await flushPromises()
    const vm: any = wrapper.vm
    await vm.handleSave()
    await flushPromises()
    const cmds: any[] = saveConfigMock.mock.calls.at(-1)![1].modulePermissions
    const legacy = cmds.find(c => c.moduleCode === 'legacy_x')
    expect(legacy).toBeTruthy()
    expect(legacy.scopeCode).toBe('DEPARTMENT')
    expect(legacy.typeFilter).toEqual(['FOO'])
    // 去重: 只一次
    expect(cmds.filter(c => c.moduleCode === 'legacy_x')).toHaveLength(1)
  })

  it('props.modules 为空 → 防呆不保存', async () => {
    const wrapper = mountStudio([])
    await flushPromises()
    const vm: any = wrapper.vm
    await vm.handleSave()
    await flushPromises()
    expect(saveConfigMock).not.toHaveBeenCalled()
  })
})

describe('DataScopeStudio — 模拟用户', () => {
  it('输入用户 ID + 模拟 → 用当前范围快照调 simulate 并渲染', async () => {
    simulateMock.mockResolvedValue({
      userId: '42',
      results: [
        { moduleCode: 'org_unit', scopeCode: 'X', accessibleCount: 5 },
        { moduleCode: 'user', scopeCode: 'X', accessibleCount: 3, samples: [{ id: '9', name: '张三' }] },
      ],
    })
    const wrapper = mountStudio()
    await flushPromises()
    const vm: any = wrapper.vm
    vm.simulateUserId = 42
    await vm.runSimulate()
    await flushPromises()
    expect(simulateMock).toHaveBeenCalledTimes(1)
    const req = simulateMock.mock.calls[0][0]
    expect(req.userId).toBe('42')
    expect(req.modulePermissions.map((m: any) => m.moduleCode).sort()).toEqual(['inspection_record', 'org_unit', 'place', 'user'].sort())
    expect(vm.simulateResults).toHaveLength(2)
    expect(wrapper.html()).toContain('5 条')
    expect(wrapper.html()).toContain('张三')
  })

  it('模拟失败 → 显示错误不抛', async () => {
    simulateMock.mockRejectedValue({ message: '服务器错误' })
    const wrapper = mountStudio()
    await flushPromises()
    const vm: any = wrapper.vm
    vm.simulateUserId = 7
    await vm.runSimulate()
    await flushPromises()
    expect(vm.simulateError).toContain('服务器错误')
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
