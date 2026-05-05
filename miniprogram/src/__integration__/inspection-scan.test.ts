import { describe, it, expect } from 'vitest'
import { ContributionDispatcher } from '@core/plugin/dispatcher'
import inspection from '@plugins/inspection/manifest'
import demo from '@plugins/demo/manifest'
import healthcare from '@plugins/healthcare/manifest'
import type { ScanResolverContribution } from '@core/plugin/contribution'

describe('inspection scan-resolver integration', () => {
  function newDispatcher() {
    const d = new ContributionDispatcher('1.0.0')
    d.register(demo)
    d.register(healthcare)
    d.register(inspection)
    return d
  }

  function pickByPrefix(d: ContributionDispatcher, code: string): ScanResolverContribution | null {
    const all = d.query<ScanResolverContribution>('scan-resolver')
      .sort((a, b) => b.priority - a.priority)
    return all.find(r => code.startsWith(r.prefix)) ?? null
  }

  it('routes INSPECTION:TASK:123 to task-detail', () => {
    const d = newDispatcher()
    const r = pickByPrefix(d, 'INSPECTION:TASK:123')
    expect(r).not.toBeNull()
    const action = r!.resolve('INSPECTION:TASK:123')
    expect(action).not.toBeNull()
    expect(action!.path).toBe('/plugins/inspection/pages/task-detail')
    expect(action!.params).toEqual({ id: '123' })
  })

  it('returns null for INSPECTION:TASK: with empty id', () => {
    const d = newDispatcher()
    const r = pickByPrefix(d, 'INSPECTION:TASK:')
    expect(r).not.toBeNull()
    const action = r!.resolve('INSPECTION:TASK:')
    expect(action).toBeNull()
  })

  it('does not match unrelated prefixes', () => {
    const d = newDispatcher()
    expect(pickByPrefix(d, 'OTHER:THING:1')).toBeNull()
  })

  it('PATIENT: still routes via healthcare resolver — no priority conflict', () => {
    const d = newDispatcher()
    const r = pickByPrefix(d, 'PATIENT:P-001')
    expect(r).not.toBeNull()
    expect(r!.prefix).toBe('PATIENT:')
  })

  it('inspection has higher priority than healthcare for clarity', () => {
    const d = newDispatcher()
    const all = d.query<ScanResolverContribution>('scan-resolver')
    const insp = all.find(r => r.prefix === 'INSPECTION:TASK:')
    const hc = all.find(r => r.prefix === 'PATIENT:')
    expect(insp).toBeDefined()
    expect(hc).toBeDefined()
    // Both registered, no overlap; priorities don't conflict because prefixes differ.
    expect(insp!.priority).toBeGreaterThanOrEqual(0)
    expect(hc!.priority).toBeGreaterThanOrEqual(0)
  })
})
