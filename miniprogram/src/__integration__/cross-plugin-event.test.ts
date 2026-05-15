import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { ContributionDispatcher } from '@core/plugin/dispatcher'
import { activatePlugins } from '@core/stores/plugin-registry'
import { createEventBus } from '@core/plugin/event-bus'
import demo, { _crossPluginAuditLog } from '@plugins/demo/manifest'
import inspection from '@plugins/inspection/manifest'

describe('V4: cross-plugin event via bus (demo ← inspection)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    _crossPluginAuditLog.length = 0
  })

  it('demo bootstrap subscribes to inspection.task.submitted and receives emits', async () => {
    const d = new ContributionDispatcher('1.0.0')
    d.register(demo)
    d.register(inspection)

    const bus = createEventBus()
    await activatePlugins(d, {
      user: { id: '1', username: 'u', name: 'U', roles: [] },
      permissions: [],
      tenantPlugins: ['demo', 'inspection'],
      capability: { platform: 'mp-weixin' } as any,
      bus
    })

    bus.emit('inspection.task.submitted', { taskId: '1', submitterId: 99 })

    expect(_crossPluginAuditLog).toHaveLength(1)
    expect(_crossPluginAuditLog[0].event).toBe('inspection.task.submitted')
    expect(_crossPluginAuditLog[0].payload).toEqual({ taskId: '1', submitterId: 99 })
  })

  it('subscription persists across activations (bus is namespace-routing, not lifecycle-coupled)', async () => {
    const d = new ContributionDispatcher('1.0.0')
    d.register(demo)
    d.register(inspection)

    const bus = createEventBus()
    // Only demo activated (inspection not in tenantPlugins)
    await activatePlugins(d, {
      user: { id: '1', username: 'u', name: 'U', roles: [] },
      permissions: [],
      tenantPlugins: ['demo'],
      capability: { platform: 'mp-weixin' } as any,
      bus
    })

    // demo's bootstrap ran, subscription is live. If something emits the event, demo still receives.
    // In real usage inspection wouldn't emit when not active. This test documents the routing semantics.
    bus.emit('inspection.task.submitted', { taskId: '2' })
    expect(_crossPluginAuditLog).toHaveLength(1)
  })
})
