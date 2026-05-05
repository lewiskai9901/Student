import { describe, it, expect } from 'vitest'
import type { Contribution, ContributionType } from './contribution'

describe('Contribution sealed union', () => {
  it('should accept all 8 contribution types', () => {
    const all: Contribution[] = [
      { type: 'menu', key: 'a.b', label: 'A', icon: 'i', path: '/p', order: 1 },
      { type: 'route', path: '/p', inSubPackage: true },
      { type: 'permission', code: 'a:b', description: 'd' },
      { type: 'message-handler', category: 'c', render: () => ({} as any) },
      { type: 'scan-resolver', prefix: 'X:', resolve: () => null, priority: 1 },
      { type: 'subscribe-template', templateId: 't', scenario: 's', description: 'd' },
      { type: 'config-schema', schema: { type: 'object' } },
      { type: 'event', eventName: 'a.b.c', payloadSchema: { type: 'object' } }
    ]
    expect(all).toHaveLength(8)
  })

  it('exhaustive check via never (compile-time guarantee)', () => {
    function exhaustive(c: Contribution): string {
      switch (c.type) {
        case 'menu': return 'menu'
        case 'route': return 'route'
        case 'permission': return 'permission'
        case 'message-handler': return 'message-handler'
        case 'scan-resolver': return 'scan-resolver'
        case 'subscribe-template': return 'subscribe-template'
        case 'config-schema': return 'config-schema'
        case 'event': return 'event'
        default: {
          const _x: never = c
          return _x
        }
      }
    }
    expect(exhaustive({ type: 'menu', key: 'a.b', label: 'A', icon: 'i', path: '/p', order: 1 })).toBe('menu')
  })
})
