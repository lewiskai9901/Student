import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { ContributionDispatcher } from '@core/plugin/dispatcher'
import { activatePlugins } from '@core/stores/plugin-registry'
import { createEventBus } from '@core/plugin/event-bus'
import inspection from '@plugins/inspection/manifest'
import type { MenuContribution } from '@core/plugin/contribution'

describe('inspection activation integration', () => {
  beforeEach(() => { setActivePinia(createPinia()) })

  const baseInput = () => ({
    user: { id: 1, username: 'u', name: 'U', roles: [] },
    permissions: [
      'inspection:task:list',
      'inspection:task:view',
      'inspection:correction:list',
      'inspection:correction:view',
      'inspection:appeal:list'
    ],
    capability: { platform: 'mp-weixin' } as any,
    bus: createEventBus()
  })

  it('does NOT activate when tenantPlugins lacks inspection', async () => {
    const d = new ContributionDispatcher('1.0.0')
    d.register(inspection)
    const active = await activatePlugins(d, { ...baseInput(), tenantPlugins: [] })
    expect(active).toHaveLength(0)
  })

  it('activates when tenantPlugins includes inspection', async () => {
    const d = new ContributionDispatcher('1.0.0')
    d.register(inspection)
    const active = await activatePlugins(d, { ...baseInput(), tenantPlugins: ['inspection'] })
    expect(active.map(p => p.key)).toEqual(['inspection'])
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
    const ctx = { ...baseInput(), permissions: ['inspection:task:view'], tenantPlugins: ['inspection'] }
    await activatePlugins(d, ctx)

    const menus = d.query<MenuContribution>('menu')
      .filter(m => m.group === 'home-grid')
      .filter(m => !m.perm || ctx.permissions.includes(m.perm))
    // Only inspection.scan has perm 'inspection:task:view'
    expect(menus.map(m => m.key)).toEqual(['inspection.scan'])
  })
})
