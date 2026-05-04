import { describe, it, expect } from 'vitest'
import { verify } from './verify-manifest.js'

describe('verify', () => {
  it('returns no errors for valid manifest list', () => {
    const plugins = [
      {
        key: 'demo', schemaVersion: 1,
        contributions: [
          { type: 'permission', code: 'demo:hello:view', description: '' },
          { type: 'menu', key: 'demo.hello', label: 'H', icon: 'h', path: '/x', perm: 'demo:hello:view', order: 1 },
          { type: 'route', path: 'plugins/demo/pages/hello', inSubPackage: true }
        ]
      }
    ]
    expect(verify(plugins)).toEqual([])
  })

  it('flags duplicate plugin key', () => {
    const errs = verify([
      { key: 'a', schemaVersion: 1, contributions: [] },
      { key: 'a', schemaVersion: 1, contributions: [] }
    ])
    expect(errs.some(e => /duplicate plugin key/i.test(e))).toBe(true)
  })

  it('flags wrong schemaVersion', () => {
    const errs = verify([{ key: 'x', schemaVersion: 2, contributions: [] }])
    expect(errs.some(e => /schemaVersion/.test(e))).toBe(true)
  })

  it('flags duplicate menu key across plugins', () => {
    const errs = verify([
      { key: 'a', schemaVersion: 1, contributions: [{ type: 'menu', key: 'shared.x', label: '', icon: '', path: '/', order: 1 }] },
      { key: 'b', schemaVersion: 1, contributions: [{ type: 'menu', key: 'shared.x', label: '', icon: '', path: '/', order: 1 }] }
    ])
    expect(errs.some(e => /duplicate menu key/i.test(e))).toBe(true)
  })

  it('flags duplicate route path across plugins', () => {
    const errs = verify([
      { key: 'a', schemaVersion: 1, contributions: [{ type: 'route', path: 'plugins/x/p', inSubPackage: true }] },
      { key: 'b', schemaVersion: 1, contributions: [{ type: 'route', path: 'plugins/x/p', inSubPackage: true }] }
    ])
    expect(errs.some(e => /duplicate route path/i.test(e))).toBe(true)
  })

  it('flags duplicate scan prefix', () => {
    const errs = verify([
      { key: 'a', schemaVersion: 1, contributions: [{ type: 'scan-resolver', prefix: 'X:', priority: 1 }] },
      { key: 'b', schemaVersion: 1, contributions: [{ type: 'scan-resolver', prefix: 'X:', priority: 1 }] }
    ])
    expect(errs.some(e => /duplicate scan/i.test(e))).toBe(true)
  })

  it('flags menu referencing undeclared permission', () => {
    const errs = verify([
      { key: 'a', schemaVersion: 1, contributions: [
        { type: 'menu', key: 'a.x', label: '', icon: '', path: '/', perm: 'a:b:c', order: 1 }
      ] }
    ])
    expect(errs.some(e => /a:b:c.*not declared/i.test(e))).toBe(true)
  })
})
