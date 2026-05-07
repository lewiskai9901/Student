import { describe, it, expect } from 'vitest'
import { ContributionDispatcher } from '@core/plugin/dispatcher'
import education from './manifest'
import demo from '../demo/manifest'
import healthcare from '../healthcare/manifest'
import inspection from '../inspection/manifest'
import type { MenuContribution, PermissionContribution } from '@core/plugin/contribution'

describe('education manifest', () => {
  it('registers without conflict alongside demo + healthcare + inspection', () => {
    const d = new ContributionDispatcher('1.0.0')
    expect(() => {
      d.register(demo)
      d.register(healthcare)
      d.register(inspection)
      d.register(education)
    }).not.toThrow()
    expect(d.allPlugins().map(p => p.key).sort()).toEqual(
      ['demo', 'education', 'healthcare', 'inspection']
    )
  })

  it('declares 4 permissions / 2 menus / 4 routes', () => {
    const counts = education.contributions.reduce((acc, c) => {
      acc[c.type] = (acc[c.type] ?? 0) + 1
      return acc
    }, {} as Record<string, number>)
    expect(counts['permission']).toBe(4)
    expect(counts['menu']).toBe(2)
    expect(counts['route']).toBe(4)
  })

  it('every menu.perm references a declared permission', () => {
    const declaredPerms = new Set(
      education.contributions
        .filter((c): c is PermissionContribution => c.type === 'permission')
        .map(c => c.code)
    )
    const referenced = education.contributions
      .filter((c): c is MenuContribution => c.type === 'menu')
      .map(c => c.perm)
      .filter((p): p is string => !!p)
    for (const ref of referenced) {
      expect(declaredPerms.has(ref)).toBe(true)
    }
  })

  it('enabled gate: tenantPlugins must include EDU (uppercase)', () => {
    const ctxBase = {
      user: {} as any,
      permissions: [] as string[],
      capability: {} as any,
      bus: {} as any
    }
    expect(education.enabled?.({ ...ctxBase, tenantPlugins: ['EDU'] })).toBe(true)
    expect(education.enabled?.({ ...ctxBase, tenantPlugins: [] })).toBe(false)
    expect(education.enabled?.({ ...ctxBase, tenantPlugins: ['edu'] })).toBe(false)
    expect(education.enabled?.({ ...ctxBase, tenantPlugins: ['inspection'] })).toBe(false)
  })
})
