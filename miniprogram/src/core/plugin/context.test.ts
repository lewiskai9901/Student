import { describe, it, expect } from 'vitest'
import { hasPerm, type PluginContext } from './context'

describe('PluginContext', () => {
  it('should have required readonly fields', () => {
    const ctx: PluginContext = {
      tenantPlugins: ['inspection'],
      permissions: ['inspection:task:view'],
      user: { id: '1', username: 'admin', name: '管理员', roles: [] },
      capability: {} as any,
      bus: {} as any
    }
    expect(ctx.tenantPlugins).toContain('inspection')
    expect(ctx.permissions).toHaveLength(1)
  })

  it('hasPerm helper should check permissions correctly', () => {
    const ctx: PluginContext = {
      tenantPlugins: [],
      permissions: ['a:b:c', 'x:y:z'],
      user: {} as any, capability: {} as any, bus: {} as any
    }
    expect(hasPerm(ctx, 'a:b:c')).toBe(true)
    expect(hasPerm(ctx, 'unknown')).toBe(false)
  })
})
