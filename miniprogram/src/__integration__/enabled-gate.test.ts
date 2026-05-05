import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { ContributionDispatcher } from '@core/plugin/dispatcher'
import { activatePlugins } from '@core/stores/plugin-registry'
import { createEventBus } from '@core/plugin/event-bus'
import healthcare from '@plugins/healthcare/manifest'

describe('V3: healthcare enabled() gate', () => {
  beforeEach(() => { setActivePinia(createPinia()) })

  const baseInput = () => ({
    user: { id: 1, username: 'u', name: 'U', roles: [] },
    permissions: ['healthcare:patient:view'],
    capability: { platform: 'mp-weixin' } as any,
    bus: createEventBus()
  })

  it('does NOT activate when tenantPlugins is empty', async () => {
    const d = new ContributionDispatcher('1.0.0')
    d.register(healthcare)
    const active = await activatePlugins(d, { ...baseInput(), tenantPlugins: [] })
    expect(active).toHaveLength(0)
  })

  it('activates when tenantPlugins includes healthcare', async () => {
    const d = new ContributionDispatcher('1.0.0')
    d.register(healthcare)
    const active = await activatePlugins(d, { ...baseInput(), tenantPlugins: ['healthcare'] })
    expect(active.map(p => p.key)).toEqual(['healthcare'])
  })

  it('does NOT activate when tenantPlugins has other plugins but not healthcare', async () => {
    const d = new ContributionDispatcher('1.0.0')
    d.register(healthcare)
    const active = await activatePlugins(d, { ...baseInput(), tenantPlugins: ['demo'] })
    expect(active).toHaveLength(0)
  })
})
