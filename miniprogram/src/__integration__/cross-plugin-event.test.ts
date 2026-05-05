import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { ContributionDispatcher } from '@core/plugin/dispatcher'
import { activatePlugins } from '@core/stores/plugin-registry'
import { createEventBus } from '@core/plugin/event-bus'
import demo, { _crossPluginAuditLog } from '@plugins/demo/manifest'
import healthcare from '@plugins/healthcare/manifest'

describe('V4: cross-plugin event via bus (demo ← healthcare)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    _crossPluginAuditLog.length = 0
  })

  it('demo bootstrap subscribes to healthcare.patient.scanned and receives emits', async () => {
    const d = new ContributionDispatcher('1.0.0')
    d.register(demo)
    d.register(healthcare)

    const bus = createEventBus()
    await activatePlugins(d, {
      user: { id: 1, username: 'u', name: 'U', roles: [] },
      permissions: [],
      tenantPlugins: ['demo', 'healthcare'],
      capability: { platform: 'mp-weixin' } as any,
      bus
    })

    bus.emit('healthcare.patient.scanned', { patientId: 'P-001', scannedAt: 12345 })

    expect(_crossPluginAuditLog).toHaveLength(1)
    expect(_crossPluginAuditLog[0].event).toBe('healthcare.patient.scanned')
    expect(_crossPluginAuditLog[0].payload).toEqual({ patientId: 'P-001', scannedAt: 12345 })
  })

  it('subscription persists across activations (bus is namespace-routing, not lifecycle-coupled)', async () => {
    const d = new ContributionDispatcher('1.0.0')
    d.register(demo)
    d.register(healthcare)

    const bus = createEventBus()
    // Only demo activated (healthcare not in tenantPlugins)
    await activatePlugins(d, {
      user: { id: 1, username: 'u', name: 'U', roles: [] },
      permissions: [],
      tenantPlugins: ['demo'],
      capability: { platform: 'mp-weixin' } as any,
      bus
    })

    // demo's bootstrap ran, subscription is live. If something emits the event, demo still receives.
    // In real usage healthcare wouldn't emit when not active. This test documents the routing semantics.
    bus.emit('healthcare.patient.scanned', { patientId: 'P-002' })
    expect(_crossPluginAuditLog).toHaveLength(1)
  })
})
