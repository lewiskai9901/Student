import { describe, it, expect } from 'vitest'
import { ContributionDispatcher } from '@core/plugin/dispatcher'
import inspection from './manifest'
import demo from '../demo/manifest'
import healthcare from '../healthcare/manifest'
import type { MenuContribution, RouteContribution, PermissionContribution, ScanResolverContribution, EventContribution } from '@core/plugin/contribution'

describe('inspection manifest', () => {
  it('registers without conflict alongside demo + healthcare', () => {
    const d = new ContributionDispatcher('1.0.0')
    expect(() => {
      d.register(demo)
      d.register(healthcare)
      d.register(inspection)
    }).not.toThrow()
    expect(d.allPlugins().map(p => p.key).sort()).toEqual(['demo', 'healthcare', 'inspection'])
  })

  it('declares INSPECTION:TASK: scan prefix', () => {
    const d = new ContributionDispatcher('1.0.0')
    d.register(inspection)
    const resolvers = d.query<ScanResolverContribution>('scan-resolver')
    expect(resolvers).toHaveLength(1)
    expect(resolvers[0].prefix).toBe('INSPECTION:TASK:')
  })

  it('scan resolver routes INSPECTION:TASK:123 to task-detail', () => {
    const d = new ContributionDispatcher('1.0.0')
    d.register(inspection)
    const resolver = d.query<ScanResolverContribution>('scan-resolver')[0]
    const action = resolver.resolve('INSPECTION:TASK:123')
    expect(action).not.toBeNull()
    expect(action!.path).toBe('/plugins/inspection/pages/task-detail')
    expect(action!.params).toEqual({ id: '123' })
  })

  it('declares 12 permissions / 5 menus / 9 routes / 3 events', () => {
    const counts = inspection.contributions.reduce((acc, c) => {
      acc[c.type] = (acc[c.type] ?? 0) + 1
      return acc
    }, {} as Record<string, number>)
    expect(counts['permission']).toBe(12)
    expect(counts['menu']).toBe(5)
    expect(counts['route']).toBe(9)
    expect(counts['scan-resolver']).toBe(1)
    expect(counts['event']).toBe(3)
  })

  it('every menu.perm references a declared permission', () => {
    const declaredPerms = new Set(
      inspection.contributions
        .filter((c): c is PermissionContribution => c.type === 'permission')
        .map(c => c.code)
    )
    const referenced = inspection.contributions
      .filter((c): c is MenuContribution => c.type === 'menu')
      .map(c => c.perm)
      .filter((p): p is string => !!p)
    for (const ref of referenced) {
      expect(declaredPerms.has(ref)).toBe(true)
    }
  })

  it('enabled gate: tenantPlugins must include inspection', () => {
    const ctxBase = {
      user: {} as any,
      permissions: [] as string[],
      capability: {} as any,
      bus: {} as any
    }
    expect(inspection.enabled?.({ ...ctxBase, tenantPlugins: ['inspection'] })).toBe(true)
    expect(inspection.enabled?.({ ...ctxBase, tenantPlugins: [] })).toBe(false)
    expect(inspection.enabled?.({ ...ctxBase, tenantPlugins: ['demo'] })).toBe(false)
  })
})
