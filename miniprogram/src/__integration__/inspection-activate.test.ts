import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { ContributionDispatcher } from '@core/plugin/dispatcher'
import { activatePlugins } from '@core/stores/plugin-registry'
import { createEventBus } from '@core/plugin/event-bus'
import inspection from '@plugins/inspection/manifest'
import type { MenuContribution } from '@core/plugin/contribution'

describe('inspection activation integration', () => {
  beforeEach(() => { setActivePinia(createPinia()) })

  // Note: inspection.enabled 是 () => true (后端核心通用层),所以 tenantPlugins 不影响激活.
  // perm 字符串在 Phase 6 真机测试时对齐到后端实际下发的 perm code.
  const baseInput = () => ({
    user: { id: '1', username: 'u', name: 'U', roles: [] },
    permissions: [
      'inspection:task:view',          // 我的任务/可领任务/扫码
      'inspection:corrective:view',    // 我的整改
      'inspection:appeal:view'         // 我的申诉
    ],
    capability: { platform: 'mp-weixin' } as any,
    bus: createEventBus()
  })

  it('activates regardless of tenantPlugins (核心通用层)', async () => {
    const d = new ContributionDispatcher('1.0.0')
    d.register(inspection)
    const empty = await activatePlugins(d, { ...baseInput(), tenantPlugins: [] })
    expect(empty.map(p => p.key)).toEqual(['inspection'])
    const withTenant = await activatePlugins(d, { ...baseInput(), tenantPlugins: ['inspection'] })
    expect(withTenant.map(p => p.key)).toEqual(['inspection'])
  })

  it('home menu computes 5 inspection entries when permissions match', async () => {
    const d = new ContributionDispatcher('1.0.0')
    d.register(inspection)
    await activatePlugins(d, { ...baseInput(), tenantPlugins: ['inspection'] })

    const menus = d.query<MenuContribution>('menu')
      .filter(m => m.group === 'home-grid')
      .filter(m => !m.perm || baseInput().permissions.includes(m.perm))
    expect(menus).toHaveLength(5)
    expect(menus.map(m => m.key).sort()).toEqual([
      'inspection.available',
      'inspection.my-appeals',
      'inspection.my-corrections',
      'inspection.my-tasks',
      'inspection.scan'
    ])
  })

  it('home menu hides entries when user lacks permission', async () => {
    const d = new ContributionDispatcher('1.0.0')
    d.register(inspection)
    const ctx = { ...baseInput(), permissions: ['inspection:appeal:view'], tenantPlugins: ['inspection'] }
    await activatePlugins(d, ctx)

    const menus = d.query<MenuContribution>('menu')
      .filter(m => m.group === 'home-grid')
      .filter(m => !m.perm || ctx.permissions.includes(m.perm))
    // 仅 inspection.my-appeals 用 perm 'inspection:appeal:view' (P3 后真后端 perm)
    expect(menus.map(m => m.key)).toEqual(['inspection.my-appeals'])
  })
})
