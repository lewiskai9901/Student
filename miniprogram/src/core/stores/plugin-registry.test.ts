import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { ContributionDispatcher } from '../plugin/dispatcher'
import { definePlugin } from '../plugin/package'
import { createEventBus } from '../plugin/event-bus'
import { activatePlugins } from './plugin-registry'

describe('activatePlugins', () => {
  beforeEach(() => { setActivePinia(createPinia()) })

  it('filters plugins by tenantPlugins and runs bootstrap on actives', async () => {
    const d = new ContributionDispatcher('1.0.0')
    let bootedA = 0, bootedB = 0
    d.register(definePlugin({
      key: 'a', label: 'A', schemaVersion: 1, minCoreVersion: '0.1.0',
      contributions: [],
      bootstrap: () => { bootedA++ }
    }))
    d.register(definePlugin({
      key: 'b', label: 'B', schemaVersion: 1, minCoreVersion: '0.1.0',
      contributions: [],
      bootstrap: () => { bootedB++ }
    }))
    const active = await activatePlugins(d, {
      user: { id: '1', username: 'u', name: 'U', roles: [] },
      permissions: [],
      tenantPlugins: ['a'],
      capability: { platform: 'mp-weixin' } as any,
      bus: createEventBus()
    })
    expect(active.map(p => p.key)).toEqual(['a'])
    expect(bootedA).toBe(1)
    expect(bootedB).toBe(0)
  })

  it('respects enabled() runtime gate', async () => {
    const d = new ContributionDispatcher('1.0.0')
    d.register(definePlugin({
      key: 'gated', label: 'G', schemaVersion: 1, minCoreVersion: '0.1.0',
      contributions: [],
      enabled: (ctx) => ctx.permissions.includes('gated:enter')
    }))
    const ctxBase = {
      user: { id: '1', username: 'u', name: 'U', roles: [] },
      tenantPlugins: ['gated'],
      capability: { platform: 'mp-weixin' } as any,
      bus: createEventBus()
    }
    const denied = await activatePlugins(d, { ...ctxBase, permissions: [] })
    expect(denied).toHaveLength(0)
    const allowed = await activatePlugins(d, { ...ctxBase, permissions: ['gated:enter'] })
    expect(allowed).toHaveLength(1)
  })
})
