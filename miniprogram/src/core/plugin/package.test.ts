import { describe, it, expect } from 'vitest'
import { definePlugin } from './package'

describe('definePlugin', () => {
  it('returns a frozen plugin package', () => {
    const p = definePlugin({
      key: 'demo',
      label: 'Demo',
      schemaVersion: 1,
      minCoreVersion: '0.1.0',
      contributions: []
    })
    expect(Object.isFrozen(p)).toBe(true)
    expect(p.key).toBe('demo')
  })

  it('rejects empty key', () => {
    expect(() => definePlugin({
      key: '', label: 'x', schemaVersion: 1, minCoreVersion: '0.1.0', contributions: []
    })).toThrow(/key/)
  })

  it('rejects schemaVersion other than 1', () => {
    expect(() => definePlugin({
      key: 'x', label: 'x', schemaVersion: 2 as any,
      minCoreVersion: '0.1.0', contributions: []
    })).toThrow(/schemaVersion/)
  })
})
