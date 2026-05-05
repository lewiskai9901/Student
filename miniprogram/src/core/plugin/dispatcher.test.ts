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

  it('rejects duplicate route path across plugins', () => {
    const a = definePlugin({
      key: 'a', label: 'A', schemaVersion: 1, minCoreVersion: '0.1.0',
      contributions: [{ type: 'route', path: 'plugins/x/foo', inSubPackage: true }]
    })
    const b = definePlugin({
      key: 'b', label: 'B', schemaVersion: 1, minCoreVersion: '0.1.0',
      contributions: [{ type: 'route', path: 'plugins/x/foo', inSubPackage: true }]
    })
    d.register(a)
    expect(() => d.register(b)).toThrow(/duplicate route path/i)
  })

  it('rejects intra-plugin duplicate menu key', () => {
    const p = definePlugin({
      key: 'a', label: 'A', schemaVersion: 1, minCoreVersion: '0.1.0',
      contributions: [
        { type: 'menu', key: 'a.x', label: 'A1', icon: 'i', path: '/a1', order: 1 },
        { type: 'menu', key: 'a.x', label: 'A2', icon: 'i', path: '/a2', order: 2 }
      ]
    })
    expect(() => d.register(p)).toThrow(/duplicate menu key/i)
  })

  it('failed register leaves no ghost state — retry with cleaned plugin succeeds', () => {
    const bad = definePlugin({
      key: 'p', label: 'P', schemaVersion: 1, minCoreVersion: '0.1.0',
      contributions: [
        { type: 'menu', key: 'p.first', label: 'F', icon: 'i', path: '/f', order: 1 },
        { type: 'route', path: 'plugins/p/dup', inSubPackage: true },
        { type: 'route', path: 'plugins/p/dup', inSubPackage: true }
      ]
    })
    expect(() => d.register(bad)).toThrow(/duplicate route path/i)

    // Now register a different plugin claiming the same menu key the bad attempt
    // tried — it must succeed because the bad attempt rolled back atomically.
    const good = definePlugin({
      key: 'q', label: 'Q', schemaVersion: 1, minCoreVersion: '0.1.0',
      contributions: [
        { type: 'menu', key: 'p.first', label: 'F', icon: 'i', path: '/f', order: 1 }
      ]
    })
    expect(() => d.register(good)).not.toThrow()
    expect(d.query('menu')).toHaveLength(1)
  })

  it('rejects malformed semver in coreVersion or minCoreVersion', () => {
    const dBad = new ContributionDispatcher('1.0' as any) // truncated
    const p = definePlugin({ key: 'x', label: 'X', schemaVersion: 1, minCoreVersion: '0.1.0', contributions: [] })
    expect(() => dBad.register(p)).toThrow(/invalid semver/i)
  })
})
