import { describe, it, expect } from 'vitest'
import { ContributionDispatcher } from '@core/plugin/dispatcher'
import healthcare from './manifest'
import demo from '../demo/manifest'

describe('healthcare manifest', () => {
  it('registers without conflict alongside demo', () => {
    const d = new ContributionDispatcher('1.0.0')
    expect(() => {
      d.register(demo)
      d.register(healthcare)
    }).not.toThrow()
    expect(d.allPlugins().map(p => p.key)).toEqual(['demo', 'healthcare'])
  })

  it('declares unique scan prefix PATIENT:', () => {
    const d = new ContributionDispatcher('1.0.0')
    d.register(healthcare)
    const resolvers = d.query<any>('scan-resolver')
    expect(resolvers).toHaveLength(1)
    expect(resolvers[0].prefix).toBe('PATIENT:')
  })

  it('contributes 5 items: 1 perm + 1 menu + 1 route + 1 scan + 1 event', () => {
    expect(healthcare.contributions).toHaveLength(5)
    const types = healthcare.contributions.map(c => c.type).sort()
    expect(types).toEqual(['event', 'menu', 'permission', 'route', 'scan-resolver'])
  })
})
