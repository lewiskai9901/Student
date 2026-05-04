import { describe, it, expect, beforeEach } from 'vitest'
import { ContributionDispatcher } from './dispatcher'
import { definePlugin } from './package'
import type { MenuContribution, RouteContribution } from './contribution'

describe('ContributionDispatcher', () => {
  let d: ContributionDispatcher
  beforeEach(() => { d = new ContributionDispatcher('1.0.0') })

  it('registers and queries menu contributions', () => {
    const p = definePlugin({
      key: 'demo', label: 'Demo', schemaVersion: 1, minCoreVersion: '0.1.0',
      contributions: [
        { type: 'menu', key: 'demo.home', label: 'Home', icon: 'home', path: '/x', order: 1 } satisfies MenuContribution
      ]
    })
    d.register(p)
    const menus = d.query<MenuContribution>('menu')
    expect(menus).toHaveLength(1)
    expect(menus[0].key).toBe('demo.home')
  })

  it('rejects duplicate menu key across plugins', () => {
    const a = definePlugin({
      key: 'a', label: 'A', schemaVersion: 1, minCoreVersion: '0.1.0',
      contributions: [{ type: 'menu', key: 'shared.x', label: 'A', icon: 'i', path: '/a', order: 1 }]
    })
    const b = definePlugin({
      key: 'b', label: 'B', schemaVersion: 1, minCoreVersion: '0.1.0',
      contributions: [{ type: 'menu', key: 'shared.x', label: 'B', icon: 'i', path: '/b', order: 1 }]
    })
    d.register(a)
    expect(() => d.register(b)).toThrow(/duplicate menu key/i)
  })

  it('rejects duplicate scan-resolver prefix', () => {
    const a = definePlugin({
      key: 'a', label: 'A', schemaVersion: 1, minCoreVersion: '0.1.0',
      contributions: [{ type: 'scan-resolver', prefix: 'X:', resolve: () => null, priority: 1 }]
    })
    const b = definePlugin({
      key: 'b', label: 'B', schemaVersion: 1, minCoreVersion: '0.1.0',
      contributions: [{ type: 'scan-resolver', prefix: 'X:', resolve: () => null, priority: 2 }]
    })
    d.register(a)
    expect(() => d.register(b)).toThrow(/duplicate scan/i)
  })

  it('rejects plugin requiring newer core version', () => {
    const dOld = new ContributionDispatcher('0.0.5')
    const p = definePlugin({
      key: 'x', label: 'X', schemaVersion: 1, minCoreVersion: '1.0.0', contributions: []
    })
    expect(() => dOld.register(p)).toThrow(/minCoreVersion/i)
  })

  it('rejects duplicate plugin key', () => {
    const p = definePlugin({ key: 'same', label: 'A', schemaVersion: 1, minCoreVersion: '0.1.0', contributions: [] })
    d.register(p)
    expect(() => d.register(p)).toThrow(/already registered/i)
  })

  it('queryFiltered applies predicate', () => {
    const p = definePlugin({
      key: 'demo', label: 'Demo', schemaVersion: 1, minCoreVersion: '0.1.0',
      contributions: [
        { type: 'menu', key: 'demo.a', label: 'A', icon: 'i', path: '/a', order: 1, group: 'home-grid' },
        { type: 'menu', key: 'demo.b', label: 'B', icon: 'i', path: '/b', order: 2, group: 'mine-extra' }
      ]
    })
    d.register(p)
    const home = d.queryFiltered<MenuContribution>('menu', m => m.group === 'home-grid')
    expect(home).toHaveLength(1)
    expect(home[0].key).toBe('demo.a')
  })
})
